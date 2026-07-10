package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.CheckoutRequestDTO;
import com.demoproject.shoppingcart.dto.OrderDetailDTO;
import com.demoproject.shoppingcart.dto.OrderDetailItemDTO;
import com.demoproject.shoppingcart.dto.OrderItemDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemReturnRequestDTO;
import com.demoproject.shoppingcart.event.OrderCancelledEvent;
import com.demoproject.shoppingcart.event.OrderPlacedEvent;
import com.demoproject.shoppingcart.event.ReturnRequestedEvent;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.AddressRepository;
import com.demoproject.shoppingcart.repository.CartItemRepository;
import com.demoproject.shoppingcart.repository.CartRepository;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.repository.ShipmentRepository;
import com.demoproject.shoppingcart.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Service
public class OrderServiceImpl implements OrderService {

    private static final int MAX_PAYMENT_RETRIES = 3;

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final ShipmentRepository shipmentRepository;
    private final com.demoproject.shoppingcart.service.InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(UserRepository userRepository,
                            CartRepository cartRepository,
                            OrderRepository orderRepository,
                            CartItemRepository cartItemRepository,
                            AddressRepository addressRepository,
                            ShipmentRepository shipmentRepository,
                            com.demoproject.shoppingcart.service.InventoryService inventoryService,
                            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.shipmentRepository = shipmentRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponseDTO checkout(CheckoutRequestDTO request) {

        AppUser user = getLoggedInUser();

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Order order = new Order();
        order.setUser(user);
        // PENDING_PAYMENT: order created; awaiting payment — not yet confirmed
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        // ── Address resolution (saved address → default address → manual form) ──
        String shippingName;
        Long shippingPhone;
        String shippingEmail = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) ? request.getEmail() : user.getEmailId();
        String shippingAddressStr;

        if (request.getAddressId() != null) {
            Address address = addressRepository.findByIdAndUser(request.getAddressId(), user)
                    .orElseThrow(() -> new RuntimeException("Saved address not found or unauthorized"));
            shippingName = address.getContactName();
            shippingPhone = parsePhone(address.getMobileNumber());
            shippingAddressStr = formatAddress(address);
        } else {
            java.util.Optional<Address> defaultAddressOpt = addressRepository.findByUserAndIsDefault(user, true);
            if (defaultAddressOpt.isPresent()) {
                Address address = defaultAddressOpt.get();
                shippingName = address.getContactName();
                shippingPhone = parsePhone(address.getMobileNumber());
                shippingAddressStr = formatAddress(address);
            } else {
                if (request.getName() == null || request.getName().trim().isEmpty() ||
                        request.getPhoneNo() == null ||
                        request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                    throw new RuntimeException(
                            "Shipping address information is missing. Please provide address details or select a saved address.");
                }
                shippingName = request.getName();
                shippingPhone = request.getPhoneNo();
                shippingAddressStr = request.getAddress();
            }
        }

        order.setName(shippingName);
        order.setPhoneNo(shippingPhone);
        order.setEmail(shippingEmail);
        order.setAddress(shippingAddressStr);

        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(cartItem.getProduct());
            oi.setQuantity(cartItem.getQuantity());
            oi.setPrice(cartItem.getPrice()); // snapshot price at time of order
            return oi;
        }).toList();

        order.setItems(orderItems);
        order.setTotal(orderItems.stream().mapToLong(i -> i.getPrice() * i.getQuantity()).sum());

        Order savedOrder = orderRepository.save(order);

        // Reserve stock — released if payment fails, consumed if payment succeeds
        for (OrderItem item : savedOrder.getItems()) {
            inventoryService.reserveStock(
                    item.getProduct().getId(),
                    item.getQuantity().intValue(),
                    "ORDER",
                    savedOrder.getId().toString(),
                    "User checkout"
            );
        }

        // Clear cart
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setTotalPrice(0L);
        cartRepository.save(cart);

        // Publish OrderPlacedEvent (order created, payment pending)
        List<OrderPlacedEvent.OrderItemEvent> itemEvents = savedOrder.getItems().stream()
                .map(item -> new OrderPlacedEvent.OrderItemEvent(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()
                )).collect(Collectors.toList());

        eventPublisher.publishEvent(new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getUser().getId(),
                savedOrder.getUser().getEmailId(),
                savedOrder.getTotal(),
                savedOrder.getCreatedAt(),
                itemEvents
        ));

        return convertToOrderDTO(savedOrder);
    }

    @Override
    public com.demoproject.shoppingcart.dto.PageResponse<OrderResponseDTO> getMyOrders(int page, int size) {
        AppUser user = getLoggedInUser();
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<Order> orderPage =
                orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<OrderResponseDTO> dtoList = orderPage.getContent()
                .stream()
                .map(this::convertToOrderDTO)
                .toList();

        return new com.demoproject.shoppingcart.dto.PageResponse<>(
                dtoList,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.isLast()
        );
    }

    @Override
    public OrderDetailDTO getOrderById(Long orderId) {
        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        return convertToOrderDetailDTO(order);
    }

    /**
     * User-initiated cancellation.
     * Allowed only from PENDING_PAYMENT (before payment) or CONFIRMED (after payment).
     * Once PROCESSING has started the parcel has been picked up — no user cancellation.
     * If payment was already successful (CONFIRMED state), marks for refund.
     */
    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponseDTO cancelOrder(Long orderId) {
        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You can only cancel your own orders.");
        }

        OrderStatus current = order.getStatus();
        if (current != OrderStatus.PENDING_PAYMENT && current != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Order cannot be cancelled. Cancellation is only allowed before processing begins. Current status: " + current);
        }

        boolean refundRequired = (current == OrderStatus.CONFIRMED);
        Long refundAmount = refundRequired ? order.getTotal() : 0L;

        order.setStatus(OrderStatus.CANCELLED);
        if (refundRequired) {
            order.setPaymentStatus(PaymentStatus.SUCCESS_REQUIRES_REFUND);
        }
        orderRepository.save(order);

        // Release reserved stock back to available
        for (OrderItem item : order.getItems()) {
            inventoryService.cancelOrderStock(
                    item.getProduct().getId(),
                    item.getQuantity().intValue(),
                    "ORDER_CANCEL",
                    order.getId().toString(),
                    "Order cancelled by user"
            );
        }

        eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                user.getId(),
                user.getEmailId(),
                OrderCancelledEvent.CancelledBy.USER,
                null,
                refundRequired,
                refundAmount,
                LocalDateTime.now()
        ));

        return convertToOrderDTO(order);
    }

    /**
     * User-initiated return request for specific order items.
     * Only allowed once the entire order is in DELIVERED state.
     * Sets each requested item to RETURN_REQUESTED; admin reviews and approves/rejects.
     *
     * NOTE: Full return workflow (pickup, restock, refund) is a future iteration.
     *       This method only captures the request and fires the event.
     */
    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponseDTO processReturn(Long orderId, OrderItemReturnRequestDTO request) {
        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized: You can only request returns for your own orders.");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Return requests can only be raised for DELIVERED orders.");
        }

        List<ReturnRequestedEvent.ReturnItemDetail> itemDetails = new java.util.ArrayList<>();

        for (OrderItemReturnRequestDTO.ReturnItemDTO returnItem : request.getItems()) {
            OrderItem orderItem = order.getItems().stream()
                    .filter(item -> item.getId().equals(returnItem.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "OrderItem not found: " + returnItem.getOrderItemId()));

            if (orderItem.getStatus() == OrderItemStatus.RETURN_REQUESTED ||
                    orderItem.getStatus() == OrderItemStatus.RETURN_APPROVED ||
                    orderItem.getStatus() == OrderItemStatus.RETURNED) {
                throw new IllegalStateException(
                        "Return already in progress for OrderItem: " + orderItem.getId());
            }

            if (returnItem.getQuantity().longValue() > orderItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Return quantity cannot exceed purchased quantity for item: " + orderItem.getId());
            }

            orderItem.setStatus(OrderItemStatus.RETURN_REQUESTED);
            itemDetails.add(new ReturnRequestedEvent.ReturnItemDetail(
                    orderItem.getId(),
                    orderItem.getProduct().getName(),
                    returnItem.getQuantity().longValue()
            ));
        }

        orderRepository.save(order);

        // Publish event for future return workflow listener
        eventPublisher.publishEvent(new ReturnRequestedEvent(
                order.getId(),
                user.getId(),
                user.getEmailId(),
                itemDetails,
                LocalDateTime.now()
        ));

        return convertToOrderDTO(order);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private OrderResponseDTO convertToOrderDTO(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getImageUrl(),
                        i.getPrice(),
                        i.getQuantity(),
                        i.getPrice() * i.getQuantity()
                )).toList();

        String deliveryStatus = shipmentRepository.findByOrderId(order.getId())
                .map(s -> s.getStatus().name())
                .orElse("PENDING");

        return new OrderResponseDTO(
                order.getId(),
                order.getTotal(),
                order.getStatus().name(),
                order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING",
                deliveryStatus,
                order.getCreatedAt(),
                items
        );
    }

    private OrderDetailDTO convertToOrderDetailDTO(Order order) {
        List<OrderDetailItemDTO> items = order.getItems().stream()
                .map(i -> {
                    Product product = i.getProduct();
                    String categoryName = (product.getCategory() != null)
                            ? product.getCategory().getName()
                            : null;
                    return new OrderDetailItemDTO(
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            categoryName,
                            i.getQuantity(),
                            i.getPrice(),
                            i.getPrice() * i.getQuantity()
                    );
                }).toList();

        Long deliveryPartnerId = null;
        String deliveryPartnerName = null;
        String deliveryPartnerPhone = null;

        java.util.Optional<Shipment> shipmentOpt = shipmentRepository.findByOrderId(order.getId());
        if (shipmentOpt.isPresent() && shipmentOpt.get().getDeliveryPartner() != null) {
            DeliveryPartner partner = shipmentOpt.get().getDeliveryPartner();
            deliveryPartnerId = partner.getId();
            deliveryPartnerName = partner.getFullName();
            deliveryPartnerPhone = partner.getPhoneNumber();
        }

        return new OrderDetailDTO(
                order.getId(),
                order.getStatus().name(),
                order.getPaymentStatus().name(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getName(),
                order.getEmail(),
                order.getPhoneNo(),
                order.getAddress(),
                deliveryPartnerId,
                deliveryPartnerName,
                deliveryPartnerPhone,
                items,
                items.size(),
                order.getTotal()
        );
    }

    private Long parsePhone(String mobileNumber) {
        if (mobileNumber == null) return 0L;
        try {
            return Long.parseLong(mobileNumber.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getAddressLine());
        if (address.getCity() != null && !address.getCity().trim().isEmpty()) {
            sb.append(", ").append(address.getCity());
        }
        if (address.getState() != null && !address.getState().trim().isEmpty()) {
            sb.append(", ").append(address.getState());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().trim().isEmpty()) {
            sb.append(" - ").append(address.getPostalCode());
        }
        if (address.getCountry() != null && !address.getCountry().trim().isEmpty()) {
            sb.append(", ").append(address.getCountry());
        }
        return sb.toString();
    }
}
