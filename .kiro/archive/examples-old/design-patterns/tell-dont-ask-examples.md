# Tell, Don't Ask - Detailed Examples

## Principle Overview

**Tell, Don't Ask** means that objects should tell other objects what to do, rather than asking for their state and making decisions based on that state. This principle helps maintain encapsulation and keeps behavior with the data it operates on.

## Core Concept

- **Ask**: Getting data from an object and making decisions
- **Tell**: Instructing an object to perform an operation

---

## Example 1: Order Status Management

### ❌ Bad: Asking for State

```java
// Controller asks for state and makes decisions
@RestController
public class OrderController {
    
    @PostMapping("/orders/{id}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();
        
        // Asking for state and making decisions
        if (order.getStatus() == OrderStatus.CREATED) {
            if (order.getItems().isEmpty()) {
                throw new BusinessRuleViolationException("Cannot submit empty order");
            }
            
            order.setStatus(OrderStatus.PENDING);
            order.setSubmittedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            
            orderRepository.save(order);
            
            // More external logic
            emailService.sendOrderConfirmation(order.getCustomerId());
            
            return ResponseEntity.ok(OrderResponse.from(order));
        } else {
            throw new InvalidOrderStateException("Order cannot be submitted");
        }
    }
}
```

**Problems:**
- Controller knows too much about Order's internal state
- Business logic is scattered outside the domain model
- Hard to test business rules
- Violates encapsulation

### ✅ Good: Telling What to Do

```java
// Controller tells the order what to do
@RestController
public class OrderController {
    
    @PostMapping("/orders/{id}/submit")
    public ResponseEntity<OrderResponse> submitOrder(@PathVariable String id) {
        Order order = orderRepository.findById(id).orElseThrow();
        
        // Tell the order to submit itself
        order.submit();
        
        orderRepository.save(order);
        
        // Publish events collected by the order
        domainEventService.publishEventsFromAggregate(order);
        
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}

// Order handles its own state transitions
@AggregateRoot
public class Order {
    private OrderId id;
    private OrderStatus status;
    private List<OrderItem> items;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
    
    public void submit() {
        // Business rules are encapsulated
        validateCanBeSubmitted();
        
        // State changes are internal
        this.status = OrderStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Collect domain event
        collectEvent(OrderSubmittedEvent.create(id, customerId, calculateTotal()));
    }
    
    private void validateCanBeSubmitted() {
        if (status != OrderStatus.CREATED) {
            throw new InvalidOrderStateException(
                "Order must be in CREATED status to be submitted. Current status: " + status
            );
        }
        
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
    }
}
```

**Benefits:**
- Business logic is in the domain model
- Order controls its own state transitions
- Easy to test business rules
- Maintains encapsulation

---

## Example 2: Customer Discount Calculation

### ❌ Bad: Asking for State

```java
@Service
public class OrderService {
    
    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = Money.ZERO;
        
        // Asking for items and calculating externally
        for (OrderItem item : order.getItems()) {
            Money itemPrice = item.getProduct().getPrice();
            int quantity = item.getQuantity();
            subtotal = subtotal.add(itemPrice.multiply(quantity));
        }
        
        // Asking for customer type and calculating discount externally
        if (customer.getMembershipLevel() == MembershipLevel.PREMIUM) {
            Money discount = subtotal.multiply(0.10); // 10% discount
            return subtotal.subtract(discount);
        } else if (customer.getMembershipLevel() == MembershipLevel.GOLD) {
            Money discount = subtotal.multiply(0.05); // 5% discount
            return subtotal.subtract(discount);
        }
        
        return subtotal;
    }
}
```

**Problems:**
- Service knows about discount calculation logic
- Customer's behavior is implemented outside Customer
- Hard to add new membership levels
- Discount logic is duplicated

### ✅ Good: Telling What to Do

