# ADR-001: DDD + 六角形架構基礎

## 狀態

**已接受** - 2024-01-15

## 背景

GenAI Demo 專案需要一個強健的軟體架構，能夠：

- 處理複雜的電商業務邏輯，具有清晰的領域邊界
- 支援多種介面（REST API、CMC 前端、消費者前端）
- 實現業務邏輯的獨立測試和部署
- 促進團隊協作和程式碼可維護性
- 支援未來的微服務演進

### 業務目標

- 實現全面的電商平台，包含客戶管理、訂單處理、庫存和支付系統
- 支援複雜的業務規則，如會員等級、促銷活動和定價策略

### 技術挑戰

- **複雜性管理**: 電商領域包含多個相互關聯的子領域
- **可測試性**: 需要能夠獨立測試業務邏輯
- **可擴展性**: 架構必須支援未來的功能擴展
- **團隊協作**: 多個開發者需要能夠並行工作而不互相干擾

## 決策

我們決定採用 **領域驅動設計 (DDD) + 六角形架構** 作為專案的核心架構模式。

### 核心架構原則

#### 1. 六角形架構（端口與適配器）

```
外部世界 → 適配器 → 端口 → 應用核心 ← 端口 ← 適配器 ← 外部世界
```

- **應用核心**: 包含業務邏輯和領域模型
- **端口**: 定義應用核心與外部世界的介面
- **適配器**: 實現端口，處理外部系統的具體技術細節

#### 2. DDD 戰術模式

- **聚合根 (Aggregate Root)**: 管理一致性邊界
- **值對象 (Value Object)**: 不可變的領域概念
- **領域事件 (Domain Event)**: 表示領域中發生的重要事件
- **規格模式 (Specification)**: 封裝複雜的業務規則
- **政策模式 (Policy)**: 處理業務決策邏輯

#### 3. 分層架構

```
interfaces/     → 介面層（REST 控制器、Web UI）
application/    → 應用層（用例協調、事件發布）
domain/         → 領域層（業務邏輯、領域模型）
infrastructure/ → 基礎設施層（持久化、外部服務）
```

### 實現策略

#### 1. 包結構設計

```
solid.humank.genaidemo/
├── interfaces/
│   └── web/           # REST 控制器
├── application/
│   ├── customer/      # 客戶用例
│   ├── order/         # 訂單用例
│   └── inventory/     # 庫存用例
├── domain/
│   ├── customer/      # 客戶聚合
│   ├── order/         # 訂單聚合
│   └── inventory/     # 庫存聚合
└── infrastructure/
    ├── persistence/   # 資料持久化
    └── messaging/     # 訊息處理
```

#### 2. 依賴規則

- **領域層**: 不依賴任何其他層
- **應用層**: 只依賴領域層
- **基礎設施層**: 可以依賴所有層
- **介面層**: 依賴應用層和基礎設施層

#### 3. 聚合設計原則

- **小聚合**: 每個聚合專注於單一業務概念
- **一致性邊界**: 聚合內部保持強一致性
- **最終一致性**: 聚合間通過領域事件實現最終一致性

## 結果

### 正面影響

#### 1. **可測試性提升**

- 業務邏輯與技術細節分離，單元測試覆蓋率達到 85%+
- 可以獨立測試領域邏輯，不需要外部依賴

#### 2. **可維護性改善**

- 清晰的分層結構，程式碼職責明確
- 新功能開發時，影響範圍可控

#### 3. **團隊協作效率**

- 不同團隊可以並行開發不同的聚合
- 介面定義清晰，減少溝通成本

#### 4. **技術債務控制**

- 架構約束防止不當的依賴關係
- 定期的架構合規性檢查（ArchUnit）

### 量化指標

- **架構合規性**: 9.5/10（ArchUnit 測試結果）
- **程式碼重複率**: < 5%
- **循環依賴**: 0 個
- **測試覆蓋率**: 85%+

### 負面影響與緩解措施

#### 1. **學習曲線**

- **問題**: 團隊需要學習 DDD 概念
- **緩解**: 提供培訓文檔和程式碼範例

#### 2. **初期開發速度**

- **問題**: 架構設置需要額外時間
- **緩解**: 建立程式碼模板和生成工具

#### 3. **過度設計風險**

- **問題**: 可能為簡單功能創建過於複雜的結構
- **緩解**: 定期架構審查，保持 YAGNI 原則

## 實現細節

### 1. 聚合根實現

```java
@AggregateRoot(name = "Customer", description = "客戶聚合根")
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

### 2. 值對象實現

```java
@ValueObject
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("客戶 ID 不能為空");
        }
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString());
    }
}
```

### 3. 領域事件實現

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
        
        // 發布領域事件
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

### 2. 測試策略

- **單元測試**: 測試領域邏輯，不涉及外部依賴
- **集成測試**: 測試適配器與外部系統的整合
- **架構測試**: 驗證架構規則的遵循

### 3. 文檔維護

- **領域模型圖**: 使用 PlantUML 維護最新的領域模型
- **架構決策記錄**: 記錄重要的架構變更
- **程式碼範例**: 提供標準的實現模式

## 相關決策

- [ADR-002: 限界上下文設計策略](./ADR-002-bounded-context-design.md)
- [ADR-003: 領域事件和 CQRS 實現](./ADR-003-domain-events-cqrs.md)
- [ADR-004: Spring Boot 設定檔配置策略](./ADR-004-spring-boot-profiles.md)

## 參考資料

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.amazon.com/Domain-Driven-Design-Tackling-Complexity-Software/dp/0321125215)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot DDD 最佳實踐](https://spring.io/guides/gs/spring-boot/)

---

**最後更新**: 2024-01-15  
**審核者**: 架構團隊  
**下次審查**: 2024-07-15
