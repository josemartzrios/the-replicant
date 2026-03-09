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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication REST controller.
 * 
 * Endpoints:
 * - GET /auth/status - Check if first-time setup is required
 * - POST /auth/setup - Create first admin (one-time only)
 * - POST /auth/login - Authenticate user
 * - POST /auth/refresh - Refresh access token
 * - POST /auth/logout - Revoke all tokens (requires JWT)
 * 
 * All endpoints are public except logout (requires valid JWT).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and authorization")
public class AuthController {

    private final AuthService authService;

    /**
     * Helper method to attach JWT as an HttpOnly cookie
     */
    private void setTokenCookie(HttpServletResponse response, AuthResponse authData) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", authData.getAccessToken())
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .maxAge(authData.getExpiresIn())
                .sameSite("Lax") // "Lax" or "Strict" to prevent CSRF
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // We remove the accessToken from the raw JSON response payload
        // to enforce frontend relying strictly on the Cookie for auth calls.
        authData.setAccessToken(null);
    }

    /**
     * Check if first-time setup is required.
     * Returns true if no users exist in the system.
     */
    @GetMapping("/status")
    @Operation(summary = "Check if setup is required", description = "Returns whether the system needs first-time admin setup")
    @ApiResponse(responseCode = "200", description = "Auth status returned")
    public ResponseEntity<AuthStatusResponse> getStatus() {
        return ResponseEntity.ok(authService.checkStatus());
    }

    /**
     * First-time admin setup.
     * Only works when no users exist. Creates the first admin user.
     */
    @PostMapping("/setup")
    @Operation(summary = "First admin setup (one-time only)", description = "Creates the first admin user. Only works when no users exist.")
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

    /**
     * Authenticate user with email and password.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT tokens")
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

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange refresh token for new access and refresh tokens")
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

    /**
     * Logout user by revoking all their refresh tokens.
     * Requires valid JWT (protected endpoint).
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revokes all refresh tokens for the authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user, HttpServletResponse httpServletResponse) {
        authService.logout(user);

        // Clear the cookie by setting max age to 0
        ResponseCookie cookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }
}
