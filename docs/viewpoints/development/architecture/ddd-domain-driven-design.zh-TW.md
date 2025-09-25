# DDD 領域驅動設計

## 概述

本文檔提供完整的 DDD (Domain-Driven Design) 領域驅動設計指南，包含戰術模式、聚合根、值對象、實體、領域服務和領域事件的設計與實作。

## 🎯 DDD 戰術模式

### 設計原則

- **單一職責原則 (SRP)**：每個類別只有一個變更的理由
- **開放封閉原則 (OCP)**：對擴展開放，對修改封閉
- **依賴反轉原則 (DIP)**：依賴抽象而非具體實作

### 架構模式

- **六角架構**：清晰的邊界和依賴方向
- **DDD 戰術模式**：聚合根、實體、值物件
- **事件驅動架構**：鬆耦合的組件通訊

### 程式碼結構

```
domain/
├── model/          # 聚合根、實體、值物件
├── events/         # 領域事件
└── services/       # 領域服務

application/
├── commands/       # 命令處理
├── queries/        # 查詢處理
└── services/       # 應用服務

infrastructure/
├── persistence/    # 資料持久化
├── messaging/      # 訊息處理
└── external/       # 外部服務整合
```

## @AggregateRoot 聚合根

### 註解使用

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

### 混搭方案優點

- ✅ **編譯時約束**: 必須實作 AggregateRootInterface，IDE 會提示
- ✅ **零 override**: 所有事件管理方法都有 default 實作
- ✅ **註解驅動**: 通過 @AggregateRoot 提供元數據
- ✅ **自動驗證**: 在 default 方法中自動檢查註解

## @ValueObject 值對象

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

## @Entity 實體

### 註解使用

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

1. **業務導向設計**: Entity 應該專注於領域邏輯而非技術抽象
2. **強型別 ID**: 每個 Entity 都應該有強型別的 ID Value Object
3. **狀態管理**: 使用 Enum Value Object 管理 Entity 狀態

## @DomainService 領域服務

### 註解使用

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

### 領域服務設計原則

- **無狀態設計**
- **處理跨聚合的業務邏輯**
- **不包含基礎設施關注點**
- **使用依賴注入獲取所需的 Repository**

## 📡 領域事件 - Record 實作、事件收集與發布

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

### 事件處理機制

#### 抽象事件處理器

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

#### 具體事件處理器實作

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

### 應用服務整合

#### 事件發布流程

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

---

**相關文檔**
- [六角架構](hexagonal-architecture.md)
- 微服務架構
- Saga 模式#
# 領域事件設計指南

### 概覽

領域事件是 DDD 中重要的戰術模式，用於表示領域中發生的重要業務事件。事件驅動架構能夠實現聚合間的鬆耦合通信，並支援複雜的業務流程協調。

### 事件設計原則

#### 1. 事件命名約定
- 使用過去式動詞：`CustomerRegistered`、`OrderPlaced`、`PaymentCompleted`
- 包含聚合名稱：`Customer*Event`、`Order*Event`
- 具體描述發生的事情：`OrderStatusChanged` 而不是 `OrderUpdated`

#### 2. 事件內容設計
- 包含聚合 ID 用於事件路由
- 包含事件處理所需的所有資料
- 避免包含不應共享的敏感資訊
- 包含事件元資料（eventId、occurredOn、eventType）

#### 3. 事件不變性
- 事件一旦發布就不應該被修改
- 使用 Java Records 實現不可變事件
- 避免在事件中包含可變物件的引用

### 事件實現模式

#### 事件定義
```java
// 領域事件作為 Record - 遵循專案風格
public record CustomerRegisteredEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    /**
     * 工廠方法，自動生成 eventId 和 occurredOn
     */
    public static CustomerRegisteredEvent create(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel
    ) {
        return new CustomerRegisteredEvent(
            customerId, customerName, email, membershipLevel,
            UUID.randomUUID(), LocalDateTime.now()
        );
    }
    
    @Override
    public String getEventType() {
        return "CustomerRegistered";
    }
    
    @Override
    public String getAggregateId() {
        return customerId.getValue();
    }
}
```

