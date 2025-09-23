
# 電子商務系統 UML 文檔說明

本目錄包含電子商務系統的各種 UML 圖表，用於描述系統的架構、設計和行為。

## 圖表列表

### 基礎圖表

1. **類別圖 (class-diagram.puml)**
   - 描述系統中的主要類別及其關係
   - 包括訂單、支付、定價和配送Aggregate Root，以及相關Entity、Value Object和Domain Service

2. **對象圖 (object-diagram.puml)**
   - 展示領域模型的實例關係
   - 包括訂單、訂單項目、支付和配送等具體對象實例

3. **組件圖 (component-diagram.puml)**
   - 展示系統的主要組件及其交互
   - 基於Hexagonal Architecture，展示Port和Adapter
   - 包括持久化Adapter和External SystemAdapter

4. **Deployment圖 (deployment-diagram.puml)**
   - 描述系統的Deployment架構
   - 包括服務器、數據庫、消息中間件和External System

5. **套件圖 (package-diagram.puml)**
   - 展示系統的套件結構和依賴關係
   - 按照Hexagonal Architecture劃分為接口層、Application Layer、Domain Layer和Infrastructure Layer
   - 包括定價和配送模組的套件結構

6. **時序圖 (sequence-diagram.puml)**
   - 描述訂單處理的主要流程
   - 包括創建訂單、處理支付和添加訂單項目的時序

7. **定價處理時序圖 (pricing-sequence-diagram.puml)**
   - 描述定價處理的主要流程
   - 包括創建定價規則、更新佣金費率、獲取產品類別的定價規則和計算佣金
   - !\1

8. **配送處理時序圖 (delivery-sequence-diagram.puml)**
   - 描述配送處理的主要流程
   - 包括創建配送、安排配送、分配配送Resource、更新配送地址、標記為已送達等操作
   - !\1

9. **狀態圖 (state-diagram.puml)**
   - 展示訂單在不同狀態之間的轉換
   - 包括子狀態和可能的回退路徑

10. **活動圖概覽 (activity-diagram-overview.puml)**
    - 高層次展示電子商務系統的主要業務流程
    - 包括Customer、訂單系統、支付系統和物流系統的交互

11. **活動圖詳細 (activity-diagram-detail.puml)**
    - 詳細展示訂單處理的具體步驟
    - 包括各層之間的交互和事件流

12. **使用案例圖 (use-case-diagram.puml)**
    - 描述系統的主要功能和Actor
    - 區分核心用例和擴展用例

### Design

13. **領域模型圖 (domain-model-diagram.puml)**
    - 詳細展示系統中的Aggregate Root、Entity、Value Object和Domain Service
    - 按照領域上下文組織
    - 包括訂單、支付、定價和配送Aggregate

14. **Hexagonal Architecture圖 (hexagonal-architecture-diagram.puml)**
    - 詳細展示系統的Port和Adapter模式
    - 包括驅動Adapter、Application Core和被驅動Adapter
    - 展示Application Layer映射器的作用

15. **DDDLayered Architecture圖 (ddd-layers-diagram.puml)**
    - 展示DDDLayered Architecture的依賴關係和數據流向
    - 詳細說明每一層的職責和組件
    - 特別強調數據轉換和映射器的作用

16. **Saga Pattern圖 (saga-pattern-diagram.puml)**
    - 展示分布式事務處理流程
    - 包括正常流程和補償事務

17. **限界上下文圖 (bounded-context-diagram.puml)**
    - 展示系統中不同上下文之間的關係
    - 包括Context Mapping模式

18. **Event Storming圖 (big-picture-exploration.puml, process-modeling.puml, design-level.puml)**
    - 展示系統中的Command、事件、Aggregate Root、Policy和Read Model
    - 基於Event Storming工作坊的結果
    - **Big Picture Exploration階段 (Big Picture Exploration)**：快速了解整個業務領域
    - **流程建模階段 (Process Modeling)**：深入理解事件之間的因果關係
    - **設計級別階段 (Design Level)**：為軟體實現提供詳細設計

### 進階架構圖表

19. **Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS))模式圖 (cqrs-pattern-diagram.puml)**
    - 展示Command和查詢責任分離模式
    - 包括Command端和查詢端的架構

20. **Event Sourcing圖 (event-sourcing-diagram.puml)**
    - 展示事件的存儲和重放機制
    - 包括如何從事件構建Read Model

21. **API接口圖 (api-interface-diagram.puml)**
    - 展示系統對外提供的API接口
    - 包括端點和數據結構

22. **數據模型圖 (data-model-diagram.puml)**
    - 展示系統的數據庫模型和關係
    - 包括表、列和關係

