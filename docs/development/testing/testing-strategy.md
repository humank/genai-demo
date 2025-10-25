# Testing Strategy

> **Last Updated**: 2025-10-25

## Overview

This document defines the comprehensive testing strategy for the Enterprise E-Commerce Platform. Our testing approach follows the test pyramid principle and emphasizes Test-Driven Development (TDD) and Behavior-Driven Development (BDD) practices.

## Testing Philosophy

### Core Principles

1. **Test-First Development**: Write tests before implementation code
2. **Fast Feedback**: Tests should run quickly to enable rapid iteration
3. **Comprehensive Coverage**: Aim for >80% code coverage
4. **Maintainable Tests**: Tests should be as clean as production code
5. **Isolated Tests**: Tests should be independent and repeatable

### Test Pyramid

Our testing strategy follows the test pyramid model:

```
        /\
       /  \
      / E2E \     5% - End-to-End Tests
     /______\
    /        \
   /Integration\ 15% - Integration Tests
  /____________\
 /              \
/   Unit Tests   \ 80% - Unit Tests
/________________\
```

**Distribution:**
- **Unit Tests (80%)**: Fast, isolated tests of business logic
- **Integration Tests (15%)**: Tests of component interactions
- **End-to-End Tests (5%)**: Tests of complete user journeys

## Test Types

### 1. Unit Tests

**Purpose**: Test individual units of code in isolation

**Characteristics:**
- Fast execution (< 50ms per test)
- No external dependencies
- Use mocking for dependencies
- Focus on business logic

**When to Write:**
- Testing domain logic
- Testing value objects
- Testing utility functions
- Testing calculations and transformations

**Example:**

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    @Test
    void should_calculate_total_correctly() {
        // Given
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"));
        order.addItem(new OrderItem(ProductId.of("PROD-001"), 2, Money.of(50.00)));
        order.addItem(new OrderItem(ProductId.of("PROD-002"), 1, Money.of(30.00)));
        
        // When
        Money total = order.calculateTotal();
        
        // Then
        assertThat(total).isEqualTo(Money.of(130.00));
    }
    
    @Test
    void should_throw_exception_when_submitting_empty_order() {
        // Given
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"));
        
        // When & Then
        assertThatThrownBy(() -> order.submit())
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Cannot submit empty order");
    }
}
```

**Running Unit Tests:**

```bash
# Run all unit tests
./gradlew unitTest

# Run specific test class
./gradlew test --tests OrderTest

# Run with coverage
./gradlew test jacocoTestReport
```

### 2. Integration Tests

**Purpose**: Test integration between components and external systems

**Characteristics:**
- Moderate execution time (< 500ms per test)
- Use test database (H2 or Testcontainers)
- Test repository implementations
- Test API endpoints

**When to Write:**
- Testing repository implementations
- Testing database queries
- Testing REST API endpoints
- Testing message handling

**Example:**

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void should_find_orders_by_customer_id() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-001");
        Order order1 = createOrder(customerId);
        Order order2 = createOrder(customerId);
        
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        
        // When
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting(Order::getCustomerId)
            .containsOnly(customerId);
    }
}
```

**Running Integration Tests:**

```bash
# Run all integration tests
./gradlew integrationTest

# Run with specific profile
./gradlew integrationTest -Dspring.profiles.active=test
```

### 3. End-to-End Tests

**Purpose**: Test complete user journeys through the system

**Characteristics:**
- Slower execution (< 3s per test)
- Full Spring context
- Test complete workflows
- Minimal number of tests

**When to Write:**
- Testing critical user journeys
- Testing complete business processes
- Smoke testing after deployment

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
        ResponseEntity<CustomerResponse> customerResponse = 
            restTemplate.postForEntity("/api/v1/customers", customerRequest, CustomerResponse.class);
        
        assertThat(customerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String customerId = customerResponse.getBody().id();
        
        // When: Create and submit order
        CreateOrderRequest orderRequest = new CreateOrderRequest(
            customerId,
            List.of(new OrderItemRequest("PROD-001", 2))
        );
        ResponseEntity<OrderResponse> orderResponse = 
            restTemplate.postForEntity("/api/v1/orders", orderRequest, OrderResponse.class);
        
        // Then: Verify order created
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(orderResponse.getBody().status()).isEqualTo("PENDING");
    }
}
```

**Running E2E Tests:**

```bash
# Run all E2E tests
./gradlew e2eTest

# Run with full logging
./gradlew e2eTest --info
```

## Behavior-Driven Development (BDD)

### Gherkin Scenarios

**Purpose**: Define behavior in business language before implementation

**Structure**: Given-When-Then format

**Example Feature File:**

```gherkin
# src/test/resources/features/order-submission.feature

