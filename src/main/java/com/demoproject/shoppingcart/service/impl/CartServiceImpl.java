package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.*;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.*;
import com.demoproject.shoppingcart.service.CartService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Override
    public CartDTO getCart() {
        AppUser user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        cart.setTotalPrice(calculateTotal(cart));
        cartRepository.save(cart);

        return convertToDTO(cart);
    }


    @Override
    public CartDTO addToCart(AddToCartRequest request) {

        AppUser user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        // Check if product already in cart first
        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Increase quantity
            long newQty = existingItem.getQuantity() + request.getQuantity();
            existingItem.setQuantity(newQty);
        } else {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Create new cart item
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            item.setPrice(product.getPrice());  // snapshot!

            cart.getItems().add(item);
        }

        cart.setTotalPrice(calculateTotal(cart));
        cartRepository.save(cart);

        return convertToDTO(cart);
    }

    @Override
    public CartDTO updateItem(Long itemId, Long quantity) {
        AppUser user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized modification");
        }

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
        }

        cart.setTotalPrice(calculateTotal(cart));
        cartRepository.save(cart);

        return convertToDTO(cart);
    }

    @Override
    public CartDTO removeItem(Long itemId) {
        AppUser user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        cart.setTotalPrice(calculateTotal(cart));
        cartRepository.save(cart);

        return convertToDTO(cart);
    }

    @Override
    public void clearCart() {
        AppUser user = getLoggedInUser();
        Cart cart = getOrCreateCart(user);

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();

        cart.setTotalPrice(0L);
        cartRepository.save(cart);
    }
    
    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setTotalPrice(0L);
                    return cartRepository.save(newCart);
                });
    }

    private Long calculateTotal(Cart cart) {
        return cart.getItems().stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    private CartDTO convertToDTO(Cart cart) {
        List<CartItemDTO> itemDTOs = cart.getItems()
                .stream()
                .map(item -> new CartItemDTO(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl(),
                        item.getPrice(),
                        item.getQuantity(),
                        item.getPrice() * item.getQuantity()
                ))
                .collect(Collectors.toList());

        return new CartDTO(itemDTOs, cart.getTotalPrice());
    }
}
