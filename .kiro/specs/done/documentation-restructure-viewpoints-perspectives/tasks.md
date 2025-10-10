# 實施計畫

## 任務列表

### 階段一：基礎設施建立 (週 1-2)

- [x] 1. 專案重新定位和根目錄重構
  - 重新撰寫根目錄 README.md，定位為「現代軟體架構最佳實踐範例專案」
  - 移除電商 demo 的定位，強調架構方法論展示價值
  - 突出 Rozanski & Woods 方法論、AI 輔助開發、企業級標準等核心價值
  - _需求: 需求 1, 需求 7_

- [x] 2. 建立新的文件目錄結構
  - 建立 `docs/viewpoints/` 目錄結構，包含 7 大 Viewpoints
  - 建立 `docs/perspectives/` 目錄結構，包含 8 大 Perspectives  
  - 建立對應的圖表目錄結構 `docs/diagrams/viewpoints/` 和 `docs/diagrams/perspectives/`
  - 保留 `docs/legacy/` 目錄存放現有圖表
  - _需求: 需求 1_

- [x] 3. 創建文件模板系統
  - 建立 Viewpoint 文件模板，包含概覽、利害關係人、關注點、架構元素等標準結構
  - 建立 Perspective 文件模板，包含品質屬性、跨視點應用、設計策略等標準結構
  - 建立文件元資料 Front Matter 標準格式
  - 建立圖表元資料標準格式
  - _需求: 需求 1, 需求 5_

- [x] 4. 設定自動化翻譯系統
  - 更新 Hook 配置，支援新的文件結構自動翻譯
  - 建立專業術語字典，包含 Viewpoints、Perspectives 和 DDD 相關術語
  - 測試自動翻譯功能，確保中英文版本同步
  - 建立翻譯品質檢查機制
  - _需求: 需求 2, 需求 6_

### 階段二：內容分析和生成 (週 3-4)

- [x] 5. 基於現有程式碼直接生成圖表和文件
  - 直接讀取和分析 Java 程式碼中的 @AggregateRoot、@ValueObject、@Entity 等 DDD 註解
  - 直接讀取和分析 Gherkin Feature 檔案，提取 BDD 場景和業務流程
  - 基於程式碼分析結果直接生成 PlantUML 類圖和聚合根設計圖
  - 基於 Feature 檔案直接生成 Event Storming PlantUML 圖表
  - 建立圖表自動生成和 PNG 轉換工作流程
  - 生成了 27 個 PlantUML 圖表文件和對應的 PNG 圖片
  - _需求: 需求 3, 需求 4, 需求 6_

- [x] 5.1 分析 DDD 程式碼並生成領域模型圖表
  - 直接讀取 `app/src/main/java/` 目錄下的 Java 檔案
  - 識別和分析所有 @AggregateRoot 註解的類別及其方法和屬性
  - 識別和分析所有 @ValueObject 註解的 Record 及其關係
  - 識別和分析所有 @Entity 註解的類別及其生命週期
  - 識別領域事件類別和事件處理器
  - 直接生成 PlantUML 領域模型類圖和聚合根詳細設計圖
  - 額外生成六角架構概覽圖、應用服務圖和基礎設施層圖
  - _需求: 需求 3, 需求 4_

- [x] 5.2 分析 BDD Feature 檔案並生成業務流程圖表
  - 直接讀取 `app/src/test/resources/features/` 目錄下的所有 .feature 檔案
  - 解析每個 Feature 的 Scenario 和 Given-When-Then 步驟
  - 識別業務流程、使用者旅程和跨聚合互動
  - 提取事件觸發點和業務規則
  - 直接生成 Event Storming Big Picture 和 Process Level PlantUML 圖表
  - 直接生成業務流程圖和用戶旅程概覽圖
  - _需求: 需求 3, 需求 4_

- [x] 5.3 使用 Excalidraw MCP 生成概念圖
  - 基於程式碼分析結果使用 Excalidraw MCP 生成界限上下文概念圖
  - 基於 Feature 檔案使用 Excalidraw MCP 生成使用者旅程概念圖
  - 生成系統架構概念圖和利害關係人對應圖
  - 直接轉換 Excalidraw 檔案為 PNG 格式
  - _需求: 需求 4, 需求 6_

- [x] 6. 完善 Event Storming 三階段圖表和 UML 標準化
  - 更新 Big Picture Event Storming，使用標準配色（事件橙色、熱點紅色、參與者黃色）
  - 更新 Process Level Event Storming，加入命令藍色、聚合黃色、讀模型綠色、策略紫色
  - 更新 Design Level Event Storming，完整展示界限上下文、服務和外部系統
  - 標準化所有 UML 圖表，使用最新 PlantUML 語法和 UML 2.5 標準
  - 確保所有圖表配色符合 Event Storming 和 UML 標準規範
  - _需求: 需求 3, 需求 4_

### 階段三：內容遷移和更新 (週 5-6)

- [x] 7. 遷移現有文件到新結構
  - 將 `design/ddd-guide.md` 遷移到 `viewpoints/functional/domain-model.md`
  - 將 `architecture/hexagonal-architecture.md` 遷移到 `viewpoints/development/hexagonal-architecture.md`
  - 將 `development/` 相關文件遷移到 `viewpoints/development/`
  - 將 `deployment/` 相關文件遷移到 `viewpoints/deployment/`
  - 將 `observability/` 相關文件遷移到 `viewpoints/operational/`
  - 將 `infrastructure/` CDK 相關文件整合到 `viewpoints/deployment/infrastructure-as-code.md`
  - _需求: 需求 1_

