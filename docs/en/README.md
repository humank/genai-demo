# GenAI Demo

> **Language / 語言選擇**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [繁體中文文檔](../README.md)

This is a demonstration project based on Domain-Driven Design (DDD) and Hexagonal Architecture, showcasing how to build a Java application with good architecture and testing practices.

## Project Architecture

This project adopts Hexagonal Architecture (also known as Ports and Adapters Architecture) and Domain-Driven Design, dividing the application into the following main layers:

1. **Domain Layer**
   - Contains core business logic and rules
   - Does not depend on other layers
   - Includes aggregate roots, entities, value objects, domain events, domain services, and domain exceptions

2. **Application Layer**
   - Coordinates domain objects to complete user use cases
   - Only depends on the domain layer
   - Contains application services, DTOs, command and query objects
   - Responsible for data transformation between interface layer and domain layer

3. **Infrastructure Layer**
   - Provides technical implementation
   - Depends on the domain layer, implementing interfaces defined by the domain layer
   - Contains repository implementations, external system adapters, ORM mappings, etc.
   - Organized by functionality into sub-packages like persistence and external systems

4. **Interface Layer**
   - Handles user interactions
   - Only depends on the application layer, not directly on the domain layer
   - Contains controllers, view models, request/response objects, etc.
   - Uses its own DTOs to interact with the application layer

## Tech Stack

- **Core Framework**: Spring Boot 3.2.0
- **Build Tool**: Gradle 8.x
- **Testing Frameworks**:
  - JUnit 5 - Unit testing
  - Cucumber 7 - BDD testing
  - ArchUnit - Architecture testing
  - Mockito - Mock objects
  - Allure 2 - Test reporting and visualization
- **Other Tools**:
  - Lombok - Reduce boilerplate code
  - PlantUML - UML diagram generation

## Documentation

The project contains rich documentation located in the `docs/en` directory:

- **Architecture Documentation**:
  - [System Architecture Overview](architecture-overview.md) - Provides a high-level view of system architecture, including features of hexagonal architecture, DDD, and event-driven architecture
  - [Hexagonal Architecture Implementation Summary](HexagonalArchitectureSummary.md) - Detailed explanation of hexagonal architecture implementation methods and advantages
  - [Hexagonal Architecture and Event Storming Integration Refactoring Guide](HexagonalRefactoring.MD) - How to refactor to hexagonal architecture using Event Storming
  - [Layered Architecture Design Analysis and Recommendations](LayeredArchitectureDesign.MD) - Analysis of pros and cons of different layered architectures and applicable scenarios

- **Design Documentation**:
  - [Design Guidelines](DesignGuideline.MD) - Contains Tell, Don't Ask principles, DDD tactical patterns, and defensive programming practices
  - [System Development and Testing Design Compliance Specifications](DesignPrinciple.md) - Defines design specifications for system development and testing
  - [Software Design Classics Essentials](SoftwareDesignClassics.md) - Summarizes core concepts from classic books in the software design field

- **Code Quality**:
  - [Code Analysis Report](CodeAnalysis.md) - Code analysis and improvement suggestions based on "Refactoring" principles
  - [Refactoring Guide](RefactoringGuidance.md) - Provides specific techniques and best practices for code refactoring

- **Refactoring Process**:
  - [DDD and Hexagonal Architecture Refactoring Journey](instruction.md) - Records the refactoring process from chaotic code structure to DDD and hexagonal architecture

- **Release Notes**:
  - [Test Code Quality Improvement and Refactoring - 2025-07-18](releases/test-quality-improvement-2025-07-18.md) - Records comprehensive improvement and refactoring of test code quality
  - [Architecture Optimization and DDD Layering Implementation - 2025-06-08](releases/architecture-optimization-2025-06-08.md) - Records detailed description of architecture optimization and DDD layering implementation
  - [Promotion Module Implementation and Architecture Optimization - 2025-05-21](releases/promotion-module-implementation-2025-05-21.md) - Records implementation of promotion functionality module and architecture optimization

- **UML Diagrams**:
  - [UML Documentation](uml/README.md) - Contains various UML diagrams such as class diagrams, component diagrams, domain model diagrams, etc.
  - [Event Storming Guide](uml/es-gen-guidance-tc.md) - Guide for drawing Event Storming three-phase outputs using PlantUML

## How to Run

### Prerequisites

- JDK 17 or higher
- Gradle 8.x

### Build Project

```bash
./gradlew build
```

### Run Application

```bash
./gradlew bootRun
```

### Run Tests

#### Run All Tests

```bash
./gradlew runAllTests
```

#### Run All Tests and View Allure Report

```bash
./gradlew runAllTestsWithReport
```

#### Run Specific Types of Tests

```bash
# Run unit tests
./gradlew test

# Run Cucumber BDD tests
./gradlew cucumber

# Run architecture tests
./gradlew testArchitecture
```

### Generate Test Reports

After tests complete, you can view the following reports:

1. **Cucumber HTML Report**: `app/build/reports/cucumber/cucumber-report.html`
2. **Cucumber JSON Report**: `app/build/reports/cucumber/cucumber-report.json`
3. **JUnit HTML Report**: `app/build/reports/tests/test/index.html`
4. **Architecture Test Report**: `app/build/reports/tests/architecture/index.html`
5. **Allure Report**: `app/build/reports/allure-report/allureReport/index.html`

   ```bash
   ./gradlew allureReport  # Generate report
   ./gradlew allureServe   # Start local server to view report
   ```

