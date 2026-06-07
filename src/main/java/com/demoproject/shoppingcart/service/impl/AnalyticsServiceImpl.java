package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.DashboardAnalyticsDTO;
import com.demoproject.shoppingcart.dto.OrderStatusCountDTO;
import com.demoproject.shoppingcart.dto.MonthlySalesDTO;
import com.demoproject.shoppingcart.dto.TopProductDTO;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.model.Role;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.AnalyticsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AnalyticsServiceImpl(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @Override
    public DashboardAnalyticsDTO getDashboardAnalytics() {
        DashboardAnalyticsDTO analytics = new DashboardAnalyticsDTO();

        // Total users (excluding admins)
        Long totalUsers = userRepository.countByRoleNot(Role.ROLE_ADMIN);
        analytics.setTotalUsers(totalUsers);

        // Total orders with successful payments
        Long totalOrders = orderRepository.countSuccessfulOrders();
        analytics.setTotalOrders(totalOrders);

        // Total revenue
        Long totalRevenue = orderRepository.totalRevenue();
        analytics.setTotalRevenue(totalRevenue);

        // Orders by status
        List<OrderStatusCountDTO> ordersByStatus = getOrdersByStatus();
        analytics.setOrdersByStatus(ordersByStatus);

        // Monthly sales graph
        List<MonthlySalesDTO> monthlySales = getMonthlySalesData();
        analytics.setMonthlySalesGraph(monthlySales);

        // Top selling products
        List<TopProductDTO> topProducts = getTopSellingProducts();
        analytics.setTopSellingProducts(topProducts);

        return analytics;
    }

    private List<OrderStatusCountDTO> getOrdersByStatus() {
        List<OrderStatusCountDTO> result = new ArrayList<>();
        List<Object[]> data = orderRepository.countOrdersByStatus();

        for (Object[] row : data) {
            String status = ((OrderStatus) row[0]).name();
            Long count = ((Number) row[1]).longValue();
            result.add(new OrderStatusCountDTO(status, count));
        }

        return result;
    }

    private List<MonthlySalesDTO> getMonthlySalesData() {
        List<MonthlySalesDTO> result = new ArrayList<>();
        List<Object[]> data = orderRepository.getMonthlySales();

        for (Object[] row : data) {
            Integer month = ((Number) row[0]).intValue();
            Long totalOrders = ((Number) row[1]).longValue();
            Long totalRevenue = ((Number) row[2]) != null ? ((Number) row[2]).longValue() : 0L;

            MonthlySalesDTO monthly = new MonthlySalesDTO(month, totalOrders, totalOrders, totalRevenue);
            result.add(monthly);
        }

        return result;
    }

    private List<TopProductDTO> getTopSellingProducts() {
        List<TopProductDTO> result = new ArrayList<>();
        List<Object[]> data = orderRepository.getTopSellingProducts();

        // Limit to top 10 products
        for (int i = 0; i < Math.min(data.size(), 10); i++) {
            Object[] row = data.get(i);
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            Long unitsSold = ((Number) row[2]).longValue();
            Long totalRevenue = ((Number) row[3]).longValue();
            Double rating = (Double) row[4];

            TopProductDTO top = new TopProductDTO(productId, productName, unitsSold, totalRevenue, rating);
            result.add(top);
        }

        return result;
    }
}

