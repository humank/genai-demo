---
inclusion: always
last_updated: 2026-02-21
---

# Development Guide

Complete guide for daily development work including testing, BDD/TDD, and code quality standards.

## Quick Reference

### Test Pyramid
- **Unit Tests (80%)**: < 50ms, < 5MB per test
- **Integration Tests (15%)**: < 500ms, < 50MB per test
- **E2E Tests (5%)**: < 3s, < 500MB per test

### Coverage & Quality
- Code coverage: > 80%
- Test execution: < 15s (unit tests)
- Architecture compliance: 100%

### Daily Commands
```bash
./gradlew quickTest              # Fast feedback (< 2 min)
./gradlew preCommitTest          # Pre-commit check (< 5 min)
./gradlew fullTest               # Complete suite
./gradlew archUnit               # Architecture validation
```

---

## Technology Stack

### Backend
- Spring Boot 3.4.5 + Java 21 + Gradle 8.x
- Spring Data JPA + Hibernate + Flyway
- H2 (dev/test) + PostgreSQL (prod)
- SpringDoc OpenAPI 3 + Swagger UI

### Frontend
- CMC Management: Next.js 16 + React 19 + TypeScript (Turborepo Monorepo)
- Consumer App: Next.js 15 + React 19 + TypeScript (Turborepo Monorepo)
- UI Components: shadcn/ui + Radix UI (@repo/ui 共用套件)

### Testing
- JUnit 5 + Mockito + AssertJ
- Cucumber 7 (BDD) + Gherkin
- ArchUnit (Architecture Testing)

---

## Testing Standards

### Unit Tests (Preferred)

**When to Use:**
- Testing domain logic in isolation
- Validating business rules
- Testing utility functions

**Must Follow:**
- Use `@ExtendWith(MockitoExtension.class)`
- No Spring context
- Mock external dependencies
- Fast execution (< 50ms)

**Example:**
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

    @Test
    void should_calculate_total_correctly() {
        // Given
        Order order = new Order(customerId, shippingAddress);
        order.addItem(new OrderItem(productId, 2, Money.of(100)));
        order.addItem(new OrderItem(productId2, 1, Money.of(50)));

        // When
        Money total = order.calculateTotal();

        // Then
        assertThat(total).isEqualTo(Money.of(250));
    }
}
```

### Integration Tests

**When to Use:**
- Testing repository implementations
- Validating database queries
- Testing API endpoints

**Must Follow:**
- Use `@DataJpaTest`, `@WebMvcTest`, or `@JsonTest`
- Partial Spring context only
- Use test database (H2)
- Clean state between tests

**Example:**
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
        Order order1 = createOrder(customerId);
        Order order2 = createOrder(customerId);
        Order order3 = createOrder(otherCustomerId);

        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        entityManager.persistAndFlush(order3);

        // When
        List<Order> results = repository.findByCustomerId(customerId);

        // Then
        assertThat(results).hasSize(2)
            .extracting(Order::getId)
            .containsExactlyInAnyOrder(order1.getId(), order2.getId());
    }
}
```

### E2E Tests

**When to Use:**
- Testing complete user journeys
- Validating system integration
- Smoke testing critical paths

**Must Follow:**
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- Full Spring context
- Test complete workflows
- Minimal number of E2E tests

