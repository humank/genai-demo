# Refactoring Guide

## Overview

This document provides practical guidance on refactoring techniques, when to refactor, and how to refactor safely in our DDD + Hexagonal Architecture project.

**Related Standards**: [Design Principles](../../steering/design-principles.md), [Simple Design](simple-design-examples.md)

---

## What is Refactoring?

**Refactoring** is the process of improving the internal structure of code without changing its external behavior.

### Key Principles

- **Behavior Preservation**: External behavior must not change
- **Small Steps**: Make small, incremental changes
- **Test Coverage**: Always have tests before refactoring
- **Continuous**: Refactor as you go, not in big batches

---

## When to Refactor

### The Rule of Three

1. **First time**: Just do it
2. **Second time**: Notice the duplication, but proceed
3. **Third time**: Refactor

### Refactoring Triggers

#### Code Smells
- Long methods (> 20 lines)
- Large classes (> 200 lines)
- Long parameter lists (> 3 parameters)
- Duplicated code
- Feature envy
- Data clumps
- Primitive obsession

#### Before Adding Features
- Refactor to make the new feature easy to add
- "Make the change easy, then make the easy change"

#### During Code Review
- Address reviewer feedback
- Improve code clarity

#### When Tests Are Hard to Write
- If testing is difficult, the design needs improvement

---

## Common Refactoring Techniques

### 1. Extract Method

**When**: Method is too long or does multiple things

```java
// ❌ BEFORE: Long method doing multiple things
public void processOrder(Order order) {
    // Validate
    if (order.getItems() == null || order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Order must have items");
    }
    if (order.getCustomer() == null) {
        throw new BusinessRuleViolationException("Order must have customer");
    }
    
    // Calculate total
    BigDecimal total = BigDecimal.ZERO;
    for (OrderItem item : order.getItems()) {
        BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
        total = total.add(itemTotal);
    }
    
    // Apply discount
    if (order.getCustomer().isPremium() && total.compareTo(new BigDecimal("1000")) > 0) {
        total = total.multiply(new BigDecimal("0.9"));
    }
    
    order.setTotal(total);
    order.setStatus(OrderStatus.PROCESSING);
    
    // Reserve inventory
    for (OrderItem item : order.getItems()) {
        inventoryService.reserve(item.getProductId(), item.getQuantity());
    }
    
    // Send notification
    emailService.sendOrderConfirmation(order.getCustomer().getEmail(), order.getId());
}

// ✅ AFTER: Extracted methods with clear responsibilities
public void processOrder(Order order) {
    validateOrder(order);
    
    BigDecimal total = calculateTotal(order);
    BigDecimal finalTotal = applyDiscounts(order, total);
    
    order.setTotal(finalTotal);
    order.setStatus(OrderStatus.PROCESSING);
    
    reserveInventory(order);
    sendConfirmation(order);
}

private void validateOrder(Order order) {
    if (!order.hasItems()) {
        throw new BusinessRuleViolationException("Order must have items");
    }
    if (!order.hasCustomer()) {
        throw new BusinessRuleViolationException("Order must have customer");
    }
}

private BigDecimal calculateTotal(Order order) {
    return order.getItems().stream()
        .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}

private BigDecimal applyDiscounts(Order order, BigDecimal total) {
    if (order.getCustomer().isPremium() && total.compareTo(new BigDecimal("1000")) > 0) {
        return total.multiply(new BigDecimal("0.9"));
    }
    return total;
}

private void reserveInventory(Order order) {
    order.getItems().forEach(item -> 
        inventoryService.reserve(item.getProductId(), item.getQuantity())
    );
}

private void sendConfirmation(Order order) {
    emailService.sendOrderConfirmation(
        order.getCustomer().getEmail(), 
        order.getId()
    );
}
```

### 2. Introduce Parameter Object

**When**: Methods have long parameter lists or data clumps

