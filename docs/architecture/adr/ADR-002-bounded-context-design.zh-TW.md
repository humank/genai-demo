# ADR-002: 限界上下文設計策略

## 狀態

**已接受** - 2024-01-20

## 背景

GenAI Demo 電商平台需要清晰的領域邊界來管理複雜性並促進團隊協作。基於用戶故事分析和業務需求，我們需要識別和設計適當的限界上下文，以反映業務領域結構。

### 業務需求分析

從 BDD 功能文件和用戶故事中，我們識別出以下關鍵業務能力：

#### 客戶管理 (Customer Management)

- **用戶故事**: "作為客戶，我想管理我的個人資料和會員等級，以便獲得個人化的優惠"
- **關鍵場景**: 註冊、個人資料更新、會員升級、紅利點數管理

#### 訂單處理 (Order Processing)

- **用戶故事**: "作為客戶，我想下訂單並追蹤訂單，以便高效地購買產品"
- **關鍵場景**: 訂單創建、項目管理、狀態追蹤、取消訂單

#### 產品目錄 (Product Catalog)

- **用戶故事**: "作為客戶，我想瀏覽產品並查看詳細資訊，以便做出明智的購買決策"
- **關鍵場景**: 產品瀏覽、搜尋、篩選、會員專屬產品

#### 庫存管理 (Inventory Management)

- **用戶故事**: "作為商家，我想管理產品庫存，以便確保產品可用性"
- **關鍵場景**: 庫存追蹤、補貨、預留、釋放

#### 支付處理 (Payment Processing)

- **用戶故事**: "作為客戶，我想安全地支付訂單，以便完成購買"
- **關鍵場景**: 支付處理、退款、支付方式管理

#### 物流配送 (Delivery Management)

- **用戶故事**: "作為客戶，我想追蹤我的配送狀態，以便了解訂單進度"
- **關鍵場景**: 配送安排、狀態更新、配送追蹤

#### 促銷活動 (Promotion Management)

- **用戶故事**: "作為客戶，我想使用優惠券和參與促銷活動，以便獲得折扣"
- **關鍵場景**: 優惠券管理、促銷規則、折扣計算

#### 定價管理 (Pricing Management)

- **用戶故事**: "作為商家，我想設定靈活的定價策略，以便優化收益"
- **關鍵場景**: 價格計算、佣金管理、動態定價

#### 通知服務 (Notification Service)

- **用戶故事**: "作為客戶，我想收到重要的訂單和促銷通知，以便及時了解資訊"
- **關鍵場景**: 通知發送、模板管理、通知偏好

#### 工作流管理 (Workflow Management)

- **用戶故事**: "作為系統，我需要協調複雜的業務流程，以便確保操作的一致性"
- **關鍵場景**: 流程編排、狀態管理、異常處理

## 決策

我們決定建立 **10 個限界上下文**，基於業務能力和團隊結構進行劃分。

### 上下文映射策略

#### 核心上下文 (Core Contexts)

這些上下文包含核心業務邏輯，是系統的競爭優勢所在：

1. **Customer Context** - 客戶聚合
   - 客戶資料管理
   - 會員等級和紅利點數
   - 客戶偏好設定

2. **Order Context** - 訂單聚合
   - 訂單生命週期管理
   - 訂單項目管理
   - 訂單狀態追蹤

3. **Product Context** - 產品聚合
   - 產品目錄管理
   - 產品資訊維護
   - 產品分類和標籤

4. **Inventory Context** - 庫存聚合
   - 庫存水準追蹤
   - 庫存預留和釋放
   - 庫存補充管理

#### 支援上下文 (Supporting Contexts)

這些上下文支援核心業務流程：

5. **Payment Context** - 支付聚合
   - 支付處理
   - 支付方式管理
   - 退款處理

6. **Delivery Context** - 配送聚合
   - 配送安排
   - 配送狀態追蹤
   - 配送商管理

7. **Promotion Context** - 促銷聚合
   - 優惠券管理
   - 促銷規則引擎
   - 折扣計算

8. **Pricing Context** - 定價聚合
   - 價格計算引擎
   - 佣金管理
   - 定價策略

#### 通用上下文 (Generic Contexts)

這些上下文提供通用服務：

9. **Notification Context** - 通知聚合
   - 通知發送
   - 通知模板管理
   - 通知偏好

