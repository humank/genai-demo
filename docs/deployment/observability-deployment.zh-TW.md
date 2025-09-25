# 可觀測性部署指南

## 概述

本指南詳細說明如何在不同環境中部署前端後端可觀測性整合系統，包括開發、測試和生產環境的部署步驟和配置。

## 部署架構

### 系統組件

```mermaid
graph TB
    subgraph "前端部署"
        A[Angular 應用]
        B[可觀測性 SDK]
        C[Nginx 反向代理]
    end
    
    subgraph "後端部署"
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

## 環境準備

### 開發環境部署

#### 1. 本地開發環境設置

**前置條件**:

- Node.js 18+
- Java 21
- Docker & Docker Compose
- Git

**部署步驟**:

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

# 5. 驗證部署
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

#### 2. 開發環境配置驗證

```bash
#!/bin/bash
# verify-dev-deployment.sh

echo "=== 驗證開發環境部署 ==="

# 檢查後端健康狀態
echo "1. 檢查後端服務..."
curl -f http://localhost:8080/actuator/health || exit 1

# 檢查前端服務
echo "2. 檢查前端服務..."
curl -f http://localhost:4200 || exit 1

# 檢查資料庫連接
echo "3. 檢查資料庫連接..."
curl -f http://localhost:8080/actuator/health/db || exit 1

# 檢查 Redis 連接
echo "4. 檢查 Redis 連接..."
curl -f http://localhost:8080/actuator/health/redis || exit 1

# 測試可觀測性 API
echo "5. 測試可觀測性 API..."
curl -X POST http://localhost:8080/../api/analytics/events \
  -H "Content-Type: application/json" \
  -H "X-Trace-Id: dev-test-$(date +%s)" \
  -H "X-Session-Id: dev-session" \
  -d '[{"eventId":"dev-test","eventType":"page_view","sessionId":"dev-session","traceId":"dev-test-'$(date +%s)'","timestamp":'$(date +%s000)',"data":{"page":"/test"}}]' || exit 1

echo "✅ 開發環境部署驗證完成"
```

### 測試環境部署

#### 1. CI/CD 管道配置

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
        # 部署到測試環境的腳本
        ./scripts/deploy-test.sh
```

#### 2. 測試環境基礎設施

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

### 生產環境部署

#### 1. AWS 基礎設施部署

**CDK 部署腳本**:

```bash
#!/bin/bash
# deploy-production.sh

set -e

ENVIRONMENT="production"
PROJECT_NAME="genai-demo"
AWS_REGION="us-east-1"

echo "=== 部署生產環境基礎設施 ==="

# 1. 部署網路基礎設施
echo "1. 部署 VPC 和網路組件..."
cd infrastructure
npm install
npx cdk deploy NetworkStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# 2. 部署安全組件
echo "2. 部署安全和 KMS..."
npx cdk deploy SecurityStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# 3. 部署 MSK 叢集
echo "3. 部署 MSK 叢集..."
npx cdk deploy MSKStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# 4. 部署 RDS 資料庫
echo "4. 部署 RDS 資料庫..."
npx cdk deploy RDSStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# 5. 部署 EKS 叢集
echo "5. 部署 EKS 叢集..."
npx cdk deploy EKSStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

# 6. 部署可觀測性基礎設施
echo "6. 部署可觀測性組件..."
npx cdk deploy ObservabilityStack --require-approval never \
  --context environment=$ENVIRONMENT \
  --context projectName=$PROJECT_NAME

echo "✅ 基礎設施部署完成"

# 7. 創建 Kafka 主題
echo "7. 創建 Kafka 主題..."
./scripts/create-kafka-topics.sh $ENVIRONMENT

# 8. 部署應用程式
echo "8. 部署應用程式..."
./scripts/deploy-application.sh $ENVIRONMENT

echo "✅ 生產環境部署完成"
```

#### 2. Kafka 主題創建

