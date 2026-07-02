package com.demoproject.shoppingcart.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Persisted refresh token for server-side token management.
 *
 * <p>Supports refresh token rotation and reuse detection:
 * <ul>
 *   <li>{@code isRevoked} – set to true when the token is consumed (rotated) or explicitly revoked (logout).</li>
 *   <li>{@code replacedBy} – stores the new token string that replaced this one during rotation.
 *       When a revoked token is presented again, the server can detect the reuse attack
 *       and revoke the entire token family (all tokens for this user).</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user this refresh token belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** The opaque token string (UUID-based). Stored as plain text – not a JWT. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /** Absolute expiry instant for this token. */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Whether this token has been revoked.
     * Revocation happens on:
     * <ul>
     *   <li>Normal rotation (token consumed → new token issued)</li>
     *   <li>Explicit logout</li>
     *   <li>Reuse-attack detection (all family tokens revoked)</li>
     * </ul>
     */
    @Column(nullable = false)
    private boolean isRevoked = false;

    /**
     * The token string of the rotated successor of this token.
     * Null for currently active (non-rotated) tokens.
     * Used for reuse-attack detection: if a revoked token is presented again,
     * we trace the chain and revoke all descendants.
     */
    @Column(length = 512)
    private String replacedBy;

    /** Timestamp when this record was created. */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
