package com.demoproject.shoppingcart.chatbot.config;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.demoproject.shoppingcart.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ChatbotTools {

    private static final Logger log = LoggerFactory.getLogger(ChatbotTools.class);

    private final ProductService productService;
    private final com.demoproject.shoppingcart.service.CartService cartService;
    private final com.demoproject.shoppingcart.service.OrderService orderService;
    private final com.demoproject.shoppingcart.service.WishlistService wishlistService;
    private final com.demoproject.shoppingcart.service.AddressService addressService;
    private final com.demoproject.shoppingcart.service.PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public ChatbotTools(ProductService productService,
                        com.demoproject.shoppingcart.service.CartService cartService,
                        com.demoproject.shoppingcart.service.OrderService orderService,
                        com.demoproject.shoppingcart.service.WishlistService wishlistService,
                        com.demoproject.shoppingcart.service.AddressService addressService,
                        com.demoproject.shoppingcart.service.PaymentService paymentService,
                        ObjectMapper objectMapper) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.wishlistService = wishlistService;
        this.addressService = addressService;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    // ponytail: every tool returns String so exceptions become error text the LLM can
    // reason about instead of crashing Spring AI's internal tool-call pipeline.

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    @Tool(description = "Get details of a specific product using its unique ID.")
    public String getProductDetails(Long id) {
        try {
            return toJson(productService.getProductById(id));
        } catch (Exception e) {
            log.warn("getProductDetails failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public record ProductSearchRequest(
            @JsonPropertyDescription("The name or keyword of the product to search for, e.g., 'shoes' or 'laptop'")
            String search,
            @JsonPropertyDescription("The category of the product, e.g., 'Electronics' or 'Clothing'")
            String category,
            @JsonPropertyDescription("The brand of the product, e.g., 'Samsung' or 'Apple'")
            String brand,
            @JsonPropertyDescription("The minimum price filter in rupees")
            Long minPrice,
            @JsonPropertyDescription("The maximum price filter in rupees")
            Long maxPrice
    ) {}

    @Tool(description = "Search for products by name, category, or brand. Returns a list of matching products with their IDs. Use this tool first when a user asks about a product without providing an ID.")
    public String searchProducts(ProductSearchRequest request) {
        try {
            return toJson(productService.getAllProducts(
                    0, 10,
                    request.category(),
                    request.brand(),
                    request.search(),
                    request.minPrice(),
                    request.maxPrice(),
                    null, null
            ));
        } catch (Exception e) {
            log.warn("searchProducts failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // --- Cart Tools ---

    @Tool(description = "View the user's current shopping cart, including all items and the total price.")
    public String viewCart() {
        try {
            return toJson(cartService.getCart());
        } catch (Exception e) {
            log.warn("viewCart failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Add a specific product to the user's shopping cart.")
    public String addToCart(Long productId, Long quantity) {
        try {
            Long qty = quantity != null && quantity > 0 ? quantity : 1L;
            com.demoproject.shoppingcart.dto.AddToCartRequest req = new com.demoproject.shoppingcart.dto.AddToCartRequest();
            req.setProductId(productId);
            req.setQuantity(qty);
            return toJson(cartService.addToCart(req));
        } catch (Exception e) {
            log.warn("addToCart failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Remove a specific item from the user's shopping cart using the cart item ID.")
    public String removeCartItem(Long cartItemId) {
        try {
            return toJson(cartService.removeItem(cartItemId));
        } catch (Exception e) {
            log.warn("removeCartItem failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Clear all items from the user's shopping cart.")
    public String clearCart() {
        try {
            cartService.clearCart();
            return "Cart has been cleared successfully.";
        } catch (Exception e) {
            log.warn("clearCart failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // --- Address Tools ---

    @Tool(description = "Get a list of all saved shipping addresses for the user.")
    public String getMyAddresses() {
        try {
            return toJson(addressService.getMyAddresses());
        } catch (Exception e) {
            log.warn("getMyAddresses failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // --- Order Tools ---

    @Tool(description = "Proceed to checkout and place an order using a specified shipping address ID. This is the first step of checkout.")
    public String checkout(Long addressId) {
        try {
            com.demoproject.shoppingcart.dto.CheckoutRequestDTO req = new com.demoproject.shoppingcart.dto.CheckoutRequestDTO();
            req.setAddressId(addressId);
            return toJson(orderService.checkout(req));
        } catch (Exception e) {
            log.warn("checkout failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get a paginated list of the user's past orders.")
    public String getMyOrders() {
        try {
            return toJson(orderService.getMyOrders(0, 10));
        } catch (Exception e) {
            log.warn("getMyOrders failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get the full details of a specific order by its order ID.")
    public String getOrderDetails(Long orderId) {
        try {
            return toJson(orderService.getOrderById(orderId));
        } catch (Exception e) {
            log.warn("getOrderDetails failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Cancel a specific order by its order ID.")
    public String cancelOrder(Long orderId) {
        try {
            return toJson(orderService.cancelOrder(orderId));
        } catch (Exception e) {
            log.warn("cancelOrder failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // --- Payment Tools ---

    @Tool(description = "Initiate payment for an order. This generates the Razorpay token required for the user to pay. This is the second step of checkout.")
    public String initiatePayment(Long orderId) {
        try {
            paymentService.initiatePayment(orderId);
            return "Payment initiated successfully for order " + orderId + ". Please provide the payment URL to the user.";
        } catch (Exception e) {
            log.warn("initiatePayment failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // --- Wishlist Tools ---

    @Tool(description = "View the user's current wishlist.")
    public String viewWishlist() {
        try {
            return toJson(wishlistService.getWishlist());
        } catch (Exception e) {
            log.warn("viewWishlist failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Toggle (add or remove) a specific product in the user's wishlist.")
    public String toggleWishlist(Long productId) {
        try {
            return toJson(wishlistService.toggleWishlist(productId));
        } catch (Exception e) {
            log.warn("toggleWishlist failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
