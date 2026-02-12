package com.thereplicant.api.service;

import com.thereplicant.api.dto.auth.*;
import com.thereplicant.api.entity.RefreshToken;
import com.thereplicant.api.entity.Role;
import com.thereplicant.api.entity.User;
import com.thereplicant.api.repository.RefreshTokenRepository;
import com.thereplicant.api.repository.UserRepository;
import com.thereplicant.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Authentication service handling user registration, login, and token
 * management.
 * 
 * Security considerations (OWASP):
 * - Passwords hashed with BCrypt (strength 12)
 * - Refresh tokens stored as SHA-256 hashes
 * - Generic error messages to prevent user enumeration
 * - Token rotation on refresh (old token invalidated)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration-ms}")
    private long accessTokenExpirationMs;

    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 7;
    private static final int REFRESH_TOKEN_LENGTH = 64;

    // =========================================================================
    // Public Methods
    // =========================================================================

    /**
     * Check if first-time setup is required (no users exist).
     */
    @Transactional(readOnly = true)
    public AuthStatusResponse checkStatus() {
        boolean setupRequired = userRepository.count() == 0;
        return AuthStatusResponse.builder()
                .setupRequired(setupRequired)
                .build();
    }

    /**
     * Create the first admin user (one-time setup).
     * 
     * @throws IllegalStateException if users already exist
     */
    @Transactional
    public AuthResponse setup(SetupRequest request) {
        // Validate: only works when no users exist
        if (userRepository.count() > 0) {
            throw new IllegalStateException("Setup already completed. Admin user exists.");
        }

        // Create admin user
        User admin = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .name(request.getName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .mustChangePassword(false)
                .build();

        User savedUser = userRepository.save(admin);
        log.info("SETUP_COMPLETE: First admin created with email={}", maskEmail(savedUser.getEmail()));

        return generateAuthResponse(savedUser);
    }

    /**
     * Authenticate user with email and password.
     * 
     * @throws BadCredentialsException if credentials are invalid
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("LOGIN_FAILED: User not found email={}", maskEmail(request.getEmail()));
                    // Generic message to prevent user enumeration
                    return new BadCredentialsException("Invalid email or password");
                });

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("LOGIN_FAILED: Invalid password for email={}", maskEmail(request.getEmail()));
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("LOGIN_SUCCESS: email={}", maskEmail(user.getEmail()));
        return generateAuthResponse(user);
    }

    /**
     * Refresh access token using refresh token.
     * Implements token rotation (old token is revoked).
     * 
     * @throws BadCredentialsException if refresh token is invalid or expired
     */
    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        String tokenHash = hashToken(request.getRefreshToken());

        // Find and validate refresh token
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("REFRESH_FAILED: Token not found");
                    return new BadCredentialsException("Invalid refresh token");
                });

        // Check if token is valid (not expired, not revoked)
        if (!storedToken.isValid()) {
            log.warn("REFRESH_FAILED: Token expired or revoked for user={}",
                    storedToken.getUser().getId());
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        User user = storedToken.getUser();

        // Token rotation: revoke old token
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        log.info("REFRESH_SUCCESS: Token rotated for user={}", user.getId());
        return generateAuthResponse(user);
    }

    /**
     * Logout user by revoking all their refresh tokens.
     */
    @Transactional
    public void logout(User user) {
        int revokedCount = refreshTokenRepository.revokeAllByUser(user, Instant.now());
        log.info("LOGOUT: Revoked {} tokens for user={}", revokedCount, user.getId());
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Generate complete auth response with tokens and user info.
     */
    private AuthResponse generateAuthResponse(User user) {
        // Generate JWT access token
        String accessToken = jwtService.generateToken(user);

        // Generate and store refresh token
        String refreshToken = generateSecureToken();
        String refreshTokenHash = hashToken(refreshToken);

        RefreshToken tokenEntity = RefreshToken.builder()
                .tokenHash(refreshTokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_EXPIRATION_DAYS, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(tokenEntity);

        // Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpirationMs / 1000) // Convert to seconds
                .user(toUserDTO(user))
                .build();
    }

    /**
     * Convert User entity to safe DTO (excludes sensitive data).
     */
    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }

    /**
     * Generate cryptographically secure random token.
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[REFRESH_TOKEN_LENGTH];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash token using SHA-256.
     * Tokens are stored hashed to protect against DB leaks.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Mask email for logging (security: don't log full emails).
     * Example: admin@example.com → a***@example.com
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
