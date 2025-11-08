---
adr_number: 025
title: "Saga Pattern for Distributed Transactions"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [020, 021, 026]
affected_viewpoints: ["functional", "concurrency"]
affected_perspectives: ["availability", "performance"]
---

# ADR-025: Saga Pattern for Distributed Transactions

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a reliable way to handle distributed transactions across multiple bounded contexts:

**Business Requirements**:

- **Data Consistency**: Maintain consistency across microservices
- **Reliability**: Ensure business processes complete successfully
- **Compensation**: Handle failures gracefully with rollback
- **Visibility**: Track long-running business processes
- **Performance**: Avoid blocking distributed transactions
- **Scalability**: Support high transaction volumes

**Technical Challenges**:

- 13 bounded contexts with separate databases
- No distributed transactions (2PC not suitable)
- Complex business workflows spanning multiple services
- Need for compensation logic
- Failure handling and recovery
- Monitoring long-running processes

**Example Workflows**:

1. **Order Submission**: Order → Inventory → Payment → Shipping
2. **Order Cancellation**: Order → Payment Refund → Inventory Release
3. **Customer Registration**: Customer → Email → Loyalty Program

### Business Context

**Business Drivers**:

- Ensure order processing reliability
- Handle payment failures gracefully
- Maintain inventory accuracy
- Support complex business workflows
- Enable business process monitoring

**Constraints**:

- Budget: $60,000 for implementation
- Timeline: 2 months
- Team: 3 senior developers
- Must work with existing event-driven architecture
- Cannot use distributed transactions (2PC)
- Must support compensation

### Technical Context

**Current Approach**:

- Domain events for integration
- No coordination mechanism
- Manual compensation logic
- Difficult to track workflow state

**Target Approach**:

- Saga pattern for orchestration
- Automated compensation
- Workflow state tracking
- Monitoring and observability

## Decision Drivers

1. **Consistency**: Maintain eventual consistency across services
2. **Reliability**: Ensure workflows complete or compensate
3. **Observability**: Track workflow progress and failures
4. **Simplicity**: Easy to understand and implement
5. **Performance**: Non-blocking, asynchronous execution
6. **Scalability**: Handle high transaction volumes
7. **Maintainability**: Easy to add new workflows
8. **Testability**: Easy to test compensation logic

## Considered Options

### Option 1: Choreography-Based Saga (Recommended)

**Description**: Services coordinate through domain events, no central orchestrator

**Order Submission Saga Example**:

```java
// Step 1: Order Service - Create Order
@Service
@Transactional
public class OrderApplicationService {
    
    public void submitOrder(SubmitOrderCommand command) {
        // Create order
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow();
        
        order.submit();
        orderRepository.save(order);
        
        // Publish event (triggers next step)
        eventPublisher.publish(OrderSubmittedEvent.create(
            order.getId(),
            order.getCustomerId(),
            order.getItems(),
            order.getTotalAmount()
        ));
    }
}

// Step 2: Inventory Service - Reserve Items
@Component
public class OrderSubmittedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        try {
            // Reserve inventory
            inventoryService.reserveItems(
                event.orderId(),
                event.items()
            );
            
            // Publish success event (triggers next step)
            eventPublisher.publish(InventoryReservedEvent.create(
                event.orderId(),
                event.items()
            ));
            
        } catch (InsufficientInventoryException e) {
            // Publish failure event (triggers compensation)
            eventPublisher.publish(InventoryReservationFailedEvent.create(
                event.orderId(),
                e.getMessage()
            ));
        }
    }
}

// Step 3: Payment Service - Process Payment
@Component
public class InventoryReservedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(InventoryReservedEvent event) {
        try {
            // Process payment
            Payment payment = paymentService.processPayment(
                event.orderId(),
                event.amount()
            );
            
            // Publish success event (triggers next step)
            eventPublisher.publish(PaymentProcessedEvent.create(
                event.orderId(),
                payment.getId(),
                payment.getAmount()
            ));
            
        } catch (PaymentFailedException e) {
            // Publish failure event (triggers compensation)
            eventPublisher.publish(PaymentFailedEvent.create(
                event.orderId(),
                e.getMessage()
            ));
        }
    }
}

// Step 4: Order Service - Confirm Order
@Component
public class PaymentProcessedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(PaymentProcessedEvent event) {
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow();
        
        order.confirm();
        orderRepository.save(order);
        
        // Publish completion event
        eventPublisher.publish(OrderConfirmedEvent.create(
            event.orderId()
        ));
    }
}

// Compensation: Payment Failed - Release Inventory
@Component
public class PaymentFailedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(PaymentFailedEvent event) {
        // Release reserved inventory
        inventoryService.releaseReservation(event.orderId());
        
        // Update order status
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow();
        
        order.fail("Payment failed: " + event.reason());
        orderRepository.save(order);
        
        // Publish compensation event
        eventPublisher.publish(OrderFailedEvent.create(
            event.orderId(),
            "Payment failed"
        ));
    }
}

// Compensation: Inventory Failed - Cancel Order
@Component
public class InventoryReservationFailedEventHandler {
    
    @EventListener
    @Transactional
    public void handle(InventoryReservationFailedEvent event) {
        // Update order status
        Order order = orderRepository.findById(event.orderId())
            .orElseThrow();
        
        order.fail("Insufficient inventory: " + event.reason());
        orderRepository.save(order);
        
        // Publish compensation event
        eventPublisher.publish(OrderFailedEvent.create(
            event.orderId(),
            "Insufficient inventory"
        ));
    }
}
```

