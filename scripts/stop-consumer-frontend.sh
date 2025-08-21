#!/bin/bash

# GenAI Demo Consumer 前端應用停止腳本

echo "🛑 停止 GenAI Demo Consumer 前端應用"
echo "=================================="

# 停止 Consumer 前端
if [ -f "logs/consumer-frontend.pid" ]; then
    CONSUMER_FRONTEND_PID=$(cat logs/consumer-frontend.pid)
    if kill -0 $CONSUMER_FRONTEND_PID 2>/dev/null; then
        echo "🔥 停止 Consumer 前端進程 (PID: $CONSUMER_FRONTEND_PID)..."
        kill $CONSUMER_FRONTEND_PID
        echo "✅ Consumer 前端已停止"
    else
        echo "⚠️  Consumer 前端進程不存在"
    fi
    rm -f logs/consumer-frontend.pid
else
    echo "⚠️  未找到 Consumer 前端 PID 文件"
fi

# 額外清理
echo "🧹 清理殘留進程..."

# 清理 Angular 進程
NG_PIDS=$(pgrep -f "ng serve" 2>/dev/null || true)
if [ ! -z "$NG_PIDS" ]; then
    echo "清理 Angular 進程: $NG_PIDS"
    kill $NG_PIDS 2>/dev/null || true
fi

# 清理 3001 端口
PORT_3001_PID=$(lsof -ti:3001 2>/dev/null || true)
if [ ! -z "$PORT_3001_PID" ]; then
    echo "清理端口 3001 的進程: $PORT_3001_PID"
    kill $PORT_3001_PID 2>/dev/null || true
fi

echo "✅ Consumer 前端應用已停止"
echo "👋 再見！"