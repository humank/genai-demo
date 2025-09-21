
# Design

## 狀態

**已接受** - 2024-01-20

## 背景

GenAI Demo 電商平台需要清晰的領域邊界來管理複雜性並促進團隊協作。基於用戶故事分析和業務需求，我們需要識別和設計適當的限界上下文，以反映業務領域結構。

### Requirements

從 BDD 功能文件和用戶故事中，我們識別出以下關鍵業務能力：

#### Customer管理 (Customer Management)

- **用戶故事**: "作為Customer，我想管理我的個人資料和會員等級，以便獲得個人化的優惠"
- **關鍵場景**: 註冊、個人資料更新、會員升級、紅利點數管理

#### 訂單處理 (Order Processing)

- **用戶故事**: "作為Customer，我想下訂單並Tracing訂單，以便高效地購買產品"
- **關鍵場景**: 訂單創建、項目管理、狀態Tracing、取消訂單

#### 產品目錄 (Product Catalog)

- **用戶故事**: "作為Customer，我想瀏覽產品並查看detailed information，以便做出明智的購買決策"
- **關鍵場景**: 產品瀏覽、搜尋、篩選、會員專屬產品

#### 庫存管理 (Inventory Management)

- **用戶故事**: "作為商家，我想管理產品庫存，以便確保產品Availability"
- **關鍵場景**: 庫存Tracing、補貨、預留、釋放

#### 支付處理 (Payment Processing)

- **用戶故事**: "作為Customer，我想安全地支付訂單，以便完成購買"
- **關鍵場景**: 支付處理、退款、支付方式管理

#### 物流配送 (Delivery Management)

- **用戶故事**: "作為Customer，我想Tracing我的配送狀態，以便了解訂單進度"
- **關鍵場景**: 配送安排、狀態更新、配送Tracing

#### 促銷活動 (Promotion Management)

- **用戶故事**: "作為Customer，我想使用優惠券和參與促銷活動，以便獲得折扣"
- **關鍵場景**: 優惠券管理、促銷規則、折扣計算

#### 定價管理 (Pricing Management)

- **用戶故事**: "作為商家，我想設定靈活的定價Policy，以便優化收益"
- **關鍵場景**: 價格計算、佣金管理、動態定價

#### 通知服務 (Notification Service)

- **用戶故事**: "作為Customer，我想收到重要的訂單和促銷通知，以便及時了解資訊"
- **關鍵場景**: 通知發送、模板管理、通知偏好

#### 工作流管理 (Workflow Management)

- **用戶故事**: "作為系統，我需要協調複雜的業務流程，以便確保操作的一致性"
- **關鍵場景**: 流程編排、狀態管理、異常處理

## 決策

我們決定建立 **10 個限界上下文**，基於業務能力和團隊結構進行劃分。

### Context MappingPolicy

#### 核心上下文 (Core Contexts)

這些上下文包含核心業務邏輯，是系統的競爭優勢所在：

1. **Customer Context** - CustomerAggregate
   - Customer資料管理
   - 會員等級和紅利點數
   - Customer偏好設定

2. **Order Context** - 訂單Aggregate
   - 訂單生命週期管理
   - 訂單項目管理
   - 訂單狀態Tracing

3. **Product Context** - 產品Aggregate
   - 產品目錄管理
   - 產品資訊維護
   - 產品分類和標籤

4. **Inventory Context** - 庫存Aggregate
   - 庫存水準Tracing
   - 庫存預留和釋放
   - 庫存補充管理

#### 支援上下文 (Supporting Contexts)

這些上下文支援核心業務流程：

5. **Payment Context** - 支付Aggregate
   - 支付處理
   - 支付方式管理
   - 退款處理

6. **Delivery Context** - 配送Aggregate
   - 配送安排
   - 配送狀態Tracing
   - 配送商管理

7. **Promotion Context** - 促銷Aggregate
   - 優惠券管理
   - 促銷規則引擎
   - 折扣計算

8. **Pricing Context** - 定價Aggregate
   - 價格計算引擎
   - 佣金管理
   - 定價Policy

#### 通用上下文 (Generic Contexts)

這些上下文提供通用服務：

9. **Notification Context** - 通知Aggregate
   - 通知發送
   - 通知模板管理
   - 通知偏好

10. **Workflow Context** - 工作流Aggregate
    - 業務流程編排
    - 狀態機管理
    - 流程Monitoring

### 上下文關係映射

#### 1. 夥伴關係 (Partnership)

- **Customer ↔ Order**: Customer和訂單緊密協作
- **Order ↔ Inventory**: 訂單和庫存需要同步協調

#### 2. Customer-供應商 (Customer-Supplier)

- **Order → Payment**: 訂單驅動支付處理
- **Order → Delivery**: 訂單觸發配送安排
- **Customer → Notification**: Customer事件觸發通知

#### 3. Conformist (Conformist)

- **Promotion → Pricing**: 促銷遵循定價規則
- **All Contexts → Workflow**: 所有上下文遵循工作流規範

#### 4. Anti-Corruption Layer (Anti-Corruption Layer)

- **Payment Context**: 與外部支付系統整合時使用Anti-Corruption Layer
- **Delivery Context**: 與第三方物流系統整合時使用Anti-Corruption Layer

### 實現Policy

#### Design

