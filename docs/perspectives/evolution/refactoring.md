# Refactoring Strategy

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Development & Architecture Team

## Overview

This document defines the refactoring strategy for the Enterprise E-Commerce Platform. Refactoring is essential for maintaining code quality, managing technical debt, and ensuring long-term maintainability as the system evolves.

## Refactoring Philosophy

### Core Principles

1. **Continuous Improvement**: Refactor regularly, not in big batches
2. **Test-Driven**: Always have tests before refactoring
3. **Small Steps**: Make small, incremental changes
4. **Boy Scout Rule**: Leave code better than you found it
5. **Pragmatic Approach**: Balance perfection with delivery

### When to Refactor

✅ **Refactor when**:
- Adding new features to existing code
- Fixing bugs in complex code
- Code review identifies issues
- Performance optimization needed
- Preparing for major changes
- Technical debt is blocking progress

❌ **Don't refactor when**:
- Under tight deadline pressure
- No tests exist (write tests first)
- You don't understand the code
- It's working and rarely changes
- Refactoring would break existing integrations

---

## Technical Debt Management

### Technical Debt Classification

| Type | Description | Priority | Action |
|------|-------------|----------|--------|
| **Critical Debt** | Security vulnerabilities, data corruption risks | P0 | Fix immediately |
| **High Debt** | Performance issues, scalability blockers | P1 | Fix within sprint |
| **Medium Debt** | Code smells, duplication, complexity | P2 | Fix within quarter |
| **Low Debt** | Minor improvements, style issues | P3 | Fix when convenient |

### Technical Debt Tracking

```java
// Document technical debt with TODO comments
public class OrderService {
    
    // TODO: TECH-DEBT-123 - Refactor to use Strategy pattern
    // Priority: Medium
    // Estimated effort: 2 days
    // Reason: Multiple if-else blocks make this hard to extend
    public Money calculateDiscount(Order order, Customer customer) {
        if (customer.isPremium()) {
            if (order.getTotalAmount().isGreaterThan(Money.of(100))) {
                return order.getTotalAmount().multiply(0.15);
            } else {
                return order.getTotalAmount().multiply(0.10);
            }
        } else if (customer.isRegular()) {
            if (order.getTotalAmount().isGreaterThan(Money.of(200))) {
                return order.getTotalAmount().multiply(0.05);
            }
        }
        return Money.zero();
    }
}
```

### Technical Debt Metrics

| Metric | Target | Current | Trend |
|--------|--------|---------|-------|
| **Code Coverage** | > 80% | 85% | ↗️ |
| **Cyclomatic Complexity** | < 10 | 8.5 | → |
| **Code Duplication** | < 3% | 2.8% | ↘️ |
| **Technical Debt Ratio** | < 5% | 4.2% | ↘️ |
| **Critical Issues** | 0 | 0 | → |
| **High Issues** | < 5 | 3 | ↘️ |

---

## Refactoring Patterns

### 1. Extract Method

**Problem**: Long methods that do multiple things

**Before**:
```java
public void processOrder(Order order) {
    // Validate order
    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Order must have items");
    }
    if (order.getCustomerId() == null) {
        throw new BusinessRuleViolationException("Order must have customer");
    }
    
    // Calculate total
    Money total = Money.zero();
    for (OrderItem item : order.getItems()) {
        total = total.add(item.getPrice().multiply(item.getQuantity()));
    }
    order.setTotalAmount(total);
    
    // Apply discount
    Customer customer = customerRepository.findById(order.getCustomerId());
    if (customer.isPremium()) {
        Money discount = total.multiply(0.10);
        order.setDiscount(discount);
        order.setTotalAmount(total.subtract(discount));
    }
    
    // Save order
    orderRepository.save(order);
    
    // Send notification
    emailService.sendOrderConfirmation(customer.getEmail(), order);
}
```

