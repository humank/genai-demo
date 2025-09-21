
# Guidelines

## 概述

AI-DLC (AI-Driven Development Lifecycle) 是一個結合人工智慧輔助的軟體開發生命週期方法論，從Requirements Analysis到程式碼實現的完整流程。

## 開發階段

### Inception (啟動階段)

#### Intent to User Stories (意圖轉換為User故事)

**角色**: Product Manager專家

**任務**: 將系統描述轉換為明確定義的User故事，作為系統開發的契約。

**流程**:
1. 在 `aidlc-docs/inception/user_stories_plan.md` 中規劃工作步驟
2. 為每個步驟建立核取方塊
3. 標記需要澄清的問題 `[Question]` 和空白回答 `[Answer]`
4. 請求審查和批准
5. 逐步執行計劃並標記完成

**範例任務**: 
- 解決方案Architect註冊技能和Availability
- 銷售經理註冊Customer機會和問題陳述
- 系統根據技能和Availability匹配Customer機會與解決方案Architect
- 銷售經理從系統推薦選項中選擇解決方案Architect

#### Stories to Units (故事轉換為工作單元)

**角色**: 經驗豐富的軟體Architect

**任務**: 將User故事分組為可並行實現的工作單元，每個單元包含高度內聚的User故事。

**流程**:
1. 理解完整系統的User故事
2. 將故事分組為工作單元（等同於 DDD 中的Bounded Context）
3. 在 `aidlc-docs/inception/units/` 資料夾中為每個單元建立個別的 md 檔案
4. 在 `aidlc-docs/inception/units/units_plan.md` 中規劃步驟

**原則**:
- 每個單元可由單一團隊建置
- 單元對應特定子領域或業務角色
- 高內聚、低耦合的設計

#### BDD Specifications (BDD 規格)

**角色**: 經驗豐富的 BDD 實踐者

**任務**: 使用 Specification by Example 方法建立具體範例，說明每個User故事在不同場景下的行為。

**流程**:
1. 為每個單元建立 BDD 規格
2. 使用 Given-When-Then 格式
3. 專注於具體範例、邊界條件和錯誤場景
4. 儲存在 `aidlc-docs/inception/bdd/` 資料夾

**重點**:
- 具體的行為範例
- 邊界條件和業務規則
- 錯誤處理場景

#### Shared Data Models (共享資料模型)

**角色**: 經驗豐富的軟體Architect

**任務**: 建立組件模型，定義核心Entity及其屬性。

**流程**:
1. 參考 `aidlc-docs/inception/dependencies_integration_summary.md`
2. 建立核心Entity的組件模型
3. 定義屬性和關係
4. 儲存在 `aidlc-docs/inception/units/shared_model.md`

### Construction (建構階段)

#### Requirements

**角色**: 經驗豐富的軟體工程師

**任務**: 分析 BDD 規格以提取領域需求和業務規則，指導領域模型設計。

**流程**:
1. 審查所有 BDD 規格
2. 識別領域行為、業務規則、Constraint和不變量
3. 記錄在 `aidlc-docs/construction/domain_requirements.md`

#### Domain Modelling (領域建模)

**角色**: 經驗豐富的軟體工程師

**任務**: 設計領域模型以實現所有User故事，包含組件、屬性、行為和互動。

**流程**:
1. 參考User故事和領域需求
2. 設計組件模型
3. 定義屬性和行為
4. 描述組件互動
5. 儲存在 `aidlc-docs/construction/` 資料夾

**原則**:
- 不產生架構組件
- 不產生程式碼
- 嚴格參考共享組件定義

#### Domain Model to Code (領域模型轉換為程式碼)

**角色**: 經驗豐富的軟體工程師

**任務**: 將領域模型轉換為簡單直觀的程式碼實現。

**流程**:
1. 參考 `aidlc-docs/construction/domain_model.md`
2. 產生 Python 實現（或其他語言）
3. 保持目錄結構扁平
4. 使用標準組件和工具
5. Assumption儲存庫為記憶體內實現

**要求**:
- 簡單直觀的實現
- 重用標準組件
- 個別檔案中的類別

#### Testing

**角色**: 經驗豐富的測試自動化工程師

**任務**: 實現可執行的 BDD 測試，驗證實現的領域程式碼。

**流程**:
1. 將 BDD 規格轉換為可執行測試
2. 使用適當的測試框架（如 pytest-bdd）
3. 確保所有 Given-When-Then 場景覆蓋
4. 建立測試檔案在 `aidlc-docs/construction/tests/`

#### Adding Architectural Components (新增架構組件)

**角色**: 經驗豐富的軟體Architect

**任務**: 新增架構組件以支援網路存取和外部整合。

**流程**:
1. 參考服務實現
2. 設計 API 介面
3. 實現網路存取層
4. 整合外部服務

## Tools

### 程式語言和框架
- **後端**: Java 21 + Spring Boot 3.4.5
- **前端**: TypeScript + React + Next.js
- **測試**: JUnit 5 + Cucumber + Mockito

### Tools
- **建置工具**: Gradle 8.x
- **版本控制**: Git
- **IDE**: IntelliJ IDEA / VS Code
- **Containerization**: Docker + Docker Compose

### Quality Assurance
- **程式碼格式化**: Spotless
- **Static Analysis**: SonarQube
- **Architecture Test**: ArchUnit
- **測試報告**: Allure

## Best Practices

### 規劃階段
1. **明確定義問題**: 確保所有Stakeholder理解需求
2. **詳細規劃**: 為每個階段建立詳細的執行計劃
3. **風險識別**: 提前識別潛在問題和依賴關係

### 實現階段
1. **Test-Driven Development (TDD)**: 先寫測試，再實現功能
2. **Continuous Integration (CI)**: 頻繁提交和整合程式碼
3. **Code Review**: 確保Code Quality和知識分享

### 品質控制
1. **Automated Testing**: 建立完整的測試套件
2. **持續Monitoring**: Monitoring系統Performance和錯誤
3. **文件維護**: 保持文件與程式碼同步

## Related Diagrams

- [AI-DLC 流程圖](../../diagrams/viewpoints/development/ai-dlc-workflow.mmd)
- [開發階段關係圖](../../diagrams/viewpoints/development/development-phases.puml)

## Relationships with Other Viewpoints

- **[Functional Viewpoint](../functional/README.md)**: User故事和Requirements Analysis
- **[Information Viewpoint](../information/README.md)**: 資料模型設計和事件處理
- **[Deployment Viewpoint](../deployment/README.md)**: 建置和Deployment自動化
- **[Operational Viewpoint](../operational/README.md)**: Monitoring和維護Policy