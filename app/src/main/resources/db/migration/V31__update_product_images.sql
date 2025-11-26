-- 更新產品圖片
UPDATE products SET image_url = 'assets/images/products/iphone-15-pro-max.png' WHERE product_id = 'PROD-001';
UPDATE products SET image_url = 'assets/images/products/samsung-galaxy-s24-ultra.png' WHERE product_id = 'PROD-002';
UPDATE products SET image_url = 'assets/images/products/macbook-pro-14-m3.png' WHERE product_id = 'PROD-003';
UPDATE products SET image_url = 'assets/images/products/ipad-air-11-m2.png' WHERE product_id = 'PROD-004';
UPDATE products SET image_url = 'assets/images/products/placeholder.png' WHERE product_id IN ('PROD-005', 'PROD-006', 'PROD-007', 'PROD-008', 'PROD-009', 'PROD-010');
