#!/bin/bash

# Swagger UI 功能驗證腳本
# 此腳本用於手動驗證 Swagger UI 的各項功能

echo "=== Swagger UI 功能驗證腳本 ==="
echo ""

BASE_URL="http://localhost:8080"

# 顏色定義
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 檢查函數
check_endpoint() {
    local url=$1
    local description=$2
    local expected_status=${3:-200}
    
    echo -n "檢查 $description... "
    
    status_code=$(curl -s -o /dev/null -w "%{http_code}" "$url")
    
    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ 通過${NC} (HTTP $status_code)"
        return 0
    else
        echo -e "${RED}✗ 失敗${NC} (HTTP $status_code, 預期 $expected_status)"
        return 1
    fi
}

# 檢查 JSON 內容
check_json_content() {
    local url=$1
    local description=$2
    local jq_filter=$3
    local expected_value=$4
    
    echo -n "檢查 $description... "
    
    result=$(curl -s "$url" | jq -r "$jq_filter" 2>/dev/null)
    
    if [ "$result" = "$expected_value" ]; then
        echo -e "${GREEN}✓ 通過${NC} ($result)"
        return 0
    else
        echo -e "${RED}✗ 失敗${NC} (得到: $result, 預期: $expected_value)"
        return 1
    fi
}

echo "1. 驗證 Swagger UI 可正常載入"
echo "================================"

check_endpoint "$BASE_URL/swagger-ui.html" "Swagger UI 重定向" 302
check_endpoint "$BASE_URL/swagger-ui/index.html" "Swagger UI 主頁面"

echo ""
echo "2. 驗證 OpenAPI 文檔端點"
echo "========================"

check_endpoint "$BASE_URL/v3/api-docs" "主要 API 文檔"
check_json_content "$BASE_URL/v3/api-docs" "API 標題" '.info.title' "GenAI Demo - DDD 電商平台 API"
check_json_content "$BASE_URL/v3/api-docs" "API 版本" '.info.version' "1.0.0"

echo ""
echo "3. 驗證 API 分組配置"
echo "==================="

check_endpoint "$BASE_URL/v3/api-docs/public-api" "公開 API 分組"
check_endpoint "$BASE_URL/v3/api-docs/internal-api" "內部 API 分組"
check_endpoint "$BASE_URL/v3/api-docs/management" "管理端點分組"

echo ""
echo "4. 驗證 API 標籤和分類"
echo "====================="

# 檢查標籤數量
tag_count=$(curl -s "$BASE_URL/v3/api-docs" | jq '.tags | length' 2>/dev/null)
echo -n "檢查 API 標籤數量... "
if [ "$tag_count" -gt 5 ]; then
    echo -e "${GREEN}✓ 通過${NC} (找到 $tag_count 個標籤)"
else
    echo -e "${RED}✗ 失敗${NC} (只找到 $tag_count 個標籤)"
fi

# 檢查重要標籤
important_tags=("訂單管理" "支付管理" "庫存管理" "產品管理" "客戶管理")
for tag in "${important_tags[@]}"; do
    echo -n "檢查標籤 '$tag'... "
    if curl -s "$BASE_URL/v3/api-docs" | jq -e ".tags[] | select(.name == \"$tag\")" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 存在${NC}"
    else
        echo -e "${RED}✗ 不存在${NC}"
    fi
done

echo ""
echo "5. 驗證錯誤回應格式"
echo "=================="

# 檢查 StandardErrorResponse schema
echo -n "檢查 StandardErrorResponse schema... "
if curl -s "$BASE_URL/v3/api-docs" | jq -e '.components.schemas.StandardErrorResponse' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 存在${NC}"
    
    # 檢查必填欄位
    required_fields=("code" "message" "timestamp" "path")
    for field in "${required_fields[@]}"; do
        echo -n "  檢查必填欄位 '$field'... "
        if curl -s "$BASE_URL/v3/api-docs" | jq -e ".components.schemas.StandardErrorResponse.required[] | select(. == \"$field\")" > /dev/null 2>&1; then
            echo -e "${GREEN}✓${NC}"
        else
            echo -e "${RED}✗${NC}"
        fi
    done
else
    echo -e "${RED}✗ 不存在${NC}"
fi

echo ""
echo "6. 驗證 API 端點回應定義"
echo "======================="

# 檢查訂單創建端點的錯誤回應
echo -n "檢查訂單創建端點的 400 錯誤回應... "
if curl -s "$BASE_URL/v3/api-docs" | jq -e '.paths."/api/orders".post.responses."400"' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 存在${NC}"
else
    echo -e "${RED}✗ 不存在${NC}"
fi

echo -n "檢查訂單創建端點的 500 錯誤回應... "
if curl -s "$BASE_URL/v3/api-docs" | jq -e '.paths."/api/orders".post.responses."500"' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 存在${NC}"
else
    echo -e "${RED}✗ 不存在${NC}"
fi

echo ""
echo "7. 測試實際 API 功能"
echo "==================="

# 測試客戶列表 API
echo -n "測試客戶列表 API... "
customers_response=$(curl -s "$BASE_URL/api/customers")
if echo "$customers_response" | jq -e '. | type == "array"' > /dev/null 2>&1; then
    customer_count=$(echo "$customers_response" | jq '. | length')
    echo -e "${GREEN}✓ 通過${NC} (返回 $customer_count 個客戶)"
else
    echo -e "${RED}✗ 失敗${NC} (回應格式不正確)"
fi

# 測試產品列表 API
echo -n "測試產品列表 API... "
if check_endpoint "$BASE_URL/api/products" "產品列表" 200 > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 通過${NC}"
else
    echo -e "${RED}✗ 失敗${NC}"
fi

# 測試錯誤回應格式
echo -n "測試錯誤回應格式... "
error_response=$(curl -s -X POST "$BASE_URL/api/orders" -H "Content-Type: application/json" -d '{"invalid": "data"}')
if echo "$error_response" | jq -e '.message' > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 通過${NC} (返回錯誤訊息)"
else
    echo -e "${RED}✗ 失敗${NC} (錯誤格式不正確)"
fi

echo ""
echo "8. Swagger UI 功能建議測試"
echo "========================="
echo -e "${YELLOW}以下功能需要手動在瀏覽器中測試：${NC}"
echo ""
echo "• 開啟瀏覽器訪問: $BASE_URL/swagger-ui.html"
echo "• 驗證所有 API 端點都正確顯示"
echo "• 測試 'Try it out' 功能："
echo "  - 選擇一個 GET 端點（如 /api/customers）"
echo "  - 點擊 'Try it out' 按鈕"
echo "  - 點擊 'Execute' 按鈕"
echo "  - 檢查回應是否正確顯示"
echo "• 驗證 API 分組功能："
echo "  - 檢查右上角的分組選擇器"
echo "  - 切換不同的 API 分組"
echo "• 測試搜尋功能："
echo "  - 使用搜尋框搜尋特定的 API"
echo "• 檢查錯誤回應顯示："
echo "  - 查看端點的錯誤回應定義"
echo "  - 確認錯誤格式符合 StandardErrorResponse"

echo ""
echo "=== 驗證完成 ==="
echo ""
echo -e "${YELLOW}注意：${NC}"
echo "• 確保應用程式正在 $BASE_URL 上運行"
echo "• 某些功能需要在瀏覽器中手動測試"
echo "• 如有任何測試失敗，請檢查應用程式日誌"