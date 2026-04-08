package com.thereplicant.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Blog post entity — central content unit of The Replicant.
 *
 * Design decisions:
 * - Soft delete via {@code deletedAt} (OWASP: data integrity, audit trail)
 * - Slug for SEO-friendly URLs (unique, auto-generated from title)
 * - Excerpt auto-generated if not provided (first 160 chars of content)
 * - Reading time calculated from word count (~200 words/minute)
 * - ManyToMany with Tag via {@code post_tags} junction table
 *
 * @see docs/architecture/database-schema.md
 * @see docs/user-stories/epic-002-blog-management.md
 */
@Entity
@Table(name = "posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 250)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 300)
    private String excerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    /**
     * Post author — every post must have an author.
     * Lazy-fetched to avoid N+1 on list queries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /**
     * Optional category for this post.
     * Null when the post has no category assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Tags associated with this post.
     * Junction table managed by JPA — no explicit PostTag entity needed.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "reading_time_minutes")
    private Integer readingTimeMinutes;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Soft delete marker. When non-null, the post is considered deleted.
     * Excluded from public queries via repository methods.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    // =========================================================================
    // JPA Lifecycle Callbacks
    // =========================================================================

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // =========================================================================
    // Domain Logic
    // =========================================================================

    /**
     * Checks if this post has been soft-deleted.
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Checks if this post is published and visible to the public.
     */
    public boolean isPublished() {
        return status == PostStatus.PUBLISHED && !isDeleted();
    }
}
