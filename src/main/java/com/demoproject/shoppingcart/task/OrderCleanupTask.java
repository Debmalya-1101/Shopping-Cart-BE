package com.demoproject.shoppingcart.task;

import com.demoproject.shoppingcart.model.Order;
import com.demoproject.shoppingcart.model.OrderItem;
import com.demoproject.shoppingcart.model.OrderStatus;
import com.demoproject.shoppingcart.model.PaymentStatus;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that auto-cancels stale PENDING_PAYMENT orders.
 *
 * An order in PENDING_PAYMENT state represents a checkout where the user has
 * not yet completed payment. If no payment is received within the 30-minute
 * window, the reserved stock is released and the order is marked CANCELLED
 * with PaymentStatus.FAILED.
 *
 * Runs every minute. Only targets orders in PENDING_PAYMENT state (payment
 * not yet initiated or failed after retry — new state machine behaviour).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupTask {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    private static final int PAYMENT_WINDOW_MINUTES = 30;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredOrderReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(PAYMENT_WINDOW_MINUTES);

        // Find all PENDING_PAYMENT orders created more than 30 minutes ago.
        // Using createdAt (not updatedAt) so retry attempts don't reset the window.
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING_PAYMENT,
                cutoff
        );

        if (!expiredOrders.isEmpty()) {
            log.info("[OrderCleanupTask] Found {} expired PENDING_PAYMENT orders to auto-cancel.",
                    expiredOrders.size());

            for (Order order : expiredOrders) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);

                log.info("[OrderCleanupTask] Auto-cancelled Order ID: {} — payment window expired after {} min.",
                        order.getId(), PAYMENT_WINDOW_MINUTES);

                // Release reserved stock back to available inventory
                for (OrderItem item : order.getItems()) {
                    try {
                        inventoryService.releaseStock(
                                item.getProduct().getId(),
                                item.getQuantity().intValue(),
                                "ORDER",
                                order.getId().toString(),
                                "Reservation timeout — order auto-cancelled after " + PAYMENT_WINDOW_MINUTES + " min"
                        );
                    } catch (Exception e) {
                        log.error("[OrderCleanupTask] Failed to release stock for Order ID: {}, Product ID: {}. " +
                                  "Manual reconciliation may be required.",
                                order.getId(), item.getProduct().getId(), e);
                    }
                }
            }
        }
    }
}
