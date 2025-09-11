# Steering 規則索引

## 📋 整合後的 Steering 文件結構

### 🎯 核心開發指導

- **[development-standards.md](development-standards.md)** - 開發標準與規範
  - 測試標準 (UnitTest > IntegrationTest > SpringBootTest)
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

- **文件數量**: 8個 → **5個** (減少 37.5%)
- **重複內容**: 大幅減少測試、架構、技術棧相關重複
- **Context Window**: 顯著減少 steering 規則的 token 使用
- **維護性**: 集中管理，避免信息分散

### 📊 文件大小對比

| 文件 | 整合前 | 整合後 | 變化 |
|------|--------|--------|------|
| 測試相關 | 3個文件 | 1個文件 | -67% |
| 架構相關 | 3個文件 | 1個文件 | -67% |
| 項目信息 | 3個文件 | 1個文件 | -67% |

### 🎯 使用指導

1. **日常開發** → 參考 `development-standards.md`
2. **架構設計** → 參考 `architecture-patterns.md`  
3. **項目了解** → 參考 `project-overview.md`
4. **領域事件** → 參考 `domain-events.md`
5. **文檔翻譯** → 參考 `translation-guide.md`

## 📖 詳細文檔引用

完整的實施指南和詳細文檔已移至：

- `docs/testing/` - 測試優化指南
- `docs/architecture/` - 架構設計文檔  
- `docs/development/` - 開發工具配置
