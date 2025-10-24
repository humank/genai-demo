# Development Viewpoint Overview

> **Last Updated**: 2025-10-23  
> **Status**: Active  
> **Stakeholders**: Developers, Build Engineers, Architects

## Purpose

The Development Viewpoint describes how the system is structured from a developer's perspective, focusing on code organization, module structure, and development practices. This viewpoint helps developers understand:

- How the codebase is organized
- What architectural patterns are used
- How modules depend on each other
- How to navigate and contribute to the codebase

## Key Concerns

This viewpoint addresses the following concerns:

1. **Code Organization**: How is the code structured to support maintainability and scalability?
2. **Module Structure**: How are modules organized and what are their responsibilities?
3. **Dependency Management**: What are the dependency rules and how are they enforced?
4. **Build Process**: How is the system built, tested, and packaged?
5. **Development Environment**: What tools and setup are required for development?
6. **Code Quality**: How is code quality maintained and enforced?

## Architectural Approach

### Hexagonal Architecture (Ports and Adapters)

The system follows **Hexagonal Architecture** (also known as Ports and Adapters pattern) combined with **Domain-Driven Design (DDD)** tactical patterns. This architecture ensures:

- **Domain Independence**: Business logic is isolated from technical concerns
- **Testability**: Core domain can be tested without infrastructure
- **Flexibility**: Easy to swap infrastructure implementations
- **Maintainability**: Clear separation of concerns

### Layer Structure

```
┌─────────────────────────────────────────────────────────┐
│                    Interfaces Layer                      │
│  (REST Controllers, Event Handlers, Web UI)             │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│                  Application Layer                       │
│  (Use Case Orchestration, Application Services)         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                          │
│  (Business Logic, Aggregates, Events, Value Objects)    │
└─────────────────────────────────────────────────────────┘
                     ↑
                     │
┌─────────────────────────────────────────────────────────┐
│                Infrastructure Layer                      │
│  (Persistence, Messaging, External Services)            │
└─────────────────────────────────────────────────────────┘
```

### Dependency Rules

The architecture enforces strict dependency rules:

1. **Domain Layer**: Has NO dependencies on any other layer
   - Contains pure business logic
   - Defines repository interfaces
   - Defines domain events
   - No framework dependencies (except Java standard library)

2. **Application Layer**: Depends ONLY on Domain Layer
   - Orchestrates use cases
   - Coordinates domain objects
   - Publishes domain events
   - Manages transactions

3. **Infrastructure Layer**: Depends on Domain Layer
   - Implements repository interfaces
   - Implements external service adapters
   - Handles persistence (JPA entities)
   - Manages messaging (Kafka, Redis)

4. **Interfaces Layer**: Depends on Application and Domain Layers
   - Exposes REST APIs
   - Handles HTTP requests/responses
   - Manages authentication/authorization
   - Converts DTOs to domain objects

## Code Organization

### Root Package Structure

```
solid.humank.genaidemo/
├── domain/              # Domain Layer
│   ├── customer/       # Customer Bounded Context
│   ├── order/          # Order Bounded Context
│   ├── product/        # Product Bounded Context
│   ├── payment/        # Payment Bounded Context
│   ├── inventory/      # Inventory Bounded Context
│   ├── promotion/      # Promotion Bounded Context
│   ├── logistics/      # Logistics Bounded Context
│   ├── notification/   # Notification Bounded Context
│   ├── review/         # Review Bounded Context
│   ├── shoppingcart/   # Shopping Cart Bounded Context
│   ├── pricing/        # Pricing Bounded Context
│   ├── seller/         # Seller Bounded Context
│   ├── delivery/       # Delivery Bounded Context
│   └── shared/         # Shared Kernel
├── application/        # Application Layer
│   ├── customer/       # Customer Use Cases
│   ├── order/          # Order Use Cases
│   └── ...
├── infrastructure/     # Infrastructure Layer
│   ├── customer/       # Customer Infrastructure
│   ├── order/          # Order Infrastructure
│   ├── config/         # Configuration
│   ├── event/          # Event Publishing
│   └── ...
└── interfaces/         # Interfaces Layer
    ├── rest/           # REST API Controllers
    └── web/            # Web UI Controllers
```

