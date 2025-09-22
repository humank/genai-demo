
# Excalidraw 概念圖集合

This document記錄了使用 Excalidraw MCP 生成的四個核心概念圖，這些圖表為系統架構提供了高層次的視覺化概覽。

## 概念圖列表

### 1. Bounded Context概念圖 (Bounded Contexts Concept Map)

**目的**: 展示系統中的主要Bounded Context及其關係

**內容**:
- **核心業務上下文**: Customer (身份與檔案), Order (交易), Product (目錄), Payment (金融)
- **支援上下文**: Inventory (庫存), Delivery (物流), Promotion (行銷), Notification (通訊)
- **關係箭頭**: 顯示上下文之間的依賴關係和資料流

**特色**:
- 使用不同顏色區分不同類型的上下文
- 清楚標示核心業務流程: Customer → Order → Product → Payment
- 展示支援服務如何與核心業務互動

### 2. User旅程概念圖 (User Journey Concept Map)

**目的**: 視覺化User與系統互動的完整流程

**內容**:
- **起點**: Customer (橢圓形，代表User)
- **關鍵步驟**: Browse → Select → Order → Payment → Delivery → Complete
- **觸點**: 註冊、產品目錄、購物車、結帳、付款閘道、訂單Tracing、配送確認

**特色**:
- 使用流程箭頭顯示User旅程的順序
- 不同顏色代表不同階段的重要性
- 清楚標示每個階段的關鍵觸點

### 3. 系統架構概念圖 (System Architecture Concept)

**目的**: 展示Hexagonal Architecture的高層次結構

**內容**:
- **展示層** (Presentation Layer): REST APIs, Web UI
- **Application Layer** (Application Layer): 用例實現, 服務協調
- **Domain Layer** (Domain Layer): 業務邏輯, Entity
- **Infrastructure Layer** (Infrastructure): Repository, 訊息佇列, 檔案儲存
- **External System** (External Systems): 付款閘道, 電子郵件服務, 第三方 APIs

**特色**:
- 清楚展示Hexagonal Architecture的分層結構
- 使用箭頭顯示層之間的依賴關係
- 區分內部架構和External System

### 4. Stakeholder對應圖 (Stakeholder Mapping)

**目的**: 識別和視覺化所有系統Stakeholder及其Concern

**內容**:
- **主要Stakeholder**: End Users (功能性), Business (需求), Developers (實現), Operations (Deployment)
- **次要Stakeholder**: Architects (設計), QA Team (品質), Security (合規)
- **中央系統**: 所有Stakeholder都與系統相關聯

**特色**:
- 使用橢圓形代表Stakeholder，強調其重要性
- 連接線顯示每個Stakeholder與系統的關係
- 不同顏色區分不同類型的Stakeholder

## 技術實現

### 使用的 Excalidraw MCP 功能

1. **批次元素創建** (`mcp_excalidraw_batch_create_elements`)
   - 一次創建多個相關元素
   - 提高創建效率
   - 確保元素之間的一致性

2. **單一元素創建** (`mcp_excalidraw_create_element`)
   - 創建特定的元素（箭頭、文字等）
   - 精確控制元素屬性
   - 支援動態調整

3. **元素查詢** (`mcp_excalidraw_query_elements`)
   - 檢查畫布上的現有元素
   - 確保不重複創建
   - 支援元素管理

### Design

1. **顏色一致性**
   - 每種類型的概念使用一致的顏色方案
   - 藍色系：核心業務功能
   - 橙色系：User互動
   - 綠色系：技術實現
   - 紫色系：管理和控制

2. **形狀語義**
   - 矩形：系統組件、流程步驟
   - 橢圓：User、Stakeholder
   - 箭頭：流程方向、依賴關係
   - 線條：關聯關係

3. **佈局邏輯**
   - 從左到右：時間順序或流程順序
   - 從上到下：層次結構或重要性
   - 中心輻射：核心概念與相關元素

## Guidelines

### 如何閱讀這些概念圖

1. **Bounded Context圖**: 理解系統的業務領域劃分
2. **User旅程圖**: 了解User如何與系統互動
3. **系統架構圖**: 掌握技術架構的整體結構
4. **Stakeholder圖**: 識別所有相關的Stakeholder

### Maintenance

- 當系統需求變更時，更新相應的概念圖
- 定期檢查概念圖與實際實現的一致性
- 使用版本控制Tracing概念圖的變更歷史

## 相關文檔

- [Functional Viewpoint文檔](../../viewpoints/functional/)
- [領域模型設計](../../../viewpoints/functional/domain-model.md)
- \1
- \1

---

*This document由 Excalidraw MCP 自動生成的概念圖支援，提供系統架構的高層次視覺化概覽。*