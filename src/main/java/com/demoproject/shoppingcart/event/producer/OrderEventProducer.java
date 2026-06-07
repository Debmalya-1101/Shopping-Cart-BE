package com.demoproject.shoppingcart.event.producer;

import com.demoproject.shoppingcart.event.OrderPlacedEvent;

/**
 * Producer interface for Order events
 * Implementation will use Kafka KafkaTemplate to publish events
 *
 * TODO: Implement with Kafka producer when Kafka dependency is added
 */
public interface OrderEventProducer {

    /**
     * Publish event when order is successfully placed
     * Topic: orders.placed
     */
    void sendOrderPlacedEvent(OrderPlacedEvent event);

    // Future events:
    // void sendOrderCancelledEvent(OrderCancelledEvent event);
    // void sendOrderShippedEvent(OrderShippedEvent event);
    // void sendOrderDeliveredEvent(OrderDeliveredEvent event);
}

