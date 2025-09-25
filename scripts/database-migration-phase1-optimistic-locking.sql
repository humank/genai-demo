-- Aurora 樂觀鎖數據庫遷移腳本 - 第一階段
-- 建立日期: 2025年9月24日 下午12:06 (台北時間)
-- 需求: 1.1 - 並發控制機制全面重構
-- 階段: 第一階段 - 高優先級實體

-- 為第一階段遷移的實體表添加樂觀鎖支援

BEGIN;

-- 創建通用的 updated_at 更新函數（如果不存在）
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 1. 訂單表 (orders)
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 確保時間戳記欄位存在
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 初始化現有記錄
UPDATE orders SET version = 0 WHERE version IS NULL;
UPDATE orders SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

-- 創建觸發器
DROP TRIGGER IF EXISTS update_orders_updated_at ON orders;
CREATE TRIGGER update_orders_updated_at 
    BEFORE UPDATE ON orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 2. 訂單項目表 (order_items)
ALTER TABLE order_items 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE order_items SET version = 0 WHERE version IS NULL;
UPDATE order_items SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

DROP TRIGGER IF EXISTS update_order_items_updated_at ON order_items;
CREATE TRIGGER update_order_items_updated_at 
    BEFORE UPDATE ON order_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. 庫存表 (inventories)
ALTER TABLE inventories 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 確保時間戳記欄位存在
ALTER TABLE inventories 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE inventories SET version = 0 WHERE version IS NULL;
UPDATE inventories SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

DROP TRIGGER IF EXISTS update_inventories_updated_at ON inventories;
CREATE TRIGGER update_inventories_updated_at 
    BEFORE UPDATE ON inventories 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 4. 庫存預留表 (inventory_reservations)
ALTER TABLE inventory_reservations 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE inventory_reservations SET version = 0 WHERE version IS NULL;
UPDATE inventory_reservations SET 
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

DROP TRIGGER IF EXISTS update_inventory_reservations_updated_at ON inventory_reservations;
CREATE TRIGGER update_inventory_reservations_updated_at 
    BEFORE UPDATE ON inventory_reservations 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 5. 購物車表 (shopping_carts)
ALTER TABLE shopping_carts 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 確保時間戳記欄位存在
ALTER TABLE shopping_carts 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE shopping_carts SET version = 0 WHERE version IS NULL;
UPDATE shopping_carts SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

DROP TRIGGER IF EXISTS update_shopping_carts_updated_at ON shopping_carts;
CREATE TRIGGER update_shopping_carts_updated_at 
    BEFORE UPDATE ON shopping_carts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 6. 購物車項目表 (cart_items)
ALTER TABLE cart_items 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 確保 updated_at 欄位存在（保留 added_at）
ALTER TABLE cart_items 
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE cart_items SET version = 0 WHERE version IS NULL;
UPDATE cart_items SET 
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE updated_at IS NULL;

DROP TRIGGER IF EXISTS update_cart_items_updated_at ON cart_items;
CREATE TRIGGER update_cart_items_updated_at 
    BEFORE UPDATE ON cart_items 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 7. 支付表 (payments)
ALTER TABLE payments 
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 確保時間戳記欄位存在
ALTER TABLE payments 
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE payments SET version = 0 WHERE version IS NULL;
UPDATE payments SET 
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE created_at IS NULL OR updated_at IS NULL;

DROP TRIGGER IF EXISTS update_payments_updated_at ON payments;
CREATE TRIGGER update_payments_updated_at 
    BEFORE UPDATE ON payments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 為版本號欄位添加索引（提升樂觀鎖性能）
CREATE INDEX IF NOT EXISTS idx_orders_version ON orders(version);
CREATE INDEX IF NOT EXISTS idx_order_items_version ON order_items(version);
CREATE INDEX IF NOT EXISTS idx_inventories_version ON inventories(version);
CREATE INDEX IF NOT EXISTS idx_inventory_reservations_version ON inventory_reservations(version);
CREATE INDEX IF NOT EXISTS idx_shopping_carts_version ON shopping_carts(version);
CREATE INDEX IF NOT EXISTS idx_cart_items_version ON cart_items(version);
CREATE INDEX IF NOT EXISTS idx_payments_version ON payments(version);

