# Design Guidelines

## Tell, Don't Ask Principle

### Origin and History
The "Tell, Don't Ask" principle was first proposed by Alec Sharp in 1997 and later widely disseminated in Andy Hunt and Dave Thomas's book "The Pragmatic Programmer". This principle emphasizes that you should tell objects what to do, rather than asking about their state and then deciding what to do.

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

3. Delivery Status Management
```java
// Good design: Let delivery object handle state transition
delivery.markAsDelivered();

// Bad design: Externally check status and set
if (delivery.getStatus() == DeliveryStatus.IN_TRANSIT) {
    delivery.setStatus(DeliveryStatus.DELIVERED);
    delivery.setUpdatedAt(LocalDateTime.now());
}
```

## Other Important Design Considerations

### 1. Single Responsibility Principle (SRP)
- Each class should have a clear responsibility
- There should be only one reason to change
- Good practices in the project:
  - `Order` aggregate root focuses on order lifecycle management
  - `Inventory` aggregate root focuses on inventory management
  - `Delivery` aggregate root focuses on delivery processes

### 2. Layered Architecture
- Presentation layer: Handles HTTP requests/responses
- Application layer: Coordinates different services, handles transactions
- Domain layer: Implements core business logic
- Infrastructure layer: Provides technical support
- Practices in the project:
  - Aggregate roots in the domain layer (like `Order`, `Inventory`) don't depend on infrastructure
  - Application services coordinate domain objects to complete use cases
  - Infrastructure layer implements interfaces defined by the domain layer

### 3. Separation of Concerns
- Separate business logic from technical details
- Separate domain logic from infrastructure concerns
- Practices in the project:
  - Use `OrderProcessingSaga` to coordinate operations across aggregate roots
  - Domain events are used to decouple different contexts

### 4. Dependency Inversion Principle (DIP)
- High-level modules should not depend on low-level modules
- Abstractions should not depend on details
- Practices in the project:
  - Use `Repository` interfaces to isolate domain layer from persistence implementation
  - Use `DomainEvent` interface rather than concrete event classes

### 5. Encapsulation
- Hide implementation details
- Provide meaningful interfaces
- Control the scope of change impact
- Practices in the project:
  - `Money` value object encapsulates amount and currency, providing safe operation methods
  - `OrderStatus` encapsulates state transition rules

## Application in Domain-Driven Design (DDD)

### Tell, Don't Ask and Bounded Contexts

The Tell, Don't Ask principle is particularly important when handling communication across bounded contexts:

1. Event-Driven Communication
   ```java
   // Bad design
   if (order.getStatus() == OrderStatus.CONFIRMED) {
       Payment payment = new Payment(order.getId(), order.getTotalAmount());
       paymentService.process(payment);
   }

   // Good design
   order.confirm();  // Internally publishes OrderConfirmedEvent
   // PaymentService subscribes to and handles OrderConfirmedEvent
   ```

2. Bounded Context Autonomy
   - Each context is responsible for its own decisions
   - Notify other contexts through events
   - Avoid direct queries between contexts
   - Practices in the project:
     - Order and Payment are independent contexts
     - Communicate through `PaymentRequestedEvent`

3. Anti-Corruption Layer Application
   ```java
   // Bad design: Directly expose external system details
   ExternalPaymentSystem.PaymentStatus status = externalSystem.getPaymentStatus(id);
   if (status == ExternalPaymentSystem.PaymentStatus.SUCCESS) {
       // Processing logic
   }

   // Good design: Use anti-corruption layer to encapsulate external system
   paymentAntiCorruptionLayer.processPayment(payment);
   ```

### DDD Tactical Pattern Application in the Project

#### 1. Aggregate Roots
- Maintain their own business rules and invariants
- Practices in the project:
  - `Order` aggregate root ensures correctness of order state transitions
  - `Inventory` aggregate root ensures inventory quantity consistency
  - `Delivery` aggregate root manages delivery state transitions