10. **Workflow Context** - 工作流聚合
    - 業務流程編排
    - 狀態機管理
    - 流程監控

### 上下文關係映射

#### 1. 夥伴關係 (Partnership)

- **Customer ↔ Order**: 客戶和訂單緊密協作
- **Order ↔ Inventory**: 訂單和庫存需要同步協調

#### 2. 客戶-供應商 (Customer-Supplier)

- **Order → Payment**: 訂單驅動支付處理
- **Order → Delivery**: 訂單觸發配送安排
- **Customer → Notification**: 客戶事件觸發通知

#### 3. 遵循者 (Conformist)

- **Promotion → Pricing**: 促銷遵循定價規則
- **All Contexts → Workflow**: 所有上下文遵循工作流規範

#### 4. 防腐層 (Anti-Corruption Layer)

- **Payment Context**: 與外部支付系統整合時使用防腐層
- **Delivery Context**: 與第三方物流系統整合時使用防腐層

### 實現策略

#### 1. 包結構設計

```
solid.humank.genaidemo/
├── domain/
│   ├── customer/          # 客戶上下文
│   ├── order/             # 訂單上下文
│   ├── product/           # 產品上下文
│   ├── inventory/         # 庫存上下文
│   ├── payment/           # 支付上下文
│   ├── delivery/          # 配送上下文
│   ├── promotion/         # 促銷上下文
│   ├── pricing/           # 定價上下文
│   ├── notification/      # 通知上下文
│   └── workflow/          # 工作流上下文
├── application/
│   ├── customer/          # 客戶用例
│   ├── order/             # 訂單用例
│   └── ...                # 其他用例
└── infrastructure/
    ├── persistence/       # 持久化實現
    └── messaging/         # 訊息處理
```

#### 2. 聚合設計原則

每個限界上下文包含一個或多個聚合：

- **小聚合**: 每個聚合專注於單一業務概念
- **一致性邊界**: 聚合內部保持強一致性
- **事件驅動**: 聚合間通過領域事件通信

#### 3. 資料一致性策略

- **聚合內**: 強一致性（ACID 事務）
- **聚合間**: 最終一致性（領域事件）
- **上下文間**: 最終一致性（集成事件）

## 結果

### 正面影響

#### 1. **清晰的業務邊界**

- 每個上下文對應明確的業務能力
- 減少跨團隊的溝通複雜度
- 支援獨立的業務決策

#### 2. **技術自主性**

- 每個上下文可以選擇適合的技術棧
- 支援獨立部署和擴展
- 降低技術債務的傳播

#### 3. **團隊組織對齊**

- 上下文邊界與團隊邊界對齊
- 支援 Conway's Law 的正面應用
- 提高團隊自主性和責任感

#### 4. **可測試性提升**

- 每個上下文可以獨立測試
- 減少測試間的相互依賴
- 提高測試執行速度

### 量化指標

- **上下文數量**: 10 個
- **聚合數量**: 15 個
- **跨上下文依賴**: 12 個（通過事件）
- **直接依賴**: 0 個
- **測試隔離度**: 95%

### 負面影響與緩解措施

#### 1. **複雜性增加**

- **問題**: 多個上下文增加系統複雜性
- **緩解**: 提供清晰的上下文映射文檔和工具

#### 2. **資料一致性挑戰**

- **問題**: 最終一致性可能導致暫時的資料不一致
- **緩解**: 實現補償機制和監控工具

#### 3. **跨上下文查詢困難**

- **問題**: 需要跨多個上下文的查詢變得複雜
- **緩解**: 實現 CQRS 讀取模型和資料投影

## 實現細節

### 1. 上下文介面定義

```java
// 客戶上下文的公開介面
public interface CustomerService {
    Customer findById(CustomerId id);
    void upgradeToVip(CustomerId id);
    LoyaltyPoints getLoyaltyPoints(CustomerId id);
}

// 訂單上下文的公開介面
public interface OrderService {
    Order createOrder(CreateOrderCommand command);
    void cancelOrder(OrderId orderId);
    OrderStatus getOrderStatus(OrderId orderId);
}
```

### 2. 領域事件定義

