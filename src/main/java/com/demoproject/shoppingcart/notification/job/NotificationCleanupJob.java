package com.demoproject.shoppingcart.notification.job;

import com.demoproject.shoppingcart.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    /**
     * Runs every day at 2:00 AM to clean up notifications older than 20 days.
     * This keeps the TiDB database clean and within free tier limits.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications...");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(20);
        int deletedCount = notificationRepository.deleteOlderThan(cutoffDate);
        log.info("Finished cleanup. Deleted {} notifications older than 20 days (cutoff: {}).", deletedCount, cutoffDate);
    }
}
