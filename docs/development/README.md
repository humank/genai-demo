# Development Documentation

> **Last Updated**: 2025-01-17

## Overview

This section contains comprehensive development documentation for the GenAI Demo e-commerce platform, including setup guides, coding standards, testing strategies, workflows, and examples.

## Quick Navigation

### 🚀 Getting Started

- [Development Setup](setup/README.md) - Environment setup and prerequisites
- [IDE Configuration](setup/ide-configuration.md) - IDE setup and plugins
- [Onboarding Guide](setup/onboarding.md) - New developer onboarding

### 📝 Coding Standards

- [Java Standards](coding-standards/java-standards.md) - Java coding conventions

### 🧪 Testing

- [Testing Strategy](testing/testing-strategy.md) - Overall testing approach
- [Unit Testing](testing/unit-testing.md) - Unit test guidelines
- [Integration Testing](testing/integration-testing.md) - Integration test guidelines
- [BDD Testing](testing/bdd-testing.md) - Behavior-driven development

### 🔄 Workflows

- [Git Workflow](workflows/git-workflow.md) - Branching and merging strategy
- [Code Review Process](workflows/code-review.md) - Review procedures

### 💡 Examples

- [Creating an Aggregate](examples/creating-aggregate.md) - DDD aggregate example
- [Implementing Events](examples/implementing-event.md) - Domain event example

### 🔧 Tools & Hooks

- [Diagram Generation](hooks/diagram-hooks-design.md) - Diagram automation

## Development Environment

### Prerequisites

- **Java**: JDK 21 or higher
- **Node.js**: v18 or higher (for frontend)
- **Docker**: Latest version
- **Gradle**: 8.x (wrapper included)
- **Git**: Latest version
- **IDE**: IntelliJ IDEA or VS Code

### Quick Start

```bash
# Clone repository
git clone https://github.com/company/genai-demo.git
cd genai-demo

# Setup environment
./scripts/setup-dev-environment.sh

# Run application
./gradlew bootRun

# Run tests
./gradlew test
```

[Detailed Setup Guide](setup/README.md)

## Architecture Overview

### Hexagonal Architecture

The application follows hexagonal architecture (ports and adapters):

```
src/
├── main/
│   └── java/
│       └── solid/humank/genaidemo/
│           ├── domain/          # Business logic (no dependencies)
│           ├── application/     # Use cases (depends on domain)
│           ├── infrastructure/  # Technical implementations
│           └── interfaces/      # API controllers
```

[Architecture Guide](../viewpoints/development/README.md)

### Domain-Driven Design

We follow DDD tactical patterns:

- **Aggregates**: Consistency boundaries
- **Entities**: Objects with identity
- **Value Objects**: Immutable objects
- **Domain Events**: Business events
- **Repositories**: Data access interfaces
- **Domain Services**: Cross-aggregate logic

[DDD Patterns Guide](../architecture/patterns/ddd-patterns.md)

## Coding Standards

### Java Coding Standards

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Java 21 features (records, pattern matching, etc.)
- Write self-documenting code
- Use meaningful variable names
- Keep methods small (< 20 lines)

[Full Java Standards](coding-standards/java-standards.md)

### Code Quality Tools

- **Checkstyle**: Code style checking
- **PMD**: Code quality analysis
- **SpotBugs**: Bug detection
- **SonarQube**: Comprehensive code analysis
- **ArchUnit**: Architecture testing

```bash
# Run code quality checks
./gradlew check
./gradlew pmdMain
./gradlew spotbugsMain
```

## Testing Strategy

### Test Pyramid

- **Unit Tests (80%)**: Fast, isolated tests
- **Integration Tests (15%)**: Component integration
- **E2E Tests (5%)**: Complete user journeys

### Test Commands

```bash
# Run all tests
./gradlew test

# Run unit tests only
./gradlew unitTest

# Run integration tests
./gradlew integrationTest

# Run E2E tests
./gradlew e2eTest

# Run BDD tests
./gradlew cucumber

# Generate coverage report
./gradlew jacocoTestReport
```

[Testing Strategy Guide](testing/testing-strategy.md)

### Test Coverage Requirements

- **Minimum Coverage**: 80% line coverage
- **Critical Paths**: 100% coverage
- **New Code**: Must have tests
- **Bug Fixes**: Must include regression test

## Development Workflows

### Git Workflow

