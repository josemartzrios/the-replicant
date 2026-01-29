package com.thereplicant.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoints for monitoring and orchestration.
 * These endpoints are used by load balancers, Kubernetes probes,
 * and monitoring systems to verify service availability.
 */
@RestController
@Tag(name = "Health", description = "Service health check endpoints for monitoring and orchestration")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Basic health check", description = "Returns the basic health status of the service. " +
            "Use this endpoint for simple uptime monitoring.", responses = {
                    @ApiResponse(responseCode = "200", description = "Service is healthy and responding", content = @Content(schema = @Schema(implementation = HealthResponse.class)))
            })
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()));
    }

    @GetMapping("/health/ready")
    @Operation(summary = "Readiness check", description = "Checks if the service is ready to receive traffic. " +
            "Validates connectivity to dependent services like the database. " +
            "Use this for Kubernetes readiness probes.", responses = {
                    @ApiResponse(responseCode = "200", description = "Service is ready to accept requests"),
                    @ApiResponse(responseCode = "503", description = "Service is not ready (database unavailable)")
            })
    public ResponseEntity<Map<String, Object>> readiness() {
        // TODO: Add actual database connectivity check when JPA is configured
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "checks", Map.of(
                        "database", "NOT_CONFIGURED")));
    }

    @GetMapping("/health/live")
    @Operation(summary = "Liveness check", description = "Checks if the service is alive and not deadlocked. " +
            "A failed liveness check indicates the service should be restarted. " +
            "Use this for Kubernetes liveness probes.", responses = {
                    @ApiResponse(responseCode = "200", description = "Service is alive and responsive")
            })
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()));
    }

    /**
     * Schema for health response documentation.
     */
    @Schema(description = "Health check response")
    private record HealthResponse(
            @Schema(description = "Health status", example = "UP", allowableValues = {
                    "UP", "DOWN" }) String status,

            @Schema(description = "Timestamp of the check", example = "2026-01-28T19:00:00Z") String timestamp) {
    }
}
