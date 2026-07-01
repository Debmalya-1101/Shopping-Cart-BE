package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private Long orderId;
    private Long total;
    private String status;
    private String paymentStatus;
    private String deliveryStatus;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
