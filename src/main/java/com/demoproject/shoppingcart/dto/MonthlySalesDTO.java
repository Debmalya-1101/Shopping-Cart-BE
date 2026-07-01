package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySalesDTO {
    private Integer month;
    private Long totalSales;
    private Long totalOrders;
    private Long totalRevenue;
}

