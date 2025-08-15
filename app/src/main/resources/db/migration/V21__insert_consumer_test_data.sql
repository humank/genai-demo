-- ========================================
-- 插入消費者導向的測試資料
-- ========================================

-- 更新客戶資料，添加生日和獎勵積點
UPDATE customers SET 
    birth_date = '1990-03-15',
    reward_points_balance = 1500,
    notification_enabled_types = '["ORDER_STATUS", "DELIVERY_STATUS", "PROMOTION"]',
    notification_enabled_channels = '["EMAIL", "IN_APP"]',
    marketing_enabled = true
WHERE id = '660e8400-e29b-41d4-a716-446655440001';

UPDATE customers SET 
    birth_date = '1985-07-22',
    reward_points_balance = 3200,
    membership_level = 'PREMIUM',
    notification_enabled_types = '["ORDER_STATUS", "DELIVERY_STATUS", "PROMOTION", "MARKETING"]',
    notification_enabled_channels = '["EMAIL", "SMS", "IN_APP"]',
    marketing_enabled = true
WHERE id = '660e8400-e29b-41d4-a716-446655440002';

UPDATE customers SET 
    birth_date = '1992-12-08',
    reward_points_balance = 800,
    notification_enabled_types = '["ORDER_STATUS", "DELIVERY_STATUS"]',
    notification_enabled_channels = '["EMAIL"]',
    marketing_enabled = false
WHERE id = '660e8400-e29b-41d4-a716-446655440003';

UPDATE customers SET 
    birth_date = '1988-05-30',
    reward_points_balance = 5800,
    membership_level = 'VIP',
    notification_enabled_types = '["ORDER_STATUS", "DELIVERY_STATUS", "PROMOTION", "MARKETING", "SYSTEM_ANNOUNCEMENT"]',
    notification_enabled_channels = '["EMAIL", "SMS", "PUSH", "IN_APP"]',
    marketing_enabled = true
WHERE id = '660e8400-e29b-41d4-a716-446655440004';

-- 插入購物車測試資料
INSERT INTO shopping_carts (id, customer_id, total_amount, currency, discount_amount, final_amount, status) VALUES
('cart-001', '660e8400-e29b-41d4-a716-446655440001', 45900.00, 'TWD', 2000.00, 43900.00, 'ACTIVE'),
('cart-002', '660e8400-e29b-41d4-a716-446655440002', 12450.00, 'TWD', 0.00, 12450.00, 'ACTIVE'),
('cart-003', '660e8400-e29b-41d4-a716-446655440003', 8990.00, 'TWD', 500.00, 8490.00, 'ACTIVE');

-- 插入購物車項目
INSERT INTO cart_items (id, cart_id, product_id, product_name, quantity, unit_price, total_price, currency) VALUES
('item-001', 'cart-001', 'PROD-001', 'iPhone 15 Pro Max 256GB', 1, 35900.00, 35900.00, 'TWD'),
('item-002', 'cart-001', 'PROD-010', 'Apple Watch Series 9', 1, 10000.00, 10000.00, 'TWD'),
('item-003', 'cart-002', 'PROD-003', 'MacBook Pro 14 M3', 1, 12450.00, 12450.00, 'TWD'),
('item-004', 'cart-003', 'PROD-005', 'AirPods Pro 第三代', 1, 8990.00, 8990.00, 'TWD');

-- 插入促銷活動測試資料
INSERT INTO promotions (id, name, description, type, status, start_date, end_date, priority, max_usage_count, minimum_order_amount, applicable_membership_levels) VALUES
('promo-001', '新春特惠', '全站商品9折優惠', 'PERCENTAGE_DISCOUNT', 'ACTIVE', '2024-01-01 00:00:00', '2024-02-29 23:59:59', 1, 1000, 1000.00, '["STANDARD", "PREMIUM", "VIP"]'),
('promo-002', '會員專享', 'VIP會員滿萬免運', 'FIXED_AMOUNT_DISCOUNT', 'ACTIVE', '2024-01-01 00:00:00', '2024-12-31 23:59:59', 2, NULL, 10000.00, '["VIP"]'),
('promo-003', '限時閃購', 'iPhone 15 Pro Max 限時特價', 'FLASH_SALE', 'ACTIVE', '2024-08-15 10:00:00', '2024-08-15 18:00:00', 3, 50, NULL, '["STANDARD", "PREMIUM", "VIP"]'),
('promo-004', '買一送一', 'AirPods Pro 買一送一', 'BUY_ONE_GET_ONE', 'ACTIVE', '2024-08-01 00:00:00', '2024-08-31 23:59:59', 1, 100, NULL, '["PREMIUM", "VIP"]');

