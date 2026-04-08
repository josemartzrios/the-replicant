package com.thereplicant.api.dto.auth;

import com.thereplicant.api.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Safe user projection for API responses.
 * Excludes sensitive data (password hash, internal fields).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private UUID id;
    private String email;
    private String name;
    private Role role;
}
