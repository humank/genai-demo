# DDD Tactical Patterns

## Overview

This document defines the Domain-Driven Design tactical patterns used in this project. These patterns are mandatory for all domain model implementations.

**Purpose**: Provide rules and quick checks for DDD pattern implementation.
**Detailed Examples**: See `.kiro/examples/ddd-patterns/` for comprehensive implementation guides.

---

## Aggregate Root Pattern

### Must Follow
- [ ] Extend `AggregateRoot` base class
- [ ] Use `@AggregateRoot` annotation with metadata
- [ ] Collect events with `collectEvent()` method
- [ ] No direct repository access from domain
- [ ] Aggregates are consistency boundaries
- [ ] Only aggregate roots can be obtained from repositories

### Must Avoid
- [ ] ❌ Exposing internal collections directly
- [ ] ❌ Setters for internal state
- [ ] ❌ Publishing events directly from aggregate
- [ ] ❌ Accessing other aggregates directly

### Example Structure
```java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends AggregateRoot {
    private final OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;
    
    public void submit() {
        // 1. Validate business rules
        validateOrderSubmission();
        
        // 2. Update state
        status = OrderStatus.PENDING;
        
        // 3. Collect domain event
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount));
    }
    
    private void validateOrderSubmission() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
    }
}
```

**Full Examples**: #[[file:../examples/ddd-patterns/aggregate-root-examples.md]]

---

## Domain Events Pattern

### Must Follow
- [ ] Use Record implementation for immutability
- [ ] Implement `DomainEvent` interface
- [ ] Use factory method with `createEventMetadata()`
- [ ] Events are immutable and contain all necessary data
- [ ] Use past tense naming (e.g., `OrderSubmitted`, not `SubmitOrder`)
- [ ] Include aggregate ID in event

### Must Avoid
- [ ] ❌ Mutable event fields
- [ ] ❌ Business logic in events
- [ ] ❌ References to mutable objects
- [ ] ❌ Missing event metadata (eventId, occurredOn)

### Example Structure
```java
public record OrderSubmittedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    int itemCount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    // Factory method with automatic metadata
    public static OrderSubmittedEvent create(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        int itemCount
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(
            orderId, customerId, totalAmount, itemCount,
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
}
```

**Full Examples**: #[[file:../examples/ddd-patterns/domain-events-examples.md]]

---

## Value Objects Pattern

### Must Follow
- [ ] Use Record for immutability
- [ ] Validate in constructor
- [ ] No setters
- [ ] Implement equals/hashCode based on value
- [ ] Make validation explicit and fail fast
- [ ] Use descriptive names (e.g., `Email`, not `String`)

### Must Avoid
- [ ] ❌ Mutable fields
- [ ] ❌ Setters or modification methods
- [ ] ❌ Identity-based equality
- [ ] ❌ Primitive obsession (using String instead of Email)

### Example Structure
```java
public record Email(String value) {
    
    public Email {
        // Validation in compact constructor
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
    
    // Factory method for clarity
    public static Email of(String value) {
        return new Email(value);
    }
}

public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }
    
    // Business methods
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    }
}
```

**Full Examples**: #[[file:../examples/ddd-patterns/value-objects-examples.md]]

---

## Repository Pattern

### Must Follow
- [ ] Interface in domain layer
- [ ] Implementation in infrastructure layer
- [ ] Return domain objects, not entities
- [ ] Use Optional for single results
- [ ] Repository per aggregate root
- [ ] Only aggregate roots have repositories

### Must Avoid
- [ ] ❌ Repository in infrastructure layer without domain interface
- [ ] ❌ Exposing JPA entities
- [ ] ❌ Business logic in repository
- [ ] ❌ Repositories for non-aggregate entities

### Example Structure
```java
// Domain layer: interface
package solid.humank.genaidemo.domain.order.repository;

public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId orderId);
}

// Infrastructure layer: implementation
package solid.humank.genaidemo.infrastructure.order;

@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

**Full Examples**: #[[file:../examples/ddd-patterns/repository-examples.md]]

---

## Domain Services Pattern

### When to Use
- [ ] Business logic that doesn't naturally fit in an aggregate
- [ ] Operations involving multiple aggregates
- [ ] Complex calculations or algorithms
- [ ] Integration with external domain concepts

### Must Follow
- [ ] Stateless services
- [ ] Interface in domain layer
- [ ] Implementation can be in domain or infrastructure
- [ ] Use domain language in method names

### Example Structure
```java
// Domain layer
package solid.humank.genaidemo.domain.pricing.service;

