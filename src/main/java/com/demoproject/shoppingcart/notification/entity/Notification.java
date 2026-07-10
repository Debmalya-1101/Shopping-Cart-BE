package com.demoproject.shoppingcart.notification.entity;

import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationStatus;
import com.demoproject.shoppingcart.notification.model.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "recipient_to", nullable = false, length = 1000)
    private String to;

    @Column(name = "recipient_cc", length = 1000)
    private String cc;

    @Column(name = "recipient_bcc", length = 1000)
    private String bcc;

    @Column(length = 500)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
