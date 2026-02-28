package com.thereplicant.api.service;

import com.thereplicant.api.dto.auth.*;
import com.thereplicant.api.entity.RefreshToken;
import com.thereplicant.api.entity.Role;
import com.thereplicant.api.entity.User;
import com.thereplicant.api.exception.SetupAlreadyCompletedException;
import com.thereplicant.api.repository.RefreshTokenRepository;
import com.thereplicant.api.repository.UserRepository;
import com.thereplicant.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuthService.
 *
 * Tests cover all 5 public methods:
 * - checkStatus (setup required check)
 * - setup (first admin creation)
 * - login (credential validation)
 * - refreshToken (token rotation)
 * - logout (token revocation)
 *
 * Uses Mockito to mock dependencies (repositories, JwtService,
 * PasswordEncoder).
 * Follows AAA pattern, @Nested grouping, and F.I.R.S.T principles.
 */
@DisplayName("AuthService Unit Tests")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // Test constants
    private static final String TEST_EMAIL = "admin@thereplicant.com";
    private static final String TEST_PASSWORD = "SecurePassword123!"; // 12+ chars, 1 upper, 1 digit, 1 special
    private static final String TEST_NAME = "Admin User";
    private static final String ENCODED_PASSWORD = "$2a$12$hashedPasswordValue";
    private static final String MOCK_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.mock.token";
    private static final long TEST_EXPIRATION_MS = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", TEST_EXPIRATION_MS);
    }

    /**
     * Helper: Create a test User entity.
     */
    private User createTestUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email(TEST_EMAIL)
                .name(TEST_NAME)
                .passwordHash(ENCODED_PASSWORD)
                .role(Role.ADMIN)
                .mustChangePassword(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // checkStatus Tests
    // =========================================================================

    @Nested
    @DisplayName("Check Status")
    class CheckStatusTests {

        @Test
        @DisplayName("Should return setupRequired=true when no users exist")
        void shouldReturnSetupRequiredWhenNoUsers() {
            // Arrange
            when(userRepository.count()).thenReturn(0L);

            // Act
            AuthStatusResponse response = authService.checkStatus();

            // Assert
            assertThat(response.isSetupRequired()).isTrue();
            verify(userRepository).count();
        }

        @Test
        @DisplayName("Should return setupRequired=false when users exist")
        void shouldReturnSetupNotRequiredWhenUsersExist() {
            // Arrange
            when(userRepository.count()).thenReturn(1L);

            // Act
            AuthStatusResponse response = authService.checkStatus();

            // Assert
            assertThat(response.isSetupRequired()).isFalse();
            verify(userRepository).count();
        }
    }

    // =========================================================================
    // setup Tests
    // =========================================================================

    @Nested
    @DisplayName("Setup (First Admin)")
    class SetupTests {

        private SetupRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = SetupRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .name(TEST_NAME)
                    .build();
        }

        @Test
        @DisplayName("Should create admin user when no users exist")
        void shouldCreateAdminWhenNoUsers() {
            // Arrange
            User savedUser = createTestUser();
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

            // Act
            AuthResponse response = authService.setup(validRequest);

            // Assert
            assertThat(response.getAccessToken()).isEqualTo(MOCK_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getExpiresIn()).isEqualTo(TEST_EXPIRATION_MS / 1000);
            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("Should hash password with BCrypt on setup")
        void shouldHashPasswordOnSetup() {
            // Arrange
            User savedUser = createTestUser();
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

            // Act
            authService.setup(validRequest);

            // Assert
            verify(passwordEncoder).encode(TEST_PASSWORD);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("Should normalize email to lowercase on setup")
        void shouldNormalizeEmailOnSetup() {
            // Arrange
            validRequest.setEmail("Admin@TheReplicant.COM");
            User savedUser = createTestUser();
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

            // Act
            authService.setup(validRequest);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@thereplicant.com");
        }

        @Test
        @DisplayName("Should throw SetupAlreadyCompletedException when users already exist")
        void shouldThrowWhenUsersExist() {
            // Arrange
            when(userRepository.count()).thenReturn(1L);

            // Act & Assert
            assertThatThrownBy(() -> authService.setup(validRequest))
                    .isInstanceOf(SetupAlreadyCompletedException.class)
                    .hasMessageContaining("Setup already completed");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should store refresh token hash in database on setup")
        void shouldStoreRefreshTokenHashOnSetup() {
            // Arrange
            User savedUser = createTestUser();
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

            // Act
            authService.setup(validRequest);

            // Assert
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getTokenHash()).isNotBlank();
            assertThat(savedToken.getUser()).isEqualTo(savedUser);
            assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        }
    }

    // =========================================================================
    // login Tests
    // =========================================================================

    @Nested
    @DisplayName("Login")
    class LoginTests {

        private LoginRequest validRequest;
        private User existingUser;

        @BeforeEach
        void setUp() {
            validRequest = LoginRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .build();
            existingUser = createTestUser();
        }

        @Test
        @DisplayName("Should return tokens on valid credentials")
        void shouldReturnTokensOnValidCredentials() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

            // Act
            AuthResponse response = authService.login(validRequest);

            // Assert
            assertThat(response.getAccessToken()).isEqualTo(MOCK_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Arrange
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(validRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when password is wrong")
        void shouldThrowWhenPasswordIsWrong() {
            // Arrange
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> authService.login(validRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should use same error message for both user-not-found and wrong-password")
        void shouldUseSameErrorMessageForSecurityReasons() {
            // Arrange - user not found
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            String notFoundMessage = null;
            try {
                authService.login(validRequest);
            } catch (BadCredentialsException e) {
                notFoundMessage = e.getMessage();
            }

            // Arrange - wrong password
            reset(userRepository, passwordEncoder);
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

            String wrongPasswordMessage = null;
            try {
                authService.login(validRequest);
            } catch (BadCredentialsException e) {
                wrongPasswordMessage = e.getMessage();
            }

            // Assert - Same message prevents user enumeration (OWASP)
            assertThat(notFoundMessage).isEqualTo(wrongPasswordMessage);
        }
    }

    // =========================================================================
    // refreshToken Tests
    // =========================================================================

    @Nested
    @DisplayName("Refresh Token")
    class RefreshTokenTests {

        private RefreshRequest validRequest;
        private User existingUser;

        @BeforeEach
        void setUp() {
            validRequest = RefreshRequest.builder()
                    .refreshToken("valid-refresh-token-string")
                    .build();
            existingUser = createTestUser();
        }

        @Test
        @DisplayName("Should return new tokens on valid refresh token")
        void shouldReturnNewTokensOnValidRefresh() {
            // Arrange
            RefreshToken storedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hashed-value")
                    .user(existingUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdAt(Instant.now())
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(storedToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenReturn(storedToken) // save revoked token
                    .thenReturn(new RefreshToken()); // save new token
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);

            // Act
            AuthResponse response = authService.refreshToken(validRequest);

            // Assert
            assertThat(response.getAccessToken()).isEqualTo(MOCK_ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getUser().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("Should revoke old token on refresh (token rotation)")
        void shouldRevokeOldTokenOnRefresh() {
            // Arrange
            RefreshToken storedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hashed-value")
                    .user(existingUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdAt(Instant.now())
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(storedToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(storedToken);
            when(jwtService.generateToken(any(User.class))).thenReturn(MOCK_ACCESS_TOKEN);

            // Act
            authService.refreshToken(validRequest);

            // Assert - Old token should be revoked
            assertThat(storedToken.getRevokedAt()).isNotNull();
            verify(refreshTokenRepository, atLeast(1)).save(storedToken);
        }

        @Test
        @DisplayName("Should throw when refresh token not found")
        void shouldThrowWhenTokenNotFound() {
            // Arrange
            when(refreshTokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(validRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw when refresh token is expired")
        void shouldThrowWhenTokenExpired() {
            // Arrange
            RefreshToken expiredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hashed-value")
                    .user(existingUser)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS)) // Expired yesterday
                    .createdAt(Instant.now().minus(8, ChronoUnit.DAYS))
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(expiredToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(validRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("expired or revoked");
        }

        @Test
        @DisplayName("Should throw when refresh token is already revoked")
        void shouldThrowWhenTokenAlreadyRevoked() {
            // Arrange
            RefreshToken revokedToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .tokenHash("hashed-value")
                    .user(existingUser)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdAt(Instant.now())
                    .revokedAt(Instant.now().minus(1, ChronoUnit.HOURS)) // Already revoked
                    .build();

            when(refreshTokenRepository.findByTokenHash(anyString()))
                    .thenReturn(Optional.of(revokedToken));

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(validRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("expired or revoked");
        }
    }

    // =========================================================================
    // logout Tests
    // =========================================================================

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("Should revoke all tokens for user on logout")
        void shouldRevokeAllTokensOnLogout() {
            // Arrange
            User user = createTestUser();
            when(refreshTokenRepository.revokeAllByUser(eq(user), any(Instant.class)))
                    .thenReturn(3);

            // Act
            authService.logout(user);

            // Assert
            verify(refreshTokenRepository).revokeAllByUser(eq(user), any(Instant.class));
        }

        @Test
        @DisplayName("Should handle logout when user has no tokens")
        void shouldHandleLogoutWithNoTokens() {
            // Arrange
            User user = createTestUser();
            when(refreshTokenRepository.revokeAllByUser(eq(user), any(Instant.class)))
                    .thenReturn(0);

            // Act
            authService.logout(user);

            // Assert - No exception thrown, method completes normally
            verify(refreshTokenRepository).revokeAllByUser(eq(user), any(Instant.class));
        }
    }
}
