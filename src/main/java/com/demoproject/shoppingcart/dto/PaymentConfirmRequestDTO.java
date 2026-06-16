package com.demoproject.shoppingcart.dto;

import lombok.Data;

@Data
public class PaymentConfirmRequestDTO {
    private Long orderId;
    private String paymentToken;
    private String paymentReferenceId;
    private boolean success;
    
    // Razorpay specific fields
    private String razorpayPaymentId;
    private String razorpaySignature;
}

