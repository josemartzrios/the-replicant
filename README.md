# The Replicant

> A modern, API-first blogging platform built with Spring Boot and Next.js

## Vision

The Replicant is a cyberpunk blog platform designed for readers that wants a different opinion from a normal human (maybe). Built with an API-first approach, it enables future expansion to mobile apps or alternative frontends without backend changes.

## Core Features (MVP v1.0)

### Must Have
- ✅ User authentication (JWT-based)
- ✅ Create, read, update, delete blog posts
- ✅ Markdown support for writing
- ✅ Post categorization and tagging
- ✅ Search functionality
- ✅ Responsive reading experience

### Future features
- Progress bar
- IA Fact checking for sources
- Sharing for IG Stories
- Comments


### Won't Have (v1.0)
- ❌ Comments system (future)
- ❌ Social media integration
- ❌ Analytics dashboard
- ❌ Multiple authors
- ❌ Draft scheduling

## Tech Stack

### Backend (API)
- **Framework**: Spring Boot 3.x
- **Language**: Java 21+
- **Database**: PostgreSQL 15+
- **Cache**: Redis
- **Auth**: Spring Security + JWT
- **API Documentation**: OpenAPI 3.0 (Swagger)

### Frontend
- **Framework**: Next.js 14+ (React)
- **Language**: TypeScript
- **Styling**: TailwindCSS
- **State Management**: React Query
- **API Client**: Auto-generated from OpenAPI spec

### Infrastructure
- **Development**: Docker Compose
- **CI/CD**: GitHub Actions
- **Hosting**: Railway (backend), Vercel (frontend)

## Project Structure

```
the-replicant/
├── backend/              # Spring Boot API
│   ├── src/
│   ├── api-spec.yaml    # OpenAPI specification
│   └── docker-compose.yml
├── frontend/            # Next.js app
│   ├── src/
│   └── package.json
├── docs/               # Architecture & diagrams
└── README.md
```

## Quick Start

### Prerequisites
- Java 21+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL 15+ (or use Docker)

### Backend Setup
```bash
cd backend
./mvnw spring-boot:run
```

API will be available at `http://localhost:8080`
Swagger UI at `http://localhost:8080/swagger-ui.html`

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

Frontend will be available at `http://localhost:3000`

## Development Roadmap

### Sprint 0: Foundation (Week 1)
- [x] Project setup and repository structure
- [ ] Docker Compose configuration
- [ ] Database schema design
- [ ] OpenAPI specification v1
- [ ] Basic authentication setup

### Sprint 1: Core API (Week 2)
- [ ] POST /api/v1/posts - Create post
- [ ] GET /api/v1/posts - List posts
- [ ] GET /api/v1/posts/{id} - Get single post
- [ ] PUT /api/v1/posts/{id} - Update post
- [ ] DELETE /api/v1/posts/{id} - Delete post
- [ ] Unit and integration tests

### Sprint 2: Frontend MVP (Week 3)
- [ ] Homepage with post list
- [ ] Post detail page
- [ ] Markdown renderer
- [ ] Admin dashboard for CRUD
- [ ] Responsive design

### Sprint 3: Polish & Deploy (Week 4)
- [ ] Search implementation
- [ ] SEO optimization
- [ ] Performance tuning
- [ ] Production deployment
- [ ] Basic monitoring

## API Endpoints (Planned)

```
POST   /api/v1/auth/login
POST   /api/v1/auth/register
GET    /api/v1/posts
GET    /api/v1/posts/{id}
POST   /api/v1/posts
PUT    /api/v1/posts/{id}
DELETE /api/v1/posts/{id}
GET    /api/v1/categories
GET    /api/v1/tags
```

## Contributing

This is a personal project, but feedback and suggestions are welcome via issues.

## License

MIT License - See LICENSE file for details

## Contact

- Blog: [To be deployed]
- GitHub: [Your GitHub URL]
- Email: [Your Email]

---

**Current Status**: 🚧 In Development - Sprint 0

**Last Updated**: January 2026