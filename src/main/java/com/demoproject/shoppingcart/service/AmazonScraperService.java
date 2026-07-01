package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AmazonScrapeRequest;
import com.demoproject.shoppingcart.dto.AmazonScrapeResultDTO;

import java.util.List;

/**
 * Service contract for scraping Amazon India product pages using
 * a Playwright headless browser and persisting all extracted data
 * (product, images, attributes, reviews, orders) into the database.
 */
public interface AmazonScraperService {

    /**
     * Scrapes the Amazon India product page for the given ASIN(s),
     * maps all extracted data to the domain model, persists it,
     * and returns a list of detailed summaries of what was inserted.
     *
     * @param request Contains ASINs, target category name, and seeding counts.
     * @return A list of populated {@link AmazonScrapeResultDTO} summarising the operations.
     */
    List<AmazonScrapeResultDTO> scrapeAndSave(AmazonScrapeRequest request);
}
