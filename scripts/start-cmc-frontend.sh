#!/bin/bash

# GenAI Demo CMC 前端應用啟動腳本

echo "🚀 啟動 GenAI Demo CMC 前端應用 (Next.js)"
echo "=========================================="

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

# 檢查 cmc-frontend 目錄
if [ ! -d "cmc-frontend" ]; then
    echo "❌ 錯誤: 未找到 cmc-frontend 目錄。"
    exit 1
fi

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

# 應用配置
NEXT_PUBLIC_APP_NAME=商務管理中心
NEXT_PUBLIC_APP_VERSION=1.0.0

# 開發模式設置
NODE_ENV=development
EOF
    echo "✅ CMC 環境變數文件已創建"
fi

# 創建日誌目錄
mkdir -p ../logs

echo "🔥 啟動 CMC 前端 Next.js 應用..."
PORT=3002 npm run dev > ../logs/cmc-frontend.log 2>&1 &
CMC_FRONTEND_PID=$!

echo "CMC 前端 PID: $CMC_FRONTEND_PID"
echo $CMC_FRONTEND_PID > ../logs/cmc-frontend.pid

cd ..

echo "⏳ 等待前端啟動..."
sleep 10

# 檢查服務是否啟動成功
echo "🔍 檢查 CMC 前端狀態..."
for i in {1..6}; do
    if curl -s http://localhost:3002 > /dev/null; then
        echo "✅ CMC 前端啟動成功！"
        break
    else
        echo "⏳ CMC 前端還在啟動中... ($i/6)"
        sleep 5
    fi
done

echo "✅ CMC 前端啟動完成！"
echo ""
echo "🌐 CMC 前端訪問地址:"
echo "   📱 商務管理中心: http://localhost:3002"
echo "   📝 日誌文件: logs/cmc-frontend.log"
echo ""
echo "🛑 停止服務: ./scripts/stop-cmc-frontend.sh"
echo "🎉 CMC 前端應用啟動完成！"