<!-- 
此文件需要手動翻譯
原文件: .kiro/steering/bdd-tdd-principles.md
翻譯日期: Thu Aug 21 22:22:38 CST 2025

請將以下中文內容翻譯為英文，保持 Markdown 格式不變
-->

# BDD + TDD Development Principles

This project follows a strict BDD (Behavior-Driven Development) + TDD (Test-Driven Development) approach for all feature development. This ensures that every feature is driven by business requirements and implemented with high-quality, well-tested code.

## Development Workflow

### 1. BDD-First Approach (Outside-In)

#### Step 1: Write Feature Files (Gherkin)
- Start with business requirements expressed as Gherkin scenarios
- Location: `src/test/resources/features/{domain}/`
- Use clear, business-focused language
- Include happy path, edge cases, and error scenarios

```gherkin
Feature: Order Processing
  As a customer
  I want to place an order
  So that I can purchase products

  Scenario: Successfully place an order with valid items
    Given I am a registered customer with ID "CUST-001"
    And the following products are available:
      | productId | name        | price | stock |
      | PROD-001  | iPhone 15   | 999   | 10    |
      | PROD-002  | MacBook Pro | 2499  | 5     |
    When I place an order with the following items:
      | productId | quantity |
      | PROD-001  | 1        |
      | PROD-002  | 1        |
    Then the order should be created successfully
    And the order total should be 3498
    And the inventory should be updated accordingly
```

#### Step 2: Implement Step Definitions (Red Phase)
- Location: `src/test/java/solid/humank/genaidemo/bdd/{domain}/`
- Start with failing step definitions
- Use test builders and scenario handlers for clean test code

```java
@Given("I am a registered customer with ID {string}")
public void i_am_a_registered_customer_with_id(String customerId) {
    this.customer = CustomerTestBuilder.aCustomer()
        .withId(new CustomerId(customerId))
        .withEmail("customer@example.com")
        .build();
    // This will fail initially - no implementation yet
}
```

### 2. TDD Implementation (Inside-Out)

#### Step 3: Domain Model TDD
For each failing BDD step, implement the domain logic using TDD:

**Red → Green → Refactor Cycle:**

```java
// RED: Write failing unit test
@Test
@DisplayName("Should create order with valid customer and items")
void shouldCreateOrderWithValidCustomerAndItems() {
    // Arrange
    CustomerId customerId = new CustomerId("CUST-001");
    List<OrderItem> items = List.of(
        new OrderItem(new ProductId("PROD-001"), 1, new Money(BigDecimal.valueOf(999), "USD"))
    );
    
    // Act & Assert
    assertThatThrownBy(() -> new Order(customerId, items))
        .isInstanceOf(IllegalStateException.class); // Will fail - no Order class yet
}

// GREEN: Implement minimal code to pass
@AggregateRoot
public class Order {
    private final CustomerId customerId;
    private final List<OrderItem> items;
    
    public Order(CustomerId customerId, List<OrderItem> items) {
        this.customerId = customerId;
        this.items = items;
    }
}

// REFACTOR: Improve design while keeping tests green
@AggregateRoot
public class Order {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private final Money totalAmount;
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    public Order(CustomerId customerId, List<OrderItem> items) {
        validateInputs(customerId, items);
        this.orderId = OrderId.generate();
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.totalAmount = calculateTotal(items);
        
        // Publish domain event
        this.domainEvents.add(OrderCreatedEvent.of(orderId, customerId, totalAmount));
    }
    
    private void validateInputs(CustomerId customerId, List<OrderItem> items) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
    
    private Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO_USD, Money::add);
    }
}

// Value Objects as Records - following project conventions
@ValueObject(name = "CustomerId", description = "客戶唯一標識符")
public record CustomerId(String value) {
    public CustomerId {
        Objects.requireNonNull(value, "Customer ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
    
    public static CustomerId of(String id) {
        return new CustomerId(id);
    }
}

@ValueObject
public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
    }
    
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
    
    public static OrderId of(String id) {
        return new OrderId(UUID.fromString(id));
    }
}

@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));
    
    public Money {
        Objects.requireNonNull(amount, "金額不能為空");
        Objects.requireNonNull(currency, "貨幣不能為空");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不能為負數");
        }
    }
    
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on money with different currencies");
        }
    }
}
```

