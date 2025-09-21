
# Design

This document描述了系統中Domain Event的發佈和處理機制，包括相關組件的設計、交互流程以及測試Policy。

## Design

系統採用了基於Spring框架的自定義Domain Event處理機制，主要特點包括：

1. **Domain Event發佈**：通過`DomainEventPublisherAdapter`將Domain Event轉換為Spring應用事件
2. **事件訂閱**：使用自定義的`@EventSubscriber`註解標記事件處理方法
3. **事件路由**：通過`DomainEventSubscriptionManager`在運行時掃描和註冊事件處理器
4. **降級機制**：當`AggregateLifecycle`不可用時，自動降級到`DomainEventPublisherService`

## UML圖表說明

### 類圖 (Class Diagram)

類圖展示了Domain Event處理機制的核心組件及其關係：

- **Domain Layer**：
  - `DomainEvent`：Domain Event介面
  - `EventHandler`：事件處理器介面
  - `EventSubscriber`：事件訂閱註解
  - `DomainEventBus`：Domain Event總線
  - `DomainEventPublisherService`：Domain Event發佈服務
  - 具體事件處理器（如`OrderEventHandler`）

- **Infrastructure Layer**：
  - `DomainEventPublisherAdapter`：Domain Event發佈Adapter
  - `DomainEventSubscriptionManager`：事件訂閱管理器

![Domain Event處理 - 類圖](Domain%20Event%20Handling%20-%20Class%20Diagram.svg)

### 序列圖 (Sequence Diagram)

序列圖展示了Domain Event從發佈到處理的完整流程：

1. **初始化階段**：`DomainEventSubscriptionManager`掃描並註冊事件訂閱者
2. **事件發佈流程**：從Aggregate Root到Spring事件發佈器的事件傳遞路徑
3. **事件處理流程**：從Spring事件到具體事件處理器的調用過程

![Domain Event處理 - 序列圖](Domain%20Event%20Handling%20-%20Sequence%20Diagram.svg)

### 組件圖 (Component Diagram)

組件圖展示了Domain Event處理機制的主要組件及其依賴關係：

- Domain Layer組件與Infrastructure Layer組件的交互
- 與Spring框架的集成點
- 各組件之間的依賴關係

![Domain Event處理 - 組件圖](Domain%20Event%20Handling%20-%20Component%20Diagram.svg)

### 事件流程圖 (Event Flow Diagram)

事件流程圖以活動圖的形式展示了Domain Event的完整生命週期：

- 事件發佈的觸發點
- 降級處理機制
- 事件路由和處理的決策點
- 錯誤處理Policy

![Domain Event處理 - 事件流程圖](Domain%20Event%20Handling%20-%20Event%20Flow%20Diagram.svg)

## Testing

為驗證Domain Event處理機制的正確性，recommendations採用以下測試Policy：

### Testing

- 測試各個組件的獨立功能
- 使用模擬對象隔離依賴

### Testing

集成測試應放置在`/Users/yikaikao/git/genai-demo/app/src/test/java/solid/humank/genaidemo/integration/event/`目錄下，主要測試：

1. **事件發佈測試**：驗證事件是否正確發佈
   ```java
   @Test
   public void testEventPublishing() {
       // 直接發佈事件
       OrderCreatedEvent event = new OrderCreatedEvent("test-order", "test-customer", BigDecimal.ZERO);
       eventPublisherAdapter.publish(event);
       
       // 驗證事件處理
       verify(orderEventHandler, timeout(1000)).handleOrderCreated(eq(event));
   }
   ```

2. **端到端事件流測試**：驗證從業務操作到事件處理的完整流程
   ```java
   @Test
   public void testEndToEndEventFlow() {
       // 執行業務操作
       String orderId = orderService.createOrder("customer-123", "台北市信義區");
       
       // 驗證事件處理器被調用
       verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
   }
   ```

## Best Practices

1. **事件設計**：
   - 事件應該是不可變的
   - 事件應包含足夠的上下文信息
   - 事件名稱應使用過去時態（如`OrderCreated`而非`CreateOrder`）

2. **事件處理**：
   - 事件處理器應該是冪等的
   - 避免在事件處理器中執行長時間運行的操作
   - 考慮使用異步處理機制處理耗時操作

3. **測試**：
   - 使用`@SpyBean`Monitoring事件處理器
   - 添加超時參數處理異步事件
   - 驗證事件處理後的系統狀態變化
