# DDD 戰術模式實作指南

## 概述

本指南基於專案中的實際 DDD 實作，提供了完整的戰術模式使用指南，包括聚合根、值對象、領域事件和實體的設計與實作最佳實踐。本文檔整合了專案中的 DDD Entity 設計經驗，並提供了 @AggregateRoot、@ValueObject、@DomainService 註解的詳細使用指南。

## DDD 註解使用指南

### @AggregateRoot 註解使用

`@AggregateRoot` 註解用於標識聚合根，提供聚合的元數據和配置：

```java
@AggregateRoot(
    name = "Customer",                    // 聚合根名稱
    description = "客戶聚合根",            // 中文描述
    boundedContext = "Customer",          // 所屬限界上下文
    version = "2.0",                     // 版本號
    enableEventCollection = true         // 是否啟用事件收集
)
public class Customer implements AggregateRootInterface {
    // 實作內容
}
```

**註解屬性說明：**
- `name`: 聚合根的業務名稱，用於識別和文檔
- `description`: 中文描述，便於理解業務含義
- `boundedContext`: 所屬的限界上下文，用於架構分析
- `version`: 版本號，用於演進管理
- `enableEventCollection`: 是否啟用事件收集機制

### @ValueObject 註解使用

`@ValueObject` 註解用於標識值對象，強調不可變性和值語義：

```java
@ValueObject(name = "CustomerId", description = "客戶唯一標識符")
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
}
```

**使用原則：**
- 優先使用 Java Record 實作
- 在 compact constructor 中進行驗證
- 提供靜態工廠方法
- 確保不可變性

### @DomainService 註解使用

`@DomainService` 註解用於標識領域服務，處理跨聚合的業務邏輯：

```java
@DomainService(
    name = "CustomerValidationService",
    description = "客戶驗證領域服務",
    boundedContext = "Customer"
)
@Component
public class CustomerValidationService {
    
    public boolean isEmailUnique(Email email, CustomerId excludeCustomerId) {
        // 跨聚合的唯一性檢查邏輯
        return customerRepository.findByEmail(email)
            .map(Customer::getId)
            .filter(id -> !id.equals(excludeCustomerId))
            .isEmpty();
    }
    
    public ValidationResult validateCustomerData(CustomerData data) {
        // 複雜的跨領域驗證邏輯
        return ValidationResult.builder()
            .addCheck("email", validateEmail(data.email()))
            .addCheck("phone", validatePhone(data.phone()))
            .build();
    }
}
```

**領域服務設計原則：**
- 無狀態設計
- 處理跨聚合的業務邏輯
- 不包含基礎設施關注點
- 使用依賴注入獲取所需的 Repository

## 聚合根 (Aggregate Root) 設計

### 混搭方案：Annotation + Interface

專案採用創新的混搭方案，結合註解驅動和介面預設方法的優點：

```java
@AggregateRoot(name = "Customer", description = "客戶聚合根", 
               boundedContext = "Customer", version = "2.0")
public class Customer implements AggregateRootInterface {
    
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private Phone phone;
    
    public Customer(CustomerId id, CustomerName name, Email email, Phone phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        
        // 發布客戶創建事件
        collectEvent(CustomerCreatedEvent.create(id, name, email, MembershipLevel.STANDARD));
    }
    
    public void updateProfile(CustomerName newName, Email newEmail, Phone newPhone) {
        // 1. 執行業務邏輯驗證
        validateProfileUpdate(newName, newEmail, newPhone);
        
        // 2. 更新狀態
        this.name = newName;
        this.email = newEmail;
        this.phone = newPhone;
        
        // 3. 收集領域事件
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail, newPhone));
    }
    
    // === 聚合根事件管理方法由 AggregateRootInterface 自動提供 ===
    // 無需 override 任何方法！所有功能都由 interface default methods 提供：
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
    // - getAggregateRootName()
    // - getBoundedContext()
    // - getVersion()
}
```

