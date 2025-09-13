# ADR-002: Bounded Context Design Strategy

## Status

**Accepted** - 2024-01-15

## Context

The GenAI Demo e-commerce platform requires clear domain boundaries to manage complexity and enable team collaboration. Based on user story analysis and business requirements, we need to identify and design appropriate bounded contexts that reflect the business domain structure.

### Business Requirements Analysis

From the BDD feature files and user stories, we identified the following key business capabilities:

#### Customer Management

- **User Story**: "As a customer, I want to manage my profile and membership levels, so that I can receive personalized benefits"
- **Key Scenarios**: Registration, profile updates, membership upgrades, loyalty points management

#### Order Processing  

- **User Story**: "As a customer, I want to place and track orders, so that I can purchase products efficiently"
- **Key Scenarios**: Order creation, item management, status tracking, cancellation

#### Product Catalog

- **User Story**: "As a customer, I want to browse products and see detailed information, so that I can make informed purchase decisions"
- **Key Scenarios**: Product browsing, search, filtering, member-exclusive products

#### Inventory Management

- **User Story**: "As a system, I need to manage stock levels and reservations, so that overselling is prevented"
- **Key Scenarios**: Stock checking, reservation, release, replenishment

### Technical Constraints

- Monolithic deployment initially with microservices evolution path
- Event-driven communication between contexts
- Shared kernel for common value objects
- Anti-corruption layers for external system integration

## Decision

We establish **10 Bounded Contexts** based on business capability analysis and domain expert interviews.

### Core Business Contexts

#### 1. Customer Context

**Responsibility**: Customer lifecycle management, membership system, loyalty programs

**Aggregate Roots**:

- `Customer`: Manages customer profile, membership level, spending history
- `CustomerPreferences`: Notification preferences, delivery preferences
- `RewardAccount`: Loyalty points, redemption history

**Key Domain Events**:

```java
CustomerCreatedEvent              // New customer registration
MembershipLevelUpgradedEvent     // Automatic level upgrade
RewardPointsEarnedEvent          // Points accumulation
CustomerSpendingUpdatedEvent     // Spending threshold tracking
```

**Business Rules**:

- Membership level based on 12-month spending history
- Automatic level upgrades when thresholds reached
- Points expiration after 24 months of inactivity
- Birthday month special discounts

#### 2. Order Context

**Responsibility**: Order lifecycle management, order processing workflow

**Aggregate Roots**:

- `Order`: Order state, items, pricing, status transitions
- `OrderWorkflow`: Process orchestration, state machine

**Key Domain Events**:

```java
OrderCreatedEvent                        // Order initialization
OrderSubmittedEvent                      // Order confirmation
OrderPaymentRequestedEvent               // Payment processing trigger
OrderInventoryReservationRequestedEvent // Inventory allocation trigger
OrderConfirmedEvent                      // Final confirmation
```

**Business Rules**:

- Order total calculation including discounts and taxes
- Status transition validation (CREATED → PENDING → CONFIRMED → SHIPPED)
- Maximum order value limits
- Cancellation rules based on order status

#### 3. Product Context

**Responsibility**: Product catalog management, product information

**Aggregate Roots**:

- `Product`: Product details, pricing, availability
- `ProductCategory`: Category hierarchy, attributes
- `ProductBundle`: Product combinations, bundle pricing

**Key Domain Events**:

```java
ProductCreatedEvent              // New product addition
ProductPriceUpdatedEvent         // Price changes
ProductAvailabilityChangedEvent  // Stock status updates
```

**Business Rules**:

- Member-exclusive product access control
- Product lifecycle management (active, discontinued)
- Category-based attribute validation

#### 4. Inventory Context

**Responsibility**: Stock management, reservation system

**Aggregate Roots**:

- `Inventory`: Stock levels, reservations, movements
- `InventoryReservation`: Temporary stock allocation

**Key Domain Events**:

```java
InventoryReservedEvent           // Stock reservation
InventoryReleasedEvent           // Reservation cancellation
InventoryReplenishedEvent        // Stock replenishment
LowStockAlertEvent              // Reorder notifications
```

**Business Rules**:

- Reservation timeout (30 minutes for unpaid orders)
- Minimum stock level alerts
- FIFO reservation processing

### Supporting Contexts

#### 5. Payment Context

**Responsibility**: Payment processing, payment method management

**Aggregate Roots**:

- `Payment`: Payment transactions, status tracking
- `PaymentMethod`: Customer payment preferences

**Key Domain Events**:

```java
PaymentProcessedEvent            // Successful payment
PaymentFailedEvent              // Payment failure
RefundProcessedEvent            // Refund completion
```

#### 6. Delivery Context

