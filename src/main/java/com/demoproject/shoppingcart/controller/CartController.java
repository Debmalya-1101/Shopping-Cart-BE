package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.*;
import com.demoproject.shoppingcart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }

    @PostMapping("/add")
    public ResponseEntity<CartDTO> addToCart(@RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addToCart(request));
    }

    @PutMapping("/item/{itemId}")
    public ResponseEntity<CartDTO> updateQuantity(
            @PathVariable Long itemId,
            @RequestParam Long quantity) {

        return ResponseEntity.ok(cartService.updateItem(itemId, quantity));
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<CartDTO> removeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(itemId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clear() {
        cartService.clearCart();
        return ResponseEntity.ok("Cart cleared");
    }
}