**After**:
```java
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);
    applyDiscount(order);
    saveOrder(order);
    sendNotification(order);
}

private void validateOrder(Order order) {
    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Order must have items");
    }
    if (order.getCustomerId() == null) {
        throw new BusinessRuleViolationException("Order must have customer");
    }
}

private void calculateTotal(Order order) {
    Money total = order.getItems().stream()
        .map(item -> item.getPrice().multiply(item.getQuantity()))
        .reduce(Money.zero(), Money::add);
    order.setTotalAmount(total);
}

private void applyDiscount(Order order) {
    Customer customer = customerRepository.findById(order.getCustomerId());
    if (customer.isPremium()) {
        Money discount = order.getTotalAmount().multiply(0.10);
        order.setDiscount(discount);
        order.setTotalAmount(order.getTotalAmount().subtract(discount));
    }
}

private void saveOrder(Order order) {
    orderRepository.save(order);
}

private void sendNotification(Order order) {
    Customer customer = customerRepository.findById(order.getCustomerId());
    emailService.sendOrderConfirmation(customer.getEmail(), order);
}
```

---

### 2. Replace Conditional with Polymorphism

**Problem**: Complex if-else or switch statements

**Before**:
```java
public Money calculateShipping(Order order, String shippingMethod) {
    if ("STANDARD".equals(shippingMethod)) {
        return Money.of(5.00);
    } else if ("EXPRESS".equals(shippingMethod)) {
        if (order.getTotalAmount().isGreaterThan(Money.of(100))) {
            return Money.of(10.00);
        } else {
            return Money.of(15.00);
        }
    } else if ("OVERNIGHT".equals(shippingMethod)) {
        return Money.of(25.00);
    } else {
        throw new IllegalArgumentException("Unknown shipping method");
    }
}
```

**After**:
```java
// Strategy interface
public interface ShippingStrategy {
    Money calculateCost(Order order);
    String getMethodName();
}

// Implementations
@Component("STANDARD")
public class StandardShipping implements ShippingStrategy {
    @Override
    public Money calculateCost(Order order) {
        return Money.of(5.00);
    }
    
    @Override
    public String getMethodName() {
        return "STANDARD";
    }
}

@Component("EXPRESS")
public class ExpressShipping implements ShippingStrategy {
    @Override
    public Money calculateCost(Order order) {
        return order.getTotalAmount().isGreaterThan(Money.of(100))
            ? Money.of(10.00)
            : Money.of(15.00);
    }
    
    @Override
    public String getMethodName() {
        return "EXPRESS";
    }
}

@Component("OVERNIGHT")
public class OvernightShipping implements ShippingStrategy {
    @Override
    public Money calculateCost(Order order) {
        return Money.of(25.00);
    }
    
    @Override
    public String getMethodName() {
        return "OVERNIGHT";
    }
}

// Service
@Service
public class ShippingService {
    private final Map<String, ShippingStrategy> strategies;
    
    public ShippingService(List<ShippingStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                ShippingStrategy::getMethodName,
                Function.identity()
            ));
    }
    
    public Money calculateShipping(Order order, String shippingMethod) {
        ShippingStrategy strategy = strategies.get(shippingMethod);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown shipping method: " + shippingMethod);
        }
        return strategy.calculateCost(order);
    }
}
```

---

### 3. Introduce Parameter Object

**Problem**: Methods with many parameters

**Before**:
```java
public Order createOrder(
    String customerId,
    String shippingAddress,
    String billingAddress,
    String paymentMethod,
    String shippingMethod,
    List<OrderItem> items,
    String promoCode
) {
    // Implementation
}
```

**After**:
```java
public record CreateOrderRequest(
    String customerId,
    String shippingAddress,
    String billingAddress,
    String paymentMethod,
    String shippingMethod,
    List<OrderItem> items,
    String promoCode
) {}

public Order createOrder(CreateOrderRequest request) {
    // Implementation
}
```

---

### 4. Replace Magic Numbers with Constants

**Problem**: Hard-coded values scattered throughout code

**Before**:
```java
public boolean isEligibleForDiscount(Customer customer) {
    return customer.getOrderCount() > 10 
        && customer.getTotalSpent().isGreaterThan(Money.of(1000));
}

public Money calculateLoyaltyPoints(Money amount) {
    return amount.multiply(0.05);
}
```

**After**:
```java
public class CustomerConstants {
    public static final int DISCOUNT_ELIGIBILITY_ORDER_COUNT = 10;
    public static final Money DISCOUNT_ELIGIBILITY_TOTAL_SPENT = Money.of(1000);
    public static final double LOYALTY_POINTS_RATE = 0.05;
}

public boolean isEligibleForDiscount(Customer customer) {
    return customer.getOrderCount() > CustomerConstants.DISCOUNT_ELIGIBILITY_ORDER_COUNT
        && customer.getTotalSpent().isGreaterThan(CustomerConstants.DISCOUNT_ELIGIBILITY_TOTAL_SPENT);
}

public Money calculateLoyaltyPoints(Money amount) {
    return amount.multiply(CustomerConstants.LOYALTY_POINTS_RATE);
}
```

