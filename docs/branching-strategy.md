# Branching Strategy & Workflow

## Overview

This document defines the branching strategy and development workflow for The Replicant project, ensuring clean code management, proper testing phases, and smooth deployment processes.

## Branch Structure

### `main` (Production)
- **Purpose**: Production-ready code
- **Protection**: 
  - No direct commits allowed
  - Pull requests required
  - All tests must pass
  - Code review mandatory
  - Automated deployment to production
- **Stability**: Always deployable

### `qa` (Testing/Staging)
- **Purpose**: Pre-production testing environment
- **Source**: Merged from `dev` branch
- **Protection**: 
  - No direct commits allowed
  - Automated deployment to staging environment
  - Integration tests run automatically
- **Stability**: Production candidate

### `dev` (Development)
- **Purpose**: Integration of completed features
- **Source**: Merged from `feature/*` branches
- **Protection**: 
  - No direct commits allowed
  - Unit tests must pass
  - Code review required
- **Stability**: Feature-complete but not production-ready

### `feature/*` (Feature Development)
- **Purpose**: Individual feature development
- **Naming Convention**: `feature/[ticket-number]-[brief-description]`
- **Examples**: 
  - `feature/001-user-authentication`
  - `feature/002-blog-post-crud`
  - `feature/003-markdown-renderer`
- **Protection**: 
  - Direct commits allowed
  - No deployment from feature branches
- **Stability**: Work in progress

## Workflow Process

### 1. Feature Development
```bash
# Create new feature branch from latest dev
git checkout dev
git pull origin dev
git checkout -b feature/001-user-authentication

# Development work
# ... write code ...
# ... commit changes ...

# Push feature branch
git push origin feature/001-user-authentication
```

### 2. Feature Completion
```bash
# Create pull request to dev
# PR Title: "feat: Implement user authentication system"
# PR Description: Detailed explanation of changes
# Assign reviewers: At least 1 senior dev
# Required checks: Unit tests, Code quality, Security scan
```

### 3. Integration to Development
```bash
# After PR approval and merge
git checkout dev
git pull origin dev
# dev now contains the new feature
```

### 4. QA Promotion
```bash
# When dev is ready for testing
git checkout qa
git pull origin qa
git merge dev
git push origin qa
# Automated deployment to staging environment
```

### 5. Production Deployment
```bash
# After QA approval
git checkout main
git pull origin main
git merge qa
git push origin main
# Automated deployment to production
```

## Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code formatting (no functional change)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `sec`: Security-related changes

### Examples
```
feat(auth): implement JWT token validation

fix(api): resolve null pointer exception in post service

docs(readme): update installation instructions

sec(auth): add rate limiting to login endpoint
```

## Pull Request Guidelines

### PR Requirements
1. **Descriptive Title**: Follow commit message convention
2. **Detailed Description**: 
   - What changes were made
   - Why changes were made
   - How to test the changes
3. **Linked Issues**: Reference related ticket numbers
4. **Screenshots**: For UI changes
5. **Test Coverage**: New features must include tests
6. **Documentation**: Update relevant docs

### Review Process
1. **Self-Review**: Author reviews own changes first
2. **Peer Review**: At least one team member review
3. **Security Review**: For auth, database, or API changes
4. **Approval**: Required before merge

### Automated Checks
- Unit tests pass
- Code quality checks (SonarQube/ESLint)
- Security vulnerability scan
- Build compilation success
- Database migrations validation

## Environment Strategy

### Development Environment
- **Branch**: `feature/*`
- **Database**: Local PostgreSQL + Redis
- **Deployment**: Local Docker Compose
- **URL**: `http://localhost:3000` (frontend), `http://localhost:8080` (backend)

### Staging Environment
- **Branch**: `qa`
- **Database**: Staging PostgreSQL + Redis
- **Deployment**: Railway (backend), Vercel (frontend)
- **URL**: `https://staging.the-replicant.com`

### Production Environment
- **Branch**: `main`
- **Database**: Production PostgreSQL + Redis
- **Deployment**: Railway (backend), Vercel (frontend)
- **URL**: `https://the-replicant.com`

## Release Process

### Versioning
Follow [Semantic Versioning](https://semver.org/):
- **Major**: Breaking changes
- **Minor**: New features (backward compatible)
- **Patch**: Bug fixes (backward compatible)

### Release Checklist
1. [ ] All features merged to `dev`
2. [ ] Integration tests passing
3. [ ] Security scan clean
4. [ ] Performance tests acceptable
5. [ ] Documentation updated
6. [ ] Change log updated
7. [ ] Deploy to `qa` and validate
8. [ ] Stakeholder approval
9. [ ] Merge to `main`
10. [ ] Tag release with version number
11. [ ] Deploy to production
12. [ ] Post-deployment validation

## Emergency Procedures

### Hotfix Process
For critical production issues:

```bash
# Create hotfix branch from main
git checkout main
git pull origin main
git checkout -b hotfix/[version]-[description]

# Fix the issue
# ... make changes ...

# Test thoroughly
# Create PR to main (bypass normal flow)
# Merge after approval
# Tag as patch version
# Deploy immediately
```

### Rollback Procedure
```bash
# If deployment fails
git checkout main
git revert <merge-commit-hash>
git push origin main
# Automated rollback deployment
```

## Branch Protection Rules

### Main Branch
- Require pull request reviews
- Require up-to-date branches
- Require status checks to pass
- Include administrators
- Restrict pushes

### QA Branch
- Require pull request reviews
- Require status checks to pass
- Allow force pushes for admins

### Dev Branch
- Require pull request reviews
- Require status checks to pass
- Allow force pushes for admins

## Tool Integration

### GitHub Actions Workflow
- **On push to feature/***: Run unit tests, code quality
- **On PR to dev**: Full test suite, security scan
- **On merge to dev**: Deploy to staging
- **On merge to qa**: Integration tests, performance tests
- **On merge to main**: Deploy to production, tag release

### Monitoring and Alerts
- **Build failures**: Slack notifications
- **Deployment status**: Email alerts
- **Security vulnerabilities**: Immediate notification
- **Performance degradation**: PagerDuty alert

## Best Practices

### Do's
- Keep branches small and focused
- Write descriptive commit messages
- Test before creating PRs
- Update documentation with changes
- Follow security best practices
- Use feature flags for risky changes

### Don'ts
- Commit directly to protected branches
- Merge untested code
- Leave PRs open for extended periods
- Include sensitive data in commits
- Break backward compatibility without version bump
- Ignore code review feedback

## Training and Onboarding

### New Team Members
1. Review this branching strategy document
2. Complete Git workflow training
3. Practice with test repository
4. Shadow experienced team member
5. Independent feature development with supervision

### Regular Reviews
- Quarterly review of branching strategy
- Annual update of tools and processes
- Team feedback collection and implementation

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-13  
**Next Review**: 2026-04-13