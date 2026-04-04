# Production Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve all 4 blockers, 4 high-priority issues, 3 medium issues, and 5 UI/accessibility gaps identified in the pre-launch audit so the MVP is safe to deploy to production.

**Architecture:** The fixes span four subsystems — backend Spring Boot config hardening, a cookie-based refresh token migration (backend + frontend), Next.js security layer additions, and frontend accessibility corrections. Tasks are ordered by dependency: backend config first, then auth cookie migration, then frontend security, then performance/API consistency, then UI.

**Tech Stack:** Spring Boot 3.2.2 (Java 21, Maven), Next.js 16 (React 19, TypeScript, TailwindCSS 4), PostgreSQL, Bucket4j, jjwt 0.12.5.

---

## Files Modified or Created

| File | Change |
|---|---|
| `backend/src/main/resources/application.yml` | Fix ddl-auto default, error messages, actuator details, add cookie.secure + dev profile section |
| `backend/src/main/java/com/thereplicant/api/controller/AuthController.java` | Wire cookie.secure, add refresh cookie, read refresh from cookie |
| `backend/src/main/java/com/thereplicant/api/service/AuthService.java` | Change refreshToken() signature to accept String |
| `backend/src/main/java/com/thereplicant/api/dto/auth/RefreshRequest.java` | Delete — no longer needed |
| `backend/src/main/java/com/thereplicant/api/security/RateLimitFilter.java` | Fix X-Forwarded-For extraction |
| `backend/src/main/java/com/thereplicant/api/controller/CategoryController.java` | @PutMapping → @PatchMapping |
| `backend/src/test/java/com/thereplicant/api/AuthControllerIT.java` | Update refresh/logout test to use cookies |
| `frontend/next.config.mjs` | CSP connect-src driven by env var |
| `frontend/src/middleware.ts` | New — server-side admin route guard |
| `frontend/src/lib/api.ts` | logout/refreshToken no longer send body token |
| `frontend/src/lib/auth.tsx` | Remove refreshToken from localStorage |
| `frontend/src/app/(blog)/posts/[slug]/page.tsx` | React.cache() to deduplicate fetch |
| `frontend/src/app/login/page.tsx` | focus-visible rings on inputs |
| `frontend/src/app/admin/posts/new/page.tsx` | focus-visible, aria-hidden, ellipsis |
| `frontend/src/app/admin/posts/[id]/edit/page.tsx` | focus-visible, aria-hidden, ellipsis |
| `frontend/src/components/ui/Toast.tsx` | aria-live, aria-label on close button, aria-hidden on icons |
| `frontend/src/app/globals.css` | prefers-reduced-motion global rule |

---

## Section 1 — Backend Config Blockers

### Task 1: Harden `application.yml` defaults

**Files:**
- Modify: `backend/src/main/resources/application.yml`

The default profile (no active profile) currently has 3 dangerous settings that are never overridden if `SPRING_PROFILES_ACTIVE` is not set: `ddl-auto: update`, `server.error.include-message: always`, and `management.endpoint.health.show-details: always`. This task makes the defaults safe and introduces an explicit `dev` profile for local development.

- [ ] **Step 1: Replace the contents of `application.yml`**

Replace the entire file with the following. Changes vs current:
  - Default `ddl-auto` → `validate` (was `update`)
  - Default `server.error.include-message` → `never` (was `always`)
  - Default `management.endpoint.health.show-details` → `when-authorized` (was `always`)
  - New `cookie.secure` property (wired to `AuthController` in Task 3)
  - New explicit `dev` profile section that restores the developer-friendly defaults
  - Prod profile now additionally disables Swagger and locks Actuator

```yaml
# ===========================================
# The Replicant - Application Configuration
# ===========================================

server:
  port: ${PORT:8080}
  error:
    include-message: never
    include-binding-errors: never

spring:
  application:
    name: the-replicant-api

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:replicant_db}
    username: ${POSTGRES_USER:replicant}
    password: ${POSTGRES_PASSWORD:replicant123}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

# Actuator — safe defaults, detail requires auth
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized
      show-components: when-authorized

# OpenAPI / Swagger
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:dGhlLXJlcGxpY2FudC1kZXYtc2VjcmV0LWtleS0yMDI2LWNoYW5nZS1pbi1wcm9kdWN0aW9uLTEyMzQ1Njc4OTA=}
  expiration-ms: ${JWT_EXPIRATION_MS:86400000}

# Cookie security — set COOKIE_SECURE=true in production
cookie:
  secure: ${COOKIE_SECURE:false}

# CORS Configuration
cors:
  allowed-origins: ${CORS_ORIGINS:http://localhost:3000}

# Logging
logging:
  level:
    root: INFO
    com.thereplicant: DEBUG

---
# ===========================================
# Dev Profile — relaxed settings for local development
# Activate with: SPRING_PROFILES_ACTIVE=dev
# ===========================================
spring:
  config:
    activate:
      on-profile: dev

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

server:
  error:
    include-message: always
    include-binding-errors: always

management:
  endpoint:
    health:
      show-details: always
      show-components: always

---
# ===========================================
# Test Profile (H2 In-Memory)
# ===========================================
spring:
  config:
    activate:
      on-profile: test

  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

---
# ===========================================
# Production Profile (Railway + Supabase)
# Activate with: SPRING_PROFILES_ACTIVE=prod
# ===========================================
spring:
  config:
    activate:
      on-profile: prod

  datasource:
    url: ${DATABASE_URL}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

# Disable API documentation in production
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false

# Actuator — health only, no details
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
      show-components: never

logging:
  level:
    root: WARN
    com.thereplicant: INFO
```

