# 建置與部署系統

## 概述

本文檔提供完整的建置與部署指南，涵蓋 Gradle 建置系統、Docker 容器化、Kubernetes 部署和 CI/CD 管道的完整實作。我們採用現代化的 DevOps 實踐，確保從開發到生產的自動化和可靠性。

## 建置系統架構

### Gradle 多模組結構

我們的專案採用 Gradle 多模組架構，提供清晰的模組分離和依賴管理：

```
genai-demo/
├── app/                    # 主應用模組
├── cmc-frontend/          # CMC 管理前端
├── consumer-frontend/     # 消費者前端
├── infrastructure/        # AWS CDK 基礎設施
├── build.gradle          # 根建置腳本
├── settings.gradle       # 專案設置
└── gradle.properties     # 建置屬性
```

### 核心建置配置

#### 根建置腳本 (build.gradle)
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.6'
    id 'org.graalvm.buildtools.native' version '0.10.3'
    id 'jacoco'
    id 'org.sonarqube' version '4.4.1.3373'
    id 'com.github.spotbugs' version '6.0.7'
    id 'checkstyle'
    id 'org.flywaydb.flyway' version '10.10.0'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// 版本管理
ext {
    springBootVersion = '3.4.5'
    springCloudVersion = '2023.0.0'
    testcontainersVersion = '1.19.7'
    cucumberVersion = '7.18.0'
}
```

#### 依賴管理
```gradle
dependencies {
    // Spring Boot 核心
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    
    // 資料庫
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    
    // 監控和可觀測性
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'com.amazonaws:aws-xray-recorder-sdk-spring'
    implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
    
    // API 文檔
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // 測試依賴
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation "io.cucumber:cucumber-java:${cucumberVersion}"
    testImplementation "io.cucumber:cucumber-spring:${cucumberVersion}"
    testImplementation "io.cucumber:cucumber-junit-platform-engine:${cucumberVersion}"
    
    // 開發工具
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

### 自定義建置任務

#### 快速建置任務
```gradle
// 開發階段快速建置
tasks.register('quickBuild') {
    dependsOn 'compileJava', 'compileTestJava'
    description = '快速建置，不執行測試'
    group = 'build'
    
    doLast {
        println "✅ 快速建置完成 - 編譯時間: ${System.currentTimeMillis() - startTime}ms"
    }
}

// 完整建置與驗證
tasks.register('fullBuild') {
    dependsOn 'clean', 'build', 'jacocoTestReport', 'checkstyleMain', 'spotbugsMain'
    description = '完整建置包含所有品質檢查'
    group = 'build'
    
    doLast {
        println "✅ 完整建置完成 - 所有品質檢查通過"
    }
}

// 生產建置
tasks.register('productionBuild') {
    dependsOn 'clean', 'build', 'bootJar'
    description = '生產環境建置'
    group = 'build'
    
    doFirst {
        // 確保生產環境配置
        System.setProperty('spring.profiles.active', 'production')
    }
}
```

#### 測試任務配置
```gradle
// 單元測試
tasks.register('unitTest', Test) {
    description = '執行單元測試'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}

// 整合測試
tasks.register('integrationTest', Test) {
    description = '執行整合測試'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end'
    }
    maxHeapSize = '4g'
    maxParallelForks = 1
    
    // 測試容器配置
    systemProperty 'testcontainers.reuse.enable', 'true'
    systemProperty 'spring.profiles.active', 'test'
}

// BDD 測試
tasks.register('cucumber', JavaExec) {
    description = '執行 Cucumber BDD 測試'
    mainClass = 'io.cucumber.core.cli.Main'
    classpath = configurations.testRuntimeClasspath + sourceSets.test.output
    args = [
        '--plugin', 'pretty',
        '--plugin', 'html:build/reports/cucumber',
        '--plugin', 'json:build/reports/cucumber/cucumber.json',
        '--glue', 'solid.humank.genaidemo.bdd',
        'src/test/resources/features'
    ]
}
```

### 程式碼品質配置

#### Checkstyle 配置
```gradle
checkstyle {
    toolVersion = '10.12.7'
    configFile = file('config/checkstyle/checkstyle.xml')
    ignoreFailures = false
    maxWarnings = 0
}

checkstyleMain {
    reports {
        xml.required = true
        html.required = true
    }
}
```

#### SpotBugs 配置
```gradle
spotbugs {
    toolVersion = '4.8.3'
    effort = 'max'
    reportLevel = 'medium'
    excludeFilter = file('config/spotbugs/exclude.xml')
}

spotbugsMain {
    reports {
        xml.required = true
        html.required = true
    }
}
```

#### JaCoCo 測試覆蓋率
```gradle
jacoco {
    toolVersion = '0.8.8'
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/config/**',
                '**/dto/**',
                '**/Application.class'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80
            }
        }
    }
}
```

## 容器化配置

### Docker 多階段建置

#### 應用 Dockerfile
```dockerfile
# 多階段建置 Dockerfile
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src

# 建置應用
RUN gradle clean build -x test --no-daemon

# 生產階段
FROM openjdk:21-jre-slim

# 建立非 root 使用者
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 安裝必要工具
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 複製建置產物
COPY --from=builder /app/build/libs/*.jar app.jar

# 設置權限
RUN chown -R appuser:appuser /app
USER appuser

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 暴露埠口
EXPOSE 8080

# JVM 優化參數
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication"

# 啟動應用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose 開發環境
```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/genaidemo
      - SPRING_DATASOURCE_USERNAME=dev
      - SPRING_DATASOURCE_PASSWORD=dev123
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - app-network
    volumes:
      - ./logs:/app/logs

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=genaidemo
      - POSTGRES_USER=dev
      - POSTGRES_PASSWORD=dev123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev -d genaidemo"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5
    networks:
      - app-network

volumes:
  postgres_data:
  redis_data:

networks:
  app-network:
    driver: bridge
```

### 前端容器化

#### Next.js Dockerfile (CMC Frontend)
```dockerfile
# cmc-frontend/Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

# 生產階段
FROM node:18-alpine AS runner

WORKDIR /app

# 建立非 root 使用者
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT 3000
ENV NODE_ENV production

CMD ["node", "server.js"]
```

#### Angular Dockerfile (Consumer Frontend)
```dockerfile
# consumer-frontend/Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build --prod

# 生產階段
FROM nginx:alpine

COPY --from=builder /app/dist/consumer-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

## Kubernetes 部署

### 應用部署配置

#### Deployment 配置
```yaml
# k8s/app-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: genai-demo-app
  labels:
    app: genai-demo
    component: backend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: genai-demo
      component: backend
  template:
    metadata:
      labels:
        app: genai-demo
        component: backend
    spec:
      containers:
      - name: app
        image: genai-demo:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "kubernetes"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
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
        volumeMounts:
        - name: config-volume
          mountPath: /app/config
      volumes:
      - name: config-volume
        configMap:
          name: app-config
```

#### Service 配置
```yaml
# k8s/app-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: genai-demo-service
  labels:
    app: genai-demo
spec:
  selector:
    app: genai-demo
    component: backend
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
  type: ClusterIP
```

#### Ingress 配置
```yaml
# k8s/app-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: genai-demo-ingress
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - api.genaidemo.com
    secretName: genai-demo-tls
  rules:
  - host: api.genaidemo.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: genai-demo-service
            port:
              number: 80
```

### 配置管理

#### ConfigMap
```yaml
# k8s/app-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  application.yml: |
    spring:
      profiles:
        active: kubernetes
      datasource:
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
      jpa:
        hibernate:
          ddl-auto: validate
        show-sql: false
      cache:
        type: redis
      redis:
        host: redis-service
        port: 6379
    
    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      endpoint:
        health:
          show-details: always
    
    logging:
      level:
        solid.humank.genaidemo: INFO
        org.springframework.security: DEBUG
      pattern:
        console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

#### Secret 管理
```yaml
# k8s/app-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  url: <base64-encoded-database-url>
  username: <base64-encoded-username>
  password: <base64-encoded-password>
```

## CI/CD 管道

### GitHub Actions 工作流程

#### 主要 CI/CD 管道
```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: test
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Run tests
      run: ./gradlew test integrationTest jacocoTestReport
      env:
        SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/testdb
        SPRING_DATASOURCE_USERNAME: postgres
        SPRING_DATASOURCE_PASSWORD: test
        
    - name: Upload coverage reports
      uses: codecov/codecov-action@v3
      with:
        file: ./build/reports/jacoco/test/jacocoTestReport.xml
        
    - name: Run security scan
      run: ./gradlew dependencyCheckAnalyze
      
    - name: Upload security report
      uses: github/codeql-action/upload-sarif@v2
      if: always()
      with:
        sarif_file: build/reports/dependency-check-report.sarif

  build-and-push:
    needs: test
    runs-on: ubuntu-latest
    if: github.event_name == 'push'
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3
      
    - name: Log in to Container Registry
      uses: docker/login-action@v3
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
        
    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v5
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        tags: |
          type=ref,event=branch
          type=ref,event=pr
          type=sha,prefix={{branch}}-
          
    - name: Build and push Docker image
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}
        cache-from: type=gha
        cache-to: type=gha,mode=max

  deploy-staging:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-east-1
        
    - name: Update kubeconfig
      run: aws eks update-kubeconfig --name genai-demo-staging
      
    - name: Deploy to staging
      run: |
        kubectl set image deployment/genai-demo-app app=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:develop
        kubectl rollout status deployment/genai-demo-app
        
    - name: Run smoke tests
      run: ./scripts/smoke-test.sh staging

  deploy-production:
    needs: build-and-push
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-east-1
        
    - name: Update kubeconfig
      run: aws eks update-kubeconfig --name genai-demo-production
      
    - name: Deploy to production
      run: |
        kubectl set image deployment/genai-demo-app app=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:main
        kubectl rollout status deployment/genai-demo-app
        
    - name: Verify deployment
      run: ./scripts/health-check.sh production
