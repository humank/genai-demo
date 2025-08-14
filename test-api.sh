#!/bin/bash

# 測試前端 API 整合的腳本
echo "測試 GenAI Demo API 整合..."

BASE_URL="http://localhost:8080/api"

echo "1. 測試統計 API..."
curl -s "$BASE_URL/stats" | jq '.'

echo -e "\n2. 測試產品 API..."
curl -s "$BASE_URL/products?page=0&size=5" | jq '.data.content[0]'

echo -e "\n3. 測試客戶 API..."
curl -s "$BASE_URL/customers?page=0&size=5" | jq '.data.content[0]'

echo -e "\n4. 測試活動記錄 API..."
curl -s "$BASE_URL/activities?limit=3" | jq '.data[0]'

echo -e "\n5. 測試訂單狀態統計 API..."
curl -s "$BASE_URL/stats/order-status" | jq '.'

echo -e "\n6. 測試支付方式統計 API..."
curl -s "$BASE_URL/stats/payment-methods" | jq '.'

echo -e "\n7. 測試訂單列表 API..."
curl -s "$BASE_URL/orders?page=0&size=3" | jq '.data | {totalElements, totalPages, contentCount: (.content | length)}'

echo -e "\n8. 測試單個訂單 API (UUID 格式)..."
curl -s "$BASE_URL/orders/550e8400-e29b-41d4-a716-446655440001" | jq '{orderId, customerId, status, totalAmount}'

echo -e "\n9. 測試不存在的訂單..."
curl -s "$BASE_URL/orders/550e8400-e29b-41d4-a716-446655440999" | head -1

echo -e "\n10. 測試無效的 UUID 格式..."
curl -s "$BASE_URL/orders/invalid-uuid" | head -1

echo -e "\nAPI 測試完成！"
