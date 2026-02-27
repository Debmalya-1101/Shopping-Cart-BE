package com.demoproject.shoppingcart.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {
    private Long itemId;
    private Long productId;
    private String productName;
    private String imageUrl;
    private Long price;       // current product price
    private Double rating;
}
