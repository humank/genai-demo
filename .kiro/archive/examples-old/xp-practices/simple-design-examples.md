# Simple Design Examples

## Overview

This document provides comprehensive examples of Kent Beck's Four Rules of Simple Design, demonstrating how to write code that is simple, clear, and maintainable.

**Related Standards**: [Design Principles](../../steering/design-principles.md)

---

## Four Rules of Simple Design (Priority Order)

1. **Passes all tests** - Code must work correctly
2. **Reveals intention** - Code communicates clearly
3. **No duplication** - DRY principle
4. **Fewest elements** - Minimal classes and methods

---

## Rule 1: Passes All Tests

### Principle

Code must be correct before it can be simple. Tests provide confidence that the code works as intended.

### Example: Test-Driven Development

```java
// ❌ BAD: Writing code without tests
public class OrderCalculator {
    public BigDecimal calculateTotal(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        if (order.getCustomer().isPremium()) {
            total = total.multiply(new BigDecimal("0.9")); // 10% discount
        }
        return total;
    }
}

// ✅ GOOD: Test-first approach
@Test
void should_calculate_total_for_regular_customer() {
    // Given
    Order order = anOrder()
        .withItem(aProduct().withPrice(100.00), quantity(2))
        .withItem(aProduct().withPrice(50.00), quantity(1))
        .withCustomer(aRegularCustomer())
        .build();
    
    // When
    BigDecimal total = calculator.calculateTotal(order);
    
    // Then
    assertThat(total).isEqualByComparingTo("250.00");
}

@Test
void should_apply_premium_discount() {
    // Given
    Order order = anOrder()
        .withItem(aProduct().withPrice(100.00), quantity(1))
        .withCustomer(aPremiumCustomer())
        .build();
    
    // When
    BigDecimal total = calculator.calculateTotal(order);
    
    // Then
    assertThat(total).isEqualByComparingTo("90.00"); // 10% discount applied
}

// Implementation driven by tests
public class OrderCalculator {
    public BigDecimal calculateTotal(Order order) {
        BigDecimal subtotal = calculateSubtotal(order);
        return applyCustomerDiscount(subtotal, order.getCustomer());
    }
    
    private BigDecimal calculateSubtotal(Order order) {
        return order.getItems().stream()
            .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal applyCustomerDiscount(BigDecimal amount, Customer customer) {
        if (customer.isPremium()) {
            return amount.multiply(new BigDecimal("0.9"));
        }
        return amount;
    }
}
```

### Key Practices

- Write tests first (Red-Green-Refactor)
- Test behavior, not implementation
- Keep tests simple and focused
- Use descriptive test names

---

## Rule 2: Reveals Intention

### Principle

Code should clearly communicate its purpose. A reader should understand what the code does without having to figure out how it works.

### Example: Meaningful Names

```java
// ❌ BAD: Unclear names
public class OrderProc {
    public void proc(Order o) {
        if (o.getS() == 1) {
            List<Item> items = o.getI();
            for (Item i : items) {
                if (i.getQ() > 0) {
                    inv.res(i.getPid(), i.getQ());
                }
            }
            o.setS(2);
        }
    }
}

// ✅ GOOD: Clear, intention-revealing names
public class OrderProcessor {
    private final InventoryService inventoryService;
    
    public void processOrder(Order order) {
        if (order.isPending()) {
            reserveInventoryForOrder(order);
            order.markAsProcessing();
        }
    }
    
    private void reserveInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.hasValidQuantity()) {
                inventoryService.reserve(item.getProductId(), item.getQuantity());
            }
        }
    }
}
```

### Example: Extract Method for Clarity

```java
// ❌ BAD: Complex logic buried in method
public void submitOrder(Order order) {
    if (order.getItems() != null && !order.getItems().isEmpty() && 
        order.getCustomer() != null && order.getCustomer().getEmail() != null &&
        order.getShippingAddress() != null && order.getShippingAddress().getPostalCode() != null) {
        
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            total = total.add(item.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        
        if (total.compareTo(new BigDecimal("1000")) > 0) {
            total = total.multiply(new BigDecimal("0.95"));
        }
        
        order.setTotal(total);
        order.setStatus(OrderStatus.SUBMITTED);
        orderRepository.save(order);
    }
}

// ✅ GOOD: Extracted methods reveal intention
public void submitOrder(Order order) {
    validateOrderForSubmission(order);
    
    BigDecimal total = calculateOrderTotal(order);
    BigDecimal finalTotal = applyBulkDiscount(total);
    
    order.setTotal(finalTotal);
    order.markAsSubmitted();
    
    orderRepository.save(order);
}

private void validateOrderForSubmission(Order order) {
    if (!order.hasItems()) {
        throw new BusinessRuleViolationException("Order must have at least one item");
    }
    if (!order.hasValidCustomer()) {
        throw new BusinessRuleViolationException("Order must have valid customer information");
    }
    if (!order.hasValidShippingAddress()) {
        throw new BusinessRuleViolationException("Order must have valid shipping address");
    }
}

private BigDecimal calculateOrderTotal(Order order) {
    return order.getItems().stream()
        .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
}

private BigDecimal applyBulkDiscount(BigDecimal total) {
    BigDecimal bulkOrderThreshold = new BigDecimal("1000");
    BigDecimal bulkDiscountRate = new BigDecimal("0.95");
    
    if (total.compareTo(bulkOrderThreshold) > 0) {
        return total.multiply(bulkDiscountRate);
    }
    return total;
}
```

