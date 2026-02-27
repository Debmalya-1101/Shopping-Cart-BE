package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AddToWishlistRequest;
import com.demoproject.shoppingcart.dto.WishlistDTO;
import com.demoproject.shoppingcart.dto.WishlistItemDTO;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.model.WishlistItem;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.repository.WishlistRepository;
import com.demoproject.shoppingcart.service.WishlistService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               ProductRepository productRepository,
                               UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    private WishlistDTO convertToDTO(List<WishlistItem> items) {
        List<WishlistItemDTO> dtoList = items.stream()
                .map(item -> {
                    Product p = item.getProduct();
                    return new WishlistItemDTO(
                            item.getId(),
                            p.getId(),
                            p.getName(),
                            p.getImageUrl(),
                            p.getPrice(),
                            p.getRating()
                    );
                })
                .collect(Collectors.toList());

        return new WishlistDTO(dtoList);
    }

    @Override
    public WishlistDTO getWishlist() {
        AppUser user = getLoggedInUser();
        List<WishlistItem> items = wishlistRepository.findByUser(user);
        return convertToDTO(items);
    }

    @Override
    public WishlistDTO addToWishlist(AddToWishlistRequest request) {
        AppUser user = getLoggedInUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Check if already in wishlist (no duplicates)
        wishlistRepository.findByUserAndProduct(user, product)
                .ifPresent(existing -> {
                    // If you want "toggle" behavior, you could delete here.
                    // For now, we'll just ignore duplicate add.
                    throw new RuntimeException("Product already in wishlist");
                });

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);

        wishlistRepository.save(item);

        List<WishlistItem> items = wishlistRepository.findByUser(user);
        return convertToDTO(items);
    }

    @Override
    public WishlistDTO removeItem(Long itemId) {
        AppUser user = getLoggedInUser();

        WishlistItem item = wishlistRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Wishlist item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized action");
        }

        wishlistRepository.delete(item);

        List<WishlistItem> items = wishlistRepository.findByUser(user);
        return convertToDTO(items);
    }

    @Override
    public WishlistDTO toggleWishlist(Long productId) {

        AppUser user = getLoggedInUser();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        wishlistRepository.findByUserAndProduct(user, product)
                .ifPresentOrElse(
                        // Case 1: already exists → remove
                        wishlistRepository::delete,

                        // Case 2: not exists → add
                        () -> {
                            WishlistItem item = new WishlistItem();
                            item.setUser(user);
                            item.setProduct(product);
                            wishlistRepository.save(item);
                        }
                );

        // Return updated wishlist
        return convertToDTO(wishlistRepository.findByUser(user));
    }
}
