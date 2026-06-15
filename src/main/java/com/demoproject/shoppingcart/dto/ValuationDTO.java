package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValuationDTO {
    private double totalAvailableValue;
    private double totalReservedValue;
    private double totalDamagedValue;
}