public interface PricingService {
    Money calculateOrderTotal(Order order, Customer customer);
    Money applyPromotions(Money amount, List<Promotion> promotions);
}

// Domain layer implementation
@Service
public class PricingServiceImpl implements PricingService {
    
    @Override
    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = order.calculateSubtotal();
        Money discount = customer.getDiscountRate().apply(subtotal);
        return subtotal.subtract(discount);
    }
}
```

---

## Application Services Pattern

### Responsibilities
- [ ] Orchestrate use cases
- [ ] Load aggregates from repositories
- [ ] Invoke aggregate methods
- [ ] Save aggregates
- [ ] Publish domain events
- [ ] Transaction management

### Must Follow
- [ ] Thin application services (orchestration only)
- [ ] No business logic in application services
- [ ] Use `@Transactional` for consistency
- [ ] Publish events after successful transaction

### Example Structure
```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final DomainEventApplicationService eventService;
    
    public void submitOrder(SubmitOrderCommand command) {
        // 1. Load aggregate
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        
        // 2. Execute business operation (aggregate handles logic)
        order.submit();
        
        // 3. Save aggregate
        orderRepository.save(order);
        
        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
}
```

---

## Bounded Context Pattern

### Must Follow
- [ ] Each context is independent
- [ ] Communication via domain events
- [ ] No direct dependencies between contexts
- [ ] Shared kernel in `domain/shared/`
- [ ] Context map documented

### Package Structure
```
domain/
├── customer/          # Customer bounded context
│   ├── model/
│   ├── events/
│   ├── repository/
│   └── service/
├── order/             # Order bounded context
│   ├── model/
│   ├── events/
│   ├── repository/
│   └── service/
└── shared/            # Shared kernel
    └── valueobject/
```

---

## Event Handlers Pattern

### Must Follow
- [ ] Extend `AbstractDomainEventHandler<T>`
- [ ] Annotate with `@Component`
- [ ] Implement idempotency checks
- [ ] Use `@TransactionalEventListener(phase = AFTER_COMMIT)`
- [ ] Handle errors gracefully

### Example Structure
```java
@Component
public class OrderSubmittedEventHandler 
    extends AbstractDomainEventHandler<OrderSubmittedEvent> {
    
    @Override
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        // 1. Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            return;
        }
        
        // 2. Execute cross-aggregate logic
        inventoryService.reserveItems(event.orderId());
        notificationService.sendOrderConfirmation(event.customerId());
        
        // 3. Mark as processed
        markEventAsProcessed(event.getEventId());
    }
    
    @Override
    public Class<OrderSubmittedEvent> getSupportedEventType() {
        return OrderSubmittedEvent.class;
    }
}
```

---

## Validation

### Architecture Compliance
```bash
./gradlew archUnit  # Verify DDD patterns compliance
```

### ArchUnit Rules
```java
@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().resideInAPackage("..domain..model.aggregate..");

@ArchTest
static final ArchRule domainEventRules = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords();

@ArchTest
static final ArchRule repositoryRules = classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().areInterfaces()
    .should().resideInAPackage("..domain..repository..");
```

---

## Quick Reference

| Pattern | Key Rule | Location |
|---------|----------|----------|
| Aggregate Root | Collect events, don't publish | `domain/{context}/model/aggregate/` |
| Domain Event | Immutable Record | `domain/{context}/events/` |
| Value Object | Immutable Record with validation | `domain/{context}/model/valueobject/` |
| Repository | Interface in domain, impl in infra | Interface: `domain/{context}/repository/`<br>Impl: `infrastructure/{context}/` |
| Domain Service | Stateless, domain logic | `domain/{context}/service/` |
| Application Service | Orchestration only | `application/{context}/` |

---

## Related Documentation

- **Core Principles**: #[[file:core-principles.md]]
- **Design Principles**: #[[file:design-principles.md]]
- **Architecture Constraints**: #[[file:architecture-constraints.md]]
- **DDD Examples**: #[[file:../examples/ddd-patterns/]]

---

**Document Version**: 1.0
**Last Updated**: 2025-01-17
**Owner**: Architecture Team
