package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when the delivery partner marks the shipment as PICKED_UP.
 * Use case: Notify customer that their order is now in transit / on its way.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentPickedUpEvent {
    private Long shipmentId;
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String trackingNumber;
    private LocalDateTime pickedUpAt;
}
