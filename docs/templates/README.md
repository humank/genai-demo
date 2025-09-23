
<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 文件模板系統

## 概覽

本目錄包含了基於 Rozanski & Woods 方法論的完整文件模板系統，用於創建一致性的架構文件和圖表。

## 模板結構

```
docs/templates/
├── README.md                           # 本文件 - 模板使用指南
├── viewpoint-template.md               # Viewpoint 文件模板
├── perspective-template.md             # Perspective 文件模板
├── metadata-standards.md               # 文件元資料標準格式
├── diagram-metadata-standards.md       # 圖表元資料標準格式
└── examples/                           # 範例文件
    ├── functional-viewpoint-example.md
    ├── security-perspective-example.md
    └── diagram-examples/
        ├── system-overview.md  # 包含 Mermaid 代碼塊
        ├── domain-model.puml
        └── concept-diagram.excalidraw
```

## 模板使用指南

### 1. Viewpoint 文件創建

**使用場景**: 創建七大架構視點文件時使用

**步驟**:
1. 複製 `viewpoint-template.md` 到目標位置
2. 根據具體視點更新 Front Matter 元資料
3. 填寫各個章節內容
4. 確保所有必填欄位都已完成

**命名規範**:
- 檔案路徑: `docs/viewpoints/{viewpoint}/README.md`
- 具體文件: `docs/viewpoints/{viewpoint}/{specific-topic}.md`

**範例**:
```bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 創建功能視點文件
cp docs/templates/viewpoint-template.md docs/viewpoints/functional/domain-model.md


<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 編輯元資料

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 將 viewpoint: "[viewpoint]" 改為 viewpoint: "functional"

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 更新其他相關欄位
```

### 2. Perspective 文件創建

**使用場景**: 創建八大架構觀點文件時使用

**步驟**:
1. 複製 `perspective-template.md` 到目標位置
2. 根據具體觀點更新 Front Matter 元資料
3. 填寫跨視點應用章節
4. 定義品質屬性和度量指標

**命名規範**:
- 檔案路徑: `docs/perspectives/{perspective}/README.md`
- 具體文件: `docs/perspectives/{perspective}/{specific-topic}.md`

**範例**:
```bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 創建安全性觀點文件
cp docs/templates/perspective-template.md docs/perspectives/security/authentication.md


<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 編輯元資料

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 將 perspective_type: "[perspective]" 改為 perspective_type: "security"

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 更新適用視點和品質屬性
```

### 3. 圖表文件創建

**使用場景**: 創建各種類型的架構圖表時使用

**步驟**:
1. 根據圖表類型選擇合適的元資料格式
2. 在圖表檔案開頭添加 Front Matter
3. 設定正確的視點和觀點關聯
4. 配置自動生成和維護資訊

