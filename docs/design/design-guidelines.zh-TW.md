# 設計指南

## Tell, Don't Ask 原則

### 起源與歷史
"Tell, Don't Ask" 原則最早由 Alec Sharp 在 1997 年提出，後來在 Andy Hunt 和 Dave Thomas 的《程序員修煉之道》(The Pragmatic Programmer) 一書中得到廣泛傳播。這個原則強調應該告訴對象要做什麼，而不是詢問對象的狀態後再決定做什麼。

### 核心概念
- 對象應該負責其內部狀態的處理
- 調用者不應該基於對象的內部狀態做出決策
- 封裝不僅是隱藏數據，更是隱藏行為

### 不好的設計示例
```java
// 違反 Tell, Don't Ask
if (order.getStatus() == OrderStatus.PENDING) {
    order.setStatus(OrderStatus.PROCESSING);
    // 處理訂單...
}
```

### 好的設計示例
```java
// 遵循 Tell, Don't Ask
order.process();  // 讓訂單自己處理狀態轉換
```

### 在我們的專案中的應用

1. 訂單處理
```java
// 好的設計：直接告訴訂單處理自己
order.process();

// 不好的設計：檢查狀態後再決定做什麼
if (order.getStatus() == OrderStatus.CREATED) {
    order.submit();
}
```

2. 庫存管理
```java
// 好的設計：讓庫存自己處理預留邏輯
ReservationId reservationId = inventory.reserve(orderId, quantity);

// 不好的設計：檢查庫存後再決定做什麼
if (inventory.getAvailableQuantity() >= quantity) {
    inventory.setAvailableQuantity(inventory.getAvailableQuantity() - quantity);
    // 創建預留...
}
```

3. 配送狀態管理
```java
// 好的設計：讓配送對象處理狀態轉換
delivery.markAsDelivered();

// 不好的設計：外部檢查狀態並設置
if (delivery.getStatus() == DeliveryStatus.IN_TRANSIT) {
    delivery.setStatus(DeliveryStatus.DELIVERED);
    delivery.setUpdatedAt(LocalDateTime.now());
}
```

## 其他重要的設計思考點

### 1. 單一職責原則 (SRP)
- 每個類別都應該有一個明確的職責
- 變化的原因應該只有一個
- 專案中的良好實踐：
  - `Order` 聚合根專注於訂單生命週期管理
  - `Inventory` 聚合根專注於庫存管理
  - `Delivery` 聚合根專注於配送流程

### 2. 分層架構
- 表現層：處理 HTTP 請求/響應
- 應用層：協調不同服務，處理事務
- 領域層：實現核心業務邏輯
- 基礎設施層：提供技術支持
- 專案中的實踐：
  - 領域層中的聚合根（如 `Order`、`Inventory`）不依賴於基礎設施
  - 應用服務協調領域對象完成用例
  - 基礎設施層實現領域層定義的接口

### 3. 關注點分離
- 業務邏輯與技術細節分離
- 領域邏輯與基礎設施關注點分離
- 專案中的實踐：
  - 使用 `OrderProcessingSaga` 協調跨聚合根的操作
  - 領域事件用於解耦不同上下文

### 4. 依賴倒置原則 (DIP)
- 高層模組不應依賴低層模組
- 抽象不應依賴細節
- 專案中的實踐：
  - 使用 `Repository` 接口隔離領域層和持久化實現
  - 使用 `DomainEvent` 接口而不是具體的事件類別

### 5. 封裝
- 隱藏實現細節
- 提供有意義的介面
- 控制變化的影響範圍
- 專案中的實踐：
  - `Money` 值對象封裝了金額和貨幣，提供了安全的操作方法
  - `OrderStatus` 封裝了狀態轉換規則

## 在領域驅動設計(DDD)中的應用

### Tell, Don't Ask 與界限上下文

在處理跨界限上下文的溝通時，Tell, Don't Ask 原則特別重要：

1. 事件驅動通信
   ```java
   // 不好的設計
   if (order.getStatus() == OrderStatus.CONFIRMED) {
       Payment payment = new Payment(order.getId(), order.getTotalAmount());
       paymentService.process(payment);
   }

   // 好的設計
   order.confirm();  // 內部發布 OrderConfirmedEvent
   // PaymentService 訂閱並處理 OrderConfirmedEvent
   ```

2. 界限上下文的自治
   - 每個上下文負責自己的決策
   - 通過事件通知其他上下文
   - 避免上下文間的直接查詢
   - 專案中的實踐：
     - 訂單和支付是獨立的上下文
     - 通過 `PaymentRequestedEvent` 進行通信