-- 為時間戳記欄位添加索引（提升查詢性能）
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_updated_at ON orders(updated_at);
CREATE INDEX IF NOT EXISTS idx_inventories_updated_at ON inventories(updated_at);
CREATE INDEX IF NOT EXISTS idx_shopping_carts_updated_at ON shopping_carts(updated_at);
CREATE INDEX IF NOT EXISTS idx_payments_updated_at ON payments(updated_at);

-- 驗證遷移結果
DO $$
DECLARE
    table_name TEXT;
    record_count INTEGER;
    tables_to_check TEXT[] := ARRAY['orders', 'order_items', 'inventories', 'inventory_reservations', 'shopping_carts', 'cart_items', 'payments'];
BEGIN
    RAISE NOTICE '=== 第一階段樂觀鎖遷移驗證 ===';
    
    FOREACH table_name IN ARRAY tables_to_check
    LOOP
        EXECUTE format('SELECT COUNT(*) FROM %I WHERE version IS NOT NULL', table_name) INTO record_count;
        RAISE NOTICE '表 %: % 條記錄已設置版本號', table_name, record_count;
    END LOOP;
    
    RAISE NOTICE '=== 驗證完成 ===';
END $$;

-- 創建樂觀鎖測試函數
CREATE OR REPLACE FUNCTION test_optimistic_locking_phase1()
RETURNS TEXT AS $$
DECLARE
    test_order_id TEXT := 'test-order-' || extract(epoch from now());
    initial_version BIGINT;
    updated_version BIGINT;
    result TEXT := '';
BEGIN
    -- 測試訂單表的樂觀鎖
    INSERT INTO orders (id, customer_id, shipping_address, status, total_amount, currency, effective_amount, version)
    VALUES (test_order_id, 'test-customer', 'Test Address', 'PENDING', 100.00, 'TWD', 100.00, 0);
    
    SELECT version INTO initial_version FROM orders WHERE id = test_order_id;
    
    UPDATE orders SET status = 'CONFIRMED' WHERE id = test_order_id AND version = initial_version;
    
    SELECT version INTO updated_version FROM orders WHERE id = test_order_id;
    
    DELETE FROM orders WHERE id = test_order_id;
    
    IF updated_version > initial_version THEN
        result := 'SUCCESS: 第一階段樂觀鎖機制運作正常。版本號從 ' || 
                 initial_version || ' 遞增到 ' || updated_version;
    ELSE
        result := 'ERROR: 樂觀鎖機制可能有問題。版本號未遞增。';
    END IF;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

COMMIT;

-- 執行測試（可選）
-- SELECT test_optimistic_locking_phase1();

-- 遷移完成提示
DO $$
BEGIN
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Aurora 樂觀鎖第一階段遷移完成！';
    RAISE NOTICE '=================================================';
    RAISE NOTICE '已完成的操作:';
    RAISE NOTICE '1. 為 7 個高優先級表添加 version 欄位';
    RAISE NOTICE '2. 確保所有表都有 created_at 和 updated_at 欄位';
    RAISE NOTICE '3. 創建自動更新 updated_at 的觸發器';
    RAISE NOTICE '4. 為版本號和時間戳記欄位添加索引';
    RAISE NOTICE '5. 初始化現有記錄的版本號和時間戳記';
    RAISE NOTICE '';
    RAISE NOTICE '遷移的表:';
    RAISE NOTICE '- orders (訂單)';
    RAISE NOTICE '- order_items (訂單項目)';
    RAISE NOTICE '- inventories (庫存)';
    RAISE NOTICE '- inventory_reservations (庫存預留)';
    RAISE NOTICE '- shopping_carts (購物車)';
    RAISE NOTICE '- cart_items (購物車項目)';
    RAISE NOTICE '- payments (支付)';
    RAISE NOTICE '';
    RAISE NOTICE '下一步:';
    RAISE NOTICE '1. 驗證應用程式與新的樂觀鎖機制相容';
    RAISE NOTICE '2. 更新應用服務使用 OptimisticLockingRetryService';
    RAISE NOTICE '3. 執行完整的測試驗證';
    RAISE NOTICE '4. 繼續第二階段中優先級實體遷移';
    RAISE NOTICE '=================================================';
END $$;