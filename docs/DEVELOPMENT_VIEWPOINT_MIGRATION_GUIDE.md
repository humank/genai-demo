# Development Viewpoint Migration Guide

> **Complete Development Documentation Migration Guide and Bookmark Update Instructions**

## 📋 Migration Overview

To provide a more systematic and professional development documentation experience, we have integrated development-related documents scattered across multiple directories into a unified **Development Viewpoint** structure.

### Migration Scope

- **`docs/development/`** → **`docs/viewpoints/development/`**
- **`docs/design/`** → **`docs/viewpoints/development/architecture/`**
- **`docs/testing/`** → **`docs/viewpoints/development/testing/`**

## 🔗 Complete Migration Mapping Table

### Development Guide Migration

| Original Path | New Path | Status |
|---------------|----------|--------|
| `docs/development/README.md` | `docs/viewpoints/development/README.md` | ✅ Migrated |
| `docs/development/getting-started.md` | `docs/viewpoints/development/getting-started/README.md` | ✅ Migrated |
| `docs/development/coding-standards.md` | `docs/viewpoints/development/coding-standards/README.md` | ✅ Migrated |
| `docs/development/testing-guide.md` | `docs/viewpoints/development/testing/README.md` | ✅ Migrated |
| `docs/development/documentation-guide.md` | `docs/viewpoints/development/coding-standards/documentation-standards.md` | ✅ Migrated |
| `docs/development/instructions.md` | `docs/viewpoints/development/workflows/development-workflow.md` | ✅ Migrated |
| `docs/development/epic.md` | `docs/viewpoints/development/workflows/epic-implementation.md` | ✅ Migrated |

### Design Documentation Migration

| Original Path | New Path | Status |
|---------------|----------|--------|
| `docs/design/README.md` | `docs/viewpoints/development/architecture/README.md` | ✅ Migrated |
| `docs/design/ddd-guide.md` | `docs/viewpoints/development/architecture/ddd-patterns/tactical-patterns.md` | ✅ Migrated |
| `docs/design/design-principles.md` | `docs/viewpoints/development/architecture/design-principles/solid-principles.md` | ✅ Migrated |
| `docs/design/refactoring-guide.md` | `docs/viewpoints/development/workflows/refactoring-strategy.md` | ✅ Migrated |

### Testing Documentation Migration

| Original Path | New Path | Status |
|---------------|----------|--------|
| `docs/testing/README.md` | `docs/viewpoints/development/testing/README.md` | ✅ Migrated |
| `docs/testing/test-performance-monitoring.md` | `docs/viewpoints/development/testing/performance-monitoring/test-performance-extension.md` | ✅ Migrated |
| `docs/testing/test-optimization-guidelines.md` | `docs/viewpoints/development/testing/test-optimization.md` | ✅ Migrated |
| `docs/testing/http-client-configuration-guide.md` | `docs/viewpoints/development/testing/integration-testing.md` | ✅ Migrated |
| `docs/testing/new-developer-onboarding-guide.md` | `docs/viewpoints/development/getting-started/first-contribution.md` | ✅ Migrated |

## 📚 New Development Viewpoint Structure