#### Step 4: Application Service TDD
Implement application services using TDD:

```java
// RED: Failing application service test
@Test
@DisplayName("Should create order and publish domain event")
void shouldCreateOrderAndPublishDomainEvent() {
    // Arrange
    CreateOrderCommand command = new CreateOrderCommand(
        new CustomerId("CUST-001"),
        List.of(new OrderItemCommand(new ProductId("PROD-001"), 1))
    );
    
    // Mock dependencies
    when(customerRepository.findById(any(CustomerId.class)))
        .thenReturn(Optional.of(CustomerTestBuilder.aCustomer().build()));
    when(productRepository.findById(any(ProductId.class)))
        .thenReturn(Optional.of(ProductTestBuilder.aProduct().build()));
    
    // Act
    OrderId orderId = orderApplicationService.createOrder(command);
    
    // Assert
    assertThat(orderId).isNotNull();
    verify(orderRepository).save(any(Order.class));
    // Events are published automatically when repository saves the aggregate
}

// GREEN: Implement application service following hexagonal architecture
@Service
@Transactional
public class OrderApplicationService implements OrderManagementUseCase {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    
    public OrderApplicationService(
        OrderRepository orderRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
    }
    
    @Override
    public OrderId createOrder(CreateOrderCommand command) {
        // Validate customer exists
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // Load products and create order items
        List<OrderItem> items = command.items().stream()
            .map(this::createOrderItem)
            .toList();
            
        // Create order (domain logic)
        Order order = new Order(command.customerId(), items);
        
        // Save order (repository will handle domain events)
        orderRepository.save(order);
        
        return order.getId();
    }
    
    private OrderItem createOrderItem(OrderItemCommand itemCommand) {
        Product product = productRepository.findById(itemCommand.productId())
            .orElseThrow(() -> new ProductNotFoundException(itemCommand.productId()));
        
        return new OrderItem(
            itemCommand.productId(),
            itemCommand.quantity(),
            product.getPrice()
        );
    }
}

// Command as Record
public record CreateOrderCommand(
    CustomerId customerId,
    List<OrderItemCommand> items
) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(items, "Items cannot be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}

public record OrderItemCommand(
    ProductId productId,
    int quantity
) {
    public OrderItemCommand {
        Objects.requireNonNull(productId, "Product ID cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
```

#### Step 5: Infrastructure TDD
Implement infrastructure adapters using TDD:

```java
// RED: Repository adapter test
@Test
@DisplayName("Should save and retrieve order")
void shouldSaveAndRetrieveOrder() {
    // Arrange
    Order order = OrderTestBuilder.anOrder().build();
    
    // Act
    orderRepositoryAdapter.save(order);
    Optional<Order> retrieved = orderRepositoryAdapter.findById(order.getId());
    
    // Assert
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getId()).isEqualTo(order.getId());
}
```

### 3. Integration and BDD Completion

#### Step 6: Make BDD Steps Green
- Wire up all components through Spring configuration
- Ensure BDD scenarios pass end-to-end
- Use real implementations in integration tests

#### Step 7: Refactor and Optimize
- Refactor code while keeping all tests green
- Optimize performance if needed
- Ensure architectural compliance with ArchUnit tests

## Testing Strategy

### Test Pyramid Structure

```
    /\
   /  \     E2E BDD Tests (Few)
  /____\    - Full system scenarios
 /      \   - Real database, real services
/________\  - Cucumber feature files

   /\
  /  \      Integration Tests (Some)
 /____\     - Application service tests
/      \    - Repository tests
\______/    - Spring Boot tests

     /\
    /  \    Unit Tests (Many)
   /____\   - Domain model tests
  /      \  - Value object tests
 /________\ - Business logic tests
```

