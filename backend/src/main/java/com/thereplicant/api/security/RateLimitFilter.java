package com.thereplicant.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter to prevent brute-force attacks and abuse.
 * Uses Bucket4j for token-bucket algorithm per IP Address.
 */
@Component
@Profile("!test")
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    // Global limits: 100 requests per minute per IP
    private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();

    // Login limits: Brute force protection - 5 requests per 15 minutes per IP
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIP(request);
        String path = request.getRequestURI();

        // 1. Check strict Login Endpoint Rate Limits
        if (path.startsWith("/api/v1/auth/login")) {
            Bucket loginBucket = loginBuckets.computeIfAbsent(ip, this::createLoginBucket);
            if (!loginBucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for LOGIN endpoint from IP: {}", ip);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many login attempts. Please try again later.\"}");
                return;
            }
        }

        // 2. Check Global API Limits
        if (path.startsWith("/api/")) {
            Bucket globalBucket = globalBuckets.computeIfAbsent(ip, this::createGlobalBucket);
            if (!globalBucket.tryConsume(1)) {
                log.warn("Global rate limit exceeded from IP: {}", ip);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many requests. Please slow down.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createGlobalBucket(String ip) {
        // 100 requests per minute
        Bandwidth limit = Bandwidth.builder().capacity(100).refillGreedy(100, Duration.ofMinutes(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createLoginBucket(String ip) {
        // 5 requests per 15 minutes
        Bandwidth limit = Bandwidth.builder().capacity(5).refillGreedy(5, Duration.ofMinutes(15)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
