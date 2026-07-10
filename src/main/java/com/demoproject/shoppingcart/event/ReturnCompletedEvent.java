package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when a returned item is received at the warehouse and marked as RETURNED.
 * NOTE: Future-ready shell. Add listeners when return workflow is implemented.
 * Use case: Trigger refund processing and notify customer that refund is initiated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCompletedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private Long refundAmount;
    private LocalDateTime completedAt;
}
