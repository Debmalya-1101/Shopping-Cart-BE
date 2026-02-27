package com.demoproject.shoppingcart.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {
    private Long itemId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Long price;       // snapshot price
    private Long quantity;
    private Long total;       // price * quantity
}