**優點**:
- ✅ **編譯時約束**: 必須實作 AggregateRootInterface，IDE 會提示
- ✅ **零 override**: 所有事件管理方法都有 default 實作
- ✅ **註解驅動**: 通過 @AggregateRoot 提供元數據
- ✅ **自動驗證**: 在 default 方法中自動檢查註解

### 聚合根註解配置

```java
@AggregateRoot(
    name = "Customer",                    // 聚合根名稱
    description = "客戶聚合根",            // 描述
    boundedContext = "Customer",          // 所屬限界上下文
    version = "2.0",                     // 版本號
    enableEventCollection = true         // 是否啟用事件收集
)
```

### 事件收集機制

```java
public interface AggregateRootInterface {
    
    default void collectEvent(DomainEvent event) {
        getEventCollector().collectEvent(event);
    }
    
    default List<DomainEvent> getUncommittedEvents() {
        return getEventCollector().getUncommittedEvents();
    }
    
    default void markEventsAsCommitted() {
        getEventCollector().markEventsAsCommitted();
    }
    
    default boolean hasUncommittedEvents() {
        return getEventCollector().hasUncommittedEvents();
    }
}
```

## 值對象 (Value Object) 設計

### Record 實作模式 (推薦)

專案廣泛使用 Java Record 實作值對象，確保不可變性：

```java
@ValueObject(name = "CustomerId", description = "客戶唯一標識符")
public record CustomerId(String value) {
    
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID().toString());
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
}
```

### 複合值對象

```java
@ValueObject(name = "RewardPoints", description = "紅利點數值對象")
public record RewardPoints(int balance, LocalDateTime lastUpdated) {
    
    public RewardPoints {
        if (balance < 0) {
            throw new IllegalArgumentException("Reward points balance cannot be negative");
        }
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }
    
    public static RewardPoints empty() {
        return new RewardPoints(0, LocalDateTime.now());
    }
    
    public RewardPoints add(int points) {
        return new RewardPoints(balance + points, LocalDateTime.now());
    }
    
    public RewardPoints subtract(int points) {
        if (points > balance) {
            throw new IllegalArgumentException("Insufficient reward points");
        }
        return new RewardPoints(balance - points, LocalDateTime.now());
    }
    
    public boolean canRedeem(int points) {
        return balance >= points;
    }
}
```

### 枚舉值對象

```java
@ValueObject
public enum CustomerStatus {
    ACTIVE("活躍"),
    INACTIVE("非活躍"),
    SUSPENDED("暫停"),
    DELETED("已刪除");
    
    private final String description;
    
    CustomerStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    public boolean canPlaceOrder() {
        return this == ACTIVE;
    }
}
```

### Money 值對象

```java
@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.TWD);
    }
    
    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.USD);
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
    
    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }
    
    public boolean isGreaterThan(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies");
        }
        return amount.compareTo(other.amount) > 0;
    }
}
```

## 領域事件 (Domain Event) 設計

### Record 實作模式

專案使用 Record 實作領域事件，確保不可變性和簡潔性：

```java
public record CustomerCreatedEvent(
        CustomerId customerId,
        CustomerName customerName,
        Email email,
        MembershipLevel membershipLevel,
        UUID eventId,
        LocalDateTime occurredOn) implements DomainEvent {

    /**
     * 工廠方法，自動設定 eventId 和 occurredOn
     */
    public static CustomerCreatedEvent create(
            CustomerId customerId, 
            CustomerName customerName, 
            Email email,
            MembershipLevel membershipLevel) {
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
```

### DomainEvent 介面設計

```java
public interface DomainEvent extends Serializable {
    
    UUID getEventId();
    LocalDateTime getOccurredOn();
    String getEventType();
    String getAggregateId();
    
    /**
     * 從類別名稱自動推導事件類型
     */
    static String getEventTypeFromClass(Class<? extends DomainEvent> eventClass) {
        String className = eventClass.getSimpleName();
        if (className.endsWith("Event")) {
            return className.substring(0, className.length() - 5);
        }
        return className;
    }
    
    /**
     * 創建事件元數據的輔助方法
     */
    static EventMetadata createEventMetadata() {
        return new EventMetadata(UUID.randomUUID(), LocalDateTime.now());
    }
    
    record EventMetadata(UUID eventId, LocalDateTime occurredOn) {}
}
```

