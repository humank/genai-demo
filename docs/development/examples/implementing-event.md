# Implementing a Domain Event

> **Last Updated**: 2025-10-25

## Overview

This guide shows how to implement domain events in the Enterprise E-Commerce Platform, following event-driven architecture principles and DDD patterns.

## Example: Implement Order Shipped Event

We'll implement an event that is published when an order is shipped, and create handlers to update inventory and send notifications.

### Step 1: Define the Event

#### Create OrderShippedEvent

```java
// Location: domain/order/events/OrderShippedEvent.java
package solid.humank.genaidemo.domain.order.events;

import solid.humank.genaidemo.domain.order.model.valueobject.OrderId;
import solid.humank.genaidemo.domain.customer.model.valueobject.CustomerId;
import solid.humank.genaidemo.domain.shared.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderShippedEvent(
    OrderId orderId,
    CustomerId customerId,
    String trackingNumber,
    String carrier,
    LocalDateTime shippedAt,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static OrderShippedEvent create(
        OrderId orderId,
        CustomerId customerId,
        String trackingNumber,
        String carrier,
        LocalDateTime shippedAt
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderShippedEvent(
            orderId,
            customerId,
            trackingNumber,
            carrier,
            shippedAt,
            metadata.eventId(),
            metadata.occurredOn()
        );
    }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() {
        return orderId.value();
    }
}
```

### Step 2: Publish Event from Aggregate

#### Update Order Aggregate

```java
// Location: domain/order/model/aggregate/Order.java
@AggregateRoot(name = "Order", boundedContext = "Order", version = "1.0")
public class Order extends AggregateRoot {
    
    private final OrderId id;
    private final CustomerId customerId;
    private OrderStatus status;
    private String trackingNumber;
    private String carrier;
    private LocalDateTime shippedAt;
    
    // Existing methods...
    
    public void ship(String trackingNumber, String carrier) {
        validateCanShip();
        
        this.status = OrderStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.shippedAt = LocalDateTime.now();
        
        // Collect domain event
        collectEvent(OrderShippedEvent.create(
            id,
            customerId,
            trackingNumber,
            carrier,
            shippedAt
        ));
    }
    
    private void validateCanShip() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException(
                "Can only ship confirmed orders. Current status: " + status
            );
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot ship empty order");
        }
    }
    
    // Getters...
}
```

### Step 3: Publish Event from Application Service

```java
// Location: application/order/OrderApplicationService.java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final DomainEventApplicationService eventService;
    
    public void shipOrder(ShipOrderCommand command) {
        // Load order
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        
        // Execute business operation (event is collected)
        order.ship(command.trackingNumber(), command.carrier());
        
        // Save order
        orderRepository.save(order);
        
        // Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
}
```

### Step 4: Create Event Handlers

#### Handler 1: Send Shipping Notification

```java
// Location: infrastructure/order/messaging/OrderShippedNotificationHandler.java
package solid.humank.genaidemo.infrastructure.order.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import solid.humank.genaidemo.domain.order.events.OrderShippedEvent;
import solid.humank.genaidemo.infrastructure.shared.AbstractDomainEventHandler;

@Component
public class OrderShippedNotificationHandler 
    extends AbstractDomainEventHandler<OrderShippedEvent> {
    
    private final EmailService emailService;
    private final SmsService smsService;
    
    public OrderShippedNotificationHandler(
        EmailService emailService,
        SmsService smsService
    ) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
    
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderShippedEvent event) {
        // Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            logger.info("Event already processed: {}", event.getEventId());
            return;
        }
        
        try {
            // Send email notification
            emailService.sendShippingNotification(
                event.customerId(),
                event.orderId(),
                event.trackingNumber(),
                event.carrier()
            );
            
            // Send SMS notification
            smsService.sendShippingNotification(
                event.customerId(),
                event.trackingNumber()
            );
            
            // Mark as processed
            markEventAsProcessed(event.getEventId());
            
            logger.info("Shipping notifications sent for order: {}", 
                event.orderId());
                
        } catch (Exception e) {
            logger.error("Failed to send shipping notifications for order: {}", 
                event.orderId(), e);
            throw new EventProcessingException(
                "Failed to send shipping notifications", e
            );
        }
    }
    
    @Override
    public Class<OrderShippedEvent> getSupportedEventType() {
        return OrderShippedEvent.class;
    }
}
```

