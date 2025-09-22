# Development Viewpoint 遷移指南

> **完整的開發文檔遷移指南和書籤更新說明**

## 📋 遷移概覽

為了提供更系統化和專業的開發文檔體驗，我們已將分散在多個目錄的開發相關文檔整合到統一的 **Development Viewpoint** 結構中。

### 遷移範圍

- **`docs/development/`** → **`docs/viewpoints/development/`**
- **`docs/design/`** → **`docs/viewpoints/development/architecture/`**
- **`docs/testing/`** → **`docs/viewpoints/development/testing/`**

## 🔗 完整遷移對照表

### 開發指南遷移

| 原始路徑 | 新路徑 | 狀態 |
|----------|--------|------|
| `docs/development/README.md` | `docs/viewpoints/development/README.md` | ✅ 已遷移 |
| `docs/development/getting-started.md` | `docs/viewpoints/development/getting-started/README.md` | ✅ 已遷移 |
| `docs/development/coding-standards.md` | `docs/viewpoints/development/coding-standards/README.md` | ✅ 已遷移 |
| `docs/development/testing-guide.md` | `docs/viewpoints/development/testing/README.md` | ✅ 已遷移 |
| `docs/development/documentation-guide.md` | `docs/viewpoints/development/coding-standards/documentation-standards.md` | ✅ 已遷移 |
| `docs/development/instructions.md` | `docs/viewpoints/development/workflows/development-workflow.md` | ✅ 已遷移 |
| `docs/development/epic.md` | `docs/viewpoints/development/workflows/epic-implementation.md` | ✅ 已遷移 |

### 設計文檔遷移

| 原始路徑 | 新路徑 | 狀態 |
|----------|--------|------|
| `docs/design/README.md` | `docs/viewpoints/development/architecture/README.md` | ✅ 已遷移 |
| `docs/design/ddd-guide.md` | `docs/viewpoints/development/architecture/ddd-patterns/tactical-patterns.md` | ✅ 已遷移 |
| `docs/design/design-principles.md` | `docs/viewpoints/development/architecture/design-principles/solid-principles.md` | ✅ 已遷移 |
| `docs/design/refactoring-guide.md` | `docs/viewpoints/development/workflows/refactoring-strategy.md` | ✅ 已遷移 |

### 測試文檔遷移

| 原始路徑 | 新路徑 | 狀態 |
|----------|--------|------|
| `docs/testing/README.md` | `docs/viewpoints/development/testing/README.md` | ✅ 已遷移 |
| `docs/testing/test-performance-monitoring.md` | `docs/viewpoints/development/testing/performance-monitoring/test-performance-extension.md` | ✅ 已遷移 |
| `docs/testing/test-optimization-guidelines.md` | `docs/viewpoints/development/testing/test-optimization.md` | ✅ 已遷移 |
| `docs/testing/http-client-configuration-guide.md` | `docs/viewpoints/development/testing/integration-testing.md` | ✅ 已遷移 |
| `docs/testing/new-developer-onboarding-guide.md` | `docs/viewpoints/development/getting-started/first-contribution.md` | ✅ 已遷移 |

## 📚 新的 Development Viewpoint 結構

