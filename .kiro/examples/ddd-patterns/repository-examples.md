# Repository Pattern - Detailed Examples

## Principle Overview

**Repository** provides an abstraction over data access, allowing the domain layer to work with domain objects without knowing about persistence details. The repository interface lives in the domain layer, while the implementation lives in the infrastructure layer.

## Key Concepts

- **Interface in Domain Layer** - Repository contract defined in domain
- **Implementation in Infrastructure Layer** - Actual persistence logic
- **Return Domain Objects** - Not database entities
- **One Repository Per Aggregate Root** - Only aggregate roots have repositories
- **Use Optional for Single Results** - Explicit handling of not found cases

**Related Standards**: [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)

---

## Repository Interface (Domain Layer - Production Code)

### OrderRepository Interface

This is the actual OrderRepository interface from our production codebase:

```java
package solid.humank.genaidemo.domain.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.common.repository.BaseRepository;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * Order repository interface
 */
@Repository(name = "OrderRepository", description = "訂單聚合根儲存庫")
public interface OrderRepository extends BaseRepository<Order, OrderId> {
    
    /**
     * Save order
     */
    @Override
    Order save(Order order);
    
    /**
     * Find order by ID
     */
    Optional<Order> findById(OrderId id);
    
    /**
     * Find orders by customer ID
     */
    List<Order> findByCustomerId(CustomerId customerId);
    
    /**
     * Find orders by customer ID (UUID version)
     */
    List<Order> findByCustomerId(UUID customerId);
}
```

### CustomerRepository Interface

```java
package solid.humank.genaidemo.domain.customer.repository;

import java.util.List;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.Repository;
import solid.humank.genaidemo.domain.common.repository.BaseRepository;
import solid.humank.genaidemo.domain.customer.model.aggregate.Customer;
import solid.humank.genaidemo.domain.customer.model.valueobject.Email;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;

/**
 * Customer repository interface
 */
@Repository(name = "CustomerRepository", description = "客戶聚合根儲存庫")
public interface CustomerRepository extends BaseRepository<Customer, CustomerId> {
    
    /**
     * Save customer
     */
    @Override
    Customer save(Customer customer);
    
    /**
     * Find customer by ID
     */
    Optional<Customer> findById(CustomerId id);
    
    /**
     * Find customer by email
     */
    Optional<Customer> findByEmail(Email email);
    
    /**
     * Find all customers
     */
    List<Customer> findAll();
    
    /**
     * Delete customer
     */
    void delete(Customer customer);
    
    /**
     * Check if customer exists by email
     */
    boolean existsByEmail(Email email);
}
```

---

## Repository Implementation (Infrastructure Layer - Production Code)

### JPA Repository Interface

This is the actual JPA repository from our production codebase:

```java
package solid.humank.genaidemo.infrastructure.order.persistence.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;

/**
 * JPA Order Repository
 * Used for database interaction with Spring Data JPA
 */
@Repository
public interface JpaOrderRepository extends JpaRepository<JpaOrderEntity, String> {
    
    /**
     * Find orders by customer ID
     */
    List<JpaOrderEntity> findByCustomerId(String customerId);
    
    /**
     * Find order by ID
     */
    Optional<JpaOrderEntity> findById(String id);
    
    // ========== Statistical Query Methods ==========
    
    /**
     * Count all order items
     */
    @Query("SELECT COUNT(oi) FROM JpaOrderEntity o JOIN o.items oi")
    long countAllOrderItems();
    
    /**
     * Sum total amount by status
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM JpaOrderEntity o WHERE o.status = :status")
    BigDecimal sumTotalAmountByStatus(
            @Param("status") solid.humank.genaidemo.domain.common.valueobject.OrderStatus status);
    
    /**
     * Count distinct customers
     */
    @Query("SELECT COUNT(DISTINCT o.customerId) FROM JpaOrderEntity o")
    long countDistinctCustomers();
    
    /**
     * Count orders grouped by status
     */
    @Query("SELECT o.status, COUNT(o) FROM JpaOrderEntity o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();
    
    /**
     * Find distinct customer IDs
     */
    @Query("SELECT DISTINCT o.customerId FROM JpaOrderEntity o ORDER BY o.customerId")
    List<String> findDistinctCustomerIds();
    
    /**
     * Check if customer has orders
     */
    boolean existsByCustomerId(String customerId);
    
    /**
     * Count orders by customer ID
     */
    long countByCustomerId(String customerId);
}
```

### Repository Adapter Implementation

