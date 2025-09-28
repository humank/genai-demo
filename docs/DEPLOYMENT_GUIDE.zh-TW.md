# Deployment Guide (繁體中文版)

> **注意**: 此文件需要翻譯。原始英文版本請參考對應的英文文件。

# Deployment Guide

## 🚀 Quick Start

### Prerequisites

- **Java 21+** (Backend)
- **Node.js 18+** (Frontend and Infrastructure)
- **AWS CLI** (Cloud deployment)
- **Docker** (Optional, for local development)

### Local Development Environment

```bash
# 1. Start backend (Spring Boot)
cd app
./gradlew bootRun

# 2. Start consumer frontend (Angular)
cd consumer-frontend
npm install
npm start

# 3. Start management frontend (Next.js)
cd cmc-frontend
npm install
npm run dev
```

### Cloud Deployment

#### Development Environment Deployment

```bash
# Infrastructure deployment (without Analytics)
npm run deploy:dev

# Or manual deployment
cd infrastructure
./deploy-consolidated.sh development us-east-1 false
```

#### Production Environment Deployment

```bash
# Full feature deployment (with Analytics)
npm run deploy:prod

# Or manual deployment
cd infrastructure
./deploy-consolidated.sh production us-east-1 true
```

## 🏗️ Architecture Overview

### Backend Service (Spring Boot)

- **Port**: 8080
- **Health Check**: <http://localhost:8080/actuator/health>
- **API Documentation**: <http://localhost:8080/swagger-ui.html>

### Frontend Applications

- **Consumer Frontend**: <http://localhost:4200> (Angular)
- **Management Frontend**: <http://localhost:3000> (Next.js)

### Infrastructure (AWS CDK)

- **Network Layer**: VPC, subnets, security groups
- **Security Layer**: KMS keys, IAM roles
- **Core Layer**: Load balancers, compute resources
- **Monitoring Layer**: CloudWatch, alarms
- **Analytics Layer**: Data lake, Kinesis, QuickSight (optional)

## 🔧 Configuration Guide

### Environment Variables

```bash
# Development environment
SPRING_PROFILES_ACTIVE=development
DATABASE_URL=jdbc:h2:mem:testdb

# Production environment
SPRING_PROFILES_ACTIVE=production
DATABASE_URL=jdbc:postgresql://...
AWS_REGION=us-east-1
```

### Feature Toggles

```yaml
# application.yml
observability:
  analytics:
    enabled: false  # Default disabled in development
  websocket:
    enabled: false  # Planned feature
  kafka:
    enabled: false  # Default disabled in development
```

## 📊 Monitoring and Logging

### Available Monitoring Endpoints

- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/info` - Application information
- `/actuator/prometheus` - Prometheus metrics

### Log Locations

- **Application Logs**: `logs/application.log`
- **Access Logs**: `logs/access.log`
- **Error Logs**: `logs/error.log`

## 🧪 Testing

### Backend Testing

```bash
cd app

# Unit tests (fast)
./gradlew unitTest

# Integration tests
./gradlew integrationTest

# Complete test suite
./gradlew test
```

### Infrastructure Testing

```bash
cd infrastructure

# CDK tests
npm test

# Specific tests
npm run test:unit
npm run test:integration
```

## 🔍 Troubleshooting

### Common Issues

1. **Backend Startup Failure**
   - Check Java version (requires 21+)
   - Check if port 8080 is occupied
   - Check database connection

2. **Frontend Compilation Errors**
   - Clear node_modules: `rm -rf node_modules && npm install`
   - Check Node.js version (requires 18+)

3. **CDK Deployment Failure**
   - Check AWS credentials: `aws sts get-caller-identity`
   - Check CDK version: `cdk --version`
   - Check region settings

### Log Checking

```bash
# Check application logs
tail -f logs/application.log

# Check Docker container logs (if using)
docker logs genai-demo-app

# Check AWS CloudWatch logs
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/genai-demo
```

## 📞 Support Resources

- **Project Documentation**: PROJECT_STRUCTURE.md
- **Refactoring Summary**: reports-summaries/project-management/REFACTORING_SUMMARY.md
- **Infrastructure Guide**: infrastructure/CONSOLIDATED_DEPLOYMENT.md
- **Troubleshooting**: docs/troubleshooting/README.md

---

**Last Updated**: September 24, 2025 11:28 PM (Taipei Time)  
**Maintainer**: Development Team


---
*此文件由自動翻譯系統生成，可能需要人工校對。*
