-- ========================================
-- 創建支付方式折扣系統表 - 支援 Payment Context
-- ========================================

-- 創建支付方式表
CREATE TABLE payment_methods (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- CREDIT_CARD, DEBIT_CARD, DIGITAL_WALLET, BANK_TRANSFER, CASH_ON_DELIVERY, INSTALLMENT
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    processing_fee_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000, -- 手續費率
    min_processing_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- 最低手續費
    max_processing_fee DECIMAL(10,2), -- 最高手續費
    min_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- 最低消費金額
    max_amount DECIMAL(10,2), -- 最高消費金額
    icon_url VARCHAR(500), -- 圖示URL
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
);

-- 創建支付方式折扣規則表
CREATE TABLE payment_method_discounts (
    id VARCHAR(36) PRIMARY KEY,
    payment_method_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT, CASHBACK
    discount_value DECIMAL(10,2) NOT NULL, -- 折扣值（百分比或固定金額）
    min_spend DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- 最低消費門檻
    max_discount DECIMAL(10,2), -- 最高折扣金額
    applicable_membership_levels TEXT, -- JSON array of membership levels
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    usage_limit INTEGER, -- 使用次數限制
    used_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE CASCADE,
    CHECK (discount_value > 0),
    CHECK (end_date IS NULL OR end_date > start_date),
    CHECK (usage_limit IS NULL OR usage_limit > 0),
    CHECK (used_count >= 0),
    CHECK (usage_limit IS NULL OR used_count <= usage_limit)
);

-- 創建支付方式限制表
CREATE TABLE payment_method_restrictions (
    id VARCHAR(36) PRIMARY KEY,
    payment_method_id VARCHAR(36) NOT NULL,
    restriction_type VARCHAR(50) NOT NULL, -- PRODUCT_CATEGORY, CUSTOMER_TYPE, TIME_PERIOD, REGION
    restriction_value TEXT NOT NULL, -- JSON data for restriction details
    is_blacklist BOOLEAN NOT NULL DEFAULT TRUE, -- true=黑名單, false=白名單
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE CASCADE
);

-- 創建支付方式使用記錄表
CREATE TABLE payment_method_usage_history (
    id VARCHAR(36) PRIMARY KEY,
    payment_id VARCHAR(36) NOT NULL, -- 關聯到 payments 表
    payment_method_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    order_amount DECIMAL(10,2) NOT NULL,
    processing_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(10,2) NOT NULL,
    discount_rule_id VARCHAR(36), -- 使用的折扣規則
    transaction_reference VARCHAR(200), -- 交易參考號
    status VARCHAR(20) NOT NULL, -- SUCCESS, FAILED, PENDING, CANCELLED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    FOREIGN KEY (discount_rule_id) REFERENCES payment_method_discounts(id)
);

-- 創建分期付款方案表
CREATE TABLE installment_plans (
    id VARCHAR(36) PRIMARY KEY,
    payment_method_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    installment_count INTEGER NOT NULL, -- 分期期數
    interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000, -- 利率
    handling_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- 手續費
    min_amount DECIMAL(10,2) NOT NULL, -- 最低分期金額
    max_amount DECIMAL(10,2), -- 最高分期金額
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id) ON DELETE CASCADE,
    CHECK (installment_count > 1),
    CHECK (interest_rate >= 0),
    CHECK (min_amount > 0),
    CHECK (max_amount IS NULL OR max_amount > min_amount)
);

-- 創建索引
CREATE INDEX idx_payment_methods_type ON payment_methods(type);
CREATE INDEX idx_payment_methods_active ON payment_methods(is_active);
CREATE INDEX idx_payment_methods_display_order ON payment_methods(display_order);
CREATE INDEX idx_payment_method_discounts_method_id ON payment_method_discounts(payment_method_id);
CREATE INDEX idx_payment_method_discounts_active ON payment_method_discounts(is_active);
CREATE INDEX idx_payment_method_discounts_dates ON payment_method_discounts(start_date, end_date);
CREATE INDEX idx_payment_method_restrictions_method_id ON payment_method_restrictions(payment_method_id);
CREATE INDEX idx_payment_method_restrictions_type ON payment_method_restrictions(restriction_type);
CREATE INDEX idx_installment_plans_method_id ON installment_plans(payment_method_id);
CREATE INDEX idx_installment_plans_active ON installment_plans(is_active);
CREATE INDEX idx_payment_method_usage_payment_id ON payment_method_usage_history(payment_id);
CREATE INDEX idx_payment_method_usage_customer_id ON payment_method_usage_history(customer_id);
CREATE INDEX idx_payment_method_usage_created_at ON payment_method_usage_history(created_at);