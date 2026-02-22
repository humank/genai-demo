#!/usr/bin/env bash
set -euo pipefail

###############################################################################
# deploy-frontend.sh — 前端應用部署腳本
#
# 功能：
#   1. 建置 Consumer / CMC 前端 Docker 映像
#   2. 推送至 ECR
#   3. 更新 K8s Argo Rollouts（觸發 Canary 部署）
#
# 用法：
#   ./deploy-frontend.sh [選項]
#
# 選項：
#   -e, --env          部署環境 (development|staging|production)  預設: development
#   -a, --app          部署目標 (all|consumer|cmc)                預設: all
#   -t, --tag          映像標籤                                   預設: git short SHA
#   -r, --region       AWS 區域                                   預設: ap-northeast-1
#       --api-url      後端 API URL                               預設: 依環境決定
#       --skip-build   跳過 Docker 建置（僅推送已存在的映像）
#       --skip-push    跳過 ECR 推送（僅建置映像）
#       --skip-deploy  跳過 K8s 部署（僅建置並推送）
#       --dry-run      僅顯示將執行的指令，不實際執行
#   -h, --help         顯示說明
###############################################################################

# ── 顏色 ──
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()   { echo -e "${GREEN}[✓]${NC} $*"; }
warn()  { echo -e "${YELLOW}[!]${NC} $*"; }
err()   { echo -e "${RED}[✗]${NC} $*" >&2; }
info()  { echo -e "${BLUE}[→]${NC} $*"; }

# ── 預設值 ──
ENVIRONMENT="development"
APP_TARGET="all"
IMAGE_TAG=""
AWS_REGION="ap-northeast-1"
API_URL=""
SKIP_BUILD=false
SKIP_PUSH=false
SKIP_DEPLOY=false
DRY_RUN=false
PROJECT_NAME="genai-demo"
K8S_NAMESPACE="genai-demo"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="${SCRIPT_DIR}/frontend"

# ── 環境對應 API URL ──
# NOTE: 無自訂域名，部署後請以 --api-url 傳入實際 EKS ALB 端點
# 可用以下指令取得後端 ALB DNS：
#   kubectl get ingress genai-demo-ingress -n genai-demo -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
declare -A ENV_API_URLS=(
  ["development"]="http://localhost:8080"
  ["staging"]="http://localhost:8080"
  ["production"]="http://localhost:8080"
)

usage() {
  head -30 "$0" | grep '^#' | sed 's/^# \?//'
  exit 0
}

# ── 參數解析 ──
while [[ $# -gt 0 ]]; do
  case $1 in
    -e|--env)        ENVIRONMENT="$2"; shift 2 ;;
    -a|--app)        APP_TARGET="$2"; shift 2 ;;
    -t|--tag)        IMAGE_TAG="$2"; shift 2 ;;
    -r|--region)     AWS_REGION="$2"; shift 2 ;;
    --api-url)       API_URL="$2"; shift 2 ;;
    --skip-build)    SKIP_BUILD=true; shift ;;
    --skip-push)     SKIP_PUSH=true; shift ;;
    --skip-deploy)   SKIP_DEPLOY=true; shift ;;
    --dry-run)       DRY_RUN=true; shift ;;
    -h|--help)       usage ;;
    *) err "未知選項: $1"; usage ;;
  esac
done

# ── 驗證環境 ──
if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
  err "無效的環境: $ENVIRONMENT (可用: development, staging, production)"
  exit 1
fi

if [[ ! "$APP_TARGET" =~ ^(all|consumer|cmc)$ ]]; then
  err "無效的部署目標: $APP_TARGET (可用: all, consumer, cmc)"
  exit 1
fi

# ── 自動偵測 ──
if [[ -z "$IMAGE_TAG" ]]; then
  IMAGE_TAG="$(git rev-parse --short HEAD 2>/dev/null || echo 'latest')"
fi

if [[ -z "$API_URL" ]]; then
  API_URL="${ENV_API_URLS[$ENVIRONMENT]}"
fi

AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text 2>/dev/null || true)"
if [[ -z "$AWS_ACCOUNT_ID" ]]; then
  err "無法取得 AWS Account ID，請確認 AWS CLI 已設定正確的認證"
  exit 1
fi

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
CONSUMER_REPO="${ECR_REGISTRY}/${PROJECT_NAME}/consumer-frontend"
CMC_REPO="${ECR_REGISTRY}/${PROJECT_NAME}/cmc-frontend"

# ── 顯示部署資訊 ──
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║           前端應用部署                                   ║"
echo "╠══════════════════════════════════════════════════════════╣"
printf "║  環境:       %-42s║\n" "$ENVIRONMENT"
printf "║  目標:       %-42s║\n" "$APP_TARGET"
printf "║  映像標籤:   %-42s║\n" "$IMAGE_TAG"
printf "║  AWS 區域:   %-42s║\n" "$AWS_REGION"
printf "║  AWS 帳號:   %-42s║\n" "$AWS_ACCOUNT_ID"
printf "║  API URL:    %-42s║\n" "$API_URL"
printf "║  ECR:        %-42s║\n" "$ECR_REGISTRY"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

if $DRY_RUN; then
  warn "Dry-run 模式：僅顯示指令，不實際執行"
  echo ""
fi

run_cmd() {
  info "$*"
  if ! $DRY_RUN; then
    eval "$@"
  fi
}

# ══════════════════════════════════════════════════════════════
# Step 1: ECR 登入
# ══════════════════════════════════════════════════════════════
if ! $SKIP_PUSH; then
  log "登入 ECR..."
  run_cmd "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}"
fi

