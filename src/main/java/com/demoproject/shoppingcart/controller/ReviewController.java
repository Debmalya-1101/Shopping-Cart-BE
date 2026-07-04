package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.CreateReviewRequest;
import com.demoproject.shoppingcart.dto.UpdateReviewRequest;
import com.demoproject.shoppingcart.dto.ReviewDTO;
import com.demoproject.shoppingcart.dto.PageResponse;
import com.demoproject.shoppingcart.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")

public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Create a new review for a product
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewDTO review = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * Update an existing review
     * PUT /api/reviews/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewDTO review = reviewService.updateReview(id, request);
        return ResponseEntity.ok(review);
    }

    /**
     * Delete a review
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.ok("Review deleted successfully");
    }

    /**
     * Get all reviews for a specific product with pagination
     * GET /api/products/{productId}/reviews?page=0&size=10
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<ReviewDTO> response = reviewService.getProductReviews(productId, page, size);
        return ResponseEntity.ok(response);
    }
}

