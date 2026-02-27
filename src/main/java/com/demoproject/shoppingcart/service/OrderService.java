package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.CheckoutRequestDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO checkout(CheckoutRequestDTO request);

    List<OrderResponseDTO> getMyOrders();

    OrderResponseDTO getOrderById(Long orderId);
}
