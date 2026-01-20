# The Replicant - Development Roadmap

## Overview

This document outlines the complete development roadmap for The Replicant MVP, from initial setup to deployment and iteration.

> **Current Focus**: Step 3 - Generate Architecture Diagram  
> **Working Branch**: `feature/002-user-stories` (pending merge to `dev`)

---

## Development Steps

### 1. Create Branches ✅
**Status**: ✅ Completed  
**Completed**: 2026-01-18  
**Description**: Set up the branching structure according to our branching strategy

**Tasks**:
- [x] Create `dev` branch from `main`
- [x] Create `qa` branch from `main`
- [x] Verify branch structure follows `docs/branching-strategy.md`
- [x] Update README.md with MVP scope and standards

---

### 2. Generate User Stories ✅
**Status**: ✅ Completed  
**Completed**: 2026-01-18  
**Branch**: `feature/002-user-stories`  
**Description**: Document all MVP user stories with acceptance criteria

**Tasks**:
- [x] Create `docs/user-stories/` directory structure
- [x] Document Epic 001: Authentication (US-001, US-002a, US-002 to US-005)
- [x] Document Epic 002: Blog Management (US-006 to US-009)
- [x] Document Epic 003: Reading Experience (US-010 to US-012)
- [x] Document Epic 004: Organization (US-013 to US-015)
- [x] Create MVP backlog with priorities (16 stories, 67 story points)
- [x] Review and validate all user stories
- [ ] Merge to `dev` after review

**Deliverables**:
- 16 user stories across 4 epics
- Backlog with sprint allocation
- Security-focused acceptance criteria

---

### 3. Generate Architecture Diagram ✅
**Status**: ✅ Completed  
**Completed**: 2026-01-19  
**Branch**: `feature/003-architecture` (create from `dev`)  
**Description**: Create comprehensive architecture documentation

**Tasks**:
- [x] Design system architecture diagram (Mermaid/PlantUML)
- [x] Create database schema diagram (ERD)
- [x] Document API architecture
- [x] Design security architecture (JWT flow)
- [x] Create deployment architecture diagram
- [x] Document technology decisions (ADR format)

**Deliverables**:
- `docs/architecture/system-architecture.md`
- `docs/architecture/database-schema.md`
- `docs/architecture/api-architecture.md`
- `docs/architecture/security-architecture.md`
- `docs/architecture/deployment-architecture.md`

---

### 4. Create API Contract ⏳
**Status**: ⏳ Pending  
**Branch**: `feature/004-api-contract` (create from `dev`)  
**Description**: Define complete API specification using OpenAPI 3.0

**Tasks**:
- [ ] Create `backend/api-spec.yaml` OpenAPI specification
- [ ] Define authentication endpoints (`/api/v1/auth/*`)
- [ ] Define blog posts endpoints (`/api/v1/posts/*`)
- [ ] Define categories and tags endpoints
- [ ] Add request/response schemas
- [ ] Document error responses (RFC 7807)
- [ ] Add API security definitions
- [ ] Validate specification with Swagger Editor

---

### 5. Project Setup ⏳
**Status**: ⏳ Pending  
**Branch**: `feature/005-project-setup` (create from `dev`)  
**Description**: Initialize project structure and development environment

**Tasks**:
- [ ] Initialize Spring Boot backend (Maven, Java 21)
- [ ] Initialize Next.js frontend (TypeScript, TailwindCSS)
- [ ] Create `docker-compose.yml` (PostgreSQL only - no Redis for MVP)
- [ ] Configure `.env.example` files
- [ ] Setup Checkstyle + ESLint/Prettier
- [ ] Create initial GitHub Actions workflow
- [ ] Verify local development environment works

> **💡 MVP Decision**: No Redis. PostgreSQL handles caching needs for MVP scale.

---

### 6. Basic Auth ⏳
**Status**: ⏳ Pending  
**Branch**: `feature/006-authentication` (create from `dev`)  
**Description**: Implement JWT-based authentication system

**Tasks**:
- [ ] Setup Spring Security configuration
- [ ] Create User entity and repository
- [ ] Implement JWT token generation/validation
- [ ] Implement `/api/v1/auth/register` endpoint
- [ ] Implement `/api/v1/auth/login` endpoint
- [ ] Implement `/api/v1/auth/refresh` endpoint
- [ ] Add password encryption (BCrypt)
- [ ] Create user DTOs and validators
- [ ] Write unit + integration tests (≥80% coverage)

---

### 7. CRUD Posts ⏳
**Status**: ⏳ Pending  
**Branch**: `feature/007-blog-crud` (create from `dev`)  
**Description**: Implement complete blog post management system