### 業務邏輯增強的事件

```java
public record PerformanceMetricReceivedEvent(
        String metricId,
        String metricType,
        double value,
        String page,
        String sessionId,
        String traceId,
        LocalDateTime receivedAt,
        UUID domainEventId,
        LocalDateTime occurredOn) implements DomainEvent {

    public static PerformanceMetricReceivedEvent create(
            String metricId, String metricType, double value,
            String page, String sessionId, String traceId) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new PerformanceMetricReceivedEvent(
                metricId, metricType, value, page, sessionId, traceId,
                LocalDateTime.now(), metadata.eventId(), metadata.occurredOn());
    }

    /**
     * 檢查是否為核心網頁指標 (Core Web Vitals)
     */
    public boolean isCoreWebVital() {
        return "lcp".equals(metricType) ||
                "fid".equals(metricType) ||
                "cls".equals(metricType);
    }

    /**
     * 檢查指標是否超過建議閾值
     */
    public boolean exceedsRecommendedThreshold() {
        return switch (metricType) {
            case "lcp" -> value > 2500; // LCP > 2.5s
            case "fid" -> value > 100;  // FID > 100ms
            case "cls" -> value > 0.1;  // CLS > 0.1
            default -> false;
        };
    }

    @Override
    public String getEventType() {
        return "PerformanceMetricReceived";
    }

    @Override
    public String getAggregateId() {
        return sessionId;
    }
}
```

## 實體 (Entity) 設計

### @Entity 註解使用

`@Entity` 註解用於標識領域實體，區別於聚合根：

```java
@Entity(name = "SellerRating", description = "賣家評級實體")
public class SellerRating {
    
    private final SellerRatingId id;
    private final CustomerId customerId;
    private final int rating;
    private final String comment;
    private final LocalDateTime ratedAt;
    private RatingStatus status;
    
    public SellerRating(SellerRatingId id, CustomerId customerId, int rating, String comment) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.rating = validateRating(rating);
        this.comment = comment;
        this.ratedAt = LocalDateTime.now();
        this.status = RatingStatus.ACTIVE;
    }
    
    // 業務邏輯方法
    public boolean isPositive() {
        return rating >= 4; // 4分以上視為正面評價
    }
    
    public boolean isRecent() {
        return ChronoUnit.DAYS.between(ratedAt, LocalDateTime.now()) <= 30;
    }
    
    public void hide() {
        this.status = RatingStatus.HIDDEN;
    }
    
    public boolean isVisible() {
        return status == RatingStatus.ACTIVE;
    }
    
    private int validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("評級必須在1-5之間");
        }
        return rating;
    }
    
    // equals 和 hashCode 基於 ID
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SellerRating that = (SellerRating) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### Entity 設計原則

#### 1. 業務導向設計

Entity 應該專注於領域邏輯而非技術抽象：

```java
@Entity(name = "SellerRating", description = "賣家評級實體")
public class SellerRating {
    // 業務邏輯方法
    public boolean isPositive() {
        return rating >= 4; // 4分以上視為正面評價
    }
    
    public boolean isRecent() {
        return ChronoUnit.DAYS.between(ratedAt, LocalDateTime.now()) <= 30;
    }
    
    public void hide() {
        this.status = RatingStatus.HIDDEN;
    }
}
```

#### 2. 強型別 ID

每個 Entity 都應該有強型別的 ID Value Object：

```java
@ValueObject(name = "SellerRatingId", description = "賣家評級ID")
public record SellerRatingId(UUID value) {
    public SellerRatingId {
        Objects.requireNonNull(value, "SellerRating ID cannot be null");
    }
    
    public static SellerRatingId generate() {
        return new SellerRatingId(UUID.randomUUID());
    }
    
