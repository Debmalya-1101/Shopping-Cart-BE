package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AddressDTO;
import com.demoproject.shoppingcart.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")

public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * Get all saved addresses of the logged-in user
     * GET /api/addresses
     */
    @GetMapping
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        List<AddressDTO> addresses = addressService.getMyAddresses();
        return ResponseEntity.ok(addresses);
    }

    /**
     * Add a new saved address for the logged-in user
     * POST /api/addresses
     */
    @PostMapping
    public ResponseEntity<AddressDTO> addAddress(@Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO created = addressService.addAddress(addressDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing address
     * PUT /api/addresses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressDTO addressDTO) {
        AddressDTO updated = addressService.updateAddress(id, addressDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a saved address
     * DELETE /api/addresses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok("Address deleted successfully");
    }

    /**
     * Mark an address as the default address
     * PUT /api/addresses/{id}/default
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<AddressDTO> setDefaultAddress(@PathVariable Long id) {
        AddressDTO updated = addressService.setDefaultAddress(id);
        return ResponseEntity.ok(updated);
    }
}