**Tasks**:
- [ ] Create Post, Category, Tag entities with JPA
- [ ] Create repository interfaces
- [ ] Implement PostService with CRUD operations
- [ ] Create PostController with REST endpoints
- [ ] Add input validation (Jakarta Validation)
- [ ] Implement slug generation
- [ ] Add pagination support
- [ ] Write integration tests for CRUD operations

---

### 8. Basic Frontend ⏳
**Status**: ⏳ Pending  
**Branch**: `feature/008-frontend-mvp` (create from `dev`)  
**Description**: Implement core frontend functionality

**Tasks**:
- [ ] Generate API client from OpenAPI spec
- [ ] Implement login page (admin only)
- [ ] Create homepage with post listing (SSG)
- [ ] Implement post detail page (SSG + ISR)
- [ ] Create admin dashboard for CRUD
- [ ] Add markdown renderer (react-markdown)
- [ ] Implement responsive design (mobile-first)
- [ ] Add loading states and error handling
- [ ] Implement SEO meta tags

---

### 9. Deploy ⏳
**Status**: ⏳ Pending  
**Branch**: `feature/009-deployment` (create from `dev`)  
**Description**: Deploy application to production environments

**Tasks**:
- [ ] Configure Railway for backend deployment
- [ ] Configure Vercel for frontend deployment
- [ ] Setup Supabase PostgreSQL (free tier)
- [ ] Configure production environment variables
- [ ] Complete CI/CD pipeline (GitHub Actions)
- [ ] Configure custom domain (optional)
- [ ] Implement health check endpoint
- [ ] Test production deployment
- [ ] Merge to `qa` → validate → Merge to `main`

---

### 10. MVP Iteration ⏳
**Status**: ⏳ Pending  
**Description**: Review, refine, and iterate on MVP based on feedback

**Tasks**:
- [ ] Self-test complete user flows
- [ ] Fix critical bugs
- [ ] Optimize Core Web Vitals
- [ ] Update documentation
- [ ] Tag release v1.0.0
- [ ] Plan v1.1 features

---

### 11. Narrative Implementation ⏳
**Status**: ⏳ Pending  
**Description**: Integrar el sistema de lore y ARG según el Epic 005.

**Tasks**:
- [ ] Implement Narrative Phase Manager
- [ ] Add hidden metadata and comments
- [ ] Create custom narrative 404
- [ ] Implement console easter eggs
- [ ] Deploy Fase 1 seeds

---


## Progress Tracker

| Step | Name | Status | Sprint |
|------|------|--------|--------|
| 1 | Create Branches | ✅ Done | 0 |
| 2 | User Stories | ✅ Done | 0 |
| 3 | Architecture | 🔄 Next | 0 |
| 4 | API Contract | ⏳ Pending | 0 |
| 5 | Project Setup | ⏳ Pending | 1 |
| 6 | Basic Auth | ⏳ Pending | 1 |
| 7 | CRUD Posts | ⏳ Pending | 2 |
| 8 | Basic Frontend | ⏳ Pending | 2 |
| 9 | Deploy | ⏳ Pending | 3 |
| 10 | MVP Iteration | ⏳ Pending | 3 |

---

## Git Workflow Reminder

```bash
# Starting a new step (example: Step 2)
git checkout dev
git pull origin dev
git checkout -b feature/002-user-stories

# After completing work
git add .
git commit -m "docs: add user stories for MVP"
git push origin feature/002-user-stories

# Create PR to dev → Review → Merge
```

See [branching-strategy.md](branching-strategy.md) for full workflow details.

---

## Sprint Planning

| Sprint | Steps | Focus | Duration |
|--------|-------|-------|----------|
| 0 | 1-4 | Planning & Design | Week 1 |
| 1 | 5-6 | Setup & Auth | Week 2 |
| 2 | 7-8 | Core Features | Week 3 |
| 3 | 9-10 | Deploy & Polish | Week 4 |

**Estimated Total**: 4 weeks  
**Buffer**: +20% for unexpected issues

---

## Next Action

> **🎯 Current**: Commit and merge Step 2 (User Stories), then start Step 3 (Architecture).
>
> ```bash
> # 1. Commit Step 2 changes
> git add .
> git commit -m "docs: add MVP user stories and backlog (16 stories, 4 epics)"
> git push origin feature/002-user-stories
>
> # 2. Create PR: feature/002-user-stories → dev
> # 3. After merge, start Step 3
> git checkout dev
> git pull origin dev
> git checkout -b feature/003-architecture
> ```

---

**Document Version**: 1.2  
**Created**: 2026-01-13  
**Last Updated**: 2026-01-18  
**Next Review**: After Step 3 completion
**Next Review**: After Step 2 completion