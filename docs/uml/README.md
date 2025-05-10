# 訂單系統 UML 文檔說明

本目錄包含訂單系統的各種 UML 圖表，用於描述系統的架構、設計和行為。

## 圖表列表

### 基礎圖表

1. **類別圖 (class-diagram.puml)**
   - 描述系統中的主要類別及其關係
   - 包括訂單聚合根、實體、值對象和領域服務

2. **對象圖 (object-diagram.puml)**
   - 展示領域模型的實例關係
   - 包括訂單、訂單項目、支付和配送等具體對象實例

3. **組件圖 (component-diagram.puml)**
   - 展示系統的主要組件及其交互
   - 基於六角形架構，展示端口和適配器

4. **部署圖 (deployment-diagram.puml)**
   - 描述系統的部署架構
   - 包括服務器、數據庫、消息中間件和外部系統

5. **套件圖 (package-diagram.puml)**
   - 展示系統的套件結構和依賴關係
   - 按照六角形架構劃分為接口層、應用層、領域層和基礎設施層

6. **時序圖 (sequence-diagram.puml)**
   - 描述訂單處理的主要流程
   - 包括創建訂單、處理支付和添加訂單項目的時序

7. **狀態圖 (state-diagram.puml)**
   - 展示訂單在不同狀態之間的轉換
   - 包括子狀態和可能的回退路徑

8. **活動圖概覽 (activity-diagram-overview.puml)**
   - 高層次展示訂單系統的主要業務流程
   - 包括客戶、訂單系統、支付系統和物流系統的交互

9. **活動圖詳細 (activity-diagram-detail.puml)**
   - 詳細展示訂單處理的具體步驟
   - 包括各層之間的交互和事件流

10. **使用案例圖 (use-case-diagram.puml)**
    - 描述系統的主要功能和參與者
    - 區分核心用例和擴展用例

### 領域驅動設計圖表

11. **領域模型圖 (domain-model-diagram.puml)**

    - 詳細展示系統中的聚合根、實體、值對象和領域服務
    - 按照領域上下文組織

12. **六角形架構圖 (hexagonal-architecture-diagram.puml)**

    - 詳細展示系統的端口和適配器模式
    - 包括驅動適配器、應用核心和被驅動適配器

13. **Saga模式圖 (saga-pattern-diagram.puml)**

    - 展示分布式事務處理流程
    - 包括正常流程和補償事務

14. **限界上下文圖 (bounded-context-diagram.puml)**

    - 展示系統中不同上下文之間的關係
    - 包括上下文映射模式

15. **事件風暴圖 (event-storming-diagram.puml)**

    - 展示系統中的命令、事件、聚合根、策略和讀模型
    - 基於事件風暴工作坊的結果

### 進階架構圖表

16. **CQRS模式圖 (cqrs-pattern-diagram.puml)**

    - 展示命令和查詢責任分離模式
    - 包括命令端和查詢端的架構

17. **事件溯源圖 (event-sourcing-diagram.puml)**

    - 展示事件的存儲和重放機制
    - 包括如何從事件構建讀模型

18. **API接口圖 (api-interface-diagram.puml)**

    - 展示系統對外提供的API接口
    - 包括端點和數據結構

19. **數據模型圖 (data-model-diagram.puml)**

    - 展示系統的數據庫模型和關係
    - 包括表、列和關係

20. **安全架構圖 (security-architecture-diagram.puml)**

    - 展示系統的安全機制和認證授權流程
    - 包括安全控制和監控

21. **可觀測性架構圖 (observability-diagram.puml)**

    - 展示系統的監控、日誌和可觀測性架構
    - 包括指標、日誌、追蹤和告警

## 如何查看圖表

這些圖表使用 PlantUML 創建，可以通過以下方式查看：

1. **使用 PlantUML 在線服務**：
   - 訪問 <http://www.plantuml.com/plantuml/uml/>
   - 複製 .puml 文件內容並粘貼到編輯器中

2. **使用 PlantUML 本地渲染**：
   - 使用項目根目錄中的 plantuml.jar
   - 執行命令：`java -jar plantuml.jar docs/uml/圖表名稱.puml`

3. **使用 IDE 插件**：
   - IntelliJ IDEA、VS Code 等 IDE 都有 PlantUML 插件
   - 安裝插件後可直接在 IDE 中預覽

## 圖表更新指南

更新這些 UML 圖表時，請遵循以下原則：

1. 保持圖表與實際代碼一致
2. 使用中文命名和註釋，提高可讀性
3. 適當使用顏色和分組，增強視覺效果
4. 添加必要的註釋，解釋複雜的關係或概念
5. 更新後在本文檔中記錄變更

## 最近更新

- 2023-03-23：初始版本創建
- 2024-05-10：更新所有圖表以反映最新系統架構
- 2024-05-10：添加新的領域驅動設計圖表
- 2024-05-10：添加進階架構圖表
- 2024-05-10：添加對象圖和活動圖

## 圖表預覽

要查看圖表，請使用上述方法之一渲染 .puml 文件。以下是一些示例圖表的預覽：

### 類別圖

![類別圖](./class-diagram.svg)

### 對象圖

![對象圖](./object-diagram.svg)

### 組件圖

![組件圖](./component-diagram.svg)

### 部署圖

![部署圖](./deployment-diagram.svg)

### 套件圖

![套件圖](./package-diagram.svg)

### 時序圖

![時序圖](./sequence-diagram.svg)

### 狀態圖

![狀態圖](./state-diagram.svg)

### 活動圖概覽

![活動圖概覽](./activity-diagram-overview.svg)

### 活動圖詳細

![活動圖詳細](./activity-diagram-detail.svg)

### 使用案例圖

![使用案例圖](./use-case-diagram.svg)

### 領域模型圖

![領域模型圖](./domain-model-diagram.svg)

### 六角形架構圖

![六角形架構圖](./hexagonal-architecture-diagram.svg)

### Saga模式圖

![Saga模式圖](./saga-pattern-diagram.svg)

### 限界上下文圖

![限界上下文圖](./bounded-context-diagram.svg)

### 事件風暴圖

![事件風暴圖](./event-storming-diagram.svg)

### CQRS模式圖

![CQRS模式圖](./cqrs-pattern-diagram.svg)

### 事件溯源圖

![事件溯源圖](./event-sourcing-diagram.svg)

### API接口圖

![API接口圖](./api-interface-diagram.svg)

### 數據模型圖

![數據模型圖](./data-model-diagram.svg)

### 安全架構圖

![安全架構圖](./security-architecture-diagram.svg)

### 可觀測性架構圖

![可觀測性架構圖](./observability-diagram.svg)
