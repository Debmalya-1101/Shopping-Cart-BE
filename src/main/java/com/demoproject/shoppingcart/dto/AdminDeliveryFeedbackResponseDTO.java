package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDeliveryFeedbackResponseDTO {
    private Long id;
    private Long orderId;
    private Long customerId;
    private String customerUsername;
    private String customerEmail;
    private Long deliveryPartnerId;
    private String deliveryPartnerName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
