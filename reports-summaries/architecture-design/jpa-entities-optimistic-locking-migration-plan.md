# JPA 實體樂觀鎖遷移計劃

## 概述

為了確保整個系統的並發安全性和一致性，需要將所有重要的 JPA 實體都遷移到繼承 `BaseOptimisticLockingEntity`，以支援 Aurora 樂觀鎖機制。

**建立日期**: 2025年9月24日 下午2:34 (台北時間)  
**需求**: 1.1 - 並發控制機制全面重構  
**狀態**: 規劃中

## 遷移策略

### 優先級分類

#### 🔴 **高優先級** (立即遷移)
這些實體涉及核心業務邏輯，經常有並發更新操作：

1. **訂單相關**
   - `JpaOrderEntity` - 訂單狀態更新
   - `JpaOrderItemEntity` - 訂單項目修改
   - `JpaOrderWorkflowEntity` - 工作流狀態變更

2. **庫存相關**
   - `JpaInventoryEntity` - 庫存數量更新
   - `JpaReservationEntity` - 庫存預留操作
   - `StockMovement` - 庫存異動記錄

3. **購物車相關**
   - `JpaShoppingCartEntity` - 購物車內容更新
   - `JpaCartItemEntity` - 購物車項目修改

4. **支付相關**
   - `JpaPaymentEntity` - 支付狀態更新
   - `JpaPaymentMethodEntity` - 支付方式管理

#### 🟡 **中優先級** (第二階段遷移)
這些實體有一定的並發更新需求：

1. **產品相關**
   - `ProductJpaEntity` - 產品信息更新
   - `JpaProductReviewEntity` - 評價管理

2. **促銷相關**
   - `JpaPromotionEntity` - 促銷活動管理
   - `JpaVoucherEntity` - 優惠券使用

3. **賣家相關**
   - `JpaSellerEntity` - 賣家信息更新

#### 🟢 **低優先級** (第三階段遷移)
這些實體更新頻率較低，但為了一致性也應該遷移：

1. **通知相關**
   - `JpaNotificationEntity` - 通知記錄
   - `JpaNotificationTemplateEntity` - 通知模板

2. **分析相關**
   - `JpaAnalyticsEventEntity` - 分析事件
   - `JpaAnalyticsSessionEntity` - 分析會話

3. **配置相關**
   - `JpaPricingRuleEntity` - 定價規則

## 遷移步驟

### 第一階段：高優先級實體遷移

#### 1. 訂單實體遷移

```java
// 原始代碼
@Entity
@Table(name = "orders")
public class JpaOrderEntity {
    @Id
    private String id;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 其他欄位...
}

// 遷移後
@Entity
@Table(name = "orders")
public class JpaOrderEntity extends BaseOptimisticLockingEntity {
    @Id
    private String id;
    
    // 移除 createdAt 和 updatedAt，因為已在基礎類中定義
    // @Column(name = "created_at")
    // private LocalDateTime createdAt;
    
    // @Column(name = "updated_at")
    // private LocalDateTime updatedAt;
    
    // 其他欄位...
}
```

#### 2. 庫存實體遷移

```java
// JpaInventoryEntity 遷移
@Entity
@Table(name = "inventories")
public class JpaInventoryEntity extends BaseOptimisticLockingEntity {
    @Id
    private String id;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "available_quantity")
    private Integer availableQuantity;
    
    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;
    
    // 其他欄位...
}
```

#### 3. 購物車實體遷移

```java
// JpaShoppingCartEntity 遷移
@Entity
@Table(name = "shopping_carts")
public class JpaShoppingCartEntity extends BaseOptimisticLockingEntity {
    @Id
    private String id;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    // 其他欄位...
}
```

### 數據庫遷移腳本

#### 1. 添加版本號欄位

```sql
-- 為所有需要遷移的表添加 version 欄位
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE order_items ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventories ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE inventory_reservations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE shopping_carts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cart_items ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_methods ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- 為沒有時間戳記的表添加 created_at 和 updated_at
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 創建觸發器自動更新 updated_at（PostgreSQL）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 為每個表創建觸發器
CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

#### 2. 數據遷移腳本

```sql
-- 初始化現有記錄的版本號
UPDATE orders SET version = 0 WHERE version IS NULL;
UPDATE inventories SET version = 0 WHERE version IS NULL;
UPDATE shopping_carts SET version = 0 WHERE version IS NULL;

-- 設置現有記錄的時間戳記（如果為空）
UPDATE orders SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;
```

## 遷移檢查清單

### 代碼遷移檢查

- [ ] **移除重複欄位**: 刪除實體中的 `createdAt`, `updatedAt`, `version` 欄位定義
- [ ] **繼承基礎類**: 將 `extends BaseOptimisticLockingEntity` 添加到類別聲明
- [ ] **更新建構子**: 移除時間戳記相關的建構子參數
- [ ] **更新 getter/setter**: 移除重複的 getter/setter 方法
- [ ] **更新測試**: 修改相關的單元測試和整合測試

### 數據庫遷移檢查

- [ ] **添加版本欄位**: 為所有表添加 `version BIGINT NOT NULL DEFAULT 0`
- [ ] **添加時間戳記**: 確保所有表都有 `created_at` 和 `updated_at` 欄位
- [ ] **創建觸發器**: 設置自動更新 `updated_at` 的觸發器
- [ ] **數據初始化**: 為現有記錄設置初始版本號和時間戳記
- [ ] **索引優化**: 為 `version` 欄位添加適當的索引

### 應用服務更新檢查

- [ ] **使用重試服務**: 將關鍵更新操作包裝在 `OptimisticLockingRetryService` 中
- [ ] **錯誤處理**: 添加適當的樂觀鎖異常處理
- [ ] **事務邊界**: 確保事務邊界正確設置
- [ ] **監控集成**: 添加相關的監控和日誌記錄

## 實施範例

### 完整的實體遷移範例

```java
// 遷移前
@Entity
@Table(name = "orders")
public class JpaOrderEntity {
    @Id
    private String id;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 建構子、getter、setter...
}

