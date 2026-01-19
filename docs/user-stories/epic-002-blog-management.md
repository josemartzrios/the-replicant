# Epic 002: Blog Management

> **Priority**: P0 (Must Have)  
> **Sprint**: 2  
> **Dependencies**: Epic 001 (Authentication)

## Overview

Complete CRUD operations for blog posts. Admin can create, edit, publish, and delete posts with markdown support.

---

## US-006: Create Blog Post (Admin)

**As an** admin  
**I want** to create a new blog post with markdown content  
**So that** I can publish articles on my blog

### Acceptance Criteria

- [ ] Form includes: title, content (markdown), excerpt, category, tags
- [ ] Title is required, max 200 characters
- [ ] Content supports full markdown syntax
- [ ] Auto-generate slug from title (URL-friendly)
- [ ] Preview markdown before publishing
- [ ] Save as draft or publish immediately
- [ ] Success notification after creation

### Technical Notes

- Endpoint: `POST /api/v1/posts`
- Auth: Required (Bearer token)
- Request body:
```json
{
  "title": "string (required)",
  "content": "string (markdown, required)",
  "excerpt": "string (optional, auto-generated if empty)",
  "categoryId": "uuid (optional)",
  "tags": ["string"] (optional),
  "status": "DRAFT | PUBLISHED"
}
```
- Slug generation: lowercase, replace spaces with hyphens, remove special chars
- Excerpt: First 160 chars of content if not provided

### Validation Rules

- [ ] Title: 1-200 characters
- [ ] Content: 1-100,000 characters
- [ ] Excerpt: 0-300 characters
- [ ] Tags: max 10 tags, each max 50 characters
- [ ] Sanitize HTML in markdown to prevent XSS

---

## US-007: Edit Blog Post (Admin)

**As an** admin  
**I want** to edit an existing blog post  
**So that** I can fix errors or update content

### Acceptance Criteria

- [ ] Load existing post data into edit form
- [ ] All fields editable (title, content, excerpt, category, tags)
- [ ] Option to update slug (with warning about SEO impact)
- [ ] Track last modified date
- [ ] Preview changes before saving
- [ ] Publish/unpublish toggle
- [ ] Success notification after save

### Technical Notes

- Endpoint: `PUT /api/v1/posts/{id}`
- Auth: Required (Bearer token)
- Response includes updated post with new `updatedAt` timestamp
- Maintain `createdAt` unchanged

---

## US-008: Delete Blog Post (Admin)

**As an** admin  
**I want** to delete a blog post  
**So that** I can remove content I no longer want published

### Acceptance Criteria

- [ ] Confirmation dialog before deletion
- [ ] Soft delete (mark as deleted, don't remove from DB)
- [ ] Post no longer visible on public site
- [ ] Success notification after deletion
- [ ] Option to restore within 30 days (future feature)

### Technical Notes

- Endpoint: `DELETE /api/v1/posts/{id}`
- Auth: Required (Bearer token)
- Soft delete: Set `deletedAt = NOW()`
- Exclude soft-deleted posts from public queries

---

## US-009: List All Posts (Admin)

**As an** admin  
**I want** to see a list of all my posts  
**So that** I can manage my blog content

### Acceptance Criteria

- [ ] Table view with: title, status, category, created date, actions
- [ ] Filter by status (draft, published, deleted)
- [ ] Sort by date (newest first by default)
- [ ] Pagination (10 posts per page)
- [ ] Quick actions: edit, delete, view
- [ ] Search by title

### Technical Notes

- Endpoint: `GET /api/v1/posts?status=all&page=0&size=10`
- Auth: Required (Bearer token)
- Include drafts and soft-deleted posts for admin view
- Response includes pagination metadata

---

**Epic Status**: ⏳ Pending  
**Created**: 2026-01-18  
**Last Updated**: 2026-01-18  
**Stories**: US-006, US-007, US-008, US-009
