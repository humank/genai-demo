---
adr_number: 026
title: "CQRS Pattern for Read/Write Separation"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [020, 021, 025]
affected_viewpoints: ["information", "functional", "performance"]
affected_perspectives: ["performance", "scalability", "evolution"]
---

# ADR-026: CQRS Pattern for Read/Write Separation

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires optimized read and write operations with different characteristics:

**Business Requirements**:

- **Read Performance**: Fast product searches and catalog browsing
- **Write Consistency**: Strong consistency for orders and payments
- **Scalability**: Scale reads independently from writes
- **Complex Queries**: Support complex reporting and analytics
- **Real-time Updates**: Near real-time data for customer-facing features
- **Flexibility**: Different data models for reads and writes

**Technical Challenges**:

- Read-heavy workload (90% reads, 10% writes)
- Complex queries spanning multiple aggregates
- Different performance requirements for reads vs writes
- Need for denormalized data for queries
- Reporting requirements
- Search functionality

**Current Issues**:

- Single model for reads and writes
- Complex queries impact write performance
- Difficult to optimize for both use cases
- Denormalization creates data duplication
- Reporting queries slow down transactional operations

### Business Context

**Business Drivers**:

- Improve customer experience (fast searches)
- Support business intelligence and reporting
- Enable real-time dashboards
- Scale for growth
- Reduce infrastructure costs

**Constraints**:

- Budget: $70,000 for implementation
- Timeline: 3 months
- Team: 3 senior developers
- Must maintain data consistency
- Cannot impact current operations
- Gradual adoption strategy

### Technical Context

**Current Approach**:

- Single database model
- JPA entities for all operations
- Complex queries on write model
- Performance bottlenecks

**Target Approach**:

- Separate read and write models
- Optimized read models for queries
- Event-driven synchronization
- Independent scaling

## Decision Drivers

1. **Performance**: Optimize reads and writes independently
2. **Scalability**: Scale reads separately from writes
3. **Flexibility**: Different models for different needs
4. **Complexity**: Manage additional complexity
5. **Consistency**: Maintain acceptable consistency
6. **Maintainability**: Keep system maintainable
7. **Cost**: Optimize infrastructure costs
8. **Team Skills**: Team capability to implement

## Considered Options

### Option 1: CQRS with Event-Driven Synchronization (Recommended)

**Description**: Separate read and write models, synchronized through domain events

**Architecture Overview**:

```text
Write Side (Commands)          Read Side (Queries)
┌─────────────────┐           ┌─────────────────┐
│  Command API    │           │   Query API     │
└────────┬────────┘           └────────┬────────┘
         │                             │
         ▼                             ▼
┌─────────────────┐           ┌─────────────────┐
│ Domain Model    │           │  Read Models    │
│ (Aggregates)    │           │ (Projections)   │
└────────┬────────┘           └────────┬────────┘
         │                             │
         ▼                             ▼
┌─────────────────┐           ┌─────────────────┐
│  Write DB       │           │   Read DB       │
│  (PostgreSQL)   │           │ (PostgreSQL +   │
│                 │           │  ElasticSearch) │
└────────┬────────┘           └─────────────────┘
         │
         │ Domain Events
         └──────────────────────────────►
```

**Write Model (Command Side)**:

```java
// Command
public record CreateOrderCommand(
    OrderId orderId,
    CustomerId customerId,
    List<OrderItemDto> items,
    ShippingAddress shippingAddress
) {}

// Command Handler
@Service
@Transactional
public class OrderCommandService {
    
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;
    
    public void createOrder(CreateOrderCommand command) {
        // Create aggregate
        Order order = new Order(
            command.orderId(),
            command.customerId(),
            command.items(),
            command.shippingAddress()
        );
        
        // Save to write database
        orderRepository.save(order);
        
        // Publish events for read model synchronization
        eventPublisher.publishEventsFromAggregate(order);
    }
    
    public void submitOrder(SubmitOrderCommand command) {
        Order order = orderRepository.findById(command.orderId())
            .orElseThrow();
        
        order.submit();
        orderRepository.save(order);
        
        eventPublisher.publishEventsFromAggregate(order);
    }
}

// Write Model (Domain Aggregate)
@Entity
@Table(name = "orders")
public class Order extends AggregateRoot {
    
    @Id
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items;
    
    @Embedded
    private ShippingAddress shippingAddress;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    // Business methods that collect events
    public void submit() {
        validateSubmission();
        status = OrderStatus.PENDING;
        collectEvent(OrderSubmittedEvent.create(orderId, customerId, totalAmount));
    }
}
```

