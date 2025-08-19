#!/bin/bash

# GenAI Demo Consumer 前端應用啟動腳本

echo "🚀 啟動 GenAI Demo Consumer 前端應用 (Angular)"
echo "=============================================="

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

# 檢查 consumer-frontend 目錄
if [ ! -d "consumer-frontend" ]; then
    echo "❌ 錯誤: 未找到 consumer-frontend 目錄。"
    exit 1
fi

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
    echo "📝 創建環境變數文件..."
    mkdir -p src/environments
    cat > src/environments/environment.ts << EOF
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080',
  appName: '電商購物平台',
  version: '1.0.0'
};
EOF
    echo "✅ 環境變數文件已創建"
fi

# 創建日誌目錄
mkdir -p ../logs

echo "🔥 啟動 Consumer 前端 Angular 應用..."
ng serve --port 3001 --host 0.0.0.0 > ../logs/consumer-frontend.log 2>&1 &
CONSUMER_FRONTEND_PID=$!

echo "Consumer 前端 PID: $CONSUMER_FRONTEND_PID"
echo $CONSUMER_FRONTEND_PID > ../logs/consumer-frontend.pid

cd ..

echo "⏳ 等待前端啟動..."
sleep 10

echo "✅ Consumer 前端啟動完成！"
echo ""
echo "🌐 Consumer 前端訪問地址:"
echo "   🛒 應用: http://localhost:3001"
echo "   📝 日誌: logs/consumer-frontend.log"
echo ""
echo "🛑 停止服務: ./scripts/stop-consumer-frontend.sh"
echo "🎉 Consumer 前端應用啟動完成！"