    public static SellerRatingId of(UUID uuid) {
        return new SellerRatingId(uuid);
    }
}
```

#### 3. 狀態管理

使用 Enum Value Object 管理 Entity 狀態：

```java
@ValueObject(name = "RatingStatus", description = "評級狀態")
public enum RatingStatus {
    ACTIVE("活躍"),
    HIDDEN("隱藏"),
    DELETED("已刪除");
    
    private final String description;
    
    RatingStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

### Entity 實作模式

#### 1. 基本結構模板

```java
@Entity(name = "EntityName", description = "實體描述")
public class EntityName {
    private final EntityNameId id;
    private String businessField;
    private EntityStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 建構子
    public EntityName(EntityNameId id, String businessField) {
        this.id = Objects.requireNonNull(id);
        this.businessField = Objects.requireNonNull(businessField);
        this.status = EntityStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 業務邏輯方法
    public void updateBusinessField(String newValue) {
        this.businessField = newValue;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isValid() {
        return status == EntityStatus.ACTIVE && businessField != null;
    }
    
    // Getters
    public EntityNameId getId() { return id; }
    public String getBusinessField() { return businessField; }
    public EntityStatus getStatus() { return status; }
    
    // equals 和 hashCode 基於 ID
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EntityName that = (EntityName) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

#### 2. 生命週期管理

```java
public class ContactInfo {
    private boolean emailVerified;
    private boolean phoneVerified;
    private LocalDateTime lastUpdated;
    
    public void verifyEmail() {
        this.emailVerified = true;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void verifyPhone() {
        this.phoneVerified = true;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public boolean isFullyVerified() {
        return emailVerified && phoneVerified;
    }
    
    public void updateContactInfo(String email, String phone) {
        this.email = email;
        this.phone = phone;
        this.lastUpdated = LocalDateTime.now();
        // 重新驗證
        this.emailVerified = false;
        this.phoneVerified = false;
    }
}
```
```

### 狀態管理實體

```java
@Entity(name = "StockReservation", description = "庫存預留實體")
public class StockReservation {
    
    private final ReservationId id;
    private final ProductId productId;
    private final int quantity;
    private ReservationStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    
    public StockReservation(ReservationId id, ProductId productId, int quantity, Duration duration) {
        this.id = Objects.requireNonNull(id);
        this.productId = Objects.requireNonNull(productId);
        this.quantity = validateQuantity(quantity);
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plus(duration);
    }
    
    public void confirm() {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("只有活躍的預留可以確認");
        }
        if (isExpired()) {
            throw new IllegalStateException("預留已過期，無法確認");
        }
        this.status = ReservationStatus.CONFIRMED;
    }
    
    public void release() {
        if (status == ReservationStatus.RELEASED) {
            throw new IllegalStateException("預留已經被釋放");
        }
        this.status = ReservationStatus.RELEASED;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public void extend(Duration duration) {
        if (status != ReservationStatus.ACTIVE) {
            throw new IllegalStateException("只有活躍的預留可以延期");
        }
        this.expiresAt = this.expiresAt.plus(duration);
    }
    
    private int validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("預留數量必須大於0");
        }
        return quantity;
    }
}
```

## 事件處理機制

### 抽象事件處理器

```java
public abstract class AbstractDomainEventHandler<T extends DomainEvent> 
    implements DomainEventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDomainEventHandler.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Order(100)
    public void onDomainEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();

        if (getSupportedEventType().isInstance(event)) {
            @SuppressWarnings("unchecked")
            T typedEvent = (T) event;

            if (shouldHandle(typedEvent)) {
                try {
                    logEventProcessingStart(typedEvent);
                    handle(typedEvent);
                    logEventProcessingSuccess(typedEvent);
                } catch (Exception e) {
                    logEventProcessingError(typedEvent, e);
                    throw new DomainEventProcessingException(
                            "Failed to process event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    protected abstract void handle(T event);
    protected abstract Class<T> getSupportedEventType();
    
    protected boolean shouldHandle(T event) {
        return true; // 預設處理所有事件
    }
}
```

### 具體事件處理器實作

```java
@Component
public class CustomerCreatedEventHandler extends AbstractDomainEventHandler<CustomerCreatedEvent> {
    
    private final EmailService emailService;
    private final CustomerStatsService customerStatsService;
    
    public CustomerCreatedEventHandler(EmailService emailService, 
                                     CustomerStatsService customerStatsService) {
        this.emailService = emailService;
        this.customerStatsService = customerStatsService;
    }
    
    @Override
    protected void handle(CustomerCreatedEvent event) {
        // 發送歡迎郵件
        emailService.sendWelcomeEmail(event.email(), event.customerName());
        
        // 更新客戶統計
        customerStatsService.createStatsRecord(event.customerId());
        
        // 記錄業務指標
        recordCustomerCreationMetrics(event);
    }
    
    @Override
    protected Class<CustomerCreatedEvent> getSupportedEventType() {
        return CustomerCreatedEvent.class;
    }
    
    private void recordCustomerCreationMetrics(CustomerCreatedEvent event) {
        // 記錄客戶創建指標
        LOGGER.info("Customer created: {} with membership level: {}", 
                   event.customerId(), event.membershipLevel());
    }
}
```

## 應用服務整合

### 事件發布流程

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService domainEventService;
    
    public void createCustomer(CreateCustomerCommand command) {
        // 1. 創建聚合根（事件被收集）
        Customer customer = new Customer(
            CustomerId.generate(),
            command.name(),
            command.email(),
            command.phone()
        );
        
        // 2. 保存聚合根
        customerRepository.save(customer);
        
        // 3. 發布收集的事件
        domainEventService.publishEventsFromAggregate(customer);
    }
    
    public void updateCustomerProfile(UpdateProfileCommand command) {
        // 1. 載入聚合根
        Customer customer = customerRepository.findById(command.customerId())
            .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));
        
