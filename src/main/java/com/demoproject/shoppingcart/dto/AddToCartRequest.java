package com.demoproject.shoppingcart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {
    private Long productId;
    private Long quantity; // default = 1
}
