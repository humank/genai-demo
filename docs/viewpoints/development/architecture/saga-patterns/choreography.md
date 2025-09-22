# Saga 編舞模式 (Choreography)

## 概述

編舞式 Saga 沒有中央協調器，各個服務通過發布和監聽事件來協調分散式事務。每個服務負責監聽相關事件並決定下一步動作。

## 💃 編舞模式特性

### 優點

- **去中心化**: 沒有單點故障，提高系統可用性
- **鬆耦合**: 服務間通過事件通訊，降低耦合度
- **擴展性**: 新增服務不需要修改現有協調邏輯
- **自治性**: 每個服務自主決定如何回應事件

### 缺點

- **複雜性**: 業務流程分散在多個服務中，難以理解全貌
- **除錯困難**: 沒有集中的控制點，問題追蹤困難
- **循環依賴**: 容易產生事件循環，需要謹慎設計
- **一致性**: 更難保證強一致性

## 🏗️ 實作架構

### 事件驅動協調

```java
// 訂單服務 - 發起 Saga
@Component
public class OrderEventHandler {
    
    private final EventPublisher eventPublisher;
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Order created, starting saga choreography: {}", event.getOrderId());
        
        // 發布庫存預留請求事件
        eventPublisher.publish(InventoryReservationRequested.create(
            event.getOrderId(),
            event.getOrderItems(),
            event.getCustomerId()
        ));
    }
    
    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Order processing completed: {}", event.getOrderId());
        
        // 發送完成通知
        eventPublisher.publish(OrderCompletionNotificationRequested.create(
            event.getOrderId(),
            event.getCustomerId()
        ));
    }
    
    @EventListener
    public void handleSagaFailed(SagaFailedEvent event) {
        log.warn("Saga failed for order: {}, reason: {}", event.getOrderId(), event.getReason());
        
        // 取消訂單
        orderService.cancelOrder(event.getOrderId(), event.getReason());
        
        // 發送取消通知
        eventPublisher.publish(OrderCancellationNotificationRequested.create(
            event.getOrderId(),
            event.getCustomerId(),
            event.getReason()
        ));
    }
}
```

### 庫存服務協調

```java
@Component
public class InventoryEventHandler {
    
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;
    
    @EventListener
    public void handleInventoryReservationRequested(InventoryReservationRequested event) {
        log.info("Processing inventory reservation for order: {}", event.getOrderId());
        
        try {
            InventoryReservationResult result = inventoryService.reserveItems(
                event.getOrderId(),
                event.getOrderItems()
            );
            
            if (result.isSuccess()) {
                // 庫存預留成功，觸發付款處理
                eventPublisher.publish(PaymentRequested.create(
                    event.getOrderId(),
                    event.getCustomerId(),
                    calculateTotalAmount(event.getOrderItems()),
                    result.getReservationId()
                ));
                
                log.info("Inventory reserved successfully for order: {}", event.getOrderId());
                
            } else {
                // 庫存預留失敗，觸發 Saga 失敗
                eventPublisher.publish(SagaFailedEvent.create(
                    event.getOrderId(),
                    "INVENTORY_RESERVATION",
                    result.getFailureReason()
                ));
                
                log.warn("Inventory reservation failed for order: {}, reason: {}", 
                        event.getOrderId(), result.getFailureReason());
            }
            
        } catch (Exception e) {
            log.error("Exception during inventory reservation for order: {}", event.getOrderId(), e);
            
            eventPublisher.publish(SagaFailedEvent.create(
                event.getOrderId(),
                "INVENTORY_RESERVATION",
                e.getMessage()
            ));
        }
    }
    
    @EventListener
    public void handleInventoryCompensationRequested(InventoryCompensationRequested event) {
        log.info("Processing inventory compensation for order: {}", event.getOrderId());
        
        try {
            inventoryService.releaseReservation(event.getReservationId());
            
            eventPublisher.publish(InventoryCompensationCompleted.create(
                event.getOrderId(),
                event.getReservationId()
            ));
            
            log.info("Inventory compensation completed for order: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Inventory compensation failed for order: {}", event.getOrderId(), e);
            
            eventPublisher.publish(InventoryCompensationFailed.create(
                event.getOrderId(),
                event.getReservationId(),
                e.getMessage()
            ));
        }
    }
}
```

