---
adr_number: 006
title: "Environment-Specific Testing Strategy"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 004]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "evolution"]
---

# ADR-006: Environment-Specific Testing Strategy

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a comprehensive testing strategy that:

- Ensures high code quality and reliability
- Provides fast feedback during development
- Supports Test-Driven Development (TDD) and Behavior-Driven Development (BDD)
- Balances test coverage with execution speed
- Works effectively with Hexagonal Architecture (ADR-002)
- Handles different testing needs across environments
- Maintains test performance standards
- Supports continuous integration and deployment

### Business Context

**Business Drivers**:
- Need for high-quality, reliable software (99.9% uptime SLA)
- Rapid feature development without breaking existing functionality
- Regulatory compliance requiring thorough testing
- Team growth from 5 to 20+ developers
- Need for fast feedback loops (< 5 minutes for pre-commit tests)

**Constraints**:
- Development team has strong Java/JUnit experience
- Limited experience with BDD/Cucumber
- CI/CD pipeline must complete in < 15 minutes
- Test execution must not slow down development
- Budget: No additional testing infrastructure costs

### Technical Context

**Current State**:
- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture with clear layer boundaries (ADR-002)
- Domain-Driven Design tactical patterns
- Event-driven architecture (ADR-003)
- PostgreSQL database (ADR-001)

**Requirements**:
- Test pyramid approach (80% unit, 15% integration, 5% E2E)
- Fast unit tests (< 50ms, < 5MB per test)
- Moderate integration tests (< 500ms, < 50MB per test)
- Comprehensive E2E tests (< 3s, < 500MB per test)
- BDD support for business scenarios
- Test performance monitoring
- Environment-specific test configurations

## Decision Drivers

1. **Fast Feedback**: Developers need quick feedback (< 5 minutes)
2. **Test Pyramid**: Majority of tests should be fast unit tests
3. **Architecture Alignment**: Tests should respect layer boundaries
4. **BDD Support**: Business scenarios need executable specifications
5. **Performance Standards**: Tests must meet performance requirements
6. **CI/CD Integration**: Tests must run efficiently in pipeline
7. **Developer Experience**: Easy to write and maintain tests
8. **Cost Efficiency**: No expensive testing infrastructure

## Considered Options

### Option 1: Environment-Specific Testing Strategy with Test Pyramid

**Description**: Layered testing approach with different test types optimized for different purposes

**Test Types**:
```
Unit Tests (80%):
- @ExtendWith(MockitoExtension.class)
- No Spring context
- Test domain logic in isolation
- < 50ms, < 5MB per test

Integration Tests (15%):
- @DataJpaTest, @WebMvcTest, @JsonTest
- Partial Spring context
- Test infrastructure components
- < 500ms, < 50MB per test

E2E Tests (5%):
- @SpringBootTest(webEnvironment = RANDOM_PORT)
- Full Spring context
- Test complete workflows
- < 3s, < 500MB per test

BDD Tests:
- Cucumber with Gherkin scenarios
- Business-readable specifications
- Executable documentation
```

**Pros**:
- ✅ Fast feedback (unit tests run in seconds)
- ✅ Comprehensive coverage at all levels
- ✅ Aligns with test pyramid best practices
- ✅ Supports TDD and BDD workflows
- ✅ Clear separation of test types
- ✅ Performance standards enforced
- ✅ Cost-effective (no additional infrastructure)
- ✅ Works well with Hexagonal Architecture

**Cons**:
- ⚠️ Multiple test frameworks to learn
- ⚠️ Need to maintain test performance standards
- ⚠️ BDD learning curve for team

**Cost**: $0 (uses existing tools)

**Risk**: **Low** - Industry-standard approach

### Option 2: Integration Tests Only

**Description**: Focus primarily on integration tests with full Spring context

**Pros**:
- ✅ Tests real system behavior
- ✅ Simpler test setup
- ✅ Catches integration issues

**Cons**:
- ❌ Slow feedback (minutes instead of seconds)
- ❌ Difficult to isolate failures
- ❌ High resource usage
- ❌ Violates test pyramid principles
- ❌ Poor developer experience

**Cost**: $0

**Risk**: **High** - Slow tests hurt productivity

### Option 3: E2E Tests Only

**Description**: Focus on end-to-end tests through UI/API

