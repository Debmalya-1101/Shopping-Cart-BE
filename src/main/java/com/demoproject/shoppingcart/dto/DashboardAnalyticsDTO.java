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
public class DashboardAnalyticsDTO {

    private Long totalUsers;
    private Long totalOrders;
    private Long totalRevenue;
    private List<OrderStatusCountDTO> ordersByStatus;
    private List<MonthlySalesDTO> monthlySalesGraph;
    private List<TopProductDTO> topSellingProducts;
}

