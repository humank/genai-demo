# Aurora 樂觀鎖遷移檢查清單

## 概述

這個檢查清單幫助你系統性地將所有 JPA 實體遷移到使用 Aurora 樂觀鎖機制。

**建立日期**: 2025年9月24日 下午2:34 (台北時間)  
**需求**: 1.1 - 並發控制機制全面重構

## 🎯 **你的問題回答**

**是的，你應該把專案的所有 JPA 實體都改用支援 version 的版本！**

### 為什麼需要全面遷移？

1. **一致性**: 確保整個系統使用統一的並發控制機制
2. **可靠性**: 避免數據競爭和不一致問題
3. **可維護性**: 統一的基礎架構更容易維護和擴展
4. **性能**: Aurora 樂觀鎖機制針對高並發場景優化

## 📋 **遷移檢查清單**

### 階段一：準備工作

- [ ] **備份數據庫**: 在開始遷移前完整備份生產數據庫
- [ ] **代碼備份**: 為所有要修改的 JPA 實體文件創建備份
- [ ] **測試環境準備**: 確保測試環境與生產環境一致
- [ ] **依賴檢查**: 確認 `BaseOptimisticLockingEntity` 已正確實現

### 階段二：高優先級實體遷移 🔴

#### 訂單相關實體
- [ ] **JpaOrderEntity** - 訂單主表
  - [ ] 添加 `extends BaseOptimisticLockingEntity`
  - [ ] 移除重複的 `createdAt`, `updatedAt` 欄位
  - [ ] 移除相關的 getter/setter 方法
  - [ ] 更新建構子（移除時間戳記參數）
  - [ ] 執行數據庫遷移腳本
  - [ ] 更新相關的應用服務使用重試機制
  - [ ] 執行單元測試和整合測試

- [ ] **JpaOrderItemEntity** - 訂單項目
  - [ ] 同上述步驟

- [ ] **JpaOrderWorkflowEntity** - 訂單工作流
  - [ ] 同上述步驟

#### 庫存相關實體
- [ ] **JpaInventoryEntity** - 庫存主表
  - [ ] 遷移步驟同上
  - [ ] 特別注意：庫存更新是高並發操作，需要重點測試

- [ ] **JpaReservationEntity** - 庫存預留
  - [ ] 遷移步驟同上

- [ ] **StockMovement** - 庫存異動記錄
  - [ ] 遷移步驟同上

#### 購物車相關實體
- [ ] **JpaShoppingCartEntity** - 購物車
  - [ ] 遷移步驟同上

- [ ] **JpaCartItemEntity** - 購物車項目
  - [ ] 遷移步驟同上

#### 支付相關實體
- [ ] **JpaPaymentEntity** - 支付記錄
  - [ ] 遷移步驟同上

- [ ] **JpaPaymentMethodEntity** - 支付方式
  - [ ] 遷移步驟同上

### 階段三：中優先級實體遷移 🟡

#### 產品相關實體
- [ ] **ProductJpaEntity** - 產品主表
- [ ] **JpaProductReviewEntity** - 產品評價

#### 促銷相關實體
- [ ] **JpaPromotionEntity** - 促銷活動
- [ ] **JpaVoucherEntity** - 優惠券

#### 賣家相關實體
- [ ] **JpaSellerEntity** - 賣家信息

### 階段四：低優先級實體遷移 🟢

#### 通知相關實體
- [ ] **JpaNotificationEntity** - 通知記錄
- [ ] **JpaNotificationTemplateEntity** - 通知模板

#### 分析相關實體
- [ ] **JpaAnalyticsEventEntity** - 分析事件
- [ ] **JpaAnalyticsSessionEntity** - 分析會話

#### 配置相關實體
- [ ] **JpaPricingRuleEntity** - 定價規則

## 🛠️ **每個實體的遷移步驟**

### 1. 代碼遷移

```java
// ✅ 遷移前檢查清單
- [ ] 確認實體有 @Entity 註解
- [ ] 識別重複欄位（version, createdAt, updatedAt）
- [ ] 識別相關的 getter/setter 方法
- [ ] 檢查建構子中的時間戳記參數

// ✅ 執行遷移
- [ ] 添加 import: import solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity;
- [ ] 修改類聲明: public class XxxEntity extends BaseOptimisticLockingEntity
- [ ] 移除重複欄位定義
- [ ] 移除重複的 getter/setter 方法
- [ ] 更新建構子（移除時間戳記參數）
- [ ] 添加遷移註釋

// ✅ 遷移後檢查
- [ ] 編譯通過
- [ ] 沒有重複的欄位定義
- [ ] 時間戳記通過基礎類訪問
```

### 2. 數據庫遷移