### Key Practices

- Use descriptive variable and method names
- Extract methods to reveal intent
- Use domain language in code
- Avoid abbreviations and cryptic names
- Make boolean expressions readable

---

## Rule 3: No Duplication (DRY)

### Principle

Duplication is the root of all evil in software. Every piece of knowledge should have a single, unambiguous representation.

### Example: Extract Common Logic

```java
// ❌ BAD: Duplicated validation logic
public class CustomerService {
    public void createCustomer(CreateCustomerCommand command) {
        if (command.getEmail() == null || command.getEmail().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (!command.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
        
        Customer customer = new Customer(command.getName(), command.getEmail());
        customerRepository.save(customer);
    }
    
    public void updateCustomer(UpdateCustomerCommand command) {
        if (command.getEmail() == null || command.getEmail().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (!command.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
        
        Customer customer = customerRepository.findById(command.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.getCustomerId()));
        customer.updateEmail(command.getEmail());
        customerRepository.save(customer);
    }
}

// ✅ GOOD: Extracted validation to value object
public record Email(String value) {
    public Email {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (!value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format");
        }
    }
    
    public static Email of(String value) {
        return new Email(value);
    }
}

public class CustomerService {
    public void createCustomer(CreateCustomerCommand command) {
        Email email = Email.of(command.getEmail());
        Customer customer = new Customer(command.getName(), email);
        customerRepository.save(customer);
    }
    
    public void updateCustomer(UpdateCustomerCommand command) {
        Email email = Email.of(command.getEmail());
        Customer customer = customerRepository.findById(command.getCustomerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.getCustomerId()));
        customer.updateEmail(email);
        customerRepository.save(customer);
    }
}
```

### Example: Template Method Pattern

```java
// ❌ BAD: Duplicated processing logic
public class OrderProcessor {
    public void processStandardOrder(Order order) {
        validateOrder(order);
        calculateTotal(order);
        reserveInventory(order);
        processPayment(order);
        sendConfirmation(order);
    }
    
    public void processExpressOrder(Order order) {
        validateOrder(order);
        calculateTotal(order);
        reserveInventory(order);
        processPayment(order);
        scheduleExpressShipping(order); // Different
        sendConfirmation(order);
    }
    
    public void processInternationalOrder(Order order) {
        validateOrder(order);
        calculateTotal(order);
        calculateCustomsDuty(order); // Different
        reserveInventory(order);
        processPayment(order);
        scheduleInternationalShipping(order); // Different
        sendConfirmation(order);
    }
}

// ✅ GOOD: Template method eliminates duplication
public abstract class OrderProcessor {
    
    public final void processOrder(Order order) {
        validateOrder(order);
        calculateTotal(order);
        performAdditionalCalculations(order);
        reserveInventory(order);
        processPayment(order);
        scheduleShipping(order);
        sendConfirmation(order);
    }
    
    // Common methods
    private void validateOrder(Order order) { /* ... */ }
    private void calculateTotal(Order order) { /* ... */ }
    private void reserveInventory(Order order) { /* ... */ }
    private void processPayment(Order order) { /* ... */ }
    private void sendConfirmation(Order order) { /* ... */ }
    
    // Hook methods for customization
    protected void performAdditionalCalculations(Order order) {
        // Default: do nothing
    }
    
    protected abstract void scheduleShipping(Order order);
}

public class StandardOrderProcessor extends OrderProcessor {
    @Override
    protected void scheduleShipping(Order order) {
        shippingService.scheduleStandardShipping(order);
    }
}

public class ExpressOrderProcessor extends OrderProcessor {
    @Override
    protected void scheduleShipping(Order order) {
        shippingService.scheduleExpressShipping(order);
    }
}

public class InternationalOrderProcessor extends OrderProcessor {
    @Override
    protected void performAdditionalCalculations(Order order) {
        customsService.calculateDuty(order);
    }
    
    @Override
    protected void scheduleShipping(Order order) {
        shippingService.scheduleInternationalShipping(order);
    }
}
```

