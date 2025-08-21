# 事件處理錯誤處理和監控機制

本模組實現了完整的事件處理錯誤處理和監控機制，滿足需求 9.1 到 9.5 的所有要求。

## 功能概述

### 需求 9.1: 事件處理失敗的錯誤記錄和重試機制

- **EventRetryManager**: 管理事件重試邏輯
- **RetryPolicy**: 配置重試策略（最大嘗試次數、延遲時間、退避策略）
- **EventProcessingException**: 事件處理異常，支持重試標記

### 需求 9.2: 系統異常的適當錯誤回應和日誌記錄

- **GlobalExceptionHandler**: 擴展了全局異常處理器，支持事件處理異常
- **EventProcessingMonitor**: 監控事件處理過程，記錄詳細日誌
- **StandardErrorResponse**: 統一的錯誤回應格式

### 需求 9.3: 事件處理超時的檢測和處理機制

- **EventProcessingTimeoutException**: 超時異常
- **EventProcessingContext**: 追蹤事件處理時間
- **ResilientEventHandler**: 整合超時檢測和處理

### 需求 9.4: 系統負載過高時的背壓機制

- **BackpressureManager**: 管理系統負載和背壓
- **BackpressureLevel**: 定義負載等級（NORMAL, MODERATE, HIGH, CRITICAL）
- **BackpressureDecision**: 背壓決策（PROCEED, DELAY, REJECT）

### 需求 9.5: 事件順序錯亂的檢測和處理機制

- **EventSequenceTracker**: 追蹤事件順序
- **EventSequenceException**: 順序錯亂異常
- **EventSequenceRecord**: 序列驗證記錄

## 核心組件

### ResilientEventHandler

統一的事件處理入口，整合所有錯誤處理和監控機制：

```java
@Autowired
private ResilientEventHandler resilientEventHandler;

// 使用默認配置處理事件
CompletableFuture<Void> result = resilientEventHandler.handleEvent(
    event, "MyEventHandler", eventHandler);

// 使用自定義配置
CompletableFuture<Void> result = resilientEventHandler.handleEvent(
    event, "MyEventHandler", eventHandler, 
    Duration.ofSeconds(60), RetryPolicy.fastRetryPolicy());
```

### 重試策略配置

```java
// 默認重試策略：最多3次，指數退避
RetryPolicy defaultPolicy = RetryPolicy.defaultPolicy();

// 快速重試策略：最多5次，較短延遲
RetryPolicy fastPolicy = RetryPolicy.fastRetryPolicy();

// 自定義重試策略
RetryPolicy customPolicy = new RetryPolicy.Builder()
    .maxAttempts(5)
    .initialDelay(Duration.ofMillis(500))
    .maxDelay(Duration.ofSeconds(10))
    .backoffMultiplier(2.0)
    .retryOn(throwable -> !(throwable instanceof ValidationException))
    .build();
```

### 背壓管理

```java
@Autowired
private BackpressureManager backpressureManager;

// 檢查是否可以處理新事件
BackpressureDecision decision = backpressureManager.shouldProcessEvent();

switch (decision) {
    case PROCEED:
        // 正常處理
        break;
    case DELAY:
        // 延遲處理
        Duration delay = backpressureManager.getSuggestedDelay();
        break;
    case REJECT:
        // 拒絕處理
        break;
}
```

### 監控和統計

```java
@Autowired
private EventProcessingMonitor monitor;

// 獲取處理統計
ProcessingStatistics stats = monitor.getStatistics();
System.out.println("成功率: " + stats.getSuccessRate() * 100 + "%");
System.out.println("失敗率: " + stats.getFailureRate() * 100 + "%");
System.out.println("超時率: " + stats.getTimeoutRate() * 100 + "%");
```

## 監控端點

系統提供了 REST API 端點用於監控：

- `GET /api/monitoring/events/health` - 系統健康狀態
- `GET /api/monitoring/events/statistics/processing` - 事件處理統計
- `GET /api/monitoring/events/statistics/retry` - 重試統計
- `GET /api/monitoring/events/backpressure/status` - 背壓狀態
- `GET /api/monitoring/events/statistics/sequence` - 序列追蹤統計
- `POST /api/monitoring/events/statistics/processing/reset` - 重置統計
- `POST /api/monitoring/events/sequence/{aggregateId}/reset` - 重置序列

