package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when the delivery partner confirms successful delivery.
 * Use case:
 *  - Send "Your order has been delivered!" email/push to customer.
 *  - Prompt customer to rate the delivery experience.
 *  - Trigger the review-eligibility window for the product.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private Long deliveryPartnerId;
    private String trackingNumber;
    private LocalDateTime deliveredAt;
}
