# Epic 004: Organization

> **Priority**: P1 (Should Have)  
> **Sprint**: 3  
> **Dependencies**: Epic 002 (Blog Management)

## Overview

Content organization through categories and tags. Helps readers navigate and discover related content.

---

## US-013: Browse by Category (Public)

**As a** reader  
**I want** to browse posts by category  
**So that** I can find content on topics I'm interested in

### Acceptance Criteria

- [ ] Category list visible in navigation or sidebar
- [ ] Click category to see all posts in that category
- [ ] Category page shows: category name, description, post count
- [ ] Posts displayed same as homepage (cards with pagination)
- [ ] SEO-friendly category pages (`/category/{slug}`)

### Technical Notes

- Endpoint: `GET /api/v1/categories`
- Endpoint: `GET /api/v1/posts?categoryId={id}`
- Auth: Not required (public)
- Categories are predefined by admin (no user-generated)

### Data Model

```
Category {
  id: UUID
  name: String (unique, max 50 chars)
  slug: String (unique, URL-friendly)
  description: String (optional, max 200 chars)
  createdAt: DateTime
}
```

---

## US-014: Browse by Tag (Public)

**As a** reader  
**I want** to browse posts by tag  
**So that** I can find related content across categories

### Acceptance Criteria

- [ ] Tags displayed on post cards and detail pages
- [ ] Click tag to see all posts with that tag
- [ ] Tag page shows: tag name, post count
- [ ] Posts displayed same as homepage
- [ ] SEO-friendly tag pages (`/tag/{slug}`)

### Technical Notes

- Endpoint: `GET /api/v1/tags`
- Endpoint: `GET /api/v1/posts?tag={slug}`
- Auth: Not required (public)
- Tags created dynamically when used in posts

### Data Model

```
Tag {
  id: UUID
  name: String (unique, max 50 chars)
  slug: String (unique, URL-friendly)
  createdAt: DateTime
}

PostTag (junction table) {
  postId: UUID (FK)
  tagId: UUID (FK)
}
```

---

## US-015: Manage Categories (Admin)

**As an** admin  
**I want** to create and manage categories  
**So that** I can organize my blog content

### Acceptance Criteria

- [ ] List all categories with post count
- [ ] Create new category (name, description)
- [ ] Edit category (name, description)
- [ ] Delete category (only if no posts assigned)
- [ ] Auto-generate slug from name

### Technical Notes

- Endpoint: `POST /api/v1/categories` (create)
- Endpoint: `PUT /api/v1/categories/{id}` (update)
- Endpoint: `DELETE /api/v1/categories/{id}` (delete)
- Auth: Required (Bearer token)
- Prevent deletion if `postCount > 0`

---

**Epic Status**: ⏳ Pending  
**Created**: 2026-01-18  
**Last Updated**: 2026-01-18  
**Stories**: US-013, US-014, US-015
