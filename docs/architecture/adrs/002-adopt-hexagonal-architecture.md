---
adr_number: 002
title: "Adopt Hexagonal Architecture"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 003, 006]
affected_viewpoints: ["development", "functional", "information"]
affected_perspectives: ["evolution", "development-resource"]
---

# ADR-002: Adopt Hexagonal Architecture

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform needs an architecture that:

- Separates business logic from technical concerns
- Enables independent testing of domain logic
- Allows technology changes without affecting business rules
- Supports multiple interfaces (REST API, CLI, messaging)
- Facilitates team collaboration and parallel development
- Maintains clean boundaries between layers

### Business Context

**Business Drivers**:
- Need for rapid feature development without breaking existing functionality
- Requirement to support multiple client types (web, mobile, API consumers)
- Expected evolution of technology stack over 5+ year lifespan
- Need for comprehensive testing to ensure quality
- Team growth from 5 to 20+ developers

**Constraints**:
- Team has Spring Boot experience but limited DDD experience
- 3-month timeline to MVP
- Must integrate with existing AWS infrastructure
- Need to maintain backward compatibility as system evolves

### Technical Context

**Current State**:
- New greenfield project
- Spring Boot 3.4.5 + Java 21
- Domain-Driven Design (DDD) tactical patterns chosen
- Event-driven architecture for cross-context communication

**Requirements**:
- Clear separation of concerns
- Testable business logic
- Technology-agnostic domain layer
- Support for multiple adapters (REST, messaging, CLI)
- Easy to onboard new developers
- Maintainable over 5+ years

## Decision Drivers

1. **Testability**: Need to test business logic without infrastructure dependencies
2. **Maintainability**: Clear boundaries reduce coupling and improve maintainability
3. **Technology Independence**: Business logic should not depend on frameworks
4. **Team Scalability**: Clear structure helps teams work independently
5. **Evolution**: Easy to add new interfaces and change implementations
6. **DDD Alignment**: Architecture should support DDD tactical patterns
7. **Spring Boot Integration**: Must work well with Spring ecosystem
8. **Learning Curve**: Team needs to adopt quickly

## Considered Options

### Option 1: Hexagonal Architecture (Ports and Adapters)

**Description**: Architecture pattern that separates core business logic from external concerns through ports (interfaces) and adapters (implementations)

**Structure**:
```
domain/          # Core business logic (no dependencies)
├── model/       # Aggregates, entities, value objects
├── events/      # Domain events
├── repository/  # Repository interfaces (ports)
└── service/     # Domain services

application/     # Use case orchestration
└── {context}/   # Application services

infrastructure/  # Technical implementations (adapters)
├── persistence/ # Repository implementations
├── messaging/   # Event publishers
└── external/    # External service adapters

interfaces/      # External interfaces (adapters)
├── rest/        # REST controllers
└── messaging/   # Message consumers
```

**Pros**:
- ✅ Complete separation of business logic from infrastructure
- ✅ Domain layer has zero external dependencies
- ✅ Easy to test domain logic in isolation
- ✅ Technology changes don't affect business rules
- ✅ Multiple adapters can coexist (REST, CLI, messaging)
- ✅ Clear dependency direction (all depend on domain)
- ✅ Excellent for DDD implementation
- ✅ Supports event-driven architecture naturally

**Cons**:
- ⚠️ More initial setup and boilerplate
- ⚠️ Learning curve for team
- ⚠️ More interfaces and abstractions
- ⚠️ Can feel over-engineered for simple CRUD

**Cost**: 
- Initial: 2 weeks additional setup time
- Ongoing: Minimal (pays off in maintainability)

**Risk**: **Low** - Well-established pattern with proven benefits

### Option 2: Layered Architecture (Traditional N-Tier)

**Description**: Traditional layered architecture with presentation, business, and data layers

**Structure**:
```
presentation/    # Controllers, views
business/        # Business logic
data/            # Data access
```

**Pros**:
- ✅ Familiar to most developers
- ✅ Simple to understand
- ✅ Less boilerplate
- ✅ Quick to implement

