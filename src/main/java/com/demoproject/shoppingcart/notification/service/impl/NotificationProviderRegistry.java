package com.demoproject.shoppingcart.notification.service.impl;

import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.service.NotificationProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProviderRegistry {

    private final List<NotificationProvider> providers;
    private final Map<NotificationChannel, NotificationProvider> registry = new EnumMap<>(NotificationChannel.class);

    @PostConstruct
    public void init() {
        for (NotificationProvider provider : providers) {
            registry.put(provider.getSupportedChannel(), provider);
            log.info("Registered NotificationProvider for channel: {}", provider.getSupportedChannel());
        }
    }

    public NotificationProvider getProvider(NotificationChannel channel) {
        NotificationProvider provider = registry.get(channel);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for channel: " + channel);
        }
        return provider;
    }
}
