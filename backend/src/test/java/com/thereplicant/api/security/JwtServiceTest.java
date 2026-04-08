package com.thereplicant.api.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit Tests for JwtService.
 * 
 * Tests cover:
 * - Token generation (access + refresh)
 * - Username extraction from token
 * - Token validation (correct user, wrong user, expired)
 * 
 * Following best practices:
 * - AAA pattern (Arrange-Act-Assert)
 * - Descriptive test names (should_ExpectedBehavior_When_Condition)
 * - One assertion concept per test
 * - F.I.R.S.T principles (Fast, Independent, Repeatable, Self-validating,
 * Timely)
 */
@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // Test user data
    private static final String TEST_EMAIL = "test@thereplicant.com";
    private static final String OTHER_EMAIL = "other@thereplicant.com";

    // Base64-encoded 256-bit secret key for testing
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdGVzdGluZy0xMjM0NTY3ODkwMTIzNDU2";

    // 1 hour in milliseconds (for access token)
    private static final long TEST_EXPIRATION = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject test values using reflection (simulates @Value injection)
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", TEST_EXPIRATION);
    }

    /**
     * Create a mock UserDetails for testing.
     */
    private UserDetails createTestUser(String email) {
        return User.builder()
                .username(email)
                .password("not-relevant-for-jwt")
                .authorities(Collections.emptyList())
                .build();
    }

    // =========================================================================
    // Token Generation Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should generate a non-empty access token")
        void shouldGenerateValidToken() {
            // Arrange
            UserDetails user = createTestUser(TEST_EMAIL);

            // Act
            String token = jwtService.generateToken(user);

            // Assert
            assertThat(token)
                    .isNotNull()
                    .isNotBlank()
                    .contains("."); // JWT format: header.payload.signature
        }

        @Test
        @DisplayName("Should generate a valid refresh token")
        void shouldGenerateRefreshToken() {
            // Arrange
            UserDetails user = createTestUser(TEST_EMAIL);

            // Act
            String refreshToken = jwtService.generateRefreshToken(user);

            // Assert
            assertThat(refreshToken)
                    .isNotNull()
                    .isNotBlank()
                    .contains(".");
        }

        @Test
        @DisplayName("Access token and refresh token should be different")
        void accessAndRefreshTokensShouldBeDifferent() {
            // Arrange
            UserDetails user = createTestUser(TEST_EMAIL);

            // Act
            String accessToken = jwtService.generateToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Assert
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }
    }

    // =========================================================================
    // Username Extraction Tests
    // =========================================================================

    @Nested
    @DisplayName("Username Extraction")
    class UsernameExtractionTests {

        @Test
        @DisplayName("Should extract correct username from token")
        void shouldExtractUsernameFromToken() {
            // Arrange
            UserDetails user = createTestUser(TEST_EMAIL);
            String token = jwtService.generateToken(user);

            // Act
            String extractedUsername = jwtService.extractUsername(token);

            // Assert
            assertThat(extractedUsername).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should extract username from refresh token")
        void shouldExtractUsernameFromRefreshToken() {
            // Arrange
            UserDetails user = createTestUser(TEST_EMAIL);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Act
            String extractedUsername = jwtService.extractUsername(refreshToken);

            // Assert
            assertThat(extractedUsername).isEqualTo(TEST_EMAIL);
        }
    }

    // =========================================================================
    // Token Validation Tests
    // =========================================================================

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token for correct user")
        void shouldValidateTokenForCorrectUser() {
            // Arrange
            UserDetails user = createTestUser(TEST_EMAIL);
            String token = jwtService.generateToken(user);

            // Act
            boolean isValid = jwtService.isTokenValid(token, user);

            // Assert
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should reject token for wrong user")
        void shouldRejectTokenForWrongUser() {
            // Arrange
            UserDetails originalUser = createTestUser(TEST_EMAIL);
            UserDetails differentUser = createTestUser(OTHER_EMAIL);
            String token = jwtService.generateToken(originalUser);

            // Act
            boolean isValid = jwtService.isTokenValid(token, differentUser);

            // Assert
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            // Arrange
            JwtService expiredJwtService = new JwtService();
            ReflectionTestUtils.setField(expiredJwtService, "secretKey", TEST_SECRET);
            // Set expiration to -1 hour (already expired)
            ReflectionTestUtils.setField(expiredJwtService, "accessTokenExpiration", -3600000L);

            UserDetails user = createTestUser(TEST_EMAIL);
            String expiredToken = expiredJwtService.generateToken(user);

            // Act & Assert
            // Extracting from expired token should throw ExpiredJwtException
            assertThatThrownBy(() -> jwtService.extractUsername(expiredToken))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}
