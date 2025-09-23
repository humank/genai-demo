# 行為驅動開發實踐

## 概覽

本目錄包含行為驅動開發（BDD）的實踐指南和 Gherkin 語法使用。

## BDD 原則

### 三個層次
1. **Discovery** - 發現和理解需求
2. **Formulation** - 將需求轉換為可執行的規格
3. **Automation** - 自動化執行規格

### Given-When-Then 格式
- **Given** - 設定初始條件
- **When** - 執行動作
- **Then** - 驗證結果

## Gherkin 語法

### 基本結構
```gherkin
Feature: 客戶管理
  作為系統管理員
  我想要管理客戶資料
  以便提供更好的服務

  Background:
    Given 系統已經啟動
    And 管理員已經登入

  Scenario: 成功創建客戶
    Given 我在客戶管理頁面
    When 我輸入有效的客戶資訊
    And 我點擊"創建客戶"按鈕
    Then 我應該看到"客戶創建成功"的訊息
    And 新客戶應該出現在客戶列表中
```

### 場景大綱
```gherkin
Scenario Outline: 驗證客戶資料
  Given 我在客戶創建表單
  When 我輸入 "<name>" 作為姓名
  And 我輸入 "<email>" 作為電子郵件
  Then 我應該看到 "<result>"

  Examples:
    | name     | email           | result     |
    | John Doe | john@email.com  | 創建成功   |
    | ""       | john@email.com  | 姓名必填   |
    | John Doe | invalid-email   | 郵件格式錯誤 |
```

## 步驟定義實現

### Java 步驟定義
```java
@Given("我在客戶管理頁面")
public void 我在客戶管理頁面() {
    customerPage.navigate();
}

@When("我輸入有效的客戶資訊")
public void 我輸入有效的客戶資訊() {
    customerPage.enterName("John Doe");
    customerPage.enterEmail("john@example.com");
}

@Then("我應該看到{string}的訊息")
public void 我應該看到的訊息(String expectedMessage) {
    String actualMessage = customerPage.getSuccessMessage();
    assertThat(actualMessage).isEqualTo(expectedMessage);
}
```

## 最佳實踐

### 場景設計
- 使用業務語言，避免技術細節
- 每個場景測試一個業務規則
- 保持場景簡潔明瞭
- 使用具體的例子

### 步驟重用
- 創建可重用的步驟定義
- 使用參數化步驟
- 建立共用的 Background

### 資料管理
- 使用測試資料建構器
- 清理測試資料
- 避免測試間的依賴

## 相關文檔

- [TDD/BDD 測試](../tdd-bdd-testing.md)
- [BDD/TDD 原則](../../../../../.kiro/steering/bdd-tdd-principles.md)
- [Cucumber 配置](../../tools-and-environment/technology-stack.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日