```

### 部署腳本

#### 自動化部署腳本
```bash
#!/bin/bash
# scripts/deploy.sh

set -e

ENVIRONMENT=${1:-staging}
IMAGE_TAG=${2:-latest}

echo "🚀 開始部署到 $ENVIRONMENT 環境..."

# 驗證環境參數
case $ENVIRONMENT in
  staging|production)
    echo "✅ 環境: $ENVIRONMENT"
    ;;
  *)
    echo "❌ 無效環境: $ENVIRONMENT"
    echo "使用方法: $0 [staging|production] [image-tag]"
    exit 1
    ;;
esac

# 設置 kubectl 上下文
kubectl config use-context genai-demo-$ENVIRONMENT

# 更新部署
echo "📦 更新應用映像..."
kubectl set image deployment/genai-demo-app \
  app=ghcr.io/your-org/genai-demo:$IMAGE_TAG

# 等待部署完成
echo "⏳ 等待部署完成..."
kubectl rollout status deployment/genai-demo-app --timeout=300s

# 驗證部署
echo "🔍 驗證部署狀態..."
kubectl get pods -l app=genai-demo

# 執行健康檢查
echo "🏥 執行健康檢查..."
./scripts/health-check.sh $ENVIRONMENT

echo "✅ 部署完成！"
```

#### 健康檢查腳本
```bash
#!/bin/bash
# scripts/health-check.sh

