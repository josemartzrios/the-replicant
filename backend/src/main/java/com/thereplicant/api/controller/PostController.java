package com.thereplicant.api.controller;

import com.thereplicant.api.dto.blog.*;
import com.thereplicant.api.entity.PostStatus;
import com.thereplicant.api.entity.User;
import com.thereplicant.api.dto.common.ResponseMeta;
import com.thereplicant.api.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for blog post operations.
 *
 * Public endpoints (no auth):
 * - GET /api/v1/posts → list published posts
 * - GET /api/v1/posts/search → full-text search
 * - GET /api/v1/posts/{slug} → get post by slug
 *
 * Protected endpoints (auth required):
 * - POST /api/v1/posts → create post
 * - PUT /api/v1/posts/{id} → update post
 * - DELETE /api/v1/posts/{id} → soft-delete post (ADMIN only)
 *
 * @see docs/user-stories/epic-002-blog-management.md
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // =========================================================================
    // Public Endpoints
    // =========================================================================

    /**
     * List published posts with optional filters.
     * Publicly accessible — returns only PUBLISHED, non-deleted posts.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Map<String, Object> response = postService.listPublished(pageable, category, tag);
        return ResponseEntity.ok(response);
    }

    /**
     * Full-text search across published posts.
     * Uses LIKE-based search (PostgreSQL TSVECTOR deferred to post-MVP).
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Map<String, Object> response = postService.search(q, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a single post by slug.
     * Publicly accessible — returns only published, non-deleted posts.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> getPostBySlug(@PathVariable String slug) {
        PostDTO post = postService.getBySlug(slug);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", post);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Protected Endpoints (Auth Required)
    // =========================================================================

    /**
     * Create a new blog post.
     * Requires authentication. Any authenticated user can create posts.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new post")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Post created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, Object>> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal User user) {

        PostDTO post = postService.create(request, user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", post);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing post.
     * Requires authentication.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal User user) {

        PostDTO post = postService.update(id, request, user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", post);
        response.put("meta", ResponseMeta.builder().build());

        return ResponseEntity.ok(response);
    }

    /**
     * Soft-delete a post.
     * Requires ADMIN role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete post (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Post deleted"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized (Admin only)"),
            @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public ResponseEntity<Void> deletePost(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        postService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Admin Endpoints
    // =========================================================================

    /**
     * List all posts (admin view — includes drafts).
     * Requires authentication. Supports status filter.
     */
    @GetMapping("/admin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> listAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) PostStatus status) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Map<String, Object> response = postService.listAll(pageable, status);
        return ResponseEntity.ok(response);
    }
}