```
docs/viewpoints/development/
├── README.md                           # Development viewpoint overview
├── getting-started/                    # Quick start layer
│   ├── README.md                      # Getting started guide overview
│   ├── environment-setup.md           # Environment configuration guide
│   ├── prerequisites.md               # Prerequisites checklist
│   ├── first-contribution.md          # First contribution guide
│   └── quickstart-checklist.md       # Quick start checklist
├── architecture/                      # Architecture design layer
│   ├── README.md                      # Architecture guide overview
│   ├── ddd-patterns/                  # DDD patterns subdirectory
│   │   ├── README.md                  # DDD patterns overview
│   │   ├── tactical-patterns.md       # Tactical patterns: @AggregateRoot, @ValueObject, @Entity, @DomainService
│   │   ├── strategic-patterns.md      # Strategic patterns: Bounded Context, Context Mapping
│   │   ├── domain-events.md           # Domain events: Record implementation, event collection and publishing
│   │   └── aggregate-design.md        # Aggregate design principles and best practices
│   ├── hexagonal-architecture/        # Hexagonal architecture subdirectory
│   │   ├── README.md                  # Hexagonal architecture overview
│   │   ├── ports-adapters.md          # Port-Adapter pattern implementation
│   │   ├── dependency-inversion.md    # Dependency inversion principle application
│   │   ├── layered-design.md          # Layered design and boundary definition
│   │   └── integration-patterns.md    # Integration patterns and adapter design
│   ├── microservices/                 # Microservices architecture subdirectory
│   │   ├── README.md                  # Microservices architecture overview
│   │   ├── service-design.md          # Service design principles
│   │   ├── api-gateway.md             # API Gateway pattern
│   │   ├── service-discovery.md       # Service discovery mechanism
│   │   ├── load-balancing.md          # Load balancing strategy
│   │   ├── circuit-breaker.md         # Circuit breaker pattern
│   │   └── distributed-patterns.md    # Distributed system patterns
│   ├── saga-patterns/                 # Saga patterns subdirectory
│   │   ├── README.md                  # Saga patterns overview
│   │   ├── orchestration.md           # Orchestration-based Saga
│   │   ├── choreography.md            # Choreography-based Saga
│   │   ├── order-processing-saga.md   # Order processing Saga implementation
│   │   ├── payment-saga.md            # Payment Saga implementation
│   │   └── saga-coordination.md       # Saga coordination mechanism
│   └── design-principles/             # Design principles subdirectory
│       └── solid-principles.md        # SOLID principles and design patterns
├── coding-standards/                  # Coding standards layer
│   ├── README.md                      # Coding standards overview
│   ├── java-standards.md              # Java coding standards
│   ├── frontend-standards.md          # Frontend coding standards (React/Angular)
│   ├── api-design.md                  # API design standards
│   ├── documentation-standards.md     # Documentation writing standards
│   ├── naming-conventions.md          # Naming conventions
│   └── code-review-guidelines.md      # Code review guidelines
├── testing/                           # Testing strategy layer
│   ├── README.md                      # Testing strategy overview
│   ├── tdd-practices/                 # TDD practices subdirectory
│   │   ├── README.md                  # TDD practices overview
│   │   ├── red-green-refactor.md      # Red-Green-Refactor cycle
│   │   ├── test-pyramid.md            # Test pyramid strategy
│   │   └── unit-testing-patterns.md   # Unit testing patterns
│   ├── bdd-practices/                 # BDD practices subdirectory
│   │   ├── README.md                  # BDD practices overview
│   │   ├── gherkin-guidelines.md      # Gherkin syntax guide
│   │   ├── given-when-then.md         # Given-When-Then pattern
│   │   ├── feature-writing.md         # Feature file writing
│   │   └── scenario-design.md         # Scenario design best practices
│   ├── performance-monitoring/        # Performance monitoring subdirectory
│   │   └── test-performance-extension.md  # @TestPerformanceExtension usage guide
│   ├── integration-testing.md         # Integration testing guide
│   ├── architecture-testing.md        # Architecture testing: ArchUnit rules
│   ├── test-optimization.md           # Test optimization guide
│   └── test-automation.md             # Test automation strategy
├── build-system/                      # Build system layer
│   ├── README.md                      # Build system overview
│   ├── gradle-configuration.md        # Gradle configuration guide
│   ├── multi-module-setup.md          # Multi-module setup
│   ├── dependency-management.md       # Dependency management strategy
│   ├── build-optimization.md          # Build optimization techniques
│   └── ci-cd-integration.md           # CI/CD integration configuration
├── quality-assurance/                 # Quality assurance layer
│   ├── README.md                      # Quality assurance overview
│   ├── code-review.md                 # Code review process
│   ├── static-analysis.md             # Static analysis tools
│   ├── security-scanning.md           # Security scanning configuration
│   ├── performance-monitoring.md      # Performance monitoring setup
│   └── quality-gates.md               # Quality gate standards
├── tools-and-environment/             # Toolchain layer
│   ├── README.md                      # Toolchain overview
│   ├── technology-stack/              # Technology stack subdirectory
│   │   ├── README.md                  # Technology stack overview
│   │   ├── backend-stack.md           # Spring Boot 3.4.5 + Java 21 + Gradle 8.x
│   │   ├── frontend-stack.md          # Next.js 14 + React 18 + Angular 18 + TypeScript
│   │   ├── testing-stack.md           # JUnit 5 + Mockito + AssertJ + Cucumber 7
│   │   ├── database-stack.md          # H2 (dev/test) + PostgreSQL (prod) + Flyway
│   │   ├── monitoring-stack.md        # Spring Boot Actuator + AWS X-Ray + Micrometer
│   │   └── infrastructure-stack.md    # AWS CDK + EKS + MSK + Route 53
│   ├── ide-configuration.md           # IDE configuration guide
│   ├── version-control.md             # Git workflow and best practices
│   ├── debugging-tools.md             # Debugging tools configuration
│   └── development-tools.md           # Development toolchain integration
└── workflows/                         # Workflow layer
    ├── README.md                      # Workflow overview
    ├── development-workflow.md         # Development process standards
    ├── release-process.md              # Release process management
    ├── hotfix-process.md               # Hotfix process
    ├── refactoring-strategy.md         # Refactoring strategy guide
    └── collaboration-guidelines.md     # Team collaboration guidelines
```

