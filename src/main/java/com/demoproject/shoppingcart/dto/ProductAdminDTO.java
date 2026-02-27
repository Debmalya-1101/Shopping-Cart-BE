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
public class ProductAdminDTO {
    private Long id;
    private String name;
    private String description;
    private Long price;
    private Boolean active;
    private String brand;
    private Long categoryId;
    private List<AdminAttributeDTO> attributes;
    private List<String> imageUrls;
}