### 付款服務協調

```java
@Component
public class PaymentEventHandler {
    
    private final PaymentService paymentService;
    private final EventPublisher eventPublisher;
    
    @EventListener
    public void handlePaymentRequested(PaymentRequested event) {
        log.info("Processing payment for order: {}", event.getOrderId());
        
        try {
            PaymentResult result = paymentService.processPayment(
                event.getOrderId(),
                event.getAmount(),
                event.getPaymentInfo()
            );
            
            if (result.isSuccess()) {
                // 付款成功，觸發配送安排
                eventPublisher.publish(ShippingRequested.create(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getShippingInfo(),
                    result.getPaymentId()
                ));
                
                log.info("Payment processed successfully for order: {}", event.getOrderId());
                
            } else {
                // 付款失敗，觸發補償
                eventPublisher.publish(PaymentFailedEvent.create(
                    event.getOrderId(),
                    result.getFailureReason()
                ));
                
                // 同時觸發庫存補償
                eventPublisher.publish(InventoryCompensationRequested.create(
                    event.getOrderId(),
                    event.getReservationId()
                ));
                
                log.warn("Payment failed for order: {}, reason: {}", 
                        event.getOrderId(), result.getFailureReason());
            }
            
        } catch (Exception e) {
            log.error("Exception during payment processing for order: {}", event.getOrderId(), e);
            
            eventPublisher.publish(PaymentFailedEvent.create(
                event.getOrderId(),
                e.getMessage()
            ));
            
            // 觸發庫存補償
            eventPublisher.publish(InventoryCompensationRequested.create(
                event.getOrderId(),
                event.getReservationId()
            ));
        }
    }
    
    @EventListener
    public void handlePaymentCompensationRequested(PaymentCompensationRequested event) {
        log.info("Processing payment compensation for order: {}", event.getOrderId());
        
        try {
            RefundResult result = paymentService.refundPayment(
                event.getPaymentId(),
                event.getAmount()
            );
            
            if (result.isSuccess()) {
                eventPublisher.publish(PaymentCompensationCompleted.create(
                    event.getOrderId(),
                    event.getPaymentId()
                ));
                
                log.info("Payment compensation completed for order: {}", event.getOrderId());
            } else {
                eventPublisher.publish(PaymentCompensationFailed.create(
                    event.getOrderId(),
                    event.getPaymentId(),
                    result.getFailureReason()
                ));
                
                log.error("Payment compensation failed for order: {}, reason: {}", 
                         event.getOrderId(), result.getFailureReason());
            }
            
        } catch (Exception e) {
            log.error("Exception during payment compensation for order: {}", event.getOrderId(), e);
            
            eventPublisher.publish(PaymentCompensationFailed.create(
                event.getOrderId(),
                event.getPaymentId(),
                e.getMessage()
            ));
        }
    }
}
```

## 🔄 事件設計模式

### 1. 事件命名約定

```java
// 請求類事件 - 觸發動作
public record InventoryReservationRequested(
    String orderId,
    List<OrderItem> orderItems,
    String customerId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}

// 結果類事件 - 動作完成
public record InventoryReserved(
    String orderId,
    String reservationId,
    List<ReservedItem> reservedItems,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}

// 失敗類事件 - 動作失敗
public record InventoryReservationFailed(
    String orderId,
    String reason,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}

// 補償類事件 - 補償動作
public record InventoryCompensationRequested(
    String orderId,
    String reservationId,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent {}
```

### 2. 事件路由策略

