-- ========================================
-- 創建客戶表 - 支援完整的消費者功能
-- ========================================

-- 創建客戶表
CREATE TABLE customers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    birth_date DATE,
    registration_date DATE NOT NULL DEFAULT CURRENT_DATE,
    membership_level VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    reward_points_balance INTEGER NOT NULL DEFAULT 0,
    reward_points_last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_enabled_types TEXT, -- JSON array of enabled notification types
    notification_enabled_channels TEXT, -- JSON array of enabled notification channels
    marketing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    address_city VARCHAR(100),
    address_district VARCHAR(100),
    address_street VARCHAR(255),
    address_postal_code VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
);

-- 創建索引
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_membership_level ON customers(membership_level);
CREATE INDEX idx_customers_birth_date ON customers(birth_date);
CREATE INDEX idx_customers_registration_date ON customers(registration_date);
CREATE INDEX idx_customers_reward_points ON customers(reward_points_balance);

-- 插入現有客戶資料（基於訂單資料）
INSERT INTO customers (id, name, email, phone, membership_level, address_city, address_district, address_street) VALUES
('660e8400-e29b-41d4-a716-446655440001', '張小明', 'zhang.xiaoming@gmail.com', '0912345678', 'STANDARD', '台北市', '信義區', '信義路五段7號'),
('660e8400-e29b-41d4-a716-446655440002', '李小華', 'li.xiaohua@yahoo.com', '0923456789', 'PREMIUM', '新北市', '板橋區', '中山路一段161號'),
('660e8400-e29b-41d4-a716-446655440003', '王大明', 'wang.daming@hotmail.com', '0934567890', 'STANDARD', '桃園市', '中壢區', '中正路123號'),
('660e8400-e29b-41d4-a716-446655440004', '陳美麗', 'chen.meili@email.com', '0945678901', 'VIP', '台中市', '西屯區', '台灣大道三段99號'),
('660e8400-e29b-41d4-a716-446655440005', '林志偉', 'lin.zhiwei@gmail.com', '0956789012', 'STANDARD', '高雄市', '前金區', '中正四路211號'),
('660e8400-e29b-41d4-a716-446655440006', '黃淑芬', 'huang.shufen@yahoo.com', '0967890123', 'PREMIUM', '台南市', '中西區', '民權路二段158號'),
('660e8400-e29b-41d4-a716-446655440007', '吳建國', 'wu.jianguo@hotmail.com', '0978901234', 'STANDARD', '新竹市', '東區', '光復路二段101號'),
('660e8400-e29b-41d4-a716-446655440008', '劉雅婷', 'liu.yating@email.com', '0989012345', 'VIP', '基隆市', '仁愛區', '愛一路25號'),
('660e8400-e29b-41d4-a716-446655440009', '蔡俊傑', 'cai.junjie@gmail.com', '0990123456', 'PREMIUM', '宜蘭縣', '宜蘭市', '中山路三段145號'),
('660e8400-e29b-41d4-a716-446655440010', '楊怡君', 'yang.yijun@yahoo.com', '0901234567', 'STANDARD', '花蓮縣', '花蓮市', '中華路123號');

-- 更新現有訂單表，確保客戶ID一致性
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440001' WHERE customer_id = 'CUST-001';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440002' WHERE customer_id = 'CUST-002';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440003' WHERE customer_id = 'CUST-003';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440004' WHERE customer_id = 'CUST-004';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440005' WHERE customer_id = 'CUST-005';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440006' WHERE customer_id = 'CUST-006';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440007' WHERE customer_id = 'CUST-007';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440008' WHERE customer_id = 'CUST-008';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440009' WHERE customer_id = 'CUST-009';
UPDATE orders SET customer_id = '660e8400-e29b-41d4-a716-446655440010' WHERE customer_id = 'CUST-010';

-- 添加外鍵約束
ALTER TABLE orders ADD CONSTRAINT fk_orders_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id);