# Aurora 樂觀鎖策略實作指南

## 概述

本文檔說明 Aurora Global Database 樂觀鎖策略的完整實作，包含基礎實體類、衝突檢測、讀寫端點分離和重試機制。

**建立日期**: 2025年9月24日 上午10:18 (台北時間)  
**需求**: 1.1 - 並發控制機制全面重構

## 核心組件

### 1. BaseOptimisticLockingEntity

所有需要樂觀鎖的 JPA 實體都應該繼承此基礎類：

```java
@Entity
@Table(name = "customers")
public class JpaCustomerEntity extends BaseOptimisticLockingEntity {
    // 實體欄位...
}
```

**提供功能**:
- `@Version` 註解的版本號欄位
- 自動時間戳記管理 (`createdAt`, `updatedAt`)
- 實體狀態檢查方法 (`isNew()`, `isPersisted()`)

### 2. 讀寫端點分離 (AuroraReadWriteConfiguration)

自動路由讀寫操作到不同的 Aurora 端點：

```java
// 手動指定讀取端點
Customer customer = DataSourceRouter.executeWithReadDataSource(() -> {
    return customerRepository.findById(customerId);
});

// 手動指定寫入端點
DataSourceRouter.executeWithWriteDataSource(() -> {
    customerRepository.save(customer);
    return null;
});
```

**自動路由規則**:
- 事務中的操作 → 寫入端點
- 只讀事務 → 讀取端點
- 默認 → 寫入端點

### 3. 衝突檢測 (OptimisticLockingConflictDetector)

自動檢測和分析樂觀鎖衝突：

```java
@Component
public class MyService {
    
    private final OptimisticLockingConflictDetector conflictDetector;
    
    public void handleConflict(Exception e) {
        if (conflictDetector.isOptimisticLockingException(e)) {
            ConflictInfo info = conflictDetector.detectConflict(e, "Customer", "123", "update");
            RetryStrategy strategy = conflictDetector.analyzeAndSuggestRetryStrategy(info);
            // 處理衝突...
        }
    }
}
```

### 4. 重試服務 (OptimisticLockingRetryService)

提供自動重試機制：

```java
@Service
public class CustomerService {
    
    private final OptimisticLockingRetryService retryService;
    
    @Transactional
    public boolean updateCustomer(String customerId, UpdateData data) {
        try {
            Boolean result = retryService.executeWithRetry(
                () -> performUpdate(customerId, data),
                "Customer",
                customerId,
                "updateCustomer"
            );
            return result != null && result;
        } catch (OptimisticLockingConflictException e) {
            logger.error("Failed to update customer after retries: {}", e.getMessage());
            return false;
        }
    }
}
```

## 配置

### application-aurora.yml

```yaml
aurora:
  # 樂觀鎖配置
  optimistic-locking:
    enabled: true
    default-max-retries: 3
    enable-conflict-detection: true
    enable-performance-monitoring: true

  # 讀寫分離配置
  read-write-separation:
    enabled: true
    force-write-in-transaction: true

  # 數據源配置
  datasource:
    write:
      url: jdbc:postgresql://aurora-writer.cluster-xxx.rds.amazonaws.com:5432/genaidemo
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 20
        connection-timeout: 20000
    
    read:
      url: jdbc:postgresql://aurora-reader.cluster-xxx.rds.amazonaws.com:5432/genaidemo
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 15
        connection-timeout: 10000
        read-only: true
```

## 使用範例

### 基本使用

```java
@Service
public class OrderService {
    
    private final OptimisticLockingRetryService retryService;
    private final OrderRepository orderRepository;
    
    @Transactional
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        retryService.executeWithRetry(
            () -> {
                Order order = orderRepository.findById(orderId).orElseThrow();
                order.updateStatus(newStatus);
                orderRepository.save(order);
                return null;
            },
            "Order",
            orderId,
            "updateOrderStatus"
        );
    }
}
```

### 批量操作

```java
@Service
public class BatchService {
    
    public int batchUpdateCustomers(List<String> customerIds) {
        int successCount = 0;
        
        for (String customerId : customerIds) {
            try {
                retryService.executeWithRetry(
                    () -> performCustomerUpdate(customerId),
                    "Customer",
                    customerId,
                    "batchUpdate",
                    3 // 自定義重試次數
                );
                successCount++;
            } catch (Exception e) {
                logger.warn("Failed to update customer {}: {}", customerId, e.getMessage());
            }
        }
        
        return successCount;
    }
}
```

### 並發場景處理

```java
@Service
public class ConcurrentService {
    
    @Async
    public CompletableFuture<Boolean> processCustomerAsync(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return retryService.executeWithRetry(
                    () -> performAsyncProcessing(customerId),
                    "Customer",
                    customerId,
                    "asyncProcessing"
                );
            } catch (Exception e) {
                logger.error("Async processing failed for customer {}: {}", customerId, e.getMessage());
                return false;
            }
        });
    }
}
```

