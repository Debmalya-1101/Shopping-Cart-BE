package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AmazonScrapeRequest;
import com.demoproject.shoppingcart.dto.AmazonScrapeResultDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.*;
import com.demoproject.shoppingcart.service.AmazonScraperService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Amazon Scraper Service Implementation.
 *
 * Uses Microsoft Playwright (headless Chromium) to load Amazon India product pages,
 * extract product data, and persist it across all related DB tables.
 *
 * Scraping flow per ASIN:
 *  1. Launch headless Chromium with realistic browser headers.
 *  2. Navigate to https://www.amazon.in/dp/{ASIN}
 *  3. Wait for the page to fully load (networkidle).
 *  4. Extract: title, price (INR), description, main image, gallery images, specs table.
 *  5. Resolve/create Category → save Product → save ProductImages → save AttributeKeys + ProductAttributes.
 *  6. Simulate ProductReviews and Orders mapped to existing DB users.
 *  7. Close browser and return AmazonScrapeResultDTO.
 */
@Service
public class AmazonScraperServiceImpl implements AmazonScraperService {

    private static final Logger log = LoggerFactory.getLogger(AmazonScraperServiceImpl.class);

    // Amazon India base URL
    private static final String AMAZON_PRODUCT_URL = "https://www.amazon.in/dp/";

    // Predefined pool of realistic review texts for simulated reviews
    private static final List<String> REVIEW_POOL = List.of(
            "Excellent product! Exactly as described. Highly recommended.",
            "Very satisfied with this purchase. Great value for money.",
            "Good quality product. Delivery was fast and packaging was secure.",
            "Works perfectly fine. Would definitely buy again.",
            "Amazing product! Exceeded my expectations. 5 stars!",
            "Decent product for the price. Build quality is solid.",
            "Great purchase! The product feels premium and well-built.",
            "Very happy with this item. Customer service was also responsive.",
            "Good product overall. Minor issues but nothing deal-breaking.",
            "Fantastic! This is exactly what I was looking for. No complaints."
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final AttributeKeyRepository attributeKeyRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ShipmentRepository shipmentRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final DeliveryFeedbackRepository deliveryFeedbackRepository;
    private final TransactionTemplate transactionTemplate;

    public AmazonScraperServiceImpl(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            AttributeKeyRepository attributeKeyRepository,
            ProductAttributeRepository productAttributeRepository,
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            InventoryRepository inventoryRepository,
            InventoryTransactionRepository inventoryTransactionRepository,
            ShipmentRepository shipmentRepository,
            DeliveryPartnerRepository deliveryPartnerRepository,
            DeliveryFeedbackRepository deliveryFeedbackRepository,
            PlatformTransactionManager transactionManager) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.attributeKeyRepository = attributeKeyRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.shipmentRepository = shipmentRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.deliveryFeedbackRepository = deliveryFeedbackRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // =====================================================================
    // PUBLIC METHOD: Entry point called by the controller
    // =====================================================================

    @Override
    public List<AmazonScrapeResultDTO> scrapeAndSave(AmazonScrapeRequest request) {
        List<AmazonScrapeResultDTO> results = new ArrayList<>();
        List<String> asins = request.getAsins();
        String categoryName = request.getCategoryName().trim();

        for (int i = 0; i < asins.size(); i++) {
            String asin = asins.get(i).trim().toUpperCase();
            if (i > 0) {
                log.info("Waiting 5 seconds before scraping the next ASIN: {}...", asin);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Scraping delay sleep interrupted: {}", e.getMessage());
                }
            }

            AmazonScrapeResultDTO result = scrapeSingleAsin(asin, categoryName, request);
            results.add(result);
        }
        return results;
    }

    private AmazonScrapeResultDTO scrapeSingleAsin(String asin, String categoryName, AmazonScrapeRequest request) {
        String targetUrl = AMAZON_PRODUCT_URL + asin;

        log.info("=== Starting Amazon Scrape: ASIN={}, Category={} ===", asin, categoryName);

        try (Playwright playwright = Playwright.create()) {
            // ── Step 1: Launch headless Chromium with realistic settings ──────────
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            // Uncomment below to see the browser window during debugging:
                            // .setHeadless(false)
                            .setSlowMo(200) // 200ms between actions to appear more human-like
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            // Mimic a real Windows Chrome user agent to bypass basic bot detection
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36")
                            .setViewportSize(1280, 800)
                            .setLocale("en-IN")
                            .setTimezoneId("Asia/Kolkata")
            );

            Page page = context.newPage();

            // ── Step 2: Navigate to Amazon India product page ─────────────────────
            log.info("Navigating to: {}", targetUrl);
            page.navigate(targetUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30_000));

