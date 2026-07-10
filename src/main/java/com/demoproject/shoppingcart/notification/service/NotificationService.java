package com.demoproject.shoppingcart.notification.service;

import com.demoproject.shoppingcart.notification.model.NotificationRequest;

public interface NotificationService {
    
    /**
     * Processes a notification request. Persists it to the database and attempts to send it
     * via the appropriate channel provider.
     * 
     * @param request the request detailing the notification to be sent
     */
    void send(NotificationRequest request);
}