3. 防腐層的應用
   ```java
   // 不好的設計：直接暴露外部系統的細節
   ExternalPaymentSystem.PaymentStatus status = externalSystem.getPaymentStatus(id);
   if (status == ExternalPaymentSystem.PaymentStatus.SUCCESS) {
       // 處理邏輯
   }

   // 好的設計：使用防腐層封裝外部系統
   paymentAntiCorruptionLayer.processPayment(payment);
   ```

### 專案中的 DDD 戰術模式應用

#### 1. 聚合根
- 維護自身的業務規則和不變條件
- 專案中的實踐：
  - `Order` 聚合根確保訂單狀態轉換的正確性
  - `Inventory` 聚合根確保庫存數量的一致性
  - `Delivery` 聚合根管理配送狀態轉換

#### 2. 值對象
- 描述領域中的概念，無身份標識
- 不可變性確保線程安全
- 專案中的實踐：
  - `Money` 值對象封裝金額和貨幣
  - `OrderId`、`CustomerId` 等標識符
  - `OrderStatus`、`DeliveryStatus` 等狀態枚舉

#### 3. 領域事件
- 表達領域中發生的重要事件
- 解耦不同界限上下文
- 專案中的實踐：
  - `OrderCreatedEvent`
  - `OrderItemAddedEvent`
  - `PaymentRequestedEvent`

#### 4. 領域服務
- 處理跨聚合根的業務邏輯
- 無狀態
- 專案中的實踐：
  - `DomainEventPublisherService` 處理事件發布

#### 5. 規格模式
- 封裝複雜的業務規則
- 可組合的條件表達式
- 專案中的實踐：
  - `Specification` 接口及其實現
  - `AndSpecification`、`OrSpecification` 等組合規格

## 防禦性編程實踐

專案中的防禦性編程實踐：

### 1. 前置條件檢查
```java
// Order 聚合根中的參數驗證
public Order(OrderId orderId, CustomerId customerId, String shippingAddress) {
    Preconditions.requireNonNull(orderId, "訂單ID不能為空");
    Preconditions.requireNonNull(customerId, "客戶ID不能為空");
    Preconditions.requireNonEmpty(shippingAddress, "配送地址不能為空");
    // ...
}
```

### 2. 狀態轉換保護
```java
// 訂單狀態轉換保護
public void confirm() {
    // 檢查狀態轉換
    if (!status.canTransitionTo(OrderStatus.CONFIRMED)) {
        throw new IllegalStateException("Cannot confirm an order in " + status + " state");
    }
    // 更新狀態
    status = OrderStatus.CONFIRMED;
    updatedAt = LocalDateTime.now();
}
```

### 3. 不可變值對象
```java
// Money 值對象的不可變設計
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    // 不提供修改器，只提供返回新實例的操作方法
    public Money add(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(money.amount), this.currency);
    }
}
```

### 4. 業務規則封裝
```java
// 庫存聚合根中的業務規則
public ReservationId reserve(UUID orderId, int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("預留數量必須大於零");
    }
    
    if (!isSufficient(quantity)) {
        throw new IllegalStateException("庫存不足，無法預留");
    }
    
    // 業務邏輯...
}
```

## 設計模式應用

專案中應用的設計模式：

### 1. 工廠方法模式
```java
// OrderId 值對象中的工廠方法
public static OrderId generate() {
    return new OrderId(UUID.randomUUID());
}

public static OrderId of(String id) {
    return new OrderId(UUID.fromString(id));
}
```

### 2. 策略模式
通過不同的規格實現提供可互換的業務規則驗證策略。

### 3. 觀察者模式
通過領域事件實現觀察者模式，解耦事件發布者和訂閱者。

### 4. 命令模式
Saga 模式中的命令和補償操作實現了命令模式的思想。

## 改進建議

基於專案代碼分析，以下是一些改進建議：

### 1. 增強錯誤處理
- 引入專門的業務異常類型，如 `InsufficientInventoryException`
- 在應用層統一處理異常，轉換為適當的響應

### 2. 增強領域事件機制
- 考慮使用事件溯源模式記錄聚合根的狀態變化
- 實現事件持久化，支持事件重播

### 3. 優化 Saga 實現
- 考慮使用狀態機模式管理 Saga 的狀態轉換
- 增強補償邏輯的健壯性

### 4. 增強測試覆蓋
- 增加針對邊界條件的單元測試
- 增加針對並發場景的測試

## 參考資源

1. 《程序員修煉之道》(The Pragmatic Programmer) - Andy Hunt & Dave Thomas
2. 《重構》(Refactoring) - Martin Fowler
3. 《領域驅動設計》(Domain-Driven Design) - Eric Evans
4. 《實現領域驅動設計》(Implementing Domain-Driven Design) - Vaughn Vernon
5. [Tell, Don't Ask by Alec Sharp](http://pragprog.com/articles/tell-dont-ask)
