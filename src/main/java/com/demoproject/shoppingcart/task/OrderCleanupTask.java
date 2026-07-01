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

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupTask {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    /**
     * Runs every minute to find orders that were placed but never paid for within 30 minutes.
     * Cancels the order and releases the reserved stock back to available stock.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredOrderReservations() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        List<Order> expiredOrders = orderRepository.findByStatusAndPaymentStatusAndUpdatedAtBefore(
                OrderStatus.PLACED,
                PaymentStatus.INITIATED,
                thirtyMinutesAgo
        );

        if (!expiredOrders.isEmpty()) {
            log.info("Found {} expired pending orders to clean up.", expiredOrders.size());

            for (Order order : expiredOrders) {
                // Cancel the order
                order.setStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                orderRepository.save(order);

                log.info("Cancelled Order ID: {} due to payment timeout", order.getId());

                // Release the reserved stock
                for (OrderItem item : order.getItems()) {
                    try {
                        inventoryService.releaseStock(
                                item.getProduct().getId(),
                                item.getQuantity().intValue(),
                                "ORDER",
                                order.getId().toString(),
                                "Reservation timeout - Order Cancelled"
                        );
                    } catch (Exception e) {
                        log.error("Failed to release stock for Order ID: {}, Product ID: {}", order.getId(), item.getProduct().getId(), e);
                    }
                }
            }
        }
    }
}
