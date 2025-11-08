# Extensibility Points

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Architecture & Development Team

## Overview

This document describes the extensibility mechanisms built into the Enterprise E-Commerce Platform. These extension points allow adding new functionality without modifying existing code, following the Open/Closed Principle.

## Extension Mechanisms

### 1. Domain Event Extensions

#### Purpose

Add new behavior by listening to domain events without modifying the event publisher.

#### How It Works

```java
// Existing: Order aggregate publishes event
@AggregateRoot
public class Order {
    
    public void submit() {
        validateOrderSubmission();
        this.status = OrderStatus.PENDING;
        
        // Publish event - existing code unchanged
        collectEvent(OrderSubmittedEvent.create(id, customerId, totalAmount));
    }
}
```

```java
// Extension: Add new handler without modifying Order
@Component
public class LoyaltyPointsEventHandler 
    extends AbstractDomainEventHandler<OrderSubmittedEvent> {
    
    @Override
    public void handle(OrderSubmittedEvent event) {
        // New functionality: Award loyalty points
        loyaltyService.awardPoints(
            event.customerId(),
            calculatePoints(event.totalAmount())
        );
    }
    
    @Override
    public Class<OrderSubmittedEvent> getSupportedEventType() {
        return OrderSubmittedEvent.class;
    }
}
```

#### Extension Points

| Event | Extension Opportunities |
|-------|------------------------|
| `CustomerCreatedEvent` | Welcome email, loyalty enrollment, analytics tracking |
| `OrderSubmittedEvent` | Inventory reservation, payment processing, notifications |
| `PaymentProcessedEvent` | Order confirmation, invoice generation, accounting sync |
| `ProductCreatedEvent` | Search index update, recommendation engine update |
| `ReviewSubmittedEvent` | Moderation queue, sentiment analysis, notification |

#### Best Practices

- ✅ Event handlers are independent and can be added/removed freely
- ✅ Use `@Order` annotation to control execution sequence if needed
- ✅ Implement idempotency in event handlers
- ❌ Don't create circular event dependencies
- ❌ Don't modify event payload structure (create new event version instead)

---

### 2. Strategy Pattern Extensions

#### Purpose

Plug in different algorithms or behaviors at runtime.

#### Pricing Strategy Example

```java
// Strategy interface
public interface PricingStrategy {
    Money calculatePrice(Product product, Customer customer);
    String getStrategyName();
}

// Standard pricing
@Component("standardPricing")
public class StandardPricingStrategy implements PricingStrategy {
    
    @Override
    public Money calculatePrice(Product product, Customer customer) {
        return product.getBasePrice();
    }
    
    @Override
    public String getStrategyName() {
        return "STANDARD";
    }
}

// Premium customer pricing
@Component("premiumPricing")
public class PremiumPricingStrategy implements PricingStrategy {
    
    @Override
    public Money calculatePrice(Product product, Customer customer) {
        Money basePrice = product.getBasePrice();
        return basePrice.multiply(0.9); // 10% discount
    }
    
    @Override
    public String getStrategyName() {
        return "PREMIUM";
    }
}

// Seasonal pricing - NEW EXTENSION
@Component("seasonalPricing")
public class SeasonalPricingStrategy implements PricingStrategy {
    
    @Override
    public Money calculatePrice(Product product, Customer customer) {
        Money basePrice = product.getBasePrice();
        
        if (isHolidaySeason()) {
            return basePrice.multiply(0.85); // 15% holiday discount
        }
        
        return basePrice;
    }
    
    @Override
    public String getStrategyName() {
        return "SEASONAL";
    }
}
```

```java
// Strategy selector
@Service
public class PricingService {
    
    private final Map<String, PricingStrategy> strategies;
    
    public PricingService(List<PricingStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                PricingStrategy::getStrategyName,
                Function.identity()
            ));
    }
    
    public Money calculatePrice(Product product, Customer customer) {
        String strategyName = determineStrategy(customer);
        PricingStrategy strategy = strategies.get(strategyName);
        
        return strategy.calculatePrice(product, customer);
    }
    
    private String determineStrategy(Customer customer) {
        if (customer.isPremium()) {
            return "PREMIUM";
        }
        if (isHolidaySeason()) {
            return "SEASONAL";
        }
        return "STANDARD";
    }
}
```