```
docs/viewpoints/development/
├── README.md                           # 開發視點總覽
├── getting-started/                    # 快速入門層
│   ├── README.md                      # 入門指南總覽
│   ├── environment-setup.md           # 環境配置指南
│   ├── prerequisites.md               # 前置需求檢查清單
│   ├── first-contribution.md          # 首次貢獻指南
│   └── quickstart-checklist.md       # 快速開始檢查清單
├── architecture/                      # 架構設計層
│   ├── README.md                      # 架構指南總覽
│   ├── ddd-patterns/                  # DDD 模式子目錄
│   │   ├── README.md                  # DDD 模式總覽
│   │   ├── tactical-patterns.md       # 戰術模式：@AggregateRoot, @ValueObject, @Entity, @DomainService
│   │   ├── strategic-patterns.md      # 戰略模式：Bounded Context, Context Mapping
│   │   ├── domain-events.md           # 領域事件：Record 實作、事件收集與發布
│   │   └── aggregate-design.md        # 聚合設計原則和最佳實踐
│   ├── hexagonal-architecture/        # 六角架構子目錄
│   │   ├── README.md                  # 六角架構總覽
│   │   ├── ports-adapters.md          # Port-Adapter 模式實作
│   │   ├── dependency-inversion.md    # 依賴反轉原則應用
│   │   ├── layered-design.md          # 分層設計和邊界定義
│   │   └── integration-patterns.md    # 整合模式和適配器設計
│   ├── microservices/                 # 微服務架構子目錄
│   │   ├── README.md                  # 微服務架構總覽
│   │   ├── service-design.md          # 服務設計原則
│   │   ├── api-gateway.md             # API Gateway 模式
│   │   ├── service-discovery.md       # 服務發現機制
│   │   ├── load-balancing.md          # 負載均衡策略
│   │   ├── circuit-breaker.md         # 斷路器模式
│   │   └── distributed-patterns.md    # 分散式系統模式
│   ├── saga-patterns/                 # Saga 模式子目錄
│   │   ├── README.md                  # Saga 模式總覽
│   │   ├── orchestration.md           # 編排式 Saga
│   │   ├── choreography.md            # 編舞式 Saga
│   │   ├── order-processing-saga.md   # 訂單處理 Saga 實作
│   │   ├── payment-saga.md            # 支付 Saga 實作
│   │   └── saga-coordination.md       # Saga 協調機制
│   └── design-principles/             # 設計原則子目錄
│       └── solid-principles.md        # SOLID 原則和設計模式
├── coding-standards/                  # 編碼標準層
│   ├── README.md                      # 編碼標準總覽
│   ├── java-standards.md              # Java 編碼規範
│   ├── frontend-standards.md          # 前端編碼規範（React/Angular）
│   ├── api-design.md                  # API 設計規範
│   ├── documentation-standards.md     # 文檔編寫規範
│   ├── naming-conventions.md          # 命名約定
│   └── code-review-guidelines.md      # 程式碼審查指南
├── testing/                           # 測試策略層
│   ├── README.md                      # 測試策略總覽
│   ├── tdd-practices/                 # TDD 實踐子目錄
│   │   ├── README.md                  # TDD 實踐總覽
│   │   ├── red-green-refactor.md      # Red-Green-Refactor 循環
│   │   ├── test-pyramid.md            # 測試金字塔策略
│   │   └── unit-testing-patterns.md   # 單元測試模式
│   ├── bdd-practices/                 # BDD 實踐子目錄
│   │   ├── README.md                  # BDD 實踐總覽
│   │   ├── gherkin-guidelines.md      # Gherkin 語法指南
│   │   ├── given-when-then.md         # Given-When-Then 模式
│   │   ├── feature-writing.md         # Feature 文件編寫
│   │   └── scenario-design.md         # 場景設計最佳實踐
│   ├── performance-monitoring/        # 效能監控子目錄
│   │   └── test-performance-extension.md  # @TestPerformanceExtension 使用指南
│   ├── integration-testing.md         # 整合測試指南
│   ├── architecture-testing.md        # 架構測試：ArchUnit 規則
│   ├── test-optimization.md           # 測試優化指南
│   └── test-automation.md             # 測試自動化策略
├── build-system/                      # 建置系統層
│   ├── README.md                      # 建置系統總覽
│   ├── gradle-configuration.md        # Gradle 配置指南
│   ├── multi-module-setup.md          # 多模組設置
│   ├── dependency-management.md       # 依賴管理策略
│   ├── build-optimization.md          # 建置優化技巧
│   └── ci-cd-integration.md           # CI/CD 整合配置
├── quality-assurance/                 # 品質保證層
│   ├── README.md                      # 品質保證總覽
│   ├── code-review.md                 # 程式碼審查流程
│   ├── static-analysis.md             # 靜態分析工具
│   ├── security-scanning.md           # 安全掃描配置
│   ├── performance-monitoring.md      # 效能監控設置
│   └── quality-gates.md               # 品質門檻標準
├── tools-and-environment/             # 工具鏈層
│   ├── README.md                      # 工具鏈總覽
│   ├── technology-stack/              # 技術棧子目錄
│   │   ├── README.md                  # 技術棧總覽
│   │   ├── backend-stack.md           # Spring Boot 3.4.5 + Java 21 + Gradle 8.x
│   │   ├── frontend-stack.md          # Next.js 14 + React 18 + Angular 18 + TypeScript
│   │   ├── testing-stack.md           # JUnit 5 + Mockito + AssertJ + Cucumber 7
│   │   ├── database-stack.md          # H2 (dev/test) + PostgreSQL (prod) + Flyway
│   │   ├── monitoring-stack.md        # Spring Boot Actuator + AWS X-Ray + Micrometer
│   │   └── infrastructure-stack.md    # AWS CDK + EKS + MSK + Route 53
│   ├── ide-configuration.md           # IDE 配置指南
│   ├── version-control.md             # Git 工作流程和最佳實踐
│   ├── debugging-tools.md             # 除錯工具配置
│   └── development-tools.md           # 開發工具鏈整合
└── workflows/                         # 工作流程層
    ├── README.md                      # 工作流程總覽
    ├── development-workflow.md         # 開發流程標準
    ├── release-process.md              # 發布流程管理
    ├── hotfix-process.md               # 熱修復流程
    ├── refactoring-strategy.md         # 重構策略指南
    └── collaboration-guidelines.md     # 團隊協作指南
```

