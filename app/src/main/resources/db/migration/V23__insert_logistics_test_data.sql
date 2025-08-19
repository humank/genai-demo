-- ========================================
-- 插入物流配送系統測試資料
-- ========================================

-- 插入配送區域資料
INSERT INTO delivery_regions (id, name, code, parent_region_id, region_type, status) VALUES
-- 國家層級
('region-tw', '台灣', 'TW', NULL, 'COUNTRY', 'ACTIVE'),

-- 城市層級
('region-tpe', '台北市', 'TPE', 'region-tw', 'CITY', 'ACTIVE'),
('region-ntpc', '新北市', 'NTPC', 'region-tw', 'CITY', 'ACTIVE'),
('region-tao', '桃園市', 'TAO', 'region-tw', 'CITY', 'ACTIVE'),
('region-tc', '台中市', 'TC', 'region-tw', 'CITY', 'ACTIVE'),
('region-tn', '台南市', 'TN', 'region-tw', 'CITY', 'ACTIVE'),
('region-khh', '高雄市', 'KHH', 'region-tw', 'CITY', 'ACTIVE'),

-- 區域層級（台北市）
('region-tpe-xinyi', '信義區', 'TPE-XINYI', 'region-tpe', 'DISTRICT', 'ACTIVE'),
('region-tpe-daan', '大安區', 'TPE-DAAN', 'region-tpe', 'DISTRICT', 'ACTIVE'),
('region-tpe-zhongshan', '中山區', 'TPE-ZHONGSHAN', 'region-tpe', 'DISTRICT', 'ACTIVE'),

-- 區域層級（新北市）
('region-ntpc-banqiao', '板橋區', 'NTPC-BANQIAO', 'region-ntpc', 'DISTRICT', 'ACTIVE'),
('region-ntpc-zhonghe', '中和區', 'NTPC-ZHONGHE', 'region-ntpc', 'DISTRICT', 'ACTIVE');

-- 插入配送方式資料
INSERT INTO delivery_methods (id, name, description, base_fee, free_shipping_threshold, weight_limit_kg, estimated_days_min, estimated_days_max, status) VALUES
('dm-standard', '標準配送', '一般宅配服務，3-5個工作天送達', 80.00, 1000.00, 30.00, 3, 5, 'ACTIVE'),
('dm-express', '快速配送', '快速宅配服務，1-2個工作天送達', 150.00, 2000.00, 20.00, 1, 2, 'ACTIVE'),
('dm-same-day', '當日配送', '當日配送服務，限台北市區', 300.00, 5000.00, 10.00, 0, 0, 'ACTIVE'),
('dm-convenience', '超商取貨', '7-11或全家便利商店取貨', 60.00, 800.00, 5.00, 2, 4, 'ACTIVE'),
('dm-pickup', '門市自取', '至指定門市自行取貨', 0.00, NULL, 50.00, 0, 1, 'ACTIVE');

-- 插入配送方式支援區域關聯
INSERT INTO delivery_method_regions (id, delivery_method_id, region_id, additional_fee, estimated_days_adjustment) VALUES
-- 標準配送支援所有區域
('dmr-001', 'dm-standard', 'region-tpe', 0.00, 0),
('dmr-002', 'dm-standard', 'region-ntpc', 0.00, 0),
('dmr-003', 'dm-standard', 'region-tao', 20.00, 1),
('dmr-004', 'dm-standard', 'region-tc', 30.00, 1),
('dmr-005', 'dm-standard', 'region-tn', 40.00, 2),
('dmr-006', 'dm-standard', 'region-khh', 50.00, 2),

-- 快速配送支援主要城市
('dmr-007', 'dm-express', 'region-tpe', 0.00, 0),
('dmr-008', 'dm-express', 'region-ntpc', 0.00, 0),
('dmr-009', 'dm-express', 'region-tao', 50.00, 0),
('dmr-010', 'dm-express', 'region-tc', 80.00, 1),

-- 當日配送僅支援台北市
('dmr-011', 'dm-same-day', 'region-tpe', 0.00, 0),

-- 超商取貨支援所有區域
('dmr-012', 'dm-convenience', 'region-tpe', 0.00, 0),
('dmr-013', 'dm-convenience', 'region-ntpc', 0.00, 0),
('dmr-014', 'dm-convenience', 'region-tao', 0.00, 0),
('dmr-015', 'dm-convenience', 'region-tc', 0.00, 1),
('dmr-016', 'dm-convenience', 'region-tn', 0.00, 1),
('dmr-017', 'dm-convenience', 'region-khh', 0.00, 1),

-- 門市自取支援主要城市
('dmr-018', 'dm-pickup', 'region-tpe', 0.00, 0),
('dmr-019', 'dm-pickup', 'region-ntpc', 0.00, 0),
('dmr-020', 'dm-pickup', 'region-tc', 0.00, 0);

