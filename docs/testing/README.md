# 📍 測試文檔已遷移

> **重要通知**: 測試相關文檔已遷移到新的 Development Viewpoint 測試策略中

## 🚀 新位置

所有測試相關文檔現在統一整合在 **[Development Viewpoint 測試策略](../viewpoints/development/testing/)** 中，提供更完整和系統化的測試指南。

**主要入口**: [測試策略總覽](../viewpoints/development/testing/README.md)

## 📋 文檔遷移對照表

| 原始文檔 | 新位置 | 說明 |
|----------|--------|------|
| [test-execution-maintenance-guide.md](test-execution-maintenance-guide.md) | **[測試優化](../viewpoints/development/testing/test-optimization.md)** | 測試執行和維護指南 |
| [test-performance-monitoring.md](test-performance-monitoring.md) | **[TestPerformanceExtension](../viewpoints/development/testing/performance-monitoring/test-performance-extension.md)** | 測試效能監控框架 |
| [http-client-configuration-guide.md](http-client-configuration-guide.md) | **[整合測試](../viewpoints/development/testing/integration-testing.md)** | HTTP 客戶端配置指南 |
| [new-developer-onboarding-guide.md](new-developer-onboarding-guide.md) | **[入門指南](../viewpoints/development/getting-started/first-contribution.md)** | 新開發者測試入門 |
| [test-optimization-guidelines.md](test-optimization-guidelines.md) | **[測試優化](../viewpoints/development/testing/test-optimization.md)** | 測試優化指南 |
| [testresttemplate-troubleshooting-guide.md](testresttemplate-troubleshooting-guide.md) | **[整合測試](../viewpoints/development/testing/integration-testing.md)** | TestRestTemplate 故障排除 |
| [common-test-failures-troubleshooting.md](common-test-failures-troubleshooting.md) | **[測試優化](../viewpoints/development/testing/test-optimization.md)** | 常見測試失敗排除 |

## 📚 新的測試文檔結構

```
docs/viewpoints/development/testing/
├── README.md                           # 測試策略總覽
├── tdd-practices/                      # TDD 實踐指南
│   ├── red-green-refactor.md          # Red-Green-Refactor 循環
│   ├── test-pyramid.md                # 測試金字塔策略
│   └── unit-testing-patterns.md       # 單元測試模式
├── bdd-practices/                      # BDD 實踐指南
│   ├── gherkin-guidelines.md          # Gherkin 語法指南
│   ├── given-when-then.md             # Given-When-Then 模式
│   ├── feature-writing.md             # Feature 文件編寫
│   └── scenario-design.md             # 場景設計最佳實踐
├── performance-monitoring/             # 效能監控
│   └── test-performance-extension.md  # @TestPerformanceExtension 使用指南
├── integration-testing.md             # 整合測試指南
├── architecture-testing.md            # 架構測試指南
├── test-optimization.md               # 測試優化指南
└── test-automation.md                 # 測試自動化指南
```

## 📅 遷移資訊

- **遷移日期**: 2025年1月21日
- **原因**: 統一測試文檔到 Development Viewpoint 結構
- **狀態**: 已完成，內容已整合並增強

## 🚀 快速開始

### 測試執行命令

```bash
# 日常開發 - 快速反饋
./gradlew quickTest              # 單元測試 (< 2 分鐘)

# 提交前驗證  
./gradlew preCommitTest          # 單元 + 整合測試 (< 5 分鐘)

# 發布前驗證
./gradlew fullTest               # 所有測試包括 E2E (< 30 分鐘)

# 特定測試類型
./gradlew unitTest               # 快速單元測試
./gradlew integrationTest        # 整合測試
./gradlew e2eTest               # 端到端測試
./gradlew cucumber              # BDD Cucumber 測試

# 效能監控
./gradlew generatePerformanceReport  # 生成效能報告
```

### 效能基準

| 測試類型 | 執行時間 | 記憶體使用 | 成功率 |
|----------|----------|------------|--------|
| 單元測試 | < 50ms | < 5MB | > 99% |
| 整合測試 | < 500ms | < 50MB | > 95% |
| E2E 測試 | < 3s | < 500MB | > 90% |

---

*此目錄將在下一個版本中重構。請更新您的書籤和引用到新的位置。*