### Key Practices

- Extract common logic to shared methods
- Use value objects for validation
- Apply design patterns to eliminate duplication
- Don't repeat yourself (DRY)
- Look for similar code patterns

---

## Rule 4: Fewest Elements

### Principle

Minimize the number of classes and methods. Every class and method should earn its keep. Remove unnecessary abstractions.

### Example: Avoid Premature Abstraction

```java
// ❌ BAD: Over-engineered with unnecessary abstractions
public interface CustomerFactory {
    Customer create(CustomerData data);
}

public class StandardCustomerFactory implements CustomerFactory {
    @Override
    public Customer create(CustomerData data) {
        return new Customer(data.getName(), data.getEmail());
    }
}

public class PremiumCustomerFactory implements CustomerFactory {
    @Override
    public Customer create(CustomerData data) {
        Customer customer = new Customer(data.getName(), data.getEmail());
        customer.setPremium(true);
        return customer;
    }
}

public interface CustomerValidator {
    boolean validate(Customer customer);
}

public class EmailValidator implements CustomerValidator {
    @Override
    public boolean validate(Customer customer) {
        return customer.getEmail() != null && customer.getEmail().contains("@");
    }
}

public class NameValidator implements CustomerValidator {
    @Override
    public boolean validate(Customer customer) {
        return customer.getName() != null && !customer.getName().isEmpty();
    }
}

public class CustomerService {
    private final CustomerFactory factory;
    private final List<CustomerValidator> validators;
    
    public Customer createCustomer(CustomerData data, boolean isPremium) {
        CustomerFactory factory = isPremium ? 
            new PremiumCustomerFactory() : new StandardCustomerFactory();
        Customer customer = factory.create(data);
        
        for (CustomerValidator validator : validators) {
            if (!validator.validate(customer)) {
                throw new ValidationException("Invalid customer");
            }
        }
        
        return customer;
    }
}

// ✅ GOOD: Simple, direct implementation
public class CustomerService {
    
    public Customer createCustomer(String name, String email, boolean isPremium) {
        validateCustomerData(name, email);
        
        Customer customer = new Customer(name, email);
        if (isPremium) {
            customer.setPremium(true);
        }
        
        return customer;
    }
    
    private void validateCustomerData(String name, String email) {
        if (name == null || name.isEmpty()) {
            throw new ValidationException("Name is required");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Valid email is required");
        }
    }
}
```

### Example: Remove Unnecessary Classes

```java
// ❌ BAD: Unnecessary wrapper classes
public class OrderId {
    private final String value;
    public OrderId(String value) { this.value = value; }
    public String getValue() { return value; }
}

public class OrderStatus {
    private final String value;
    public OrderStatus(String value) { this.value = value; }
    public String getValue() { return value; }
}

public class OrderTotal {
    private final BigDecimal value;
    public OrderTotal(BigDecimal value) { this.value = value; }
    public BigDecimal getValue() { return value; }
}

public class Order {
    private OrderId id;
    private OrderStatus status;
    private OrderTotal total;
    
    public void setId(OrderId id) { this.id = id; }
    public OrderId getId() { return id; }
    // More getters/setters...
}

// ✅ GOOD: Use value objects only when they add value
public record OrderId(String value) {
    public OrderId {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be empty");
        }
    }
    
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }
}

public enum OrderStatus {
    PENDING, PROCESSING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
}

public class Order {
    private final OrderId id;
    private OrderStatus status;
    private Money total;
    
    // Value objects provide behavior, not just data
}
```

### Key Practices

