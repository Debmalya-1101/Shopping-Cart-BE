package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Long price;
    private Long quantity;
    private Long total;

    public OrderItemDTO(Long productId, String productName, String productImageUrl,
                        Long price, Long quantity, Long total) {
        this.productId = productId;
        this.productName = productName;
        this.productImageUrl = productImageUrl;
        this.price = price;
        this.quantity = quantity;
        this.total = total;
    }
}