**Saga State Tracking**:

```java
// Track saga execution state
@Entity
@Table(name = "saga_instances")
public class SagaInstance {
    
    @Id
    private String sagaId;
    
    @Column(name = "saga_type")
    private String sagaType;
    
    @Column(name = "aggregate_id")
    private String aggregateId;
    
    @Column(name = "current_step")
    private String currentStep;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "compensation_started_at")
    private LocalDateTime compensationStartedAt;
}

@Service
public class SagaTracker {
    
    private final SagaInstanceRepository repository;
    
    public void startSaga(String sagaType, String aggregateId) {
        SagaInstance saga = new SagaInstance(
            UUID.randomUUID().toString(),
            sagaType,
            aggregateId,
            "STARTED",
            SagaStatus.IN_PROGRESS,
            LocalDateTime.now()
        );
        
        repository.save(saga);
    }
    
    public void updateStep(String aggregateId, String step) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setCurrentStep(step);
        repository.save(saga);
    }
    
    public void completeSaga(String aggregateId) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCompletedAt(LocalDateTime.now());
        repository.save(saga);
    }
    
    public void failSaga(String aggregateId, String error) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setErrorMessage(error);
        saga.setCompensationStartedAt(LocalDateTime.now());
        repository.save(saga);
    }
    
    public void compensationComplete(String aggregateId) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow();
        
        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCompletedAt(LocalDateTime.now());
        repository.save(saga);
    }
}
```

**Saga Monitoring**:

```java
@RestController
@RequestMapping("/api/v1/sagas")
public class SagaMonitoringController {
    
    private final SagaInstanceRepository repository;
    
    @GetMapping("/{sagaId}")
    public SagaInstanceDto getSagaStatus(@PathVariable String sagaId) {
        SagaInstance saga = repository.findById(sagaId)
            .orElseThrow(() -> new SagaNotFoundException(sagaId));
        
        return SagaInstanceDto.from(saga);
    }
    
    @GetMapping("/aggregate/{aggregateId}")
    public SagaInstanceDto getSagaByAggregate(@PathVariable String aggregateId) {
        SagaInstance saga = repository.findByAggregateId(aggregateId)
            .orElseThrow(() -> new SagaNotFoundException(aggregateId));
        
        return SagaInstanceDto.from(saga);
    }
    
    @GetMapping("/failed")
    public List<SagaInstanceDto> getFailedSagas() {
        return repository.findByStatus(SagaStatus.FAILED)
            .stream()
            .map(SagaInstanceDto::from)
            .collect(Collectors.toList());
    }
}
```

**Idempotency for Saga Steps**:

