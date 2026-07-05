package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /**
     * Revoke all active (non-revoked) tokens belonging to the given user.
     * Used on logout and on reuse-attack detection.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user AND rt.isRevoked = false")
    void revokeAllActiveTokensForUser(@Param("user") AppUser user);

    /** Remove all tokens (active and revoked) for a user – used for hard cleanup. */
    void deleteAllByUser(AppUser user);

    /**
     * Bulk-deletes tokens that are either:
     * <ul>
     *   <li>Past their expiry date (regardless of revocation status), OR</li>
     *   <li>Explicitly revoked AND older than the given cutoff (giving a brief audit window).</li>
     * </ul>
     * Called by the scheduled cleanup task to keep the table lean.
     *
     * @param now    current instant – tokens with expiryDate before this are expired
     * @param cutoff revoked tokens created before this instant are also purged
     * @return number of rows deleted
     */
    @Modifying
    @Query("""
            DELETE FROM RefreshToken rt
            WHERE rt.expiryDate < :now
               OR (rt.isRevoked = true AND rt.createdAt < :cutoff)
            """)
    int deleteExpiredAndOldRevoked(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}
