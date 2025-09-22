# 界限上下文設計

## 概覽

本專案實現了 13 個界限上下文，每個上下文都有明確的業務邊界和職責劃分。界限上下文的設計遵循 DDD 戰略模式，確保高內聚、低耦合的領域模型。

## 界限上下文架構圖

```mermaid
graph TB
    subgraph "核心業務上下文"
        Customer[Customer<br/>客戶管理]
        Order[Order<br/>訂單管理]
        Product[Product<br/>產品管理]
        Inventory[Inventory<br/>庫存管理]
    end
    
    subgraph "支付與交易上下文"
        Payment[Payment<br/>支付管理]
        Pricing[Pricing<br/>定價管理]
        Promotion[Promotion<br/>促銷管理]
    end
    
    subgraph "履約與服務上下文"
        Delivery[Delivery<br/>配送管理]
        Review[Review<br/>評價管理]
        Notification[Notification<br/>通知管理]
    end
    
    subgraph "平台與運營上下文"
        Seller[Seller<br/>賣家管理]
        ShoppingCart[ShoppingCart<br/>購物車管理]
        Observability[Observability<br/>可觀測性]
    end
    
    Customer --> Order
    Order --> Payment
    Order --> Inventory
    Order --> Delivery
    Product --> Inventory
    Customer --> Review
    Seller --> Product
    Customer --> ShoppingCart
    ShoppingCart --> Order
```

## 界限上下文詳細說明

### 1. Customer Context (客戶上下文)

**職責**: 管理客戶生命週期、會員等級、個人資料和偏好設定

**聚合根**: 
- `Customer` (v2.0) - 增強的客戶聚合根，支援完整的消費者功能

**核心實體**:
- `DeliveryAddress` - 配送地址
- `PaymentMethod` - 支付方式配置
- `CustomerPreferences` - 客戶偏好設定

**主要業務能力**:
- 客戶註冊與認證
- 會員等級管理 (STANDARD, PREMIUM, VIP)
- 個人資料維護
- 配送地址管理
- 通知偏好設定
- 紅利點數累積與兌換

**領域服務**:
- `RewardPointsService` - 紅利點數服務
- `CustomerDiscountService` - 客戶折扣服務

### 2. Order Context (訂單上下文)

**職責**: 管理訂單生命週期、訂單處理流程和工作流狀態

**聚合根**:
- `Order` (v1.0) - 訂單聚合根，封裝訂單相關的業務規則和行為
- `OrderWorkflow` (v1.0) - 訂單工作流聚合根，管理訂單從創建到完成的整個生命週期

**核心實體**:
- `OrderItem` - 訂單項目
- `OrderStatus` - 訂單狀態
- `WorkflowStep` - 工作流步驟

**主要業務能力**:
- 訂單創建與確認
- 訂單項目管理
- 訂單狀態追蹤
- 工作流程控制
- 訂單取消與退款

### 3. Product Context (產品上下文)

**職責**: 管理產品資訊、產品分類和產品生命週期

**聚合根**:
- `Product` (v1.0) - 產品聚合根，管理產品信息和庫存

**核心實體**:
- `ProductCategory` - 產品分類
- `ProductSpecification` - 產品規格
- `ProductImage` - 產品圖片

**主要業務能力**:
- 產品資訊管理
- 產品分類維護
- 產品上下架控制
- 產品規格管理
- 產品圖片管理

**領域服務**:
- `BundleService` - 捆綁銷售服務

### 4. Inventory Context (庫存上下文)

**職責**: 管理產品庫存、庫存預留和庫存調整

**聚合根**:
- `Inventory` (v1.0) - 庫存聚合根，管理產品庫存和預留

**核心實體**:
- `StockReservation` - 庫存預留
- `StockMovement` - 庫存異動記錄

**主要業務能力**:
- 庫存數量管理
- 庫存預留與釋放
- 庫存調整記錄
- 安全庫存監控

**領域服務**:
- `InventoryService` - 庫存服務介面
- `InventoryDomainService` - 庫存領域服務

### 5. Payment Context (支付上下文)

**職責**: 管理支付流程、支付方式和支付狀態