-- 插入配送記錄測試資料
INSERT INTO deliveries (id, order_id, delivery_method_id, recipient_name, recipient_phone, delivery_address, region_id, delivery_fee, estimated_delivery_date, tracking_number, carrier_name, status) VALUES
('delivery-001', '550e8400-e29b-41d4-a716-446655440001', 'dm-standard', '張小明', '0912345678', '台北市信義區信義路五段7號', 'region-tpe-xinyi', 80.00, '2024-01-05', 'TW123456789', '黑貓宅急便', 'DELIVERED'),
('delivery-002', '550e8400-e29b-41d4-a716-446655440002', 'dm-express', '李小華', '0923456789', '新北市板橋區中山路一段161號', 'region-ntpc-banqiao', 150.00, '2024-01-04', 'TW234567890', '新竹物流', 'DELIVERED'),
('delivery-003', '550e8400-e29b-41d4-a716-446655440003', 'dm-convenience', '王大明', '0934567890', '台北市大安區復興南路一段390號 7-11復興門市', 'region-tpe-daan', 60.00, '2024-01-06', 'CVS789012345', '統一超商', 'DELIVERED');

-- 插入配送狀態歷史
INSERT INTO delivery_status_history (id, delivery_id, status, location, description, occurred_at) VALUES
-- delivery-001 的配送歷史
('dsh-001', 'delivery-001', 'PENDING', '台北轉運中心', '訂單已建立，準備出貨', '2024-01-02 10:00:00'),
('dsh-002', 'delivery-001', 'SHIPPED', '台北轉運中心', '商品已出貨', '2024-01-02 15:30:00'),
('dsh-003', 'delivery-001', 'IN_TRANSIT', '信義區配送站', '商品運送中', '2024-01-05 09:00:00'),
('dsh-004', 'delivery-001', 'DELIVERED', '台北市信義區信義路五段7號', '商品已送達', '2024-01-05 14:20:00'),

-- delivery-002 的配送歷史
('dsh-005', 'delivery-002', 'PENDING', '新北轉運中心', '訂單已建立，準備出貨', '2024-01-02 11:00:00'),
('dsh-006', 'delivery-002', 'SHIPPED', '新北轉運中心', '商品已出貨', '2024-01-03 08:00:00'),
('dsh-007', 'delivery-002', 'IN_TRANSIT', '板橋區配送站', '商品運送中', '2024-01-04 10:00:00'),
('dsh-008', 'delivery-002', 'DELIVERED', '新北市板橋區中山路一段161號', '商品已送達', '2024-01-04 16:45:00'),

-- delivery-003 的配送歷史
('dsh-009', 'delivery-003', 'PENDING', '台北轉運中心', '訂單已建立，準備出貨', '2024-01-03 09:00:00'),
('dsh-010', 'delivery-003', 'SHIPPED', '台北轉運中心', '商品已出貨至超商', '2024-01-04 14:00:00'),
('dsh-011', 'delivery-003', 'IN_TRANSIT', '7-11復興門市', '商品已到達取貨門市', '2024-01-05 16:00:00'),
('dsh-012', 'delivery-003', 'DELIVERED', '7-11復興門市', '客戶已取貨', '2024-01-06 10:30:00');

-- 插入配送時段資料（以台北市為例）
INSERT INTO delivery_time_slots (id, delivery_method_id, region_id, day_of_week, start_time, end_time, is_available, max_deliveries) VALUES
-- 標準配送時段（週一到週五）
('dts-001', 'dm-standard', 'region-tpe', 1, '09:00:00', '12:00:00', true, 50),
('dts-002', 'dm-standard', 'region-tpe', 1, '13:00:00', '18:00:00', true, 80),
('dts-003', 'dm-standard', 'region-tpe', 2, '09:00:00', '12:00:00', true, 50),
('dts-004', 'dm-standard', 'region-tpe', 2, '13:00:00', '18:00:00', true, 80),
('dts-005', 'dm-standard', 'region-tpe', 3, '09:00:00', '12:00:00', true, 50),
('dts-006', 'dm-standard', 'region-tpe', 3, '13:00:00', '18:00:00', true, 80),
('dts-007', 'dm-standard', 'region-tpe', 4, '09:00:00', '12:00:00', true, 50),
('dts-008', 'dm-standard', 'region-tpe', 4, '13:00:00', '18:00:00', true, 80),
('dts-009', 'dm-standard', 'region-tpe', 5, '09:00:00', '12:00:00', true, 50),
('dts-010', 'dm-standard', 'region-tpe', 5, '13:00:00', '18:00:00', true, 80),

-- 當日配送時段（僅台北市，週一到週五）
('dts-011', 'dm-same-day', 'region-tpe', 1, '10:00:00', '14:00:00', true, 20),
('dts-012', 'dm-same-day', 'region-tpe', 1, '15:00:00', '19:00:00', true, 20),
('dts-013', 'dm-same-day', 'region-tpe', 2, '10:00:00', '14:00:00', true, 20),
('dts-014', 'dm-same-day', 'region-tpe', 2, '15:00:00', '19:00:00', true, 20),
('dts-015', 'dm-same-day', 'region-tpe', 3, '10:00:00', '14:00:00', true, 20),
('dts-016', 'dm-same-day', 'region-tpe', 3, '15:00:00', '19:00:00', true, 20),
('dts-017', 'dm-same-day', 'region-tpe', 4, '10:00:00', '14:00:00', true, 20),
('dts-018', 'dm-same-day', 'region-tpe', 4, '15:00:00', '19:00:00', true, 20),
('dts-019', 'dm-same-day', 'region-tpe', 5, '10:00:00', '14:00:00', true, 20),
('dts-020', 'dm-same-day', 'region-tpe', 5, '15:00:00', '19:00:00', true, 20);