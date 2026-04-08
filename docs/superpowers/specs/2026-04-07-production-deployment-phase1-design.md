# Production Deployment — Phase 1 Design

**Date:** 2026-04-07
**Scope:** First production deploy using default Railway + Vercel + Supabase URLs (no custom domain)
**Branch strategy:** `dev` → PR → `main` triggers deploy

---

## 1. Architecture

```
Browser
  └── Vercel (Next.js)          https://<app>.vercel.app
        └── fetch credentials:include
              └── Railway (Spring Boot)   https://<app>.railway.app
                    └── Supabase (PostgreSQL)
```

Frontend and backend live on different eTLD+1 domains (`.vercel.app` ≠ `.railway.app`), making them cross-site. This requires `SameSite=None; Secure` on all auth cookies.

---

## 2. Code Changes

### 2.1 Cookie SameSite Fix (`AuthController.java`)

Change `sameSite("Lax")` → `sameSite("None")` in all 4 cookie declarations:
- `accessToken` set (login/setup/refresh)
- `refreshToken` set (login/setup/refresh)
- `accessToken` clear (logout)
- `refreshToken` clear (logout)

`Secure=true` is already enforced in prod via `cookieSecure` injected from `COOKIE_SECURE=true`.

### 2.2 Flyway Dependency (`pom.xml`)

Add to `<dependencies>`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Spring Boot manages the Flyway version via `spring-boot-dependencies` BOM — no version needed.

### 2.3 Flyway Migration (`V1__init.sql`)

Path: `backend/src/main/resources/db/migration/V1__init.sql`

```sql
-- Users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ
);

-- Categories
CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL
);

-- Tags
CREATE TABLE tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50) NOT NULL UNIQUE,
    slug       VARCHAR(60) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

-- Posts
CREATE TABLE posts (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                VARCHAR(200) NOT NULL,
    slug                 VARCHAR(250) NOT NULL UNIQUE,
    content              TEXT         NOT NULL,
    excerpt              VARCHAR(300),
    status               VARCHAR(20)  NOT NULL,
    author_id            UUID         NOT NULL REFERENCES users(id),
    category_id          UUID         REFERENCES categories(id),
    reading_time_minutes INTEGER,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    deleted_at           TIMESTAMPTZ
);

-- Post-Tag junction
CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts(id),
    tag_id  UUID NOT NULL REFERENCES tags(id),
    PRIMARY KEY (post_id, tag_id)
);

-- Refresh Tokens
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id    UUID         NOT NULL REFERENCES users(id),
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_refresh_token_hash    ON refresh_tokens(token_hash);
CREATE        INDEX idx_refresh_token_expires ON refresh_tokens(expires_at);
CREATE        INDEX idx_refresh_token_user    ON refresh_tokens(user_id);
```

Flyway runs this automatically on startup before the app accepts traffic. `ddl-auto: validate` then confirms the schema matches the entities.

---

## 3. Infrastructure Setup (Manual, One-Time)

### 3.1 Supabase

1. Create new project at supabase.com
2. Go to **Settings → Database → Connection string → URI**
3. Copy `DATABASE_URL` (format: `postgresql://user:pass@host:5432/postgres`)

### 3.2 Railway

1. Create new project → **Deploy from GitHub repo**
2. Select `the-replicant` repo, set root directory to `backend/`
3. Railway auto-detects Maven and builds the JAR
4. Set environment variables:

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | From Supabase |
| `JWT_SECRET` | Generate: `openssl rand -base64 64` |
| `JWT_EXPIRATION_MS` | `86400000` |
| `CORS_ORIGINS` | `https://<app>.vercel.app` |
| `COOKIE_SECURE` | `true` |

5. Note the Railway service URL and service ID for GitHub secrets.

### 3.3 Vercel

1. Create new project → Import from GitHub repo
2. Set **Root Directory** to `frontend/`
3. Framework preset: Next.js (auto-detected)
4. Set environment variables:

| Variable | Value |
|---|---|
| `NEXT_PUBLIC_API_URL` | `https://<app>.railway.app/api/v1` |
| `NEXT_PUBLIC_SITE_URL` | `https://<app>.vercel.app` |

---

## 4. GitHub Secrets

Go to repo **Settings → Secrets and variables → Actions**. Add:

| Secret | Where to get it |
|---|---|
| `RAILWAY_TOKEN` | Railway → Account Settings → Tokens |
| `RAILWAY_SERVICE_ID` | Railway → Project → Service → Settings |
| `VERCEL_TOKEN` | Vercel → Settings → Tokens |
| `VERCEL_ORG_ID` | Vercel → Project Settings → General |
| `VERCEL_PROJECT_ID` | Vercel → Project Settings → General |

---

## 5. CI/CD Workflows

### 5.1 `.github/workflows/backend.yml`

Triggers on push to `main` with changes under `backend/**`.

Steps:
1. Checkout
2. Setup Java 21 (Temurin)
3. Cache `~/.m2/repository`
4. `./mvnw test` — runs against H2, no Docker needed
5. `./mvnw package -DskipTests` — builds JAR
6. Deploy to Railway via `railway up` CLI using `RAILWAY_TOKEN` + `RAILWAY_SERVICE_ID`

### 5.2 `.github/workflows/frontend.yml`

Triggers on push to `main` with changes under `frontend/**`.

Steps:
1. Checkout
2. Setup Node 20
3. Cache `frontend/node_modules`
4. `npm ci`
5. `npm run lint`
6. `npm run build`
7. Deploy to Vercel via `vercel --prod` CLI using `VERCEL_TOKEN`, `VERCEL_ORG_ID`, `VERCEL_PROJECT_ID`

---

## 6. First Deploy Sequence

Execute in this exact order to avoid dependency failures:

1. **Supabase:** Create project, copy `DATABASE_URL`
2. **Railway:** Create project, set all env vars, deploy manually once to validate Flyway runs
3. **Vercel:** Create project, set `NEXT_PUBLIC_API_URL` (Railway URL) + `NEXT_PUBLIC_SITE_URL`
4. **Update Railway `CORS_ORIGINS`** with the Vercel URL (now known)
5. **Add GitHub secrets** (Railway + Vercel tokens)
6. **Merge `dev` → `main`** — both Actions workflows fire
7. **Initialize admin:** `POST https://<app>.railway.app/api/v1/auth/setup` with `{ "email": "...", "password": "...", "name": "..." }`
8. **Smoke test:** Login at `https://<app>.vercel.app/login`, create a draft post, verify public blog renders it

---

## 7. Out of Scope (Phase 2)

- Custom domain (`thereplicante.com`) — requires DNS config in Railway + Vercel + updating `CORS_ORIGINS` and `NEXT_PUBLIC_API_URL`
- Project rename from `thereplicant` → `thereplicante`
- Staging environment
- GitHub Actions on PRs (only `main` deploys in Phase 1)
