-- ========================================
-- 創建訂單工作流表 - 支援 Order Workflow Context
-- ========================================

-- 創建訂單工作流表
CREATE TABLE order_workflows (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('CREATED', 'PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    cancellation_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 創建索引
CREATE INDEX idx_order_workflows_order_id ON order_workflows(order_id);
CREATE INDEX idx_order_workflows_status ON order_workflows(status);
CREATE INDEX idx_order_workflows_created_at ON order_workflows(created_at);

-- 插入一些測試訂單工作流數據
INSERT INTO order_workflows (id, order_id, status, cancellation_reason, created_at, updated_at) VALUES
('WF-001', 'ORDER-001', 'COMPLETED', NULL, '2024-01-01 10:00:00', '2024-01-01 15:00:00'),
('WF-002', 'ORDER-002', 'PROCESSING', NULL, '2024-01-02 11:00:00', '2024-01-02 11:30:00'),
('WF-003', 'ORDER-003', 'CANCELLED', '客戶要求取消', '2024-01-03 09:00:00', '2024-01-03 10:00:00'),
('WF-004', 'ORDER-004', 'CONFIRMED', NULL, '2024-01-04 14:00:00', '2024-01-04 14:15:00'),
('WF-005', 'ORDER-005', 'PENDING_PAYMENT', NULL, '2024-01-05 16:00:00', '2024-01-05 16:00:00');