We use **Git Flow** with the following branches:

- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: Feature development
- `bugfix/*`: Bug fixes
- `hotfix/*`: Production hotfixes
- `release/*`: Release preparation

[Git Workflow Guide](workflows/git-workflow.md)

### Code Review Process

1. **Create PR**: From feature branch to develop
2. **Automated Checks**: CI/CD runs tests
3. **Code Review**: At least 2 reviewers
4. **Address Feedback**: Make requested changes
5. **Approval**: Get approvals from reviewers
6. **Merge**: Squash and merge to develop

[Code Review Guide](workflows/code-review.md)

### CI/CD Pipeline

Our CI/CD pipeline includes:

1. **Build**: Compile and package
2. **Test**: Run all tests
3. **Quality**: Code quality checks
4. **Security**: Security scanning
5. **Deploy**: Deploy to environment

## Development Best Practices

### DDD Best Practices

1. **Ubiquitous Language**: Use domain terminology
2. **Bounded Contexts**: Clear context boundaries
3. **Aggregate Design**: Small, focused aggregates
4. **Event-Driven**: Use domain events for communication
5. **Repository Pattern**: Abstract data access

### Clean Code Principles

1. **SOLID Principles**: Follow SOLID design
2. **DRY**: Don't Repeat Yourself
3. **KISS**: Keep It Simple, Stupid
4. **YAGNI**: You Aren't Gonna Need It
5. **Boy Scout Rule**: Leave code better than you found it

### Performance Best Practices

1. **Database Optimization**: Use indexes, avoid N+1
2. **Caching**: Cache frequently accessed data
3. **Async Processing**: Use async for long operations
4. **Connection Pooling**: Configure properly
5. **Monitoring**: Monitor performance metrics

## Common Development Tasks

### Creating a New Feature

1. **Create Feature Branch**: `git checkout -b feature/my-feature`
2. **Write BDD Scenarios**: Define behavior in Gherkin
3. **Implement Domain Logic**: Start with domain layer
4. **Add Tests**: Write unit and integration tests
5. **Implement API**: Add REST endpoints
6. **Update Documentation**: Document changes
7. **Create PR**: Submit for review

[Feature Development Guide](examples/creating-aggregate.md)

### Implementing Domain Events

1. **Define Event**: Create event record
2. **Collect in Aggregate**: Use `collectEvent()`
3. **Publish in Service**: Use `DomainEventApplicationService`
4. **Handle Event**: Create event handler
5. **Test**: Write event tests

[Event Implementation Guide](examples/implementing-event.md)

### Adding API Endpoint

1. **Design Endpoint**: Follow REST principles
2. **Create DTO**: Request/response objects
3. **Implement Controller**: REST controller
4. **Add Validation**: Input validation
5. **Write Tests**: API tests
6. **Document**: Update API docs

## Troubleshooting

### Common Issues

#### Build Failures

```bash
# Clean and rebuild
./gradlew clean build

# Clear Gradle cache
rm -rf ~/.gradle/caches
```

#### Test Failures

```bash
# Run specific test
./gradlew test --tests "ClassName.testMethod"

# Run with debug logging
./gradlew test --debug
```

#### IDE Issues

- **IntelliJ**: File → Invalidate Caches / Restart
- **VS Code**: Reload window (Cmd+Shift+P → Reload Window)

[Troubleshooting Guide](../operations/troubleshooting/README.md)

## Resources

### Documentation

- [Architecture Documentation](../viewpoints/README.md)
- [API Documentation](../api/README.md)
- [Operations Documentation](../operations/README.md)

### External Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

### Training Materials

## Contributing

### Contributing Guidelines

1. Follow coding standards
2. Write tests for new code
3. Update documentation
4. Submit PR with clear description
5. Respond to review feedback

### Documentation Updates

1. Follow [style guide](../STYLE-GUIDE.md)
2. Use templates from [templates](../templates/)
3. Keep documentation current
4. Add examples where helpful

## Support

### Getting Help

- **Slack**: #dev-support
- **Email**: dev-team@company.com
- **Wiki**: Internal development wiki
- **Office Hours**: Tuesday 2-3 PM

### Reporting Issues

1. Check existing issues
2. Provide reproduction steps
3. Include error messages
4. Add relevant logs

---

**Document Owner**: Development Team
**Last Review**: 2025-01-17
**Next Review**: 2025-04-17
**Status**: Active