#### Available Strategy Extension Points

| Strategy Type | Interface | Use Case |
|---------------|-----------|----------|
| **Pricing** | `PricingStrategy` | Different pricing algorithms |
| **Shipping** | `ShippingStrategy` | Shipping cost calculation |
| **Payment** | `PaymentStrategy` | Payment gateway integration |
| **Discount** | `DiscountStrategy` | Discount calculation rules |
| **Recommendation** | `RecommendationStrategy` | Product recommendation algorithms |
| **Notification** | `NotificationStrategy` | Notification delivery methods |

---

### 3. Repository Pattern Extensions

#### Purpose

Switch data sources or add new storage mechanisms without changing business logic.

#### Implementation

```java
// Domain interface (stable)
package solid.humank.genaidemo.domain.order.repository;

public interface OrderRepository {
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
    Order save(Order order);
    void delete(OrderId orderId);
}
```

```java
// PostgreSQL implementation (existing)
package solid.humank.genaidemo.infrastructure.order.persistence;

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

```java
// MongoDB implementation - NEW EXTENSION
package solid.humank.genaidemo.infrastructure.order.persistence.mongo;

@Repository
@Profile("mongodb")
public class MongoOrderRepository implements OrderRepository {
    
    private final MongoTemplate mongoTemplate;
    private final OrderMapper mapper;
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        Query query = new Query(Criteria.where("_id").is(orderId.getValue()));
        OrderDocument doc = mongoTemplate.findOne(query, OrderDocument.class);
        return Optional.ofNullable(doc).map(mapper::toDomain);
    }
    
    @Override
    public Order save(Order order) {
        OrderDocument doc = mapper.toDocument(order);
        mongoTemplate.save(doc);
        return order;
    }
}
```

```java
// Cached repository - NEW EXTENSION
@Repository
@Primary
public class CachedOrderRepository implements OrderRepository {
    
    private final OrderRepository delegate;
    private final RedisTemplate<String, Order> cache;
    
    public CachedOrderRepository(
        @Qualifier("jpaOrderRepository") OrderRepository delegate,
        RedisTemplate<String, Order> cache
    ) {
        this.delegate = delegate;
        this.cache = cache;
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        // Try cache first
        String cacheKey = "order:" + orderId.getValue();
        Order cached = cache.opsForValue().get(cacheKey);
        
        if (cached != null) {
            return Optional.of(cached);
        }
        
        // Fetch from database
        Optional<Order> order = delegate.findById(orderId);
        
        // Cache for future requests
        order.ifPresent(o -> 
            cache.opsForValue().set(cacheKey, o, Duration.ofMinutes(15))
        );
        
        return order;
    }
    
    @Override
    public Order save(Order order) {
        Order saved = delegate.save(order);
        
        // Invalidate cache
        String cacheKey = "order:" + order.getId().getValue();
        cache.delete(cacheKey);
        
        return saved;
    }
}
```

---

### 4. Plugin Architecture

#### Purpose

Load and execute external plugins at runtime.

#### Plugin Interface

```java
// Plugin interface
public interface EcommercePlugin {
    String getPluginId();
    String getPluginName();
    String getVersion();
    void initialize(PluginContext context);
    void execute(PluginEvent event);
    void shutdown();
}

// Plugin context
public interface PluginContext {
    <T> T getService(Class<T> serviceClass);
    Configuration getConfiguration();
    Logger getLogger();
}

// Plugin event
public interface PluginEvent {
    String getEventType();
    Map<String, Object> getData();
    Instant getTimestamp();
}
```

#### Plugin Implementation Example

```java
// Fraud detection plugin
public class FraudDetectionPlugin implements EcommercePlugin {
    
