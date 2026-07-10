package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when admin rejects a return request.
 * NOTE: Future-ready shell. Add listeners when return workflow is implemented.
 * Use case: Notify customer of rejection with reason.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRejectedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private String rejectionReason;
    private LocalDateTime rejectedAt;
}
