package com.demoproject.shoppingcart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemReturnRequestDTO {

    @NotNull(message = "Items list cannot be null")
    private List<ReturnItemDTO> items;

    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDTO {
        @NotNull(message = "Order item ID cannot be null")
        private Long orderItemId;

        @NotNull(message = "Quantity cannot be null")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @NotBlank(message = "Condition is required (GOOD or DAMAGED)")
        private String condition;
    }
}