```java
@Component
public class IdempotentEventHandler {
    
    private final ProcessedEventRepository processedEventRepository;
    
    @EventListener
    @Transactional
    public void handle(OrderSubmittedEvent event) {
        // Check if already processed
        if (isEventProcessed(event.getEventId())) {
            logger.info("Event {} already processed, skipping", 
                event.getEventId());
            return;
        }
        
        try {
            // Process event
            processOrderSubmission(event);
            
            // Mark as processed
            markEventAsProcessed(event.getEventId());
            
        } catch (Exception e) {
            logger.error("Failed to process event {}", 
                event.getEventId(), e);
            throw e;
        }
    }
    
    private boolean isEventProcessed(UUID eventId) {
        return processedEventRepository.existsByEventId(eventId);
    }
    
    private void markEventAsProcessed(UUID eventId) {
        ProcessedEvent processed = new ProcessedEvent(
            eventId,
            LocalDateTime.now()
        );
        processedEventRepository.save(processed);
    }
}
```

**Saga Timeout Handling**:

```java
@Component
@Scheduled(fixedDelay = 60000) // Every minute
public class SagaTimeoutMonitor {
    
    private final SagaInstanceRepository repository;
    private final SagaCompensationService compensationService;
    
    public void checkTimeouts() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
        
        List<SagaInstance> timedOutSagas = repository
            .findByStatusAndStartedAtBefore(
                SagaStatus.IN_PROGRESS,
                timeout
            );
        
        for (SagaInstance saga : timedOutSagas) {
            logger.warn("Saga {} timed out after 30 minutes", 
                saga.getSagaId());
            
            // Trigger compensation
            compensationService.compensate(saga);
        }
    }
}
```

**Pros**:

- ✅ Decentralized, no single point of failure
- ✅ Services remain loosely coupled
- ✅ Natural fit for event-driven architecture
- ✅ Easy to add new participants
- ✅ Scales well
- ✅ Simple to understand

**Cons**:

- ⚠️ Difficult to track overall workflow
- ⚠️ Complex to debug
- ⚠️ Cyclic dependencies possible
- ⚠️ Requires careful event design

**Cost**: $60,000 implementation + $5,000/year operational

**Risk**: **Low** - Fits existing architecture

### Option 2: Orchestration-Based Saga

**Description**: Central orchestrator coordinates saga execution

**Pros**:

- ✅ Centralized workflow logic
- ✅ Easy to track progress
- ✅ Simpler debugging
- ✅ Clear compensation flow

**Cons**:

- ❌ Single point of failure
- ❌ Tight coupling to orchestrator
- ❌ More complex infrastructure
- ❌ Orchestrator becomes bottleneck

**Cost**: $90,000 implementation + $10,000/year

**Risk**: **Medium** - Additional complexity

### Option 3: Two-Phase Commit (2PC)

**Description**: Distributed transaction protocol

**Pros**:

- ✅ Strong consistency
- ✅ ACID guarantees

**Cons**:

- ❌ Blocking protocol
- ❌ Poor performance
- ❌ Scalability issues
- ❌ Single point of failure (coordinator)
- ❌ Not suitable for microservices

**Cost**: $40,000 implementation

**Risk**: **High** - Not recommended for microservices

## Decision Outcome

**Chosen Option**: **Choreography-Based Saga (Option 1)**

### Rationale

Choreography-based saga fits naturally with our existing event-driven architecture, maintains loose coupling between services, and provides the flexibility and scalability needed for our distributed system.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New pattern to learn | Training, examples, documentation |
| QA Team | Medium | New testing approaches | Test frameworks, saga testing tools |
| Operations Team | Medium | New monitoring needs | Dashboards, alerts, runbooks |
| Business Team | Low | Better process visibility | Monitoring dashboards |

### Impact Radius Assessment

**Selected Impact Radius**: **System**

Affects:

- All bounded contexts
- Event-driven architecture
- Business workflows
- Monitoring systems
- Testing approach

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Complex debugging | Medium | Medium | Saga tracking, correlation IDs |
| Event ordering issues | Low | High | Idempotency, event versioning |
| Compensation failures | Low | High | Retry mechanism, manual intervention |
| Saga timeouts | Medium | Medium | Timeout monitoring, alerts |
| Cyclic dependencies | Low | Medium | Careful event design, reviews |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Framework Setup (Week 1-2)

**Tasks**:

- [ ] Create saga tracking infrastructure
- [ ] Implement idempotency framework
- [ ] Set up monitoring dashboards
- [ ] Create saga testing utilities
- [ ] Document patterns

