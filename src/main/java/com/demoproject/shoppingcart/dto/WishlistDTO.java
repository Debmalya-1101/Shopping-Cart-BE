package com.demoproject.shoppingcart.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {
    private List<WishlistItemDTO> items;
}
