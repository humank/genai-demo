# 架構模式與設計原則

## DDD 戰術模式

### 聚合根設計

- 使用 `@AggregateRoot` 註解
- 實現 `AggregateRootInterface`
- 負責收集領域事件
- 維護業務不變性

### 值對象實現

- 使用不可變 Records
- 添加 `@ValueObject` 註解
- 包含驗證邏輯
- 提供靜態工廠方法

### 領域事件模式

- Records 實現 `DomainEvent` 介面
- 聚合根收集，應用服務發布
- 事件處理器在基礎設施層
- 支持事件溯源和 CQRS

### 規格模式 (Specification)

- 封裝複雜業務規則
- 使用 `@Specification` 註解
- 支持組合和重用
- 分離查詢條件邏輯

### 策略模式 (Policy)

- 封裝業務決策邏輯
- 使用 `@Policy` 註解
- 支持運行時策略切換
- 處理可變業務規則

## 六角架構實現

### 端口與適配器

- **主端口**: 用例介面 (application layer)
- **次端口**: 倉儲介面 (domain layer)
- **主適配器**: REST 控制器 (interfaces layer)
- **次適配器**: JPA 倉儲 (infrastructure layer)

### 依賴反轉

```java
// Domain 定義介面
public interface CustomerRepository {
    void save(Customer customer);
}

// Infrastructure 實現介面
@Repository
public class JpaCustomerRepository implements CustomerRepository {
    // JPA 實現
}
```

### 分層依賴規則

- Interfaces → Application → Domain
- Infrastructure → Domain (實現介面)
- 禁止跨層直接依賴

## 事件驅動架構

### 事件發布模式

1. 聚合根收集事件 (`collectEvent()`)
2. 應用服務發布事件 (`publishEventsFromAggregate()`)
3. 事件處理器異步處理 (`@TransactionalEventListener`)

### 事件存儲選項

- **開發環境**: JPA Event Store
- **測試環境**: In-Memory Event Store  
- **生產環境**: EventStore DB

### CQRS 實現

- 命令端：聚合根 + 事件
- 查詢端：讀取模型 + 投影
- 事件同步：異步事件處理

詳細實現請參考：[領域事件指南](domain-events.md)
