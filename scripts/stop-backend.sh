#!/bin/bash

# GenAI Demo 後端應用停止腳本

echo "🛑 停止 GenAI Demo 後端應用"
echo "=========================="

# 停止後端
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo "🔥 停止後端進程 (PID: $BACKEND_PID)..."
        kill $BACKEND_PID
        
        # 等待進程結束
        for i in {1..10}; do
            if ! kill -0 $BACKEND_PID 2>/dev/null; then
                echo "✅ 後端已停止"
                break
            fi
            sleep 1
        done
        
        # 如果還沒停止，強制殺死
        if kill -0 $BACKEND_PID 2>/dev/null; then
            echo "強制停止後端進程..."
            kill -9 $BACKEND_PID
            echo "✅ 後端已強制停止"
        fi
    else
        echo "⚠️  後端進程不存在"
    fi
    rm -f logs/backend.pid
else
    echo "⚠️  未找到後端 PID 文件"
fi

# 額外清理
echo "🧹 清理殘留進程..."

# 清理 Spring Boot 進程
SPRING_PIDS=$(pgrep -f "spring-boot" 2>/dev/null || true)
if [ ! -z "$SPRING_PIDS" ]; then
    echo "清理 Spring Boot 進程: $SPRING_PIDS"
    kill $SPRING_PIDS 2>/dev/null || true
fi

# 清理 8080 端口
PORT_8080_PID=$(lsof -ti:8080 2>/dev/null || true)
if [ ! -z "$PORT_8080_PID" ]; then
    echo "清理端口 8080 的進程: $PORT_8080_PID"
    kill $PORT_8080_PID 2>/dev/null || true
fi

echo "✅ 後端應用已停止"
echo "👋 再見！"