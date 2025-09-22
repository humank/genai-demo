# 聚合根設計指南

## 概覽

本指南基於專案中 15 個聚合根的實際實現經驗，提供聚合根設計的最佳實踐和具體範例。專案採用混合實現模式，支援兩種聚合根實現方式，並通過註解驅動的方式提供統一的聚合根管理。

## 當前專案聚合根概覽

### 聚合根分佈統計

| 界限上下文 | 聚合根數量 | 主要聚合根 | 實現模式 | 版本 |
|-----------|-----------|-----------|----------|------|
| Customer | 1 | Customer | Interface | 2.0 |
| Order | 1 | Order | Interface | 1.0 |
| Product | 1 | Product | Inheritance | 1.0 |
| Inventory | 1 | Inventory | Inheritance | 1.0 |
| Payment | 1 | Payment | Inheritance | 1.0 |
| Delivery | 1 | Delivery | Inheritance | 1.0 |
| Review | 1 | ProductReview | Interface | 2.0 |
| Seller | 1 | Seller | Interface | 2.0 |
| ShoppingCart | 1 | ShoppingCart | Inheritance | 1.0 |
| Promotion | 1 | Promotion | Inheritance | 1.0 |
| Pricing | 1 | PricingRule | Inheritance | 1.0 |
| Notification | 1 | Notification | Interface | 1.0 |
| Observability | 2 | ObservabilitySession, AnalyticsSession | Interface | 1.0 |

**總計**: 15 個聚合根，13 個界限上下文

## 聚合根實現模式

### 模式選擇指南

| 實現模式 | 適用場景 | 優勢 | 劣勢 | 專案使用情況 |
|---------|---------|------|------|-------------|
| **介面模式** | 新開發的聚合根 | 零 override、類型安全、自動驗證 | 需要理解介面設計 | 7個聚合根 |
| **繼承模式** | 遺留系統整合 | 傳統 OOP 模式、易於理解 | 需要 override 方法 | 8個聚合根 |

### 模式 A: 介面實現模式 (推薦)