```java
@Service
public class OrderService {
    
    public Money calculateOrderTotal(Order order, Customer customer) {
        // Tell order to calculate its subtotal
        Money subtotal = order.calculateSubtotal();
        
        // Tell customer to apply its discount
        return customer.applyDiscount(subtotal);
    }
}

// Order knows how to calculate its own subtotal
@AggregateRoot
public class Order {
    private List<OrderItem> items;
    
    public Money calculateSubtotal() {
        return items.stream()
            .map(OrderItem::calculateSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}

// OrderItem knows how to calculate its own subtotal
public class OrderItem {
    private Product product;
    private int quantity;
    
    public Money calculateSubtotal() {
        return product.getPrice().multiply(quantity);
    }
}

// Customer knows how to apply its own discount
@AggregateRoot
public class Customer {
    private MembershipLevel membershipLevel;
    
    public Money applyDiscount(Money amount) {
        return membershipLevel.applyDiscount(amount);
    }
}

// MembershipLevel encapsulates discount logic
public enum MembershipLevel {
    STANDARD(0.0),
    PREMIUM(0.10),
    GOLD(0.05);
    
    private final double discountRate;
    
    MembershipLevel(double discountRate) {
        this.discountRate = discountRate;
    }
    
    public Money applyDiscount(Money amount) {
        if (discountRate == 0.0) {
            return amount;
        }
        Money discount = amount.multiply(discountRate);
        return amount.subtract(discount);
    }
}
```

**Benefits:**
- Each object handles its own calculations
- Easy to add new membership levels
- Discount logic is centralized
- Follows Single Responsibility Principle

---

## Example 3: Inventory Management

### ❌ Bad: Asking for State

```java
@Service
public class OrderProcessingService {
    
    public void processOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int requestedQuantity = item.getQuantity();
            
            // Asking for inventory state
            Inventory inventory = inventoryRepository.findByProductId(product.getId());
            int availableQuantity = inventory.getAvailableQuantity();
            
            // Making decisions based on state
            if (availableQuantity >= requestedQuantity) {
                inventory.setAvailableQuantity(availableQuantity - requestedQuantity);
                inventory.setReservedQuantity(inventory.getReservedQuantity() + requestedQuantity);
                inventoryRepository.save(inventory);
            } else {
                throw new InsufficientInventoryException(
                    "Not enough inventory for product: " + product.getName()
                );
            }
        }
    }
}
```

**Problems:**
- Service manipulates inventory state directly
- Inventory business rules are outside Inventory
- No encapsulation of inventory logic

### ✅ Good: Telling What to Do

```java
@Service
public class OrderProcessingService {
    
    public void processOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            int requestedQuantity = item.getQuantity();
            
            // Tell inventory to reserve items
            Inventory inventory = inventoryRepository.findByProductId(product.getId());
            inventory.reserve(requestedQuantity);
            
            inventoryRepository.save(inventory);
        }
    }
}

// Inventory handles its own state
@AggregateRoot
public class Inventory {
    private ProductId productId;
    private int availableQuantity;
    private int reservedQuantity;
    
    public void reserve(int quantity) {
        validateCanReserve(quantity);
        
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        
        collectEvent(InventoryReservedEvent.create(productId, quantity));
    }
    
    private void validateCanReserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (availableQuantity < quantity) {
            throw new InsufficientInventoryException(
                String.format("Cannot reserve %d items. Only %d available", 
                    quantity, availableQuantity)
            );
        }
    }
    
    public void release(int quantity) {
        validateCanRelease(quantity);
        
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        
        collectEvent(InventoryReleasedEvent.create(productId, quantity));
    }
    
    private void validateCanRelease(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        if (reservedQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Cannot release %d items. Only %d reserved", 
                    quantity, reservedQuantity)
            );
        }
    }
}
```

**Benefits:**
- Inventory controls its own state
- Business rules are encapsulated
- Easy to add new inventory operations
- Clear validation logic

---

## Example 4: Payment Processing

### ❌ Bad: Asking for State

