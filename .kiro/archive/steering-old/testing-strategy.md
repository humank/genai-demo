# Testing Strategy

## Overview

This document defines the testing strategy and standards for this project, following the test pyramid approach and BDD/TDD practices.

**Purpose**: Provide clear rules for test implementation and execution.
**Detailed Guides**: See `.kiro/examples/testing/` for comprehensive testing guides.

---

## Test Pyramid

### Distribution

- **Unit Tests (80%)**: < 50ms, < 5MB per test
- **Integration Tests (15%)**: < 500ms, < 50MB per test
- **E2E Tests (5%)**: < 3s, < 500MB per test

### Must Follow

- [ ] Majority of tests are unit tests
- [ ] Integration tests for infrastructure
- [ ] Minimal E2E tests for critical paths
- [ ] All tests are automated

---

## Test Classification

### Unit Tests (Preferred)

#### When to Use

- Testing domain logic in isolation
- Validating business rules
- Testing utility functions
- Verifying calculations

#### Must Follow

- [ ] Use `@ExtendWith(MockitoExtension.class)`
- [ ] No Spring context
- [ ] Mock external dependencies
- [ ] Fast execution (< 50ms)

#### Example

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    @Test
    void should_throw_exception_when_submitting_empty_order() {
        // Given
        Order order = new Order(customerId, shippingAddress);
        
        // When & Then
        assertThatThrownBy(() -> order.submit())
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Cannot submit empty order");
    }
}
```

**Detailed Guide**: #[[file:../examples/testing/unit-testing-guide.md]]

---

### Integration Tests

#### When to Use

- Testing repository implementations
- Validating database queries
- Testing API endpoints
- Verifying serialization/deserialization

#### Must Follow

- [ ] Use `@DataJpaTest`, `@WebMvcTest`, or `@JsonTest`
- [ ] Partial Spring context only
- [ ] Use test database (H2)
- [ ] Clean state between tests

#### Example

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private OrderRepository repository;
    
    @Test
    void should_find_orders_by_customer_id() {
        // Given
        Order order = createOrder(customerId);
        entityManager.persistAndFlush(order);
        
        // When
        List<Order> results = repository.findByCustomerId(customerId);
        
        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCustomerId()).isEqualTo(customerId);
    }
}
```

**Detailed Guide**: #[[file:../examples/testing/integration-testing-guide.md]]

---

### E2E Tests

#### When to Use

- Testing complete user journeys
- Validating system integration
- Smoke testing critical paths

#### Must Follow

- [ ] Use `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- [ ] Full Spring context
- [ ] Test complete workflows
- [ ] Minimal number of E2E tests

#### Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void should_complete_order_submission_flow() {
        // Given: Create customer and add items to cart
        // When: Submit order
        // Then: Verify order created, inventory reserved, email sent
    }
}
```

---

## BDD Testing with Cucumber

### Must Follow

- [ ] Write Gherkin scenarios before implementation
- [ ] Use Given-When-Then format
- [ ] Use ubiquitous language
- [ ] One scenario per business rule

### Gherkin Structure

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

### Step Definitions

```java
@Given("a customer with ID {string}")
public void aCustomerWithId(String customerId) {
    this.customerId = CustomerId.of(customerId);
    this.customer = createCustomer(customerId);
}

@When("the customer submits the order")
public void theCustomerSubmitsTheOrder() {
    submitOrderCommand = new SubmitOrderCommand(orderId);
    orderService.submitOrder(submitOrderCommand);
}

@Then("the order status should be {string}")
public void theOrderStatusShouldBe(String expectedStatus) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
}
```

**Detailed Guide**: #[[file:../examples/testing/bdd-cucumber-guide.md]]

---

## Test Performance

### Must Follow

- [ ] Use `@TestPerformanceExtension` for monitoring
- [ ] Unit tests: < 50ms, < 5MB
- [ ] Integration tests: < 500ms, < 50MB
- [ ] E2E tests: < 3s, < 500MB
- [ ] Clean up resources after tests

