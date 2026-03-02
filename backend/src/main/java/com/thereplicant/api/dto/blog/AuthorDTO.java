package com.thereplicant.api.dto.blog;

import lombok.*;

import java.util.UUID;

/**
 * Lightweight author representation for post responses.
 * Only exposes id and name — no sensitive user data.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {

    private UUID id;
    private String name;
}
