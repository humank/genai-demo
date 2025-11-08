---
adr_number: 013
title: "DDD Tactical Patterns Implementation"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [002, 003, 012]
affected_viewpoints: ["development", "functional"]
affected_perspectives: ["evolution", "development-resource"]
---

# ADR-013: DDD Tactical Patterns Implementation

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a structured approach to domain modeling that:

- Captures complex business logic in a maintainable way
- Provides clear boundaries between different business concepts
- Ensures consistency within business transactions
- Enables rich domain models with behavior
- Supports ubiquitous language from business domain
- Facilitates collaboration between domain experts and developers
- Provides patterns for common domain modeling challenges
- Enables evolution of the domain model over time

### Business Context

**Business Drivers**:

- Complex business rules for e-commerce operations
- Need for clear business logic encapsulation
- Requirement for data consistency in transactions
- Support for evolving business requirements
- Collaboration between business and technical teams
- Long-term maintainability of business logic

**Constraints**:

- Team has limited DDD experience
- Hexagonal Architecture already adopted (ADR-002)
- Domain events for cross-context communication (ADR-003)
- Must integrate with existing Spring Boot stack
- Timeline: 3 months to establish DDD practices

### Technical Context

**Current State**:

- Spring Boot 3.4.5 + Java 21
- Hexagonal Architecture (ADR-002)
- Domain events (ADR-003)
- BDD with Cucumber (ADR-012)
- 13 bounded contexts identified

**Requirements**:

- Rich domain models with behavior
- Clear aggregate boundaries
- Consistency guarantees within aggregates
- Value objects for domain concepts
- Repository pattern for persistence
- Domain services for cross-aggregate logic
- Factory pattern for complex object creation
- Specification pattern for business rules

## Decision Drivers

1. **Business Logic Encapsulation**: Keep business rules in domain layer
2. **Consistency Boundaries**: Clear transactional boundaries
3. **Ubiquitous Language**: Code reflects business language
4. **Maintainability**: Easy to understand and modify
5. **Testability**: Domain logic testable in isolation
6. **Evolution**: Support changing business requirements
7. **Team Collaboration**: Patterns facilitate communication
8. **Industry Standards**: Follow proven DDD patterns

## Considered Options

### Option 1: Full DDD Tactical Patterns

**Description**: Implement complete set of DDD tactical patterns (Aggregates, Entities, Value Objects, Repositories, Domain Services, Factories, Specifications)

**Pros**:

- ✅ Rich domain models with behavior
- ✅ Clear aggregate boundaries
- ✅ Strong consistency guarantees
- ✅ Value objects for type safety
- ✅ Testable domain logic
- ✅ Ubiquitous language in code
- ✅ Well-documented patterns
- ✅ Supports complex business logic

**Cons**:

- ⚠️ Learning curve for team
- ⚠️ More initial design effort
- ⚠️ Can be over-engineering for simple CRUD

**Cost**: $0 (patterns, not tools)

**Risk**: **Low** - Proven patterns

### Option 2: Anemic Domain Model

**Description**: Simple data objects with getters/setters, business logic in services

**Pros**:

- ✅ Simple to understand
- ✅ Familiar to most developers
- ✅ Quick to implement

**Cons**:

- ❌ Business logic scattered across services
- ❌ No encapsulation
- ❌ Difficult to maintain complex logic
- ❌ No consistency guarantees
- ❌ Poor testability

**Cost**: $0

**Risk**: **High** - Technical debt accumulates

### Option 3: Transaction Script Pattern

**Description**: Procedural code organized by use cases

**Pros**:

- ✅ Simple for simple use cases
- ✅ Easy to understand

**Cons**:

- ❌ Doesn't scale to complex domains
- ❌ Code duplication
- ❌ No reusability
- ❌ Difficult to test

**Cost**: $0

**Risk**: **High** - Not suitable for complex domain

### Option 4: Partial DDD (Aggregates Only)

**Description**: Use only aggregates and entities, skip value objects and other patterns

**Pros**:

- ✅ Some benefits of DDD
- ✅ Lower learning curve

**Cons**:

- ❌ Missing type safety from value objects
- ❌ Incomplete pattern implementation
- ❌ Confusion about which patterns to use
- ❌ Less benefit than full DDD

