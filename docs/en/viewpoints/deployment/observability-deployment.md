
# Deployment

## 概述

本指南詳細說明如何在不同Environment中Deployment前端後端Observability整合系統，包括開發、測試和生產Environment的Deployment步驟和配置。

## Deployment

### 系統組件

```mermaid
graph TB
    subgraph "前端Deployment"
        A[Angular 應用]
        B[Observability SDK]
        C[Nginx 反向代理]
    end
    
    subgraph "後端Deployment"
        D[Spring Boot 應用]
        E[分析控制器]
        F[事件處理服務]
    end
    
    subgraph "基礎設施"
        G[Amazon MSK]
        H[RDS PostgreSQL]
        I[ElastiCache Redis]
        J[CloudWatch]
        K[X-Ray]
    end
    
    A --> C
    C --> D
    D --> E
    E --> F
    F --> G
    F --> H
    F --> I
    D --> J
    D --> K
```

## Environment準備

### Deployment

#### 1. 本地開發Environment設置

**前置條件**:

- Node.js 18+
- Java 21
- Docker & Docker Compose
- Git

**Deployment步驟**:

```bash
# 1. 克隆專案
git clone https://github.com/your-org/genai-demo.git
cd genai-demo

# 2. 啟動基礎設施服務
docker-compose up -d redis postgresql

# 3. 啟動後端服務
./gradlew bootRun --args='--spring.profiles.active=dev'

# 4. 啟動前端服務
cd consumer-frontend
npm install
npm run start

# Deployment
curl http://localhost:8080/actuator/health
curl http://localhost:4200
```

**Docker Compose 配置**:

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data

  postgresql:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: genai_demo_dev
      POSTGRES_USER: genai_demo
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: true

volumes:
  redis-data:
  postgres-data:
```

#### 2. 開發Environment配置驗證

```bash
#!/bin/bash
# verify-dev-deployment.sh

echo "=== 驗證開發EnvironmentDeployment ==="

# 檢查後端健康狀態
echo "1. 檢查後端服務..."
curl -f http://localhost:8080/actuator/health || exit 1

# 檢查前端服務
echo "2. 檢查前端服務..."
curl -f http://localhost:4200 || exit 1

# 檢查Repository連接
echo "3. 檢查Repository連接..."
curl -f http://localhost:8080/actuator/health/db || exit 1

# 檢查 Redis 連接
echo "4. 檢查 Redis 連接..."
curl -f http://localhost:8080/actuator/health/redis || exit 1

# Testing
echo "5. 測試Observability API..."
curl -X POST http://localhost:8080/../api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: dev-test-$(date +%s)" \
  -H "X-Session-Id: dev-session" \
  -d '[{"eventId":"dev-test","eventType":"page_view","sessionId":"dev-session","traceId":"dev-test-'$(date +%s)'","timestamp":'$(date +%s000)',"data":{"page":"/test"}}]' || exit 1

echo "✅ 開發EnvironmentDeployment驗證完成"
```

### Testing

#### 1. CI/CD Pipeline配置

**GitHub Actions 工作流程**:

```yaml
# .github/workflows/test-deployment.yml
name: Test Environment Deployment

on:
  pull_request:
    branches: [ main, develop ]
  push:
    branches: [ develop ]

jobs:
  test-deployment:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test_password
          POSTGRES_DB: genai_demo_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
      
      redis:
        image: redis:7
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 6379:6379

    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
    
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    
    - name: Run backend tests
      run: ./gradlew test --profile test
      env:
        SPRING_PROFILES_ACTIVE: test
        SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/genai_demo_test
        SPRING_DATASOURCE_USERNAME: postgres
        SPRING_DATASOURCE_PASSWORD: test_password
        SPRING_REDIS_HOST: localhost
        SPRING_REDIS_PORT: 6379
    
    - name: Build backend application
      run: ./gradlew build -x test
    
    - name: Install frontend dependencies
      run: |
        cd consumer-frontend
        npm ci
    
    - name: Run frontend tests
      run: |
        cd consumer-frontend
        npm run test:ci
    
    - name: Build frontend application
      run: |
        cd consumer-frontend
        npm run build:test
    
    - name: Run integration tests
      run: ./gradlew integrationTest
      env:
        SPRING_PROFILES_ACTIVE: test
    
    - name: Deploy to test environment
      if: github.ref == 'refs/heads/develop'
      run: |
        # Testing
        ./scripts/deploy-test.sh