**Cons**:
- ❌ Business logic often leaks into other layers
- ❌ Tight coupling to frameworks and databases
- ❌ Difficult to test in isolation
- ❌ Technology changes affect business logic
- ❌ Doesn't support DDD well
- ❌ Single adapter type (usually REST)

**Cost**: Lower initial cost, higher maintenance cost

**Risk**: **Medium** - Technical debt accumulates quickly

### Option 3: Clean Architecture (Uncle Bob)

**Description**: Similar to Hexagonal but with more explicit layer definitions

**Structure**:
```
entities/        # Enterprise business rules
use-cases/       # Application business rules
interface-adapters/  # Controllers, presenters, gateways
frameworks/      # External frameworks and tools
```

**Pros**:
- ✅ Similar benefits to Hexagonal
- ✅ Very explicit layer boundaries
- ✅ Strong emphasis on dependency rule
- ✅ Good for complex domains

**Cons**:
- ⚠️ More layers than Hexagonal
- ⚠️ Can be overly complex
- ⚠️ Less Spring Boot integration examples
- ⚠️ Steeper learning curve

**Cost**: Similar to Hexagonal, slightly higher complexity

**Risk**: **Low** - Proven pattern, but more complex

### Option 4: Modular Monolith

**Description**: Single deployable with clear module boundaries

**Pros**:
- ✅ Simpler deployment
- ✅ Clear module boundaries
- ✅ Can evolve to microservices

**Cons**:
- ❌ Doesn't address layer separation
- ❌ Can still have tight coupling
- ❌ Doesn't solve testability issues
- ❌ Not mutually exclusive with Hexagonal

**Cost**: Similar to layered architecture

**Risk**: **Medium** - Boundaries can erode over time

## Decision Outcome

**Chosen Option**: **Hexagonal Architecture (Ports and Adapters)**

### Rationale

Hexagonal Architecture was selected for the following reasons:

1. **Perfect DDD Fit**: Aligns perfectly with our DDD tactical patterns (aggregates, repositories, domain events)
2. **Testability**: Domain logic can be tested without any infrastructure dependencies
3. **Technology Independence**: Business rules are completely isolated from Spring Boot, databases, and messaging
4. **Multiple Adapters**: Naturally supports REST API, messaging, and future CLI/GraphQL interfaces
5. **Clear Boundaries**: Explicit ports (interfaces) and adapters (implementations) prevent coupling
6. **Event-Driven Support**: Domain events fit naturally into the architecture
7. **Team Growth**: Clear structure helps new developers understand the system
8. **Long-Term Maintainability**: Technology changes (e.g., database, messaging) don't affect business logic

**Why Not Layered**: Layered architecture leads to tight coupling and makes testing difficult. Business logic often leaks into controllers and repositories.

**Why Not Clean Architecture**: While similar, Hexagonal is simpler and has better Spring Boot integration examples. The additional layers in Clean Architecture add complexity without significant benefit for our use case.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | Need to learn Hexagonal Architecture | Training sessions, code examples, pair programming |
| Architects | Positive | Clear architecture boundaries | Architecture documentation, ADRs |
| QA Team | Positive | Easier to test business logic | Testing guides, example tests |
| Operations | Low | No operational impact | N/A |
| Business | Positive | Faster feature development | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All bounded contexts (package structure)
- All layers (domain, application, infrastructure, interfaces)
- Testing strategy (unit tests for domain)
- Development workflow (where to put code)
- Onboarding process (architecture training)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Team learning curve | High | Medium | Training, examples, pair programming, code reviews |
| Over-engineering simple features | Medium | Low | Pragmatic approach, allow simpler patterns for CRUD |
| Boilerplate code | Medium | Low | Code generators, templates, IDE snippets |
| Inconsistent implementation | Medium | Medium | ArchUnit tests, code reviews, architecture guidelines |
| Resistance to change | Low | Medium | Demonstrate benefits, involve team in decision |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Foundation Setup (Week 1)

- [x] Create package structure (domain, application, infrastructure, interfaces)
- [x] Define base interfaces (AggregateRoot, DomainEvent, Repository)
- [x] Set up ArchUnit tests to enforce architecture rules
- [x] Create example aggregate with full implementation
- [x] Document architecture guidelines