23. **安全架構圖 (security-architecture-diagram.puml)**
    - 展示系統的安全機制和認證授權流程
    - 包括安全控制和Monitoring

24. **Observability架構圖 (observability-diagram.puml)**
    - 展示系統的Monitoring、Logging和Observability架構
    - 包括Metrics、Logging、Tracing和告警

## 如何查看圖表

這些圖表使用 PlantUML 創建，可以通過以下方式查看：

1. **使用 PlantUML 在線服務**：
   - 訪問 <http://www.plantuml.com/plantuml/uml/>
   - 複製 .puml 文件內容並粘貼到編輯器中

2. **使用 PlantUML 本地渲染**：
   - 使用項目根目錄中的 plantuml.jar
   - 執行Command：`java -jar plantuml.jar docs/uml/圖表名稱.puml`

3. **使用 IDE 插件**：
   - IntelliJ IDEA、VS Code 等 IDE 都有 PlantUML 插件
   - 安裝插件後可直接在 IDE 中預覽

## Guidelines

更新這些 UML 圖表時，請遵循以下原則：

1. 保持圖表與實際代碼一致
2. 使用中文命名和註釋，提高可讀性
3. 適當使用顏色和分組，增強視覺效果
4. 添加必要的註釋，解釋複雜的關係或概念
5. 更新後在This document中記錄變更

## 最近更新

- 2023-03-23：初始版本創建
- 2024-05-10：更新所有圖表以反映最新系統架構
- 2024-05-10：添加新的Domain-Driven Design圖表
- 2024-05-10：添加進階架構圖表
- 2024-05-10：添加對象圖和活動圖
- 2024-06-08：更新類別圖、組件圖、領域模型圖、Hexagonal Architecture圖和套件圖
- 2024-06-08：添加定價處理時序圖、配送處理時序圖和DDDLayered Architecture圖

## 圖表預覽

要查看圖表，請使用上述方法之一渲染 .puml 文件。以下是一些示例圖表的預覽：

### 類別圖

![類別圖](../../../diagrams/plantuml/class-diagram.svg)

### 對象圖

![對象圖](../../../diagrams/plantuml/object-diagram.svg)

### 組件圖

!!!!!![組件圖](../../../diagrams/generated/legacy/.png)

### Deployment

![Deployment圖](../../../diagrams/plantuml/deployment-diagram.svg)

### 套件圖

!!!!!![套件圖](../../../diagrams/generated/legacy/.png)

### 時序圖

!!!!!![時序圖](../../../diagrams/generated/legacy/.png)

### 定價處理時序圖

!!!!!![定價處理時序圖](../../../diagrams/generated/legacy/.png)

### 配送處理時序圖

!!!!!![配送處理時序圖](../../../diagrams/generated/legacy/.png)

### 狀態圖

!!!!!![狀態圖](../../../diagrams/generated/legacy/.png)

### Overview

!!!!!![活動圖概覽](../../../diagrams/generated/legacy/.png)

### 活動圖詳細

!!!!!![活動圖詳細](../../../diagrams/generated/legacy/.png)

### 使用案例圖

!!!!!![使用案例圖](../../../diagrams/generated/legacy/.png)

### 領域模型圖

!!!!!![領域模型圖](../../../diagrams/generated/legacy/.png)

### Hexagonal Architecture圖

!\1

### DDDLayered Architecture圖

!\1

### Saga Pattern圖

!\1

### 限界上下文圖

!!!!!![限界上下文圖](../../../diagrams/generated/legacy/.png)

### Event Storming圖-Big Picture Exploration

!!!!!![Event Storming圖- Big Picture Exploration](../../../diagrams/generated/legacy/big-picture-exploration.png)

### Event Storming圖-Process Modeling

!!!!!![Event Storming圖- Process Modeling](../../../diagrams/generated/legacy/process-modeling.png)

### Event Storming圖-Design Level

!!!!!![Event Storming圖- design-level](../../../diagrams/generated/legacy/design-level.png)

### Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS))模式圖

!!!!!![Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS))模式圖](../../../diagrams/generated/legacy/.png)

### Event Sourcing圖

!!!!!![Event Sourcing圖](../../../diagrams/generated/legacy/.png)

### API接口圖

!!!!!![API接口圖](../../../diagrams/generated/legacy/.png)

### 數據模型圖

!!!!!![數據模型圖](../../../diagrams/generated/legacy/.png)

### 安全架構圖

!!!!!![安全架構圖](../../../diagrams/generated/legacy/.png)

### Observability架構圖

![Observability架構圖](../../../diagrams/plantuml/observability-diagram.svg)
