
# Standards

本目錄包含基於 Event Storming 官方標準的三階段圖表，使用標準化配色和符號規範。

## Overview

### 1. Big Picture Event Storming (Big Picture Exploration階段)

**檔案**: `big-picture-standardized.puml`

**目的**: 識別核心業務事件、發現問題Hotspot、找出改進機會

!\1

**標準配色**:
- 🟠 **事件** (#FFA500): 系統中發生的重要業務事件
- 🟡 **Actor** (#FFD700): 觸發事件的人或角色
- 🩷 **External System** (#FF69B4): 與系統整合的外部服務
- 🔴 **Hotspot問題** (#FF0000): 需要解決的問題或風險點
- 🟢 **機會點** (#32CD32): 系統改進和優化的機會

**重點內容**:
- 完整的業務流程事件鏈
- 跨系統的整合點
- 業務風險和改進機會
- Stakeholder互動

### 2. Process Level Event Storming (流程建模階段)

**檔案**: `process-level-standardized.puml`

**目的**: 詳細的Command和事件流程、Aggregate邊界和職責、業務Policy和規則

!\1

**標準配色**:
- 🟠 **事件** (#FFA500): 業務狀態變化的結果
- 🔵 **Command** (#1E90FF): 觸發業務行為的意圖
- 🟡 **Aggregate** (#FFFF00): 維護業務規則的Entity
- 🟢 **Read Model** (#32CD32): 用戶查詢的資訊視圖
- 🟣 **Policy** (#800080): 事件觸發的業務規則
- 🟡 **Actor** (#FFD700): 執行Command的人或角色
- 🩷 **External System** (#FF69B4): 整合的外部服務

**重點內容**:
- Command到Aggregate的執行流程
- 事件驅動的業務規則
- Read Model的投影Policy
- External System整合點

### Design

**檔案**: `design-level-standardized.puml`

!\1

**目的**: 完整的Bounded Context設計、Aggregate邊界和職責劃分、跨上下文的事件整合

**標準配色**:
- 🟠 **事件** (#FFA500): 業務狀態變化的結果
- 🔵 **Command** (#1E90FF): 觸發業務行為的意圖
- 🟡 **Aggregate** (#FFFF00): 維護業務規則和一致性的Entity
- 🟢 **Read Model** (#32CD32): 用戶查詢的資訊視圖
- 🟣 **Policy** (#800080): 事件觸發的業務規則
- 🔷 **服務** (#ADD8E6): 協調Aggregate和處理複雜業務邏輯
- 🟡 **Actor** (#FFD700): 執行Command的人或角色
- 🩷 **External System** (#FF69B4): 整合的外部服務

**Bounded Context**:
- 📦 **訂單上下文** (Order Context)
- 📦 **支付上下文** (Payment Context)
- 📦 **庫存上下文** (Inventory Context)
- 📦 **配送上下文** (Delivery Context)
- 📦 **通知上下文** (Notification Context)
- 📦 **Customer服務上下文** (Customer Service Context)
- 📦 **整合視圖上下文** (Integration View Context)

**重點內容**:
- 完整的Bounded Context邊界
- 跨上下文的事件整合
- 服務和External System整合
- Read Model投影Policy

## 連線類型說明

| 連線類型 | 顏色 | 說明 |
|---------|------|------|
| 實線箭頭 | 黑色 | Command執行或事件發布 |
| 粗實線 | 紅色 | 跨Bounded Context的事件整合 |
| 虛線 | 綠色 | 事件到Read Model的投影 |
| 粗虛線 | 紫色 | 跨Bounded Context的Read Model投影 |
| 實線 | 粉色 | External System整合 |
| 虛線箭頭 | 紅色 | 潛在問題或風險點 |

## Guidelines

### Standards

其他 Event Storming 圖表可以引用標準配色定義：

```plantuml
!include event-storming-colors.puml
```

### 2. 圖表生成

使用 PlantUML 生成 PNG 圖片：

```bash
java -jar tools-and-environment/plantuml.jar -tpng ../diagrams/plantuml/event-storming/*.puml
```

### 3. 圖表更新

當業務流程變更時，按以下順序更新：

1. **Big Picture**: 更新核心業務事件和Hotspot問題
2. **Process Level**: 更新Command、Aggregate和Policy
3. **Design Level**: 更新Bounded Context和服務設計

## Best Practices

### Big Picture 階段
- 專注於業務事件，不要過早考慮技術實現
- 識別所有Stakeholder和External System
- 標記問題Hotspot和改進機會
- 保持高層次視角，避免過多細節

### Process Level 階段
- 明確Command和事件的因果關係
- 定義Aggregate邊界和職責
- 識別業務Policy和規則
- 設計Read Model以支援查詢需求

### Design Level 階段
- 劃分清晰的Bounded Context
- 設計跨上下文的事件整合
- 定義服務職責和External System整合
- 考慮非功能性需求（Performance、Security等）

## 相關文檔

- \1
- [DDD 戰術模式實現](../../../design/ddd-guide.md)
- \1
- **Domain EventImplementation Guide** (請參考專案內部文檔)

## Tools

- **PlantUML**: 圖表生成工具
- **Event Storming 官方網站**: https://www.eventstorming.com/
- **DDD 社群Resource**: https://github.com/ddd-crew
- **PlantUML 語法參考**: https://plantuml.com/