# Steering 規則索引

## 📋 整合後的 Steering 文件結構

### 🎯 核心開發指導

- **[development-standards.md](development-standards.md)** - 開發標準與規範
  - 測試分層策略與優化 (98.2% 性能提升)
  - 測試標籤系統 (@UnitTest, @SmokeTest, @IntegrationTest)
  - BDD/TDD 開發流程
  - 代碼規範與命名約定
  - 質量標準與性能基準

### 🏗️ 架構設計指導  

- **[architecture-patterns.md](architecture-patterns.md)** - 架構模式與設計原則
  - DDD 戰術模式 (聚合根、值對象、領域事件)
  - 六角架構實現 (端口與適配器)
  - 事件驅動架構 (CQRS、事件存儲)

### 📚 項目信息

- **[project-overview.md](project-overview.md)** - 項目概覽與技術棧
  - 項目簡介與核心特性
  - 完整技術棧信息
  - 項目結構與快速開始

### 🔧 專項指導

- **[domain-events.md](domain-events.md)** - 領域事件詳細實現指南
- **[translation-guide.md](translation-guide.md)** - 文檔翻譯自動化規則

## 🚀 整合效果

### ✅ 優化成果

#### Steering 文件整合

- **文件數量**: 7個 → **6個** (進一步精簡)
- **測試內容整合**: 獨立測試指南 → development-standards.md
- **Context Window**: 顯著減少 steering 規則的 token 使用
- **維護性**: 集中管理，避免信息分散

#### 測試性能優化 (2025年1月)

- **測試執行時間**: 13分52秒 → 15秒 (**98.2% 改善**)
- **記憶體使用**: 6GB → 1-3GB (**50-83% 節省**)
- **並行執行**: 單線程 → 多核心 (**8倍提升**)

### 📊 文件結構對比

| 類別 | 整合前 | 整合後 | 變化 |
|------|--------|--------|------|
| 測試相關 | 2個獨立文件 | 整合到 development-standards.md | -100% |
| 架構相關 | 1個文件 | 1個文件 | 保持 |
| 項目信息 | 1個文件 | 1個文件 | 保持 |
| 專項指導 | 2個文件 | 2個文件 | 保持 |

### 🎯 使用指導

1. **日常開發** → 參考 `development-standards.md`
2. **架構設計** → 參考 `architecture-patterns.md`  
3. **項目了解** → 參考 `project-overview.md`
4. **領域事件** → 參考 `domain-events.md`
5. **文檔翻譯** → 參考 `translation-guide.md`

## 📖 詳細文檔引用

完整的實施指南和詳細文檔已移至：

- `docs/architecture/` - 架構設計文檔  
- `docs/development/` - 開發工具配置
- `app/build/reports/jacoco/test/html/index.html` - 測試覆蓋率報告

## 🎯 快速開始

### 測試執行 (優化後)

```bash
./gradlew quickTest      # 快速檢查 (2秒)
./gradlew unitTest       # 單元測試 (11秒)  
./gradlew integrationTest # 集成測試
./gradlew test           # 完整測試套件
```

### 開發流程

1. **日常開發**: 使用 `quickTest` 快速回饋
2. **提交前**: 運行 `unitTest` 完整驗證
3. **PR 檢查**: 執行 `integrationTest` 集成驗證
4. **發布前**: 運行 `test` 完整測試套件
