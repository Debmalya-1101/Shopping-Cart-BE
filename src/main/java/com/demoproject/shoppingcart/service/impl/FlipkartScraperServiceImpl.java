package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.FlipkartScrapeRequest;
import com.demoproject.shoppingcart.dto.FlipkartScrapeResultDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.*;
import com.demoproject.shoppingcart.service.FlipkartScraperService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flipkart Scraper Service Implementation.
 *
 * Uses Microsoft Playwright (headless Chromium) to load Flipkart product pages,
 * extract product data, format paragraphs as bullets, and persist it across database tables.
 */
@Service
@Profile("!dev")
public class FlipkartScraperServiceImpl implements FlipkartScraperService {

    private static final Logger log = LoggerFactory.getLogger(FlipkartScraperServiceImpl.class);

    // Flipkart product URL pattern
    private static final String FLIPKART_PRODUCT_URL = "https://www.flipkart.com/product-name/p/";

    // Pool of realistic reviews for simulation
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
    private final TransactionTemplate transactionTemplate;

    public FlipkartScraperServiceImpl(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            AttributeKeyRepository attributeKeyRepository,
            ProductAttributeRepository productAttributeRepository,
            ReviewRepository reviewRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.attributeKeyRepository = attributeKeyRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // =====================================================================
    // PUBLIC ENTRY POINT
    // =====================================================================

    @Override
    public List<FlipkartScrapeResultDTO> scrapeAndSave(FlipkartScrapeRequest request) {
        List<FlipkartScrapeResultDTO> results = new ArrayList<>();
        List<String> fsns = request.getFsns();
        String categoryName = request.getCategoryName().trim();

        for (int i = 0; i < fsns.size(); i++) {
            String fsn = fsns.get(i).trim();
            if (i > 0) {
                log.info("Waiting 5 seconds before scraping the next FSN: {}...", fsn);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Scraping delay sleep interrupted: {}", e.getMessage());
                }
            }

            FlipkartScrapeResultDTO result = scrapeSingleFsn(fsn, categoryName, request);
            results.add(result);
        }
        return results;
    }

    // =====================================================================
    // SINGLE ASIN/FSN SCRAPING
    // =====================================================================

