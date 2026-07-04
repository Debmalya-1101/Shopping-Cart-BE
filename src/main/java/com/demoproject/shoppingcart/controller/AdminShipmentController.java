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
    public ResponseEntity<List<ShipmentResponseDTO>> getUnassignedShipments() {
        return ResponseEntity.ok(shipmentService.getUnassignedShipments());
    }

    @PostMapping("/{shipmentId}/assign/{partnerId}")
    public ResponseEntity<ShipmentResponseDTO> assignDeliveryPartner(
            @PathVariable Long shipmentId,
            @PathVariable Long partnerId) {
        ShipmentResponseDTO response = shipmentService.assignDeliveryPartner(shipmentId, partnerId);
        return ResponseEntity.ok(response);
    }
}
