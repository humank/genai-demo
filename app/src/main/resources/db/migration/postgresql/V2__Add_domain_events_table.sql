-- V2__Add_domain_events_table.sql
-- 新增領域事件儲存表格 (如果使用 Event Sourcing)

-- 建立 domain_events 表格
CREATE TABLE IF NOT EXISTS domain_events (
    id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_data TEXT NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    occurred_on TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255)
);

-- 建立索引
CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate_id ON domain_events(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate_type ON domain_events(aggregate_type);
CREATE INDEX IF NOT EXISTS idx_domain_events_event_type ON domain_events(event_type);
CREATE INDEX IF NOT EXISTS idx_domain_events_occurred_on ON domain_events(occurred_on);
CREATE INDEX IF NOT EXISTS idx_domain_events_processed_at ON domain_events(processed_at);
CREATE INDEX IF NOT EXISTS idx_domain_events_correlation_id ON domain_events(correlation_id);

-- 建立複合索引 (查詢優化)
CREATE INDEX IF NOT EXISTS idx_domain_events_aggregate_composite 
    ON domain_events(aggregate_type, aggregate_id, occurred_on);

-- 建立 processed_events 表格 (用於冪等性檢查)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    handler_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms BIGINT,
    success BOOLEAN NOT NULL DEFAULT true,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0
);

-- 建立索引
CREATE INDEX IF NOT EXISTS idx_processed_events_handler_name ON processed_events(handler_name);
CREATE INDEX IF NOT EXISTS idx_processed_events_processed_at ON processed_events(processed_at);
CREATE INDEX IF NOT EXISTS idx_processed_events_success ON processed_events(success);

-- 建立複合索引
CREATE INDEX IF NOT EXISTS idx_processed_events_composite 
    ON processed_events(handler_name, processed_at, success);