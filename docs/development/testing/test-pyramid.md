# Test Pyramid Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide explains the test pyramid approach used in the GenAI Demo project.

---

## Quick Reference

For complete testing guidance, see:
- [Testing Strategy](testing-strategy.md) - Complete testing approach
- [Testing Standards](.kiro/steering/testing-strategy.md) - Testing development standards

---

## The Test Pyramid

```
        /\
       /  \
      / E2E \      5% - End-to-End Tests
     /______\
    /        \
   /Integration\ 15% - Integration Tests
  /____________\
 /              \
/   Unit Tests   \ 80% - Unit Tests
/________________\
```

### Distribution

- **Unit Tests (80%)**: Fast, isolated, focused on business logic
- **Integration Tests (15%)**: Test component interactions
- **E2E Tests (5%)**: Test complete user journeys

---

## Why the Pyramid?

### Benefits

✅ **Fast Feedback**: Majority of tests run quickly
✅ **Cost Effective**: Unit tests are cheaper to write and maintain
✅ **Easy to Debug**: Failures are easier to isolate
✅ **Stable**: Less flaky than E2E tests
✅ **Comprehensive**: Good coverage at all levels

### Anti-Pattern: Ice Cream Cone

```
        /\
       /  \
      /    \
     /  E2E \     ❌ Too many E2E tests
    /________\
   /          \
  / Integration\ ❌ Some integration tests
 /____________\
/              \
/     Unit      \ ❌ Few unit tests
/________________\
```

**Problems**:
- Slow test execution
- Flaky tests
- Hard to debug
- Expensive to maintain

---

## Unit Tests (80%)

### Characteristics

- **Fast**: < 50ms per test
- **Isolated**: No external dependencies
- **Focused**: Test single units of code
- **Deterministic**: Same input = same output

### What to Test

✅ **Business Logic**: Domain models, services
✅ **Validation**: Input validation, business rules
✅ **Calculations**: Pricing, discounts, totals
✅ **Transformations**: Data mapping, formatting

### Example

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    @Test
    void should_calculate_total_with_discount() {
        // Given
        Order order = Order.builder()
            .addItem(Product.of("PROD-001", Money.of(100)), 2)
            .discount(Percentage.of(10))
            .build();
        
        // When
        Money total = order.calculateTotal();
        
        // Then
        assertThat(total).isEqualTo(Money.of(180)); // 200 - 10%
    }
}
```

### Performance Standards

- **Execution Time**: < 50ms per test
- **Memory Usage**: < 5MB per test
- **Success Rate**: > 99%

---

## Integration Tests (15%)

### Characteristics

- **Moderate Speed**: < 500ms per test
- **Partial Integration**: Test component interactions
- **Real Infrastructure**: Use test databases, message queues
- **Isolated**: Clean state between tests

### What to Test

✅ **Repository Operations**: Database queries, transactions
✅ **API Endpoints**: REST controllers, request/response
✅ **Message Handling**: Event publishers, consumers
✅ **External Services**: Third-party integrations (mocked)

### Example

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
        Customer customer = createCustomer();
        Order order1 = createOrder(customer);
        Order order2 = createOrder(customer);
        
        entityManager.persistAndFlush(customer);
        entityManager.persistAndFlush(order1);
        entityManager.persistAndFlush(order2);
        
        // When
        List<Order> orders = orderRepository.findByCustomerId(customer.getId());
        
        // Then
        assertThat(orders).hasSize(2);
    }
}
```

### Performance Standards

- **Execution Time**: < 500ms per test
- **Memory Usage**: < 50MB per test
- **Success Rate**: > 95%

---

## E2E Tests (5%)

### Characteristics

- **Slow**: < 3s per test
- **Complete Integration**: Full system test
- **Real Environment**: Production-like setup
- **User-Focused**: Test user journeys

### What to Test

✅ **Critical Paths**: Order submission, payment processing
✅ **User Journeys**: Registration to purchase
✅ **System Integration**: All components working together
✅ **Smoke Tests**: Basic functionality verification

