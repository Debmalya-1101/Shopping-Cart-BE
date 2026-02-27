package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.PaymentConfirmRequestDTO;
import com.demoproject.shoppingcart.dto.PaymentInitiateResponseDTO;

public interface PaymentService {

    PaymentInitiateResponseDTO initiatePayment(Long orderId);

    String confirmPayment(PaymentConfirmRequestDTO request);
}

