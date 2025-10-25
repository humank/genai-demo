# Java Coding Standards

> **Last Updated**: 2025-10-25

## Overview

This document defines the Java coding standards for the Enterprise E-Commerce Platform. These standards ensure code consistency, maintainability, and quality across the entire codebase.

## Naming Conventions

### Classes and Interfaces

**Classes:**
- Use **PascalCase** (UpperCamelCase)
- Use nouns or noun phrases
- Be specific and descriptive

```java
// ✅ Good
public class CustomerService { }
public class OrderRepository { }
public class PaymentProcessor { }

// ❌ Bad
public class customer { }           // Wrong case
public class CS { }                 // Too abbreviated
public class Manager { }            // Too generic
```

**Interfaces:**
- Use **PascalCase**
- Use nouns or adjectives
- Avoid "I" prefix

```java
// ✅ Good
public interface OrderRepository { }
public interface Serializable { }
public interface Comparable<T> { }

// ❌ Bad
public interface IOrderRepository { }  // Avoid "I" prefix
public interface orderRepository { }   // Wrong case
```

**Abstract Classes:**
- Use **PascalCase**
- Consider "Abstract" prefix or "Base" suffix for clarity

```java
// ✅ Good
public abstract class AbstractDomainEventHandler<T> { }
public abstract class BaseEntity { }

// ❌ Bad
public abstract class DomainEventHandler { }  // Not clear it's abstract
```

### Methods

**Method Names:**
- Use **camelCase** (lowerCamelCase)
- Start with a verb
- Be descriptive of the action

```java
// ✅ Good
public Customer findCustomerById(String id) { }
public void processOrder(Order order) { }
public boolean isEligibleForDiscount(Customer customer) { }
public List<Product> searchProductsByCategory(String category) { }

// ❌ Bad
public Customer customer(String id) { }        // Missing verb
public void process(Order order) { }           // Too generic
public boolean discount(Customer customer) { } // Not descriptive
```

**Boolean Methods:**
- Use `is`, `has`, `can`, `should` prefixes

```java
// ✅ Good
public boolean isActive() { }
public boolean hasPermission() { }
public boolean canProcess() { }
public boolean shouldRetry() { }

// ❌ Bad
public boolean active() { }
public boolean permission() { }
```

**Getter/Setter Methods:**
- Use `get`/`set` prefix for properties
- Use `is` prefix for boolean getters

```java
// ✅ Good
public String getName() { }
public void setName(String name) { }
public boolean isActive() { }

// ❌ Bad
public String name() { }              // Missing get prefix
public void name(String name) { }     // Missing set prefix
public boolean getActive() { }        // Use 'is' for boolean
```

### Variables

**Local Variables and Parameters:**
- Use **camelCase**
- Use descriptive names
- Avoid single-letter names (except loop counters)

```java
// ✅ Good
String customerEmail = "john@example.com";
int orderCount = 10;
List<Product> availableProducts = new ArrayList<>();

// ❌ Bad
String e = "john@example.com";        // Too short
int cnt = 10;                         // Abbreviated
List<Product> list = new ArrayList<>();  // Too generic
```

**Constants:**
- Use **UPPER_SNAKE_CASE**
- Declare as `static final`
- Group related constants

```java
// ✅ Good
public static final int MAX_RETRY_ATTEMPTS = 3;
public static final String DEFAULT_CURRENCY = "USD";
public static final long TIMEOUT_MILLISECONDS = 5000L;

// ❌ Bad
public static final int maxRetryAttempts = 3;  // Wrong case
public static final String currency = "USD";   // Not descriptive
```

**Instance Variables:**
- Use **camelCase**
- Use descriptive names
- Consider `private` access by default

```java
// ✅ Good
private String customerId;
private LocalDateTime createdAt;
private OrderStatus status;

// ❌ Bad
private String id;                    // Too generic
private LocalDateTime date;           // Not specific
public String customerId;             // Should be private
```

### Packages

**Package Names:**
- Use **lowercase**
- Use singular nouns
- Follow reverse domain name convention

```java
// ✅ Good
package solid.humank.genaidemo.domain.order.model;
package solid.humank.genaidemo.application.customer;
package solid.humank.genaidemo.infrastructure.persistence;

// ❌ Bad
package solid.humank.genaidemo.Domain.Order.Model;  // Wrong case
package solid.humank.genaidemo.domain.orders;       // Use singular
```

