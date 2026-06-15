package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDTO {
    private Long id;
    private InventoryTransactionType transactionType;
    private String referenceType;
    private String referenceId;
    private Integer quantity;
    private String notes;
    private LocalDateTime createdAt;
}
