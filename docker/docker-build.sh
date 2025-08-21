#!/bin/bash

# Docker 構建腳本 - ARM64 優化版本
# 適用於 MacBook M4 Silicon 開發和 AWS Graviton3 EKS 部署

set -e

echo "🚀 開始構建 GenAI Demo ARM64 優化映像..."

# 檢查當前架構
ARCH=$(uname -m)
echo "📱 當前架構：$ARCH"

if [[ "$ARCH" == "arm64" || "$ARCH" == "aarch64" ]]; then
    echo "✅ ARM64 架構確認，適合 MacBook M4 Silicon 和 AWS Graviton3"
else
    echo "⚠️  當前架構為 $ARCH，將使用跨平台構建"
fi

# 啟用 Docker BuildKit
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# 檢查 Docker 版本和 BuildKit 支援
echo "🔧 檢查 Docker 環境..."
docker version --format 'Docker版本: {{.Server.Version}}'
docker buildx version || echo "⚠️  BuildKit 不可用，使用傳統構建"

# 清理舊的映像和容器
echo "🧹 清理舊的映像和容器..."
docker system prune -f

# 創建並使用 buildx builder (支援多平台)
echo "🔨 設定多平台構建器..."
docker buildx create --name genai-builder --use --bootstrap 2>/dev/null || docker buildx use genai-builder

# 構建映像 (針對 ARM64/v8 優化)
echo "🔨 構建 ARM64 優化映像..."
docker buildx build \
    --platform linux/arm64/v8 \
    --build-arg BUILDKIT_INLINE_CACHE=1 \
    --cache-from type=local,src=/tmp/.buildx-cache \
    --cache-to type=local,dest=/tmp/.buildx-cache-new,mode=max \
    --tag genai-demo:latest \
    --tag genai-demo:arm64-graviton3 \
    --load \
    .

# 更新快取
rm -rf /tmp/.buildx-cache
mv /tmp/.buildx-cache-new /tmp/.buildx-cache 2>/dev/null || true

# 顯示映像資訊
echo "📊 映像大小資訊："
docker images genai-demo:latest

# 分析映像層
echo "🔍 映像層分析："
docker history --human genai-demo:latest

# 檢查映像架構
echo "🏗️  映像架構資訊："
docker inspect genai-demo:latest | grep -A 5 "Architecture"

# 測試映像啟動
echo "🧪 測試映像啟動..."
docker run --rm --name genai-demo-test -d -p 8081:8080 genai-demo:latest
sleep 10

# 健康檢查
if curl -f http://localhost:8081/actuator/health >/dev/null 2>&1; then
    echo "✅ 健康檢查通過"
else
    echo "❌ 健康檢查失敗"
fi

# 停止測試容器
docker stop genai-demo-test 2>/dev/null || true

echo "✅ 構建完成！"
echo ""
echo "🏷️  映像標籤："
echo "   genai-demo:latest"
echo "   genai-demo:arm64-graviton3"
echo ""
echo "🚀 本地啟動指令："
echo "   docker-compose up -d"
echo ""
echo "☁️  EKS 部署準備："
echo "   docker tag genai-demo:latest <ECR_URI>:latest"
echo "   docker push <ECR_URI>:latest"
echo ""
echo "📱 訪問應用："
echo "   http://localhost:8080/swagger-ui/index.html"
echo ""
echo "🔍 查看日誌："
echo "   docker-compose logs -f genai-demo"