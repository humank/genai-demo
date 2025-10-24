# Unit Testing Guide

## Overview

Unit tests are the foundation of the test pyramid, focusing on testing individual components in isolation without external dependencies. They should be fast, focused, and provide immediate feedback during development.

**Purpose**: Test business logic, domain models, and utility functions in complete isolation.

**Key Characteristics**:
- **Fast**: < 50ms per test
- **Lightweight**: < 5MB memory usage
- **Isolated**: No Spring context, no database, no external services
- **Focused**: Test one thing at a time

---

## When to Use Unit Tests

### ✅ Perfect For

- **Domain Logic**: Aggregates, entities, value objects
- **Business Rules**: Validation, calculations, transformations
- **Utility Functions**: Helpers, formatters, converters
- **Configuration Classes**: Simple configuration logic
- **Pure Functions**: Functions without side effects

### ❌ Not Suitable For

- Database operations (use Integration Tests)
- REST API endpoints (use Integration Tests)
- External service integration (use Integration Tests)
- Full Spring context behavior (use E2E Tests)

---

## Basic Setup

### Required Dependencies

```gradle
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.mockito:mockito-core:5.5.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.5.0'
    testImplementation 'org.assertj:assertj-core:3.24.2'
}
```

### Test Class Structure

```java
package solid.humank.genaidemo.domain.customer.model.aggregate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for Customer aggregate
 * 
 * Memory: ~5MB
 * Speed: ~50ms per test
 * 
 * Tests business logic without Spring context
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Aggregate Unit Tests")
class CustomerTest {
    
    @Test
    @DisplayName("Should create customer with valid information")
    void shouldCreateCustomerWithValidInformation() {
        // Test implementation
    }
}
```

---

## Test Structure: Given-When-Then

All tests should follow the Given-When-Then (Arrange-Act-Assert) pattern for clarity.

### Example: Testing Value Object

```java
@Test
@DisplayName("Should create valid email address")
void shouldCreateValidEmailAddress() {
    // Given - Arrange test data
    String validEmail = "customer@example.com";
    
    // When - Execute the behavior
    Email email = Email.of(validEmail);
    
    // Then - Verify the outcome
    assertThat(email).isNotNull();
    assertThat(email.value()).isEqualTo(validEmail);
}

@Test
@DisplayName("Should throw exception for invalid email format")
void shouldThrowExceptionForInvalidEmailFormat() {
    // Given
    String invalidEmail = "not-an-email";
    
    // When & Then - Combined for exception testing
    assertThatThrownBy(() -> Email.of(invalidEmail))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid email format");
}
```

---

## Testing Domain Logic

### Testing Aggregate Roots

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Aggregate Tests")
class CustomerTest {
    
    @Test
    @DisplayName("Should collect CustomerCreated event when customer is created")
    void shouldCollectCustomerCreatedEventWhenCustomerIsCreated() {
        // Given
        CustomerId customerId = CustomerId.generate();
        CustomerName name = new CustomerName("John Doe");
        Email email = Email.of("john@example.com");
        MembershipLevel level = MembershipLevel.BRONZE;
        
        // When
        Customer customer = new Customer(customerId, name, email, level);
        
        // Then
        assertThat(customer.hasUncommittedEvents()).isTrue();
        
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerCreatedEvent.class);
        
        CustomerCreatedEvent event = (CustomerCreatedEvent) events.get(0);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.customerName()).isEqualTo(name);
        assertThat(event.email()).isEqualTo(email);
    }
    
    @Test
    @DisplayName("Should upgrade membership level when spending threshold reached")
    void shouldUpgradeMembershipLevelWhenSpendingThresholdReached() {
        // Given
        Customer customer = createCustomer(MembershipLevel.BRONZE);
        Money spendingAmount = Money.of(50000, Currency.getInstance("TWD"));
        
        // When
        customer.recordPurchase(spendingAmount);
        
        // Then
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.SILVER);
        assertThat(customer.hasUncommittedEvents()).isTrue();
        
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).anyMatch(e -> e instanceof MembershipLevelUpgradedEvent);
    }
    
    @Test
    @DisplayName("Should throw exception when email is already registered")
    void shouldThrowExceptionWhenEmailIsAlreadyRegistered() {
        // Given
        Email duplicateEmail = Email.of("existing@example.com");
        
        // When & Then
        assertThatThrownBy(() -> {
            // Simulate business rule validation
            if (isEmailAlreadyRegistered(duplicateEmail)) {
                throw new EmailAlreadyRegisteredException(duplicateEmail);
            }
        })
        .isInstanceOf(EmailAlreadyRegisteredException.class)
        .hasMessageContaining("existing@example.com");
    }
    
    // Helper methods
    private Customer createCustomer(MembershipLevel level) {
        return new Customer(
            CustomerId.generate(),
            new CustomerName("Test Customer"),
            Email.of("test@example.com"),
            level
        );
    }
    
    private boolean isEmailAlreadyRegistered(Email email) {
        // Simulate check - in real test, this would be mocked
        return email.value().equals("existing@example.com");
    }
}
```

### Testing Value Objects

```java
@DisplayName("Money Value Object Tests")
class MoneyTest {
    