```java
package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.order.repository.OrderRepository;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.mapper.OrderMapper;
import solid.humank.genaidemo.infrastructure.order.persistence.repository.JpaOrderRepository;

/**
 * Order Repository Adapter
 * Implements domain repository interface using JPA
 */
@Component
public class OrderRepositoryAdapter implements OrderRepository {
    
    private final JpaOrderRepository jpaRepository;
    private final OrderMapper mapper;
    
    public OrderRepositoryAdapter(JpaOrderRepository jpaRepository, OrderMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Order save(Order order) {
        // Convert domain object to JPA entity
        JpaOrderEntity entity = mapper.toEntity(order);
        
        // Save to database
        JpaOrderEntity savedEntity = jpaRepository.save(entity);
        
        // Convert back to domain object
        Order savedOrder = mapper.toDomain(savedEntity);
        
        // Mark events as committed after successful save
        savedOrder.markEventsAsCommitted();
        
        return savedOrder;
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.getValue())
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Order> findByCustomerId(CustomerId customerId) {
        return jpaRepository.findByCustomerId(customerId.getValue())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    public List<Order> findByCustomerId(UUID customerId) {
        return jpaRepository.findByCustomerId(customerId.toString())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
```

---

## Domain-Entity Mapping (Production Code)

### OrderMapper

```java
package solid.humank.genaidemo.infrastructure.order.persistence.mapper;

import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.common.valueobject.OrderItem;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;
import solid.humank.genaidemo.domain.shared.valueobject.CustomerId;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderEntity;
import solid.humank.genaidemo.infrastructure.order.persistence.entity.JpaOrderItemEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper between Order domain object and JPA entity
 */
@Component
public class OrderMapper {
    
    /**
     * Convert domain object to JPA entity
     */
    public JpaOrderEntity toEntity(Order order) {
        JpaOrderEntity entity = new JpaOrderEntity();
        entity.setId(order.getId().getValue());
        entity.setCustomerId(order.getCustomerId().getValue());
        entity.setShippingAddress(order.getShippingAddress());
        entity.setStatus(order.getStatus());
        entity.setTotalAmount(order.getTotalAmount().getAmount());
        entity.setEffectiveAmount(order.getEffectiveAmount().getAmount());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());
        
        // Convert order items
        List<JpaOrderItemEntity> itemEntities = order.getItems().stream()
                .map(item -> toItemEntity(item, entity))
                .collect(Collectors.toList());
        entity.setItems(itemEntities);
        
        return entity;
    }
    
    /**
     * Convert JPA entity to domain object
     */
    public Order toDomain(JpaOrderEntity entity) {
        // Convert order items
        List<OrderItem> items = entity.getItems().stream()
                .map(this::toItemDomain)
                .collect(Collectors.toList());
        
        // Use reconstruction constructor
        return new Order(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                entity.getShippingAddress(),
                items,
                entity.getStatus(),
                Money.of(entity.getTotalAmount()),
                Money.of(entity.getEffectiveAmount()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
    
    private JpaOrderItemEntity toItemEntity(OrderItem item, JpaOrderEntity order) {
        JpaOrderItemEntity entity = new JpaOrderItemEntity();
        entity.setProductId(item.getProductId());
        entity.setProductName(item.getProductName());
        entity.setQuantity(item.getQuantity());
        entity.setPrice(item.getPrice().getAmount());
        entity.setOrder(order);
        return entity;
    }
    
    private OrderItem toItemDomain(JpaOrderItemEntity entity) {
        return new OrderItem(
                entity.getProductId(),
                entity.getProductName(),
                entity.getQuantity(),
                Money.of(entity.getPrice())
        );
    }
}
```

---

## Usage in Application Services (Production Code)

```java
@Service
@Transactional
public class OrderApplicationService {
    
    private final OrderRepository orderRepository;
    private final DomainEventApplicationService eventService;
    
    public OrderApplicationService(
            OrderRepository orderRepository,
            DomainEventApplicationService eventService) {
        this.orderRepository = orderRepository;
        this.eventService = eventService;
    }
    
    /**
     * Submit order
     */
    public void submitOrder(SubmitOrderCommand command) {
        // 1. Load aggregate from repository
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        
        // 2. Execute business operation
        order.submit();
        
        // 3. Save aggregate to repository
        orderRepository.save(order);
        
        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
    }
    
    /**
     * Create order
     */
    public OrderId createOrder(CreateOrderCommand command) {
        // 1. Create new aggregate
        Order order = new Order(
                OrderId.generate(),
                CustomerId.of(command.customerId()),
                command.shippingAddress()
        );
        
        // 2. Add items
        for (var item : command.items()) {
            order.addItem(
                    item.productId(),
                    item.productName(),
                    item.quantity(),
                    Money.twd(item.price())
            );
        }
        
        // 3. Save aggregate to repository
        orderRepository.save(order);
        
        // 4. Publish collected events
        eventService.publishEventsFromAggregate(order);
        
        return order.getId();
    }
    
    /**
     * Find orders by customer
     */
    public List<OrderDto> findOrdersByCustomer(CustomerId customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(OrderDto::from)
                .toList();
    }
}
```

---

## Key Patterns

### 1. Interface in Domain, Implementation in Infrastructure