---

### 5. Extract Class

**Problem**: Class doing too many things

**Before**:
```java
public class Order {
    private OrderId id;
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    
    // Shipping-related fields
    private String shippingAddress;
    private String shippingMethod;
    private Money shippingCost;
    private LocalDateTime estimatedDelivery;
    
    // Payment-related fields
    private String paymentMethod;
    private String paymentTransactionId;
    private LocalDateTime paymentDate;
    
    // Too many responsibilities!
}
```

**After**:
```java
public class Order {
    private OrderId id;
    private CustomerId customerId;
    private List<OrderItem> items;
    private Money totalAmount;
    private OrderStatus status;
    private ShippingInfo shippingInfo;
    private PaymentInfo paymentInfo;
}

public class ShippingInfo {
    private String address;
    private String method;
    private Money cost;
    private LocalDateTime estimatedDelivery;
    
    // Shipping-specific behavior
    public boolean isExpressShipping() {
        return "EXPRESS".equals(method);
    }
}

public class PaymentInfo {
    private String method;
    private String transactionId;
    private LocalDateTime paymentDate;
    
    // Payment-specific behavior
    public boolean isPaid() {
        return transactionId != null && paymentDate != null;
    }
}
```

---

## Refactoring Workflow

### Safe Refactoring Process

```
┌─────────────────────────────────────────────────────────┐
│           Safe Refactoring Workflow                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Step 1: Ensure Tests Exist                            │
│  - Write tests if none exist                            │
│  - Verify tests pass                                    │
│  - Achieve > 80% coverage                               │
│         │                                               │
│         ▼                                               │
│  Step 2: Make Small Change                              │
│  - One refactoring at a time                            │
│  - Keep changes focused                                 │
│  - Commit frequently                                    │
│         │                                               │
│         ▼                                               │
│  Step 3: Run Tests                                      │
│  - All tests must pass                                  │
│  - No new warnings                                      │
│  - Performance maintained                               │
│         │                                               │
│         ▼                                               │
│  Step 4: Commit                                         │
│  - Commit working code                                  │
│  - Clear commit message                                 │
│  - Reference issue/ticket                               │
│         │                                               │
│         ▼                                               │
│  Step 5: Repeat or Done                                 │
│  - Continue with next refactoring                       │
│  - Or complete and create PR                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Refactoring Checklist

Before refactoring:
- [ ] Tests exist and pass
- [ ] Code is under version control
- [ ] You understand what the code does
- [ ] You have time to complete the refactoring
- [ ] No urgent deadlines

During refactoring:
- [ ] Make one change at a time
- [ ] Run tests after each change
- [ ] Commit working code frequently
- [ ] Keep changes small and focused
- [ ] Don't add new features

After refactoring:
- [ ] All tests pass
- [ ] No new warnings or errors
- [ ] Code is more readable
- [ ] Performance is maintained or improved
- [ ] Documentation is updated

---

## Code Quality Tools

### Static Analysis

```bash
# Run PMD for code quality checks
./gradlew pmdMain

# Run SpotBugs for bug detection
./gradlew spotbugsMain

# Run Checkstyle for style violations
./gradlew checkstyleMain

# Run all quality checks
./gradlew check
```

### Code Coverage

```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html

# Enforce minimum coverage
./gradlew jacocoTestCoverageVerification
```

### Complexity Analysis

```bash
# Analyze cyclomatic complexity
./gradlew cyclomaticComplexity

# Generate complexity report
./gradlew complexityReport
```

---

## Refactoring Sprints

### Dedicated Refactoring Time

**20% Time Rule**: Developers can spend 20% of their time on technical improvements

**Quarterly Refactoring Sprint**: One sprint per quarter dedicated to refactoring

### Refactoring Sprint Planning

```
Week 1: Planning
- Identify technical debt
- Prioritize refactoring tasks
- Estimate effort
- Assign tasks

Week 2-3: Execution
- Implement refactorings
- Write/update tests
- Code reviews
- Documentation

