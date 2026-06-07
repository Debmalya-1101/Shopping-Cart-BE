package com.demoproject.shoppingcart.event.producer;

import com.demoproject.shoppingcart.event.PaymentSuccessEvent;

/**
 * Producer interface for Payment events
 * Implementation will use Kafka KafkaTemplate to publish events
 *
 * TODO: Implement with Kafka producer when Kafka dependency is added
 */
public interface PaymentEventProducer {

    /**
     * Publish event when payment is successfully completed
     * Topic: payments.success
     */
    void sendPaymentSuccessEvent(PaymentSuccessEvent event);

    // Future events:
    // void sendPaymentFailedEvent(PaymentFailedEvent event);
    // void sendPaymentRefundedEvent(PaymentRefundedEvent event);
    // void sendPaymentRetryEvent(PaymentRetryEvent event);
}

