package com.thereplicant.api.dto.blog;

import lombok.*;

import java.util.UUID;

/**
 * Tag data transfer object for API responses.
 * Includes postCount for display purposes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO {

    private UUID id;
    private String name;
    private String slug;
    private Long postCount;
}
