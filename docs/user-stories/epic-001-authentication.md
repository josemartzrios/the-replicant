# Epic 001: Authentication

> **Priority**: P0 (Must Have)  
> **Sprint**: 1  
> **Dependencies**: None

## Overview

Secure authentication system with JWT tokens and invite-based user management. Designed for a personal blog where the owner (admin) controls who can write.

---

## US-001: Admin Login ✅

**As an** admin  
**I want** to log in with my credentials  
**So that** I can access the admin dashboard to manage blog posts

### Acceptance Criteria

- [x] Login form accepts email and password
- [x] Invalid credentials show error message (generic, no user enumeration)
- [ ] Successful login redirects to admin dashboard (frontend pending)
- [ ] JWT token stored securely (httpOnly cookie or secure storage) (frontend pending)
- [x] Token expires after 24 hours
- [ ] Rate limiting: max 5 failed attempts per 15 minutes
- [ ] Account lockout after 10 consecutive failures (30 min cooldown)

### Technical Notes

- Endpoint: `POST /api/v1/auth/login`
- Request: `{ "email": string, "password": string }`
- Response: `{ "accessToken": string, "refreshToken": string, "expiresIn": number }`
- Password validation: BCrypt with cost factor 12
- JWT includes: userId, role, exp, iat

### Security Considerations

- [ ] Use HTTPS only (deploy time)
- [ ] Implement CSRF protection (deploy time)
- [x] Log all authentication attempts (success and failure)
- [x] Generic error messages (prevent user enumeration)
- [ ] Delay response on failed attempts (+500ms per failure)

---

## US-002a: First Admin Setup (One-Time) ✅

**As the** blog owner deploying the application for the first time  
**I want** to create my admin account through a secure one-time setup  
**So that** I can initialize the system without exposing a permanent public registration endpoint

### Acceptance Criteria

- [x] Setup page available ONLY when zero users exist in database
- [ ] Setup route: `/setup` (frontend) redirects to login if users exist (frontend pending)
- [x] Form requires: email, name, password, confirm password
- [x] Password minimum: 12 characters, 1 uppercase, 1 number, 1 special
- [x] User created with role = ADMIN automatically
- [x] Automatic login after successful setup
- [x] Endpoint returns 403 Forbidden if any user already exists
- [ ] Success message: "Admin account created. Welcome to The Replicant!" (frontend pending)

### Technical Notes

- Endpoint: `POST /api/v1/auth/setup`
- Auth: Not required
- Request: `{ "email": string, "name": string, "password": string }`
- Response: `{ "accessToken": string, "refreshToken": string, "user": UserDTO }`

**Pre-condition check (CRITICAL):**
```java
// Before processing request
long userCount = userRepository.count();
if (userCount > 0) {
    throw new ForbiddenException("Setup already completed");
}
```

### Frontend Flow

```
User visits /setup
  ├── Backend check: users exist?
  │     ├── YES → Redirect to /login
  │     └── NO  → Show setup form
  └── On submit → Create admin → Auto-login → Redirect to /admin
```

### Security Considerations

- [x] Double-check user count in a transaction (prevent race conditions)
- [x] Log the setup event with IP address and timestamp
- [x] No way to re-enable this endpoint after first admin exists

### Test Cases

| Scenario | Expected Result |
|----------|-----------------|
| No users exist, valid data | Admin created, token returned |
| No users exist, weak password | 400 Bad Request |
| Users already exist | 403 Forbidden |
| Concurrent setup requests | Only first succeeds, others fail |

---

## US-002: Create User (Admin)

**As an** admin  
**I want** to create new user accounts directly from the dashboard  
**So that** I can add trusted collaborators without a public registration endpoint

### Acceptance Criteria

- [ ] Admin can create users from dashboard (Users section)
- [ ] Form requires: email, name, temporary password, role (ADMIN or AUTHOR)
- [ ] Password minimum: 12 characters, 1 uppercase, 1 number, 1 special
- [ ] New user must change password on first login
- [ ] Confirmation message after successful creation
- [ ] List of all users visible in admin dashboard

### Technical Notes

**Create User (Admin only):**
- Endpoint: `POST /api/v1/users`
- Auth: Required (Admin role only)
- Request: `{ "email": string, "name": string, "password": string, "role": "ADMIN" | "AUTHOR" }`
- Response: `{ "user": UserDTO }`

**List Users (Admin only):**
- Endpoint: `GET /api/v1/users`
- Auth: Required (Admin role only)

