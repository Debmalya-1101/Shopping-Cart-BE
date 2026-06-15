package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.model.Inventory;
import com.demoproject.shoppingcart.model.InventoryTransaction;
import com.demoproject.shoppingcart.model.InventoryTransactionType;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.repository.InventoryRepository;
import com.demoproject.shoppingcart.repository.InventoryTransactionRepository;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demoproject.shoppingcart.dto.InventoryResponseDTO;
import com.demoproject.shoppingcart.dto.InventoryTransactionDTO;
import com.demoproject.shoppingcart.dto.InventoryAnalyticsDashboardDTO;
import com.demoproject.shoppingcart.dto.ProductMovementDTO;
import com.demoproject.shoppingcart.dto.TransactionSummaryDTO;
import com.demoproject.shoppingcart.dto.ValuationDTO;
import com.demoproject.shoppingcart.specification.InventorySpecifications;
import com.demoproject.shoppingcart.specification.InventoryTransactionSpecifications;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Inventory initializeInventory(Long productId, Integer initialStock) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (inventoryRepository.findByProductId(productId).isPresent()) {
            throw new IllegalStateException("Inventory already initialized for product ID: " + productId);
        }

        int stock = initialStock != null ? initialStock : 0;

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(stock);
        inventory.setReservedQuantity(0);

        inventory = inventoryRepository.saveAndFlush(inventory);

        // Record initialization transaction if stock > 0
        if (stock > 0) {
            recordTransaction(inventory, InventoryTransactionType.RESTOCK, stock, "INITIALIZATION", null, "Initial stock");
        }

        syncProductStock(product, inventory);
        return inventory;
    }

    @Override
    @Transactional
    public Inventory adjustStock(Long productId, Integer quantityDelta, String referenceType, String referenceId, String notes) {
        Inventory inventory = getInventory(productId);
        
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantityDelta);
        if (inventory.getAvailableQuantity() < 0) {
            throw new IllegalStateException("Cannot adjust stock below zero. Current: " + (inventory.getAvailableQuantity() - quantityDelta) + ", Delta: " + quantityDelta);
        }
        
        inventory = inventoryRepository.saveAndFlush(inventory);
        
        InventoryTransactionType type = quantityDelta >= 0 ? InventoryTransactionType.RESTOCK : InventoryTransactionType.ADJUSTMENT;
        recordTransaction(inventory, type, quantityDelta, referenceType, referenceId, notes);
        
        syncProductStock(inventory.getProduct(), inventory);
        return inventory;
    }

    @Override
    @Transactional
    public Inventory reserveStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be greater than zero");
        }
        
        Inventory inventory = getInventory(productId);
        
        if (inventory.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock to reserve. Available: " + inventory.getAvailableQuantity() + ", Requested: " + quantity);
        }
        
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventory = inventoryRepository.saveAndFlush(inventory);
        
        recordTransaction(inventory, InventoryTransactionType.RESERVE, quantity, referenceType, referenceId, notes);
        
        syncProductStock(inventory.getProduct(), inventory);
        return inventory;
    }

    @Override
    @Transactional
    public Inventory releaseStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be greater than zero");
        }
        
        Inventory inventory = getInventory(productId);
        
        if (inventory.getReservedQuantity() < quantity) {
            log.warn("Cannot release more than reserved for product ID {}. Reserved: {}, Requested release: {}. Adjusting release quantity.", 
                    productId, inventory.getReservedQuantity(), quantity);
            quantity = inventory.getReservedQuantity();
        }
        
        if (quantity > 0) {
            inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
            inventory = inventoryRepository.saveAndFlush(inventory);
            
            recordTransaction(inventory, InventoryTransactionType.RELEASE, quantity, referenceType, referenceId, notes);
            
            syncProductStock(inventory.getProduct(), inventory);
        }
        return inventory;
    }

    @Override
    @Transactional
    public Inventory consumeStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Consume quantity must be greater than zero");
        }
        
        Inventory inventory = getInventory(productId);
        
        if (inventory.getReservedQuantity() < quantity) {
            throw new IllegalStateException("Cannot consume more than reserved. Reserved: " + inventory.getReservedQuantity() + ", Requested consume: " + quantity);
        }
        
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        inventory = inventoryRepository.saveAndFlush(inventory);
        
        recordTransaction(inventory, InventoryTransactionType.CONSUME, quantity, referenceType, referenceId, notes);
        
        // Product stock doesn't change here since it was already deducted from available during RESERVE
        return inventory;
    }

    @Override
    @Transactional
    public Inventory cancelOrderStock(Long productId, Integer quantity, String referenceType, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Cancel quantity must be greater than zero");
        }
        
        Inventory inventory = getInventory(productId);
        
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
        inventory = inventoryRepository.saveAndFlush(inventory);
        
        recordTransaction(inventory, InventoryTransactionType.CANCEL_RESTOCK, quantity, referenceType, referenceId, notes);
        
        syncProductStock(inventory.getProduct(), inventory);
        return inventory;
    }

    @Override
    @Transactional
    public Inventory returnStock(Long productId, Integer quantity, String condition, String referenceType, String referenceId, String notes) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Return quantity must be greater than zero");
        }
        
        Inventory inventory = getInventory(productId);
        
        if ("DAMAGED".equalsIgnoreCase(condition)) {
            inventory.setDamagedQuantity(inventory.getDamagedQuantity() + quantity);
            inventory = inventoryRepository.saveAndFlush(inventory);
            recordTransaction(inventory, InventoryTransactionType.RETURN_DAMAGED, quantity, referenceType, referenceId, notes);
        } else {
            inventory.setAvailableQuantity(inventory.getAvailableQuantity() + quantity);
            inventory = inventoryRepository.saveAndFlush(inventory);
            recordTransaction(inventory, InventoryTransactionType.RETURN_RESTOCK, quantity, referenceType, referenceId, notes);
            syncProductStock(inventory.getProduct(), inventory);
        }
        
        return inventory;
    }
    
    private Inventory getInventory(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product ID: " + productId));
    }
    
    private void recordTransaction(Inventory inventory, InventoryTransactionType type, Integer quantity, String refType, String refId, String notes) {
        InventoryTransaction tx = new InventoryTransaction();
        tx.setInventory(inventory);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        tx.setReferenceType(refType);
        tx.setReferenceId(refId);
        tx.setNotes(notes);
        transactionRepository.save(tx);
    }
    
    private void syncProductStock(Product product, Inventory inventory) {
        product.setStock(inventory.getAvailableQuantity());
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponseDTO> getAllInventory(Long productId, String productName, Boolean lowStock, Boolean outOfStock, String sortBy, String order, int page, int size) {
        String sortField = mapInventorySortField(sortBy);
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Specification<Inventory> spec = InventorySpecifications.buildSpecification(productId, productName, lowStock, outOfStock);
        
        return inventoryRepository.findAll(spec, pageable).map(this::toInventoryResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDTO getInventoryByProductId(Long productId) {
        return toInventoryResponseDTO(getInventory(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionDTO> getTransactionHistory(Long inventoryId, InventoryTransactionType transactionType, String referenceType, String referenceId, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Specification<InventoryTransaction> spec = InventoryTransactionSpecifications.buildSpecification(
                inventoryId, transactionType, referenceType, referenceId, startDate, endDate);
        
        return transactionRepository.findAll(spec, pageable).map(this::toInventoryTransactionDTO);
    }

    @Override
    public InventoryAnalyticsDashboardDTO getAnalyticsDashboard(LocalDateTime startDate) {
        if (startDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
        }

        long lowStockCount = inventoryRepository.countLowStock();
        long outOfStockCount = inventoryRepository.countOutOfStock();

        Object[] valuationRaw = inventoryRepository.getValuation();
        ValuationDTO valuation = new ValuationDTO(0.0, 0.0, 0.0);
        if (valuationRaw != null && valuationRaw.length > 0) {
            Object[] row = (Object[]) valuationRaw[0];
            valuation.setTotalAvailableValue(row[0] != null ? ((Number) row[0]).doubleValue() : 0.0);
            valuation.setTotalReservedValue(row[1] != null ? ((Number) row[1]).doubleValue() : 0.0);
            valuation.setTotalDamagedValue(row[2] != null ? ((Number) row[2]).doubleValue() : 0.0);
        }

        java.util.List<ProductMovementDTO> fastMoving = transactionRepository.getFastMovingProducts(startDate, PageRequest.of(0, 10))
                .stream().map(row -> new ProductMovementDTO(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).longValue(), ((Number) row[2]).longValue()))
                .toList();

        java.util.List<ProductMovementDTO> slowMoving = inventoryRepository.getSlowMovingProducts(startDate, PageRequest.of(0, 10))
                .stream().map(row -> new ProductMovementDTO(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).longValue(), ((Number) row[2]).longValue()))
                .toList();

        java.util.List<TransactionSummaryDTO> summaries = transactionRepository.getTransactionSummaries(startDate)
                .stream().map(row -> new TransactionSummaryDTO(((InventoryTransactionType) row[0]).name(), ((Number) row[1]).longValue()))
                .toList();

        return new InventoryAnalyticsDashboardDTO(
                valuation,
                lowStockCount,
                outOfStockCount,
                fastMoving,
                slowMoving,
                summaries
        );
    }

    private InventoryResponseDTO toInventoryResponseDTO(Inventory inventory) {
        return new InventoryResponseDTO(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getProduct().getName(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(),
                inventory.getReorderLevel(),
                inventory.getAvailableQuantity() + inventory.getReservedQuantity(),
                inventory.getVersion(),
                inventory.getUpdatedAt()
        );
    }

    private InventoryTransactionDTO toInventoryTransactionDTO(InventoryTransaction tx) {
        return new InventoryTransactionDTO(
                tx.getId(),
                tx.getTransactionType(),
                tx.getReferenceType(),
                tx.getReferenceId(),
                tx.getQuantity(),
                tx.getNotes(),
                tx.getCreatedAt()
        );
    }

    private String mapInventorySortField(String sortBy) {
        if ("availableQuantity".equalsIgnoreCase(sortBy)) return "availableQuantity";
        if ("reservedQuantity".equalsIgnoreCase(sortBy)) return "reservedQuantity";
        if ("reorderLevel".equalsIgnoreCase(sortBy)) return "reorderLevel";
        return "updatedAt";
    }
}
