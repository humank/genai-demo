-- H2 Database Schema for Analytics Tests
-- Compatible with H2 in PostgreSQL mode

-- Create analytics_sessions table
CREATE TABLE IF NOT EXISTS analytics_sessions (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL UNIQUE,
    user_id VARCHAR(255),
    trace_id VARCHAR(255),
    start_time TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_seconds BIGINT,
    page_views_count INTEGER NOT NULL DEFAULT 0,
    user_actions_count INTEGER NOT NULL DEFAULT 0,
    business_events_count INTEGER NOT NULL DEFAULT 0,
    performance_metrics_count INTEGER NOT NULL DEFAULT 0,
    is_anonymous BOOLEAN NOT NULL DEFAULT TRUE,
    session_metadata TEXT,
    retention_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create analytics_events table
CREATE TABLE IF NOT EXISTS analytics_events (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    domain_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    frontend_event_type VARCHAR(255) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    trace_id VARCHAR(255) NOT NULL,
    page_path VARCHAR(500),
    action_type VARCHAR(255),
    metric_type VARCHAR(255),
    metric_value DOUBLE,
    event_data TEXT,
    occurred_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    retention_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for analytics_sessions
CREATE INDEX IF NOT EXISTS idx_session_id ON analytics_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_user_id ON analytics_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_created_at ON analytics_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_last_activity ON analytics_sessions(last_activity_at);
CREATE INDEX IF NOT EXISTS idx_retention_date ON analytics_sessions(retention_date);

-- Create indexes for analytics_events
CREATE INDEX IF NOT EXISTS idx_event_id ON analytics_events(event_id);
CREATE INDEX IF NOT EXISTS idx_session_id_events ON analytics_events(session_id);
CREATE INDEX IF NOT EXISTS idx_user_id_events ON analytics_events(user_id);
CREATE INDEX IF NOT EXISTS idx_event_type ON analytics_events(event_type);
CREATE INDEX IF NOT EXISTS idx_frontend_event_type ON analytics_events(frontend_event_type);
CREATE INDEX IF NOT EXISTS idx_occurred_at ON analytics_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_retention_date_events ON analytics_events(retention_date);
CREATE INDEX IF NOT EXISTS idx_session_event_type ON analytics_events(session_id, frontend_event_type);
CREATE INDEX IF NOT EXISTS idx_user_event_type_date ON analytics_events(user_id, frontend_event_type, occurred_at);