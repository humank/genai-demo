
-- 添加 image_url 欄位
ALTER TABLE products ADD COLUMN image_url VARCHAR(500);

-- 更新現有產品的圖片連結
-- 電子產品
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

-- 服飾
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Shoes' WHERE category = 'CLOTHING' AND name LIKE '%鞋%';
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Clothing' WHERE category = 'CLOTHING' AND image_url IS NULL;

-- 家居
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Furniture' WHERE category = 'HOME' OR name LIKE '%IKEA%';

-- 其他預設圖
UPDATE products SET image_url = 'https://placehold.co/600x400?text=Product' WHERE image_url IS NULL;