```bash
#!/bin/bash
# scripts/create-kafka-topics.sh

ENVIRONMENT=$1
PROJECT_NAME="genai-demo"

if [ -z "$ENVIRONMENT" ]; then
    echo "Usage: $0 <environment>"
    exit 1
fi

# 獲取 MSK 叢集資訊
MSK_CLUSTER_ARN=$(aws cloudformation describe-stacks \
    --stack-name ${PROJECT_NAME}-${ENVIRONMENT}-msk \
    --query 'Stacks[0].Outputs[?OutputKey==`MSKClusterArn`].OutputValue' \
    --output text)

MSK_BOOTSTRAP_SERVERS=$(aws kafka describe-cluster \
    --cluster-arn $MSK_CLUSTER_ARN \
    --query 'ClusterInfo.BrokerNodeGroupInfo.ClientSubnets[0]' \
    --output text)

echo "創建可觀測性 Kafka 主題..."

# 可觀測性主題列表
TOPICS=(
    "${PROJECT_NAME}.${ENVIRONMENT}.observability.user.behavior"
    "${PROJECT_NAME}.${ENVIRONMENT}.observability.performance.metrics"
    "${PROJECT_NAME}.${ENVIRONMENT}.observability.business.analytics"
    "${PROJECT_NAME}.${ENVIRONMENT}.observability.user.behavior.dlq"
    "${PROJECT_NAME}.${ENVIRONMENT}.observability.performance.metrics.dlq"
    "${PROJECT_NAME}.${ENVIRONMENT}.observability.business.analytics.dlq"
)

for topic in "${TOPICS[@]}"; do
    echo "創建主題: $topic"
    
    # 根據主題類型設定不同的分區數和保留期
    if [[ $topic == *"performance"* ]]; then
        PARTITIONS=3
        RETENTION_MS=1209600000  # 14 天
    elif [[ $topic == *"dlq"* ]]; then
        PARTITIONS=1
        RETENTION_MS=2592000000  # 30 天
    else
        PARTITIONS=6
        RETENTION_MS=7776000000  # 90 天
    fi
    
    aws kafka create-configuration \
        --name "${topic}-config" \
        --kafka-versions "2.8.1" \
        --server-properties "
            auto.create.topics.enable=false
            default.replication.factor=3
            min.insync.replicas=2
            log.retention.ms=${RETENTION_MS}
            compression.type=gzip
        " || true
    
    # 注意：MSK 不支援直接創建主題，需要通過應用程式創建
    echo "主題 $topic 將在應用程式首次連接時自動創建"
done

echo "✅ Kafka 主題配置完成"
```

#### 3. 應用程式部署

**Kubernetes 生產配置**:

```yaml
# k8s/production/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: genai-demo-backend
  namespace: genai-demo-production
  labels:
    app: genai-demo-backend
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: genai-demo-backend
  template:
    metadata:
      labels:
        app: genai-demo-backend
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: genai-demo-service-account
      containers:
      - name: backend
        image: genai-demo/backend:latest
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "msk"
        - name: MSK_BOOTSTRAP_SERVERS
          valueFrom:
            secretKeyRef:
              name: msk-config
              key: bootstrap-servers
        - name: DB_HOST
          valueFrom:
            secretKeyRef:
              name: rds-config
              key: host
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rds-config
              key: password
        - name: REDIS_HOST
          valueFrom:
            configMapKeyRef:
              name: redis-config
              key: host
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
          timeoutSeconds: 10
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        volumeMounts:
        - name: config
          mountPath: /app/config
        - name: logs
          mountPath: /app/logs
      volumes:
      - name: config
        configMap:
          name: genai-demo-config
      - name: logs
        emptyDir: {}

---
apiVersion: v1
kind: Service
metadata:
  name: genai-demo-backend-service
  namespace: genai-demo-production
  labels:
    app: genai-demo-backend
spec:
  selector:
    app: genai-demo-backend
  ports:
  - port: 80
    targetPort: 8080
    name: http
  type: ClusterIP

---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: genai-demo-ingress
  namespace: genai-demo-production
  annotations:
    kubernetes.io/ingress.class: "alb"
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/ssl-redirect: '443'
    alb.ingress.kubernetes.io/certificate-arn: ${SSL_CERTIFICATE_ARN}
spec:
  rules:
  - host: api.genai-demo.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: genai-demo-backend-service
            port:
              number: 80
```

#### 4. 前端部署配置

**Nginx 配置**:

```nginx
# nginx.conf
upstream backend {
    server genai-demo-backend-service:80;
}

server {
    listen 80;
    server_name genai-demo.com www.genai-demo.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name genai-demo.com www.genai-demo.com;

    ssl_certificate /etc/ssl/certs/genai-demo.crt;
    ssl_certificate_key /etc/ssl/private/genai-demo.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    # 前端靜態檔案
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
        
        # 快取設定
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    # API 代理
    location /../api/ {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 可觀測性標頭
        proxy_set_header X-Request-ID $request_id;
        proxy_set_header X-Forwarded-Request-ID $request_id;
    }

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 健康檢查
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
```

## 部署驗證

### 自動化部署驗證

