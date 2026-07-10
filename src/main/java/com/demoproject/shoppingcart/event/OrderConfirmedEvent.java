package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Event fired after payment succeeds AND a shipment record has been created.
 * This is the canonical "your order is confirmed" event — richer than
 * PaymentSuccessEvent because it includes shipment tracking info.
 * Use case: Send order confirmation email with tracking number and ETA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private Long totalAmount;
    private String trackingNumber;
    private LocalDate expectedDeliveryDate;
    private LocalDateTime confirmedAt;
}
