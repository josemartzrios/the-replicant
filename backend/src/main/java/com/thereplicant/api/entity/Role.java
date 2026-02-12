package com.thereplicant.api.entity;

/**
 * User roles for authorization (RBAC).
 * 
 * Role hierarchy:
 * - ADMIN: Full system access, can manage users and all content
 * - AUTHOR: Can create and edit own posts
 * 
 * @see docs/architecture/security-architecture.md
 */
public enum Role {

    /**
     * Administrator role with full system access.
     * Permissions: manage users, manage all posts, manage categories/tags.
     */
    ADMIN,

    /**
     * Author role for content creation.
     * Permissions: create posts, edit own posts, read categories.
     */
    AUTHOR
}
