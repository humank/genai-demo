
# Design

## Overview

本指南基於專案中 17 個Aggregate Root的實際實現經驗，提供Aggregate Root設計的Best Practice和具體範例。專案採用混合實現模式，支援兩種Aggregate Root實現方式，並通過註解驅動的方式提供統一的Aggregate Root管理。

## Aggregate Root實現模式

### Guidelines

| 實現模式 | 適用場景 | 優勢 | 劣勢 |
|---------|---------|------|------|
| **介面模式** | 新開發的Aggregate Root | 零 override、類型安全、自動驗證 | 需要理解介面設計 |
| **繼承模式** | 遺留系統整合 | 傳統 OOP 模式、易於理解 | 需要 override 方法 |

### 模式 A: 介面實現模式 (推薦)

```java
@AggregateRoot(
    name = "Customer", 
    description = "增強的CustomerAggregate Root，支援完整的消費者功能", 
    boundedContext = "Customer", 
    version = "2.0"
)
public class Customer implements AggregateRootInterface {
    
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private Phone phone;
    private MembershipLevel membershipLevel;
    private final List<DeliveryAddress> addresses;
    private final List<PaymentMethod> paymentMethods;
    private CustomerPreferences preferences;
    private RewardPoints rewardPoints;
    
    // 建構子
    public Customer(CustomerId id, CustomerName name, Email email, MembershipLevel membershipLevel) {
        this.id = Objects.requireNonNull(id, "Customer ID cannot be null");
        this.name = Objects.requireNonNull(name, "Customer name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.membershipLevel = Objects.requireNonNull(membershipLevel, "Membership level cannot be null");
        this.addresses = new ArrayList<>();
        this.paymentMethods = new ArrayList<>();
        this.preferences = CustomerPreferences.defaultPreferences();
        this.rewardPoints = RewardPoints.zero();
        
        // 收集Domain Event
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }
    
    // 業務方法
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        validateProfileUpdate(newName, newEmail, newPhone);
        
        CustomerName oldName = this.name;
        Email oldEmail = this.email;
        
        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;
        
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
    }
    
    public void addDeliveryAddress(Address address) {
        if (addresses.size() >= 10) {
            throw new TooManyAddressesException("Customer最多只能有 10 個配送地址");
        }
        
        DeliveryAddress deliveryAddress = new DeliveryAddress(
            DeliveryAddressId.generate(),
            address,
            false // 預設非主要地址
        );
        
        this.addresses.add(deliveryAddress);
        
        collectEvent(DeliveryAddressAddedEvent.create(
            this.id, 
            deliveryAddress.getId(), 
            address, 
            addresses.size()
        ));
    }
    
    public void upgradeMembership(MembershipLevel newLevel) {
        if (!canUpgradeTo(newLevel)) {
            throw new InvalidMembershipUpgradeException(
                String.format("無法從 %s 升級到 %s", membershipLevel, newLevel)
            );
        }
        
        MembershipLevel oldLevel = this.membershipLevel;
        this.membershipLevel = newLevel;
        
        collectEvent(MembershipLevelUpgradedEvent.create(this.id, oldLevel, newLevel));
    }
    
    public void earnRewardPoints(int points, String reason) {
        if (points <= 0) {
            throw new IllegalArgumentException("獲得的點數必須大於 0");
        }
        
        this.rewardPoints = this.rewardPoints.add(points);
        
        collectEvent(RewardPointsEarnedEvent.create(this.id, points, reason));
    }
    
    public boolean redeemRewardPoints(int points, String reason) {
        if (!canRedeemPoints(points)) {
            return false;
        }
        
        this.rewardPoints = this.rewardPoints.subtract(points);
        
        collectEvent(RewardPointsRedeemedEvent.create(this.id, points, reason));
        return true;
    }
    
    // 查詢方法
    public boolean isVip() {
        return membershipLevel == MembershipLevel.VIP;
    }
    
    public boolean canRedeemPoints(int points) {
        return rewardPoints.getPoints() >= points && points > 0;
    }
    
    public Optional<DeliveryAddress> getPrimaryAddress() {
        return addresses.stream()
            .filter(DeliveryAddress::isPrimary)
            .findFirst();
    }
    
    // 私有輔助方法
    private void validateProfileUpdate(CustomerName name, Email email, Phone phone) {
        Objects.requireNonNull(name, "Customer name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");
        // phone 可以為 null
    }
    
    private boolean canUpgradeTo(MembershipLevel newLevel) {
        return newLevel.ordinal() > membershipLevel.ordinal();
    }
    
    // Getters
    public CustomerId getId() { return id; }
    public CustomerName getName() { return name; }
    public Email getEmail() { return email; }
    public Phone getPhone() { return phone; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public List<DeliveryAddress> getAddresses() { return List.copyOf(addresses); }
    public RewardPoints getRewardPoints() { return rewardPoints; }
}
```

