package com.thereplicant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token entity for JWT token rotation.
 * 
 * Security considerations (OWASP):
 * - Token is stored HASHED (SHA-256), not plain text
 * - Single-use: rotated on each refresh (old token deleted)
 * - Expiration: 7 days from creation
 * - Revocation: revokedAt marks invalidated tokens
 * 
 * Token lifecycle:
 * 1. User logs in → new refresh token created
 * 2. User refreshes → old token deleted, new token created
 * 3. User logs out → token revoked
 * 4. Token expires → rejected on refresh attempt
 * 
 * @see docs/architecture/security-architecture.md
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_token_expires", columnList = "expires_at"),
        @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-256 hash of the actual token.
     * The plain token is only sent to the client once and never stored.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    /**
     * User who owns this refresh token.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Token expiration timestamp.
     * Default: 7 days from creation.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * When the token was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When the token was revoked (null if still valid).
     * Set on logout or when token is rotated.
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    // =========================================================================
    // JPA Lifecycle Callbacks
    // =========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // =========================================================================
    // Business Methods
    // =========================================================================

    /**
     * Check if this token is still valid (not expired and not revoked).
     */
    public boolean isValid() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }

    /**
     * Revoke this token (called on logout or rotation).
     */
    public void revoke() {
        this.revokedAt = Instant.now();
    }
}
