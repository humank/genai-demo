# Consumer API Gap Analysis Implementation Plan

## 任務概述

基於現有的 Customer 聚合根，增強和新增消費者導向的 API 功能，填補 feature files 描述的消費者需求與現有 API 之間的差距。

**任務數量優化：** 原本 42 個子任務合併為 **5 個主要任務**，保持所有必要工作內容。

## 實施任務

- [x] 1. 實現所有領域聚合根和值對象
  - **增強現有 Customer 聚合根**：新增 birthDate、registrationDate、rewardPoints 等屬性，使用 Java Record 實現 RewardPoints 和 NotificationPreferences 值對象，完善領域方法和事件機制
  - **創建 ShoppingCart 聚合根**：實現購物車商品管理、總價計算、優惠計算邏輯，整合促銷規則和多重優惠選擇算法
  - **創建 Promotion 聚合根和促銷規則引擎**：使用 Sealed Interface 實現 PromotionRule 體系，創建各種促銷規則，實現優惠券生成驗證邏輯
  - **創建 Voucher 聚合根（超商優惠券系統）**：實現優惠券購買使用報失補發完整流程，兌換碼生成驗證，有效期管理
  - **創建 ProductReview 聚合根（評價系統）**：實現評價提交修改審核功能，統計分析，檢舉處理機制
  - 所有聚合根使用正確的 DDD 註解（@AggregateRoot、@ValueObject、@DomainService）
  - 創建對應的 JPA 實體和 Repository 適配器
  - 實現完整的領域事件發布機制
  - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 7.2, 8.1, 8.2, 8.3, 8.4, 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 2. 實現所有消費者端 REST API 控制器
  - **商品相關 API**：創建 ConsumerProductController（商品瀏覽、搜尋、詳情、分類、推薦 API）
  - **購物車 API**：創建 ShoppingCartController（購物車 CRUD、結帳計算、優惠應用 API）
  - **促銷活動 API**：創建 ConsumerPromotionController（促銷活動列表、個人化推薦、優惠券查詢使用 API）
  - **會員功能 API**：創建 ConsumerMemberController（會員資料管理、紅利點數查詢兌換、通知偏好設定 API）
  - **訂單追蹤 API**：增強現有 OrderController 支援消費者視角，實現個人訂單列表、詳情查詢、狀態篩選、排序、搜尋功能
  - **配送管理 API**：創建 DeliveryTrackingController（即時配送狀態、配送員資訊、路線追蹤、地址修改、時間預約、異常處理、確認評價 API）
  - **通知系統 API**：創建 ConsumerNotificationController（個人通知列表、詳情查詢、分類篩選、已讀狀態更新、刪除歸檔、偏好設定、訂閱管理 API）
  - **評價系統 API**：創建 ProductReviewController（商品評價列表、統計分布、個人評價歷史查詢、評價提交修改、圖片上傳、檢舉回覆功能 API）
  - **推薦系統 API**：創建 RecommendationService 和 RecommendationController（個人化商品推薦、相關商品推薦、熱門商品查詢、新品推薦、價格趨勢分析 API）
  - 所有控制器加上完整的 OpenAPI 註解（@Tag、@Operation、@ApiResponses）
  - 編寫完整的 API 整合測試和 BDD 測試
  - 更新 OpenAPI 規範文件
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 6.4, 7.1, 7.2, 7.3, 7.4, 9.1, 9.2, 9.3, 9.4, 9.5, 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 3. 實現資料庫遷移和測試資料
  - 設計 shopping_carts、promotions、vouchers、product_reviews、notifications 等新表
  - 更新 customers 表新增缺失欄位（birthDate、registrationDate 等）
  - 創建新表的 DDL 遷移腳本和現有表的 ALTER 遷移腳本（使用 JPA 優先策略）
  - 新增索引和約束條件，設計表之間的關聯關係
  - 擴展現有的測試資料生成腳本，新增消費者導向的測試資料
  - 創建促銷活動、優惠券、評價、通知等測試資料
  - 確保所有遷移腳本通過測試驗證
  - _Requirements: 所有需求_

- [x] 4. 實現完整的測試覆蓋
  - 實現所有新聚合根的單元測試（業務邏輯、領域事件、異常情況、邊界條件）
  - 實現所有新 API 端點的整合測試（功能驗證、資料格式、認證授權、錯誤處理）
  - 新增消費者 BDD 測試（完整購物流程、會員優惠使用、促銷活動參與、評價通知）
  - 實現 ArchUnit 架構測試（DDD 註解規範、架構合規性、依賴方向、分層約束檢查）
  - 實現測試資料建構器模式和效能測試
  - 確保所有測試通過並生成 Allure 報告
  - _Requirements: 所有需求_

- [x] 5. 更新文檔、部署配置和專案維護
  - 更新 OpenAPI 規範包含所有新消費者 API 的定義
  - 實現 API 分組和標籤管理，新增詳細的請求回應範例
  - 更新 Docker 配置支援新功能，新增監控日誌配置
  - 實現 API 效能監控和健康檢查端點
  - 更新主要 README.md 和 FULLSTACK_README.md 文件包含新功能說明
  - 更新架構文檔和 UML 圖表反映新的聚合根和 API
  - 實現 API 版本管理策略和文檔自動生成
  - 確保所有文檔與實作保持同步
  - _Requirements: 所有需求_
