package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.CreateReviewRequest;
import com.demoproject.shoppingcart.dto.UpdateReviewRequest;
import com.demoproject.shoppingcart.dto.ReviewDTO;
import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.exception.ResourceNotFoundException;
import com.demoproject.shoppingcart.model.ProductReview;
import com.demoproject.shoppingcart.model.Product;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.PaymentStatus;
import com.demoproject.shoppingcart.repository.ReviewRepository;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.service.ReviewService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           OrderRepository orderRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public ReviewDTO createReview(CreateReviewRequest request) {
        AppUser user = getLoggedInUser();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        // Check if user has already reviewed this product
        if (reviewRepository.existsByProductAndUser(product, user)) {
            throw new RuntimeException("You have already reviewed this product. Edit your existing review or delete it first.");
        }

        // Check if user has a successful payment order for this product
        boolean hasSuccessfulOrder = orderRepository.hasSuccessfulOrderForProduct(user, product, PaymentStatus.SUCCESS);
        if (!hasSuccessfulOrder) {
            throw new RuntimeException("You must purchase this product before reviewing it");
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());

        ProductReview savedReview = reviewRepository.save(review);

        // Update product rating after adding review
        updateProductRating(product);

        return toReviewDTO(savedReview);
    }

    @Override
    public ReviewDTO updateReview(Long reviewId, UpdateReviewRequest request) {
        AppUser user = getLoggedInUser();

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Check if review belongs to current user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());

        ProductReview updatedReview = reviewRepository.save(review);

        // Update product rating after modifying review
        updateProductRating(review.getProduct());

        return toReviewDTO(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        AppUser user = getLoggedInUser();

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

        // Check if review belongs to current user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own reviews");
        }

        Product product = review.getProduct();
        reviewRepository.deleteById(reviewId);

        // Update product rating after deleting review
        updateProductRating(product);
    }

    @Override
    public PageResponse<ReviewDTO> getProductReviews(Long productId, int page, int size) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProductReview> reviewPage = reviewRepository.findByProduct(product, pageable);

        List<ReviewDTO> dtoList = reviewPage.getContent()
                .stream()
                .map(this::toReviewDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                dtoList,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.isLast()
        );
    }

    // ============ Helper Methods ============

    private void updateProductRating(Product product) {
        Page<ProductReview> allReviews = reviewRepository.findByProduct(product,
                PageRequest.of(0, Integer.MAX_VALUE));

        if (allReviews.isEmpty()) {
            product.setRating(0.0);
            product.setRatingCount(0L);
        } else {
            double avgRating = allReviews.getContent()
                    .stream()
                    .mapToInt(ProductReview::getRating)
                    .average()
                    .orElse(0.0);
            product.setRating(Math.round(avgRating * 10.0) / 10.0); // Round to 1 decimal place
            product.setRatingCount((long) allReviews.getContent().size());
        }

        productRepository.save(product);
    }

    private ReviewDTO toReviewDTO(ProductReview review) {
        return new ReviewDTO(
                review.getId(),
                review.getProduct().getId(),
                review.getUser().getId(),
                review.getUser().getUserName(),
                review.getRating(),
                review.getReviewText(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}

