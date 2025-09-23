
# 發布說明

本目錄包含系統各版本的發布說明，記錄了每次重要更新的內容、架構變更和功能實現。

## 發布歷史

### [專案Refactoring和 API 分組優化 - 2025-01-15](2025-01-15-project-restructure-and-api-grouping.md)

主要內容：

- 專案檔案結構重整，將散亂的根目錄檔案整理到功能目錄
- API 分組Policy重新設計，基於 DDD 和User角色的智能分組
- OpenAPI 標籤優化，使用中文標籤提升使用體驗
- Docker Containerization優化，ARM64 原生支援和效能調優
- 領域模型完善，實作完整的 DDD 架構和測試體系

### Testing

主要內容：

- 建立完整的測試輔助工具基礎設施（資料建構器、場景處理器、自定義匹配器）
- RefactoringBDD步驟定義，消除所有條件邏輯（if-else語句）
- 改善Integration Test的3A結構，拆分複雜測試為獨立測試方法
- 建立測試分類和標籤系統（@UnitTest, @IntegrationTest, @SlowTest, @BddTest）
- 大幅提升測試程式碼的可讀性、維護性和Reliability

### [架構優化與DDD分層實現 - 2025-06-08](architecture-optimization-2025-06-08.md)

主要內容：

- 解決Interface Layer直接依賴Domain Layer問題
- 調整Adapter包結構至正確的位置
- 處理Aggregate Root內部類問題
- 實現嚴格的DDDLayered Architecture

### [促銷模組實作與架構優化 - 2025-05-21](promotion-module-implementation-2025-05-21.md)

主要內容：

- 實現電子商務平台的促銷功能模組
- 超商優惠券系統、限時特價、限量特價等功能
- 架構優化，將Voucher從Value Object重新分類為Entity
- 實現PromotionContext類的Specification接口

## 發布流程

每次重要更新都應該在本目錄下創建一個新的發布說明文檔，命名格式為：`<主題>-<年份>-<月份>-<日期>.md`。

發布說明應包含以下內容：

1. 業務需求概述
2. 技術實現
3. 架構變更
4. 技術細節
5. 測試覆蓋
6. conclusion
