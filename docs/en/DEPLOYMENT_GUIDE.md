
# Deployment

## 🚀 快速開始

### Requirements

- **Java 21+** (後端)
- **Node.js 18+** (前端和基礎設施)
- **AWS CLI** (雲端Deployment)
- **Docker** (可選，用於本地開發)

### 本地開發Environment

```bash
# 1. 啟動後端 (Spring Boot)
cd app
./gradlew bootRun

# 2. 啟動消費者前端 (Angular)
cd consumer-frontend
npm install
npm start

# 3. 啟動管理前端 (Next.js)
cd cmc-frontend
npm install
npm run dev
```

### Deployment

#### Deployment

```bash
# Deployment
npm run deploy:dev

# Deployment
cd infrastructure
./deploy-consolidated.sh development us-east-1 false
```

#### Deployment

```bash
# Deployment
npm run deploy:prod

# Deployment
cd infrastructure
./deploy-consolidated.sh production us-east-1 true
```

## Overview

### 後端服務 (Spring Boot)

- **Port**: 8080
- **Health Check**: <http://localhost:8080/actuator/health>
- **API 文檔**: <http://localhost:8080/swagger-ui.html>

### 前端應用

- **消費者前端**: <http://localhost:4200> (Angular)
- **管理前端**: <http://localhost:3000> (Next.js)

### 基礎設施 (AWS CDK)

- **網路層**: VPC, 子網路, 安全群組
- **安全層**: KMS 金鑰, IAM 角色
- **核心層**: Load Balancer, 運算Resource
- **Monitoring層**: CloudWatch, Alerting
- **分析層**: 資料湖, Kinesis, QuickSight (可選)

## 🔧 配置說明

### Environment變數

```bash
# 開發Environment
SPRING_PROFILES_ACTIVE=development
DATABASE_URL=jdbc:h2:mem:testdb

# 生產Environment
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=jdbc:postgresql://...
AWS_REGION=us-east-1
```

### 功能開關

```yaml
# application.yml
observability:
  analytics:
    enabled: false  # 開發Environment預設關閉
  websocket:
    enabled: false  # 計劃中功能
  kafka:
    enabled: false  # 開發Environment預設關閉
```

## 📊 Monitoring和Logging

### 可用的Monitoring端點

- `/actuator/health` - 應用健康狀態
- `/actuator/metrics` - 應用Metrics
- `/actuator/info` - 應用資訊
- `/actuator/prometheus` - Prometheus Metrics

### Logging位置

- **應用Logging**: `logs/application.log`
- **存取Logging**: `logs/access.log`
- **錯誤Logging**: `logs/error.log`

## Testing

### Testing

```bash
cd app

# Testing
./gradlew unitTest

# Testing
./gradlew integrationTest

# Testing
./gradlew test
```

### Testing

```bash
cd infrastructure

# Testing
npm test

# Testing
npm run test:unit
npm run test:integration
```

## Troubleshooting

### 常見問題

1. **後端啟動失敗**
   - 檢查 Java 版本 (需要 21+)
   - 檢查Port 8080 是否被佔用
   - 檢查Repository連線

2. **前端編譯錯誤**
   - 清除 node_modules: `rm -rf node_modules && npm install`
   - 檢查 Node.js 版本 (需要 18+)

3. **CDK Deployment失敗**
   - 檢查 AWS 認證: `aws sts get-caller-identity`
   - 檢查 CDK 版本: `cdk --version`
   - 檢查區域設定

### Logging檢查

```bash
# 檢查應用Logging
tail -f logs/application.log

# 檢查 Docker 容器Logging (如果使用)
docker logs genai-demo-app

# 檢查 AWS CloudWatch Logging
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/genai-demo
```

## Resources

- **專案文檔**: [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)
- **Refactoringsummary**: [reports-summaries/project-management/REFACTORING_SUMMARY.md](reports-summaries/project-management/reports-summaries/project-management/REFACTORING_SUMMARY.md)
- **基礎設施指南**: [infrastructure/CONSOLIDATED_DEPLOYMENT.md](infrastructure/CONSOLIDATED_DEPLOYMENT.md)
- **故障排除**: [docs/troubleshooting/](..troubleshooting/)

---

**最後更新**: 2024年12月  
**維護者**: Development Team