### 模式 B: 繼承基類模式

```java
@AggregateRoot(
    name = "Product", 
    description = "產品Aggregate Root，管理產品信息和庫存", 
    boundedContext = "Product", 
    version = "1.0"
)
public class Product extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {
    
    private final ProductId id;
    private ProductName name;
    private ProductDescription description;
    private Money price;
    private ProductCategory category;
    private ProductStatus status;
    private final List<ProductImage> images;
    
    public Product(ProductId id, ProductName name, ProductDescription description, 
                   Money price, ProductCategory category) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.price = Objects.requireNonNull(price);
        this.category = Objects.requireNonNull(category);
        this.status = ProductStatus.DRAFT;
        this.images = new ArrayList<>();
        
        // 使用基類方法收集事件
        addDomainEvent(ProductCreatedEvent.create(id, name, category, price));
    }
    
    public void updatePrice(Money newPrice) {
        validatePrice(newPrice);
        
        Money oldPrice = this.price;
        this.price = newPrice;
        
        addDomainEvent(ProductPriceChangedEvent.create(this.id, oldPrice, newPrice));
    }
    
    public void activate() {
        if (status == ProductStatus.ACTIVE) {
            throw new IllegalStateException("產品已經是活躍狀態");
        }
        
        validateCanActivate();
        
        this.status = ProductStatus.ACTIVE;
        
        addDomainEvent(ProductActivatedEvent.create(this.id));
    }
    
    public void discontinue() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("產品已經停產");
        }
        
        this.status = ProductStatus.DISCONTINUED;
        
        addDomainEvent(ProductDiscontinuedEvent.create(this.id));
    }
    
    private void validatePrice(Money price) {
        if (price.isNegativeOrZero()) {
            throw new IllegalArgumentException("產品價格必須大於 0");
        }
    }
    
    private void validateCanActivate() {
        if (name == null || description == null || price == null) {
            throw new IllegalStateException("產品資訊不完整，無法啟用");
        }
    }
    
    // Getters
    public ProductId getId() { return id; }
    public ProductName getName() { return name; }
    public Money getPrice() { return price; }
    public ProductStatus getStatus() { return status; }
}
```

## Design

### 1. 單一職責原則

每個Aggregate Root應該只負責一個業務概念的完整性：

```java
// ✅ 好的設計：Order Aggregate Root只管理訂單相關邏輯
@AggregateRoot(name = "Order", boundedContext = "Order")
public class Order implements AggregateRootInterface {
    
    public void addItem(ProductId productId, int quantity, Money unitPrice) {
        // 訂單項目管理邏輯
    }
    
    public void confirm() {
        // 訂單確認邏輯
    }
    
    public void cancel(String reason) {
        // 訂單取消邏輯
    }
}

// ❌ 不好的設計：混合多個業務概念
public class OrderAndPayment {
    // 同時管理訂單和支付 - 違反單一職責
}
```

### 2. 一致性邊界

Aggregate Root定義了強一致性的邊界：

