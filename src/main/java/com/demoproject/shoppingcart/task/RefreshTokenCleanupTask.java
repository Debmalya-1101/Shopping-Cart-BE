package com.demoproject.shoppingcart.task;

import com.demoproject.shoppingcart.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled task that periodically purges stale refresh tokens from the database
 * to prevent unbounded table growth.
 *
 * <h2>What gets deleted</h2>
 * <ul>
 *   <li><b>Expired tokens</b> – any token whose {@code expiry_date} is in the past,
 *       whether revoked or not. These can never be used again.</li>
 *   <li><b>Old revoked tokens</b> – tokens that were revoked (consumed via rotation or
 *       logout) more than 24 hours ago. The 24-hour window keeps a brief audit trail
 *       for reuse-attack detection before discarding the record entirely.</li>
 * </ul>
 *
 * <h2>Schedule</h2>
 * Runs once per day at 02:00 AM server time. Adjust the cron expression via
 * {@code app.refresh-token.cleanup-cron} in application.properties if needed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "${app.refresh-token.cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void purgeStaleRefreshTokens() {
        Instant now = Instant.now();

        // Revoked tokens older than 24 h are safe to delete – the window is enough to
        // detect any immediate reuse replay after rotation, and Flyway/audit logs
        // serve as the longer-term record if ever needed.
        Instant revokedCutoff = now.minus(24, ChronoUnit.HOURS);

        int deleted = refreshTokenRepository.deleteExpiredAndOldRevoked(now, revokedCutoff);

        if (deleted > 0) {
            log.info("Refresh token cleanup: {} stale token(s) purged.", deleted);
        } else {
            log.debug("Refresh token cleanup: no stale tokens found.");
        }
    }
}
