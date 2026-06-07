package com.demoproject.shoppingcart.event.producer;

import com.demoproject.shoppingcart.event.ProductStockUpdatedEvent;

/**
 * Producer interface for Product events
 * Implementation will use Kafka KafkaTemplate to publish events
 *
 * TODO: Implement with Kafka producer when Kafka dependency is added
 */
public interface ProductEventProducer {

    /**
     * Publish event when product stock is updated
     * Topic: products.stock.updated
     */
    void sendProductStockUpdatedEvent(ProductStockUpdatedEvent event);

    // Future events:
    // void sendProductCreatedEvent(ProductCreatedEvent event);
    // void sendProductUpdatedEvent(ProductUpdatedEvent event);
    // void sendProductDeletedEvent(ProductDeletedEvent event);
    // void sendHighDemandProductEvent(HighDemandProductEvent event);
}

