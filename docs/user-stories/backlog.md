# MVP Backlog

> **Total Stories**: 25  
> **Estimated Sprints**: 3 + Phase 4 (Narrative)

## Priority Matrix

### P0 - Must Have (MVP Launch Blockers)

| ID | Story | Epic | Sprint | Points |
|----|-------|------|--------|--------|
| US-001 | Admin Login | E001 | 1 | 5 |
| US-002a | First Admin Setup | E001 | 1 | 3 |
| US-002 | Create User (Admin) | E001 | 1 | 5 |
| US-003 | Token Refresh | E001 | 1 | 3 |
| US-004 | Password Recovery | E001 | 1 | 5 |
| US-005 | Logout | E001 | 1 | 2 |
| US-006 | Create Blog Post (Admin) | E002 | 2 | 8 |
| US-007 | Edit Blog Post (Admin) | E002 | 2 | 5 |
| US-008 | Delete Blog Post (Admin) | E002 | 2 | 2 |
| US-009 | List All Posts (Admin) | E002 | 2 | 5 |
| US-010 | View Post List (Public) | E003 | 2 | 5 |
| US-011 | Read Single Post (Public) | E003 | 2 | 5 |

**Total P0**: 53 story points

### P1 - Should Have (MVP Enhancement)

| ID | Story | Epic | Sprint | Points |
|----|-------|------|--------|--------|
| US-012 | Search Posts (Public) | E003 | 3 | 5 |
| US-013 | Browse by Category (Public) | E004 | 3 | 3 |
| US-014 | Browse by Tag (Public) | E004 | 3 | 3 |
| US-015 | Manage Categories (Admin) | E004 | 3 | 3 |

**Total P1**: 14 story points

### P2 - Narrative Lore (Post-MVP Enrichment)

| ID | Story | Epic | Phase | Points |
|----|-------|------|-------|--------|
| US-L01 | Narrative Phase Manager (Core) | E005 | 5 | 5 |
| US-L02 | HTML Comments & Metadata | E005 | 5 | 2 |
| US-L03 | Console Easter Eggs | E005 | 5 | 2 |
| US-L04 | Hidden Routes & Custom 404 | E005 | 5 | 3 |
| US-L05 | Engagement Trackers | E005 | 5 | 3 |
| US-L06 | Custom 404 Page (Narrative) | E005 | 5 | 2 |
| US-L09 | Narrative Phase Manager Admin | E005 | 5 | 3 |

**Total P2**: 20 story points

---

## Sprint Allocation

### Sprint 1: Authentication (Week 2)
- US-001: Admin Login (5 pts)
- US-002a: First Admin Setup (3 pts)
- US-002: Create User - Admin (5 pts)
- US-003: Token Refresh (3 pts)
- US-004: Password Recovery (5 pts)
- US-005: Logout (2 pts)
- **Sprint Total**: 23 points

### Sprint 2: Core Blog (Week 3)
- US-006: Create Blog Post (8 pts)
- US-007: Edit Blog Post (5 pts)
- US-008: Delete Blog Post (2 pts)
- US-009: List All Posts (5 pts)
- US-010: View Post List (5 pts)
- US-011: Read Single Post (5 pts)
- **Sprint Total**: 30 points

### Sprint 3: Polish & Organization (Week 4)
- US-012: Search Posts (5 pts)
- US-013: Browse by Category (3 pts)
- US-014: Browse by Tag (3 pts)
- US-015: Manage Categories (3 pts)
- **Sprint Total**: 14 points

---

## Definition of Done

A story is complete when:

- [ ] All acceptance criteria met
- [ ] Code reviewed and approved
- [ ] Unit tests passing (≥80% coverage for new code)
- [ ] Integration tests passing
- [ ] No critical/high security vulnerabilities
- [ ] Documentation updated (if applicable)
- [ ] Deployed to staging and verified
- [ ] Merged to `dev` branch

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| JWT security issues | High | Follow OWASP guidelines, security review |
| Invite code exposure | High | Cryptographic random, short expiry, single-use |
| Password reset vulnerabilities | High | Hashed tokens, rate limiting, timing-safe comparison |
| Markdown XSS vulnerabilities | High | Use sanitization library, CSP headers |
| Slow search performance | Medium | PostgreSQL full-text search, add index |
| Email delivery failures | Medium | Use reliable service (SendGrid/Resend), retry logic |

---

**Backlog Version**: 1.1  
**Created**: 2026-01-18  
**Last Updated**: 2026-01-19
