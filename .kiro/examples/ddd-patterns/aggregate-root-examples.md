# Aggregate Root - Detailed Examples

## Principle Overview

An **Aggregate Root** is the entry point to an aggregate - a cluster of domain objects that can be treated as a single unit. Only the aggregate root can be obtained from repositories, and all changes to the aggregate must go through the root.

## Key Concepts

- **Consistency Boundary**: Aggregate defines transaction boundary
- **Event Collection**: Aggregate collects domain events
- **Invariant Protection**: Aggregate enforces business rules
- **Single Entry Point**: Only root is accessible from outside

---

## Example 1: Order Aggregate (Production Code)

### Complete Implementation

This is the actual Order aggregate from our production codebase, demonstrating real-world DDD patterns:

```java
@AggregateRoot(
    name = "Order", 
    description = "訂單聚合根，封裝訂單相關的業務規則和行為", 
    boundedContext = "Order", 
    version = "1.0"
)
@AggregateLifecycle.ManagedLifecycle
public class Order extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {
    
    // Identity
    private final OrderId id;
    private final CustomerId customerId;
    private final String shippingAddress;
    
    // State tracking
    private final AggregateStateTracker<Order> stateTracker = new AggregateStateTracker<>(this);
    
    // State
    private OrderStatus status;
    private final List<OrderItem> items;
    private Money totalAmount;
    private Money effectiveAmount;
    
    // Timestamps
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Constructor for creating new order
     */
    public Order(OrderId orderId, CustomerId customerId, String shippingAddress) {
        Objects.requireNonNull(orderId, "訂單ID不能為空");
        Objects.requireNonNull(customerId, "客戶ID不能為空");
        requireNonEmpty(shippingAddress, "配送地址不能為空");
        
        this.id = orderId;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.items = new ArrayList<>();
        this.status = OrderStatus.CREATED;
        this.totalAmount = Money.zero();
        this.effectiveAmount = Money.zero();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        
        // Collect domain event
        collectEvent(OrderCreatedEvent.create(
            this.id, this.customerId.toString(), Money.zero(), List.of()));
    }
    
    /**
     * Reconstruction constructor - for rebuilding from persistence
     * This constructor does NOT publish domain events
     */
    @AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建訂單聚合根")
    protected Order(
            OrderId id,
            CustomerId customerId,
            String shippingAddress,
            List<OrderItem> items,
            OrderStatus status,
            Money totalAmount,
            Money effectiveAmount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "訂單ID不能為空");
        this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
        this.shippingAddress = Objects.requireNonNull(shippingAddress, "配送地址不能為空");
        this.items = new ArrayList<>(Objects.requireNonNull(items, "訂單項列表不能為空"));
        this.status = Objects.requireNonNull(status, "訂單狀態不能為空");
        this.totalAmount = Objects.requireNonNull(totalAmount, "訂單總金額不能為空");
        this.effectiveAmount = Objects.requireNonNull(effectiveAmount, "訂單實際金額不能為空");
        this.createdAt = Objects.requireNonNull(createdAt, "創建時間不能為空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "更新時間不能為空");
        
        // Note: No events published during reconstruction
    }
    
    // Business methods - enforce invariants
    public void addItem(String productId, String productName, int quantity, Money price) {
        // Check order state
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                "Cannot add items to an order that is not in CREATED state");
        }
        
        // Create order item
        OrderItem item = new OrderItem(productId, productName, quantity, price);
        items.add(item);
        
        // Update total amount
        totalAmount = totalAmount.add(item.getSubtotal());
        effectiveAmount = totalAmount;
        updatedAt = LocalDateTime.now();
        
        // Collect domain event
        collectEvent(OrderItemAddedEvent.create(this.id, productId, quantity, price));
    }
    
    public void submit() {
        // Validate business rules
        validateOrderSubmission();
        
        OrderStatus oldStatus = this.status;
        
        // Use state tracker to track changes and auto-generate events
        stateTracker.trackChange("status", oldStatus, OrderStatus.PENDING,
            (oldValue, newValue) -> OrderSubmittedEvent.create(
                this.id, this.customerId.toString(), this.totalAmount, this.items.size()));
        
        // Update state
        status = OrderStatus.PENDING;
        updatedAt = LocalDateTime.now();
        
        // Cross-aggregate operation: notify inventory system to reserve items
        CrossAggregateOperation.publishEvent(this,
            new OrderInventoryReservationRequestedEvent(
                this.id, this.customerId, this.items));
    }
    
    public void confirm() {
        // Validate business rules
        validateOrderConfirmation();
        
        OrderStatus oldStatus = this.status;
        
        // Use state tracker to track changes and auto-generate events
        stateTracker.trackChange("status", oldStatus, OrderStatus.CONFIRMED,
            (oldValue, newValue) -> new OrderConfirmedEvent(
                this.id, this.customerId, this.totalAmount));
        
        // Update state
        status = OrderStatus.CONFIRMED;
        updatedAt = LocalDateTime.now();
        
        // Cross-aggregate operation: notify payment system to prepare payment
        CrossAggregateOperation.publishEvent(this,
            new OrderPaymentRequestedEvent(
                this.id, this.customerId, this.effectiveAmount));
    }
    
    public void cancel() {
        // Check state
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                "Cannot cancel an order that is already delivered or cancelled");
        }
        
        // Update state
        status = OrderStatus.CANCELLED;
        updatedAt = LocalDateTime.now();
    }
    
    // Validation methods - protect invariants
    private void validateOrderSubmission() {
        BusinessRuleViolationException.Builder violationBuilder = 
            new BusinessRuleViolationException.Builder("Order", this.id.getValue());
        
        if (items.isEmpty()) {
            violationBuilder.addError("ORDER_ITEMS_REQUIRED", 
                "Cannot submit an order with no items");
        }
        
        if (status != OrderStatus.CREATED) {
            violationBuilder.addError("ORDER_STATUS_INVALID",
                String.format("只有狀態為 CREATED 的訂單可以提交，當前狀態：%s", status));
        }
        
        if (totalAmount == null || totalAmount.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            violationBuilder.addError("ORDER_AMOUNT_INVALID", "訂單金額必須大於零");
        }
        
        // Check for errors and throw exception
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }
    
    private void validateOrderConfirmation() {
        if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
            throw new BusinessRuleViolationException("Order", this.id.getValue(),
                "ORDER_CONFIRMATION_INVALID",
                String.format("無法確認狀態為 %s 的訂單", status));
        }
    }
    
    /**
     * Post-reconstruction validation
     */
    @AggregateReconstruction.PostReconstruction("驗證重建後的訂單聚合根狀態")
    public void validateReconstructedState() {
        BusinessRuleViolationException.Builder violationBuilder = 
            new BusinessRuleViolationException.Builder("Order", this.id.getValue());
        
        if (this.id == null) {
            violationBuilder.addError("ORDER_ID_REQUIRED", "訂單ID不能為空");
        }
        
        if (this.customerId == null) {
            violationBuilder.addError("CUSTOMER_ID_REQUIRED", "客戶ID不能為空");
        }
        
        if (this.shippingAddress == null || this.shippingAddress.isBlank()) {
            violationBuilder.addError("SHIPPING_ADDRESS_REQUIRED", "配送地址不能為空");
        }
        
        // Check for errors and throw exception
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }
    
    // Query methods - safe to expose
    public OrderId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public Money getTotalAmount() { return totalAmount; }
    public Money getEffectiveAmount() { return effectiveAmount; }
    
    // Return defensive copy
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Order order) {
            return Objects.equals(id, order.id);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

---

## Example 2: Customer Aggregate (Production Code)

This is the actual Customer aggregate from our production codebase:

```java
@AggregateRoot(
    name = "Customer", 
    description = "增強的客戶聚合根，支援完整的消費者功能", 
    boundedContext = "Customer", 
    version = "2.0"
)
public class Customer implements AggregateRootInterface {
    
