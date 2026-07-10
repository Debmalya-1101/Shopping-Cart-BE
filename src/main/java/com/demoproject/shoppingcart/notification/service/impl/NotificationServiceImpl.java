package com.demoproject.shoppingcart.notification.service.impl;

import com.demoproject.shoppingcart.notification.entity.Notification;
import com.demoproject.shoppingcart.notification.model.NotificationRequest;
import com.demoproject.shoppingcart.notification.model.NotificationStatus;
import com.demoproject.shoppingcart.notification.repository.NotificationRepository;
import com.demoproject.shoppingcart.notification.service.NotificationProvider;
import com.demoproject.shoppingcart.notification.service.NotificationService;
import com.demoproject.shoppingcart.notification.util.NotificationExceptionClassifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationProviderRegistry providerRegistry;
    private final NotificationExceptionClassifier exceptionClassifier;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public void send(NotificationRequest request) {
        String correlationId = request.getMetadata() != null ? request.getMetadata().getCorrelationId() : "N/A";
        String traceId = request.getMetadata() != null ? request.getMetadata().getTraceId() : "N/A";
        
        try (MDC.MDCCloseable cId = MDC.putCloseable("correlationId", correlationId);
             MDC.MDCCloseable tId = MDC.putCloseable("traceId", traceId)) {

            String idempotencyKey = generateIdempotencyKey(request);
            
            // 1. Check idempotency
            if (notificationRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                log.info("Duplicate notification detected for key: {}. Skipping.", idempotencyKey);
                return;
            }

            // 2. Persist initial state
            Notification notification = createAndSaveNotification(request, idempotencyKey);

            // 3. Find Provider
            NotificationProvider provider = providerRegistry.getProvider(request.getChannel());

            // 4. Send
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                provider.send(request, notification);
                
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
                
                meterRegistry.counter("notifications.sent", "channel", request.getChannel().name()).increment();
                log.info("Notification successfully sent to {}", request.getTo());
                
            } catch (Exception e) {
                handleFailure(notification, e, request.getChannel().name());
            } finally {
                sample.stop(meterRegistry.timer("notifications.duration", "channel", request.getChannel().name()));
            }
            
        } catch (DataIntegrityViolationException e) {
            // Handled concurrent duplicate inserts
            log.warn("Concurrent duplicate notification detected. Skipping.");
        }
    }

    private String generateIdempotencyKey(NotificationRequest request) {
        return request.getReferenceId() + "_" + request.getType().name() + "_" + request.getChannel().name();
    }

    private Notification createAndSaveNotification(NotificationRequest request, String idempotencyKey) {
        String metadataJson = null;
        try {
            if (request.getMetadata() != null) {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata", e);
        }

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .referenceId(request.getReferenceId())
                .idempotencyKey(idempotencyKey)
                .type(request.getType())
                .channel(request.getChannel())
                .to(request.getTo() != null ? String.join(",", request.getTo()) : "")
                .cc(request.getCc() != null ? String.join(",", request.getCc()) : "")
                .bcc(request.getBcc() != null ? String.join(",", request.getBcc()) : "")
                .subject(request.getSubject())
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .metadata(metadataJson)
                .build();
                
        return notificationRepository.save(notification);
    }

    private void handleFailure(Notification notification, Exception e, String channel) {
        notification.setFailureReason(e.getMessage());
        
        if (exceptionClassifier.isRetryable(e)) {
            // In a full implementation, you would use Spring Retry or place it in a retry queue here.
            // For now, we update the status and count.
            notification.setRetryCount(notification.getRetryCount() + 1);
            // If it exceeds max retries, mark FAILED, else keep PENDING or define a RETRY_SCHEDULED status
            notification.setStatus(NotificationStatus.FAILED); // Simplified for this demo
            log.warn("Retryable failure for notification ID {}: {}", notification.getId(), e.getMessage());
            meterRegistry.counter("notifications.failed", "channel", channel, "reason", "retryable").increment();
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            log.error("Terminal failure for notification ID {}: {}", notification.getId(), e.getMessage());
            meterRegistry.counter("notifications.failed", "channel", channel, "reason", "terminal").increment();
        }
        
        notificationRepository.save(notification);
    }
}
