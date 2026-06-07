package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.CreateReviewRequest;
import com.demoproject.shoppingcart.dto.UpdateReviewRequest;
import com.demoproject.shoppingcart.dto.ReviewDTO;
import com.demoproject.shoppingcart.dto.PageResponse;

public interface ReviewService {

    ReviewDTO createReview(CreateReviewRequest request);

    ReviewDTO updateReview(Long reviewId, UpdateReviewRequest request);

    void deleteReview(Long reviewId);

    PageResponse<ReviewDTO> getProductReviews(Long productId, int page, int size);
}