```java
// 客戶上下文發布的事件
public record CustomerUpgradedToVipEvent(
    CustomerId customerId,
    MembershipLevel newLevel,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}

// 訂單上下文發布的事件
public record OrderCreatedEvent(
    OrderId orderId,
    CustomerId customerId,
    List<OrderItem> items,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}
```

### 3. 事件處理器

```java
// 促銷上下文處理客戶升級事件
@Component
public class CustomerUpgradedEventHandler {
    
    @EventHandler
    public void handle(CustomerUpgradedToVipEvent event) {
        // 為新 VIP 客戶創建專屬優惠券
        promotionService.createVipWelcomeCoupon(event.customerId());
    }
}
```

### 4. 防腐層實現

```java
// 支付上下文的防腐層
@Component
public class PaymentGatewayAdapter {
    
    public PaymentResult processPayment(PaymentRequest request) {
        // 將內部支付請求轉換為外部 API 格式
        ExternalPaymentRequest externalRequest = mapToExternal(request);
        
        // 調用外部支付 API
        ExternalPaymentResponse response = externalPaymentApi.process(externalRequest);
        
        // 將外部響應轉換為內部格式
        return mapToInternal(response);
    }
}
```

## 驗證與測試

### 1. 上下文邊界測試

```java
@ArchTest
static final ArchRule contexts_should_not_have_cyclic_dependencies =
    slices()
        .matching("..domain.(*)..")
        .should().beFreeOfCycles();

@ArchTest
static final ArchRule contexts_should_only_communicate_through_events =
    noClasses()
        .that().resideInAPackage("..domain.customer..")
        .should().dependOnClassesThat()
        .resideInAPackage("..domain.order..");
```

### 2. 集成測試策略

```java
@SpringBootTest
class CustomerOrderIntegrationTest {
    
    @Test
    void shouldCreateOrderWhenCustomerExists() {
        // Given: 客戶存在
        CustomerId customerId = createTestCustomer();
        
        // When: 創建訂單
        OrderId orderId = orderService.createOrder(
            new CreateOrderCommand(customerId, orderItems)
        );
        
        // Then: 驗證訂單創建成功
        assertThat(orderService.getOrderStatus(orderId))
            .isEqualTo(OrderStatus.PENDING);
    }
}
```

### 3. 事件流測試

```java
@Test
void shouldTriggerPromotionWhenCustomerUpgraded() {
    // Given: 客戶和事件處理器
    CustomerId customerId = createTestCustomer();
    
    // When: 客戶升級為 VIP
    customerService.upgradeToVip(customerId);
    
    // Then: 應該收到 VIP 歡迎優惠券
    await().atMost(5, SECONDS).untilAsserted(() -> {
        List<Coupon> coupons = promotionService.getCouponsForCustomer(customerId);
        assertThat(coupons).anyMatch(c -> c.getType() == CouponType.VIP_WELCOME);
    });
}
```

## 演進策略

### 1. 微服務演進路徑

當系統需要拆分為微服務時，限界上下文提供了自然的拆分邊界：

```
階段 1: 單體應用 (當前)
├── 所有上下文在同一個部署單元中
└── 通過領域事件進行內部通信

階段 2: 模組化單體
├── 每個上下文作為獨立模組
└── 保持在同一個部署單元中

階段 3: 微服務
├── 核心上下文優先拆分
├── 支援上下文按需拆分
└── 通用上下文最後拆分
```

### 2. 資料庫拆分策略

```
階段 1: 共享資料庫
├── 所有上下文共享同一個資料庫
└── 通過 schema 或 table prefix 進行邏輯分離

階段 2: 資料庫分離
├── 每個上下文擁有獨立的資料庫 schema
└── 跨上下文查詢通過 API 或事件

階段 3: 完全獨立
├── 每個微服務擁有獨立的資料庫實例
└── 完全的資料自主性
```

## 相關決策

- [ADR-001: DDD + 六角形架構基礎](./ADR-001-ddd-hexagonal-architecture.md)
- [ADR-003: 領域事件和 CQRS 實現](./ADR-003-domain-events-cqrs.md)
- \1

## 參考資料

- Domain-Driven Design: Tackling Complexity in the Heart of Software
- Implementing Domain-Driven Design
- Bounded Context Canvas
- Context Mapping

---

**最後更新**: 2024-01-20  
**審核者**: 架構團隊、領域專家  
**下次審查**: 2024-07-20