    @Test
    @DisplayName("Should create money with valid amount and currency")
    void shouldCreateMoneyWithValidAmountAndCurrency() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        Currency currency = Currency.getInstance("TWD");
        
        // When
        Money money = Money.of(amount, currency);
        
        // Then
        assertThat(money.amount()).isEqualByComparingTo(amount);
        assertThat(money.currency()).isEqualTo(currency);
    }
    
    @Test
    @DisplayName("Should add two money amounts with same currency")
    void shouldAddTwoMoneyAmountsWithSameCurrency() {
        // Given
        Money money1 = Money.of(new BigDecimal("100.00"), Currency.getInstance("TWD"));
        Money money2 = Money.of(new BigDecimal("50.00"), Currency.getInstance("TWD"));
        
        // When
        Money result = money1.add(money2);
        
        // Then
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.currency()).isEqualTo(Currency.getInstance("TWD"));
    }
    
    @Test
    @DisplayName("Should throw exception when adding different currencies")
    void shouldThrowExceptionWhenAddingDifferentCurrencies() {
        // Given
        Money twd = Money.of(new BigDecimal("100.00"), Currency.getInstance("TWD"));
        Money usd = Money.of(new BigDecimal("50.00"), Currency.getInstance("USD"));
        
        // When & Then
        assertThatThrownBy(() -> twd.add(usd))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot add different currencies");
    }
    
    @Test
    @DisplayName("Should multiply money by factor")
    void shouldMultiplyMoneyByFactor() {
        // Given
        Money money = Money.of(new BigDecimal("100.00"), Currency.getInstance("TWD"));
        int factor = 3;
        
        // When
        Money result = money.multiply(factor);
        
        // Then
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("300.00"));
    }
    
    @Test
    @DisplayName("Should compare money amounts correctly")
    void shouldCompareMoneyAmountsCorrectly() {
        // Given
        Money smaller = Money.of(new BigDecimal("50.00"), Currency.getInstance("TWD"));
        Money larger = Money.of(new BigDecimal("100.00"), Currency.getInstance("TWD"));
        
        // When & Then
        assertThat(smaller.isLessThan(larger)).isTrue();
        assertThat(larger.isGreaterThan(smaller)).isTrue();
        assertThat(smaller.isLessThan(smaller)).isFalse();
    }
}
```

---

## Mocking Strategies

### When to Mock

Mock external dependencies that are:
- Repositories (database access)
- External services (payment gateways, email services)
- Time-dependent operations
- Complex dependencies tested separately

### When NOT to Mock

Don't mock:
- Value objects (create real instances)
- Simple data structures
- Domain logic being tested
- Pure functions

### Mocking with Mockito

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private CustomerDomainService customerService;
    
    @Test
    @DisplayName("Should send welcome email when customer is created")
    void shouldSendWelcomeEmailWhenCustomerIsCreated() {
        // Given
        Customer customer = createCustomer();
        when(customerRepository.existsByEmail(customer.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        
        // When
        customerService.registerCustomer(customer);
        
        // Then
        verify(customerRepository).save(customer);
        verify(emailService).sendWelcomeEmail(
            customer.getEmail().value(),
            customer.getName().value()
        );
    }
    
    @Test
    @DisplayName("Should not send email when customer creation fails")
    void shouldNotSendEmailWhenCustomerCreationFails() {
        // Given
        Customer customer = createCustomer();
        when(customerRepository.existsByEmail(customer.getEmail())).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerService.registerCustomer(customer))
            .isInstanceOf(EmailAlreadyRegisteredException.class);
        
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }
    
    private Customer createCustomer() {
        return new Customer(
            CustomerId.generate(),
            new CustomerName("John Doe"),
            Email.of("john@example.com"),
            MembershipLevel.BRONZE
        );
    }
}
```

---

## Test Data Builders

Use the Builder pattern to create test data cleanly and maintainably.

### Test Data Builder Pattern

