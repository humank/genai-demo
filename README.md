# GenAI Demo

這是一個基於領域驅動設計 (DDD) 和六角形架構 (Hexagonal Architecture) 的示範專案，展示了如何構建一個具有良好架構和測試實踐的 Java 應用程式。

## 專案架構

本專案採用六角形架構（又稱端口與適配器架構）和領域驅動設計，將應用程序分為以下幾個主要層次：

1. **領域層 (Domain Layer)**
   - 包含業務核心邏輯和規則
   - 不依賴於其他層
   - 包含聚合根、實體、值對象、領域事件、領域服務和領域異常

2. **應用層 (Application Layer)**
   - 協調領域對象完成用戶用例
   - 只依賴於領域層
   - 包含應用服務、DTO、命令和查詢對象

3. **基礎設施層 (Infrastructure Layer)**
   - 提供技術實現
   - 依賴於領域層，實現領域層定義的接口
   - 包含儲存庫實現、外部系統適配器、ORM 映射等

4. **介面層 (Interfaces Layer)**
   - 處理用戶交互
   - 只依賴於應用層
   - 包含控制器、視圖模型、請求/響應對象等

## 技術棧

- **核心框架**: Spring Boot 3.2.0
- **構建工具**: Gradle 8.x
- **測試框架**:
  - JUnit 5 - 單元測試
  - Cucumber - BDD 測試
  - ArchUnit - 架構測試
  - Mockito - 模擬對象
  - Allure - 測試報告
- **其他工具**:
  - Lombok - 減少樣板代碼
  - PlantUML - UML 圖表生成

## 文檔

專案包含豐富的文檔，位於 `docs` 目錄下：

- **架構文檔**:
  - `architecture-overview.md` - 系統架構概覽
  - `HexagonalArchitectureSummary.md` - 六角架構實現總結
  - `HexagonalRefactoring.MD` - 六角架構與 Event Storming 整合重構指南
  - `LayeredArchitectureDesign.MD` - 分層架構設計分析與建議

- **設計文檔**:
  - `DesignGuideline.MD` - 設計指南，包含 Tell, Don't Ask 原則等
  - `DesignPrinciple.md` - 系統開發與測試的設計遵循規範
  - `SoftwareDesignClassics.md` - 軟體設計經典書籍精要

- **代碼質量**:
  - `CodeAnalysis.md` - 代碼分析報告
  - `RefactoringGuidance.md` - 重構指南

- **UML 圖表**:
  - `docs/uml/` 目錄包含各種 UML 圖表，詳見 `docs/uml/README.md`

## 如何運行

### 前置條件

- JDK 17 或更高版本
- Gradle 8.x

### 構建專案

```bash
./gradlew build
```

### 運行應用

```bash
./gradlew bootRun
```

### 運行測試

#### 運行所有測試

```bash
./gradlew --no-configuration-cache runAllTests
```

#### 運行所有測試並查看 Allure 報告

```bash
./gradlew --no-configuration-cache runAllTestsWithReport
```

#### 運行特定類型的測試

```bash
# 運行單元測試
./gradlew test

# 運行 Cucumber BDD 測試
./gradlew cucumber

# 運行架構測試
./gradlew testArchitecture
```

### 生成測試報告

測試完成後，可以查看以下報告：

1. **Cucumber HTML 報告**: `app/build/reports/cucumber/cucumber-report.html`
2. **Cucumber JSON 報告**: `app/build/reports/cucumber/cucumber-report.json`
3. **JUnit HTML 報告**: `app/build/reports/tests/test/index.html`
4. **架構測試報告**: `app/build/reports/tests/architecture/index.html`
5. **Allure 報告**:
   ```bash
   ./gradlew --no-configuration-cache allureReport  # 生成報告
   ./gradlew --no-configuration-cache allureServe   # 啟動本地服務器查看報告
   ```

## 架構測試

本專案使用 ArchUnit 確保代碼遵循預定的架構規則。架構測試位於 `app/src/test/java/solid/humank/genaidemo/architecture/` 目錄下，包括：

1. **DddArchitectureTest** - 確保遵循 DDD 分層架構
2. **HexagonalArchitectureTest** - 確保遵循六角形架構
3. **DddTacticalPatternsTest** - 確保正確使用 DDD 戰術模式

運行架構測試：

```bash
./gradlew testArchitecture
```

## BDD 測試

本專案使用 Cucumber 進行行為驅動開發 (BDD) 測試。BDD 測試文件位於：

- **Feature 文件**: `app/src/test/resources/features/` 目錄
- **步驟定義**: `app/src/test/java/solid/humank/genaidemo/bdd/` 目錄

運行 BDD 測試：

```bash
./gradlew cucumber
```

## UML 圖表

本專案使用 PlantUML 生成各種 UML 圖表，包括：

- 類別圖、對象圖、組件圖、部署圖
- 時序圖、狀態圖、活動圖
- 領域模型圖、六角形架構圖、事件風暴圖等

查看 `docs/uml/README.md` 獲取更多信息。

## 常見問題

### 配置緩存問題

如果遇到配置緩存相關的錯誤，可以使用 `--no-configuration-cache` 參數：

```bash
./gradlew --no-configuration-cache <task>
```

### Allure 報告問題

如果 Allure 報告生成失敗，可以嘗試：

1. 清理項目：`./gradlew clean`
2. 確保 Allure 結果目錄存在：`mkdir -p app/build/allure-results`
3. 重新運行測試並生成報告：`./gradlew --no-configuration-cache runAllTestsWithReport`

## 貢獻

歡迎提交 Pull Request 或開 Issue 討論改進建議。

## 授權

本專案採用 MIT 授權協議 - 詳見 [LICENSE](LICENSE) 文件。