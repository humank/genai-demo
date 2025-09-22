# Saga 編排模式 (Orchestration)

## 概述

編排式 Saga 使用中央協調器來管理整個事務流程。協調器負責決定下一步要執行什麼動作，並處理失敗和補償邏輯。

## 🎭 編排模式特性

### 優點

- **集中控制**: 所有邏輯集中在協調器中，易於理解和維護
- **明確流程**: 業務流程清晰可見，便於除錯和監控
- **複雜邏輯**: 能夠處理複雜的條件分支和業務規則
- **狀態管理**: 集中的狀態管理，便於追蹤和恢復

### 缺點

- **單點故障**: 協調器可能成為系統的瓶頸
- **耦合度高**: 協調器需要了解所有參與服務的介面
- **擴展性限制**: 新增服務需要修改協調器
- **性能瓶頸**: 所有請求都要經過協調器

## 🏗️ 實作架構

### 協調器設計

```java
@Component
@Slf4j
public class OrderSagaOrchestrator {
    
    private final SagaStateManager stateManager;
    private final List<SagaStep> sagaSteps;
    
    public OrderSagaOrchestrator(SagaStateManager stateManager,
                                List<SagaStep> sagaSteps) {
        this.stateManager = stateManager;
        this.sagaSteps = sagaSteps;
    }
    
    public void orchestrateOrderProcessing(OrderCreatedEvent event) {
        String sagaId = generateSagaId(event.getOrderId());
        
        SagaExecution execution = SagaExecution.builder()
            .sagaId(sagaId)
            .orderId(event.getOrderId())
            .steps(sagaSteps)
            .build();
        
        try {
            executeNextStep(execution);
        } catch (Exception e) {
            log.error("Saga orchestration failed for order: {}", event.getOrderId(), e);
            startCompensation(execution, e);
        }
    }
    
    private void executeNextStep(SagaExecution execution) {
        SagaStep currentStep = execution.getCurrentStep();
        
        if (currentStep == null) {
            // 所有步驟完成
            completeSaga(execution);
            return;
        }
        
        log.info("Executing saga step: {} for order: {}", 
                currentStep.getName(), execution.getOrderId());
        
        try {
            StepResult result = currentStep.execute(execution.getContext());
            
            if (result.isSuccess()) {
                // 步驟成功，繼續下一步
                execution.markStepCompleted(currentStep, result);
                stateManager.updateSagaState(execution);
                executeNextStep(execution);
            } else {
                // 步驟失敗，開始補償
                throw new SagaStepFailedException(currentStep.getName(), result.getError());
            }
            
        } catch (Exception e) {
            log.error("Saga step failed: {} for order: {}", 
                     currentStep.getName(), execution.getOrderId(), e);
            startCompensation(execution, e);
        }
    }
}
```

### Saga 步驟定義

```java
public interface SagaStep {
    String getName();
    StepResult execute(SagaContext context);
    CompensationResult compensate(SagaContext context);
    boolean isCompensatable();
}

@Component
public class InventoryReservationStep implements SagaStep {
    
    private final InventoryService inventoryService;
    
    @Override
    public String getName() {
        return "INVENTORY_RESERVATION";
    }
    
    @Override
    public StepResult execute(SagaContext context) {
        try {
            String orderId = context.getOrderId();
            List<OrderItem> items = context.getOrderItems();
            
            InventoryReservationResult result = inventoryService.reserveItems(orderId, items);
            
            if (result.isSuccess()) {
                // 保存補償所需的資料
                context.addCompensationData("reservationId", result.getReservationId());
                context.addCompensationData("reservedItems", result.getReservedItems());
                
                return StepResult.success(result);
            } else {
                return StepResult.failure(result.getFailureReason());
            }
            
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }
    
    @Override
    public CompensationResult compensate(SagaContext context) {
        try {
            String reservationId = context.getCompensationData("reservationId");
            
            if (reservationId != null) {
                inventoryService.releaseReservation(reservationId);
                return CompensationResult.success();
            } else {
                return CompensationResult.success(); // 沒有需要補償的資源
            }
            
        } catch (Exception e) {
            log.error("Failed to compensate inventory reservation", e);
            return CompensationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public boolean isCompensatable() {
        return true;
    }
}
```

