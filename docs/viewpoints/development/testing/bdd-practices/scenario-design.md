# 場景設計最佳實踐

本文檔描述 場景設計最佳實踐 的測試策略和實作方法。

## 測試原則

### 測試金字塔

- **單元測試 (80%)**：快速、隔離、專注
- **整合測試 (15%)**：組件互動驗證
- **端到端測試 (5%)**：完整用戶旅程

### 測試標準

- 測試覆蓋率 > 80%
- 單元測試執行時間 < 50ms
- 整合測試執行時間 < 500ms

## 實作指南

### 測試結構

```java
// Given-When-Then 結構
@Test
void should_do_something_when_condition_met() {
    // Given - 準備測試數據
    // When - 執行被測試的行為
    // Then - 驗證結果
}
```

### 最佳實踐

- 使用描述性的測試名稱
- 保持測試簡單和專注
- 維護測試獨立性
- 測試行為而非實作

## 相關工具

- JUnit 5：單元測試框架
- Mockito：模擬框架
- AssertJ：斷言庫
- Cucumber：BDD 測試框架

## 相關文檔

- [測試總覽](../README.md)
- [TDD 實踐](../tdd-practices/README.md)
- [BDD 實踐](../bdd-practices/README.md)

---

*本文檔遵循 [測試標準](../../../../.kiro/steering/test-performance-standards.md)*