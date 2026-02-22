---
inclusion: always
last_updated: 2026-02-21
---

# Architecture Guide

Complete guide for DDD patterns, hexagonal architecture, and bounded contexts.

## Quick Reference

### Architecture Layers
```
interfaces/ → application/ → domain/ ← infrastructure/
```

### Must Follow
- Domain has NO dependencies on infrastructure
- Aggregates collect events, services publish them
- Use Records for Value Objects and Events
- Repository interfaces in domain, implementations in infrastructure

---

## DDD Tactical Patterns

### Aggregate Root

**Must Follow:**
- Extend `AggregateRoot` base class
- Use `@AggregateRoot` annotation
- Collect events with `collectEvent()`
- No direct repository access

**Example:**
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

### Domain Events

**Must Follow:**
- Use Record implementation
- Implement `DomainEvent` interface
- Use factory method with `createEventMetadata()`
- Immutable with all necessary data

**Example:**
```java
public record OrderSubmittedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    int itemCount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
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

### Value Objects

**Must Follow:**
- Use Record for immutability
- Validate in constructor
- No setters
- Implement equals/hashCode based on value

**Example:**
```java
public record Email(String value) {
    
    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }
    
    public static Email of(String value) {
        return new Email(value);
    }
}

public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("Amount and currency cannot be null");
        }
    }
    
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

### Repository Pattern

**Must Follow:**
- Interface in domain layer
- Implementation in infrastructure layer
- Return domain objects, not entities
- Use Optional for single results

**Example:**
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

### Application Services

**Must Follow:**
- Orchestrate use cases
- Load aggregates from repositories
- Invoke aggregate methods
- Save aggregates
- Publish domain events

**Example:**
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
        
        // 2. Execute business operation
        order.submit();
        
        // 3. Save aggregate
        orderRepository.save(order);
        
        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
}
```

### Event Handlers

**Must Follow:**
- Extend `AbstractDomainEventHandler<T>`
- Annotate with `@Component`
- Implement idempotency checks
- Use `@TransactionalEventListener(phase = AFTER_COMMIT)`

**Example:**
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

## Hexagonal Architecture

### Layer Dependencies

**Dependency Rule:**
```
interfaces/ → application/ → domain/ ← infrastructure/
```

**Must Follow:**
- Domain layer has NO dependencies on any other layer
- Application layer depends only on domain
- Infrastructure implements domain interfaces
- Interfaces layer depends on application and domain

### Package Structure

```
domain/
├── {context}/
│   ├── model/
│   │   ├── aggregate/      # Aggregate roots
│   │   ├── entity/         # Entities
│   │   ├── valueobject/    # Value objects
│   │   └── specification/  # Business rules
│   ├── events/             # Domain events
│   ├── repository/         # Repository interfaces
│   └── service/            # Domain services
└── shared/                 # Shared kernel

application/
└── {context}/
    ├── {UseCase}ApplicationService.java
    ├── command/            # Command objects
    ├── query/              # Query objects
    └── dto/                # Data transfer objects

infrastructure/
└── {context}/
    ├── persistence/        # Repository implementations
    │   ├── entity/        # JPA entities
    │   ├── mapper/        # Domain ↔ Entity mappers
    │   └── repository/    # JPA repositories
    ├── messaging/          # Event publishers
    └── external/           # External service adapters

interfaces/
├── rest/
│   └── {context}/
│       ├── controller/     # REST controllers
│       ├── dto/            # Request/Response DTOs
│       └── mapper/         # DTO ↔ Domain mappers
└── web/                    # Web UI controllers
```

---

## Bounded Contexts

### 13 Bounded Contexts

**Core Domains:**
- Customer Management
- Order Management
- Payment Processing
- Inventory Management

**Supporting Domains:**
- Product Catalog
- Shopping Cart
- Pricing Strategy
- Promotion Engine
- Logistics & Delivery

**Generic Domains:**
- Notification Service
- Review & Rating
- Analytics & Reporting
- Workflow Orchestration

### Context Communication

**Must Follow:**
- Each context is independent
- Communication via domain events only
- No direct dependencies between contexts
- Shared kernel in `domain/shared/`

**Example:**
```java
// ✅ GOOD: Communication via events
@Component
public class OrderSubmittedEventHandler {
    private final InventoryService inventoryService;
    
    @EventListener
    public void handle(OrderSubmittedEvent event) {
        inventoryService.reserveItems(event.orderId());
    }
}

// ❌ BAD: Direct dependency between contexts
public class OrderService {
    private final InventoryRepository inventoryRepository; // Wrong!
}
```

---

## Design Principles

### SOLID Principles

**Single Responsibility:**
- One class, one reason to change
- Each method does one thing well

**Open/Closed:**
- Open for extension, closed for modification
- Use interfaces and strategy pattern

**Liskov Substitution:**
- Subtypes must be substitutable for base types
- Don't strengthen preconditions

**Interface Segregation:**
- Small, focused interfaces
- Clients don't depend on unused methods

**Dependency Inversion:**
- Depend on abstractions, not concretions
- Use dependency injection

### Tell, Don't Ask

**Principle:** Objects should tell other objects what to do, not ask for their state.

```java
// ❌ BAD: Asking for state
if (order.getStatus() == OrderStatus.CREATED) {
    order.setStatus(OrderStatus.PENDING);
    order.setUpdatedAt(LocalDateTime.now());
}

// ✅ GOOD: Telling what to do
order.submit();  // Object handles its own state
```

### Law of Demeter

**Principle:** Only talk to your immediate friends.

```java
// ❌ BAD: Violates Law of Demeter
customer.getAddress().getCity().getPostalCode();

// ✅ GOOD: Ask the object directly
customer.getPostalCode();
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
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..");

@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class);

@ArchTest
static final ArchRule domainEventRules = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords();
```

---

## Related Guides

- **Development**: See `development-guide.md` for testing and code quality
- **Design**: See `design-guide.md` for SOLID principles
- **Security**: See `security-guide.md` for security patterns
- **Performance**: See `performance-guide.md` for optimization

---

**Last Updated**: 2026-02-21
**Owner**: Architecture Team
