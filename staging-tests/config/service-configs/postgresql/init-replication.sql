-- Initialize replication user and settings
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'repl_password';

-- Create test database and schema
CREATE DATABASE genai_demo_staging;

\c genai_demo_staging;

-- Create test tables for integration testing
CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) REFERENCES customers(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_items (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) REFERENCES orders(id),
    product_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance testing
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_created_date ON customers(created_date);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- Insert sample data for testing
INSERT INTO customers (id, name, email, phone) VALUES
('CUST-001', 'John Doe', 'john.doe@example.com', '+1-555-0101'),
('CUST-002', 'Jane Smith', 'jane.smith@example.com', '+1-555-0102'),
('CUST-003', 'Bob Johnson', 'bob.johnson@example.com', '+1-555-0103'),
('CUST-004', 'Alice Brown', 'alice.brown@example.com', '+1-555-0104'),
('CUST-005', 'Charlie Wilson', 'charlie.wilson@example.com', '+1-555-0105')
ON CONFLICT (id) DO NOTHING;

INSERT INTO orders (id, customer_id, status, total_amount) VALUES
('ORDER-001', 'CUST-001', 'COMPLETED', 99.99),
('ORDER-002', 'CUST-002', 'PENDING', 149.50),
('ORDER-003', 'CUST-001', 'SHIPPED', 75.25),
('ORDER-004', 'CUST-003', 'COMPLETED', 200.00),
('ORDER-005', 'CUST-004', 'PENDING', 50.75)
ON CONFLICT (id) DO NOTHING;

INSERT INTO order_items (id, order_id, product_id, quantity, unit_price) VALUES
('ITEM-001', 'ORDER-001', 'PROD-001', 2, 49.99),
('ITEM-002', 'ORDER-002', 'PROD-002', 1, 149.50),
('ITEM-003', 'ORDER-003', 'PROD-003', 3, 25.08),
('ITEM-004', 'ORDER-004', 'PROD-001', 4, 50.00),
('ITEM-005', 'ORDER-005', 'PROD-004', 1, 50.75)
ON CONFLICT (id) DO NOTHING;