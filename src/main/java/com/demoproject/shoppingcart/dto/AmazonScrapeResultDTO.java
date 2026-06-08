package com.demoproject.shoppingcart.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response DTO returned after a successful Amazon scrape & seed operation.
 * Gives the admin a clear summary of exactly what was inserted into the database.
 */
@Getter
@Setter
@Builder
public class AmazonScrapeResultDTO {

    private String status;           // "SUCCESS" or "PARTIAL"
    private String asin;             // The ASIN that was scraped
    private String message;

    // === Scraped Product Info ===
    private Long productId;          // DB ID of the newly created product
    private String productName;      // Short display name (e.g. "Samsung Galaxy M07")
    private String fullProductName;  // Full Amazon title (e.g. "Samsung Galaxy M07 Mobile (Black, 4GB RAM...)"
    private Long priceInr;           // Price converted to INR (Long paise/rupees)
    private String category;         // Category name resolved or created
    private String mainImageUrl;     // Primary product image URL

    // === Insertion Counts ===
    private int imagesInserted;      // Number of gallery images saved
    private int attributesInserted;  // Number of spec attributes saved
    private int reviewsSimulated;    // Number of fake reviews created
    private int ordersSimulated;     // Number of fake orders created

    // === Full Gallery (for Postman visibility) ===
    private List<String> galleryImageUrls;
}