            // Short wait to let lazy-loaded elements render
            page.waitForTimeout(2000);

            // ── Step 3: Check for CAPTCHA or redirect ─────────────────────────────
            String pageTitle = page.title();
            if (pageTitle.toLowerCase().contains("robot check") ||
                    pageTitle.toLowerCase().contains("sorry") ||
                    page.url().contains("captcha")) {
                browser.close();
                log.warn("CAPTCHA detected for ASIN: {}", asin);
                return AmazonScrapeResultDTO.builder()
                        .status("FAILED")
                        .asin(asin)
                        .message("Amazon returned a CAPTCHA page. Please wait a few minutes and retry.")
                        .build();
            }

            // ── Step 4: Extract all product data ──────────────────────────────────
            String title           = extractTitle(page);
            Long   priceInr        = extractPrice(page);
            String description     = extractDescription(page);
            String mainImageUrl    = extractMainImage(page);
            List<String> gallery   = extractGalleryImages(page);
            Map<String, String> specs = extractSpecifications(page);

            browser.close();
            log.info("Scrape complete. Title='{}', Price=₹{}, Images={}, Specs={}",
                    title, priceInr, gallery.size(), specs.size());

            // ── Step 5: Validate we got meaningful data ───────────────────────────
            if (title == null || title.isBlank()) {
                return AmazonScrapeResultDTO.builder()
                        .status("FAILED")
                        .asin(asin)
                        .message("Could not extract product title. The page layout may have changed or the ASIN is invalid.")
                        .build();
            }