### Bounded Context Organization

Each bounded context follows a consistent structure within the domain layer:

```
domain/{context}/
├── model/
│   ├── aggregate/      # Aggregate Roots
│   ├── entity/         # Entities (non-root)
│   ├── valueobject/    # Value Objects
│   └── specification/  # Business Rules
├── events/             # Domain Events
├── repository/         # Repository Interfaces
├── service/            # Domain Services
└── validation/         # Validation Logic
```

### Example: Customer Bounded Context

```
domain/customer/
├── model/
│   ├── aggregate/
│   │   └── Customer.java              # Aggregate Root
│   ├── entity/
│   │   └── CustomerProfile.java       # Entity
│   ├── valueobject/
│   │   ├── CustomerId.java            # Value Object
│   │   ├── Email.java                 # Value Object
│   │   └── MembershipLevel.java       # Value Object
│   └── specification/
│       └── CustomerEligibilitySpec.java
├── events/
│   ├── CustomerCreatedEvent.java      # Domain Event
│   └── CustomerProfileUpdatedEvent.java
├── repository/
│   └── CustomerRepository.java        # Repository Interface
├── service/
│   └── CustomerDomainService.java     # Domain Service
└── validation/
    └── CustomerValidator.java
```

## Module Dependencies

### Technology Stack

#### Backend Core
- **Java**: 21 (LTS)
- **Spring Boot**: 3.4.5
- **Spring Framework**: 6.x
- **Gradle**: 8.x

#### Data & Persistence
- **Spring Data JPA**: For repository implementations
- **Hibernate**: ORM framework
- **Flyway**: Database migrations
- **H2**: In-memory database (dev/test)
- **PostgreSQL**: Production database

#### Testing
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **Cucumber 7**: BDD testing
- **ArchUnit**: Architecture testing

#### API & Documentation
- **SpringDoc OpenAPI 3**: API documentation
- **Swagger UI**: API exploration

#### Observability
- **Spring Boot Actuator**: Metrics and health checks
- **AWS X-Ray**: Distributed tracing
- **Micrometer**: Metrics collection

### Dependency Management

Dependencies are managed through Gradle's version catalog:

```gradle
// gradle/libs.versions.toml
[versions]
spring-boot = "3.4.5"
java = "21"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
```

### Prohibited Dependencies

To maintain architectural integrity, certain dependencies are prohibited:

1. **Domain Layer**:
   - ❌ No Spring Framework annotations (except `@Component` for domain services)
   - ❌ No JPA annotations
   - ❌ No infrastructure libraries
   - ✅ Only Java standard library and domain-specific libraries

2. **Application Layer**:
   - ❌ No infrastructure implementations
   - ❌ No REST/HTTP libraries
   - ✅ Only domain interfaces and Spring transaction management

3. **All Layers**:
   - ❌ No circular dependencies
   - ❌ No direct database access from domain/application layers

## Development Practices

### Domain-Driven Design (DDD)

The codebase follows DDD tactical patterns:

1. **Aggregates**: Consistency boundaries with aggregate roots
2. **Entities**: Objects with identity
3. **Value Objects**: Immutable objects defined by their attributes
4. **Domain Events**: Capture business events
5. **Repositories**: Persistence abstraction
6. **Domain Services**: Stateless business logic

### Event-Driven Architecture

The system uses domain events for:

- **Cross-Context Communication**: Bounded contexts communicate via events
- **Eventual Consistency**: Asynchronous data synchronization
- **Audit Trail**: Event sourcing for critical operations
- **Integration**: External system integration

### Test-Driven Development (TDD)

Development follows TDD practices:

