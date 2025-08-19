#!/bin/bash

# GenAI Demo CMC 前端應用停止腳本

echo "🛑 停止 GenAI Demo CMC 前端應用"
echo "=============================="

# 停止 CMC 前端
if [ -f "logs/cmc-frontend.pid" ]; then
    CMC_FRONTEND_PID=$(cat logs/cmc-frontend.pid)
    if kill -0 $CMC_FRONTEND_PID 2>/dev/null; then
        echo "🔥 停止 CMC 前端進程 (PID: $CMC_FRONTEND_PID)..."
        kill $CMC_FRONTEND_PID
        
        # 等待進程結束
        for i in {1..10}; do
            if ! kill -0 $CMC_FRONTEND_PID 2>/dev/null; then
                echo "✅ CMC 前端已停止"
                break
            fi
            sleep 1
        done
        
        # 如果還沒停止，強制殺死
        if kill -0 $CMC_FRONTEND_PID 2>/dev/null; then
            echo "強制停止 CMC 前端進程..."
            kill -9 $CMC_FRONTEND_PID
            echo "✅ CMC 前端已強制停止"
        fi
    else
        echo "⚠️  CMC 前端進程不存在"
    fi
    rm -f logs/cmc-frontend.pid
else
    echo "⚠️  未找到 CMC 前端 PID 文件"
fi

# 額外清理
echo "🧹 清理殘留進程..."

# 清理 Next.js 進程
NEXTJS_PIDS=$(pgrep -f "next-server" 2>/dev/null || true)
if [ ! -z "$NEXTJS_PIDS" ]; then
    echo "清理 Next.js 進程: $NEXTJS_PIDS"
    kill $NEXTJS_PIDS 2>/dev/null || true
fi

# 清理可能的 npm run dev 進程
NPM_DEV_PIDS=$(pgrep -f "npm run dev" 2>/dev/null || true)
if [ ! -z "$NPM_DEV_PIDS" ]; then
    echo "清理 npm run dev 進程: $NPM_DEV_PIDS"
    kill $NPM_DEV_PIDS 2>/dev/null || true
fi

# 清理 3002 端口
PORT_3002_PID=$(lsof -ti:3002 2>/dev/null || true)
if [ ! -z "$PORT_3002_PID" ]; then
    echo "清理端口 3002 的進程: $PORT_3002_PID"
    kill $PORT_3002_PID 2>/dev/null || true
fi

echo "✅ CMC 前端應用已停止"
echo "👋 再見！"