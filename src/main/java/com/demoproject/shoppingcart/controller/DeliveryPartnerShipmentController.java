package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.DeliveryPartnerDashboardDTO;
import com.demoproject.shoppingcart.dto.ShipmentResponseDTO;
import com.demoproject.shoppingcart.dto.ShipmentStatusUpdateRequest;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.DeliveryPartner;
import com.demoproject.shoppingcart.repository.DeliveryPartnerRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/delivery-partner/shipments")
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class DeliveryPartnerShipmentController {

    private final ShipmentService shipmentService;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public DeliveryPartnerShipmentController(ShipmentService shipmentService,
                                             UserRepository userRepository,
                                             DeliveryPartnerRepository deliveryPartnerRepository) {
        this.shipmentService = shipmentService;
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

    @GetMapping("/dashboard")
    public ResponseEntity<DeliveryPartnerDashboardDTO> getDashboard() {
        Long partnerId = getLoggedInPartnerId();
        return ResponseEntity.ok(shipmentService.getDashboardMetricsForPartner(partnerId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ShipmentResponseDTO>> getActiveShipments() {
        Long partnerId = getLoggedInPartnerId();
        return ResponseEntity.ok(shipmentService.getActiveShipmentsForPartner(partnerId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ShipmentResponseDTO>> getShipmentHistory() {
        Long partnerId = getLoggedInPartnerId();
        return ResponseEntity.ok(shipmentService.getShipmentHistoryForPartner(partnerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponseDTO> getShipmentDetails(@PathVariable Long id) {
        Long partnerId = getLoggedInPartnerId();
        return ResponseEntity.ok(shipmentService.getShipmentDetailsForPartner(id, partnerId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ShipmentResponseDTO> updateShipmentStatus(
            @PathVariable Long id,
            @RequestBody ShipmentStatusUpdateRequest request) {
        Long partnerId = getLoggedInPartnerId();
        ShipmentResponseDTO response = shipmentService.updateShipmentStatusByPartner(
                id, partnerId, request.getStatus(), request.getFailureReason());
        return ResponseEntity.ok(response);
    }
}