**Read Model (Query Side)**:

```java
// Query
public record GetOrderQuery(OrderId orderId) {}

public record SearchOrdersQuery(
    CustomerId customerId,
    OrderStatus status,
    LocalDate startDate,
    LocalDate endDate,
    Pageable pageable
) {}

// Query Handler
@Service
@Transactional(readOnly = true)
public class OrderQueryService {
    
    private final OrderReadModelRepository readModelRepository;
    
    public OrderReadModel getOrder(GetOrderQuery query) {
        return readModelRepository.findById(query.orderId().getValue())
            .orElseThrow(() -> new OrderNotFoundException(query.orderId()));
    }
    
    public Page<OrderSummaryReadModel> searchOrders(SearchOrdersQuery query) {
        return readModelRepository.findByCustomerIdAndStatusAndDateRange(
            query.customerId().getValue(),
            query.status(),
            query.startDate(),
            query.endDate(),
            query.pageable()
        );
    }
}

// Read Model (Denormalized for Queries)
@Entity
@Table(name = "order_read_model")
public class OrderReadModel {
    
    @Id
    private String orderId;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "customer_name")
    private String customerName;  // Denormalized
    
    @Column(name = "customer_email")
    private String customerEmail;  // Denormalized
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "item_count")
    private Integer itemCount;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "shipping_address")
    private String shippingAddress;  // Denormalized as text
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    // Optimized for queries - no business logic
}

// Optimized repository for read model
@Repository
public interface OrderReadModelRepository 
    extends JpaRepository<OrderReadModel, String> {
    
    @Query("""
        SELECT o FROM OrderReadModel o
        WHERE o.customerId = :customerId
          AND (:status IS NULL OR o.status = :status)
          AND o.createdAt BETWEEN :startDate AND :endDate
        ORDER BY o.createdAt DESC
        """)
    Page<OrderReadModel> findByCustomerIdAndStatusAndDateRange(
        @Param("customerId") String customerId,
        @Param("status") OrderStatus status,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
    
    @Query("""
        SELECT new OrderSummaryReadModel(
            o.orderId,
            o.customerName,
            o.status,
            o.totalAmount,
            o.createdAt
        )
        FROM OrderReadModel o
        WHERE o.status = :status
        ORDER BY o.createdAt DESC
        """)
    List<OrderSummaryReadModel> findRecentOrdersByStatus(
        @Param("status") OrderStatus status,
        Pageable pageable
    );
}
```

**Event-Driven Synchronization**:

```java
// Projection Builder (Updates Read Model)
@Component
public class OrderReadModelProjection {
    
    private final OrderReadModelRepository readModelRepository;
    private final CustomerQueryService customerQueryService;
    
    @EventListener
    @Transactional
    public void on(OrderCreatedEvent event) {
        // Get customer details for denormalization
        CustomerReadModel customer = customerQueryService
            .getCustomer(event.customerId());
        
        // Create read model
        OrderReadModel readModel = new OrderReadModel();
        readModel.setOrderId(event.orderId().getValue());
        readModel.setCustomerId(event.customerId().getValue());
        readModel.setCustomerName(customer.getName());
        readModel.setCustomerEmail(customer.getEmail());
        readModel.setStatus(OrderStatus.DRAFT);
        readModel.setItemCount(event.items().size());
        readModel.setTotalAmount(event.totalAmount());
        readModel.setShippingAddress(formatAddress(event.shippingAddress()));
        readModel.setCreatedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(OrderSubmittedEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.PENDING);
        readModel.setSubmittedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(OrderCompletedEvent event) {
        OrderReadModel readModel = readModelRepository
            .findById(event.orderId().getValue())
            .orElseThrow();
        
        readModel.setStatus(OrderStatus.COMPLETED);
        readModel.setCompletedAt(event.occurredOn());
        
        readModelRepository.save(readModel);
    }
    
    @EventListener
    @Transactional
    public void on(CustomerUpdatedEvent event) {
        // Update denormalized customer data in all orders
        List<OrderReadModel> orders = readModelRepository
            .findByCustomerId(event.customerId().getValue());
        
        for (OrderReadModel order : orders) {
            order.setCustomerName(event.name());
            order.setCustomerEmail(event.email());
        }
        
        readModelRepository.saveAll(orders);
    }
}
```

