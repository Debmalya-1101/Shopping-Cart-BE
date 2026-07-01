package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AdminAttributeKeyDTO;
import com.demoproject.shoppingcart.dto.CreateAttributeKeyRequest;
import com.demoproject.shoppingcart.service.AdminAttributeKeyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/attribute-keys")
@CrossOrigin(origins = "*")
public class AdminAttributeKeyController {

    private final AdminAttributeKeyService adminAttributeKeyService;

    public AdminAttributeKeyController(AdminAttributeKeyService adminAttributeKeyService) {
        this.adminAttributeKeyService = adminAttributeKeyService;
    }

    /**
     * List all attribute keys, optionally filtered by category.
     * GET /api/admin/attribute-keys
     * GET /api/admin/attribute-keys?categoryId=1
     */
    @GetMapping
    public ResponseEntity<List<AdminAttributeKeyDTO>> getAllAttributeKeys(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(adminAttributeKeyService.getAllAttributeKeys(categoryId));
    }

    /**
     * Get a single attribute key by ID.
     * GET /api/admin/attribute-keys/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminAttributeKeyDTO> getAttributeKeyById(@PathVariable Long id) {
        return ResponseEntity.ok(adminAttributeKeyService.getAttributeKeyById(id));
    }

    /**
     * Create a new attribute key (e.g., RAM, ROM, Camera) under a category.
     * POST /api/admin/attribute-keys
     */
    @PostMapping
    public ResponseEntity<AdminAttributeKeyDTO> createAttributeKey(
            @Valid @RequestBody CreateAttributeKeyRequest request) {
        AdminAttributeKeyDTO created = adminAttributeKeyService.createAttributeKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing attribute key.
     * PUT /api/admin/attribute-keys/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdminAttributeKeyDTO> updateAttributeKey(
            @PathVariable Long id,
            @Valid @RequestBody CreateAttributeKeyRequest request) {
        return ResponseEntity.ok(adminAttributeKeyService.updateAttributeKey(id, request));
    }

    /**
     * Delete an attribute key permanently.
     * Fails with 400 if any product currently uses this key.
     * DELETE /api/admin/attribute-keys/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAttributeKey(@PathVariable Long id) {
        adminAttributeKeyService.deleteAttributeKey(id);
        return ResponseEntity.ok("Attribute key deleted successfully");
    }
}
