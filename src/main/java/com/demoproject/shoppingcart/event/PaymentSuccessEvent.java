package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when payment is successfully completed
 * Use case: Inventory reduction, order confirmation, revenue tracking, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    private Long orderId;
    private Long userId;
    private String paymentReferenceId;
    private Long amount;
    private LocalDateTime paymentCompletedAt;
    private String paymentMethod; // For future use with Razorpay integration
}

