package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.FlipkartScrapeRequest;
import com.demoproject.shoppingcart.dto.FlipkartScrapeResultDTO;
import java.util.List;

/**
 * Service contract for scraping Flipkart product pages using
 * a Playwright headless browser and persisting all extracted data
 * into the database.
 */
public interface FlipkartScraperService {

    /**
     * Scrapes the Flipkart India product pages for the given list of FSNs,
     * maps all extracted data to the domain model, persists it,
     * and returns a detailed list of summaries of what was inserted.
     *
     * @param request Contains FSNs, target category name, and seeding counts.
     * @return A list of populated {@link FlipkartScrapeResultDTO} summarising the operations.
     */
    List<FlipkartScrapeResultDTO> scrapeAndSave(FlipkartScrapeRequest request);
}
