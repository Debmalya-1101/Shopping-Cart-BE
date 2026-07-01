package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAnalyticsDashboardDTO {
    private ValuationDTO valuation;
    private long lowStockCount;
    private long outOfStockCount;
    private List<ProductMovementDTO> fastMovingProducts;
    private List<ProductMovementDTO> slowMovingProducts;
    private List<TransactionSummaryDTO> transactionSummaries;
}