**API Separation**:

```java
// Command API (Write Operations)
@RestController
@RequestMapping("/api/v1/commands/orders")
public class OrderCommandController {
    
    private final OrderCommandService commandService;
    
    @PostMapping
    public ResponseEntity<OrderCreatedResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        
        CreateOrderCommand command = toCommand(request);
        commandService.createOrder(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new OrderCreatedResponse(command.orderId()));
    }
    
    @PostMapping("/{orderId}/submit")
    public ResponseEntity<Void> submitOrder(@PathVariable String orderId) {
        SubmitOrderCommand command = new SubmitOrderCommand(
            OrderId.of(orderId)
        );
        commandService.submitOrder(command);
        
        return ResponseEntity.ok().build();
    }
}

// Query API (Read Operations)
@RestController
@RequestMapping("/api/v1/queries/orders")
public class OrderQueryController {
    
    private final OrderQueryService queryService;
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderReadModel> getOrder(
            @PathVariable String orderId) {
        
        GetOrderQuery query = new GetOrderQuery(OrderId.of(orderId));
        OrderReadModel order = queryService.getOrder(query);
        
        return ResponseEntity.ok(order);
    }
    
    @GetMapping
    public ResponseEntity<Page<OrderSummaryReadModel>> searchOrders(
            @RequestParam String customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Pageable pageable) {
        
        SearchOrdersQuery query = new SearchOrdersQuery(
            CustomerId.of(customerId),
            status,
            startDate,
            endDate,
            pageable
        );
        
        Page<OrderSummaryReadModel> orders = queryService.searchOrders(query);
        
        return ResponseEntity.ok(orders);
    }
}
```

**ElasticSearch for Advanced Queries**:

```java
// Product search read model in ElasticSearch
@Document(indexName = "products")
public class ProductSearchModel {
    
    @Id
    private String productId;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;
    
    @Field(type = FieldType.Double)
    private Double averageRating;
    
    @Field(type = FieldType.Integer)
    private Integer reviewCount;
}

// ElasticSearch repository
@Repository
public interface ProductSearchRepository 
    extends ElasticsearchRepository<ProductSearchModel, String> {
    
    @Query("""
        {
          "bool": {
            "must": [
              {
                "multi_match": {
                  "query": "?0",
                  "fields": ["name^2", "description", "tags"]
                }
              }
            ],
            "filter": [
              { "term": { "category": "?1" } },
              { "range": { "price": { "gte": ?2, "lte": ?3 } } },
              { "range": { "stockQuantity": { "gt": 0 } } }
            ]
          }
        }
        """)
    Page<ProductSearchModel> searchProducts(
        String searchText,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Pageable pageable
    );
}

// Product search projection
@Component
public class ProductSearchProjection {
    
    private final ProductSearchRepository searchRepository;
    
    @EventListener
    @Transactional
    public void on(ProductCreatedEvent event) {
        ProductSearchModel searchModel = new ProductSearchModel();
        searchModel.setProductId(event.productId().getValue());
        searchModel.setName(event.name());
        searchModel.setDescription(event.description());
        searchModel.setCategory(event.category());
        searchModel.setTags(event.tags());
        searchModel.setPrice(event.price());
        searchModel.setStockQuantity(event.initialStock());
        searchModel.setAverageRating(0.0);
        searchModel.setReviewCount(0);
        
        searchRepository.save(searchModel);
    }
    
    @EventListener
    @Transactional
    public void on(ProductReviewedEvent event) {
        ProductSearchModel searchModel = searchRepository
            .findById(event.productId().getValue())
            .orElseThrow();
        
        // Update rating
        searchModel.setAverageRating(event.newAverageRating());
        searchModel.setReviewCount(event.totalReviews());
        
        searchRepository.save(searchModel);
    }
}
```

**Eventual Consistency Handling**:

