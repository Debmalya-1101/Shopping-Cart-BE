package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AdminCancelOrderRequest;
import com.demoproject.shoppingcart.dto.AdminOrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderResponseDTO;
import com.demoproject.shoppingcart.dto.OrderItemReturnRequestDTO;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.service.AdminOrderService;
import com.demoproject.shoppingcart.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final OrderService orderService;

    public AdminOrderController(AdminOrderService adminOrderService, OrderService orderService) {
        this.adminOrderService = adminOrderService;
        this.orderService = orderService;
    }

    /**
     * GET /api/admin/orders?status=CONFIRMED&page=0&size=20
     * Returns paginated list of all orders, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<Page<AdminOrderResponseDTO>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(adminOrderService.getAllOrders(status, pageable));
    }

    /**
     * GET /api/admin/orders/{orderId}
     * Returns full details for a single order.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponseDTO> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrderById(orderId));
    }

    /**
     * POST /api/admin/orders/{orderId}/cancel
     * Admin cancels a CONFIRMED (paid) order with a mandatory reason.
     * - Sets order status to CANCELLED
     * - Flags PaymentStatus as SUCCESS_REQUIRES_REFUND
     * - Publishes OrderCancelledEvent (future listener sends apology email to user)
     *
     * Only allowed when order is in CONFIRMED state.
     * Body: { "reason": "We are unable to fulfil your order due to..." }
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<AdminOrderResponseDTO> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody AdminCancelOrderRequest request) {

        return ResponseEntity.ok(adminOrderService.cancelOrderByAdmin(orderId, request.getReason()));
    }

    /**
     * POST /api/admin/orders/{orderId}/items/return
     * Admin processes a return request for specific order items (future return workflow).
     * Currently allows marking items for return initiation.
     */
    @PostMapping("/{orderId}/items/return")
    public ResponseEntity<OrderResponseDTO> processReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemReturnRequestDTO request) {
        return ResponseEntity.ok(orderService.processReturn(orderId, request));
    }
}