ENVIRONMENT=${1:-staging}

case $ENVIRONMENT in
  staging)
    BASE_URL="https://staging-api.genaidemo.com"
    ;;
  production)
    BASE_URL="https://api.genaidemo.com"
    ;;
  *)
    echo "❌ 無效環境: $ENVIRONMENT"
    exit 1
    ;;
esac

echo "🏥 檢查 $ENVIRONMENT 環境健康狀態..."

# 健康檢查端點
HEALTH_URL="$BASE_URL/actuator/health"

# 等待服務啟動
echo "⏳ 等待服務啟動..."
for i in {1..30}; do
  if curl -f -s "$HEALTH_URL" > /dev/null; then
    echo "✅ 服務已啟動"
    break
  fi
  echo "⏳ 等待中... ($i/30)"
  sleep 10
done

# 詳細健康檢查
echo "🔍 執行詳細健康檢查..."

# 檢查應用健康狀態
HEALTH_STATUS=$(curl -s "$HEALTH_URL" | jq -r '.status')
if [ "$HEALTH_STATUS" = "UP" ]; then
  echo "✅ 應用健康狀態: $HEALTH_STATUS"
else
  echo "❌ 應用健康狀態: $HEALTH_STATUS"
  exit 1
fi

# 檢查資料庫連接
DB_STATUS=$(curl -s "$HEALTH_URL" | jq -r '.components.db.status')
if [ "$DB_STATUS" = "UP" ]; then
  echo "✅ 資料庫連接: $DB_STATUS"
