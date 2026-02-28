package com.thereplicant.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thereplicant.api.repository.RefreshTokenRepository;
import com.thereplicant.api.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for AuthController.
 *
 * Boots the full Spring context with H2 database.
 * Tests the complete HTTP request/response cycle:
 * Controller → Service → Repository → H2 Database.
 *
 * Each test class cleans the DB before running to ensure independence.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RefreshTokenRepository refreshTokenRepository;

        // Test data
        private static final String SETUP_JSON = """
                        {
                            "email": "admin@thereplicant.com",
                            "password": "SecurePass123!",
                            "name": "Admin User"
                        }
                        """;

        private static final String LOGIN_JSON = """
                        {
                            "email": "admin@thereplicant.com",
                            "password": "SecurePass123!"
                        }
                        """;

        @BeforeEach
        void cleanDatabase() {
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();
        }

        // =========================================================================
        // GET /api/v1/auth/status
        // =========================================================================

        @Nested
        @DisplayName("GET /auth/status")
        class StatusEndpointTests {

                @Test
                @DisplayName("Should return setupRequired=true when no users exist")
                void shouldReturnSetupRequiredTrue() throws Exception {
                        mockMvc.perform(get("/api/v1/auth/status"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.setupRequired").value(true));
                }

                @Test
                @DisplayName("Should return setupRequired=false after setup is completed")
                void shouldReturnSetupRequiredFalseAfterSetup() throws Exception {
                        // First, create the admin
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON));

                        // Then check status
                        mockMvc.perform(get("/api/v1/auth/status"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.setupRequired").value(false));
                }
        }

        // =========================================================================
        // POST /api/v1/auth/setup
        // =========================================================================

        @Nested
        @DisplayName("POST /auth/setup")
        class SetupEndpointTests {

                @Test
                @DisplayName("Should create admin and return 201 with tokens")
                void shouldCreateAdminSuccessfully() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                                        .andExpect(jsonPath("$.expiresIn").isNumber())
                                        .andExpect(jsonPath("$.user.email").value("admin@thereplicant.com"))
                                        .andExpect(jsonPath("$.user.name").value("Admin User"))
                                        .andExpect(jsonPath("$.user.role").value("ADMIN"));

                        // Verify user persisted in database
                        assertThat(userRepository.count()).isEqualTo(1);
                        assertThat(userRepository.findByEmail("admin@thereplicant.com")).isPresent();
                }

                @Test
                @DisplayName("Should return 409 Conflict when setup already completed")
                void shouldReturn409WhenSetupAlreadyDone() throws Exception {
                        // First setup
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON));

                        // Second setup attempt
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON))
                                        .andExpect(status().isConflict())
                                        .andExpect(jsonPath("$.title").value("Setup Already Completed"));
                }

                @Test
                @DisplayName("Should return 400 when email is invalid")
                void shouldReturn400WhenEmailInvalid() throws Exception {
                        String invalidJson = """
                                        {
                                            "email": "not-an-email",
                                            "password": "SecurePass123!",
                                            "name": "Admin"
                                        }
                                        """;

                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invalidJson))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 when password is too short")
                void shouldReturn400WhenPasswordTooShort() throws Exception {
                        String weakPasswordJson = """
                                        {
                                            "email": "admin@test.com",
                                            "password": "short",
                                            "name": "Admin"
                                        }
                                        """;

                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(weakPasswordJson))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 when required fields are missing")
                void shouldReturn400WhenFieldsMissing() throws Exception {
                        String emptyJson = """
                                        {
                                            "email": "",
                                            "password": "",
                                            "name": ""
                                        }
                                        """;

                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(emptyJson))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should store password hashed, not in plain text")
                void shouldStorePasswordHashed() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON));

                        var user = userRepository.findByEmail("admin@thereplicant.com").orElseThrow();

                        // Password hash should NOT be the plain password
                        assertThat(user.getPasswordHash()).isNotEqualTo("SecurePass123!");
                        // BCrypt hashes start with $2a$
                        assertThat(user.getPasswordHash()).startsWith("$2a$");
                }
        }

        // =========================================================================
        // POST /api/v1/auth/login
        // =========================================================================

        @Nested
        @DisplayName("POST /auth/login")
        class LoginEndpointTests {

                @BeforeEach
                void createAdminUser() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON));
                }

                @Test
                @DisplayName("Should login successfully with correct credentials")
                void shouldLoginSuccessfully() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(LOGIN_JSON))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                                        .andExpect(jsonPath("$.user.email").value("admin@thereplicant.com"));
                }

                @Test
                @DisplayName("Should return 401 with wrong password")
                void shouldReturn401WithWrongPassword() throws Exception {
                        String wrongPasswordJson = """
                                        {
                                            "email": "admin@thereplicant.com",
                                            "password": "WrongPassword123!"
                                        }
                                        """;

                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(wrongPasswordJson))
                                        .andExpect(status().isUnauthorized())
                                        .andExpect(jsonPath("$.title").value("Authentication Failed"));
                }

                @Test
                @DisplayName("Should return 401 with non-existent email")
                void shouldReturn401WithNonExistentEmail() throws Exception {
                        String unknownEmailJson = """
                                        {
                                            "email": "unknown@test.com",
                                            "password": "SecurePass123!"
                                        }
                                        """;

                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(unknownEmailJson))
                                        .andExpect(status().isUnauthorized());
                }

                @Test
                @DisplayName("Should return same error for wrong email and wrong password (anti-enumeration)")
                void shouldReturnSameErrorForSecurity() throws Exception {
                        // Wrong email
                        MvcResult wrongEmail = mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email":"unknown@test.com","password":"SecurePass123!"}
                                                        """))
                                        .andExpect(status().isUnauthorized())
                                        .andReturn();

                        // Wrong password
                        MvcResult wrongPassword = mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                        {"email":"admin@thereplicant.com","password":"WrongPass!"}
                                                        """))
                                        .andExpect(status().isUnauthorized())
                                        .andReturn();

                        // Same HTTP status for both (prevents user enumeration)
                        assertThat(wrongEmail.getResponse().getStatus())
                                        .isEqualTo(wrongPassword.getResponse().getStatus());
                }

                @Test
                @DisplayName("Should normalize email to lowercase on login")
                void shouldNormalizeEmailOnLogin() throws Exception {
                        String upperCaseEmailJson = """
                                        {
                                            "email": "ADMIN@TheReplicant.COM",
                                            "password": "SecurePass123!"
                                        }
                                        """;

                        mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(upperCaseEmailJson))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty());
                }
        }

        // =========================================================================
        // POST /api/v1/auth/refresh
        // =========================================================================

        @Nested
        @DisplayName("POST /auth/refresh")
        class RefreshEndpointTests {

                @BeforeEach
                void createAdminUser() throws Exception {
                        mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON));
                }

                @Test
                @DisplayName("Should refresh tokens successfully")
                void shouldRefreshTokensSuccessfully() throws Exception {
                        // Login to get refresh token
                        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(LOGIN_JSON))
                                        .andReturn();

                        String refreshToken = objectMapper.readTree(
                                        loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

                        // Use refresh token
                        String refreshJson = String.format("""
                                        {"refreshToken": "%s"}
                                        """, refreshToken);

                        mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(refreshJson))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                                        .andExpect(jsonPath("$.refreshToken").isNotEmpty());
                }

                // Token rotation behavior is validated by shouldRejectReusedRefreshToken:
                // it proves the old token is revoked after refresh (the real security
                // guarantee).

                @Test
                @DisplayName("Should reject reused refresh token (one-time use)")
                void shouldRejectReusedRefreshToken() throws Exception {
                        // Login to get refresh token
                        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(LOGIN_JSON))
                                        .andReturn();

                        String refreshToken = objectMapper.readTree(
                                        loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

                        String refreshJson = String.format("""
                                        {"refreshToken": "%s"}
                                        """, refreshToken);

                        // First use - should succeed
                        mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(refreshJson))
                                        .andExpect(status().isOk());

                        // Second use of same token - should fail (token rotation)
                        mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(refreshJson))
                                        .andExpect(status().isUnauthorized());
                }

                @Test
                @DisplayName("Should return 401 with invalid refresh token")
                void shouldReturn401WithInvalidToken() throws Exception {
                        String invalidJson = """
                                        {"refreshToken": "completely-invalid-token"}
                                        """;

                        mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(invalidJson))
                                        .andExpect(status().isUnauthorized());
                }
        }

        // =========================================================================
        // Full Flow Tests
        // =========================================================================

        @Nested
        @DisplayName("Complete Auth Flow")
        class FullFlowTests {

                @Test
                @DisplayName("Should complete full flow: status → setup → login → refresh")
                void shouldCompleteFullAuthFlow() throws Exception {
                        // 1. Check status - setup required
                        mockMvc.perform(get("/api/v1/auth/status"))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.setupRequired").value(true));

                        // 2. Setup admin
                        MvcResult setupResult = mockMvc.perform(post("/api/v1/auth/setup")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(SETUP_JSON))
                                        .andExpect(status().isCreated())
                                        .andReturn();

                        // 3. Check status - no longer required
                        mockMvc.perform(get("/api/v1/auth/status"))
                                        .andExpect(jsonPath("$.setupRequired").value(false));

                        // 4. Login
                        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(LOGIN_JSON))
                                        .andExpect(status().isOk())
                                        .andReturn();

                        String refreshToken = objectMapper.readTree(
                                        loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

                        // 5. Refresh token
                        mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(String.format("""
                                                        {"refreshToken": "%s"}
                                                        """, refreshToken)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.accessToken").isNotEmpty());
                }
        }
}
