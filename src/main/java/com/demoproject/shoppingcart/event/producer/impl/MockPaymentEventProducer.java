package com.demoproject.shoppingcart.event.producer.impl;

import com.demoproject.shoppingcart.event.PaymentSuccessEvent;
import com.demoproject.shoppingcart.event.producer.PaymentEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of PaymentEventProducer
 *
 * Currently: Logs events to console/file
 * TODO: Replace with Kafka KafkaTemplate.send() when Kafka is integrated
 */
@Component
public class MockPaymentEventProducer implements PaymentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(MockPaymentEventProducer.class);

    @Override
    public void sendPaymentSuccessEvent(PaymentSuccessEvent event) {
        // Mock: Log the event
        logger.info("📤 [MOCK EVENT] PaymentSuccessEvent - Order ID: {}, User ID: {}, Amount: {}, Ref: {}",
                event.getOrderId(), event.getUserId(), event.getAmount(), event.getPaymentReferenceId());

        // TODO: Replace with actual Kafka producer
        /*
        try {
            kafkaTemplate.send("payments.success", event);
            logger.info("✅ PaymentSuccessEvent published to Kafka topic: payments.success");
        } catch (Exception e) {
            logger.error("❌ Failed to publish PaymentSuccessEvent", e);
        }
        */
    }
}

