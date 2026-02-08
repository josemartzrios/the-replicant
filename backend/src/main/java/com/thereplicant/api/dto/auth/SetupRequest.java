package com.thereplicant.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for first admin setup.
 * Used only when no users exist in the system.
 * 
 * Validation rules (OWASP):
 * - Email: valid format, max 255 chars
 * - Password: min 8 chars (enforced here), complexity enforced in service
 * - Name: 2-100 chars
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
}
