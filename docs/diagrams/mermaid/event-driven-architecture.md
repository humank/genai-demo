# Event-Driven Architecture

This document shows the event-driven architecture design and implementation of the system.

## Event-Driven Architecture Diagram

```mermaid
graph TB
    subgraph "👤 User Operations"
        USER_ACTION[🖱️ User Action]
        API_CALL[📞 API Call]
    end
    
    subgraph "⚙️ Application Service Layer"
        ORDER_SVC[📦 Order Service]
        CUSTOMER_SVC[👥 Customer Service]
        PAYMENT_SVC[💳 Payment Service]
        INVENTORY_SVC[📊 Inventory Service]
    end
    
    subgraph "🏛️ Domain Layer - Aggregate Roots"
        ORDER_AGG[📋 Order Aggregate]
        CUSTOMER_AGG[👤 Customer Aggregate]
        PRODUCT_AGG[🏷️ Product Aggregate]
        PAYMENT_AGG[💰 Payment Aggregate]
    end
    
    subgraph "📡 Domain Events"
        ORDER_CREATED[📦 OrderCreatedEvent]
        ORDER_CONFIRMED[✅ OrderConfirmedEvent]
        PAYMENT_PROCESSED[💳 PaymentProcessedEvent]
        INVENTORY_RESERVED[📊 InventoryReservedEvent]
        CUSTOMER_UPDATED[👥 CustomerUpdatedEvent]
        LOYALTY_EARNED[🎁 LoyaltyPointsEarnedEvent]
    end
    
    subgraph "🔄 Event Handlers"
        subgraph "📦 Order Event Handlers"
            ORDER_CREATED_HANDLER[📝 OrderCreatedHandler]
            ORDER_CONFIRMED_HANDLER[✅ OrderConfirmedHandler]
        end
        
        subgraph "💳 Payment Event Handlers"
            PAYMENT_HANDLER[💰 PaymentProcessedHandler]
            REFUND_HANDLER[↩️ RefundHandler]
        end
        
        subgraph "📊 Inventory Event Handlers"
            INVENTORY_HANDLER[📦 InventoryReservedHandler]
            STOCK_HANDLER[📈 StockUpdateHandler]
        end
        
        subgraph "👥 Customer Event Handlers"
            CUSTOMER_HANDLER[👤 CustomerUpdatedHandler]
            LOYALTY_HANDLER[🎁 LoyaltyPointsHandler]
            NOTIFICATION_HANDLER[📧 NotificationHandler]
        end
    end
    
    subgraph "📨 Event Infrastructure"
        EVENT_BUS[🚌 Event Bus]
        EVENT_STORE[📚 Event Store]
        MESSAGE_QUEUE[📬 Message Queue]
        DEAD_LETTER[💀 Dead Letter Queue]
    end
    
    subgraph "🔗 External Systems"
        EMAIL_SVC[📧 Email Service]
        SMS_SVC[📱 SMS Service]
        PAYMENT_GATEWAY[💳 Payment Gateway]
        WAREHOUSE[🏭 Warehouse System]
        ANALYTICS[📊 Analytics System]
    end
    
    subgraph "📊 Read Models (CQRS)"
        ORDER_VIEW[📋 Order View Model]
        CUSTOMER_VIEW[👤 Customer View Model]
        INVENTORY_VIEW[📊 Inventory View Model]
        ANALYTICS_VIEW[📈 Analytics View Model]
    end
    
    %% User operation flow
    USER_ACTION --> API_CALL
    API_CALL --> ORDER_SVC
    API_CALL --> CUSTOMER_SVC
    API_CALL --> PAYMENT_SVC
    
    %% Application services to aggregate roots
    ORDER_SVC --> ORDER_AGG
    CUSTOMER_SVC --> CUSTOMER_AGG
    PAYMENT_SVC --> PAYMENT_AGG
    INVENTORY_SVC --> PRODUCT_AGG
    
    %% Aggregate roots generate events
    ORDER_AGG --> ORDER_CREATED
    ORDER_AGG --> ORDER_CONFIRMED
    PAYMENT_AGG --> PAYMENT_PROCESSED
    PRODUCT_AGG --> INVENTORY_RESERVED
    CUSTOMER_AGG --> CUSTOMER_UPDATED
    CUSTOMER_AGG --> LOYALTY_EARNED
    
    %% Events to event bus
    ORDER_CREATED --> EVENT_BUS
    ORDER_CONFIRMED --> EVENT_BUS
    PAYMENT_PROCESSED --> EVENT_BUS
    INVENTORY_RESERVED --> EVENT_BUS
    CUSTOMER_UPDATED --> EVENT_BUS
    LOYALTY_EARNED --> EVENT_BUS
    
    %% Event bus to handlers
    EVENT_BUS --> ORDER_CREATED_HANDLER
    EVENT_BUS --> ORDER_CONFIRMED_HANDLER
    EVENT_BUS --> PAYMENT_HANDLER
    EVENT_BUS --> INVENTORY_HANDLER
    EVENT_BUS --> CUSTOMER_HANDLER
    EVENT_BUS --> LOYALTY_HANDLER
    EVENT_BUS --> NOTIFICATION_HANDLER
    
    %% Event handlers to external systems
    NOTIFICATION_HANDLER --> EMAIL_SVC
    NOTIFICATION_HANDLER --> SMS_SVC
    PAYMENT_HANDLER --> PAYMENT_GATEWAY
    INVENTORY_HANDLER --> WAREHOUSE
    
    %% Event handlers to read models
    ORDER_CREATED_HANDLER --> ORDER_VIEW
    ORDER_CONFIRMED_HANDLER --> ORDER_VIEW
    CUSTOMER_HANDLER --> CUSTOMER_VIEW
    INVENTORY_HANDLER --> INVENTORY_VIEW
    PAYMENT_HANDLER --> ANALYTICS_VIEW
    
    %% Event infrastructure
    EVENT_BUS --> EVENT_STORE
    EVENT_BUS --> MESSAGE_QUEUE
    MESSAGE_QUEUE --> DEAD_LETTER
    
    %% Analytics system
    EVENT_STORE --> ANALYTICS
    ANALYTICS_VIEW --> ANALYTICS
    
    %% Style definitions
    classDef userAction fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    classDef application fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef domain fill:#e8f5e8,stroke:#2e7d32,stroke-width:3px
    classDef events fill:#fff3e0,stroke:#ef6c00,stroke-width:2px
    classDef handlers fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef infrastructure fill:#f1f8e9,stroke:#558b2f,stroke-width:2px
    classDef external fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef readModel fill:#e0f2f1,stroke:#00695c,stroke-width:2px
    
    class USER_ACTION,API_CALL userAction
    class ORDER_SVC,CUSTOMER_SVC,PAYMENT_SVC,INVENTORY_SVC application
    class ORDER_AGG,CUSTOMER_AGG,PRODUCT_AGG,PAYMENT_AGG domain
    class ORDER_CREATED,ORDER_CONFIRMED,PAYMENT_PROCESSED,INVENTORY_RESERVED,CUSTOMER_UPDATED,LOYALTY_EARNED events
    class ORDER_CREATED_HANDLER,ORDER_CONFIRMED_HANDLER,PAYMENT_HANDLER,REFUND_HANDLER,INVENTORY_HANDLER,STOCK_HANDLER,CUSTOMER_HANDLER,LOYALTY_HANDLER,NOTIFICATION_HANDLER handlers
    class EVENT_BUS,EVENT_STORE,MESSAGE_QUEUE,DEAD_LETTER infrastructure
    class EMAIL_SVC,SMS_SVC,PAYMENT_GATEWAY,WAREHOUSE,ANALYTICS external
    class ORDER_VIEW,CUSTOMER_VIEW,INVENTORY_VIEW,ANALYTICS_VIEW readModel
```

