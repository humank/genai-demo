#!/bin/bash

# GenAI Demo 全棧應用啟動腳本
# 此腳本將同時啟動後端 Spring Boot 應用和前端 Angular 應用

set -e

echo "🚀 GenAI Demo 全棧應用啟動腳本"
echo "=================================="

# 檢查必要的工具
check_requirements() {
    echo "📋 檢查系統需求..."
    
    # 檢查 Java
    if ! command -v java &> /dev/null; then
        echo "❌ 錯誤: 未找到 Java。請安裝 Java 21 或更高版本。"
        exit 1
    fi
    
    # 檢查 Node.js
    if ! command -v node &> /dev/null; then
        echo "❌ 錯誤: 未找到 Node.js。請安裝 Node.js 18 或更高版本。"
        exit 1
    fi
    
    # 檢查 npm
    if ! command -v npm &> /dev/null; then
        echo "❌ 錯誤: 未找到 npm。請安裝 npm。"
        exit 1
    fi
    
    # 檢查 Angular CLI
    if ! command -v ng &> /dev/null; then
        echo "⚠️  未找到 Angular CLI，正在安裝..."
        npm install -g @angular/cli@18
    fi
    
    echo "✅ 系統需求檢查完成"
}

# 設置後端
setup_backend() {
    echo "🔧 設置後端應用..."
    
    # 檢查 Gradle wrapper
    if [ ! -f "./gradlew" ]; then
        echo "❌ 錯誤: 未找到 gradlew。請確保在專案根目錄執行此腳本。"
        exit 1
    fi
    
    # 構建後端
    echo "📦 構建後端應用..."
    ./gradlew build -x test
    
    echo "✅ 後端設置完成"
}

# 設置前端
setup_frontend() {
    echo "🔧 設置前端應用..."
    
    # 設置 CMC 前端 (Next.js)
    if [ -d "cmc-frontend" ]; then
        echo "📦 設置 CMC 前端 (Next.js)..."
        cd cmc-frontend
        
        # 檢查是否已安裝依賴
        if [ ! -d "node_modules" ]; then
            echo "📦 安裝 CMC 前端依賴..."
            npm install
        else
            echo "✅ CMC 前端依賴已安裝"
        fi
        
        # 創建環境變數文件（如果不存在）
        if [ ! -f ".env.local" ]; then
            echo "📝 創建 CMC 環境變數文件..."
            cat > .env.local << EOF
# API 後端地址
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# 開發模式設置
NODE_ENV=development
EOF
            echo "✅ CMC 環境變數文件已創建"
        fi
        
        cd ..
    fi
    
    # 設置 Consumer 前端 (Angular)
    if [ -d "consumer-frontend" ]; then
        echo "📦 設置 Consumer 前端 (Angular)..."
        cd consumer-frontend
        
        # 檢查是否已安裝依賴
        if [ ! -d "node_modules" ]; then
            echo "📦 安裝 Consumer 前端依賴..."
            npm install --legacy-peer-deps
        else
            echo "✅ Consumer 前端依賴已安裝"
        fi
        
        # 確保環境變數文件存在
        if [ ! -f "src/environments/environment.ts" ]; then
            echo "📝 創建 Consumer 環境變數文件..."
            mkdir -p src/environments
            cat > src/environments/environment.ts << EOF
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  appName: '電商購物平台',
  version: '1.0.0'
};
EOF
            echo "✅ Consumer 環境變數文件已創建"
        fi
        
        cd ..
    fi
    
    echo "✅ 前端設置完成"
}

