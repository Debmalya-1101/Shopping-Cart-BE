package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.DeliveryFeedbackStatusDTO;
import com.demoproject.shoppingcart.service.DeliveryFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/delivery-feedback")
@PreAuthorize("hasRole('USER')")
public class DeliveryFeedbackStatusController {

    private final DeliveryFeedbackService deliveryFeedbackService;

    public DeliveryFeedbackStatusController(DeliveryFeedbackService deliveryFeedbackService) {
        this.deliveryFeedbackService = deliveryFeedbackService;
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<DeliveryFeedbackStatusDTO> getFeedbackStatus(@PathVariable Long orderId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(deliveryFeedbackService.getFeedbackStatus(orderId, username));
    }
}
