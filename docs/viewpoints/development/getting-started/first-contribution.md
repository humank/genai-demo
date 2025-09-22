# 首次貢獻指南

本文檔提供 首次貢獻指南 的詳細指南。

## 快速開始

### 前置需求

- Java 21 或更高版本
- Node.js 18 或更高版本
- Git 版本控制
- IDE (IntelliJ IDEA 推薦)

### 環境設置

```bash
# 複製專案
git clone <repository-url>
cd genai-demo

# 建置專案
./gradlew build

# 執行測試
./gradlew test

# 啟動應用
./gradlew bootRun
```

## 開發流程

### 1. 功能開發

- 建立功能分支
- 編寫 BDD 場景
- TDD 實作功能
- 執行測試驗證

### 2. 程式碼審查

- 建立 Pull Request
- 同儕審查程式碼
- 修正審查意見
- 合併到主分支

## 最佳實踐

### 編碼規範

- 遵循 Java 編碼標準
- 使用有意義的命名
- 保持程式碼簡潔

### 測試策略

- 單元測試優先
- 整合測試驗證
- BDD 場景覆蓋

## 相關資源

- [開發視點總覽](../README.md)
- [架構指南](../architecture/README.md)
- [測試指南](../testing/README.md)

---

*歡迎加入開發團隊！如有問題請參考相關文檔或聯繫團隊成員。*