# Design Principles

## Overview

This document defines the design principles that guide how we write code, focusing on Extreme Programming (XP) values and Object-Oriented design principles.

**Purpose**: Provide quick checks and rules for design decisions.
**Detailed Examples**: See `.kiro/examples/design-patterns/` and `.kiro/examples/xp-practices/` for comprehensive guides.

---

## Extreme Programming (XP) Core Values

### Simplicity ⭐
- [ ] Do the simplest thing that could possibly work
- [ ] YAGNI (You Aren't Gonna Need It)
- [ ] Remove duplication
- [ ] Minimize the number of classes and methods

### Communication
- [ ] Code communicates intent clearly
- [ ] Use ubiquitous language from domain
- [ ] Pair programming for knowledge sharing
- [ ] Collective code ownership

### Feedback
- [ ] Test-first development
- [ ] Continuous integration
- [ ] Short iterations
- [ ] Customer involvement

### Courage
- [ ] Refactor when needed
- [ ] Throw away code when necessary
- [ ] Admit when you don't know
- [ ] Accept feedback

**Detailed XP Practices**: #[[file:../examples/xp-practices/]]

---

## Tell, Don't Ask ⭐

**Principle**: Objects should tell other objects what to do, not ask for their state and make decisions.

### Must Follow
- [ ] Behavior should be in the object that owns the data
- [ ] Avoid getter chains
- [ ] Methods should do work, not just return data
- [ ] Push decisions into the object

### Quick Check
```java
// ❌ BAD: Asking for state and making decisions
if (order.getStatus() == OrderStatus.CREATED) {
    order.setStatus(OrderStatus.PENDING);
    order.setUpdatedAt(LocalDateTime.now());
}

// ✅ GOOD: Telling the object what to do
order.submit();  // Object handles its own state transitions
```

**Full Examples**: #[[file:../examples/design-patterns/tell-dont-ask-examples.md]]

---

## Law of Demeter (Principle of Least Knowledge)

**Principle**: Only talk to your immediate friends, don't talk to strangers.

### Must Follow
- [ ] Only call methods on:
  - The object itself (this)
  - Objects passed as parameters
  - Objects created locally
  - Direct component objects
- [ ] Avoid method chaining across objects
- [ ] One dot rule (with exceptions for fluent APIs)

### Quick Check
```java
// ❌ BAD: Violates Law of Demeter
customer.getAddress().getCity().getPostalCode();

// ✅ GOOD: Ask the object directly
customer.getPostalCode();  // Customer knows how to get it
```

**Full Examples**: #[[file:../examples/design-patterns/law-of-demeter-examples.md]]

---

## Composition Over Inheritance

**Principle**: Favor object composition over class inheritance.

### Must Follow
- [ ] Use composition for "has-a" relationships
- [ ] Use inheritance only for "is-a" relationships
- [ ] Prefer interfaces over abstract classes
- [ ] Keep inheritance hierarchies shallow (max 2-3 levels)

### Quick Check
```java
// ❌ BAD: Inheritance for code reuse
class OrderWithDiscount extends Order {
    // Inheriting just to add discount behavior
}

// ✅ GOOD: Composition
class Order {
    private DiscountStrategy discountStrategy;
    
    public Money calculateTotal() {
        Money subtotal = calculateSubtotal();
        return discountStrategy.apply(subtotal);
    }
}
```

**Full Examples**: #[[file:../examples/design-patterns/composition-over-inheritance-examples.md]]

---

## SOLID Principles

### Single Responsibility Principle (SRP)
**Principle**: A class should have only one reason to change.

#### Must Follow
- [ ] Each class has one clear responsibility
- [ ] Methods do one thing well
- [ ] Separate business logic from infrastructure
- [ ] Split large classes into focused ones

#### Quick Check
```java
// ❌ BAD: Multiple responsibilities
class OrderService {
    void processOrder() { }
    void sendEmail() { }
    void updateInventory() { }
}

// ✅ GOOD: Single responsibility
class OrderProcessingService {
    void processOrder() { }
}
```

---

### Open/Closed Principle (OCP)
**Principle**: Open for extension, closed for modification.

#### Must Follow
- [ ] Use interfaces and abstract classes
- [ ] Strategy pattern for varying behavior
- [ ] Avoid modifying existing code
- [ ] Extend through new implementations

---

### Liskov Substitution Principle (LSP)
**Principle**: Subtypes must be substitutable for their base types.

#### Must Follow
- [ ] Subclasses honor parent class contracts
- [ ] Don't strengthen preconditions
- [ ] Don't weaken postconditions
- [ ] Preserve invariants

---

### Interface Segregation Principle (ISP)
**Principle**: Clients shouldn't depend on interfaces they don't use.

#### Must Follow
- [ ] Keep interfaces small and focused
- [ ] Split large interfaces into smaller ones
- [ ] Role-based interfaces
- [ ] Avoid "fat" interfaces

#### Quick Check
```java
// ❌ BAD: Fat interface
interface OrderOperations {
    void create(); void update(); void delete();
    void approve(); void ship(); void cancel();
}

// ✅ GOOD: Segregated interfaces
interface OrderCreation { void create(); }
interface OrderManagement { void update(); void cancel(); }
interface OrderFulfillment { void approve(); void ship(); }
```

---

### Dependency Inversion Principle (DIP)
**Principle**: Depend on abstractions, not concretions.

#### Must Follow
- [ ] High-level modules don't depend on low-level modules
- [ ] Both depend on abstractions (interfaces)
- [ ] Use dependency injection
- [ ] Program to interfaces, not implementations

#### Quick Check
```java
// ❌ BAD: Depends on concrete implementation
class OrderService {
    private PostgresOrderRepository repository = new PostgresOrderRepository();
}

// ✅ GOOD: Depends on abstraction
class OrderService {
    private final OrderRepository repository;
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Full Examples**: #[[file:../examples/design-patterns/dependency-injection-examples.md]]

---

## Four Rules of Simple Design (Kent Beck)

### Priority Order
1. **Passes all tests** - Code must work correctly
2. **Reveals intention** - Code communicates clearly
3. **No duplication** - DRY principle
4. **Fewest elements** - Minimal classes and methods

### Must Follow
- [ ] Tests pass (correctness first)
- [ ] Clear naming and structure
- [ ] Extract duplication
- [ ] Remove unnecessary abstractions

**Detailed Guide**: #[[file:../examples/xp-practices/simple-design-examples.md]]

---

## Design Smells to Avoid

### Code Smells
- [ ] ❌ Long methods (> 20 lines)
- [ ] ❌ Large classes (> 200 lines)
- [ ] ❌ Long parameter lists (> 3 parameters)
- [ ] ❌ Primitive obsession
- [ ] ❌ Feature envy
- [ ] ❌ Data clumps

### Design Smells
- [ ] ❌ Rigidity (hard to change)
- [ ] ❌ Fragility (breaks in many places)
- [ ] ❌ Immobility (hard to reuse)
- [ ] ❌ Viscosity (easier to do wrong thing)
- [ ] ❌ Needless complexity
- [ ] ❌ Needless repetition

**Refactoring Guide**: #[[file:../examples/design-patterns/design-smells-refactoring.md]]

---

## Quick Reference Card

| Principle | Key Question | Red Flag |
|-----------|-------------|----------|
| Tell, Don't Ask | "Am I asking for data to make decisions?" | Getter chains, if-else on state |
| Law of Demeter | "Am I talking to strangers?" | Multiple dots: `a.b().c().d()` |
| SRP | "Does this class have one reason to change?" | Class does multiple things |
| OCP | "Can I extend without modifying?" | Modifying existing code for new features |
| LSP | "Can I substitute subclass for parent?" | Subclass breaks parent contract |
| ISP | "Do clients use all interface methods?" | Large interfaces with unused methods |
| DIP | "Do I depend on abstractions?" | `new ConcreteClass()` in business logic |
| Composition | "Is this really an 'is-a' relationship?" | Inheritance for code reuse |

---

## Validation

### Code Review Checklist
- [ ] Does code follow Tell, Don't Ask?
- [ ] Are there any Law of Demeter violations?
- [ ] Is composition used appropriately?
- [ ] Do classes have single responsibility?
- [ ] Are dependencies injected properly?
- [ ] Are interfaces small and focused?

### Automated Checks
```bash
# Check for design violations
./gradlew archUnit

# Check for code smells
./gradlew pmdMain

# Check complexity
./gradlew checkstyleMain
```

---

## Related Documentation

- **Core Principles**: #[[file:core-principles.md]]
- **DDD Patterns**: #[[file:ddd-tactical-patterns.md]]
- **Code Quality Checklist**: #[[file:code-quality-checklist.md]]
- **Design Pattern Examples**: #[[file:../examples/design-patterns/]]
- **XP Practices**: #[[file:../examples/xp-practices/]]

---

**Document Version**: 1.0
**Last Updated**: 2025-01-17
**Owner**: Architecture Team