## Code Organization

### Class Structure

**Order of Elements:**

1. Static constants
2. Static variables
3. Instance variables
4. Constructors
5. Static methods
6. Instance methods
7. Nested classes

```java
public class OrderService {
    // 1. Static constants
    private static final int MAX_ITEMS = 100;
    
    // 2. Static variables
    private static AtomicLong orderCounter = new AtomicLong(0);
    
    // 3. Instance variables
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    
    // 4. Constructors
    public OrderService(OrderRepository orderRepository, 
                       EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
    
    // 5. Static methods
    public static long getNextOrderNumber() {
        return orderCounter.incrementAndGet();
    }
    
    // 6. Instance methods
    public Order createOrder(CreateOrderCommand command) {
        // Implementation
    }
    
    private void validateOrder(Order order) {
        // Implementation
    }
    
    // 7. Nested classes
    private static class OrderValidator {
        // Implementation
    }
}
```

### Method Organization

**Method Length:**
- Keep methods short (< 20 lines preferred)
- Extract complex logic into separate methods
- One level of abstraction per method

```java
// ✅ Good: Short, focused methods
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);
    applyDiscounts(order);
    reserveInventory(order);
    processPayment(order);
    sendConfirmation(order);
}

private void validateOrder(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Order must contain items");
    }
}

// ❌ Bad: Long, complex method
public void processOrder(Order order) {
    // 50+ lines of mixed validation, calculation, and processing
    if (order != null && !order.getItems().isEmpty()) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            // Complex calculation logic...
        }
        // More complex logic...
    }
}
```

### Parameter Lists

**Limit Parameters:**
- Maximum 3-4 parameters per method
- Use parameter objects for more parameters
- Use builder pattern for complex objects

```java
// ✅ Good: Limited parameters
public Customer createCustomer(String name, String email, String phone) {
    // Implementation
}

// ✅ Good: Parameter object for many parameters
public Customer createCustomer(CreateCustomerRequest request) {
    // Implementation
}

// ❌ Bad: Too many parameters
public Customer createCustomer(String name, String email, String phone,
                              String address, String city, String state,
                              String zipCode, String country) {
    // Implementation
}
```

## Code Style

### Formatting

**Indentation:**
- Use 4 spaces for indentation
- No tabs

**Line Length:**
- Maximum 120 characters per line
- Break long lines at logical points

**Braces:**
- Opening brace on same line
- Closing brace on new line
- Always use braces, even for single statements

```java
// ✅ Good
if (condition) {
    doSomething();
}

// ❌ Bad
if (condition)
    doSomething();  // Missing braces

if (condition) { doSomething(); }  // Not on separate lines
```

**Whitespace:**
- One blank line between methods
- One blank line between logical sections
- Space after keywords (`if`, `for`, `while`)
- Space around operators

```java
// ✅ Good
public void processOrder(Order order) {
    validateOrder(order);
    
    if (order.isValid()) {
        calculateTotal(order);
        processPayment(order);
    }
}

// ❌ Bad
public void processOrder(Order order){
    validateOrder(order);
    if(order.isValid()){
        calculateTotal(order);
        processPayment(order);
    }
}
```

### Imports

**Import Organization:**
- No wildcard imports
- Group imports logically
- Remove unused imports

```java
// ✅ Good
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.order.model.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;

// ❌ Bad
import java.util.*;                    // Wildcard import
import solid.humank.genaidemo.domain.order.model.*;  // Wildcard import
```

## Best Practices

### Use Java Records for Immutable Data

```java
// ✅ Good: Use Record for immutable data
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }
}

// ❌ Bad: Verbose class for simple data
public class CustomerId {
    private final String value;
    
    public CustomerId(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        // Boilerplate code...
    }
    
    @Override
    public int hashCode() {
        // Boilerplate code...
    }
}
```

### Use Optional for Nullable Returns

```java
// ✅ Good: Use Optional
public Optional<Customer> findCustomerById(String id) {
    return customerRepository.findById(id);
}

// Usage
Optional<Customer> customer = findCustomerById("123");
customer.ifPresent(c -> processCustomer(c));

// ❌ Bad: Return null
public Customer findCustomerById(String id) {
    return customerRepository.findById(id);  // May return null
}
```

### Use Streams for Collections

