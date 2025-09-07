-- ========================================
-- 修復配送表結構以匹配 JpaDeliveryEntity
-- ========================================

-- 由於 JpaDeliveryEntity 期望的欄位與現有的 deliveries 表不匹配，
-- 我們需要添加缺少的欄位並調整現有欄位

-- 添加 JpaDeliveryEntity 需要的欄位
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS shipping_address VARCHAR(500);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS delivery_person_name VARCHAR(100);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS delivery_person_contact VARCHAR(50);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS estimated_delivery_time TIMESTAMP;
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS actual_delivery_time TIMESTAMP;
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS refusal_reason VARCHAR(500);
ALTER TABLE deliveries ADD COLUMN IF NOT EXISTS delay_reason VARCHAR(500);

-- 更新現有記錄，將 delivery_address 複製到 shipping_address
UPDATE deliveries SET shipping_address = delivery_address WHERE shipping_address IS NULL;

-- 將 estimated_delivery_date 和 actual_delivery_date 轉換為 TIMESTAMP 類型的對應欄位
-- 只有當原始欄位存在且新欄位為空時才進行轉換
UPDATE deliveries SET 
    estimated_delivery_time = CASE 
        WHEN estimated_delivery_date IS NOT NULL 
        THEN DATEADD('HOUR', 0, estimated_delivery_date)
        ELSE NULL 
    END
WHERE estimated_delivery_time IS NULL AND estimated_delivery_date IS NOT NULL;

UPDATE deliveries SET 
    actual_delivery_time = CASE 
        WHEN actual_delivery_date IS NOT NULL 
        THEN DATEADD('HOUR', 0, actual_delivery_date)
        ELSE NULL 
    END
WHERE actual_delivery_time IS NULL AND actual_delivery_date IS NOT NULL;

-- 創建索引
CREATE INDEX IF NOT EXISTS idx_deliveries_delivery_person ON deliveries(delivery_person_name);
CREATE INDEX IF NOT EXISTS idx_deliveries_estimated_time ON deliveries(estimated_delivery_time);
CREATE INDEX IF NOT EXISTS idx_deliveries_actual_time ON deliveries(actual_delivery_time);