**Responsibility**: Shipping and logistics management

**Aggregate Roots**:

- `Delivery`: Shipment tracking, delivery status
- `DeliveryRoute`: Logistics optimization

**Key Domain Events**:

```java
DeliveryScheduledEvent           // Shipment creation
DeliveryInTransitEvent          // Status updates
DeliveryCompletedEvent          // Delivery confirmation
```

#### 7. Promotion Context

**Responsibility**: Marketing campaigns, discount management

**Aggregate Roots**:

- `Promotion`: Campaign rules, validity periods
- `Voucher`: Individual discount codes

**Key Domain Events**:

```java
PromotionActivatedEvent          // Campaign launch
VoucherUsedEvent                // Discount application
PromotionExpiredEvent           // Campaign end
```

#### 8. Pricing Context

**Responsibility**: Dynamic pricing, discount calculation

**Aggregate Roots**:

- `PricingRule`: Pricing strategies, discount rules
- `PriceCalculation`: Final price computation

**Key Domain Events**:

```java
PriceCalculatedEvent             // Price computation
DiscountAppliedEvent            // Discount processing
```

### Generic Contexts

#### 9. Notification Context

**Responsibility**: Multi-channel communication management

**Aggregate Roots**:

- `Notification`: Message content, delivery status
- `NotificationTemplate`: Message templates

**Key Domain Events**:

```java
NotificationSentEvent            // Message delivery
NotificationFailedEvent         // Delivery failure
```

#### 10. Workflow Context

**Responsibility**: Cross-context process orchestration

**Aggregate Roots**:

- `WorkflowInstance`: Process state, step tracking
- `WorkflowDefinition`: Process templates

**Key Domain Events**:

```java
WorkflowStartedEvent             // Process initiation
WorkflowCompletedEvent          // Process completion
WorkflowFailedEvent             // Process failure
```

## Context Relationships

### Relationship Mapping

```java
// Core relationships from BoundedContextMap.java
public class BoundedContextMap {
    private void initializeContextMap() {
        // Product provides catalog to Order
        addRelation("Product", "Order", UPSTREAM_DOWNSTREAM,
            "Product provides catalog information to Order");
            
        // Customer provides profile to Order  
        addRelation("Customer", "Order", UPSTREAM_DOWNSTREAM,
            "Customer provides customer information to Order");
            
        // Order requests services from supporting contexts
        addRelation("Order", "Payment", CUSTOMER_SUPPLIER,
            "Order requests payment processing from Payment");
        addRelation("Order", "Inventory", CUSTOMER_SUPPLIER,
            "Order requests inventory allocation from Inventory");
        addRelation("Order", "Delivery", CUSTOMER_SUPPLIER,
            "Order requests delivery from Delivery");
            
        // Promotion influences Pricing
        addRelation("Promotion", "Pricing", UPSTREAM_DOWNSTREAM,
            "Promotion provides discount rules to Pricing");
            
        // Workflow orchestrates other contexts
        addRelation("Workflow", "Order", CONFORMIST,
            "Workflow conforms to Order's domain model");
            
        // Notification uses ACL for integration
        addRelation("Notification", "Order", ANTI_CORRUPTION_LAYER,
            "Notification uses ACL to integrate with Order");
    }
}
```

### Integration Patterns

#### Event-Driven Integration

```java
// Order submission triggers cross-context collaboration
@EventHandler
public class OrderSubmittedEventHandler {
    
    @TransactionalEventListener
    public void handle(OrderSubmittedEvent event) {
        // Trigger payment processing
        paymentService.processPayment(
            PaymentRequest.from(event.orderId(), event.totalAmount())
        );
        
        // Reserve inventory
        inventoryService.reserveItems(
            ReservationRequest.from(event.orderId(), event.items())
        );
        
        // Send confirmation notification
        notificationService.sendOrderConfirmation(
            NotificationRequest.from(event.customerId(), event.orderId())
        );
    }
}
```

#### Anti-Corruption Layer Example

```java
// Notification Context ACL for Order integration
@Component
public class OrderNotificationACL {
    
    public NotificationRequest adaptOrderEvent(OrderSubmittedEvent orderEvent) {
        return NotificationRequest.builder()
            .recipientId(orderEvent.customerId().value())
            .templateType(NotificationTemplate.ORDER_CONFIRMATION)
            .parameters(Map.of(
                "orderId", orderEvent.orderId().value(),
                "totalAmount", orderEvent.totalAmount().toString()
            ))
            .build();
    }
}
```

## Consequences

### Positive Outcomes

#### Business Benefits

- **Clear Ownership**: Each context has well-defined business responsibilities
- **Domain Expertise**: Teams can develop deep knowledge in specific business areas
- **Business Alignment**: Context boundaries reflect actual business processes
- **Scalability**: Independent scaling based on business demand

