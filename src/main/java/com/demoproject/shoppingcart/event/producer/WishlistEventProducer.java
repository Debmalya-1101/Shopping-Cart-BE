package com.demoproject.shoppingcart.event.producer;

import com.demoproject.shoppingcart.event.WishlistAddedEvent;

/**
 * Producer interface for Wishlist events
 * Implementation will use Kafka KafkaTemplate to publish events
 *
 * TODO: Implement with Kafka producer when Kafka dependency is added
 */
public interface WishlistEventProducer {

    /**
     * Publish event when item is added to wishlist
     * Topic: wishlist.items.added
     */
    void sendWishlistAddedEvent(WishlistAddedEvent event);

    // Future events:
    // void sendWishlistRemovedEvent(WishlistRemovedEvent event);
    // void sendWishlistPriceDropEvent(WishlistPriceDropEvent event);
}

