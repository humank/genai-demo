# GenAI Demo

This is a demo project for GenAI.

## 如何運行 Cucumber 測試

### 使用 Gradle 運行

```bash
./gradlew cucumberTest
```

### 生成測試報告

測試完成後，可以查看以下報告：

1. HTML 報告：`app/build/reports/cucumber/report.html`
2. JSON 報告：`app/build/reports/cucumber/report.json`
3. Timeline 報告：`app/build/reports/cucumber/timeline`
4. Allure 報告：

```bash
./gradlew allureReport
./gradlew allureServe  # 啟動本地服務器查看報告
```

## 測試架構

本項目使用 Cucumber 進行 BDD 測試，主要組件包括：

1. Feature 文件：位於 `src/test/resources/features` 目錄下，使用 Gherkin 語法描述測試場景
2. Step Definitions：位於 `src/test/java/solid/humank/genaidemo/bdd/steps` 目錄下，實現 Feature 文件中的步驟
3. Test Runners：位於 `src/test/java/solid/humank/genaidemo/bdd` 目錄下，用於運行測試

### 依賴注入

本項目使用 PicoContainer 和 Spring 進行依賴注入，主要配置包括：

1. `CucumberPicoConfig`：配置 PicoContainer 與 Spring 的集成
2. `PicoFactory`：自定義的 PicoContainer Factory，用於解決依賴注入問題
3. `CucumberSpringConfig`：配置 Spring 與 Cucumber 的集成
4. `CucumberTestConfig`：提供測試環境所需的 Bean

### Mock 對象

本項目使用 Mockito 創建 Mock 對象，主要包括：

1. `MockFactory`：用於創建和管理 Mock 對象
2. `MockBeanFactory`：用於創建 Spring Bean 的 Mock 對象

## 測試報告

本項目使用多種報告工具：

1. Cucumber HTML 報告
2. Cucumber JSON 報告
3. Cucumber Timeline 報告
4. Allure 報告

## 常見問題

### UnsatisfiableDependenciesException

如果遇到 `UnsatisfiableDependenciesException` 問題，可能是因為：

1. 依賴注入配置不正確
2. Mock 對象未正確創建
3. 步驟定義類的構造函數參數無法解析

解決方法：

1. 確保所有依賴都已正確配置
2. 使用 `@Autowired` 注解注入依賴
3. 使用構造函數注入依賴
4. 檢查 `PicoFactory` 的配置