```sql
-- ✅ 為每個表執行
ALTER TABLE table_name 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ✅ 初始化數據
UPDATE table_name SET version = 0 WHERE version IS NULL;
UPDATE table_name SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- ✅ 創建觸發器
CREATE TRIGGER update_table_name_updated_at 
    BEFORE UPDATE ON table_name 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ✅ 添加索引
CREATE INDEX IF NOT EXISTS idx_table_name_version ON table_name(version);
```

### 3. 應用服務更新

```java
// ✅ 更新前
@Service
@Transactional
public class XxxService {
    public void updateEntity(String id, UpdateData data) {
        Entity entity = repository.findById(id).orElseThrow();
        entity.updateData(data);
        entity.setUpdatedAt(LocalDateTime.now()); // 手動設置
        repository.save(entity);
    }
}

// ✅ 更新後
@Service
@Transactional
public class XxxService {
    private final OptimisticLockingRetryService retryService;
    
    public boolean updateEntity(String id, UpdateData data) {
        try {
            return retryService.executeWithRetry(
                () -> {
                    Entity entity = repository.findById(id).orElseThrow();
                    entity.updateData(data);
                    // 時間戳記自動更新
                    repository.save(entity);
                    return true;
                },
                "Entity", id, "updateEntity"
            );
        } catch (OptimisticLockingConflictException e) {
            logger.error("Failed to update entity after retries: {}", e.getMessage());
            return false;
        }
    }
}
```

### 4. 測試驗證

```java
// ✅ 單元測試
@Test
void should_handle_optimistic_locking_conflicts() {
    // 測試樂觀鎖衝突處理
}

// ✅ 整合測試
@Test
void should_retry_on_concurrent_updates() {
    // 測試重試機制
}

// ✅ 並發測試
@Test
void should_handle_high_concurrency() {
    // 測試高並發場景
}
```

## 🚀 **快速開始指南**

### 1. 選擇第一個實體進行遷移

建議從 **JpaOrderEntity** 開始，因為：
- 訂單更新是常見的並發操作
- 業務影響相對可控
- 可以作為其他實體的遷移範本

### 2. 使用提供的工具

```bash
# 使用自動化遷移腳本
python3 scripts/migrate-entities-to-optimistic-locking.py --entity-path app/src/main/java/solid/humank/genaidemo/infrastructure/order/persistence/entity/JpaOrderEntity.java --dry-run

# 批量遷移高優先級實體
python3 scripts/migrate-entities-to-optimistic-locking.py --batch --priority high --dry-run

# 生成數據庫遷移腳本
python3 scripts/migrate-entities-to-optimistic-locking.py --batch --priority high --generate-sql
```

### 3. 執行數據庫遷移

```bash
# 執行訂單相關表的遷移
psql -d your_database -f scripts/database-migration-orders-optimistic-locking.sql
```

### 4. 驗證遷移結果

```bash
# 執行測試
./gradlew test --tests "*OptimisticLocking*"

# 檢查編譯
./gradlew compileJava compileTestJava
```

## ⚠️ **注意事項**

### 遷移風險
1. **數據完整性**: 確保遷移過程中不會遺失數據
2. **應用相容性**: 舊代碼可能依賴原有的欄位名稱
3. **性能影響**: 樂觀鎖機制會增加少量開銷

### 緩解措施
1. **分階段部署**: 按優先級分批遷移，降低風險
2. **充分測試**: 每個階段都要進行完整測試
3. **監控告警**: 密切監控遷移後的系統性能
4. **回滾準備**: 準備快速回滾方案

## 📊 **進度追蹤**

### 整體進度
- [ ] 階段一：準備工作 (0/4)
- [ ] 階段二：高優先級實體 (0/10)
- [ ] 階段三：中優先級實體 (0/5)
- [ ] 階段四：低優先級實體 (0/5)

### 成功指標
- [ ] 所有重要實體都繼承 `BaseOptimisticLockingEntity`
- [ ] 樂觀鎖衝突檢測率 < 5%
- [ ] 重試成功率 > 95%
- [ ] 系統性能無明顯下降
- [ ] 零數據遺失或損壞

## 🎉 **完成後的好處**

1. **統一的並發控制**: 整個系統使用一致的樂觀鎖機制
2. **提高可靠性**: 減少數據競爭和不一致問題
3. **更好的性能**: Aurora 樂觀鎖針對高並發場景優化
4. **易於維護**: 統一的基礎架構更容易維護和擴展
5. **監控和調試**: 統一的衝突檢測和重試機制便於監控

---

**建議**: 從高優先級實體開始，逐步遷移。每完成一個實體的遷移，就進行充分的測試驗證，確保系統穩定後再繼續下一個。這樣可以最大程度地降低風險，確保遷移的成功。