#### Handler 2: Update Inventory

```java
// Location: infrastructure/inventory/messaging/OrderShippedInventoryHandler.java
package solid.humank.genaidemo.infrastructure.inventory.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import solid.humank.genaidemo.domain.order.events.OrderShippedEvent;
import solid.humank.genaidemo.infrastructure.shared.AbstractDomainEventHandler;

@Component
public class OrderShippedInventoryHandler 
    extends AbstractDomainEventHandler<OrderShippedEvent> {
    
    private final InventoryService inventoryService;
    
    public OrderShippedInventoryHandler(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderShippedEvent event) {
        // Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            logger.info("Event already processed: {}", event.getEventId());
            return;
        }
        
        try {
            // Update inventory status from reserved to shipped
            inventoryService.markAsShipped(event.orderId());
            
            // Mark as processed
            markEventAsProcessed(event.getEventId());
            
            logger.info("Inventory updated for shipped order: {}", 
                event.orderId());
                
        } catch (Exception e) {
            logger.error("Failed to update inventory for order: {}", 
                event.orderId(), e);
            throw new EventProcessingException(
                "Failed to update inventory", e
            );
        }
    }
    
    @Override
    public Class<OrderShippedEvent> getSupportedEventType() {
        return OrderShippedEvent.class;
    }
}
```

#### Handler 3: Update Analytics

```java
// Location: infrastructure/analytics/messaging/OrderShippedAnalyticsHandler.java
package solid.humank.genaidemo.infrastructure.analytics.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import solid.humank.genaidemo.domain.order.events.OrderShippedEvent;
import solid.humank.genaidemo.infrastructure.shared.AbstractDomainEventHandler;

@Component
public class OrderShippedAnalyticsHandler 
    extends AbstractDomainEventHandler<OrderShippedEvent> {
    
    private final AnalyticsService analyticsService;
    
    public OrderShippedAnalyticsHandler(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderShippedEvent event) {
        // Check idempotency
        if (isEventAlreadyProcessed(event.getEventId())) {
            return;
        }
        
        try {
            // Track shipping metrics
            analyticsService.trackOrderShipped(
                event.orderId(),
                event.customerId(),
                event.carrier(),
                event.shippedAt()
            );
            
            // Mark as processed
            markEventAsProcessed(event.getEventId());
            
        } catch (Exception e) {
            // Log but don't fail - analytics is not critical
            logger.warn("Failed to track analytics for order: {}", 
                event.orderId(), e);
        }
    }
    
    @Override
    public Class<OrderShippedEvent> getSupportedEventType() {
        return OrderShippedEvent.class;
    }
}
```

### Step 5: Write Tests

#### Test Event Creation

```java
@Test
void should_collect_order_shipped_event_when_order_is_shipped() {
    // Given
    Order order = createConfirmedOrder();
    String trackingNumber = "TRACK-123";
    String carrier = "UPS";
    
    // When
    order.ship(trackingNumber, carrier);
    
    // Then
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    assertThat(order.getTrackingNumber()).isEqualTo(trackingNumber);
    assertThat(order.getCarrier()).isEqualTo(carrier);
    assertThat(order.hasUncommittedEvents()).isTrue();
    
    List<DomainEvent> events = order.getUncommittedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(OrderShippedEvent.class);
    
    OrderShippedEvent event = (OrderShippedEvent) events.get(0);
    assertThat(event.orderId()).isEqualTo(order.getId());
    assertThat(event.trackingNumber()).isEqualTo(trackingNumber);
    assertThat(event.carrier()).isEqualTo(carrier);
}
```

#### Test Event Handler

```java
@ExtendWith(MockitoExtension.class)
class OrderShippedNotificationHandlerTest {
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private SmsService smsService;
    
    @InjectMocks
    private OrderShippedNotificationHandler handler;
    
    @Test
    void should_send_notifications_when_order_shipped() {
        // Given
        OrderShippedEvent event = OrderShippedEvent.create(
            OrderId.of("ORD-001"),
            CustomerId.of("CUST-001"),
            "TRACK-123",
            "UPS",
            LocalDateTime.now()
        );
        
        // When
        handler.handle(event);
        
        // Then
        verify(emailService).sendShippingNotification(
            event.customerId(),
            event.orderId(),
            event.trackingNumber(),
            event.carrier()
        );
        verify(smsService).sendShippingNotification(
            event.customerId(),
            event.trackingNumber()
        );
    }
    
    @Test
    void should_not_process_event_twice() {
        // Given
        OrderShippedEvent event = OrderShippedEvent.create(
            OrderId.of("ORD-001"),
            CustomerId.of("CUST-001"),
            "TRACK-123",
            "UPS",
            LocalDateTime.now()
        );
        
        // When - process event twice
        handler.handle(event);
        handler.handle(event);
        
        // Then - should only send notifications once
        verify(emailService, times(1)).sendShippingNotification(
            any(), any(), any(), any()
        );
    }
}
```