## Architecture Testing

This project uses ArchUnit to ensure code follows predetermined architectural rules. Architecture tests are located in the `app/src/test/java/solid/humank/genaidemo/architecture/` directory, including:

1. **DddArchitectureTest** - Ensures compliance with DDD layered architecture
   - Ensures domain layer does not depend on other layers
   - Ensures application layer does not depend on infrastructure and interface layers
   - Ensures interface layer does not directly depend on infrastructure and domain layers
   - Ensures compliance with layered architecture dependency direction

2. **DddTacticalPatternsTest** - Ensures correct use of DDD tactical patterns
   - Ensures value objects are immutable
   - Ensures entities have unique identities
   - Ensures aggregate roots control access to their internal entities
   - Ensures domain events are immutable
   - Ensures specifications implement the Specification interface

3. **PackageStructureTest** - Ensures package structure complies with specifications
   - Ensures infrastructure layer adapters are located in correct package structure
   - Ensures application and interface layers are organized in correct package structure
   - Ensures subdomain model structure complies with DDD tactical design

4. **PromotionArchitectureTest** - Ensures promotion module follows architectural specifications

Run architecture tests:

```bash
./gradlew testArchitecture
```

## BDD Testing

This project uses Cucumber for Behavior-Driven Development (BDD) testing. BDD test files are located at:

- **Feature Files**: `app/src/test/resources/features/` directory, organized by functional modules
- **Step Definitions**: `app/src/test/java/solid/humank/genaidemo/bdd/` directory, containing step implementations for each module

Tests cover the following domains:

- Order Management
- Inventory Management
- Payment Processing
- Delivery & Logistics
- Notification Service
- Complete Order Workflow

### Test Utility Tools

This project has established a complete test utility tool ecosystem, located in the `app/src/test/java/solid/humank/genaidemo/testutils/` directory:

- **Test Data Builders** (`builders/`): Use Builder pattern to simplify test data creation
  - `OrderTestDataBuilder` - Order test data builder
  - `CustomerTestDataBuilder` - Customer test data builder
  - `ProductTestDataBuilder` - Product test data builder

- **Scenario Handlers** (`handlers/`): Handle complex test scenario logic
  - `TestScenarioHandler` - Unified scenario handler
  - `TestExceptionHandler` - Exception handler

- **Custom Matchers** (`matchers/`): Provide more expressive test assertions
  - `OrderMatchers` - Order-related matchers
  - `MoneyMatchers` - Money-related matchers

- **Test Fixtures** (`fixtures/`): Provide commonly used test data and constants
  - `TestFixtures` - Test fixture data
  - `TestConstants` - Test constants

- **Test Tag Annotations** (`annotations/`): Support test classification and selective execution
  - `@UnitTest` - Unit test tag
  - `@IntegrationTest` - Integration test tag
  - `@SlowTest` - Slow test tag
  - `@BddTest` - BDD test tag

### Testing Best Practices

Tests in this project follow these best practices:

- **3A Principle**: Each test has clear Arrange-Act-Assert structure
- **No Conditional Logic**: Tests do not contain if-else statements
- **Descriptive Naming**: Use clear test method names and @DisplayName
- **Test Independence**: Each test is independent and repeatable
- **DRY Principle**: Use test utility tools to avoid duplicate code

Run BDD tests:

```bash
./gradlew cucumber
```

Run specific types of tests:

```bash
# Run unit tests
./gradlew test --tests "*UnitTest*"

# Run integration tests
./gradlew test --tests "*IntegrationTest*"

# Run BDD tests
./gradlew test --tests "*BddTest*"
```

View Cucumber test reports:

```bash
./gradlew cucumber
# Then open app/build/reports/cucumber/cucumber-report.html
```

## UML Diagrams

This project uses PlantUML to generate various UML diagrams, including:

- Class diagrams, object diagrams, component diagrams, deployment diagrams
- Sequence diagrams (order processing, pricing processing, delivery processing), state diagrams, activity diagrams
- Domain model diagrams, hexagonal architecture diagrams, DDD layered architecture diagrams, event storming diagrams, etc.

Recently updated diagrams:

- **DDD Layered Architecture Diagram**: Shows dependency relationships and data flow between layers
- **Pricing Processing Sequence Diagram**: Shows the flow of pricing-related operations
- **Delivery Processing Sequence Diagram**: Shows the flow of delivery-related operations
- **Updated Domain Model Diagram**: Added pricing and delivery aggregates

See [UML Documentation](uml/README.md) for more information.

## Common Issues

### Configuration Cache Issues

If you encounter configuration cache-related errors, you can use the `--no-configuration-cache` parameter:

```bash
./gradlew --no-configuration-cache <task>
```

### Allure Report Issues

If Allure report generation fails, you can try:

1. Clean the project: `./gradlew clean`
2. Re-run tests and generate report: `./gradlew runAllTestsWithReport`

Allure reports automatically include all test results, including JUnit unit tests, architecture tests, and Cucumber BDD tests. Reports show test execution status, test steps, failure reasons, and related attachments.

## Contributing

Pull Requests and Issues for improvement suggestions are welcome.

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

## DeepWiki Integration

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)
