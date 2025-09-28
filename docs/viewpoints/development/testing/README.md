# Testing Strategy and Practices

## Overview

This directory contains testing-related documentation within the development viewpoint, covering Test-Driven Development (TDD), Behavior-Driven Development (BDD), and various testing practices.

## Directory Structure

- **[tdd-practices/](tdd-practices/)** - Test-driven development practices and guides
- **[bdd-practices/](bdd-practices/)** - Behavior-driven development practices and Gherkin syntax

## Core Documentation

- **[TDD/BDD Testing](tdd-bdd-testing.md)** - Test-driven development and behavior-driven development guide

## Test Pyramid

We follow the test pyramid principle:

```
    /\
   /E2E\     <- 5%  End-to-End Tests
  /______\
 /        \
/Integration\ <- 15% Integration Tests  
\____________/
\            /
 \   Unit   /  <- 80% Unit Tests
  \________/
```

## Testing Standards

### Performance Benchmarks
- **Unit Tests**: < 50ms, < 5MB, success rate > 99%
- **Integration Tests**: < 500ms, < 50MB, success rate > 95%
- **End-to-End Tests**: < 3s, < 500MB, success rate > 90%

### Test Categories
- `@UnitTest` - Pure business logic tests
- `@IntegrationTest` - Database and external service integration tests
- `@SlowTest` - Long-running tests
- `@SmokeTest` - Smoke tests

## Testing Tools

- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Assertion library
- **Cucumber** - BDD testing framework
- **TestContainers** - Integration testing containers

## Related Resources

- [Test Performance Standards](../../../../.kiro/steering/test-performance-standards.md)
- [BDD/TDD Principles](../../../../.kiro/steering/bdd-tdd-principles.md)
- [Code Review Standards](../../../../.kiro/steering/code-review-standards.md)

---

**Maintainer**: Development Team  
**Last Updated**: January 21, 2025  
**Version**: 1.0
![Microservices Overview](../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)