# Deployment Process

## Overview

This document provides step-by-step deployment procedures for the Enterprise E-Commerce Platform across different environments.

## Deployment Environments

- **Local**: Developer workstation
- **Staging**: Pre-production testing environment
- **Production**: Live customer-facing environment

## Prerequisites

### Required Tools

- AWS CLI v2.x
- kubectl v1.28+
- Docker v24.x+
- Gradle 8.x
- Node.js 20.x+ (for frontend deployments)
- pnpm 9.x (frontend monorepo package manager)

### Required Access

- AWS IAM credentials with appropriate permissions
- EKS cluster access (kubeconfig)
- Container registry access (ECR)
- Secrets Manager access

## Deployment to Staging

### Step 1: Pre-Deployment Checks

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Verify kubectl access
kubectl cluster-info

# Check current staging status
kubectl get pods -n staging
kubectl get services -n staging
```

### Step 2: Build Application

```bash
# Build backend application
cd app
./gradlew clean build

# Run tests
./gradlew test

# Build Docker image
docker build -t ecommerce-backend:${VERSION} .

# Tag for ECR
docker tag ecommerce-backend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### Step 3: Push to Container Registry

```bash
# Login to ECR
aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# Push image
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### Step 4: Update Kubernetes Manifests

```bash
# Update deployment manifest with new image version
cd infrastructure/k8s/overlays/staging

# Edit kustomization.yaml to update image tag
kustomize edit set image \
  ecommerce-backend=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### Step 5: Apply Database Migrations

```bash
# Run Flyway migrations
kubectl exec -it deployment/ecommerce-backend -n staging -- \
  ./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}

# Verify migration status
kubectl exec -it deployment/ecommerce-backend -n staging -- \
  ./gradlew flywayInfo
```

### Step 6: Deploy to Staging

```bash
# Apply Kubernetes manifests
kubectl apply -k infrastructure/k8s/overlays/staging

# Watch deployment progress
kubectl rollout status deployment/ecommerce-backend -n staging

# Verify pods are running
kubectl get pods -n staging -l app=ecommerce-backend
```

### Step 7: Verify Deployment

```bash
# Check pod logs
kubectl logs -f deployment/ecommerce-backend -n staging

# Check service endpoints
kubectl get svc -n staging

# Run smoke tests
./scripts/run-smoke-tests.sh staging

# Verify health endpoints
curl https://staging.ecommerce.example.com/actuator/health
curl https://staging.ecommerce.example.com/actuator/info
```

### Step 8: Post-Deployment Validation

- [ ] All pods are in Running state
- [ ] Health checks are passing
- [ ] Smoke tests pass
- [ ] No error logs in CloudWatch
- [ ] Metrics are being reported
- [ ] Database connections are healthy

## Deployment to Production

### Step 1: Pre-Deployment Checks

```bash
# Verify staging deployment is stable
./scripts/verify-staging-health.sh

# Check production status
kubectl get pods -n production
kubectl get services -n production

# Verify no ongoing incidents
# Check monitoring dashboards
# Review recent error rates
```

### Step 2: Create Deployment Plan

Document the following:

- Deployment window (date and time)
- Expected downtime (if any)
- Rollback plan
- Communication plan
- On-call engineers

### Step 3: Notify Stakeholders

```bash
# Send deployment notification
# - Engineering team
# - Product team
# - Customer support
# - Management

# Example notification:
# Subject: Production Deployment - [DATE] [TIME]
# - Version: ${VERSION}
# - Changes: [CHANGELOG_URL]
# - Expected duration: 30 minutes
# - Rollback plan: Available
```

### Step 4: Enable Maintenance Mode (if needed)

```bash
# For zero-downtime deployments, skip this step
# For deployments requiring downtime:

kubectl apply -f infrastructure/k8s/maintenance-mode.yaml
```

### Step 5: Build and Push Production Image

