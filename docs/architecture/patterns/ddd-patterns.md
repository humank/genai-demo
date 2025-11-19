# DDD Patterns Implementation

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document provides implementation guidance for Domain-Driven Design (DDD) tactical patterns in the GenAI Demo project.

---

## Quick Reference

For complete DDD guidance, see:
- [DDD Tactical Patterns](.kiro/steering/ddd-tactical-patterns.md) - Complete DDD standards
- [Core Principles](.kiro/steering/core-principles.md) - Architecture principles
- [Domain Events](.kiro/steering/domain-events.md) - Event implementation

---

## Aggregate Pattern

### Definition

An Aggregate is a cluster of domain objects that can be treated as a single unit. The Aggregate Root is the only member of the Aggregate that outside objects are allowed to hold references to.

### Implementation

```java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends AggregateRoot {
    
    private final OrderId id;
    private CustomerId customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money totalAmount;
    
    // Constructor
    public Order(OrderId id, CustomerId customerId) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.DRAFT;
        this.items = new ArrayList<>();
        this.totalAmount = Money.zero();
        
        // Collect domain event
        collectEvent(OrderCreatedEvent.create(id, customerId));
    }
    
    // Business methods
    public void addItem(Product product, int quantity) {
        validateItemAddition(product, quantity);
        
        OrderItem item = new OrderItem(product.getId(), quantity, product.getPrice());
        items.add(item);
        recalculateTotal();
        
        collectEvent(ItemAddedToOrderEvent.create(id, item));
    }
    
    public void submit() {
        validateOrderSubmission();
        
        this.status = OrderStatus.PENDING;
        
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount));
    }
    
    // Validation
    private void validateOrderSubmission() {
        if (items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot submit empty order");
        }
        if (status != OrderStatus.DRAFT) {
            throw new BusinessRuleViolationException("Order already submitted");
        }
    }
    
    private void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.zero(), Money::add);
    }
}
```

### Best Practices

✅ **Do**:
- Keep aggregates small
- Enforce invariants within aggregate boundaries
- Use domain events for cross-aggregate communication
- Load entire aggregate at once

❌ **Don't**:
- Reference other aggregates directly
- Expose internal collections
- Allow external modification of aggregate state
- Create large, complex aggregates

---

## Entity Pattern

### Definition

An Entity is an object that is defined by its identity rather than its attributes.

### Implementation

```java
public class OrderItem {
    
    private final OrderItemId id;
    private ProductId productId;
    private int quantity;
    private Money price;
    private Money subtotal;
    
    public OrderItem(ProductId productId, int quantity, Money price) {
        this.id = OrderItemId.generate();
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = price.multiply(quantity);
    }
    
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = newQuantity;
        this.subtotal = price.multiply(newQuantity);
    }
    
    // Identity-based equality
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem that = (OrderItem) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

---

## Value Object Pattern

### Definition

A Value Object is an immutable object that is defined by its attributes rather than identity.

### Implementation

```java
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    // Factory methods
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("USD"));
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO, Currency.getInstance("USD"));
    }
    
    // Business methods
    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
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

---

## Repository Pattern

### Definition

A Repository mediates between the domain and data mapping layers, acting like an in-memory collection of domain objects.

### Implementation

```java
// Domain layer - Interface
public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId orderId);
}

// Infrastructure layer - Implementation
@Repository
public class JpaOrderRepository implements OrderRepository {
    
    private final OrderJpaRepository jpaRepository;
    private final OrderMapper mapper;
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(mapper::toDomain);
    }
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

---

## Domain Service Pattern

### Definition

A Domain Service encapsulates domain logic that doesn't naturally fit within an entity or value object.

### Implementation

```java
@Service
public class PricingService {
    
    public Money calculateOrderTotal(Order order, Customer customer) {
        Money subtotal = order.calculateSubtotal();
        Money discount = calculateDiscount(subtotal, customer);
        Money tax = calculateTax(subtotal.subtract(discount));
        
        return subtotal.subtract(discount).add(tax);
    }
    
