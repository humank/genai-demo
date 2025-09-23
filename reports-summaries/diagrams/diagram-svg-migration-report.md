# 圖表格式遷移報告：PNG → SVG

## 概述

本次遷移將所有圖表生成從 PNG 格式改為 SVG 格式，以提供更高的解析度和更好的顯示品質。同時移除了所有 GitHub Actions 相關的配置和要求。

## 主要變更

### 1. 刪除的 PNG 檔案

已刪除專案中所有 PNG 檔案（除了 node_modules 目錄）：
- `docs/diagrams/` 目錄下的所有 PNG 檔案
- `docs/diagrams/plantuml/` 目錄下的所有 PNG 檔案
- `docs/diagrams/legacy/` 目錄下的所有 PNG 檔案
- `docs/diagrams/viewpoints/` 目錄下的所有 PNG 檔案
- 根目錄下的測試 PNG 檔案

### 2. 腳本修改

#### 2.1 主要圖表生成腳本
- **`scripts/generate-all-diagrams.sh`**
  - PlantUML: `-tpng` → `-tsvg`
  - Mermaid: `*.png` → `*.svg`
  - 移除 PNG 檔案計數和清理邏輯

#### 2.2 Excalidraw 轉換腳本
- **`scripts/excalidraw-to-png.js`** → **`scripts/excalidraw-to-svg.js`**
  - 重新命名檔案以反映新功能
  - 修改輸出格式從 PNG 到 SVG
  - 更新所有相關函數名稱和變數

#### 2.3 其他腳本更新
- `scripts/generate-diagram-images.sh`
- `scripts/generate-standardized-diagrams.sh`
- `scripts/generate-diagrams.sh`
- `scripts/check-hook-status.py`
- `scripts/test-diagram-automation.py`
- `scripts/diagram-automation-manager.py`
- `scripts/unified-diagram-sync.sh`

### 3. Kiro Hooks 修改

#### 3.1 diagram-documentation-sync.kiro.hook
- 移除 `**/*.png` 從排除列表
- 保留 `**/*.svg` 在排除列表中

#### 3.2 md-docs-translation.kiro.hook
- 移除 `**/*.png` 從排除列表
- 保留 `**/*.svg` 在排除列表中

### 4. Steering 文檔更新

#### 4.1 translation-guide.md
- 更新圖片範例從 PNG 到 SVG
- 修改中英文版本的圖片路徑範例

### 5. 規格文檔清理

#### 5.1 design.md 中移除的 GitHub Actions 內容
- **技術亮點部分**：移除 "完整 CI/CD: GitHub Actions + AWS CDK"
- **自動化工作流程**：完全移除 GitHub Actions 工作流程配置
- **圖表工具比較表**：移除 "GitHub 顯示" 欄位，改為 "輸出格式"
- **配置檔案**：更新所有 `convert_to_png` 為 `convert_to_svg`
- **腳本範例**：更新 PlantUML 參數從 `-tpng` 到 `-tsvg`

#### 5.2 模板檔案更新
- `docs/templates/metadata-standards.md`
- `docs/templates/diagram-metadata-standards.md`
- `docs/templates/examples/diagram-examples/`
- 所有 `generated_files` 欄位從 `["*.png", "*.svg"]` 改為 `["*.svg"]`

### 6. 文檔引用更新

#### 6.1 英文文檔
- `docs/en/architecture/hexagonal-architecture.md`
- `docs/en/translation-system-guide.md`
- `docs/en/.kiro/steering/translation-guide.md`

## 技術優勢

### SVG 格式優勢
1. **向量圖形**：無限縮放不失真
2. **檔案大小**：通常比 PNG 更小
3. **可編輯性**：可以用文字編輯器修改
4. **網頁友好**：現代瀏覽器原生支援
5. **列印品質**：高解析度列印輸出

### 移除 GitHub Actions 的好處
1. **簡化部署**：減少對特定 CI/CD 平台的依賴
2. **本地開發**：所有圖表生成都可在本地執行
3. **靈活性**：可以選擇任何 CI/CD 解決方案
4. **維護性**：減少外部依賴和配置複雜度

## 後續建議

1. **測試圖表生成**：執行 `scripts/generate-all-diagrams.sh` 確保所有圖表正常生成
2. **檢查文檔顯示**：確認 SVG 圖表在文檔中正常顯示
3. **更新 README**：如有需要，更新專案 README 中的圖表生成說明
4. **團隊通知**：通知團隊成員關於格式變更和新的生成腳本

## 驗證清單

- [ ] 所有 PNG 檔案已刪除
- [ ] 腳本生成 SVG 格式圖表
- [ ] Kiro hooks 正確排除 SVG 檔案
- [ ] 文檔模板使用 SVG 格式
- [ ] GitHub Actions 配置已完全移除
- [ ] 英文文檔引用已更新

## 執行命令

```bash
# 生成所有 SVG 圖表
./scripts/generate-all-diagrams.sh

# 檢查生成的檔案
find docs/diagrams -name "*.svg" | wc -l

# 確認沒有 PNG 檔案殘留
find . -name "*.png" -not -path "./node_modules/*" | wc -l
```

此遷移確保了更高品質的圖表輸出，同時簡化了專案的 CI/CD 需求。