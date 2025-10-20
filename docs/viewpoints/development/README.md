# Development Viewpoint

> **Status**: 📝 To be documented  
> **Last Updated**: 2025-01-17  
> **Owner**: Tech Lead

## Overview

The Development Viewpoint describes the code organization, module structure, build process, and development environment.

## Purpose

This viewpoint answers:
- How is the code organized?
- What are the module dependencies?
- How is the system built and tested?
- What tools do developers need?

## Stakeholders

- **Primary**: Developers, build engineers
- **Secondary**: DevOps, architects

## Contents

### 📄 Documents
- [Overview](overview.md) - Code organization approach
- [Module Organization](module-organization.md) - Package structure and bounded contexts
- [Dependency Rules](dependency-rules.md) - Hexagonal architecture constraints
- [Build Process](build-process.md) - Gradle build and test execution

### 📊 Diagrams
- Package structure diagram
- Dependency diagram
- Build pipeline diagram

## Key Concepts

### Code Organization
```
app/src/main/java/solid/humank/genaidemo/
├── domain/              # Domain layer (no external dependencies)
│   ├── customer/       # Customer bounded context
│   ├── order/          # Order bounded context
│   └── ...
├── application/        # Application services (use cases)
├── infrastructure/     # Infrastructure adapters
└── interfaces/         # API controllers, event handlers
```

### Dependency Rules
- Domain layer: No dependencies on other layers
- Application layer: Depends only on domain
- Infrastructure layer: Depends on domain (via interfaces)
- Interface layer: Depends on application

### Build Tools
- **Build System**: Gradle 8.x
- **Java Version**: Java 21
- **Testing**: JUnit 5, Mockito, Cucumber
- **Code Quality**: ArchUnit, JaCoCo

## Related Documentation

### Related Viewpoints
- [Functional Viewpoint](../functional/README.md) - Bounded contexts
- [Deployment Viewpoint](../deployment/README.md) - Build artifacts

### Related Perspectives
- [Evolution Perspective](../../perspectives/evolution/README.md) - Code maintainability

### Related Guides
- [Development Guide](../../development/README.md) - Detailed development instructions
- [Coding Standards](../../development/coding-standards/README.md)

## Quick Links

- [Back to All Viewpoints](../README.md)
- [Main Documentation](../../README.md)
