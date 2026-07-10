package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when a payment attempt fails for an order.
 * Use case: Notify user to retry payment, show failure reason in UI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    /** Current retry count after this failure (1-indexed). */
    private int retryCount;
    /** Maximum retries allowed before the order is auto-cancelled. */
    private int maxRetries;
    private LocalDateTime failedAt;
}
