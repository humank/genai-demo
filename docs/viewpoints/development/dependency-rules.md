# Dependency Rules

> **Last Updated**: 2025-10-23  
> **Status**: Active  
> **Stakeholders**: Developers, Architects

## Overview

This document defines the strict dependency rules that govern the Hexagonal Architecture implementation. These rules ensure architectural integrity, maintainability, and testability of the system. All rules are enforced through automated ArchUnit tests.

## Architectural Principles

### The Dependency Rule

The fundamental principle of Hexagonal Architecture is:

> **Dependencies point inward. Inner layers know nothing about outer layers.**

```text
┌─────────────────────────────────────────────────────────┐
│                    Interfaces Layer                      │
│                    (Outer Layer)                         │
└────────────────────┬────────────────────────────────────┘
                     │ depends on ↓
┌─────────────────────────────────────────────────────────┐
│                  Application Layer                       │
│                  (Orchestration)                         │
└────────────────────┬────────────────────────────────────┘
                     │ depends on ↓
┌─────────────────────────────────────────────────────────┐
│                    Domain Layer                          │
│                  (Business Logic)                        │
│                   ← NO DEPENDENCIES →                    │
└─────────────────────────────────────────────────────────┘
                     ↑ implements interfaces
┌─────────────────────────────────────────────────────────┐
│                Infrastructure Layer                      │
│              (Technical Implementations)                 │
└─────────────────────────────────────────────────────────┘
```

### Key Principles

1. **Domain Independence**: Domain layer has ZERO dependencies on other layers
2. **Dependency Inversion**: Infrastructure implements domain interfaces
3. **Unidirectional Flow**: Dependencies flow in one direction only
4. **No Circular Dependencies**: No circular dependencies between packages
5. **Interface Segregation**: Small, focused interfaces

## Layer Dependency Rules

### Domain Layer Rules

**Package**: `solid.humank.genaidemo.domain`

#### Allowed Dependencies

✅ **Java Standard Library**

```java
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
```

✅ **Domain-Specific Libraries**

```java
import javax.money.*;  // Money API (if used)
```

✅ **Other Domain Packages** (within same bounded context)

```java
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
```

✅ **Shared Kernel**

```java
import solid.humank.genaidemo.domain.shared.valueobject.Money;
import solid.humank.genaidemo.domain.shared.exception.DomainException;
```

✅ **Minimal Spring Annotations** (for domain services only)

```java
import org.springframework.stereotype.Component;  // For domain services only
```

#### Prohibited Dependencies

❌ **NO Application Layer**

```java
// ❌ WRONG
import solid.humank.genaidemo.application.customer.CustomerApplicationService;
```

❌ **NO Infrastructure Layer**

```java
// ❌ WRONG
import solid.humank.genaidemo.infrastructure.customer.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
```

❌ **NO Interfaces Layer**

```java
// ❌ WRONG
import solid.humank.genaidemo.interfaces.rest.customer.controller.CustomerController;
```

❌ **NO JPA Annotations**

```java
// ❌ WRONG - Domain objects should NOT have JPA annotations
@Entity
@Table(name = "customers")
public class Customer { }
```

❌ **NO Spring Framework** (except @Component for services)

```java
// ❌ WRONG
@Service
@Transactional
public class Customer { }
```

❌ **NO HTTP/REST Libraries**

```java
// ❌ WRONG
import org.springframework.web.bind.annotation.*;
```

#### ArchUnit Rules for Domain Layer

```java
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage(
        "..domain..",
        "java..",
        "javax..",
        "org.springframework.stereotype.."  // Only @Component
    )
    .because("Domain layer must be independent of infrastructure");

@ArchTest
static final ArchRule domainShouldNotDependOnApplication = noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..application..")
    .because("Domain should not depend on application layer");

@ArchTest
static final ArchRule domainShouldNotDependOnInfrastructure = noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..")
    .because("Domain should not depend on infrastructure layer");

@ArchTest
static final ArchRule domainShouldNotDependOnInterfaces = noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAPackage("..interfaces..")
    .because("Domain should not depend on interfaces layer");

@ArchTest
static final ArchRule domainShouldNotUseJPA = noClasses()
    .that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("javax.persistence..", "jakarta.persistence..")
    .because("Domain should not use JPA annotations");
```