```java
// ❌ BEFORE: Long parameter list
public Customer createCustomer(
    String name,
    String email,
    String phone,
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {
    // Implementation
}

// ✅ AFTER: Parameter object
public record CreateCustomerRequest(
    String name,
    String email,
    String phone,
    Address address
) {}

public record Address(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {}

public Customer createCustomer(CreateCustomerRequest request) {
    // Implementation
}
```

### 3. Replace Primitive with Value Object

**When**: Primitive types are used for domain concepts

```java
// ❌ BEFORE: Primitive obsession
public class Customer {
    private String email;
    
    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Invalid email");
        }
        this.email = email;
    }
}

public class Order {
    private String customerEmail;
    
    public void setCustomerEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Invalid email");
        }
        this.customerEmail = email;
    }
}

// ✅ AFTER: Value object
public record Email(String value) {
    public Email {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Email cannot be empty");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
    }
    
    public static Email of(String value) {
        return new Email(value);
    }
}

public class Customer {
    private Email email;
    
    public void setEmail(Email email) {
        this.email = email;
    }
}

public class Order {
    private Email customerEmail;
    
    public void setCustomerEmail(Email email) {
        this.customerEmail = email;
    }
}
```

### 4. Extract Class

**When**: A class is doing too much (violates Single Responsibility Principle)

```java
// ❌ BEFORE: God class
public class Order {
    private String id;
    private String customerId;
    private List<OrderItem> items;
    private BigDecimal total;
    private OrderStatus status;
    
    // Shipping information
    private String shippingStreet;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
    
    // Billing information
    private String billingStreet;
    private String billingCity;
    private String billingState;
    private String billingPostalCode;
    private String billingCountry;
    
    // Payment information
    private String paymentMethod;
    private String cardNumber;
    private String cardHolderName;
    private String expiryDate;
    
    // 50+ methods handling all these concerns
}

// ✅ AFTER: Extracted classes
public class Order {
    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private Money total;
    private OrderStatus status;
    private ShippingAddress shippingAddress;
    private BillingAddress billingAddress;
    private PaymentInfo paymentInfo;
    
    // Focused on order lifecycle
}

public record ShippingAddress(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {}

public record BillingAddress(
    String street,
    String city,
    String state,
    String postalCode,
    String country
) {}

public record PaymentInfo(
    PaymentMethod method,
    CardNumber cardNumber,
    CardHolderName holderName,
    ExpiryDate expiryDate
) {}
```

### 5. Replace Conditional with Polymorphism

**When**: Complex conditional logic based on type

```java
// ❌ BEFORE: Type-based conditionals
public class ShippingCalculator {
    public BigDecimal calculateShippingCost(Order order, String shippingType) {
        BigDecimal baseCost = order.getTotal();
        
        if ("STANDARD".equals(shippingType)) {
            return baseCost.multiply(new BigDecimal("0.05"));
        } else if ("EXPRESS".equals(shippingType)) {
            return baseCost.multiply(new BigDecimal("0.15"));
        } else if ("OVERNIGHT".equals(shippingType)) {
            return baseCost.multiply(new BigDecimal("0.25"));
        } else if ("INTERNATIONAL".equals(shippingType)) {
            BigDecimal cost = baseCost.multiply(new BigDecimal("0.20"));
            return cost.add(new BigDecimal("50")); // Customs fee
        }
        
        throw new IllegalArgumentException("Unknown shipping type: " + shippingType);
    }
}

// ✅ AFTER: Polymorphism
public interface ShippingStrategy {
    Money calculateCost(Order order);
}

public class StandardShipping implements ShippingStrategy {
    @Override
    public Money calculateCost(Order order) {
        return order.getTotal().multiply(Percentage.of(5));
    }
}

public class ExpressShipping implements ShippingStrategy {
    @Override
    public Money calculateCost(Order order) {
        return order.getTotal().multiply(Percentage.of(15));
    }
}

public class OvernightShipping implements ShippingStrategy {
    @Override
    public Money calculateCost(Order order) {
        return order.getTotal().multiply(Percentage.of(25));
    }
}

public class InternationalShipping implements ShippingStrategy {
    private static final Money CUSTOMS_FEE = Money.of(50);
    
    @Override
    public Money calculateCost(Order order) {
        Money shippingCost = order.getTotal().multiply(Percentage.of(20));
        return shippingCost.add(CUSTOMS_FEE);
    }
}

public class ShippingCalculator {
    private final Map<ShippingType, ShippingStrategy> strategies;
    
    public Money calculateShippingCost(Order order, ShippingType type) {
        ShippingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unknown shipping type: " + type);
        }
        return strategy.calculateCost(order);
    }
}
```

