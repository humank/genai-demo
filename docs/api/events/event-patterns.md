# Event Patterns

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document describes common event patterns and best practices for the GenAI Demo platform.

---

## Quick Reference

For complete event documentation, see:
- [Event Catalog](event-catalog.md) - All available events
- [Domain Events](.kiro/steering/domain-events.md) - Event implementation standards

---

## Event Design Patterns

### 1. Event Notification Pattern

Simple notification that something happened.

```java
public record OrderSubmittedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static OrderSubmittedEvent create(
        OrderId orderId,
        CustomerId customerId,
        Money totalAmount
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(
            orderId, customerId, totalAmount,
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

**Use When**:
- Other services need to know something happened
- No data transfer required beyond notification
- Loose coupling between services

---

### 2. Event-Carried State Transfer Pattern

Event contains all data needed by consumers.

```java
public record CustomerProfileUpdatedEvent(
    CustomerId customerId,
    CustomerName name,
    Email email,
    Phone phone,
    Address address,
    MembershipLevel membershipLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static CustomerProfileUpdatedEvent create(Customer customer) {
        var metadata = DomainEvent.createEventMetadata();
        return new CustomerProfileUpdatedEvent(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getAddress(),
            customer.getMembershipLevel(),
            metadata.eventId(),
            metadata.occurredOn()
        );
    }
}
```

**Use When**:
- Consumers need complete data
- Avoid additional queries
- Support offline processing

---

### 3. Event Sourcing Pattern

Store all state changes as events.

```java
public class Order {
    private List<DomainEvent> events = new ArrayList<>();
    
    public static Order reconstruct(List<DomainEvent> history) {
        Order order = new Order();
        for (DomainEvent event : history) {
            order.apply(event);
        }
        return order;
    }
    
    private void apply(DomainEvent event) {
        switch (event) {
            case OrderCreatedEvent e -> applyOrderCreated(e);
            case ItemAddedEvent e -> applyItemAdded(e);
            case OrderSubmittedEvent e -> applyOrderSubmitted(e);
            default -> throw new IllegalArgumentException("Unknown event type");
        }
    }
}
```

**Use When**:
- Need complete audit trail
- Time-travel queries required
- Complex domain logic

---

### 4. CQRS Pattern

Separate read and write models using events.

```java
// Write Model (Command Side)
@Service
public class OrderCommandService {
    
    public void submitOrder(SubmitOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow();
        
        order.submit();
        orderRepository.save(order);
        
        // Publish event
        eventPublisher.publish(OrderSubmittedEvent.create(order));
    }
}

// Read Model (Query Side)
@Component
public class OrderQueryModelUpdater {
    
    @EventListener
    public void on(OrderSubmittedEvent event) {
        OrderReadModel readModel = OrderReadModel.builder()
            .orderId(event.orderId().getValue())
            .customerId(event.customerId().getValue())
            .totalAmount(event.totalAmount().getAmount())
            .status("SUBMITTED")
            .submittedAt(event.occurredOn())
            .build();
        
        orderReadModelRepository.save(readModel);
    }
}
```

**Use When**:
- Different read/write requirements
- High read/write ratio
- Complex queries needed

---

### 5. Saga Pattern

Coordinate long-running transactions across services.

```java
@Component
public class OrderProcessingSaga {
    
    @EventListener
    @Order(1)
    public void on(OrderSubmittedEvent event) {
        // Step 1: Reserve inventory
        try {
            inventoryService.reserveItems(event.orderId());
        } catch (Exception e) {
            publishCompensation(new OrderSubmissionFailedEvent(event.orderId()));
        }
    }
    
    @EventListener
    @Order(2)
    public void on(InventoryReservedEvent event) {
        // Step 2: Process payment
        try {
            paymentService.processPayment(event.orderId());
        } catch (Exception e) {
            // Compensate: Release inventory
            inventoryService.releaseItems(event.orderId());
            publishCompensation(new PaymentFailedEvent(event.orderId()));
        }
    }
    
    @EventListener
    @Order(3)
    public void on(PaymentProcessedEvent event) {
        // Step 3: Confirm order
        orderService.confirmOrder(event.orderId());
    }
}
```

**Use When**:
- Multi-step business processes
- Need compensation logic
- Distributed transactions

---

## Event Versioning Patterns

### Schema Evolution Pattern

Add optional fields for backward compatibility.

```java
// V1
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName name,
    Email email,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }

// V2 - Backward compatible
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName name,
    Email email,
    Optional<Phone> phone,        // New optional field
    Optional<Address> address,    // New optional field
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    // Factory for V1 compatibility
    public static CustomerCreatedEvent createV1(
        CustomerId customerId,
        CustomerName name,
        Email email
    ) {
        var metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, name, email,
            Optional.empty(),
            Optional.empty(),
            metadata.eventId(),
            metadata.occurredOn()
        );
    }
}
```

---

## Event Ordering Patterns

### Timestamp-Based Ordering

```java
@Component
public class EventProcessor {
    
