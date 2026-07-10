package com.demoproject.shoppingcart.notification.service;

import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationType;

public interface TemplateResolver {
    /**
     * Resolves the template path for a given notification type and channel.
     * 
     * @param type the notification type
     * @param channel the channel (e.g. EMAIL)
     * @return the template path (e.g. "email/order-placed")
     */
    String resolve(NotificationType type, NotificationChannel channel);
}
