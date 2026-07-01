package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event fired when an order is placed successfully
 * Use case: Sending confirmation email, updating analytics, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private Long totalAmount;
    private LocalDateTime orderedAt;
    private List<OrderItemEvent> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEvent {
        private Long productId;
        private String productName;
        private Long quantity;
        private Long price;
    }
}

