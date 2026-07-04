package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.DeliveryFeedbackRequestDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.service.DeliveryFeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/orders/{orderId}/delivery-feedback")
@PreAuthorize("hasRole('USER')")
public class DeliveryFeedbackController {

    private final DeliveryFeedbackService deliveryFeedbackService;

    public DeliveryFeedbackController(DeliveryFeedbackService deliveryFeedbackService) {
        this.deliveryFeedbackService = deliveryFeedbackService;
    }

    @PostMapping
    public ResponseEntity<DeliveryFeedbackResponseDTO> submitFeedback(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryFeedbackRequestDTO request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(deliveryFeedbackService.submitFeedback(orderId, username, request));
    }
}

