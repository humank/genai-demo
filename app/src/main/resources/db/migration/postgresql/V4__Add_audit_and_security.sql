-- V4__Add_audit_and_security.sql
-- 新增稽核和安全相關功能

-- 建立稽核日誌表格
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    operation VARCHAR(10) NOT NULL, -- INSERT, UPDATE, DELETE
    record_id VARCHAR(255) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    ip_address INET,
    user_agent TEXT
);

-- 建立稽核日誌索引
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit_log(table_name);
CREATE INDEX IF NOT EXISTS idx_audit_log_record_id ON audit_log(record_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON audit_log(changed_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_by ON audit_log(changed_by);
CREATE INDEX IF NOT EXISTS idx_audit_log_operation ON audit_log(operation);

-- 建立複合索引
CREATE INDEX IF NOT EXISTS idx_audit_log_composite 
    ON audit_log(table_name, record_id, changed_at DESC);

-- 建立稽核觸發器函數
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    old_data JSONB;
    new_data JSONB;
    changed_by_user VARCHAR(255);
BEGIN
    -- 獲取當前使用者 (從應用程式設定)
    changed_by_user := current_setting('app.current_user', true);
    
    IF TG_OP = 'DELETE' THEN
        old_data := to_jsonb(OLD);
        new_data := NULL;
        
        INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by)
        VALUES (TG_TABLE_NAME, TG_OP, OLD.id, old_data, new_data, changed_by_user);
        
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        old_data := to_jsonb(OLD);
        new_data := to_jsonb(NEW);
        
        -- 只有當資料真的改變時才記錄
        IF old_data != new_data THEN
            INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by)
            VALUES (TG_TABLE_NAME, TG_OP, NEW.id, old_data, new_data, changed_by_user);
        END IF;
        
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        old_data := NULL;
        new_data := to_jsonb(NEW);
        
        INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by)
        VALUES (TG_TABLE_NAME, TG_OP, NEW.id, old_data, new_data, changed_by_user);
        
        RETURN NEW;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 為主要表格建立稽核觸發器
CREATE TRIGGER customers_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON customers
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

CREATE TRIGGER orders_audit_trigger
    AFTER INSERT OR UPDATE OR DELETE ON orders
    FOR EACH ROW EXECUTE FUNCTION audit_trigger_function();

-- 建立資料保留政策表格
CREATE TABLE IF NOT EXISTS data_retention_policies (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL UNIQUE,
    retention_days INTEGER NOT NULL,
    archive_enabled BOOLEAN NOT NULL DEFAULT false,
    archive_table_name VARCHAR(255),
    last_cleanup_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 插入預設保留政策
INSERT INTO data_retention_policies (table_name, retention_days, archive_enabled) VALUES
('audit_log', 2555, true),  -- 7 年稽核保留
('domain_events', 365, true), -- 1 年事件保留
('processed_events', 90, false) -- 3 個月處理記錄
ON CONFLICT (table_name) DO NOTHING;

-- 建立資料清理函數
CREATE OR REPLACE FUNCTION cleanup_old_data()
RETURNS INTEGER AS $$
DECLARE
    policy RECORD;
    deleted_count INTEGER := 0;
    total_deleted INTEGER := 0;
BEGIN
    FOR policy IN SELECT * FROM data_retention_policies LOOP
        EXECUTE format('DELETE FROM %I WHERE created_at < NOW() - INTERVAL ''%s days''', 
                      policy.table_name, policy.retention_days);
        
        GET DIAGNOSTICS deleted_count = ROW_COUNT;
        total_deleted := total_deleted + deleted_count;
        
        -- 更新最後清理時間
        UPDATE data_retention_policies 
        SET last_cleanup_at = CURRENT_TIMESTAMP 
        WHERE table_name = policy.table_name;
        
        RAISE NOTICE 'Cleaned up % rows from %', deleted_count, policy.table_name;
    END LOOP;
    
    RETURN total_deleted;
END;
$$ LANGUAGE plpgsql;