### Application Layer Rules

**Package**: `solid.humank.genaidemo.application`

#### Allowed Dependencies

✅ **Domain Layer**

```java
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
import solid.humank.genaidemo.domain.customer.events.CustomerCreatedEvent;
```

✅ **Spring Framework** (for transactions and dependency injection)

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

✅ **Java Standard Library**

```java
import java.util.*;
import java.time.*;
```

#### Prohibited Dependencies

❌ **NO Infrastructure Layer**

```java
// ❌ WRONG - Application should not depend on infrastructure implementations
import solid.humank.genaidemo.infrastructure.customer.persistence.entity.CustomerEntity;
import solid.humank.genaidemo.infrastructure.customer.persistence.repository.JpaCustomerRepository;
```

❌ **NO Interfaces Layer**

```java
// ❌ WRONG
import solid.humank.genaidemo.interfaces.rest.customer.controller.CustomerController;
```

❌ **NO JPA/Hibernate**

```java
// ❌ WRONG
import javax.persistence.*;
import org.hibernate.*;
```

❌ **NO HTTP/REST**

```java
// ❌ WRONG
import org.springframework.web.bind.annotation.*;
```

#### ArchUnit Rules for Application Layer

```java
@ArchTest
static final ArchRule applicationLayerRules = classes()
    .that().resideInAPackage("..application..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage(
        "..application..",
        "..domain..",
        "java..",
        "org.springframework.."
    )
    .because("Application layer should only depend on domain layer");

@ArchTest
static final ArchRule applicationShouldNotDependOnInfrastructure = noClasses()
    .that().resideInAPackage("..application..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..")
    .because("Application should not depend on infrastructure implementations");

@ArchTest
static final ArchRule applicationShouldNotDependOnInterfaces = noClasses()
    .that().resideInAPackage("..application..")
    .should().dependOnClassesThat()
    .resideInAPackage("..interfaces..")
    .because("Application should not depend on interfaces layer");

@ArchTest
static final ArchRule applicationServicesShouldBeTransactional = classes()
    .that().resideInAPackage("..application..")
    .and().haveSimpleNameEndingWith("ApplicationService")
    .should().beAnnotatedWith(Transactional.class)
    .because("Application services should manage transactions");
```

### Infrastructure Layer Rules

**Package**: `solid.humank.genaidemo.infrastructure`

#### Allowed Dependencies

✅ **Domain Layer** (interfaces only)

```java
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;
```

✅ **Spring Framework**

```java
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Component;
import org.springframework.data.jpa.repository.JpaRepository;
```

✅ **JPA/Hibernate**

```java
import javax.persistence.*;
import org.hibernate.*;
```

✅ **External Libraries**

```java
import com.fasterxml.jackson.databind.*;
import org.apache.kafka.clients.*;
import redis.clients.jedis.*;
```

✅ **Java Standard Library**

```java
import java.util.*;
import java.time.*;
```

#### Prohibited Dependencies

❌ **NO Application Layer**

```java
// ❌ WRONG
import solid.humank.genaidemo.application.customer.CustomerApplicationService;
```

❌ **NO Interfaces Layer**

```java
// ❌ WRONG
import solid.humank.genaidemo.interfaces.rest.customer.controller.CustomerController;
```

#### ArchUnit Rules for Infrastructure Layer

```java
@ArchTest
static final ArchRule infrastructureLayerRules = classes()
    .that().resideInAPackage("..infrastructure..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage(
        "..infrastructure..",
        "..domain..",
        "java..",
        "javax..",
        "org.springframework..",
        "org.hibernate..",
        "com.fasterxml..",
        "org.apache.kafka..",
        "redis.clients.."
    )
    .because("Infrastructure should only depend on domain interfaces");

@ArchTest
static final ArchRule infrastructureShouldNotDependOnApplication = noClasses()
    .that().resideInAPackage("..infrastructure..")
    .should().dependOnClassesThat()
    .resideInAPackage("..application..")
    .because("Infrastructure should not depend on application layer");

@ArchTest
static final ArchRule infrastructureShouldNotDependOnInterfaces = noClasses()
    .that().resideInAPackage("..infrastructure..")
    .should().dependOnClassesThat()
    .resideInAPackage("..interfaces..")
    .because("Infrastructure should not depend on interfaces layer");

@ArchTest
static final ArchRule repositoryImplementationsShouldImplementDomainInterfaces = classes()
    .that().resideInAPackage("..infrastructure..repository..")
    .and().haveSimpleNameEndingWith("RepositoryImpl")
    .should().implement(JavaClass.Predicates.resideInAPackage("..domain..repository.."))
    .because("Repository implementations should implement domain repository interfaces");
```