**聚合根**:
- `Payment` (v1.0) - 支付聚合根，管理支付流程和狀態
- `PaymentMethod` (v1.0) - 支付方式聚合根，管理客戶的支付方式配置

**核心實體**:
- `PaymentTransaction` - 支付交易
- `PaymentStatus` - 支付狀態
- `RefundRecord` - 退款記錄

**主要業務能力**:
- 支付處理流程
- 多種支付方式支援
- 支付狀態追蹤
- 退款處理
- 支付安全驗證

**領域服務**:
- `PaymentDomainService` - 支付領域服務

### 6. Delivery Context (配送上下文)

**職責**: 管理配送流程、配送狀態和配送追蹤

**聚合根**:
- `Delivery` (v1.0) - 配送聚合根，管理訂單的配送流程和狀態

**核心實體**:
- `DeliveryRoute` - 配送路線
- `DeliveryStatus` - 配送狀態
- `TrackingInfo` - 追蹤資訊

**主要業務能力**:
- 配送任務創建
- 配送狀態追蹤
- 配送路線規劃
- 配送完成確認

**領域服務**:
- `DeliveryManagementService` - 配送管理服務

### 7. Review Context (評價上下文)

**職責**: 管理商品評價、評價審核和評價展示

**聚合根**:
- `ProductReview` (v2.0) - 商品評價聚合根，管理消費者的商品評價和評分

**核心實體**:
- `ReviewImage` - 評價圖片
- `ModerationRecord` - 審核記錄
- `ReviewRating` - 評價評分

**主要業務能力**:
- 商品評價創建
- 評價內容審核
- 評價圖片管理
- 評價統計分析

**領域服務**:
- `ReviewModerationService` - 評價審核領域服務

### 8. Seller Context (賣家上下文)

**職責**: 管理賣家資訊、賣家認證和賣家評級

**聚合根**:
- `Seller` (v2.0) - 賣家聚合根，管理賣家基本資訊和狀態

**核心實體**:
- `SellerProfile` - 賣家檔案
- `SellerRating` - 賣家評級
- `SellerVerification` - 賣家認證
- `ContactInfo` - 聯絡資訊

**主要業務能力**:
- 賣家註冊與認證
- 賣家檔案管理
- 賣家評級系統
- 賣家狀態管理

**領域服務**:
- `SellerDomainService` - 賣家領域服務

### 9. ShoppingCart Context (購物車上下文)

**職責**: 管理購物車狀態、購物車項目和購物車操作

**聚合根**:
- `ShoppingCart` (v1.0) - 購物車聚合根，管理消費者的購物車狀態和商品項目

**核心實體**:
- `CartItem` - 購物車項目
- `CartStatus` - 購物車狀態

**主要業務能力**:
- 購物車項目管理
- 購物車狀態維護
- 購物車結算處理
- 購物車持久化

**領域服務**:
- `ShoppingCartDomainService` - 購物車領域服務

### 10. Promotion Context (促銷上下文)

**職責**: 管理促銷活動、優惠券和促銷規則

**聚合根**:
- `Promotion` (v1.0) - 促銷聚合根，管理各種促銷活動規則
- `Voucher` (v1.0) - 超商優惠券聚合根，管理優惠券的完整生命週期

**核心實體**:
- `PromotionRule` - 促銷規則
- `VoucherCode` - 優惠券代碼
- `DiscountCalculation` - 折扣計算

**主要業務能力**:
- 促銷活動管理
- 優惠券發放與使用
- 折扣規則計算
- 促銷效果追蹤

### 11. Pricing Context (定價上下文)

**職責**: 管理產品定價、折扣規則和佣金計算

**聚合根**:
- `PricingRule` (v1.0) - 定價規則聚合根，負責管理產品定價、折扣和佣金費率

**核心實體**:
- `PriceRule` - 價格規則
- `DiscountRule` - 折扣規則
- `CommissionRate` - 佣金費率

**主要業務能力**:
- 動態定價管理
- 折扣規則配置
- 佣金費率計算
- 價格策略執行

**領域服務**:
- `CommissionService` - 佣金服務

### 12. Notification Context (通知上下文)