**支援的圖表類型**:
- **Mermaid** (.md with ```mermaid blocks): 適合概覽圖和流程圖，使用 GitHub 原生支援的代碼塊格式
- **PlantUML** (.puml): 適合詳細的 UML 圖表
- **Excalidraw** (.excalidraw): 適合概念設計和手繪風格

## 元資料標準

### 必填欄位檢查清單

#### Viewpoint 文件
- [ ] `title`: 清楚的文件標題
- [ ] `viewpoint`: 七大視點之一
- [ ] `last_updated`: 最後更新日期 (YYYY-MM-DD)
- [ ] `version`: 版本號 (語意化版本)
- [ ] `author`: 作者或團隊

#### Perspective 文件
- [ ] `title`: 清楚的文件標題
- [ ] `perspective_type`: 八大觀點之一
- [ ] `applicable_viewpoints`: 適用的視點陣列
- [ ] `quality_attributes`: 相關品質屬性
- [ ] `last_updated`: 最後更新日期
- [ ] `version`: 版本號
- [ ] `author`: 作者或團隊

#### 圖表文件
- [ ] `title`: 圖表標題
- [ ] `type`: 圖表類型 (mermaid|plantuml|excalidraw)
- [ ] `format`: 檔案格式
- [ ] `description`: 圖表描述
- [ ] `last_updated`: 最後更新日期
- [ ] `version`: 版本號
- [ ] `author`: 作者或團隊

### 品質檢查

使用以下腳本驗證元資料完整性:

```bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 檢查所有文件的元資料
./scripts/validate-metadata.sh


<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 檢查特定目錄
./scripts/validate-metadata.sh docs/viewpoints/


<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 生成元資料報告
./scripts/generate-metadata-report.sh
```

## 自動化工具

### 文件生成器

```bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 使用模板快速創建文件
./scripts/create-viewpoint-doc.sh functional domain-model
./scripts/create-perspective-doc.sh security authentication
./scripts/create-diagram.sh mermaid system-overview functional
```

### 元資料更新器

```bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 批量更新元資料
./scripts/update-metadata.sh --field last_updated --value $(date +%Y-%m-%d)


<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 驗證關聯性
./scripts/validate-references.sh
```

### 翻譯自動化

```bash

<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 觸發自動翻譯
./scripts/trigger-translation.sh docs/viewpoints/functional/domain-model.md


<!-- 
注意：Mermaid 圖表格式更新
- 舊格式：.mmd 文件引用
- 新格式：.md 文件中的 ```mermaid 代碼塊
- 原因：GitHub 原生支援，更好的可讀性和維護性
-->

# 檢查翻譯狀態
./scripts/check-translation-status.sh
```

## 最佳實踐

### 1. 文件撰寫

**結構化撰寫**:
- 使用模板提供的章節結構
- 保持章節間的邏輯一致性
- 確保內容的完整性和準確性

**內容品質**:
- 使用清楚、簡潔的語言
- 提供具體的範例和程式碼
- 包含相關的圖表和視覺化內容

**關聯性管理**:
- 正確設定相關文件連結
- 維護視點間的一致性
- 確保圖表與文件內容同步

### 2. 元資料管理

**一致性**:
- 使用標準化的標籤和分類
- 保持命名規範的一致性
- 定期檢查和更新元資料

**可追蹤性**:
- 記錄文件的變更歷史
- 維護版本控制資訊
- 設定適當的審查流程

**自動化**:
- 使用 Git hooks 驗證元資料
- 設定 CI/CD 流程自動檢查
- 配置自動翻譯和同步

### 3. 圖表管理

**工具選擇**:
- **Mermaid**: GitHub 直接顯示，適合概覽圖
- **PlantUML**: 功能強大，適合詳細 UML 圖
- **Excalidraw**: 手繪風格，適合概念設計

**版本控制**:
- 同時維護原始檔案和生成的圖片
- 設定自動生成流程
- 記錄圖表的變更原因

**品質控制**:
- 定期檢查圖表的準確性
- 確保圖表符合可訪問性標準
- 優化圖表的載入性能

## 維護和更新

### 定期維護任務

**每週**:
- [ ] 檢查新增文件的元資料完整性
- [ ] 驗證文件間的關聯性
- [ ] 更新過期的連結和參考

**每月**:
- [ ] 審查文件內容的準確性
- [ ] 更新版本號和日期
- [ ] 檢查翻譯同步狀態

**每季**:
- [ ] 評估模板的適用性
- [ ] 更新最佳實踐指南
- [ ] 優化自動化流程

### 模板演進

**版本控制**:
- 模板變更需要版本號更新
- 記錄變更原因和影響範圍
- 提供遷移指南

**向後相容性**:
- 新版本模板應向後相容
- 提供自動遷移工具
- 保留舊版本模板的存檔

**社群回饋**:
- 收集使用者回饋
- 定期評估模板效果
- 持續改進模板設計

## 故障排除

### 常見問題

**Q: 元資料驗證失敗**
A: 檢查 YAML 語法，確保必填欄位完整，驗證日期格式

**Q: 圖表無法生成**
A: 檢查圖表語法，確認工具版本，驗證檔案路徑

**Q: 翻譯未自動觸發**
A: 檢查 Hook 配置，確認檔案路徑匹配，驗證翻譯服務狀態

**Q: 文件關聯性錯誤**
A: 使用相對路徑，檢查檔案存在性，更新移動後的檔案路徑

### 支援資源

- **文件**: [metadata-standards.md](metadata-standards.md)
- **範例**: [examples/](examples/)
- **工具**: \1
- **問題回報**: 使用 GitHub Issues

這個模板系統確保了文件的一致性、可維護性和自動化處理能力，支援整個專案的文件重構需求。