### Step 6: Integration Test

```java
@SpringBootTest
@ActiveProfiles("test")
class OrderShippedEventIntegrationTest {
    
    @Autowired
    private OrderApplicationService orderService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @MockBean
    private EmailService emailService;
    
    @MockBean
    private SmsService smsService;
    
    @Test
    void should_publish_event_and_trigger_handlers_when_order_shipped() {
        // Given
        Order order = createConfirmedOrder();
        orderRepository.save(order);
        
        ShipOrderCommand command = new ShipOrderCommand(
            order.getId(),
            "TRACK-123",
            "UPS"
        );
        
        // When
        orderService.shipOrder(command);
        
        // Then - verify order updated
        Order shippedOrder = orderRepository.findById(order.getId()).get();
        assertThat(shippedOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        
        // Then - verify handlers executed
        verify(emailService).sendShippingNotification(
            any(), any(), eq("TRACK-123"), eq("UPS")
        );
        verify(smsService).sendShippingNotification(
            any(), eq("TRACK-123")
        );
    }
}
```

### Step 7: Document the Event

Add to `docs/api/events/order-events.md`:

```markdown
## OrderShippedEvent

Published when an order is shipped.

### Event Structure

```json
{
  "eventId": "uuid",
  "eventType": "OrderShipped",
  "occurredOn": "2025-10-25T10:00:00Z",
  "aggregateId": "ORD-001",
  "data": {
    "orderId": "ORD-001",
    "customerId": "CUST-001",
    "trackingNumber": "TRACK-123",
    "carrier": "UPS",
    "shippedAt": "2025-10-25T10:00:00Z"
  }
}
```text

### Event Handlers

1. **OrderShippedNotificationHandler**: Sends email and SMS notifications
2. **OrderShippedInventoryHandler**: Updates inventory status
3. **OrderShippedAnalyticsHandler**: Tracks shipping metrics

### Example Handler

```java
@Component
public class OrderShippedNotificationHandler 
    extends AbstractDomainEventHandler<OrderShippedEvent> {
    
    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderShippedEvent event) {
        // Handle event
    }
}
```text

```

## Summary

You've successfully implemented a domain event! The key steps were:

1. ✅ Define the event as an immutable record
2. ✅ Publish event from aggregate
3. ✅ Publish event from application service
4. ✅ Create event handlers
5. ✅ Write comprehensive tests
6. ✅ Document the event

## Best Practices

- **Immutable Events**: Use records for immutability
- **Event Metadata**: Include eventId and occurredOn
- **Idempotency**: Check if event already processed
- **Error Handling**: Handle failures gracefully
- **Async Processing**: Use @TransactionalEventListener(AFTER_COMMIT)
- **Testing**: Test event creation and handlers separately
- **Documentation**: Document event structure and handlers

## Common Patterns

### Event Ordering

Use `@Order` annotation for handler precedence:

```java
@Component
@Order(1)  // Execute first
public class CriticalHandler extends AbstractDomainEventHandler<OrderShippedEvent> {
    // Handler implementation
}

@Component
@Order(2)  // Execute after critical handler
public class NonCriticalHandler extends AbstractDomainEventHandler<OrderShippedEvent> {
    // Handler implementation
}
```

### Retry Logic

```java
@Retryable(
    value = {TransientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
@Override
public void handle(OrderShippedEvent event) {
    // Handler with retry capability
}

@Recover
public void recover(TransientException ex, OrderShippedEvent event) {
    // Handle final failure
    deadLetterService.send(event, ex);
}
```

## Related Documentation

- [Domain Events Guide](../../architecture/patterns/domain-events.md)
- [Creating Aggregate](creating-aggregate.md)
- [Event-Driven Architecture](../../architecture/patterns/event-driven.md)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