# 啟動應用
start_applications() {
    echo "🚀 啟動應用程式..."
    
    # 創建日誌目錄
    mkdir -p logs
    
    # 啟動後端（背景執行）
    echo "🔥 啟動後端 Spring Boot 應用..."
    ./gradlew bootRun > logs/backend.log 2>&1 &
    BACKEND_PID=$!
    echo "後端 PID: $BACKEND_PID"
    
    # 等待後端啟動
    echo "⏳ 等待後端啟動..."
    sleep 15
    
    # 檢查後端是否啟動成功
    echo "🔍 檢查後端健康狀態..."
    for i in {1..6}; do
        if curl -s http://localhost:8080/actuator/health > /dev/null; then
            echo "✅ 後端啟動成功！"
            break
        else
            echo "⏳ 後端還在啟動中... ($i/6)"
            sleep 10
        fi
    done
    
    # 啟動 CMC 前端（如果存在）
    if [ -d "cmc-frontend" ]; then
        echo "🔥 啟動 CMC 前端 Next.js 應用..."
        cd cmc-frontend
        PORT=3002 npm run dev > ../logs/cmc-frontend.log 2>&1 &
        CMC_FRONTEND_PID=$!
        echo "CMC 前端 PID: $CMC_FRONTEND_PID"
        cd ..
        echo $CMC_FRONTEND_PID > logs/cmc-frontend.pid
    fi
    
    # 啟動 Consumer 前端（如果存在）
    if [ -d "consumer-frontend" ]; then
        echo "🔥 啟動 Consumer 前端 Angular 應用..."
        cd consumer-frontend
        ng serve --port 3001 --host 0.0.0.0 > ../logs/consumer-frontend.log 2>&1 &
        CONSUMER_FRONTEND_PID=$!
        echo "Consumer 前端 PID: $CONSUMER_FRONTEND_PID"
        cd ..
        echo $CONSUMER_FRONTEND_PID > logs/consumer-frontend.pid
    fi
    
    # 保存後端 PID 到文件
    echo $BACKEND_PID > logs/backend.pid
    
    echo "✅ 應用程式啟動完成！"
    echo ""
    echo "🌐 應用程式訪問地址:"
    echo "   🛒 Consumer 前端 (Angular): http://localhost:3001"
    if [ -d "cmc-frontend" ]; then
        echo "   📱 CMC 前端 (Next.js): http://localhost:3002"
    fi
    echo "   🔧 後端 API: http://localhost:8080"
    echo "   📊 API 文檔 (Swagger): http://localhost:8080/swagger-ui/index.html"
    echo "   💚 健康檢查: http://localhost:8080/actuator/health"
    echo ""
    echo "📝 日誌文件:"
    echo "   - 後端: logs/backend.log"
    if [ -d "cmc-frontend" ]; then
        echo "   - CMC 前端: logs/cmc-frontend.log"
    fi
    if [ -d "consumer-frontend" ]; then
        echo "   - Consumer 前端: logs/consumer-frontend.log"
    fi
    echo ""
    echo "🛑 停止應用程式: ./scripts/stop-fullstack.sh"
}

# 主函數
main() {
    check_requirements
    setup_backend
    setup_frontend
    start_applications
    
    echo "🎉 全棧應用啟動成功！"
    echo "按 Ctrl+C 停止監控，或執行 ./scripts/stop-fullstack.sh 停止所有服務"
    
    # 監控進程
    while true; do
        if ! kill -0 $BACKEND_PID 2>/dev/null; then
            echo "❌ 後端進程已停止"
            break
        fi
        
        if [ ! -z "$CMC_FRONTEND_PID" ] && ! kill -0 $CMC_FRONTEND_PID 2>/dev/null; then
            echo "❌ CMC 前端進程已停止"
        fi
        
        if [ ! -z "$CONSUMER_FRONTEND_PID" ] && ! kill -0 $CONSUMER_FRONTEND_PID 2>/dev/null; then
            echo "❌ Consumer 前端進程已停止"
        fi
        
        sleep 5
    done
}

# 信號處理
cleanup() {
    echo ""
    echo "🛑 正在停止應用程式..."
    
    if [ ! -z "$BACKEND_PID" ] && kill -0 $BACKEND_PID 2>/dev/null; then
        kill $BACKEND_PID
        echo "✅ 後端已停止"
    fi
    
    if [ ! -z "$CMC_FRONTEND_PID" ] && kill -0 $CMC_FRONTEND_PID 2>/dev/null; then
        kill $CMC_FRONTEND_PID
        echo "✅ CMC 前端已停止"
    fi
    
    if [ ! -z "$CONSUMER_FRONTEND_PID" ] && kill -0 $CONSUMER_FRONTEND_PID 2>/dev/null; then
        kill $CONSUMER_FRONTEND_PID
        echo "✅ Consumer 前端已停止"
    fi
    
    # 清理 PID 文件
    rm -f logs/backend.pid logs/cmc-frontend.pid logs/consumer-frontend.pid
    
    echo "👋 再見！"
    exit 0
}

# 設置信號處理
trap cleanup SIGINT SIGTERM

# 執行主函數
main