## 🔖 Bookmark Update Guide

### Browser Bookmark Updates

If you have the following bookmarks, please update them to the new paths:

#### Development Guide Bookmarks
```
Old Bookmark: docs/development/README.md
New Bookmark: docs/viewpoints/development/README.md

Old Bookmark: docs/development/getting-started.md
New Bookmark: docs/viewpoints/development/getting-started/README.md

Old Bookmark: docs/development/coding-standards.md
New Bookmark: docs/viewpoints/development/coding-standards/README.md

Old Bookmark: docs/development/testing-guide.md
New Bookmark: docs/viewpoints/development/testing/README.md
```

#### Design Documentation Bookmarks
```
Old Bookmark: docs/design/ddd-guide.md
New Bookmark: docs/viewpoints/development/architecture/ddd-patterns/tactical-patterns.md

Old Bookmark: docs/design/design-principles.md
New Bookmark: docs/viewpoints/development/architecture/design-principles/solid-principles.md

Old Bookmark: docs/design/refactoring-guide.md
New Bookmark: docs/viewpoints/development/workflows/refactoring-strategy.md
```

#### Testing Documentation Bookmarks
```
Old Bookmark: docs/testing/README.md
New Bookmark: docs/viewpoints/development/testing/README.md

Old Bookmark: docs/testing/test-performance-monitoring.md
New Bookmark: docs/viewpoints/development/testing/performance-monitoring/test-performance-extension.md

Old Bookmark: docs/testing/test-optimization-guidelines.md
New Bookmark: docs/viewpoints/development/testing/test-optimization.md
```

### IDE Bookmarks and Quick Access

If you have set up quick access or bookmarks in your IDE, please update the paths:

#### VS Code Workspace Settings
```json
{
  "folders": [
    {
      "name": "Development Docs",
      "path": "./docs/viewpoints/development"
    }
  ]
}
```

#### IntelliJ IDEA Bookmarks
- Remove old `docs/development/` bookmarks
- Add new `docs/viewpoints/development/` bookmarks

## 📝 External Reference Handling

### Documentation Link Updates

If you have referenced old paths in other documents, please update:

```markdown
<!-- Old references -->
Development Guide
DDD Guide
Testing Guide

<!-- New references -->
Development Guide
DDD Guide
Testing Guide
```

### Wiki and External Documentation

If you have referenced these documents in Wiki, Confluence, or other external systems:

1. **Update all links** to new paths
2. **Check embedded documents** if they need updates
3. **Notify team members** about path changes

## 🔄 Transition Period Support

### Redirect Documentation

During the transition period (until end of February 2025), README.md files in old directories will provide:

- **Clear migration notices**
- **Direct links to new locations**
- **Complete mapping tables**
- **Quick navigation guides**

### Automatic Redirects

We have set up redirect README files in old directories:

- `docs/development/README.md` - Points to new development viewpoint
- `docs/design/README.md` - Points to new architecture patterns
- `docs/testing/README.md` - Points to new testing strategy

## 🆘 Need Help?

### Frequently Asked Questions

**Q: I can't find a specific document, what should I do?**
A: Please refer to the complete mapping table above, or check the Development Viewpoint overview

**Q: Do old links still work?**
A: During the transition period (until end of February 2025), README files in old directories will provide redirect guidance

**Q: What are the advantages of the new structure?**
A: More systematic organization, more complete content, better maintainability, and compliance with Rozanski & Woods architecture methodology

### Contact Support

If you encounter issues during migration:

1. **Check redirect documents** - README.md files in old directories
2. **Refer to mapping table** - Complete mapping table in this document
3. **Check new structure** - Development Viewpoint overview
4. **Raise issues** - Create an Issue in the project

---

**Migration Completion Date**: January 21, 2025  
**Transition Period End**: February 28, 2025  
**Old Directory Removal**: March 1, 2025

**Thank you for your cooperation!** The new Development Viewpoint structure will provide you with a better development documentation experience.