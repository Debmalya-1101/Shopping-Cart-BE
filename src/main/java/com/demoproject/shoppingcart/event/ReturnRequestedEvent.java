package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event fired when a customer requests a return for one or more order items.
 * NOTE: The full return workflow is planned for a future iteration.
 *       This class is a future-ready shell — add listeners when the return flow is built.
 * Use case: Notify admin to review the return request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestedEvent {
    private Long orderId;
    private Long userId;
    private String userEmail;
    private List<ReturnItemDetail> items;
    private LocalDateTime requestedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDetail {
        private Long orderItemId;
        private String productName;
        private Long quantity;
    }
}
