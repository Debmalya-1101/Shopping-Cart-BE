package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when product stock is updated
 * Use case: Real-time inventory sync, alerting low stock, analytics, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockUpdatedEvent {
    private Long productId;
    private String productName;
    private Integer previousStock;
    private Integer currentStock;
    private String reason; // "PURCHASE", "MANUAL_UPDATE", "RETURN", etc.
    private LocalDateTime updatedAt;
    private Boolean isLowStock; // Flag if stock is below threshold
}