    private Money calculateDiscount(Money amount, Customer customer) {
        return switch (customer.getMembershipLevel()) {
            case PREMIUM -> amount.multiply(0.10);
            case GOLD -> amount.multiply(0.15);
            case PLATINUM -> amount.multiply(0.20);
            default -> Money.zero();
        };
    }
    
    private Money calculateTax(Money amount) {
        return amount.multiply(0.08); // 8% tax
    }
}
```

---

## Factory Pattern

### Definition

A Factory encapsulates complex object creation logic.

### Implementation

```java
@Component
public class OrderFactory {
    
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    
    public Order createOrder(CreateOrderCommand command) {
        // Validate command
        validateCommand(command);
        
        // Create order
        Order order = new Order(
            OrderId.generate(),
            command.customerId()
        );
        
        // Add items
        for (OrderItemDto itemDto : command.items()) {
            Product product = productRepository.findById(itemDto.productId())
                .orElseThrow(() -> new ProductNotFoundException(itemDto.productId()));
            
            order.addItem(product, itemDto.quantity());
        }
        
        return order;
    }
    
    private void validateCommand(CreateOrderCommand command) {
        if (command.items().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}
```

---

## Specification Pattern

### Definition

A Specification encapsulates business rules that can be combined and reused.

### Implementation

```java
public interface Specification<T> {
    boolean isSatisfiedBy(T candidate);
    
    default Specification<T> and(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
    }
    
    default Specification<T> or(Specification<T> other) {
        return candidate -> this.isSatisfiedBy(candidate) || other.isSatisfiedBy(candidate);
    }
    
    default Specification<T> not() {
        return candidate -> !this.isSatisfiedBy(candidate);
    }
}

// Usage
public class PremiumCustomerSpecification implements Specification<Customer> {
    
    @Override
    public boolean isSatisfiedBy(Customer customer) {
        return customer.getMembershipLevel() == MembershipLevel.PREMIUM
            && customer.getTotalPurchases().compareTo(Money.of(1000)) >= 0;
    }
}

// Combining specifications
Specification<Customer> eligibleForDiscount = 
    new PremiumCustomerSpecification()
        .and(new ActiveCustomerSpecification())
        .and(new NoOutstandingBalanceSpecification());

if (eligibleForDiscount.isSatisfiedBy(customer)) {
    applyDiscount(customer);
}
```

---

## Domain Event Pattern

### Definition

Domain Events represent something that happened in the domain that domain experts care about.

### Implementation

```java
public record OrderSubmittedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    int itemCount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static OrderSubmittedEvent create(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount,
        int itemCount
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(
            orderId, customerId, totalAmount, itemCount,
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    @Override
    public String getEventType() {
        return "OrderSubmitted";
    }
    
    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
}
```

---

## Anti-Corruption Layer Pattern

### Definition

An Anti-Corruption Layer translates between different domain models to prevent external concepts from polluting your domain.

### Implementation

```java
@Component
public class PaymentGatewayAdapter {
    
    private final ExternalPaymentGateway externalGateway;
    
    public PaymentResult processPayment(Payment payment) {
        // Translate domain model to external API model
        ExternalPaymentRequest request = translateToExternal(payment);
        
        // Call external service
        ExternalPaymentResponse response = externalGateway.process(request);
        
        // Translate back to domain model
        return translateToDomain(response);
    }
    
    private ExternalPaymentRequest translateToExternal(Payment payment) {
        return ExternalPaymentRequest.builder()
            .transactionId(payment.getId().getValue())
            .amount(payment.getAmount().getAmount().doubleValue())
            .currency(payment.getAmount().getCurrency().getCurrencyCode())
            .cardNumber(payment.getCardNumber())
            .build();
    }
    
    private PaymentResult translateToDomain(ExternalPaymentResponse response) {
        return new PaymentResult(
            PaymentStatus.valueOf(response.getStatus()),
            response.getTransactionId(),
            response.getMessage()
        );
    }
}
```

---

## Related Documentation

- [DDD Tactical Patterns](.kiro/steering/ddd-tactical-patterns.md)
- [Domain Events](.kiro/steering/domain-events.md)
- [Architecture Constraints](.kiro/steering/architecture-constraints.md)
- [Design Principles](.kiro/steering/design-principles.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Architecture Team
