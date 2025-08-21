-- 創建支付表
CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20),
    transaction_id VARCHAR(100),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    can_retry BOOLEAN NOT NULL DEFAULT TRUE
);

-- 創建訂單ID索引，用於快速查詢
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);

-- 創建狀態索引，用於按狀態查詢
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);