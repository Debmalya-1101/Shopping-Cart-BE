package com.demoproject.shoppingcart.event.producer.impl;

import com.demoproject.shoppingcart.event.WishlistAddedEvent;
import com.demoproject.shoppingcart.event.producer.WishlistEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of WishlistEventProducer
 *
 * Currently: Logs events to console/file
 * TODO: Replace with Kafka KafkaTemplate.send() when Kafka is integrated
 */
@Component
public class MockWishlistEventProducer implements WishlistEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MockWishlistEventProducer.class);

    @Override
    public void sendWishlistAddedEvent(WishlistAddedEvent event) {
        // Mock: Log the event
        logger.info("📤 [MOCK EVENT] WishlistAddedEvent - User ID: {}, Product: {}, Price: {}, Added At: {}",
                event.getUserId(), event.getProductName(), event.getProductPrice(), event.getAddedAt());

        // TODO: Replace with actual Kafka producer
        /*
        try {
            kafkaTemplate.send("wishlist.items.added", event);
            logger.info("✅ WishlistAddedEvent published to Kafka topic: wishlist.items.added");
        } catch (Exception e) {
            logger.error("❌ Failed to publish WishlistAddedEvent", e);
        }
        */
    }
}

