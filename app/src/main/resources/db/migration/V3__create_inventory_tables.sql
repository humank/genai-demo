-- 創建庫存表
CREATE TABLE inventories (
    id UUID PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL UNIQUE,
    product_name VARCHAR(255) NOT NULL,
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL,
    threshold INT DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 創建庫存預留表
CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    inventory_id UUID NOT NULL,
    order_id UUID NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    FOREIGN KEY (inventory_id) REFERENCES inventories(id)
);

-- 創建索引
CREATE INDEX idx_inventory_product_id ON inventories(product_id);
CREATE INDEX idx_reservation_inventory_id ON inventory_reservations(inventory_id);
CREATE INDEX idx_reservation_order_id ON inventory_reservations(order_id);
CREATE INDEX idx_reservation_status ON inventory_reservations(status);