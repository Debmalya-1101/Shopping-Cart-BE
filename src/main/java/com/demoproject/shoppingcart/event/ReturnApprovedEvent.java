package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when admin approves a return request.
 * NOTE: Future-ready shell. Add listeners when return workflow is implemented.
 * Use case: Notify customer of approval and next steps (drop-off/pickup info).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnApprovedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private LocalDateTime approvedAt;
}
