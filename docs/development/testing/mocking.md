# Mocking Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide provides best practices for using mocks in tests for the GenAI Demo project.

---

## Quick Reference

For complete testing guidance, see:
- [Unit Testing](unit-testing.md) - Unit testing guide
- [Testing Strategy](testing-strategy.md) - Overall testing approach

---

## When to Mock

### ✅ Do Mock

- **External Services**: Payment gateways, email services, SMS providers
- **Repositories**: In service layer tests
- **Time-Dependent Operations**: Clock, date/time providers
- **Complex Dependencies**: Already tested separately
- **Slow Operations**: Database calls, network requests

### ❌ Don't Mock

- **Value Objects**: Simple, immutable objects
- **Entities**: Domain objects being tested
- **Simple Data Structures**: DTOs, POJOs
- **Domain Logic**: The code you're testing
- **Infrastructure with Alternatives**: Use in-memory implementations

---

## Mockito Basics

### Creating Mocks

```java
// Using @Mock annotation
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private CustomerService customerService;
}

// Manual mock creation
CustomerRepository mockRepository = Mockito.mock(CustomerRepository.class);
```

---

## Stubbing

### Basic Stubbing

```java
// Return a value
when(customerRepository.findById("CUST-001"))
    .thenReturn(Optional.of(customer));

// Return different values on successive calls
when(inventoryService.checkStock(productId))
    .thenReturn(10)
    .thenReturn(5)
    .thenReturn(0);

// Throw an exception
when(paymentService.processPayment(any()))
    .thenThrow(new PaymentFailedException("Insufficient funds"));
```

### Argument Matchers

```java
// Any argument
when(customerRepository.save(any(Customer.class)))
    .thenReturn(customer);

// Specific value
when(customerRepository.findById(eq("CUST-001")))
    .thenReturn(Optional.of(customer));

// Argument capture
ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
verify(customerRepository).save(captor.capture());
Customer savedCustomer = captor.getValue();

// Custom matcher
when(orderService.calculateTotal(argThat(order -> 
    order.getItems().size() > 5)))
    .thenReturn(Money.of(100));
```

---

## Verification

### Basic Verification

```java
// Verify method was called
verify(emailService).sendWelcomeEmail(customer.getEmail());

// Verify with specific arguments
verify(customerRepository).save(eq(customer));

// Verify number of invocations
verify(emailService, times(1)).sendEmail(any());
verify(emailService, never()).sendEmail(any());
verify(emailService, atLeast(1)).sendEmail(any());
verify(emailService, atMost(3)).sendEmail(any());

// Verify no more interactions
verifyNoMoreInteractions(emailService);

// Verify no interactions at all
verifyNoInteractions(emailService);
```

### Verification Order

```java
// Verify order of invocations
InOrder inOrder = inOrder(inventoryService, paymentService, emailService);

inOrder.verify(inventoryService).reserveItems(orderId);
inOrder.verify(paymentService).processPayment(orderId);
inOrder.verify(emailService).sendConfirmation(orderId);
```

---

## Advanced Mocking

### Answer Interface

```java
// Custom behavior
when(customerRepository.save(any(Customer.class)))
    .thenAnswer(invocation -> {
        Customer customer = invocation.getArgument(0);
        customer.setId("GENERATED-ID");
        return customer;
    });

// Return argument
when(customerRepository.save(any(Customer.class)))
    .thenAnswer(AdditionalAnswers.returnsFirstArg());
```

### Spy Objects

```java
// Spy on real object
CustomerService realService = new CustomerService(repository);
CustomerService spyService = spy(realService);

// Stub specific methods
when(spyService.generateCustomerId()).thenReturn("CUST-001");

// Real methods are called unless stubbed
spyService.createCustomer(command); // Calls real method
```

### Partial Mocking

```java
// Mock only specific methods
CustomerService service = spy(new CustomerService(repository));
doReturn(true).when(service).isValidEmail(anyString());

// Other methods use real implementation
service.createCustomer(command);
```

---

## Best Practices

### 1. Mock at the Right Level

```java
// ✅ Good: Mock external dependencies
@Test
void should_send_email_when_customer_created() {
    when(emailService.sendWelcomeEmail(any()))
        .thenReturn(EmailResult.success());
    
    customerService.createCustomer(command);
    
    verify(emailService).sendWelcomeEmail(customer.getEmail());
}

// ❌ Bad: Over-mocking
@Test
void should_create_customer() {
    when(customerRepository.save(any())).thenReturn(customer);
    when(eventPublisher.publish(any())).thenReturn(true);
    when(validator.validate(any())).thenReturn(valid);
    when(idGenerator.generate()).thenReturn("id");
    when(clock.now()).thenReturn(now);
    // Too many mocks!
}
```

### 2. Use Specific Matchers

```java
// ✅ Good: Specific verification
verify(emailService).sendWelcomeEmail(eq("john@example.com"));

// ❌ Bad: Too generic
verify(emailService).sendWelcomeEmail(any());
```

### 3. Verify Behavior, Not Implementation

```java
// ✅ Good: Verify business behavior
@Test
void should_notify_customer_when_order_confirmed() {
    orderService.confirmOrder(orderId);
    
    verify(notificationService).notifyCustomer(
        eq(customerId),
        eq(NotificationType.ORDER_CONFIRMED)
    );
}

// ❌ Bad: Verify implementation details
@Test
void should_call_repository_save() {
    orderService.confirmOrder(orderId);
    
    verify(orderRepository).save(any());
    verify(orderRepository).flush();
}
```