```java
@Service
public class PaymentService {
    
    public void processPayment(Order order, PaymentMethod paymentMethod) {
        Money amount = order.getTotalAmount();
        
        // Asking for payment method details
        if (paymentMethod.getType() == PaymentMethodType.CREDIT_CARD) {
            CreditCard card = paymentMethod.getCreditCard();
            
            // External validation
            if (card.getExpiryDate().isBefore(LocalDate.now())) {
                throw new ExpiredCardException("Credit card has expired");
            }
            
            if (card.getAvailableCredit().isLessThan(amount)) {
                throw new InsufficientFundsException("Insufficient credit");
            }
            
            // External processing
            card.setAvailableCredit(card.getAvailableCredit().subtract(amount));
            paymentMethodRepository.save(paymentMethod);
            
        } else if (paymentMethod.getType() == PaymentMethodType.BANK_ACCOUNT) {
            BankAccount account = paymentMethod.getBankAccount();
            
            if (account.getBalance().isLessThan(amount)) {
                throw new InsufficientFundsException("Insufficient balance");
            }
            
            account.setBalance(account.getBalance().subtract(amount));
            paymentMethodRepository.save(paymentMethod);
        }
    }
}
```

**Problems:**
- Service knows about payment method internals
- Validation logic is external
- Hard to add new payment methods
- Violates Open/Closed Principle

### ✅ Good: Telling What to Do

```java
@Service
public class PaymentService {
    
    public void processPayment(Order order, PaymentMethod paymentMethod) {
        Money amount = order.getTotalAmount();
        
        // Tell payment method to process the payment
        paymentMethod.processPayment(amount);
        
        paymentMethodRepository.save(paymentMethod);
        
        // Tell order that payment was processed
        order.markAsPaid();
        orderRepository.save(order);
    }
}

// PaymentMethod is an interface
public interface PaymentMethod {
    void processPayment(Money amount);
    PaymentMethodType getType();
}

// CreditCard implements its own logic
public class CreditCard implements PaymentMethod {
    private String cardNumber;
    private LocalDate expiryDate;
    private Money availableCredit;
    
    @Override
    public void processPayment(Money amount) {
        validatePayment(amount);
        
        this.availableCredit = availableCredit.subtract(amount);
    }
    
    private void validatePayment(Money amount) {
        if (expiryDate.isBefore(LocalDate.now())) {
            throw new ExpiredCardException("Credit card has expired");
        }
        
        if (availableCredit.isLessThan(amount)) {
            throw new InsufficientFundsException(
                String.format("Insufficient credit. Available: %s, Required: %s",
                    availableCredit, amount)
            );
        }
    }
    
    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.CREDIT_CARD;
    }
}

// BankAccount implements its own logic
public class BankAccount implements PaymentMethod {
    private String accountNumber;
    private Money balance;
    
    @Override
    public void processPayment(Money amount) {
        validatePayment(amount);
        
        this.balance = balance.subtract(amount);
    }
    
    private void validatePayment(Money amount) {
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(
                String.format("Insufficient balance. Available: %s, Required: %s",
                    balance, amount)
            );
        }
    }
    
    @Override
    public PaymentMethodType getType() {
        return PaymentMethodType.BANK_ACCOUNT;
    }
}
```

**Benefits:**
- Each payment method handles its own logic
- Easy to add new payment methods
- Follows Strategy Pattern
- Polymorphic behavior

---

## Quick Reference

### When to Use Tell, Don't Ask

✅ **Use when:**
- An object has the data needed to make a decision
- Behavior naturally belongs with the data
- You want to maintain encapsulation
- You need to follow Single Responsibility Principle

❌ **Don't use when:**
- You need to coordinate multiple objects
- You're implementing a pure algorithm
- You're in an application service orchestrating use cases

### Common Violations

1. **Getter chains**: `order.getCustomer().getAddress().getCity()`
2. **If-else on type**: `if (customer.getType() == CustomerType.PREMIUM)`
3. **External calculations**: Calculating totals outside the aggregate
4. **State manipulation**: Setting multiple fields from outside

### Refactoring Steps

1. Identify where decisions are made based on object state
2. Move the decision logic into the object that owns the data
3. Replace getters with behavior methods
4. Test that business rules are now encapsulated

---

## Related Principles

- **Law of Demeter**: Only talk to immediate friends
- **Single Responsibility**: Each class has one reason to change
- **Encapsulation**: Hide internal state and expose behavior

## Further Reading

- [Law of Demeter Examples](law-of-demeter-examples.md)
- [Design Principles](../../steering/design-principles.md)
