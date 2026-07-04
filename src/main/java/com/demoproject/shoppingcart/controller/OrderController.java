package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.CheckoutRequestDTO;
import com.demoproject.shoppingcart.dto.OrderDetailDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")

public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(
            @RequestBody CheckoutRequestDTO request) {
        return ResponseEntity.ok(orderService.checkout(request));
    }

    @GetMapping
    public ResponseEntity<com.demoproject.shoppingcart.dto.PageResponse<OrderResponseDTO>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getMyOrders(page, size));
    }

    /**
     * Returns full order details including shipping snapshot, enriched line items,
     * payment status, and order summary. Only the owning user may access this endpoint.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDTO> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }
}
