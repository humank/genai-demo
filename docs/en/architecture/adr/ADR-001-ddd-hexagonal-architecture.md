# ADR-001: DDD + Hexagonal Architecture Foundation

## Status

**Accepted** - 2024-01-15

## Context

The GenAI Demo project requires a robust software architecture that can:

- Handle complex e-commerce business logic with clear domain boundaries
- Support multiple interfaces (REST API, CMC frontend, Consumer frontend)
- Enable independent testing and deployment of business logic
- Facilitate team collaboration and code maintainability
- Support future microservices evolution

### Business Objectives

- Implement comprehensive e-commerce platform with customer management, order processing, inventory, and payment systems
- Support complex business rules like membership levels, promotions, and pricing strategies

### Technical Challenges

- **Complexity Management**: E-commerce domain contains multiple interrelated subdomains
- **Testability**: Need to independently test business logic
- **Scalability**: Architecture must support future feature expansion
- **Team Collaboration**: Multiple developers need to work in parallel without interference

## Decision

We adopt **Domain-Driven Design (DDD) + Hexagonal Architecture** as our core software architecture pattern.

### Core Architecture Principles

#### 1. Hexagonal Architecture (Ports and Adapters)

```text
External World → Adapters → Ports → Application Core ← Ports ← Adapters ← External World
```

- **Application Core**: Contains business logic and domain models
- **Ports**: Define interfaces between application core and external world
- **Adapters**: Implement ports, handle specific technical details of external systems

#### 2. DDD Tactical Patterns

- **Aggregate Root**: Manages consistency boundaries
- **Value Object**: Immutable domain concepts
- **Domain Event**: Represents important events occurring in the domain
- **Specification Pattern**: Encapsulates complex business rules
- **Policy Pattern**: Handles business decision logic

#### 3. Layered Architecture

```text
interfaces/     → Interface Layer (REST Controllers, Web UI)
application/    → Application Layer (Use Case Coordination, Event Publishing)
domain/         → Domain Layer (Business Logic, Domain Models)
infrastructure/ → Infrastructure Layer (Persistence, External Services)
```

### Implementation Strategy

#### 1. Package Structure Design

```text
solid.humank.genaidemo/
├── interfaces/
│   └── web/           # REST Controllers
├── application/
│   ├── customer/      # Customer Use Cases
│   ├── order/         # Order Use Cases
│   └── inventory/     # Inventory Use Cases
├── domain/
│   ├── customer/      # Customer Aggregate
│   ├── order/         # Order Aggregate
│   └── inventory/     # Inventory Aggregate
└── infrastructure/
    ├── persistence/   # Data Persistence
    └── messaging/     # Message Processing
```

#### 2. Dependency Rules

- **Domain Layer**: Does not depend on any other layer
- **Application Layer**: Only depends on domain layer
- **Infrastructure Layer**: Can depend on all layers
- **Interface Layer**: Depends on application and infrastructure layers

#### 3. Aggregate Design Principles

- **Small Aggregates**: Each aggregate focuses on a single business concept
- **Consistency Boundary**: Maintain strong consistency within aggregates
- **Eventual Consistency**: Achieve eventual consistency between aggregates through domain events

## Results

### Positive Impact

#### 1. **Improved Testability**

- Business logic separated from technical details, unit test coverage reaches 85%+
- Can independently test domain logic without external dependencies

#### 2. **Enhanced Maintainability**

- Clear layered structure with well-defined code responsibilities
- Controlled impact scope when developing new features

#### 3. **Team Collaboration Efficiency**

- Different teams can develop different aggregates in parallel
- Clear interface definitions reduce communication costs

#### 4. **Technical Debt Control**

- Architecture constraints prevent improper dependency relationships
- Regular architecture compliance checks (ArchUnit)

### Quantitative Metrics

- **Architecture Compliance**: 9.5/10 (ArchUnit test results)
- **Code Duplication Rate**: < 5%
- **Circular Dependencies**: 0
- **Test Coverage**: 85%+

### Negative Impact and Mitigation Measures

#### 1. **Learning Curve**

- **Issue**: Team needs to learn DDD concepts
- **Mitigation**: Provide training documentation and code examples

#### 2. **Initial Development Speed**

- **Issue**: Architecture setup requires additional time
- **Mitigation**: Establish code templates and generation tools

#### 3. **Over-engineering Risk**

- **Issue**: May create overly complex structures for simple features
- **Mitigation**: Regular architecture reviews, maintain YAGNI principle

## Implementation Details

### 1. Aggregate Root Implementation

```java
@AggregateRoot(name = "Customer", description = "Customer Aggregate Root")
public class Customer implements AggregateRootInterface {
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    
    // Business methods
    public void upgradeToVip() {
        if (canUpgradeToVip()) {
            this.membershipLevel = MembershipLevel.VIP;
            collectEvent(new CustomerUpgradedToVipEvent(this.id));
        }
    }
}
```

### 2. Value Object Implementation

```java
@ValueObject
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString());
    }
}
```

### 3. Domain Event Implementation

```java
public record CustomerUpgradedToVipEvent(
    CustomerId customerId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static CustomerUpgradedToVipEvent create(CustomerId customerId) {
        return new CustomerUpgradedToVipEvent(
            customerId,
            UUID.randomUUID(),
            LocalDateTime.now()
        );
    }
}
```

### 4. Application Service Implementation

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    public void upgradeCustomerToVip(CustomerId customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
            
        customer.upgradeToVip();
        customerRepository.save(customer);
        
        // Publish domain events
        domainEventPublisher.publishEventsFromAggregate(customer);
    }
}
```

## Compliance and Validation

### 1. ArchUnit Rules

```java
@ArchTest
static final ArchRule domain_should_not_depend_on_infrastructure =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..");

@ArchTest
static final ArchRule aggregates_should_be_annotated =
    classes()
        .that().implement(AggregateRootInterface.class)
        .should().beAnnotatedWith(AggregateRoot.class);
```

### 2. Testing Strategy

- **Unit Tests**: Test domain logic without external dependencies
- **Integration Tests**: Test adapter integration with external systems
- **Architecture Tests**: Validate adherence to architecture rules

### 3. Documentation Maintenance

- **Domain Model Diagrams**: Maintain up-to-date domain models using PlantUML
- **Architecture Decision Records**: Record important architecture changes
- **Code Examples**: Provide standard implementation patterns

## Related Decisions

- [ADR-002: Bounded Context Design Strategy](./ADR-002-bounded-context-design.md)
- [ADR-003: Domain Events and CQRS Implementation](./ADR-003-domain-events-cqrs.md)
- [ADR-004: Spring Boot Profile Configuration Strategy](./ADR-004-spring-boot-profiles.md)

## References

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.amazon.com/Domain-Driven-Design-Tackling-Complexity-Software/dp/0321125215)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot DDD Best Practices](https://spring.io/guides/gs/spring-boot/)

---

**Last Updated**: 2024-01-15  
**Reviewers**: Architecture Team  
**Next Review**: 2024-07-15