### Performance Monitoring

```java
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
@DataJpaTest
class OrderRepositoryTest {
    // Tests are automatically monitored
}
```

**Detailed Guide**: #[[file:../examples/testing/test-performance-guide.md]]

---

## Test Data Management

### Must Follow

- [ ] Use test data builders
- [ ] Create reusable test fixtures
- [ ] Use meaningful test data
- [ ] Clean state between tests

### Test Data Builder

```java
public class OrderTestDataBuilder {
    private OrderId orderId = OrderId.generate();
    private CustomerId customerId = CustomerId.of("CUST-001");
    
    public static OrderTestDataBuilder anOrder() {
        return new OrderTestDataBuilder();
    }
    
    public OrderTestDataBuilder withCustomerId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public Order build() {
        return new Order(orderId, customerId, "Test Address");
    }
}

// Usage
Order order = anOrder()
    .withCustomerId(customerId)
    .build();
```

---

## Test Organization

### Test Package Structure

```text
test/
├── java/
│   └── solid/humank/genaidemo/
│       ├── domain/          # Unit tests
│       ├── application/     # Application service tests
│       ├── infrastructure/  # Integration tests
│       └── bdd/             # BDD step definitions
└── resources/
    ├── features/            # Gherkin scenarios
    └── application-test.yml # Test configuration
```

### Test Naming

```java
// Unit test
class OrderTest { }

// Integration test
@DataJpaTest
class OrderRepositoryTest { }

// E2E test
@SpringBootTest
class OrderE2ETest { }
```

---

## Gradle Test Commands

### Daily Development

```bash
./gradlew quickTest          # Unit tests only (< 2 min)
./gradlew preCommitTest      # Unit + Integration (< 5 min)
./gradlew fullTest           # All tests including E2E
```

### Specific Test Types

```bash
./gradlew unitTest           # Fast unit tests
./gradlew integrationTest    # Integration tests
./gradlew e2eTest           # End-to-end tests
./gradlew cucumber          # BDD Cucumber tests
```

### Test Reports

```bash
./gradlew test jacocoTestReport              # Coverage report
./gradlew generatePerformanceReport          # Performance report
```

---

## Test Quality Standards

### Must Achieve

- [ ] Code coverage > 80%
- [ ] All tests pass
- [ ] No flaky tests
- [ ] Test execution time within limits
- [ ] No skipped tests in CI/CD

### Test Characteristics

- [ ] **Fast**: Quick feedback
- [ ] **Isolated**: Independent tests
- [ ] **Repeatable**: Same result every time
- [ ] **Self-validating**: Clear pass/fail
- [ ] **Timely**: Written with code

---

## Validation

### Coverage Check

```bash
./gradlew test jacocoTestReport
# Check: build/reports/jacoco/test/html/index.html
# Target: > 80% line coverage
```

### Performance Check

```bash
./gradlew generatePerformanceReport
# Check: build/reports/test-performance/performance-report.html
```

---

## Quick Reference

| Test Type | Annotation | Speed | Memory | Use Case |
|-----------|-----------|-------|--------|----------|
| Unit | `@ExtendWith(MockitoExtension.class)` | < 50ms | < 5MB | Domain logic |
| Integration | `@DataJpaTest`, `@WebMvcTest` | < 500ms | < 50MB | Infrastructure |
| E2E | `@SpringBootTest` | < 3s | < 500MB | Complete flows |
| BDD | Cucumber | Varies | Varies | Business scenarios |

---

## Related Documentation

- **Core Principles**: #[[file:core-principles.md]]
- **Code Quality Checklist**: #[[file:code-quality-checklist.md]]
- **Testing Examples**: #[[file:../examples/testing/]]

---

**Document Version**: 1.0
**Last Updated**: 2025-01-17
**Owner**: QA Team