**Example:**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class OrderE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    void should_complete_order_submission_flow() {
        // Given: Create customer
        CreateCustomerRequest customerRequest = new CreateCustomerRequest(
            "John Doe", "john@example.com", "password123"
        );
        ResponseEntity<CustomerResponse> customerResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/customers",
            customerRequest,
            CustomerResponse.class
        );
        String customerId = customerResponse.getBody().getId();

        // When: Submit order
        SubmitOrderRequest orderRequest = new SubmitOrderRequest(
            customerId,
            List.of(new OrderItemRequest(productId, 2))
        );
        ResponseEntity<OrderResponse> orderResponse = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/v1/orders",
            orderRequest,
            OrderResponse.class
        );

        // Then: Verify order created
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(orderResponse.getBody().getStatus()).isEqualTo("PENDING");
    }
}
```

---

## BDD Testing with Cucumber

### Gherkin Scenarios

**Must Follow:**
- Write scenarios before implementation
- Use Given-When-Then format
- Use ubiquitous language
- One scenario per business rule

**Example:**
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

---

## Test Performance

### Performance Monitoring

**Must Follow:**
- Use `@TestPerformanceExtension` for monitoring
- Unit tests: < 50ms, < 5MB
- Integration tests: < 500ms, < 50MB
- E2E tests: < 3s, < 500MB

**Example:**
```java
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
@DataJpaTest
class OrderRepositoryTest {
    // Tests are automatically monitored
}
```

### Performance Commands
```bash
./gradlew test jacocoTestReport              # Coverage report
./gradlew generatePerformanceReport          # Performance report
```

---

## Test Data Management

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

    public OrderTestDataBuilder withItem(ProductId productId, int quantity, Money price) {
        items.add(new OrderItem(productId, quantity, price));
        return this;
    }

    public Order build() {
        Order order = new Order(orderId, customerId, "Test Address");
        items.forEach(order::addItem);
        return order;
    }
}

// Usage
Order order = anOrder()
    .withCustomerId(customerId)
    .withItem(productId, 2, Money.of(100))
    .build();
```

---

## Error Handling

### Exception Design

**Must Follow:**
- Use specific exception types
- Include error context
- Log errors with structured data
- Use try-with-resources for closeable resources

**Example:**
```java
// Custom exception hierarchy
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;

    protected DomainException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : Map.of();
    }
}

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(String orderId) {
        super("ORDER_NOT_FOUND",
              "Order not found with id: " + orderId,
              Map.of("orderId", orderId));
    }
}

// Usage
@Service
public class OrderService {
    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
```

---

## API Design

### REST API Standards

**Must Follow:**
- RESTful URL conventions
- Proper HTTP methods (GET, POST, PUT, DELETE)
- Consistent response format
- Input validation with `@Valid`

**Example:**
```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        CreateOrderCommand command = new CreateOrderCommand(
            request.customerId(),
            request.items()
        );

        Order order = orderService.createOrder(command);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        Order order = orderService.findById(orderId);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

### HTTP Status Codes
- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST
- **204 No Content**: Successful DELETE
- **400 Bad Request**: Validation errors
- **404 Not Found**: Resource not found
- **409 Conflict**: Business rule violation
- **500 Internal Server Error**: System errors

---

## Code Quality

### Naming Conventions

**Must Follow:**
- Classes: PascalCase (e.g., `OrderService`)
- Methods: camelCase with verb-noun (e.g., `findCustomerById`)
- Variables: camelCase, descriptive (e.g., `customerEmail`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`)
- Test methods: `should_expectedBehavior_when_condition`

**Must Avoid:**
- Abbreviations (e.g., `cust` instead of `customer`)
- Single letter variables (except loop counters)
- Meaningless names (e.g., `data`, `info`, `manager`)

---

## Documentation Standards

### Documentation Dates

**🚨 CRITICAL - ALWAYS FIRST:**
```bash
# Get current date BEFORE any documentation work
date +%Y-%m-%d
```

**Must Update:**
- Frontmatter `last_updated` field
- Document header `Last Updated` field
- Change History table entries

**Never Use:**
- Placeholder dates (YYYY-MM-DD)
- Hardcoded old dates
- Copied dates from other files

---

## Validation Commands

```bash
# Code quality
./gradlew test jacocoTestReport  # Check coverage
./gradlew pmdMain                # Check code smells
./gradlew checkstyleMain         # Check style
./gradlew spotbugsMain           # Find bugs

# Architecture
./gradlew archUnit               # Verify architecture

# Documentation
node scripts/check-links-advanced.js --internal-only
./scripts/validate-diagrams.sh --check-references
```

---

## Related Guides

- **Architecture**: See `architecture-guide.md` for DDD patterns
- **Design**: See `design-guide.md` for SOLID principles
- **Security**: See `security-guide.md` for security standards
- **Performance**: See `performance-guide.md` for optimization

---

**Last Updated**: 2026-02-21
**Owner**: Development Team
