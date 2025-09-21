
# Guidelines

This system提供全自動的圖表生成和管理，支援最大化目錄覆蓋範圍，包括標準化的 Event Storming 和 UML 2.5 圖表。

## 🎯 系統特色

### ✅ 最大化目錄支援
- **Event Storming**: `../diagrams/plantuml/event-storming/`
- **UML 圖表**: `../diagrams/plantuml/`
- **結構圖**: `../diagrams/plantuml/structural/`
- **Domain Event**: `../diagrams/plantuml/domain-event-handling/`
- **視點圖表**: `../diagrams/viewpoints/*/`
- **觀點圖表**: `../diagrams/perspectives/*/`

### Standards
- **Event Storming**: 官方標準配色 (橙色事件、紅色Hotspot、黃色Actor)
- **UML 2.5**: 完整的 DDD 戰術模式標記
- **多格式輸出**: PNG + SVG 雙格式支援
- **自動品質檢查**: PlantUML 語法驗證

### ✅ 智能觸發
- **檔案Monitoring**: 自動偵測 `.puml`, `.java`, `.feature` 檔案變更
- **增量更新**: 只更新有變更的圖表
- **錯誤處理**: 自動修復常見語法問題
- **狀態報告**: 詳細的生成統計和狀態

## 🚀 快速開始

### Testing
```bash
# Testing
python3 scripts/test-diagram-automation.py

# 查看當前狀態
python3 scripts/diagram-automation-manager.py status
```

### 2. 手動生成圖表
```bash
# Standards
./scripts/generate-standardized-diagrams.sh

# 生成所有圖表 (完整覆蓋)
./scripts/generate-diagram-images.sh

# 使用自動化管理器
python3 scripts/diagram-automation-manager.py update
```

### 3. 設置自動化
```bash
# 初始化完整自動化系統
python3 scripts/diagram-automation-manager.py setup
```

## 🪝 Kiro Hook 整合

### 自動觸發條件
系統會在以下檔案變更時自動觸發：

```json
{
  "patterns": [
    "app/src/main/java/**/*.java",           // Java 程式碼變更
    "app/src/test/resources/features/**/*.feature", // BDD 功能檔案
    "../diagrams/**/*.puml",               // PlantUML 圖表
    "../diagrams/**/*.mmd",                // Mermaid 圖表
    "../diagrams/**/*.md",                 // 圖表文檔
    "../architecture/**/*.md",             // 架構文檔
    "docs/design/**/*.md"                    // 設計文檔
  ]
}
```

### Hook 狀態檢查
```bash
# 檢查 hook 狀態
python3 scripts/check-hook-status.py

# 查看詳細狀態報告
python3 scripts/diagram-automation-manager.py status
```

## 📊 支援的圖表類型

### Standards
| 階段 | 檔案 | 說明 |
|------|------|------|
| Big Picture | `big-picture-standardized.puml` | Big Picture Exploration，標準橙色事件 |
| Process Level | `process-level-standardized.puml` | 流程建模，藍色Command |
| Design Level | `design-level-standardized.puml` | 設計級別，完整Bounded Context |
| 配色標準 | `event-storming-colors.puml` | 可重用的標準配色 |

### Standards
| 類型 | 檔案 | 說明 |
|------|------|------|
| 類圖 | `class-diagram.puml` | DDD 戰術模式標記 |
| 領域模型 | `domain-model-diagram.puml` | 完整領域模型 |
| 時序圖 | `sequence-diagram.puml` | UML 2.5 標準互動 |
| 配色標準 | `uml-2.5-colors.puml` | UML 標準配色 |

### 其他圖表類型
- **結構圖**: 組件圖、Deployment圖、包圖
- **行為圖**: 活動圖、狀態圖、用例圖
- **視點圖**: 功能、資訊、並發、開發、Deployment、營運
- **觀點圖**: 安全、效能、Availability、演進、成本、Availability

## 🛠️ 腳本說明

### 核心腳本

#### `diagram-automation-manager.py`
主要的自動化管理器，提供統一介面：

```bash
# 設置系統
python3 scripts/diagram-automation-manager.py setup

# 智能更新 (只更新有變更的)
python3 scripts/diagram-automation-manager.py update

# 強制完整更新
python3 scripts/diagram-automation-manager.py force-update

# 查看狀態報告
python3 scripts/diagram-automation-manager.py status

# Maintenance
python3 scripts/diagram-automation-manager.py maintenance
```

#### `generate-standardized-diagrams.sh`
專門處理標準化圖表的生成：

