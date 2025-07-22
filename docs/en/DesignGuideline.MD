# Design Guidelines

## Tell, Don't Ask Principle

### Origin and History
The "Tell, Don't Ask" principle was first introduced by Alec Sharp in 1997 and later popularized in "The Pragmatic Programmer" by Andy Hunt and Dave Thomas. This principle emphasizes that you should tell objects what to do, rather than asking about their state and then deciding what to do.

### Core Concepts
- Objects should be responsible for handling their internal state
- Callers should not make decisions based on an object's internal state
- Encapsulation is not just about hiding data, but also hiding behavior

### Bad Design Example
```java
// Violates Tell, Don't Ask
if (order.getStatus() == OrderStatus.PENDING) {
    order.setStatus(OrderStatus.PROCESSING);
    // Process order...
}
```

### Good Design Example
```java
// Follows Tell, Don't Ask
order.process();  // Let the order handle its own state transition
```

### Application in Our Project

1. Order Processing
```java
// Good design: Directly tell the order to process itself
order.process();

// Bad design: Check status then decide what to do
if (order.getStatus() == OrderStatus.CREATED) {
    order.submit();
}
```

2. Inventory Management
```java
// Good design: Let inventory handle reservation logic itself
ReservationId reservationId = inventory.reserve(orderId, quantity);

// Bad design: Check inventory then decide what to do
if (inventory.getAvailableQuantity() >= quantity) {
    inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
    // Create reservation...
}
```

3. Payment Processing
```java
// Good design: Tell payment to process itself
PaymentResult result = payment.process(amount);

// Bad design: Check payment status then decide
if (payment.getStatus() == PaymentStatus.PENDING) {
    payment.setStatus(PaymentStatus.PROCESSING);
    // Process payment...
}
```

## DDD Tactical Patterns

### Entity
- Has a unique identity that persists throughout its lifecycle
- Identity is more important than attributes
- Mutable objects that can change over time

```java
@Entity
public class Order {
    private OrderId id;  // Identity
    private CustomerId customerId;
    private OrderStatus status;
    
    // Business methods that maintain invariants
    public void addItem(OrderItem item) {
        validateItem(item);
        this.items.add(item);
        recalculateTotal();
    }
}
```

### Value Object
- Defined by their attributes, not identity
- Immutable objects
- Can be freely shared and replaced

```java
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    // Immutable - operations return new instances
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

### Aggregate Root
- Entry point to an aggregate
- Ensures consistency boundaries
- Controls access to internal entities

```java
@AggregateRoot
public class Order {
    private List<OrderItem> items = new ArrayList<>();
    
    // Only aggregate root can modify internal entities
    public void addItem(ProductId productId, int quantity, Money unitPrice) {
        OrderItem item = new OrderItem(productId, quantity, unitPrice);
        validateBusinessRules(item);
        this.items.add(item);
    }
    
    // Internal entities are not directly accessible
    List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
```

### Domain Service
- Contains domain logic that doesn't naturally fit in entities or value objects
- Stateless operations
- Coordinates between multiple domain objects

```java
@DomainService
public class PricingService {
    public Money calculateOrderTotal(Order order, List<Promotion> promotions) {
        Money subtotal = order.calculateSubtotal();
        Money discount = calculateDiscount(order, promotions);
        return subtotal.subtract(discount);
    }
}
```

### Repository
- Provides collection-like interface for aggregates
- Encapsulates data access logic
- Only for aggregate roots

```java
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
    List<Order> findByCustomerId(CustomerId customerId);
}
```

### Domain Event
- Represents something important that happened in the domain
- Immutable objects
- Used for decoupling and integration

```java
@DomainEvent
public class OrderSubmitted {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final Instant occurredOn;
    
    // Immutable event
    public OrderSubmitted(OrderId orderId, CustomerId customerId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.occurredOn = Instant.now();
    }
}
```

## Defensive Programming Practices

### Input Validation
Always validate inputs at the boundaries of your domain:

```java
public class Order {
    public void addItem(OrderItem item) {
        Objects.requireNonNull(item, "Order item cannot be null");
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (item.getUnitPrice().isNegative()) {
            throw new IllegalArgumentException("Unit price cannot be negative");
        }
        
        this.items.add(item);
    }
}
```

### Fail Fast
Detect problems as early as possible:

```java
public class Money {
    public Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
        
        if (amount.scale() > currency.getDefaultFractionDigits()) {
            throw new IllegalArgumentException("Amount precision exceeds currency precision");
        }
    }
}
```

### Immutability
Prefer immutable objects to reduce complexity:

```java
// Immutable value object
public final class OrderId {
    private final String value;
    
    public OrderId(String value) {
        this.value = Objects.requireNonNull(value, "Order ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
    }
    
    public String getValue() {
        return value;
    }
    
    // No setters - object is immutable
}
```

### Exception Handling
Use domain-specific exceptions:

```java
public class InsufficientInventoryException extends DomainException {
    public InsufficientInventoryException(ProductId productId, int requested, int available) {
        super(String.format("Insufficient inventory for product %s: requested %d, available %d", 
              productId.getValue(), requested, available));
    }
}
```

## Testing Strategies

### Unit Testing
Test individual components in isolation:

```java
@Test
@DisplayName("Should calculate order total correctly")
void shouldCalculateOrderTotalCorrectly() {
    // Given
    Order order = new Order(new OrderId("ORDER-001"), new CustomerId("CUSTOMER-001"));
    order.addItem(new ProductId("PRODUCT-001"), 2, Money.of(100, "USD"));
    order.addItem(new ProductId("PRODUCT-002"), 1, Money.of(50, "USD"));
    
    // When
    Money total = order.calculateTotal();
    
    // Then
    assertThat(total).isEqualTo(Money.of(250, "USD"));
}
```

### Integration Testing
Test component interactions:

```java
@Test
@DisplayName("Should process order workflow correctly")
void shouldProcessOrderWorkflowCorrectly() {
    // Given
    Order order = createTestOrder();
    
    // When
    orderService.processOrder(order);
    
    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    verify(inventoryService).reserveItems(order.getItems());
    verify(paymentService).processPayment(order.getPaymentInfo());
}
```

### Architecture Testing
Ensure architectural constraints are maintained:

```java
@Test
@DisplayName("Domain layer should not depend on infrastructure")
void domainShouldNotDependOnInfrastructure() {
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..")
        .check(importedClasses);
}
```

## Best Practices Summary

1. **Follow Tell, Don't Ask**: Let objects handle their own state and behavior
2. **Use DDD Tactical Patterns**: Apply appropriate patterns for different domain concepts
3. **Practice Defensive Programming**: Validate inputs, fail fast, prefer immutability
4. **Write Comprehensive Tests**: Unit, integration, and architecture tests
5. **Maintain Clear Boundaries**: Respect layer dependencies and aggregate boundaries
6. **Express Domain Logic Clearly**: Use ubiquitous language and domain-specific exceptions