        // 2. 執行業務操作（事件被收集）
        customer.updateProfile(command.name(), command.email(), command.phone());
        
        // 3. 保存聚合根
        customerRepository.save(customer);
        
        // 4. 發布收集的事件
        domainEventService.publishEventsFromAggregate(customer);
    }
}
```

## 測試策略

### 聚合根測試

```java
@ExtendWith(MockitoExtension.class)
class CustomerTest {
    
    @Test
    void should_collect_customer_created_event_when_customer_is_created() {
        // Given
        CustomerId customerId = CustomerId.generate();
        CustomerName name = new CustomerName("John Doe");
        Email email = new Email("john@example.com");
        Phone phone = new Phone("0912345678");
        
        // When
        Customer customer = new Customer(customerId, name, email, phone);
        
        // Then
        assertThat(customer.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerCreatedEvent.class);
        
        CustomerCreatedEvent event = (CustomerCreatedEvent) events.get(0);
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.customerName()).isEqualTo(name);
        assertThat(event.email()).isEqualTo(email);
    }
    
    @Test
    void should_collect_profile_updated_event_when_profile_is_updated() {
        // Given
        Customer customer = createTestCustomer();
        CustomerName newName = new CustomerName("Jane Doe");
        Email newEmail = new Email("jane@example.com");
        Phone newPhone = new Phone("0987654321");
        
        // When
        customer.updateProfile(newName, newEmail, newPhone);
        
        // Then
        assertThat(customer.hasUncommittedEvents()).isTrue();
        List<DomainEvent> events = customer.getUncommittedEvents();
        assertThat(events).hasSize(2); // 創建事件 + 更新事件
        
        CustomerProfileUpdatedEvent updateEvent = events.stream()
            .filter(e -> e instanceof CustomerProfileUpdatedEvent)
            .map(e -> (CustomerProfileUpdatedEvent) e)
            .findFirst()
            .orElseThrow();
            
        assertThat(updateEvent.customerName()).isEqualTo(newName);
        assertThat(updateEvent.email()).isEqualTo(newEmail);
        assertThat(updateEvent.phone()).isEqualTo(newPhone);
    }
}
```

### 事件處理器測試

```java
@ExtendWith(MockitoExtension.class)
class CustomerCreatedEventHandlerTest {
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private CustomerStatsService customerStatsService;
    
    @InjectMocks
    private CustomerCreatedEventHandler handler;
    