```bash
# Build production image
docker build -t ecommerce-backend:${VERSION} \
  --build-arg ENV=production .

# Tag for production ECR
docker tag ecommerce-backend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}-prod

# Push to production registry
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}-prod
```

### Step 6: Apply Database Migrations

```bash
# Backup database before migration
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod \
  --db-snapshot-identifier ecommerce-prod-pre-${VERSION}

# Run migrations
kubectl exec -it deployment/ecommerce-backend -n production -- \
  ./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://${PROD_DB_HOST}:5432/${PROD_DB_NAME}

# Verify migration
kubectl exec -it deployment/ecommerce-backend -n production -- \
  ./gradlew flywayInfo
```

### Step 7: Deploy Using Canary Strategy

```bash
# Deploy canary (10% traffic)
kubectl apply -f infrastructure/k8s/overlays/production/canary.yaml

# Monitor canary metrics for 15 minutes
# - Error rate
# - Response time
# - CPU/Memory usage
# - Business metrics (orders, payments)

# If canary is healthy, proceed to full deployment
kubectl apply -k infrastructure/k8s/overlays/production

# Watch rollout
kubectl rollout status deployment/ecommerce-backend -n production
```

### Step 8: Verify Production Deployment

```bash
# Check all pods are running
kubectl get pods -n production -l app=ecommerce-backend

# Verify health endpoints
curl https://api.ecommerce.example.com/actuator/health

# Run production smoke tests
./scripts/run-smoke-tests.sh production

# Monitor key metrics
# - API response times
# - Error rates
# - Database connections
# - Cache hit rates
```

### Step 9: Monitor Post-Deployment

Monitor for 1 hour after deployment:

- [ ] Error rates are normal
- [ ] Response times are within SLA
- [ ] No increase in customer support tickets
- [ ] Business metrics are normal (orders, payments)
- [ ] No alerts triggered

### Step 10: Disable Maintenance Mode

```bash
# If maintenance mode was enabled
kubectl delete -f infrastructure/k8s/maintenance-mode.yaml
```

### Step 11: Post-Deployment Communication

```bash
# Send deployment completion notification
# Subject: Production Deployment Complete - [VERSION]
# - Deployment status: Success
# - All systems operational
# - Monitoring continues for 24 hours
```

## Frontend Deployment

### 前端一鍵部署腳本

推薦使用專案根目錄的 `deploy-frontend.sh` 腳本，整合建置、推送、部署全流程：

```bash
# 部署兩個前端至開發環境
./deploy-frontend.sh -e development

# 部署至 Staging
./deploy-frontend.sh -e staging

# 僅部署 CMC 至 Production（指定映像標籤）
./deploy-frontend.sh -e production -a cmc -t v1.2.0

# 預覽部署指令
./deploy-frontend.sh -e production --dry-run
```

腳本自動處理：AWS Account ID 偵測、ECR 登入、Docker 多平台建置（linux/amd64）、映像推送、K8s manifest 佔位符替換、Argo Rollouts Canary 觸發。

#### 前置條件

1. CDK `FrontendStack` 已部署（ECR 儲存庫已建立）：
   ```bash
   cd infrastructure
   npx cdk deploy '*-FrontendStack' -c environment=development
   ```
2. AWS CLI 已設定正確認證
3. kubectl 已連線至目標 EKS 叢集
4. Docker 已安裝並執行

### 前端 Monorepo 手動建置與部署

前端已遷移至 Turborepo + pnpm Monorepo 架構（`frontend/` 目錄），兩個應用皆使用 Next.js + React 19，透過 Docker 多階段建置部署至 EKS。

#### 建置 Docker 映像

```bash
# 進入前端 Monorepo 目錄
cd frontend

# 建置 CMC 管理後台映像（Next.js 16 + React 19，port 3002）
docker build -f Dockerfile.cmc -t genai-demo/cmc-frontend:${VERSION} .

# 建置 Consumer 消費者應用映像（Next.js 15 + React 19，port 3000）
docker build -f Dockerfile.consumer -t genai-demo/consumer-frontend:${VERSION} .
```

