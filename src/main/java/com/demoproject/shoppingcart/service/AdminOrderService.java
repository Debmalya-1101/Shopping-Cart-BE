package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AdminOrderResponseDTO;
import com.demoproject.shoppingcart.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    Page<AdminOrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable);

    AdminOrderResponseDTO getOrderById(Long orderId);

    /**
     * Admin cancels a confirmed (paid) order with a mandatory justification.
     * Sets PaymentStatus to SUCCESS_REQUIRES_REFUND and publishes OrderCancelledEvent.
     * Only allowed from CONFIRMED state — once PROCESSING starts the parcel has been picked up.
     *
     * @param orderId the ID of the order to cancel
     * @param reason  mandatory justification (surfaced in the apology notification to the user)
     */
    AdminOrderResponseDTO cancelOrderByAdmin(Long orderId, String reason);
}