**Cost**: $0

**Risk**: **Medium** - Incomplete benefits

## Decision Outcome

**Chosen Option**: **Full DDD Tactical Patterns**

### Rationale

Full DDD tactical patterns were selected for the following reasons:

1. **Complex Domain**: E-commerce has complex business rules that benefit from rich domain models
2. **Consistency**: Aggregates provide clear transactional boundaries
3. **Type Safety**: Value objects prevent primitive obsession
4. **Testability**: Domain logic can be tested without infrastructure
5. **Maintainability**: Clear patterns make code easier to understand
6. **Evolution**: Patterns support changing requirements
7. **Hexagonal Architecture Fit**: DDD patterns align perfectly with hexagonal architecture
8. **Industry Proven**: Patterns are well-documented and proven in production

**Implementation Strategy**:

**Core Patterns**:

1. **Aggregate Root**:

```java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends AggregateRootBase {
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private final List<OrderItem> items;
    private Money totalAmount;
    
    // Constructor enforces invariants
    public Order(OrderId id, CustomerId customerId, List<OrderItem> items) {
        this.id = Objects.requireNonNull(id, "Order ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "Items cannot be null"));
        this.status = OrderStatus.DRAFT;
        
        validateOrderCreation();
        calculateTotal();
        
        collectEvent(OrderCreatedEvent.create(id, customerId, totalAmount));
    }
    
    // Business methods
    public void submit() {
        validateOrderSubmission();
        
        this.status = OrderStatus.PENDING;
        
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount, items.size()));
    }
    
    public void addItem(OrderItem item) {
        validateItemAddition(item);
        
        items.add(item);
        calculateTotal();
        
        collectEvent(OrderItemAddedEvent.create(id, item.getProductId(), item.getQuantity()));
    }
    
    // Invariant enforcement
    private void validateOrderCreation() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Order must have at least one item");
        }
    }
    
    private void validateOrderSubmission() {
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException(
                "Only draft orders can be submitted. Current status: " + status
            );
        }
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
    }
    
    private void calculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.zero(), Money::add);
    }
    
    // Getters only, no setters
    public OrderId getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}
```

1. **Entity**:

```java
public class OrderItem {
    private final OrderItemId id;
    private final ProductId productId;
    private final String productName;
    private final Money unitPrice;
    private int quantity;
    private Money subtotal;
    
    public OrderItem(ProductId productId, String productName, Money unitPrice, int quantity) {
        this.id = OrderItemId.generate();
        this.productId = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.unitPrice = Objects.requireNonNull(unitPrice);
        
        setQuantity(quantity);
    }
    
    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(quantity);
    }
    
    public Money getSubtotal() { return subtotal; }
    public ProductId getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
```

1. **Value Object**:

```java
public record OrderId(String value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be blank");
        }
        if (!value.matches("ORD-\\d{10}")) {
            throw new IllegalArgumentException("Invalid order ID format: " + value);
        }
    }
    
    public static OrderId generate() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return new OrderId("ORD-" + timestamp);
    }
    
    public static OrderId of(String value) {
        return new OrderId(value);
    }
}

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    }
    
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }
    
    private void validateSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies");
        }
    }
}
```

1. **Repository Interface** (in domain layer):

```java
public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId orderId);
}
```

1. **Domain Service**:

```java
@Service
public class PricingService {
    
    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = order.calculateSubtotal();
        Money discount = calculateDiscount(subtotal, customer);
        Money tax = calculateTax(subtotal.subtract(discount));
        
        return subtotal.subtract(discount).add(tax);
    }
    
    private Money calculateDiscount(Money subtotal, Customer customer) {
        DiscountRate rate = customer.getDiscountRate();
        return subtotal.multiply(rate.getValue());
    }
    
    private Money calculateTax(Money amount) {
        TaxRate taxRate = TaxRate.standard();
        return amount.multiply(taxRate.getValue());
    }
}
```

1. **Factory**:

