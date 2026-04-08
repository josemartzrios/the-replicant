# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**The Replicant** is a full-stack blog platform with a Spring Boot REST API backend and a Next.js frontend. It is designed as a personal blog CMS with a single admin user, JWT-based authentication, and markdown-rendered posts.

## Development Commands

### Infrastructure (Docker)
```bash
# Start PostgreSQL (required for backend dev)
docker-compose up -d

# Stop and remove containers
docker-compose down

# Wipe data volumes
docker-compose down -v
```

### Backend (Spring Boot / Maven)
```bash
cd backend

# Run in dev mode (requires Docker Postgres running)
./mvnw spring-boot:run

# Run all tests (uses H2 in-memory, no Postgres needed)
./mvnw test

# Run a single test class
./mvnw test -Dtest=PostServiceTest

# Run a single test method
./mvnw test -Dtest=PostServiceTest#methodName

# Build JAR
./mvnw package -DskipTests
```

### Frontend (Next.js)
```bash
cd frontend

# Install dependencies
npm install

# Run dev server (http://localhost:3000)
npm run dev

# Build for production
npm run build

# Lint
npm run lint
```

## Architecture

### Monorepo Structure
- `backend/` — Spring Boot 3.2.2 REST API (Java 21, Maven)
- `frontend/` — Next.js 16 app (React 19, TypeScript 5, TailwindCSS 4)
- `docker-compose.yml` — PostgreSQL 15 for local development only
- `docs/` — Architecture docs, roadmap, lessons learned

The backend and frontend are deployed independently (Railway + Vercel + Supabase in production).

### Backend Architecture

**Package:** `com.thereplicant.api`

Follows a standard layered architecture: `controller → service → repository → entity`.

- **Auth flow:** JWT access tokens + refresh tokens. `JwtService` handles token generation/validation. `SecurityConfig` defines public vs. protected routes. `DevTokenController` exists (dev profile only) for bypassing login during development.
- **Blog domain:** Posts have status (`DRAFT`, `PUBLISHED`, `ARCHIVED`), auto-generated slugs (via `SlugService`), auto-calculated reading time, and optional excerpt auto-generation. Posts support categories (many-to-one) and tags (many-to-many).
- **Rate limiting:** Bucket4j applied at the controller layer.
- **API docs:** Swagger UI at `http://localhost:8080/swagger-ui.html`, raw spec at `/api-docs`.
- **Health:** Spring Actuator at `/actuator/health`.

**Spring profiles:**
- `dev` — connects to Docker Postgres, verbose logging
- `prod` — production settings
- `test` — H2 in-memory database (used by `./mvnw test`)

### Frontend Architecture

Uses Next.js **App Router** with two route groups:
- `(blog)/` — public-facing blog (home, post detail, category/tag pages, search)
- `admin/` — protected admin CMS (post CRUD, category management, dashboard)
- `login/` and `setup/` — auth and one-time admin initialization

**Key files:**
- `src/lib/api.ts` — central API client for all backend calls
- `src/lib/auth.tsx` — React context for auth state
- `src/lib/types.ts` — shared TypeScript types
- `src/lib/constants.ts` — configuration constants (API base URL, etc.)
- `src/components/blog/MarkdownRenderer.tsx` — renders post content with syntax highlighting

Security headers (CSP, X-Frame-Options, etc.) are configured in `next.config.mjs`.

### API Contract

The backend API base path is `/api`. CORS is configured to allow `http://localhost:3000`. Auth endpoints are under `/api/auth/`; blog endpoints under `/api/posts/`, `/api/categories/`, `/api/tags/`.

## Testing

- **Backend:** JUnit 5 + Mockito + AssertJ. Tests use the `test` Spring profile (H2, no Docker needed). Integration tests (e.g., `AuthControllerIT`) use `@SpringBootTest`.
- **Frontend:** No test framework is currently set up.

## Environment Variables

Copy `.env.example` to `.env` for local development. Key variables:
- `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_PORT` — consumed by `docker-compose.yml`
- `SPRING_PROFILES_ACTIVE` — set to `dev` locally
- `JWT_SECRET` — base64-encoded secret, min 256 bits for HS256
- `JWT_EXPIRATION_MS` — access token lifetime (default 24h)
