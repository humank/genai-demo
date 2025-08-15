-- ========================================
-- 創建通知系統表 - 支援消費者通知功能
-- ========================================

-- 創建通知模板表
CREATE TABLE notification_templates (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL, -- ORDER_STATUS, DELIVERY_STATUS, PROMOTION, SYSTEM_ANNOUNCEMENT, MARKETING
    channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
    subject_template TEXT NOT NULL,
    content_template TEXT NOT NULL,
    variables TEXT, -- JSON array of template variables
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 創建通知表
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL, -- ORDER_STATUS, DELIVERY_STATUS, PROMOTION, SYSTEM_ANNOUNCEMENT, MARKETING
    channel VARCHAR(20) NOT NULL, -- EMAIL, SMS, PUSH, IN_APP
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, DELIVERED, READ, FAILED, CANCELLED
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    scheduled_at TIMESTAMP, -- 預定發送時間，NULL表示立即發送
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_reason TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retry_count INTEGER NOT NULL DEFAULT 3,
    related_entity_type VARCHAR(50), -- ORDER, PROMOTION, PRODUCT, etc.
    related_entity_id VARCHAR(50),
    metadata TEXT, -- JSON data for additional information
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CHECK (retry_count <= max_retry_count)
);

-- 創建通知訂閱表
CREATE TABLE notification_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(36) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    is_subscribed BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unsubscribed_at TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_customer_type_channel (customer_id, notification_type, channel)
);

-- 創建系統公告表
CREATE TABLE system_announcements (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- MAINTENANCE, FEATURE_UPDATE, PROMOTION, GENERAL
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    target_audience VARCHAR(50) NOT NULL DEFAULT 'ALL', -- ALL, PREMIUM_MEMBERS, VIP_MEMBERS, NEW_MEMBERS
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, PUBLISHED, ARCHIVED
    publish_at TIMESTAMP,
    expire_at TIMESTAMP,
    is_sticky BOOLEAN NOT NULL DEFAULT FALSE, -- 是否置頂
    view_count INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (expire_at IS NULL OR expire_at > publish_at)
);

-- 創建公告閱讀記錄表
CREATE TABLE announcement_read_records (
    id VARCHAR(36) PRIMARY KEY,
    announcement_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (announcement_id) REFERENCES system_announcements(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_customer_announcement (customer_id, announcement_id)
);

-- 創建通知統計表
CREATE TABLE notification_statistics (
    id VARCHAR(36) PRIMARY KEY,
    date DATE NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    total_sent INTEGER NOT NULL DEFAULT 0,
    total_delivered INTEGER NOT NULL DEFAULT 0,
    total_read INTEGER NOT NULL DEFAULT 0,
    total_failed INTEGER NOT NULL DEFAULT 0,
    delivery_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    read_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_date_type_channel (date, notification_type, channel)
);

-- 創建索引
CREATE INDEX idx_notification_templates_type ON notification_templates(type);
CREATE INDEX idx_notification_templates_channel ON notification_templates(channel);
CREATE INDEX idx_notifications_customer_id ON notifications(customer_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_channel ON notifications(channel);
CREATE INDEX idx_notifications_priority ON notifications(priority);
CREATE INDEX idx_notifications_scheduled_at ON notifications(scheduled_at);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notification_subscriptions_customer_id ON notification_subscriptions(customer_id);
CREATE INDEX idx_system_announcements_status ON system_announcements(status);
CREATE INDEX idx_system_announcements_type ON system_announcements(type);
CREATE INDEX idx_system_announcements_target_audience ON system_announcements(target_audience);
CREATE INDEX idx_system_announcements_publish_at ON system_announcements(publish_at);
CREATE INDEX idx_announcement_read_records_announcement_id ON announcement_read_records(announcement_id);
CREATE INDEX idx_announcement_read_records_customer_id ON announcement_read_records(customer_id);
CREATE INDEX idx_notification_statistics_date ON notification_statistics(date);
CREATE INDEX idx_notification_statistics_type ON notification_statistics(notification_type);