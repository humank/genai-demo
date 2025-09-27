# System Development and Testing Design Compliance Standards

In this project, please follow DDD tactical design patterns and layered principles, as required by the 3 ArchUnit tests under `../app/src/test/java/solid/humank/genaidemo/architecture`, to complete the code development work for this project.

## Architecture Design Principles

### Layered Architecture

1. **Domain Layer**
   - Contains core business logic and rules
   - Does not depend on other layers
   - Includes aggregate roots, entities, value objects, domain events, domain services, and domain exceptions

2. **Application Layer**
   - Coordinates domain objects to complete user use cases
   - Only depends on the domain layer
   - Includes application services, DTOs, command and query objects

3. **Infrastructure Layer**
   - Provides technical implementation
   - Depends on the domain layer, implements interfaces defined by the domain layer
   - Includes repository implementations, external system adapters, ORM mappings, etc.

4. **Interfaces Layer**
   - Handles user interaction
   - Only depends on the application layer
   - Includes controllers, view models, request/response objects, etc.

### DDD Tactical Design Patterns

1. **Aggregate Root**
   - Must be located in `domain.*.model.aggregate` package
   - Marked with `@AggregateRoot` annotation
   - Controls access to internal entities
   - Ensures business invariants

2. **Entity**
   - Must be located in `domain.*.model.entity` package
   - Marked with `@Entity` annotation
   - Has unique identity
   - Mutable but maintains identity consistency

3. **Value Object**
   - Must be located in `domain.*.model.valueobject` package
   - Marked with `@ValueObject` annotation
   - Immutable
   - No unique identity, equality compared through attribute values

4. **Domain Event**
   - Must be located in `domain.*.events` package
   - Implements `DomainEvent` interface
   - Immutable, records business events that have occurred

5. **Domain Service**
   - Must be located in `domain.*.service` package
   - Stateless, handles business logic across aggregates

6. **Repository**
   - Interface must be located in `domain.*.repository` package
   - Implementation must be located in `infrastructure.persistence` package
   - Operates on aggregate roots

## Testing Design Principles

### Test Architecture

This project has established a complete test support tool ecosystem, located in the `app/src/test/java/solid/humank/genaidemo/testutils/` directory:

1. **Test Data Builders**
   - Use Builder pattern to simplify test data creation
   - Support method chaining and default value setting
   - Provide domain-specific construction methods

2. **Scenario Handlers**
   - Handle complex test scenario logic
   - Use strategy pattern to handle different situations
   - Avoid conditional logic in tests

3. **Custom Matchers**
   - Provide more expressive test assertions
   - Improve error messages when tests fail
   - Support domain-specific validation logic

### Testing Best Practices

1. **3A Principle (Arrange-Act-Assert)**
   - Each test method must have clear three sections
   - Arrange: Prepare test data and environment
   - Act: Execute the operation being tested
   - Assert: Verify results

2. **No Conditional Logic**
   - Tests should not contain if-else statements
   - Use scenario handlers to handle complex logic
   - Maintain test simplicity and readability

3. **Test Independence**
   - Each test should be independent and repeatable
   - Not dependent on execution order of other tests
   - Use appropriate test data isolation mechanisms

4. **Descriptive Naming**
   - Use clear test method names
   - Use @DisplayName to provide detailed descriptions
   - Test names should explain the purpose and expected results

5. **Test Classification**
   - Use test tag annotations for classification:
     - `@UnitTest`: Fast-executing unit tests
     - `@IntegrationTest`: Integration tests requiring external dependencies
     - `@SlowTest`: Tests with longer execution time
     - `@BddTest`: Behavior-driven development tests

### BDD Testing Principles

1. **Step Definition Simplicity**
   - Each step definition is responsible for only one clear operation
   - Does not contain complex conditional logic
   - Use scenario handlers to handle complex scenarios

2. **Test Data Management**
   - Use test builders to create test data
   - Use TestFixtures to provide common data
   - Avoid hard-coded test data

3. **Exception Handling**
   - Use unified exception handling mechanism
   - Capture and verify exceptions through TestExceptionHandler
   - Provide clear exception verification methods

4. **Domain Event**
   - Implements `DomainEvent` interface
   - Immutable
   - Records important events occurring in the domain

5. **Repository**
   - Interface must be located in `domain.*.repository` package
   - Implementation must be located in `infrastructure.*.persistence` package
   - Operates on aggregate roots
   - Provides persistence and query functionality

6. **Domain Service**
   - Must be located in `domain.*.service` package
   - Marked with `@DomainService` annotation
   - Stateless
   - Handles business logic across aggregates

7. **Specification**
   - Must be located in `domain.*.specification` package
   - Implements `Specification` interface
   - Encapsulates complex business rules and query conditions

8. **Anti-Corruption Layer**
   - Must be located in `infrastructure.*.acl` package
   - Isolates external systems
   - Translates external models to internal models

## Testing Strategy

### 1. Unit Testing

Write unit tests for aggregate roots based on Cucumber-JVM, without depending on the Spring Boot framework.

- **Test Scope**: Aggregate roots, entities, value objects, domain services
- **Test Tools**: Cucumber-JVM, JUnit 5
- **Test Objectives**: Verify business rules and invariants

#### Cucumber BDD Testing Specifications

1. **Feature Files**
   - Use Gherkin syntax to describe business scenarios
   - Located in `src/test/resources/features` directory
   - Organized by subdomain, such as `inventory/inventory_management.feature`

2. **Step Definitions**
   - Located in `src/test/java/solid/humank/genaidemo/bdd` directory
   - Organized by subdomain, such as `inventory/InventoryStepDefinitions.java`
   - Implement steps from Feature files

