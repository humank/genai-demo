-- ========================================
-- 插入支付方式折扣系統測試資料
-- ========================================

-- 插入支付方式資料
INSERT INTO payment_methods (id, name, type, description, is_active, processing_fee_rate, min_processing_fee, max_processing_fee, min_amount, max_amount, display_order) VALUES
('pm-credit-card', '信用卡', 'CREDIT_CARD', '支援各大銀行信用卡付款', true, 0.0280, 5.00, 500.00, 100.00, 1000000.00, 1),
('pm-debit-card', '金融卡', 'DEBIT_CARD', '支援各大銀行金融卡付款', true, 0.0150, 3.00, 200.00, 100.00, 500000.00, 2),
('pm-line-pay', 'LINE Pay', 'DIGITAL_WALLET', 'LINE Pay 數位錢包付款', true, 0.0200, 2.00, 100.00, 50.00, 100000.00, 3),
('pm-apple-pay', 'Apple Pay', 'DIGITAL_WALLET', 'Apple Pay 行動支付', true, 0.0180, 2.00, 100.00, 50.00, 100000.00, 4),
('pm-google-pay', 'Google Pay', 'DIGITAL_WALLET', 'Google Pay 行動支付', true, 0.0180, 2.00, 100.00, 50.00, 100000.00, 5),
('pm-bank-transfer', '銀行轉帳', 'BANK_TRANSFER', 'ATM或網路銀行轉帳', true, 0.0000, 0.00, 0.00, 100.00, 1000000.00, 6),
('pm-cash-on-delivery', '貨到付款', 'CASH_ON_DELIVERY', '商品送達時現金付款', true, 0.0000, 30.00, 30.00, 500.00, 50000.00, 7),
('pm-installment', '分期付款', 'INSTALLMENT', '信用卡分期付款服務', true, 0.0300, 10.00, 1000.00, 3000.00, 500000.00, 8);

-- 插入支付方式折扣規則
INSERT INTO payment_method_discounts (id, payment_method_id, name, description, discount_type, discount_value, min_spend, max_discount, applicable_membership_levels, start_date, end_date, usage_limit, is_active) VALUES
-- 信用卡折扣
('pmd-001', 'pm-credit-card', '信用卡首刷優惠', '新戶首次使用信用卡付款享9折優惠', 'PERCENTAGE', 10.00, 1000.00, 500.00, '["STANDARD", "PREMIUM", "VIP"]', '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, true),
('pmd-002', 'pm-credit-card', 'VIP信用卡專享', 'VIP會員使用信用卡付款享額外5%折扣', 'PERCENTAGE', 5.00, 2000.00, 1000.00, '["VIP"]', '2024-01-01 00:00:00', '2024-12-31 23:59:59', NULL, true),

-- LINE Pay 折扣
('pmd-003', 'pm-line-pay', 'LINE Pay 回饋', '使用LINE Pay付款享2%回饋', 'CASHBACK', 2.00, 500.00, 200.00, '["STANDARD", "PREMIUM", "VIP"]', '2024-01-01 00:00:00', '2024-12-31 23:59:59', NULL, true),
('pmd-004', 'pm-line-pay', 'LINE Pay 滿千折百', '使用LINE Pay滿1000折100', 'FIXED_AMOUNT', 100.00, 1000.00, 100.00, '["STANDARD", "PREMIUM", "VIP"]', '2024-08-01 00:00:00', '2024-08-31 23:59:59', 100, true),

-- Apple Pay 折扣
('pmd-005', 'pm-apple-pay', 'Apple Pay 新用戶', '首次使用Apple Pay享8折優惠', 'PERCENTAGE', 20.00, 800.00, 300.00, '["STANDARD", "PREMIUM", "VIP"]', '2024-01-01 00:00:00', '2024-12-31 23:59:59', 1, true),

-- 銀行轉帳折扣
('pmd-006', 'pm-bank-transfer', '轉帳付款優惠', '使用銀行轉帳付款享50元折扣', 'FIXED_AMOUNT', 50.00, 2000.00, 50.00, '["STANDARD", "PREMIUM", "VIP"]', '2024-01-01 00:00:00', '2024-12-31 23:59:59', NULL, true);

-- 插入支付方式限制
INSERT INTO payment_method_restrictions (id, payment_method_id, restriction_type, restriction_value, is_blacklist) VALUES
-- 貨到付款限制：不支援某些商品類別
('pmr-001', 'pm-cash-on-delivery', 'PRODUCT_CATEGORY', '["DIGITAL_GOODS", "GIFT_CARDS"]', true),
-- 貨到付款限制：僅支援特定地區
('pmr-002', 'pm-cash-on-delivery', 'REGION', '["TPE", "NTPC", "TAO", "TC"]', false),
-- 分期付款限制：僅VIP和PREMIUM會員
('pmr-003', 'pm-installment', 'CUSTOMER_TYPE', '["VIP", "PREMIUM"]', false),
-- Apple Pay 限制：週末不可使用（示例）
('pmr-004', 'pm-apple-pay', 'TIME_PERIOD', '{"excluded_days": [6, 7]}', true);

-- 插入分期付款方案
INSERT INTO installment_plans (id, payment_method_id, name, installment_count, interest_rate, handling_fee, min_amount, max_amount, is_active) VALUES
('ip-001', 'pm-installment', '3期0利率', 3, 0.0000, 0.00, 3000.00, 50000.00, true),
('ip-002', 'pm-installment', '6期0利率', 6, 0.0000, 100.00, 6000.00, 100000.00, true),
('ip-003', 'pm-installment', '12期低利率', 12, 0.0299, 200.00, 12000.00, 200000.00, true),
('ip-004', 'pm-installment', '24期分期', 24, 0.0599, 500.00, 24000.00, 500000.00, true);

-- 插入支付方式使用記錄（模擬歷史數據）
INSERT INTO payment_method_usage_history (id, payment_id, payment_method_id, customer_id, order_amount, processing_fee, discount_amount, final_amount, discount_rule_id, transaction_reference, status) VALUES
('pmuh-001', 'PAY-2024-01-001', 'pm-credit-card', '660e8400-e29b-41d4-a716-446655440001', 35900.00, 100.52, 0.00, 36000.52, NULL, 'CC-TXN-20240102-001', 'SUCCESS'),
('pmuh-002', 'PAY-2024-01-002', 'pm-line-pay', '660e8400-e29b-41d4-a716-446655440002', 8990.00, 17.98, 179.80, 8828.18, 'pmd-003', 'LP-TXN-20240102-002', 'SUCCESS'),
('pmuh-003', 'PAY-2024-01-003', 'pm-apple-pay', '660e8400-e29b-41d4-a716-446655440003', 12450.00, 22.41, 2490.00, 9982.41, 'pmd-005', 'AP-TXN-20240103-003', 'SUCCESS'),
('pmuh-004', 'PAY-2024-01-004', 'pm-bank-transfer', '660e8400-e29b-41d4-a716-446655440004', 7890.00, 0.00, 50.00, 7840.00, 'pmd-006', 'BT-TXN-20240103-004', 'SUCCESS'),
('pmuh-005', 'PAY-2024-01-005', 'pm-cash-on-delivery', '660e8400-e29b-41d4-a716-446655440005', 3680.00, 30.00, 0.00, 3710.00, NULL, 'COD-TXN-20240104-005', 'SUCCESS');