```

#### Testing

**Kubernetes 配置**:

```yaml
# k8s/test/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: genai-demo-test
  labels:
    environment: test
    project: genai-demo

---
# k8s/test/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: genai-demo-config
  namespace: genai-demo-test
data:
  application.yml: |
    spring:
      profiles:
        active: test
      datasource:
        url: jdbc:postgresql://postgres-service:5432/genai_demo_test
        username: genai_demo
        password: ${DB_PASSWORD}
      redis:
        host: redis-service
        port: 6379
    genai-demo:
      events:
        publisher: in-memory
      observability:
        analytics:
          enabled: true
          batch-size: 10
          flush-interval: 10s

---
# k8s/test/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: genai-demo-backend
  namespace: genai-demo-test
spec:
  replicas: 2
  selector:
    matchLabels:
      app: genai-demo-backend
  template:
    metadata:
      labels:
        app: genai-demo-backend
    spec:
      containers:
      - name: backend
        image: genai-demo/backend:test
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "test"
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        volumeMounts:
        - name: config
          mountPath: /app/config
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
      volumes:
      - name: config
        configMap:
          name: genai-demo-config
```

### Deployment

#### Deployment

**CDK Deployment腳本**:

```bash
#!/bin/bash
# deploy-production.sh

set -e

ENVIRONMENT="production"
PROJECT_NAME="genai-demo"
AWS_REGION="us-east-1"

echo "=== Deployment生產Environment基礎設施 ==="

# Deployment
echo "1. Deployment VPC 和網路組件..."
cd infrastructure
npm install
npx cdk deploy NetworkStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# Deployment
echo "2. Deployment安全和 KMS..."
npx cdk deploy SecurityStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# Deployment
echo "3. Deployment MSK 叢集..."
npx cdk deploy MSKStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# Deployment
echo "4. Deployment RDS Repository..."
npx cdk deploy RDSStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# Deployment
echo "5. Deployment EKS 叢集..."
npx cdk deploy EKSStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# Deployment
echo "6. DeploymentObservability組件..."
npx cdk deploy ObservabilityStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

echo "✅ 基礎設施Deployment完成"

# 7. 創建 Kafka 主題
echo "7. 創建 Kafka 主題..."
./scripts/create-kafka-topics.sh $ENVIRONMENT

# Deployment
echo "8. Deployment應用程式..."
./scripts/deploy-application.sh $ENVIRONMENT

echo "✅ 生產EnvironmentDeployment完成"
```

## Deployment

### Deployment

```bash
#!/bin/bash
# scripts/verify-production-deployment.sh

ENVIRONMENT="production"
API_BASE_URL="https://api.genai-demo.com"
FRONTEND_URL="https://genai-demo.com"

echo "=== 驗證生產EnvironmentDeployment ==="

# 1. 檢查基礎設施健康狀態
echo "1. 檢查基礎設施..."

# 檢查 MSK 叢集
MSK_CLUSTER_ARN=$(aws cloudformation describe-stacks \
    --stack-name genai-demo-${ENVIRONMENT}-msk \
    --query 'Stacks[0].Outputs[?OutputKey==`MSKClusterArn`].OutputValue' \
    --output text)

MSK_STATUS=$(aws kafka describe-cluster --cluster-arn $MSK_CLUSTER_ARN \
    --query 'ClusterInfo.State' --output text)

if [ "$MSK_STATUS" != "ACTIVE" ]; then
    echo "❌ MSK 叢集狀態異常: $MSK_STATUS"
    exit 1
fi
echo "✅ MSK 叢集狀態正常"

# 2. 檢查應用程式健康狀態
echo "2. 檢查應用程式..."

# 檢查後端健康狀態
BACKEND_HEALTH=$(curl -s -w "%{http_code}" -o /dev/null ${API_BASE_URL}/actuator/health)
if [ "$BACKEND_HEALTH" != "200" ]; then
    echo "❌ 後端Health Check失敗: HTTP $BACKEND_HEALTH"
    exit 1
fi
echo "✅ 後端健康狀態正常"

# 3. 檢查Observability功能
echo "3. 檢查Observability功能..."

