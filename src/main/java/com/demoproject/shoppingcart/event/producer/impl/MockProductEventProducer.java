package com.demoproject.shoppingcart.event.producer.impl;

import com.demoproject.shoppingcart.event.ProductStockUpdatedEvent;
import com.demoproject.shoppingcart.event.producer.ProductEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of ProductEventProducer
 *
 * Currently: Logs events to console/file
 * TODO: Replace with Kafka KafkaTemplate.send() when Kafka is integrated
 */
@Component
public class MockProductEventProducer implements ProductEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MockProductEventProducer.class);

    @Override
    public void sendProductStockUpdatedEvent(ProductStockUpdatedEvent event) {
        // Mock: Log the event
        logger.info("📤 [MOCK EVENT] ProductStockUpdatedEvent - Product ID: {}, Name: {}, Previous: {}, Current: {}, Reason: {}",
                event.getProductId(), event.getProductName(), event.getPreviousStock(),
                event.getCurrentStock(), event.getReason());

        if (event.getIsLowStock()) {
            logger.warn("⚠️  LOW STOCK ALERT: Product {} has only {} units left",
                    event.getProductName(), event.getCurrentStock());
        }

        // TODO: Replace with actual Kafka producer
        /*
        try {
            kafkaTemplate.send("products.stock.updated", event);
            logger.info("✅ ProductStockUpdatedEvent published to Kafka topic: products.stock.updated");
        } catch (Exception e) {
            logger.error("❌ Failed to publish ProductStockUpdatedEvent", e);
        }
        */
    }
}