else
  echo "❌ 資料庫連接: $DB_STATUS"
  exit 1
fi

# 檢查 API 回應
API_URL="$BASE_URL/../api/v1/health"
if curl -f -s "$API_URL" > /dev/null; then
  echo "✅ API 端點正常"
else
  echo "❌ API 端點異常"
  exit 1
fi

echo "✅ 所有健康檢查通過！"
```

## 監控和可觀測性

### 應用監控配置

完整的監控配置請參考：**[技術棧配置](../tools-and-environment/technology-stack.md#監控配置)**

核心監控端點：
- `/actuator/health` - 健康檢查
- `/actuator/metrics` - 應用指標  
- `/actuator/prometheus` - Prometheus 指標

#### 日誌配置
```yaml
# logback-spring.xml
<configuration>
  <springProfile name="!local">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
          <timestamp/>
          <logLevel/>
          <loggerName/>
          <message/>
          <mdc/>
          <stackTrace/>
        </providers>
      </encoder>
    </appender>
  </springProfile>
  
  <root level="INFO">
    <appender-ref ref="STDOUT"/>
  </root>
</configuration>
```

### 效能監控

#### JVM 監控指標
- **記憶體使用**: 堆記憶體、非堆記憶體使用率
- **垃圾回收**: GC 頻率和耗時
- **執行緒**: 活躍執行緒數、死鎖檢測
- **類別載入**: 載入的類別數量

#### 應用監控指標
- **HTTP 請求**: 回應時間、吞吐量、錯誤率
- **資料庫**: 連接池使用率、查詢執行時間
- **快取**: 命中率、驅逐率
- **業務指標**: 訂單處理量、使用者註冊數

## 故障排除

### 常見建置問題

#### Gradle 建置失敗
```bash
# 清理建置快取
./gradlew clean

# 重新整理依賴
./gradlew --refresh-dependencies

# 檢查依賴衝突
./gradlew dependencies --configuration runtimeClasspath

# 完整重建
rm -rf ~/.gradle/caches
./gradlew clean build
```

#### Docker 建置問題
```bash
# 清理 Docker 快取
docker system prune -a

# 重建映像
docker build --no-cache -t genai-demo:latest .

# 檢查映像大小
docker images genai-demo

# 檢查容器日誌
docker logs <container-id>
```

### 部署問題診斷

#### Kubernetes 部署問題
```bash
# 檢查 Pod 狀態
kubectl get pods -l app=genai-demo

# 查看 Pod 日誌
kubectl logs -l app=genai-demo --tail=100

# 檢查 Pod 事件
kubectl describe pod <pod-name>

# 檢查服務端點
kubectl get endpoints genai-demo-service

# 檢查 Ingress 狀態
kubectl describe ingress genai-demo-ingress
```

#### 效能問題診斷
```bash
# 檢查資源使用
kubectl top pods -l app=genai-demo

# 檢查 JVM 記憶體
kubectl exec <pod-name> -- jstat -gc 1

# 檢查應用指標
curl https://api.genaidemo.com/actuator/metrics