    private final CustomerId id;
    private final AggregateStateTracker<Customer> stateTracker = new AggregateStateTracker<>(this);
    private CustomerName name;
    private Email email;
    private Phone phone;
    private Address address;
    private MembershipLevel membershipLevel;
    private LocalDate birthDate;
    private LocalDateTime registrationDate;
    private RewardPoints rewardPoints;
    private CustomerStatus status;
    private Money totalSpending;
    
    // Entity collections
    private final List<DeliveryAddress> deliveryAddresses;
    private CustomerPreferences preferences;
    private final List<PaymentMethod> paymentMethods;
    
    /**
     * Constructor for creating new customer
     */
    public Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel,
            LocalDate birthDate,
            LocalDateTime registrationDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipLevel = membershipLevel;
        this.birthDate = birthDate;
        this.registrationDate = registrationDate != null ? registrationDate : LocalDateTime.now();
        this.rewardPoints = RewardPoints.empty();
        this.status = CustomerStatus.ACTIVE;
        this.totalSpending = Money.twd(0);
        this.deliveryAddresses = new ArrayList<>();
        this.preferences = new CustomerPreferences(CustomerPreferencesId.generate());
        this.paymentMethods = new ArrayList<>();
        
        // Collect domain event
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }
    
    /**
     * Reconstruction constructor - for rebuilding from persistence
     * This constructor does NOT publish domain events
     */
    @AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建客戶聚合根")
    protected Customer(
            CustomerId id,
            CustomerName name,
            Email email,
            Phone phone,
            Address address,
            MembershipLevel membershipLevel,
            LocalDate birthDate,
            LocalDateTime registrationDate,
            RewardPoints rewardPoints,
            List<DeliveryAddress> deliveryAddresses,
            CustomerPreferences preferences,
            List<PaymentMethod> paymentMethods,
            CustomerStatus status,
            Money totalSpending) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.membershipLevel = membershipLevel;
        this.birthDate = birthDate;
        this.registrationDate = registrationDate != null ? registrationDate : LocalDateTime.now();
        this.rewardPoints = rewardPoints != null ? rewardPoints : RewardPoints.empty();
        this.status = status != null ? status : CustomerStatus.ACTIVE;
        this.totalSpending = totalSpending != null ? totalSpending : Money.twd(0);
        this.deliveryAddresses = deliveryAddresses != null ? new ArrayList<>(deliveryAddresses) : new ArrayList<>();
        this.preferences = preferences != null ? preferences : new CustomerPreferences(CustomerPreferencesId.generate());
        this.paymentMethods = paymentMethods != null ? new ArrayList<>(paymentMethods) : new ArrayList<>();
        
        // Reconstruction does not publish events
    }
    
    // Business methods
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        // Validate business rules
        validateProfileUpdate(newName, newEmail, newPhone);
        
        // Check if there are any changes
        boolean hasChanges = !Objects.equals(this.name, newName) ||
                !Objects.equals(this.email, newEmail) ||
                !Objects.equals(this.phone, newPhone);
        
        if (hasChanges) {
            // Track changes using state tracker (without generating events)
            stateTracker.trackChange("name", this.name, newName);
            stateTracker.trackChange("email", this.email, newEmail);
            stateTracker.trackChange("phone", this.phone, newPhone);
            
            // Update values
            this.name = newName;
            this.email = newEmail;
            this.phone = newPhone;
            
            // Generate single profile updated event
            collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
        }
    }
    
    public void upgradeMembershipLevel(MembershipLevel newLevel) {
        // Validate business rules
        validateMembershipUpgrade(newLevel);
        
        // Use state tracker to track changes and auto-generate events
        stateTracker.trackChange("membershipLevel", this.membershipLevel, newLevel,
            (oldValue, newValue) -> new MembershipLevelUpgradedEvent(this.id, oldValue, newValue));
        
        this.membershipLevel = newLevel;
        
        // Cross-aggregate operation: notify promotion system to update customer discount eligibility
        CrossAggregateOperation.publishEventIf(this,
            newLevel == MembershipLevel.VIP,
            () -> new CustomerVipUpgradedEvent(this.id, this.membershipLevel, newLevel));
    }
    
    public void updateSpending(Money amount, String orderId, String spendingType) {
        // Validate business rules
        validateSpendingUpdate(amount, orderId, spendingType);
        
        Money oldTotalSpending = this.totalSpending;
        this.totalSpending = this.totalSpending.add(amount);
        
        // Use state tracker to track changes and auto-generate events
        stateTracker.trackChange("totalSpending", oldTotalSpending, this.totalSpending,
            (oldValue, newValue) -> CustomerSpendingUpdatedEvent.create(
                this.id, amount, newValue, orderId, spendingType));
        
        // Check if membership upgrade eligibility is met
        checkMembershipUpgradeEligibility();
    }
    
    // Validation methods
    private void validateProfileUpdate(CustomerName newName, Email newEmail, Phone newPhone) {
        BusinessRuleViolationException.Builder violationBuilder = 
            new BusinessRuleViolationException.Builder("Customer", this.id.getValue());
        
        if (newName == null) {
            violationBuilder.addError("CUSTOMER_NAME_REQUIRED", "客戶姓名不能為空");
        }
        
        if (newEmail == null) {
            violationBuilder.addError("CUSTOMER_EMAIL_REQUIRED", "客戶郵箱不能為空");
        }
        
        if (newPhone == null) {
            violationBuilder.addError("CUSTOMER_PHONE_REQUIRED", "客戶電話不能為空");
        }
        
        // Check for errors and throw exception
        BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
        if (exception != null) {
            throw exception;
        }
    }
    
    private void validateMembershipUpgrade(MembershipLevel newLevel) {
        if (newLevel == null) {
            throw new BusinessRuleViolationException("Customer", this.id.getValue(),
                "MEMBERSHIP_LEVEL_REQUIRED", "會員等級不能為空");
        }
        
        if (newLevel.ordinal() < this.membershipLevel.ordinal()) {
            throw new BusinessRuleViolationException("Customer", this.id.getValue(),
                "MEMBERSHIP_DOWNGRADE_NOT_ALLOWED",
                String.format("不能從 %s 降級到 %s", this.membershipLevel, newLevel));
        }
    }
    
    private void checkMembershipUpgradeEligibility() {
        // Auto-upgrade membership level based on spending amount
        BigDecimal totalAmount = this.totalSpending.getAmount();
        
        if (this.membershipLevel == MembershipLevel.STANDARD &&
                totalAmount.compareTo(BigDecimal.valueOf(10000)) >= 0) {
            upgradeMembershipLevel(MembershipLevel.SILVER);
        } else if (this.membershipLevel == MembershipLevel.SILVER &&
                totalAmount.compareTo(BigDecimal.valueOf(50000)) >= 0) {
            upgradeMembershipLevel(MembershipLevel.GOLD);
        } else if (this.membershipLevel == MembershipLevel.GOLD &&
                totalAmount.compareTo(BigDecimal.valueOf(100000)) >= 0) {
            upgradeMembershipLevel(MembershipLevel.PLATINUM);
        }
    }
    
    // Query methods
    public CustomerId getId() { return id; }
    public CustomerName getName() { return name; }
    public Email getEmail() { return email; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public CustomerStatus getStatus() { return status; }
    public Money getTotalSpending() { return totalSpending; }
    
    // Event management methods are automatically provided by AggregateRootInterface
    // No need to override any methods! All functionality provided by interface default methods:
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
    // - clearEvents()
}
```

---

## Key Patterns

### 1. Invariant Protection

```java
// Always validate before changing state
public void addItem(String productId, String productName, int quantity, Money price) {
    // Check state
    if (status != OrderStatus.CREATED) {
        throw new IllegalStateException(
            "Cannot add items to an order that is not in CREATED state");
    }
    
    // Create order item
    OrderItem item = new OrderItem(productId, productName, quantity, price);
    items.add(item);
    
    // Update total amount
    totalAmount = totalAmount.add(item.getSubtotal());
}
```

### 2. Event Collection with State Tracker

```java
// Use state tracker to automatically generate events
public void submit() {
    validateOrderSubmission();
    
    OrderStatus oldStatus = this.status;
    
    // State tracker tracks changes and auto-generates events
    stateTracker.trackChange("status", oldStatus, OrderStatus.PENDING,
        (oldValue, newValue) -> OrderSubmittedEvent.create(
            this.id, this.customerId.toString(), this.totalAmount, this.items.size()));
    
    status = OrderStatus.PENDING;
}
```

### 3. Cross-Aggregate Operations

```java
// Publish events for cross-aggregate communication
public void submit() {
    // ... state changes ...
    
    // Cross-aggregate operation: notify inventory system
    CrossAggregateOperation.publishEvent(this,
        new OrderInventoryReservationRequestedEvent(
            this.id, this.customerId, this.items));
}
```

### 4. Reconstruction Pattern

```java
/**
 * Reconstruction constructor - for rebuilding from persistence
 * This constructor does NOT publish domain events
 */
@AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建訂單聚合根")
protected Order(
        OrderId id,
        CustomerId customerId,
        String shippingAddress,
        List<OrderItem> items,
        OrderStatus status,
        Money totalAmount,
        Money effectiveAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    // Initialize all fields
    this.id = Objects.requireNonNull(id, "訂單ID不能為空");
    this.customerId = Objects.requireNonNull(customerId, "客戶ID不能為空");
    // ... other fields ...
    
    // Note: No events published during reconstruction
}

/**
 * Post-reconstruction validation
 */
@AggregateReconstruction.PostReconstruction("驗證重建後的訂單聚合根狀態")
public void validateReconstructedState() {
    BusinessRuleViolationException.Builder violationBuilder = 
        new BusinessRuleViolationException.Builder("Order", this.id.getValue());
    
    if (this.id == null) {
        violationBuilder.addError("ORDER_ID_REQUIRED", "訂單ID不能為空");
    }
    
    // ... other validations ...
    
    BusinessRuleViolationException exception = violationBuilder.buildIfHasErrors();
    if (exception != null) {
        throw exception;
    }
}
```

### 5. Defensive Copies

```java
// Return unmodifiable collections
public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
}
```

### 6. No Setters

```java
// ❌ Bad: Setters bypass validation
public void setStatus(OrderStatus status) {
    this.status = status;
}

