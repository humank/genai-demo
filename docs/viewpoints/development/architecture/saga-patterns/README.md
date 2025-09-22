# Saga 模式總覽

## 概述

Saga 模式是一種管理分散式事務的設計模式，通過將長時間運行的事務分解為一系列較小的本地事務來確保資料一致性。本指南詳細說明如何在專案中實作和使用 Saga 模式。

## 🔄 Saga 模式基本概念

### 什麼是 Saga？

Saga 是一個長時間運行的事務，由一系列本地事務組成。每個本地事務都有對應的補償動作，當 Saga 失敗時，已執行的本地事務會通過補償動作進行回滾。

### 核心特性

- **原子性**: 整個 Saga 要麼全部成功，要麼全部回滾
- **一致性**: 通過補償動作維護資料一致性
- **隔離性**: 中間狀態對外部可見，需要謹慎設計
- **持久性**: 每個步驟的狀態都會持久化

## 🏗️ Saga 實作模式

### 1. 編排式 Saga (Orchestration)

中央協調器負責管理整個 Saga 的執行流程：

```java
@Component
public class OrderProcessingSaga {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    
    @SagaOrchestrationStart
    public void processOrder(OrderCreatedEvent event) {
        SagaTransaction saga = SagaTransaction.builder()
            .sagaId(event.getOrderId())
            .sagaType("OrderProcessing")
            .build();
            
        // 步驟 1: 預留庫存
        saga.addStep(
            () -> inventoryService.reserveItems(event.getOrderItems()),
            () -> inventoryService.releaseReservation(event.getOrderId())
        );
        
        // 步驟 2: 處理付款
        saga.addStep(
            () -> paymentService.processPayment(event.getPaymentInfo()),
            () -> paymentService.refundPayment(event.getPaymentId())
        );
        
        // 步驟 3: 安排配送
        saga.addStep(
            () -> shippingService.scheduleShipping(event.getShippingInfo()),
            () -> shippingService.cancelShipping(event.getShippingId())
        );
        
        sagaManager.execute(saga);
    }
}
```

### 2. 編舞式 Saga (Choreography)

各個服務通過事件進行協調，沒有中央協調器：

```java
@Component
public class OrderEventHandler {
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 發布庫存預留事件
        eventPublisher.publish(InventoryReservationRequested.create(
            event.getOrderId(),
            event.getOrderItems()
        ));
    }
}

@Component
public class InventoryEventHandler {
    
    @EventListener
    public void handleInventoryReservationRequested(InventoryReservationRequested event) {
        try {
            inventoryService.reserveItems(event.getOrderItems());
            
            // 成功後發布付款請求事件
            eventPublisher.publish(PaymentRequested.create(
                event.getOrderId(),
                event.getPaymentInfo()
            ));
        } catch (InsufficientInventoryException e) {
            // 失敗時發布補償事件
            eventPublisher.publish(OrderCancelled.create(
                event.getOrderId(),
                "Insufficient inventory"
            ));
        }
    }
}
```

## 📋 專案中的 Saga 實作

### OrderProcessingSaga

處理訂單的完整生命週期：

```java
@Component
@Slf4j
public class OrderProcessingSaga {
    
    @TransactionalEventListener
    @Order(1)
    public void on(OrderCreatedEvent event) {
        log.info("Starting order processing saga for order: {}", event.getOrderId());
        
        // 步驟 1: 驗證訂單
        validateOrder(event);
        
        // 步驟 2: 預留庫存
        reserveInventory(event);
    }
    
    @TransactionalEventListener
    @Order(2)
    public void on(InventoryReservedEvent event) {
        log.info("Inventory reserved for order: {}, proceeding to payment", event.getOrderId());
        
        // 步驟 3: 處理付款
        processPayment(event);
    }
    
    @TransactionalEventListener
    @Order(3)
    public void on(PaymentProcessedEvent event) {
        log.info("Payment processed for order: {}, proceeding to fulfillment", event.getOrderId());
        
        // 步驟 4: 執行配送
        fulfillOrder(event);
    }
    
    @TransactionalEventListener
    public void on(OrderFulfilledEvent event) {
        log.info("Order processing saga completed successfully for order: {}", event.getOrderId());
        
        // 發送確認通知
        sendOrderConfirmation(event);
    }
    
    // 補償動作
    @TransactionalEventListener
    public void on(PaymentFailedEvent event) {
        log.warn("Payment failed for order: {}, starting compensation", event.getOrderId());
        
        // 釋放庫存預留
        releaseInventoryReservation(event.getOrderId());
        
        // 取消訂單
        cancelOrder(event.getOrderId(), "Payment failed");
    }
}
```