-- 插入促銷規則
INSERT INTO promotion_rules (id, promotion_id, rule_type, rule_data) VALUES
('rule-001', 'promo-001', 'PERCENTAGE_DISCOUNT', '{"percentage": 10, "minimumAmount": 1000}'),
('rule-002', 'promo-002', 'FIXED_AMOUNT_DISCOUNT', '{"discountAmount": 200, "minimumAmount": 10000}'),
('rule-003', 'promo-003', 'FLASH_SALE', '{"discountPercentage": 15, "productIds": ["PROD-001"], "maxQuantity": 1}'),
('rule-004', 'promo-004', 'BUY_ONE_GET_ONE', '{"productIds": ["PROD-005"], "freeProductId": "PROD-005", "maxSets": 1}');

-- 插入優惠券
INSERT INTO coupons (id, promotion_id, code, customer_id, status, max_usage_count) VALUES
('coupon-001', 'promo-001', 'NEWYEAR2024', NULL, 'ACTIVE', 1),
('coupon-002', 'promo-002', 'VIP-FREE-SHIP', '660e8400-e29b-41d4-a716-446655440004', 'ACTIVE', 5),
('coupon-003', 'promo-003', 'FLASH-IPHONE', NULL, 'ACTIVE', 1),
('coupon-004', 'promo-004', 'BOGO-AIRPODS', NULL, 'ACTIVE', 1);

-- 插入購物車應用的促銷
INSERT INTO cart_applied_promotions (id, cart_id, promotion_id, promotion_name, discount_amount) VALUES
('applied-001', 'cart-001', 'promo-001', '新春特惠', 2000.00),
('applied-002', 'cart-003', 'promo-001', '新春特惠', 500.00);

-- 插入超商優惠券商品
INSERT INTO voucher_products (id, name, description, face_value, selling_price, currency, validity_days, category, applicable_stores, terms_and_conditions, status) VALUES
('vp-001', '7-11 $100 商品券', '可於全台7-11門市使用的商品券', 100.00, 95.00, 'TWD', 365, 'RETAIL', '["7-11"]', '限於7-11門市使用，不可找零', 'ACTIVE'),
('vp-002', '全家 $200 商品券', '可於全台全家便利商店使用的商品券', 200.00, 190.00, 'TWD', 365, 'RETAIL', '["FAMILY_MART"]', '限於全家便利商店使用，不可找零', 'ACTIVE'),
('vp-003', '星巴克 $300 飲品券', '可於星巴克門市兌換飲品', 300.00, 280.00, 'TWD', 180, 'BEVERAGE', '["STARBUCKS"]', '限於星巴克門市使用，可兌換等值或較低價位飲品', 'ACTIVE'),
('vp-004', '麥當勞 $150 餐券', '可於麥當勞門市使用的餐券', 150.00, 140.00, 'TWD', 90, 'FOOD', '["MCDONALDS"]', '限於麥當勞門市使用，不可找零', 'ACTIVE');