### Example

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class OrderE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void should_complete_order_submission_flow() {
        // Given: Create customer
        CustomerResponse customer = createCustomer();
        
        // When: Add items to cart
        addItemToCart(customer.id(), "PROD-001", 2);
        addItemToCart(customer.id(), "PROD-002", 1);
        
        // And: Submit order
        OrderResponse order = submitOrder(customer.id());
        
        // Then: Verify order created
        assertThat(order.status()).isEqualTo("PENDING");
        assertThat(order.items()).hasSize(2);
        
        // And: Verify inventory reserved
        verifyInventoryReserved(order.id());
        
        // And: Verify confirmation email sent
        verifyEmailSent(customer.email(), "Order Confirmation");
    }
}
```

### Performance Standards

- **Execution Time**: < 3s per test
- **Memory Usage**: < 500MB per test
- **Success Rate**: > 90%

---

## Test Distribution Guidelines

### By Layer

```
Domain Layer:
  - Unit Tests: 90%
  - Integration Tests: 10%
  - E2E Tests: 0%

Application Layer:
  - Unit Tests: 70%
  - Integration Tests: 30%
  - E2E Tests: 0%

Infrastructure Layer:
  - Unit Tests: 30%
  - Integration Tests: 70%
  - E2E Tests: 0%

Complete System:
  - Unit Tests: 0%
  - Integration Tests: 0%
  - E2E Tests: 100%
```

### By Feature

```
Critical Features (Payment, Checkout):
  - Unit Tests: 75%
  - Integration Tests: 20%
  - E2E Tests: 5%

Standard Features (Product Catalog):
  - Unit Tests: 80%
  - Integration Tests: 15%
  - E2E Tests: 5%

Low-Risk Features (UI Preferences):
  - Unit Tests: 85%
  - Integration Tests: 15%
  - E2E Tests: 0%
```

---

## Balancing the Pyramid

### When to Add More Unit Tests

- Complex business logic
- Many edge cases
- Calculation-heavy code
- Validation rules

### When to Add More Integration Tests

- Database queries
- API endpoints
- Message handling
- External service integration

### When to Add More E2E Tests

- Critical user journeys
- Payment flows
- Security-sensitive operations
- Regulatory compliance

---

## Anti-Patterns to Avoid

### 1. Testing Through the UI

```java
// ❌ Bad: Testing business logic through UI
@Test
void testOrderCalculation() {
    driver.get("/order");
    driver.findElement(By.id("product")).sendKeys("PROD-001");
    driver.findElement(By.id("quantity")).sendKeys("2");
    driver.findElement(By.id("calculate")).click();
    String total = driver.findElement(By.id("total")).getText();
    assertEquals("$200", total);
}

// ✅ Good: Test business logic directly
@Test
void should_calculate_order_total() {
    Order order = createOrderWithItems(2, Money.of(100));
    Money total = order.calculateTotal();
    assertThat(total).isEqualTo(Money.of(200));
}
```

### 2. Too Many Mocks in Integration Tests

```java
// ❌ Bad: Mocking everything in integration test
@DataJpaTest
class OrderRepositoryTest {
    @Mock private EntityManager entityManager;
    @Mock private TransactionManager transactionManager;
    // ... more mocks
}

// ✅ Good: Use real infrastructure
@DataJpaTest
class OrderRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private OrderRepository orderRepository;
}
```

### 3. Slow Unit Tests

```java
// ❌ Bad: Unit test with database
@Test
void testOrderCreation() {
    Order order = new Order();
    orderRepository.save(order); // Database call!
}

// ✅ Good: Pure unit test
@Test
void should_create_order_with_valid_data() {
    Order order = Order.builder()
        .customerId("CUST-001")
        .build();
    assertThat(order.getCustomerId()).isEqualTo("CUST-001");
}
```

---

## Measuring Pyramid Health

### Metrics to Track

```bash
# Test count by type
./gradlew test --info | grep "tests completed"

# Test execution time
./gradlew test --profile

# Test coverage
./gradlew test jacocoTestReport
```

### Healthy Pyramid Indicators

✅ **Fast Build**: < 5 minutes for all tests
✅ **High Coverage**: > 80% code coverage
✅ **Low Flakiness**: < 1% flaky tests
✅ **Good Distribution**: 80/15/5 ratio maintained

---

## Related Documentation

- [Testing Strategy](testing-strategy.md)
- [Unit Testing](unit-testing.md)
- [Integration Testing](integration-testing.md)
- [E2E Testing](e2e-testing.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Development Team