**Success Criteria**:

- Framework operational
- Monitoring working
- Documentation complete

### Phase 2: Pilot Saga (Week 3-4)

**Tasks**:

- [ ] Implement Order Submission saga
- [ ] Add compensation logic
- [ ] Test failure scenarios
- [ ] Monitor in staging
- [ ] Gather feedback

**Success Criteria**:

- Saga working end-to-end
- Compensation tested
- Team comfortable

### Phase 3: Additional Sagas (Week 5-6)

**Tasks**:

- [ ] Implement Order Cancellation saga
- [ ] Implement Customer Registration saga
- [ ] Add monitoring for all sagas
- [ ] Update documentation
- [ ] Train team

**Success Criteria**:

- All critical sagas implemented
- Monitoring comprehensive
- Team trained

### Phase 4: Production Rollout (Week 7-8)

**Tasks**:

- [ ] Deploy to production
- [ ] Monitor saga execution
- [ ] Handle edge cases
- [ ] Optimize performance
- [ ] Document lessons learned

**Success Criteria**:

- Production stable
- Sagas executing reliably
- Team confident

### Rollback Strategy

**Trigger Conditions**:

- Unacceptable failure rate
- Performance issues
- Team cannot maintain

**Rollback Steps**:

1. Disable saga pattern
2. Use synchronous calls
3. Fix issues
4. Re-enable sagas

**Rollback Time**: 1 day

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Saga Success Rate | > 99% | Saga tracking |
| Compensation Success Rate | > 99.9% | Saga tracking |
| Average Saga Duration | < 5s | Monitoring |
| Failed Saga Detection | < 1 minute | Alerts |
| Manual Intervention Rate | < 0.1% | Operations metrics |

### Review Schedule

- **Weekly**: Saga metrics review
- **Monthly**: Pattern optimization
- **Quarterly**: Strategy review

## Consequences

### Positive Consequences

- ✅ **Consistency**: Eventual consistency across services
- ✅ **Reliability**: Workflows complete or compensate
- ✅ **Observability**: Track workflow progress
- ✅ **Scalability**: Non-blocking, asynchronous
- ✅ **Flexibility**: Easy to add new workflows
- ✅ **Loose Coupling**: Services remain independent

### Negative Consequences

- ⚠️ **Complexity**: More complex than synchronous calls
- ⚠️ **Debugging**: Harder to debug distributed workflows
- ⚠️ **Testing**: More complex testing scenarios
- ⚠️ **Eventual Consistency**: Not immediate consistency

### Technical Debt

**Identified Debt**:

1. Manual saga timeout handling
2. Limited saga visualization
3. Basic compensation testing
4. No saga replay capability

**Debt Repayment Plan**:

- **Q2 2026**: Automated timeout handling
- **Q3 2026**: Saga visualization dashboard
- **Q4 2026**: Comprehensive compensation testing
- **Q1 2027**: Saga replay for debugging

## Related Decisions

- [ADR-020: Database Migration Strategy with Flyway](020-database-migration-strategy-flyway.md)
- [ADR-021: Event Sourcing for Critical Aggregates](021-event-sourcing-critical-aggregates.md)
- [ADR-026: CQRS Pattern for Read/Write Separation](026-cqrs-pattern-read-write-separation.md)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)

## Notes

### Saga Design Best Practices

**DO**:

- ✅ Design idempotent operations
- ✅ Use correlation IDs
- ✅ Implement compensation logic
- ✅ Monitor saga execution
- ✅ Handle timeouts
- ✅ Test failure scenarios

**DON'T**:

- ❌ Create cyclic dependencies
- ❌ Ignore idempotency
- ❌ Skip compensation logic
- ❌ Forget timeout handling
- ❌ Ignore monitoring

### Common Saga Patterns

**Order Processing**:

1. Create Order → Reserve Inventory → Process Payment → Confirm Order
2. Compensation: Release Inventory ← Refund Payment ← Cancel Order

**Order Cancellation**:

1. Cancel Order → Refund Payment → Release Inventory → Notify Customer
2. Compensation: Restore Order ← Reverse Refund

**Customer Registration**:

1. Create Customer → Send Email → Create Loyalty Account → Send Welcome Gift
2. Compensation: Delete Customer ← Delete Loyalty Account
