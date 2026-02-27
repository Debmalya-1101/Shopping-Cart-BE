package com.demoproject.shoppingcart.dto;

import lombok.Data;

@Data
public class PaymentConfirmRequestDTO {
    private Long orderId;
    private String paymentToken;
    private boolean success;
}

