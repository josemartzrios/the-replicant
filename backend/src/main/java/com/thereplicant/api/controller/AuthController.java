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
        // SameSite=None requires Secure=true (enforced by all modern browsers, strictly by mobile Safari).
        // In dev (cookieSecure=false, HTTP), fall back to SameSite=Lax so cookies still work locally.
        String sameSite = cookieSecure ? "None" : "Lax";

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", authData.getAccessToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(authData.getExpiresIn() > 0 ? authData.getExpiresIn() : ACCESS_TOKEN_MAX_AGE)
                .sameSite(sameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (authData.getRefreshToken() != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authData.getRefreshToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/api/v1/auth")
                    .maxAge(REFRESH_TOKEN_MAX_AGE)
                    .sameSite(sameSite)
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

        String sameSite = cookieSecure ? "None" : "Lax";
        ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite(sameSite).build();
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(cookieSecure).path("/api/v1/auth").maxAge(0).sameSite(sameSite).build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, clearAccess.toString());
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        return ResponseEntity.noContent().build();
    }
}