### Interfaces Layer Rules

**Package**: `solid.humank.genaidemo.interfaces`

#### Allowed Dependencies

✅ **Application Layer**

```java
import solid.humank.genaidemo.application.customer.CustomerApplicationService;
import solid.humank.genaidemo.application.customer.command.CreateCustomerCommand;
```

✅ **Domain Layer**

```java
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
```

✅ **Spring Web**

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
```

✅ **Validation**

```java
import javax.validation.*;
import org.springframework.validation.annotation.*;
```

✅ **Java Standard Library**

```java
import java.util.*;
import java.time.*;
```

#### Prohibited Dependencies

❌ **NO Infrastructure Layer** (except via dependency injection)

```java
// ❌ WRONG - Direct dependency on infrastructure
import solid.humank.genaidemo.infrastructure.customer.persistence.entity.CustomerEntity;

// ✅ CORRECT - Dependency injection of domain interface
@RestController
public class CustomerController {
    private final CustomerApplicationService customerService;  // Injected
}
```

#### ArchUnit Rules for Interfaces Layer

```java
@ArchTest
static final ArchRule interfacesLayerRules = classes()
    .that().resideInAPackage("..interfaces..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage(
        "..interfaces..",
        "..application..",
        "..domain..",
        "java..",
        "javax..",
        "org.springframework.."
    )
    .because("Interfaces layer should depend on application and domain layers");

@ArchTest
static final ArchRule interfacesShouldNotDependOnInfrastructure = noClasses()
    .that().resideInAPackage("..interfaces..")
    .should().dependOnClassesThat()
    .resideInAPackage("..infrastructure..")
    .because("Interfaces should not directly depend on infrastructure");

@ArchTest
static final ArchRule controllersShouldBeAnnotated = classes()
    .that().resideInAPackage("..interfaces..controller..")
    .and().haveSimpleNameEndingWith("Controller")
    .should().beAnnotatedWith(RestController.class)
    .because("Controllers should be annotated with @RestController");
```

## Bounded Context Dependency Rules

### Context Independence

Each bounded context must be independent:

```java
@ArchTest
static final ArchRule boundedContextsShouldBeIndependent = noClasses()
    .that().resideInAPackage("..domain.customer..")
    .should().dependOnClassesThat()
    .resideInAnyPackage(
        "..domain.order..",
        "..domain.product..",
        "..domain.payment.."
        // ... other contexts
    )
    .because("Bounded contexts should be independent");
```

### Shared Kernel Usage

Only the shared kernel can be used across contexts:

```java
@ArchTest
static final ArchRule onlySharedKernelCanBeCrossContext = classes()
    .that().resideInAPackage("..domain..")
    .and().resideOutsideOfPackage("..domain.shared..")
    .should().onlyAccessClassesThat()
    .resideInAnyPackage(
        "..domain.shared..",
        "java.."
    )
    .orShould().resideInTheSamePackageAs(JavaClass.class)
    .because("Only shared kernel can be used across bounded contexts");
```

## DDD Pattern Rules

### Aggregate Root Rules

```java
@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().resideInAPackage("..domain..model.aggregate..")
    .andShould().implement(AggregateRootInterface.class)
    .because("Aggregate roots should be in aggregate package and implement interface");

@ArchTest
static final ArchRule aggregateRootsShouldCollectEvents = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().haveOnlyPrivateConstructors()
    .orShould().haveOnlyPackagePrivateConstructors()
    .because("Aggregate roots should control their creation");
