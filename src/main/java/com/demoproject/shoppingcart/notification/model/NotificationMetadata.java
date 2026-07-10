package com.demoproject.shoppingcart.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMetadata {
    private String correlationId;
    private String eventId;
    private String traceId;
    private String sourceService;
}
