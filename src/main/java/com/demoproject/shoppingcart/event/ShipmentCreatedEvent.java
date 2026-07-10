package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event fired immediately after a Shipment record is created following a successful payment.
 * Use case: Alert admin/fulfilment team that a new shipment needs to be assigned to a partner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentCreatedEvent {
    private Long shipmentId;
    private Long orderId;
    private String trackingNumber;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime createdAt;
}
