# Production Deployment — Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy The Replicant to production (Railway + Vercel + Supabase) with cross-origin cookie auth and automated CI/CD via GitHub Actions.

**Architecture:** Next.js on Vercel makes credentialed fetch requests to Spring Boot on Railway. Since these are different eTLD+1 domains, cookies must use `SameSite=None; Secure`. Flyway manages the PostgreSQL schema on Supabase. Two independent GitHub Actions workflows (backend + frontend) deploy on push to `main`.

**Tech Stack:** Spring Boot 3.2.2, Flyway 10, Next.js 16, Railway CLI, Vercel CLI, GitHub Actions

---

## File Map

| Action | File |
|---|---|
| Modify | `backend/src/main/java/com/thereplicant/api/controller/AuthController.java` |
| Modify | `backend/src/test/java/com/thereplicant/api/controller/AuthControllerIT.java` |
| Modify | `backend/pom.xml` |
| Modify | `backend/src/main/resources/application.yml` |
| Create | `backend/src/main/resources/db/migration/V1__init.sql` |
| Create | `.github/workflows/backend.yml` |
| Create | `.github/workflows/frontend.yml` |

---

## Task 1: Fix SameSite Cookie Attribute

**Files:**
- Modify: `backend/src/main/java/com/thereplicant/api/controller/AuthController.java:54,64,141,143`
- Modify: `backend/src/test/java/com/thereplicant/api/controller/AuthControllerIT.java`

- [ ] **Step 1: Add a failing test that asserts SameSite=None**

In `AuthControllerIT.java`, add a new test inside the `SetupEndpointTests` class (after the existing `shouldCreateAdminSuccessfully` test):

```java
@Test
@DisplayName("Should set SameSite=None on auth cookies for cross-origin support")
void shouldSetSameSiteNoneOnCookies() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/auth/setup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SETUP_JSON))
            .andExpect(status().isCreated())
            .andReturn();

    // Set-Cookie header is the only way to inspect SameSite via MockMvc
    String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
    assertThat(setCookieHeader).containsIgnoringCase("SameSite=None");
}
```

- [ ] **Step 2: Run the test to confirm it fails**

```bash
cd backend && ./mvnw test -Dtest=AuthControllerIT#shouldSetSameSiteNoneOnCookies
```

Expected: FAIL — `expected: "...SameSite=None..." but was "...SameSite=Lax..."`

- [ ] **Step 3: Change all 4 sameSite("Lax") calls to sameSite("None")**

In `AuthController.java`, make the following replacements:

Replace the `setAuthCookies` method (lines 48–71) with:

```java
private void setAuthCookies(HttpServletResponse response, AuthResponse authData) {
    ResponseCookie accessCookie = ResponseCookie.from("accessToken", authData.getAccessToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(authData.getExpiresIn() > 0 ? authData.getExpiresIn() : ACCESS_TOKEN_MAX_AGE)
            .sameSite("None")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

    if (authData.getRefreshToken() != null) {
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authData.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    authData.setAccessToken(null);
    authData.setRefreshToken(null);
}
```

Replace the `logout` method cookie lines (141–143) with:

```java
ResponseCookie clearAccess = ResponseCookie.from("accessToken", "")
        .httpOnly(true).secure(cookieSecure).path("/").maxAge(0).sameSite("None").build();
ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
        .httpOnly(true).secure(cookieSecure).path("/api/v1/auth").maxAge(0).sameSite("None").build();
```

- [ ] **Step 4: Run the new test to confirm it passes**

```bash
cd backend && ./mvnw test -Dtest=AuthControllerIT#shouldSetSameSiteNoneOnCookies
```

Expected: PASS

- [ ] **Step 5: Run the full AuthControllerIT to confirm no regressions**

```bash
cd backend && ./mvnw test -Dtest=AuthControllerIT
```

Expected: All tests PASS (no failures, no errors)

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/thereplicant/api/controller/AuthController.java
git add backend/src/test/java/com/thereplicant/api/controller/AuthControllerIT.java
git commit -m "fix(auth): SameSite=None on cookies for cross-origin prod deploy"
```

---

## Task 2: Add Flyway + Schema Migration

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__init.sql`

- [ ] **Step 1: Add Flyway dependencies to pom.xml**

In `backend/pom.xml`, insert after the PostgreSQL driver block (after line 77, before the H2 block):

```xml
        <!-- Database: Flyway Migrations -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
```

Spring Boot's BOM manages the Flyway version — no `<version>` tag needed.

- [ ] **Step 2: Disable Flyway in the test profile**

In `backend/src/main/resources/application.yml`, find the test profile block (starts with `# Test Profile (H2 In-Memory)`) and add `flyway.enabled: false` so it becomes:

