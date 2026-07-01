package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponseDTO {
    private Long id;
    private Long orderId;
    private Long deliveryPartnerId;
    private ShipmentStatus status;
    private String trackingNumber;
    private LocalDate expectedDeliveryDate;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
