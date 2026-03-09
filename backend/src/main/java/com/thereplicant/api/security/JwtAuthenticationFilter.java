package com.thereplicant.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 * 
 * Intercepts every HTTP request and:
 * 1. Extracts JWT from Authorization header
 * 2. Validates the token
 * 3. Sets Spring Security context if valid
 * 
 * Security considerations (OWASP):
 * - Tokens are validated on every request (stateless)
 * - No token = no authentication (fail securely)
 * - Expired/invalid tokens are rejected
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract token from cookies
        String jwt = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // Fallback to Authorization header (optional, e.g. for Postman testing)
        if (jwt == null) {
            final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                jwt = authHeader.substring(BEARER_PREFIX.length());
            }
        }

        // If no token found, continue without auth
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 3. Extract username from token
            final String userEmail = jwtService.extractUsername(jwt);

            // 4. Validate: user exists and not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // 5. Validate token against user
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // 6. Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // No credentials needed - already authenticated via JWT
                            userDetails.getAuthorities());

                    // Add request details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 7. Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authenticated user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            // Log the error but continue - request will be treated as unauthenticated
            // This follows "fail securely" principle
            log.warn("JWT authentication failed: {}", e.getMessage());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
