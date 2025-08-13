#!/bin/bash

# GenAI Demo 全棧應用啟動腳本
# 此腳本將同時啟動後端 Spring Boot 應用和前端 Next.js 應用

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
    
    # 進入前端目錄
    cd frontend
    
    # 檢查是否已安裝依賴
    if [ ! -d "node_modules" ]; then
        echo "📦 安裝前端依賴..."
        npm install
    else
        echo "✅ 前端依賴已安裝"
    fi
    
    # 創建環境變數文件（如果不存在）
    if [ ! -f ".env.local" ]; then
        echo "📝 創建環境變數文件..."
        cat > .env.local << EOF
# API 後端地址
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# 開發模式設置
NODE_ENV=development
EOF
        echo "✅ 環境變數文件已創建"
    fi
    
    # 返回根目錄
    cd ..
    
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
    sleep 10
    
    # 檢查後端是否啟動成功
    if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo "⚠️  後端可能還在啟動中，繼續等待..."
        sleep 10
    fi
    
    # 啟動前端（背景執行）
    echo "🔥 啟動前端 Next.js 應用..."
    cd frontend
    npm run dev > ../logs/frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "前端 PID: $FRONTEND_PID"
    cd ..
    
    # 保存 PID 到文件
    echo $BACKEND_PID > logs/backend.pid
    echo $FRONTEND_PID > logs/frontend.pid
    
    echo "✅ 應用程式啟動完成！"
    echo ""
    echo "📱 前端應用: http://localhost:3000"
    echo "🔧 後端 API: http://localhost:8080"
    echo "📊 後端健康檢查: http://localhost:8080/actuator/health"
    echo ""
    echo "📝 日誌文件:"
    echo "   - 後端: logs/backend.log"
    echo "   - 前端: logs/frontend.log"
    echo ""
    echo "🛑 停止應用程式: ./stop-fullstack.sh"
}

# 主函數
main() {
    check_requirements
    setup_backend
    setup_frontend
    start_applications
    
    echo "🎉 全棧應用啟動成功！"
    echo "按 Ctrl+C 停止監控，或執行 ./stop-fullstack.sh 停止所有服務"
    
    # 監控進程
    while true; do
        if ! kill -0 $BACKEND_PID 2>/dev/null; then
            echo "❌ 後端進程已停止"
            break
        fi
        
        if ! kill -0 $FRONTEND_PID 2>/dev/null; then
            echo "❌ 前端進程已停止"
            break
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
    
    if [ ! -z "$FRONTEND_PID" ] && kill -0 $FRONTEND_PID 2>/dev/null; then
        kill $FRONTEND_PID
        echo "✅ 前端已停止"
    fi
    
    # 清理 PID 文件
    rm -f logs/backend.pid logs/frontend.pid
    
    echo "👋 再見！"
    exit 0
}

# 設置信號處理
trap cleanup SIGINT SIGTERM

# 執行主函數
main
