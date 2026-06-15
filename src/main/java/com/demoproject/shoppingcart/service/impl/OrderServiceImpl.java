package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.CheckoutRequestDTO;
import com.demoproject.shoppingcart.dto.OrderDetailDTO;
import com.demoproject.shoppingcart.dto.OrderDetailItemDTO;
import com.demoproject.shoppingcart.dto.OrderItemDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemReturnRequestDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.AddressRepository;
import com.demoproject.shoppingcart.repository.CartItemRepository;
import com.demoproject.shoppingcart.repository.CartRepository;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Service
public class OrderServiceImpl implements OrderService {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final com.demoproject.shoppingcart.service.InventoryService inventoryService;

    public OrderServiceImpl(UserRepository userRepository, CartRepository cartRepository, OrderRepository orderRepository, CartItemRepository cartItemRepository, AddressRepository addressRepository, com.demoproject.shoppingcart.service.InventoryService inventoryService) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.inventoryService = inventoryService;
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

        for (CartItem cartItem : cart.getItems()) {
            // Replaced manual stock check with InventoryService reservation later in the transaction
        }


        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PLACED);

        // Address fallback resolution logic
        String shippingName;
        Long shippingPhone;
        String shippingEmail = request.getEmail() != null ? request.getEmail() : user.getEmailId();
        String shippingAddressStr;

        if (request.getAddressId() != null) {
            // 1. User selected a specific saved address
            Address address = addressRepository.findByIdAndUser(request.getAddressId(), user)
                    .orElseThrow(() -> new RuntimeException("Saved address not found or unauthorized"));
            shippingName = address.getContactName();
            shippingPhone = parsePhone(address.getMobileNumber());
            shippingAddressStr = formatAddress(address);
        } else {
            // 2. Try default address fallback
            java.util.Optional<Address> defaultAddressOpt = addressRepository.findByUserAndIsDefault(user, true);
            if (defaultAddressOpt.isPresent()) {
                Address address = defaultAddressOpt.get();
                shippingName = address.getContactName();
                shippingPhone = parsePhone(address.getMobileNumber());
                shippingAddressStr = formatAddress(address);
            } else {
                // 3. Fallback to manual checkout form details
                if (request.getName() == null || request.getName().trim().isEmpty() ||
                        request.getPhoneNo() == null ||
                        request.getAddress() == null || request.getAddress().trim().isEmpty()) {
                    throw new RuntimeException("Shipping address information is missing. Please provide address details or select a saved address.");
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
            oi.setPrice(cartItem.getPrice()); // snapshot
            return oi;
        }).toList();

        order.setItems(orderItems);

        Long total = orderItems.stream()
                .mapToLong(i -> i.getPrice() * i.getQuantity())
                .sum();

        order.setTotal(total);

        Order savedOrder = orderRepository.save(order);

        // Reserve stock using InventoryService
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

        // 🔮 Kafka later:
        // orderEventProducer.sendOrderPlacedEvent(savedOrder);

        return convertToOrderDTO(savedOrder);
    }

    @Override
    public com.demoproject.shoppingcart.dto.PageResponse<OrderResponseDTO> getMyOrders(int page, int size) {
        AppUser user = getLoggedInUser();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);

        org.springframework.data.domain.Page<Order> orderPage = orderRepository.findByUserOrderByCreatedAtDesc(user, pageable);

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


    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ── Lightweight DTO used for list view and checkout response ─────────────

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

        return new OrderResponseDTO(
                order.getId(),
                order.getTotal(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
        );
    }

    // ── Rich DTO used for the Order Details page ──────────────────────────────

    private OrderDetailDTO convertToOrderDetailDTO(Order order) {

        List<OrderDetailItemDTO> items = order.getItems().stream()
                .map(i -> {
                    Product product = i.getProduct();

                    // Resolve category name if the product has a category assigned
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

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponseDTO cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new IllegalStateException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            inventoryService.cancelOrderStock(
                    item.getProduct().getId(),
                    item.getQuantity().intValue(),
                    "ORDER_CANCEL",
                    order.getId().toString(),
                    "Order cancelled by user/admin"
            );
        }

        return convertToOrderDTO(order);
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public OrderResponseDTO processReturn(Long orderId, OrderItemReturnRequestDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order must be DELIVERED to process returns.");
        }

        for (OrderItemReturnRequestDTO.ReturnItemDTO returnItem : request.getItems()) {
            OrderItem orderItem = order.getItems().stream()
                    .filter(item -> item.getId().equals(returnItem.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("OrderItem not found: " + returnItem.getOrderItemId()));

            if (orderItem.getStatus() == OrderItemStatus.RETURNED) {
                throw new IllegalStateException("OrderItem is already RETURNED: " + orderItem.getId());
            }

            if (returnItem.getQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException("Return quantity cannot exceed purchased quantity for item: " + orderItem.getId());
            }

            orderItem.setStatus(OrderItemStatus.RETURNED);
            
            inventoryService.returnStock(
                    orderItem.getProduct().getId(),
                    returnItem.getQuantity(),
                    returnItem.getCondition(),
                    "ORDER_RETURN",
                    order.getId().toString(),
                    request.getNotes() != null ? request.getNotes() : "Item returned"
            );
        }

        orderRepository.save(order);
        return convertToOrderDTO(order);
    }

}