    private final Map<String, LocalDateTime> lastProcessedTimestamp = new ConcurrentHashMap<>();
    
    @EventListener
    public void process(DomainEvent event) {
        String aggregateId = event.getAggregateId();
        LocalDateTime eventTime = event.getOccurredOn();
        
        // Check if event is out of order
        LocalDateTime lastProcessed = lastProcessedTimestamp.get(aggregateId);
        if (lastProcessed != null && eventTime.isBefore(lastProcessed)) {
            logger.warn("Out of order event detected",
                kv("aggregateId", aggregateId),
                kv("eventTime", eventTime),
                kv("lastProcessed", lastProcessed)
            );
            // Handle out-of-order event
            return;
        }
        
        // Process event
        handleEvent(event);
        
        // Update timestamp
        lastProcessedTimestamp.put(aggregateId, eventTime);
    }
}
```

### Sequence Number Ordering

```java
public interface DomainEvent {
    UUID getEventId();
    LocalDateTime getOccurredOn();
    String getAggregateId();
    long getSequenceNumber(); // Add sequence number
}

@Component
public class SequencedEventProcessor {
    
    private final Map<String, Long> lastSequence = new ConcurrentHashMap<>();
    
    @EventListener
    public void process(DomainEvent event) {
        String aggregateId = event.getAggregateId();
        long eventSequence = event.getSequenceNumber();
        
        Long lastSeq = lastSequence.get(aggregateId);
        if (lastSeq != null && eventSequence <= lastSeq) {
            logger.warn("Duplicate or out-of-order event",
                kv("aggregateId", aggregateId),
                kv("eventSequence", eventSequence),
                kv("lastSequence", lastSeq)
            );
            return;
        }
        
        handleEvent(event);
        lastSequence.put(aggregateId, eventSequence);
    }
}
```

---

## Event Idempotency Patterns

### Event ID Tracking

```java
@Component
public class IdempotentEventHandler {
    
    private final ProcessedEventRepository processedEventRepository;
    
    @EventListener
    @Transactional
    public void handle(DomainEvent event) {
        // Check if already processed
        if (processedEventRepository.existsByEventId(event.getEventId())) {
            logger.debug("Event already processed, skipping",
                kv("eventId", event.getEventId())
            );
            return;
        }
        
        try {
            // Process event
            processEvent(event);
            
            // Mark as processed
            processedEventRepository.save(
                new ProcessedEvent(event.getEventId(), Instant.now())
            );
            
        } catch (Exception e) {
            logger.error("Event processing failed",
                kv("eventId", event.getEventId()),
                e
            );
            throw e;
        }
    }
}
```

---

## Event Retry Patterns

### Exponential Backoff

```java
@Component
public class RetryableEventHandler {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 5,
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2,
            maxDelay = 60000
        )
    )
    @EventListener
    public void handle(DomainEvent event) {
        processEvent(event);
    }
    
    @Recover
    public void recover(TransientException e, DomainEvent event) {
        logger.error("Event processing failed after retries",
            kv("eventId", event.getEventId()),
            kv("attempts", 5),
            e
        );
        
        // Send to dead letter queue
        deadLetterService.send(event, e);
    }
}
```

---

## Event Filtering Patterns

### Content-Based Filtering

```java
@Component
public class EventFilter {
    
    @EventListener
    public void onCustomerEvent(CustomerEvent event) {
        // Filter by customer type
        if (event.getCustomerType() == CustomerType.PREMIUM) {
            handlePremiumCustomerEvent(event);
        }
    }
    
    @EventListener
    public void onOrderEvent(OrderEvent event) {
        // Filter by order amount
        if (event.getTotalAmount().compareTo(Money.of(1000)) > 0) {
            handleLargeOrderEvent(event);
        }
    }
}
```

---

## Best Practices

### Event Naming

✅ **Do**:
- Use past tense: `OrderSubmitted`, `PaymentProcessed`
- Be specific: `CustomerEmailUpdated` not `CustomerChanged`
- Include context: `Order.Submitted` not just `Submitted`

❌ **Don't**:
- Use present tense: `SubmitOrder`
- Be vague: `DataChanged`
- Omit context: `Updated`

### Event Size

✅ **Do**:
- Keep events small (< 1KB)
- Include only necessary data
- Use references for large data

❌ **Don't**:
- Include entire aggregates
- Embed binary data
- Add unnecessary fields

### Event Granularity

✅ **Do**:
- One event per business fact
- Atomic events
- Clear business meaning

❌ **Don't**:
- Combine multiple facts
- Technical events
- Implementation details

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Architecture Team
