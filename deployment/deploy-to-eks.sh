#!/bin/bash

# EKS 部署腳本 - AWS Graviton3 優化版本

set -e

# 配置變數
ECR_REGISTRY="${ECR_REGISTRY:-your-account-id.dkr.ecr.region.amazonaws.com}"
ECR_REPOSITORY="${ECR_REPOSITORY:-genai-demo}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
EKS_CLUSTER_NAME="${EKS_CLUSTER_NAME:-genai-demo-cluster}"
AWS_REGION="${AWS_REGION:-ap-northeast-1}"

echo "🚀 開始部署到 AWS EKS (Graviton3)..."
echo "📋 部署配置："
echo "   ECR Registry: $ECR_REGISTRY"
echo "   Repository: $ECR_REPOSITORY"
echo "   Image Tag: $IMAGE_TAG"
echo "   EKS Cluster: $EKS_CLUSTER_NAME"
echo "   AWS Region: $AWS_REGION"

# 檢查必要工具
echo "🔧 檢查必要工具..."
command -v aws >/dev/null 2>&1 || { echo "❌ AWS CLI 未安裝"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo "❌ kubectl 未安裝"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ Docker 未安裝"; exit 1; }

# 檢查 AWS 認證
echo "🔐 檢查 AWS 認證..."
aws sts get-caller-identity >/dev/null || { echo "❌ AWS 認證失敗"; exit 1; }

# 登入 ECR
echo "🔑 登入 ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

# 檢查 ECR 倉庫是否存在
echo "📦 檢查 ECR 倉庫..."
if ! aws ecr describe-repositories --repository-names $ECR_REPOSITORY --region $AWS_REGION >/dev/null 2>&1; then
    echo "📦 創建 ECR 倉庫..."
    aws ecr create-repository \
        --repository-name $ECR_REPOSITORY \
        --region $AWS_REGION \
        --image-scanning-configuration scanOnPush=true \
        --image-tag-mutability MUTABLE
fi

# 構建並推送映像
echo "🔨 構建 ARM64 映像..."
docker buildx build \
    --platform linux/arm64/v8 \
    --tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG \
    --tag $ECR_REGISTRY/$ECR_REPOSITORY:graviton3-latest \
    --push \
    .

# 更新 kubeconfig
echo "⚙️  更新 kubeconfig..."
aws eks update-kubeconfig --region $AWS_REGION --name $EKS_CLUSTER_NAME

# 檢查 EKS 連接
echo "🔍 檢查 EKS 連接..."
kubectl cluster-info

# 檢查 Graviton3 節點
echo "🖥️  檢查 Graviton3 節點..."
kubectl get nodes -l kubernetes.io/arch=arm64 -o wide

# 更新 Kubernetes 配置中的映像 URI
echo "📝 更新 Kubernetes 配置..."
sed -i.bak "s|<ECR_URI>|$ECR_REGISTRY|g" k8s/deployment.yaml

# 部署 ConfigMap
echo "🗂️  部署 ConfigMap..."
kubectl apply -f k8s/configmap.yaml

# 部署應用
echo "🚀 部署應用..."
kubectl apply -f k8s/deployment.yaml

# 等待部署完成
echo "⏳ 等待部署完成..."
kubectl rollout status deployment/genai-demo --timeout=300s

# 檢查 Pod 狀態
echo "📊 檢查 Pod 狀態..."
kubectl get pods -l app=genai-demo -o wide

# 檢查服務狀態
echo "🌐 檢查服務狀態..."
kubectl get svc genai-demo-service

# 檢查 Ingress 狀態
echo "🔗 檢查 Ingress 狀態..."
kubectl get ingress genai-demo-ingress

# 獲取 Load Balancer URL
echo "🌍 獲取訪問 URL..."
LB_URL=$(kubectl get ingress genai-demo-ingress -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')
if [ -n "$LB_URL" ]; then
    echo "✅ 應用已部署成功！"
    echo "🌐 訪問 URL: http://$LB_URL"
    echo "📚 API 文檔: http://$LB_URL/swagger-ui/index.html"
else
    echo "⏳ Load Balancer 正在配置中，請稍後檢查..."
fi

# 顯示有用的命令
echo ""
echo "🔧 有用的命令："
echo "   查看 Pod 日誌: kubectl logs -f deployment/genai-demo"
echo "   查看 Pod 狀態: kubectl get pods -l app=genai-demo"
echo "   進入 Pod: kubectl exec -it deployment/genai-demo -- sh"
echo "   查看服務: kubectl get svc"
echo "   查看 HPA: kubectl get hpa"
echo ""
echo "🔄 更新部署："
echo "   kubectl set image deployment/genai-demo genai-demo=$ECR_REGISTRY/$ECR_REPOSITORY:new-tag"
echo ""
echo "🗑️  清理資源："
echo "   kubectl delete -f k8s/"

# 恢復原始配置文件
mv k8s/deployment.yaml.bak k8s/deployment.yaml 2>/dev/null || true

echo "✅ 部署腳本執行完成！"