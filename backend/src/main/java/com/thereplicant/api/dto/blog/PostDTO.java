package com.thereplicant.api.dto.blog;

import com.thereplicant.api.entity.PostStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full post data transfer object for API responses.
 * Matches the PostDTO schema defined in api-spec.yaml.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDTO {

    private UUID id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private PostStatus status;
    private CategoryDTO category;
    private List<TagDTO> tags;
    private AuthorDTO author;
    private Integer readingTimeMinutes;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