Feature: Order Submission
  As a customer
  I want to submit an order
  So that I can purchase products

  Scenario: Submit order successfully
    Given a customer with ID "CUST-001"
    And the customer has items in shopping cart
      | productId | quantity | price |
      | PROD-001  | 2        | 50.00 |
      | PROD-002  | 1        | 30.00 |
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved for the order items

  Scenario: Cannot submit empty order
    Given a customer with ID "CUST-001"
    And the customer has an empty shopping cart
    When the customer attempts to submit the order
    Then the order submission should fail
    And an error message "Cannot submit empty order" should be displayed
```

**Step Definitions:**

```java
@Component
public class OrderSubmissionSteps {
    
    private String customerId;
    private Order order;
    private Exception exception;
    
    @Given("a customer with ID {string}")
    public void aCustomerWithId(String customerId) {
        this.customerId = customerId;
    }
    
    @Given("the customer has items in shopping cart")
    public void theCustomerHasItemsInShoppingCart(DataTable dataTable) {
        List<Map<String, String>> items = dataTable.asMaps();
        order = new Order(OrderId.generate(), CustomerId.of(customerId));
        
        for (Map<String, String> item : items) {
            order.addItem(new OrderItem(
                ProductId.of(item.get("productId")),
                Integer.parseInt(item.get("quantity")),
                Money.of(new BigDecimal(item.get("price")))
            ));
        }
    }
    
    @When("the customer submits the order")
    public void theCustomerSubmitsTheOrder() {
        try {
            order.submit();
        } catch (Exception e) {
            exception = e;
        }
    }
    
    @Then("the order status should be {string}")
    public void theOrderStatusShouldBe(String expectedStatus) {
        assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
    }
}
```

**Running BDD Tests:**

```bash
# Run all Cucumber tests
./gradlew cucumber

# Run specific feature
./gradlew cucumber -Dcucumber.filter.tags="@order-submission"
```

## Test-Driven Development (TDD)

### TDD Cycle

**Red-Green-Refactor:**

1. **Red**: Write a failing test
2. **Green**: Write minimal code to make it pass
3. **Refactor**: Improve code quality

**Example TDD Session:**

```java
// Step 1: RED - Write failing test
@Test
void should_apply_discount_for_premium_customers() {
    // Given
    Customer customer = createPremiumCustomer();
    Order order = createOrderWithTotal(Money.of(100.00));
    
    // When
    Money discountedTotal = order.calculateTotalWithDiscount(customer);
    
    // Then
    assertThat(discountedTotal).isEqualTo(Money.of(90.00));
}

// Step 2: GREEN - Implement minimal code
public Money calculateTotalWithDiscount(Customer customer) {
    Money total = calculateTotal();
    if (customer.isPremium()) {
        return total.multiply(0.90);  // 10% discount
    }
    return total;
}

// Step 3: REFACTOR - Improve code
public Money calculateTotalWithDiscount(Customer customer) {
    Money total = calculateTotal();
    Discount discount = customer.getApplicableDiscount(total);
    return discount.apply(total);
}
```

## Test Organization

### Package Structure

```
src/test/
├── java/
│   └── solid/humank/genaidemo/
│       ├── domain/              # Unit tests for domain logic
│       │   ├── order/
│       │   ├── customer/
│       │   └── product/
│       ├── application/         # Application service tests
│       ├── infrastructure/      # Integration tests
│       │   ├── persistence/
│       │   └── messaging/
│       ├── interfaces/          # API tests
│       │   └── rest/
│       └── bdd/                 # BDD step definitions
│           └── steps/
└── resources/
    ├── features/                # Gherkin feature files
    ├── application-test.yml     # Test configuration
    └── test-data/               # Test data files
```

### Test Naming Conventions

**Test Classes:**
- Unit tests: `{ClassName}Test`
- Integration tests: `{ClassName}IntegrationTest`
- E2E tests: `{Feature}E2ETest`

**Test Methods:**
- Use `should_expectedBehavior_when_condition` format
- Be descriptive and specific
- Avoid abbreviations

```java
// ✅ Good
@Test
void should_create_customer_when_valid_data_provided() { }

@Test
void should_throw_exception_when_email_already_exists() { }

// ❌ Bad
@Test
void testCreateCustomer() { }

@Test
void test1() { }
```

## Test Data Management

### Test Data Builders

**Purpose**: Create test data in a readable and maintainable way

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
        Order order = new Order(orderId, customerId);
        items.forEach(order::addItem);
        return order;
    }
}

// Usage
Order order = anOrder()
    .withCustomerId(CustomerId.of("CUST-001"))
    .withItem(ProductId.of("PROD-001"), 2, Money.of(50.00))
    .withItem(ProductId.of("PROD-002"), 1, Money.of(30.00))
    .build();
```

### Test Fixtures

**Purpose**: Reusable test data setup

