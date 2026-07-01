package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.model.Inventory;

public interface InventoryService {
    Inventory initializeInventory(Long productId, Integer initialStock);
    
    Inventory adjustStock(Long productId, Integer quantityDelta, String referenceType, String referenceId, String notes);
    
    Inventory reserveStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes);
    
    Inventory releaseStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes);
    
    Inventory consumeStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes);

    Inventory cancelOrderStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes);

    Inventory returnStock(Long productId, Integer quantity, String condition, String referenceType, String referenceId, String notes);

    com.demoproject.shoppingcart.dto.InventoryAnalyticsDashboardDTO getAnalyticsDashboard(java.time.LocalDateTime startDate);

    org.springframework.data.domain.Page<com.demoproject.shoppingcart.dto.InventoryResponseDTO> getAllInventory(
            Long productId, String productName, Boolean lowStock, Boolean outOfStock,
            String sortBy, String order, int page, int size);

    com.demoproject.shoppingcart.dto.InventoryResponseDTO getInventoryByProductId(Long productId);

    org.springframework.data.domain.Page<com.demoproject.shoppingcart.dto.InventoryTransactionDTO> getTransactionHistory(
            Long inventoryId,
            com.demoproject.shoppingcart.model.InventoryTransactionType transactionType,
            String referenceType, String referenceId,
            java.time.LocalDateTime startDate, java.time.LocalDateTime endDate,
            int page, int size);
}