    @Test
    void should_send_welcome_email_when_customer_created() {
        // Given
        CustomerCreatedEvent event = CustomerCreatedEvent.create(
            CustomerId.of("CUST-001"),
            new CustomerName("John Doe"),
            new Email("john@example.com"),
            MembershipLevel.STANDARD
        );
        
        // When
        handler.handle(event);
        
        // Then
        verify(emailService).sendWelcomeEmail(event.email(), event.customerName());
        verify(customerStatsService).createStatsRecord(event.customerId());
    }
}
```

## 架構測試

### ArchUnit 規則

```java
@ArchTest
static final ArchRule aggregateRootRules = classes()
    .that().areAnnotatedWith(AggregateRoot.class)
    .should().implement(AggregateRootInterface.class)
    .because("聚合根必須實作 AggregateRootInterface");

@ArchTest
static final ArchRule valueObjectRules = classes()
    .that().areAnnotatedWith(ValueObject.class)
    .should().beRecords()
    .because("值對象應該使用 Record 實作");

@ArchTest
static final ArchRule domainEventRules = classes()
    .that().implement(DomainEvent.class)
    .should().beRecords()
    .and().haveSimpleNameEndingWith("Event")
    .because("領域事件應該使用 Record 實作並以 Event 結尾");

@ArchTest
static final ArchRule eventHandlerRules = classes()
    .that().areAnnotatedWith(Component.class)
    .and().haveSimpleNameEndingWith("EventHandler")
    .should().beAssignableTo(DomainEventHandler.class)
    .because("事件處理器必須實作 DomainEventHandler 介面");
```

## 最佳實踐總結

### 聚合根設計
1. **使用混搭方案**: Annotation + Interface 提供最佳的開發體驗
2. **事件收集**: 在業務操作中收集事件，由應用服務發布
3. **邊界清晰**: 一個聚合根管理一個業務不變性邊界

### 值對象設計
1. **Record 優先**: 使用 Java Record 確保不可變性
2. **業務驗證**: 在建構子中進行業務規則驗證
3. **工廠方法**: 提供語意清晰的創建方法

### 領域事件設計
1. **Record 實作**: 確保事件的不可變性
2. **工廠方法**: 自動設定事件元數據
3. **業務邏輯**: 在事件中包含業務判斷方法

### 事件處理
1. **事務感知**: 使用 @TransactionalEventListener 確保一致性
2. **類型安全**: 抽象基類提供類型安全的事件處理
3. **錯誤處理**: 完整的錯誤處理和日誌記錄

## 聚合內 Entity 關係

### 1. 一對多關係

```java
@AggregateRoot(name = "Seller", description = "賣家聚合根")
public class Seller implements AggregateRootInterface {
    private final SellerId sellerId;
    private final List<SellerRating> ratings;
    
    public void addRating(CustomerId customerId, int rating, String comment) {
        SellerRating newRating = new SellerRating(
            SellerRatingId.generate(),
            customerId,
            rating,
            comment
        );
        this.ratings.add(newRating);
        
        // 收集領域事件
        collectEvent(SellerRatingAddedEvent.create(sellerId, newRating.getId(), rating));
    }
    
    public double calculateAverageRating() {
        return ratings.stream()
            .filter(SellerRating::isVisible)
            .mapToInt(SellerRating::getRating)
            .average()
            .orElse(0.0);
    }
}
```

### 2. 一對一關係

```java
@AggregateRoot(name = "Seller", description = "賣家聚合根")
public class Seller implements AggregateRootInterface {
    private SellerProfile profile;
    private ContactInfo contactInfo;
    private SellerVerification verification;
    
    public void updateContactInfo(String email, String phone) {
        this.contactInfo.updateContactInfo(email, phone);
        collectEvent(SellerContactInfoUpdatedEvent.create(sellerId, email, phone));
    }
    