-- 插入優惠券實例
INSERT INTO vouchers (id, voucher_product_id, customer_id, redemption_code, status, face_value, purchase_price, currency, expires_at) VALUES
('voucher-001', 'vp-001', '660e8400-e29b-41d4-a716-446655440001', 'V711001234567890', 'ACTIVE', 100.00, 95.00, 'TWD', '2025-08-15 23:59:59'),
('voucher-002', 'vp-002', '660e8400-e29b-41d4-a716-446655440002', 'VFM2001234567890', 'ACTIVE', 200.00, 190.00, 'TWD', '2025-08-15 23:59:59'),
('voucher-003', 'vp-003', '660e8400-e29b-41d4-a716-446655440003', 'VSB3001234567890', 'USED', 300.00, 280.00, 'TWD', '2025-02-15 23:59:59'),
('voucher-004', 'vp-004', '660e8400-e29b-41d4-a716-446655440004', 'VMD1501234567890', 'ACTIVE', 150.00, 140.00, 'TWD', '2024-11-15 23:59:59');

-- 插入優惠券使用記錄
INSERT INTO voucher_usage_history (id, voucher_id, action, store_code, amount_used, currency, notes) VALUES
('vh-001', 'voucher-001', 'PURCHASED', NULL, NULL, NULL, '線上購買'),
('vh-002', 'voucher-002', 'PURCHASED', NULL, NULL, NULL, '線上購買'),
('vh-003', 'voucher-003', 'PURCHASED', NULL, NULL, NULL, '線上購買'),
('vh-004', 'voucher-003', 'USED', 'SB-TPE-001', 300.00, 'TWD', '兌換大杯拿鐵'),
('vh-005', 'voucher-004', 'PURCHASED', NULL, NULL, NULL, '線上購買');

-- 插入商品評價
INSERT INTO product_reviews (id, product_id, customer_id, order_id, rating, title, comment, status, is_anonymous) VALUES
('review-001', 'PROD-001', '660e8400-e29b-41d4-a716-446655440001', 'ORD-2024-01-001', 5, '非常滿意的購買體驗', '手機品質很好，配送也很快速，推薦！', 'APPROVED', false),
('review-002', 'PROD-005', '660e8400-e29b-41d4-a716-446655440002', 'ORD-2024-01-002', 4, '音質不錯', '降噪效果很好，但價格有點高', 'APPROVED', false),
('review-003', 'PROD-007', '660e8400-e29b-41d4-a716-446655440004', 'ORD-2024-01-004', 5, '遊戲體驗極佳', 'OLED螢幕真的很棒，遊戲畫面超清晰', 'APPROVED', false),
('review-004', 'PROD-002', '660e8400-e29b-41d4-a716-446655440009', 'ORD-2024-01-009', 3, '普通', '手機還可以，但沒有特別驚艷', 'APPROVED', true);

-- 插入評價統計
INSERT INTO product_review_statistics (product_id, total_reviews, average_rating, rating_1_count, rating_2_count, rating_3_count, rating_4_count, rating_5_count) VALUES
('PROD-001', 1, 5.00, 0, 0, 0, 0, 1),
('PROD-002', 1, 3.00, 0, 0, 1, 0, 0),
('PROD-005', 1, 4.00, 0, 0, 0, 1, 0),
('PROD-007', 1, 5.00, 0, 0, 0, 0, 1);

-- 插入通知模板
INSERT INTO notification_templates (id, name, type, channel, subject_template, content_template, variables, status) VALUES
('nt-001', '訂單確認通知', 'ORDER_STATUS', 'EMAIL', '訂單確認 - {{orderNumber}}', '親愛的{{customerName}}，您的訂單{{orderNumber}}已確認，總金額{{totalAmount}}元。', '["customerName", "orderNumber", "totalAmount"]', 'ACTIVE'),
('nt-002', '配送通知', 'DELIVERY_STATUS', 'SMS', '您的包裹已出貨', '{{customerName}}您好，訂單{{orderNumber}}已出貨，預計{{deliveryDate}}送達。', '["customerName", "orderNumber", "deliveryDate"]', 'ACTIVE'),
('nt-003', '促銷活動通知', 'PROMOTION', 'IN_APP', '限時優惠來了！', '{{promotionName}}現正進行中，折扣高達{{discount}}%，快來搶購！', '["promotionName", "discount"]', 'ACTIVE'),
('nt-004', '系統維護通知', 'SYSTEM_ANNOUNCEMENT', 'EMAIL', '系統維護通知', '系統將於{{maintenanceDate}}進行維護，預計維護時間{{duration}}小時。', '["maintenanceDate", "duration"]', 'ACTIVE');

