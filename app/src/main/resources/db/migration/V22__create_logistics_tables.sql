-- ========================================
-- 創建物流配送系統表 - 支援 Logistics Context
-- ========================================

-- 創建配送方式表
CREATE TABLE delivery_methods (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    base_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    free_shipping_threshold DECIMAL(10,2), -- 免運門檻
    weight_limit_kg DECIMAL(8,2), -- 重量限制（公斤）
    estimated_days_min INTEGER NOT NULL DEFAULT 1,
    estimated_days_max INTEGER NOT NULL DEFAULT 7,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    CHECK (estimated_days_min <= estimated_days_max),
    CHECK (free_shipping_threshold IS NULL OR free_shipping_threshold > 0)
);

-- 創建配送區域表
CREATE TABLE delivery_regions (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE, -- 區域代碼，如 TPE, KHH
    parent_region_id VARCHAR(36), -- 父區域ID，支援階層結構
    region_type VARCHAR(20) NOT NULL, -- COUNTRY, CITY, DISTRICT
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_region_id) REFERENCES delivery_regions(id)
);

-- 創建配送方式支援區域關聯表
CREATE TABLE delivery_method_regions (
    id VARCHAR(36) PRIMARY KEY,
    delivery_method_id VARCHAR(36) NOT NULL,
    region_id VARCHAR(36) NOT NULL,
    additional_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- 額外費用
    estimated_days_adjustment INTEGER NOT NULL DEFAULT 0, -- 預估天數調整
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_method_id) REFERENCES delivery_methods(id) ON DELETE CASCADE,
    FOREIGN KEY (region_id) REFERENCES delivery_regions(id) ON DELETE CASCADE,
    CONSTRAINT unique_method_region UNIQUE (delivery_method_id, region_id)
);

-- 創建配送記錄表
CREATE TABLE deliveries (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    delivery_method_id VARCHAR(36) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    delivery_address TEXT NOT NULL,
    region_id VARCHAR(36) NOT NULL,
    delivery_fee DECIMAL(10,2) NOT NULL,
    estimated_delivery_date DATE,
    actual_delivery_date DATE,
    tracking_number VARCHAR(100),
    carrier_name VARCHAR(100), -- 承運商名稱
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SHIPPED, IN_TRANSIT, DELIVERED, FAILED, CANCELLED
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ,
    FOREIGN KEY (delivery_method_id) REFERENCES delivery_methods(id),
    FOREIGN KEY (region_id) REFERENCES delivery_regions(id),
    CHECK (actual_delivery_date IS NULL OR estimated_delivery_date IS NULL OR actual_delivery_date >= DATEADD('DAY', -7, estimated_delivery_date))
);

-- 創建配送狀態歷史表
CREATE TABLE delivery_status_history (
    id VARCHAR(36) PRIMARY KEY,
    delivery_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    location VARCHAR(200), -- 當前位置
    description TEXT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
);

-- 創建配送時段表
CREATE TABLE delivery_time_slots (
    id VARCHAR(36) PRIMARY KEY,
    delivery_method_id VARCHAR(36) NOT NULL,
    region_id VARCHAR(36) NOT NULL,
    day_of_week INTEGER NOT NULL, -- 1=Monday, 7=Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    max_deliveries INTEGER, -- 該時段最大配送數量
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (delivery_method_id) REFERENCES delivery_methods(id) ON DELETE CASCADE,
    FOREIGN KEY (region_id) REFERENCES delivery_regions(id) ON DELETE CASCADE,
    CHECK (day_of_week BETWEEN 1 AND 7),
    CHECK (start_time < end_time)
);

-- 創建索引
CREATE INDEX idx_delivery_methods_status ON delivery_methods(status);
CREATE INDEX idx_delivery_regions_code ON delivery_regions(code);
CREATE INDEX idx_delivery_regions_parent ON delivery_regions(parent_region_id);
CREATE INDEX idx_delivery_method_regions_method ON delivery_method_regions(delivery_method_id);
CREATE INDEX idx_delivery_method_regions_region ON delivery_method_regions(region_id);
CREATE INDEX idx_deliveries_order_id ON deliveries(order_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_deliveries_tracking_number ON deliveries(tracking_number);
CREATE INDEX idx_deliveries_estimated_date ON deliveries(estimated_delivery_date);
CREATE INDEX idx_delivery_status_history_delivery_id ON delivery_status_history(delivery_id);
CREATE INDEX idx_delivery_status_history_occurred_at ON delivery_status_history(occurred_at);
CREATE INDEX idx_delivery_time_slots_method_region ON delivery_time_slots(delivery_method_id, region_id);
CREATE INDEX idx_delivery_time_slots_day_time ON delivery_time_slots(day_of_week, start_time);