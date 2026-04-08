# The Replicant - Development Roadmap

## Overview

This document outlines the complete development roadmap for The Replicant MVP, from initial setup to deployment and iteration.

> **Current Focus**: Step 9 - Deploy  
> **Last Completed**: Step 11 - Narrative Implementation (2026-03-09)

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

### 5. Infrastructure Setup ✅
**Status**: ✅ Completed  
**Completed**: 2026-02-05  
**Branch**: `feature/005-infrastructure`  
**Description**: Configure minimal infrastructure required for authentication

**Tasks**:
- [x] Initialize Spring Boot backend (Maven, Java 21)
- [x] Add health endpoints with Swagger documentation
- [x] Create `docker-compose.yml` (PostgreSQL only)
- [x] Configure `.env.example` + `application.yml` profiles
- [x] Verify backend → PostgreSQL connection works

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

### 6. Basic Auth ✅
**Status**: ✅ Completed  
**Completed**: 2026-02-13  
**Branch**: `feature/006-authentication`  
**Description**: Implement JWT-based authentication system

**Tasks**:
- [x] Setup Spring Security configuration (`SecurityConfig.java`)
- [x] Implement JWT token generation/validation (`JwtService.java`)
- [x] Add JWT authentication filter (`JwtAuthenticationFilter.java`)
- [x] Add password encryption (BCrypt - strength 12)
- [x] Create dev testing endpoint (`DevTokenController.java` - dev profile only)
- [x] Write Unit Tests for JwtService (8 test cases - ✅ all passing)
- [x] Create User entity, Role enum, and UserRepository
- [x] Implement `/api/v1/auth/setup` endpoint (first admin)
- [x] Implement `/api/v1/auth/login` endpoint
- [x] Implement `/api/v1/auth/refresh` endpoint (token rotation)
- [x] Implement `/api/v1/auth/logout` endpoint (revoke tokens)
- [x] Create RefreshToken entity and repository
- [x] Create auth DTOs and validators (SetupRequest, LoginRequest, etc.)
- [x] Write AuthService unit tests (18 test cases)
- [x] Write AuthController integration tests (15 test cases)
- [x] Security audit remediation (OWASP headers, CORS, password policy, custom exceptions)

---

### 7. CRUD Posts ✅
**Status**: ✅ Completed  
**Completed**: 2026-03-02  
**Branch**: `feature/007-blog-crud`  
**Description**: Implement complete blog post management system (US-006 to US-009)

**Tasks**:
- [x] Create Post, Category, Tag entities with JPA
- [x] Create PostTag junction table (ManyToMany)
- [x] Create repository interfaces (PostRepository, CategoryRepository, TagRepository)
- [x] Implement PostService with CRUD operations
- [x] Create PostController with REST endpoints (PATCH for partial update)
- [x] Create blog DTOs (CreatePostRequest, UpdatePostRequest, PostResponse, etc.)
- [x] Add input validation (Jakarta Validation + strict Jackson coercion)
- [x] Implement slug generation (auto from title)
- [x] Implement excerpt auto-generation (first 160 chars)
- [x] Implement reading time calculation
- [x] Add soft delete support (deletedAt)
- [x] Add pagination support
- [x] Implement CategoryController and TagController
- [x] Configure SecurityConfig with method-specific public endpoints (GET only)
- [x] Add exception handlers (404, 409, 405, malformed request)
- [x] Write unit tests (67 tests passing)
- [x] Smoke tests with Postman (full CRUD flow validated)

---

### 8. Basic Frontend ✅
**Status**: ✅ Completed  
**Branch**: `feature/008-frontend-mvp`  
**Description**: Implement core frontend functionality

**Tasks**:
- [x] Generate API client from OpenAPI spec
- [x] Implement login page (admin only)
- [x] Create homepage with post listing (SSG)
- [x] Implement post detail page (SSG + ISR)
- [x] Create admin dashboard for CRUD
- [x] Add markdown renderer (react-markdown)
- [x] Implement responsive design (mobile-first)
- [x] Add loading states and error handling
- [x] Implement SEO meta tags

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
- [ ] Investigate and fix React Hydration TypeError (removeChild) caused by browser extensions in Admin layout during dev mode.
- [ ] Optimize Core Web Vitals
- [ ] Update documentation
- [ ] Tag release v1.0.0
- [ ] Plan v1.1 features

---

### 11. Narrative Implementation ✅
**Status**: ✅ Completed  
**Description**: Integrar el sistema de lore y ARG según el Epic 005.

**Tasks**:
- [x] Implement Narrative Phase Manager
- [x] Add hidden metadata and comments
- [x] Create custom narrative 404
- [x] Implement console easter eggs
- [x] Deploy Fase 1 seeds

---


## Progress Tracker

| Step | Name | Status | Sprint |
|------|------|--------|--------|
| 1 | Create Branches | ✅ Done | 0 |
| 2 | User Stories | ✅ Done | 0 |
| 3 | Architecture | ✅ Done | 0 |
| 4 | API Contract | ✅ Done | 0 |
| 5 | Infrastructure Setup | ✅ Done | 1 |
| 5b | Frontend & CI Setup | ⏳ Parallel | 1-2 |
| 6 | Basic Auth | ✅ Done | 1 |
| 7 | CRUD Posts | 🔄 In Progress | 2 |
| 8 | Basic Frontend | ✅ Done | 2 |
| 9 | Deploy | ⏳ Pending | 3 |
| 10 | MVP Iteration | ⏳ Pending | 3 |
| 11 | Narrative Implementation | ✅ Done | Post-MVP |

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

> **🎯 Current**: Step 9 - Deploy
>
> **Next Tasks**:
> 1. Configure production environments (Railway/Supabase for Backend, Vercel for Frontend)
> 2. Document environment variables for CI/CD
> 3. Implement basic health checks
> 4. Run automated deployments
> 5. Create PR from `dev` to `qa` for the test environment.

---

**Document Version**: 1.4  
**Created**: 2026-01-13  
**Last Updated**: 2026-02-27  
**Next Review**: After Step 7 completion