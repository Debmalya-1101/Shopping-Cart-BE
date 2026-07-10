package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.NotificationLogDTO;
import com.demoproject.shoppingcart.notification.entity.Notification;
import com.demoproject.shoppingcart.notification.model.NotificationStatus;
import com.demoproject.shoppingcart.notification.repository.NotificationRepository;
import com.demoproject.shoppingcart.service.AdminNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationRepository notificationRepository;

    public AdminNotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Page<NotificationLogDTO> getNotificationLogs(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifications;

        if (status != null && !status.isEmpty()) {
            try {
                NotificationStatus notifStatus = NotificationStatus.valueOf(status.toUpperCase());
                notifications = notificationRepository.findByStatus(notifStatus, pageable);
            } catch (IllegalArgumentException e) {
                // fallback to all if invalid status
                notifications = notificationRepository.findAll(pageable);
            }
        } else {
            notifications = notificationRepository.findAll(pageable);
        }

        return notifications.map(this::mapToDTO);
    }

    private NotificationLogDTO mapToDTO(Notification n) {
        return NotificationLogDTO.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .referenceId(n.getReferenceId())
                .type(n.getType() != null ? n.getType().name() : null)
                .channel(n.getChannel() != null ? n.getChannel().name() : null)
                .recipient(n.getTo())
                .subject(n.getSubject())
                .status(n.getStatus() != null ? n.getStatus().name() : null)
                .retryCount(n.getRetryCount())
                .failureReason(n.getFailureReason())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}
