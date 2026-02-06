package com.thereplicant.api.config;

import com.thereplicant.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration for The Replicant API.
 * 
 * Configures JWT-based stateless authentication with the following security
 * rules:
 * - Public endpoints: health checks, API docs, auth endpoints
 * - Protected endpoints: All other API routes require valid JWT
 * 
 * @see JwtAuthenticationFilter for JWT token validation
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Public endpoints that don't require authentication.
     * Following OWASP guidelines: minimum necessary access.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // Health & Monitoring
            "/actuator/**",
            "/health",

            // API Documentation
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // Authentication endpoints
            "/api/v1/auth/**",

            // Public blog content (read-only)
            "/api/v1/posts",
            "/api/v1/posts/{slug}",
            "/api/v1/categories",
            "/api/v1/tags",

            // Development testing (disabled in prod via @Profile)
            "/api/test/**"
    };

    /**
     * Main security filter chain configuration.
     * 
     * Security decisions:
     * - CSRF disabled: Stateless JWT-based auth doesn't need CSRF protection
     * - Session: STATELESS - no server-side session storage
     * - JWT filter: Validates token before Spring Security's auth filter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF - not needed for stateless JWT auth
                .csrf(AbstractHttpConfigurer::disable)

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())

                // Stateless session - no cookies, no server-side sessions
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Use our custom authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * Authentication provider using DAO pattern.
     * Connects UserDetailsService with password encoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder with strength 12.
     * OWASP recommendation: BCrypt with cost factor >= 10
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Exposes AuthenticationManager as a bean for use in AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
