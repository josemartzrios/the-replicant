# Deployment Architecture

> Infrastructure, CI/CD pipeline, and deployment strategy for The Replicant.

---

## Infrastructure Overview

```mermaid
flowchart TB
    subgraph Developer_Machine
        DEV["💻 Local Machine"]
    end

    subgraph GitHub_Infra
        REPO["📦 Repository"]
        ACTIONS["⚙️ GitHub Actions"]
    end

    subgraph Vercel_Frontend
        VERCEL_EDGE["🌐 Edge Network"]
        NEXTJS["⚛️ Next.js App"]
    end

    subgraph Railway_Backend
        RAILWAY["🚂 Container"]
        SPRING["☕ Spring Boot"]
    end

    subgraph Supabase_Database
        SUPABASE["🐘 PostgreSQL"]
    end

    subgraph Resend_Email
        RESEND["📧 Email Service"]
    end

    DEV -->|git push| REPO
    REPO -->|trigger| ACTIONS
    ACTIONS -->|deploy| VERCEL_EDGE
    ACTIONS -->|deploy| RAILWAY
    
    VERCEL_EDGE --> NEXTJS
    NEXTJS -->|API calls| SPRING
    SPRING --> SUPABASE
    SPRING --> RESEND

    classDef service fill:#4ade80,color:#000
    classDef infra fill:#60a5fa,color:#000

    class VERCEL_EDGE,RAILWAY,SUPABASE,RESEND service
    class ACTIONS infra
```

---

## Environment Configuration

### Development (Local)

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: replicant_dev
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
    ports:
      - "5432:5432"
```

### Staging

| Service | URL |
|---------|-----|
| Frontend | `https://staging.thereplicant.blog` |
| Backend | `https://api-staging.thereplicant.blog` |
| Database | Supabase (staging project) |

### Production

| Service | URL | Provider |
|---------|-----|----------|
| Frontend | `https://thereplicant.blog` | Vercel |
| Backend | `https://api.thereplicant.blog` | Railway |
| Database | Supabase connection string | Supabase |
| Email | API Key | Resend |

---

## CI/CD Pipeline

```mermaid
flowchart LR
    subgraph Trigger_Events
        PUSH["git push"]
        PR["Pull Request"]
    end

    subgraph CI_Pipeline
        LINT["🔍 Lint"]
        TEST["🧪 Test"]
        BUILD["🔨 Build"]
        SECURITY["🔒 Security Scan"]
    end

    subgraph CD_Pipeline
        DEPLOY_DEV["🚀 Deploy Dev"]
        DEPLOY_QA["🚀 Deploy QA"]
        DEPLOY_PROD["🚀 Deploy Prod"]
    end

    PUSH --> LINT
    PR --> LINT
    LINT --> TEST
    TEST --> BUILD
    BUILD --> SECURITY

    SECURITY -->|dev branch| DEPLOY_DEV
    SECURITY -->|qa branch| DEPLOY_QA
    SECURITY -->|main branch| DEPLOY_PROD
```

### GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, dev, qa]
  pull_request:
    branches: [main, dev]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Tests
        run: cd backend && ./mvnw verify
      
      - name: Build
        run: cd backend && ./mvnw package -DskipTests

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install & Test
        run: |
          cd frontend
          npm ci
          npm run lint
          npm run test
          npm run build
```

---

## Deployment Strategy

### Blue-Green Deployment (Railway)

```mermaid
flowchart LR
    subgraph "Production"
        LB["Load Balancer"]
        BLUE["🔵 Blue (Current)"]
        GREEN["🟢 Green (New)"]
    end

    LB -->|100%| BLUE
    LB -.->|0%| GREEN

    subgraph "After Validation"
        LB2["Load Balancer"]
        BLUE2["🔵 Blue (Old)"]
        GREEN2["🟢 Green (Current)"]
    end

    LB2 -.->|0%| BLUE2
    LB2 -->|100%| GREEN2
```

### Rollback Strategy

1. **Automatic**: Health check fails → Rollback to previous
2. **Manual**: Railway dashboard → Restore previous deployment
3. **Database**: No destructive migrations in single release

---

## Cost Breakdown

### Free Tier (MVP)

| Service | Tier | Limit | Cost |
|---------|------|-------|------|
| Vercel | Hobby | 100GB bandwidth | $0 |
| Railway | Free | 500 hours/month | $0 |
| Supabase | Free | 500MB, 2GB transfer | $0 |
| Resend | Free | 3,000 emails/month | $0 |
| GitHub Actions | Free | 2,000 min/month | $0 |

**Total**: **$0/month**

### Growth Tier (Post-MVP)

| Service | Tier | Limit | Cost |
|---------|------|-------|------|
| Vercel | Pro | 1TB bandwidth | $20 |
| Railway | Hobby | $5 credits | $5 |
| Supabase | Pro | 8GB, 50GB transfer | $25 |
| Resend | Pro | 50,000 emails | $20 |

**Total**: **~$70/month**

---

## Monitoring & Observability

### Health Checks

```java
@RestController
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Health> health() {
        return ResponseEntity.ok(new Health("UP", Instant.now()));
    }
    
    @GetMapping("/health/ready")
    public ResponseEntity<?> readiness(DataSource ds) {
        // Check database connection
        try (Connection conn = ds.getConnection()) {
            return ResponseEntity.ok().build();
        } catch (SQLException e) {
            return ResponseEntity.status(503).build();
        }
    }
}
```

### Logging

```java
// Structured logging with Logback
{
  "timestamp": "2026-01-19T12:00:00Z",
  "level": "INFO",
  "service": "the-replicant-api",
  "traceId": "abc123",
  "message": "Post created",
  "postId": "uuid",
  "authorId": "uuid"
}
```

### Alerts

| Metric | Threshold | Action |
|--------|-----------|--------|
| Response time | > 2s | Slack notification |
| Error rate | > 1% | Page on-call |
| Database connections | > 80% | Scale alert |
| Disk usage | > 80% | Cleanup trigger |

---

## Security in Deployment

### Secrets Management

```bash
# Railway environment variables
DATABASE_URL=postgresql://user:pass@host:5432/db
JWT_SECRET=<64-char-random>
RESEND_API_KEY=re_xxx

# Vercel environment variables
NEXT_PUBLIC_API_URL=https://api.thereplicant.blog
```

### Network Security

- **HTTPS Only**: Enforced at edge (Vercel, Railway)
- **CORS**: Restrict to frontend domain only
- **Rate Limiting**: At API gateway level
- **DDoS Protection**: Vercel Edge built-in

---

## Backup Strategy

| Data | Frequency | Retention | Location |
|------|-----------|-----------|----------|
| Database | Daily | 7 days | Supabase automatic |
| Database | Weekly | 30 days | Manual export to S3 |
| Media files | N/A | N/A | Not in MVP scope |

---

**Document Version**: 1.0  
**Created**: 2026-01-19  
**Last Updated**: 2026-01-19