```bash
#!/bin/bash
# scripts/verify-production-deployment.sh

ENVIRONMENT="production"
API_BASE_URL="https://api.genai-demo.com"
FRONTEND_URL="https://genai-demo.com"

echo "=== 驗證生產環境部署 ==="

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

# 檢查 RDS 資料庫
RDS_INSTANCE_ID="genai-demo-${ENVIRONMENT}-db"
RDS_STATUS=$(aws rds describe-db-instances \
    --db-instance-identifier $RDS_INSTANCE_ID \
    --query 'DBInstances[0].DBInstanceStatus' --output text)

if [ "$RDS_STATUS" != "available" ]; then
    echo "❌ RDS 資料庫狀態異常: $RDS_STATUS"
    exit 1
fi
echo "✅ RDS 資料庫狀態正常"

# 2. 檢查應用程式健康狀態
echo "2. 檢查應用程式..."

# 檢查後端健康狀態
BACKEND_HEALTH=$(curl -s -w "%{http_code}" -o /dev/null ${API_BASE_URL}/actuator/health)
if [ "$BACKEND_HEALTH" != "200" ]; then
    echo "❌ 後端健康檢查失敗: HTTP $BACKEND_HEALTH"
    exit 1
fi
echo "✅ 後端健康狀態正常"

# 檢查前端可訪問性
FRONTEND_STATUS=$(curl -s -w "%{http_code}" -o /dev/null $FRONTEND_URL)
if [ "$FRONTEND_STATUS" != "200" ]; then
    echo "❌ 前端訊問失敗: HTTP $FRONTEND_STATUS"
    exit 1
fi
echo "✅ 前端可正常訪問"

# 3. 檢查可觀測性功能
echo "3. 檢查可觀測性功能..."

# 測試分析 API
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

# 檢查指標端點
METRICS_RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null ${API_BASE_URL}/actuator/metrics)
if [ "$METRICS_RESPONSE" != "200" ]; then
    echo "❌ 指標端點異常: HTTP $METRICS_RESPONSE"
    exit 1
fi
echo "✅ 指標端點正常"

# 4. 檢查監控和警報
echo "4. 檢查監控配置..."

# 檢查 CloudWatch 指標
NAMESPACE="GenAI/Demo/production"
METRIC_COUNT=$(aws cloudwatch list-metrics --namespace $NAMESPACE \
    --query 'length(Metrics)' --output text)

if [ "$METRIC_COUNT" -lt "10" ]; then
    echo "⚠️  CloudWatch 指標數量較少: $METRIC_COUNT"
else
    echo "✅ CloudWatch 指標正常: $METRIC_COUNT 個指標"
fi

# 檢查警報配置
ALARM_COUNT=$(aws cloudwatch describe-alarms \
    --alarm-name-prefix "genai-demo-${ENVIRONMENT}" \
    --query 'length(MetricAlarms)' --output text)

if [ "$ALARM_COUNT" -lt "5" ]; then
    echo "⚠️  CloudWatch 警報數量較少: $ALARM_COUNT"
else
    echo "✅ CloudWatch 警報配置正常: $ALARM_COUNT 個警報"
fi

# 5. 效能基準測試
echo "5. 執行效能基準測試..."

# 使用 Apache Bench 進行簡單的負載測試
ab -n 100 -c 10 ${API_BASE_URL}/actuator/health > /tmp/ab_results.txt 2>&1

REQUESTS_PER_SECOND=$(grep "Requests per second" /tmp/ab_results.txt | awk '{print $4}')
MEAN_TIME=$(grep "Time per request" /tmp/ab_results.txt | head -1 | awk '{print $4}')

echo "效能測試結果:"
echo "  - 每秒請求數: $REQUESTS_PER_SECOND"
echo "  - 平均響應時間: ${MEAN_TIME}ms"

if (( $(echo "$REQUESTS_PER_SECOND < 50" | bc -l) )); then
    echo "⚠️  效能可能需要優化"
else
    echo "✅ 效能表現良好"
fi

echo "=== 生產環境部署驗證完成 ==="
```

### 監控儀表板設置

**CloudWatch 儀表板配置**:

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
        "title": "可觀測性事件處理"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", "genai-demo-production-alb"],
          [".", "TargetResponseTime", ".", "."],
          [".", "HTTPCode_Target_2XX_Count", ".", "."],
          [".", "HTTPCode_Target_4XX_Count", ".", "."],
          [".", "HTTPCode_Target_5XX_Count", ".", "."]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "應用程式負載均衡器指標"
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/MSK", "BytesInPerSec", "Cluster Name", "genai-demo-production-msk"],
          [".", "BytesOutPerSec", ".", "."],
          [".", "MessagesInPerSec", ".", "."]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "MSK 叢集指標"
      }
    }
  ]
}
```

## 回滾策略

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
      scaleDownDelaySeconds: 30
      abortScaleDownDelaySeconds: 30
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
        image: genai-demo/backend:latest
```

