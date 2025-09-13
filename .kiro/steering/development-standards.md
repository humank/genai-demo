# 開發標準與規範

## 技術棧要求

### 後端技術

- Spring Boot 3.4.5 + Java 21 + Gradle 8.x
- Spring Data JPA + Hibernate + Flyway
- H2 (dev/test) + PostgreSQL (prod)
- SpringDoc OpenAPI 3 + Swagger UI
- Spring Boot Actuator + AWS X-Ray + Micrometer

### 前端技術

- CMC 管理端: Next.js 14 + React 18 + TypeScript
- 消費者端: Angular 18 + TypeScript
- UI 組件: shadcn/ui + Radix UI

### 測試框架

- JUnit 5 + Mockito + AssertJ
- Cucumber 7 (BDD) + Gherkin
- ArchUnit (架構測試)

## 架構約束

### 包結構規範

- `domain/{context}/model/` - 聚合根、實體、值對象
- `domain/{context}/events/` - 領域事件 (Records)
- `application/{context}/` - 用例實現
- `infrastructure/{context}/persistence/` - 持久化適配器

### 分層依賴規則

```
interfaces/ → application/ → domain/ ← infrastructure/
```

### 領域事件設計約束

- 使用不可變 Records 實現
- 聚合根收集事件，應用服務發布事件
- 事件處理器在基礎設施層

## 測試標準

### 測試分層要求 (測試金字塔)

- Unit Tests (80%): < 50ms, < 5MB
- Integration Tests (15%): < 500ms, < 50MB  
- E2E Tests (5%): < 3s, < 500MB

### 測試分類標準

#### Unit Tests (優先使用)

- 註解: `@ExtendWith(MockitoExtension.class)`
- 適用: 純業務邏輯、工具類、配置類
- 禁止: Spring 上下文

#### Integration Tests (謹慎使用)

- 註解: `@DataJpaTest`, `@WebMvcTest`, `@JsonTest`
- 適用: 數據庫集成、外部服務
- 要求: 部分 Spring 上下文

#### E2E Tests (最少使用)

- 註解: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- 適用: 完整業務流程驗證
- 要求: 完整 Spring 上下文

### 測試標籤系統

```java
@UnitTest        // 快速單元測試
@SmokeTest       // 核心功能測試
@SlowTest        // 慢速測試標記
@IntegrationTest // 集成測試
```

### 性能基準要求

- 單元測試: < 100ms, < 10MB, 成功率 > 99%
- 集成測試: < 1s, < 100MB, 成功率 > 95%
- 端到端測試: < 5s, < 1GB, 成功率 > 90%

## BDD 開發流程

### 強制步驟

1. 編寫 Gherkin 場景 (`src/test/resources/features/`)
2. 實現步驟定義 (Red)
3. TDD 實現領域邏輯 (Green)
4. 重構優化 (Refactor)

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

### Mock 使用規則

- 只 mock 測試中實際使用的交互
- 避免全局 stubbing
- 處理 null 情況

## ArchUnit 規則

### 強制架構規則

- 分層依賴檢查
- DDD 戰術模式驗證
- 包命名規範檢查

### 禁止的反模式

```java
// ❌ 錯誤：配置類測試不需要完整 Spring 上下文
@SpringBootTest
class DatabaseConfigurationTest { ... }

// ✅ 正確：使用單元測試
@ExtendWith(MockitoExtension.class)
class DatabaseConfigurationUnitTest { ... }
```

## 質量標準

### 必須達成指標

- 代碼覆蓋率 > 80%
- 測試執行時間 < 15秒 (單元測試)
- 測試失敗率 < 1%
- 架構合規性 100%

### BDD 場景覆蓋要求

- 核心業務流程 100% 覆蓋
- 異常處理場景覆蓋
- 用戶體驗關鍵路徑覆蓋

## 開發工作流

### 新功能開發順序

1. BDD 場景設計
2. 領域建模 (DDD)
3. TDD 實現
4. 集成測試
5. ArchUnit 驗證

### 日常開發命令

```bash
./gradlew quickTest              # 開發時快速回饋 (2秒)
./gradlew unitTest               # 提交前完整驗證 (11秒)
./gradlew integrationTest        # PR 檢查集成測試
./gradlew test                   # 發布前完整測試
```