## 配置

### 線程池配置

```java
@Configuration
@EnableAsync
@EnableScheduling
public class EventProcessingConfig {
    
    @Bean(name = "eventProcessingExecutor")
    public Executor eventProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        return executor;
    }
}
```

### 背壓配置

```java
// 自定義背壓管理器
BackpressureManager customManager = new BackpressureManager(
    100,                              // 最大並發事件數
    1000,                            // 最大隊列大小
    Duration.ofMinutes(1),           // 滑動窗口時間
    1000                             // 窗口內最大事件數
);
```

## 維護任務

系統自動執行以下維護任務：

- **每分鐘**: 檢查超時的事件處理
- **每5分鐘**: 清理過期的重試上下文
- **每小時**: 清理過期的序列記錄
- **每10分鐘**: 記錄系統統計信息

## 異常處理

系統定義了以下專用異常：

- **EventProcessingException**: 一般事件處理異常
- **EventProcessingTimeoutException**: 事件處理超時異常
- **EventSequenceException**: 事件順序錯亂異常

所有異常都會被 `GlobalExceptionHandler` 捕獲並轉換為標準的錯誤回應。

## 使用示例

### 基本事件處理

```java
@Component
public class OrderEventHandler {
    
    @Autowired
    private ResilientEventHandler resilientEventHandler;
    
    public void handleOrderCreated(OrderCreatedEvent event) {
        resilientEventHandler.handleEvent(
            event, 
            "OrderCreatedHandler",
            this::processOrderCreated
        );
    }
    
    private void processOrderCreated(DomainEvent event) {
        OrderCreatedEvent orderEvent = (OrderCreatedEvent) event;
        // 處理訂單創建邏輯
    }
}
```

### 自定義重試策略

```java
@Component
public class PaymentEventHandler {
    
    @Autowired
    private ResilientEventHandler resilientEventHandler;
    
    public void handlePaymentProcessing(PaymentEvent event) {
        // 支付處理使用更積極的重試策略
        RetryPolicy paymentRetryPolicy = new RetryPolicy.Builder()
            .maxAttempts(5)
            .initialDelay(Duration.ofSeconds(2))
            .maxDelay(Duration.ofMinutes(5))
            .backoffMultiplier(2.0)
            .retryOn(throwable -> !(throwable instanceof PaymentValidationException))
            .build();
        
        resilientEventHandler.handleEvent(
            event,
            "PaymentHandler",
            this::processPayment,
            Duration.ofMinutes(2),  // 2分鐘超時
            paymentRetryPolicy
        );
    }
}
```

## 性能考量

1. **線程池大小**: 根據系統負載調整事件處理線程池大小
2. **重試策略**: 避免過於激進的重試策略，防止系統過載
3. **背壓閾值**: 根據系統容量設置合適的背壓閾值
4. **監控開銷**: 監控功能會增加一定的性能開銷，可根據需要調整監控級別
5. **清理頻率**: 定期清理過期數據，避免內存洩漏

## 故障排除

### 常見問題

1. **事件處理超時**
   - 檢查處理邏輯是否有阻塞操作
   - 調整超時時間配置
   - 檢查線程池是否飽和

2. **重試次數過多**
   - 檢查重試策略配置
   - 分析失敗原因，是否需要調整重試條件
   - 考慮使用死信隊列處理持續失敗的事件

3. **背壓頻繁觸發**
   - 檢查系統負載
   - 調整背壓閾值
   - 考慮水平擴展

4. **事件順序錯亂**
   - 檢查事件發布順序
   - 驗證聚合根狀態管理
   - 考慮使用事件版本號

### 日誌分析

系統會記錄詳細的日誌信息，包括：

- 事件處理開始和結束時間
- 重試嘗試和失敗原因
- 背壓狀態變化
- 序列驗證結果
- 系統統計信息

通過分析這些日誌，可以快速定位和解決問題。
