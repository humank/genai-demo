#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
數據生成腳本
生成大量的訂單、訂單項目和支付數據
"""

import random
from datetime import datetime, timedelta
import uuid

# 產品列表
products = [
    ('PROD-001', 'iPhone 15 Pro Max 256GB', 35900.00),
    ('PROD-002', 'Samsung Galaxy S24 Ultra', 32900.00),
    ('PROD-003', 'MacBook Pro 14 M3', 59900.00),
    ('PROD-004', 'iPad Air 11 M2', 18900.00),
    ('PROD-005', 'AirPods Pro 第三代', 8990.00),
    ('PROD-006', 'Sony WH-1000XM5 耳機', 9990.00),
    ('PROD-007', 'Nintendo Switch OLED', 10780.00),
    ('PROD-008', 'PlayStation 5', 15980.00),
    ('PROD-009', 'Xbox Series X', 15380.00),
    ('PROD-010', 'Apple Watch Series 9', 12900.00),
    ('PROD-011', 'iPhone 14 Pro 128GB', 32900.00),
    ('PROD-012', 'iPhone 13 256GB', 26900.00),
    ('PROD-013', 'Samsung Galaxy S23', 28900.00),
    ('PROD-014', 'Google Pixel 8 Pro', 31900.00),
    ('PROD-015', 'MacBook Air M2', 35900.00),
    ('PROD-016', 'MacBook Pro 16 M3', 79900.00),
    ('PROD-017', 'iPad Pro 12.9', 35900.00),
    ('PROD-018', 'iPad mini 6', 15900.00),
    ('PROD-019', 'AirPods Max', 18900.00),
    ('PROD-020', 'AirPods 第二代', 4490.00),
    ('PROD-051', 'Nike Air Force 1', 3290.00),
    ('PROD-052', 'Adidas Stan Smith', 2990.00),
    ('PROD-053', 'New Balance 990v5', 5490.00),
    ('PROD-054', 'Puma Suede Classic', 2790.00),
    ('PROD-055', 'Uniqlo Heattech 長袖', 590.00),
    ('PROD-056', 'Uniqlo Ultra Light Down', 1990.00),
    ('PROD-057', 'Zara 羊毛針織衫', 1590.00),
    ('PROD-058', 'H&M 基本款T恤', 299.00),
    ('PROD-059', 'Gap 經典牛仔外套', 2490.00),
    ('PROD-060', 'Levis 511 Slim Jeans', 2290.00),
]

# 台灣地址列表
addresses = [
    '台北市信義區信義路五段7號',
    '新北市板橋區中山路一段161號',
    '桃園市中壢區中正路123號',
    '台中市西屯區台灣大道三段99號',
    '高雄市前金區中正四路211號',
    '台南市中西區民權路二段158號',
    '新竹市東區光復路二段101號',
    '基隆市仁愛區愛一路25號',
    '宜蘭縣宜蘭市中山路三段145號',
    '花蓮縣花蓮市中華路123號',
    '台北市大安區敦化南路二段76號',
    '新北市新店區北新路三段200號',
    '桃園市桃園區復興路195號',
    '台中市北區三民路三段161號',
    '高雄市苓雅區四維三路2號',
    '台南市東區東門路二段89號',
    '新竹縣竹北市光明六路10號',
    '苗栗縣苗栗市中正路1047號',
    '彰化縣彰化市中山路二段416號',
    '南投縣南投市中興路660號',
]

# 支付方式
payment_methods = ['CREDIT_CARD', 'DIGITAL_WALLET', 'BANK_TRANSFER', 'CASH_ON_DELIVERY']

def generate_orders_sql(start_num=11, count=500):
    """生成訂單 SQL"""
    orders = []
    order_items = []
    payments = []
    
    base_date = datetime(2024, 3, 1)
    
    for i in range(count):
        order_num = start_num + i
        order_id = f'ORD-2024-{order_num:05d}'
        customer_id = f'CUST-{random.randint(1, 200):03d}'
        
        # 隨機選擇地址
        address = random.choice(addresses)
        
        # 隨機生成訂單日期
        days_offset = random.randint(0, 150)  # 5個月內
        order_date = base_date + timedelta(days=days_offset)
        delivery_date = order_date + timedelta(days=random.randint(1, 7))
        
        # 隨機選擇1-4個產品
        num_items = random.randint(1, 4)
        selected_products = random.sample(products, num_items)
        
        total_amount = 0
        order_item_sqls = []
        
        for product_id, product_name, price in selected_products:
            quantity = random.randint(1, 3)
            item_total = price * quantity
            total_amount += item_total
            
            order_item_sqls.append(
                f"('{order_id}', '{product_id}', '{product_name}', {quantity}, {price:.2f}, 'TWD')"
            )
        
        # 生成訂單 SQL
        orders.append(
            f"('{order_id}', '{customer_id}', '{address}', 'COMPLETED', {total_amount:.2f}, 'TWD', {total_amount:.2f}, '{order_date.strftime('%Y-%m-%d %H:%M:%S')}', '{delivery_date.strftime('%Y-%m-%d %H:%M:%S')}')"
        )
        
        # 添加訂單項目
        order_items.extend(order_item_sqls)
        
        # 生成支付記錄
        payment_id = f'PAY-2024-{order_num:05d}'
        payment_method = random.choice(payment_methods)
        transaction_id = f'TXN-{payment_method[:2]}-{order_date.strftime("%Y%m%d")}-{order_num:03d}' if payment_method != 'CASH_ON_DELIVERY' else 'NULL'
        
        payment_date = order_date + timedelta(minutes=random.randint(5, 30))
        
        payments.append(
            f"('{payment_id}', '{order_id}', {total_amount:.2f}, 'TWD', 'COMPLETED', '{payment_method}', {transaction_id if transaction_id != 'NULL' else 'NULL'}, NULL, '{payment_date.strftime('%Y-%m-%d %H:%M:%S')}', '{payment_date.strftime('%Y-%m-%d %H:%M:%S')}', FALSE)"
        )
    
    return orders, order_items, payments

def main():
    orders, order_items, payments = generate_orders_sql(11, 500)
    
    # 生成 SQL 文件
    with open('/Users/yikaikao/git/genai-demo/app/src/main/resources/db/migration/V12__insert_bulk_orders.sql', 'w', encoding='utf-8') as f:
        f.write('-- ========================================\n')
        f.write('-- 大量訂單數據初始化 (500筆訂單)\n')
        f.write('-- ========================================\n\n')
        
        # 訂單數據
        f.write('-- 插入訂單數據\n')
        f.write('INSERT INTO orders (id, customer_id, shipping_address, status, total_amount, currency, effective_amount, created_at, updated_at) VALUES\n')
        f.write(',\n'.join(orders))
        f.write(';\n\n')
        
        # 訂單項目數據 (分批插入)
        f.write('-- 插入訂單項目數據\n')
        batch_size = 100
        for i in range(0, len(order_items), batch_size):
            batch = order_items[i:i+batch_size]
            f.write('INSERT INTO order_items (order_id, product_id, product_name, quantity, price, currency) VALUES\n')
            f.write(',\n'.join(batch))
            f.write(';\n\n')
        
        # 支付數據
        f.write('-- 插入支付數據\n')
        f.write('INSERT INTO payments (id, order_id, amount, currency, status, payment_method, transaction_id, failure_reason, created_at, updated_at, can_retry) VALUES\n')
        f.write(',\n'.join(payments))
        f.write(';\n')
    
    print(f"生成了 {len(orders)} 筆訂單，{len(order_items)} 筆訂單項目，{len(payments)} 筆支付記錄")
    print("SQL 文件已生成: V12__insert_bulk_orders.sql")

if __name__ == '__main__':
    main()
