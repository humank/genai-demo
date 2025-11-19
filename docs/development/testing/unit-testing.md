# Unit Testing Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide provides comprehensive guidance for writing effective unit tests in the GenAI Demo project.

---

## Quick Reference

For complete testing standards, see:
- [Testing Strategy](testing-strategy.md) - Complete testing approach
- [Testing Standards](.kiro/steering/testing-strategy.md) - Testing development standards
- [BDD Testing](bdd-testing.md) - Behavior-driven development

---

## Unit Testing Principles

### What to Test

✅ **Do Test**:
- Business logic in domain layer
- Value object validation
- Aggregate root behavior
- Domain service logic
- Utility functions
- Calculations and transformations

❌ **Don't Test**:
- Framework code
- Simple getters/setters
- Configuration classes
- Infrastructure code (use integration tests)

---

## Test Structure

### Given-When-Then Pattern

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void should_create_customer_when_valid_data_provided() {
        // Given - Setup test data
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            "password123"
        );
        
        Customer expectedCustomer = Customer.builder()
            .id("customer-123")
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        when(customerRepository.save(any(Customer.class)))
            .thenReturn(expectedCustomer);
        
        // When - Execute the behavior
        Customer result = customerService.createCustomer(command);
        
        // Then - Verify the outcome
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        verify(customerRepository).save(any(Customer.class));
    }
}
```

---

## Test Naming Conventions

### Method Naming

Use descriptive names that explain the test:

```java
// ✅ Good: Clear and descriptive
@Test
void should_throw_exception_when_email_is_invalid()

@Test
void should_calculate_discount_correctly_for_premium_customer()

@Test
void should_reject_order_when_inventory_insufficient()

// ❌ Bad: Unclear purpose
@Test
void testCustomer()

@Test
void test1()
```

---

## Mocking Best Practices

### When to Mock

✅ **Mock**:
- External services (payment gateways, email services)
- Repositories in service tests
- Time-dependent operations
- Complex dependencies tested separately

❌ **Don't Mock**:
- Value objects and entities
- Simple data structures
- Domain logic being tested
- Infrastructure with in-memory alternatives

### Mocking Examples

```java
// ✅ Good: Specific, focused mocking
@Test
void should_send_welcome_email_when_customer_created() {
    // Given
    Customer customer = createCustomer();
    when(emailService.sendWelcomeEmail(customer.getEmail()))
        .thenReturn(EmailResult.success());
    
    // When
    customerService.createCustomer(customer);
    
    // Then
    verify(emailService).sendWelcomeEmail(customer.getEmail());
}

// ❌ Bad: Over-mocking
@Test
void should_create_customer() {
    when(customerRepository.save(any())).thenReturn(customer);
    when(eventPublisher.publish(any())).thenReturn(true);
    when(validator.validate(any())).thenReturn(ValidationResult.valid());
    when(idGenerator.generate()).thenReturn("id");
    // ... too many mocks
}
```

---

## Test Data Builders

### Builder Pattern

```java
public class CustomerTestDataBuilder {
    private String name = "John Doe";
    private String email = "john@example.com";
    private CustomerType type = CustomerType.REGULAR;
    
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    public CustomerTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public CustomerTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public CustomerTestDataBuilder premium() {
        this.type = CustomerType.PREMIUM;
        return this;
    }
    
    public Customer build() {
        return new Customer(name, email, type);
    }
}

// Usage
Customer customer = aCustomer()
    .withName("Jane Smith")
    .withEmail("jane@example.com")
    .premium()
    .build();
```

---

## Assertions

### AssertJ Best Practices

```java
// ✅ Good: Fluent, readable assertions
assertThat(customer).isNotNull();
assertThat(customer.getName()).isEqualTo("John Doe");
assertThat(customer.getOrders()).hasSize(3);
assertThat(customer.isActive()).isTrue();

// Exception assertions
assertThatThrownBy(() -> customer.submit())
    .isInstanceOf(BusinessRuleViolationException.class)
    .hasMessage("Cannot submit empty order");

// Collection assertions
assertThat(orders)
    .hasSize(2)
    .extracting(Order::getStatus)
    .containsExactly(OrderStatus.PENDING, OrderStatus.CONFIRMED);
```

---

## Test Performance

### Performance Standards

- **Unit tests**: < 50ms per test
- **Memory usage**: < 5MB per test
- **Success rate**: > 99%

### Performance Monitoring

```java
@TestPerformanceExtension(maxExecutionTimeMs = 50, maxMemoryIncreaseMB = 5)
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    // Tests are automatically monitored
}
```

---

## Common Patterns

### Testing Value Objects

```java
@Test
void should_create_valid_email() {
    Email email = Email.of("john@example.com");
    assertThat(email.getValue()).isEqualTo("john@example.com");
}

@Test
void should_throw_exception_for_invalid_email() {
    assertThatThrownBy(() -> Email.of("invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid email format");
}
```

### Testing Aggregate Roots

```java
@Test
void should_collect_event_when_order_submitted() {
    // Given
    Order order = createOrder();
    
    // When
    order.submit();
    
    // Then
    assertThat(order.hasUncommittedEvents()).isTrue();
    List<DomainEvent> events = order.getUncommittedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(OrderSubmittedEvent.class);
}
```

### Testing Domain Services

```java
@Test
void should_calculate_discount_correctly() {
    // Given
    PricingService pricingService = new PricingService();
    Order order = createOrderWithTotal(Money.of(100));
    Customer premiumCustomer = createPremiumCustomer();
    
    // When
    Money total = pricingService.calculateOrderTotal(order, premiumCustomer);
    
    // Then
    assertThat(total).isEqualTo(Money.of(90)); // 10% discount
}
```

---

## Test Organization

### Package Structure

```text
src/test/java/
└── solid/humank/genaidemo/
    ├── domain/
    │   ├── customer/
    │   │   ├── CustomerTest.java
    │   │   └── EmailTest.java
    │   └── order/
    │       ├── OrderTest.java
    │       └── OrderItemTest.java
    └── application/
        └── customer/
            └── CustomerApplicationServiceTest.java
```

---

## Test Coverage

### Coverage Requirements

- **Minimum**: 80% line coverage
- **Target**: 90% line coverage
- **Critical paths**: 100% coverage

### Checking Coverage

```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

---

## Troubleshooting

### Common Issues

**Issue**: Tests are slow
- **Solution**: Check for unnecessary mocks, use test data builders

**Issue**: Flaky tests
- **Solution**: Remove time dependencies, ensure test isolation

**Issue**: Hard to understand test failures
- **Solution**: Use descriptive assertions, add context to error messages

---

## Related Documentation

- [Testing Strategy](testing-strategy.md)
- [Integration Testing](integration-testing.md)
- [BDD Testing](bdd-testing.md)
- [Test Performance Standards](.kiro/steering/test-performance-standards.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Development Team