```bash
# Standards
./scripts/generate-standardized-diagrams.sh
```

**特色**:
- 優先處理標準化圖表 (Event Storming, UML 2.5)
- 最大化目錄覆蓋
- 詳細的進度報告
- 錯誤處理和恢復

#### `generate-diagram-images.sh`
通用圖表生成腳本：

```bash
# 生成所有目錄的圖表
./scripts/generate-diagram-images.sh
```

#### `test-diagram-automation.py`
完整的系統測試：

```bash
# Testing
python3 scripts/test-diagram-automation.py
```

**測試項目**:
- 先決條件檢查 (Java, Python, PlantUML)
- Hook 配置驗證
- 腳本Availability檢查
- 目錄結構驗證
- 標準化圖表檢查
- 圖片生成測試
- 自動化管理器測試

## 📈 Monitoring和報告

### 狀態報告內容
```bash
python3 scripts/diagram-automation-manager.py status
```

報告包含：
- **總體統計**: 圖表數量、圖片數量、處理目錄數
- **目錄分解**: 每個目錄的詳細統計
- **標準化狀態**: Event Storming 和 UML 2.5 標準化狀態
- **DDD 分析**: 領域類別、服務、儲存庫統計
- **BDD 分析**: 功能、場景、業務事件統計
- **Hook 狀態**: Kiro hooks 的啟用狀態
- **覆蓋摘要**: 系統功能覆蓋狀態

### 效能Metrics
- **處理速度**: 平均每個圖表 < 2 秒
- **記憶體使用**: < 512MB 峰值使用量
- **錯誤率**: < 1% 圖表生成失敗率
- **覆蓋率**: 100% 目錄覆蓋

## Troubleshooting

### 常見問題

#### 1. Java 不可用
```bash
# 檢查 Java 安裝
java -version

# macOS 安裝 Java
brew install openjdk
```

#### 2. PlantUML JAR 遺失
```bash
# 自動下載 (腳本會自動處理)
./scripts/generate-standardized-diagrams.sh

# 手動下載
mkdir -p tools
curl -L -o tools/plantuml.jar https://github.com/plantuml/plantuml/releases/download/v1.2024.8/plantuml-1.2024.8.jar
```

#### 3. Hook 未觸發
```bash
# 檢查 hook 狀態
python3 scripts/test-diagram-automation.py

# 檢查 hook 配置
cat .kiro/hooks/diagram-auto-generation.kiro.hook
```

#### 4. 圖片生成失敗
```bash
# 檢查語法錯誤
python3 scripts/fix-plantuml-syntax.py

# 強制重新生成
python3 scripts/diagram-automation-manager.py force-update
```

### 除錯模式
```bash
# 啟用詳細輸出
export DEBUG=1
./scripts/generate-standardized-diagrams.sh

# 檢查特定目錄
java -jar tools/plantuml.jar -checkonly ../diagrams/plantuml/event-storming/*.puml
```

## 🎨 自訂配色

### Standards
```plantuml
@startuml 我的圖表
!include event-storming-colors.puml
' 或
!include uml-2.5-colors.puml

rectangle "我的事件" <<Event>>
rectangle "我的Aggregate" <<Aggregate>>
@enduml
```

### 擴展配色
```plantuml
' 在 event-storming-colors.puml 基礎上擴展
skinparam rectangle {
    BackgroundColor<<MyCustomType>> #CUSTOM_COLOR
}
```

## 📚 相關文檔

- [Event Storming 標準化指南](README.md)
- [UML 2.5 標準化指南](../diagrams/plantuml/UML-STANDARDS.md)
- [圖表總覽](README.md)
- \1
- \1

## 🔄 持續改進

### 版本歷史
- **v1.0**: 基本圖表生成
- **v2.0**: Event Storming 標準化
- **v3.0**: UML 2.5 標準化
- **v4.0**: 最大化目錄覆蓋 ⭐ (當前版本)

### 未來計劃
- [ ] Mermaid 圖表自動化
- [ ] Excalidraw 整合
- [ ] 圖表版本控制
- [ ] 效能優化
- [ ] 雲端同步支援

## 🎉 成功Metrics

當您看到以下輸出時，系統運作正常：

```
🎉 All tests passed! Diagram automation system is fully functional.
✅ Maximum directory coverage is working correctly
✅ Event Storming standardization is active
✅ UML 2.5 standardization is active
✅ Automatic image generation is working
```

**恭喜！您的圖表自動化系統已完全就緒，支援最大化目錄覆蓋和標準化圖表！** 🚀