### Test Categories

#### 1. BDD Tests (Cucumber)
- **Purpose**: Validate business requirements
- **Scope**: End-to-end scenarios
- **Location**: `src/test/resources/features/`
- **Tools**: Cucumber, Gherkin
- **Database**: Real H2 database (test profile)

#### 2. Unit Tests (JUnit)
- **Purpose**: Test individual components
- **Scope**: Single class/method
- **Location**: `src/test/java/.../domain/`
- **Tools**: JUnit 5, Mockito, AssertJ
- **Database**: No database dependencies

#### 3. Integration Tests (Spring Boot Test)
- **Purpose**: Test component interactions
- **Scope**: Multiple components
- **Location**: `src/test/java/.../infrastructure/`
- **Tools**: Spring Boot Test, TestContainers
- **Database**: Test database

#### 4. Architecture Tests (ArchUnit)
- **Purpose**: Enforce architectural rules
- **Scope**: Package structure, dependencies
- **Location**: `src/test/java/.../architecture/`
- **Tools**: ArchUnit
- **Database**: No database

## Code Quality Standards

### Test Code Quality
- Use descriptive test names that explain business intent
- Follow AAA pattern (Arrange, Act, Assert)
- Use test builders for complex object creation
- Avoid conditional logic in tests
- One assertion per test (when possible)

### Domain Model Quality
- Rich domain models with behavior, not anemic data structures
- Immutable value objects implemented as Java Records with `@ValueObject`
- Clear aggregate boundaries following DDD patterns
- Domain events as immutable Records implementing `DomainEvent`
- Specifications for complex business rules with `@Specification`
- Policies for business decisions with `@Policy`
- Business-focused method names
- Follow hexagonal architecture principles with clear port/adapter separation

### Application Service Quality
- Thin coordination layer
- Transaction boundaries
- Event publishing
- Input validation and transformation
- Error handling and recovery

## Development Rules

### 1. No Code Without Tests
- Every line of production code must be covered by tests
- Start with failing tests (Red phase)
- Write minimal code to pass (Green phase)
- Refactor while keeping tests green

### 2. BDD Scenarios Drive Development
- All features must start with Gherkin scenarios
- Scenarios must be reviewed by business stakeholders
- Implementation follows the scenarios exactly
- No gold-plating or extra features

### 3. Domain-First Design (DDD + Hexagonal Architecture)
- Start with domain modeling following DDD tactical patterns
- Business logic lives in domain layer (aggregates, entities, value objects, specifications, policies)
- Infrastructure is just technical details (adapters)
- Application services coordinate, don't contain business logic
- Use Java Records for immutable Value Objects and Domain Events
- Follow hexagonal architecture with clear ports (interfaces) and adapters (implementations)
- Respect aggregate boundaries and consistency rules
- Use Specification pattern for complex business rules
- Use Policy pattern for business decisions that may change

### 4. Continuous Refactoring
- Refactor after each green phase
- Maintain clean code standards
- Remove duplication
- Improve naming and structure

## DDD + Hexagonal Architecture Design Standards

### 1. Value Objects (Java Records)
All value objects must be implemented as immutable Java Records with `@ValueObject` annotation:

```java
// Value Object as Record - following project style
@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));
    
    public Money {
        Objects.requireNonNull(amount, "金額不能為空");
        Objects.requireNonNull(currency, "貨幣不能為空");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不能為負數");
        }
    }
    
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on money with different currencies");
        }
    }
}

@ValueObject(name = "CustomerId", description = "客戶唯一標識符")
public record CustomerId(String value) {
    public CustomerId {
        Objects.requireNonNull(value, "Customer ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
    
    public static CustomerId of(String id) {
        return new CustomerId(id);
    }
}

@ValueObject
public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
    }
    
    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }
    
    public static OrderId of(String id) {
        return new OrderId(UUID.fromString(id));
    }
}
```

