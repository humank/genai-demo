# Event Storming 標準化圖表

本目錄包含基於 Event Storming 官方標準的三階段圖表，使用標準化配色和符號規範。

## 圖表概覽

### 1. Big Picture Event Storming (大圖探索階段)

**檔案**: `big-picture-standardized.puml`

**目的**: 識別核心業務事件、發現問題熱點、找出改進機會

![Big Picture Event Storming](big-picture-standardized.svg)

**標準配色**:
- 🟠 **事件** (#FFA500): 系統中發生的重要業務事件
- 🟡 **參與者** (#FFD700): 觸發事件的人或角色
- 🩷 **外部系統** (#FF69B4): 與系統整合的外部服務
- 🔴 **熱點問題** (#FF0000): 需要解決的問題或風險點
- 🟢 **機會點** (#32CD32): 系統改進和優化的機會

**重點內容**:
- 完整的業務流程事件鏈
- 跨系統的整合點
- 業務風險和改進機會
- 利害關係人互動

### 2. Process Level Event Storming (流程建模階段)

**檔案**: `process-level-standardized.puml`

**目的**: 詳細的命令和事件流程、聚合邊界和職責、業務策略和規則

![Process Level Event Storming](process-level-standardized.svg)

**標準配色**:
- 🟠 **事件** (#FFA500): 業務狀態變化的結果
- 🔵 **命令** (#1E90FF): 觸發業務行為的意圖
- 🟡 **聚合** (#FFFF00): 維護業務規則的實體
- 🟢 **讀模型** (#32CD32): 用戶查詢的資訊視圖
- 🟣 **策略** (#800080): 事件觸發的業務規則
- 🟡 **參與者** (#FFD700): 執行命令的人或角色
- 🩷 **外部系統** (#FF69B4): 整合的外部服務

**重點內容**:
- 命令到聚合的執行流程
- 事件驅動的業務規則
- 讀模型的投影策略
- 外部系統整合點

### 3. Design Level Event Storming (設計級別階段)

**檔案**: `design-level-standardized.puml`

![Design Level Event Storming](design-level-standardized.svg)

**目的**: 完整的界限上下文設計、聚合邊界和職責劃分、跨上下文的事件整合

**標準配色**:
- 🟠 **事件** (#FFA500): 業務狀態變化的結果
- 🔵 **命令** (#1E90FF): 觸發業務行為的意圖
- 🟡 **聚合** (#FFFF00): 維護業務規則和一致性的實體
- 🟢 **讀模型** (#32CD32): 用戶查詢的資訊視圖
- 🟣 **策略** (#800080): 事件觸發的業務規則
- 🔷 **服務** (#ADD8E6): 協調聚合和處理複雜業務邏輯
- 🟡 **參與者** (#FFD700): 執行命令的人或角色
- 🩷 **外部系統** (#FF69B4): 整合的外部服務

**界限上下文**:
- 📦 **訂單上下文** (Order Context)
- 📦 **支付上下文** (Payment Context)
- 📦 **庫存上下文** (Inventory Context)
- 📦 **配送上下文** (Delivery Context)
- 📦 **通知上下文** (Notification Context)
- 📦 **客戶服務上下文** (Customer Service Context)
- 📦 **整合視圖上下文** (Integration View Context)

**重點內容**:
- 完整的界限上下文邊界
- 跨上下文的事件整合
- 服務和外部系統整合
- 讀模型投影策略

## 連線類型說明

| 連線類型 | 顏色 | 說明 |
|---------|------|------|
| 實線箭頭 | 黑色 | 命令執行或事件發布 |
| 粗實線 | 紅色 | 跨界限上下文的事件整合 |
| 虛線 | 綠色 | 事件到讀模型的投影 |
| 粗虛線 | 紫色 | 跨界限上下文的讀模型投影 |
| 實線 | 粉色 | 外部系統整合 |
| 虛線箭頭 | 紅色 | 潛在問題或風險點 |

## 使用指南

### 1. 引用標準配色

其他 Event Storming 圖表可以引用標準配色定義：

```plantuml
!include event-storming-colors.puml
```

### 2. 圖表生成

使用 PlantUML 生成 PNG 圖片：

```bash
java -jar tools/plantuml.jar -tpng docs/diagrams/plantuml/event-storming/*.puml
```

### 3. 圖表更新

當業務流程變更時，按以下順序更新：

1. **Big Picture**: 更新核心業務事件和熱點問題
2. **Process Level**: 更新命令、聚合和策略
3. **Design Level**: 更新界限上下文和服務設計

## Event Storming 最佳實踐

### Big Picture 階段
- 專注於業務事件，不要過早考慮技術實現
- 識別所有利害關係人和外部系統
- 標記問題熱點和改進機會
- 保持高層次視角，避免過多細節

### Process Level 階段
- 明確命令和事件的因果關係
- 定義聚合邊界和職責
- 識別業務策略和規則
- 設計讀模型以支援查詢需求

### Design Level 階段
- 劃分清晰的界限上下文
- 設計跨上下文的事件整合
- 定義服務職責和外部系統整合
- 考慮非功能性需求（性能、安全性等）

## 相關文檔

- [Event Storming 方法論指南](../../../architecture/event-storming-methodology.md)
- [DDD 戰術模式實現](../../../design/ddd-guide.md)
- [界限上下文設計](../../../architecture/bounded-context-design.md)
- [領域事件實現指南](../../../design/domain-events.md)

## 工具和資源

- **PlantUML**: 圖表生成工具
- **Event Storming 官方網站**: https://www.eventstorming.com/
- **DDD 社群資源**: https://github.com/ddd-crew
- **PlantUML 語法參考**: https://plantuml.com/