**職責**: 管理系統通知、通知模板和通知發送

**聚合根**:
- `Notification` (v1.0) - 通知聚合根，管理系統通知的發送和狀態
- `NotificationTemplate` (v1.0) - 通知模板聚合根，管理通知模板的內容和格式

**核心實體**:
- `NotificationChannel` - 通知渠道
- `NotificationStatus` - 通知狀態
- `TemplateVariable` - 模板變數

**主要業務能力**:
- 多渠道通知發送
- 通知模板管理
- 通知狀態追蹤
- 通知偏好設定

**領域服務**:
- `NotificationService` - 通知服務

### 13. Observability Context (可觀測性上下文)

**職責**: 管理系統監控、分析數據和可觀測性會話

**聚合根**:
- `ObservabilitySession` (v1.0) - 可觀測性會話聚合根
- `AnalyticsSession` (v1.0) - 分析會話聚合根

**核心實體**:
- `MetricData` - 指標數據
- `TraceInfo` - 追蹤資訊
- `LogEntry` - 日誌條目

**主要業務能力**:
- 系統監控數據收集
- 業務分析數據處理
- 可觀測性會話管理
- 性能指標追蹤

## 上下文間關係

### 核心依賴關係

```mermaid
graph LR
    Customer --> Order
    Order --> Payment
    Order --> Inventory
    Order --> Delivery
    Product --> Inventory
    Customer --> Review
    Seller --> Product
    Customer --> ShoppingCart
    ShoppingCart --> Order
    Order --> Promotion
    Product --> Pricing
    Order --> Notification
```

### 事件驅動通信

各界限上下文通過領域事件進行異步通信：

- **Customer Context** 發布客戶相關事件 (CustomerCreated, CustomerProfileUpdated 等)
- **Order Context** 發布訂單相關事件 (OrderCreated, OrderConfirmed 等)
- **Payment Context** 發布支付相關事件 (PaymentProcessed, PaymentFailed 等)
- **Inventory Context** 發布庫存相關事件 (StockReserved, StockAdded 等)

## 上下文演進策略

### 版本管理

- **v1.0**: 基礎功能實現
- **v2.0**: 增強功能和優化 (Customer, ProductReview, Seller)

### 未來演進方向

1. **微服務拆分**: 每個界限上下文可獨立部署為微服務
2. **事件溯源**: 關鍵上下文 (Order, Payment) 可引入事件溯源
3. **CQRS**: 讀寫分離優化查詢性能
4. **新上下文**: 根據業務發展需要添加新的界限上下文

## 相關圖表

- [界限上下文概念圖](../../diagrams/generated/functional/Bounded%20Contexts%20Concept.png) - **New**: Comprehensive bounded contexts concept diagram showing all 13 contexts with their relationships, responsibilities, and domain events
- [界限上下文概覽圖](../../diagrams/generated/functional/Bounded%20Contexts%20Overview.png)
- [Event Storming Big Picture](../../diagrams/generated/functional/Event%20Storming%20Big%20Picture.png)
- [Event Storming Process Level](../../diagrams/generated/functional/Event%20Storming%20Process%20Level.png)
- [業務流程圖](../../diagrams/generated/functional/Business%20Process%20Flows.png) - **Recently Updated**: Comprehensive e-commerce core processes including customer shopping journey, order processing workflow, coupon management, inventory management, and customer notifications
- [使用者旅程概覽圖](../../diagrams/generated/functional/User%20Journey%20Overview.png)

## 與其他視點的關聯

- **[資訊視點](../information/README.md)**: 上下文間的事件通信和資料流
- **[開發視點](../development/README.md)**: 模組化架構和程式碼組織
- **[部署視點](../deployment/README.md)**: 微服務部署策略

## 設計原則

1. **高內聚**: 每個上下文內部功能緊密相關
2. **低耦合**: 上下文間通過事件和介面通信
3. **業務導向**: 上下文劃分基於業務能力而非技術考量
4. **演進友好**: 支援獨立演進和版本管理
5. **測試隔離**: 每個上下文可獨立測試和驗證

這種界限上下文設計確保了系統的可維護性、可擴展性和業務對齊性。