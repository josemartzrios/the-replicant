package com.thereplicant.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User entity implementing Spring Security's UserDetails.
 * 
 * Security considerations (OWASP):
 * - Password stored as BCrypt hash (never plain text)
 * - Email as unique identifier (prevents enumeration via different error
 * messages)
 * - mustChangePassword flag for forced password rotation
 * - Audit fields for tracking user creation
 * 
 * @see docs/architecture/database-schema.md
 * @see docs/architecture/security-architecture.md
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    /**
     * BCrypt hashed password (strength 12).
     * NEVER store plain text passwords.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Forces user to change password on next login.
     * Set to true when admin creates a user with temporary password.
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    /**
     * Reference to the admin who created this user (null for first admin).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // =========================================================================
    // JPA Lifecycle Callbacks
    // =========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // =========================================================================
    // UserDetails Implementation (Spring Security)
    // =========================================================================

    /**
     * Returns authorities for this user.
     * Uses ROLE_ prefix as per Spring Security convention.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Returns the password hash (used by Spring Security for authentication).
     */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Email is used as the username for authentication.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Account is never expired in MVP (no expiration feature).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Account is never locked in MVP (no lockout feature yet).
     * TODO: Implement account lockout after N failed attempts (rate limiting).
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Credentials are never expired in MVP.
     * Could be tied to mustChangePassword in future.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * All users are enabled in MVP (no soft-disable feature).
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