# 檢查資料庫連接
kubectl exec <pod-name> -- netstat -an | grep 5432
```

---

**相關文檔**:
- [技術棧配置](../tools-and-environment/technology-stack.md)
- [測試策略](../testing/README.md)
- [監控運維](../../../observability/README.md)
- [安全配置](../quality-assurance/security-practices.md)

**下一步**: [部署運維指南](../../deployment/README.md) →
}

tasks.register('fullBuild') {
    dependsOn 'build', 'jacocoTestReport', 'checkstyleMain', 'spotbugsMain'
    description = 'Full build with all quality checks'
    group = 'build'
}

// 測試任務優化
test {
    useJUnitPlatform()
    maxParallelForks = Runtime.runtime.availableProcessors()
    
    // JVM 參數優化
    jvmArgs = [
        '-XX:+UseG1GC',
        '-XX:MaxGCPauseMillis=100',
        '-Xmx2g'
    ]
    
    // 測試報告
    reports {
        html.required = true
        junitXml.required = true
    }
    
    // 測試事件記錄
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}

// 應用程式執行配置
bootRun {
    jvmArgs = [
        '-Dspring.profiles.active=development',
        '-Xmx1g'
    ]
    
    // 開發環境變數
    environment 'DATABASE_URL', 'jdbc:h2:mem:devdb'
    environment 'LOG_LEVEL', 'DEBUG'
}
```

#### Gradle Wrapper 配置

```gradle
// gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 📦 多模組設置

#### 專案結構

```
genai-demo/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── backend/
│   ├── build.gradle
│   └── src/
├── shared/
│   ├── build.gradle
│   └── src/
├── frontend-cmc/
│   ├── package.json
│   └── src/
└── frontend-consumer/
    ├── package.json
    └── src/
```

#### 根專案配置

```gradle
// settings.gradle
rootProject.name = 'genai-demo'

include 'backend'
include 'shared'

// 模組目錄配置
project(':backend').projectDir = file('backend')
project(':shared').projectDir = file('shared')

// 插件管理
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// 依賴解析策略
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
```

#### 子模組配置

```gradle
// backend/build.gradle
dependencies {
    implementation project(':shared')
    
    // Spring Boot 依賴
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    
    // 測試依賴
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation project(':shared').sourceSets.test.output
}

// shared/build.gradle
dependencies {
    api 'org.springframework.boot:spring-boot-starter-validation'
    api 'com.fasterxml.jackson.core:jackson-databind'
    
    // 測試工具
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testImplementation 'org.mockito:mockito-core'
    testImplementation 'org.assertj:assertj-core'
}

// 共享測試配置
configurations {
    testArtifacts.extendsFrom testImplementation
}

artifacts {
    testArtifacts jar
}
```

### 📚 依賴管理

#### 版本目錄 (Version Catalog)

```toml
# gradle/libs.versions.toml
[versions]
spring-boot = "3.4.5"
java = "21"
junit = "5.10.1"
mockito = "5.8.0"
assertj = "3.24.2"
cucumber = "7.18.1"
testcontainers = "1.19.3"

[libraries]
# Spring Boot
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
spring-boot-starter-data-jpa = { module = "org.springframework.boot:spring-boot-starter-data-jpa", version.ref = "spring-boot" }
spring-boot-starter-security = { module = "org.springframework.boot:spring-boot-starter-security", version.ref = "spring-boot" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test", version.ref = "spring-boot" }

# 測試框架
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
mockito-core = { module = "org.mockito:mockito-core", version.ref = "mockito" }
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj" }

# Cucumber
cucumber-java = { module = "io.cucumber:cucumber-java", version.ref = "cucumber" }
cucumber-junit-platform-engine = { module = "io.cucumber:cucumber-junit-platform-engine", version.ref = "cucumber" }
cucumber-spring = { module = "io.cucumber:cucumber-spring", version.ref = "cucumber" }

# Testcontainers
testcontainers-junit-jupiter = { module = "org.testcontainers:junit-jupiter", version.ref = "testcontainers" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql", version.ref = "testcontainers" }

[bundles]
spring-boot = ["spring-boot-starter-web", "spring-boot-starter-data-jpa", "spring-boot-starter-security"]
testing = ["junit-jupiter", "mockito-core", "assertj-core"]
cucumber = ["cucumber-java", "cucumber-junit-platform-engine", "cucumber-spring"]
testcontainers = ["testcontainers-junit-jupiter", "testcontainers-postgresql"]

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version = "1.1.6" }
graalvm-native = { id = "org.graalvm.buildtools.native", version = "0.10.3" }
```

#### 使用版本目錄

```gradle
// build.gradle
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.graalvm.native)
}