    public boolean canAcceptOrders() {
        return isActive() && 
               contactInfo.isFullyVerified() && 
               verification.isVerified() &&
               profile.isProfileComplete();
    }
}
```

## 驗證和業務規則

### 1. 輸入驗證

```java
public class SellerRating {
    public SellerRating(SellerRatingId id, CustomerId customerId, int rating, String comment) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.rating = validateRating(rating);
        this.comment = comment;
        this.ratedAt = LocalDateTime.now();
        this.status = RatingStatus.ACTIVE;
    }
    
    private int validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("評級必須在1-5之間");
        }
        return rating;
    }
}
```

### 2. 業務規則檢查

```java
public class SellerVerification {
    private static final Set<String> REQUIRED_DOCUMENTS = Set.of(
        "business_license", "tax_certificate", "identity_document"
    );
    
    public void approve(String verifierUserId, LocalDateTime expiresAt) {
        if (!hasAllRequiredDocuments()) {
            throw new IllegalStateException("必須提交所有必要文件才能通過驗證");
        }
        
        this.status = VerificationStatus.APPROVED;
        this.verifierUserId = verifierUserId;
        this.approvedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }
    
    public boolean hasAllRequiredDocuments() {
        return submittedDocuments.containsAll(REQUIRED_DOCUMENTS);
    }
}
```

## 常見模式和最佳實踐

### 1. 工廠方法

```java
public class ReviewImage {
    public static ReviewImage createFromUpload(String originalUrl, String fileName, long fileSize) {
        ReviewImage image = new ReviewImage(
            ReviewImageId.generate(),
            originalUrl,
            fileName,
            fileSize
        );
        
        if (!image.isValidImage()) {
            throw new IllegalArgumentException("無效的圖片格式");
        }
        
        return image;
    }
}
```

### 2. 查詢方法

```java
public class Seller {
    public List<SellerRating> getRecentRatings(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return ratings.stream()
            .filter(rating -> rating.getRatedAt().isAfter(cutoff))
            .filter(SellerRating::isVisible)
            .sorted((r1, r2) -> r2.getRatedAt().compareTo(r1.getRatedAt()))
            .toList();
    }
    
    public Optional<SellerRating> findRatingByCustomer(CustomerId customerId) {
        return ratings.stream()
            .filter(rating -> rating.getCustomerId().equals(customerId))
            .filter(SellerRating::isVisible)
            .findFirst();
    }
}
```

### 3. 聚合操作

```java
public class ProductReview {
    public void addModerationRecord(String moderatorId, ModerationAction action, String reason) {
        ModerationRecord record = new ModerationRecord(
            ModerationRecordId.generate(),
            moderatorId,
            action,
            reason
        );
        
        this.moderations.add(record);
        
        // 根據審核結果更新評價狀態
        if (action == ModerationAction.APPROVE) {
            this.status = ReviewStatus.APPROVED;
        } else if (action == ModerationAction.REJECT) {
            this.status = ReviewStatus.REJECTED;
        }
        
        collectEvent(ReviewModerationCompletedEvent.create(this.id, action, reason));
    }
}
```

## 效能考量

### 1. 延遲載入

```java
public class Seller {
    // 避免一次載入所有評級
    public List<SellerRating> getTopRatings(int limit) {
        return ratings.stream()
            .filter(SellerRating::isVisible)
            .sorted((r1, r2) -> Integer.compare(r2.getRating(), r1.getRating()))
            .limit(limit)
            .toList();
    }
}
```

### 2. 快取友好設計

```java
public class SellerProfile {
    private String businessInfoSummary; // 快取計算結果
    
    public String getBusinessInfoSummary() {
        if (businessInfoSummary == null) {
            businessInfoSummary = calculateBusinessInfoSummary();
        }
        return businessInfoSummary;
    }
    
    public void updateBusinessInfo(String name, String address, String description) {
        this.businessName = name;
        this.businessAddress = address;
        this.description = description;
        this.businessInfoSummary = null; // 清除快取
        this.lastProfileUpdate = LocalDateTime.now();
    }
}
```

## 領域事件 Record 實作最佳實踐

### 1. 基本事件結構

所有領域事件都應該使用 Record 實作，確保不可變性：

```java
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

### 2. 事件命名約定