- [x] 8. 更新和現代化 DDD 相關內容
  - 更新領域模型文件，反映當前專案的聚合根設計
  - 更新領域事件文件，包含當前實現的事件類型和處理流程
  - 更新界限上下文文件，反映 13 個界限上下文的最新狀態
  - 建立聚合根設計指南，包含當前專案的實際範例
  - _需求: 需求 3_

- [x] 9. 更新架構圖表和視覺化內容
  - ✅ 新增系統架構概覽圖 (system-overview.mmd)，展示7層架構：外部系統、API閘道層、應用服務層、領域層、事件驅動架構、基礎設施層、可觀測性平台、部署平台
  - ✅ 更新文件引用，確保所有圖表連結正確指向新的系統概覽圖
  - ✅ 更新六角架構圖，展示當前的端口和適配器實現
  - ✅ **新增完整的 DDD 分層架構圖** (ddd-layered-architecture.mmd)，包含298行詳細架構描述：
    - 用戶界面層：Web應用、移動應用、管理面板、API文檔
    - 應用層：REST Controllers、Application Services、DTOs & Mappers、Event Handling
    - 領域層：12個聚合根、實體、值對象、領域事件、領域服務、倉庫接口、端口接口
    - 基礎設施層：持久化實現、外部服務適配器、事件基礎設施、快取與搜尋、配置與環境
    - 數據存儲層：PostgreSQL、H2、Redis、OpenSearch、MSK、S3
  - ✅ 更新事件驅動架構圖，展示當前的事件處理機制
  - ✅ 按新的目錄結構重新組織所有圖表
  - ✅ 建立圖表工具使用指南：Mermaid (GitHub 直接顯示) + PlantUML (詳細 UML) + Excalidraw (概念設計)
  - ✅ 設定自動化 PNG 生成腳本，支援所有圖表格式
  - ✅ **執行智能圖表-文件同步系統**，成功整合新的DDD分層架構圖：
    - 分析110個圖表和76個文件
    - 修復62個破損引用
    - 新增18個策略性引用
    - 識別35個孤立圖表待整合
    - 確保所有引用準確無誤
  - ✅ **2025-09-21 執行全面圖表-文件同步驗證**：
    - 成功修復62個破損引用，提升引用準確性70%
    - 新增18個策略性引用，增強文件覆蓋率
    - 識別35個孤立圖表和77個缺失圖表，建立整合計劃
    - 建立自動化同步系統，支援未來架構演進
    - 生成詳細分析報告：reports-summaries/diagrams/diagram-sync-comprehensive-analysis-report.md
  - _需求: 需求 4_

- [x] 10. 建立完整的 Viewpoints & Perspectives 文件和圖表矩陣
  - 為每個 Viewpoint 建立標準文件集合（概覽、架構元素、品質考量、實現指南）
  - 為每個 Perspective 建立跨視點應用文件（設計策略、實現技術、測試驗證）
  - 建立 Viewpoint-Perspective 交叉引用矩陣和相關圖表
  - 確保每個 Viewpoint 都有對應的 Mermaid 概覽圖和 PlantUML 詳細圖
  - 建立概念層級的 Excalidraw 圖表，展示利害關係人和系統互動
  - _需求: 需求 1, 需求 4, 需求 5_

### 階段四：整合和自動化 (週 7-8)

- [x] 11. 建立跨 Viewpoint 和 Perspective 的關聯
  - 在每個 Viewpoint 文件中加入相關 Perspectives 的考量
  - 在每個 Perspective 文件中說明如何影響各個 Viewpoints
  - 建立視點-觀點交叉引用矩陣
  - 建立相關文件的交叉引用連結
  - _需求: 需求 5_

- [x] 12. 建立新的導航和搜尋系統
  - 重新設計文件中心 README.md，提供多維度導航
  - 建立按角色的快速導航路徑
  - 建立按關注點的主題導航
  - 建立視覺化導航，連結到相關圖表
  - 優化文件間的連結和引用
  - _需求: 需求 7_

- [x] 13. 建立 Kiro Hook 自動化圖表生成系統
  - 建立 Kiro Hook 監控 Java 程式碼變更（@AggregateRoot、@ValueObject、@Entity 等）
  - 建立 Kiro Hook 監控 Feature 檔案變更（.feature 檔案）
  - 設定自動生成 PlantUML 圖表並轉換為 PNG
  - 建立智能變更檢測和快取機制
  - 建立完整的自動化管理系統
  - 建立圖表工具選擇指南和最佳實踐
  - _需求: 需求 4, 需求 6_

- [x] 13.1 設定 Kiro Hook 配置檔案
  - 建立 `.kiro/hooks/diagram-auto-generation.kiro.hook` 通用圖表生成 Hook
  - 建立 `.kiro/hooks/ddd-annotation-monitor.kiro.hook` DDD 註解監控 Hook
  - 建立 `.kiro/hooks/bdd-feature-monitor.kiro.hook` BDD Feature 監控 Hook
  - 設定觸發條件：Java 檔案變更、Feature 檔案變更
  - 設定執行動作：智能分析、生成圖表、轉換 PNG、更新文件
  - 建立統一的自動化管理介面
  - 測試 Hook 功能，確保程式碼變更時自動觸發圖表更新
  - _需求: 需求 4, 需求 6_

- [x] 14. 實施文件品質檢查和測試
  - 建立 Markdown 語法檢查機制
  - 建立連結有效性檢查
  - 建立圖表渲染測試
  - 建立翻譯同步檢查
  - 建立文件元資料驗證
  - _需求: 需求 6_

- [x] 15. 生成英文版本和最終整合
  - 觸發自動翻譯，生成完整的英文版本文件結構
  - 檢查翻譯品質，特別是專業術語的一致性
  - 建立舊連結的重定向機制
  - 進行完整的使用者體驗測試
  - 建立文件維護指南
  - _需求: 需求 2, 需求 6, 需求 7_
