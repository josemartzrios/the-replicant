package com.thereplicant.api.config;

import com.thereplicant.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security Configuration for The Replicant API.
 * 
 * Configures JWT-based stateless authentication with:
 * - Public/protected endpoint authorization
 * - OWASP Security Headers (CSP, X-Frame-Options, HSTS)
 * - CORS for frontend communication
 * - Stateless session management
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

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * Endpoints accessible without authentication (any HTTP method).
     * Following OWASP: minimum necessary access.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            // Health & Monitoring
            "/actuator/**",
            "/health",
            "/health/**",

            // API Documentation
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // Authentication endpoints (logout excluded - requires JWT)
            "/api/v1/auth/status",
            "/api/v1/auth/setup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",

            // Development testing (disabled in prod via @Profile)
            "/api/test/**"
    };

    /**
     * Blog content endpoints — public for GET only.
     * POST/PATCH/DELETE to these paths require authentication.
     */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/posts",
            "/api/v1/posts/search",
            "/api/v1/posts/{slug}",
            "/api/v1/categories",
            "/api/v1/tags"
    };

    /**
     * Main security filter chain configuration.
     * 
     * Security decisions:
     * - CSRF disabled: Stateless JWT-based auth doesn't need CSRF
     * - Session: STATELESS - no server-side session storage
     * - Headers: OWASP recommended security headers
     * - CORS: Configured for frontend origin
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF - not needed for stateless JWT auth
                .csrf(AbstractHttpConfigurer::disable)

                // CORS configuration for frontend
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // OWASP Security Headers
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(frame -> frame.deny())
                        // Prevent MIME type sniffing
                        .contentTypeOptions(contentType -> {
                        })
                        // Content Security Policy - restrict resource loading
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        // Force HTTPS (1 year)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)))

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
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
     * CORS configuration.
     * Restricts cross-origin requests to allowed frontend origins.
     * OWASP: Never use wildcard (*) in production.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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
