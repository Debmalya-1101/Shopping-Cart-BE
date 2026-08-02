package com.demoproject.shoppingcart.chatbot.service;

import com.demoproject.shoppingcart.model.UserChatContext;
import com.demoproject.shoppingcart.repository.UserChatContextRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class JpaChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(JpaChatMemory.class);

    private final UserChatContextRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${chat.memory.max-messages:20}")
    private int maxMessages;

    @Value("${chat.session.timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    public JpaChatMemory(UserChatContextRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        Long userId = Long.valueOf(conversationId);
        UserChatContext context = repository.findById(userId).orElse(new UserChatContext());
        context.setUserId(userId);

        // Seamless Session Reset: clear stale context instead of throwing
        if (isSessionExpired(context)) {
            context.setMessages("[]");
        }

        context.setLastAccessed(LocalDateTime.now());

        List<Message> existing = deserializeMessages(context.getMessages());
        List<Message> allMessages = new ArrayList<>(existing);
        allMessages.addAll(messages);

        // Trim to maxMessages
        if (allMessages.size() > maxMessages) {
            allMessages = allMessages.subList(allMessages.size() - maxMessages, allMessages.size());
        }

        context.setMessages(serializeMessages(allMessages));
        repository.save(context);
    }

    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = get(conversationId);
        if (all.size() > lastN) {
            return all.subList(all.size() - lastN, all.size());
        }
        return all;
    }

    @Override
    public List<Message> get(String conversationId) {
        Long userId = Long.valueOf(conversationId);
        return repository.findById(userId)
                .map(context -> {
                    // H1 fix: don't return stale messages from an expired session
                    if (isSessionExpired(context)) {
                        return new ArrayList<Message>();
                    }
                    return deserializeMessages(context.getMessages());
                })
                .orElse(new ArrayList<>());
    }

    @Override
    public void clear(String conversationId) {
        repository.deleteById(Long.valueOf(conversationId));
    }

    private boolean isSessionExpired(UserChatContext context) {
        return context.getLastAccessed() != null &&
               context.getLastAccessed().plusMinutes(sessionTimeoutMinutes).isBefore(LocalDateTime.now());
    }

    // ── Slim serialization: stores only role + content, avoids AbstractMessage polymorphism ──

    private String serializeMessages(List<Message> messages) {
        try {
            List<Map<String, String>> slim = messages.stream()
                    .map(m -> Map.of(
                            "role", m.getMessageType().getValue(),
                            "content", m.getText() != null ? m.getText() : ""))
                    .toList();
            return objectMapper.writeValueAsString(slim);
        } catch (Exception e) {
            log.error("Failed to serialize chat memory", e);
            return "[]";
        }
    }

    private List<Message> deserializeMessages(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<Map<String, String>> list = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, String>>>() {});
            return list.stream().<Message>map(m -> {
                String content = m.getOrDefault("content", "");
                return switch (m.getOrDefault("role", "user")) {
                    case "assistant" -> new AssistantMessage(content);
                    case "system"    -> new SystemMessage(content);
                    default          -> new UserMessage(content);
                };
            }).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.error("Failed to deserialize chat memory, starting fresh", e);
            return new ArrayList<>();
        }
    }
}
