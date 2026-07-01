package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminAttributeKeyDTO {
    private Long id;
    private String keyName;  // e.g. "RAM", "ROM", "Camera"
    private String type;     // "TEXT" or "NUMBER"
    private Long categoryId;
    private String categoryName;
}
