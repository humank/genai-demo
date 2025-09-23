# 測試驅動開發實踐

## 概覽

本目錄包含測試驅動開發（TDD）的實踐指南和最佳實踐。

## TDD 原則

### Red-Green-Refactor 循環
1. **Red** - 寫一個失敗的測試
2. **Green** - 寫最少的程式碼讓測試通過
3. **Refactor** - 重構程式碼提升品質

### 測試結構
- **Arrange** - 設置測試資料
- **Act** - 執行被測試的行為
- **Assert** - 驗證結果

## 測試分類

### 單元測試 (80%)
- **特點**: 快速、隔離、專注
- **範圍**: 純業務邏輯、工具函數
- **工具**: JUnit 5, Mockito, AssertJ
- **效能**: < 50ms, < 5MB

### 整合測試 (15%)
- **特點**: 元件互動、資料庫整合
- **範圍**: Repository、外部服務
- **工具**: @DataJpaTest, @WebMvcTest
- **效能**: < 500ms, < 50MB

### 端到端測試 (5%)
- **特點**: 完整業務流程驗證
- **範圍**: 使用者旅程、系統整合
- **工具**: @SpringBootTest, TestContainers
- **效能**: < 3s, < 500MB

## 最佳實踐

### 測試命名
```java
@Test
void should_throw_exception_when_email_is_invalid() {
    // 測試內容
}
```

### 測試資料建構器
```java
public static CustomerTestDataBuilder aCustomer() {
    return new CustomerTestDataBuilder();
}

Customer customer = aCustomer()
    .withName("John Doe")
    .withEmail("john@example.com")
    .build();
```

### Mock 使用原則
- Mock 外部依賴
- 不要 Mock 值對象
- 避免過度 Mock

## 相關文檔

- [TDD/BDD 測試](../tdd-bdd-testing.md)
- [測試效能標準](../../../../../.kiro/steering/test-performance-standards.md)
- [開發標準](../../../../../.kiro/steering/development-standards.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日