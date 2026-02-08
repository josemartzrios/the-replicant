package com.thereplicant.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for successful authentication.
 * Contains tokens and user info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * JWT access token for API authentication.
     * Include in Authorization header: Bearer {accessToken}
     */
    private String accessToken;

    /**
     * Refresh token for obtaining new access tokens.
     * Use with POST /api/v1/auth/refresh
     */
    private String refreshToken;

    /**
     * Access token expiry in seconds.
     */
    private Long expiresIn;

    /**
     * Authenticated user information.
     */
    private UserDTO user;
}