```java
// Read model consistency checker
@Component
public class ReadModelConsistencyChecker {
    
    private final OrderReadModelRepository readModelRepository;
    private final OrderRepository writeRepository;
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkConsistency() {
        // Find orders with mismatched status
        List<String> inconsistentOrders = findInconsistentOrders();
        
        if (!inconsistentOrders.isEmpty()) {
            logger.warn("Found {} inconsistent orders", 
                inconsistentOrders.size());
            
            // Trigger rebuild for inconsistent orders
            for (String orderId : inconsistentOrders) {
                rebuildReadModel(orderId);
            }
        }
    }
    
    private void rebuildReadModel(String orderId) {
        // Load from write model
        Order order = writeRepository.findById(OrderId.of(orderId))
            .orElseThrow();
        
        // Rebuild read model
        OrderReadModel readModel = buildReadModelFromAggregate(order);
        readModelRepository.save(readModel);
        
        logger.info("Rebuilt read model for order {}", orderId);
    }
}
```

**Monitoring and Metrics**:

```java
@Component
public class CQRSMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void onCommandExecuted(CommandExecutedEvent event) {
        Counter.builder("cqrs.commands.executed")
            .tag("command", event.getCommandType())
            .tag("status", event.getStatus())
            .register(meterRegistry)
            .increment();
        
        Timer.builder("cqrs.commands.duration")
            .tag("command", event.getCommandType())
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void onQueryExecuted(QueryExecutedEvent event) {
        Counter.builder("cqrs.queries.executed")
            .tag("query", event.getQueryType())
            .register(meterRegistry)
            .increment();
        
        Timer.builder("cqrs.queries.duration")
            .tag("query", event.getQueryType())
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
    }
    
    @EventListener
    public void onProjectionUpdated(ProjectionUpdatedEvent event) {
        Counter.builder("cqrs.projections.updated")
            .tag("projection", event.getProjectionType())
            .register(meterRegistry)
            .increment();
        
        Gauge.builder("cqrs.projections.lag")
            .tag("projection", event.getProjectionType())
            .register(meterRegistry, event, e -> e.getLagMillis());
    }
}
```

**Pros**:

- ✅ Optimized read and write performance
- ✅ Independent scaling
- ✅ Flexible data models
- ✅ Complex queries without impacting writes
- ✅ Natural fit for event-driven architecture
- ✅ Supports multiple read models (SQL + ElasticSearch)

**Cons**:

- ⚠️ Eventual consistency
- ⚠️ Increased complexity
- ⚠️ Data duplication
- ⚠️ Synchronization overhead
- ⚠️ More infrastructure to manage

**Cost**: $70,000 implementation + $8,000/year operational

**Risk**: **Medium** - Complexity and consistency challenges

### Option 2: Simple Read Replicas

**Description**: Use database read replicas for read scaling

**Pros**:

- ✅ Simple to implement
- ✅ Strong consistency
- ✅ No code changes

**Cons**:

- ❌ Same data model for reads and writes
- ❌ Limited optimization
- ❌ Replication lag
- ❌ Cannot use different databases

**Cost**: $20,000 implementation + $15,000/year (replicas)

**Risk**: **Low** - But limited benefits

### Option 3: Materialized Views

**Description**: Use database materialized views for complex queries

**Pros**:

- ✅ Database-level optimization
- ✅ No application changes
- ✅ Familiar to DBAs

**Cons**:

- ❌ Database-specific
- ❌ Limited flexibility
- ❌ Refresh overhead
- ❌ Cannot use different databases

**Cost**: $15,000 implementation

**Risk**: **Low** - But limited scalability

## Decision Outcome

**Chosen Option**: **CQRS with Event-Driven Synchronization (Option 1)**

### Rationale

CQRS provides the flexibility and performance optimization needed for our read-heavy workload, allowing independent scaling and optimization of reads and writes while fitting naturally with our event-driven architecture.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New pattern to learn | Training, examples, documentation |
| Database Team | Medium | New data models | Training, monitoring tools |
| QA Team | Medium | New testing approaches | Test frameworks, consistency checks |
| Operations Team | Medium | More infrastructure | Monitoring, automation |
| API Consumers | Low | Separate endpoints | Clear documentation |

### Impact Radius Assessment

**Selected Impact Radius**: **System**

Affects:

- All bounded contexts
- API design
- Database architecture
- Event-driven architecture
- Monitoring systems

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Eventual consistency issues | Medium | Medium | Consistency checks, monitoring |
| Projection lag | Low | Medium | Performance optimization, alerts |
| Data synchronization failures | Low | High | Retry mechanism, manual tools |
| Increased complexity | High | Medium | Training, documentation, tooling |
| Storage costs | Low | Low | Optimize projections, archiving |

**Overall Risk Level**: **Medium**

## Implementation Plan

### Phase 1: Framework Setup (Month 1)

**Tasks**:

- [ ] Design CQRS infrastructure
- [ ] Implement projection framework
- [ ] Set up monitoring
- [ ] Create testing utilities
- [ ] Document patterns

**Success Criteria**:

- Framework operational
- Monitoring working
- Documentation complete

### Phase 2: Pilot Implementation (Month 2)

**Tasks**:

- [ ] Implement Order CQRS
- [ ] Create read models
- [ ] Build projections
- [ ] Test in staging
- [ ] Gather feedback

**Success Criteria**:

- Order CQRS working
- Performance improved
- Team comfortable

### Phase 3: Expansion (Month 3)

**Tasks**:

- [ ] Implement Product CQRS with ElasticSearch
- [ ] Implement Customer CQRS
- [ ] Add consistency checks
- [ ] Optimize performance
- [ ] Update documentation

**Success Criteria**:

- All critical entities using CQRS
- Performance targets met
- Team trained

### Rollback Strategy

**Trigger Conditions**:

- Unacceptable consistency issues
- Performance degradation
- Team cannot maintain

**Rollback Steps**:

1. Disable read models
2. Use write model for queries
3. Fix issues
4. Re-enable CQRS

**Rollback Time**: 1 day

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Read Query Performance | < 100ms p95 | Application metrics |
| Write Performance | < 200ms p95 | Application metrics |
| Projection Lag | < 1s | Monitoring |
| Consistency Rate | > 99.9% | Consistency checks |
| Read Scalability | 10x improvement | Load testing |

### Review Schedule

- **Weekly**: Performance metrics review
- **Monthly**: Pattern optimization
- **Quarterly**: Strategy review

## Consequences

### Positive Consequences

- ✅ **Performance**: Optimized reads and writes
- ✅ **Scalability**: Independent scaling
- ✅ **Flexibility**: Different models for different needs
- ✅ **Complex Queries**: Support without impacting writes
- ✅ **Multiple Stores**: SQL + ElasticSearch
- ✅ **Cost Optimization**: Scale only what's needed

### Negative Consequences

- ⚠️ **Complexity**: Significant increase
- ⚠️ **Eventual Consistency**: Not immediate
- ⚠️ **Data Duplication**: More storage
- ⚠️ **Synchronization**: Overhead and potential failures
- ⚠️ **Testing**: More complex scenarios

### Technical Debt

**Identified Debt**:

1. Manual projection rebuild
2. Limited consistency monitoring
3. Basic error handling
4. No projection versioning

**Debt Repayment Plan**:

- **Q2 2026**: Automated projection rebuild
- **Q3 2026**: Comprehensive consistency monitoring
- **Q4 2026**: Advanced error handling
- **Q1 2027**: Projection versioning

## Related Decisions

- [ADR-020: Database Migration Strategy with Flyway](020-database-migration-strategy-flyway.md)
- [ADR-021: Event Sourcing for Critical Aggregates](021-event-sourcing-critical-aggregates.md)
- [ADR-025: Saga Pattern for Distributed Transactions](025-saga-pattern-distributed-transactions.md)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)

## Notes

### CQRS Best Practices

**DO**:

- ✅ Use for read-heavy workloads
- ✅ Optimize read models for queries
- ✅ Monitor projection lag
- ✅ Implement consistency checks
- ✅ Use different databases when beneficial
- ✅ Keep write model normalized

**DON'T**:

- ❌ Apply to all entities
- ❌ Ignore eventual consistency
- ❌ Over-denormalize
- ❌ Skip monitoring
- ❌ Forget about projection failures

### When to Use CQRS

**Good Candidates**:

- ✅ Read-heavy entities (products, catalog)
- ✅ Complex query requirements
- ✅ Different scaling needs
- ✅ Multiple read models needed

**Poor Candidates**:

- ❌ Simple CRUD entities
- ❌ Low traffic entities
- ❌ Strong consistency required
- ❌ Simple queries only