// 遷移後
@Entity
@Table(name = "orders")
public class JpaOrderEntity extends BaseOptimisticLockingEntity {
    @Id
    private String id;
    
    @Column(name = "customer_id")
    private String customerId;
    
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    
    // 移除重複的時間戳記欄位
    // createdAt 和 updatedAt 已在 BaseOptimisticLockingEntity 中定義
    
    // 建構子不再需要時間戳記參數
    public JpaOrderEntity(String id, String customerId, OrderStatus status, BigDecimal totalAmount) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        // 時間戳記會在 @PrePersist 時自動設置
    }
    
    // getter、setter...（移除時間戳記相關的方法）
}
```

### 應用服務更新範例

```java
// 遷移前
@Service
@Transactional
public class OrderService {
    
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        JpaOrderEntity order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now()); // 手動設置更新時間
        orderRepository.save(order);
    }
}

// 遷移後
@Service
@Transactional
public class OrderService {
    
    private final OptimisticLockingRetryService retryService;
    
    public boolean updateOrderStatus(String orderId, OrderStatus newStatus) {
        try {
            return retryService.executeWithRetry(
                () -> {
                    JpaOrderEntity order = orderRepository.findById(orderId).orElseThrow();
                    order.setStatus(newStatus);
                    // 更新時間會在 @PreUpdate 時自動設置
                    orderRepository.save(order);
                    return true;
                },
                "Order",
                orderId,
                "updateOrderStatus"
            );
        } catch (OptimisticLockingConflictException e) {
            logger.error("Failed to update order status after retries: {}", e.getMessage());
            return false;
        }
    }
}
```

## 測試策略

### 1. 單元測試

```java
@Test
void should_handle_optimistic_locking_for_order_updates() {
    // Given
    JpaOrderEntity order = new JpaOrderEntity("order-1", "customer-1", OrderStatus.PENDING, new BigDecimal("100.00"));
    orderRepository.save(order);
    
    // When - 模擬並發更新
    JpaOrderEntity order1 = orderRepository.findById("order-1").orElseThrow();
    JpaOrderEntity order2 = orderRepository.findById("order-1").orElseThrow();
    
    order1.setStatus(OrderStatus.CONFIRMED);
    orderRepository.save(order1); // 成功
    
    order2.setStatus(OrderStatus.CANCELLED);
    
    // Then - 第二個更新應該拋出樂觀鎖異常
    assertThatThrownBy(() -> orderRepository.save(order2))
        .isInstanceOf(OptimisticLockingFailureException.class);
}
```

### 2. 整合測試

```java
@SpringBootTest
@Transactional
class OrderOptimisticLockingIntegrationTest {
    
    @Test
    void should_retry_on_concurrent_order_updates() {
        // 測試重試服務在訂單並發更新時的行為
        // 類似於 AuroraOptimisticLockingIntegrationTest 的測試模式
    }
}
```

## 風險評估和緩解措施

### 潛在風險

1. **數據遷移風險**
   - 現有數據可能缺少版本號
   - 時間戳記格式不一致

2. **應用程式相容性**
   - 現有代碼可能依賴舊的欄位名稱
   - 建構子簽名變更

3. **性能影響**
   - 額外的版本號檢查
   - 重試機制的開銷

### 緩解措施

1. **分階段部署**
   - 先在測試環境完整驗證
   - 按優先級分批遷移

2. **向後相容性**
   - 保留舊的 getter 方法作為 deprecated
   - 提供遷移期間的相容性層

3. **監控和回滾**
   - 密切監控遷移後的系統性能
   - 準備快速回滾方案

## 時程規劃

### 第一週：高優先級實體
- 訂單相關實體遷移
- 庫存相關實體遷移
- 基礎測試驗證

### 第二週：中優先級實體
- 產品相關實體遷移
- 促銷相關實體遷移
- 購物車相關實體遷移

### 第三週：低優先級實體
- 通知相關實體遷移
- 分析相關實體遷移
- 全面測試和優化

### 第四週：部署和監控
- 生產環境部署
- 性能監控和調優
- 文檔更新

## 成功指標

- [ ] 所有重要 JPA 實體都繼承 `BaseOptimisticLockingEntity`
- [ ] 樂觀鎖衝突檢測率 < 5%
- [ ] 重試成功率 > 95%
- [ ] 系統性能無明顯下降
- [ ] 零數據遺失或損壞

## 後續維護

1. **新實體規範**: 所有新創建的 JPA 實體都必須繼承 `BaseOptimisticLockingEntity`
2. **代碼審查**: 在 PR 審查中檢查樂觀鎖的正確使用
3. **監控告警**: 設置樂觀鎖衝突率的監控告警
4. **定期檢查**: 定期檢查系統中是否有遺漏的實體需要遷移

---

**結論**: 這個遷移計劃將確保整個系統具有一致的並發控制機制，提高系統的可靠性和數據一致性。建議按照優先級分階段實施，並在每個階段進行充分的測試驗證。