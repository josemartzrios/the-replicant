package com.thereplicant.api.repository;

import com.thereplicant.api.entity.Category;
import com.thereplicant.api.entity.Post;
import com.thereplicant.api.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Post entity operations.
 *
 * All public-facing queries exclude soft-deleted posts (deletedAt IS NULL).
 * Search uses LIKE for H2 compatibility; PostgreSQL full-text search (TSVECTOR)
 * can be added post-MVP via native queries.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

        /**
         * Find a non-deleted post by slug (public endpoint).
         */
        Optional<Post> findBySlugAndDeletedAtIsNull(String slug);

        /**
         * Check if a slug already exists (for slug uniqueness during generation).
         */
        boolean existsBySlug(String slug);

        /**
         * List published, non-deleted posts with pagination (public listing).
         */
        Page<Post> findByStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                        PostStatus status, Pageable pageable);

        /**
         * List non-deleted posts of any status with pagination (admin listing).
         */
        Page<Post> findByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

        /**
         * List non-deleted posts filtered by status (admin listing with filter).
         */
        Page<Post> findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        PostStatus status, Pageable pageable);

        /**
         * List published, non-deleted posts filtered by category (public).
         */
        Page<Post> findByCategoryAndStatusAndDeletedAtIsNullOrderByPublishedAtDesc(
                        Category category, PostStatus status, Pageable pageable);

        /**
         * List published, non-deleted posts filtered by tag slug (public).
         */
        @Query("SELECT p FROM Post p JOIN p.tags t WHERE t.slug = :tagSlug " +
                        "AND p.status = :status AND p.deletedAt IS NULL ORDER BY p.publishedAt DESC")
        Page<Post> findByTagSlugAndStatus(
                        @Param("tagSlug") String tagSlug,
                        @Param("status") PostStatus status,
                        Pageable pageable);

        /**
         * Full-text search across title, excerpt, and content (LIKE fallback).
         * Uses case-insensitive LIKE for H2 compatibility.
         * PostgreSQL TSVECTOR native query can replace this post-MVP.
         */
        @Query("SELECT p FROM Post p WHERE p.status = 'PUBLISHED' AND p.deletedAt IS NULL " +
                        "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(p.excerpt) LIKE LOWER(CONCAT('%', :query, '%')) " +
                        "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
                        "ORDER BY p.publishedAt DESC")
        Page<Post> searchByKeyword(@Param("query") String query, Pageable pageable);

        /**
         * Count published, non-deleted posts in a category.
         */
        long countByCategoryAndStatusAndDeletedAtIsNull(Category category, PostStatus status);

        /**
         * Count all non-deleted posts in a category (any status, for admin views).
         */
        long countByCategoryAndDeletedAtIsNull(Category category);

        /**
         * Count published, non-deleted posts with a specific tag.
         */
        @Query("SELECT COUNT(p) FROM Post p JOIN p.tags t WHERE t.id = :tagId " +
                        "AND p.status = 'PUBLISHED' AND p.deletedAt IS NULL")
        long countByTagIdAndPublished(@Param("tagId") UUID tagId);
}
