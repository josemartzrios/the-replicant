# The Replicant - Development Roadmap

## Overview

This document outlines the complete development roadmap for The Replicant MVP, from initial setup to deployment and iteration.

> **Current Focus**: Step 5 - Infrastructure Setup (PostgreSQL + Docker)  
> **Working Branch**: `feature/005-infrastructure`

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
- [x] Merge to `dev` after review

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

### 4. Create API Contract ✅
**Status**: ✅ Complete  
**Branch**: `feature/004-api-contract`  
**Description**: Define complete API specification using OpenAPI 3.0

**Tasks**:
- [x] Create `backend/api-spec.yaml` OpenAPI specification
- [x] Define authentication endpoints (`/api/v1/auth/*`)
- [x] Define blog posts endpoints (`/api/v1/posts/*`)
- [x] Define categories and tags endpoints
- [x] Add request/response schemas
- [x] Document error responses (RFC 7807)
- [x] Add API security definitions
- [ ] Validate specification with Swagger Editor

---

### 5. Infrastructure Setup 🔄
**Status**: 🔄 In Progress  
**Branch**: `feature/005-infrastructure`  
**Description**: Configure minimal infrastructure required for authentication

**Tasks** (Priority: CRITICAL - Blocker for Auth):
- [x] Initialize Spring Boot backend (Maven, Java 21)
- [x] Add health endpoints with Swagger documentation
- [ ] Create `docker-compose.yml` (PostgreSQL only)
- [ ] Configure `.env.example` + `application.yml` profiles
- [ ] Verify backend → PostgreSQL connection works

> **💡 MVP Decision**: No Redis. PostgreSQL handles caching needs for MVP scale.

---

### 5b. Frontend & CI Setup ⏳
**Status**: ⏳ Pending (Can run parallel to Auth)  
**Branch**: `feature/005b-frontend-setup`  
**Description**: Frontend initialization and CI pipeline (non-blocking)

**Tasks**:
- [ ] Initialize Next.js frontend (TypeScript, TailwindCSS)
- [ ] Setup Checkstyle + ESLint/Prettier
- [ ] Create initial GitHub Actions workflow
- [ ] Configure frontend `.env.example`

> **� Note**: These tasks can be done in parallel with Step 6 (Auth) since they don't block backend development.

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
| 3 | Architecture | ✅ Done | 0 |
| 4 | API Contract | ✅ Done | 0 |
| 5 | Infrastructure Setup | 🔄 In Progress | 1 |
| 5b | Frontend & CI Setup | ⏳ Parallel | 1-2 |
| 6 | Basic Auth | ⏳ Pending | 1 |
| 7 | CRUD Posts | ⏳ Pending | 2 |
| 8 | Basic Frontend | ⏳ Pending | 2 |
| 9 | Deploy | ⏳ Pending | 3 |
| 10 | MVP Iteration | ⏳ Pending | 3 |
| 11 | Narrative Implementation | ⏳ Pending | Post-MVP |

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

> **🎯 Current**: Complete Step 5 - Infrastructure Setup
>
> **Today's Tasks**:
> 1. Create `docker-compose.yml` with PostgreSQL
> 2. Configure `.env.example` + `application.yml` profiles
> 3. Verify backend connects to PostgreSQL
> 4. Begin Step 6 - Authentication implementation

---

**Document Version**: 1.3  
**Created**: 2026-01-13  
**Last Updated**: 2026-01-29  
**Next Review**: After Step 6 completion