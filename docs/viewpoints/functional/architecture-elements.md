# Functional Viewpoint Architecture Elements

## Overview

This document provides detailed descriptions of core architecture elements in the functional viewpoint, including domain models, aggregate roots, entities, value objects, domain services, and application services.

## Core Architecture Elements

### 1. Aggregate Roots

Aggregate roots are core elements of the domain model, responsible for maintaining business invariants and coordinating operations within aggregates.

#### Main Aggregate Roots

| Aggregate Root | Bounded Context | Responsibility | Key Business Rules |
|----------------|-----------------|----------------|-------------------|
| Customer | Customer Management | Customer lifecycle management | Unique email, membership level rules |
| Order | Order Management | Order processing and state management | Order state transitions, amount calculations |
| Product | Product Catalog | Product information and inventory management | Stock quantities, price validity |
| Payment | Payment Processing | Payment processing and recording | Payment status, refund rules |
| Inventory | Inventory Management | Inventory control and reservation | Stock deduction, reservation mechanism |
| Promotion | Promotion Engine | Promotion rules and discounts | Promotion conditions, discount calculations |
| Shipping | Shipping Management | Delivery management and tracking | Delivery status, address validation |

#### Aggregate Root Design Principles

```java
@AggregateRoot(name = "Customer", description = "Customer aggregate root", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {
    
    // Aggregate root identity
    private final CustomerId id;
    
    // Business attributes
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    
    // Aggregate internal entities
    private List<DeliveryAddress> addresses;
    private List<PaymentMethod> paymentMethods;
    
    // Business methods
    public void updateProfile(CustomerName newName, Email newEmail) {
        validateProfileUpdate(newName, newEmail);
        this.name = newName;
        this.email = newEmail;
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail));
    }
    
    // Business rule validation
    private void validateProfileUpdate(CustomerName newName, Email newEmail) {
        if (newName == null || newEmail == null) {
            throw new InvalidProfileDataException("Name and email cannot be null");
        }
    }
}
```

### 2. Entities

Entities are domain objects with unique identity and lifecycle.

#### Entity Design Pattern

```java
@Entity
public class DeliveryAddress {
    
    private final DeliveryAddressId id;
    private Address address;
    private AddressType type;
    private boolean isDefault;
    
    public void markAsDefault() {
        this.isDefault = true;
        // Business logic: ensure only one default address
    }
    
    public boolean isValidForDelivery() {
        return address.isComplete() && address.isDeliverable();
    }
}
```

### 3. Value Objects

Value objects are immutable domain concepts defined by their attribute values for equality.

#### Value Object Implementation

```java
@ValueObject
public record CustomerId(String value) {
    
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        if (!value.matches("CUST-\\d{6}")) {
            throw new IllegalArgumentException("Invalid customer ID format");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + String.format("%06d", new Random().nextInt(999999)));
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}

@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }
    
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
}
```

### 4. Domain Services

Domain services encapsulate business logic that spans across aggregates.

#### Domain Service Implementation

```java
@DomainService
public class PricingService {
    
    public Money calculateOrderTotal(Order order, List<Promotion> applicablePromotions) {
        Money subtotal = calculateSubtotal(order);
        Money discount = calculateDiscount(subtotal, applicablePromotions);
        Money tax = calculateTax(subtotal.subtract(discount));
        
        return subtotal.subtract(discount).add(tax);
    }
    
    private Money calculateDiscount(Money subtotal, List<Promotion> promotions) {
        return promotions.stream()
            .map(promotion -> promotion.calculateDiscount(subtotal))
            .reduce(Money.ZERO, Money::add);
    }
}

@DomainService
public class InventoryAllocationService {
    
    public AllocationResult allocateInventory(Order order) {
        List<AllocationItem> allocations = new ArrayList<>();
        
        for (OrderItem item : order.getItems()) {
            AllocationResult itemResult = allocateItem(item);
            if (!itemResult.isSuccessful()) {
                return AllocationResult.failed(itemResult.getReason());
            }
            allocations.add(itemResult.getAllocation());
        }
        
        return AllocationResult.successful(allocations);
    }
}
```

