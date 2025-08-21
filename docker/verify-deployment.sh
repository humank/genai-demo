#!/bin/bash

# 部署驗證腳本

set -e

echo "🔍 開始驗證 GenAI Demo 部署..."

# 檢查 Docker 是否運行
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker 未運行，請先啟動 Docker"
    exit 1
fi

# 檢查容器狀態
echo "📋 檢查容器狀態..."
if docker-compose ps | grep -q "genai-demo-app.*Up"; then
    echo "✅ 應用程式容器正在運行"
else
    echo "❌ 應用程式容器未運行"
    docker-compose ps
    exit 1
fi

# 等待應用程式啟動
echo "⏳ 等待應用程式啟動..."
for i in {1..30}; do
    if curl -f -s http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ 應用程式已啟動"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ 應用程式啟動超時"
        exit 1
    fi
    sleep 2
done

# 檢查健康狀態
echo "🏥 檢查應用程式健康狀態..."
HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
if [ "$HEALTH_STATUS" = "UP" ]; then
    echo "✅ 應用程式健康狀態正常"
else
    echo "❌ 應用程式健康狀態異常: $HEALTH_STATUS"
    exit 1
fi

# 檢查 API 文檔
echo "📚 檢查 API 文檔..."
if curl -f -s http://localhost:8080/swagger-ui/index.html > /dev/null; then
    echo "✅ API 文檔可訪問"
else
    echo "❌ API 文檔無法訪問"
    exit 1
fi

# 檢查 OpenAPI 規範
echo "📋 檢查 OpenAPI 規範..."
if curl -f -s http://localhost:8080/v3/api-docs > /dev/null; then
    echo "✅ OpenAPI 規範可訪問"
else
    echo "❌ OpenAPI 規範無法訪問"
    exit 1
fi

# 檢查 H2 控制台
echo "🗄️ 檢查 H2 資料庫控制台..."
if curl -f -s http://localhost:8080/h2-console > /dev/null; then
    echo "✅ H2 控制台可訪問"
else
    echo "❌ H2 控制台無法訪問"
    exit 1
fi

# 測試基本 API 端點
echo "🧪 測試基本 API 端點..."

# 測試客戶 API
if curl -f -s http://localhost:8080/api/customers > /dev/null; then
    echo "✅ 客戶 API 可訪問"
else
    echo "⚠️  客戶 API 無法訪問 (可能是正常的，如果沒有測試資料)"
fi

# 測試產品 API
if curl -f -s http://localhost:8080/api/products > /dev/null; then
    echo "✅ 產品 API 可訪問"
else
    echo "⚠️  產品 API 無法訪問 (可能是正常的，如果沒有測試資料)"
fi

# 檢查日誌
echo "📝 檢查應用程式日誌..."
if docker-compose logs genai-demo | grep -q "Started GenaiDemoApplication"; then
    echo "✅ 應用程式成功啟動"
else
    echo "⚠️  未找到啟動成功日誌，請檢查詳細日誌"
fi

# 檢查錯誤日誌
ERROR_COUNT=$(docker-compose logs genai-demo | grep -c "ERROR" || true)
if [ "$ERROR_COUNT" -eq 0 ]; then
    echo "✅ 沒有發現錯誤日誌"
else
    echo "⚠️  發現 $ERROR_COUNT 個錯誤日誌，請檢查詳細日誌"
fi

echo ""
echo "🎉 部署驗證完成！"
echo ""
echo "📱 訪問連結："
echo "   🌐 API 文檔: http://localhost:8080/swagger-ui/index.html"
echo "   🏥 健康檢查: http://localhost:8080/actuator/health"
echo "   🗄️ H2 控制台: http://localhost:8080/h2-console"
echo ""
echo "🔍 查看日誌："
echo "   docker-compose logs -f genai-demo"
echo ""
echo "🛑 停止服務："
echo "   docker-compose down"