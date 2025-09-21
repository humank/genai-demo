
# ADR-001: DDD + Hexagonal Architecture基礎

## 狀態

**已接受** - 2024-01-15

## 背景

GenAI Demo 專案需要一個強健的軟體架構，能夠：

- 處理複雜的電商業務邏輯，具有清晰的領域邊界
- 支援多種介面（REST API、CMC 前端、消費者前端）
- 實現業務邏輯的獨立測試和Deployment
- 促進團隊協作和程式碼Maintainability
- 支援未來的微服務演進

### 業務目標

- 實現全面的電商平台，包含Customer管理、訂單處理、庫存和支付系統
- 支援複雜的業務規則，如會員等級、促銷活動和定價Policy

### 技術挑戰

- **複雜性管理**: 電商領域包含多個相互關聯的子領域
- **Testability**: 需要能夠獨立測試業務邏輯
- **Scalability**: 架構必須支援未來的功能擴展
- **團隊協作**: 多個Developer需要能夠並行工作而不互相干擾

## 決策

我們決定採用 **Domain-Driven Design (DDD) + Hexagonal Architecture** 作為專案的核心Architectural Pattern。

### 核心Architectural Principle

#### 1. Hexagonal Architecture（Port與Adapter）

```
外部世界 → Adapter → Port → Application Core ← Port ← Adapter ← 外部世界
```

- **Application Core**: 包含業務邏輯和領域模型
- **Port**: 定義Application Core與外部世界的介面
- **Adapter**: 實現Port，處理External System的具體技術細節

#### 2. DDD 戰術模式

- **Aggregate Root (Aggregate Root)**: 管理一致性邊界
- **Value Object (Value Object)**: 不可變的領域概念
- **Domain Event (Domain Event)**: 表示領域中發生的重要事件
- **Specification Pattern (Specification)**: 封裝複雜的業務規則
- **Policy Pattern (Policy)**: 處理業務決策邏輯

#### 3. Layered Architecture

```
interfaces/     → Interface Layer（REST 控制器、Web UI）
application/    → Application Layer（用例協調、事件發布）
domain/         → Domain Layer（業務邏輯、領域模型）
../../infrastructure/ → Infrastructure Layer（持久化、外部服務）
```

### 實現Policy

#### Design

```
solid.humank.genaidemo/
├── interfaces/
│   └── web/           # REST 控制器
├── application/
│   ├── customer/      # Customer用例
│   ├── order/         # 訂單用例
│   └── inventory/     # 庫存用例
├── domain/
│   ├── customer/      # CustomerAggregate
│   ├── order/         # 訂單Aggregate
│   └── inventory/     # 庫存Aggregate
└── ../../infrastructure/
    ├── persistence/   # 資料持久化
    └── messaging/     # 訊息處理
```

#### 2. 依賴規則

- **Domain Layer**: 不依賴任何其他層
- **Application Layer**: 只依賴Domain Layer
- **Infrastructure Layer**: 可以依賴所有層
- **Interface Layer**: 依賴Application Layer和Infrastructure Layer

#### Design

- **小Aggregate**: 每個Aggregate專注於單一業務概念
- **一致性邊界**: Aggregate內部保持強一致性
- **最終一致性**: Aggregate間通過Domain Event實現最終一致性

## 結果

### 正面影響

#### Testing

- 業務邏輯與技術細節分離，Unit Test覆蓋率達到 85%+
- 可以獨立測試領域邏輯，不需要外部依賴

#### Maintenance

- 清晰的分層結構，程式碼職責明確
- 新功能開發時，影響範圍可控

#### 3. **團隊協作效率**

- 不同團隊可以並行開發不同的Aggregate
- 介面定義清晰，減少溝通成本

#### 4. **Technical Debt控制**

- 架構Constraint防止不當的依賴關係
- 定期的架構合規性檢查（ArchUnit）

### 量化Metrics

- **架構合規性**: 9.5/10（ArchUnit 測試結果）
- **程式碼重複率**: < 5%
- **循環依賴**: 0 個
- **Test Coverage**: 85%+

### 負面影響與緩解措施

#### 1. **學習曲線**

- **問題**: 團隊需要學習 DDD 概念
- **緩解**: 提供培訓文檔和程式碼範例

#### 2. **初期開發速度**

- **問題**: 架構設置需要額外時間
- **緩解**: 建立程式碼模板和生成工具

#### Design

- **問題**: 可能為簡單功能創建過於複雜的結構
- **緩解**: 定期架構審查，保持 YAGNI 原則

## 實現細節

### 1. Aggregate Root實現

```java
@AggregateRoot(name = "Customer", description = "CustomerAggregate Root")
public class Customer implements AggregateRootInterface {
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private MembershipLevel membershipLevel;
    
    // 業務方法
    public void upgradeToVip() {
        if (canUpgradeToVip()) {
            this.membershipLevel = MembershipLevel.VIP;
            collectEvent(new CustomerUpgradedToVipEvent(this.id));
        }
    }
}
```

### 2. Value Object實現

```java
@ValueObject
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID 不能為空");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString());
    }
}
```

### 3. Domain Event實現

```java
public record CustomerUpgradedToVipEvent(
    CustomerId customerId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {
    
    public static CustomerUpgradedToVipEvent create(CustomerId customerId) {
        return new CustomerUpgradedToVipEvent(
            customerId,
            UUID.randomUUID(),
            LocalDateTime.now()
        );
    }
}
```

### 4. 應用服務實現

```java
@Service
@Transactional
public class CustomerApplicationService {
    
    public void upgradeCustomerToVip(CustomerId customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
            
        customer.upgradeToVip();
        customerRepository.save(customer);
        
        // 發布Domain Event
        domainEventPublisher.publishEventsFromAggregate(customer);
    }
}
```

## 合規性與驗證

### 1. ArchUnit 規則

```java
@ArchTest
static final ArchRule domain_should_not_depend_on_infrastructure =
    noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("..infrastructure..");

@ArchTest
static final ArchRule aggregates_should_be_annotated =
    classes()
        .that().implement(AggregateRootInterface.class)
        .should().beAnnotatedWith(AggregateRoot.class);
```

### Testing

- **Unit Test**: 測試領域邏輯，不涉及外部依賴
- **集成測試**: 測試Adapter與External System的整合
- **Architecture Test**: 驗證架構規則的遵循

### Maintenance

- **領域模型圖**: 使用 PlantUML 維護最新的領域模型
- **Architecture Decision Record (ADR)**: 記錄重要的架構變更
- **程式碼範例**: 提供標準的實現模式

## 相關決策

- [ADR-002: 限界上下文設計Policy](./ADR-002-bounded-context-design.md)
- [ADR-003: Domain Event和 Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS)) 實現](./ADR-003-domain-events-cqrs.md)
- \1

## Reference

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.amazon.com/Domain-Driven-Design-Tackling-Complexity-Software/dp/0321125215)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot DDD Best Practice](https://spring.io/guides/gs/spring-boot/)

---

**最後更新**: 2024-01-15  
**審核者**: 架構團隊  
**下次審查**: 2024-07-15