-- 插入通知記錄
INSERT INTO notifications (id, customer_id, type, channel, priority, subject, content, status, is_read, related_entity_type, related_entity_id) VALUES
('notif-001', '660e8400-e29b-41d4-a716-446655440001', 'ORDER_STATUS', 'EMAIL', 'NORMAL', '訂單確認 - ORD-2024-01-001', '親愛的張小明，您的訂單ORD-2024-01-001已確認，總金額35900元。', 'DELIVERED', true, 'ORDER', 'ORD-2024-01-001'),
('notif-002', '660e8400-e29b-41d4-a716-446655440002', 'DELIVERY_STATUS', 'SMS', 'HIGH', '您的包裹已出貨', '李小華您好，訂單ORD-2024-01-002已出貨，預計2024-01-04送達。', 'DELIVERED', true, 'ORDER', 'ORD-2024-01-002'),
('notif-003', '660e8400-e29b-41d4-a716-446655440001', 'PROMOTION', 'IN_APP', 'NORMAL', '限時優惠來了！', '新春特惠現正進行中，折扣高達10%，快來搶購！', 'DELIVERED', false, 'PROMOTION', 'promo-001'),
('notif-004', '660e8400-e29b-41d4-a716-446655440004', 'MARKETING', 'EMAIL', 'LOW', 'VIP會員專屬優惠', '陳美麗您好，作為VIP會員，您享有專屬優惠和免運服務。', 'DELIVERED', false, NULL, NULL);

-- 插入通知訂閱
INSERT INTO notification_subscriptions (customer_id, notification_type, channel, is_subscribed) VALUES
('660e8400-e29b-41d4-a716-446655440001', 'ORDER_STATUS', 'EMAIL', true),
('660e8400-e29b-41d4-a716-446655440001', 'DELIVERY_STATUS', 'IN_APP', true),
('660e8400-e29b-41d4-a716-446655440001', 'PROMOTION', 'EMAIL', true),
('660e8400-e29b-41d4-a716-446655440002', 'ORDER_STATUS', 'EMAIL', true),
('660e8400-e29b-41d4-a716-446655440002', 'DELIVERY_STATUS', 'SMS', true),
('660e8400-e29b-41d4-a716-446655440002', 'PROMOTION', 'IN_APP', true),
('660e8400-e29b-41d4-a716-446655440002', 'MARKETING', 'EMAIL', true);

-- 插入系統公告
INSERT INTO system_announcements (id, title, content, type, priority, target_audience, status, publish_at, is_sticky, created_by) VALUES
('ann-001', '系統升級通知', '為提供更好的服務體驗，系統將於2024年8月20日凌晨2:00-4:00進行升級維護，期間可能影響部分功能使用。', 'MAINTENANCE', 'HIGH', 'ALL', 'PUBLISHED', '2024-08-15 09:00:00', true, 'admin-001'),
('ann-002', '新功能上線', '購物車功能全新升級！現在支援更多促銷組合和個人化推薦。', 'FEATURE_UPDATE', 'NORMAL', 'ALL', 'PUBLISHED', '2024-08-10 10:00:00', false, 'admin-001'),
('ann-003', 'VIP會員專屬活動', 'VIP會員限定：全站商品額外9折優惠，活動期間至8月底。', 'PROMOTION', 'NORMAL', 'VIP_MEMBERS', 'PUBLISHED', '2024-08-01 00:00:00', false, 'admin-002');

-- 插入公告閱讀記錄
INSERT INTO announcement_read_records (id, announcement_id, customer_id) VALUES
('arr-001', 'ann-001', '660e8400-e29b-41d4-a716-446655440001'),
('arr-002', 'ann-002', '660e8400-e29b-41d4-a716-446655440001'),
('arr-003', 'ann-001', '660e8400-e29b-41d4-a716-446655440002'),
('arr-004', 'ann-003', '660e8400-e29b-41d4-a716-446655440004');