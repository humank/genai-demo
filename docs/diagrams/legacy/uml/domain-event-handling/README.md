# Domain Event Handling Mechanism Design

This document describes the domain event publishing and handling mechanism in the system, including the design of related components, interaction flows, and testing strategies.

## Design Overview

The system adopts a custom domain event handling mechanism based on the Spring framework, with the following key features:

1. **Domain Event Publishing**: Converts domain events to Spring application events through `DomainEventPublisherAdapter`
2. **Event Subscription**: Uses custom `@EventSubscriber` annotation to mark event handling methods
3. **Event Routing**: Scans and registers event handlers at runtime through `DomainEventSubscriptionManager`
4. **Fallback Mechanism**: Automatically falls back to `DomainEventPublisherService` when `AggregateLifecycle` is unavailable

## UML Diagram Descriptions

### Class Diagram

The class diagram shows the core components of the domain event handling mechanism and their relationships:

- **Domain Layer**:
  - `DomainEvent`: Domain event interface
  - `EventHandler`: Event handler interface
  - `EventSubscriber`: Event subscription annotation
  - `DomainEventBus`: Domain event bus
  - `DomainEventPublisherService`: Domain event publishing service
  - Concrete event handlers (e.g., `OrderEventHandler`)

- **Infrastructure Layer**:
  - `DomainEventPublisherAdapter`: Domain event publisher adapter
  - `DomainEventSubscriptionManager`: Event subscription manager

!Domain Event Handling Class Diagram

### Sequence Diagram

The sequence diagram shows the complete flow from domain event publishing to handling:

1. **Initialization Phase**: `DomainEventSubscriptionManager` scans and registers event subscribers
2. **Event Publishing Flow**: Event transmission path from aggregate root to Spring event publisher
3. **Event Handling Flow**: Invocation process from Spring events to concrete event handlers

!Domain Event Handling Sequence Diagram

### Component Diagram

The component diagram shows the main components of the domain event handling mechanism and their dependencies:

- Interaction between domain layer components and infrastructure layer components
- Integration points with the Spring framework
- Dependency relationships between components

!Domain Event Handling Component Diagram

### Event Flow Diagram

The event flow diagram shows the complete lifecycle of domain events in activity diagram format:

- Event publishing trigger points
- Fallback handling mechanism
- Decision points for event routing and handling
- Error handling strategies

!Domain Event Handling Flow Diagram

## Testing Strategy

To verify the correctness of the domain event handling mechanism, the following testing strategy is recommended:

### Unit Testing

- Test the independent functionality of each component
- Use mock objects to isolate dependencies

### Integration Testing

Integration tests should be placed in the 
`/Users/yikaikao/git/genai-demo/app/src/test/java/solid/humank/genaidemo/integration/event/` 
directory, mainly testing:

1. **Event Publishing Tests**: Verify that events are published correctly

   ```java
   @Test
   public void testEventPublishing() {
       // Publish event directly
       OrderCreatedEvent event = new OrderCreatedEvent("test-order", "test-customer", BigDecimal.ZERO);
       eventPublisherAdapter.publish(event);
       
       // Verify event handling
       verify(orderEventHandler, timeout(1000)).handleOrderCreated(eq(event));
   }
   ```

2. **End-to-End Event Flow Tests**: Verify the complete flow from business operations to event handling

   ```java
   @Test
   public void testEndToEndEventFlow() {
       // Execute business operation
       String orderId = orderService.createOrder("customer-123", "Taipei Xinyi District");
       
       // Verify event handler is called
       verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
   }
   ```

## Best Practices

1. **Event Design**:
   - Events should be immutable
   - Events should contain sufficient contextual information
   - Event names should use past tense (e.g., `OrderCreated` instead of `CreateOrder`)

2. **Event Handling**:
   - Event handlers should be idempotent
   - Avoid executing long-running operations in event handlers
   - Consider using asynchronous processing mechanisms for time-consuming operations

3. **Testing**:
   - Use `@SpyBean` to monitor event handlers
   - Add timeout parameters to handle asynchronous events
   - Verify system state changes after event processing