            // ── Step 6: Persist data to database inside a transaction ────────────
            return transactionTemplate.execute(status ->
                persistToDatabase(asin, categoryName, title, priceInr, description,
                        mainImageUrl, gallery, specs, request)
            );

        } catch (Exception e) {
            log.error("Scraping failed for ASIN {}: {}", asin, e.getMessage(), e);
            return AmazonScrapeResultDTO.builder()
                    .status("FAILED")
                    .asin(asin)
                    .message("Scraping error: " + e.getMessage())
                    .build();
        }
    }

    // =====================================================================
    // EXTRACTION METHODS
    // =====================================================================

    /**
     * Extracts the product title from Amazon's #productTitle element.
     */
    private String extractTitle(Page page) {
        try {
            Locator el = page.locator("#productTitle");
            if (el.count() > 0) {
                return el.first().innerText().trim();
            }
        } catch (Exception e) {
            log.warn("Title extraction failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts the price and parses it as a Long (INR).
     * Handles formats like: ₹52,999, ₹1,04,999.00, 52999
     */
    private Long extractPrice(Page page) {
        // Try multiple selectors Amazon uses depending on product type
        String[] priceSelectors = {
                ".a-price-whole",
                "#priceblock_ourprice",
                "#priceblock_dealprice",
                ".priceToPay .a-price-whole",
                "#corePriceDisplay_desktop_feature_div .a-price-whole"
        };

        for (String selector : priceSelectors) {
            try {
                Locator el = page.locator(selector);
                if (el.count() > 0) {
                    String raw = el.first().innerText().trim();
                    // Strip currency symbols, commas, and decimal parts
                    String cleaned = raw.replaceAll("[₹,\\s]", "")
                            .replaceAll("\\.\\d+$", "")  // remove .00
                            .trim();
                    if (!cleaned.isEmpty()) {
                        return Long.parseLong(cleaned);
                    }
                }
            } catch (Exception ignored) {
                // Try next selector
            }
        }
        log.warn("Price extraction failed, defaulting to 0");
        return 0L;
    }

    /**
     * Extracts product description from the product description section
     * or the feature bullets as a fallback.
     */
    private String extractDescription(Page page) {
        // Try the dedicated description box first
        try {
            Locator desc = page.locator("#productDescription p");
            if (desc.count() > 0) {
                String text = desc.first().innerText().trim();
                if (!text.isBlank()) return text;
            }
        } catch (Exception ignored) {}

        // Fall back to bullet points (About This Item)
        try {
            Locator bullets = page.locator("#feature-bullets .a-list-item");
            if (bullets.count() > 0) {
                StringBuilder sb = new StringBuilder();
                int count = Math.min(bullets.count(), 5); // max 5 bullets
                for (int i = 0; i < count; i++) {
                    String line = bullets.nth(i).innerText().trim();
                    if (!line.isBlank()) {
                        sb.append("• ").append(line).append("\n");
                    }
                }
                return sb.toString().trim();
            }
        } catch (Exception ignored) {}

        return "No description available.";
    }

    /**
     * Extracts the main product image URL from the #landingImage element.
     */
    private String extractMainImage(Page page) {
        try {
            Locator img = page.locator("#landingImage");
            if (img.count() > 0) {
                // Prefer the high-res src over the thumbnail data-src
                String src = img.first().getAttribute("src");
                if (src != null && !src.isBlank() && !src.startsWith("data:")) {
                    return src;
                }
                // Try data-old-hires for even higher resolution
                String hiRes = img.first().getAttribute("data-old-hires");
                if (hiRes != null && !hiRes.isBlank()) {
                    return hiRes;
                }
            }
        } catch (Exception e) {
            log.warn("Main image extraction failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Extracts all high-resolution gallery image URLs.
     *
     * Amazon stores the full image gallery inside a JavaScript variable called
     * 'colorImages' embedded in a <script> tag on the page.
     * We read the raw page source and extract all HTTPS image URLs from that block.
     */
    private List<String> extractGalleryImages(Page page) {
        List<String> images = new ArrayList<>();
        try {
            String pageSource = page.content();

            // Amazon embeds image data in the 'colorImages' JS variable
            // Pattern matches all "hiRes" or "large" image URLs in the colorImages block
            Pattern pattern = Pattern.compile(
                    "\"(https://m\\.media-amazon\\.com/images/I/[^\"]+\\._[^\"]*\\.(?:jpg|jpeg|png|webp))\"",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(pageSource);

            Set<String> seen = new LinkedHashSet<>();
            while (matcher.find()) {
                String url = matcher.group(1);
                // Filter out tiny thumbnails (less than 200px marker in URL)
                if (!url.contains("_SR38,50_") && !url.contains("_SS40_") &&
                        !url.contains("_AC_US40_") && !url.contains("_SX38_")) {
                    seen.add(url);
                }
                if (seen.size() >= 8) break; // limit to 8 gallery images
            }
            images.addAll(seen.stream().skip(1).toList()); // skip first — it's usually a duplicate of main image
        } catch (Exception e) {
            log.warn("Gallery image extraction failed: {}", e.getMessage());
        }

        log.info("Extracted {} gallery images", images.size());
        return images;
    }

    /**
     * Extracts the product specifications from Amazon's tech spec table.
     * Returns a map of {spec name -> spec value}, e.g.:
     * { "Brand" -> "Apple", "RAM" -> "6 GB", "Battery" -> "3227 mAh" }
     */
    private Map<String, String> extractSpecifications(Page page) {
        Map<String, String> specs = new LinkedHashMap<>();

        // Amazon uses two different table formats for specs
        String[] tableSelectors = {
                "#productDetails_techSpec_section_1 tr",
                "#productDetails_detailBullets_sections1 tr",
                ".prodDetTable tr",
                "#detailBullets_feature_div li"
        };

        for (String selector : tableSelectors) {
            try {
                Locator rows = page.locator(selector);
                if (rows.count() == 0) continue;

                for (int i = 0; i < Math.min(rows.count(), 20); i++) {
                    try {
                        Locator row = rows.nth(i);
                        // Table rows have <th> for key and <td> for value
                        String key   = row.locator("th").innerText().trim()
                                .replaceAll("[\\n\\t]", " ")
                                .replaceAll("\\s+", " ");
                        String value = row.locator("td").innerText().trim()
                                .replaceAll("[\\n\\t]", " ")
                                .replaceAll("\\s+", " ");

                        if (!key.isBlank() && !value.isBlank() &&
                                !key.contains("Customer Reviews") &&
                                !key.contains("Best Sellers")) {
                            specs.put(key, value);
                        }
                    } catch (Exception ignored) {}
                }

                if (!specs.isEmpty()) break; // Stop once we get specs from one table
            } catch (Exception e) {
                log.warn("Spec extraction selector '{}' failed: {}", selector, e.getMessage());
            }
        }

        log.info("Extracted {} specifications", specs.size());
        return specs;
    }

    // =====================================================================
    // DATABASE PERSISTENCE
    // =====================================================================

    /**
     * Persists all scraped data into the database tables.
     */
    private AmazonScrapeResultDTO persistToDatabase(
            String asin, String categoryName,
            String title, Long priceInr, String description,
            String mainImageUrl, List<String> gallery,
            Map<String, String> specs, AmazonScrapeRequest request) {

        // ── 1. Resolve or create Category ─────────────────────────────────────
        Category category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName(categoryName);
                    Category saved = categoryRepository.save(newCat);
                    log.info("Created new category: '{}'", categoryName);
                    return saved;
                });

        // ── 2. Create and save the Product ────────────────────────────────────
        String shortName = extractShortName(title);  // e.g. "Samsung Galaxy M07"
        String brand     = extractBrand(specs, shortName);

        Product product = new Product();
        product.setName(shortName);              // Short display name
        product.setFullName(title);              // Full Amazon title stored separately
        product.setPrice(priceInr);
        product.setDescription(truncate(description, 5000));
        product.setImageUrl(mainImageUrl);
        product.setCategory(category);
        product.setStock(generateRealisticStock());
        product.setRating(generateRealisticRating());
        product.setActive(true);
        product.setBrand(brand);

        product = productRepository.save(product);
        log.info("Saved product: id={}, name='{}'", product.getId(), product.getName());

        // ── 2.5. Initialize Inventory ─────────────────────────────────────────
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(product.getStock());
        inventory.setReservedQuantity(0);
        inventory.setDamagedQuantity(0);
        inventory.setReorderLevel(5);
        inventory = inventoryRepository.save(inventory);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setInventory(inventory);
        tx.setTransactionType(InventoryTransactionType.RESTOCK);
        tx.setReferenceType("SYSTEM_SEED");
        tx.setReferenceId("AMAZON-SCRAPE");
        tx.setQuantity(product.getStock());
        tx.setNotes("Initial stock from scraping");
        inventoryTransactionRepository.save(tx);
        boolean inventorySimulated = true;

        // ── 3. Save gallery images to product_images table ────────────────────
        int imagesInserted = 0;
        for (String imageUrl : gallery) {
            if (imageUrl != null && !imageUrl.isBlank()) {
                ProductImage pi = new ProductImage();
                pi.setImageUrl(imageUrl);
                pi.setProduct(product);
                productImageRepository.save(pi);
                imagesInserted++;
            }
        }
        log.info("Saved {} gallery images", imagesInserted);

        // ── 4. Save specifications to attribute_keys + product_attributes ──────
        int attributesInserted = 0;
        if (!specs.isEmpty()) {
            for (Map.Entry<String, String> entry : specs.entrySet()) {
                String specKey   = entry.getKey();
                String specValue = entry.getValue();

                // Resolve or create the AttributeKey for this category
                final Category finalCategory = category;
                AttributeKey attrKey = resolveAttributeKey(specKey, finalCategory);

                // Create the ProductAttribute linking value → key → product
                ProductAttribute attrVal = new ProductAttribute();
                attrVal.setProduct(product);
                attrVal.setAttributeKey(attrKey);
                attrVal.setValueText(specValue);

                // If the value looks numeric, store the numeric form too
                try {
                    double numeric = Double.parseDouble(specValue.replaceAll("[^\\d.]", ""));
                    attrVal.setValueNumber(numeric);
                } catch (NumberFormatException ignored) {}

                productAttributeRepository.save(attrVal);
                attributesInserted++;
            }
        }
        log.info("Saved {} attributes", attributesInserted);

        // ── 5. Simulate reviews linked to existing users ───────────────────────
        int reviewsCreated = simulateReviews(product, request.getSimulatedReviews());

        // Set the rating count based on simulated reviews
        product.setRatingCount((long) reviewsCreated);
        product = productRepository.save(product);

        // ── 6. Simulate historical orders linked to existing users ─────────────
        int[] ordersSimStats = simulateOrders(product, request.getSimulatedOrders());
        int ordersCreated = ordersSimStats[0];
        int shipmentsCreated = ordersSimStats[1];
        int feedbacksCreated = ordersSimStats[2];

        // ── 7. Build and return the result DTO ────────────────────────────────
        return AmazonScrapeResultDTO.builder()
                .status("SUCCESS")
                .asin(asin)
                .message("Product successfully scraped from Amazon India and saved to database.")
                .productId(product.getId())
                .productName(product.getName())           // Short name: "Samsung Galaxy M07"
                .fullProductName(product.getFullName())   // Full title from Amazon
                .priceInr(product.getPrice())
                .category(category.getName())
                .mainImageUrl(mainImageUrl)
                .galleryImageUrls(gallery)
                .imagesInserted(imagesInserted)
                .attributesInserted(attributesInserted)
                .reviewsSimulated(reviewsCreated)
                .ordersSimulated(ordersCreated)
                .inventorySimulated(inventorySimulated)
                .shipmentsSimulated(shipmentsCreated)
                .feedbacksSimulated(feedbacksCreated)
                .build();
    }

    /**
     * Resolves an existing AttributeKey by name in this category,
     * or creates a new one if it doesn't exist yet.
     */
    private AttributeKey resolveAttributeKey(String keyName, Category category) {
        List<AttributeKey> existing = attributeKeyRepository.findByCategoryId(category.getId());
        return existing.stream()
                .filter(k -> k.getKeyName().equalsIgnoreCase(keyName))
                .findFirst()
                .orElseGet(() -> {
                    AttributeKey newKey = new AttributeKey();
                    newKey.setKeyName(keyName);
                    newKey.setType(AttributeType.TEXT);
                    newKey.setCategory(category);
                    return attributeKeyRepository.save(newKey);
                });
    }

    /**
     * Simulates realistic product reviews by assigning them to random existing users.
     * Skips users who already have a review for this product (due to unique constraint).
     */
    private int simulateReviews(Product product, int count) {
        List<AppUser> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found in DB, skipping review simulation.");
            return 0;
        }

        Collections.shuffle(users);
        Random random = new Random();
        int created = 0;

        for (int i = 0; i < Math.min(count, users.size()); i++) {
            AppUser user = users.get(i);
            try {
                // Skip if this user already reviewed this product
                if (reviewRepository.existsByProductAndUser(product, user)) {
                    continue;
                }
                ProductReview review = new ProductReview();
                review.setProduct(product);
                review.setUser(user);
                review.setRating(3 + random.nextInt(3)); // Random rating: 3, 4, or 5
                review.setReviewText(REVIEW_POOL.get(random.nextInt(REVIEW_POOL.size())));
                reviewRepository.save(review);
                created++;
            } catch (Exception e) {
                log.warn("Could not create review for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("Simulated {} reviews", created);
        return created;
    }

    /**
     * Simulates historical DELIVERED orders spread across the last 12 months
     * to populate the admin analytics dashboard with realistic data.
     */
    private int[] simulateOrders(Product product, int count) {
        List<AppUser> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found in DB, skipping order simulation.");
            return new int[]{0, 0, 0};
        }

        List<DeliveryPartner> activePartners = deliveryPartnerRepository.findByStatus(DeliveryPartnerStatus.APPROVED);

        Random random = new Random();
        int ordersCreated = 0;
        int shipmentsCreated = 0;
        int feedbacksCreated = 0;

        for (int i = 0; i < count; i++) {
            try {
                AppUser user = users.get(random.nextInt(users.size()));
                long quantity = 1 + random.nextInt(3);
                long total    = product.getPrice() * quantity;

                // Build the Order
                Order order = new Order();
                order.setUser(user);
                order.setStatus(OrderStatus.DELIVERED);
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setTotal(total);
                order.setName(user.getUserName());
                order.setEmail(user.getEmailId());
                order.setAddress("123 Sample Street, Kolkata, West Bengal - 700001");
                order.setPhoneNo(9876543210L);
                order.setPaymentReferenceId("SEEDED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                order.setRetryCount(0);

                // Set historical createdAt timestamp (random month in last 12 months)
                // NOTE: @CreatedDate from JPA auditing will be overridden by manual set via reflection is not needed;
                // instead, we set paymentCompletedAt for analytics purposes
                LocalDateTime historicalDate = LocalDateTime.now()
                        .minusMonths(random.nextInt(12))
                        .minusDays(random.nextInt(28));
                order.setPaymentCompletedAt(historicalDate);
                order.setPaymentInitiatedAt(historicalDate.minusMinutes(5));

                // Build the OrderItem
                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(quantity);
                item.setPrice(product.getPrice());
                order.addItem(item);

                orderRepository.save(order);
                ordersCreated++;

                if (!activePartners.isEmpty()) {
                    DeliveryPartner partner = activePartners.get(random.nextInt(activePartners.size()));

                    Shipment shipment = new Shipment();
                    shipment.setOrder(order);
                    shipment.setDeliveryPartner(partner);
                    shipment.setStatus(ShipmentStatus.DELIVERED);
                    shipment.setTrackingNumber("TRK-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
                    shipment.setExpectedDeliveryDate(historicalDate.toLocalDate().plusDays(random.nextInt(5) + 1));
                    shipmentRepository.save(shipment);
                    shipmentsCreated++;

                    DeliveryFeedback feedback = new DeliveryFeedback();
                    feedback.setOrder(order);
                    feedback.setShipment(shipment);
                    feedback.setCustomer(user);
                    feedback.setDeliveryPartner(partner);
                    feedback.setRating(3 + random.nextInt(3)); // 3, 4, 5
                    feedback.setComment("Good delivery service.");
                    deliveryFeedbackRepository.save(feedback);
                    feedbacksCreated++;
                }

            } catch (Exception e) {
                log.warn("Could not create order #{}: {}", i + 1, e.getMessage());
            }
        }
        log.info("Simulated {} orders, {} shipments, {} feedbacks", ordersCreated, shipmentsCreated, feedbacksCreated);
        return new int[]{ordersCreated, shipmentsCreated, feedbacksCreated};
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    /** Generates a realistic stock level between 10 and 200. */
    private Integer generateRealisticStock() {
        return 10 + new Random().nextInt(191);
    }

    /** Generates a realistic product rating between 3.5 and 5.0. */
    private Double generateRealisticRating() {
        double base = 3.5 + (new Random().nextDouble() * 1.5);
        return Math.round(base * 10.0) / 10.0;
    }

    /**
     * Safely truncates a string to the given max length.
     * Prevents DB column overflow errors for very long scraped descriptions.
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /**
     * Extracts a short, clean product name from the full Amazon title.
     *
     * Amazon titles are typically formatted as:
     *   "Samsung Galaxy M07 Mobile (Black, 4GB RAM, 64GB Storage) | MediaTek... | ..."
     *
     * Strategy:
     *  1. Take only the first segment before '|' (strips feature tags)
     *  2. Take only the part before '(' (strips color/variant details)
     *  3. Trim and limit to first 3 words (Brand + Model Line + Model Number)
     *
     * Examples:
     *   "Samsung Galaxy M07 Mobile (Black...)" → "Samsung Galaxy M07"
     *   "Apple iPhone 15 (128 GB) - Black"     → "Apple iPhone 15"
     *   "OnePlus Nord CE 3 Lite 5G (...)"      → "OnePlus Nord CE"
     */
    private String extractShortName(String fullTitle) {
        if (fullTitle == null || fullTitle.isBlank()) return fullTitle;

        // Step 1: Take only first part before '|'
        String part = fullTitle.split("\\|")[0].trim();

        // Step 2: Take only first part before '('
        if (part.contains("(")) {
            part = part.substring(0, part.indexOf("(")).trim();
        }

        // Step 3: Take only first part before '-' (handles "Apple iPhone 15 - Black" style)
        if (part.contains(" - ")) {
            part = part.substring(0, part.indexOf(" - ")).trim();
        }

        // Step 4: Limit to first 3 words (covers Brand + Series + Model)
        String[] words = part.trim().split("\\s+");
        if (words.length <= 3) return part.trim();
        return words[0] + " " + words[1] + " " + words[2];
    }

    /**
     * Extracts the brand name using a two-tier strategy:
     *  1. Primary: reads 'Brand' or 'Manufacturer' from the scraped specs table.
     *  2. Fallback: uses the first word of the short product name
     *     (for electronics, the first word is almost always the brand:
     *      Samsung, Apple, OnePlus, Xiaomi, boAt, Sony, etc.)
     */
    private String extractBrand(Map<String, String> specs, String shortName) {
        // Tier 1: specs table (most reliable)
        if (specs.containsKey("Brand") && !specs.get("Brand").isBlank()) {
            return specs.get("Brand").trim();
        }
        if (specs.containsKey("Manufacturer") && !specs.get("Manufacturer").isBlank()) {
            // Manufacturer entries sometimes have extra info, just take first word
            return specs.get("Manufacturer").trim().split("\\s+")[0];
        }

        // Tier 2: first word of short name (e.g. "Samsung" from "Samsung Galaxy M07")
        if (shortName != null && !shortName.isBlank()) {
            return shortName.trim().split("\\s+")[0];
        }

        return "Unknown";
    }
}