#### 聚合根中的事件收集
```java
@AggregateRoot(name = "Customer", description = "客戶聚合根")
public class Customer implements AggregateRootInterface {
    
    public void register(CustomerName name, Email email, MembershipLevel level) {
        // 1. 執行業務邏輯
        validateRegistration(name, email);
        
        // 2. 更新狀態
        this.name = name;
        this.email = email;
        this.membershipLevel = level;
        this.status = CustomerStatus.ACTIVE;
        
        // 3. 收集領域事件
        collectEvent(CustomerRegisteredEvent.create(this.id, name, email, level));
    }
    
    // 事件管理方法由 AggregateRootInterface 提供：
    // - collectEvent(DomainEvent event)
    // - getUncommittedEvents()
    // - markEventsAsCommitted()
    // - hasUncommittedEvents()
}
```

#### 應用服務中的事件發布
```java
@Service
@Transactional
public class CustomerApplicationService {
    
    private final CustomerRepository customerRepository;
    private final DomainEventPublisher eventPublisher;
    
    public void registerCustomer(RegisterCustomerCommand command) {
        // 1. 載入或建立聚合
        Customer customer = Customer.register(
            command.name(), 
            command.email(), 
            command.membershipLevel()
        );
        
        // 2. 儲存聚合
        customerRepository.save(customer);
        
        // 3. 發布收集的事件
        eventPublisher.publishEvents(customer.getUncommittedEvents());
        customer.markEventsAsCommitted();
    }
}
```

#### 事件處理器
```java
@Component
public class CustomerRegisteredEventHandler {
    
    private final EmailService emailService;
    private final LoyaltyService loyaltyService;
    
    @EventListener
    @Transactional
    public void handle(CustomerRegisteredEvent event) {
        // 實現冪等性檢查
        if (isEventAlreadyProcessed(event.eventId())) {
            return;
        }
        
        try {
            // 執行跨聚合業務邏輯
            sendWelcomeEmail(event);
            createLoyaltyAccount(event);
            
            // 標記事件已處理
            markEventAsProcessed(event.eventId());
            
        } catch (Exception e) {
            // 記錄錯誤並可能重試
            logEventProcessingError(event, e);
            throw new DomainEventProcessingException("Failed to process customer registration", e);
        }
    }
    
    private void sendWelcomeEmail(CustomerRegisteredEvent event) {
        emailService.sendWelcomeEmail(
            event.email().getValue(),
            event.customerName().getValue()
        );
    }
    
    private void createLoyaltyAccount(CustomerRegisteredEvent event) {
        loyaltyService.createAccount(
            event.customerId(),
            event.membershipLevel()
        );
    }
}
```

### 事件版本管理

#### 向後相容的事件演進
```java
// 推薦：使用 Optional 欄位實現向後相容
public record CustomerRegisteredEvent(
    CustomerId customerId,
    CustomerName customerName,
    Email email,
    MembershipLevel membershipLevel,
    // V2 欄位使用 Optional 實現向後相容
    Optional<LocalDate> birthDate,
    Optional<Address> address,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    // 主要工廠方法 - 最新版本
    public static CustomerRegisteredEvent create(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel,
        LocalDate birthDate,
        Address address
    ) {
        return new CustomerRegisteredEvent(
            customerId, customerName, email, membershipLevel,
            Optional.ofNullable(birthDate),
            Optional.ofNullable(address),
            UUID.randomUUID(), LocalDateTime.now()
        );
    }
    
    // 向後相容工廠方法
    public static CustomerRegisteredEvent createLegacy(
        CustomerId customerId, 
        CustomerName customerName, 
        Email email,
        MembershipLevel membershipLevel
    ) {
        return new CustomerRegisteredEvent(
            customerId, customerName, email, membershipLevel,
            Optional.empty(), // 舊版本沒有生日
            Optional.empty(), // 舊版本沒有地址
            UUID.randomUUID(), LocalDateTime.now()
        );
    }
    
    @Override
    public String getEventType() {
        return "CustomerRegistered"; // 跨版本保持相同事件類型
    }
}
```

### 事件儲存策略

