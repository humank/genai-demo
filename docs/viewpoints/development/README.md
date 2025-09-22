# Development Viewpoint

## 概覽

Development Viewpoint 提供了完整的開發指南和最佳實踐，整合了專案中的所有開發模式、技術棧和工具鏈。本視點遵循 Rozanski & Woods 架構方法論，為開發者、架構師和技術團隊提供統一的開發標準。

## 🚀 快速開始

### 新手入門
- [📚 快速入門指南](getting-started/README.md) - 環境設置和首次貢獻
- [⚙️ 環境配置](getting-started/environment-setup.md) - 開發環境完整設置
- [✅ 前置需求](getting-started/prerequisites.md) - 必要工具和知識檢查清單

### 核心概念
- [🏗️ 架構設計](architecture/README.md) - DDD、六角架構、微服務、Saga 模式
- [📋 編碼標準](coding-standards/README.md) - Java、前端、API 設計規範
- [🧪 測試策略](testing/README.md) - TDD、BDD、效能測試、架構測試

## 🏗️ 架構與設計模式

### DDD 領域驅動設計
- [🎯 DDD 戰術模式](architecture/ddd-patterns/README.md)
  - [@AggregateRoot 聚合根](architecture/ddd-patterns/tactical-patterns.md#aggregateroot)
  - [@ValueObject 值對象](architecture/ddd-patterns/tactical-patterns.md#valueobject)
  - [@Entity 實體](architecture/ddd-patterns/tactical-patterns.md#entity)
  - [@DomainService 領域服務](architecture/ddd-patterns/tactical-patterns.md#domainservice)
- [📡 領域事件](architecture/ddd-patterns/domain-events.md) - Record 實作、事件收集與發布

### 六角架構
- [🔵 六角架構總覽](architecture/hexagonal-architecture/README.md)
- [🔌 Port-Adapter 模式](architecture/hexagonal-architecture/ports-adapters.md)
- [🔄 依賴反轉](architecture/hexagonal-architecture/dependency-inversion.md)
- [📚 分層設計](architecture/hexagonal-architecture/layered-design.md)

### 微服務架構
- [🌐 微服務設計](architecture/microservices/README.md)
- [🚪 API Gateway](architecture/microservices/api-gateway.md)
- [🔍 Service Discovery](architecture/microservices/service-discovery.md)
- [⚖️ Load Balancing](architecture/microservices/load-balancing.md)
- [🔧 Circuit Breaker](architecture/microservices/circuit-breaker.md)

### Saga 模式
- [🎭 Saga 模式總覽](architecture/saga-patterns/README.md)
- [🎼 編排式 Saga](architecture/saga-patterns/orchestration.md)
- [💃 編舞式 Saga](architecture/saga-patterns/choreography.md)
- [🛒 訂單處理 Saga](architecture/saga-patterns/order-processing-saga.md)
- [💳 支付 Saga](architecture/saga-patterns/payment-saga.md)

## 🧪 測試與品質保證

### TDD 測試驅動開發
- [🔴🟢🔵 Red-Green-Refactor](testing/tdd-practices/red-green-refactor.md)
- [🏗️ 測試金字塔](testing/tdd-practices/test-pyramid.md)
- [⚡ 單元測試模式](testing/tdd-practices/unit-testing-patterns.md)

### BDD 行為驅動開發
- [📝 Gherkin 語法](testing/bdd-practices/gherkin-guidelines.md)
- [📋 Given-When-Then](testing/bdd-practices/given-when-then.md)
- [🎬 Feature 文件編寫](testing/bdd-practices/feature-writing.md)
- [🎯 場景設計](testing/bdd-practices/scenario-design.md)

### 測試類型
- [🔗 整合測試](testing/integration-testing.md)
- [⚡ 效能測試](testing/performance-testing.md) - @TestPerformanceExtension
- [🏛️ 架構測試](testing/architecture-testing.md) - ArchUnit 規則
- [🤖 測試自動化](testing/test-automation.md)

## 🛠️ 技術棧與工具鏈

### 後端技術
- [☕ Spring Boot 3.4.5 + Java 21](tools-and-environment/technology-stack/backend-stack.md)
- [🗄️ PostgreSQL + H2 + Flyway](tools-and-environment/technology-stack/database-stack.md)
- [📊 Spring Boot Actuator + AWS X-Ray](tools-and-environment/technology-stack/monitoring-stack.md)

### 前端技術
- [⚛️ Next.js 14 + React 18](tools-and-environment/technology-stack/frontend-stack.md)
- [🅰️ Angular 18 + TypeScript](tools-and-environment/technology-stack/frontend-stack.md)
- [🎨 shadcn/ui + Radix UI](tools-and-environment/technology-stack/frontend-stack.md)

### 測試框架
- [🧪 JUnit 5 + Mockito + AssertJ](tools-and-environment/technology-stack/testing-stack.md)
- [🥒 Cucumber 7 + Gherkin](tools-and-environment/technology-stack/testing-stack.md)

### 基礎設施
- [☁️ AWS CDK + TypeScript](tools-and-environment/technology-stack/infrastructure-stack.md)
- [🐳 EKS + MSK + Route 53](tools-and-environment/technology-stack/infrastructure-stack.md)

## 🔧 建置與部署

### 建置系統
- [🐘 Gradle 配置](build-system/gradle-configuration.md)
- [📦 多模組設置](build-system/multi-module-setup.md)
- [📚 依賴管理](build-system/dependency-management.md)
- [🚀 CI/CD 整合](build-system/ci-cd-integration.md)

### 品質保證
- [👀 程式碼審查](quality-assurance/code-review.md)
- [🔍 靜態分析](quality-assurance/static-analysis.md)
- [🔒 安全掃描](quality-assurance/security-scanning.md)
- [📊 效能監控](quality-assurance/performance-monitoring.md)

## 🔄 工作流程與協作

### 開發流程
- [🔄 開發工作流程](workflows/development-workflow.md)
- [🚀 發布流程](workflows/release-process.md)
- [🔥 熱修復流程](workflows/hotfix-process.md)
- [♻️ 重構策略](workflows/refactoring-strategy.md)

### 團隊協作
- [🤝 協作指南](workflows/collaboration-guidelines.md)
- [📝 文檔標準](coding-standards/documentation-standards.md)
- [🔍 程式碼審查指南](coding-standards/code-review-guidelines.md)

## 📊 相關圖表

### 架構圖表
- [🔵 六角架構圖](../../diagrams/viewpoints/development/architecture/hexagonal-architecture.mmd)
- [🏛️ DDD 分層架構](../../diagrams/viewpoints/development/architecture/ddd-layered-architecture.mmd)
- [🌐 微服務架構](../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
- [🎭 Saga 編排模式](../../diagrams/viewpoints/development/architecture/saga-orchestration.mmd)

### 流程圖表
- [🔄 開發工作流程](../../diagrams/viewpoints/development/workflows/development-workflow.mmd)
- [🔴🟢🔵 TDD 循環](../../diagrams/viewpoints/development/workflows/tdd-cycle.mmd)
- [📝 BDD 流程](../../diagrams/viewpoints/development/workflows/bdd-process.mmd)
- [👀 程式碼審查流程](../../diagrams/viewpoints/development/workflows/code-review-process.mmd)

## 🎯 SOLID 原則與設計模式

### SOLID 原則
- [📏 單一職責原則 (SRP)](architecture/design-principles.md#single-responsibility)
- [🔓 開放封閉原則 (OCP)](architecture/design-principles.md#open-closed)
- [🔄 里氏替換原則 (LSP)](architecture/design-principles.md#liskov-substitution)
- [🔌 介面隔離原則 (ISP)](architecture/design-principles.md#interface-segregation)
- [🔄 依賴反轉原則 (DIP)](architecture/design-principles.md#dependency-inversion)

### 設計模式
- [🏭 Factory 模式](architecture/design-principles.md#factory-pattern)
- [🔨 Builder 模式](architecture/design-principles.md#builder-pattern)
- [📋 Strategy 模式](architecture/design-principles.md#strategy-pattern)
- [👁️ Observer 模式](architecture/design-principles.md#observer-pattern)
- [🙈 Show Don't Ask](architecture/design-principles.md#show-dont-ask)

## 📚 學習路徑

### 初學者路徑
1. [📚 快速入門](getting-started/README.md)
2. [☕ Java 編碼標準](coding-standards/java-standards.md)
3. [🧪 單元測試基礎](testing/tdd-practices/unit-testing-patterns.md)
4. [🏗️ 基本架構概念](architecture/README.md)

### 中級開發者路徑
1. [🎯 DDD 戰術模式](architecture/ddd-patterns/tactical-patterns.md)
2. [🔵 六角架構實作](architecture/hexagonal-architecture/README.md)
3. [🔴🟢🔵 TDD 實踐](testing/tdd-practices/red-green-refactor.md)
4. [📝 BDD 場景設計](testing/bdd-practices/scenario-design.md)

### 高級架構師路徑
1. [🌐 微服務設計](architecture/microservices/README.md)
2. [🎭 Saga 模式實作](architecture/saga-patterns/README.md)
3. [🔧 分散式系統模式](architecture/microservices/distributed-patterns.md)
4. [📊 系統監控與可觀測性](tools-and-environment/technology-stack/monitoring-stack.md)

## 🔗 相關資源

### 內部連結
- [📋 Functional Viewpoint](../functional/README.md) - 功能需求和業務邏輯
- [📊 Information Viewpoint](../information/README.md) - 資料模型和資訊流
- [⚡ Concurrency Viewpoint](../concurrency/README.md) - 並發處理和事件驅動
- [🌐 Context Viewpoint](../context/README.md) - 系統邊界和外部整合
- [🚀 Deployment Viewpoint](../deployment/README.md) - 部署和基礎設施

### 外部資源
- [Rozanski & Woods Architecture Viewpoints](https://www.viewpoints-and-perspectives.info/)
- [Domain-Driven Design Reference](https://domainlanguage.com/ddd/reference/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)

---

**最後更新**: 2025年1月21日  
**維護者**: Development Team  
**版本**: 1.0  
**狀態**: Active

> 💡 **提示**: 這是一個活躍維護的文檔。如果你發現任何問題或有改進建議，請通過 GitHub Issues 或直接聯繫開發團隊。