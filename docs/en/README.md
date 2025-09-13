# GenAI Demo Documentation Center

Welcome to the GenAI Demo project documentation center! This contains the complete project documentation, organized by functionality and purpose.

## 🌐 Language Versions

- **Chinese Version**: [docs/](../)
- **English Version** (Current): [docs/en/](.)

## 📖 Main Project README

- **English Project Overview**: [PROJECT_README.md](PROJECT_README.md) - Complete English translation of the main project README
- **Chinese Project Overview**: [../../README.md](../../README.md) - Original Chinese project README

## 📚 Documentation Categories

### 🏗️ [Architecture Documentation](architecture/)

System architecture related documentation, suitable for architects and senior developers.

- **[Architecture Decision Records (ADR)](architecture/adr/)** - Complete record of all important architectural decisions
- [Architecture Overview](architecture/overview.md) - System overall architecture introduction
- [Hexagonal Architecture](architecture/hexagonal-architecture.md) - Hexagonal architecture implementation details
- [Layered Architecture Design](architecture/layered-architecture-design.md) - Layered architecture design guide
- [2025 Architecture Improvements](architecture/improvements-2025.md) - Latest architecture improvement records

### 🔌 [API Documentation](api/)

API related documentation, suitable for API users and frontend developers.

- [API Versioning Strategy](api/API_VERSIONING_STRATEGY.md) - API version management strategy
- [OpenAPI Specification](api/openapi-spec.md) - OpenAPI 3.0 specification documentation
- [Frontend API Integration](api/frontend-integration.md) - Frontend API integration guide

### 📊 [Diagram Documentation](diagrams/)

Various system diagrams and visualization documentation, suitable for all roles.

#### Mermaid Diagrams (Direct GitHub Display)

- [Architecture Overview](diagrams/mermaid/architecture-overview.md) - System overall architecture diagram
- [Hexagonal Architecture](diagrams/mermaid/hexagonal-architecture.md) - Hexagonal architecture diagram
- [DDD Layered Architecture](diagrams/mermaid/ddd-layered-architecture.md) - DDD layered architecture diagram
- [Event-Driven Architecture](diagrams/mermaid/event-driven-architecture.md) - Event-driven architecture diagram
- [API Interaction Diagram](diagrams/mermaid/api-interactions.md) - API interaction relationship diagram

#### PlantUML Diagrams (Detailed UML Diagrams)

- **Structural Diagrams**: Class diagrams, object diagrams, component diagrams, deployment diagrams, package diagrams, composite structure diagrams
- **Behavioral Diagrams**: Use case diagrams, activity diagrams, state diagrams
- **Interaction Diagrams**: Sequence diagrams, communication diagrams, interaction overview diagrams, timing diagrams
- **Event Storming**: Big Picture, Process Level, Design Level

### 💻 [Development Guide](development/)

Development related documentation, suitable for developers and new team members.

- [Getting Started](development/getting-started.md) - Project quick start guide
- [Coding Standards](development/coding-standards.md) - Coding standards and best practices
- [Development Instructions](development/instructions.md) - Development process and instructions
- [Documentation Maintenance Guide](development/documentation-guide.md) - Documentation creation and maintenance guide

### 🚀 [Deployment Documentation](deployment/)

Deployment related documentation, suitable for DevOps engineers and operations personnel.

- [Docker Guide](deployment/docker-guide.md) - Docker containerized deployment
- [Kubernetes Guide](deployment/kubernetes-guide.md) - Kubernetes cluster deployment

### 🎨 [Design Documentation](design/)

Design related documentation, suitable for software architects and design decision makers.

- [DDD Guide](design/ddd-guide.md) - Domain-driven design guide
- [Design Principles](design/design-principles.md) - Software design principles
- [Refactoring Guide](design/refactoring-guide.md) - Code refactoring guide

### 📋 [Release Notes](releases/)

Version releases and change records, suitable for all stakeholders.

- [Release Records](releases/) - Version release history

### 📊 [Report Documentation](reports/)

Project reports and analysis documentation, suitable for project managers and technical leads.