```java
public class CustomerTestDataBuilder {
    
    private CustomerId customerId = CustomerId.generate();
    private CustomerName name = new CustomerName("Test Customer");
    private Email email = Email.of("test@example.com");
    private MembershipLevel level = MembershipLevel.BRONZE;
    private Money totalSpending = Money.zero();
    private int loyaltyPoints = 0;
    
    public static CustomerTestDataBuilder aCustomer() {
        return new CustomerTestDataBuilder();
    }
    
    public CustomerTestDataBuilder withId(CustomerId customerId) {
        this.customerId = customerId;
        return this;
    }
    
    public CustomerTestDataBuilder withName(String name) {
        this.name = new CustomerName(name);
        return this;
    }
    
    public CustomerTestDataBuilder withEmail(String email) {
        this.email = Email.of(email);
        return this;
    }
    
    public CustomerTestDataBuilder withMembershipLevel(MembershipLevel level) {
        this.level = level;
        return this;
    }
    
    public CustomerTestDataBuilder silverMember() {
        this.level = MembershipLevel.SILVER;
        this.totalSpending = Money.of(new BigDecimal("50000"), Currency.getInstance("TWD"));
        return this;
    }
    
    public CustomerTestDataBuilder goldMember() {
        this.level = MembershipLevel.GOLD;
        this.totalSpending = Money.of(new BigDecimal("150000"), Currency.getInstance("TWD"));
        return this;
    }
    
    public CustomerTestDataBuilder withLoyaltyPoints(int points) {
        this.loyaltyPoints = points;
        return this;
    }
    
    public Customer build() {
        Customer customer = new Customer(customerId, name, email, level);
        // Set additional properties if needed
        return customer;
    }
}

// Usage in tests
@Test
void shouldApplyDiscountForSilverMember() {
    // Given
    Customer customer = aCustomer()
        .withName("Jane Smith")
        .withEmail("jane@example.com")
        .silverMember()
        .withLoyaltyPoints(1000)
        .build();
    
    // When & Then
    assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.SILVER);
}
```

---

## Common Patterns and Best Practices

### 1. Test Naming Convention

```java
// Pattern: should_expectedBehavior_when_condition
@Test
@DisplayName("Should throw exception when email format is invalid")
void should_throwException_when_emailFormatIsInvalid() {
    // Test implementation
}

@Test
@DisplayName("Should calculate discount correctly for premium members")
void should_calculateDiscountCorrectly_for_premiumMembers() {
    // Test implementation
}
```

### 2. Testing Edge Cases

```java
@Test
@DisplayName("Should handle null input gracefully")
void shouldHandleNullInputGracefully() {
    assertThatThrownBy(() -> new CustomerName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Name cannot be null");
}

@Test
@DisplayName("Should handle empty string input")
void shouldHandleEmptyStringInput() {
    assertThatThrownBy(() -> new CustomerName(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Name cannot be empty");
}

@Test
@DisplayName("Should handle whitespace-only input")
void shouldHandleWhitespaceOnlyInput() {
    assertThatThrownBy(() -> new CustomerName("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Name cannot be empty");
}
```

### 3. Testing Collections

```java
@Test
@DisplayName("Should add item to shopping cart")
void shouldAddItemToShoppingCart() {
    // Given
    ShoppingCart cart = new ShoppingCart(CustomerId.generate());
    Product product = createProduct();
    int quantity = 2;
    
    // When
    cart.addItem(product, quantity);
    
    // Then
    assertThat(cart.getItems()).hasSize(1);
    assertThat(cart.getItems().get(0).getProduct()).isEqualTo(product);
    assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(quantity);
}

@Test
@DisplayName("Should remove item from shopping cart")
void shouldRemoveItemFromShoppingCart() {
    // Given
    ShoppingCart cart = new ShoppingCart(CustomerId.generate());
    Product product = createProduct();
    cart.addItem(product, 2);
    
    // When
    cart.removeItem(product.getId());
    
    // Then
    assertThat(cart.getItems()).isEmpty();
}
```

### 4. Parameterized Tests

```java
@ParameterizedTest
@DisplayName("Should validate email format correctly")
@ValueSource(strings = {
    "user@example.com",
    "user.name@example.com",
    "user+tag@example.co.uk"
})
void shouldValidateEmailFormatCorrectly(String validEmail) {
    // When
    Email email = Email.of(validEmail);
    
    // Then
    assertThat(email.value()).isEqualTo(validEmail);
}

@ParameterizedTest
@DisplayName("Should reject invalid email formats")
@ValueSource(strings = {
    "not-an-email",
    "@example.com",
    "user@",
    "user @example.com"
})
void shouldRejectInvalidEmailFormats(String invalidEmail) {
    // When & Then
    assertThatThrownBy(() -> Email.of(invalidEmail))
        .isInstanceOf(IllegalArgumentException.class);
}

@ParameterizedTest
@DisplayName("Should calculate membership discount correctly")
@CsvSource({
    "BRONZE, 0, 0.00",
    "SILVER, 3, 0.03",
    "GOLD, 5, 0.05",
    "PLATINUM, 8, 0.08"
})
void shouldCalculateMembershipDiscountCorrectly(
    MembershipLevel level,
    int expectedPercentage,
    BigDecimal expectedRate
) {
    // Given
    Customer customer = aCustomer().withMembershipLevel(level).build();
    
    // When
    BigDecimal discountRate = customer.getDiscountRate();
    
    // Then
    assertThat(discountRate).isEqualByComparingTo(expectedRate);
}
```

