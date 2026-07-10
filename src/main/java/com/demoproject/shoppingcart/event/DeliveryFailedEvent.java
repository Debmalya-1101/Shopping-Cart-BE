package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when a delivery attempt fails (shipment → DELIVERY_FAILED).
 * Use case:
 *  - Notify customer why delivery failed and what happens next.
 *  - Alert admin to take action (reassign partner or initiate return).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFailedEvent {
    private Long shipmentId;
    private Long orderId;
    private Long userId;
    private String userEmail;
    /** Reason provided by the delivery partner (e.g. "Customer not available"). */
    private String failureReason;
    private LocalDateTime failedAt;
}
