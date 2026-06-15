package com.demoproject.shoppingcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryAdjustmentRequest {

    @NotNull(message = "quantityDelta cannot be null")
    private Integer quantityDelta;

    private String referenceType = "MANUAL_ADJUSTMENT";

    private String referenceId;

    @NotBlank(message = "Notes are required for manual adjustments")
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
