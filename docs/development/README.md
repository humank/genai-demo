# 📍 開發文檔已遷移

> **重要通知**: 開發相關文檔已遷移到新的 Development Viewpoint 結構中

## 🚀 新位置

所有開發相關文檔現在統一整合在 **[Development Viewpoint](../viewpoints/development/)** 中，提供更完整和系統化的開發指南。

**主要入口**: [開發視點總覽](../viewpoints/development/README.md)

## 📋 文檔遷移對照表

| 原始文檔 | 新位置 | 說明 |
|----------|--------|------|
| [getting-started.md](getting-started.md) | **[入門指南](../viewpoints/development/getting-started/README.md)** | 開發環境設置和快速開始 |
| [coding-standards.md](coding-standards.md) | **[編碼標準](../viewpoints/development/coding-standards/README.md)** | 程式碼風格和品質標準 |
| [testing-guide.md](testing-guide.md) | **[測試策略](../viewpoints/development/testing/README.md)** | 測試策略和最佳實踐 |
| [documentation-guide.md](documentation-guide.md) | **[文檔標準](../viewpoints/development/coding-standards/documentation-standards.md)** | 文檔撰寫和維護標準 |
| [instructions.md](instructions.md) | **[開發工作流程](../viewpoints/development/workflows/development-workflow.md)** | 詳細的開發流程和工具使用 |
| [epic.md](epic.md) | **[Epic 實現指南](../viewpoints/development/workflows/epic-implementation.md)** | 大型功能開發指南 |

## 📚 新的開發文檔結構

```
docs/viewpoints/development/
├── README.md                           # 開發視點總覽
├── getting-started/                    # 快速入門
│   ├── README.md                      # 快速入門指南
│   ├── environment-setup.md           # 環境配置指南
│   ├── prerequisites.md               # 前置需求檢查清單
│   └── first-contribution.md          # 首次貢獻指南
├── architecture/                      # 架構設計
│   ├── ddd-patterns/                  # DDD 模式
│   ├── hexagonal-architecture/        # 六角架構
│   ├── microservices/                 # 微服務架構
│   └── saga-patterns/                 # Saga 模式
├── coding-standards/                  # 編碼標準
│   ├── README.md                      # 編碼標準總覽
│   ├── java-standards.md              # Java 編碼規範
│   ├── frontend-standards.md          # 前端編碼規範
│   └── api-design.md                  # API 設計規範
├── testing/                           # 測試策略
│   ├── README.md                      # 測試策略總覽
│   ├── tdd-practices/                 # TDD 實踐
│   ├── bdd-practices/                 # BDD 實踐
│   └── performance-monitoring/        # 效能監控
├── workflows/                         # 工作流程
│   ├── README.md                      # 工作流程總覽
│   ├── development-workflow.md        # 開發流程標準
│   └── release-process.md             # 發布流程管理
└── tools-and-environment/             # 工具鏈
    ├── README.md                      # 工具鏈總覽
    └── technology-stack/              # 技術棧
```

## 🚀 快速開始

### 開發環境

- **Java**: 21
- **Spring Boot**: 3.4.5
- **Gradle**: 8.x
- **Node.js**: 18+ (前端)

### 基本命令

```bash
# 建置專案
./gradlew build

# 執行測試
./gradlew test

# 啟動應用
./gradlew bootRun
```

## 📅 遷移資訊

- **遷移日期**: 2025年1月21日
- **原因**: 統一開發文檔到 Development Viewpoint 結構
- **狀態**: 已完成，內容已整合並增強

---

*此目錄將在下一個版本中重構。請更新您的書籤和引用到新的位置。*