### PaymentProcessingSaga

處理複雜的付款流程：

```java
@Component
public class PaymentProcessingSaga {
    
    @TransactionalEventListener
    public void on(PaymentRequested event) {
        // 步驟 1: 驗證付款資訊
        validatePaymentInfo(event.getPaymentInfo());
        
        // 步驟 2: 預授權
        preAuthorizePayment(event);
    }
    
    @TransactionalEventListener
    public void on(PaymentPreAuthorizedEvent event) {
        // 步驟 3: 確認付款
        confirmPayment(event);
    }
    
    @TransactionalEventListener
    public void on(PaymentConfirmedEvent event) {
        // 步驟 4: 更新訂單狀態
        updateOrderPaymentStatus(event);
    }
    
    // 補償動作
    @TransactionalEventListener
    public void on(PaymentPreAuthorizationFailedEvent event) {
        // 取消付款請求
        cancelPaymentRequest(event.getPaymentId());
        
        // 通知訂單服務付款失敗
        notifyPaymentFailure(event.getOrderId());
    }
}
```

## 🔄 編排 vs 編舞模式對比

### 編排式 Saga

**優點**:
- 集中控制，易於理解和除錯
- 明確的流程定義
- 容易實作複雜的業務邏輯
- 更好的監控和追蹤

**缺點**:
- 中央協調器可能成為單點故障
- 協調器與各服務耦合度較高
- 擴展性受限

**適用場景**:
- 複雜的業務流程
- 需要嚴格控制執行順序
- 團隊規模較小
- 對一致性要求較高

### 編舞式 Saga

**優點**:
- 去中心化，沒有單點故障
- 服務間鬆耦合
- 更好的擴展性
- 符合微服務理念

**缺點**:
- 流程分散，難以理解全貌
- 除錯和監控較困難
- 容易產生循環依賴
- 錯誤處理複雜

**適用場景**:
- 簡單的業務流程
- 服務間相對獨立
- 大型分散式系統
- 對可用性要求較高

## 🛠️ Saga 實作最佳實踐

### 1. 狀態管理

```java
@Entity
public class SagaInstance {
    
    @Id
    private String sagaId;
    
    @Enumerated(EnumType.STRING)
    private SagaStatus status;
    
    private String sagaType;
    private String currentStep;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    @ElementCollection
    @CollectionTable(name = "saga_steps")
    private List<SagaStep> steps = new ArrayList<>();
    
    public void addStep(String stepName, SagaStepStatus status) {
        steps.add(new SagaStep(stepName, status, LocalDateTime.now()));
    }
    
    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED || status == SagaStatus.COMPENSATED;
    }
    
    public boolean requiresCompensation() {
        return steps.stream().anyMatch(step -> step.getStatus() == SagaStepStatus.FAILED);
    }
}
```

### 2. 補償動作設計

