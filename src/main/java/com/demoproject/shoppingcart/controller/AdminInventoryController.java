package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.InventoryAdjustmentRequest;
import com.demoproject.shoppingcart.dto.InventoryResponseDTO;
import com.demoproject.shoppingcart.dto.InventoryTransactionDTO;
import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.model.InventoryTransactionType;
import com.demoproject.shoppingcart.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryResponseDTO>> getAllInventory(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Boolean outOfStock,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<InventoryResponseDTO> inventoryPage = inventoryService.getAllInventory(
                productId, productName, lowStock, outOfStock, sortBy, order, page, size);
        return ResponseEntity.ok(new PageResponse<>(inventoryPage));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryResponseDTO> getInventoryByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/{inventoryId}/transactions")
    public ResponseEntity<PageResponse<InventoryTransactionDTO>> getTransactionHistory(
            @PathVariable Long inventoryId,
            @RequestParam(required = false) InventoryTransactionType transactionType,
            @RequestParam(required = false) String referenceType,
            @RequestParam(required = false) String referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<InventoryTransactionDTO> transactions = inventoryService.getTransactionHistory(
                inventoryId, transactionType, referenceType, referenceId, startDate, endDate, page, size);
        return ResponseEntity.ok(new PageResponse<>(transactions));
    }

    @PostMapping("/product/{productId}/adjust")
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ResponseEntity<InventoryResponseDTO> adjustInventory(
            @PathVariable Long productId,
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        
        inventoryService.adjustStock(
                productId,
                request.getQuantityDelta(),
                request.getReferenceType(),
                request.getReferenceId(),
                request.getNotes()
        );
        
        return ResponseEntity.ok(inventoryService.getInventoryByProductId(productId));
    }

    @GetMapping("/analytics")
    public ResponseEntity<com.demoproject.shoppingcart.dto.InventoryAnalyticsDashboardDTO> getAnalytics(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate) {
        return ResponseEntity.ok(inventoryService.getAnalyticsDashboard(startDate));
    }
}
