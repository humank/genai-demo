# 建置與部署

## 概述

本文檔提供完整的建置與部署指南，包含 Gradle 配置、多模組設置、依賴管理和 CI/CD 整合的實作方法。

## 🔧 建置系統

### 🐘 Gradle 配置

#### 基本配置

```gradle
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.6'
    id 'org.graalvm.buildtools.native' version '0.10.3'
    id 'jacoco'
    id 'org.sonarqube' version '4.4.1.3373'
    id 'com.github.spotbugs' version '6.0.7'
    id 'checkstyle'
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
}
```

#### 建置任務配置

```gradle
// 自定義建置任務
tasks.register('quickBuild') {
    dependsOn 'compileJava', 'compileTestJava'
    description = 'Quick build without tests'
    group = 'build'
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