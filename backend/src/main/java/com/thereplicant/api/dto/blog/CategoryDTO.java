package com.thereplicant.api.dto.blog;

import lombok.*;

import java.util.UUID;

/**
 * Category data transfer object for API responses.
 * Includes postCount for display purposes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private Long postCount;
}