## 重試策略

系統會根據衝突情況自動選擇重試策略：

| 版本差異 | 重試策略 | 延遲算法 | 最大重試次數 |
|---------|---------|---------|-------------|
| 1 | IMMEDIATE_RETRY | 0ms | 3 |
| 2-5 | LINEAR_BACKOFF | 100ms × 重試次數 | 5 |
| >5 | EXPONENTIAL_BACKOFF | 100ms × 2^(重試次數-1) | 4 |

## 監控和指標

### 日誌記錄

```
2025-09-24 10:18:00.123 WARN  [...] Optimistic locking conflict detected for entity: Customer with id: 123
2025-09-24 10:18:00.124 DEBUG [...] Retrying operation updateCustomer for entity Customer (id: 123) in 100ms (attempt 2)
2025-09-24 10:18:00.225 INFO  [...] Operation updateCustomer for entity Customer (id: 123) succeeded after 2 attempts
```

### 自定義指標

```yaml
custom:
  metrics:
    optimistic-locking:
      enabled: true
      conflict-rate-threshold: 0.1
      retry-success-rate-threshold: 0.8
```

## 最佳實踐

### 1. 實體設計

```java
// ✅ 正確：繼承 BaseOptimisticLockingEntity
@Entity
public class CustomerEntity extends BaseOptimisticLockingEntity {
    // 業務欄位...
}

// ❌ 錯誤：手動管理版本號
@Entity
public class CustomerEntity {
    @Version
    private Long version; // 不建議手動管理
}
```

### 2. 應用服務設計

```java
// ✅ 正確：使用重試服務包裝業務邏輯
@Transactional
public boolean updateCustomer(String customerId, UpdateData data) {
    return retryService.executeWithRetry(
        () -> performBusinessLogic(customerId, data),
        "Customer", customerId, "updateCustomer"
    );
}

// ❌ 錯誤：直接處理樂觀鎖異常
@Transactional
public boolean updateCustomer(String customerId, UpdateData data) {
    try {
        performBusinessLogic(customerId, data);
        return true;
    } catch (OptimisticLockException e) {
        // 手動重試邏輯...
    }
}
```

### 3. 並發處理

```java
// ✅ 正確：每個並發操作使用獨立的重試機制
public void processConcurrentUpdates(List<String> customerIds) {
    customerIds.parallelStream().forEach(customerId -> {
        retryService.executeWithRetry(
            () -> processCustomer(customerId),
            "Customer", customerId, "concurrentProcessing"
        );
    });
}
```

### 4. 錯誤處理

```java
// ✅ 正確：適當的異常處理和日誌記錄
try {
    retryService.executeWithRetry(operation, entityType, entityId, operationName);
} catch (OptimisticLockingConflictException e) {
    logger.error("Operation failed after {} retries: {}", 
                e.getMaxRetryAttempts(), e.getMessage());
    // 業務降級處理...
} catch (Exception e) {
    logger.error("Unexpected error in operation: {}", e.getMessage(), e);
    throw e;
}
```

## 故障排除

### 常見問題

1. **高衝突率**
   - 檢查業務邏輯是否有不必要的並發操作
   - 考慮使用分散式鎖替代樂觀鎖
   - 優化事務邊界

2. **重試失敗**
   - 檢查網路連接和數據庫性能
   - 調整重試策略和次數
   - 分析衝突模式

3. **性能問題**
   - 監控重試頻率和延遲
   - 檢查數據庫索引和查詢性能
   - 考慮讀寫分離配置

### 調試工具

```java
// 啟用詳細日誌
logging:
  level:
    solid.humank.genaidemo.infrastructure.common.persistence: DEBUG
    org.springframework.transaction: DEBUG
    org.hibernate.SQL: DEBUG
```

## 測試

參考 `AuroraOptimisticLockingIntegrationTest` 了解完整的測試範例，包含：

- 基本樂觀鎖功能測試
- 並發衝突處理測試
- 讀寫端點分離測試
- 批量操作測試
- 高頻率並發更新測試

## 相關文檔

- [BaseOptimisticLockingEntity.java](BaseOptimisticLockingEntity.java) - 基礎實體類
- [AuroraReadWriteConfiguration.java](AuroraReadWriteConfiguration.java) - 讀寫分離配置
- [OptimisticLockingConflictDetector.java](OptimisticLockingConflictDetector.java) - 衝突檢測器
- [OptimisticLockingRetryService.java](OptimisticLockingRetryService.java) - 重試服務
- OptimisticLockingCustomerService.java - 應用服務範例