```java
@AggregateRoot(name = "ShoppingCart", boundedContext = "ShoppingCart")
public class ShoppingCart implements AggregateRootInterface {
    
    private final List<CartItem> items;
    private Money totalAmount;
    
    public void addItem(ProductId productId, int quantity, Money unitPrice) {
        CartItem existingItem = findItem(productId);
        
        if (existingItem != null) {
            // 更新現有項目
            existingItem.updateQuantity(existingItem.getQuantity() + quantity);
        } else {
            // 添加新項目
            CartItem newItem = new CartItem(productId, quantity, unitPrice);
            items.add(newItem);
        }
        
        // 重新計算總金額 - 保持一致性
        recalculateTotalAmount();
        
        collectEvent(CartItemAddedEvent.create(getId(), productId, quantity, unitPrice));
    }
    
    private void recalculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(item -> item.getUnitPrice().multiply(item.getQuantity()))
            .reduce(Money.ZERO, Money::add);
    }
}
```

### Maintenance

Aggregate Root負責維護業務不變性：

```java
@AggregateRoot(name = "Inventory", boundedContext = "Inventory")
public class Inventory extends AggregateRoot {
    
    private int availableQuantity;
    private int reservedQuantity;
    
    public void reserve(OrderId orderId, int quantity) {
        // 業務不變性：可用數量必須足夠
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                String.format("庫存不足：需要 %d，可用 %d", quantity, availableQuantity)
            );
        }
        
        // 維護不變性：總庫存 = 可用 + 已預留
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
        
        addDomainEvent(StockReservedEvent.create(getProductId(), orderId, quantity, availableQuantity));
    }
    
    public void release(OrderId orderId, int quantity) {
        // 業務不變性：預留數量必須足夠
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("預留數量不足");
        }
        
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
        
        addDomainEvent(StockReleasedEvent.create(getProductId(), orderId, quantity, availableQuantity));
    }
}
```

## Aggregate Root生命週期管理

### 1. 創建階段

```java
public class Customer implements AggregateRootInterface {
    
    // Factory方法
    public static Customer createNew(CustomerName name, Email email) {
        CustomerId id = CustomerId.generate();
        return new Customer(id, name, email, MembershipLevel.STANDARD);
    }
    
    // 重建方法 (從持久化載入)
    public static Customer reconstruct(CustomerId id, CustomerName name, Email email, 
                                     MembershipLevel membershipLevel, List<DeliveryAddress> addresses) {
        Customer customer = new Customer(id, name, email, membershipLevel);
        customer.addresses.addAll(addresses);
        // 重建時不發布事件
        customer.markEventsAsCommitted();
        return customer;
    }
}
```

### 2. 狀態轉換

```java
public class Order implements AggregateRootInterface {
    
    private OrderStatus status;
    
    public void submit() {
        validateCanSubmit();
        
        OrderStatus oldStatus = this.status;
        this.status = OrderStatus.SUBMITTED;
        
        collectEvent(OrderSubmittedEvent.create(getId(), getCustomerId(), getTotalAmount(), getItemCount()));
    }
    
    public void confirm() {
        if (status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交的訂單可以確認");
        }
        
        this.status = OrderStatus.CONFIRMED;
        
        collectEvent(OrderConfirmedEvent.create(getId(), getCustomerId(), status, getTotalAmount()));
    }
    
    public void cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException("訂單無法取消");
        }
        
        this.status = OrderStatus.CANCELLED;
        
        collectEvent(OrderCancelledEvent.create(getId(), reason));
    }
    
    private boolean canCancel() {
        return status == OrderStatus.DRAFT || 
               status == OrderStatus.SUBMITTED || 
               status == OrderStatus.CONFIRMED;
    }
}
```

### 3. Aggregate內Entity管理

