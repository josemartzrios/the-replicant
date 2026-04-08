package com.thereplicant.api.dto.blog;

import com.thereplicant.api.entity.PostStatus;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an existing blog post.
 *
 * All fields are optional — only provided fields are updated (partial update).
 * Validation rules match those of CreatePostRequest where applicable.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(min = 1, max = 100000, message = "Content must be between 1 and 100,000 characters")
    private String content;

    @Size(max = 300, message = "Excerpt must not exceed 300 characters")
    private String excerpt;

    private UUID categoryId;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<@Size(max = 50, message = "Tag name must not exceed 50 characters") String> tags;

    private PostStatus status;
}