### 4. Keep Tests Readable

```java
// ✅ Good: Clear setup
@Test
void should_apply_discount_for_premium_customer() {
    // Given
    Customer premiumCustomer = createPremiumCustomer();
    Order order = createOrderWithTotal(Money.of(100));
    
    when(customerRepository.findById(customerId))
        .thenReturn(Optional.of(premiumCustomer));
    
    // When
    Money total = orderService.calculateTotal(orderId);
    
    // Then
    assertThat(total).isEqualTo(Money.of(90));
}
```

---

## Common Patterns

### Repository Mocking

```java
@Test
void should_find_customer_by_id() {
    // Given
    Customer expectedCustomer = createCustomer();
    when(customerRepository.findById("CUST-001"))
        .thenReturn(Optional.of(expectedCustomer));
    
    // When
    Customer result = customerService.findById("CUST-001");
    
    // Then
    assertThat(result).isEqualTo(expectedCustomer);
}

@Test
void should_throw_exception_when_customer_not_found() {
    // Given
    when(customerRepository.findById("INVALID"))
        .thenReturn(Optional.empty());
    
    // When & Then
    assertThatThrownBy(() -> customerService.findById("INVALID"))
        .isInstanceOf(CustomerNotFoundException.class);
}
```

### External Service Mocking

```java
@Test
void should_handle_payment_failure() {
    // Given
    when(paymentService.processPayment(any()))
        .thenThrow(new PaymentFailedException("Card declined"));
    
    // When
    Result result = orderService.submitOrder(command);
    
    // Then
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getError()).contains("Card declined");
}
```

### Time-Dependent Mocking

```java
@Test
void should_set_creation_timestamp() {
    // Given
    LocalDateTime fixedTime = LocalDateTime.of(2024, 11, 19, 10, 0);
    Clock fixedClock = Clock.fixed(
        fixedTime.toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC
    );
    
    when(clock.instant()).thenReturn(fixedClock.instant());
    
    // When
    Customer customer = customerService.createCustomer(command);
    
    // Then
    assertThat(customer.getCreatedAt()).isEqualTo(fixedTime);
}
```

---

## Anti-Patterns

### 1. Mocking Everything

```java
// ❌ Bad: Too many mocks
@Test
void testCreateOrder() {
    when(validator.validate(any())).thenReturn(true);
    when(idGenerator.generate()).thenReturn("id");
    when(clock.now()).thenReturn(now);
    when(repository.save(any())).thenReturn(order);
    when(eventPublisher.publish(any())).thenReturn(true);
    when(emailService.send(any())).thenReturn(true);
    // ... more mocks
}

// ✅ Good: Mock only external dependencies
@Test
void should_create_order_and_send_confirmation() {
    when(emailService.sendConfirmation(any()))
        .thenReturn(EmailResult.success());
    
    Order order = orderService.createOrder(command);
    
    assertThat(order).isNotNull();
    verify(emailService).sendConfirmation(order.getId());
}
```

### 2. Mocking Value Objects

```java
// ❌ Bad: Mocking value objects
@Test
void testCalculateTotal() {
    Money mockMoney = mock(Money.class);
    when(mockMoney.getAmount()).thenReturn(BigDecimal.TEN);
    // Don't mock value objects!
}

// ✅ Good: Use real value objects
@Test
void should_calculate_total() {
    Money price = Money.of(10);
    Money total = order.calculateTotal();
    assertThat(total).isEqualTo(price);
}
```

### 3. Verifying Too Much

```java
// ❌ Bad: Over-verification
@Test
void testCreateCustomer() {
    customerService.createCustomer(command);
    
    verify(validator).validate(command);
    verify(repository).findByEmail(command.getEmail());
    verify(repository).save(any());
    verify(eventPublisher).publish(any());
    verify(emailService).send(any());
    // Testing implementation, not behavior
}

// ✅ Good: Verify key behavior
@Test
void should_send_welcome_email_when_customer_created() {
    customerService.createCustomer(command);
    
    verify(emailService).sendWelcomeEmail(command.getEmail());
}
```

---

## Troubleshooting

### Common Issues

**Issue**: `UnnecessaryStubbingException`
```java
// Problem: Stubbed but never used
when(service.method()).thenReturn(value); // Never called

// Solution: Remove unused stubs or use lenient()
lenient().when(service.method()).thenReturn(value);
```

**Issue**: `ArgumentsAreDifferent`
```java
// Problem: Verification with wrong arguments
verify(service).method("expected");
// But was called with "actual"

// Solution: Use argument captors to debug
ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
verify(service).method(captor.capture());
System.out.println("Actual: " + captor.getValue());
```

**Issue**: `NullPointerException` in mocks
```java
// Problem: Mock returns null by default
when(service.getCustomer()).thenReturn(null); // Explicit null

// Solution: Return Optional or proper default
when(service.getCustomer()).thenReturn(Optional.empty());
```

---

## Related Documentation

- [Unit Testing](unit-testing.md)
- [Integration Testing](integration-testing.md)
- [Testing Strategy](testing-strategy.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Development Team
