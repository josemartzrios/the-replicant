# Security Architecture

> Authentication flow, JWT lifecycle, and security model for The Replicant.

---

## Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Note over U,DB: First-Time Setup (One-Time Only)
    U->>FE: Visit /setup
    FE->>BE: GET /api/v1/auth/status
    BE->>DB: SELECT COUNT(*) FROM users
    DB-->>BE: count = 0
    BE-->>FE: { "setupRequired": true }
    FE-->>U: Show setup form
    U->>FE: Submit admin credentials
    FE->>BE: POST /api/v1/auth/setup
    BE->>DB: INSERT INTO users (admin)
    BE->>BE: Generate JWT tokens
    BE-->>FE: { accessToken, refreshToken }
    FE-->>U: Redirect to /admin

    Note over U,DB: Normal Login Flow
    U->>FE: Visit /login
    U->>FE: Submit credentials
    FE->>BE: POST /api/v1/auth/login
    BE->>DB: SELECT user WHERE email = ?
    BE->>BE: Verify password (BCrypt)
    BE->>BE: Generate JWT tokens
    BE->>DB: INSERT refresh_token
    BE-->>FE: { accessToken, refreshToken }
    FE->>FE: Store tokens securely
    FE-->>U: Redirect to /admin
```

---

## JWT Token Structure

### Access Token (Short-lived)

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid-here",
    "email": "admin@example.com",
    "role": "ADMIN",
    "iat": 1706000000,
    "exp": 1706086400
  }
}
```

| Claim | Description | Value |
|-------|-------------|-------|
| sub | User ID (UUID) | `"550e8400-e29b-41d4..."` |
| email | User email | `"admin@example.com"` |
| role | Authorization level | `"ADMIN"` or `"AUTHOR"` |
| iat | Issued at | Unix timestamp |
| exp | Expires at | iat + 24 hours |

### Refresh Token (Long-lived)
- Random 64-character string
- Stored **hashed** in database
- Valid for 7 days
- Single-use (rotated on refresh)

---

## Token Refresh Flow

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database

    Note over FE,DB: Access Token Expired
    FE->>BE: POST /api/v1/auth/refresh
    Note right of FE: { refreshToken: "abc123..." }
    
    BE->>DB: SELECT token WHERE hash = ?
    DB-->>BE: Token record
    
    alt Token Valid & Not Expired
        BE->>DB: DELETE old refresh token
        BE->>BE: Generate new access + refresh tokens
        BE->>DB: INSERT new refresh token
        BE-->>FE: { accessToken, refreshToken }
    else Token Invalid or Expired
        BE-->>FE: 401 Unauthorized
        FE->>FE: Redirect to /login
    end
```

---

## Password Recovery Flow

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant BE as Backend
    participant DB as Database
    participant Email as Email Service

    U->>FE: Click "Forgot Password"
    U->>FE: Enter email
    FE->>BE: POST /api/v1/auth/forgot-password
    BE->>DB: SELECT user WHERE email = ?
    
    alt User Exists
        BE->>BE: Generate random 64-char token
        BE->>DB: INSERT password_reset_token (hashed)
        BE->>Email: Send reset email
    end
    
    BE-->>FE: { "message": "If account exists, email sent" }
    Note right of BE: Always same response (prevent enumeration)
    
    U->>Email: Click reset link
    U->>FE: Visit /reset-password?token=xyz
    U->>FE: Enter new password
    FE->>BE: POST /api/v1/auth/reset-password
    BE->>DB: SELECT token WHERE hash = ?
    
    alt Token Valid & Not Used
        BE->>DB: UPDATE user SET password_hash = ?
        BE->>DB: UPDATE token SET used_at = NOW()
        BE->>DB: DELETE all refresh_tokens for user
        BE-->>FE: { "message": "Password updated" }
    else Token Invalid
        BE-->>FE: 400 Bad Request
    end
```

---

## Security Model

### Role-Based Access Control (RBAC)

```mermaid
flowchart LR
    subgraph Roles_Section
        ADMIN["🔑 ADMIN"]
        AUTHOR["✍️ AUTHOR"]
        PUBLIC["👤 PUBLIC"]
    end

    subgraph Resources_Section
        USERS["Users"]
        POSTS["Posts"]
        CATEGORIES["Categories"]
        READ_PUBLIC["Public Read"]
    end

    ADMIN -->|Full Access| USERS
    ADMIN -->|Full Access| POSTS
    ADMIN -->|Full Access| CATEGORIES
    
    AUTHOR -->|CRUD Own| POSTS
    AUTHOR -->|Read| CATEGORIES
    
    PUBLIC -->|Read Published| READ_PUBLIC
```

### Endpoint Security Matrix

| Endpoint | Method | Public | Author | Admin |
|----------|--------|--------|--------|-------|
| `/api/v1/auth/login` | POST | ✅ | ✅ | ✅ |
| `/api/v1/auth/setup` | POST | ✅* | ❌ | ❌ |
| `/api/v1/posts` | GET | ✅ | ✅ | ✅ |
| `/api/v1/posts` | POST | ❌ | ✅ | ✅ |
| `/api/v1/posts/{id}` | PUT | ❌ | ✅** | ✅ |
| `/api/v1/posts/{id}` | DELETE | ❌ | ❌ | ✅ |
| `/api/v1/users` | GET | ❌ | ❌ | ✅ |
| `/api/v1/users` | POST | ❌ | ❌ | ✅ |

*Only when no users exist  
**Only own posts

---

## Security Headers

```java
// Spring Security Configuration
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .and()
    .frameOptions().deny()
    .and()
    .xssProtection().enable()
    .and()
    .httpStrictTransportSecurity()
        .maxAgeInSeconds(31536000)
        .includeSubDomains(true);
```

| Header | Value | Purpose |
|--------|-------|---------|
| Content-Security-Policy | `default-src 'self'` | Prevent XSS |
| X-Frame-Options | `DENY` | Prevent clickjacking |
| X-Content-Type-Options | `nosniff` | Prevent MIME sniffing |
| Strict-Transport-Security | `max-age=31536000` | Force HTTPS |
| X-XSS-Protection | `1; mode=block` | XSS filter |

---

## Rate Limiting

| Endpoint | Limit | Window | Action on Exceed |
|----------|-------|--------|------------------|
| Login | 5 attempts | 15 min | Delay response |
| Login | 10 attempts | 30 min | Account lockout |
| Password Reset | 3 requests | 1 hour | Silent ignore |
| User Creation | 5 users | 1 hour | 429 Error |
| API (general) | 100 requests | 1 min | 429 Error |

---

## Audit Logging

All security events are logged:

```java
@Slf4j
public class SecurityAuditLogger {
    
    public void logLoginAttempt(String email, boolean success, String ip) {
        log.info("LOGIN_ATTEMPT email={} success={} ip={}", 
                 maskEmail(email), success, ip);
    }
    
    public void logPasswordReset(UUID userId, String ip) {
        log.info("PASSWORD_RESET userId={} ip={}", userId, ip);
    }
    
    public void logUserCreated(UUID creatorId, UUID newUserId) {
        log.info("USER_CREATED by={} new={}", creatorId, newUserId);
    }
}
```

---

**Document Version**: 1.0  
**Created**: 2026-01-19  
**Last Updated**: 2026-01-19