```java
public class TestFixtures {
    
    public static Customer createCustomer() {
        return new Customer(
            CustomerId.of("CUST-001"),
            new CustomerName("John Doe"),
            new Email("john@example.com"),
            MembershipLevel.STANDARD
        );
    }
    
    public static Customer createPremiumCustomer() {
        return new Customer(
            CustomerId.of("CUST-002"),
            new CustomerName("Jane Smith"),
            new Email("jane@example.com"),
            MembershipLevel.PREMIUM
        );
    }
    
    public static Order createOrderWithItems(Money totalAmount) {
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"));
        order.addItem(new OrderItem(
            ProductId.of("PROD-001"),
            1,
            totalAmount
        ));
        return order;
    }
}
```

## Mocking Strategy

### When to Mock

**Mock External Dependencies:**
- External services (payment gateway, email service)
- Repositories (in service tests)
- Time-dependent operations
- Complex dependencies tested separately

**Don't Mock:**
- Value objects
- Entities
- Domain logic being tested
- Simple data structures

### Mocking Examples

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void should_process_order_successfully() {
        // Given
        Order order = createOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentService.processPayment(any())).thenReturn(PaymentResult.success());
        
        // When
        orderService.processOrder(order.getId());
        
        // Then
        verify(orderRepository).save(order);
        verify(emailService).sendOrderConfirmation(order.getCustomerId());
    }
}
```

## Test Coverage

### Coverage Goals

- **Overall Coverage**: > 80%
- **Domain Layer**: > 90%
- **Application Layer**: > 85%
- **Infrastructure Layer**: > 70%

### Measuring Coverage

```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View report
open app/build/reports/jacoco/test/html/index.html

# Check coverage threshold
./gradlew jacocoTestCoverageVerification
```

### Coverage Configuration

```gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80
            }
        }
        rule {
            element = 'PACKAGE'
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
            excludes = [
                '*.config.*',
                '*.dto.*'
            ]
        }
    }
}
```

## Test Performance

### Performance Standards

- **Unit Tests**: < 50ms per test, < 5MB memory
- **Integration Tests**: < 500ms per test, < 50MB memory
- **E2E Tests**: < 3s per test, < 500MB memory

### Performance Monitoring

```java
@TestPerformanceExtension(maxExecutionTimeMs = 500, maxMemoryIncreaseMB = 50)
@DataJpaTest
class OrderRepositoryTest {
    // Tests are automatically monitored for performance
}
```

### Optimization Tips

1. **Use Test Slices**: `@DataJpaTest`, `@WebMvcTest` instead of `@SpringBootTest`
2. **Mock External Services**: Avoid real HTTP calls
3. **Use In-Memory Database**: H2 for fast tests
4. **Parallel Execution**: Configure Gradle for parallel tests
5. **Clean Up Resources**: Properly close connections and streams

## Continuous Integration

### CI Pipeline

```yaml
# .github/workflows/test.yml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Unit Tests
        run: ./gradlew unitTest
      
      - name: Run Integration Tests
        run: ./gradlew integrationTest
      
      - name: Run E2E Tests
        run: ./gradlew e2eTest
      
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
```

## Best Practices

### Test Writing Guidelines

1. **Arrange-Act-Assert (AAA)**: Structure tests clearly
2. **One Assertion Per Test**: Focus on single behavior
3. **Test Behavior, Not Implementation**: Test what, not how
4. **Use Descriptive Names**: Make test intent clear
5. **Keep Tests Independent**: No test dependencies
6. **Clean Up After Tests**: Reset state properly

### Common Pitfalls to Avoid

1. **Testing Implementation Details**: Focus on behavior
2. **Brittle Tests**: Don't couple tests to implementation
3. **Slow Tests**: Keep tests fast
4. **Flaky Tests**: Ensure tests are deterministic
5. **Over-Mocking**: Don't mock everything
6. **Under-Testing**: Achieve adequate coverage

## Troubleshooting

### Common Issues

**Issue: Tests Pass Locally But Fail in CI**

**Solution:**
- Check for environment-specific configuration
- Ensure test data is not environment-dependent
- Verify timezone and locale settings
- Check for race conditions in parallel execution

**Issue: Slow Test Execution**

**Solution:**
- Profile tests to identify slow tests
- Use test slices instead of full Spring context
- Mock external services
- Use in-memory database
- Enable parallel execution

**Issue: Flaky Tests**

**Solution:**
- Identify non-deterministic behavior
- Fix timing issues with proper waits
- Ensure proper test isolation
- Check for shared mutable state

## Related Documentation

- [Coding Standards](../coding-standards/java-standards.md)
- [BDD Testing Guide](bdd-testing.md)
- [Unit Testing Guide](unit-testing.md)
- [Integration Testing Guide](integration-testing.md)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: QA Team
