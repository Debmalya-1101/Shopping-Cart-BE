package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.AmazonScrapeRequest;
import com.demoproject.shoppingcart.dto.AmazonScrapeResultDTO;
import com.demoproject.shoppingcart.service.AmazonScraperService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only REST controller for the Amazon Scraper module.
 *
 * Security: Protected by Spring Security — only users with ROLE_ADMIN
 * can access /api/admin/** endpoints (see SecurityConfig).
 *
 * Usage from Postman:
 *   1. POST /auth/login  → get JWT token for Admin01
 *   2. POST /api/admin/scraper/amazon
 *      Authorization: Bearer <jwt_token>
 *      Body: { "asin": "B09G93C5DK", "categoryName": "Smartphones" }
 */
@RestController
@RequestMapping("/api/admin/scraper")
@CrossOrigin(origins = "*")
public class AmazonScraperController {

    private final AmazonScraperService amazonScraperService;

    public AmazonScraperController(AmazonScraperService amazonScraperService) {
        this.amazonScraperService = amazonScraperService;
    }

    /**
     * Scrapes a single Amazon India product page by ASIN and saves all
     * extracted data (product, images, specs, reviews, orders) to the database.
     *
     * POST /api/admin/scraper/amazon
     *
     * Request Body:
     * {
     *   "asin": "B09G93C5DK",          ← Required: 10-character Amazon ASIN
     *   "categoryName": "Smartphones", ← Required: category to assign product to
     *   "simulatedReviews": 3,         ← Optional: number of fake reviews (default: 3)
     *   "simulatedOrders": 5           ← Optional: number of fake orders (default: 5)
     * }
     *
     * Response (200 OK):
     * {
     *   "status": "SUCCESS",
     *   "asin": "B09G93C5DK",
     *   "productId": 42,
     *   "productName": "Apple iPhone 13 (128GB) - Midnight",
     *   "priceInr": 52999,
     *   "category": "Smartphones",
     *   "mainImageUrl": "https://m.media-amazon.com/images/I/...",
     *   "galleryImageUrls": [...],
     *   "imagesInserted": 6,
     *   "attributesInserted": 12,
     *   "reviewsSimulated": 3,
     *   "ordersSimulated": 5,
     *   "message": "Product successfully scraped from Amazon India and saved to database."
     * }
     */
    @PostMapping("/amazon")
    public ResponseEntity<List<AmazonScrapeResultDTO>> scrapeAmazonProduct(
            @Valid @RequestBody AmazonScrapeRequest request) {

        List<AmazonScrapeResultDTO> results = amazonScraperService.scrapeAndSave(request);

        // Return 200 OK if at least one ASIN succeeded (or got partial results),
        // or 422 Unprocessable Entity if all of them failed.
        boolean allFailed = results.stream().allMatch(res -> "FAILED".equals(res.getStatus()));
        if (allFailed) {
            return ResponseEntity.unprocessableEntity().body(results);
        }
        return ResponseEntity.ok(results);
    }
}