1. **Red**: Write failing test
2. **Green**: Write minimal code to pass
3. **Refactor**: Improve code quality

### Behavior-Driven Development (BDD)

Business requirements are captured as Gherkin scenarios:

```gherkin
Feature: Customer Registration
  Scenario: Successful customer registration
    Given a new customer with valid information
    When they submit the registration form
    Then they should receive a confirmation email
    And their account should be created
```

## Build System

### Gradle Build Structure

```
project/
├── build.gradle                # Root build configuration
├── settings.gradle             # Project settings
├── gradle.properties           # Build properties
├── app/
│   └── build.gradle           # Application module
└── gradle/
    ├── libs.versions.toml     # Version catalog
    └── wrapper/               # Gradle wrapper
```

### Key Gradle Tasks

```bash
# Build and test
./gradlew build                 # Full build with tests
./gradlew clean build          # Clean build

# Testing
./gradlew test                 # Run all tests
./gradlew unitTest             # Run unit tests only
./gradlew integrationTest      # Run integration tests
./gradlew cucumber             # Run BDD tests

# Code Quality
./gradlew archUnit             # Architecture tests
./gradlew jacocoTestReport     # Code coverage report
./gradlew pmdMain              # Static analysis

# Development
./gradlew bootRun              # Run application
./gradlew bootJar              # Create executable JAR
```

## Code Quality Standards

### Automated Quality Checks

1. **Architecture Compliance**: ArchUnit tests enforce architectural rules
2. **Code Coverage**: Minimum 80% line coverage required
3. **Static Analysis**: PMD and SpotBugs for code quality
4. **Style Checks**: Checkstyle for consistent formatting
5. **Dependency Analysis**: Gradle dependency verification

### Quality Gates

Before merging code:

- ✅ All tests pass
- ✅ Code coverage ≥ 80%
- ✅ No ArchUnit violations
- ✅ No critical PMD/SpotBugs issues
- ✅ Code review approved

## Development Environment

### Required Tools

1. **Java Development Kit (JDK)**: Java 21
2. **IDE**: IntelliJ IDEA (recommended) or VS Code
3. **Build Tool**: Gradle 8.x (via wrapper)
4. **Version Control**: Git
5. **Container Runtime**: Docker Desktop
6. **Database Client**: DBeaver or similar

### Optional Tools

1. **API Testing**: Postman or Insomnia
2. **Database**: PostgreSQL (for local testing)
3. **Message Broker**: Kafka (via Docker)
4. **Cache**: Redis (via Docker)

### IDE Configuration

#### IntelliJ IDEA

1. **Code Style**: Import `config/intellij-code-style.xml`
2. **Inspections**: Enable all Java inspections
3. **Plugins**:
   - Lombok
   - Spring Boot
   - Cucumber for Java
   - PlantUML integration

#### VS Code

1. **Extensions**:
   - Java Extension Pack
   - Spring Boot Extension Pack
   - Cucumber (Gherkin) Full Support
   - PlantUML

## Navigation

### Related Documents

- [Module Organization](module-organization.md) - Detailed package structure
- [Dependency Rules](dependency-rules.md) - Architecture constraints
- [Build Process](build-process.md) - Build and deployment

### Related Viewpoints

- [Functional Viewpoint](../functional/README.md) - Business capabilities
- [Information Viewpoint](../information/README.md) - Data models
- [Deployment Viewpoint](../deployment/README.md) - Infrastructure

### Related Perspectives

- [Evolution Perspective](../../perspectives/evolution/README.md) - Maintainability
- [Development Resource Perspective](../../perspectives/development-resource/README.md) - Team skills

### Development Guides

- [Local Environment Setup](../../development/setup/local-environment.md)
- [Coding Standards](../../development/coding-standards/java-standards.md)
- [Testing Strategy](../../development/testing/testing-strategy.md)

---

**Next**: [Module Organization](module-organization.md) →

