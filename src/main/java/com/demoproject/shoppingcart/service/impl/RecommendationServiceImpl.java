package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.ProductListDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.*;
import com.demoproject.shoppingcart.service.RecommendationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public RecommendationServiceImpl(ProductRepository productRepository,
                                     CartRepository cartRepository,
                                     WishlistRepository wishlistRepository,
                                     OrderRepository orderRepository,
                                     UserRepository userRepository) {
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    private ProductListDTO toProductListDTO(Product product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;
        Long ratingCount = product.getRatingCount() != null ? product.getRatingCount() : 0L;
        return new ProductListDTO(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getImageUrl(),
                product.getRating(),
                ratingCount,
                product.getActive(),
                product.getBrand(),
                categoryName
        );
    }

    @Override
    public List<ProductListDTO> getRecommendations() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = null;
        if (username != null && !username.trim().isEmpty() && !username.equals("anonymousUser")) {
            user = userRepository.findByUserName(username).orElse(null);
        }

        if (user == null) {
            // Unauthenticated user fallback
            return productRepository.findTop15ByActiveTrueOrderByRatingDescRatingCountDesc()
                    .stream().limit(8).map(this::toProductListDTO).collect(Collectors.toList());
        }

        Set<String> categoryNames = new HashSet<>();
        Set<Long> excludedProductIds = new HashSet<>();
        excludedProductIds.add(-1L); // Prevent empty NOT IN clause

        // 1. From Cart
        cartRepository.findByUser(user).ifPresent(cart -> {
            for (CartItem item : cart.getItems()) {
                Product p = item.getProduct();
                excludedProductIds.add(p.getId());
                if (p.getCategory() != null) {
                    categoryNames.add(p.getCategory().getName());
                }
            }
        });

        // 2. From Wishlist
        List<WishlistItem> wishlistItems = wishlistRepository.findByUser(user);
        for (WishlistItem w : wishlistItems) {
            Product p = w.getProduct();
            excludedProductIds.add(p.getId());
            if (p.getCategory() != null) {
                categoryNames.add(p.getCategory().getName());
            }
        }

        // 3. From Orders
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        for (Order o : orders) {
            for (OrderItem oi : o.getItems()) {
                Product p = oi.getProduct();
                excludedProductIds.add(p.getId());
                if (p.getCategory() != null) {
                    categoryNames.add(p.getCategory().getName());
                }
            }
        }

        List<Product> recommendations;
        if (categoryNames.isEmpty()) {
            recommendations = productRepository.findTop15ByActiveTrueOrderByRatingDescRatingCountDesc();
        } else {
            recommendations = productRepository.findTop15ByCategoryNameInAndIdNotInAndActiveTrueOrderByRatingDesc(
                    new ArrayList<>(categoryNames), new ArrayList<>(excludedProductIds)
            );
            if (recommendations.isEmpty()) {
                recommendations = productRepository.findTop15ByActiveTrueOrderByRatingDescRatingCountDesc();
            }
        }

        return recommendations.stream()
                .limit(8)
                .map(this::toProductListDTO)
                .collect(Collectors.toList());
    }
}
