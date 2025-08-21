# 事件驅動架構驗證機制

## 概述

本文檔描述了專案中實現的完整事件驅動架構驗證機制，確保跨 bounded context 的事件流轉正確實作，並遵循 DDD 和六角架構的設計原則。

## 實現的事件流轉

### 1. OrderCreatedEvent → 庫存 Bounded Context

**事件流程**：

```
Order Aggregate → OrderCreatedEvent → InventoryEventHandler → InventoryApplicationService
```

**實現位置**：

- 事件：`domain.order.model.events.OrderCreatedEvent`
- 處理器：`infrastructure.event.handler.InventoryEventHandler`
- 應用服務：`application.inventory.service.InventoryApplicationService`

**業務邏輯**：

- 當訂單創建時，自動預留相關商品的庫存
- 確保庫存充足，避免超賣問題
- 在事務提交後執行，保證數據一致性

### 2. PaymentCompletedEvent → 訂單 Bounded Context

**事件流程**：

```
Payment Aggregate → PaymentCompletedEvent → OrderEventHandler → OrderApplicationService
```

**實現位置**：

- 事件：`domain.payment.model.events.PaymentCompletedEvent`
- 處理器：`infrastructure.event.handler.OrderEventHandler`
- 應用服務：`application.order.service.OrderApplicationService`

**業務邏輯**：

- 當支付完成時，更新訂單狀態為已支付
- 觸發後續的訂單處理流程
- 記錄交易ID以便追蹤

### 3. CustomerCreatedEvent → 通知 Bounded Context

**事件流程**：

```
Customer Aggregate → CustomerCreatedEvent → NotificationEventHandler → NotificationApplicationService
```

**實現位置**：

- 事件：`domain.customer.model.events.CustomerCreatedEvent`
- 處理器：`infrastructure.event.handler.NotificationEventHandler`
- 應用服務：`application.notification.service.NotificationApplicationService`

**業務邏輯**：

- 當新客戶註冊時，發送歡迎通知
- 根據會員等級發送不同的歡迎內容
- 支援多種通知渠道（郵件、簡訊等）

### 4. CartItemAddedEvent → 推薦 Bounded Context

**事件流程**：

```
ShoppingCart Aggregate → CartItemAddedEvent → RecommendationEventHandler → RecommendationService
```

**實現位置**：

- 事件：`domain.shoppingcart.model.events.CartItemAddedEvent`
- 處理器：`infrastructure.event.handler.RecommendationEventHandler`
- 服務：推薦服務（目前為模擬實作）

**業務邏輯**：

- 當客戶添加商品到購物車時，更新推薦算法
- 分析客戶偏好，生成個性化推薦
- 即時響應，提升用戶體驗

### 5. PromotionActivatedEvent → 定價 Bounded Context

**事件流程**：

```
Promotion Aggregate → PromotionActivatedEvent → PricingEventHandler → PricingApplicationService
```

**實現位置**：

- 事件：`domain.promotion.model.events.PromotionActivatedEvent`
- 處理器：`infrastructure.event.handler.PricingEventHandler`
- 應用服務：`application.pricing.service.PricingApplicationService`

**業務邏輯**：

- 當促銷活動激活時，更新相關商品的價格
- 根據促銷類型應用不同的定價策略
- 確保價格變更的及時性和準確性

## 架構設計原則

### 1. 端口和適配器模式

**端口（Ports）**：

- `DomainEventPublisher`：領域事件發布端口
- 各種 `ApplicationService`：應用服務端口

**適配器（Adapters）**：

- `DomainEventPublisherAdapter`：Spring 事件發布適配器
- 各種 `EventHandler`：事件處理適配器

### 2. 事件處理器設計原則

**位置約束**：

- 所有事件處理器位於 `infrastructure.event.handler` 包
- 不允許在領域層直接處理跨 bounded context 事件

**依賴約束**：

- 事件處理器只能調用應用服務或領域服務
- 不允許直接操作領域物件或基礎設施組件

**事務管理**：

- 使用 `@TransactionalEventListener` 確保事件在事務提交後處理
- 對於需要即時響應的事件使用 `@EventListener`

