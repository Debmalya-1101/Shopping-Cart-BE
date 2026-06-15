package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.CheckoutRequestDTO;
import com.demoproject.shoppingcart.dto.OrderDetailDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemReturnRequestDTO;

import java.util.List;

public interface OrderService {

    OrderResponseDTO checkout(CheckoutRequestDTO request);

    List<OrderResponseDTO> getMyOrders();

    OrderDetailDTO getOrderById(Long orderId);

    OrderResponseDTO cancelOrder(Long orderId);

    OrderResponseDTO processReturn(Long orderId, OrderItemReturnRequestDTO request);
}
