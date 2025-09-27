# 處理孤立的 .mmd 文件 - Mermaid 遷移後續清理

## 📋 背景

在完成 Mermaid 圖表遷移後，發現了 17 個孤立的 .mmd 文件。這些文件包含有效的 Mermaid 圖表內容，但目前沒有被任何文檔引用。

## 🎯 目標

1. 評估每個孤立文件的價值
2. 轉換有價值的內容為標準的 .md 文檔格式
3. 整合相關內容到現有文檔中
4. 清理重複或過時的文件

## 📁 孤立文件清單

### 根目錄圖表文件 (7 個)
- `docs/diagrams/multi_environment.mmd` - 多環境配置架構圖
- `docs/diagrams/ddd_architecture.mmd` - DDD 架構概覽圖
- `docs/diagrams/hexagonal_architecture.mmd` - 六角形架構圖
- `docs/diagrams/aws_infrastructure.mmd` - AWS 基礎設施圖
- `docs/diagrams/aws-infrastructure-detailed.mmd` - 詳細 AWS 架構圖
- `docs/diagrams/observability_architecture.mmd` - 可觀測性架構圖
- `docs/diagrams/event_driven_architecture.mmd` - 事件驅動架構圖

### Viewpoints 目錄文件 (9 個)
- `docs/diagrams/viewpoints/information/information-overview.mmd`
- `docs/diagrams/viewpoints/information/event-driven-architecture.mmd`
- `docs/diagrams/viewpoints/development/hexagonal-architecture.mmd`
- `docs/diagrams/viewpoints/development/ddd-layered-architecture.mmd`
- `docs/diagrams/viewpoints/concurrency/async-processing.mmd`
- `docs/diagrams/viewpoints/deployment/infrastructure-overview.mmd`
- `docs/diagrams/viewpoints/operational/monitoring-architecture.mmd`
- `docs/diagrams/viewpoints/functional/functional-overview.mmd`
- `docs/diagrams/viewpoints/functional/system-overview.mmd`

### 模板範例文件 (1 個)
- `docs/templates/examples/diagram-examples/system-overview.mmd`

## 📋 任務清單

### 階段 1: 分析和評估
- [ ] 檢查重複文件內容
  - [ ] 比較 `hexagonal_architecture.mmd` vs `viewpoints/development/hexagonal-architecture.mmd`
  - [ ] 比較 `event_driven_architecture.mmd` vs `viewpoints/information/event-driven-architecture.mmd`
- [ ] 確認 `viewpoints/functional/system-overview.mmd` 是否已完全轉換
- [ ] 評估每個文件的技術價值和內容品質

### 階段 2: 內容轉換和整合
- [ ] 轉換高價值文件為 .md 文檔
  - [ ] `aws-infrastructure-detailed.mmd` → 獨立文檔
  - [ ] `multi_environment.mmd` → 獨立文檔
  - [ ] `async-processing.mmd` → 整合到並發視角文檔
- [ ] 整合到現有文檔
  - [ ] 監控相關圖表 → `docs/viewpoints/operational/README.md`
  - [ ] 部署相關圖表 → `docs/viewpoints/deployment/README.md`
  - [ ] 資訊相關圖表 → `docs/viewpoints/information/README.md`

### 階段 3: 清理和驗證
- [ ] 刪除重複文件（保留最完整版本）
- [ ] 更新模板範例為新格式
- [ ] 更新相關文檔的導航連結
- [ ] 驗證所有新文檔符合標準

## 🔧 可用工具

- `scripts/process-orphaned-mmd-files.py` - 自動轉換腳本
- 需要創建的輔助腳本：
  - `scripts/check-duplicate-mmd.sh` - 重複文件檢查
  - `scripts/convert-high-value-mmd.sh` - 批量轉換腳本

## 📊 優先級

- **高**: 處理重複文件和已轉換內容
- **中**: 轉換高價值內容
- **低**: 清理和模板更新

## 📅 預估時程

- **階段 1**: 1 週（分析評估）
- **階段 2**: 2-3 週（轉換整合）
- **階段 3**: 1 週（清理驗證）

## 📚 相關資源

- [孤立文件詳細報告](reports-summaries/diagrams/orphaned-mmd-files-report.md)
- [Mermaid 遷移完成報告](reports-summaries/diagrams/mermaid-migration-complete-report.md)
- **圖表生成標準** (請參考專案內部文檔)

## 💡 備註

- 這是非緊急任務，不影響當前系統功能
- 在刪除任何文件前，確保內容已適當保存
- 可以分批處理，不需要一次性完成
- 建議與團隊討論哪些內容最有價值

## ✅ 完成標準

- 所有有價值的內容都已保存並整合到適當的文檔中
- 沒有重複或過時的文件
- 所有新文檔都符合圖表生成標準
- 文檔導航已更新以包含新內容