- Start simple, add complexity only when needed
- Remove classes that don't add value
- Avoid speculative generality
- YAGNI (You Aren't Gonna Need It)
- Refactor to patterns only when duplication emerges

---

## Applying All Four Rules Together

### Example: Refactoring to Simple Design

```java
// ❌ BEFORE: Complex, unclear, duplicated code
public class OrderManager {
    public void handleOrder(Map<String, Object> orderData) {
        // Rule 1 violated: No tests
        // Rule 2 violated: Unclear intention
        // Rule 3 violated: Duplication
        // Rule 4 violated: Too many responsibilities
        
        String custId = (String) orderData.get("customerId");
        List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
        
        if (custId == null || custId.isEmpty()) {
            throw new RuntimeException("Invalid customer");
        }
        
        double total = 0;
        for (Map<String, Object> item : items) {
            double price = (Double) item.get("price");
            int qty = (Integer) item.get("quantity");
            total += price * qty;
        }
        
        if (total > 1000) {
            total = total * 0.9;
        }
        
        // Save to database
        // Send email
        // Update inventory
    }
}

// ✅ AFTER: Simple, clear, tested code
@Test
void should_process_order_with_bulk_discount() {
    // Rule 1: Tests first
    Order order = anOrder()
        .withCustomer(aCustomer().withId("CUST-001"))
        .withItem(aProduct().withPrice(600.00), quantity(2))
        .build();
    
    orderService.processOrder(order);
    
    assertThat(order.getTotal()).isEqualByComparingTo("1080.00"); // 10% discount applied
    verify(inventoryService).reserveItems(order.getItems());
    verify(emailService).sendOrderConfirmation(order);
}

// Rule 2: Clear intention
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final EmailService emailService;
    
    public void processOrder(Order order) {
        validateOrder(order);
        
        Money total = calculateTotal(order);
        Money finalTotal = applyDiscounts(total);
        
        order.setTotal(finalTotal);
        order.markAsProcessing();
        
        reserveInventory(order);
        saveOrder(order);
        sendConfirmation(order);
    }
    
    // Rule 3: No duplication - extracted to value objects and services
    private void validateOrder(Order order) {
        if (!order.hasValidCustomer()) {
            throw new BusinessRuleViolationException("Order must have valid customer");
        }
        if (!order.hasItems()) {
            throw new BusinessRuleViolationException("Order must have items");
        }
    }
    
    private Money calculateTotal(Order order) {
        return order.getItems().stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.zero(), Money::add);
    }
    
    private Money applyDiscounts(Money total) {
        if (total.isGreaterThan(Money.of(1000))) {
            return total.applyDiscount(Percentage.of(10));
        }
        return total;
    }
    
    // Rule 4: Fewest elements - delegate to specialized services
    private void reserveInventory(Order order) {
        inventoryService.reserveItems(order.getItems());
    }
    
    private void saveOrder(Order order) {
        orderRepository.save(order);
    }
    
    private void sendConfirmation(Order order) {
        emailService.sendOrderConfirmation(order);
    }
}
```

---

## Common Pitfalls

### Pitfall 1: Premature Optimization

```java
// ❌ BAD: Optimizing before it's needed
public class CustomerCache {
    private final Map<String, SoftReference<Customer>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    
    public CustomerCache() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.HOURS);
    }
    
    public Customer get(String id) {
        SoftReference<Customer> ref = cache.get(id);
        if (ref != null) {
            Customer customer = ref.get();
            if (customer != null) {
                return customer;
            }
        }
        Customer customer = loadFromDatabase(id);
        cache.put(id, new SoftReference<>(customer));
        return customer;
    }
    
    private void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
}

// ✅ GOOD: Start simple, optimize when needed
@Cacheable("customers")
public Customer getCustomer(String id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new CustomerNotFoundException(id));
}
```

### Pitfall 2: Over-Engineering

```java
// ❌ BAD: Too many layers of abstraction
public interface DataAccessStrategy {
    <T> T execute(DataAccessCallback<T> callback);
}

public interface DataAccessCallback<T> {
    T doInDataAccess(DataAccessContext context);
}

public class TransactionalDataAccessStrategy implements DataAccessStrategy {
    @Override
    public <T> T execute(DataAccessCallback<T> callback) {
        // Complex transaction management
    }
}

// ✅ GOOD: Use framework features
@Transactional
public Customer createCustomer(CreateCustomerCommand command) {
    Customer customer = new Customer(command.getName(), command.getEmail());
    return customerRepository.save(customer);
}
```

---

## Refactoring Checklist

When refactoring towards simple design, ask:

### Rule 1: Passes All Tests
- [ ] Do I have tests for this code?
- [ ] Do all tests pass?
- [ ] Are tests clear and maintainable?

### Rule 2: Reveals Intention
- [ ] Can I understand what this code does without reading implementation?
- [ ] Are names descriptive and meaningful?
- [ ] Is the code at a consistent level of abstraction?
- [ ] Would a new team member understand this?

### Rule 3: No Duplication
- [ ] Is there any duplicated logic?
- [ ] Can I extract common behavior?
- [ ] Are there similar code patterns?
- [ ] Can I use value objects or design patterns?

### Rule 4: Fewest Elements
- [ ] Is every class necessary?
- [ ] Is every method necessary?
- [ ] Am I adding complexity for future needs (YAGNI)?
- [ ] Can I simplify without losing clarity?

---

## Summary

Simple design is achieved by:

1. **Writing tests first** - Ensures correctness
2. **Revealing intention** - Makes code understandable
3. **Eliminating duplication** - Reduces maintenance burden
4. **Minimizing elements** - Keeps design lean

Remember: **Simple ≠ Easy**. Simple design requires discipline and continuous refactoring.

---

**Related Documentation**:
- [Design Principles](../../steering/design-principles.md)
- [Refactoring Guide](refactoring-guide.md)
- [Code Quality Checklist](../../steering/code-quality-checklist.md)
