-- ========================================
-- 創建商品評價表 - 支援商品評價聚合根
-- ========================================

-- 創建商品評價表
CREATE TABLE product_reviews (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(50), -- 關聯的訂單ID，確保只有購買過的客戶才能評價
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    comment TEXT,
    review_images TEXT, -- JSON array of image URLs
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, HIDDEN
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count INTEGER NOT NULL DEFAULT 0,
    unhelpful_count INTEGER NOT NULL DEFAULT 0,
    reported_count INTEGER NOT NULL DEFAULT 0,
    reviewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by VARCHAR(36), -- 審核人員ID
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_customer_product_order (customer_id, product_id, order_id)
);

-- 創建評價回覆表
CREATE TABLE review_replies (
    id VARCHAR(36) PRIMARY KEY,
    review_id VARCHAR(36) NOT NULL,
    replier_type VARCHAR(20) NOT NULL, -- CUSTOMER, MERCHANT, ADMIN
    replier_id VARCHAR(36) NOT NULL,
    reply_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, HIDDEN, DELETED
    replied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES product_reviews(id) ON DELETE CASCADE
);

-- 創建評價有用性投票表
CREATE TABLE review_helpfulness_votes (
    id VARCHAR(36) PRIMARY KEY,
    review_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36) NOT NULL,
    vote_type VARCHAR(10) NOT NULL CHECK (vote_type IN ('HELPFUL', 'UNHELPFUL')),
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES product_reviews(id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_customer_review_vote (customer_id, review_id)
);

-- 創建評價檢舉表
CREATE TABLE review_reports (
    id VARCHAR(36) PRIMARY KEY,
    review_id VARCHAR(36) NOT NULL,
    reporter_id VARCHAR(36) NOT NULL,
    report_reason VARCHAR(50) NOT NULL, -- INAPPROPRIATE_CONTENT, SPAM, FAKE_REVIEW, OFFENSIVE_LANGUAGE, OTHER
    report_description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, REVIEWED, RESOLVED, DISMISSED
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processor_id VARCHAR(36), -- 處理人員ID
    processing_notes TEXT,
    FOREIGN KEY (review_id) REFERENCES product_reviews(id) ON DELETE CASCADE,
    FOREIGN KEY (reporter_id) REFERENCES customers(id) ON DELETE CASCADE,
    UNIQUE KEY unique_reporter_review (reporter_id, review_id)
);

-- 創建商品評價統計表（用於快速查詢）
CREATE TABLE product_review_statistics (
    product_id VARCHAR(50) PRIMARY KEY,
    total_reviews INTEGER NOT NULL DEFAULT 0,
    average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_1_count INTEGER NOT NULL DEFAULT 0,
    rating_2_count INTEGER NOT NULL DEFAULT 0,
    rating_3_count INTEGER NOT NULL DEFAULT 0,
    rating_4_count INTEGER NOT NULL DEFAULT 0,
    rating_5_count INTEGER NOT NULL DEFAULT 0,
    last_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CHECK (average_rating >= 0.00 AND average_rating <= 5.00)
);

-- 創建索引
CREATE INDEX idx_product_reviews_product_id ON product_reviews(product_id);
CREATE INDEX idx_product_reviews_customer_id ON product_reviews(customer_id);
CREATE INDEX idx_product_reviews_status ON product_reviews(status);
CREATE INDEX idx_product_reviews_rating ON product_reviews(rating);
CREATE INDEX idx_product_reviews_reviewed_at ON product_reviews(reviewed_at);
CREATE INDEX idx_product_reviews_order_id ON product_reviews(order_id);
CREATE INDEX idx_review_replies_review_id ON review_replies(review_id);
CREATE INDEX idx_review_replies_replier_type ON review_replies(replier_type);
CREATE INDEX idx_review_helpfulness_votes_review_id ON review_helpfulness_votes(review_id);
CREATE INDEX idx_review_helpfulness_votes_customer_id ON review_helpfulness_votes(customer_id);
CREATE INDEX idx_review_reports_review_id ON review_reports(review_id);
CREATE INDEX idx_review_reports_status ON review_reports(status);
CREATE INDEX idx_product_review_statistics_average_rating ON product_review_statistics(average_rating);