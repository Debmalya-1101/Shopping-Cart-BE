package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.exception.TokenRefreshException;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.RefreshToken;
import com.demoproject.shoppingcart.repository.RefreshTokenRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles all server-side lifecycle management for refresh tokens.
 *
 * <h2>Security model</h2>
 * <ul>
 *   <li><b>Rotation:</b> Every call to {@link #rotateRefreshToken} consumes the current
 *       token (marks it as revoked + records the successor token value) and issues a brand-new one.
 *       Clients must always store and send the latest token.</li>
 *   <li><b>Reuse detection:</b> If a previously-rotated (revoked) token is presented, the server
 *       can detect it because the token row exists but {@code isRevoked=true}.
 *       The entire session is immediately terminated – all active tokens for that user are revoked –
 *       and a {@link TokenRefreshException} is thrown so the client must re-login.</li>
 * </ul>
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    // -----------------------------------------------------------------------
    //  Public API
    // -----------------------------------------------------------------------

    /**
     * Creates and persists a new refresh token for the given user.
     * Called once on successful login.
     *
     * @param userId the primary key of the authenticating user
     * @return the persisted {@link RefreshToken}
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(generateTokenValue())
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(token);
    }

    /**
     * Validates a raw token string and returns the persisted {@link RefreshToken}.
     *
     * <p>Validation steps:
     * <ol>
     *   <li>Token must exist in the database.</li>
     *   <li>Token must NOT be revoked. If it is revoked → <b>reuse attack detected</b> →
     *       all active tokens for the owner are revoked immediately.</li>
     *   <li>Token must NOT be expired. Expired tokens are deleted before throwing.</li>
     * </ol>
     *
     * @param rawToken the opaque token string sent by the client
     * @return the valid {@link RefreshToken} record
     * @throws TokenRefreshException on any validation failure
     */
    @Transactional
    public RefreshToken validateRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new TokenRefreshException(rawToken, "Token not found"));

        if (token.isRevoked()) {
            // === REUSE ATTACK DETECTED ===
            // A previously-rotated token was replayed. Revoke all active sessions for this user.
            log.warn("Refresh token reuse attack detected for user '{}'. Revoking all sessions.",
                    token.getUser().getUserName());
            refreshTokenRepository.revokeAllActiveTokensForUser(token.getUser());
            throw new TokenRefreshException(rawToken,
                    "Token has already been used (possible reuse attack). All sessions have been revoked. Please log in again.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(rawToken, "Token has expired. Please log in again.");
        }

        return token;
    }

    /**
     * Performs refresh token rotation:
     * <ol>
     *   <li>Marks the old token as revoked and records the new token value in {@code replacedBy}.</li>
     *   <li>Creates and persists a fresh token for the same user.</li>
     * </ol>
     *
     * @param oldToken the {@link RefreshToken} being consumed (already validated)
     * @return the newly issued {@link RefreshToken}
     */
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        // Build the new token first so we can store its value in the old record
        String newTokenValue = generateTokenValue();

        // Mark old token as revoked and link to successor for audit trail
        oldToken.setRevoked(true);
        oldToken.setReplacedBy(newTokenValue);
        refreshTokenRepository.save(oldToken);

        // Persist the new token
        RefreshToken newToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(newTokenValue)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(newToken);
    }

    /**
     * Revokes all active refresh tokens for a user.
     * Called on explicit logout.
     *
     * @param userId the primary key of the user logging out
     */
    @Transactional
    public void revokeAllTokensForUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        refreshTokenRepository.revokeAllActiveTokensForUser(user);
        log.info("All refresh tokens revoked for user '{}'.", user.getUserName());
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    private String generateTokenValue() {
        // Two UUIDs concatenated give 64 hex chars – opaque and unguessable
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }
}
