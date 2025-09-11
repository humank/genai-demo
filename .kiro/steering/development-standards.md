# 開發標準與規範

## 項目概述

GenAI Demo - 基於 DDD + 六角架構的電商平台演示項目

## 核心技術棧

- **Backend**: Spring Boot 3.4.5 + Java 21 + Gradle 8.x
- **Frontend**: Next.js 14 (CMC) + Angular 18 (Consumer)
- **Database**: H2 (dev/test) + PostgreSQL (prod)
- **Testing**: JUnit 5 + Cucumber 7 + ArchUnit + Mockito

## 架構原則

### DDD + 六角架構分層

```
interfaces/ → application/ → domain/ ← infrastructure/
```

### 包結構規範

- `domain/{context}/model/` - 聚合根、實體、值對象
- `domain/{context}/events/` - 領域事件 (Records)
- `application/{context}/` - 用例實現
- `infrastructure/{context}/persistence/` - 持久化適配器

### 領域事件設計

- 使用不可變 Records 實現
- 聚合根收集事件，應用服務發布事件
- 事件處理器在基礎設施層

## 測試標準

### 測試分類 (按優先級)

1. **UnitTest** - 純業務邏輯，Mock，~5MB，~50ms
2. **IntegrationTest** - 數據庫集成，~50MB，~500ms
3. **SpringBootTest** - 端到端測試，~500MB，~3s (謹慎使用)

### 性能基準

- 單元測試：< 100ms, < 10MB
- 測試覆蓋率：> 80%
- 失敗率：< 1%

### BDD 開發流程

1. 編寫 Gherkin 場景 (`src/test/resources/features/`)
2. 實現步驟定義 (Red)
3. TDD 實現領域邏輯 (Green)
4. 重構優化 (Refactor)

### ArchUnit 規則

- 分層依賴檢查
- DDD 戰術模式驗證
- 包命名規範檢查

## 代碼規範

### 命名約定

```java
// 聚合根
@AggregateRoot
public class Customer implements AggregateRootInterface { }

// 值對象
@ValueObject
public record CustomerId(String value) { }

// 領域事件
public record CustomerCreatedEvent(...) implements DomainEvent { }

// 測試類
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest { }
```

### Mock 使用原則

- 只 mock 測試中實際使用的交互
- 避免全局 stubbing
- 處理 null 情況

## 開發工作流

### 新功能開發

1. BDD 場景設計
2. 領域建模 (DDD)
3. TDD 實現
4. 集成測試
5. ArchUnit 驗證

### 常用命令

```bash
# 測試
./gradlew test                    # 全部測試
./gradlew unitTest               # 單元測試
./gradlew cucumber               # BDD 測試
./gradlew testArchitecture       # 架構測試

# 構建
./gradlew build                  # 構建應用
./gradlew bootRun               # 運行應用
```

## 質量標準

- 代碼覆蓋率 > 80%
- 架構合規性 100%
- 性能基準達標
- BDD 場景覆蓋

詳細指南：

- [測試優化指南](../../docs/testing/test-optimization-guidelines.md)
- [架構設計文檔](../../docs/architecture/)
- [開發工具配置](../../docs/development/)
