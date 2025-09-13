# 開發標準與規範

## 項目概述

GenAI Demo - 基於 DDD + 六角架構的全棧電商平台演示項目，整合企業級可觀測性和 AI 輔助開發

### 核心特性

- **智能購物車**: 多重促銷計算引擎
- **個性化推薦**: 基於購買歷史的產品推薦
- **會員積分系統**: 完整的積分累積與兌換機制
- **即時配送追蹤**: 實時配送狀態更新
- **訂單生命週期管理**: 完整的訂單處理流程
- **成本優化**: 即時成本追蹤和資源右調建議

### 技術架構亮點

- **領域驅動設計**: 完整的 DDD 戰術模式實現
- **六角架構**: 清晰的關注點分離
- **事件驅動架構**: 鬆耦合的領域事件機制
- **CQRS 模式**: 命令查詢職責分離
- **全面測試覆蓋**: BDD + TDD + 架構測試
- **企業級可觀測性**: 分散式追蹤 + 結構化日誌 + 業務指標
- **AI 輔助開發**: MCP 整合提供智能開發指導

## 核心技術棧

### 後端技術

- **Spring Boot 3.4.5** + **Java 21** + **Gradle 8.x**
- **Spring Data JPA** + **Hibernate** + **Flyway**
- **H2** (開發/測試) + **PostgreSQL** (生產)
- **SpringDoc OpenAPI 3** + **Swagger UI**
- **Spring Boot Actuator** (監控)
- **AWS X-Ray** (分散式追蹤) + **Micrometer** (指標)
- **Logback** (結構化日誌) + **CloudWatch** (日誌聚合)

### 前端技術

- **CMC 管理端**: Next.js 14 + React 18 + TypeScript + Tailwind CSS
- **消費者端**: Angular 18 + TypeScript + Tailwind CSS
- **UI 組件**: shadcn/ui + Radix UI
- **狀態管理**: React Query + Zustand

### 測試框架

- **JUnit 5** + **Mockito** + **AssertJ**
- **Cucumber 7** (BDD) + **Gherkin**
- **ArchUnit** (架構測試)
- **Allure 2** (測試報告)

### 開發工具

- **Lombok** (減少樣板代碼)
- **PlantUML** (UML 圖表生成)
- **Docker** + **Docker Compose**
- **AWS CDK** (基礎設施即程式碼)
- **GitHub Actions** (CI/CD 管道)
- **MCP 伺服器** (AI 輔助開發)

### 項目結構

```
genai-demo/
├── app/                    # Spring Boot 主應用
├── cmc-frontend/           # 商務管理中心 (Next.js)
├── consumer-frontend/      # 消費者應用 (Angular)
├── infrastructure/         # AWS CDK 基礎設施程式碼
├── deployment/             # Kubernetes 部署配置
├── docker/                 # Docker 構建腳本
├── docs/                   # 項目文檔
├── scripts/                # 開發部署腳本
└── .github/                # GitHub Actions CI/CD
```

### 開發環境端口

- **後端 API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **H2 控制台**: <http://localhost:8080/h2-console>
- **健康檢查**: <http://localhost:8080/actuator/health>
- **應用指標**: <http://localhost:8080/actuator/metrics>
- **成本優化**: <http://localhost:8080/api/cost-optimization/recommendations>
- **CMC 前端**: <http://localhost:3002>
- **消費者前端**: <http://localhost:3001>

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

## 測試標準與優化

### 測試分層策略 (測試金字塔原則)

```
    /\     E2E Tests (5%)
   /  \    ~10 個，~3s 每個，~500MB 記憶體
  /____\   Integration Tests (15%)  
 /      \  ~50 個，~500ms 每個，~50MB 記憶體
/________\ Unit Tests (80%)
           ~500 個，~50ms 每個，~5MB 記憶體
```

### 測試分類標準

#### 1. Unit Tests (優先使用)

- **使用場景**: 純業務邏輯、工具類、配置類邏輯
- **特徵**: 不需要 Spring 上下文
- **註解**: `@ExtendWith(MockitoExtension.class)`
- **記憶體**: ~5MB
- **執行時間**: ~50ms