---

## Performance Considerations

### Memory Management

```java
@Test
@DisplayName("Should not cause memory leak with large collections")
void shouldNotCauseMemoryLeakWithLargeCollections() {
    // Given
    List<Customer> customers = new ArrayList<>();
    
    // When - Create many customers
    for (int i = 0; i < 1000; i++) {
        customers.add(aCustomer()
            .withEmail("customer" + i + "@example.com")
            .build());
    }
    
    // Then - Verify memory usage is acceptable
    assertThat(customers).hasSize(1000);
    
    // Cleanup
    customers.clear();
}
```

### Execution Speed

```java
/**
 * Fast unit test - should complete in < 50ms
 * 
 * If this test is slow, check for:
 * - Unnecessary object creation
 * - Complex calculations
 * - Hidden I/O operations
 */
@Test
@DisplayName("Should execute quickly")
void shouldExecuteQuickly() {
    long startTime = System.currentTimeMillis();
    
    // Given & When
    Customer customer = aCustomer().build();
    Money discount = customer.calculateDiscount(Money.of(1000));
    
    // Then
    long executionTime = System.currentTimeMillis() - startTime;
    assertThat(executionTime).isLessThan(50); // Should be < 50ms
}
```

---

## Common Pitfalls and Anti-Patterns

### ❌ Anti-Pattern: Testing Implementation Details

```java
// BAD - Testing internal implementation
@Test
void badTest() {
    Customer customer = new Customer(...);
    assertThat(customer.getInternalState()).isEqualTo(expectedState);
}

// GOOD - Testing behavior
@Test
void goodTest() {
    Customer customer = new Customer(...);
    customer.updateProfile(newName, newEmail);
    assertThat(customer.getName()).isEqualTo(newName);
}
```

### ❌ Anti-Pattern: Over-Mocking

```java
// BAD - Mocking everything
@Test
void badTest() {
    when(mockValueObject.getValue()).thenReturn("value");
    when(mockEntity.getId()).thenReturn(id);
    when(mockCalculator.calculate()).thenReturn(result);
    // Too many mocks!
}

// GOOD - Only mock external dependencies
@Test
void goodTest() {
    ValueObject realValueObject = new ValueObject("value");
    Entity realEntity = new Entity(id);
    when(mockRepository.save(any())).thenReturn(realEntity);
}
```

### ❌ Anti-Pattern: Testing Multiple Things

```java
// BAD - Testing too much in one test
@Test
void badTest() {
    customer.updateProfile(...);
    customer.addAddress(...);
    customer.updateMembership(...);
    // Testing 3 different behaviors!
}

// GOOD - One test, one behavior
@Test
void shouldUpdateProfile() {
    customer.updateProfile(newName, newEmail);
    assertThat(customer.getName()).isEqualTo(newName);
}

@Test
void shouldAddAddress() {
    customer.addAddress(address);
    assertThat(customer.getAddresses()).contains(address);
}
```

---

## Quick Reference

### Test Structure Template

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("Component Name Tests")
class ComponentTest {
    
    @Mock
    private Dependency mockDependency;
    
    @InjectMocks
    private ComponentUnderTest component;
    
    @Test
    @DisplayName("Should do something when condition is met")
    void should_doSomething_when_conditionIsMet() {
        // Given - Arrange
        // Set up test data and mocks
        
        // When - Act
        // Execute the behavior being tested
        
        // Then - Assert
        // Verify the expected outcome
    }
}
```

### Common Assertions

```java
// Equality
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotEqualTo(unexpected);

// Null checks
assertThat(actual).isNotNull();
assertThat(actual).isNull();

// Boolean
assertThat(condition).isTrue();
assertThat(condition).isFalse();

// Collections
assertThat(list).hasSize(3);
assertThat(list).contains(item);
assertThat(list).containsExactly(item1, item2);
assertThat(list).isEmpty();

// Exceptions
assertThatThrownBy(() -> method())
    .isInstanceOf(ExceptionType.class)
    .hasMessageContaining("expected message");

// Numeric
assertThat(number).isGreaterThan(5);
assertThat(number).isLessThan(10);
assertThat(number).isBetween(5, 10);
```

---

## Related Documentation

- **Testing Strategy**: #[[file:../../steering/testing-strategy.md]]
- **Integration Testing**: #[[file:integration-testing-guide.md]]
- **BDD Testing**: #[[file:bdd-cucumber-guide.md]]
- **Test Performance**: #[[file:test-performance-guide.md]]

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-22  
**Owner**: Development Team
