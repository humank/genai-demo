#!/bin/bash

# GenAI Demo 全棧應用停止腳本

echo "🛑 停止 GenAI Demo 全棧應用"
echo "============================="

# 停止後端
stop_backend() {
    echo "🔥 停止後端 Spring Boot 應用..."
    
    if [ -f "logs/backend.pid" ]; then
        BACKEND_PID=$(cat logs/backend.pid)
        if kill -0 $BACKEND_PID 2>/dev/null; then
            echo "   停止後端進程 (PID: $BACKEND_PID)..."
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
                echo "   強制停止後端進程..."
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
    
    # 額外清理：殺死可能殘留的 Spring Boot 進程
    SPRING_PIDS=$(pgrep -f "spring-boot" 2>/dev/null || true)
    if [ ! -z "$SPRING_PIDS" ]; then
        echo "   清理殘留的 Spring Boot 進程: $SPRING_PIDS"
        kill $SPRING_PIDS 2>/dev/null || true
    fi
    
    # 清理 Gradle daemon 進程
    GRADLE_PIDS=$(pgrep -f "GradleDaemon" 2>/dev/null || true)
    if [ ! -z "$GRADLE_PIDS" ]; then
        echo "   清理 Gradle daemon 進程: $GRADLE_PIDS"
        kill $GRADLE_PIDS 2>/dev/null || true
    fi
}

# 停止 CMC 前端 (Next.js)
stop_cmc_frontend() {
    if [ -d "cmc-frontend" ]; then
        echo "🔥 停止 CMC 前端 Next.js 應用..."
        
        if [ -f "logs/cmc-frontend.pid" ]; then
            CMC_FRONTEND_PID=$(cat logs/cmc-frontend.pid)
            if kill -0 $CMC_FRONTEND_PID 2>/dev/null; then
                echo "   停止 CMC 前端進程 (PID: $CMC_FRONTEND_PID)..."
                kill $CMC_FRONTEND_PID
                echo "✅ CMC 前端已停止"
            else
                echo "⚠️  CMC 前端進程不存在"
            fi
            rm -f logs/cmc-frontend.pid
        else
            echo "⚠️  未找到 CMC 前端 PID 文件"
        fi
        
        # 額外清理：殺死可能殘留的 Next.js 進程
        NEXTJS_PIDS=$(pgrep -f "next-server" 2>/dev/null || true)
        if [ ! -z "$NEXTJS_PIDS" ]; then
            echo "   清理殘留的 Next.js 進程: $NEXTJS_PIDS"
            kill $NEXTJS_PIDS 2>/dev/null || true
        fi
    fi
}

# 停止 Consumer 前端 (Angular)
stop_consumer_frontend() {
    if [ -d "consumer-frontend" ]; then
        echo "🔥 停止 Consumer 前端 Angular 應用..."
        
        if [ -f "logs/consumer-frontend.pid" ]; then
            CONSUMER_FRONTEND_PID=$(cat logs/consumer-frontend.pid)
            if kill -0 $CONSUMER_FRONTEND_PID 2>/dev/null; then
                echo "   停止 Consumer 前端進程 (PID: $CONSUMER_FRONTEND_PID)..."
                kill $CONSUMER_FRONTEND_PID
                echo "✅ Consumer 前端已停止"
            else
                echo "⚠️  Consumer 前端進程不存在"
            fi
            rm -f logs/consumer-frontend.pid
        else
            echo "⚠️  未找到 Consumer 前端 PID 文件"
        fi
        
        # 額外清理：殺死可能殘留的 Angular 進程
        NG_PIDS=$(pgrep -f "ng serve" 2>/dev/null || true)
        if [ ! -z "$NG_PIDS" ]; then
            echo "   清理殘留的 Angular 進程: $NG_PIDS"
            kill $NG_PIDS 2>/dev/null || true
        fi
    fi
}

# 清理端口佔用
cleanup_ports() {
    echo "🧹 清理端口佔用..."
    
    # 清理 8080 端口 (後端)
    PORT_8080_PID=$(lsof -ti:8080 2>/dev/null || true)
    if [ ! -z "$PORT_8080_PID" ]; then
        echo "   清理端口 8080 的進程: $PORT_8080_PID"
        kill $PORT_8080_PID 2>/dev/null || true
    fi
    
    # 清理 3001 端口 (Consumer 前端)
    PORT_3001_PID=$(lsof -ti:3001 2>/dev/null || true)
    if [ ! -z "$PORT_3001_PID" ]; then
        echo "   清理端口 3001 的進程: $PORT_3001_PID"
        kill $PORT_3001_PID 2>/dev/null || true
    fi
    
    # 清理 3002 端口 (CMC 前端)
    PORT_3002_PID=$(lsof -ti:3002 2>/dev/null || true)
    if [ ! -z "$PORT_3002_PID" ]; then
        echo "   清理端口 3002 的進程: $PORT_3002_PID"
        kill $PORT_3002_PID 2>/dev/null || true
    fi
}

# 清理臨時文件
cleanup_temp_files() {
    echo "🧹 清理臨時文件..."
    
    # 清理日誌文件（可選）
    if [ "$1" = "--clean-logs" ]; then
        echo "   清理日誌文件..."
        rm -f logs/*.log
    fi
    
    # 清理 PID 文件
    rm -f logs/*.pid
    
    echo "✅ 臨時文件清理完成"
}

# 主函數
main() {
    stop_backend
    stop_cmc_frontend
    stop_consumer_frontend
    cleanup_ports
    cleanup_temp_files $1
    
    echo ""
    echo "✅ 全棧應用已完全停止"
    echo "👋 再見！"
}

# 顯示幫助信息
show_help() {
    echo "GenAI Demo 全棧應用停止腳本"
    echo ""
    echo "用法:"
    echo "  ./scripts/stop-fullstack.sh [選項]"
    echo ""
    echo "選項:"
    echo "  --clean-logs    同時清理日誌文件"
    echo "  --help         顯示此幫助信息"
    echo ""
    echo "範例:"
    echo "  ./scripts/stop-fullstack.sh                # 停止所有服務"
    echo "  ./scripts/stop-fullstack.sh --clean-logs   # 停止所有服務並清理日誌"
}

# 處理命令行參數
case "$1" in
    --help)
        show_help
        exit 0
        ;;
    *)
        main $1
        ;;
esac