3. **Test Isolation**
   - Each test scenario should be independent
   - Use appropriate test data
   - Clean up after tests

### 2. Integration Testing

Test application layer, infrastructure layer, and interface layer, can start Spring Boot for testing.

- **Test Scope**: Application services, repository implementations, controllers
- **Test Tools**: Spring Boot Test, MockMvc, TestContainers
- **Test Objectives**: Verify component integration and end-to-end processes

#### Application Service Testing

- Verify use case coordination logic
- Ensure correct invocation of domain objects
- Check transaction management and event publishing

#### Repository Testing

- Verify CRUD operations
- Ensure correct query conditions
- Check optimistic locking and concurrency control

#### Controller Testing

- Verify HTTP request handling
- Ensure correct status codes and response formats
- Check input validation and error handling

## Implementation Guidelines

### 1. Ensure Repository Adapters Correctly Implement Domain Repository Interfaces

- **Complete Implementation of All Methods**: Repository adapters must implement all methods defined in domain repository interfaces, such as `save`, `findById`, `findAll`, etc.

- **Correct Conversion Logic**: Adapters need to correctly convert domain models to persistence models (JPA entities) and vice versa. For example, in `OrderRepositoryAdapter`, ensure that `OrderMapper.toJpaEntity()` and `OrderMapper.toDomainEntity()` methods are called correctly.

- **Transaction Management**: Ensure adapters handle transactions correctly, especially in operations involving multiple entities.

- **Exception Handling**: Appropriately catch and convert infrastructure layer exceptions, avoiding leaking technical details to the domain layer.

- **Domain Event Handling**: If domain models publish events, ensure adapters can correctly handle these events, such as publishing events after saving aggregate roots.

### 2. Check if Mapper Classes Correctly Handle Conversion Between Domain Models and Persistence Models

- **Bidirectional Conversion**: Ensure `toJpaEntity()` and `toDomainEntity()` methods can correctly convert all attributes bidirectionally.

- **Value Object Conversion**: Pay special attention to value object conversion (such as Money, OrderId), ensuring their semantics and invariants are preserved during conversion.

- **Collection Conversion**: Correctly handle collection conversion (such as order item lists), including handling empty collections.

- **State Conversion**: Ensure correct conversion of enum types (such as OrderStatus), especially when JPA entities use strings to store states.

- **Association Relationships**: Handle association relationships between entities, ensuring they are correctly maintained during conversion.

- **Aggregate Boundaries**: Ensure the conversion process respects aggregate boundaries and doesn't accidentally load data across aggregate boundaries.

### 3. Ensure JPA Entity Classes Match Database Table Structure

- **Table and Column Names**: Ensure names in `@Table` and `@Column` annotations match database table and column names.

- **Primary Key Strategy**: Ensure primary key generation strategy (such as `@Id` and `@GeneratedValue`) is consistent with database design.

- **Association Relationships**: Ensure association relationship annotations like `@OneToMany`, `@ManyToOne` correctly reflect relationships between database tables.

- **Cascade Operations**: Check cascade operation settings (such as `cascade = CascadeType.ALL`) meet business requirements.

- **Data Types**: Ensure Java types are compatible with database column types, especially for date, time, and numeric types.

- **Constraints**: Ensure constraint conditions like `nullable`, `unique` are consistent with database design.

- **Indexes**: If needed, add `@Index` annotations to optimize query performance.

### 4. Check if Cucumber Test Step Definitions Cover All Scenarios

- **Scenario Coverage**: Ensure all scenarios defined in feature files have corresponding step definitions.

- **Step Implementation**: Ensure each step definition has correct implementation and uses appropriate assertions to verify results.

- **Test Isolation**: Ensure each test scenario is independent and not affected by other tests.

- **Test Data**: Ensure tests use appropriate test data and clean up after testing.

- **Exception Handling**: Ensure tests correctly handle exceptional situations, especially for scenarios that should throw exceptions.

- **Boundary Conditions**: Ensure tests cover boundary conditions and extreme cases.

## Common Issues and Solutions

### 1. Handling Test Failures

When Cucumber tests fail, possible causes and solutions:

- **Step Definition Mismatch**: Ensure step definitions exactly match steps in feature files, including spaces and punctuation.
- **Test Data Issues**: Ensure test data is correctly set up, especially when sharing data between multiple steps.
- **Assertion Failures**: Check expected and actual values, may need to adjust business logic or test expectations.
- **State Management Issues**: Ensure state is correctly reset between test scenarios, avoiding test interference.

### 2. Domain Model and Persistence Model Conversion Issues

- **Data Loss**: Ensure all necessary attributes are handled during conversion.
- **Type Mismatch**: Handle conversion between different types, such as string to enum, string to date, etc.
- **Circular Dependencies**: Handle circular references between entities, may need to use lazy loading or separate conversion processes.

### 3. Aggregate Boundary Issues

- **Oversized Aggregates**: Split oversized aggregates into multiple smaller aggregates, using references between aggregates.
- **Inter-Aggregate Consistency**: Use domain events or application services to coordinate consistency between multiple aggregates.
- **Query Performance**: For complex queries, consider using dedicated query models or CQRS pattern.

## Conclusion

Following these design principles and testing strategies helps us build a robust, maintainable, and testable system. DDD tactical design patterns and layered architecture provide a clear structure that allows us to focus on business logic while maintaining flexibility in technical implementation. Cucumber BDD testing ensures our code meets business requirements, while integration testing ensures components work together correctly.