- 使用過去式動詞：`CustomerCreated`、`OrderSubmitted`、`PaymentCompleted`
- 包含聚合名稱：`Customer*Event`、`Order*Event`
- 具體描述發生的事情：`CustomerProfileUpdated` 而非 `CustomerChanged`

### 3. 事件內容指導原則

- 包含聚合 ID 用於事件路由
- 包含事件處理器需要的所有數據
- 避免包含不應共享的敏感信息
- 包含事件元數據（eventId、occurredOn、eventType）

### 4. 事件版本演進

使用 Schema Evolution 模式處理事件版本變化：

```java
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    // V2 欄位使用 Optional 保持向後相容
    Optional<LocalDate> birthDate,
    Optional<Address> address,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    // 主要工廠方法 - 最新版本
    public static CustomerCreatedEvent create(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel,
        LocalDate birthDate,
        Address address
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            Optional.ofNullable(birthDate),
            Optional.ofNullable(address),
            metadata.eventId(), metadata.occurredOn()
        );
    }
    
    // 向後相容的工廠方法
    public static CustomerCreatedEvent createLegacy(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel
    ) {
        DomainEvent.EventMetadata metadata = DomainEvent.createEventMetadata();
        return new CustomerCreatedEvent(
            customerId, customerName, email, membershipLevel,
            Optional.empty(), // 舊版本沒有生日
            Optional.empty(), // 舊版本沒有地址
            metadata.eventId(), metadata.occurredOn()
        );
    }
}
```

## BDD 測試覆蓋策略

### 1. 聚合根行為測試

所有聚合根的業務邏輯都應該由 BDD 測試覆蓋：

```gherkin
Feature: 客戶聚合根管理
  Scenario: 客戶資料更新
    Given 一個已存在的客戶
    When 客戶更新個人資料
    Then 客戶資料應該被更新
    And 應該發布客戶資料更新事件

  Scenario: 賣家評級狀態管理
    Given 一個活躍的賣家評級
    When 管理員隱藏該評級
    Then 評級狀態應該變為隱藏
    And 評級不應該在公開列表中顯示
```

### 2. 架構測試

使用 ArchUnit 確保 DDD 戰術模式設計合規：

```java
@Test
void aggregateRootsShouldBeProperlyAnnotatedAndImplementInterface() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(AggregateRoot.class)
        .should().implement(AggregateRootInterface.class)
        .because("聚合根必須實作 AggregateRootInterface");
    
    rule.check(classes);
}

@Test
void entitiesShouldBeProperlyAnnotatedAndLocated() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(Entity.class)
        .should().resideInAPackage("..domain.*.model.entity..")
        .because("Entity 必須位於 entity 套件中");
    
    rule.check(classes);
}

@Test
void valueObjectsShouldBeRecords() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(ValueObject.class)
        .should().beRecords()
        .because("值對象應該使用 Record 實作");
    
    rule.check(classes);
}

@Test
void domainServicesShouldBeProperlyAnnotated() {
    ArchRule rule = classes()
        .that().areAnnotatedWith(DomainService.class)
        .should().beAnnotatedWith(Component.class)
        .and().should().resideInAPackage("..domain.*.service..")
        .because("領域服務必須是 Spring 組件並位於正確套件中");
    
    rule.check(classes);
}
```

## 總結

良好的 DDD 戰術模式實作應該：

1. **註解驅動**: 使用 @AggregateRoot、@ValueObject、@Entity、@DomainService 提供清晰的語義
2. **業務導向**: 專注於領域邏輯而非技術實作
3. **類型安全**: 使用強型別 ID 和狀態 Enum
4. **事件驅動**: 通過領域事件實現聚合間的解耦
5. **不可變性**: 值對象和事件使用 Record 確保不可變性
6. **封裝良好**: 透過方法而非直接屬性存取來維護不變性
7. **測試友好**: 設計易於測試的介面和行為
8. **效能考量**: 避免不必要的資料載入和計算

這些模式和實踐確保了 DDD 戰術模式的正確實作，提供了可維護、可測試且符合領域驅動設計原則的程式碼結構。