package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.*;

public interface CartService {

    CartDTO getCart();

    CartDTO addToCart(AddToCartRequest request);

    CartDTO updateItem(Long itemId, Long quantity);

    CartDTO removeItem(Long itemId);

    void clearCart();
}
