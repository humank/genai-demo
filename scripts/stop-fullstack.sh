#!/bin/bash

# GenAI Demo 全棧應用停止腳本

echo "🛑 停止 GenAI Demo 全棧應用"
echo "============================="

# 停止後端
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo "🔥 停止後端應用 (PID: $BACKEND_PID)..."
        kill $BACKEND_PID
        echo "✅ 後端已停止"
    else
        echo "⚠️  後端進程不存在"
    fi
    rm -f logs/backend.pid
else
    echo "⚠️  未找到後端 PID 文件"
fi

# 停止前端
if [ -f "logs/cmc-frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/cmc-frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "🔥 停止前端應用 (PID: $FRONTEND_PID)..."
        kill $FRONTEND_PID
        echo "✅ 前端已停止"
    else
        echo "⚠️  前端進程不存在"
    fi
    rm -f logs/cmc-frontend.pid
else
    echo "⚠️  未找到前端 PID 文件"
fi

# 額外清理：殺死可能殘留的進程
echo "🧹 清理殘留進程..."

# 查找並殺死 Spring Boot 進程
SPRING_PIDS=$(pgrep -f "spring-boot")
if [ ! -z "$SPRING_PIDS" ]; then
    echo "🔥 發現 Spring Boot 進程: $SPRING_PIDS"
    kill $SPRING_PIDS
fi

# 查找並殺死 Next.js 進程
NEXTJS_PIDS=$(pgrep -f "next-server")
if [ ! -z "$NEXTJS_PIDS" ]; then
    echo "🔥 發現 Next.js 進程: $NEXTJS_PIDS"
    kill $NEXTJS_PIDS
fi

echo "✅ 全棧應用已完全停止"
echo "👋 再見！"
