# System Architecture

> High-level architecture for The Replicant blog platform.

---

## Overview

The Replicant follows a modern **API-First** architecture with clear separation between frontend and backend. This enables future expansion to mobile apps or alternative frontends.

---

## Architecture Diagram

```mermaid
flowchart TB
    subgraph "Client_Layer"
        Browser["🌐 Browser"]
        Mobile["📱 Mobile App (Future)"]
    end

    subgraph "Frontend_Vercel"
        NextJS["⚛️ Next.js 14+<br/>SSR/SSG, TS, Tailwind"]
    end

    subgraph "Backend_Railway"
        SpringBoot["☕ Spring Boot 3.x<br/>REST API, Security, JPA"]
    end

    subgraph "Database_Supabase"
        PostgreSQL[("🐘 PostgreSQL 15+<br/>Posts, Users, Tags")]
    end

    subgraph "External_Services"
        Resend["📧 Resend<br/>Email Service"]
    end

    Browser --> NextJS
    Mobile -.-> SpringBoot
    NextJS --> |"HTTPS/REST"| SpringBoot
    SpringBoot --> PostgreSQL
    SpringBoot --> Resend

    classDef frontend fill:#61dafb,color:#000
    classDef backend fill:#6db33f,color:#fff
    classDef database fill:#336791,color:#fff
    classDef external fill:#9ca3af,color:#000

    class NextJS frontend
    class SpringBoot backend
    class PostgreSQL database
    class Resend external
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

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Next.js
    participant BE as Spring Boot
    participant DB as PostgreSQL

    Note over U,DB: Public Read Flow (SSG)
    U->>FE: Visit /blog/my-post
    FE->>BE: GET /api/v1/posts/my-post
    BE->>DB: SELECT * FROM posts WHERE slug = ?
    DB-->>BE: Post data
    BE-->>FE: JSON response
    FE-->>U: Rendered HTML (cached)

    Note over U,DB: Admin Write Flow
    U->>FE: Submit new post
    FE->>BE: POST /api/v1/posts (+ JWT)
    BE->>BE: Validate JWT
    BE->>DB: INSERT INTO posts
    DB-->>BE: Created post
    BE-->>FE: 201 Created
    FE-->>U: Success notification
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
