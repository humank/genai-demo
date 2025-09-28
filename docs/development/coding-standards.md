# Coding Standards

This document provides coding standards and guidelines for the project. For comprehensive details, please refer to our steering rules documentation.

## Quick Reference

### Core Standards
- **Development Standards** - Technology stack, testing frameworks, and code quality requirements
- **Code Review Standards** - Review process, quality gates, and feedback guidelines  
- **Security Standards** - Security implementation, authentication, and data protection

### Key Principles

#### Code Quality
- Follow SOLID principles and DDD tactical patterns
- Maintain test coverage > 80%
- Use meaningful naming conventions
- Keep methods focused and concise

#### Architecture Compliance
- Respect layer dependency rules: `interfaces/ → application/ → domain/ ← infrastructure/`
- Use proper aggregate boundaries and domain events
- Implement hexagonal architecture patterns

#### Testing Standards
- Unit Tests: < 50ms, < 5MB memory usage
- Integration Tests: < 500ms, < 50MB memory usage
- Follow BDD/TDD principles with Given-When-Then structure

## Implementation Guidelines

For detailed implementation guidelines, examples, and best practices, please refer to the comprehensive steering rules documentation linked above.