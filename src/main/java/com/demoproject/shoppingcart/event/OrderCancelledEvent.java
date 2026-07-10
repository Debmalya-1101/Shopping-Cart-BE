package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when an order is cancelled — by the user or by an admin.
 *
 * When cancelledBy is ADMIN:
 *  - refundRequired will be true (payment already succeeded)
 *  - cancelReason will contain the admin's justification
 *  - Notification listener should send an apology email to the user
 *
 * When cancelledBy is USER:
 *  - refundRequired depends on whether payment was already made
 *  - cancelReason may be null
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    public enum CancelledBy { USER, ADMIN }

    private Long orderId;
    private Long userId;
    private String userEmail;
    private CancelledBy cancelledBy;
    /** Admin-provided justification (only present when cancelledBy == ADMIN). */
    private String cancelReason;
    /**
     * True when payment was already successful before cancellation.
     * Listener should flag the order for refund processing.
     */
    private boolean refundRequired;
    private Long refundAmount;
    private LocalDateTime cancelledAt;
}