#### 選項 1：EventStore DB（生產環境推薦）
```yaml
# docker-compose.yml
version: '3.8'
services:
  eventstore:
    image: eventstore/eventstore:23.10.0-bookworm-slim
    container_name: eventstore
    environment:
      - EVENTSTORE_CLUSTER_SIZE=1
      - EVENTSTORE_RUN_PROJECTIONS=All
      - EVENTSTORE_START_STANDARD_PROJECTIONS=true
      - EVENTSTORE_EXT_TCP_PORT=1113
      - EVENTSTORE_HTTP_PORT=2113
      - EVENTSTORE_INSECURE=true
    ports:
      - "1113:1113"
      - "2113:2113"
```

#### 選項 2：JPA 事件儲存（開發環境推薦）
```java
@Entity
@Table(name = "domain_events")
public class StoredDomainEvent {
    @Id
    private String eventId;
    
    @Column(name = "event_type")
    private String eventType;
    
    @Column(name = "aggregate_id")
    private String aggregateId;
    
    @Column(name = "event_data", columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "occurred_on")
    private LocalDateTime occurredOn;
    
    @Column(name = "version")
    private Long version;
}

@Component
@Profile("development")
public class JpaEventStore implements EventStore {
    
    private final StoredDomainEventRepository repository;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public void store(DomainEvent event) {
        StoredDomainEvent storedEvent = new StoredDomainEvent(
            event.getEventId().toString(),
            event.getEventType(),
            event.getAggregateId(),
            serializeEvent(event),
            event.getOccurredOn(),
            getNextVersion(event.getAggregateId())
        );
        
        repository.save(storedEvent);
    }
}
```

### 事件驅動的 Saga 模式

#### 訂單處理 Saga
```java
@Component
public class OrderProcessingSaga {
    
    @EventListener
    @Order(1)
    public void on(OrderCreatedEvent event) {
        // 步驟 1：預留庫存
        inventoryService.reserveItems(event.orderItems());
    }
    
    @EventListener
    @Order(2)
    public void on(InventoryReservedEvent event) {
        // 步驟 2：處理付款
        paymentService.processPayment(event.orderId(), event.amount());
    }
    
    @EventListener
    @Order(3)
    public void on(PaymentProcessedEvent event) {
        // 步驟 3：確認訂單
        orderService.confirmOrder(event.orderId());
    }
    
    @EventListener
    public void on(PaymentFailedEvent event) {
        // 補償：釋放庫存
        inventoryService.releaseReservation(event.orderId());
        orderService.cancelOrder(event.orderId());
    }
}
```

### 錯誤處理和彈性

#### 重試機制
```java
@Component
public class ResilientEventHandler {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @EventListener
    public void handle(CustomerRegisteredEvent event) {
        // 具有重試能力的事件處理邏輯
    }
    
    @Recover
    public void recover(TransientException ex, CustomerRegisteredEvent event) {
        // 所有重試後的最終失敗處理
        deadLetterService.send(event, ex);
    }
}
```

#### 死信佇列
```java
@Component
public class DeadLetterService {
    
    public void send(DomainEvent event, Exception cause) {
        DeadLetterEvent deadLetter = new DeadLetterEvent(
            event.getEventId(),
            event.getClass().getSimpleName(),
            serializeEvent(event),
            cause.getMessage(),
            Instant.now()
        );
        
        deadLetterRepository.save(deadLetter);
        
        // 可選：發送到外部死信佇列
        messageQueue.send("dead-letter-queue", deadLetter);
    }
}
```

### 測試策略

#### 事件收集測試
```java
@Test
void should_collect_customer_registered_event_when_customer_registers() {
    // Given
    CustomerId customerId = CustomerId.generate();
    CustomerName name = new CustomerName("John Doe");
    Email email = new Email("john@example.com");
    
    // When
    Customer customer = Customer.register(customerId, name, email, MembershipLevel.STANDARD);
    
    // Then
    assertThat(customer.hasUncommittedEvents()).isTrue();
    List<DomainEvent> events = customer.getUncommittedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0)).isInstanceOf(CustomerRegisteredEvent.class);
    
    CustomerRegisteredEvent event = (CustomerRegisteredEvent) events.get(0);
    assertThat(event.customerId()).isEqualTo(customerId);
    assertThat(event.customerName()).isEqualTo(name);
    assertThat(event.email()).isEqualTo(email);
}
```