```java
@Component
public class OrderFactory {
    
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    
    public Order createOrder(CustomerId customerId, List<OrderItemRequest> itemRequests) {
        List<OrderItem> items = new ArrayList<>();
        
        for (OrderItemRequest request : itemRequests) {
            Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
            
            OrderItem item = new OrderItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                request.quantity()
            );
            
            items.add(item);
        }
        
        return new Order(OrderId.generate(), customerId, items);
    }
}
```

1. **Specification**:

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);
    Specification<T> and(Specification<T> other);
    Specification<T> or(Specification<T> other);
    Specification<T> not();
}

public class OrderEligibleForDiscountSpecification implements Specification<Order> {
    
    private final Money minimumAmount;
    
    public OrderEligibleForDiscountSpecification(Money minimumAmount) {
        this.minimumAmount = minimumAmount;
    }
    
    @Override
    public boolean isSatisfiedBy(Order order) {
        return order.getTotalAmount().isGreaterThan(minimumAmount)
            && order.getStatus() == OrderStatus.DRAFT;
    }
    
    @Override
    public Specification<Order> and(Specification<Order> other) {
        return new AndSpecification<>(this, other);
    }
    
    // ... other methods
}

// Usage
Specification<Order> eligibleForDiscount = new OrderEligibleForDiscountSpecification(Money.of(100))
    .and(new CustomerIsPremiumSpecification());

