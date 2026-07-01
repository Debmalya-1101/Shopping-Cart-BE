package com.demoproject.shoppingcart.event.producer.impl;

import com.demoproject.shoppingcart.event.OrderPlacedEvent;
import com.demoproject.shoppingcart.event.producer.OrderEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of OrderEventProducer
 *
 * Currently: Logs events to console/file
 * TODO: Replace with Kafka KafkaTemplate.send() when Kafka is integrated
 */
@Component
public class MockOrderEventProducer implements OrderEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MockOrderEventProducer.class);

    @Override
    public void sendOrderPlacedEvent(OrderPlacedEvent event) {
        // Mock: Log the event
        logger.info("📤 [MOCK EVENT] OrderPlacedEvent - Order ID: {}, User ID: {}, Amount: {}, Items: {}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount(), event.getItems().size());

        // TODO: Replace with actual Kafka producer
        /*
        try {
            kafkaTemplate.send("orders.placed", event);
            logger.info("✅ OrderPlacedEvent published to Kafka topic: orders.placed");
        } catch (Exception e) {
            logger.error("❌ Failed to publish OrderPlacedEvent", e);
        }
        */
    }
}

