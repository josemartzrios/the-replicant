# Epic 003: Reading Experience

> **Priority**: P0 (Must Have)  
> **Sprint**: 2  
> **Dependencies**: Epic 002 (Blog Management)

## Overview

Public-facing reading experience. Visitors can browse and read blog posts with a clean, responsive interface.

---

## US-010: View Post List (Public)

**As a** reader  
**I want** to see a list of published blog posts  
**So that** I can discover content to read

### Acceptance Criteria

- [ ] Homepage displays list of published posts
- [ ] Each post card shows: title, excerpt, date, category, reading time
- [ ] Posts ordered by publish date (newest first)
- [ ] infinite scroll (10 posts per load)
- [ ] Responsive design (mobile, tablet, desktop)
- [ ] Fast loading (< 2s initial load)
- [ ] SEO-friendly (SSG or SSR)

### Technical Notes

- Endpoint: `GET /api/v1/posts?status=PUBLISHED&page=0&size=10`
- Auth: Not required (public)
- Use Next.js SSG with ISR (revalidate: 60 seconds)
- Calculate reading time: ~200 words per minute

### Performance Requirements

- [ ] Lighthouse Performance score > 90
- [ ] First Contentful Paint < 1.5s
- [ ] Cumulative Layout Shift < 0.1

---

## US-011: Read Single Post (Public)

**As a** reader  
**I want** to read a full blog post  
**So that** I can consume the content

### Acceptance Criteria

- [ ] Clean, distraction-free reading layout
- [ ] Full markdown rendering (headers, lists, code blocks, images, links)
- [ ] Syntax highlighting for code blocks
- [ ] Post metadata: title, author, date, category, tags, reading time
- [ ] **Back to posts** button/link clearly visible (header or breadcrumb)
- [ ] Responsive typography (comfortable on all devices)
- [ ] Share buttons (copy link, future: social)
- [ ] Related posts suggestion (future)

### Technical Notes

- Endpoint: `GET /api/v1/posts/{slug}`
- Auth: Not required (public)
- Use Next.js SSG with ISR for each post
- Markdown renderer: react-markdown + rehype plugins
- Code highlighting: rehype-highlight or prism

### SEO Requirements

- [ ] Unique title tag: `{Post Title} | The Replicant`
- [ ] Meta description from excerpt
- [ ] Open Graph tags for social sharing
- [ ] Canonical URL
- [ ] JSON-LD structured data (Article schema)

---

## US-012: Search Posts (Public)

**As a** reader  
**I want** to search for blog posts by keyword  
**So that** I can find specific content

### Acceptance Criteria

- [ ] Search input in header/navigation
- [ ] Search by title and content
- [ ] Display results with highlighted matches
- [ ] Debounced search (300ms delay)
- [ ] Minimum 2 characters to trigger search

### Empty State (No Results)

- [ ] Friendly message: "No posts found for '{query}'"
- [ ] Suggestions: "Try different keywords or browse by category"
- [ ] Show links to categories/tags as alternatives
- [ ] Clear search button to reset

### Technical Notes

- Endpoint: `GET /api/v1/posts/search?q={query}`
- Auth: Not required (public)
- PostgreSQL full-text search (tsvector/tsquery)
- Return: matching posts with relevance score
- Limit: 20 results

### MVP Scope

For MVP, use PostgreSQL full-text search. Consider Elasticsearch/Algolia post-MVP if needed.

---

**Epic Status**: ⏳ Pending  
**Created**: 2026-01-18  
**Last Updated**: 2026-01-18  
**Stories**: US-010, US-011, US-012
