# Deployment 目錄

此目錄包含專案的部署相關檔案。

## 檔案說明

### Kubernetes 部署

- `k8s/` - Kubernetes 部署配置檔案
  - `configmap.yaml` - 配置映射
  - `deployment.yaml` - 部署配置

### AWS EKS 部署

- `deploy-to-eks.sh` - EKS 部署腳本
- `aws-eks-architecture.md` - AWS EKS 架構說明

### Docker 部署

- `docker/` - Docker Compose 配置
  - `docker-compose-redis-dev.yml` - Redis 開發環境
  - `docker-compose-redis-ha.yml` - Redis 高可用環境

## 使用方式

### 後端部署

```bash
# 部署到 EKS
./deployment/deploy-to-eks.sh

# 使用 kubectl 部署
kubectl apply -f deployment/k8s/
```

### 前端一鍵部署

專案根目錄提供 `deploy-frontend.sh` 腳本，整合 Docker 建置、ECR 推送、EKS Canary 部署：

```bash
# 部署兩個前端至開發環境（自動偵測 AWS Account ID、git SHA 作為映像標籤）
./deploy-frontend.sh -e development

# 僅部署 Consumer 至 Staging
./deploy-frontend.sh -e staging -a consumer

# 部署至 Production（指定映像標籤）
./deploy-frontend.sh -e production -t v1.2.0

# 預覽部署指令（不實際執行）
./deploy-frontend.sh --dry-run

# 僅建置 Docker 映像，不推送不部署
./deploy-frontend.sh --skip-push --skip-deploy
```

腳本會自動處理：ECR 登入、Docker 多平台建置（linux/amd64）、映像推送、K8s manifest 佔位符替換、Argo Rollouts Canary 觸發。

詳細選項請參考 `./deploy-frontend.sh --help`。

### 前端 CDK 基礎設施

前端 ECR 儲存庫由 CDK `FrontendStack` 管理：

```bash
cd infrastructure
npx cdk deploy '*-FrontendStack' -c environment=development
```

此 Stack 建立兩個 ECR Repository（`genai-demo/consumer-frontend`、`genai-demo/cmc-frontend`），含映像掃描、生命週期策略、EKS 拉取權限。

### 前端 Docker 手動部署

前端應用使用多階段 Docker 建置，Dockerfile 位於 `frontend/` 目錄。

```bash
# 建置 CMC 管理後台映像（port 3002）
cd frontend
docker build -f Dockerfile.cmc -t cmc-frontend .

# 建置 Consumer 消費者應用映像（port 3000）
docker build -f Dockerfile.consumer -t consumer-frontend .
```

#### 帶環境變數建置

```bash
# 指定後端 API URL
docker build -f Dockerfile.cmc \
  --build-arg NEXT_PUBLIC_API_URL=https://api.example.com \
  -t cmc-frontend .

docker build -f Dockerfile.consumer \
  --build-arg NEXT_PUBLIC_API_URL=https://api.example.com \
  -t consumer-frontend .
```

### 使用 Docker Compose 一鍵啟動

從專案根目錄執行，同時啟動後端 + CMC + Consumer：

```bash
docker-compose up -d
```

服務端口：
- 後端 API: http://localhost:8080
- CMC 管理後台: http://localhost:3002
- Consumer 應用: http://localhost:3000

### 前端 Docker 映像說明

| 映像 | 基礎映像 | 建置工具 | 輸出 |
|------|----------|----------|------|
| `cmc-frontend` | node:20-alpine | pnpm 9.x + Turborepo | Next.js standalone |
| `consumer-frontend` | node:20-alpine | pnpm 9.x + Turborepo | Next.js standalone |

兩個映像皆使用 Next.js `standalone` 輸出模式，最終映像僅包含執行所需的最小檔案。

**注意**: EKS 部署需要先配置 AWS CLI 和 kubectl。

---

**最後更新**: 2026-03-17