#### 2. Value Objects
- Describe concepts in the domain without identity
- Immutability ensures thread safety
- Practices in the project:
  - `Money` value object encapsulates amount and currency
  - Identifiers like `OrderId`, `CustomerId`
  - Status enums like `OrderStatus`, `DeliveryStatus`

#### 3. Domain Events
- Express important events that occur in the domain
- Decouple different bounded contexts
- Practices in the project:
  - `OrderCreatedEvent`
  - `OrderItemAddedEvent`
  - `PaymentRequestedEvent`

#### 4. Domain Services
- Handle business logic across aggregate roots
- Stateless
- Practices in the project:
  - `DomainEventPublisherService` handles event publishing

#### 5. Specification Pattern
- Encapsulate complex business rules
- Composable conditional expressions
- Practices in the project:
  - `Specification` interface and its implementations
  - `AndSpecification`, `OrSpecification` and other composite specifications

## Defensive Programming Practices

Defensive programming practices in the project:

### 1. Precondition Checks
```java
// Parameter validation in Order aggregate root
public Order(OrderId orderId, CustomerId customerId, String shippingAddress) {
    Preconditions.requireNonNull(orderId, "Order ID cannot be null");
    Preconditions.requireNonNull(customerId, "Customer ID cannot be null");
    Preconditions.requireNonEmpty(shippingAddress, "Shipping address cannot be empty");
    // ...
}
```

### 2. State Transition Protection
```java
// Order state transition protection
public void confirm() {
    // Check state transition
    if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
        throw new IllegalStateException("Cannot confirm an order in " + status + " state");
    }
    // Update state
    status = OrderStatus.CONFIRMED;
    updatedAt = LocalDateTime.now();
}
```

### 3. Immutable Value Objects
```java
// Immutable design of Money value object
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    // Don't provide setters, only provide operations that return new instances
    public Money add(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(money.amount), this.currency);
    }
}
```

### 4. Business Rule Encapsulation
```java
// Business rules in Inventory aggregate root
public ReservationId reserve(UUID orderId, int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("Reservation quantity must be greater than zero");
    }
    
    if (!isSufficient(quantity)) {
        throw new IllegalStateException("Insufficient inventory, cannot reserve");
    }
    
    // Business logic...
}
```

## Design Pattern Applications

Design patterns applied in the project:

### 1. Factory Method Pattern
```java
// Factory methods in OrderId value object
public static OrderId generate() {
    return new OrderId(UUID.randomUUID());
}

public static OrderId of(String id) {
    return new OrderId(UUID.fromString(id));
}
```

### 2. Strategy Pattern
Provides interchangeable business rule validation strategies through different specification implementations.

### 3. Observer Pattern
Implements observer pattern through domain events, decoupling event publishers and subscribers.

### 4. Command Pattern
Command and compensation operations in Saga pattern implement the command pattern concept.

## Improvement Suggestions

Based on project code analysis, here are some improvement suggestions:

### 1. Enhanced Error Handling
- Introduce specialized business exception types, such as `InsufficientInventoryException`
- Uniformly handle exceptions at the application layer, converting them to appropriate responses

### 2. Enhanced Domain Event Mechanism
- Consider using event sourcing pattern to record aggregate root state changes
- Implement event persistence to support event replay

### 3. Optimize Saga Implementation
- Consider using state machine pattern to manage Saga state transitions
- Enhance robustness of compensation logic

### 4. Enhanced Test Coverage
- Add unit tests for boundary conditions
- Add tests for concurrent scenarios

## Reference Resources

1. "The Pragmatic Programmer" - Andy Hunt & Dave Thomas
2. "Refactoring" - Martin Fowler
3. "Domain-Driven Design" - Eric Evans
4. "Implementing Domain-Driven Design" - Vaughn Vernon
5. [Tell, Don't Ask by Alec Sharp](http://pragprog.com/articles/tell-dont-ask)