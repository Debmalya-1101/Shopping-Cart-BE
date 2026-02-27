package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductListDTO {
    private Long id;
    private String name;
    private Long price;
    private String imageUrl; // Thumbnail
    private Double rating;
    private Boolean active;
    private String brand;
    private String categoryName;
}

