package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.PaymentConfirmRequestDTO;
import com.demoproject.shoppingcart.dto.PaymentInitiateResponseDTO;
import com.demoproject.shoppingcart.dto.RetryPaymentRequest;
import com.demoproject.shoppingcart.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")

public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Initiate payment for an order
     * POST /api/payments/initiate/{orderId}
     *
     * Returns:
     * - orderId
     * - amount
     * - currency (INR)
     * - paymentToken
     * - paymentReferenceId (unique reference)
     * - paymentInitiatedAt (timestamp)
     */
    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<PaymentInitiateResponseDTO> initiate(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    /**
     * Confirm payment after user completes payment gateway flow
     * POST /api/payments/confirm
     *
     * Request body includes:
     * - orderId
     * - paymentToken
     * - paymentReferenceId
     * - success (boolean)
     */
    @PostMapping("/confirm")
    public ResponseEntity<String> confirm(
            @Valid @RequestBody PaymentConfirmRequestDTO request) {

        return ResponseEntity.ok(paymentService.confirmPayment(request));
    }

    /**
     * Retry payment for failed orders
     * POST /api/payments/retry
     *
     * Allows users to retry payment up to 3 times
     * Generates new payment reference ID
     */
    @PostMapping("/retry")
    public ResponseEntity<PaymentInitiateResponseDTO> retryPayment(
            @Valid @RequestBody RetryPaymentRequest request) {

        return ResponseEntity.ok(paymentService.initiatePayment(request.getOrderId()));
    }
}


