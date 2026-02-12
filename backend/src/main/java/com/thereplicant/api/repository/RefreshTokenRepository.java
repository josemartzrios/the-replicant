package com.thereplicant.api.repository;

import com.thereplicant.api.entity.RefreshToken;
import com.thereplicant.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RefreshToken entity operations.
 * 
 * Handles token persistence, lookup, and cleanup operations.
 * Tokens are stored hashed for security.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Find refresh token by its hash (for validation).
     * Used during token refresh flow.
     * 
     * @param tokenHash SHA-256 hash of the token
     * @return Optional containing token if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Delete all refresh tokens for a user (used on logout/password change).
     * Invalidates all user sessions across devices.
     * 
     * @param user the user whose tokens to delete
     */
    void deleteAllByUser(User user);

    /**
     * Delete expired tokens (cleanup job).
     * Should be run periodically to clean up the database.
     * 
     * @param now current timestamp
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    /**
     * Revoke all tokens for a user (soft delete).
     * Sets revoked_at timestamp instead of deleting.
     * 
     * @param user      the user whose tokens to revoke
     * @param revokedAt timestamp of revocation
     * @return number of revoked tokens
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :revokedAt WHERE rt.user = :user AND rt.revokedAt IS NULL")
    int revokeAllByUser(@Param("user") User user, @Param("revokedAt") Instant revokedAt);
}