### 6. Introduce Null Object

**When**: Null checks are scattered throughout code

```java
// ❌ BEFORE: Null checks everywhere
public class OrderService {
    public void processOrder(Order order) {
        Customer customer = order.getCustomer();
        if (customer != null) {
            Discount discount = customer.getDiscount();
            if (discount != null) {
                BigDecimal discountAmount = discount.calculate(order.getTotal());
                if (discountAmount != null) {
                    order.applyDiscount(discountAmount);
                }
            }
        }
    }
}

// ✅ AFTER: Null object pattern
public interface Discount {
    Money calculate(Money total);
}

public class PercentageDiscount implements Discount {
    private final Percentage percentage;
    
    @Override
    public Money calculate(Money total) {
        return total.multiply(percentage);
    }
}

public class NoDiscount implements Discount {
    @Override
    public Money calculate(Money total) {
        return Money.zero();
    }
}

public class Customer {
    private Discount discount = new NoDiscount(); // Default
    
    public Discount getDiscount() {
        return discount; // Never null
    }
}

public class OrderService {
    public void processOrder(Order order) {
        Money discountAmount = order.getCustomer()
            .getDiscount()
            .calculate(order.getTotal());
        order.applyDiscount(discountAmount);
    }
}
```

---

## Refactoring Workflow

### Step-by-Step Process

1. **Ensure Tests Exist**
   ```bash
   ./gradlew test
   # All tests must pass before refactoring
   ```

2. **Make Small Change**
   - Change one thing at a time
   - Keep changes focused

3. **Run Tests**
   ```bash
   ./gradlew test
   # Tests must still pass
   ```

4. **Commit**
   ```bash
   git add .
   git commit -m "refactor: extract calculateTotal method"
   ```

5. **Repeat**
   - Continue with next small change

### Red-Green-Refactor Cycle

```
┌─────────────┐
│   RED       │  Write failing test
│   (Test)    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   GREEN     │  Make test pass (minimal code)
│   (Code)    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  REFACTOR   │  Improve code structure
│  (Clean)    │
└──────┬──────┘
       │
       └──────► Repeat
```

---

## Refactoring Patterns for DDD

### Pattern 1: Extract Aggregate Root

```java
// ❌ BEFORE: Anemic domain model
public class Order {
    private String id;
    private List<OrderItem> items;
    private BigDecimal total;
    
    // Only getters and setters
}

public class OrderService {
    public void submitOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        
        // Business logic in service
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        order.setTotal(total);
        order.setStatus("SUBMITTED");
        
        orderRepository.save(order);
    }
}

// ✅ AFTER: Rich domain model
@AggregateRoot
public class Order extends AggregateRoot {
    private final OrderId id;
    private final List<OrderItem> items;
    private Money total;
    private OrderStatus status;
    
    public void submit() {
        validateCanSubmit();
        
        this.total = calculateTotal();
        this.status = OrderStatus.SUBMITTED;
        
        collectEvent(OrderSubmittedEvent.create(id, total));
    }
    
    private void validateCanSubmit() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException("Order already submitted");
        }
    }
    
    private Money calculateTotal() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.zero(), Money::add);
    }
}

public class OrderApplicationService {
    public void submitOrder(String orderId) {
        Order order = orderRepository.findById(OrderId.of(orderId))
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        order.submit(); // Business logic in aggregate
        
        orderRepository.save(order);
        eventService.publishEventsFromAggregate(order);
    }
}
```

