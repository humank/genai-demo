# 📍 Development Documentation Migrated

> **Important Notice**: Development-related documentation has been migrated to the new Development Viewpoint structure

## 🚀 New Location

All development-related documentation is now unified and integrated in **[Development Viewpoint](../viewpoints/development/)**, providing more complete and systematic development guidelines.

**Main Entry**: [Development Viewpoint Overview](../viewpoints/development/README.md)

## 📋 Documentation Migration Mapping

| Original Document | New Location | Description |
|-------------------|--------------|-------------|
| [getting-started.md](getting-started.md) | **Getting Started Guide** | Development environment setup and quick start |
| [coding-standards.md](coding-standards.md) | **Coding Standards** | Code style and quality standards |
| [testing-guide.md](testing-guide.md) | **Testing Strategy** | Testing strategy and best practices |
| [documentation-guide.md](documentation-guide.md) | **Documentation Standards** | Documentation writing and maintenance standards |
| [instructions.md](instructions.md) | **Development Workflow** | Detailed development process and tool usage |
| [epic.md](epic.md) | **Epic Implementation Guide** | Large feature development guide |

## 📚 New Development Documentation Structure

```text
docs/viewpoints/development/
├── README.md                           # Development viewpoint overview
├── getting-started/                    # Quick start
│   ├── README.md                      # Quick start guide
│   ├── environment-setup.md           # Environment configuration guide
│   ├── prerequisites.md               # Prerequisites checklist
│   └── first-contribution.md          # First contribution guide
├── architecture/                      # Architecture design
│   ├── ddd-patterns/                  # DDD patterns
│   ├── hexagonal-architecture/        # Hexagonal architecture
│   ├── microservices/                 # Microservices architecture
│   └── saga-patterns/                 # Saga patterns
├── coding-standards/                  # Coding standards
│   ├── README.md                      # Coding standards overview
│   ├── java-standards.md              # Java coding specifications
│   ├── frontend-standards.md          # Frontend coding specifications
│   └── api-design.md                  # API design specifications
├── testing/                           # Testing strategy
│   ├── README.md                      # Testing strategy overview
│   ├── tdd-practices/                 # TDD practices
│   ├── bdd-practices/                 # BDD practices
│   └── performance-monitoring/        # Performance monitoring
├── workflows/                         # Workflows
│   ├── README.md                      # Workflow overview
│   ├── development-workflow.md        # Development process standards
│   └── release-process.md             # Release process management
└── tools-and-environment/             # Tool chain
    ├── README.md                      # Tool chain overview
    └── technology-stack/              # Technology stack
```

## 🚀 Quick Start

### Development Environment

- **Java**: 21
- **Spring Boot**: 3.4.5
- **Gradle**: 8.x
- **Node.js**: 18+ (frontend)

### Basic Commands

```bash
# Build project
./gradlew build

# Run tests
./gradlew test

# Start application
./gradlew bootRun
```

## 📅 Migration Information

- **Migration Date**: January 21, 2025
- **Reason**: Unify development documentation into Development Viewpoint structure
- **Status**: Completed, content has been integrated and enhanced

---

*This directory will be restructured in the next version. Please update your bookmarks and references to the new location.*