**Pros**:
- ✅ Tests complete user journeys
- ✅ High confidence in system behavior

**Cons**:
- ❌ Very slow feedback (minutes to hours)
- ❌ Brittle and hard to maintain
- ❌ Difficult to debug failures
- ❌ Expensive to run
- ❌ Poor test coverage

**Cost**: High (requires test infrastructure)

**Risk**: **Critical** - Unsustainable for development

### Option 4: Manual Testing Only

**Description**: Rely primarily on manual QA testing

**Pros**:
- ✅ No test automation effort
- ✅ Flexible exploratory testing

**Cons**:
- ❌ No fast feedback
- ❌ Not repeatable
- ❌ Expensive and slow
- ❌ Cannot support CI/CD
- ❌ High risk of regressions

**Cost**: High (QA team costs)

**Risk**: **Critical** - Unacceptable for modern development

## Decision Outcome

**Chosen Option**: **Environment-Specific Testing Strategy with Test Pyramid**

### Rationale

The environment-specific testing strategy with test pyramid was selected for the following reasons:

1. **Fast Feedback**: Unit tests provide feedback in seconds, enabling TDD workflow
2. **Comprehensive Coverage**: All layers tested appropriately (domain, infrastructure, API)
3. **Architecture Alignment**: Tests respect Hexagonal Architecture boundaries
4. **Performance Standards**: Clear performance requirements prevent slow tests
5. **BDD Support**: Cucumber enables business-readable specifications
6. **Cost-Effective**: Uses existing tools and infrastructure
7. **Scalable**: Test suite scales with codebase without slowing down
8. **CI/CD Ready**: Fast enough for continuous integration pipeline

**Implementation Strategy**:

**Unit Tests (80% of tests)**:
- Test domain logic without any infrastructure
- Use Mockito for dependencies
- Run in milliseconds
- No Spring context overhead

**Integration Tests (15% of tests)**:
- Test repository implementations with @DataJpaTest
- Test REST controllers with @WebMvcTest
- Test JSON serialization with @JsonTest
- Use partial Spring context for efficiency

**E2E Tests (5% of tests)**:
- Test critical user journeys
- Use full Spring context
- Focus on smoke tests and happy paths
- Run less frequently (pre-release)

**BDD Tests (Cross-cutting)**:
- Cucumber scenarios for business requirements
- Executable specifications
- Living documentation
- Run as part of integration test suite

**Why Not Integration-Only**: Slow feedback kills productivity. Unit tests provide instant feedback for TDD.

**Why Not E2E-Only**: E2E tests are too slow and brittle for primary testing strategy.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn test pyramid and BDD | Training, examples, pair programming |
| QA Team | Medium | Shift from manual to automated testing | Training, collaboration with developers |
| Architects | Positive | Clear testing standards | Architecture documentation |
| Operations | Low | Automated tests reduce production issues | N/A |
| Business | Positive | Faster, more reliable releases | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All bounded contexts (testing approach)
- Development workflow (TDD/BDD)
- CI/CD pipeline (test execution)
- Code review process (test coverage requirements)
- Onboarding (testing training)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Team learning curve | High | Medium | Training, examples, pair programming |
| Test performance degradation | Medium | High | Automated performance monitoring, strict standards |
| BDD adoption resistance | Medium | Low | Demonstrate value, start with critical scenarios |
| Test maintenance overhead | Medium | Medium | Good test design, regular refactoring |
| Flaky tests | Low | High | Strict isolation, proper cleanup, retry mechanisms |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Foundation and Training (Week 1-2)

- [x] Set up test performance monitoring framework
  - Implement @TestPerformanceExtension
  - Create TestPerformanceMonitor
  - Configure performance thresholds

- [x] Create base test classes
  - BaseIntegrationTest with common setup
  - Test data builders
  - Test utilities

- [x] Configure Gradle test tasks
  ```bash
  ./gradlew quickTest        # Unit tests only (< 2 min)
  ./gradlew preCommitTest    # Unit + Integration (< 5 min)
  ./gradlew fullTest         # All tests including E2E
  ```

- [x] Conduct team training
  - Test pyramid principles
  - TDD workflow
  - BDD with Cucumber
  - Test performance standards

### Phase 2: Unit Testing Implementation (Week 2-4)