- [Project Summary 2025](reports/project-summary-2025.md) - 2025 project summary report
- [Architecture Excellence 2025](reports/architecture-excellence-2025.md) - Architecture excellence assessment
- [Technology Stack 2025](reports/technology-stack-2025.md) - Technology stack analysis report
- [Documentation Cleanup 2025](reports/documentation-cleanup-2025.md) - Documentation cleanup report

## 🎯 Quick Navigation

### 👨‍💼 I am a Project Manager

- [Project Summary 2025](reports/project-summary-2025.md) - Understand project status
- [Architecture Overview](diagrams/mermaid/architecture-overview.md) - System overall architecture
- [Release Records](releases/) - Version release history

### 🏗️ I am an Architect

- [Architecture Documentation](architecture/) - Complete architecture design
- [Diagram Documentation](diagrams/) - Visual architecture diagrams
- [Design Documentation](design/) - Design principles and guidelines

### 👨‍💻 I am a Developer

- [Development Guide](development/) - Development environment and standards
- [API Documentation](api/) - API usage guide
- [Development Instructions](development/instructions.md) - Development process and instructions

### 🚀 I am a DevOps Engineer

- [Deployment Documentation](deployment/) - Deployment guide
- [Docker Guide](deployment/docker-guide.md) - Containerized deployment
- [Kubernetes Guide](deployment/kubernetes-guide.md) - Cluster deployment

### 🔍 I am a Business Analyst

- [Event Storming Diagrams](diagrams/plantuml/event-storming/) - Business process analysis
- [Use Case Diagrams](diagrams/plantuml/behavioral/) - System functionality overview
- [API Interaction Diagram](diagrams/mermaid/api-interactions.md) - System interactions

## 🛠️ Tools and Scripts

### Diagram Generation

```bash
# Generate all PlantUML diagrams
./scripts/generate-diagrams.sh

# Generate specific diagram
./scripts/generate-diagrams.sh domain-model-class-diagram.puml

# Validate diagram syntax
./scripts/generate-diagrams.sh --validate
```

### Documentation Synchronization

```bash
# Synchronize Chinese and English documentation
./scripts/sync-docs.sh

# Validate documentation quality
./scripts/validate-docs.sh
```

## 📈 Project Statistics

- **Total Documents**: 50+ documents
- **Number of Diagrams**: 20+ diagrams
- **Supported Languages**: Chinese, English
- **Architecture Patterns**: DDD + Hexagonal Architecture + Event-Driven
- **Technology Stack**: Java 21 + Spring Boot 3.5.5 + Next.js 14.2.30 + Angular 18.2.0
- **Test Coverage**: 272 tests, 100% pass rate
- **Database Migrations**: 22 Flyway migration scripts
- **Code Quality**: ArchUnit architecture tests ensure DDD compliance

## 🔗 External Links

### Online Editors

- [Mermaid Live Editor](https://mermaid.live/) - Online Mermaid diagram editing
- [PlantUML Online Server](http://www.plantuml.com/plantuml/uml/) - Online PlantUML diagram editing

### API Endpoints

- **Backend API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **Health Check**: <http://localhost:8080/actuator/health>
- **CMC Frontend**: <http://localhost:3002>
- **Consumer Frontend**: <http://localhost:3001>

## 📝 Contribution Guidelines

### Documentation Update Process

1. Update Chinese documentation
2. Kiro Hook automatically generates English version
3. Manual review of translation quality
4. Submit changes

### Diagram Update Process

1. Modify PlantUML source files
2. Run `./scripts/generate-diagrams.sh`
3. Check generated images
4. Submit source files and generated images

## 📞 Support

If you have questions or suggestions, please:

1. Check relevant documentation
2. Check [Issues](../../issues)
3. Create new Issue

---

**Last Updated**: January 21, 2025  
**Documentation Version**: v3.0.0  
**Maintainer**: GenAI Demo Team  
**Technology Stack**: Java 21 + Spring Boot 3.5.5 + Next.js 14.2.30 + Angular 18.2.0  
**Hook Testing**: January 21, 2025 - Testing automatic translation functionality
