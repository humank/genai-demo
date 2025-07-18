# 共享內核重構方案

## 當前問題
- `domain/common` 包含太多共享代碼
- 服務間會產生強耦合
- 版本管理困難

## 重構方案

### 1. 拆分共享內核

#### A. 核心值對象庫 (core-valueobjects)
```java
// 僅包含真正通用的值對象
- Money
- Currency  
- CustomerId (作為通用標識符)
- 基礎 DomainEvent 接口
```

#### B. 各服務專用值對象
```java
// Order Service 專用
- OrderId
- OrderStatus
- OrderItem

// Payment Service 專用  
- PaymentId
- PaymentStatus
- PaymentMethod

// Inventory Service 專用
- ProductId
- InventoryLevel
- ReservationId
```

### 2. 事件契約庫 (event-contracts)
```java
// 跨服務事件定義
public class OrderCreatedEvent {
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    // 只包含其他服務需要的數據
}

public class PaymentProcessedEvent {
    private String paymentId;
    private String orderId;
    private PaymentStatus status;
    private BigDecimal amount;
}
```

### 3. 服務間 API 契約
```java
// 使用 OpenAPI 規範定義
// order-service-api.yaml
paths:
  /orders:
    post:
      requestBody:
        $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          $ref: '#/components/schemas/OrderResponse'
```

## 實施建議

1. **漸進式重構**：先建立新的共享庫，再逐步遷移
2. **版本管理**：使用語義化版本控制
3. **向後兼容**：保持 API 契約的向後兼容性
4. **文檔化**：完整的 API 文檔和變更日誌