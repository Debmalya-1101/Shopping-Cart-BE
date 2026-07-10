package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.NotificationLogDTO;
import com.demoproject.shoppingcart.service.AdminNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    public AdminNotificationController(AdminNotificationService adminNotificationService) {
        this.adminNotificationService = adminNotificationService;
    }

    /**
     * Fetch paginated notification logs for auditing.
     * @param page page number (0-based)
     * @param size page size
     * @param status optional status filter (e.g. SENT, FAILED, PENDING)
     * @return Paginated Notification Logs
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<NotificationLogDTO>> getNotificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Page<NotificationLogDTO> logs = adminNotificationService.getNotificationLogs(page, size, status);
        return ResponseEntity.ok(logs);
    }
}
