package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AdminOrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemDTO;
import com.demoproject.shoppingcart.event.OrderCancelledEvent;
import com.demoproject.shoppingcart.model.Order;
import com.demoproject.shoppingcart.model.OrderItem;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.model.PaymentStatus;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.service.AdminOrderService;
import com.demoproject.shoppingcart.service.InventoryService;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    public AdminOrderServiceImpl(OrderRepository orderRepository,
                                 InventoryService inventoryService,
                                 ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Page<AdminOrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable) {
        Page<Order> orders = (status != null)
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return orders.map(this::convertToAdminDTO);
    }

    @Override
    public AdminOrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToAdminDTO(order);
    }

    /**
     * Admin-initiated order cancellation with mandatory justification.
     *
     * Rules:
     *  - Only CONFIRMED orders can be admin-cancelled (payment succeeded, but processing not started).
     *  - PENDING_PAYMENT orders should be handled by the auto-cleanup task or user cancellation.
     *  - PROCESSING or beyond: the parcel has already been handed to the delivery partner — admin
     *    must contact the partner separately; this endpoint will reject the request.
     *
     * Side-effects:
     *  - Sets OrderStatus → CANCELLED
     *  - Sets PaymentStatus → SUCCESS_REQUIRES_REFUND (since payment was already taken)
     *  - Releases reserved inventory
     *  - Publishes OrderCancelledEvent with refundRequired=true and the admin's reason
     */
    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public AdminOrderResponseDTO cancelOrderByAdmin(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus current = order.getStatus();

        if (current != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Admin can only cancel orders in CONFIRMED state. " +
                    "Current status: " + current + ". " +
                    "If the order is PENDING_PAYMENT it will auto-expire, or the user can cancel it. " +
                    "If PROCESSING or beyond, please coordinate with the delivery partner directly.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.SUCCESS_REQUIRES_REFUND);
        order.setAdminCancelReason(reason);
        orderRepository.save(order);

        // Release inventory - stock was consumed at payment; return it to available pool
        for (OrderItem item : order.getItems()) {
            try {
                String notes = "Admin cancelled order: " + reason;
                if (notes.length() > 255) {
                    notes = notes.substring(0, 255);
                }
                inventoryService.cancelOrderStock(
                        item.getProduct().getId(),
                        item.getQuantity().intValue(),
                        "ORDER_CANCEL",
                        order.getId().toString(),
                        notes
                );
            } catch (Exception e) {
                // Log but don't fail - inventory can be reconciled separately
                System.err.println("Failed to return inventory for order " + orderId + ": " + e.getMessage());
            }
        }

        // Fire event — future listener sends apology email with refund notice
        eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmailId(),
                OrderCancelledEvent.CancelledBy.ADMIN,
                reason,
                true,   // refundRequired = always true for admin cancel of CONFIRMED order
                order.getTotal(),
                LocalDateTime.now()
        ));

        return convertToAdminDTO(order);
    }

    private AdminOrderResponseDTO convertToAdminDTO(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getImageUrl(),
                        i.getPrice(),
                        i.getQuantity(),
                        i.getPrice() * i.getQuantity()
                )).toList();

        return new AdminOrderResponseDTO(
                order.getId(),
                order.getUser().getUserName(),
                order.getEmail(),
                order.getAddress(),
                order.getPhoneNo(),
                order.getTotal(),
                order.getStatus().name(),
                order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null,
                order.getCreatedAt(),
                order.getAdminCancelReason(),
                items
        );
    }
}
