package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when admin assigns a delivery partner to a shipment.
 * Use case: Notify the delivery partner of a new job via push/SMS.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentAssignedEvent {
    private Long shipmentId;
    private Long orderId;
    private Long deliveryPartnerId;
    private String deliveryPartnerName;
    private String trackingNumber;
    private LocalDateTime assignedAt;
}