```yaml
---
# ===========================================
# Test Profile (H2 In-Memory)
# ===========================================
spring:
  config:
    activate:
      on-profile: test

  flyway:
    enabled: false

  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

This prevents Flyway from running PostgreSQL-specific SQL against the H2 test database.

- [ ] **Step 3: Create the migrations directory and V1__init.sql**

Create `backend/src/main/resources/db/migration/V1__init.sql` with the full schema:

```sql
-- =============================================
-- V1: Initial schema for The Replicant
-- =============================================

-- Users
CREATE TABLE users (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email                VARCHAR(255) NOT NULL UNIQUE,
    name                 VARCHAR(100) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by           UUID         REFERENCES users(id),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ
);

-- Categories
CREATE TABLE categories (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL
);

-- Tags
CREATE TABLE tags (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50)  NOT NULL UNIQUE,
    slug       VARCHAR(60)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL
);

-- Posts
CREATE TABLE posts (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
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
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
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

- [ ] **Step 4: Run the full test suite to confirm Flyway doesn't break H2 tests**

```bash
cd backend && ./mvnw test
```

Expected: All tests PASS. If you see `FlywayException` or `H2` errors, verify `spring.flyway.enabled: false` is under the correct profile block in `application.yml`.

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml
git add backend/src/main/resources/application.yml
git add backend/src/main/resources/db/migration/V1__init.sql
git commit -m "feat(db): add Flyway + V1 init schema migration"
```

---

## Task 3: Backend GitHub Actions Workflow

**Files:**
- Create: `.github/workflows/backend.yml`

- [ ] **Step 1: Create the .github/workflows directory if it doesn't exist**

```bash
mkdir -p .github/workflows
```

- [ ] **Step 2: Create backend.yml**

Create `.github/workflows/backend.yml`:

```yaml
name: Backend CI/CD

on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/backend.yml'

jobs:
  test-and-deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2/repository
          key: ${{ runner.os }}-maven-${{ hashFiles('backend/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-maven-

      - name: Run tests
        working-directory: backend
        run: ./mvnw test

      - name: Build JAR
        working-directory: backend
        run: ./mvnw package -DskipTests

      - name: Install Railway CLI
        run: npm install -g @railway/cli

      - name: Deploy to Railway
        working-directory: backend
        run: railway up --service=${{ secrets.RAILWAY_SERVICE_ID }} --detach
        env:
          RAILWAY_TOKEN: ${{ secrets.RAILWAY_TOKEN }}
```

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/backend.yml
git commit -m "ci: add backend GitHub Actions workflow (test + Railway deploy)"
```

---

## Task 4: Frontend GitHub Actions Workflow

**Files:**
- Create: `.github/workflows/frontend.yml`

- [ ] **Step 1: Create frontend.yml**

Create `.github/workflows/frontend.yml`:

```yaml
name: Frontend CI/CD

on:
  push:
    branches: [main]
    paths:
      - 'frontend/**'
      - '.github/workflows/frontend.yml'

jobs:
  lint-build-deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Node 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Cache node_modules
        uses: actions/cache@v4
        with:
          path: frontend/node_modules
          key: ${{ runner.os }}-node-${{ hashFiles('frontend/package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-node-

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Lint
        working-directory: frontend
        run: npm run lint

      - name: Build
        working-directory: frontend
        run: npm run build
        env:
          NEXT_PUBLIC_API_URL: ${{ secrets.NEXT_PUBLIC_API_URL }}
          NEXT_PUBLIC_SITE_URL: ${{ secrets.NEXT_PUBLIC_SITE_URL }}

      - name: Install Vercel CLI
        run: npm install -g vercel

      - name: Deploy to Vercel
        working-directory: frontend
        run: vercel --prod --token=${{ secrets.VERCEL_TOKEN }} --yes
        env:
          VERCEL_ORG_ID: ${{ secrets.VERCEL_ORG_ID }}
          VERCEL_PROJECT_ID: ${{ secrets.VERCEL_PROJECT_ID }}
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/frontend.yml
git commit -m "ci: add frontend GitHub Actions workflow (lint + build + Vercel deploy)"
```

---

## Task 5: Infrastructure Setup + First Deploy

This task is entirely manual. Follow the steps in order — each depends on the previous.

### 5.1 Generate JWT Secret

- [ ] **Run this locally to generate a secure secret:**

```bash
openssl rand -base64 64
```

Copy the output. You will paste it as `JWT_SECRET` in Railway.

### 5.2 Supabase

- [ ] Create a new project at supabase.com (free tier)
- [ ] Go to **Project Settings → Database → Connection string → URI**
- [ ] Copy the `DATABASE_URL`. It looks like:
  ```
  postgresql://postgres:<password>@db.<project-ref>.supabase.co:5432/postgres
  ```
- [ ] Keep it handy for the next step.

### 5.3 Railway

- [ ] Create a new project at railway.app
- [ ] Click **+ New** → **GitHub Repo** → select `the-replicant`
- [ ] Set **Root Directory** to `backend`
- [ ] Railway detects Maven automatically and will build the JAR
- [ ] Go to **Service → Variables** and add:

  | Variable | Value |
  |---|---|
  | `SPRING_PROFILES_ACTIVE` | `prod` |
  | `DATABASE_URL` | From Supabase step above |
  | `JWT_SECRET` | From openssl step above |
  | `JWT_EXPIRATION_MS` | `86400000` |
  | `CORS_ORIGINS` | `https://PLACEHOLDER` (update after Vercel) |
  | `COOKIE_SECURE` | `true` |

- [ ] Trigger a manual deploy from the Railway dashboard
- [ ] Wait for the deploy to succeed — check logs for `Flyway` migration output:
  ```
  Flyway Community Edition ... by Redgate
  Database: jdbc:postgresql://... (PostgreSQL ...)
  Successfully validated 1 migration (execution time ...) 
  Current version of schema "public": << Empty Schema >>
  Migrating schema "public" to version "1 - init"
  Successfully applied 1 migration
  ```
- [ ] Note your Railway service URL: `https://<app>.railway.app`
- [ ] Go to **Project → Settings → Tokens** → generate a token — save as `RAILWAY_TOKEN`
- [ ] Go to **Service → Settings** → copy the Service ID — save as `RAILWAY_SERVICE_ID`

### 5.4 Vercel

- [ ] Go to vercel.com → **Add New Project** → Import from GitHub → select `the-replicant`
- [ ] Set **Root Directory** to `frontend`
- [ ] Framework Preset: Next.js (auto-detected)
- [ ] Under **Environment Variables**, add:

  | Variable | Value |
  |---|---|
  | `NEXT_PUBLIC_API_URL` | `https://<app>.railway.app/api/v1` |
  | `NEXT_PUBLIC_SITE_URL` | `https://<app>.vercel.app` |

- [ ] Click **Deploy**
- [ ] Note your Vercel URL: `https://<app>.vercel.app`
- [ ] Go to **Vercel Account Settings → Tokens** → create token → save as `VERCEL_TOKEN`
- [ ] Go to **Project Settings → General** → copy Org ID → save as `VERCEL_ORG_ID`
- [ ] On the same page, copy Project ID → save as `VERCEL_PROJECT_ID`

### 5.5 Update Railway CORS

- [ ] Back in Railway → Service → Variables, update:
  ```
  CORS_ORIGINS = https://<app>.vercel.app
  ```
  (Replace `PLACEHOLDER` with the actual Vercel URL from 5.4)
- [ ] Redeploy Railway service so the CORS change takes effect

### 5.6 Add GitHub Secrets

- [ ] Go to GitHub repo → **Settings → Secrets and variables → Actions → New repository secret**
- [ ] Add all 7 secrets:

  | Secret | Value |
  |---|---|
  | `RAILWAY_TOKEN` | From step 5.3 |
  | `RAILWAY_SERVICE_ID` | From step 5.3 |
  | `VERCEL_TOKEN` | From step 5.4 |
  | `VERCEL_ORG_ID` | From step 5.4 |
  | `VERCEL_PROJECT_ID` | From step 5.4 |
  | `NEXT_PUBLIC_API_URL` | `https://<app>.railway.app/api/v1` |
  | `NEXT_PUBLIC_SITE_URL` | `https://<app>.vercel.app` |

### 5.7 Merge dev → main (triggers CI/CD)

- [ ] Open a PR from `dev` → `main` on GitHub
- [ ] Merge the PR
- [ ] Go to **Actions** tab — verify both `Backend CI/CD` and `Frontend CI/CD` workflows start
- [ ] Wait for both to complete (green checkmarks)

### 5.8 Initialize Admin User

- [ ] Run this curl command (replace the Railway URL and credentials):

```bash
curl -X POST https://<app>.railway.app/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "your@email.com",
    "password": "YourSecurePassword123!",
    "name": "Your Name"
  }'
```

Expected response: HTTP 201 with `"user": { "role": "ADMIN", ... }`

### 5.9 Smoke Test

- [ ] Open `https://<app>.vercel.app/login` — enter your credentials
- [ ] Verify you land on the admin dashboard
- [ ] Create a new post with status PUBLISHED
- [ ] Open `https://<app>.vercel.app` — verify the post appears on the public blog
- [ ] Logout — verify you are redirected to `/login`
