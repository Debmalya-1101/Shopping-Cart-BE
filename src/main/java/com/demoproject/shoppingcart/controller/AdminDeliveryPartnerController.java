package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AdminDeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerRatingSummaryDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerStatusUpdateRequest;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.DeliveryFeedbackService;
import com.demoproject.shoppingcart.service.DeliveryPartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController
@RequestMapping("/api/admin/delivery-partners")
public class AdminDeliveryPartnerController {

    private final DeliveryPartnerService deliveryPartnerService;
    private final UserRepository userRepository;
    private final DeliveryFeedbackService deliveryFeedbackService;

    public AdminDeliveryPartnerController(DeliveryPartnerService deliveryPartnerService, 
                                          UserRepository userRepository,
                                          DeliveryFeedbackService deliveryFeedbackService) {
        this.deliveryPartnerService = deliveryPartnerService;
        this.userRepository = userRepository;
        this.deliveryFeedbackService = deliveryFeedbackService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.demoproject.shoppingcart.dto.PageResponse<DeliveryPartnerResponseDTO>> getAllDeliveryPartners(
            @RequestParam(required = false) DeliveryPartnerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(deliveryPartnerService.getAllDeliveryPartners(status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getDeliveryPartnerById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(deliveryPartnerService.getDeliveryPartnerById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDeliveryPartnerStatus(
            @PathVariable Long id,
            @RequestBody DeliveryPartnerStatusUpdateRequest request,
            Authentication authentication) {
        
        try {
            String adminUsername = authentication.getName();
            AppUser adminUser = userRepository.findByUserName(adminUsername)
                    .orElseThrow(() -> new RuntimeException("Admin user not found"));

            DeliveryPartnerResponseDTO response = deliveryPartnerService.updateDeliveryPartnerStatus(
                    id, request, adminUser.getId(), adminUsername);
            
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}/feedback")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.demoproject.shoppingcart.dto.PageResponse<AdminDeliveryFeedbackResponseDTO>> getPartnerFeedback(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(deliveryFeedbackService.getAdminFeedbackForPartner(id, page, size));
    }

    @GetMapping("/ratings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryPartnerRatingSummaryDTO>> getAllPartnerRatings() {
        return ResponseEntity.ok(deliveryFeedbackService.getAllPartnerRatings());
    }
}
