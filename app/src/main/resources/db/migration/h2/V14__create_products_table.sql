-- 創建產品表
CREATE TABLE products (
    product_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'TWD',
    category VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 插入一些測試產品數據
INSERT INTO products (product_id, name, description, price, currency, category, status) VALUES
('PROD-001', 'iPhone 15 Pro', '最新款 iPhone，配備 A17 Pro 晶片', 35900.00, 'TWD', 'ELECTRONICS', 'ACTIVE'),
('PROD-002', 'MacBook Air M3', '輕薄筆記型電腦，搭載 M3 晶片', 36900.00, 'TWD', 'ELECTRONICS', 'ACTIVE'),
('PROD-003', 'AirPods Pro', '主動降噪無線耳機', 7490.00, 'TWD', 'ELECTRONICS', 'ACTIVE'),
('PROD-004', '咖啡豆 - 藍山', '牙買加藍山咖啡豆 250g', 1200.00, 'TWD', 'FOOD', 'ACTIVE'),
('PROD-005', '有機綠茶', '台灣高山有機綠茶 100g', 450.00, 'TWD', 'FOOD', 'ACTIVE'),
('PROD-006', '運動鞋', '透氣運動跑鞋', 2890.00, 'TWD', 'CLOTHING', 'ACTIVE'),
('PROD-007', 'T恤', '純棉短袖T恤', 590.00, 'TWD', 'CLOTHING', 'ACTIVE'),
('PROD-008', '筆記本', 'A5 精裝筆記本', 280.00, 'TWD', 'STATIONERY', 'ACTIVE');

-- 為現有的庫存記錄添加對應的產品數據
-- 假設庫存表中的 product_id 對應產品表的 product_id
-- H2 不支持 ON CONFLICT，所以使用 MERGE 語句
MERGE INTO products (product_id, name, description, price, currency, category, status)
KEY (product_id)
SELECT DISTINCT 
    i.product_id,
    i.product_name,
    CONCAT('庫存商品: ', i.product_name),
    100.00, -- 默認價格
    'TWD',
    'GENERAL',
    'ACTIVE'
FROM inventories i;

-- 創建索引
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_name ON products(name);