package com.thereplicant.api.dto.blog;

import com.thereplicant.api.entity.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new blog post.
 *
 * Validation rules per US-006:
 * - Title: required, 1-200 chars
 * - Content: required, 1-100,000 chars
 * - Excerpt: optional, max 300 chars (auto-generated if empty)
 * - Tags: optional, max 10 items, each max 50 chars
 * - Status: defaults to DRAFT
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 100000, message = "Content must be between 1 and 100,000 characters")
    private String content;

    @Size(max = 300, message = "Excerpt must not exceed 300 characters")
    private String excerpt;

    private UUID categoryId;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<@Size(max = 50, message = "Tag name must not exceed 50 characters") String> tags;

    @Builder.Default
    private PostStatus status = PostStatus.DRAFT;
}