Week 4: Validation
- Integration testing
- Performance testing
- Deploy to staging
- Monitor metrics
```

---

## Refactoring Metrics

### Before/After Comparison

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| **Lines of Code** | 1,500 | 1,200 | -20% |
| **Cyclomatic Complexity** | 15 | 8 | -47% |
| **Code Duplication** | 5% | 2% | -60% |
| **Test Coverage** | 75% | 85% | +13% |
| **Method Length (avg)** | 25 lines | 15 lines | -40% |

### ROI Calculation

```
Refactoring Cost:
- Developer time: 40 hours × $100/hour = $4,000
- Testing time: 10 hours × $100/hour = $1,000
- Total cost: $5,000

Benefits (Annual):
- Reduced bug fixing: 20 hours × $100/hour = $2,000
- Faster feature development: 30 hours × $100/hour = $3,000
- Improved maintainability: $2,000
- Total benefit: $7,000

ROI = (Benefit - Cost) / Cost = ($7,000 - $5,000) / $5,000 = 40%
```

---

## Common Refactoring Scenarios

### Scenario 1: Legacy Code Modernization

**Goal**: Update old code to use modern Java features

```java
// Before: Java 8 style
List<String> customerNames = new ArrayList<>();
for (Customer customer : customers) {
    if (customer.isActive()) {
        customerNames.add(customer.getName());
    }
}

// After: Modern Java style
List<String> customerNames = customers.stream()
    .filter(Customer::isActive)
    .map(Customer::getName)
    .toList();
```

### Scenario 2: Performance Optimization

**Goal**: Improve query performance

```java
// Before: N+1 query problem
public List<OrderDTO> getOrders(String customerId) {
    List<Order> orders = orderRepository.findByCustomerId(customerId);
    return orders.stream()
        .map(order -> {
            // N+1: Fetches items for each order separately
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return OrderDTO.from(order, items);
        })
        .toList();
}

// After: Single query with JOIN FETCH
public List<OrderDTO> getOrders(String customerId) {
    List<Order> orders = orderRepository.findByCustomerIdWithItems(customerId);
    return orders.stream()
        .map(OrderDTO::from)
        .toList();
}

// Repository
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.customerId = :customerId")
List<Order> findByCustomerIdWithItems(@Param("customerId") String customerId);
```

### Scenario 3: Improving Testability

**Goal**: Make code easier to test

```java
// Before: Hard to test (tight coupling)
public class OrderService {
    public void processOrder(Order order) {
        // Direct instantiation - hard to mock
        PaymentGateway gateway = new StripePaymentGateway();
        gateway.charge(order.getTotalAmount());
        
        // Static method call - hard to test
        EmailSender.send(order.getCustomerEmail(), "Order confirmed");
    }
}

// After: Easy to test (dependency injection)
public class OrderService {
    private final PaymentGateway paymentGateway;
    private final EmailService emailService;
    
    public OrderService(PaymentGateway paymentGateway, EmailService emailService) {
        this.paymentGateway = paymentGateway;
        this.emailService = emailService;
    }
    
    public void processOrder(Order order) {
        paymentGateway.charge(order.getTotalAmount());
        emailService.send(order.getCustomerEmail(), "Order confirmed");
    }
}
```

---

## Refactoring Guidelines

### Do's ✅

1. **Write Tests First**: Ensure tests exist before refactoring
2. **Small Steps**: Make incremental changes
3. **Commit Often**: Commit after each successful refactoring
4. **Run Tests**: Run tests after every change
5. **Pair Program**: Refactor with a colleague for better results
6. **Document Decisions**: Explain why you refactored
7. **Measure Impact**: Track metrics before and after

### Don'ts ❌

1. **Don't Refactor Without Tests**: Always have test coverage
2. **Don't Mix with Features**: Separate refactoring from feature work
3. **Don't Refactor Everything**: Focus on high-impact areas
4. **Don't Break APIs**: Maintain backward compatibility
5. **Don't Ignore Performance**: Monitor performance impact
6. **Don't Skip Code Review**: Get feedback on refactorings
7. **Don't Refactor Under Pressure**: Avoid refactoring during crunch time

---

**Related Documents**:
- [Overview](overview.md) - Evolution perspective introduction
- [Extensibility](extensibility.md) - Extension points and mechanisms
- [Technology Evolution](technology-evolution.md) - Framework upgrades
- [API Versioning](api-versioning.md) - API compatibility
- [Development Standards](../../../.kiro/steering/development-standards.md) - Code quality standards