# ══════════════════════════════════════════════════════════════
# Step 2: 建置 Docker 映像
# ══════════════════════════════════════════════════════════════
build_image() {
  local app_name="$1"
  local dockerfile="$2"
  local repo="$3"

  log "建置 ${app_name} 映像..."
  run_cmd "docker build \
    -f ${FRONTEND_DIR}/${dockerfile} \
    --build-arg NEXT_PUBLIC_API_URL=${API_URL} \
    -t ${repo}:${IMAGE_TAG} \
    -t ${repo}:latest \
    --platform linux/amd64 \
    ${FRONTEND_DIR}"
}

if ! $SKIP_BUILD; then
  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "consumer" ]]; then
    build_image "Consumer Frontend" "Dockerfile.consumer" "$CONSUMER_REPO"
  fi

  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "cmc" ]]; then
    build_image "CMC Frontend" "Dockerfile.cmc" "$CMC_REPO"
  fi

  log "Docker 映像建置完成"
else
  warn "跳過 Docker 建置"
fi

# ══════════════════════════════════════════════════════════════
# Step 3: 推送至 ECR
# ══════════════════════════════════════════════════════════════
push_image() {
  local app_name="$1"
  local repo="$2"

  log "推送 ${app_name} 映像至 ECR..."
  run_cmd "docker push ${repo}:${IMAGE_TAG}"
  run_cmd "docker push ${repo}:latest"
}

if ! $SKIP_PUSH; then
  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "consumer" ]]; then
    push_image "Consumer Frontend" "$CONSUMER_REPO"
  fi

  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "cmc" ]]; then
    push_image "CMC Frontend" "$CMC_REPO"
  fi

  log "ECR 推送完成"
else
  warn "跳過 ECR 推送"
fi

# ══════════════════════════════════════════════════════════════
# Step 4: 更新 K8s Rollout（觸發 Canary 部署）
# ══════════════════════════════════════════════════════════════
deploy_rollout() {
  local app_name="$1"
  local rollout_name="$2"
  local repo="$3"
  local rollout_file="$4"

  log "部署 ${app_name} 至 EKS (Canary Rollout)..."

  # 確認 kubectl 可連線
  if ! kubectl get namespace "${K8S_NAMESPACE}" &>/dev/null; then
    warn "Namespace ${K8S_NAMESPACE} 不存在，嘗試建立..."
    run_cmd "kubectl create namespace ${K8S_NAMESPACE} || true"
  fi

  # 替換 K8s manifest 中的佔位符並套用
  local tmp_manifest="/tmp/${rollout_name}-deploy.yaml"
  sed \
    -e "s|ACCOUNT_ID.dkr.ecr.ap-northeast-1.amazonaws.com|${ECR_REGISTRY}|g" \
    -e "s|:latest|:${IMAGE_TAG}|g" \
    -e "s|ACCOUNT_ID|${AWS_ACCOUNT_ID}|g" \
    -e "s|value: \"https://api.kimkao.io\"|value: \"${API_URL}\"|g" \
    -e "s|value: \"http://localhost:8080\"|value: \"${API_URL}\"|g" \
    "${rollout_file}" > "${tmp_manifest}"

  run_cmd "kubectl apply -f ${tmp_manifest}"

  # 如果有 Argo Rollouts，觸發重啟
  if kubectl get crd rollouts.argoproj.io &>/dev/null 2>&1; then
    info "偵測到 Argo Rollouts，觸發 Canary 部署..."
    run_cmd "kubectl argo rollouts set image ${rollout_name} \
      ${rollout_name}=${repo}:${IMAGE_TAG} \
      -n ${K8S_NAMESPACE} || true"
  fi

  rm -f "${tmp_manifest}"
}

if ! $SKIP_DEPLOY; then
  # 確認 kubectl context
  CURRENT_CONTEXT="$(kubectl config current-context 2>/dev/null || echo 'none')"
  info "目前 kubectl context: ${CURRENT_CONTEXT}"

  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "consumer" ]]; then
    deploy_rollout \
      "Consumer Frontend" \
      "genai-demo-consumer-frontend" \
      "$CONSUMER_REPO" \
      "${SCRIPT_DIR}/infrastructure/k8s/rollouts/consumer-frontend-canary.yaml"
  fi

  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "cmc" ]]; then
    deploy_rollout \
      "CMC Frontend" \
      "genai-demo-cmc-frontend" \
      "$CMC_REPO" \
      "${SCRIPT_DIR}/infrastructure/k8s/rollouts/cmc-frontend-canary.yaml"
  fi

  log "K8s 部署完成"
else
  warn "跳過 K8s 部署"
fi

# ══════════════════════════════════════════════════════════════
# 完成
# ══════════════════════════════════════════════════════════════
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║  部署完成                                                ║"
echo "╠══════════════════════════════════════════════════════════╣"

if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "consumer" ]]; then
  printf "║  Consumer: %-44s║\n" "(透過 EKS ALB 或 kubectl port-forward 存取)"
fi

if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "cmc" ]]; then
  printf "║  CMC:      %-44s║\n" "(透過 EKS ALB 或 kubectl port-forward 存取)"
fi

printf "║  映像標籤: %-44s║\n" "$IMAGE_TAG"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""

if ! $SKIP_DEPLOY; then
  info "查看部署狀態："
  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "consumer" ]]; then
    echo "  kubectl argo rollouts status genai-demo-consumer-frontend -n ${K8S_NAMESPACE} -w"
  fi
  if [[ "$APP_TARGET" == "all" || "$APP_TARGET" == "cmc" ]]; then
    echo "  kubectl argo rollouts status genai-demo-cmc-frontend -n ${K8S_NAMESPACE} -w"
  fi
  echo ""
fi
