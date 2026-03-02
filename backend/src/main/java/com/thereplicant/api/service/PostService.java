package com.thereplicant.api.service;

import com.thereplicant.api.dto.blog.*;
import com.thereplicant.api.dto.common.PaginationMeta;
import com.thereplicant.api.entity.*;
import com.thereplicant.api.exception.ResourceNotFoundException;
import com.thereplicant.api.repository.CategoryRepository;
import com.thereplicant.api.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for blog post management.
 *
 * Handles CRUD operations for posts including:
 * - Slug generation (unique, auto-generated from title)
 * - Excerpt auto-generation (first 160 chars if not provided)
 * - Reading time calculation (~200 words per minute)
 * - Tag find-or-create pattern
 * - Soft delete
 * - Pagination and filtering
 *
 * @see docs/user-stories/epic-002-blog-management.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int EXCERPT_MAX_LENGTH = 160;
    private static final int WORDS_PER_MINUTE = 200;

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final TagService tagService;
    private final SlugService slugService;

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Create a new blog post.
     *
     * @param request the create post request
     * @param author  the authenticated user creating the post
     * @return the created post DTO
     */
    @Transactional
    public PostDTO create(CreatePostRequest request, User author) {
        String slug = generateUniqueSlug(request.getTitle());
        String excerpt = resolveExcerpt(request.getExcerpt(), request.getContent());
        int readingTime = calculateReadingTime(request.getContent());
        Category category = resolveCategory(request.getCategoryId());
        Set<Tag> tags = tagService.findOrCreateTags(request.getTags());

        Post post = Post.builder()
                .title(request.getTitle().trim())
                .slug(slug)
                .content(request.getContent())
                .excerpt(excerpt)
                .status(request.getStatus())
                .author(author)
                .category(category)
                .tags(tags)
                .readingTimeMinutes(readingTime)
                .build();

        // Set publishedAt if publishing immediately
        if (request.getStatus() == PostStatus.PUBLISHED) {
            post.setPublishedAt(Instant.now());
        }

        Post saved = postRepository.save(post);
        log.info("Post created: id={}, slug={}, status={}", saved.getId(), saved.getSlug(), saved.getStatus());

        return toDTO(saved);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Update an existing blog post (partial update).
     *
     * @param id      the post ID
     * @param request the update request (only non-null fields are applied)
     * @param user    the authenticated user performing the update
     * @return the updated post DTO
     * @throws ResourceNotFoundException if the post does not exist or is deleted
     */
    @Transactional
    public PostDTO update(UUID id, UpdatePostRequest request, User user) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", id));

        // Apply partial updates — only non-null fields
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle().trim());
            post.setSlug(generateUniqueSlug(request.getTitle(), post.getSlug()));
        }

        if (request.getContent() != null) {
            post.setContent(request.getContent());
            post.setReadingTimeMinutes(calculateReadingTime(request.getContent()));
        }

        if (request.getExcerpt() != null) {
            post.setExcerpt(request.getExcerpt().trim());
        } else if (request.getContent() != null && (post.getExcerpt() == null || post.getExcerpt().isBlank())) {
            post.setExcerpt(resolveExcerpt(null, request.getContent()));
        }

        if (request.getCategoryId() != null) {
            post.setCategory(resolveCategory(request.getCategoryId()));
        }

        if (request.getTags() != null) {
            post.setTags(tagService.findOrCreateTags(request.getTags()));
        }

        if (request.getStatus() != null) {
            // Set publishedAt on first publish
            if (request.getStatus() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
                post.setPublishedAt(Instant.now());
            }
            post.setStatus(request.getStatus());
        }

        Post saved = postRepository.save(post);
        log.info("Post updated: id={}, slug={}", saved.getId(), saved.getSlug());

        return toDTO(saved);
    }

    // =========================================================================
    // Delete (Soft)
    // =========================================================================

    /**
     * Soft-delete a post by setting deletedAt timestamp.
     *
     * @param id   the post ID
     * @param user the authenticated user performing the deletion
     * @throws ResourceNotFoundException if the post does not exist or is already
     *                                   deleted
     */
    @Transactional
    public void delete(UUID id, User user) {
        Post post = postRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", id));

        post.setDeletedAt(Instant.now());
        postRepository.save(post);
        log.info("Post soft-deleted: id={}, slug={}, by={}", id, post.getSlug(), user.getEmail());
    }

    // =========================================================================
    // Read (Public)
    // =========================================================================

    /**
     * Get a single published post by slug (public endpoint).
     *
     * @throws ResourceNotFoundException if the post is not found, not published, or
     *                                   deleted
     */
    @Transactional(readOnly = true)
    public PostDTO getBySlug(String slug) {
        Post post = postRepository.findBySlugAndDeletedAtIsNull(slug)
                .filter(Post::isPublished)
                .orElseThrow(() -> new ResourceNotFoundException("Post with slug '" + slug + "' not found"));

        return toDTO(post);
    }

    /**
     * List published posts with optional category/tag filters (public endpoint).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> listPublished(Pageable pageable, String categorySlug, String tagSlug) {
        Page<Post> page;

        if (categorySlug != null && !categorySlug.isBlank()) {
            Category category = categoryRepository.findBySlug(categorySlug)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category with slug '" + categorySlug + "' not found"));
            page = postRepository.findByCategoryAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                    category, PostStatus.PUBLISHED, pageable);
        } else if (tagSlug != null && !tagSlug.isBlank()) {
            page = postRepository.findByTagSlugAndStatus(tagSlug, PostStatus.PUBLISHED, pageable);
        } else {
            page = postRepository.findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                    PostStatus.PUBLISHED, pageable);
        }

        return buildPageResponse(page);
    }

    /**
     * List all non-deleted posts with optional status filter (admin endpoint).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> listAll(Pageable pageable, PostStatus status) {
        Page<Post> page;

        if (status != null) {
            page = postRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status, pageable);
        } else {
            page = postRepository.findByDeletedAtIsNullOrderByCreatedAtDesc(pageable);
        }

        return buildPageResponse(page);
    }

    /**
     * Search published posts by keyword (public endpoint, LIKE fallback).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> search(String query, Pageable pageable) {
        Page<Post> page = postRepository.searchByKeyword(query.trim(), pageable);
        return buildPageResponse(page);
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Generate a unique slug from the given title.
     * If the slug already exists, append numeric suffix (-2, -3, etc.).
     */
    private String generateUniqueSlug(String title) {
        String baseSlug = slugService.generateSlug(title);
        String slug = baseSlug;
        int suffix = 2;

        while (postRepository.existsBySlug(slug)) {
            slug = slugService.appendSuffix(baseSlug, suffix++);
        }

        return slug;
    }

    /**
     * Generate a unique slug, keeping the current slug if the title hasn't changed.
     */
    private String generateUniqueSlug(String newTitle, String currentSlug) {
        String newSlug = slugService.generateSlug(newTitle);

        // If the slug hasn't changed, keep the current one
        if (newSlug.equals(currentSlug)) {
            return currentSlug;
        }

        // Otherwise generate a unique one
        String slug = newSlug;
        int suffix = 2;

        while (postRepository.existsBySlug(slug)) {
            slug = slugService.appendSuffix(newSlug, suffix++);
        }

        return slug;
    }

    /**
     * Resolve excerpt: use provided value or auto-generate from content.
     */
    private String resolveExcerpt(String excerpt, String content) {
        if (excerpt != null && !excerpt.isBlank()) {
            return excerpt.trim();
        }

        if (content == null || content.isBlank()) {
            return null;
        }

        // Strip markdown formatting for clean excerpt
        String plain = content
                .replaceAll("#+ ", "") // Headers
                .replaceAll("\\*+", "") // Bold/italic
                .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1") // Links → text only
                .replaceAll("`+", "") // Code
                .replaceAll("\\n+", " ") // Newlines → spaces
                .trim();

        if (plain.length() <= EXCERPT_MAX_LENGTH) {
            return plain;
        }

        // Truncate at word boundary
        String truncated = plain.substring(0, EXCERPT_MAX_LENGTH);
        int lastSpace = truncated.lastIndexOf(' ');
        if (lastSpace > EXCERPT_MAX_LENGTH / 2) {
            truncated = truncated.substring(0, lastSpace);
        }

        return truncated + "...";
    }

    /**
     * Calculate reading time in minutes based on word count (~200 wpm).
     */
    private int calculateReadingTime(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }

        long wordCount = Arrays.stream(content.trim().split("\\s+")).count();
        return (int) Math.max(1, Math.ceil((double) wordCount / WORDS_PER_MINUTE));
    }

    /**
     * Resolve a category by ID (nullable — posts can be uncategorized).
     */
    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    /**
     * Build a paginated response map with data and meta.
     */
    private Map<String, Object> buildPageResponse(Page<Post> page) {
        List<PostDTO> data = page.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        PaginationMeta meta = PaginationMeta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("meta", meta);
        return response;
    }

    // =========================================================================
    // Mapping
    // =========================================================================

    private PostDTO toDTO(Post post) {
        AuthorDTO authorDTO = AuthorDTO.builder()
                .id(post.getAuthor().getId())
                .name(post.getAuthor().getName())
                .build();

        CategoryDTO categoryDTO = null;
        if (post.getCategory() != null) {
            categoryDTO = CategoryDTO.builder()
                    .id(post.getCategory().getId())
                    .name(post.getCategory().getName())
                    .slug(post.getCategory().getSlug())
                    .description(post.getCategory().getDescription())
                    .build();
        }

        List<TagDTO> tagDTOs = post.getTags().stream()
                .map(tag -> TagDTO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .slug(tag.getSlug())
                        .build())
                .collect(Collectors.toList());

        return PostDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .excerpt(post.getExcerpt())
                .status(post.getStatus())
                .category(categoryDTO)
                .tags(tagDTOs)
                .author(authorDTO)
                .readingTimeMinutes(post.getReadingTimeMinutes())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