    private OrderService orderService;
    private Logger logger;
    
    @Override
    public String getPluginId() {
        return "fraud-detection";
    }
    
    @Override
    public String getPluginName() {
        return "Fraud Detection Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public void initialize(PluginContext context) {
        this.orderService = context.getService(OrderService.class);
        this.logger = context.getLogger();
        logger.info("Fraud Detection Plugin initialized");
    }
    
    @Override
    public void execute(PluginEvent event) {
        if ("ORDER_SUBMITTED".equals(event.getEventType())) {
            String orderId = (String) event.getData().get("orderId");
            
            // Perform fraud detection
            FraudScore score = analyzeFraud(orderId);
            
            if (score.isHighRisk()) {
                orderService.flagForReview(orderId, "High fraud risk detected");
            }
        }
    }
    
    @Override
    public void shutdown() {
        logger.info("Fraud Detection Plugin shutting down");
    }
    
    private FraudScore analyzeFraud(String orderId) {
        // Fraud detection logic
        return new FraudScore(0.3); // Low risk
    }
}
```

#### Plugin Manager

```java
@Service
public class PluginManager {
    
    private final Map<String, EcommercePlugin> plugins = new ConcurrentHashMap<>();
    private final PluginContext context;
    
    public void loadPlugin(EcommercePlugin plugin) {
        plugin.initialize(context);
        plugins.put(plugin.getPluginId(), plugin);
        logger.info("Loaded plugin: {} v{}", 
            plugin.getPluginName(), plugin.getVersion());
    }
    
    public void unloadPlugin(String pluginId) {
        EcommercePlugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            plugin.shutdown();
            logger.info("Unloaded plugin: {}", plugin.getPluginName());
        }
    }
    
    public void notifyPlugins(PluginEvent event) {
        plugins.values().forEach(plugin -> {
            try {
                plugin.execute(event);
            } catch (Exception e) {
                logger.error("Plugin {} failed to process event", 
                    plugin.getPluginId(), e);
            }
        });
    }
}
```

---

### 5. Dependency Injection Extensions

#### Purpose

Configure different implementations based on environment or feature flags.

#### Configuration-Based Extension

```java
@Configuration
public class PaymentConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "stripe")
    public PaymentGateway stripePaymentGateway() {
        return new StripePaymentGateway();
    }
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "paypal")
    public PaymentGateway paypalPaymentGateway() {
        return new PayPalPaymentGateway();
    }
    
    @Bean
    @ConditionalOnProperty(name = "payment.provider", havingValue = "mock")
    @Profile("test")
    public PaymentGateway mockPaymentGateway() {
        return new MockPaymentGateway();
    }
}
```

#### Profile-Based Extension

```java
@Configuration
public class CacheConfiguration {
    
    @Bean
    @Profile("production")
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultCacheConfig())
            .build();
    }
    
    @Bean
    @Profile("development")
    public CacheManager simpleCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("products"),
            new ConcurrentMapCache("customers")
        ));
        return cacheManager;
    }
    
    @Bean
    @Profile("test")
    public CacheManager noOpCacheManager() {
        return new NoOpCacheManager();
    }
}
```

---

### 6. API Extension Points

#### Purpose

Extend API functionality without breaking existing clients.

#### Custom Headers

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @RequestBody CreateOrderRequest request,
        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
        @RequestHeader(value = "X-Client-Version", required = false) String clientVersion,
        @RequestHeader(value = "X-Feature-Flags", required = false) String featureFlags
    ) {
        // Use headers to enable optional features
        CreateOrderCommand command = CreateOrderCommand.builder()
            .items(request.getItems())
            .customerId(request.getCustomerId())
            .idempotencyKey(idempotencyKey)
            .build();
        
        // Apply feature flags if provided
        if (featureFlags != null && featureFlags.contains("express-checkout")) {
            command.setExpressCheckout(true);
        }
        
        Order order = orderService.createOrder(command);
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
```