#### 2. Integration Tests (謹慎使用)

- **使用場景**: 數據庫集成、消息隊列、外部服務
- **特徵**: 需要部分 Spring 上下文
- **註解**: `@DataJpaTest`, `@WebMvcTest`, `@JsonTest`
- **記憶體**: ~50MB
- **執行時間**: ~500ms

#### 3. End-to-End Tests (最少使用)

- **使用場景**: 完整業務流程驗證
- **特徵**: 完整 Spring 上下文 + Web 環境
- **註解**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- **記憶體**: ~500MB
- **執行時間**: ~3s

### 測試任務配置

```gradle
// 快速單元測試 (11秒)
./gradlew unitTest

// 冒煙測試 (2秒)
./gradlew quickTest

// 集成測試
./gradlew integrationTest

// 端到端測試
./gradlew e2eTest
```

### 性能基準與優化成果

#### 目標指標

- **單元測試**: < 100ms, < 10MB, 成功率 > 99%
- **集成測試**: < 1s, < 100MB, 成功率 > 95%
- **端到端測試**: < 5s, < 1GB, 成功率 > 90%

#### 實際達成 (2025年1月)

- **總測試時間**: 從 13分52秒 → 15秒 (**98.2% 改善**)
- **單元測試**: 11秒 (極快)
- **快速檢查**: 2秒 (極快)
- **記憶體使用**: 從 6GB → 1-3GB (**50-83% 節省**)

### 測試標籤系統

```java
@UnitTest        // 快速單元測試
@SmokeTest       // 核心功能測試
@SlowTest        // 慢速測試標記
@IntegrationTest // 集成測試
```

### BDD 開發流程

1. 編寫 Gherkin 場景 (`src/test/resources/features/`)
2. 實現步驟定義 (Red)
3. TDD 實現領域邏輯 (Green)
4. 重構優化 (Refactor)

### ArchUnit 規則

- 分層依賴檢查
- DDD 戰術模式驗證
- 包命名規範檢查

### 測試重構原則

#### 禁止的反模式

```java
// ❌ 錯誤：配置類測試不需要完整 Spring 上下文
@SpringBootTest
class DatabaseConfigurationTest { ... }

// ✅ 正確：使用單元測試
@ExtendWith(MockitoExtension.class)
class DatabaseConfigurationUnitTest { ... }
```

#### 重構策略

1. **分析依賴**: 測試真正需要哪些組件？
2. **選擇合適註解**: 使用最小化的測試切片
3. **Mock 外部依賴**: 隔離被測試的組件
4. **驗證性能**: 確保重構後測試更快

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
# 測試 (優化後的分層執行)
./gradlew quickTest              # 快速檢查 (2秒)
./gradlew unitTest               # 單元測試 (11秒)
./gradlew integrationTest        # 集成測試
./gradlew e2eTest               # 端到端測試
./gradlew test                   # 完整測試套件
./gradlew cucumber               # BDD 測試
./gradlew testArchitecture       # 架構測試

# 構建
./gradlew build                  # 構建應用
./gradlew bootRun               # 運行應用
```

## 質量標準

### 測試質量指標

- 代碼覆蓋率 > 80%
- 測試執行時間 < 15秒 (單元測試)
- 測試失敗率 < 1%
- 架構合規性 100%

### 開發工作流程

```bash
# 日常開發循環
./gradlew quickTest              # 開發時快速回饋 (2秒)
./gradlew unitTest               # 提交前完整驗證 (11秒)
./gradlew integrationTest        # PR 檢查集成測試
./gradlew test                   # 發布前完整測試
```

### BDD 場景覆蓋

- 核心業務流程 100% 覆蓋
- 異常處理場景覆蓋
- 用戶體驗關鍵路徑覆蓋

詳細指南：

- [架構設計文檔](../../docs/architecture/)
- [開發工具配置](../../docs/development/)
- [測試覆蓋率報告](../build/reports/jacoco/test/html/index.html)
