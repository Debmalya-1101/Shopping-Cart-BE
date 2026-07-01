package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMovementDTO {
    private Long productId;
    private String productName;
    private Long unitsConsumed; // Deprecated, kept for backward compatibility
    private Long netUnitsSold;
}