### 付款處理步驟

```java
@Component
public class PaymentProcessingStep implements SagaStep {
    
    private final PaymentService paymentService;
    
    @Override
    public String getName() {
        return "PAYMENT_PROCESSING";
    }
    
    @Override
    public StepResult execute(SagaContext context) {
        try {
            String orderId = context.getOrderId();
            BigDecimal amount = context.getTotalAmount();
            PaymentInfo paymentInfo = context.getPaymentInfo();
            
            PaymentResult result = paymentService.processPayment(orderId, amount, paymentInfo);
            
            if (result.isSuccess()) {
                // 保存補償所需的資料
                context.addCompensationData("paymentId", result.getPaymentId());
                context.addCompensationData("paymentAmount", amount);
                
                return StepResult.success(result);
            } else {
                return StepResult.failure(result.getFailureReason());
            }
            
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }
    
    @Override
    public CompensationResult compensate(SagaContext context) {
        try {
            String paymentId = context.getCompensationData("paymentId");
            BigDecimal amount = context.getCompensationData("paymentAmount");
            
            if (paymentId != null && amount != null) {
                RefundResult result = paymentService.refundPayment(paymentId, amount);
                
                if (result.isSuccess()) {
                    return CompensationResult.success();
                } else {
                    return CompensationResult.failure(result.getFailureReason());
                }
            } else {
                return CompensationResult.success(); // 沒有需要補償的付款
            }
            
        } catch (Exception e) {
            log.error("Failed to compensate payment", e);
            return CompensationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public boolean isCompensatable() {
        return true;
    }
}
```

## 🔧 配置和設置

### Saga 配置

```java
@Configuration
@EnableSaga
public class SagaConfiguration {
    
    @Bean
    public SagaManager sagaManager(SagaStateManager stateManager,
                                  List<SagaStep> steps) {
        return new DefaultSagaManager(stateManager, steps);
    }
    
    @Bean
    public SagaStateManager sagaStateManager(SagaStateRepository repository) {
        return new JpaSagaStateManager(repository);
    }
    
    @Bean
    @Order(1)
    public SagaStep inventoryReservationStep(InventoryService inventoryService) {
        return new InventoryReservationStep(inventoryService);
    }
    
    @Bean
    @Order(2)
    public SagaStep paymentProcessingStep(PaymentService paymentService) {
        return new PaymentProcessingStep(paymentService);
    }
    
    @Bean
    @Order(3)
    public SagaStep shippingArrangementStep(ShippingService shippingService) {
        return new ShippingArrangementStep(shippingService);
    }
}
```

### 資料庫配置

```sql
-- Saga 狀態表
CREATE TABLE saga_state (
    saga_id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_step VARCHAR(100),
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    compensation_data TEXT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Saga 步驟記錄表
CREATE TABLE saga_step_record (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(255) NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_data TEXT,
    executed_at TIMESTAMP NOT NULL,
    FOREIGN KEY (saga_id) REFERENCES saga_state(saga_id)
);

-- 索引
CREATE INDEX idx_saga_state_order_id ON saga_state(order_id);
CREATE INDEX idx_saga_state_status ON saga_state(status);
CREATE INDEX idx_saga_step_saga_id ON saga_step_record(saga_id);
```

## 🎯 最佳實踐

### 1. 設計原則

- **冪等性**: 每個步驟都應該是冪等的
- **補償性**: 每個步驟都應該有對應的補償動作
- **可觀測性**: 提供充分的日誌和監控
- **錯誤處理**: 優雅處理各種異常情況

### 2. 實作建議

- 使用狀態機管理 Saga 狀態
- 實作重試機制處理暫時性錯誤
- 提供人工介入機制處理無法自動補償的情況
- 使用分散式追蹤監控 Saga 執行

### 3. 監控要點

- Saga 執行時間和成功率
- 各步驟的執行時間和失敗率
- 補償動作的執行情況
- 人工介入的頻率和原因

## 🔗 相關資源

- [編舞模式對比](choreography.md)
- [Saga 協調機制](saga-coordination.md)
- [付款 Saga 實作](payment-saga.md)

---

**最後更新**: 2025年1月21日  
**維護者**: Architecture Team  
**版本**: 1.0