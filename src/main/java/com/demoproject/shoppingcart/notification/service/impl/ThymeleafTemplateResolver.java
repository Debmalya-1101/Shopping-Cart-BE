package com.demoproject.shoppingcart.notification.service.impl;

import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationType;
import com.demoproject.shoppingcart.notification.service.TemplateResolver;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ThymeleafTemplateResolver implements TemplateResolver {

    @Override
    public String resolve(NotificationType type, NotificationChannel channel) {
        // e.g., ORDER_PLACED via EMAIL -> "email/order-placed"
        String folder = channel.name().toLowerCase(Locale.ROOT);
        String templateName = type.name().toLowerCase(Locale.ROOT).replace("_", "-");
        return folder + "/" + templateName;
    }
}
