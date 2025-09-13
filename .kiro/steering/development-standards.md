# Development Standards and Guidelines

## Technology Stack Requirements

### Backend Technologies

- Spring Boot 3.4.5 + Java 21 + Gradle 8.x
- Spring Data JPA + Hibernate + Flyway
- H2 (dev/test) + PostgreSQL (prod)
- SpringDoc OpenAPI 3 + Swagger UI
- Spring Boot Actuator + AWS X-Ray + Micrometer

### Frontend Technologies

- CMC Management: Next.js 14 + React 18 + TypeScript
- Consumer App: Angular 18 + TypeScript
- UI Components: shadcn/ui + Radix UI

### Testing Frameworks

- JUnit 5 + Mockito + AssertJ
- Cucumber 7 (BDD) + Gherkin
- ArchUnit (Architecture Testing)

## Architecture Constraints

### Package Structure Standards

- `domain/{context}/model/` - Aggregate roots, entities, value objects
- `domain/{context}/events/` - Domain events (Records)
- `application/{context}/` - Use case implementations
- `infrastructure/{context}/persistence/` - Persistence adapters

### Layer Dependency Rules

```
interfaces/ → application/ → domain/ ← infrastructure/
```

### Domain Event Design Constraints

- Use immutable Records implementation
- Aggregate roots collect events, application services publish events
- Event handlers in infrastructure layer

## Testing Standards

### Test Layer Requirements (Test Pyramid)

- Unit Tests (80%): < 50ms, < 5MB
- Integration Tests (15%): < 500ms, < 50MB  
- E2E Tests (5%): < 3s, < 500MB

### Test Classification Standards

#### Unit Tests (Preferred)

- Annotation: `@ExtendWith(MockitoExtension.class)`
- Applicable: Pure business logic, utilities, configuration classes
- Prohibited: Spring context

#### Integration Tests (Use Cautiously)

- Annotation: `@DataJpaTest`, `@WebMvcTest`, `@JsonTest`
- Applicable: Database integration, external services
- Requirement: Partial Spring context

#### E2E Tests (Minimal Use)

- Annotation: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Applicable: Complete business process verification
- Requirement: Full Spring context

### Test Tagging System

```java
@UnitTest        // Fast unit tests
@SmokeTest       // Core functionality tests
@SlowTest        // Slow test marker
@IntegrationTest // Integration tests
```

### Performance Benchmark Requirements

- Unit tests: < 100ms, < 10MB, success rate > 99%
- Integration tests: < 1s, < 100MB, success rate > 95%
- End-to-end tests: < 5s, < 1GB, success rate > 90%

## BDD Development Process

### Mandatory Steps

1. Write Gherkin scenarios (`src/test/resources/features/`)
2. Implement step definitions (Red)
3. TDD implement domain logic (Green)
4. Refactor optimization (Refactor)

## Code Standards

### Naming Conventions

```java
// Aggregate root
@AggregateRoot
public class Customer implements AggregateRootInterface { }

// Value object
@ValueObject
public record CustomerId(String value) { }

// Domain event
public record CustomerCreatedEvent(...) implements DomainEvent { }

// Test class
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest { }
```

### Mock Usage Rules

- Only mock interactions actually used in tests
- Avoid global stubbing
- Handle null cases

## ArchUnit Rules

### Mandatory Architecture Rules

- Layer dependency checks
- DDD tactical pattern verification
- Package naming convention checks

### Prohibited Anti-patterns

```java
// ❌ Wrong: Configuration class tests don't need full Spring context
@SpringBootTest
class DatabaseConfigurationTest { ... }

// ✅ Correct: Use unit tests
@ExtendWith(MockitoExtension.class)
class DatabaseConfigurationUnitTest { ... }
```

## Quality Standards

### Must-Achieve Metrics

- Code coverage > 80%
- Test execution time < 15s (unit tests)
- Test failure rate < 1%
- Architecture compliance 100%

### BDD Scenario Coverage Requirements

- Core business processes 100% coverage
- Exception handling scenario coverage
- User experience critical path coverage

## Development Workflow

### New Feature Development Sequence

1. BDD scenario design
2. Domain modeling (DDD)
3. TDD implementation
4. Integration testing
5. ArchUnit verification

### Daily Development Commands

```bash
./gradlew quickTest              # Development quick feedback (2s)
./gradlew unitTest               # Pre-commit full verification (11s)
./gradlew integrationTest        # PR integration test check
./gradlew test                   # Pre-release full test
```
