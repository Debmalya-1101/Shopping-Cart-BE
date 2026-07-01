package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.FlipkartScrapeRequest;
import com.demoproject.shoppingcart.dto.FlipkartScrapeResultDTO;
import com.demoproject.shoppingcart.service.FlipkartScraperService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import java.util.List;

/**
 * Admin-only REST controller for the Flipkart Scraper module.
 *
 * Security: Protected by Spring Security — only users with ROLE_ADMIN
 * can access /api/admin/** endpoints (see SecurityConfig).
 *
 * Usage from Postman:
 *   1. POST /auth/login  → get JWT token for Admin01
 *   2. POST /api/admin/scraper/flipkart
 *      Authorization: Bearer <jwt_token>
 *      Body: { "fsns": ["MOBGTAGMG5GB3BD3"], "categoryName": "Smartphones" }
 */
@RestController
@RequestMapping("/api/admin/scraper")
@CrossOrigin(origins = "*")
@Profile("!dev")
public class FlipkartScraperController {

    private final FlipkartScraperService flipkartScraperService;

    public FlipkartScraperController(FlipkartScraperService flipkartScraperService) {
        this.flipkartScraperService = flipkartScraperService;
    }

    /**
     * Scrapes Flipkart product pages by FSNs and saves all extracted data
     * (product, images, specs, reviews, orders) to the database.
     *
     * POST /api/admin/scraper/flipkart
     */
    @PostMapping("/flipkart")
    public ResponseEntity<List<FlipkartScrapeResultDTO>> scrapeFlipkartProduct(
            @Valid @RequestBody FlipkartScrapeRequest request) {

        List<FlipkartScrapeResultDTO> results = flipkartScraperService.scrapeAndSave(request);

        // Return 200 OK if at least one FSN succeeded (or got partial results),
        // or 422 Unprocessable Entity if all of them failed.
        boolean allFailed = results.stream().allMatch(res -> "FAILED".equals(res.getStatus()));
        if (allFailed) {
            return ResponseEntity.unprocessableEntity().body(results);
        }
        return ResponseEntity.ok(results);
    }
}