- [x] Implement unit tests for domain layer
  - Aggregate root tests
  - Value object tests
  - Domain service tests
  - Event collection tests

- [x] Achieve 80%+ unit test coverage
- [x] Enforce performance standards (< 50ms, < 5MB)
- [x] Set up ArchUnit tests for architecture compliance

### Phase 3: Integration Testing Implementation (Week 4-6)

- [x] Implement repository integration tests
  - @DataJpaTest for JPA repositories
  - Test database queries
  - Test data mapping

- [x] Implement API integration tests
  - @WebMvcTest for controllers
  - Test request/response handling
  - Test validation

- [x] Enforce performance standards (< 500ms, < 50MB)

### Phase 4: E2E and BDD Testing (Week 6-8)

- [ ] Set up Cucumber framework
  - Configure Cucumber with Spring Boot
  - Create step definition base classes
  - Set up test data management

- [ ] Implement BDD scenarios
  - Write Gherkin scenarios for key features
  - Implement step definitions
  - Link to requirements

- [ ] Implement E2E tests
  - Critical user journeys
  - Smoke tests
  - Happy path scenarios

### Phase 5: CI/CD Integration (Week 8)

- [x] Configure GitHub Actions workflows
  - Run quickTest on every push
  - Run preCommitTest on PR
  - Run fullTest before merge

- [x] Set up test reporting
  - JaCoCo coverage reports
  - Test performance reports
  - Cucumber reports

- [x] Configure quality gates
  - Minimum 80% coverage
  - All tests must pass
  - Performance standards met

### Rollback Strategy

**Trigger Conditions**:
- Test execution time > 15 minutes
- Developer productivity decreases > 30%
- Test maintenance overhead > 20% of development time
- Team unable to adopt after 8 weeks

**Rollback Steps**:
1. Simplify to integration tests only for critical paths
2. Reduce BDD scope to essential scenarios
3. Relax performance standards temporarily
4. Provide additional training and support
5. Re-evaluate strategy after addressing issues

**Rollback Time**: 1 week

## Monitoring and Success Criteria

### Success Metrics

- ✅ Unit test coverage > 80%
- ✅ Integration test coverage > 60%
- ✅ E2E test coverage for critical paths
- ✅ Test execution time: quickTest < 2 min, preCommitTest < 5 min
- ✅ Test performance standards met (100% compliance)
- ✅ Zero flaky tests
- ✅ BDD scenarios for all user stories
- ✅ Developer satisfaction > 4/5

### Monitoring Plan

**Test Performance Monitoring**:
```java
@TestPerformanceExtension(maxExecutionTimeMs = 50, maxMemoryIncreaseMB = 5)
@ExtendWith(MockitoExtension.class)
class CustomerUnitTest {
    // Automatically monitored for performance
}
```

**Metrics Tracked**:
- Test execution time per test
- Memory usage per test
- Test success/failure rates
- Coverage percentages
- Flaky test detection

**Alerts**:
- Test execution time exceeds threshold
- Coverage drops below 80%
- Flaky tests detected
- Performance regression detected

**Review Schedule**:
- Daily: Check test execution times in CI/CD
- Weekly: Review test coverage reports
- Monthly: Test strategy retrospective
- Quarterly: Performance optimization review

## Consequences

### Positive Consequences

- ✅ **Fast Feedback**: Unit tests provide instant feedback for TDD
- ✅ **High Coverage**: Comprehensive testing at all levels
- ✅ **Maintainable**: Clear test organization and standards
- ✅ **Architecture Compliance**: Tests enforce layer boundaries
- ✅ **BDD Support**: Business-readable specifications
- ✅ **Performance Standards**: Tests remain fast as codebase grows
- ✅ **CI/CD Ready**: Fast enough for continuous integration
- ✅ **Cost-Effective**: No additional infrastructure needed

### Negative Consequences

- ⚠️ **Learning Curve**: Team needs to learn multiple testing approaches
- ⚠️ **Initial Overhead**: Setting up test infrastructure takes time
- ⚠️ **Maintenance**: Need to maintain tests alongside code
- ⚠️ **Discipline Required**: Must enforce test pyramid and performance standards

### Technical Debt

**Identified Debt**:
1. Some legacy code lacks unit tests (acceptable during migration)
2. BDD scenarios not yet complete for all features (ongoing work)
3. E2E test coverage limited to critical paths (acceptable trade-off)
4. Test data management could be improved (future enhancement)

