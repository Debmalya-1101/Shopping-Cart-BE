package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AdminOrderResponseDTO;
import com.demoproject.shoppingcart.dto.UpdateOrderStatusRequest;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.service.AdminOrderService;
import com.demoproject.shoppingcart.service.OrderService;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemReturnRequestDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "*")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final OrderService orderService;

    public AdminOrderController(AdminOrderService adminOrderService, OrderService orderService) {
        this.adminOrderService = adminOrderService;
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminOrderResponseDTO>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                adminOrderService.getAllOrders(status, pageable)
        );
    }


    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponseDTO> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderResponseDTO> updateStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request) {

        return ResponseEntity.ok(
                adminOrderService.updateOrderStatus(orderId, request.getStatus())
        );
    }

    @PostMapping("/{orderId}/items/return")
    public ResponseEntity<OrderResponseDTO> processReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemReturnRequestDTO request) {
        return ResponseEntity.ok(orderService.processReturn(orderId, request));
    }
}

