-- Insert products
INSERT INTO products (product_id, name, description, price, currency, category, status, created_at, updated_at) VALUES
('PROD-001', 'iPhone 15 Pro', '最新款 iPhone，配備 A17 Pro 晶片', 35900.00, 'TWD', 'ELECTRONICS', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-002', 'MacBook Air M3', '輕薄筆記型電腦，搭載 M3 晶片', 36900.00, 'TWD', 'ELECTRONICS', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-003', 'AirPods Pro', '主動降噪無線耳機', 7490.00, 'TWD', 'ELECTRONICS', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-004', '咖啡豆 - 藍山', '牙買加藍山咖啡豆 250g', 1200.00, 'TWD', 'FOOD', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-005', '有機綠茶', '台灣高山有機綠茶 100g', 450.00, 'TWD', 'FOOD', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-006', '運動鞋', '透氣運動跑鞋', 2890.00, 'TWD', 'CLOTHING', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-007', 'T恤', '純棉短袖T恤', 590.00, 'TWD', 'CLOTHING', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PROD-008', '筆記本', 'A5 精裝筆記本', 280.00, 'TWD', 'STATIONERY', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update image URLs
UPDATE products SET image_url = 'https://placehold.co/600x400?text=iPhone+15+Pro' WHERE name LIKE '%iPhone%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=MacBook' WHERE name LIKE '%MacBook%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=AirPods' WHERE name LIKE '%AirPods%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Samsung' WHERE name LIKE '%Samsung%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Sony' WHERE name LIKE '%Sony%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Switch' WHERE name LIKE '%Switch%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=PS5' WHERE name LIKE '%PlayStation%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Xbox' WHERE name LIKE '%Xbox%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=iPad' WHERE name LIKE '%iPad%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Watch' WHERE name LIKE '%Watch%';

-- Clothing
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Shoes' WHERE category = 'CLOTHING' AND name LIKE '%鞋%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Clothing' WHERE category = 'CLOTHING' AND image_url IS NULL;

-- Home
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Furniture' WHERE category = 'HOME' OR name LIKE '%IKEA%';

-- Default
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Product' WHERE image_url IS NULL;
