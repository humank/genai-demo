-- 訂單表樂觀鎖遷移腳本
-- 建立日期: 2025年9月24日 下午2:34 (台北時間)
-- 需求: 1.1 - 並發控制機制全面重構

-- 為訂單相關表添加樂觀鎖支援

-- 1. 為 orders 表添加版本號欄位
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. 確保時間戳記欄位存在（如果不存在則添加）
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 3. 初始化現有記錄的版本號
UPDATE orders SET version = 0 WHERE version IS NULL;

-- 4. 初始化現有記錄的時間戳記（如果為空）
UPDATE orders SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- 5. 為 order_items 表添加樂觀鎖支援
ALTER TABLE order_items 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 初始化 order_items 的版本號和時間戳記
UPDATE order_items SET version = 0 WHERE version IS NULL;
UPDATE order_items SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- 6. 為 order_workflows 表添加樂觀鎖支援（如果存在）
ALTER TABLE order_workflows 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 初始化 order_workflows 的版本號和時間戳記
UPDATE order_workflows SET version = 0 WHERE version IS NULL;
UPDATE order_workflows SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- 7. 創建通用的 updated_at 更新函數（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 8. 為 orders 表創建更新觸發器
DROP TRIGGER IF EXISTS update_orders_updated_at ON orders;
CREATE TRIGGER update_orders_updated_at 
    BEFORE UPDATE ON orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 9. 為 order_items 表創建更新觸發器
DROP TRIGGER IF EXISTS update_order_items_updated_at ON order_items;
CREATE TRIGGER update_order_items_updated_at 
    BEFORE UPDATE ON order_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 10. 為 order_workflows 表創建更新觸發器
DROP TRIGGER IF EXISTS update_order_workflows_updated_at ON order_workflows;
CREATE TRIGGER update_order_workflows_updated_at 
    BEFORE UPDATE ON order_workflows 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 11. 為版本號欄位添加索引（提升樂觀鎖性能）
CREATE INDEX IF NOT EXISTS idx_orders_version ON orders(version);
CREATE INDEX IF NOT EXISTS idx_order_items_version ON order_items(version);
CREATE INDEX IF NOT EXISTS idx_order_workflows_version ON order_workflows(version);

-- 12. 為時間戳記欄位添加索引（提升查詢性能）
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at);

-- 13. 驗證遷移結果
DO $$
DECLARE
    orders_count INTEGER;
    order_items_count INTEGER;
BEGIN
    -- 檢查 orders 表
    SELECT COUNT(*) INTO orders_count FROM orders WHERE version IS NOT NULL;
    RAISE NOTICE 'Orders table: % records have version numbers', orders_count;
    
    -- 檢查 order_items 表
    SELECT COUNT(*) INTO order_items_count FROM order_items WHERE version IS NOT NULL;
    RAISE NOTICE 'Order items table: % records have version numbers', order_items_count;
    
    -- 檢查是否有記錄缺少時間戳記
    SELECT COUNT(*) INTO orders_count FROM orders WHERE created_at IS NULL OR updated_at IS NULL;
    IF orders_count > 0 THEN
        RAISE WARNING 'Found % orders without proper timestamps', orders_count;
    END IF;
END $$;

-- 14. 創建樂觀鎖測試函數（可選，用於驗證樂觀鎖機制）
CREATE OR REPLACE FUNCTION test_optimistic_locking()
RETURNS TEXT AS $$
DECLARE
    test_order_id TEXT := 'test-order-' || extract(epoch from now());
    initial_version BIGINT;
    updated_version BIGINT;
BEGIN
    -- 創建測試訂單
    INSERT INTO orders (id, customer_id, shipping_address, status, total_amount, currency, effective_amount, version)
    VALUES (test_order_id, 'test-customer', 'Test Address', 'PENDING', 100.00, 'TWD', 100.00, 0);
    
    -- 獲取初始版本號
    SELECT version INTO initial_version FROM orders WHERE id = test_order_id;
    
    -- 更新訂單狀態
    UPDATE orders SET status = 'CONFIRMED' WHERE id = test_order_id AND version = initial_version;
    
    -- 檢查版本號是否自動遞增
    SELECT version INTO updated_version FROM orders WHERE id = test_order_id;
    
    -- 清理測試數據
    DELETE FROM orders WHERE id = test_order_id;
    
    -- 返回測試結果
    IF updated_version > initial_version THEN
        RETURN 'SUCCESS: Optimistic locking is working correctly. Version incremented from ' || 
               initial_version || ' to ' || updated_version;
    ELSE
        RETURN 'ERROR: Optimistic locking may not be working. Version did not increment.';
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 執行樂觀鎖測試（可選）
-- SELECT test_optimistic_locking();

COMMIT;

-- 遷移完成提示
DO $$
BEGIN
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Aurora 樂觀鎖遷移完成！';
    RAISE NOTICE '=================================================';
    RAISE NOTICE '已完成的操作:';
    RAISE NOTICE '1. 為 orders, order_items, order_workflows 表添加 version 欄位';
    RAISE NOTICE '2. 確保所有表都有 created_at 和 updated_at 欄位';
    RAISE NOTICE '3. 創建自動更新 updated_at 的觸發器';
    RAISE NOTICE '4. 為版本號和時間戳記欄位添加索引';
    RAISE NOTICE '5. 初始化現有記錄的版本號和時間戳記';
    RAISE NOTICE '';
    RAISE NOTICE '下一步:';
    RAISE NOTICE '1. 更新 JPA 實體類繼承 BaseOptimisticLockingEntity';
    RAISE NOTICE '2. 更新應用服務使用 OptimisticLockingRetryService';
    RAISE NOTICE '3. 執行完整的測試驗證';
    RAISE NOTICE '=================================================';
END $$;