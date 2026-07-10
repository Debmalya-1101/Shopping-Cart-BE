package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogDTO {
    private Long id;
    private Long userId;
    private String referenceId;
    private String type;
    private String channel;
    private String recipient;
    private String subject;
    private String status;
    private Integer retryCount;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
