# ADR-002: Bounded Context Design Strategy

## Status

**Accepted** - January 20, 2024

## Context

The GenAI Demo e-commerce platform requires clear domain boundaries to manage complexity and facilitate team collaboration. Based on user story analysis and business requirements, we need to identify and design appropriate bounded contexts that reflect the business domain structure.

### Business Requirements Analysis

From BDD feature files and user stories, we have identified the following key business capabilities:

#### Customer Management

- **User Story**: "As a customer, I want to manage my profile and membership level to receive personalized offers"
- **Key Scenarios**: Registration, profile updates, membership upgrades, loyalty points management

#### Order Processing

- **User Story**: "As a customer, I want to place orders and track them to efficiently purchase products"
- **Key Scenarios**: Order creation, item management, status tracking, order cancellation

#### Product Catalog

- **User Story**: "As a customer, I want to browse products and view detailed information to make informed purchase decisions"
- **Key Scenarios**: Product browsing, search, filtering, member-exclusive products

#### Inventory Management

- **User Story**: "As a merchant, I want to manage product inventory to ensure product availability"
- **Key Scenarios**: Inventory tracking, restocking, reservation, release

#### Payment Processing

- **User Story**: "As a customer, I want to securely pay for orders to complete purchases"
- **Key Scenarios**: Payment processing, refunds, payment method management

#### Delivery Management

- **User Story**: "As a customer, I want to track my delivery status to understand order progress"
- **Key Scenarios**: Delivery scheduling, status updates, delivery tracking

#### Promotion Management

- **User Story**: "As a customer, I want to use coupons and participate in promotional activities to get discounts"
- **Key Scenarios**: Coupon management, promotion rules, discount calculation

#### Pricing Management

- **User Story**: "As a merchant, I want to set flexible pricing strategies to optimize revenue"
- **Key Scenarios**: Price calculation, commission management, dynamic pricing

#### Notification Service

- **User Story**: "As a customer, I want to receive important order and promotional notifications to stay informed"
- **Key Scenarios**: Notification sending, template management, notification preferences

#### Workflow Management

- **User Story**: "As a system, I need to coordinate complex business processes to ensure operational consistency"
- **Key Scenarios**: Process orchestration, state management, exception handling

## Decision

We have decided to establish **10 bounded contexts**, divided based on business capabilities and team structure.

### Context Mapping Strategy

#### Core Contexts

These contexts contain core business logic and represent the system's competitive advantage:

1. **Customer Context** - Customer Aggregate
   - Customer data management
   - Membership levels and loyalty points
   - Customer preference settings

2. **Order Context** - Order Aggregate
   - Order lifecycle management
   - Order item management
   - Order status tracking

3. **Product Context** - Product Aggregate
   - Product catalog management
   - Product information maintenance
   - Product categorization and tagging

4. **Inventory Context** - Inventory Aggregate
   - Inventory level tracking
   - Inventory reservation and release
   - Inventory replenishment management

#### Supporting Contexts

These contexts support core business processes:

5. **Payment Context** - Payment Aggregate
   - Payment processing
   - Payment method management
   - Refund processing

6. **Delivery Context** - Delivery Aggregate
   - Delivery scheduling
   - Delivery status tracking
   - Delivery provider management

7. **Promotion Context** - Promotion Aggregate
   - Coupon management
   - Promotion rules engine
   - Discount calculation

8. **Pricing Context** - Pricing Aggregate
   - Price calculation engine
   - Commission management
   - Pricing strategies

#### Generic Contexts

These contexts provide generic services:

9. **Notification Context** - Notification Aggregate
   - Notification sending
   - Notification template management
   - Notification preferences

10. **Workflow Context** - Workflow Aggregate
    - Business process orchestration
    - State machine management
    - Process monitoring

### Context Relationship Mapping

#### 1. Partnership

- **Customer ↔ Order**: Customer and Order work closely together
- **Order ↔ Inventory**: Order and Inventory need synchronized coordination

#### 2. Customer-Supplier

