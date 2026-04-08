package com.thereplicant.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for auth status check.
 * Indicates whether first-time setup is required.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthStatusResponse {

    /**
     * True if no users exist and setup is needed.
     */
    private boolean setupRequired;
}
