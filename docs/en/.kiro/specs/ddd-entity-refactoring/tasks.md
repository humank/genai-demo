
# Implementation

## Implementation

- [x] 1. 建立 Entity 基礎設施
  - ~~創建 BaseEntity 抽象類別和相關介面~~ (跳過，直接使用具體 Entity)
  - Entity 註解已存在，無需額外建立
  - 測試基礎設施已就緒
  - _需求: 1.1, 2.1, 3.1_

- [ ] 2. Refactoring Seller Aggregate邊界
- [x] 2.1 創建 Seller Aggregate內的新 Entity 類別
  - 實作 SellerProfile Entity（從Aggregate Root轉換），遵循專案現有 Entity 模式
  - 創建 ContactInfo Entity 管理聯繫資訊，使用 @Entity 註解
  - 實作 SellerRating Entity 管理評級歷史，包含強型別 ID Value Object
  - 創建 SellerVerification Entity 管理驗證狀態，遵循現有狀態管理模式
  - 為所有 Entity 創建對應的 ID Value Object，使用 record 實作
  - _需求: 1.1, 1.2_

- [x] 2.2 Refactoring Seller Aggregate Root類別
  - 修改 Seller 類別以包含新的 Entity 集合，保持現有 @AggregateRoot 註解格式
  - 遷移 SellerProfile 的業務邏輯到新的 Entity 結構
  - 實作Aggregate內的一致性規則和業務邏輯，遵循現有Aggregate Root模式
  - 更新Aggregate Root的事件發布機制，使用現有的事件收集機制
  - 提供向後相容的 API，確保現有程式碼不受影響
  - _需求: 1.1, 1.3_

- [x] 2.3 移除獨立的 SellerProfile Aggregate Root
  - 刪除 SellerProfile Aggregate Root類別
  - 更新所有引用 SellerProfile Aggregate Root的程式碼
  - 遷移 SellerProfileRepository 邏輯到 SellerRepository，遵循現有 Repository 模式
  - 更新相關的 Domain Service，使用 @DomainService 註解
  - 更新Repository映射和持久化邏輯，保持資料完整性
  - _需求: 1.4_

- [ ] 3. Refactoring ProductReview Aggregate的 Entity 結構
- [x] 3.1 創建 ProductReview Aggregate內的新 Entity
  - 實作 ReviewImage Entity 替換簡單的 String 列表
  - 創建 ModerationRecord Entity（從 ReviewModeration Aggregate Root轉換）
  - 實作 ReviewResponse Entity 支援商家回覆功能
  - 為每個 Entity 添加豐富的業務邏輯和狀態管理
  - _需求: 2.1, 2.2, 2.4_

- [x] 3.2 Refactoring ProductReview Aggregate Root
  - 修改 ProductReview 類別以使用新的 Entity 集合
  - 遷移圖片管理邏輯到 ReviewImage Entity
  - 整合審核邏輯到 ModerationRecord Entity
  - 實作Aggregate內的業務規則和一致性檢查
  - _需求: 2.1, 2.3_

- [x] 3.3 移除 ReviewModeration 獨立Aggregate Root
  - 刪除 ReviewModeration Aggregate Root類別
  - 更新審核流程以使用 ProductReview Aggregate內的 ModerationRecord
  - 遷移相關的業務邏輯和事件處理
  - 更新測試以反映新的Aggregate結構
  - _需求: 2.1_

- [ ] 4. 豐富 Customer Aggregate的 Entity 結構
- [x] 4.1 創建 Customer Aggregate內的新 Entity
  - 實作 DeliveryAddress Entity 替換簡單的 Address 列表
  - 創建 CustomerPreferences Entity 統一管理偏好設定
  - 為 DeliveryAddress 添加狀態管理和驗證邏輯
  - 實作地址的預設設定和優先級管理
  - _需求: 3.1, 3.3_

- [x] 4.2 Refactoring Customer Aggregate Root
  - 修改 Customer 類別以使用新的 Entity 結構
  - 遷移地址管理邏輯到 DeliveryAddress Entity
  - 整合偏好設定邏輯到 CustomerPreferences Entity
  - 更新Aggregate內的業務規則和一致性檢查
  - _需求: 3.1, 3.3_

- [ ] 5. 豐富 Inventory Aggregate的 Entity 結構
- [x] 5.1 創建 Inventory Aggregate內的新 Entity
  - 實作 StockReservation Entity 替換簡單的 Map 結構
  - 創建 StockMovement Entity 記錄庫存異動歷史
  - 實作 InventoryThreshold Entity 管理庫存閾值規則
  - 為每個 Entity 添加過期處理和狀態轉換邏輯
  - _需求: 3.2, 3.3_

- [x] 5.2 Refactoring Inventory Aggregate Root
  - 修改 Inventory 類別以使用新的 Entity 集合
  - 遷移預留管理邏輯到 StockReservation Entity
  - 整合庫存異動Tracing到 StockMovement Entity
  - 實作Aggregate內的庫存一致性規則
  - _需求: 3.2, 3.4_

- [ ] 6. 處理過於簡單的Aggregate Root
- [x] 6.1 評估和Refactoring PaymentMethod Aggregate Root
  - 分析 PaymentMethod 是否應該作為 Customer Aggregate內的 Entity
  - 如果適合，將 PaymentMethod 轉換為 Customer Aggregate內的 Entity
  - 如果保持獨立，豐富其業務邏輯和內部結構
  - 更新相關的業務流程和資料存取邏輯
  - _需求: 4.1, 4.3_

- [x] 6.2 評估和改善 NotificationTemplate Aggregate Root
  - 分析 NotificationTemplate 的Aggregate邊界合理性
  - 添加模板變數管理和版本控制的 Entity 結構
  - 豐富模板渲染和驗證的業務邏輯
  - 實作模板使用統計和效能Tracing功能
  - _需求: 4.2, 4.3_

- [x] 7. 更新測試和文件
- [x] 7.1 更新Architecture Test和Integration Test
  - 所有 Domain Model 已由現有 BDD 測試覆蓋，無需額外Unit Test
  - 使用 ArchUnit 驗證新的 Entity 結構符合 DDD 戰術模式
  - 驗證Aggregate邊界和依賴關係的正確性
  - 更新現有的Integration Test以涵蓋新的 Entity 結構
  - 確保Refactoring後的向後相容性測試通過
  - _需求: 1.4, 2.4, 3.4, 4.4_

- [x] 7.2 更新技術文件和 API 文件
  - 更新 DDD 設計文件以反映新的Aggregate結構
  - 編寫 Entity 設計和使用指南
  - 更新 API 文件以反映Aggregate邊界的變更
  - 創建Refactoring前後的對比文件和遷移指南
  - _需求: 1.4, 2.4, 3.4, 4.4_

- [ ] 8. 驗證和Deployment
- [ ] 8.1 執行完整的回歸測試
  - 運行所有現有的測試套件確保無回歸問題
  - 執行效能測試驗證Refactoring不影響系統效能
  - 進行資料一致性檢查確保資料完整性
  - 驗證 API 向後相容性和業務流程正確性
  - _需求: 1.4, 2.4, 3.4, 4.4_

- [ ] 8.2 準備生產Deployment
  - 準備Repository遷移腳本和回滾計劃
  - 建立Monitoring和告警機制監測Refactoring影響
  - 準備Deployment檢查清單和驗證步驟
  - 制定緊急回滾程序和故障排除指南
  - _需求: 1.4, 2.4, 3.4, 4.4_