```

### Domain Event Rules

```java
@ArchTest
static final ArchRule domainEventRules = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords()
    .andShould().resideInAPackage("..domain..events..")
    .andShould().haveSimpleNameEndingWith("Event")
    .because("Domain events should be immutable records in events package");

@ArchTest
static final ArchRule domainEventsShouldBeImmutable = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords()
    .because("Domain events must be immutable");
```

### Value Object Rules

```java
@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().resideInAPackage("..domain..valueobject..")
    .should().beRecords()
    .orShould().haveModifier(JavaModifier.FINAL)
    .because("Value objects should be immutable");

@ArchTest
static final ArchRule valueObjectsShouldNotHaveSetters = noClasses()
    .that().resideInAPackage("..domain..valueobject..")
    .should().haveMethodsThat(DescribedPredicate.describe("are setters",
        method -> method.getName().startsWith("set")))
    .because("Value objects should be immutable");
```

### Repository Rules

```java
@ArchTest
static final ArchRule repositoryInterfaceRules = classes()
    .that().haveSimpleNameEndingWith("Repository")
    .and().areInterfaces()
    .should().resideInAPackage("..domain..repository..")
    .because("Repository interfaces should be in domain layer");

@ArchTest
static final ArchRule repositoryImplementationRules = classes()
    .that().haveSimpleNameEndingWith("RepositoryImpl")
    .should().resideInAPackage("..infrastructure..repository..")
    .andShould().implement(JavaClass.Predicates.resideInAPackage("..domain..repository.."))
    .because("Repository implementations should be in infrastructure layer");
```

## Naming Convention Rules

### Package Naming

```java
@ArchTest
static final ArchRule packagesShouldBeLowercase = classes()
    .should().resideInAPackage("..solid.humank.genaidemo..")
    .because("All packages should be lowercase");
```

### Class Naming

```java
@ArchTest
static final ArchRule servicesShouldBeNamedCorrectly = classes()
    .that().areAnnotatedWith(Service.class)
    .should().haveSimpleNameEndingWith("Service")
    .because("Services should end with 'Service'");

@ArchTest
static final ArchRule controllersShouldBeNamedCorrectly = classes()
    .that().areAnnotatedWith(RestController.class)
    .should().haveSimpleNameEndingWith("Controller")
    .because("Controllers should end with 'Controller'");

@ArchTest
static final ArchRule repositoriesShouldBeNamedCorrectly = classes()
    .that().areAnnotatedWith(Repository.class)
    .should().haveSimpleNameEndingWith("Repository")
    .orShould().haveSimpleNameEndingWith("RepositoryImpl")
    .because("Repositories should end with 'Repository' or 'RepositoryImpl'");
```

## Circular Dependency Prevention

```java
@ArchTest
static final ArchRule noCircularDependencies = slices()
    .matching("solid.humank.genaidemo.(*)..")
    .should().beFreeOfCycles()
    .because("There should be no circular dependencies between packages");
```

## Running Architecture Tests

### Gradle Command

```bash
# Run all architecture tests
./gradlew archUnit

# Run specific test class
./gradlew test --tests "*ArchitectureTest"
```

### Test Location

```text
app/src/test/java/solid/humank/genaidemo/architecture/
├── ArchitectureTest.java              # Main architecture tests
├── LayerDependencyTest.java           # Layer dependency tests
├── BoundedContextTest.java            # Bounded context tests
├── DddPatternTest.java                # DDD pattern tests
└── NamingConventionTest.java          # Naming convention tests
```

### Example Test Class

```java
@AnalyzeClasses(packages = "solid.humank.genaidemo")
public class ArchitectureTest {
    
    @ArchTest
    static final ArchRule domainLayerRules = classes()
        .that().resideInAPackage("..domain..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage("..domain..", "java..")
        .because("Domain layer must be independent");
    
    // More rules...
}
```

## Violation Examples and Fixes

### Example 1: Domain Depending on Infrastructure

❌ **WRONG**:

```java
// domain/customer/model/aggregate/Customer.java
package solid.humank.genaidemo.domain.customer.model.aggregate;

import javax.persistence.*;  // ❌ WRONG!

@Entity  // ❌ WRONG!
public class Customer {
    @Id  // ❌ WRONG!
    private String id;
}
```

✅ **CORRECT**:

```java
// domain/customer/model/aggregate/Customer.java
package solid.humank.genaidemo.domain.customer.model.aggregate;

@AggregateRoot
public class Customer {
    private CustomerId id;  // Value object
    