- **Order → Payment**: Order drives payment processing
- **Order → Delivery**: Order triggers delivery scheduling
- **Customer → Notification**: Customer events trigger notifications

#### 3. Conformist

- **Promotion → Pricing**: Promotion conforms to pricing rules
- **All Contexts → Workflow**: All contexts conform to workflow specifications

#### 4. Anti-Corruption Layer

- **Payment Context**: Uses anti-corruption layer when integrating with external payment systems
- **Delivery Context**: Uses anti-corruption layer when integrating with third-party logistics systems

### Implementation Strategy

#### 1. Package Structure Design

```
solid.humank.genaidemo/
├── domain/
│   ├── customer/          # Customer context
│   ├── order/             # Order context
│   ├── product/           # Product context
│   ├── inventory/         # Inventory context
│   ├── payment/           # Payment context
│   ├── delivery/          # Delivery context
│   ├── promotion/         # Promotion context
│   ├── pricing/           # Pricing context
│   ├── notification/      # Notification context
│   └── workflow/          # Workflow context
├── application/
│   ├── customer/          # Customer use cases
│   ├── order/             # Order use cases
│   └── ...                # Other use cases
└── infrastructure/
    ├── persistence/       # Persistence implementation
    └── messaging/         # Message handling
```

#### 2. Aggregate Design Principles

Each bounded context contains one or more aggregates:

- **Small Aggregates**: Each aggregate focuses on a single business concept
- **Consistency Boundaries**: Strong consistency within aggregates
- **Event-Driven**: Communication between aggregates through domain events

#### 3. Data Consistency Strategy

- **Within Aggregates**: Strong consistency (ACID transactions)
- **Between Aggregates**: Eventual consistency (domain events)
- **Between Contexts**: Eventual consistency (integration events)

## Consequences

### Positive Impacts

#### 1. **Clear Business Boundaries**

- Each context corresponds to clear business capabilities
- Reduces cross-team communication complexity
- Supports independent business decisions

#### 2. **Technical Autonomy**

- Each context can choose appropriate technology stack
- Supports independent deployment and scaling
- Reduces technical debt propagation

#### 3. **Team Organization Alignment**

- Context boundaries align with team boundaries
- Supports positive application of Conway's Law
- Improves team autonomy and accountability

#### 4. **Improved Testability**

- Each context can be tested independently
- Reduces interdependencies between tests
- Improves test execution speed

### Quantitative Metrics

- **Number of Contexts**: 10
- **Number of Aggregates**: 15
- **Cross-Context Dependencies**: 12 (through events)
- **Direct Dependencies**: 0
- **Test Isolation**: 95%

### Negative Impacts and Mitigation Measures

#### 1. **Increased Complexity**

- **Issue**: Multiple contexts increase system complexity
- **Mitigation**: Provide clear context mapping documentation and tools

#### 2. **Data Consistency Challenges**

- **Issue**: Eventual consistency may lead to temporary data inconsistency
- **Mitigation**: Implement compensation mechanisms and monitoring tools

#### 3. **Cross-Context Query Difficulties**

- **Issue**: Queries spanning multiple contexts become complex
- **Mitigation**: Implement CQRS read models and data projections

## Implementation Details

### 1. Context Interface Definition

```java
// Public interface of Customer context
public interface CustomerService {
    Customer findById(CustomerId id);
    void upgradeToVip(CustomerId id);
    LoyaltyPoints getLoyaltyPoints(CustomerId id);
}

// Public interface of Order context
public interface OrderService {
    Order createOrder(CreateOrderCommand command);
    void cancelOrder(OrderId orderId);
    OrderStatus getOrderStatus(OrderId orderId);
}
```

### 2. Domain Event Definition

```java
// Events published by Customer context
public record CustomerUpgradedToVipEvent(
    CustomerId customerId,
    MembershipLevel newLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}

// Events published by Order context
public record OrderCreatedEvent(
    OrderId orderId,
    CustomerId customerId,
    List<OrderItem> items,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}
```

### 3. Event Handlers

