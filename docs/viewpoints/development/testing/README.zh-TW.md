# 測試策略與實踐

## 概覽

本目錄包含開發視點中的測試相關文檔，涵蓋了測試驅動開發（TDD）、行為驅動開發（BDD）和各種測試實踐。

## 目錄結構

- **[tdd-practices/](tdd-practices/)** - 測試驅動開發實踐和指南
- **[bdd-practices/](bdd-practices/)** - 行為驅動開發實踐和 Gherkin 語法

## 核心文檔

- **[TDD/BDD 測試](tdd-bdd-testing.md)** - 測試驅動開發和行為驅動開發指南

## 測試金字塔

我們遵循測試金字塔原則：

```
    /\
   /E2E\     <- 5%  端到端測試
  /______\
 /        \
/Integration\ <- 15% 整合測試  
\____________/
\            /
 \   Unit   /  <- 80% 單元測試
  \________/
```

## 測試標準

### 效能基準
- **單元測試**: < 50ms, < 5MB, 成功率 > 99%
- **整合測試**: < 500ms, < 50MB, 成功率 > 95%
- **端到端測試**: < 3s, < 500MB, 成功率 > 90%

### 測試分類
- `@UnitTest` - 純業務邏輯測試
- `@IntegrationTest` - 資料庫和外部服務整合測試
- `@SlowTest` - 長時間執行的測試
- `@SmokeTest` - 冒煙測試

## 測試工具

- **JUnit 5** - 單元測試框架
- **Mockito** - Mock 框架
- **AssertJ** - 斷言庫
- **Cucumber** - BDD 測試框架
- **TestContainers** - 整合測試容器

## 相關資源

- [測試效能標準](../../../../.kiro/steering/test-performance-standards.md)
- [BDD/TDD 原則](../../../../.kiro/steering/bdd-tdd-principles.md)
- [程式碼審查標準](../../../../.kiro/steering/code-review-standards.md)

---

**維護者**: 開發團隊  
**最後更新**: 2025年1月21日  
**版本**: 1.0
![Microservices Overview](../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)
