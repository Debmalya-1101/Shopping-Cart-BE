package com.demoproject.shoppingcart.chatbot.controller;

import com.demoproject.shoppingcart.chatbot.dto.ChatRequest;
import com.demoproject.shoppingcart.chatbot.service.ChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<String> chat(@RequestBody ChatRequest request) {
        try {
            String response = chatbotService.chat(request.message());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Chatbot error", e);
            // Return 200 so the frontend treats this as a normal bot reply
            // rather than a connection failure
            return ResponseEntity.ok(
                    "I ran into an issue while processing your request. Please try again, or type `/clear` to reset our conversation context and start fresh.");
        }
    }
}