// ✅ Good: Business methods with validation
public void submit() {
    validateOrderSubmission();
    this.status = OrderStatus.PENDING;
    collectEvent(OrderSubmittedEvent.create(id));
}
```

---

## Common Mistakes

### ❌ Mistake 1: Exposing Mutable Collections

```java
// Bad: Allows external modification
public List<OrderItem> getItems() {
    return items;  // Caller can modify!
}

// Good: Return defensive copy
public List<OrderItem> getItems() {
    return Collections.unmodifiableList(items);
}
```

### ❌ Mistake 2: Publishing Events Directly

```java
// Bad: Publishing from aggregate
public void submit() {
    this.status = OrderStatus.PENDING;
    eventPublisher.publish(new OrderSubmittedEvent(id));  // Wrong!
}

// Good: Collecting events
public void submit() {
    this.status = OrderStatus.PENDING;
    collectEvent(OrderSubmittedEvent.create(id));  // Correct!
}
```

### ❌ Mistake 3: Weak Invariants

```java
// Bad: No validation
public void addItem(Product product, int quantity) {
    items.add(new OrderItem(product, quantity));
}

// Good: Strong validation
public void addItem(String productId, String productName, int quantity, Money price) {
    if (status != OrderStatus.CREATED) {
        throw new IllegalStateException(
            "Cannot add items to an order that is not in CREATED state");
    }
    
    OrderItem item = new OrderItem(productId, productName, quantity, price);
    items.add(item);
}
```

---

## Testing Aggregates

```java
@ExtendWith(MockitoExtension.class)
class OrderTest {
    