#### Query Parameters for Extensions

```java
@GetMapping
public ResponseEntity<Page<OrderResponse>> getOrders(
    @RequestParam String customerId,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String sortBy,
    @RequestParam(required = false) String include, // Extension point
    Pageable pageable
) {
    Page<Order> orders = orderService.findOrders(customerId, status, pageable);
    
    // Optional includes based on query parameter
    if ("items".equals(include)) {
        orders.forEach(order -> order.loadItems());
    }
    if ("customer".equals(include)) {
        orders.forEach(order -> order.loadCustomer());
    }
    
    return ResponseEntity.ok(orders.map(OrderResponse::from));
}
```

---

## Extension Guidelines

### When to Create Extension Points

✅ **Create extension points when**:

- Multiple implementations are likely (payment gateways, shipping providers)
- Behavior varies by customer segment or region
- Third-party integrations are needed
- A/B testing different approaches
- Gradual rollout of new features

❌ **Don't create extension points when**:

- Only one implementation will ever exist
- Behavior is core to the domain and unlikely to change
- Complexity outweighs benefits
- Performance is critical and abstraction adds overhead

### Extension Point Design Principles

1. **Interface Segregation**: Keep interfaces small and focused
2. **Dependency Inversion**: Depend on abstractions, not concretions
3. **Liskov Substitution**: Implementations must be substitutable
4. **Documentation**: Document extension points clearly
5. **Examples**: Provide example implementations

### Testing Extensions

```java
@SpringBootTest
class ExtensionTest {
    
    @Test
    void should_load_custom_pricing_strategy() {
        // Given: Custom pricing strategy is registered
        PricingStrategy customStrategy = new CustomPricingStrategy();
        pricingService.registerStrategy(customStrategy);
        
        // When: Pricing is calculated
        Money price = pricingService.calculatePrice(product, customer);
        
        // Then: Custom strategy is used
        assertThat(price).isEqualTo(expectedCustomPrice);
    }
    
    @Test
    void should_handle_plugin_failure_gracefully() {
        // Given: Plugin that throws exception
        EcommercePlugin faultyPlugin = new FaultyPlugin();
        pluginManager.loadPlugin(faultyPlugin);
        
        // When: Event is processed
        pluginManager.notifyPlugins(event);
        
        // Then: Other plugins still execute
        verify(otherPlugin).execute(event);
    }
}
```

---

## Extension Registry

### Current Extension Points

| Extension Point | Type | Interface/Event | Status |
|----------------|------|-----------------|--------|
| **Pricing** | Strategy | `PricingStrategy` | ✅ Active |
| **Shipping** | Strategy | `ShippingStrategy` | ✅ Active |
| **Payment** | Strategy | `PaymentGateway` | ✅ Active |
| **Discount** | Strategy | `DiscountStrategy` | ✅ Active |
| **Notification** | Event Handler | `OrderSubmittedEvent` | ✅ Active |
| **Inventory** | Event Handler | `ProductCreatedEvent` | ✅ Active |
| **Analytics** | Event Handler | All domain events | ✅ Active |
| **Fraud Detection** | Plugin | `EcommercePlugin` | 🚧 Beta |
| **Recommendation** | Strategy | `RecommendationStrategy` | 📋 Planned |
| **Search** | Repository | `ProductSearchRepository` | 📋 Planned |

### Requesting New Extension Points

To request a new extension point:

1. **Create RFC**: Document the use case and proposed interface
2. **Architecture Review**: Present to architecture team
3. **Prototype**: Create proof-of-concept implementation
4. **Documentation**: Document the extension point
5. **Examples**: Provide at least 2 example implementations

---

**Related Documents**:

- [Overview](overview.md) - Evolution perspective introduction
- [Technology Evolution](technology-evolution.md) - Framework upgrade strategies
- [API Versioning](api-versioning.md) - API compatibility and versioning
- [Refactoring](refactoring.md) - Code quality and technical debt