```java
public class Seller implements AggregateRootInterface {
    
    private final List<SellerRating> ratings;
    private SellerProfile profile;
    private ContactInfo contactInfo;
    
    public void addRating(CustomerId customerId, int rating, String comment) {
        // 業務規則：同一Customer只能評價一次
        if (hasRatingFromCustomer(customerId)) {
            throw new DuplicateRatingException("Customer已經評價過此賣家");
        }
        
        SellerRating newRating = new SellerRating(
            SellerRatingId.generate(),
            customerId,
            rating,
            comment
        );
        
        this.ratings.add(newRating);
        
        collectEvent(SellerRatingAddedEvent.create(getId(), newRating.getId(), rating));
    }
    
    public void updateContactInfo(String email, String phone) {
        this.contactInfo.updateContactInfo(email, phone);
        
        collectEvent(SellerContactInfoUpdatedEvent.create(getId(), email, phone));
    }
    
    public double calculateAverageRating() {
        return ratings.stream()
            .filter(SellerRating::isVisible)
            .mapToInt(SellerRating::getRating)
            .average()
            .orElse(0.0);
    }
    
    private boolean hasRatingFromCustomer(CustomerId customerId) {
        return ratings.stream()
            .anyMatch(rating -> rating.getCustomerId().equals(customerId) && rating.isVisible());
    }
}
```

## Aggregate Root間協作

### 1. 通過Domain Event協作

```java
// Order Aggregate Root發布事件
public class Order implements AggregateRootInterface {
    
    public void submit() {
        // ... 狀態變更邏輯
        
        // 發布事件，觸發其他Aggregate Root的處理
        collectEvent(OrderSubmittedEvent.create(getId(), getCustomerId(), getItems()));
    }
}

// 事件處理器協調其他Aggregate Root
@Component
public class OrderSubmittedEventHandler extends AbstractDomainEventHandler<OrderSubmittedEvent> {
    
    @Override
    protected void handleEvent(OrderSubmittedEvent event) {
        // 預留庫存
        inventoryService.reserveStock(event.orderId(), event.items());
        
        // 處理支付
        paymentService.processPayment(event.orderId(), event.totalAmount());
        
        // 更新Customer統計
        customerService.updateOrderStatistics(event.customerId());
    }
}
```

### 2. 通過Domain Service協作

```java
@DomainService(name = "OrderProcessingService", boundedContext = "Order")
public class OrderProcessingService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    
    @Transactional
    public void processOrder(OrderId orderId) {
        // 載入訂單Aggregate Root
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // 檢查庫存
        boolean stockAvailable = inventoryService.checkAvailability(order.getItems());
        if (!stockAvailable) {
            order.markAsOutOfStock();
            return;
        }
        
        // 預留庫存
        inventoryService.reserveStock(orderId, order.getItems());
        
        // 確認訂單
        order.confirm();
        
        // 保存變更
        orderRepository.save(order);
    }
}
```

## Testing

### Testing

```java
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    
    @Test
    void should_collect_customer_created_event_when_customer_is_created() {
        // Given
        CustomerId customerId = CustomerId.generate();
        CustomerName name = new CustomerName("John Doe");
        Email email = new Email("john@example.com");
        
        // When
        Customer customer = new Customer(customerId, name, email, MembershipLevel.STANDARD);
        
        // Then
        assertThat(customer.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerCreatedEvent.class);
    }
    
    @Test
    void should_upgrade_membership_when_conditions_are_met() {
        // Given
        Customer customer = createStandardCustomer();
        
        // When
        customer.upgradeMembership(MembershipLevel.PREMIUM);
        
        // Then
        assertThat(customer.getMembershipLevel()).isEqualTo(MembershipLevel.PREMIUM);
        assertThat(customer.getUncommittedEvents())
            .anyMatch(event -> event instanceof MembershipLevelUpgradedEvent);
    }
    
    @Test
    void should_throw_exception_when_upgrading_to_lower_level() {
        // Given
        Customer customer = createPremiumCustomer();
        
        // When & Then
        assertThatThrownBy(() -> customer.upgradeMembership(MembershipLevel.STANDARD))
            .isInstanceOf(InvalidMembershipUpgradeException.class);
    }
}
```

### Testing

```java
@SpringBootTest
@Transactional
class CustomerIntegrationTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private DomainEventApplicationService domainEventService;
    
    @Test
    void should_persist_customer_and_publish_events() {
        // Given
        Customer customer = Customer.createNew(
            new CustomerName("John Doe"),
            new Email("john@example.com")
        );
        
        // When
        customerRepository.save(customer);
        domainEventService.publishEventsFromAggregate(customer);
        
        // Then
        Customer savedCustomer = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(savedCustomer.getName()).isEqualTo(customer.getName());
        assertThat(savedCustomer.hasUncommittedEvents()).isFalse();
    }
}
```