### Phase 2: Team Training (Week 1-2)

- [x] Conduct architecture training session
- [x] Create code examples for common patterns
- [x] Set up pair programming sessions
- [x] Create architecture decision flowchart
- [x] Document "where does this code go?" guide

### Phase 3: Implementation (Week 2-12)

- [x] Implement Customer bounded context
- [x] Implement Order bounded context
- [x] Implement Product bounded context
- [ ] Implement remaining bounded contexts
- [ ] Continuous code reviews for architecture compliance
- [ ] Refine patterns based on feedback

### Phase 4: Validation (Ongoing)

- [x] ArchUnit tests run in CI/CD
- [x] Regular architecture reviews
- [x] Collect team feedback
- [x] Update guidelines based on learnings

### Rollback Strategy

**Trigger Conditions**:
- Team unable to adopt after 4 weeks
- Development velocity decreases by > 30%
- Excessive boilerplate slows development
- Architecture violations > 50% of PRs

**Rollback Steps**:
1. Simplify to layered architecture
2. Keep domain models but allow direct dependencies
3. Merge infrastructure into application layer
4. Update ArchUnit rules
5. Refactor existing code gradually

**Rollback Time**: 2 weeks

**Note**: Rollback is unlikely as benefits typically outweigh costs after initial learning period.

## Monitoring and Success Criteria

### Success Metrics

- ✅ 100% of domain layer has zero infrastructure dependencies (ArchUnit)
- ✅ 80%+ unit test coverage for domain logic
- ✅ < 5% architecture violations in code reviews
- ✅ New developers productive within 1 week
- ✅ Technology changes (e.g., database) take < 1 day
- ✅ Team satisfaction score > 4/5

### Monitoring Plan

**ArchUnit Tests**:
```java
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..");

@ArchTest
static final ArchRule repositoryRules = classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().areInterfaces()
    .should().resideInAPackage("..domain..repository..");
```

**Code Review Checklist**:
- [ ] Domain logic in domain layer
- [ ] No infrastructure dependencies in domain
- [ ] Repositories are interfaces in domain
- [ ] Adapters implement domain interfaces
- [ ] Application services orchestrate use cases

**Review Schedule**:
- Weekly: Architecture review in team meeting
- Monthly: ArchUnit test results review
- Quarterly: Architecture retrospective

## Consequences

### Positive Consequences

- ✅ **Testability**: Domain logic tested without infrastructure (fast, reliable tests)
- ✅ **Technology Independence**: Can change databases, frameworks without affecting business logic
- ✅ **Clear Boundaries**: Explicit ports and adapters prevent coupling
- ✅ **Multiple Interfaces**: Easy to add REST, GraphQL, CLI, messaging adapters
- ✅ **DDD Support**: Perfect fit for aggregates, repositories, domain events
- ✅ **Team Scalability**: Clear structure helps teams work independently
- ✅ **Maintainability**: Changes are localized to specific layers
- ✅ **Onboarding**: New developers understand structure quickly

### Negative Consequences

- ⚠️ **Initial Overhead**: More setup time and boilerplate code
- ⚠️ **Learning Curve**: Team needs to learn new patterns
- ⚠️ **More Abstractions**: More interfaces and classes
- ⚠️ **Potential Over-Engineering**: Simple CRUD might feel complex

### Technical Debt

**Identified Debt**:
1. Some simple CRUD operations have unnecessary abstraction (acceptable trade-off)
2. Boilerplate code for mappers between layers (can be reduced with MapStruct)
3. Learning curve for new team members (decreases over time)

**Debt Repayment Plan**:
- **Q1 2026**: Introduce MapStruct to reduce mapper boilerplate
- **Q2 2026**: Create code generators for common patterns
- **Q3 2026**: Refine patterns based on 6 months of experience

## Related Decisions

