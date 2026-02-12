# Smoke Test - Auth Endpoints in bash terminal
# Run these curls in order after starting the server with: mvnw spring-boot:run
# Base URL: http://localhost:8080

# ===========================================
# 1. Check if setup is required
# Expected: { "setupRequired": true }
# ===========================================
curl -s http://localhost:8080/api/v1/auth/status | json_pp

# ===========================================
# 2. Create first admin (one-time setup)
# Expected: 201 with accessToken, refreshToken, user
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@thereplicant.com","password":"SecurePass123!","name":"Admin User"}' | json_pp

# ===========================================
# 3. Verify setup is no longer required
# Expected: { "setupRequired": false }
# ===========================================
curl -s http://localhost:8080/api/v1/auth/status | json_pp

# ===========================================
# 4. Attempt setup again (must fail)
# Expected: 409 Conflict
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/setup \
  -H "Content-Type: application/json" \
  -d '{"email":"hacker@evil.com","password":"HackPass123!","name":"Hacker"}' | json_pp

# ===========================================
# 5. Login with correct credentials
# Expected: 200 with accessToken, refreshToken, user
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@thereplicant.com","password":"SecurePass123!"}' | json_pp

# ===========================================
# 6. Login with wrong password
# Expected: 401 Authentication Failed
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@thereplicant.com","password":"WrongPassword!"}' | json_pp

# ===========================================
# 7. Login with non-existent email
# Expected: 401 (same error as wrong password - anti-enumeration)
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"unknown@test.com","password":"SomePass123!"}' | json_pp

# ===========================================
# 8. Refresh token (replace YOUR_REFRESH_TOKEN with token from step 5)
# Expected: 200 with new accessToken and refreshToken
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}' | json_pp

# ===========================================
# 9. Reuse same refresh token (must fail - token rotation)
# Expected: 401 (token already revoked)
# ===========================================
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}' | json_pp
