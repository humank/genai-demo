# Build and Deployment System

## Overview

This document provides a complete build and deployment guide, covering Gradle build system, Docker containerization, Kubernetes deployment, and CI/CD pipeline implementation. We adopt modern DevOps practices to ensure automation and reliability from development to production.

## Build System Architecture

### Gradle Multi-Module Structure

Our project adopts a Gradle multi-module architecture, providing clear module separation and dependency management:

```
genai-demo/
├── app/                    # Main application module
├── cmc-frontend/          # CMC management frontend
├── consumer-frontend/     # Consumer frontend
├── infrastructure/        # AWS CDK infrastructure
├── build.gradle          # Root build script
├── settings.gradle       # Project settings
└── gradle.properties     # Build properties
```

### Core Build Configuration

#### Root Build Script (build.gradle)
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

// Version management
ext {
    springBootVersion = '3.4.5'
    springCloudVersion = '2023.0.0'
    testcontainersVersion = '1.19.7'
    cucumberVersion = '7.18.0'
}
```

#### Dependency Management
```gradle
dependencies {
    // Spring Boot core
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    
    // Database
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'
    
    // Monitoring and observability
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'com.amazonaws:aws-xray-recorder-sdk-spring'
    implementation 'org.springframework.cloud:spring-cloud-starter-sleuth'
    
    // API documentation
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
    
    // Test dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation "io.cucumber:cucumber-java:${cucumberVersion}"
    testImplementation "io.cucumber:cucumber-spring:${cucumberVersion}"
    testImplementation "io.cucumber:cucumber-junit-platform-engine:${cucumberVersion}"
    
    // Development tools
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

### Custom Build Tasks

#### Quick Build Tasks
```gradle
// Quick build for development phase
tasks.register('quickBuild') {
    dependsOn 'compileJava', 'compileTestJava'
    description = 'Quick build without running tests'
    group = 'build'
    
    doLast {
        println "✅ Quick build completed - Compile time: ${System.currentTimeMillis() - startTime}ms"
    }
}

// Full build with validation
tasks.register('fullBuild') {
    dependsOn 'clean', 'build', 'jacocoTestReport', 'checkstyleMain', 'spotbugsMain'
    description = 'Full build with all quality checks'
    group = 'build'
    
    doLast {
        println "✅ Full build completed - All quality checks passed"
    }
}

// Production build
tasks.register('productionBuild') {
    dependsOn 'clean', 'build', 'bootJar'
    description = 'Production environment build'
    group = 'build'
    
    doFirst {
        // Ensure production environment configuration
        System.setProperty('spring.profiles.active', 'production')
    }
}
```

#### Test Task Configuration
```gradle
// Unit tests
tasks.register('unitTest', Test) {
    description = 'Run unit tests'
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

// Integration tests
tasks.register('integrationTest', Test) {
    description = 'Run integration tests'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end'
    }
    maxHeapSize = '4g'
    maxParallelForks = 1
    
    // Test container configuration
    systemProperty 'testcontainers.reuse.enable', 'true'
    systemProperty 'spring.profiles.active', 'test'
}

// BDD tests
tasks.register('cucumber', JavaExec) {
    description = 'Run Cucumber BDD tests'
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

## Containerization Configuration

### Docker Multi-stage Build

#### Application Dockerfile
```dockerfile
# Multi-stage build Dockerfile
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
COPY src ./src

# Build application
RUN gradle clean build -x test --no-daemon

# Production stage
FROM openjdk:21-jre-slim

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install necessary tools
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy build artifacts
COPY --from=builder /app/build/libs/*.jar app.jar

# Set permissions
RUN chown -R appuser:appuser /app
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM optimization parameters
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+UseStringDeduplication"

# Start application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Docker Compose Development Environment
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

## Kubernetes Deployment

### Application Deployment Configuration

#### Deployment Configuration
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

## CI/CD Pipeline

### GitHub Actions Workflow

#### Main CI/CD Pipeline
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

## Monitoring and Observability

### Application Monitoring Configuration

For complete monitoring configuration, please refer to: **Technology Stack Configuration**

Core monitoring endpoints:
- `/actuator/health` - Health checks
- `/actuator/metrics` - Application metrics  
- `/actuator/prometheus` - Prometheus metrics

#### Logging Configuration
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

## Troubleshooting

### Common Build Issues

#### Gradle Build Failures
```bash
# Clean build cache
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# Check dependency conflicts
./gradlew dependencies --configuration runtimeClasspath

# Complete rebuild
rm -rf ~/.gradle/caches
./gradlew clean build
```

#### Docker Build Issues
```bash
# Clean Docker cache
docker system prune -a

# Rebuild image
docker build --no-cache -t genai-demo:latest .

# Check image size
docker images genai-demo

# Check container logs
docker logs <container-id>
```

### Deployment Issue Diagnosis

#### Kubernetes Deployment Issues
```bash
# Check Pod status
kubectl get pods -l app=genai-demo

# View Pod logs
kubectl logs -l app=genai-demo --tail=100

# Check Pod events
kubectl describe pod <pod-name>

# Check service endpoints
kubectl get endpoints genai-demo-service

# Check Ingress status
kubectl describe ingress genai-demo-ingress
```

---

**Related Documentation**:
- [Technology Stack Configuration](../tools-and-environment/technology-stack.md)
- [Testing Strategy](../testing/README.md)
- [Monitoring Operations](../../../observability/README.md)
- [Security Configuration](../quality-assurance/security-practices.md)

**Next Step**: [Deployment Operations Guide](../../deployment/README.md) →