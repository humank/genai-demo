-- V3__Add_performance_indexes.sql
-- 新增效能優化索引

-- 客戶相關的效能索引
CREATE INDEX IF NOT EXISTS idx_customers_created_at ON customers(created_at);
CREATE INDEX IF NOT EXISTS idx_customers_name_gin ON customers USING gin(to_tsvector('english', name));

-- 訂單相關的效能索引
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_orders_total_amount ON orders(total_amount);
CREATE INDEX IF NOT EXISTS idx_orders_customer_status ON orders(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_date_status ON orders(order_date, status);

-- 訂單項目的效能索引
CREATE INDEX IF NOT EXISTS idx_order_items_product_name ON order_items(product_name);
CREATE INDEX IF NOT EXISTS idx_order_items_total_price ON order_items(total_price);

-- 部分索引 (條件索引) - 只索引活躍資料
CREATE INDEX IF NOT EXISTS idx_orders_pending ON orders(order_date, customer_id) 
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_orders_completed ON orders(order_date, total_amount) 
    WHERE status = 'COMPLETED';

-- 複合索引用於常見查詢
CREATE INDEX IF NOT EXISTS idx_orders_customer_date_status 
    ON orders(customer_id, order_date DESC, status);

-- 統計查詢優化索引
CREATE INDEX IF NOT EXISTS idx_orders_monthly_stats 
    ON orders(DATE_TRUNC('month', order_date), status, total_amount);

-- 全文搜尋索引 (如果需要)
CREATE INDEX IF NOT EXISTS idx_customers_search 
    ON customers USING gin(to_tsvector('english', name || ' ' || email));

-- 新增表格統計資訊更新
-- 這有助於 PostgreSQL 查詢優化器做出更好的決策
ANALYZE customers;
ANALYZE orders;
ANALYZE order_items;
ANALYZE domain_events;
ANALYZE processed_events;