    private FlipkartScrapeResultDTO scrapeSingleFsn(String fsn, String categoryName, FlipkartScrapeRequest request) {
        String targetUrl = FLIPKART_PRODUCT_URL + fsn;

        log.info("=== Starting Flipkart Scrape: FSN={}, Category={} ===", fsn, categoryName);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true).setSlowMo(200)
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Safari/537.36")
                            .setViewportSize(1280, 800)
                            .setLocale("en-IN")
                            .setTimezoneId("Asia/Kolkata")
            );

            Page page = context.newPage();

            log.info("Navigating to: {}", targetUrl);
            page.navigate(targetUrl, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30_000));

            // Wait briefly for details to render
            page.waitForTimeout(2000);

            // Check for bot detection or CAPTCHA pages
            String pageTitle = page.title();
            if (pageTitle.toLowerCase().contains("robot check") ||
                    pageTitle.toLowerCase().contains("sorry") ||
                    page.url().contains("captcha")) {
                browser.close();
                log.warn("CAPTCHA / Bot detection triggered for FSN: {}", fsn);
                return FlipkartScrapeResultDTO.builder()
                        .status("FAILED")
                        .fsn(fsn)
                        .message("Flipkart returned a verification / captcha page. Please retry later.")
                        .build();
            }

            // Extract Flipkart data fields using fallback selectors
            String title = extractTitle(page);
            Long priceInr = extractPrice(page);
            String rawDescription = extractDescription(page);
            String description = rawDescription;
            String mainImageUrl = extractMainImage(page);
            List<String> gallery = extractGalleryImages(page, mainImageUrl);
            Map<String, String> specs = extractSpecifications(page);

            browser.close();
            log.info("Scrape complete. Title='{}', Price=₹{}, Images={}, Specs={}",
                    title, priceInr, gallery.size(), specs.size());

            if (title == null || title.isBlank()) {
                return FlipkartScrapeResultDTO.builder()
                        .status("FAILED")
                        .fsn(fsn)
                        .message("Could not extract product title. The page layout may have changed or the FSN is invalid.")
                        .build();
            }

            // Save inside a clean database transaction
            return transactionTemplate.execute(status ->
                    persistToDatabase(fsn, categoryName, title, priceInr, description,
                            mainImageUrl, gallery, specs, request)
            );

        } catch (Exception e) {
            log.error("Scraping failed for FSN {}: {}", fsn, e.getMessage(), e);
            return FlipkartScrapeResultDTO.builder()
                    .status("FAILED")
                    .fsn(fsn)
                    .message("Scraping error: " + e.getMessage())
                    .build();
        }
    }

    // =====================================================================
    // EXTRACTION METHOD IMPLEMENTATIONS
    // =====================================================================

    private String extractTitle(Page page) {
        // Attempt to get clean title from page.title() first to avoid truncation
        try {
            String title = page.title();
            if (title != null && !title.isEmpty()) {
                // Remove generic suffix like "Rs.22990 Price in India - Buy ..."
                title = title.replaceAll(" Rs\\..*Price in India.*", "").trim();
                title = title.replaceAll(" - Buy .*Online.*", "").trim();
                title = title.replaceAll(" : Flipkart\\.com$", "").trim();
                if (!title.isEmpty()) {
                    return title;
                }
            }
        } catch (Exception ignored) {}

        String[] selectors = { "span.B_NuCI", "span.VU-ZEG", "h1.yhB1nd", "h1" };
        for (String selector : selectors) {
            try {
                Locator el = page.locator(selector);
                if (el.count() > 0) {
                    String title = el.first().innerText().trim();
                    if (!title.isEmpty()) return title;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Long extractPrice(Page page) {
        // First try specific known classes
        String[] selectors = {
                "div.Nx9bqj.CxhGGd",
                "div.Nx9bqj",
                "div[class*='Nx9bqj']",
                "div.hlb39e",
                "div._30jeq3",
                "span[class*='price']"
        };
        for (String selector : selectors) {
            try {
                Locator el = page.locator(selector);
                if (el.count() > 0) {
                    for (int i = 0; i < el.count(); i++) {
                        String raw = el.nth(i).innerText().trim();
                        if (raw.startsWith("₹") && raw.length() < 15 && !raw.contains("\n")) {
                            String cleaned = raw.replaceAll("[₹,\\s]", "")
                                    .replaceAll("\\.\\d+$", "").trim();
                            if (!cleaned.isEmpty()) {
                                return Long.parseLong(cleaned);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        // Fallback: search all divs for the first short price-like text
        try {
            List<Locator> els = page.locator("div").all();
            for (Locator el : els) {
                try {
                    String raw = el.innerText().trim();
                    if (raw.startsWith("₹") && raw.length() < 15 && !raw.contains("\n")) {
                        String cleaned = raw.replaceAll("[₹,\\s]", "")
                                .replaceAll("\\.\\d+$", "").trim();
                        if (!cleaned.isEmpty()) {
                            return Long.parseLong(cleaned);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        log.warn("Price extraction failed, defaulting to 0");
        return 0L;
    }

    private String extractDescription(Page page) {
        // Look for typical description header and get the sibling or parent text
        try {
            Locator descHeader = page.locator("div:has-text('Description'), div:has-text('Product Description')").last();
            if (descHeader.count() > 0) {
                String parentText = descHeader.locator("xpath=..").innerText();
                if (parentText != null && parentText.length() > 20) {
                    return parentText.replace("Description", "").trim();
                }
            }
        } catch (Exception ignored) {}
        
        // Try fallback selectors
        String[] selectors = {
                "div.RmwPPm",
                "div._2415tS",
                "div._1mXF1G",
                "div[class*='product-description']"
        };
        for (String selector : selectors) {
            try {
                Locator el = page.locator(selector);
                if (el.count() > 0) {
                    String text = el.first().innerText().trim();
                    if (!text.isEmpty()) return text;
                }
            } catch (Exception ignored) {}
        }
        
        // Final fallback: Look for a generic long text block
        try {
            List<Locator> divs = page.locator("div").all();
            for (Locator d : divs) {
                try {
                    String t = d.innerText();
                    if (t != null && t.length() > 200 && t.length() < 2000 && !t.contains("₹") && !t.contains("Reviews")) {
                        return t.trim();
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        return "No description available.";
    }

    private String extractMainImage(Page page) {
        try {
            List<Locator> imgs = page.locator("img").all();
            Pattern pattern = Pattern.compile("/image/(\\d+)/(\\d+)/");
            for (Locator img : imgs) {
                try {
                    String src = img.getAttribute("src");
                    if (src != null && src.contains("flixcart.com/image/") && src.contains("-original-")) {
                        Matcher matcher = pattern.matcher(src);
                        if (matcher.find()) {
                            int w = Integer.parseInt(matcher.group(1));
                            int h = Integer.parseInt(matcher.group(2));
                            if ((w <= 200 && h <= 200) || (w >= 600 || h >= 600)) {
                                return src.replaceAll("/image/\\d+/\\d+/", "/image/832/832/");
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Main image extraction failed: {}", e.getMessage());
        }
        return null;
    }

    private List<String> extractGalleryImages(Page page, String mainImageUrl) {
        Set<String> seen = new LinkedHashSet<>();
        try {
            List<Locator> imgs = page.locator("img").all();
            Pattern pattern = Pattern.compile("/image/(\\d+)/(\\d+)/");
            for (Locator img : imgs) {
                try {
                    String src = img.getAttribute("src");
                    if (src != null && src.contains("flixcart.com/image/") && src.contains("-original-")) {
                        Matcher matcher = pattern.matcher(src);
                        if (matcher.find()) {
                            int w = Integer.parseInt(matcher.group(1));
                            int h = Integer.parseInt(matcher.group(2));
                            if ((w <= 200 && h <= 200) || (w >= 600 || h >= 600)) {
                                String hiRes = src.replaceAll("/image/\\d+/\\d+/", "/image/832/832/");
                                seen.add(hiRes);
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Gallery image extraction failed: {}", e.getMessage());
        }

        List<String> images = new ArrayList<>(seen);
        return images;
    }

    private Map<String, String> extractSpecifications(Page page) {
        Map<String, String> specs = new LinkedHashMap<>();
        String[] rowSelectors = { "tr._1sDu2A", "._3k-BhJ tr", "table[class*='spec'] tr" };

        for (String selector : rowSelectors) {
            try {
                Locator rows = page.locator(selector);
                if (rows.count() == 0) continue;

                for (int i = 0; i < Math.min(rows.count(), 25); i++) {
                    try {
                        Locator row = rows.nth(i);
                        // Key is typically first td, value is the second td
                        Locator tdList = row.locator("td");
                        if (tdList.count() >= 2) {
                            String key = tdList.nth(0).innerText().trim()
                                    .replaceAll("[\\n\\t]", " ").replaceAll("\\s+", " ");
                            String value = tdList.nth(1).innerText().trim()
                                    .replaceAll("[\\n\\t]", " ").replaceAll("\\s+", " ");

                            if (!key.isEmpty() && !value.isEmpty()) {
                                specs.put(key, value);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (!specs.isEmpty()) break;
            } catch (Exception e) {
                log.warn("Spec selector '{}' failed: {}", selector, e.getMessage());
            }
        }
        return specs;
    }

    // =====================================================================
    // HELPER UTILITIES
    // =====================================================================

    /**
     * Converts a plain text product description paragraph into clean, reader-friendly
     * bullet points by splitting sentences and prefixing them with a bullet symbol.
     */
    private String formatAsBulletPoints(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        // If it already has standard formatting, preserve it
        if (text.contains("•") || text.contains("\n*") || text.contains("\n-")) {
            return text;
        }

        // Split by periods followed by spaces OR newlines to capture paragraph splits as well
        String[] sentences = text.split("(?<=\\.)\\s+|\\n+");
        StringBuilder sb = new StringBuilder();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (!trimmed.isEmpty()) {
                // Strip existing leading bullet characters if they are there
                trimmed = trimmed.replaceFirst("^[•\\-*\\s]+", "");
                if (!trimmed.isEmpty()) {
                    sb.append("• ").append(trimmed);
                    // Add period if it doesn't end with punctuation
                    if (!trimmed.matches(".*[.!?]$")) {
                        sb.append(".");
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private String extractShortName(String fullTitle) {
        if (fullTitle == null || fullTitle.isBlank()) return fullTitle;

        // Flipkart titles format: "SAMSUNG Galaxy S24 5G (Amber Yellow, 256 GB)  (8 GB RAM)"
        String part = fullTitle;
        if (part.contains("(")) {
            part = part.substring(0, part.indexOf("(")).trim();
        }
        if (part.contains(" - ")) {
            part = part.substring(0, part.indexOf(" - ")).trim();
        }

        String[] words = part.trim().split("\\s+");
        if (words.length <= 3) return part.trim();
        return words[0] + " " + words[1] + " " + words[2];
    }

    private String extractBrand(Map<String, String> specs, String shortName) {
        if (specs.containsKey("Brand") && !specs.get("Brand").isEmpty()) {
            return specs.get("Brand").trim();
        }
        if (shortName != null && !shortName.isEmpty()) {
            return shortName.trim().split("\\s+")[0];
        }
        return "Unknown";
    }

    private Integer generateRealisticStock() {
        return 10 + new Random().nextInt(191);
    }

    private Double generateRealisticRating() {
        double base = 3.5 + (new Random().nextDouble() * 1.5);
        return Math.round(base * 10.0) / 10.0;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    // =====================================================================
    // DATABASE PERSISTENCE & TRANSACTION LOGIC
    // =====================================================================

    private FlipkartScrapeResultDTO persistToDatabase(
            String fsn, String categoryName,
            String title, Long priceInr, String description,
            String mainImageUrl, List<String> gallery,
            Map<String, String> specs, FlipkartScrapeRequest request) {

        // 1. Resolve or create Category
        Category category = categoryRepository.findByName(categoryName)
                .orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName(categoryName);
                    Category saved = categoryRepository.save(newCat);
                    log.info("Created new category: '{}'", categoryName);
                    return saved;
                });

        // 2. Create and save Product
        String shortName = extractShortName(title);
        String brand = extractBrand(specs, shortName);

        Product product = new Product();
        product.setName(shortName);
        product.setFullName(title);
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

        // 3. Save Product Images
        int imagesInserted = 0;
        for (String imageUrl : gallery) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ProductImage pi = new ProductImage();
                pi.setImageUrl(imageUrl);
                pi.setProduct(product);
                productImageRepository.save(pi);
                imagesInserted++;
            }
        }

        // 4. Save specifications/attributes
        int attributesInserted = 0;
        if (!specs.isEmpty()) {
            for (Map.Entry<String, String> entry : specs.entrySet()) {
                String specKey = entry.getKey();
                String specValue = entry.getValue();

                final Category finalCategory = category;
                AttributeKey attrKey = resolveAttributeKey(specKey, finalCategory);

                ProductAttribute attrVal = new ProductAttribute();
                attrVal.setProduct(product);
                attrVal.setAttributeKey(attrKey);
                attrVal.setValueText(specValue);

                try {
                    double numeric = Double.parseDouble(specValue.replaceAll("[^\\d.]", ""));
                    attrVal.setValueNumber(numeric);
                } catch (NumberFormatException ignored) {}

                productAttributeRepository.save(attrVal);
                attributesInserted++;
            }
        }

        // 5. Simulate Mock Reviews
        int reviewsCreated = simulateReviews(product, request.getSimulatedReviews());
        product.setRatingCount((long) reviewsCreated);
        product = productRepository.save(product);

        // 6. Simulate Sales orders
        int ordersCreated = simulateOrders(product, request.getSimulatedOrders());

        // 7. Return Result DTO
        return FlipkartScrapeResultDTO.builder()
                .status("SUCCESS")
                .fsn(fsn)
                .message("Product successfully scraped from Flipkart India and saved to database.")
                .productId(product.getId())
                .productName(product.getName())
                .fullProductName(product.getFullName())
                .priceInr(product.getPrice())
                .category(category.getName())
                .mainImageUrl(mainImageUrl)
                .galleryImageUrls(gallery)
                .imagesInserted(imagesInserted)
                .attributesInserted(attributesInserted)
                .reviewsSimulated(reviewsCreated)
                .ordersSimulated(ordersCreated)
                .build();
    }

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
                if (reviewRepository.existsByProductAndUser(product, user)) {
                    continue;
                }
                ProductReview review = new ProductReview();
                review.setProduct(product);
                review.setUser(user);
                review.setRating(3 + random.nextInt(3));
                review.setReviewText(REVIEW_POOL.get(random.nextInt(REVIEW_POOL.size())));
                reviewRepository.save(review);
                created++;
            } catch (Exception e) {
                log.warn("Could not create review for user {}: {}", user.getId(), e.getMessage());
            }
        }
        return created;
    }

    private int simulateOrders(Product product, int count) {
        List<AppUser> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found in DB, skipping order simulation.");
            return 0;
        }

        Random random = new Random();
        int created = 0;

        for (int i = 0; i < count; i++) {
            try {
                AppUser user = users.get(random.nextInt(users.size()));
                long quantity = 1 + random.nextInt(3);
                long total = product.getPrice() * quantity;

                Order order = new Order();
                order.setUser(user);
                order.setStatus(OrderStatus.DELIVERED);
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setTotal(total);
                order.setName(user.getUserName());
                order.setEmail(user.getEmailId());
                order.setAddress("123 Flipkart Seed Street, Bangalore, Karnataka - 560001");
                order.setPhoneNo(9876543210L);
                order.setPaymentReferenceId("FK-SEED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                order.setRetryCount(0);

                LocalDateTime historicalDate = LocalDateTime.now()
                        .minusMonths(random.nextInt(12))
                        .minusDays(random.nextInt(28));
                order.setPaymentCompletedAt(historicalDate);
                order.setPaymentInitiatedAt(historicalDate.minusMinutes(5));

                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(quantity);
                item.setPrice(product.getPrice());
                order.addItem(item);

                orderRepository.save(order);
                created++;
            } catch (Exception e) {
                log.warn("Could not create Flipkart simulated order: {}", e.getMessage());
            }
        }
        return created;
    }
}
