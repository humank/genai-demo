# 部署指南 (Deployment Guide)

## 🚀 快速開始

### 前置需求

- **Java 21+** (後端)
- **Node.js 18+** (前端和基礎設施)
- **AWS CLI** (雲端部署)
- **Docker** (可選，用於本地開發)

### 本地開發環境

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

### 雲端部署

#### 開發環境部署

```bash
# 基礎設施部署（不含 Analytics）
npm run deploy:dev

# 或手動部署
cd infrastructure
./deploy-consolidated.sh development us-east-1 false
```

#### 生產環境部署

```bash
# 完整功能部署（含 Analytics）
npm run deploy:prod

# 或手動部署
cd infrastructure
./deploy-consolidated.sh production us-east-1 true
```

## 🏗️ 架構概覽

### 後端服務 (Spring Boot)

- **端口**: 8080
- **健康檢查**: <http://localhost:8080/actuator/health>
- **API 文檔**: <http://localhost:8080/swagger-ui.html>

### 前端應用

- **消費者前端**: <http://localhost:4200> (Angular)
- **管理前端**: <http://localhost:3000> (Next.js)

### 基礎設施 (AWS CDK)

- **網路層**: VPC, 子網路, 安全群組
- **安全層**: KMS 金鑰, IAM 角色
- **核心層**: 負載平衡器, 運算資源
- **監控層**: CloudWatch, 警報
- **分析層**: 資料湖, Kinesis, QuickSight (可選)

## 🔧 配置說明

### 環境變數

```bash
# 開發環境
SPRING_PROFILES_ACTIVE=development
DATABASE_URL=jdbc:h2:mem:testdb

# 生產環境
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=jdbc:postgresql://...
AWS_REGION=us-east-1
```

### 功能開關

```yaml
# application.yml
observability:
  analytics:
    enabled: false  # 開發環境預設關閉
  websocket:
    enabled: false  # 計劃中功能
  kafka:
    enabled: false  # 開發環境預設關閉
```

## 📊 監控和日誌

### 可用的監控端點

- `/actuator/health` - 應用健康狀態
- `/actuator/metrics` - 應用指標
- `/actuator/info` - 應用資訊
- `/actuator/prometheus` - Prometheus 指標

### 日誌位置

- **應用日誌**: `logs/application.log`
- **存取日誌**: `logs/access.log`
- **錯誤日誌**: `logs/error.log`

## 🧪 測試

### 後端測試

```bash
cd app

# 單元測試 (快速)
./gradlew unitTest

# 整合測試
./gradlew integrationTest

# 完整測試套件
./gradlew test
```

### 基礎設施測試

```bash
cd infrastructure

# CDK 測試
npm test

# 特定測試
npm run test:unit
npm run test:integration
```

## 🔍 故障排除

### 常見問題

1. **後端啟動失敗**
   - 檢查 Java 版本 (需要 21+)
   - 檢查端口 8080 是否被佔用
   - 檢查資料庫連線

2. **前端編譯錯誤**
   - 清除 node_modules: `rm -rf node_modules && npm install`
   - 檢查 Node.js 版本 (需要 18+)

3. **CDK 部署失敗**
   - 檢查 AWS 認證: `aws sts get-caller-identity`
   - 檢查 CDK 版本: `cdk --version`
   - 檢查區域設定

### 日誌檢查

```bash
# 檢查應用日誌
tail -f logs/application.log

# 檢查 Docker 容器日誌 (如果使用)
docker logs genai-demo-app

# 檢查 AWS CloudWatch 日誌
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/genai-demo
```

## 📞 支援資源

- **專案文檔**: [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)
- **重構總結**: [reports-summaries/project-management/REFACTORING_SUMMARY.md](reports-summaries/project-management/REFACTORING_SUMMARY.md)
- **基礎設施指南**: [infrastructure/CONSOLIDATED_DEPLOYMENT.md](infrastructure/CONSOLIDATED_DEPLOYMENT.md)
- **故障排除**: [docs/troubleshooting/](docs/troubleshooting/)

---

**最後更新**: 2024年12月  
**維護者**: Development Team