# Testing
TRACE_ID="prod-verify-$(date +%s)"
SESSION_ID="prod-verify-session"

ANALYTICS_RESPONSE=$(curl -s -w "%{http_code}" -X POST ${API_BASE_URL}/../api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: $TRACE_ID" \
  -H "X-Session-Id: $SESSION_ID" \
  -d '[{"eventId":"prod-verify","eventType":"page_view","sessionId":"'$SESSION_ID'","traceId":"'$TRACE_ID'","timestamp":'$(date +%s000)',"data":{"page":"/verify"}}]')

if [[ "${ANALYTICS_RESPONSE: -3}" != "200" ]]; then
    echo "❌ 分析 API 測試失敗: HTTP ${ANALYTICS_RESPONSE: -3}"
    exit 1
fi
echo "✅ 分析 API 功能正常"

echo "=== 生產EnvironmentDeployment驗證完成 ==="
```

## Monitoring和Alerting

### CloudWatch Dashboard設置

```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["GenAI/Demo/production", "observability.events.received"],
          [".", "observability.events.processed"],
          [".", "observability.events.failed"]
        ],
        "period": 300,
        "stat": "Sum",
        "region": "us-east-1",
        "title": "Observability事件處理"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", "genai-demo-production-alb"],
          [".", "TargetResponseTime", ".", "."],
          [".", "HTTPCode_Target_2XX_Count", ".", "."]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "應用程式負載均衡器Metrics"
      }
    }
  ]
}
```

## 回滾Policy

### 自動回滾觸發條件

```yaml
# k8s/production/rollback-policy.yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: genai-demo-backend-rollout
spec:
  replicas: 3
  strategy:
    canary:
      steps:
      - setWeight: 20
      - pause: {duration: 10m}
      - setWeight: 40
      - pause: {duration: 10m}
      - setWeight: 60
      - pause: {duration: 10m}
      - setWeight: 80
      - pause: {duration: 10m}
      analysis:
        templates:
        - templateName: success-rate
        args:
        - name: service-name
          value: genai-demo-backend-service
```

## Related Diagrams

### AWS 基礎設施架構
- **[AWS 基礎設施架構](../../diagrams/aws-infrastructure.md)** - 包含完整Observability服務的 AWS 架構
- **## AWS Observability架構

```mermaid
graph TB
    subgraph APP ["Spring Boot Application"]
        ACTUATOR[Spring Boot Actuator]
        OTEL[OpenTelemetry Agent]
        LOGBACK[Logback JSON Logging]
        MICROMETER[Micrometer Metrics]
    end
    
    subgraph K8S ["Kubernetes Cluster"]
        FLUENT[Fluent Bit DaemonSet]
        PROMETHEUS[Prometheus]
        GRAFANA[Grafana]
    end
    
    subgraph AWS ["AWS Services"]
        CW_LOGS[CloudWatch Logs]
        CW_METRICS[CloudWatch Metrics]
        XRAY[AWS X-Ray]
        OPENSEARCH[OpenSearch Service]
    end
    
    ACTUATOR --> PROMETHEUS
    LOGBACK --> FLUENT
    OTEL --> XRAY
    MICROMETER --> PROMETHEUS
    
    FLUENT --> CW_LOGS
    PROMETHEUS --> CW_METRICS
    GRAFANA --> PROMETHEUS
    
    CW_LOGS --> OPENSEARCH
    
    classDef application fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef kubernetes fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef aws fill:#fff3e0,stroke:#e65100,stroke-width:2px
    
    class ACTUATOR,OTEL,LOGBACK,MICROMETER application
    class FLUENT,PROMETHEUS,GRAFANA kubernetes
    class CW_LOGS,CW_METRICS,XRAY,OPENSEARCH aws
```** - Observability服務架構圖

### Deployment
- \1
- \1

## Relationships with Other Viewpoints

- **[Operational Viewpoint](../operational/README.md)**: Monitoring和維護Policy
- **[Development Viewpoint](../development/README.md)**: CI/CD 整合和測試
- **[Concurrency Viewpoint](../concurrency/README.md)**: 事件處理和非同步架構

## 相關文檔

- [生產Deployment檢查清單](production-deployment-checklist.md)
- [Infrastructure as Code](infrastructure-as-code.md)
- [Docker Deployment指南](docker-guide.md)