## Event Flow Examples

### 📦 Order Creation Flow

```mermaid
sequenceDiagram
    participant User as 👤 User
    participant API as 🔌 Order API
    participant OrderSvc as 📦 Order Service
    participant OrderAgg as 📋 Order Aggregate
    participant EventBus as 🚌 Event Bus
    participant InventoryHandler as 📊 Inventory Handler
    participant PaymentHandler as 💳 Payment Handler
    participant NotificationHandler as 📧 Notification Handler
    
    User->>API: Create Order Request
    API->>OrderSvc: CreateOrderCommand
    OrderSvc->>OrderAgg: Create Order
    OrderAgg->>OrderAgg: Validate Business Rules
    OrderAgg->>EventBus: Publish OrderCreatedEvent
    
    par Parallel Event Processing
        EventBus->>InventoryHandler: Handle Inventory Reservation
        InventoryHandler->>InventoryHandler: Reserve Inventory
        InventoryHandler->>EventBus: Publish InventoryReservedEvent
    and
        EventBus->>PaymentHandler: Handle Payment
        PaymentHandler->>PaymentHandler: Process Payment
        PaymentHandler->>EventBus: Publish PaymentProcessedEvent
    and
        EventBus->>NotificationHandler: Send Notification
        NotificationHandler->>NotificationHandler: Send Confirmation Email
    end
    
    API-->>User: Order Created Successfully
```