```java
public interface CompensatableAction {
    
    /**
     * 執行正向動作
     */
    ActionResult execute();
    
    /**
     * 執行補償動作
     */
    ActionResult compensate();
    
    /**
     * 檢查動作是否可以補償
     */
    boolean isCompensatable();
}

@Component
public class InventoryReservationAction implements CompensatableAction {
    
    @Override
    public ActionResult execute() {
        try {
            inventoryService.reserveItems(orderItems);
            return ActionResult.success();
        } catch (Exception e) {
            return ActionResult.failure(e.getMessage());
        }
    }
    
    @Override
    public ActionResult compensate() {
        try {
            inventoryService.releaseReservation(orderId);
            return ActionResult.success();
        } catch (Exception e) {
            log.error("Failed to compensate inventory reservation", e);
            return ActionResult.failure(e.getMessage());
        }
    }
    
    @Override
    public boolean isCompensatable() {
        return inventoryService.hasReservation(orderId);
    }
}
```

### 3. 錯誤處理和重試

```java
@Component
public class SagaErrorHandler {
    
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void handleSagaStep(SagaStep step) {
        try {
            step.execute();
        } catch (PermanentException e) {
            // 永久性錯誤，直接進入補償流程
            startCompensation(step.getSagaId(), e);
        } catch (TransientException e) {
            // 暫時性錯誤，會被重試
            throw e;
        }
    }
    
    @Recover
    public void recover(TransientException ex, SagaStep step) {
        log.error("Saga step failed after all retries: {}", step.getStepName(), ex);
        startCompensation(step.getSagaId(), ex);
    }
    
    private void startCompensation(String sagaId, Exception cause) {
        SagaInstance saga = sagaRepository.findById(sagaId);
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setFailureReason(cause.getMessage());
        
        compensationService.startCompensation(saga);
    }
}
```

## 📊 監控和可觀測性

### 1. Saga 指標

```java
@Component
public class SagaMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Timer sagaExecutionTime;
    private final Counter sagaCompletions;
    private final Counter sagaFailures;
    
    public SagaMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.sagaExecutionTime = Timer.builder("saga.execution.time")
            .description("Time taken to complete saga")
            .register(meterRegistry);
        this.sagaCompletions = Counter.builder("saga.completions")
            .description("Number of completed sagas")
            .register(meterRegistry);
        this.sagaFailures = Counter.builder("saga.failures")
            .description("Number of failed sagas")
            .register(meterRegistry);
    }
    
    public void recordSagaCompletion(String sagaType, Duration duration) {
        sagaExecutionTime.record(duration);
        sagaCompletions.increment(Tags.of("saga.type", sagaType, "status", "completed"));
    }
    
    public void recordSagaFailure(String sagaType, String failureReason) {
        sagaFailures.increment(Tags.of("saga.type", sagaType, "reason", failureReason));
    }
}
```

### 2. 分散式追蹤

```java
@Component
public class SagaTracing {
    
    private final Tracer tracer;
    
    public void traceSagaExecution(SagaInstance saga) {
        Span span = tracer.nextSpan()
            .name("saga-execution")
            .tag("saga.id", saga.getSagaId())
            .tag("saga.type", saga.getSagaType())
            .start();
            
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            // Saga 執行邏輯
            executeSaga(saga);
            
            span.tag("saga.status", saga.getStatus().toString());
            span.event("saga.completed");
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            span.tag("saga.status", "failed");
            throw e;
        } finally {
            span.end();
        }
    }
}
```

## 🔗 相關資源

### 內部文檔
- [編排模式詳細指南](orchestration.md)
- [編舞模式詳細指南](choreography.md)
- [訂單處理 Saga 實作](order-processing-saga.md)
- [付款處理 Saga 實作](payment-saga.md)
- [履行處理 Saga 實作](fulfillment-saga.md)
- [Saga 協調機制](saga-coordination.md)

### 外部資源
- [Saga Pattern - Microservices.io](https://microservices.io/patterns/data/saga.html)
- [Distributed Sagas](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf)
- [Saga Pattern in Practice](https://blog.couchbase.com/saga-pattern-implement-business-transactions-using-microservices-part/)

---

**最後更新**: 2025年1月21日  
**維護者**: Architecture Team  
**版本**: 1.0

> 💡 **提示**: Saga 模式是處理分散式事務的強大工具，但也增加了系統複雜性。選擇編排還是編舞模式時，要考慮團隊能力、系統規模和業務複雜度。