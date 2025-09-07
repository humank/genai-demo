-- H2-specific observability tables for development environment
-- These tables support the AWS CDK observability integration

-- Event store table for domain events (H2 optimized)
CREATE TABLE IF NOT EXISTS event_store (
    event_id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    event_data CLOB NOT NULL,
    event_version INTEGER NOT NULL DEFAULT 1,
    occurred_on TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for event store (H2 syntax)
CREATE INDEX IF NOT EXISTS idx_event_store_aggregate ON event_store(aggregate_id, aggregate_type);
CREATE INDEX IF NOT EXISTS idx_event_store_type ON event_store(event_type);
CREATE INDEX IF NOT EXISTS idx_event_store_occurred ON event_store(occurred_on);

-- Processed events table for idempotency (H2 optimized)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(36) PRIMARY KEY,
    handler_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_time_ms BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
);

-- Application metrics table for development monitoring
CREATE TABLE IF NOT EXISTS application_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE NOT NULL,
    metric_type VARCHAR(20) NOT NULL, -- COUNTER, GAUGE, TIMER, HISTOGRAM
    tags CLOB, -- JSON format for H2
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for metrics
CREATE INDEX IF NOT EXISTS idx_metrics_name_time ON application_metrics(metric_name, timestamp);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON application_metrics(timestamp);

-- Health check results table
CREATE TABLE IF NOT EXISTS health_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_name VARCHAR(50) NOT NULL,
    status VARCHAR(10) NOT NULL, -- UP, DOWN, UNKNOWN
    details CLOB,
    check_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    response_time_ms BIGINT
);

-- Index for health checks
CREATE INDEX IF NOT EXISTS idx_health_checks_time ON health_checks(check_time);
CREATE INDEX IF NOT EXISTS idx_health_checks_name_status ON health_checks(check_name, status);

-- Database connection monitoring (H2 specific)
CREATE TABLE IF NOT EXISTS connection_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    active_connections INTEGER NOT NULL,
    idle_connections INTEGER NOT NULL,
    total_connections INTEGER NOT NULL,
    max_connections INTEGER NOT NULL,
    connection_requests BIGINT NOT NULL DEFAULT 0,
    connection_timeouts BIGINT NOT NULL DEFAULT 0,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for connection metrics
CREATE INDEX IF NOT EXISTS idx_connection_metrics_time ON connection_metrics(recorded_at);

-- Insert initial health check data for development
INSERT INTO health_checks (check_name, status, details, response_time_ms) VALUES 
('database', 'UP', '{"database":"H2","version":"2.x","mode":"in-memory"}', 1),
('application', 'UP', '{"profile":"development","features":["h2-console","debug-logging"]}', 2);

-- Insert sample metrics for development
INSERT INTO application_metrics (metric_name, metric_value, metric_type, tags) VALUES 
('application.startup.time', 5.2, 'GAUGE', '{"profile":"dev","database":"h2"}'),
('jvm.memory.used', 128.5, 'GAUGE', '{"area":"heap","profile":"dev"}'),
('http.requests.total', 0, 'COUNTER', '{"method":"GET","status":"200"}');