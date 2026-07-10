package com.demoproject.shoppingcart.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private NotificationType type;
    private NotificationChannel channel;
    private String referenceId;
    private Long userId;
    
    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    
    private String subject;
    private Map<String, Object> payload;
    private NotificationMetadata metadata;
}
