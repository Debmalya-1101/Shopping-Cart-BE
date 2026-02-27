package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {
    private Long id;
    private String name;
    private String description;
    private Long price;
    private String imageUrl;
    private Double rating;
    private Boolean active;
    private String brand;
    private String categoryName;
    private List<String> imageGallery; // Multiple images
    private List<AttributeDTO> specifications; // Dynamic specs
}

