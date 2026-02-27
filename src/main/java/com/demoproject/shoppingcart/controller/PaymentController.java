package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.PaymentConfirmRequestDTO;
import com.demoproject.shoppingcart.dto.PaymentInitiateResponseDTO;
import com.demoproject.shoppingcart.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<PaymentInitiateResponseDTO> initiate(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(paymentService.initiatePayment(orderId));
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> confirm(
            @RequestBody PaymentConfirmRequestDTO request) {

        return ResponseEntity.ok(paymentService.confirmPayment(request));
    }
}