### 2. Domain Events (Java Records)
All domain events must be implemented as immutable Java Records implementing `DomainEvent`:

```java
// Domain Event as Record - following project style
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static CustomerCreatedEvent create(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}

public record OrderSubmittedEvent(
    OrderId orderId,
    String customerId,
    Money totalAmount,
    int itemCount,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static OrderSubmittedEvent create(
        OrderId orderId, String customerId, Money totalAmount, int itemCount
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new OrderSubmittedEvent(
            orderId, customerId, totalAmount, itemCount,
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    @Override
    public UUID getEventId() {
        return eventId;
    }
    
    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() {
        return orderId.getValue();
    }
}
```

### 3. Aggregate Roots
Aggregate roots must use `@AggregateRoot` annotation and implement `AggregateRootInterface`:

```java
// Following project style - using AggregateRootInterface only
@AggregateRoot(name = "Order", description = "訂單聚合根", boundedContext = "Order", version = "1.0")
public class Order implements AggregateRootInterface {
    private final OrderId orderId;
    private final CustomerId customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final Money totalAmount;
    
    public Order(CustomerId customerId, List<OrderItem> items) {
        validateInputs(customerId, items);
        this.orderId = OrderId.generate();
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.status = OrderStatus.PENDING;
        this.totalAmount = calculateTotal(items);
        
        // Collect domain event using interface method
        collectEvent(OrderCreatedEvent.create(orderId, customerId, totalAmount));
    }
    
    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
        collectEvent(OrderConfirmedEvent.create(orderId));
    }
    
    private void validateInputs(CustomerId customerId, List<OrderItem> items) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
    
    private Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
    
    // Getters
    public OrderId getId() { return orderId; }
    public CustomerId getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    
    // Event management methods are provided by AggregateRootInterface:
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
    // - clearEvents()
}

// Customer aggregate example - also using AggregateRootInterface
@AggregateRoot(name = "Customer", description = "客戶聚合根", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    private RewardPoints rewardPoints;
    private Money totalSpending;
    
    public Customer(CustomerId id, CustomerName name, Email email, MembershipLevel membershipLevel) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.membershipLevel = membershipLevel;
        this.rewardPoints = RewardPoints.empty();
        this.totalSpending = Money.twd(0);
        
        // Collect domain event
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }
    
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        this.name = newName;
        this.email = newEmail;
        
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
    }
    
    public void updateSpending(Money amount, String orderId, String spendingType) {
        this.totalSpending = this.totalSpending.add(amount);
        
        // Calculate and add reward points
        int pointsEarned = amount.getAmount().intValue() / 10;
        this.rewardPoints = this.rewardPoints.add(pointsEarned);
        
        // Collect domain events
        collectEvent(CustomerSpendingUpdatedEvent.create(
            this.id, amount, this.totalSpending, orderId, spendingType));
        collectEvent(RewardPointsEarnedEvent.create(
            this.id, pointsEarned, this.rewardPoints.balance(), "Purchase reward"));
    }
    
    // Getters
    public CustomerId getId() { return id; }
    public CustomerName getName() { return name; }
    public Email getEmail() { return email; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public RewardPoints getRewardPoints() { return rewardPoints; }
    public Money getTotalSpending() { return totalSpending; }
}
```

### 6. Hexagonal Architecture Ports (Interfaces)
Define ports as interfaces in the domain layer:

```java
// Primary Port (Driving Port) - Use Case Interface
public interface OrderManagementUseCase {
    OrderId createOrder(CreateOrderCommand command);
    void confirmOrder(OrderId orderId);
    OrderDetails getOrderDetails(OrderId orderId);
}

// Secondary Port (Driven Port) - Repository Interface
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
    List<Order> findByCustomerId(CustomerId customerId);
}

// Secondary Port (Driven Port) - Event Publisher Interface
public interface DomainEventPublisher {
    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}
```

### 7. Hexagonal Architecture Adapters (Implementations)
Implement adapters in the infrastructure layer:

```java
// Primary Adapter (Driving Adapter) - REST Controller
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderManagementUseCase orderManagementUseCase;
    
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = OrderCommandMapper.toCommand(request);
        OrderId orderId = orderManagementUseCase.createOrder(command);
        return ResponseEntity.ok(new CreateOrderResponse(orderId.value()));
    }
}

// Secondary Adapter (Driven Adapter) - JPA Repository
@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaRepository;
    private final OrderMapper orderMapper;
    
    @Override
    public void save(Order order) {
        OrderJpaEntity entity = orderMapper.toJpaEntity(order);
        jpaRepository.save(entity);
        
        // Publish domain events after successful save
        publishDomainEvents(order.getDomainEvents());
        order.clearDomainEvents();
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.value())
            .map(orderMapper::toDomainEntity);
    }
}
```

### 8. Application Services (Use Case Implementation)
Application services implement use cases and coordinate domain objects:

```java
@Service
@Transactional
public class OrderApplicationService implements OrderManagementUseCase {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    
    @Override
    public OrderId createOrder(CreateOrderCommand command) {
        // Validate customer exists
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // Load products and create order items
        List<OrderItem> items = command.items().stream()
            .map(this::createOrderItem)
            .toList();
        
        // Create order (domain logic)
        Order order = new Order(command.customerId(), items);
        
        // Save order (will publish events)
        orderRepository.save(order);
        
        return order.getId();
    }
    
    private OrderItem createOrderItem(OrderItemCommand itemCommand) {
        Product product = productRepository.findById(itemCommand.productId())
            .orElseThrow(() -> new ProductNotFoundException(itemCommand.productId()));
        
        return new OrderItem(
            itemCommand.productId(),
            itemCommand.quantity(),
            product.getPrice()
        );
    }
}
```

### 9. Command Objects (Java Records)
Use Records for command objects:

```java
public record CreateOrderCommand(
    CustomerId customerId,
    List<OrderItemCommand> items
) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(items, "Items cannot be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}

public record OrderItemCommand(
    ProductId productId,
    int quantity
) {
    public OrderItemCommand {
        Objects.requireNonNull(productId, "Product ID cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
```

### 4. Specification Pattern
Specifications encapsulate business rules and validation logic as reusable, composable objects:

```java
// Specification implementation - following project style
@Specification(description = "訂單折扣規格，用於判斷訂單是否符合折扣條件")
public class OrderDiscountSpecification implements Specification<Order> {
    private final Money minimumAmount;
    private final LocalDateTime currentTime;
    
    public OrderDiscountSpecification(Money minimumAmount, LocalDateTime currentTime) {
        this.minimumAmount = minimumAmount;
        this.currentTime = currentTime;
    }
    
    @Override
    public boolean isSatisfiedBy(Order order) {
        return isMinimumAmountMet(order) && isWeekend() && hasMultipleItems(order);
    }
    
    private boolean isMinimumAmountMet(Order order) {
        return order.getTotalAmount().amount().compareTo(minimumAmount.amount()) >= 0;
    }
    
    private boolean isWeekend() {
        DayOfWeek dayOfWeek = currentTime.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
    
    private boolean hasMultipleItems(Order order) {
        return order.getItems().size() >= 2;
    }
    
    // Static factory method for common specifications
    public static OrderDiscountSpecification weekendSpecial() {
        return new OrderDiscountSpecification(Money.twd(1000), LocalDateTime.now());
    }
}

// Specification composition examples
Specification<Order> complexRule = OrderDiscountSpecification.weekendSpecial()
    .and(new MinimumAmountSpecification(Money.twd(2000)))
    .or(new VipCustomerSpecification());
```

### 5. Policy Pattern
Policies encapsulate business decisions and calculations that may change over time:

```java
// Policy implementation - following project style
@Policy(description = "訂單折扣政策，結合Specification和Policy模式來實作折扣規則")
public class OrderDiscountPolicy implements DomainPolicy<Order, Money> {
    private final OrderDiscountSpecification specification;
    private final BigDecimal discountRate;
    
    public OrderDiscountPolicy(LocalDateTime currentTime, BigDecimal discountRate) {
        this.specification = new OrderDiscountSpecification(Money.twd(1000), currentTime);
        this.discountRate = discountRate;
    }
    
    @Override
    public Money apply(Order order) {
        if (!isApplicableTo(order)) {
            return order.getTotalAmount();
        }
        
        BigDecimal discountAmount = order.getTotalAmount().amount().multiply(discountRate);
        return Money.of(
            order.getTotalAmount().amount().subtract(discountAmount),
            order.getTotalAmount().currency()
        );
    }
    
    @Override
    public boolean isApplicableTo(Order order) {
        return specification.isSatisfiedBy(order);
    }
    
    // Static factory method for common policies
    public static OrderDiscountPolicy weekendDiscount() {
        return new OrderDiscountPolicy(LocalDateTime.now(), new BigDecimal("0.1"));
    }
}

// Usage in Aggregate Root
@AggregateRoot(name = "Order", description = "訂單聚合根", boundedContext = "Order", version = "1.0")
public class Order implements AggregateRootInterface {
    // ... other fields
    
    public Money calculateDiscountedTotal(OrderDiscountPolicy policy) {
        Money discountedAmount = policy.apply(this);
        
        if (!discountedAmount.equals(this.totalAmount)) {
            collectEvent(OrderDiscountAppliedEvent.create(
                this.orderId, 
                this.totalAmount, 
                discountedAmount,
                policy.getClass().getSimpleName()
            ));
        }
        
        return discountedAmount;
    }
    
    public boolean isEligibleForDiscount(OrderDiscountSpecification specification) {
        return specification.isSatisfiedBy(this);
    }
}
```

### 10. Architecture Rules Summary

#### Layer Responsibilities
1. **Domain Layer**: Contains only business logic, no technical dependencies
2. **Application Layer**: Coordinates domain objects, implements use cases
3. **Infrastructure Layer**: Contains technical implementations (adapters)
4. **Interfaces Layer**: Contains controllers and external interfaces

#### DDD Tactical Patterns
5. **Value Objects**: Always implemented as immutable Records with `@ValueObject` annotation
6. **Domain Events**: Always implemented as immutable Records implementing `DomainEvent` interface
7. **Aggregate Roots**: Always use `@AggregateRoot` annotation and implement `AggregateRootInterface` (no abstract base classes)
8. **Specifications**: Use `@Specification` annotation and implement `Specification<T>` interface for business rules
9. **Policies**: Use `@Policy` annotation and implement `DomainPolicy<T, R>` interface for business decisions

#### Architectural Principles
10. **Aggregate Boundaries**: Respect consistency boundaries
11. **Port/Adapter Pattern**: Clear separation between business logic and technical concerns
12. **Event Management**: Use `collectEvent()` method provided by `AggregateRootInterface`
13. **Dependency Direction**: Follow hexagonal architecture dependency rules
14. **Immutability**: All value objects and domain events must be immutable

## Tools and Configuration

### BDD Tools
- **Cucumber 7**: BDD framework
- **Gherkin**: Business-readable scenarios
- **Allure**: Test reporting and visualization

### TDD Tools
- **JUnit 5**: Unit testing framework
- **Mockito**: Mocking framework
- **AssertJ**: Fluent assertions
- **ArchUnit**: Architecture testing

### Test Data Management
- **Test Builders**: Clean test data creation
- **Test Fixtures**: Reusable test data
- **Scenario Handlers**: Complex test scenario logic

### Continuous Integration
- All tests must pass before merge
- BDD scenarios run in CI/CD pipeline
- Test coverage reports generated
- Architecture compliance verified

## Example Development Flow

### Feature: Customer Loyalty Points

1. **Write BDD Scenario**:
```gherkin
Scenario: Customer earns loyalty points on purchase
  Given a customer with ID "CUST-001"
  And the customer has 100 loyalty points
  When the customer makes a purchase of $50
  Then the customer should earn 5 loyalty points
  And the customer's total points should be 105
```