    @Test
    void should_collect_event_when_order_submitted() {
        // Given
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"), "台北市信義區");
        order.addItem("PROD-001", "Product 1", 2, Money.twd(100));
        
        // When
        order.submit();
        
        // Then
        assertThat(order.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = order.getUncommittedEvents();
        assertThat(events).anyMatch(e -> e instanceof OrderSubmittedEvent);
    }
    
    @Test
    void should_throw_exception_when_submitting_empty_order() {
        // Given
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"), "台北市信義區");
        
        // When & Then
        assertThatThrownBy(() -> order.submit())
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("Cannot submit an order with no items");
    }
    
    @Test
    void should_not_allow_modification_after_submission() {
        // Given
        Order order = new Order(OrderId.generate(), CustomerId.of("CUST-001"), "台北市信義區");
        order.addItem("PROD-001", "Product 1", 2, Money.twd(100));
        order.submit();
        
        // When & Then
        assertThatThrownBy(() -> order.addItem("PROD-002", "Product 2", 1, Money.twd(50)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot add items to an order that is not in CREATED state");
    }
}
```

---

## Related Patterns

- [Domain Events](domain-events-examples.md)
- [Value Objects](value-objects-examples.md)
- [Repository Pattern](repository-examples.md)

## Further Reading

- [DDD Tactical Patterns](../../steering/ddd-tactical-patterns.md)
- [Domain Events Standards](../../steering/domain-events.md)