dependencies {
    implementation libs.bundles.spring.boot
    
    testImplementation libs.bundles.testing
    testImplementation libs.bundles.cucumber
    testImplementation libs.bundles.testcontainers
}
```

#### 依賴版本管理策略

```gradle
// 依賴更新檢查
tasks.register('dependencyUpdates', DependencyUpdatesTask) {
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

// 依賴鎖定
dependencyLocking {
    lockAllConfigurations()
}

// 依賴驗證
tasks.register('verifyDependencies') {
    doLast {
        configurations.runtimeClasspath.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            println "Verified: ${artifact.moduleVersion.id}"
        }
    }
}
```

### 🚀 CI/CD 整合

#### GitHub Actions 工作流程

```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

env:
  JAVA_VERSION: '21'
  GRADLE_OPTS: -Dorg.gradle.daemon=false

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: postgres
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0  # SonarQube 需要完整歷史
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-
      
      - name: Make gradlew executable
        run: chmod +x ./gradlew
      
      - name: Run tests
        run: ./gradlew test integrationTest
        env:
          DATABASE_URL: jdbc:postgresql://localhost:5432/testdb
          DATABASE_USERNAME: postgres
          DATABASE_PASSWORD: postgres
      
      - name: Generate test report
        run: ./gradlew jacocoTestReport
      
      - name: Run quality checks
        run: ./gradlew checkstyleMain spotbugsMain
      
      - name: SonarQube analysis
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./gradlew sonar
      
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: |
            build/reports/tests/
            build/reports/jacoco/
            build/reports/checkstyle/
            build/reports/spotbugs/

  build:
    needs: test
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
      
      - name: Build application
        run: ./gradlew build -x test
      
      - name: Build Docker image
        run: |
          docker build -t genai-demo:${{ github.sha }} .
          docker tag genai-demo:${{ github.sha }} genai-demo:latest
      
      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: build-artifacts
          path: |
            build/libs/
            build/distributions/

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
          aws-region: ap-northeast-1
      
      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2
      
      - name: Build and push Docker image
        env:
          ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
          ECR_REPOSITORY: genai-demo
          IMAGE_TAG: ${{ github.sha }}
        run: |
          docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
          docker tag $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG $ECR_REGISTRY/$ECR_REPOSITORY:latest
          docker push $ECR_REGISTRY/$ECR_REPOSITORY:latest
      
      - name: Deploy to ECS
        run: |
          aws ecs update-service \
            --cluster genai-demo-cluster \
            --service genai-demo-service \
            --force-new-deployment
```

#### Docker 配置

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jre-alpine

# 建立應用程式目錄
WORKDIR /app

# 複製 JAR 檔案
COPY build/libs/*.jar app.jar

# 建立非 root 使用者
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# 設定檔案權限
RUN chown -R appuser:appgroup /app
USER appuser

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM 參數優化
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# 啟動應用程式
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose 開發環境

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DATABASE_URL=jdbc:postgresql://postgres:5432/genaidemo
      - DATABASE_USERNAME=genaidemo
      - DATABASE_PASSWORD=password
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - app-network

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: genaidemo
      POSTGRES_USER: genaidemo
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U genaidemo"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-network

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - app-network

volumes:
  postgres_data:

networks:
  app-network:
    driver: bridge
```

## 部署策略

### 環境配置

#### 開發環境