    // Pure business logic, no infrastructure concerns
}

// infrastructure/customer/persistence/entity/CustomerEntity.java
package solid.humank.genaidemo.infrastructure.customer.persistence.entity;

import javax.persistence.*;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    private String id;
    
    // JPA mapping
}
```

### Example 2: Application Depending on Infrastructure

❌ **WRONG**:

```java
// application/customer/CustomerApplicationService.java
package solid.humank.genaidemo.application.customer;

import solid.humank.genaidemo.infrastructure.customer.persistence.repository.JpaCustomerRepository;  // ❌ WRONG!

@Service
public class CustomerApplicationService {
    private final JpaCustomerRepository repository;  // ❌ WRONG!
}
```

✅ **CORRECT**:

```java
// application/customer/CustomerApplicationService.java
package solid.humank.genaidemo.application.customer;

import solid.humank.genaidemo.domain.customer.repository.CustomerRepository;  // ✅ Domain interface

@Service
public class CustomerApplicationService {
    private final CustomerRepository repository;  // ✅ Domain interface
    
    public CustomerApplicationService(CustomerRepository repository) {
        this.repository = repository;  // Infrastructure implementation injected
    }
}
```

### Example 3: Cross-Context Dependencies

❌ **WRONG**:

```java
// domain/order/model/aggregate/Order.java
package solid.humank.genaidemo.domain.order.model.aggregate;

import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;  // ❌ WRONG!

public class Order {
    private Customer customer;  // ❌ Direct dependency on another context
}
```

✅ **CORRECT**:

```java
// domain/order/model/aggregate/Order.java
package solid.humank.genaidemo.domain.order.model.aggregate;

import solid.humank.genaidemo.domain.order.model.valueobject.CustomerId;  // ✅ Value object reference

public class Order {
    private CustomerId customerId;  // ✅ Reference by ID only
}
```

## Best Practices

### 1. Design by Interface

Always depend on interfaces, not implementations:

```java
// ✅ GOOD
private final CustomerRepository repository;  // Interface

// ❌ BAD
private final JpaCustomerRepository repository;  // Implementation
```

### 2. Use Dependency Injection

Let Spring inject dependencies:

```java
@Service
public class CustomerApplicationService {
    private final CustomerRepository repository;
    
    // ✅ Constructor injection
    public CustomerApplicationService(CustomerRepository repository) {
        this.repository = repository;
    }
}
```

### 3. Keep Domain Pure

Domain objects should have no framework dependencies:

```java
// ✅ GOOD - Pure domain object
public class Customer {
    private CustomerId id;
    private Email email;
    
    public void updateEmail(Email newEmail) {
        // Pure business logic
    }
}
```

### 4. Use Events for Cross-Context Communication

```java
// ✅ GOOD - Event-driven communication
@Component
public class OrderSubmittedEventHandler {
    @EventListener
    public void handle(OrderSubmittedEvent event) {
        // Update inventory in different context
    }
}
```

## Enforcement Strategy

### 1. Automated Tests

- Run ArchUnit tests in CI/CD pipeline
- Fail build on violations
- Generate violation reports

### 2. Code Reviews

- Review dependency changes carefully
- Check for architectural violations
- Ensure new code follows rules

### 3. IDE Configuration

- Configure IDE to highlight violations
- Use import organization rules
- Enable static analysis

### 4. Documentation

- Keep this document updated
- Document exceptions (if any)
- Provide examples

## Navigation

### Related Documents

- [← Module Organization](module-organization.md) - Package structure
- [Build Process](build-process.md) - Build and test execution →
- [Overview](overview.md) - Development Viewpoint overview

### Related Perspectives

- [Evolution Perspective](../../perspectives/evolution/README.md) - Maintainability

### Development Guides

- [Coding Standards](../../development/coding-standards/java-standards.md)
- [Architecture Testing](../../development/testing/architecture-testing.md)

---

**Previous**: [← Module Organization](module-organization.md) | **Next**: [Build Process →](build-process.md)