## Event Design Principles

### 📡 Event Naming Conventions

- Use past tense verbs: `OrderCreated`, `PaymentProcessed`
- Include aggregate name: `Customer*Event`, `Order*Event`
- Specifically describe what happened: `CustomerProfileUpdated`

### 💎 Event Content Design

```java
// Domain event as immutable record
public record OrderCreatedEvent(
    OrderId orderId,
    CustomerId customerId,
    Money totalAmount,
    List<OrderItem> items,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static OrderCreatedEvent create(
        OrderId orderId, 
        CustomerId customerId, 
        Money totalAmount,
        List<OrderItem> items
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderCreatedEvent(
            orderId, customerId, totalAmount, items,
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

### 🔄 Event Handler Design

```java
@Component
public class OrderCreatedEventHandler extends AbstractDomainEventHandler<OrderCreatedEvent> {
    
    @Override
    @Transactional
    public void handle(OrderCreatedEvent event) {
        // Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            return;
        }
        
        try {
            // Execute business logic
            reserveInventory(event.getItems());
            updateCustomerStatistics(event.getCustomerId());
            sendOrderConfirmation(event);
            
            // Mark event as processed
            markEventAsProcessed(event.getEventId());
            
        } catch (Exception e) {
            logEventProcessingError(event, e);
            throw new DomainEventProcessingException("Failed to process order creation", e);
        }
    }
    
    @Override
    public Class<OrderCreatedEvent> getSupportedEventType() {
        return OrderCreatedEvent.class;
    }
}
```

## CQRS Implementation

### 📝 Command Side

- Handle write operations
- Maintain aggregate root state
- Publish domain events

### 📖 Query Side

- Handle read operations
- Maintain read models
- Listen to domain events to update views

### 🔄 Event Sourcing

```java
@Component
public class EventStore {
    
    public void store(DomainEvent event) {
        StoredEvent storedEvent = new StoredEvent(
            event.getEventId().toString(),
            event.getEventType(),
            event.getAggregateId(),
            serializeEvent(event),
            event.getOccurredOn()
        );
        
        eventRepository.save(storedEvent);
    }
    
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        return eventRepository.findByAggregateIdOrderByOccurredOnAsc(aggregateId)
            .stream()
            .map(this::deserializeEvent)
            .toList();
    }
}
```

## Error Handling and Recovery

### 💀 Dead Letter Queue

- Handle failed events
- Support manual retry
- Error analysis and monitoring

### 🔄 Retry Mechanism

```java
@Component
public class ResilientEventHandler {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleEvent(DomainEvent event) {
        // Event processing logic
    }
    
    @Recover
    public void recover(TransientException ex, DomainEvent event) {
        deadLetterService.send(event, ex);
    }
}
```

## Monitoring and Observability

### 📊 Event Metrics

- Event publishing rate
- Processing latency
- Error rate
- Retry count

### 🔍 Event Tracing

- Distributed tracing
- Event correlation ID
- Processing chain tracking

## Related Documentation

- [Architecture Overview](architecture-overview.md) - Overall system architecture
- [DDD Layered Architecture](ddd-layered-architecture.md) - DDD implementation
- [API Interactions](api-interactions.md) - API design