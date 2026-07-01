package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Enriched line-item DTO used inside {@link OrderDetailDTO}.
 * Carries product snapshot data plus a computed lineTotal.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailItemDTO {

    private Long productId;
    private String productName;
    private String productImageUrl;
    private String categoryName;   // optional – populated when category data is available
    private Long quantity;
    private Long price;            // snapshot price captured at checkout
    private Long lineTotal;        // price * quantity
}
