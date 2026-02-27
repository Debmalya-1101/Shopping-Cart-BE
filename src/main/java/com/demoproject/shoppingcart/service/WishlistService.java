package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AddToWishlistRequest;
import com.demoproject.shoppingcart.dto.WishlistDTO;

public interface WishlistService {

    WishlistDTO getWishlist();

    WishlistDTO addToWishlist(AddToWishlistRequest request);

    WishlistDTO removeItem(Long itemId);

    WishlistDTO toggleWishlist(Long productId);
}