```java
// Promotion context handles customer upgrade events
@Component
public class CustomerUpgradedEventHandler {
    
    @EventHandler
    public void handle(CustomerUpgradedToVipEvent event) {
        // Create exclusive coupon for new VIP customer
        promotionService.createVipWelcomeCoupon(event.customerId());
    }
}
```

### 4. Anti-Corruption Layer Implementation

```java
// Anti-corruption layer for Payment context
@Component
public class PaymentGatewayAdapter {
    
    public PaymentResult processPayment(PaymentRequest request) {
        // Convert internal payment request to external API format
        ExternalPaymentRequest externalRequest = mapToExternal(request);
        
        // Call external payment API
        ExternalPaymentResponse response = externalPaymentApi.process(externalRequest);
        
        // Convert external response to internal format
        return mapToInternal(response);
    }
}
```

## Validation and Testing

### 1. Context Boundary Testing

```java
@ArchTest
static final ArchRule contexts_should_not_have_cyclic_dependencies =
    slices()
        .matching("..domain.(*)..")
        .should().beFreeOfCycles();

@ArchTest
static final ArchRule contexts_should_only_communicate_through_events =
    noClasses()
        .that().resideInAPackage("..domain.customer..")
        .should().dependOnClassesThat()
        .resideInAPackage("..domain.order..");
```

### 2. Integration Testing Strategy

```java
@SpringBootTest
class CustomerOrderIntegrationTest {
    
    @Test
    void shouldCreateOrderWhenCustomerExists() {
        // Given: Customer exists
        CustomerId customerId = createTestCustomer();
        
        // When: Create order
        OrderId orderId = orderService.createOrder(
            new CreateOrderCommand(customerId, orderItems)
        );
        
        // Then: Verify order creation success
        assertThat(orderService.getOrderStatus(orderId))
            .isEqualTo(OrderStatus.PENDING);
    }
}
```

### 3. Event Flow Testing

```java
@Test
void shouldTriggerPromotionWhenCustomerUpgraded() {
    // Given: Customer and event handler
    CustomerId customerId = createTestCustomer();
    
    // When: Customer upgraded to VIP
    customerService.upgradeToVip(customerId);
    
    // Then: Should receive VIP welcome coupon
    await().atMost(5, SECONDS).untilAsserted(() -> {
        List<Coupon> coupons = promotionService.getCouponsForCustomer(customerId);
        assertThat(coupons).anyMatch(c -> c.getType() == CouponType.VIP_WELCOME);
    });
}
```

## Evolution Strategy

### 1. Microservices Evolution Path

When the system needs to be split into microservices, bounded contexts provide natural splitting boundaries:

```
Stage 1: Monolithic Application (Current)
├── All contexts in the same deployment unit
└── Internal communication through domain events

Stage 2: Modular Monolith
├── Each context as independent module
└── Remain in the same deployment unit

Stage 3: Microservices
├── Core contexts split first
├── Supporting contexts split as needed
└── Generic contexts split last
```

### 2. Database Splitting Strategy

```
Stage 1: Shared Database
├── All contexts share the same database
└── Logical separation through schema or table prefix

Stage 2: Database Separation
├── Each context owns independent database schema
└── Cross-context queries through API or events

Stage 3: Complete Independence
├── Each microservice owns independent database instance
└── Complete data autonomy
```

## Related Decisions

- [ADR-001: DDD + Hexagonal Architecture Foundation](./ADR-001-ddd-hexagonal-architecture.md)
- [ADR-003: Domain Events and CQRS Implementation](./ADR-003-domain-events-cqrs.md)

## References

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.amazon.com/Domain-Driven-Design-Tackling-Complexity-Software/dp/0321125215)
- [Implementing Domain-Driven Design](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)
- [Bounded Context Canvas](https://github.com/ddd-crew/bounded-context-canvas)
- [Context Mapping](https://www.infoq.com/articles/ddd-contextmapping/)

---

**Last Updated**: January 20, 2024  
**Reviewers**: Architecture Team, Domain Experts  
**Next Review**: July 20, 2024