### Data Model

```
User {
  id: UUID
  email: String (unique)
  name: String
  passwordHash: String
  role: Enum (ADMIN, AUTHOR)
  mustChangePassword: Boolean (default: true for created users)
  createdBy: UUID (FK to User, null for first admin)
  createdAt: DateTime
  updatedAt: DateTime
}
```

### Security Considerations

- [ ] Only ADMIN role can create users
- [ ] Password hashed with BCrypt before storing
- [ ] Audit log of user creation (who created whom)
- [ ] Rate limit user creation (max 5 per hour)
- [ ] Force password change on first login

---

## US-003: Token Refresh ✅

**As an** authenticated user  
**I want** my session to refresh automatically  
**So that** I don't get logged out while actively using the dashboard

### Acceptance Criteria

- [ ] Access token refreshes before expiration (silent refresh) (frontend pending)
- [x] Refresh token valid for 7 days
- [x] Refresh token rotated on each use (one-time use)
- [x] Invalid refresh token requires re-login
- [x] Logout invalidates all tokens for that user

### Technical Notes

- Endpoint: `POST /api/v1/auth/refresh`
- Request: `{ "refreshToken": string }`
- Response: `{ "accessToken": string, "refreshToken": string }`
- Store refresh tokens in database for revocation capability
- Delete old refresh token immediately after rotation

### Security Considerations

- [x] Refresh tokens are one-time use
- [x] Detect token reuse attacks (if old token used, invalidate all)
- [x] Store hashed refresh tokens in database

---

## US-004: Password Recovery

**As an** admin who forgot my password  
**I want** to reset my password securely  
**So that** I can regain access to my account

### Acceptance Criteria

- [ ] "Forgot password" link on login page
- [ ] Request reset with email address
- [ ] Always show success message (prevent user enumeration)
- [ ] Email sent with secure reset link (if email exists)
- [ ] Reset link expires after 1 hour
- [ ] Reset link is single-use
- [ ] New password must meet strength requirements
- [ ] All existing sessions invalidated after reset

### Technical Notes

**Request Password Reset:**
- Endpoint: `POST /api/v1/auth/forgot-password`
- Request: `{ "email": string }`
- Response: `{ "message": "If an account exists, a reset link has been sent" }`
- Always return 200 OK (prevent enumeration)

**Reset Password:**
- Endpoint: `POST /api/v1/auth/reset-password`
- Request: `{ "token": string, "newPassword": string }`
- Response: `{ "message": "Password updated successfully" }`

### Data Model

```
PasswordResetToken {
  id: UUID
  token: String (unique, 64 chars, hashed in DB)
  userId: UUID (FK)
  createdAt: DateTime
  expiresAt: DateTime (createdAt + 1 hour)
  usedAt: DateTime (null until used)
}
```

### Email Template

```
Subject: Reset your password - The Replicant

You requested a password reset for your account.

Click here to reset: {FRONTEND_URL}/reset-password?token={TOKEN}

This link expires in 1 hour.

If you didn't request this, ignore this email.
```

### Security Considerations

- [ ] Token is cryptographically random (64 chars)
- [ ] Store only hashed token in database
- [ ] Rate limit: max 3 reset requests per email per hour
- [ ] Invalidate all existing sessions after password change
- [ ] Log password reset requests and completions

### Email Service Infrastructure

**Provider:** [Resend](https://resend.com) (Recommended for MVP)
- Free tier: 3,000 emails/month
- Simple REST API
- No credit card required



## US-005: Logout ✅

**As an** authenticated user  
**I want** to log out securely  
**So that** my session is terminated and no one else can access my account

### Acceptance Criteria

- [ ] Logout button in dashboard header (frontend pending)
- [x] All tokens invalidated on server
- [ ] Redirect to login page (frontend pending)
- [ ] Confirmation that logout was successful (frontend pending)

### Technical Notes

- Endpoint: `POST /api/v1/auth/logout`
- Auth: Required (Bearer token)
- Request: `{ "refreshToken": string }` (optional, to revoke specific token)
- Response: `{ "message": "Logged out successfully" }`
- Delete refresh token from database
- Add access token to blacklist (if not expired yet)

---

**Epic Status**: 🔄 In Progress (4/6 backend complete)  
**Created**: 2026-01-18  
**Last Updated**: 2026-02-11  
**Stories**: US-001 ✅, US-002a ✅, US-002 ⏳, US-003 ✅, US-004 ⏳, US-005 ✅