基於專案實際實現的 `Customer` 聚合根：

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
    
    // Entity 集合
    private final List<DeliveryAddress> deliveryAddresses;
    private CustomerPreferences preferences;
    private final List<PaymentMethod> paymentMethods;
    
    // 主要建構子
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

        // 發布客戶創建事件
        collectEvent(CustomerCreatedEvent.create(id, name, email, membershipLevel));
    }
    
    // 業務方法
    
    /** 更新個人資料 */
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        // 驗證業務規則
        validateProfileUpdate(newName, newEmail, newPhone);

        // 檢查是否有任何變化
        boolean hasChanges = !Objects.equals(this.name, newName) ||
                !Objects.equals(this.email, newEmail) ||
                !Objects.equals(this.phone, newPhone);

        if (hasChanges) {
            // 使用狀態追蹤器追蹤變化（不產生事件）
            stateTracker.trackChange("name", this.name, newName);
            stateTracker.trackChange("email", this.email, newEmail);
            stateTracker.trackChange("phone", this.phone, newPhone);

            // 更新值
            this.name = newName;
            this.email = newEmail;
            this.phone = newPhone;

            // 產生單一的個人資料更新事件
            collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
        }
    }
    
    /** 添加配送地址 */
    public DeliveryAddressId addDeliveryAddress(Address address, String label) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        if (deliveryAddresses.size() >= 10) {
            throw new IllegalArgumentException("Cannot have more than 10 delivery addresses");
        }

        // 檢查是否已存在相同地址
        boolean addressExists = deliveryAddresses.stream()
                .anyMatch(da -> da.isSameAddress(address));
        if (addressExists) {
            throw new IllegalArgumentException("Address already exists");
        }

        DeliveryAddress deliveryAddress = new DeliveryAddress(
                DeliveryAddressId.generate(), address, label);

        // 如果是第一個地址，自動設為預設
        if (deliveryAddresses.isEmpty()) {
            deliveryAddress.setAsDefault();
        }

        deliveryAddresses.add(deliveryAddress);

        // 發布配送地址添加事件
        collectEvent(DeliveryAddressAddedEvent.create(this.id, address, deliveryAddresses.size()));

        return deliveryAddress.getId();
    }
    
    /** 升級會員等級 */
    public void upgradeMembershipLevel(MembershipLevel newLevel) {
        // 驗證業務規則
        validateMembershipUpgrade(newLevel);

        // 使用狀態追蹤器追蹤變化並自動產生事件
        stateTracker.trackChange("membershipLevel", this.membershipLevel, newLevel,
                (oldValue, newValue) -> new MembershipLevelUpgradedEvent(this.id, oldValue, newValue));

        this.membershipLevel = newLevel;

        // 跨聚合根操作：通知促銷系統更新客戶折扣資格
        CrossAggregateOperation.publishEventIf(this,
                newLevel == MembershipLevel.VIP,
                () -> new CustomerVipUpgradedEvent(this.id, this.membershipLevel, newLevel));
    }
    
    /** 添加紅利點數 */
    public void addRewardPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.add(points);

        // 發布紅利點數獲得事件
        collectEvent(RewardPointsEarnedEvent.create(this.id, points, this.rewardPoints.balance(), reason));
    }
    
    /** 兌換紅利點數 */
    public void redeemPoints(int points, String reason) {
        this.rewardPoints = this.rewardPoints.redeem(points);

        // 發布紅利點數兌換事件
        collectEvent(RewardPointsRedeemedEvent.create(
                this.id, points, this.rewardPoints.balance(), reason));
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

基於專案實際實現的 `Product` 聚合根：

```java
@AggregateRoot(
    name = "Product", 
    description = "產品聚合根，管理產品信息和庫存", 
    boundedContext = "Product", 
    version = "1.0"
)
public class Product extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {
    
    private final ProductId id;
    private ProductName name;
    private ProductDescription description;
    private Money price;
    private final ProductCategory category;
    private StockQuantity stockQuantity;
    private boolean inStock;
    private boolean isActive;
    
    public Product(
            ProductId id,
            ProductName name,
            ProductDescription description,
            Money price,
            ProductCategory category,
            StockQuantity stockQuantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stockQuantity = stockQuantity;
        this.inStock = stockQuantity.getValue() > 0;
        this.isActive = true;

        // 發布商品創建事件
        collectEvent(ProductCreatedEvent.create(id, name, price, category));
    }
    
    /** 更新商品價格 */
    public void updatePrice(Money newPrice) {
        if (newPrice == null) {
            throw new IllegalArgumentException("商品價格不能為空");
        }

        Money oldPrice = this.price;
        this.price = newPrice;

        // 發布商品價格變更事件
        collectEvent(new ProductPriceChangedEvent(this.id, oldPrice, newPrice));
    }

    /** 更新庫存 */
    public void updateStock(StockQuantity newStock) {
        if (newStock == null) {
            throw new IllegalArgumentException("庫存數量不能為空");
        }

        StockQuantity oldStock = this.stockQuantity;
        this.stockQuantity = newStock;
        this.inStock = newStock.getValue() > 0;

        // 發布商品庫存更新事件
        collectEvent(new ProductStockUpdatedEvent(this.id, oldStock, newStock));
    }

    /** 下架商品 */
    public void discontinue(String reason) {
        if (!this.isActive) {
            throw new IllegalStateException("商品已經下架");
        }

        this.isActive = false;

        // 發布商品下架事件
        collectEvent(new ProductDiscontinuedEvent(this.id, reason));
    }

    /** 重新上架商品 */
    public void activate() {
        if (this.isActive) {
            throw new IllegalStateException("商品已經上架");
        }

        this.isActive = true;

        // 發布商品重新上架事件
        collectEvent(ProductActivatedEvent.create(this.id));
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

## 核心架構特性

### 1. 註解驅動設計

所有聚合根都必須使用 `@AggregateRoot` 註解，提供統一的元數據管理：

```java
@AggregateRoot(
    name = "聚合根名稱",           // 必填：聚合根識別名稱
    description = "聚合根描述",     // 必填：業務描述
    boundedContext = "上下文名稱", // 必填：所屬界限上下文
    version = "版本號",           // 必填：聚合根版本
    enableEventCollection = true  // 可選：是否啟用事件收集（預設true）
)
```

### 2. 零 Override 設計

介面模式聚合根無需重寫任何方法，所有事件管理功能都由 `AggregateRootInterface` 的 default 方法提供：

```java
public interface AggregateRootInterface {
    // 自動事件收集
    default void collectEvent(DomainEvent event) { ... }
    
    // 自動事件管理
    default List<DomainEvent> getUncommittedEvents() { ... }
    default void markEventsAsCommitted() { ... }
    default boolean hasUncommittedEvents() { ... }
    
    // 自動元數據管理
    default String getAggregateRootName() { ... }
    default String getBoundedContext() { ... }
    default String getVersion() { ... }
}
```

### 3. 狀態追蹤器 (AggregateStateTracker)

專案中的 `Customer` 聚合根使用了先進的狀態追蹤器模式：

```java
public class Customer implements AggregateRootInterface {
    private final AggregateStateTracker<Customer> stateTracker = new AggregateStateTracker<>(this);
    
    public void upgradeMembershipLevel(MembershipLevel newLevel) {
        // 使用狀態追蹤器追蹤變化並自動產生事件
        stateTracker.trackChange("membershipLevel", this.membershipLevel, newLevel,
                (oldValue, newValue) -> new MembershipLevelUpgradedEvent(this.id, oldValue, newValue));
        
        this.membershipLevel = newLevel;
    }
}
```

### 4. 跨聚合根操作 (CrossAggregateOperation)

支援條件式跨聚合根事件發布：

```java
// 跨聚合根操作：通知促銷系統更新客戶折扣資格
CrossAggregateOperation.publishEventIf(this,
        newLevel == MembershipLevel.VIP,
        () -> new CustomerVipUpgradedEvent(this.id, this.membershipLevel, newLevel));
```

### 5. 聚合重建支援 (AggregateReconstruction)

支援從持久化狀態重建聚合根，不產生領域事件：

```java
@AggregateReconstruction.ReconstructionConstructor("從持久化狀態重建客戶聚合根")
protected Customer(CustomerId id, CustomerName name, ...) {
    // 重建邏輯，不發布事件
}
```

## 聚合根設計原則

### 1. 單一職責原則

每個聚合根應該只負責一個業務概念的完整性：

```java
// ✅ 好的設計：ShoppingCart 聚合根只管理購物車相關邏輯
@AggregateRoot(name = "ShoppingCart", description = "購物車聚合根，管理消費者的購物車狀態和商品項目", 
               boundedContext = "ShoppingCart", version = "1.0")
public class ShoppingCart extends AggregateRoot {
    
    public void addItem(ProductId productId, int quantity, Money unitPrice) {
        // 購物車項目管理邏輯
        if (quantity <= 0) {
            throw new InvalidQuantityException("商品數量必須大於 0");
        }
        
        Optional<CartItem> existingItem = findItemOptional(productId);
        if (existingItem.isPresent()) {
            CartItem updatedItem = existingItem.get().increaseQuantity(quantity);
            replaceItem(existingItem.get(), updatedItem);
        } else {
            CartItem newItem = new CartItem(productId, quantity, unitPrice);
            items.add(newItem);
        }
        
        collectEvent(CartItemAddedEvent.create(this.id, this.consumerId, productId, quantity, unitPrice));
    }
    
    public void checkout() {
        // 購物車結帳邏輯
        if (isEmpty()) {
            throw new IllegalStateException("無法結帳空的購物車");
        }
        
        updateStatus(ShoppingCartStatus.CHECKED_OUT);
        collectEvent(CartCheckedOutEvent.create(this.id, this.consumerId, items, calculateTotal(), getTotalQuantity()));
    }
}

// ❌ 不好的設計：混合多個業務概念
public class OrderAndPaymentAndDelivery {
    // 同時管理訂單、支付和配送 - 違反單一職責
}
```

### 2. 一致性邊界

聚合根定義了強一致性的邊界，基於實際的 `Customer` 聚合根實現：

```java
@AggregateRoot(name = "Customer", description = "增強的客戶聚合根，支援完整的消費者功能", 
               boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {
    
    private final List<DeliveryAddress> deliveryAddresses;
    private final List<PaymentMethod> paymentMethods;
    private RewardPoints rewardPoints;
    private Money totalSpending;
    
    public DeliveryAddressId addDeliveryAddress(Address address, String label) {
        // 業務規則驗證 - 保持一致性
        if (deliveryAddresses.size() >= 10) {
            throw new IllegalArgumentException("Cannot have more than 10 delivery addresses");
        }
        
        boolean addressExists = deliveryAddresses.stream()
                .anyMatch(da -> da.isSameAddress(address));
        if (addressExists) {
            throw new IllegalArgumentException("Address already exists");
        }

        DeliveryAddress deliveryAddress = new DeliveryAddress(
                DeliveryAddressId.generate(), address, label);

        // 如果是第一個地址，自動設為預設 - 維護一致性
        if (deliveryAddresses.isEmpty()) {
            deliveryAddress.setAsDefault();
        }

        deliveryAddresses.add(deliveryAddress);
        
        // 發布事件
        collectEvent(DeliveryAddressAddedEvent.create(this.id, address, deliveryAddresses.size()));
        
        return deliveryAddress.getId();
    }
    
    public void updateSpending(Money amount, String orderId, String spendingType) {
        // 驗證業務規則
        validateSpendingUpdate(amount, orderId, spendingType);

        Money oldTotalSpending = this.totalSpending;
        this.totalSpending = this.totalSpending.add(amount);

        // 使用狀態追蹤器維護一致性
        stateTracker.trackChange("totalSpending", oldTotalSpending, this.totalSpending,
                (oldValue, newValue) -> CustomerSpendingUpdatedEvent.create(
                        this.id, amount, newValue, orderId, spendingType));

        // 檢查是否達到會員升級條件 - 跨屬性一致性
        checkMembershipUpgradeEligibility();
    }
}
```

### 3. 不變性維護

聚合根負責維護業務不變性，基於實際的 `Product` 聚合根實現：

```java
@AggregateRoot(name = "Product", description = "產品聚合根，管理產品信息和庫存", 
               boundedContext = "Product", version = "1.0")
public class Product extends AggregateRoot {
    
    private StockQuantity stockQuantity;
    private boolean inStock;
    private boolean isActive;
    
    public void updateStock(StockQuantity newStock) {
        // 業務不變性：庫存數量不能為空
        if (newStock == null) {
            throw new IllegalArgumentException("庫存數量不能為空");
        }

        StockQuantity oldStock = this.stockQuantity;
        this.stockQuantity = newStock;
        
        // 維護不變性：庫存狀態與數量一致
        this.inStock = newStock.getValue() > 0;

        // 發布商品庫存更新事件
        collectEvent(new ProductStockUpdatedEvent(this.id, oldStock, newStock));
    }
    
    public void discontinue(String reason) {
        // 業務不變性：只有活躍商品可以下架
        if (!this.isActive) {
            throw new IllegalStateException("商品已經下架");
        }

        this.isActive = false;

        // 發布商品下架事件
        collectEvent(new ProductDiscontinuedEvent(this.id, reason));
    }
    
    public boolean canBePurchased() {
        // 業務不變性：可購買 = 活躍 + 有庫存
        return isActive && inStock;
    }
}
```

## 聚合根生命週期管理

### 1. 創建階段

```java
public class Customer implements AggregateRootInterface {
    
    // 工廠方法
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

### 3. 聚合內實體管理

```java
public class Seller implements AggregateRootInterface {
    
    private final List<SellerRating> ratings;
    private SellerProfile profile;
    private ContactInfo contactInfo;
    
    public void addRating(CustomerId customerId, int rating, String comment) {
        // 業務規則：同一客戶只能評價一次
        if (hasRatingFromCustomer(customerId)) {
            throw new DuplicateRatingException("客戶已經評價過此賣家");
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

## 聚合根間協作

### 1. 通過領域事件協作

```java
// Order 聚合根發布事件
public class Order implements AggregateRootInterface {
    
    public void submit() {
        // ... 狀態變更邏輯
        
        // 發布事件，觸發其他聚合根的處理
        collectEvent(OrderSubmittedEvent.create(getId(), getCustomerId(), getItems()));
    }
}

// 事件處理器協調其他聚合根
@Component
public class OrderSubmittedEventHandler extends AbstractDomainEventHandler<OrderSubmittedEvent> {
    
    @Override
    protected void handleEvent(OrderSubmittedEvent event) {
        // 預留庫存
        inventoryService.reserveStock(event.orderId(), event.items());
        
        // 處理支付
        paymentService.processPayment(event.orderId(), event.totalAmount());
        
        // 更新客戶統計
        customerService.updateOrderStatistics(event.customerId());
    }
}
```

### 2. 通過領域服務協作

```java
@DomainService(name = "OrderProcessingService", boundedContext = "Order")
public class OrderProcessingService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    
    @Transactional
    public void processOrder(OrderId orderId) {
        // 載入訂單聚合根
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

## 聚合根測試策略

### 1. 單元測試

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

### 2. 整合測試

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

### 3. BDD 測試

```gherkin
Feature: 客戶會員等級管理
  
  Scenario: 標準會員升級為高級會員
    Given 一個標準會員客戶
    When 客戶升級會員等級為高級會員
    Then 客戶的會員等級應該是高級會員
    And 應該發布會員等級升級事件
  
  Scenario: 嘗試降級會員等級
    Given 一個高級會員客戶
    When 客戶嘗試降級會員等級為標準會員
    Then 應該拋出無效升級異常
    And 客戶的會員等級應該保持不變
```

## 效能考量

### 1. 聚合根大小控制

```java
// ✅ 好的設計：控制聚合根大小
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

// ❌ 不好的設計：無限制的聚合根
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

## 相關圖表

- [客戶聚合根詳細圖](../../diagrams/generated/functional/Customer%20Aggregate%20Details.png)
- [訂單聚合根詳細圖](../../diagrams/generated/functional/Order%20Aggregate%20Details.png)
- [產品聚合根詳細圖](../../diagrams/generated/functional/Product%20Aggregate%20Details.png)
- [賣家聚合根詳細圖](../../diagrams/generated/functional/Seller%20Aggregate%20Details.png)
- [領域模型概覽圖](../../diagrams/generated/functional/Domain%20Model%20Overview.png)
- [支付聚合根詳細圖](../../diagrams/generated/functional/Payment%20Aggregate%20Details.png)
- [庫存聚合根詳細圖](../../diagrams/generated/functional/Inventory%20Aggregate%20Details.png)
- [評價聚合根詳細圖](../../diagrams/generated/functional/Review%20Aggregate%20Details.png)
- [購物車聚合根詳細圖](../../diagrams/generated/functional/ShoppingCart%20Aggregate%20Details.png)
- [促銷聚合根詳細圖](../../diagrams/generated/functional/Promotion%20Aggregate%20Details.png)
- [定價聚合根詳細圖](../../diagrams/generated/functional/Pricing%20Aggregate%20Details.png)
- [通知聚合根詳細圖](../../diagrams/generated/functional/Notification%20Aggregate%20Details.png)
- [配送聚合根詳細圖](../../diagrams/generated/functional/Delivery%20Aggregate%20Details.png)
- [可觀測性聚合根詳細圖](../../diagrams/generated/functional/Observability%20Aggregate%20Details.png)

## 與其他視點的關聯

- **[資訊視點](../information/README.md)**: 領域事件設計和聚合根間通信
- **[並發視點](../concurrency/README.md)**: 聚合根的交易邊界和並發控制
- **[開發視點](../development/README.md)**: 聚合根的測試策略和程式碼組織

## 最佳實踐總結

1. **明確邊界**: 每個聚合根有清晰的業務邊界和職責
2. **保持小型**: 控制聚合根大小，避免性能問題
3. **強一致性**: 聚合根內部保持強一致性
4. **事件驅動**: 通過領域事件實現聚合根間協作
5. **不變性維護**: 確保業務規則和不變性得到維護
6. **測試覆蓋**: 完整的單元測試和整合測試
7. **版本管理**: 支援聚合根結構的演進
8. **效能優化**: 考慮載入策略和查詢優化

這套聚合根設計指南確保了領域模型的正確性、可維護性和高效能。