- [ ] **Step 2: Verify the backend still starts locally**

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Expected: application starts on port 8080 with `dev` profile active. No schema errors (H2 is not used, Postgres must be running via `docker-compose up -d`).

- [ ] **Step 3: Run backend tests**

```bash
./mvnw test
```

Expected: all tests pass (they use the `test` profile with H2, unaffected).

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "fix(config): harden application.yml defaults for production safety

- ddl-auto default → validate (was update)
- server.error.include-message default → never (was always)
- actuator show-details default → when-authorized (was always)
- add explicit dev profile restoring local-dev conveniences
- disable swagger + lock actuator in prod profile
- add cookie.secure property driven by COOKIE_SECURE env var"
```

---

### Task 2: Fix `secure(false)` cookie flag in `AuthController`

**Files:**
- Modify: `backend/src/main/java/com/thereplicant/api/controller/AuthController.java`

Both cookies set by `AuthController` (login and logout) have `.secure(false)` hardcoded. In production over HTTPS, the browser will still send them (HTTPS includes HTTP-level cookies), but best practice is `secure(true)` so browsers refuse to send them over plain HTTP. This wires the new `cookie.secure` property.

- [ ] **Step 1: Write the test first**

In `backend/src/test/java/com/thereplicant/api/AuthControllerIT.java`, add a test that verifies the access token cookie is present after login:

```java
@Test
@DisplayName("POST /auth/login sets HttpOnly accessToken cookie")
void login_setsCookie() throws Exception {
    // Given — create an admin first via setup
    SetupRequest setup = new SetupRequest();
    setup.setEmail("test@thereplicant.com");
    setup.setName("Test Admin");
    setup.setPassword("Password123!");

    mockMvc.perform(post("/api/v1/auth/setup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(setup)))
            .andExpect(status().isCreated());

    // When
    LoginRequest login = new LoginRequest();
    login.setEmail("test@thereplicant.com");
    login.setPassword("Password123!");

    MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

    // Then — cookie must be present and HttpOnly
    Cookie cookie = result.getResponse().getCookie("accessToken");
    assertThat(cookie).isNotNull();
    assertThat(cookie.isHttpOnly()).isTrue();
    // accessToken must NOT appear in the JSON body
    String body = result.getResponse().getContentAsString();
    assertThat(body).doesNotContain("accessToken");
}
```

- [ ] **Step 2: Run the test to verify it currently passes (cookie exists but secure is false)**

```bash
./mvnw test -Dtest=AuthControllerIT#login_setsCookie
```

Expected: PASS (cookie is already set; this confirms baseline).

- [ ] **Step 3: Update `AuthController` to wire `cookie.secure`**

Replace the entire `AuthController.java` with the following. Key changes:
  - Add `@Value("${cookie.secure:false}") private boolean cookieSecure`
  - Replace `.secure(false)` with `.secure(cookieSecure)` in both places

```java
package com.thereplicant.api.controller;

import com.thereplicant.api.dto.auth.*;
import com.thereplicant.api.entity.User;
import com.thereplicant.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization")
public class AuthController {

    private final AuthService authService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * Sets the JWT access token as an HttpOnly cookie.
     * The secure flag is driven by the cookie.secure property (false in dev, true in prod).
     */
    private void setTokenCookie(HttpServletResponse response, AuthResponse authData) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", authData.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(authData.getExpiresIn())
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        authData.setAccessToken(null);
    }

    @GetMapping("/status")
    @Operation(summary = "Check if setup is required")
    @ApiResponse(responseCode = "200", description = "Auth status returned")
    public ResponseEntity<AuthStatusResponse> getStatus() {
        return ResponseEntity.ok(authService.checkStatus());
    }

    @PostMapping("/setup")
    @Operation(summary = "First admin setup (one-time only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Admin created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Setup already completed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> setup(@Valid @RequestBody SetupRequest request,
            HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.setup(request);
        setTokenCookie(httpServletResponse, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.login(request);
        setTokenCookie(httpServletResponse, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request,
            HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.refreshToken(request);
        setTokenCookie(httpServletResponse, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user, HttpServletResponse httpServletResponse) {
        authService.logout(user);

        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./mvnw test
```

Expected: all tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/thereplicant/api/controller/AuthController.java
git add backend/src/test/java/com/thereplicant/api/AuthControllerIT.java
git commit -m "fix(auth): drive cookie secure flag from cookie.secure property

Was hardcoded to false — now reads COOKIE_SECURE env var.
Set COOKIE_SECURE=true in Railway production environment."
```

---

## Section 2 — Refresh Token Cookie Migration (HIGH)

Currently the refresh token is sent in the response body and stored in `localStorage` by the frontend, making it readable by any JavaScript (XSS-vulnerable). This section migrates it to a second httpOnly cookie. The change is backend-first, then frontend.

### Task 3: Update `AuthService.refreshToken()` to accept a token string

**Files:**
- Modify: `backend/src/main/java/com/thereplicant/api/service/AuthService.java`

The current signature `public AuthResponse refreshToken(RefreshRequest request)` ties the service to the DTO. Changing it to accept `String refreshToken` directly makes the service agnostic of how the controller receives the token (body vs cookie).

- [ ] **Step 1: Write the failing test**

In `backend/src/test/java/com/thereplicant/api/AuthServiceTest.java`, add:

```java
@Test
@DisplayName("refreshToken accepts token string and rotates it")
void refreshToken_acceptsStringToken() {
    // Given
    String rawToken = "some-secure-random-token";
    String tokenHash = hashForTest(rawToken); // SHA-256 base64

    RefreshToken storedToken = RefreshToken.builder()
            .tokenHash(tokenHash)
            .user(mockUser)
            .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
            .revoked(false)
            .build();

    when(refreshTokenRepository.findByTokenHash(tokenHash))
            .thenReturn(Optional.of(storedToken));
    when(refreshTokenRepository.save(any())).thenReturn(storedToken);
    when(jwtService.generateToken(mockUser)).thenReturn("new-access-token");

    // When
    AuthResponse response = authService.refreshToken(rawToken);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isEqualTo("new-access-token");
    verify(refreshTokenRepository).save(argThat(t -> t.isRevoked()));
}

// Helper — replicates AuthService.hashToken() logic for test assertions
private String hashForTest(String token) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=AuthServiceTest#refreshToken_acceptsStringToken
```

Expected: FAIL — `refreshToken(String)` method does not exist yet.

- [ ] **Step 3: Change the signature in `AuthService`**

In `AuthService.java`, change the `refreshToken` method signature and body:

```java
/**
 * Refresh access token using a raw refresh token string.
 * Implements token rotation (old token is revoked).
 *
 * @throws BadCredentialsException if refresh token is invalid or expired
 */
@Transactional
public AuthResponse refreshToken(String refreshTokenValue) {
    String tokenHash = hashToken(refreshTokenValue);

    RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> {
                log.warn("REFRESH_FAILED: Token not found");
                return new BadCredentialsException("Invalid refresh token");
            });

    if (!storedToken.isValid()) {
        log.warn("REFRESH_FAILED: Token expired or revoked for user={}",
                storedToken.getUser().getId());
        throw new BadCredentialsException("Refresh token expired or revoked");
    }

    User user = storedToken.getUser();
    storedToken.revoke();
    refreshTokenRepository.save(storedToken);

    log.info("REFRESH_SUCCESS: Token rotated for user={}", user.getId());
    return generateAuthResponse(user);
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./mvnw test
```

Expected: all tests pass. The `AuthController` will not compile until Task 4 updates its call site.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/thereplicant/api/service/AuthService.java
git add backend/src/test/java/com/thereplicant/api/AuthServiceTest.java
git commit -m "refactor(auth): AuthService.refreshToken() accepts String token directly

Decouples service from RefreshRequest DTO. Controller now
responsible for extracting the token value (from cookie in Task 4)."
```

---

### Task 4: Set refresh token as httpOnly cookie; read it from cookie on refresh/logout

**Files:**
- Modify: `backend/src/main/java/com/thereplicant/api/controller/AuthController.java`
- Delete: `backend/src/main/java/com/thereplicant/api/dto/auth/RefreshRequest.java`
- Modify: `backend/src/test/java/com/thereplicant/api/AuthControllerIT.java`

The `setTokenCookie` helper is renamed to `setAuthCookies` and now sets both the access token and refresh token as httpOnly cookies. The `/auth/refresh` endpoint reads the refresh token from the cookie. The `/auth/logout` endpoint clears both cookies.

The refresh token cookie is scoped to `/api/v1/auth` so it is only sent to auth endpoints, limiting exposure.

- [ ] **Step 1: Update the integration test to use cookies**

In `AuthControllerIT.java`, add a test for the refresh-via-cookie flow:

```java
@Test
@DisplayName("POST /auth/refresh works via cookie, not body")
void refresh_workViaCookie() throws Exception {
    // Setup — create admin
    SetupRequest setup = new SetupRequest();
    setup.setEmail("cookie@test.com");
    setup.setName("Cookie Tester");
    setup.setPassword("Password123!");

    MvcResult setupResult = mockMvc.perform(post("/api/v1/auth/setup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(setup)))
            .andExpect(status().isCreated())
            .andReturn();

    // Extract refresh token from login response body (still present before Task 8 frontend change)
    AuthResponse setupResponse = objectMapper.readValue(
            setupResult.getResponse().getContentAsString(), AuthResponse.class);
    String refreshToken = setupResponse.getRefreshToken();

    // When — send refresh token as cookie
    mockMvc.perform(post("/api/v1/auth/refresh")
            .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
            .andExpect(status().isOk())
            .andReturn();
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./mvnw test -Dtest=AuthControllerIT#refresh_workViaCookie
```

Expected: FAIL — refresh endpoint still expects body.

- [ ] **Step 3: Replace `AuthController.java` with cookie-based refresh**

Replace the full file content:

```java
package com.thereplicant.api.controller;

import com.thereplicant.api.dto.auth.*;
import com.thereplicant.api.entity.User;
import com.thereplicant.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization")
public class AuthController {

    private final AuthService authService;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    // 24h in seconds — matches jwt.expiration-ms default
    private static final long ACCESS_TOKEN_MAX_AGE = 86400L;
    // 7 days in seconds
    private static final long REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60L;

    /**
     * Sets both tokens as HttpOnly cookies in the response.
     * Access token: path=/ (sent on all API calls).
     * Refresh token: path=/api/v1/auth (only sent to auth endpoints).
     * Both tokens are nulled in the response body.
     */
    private void setAuthCookies(HttpServletResponse response, AuthResponse authData) {
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", authData.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(authData.getExpiresIn() > 0 ? authData.getExpiresIn() : ACCESS_TOKEN_MAX_AGE)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (authData.getRefreshToken() != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authData.getRefreshToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/api/v1/auth")
                    .maxAge(REFRESH_TOKEN_MAX_AGE)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        authData.setAccessToken(null);
        authData.setRefreshToken(null);
    }

    @GetMapping("/status")
    @Operation(summary = "Check if setup is required")
    @ApiResponse(responseCode = "200", description = "Auth status returned")
    public ResponseEntity<AuthStatusResponse> getStatus() {
        return ResponseEntity.ok(authService.checkStatus());
    }

    @PostMapping("/setup")
    @Operation(summary = "First admin setup (one-time only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Admin created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Setup already completed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> setup(@Valid @RequestBody SetupRequest request,
            HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.setup(request);
        setAuthCookies(httpServletResponse, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletResponse httpServletResponse) {
        AuthResponse response = authService.login(request);
        setAuthCookies(httpServletResponse, response);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh using the httpOnly refreshToken cookie.
     * No request body needed — the cookie is sent automatically by the browser.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpServletResponse) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token missing");
        }

        AuthResponse response = authService.refreshToken(refreshToken);
        setAuthCookies(httpServletResponse, response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user,
            HttpServletResponse httpServletResponse) {
        authService.logout(user);

        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("Lax").build();
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(cookieSecure).path("/api/v1/auth").maxAge(0).sameSite("Lax").build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Delete `RefreshRequest.java`**

```bash
rm backend/src/main/java/com/thereplicant/api/dto/auth/RefreshRequest.java
```

Verify no other class imports `RefreshRequest`:

```bash
grep -r "RefreshRequest" backend/src/main/java/
```

Expected: no output (zero references remaining).

- [ ] **Step 5: Run tests**

```bash
./mvnw test
```

Expected: all tests pass.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/thereplicant/api/controller/AuthController.java
git add backend/src/test/java/com/thereplicant/api/AuthControllerIT.java
git rm backend/src/main/java/com/thereplicant/api/dto/auth/RefreshRequest.java
git commit -m "feat(auth): migrate refresh token to httpOnly cookie

- setAuthCookies() sets both access + refresh tokens as httpOnly cookies
- /auth/refresh reads refreshToken from cookie (no body required)
- /auth/logout clears both cookies
- refresh token scoped to /api/v1/auth path for minimal exposure
- delete RefreshRequest DTO (no longer needed)"
```

---

## Section 3 — Frontend Security

### Task 5: Fix CSP `connect-src` for production

**Files:**
- Modify: `frontend/next.config.mjs`

The current CSP has `connect-src 'self' http://localhost:8080 https:` hardcoded. The `http://localhost:8080` reference goes into the production build. Fix: derive the API origin from `NEXT_PUBLIC_API_URL`.

- [ ] **Step 1: Update `next.config.mjs`**

Replace the file with:

```javascript
/** @type {import('next').NextConfig} */

// Derive the backend API origin for the connect-src CSP directive.
// In production, NEXT_PUBLIC_API_URL points to Railway (https://...).
// In development, it defaults to http://localhost:8080/api/v1.
const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const apiOrigin = (() => {
    try {
        const url = new URL(apiUrl);
        return `${url.protocol}//${url.host}`;
    } catch {
        return "http://localhost:8080";
    }
})();

const cspHeader = `
    default-src 'self';
    script-src 'self' 'unsafe-eval' 'unsafe-inline';
    style-src 'self' 'unsafe-inline';
    img-src 'self' blob: data: https:;
    font-src 'self' data:;
    object-src 'none';
    base-uri 'self';
    form-action 'self';
    frame-ancestors 'none';
    connect-src 'self' ${apiOrigin};
    upgrade-insecure-requests;
`;

const nextConfig = {
    async headers() {
        return [
            {
                source: '/(.*)',
                headers: [
                    {
                        key: 'Content-Security-Policy',
                        value: cspHeader.replace(/\n/g, ''),
                    },
                    {
                        key: 'X-Content-Type-Options',
                        value: 'nosniff',
                    },
                    {
                        key: 'X-Frame-Options',
                        value: 'DENY',
                    },
                    {
                        key: 'X-XSS-Protection',
                        value: '1; mode=block',
                    },
                    {
                        key: 'Referrer-Policy',
                        value: 'strict-origin-when-cross-origin',
                    },
                    {
                        key: 'Permissions-Policy',
                        value: 'camera=(), microphone=(), geolocation=(), browsing-topics=()',
                    }
                ],
            },
        ];
    },
};

export default nextConfig;
```

- [ ] **Step 2: Verify dev build still works**

```bash
cd frontend
npm run build
```

Expected: build succeeds. No CSP errors.

- [ ] **Step 3: Verify the connect-src in a production scenario**

```bash
cd frontend
NEXT_PUBLIC_API_URL=https://api.thereplicant.railway.app/api/v1 npm run build 2>&1 | head -20
```

Expected: build succeeds. If you inspect the generated HTML headers, `connect-src` will reference `https://api.thereplicant.railway.app` instead of `localhost`.

- [ ] **Step 4: Commit**

```bash
git add frontend/next.config.mjs
git commit -m "fix(csp): derive connect-src from NEXT_PUBLIC_API_URL

Removes hardcoded http://localhost:8080 from production CSP.
Set NEXT_PUBLIC_API_URL in Vercel env vars for the build."
```

---

### Task 6: Add Next.js middleware for server-side admin route protection

**Files:**
- Create: `frontend/src/middleware.ts`

Currently all admin route protection happens in `admin/layout.tsx` via `useEffect` — a client-side guard that runs after the page renders. A server-side middleware check prevents the flash and stops crawlers from seeing admin HTML.

The middleware only checks for the cookie's presence (it cannot verify the JWT signature without the secret). The backend enforces actual auth. This guard is for UX and to prevent admin HTML from being served to unauthenticated requests.

- [ ] **Step 1: Create `frontend/src/middleware.ts`**

```typescript
import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Server-side admin route guard.
 *
 * Checks for the presence of the httpOnly accessToken cookie before
 * rendering any /admin/* page. Redirects to /login if missing.
 *
 * Note: This does NOT validate the JWT signature — the backend enforces that.
 * This prevents unauthenticated requests from receiving admin HTML at all.
 */
export function middleware(request: NextRequest) {
    const accessToken = request.cookies.get("accessToken");

    if (!accessToken?.value) {
        const loginUrl = new URL("/login", request.url);
        loginUrl.searchParams.set("from", request.nextUrl.pathname);
        return NextResponse.redirect(loginUrl);
    }

    return NextResponse.next();
}

export const config = {
    matcher: "/admin/:path*",
};
```

- [ ] **Step 2: Start the dev server and verify the redirect works**

```bash
cd frontend
npm run dev
```

Open `http://localhost:3000/admin` in an incognito window (no cookies). Expected: redirected to `/login?from=/admin` immediately — no admin HTML visible.

- [ ] **Step 3: Verify authenticated users can still access admin**

Log in via `http://localhost:3000/login`. After login, navigate to `/admin`. Expected: admin dashboard loads normally.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/middleware.ts
git commit -m "feat(auth): add Next.js middleware for server-side admin route guard

Redirects unauthenticated requests from /admin/* to /login
before any admin HTML is rendered or served."
```

---

### Task 7: Update frontend `api.ts` and `auth.tsx` to match cookie-based refresh

**Files:**
- Modify: `frontend/src/lib/api.ts`
- Modify: `frontend/src/lib/auth.tsx`

After Tasks 3–4, the backend no longer accepts a refresh token in the body and no longer returns tokens in the response body. The frontend must stop sending/storing the refresh token.

- [ ] **Step 1: Update `api.ts` — remove refresh token from `logout` and `refreshToken` methods**

In `api.ts`, replace the `logout` and `refreshToken` methods:

```typescript
// Replace this:
async logout(refreshToken: string): Promise<void> {
    return this.request<void>("/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
    });
}

async refreshToken(refreshToken: string): Promise<AuthResponse> {
    return this.request<AuthResponse>("/auth/refresh", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
    });
}

// With this:
async logout(): Promise<void> {
    return this.request<void>("/auth/logout", {
        method: "POST",
    });
}

async refreshToken(): Promise<AuthResponse> {
    return this.request<AuthResponse>("/auth/refresh", {
        method: "POST",
    });
}
```

- [ ] **Step 2: Update `auth.tsx` — remove refresh token from localStorage**

Replace the full `auth.tsx` with the following. Key changes:
  - `saveAuthData` no longer stores `refreshToken`
  - `clearAuthData` only clears `user`
  - `logout` calls `api.logout()` with no argument
  - The `AuthResponse` type import still works (refreshToken field will be null from backend)

```typescript
"use client";

/**
 * Auth Context for The Replicant.
 *
 * Provides authentication state management across the app:
 * - Login / Setup / Logout flows
 * - User state in localStorage (non-sensitive: name, email, role only)
 * - Tokens handled exclusively via httpOnly cookies (set by backend)
 * - Auto-redirect on 401 handled in api.ts
 */

import {
    createContext,
    useContext,
    useState,
    useEffect,
    useCallback,
    type ReactNode,
} from "react";
import { api } from "./api";
import type { UserDTO, AuthResponse } from "./types";

interface AuthState {
    user: UserDTO | null;
    isAuthenticated: boolean;
    isLoading: boolean;
}

interface AuthContextType extends AuthState {
    login: (email: string, password: string) => Promise<void>;
    setup: (email: string, name: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function saveAuthData(response: AuthResponse): void {
    // Only the non-sensitive user profile is stored in localStorage.
    // Tokens are managed exclusively by the backend via httpOnly cookies.
    localStorage.setItem("user", JSON.stringify(response.user));
}

function clearAuthData(): void {
    localStorage.removeItem("user");
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [state, setState] = useState<AuthState>({
        user: null,
        isAuthenticated: false,
        isLoading: true,
    });

    // Restore user profile from localStorage on mount.
    // Actual session validity is enforced by the backend on each API call
    // via the httpOnly accessToken cookie. A 401 response clears this state
    // and redirects to /login (handled in api.ts).
    useEffect(() => {
        const userStr = localStorage.getItem("user");
        if (userStr) {
            try {
                const user: UserDTO = JSON.parse(userStr);
                setState({ user, isAuthenticated: true, isLoading: false });
            } catch {
                clearAuthData();
                setState({ user: null, isAuthenticated: false, isLoading: false });
            }
        } else {
            setState({ user: null, isAuthenticated: false, isLoading: false });
        }
    }, []);

    const handleAuthResponse = useCallback((response: AuthResponse) => {
        saveAuthData(response);
        setState({
            user: response.user,
            isAuthenticated: true,
            isLoading: false,
        });
    }, []);

    const login = useCallback(
        async (email: string, password: string) => {
            const response = await api.login({ email, password });
            handleAuthResponse(response);
        },
        [handleAuthResponse]
    );

    const setup = useCallback(
        async (email: string, name: string, password: string) => {
            const response = await api.setup({ email, name, password });
            handleAuthResponse(response);
        },
        [handleAuthResponse]
    );

    const logout = useCallback(async () => {
        try {
            await api.logout();
        } catch {
            // Logout should always succeed on the client side
        } finally {
            clearAuthData();
            setState({ user: null, isAuthenticated: false, isLoading: false });
            window.location.href = "/login";
        }
    }, []);

    return (
        <AuthContext.Provider value={{ ...state, login, setup, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}
```

- [ ] **Step 3: Verify the full login → admin → logout flow manually**

```bash
cd frontend
npm run dev
```

1. Go to `http://localhost:3000/login`, log in
2. Confirm you land on `/admin`
3. Open DevTools → Application → Cookies: verify `accessToken` and `refreshToken` cookies exist; verify `refreshToken` is NOT in localStorage
4. Click logout, verify both cookies are cleared and you are on `/login`

- [ ] **Step 4: Commit**

```bash
git add frontend/src/lib/api.ts frontend/src/lib/auth.tsx
git commit -m "feat(auth): remove refresh token from localStorage

Tokens are now managed exclusively via httpOnly cookies.
Only non-sensitive user profile (name, email, role) stays in localStorage."
```

---

## Section 4 — Performance & API Design

### Task 8: Fix double-fetch on post detail page with `React.cache()`

**Files:**
- Modify: `frontend/src/app/(blog)/posts/[slug]/page.tsx`

`generateMetadata` and the page component both call `api.getPostBySlug(slug)` independently. Since both run on the server in the same request, wrapping the call with `React.cache()` deduplicates it — the second call returns the memoized result from the first with zero network overhead.

- [ ] **Step 1: Update `posts/[slug]/page.tsx`**

Add the `cache` import and extract the fetch into a cached function. Replace the top of the file up to the component:

```typescript
import { cache } from "react";
import { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { api } from "@/lib/api";
import { MarkdownRenderer } from "@/components/blog/MarkdownRenderer";
import { SITE_NAME, BASE_URL } from "@/lib/constants";
import { ArrowLeft, Calendar, Clock, User, Tag } from "lucide-react";

// ISR revalidation for posts (60 seconds)
export const revalidate = 60;

interface PageProps {
    params: Promise<{ slug: string }>;
}

/**
 * Cached per-request fetch — deduplicates the call between
 * generateMetadata() and the page component within the same render.
 */
const fetchPost = cache(async (slug: string) => {
    return api.getPostBySlug(slug, { next: { revalidate: 60 } });
});

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
    try {
        const resolvedParams = await params;
        const response = await fetchPost(resolvedParams.slug);
        const post = response.data;

        return {
            title: `${post.title} | ${SITE_NAME}`,
            description: post.excerpt || `Read ${post.title} on ${SITE_NAME}`,
            openGraph: {
                title: post.title,
                description: post.excerpt || `Read ${post.title} on ${SITE_NAME}`,
                type: "article",
                url: `${BASE_URL}/posts/${resolvedParams.slug}`,
                siteName: SITE_NAME,
            },
        };
    } catch {
        return { title: `Post Not Found | ${SITE_NAME}` };
    }
}

export default async function PostDetailPage({ params }: PageProps) {
    let post;
    try {
        const resolvedParams = await params;
        const response = await fetchPost(resolvedParams.slug);
        post = response.data;
    } catch {
        notFound();
    }
    // ... rest of the component unchanged
```

- [ ] **Step 2: Verify build passes**

```bash
cd frontend && npm run build
```

Expected: builds without errors.

- [ ] **Step 3: Commit**

```bash
git add "frontend/src/app/(blog)/posts/[slug]/page.tsx"
git commit -m "perf(blog): deduplicate post fetch with React.cache()

generateMetadata and the page component shared a single cached
fetch instead of making two independent requests to the backend."
```

---

### Task 9: Standardize category update from `PUT` to `PATCH`

**Files:**
- Modify: `backend/src/main/java/com/thereplicant/api/controller/CategoryController.java`
- Modify: `frontend/src/lib/api.ts`

`CategoryController` uses `@PutMapping` but `PostController` uses `@PatchMapping` for partial updates. Standardize to `PATCH` (semantically correct — only provided fields are updated).

- [ ] **Step 1: Write the backend test**

In `backend/src/test/java/com/thereplicant/api/CategoryServiceTest.java`, verify the update service method works (it's already tested; this confirms the controller method change doesn't break it):

```bash
./mvnw test -Dtest=CategoryServiceTest
```

Expected: all existing tests pass (baseline).

- [ ] **Step 2: Change `@PutMapping` to `@PatchMapping` in `CategoryController.java`**

In `CategoryController.java` line 88, change:

```java
// FROM:
@PutMapping("/{id}")

// TO:
@PatchMapping("/{id}")
```

- [ ] **Step 3: Change `"PUT"` to `"PATCH"` in `api.ts`**

In `frontend/src/lib/api.ts` inside `updateCategory`, change:

```typescript
// FROM:
async updateCategory(
    id: string,
    data: CreateCategoryRequest
): Promise<ApiResponse<CategoryDTO>> {
    return this.request<ApiResponse<CategoryDTO>>(`/categories/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
    });
}

// TO:
async updateCategory(
    id: string,
    data: CreateCategoryRequest
): Promise<ApiResponse<CategoryDTO>> {
    return this.request<ApiResponse<CategoryDTO>>(`/categories/${id}`, {
        method: "PATCH",
        body: JSON.stringify(data),
    });
}
```

- [ ] **Step 4: Run backend tests + frontend lint**

```bash
cd backend && ./mvnw test
cd ../frontend && npm run lint
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/thereplicant/api/controller/CategoryController.java
git add frontend/src/lib/api.ts
git commit -m "fix(api): standardize category update to PATCH

Was PUT (full replace semantics). Both posts and categories
now consistently use PATCH for partial updates."
```

---

### Task 10: Fix `X-Forwarded-For` IP extraction in `RateLimitFilter`

**Files:**
- Modify: `backend/src/main/java/com/thereplicant/api/security/RateLimitFilter.java`

The current code takes the **first** (leftmost) value from `X-Forwarded-For`, which is the one the client controls. An attacker can bypass rate limiting by sending `X-Forwarded-For: 1.2.3.4` (fake IP). Railway's load balancer appends the real client IP as the **last** value in the chain. Taking the last value makes spoofing significantly harder.

- [ ] **Step 1: Update `getClientIP` in `RateLimitFilter.java`**

Replace the `getClientIP` method:

```java
/**
 * Extract the real client IP from the request.
 *
 * Uses the LAST value in X-Forwarded-For because Railway (and most
 * reverse proxies) append the real client IP at the end of the chain.
 * The first value is client-controlled and can be spoofed.
 *
 * Falls back to getRemoteAddr() if the header is absent.
 */
private String getClientIP(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null || xfHeader.isBlank() || "unknown".equalsIgnoreCase(xfHeader)) {
        return request.getRemoteAddr();
    }
    String[] parts = xfHeader.split(",");
    return parts[parts.length - 1].trim();
}
```

- [ ] **Step 2: Run backend tests**

```bash
cd backend && ./mvnw test
```

Expected: all tests pass.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/thereplicant/api/security/RateLimitFilter.java
git commit -m "fix(security): use rightmost X-Forwarded-For IP for rate limiting

Leftmost value is client-controlled and can be spoofed to bypass
rate limits. Railway appends the real IP last in the chain."
```

---

## Section 5 — UI & Accessibility

### Task 11: Add `focus-visible` rings to all form inputs

**Files:**
- Modify: `frontend/src/app/login/page.tsx`
- Modify: `frontend/src/app/admin/posts/new/page.tsx`
- Modify: `frontend/src/app/admin/posts/[id]/edit/page.tsx`

All form inputs currently use `focus:outline-none` which removes the visible focus ring without providing a replacement. This breaks keyboard navigation. The fix adds `focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)]` and keeps the border change.

The class pattern to replace is identical across all three files. Every input and textarea with `focus:outline-none` gets the ring added.

- [ ] **Step 1: Update `login/page.tsx` — email and password inputs**

For each input in the file, replace the focus classes:

```typescript
// Email input (line ~141) — FROM:
className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"

// TO:
className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors"

// Password input (line ~162) — same replacement
```

- [ ] **Step 2: Update `admin/posts/new/page.tsx` — title, content, excerpt, category select, status select**

Apply the same focus class pattern to all 5 form controls in the file. Each `focus:outline-none` becomes `focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)]`.

Example for the title input (line ~121):

```typescript
// FROM:
className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus:outline-none transition-colors"

// TO:
className="w-full rounded-lg bg-[var(--color-surface)] border border-[var(--color-border)] px-4 py-3 text-[var(--color-text)] placeholder-[var(--color-text-faint)] focus:border-[var(--color-accent)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] focus-visible:ring-offset-1 focus-visible:ring-offset-[var(--color-surface)] transition-colors"
```

Apply this to all inputs, textareas, and selects in the file.

- [ ] **Step 3: Apply the same fix to `admin/posts/[id]/edit/page.tsx`**

This file has the same form structure as `new/page.tsx`. Apply the identical class replacement to all form controls.

- [ ] **Step 4: Manually verify**

```bash
cd frontend && npm run dev
```

Open `http://localhost:3000/login`, press Tab. The email input should show a visible amber ring when focused via keyboard. Mouse click should show a ring too (using `:focus-visible`).

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/login/page.tsx
git add "frontend/src/app/admin/posts/new/page.tsx"
git add "frontend/src/app/admin/posts/[id]/edit/page.tsx"
git commit -m "fix(a11y): add focus-visible rings to all form inputs

Replaces focus:outline-none with focus-visible:ring-2 to restore
keyboard navigation visibility without affecting mouse users."
```

---

### Task 12: Add `aria-hidden` to decorative icons; fix Toast close button

**Files:**
- Modify: `frontend/src/app/(blog)/posts/[slug]/page.tsx`
- Modify: `frontend/src/components/ui/Toast.tsx`

Decorative icons next to text convey no additional meaning to screen readers — adding `aria-hidden="true"` prevents them from being announced as unlabeled elements. The Toast close button also lacks an `aria-label`.

- [ ] **Step 1: Update `posts/[slug]/page.tsx` — metadata icons**

In the metadata section (lines ~115–127), add `aria-hidden="true"` to the four decorative icons:

```typescript
// FROM:
<User className="h-3 w-3" />
// TO:
<User className="h-3 w-3" aria-hidden="true" />

// FROM:
<Calendar className="h-3 w-3" />
// TO:
<Calendar className="h-3 w-3" aria-hidden="true" />

// FROM:
<Clock className="h-3 w-3" />
// TO:
<Clock className="h-3 w-3" aria-hidden="true" />

// FROM:
<Tag className="h-3.5 w-3.5 text-[var(--color-text-faint)]" />
// TO:
<Tag className="h-3.5 w-3.5 text-[var(--color-text-faint)]" aria-hidden="true" />
```

- [ ] **Step 2: Update `Toast.tsx` — fix close button and icons**

Replace the `Toast` component:

```typescript
"use client";

import { useEffect } from "react";
import { CheckCircle, XCircle, X } from "lucide-react";

interface ToastProps {
    message: string;
    type: "success" | "error";
    onClose: () => void;
}

export function Toast({ message, type, onClose }: ToastProps) {
    useEffect(() => {
        const timer = setTimeout(onClose, 4000);
        return () => clearTimeout(timer);
    }, [onClose]);

    const Icon = type === "success" ? CheckCircle : XCircle;
    const borderColor =
        type === "success" ? "border-emerald-500/30" : "border-red-500/30";
    const iconColor =
        type === "success" ? "text-emerald-400" : "text-red-400";

    return (
        <div
            role="status"
            aria-live="polite"
            aria-atomic="true"
            className="fixed bottom-6 right-6 z-50 animate-[slideUp_0.3s_ease-out]"
        >
            <div
                className={`glass-panel flex items-center gap-3 px-4 py-3 border ${borderColor} max-w-sm`}
            >
                <Icon className={`h-5 w-5 shrink-0 ${iconColor}`} aria-hidden="true" />
                <p className="text-sm text-[var(--color-text)]">{message}</p>
                <button
                    onClick={onClose}
                    aria-label="Dismiss notification"
                    className="ml-2 text-[var(--color-text-faint)] hover:text-[var(--color-text)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-accent)] rounded"
                >
                    <X className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
            </div>
        </div>
    );
}
```

Changes: `role="status"` + `aria-live="polite"` + `aria-atomic="true"` on the wrapper; `aria-hidden="true"` on both icons; `aria-label="Dismiss notification"` on the close button; focus-visible ring on the close button.

- [ ] **Step 3: Run lint**

```bash
cd frontend && npm run lint
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add "frontend/src/app/(blog)/posts/[slug]/page.tsx"
git add frontend/src/components/ui/Toast.tsx
git commit -m "fix(a11y): aria-hidden decorative icons, aria-live toast, fix close button

- Metadata icons in post detail page get aria-hidden=true
- Toast gets role=status + aria-live=polite for screen reader announcements
- Toast close button gets aria-label and focus-visible ring
- Icon inside close button gets aria-hidden=true"
```

---

### Task 13: Fix `"..."` → `"…"` and apply to all ellipsis occurrences

**Files:**
- Modify: `frontend/src/app/admin/posts/new/page.tsx`
- Modify: `frontend/src/app/admin/posts/[id]/edit/page.tsx`

The Web Interface Guidelines require the Unicode ellipsis character `…` (U+2026) instead of three periods `...`. Two occurrences are in the admin post forms.

- [ ] **Step 1: Fix `admin/posts/new/page.tsx`**

```typescript
// Line ~159 — preview empty state
// FROM:
<p className="text-[var(--color-text-faint)] text-sm">
    Nothing to preview yet...
</p>
// TO:
<p className="text-[var(--color-text-faint)] text-sm">
    Nothing to preview yet…
</p>

// Line ~280 — saving spinner label
// FROM:
SAVING...
// TO:
SAVING…
```

- [ ] **Step 2: Apply the same fix to `admin/posts/[id]/edit/page.tsx`**

This file has the same "Nothing to preview yet..." and "SAVING..." strings. Apply the same replacements.

- [ ] **Step 3: Scan for any remaining `"..."` in TSX files**

```bash
grep -rn '\.\.\.' frontend/src --include="*.tsx" | grep -v "node_modules"
```

Inspect output. If `"..."` appears in user-visible strings (not TypeScript spread operators or JSX spread props), fix them. Ignore occurrences that are JavaScript/TypeScript syntax (spread, optional chaining).

- [ ] **Step 4: Commit**

```bash
git add "frontend/src/app/admin/posts/new/page.tsx"
git add "frontend/src/app/admin/posts/[id]/edit/page.tsx"
git commit -m "fix(copy): replace ... with Unicode ellipsis character in UI strings"
```

---

### Task 14: Add `prefers-reduced-motion` global rule

**Files:**
- Modify: `frontend/src/app/globals.css`

The `animate-spin` (used on loaders) and the slide-up animation on `Toast` do not respect the user's OS-level "reduce motion" preference. A single CSS media query in `globals.css` covers all animations globally.

- [ ] **Step 1: Add the rule to `globals.css`**

Append to the end of `frontend/src/app/globals.css`:

```css
/* ============================================================
 * Accessibility — Reduced Motion
 * Respects the OS "reduce motion" preference by disabling
 * all CSS transitions and animations.
 * ============================================================ */
@media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
        scroll-behavior: auto !important;
    }
}
```

- [ ] **Step 2: Verify build passes**

```bash
cd frontend && npm run build
```

Expected: build succeeds, no CSS errors.

- [ ] **Step 3: Manual verification**

On macOS: System Settings → Accessibility → Display → Reduce Motion (enable).
Reload `http://localhost:3000` and navigate to a page with a spinner or the Toast component. Expected: no animation plays; elements appear/disappear instantly.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/globals.css
git commit -m "fix(a11y): add prefers-reduced-motion global CSS rule

Disables all CSS animations and transitions for users who have
enabled the 'reduce motion' accessibility setting in their OS."
```

---

## Self-Review Checklist

### Spec coverage

| Audit finding | Task |
|---|---|
| 🔴 Cookie `secure(false)` | Task 2, Task 4 |
| 🔴 Swagger publicly accessible in prod | Task 1 |
| 🔴 Actuator details exposed without auth | Task 1 |
| 🔴 `server.error.include-message: always` | Task 1 |
| 🟠 `ddl-auto: update` in default profile | Task 1 |
| 🟠 refreshToken in localStorage | Tasks 3, 4, 7 |
| 🟠 CSP `http://localhost:8080` hardcoded | Task 5 |
| 🟠 No Next.js admin middleware | Task 6 |
| 🟡 Post page double-fetch | Task 8 |
| 🟡 Category PUT vs PATCH inconsistency | Task 9 |
| 🟡 X-Forwarded-For spoofable | Task 10 |
| 🔵 `focus:outline-none` without replacement | Task 11 |
| 🔵 Decorative icons without `aria-hidden` | Task 12 |
| 🔵 Toast missing `aria-live` + close button `aria-label` | Task 12 |
| 🔵 `"..."` instead of `"…"` | Task 13 |
| 🔵 No `prefers-reduced-motion` | Task 14 |

All 16 audit findings are covered. ✓

### Type consistency check

- `api.ts: logout()` — no args. `auth.tsx: api.logout()` — no args. ✓
- `api.ts: refreshToken()` — no args. ✓
- `AuthService.refreshToken(String)` — called from `AuthController` with the cookie value string. ✓
- `React.cache` wraps `api.getPostBySlug(slug, options)` — same signature as before. ✓
- `@PatchMapping` in `CategoryController` + `method: "PATCH"` in `api.ts`. ✓