- [ADR-001: Use PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - Repository implementations
- [ADR-003: Use Domain Events for Cross-Context Communication](003-use-domain-events-for-cross-context-communication.md) - Event-driven architecture
- [ADR-006: Environment-Specific Testing Strategy](006-environment-specific-testing-strategy.md) - Testing approach

## Notes

### Package Structure

```
solid.humank.genaidemo/
├── domain/
│   ├── customer/
│   │   ├── model/
│   │   │   ├── aggregate/
│   │   │   │   └── Customer.java
│   │   │   ├── entity/
│   │   │   └── valueobject/
│   │   │       ├── CustomerId.java
│   │   │       └── Email.java
│   │   ├── events/
│   │   │   └── CustomerCreatedEvent.java
│   │   ├── repository/
│   │   │   └── CustomerRepository.java (interface)
│   │   └── service/
│   │       └── CustomerDomainService.java
│   └── shared/
│       └── valueobject/
│           └── Money.java
├── application/
│   └── customer/
│       ├── CustomerApplicationService.java
│       ├── command/
│       │   └── CreateCustomerCommand.java
│       └── dto/
│           └── CustomerDto.java
├── infrastructure/
│   └── customer/
│       ├── persistence/
│       │   ├── entity/
│       │   │   └── CustomerEntity.java
│       │   ├── mapper/
│       │   │   └── CustomerMapper.java
│       │   └── repository/
│       │       ├── CustomerJpaRepository.java
│       │       └── JpaCustomerRepository.java (implements CustomerRepository)
│       ├── messaging/
│       │   └── CustomerEventPublisher.java
│       └── external/
│           └── EmailServiceAdapter.java
└── interfaces/
    └── rest/
        └── customer/
            ├── controller/
            │   └── CustomerController.java
            ├── dto/
            │   ├── CreateCustomerRequest.java
            │   └── CustomerResponse.java
            └── mapper/
                └── CustomerDtoMapper.java
```

### Dependency Rules

1. **Domain Layer**: No dependencies on any other layer
2. **Application Layer**: Depends only on domain layer
3. **Infrastructure Layer**: Depends on domain layer (implements interfaces)
4. **Interfaces Layer**: Depends on application and domain layers

### Example: Adding a New Feature

**Scenario**: Add "Update Customer Email" feature

1. **Domain Layer**: Add method to Customer aggregate
   ```java
   public void updateEmail(Email newEmail) {
       validateEmail(newEmail);
       this.email = newEmail;
       collectEvent(CustomerEmailUpdatedEvent.create(id, newEmail));
   }
   ```

2. **Application Layer**: Create command and service method
   ```java
   public void updateCustomerEmail(UpdateEmailCommand command) {
       Customer customer = customerRepository.findById(command.customerId());
       customer.updateEmail(command.newEmail());
       customerRepository.save(customer);
       eventService.publishEventsFromAggregate(customer);
   }
   ```

3. **Infrastructure Layer**: No changes needed (repository already exists)

4. **Interfaces Layer**: Add REST endpoint
   ```java
   @PutMapping("/{id}/email")
   public ResponseEntity<Void> updateEmail(@PathVariable String id, @RequestBody UpdateEmailRequest request) {
       applicationService.updateCustomerEmail(new UpdateEmailCommand(id, request.email()));
       return ResponseEntity.ok().build();
   }
   ```

### Testing Strategy

**Unit Tests** (Domain Layer):
```java
@Test
void should_update_email_when_valid_email_provided() {
    // Given
    Customer customer = createCustomer();
    Email newEmail = new Email("new@example.com");
    
    // When
    customer.updateEmail(newEmail);
    
    // Then
    assertThat(customer.getEmail()).isEqualTo(newEmail);
    assertThat(customer.getUncommittedEvents()).hasSize(1);
}
```

**Integration Tests** (Infrastructure Layer):
```java
@DataJpaTest
@Test
void should_save_and_retrieve_customer() {
    // Given
    Customer customer = createCustomer();
    
    // When
    customerRepository.save(customer);
    Customer retrieved = customerRepository.findById(customer.getId());
    
    // Then
    assertThat(retrieved).isEqualTo(customer);
}
```

**API Tests** (Interfaces Layer):
```java
@WebMvcTest(CustomerController.class)
@Test
void should_update_customer_email() throws Exception {
    // When & Then
    mockMvc.perform(put("/api/v1/customers/123/email")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"new@example.com\"}"))
        .andExpect(status().isOk());
}
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
