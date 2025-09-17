-- Analytics Tables Migration
-- Version: V20250915_001
-- Description: Create analytics tables for observability data persistence (production only)
-- Requirements: 2.3, 3.3

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for analytics_sessions
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_session_id ON analytics_sessions(session_id);
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_user_id ON analytics_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_created_at ON analytics_sessions(created_at);
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_last_activity ON analytics_sessions(last_activity_at);
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_retention_date ON analytics_sessions(retention_date);
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_start_time ON analytics_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_is_anonymous ON analytics_sessions(is_anonymous);

-- Create analytics_events table
CREATE TABLE IF NOT EXISTS analytics_events (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    domain_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    frontend_event_type VARCHAR(100) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    trace_id VARCHAR(255) NOT NULL,
    page_path VARCHAR(500),
    action_type VARCHAR(100),
    metric_type VARCHAR(50),
    metric_value DOUBLE PRECISION,
    event_data TEXT,
    occurred_at TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL,
    retention_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for analytics_events
CREATE INDEX IF NOT EXISTS idx_analytics_events_event_id ON analytics_events(event_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_session_id ON analytics_events(session_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_user_id ON analytics_events(user_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_event_type ON analytics_events(event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_events_frontend_event_type ON analytics_events(frontend_event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_events_occurred_at ON analytics_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_analytics_events_retention_date ON analytics_events(retention_date);
CREATE INDEX IF NOT EXISTS idx_analytics_events_page_path ON analytics_events(page_path);
CREATE INDEX IF NOT EXISTS idx_analytics_events_action_type ON analytics_events(action_type);
CREATE INDEX IF NOT EXISTS idx_analytics_events_metric_type ON analytics_events(metric_type);

-- Create composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_analytics_events_session_event_type ON analytics_events(session_id, frontend_event_type);
CREATE INDEX IF NOT EXISTS idx_analytics_events_user_event_type_date ON analytics_events(user_id, frontend_event_type, occurred_at);
CREATE INDEX IF NOT EXISTS idx_analytics_events_page_occurred_at ON analytics_events(page_path, occurred_at);
CREATE INDEX IF NOT EXISTS idx_analytics_events_metric_occurred_at ON analytics_events(metric_type, occurred_at) WHERE metric_type IS NOT NULL;

-- Add foreign key constraint (optional, for referential integrity)
-- Note: This is commented out as it might impact performance in high-volume scenarios
-- ALTER TABLE analytics_events ADD CONSTRAINT fk_analytics_events_session 
--     FOREIGN KEY (session_id) REFERENCES analytics_sessions(session_id) ON DELETE CASCADE;

-- Create trigger to update updated_at timestamp for analytics_sessions
CREATE OR REPLACE FUNCTION update_analytics_sessions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_analytics_sessions_updated_at
    BEFORE UPDATE ON analytics_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_analytics_sessions_updated_at();

-- Create partial indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_active ON analytics_sessions(last_activity_at) 
    WHERE end_time IS NULL;

CREATE INDEX IF NOT EXISTS idx_analytics_sessions_expired ON analytics_sessions(retention_date) 
    WHERE retention_date <= CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_analytics_events_expired ON analytics_events(retention_date) 
    WHERE retention_date <= CURRENT_TIMESTAMP;

-- Create indexes for statistical queries
CREATE INDEX IF NOT EXISTS idx_analytics_sessions_stats_date ON analytics_sessions(DATE(start_time), is_anonymous);
CREATE INDEX IF NOT EXISTS idx_analytics_events_stats_date ON analytics_events(DATE(occurred_at), frontend_event_type);

-- Add comments for documentation
COMMENT ON TABLE analytics_sessions IS 'Analytics sessions for observability data (production only)';
COMMENT ON TABLE analytics_events IS 'Analytics events for detailed user behavior tracking (production only)';

COMMENT ON COLUMN analytics_sessions.session_id IS 'Unique session identifier from frontend';
COMMENT ON COLUMN analytics_sessions.user_id IS 'User identifier (null for anonymous sessions)';
COMMENT ON COLUMN analytics_sessions.trace_id IS 'Distributed tracing identifier';
COMMENT ON COLUMN analytics_sessions.retention_date IS 'Date when this record should be deleted';
COMMENT ON COLUMN analytics_sessions.is_anonymous IS 'Whether this is an anonymous user session';

COMMENT ON COLUMN analytics_events.event_id IS 'Frontend event identifier';
COMMENT ON COLUMN analytics_events.domain_event_id IS 'Backend domain event identifier';
COMMENT ON COLUMN analytics_events.frontend_event_type IS 'Frontend event type (page_view, user_action, etc.)';
COMMENT ON COLUMN analytics_events.event_type IS 'Backend domain event type';
COMMENT ON COLUMN analytics_events.page_path IS 'Page path for page-related events';
COMMENT ON COLUMN analytics_events.action_type IS 'User action type for user_action events';
COMMENT ON COLUMN analytics_events.metric_type IS 'Performance metric type (lcp, fid, cls, etc.)';
COMMENT ON COLUMN analytics_events.metric_value IS 'Performance metric value';
COMMENT ON COLUMN analytics_events.event_data IS 'JSON event data payload';

-- Grant permissions (adjust as needed for your environment)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_sessions TO analytics_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON analytics_events TO analytics_user;
-- GRANT USAGE ON SEQUENCE analytics_sessions_id_seq TO analytics_user;
-- GRANT USAGE ON SEQUENCE analytics_events_id_seq TO analytics_user;