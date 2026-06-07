package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AdminOrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemDTO;
import com.demoproject.shoppingcart.model.Order;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.service.AdminOrderService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {
    private final OrderRepository orderRepository;

    public AdminOrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Page<AdminOrderResponseDTO> getAllOrders(
            OrderStatus status,
            Pageable pageable) {

        Page<Order> orders;

        if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(this::convertToAdminDTO);
    }


    @Override
    public AdminOrderResponseDTO getOrderById(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return convertToAdminDTO(order);
    }

    @Override
    @Transactional
    public AdminOrderResponseDTO updateOrderStatus(Long orderId, OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        OrderStatus current = order.getStatus();

        // 🚫 Final states
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order status cannot be changed");
        }

        // 🚫 Invalid transitions
        if (current == OrderStatus.PLACED && newStatus == OrderStatus.DELIVERED) {
            throw new RuntimeException("Order must be shipped before delivery");
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        // 🔮 Kafka later
        // orderEventProducer.sendOrderStatusUpdatedEvent(saved);

        return convertToAdminDTO(saved);
    }


    private AdminOrderResponseDTO convertToAdminDTO(Order order) {

        List<OrderItemDTO> items = order.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getProduct().getImageUrl(),
                        i.getPrice(),
                        i.getQuantity(),
                        i.getPrice() * i.getQuantity()
                )).toList();

        return new AdminOrderResponseDTO(
                order.getId(),
                order.getUser().getUserName(),
                order.getEmail(),
                order.getAddress(),
                order.getPhoneNo(),
                order.getTotal(),
                order.getStatus().name(),
                order.getCreatedAt(),
                items
        );
    }

}
