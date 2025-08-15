# Implementation Plan

- [x] 1. 建立標準化錯誤回應結構
  - 創建 StandardErrorResponse DTO 類別，包含錯誤代碼、訊息、時間戳記和詳細資訊
  - 實作 ErrorCode 列舉來定義標準錯誤代碼
  - 建立 GlobalExceptionHandler 來統一處理各種例外情況
  - 為錯誤回應添加完整的 @Schema 註解
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 2. 增強 OpenAPI 基礎配置
  - 更新 OpenApiConfig 類別，添加 Components 配置
  - 定義標準錯誤回應的 Schema 組件
  - 配置安全認證方案（Bearer Token）
  - 完善 API 資訊、聯絡方式和授權資訊
  - 建立環境特定的伺服器配置
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 3. 建立環境配置檔案
  - 創建 application-openapi.yml 配置檔案
  - 配置 SpringDoc 的各項設定（路徑、UI 選項、分組等）
  - 設定 API 分組：公開 API、內部 API、管理端點
  - 配置 Swagger UI 的客製化選項
  - _Requirements: 6.1, 6.4, 5.4_

- [x] 4. 為訂單控制器完善 OpenAPI 註解
  - 檢查並完善現有的 @Tag 註解
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 確保所有回應都引用正確的 Schema
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 5. 為支付控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述支付模組功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 引入標準錯誤回應的 Schema 引用
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 6. 為庫存控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述庫存管理功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 特別處理庫存相關的業務錯誤回應
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 7. 為產品控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述產品管理功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 確保產品相關的 DTO 都有適當的 Schema 引用
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 8. 為客戶控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述客戶管理功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 處理客戶相關的隱私和安全考量
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 9. 為定價控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述定價策略功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 特別處理定價相關的複雜業務邏輯回應
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 10. 為活動控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述活動記錄功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 處理活動記錄的查詢和篩選參數
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 11. 為統計控制器添加 OpenAPI 註解
  - 添加 @Tag 註解描述統計報表功能
  - 為所有端點方法添加 @Operation 註解
  - 為所有端點添加完整的 @ApiResponses 註解
  - 為所有參數添加 @Parameter 註解
  - 處理統計數據的複雜查詢參數
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 12. 為訂單相關 DTO 添加 Schema 註解
  - 為 CreateOrderRequest 添加完整的 @Schema 註解
  - 為 OrderResponse 添加詳細的欄位描述和範例值
  - 為 AddOrderItemRequest 添加驗證和文檔註解
  - 確保所有必填欄位都標記為 required = true
  - 為列舉類型欄位添加 allowableValues
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 13. 為支付相關 DTO 添加 Schema 註解
  - 為 PaymentRequest 添加完整的 @Schema 註解
  - 為 PaymentResponse 添加詳細的欄位描述和範例值
  - 處理金額欄位的格式和精度要求
  - 為支付狀態列舉添加完整的值列表
  - 添加敏感資訊的處理註解
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 14. 為庫存相關 DTO 添加 Schema 註解
  - 為所有庫存請求 DTO 添加 @Schema 註解
  - 為 InventoryResponse 和 ReservationResponse 添加詳細描述
  - 處理數量欄位的最小值和最大值限制
  - 為庫存操作類型添加列舉值說明
  - 添加庫存狀態的詳細文檔
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 15. 為產品相關 DTO 添加 Schema 註解
  - 為 UpdateProductRequest 添加完整的 @Schema 註解
  - 為產品相關的回應 DTO 添加詳細描述
  - 處理產品分類和屬性的複雜結構
  - 為產品狀態列舉添加完整說明
  - 添加產品圖片和媒體欄位的格式要求
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 16. 為定價相關 DTO 添加 Schema 註解
  - 為 CreatePricingRuleRequest 添加 @Schema 註解
  - 為 UpdateCommissionRateRequest 添加詳細描述
  - 處理定價規則的複雜邏輯說明
  - 為佣金費率添加百分比格式說明
  - 添加定價策略類型的列舉值
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 17. 建立 OpenAPI 文檔生成測試
  - 創建 OpenApiDocumentationTest 測試類別
  - 實作 generateOpenApiJson 測試方法
  - 實作 generateOpenApiYaml 測試方法
  - 確保生成的檔案儲存在 docs/api 目錄
  - 添加檔案格式化和美化功能
  - _Requirements: 4.1, 4.2, 4.3, 4.5_

- [x] 18. 建立 Gradle 文檔生成任務
  - 在 build.gradle 中添加 generateApiDocs 任務
  - 配置任務依賴和執行條件
  - 添加任務執行前後的提示訊息
  - 整合到現有的建構流程中
  - 提供清楚的使用說明
  - _Requirements: 4.4, 4.5_

- [x] 19. 建立 API 使用文檔
  - 創建 docs/api/README.md 文檔檔案
  - 撰寫快速開始指南
  - 提供 API 概覽和模組說明
  - 添加開發指南和最佳實踐
  - 包含配置選項和自訂說明
  - 提供相關資源連結
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 20. 驗證和測試 Swagger UI 功能
  - 啟動應用程式並驗證 Swagger UI 可正常載入
  - 測試所有 API 端點在 Swagger UI 中的顯示
  - 驗證 Try it out 功能正常運作
  - 檢查 API 分組和標籤顯示正確
  - 測試搜尋和篩選功能
  - 驗證錯誤回應格式在 UI 中的顯示
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