#### 推送至 ECR

```bash
# 標記映像
docker tag genai-demo/cmc-frontend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/genai-demo/cmc-frontend:${VERSION}

docker tag genai-demo/consumer-frontend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/genai-demo/consumer-frontend:${VERSION}

# 推送映像
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/genai-demo/cmc-frontend:${VERSION}
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/genai-demo/consumer-frontend:${VERSION}
```

#### 部署至 EKS（Canary 策略）

```bash
# 更新 CMC 前端 Rollout 映像（port 3002）
kubectl argo rollouts set image genai-demo-cmc-frontend \
  genai-demo-cmc-frontend=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/genai-demo/cmc-frontend:${VERSION} \
  -n genai-demo

# 更新 Consumer 前端 Rollout 映像（port 3000）
kubectl argo rollouts set image genai-demo-consumer-frontend \
  genai-demo-consumer-frontend=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/genai-demo/consumer-frontend:${VERSION} \
  -n genai-demo

# 監控 Canary 部署進度
kubectl argo rollouts get rollout genai-demo-cmc-frontend -n genai-demo --watch
kubectl argo rollouts get rollout genai-demo-consumer-frontend -n genai-demo --watch
```

#### 驗證前端部署

```bash
# 檢查 Pod 狀態
kubectl get pods -n genai-demo -l app=genai-demo-cmc-frontend
kubectl get pods -n genai-demo -l app=genai-demo-consumer-frontend

# 驗證健康檢查端點
# TODO: kimkao.io 域名已停用，部署後替換為實際 LoadBalancer URL
curl https://<your-cmc-domain>/api/health
curl https://<your-consumer-domain>/api/health

# 檢查 Pod 日誌
kubectl logs -f deployment/genai-demo-cmc-frontend -n genai-demo
kubectl logs -f deployment/genai-demo-consumer-frontend -n genai-demo
```

#### 前端環境變數

| 變數 | CMC (port 3002) | Consumer (port 3000) |
|------|-----------------|---------------------|
| `NODE_ENV` | production | production |
| `PORT` | 3002 | 3000 |
| `NEXT_PUBLIC_API_URL` | http://localhost:8080 | http://localhost:8080 |
| `NEXT_PUBLIC_APP_ENV` | production | production |

## Deployment Checklist

### Pre-Deployment

- [ ] All tests passing in CI/CD
- [ ] Code review completed
- [ ] Security scan passed
- [ ] Performance tests passed
- [ ] Staging deployment successful
- [ ] Deployment plan documented
- [ ] Rollback plan prepared
- [ ] Stakeholders notified

### During Deployment

- [ ] Database backup created
- [ ] Migrations applied successfully
- [ ] Application deployed
- [ ] Health checks passing
- [ ] Smoke tests passing
- [ ] Monitoring active

### Post-Deployment

- [ ] All services healthy
- [ ] Metrics within normal range
- [ ] No critical errors
- [ ] Customer-facing features working
- [ ] Deployment documented
- [ ] Stakeholders notified

## Deployment Automation

### CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    tags:

      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:

      - name: Checkout code

        uses: actions/checkout@v3

      - name: Build application

        run: ./gradlew build

      - name: Build Docker image

        run: docker build -t ecommerce-backend:${GITHUB_REF_NAME} .

      - name: Push to ECR

        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin
          docker push ${ECR_REGISTRY}/ecommerce-backend:${GITHUB_REF_NAME}

      - name: Deploy to EKS

        run: |
          kubectl set image deployment/ecommerce-backend \
            ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${GITHUB_REF_NAME}
```

## Troubleshooting

### Deployment Fails

```bash
# Check pod status
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}

# Check logs
kubectl logs ${POD_NAME} -n ${NAMESPACE}

# Check events
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
```

### Rollback Required

See [Rollback Procedures](rollback.md) for detailed rollback steps.


**Last Updated**: 2026-02-21
**Owner**: DevOps Team
**Review Cycle**: Quarterly
