-- PostgreSQL-specific observability tables for production environment
-- These tables support the AWS CDK observability integration with production optimizations

-- Event store table for domain events (PostgreSQL optimized)
CREATE TABLE IF NOT EXISTS event_store (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL, -- Use JSONB for better performance
    event_version INTEGER NOT NULL DEFAULT 1,
    occurred_on TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for event store (PostgreSQL optimized)
CREATE INDEX IF NOT EXISTS idx_event_store_aggregate ON event_store(aggregate_id, aggregate_type);
CREATE INDEX IF NOT EXISTS idx_event_store_type ON event_store(event_type);
CREATE INDEX IF NOT EXISTS idx_event_store_occurred ON event_store(occurred_on);
CREATE INDEX IF NOT EXISTS idx_event_store_data_gin ON event_store USING GIN (event_data); -- GIN index for JSONB

-- Processed events table for idempotency (PostgreSQL optimized)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    handler_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
);

-- Composite index for processed events
CREATE INDEX IF NOT EXISTS idx_processed_events_handler_time ON processed_events(handler_name, processed_at);

-- Application metrics table for production monitoring with partitioning
CREATE TABLE IF NOT EXISTS application_metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_type VARCHAR(20) NOT NULL, -- COUNTER, GAUGE, TIMER, HISTOGRAM
    tags JSONB, -- Use JSONB for structured tags
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (timestamp);

-- Create partitions for current and next month (PostgreSQL 10+ feature)
CREATE TABLE IF NOT EXISTS application_metrics_current PARTITION OF application_metrics
    FOR VALUES FROM (DATE_TRUNC('month', CURRENT_DATE)) TO (DATE_TRUNC('month', CURRENT_DATE + INTERVAL '1 month'));

CREATE TABLE IF NOT EXISTS application_metrics_next PARTITION OF application_metrics
    FOR VALUES FROM (DATE_TRUNC('month', CURRENT_DATE + INTERVAL '1 month')) TO (DATE_TRUNC('month', CURRENT_DATE + INTERVAL '2 months'));

-- Indexes for metrics (will be inherited by partitions)
CREATE INDEX IF NOT EXISTS idx_metrics_name_time ON application_metrics(metric_name, timestamp);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON application_metrics(timestamp);
CREATE INDEX IF NOT EXISTS idx_metrics_tags_gin ON application_metrics USING GIN (tags);

-- Health check results table with better data types
CREATE TABLE IF NOT EXISTS health_checks (
    id BIGSERIAL PRIMARY KEY,
    check_name VARCHAR(50) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('UP', 'DOWN', 'UNKNOWN')),
    details JSONB,
    check_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms BIGINT
);

-- Indexes for health checks
CREATE INDEX IF NOT EXISTS idx_health_checks_time ON health_checks(check_time);
CREATE INDEX IF NOT EXISTS idx_health_checks_name_status ON health_checks(check_name, status);
CREATE INDEX IF NOT EXISTS idx_health_checks_details_gin ON health_checks USING GIN (details);

-- Database connection monitoring (PostgreSQL specific with better constraints)
CREATE TABLE IF NOT EXISTS connection_metrics (
    id BIGSERIAL PRIMARY KEY,
    active_connections INTEGER NOT NULL CHECK (active_connections >= 0),
    idle_connections INTEGER NOT NULL CHECK (idle_connections >= 0),
    total_connections INTEGER NOT NULL CHECK (total_connections >= 0),
    max_connections INTEGER NOT NULL CHECK (max_connections > 0),
    connection_requests BIGINT NOT NULL DEFAULT 0 CHECK (connection_requests >= 0),
    connection_timeouts BIGINT NOT NULL DEFAULT 0 CHECK (connection_timeouts >= 0),
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for connection metrics with partial index for recent data
CREATE INDEX IF NOT EXISTS idx_connection_metrics_time ON connection_metrics(recorded_at);
CREATE INDEX IF NOT EXISTS idx_connection_metrics_recent ON connection_metrics(recorded_at) 
    WHERE recorded_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

-- AWS CloudWatch integration table for metric export
CREATE TABLE IF NOT EXISTS cloudwatch_metrics_export (
    id BIGSERIAL PRIMARY KEY,
    namespace VARCHAR(100) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    dimensions JSONB,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(20) NOT NULL DEFAULT 'Count',
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    exported_at TIMESTAMP WITH TIME ZONE,
    export_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (export_status IN ('PENDING', 'EXPORTED', 'FAILED'))
);

-- Indexes for CloudWatch export
CREATE INDEX IF NOT EXISTS idx_cloudwatch_export_status ON cloudwatch_metrics_export(export_status, timestamp);
CREATE INDEX IF NOT EXISTS idx_cloudwatch_export_namespace ON cloudwatch_metrics_export(namespace, metric_name);

-- Create a function for automatic metric cleanup (PostgreSQL specific)
CREATE OR REPLACE FUNCTION cleanup_old_metrics() RETURNS void AS $$
BEGIN
    -- Delete metrics older than 30 days
    DELETE FROM application_metrics WHERE timestamp < CURRENT_TIMESTAMP - INTERVAL '30 days';
    
    -- Delete health checks older than 7 days
    DELETE FROM health_checks WHERE check_time < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    -- Delete connection metrics older than 7 days
    DELETE FROM connection_metrics WHERE recorded_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    -- Delete exported CloudWatch metrics older than 1 day
    DELETE FROM cloudwatch_metrics_export 
    WHERE export_status = 'EXPORTED' AND exported_at < CURRENT_TIMESTAMP - INTERVAL '1 day';
END;
$$ LANGUAGE plpgsql;

-- Insert initial health check data for production
INSERT INTO health_checks (check_name, status, details, response_time_ms) VALUES 
('database', 'UP', '{"database":"PostgreSQL","version":"13+","mode":"production","ssl":true}', 5),
('application', 'UP', '{"profile":"production","features":["kafka-events","observability-enabled"]}', 8),
('aws-services', 'UP', '{"cloudwatch":true,"xray":true,"msk":true}', 12);

-- Insert sample metrics for production
INSERT INTO application_metrics (metric_name, metric_value, metric_type, tags) VALUES 
('application.startup.time', 8.7, 'GAUGE', '{"profile":"production","database":"postgresql"}'),
('jvm.memory.used', 512.3, 'GAUGE', '{"area":"heap","profile":"production"}'),
('http.requests.total', 0, 'COUNTER', '{"method":"GET","status":"200","environment":"production"}'),
('database.connections.active', 5, 'GAUGE', '{"pool":"hikari","database":"postgresql"}');