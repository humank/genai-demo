-- ========================================
-- 將客戶 ID 更新為 UUID 格式
-- ========================================

-- 創建客戶 ID 映射表（臨時使用）
CREATE TEMPORARY TABLE customer_id_mapping (
    old_id VARCHAR(50),
    new_id VARCHAR(36)
);

-- 插入客戶 ID 映射
INSERT INTO customer_id_mapping (old_id, new_id) VALUES
('CUST-001', '660e8400-e29b-41d4-a716-446655440001'),
('CUST-002', '660e8400-e29b-41d4-a716-446655440002'),
('CUST-003', '660e8400-e29b-41d4-a716-446655440003'),
('CUST-004', '660e8400-e29b-41d4-a716-446655440004'),
('CUST-005', '660e8400-e29b-41d4-a716-446655440005'),
('CUST-006', '660e8400-e29b-41d4-a716-446655440006'),
('CUST-007', '660e8400-e29b-41d4-a716-446655440007'),
('CUST-008', '660e8400-e29b-41d4-a716-446655440008'),
('CUST-009', '660e8400-e29b-41d4-a716-446655440009'),
('CUST-010', '660e8400-e29b-41d4-a716-446655440010');

-- 更新訂單表中的客戶 ID
UPDATE orders SET customer_id = (
    SELECT new_id FROM customer_id_mapping WHERE old_id = orders.customer_id
) WHERE customer_id IN (SELECT old_id FROM customer_id_mapping);
