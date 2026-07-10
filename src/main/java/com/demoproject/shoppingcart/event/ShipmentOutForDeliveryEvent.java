package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event fired when the delivery partner marks the shipment as OUT_FOR_DELIVERY.
 * Use case: Notify customer "Your order will be delivered today!" alert.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentOutForDeliveryEvent {
    private Long shipmentId;
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String trackingNumber;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime outForDeliveryAt;
}