if (eligibleForDiscount.isSatisfiedBy(order)) {
    order.applyDiscount(discount);
}
```

**Why Not Anemic Domain Model**: Scatters business logic across services, making it hard to maintain and test.

**Why Not Transaction Script**: Doesn't scale to complex domains, leads to code duplication.

**Why Not Partial DDD**: Missing key benefits like type safety from value objects and clear patterns.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Developers | High | Need to learn DDD patterns | Training, examples, pair programming |
| Architects | Positive | Clear domain modeling approach | Architecture guidelines |
| Business Experts | Medium | Participate in domain modeling | Workshops, ubiquitous language sessions |
| QA Team | Medium | Test domain logic in isolation | Testing guides, examples |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All bounded contexts
- Domain layer structure
- Testing approach
- Development workflow
- Code review process

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| DDD learning curve | High | Medium | Training, examples, pair programming, code reviews |
| Over-engineering simple features | Medium | Low | Pragmatic approach, allow simpler patterns for CRUD |
| Aggregate boundary mistakes | Medium | High | Domain modeling workshops, architecture reviews |
| Performance concerns | Low | Medium | Performance testing, optimization where needed |
| Team resistance | Medium | Medium | Demonstrate benefits, involve team in decisions |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Training and Setup (Week 1-2)

- [ ] Conduct DDD training
  - Tactical patterns overview
  - Aggregate design workshop
  - Value object benefits
  - Repository pattern
  - Domain services vs application services

- [ ] Create base classes and interfaces

  ```java
  public abstract class AggregateRootBase implements AggregateRootInterface {
      private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
      
      protected void collectEvent(DomainEvent event) {
          uncommittedEvents.add(event);
      }
      
      @Override
      public List<DomainEvent> getUncommittedEvents() {
          return Collections.unmodifiableList(uncommittedEvents);
      }
      
      @Override
      public void markEventsAsCommitted() {
          uncommittedEvents.clear();
      }
      
      @Override
      public boolean hasUncommittedEvents() {
          return !uncommittedEvents.isEmpty();
      }
  }
  ```

- [ ] Create annotations

  ```java
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface AggregateRoot {
      String name();
      String boundedContext();
      String version() default "1.0";
      String description() default "";
  }
  
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  public @interface ValueObject {
  }
  ```

### Phase 2: Customer Bounded Context (Week 2-4)

- [ ] Design Customer aggregate

  ```java
  @AggregateRoot(name = "Customer", boundedContext = "Customer")
  public class Customer extends AggregateRootBase {
      private final CustomerId id;
      private CustomerName name;
      private Email email;
      private Address address;
      private MembershipLevel membershipLevel;
      private CustomerStatus status;
      
      public Customer(CustomerId id, CustomerName name, Email email, Address address) {
          this.id = Objects.requireNonNull(id);
          this.name = Objects.requireNonNull(name);
          this.email = Objects.requireNonNull(email);
          this.address = Objects.requireNonNull(address);
          this.membershipLevel = MembershipLevel.STANDARD;
          this.status = CustomerStatus.ACTIVE;
          
          collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
      }
      
      public void updateProfile(CustomerName newName, Email newEmail, Address newAddress) {
          validateProfileUpdate(newName, newEmail, newAddress);
          
          this.name = newName;
          this.email = newEmail;
          this.address = newAddress;
          
          collectEvent(CustomerProfileUpdatedEvent.create(id, newName, newEmail, newAddress));
      }
      
      public void upgradeMembership(MembershipLevel newLevel) {
          if (!newLevel.isHigherThan(this.membershipLevel)) {
              throw new BusinessRuleViolationException(
                  "New membership level must be higher than current level"
              );
          }
          
          this.membershipLevel = newLevel;
          
          collectEvent(CustomerMembershipUpgradedEvent.create(id, newLevel));
      }
      
      private void validateProfileUpdate(CustomerName name, Email email, Address address) {
          Objects.requireNonNull(name, "Name cannot be null");
          Objects.requireNonNull(email, "Email cannot be null");
          Objects.requireNonNull(address, "Address cannot be null");
      }
  }
  ```

- [ ] Create value objects

  ```java
  @ValueObject
  public record CustomerName(String value) {
      public CustomerName {
          Objects.requireNonNull(value, "Customer name cannot be null");
          if (value.isBlank()) {
              throw new IllegalArgumentException("Customer name cannot be blank");
          }
          if (value.length() < 2 || value.length() > 100) {
              throw new IllegalArgumentException("Customer name must be between 2 and 100 characters");
          }
      }
      
      public static CustomerName of(String value) {
          return new CustomerName(value);
      }
  }
  
  @ValueObject
  public record Email(String value) {
      private static final Pattern EMAIL_PATTERN = 
          Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
      
      public Email {
          Objects.requireNonNull(value, "Email cannot be null");
          if (!EMAIL_PATTERN.matcher(value).matches()) {
              throw new IllegalArgumentException("Invalid email format: " + value);
          }
      }
      
      public static Email of(String value) {
          return new Email(value);
      }
  }
  
  @ValueObject
  public record Address(
      String street,
      String city,
      String state,
      String postalCode,
      String country
  ) {
      public Address {
          Objects.requireNonNull(street, "Street cannot be null");
          Objects.requireNonNull(city, "City cannot be null");
          Objects.requireNonNull(postalCode, "Postal code cannot be null");
          Objects.requireNonNull(country, "Country cannot be null");
      }
  }
  ```

- [ ] Implement repository
- [ ] Write unit tests

### Phase 3: Order Bounded Context (Week 4-6)

- [ ] Design Order aggregate (as shown in examples above)
- [ ] Create value objects (OrderId, Money, etc.)
- [ ] Implement domain services (PricingService)
- [ ] Create factory (OrderFactory)
- [ ] Implement specifications
- [ ] Write unit tests

### Phase 4: Product Bounded Context (Week 6-8)

- [ ] Design Product aggregate
- [ ] Create value objects
- [ ] Implement repository
- [ ] Write unit tests

### Phase 5: Remaining Bounded Contexts (Week 8-12)

- [ ] Implement remaining 10 bounded contexts
- [ ] Follow established patterns
- [ ] Conduct code reviews
- [ ] Write comprehensive tests

### Phase 6: ArchUnit Validation (Week 12)

- [ ] Create ArchUnit tests

  ```java
  @ArchTest
  static final ArchRule aggregateRootRules = classes()
      .that().areAnnotatedWith(AggregateRoot.class)
      .should().resideInAPackage("..domain..model.aggregate..")
      .andShould().implement(AggregateRootInterface.class);
  
  @ArchTest
  static final ArchRule valueObjectRules = classes()
      .that().areAnnotatedWith(ValueObject.class)
      .should().beRecords()
      .andShould().resideInAPackage("..domain..model.valueobject..");
  
  @ArchTest
  static final ArchRule repositoryRules = classes()
      .that().haveSimpleNameEndingWith("Repository")
      .and().areInterfaces()
      .should().resideInAPackage("..domain..repository..");
  
  @ArchTest
  static final ArchRule domainLayerRules = classes()
      .that().resideInAPackage("..domain..")
      .should().onlyDependOnClassesThat()
      .resideInAnyPackage("..domain..", "java..", "org.springframework..");
  ```

### Rollback Strategy

**Trigger Conditions**:

- Team unable to adopt DDD after 6 months
- Development velocity decreases > 40%
- Aggregate boundaries causing major issues
- Over-engineering becomes problematic

**Rollback Steps**:

1. Simplify to anemic domain model
2. Move business logic to application services
3. Keep value objects for type safety
4. Simplify aggregate boundaries
5. Document lessons learned

**Rollback Time**: 4 weeks

## Monitoring and Success Criteria

### Success Metrics

- ✅ 100% of aggregates follow DDD patterns
- ✅ Value objects used instead of primitives > 90%
- ✅ Domain logic testable without infrastructure
- ✅ ArchUnit tests pass 100%
- ✅ Code review compliance > 95%
- ✅ Developer satisfaction > 4/5
- ✅ Business logic bugs decrease by 50%

### Monitoring Plan

**Code Quality Metrics**:

- Number of aggregates per bounded context
- Value object usage rate
- Domain logic test coverage
- ArchUnit test pass rate
- Code review findings

**Review Schedule**:

- Weekly: Domain modeling sessions
- Monthly: Architecture review
- Quarterly: DDD practice retrospective

## Consequences

### Positive Consequences

- ✅ **Rich Domain Models**: Business logic encapsulated in domain objects
- ✅ **Type Safety**: Value objects prevent primitive obsession
- ✅ **Consistency**: Aggregates enforce invariants
- ✅ **Testability**: Domain logic testable in isolation
- ✅ **Maintainability**: Clear patterns make code easier to understand
- ✅ **Ubiquitous Language**: Code reflects business language
- ✅ **Evolution**: Patterns support changing requirements
- ✅ **Clear Boundaries**: Aggregates define transactional boundaries

### Negative Consequences

- ⚠️ **Learning Curve**: Team needs to learn DDD patterns
- ⚠️ **Initial Overhead**: More design effort upfront
- ⚠️ **Verbosity**: More classes than anemic model
- ⚠️ **Can Over-Engineer**: Risk of over-engineering simple features

### Technical Debt

**Identified Debt**:

1. Some aggregates may have incorrect boundaries initially (will refine)
2. Not all value objects implemented yet (gradual adoption)
3. Some domain services may be in wrong layer (will refactor)
4. Limited specification pattern usage (future enhancement)

**Debt Repayment Plan**:

- **Q1 2026**: Refine aggregate boundaries based on experience
- **Q2 2026**: Complete value object adoption
- **Q3 2026**: Review and refactor domain services
- **Q4 2026**: Expand specification pattern usage

## Related Decisions

- [ADR-002: Adopt Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - DDD in domain layer
- [ADR-003: Use Domain Events for Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Events from aggregates
- [ADR-012: BDD with Cucumber](012-bdd-with-cucumber-for-requirements.md) - BDD scenarios test aggregates

## Notes

### Aggregate Design Guidelines

**DO**:

- Keep aggregates small
- Design around business invariants
- Use value objects for concepts
- Enforce invariants in constructor
- Use domain events for side effects
- Make aggregates testable

**DON'T**:

- Create large aggregates
- Reference other aggregates directly
- Expose internal collections
- Use setters for state changes
- Put infrastructure concerns in domain

### Value Object Benefits

1. **Type Safety**: Compile-time checking
2. **Validation**: Centralized validation logic
3. **Immutability**: Thread-safe, no side effects
4. **Expressiveness**: Clear intent in code
5. **Reusability**: Used across aggregates

### Common Patterns

**Aggregate Root Pattern**:

- Single entry point to aggregate
- Enforces invariants
- Collects domain events
- Controls access to entities

**Repository Pattern**:

- Interface in domain layer
- Implementation in infrastructure
- One repository per aggregate root
- Returns domain objects

**Factory Pattern**:

- Complex object creation
- Enforces creation rules
- Hides construction complexity

**Specification Pattern**:

- Encapsulates business rules
- Reusable and composable
- Testable in isolation

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
