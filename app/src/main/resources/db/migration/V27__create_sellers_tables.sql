-- ========================================
-- 創建賣家系統表 - 支援 Seller Context
-- ========================================

-- 創建賣家基本資料表
CREATE TABLE sellers (
    seller_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 創建賣家檔案表
CREATE TABLE seller_profiles (
    seller_id VARCHAR(255) PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    business_address VARCHAR(255),
    business_license VARCHAR(255),
    description TEXT,
    rating DOUBLE PRECISION DEFAULT 0.0,
    total_reviews INTEGER DEFAULT 0,
    total_sales INTEGER DEFAULT 0,
    joined_at TIMESTAMP,
    last_active_at TIMESTAMP,
    is_verified BOOLEAN DEFAULT FALSE,
    verification_status VARCHAR(255),
    FOREIGN KEY (seller_id) REFERENCES sellers(seller_id) ON DELETE CASCADE
);

-- 創建索引
CREATE INDEX idx_sellers_email ON sellers(email);
CREATE INDEX idx_sellers_is_active ON sellers(is_active);
CREATE INDEX idx_seller_profiles_business_name ON seller_profiles(business_name);
CREATE INDEX idx_seller_profiles_rating ON seller_profiles(rating);
CREATE INDEX idx_seller_profiles_verification_status ON seller_profiles(verification_status);
CREATE INDEX idx_seller_profiles_is_verified ON seller_profiles(is_verified);

-- 插入一些測試賣家數據
INSERT INTO sellers (seller_id, name, email, phone, is_active) VALUES
('SELLER-001', '科技商城', 'tech@example.com', '02-1234-5678', TRUE),
('SELLER-002', '美食天地', 'food@example.com', '02-2345-6789', TRUE),
('SELLER-003', '時尚服飾', 'fashion@example.com', '02-3456-7890', TRUE),
('SELLER-004', '文具專賣', 'stationery@example.com', '02-4567-8901', TRUE);

INSERT INTO seller_profiles (seller_id, business_name, business_address, business_license, description, rating, total_reviews, total_sales, joined_at, last_active_at, is_verified, verification_status) VALUES
('SELLER-001', '科技商城有限公司', '台北市信義區信義路五段7號', 'BL-001-2024', '專營各種電子產品和3C商品', 4.5, 150, 500, '2024-01-01 10:00:00', '2024-08-19 15:00:00', TRUE, 'VERIFIED'),
('SELLER-002', '美食天地股份有限公司', '台北市大安區敦化南路二段216號', 'BL-002-2024', '提供優質食品和飲料', 4.2, 80, 200, '2024-01-15 14:30:00', '2024-08-19 12:00:00', TRUE, 'VERIFIED'),
('SELLER-003', '時尚服飾企業社', '台北市中山區南京東路三段303號', 'BL-003-2024', '時尚服飾和配件專賣', 4.0, 60, 150, '2024-02-01 09:00:00', '2024-08-18 18:00:00', FALSE, 'PENDING'),
('SELLER-004', '文具專賣店', '台北市松山區八德路四段123號', 'BL-004-2024', '各種辦公文具和學習用品', 4.3, 45, 100, '2024-02-15 11:00:00', '2024-08-19 10:00:00', TRUE, 'VERIFIED');