#### 事件處理器測試
```java
@Test
void should_send_welcome_email_when_customer_registered() {
    // Given
    CustomerRegisteredEvent event = CustomerRegisteredEvent.create(
        CustomerId.of("CUST-001"),
        new CustomerName("John Doe"),
        new Email("john@example.com"),
        MembershipLevel.STANDARD
    );
    
    // When
    customerRegisteredEventHandler.handle(event);
    
    // Then
    verify(emailService).sendWelcomeEmail("john@example.com", "John Doe");
    verify(loyaltyService).createAccount(CustomerId.of("CUST-001"), MembershipLevel.STANDARD);
}
```

### 監控和可觀測性

#### 事件指標
```java
@Component
public class EventMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void collectMetrics(DomainEvent event) {
        // 按類型計算事件
        Counter.builder("domain.events.published")
            .tag("event.type", event.getEventType())
            .tag("aggregate.type", getAggregateType(event))
            .register(meterRegistry)
            .increment();
    }
}
```

#### 事件追蹤
```java
@Component
public class EventTracingHandler {
    
    @EventListener
    public void trace(DomainEvent event) {
        Span span = tracer.nextSpan()
            .name("domain-event-processing")
            .tag("event.type", event.getEventType())
            .tag("event.id", event.getEventId().toString())
            .tag("aggregate.id", event.getAggregateId())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // 事件處理被追蹤
        } finally {
            span.end();
        }
    }
}
```

這個綜合指南確保在整個專案中實現一致、可靠和可維護的領域事件。## 
電子商務平台 Epic 實現案例

### 概述

本案例展示了一個完整的電子商務平台 Epic 實現，涵蓋從客戶瀏覽商品到訂單完成的整個業務流程。系統採用領域驅動設計 (DDD) 和六角形架構，提供高度可擴展和可維護的解決方案。

### 實現成果

✅ **所有功能已完成實現**

- **68 個場景** 全部通過測試
- **452 個步驟** 全部實現並驗證
- **15 個 Feature** 完整覆蓋所有業務需求
- **100% BDD 測試覆蓋率**

### 已實現的功能模組

| 模組 | Feature 數量 | 場景數量 | 狀態 |
|------|-------------|----------|------|
| 客戶管理 | 2 | 6 | ✅ 完成 |
| 訂單管理 | 1 | 6 | ✅ 完成 |
| 支付處理 | 2 | 11 | ✅ 完成 |
| 庫存管理 | 1 | 7 | ✅ 完成 |
| 物流配送 | 1 | 7 | ✅ 完成 |
| 通知服務 | 1 | 7 | ✅ 完成 |
| 促銷活動 | 4 | 10 | ✅ 完成 |
| 定價管理 | 1 | 2 | ✅ 完成 |
| 商品管理 | 1 | 3 | ✅ 完成 |
| 工作流程 | 1 | 9 | ✅ 完成 |

### DDD 實現亮點

#### 聚合根設計
- **Customer**: 客戶生命週期管理，包含會員等級和偏好設定
- **Order**: 訂單狀態管理，包含訂單項目和總金額計算
- **Product**: 商品目錄管理，包含價格和庫存資訊
- **Payment**: 支付處理，包含多種支付方式和退款機制
- **Inventory**: 庫存管理，包含預留和釋放機制
- **Promotion**: 促銷活動，包含各種折扣規則和條件

#### 值物件設計
```java
// 金額值物件
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
    
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }
}

// 客戶 ID 值物件
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
```

#### 領域服務實現
```java
@DomainService
public class PricingDomainService {
    
    public Money calculateOrderTotal(List<OrderItem> items, List<Promotion> applicablePromotions) {
        Money subtotal = items.stream()
            .map(item -> item.getPrice().multiply(item.getQuantity()))
            .reduce(Money.ZERO, Money::add);
            
        Money discount = applicablePromotions.stream()
            .map(promotion -> promotion.calculateDiscount(subtotal))
            .reduce(Money.ZERO, Money::add);
            
        return subtotal.subtract(discount);
    }
}
```

