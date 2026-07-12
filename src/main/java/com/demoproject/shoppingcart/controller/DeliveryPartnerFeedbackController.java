package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.DeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerRatingSummaryDTO;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.DeliveryPartner;
import com.demoproject.shoppingcart.repository.DeliveryPartnerRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.DeliveryFeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;


@RestController
@RequestMapping("/api/delivery-partner/feedback")
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class DeliveryPartnerFeedbackController {

    private final DeliveryFeedbackService deliveryFeedbackService;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public DeliveryPartnerFeedbackController(DeliveryFeedbackService deliveryFeedbackService,
                                             UserRepository userRepository,
                                             DeliveryPartnerRepository deliveryPartnerRepository) {
        this.deliveryFeedbackService = deliveryFeedbackService;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

    private Long getLoggedInPartnerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DeliveryPartner partner = deliveryPartnerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Delivery Partner profile not found"));
        return partner.getId();
    }

    @GetMapping("/summary")
    public ResponseEntity<DeliveryPartnerRatingSummaryDTO> getRatingSummary() {
        Long partnerId = getLoggedInPartnerId();
        return ResponseEntity.ok(deliveryFeedbackService.getRatingSummaryForPartner(partnerId));
    }

    @GetMapping
    public ResponseEntity<com.demoproject.shoppingcart.dto.PageResponse<DeliveryFeedbackResponseDTO>> getFeedbackList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long partnerId = getLoggedInPartnerId();
        return ResponseEntity.ok(deliveryFeedbackService.getFeedbackForPartner(partnerId, page, size));
    }
}
