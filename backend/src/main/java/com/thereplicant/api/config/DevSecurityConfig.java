package com.thereplicant.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * ⚠️ TEMPORARY: Mock UserDetailsService for development.
 * 
 * This provides a hardcoded user for testing authentication flow
 * before the real User entity and repository are implemented.
 * 
 * Mock user credentials:
 * - Email: admin@thereplicant.com
 * - Password: admin123
 * 
 * TODO: Replace this with real UserDetailsService after User entity is created.
 */
@Configuration
@Profile("!prod") // Only active in dev/test, NOT production
public class DevSecurityConfig {

    /**
     * Temporary in-memory user for development testing.
     * This bean will be replaced by the real implementation.
     */
    @Bean
    public UserDetailsService devUserDetailsService() {
        return username -> User.builder()
                .username(username)
                .password(new BCryptPasswordEncoder(12).encode("admin123"))
                .roles("ADMIN")
                .build();
    }
}
