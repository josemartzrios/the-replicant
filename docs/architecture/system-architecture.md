# System Architecture

> High-level architecture for The Replicant blog platform.

---

## Overview

The Replicant follows a modern **API-First** architecture with clear separation between frontend and backend. This enables future expansion to mobile apps or alternative frontends.

---

## Architecture Diagram

```mermaid
flowchart TB
    subgraph CLIENT["🖥️ CLIENT LAYER"]
        Browser["🌐 Browser"]
        Mobile["📱 Mobile App<br/>(Future)"]
    end

    subgraph FRONTEND["⚛️ FRONTEND (Vercel)"]
        NextJS["Next.js 14+<br/>SSR/SSG, TypeScript, Tailwind"]
    end

    subgraph BACKEND["☕ BACKEND (Railway)"]
        SpringBoot["Spring Boot 3.x<br/>REST API, Security, JPA"]
    end

    subgraph DATABASE["🐘 DATABASE (Supabase)"]
        PostgreSQL[("PostgreSQL 15+<br/>Posts, Users, Tags")]
    end

    subgraph EXTERNAL["📧 EXTERNAL SERVICES"]
        Resend["Resend<br/>Email Service"]
    end

    Browser --> NextJS
    Mobile -.->|future| NextJS
    NextJS -->|HTTPS/REST| SpringBoot
    SpringBoot --> PostgreSQL
    SpringBoot --> Resend

    style CLIENT fill:#1e3a5f,stroke:#3b82f6,color:#fff
    style FRONTEND fill:#0d9488,stroke:#14b8a6,color:#fff
    style BACKEND fill:#7c3aed,stroke:#8b5cf6,color:#fff
    style DATABASE fill:#059669,stroke:#10b981,color:#fff
    style EXTERNAL fill:#dc2626,stroke:#ef4444,color:#fff
```

---

## Component Responsibilities

### Frontend (Next.js)
| Responsibility | Implementation |
|----------------|----------------|
| Public pages (SSG) | Homepage, Post detail, Categories |
| Admin dashboard (SSR) | Protected routes with token validation |
| API client | Auto-generated from OpenAPI spec |
| State management | TanStack Query for server state |
| Styling | TailwindCSS with custom theme |

### Backend (Spring Boot)
| Responsibility | Implementation |
|----------------|----------------|
| REST API | Controllers, DTOs, Validation |
| Authentication | JWT tokens, Spring Security |
| Data access | JPA/Hibernate repositories |
| Business logic | Service layer with transactions |
| API documentation | OpenAPI 3.0 (Swagger UI) |

### Database (PostgreSQL)
| Responsibility | Implementation |
|----------------|----------------|
| Data persistence | Primary storage for all entities |
| Full-text search | PostgreSQL tsvector for post search |
| Indexing | Optimized queries for common operations |

---

## Data Flow

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         PUBLIC READ FLOW (SSG)                                │
└──────────────────────────────────────────────────────────────────────────────┘

  User              Next.js            Spring Boot          PostgreSQL
   │                   │                    │                    │
   │  Visit            │                    │                    │
   │  /blog/my-post    │                    │                    │
   │──────────────────>│                    │                    │
   │                   │                    │                    │
   │                   │ GET                │                    │
   │                   │ /api/v1/posts/     │                    │
   │                   │ my-post            │                    │
   │                   │───────────────────>│                    │
   │                   │                    │                    │
   │                   │                    │ SELECT * FROM      │
   │                   │                    │ posts WHERE        │
   │                   │                    │ slug = ?           │
   │                   │                    │───────────────────>│
   │                   │                    │                    │
   │                   │                    │     Post data      │
   │                   │                    │<───────────────────│
   │                   │                    │                    │
   │                   │   JSON response    │                    │
   │                   │<───────────────────│                    │
   │                   │                    │                    │
   │  Rendered HTML    │                    │                    │
   │  (cached)         │                    │                    │
   │<──────────────────│                    │                    │


┌──────────────────────────────────────────────────────────────────────────────┐
│                           ADMIN WRITE FLOW                                    │
└──────────────────────────────────────────────────────────────────────────────┘

  User              Next.js            Spring Boot          PostgreSQL
   │                   │                    │                    │
   │  Submit new post  │                    │                    │
   │──────────────────>│                    │                    │
   │                   │                    │                    │
   │                   │ POST               │                    │
   │                   │ /api/v1/posts      │                    │
   │                   │ (+ JWT)            │                    │
   │                   │───────────────────>│                    │
   │                   │                    │                    │
   │                   │                    │ Validate JWT       │
   │                   │                    │ (internal)         │
   │                   │                    │                    │
   │                   │                    │ INSERT INTO posts  │
   │                   │                    │───────────────────>│
   │                   │                    │                    │
   │                   │                    │   Created post     │
   │                   │                    │<───────────────────│
   │                   │                    │                    │
   │                   │   201 Created      │                    │
   │                   │<───────────────────│                    │
   │                   │                    │                    │
   │  Success          │                    │                    │
   │  notification     │                    │                    │
   │<──────────────────│                    │                    │
```

---

## Technology Decisions

### Why Spring Boot?
- Mature ecosystem for REST APIs
- Excellent security with Spring Security
- Strong typing with Java 21
- OpenAPI integration out of the box

### Why Next.js?
- Best-in-class SSR/SSG for SEO
- React ecosystem and component reuse
- Vercel deployment is free and fast
- TypeScript for type safety across stack

### Why PostgreSQL?
- Robust full-text search (no Elasticsearch needed for MVP)
- Free tier on Supabase
- Reliable and battle-tested
- Easy migration path if scaling needed

---

## Scalability Considerations

| Component | Current (MVP) | Future Scale |
|-----------|---------------|--------------|
| Frontend | Vercel Edge | Same (auto-scales) |
| Backend | Single Railway instance | Horizontal scaling |
| Database | Supabase Free | Supabase Pro or dedicated |
| Caching | PostgreSQL query cache | Add Redis layer |
| CDN | Vercel built-in | Same |

---

**Document Version**: 1.0  
**Created**: 2026-01-19  
**Last Updated**: 2026-01-19
