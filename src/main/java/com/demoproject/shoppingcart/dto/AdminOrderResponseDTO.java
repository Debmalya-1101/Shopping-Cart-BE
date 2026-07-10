package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminOrderResponseDTO {

    private Long orderId;
    private String userName;
    private String email;
    private String address;
    private Long phoneNo;

    private Long total;
    private String status;
    private String paymentStatus;
    private LocalDateTime createdAt;

    /** Populated when admin cancels a confirmed (paid) order. */
    private String adminCancelReason;

    private List<OrderItemDTO> items;
}
