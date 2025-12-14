# Local Development Environment Setup

> **Last Updated**: 2025-10-25

## Overview

This guide provides step-by-step instructions for setting up your local development environment for the Enterprise E-Commerce Platform. Following this guide will ensure you have all necessary tools and configurations to start developing.

## Prerequisites

### Required Software

#### 1. Java Development Kit (JDK) 21

**Installation:**

```bash
# macOS (using Homebrew)
brew install openjdk@21

# Verify installation
java -version
# Expected output: openjdk version "21.x.x"
```

**Configuration:**

```bash
# Add to ~/.zshrc or ~/.bash_profile
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH
```

#### 2. Gradle 8.x

**Installation:**

```bash
# macOS (using Homebrew)
brew install gradle

# Verify installation
gradle --version
# Expected output: Gradle 8.x
```

**Note:** The project includes Gradle Wrapper (`./gradlew`), so local Gradle installation is optional.

#### 3. Docker Desktop

**Installation:**

1. Download Docker Desktop from [docker.com](https://www.docker.com/products/docker-desktop)
2. Install and start Docker Desktop
3. Verify installation:

```bash
docker --version
# Expected output: Docker version 24.x.x

docker-compose --version
# Expected output: Docker Compose version 2.x.x
```

**Configuration:**

- Allocate at least 4GB RAM to Docker
- Allocate at least 2 CPU cores
- Enable Kubernetes (optional, for local K8s testing)

#### 4. Node.js and npm

**Installation:**

```bash
# macOS (using Homebrew)
brew install node@20

# Verify installation
node --version
# Expected output: v20.x.x

npm --version
# Expected output: 10.x.x
```

#### 5. Git

**Installation:**

```bash
# macOS (using Homebrew)
brew install git

# Verify installation
git --version
# Expected output: git version 2.x.x
```

**Configuration:**

```bash
# Set your identity
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Set default branch name
git config --global init.defaultBranch main

# Enable credential helper
git config --global credential.helper osxkeychain
```

### Optional but Recommended

#### 1. AWS CLI

```bash
# macOS (using Homebrew)
brew install awscli

# Verify installation
aws --version

# Configure AWS credentials
aws configure
```

#### 2. kubectl (for Kubernetes)

```bash
# macOS (using Homebrew)
brew install kubectl

# Verify installation
kubectl version --client
```

#### 3. PlantUML (for diagram generation)

```bash
# macOS (using Homebrew)
brew install plantuml

# Verify installation
plantuml -version
```

## Project Setup

### 1. Clone the Repository

```bash
# Clone the repository
git clone https://github.com/your-org/enterprise-ecommerce-platform.git

# Navigate to project directory
cd enterprise-ecommerce-platform
```

### 2. Set Up Backend

#### Build the Project

```bash
# Build the project using Gradle Wrapper
./gradlew clean build

# Expected output: BUILD SUCCESSFUL
```

#### Run Tests

```bash
# Run all tests
./gradlew test

# Run only unit tests (fast)
./gradlew unitTest

# Run integration tests
./gradlew integrationTest
```

### 3. Set Up Infrastructure Services

#### Start Local Services with Docker Compose

```bash
# Start PostgreSQL, Redis, and Kafka
docker-compose up -d

# Verify services are running
docker-compose ps

# Expected output:
# NAME                    STATUS
# postgres                Up
# redis                   Up
# kafka                   Up
# zookeeper               Up
```

#### Verify Database Connection

```bash
# Connect to PostgreSQL
docker exec -it postgres psql -U postgres -d ecommerce

# Run a test query
SELECT version();

# Exit
\q
```

#### Verify Redis Connection

```bash
# Connect to Redis
docker exec -it redis redis-cli

# Test connection
PING
# Expected output: PONG

# Exit
exit
```

### 4. Set Up Frontend Applications

#### CMC Management Frontend (Next.js)

```bash
# Navigate to CMC frontend directory
cd cmc-frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Application will be available at http://localhost:3000
```

#### Consumer Frontend (Angular)

```bash
# Navigate to consumer frontend directory
cd consumer-frontend

# Install dependencies
npm install

# Start development server
npm start

# Application will be available at http://localhost:4200
```

### 5. Run the Complete Application

#### Start Backend Application

```bash
# From project root
./gradlew bootRun

# Application will start on http://localhost:8080
```

#### Verify Backend is Running

```bash
# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected output:
# {"status":"UP"}

# Check API documentation
open http://localhost:8080/swagger-ui.html
```

## Environment Configuration

### Application Properties

Create `application-local.yml` in `app/src/main/resources/`:

```yaml
spring:
  profiles:
    active: local
  
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  redis:
    host: localhost
    port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ecommerce-local
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

server:
  port: 8080

logging:
  level:
    solid.humank.genaidemo: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

### Environment Variables

Create `.env` file in project root:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ecommerce
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# AWS (for local development)
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test

# Application
SPRING_PROFILES_ACTIVE=local
SERVER_PORT=8080
```

## Verification Steps

### 1. Verify Backend Services

```bash
# Check all services are running
docker-compose ps

# Check backend application logs
./gradlew bootRun

# In another terminal, test API endpoints
curl http://localhost:8080/api/v1/customers
```

### 2. Verify Frontend Applications

```bash
# Check CMC frontend
open http://localhost:3000

# Check consumer frontend
open http://localhost:4200
```

### 3. Run Integration Tests

```bash
# Run full test suite
./gradlew test

# Check test results
open app/build/reports/tests/test/index.html
```

### 4. Verify Database Migrations

```bash
# Check Flyway migration status
./gradlew flywayInfo

# Run migrations manually if needed
./gradlew flywayMigrate
```

## Troubleshooting

### Common Issues

#### Issue: Port Already in Use

**Symptom:**

```text
Port 8080 is already in use
```

**Solution:**

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or use a different port
SERVER_PORT=8081 ./gradlew bootRun
```

#### Issue: Docker Services Not Starting

**Symptom:**

```text
ERROR: Cannot start service postgres: port is already allocated
```

**Solution:**

```bash
# Stop all Docker containers
docker-compose down

# Remove volumes if needed
docker-compose down -v

# Start services again
docker-compose up -d
```

#### Issue: Database Connection Failed

**Symptom:**

```text
Connection to localhost:5432 refused
```

**Solution:**

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

#### Issue: Gradle Build Failed

**Symptom:**

```text
BUILD FAILED
```

**Solution:**

```bash
# Clean build directory
./gradlew clean

# Build with stack trace for details
./gradlew build --stacktrace

# Check Java version
java -version

# Ensure JAVA_HOME is set correctly
echo $JAVA_HOME
```

#### Issue: Frontend Dependencies Installation Failed

**Symptom:**

```text
npm ERR! code ERESOLVE
```

**Solution:**

```bash
# Clear npm cache
npm cache clean --force

# Delete node_modules and package-lock.json
rm -rf node_modules package-lock.json

# Install dependencies with legacy peer deps
npm install --legacy-peer-deps
```

#### Issue: Redis Connection Timeout

**Symptom:**

```text
Unable to connect to Redis at localhost:6379
```

**Solution:**

```bash
# Check if Redis is running
docker-compose ps redis

# Test Redis connection
docker exec -it redis redis-cli PING

# Restart Redis
docker-compose restart redis
```

### Getting Help

If you encounter issues not covered here:

1. Check the [Troubleshooting Guide](../../operational/troubleshooting/common-issues.md)
2. Search existing issues in the project repository
3. Ask in the team Slack channel: `#dev-support`
4. Create a new issue with detailed error messages and steps to reproduce

## Next Steps

After completing the local environment setup:

1. Read the [IDE Configuration Guide](ide-configuration.md) to set up your IDE
2. Review the [Coding Standards](../coding-standards/java-standards.md)
3. Follow the [Developer Onboarding Guide](onboarding.md) for a structured learning path
4. Try implementing a simple feature following the [Implementation Examples](../examples/creating-aggregate.md)

## Quick Reference

### Essential Commands

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# Build backend
./gradlew clean build

# Run backend
./gradlew bootRun

# Run tests
./gradlew test

# Start CMC frontend
cd cmc-frontend && npm run dev

# Start consumer frontend
cd consumer-frontend && npm start
```

### Service URLs

- Backend API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- CMC Frontend: <http://localhost:3000>
- Consumer Frontend: <http://localhost:4200>
- PostgreSQL: localhost:5432
- Redis: localhost:6379
- Kafka: localhost:9092

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