### 手動回滾程序

```bash
#!/bin/bash
# scripts/rollback-production.sh

ENVIRONMENT="production"
ROLLBACK_VERSION=$1

if [ -z "$ROLLBACK_VERSION" ]; then
    echo "Usage: $0 <rollback-version>"
    echo "Available versions:"
    kubectl get deployments genai-demo-backend -n genai-demo-production -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}'
    exit 1
fi

echo "=== 執行生產環境回滾 ==="

# 1. 確認回滾版本
echo "1. 確認回滾到版本: $ROLLBACK_VERSION"
read -p "確定要繼續嗎? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "回滾已取消"
    exit 1
fi

# 2. 執行回滾
echo "2. 執行 Kubernetes 部署回滾..."
kubectl rollout undo deployment/genai-demo-backend \
    --namespace=genai-demo-production \
    --to-revision=$ROLLBACK_VERSION

# 3. 等待回滾完成
echo "3. 等待回滾完成..."
kubectl rollout status deployment/genai-demo-backend \
    --namespace=genai-demo-production \
    --timeout=600s

# 4. 驗證回滾結果
echo "4. 驗證回滾結果..."
sleep 30

HEALTH_STATUS=$(curl -s -w "%{http_code}" -o /dev/null https://api.genai-demo.com/actuator/health)
if [ "$HEALTH_STATUS" != "200" ]; then
    echo "❌ 回滾後健康檢查失敗: HTTP $HEALTH_STATUS"
    echo "請檢查應用程式日誌並考慮進一步的回滾操作"
    exit 1
fi

echo "✅ 回滾完成，應用程式健康狀態正常"

# 5. 通知相關人員
echo "5. 發送回滾通知..."
# 這裡可以添加 Slack 或 Email 通知邏輯

echo "=== 生產環境回滾完成 ==="
```

## 安全考量

### 部署安全檢查清單

- [ ] **網路安全**
  - [ ] VPC 和子網路正確配置
  - [ ] 安全群組規則最小權限原則
  - [ ] NAT 閘道器和路由表配置

- [ ] **身份和存取管理**
  - [ ] IAM 角色和政策最小權限
  - [ ] 服務帳戶正確配置
  - [ ] 密鑰管理服務 (KMS) 啟用

- [ ] **數據加密**
  - [ ] 傳輸中加密 (TLS 1.2+)
  - [ ] 靜態數據加密
  - [ ] 密鑰輪換政策

- [ ] **監控和稽核**
  - [ ] CloudTrail 日誌啟用
  - [ ] VPC Flow Logs 啟用
  - [ ] 安全事件監控配置

### 合規性檢查

```bash
#!/bin/bash
# scripts/compliance-check.sh

echo "=== 合規性檢查 ==="

# 檢查加密配置
echo "1. 檢查加密配置..."

# 檢查 RDS 加密
RDS_ENCRYPTED=$(aws rds describe-db-instances \
    --db-instance-identifier genai-demo-production-db \
    --query 'DBInstances[0].StorageEncrypted' --output text)

if [ "$RDS_ENCRYPTED" != "True" ]; then
    echo "❌ RDS 資料庫未啟用加密"
else
    echo "✅ RDS 資料庫加密已啟用"
fi

# 檢查 MSK 加密
MSK_ENCRYPTION=$(aws kafka describe-cluster \
    --cluster-arn $MSK_CLUSTER_ARN \
    --query 'ClusterInfo.EncryptionInfo.EncryptionAtRest.DataVolumeKMSKeyId' \
    --output text)

if [ "$MSK_ENCRYPTION" == "None" ]; then
    echo "❌ MSK 叢集未啟用靜態加密"
else
    echo "✅ MSK 叢集靜態加密已啟用"
fi

# 檢查 CloudTrail
CLOUDTRAIL_STATUS=$(aws cloudtrail get-trail-status \
    --name genai-demo-production-trail \
    --query 'IsLogging' --output text)

if [ "$CLOUDTRAIL_STATUS" != "True" ]; then
    echo "❌ CloudTrail 日誌未啟用"
else
    echo "✅ CloudTrail 日誌已啟用"
fi

echo "=== 合規性檢查完成 ==="
```

## 相關文檔

- [配置指南](../observability/configuration-guide.md)
- API 文檔
- [故障排除指南](../troubleshooting/observability-troubleshooting.md)
- [架構文檔](../architecture/observability-architecture.md)
