package com.demoproject.shoppingcart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Request payload for the Flipkart scraper endpoint.
 * Pass from Postman as:
 * {
 *   "fsns": ["MOBGTAGMG5GB3BD3"],
 *   "categoryName": "Smartphones"
 * }
 */
@Getter
@Setter
public class FlipkartScrapeRequest {

    /**
     * Flipkart Serial Numbers (FSNs / PIDs).
     * Found in the product URL: flipkart.com/.../p/itm...??pid={FSN}
     * Example: MOBGTAGMG5GB3BD3
     */
    @NotEmpty(message = "At least one Flipkart FSN (Product ID) is required")
    private List<String> fsns;

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