```
solid.humank.genaidemo/
├── domain/
│   ├── customer/          # Customer上下文
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
│   ├── customer/          # Customer用例
│   ├── order/             # 訂單用例
│   └── ...                # 其他用例
└── infrastructure/
    ├── persistence/       # 持久化實現
    └── messaging/         # 訊息處理
```

#### Design

每個限界上下文包含一個或多個Aggregate：

- **小Aggregate**: 每個Aggregate專注於單一業務概念
- **一致性邊界**: Aggregate內部保持強一致性
- **事件驅動**: Aggregate間通過Domain Event通信

#### 3. 資料一致性Policy

- **Aggregate內**: 強一致性（ACID 事務）
- **Aggregate間**: 最終一致性（Domain Event）
- **上下文間**: 最終一致性（集成事件）

## 結果

### 正面影響

#### 1. **清晰的業務邊界**

- 每個上下文對應明確的業務能力
- 減少跨團隊的溝通複雜度
- 支援獨立的業務決策

#### 2. **技術自主性**

- 每個上下文可以選擇適合的技術棧
- 支援獨立Deployment和擴展
- 降低Technical Debt的傳播

#### 3. **團隊組織對齊**

- 上下文邊界與團隊邊界對齊
- 支援 Conway's Law 的正面應用
- 提高團隊自主性和責任感

#### Testing

- 每個上下文可以獨立測試
- 減少測試間的相互依賴
- 提高測試執行速度

### 量化Metrics

- **上下文數量**: 10 個
- **Aggregate數量**: 15 個
- **跨上下文依賴**: 12 個（通過事件）
- **直接依賴**: 0 個
- **測試隔離度**: 95%

### 負面影響與緩解措施

#### 1. **複雜性增加**

- **問題**: 多個上下文增加系統複雜性
- **緩解**: 提供清晰的Context Mapping文檔和工具

#### 2. **資料一致性挑戰**

- **問題**: 最終一致性可能導致暫時的資料不一致
- **緩解**: 實現補償機制和Monitoring工具

#### 3. **跨上下文查詢困難**

- **問題**: 需要跨多個上下文的查詢變得複雜
- **緩解**: 實現 Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS)) 讀取模型和資料投影

## 實現細節

### 1. 上下文介面定義

```java
// Customer上下文的公開介面
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

### 2. Domain Event定義

```java
// Customer上下文發布的事件
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
// 促銷上下文處理Customer升級事件
@Component
public class CustomerUpgradedEventHandler {
    
    @EventHandler
    public void handle(CustomerUpgradedToVipEvent event) {
        // 為新 VIP Customer創建專屬優惠券
        promotionService.createVipWelcomeCoupon(event.customerId());
    }
}
```

### 4. Anti-Corruption Layer實現

```java
// 支付上下文的Anti-Corruption Layer
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

## Testing

### Testing

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

### Testing

```java
@SpringBootTest
class CustomerOrderIntegrationTest {
    
    @Test
    void shouldCreateOrderWhenCustomerExists() {
        // Given: Customer存在
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

### Testing

```java
@Test
void shouldTriggerPromotionWhenCustomerUpgraded() {
    // Given: Customer和事件處理器
    CustomerId customerId = createTestCustomer();
    
    // When: Customer升級為 VIP
    customerService.upgradeToVip(customerId);
    
    // Then: 應該收到 VIP 歡迎優惠券
    await().atMost(5, SECONDS).untilAsserted(() -> {
        List<Coupon> coupons = promotionService.getCouponsForCustomer(customerId);
        assertThat(coupons).anyMatch(c -> c.getType() == CouponType.VIP_WELCOME);
    });
}
```

## 演進Policy

### 1. 微服務Evolution Path

當系統需要拆分為微服務時，限界上下文提供了自然的拆分邊界：

```
階段 1: 單體應用 (當前)
├── 所有上下文在同一個Deployment單元中
└── 通過Domain Event進行內部通信

階段 2: 模組化單體
├── 每個上下文作為獨立模組
└── 保持在同一個Deployment單元中

階段 3: 微服務
├── 核心上下文優先拆分
├── 支援上下文按需拆分
└── 通用上下文最後拆分
```

### 2. Repository拆分Policy

```
階段 1: 共享Repository
├── 所有上下文共享同一個Repository
└── 通過 schema 或 table prefix 進行邏輯分離

階段 2: Repository分離
├── 每個上下文擁有獨立的Repository schema
└── 跨上下文查詢通過 API 或事件

階段 3: 完全獨立
├── 每個微服務擁有獨立的Repository實例
└── 完全的資料自主性
```

## 相關決策

- [ADR-001: DDD + Hexagonal Architecture基礎](./ADR-001-ddd-hexagonal-architecture.md)
- [ADR-003: Domain Event和 Command Query Responsibility Segregation (Command Query Responsibility Segregation (CQRS)) 實現](./ADR-003-domain-events-cqrs.md)
- [ADR-009: MSK vs EventBridge 事件串流平台](./ADR-009-event-streaming-platform.md)

## Reference

- [Domain-Driven Design: Tackling Complexity in the Heart of Software](https://www.amazon.com/Domain-Driven-Design-Tackling-Complexity-Software/dp/0321125215)
- [Implementing Domain-Driven Design](https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577)
- [Bounded Context Canvas](https://github.com/ddd-crew/bounded-context-canvas)
- [Context Mapping](https://www.infoq.com/articles/ddd-contextmapping/)

---

**最後更新**: 2024-01-20  
**審核者**: 架構團隊、領域專家  
**下次審查**: 2024-07-20
