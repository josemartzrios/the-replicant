package com.thereplicant.api.controller;

import com.thereplicant.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ⚠️ DEVELOPMENT ONLY - Token testing endpoint.
 * 
 * This controller allows testing JWT generation without a full auth flow.
 * Only active in 'default' and 'dev' profiles (NOT in production).
 * 
 * Usage:
 * GET /api/test/token?email=test@example.com
 * 
 * DELETE THIS FILE before production deployment!
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("dev") // Only active when 'dev' profile is explicitly enabled
public class DevTokenController {

    private final JwtService jwtService;

    /**
     * Generate a test JWT token for development purposes.
     * 
     * @param email The email/username to encode in the token
     * @return Map containing access token and refresh token
     */
    @GetMapping("/token")
    public Map<String, String> generateTestToken(
            @RequestParam(defaultValue = "admin@thereplicant.com") String email) {
        // Create a mock user for testing
        UserDetails mockUser = User.builder()
                .username(email)
                .password("not-used-for-token-generation")
                .authorities(Collections.emptyList())
                .build();

        // Generate tokens
        String accessToken = jwtService.generateToken(mockUser);
        String refreshToken = jwtService.generateRefreshToken(mockUser);

        Map<String, String> response = new HashMap<>();
        response.put("email", email);
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("type", "Bearer");
        response.put("expiresIn", "86400000ms (24h)");
        response.put("warning", "⚠️ DEV ONLY - This endpoint is disabled in production");

        return response;
    }
}
