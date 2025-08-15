-- ========================================
-- 創建促銷活動表 - 支援促銷聚合根
-- ========================================

-- 創建促銷活動表
CREATE TABLE promotions (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- PERCENTAGE_DISCOUNT, FIXED_AMOUNT_DISCOUNT, BUY_ONE_GET_ONE, FLASH_SALE
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, PAUSED, EXPIRED, CANCELLED
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    max_usage_count INTEGER, -- 最大使用次數，NULL表示無限制
    current_usage_count INTEGER NOT NULL DEFAULT 0,
    max_usage_per_customer INTEGER, -- 每個客戶最大使用次數
    minimum_order_amount DECIMAL(10,2), -- 最低訂單金額
    applicable_membership_levels TEXT, -- JSON array of applicable membership levels
    applicable_product_ids TEXT, -- JSON array of applicable product IDs
    applicable_categories TEXT, -- JSON array of applicable categories
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (end_date > start_date),
    CHECK (max_usage_count IS NULL OR max_usage_count > 0),
    CHECK (max_usage_per_customer IS NULL OR max_usage_per_customer > 0),
    CHECK (minimum_order_amount IS NULL OR minimum_order_amount >= 0)
);

-- 創建促銷規則表
CREATE TABLE promotion_rules (
    id VARCHAR(36) PRIMARY KEY,
    promotion_id VARCHAR(36) NOT NULL,
    rule_type VARCHAR(50) NOT NULL, -- PERCENTAGE_DISCOUNT, FIXED_AMOUNT_DISCOUNT, BUY_ONE_GET_ONE, FLASH_SALE
    rule_data TEXT NOT NULL, -- JSON data containing rule-specific parameters
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE
);

-- 創建優惠券表
CREATE TABLE coupons (
    id VARCHAR(36) PRIMARY KEY,
    promotion_id VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    customer_id VARCHAR(36), -- NULL表示公開優惠券
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, USED, EXPIRED, CANCELLED
    usage_count INTEGER NOT NULL DEFAULT 0,
    max_usage_count INTEGER NOT NULL DEFAULT 1,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    used_at TIMESTAMP,
    FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CHECK (max_usage_count > 0),
    CHECK (expires_at IS NULL OR expires_at > issued_at)
);

-- 創建促銷使用記錄表
CREATE TABLE promotion_usage_history (
    id VARCHAR(36) PRIMARY KEY,
    promotion_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(50),
    coupon_id VARCHAR(36),
    discount_amount DECIMAL(10,2) NOT NULL CHECK (discount_amount >= 0),
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE SET NULL
);

-- 創建索引
CREATE INDEX idx_promotions_status ON promotions(status);
CREATE INDEX idx_promotions_type ON promotions(type);
CREATE INDEX idx_promotions_start_date ON promotions(start_date);
CREATE INDEX idx_promotions_end_date ON promotions(end_date);
CREATE INDEX idx_promotions_priority ON promotions(priority);
CREATE INDEX idx_promotion_rules_promotion_id ON promotion_rules(promotion_id);
CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_customer_id ON coupons(customer_id);
CREATE INDEX idx_coupons_status ON coupons(status);
CREATE INDEX idx_coupons_expires_at ON coupons(expires_at);
CREATE INDEX idx_promotion_usage_history_promotion_id ON promotion_usage_history(promotion_id);
CREATE INDEX idx_promotion_usage_history_customer_id ON promotion_usage_history(customer_id);
CREATE INDEX idx_promotion_usage_history_used_at ON promotion_usage_history(used_at);