### 3. 領域純淨性

**領域層約束**：

- 聚合根不直接依賴其他 bounded context
- 領域事件使用 record 實作，確保不可變性
- 領域層不依賴任何基礎設施框架

**事件設計**：

- 所有事件實作 `DomainEvent` 介面
- 使用工廠方法自動設定事件元數據
- 事件類型自動推導，避免手動錯誤

## 驗證機制

### 1. 單元測試

**EventDrivenArchitectureVerificationTest**：

- 驗證各個事件流轉的正確性
- 測試事件處理器的行為
- 確保端口和適配器模式的實作

### 2. 架構測試

**EventDrivenArchitectureRulesTest**：

- 使用 ArchUnit 驗證架構約束
- 檢查包結構和依賴關係
- 確保設計原則的遵循

### 3. 整合測試

**EventFlowIntegrationTest**：

- 測試完整的業務流程
- 驗證事件在真實環境中的流轉
- 確保事務邊界的正確性

## 監控和錯誤處理

### 1. 事件處理監控

**日誌記錄**：

- 每個事件處理器都有詳細的日誌記錄
- 記錄事件處理的開始、完成和錯誤
- 包含關鍵業務資訊以便追蹤

**性能監控**：

- 監控事件處理的執行時間
- 識別性能瓶頸和異常情況
- 支援業務決策和系統優化

### 2. 錯誤處理策略

**重試機制**：

- 對於臨時性錯誤實作重試邏輯
- 使用指數退避策略避免系統過載
- 設定最大重試次數防止無限循環

**補償機制**：

- 對於關鍵業務流程實作補償邏輯
- 確保數據一致性和業務完整性
- 記錄補償操作以便審計

**錯誤隔離**：

- 推薦系統等非關鍵功能的錯誤不影響主流程
- 使用斷路器模式防止級聯失敗
- 提供降級服務確保系統可用性

## 最佳實踐

### 1. 事件設計

**命名約定**：

- 事件名稱使用過去式，如 `CustomerCreated`、`OrderSubmitted`
- 包含足夠的上下文資訊，避免歧義
- 遵循 bounded context 的命名空間

**資料設計**：

- 事件包含必要的業務資訊，避免額外查詢
- 使用值物件確保資料完整性
- 避免包含敏感資訊，考慮安全性

### 2. 處理器實作

**冪等性**：

- 確保事件處理器的冪等性
- 使用事件ID或業務鍵避免重複處理
- 設計可重複執行的業務邏輯

**性能優化**：

- 避免在事件處理器中執行耗時操作
- 使用異步處理提升系統響應性
- 考慮批量處理提高效率

### 3. 測試策略

**測試覆蓋**：

- 單元測試覆蓋所有事件處理邏輯
- 整合測試驗證完整業務流程
- 架構測試確保設計約束

**測試隔離**：

- 使用 Mock 隔離外部依賴
- 確保測試的獨立性和可重複性
- 提供清晰的測試數據和預期結果

## 未來擴展

### 1. 事件溯源

**考慮實作事件溯源模式**：

- 將所有狀態變更記錄為事件序列
- 支援時間旅行和狀態重建
- 提供完整的審計軌跡

### 2. CQRS 整合

**命令查詢責任分離**：

- 將讀寫操作分離
- 使用事件更新讀取模型
- 提升查詢性能和擴展性

### 3. 分散式事件

**跨服務事件通信**：

- 使用訊息佇列實作跨服務事件
- 確保事件的可靠傳遞
- 支援微服務架構的演進

## 結論

本專案實作的事件驅動架構驗證機制確保了：

1. **正確的事件流轉**：所有跨 bounded context 的通信都通過事件進行
2. **架構純淨性**：領域層保持純淨，不依賴基礎設施框架
3. **設計一致性**：遵循 DDD 和六角架構的設計原則
4. **可測試性**：提供完整的測試覆蓋和驗證機制
5. **可維護性**：清晰的架構邊界和職責分離

這個機制為專案的長期演進和維護提供了堅實的基礎，確保系統的可擴展性和穩定性。
