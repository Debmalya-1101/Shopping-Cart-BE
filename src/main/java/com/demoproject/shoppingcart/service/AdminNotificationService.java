package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.NotificationLogDTO;
import org.springframework.data.domain.Page;

public interface AdminNotificationService {
    Page<NotificationLogDTO> getNotificationLogs(int page, int size, String status);
}
