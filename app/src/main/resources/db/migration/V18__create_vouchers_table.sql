-- ========================================
-- 創建超商優惠券表 - 支援優惠券聚合根
-- ========================================

-- 創建優惠券商品表
CREATE TABLE voucher_products (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    face_value DECIMAL(10,2) NOT NULL CHECK (face_value > 0),
    selling_price DECIMAL(10,2) NOT NULL CHECK (selling_price > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'TWD',
    validity_days INTEGER NOT NULL CHECK (validity_days > 0),
    category VARCHAR(50) NOT NULL, -- FOOD, BEVERAGE, RETAIL, SERVICE
    applicable_stores TEXT, -- JSON array of applicable store codes
    terms_and_conditions TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, DISCONTINUED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 
);

-- 創建優惠券實例表
CREATE TABLE vouchers (
    id VARCHAR(36) PRIMARY KEY,
    voucher_product_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    redemption_code VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, USED, EXPIRED, REPORTED_LOST, REISSUED, CANCELLED
    purchase_order_id VARCHAR(50), -- 購買此優惠券的訂單ID
    usage_order_id VARCHAR(50), -- 使用此優惠券的訂單ID
    face_value DECIMAL(10,2) NOT NULL CHECK (face_value > 0),
    purchase_price DECIMAL(10,2) NOT NULL CHECK (purchase_price > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'TWD',
    purchased_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    reported_lost_at TIMESTAMP,
    reissued_at TIMESTAMP,
    original_voucher_id VARCHAR(36), -- 如果是補發的優惠券，指向原始優惠券
    notes TEXT,
    FOREIGN KEY (voucher_product_id) REFERENCES voucher_products(id) ON DELETE RESTRICT,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (original_voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL,
    CHECK (expires_at > purchased_at),
    CHECK (used_at IS NULL OR used_at <= expires_at),
    CHECK (reported_lost_at IS NULL OR reported_lost_at >= purchased_at)
);

-- 創建優惠券使用記錄表
CREATE TABLE voucher_usage_history (
    id VARCHAR(36) PRIMARY KEY,
    voucher_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL, -- PURCHASED, USED, REPORTED_LOST, REISSUED, CANCELLED
    order_id VARCHAR(50),
    store_code VARCHAR(20),
    amount_used DECIMAL(10,2),
    currency VARCHAR(3) DEFAULT 'TWD',
    action_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE
);

-- 創建優惠券報失記錄表
CREATE TABLE voucher_loss_reports (
    id VARCHAR(36) PRIMARY KEY,
    voucher_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    report_reason VARCHAR(100) NOT NULL, -- LOST, STOLEN, DAMAGED, OTHER
    report_description TEXT,
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processor_id VARCHAR(36), -- 處理人員ID
    processing_result VARCHAR(50), -- APPROVED, REJECTED, PENDING
    reissued_voucher_id VARCHAR(36), -- 補發的新優惠券ID
    notes TEXT,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    FOREIGN KEY (reissued_voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL
);

-- 創建索引
CREATE INDEX idx_voucher_products_category ON voucher_products(category);
CREATE INDEX idx_voucher_products_status ON voucher_products(status);
CREATE INDEX idx_vouchers_customer_id ON vouchers(customer_id);
CREATE INDEX idx_vouchers_status ON vouchers(status);
CREATE INDEX idx_vouchers_redemption_code ON vouchers(redemption_code);
CREATE INDEX idx_vouchers_expires_at ON vouchers(expires_at);
CREATE INDEX idx_vouchers_voucher_product_id ON vouchers(voucher_product_id);
CREATE INDEX idx_voucher_usage_history_voucher_id ON voucher_usage_history(voucher_id);
CREATE INDEX idx_voucher_usage_history_action ON voucher_usage_history(action);
CREATE INDEX idx_voucher_usage_history_action_at ON voucher_usage_history(action_at);
CREATE INDEX idx_voucher_loss_reports_voucher_id ON voucher_loss_reports(voucher_id);
CREATE INDEX idx_voucher_loss_reports_customer_id ON voucher_loss_reports(customer_id);
CREATE INDEX idx_voucher_loss_reports_processing_result ON voucher_loss_reports(processing_result);