### 5. Application Services

Application services coordinate use case execution without containing business logic.

#### Application Service Pattern

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService domainEventService;
    
    public Customer createCustomer(CreateCustomerCommand command) {
        // 1. Validate command
        validateCommand(command);
        
        // 2. Check business rules
        if (customerRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        
        // 3. Create aggregate root
        Customer customer = Customer.create(
            command.name(),
            command.email(),
            command.membershipLevel()
        );
        
        // 4. Save aggregate root
        Customer savedCustomer = customerRepository.save(customer);
        
        // 5. Publish domain events
        domainEventService.publishEventsFromAggregate(savedCustomer);
        
        return savedCustomer;
    }
}
```

## Architecture Element Relationship Diagram

### Element Hierarchy

```
Application Service Layer
    ├── CustomerApplicationService
    ├── OrderApplicationService
    └── ProductApplicationService
        │
        ▼ Coordinates
Domain Layer
    ├── Aggregate Roots
    │   ├── Customer
    │   ├── Order
    │   └── Product
    ├── Entities
    │   ├── DeliveryAddress
    │   ├── OrderItem
    │   └── ProductVariant
    ├── Value Objects
    │   ├── CustomerId
    │   ├── Money
    │   └── Address
    └── Domain Services
        ├── PricingService
        └── InventoryAllocationService
```

### Dependency Relationships

- **Application Services** → **Aggregate Roots** (coordinate business operations)
- **Aggregate Roots** → **Entities** (contain and manage)
- **Aggregate Roots** → **Value Objects** (use and compose)
- **Aggregate Roots** → **Domain Services** (delegate complex logic)
- **Domain Services** → **Multiple Aggregate Roots** (cross-aggregate operations)

## Design Constraints

### Aggregate Design Constraints

1. **Aggregate Boundaries**: Based on business invariants and transactional consistency
2. **Aggregate Size**: Avoid overly large aggregates that impact performance
3. **Aggregate References**: Reference other aggregates by ID, avoid direct references
4. **Transaction Boundaries**: One transaction can only modify one aggregate

### Entity Design Constraints

1. **Unique Identity**: Each entity must have a unique identifier
2. **Lifecycle**: Rules for entity creation, modification, and deletion
3. **Business Methods**: Entities should contain related business methods
4. **State Consistency**: Entity state changes must maintain consistency

### Value Object Design Constraints

1. **Immutability**: Value objects cannot be modified after creation
2. **Equality**: Equality determined by attribute values
3. **Validation**: Complete integrity validation during creation
4. **Business Methods**: Include related business calculation methods

## Implementation Checklist

### Aggregate Root Checklist

- [ ] Implements AggregateRootInterface
- [ ] Uses @AggregateRoot annotation
- [ ] Contains business methods and rule validation
- [ ] Correctly collects and publishes domain events
- [ ] Maintains business invariants within aggregate

### Entity Checklist

- [ ] Uses @Entity annotation
- [ ] Has unique identifier
- [ ] Contains related business methods
- [ ] Correctly handles state transitions
- [ ] Clear relationship with aggregate root

### Value Object Checklist

- [ ] Uses @ValueObject annotation
- [ ] Implemented as immutable Record
- [ ] Contains validation logic
- [ ] Implements business calculation methods
- [ ] Correctly handles equality comparison

### Service Checklist

- [ ] Domain services use @DomainService annotation
- [ ] Application services use @Service annotation
- [ ] Clear separation of responsibilities
- [ ] Correct dependency injection
- [ ] Reasonable transaction boundaries

---

**Related Documents**:
- [Domain Model Design](domain-model.md)
- [Aggregate Root Implementation Guide](aggregates.md)
- [Domain Events Design](../information/domain-events.md)