2. **TDD Domain Model**:
```java
// RED
@Test
void shouldEarnLoyaltyPointsOnPurchase() {
    Customer customer = new Customer(
        CustomerId.of("CUST-001"),
        new CustomerName("John Doe"),
        new Email("john@example.com"),
        MembershipLevel.STANDARD
    );
    customer.addRewardPoints(100, "Initial points");
    
    customer.updateSpending(Money.twd(500), "ORDER-001", "Purchase");
    
    assertThat(customer.getRewardPoints().balance()).isEqualTo(150); // 100 + 50 points earned
}

// GREEN - Following project style
@AggregateRoot(name = "Customer", description = "客戶聚合根", boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    private RewardPoints rewardPoints;
    private Money totalSpending;
    
    public Customer(CustomerId id, CustomerName name, Email email, MembershipLevel membershipLevel) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.membershipLevel = membershipLevel;
        this.rewardPoints = RewardPoints.empty();
        this.totalSpending = Money.twd(0);
        
        // Collect domain event
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }
    
    public void updateSpending(Money amount, String orderId, String spendingType) {
        Money oldTotalSpending = this.totalSpending;
        this.totalSpending = this.totalSpending.add(amount);
        
        // Calculate reward points (10% of spending)
        int pointsEarned = amount.getAmount().intValue() / 10;
        this.rewardPoints = this.rewardPoints.add(pointsEarned);
        
        // Collect domain events
        collectEvent(CustomerSpendingUpdatedEvent.create(
            this.id, amount, this.totalSpending, orderId, spendingType));
        collectEvent(RewardPointsEarnedEvent.create(
            this.id, pointsEarned, this.rewardPoints.balance(), "Purchase reward"));
    }
    
    public void addRewardPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.add(points);
        collectEvent(RewardPointsEarnedEvent.create(this.id, points, this.rewardPoints.balance(), reason));
    }
    
    // Getters
    public CustomerId getId() { return id; }
    public RewardPoints getRewardPoints() { return rewardPoints; }
    public Money getTotalSpending() { return totalSpending; }
}

// Value Objects as Records - following project style
@ValueObject
public record RewardPoints(int balance) {
    public RewardPoints {
        if (balance < 0) {
            throw new IllegalArgumentException("Reward points cannot be negative");
        }
    }
    
    public static RewardPoints empty() {
        return new RewardPoints(0);
    }
    
    public RewardPoints add(int points) {
        return new RewardPoints(this.balance + points);
    }
    
    public RewardPoints redeem(int points) {
        if (points > this.balance) {
            throw new IllegalArgumentException("Insufficient reward points");
        }
        return new RewardPoints(this.balance - points);
    }
    
    public boolean canRedeem(int points) {
        return points <= this.balance;
    }
}

// Domain Event as Record - following project style
public record RewardPointsEarnedEvent(
    CustomerId customerId,
    int pointsEarned,
    int totalBalance,
    String reason,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static RewardPointsEarnedEvent create(
        CustomerId customerId, int pointsEarned, int totalBalance, String reason
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new RewardPointsEarnedEvent(
            customerId, pointsEarned, totalBalance, reason,
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    @Override
    public UUID getEventId() { return eventId; }
    
    @Override
    public LocalDateTime getOccurredOn() { return occurredOn; }
    
    @Override
    public String getEventType() {
        return DomainEvent.getEventTypeFromClass(this.getClass());
    }
    
    @Override
    public String getAggregateId() { return customerId.getValue(); }
}
```

3. **TDD Application Service**:
```java
@Test
void shouldProcessPurchaseAndUpdateLoyaltyPoints() {
    // Test application service coordination
}
```

4. **Make BDD Green**: Wire everything together and ensure the Cucumber scenario passes.

This approach ensures that every feature is business-driven, well-tested, and maintains high code quality throughout the development process.


<!-- 翻譯完成後請刪除此註釋 -->
