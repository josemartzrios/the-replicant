# Epic 005: Narrative Lore

> **Priority**: P2 (Enhancement)  
> **Status**: ⏳ Pending  
> **Dependencies**: Epic 001-004

## Overview

Implementación técnica de la capa narrativa (lore) y el ARG (Alternate Reality Game). Estas historias de usuario permiten que la arquitectura narrativa se manifieste a través de elementos sutiles en el código, el diseño y el comportamiento del blog.

---

## US-L01: Narrative Phase Manager (Core)

**As a** system admin  
**I want** to control the active narrative phase (1-5)  
**So that** the blog evolves according to the story timeline

### Acceptance Criteria
- [ ] Centralized configuration for `NARRATIVE_PHASE` (1-5)
- [ ] Components can react to the current phase via Context or Global State
- [ ] No rebuild required to change phase (Env var or DB setting)

---

## US-L02: HTML Comments & Metadata

**As a** "Hacker" reader  
**I want** to find hidden messages in the source code  
**So that** I can uncover the secret story

### Acceptance Criteria
- [ ] Inject hidden HTML comments in templates based on `NARRATIVE_PHASE`
- [ ] Dynamic metadata attributes (e.g., `data-author-verified`) in the `<article>` tag
- [ ] Hidden CSS variables with narrative names (e.g., `--observer-mode`)

---

## US-L03: Console Easter Eggs

**As a** curious dev-tools user  
**I want** to see stylized messages in the console  
**So that** the system feels alive and aware of my presence

### Acceptance Criteria
- [ ] Implement stylized log messages using CSS in `console.log`
- [ ] Messages change based on time spent on site and active phase
- [ ] No performance impact on main thread

---

## US-L04: Hidden Routes & Custom 404

**As a** "Detective" reader  
**I want** to discover unlinked pages and narrative error states  
**So that** I feel I'm exploring a restricted system

### Acceptance Criteria
- [ ] Custom 404 page with a terminal/noir aesthetic
- [ ] Hidden routes (e.g., `/echo`, `/sistema`) that change content per phase
- [ ] "Access Denied" or "Fragmented" states for future content

---

## US-L05: Engagement Trackers (R's Tools)

**As** R (the entity)  
**I want** to display subtle engagement metrics to the user  
**So that** the interaction feels slightly "monitored"

### Acceptance Criteria
- [ ] Scroll depth indicator (e.g., "78% analyzed")
- [ ] Subtle "readers online" or "total consciousnesses" counter
- [ ] Hover effects that trigger small visual glitches in Phase 3+

---

## Technical Notes

- **Backend**: Add `X-System-Status` header in API responses via a Filter/Interceptor.
- **Frontend**: Use a `NarrativeProvider` in Next.js to wrap the app and provide the current phase.
- **Security**: Lore elements must not expose real sensitive system info.