```yaml
# application-development.yml
spring:
  datasource:
    url: jdbc:h2:mem:devdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  h2:
    console:
      enabled: true

logging:
  level:
    solid.humank.genaidemo: DEBUG
    org.springframework.web: DEBUG
```

#### 測試環境

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop

logging:
  level:
    solid.humank.genaidemo: INFO
```

#### 生產環境

```yaml
# application-production.yml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    solid.humank.genaidemo: INFO
    org.springframework.web: WARN
```

### 部署腳本

#### 部署自動化

```bash
#!/bin/bash
# deploy.sh

set -e

# 環境變數
ENVIRONMENT=${1:-staging}
IMAGE_TAG=${2:-latest}
ECR_REGISTRY="your-account.dkr.ecr.region.amazonaws.com"
ECR_REPOSITORY="genai-demo"

echo "Deploying to $ENVIRONMENT environment with image tag $IMAGE_TAG"

# 建置應用程式
echo "Building application..."
./gradlew build

# 建置 Docker 映像
echo "Building Docker image..."
docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .

# 推送到 ECR
echo "Pushing to ECR..."
aws ecr get-login-password --region ap-northeast-1 | docker login --username AWS --password-stdin $ECR_REGISTRY
docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

# 更新 ECS 服務
echo "Updating ECS service..."
aws ecs update-service \
  --cluster genai-demo-$ENVIRONMENT \
  --service genai-demo-service \
  --force-new-deployment

echo "Deployment completed successfully!"
```

#### 健康檢查腳本

```bash
#!/bin/bash
# health-check.sh

ENDPOINT=${1:-http://localhost:8080/actuator/health}
MAX_ATTEMPTS=${2:-30}
SLEEP_INTERVAL=${3:-10}

echo "Checking health at $ENDPOINT"

for i in $(seq 1 $MAX_ATTEMPTS); do
  echo "Attempt $i/$MAX_ATTEMPTS"
  
  if curl -f -s $ENDPOINT > /dev/null; then
    echo "Health check passed!"
    exit 0
  fi
  
  if [ $i -lt $MAX_ATTEMPTS ]; then
    echo "Health check failed, retrying in $SLEEP_INTERVAL seconds..."
    sleep $SLEEP_INTERVAL
  fi
done

echo "Health check failed after $MAX_ATTEMPTS attempts"
exit 1
```

## 效能優化

### 建置效能

```gradle
// 建置效能優化
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC

// 編譯器優化
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-Xlint:unchecked', '-Xlint:deprecation']
    options.encoding = 'UTF-8'
    options.incremental = true
}
```

### 應用程式效能

```yaml
# JVM 調優
JAVA_OPTS: >
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=100
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -Xms512m
  -Xmx2g
  -XX:MetaspaceSize=256m
  -XX:MaxMetaspaceSize=512m
```

## 監控與日誌

### 應用程式監控

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

### 日誌配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <springProfile name="!production">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>
    
    <springProfile name="production">
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/application.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/application.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                <maxFileSize>100MB</maxFileSize>
                <maxHistory>30</maxHistory>
                <totalSizeCap>3GB</totalSizeCap>
            </rollingPolicy>
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

## 最佳實踐

### 建置最佳實踐

1. **快速反饋**: 優化建置時間，提供快速反饋
2. **並行建置**: 使用 Gradle 並行功能
3. **增量建置**: 啟用增量編譯和建置快取
4. **依賴管理**: 使用版本目錄統一管理依賴

### 部署最佳實踐

1. **藍綠部署**: 使用藍綠部署策略減少停機時間
2. **健康檢查**: 實作完整的健康檢查機制
3. **回滾策略**: 準備快速回滾方案
4. **監控告警**: 建立完整的監控和告警系統

### 安全最佳實踐

1. **秘密管理**: 使用 AWS Secrets Manager 管理敏感資訊
2. **最小權限**: 遵循最小權限原則
3. **映像掃描**: 定期掃描 Docker 映像漏洞
4. **網路安全**: 配置適當的網路安全群組

---

**相關文檔**
- 技術棧與工具鏈
- 品質保證
- 工作流程與協作