```java
@Component
public class SagaEventRouter {
    
    private final Map<String, List<EventHandler>> eventHandlers = new HashMap<>();
    
    @PostConstruct
    public void initializeRoutes() {
        // 註冊事件處理器
        registerHandler("InventoryReservationRequested", inventoryEventHandler);
        registerHandler("PaymentRequested", paymentEventHandler);
        registerHandler("ShippingRequested", shippingEventHandler);
        
        // 註冊補償事件處理器
        registerHandler("InventoryCompensationRequested", inventoryEventHandler);
        registerHandler("PaymentCompensationRequested", paymentEventHandler);
    }
    
    @EventListener
    public void routeEvent(DomainEvent event) {
        String eventType = event.getEventType();
        List<EventHandler> handlers = eventHandlers.get(eventType);
        
        if (handlers != null) {
            handlers.forEach(handler -> {
                try {
                    handler.handle(event);
                } catch (Exception e) {
                    log.error("Event handler failed for event: {}", eventType, e);
                    // 可能需要觸發補償或重試
                }
            });
        } else {
            log.warn("No handler found for event type: {}", eventType);
        }
    }
}
```

## 🧪 測試策略

### 事件流程測試

```java
@SpringBootTest
@ActiveProfiles("test")
class OrderSagaChoreographyTest {
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @MockBean
    private InventoryService inventoryService;
    
    @MockBean
    private PaymentService paymentService;
    
    @Test
    void should_complete_order_saga_through_choreography() {
        // Given
        String orderId = "ORDER-001";
        OrderCreatedEvent orderEvent = createOrderCreatedEvent(orderId);
        
        when(inventoryService.reserveItems(any(), any()))
            .thenReturn(InventoryReservationResult.success());
        when(paymentService.processPayment(any(), any(), any()))
            .thenReturn(PaymentResult.success());
        
        // When
        eventPublisher.publish(orderEvent);
        
        // Then - 等待異步處理完成
        await().atMost(Duration.ofSeconds(5))
            .until(() -> {
                Optional<Order> order = orderRepository.findById(orderId);
                return order.isPresent() && order.get().getStatus() == OrderStatus.COMPLETED;
            });
        
        // 驗證最終狀態
        Order completedOrder = orderRepository.findById(orderId).get();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }
    
    @Test
    void should_compensate_when_payment_fails() {
        // Given
        String orderId = "ORDER-002";
        OrderCreatedEvent orderEvent = createOrderCreatedEvent(orderId);
        
        when(inventoryService.reserveItems(any(), any()))
            .thenReturn(InventoryReservationResult.success());
        when(paymentService.processPayment(any(), any(), any()))
            .thenReturn(PaymentResult.failure("Insufficient funds"));
        
        // When
        eventPublisher.publish(orderEvent);
        
        // Then
        await().atMost(Duration.ofSeconds(5))
            .until(() -> {
                Optional<Order> order = orderRepository.findById(orderId);
                return order.isPresent() && order.get().getStatus() == OrderStatus.CANCELLED;
            });
        
        // 驗證補償動作被執行
        verify(inventoryService).releaseReservation(any());
        
        Order cancelledOrder = orderRepository.findById(orderId).get();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
```

## 🎯 最佳實踐

### 1. 事件設計原則

- **語義清晰**: 事件名稱應該清楚表達發生了什麼
- **資料完整**: 事件應包含處理所需的所有資料
- **冪等性**: 重複處理同一事件應該是安全的
- **版本相容**: 支援事件結構的演進

### 2. 錯誤處理策略

- **重試機制**: 對暫時性錯誤進行重試
- **死信佇列**: 處理無法重試的失敗事件
- **補償觸發**: 自動觸發相關的補償動作
- **人工介入**: 提供人工處理機制

### 3. 監控和追蹤

- **事件追蹤**: 記錄事件的發布和處理
- **流程監控**: 監控整個 Saga 的執行狀態
- **效能指標**: 追蹤各步驟的執行時間
- **錯誤統計**: 統計各種錯誤的發生頻率

## 🔗 相關資源

- [編排模式對比](orchestration.md)
- [事件設計指南](../../domain-events.md)
- [Saga 協調機制](saga-coordination.md)

---

**最後更新**: 2025年1月21日  
**維護者**: Architecture Team  
**版本**: 1.0