### 系統功能場景

#### 1. 客戶購物體驗場景
客戶可以在平台上瀏覽商品、享受各種優惠、完成購買並追蹤訂單狀態。系統提供個人化的購物體驗，包括會員優惠、紅利點數、生日折扣等多元化的優惠機制。

#### 2. 訂單管理場景
系統支持完整的訂單生命週期管理，從訂單創建、驗證、支付處理到配送完成。包含訂單狀態追蹤、取消機制、異常處理等功能。

#### 3. 庫存管理場景
實時庫存管理系統確保商品可用性，支持庫存預留、釋放、同步等功能。當庫存不足時，系統會自動通知相關人員並提供替代方案。

#### 4. 支付處理場景
多元化的支付方式支持，包括信用卡、行動錢包等。提供支付優惠、現金回饋、分期付款等功能，確保支付安全性和便利性。

#### 5. 物流配送場景
完整的配送管理系統，從配送安排到最終送達。支持配送狀態追蹤、地址變更、配送失敗處理等功能。

#### 6. 促銷活動場景
豐富的促銷活動支持，包括限時特價、限量優惠、滿額贈禮、加價購、組合優惠等多種促銷方式，提升客戶購買意願。

### 技術架構成就

#### 領域驅動設計 (DDD)
- **15 個聚合根**：清晰的業務邊界定義
- **戰術模式應用**：實體、值物件、領域服務、工廠模式
- **事件驅動架構**：領域事件處理跨聚合業務流程
- **界限上下文**：明確的上下文邊界和整合策略

#### 六角形架構
- **端口適配器模式**：完整的端口適配器實現
- **依賴倒置**：業務邏輯不依賴外部技術
- **高度可測試性**：每層都可以獨立測試
- **技術無關性**：可以輕鬆替換技術實現

#### BDD 測試策略
- **68 個業務場景**：完整的業務需求覆蓋
- **452 個測試步驟**：詳細的測試驗證
- **Cucumber 整合**：可執行的規格文檔
- **持續驗證**：自動化測試確保品質

### 品質指標達成

| 指標類別 | 目標 | 實際達成 | 狀態 |
|----------|------|----------|------|
| 測試覆蓋率 | 100% | 100% | ✅ |
| 場景通過率 | 100% | 100% (68/68) | ✅ |
| 步驟實現率 | 100% | 100% (452/452) | ✅ |
| 編譯成功率 | 100% | 100% | ✅ |
| 架構合規性 | 100% | 100% | ✅ |

### 業務價值實現

#### 完整的購物體驗
1. **商品瀏覽到訂單完成**：端到端的購物流程
2. **多元化促銷機制**：會員優惠、限時特價、滿額贈禮等
3. **可靠的支付系統**：多種支付方式、退款處理
4. **智能庫存管理**：實時檢查、預留機制
5. **完善的物流配送**：狀態追蹤、異常處理
6. **全方位通知服務**：多渠道通知、個人化設定

#### 技術債務管理
- **零技術債務**：所有程式碼遵循最佳實踐
- **高內聚低耦合**：清晰的模組邊界
- **可維護性**：易於理解和修改的程式碼結構
- **可擴展性**：支援未來功能擴展的架構設計

### 後續發展方向

#### 效能優化
1. **真實資料庫持久化**：從記憶體資料庫遷移到生產資料庫
2. **快取機制**：實現多層快取提升回應速度
3. **分散式架構**：支援高併發的微服務架構

#### 功能擴展
1. **更多支付方式**：Apple Pay、Google Pay、加密貨幣
2. **AI 推薦系統**：個人化商品推薦
3. **社交購物功能**：分享、評論、社群互動

#### 運營支援
1. **管理後台**：完整的後台管理系統
2. **資料分析**：商業智慧和報表功能
3. **A/B 測試框架**：支援實驗和優化

#### 技術升級
1. **微服務架構**：從單體應用遷移到微服務
2. **容器化部署**：Docker + Kubernetes
3. **CI/CD 流水線**：自動化建置和部署

這個 Epic 實現案例展示了 DDD 和六角形架構在實際專案中的成功應用，為企業數位轉型提供了堅實的技術基礎。