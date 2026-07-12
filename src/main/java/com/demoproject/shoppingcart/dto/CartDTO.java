package com.demoproject.shoppingcart.dto;

import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartDTO {
    private List<CartItemDTO> items;
    private Long cartTotal; // kept for backward compatibility if needed, but we'll add others
    private Long subTotal;
    private Long tax;
    private Long shippingFee;
    private Long platformFee;
    private Long grandTotal;
}
