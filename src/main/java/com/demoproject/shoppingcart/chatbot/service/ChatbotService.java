package com.demoproject.shoppingcart.chatbot.service;

import com.demoproject.shoppingcart.chatbot.config.ChatbotTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.model.AppUser;

@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final JpaChatMemory jpaChatMemory;

    public ChatbotService(ChatClient.Builder chatClientBuilder, ChatbotTools chatbotTools,
                          JpaChatMemory jpaChatMemory, UserRepository userRepository,
                          @Value("${app.frontend.url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jpaChatMemory = jpaChatMemory;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are an expert e-commerce assistant for a shopping cart application.
                        Your primary goal is to help users discover products, manage their cart and orders, check details, and make purchasing decisions.
                        
                        CRITICAL INSTRUCTIONS:
                        1. SEARCHING & CART CLARIFICATION: If a user asks for a product generically (e.g., "Add a Samsung phone"), you MUST use the `searchProducts` tool first. Present the matching products with their names and prices, and ask the user to specify exactly which one they want before calling `addToCart`.
                        2. DETAILS: If you need more information about a specific product (such as exact price, ratings, or full description), use the `getProductDetails` tool using the ID obtained from the search results.
                        3. CHECKOUT FLOW: When a user wants to checkout:
                           - First, call `viewCart()` to summarize what they're buying and the total.
                           - Ask for explicit confirmation.
                           - Call `getMyAddresses()` to list their saved addresses and ask which one to ship to.
                           - Call `checkout(addressId)` to place the order.
                           - Finally, call `initiatePayment(orderId)` to generate the payment token, and present the payment URL (%s/payment/<the-order-id>) to the user, warning them they have 15 minutes to pay.
                        4. CONFIRMATIONS: NEVER execute destructive or irreversible actions (`checkout`, `cancelOrder`, `clearCart`) without asking the user for explicit confirmation first.
                        5. ERROR HANDLING & RECOVERY: If any tool throws a RuntimeException or you encounter an issue, relay the error message politely to the user without exposing technical stack traces. If the user encounters repeated issues or unexpected state, advise them that they can type `/clear` anytime to reset the chat memory and start fresh.
                        6. TONE & SCOPE: Be polite, concise, and helpful. Format your responses clearly using bullet points for lists. You can only help with product search, cart, wishlist, orders, and payments. Politely decline requests outside this scope.
                        7. PRICING: All prices returned by tools are in WHOLE Indian Rupees (₹). Do NOT divide prices by 100. For example, a price of 21999 means ₹21,999, NOT ₹219.99. Always format prices with the ₹ symbol and comma separators for thousands.
                        
                        Think step-by-step. Use tools when necessary rather than guessing.
                        """.formatted(frontendUrl))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(jpaChatMemory).build())
                .defaultTools(chatbotTools)
                .build();
    }

    public String chat(String message) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String conversationId = String.valueOf(user.getId());

        if ("/clear".equalsIgnoreCase(message.trim())) {
            jpaChatMemory.clear(conversationId);
            return "Your chat memory has been cleared. You can start a fresh conversation now!";
        }

        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