#### Technical Benefits

- **Loose Coupling**: Contexts communicate through well-defined interfaces
- **Independent Development**: Teams can work on different contexts simultaneously
- **Technology Diversity**: Each context can choose optimal technology stack
- **Testing Isolation**: Context boundaries enable focused testing strategies

#### Organizational Benefits

- **Team Structure**: Context boundaries align with team responsibilities
- **Conway's Law**: Architecture reflects desired organizational structure
- **Knowledge Management**: Domain expertise concentrated in context teams
- **Career Development**: Clear specialization paths for team members

### Negative Outcomes

#### Complexity Challenges

- **Integration Overhead**: Cross-context communication requires careful design
- **Data Consistency**: Eventual consistency across contexts
- **Transaction Management**: Distributed transaction complexity
- **Debugging Difficulty**: Cross-context issue diagnosis

#### Development Challenges

- **Context Boundaries**: Requires ongoing refinement and validation
- **Shared Concepts**: Managing shared value objects and concepts
- **Event Design**: Complex event choreography across contexts
- **Performance Impact**: Network calls between contexts

### Mitigation Strategies

#### Technical Mitigations

- **Saga Pattern**: Manage distributed transactions across contexts
- **Event Sourcing**: Maintain audit trail and enable replay
- **Circuit Breakers**: Prevent cascade failures between contexts
- **Monitoring**: Comprehensive observability across context boundaries

#### Organizational Mitigations

- **Context Maps**: Regular review and update of context relationships
- **Domain Experts**: Involve business stakeholders in boundary decisions
- **Architecture Reviews**: Regular validation of context design
- **Team Rotation**: Cross-context knowledge sharing

## Validation Criteria

### Business Validation

- **User Story Mapping**: Each user story maps to single context
- **Business Process Flow**: Processes flow naturally across context boundaries
- **Domain Expert Approval**: Business stakeholders validate context responsibilities
- **Change Impact Analysis**: Business changes affect minimal contexts

### Technical Validation

- **Data Flow Analysis**: Minimal cross-context data dependencies
- **Event Flow Mapping**: Clear event choreography patterns
- **Performance Testing**: Acceptable latency across context boundaries
- **Failure Mode Analysis**: Graceful degradation when contexts unavailable

### Organizational Validation

- **Team Mapping**: Context boundaries align with team structure
- **Skill Distribution**: Teams have necessary domain expertise
- **Communication Patterns**: Minimal cross-team coordination required
- **Delivery Independence**: Teams can deliver features independently

## Evolution Strategy

### Phase 1: Modular Monolith (Current)

- All contexts in single deployable unit
- Clear module boundaries with dependency rules
- Event-driven communication within process
- Shared database with context-specific schemas

### Phase 2: Service Extraction

- Extract high-value contexts as microservices
- Implement distributed event bus (MSK)
- Separate databases per context
- API gateways for external access

### Phase 3: Full Microservices

- All contexts as independent services
- Service mesh for communication
- Distributed tracing and monitoring
- Independent deployment pipelines

## Well-Architected Framework Assessment

### Operational Excellence

- **Monitoring**: Context boundaries provide natural monitoring boundaries
- **Automation**: Independent deployment pipelines per context
- **Documentation**: Self-documenting through ubiquitous language

### Security

- **Access Control**: Context-based security boundaries
- **Data Protection**: Sensitive data isolated in appropriate contexts
- **Audit Trail**: Cross-context event flows provide audit capabilities

### Reliability

- **Fault Isolation**: Context failures don't cascade
- **Recovery**: Independent recovery procedures per context
- **Backup**: Context-specific backup strategies

### Performance Efficiency

- **Caching**: Context-specific caching strategies
- **Scaling**: Independent scaling based on context demand
- **Resource Optimization**: Right-sized resources per context

### Cost Optimization

- **Resource Allocation**: Pay only for resources each context needs
- **Development Efficiency**: Reduced coordination overhead
- **Operational Costs**: Simplified operations within context boundaries

## Related Decisions

- [ADR-001: DDD + Hexagonal Architecture Foundation](./ADR-001-ddd-hexagonal-architecture.md)
- [ADR-003: Domain Events and CQRS Implementation](./ADR-003-domain-events-cqrs.md)
- [ADR-009: MSK vs EventBridge for Event Streaming](./ADR-009-event-streaming-platform.md)

## References

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.domainlanguage.com/ddd/)
- [Implementing Domain-Driven Design](https://www.informit.com/store/implementing-domain-driven-design-9780321834577)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Building Microservices](https://samnewman.io/books/building_microservices/)
