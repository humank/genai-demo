#!/bin/bash

# GenAI Demo 後端應用啟動腳本

echo "🚀 啟動 GenAI Demo 後端應用"
echo "============================"

# 檢查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 錯誤: 未找到 Java。請安裝 Java 21 或更高版本。"
    exit 1
fi

# 檢查 Gradle wrapper
if [ ! -f "./gradlew" ]; then
    echo "❌ 錯誤: 未找到 gradlew。請確保在專案根目錄執行此腳本。"
    exit 1
fi

# 創建日誌目錄
mkdir -p logs

echo "📦 構建後端應用..."
./gradlew build -x test

echo "🔥 啟動後端 Spring Boot 應用..."
./gradlew bootRun > logs/backend.log 2>&1 &
BACKEND_PID=$!

echo "後端 PID: $BACKEND_PID"
echo $BACKEND_PID > logs/backend.pid

echo "⏳ 等待後端啟動..."
sleep 15

# 檢查後端是否啟動成功
echo "🔍 檢查後端健康狀態..."
for i in {1..6}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "✅ 後端啟動成功！"
        echo ""
        echo "🌐 後端服務訪問地址:"
        echo "   🔧 API: http://localhost:8080"
        echo "   📊 Swagger UI: http://localhost:8080/swagger-ui/index.html"
        echo "   💚 健康檢查: http://localhost:8080/actuator/health"
        echo "   📝 日誌: logs/backend.log"
        echo ""
        echo "🛑 停止服務: ./scripts/stop-backend.sh"
        break
    else
        echo "⏳ 後端還在啟動中... ($i/6)"
        sleep 10
    fi
done

echo "🎉 後端應用啟動完成！"