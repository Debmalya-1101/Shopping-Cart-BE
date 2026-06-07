package com.demoproject.shoppingcart.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Event fired when an item is added to wishlist
 * Use case: Personalized recommendations, price drop alerts, marketing campaigns, etc.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WishlistAddedEvent {
    private Long wishlistItemId;
    private Long userId;
    private Long productId;
    private String productName;
    private Long productPrice;
    private LocalDateTime addedAt;
}