```java
// ✅ Good: Use streams
List<String> activeCustomerEmails = customers.stream()
    .filter(Customer::isActive)
    .map(Customer::getEmail)
    .collect(Collectors.toList());

// ❌ Bad: Imperative style
List<String> activeCustomerEmails = new ArrayList<>();
for (Customer customer : customers) {
    if (customer.isActive()) {
        activeCustomerEmails.add(customer.getEmail());
    }
}
```

### Use Dependency Injection

```java
// ✅ Good: Constructor injection
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    
    public OrderService(OrderRepository orderRepository,
                       EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }
}

// ❌ Bad: Field injection
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private EventPublisher eventPublisher;
}
```

### Use Specific Exceptions

```java
// ✅ Good: Specific exceptions
public Customer findCustomerById(String id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new CustomerNotFoundException(
            "Customer not found with ID: " + id));
}

// ❌ Bad: Generic exception
public Customer findCustomerById(String id) {
    Customer customer = customerRepository.findById(id);
    if (customer == null) {
        throw new RuntimeException("Not found");
    }
    return customer;
}
```

## Documentation

### JavaDoc Comments

**When to Use JavaDoc:**
- Public classes and interfaces
- Public and protected methods
- Complex algorithms
- Non-obvious behavior

```java
/**
 * Service for managing customer operations.
 * 
 * This service handles customer registration, profile updates,
 * and account management. It integrates with the email service
 * for notifications.
 * 
 * @author Development Team
 * @since 1.0
 */
@Service
public class CustomerService {
    
    /**
     * Creates a new customer account.
     * 
     * @param command the customer creation command containing all required information
     * @return the created customer with generated ID and timestamps
     * @throws EmailAlreadyExistsException if the email is already registered
     * @throws ValidationException if the customer information is invalid
     */
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation
    }
}
```

### Inline Comments

**When to Use Inline Comments:**
- Complex business logic
- Non-obvious algorithms
- Workarounds or temporary solutions
- Important decisions

```java
// ✅ Good: Explains complex logic
public Money calculateDiscount(Order order, Customer customer) {
    // Premium customers get 10% discount on orders over $100
    // This is a business rule defined in BR-2024-001
    if (customer.isPremium() && order.getTotal().isGreaterThan(Money.of(100))) {
        return order.getTotal().multiply(0.10);
    }
    return Money.zero();
}

// ❌ Bad: States the obvious
public void setName(String name) {
    // Set the name
    this.name = name;
}
```

## Testing Standards

### Test Naming

```java
// ✅ Good: Descriptive test names
@Test
void should_create_customer_when_valid_data_provided() {
    // Test implementation
}

@Test
void should_throw_exception_when_email_already_exists() {
    // Test implementation
}

// ❌ Bad: Unclear test names
@Test
void testCreateCustomer() {
    // Test implementation
}

@Test
void test1() {
    // Test implementation
}
```

### Test Structure

```java
// ✅ Good: Given-When-Then structure
@Test
void should_calculate_total_with_discount() {
    // Given
    Order order = createOrderWithItems(100.00);
    Customer customer = createPremiumCustomer();
    
    // When
    Money total = orderService.calculateTotal(order, customer);
    
    // Then
    assertThat(total).isEqualTo(Money.of(90.00));
}
```

## Code Review Checklist

Before submitting code for review, ensure:

- [ ] Code follows naming conventions
- [ ] Methods are short and focused
- [ ] No code duplication
- [ ] Proper error handling
- [ ] Tests are included
- [ ] JavaDoc for public APIs
- [ ] No compiler warnings
- [ ] Code is formatted correctly
- [ ] Imports are organized
- [ ] No commented-out code

## Tools and Automation

### Static Analysis

```bash
# Run Checkstyle
./gradlew checkstyleMain

# Run PMD
./gradlew pmdMain

# Run SpotBugs
./gradlew spotbugsMain
```

### Code Formatting

```bash
# Format code (IntelliJ)
Cmd+Alt+L (Mac) or Ctrl+Alt+L (Windows/Linux)

# Organize imports
Ctrl+Alt+O (Mac) or Ctrl+Alt+O (Windows/Linux)
```

## Related Documentation

- [Design Principles](../../architecture/principles/design-principles.md)
- [DDD Tactical Patterns](../../architecture/patterns/ddd-patterns.md)
- [Code Review Guide](../workflows/code-review.md)
- [Testing Strategy](../testing/testing-strategy.md)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
