package com.demoproject.shoppingcart.notification.service;

import com.demoproject.shoppingcart.notification.entity.Notification;
import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationRequest;

public interface NotificationProvider {
    
    /**
     * @return The channel this provider supports.
     */
    NotificationChannel getSupportedChannel();
    
    /**
     * Sends the notification using the specific provider's mechanism.
     * 
     * @param request The original request containing details.
     * @param notification The persisted notification entity containing metadata and status.
     * @throws Exception if sending fails
     */
    void send(NotificationRequest request, Notification notification) throws Exception;
}
