package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AdminOrderResponseDTO;
import com.demoproject.shoppingcart.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminOrderService {

    Page<AdminOrderResponseDTO> getAllOrders(OrderStatus status, Pageable pageable);

    AdminOrderResponseDTO getOrderById(Long orderId);

    AdminOrderResponseDTO updateOrderStatus(Long orderId, OrderStatus status);
}