```java
// Domain layer - interface
package solid.humank.genaidemo.domain.order.repository;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}

// Infrastructure layer - implementation
package solid.humank.genaidemo.infrastructure.order.persistence.adapter;

@Component
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    private final OrderMapper mapper;
    
    @Override
    public Order save(Order order) {
        JpaOrderEntity entity = mapper.toEntity(order);
        JpaOrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
```

### 2. Return Domain Objects, Not Entities

```java
// ✅ Good: Return domain object
@Override
public Optional<Order> findById(OrderId id) {
    return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain); // Convert to domain object
}

// ❌ Bad: Return JPA entity
@Override
public Optional<JpaOrderEntity> findById(OrderId id) {
    return jpaRepository.findById(id.getValue()); // Wrong!
}
```

### 3. Use Optional for Single Results

```java
// ✅ Good: Use Optional
Optional<Order> findById(OrderId id);

// ❌ Bad: Return null
Order findById(OrderId id); // May return null
```

### 4. Mapper Pattern for Domain-Entity Conversion

```java
@Component
public class OrderMapper {
    
    // Domain to Entity
    public JpaOrderEntity toEntity(Order order) {
        JpaOrderEntity entity = new JpaOrderEntity();
        entity.setId(order.getId().getValue());
        entity.setCustomerId(order.getCustomerId().getValue());
        // ... map other fields
        return entity;
    }
    
    // Entity to Domain
    public Order toDomain(JpaOrderEntity entity) {
        return new Order(
                OrderId.of(entity.getId()),
                CustomerId.of(entity.getCustomerId()),
                // ... map other fields
        );
    }
}
```

### 5. Mark Events as Committed After Save

```java
@Override
public Order save(Order order) {
    JpaOrderEntity entity = mapper.toEntity(order);
    JpaOrderEntity savedEntity = jpaRepository.save(entity);
    Order savedOrder = mapper.toDomain(savedEntity);
    
    // Mark events as committed after successful save
    savedOrder.markEventsAsCommitted();
    
    return savedOrder;
}
```

---

## Best Practices

### ✅ DO

1. **Interface in domain layer** - Repository contract belongs to domain
2. **Implementation in infrastructure layer** - Persistence details hidden
3. **Return domain objects** - Not database entities
4. **Use Optional for single results** - Explicit not found handling
5. **One repository per aggregate root** - Only aggregate roots have repositories
6. **Use mapper pattern** - Clean separation between domain and persistence
7. **Mark events as committed** - After successful save

### ❌ DON'T

1. **Don't expose JPA entities** - Always return domain objects
2. **Don't put business logic in repository** - Repository is for persistence only
3. **Don't create repositories for non-aggregate entities** - Only aggregate roots
4. **Don't return null** - Use Optional instead
5. **Don't leak persistence details** - Keep JPA annotations in infrastructure layer

---

## Testing Repositories

### Integration Test

```java
@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private JpaOrderRepository jpaRepository;
    
    private OrderMapper mapper;
    private OrderRepositoryAdapter repository;
    
    @BeforeEach
    void setUp() {
        mapper = new OrderMapper();
        repository = new OrderRepositoryAdapter(jpaRepository, mapper);
    }
    
    @Test
    void should_save_and_retrieve_order() {
        // Given
        Order order = new Order(
                OrderId.generate(),
                CustomerId.of("CUST-001"),
                "台北市信義區"
        );
        order.addItem("PROD-001", "Product 1", 2, Money.twd(100));
        
        // When
        Order savedOrder = repository.save(order);
        entityManager.flush();
        entityManager.clear();
        
        // Then
        Optional<Order> retrieved = repository.findById(savedOrder.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(savedOrder.getId());
        assertThat(retrieved.get().getItems()).hasSize(1);
    }
    
    @Test
    void should_find_orders_by_customer_id() {
        // Given
        CustomerId customerId = CustomerId.of("CUST-001");
        Order order1 = new Order(OrderId.generate(), customerId, "Address 1");
        Order order2 = new Order(OrderId.generate(), customerId, "Address 2");
        
        repository.save(order1);
        repository.save(order2);
        entityManager.flush();
        
        // When
        List<Order> orders = repository.findByCustomerId(customerId);
        
        // Then
        assertThat(orders).hasSize(2);
        assertThat(orders).allMatch(o -> o.getCustomerId().equals(customerId));
    }
}
```

---

## Summary

Repository Pattern provides:
- **Abstraction** - Domain doesn't know about persistence
- **Testability** - Easy to mock for unit tests
- **Flexibility** - Can change persistence technology
- **Clean Architecture** - Clear separation of concerns
- **Domain Focus** - Work with domain objects, not entities

---

**Related Documentation**:
- [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)
- [Aggregate Root Examples](aggregate-root-examples.md)
- [Domain Events Examples](domain-events-examples.md)
