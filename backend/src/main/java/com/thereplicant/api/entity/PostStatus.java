package com.thereplicant.api.entity;

/**
 * Publication status for blog posts.
 *
 * @see Post
 */
public enum PostStatus {

    /**
     * Post is a draft and not visible to the public.
     */
    DRAFT,

    /**
     * Post is published and visible to readers.
     */
    PUBLISHED
}