**Debt Repayment Plan**:
- **Q1 2026**: Achieve 90%+ unit test coverage
- **Q2 2026**: Complete BDD scenarios for all user stories
- **Q3 2026**: Improve test data management with builders
- **Q4 2026**: Add E2E tests for additional user journeys

## Related Decisions

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Testing strategy aligns with architecture
- [ADR-004: Event Store Implementation](004-event-store-implementation.md) - In-memory event store for testing

## Notes

### Test Classification

**Unit Tests**:
```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    @Test
    void should_calculate_total_correctly() {
        // Given
        Order order = OrderTestDataBuilder.anOrder()
            .withItem("PROD-1", 2, Money.of(10.00))
            .withItem("PROD-2", 1, Money.of(20.00))
            .build();
        
        // When
        Money total = order.calculateTotal();
        
        // Then
        assertThat(total).isEqualTo(Money.of(40.00));
    }
}
```

**Integration Tests**:
```java
@DataJpaTest
@ActiveProfiles("test")
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
class OrderRepositoryTest extends BaseIntegrationTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void should_save_and_retrieve_order() {
        // Given
        Order order = createTestOrder();
        
        // When
        orderRepository.save(order);
        Order retrieved = orderRepository.findById(order.getId()).orElseThrow();
        
        // Then
        assertThat(retrieved).isEqualTo(order);
    }
}
```

**E2E Tests**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPerformanceExtension(maxExecutionTimeMs = 3000, maxMemoryIncreaseMB = 500)
class OrderE2ETest extends BaseIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void should_complete_order_submission_flow() {
        // Given: Customer and products exist
        // When: Submit order through API
        // Then: Order created, inventory reserved, email sent
    }
}
```

**BDD Tests**:
```gherkin
Feature: Order Submission
  
  Scenario: Submit order successfully
    Given a customer with ID "CUST-001"
    And the customer has items in shopping cart
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved
```

### Gradle Test Configuration

```gradle
// Unit tests - fast feedback
tasks.register('unitTest', Test) {
    description = 'Fast unit tests (~5MB, ~50ms each)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0
}

// Integration tests - pre-commit verification
tasks.register('integrationTest', Test) {
    description = 'Integration tests (~50MB, ~500ms each)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    maxHeapSize = '6g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
}

// E2E tests - pre-release verification
tasks.register('e2eTest', Test) {
    description = 'End-to-end tests (~500MB, ~3s each)'
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
    maxHeapSize = '8g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)
}

// Quick test - daily development
tasks.register('quickTest', Test) {
    dependsOn 'unitTest'
}

// Pre-commit test - before committing
tasks.register('preCommitTest', Test) {
    dependsOn 'unitTest', 'integrationTest'
}

// Full test - before release
tasks.register('fullTest', Test) {
    dependsOn 'unitTest', 'integrationTest', 'e2eTest', 'cucumber'
}
```

### Test Performance Standards

| Test Type | Max Execution Time | Max Memory | Success Rate |
|-----------|-------------------|------------|--------------|
| Unit | 50ms | 5MB | > 99% |
| Integration | 500ms | 50MB | > 95% |
| E2E | 3s | 500MB | > 90% |

### Test Data Builders

```java
public class OrderTestDataBuilder {
    private OrderId orderId = OrderId.generate();
    private CustomerId customerId = CustomerId.of("CUST-001");
    private List<OrderItem> items = new ArrayList<>();
    
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    public OrderTestDataBuilder withCustomerId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public OrderTestDataBuilder withItem(String productId, int quantity, Money price) {
        items.add(new OrderItem(ProductId.of(productId), quantity, price));
        return this;
    }
    
    public Order build() {
        Order order = new Order(orderId, customerId, "Test Address");
        items.forEach(order::addItem);
        return order;
    }
}
```

### CI/CD Integration

```yaml
# .github/workflows/test.yml
name: Test Suite

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  quick-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run quick tests
        run: ./gradlew quickTest
      - name: Upload test results
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/reports/tests/

  pre-commit-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run pre-commit tests
        run: ./gradlew preCommitTest
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3

  full-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Run full test suite
        run: ./gradlew fullTest
      - name: Generate performance report
        run: ./gradlew generatePerformanceReport
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
