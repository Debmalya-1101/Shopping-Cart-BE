package com.demoproject.shoppingcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for the Amazon scraper endpoint.
 * Pass from Postman as:
 * {
 *   "asins": ["B09G93C5DK"],
 *   "categoryName": "Smartphones"
 * }
 */
@Getter
@Setter
public class AmazonScrapeRequest {

    /**
     * Amazon Standard Identification Numbers (ASINs).
     * Found in the product URL: amazon.in/dp/{ASIN}
     * Example: B09G93C5DK (iPhone 13), B0BZM6985C (Samsung S23)
     */
    @NotEmpty(message = "At least one ASIN is required")
    private List<String> asins;

    /**
     * Category name to assign this product to.
     * A new category will be auto-created if it does not exist in the DB.
     * Example: "Smartphones", "Laptops", "Footwear"
     */
    @NotBlank(message = "Category name is required")
    private String categoryName;

    /**
     * Number of simulated reviews to create for this product (linked to existing users).
     * Defaults to 3 if not provided.
     */
    private int simulatedReviews = 3;

    /**
     * Number of simulated historical orders to create for this product.
     * Defaults to 5 if not provided.
     */
    private int simulatedOrders = 5;
}
