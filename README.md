# The Replicant

> A cyberpunk blog platform built with Spring Boot and Next.js — API-first, minimal cost develop with high value software standards.

## 🎯 Vision

A personal blogging platform for sharing unconventional perspectives. Built API-first for future expansion (mobile apps, alternative frontends) without backend changes.

**Philosophy**: Quality code over feature creep. Ship fast, iterate faster.

## 📋 MVP Scope

### ✅ Core Features (v1.0)
| Feature | Description | Priority |
|---------|-------------|----------|
| Authentication | JWT-based, single admin user | P0 |
| Blog CRUD | Create, read, update, delete posts | P0 |
| Markdown | Full markdown support for writing | P0 |
| Categories & Tags | Basic organization | P1 |
| Search | Full-text search on posts | P1 |
| Responsive UI | Mobile-first reading experience | P0 |

### 🚫 Out of Scope (v1.0)
- Comments system
- Social media integration
- Analytics dashboard
- Multiple authors
- Draft scheduling

### 🔮 Future Features (v1.1+)
- Reading progress bar
- AI fact-checking for sources
- IG Stories sharing
- Comments with moderation

---

## 🛠 Tech Stack (MVP-Optimized)

### Backend
| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.x | REST API Framework |
| Java | 21+ | Language |
| PostgreSQL | 15+ | Primary Database |
| Spring Security | - | Authentication (JWT) |
| OpenAPI | 3.0 | API Documentation |

> **💡 MVP Decision**: No Redis. PostgreSQL handles caching via query optimization. Add Redis when needed.

### Frontend
| Technology | Purpose |
|------------|---------|
| Next.js 14+ | React Framework (SSR/SSG) |
| TypeScript | Type Safety |
| TailwindCSS | Styling |
| TanStack Query | Server State Management |
| OpenAPI Generator | Type-safe API Client |

### Infrastructure (Free/Low-Cost)
| Service | Tier | Monthly Cost |
|---------|------|--------------|
| [Railway](https://railway.app) | Free/Hobby | $0-5 |
| [Vercel](https://vercel.com) | Hobby | $0 |
| [Supabase](https://supabase.com) | Free | $0 |
| GitHub Actions | Free | $0 |

**Estimated MVP Cost**: **$0-5/month**

---

## 📁 Project Structure

```
the-replicant/
├── backend/                    # Spring Boot API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/.../       # Application code
│   │   │   └── resources/      # Configuration
│   │   └── test/               # Tests
│   ├── api-spec.yaml           # OpenAPI specification
│   └── Dockerfile
├── frontend/                   # Next.js app
│   ├── src/
│   │   ├── app/               # App router
│   │   ├── components/        # UI components
│   │   └── lib/               # Utilities
│   └── package.json
├── docs/                       # Documentation
│   ├── branching-strategy.md
│   └── development-roadmap.md
├── docker-compose.yml          # Local development
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites
```bash
# Required
- Java 21+
- Node.js 18+
- Docker & Docker Compose
```

### Local Development
```bash
# 1. Start infrastructure (PostgreSQL)
docker-compose up -d

# 2. Backend (Terminal 1)
cd backend
./mvnw spring-boot:run
# API: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html

# 3. Frontend (Terminal 2)
cd frontend
npm install
npm run dev
# App: http://localhost:3000
```

---

## 📡 API Endpoints

### Authentication
```
POST /api/v1/auth/register    # Admin registration (one-time)
POST /api/v1/auth/login       # Login → JWT token
POST /api/v1/auth/refresh     # Refresh token
```

### Blog Posts
```
GET    /api/v1/posts          # List posts (paginated)
GET    /api/v1/posts/{slug}   # Get single post
POST   /api/v1/posts          # Create post [Auth Required]
PUT    /api/v1/posts/{id}     # Update post [Auth Required]
DELETE /api/v1/posts/{id}     # Delete post [Auth Required]
```

### Organization
```
GET /api/v1/categories        # List categories
GET /api/v1/tags              # List tags
GET /api/v1/posts/search      # Full-text search
```

---

## 📈 Development Roadmap

> See [docs/development-roadmap.md](docs/development-roadmap.md) for detailed breakdown.

| Phase | Focus | Duration |
|-------|-------|----------|
| Sprint 0 | Planning & Design | Week 1 |
| Sprint 1 | Backend Core (Auth + CRUD) | Week 2 |
| Sprint 2 | Frontend MVP | Week 3 |
| Sprint 3 | Deploy & Polish | Week 4 |

**Current Status**: 🚧 Sprint 0 - Foundation

---

## 🔒 Security Standards

This project follows OWASP Top 10 guidelines:

- **A01**: Role-based access control (admin-only writes)
- **A02**: BCrypt password hashing, JWT with short expiry
- **A03**: Parameterized queries (JPA), input validation
- **A05**: Security headers (CSP, HSTS, X-Frame-Options)
- **A09**: Structured logging (no sensitive data)

---

## 🧪 Quality Standards

- **Testing**: Unit + Integration tests (JUnit 5, React Testing Library)
- **Code Quality**: Checkstyle, ESLint, Prettier
- **Git Flow**: [Branching Strategy](docs/branching-strategy.md)
- **Commits**: Conventional Commits specification
- **CI/CD**: GitHub Actions (lint → test → build → deploy)

---

## 📝 Contributing

Personal project — feedback and suggestions welcome via [Issues](../../issues).

## 📄 License

MIT License — See [LICENSE](LICENSE) file.

## 📬 Contact

| Channel | Link |
|---------|------|
| Blog | *Coming Soon* |
| GitHub | [@josemartzrios](https://github.com/josemartzrios) |
| Email | josemartzrios14@gmail.com |

---

<div align="center">

**Last Updated**: January 2026  
Built with ☕ Java + ⚛️ React

</div>