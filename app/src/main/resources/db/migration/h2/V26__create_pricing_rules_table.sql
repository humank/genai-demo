-- ========================================
-- 創建定價規則表 - 支援 Pricing Context
-- ========================================

-- 創建定價規則表
CREATE TABLE pricing_rules (
    id VARCHAR(255) PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    promotion_id VARCHAR(255),
    base_price DECIMAL(19,2) NOT NULL,
    discount_percentage DOUBLE NOT NULL DEFAULT 0.0,
    discount_amount DECIMAL(19,2),
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    product_category VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 創建索引
CREATE INDEX idx_pricing_rules_product_id ON pricing_rules(product_id);
CREATE INDEX idx_pricing_rules_promotion_id ON pricing_rules(promotion_id);
CREATE INDEX idx_pricing_rules_product_category ON pricing_rules(product_category);
CREATE INDEX idx_pricing_rules_is_active ON pricing_rules(is_active);
CREATE INDEX idx_pricing_rules_valid_period ON pricing_rules(valid_from, valid_to);

-- 插入一些測試定價規則數據
INSERT INTO pricing_rules (id, product_id, promotion_id, base_price, discount_percentage, discount_amount, valid_from, valid_to, product_category, is_active) VALUES
('RULE-001', 'PROD-001', 'PROMO-001', 35900.00, 10.0, 3590.00, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'ELECTRONICS', TRUE),
('RULE-002', 'PROD-002', 'PROMO-002', 36900.00, 5.0, 1845.00, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'ELECTRONICS', TRUE),
('RULE-003', 'PROD-003', NULL, 7490.00, 0.0, NULL, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'ELECTRONICS', TRUE),
('RULE-004', 'PROD-004', 'PROMO-003', 1200.00, 15.0, 180.00, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'FOOD', TRUE),
('RULE-005', 'PROD-005', NULL, 450.00, 0.0, NULL, '2024-01-01 00:00:00', '2024-12-31 23:59:59', 'FOOD', TRUE);