## 🔖 書籤更新指南

### 瀏覽器書籤更新

如果您有以下書籤，請更新為新的路徑：

#### 開發指南書籤
```
舊書籤: docs/development/README.md
新書籤: docs/viewpoints/development/README.md

舊書籤: docs/development/getting-started.md
新書籤: docs/viewpoints/development/getting-started/README.md

舊書籤: docs/development/coding-standards.md
新書籤: docs/viewpoints/development/coding-standards/README.md

舊書籤: docs/development/testing-guide.md
新書籤: docs/viewpoints/development/testing/README.md
```

#### 設計文檔書籤
```
舊書籤: docs/design/ddd-guide.md
新書籤: docs/viewpoints/development/architecture/ddd-patterns/tactical-patterns.md

舊書籤: docs/design/design-principles.md
新書籤: docs/viewpoints/development/architecture/design-principles/solid-principles.md

舊書籤: docs/design/refactoring-guide.md
新書籤: docs/viewpoints/development/workflows/refactoring-strategy.md
```

#### 測試文檔書籤
```
舊書籤: docs/testing/README.md
新書籤: docs/viewpoints/development/testing/README.md

舊書籤: docs/testing/test-performance-monitoring.md
新書籤: docs/viewpoints/development/testing/performance-monitoring/test-performance-extension.md

舊書籤: docs/testing/test-optimization-guidelines.md
新書籤: docs/viewpoints/development/testing/test-optimization.md
```

### IDE 書籤和快速存取

如果您在 IDE 中設置了快速存取或書籤，請更新路徑：

#### VS Code 工作區設置
```json
{
  "folders": [
    {
      "name": "Development Docs",
      "path": "./docs/viewpoints/development"
    }
  ]
}
```

#### IntelliJ IDEA 書籤
- 移除舊的 `docs/development/` 書籤
- 新增 `docs/viewpoints/development/` 書籤

## 📝 外部引用處理

### 文檔連結更新

如果您在其他文檔中引用了舊路徑，請更新：

```markdown
<!-- 舊引用 -->
[開發指南](docs/development/README.md)
[DDD 指南](docs/design/ddd-guide.md)
[測試指南](docs/testing/README.md)

<!-- 新引用 -->
[開發指南](docs/viewpoints/development/README.md)
[DDD 指南](docs/viewpoints/development/architecture/ddd-patterns/tactical-patterns.md)
[測試指南](docs/viewpoints/development/testing/README.md)
```

### Wiki 和外部文檔

如果您在 Wiki、Confluence 或其他外部系統中引用了這些文檔：

1. **更新所有連結** 到新的路徑
2. **檢查嵌入的文檔** 是否需要更新
3. **通知團隊成員** 關於路徑變更

## 🔄 過渡期支援

### 重定向文檔

在過渡期間（2025年2月底前），舊目錄中的 README.md 文件將提供：

- **清晰的遷移通知**
- **新位置的直接連結**
- **完整的對照表**
- **快速導航指南**

### 自動重定向

我們已在舊目錄中設置了重定向 README 文件：

- `docs/development/README.md` - 指向新的開發視點
- `docs/design/README.md` - 指向新的架構模式
- `docs/testing/README.md` - 指向新的測試策略

## 🆘 需要幫助？

### 常見問題

**Q: 我找不到某個特定的文檔，怎麼辦？**
A: 請參考上面的完整對照表，或查看 [Development Viewpoint 總覽](docs/viewpoints/development/README.md)

**Q: 舊的連結還能用嗎？**
A: 在過渡期間（2025年2月底前），舊目錄中的 README 文件會提供重定向指引

**Q: 新結構有什麼優勢？**
A: 更系統化的組織、更完整的內容、更好的維護性，以及符合 Rozanski & Woods 架構方法論

### 聯繫支援

如果您在遷移過程中遇到問題：

1. **查看重定向文檔** - 舊目錄中的 README.md 文件
2. **參考對照表** - 本文檔中的完整對照表
3. **查看新結構** - [Development Viewpoint 總覽](docs/viewpoints/development/README.md)
4. **提出問題** - 在專案中創建 Issue

---

**遷移完成日期**: 2025年1月21日  
**過渡期結束**: 2025年2月28日  
**舊目錄移除**: 2025年3月1日

**感謝您的配合！** 新的 Development Viewpoint 結構將為您提供更好的開發文檔體驗。