### Pattern 2: Extract Value Object

```java
// ❌ BEFORE: Primitive obsession
public class Product {
    private BigDecimal price;
    private String currency;
    
    public void setPrice(BigDecimal price, String currency) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Price cannot be negative");
        }
        this.price = price;
        this.currency = currency;
    }
    
    public BigDecimal calculateTax() {
        return price.multiply(new BigDecimal("0.1"));
    }
}

// ✅ AFTER: Value object
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
    }
    
    public Money calculateTax(Percentage taxRate) {
        return new Money(amount.multiply(taxRate.value()), currency);
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

public class Product {
    private Money price;
    
    public void setPrice(Money price) {
        this.price = price;
    }
    
    public Money calculateTax() {
        return price.calculateTax(Percentage.of(10));
    }
}
```

---

## Refactoring Safety Net

### Before Refactoring Checklist

- [ ] All tests pass
- [ ] Test coverage > 80%
- [ ] No pending changes
- [ ] Code is committed to version control

### During Refactoring

- [ ] Make one change at a time
- [ ] Run tests after each change
- [ ] Commit frequently
- [ ] Keep changes small and focused

### After Refactoring

- [ ] All tests still pass
- [ ] Code is more readable
- [ ] Duplication is reduced
- [ ] Design is simpler

---

## Common Refactoring Mistakes

### Mistake 1: Refactoring Without Tests

```java
// ❌ DANGER: No tests to verify behavior
public void refactorWithoutTests() {
    // Making changes without safety net
    // If something breaks, you won't know
}

// ✅ SAFE: Tests verify behavior
@Test
void should_calculate_total_correctly() {
    // Test exists before refactoring
}

public void refactorWithTests() {
    // Changes are safe because tests verify behavior
}
```

### Mistake 2: Big Bang Refactoring

```java
// ❌ BAD: Rewriting everything at once
// - High risk
// - Hard to review
// - Difficult to debug if something breaks

// ✅ GOOD: Incremental refactoring
// Step 1: Extract method
// Step 2: Introduce parameter object
// Step 3: Extract class
// Each step is small, tested, and committed
```

### Mistake 3: Changing Behavior While Refactoring

```java
// ❌ BAD: Mixing refactoring with feature changes
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);
    
    // Adding new feature during refactoring
    if (order.getCustomer().isVIP()) {
        applyVIPDiscount(order); // NEW FEATURE
    }
    
    saveOrder(order);
}

// ✅ GOOD: Separate refactoring from feature changes
// 1. First refactor (no behavior change)
// 2. Then add feature (with new tests)
```

---

## Refactoring Tools

### IDE Refactoring Support

IntelliJ IDEA provides automated refactoring:

- **Extract Method**: `Ctrl+Alt+M` (Windows/Linux) or `Cmd+Alt+M` (Mac)
- **Extract Variable**: `Ctrl+Alt+V` or `Cmd+Alt+V`
- **Rename**: `Shift+F6`
- **Inline**: `Ctrl+Alt+N` or `Cmd+Alt+N`
- **Change Signature**: `Ctrl+F6` or `Cmd+F6`

### Automated Refactoring Scripts

```bash
# Run tests before refactoring
./gradlew test

# Check code quality
./gradlew pmdMain checkstyleMain

# Verify architecture rules
./gradlew archUnit
```

---

## Summary

Refactoring is essential for maintaining code quality:

1. **Always have tests** before refactoring
2. **Make small changes** and test frequently
3. **Commit often** to create safe checkpoints
4. **Focus on one thing** at a time
5. **Don't change behavior** while refactoring

Remember: **Refactoring is not rewriting**. It's improving structure while preserving behavior.

---

**Related Documentation**:
- [Simple Design Examples](simple-design-examples.md)
- [Design Principles](../../steering/design-principles.md)
- [Code Quality Checklist](../../steering/code-quality-checklist.md)