### Testing

```gherkin
Feature: Customer會員等級管理
  
  Scenario: 標準會員升級為高級會員
    Given 一個標準會員Customer
    When Customer升級會員等級為高級會員
    Then Customer的會員等級應該是高級會員
    And 應該發布會員等級升級事件
  
  Scenario: 嘗試降級會員等級
    Given 一個高級會員Customer
    When Customer嘗試降級會員等級為標準會員
    Then 應該拋出無效升級異常
    And Customer的會員等級應該保持不變
```

## 效能考量

### 1. Aggregate Root大小控制

```java
// ✅ 好的設計：控制Aggregate Root大小
public class Order implements AggregateRootInterface {
    
    private static final int MAX_ITEMS = 100;
    private final List<OrderItem> items;
    
    public void addItem(ProductId productId, int quantity, Money unitPrice) {
        if (items.size() >= MAX_ITEMS) {
            throw new TooManyItemsException("訂單項目不能超過 " + MAX_ITEMS + " 個");
        }
        
        // ... 添加邏輯
    }
}

// ❌ 不好的設計：無限制的Aggregate Root
public class Customer {
    private final List<Order> allOrders; // 可能包含數千個訂單
}
```

### 2. 延遲載入

```java
public class Seller implements AggregateRootInterface {
    
    // 避免一次載入所有評級
    public List<SellerRating> getRecentRatings(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return ratings.stream()
            .filter(rating -> rating.getRatedAt().isAfter(cutoff))
            .filter(SellerRating::isVisible)
            .sorted((r1, r2) -> r2.getRatedAt().compareTo(r1.getRatedAt()))
            .limit(10) // 限制數量
            .toList();
    }
}
```

## Related Diagrams

- [CustomerAggregate Root詳細圖](../../../diagrams/viewpoints/functional/customer-aggregate-details.puml)
- [訂單Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/order-aggregate-details.puml)
- [產品Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/product-aggregate-details.puml)
- [賣家Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/seller-aggregate-details.puml)
- [領域模型概覽圖](../../../diagrams/viewpoints/functional/domain-model-overview.puml)
- [支付Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/payment-aggregate-details.puml)
- [庫存Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/inventory-aggregate-details.puml)
- [評價Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/review-aggregate-details.puml)
- [購物車Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/shoppingcart-aggregate-details.puml)
- [促銷Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/promotion-aggregate-details.puml)
- [定價Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/pricing-aggregate-details.puml)
- [通知Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/notification-aggregate-details.puml)
- [配送Aggregate Root詳細圖](../../../diagrams/viewpoints/functional/delivery-aggregate-details.puml)
- [ObservabilityAggregate Root詳細圖](../../../diagrams/viewpoints/functional/observability-aggregate-details.puml)

## Relationships with Other Viewpoints

- **[Information Viewpoint](../information/README.md)**: Domain Event設計和Aggregate Root間通信
- **[Concurrency Viewpoint](../concurrency/README.md)**: Aggregate Root的交易邊界和並發控制
- **[Development Viewpoint](../development/README.md)**: Aggregate Root的測試Policy和程式碼組織

## Best Practices

1. **明確邊界**: 每個Aggregate Root有清晰的業務邊界和職責
2. **保持小型**: 控制Aggregate Root大小，避免Performance問題
3. **強一致性**: Aggregate Root內部保持強一致性
4. **事件驅動**: 通過Domain Event實現Aggregate Root間協作
5. **不變性維護**: 確保業務規則和不變性得到維護
6. **測試覆蓋**: 完整的Unit Test和Integration Test
7. **版本管理**: 支援Aggregate Root結構的演進
8. **效能優化**: 考慮載入Policy和查詢優化

這套Aggregate Root設計指南確保了領域模型的正確性、Maintainability和高效能。