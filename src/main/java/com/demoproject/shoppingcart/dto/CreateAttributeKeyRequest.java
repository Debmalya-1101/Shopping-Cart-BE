package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttributeKeyRequest {

    @NotBlank(message = "Key name is required")
    @Size(min = 1, max = 50, message = "Key name must be between 1 and 50 characters")
    private String keyName;

    @NotNull(message = "Type is required (TEXT or NUMBER)")
    private AttributeType type;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    private Long categoryId;
}
