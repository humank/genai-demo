# Docker Containerized Deployment Guide

## Overview

This guide explains how to deploy the GenAI Demo application using Docker. We provide lightweight Docker images optimized for ARM64 architecture.

## System Requirements

### Hardware Requirements

- **CPU**: ARM64 architecture (Apple Silicon M1/M2/M3 or ARM64 servers)
- **Memory**: Minimum 1GB RAM (2GB+ recommended)
- **Storage**: Minimum 2GB available space

### Software Requirements

- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **Operating System**: macOS (Apple Silicon), Linux ARM64

## Quick Start

### 1. Build Image

```bash
# Use the provided build script (recommended)
./docker/docker-build.sh

# Or build manually
docker build --platform linux/arm64 -t genai-demo:latest .
```

### 2. Start Services

```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f genai-demo
```

### 3. Access Application

- **API Documentation**: <http://localhost:8080/swagger-ui/index.html>
- **Health Check**: <http://localhost:8080/actuator/health>
- **H2 Database Console**: <http://localhost:8080/h2-console>

## Image Optimization Features

### 1. ARM64 Native Support

- Uses ARM64 native base images
- Optimized for Apple Silicon and ARM64 servers
- Avoids performance loss from x86 emulation

### 2. Multi-stage Build

```dockerfile
# Build stage - includes complete Gradle and JDK
FROM gradle:8.5-jdk21-alpine AS builder

# Runtime stage - includes only JRE and application
FROM eclipse-temurin:21-jre-alpine
```

### 3. JVM Optimization

```bash
# JVM parameters optimized for container environment
JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseSerialGC \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0"
```

### 4. Security Enhancements

- Run application with non-root user
- Minimal base image (Alpine Linux)
- Remove unnecessary packages and tools

## Configuration

### Environment Variables

| Variable Name | Default Value | Description |
|---------------|---------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `docker` | Spring Boot profile |
| `JAVA_OPTS` | See above | JVM startup parameters |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:genaidemo` | Database connection URL |

### Data Persistence

```yaml
# Volume configuration in docker-compose.yml
volumes:
  - ./logs:/app/logs  # Log persistence
```

### Health Check

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

## Troubleshooting

### 1. Image Build Failure

**Issue**: Architecture mismatch errors during build

```bash
# Solution: Explicitly specify platform
docker build --platform linux/arm64 -t genai-demo:latest .
```

### 2. Container Startup Failure

**Issue**: Out of memory

```bash
# Solution: Adjust JVM memory parameters
export JAVA_OPTS="-Xms128m -Xmx256m"
docker-compose up -d
```

### 3. Health Check Failure

**Issue**: Application takes too long to start

```bash
# Solution: Increase startup wait time
# Adjust start_period in docker-compose.yml
healthcheck:
  start_period: 120s  # Increase to 2 minutes
```

### 4. Log Viewing

```bash
# View application logs
docker-compose logs -f genai-demo

# View logs for specific time range
docker-compose logs --since="2024-01-01T00:00:00" genai-demo

# View last 100 lines of logs
docker-compose logs --tail=100 genai-demo
```

## Performance Tuning

### 1. JVM Memory Adjustment

Adjust JVM parameters based on available memory:

```bash
# 1GB RAM system
export JAVA_OPTS="-Xms128m -Xmx256m"

# 2GB RAM system
export JAVA_OPTS="-Xms256m -Xmx512m"

# 4GB+ RAM system
export JAVA_OPTS="-Xms512m -Xmx1024m"
```

### 2. Garbage Collector Selection

```bash
# Low memory environment (< 1GB)
-XX:+UseSerialGC

# Medium memory environment (1-4GB)
-XX:+UseG1GC

# High memory environment (4GB+)
-XX:+UseZGC  # Java 17+
```

### 3. Startup Time Optimization

```bash
# Fast startup parameters
-XX:+TieredCompilation \
-XX:TieredStopAtLevel=1 \
-Dspring.main.lazy-initialization=true \
-Dspring.jmx.enabled=false
```

## Production Deployment

### 1. Security Checklist

- [ ] Run with non-root user
- [ ] Remove development tools and debug endpoints
- [ ] Set appropriate resource limits
- [ ] Enable log rotation
- [ ] Configure monitoring and alerting

### 2. Resource Limits

```yaml
# docker-compose.yml
services:
  genai-demo:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. Log Management

```yaml
# Log rotation configuration
logging:
  driver: "json-file"
  options:
    max-size: "100m"
    max-file: "3"
```

## Multi-Environment Configuration

### Development Environment

```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  genai-demo:
    build:
      context: .
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JAVA_OPTS=-Xms128m -Xmx256m
    ports:
      - "8080:8080"
    volumes:
      - ./logs:/app/logs
```

### Test Environment

```yaml
# docker-compose.test.yml
version: '3.8'
services:
  genai-demo:
    image: genai-demo:test
    environment:
      - SPRING_PROFILES_ACTIVE=test
      - JAVA_OPTS=-Xms256m -Xmx512m
    depends_on:
      - postgres
      - redis
```

### Production Environment

```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  genai-demo:
    image: genai-demo:latest
    environment:
      - SPRING_PROFILES_ACTIVE=msk
      - JAVA_OPTS=-Xms512m -Xmx1024m
    deploy:
      replicas: 3
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
```

## Container Monitoring

### Health Check Configuration

```dockerfile
# Health check in Dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

### Monitoring Metrics

```bash
# View container resource usage
docker stats genai-demo

# View container details
docker inspect genai-demo

# View container logs
docker logs -f genai-demo
```

## Related Diagrams

- [Container Architecture Diagram](../../diagrams/deployment/container-architecture.puml)
- [Docker Deployment Flow](../../diagrams/deployment/docker-deployment-flow.puml)

## Relationships with Other Viewpoints

- **[Development Viewpoint](../development/README.md)**: Build process and CI/CD integration
- **[Operational Viewpoint](../operational/README.md)**: Container monitoring and log management
- **[Security Perspective](../../perspectives/security/README.md)**: Container security and image scanning

## Related Documentation

- [README.md](../../../README.md) - Project overview
- [Observability Deployment](observability-deployment.md) - Monitoring system deployment
- [Production Deployment Checklist](production-deployment-checklist.md) - Production environment checklist
