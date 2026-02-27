package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AddToWishlistRequest;
import com.demoproject.shoppingcart.dto.WishlistDTO;
import com.demoproject.shoppingcart.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "*")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<WishlistDTO> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist());
    }

    @PostMapping("/add")
    public ResponseEntity<WishlistDTO> addToWishlist(@RequestBody AddToWishlistRequest request) {
        return ResponseEntity.ok(wishlistService.addToWishlist(request));
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<WishlistDTO> removeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(wishlistService.removeItem(itemId));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<WishlistDTO> toggleWishlist(@PathVariable Long productId) {
        return ResponseEntity.ok(wishlistService.toggleWishlist(productId));
    }

}
