package com.demoproject.shoppingcart.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Response DTO returned after a successful Flipkart scrape & seed operation.
 * Gives the admin a clear summary of exactly what was inserted into the database.
 */
@Getter
@Setter
@Builder
public class FlipkartScrapeResultDTO {

    private String status;           // "SUCCESS" or "FAILED"
    private String fsn;              // The FSN that was scraped
    private String message;

    // === Scraped Product Info ===
    private Long productId;          // DB ID of the newly created product
    private String productName;      // Short display name (e.g. "Samsung Galaxy S24")
    private String fullProductName;  // Full Flipkart title (e.g. "SAMSUNG Galaxy S24 5G (Amber Yellow, 256 GB)...")
    private Long priceInr;           // Price in INR (Long rupees)
    private String category;         // Category name resolved or created
    private String mainImageUrl;     // Primary product image URL

    // === Insertion Counts ===
    private int imagesInserted;      // Number of gallery images saved
    private int attributesInserted;  // Number of spec attributes saved
    private int reviewsSimulated;    // Number of fake reviews created
    private int ordersSimulated;     // Number of fake orders created

    // === Full Gallery ===
    private List<String> galleryImageUrls;
}
