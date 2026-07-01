package com.demoproject.shoppingcart.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartDTO {
    private List<CartItemDTO> items;
    private Long cartTotal;
}
