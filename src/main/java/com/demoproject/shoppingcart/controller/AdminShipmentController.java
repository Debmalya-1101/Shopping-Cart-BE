package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.ShipmentResponseDTO;
import com.demoproject.shoppingcart.service.ShipmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController
@RequestMapping("/api/admin/shipments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShipmentController {

    private final ShipmentService shipmentService;

    public AdminShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @GetMapping("/unassigned")
    public ResponseEntity<com.demoproject.shoppingcart.dto.PageResponse<ShipmentResponseDTO>> getUnassignedShipments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(shipmentService.getUnassignedShipments(page, size));
    }

    @PostMapping("/{shipmentId}/assign/{partnerId}")
    public ResponseEntity<ShipmentResponseDTO> assignDeliveryPartner(
            @PathVariable Long shipmentId,
            @PathVariable Long partnerId) {
        ShipmentResponseDTO response = shipmentService.assignDeliveryPartner(shipmentId, partnerId);
        return ResponseEntity.ok(response);
    }
}
