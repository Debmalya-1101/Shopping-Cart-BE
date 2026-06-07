package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiateResponseDTO {
    private Long orderId;
    private Long amount;
    private String currency;
    private String paymentToken;
    private String paymentReferenceId;
    private LocalDateTime paymentInitiatedAt;
}


