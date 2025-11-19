# Environment Configuration Guide

## Overview

This document describes the configuration for each environment in the Enterprise E-Commerce Platform.

## Environment Overview

| Environment | Purpose | URL | AWS Account | Region |
|-------------|---------|-----|-------------|--------|
| Local | Development | localhost | N/A | N/A |
| Staging | Pre-production testing | staging.ecommerce.example.com | 123456789012 | ap-northeast-1 |
| Production | Live customer-facing | api.ecommerce.example.com | 987654321098 | ap-northeast-1 |

## Local Environment

### Purpose

- Individual developer workstation
- Rapid development and testing
- Unit and integration testing

### Infrastructure

- **Database**: H2 in-memory or PostgreSQL Docker container
- **Cache**: Redis Docker container
- **Message Queue**: Kafka Docker container (optional)
- **Object Storage**: LocalStack S3 (optional)

### Configuration

```yaml
# application-local.yml
spring:
  profiles:
    active: local
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  redis:
    host: localhost
    port: 6379
  
  kafka:
    bootstrap-servers: localhost:9092

server:
  port: 8080

logging:
  level:
    root: INFO
    solid.humank.genaidemo: DEBUG
```

### Environment Variables

```bash
# .env.local
SPRING_PROFILES_ACTIVE=local
DATABASE_URL=jdbc:h2:mem:testdb
REDIS_HOST=localhost
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
AWS_REGION=ap-northeast-1
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
```

### Setup Instructions

```bash
# Start local infrastructure
docker-compose -f docker-compose-local.yml up -d

# Run application
./gradlew bootRun --args='--spring.profiles.active=local'

# Run with specific port
./gradlew bootRun --args='--spring.profiles.active=local --server.port=8081'
```

## Staging Environment

### Purpose

- Pre-production testing
- Integration testing
- Performance testing
- User acceptance testing (UAT)

### Infrastructure

- **Compute**: EKS cluster (3 nodes, t3.medium)
- **Database**: RDS PostgreSQL (db.t3.medium, Multi-AZ)
- **Cache**: ElastiCache Redis (cache.t3.medium, 2 nodes)
- **Message Queue**: MSK Kafka (kafka.t3.small, 3 brokers)
- **Object Storage**: S3 bucket
- **Load Balancer**: Application Load Balancer

### Configuration

```yaml
# application-staging.yml
spring:
  profiles:
    active: staging
  
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  
  redis:
    host: ${REDIS_HOST}
    port: 6379
    password: ${REDIS_PASSWORD}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

server:
  port: 8080

logging:
  level:
    root: INFO
    solid.humank.genaidemo: DEBUG

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

### Environment Variables

```bash
# Stored in AWS Secrets Manager: staging/ecommerce/config
SPRING_PROFILES_ACTIVE=staging
DB_HOST=ecommerce-staging.cluster-xxxxx.ap-northeast-1.rds.amazonaws.com
DB_NAME=ecommerce_staging
DB_USERNAME=ecommerce_app
DB_PASSWORD=<stored-in-secrets-manager>
REDIS_HOST=ecommerce-staging.xxxxx.cache.amazonaws.com
REDIS_PASSWORD=<stored-in-secrets-manager>
KAFKA_BOOTSTRAP_SERVERS=b-1.staging.xxxxx.kafka.ap-northeast-1.amazonaws.com:9092
AWS_REGION=ap-northeast-1
S3_BUCKET=ecommerce-staging-assets
JWT_SECRET=<stored-in-secrets-manager>
```

### Resource Specifications

```yaml
# Kubernetes resource limits
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"

# Horizontal Pod Autoscaler
minReplicas: 2
maxReplicas: 5
targetCPUUtilizationPercentage: 70
```

### Access

```bash
# Configure kubectl for staging
aws eks update-kubeconfig \
  --name ecommerce-staging \
  --region ap-northeast-1 \
  --profile staging

# Verify access
kubectl get nodes
kubectl get pods -n staging
```

## Production Environment

### Purpose

- Live customer-facing application
- High availability and performance
- 24/7 operation

### Infrastructure

- **Compute**: EKS cluster (6 nodes, t3.large)
- **Database**: RDS PostgreSQL (db.r5.xlarge, Multi-AZ, Read Replicas)
- **Cache**: ElastiCache Redis (cache.r5.large, 3 nodes, cluster mode)
- **Message Queue**: MSK Kafka (kafka.m5.large, 3 brokers)
- **Object Storage**: S3 bucket with CloudFront CDN
- **Load Balancer**: Application Load Balancer with WAF

### Configuration

```yaml
# application-production.yml
spring:
  profiles:
    active: production
  
  datasource:
    url: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
  
  redis:
    host: ${REDIS_HOST}
    port: 6379
    password: ${REDIS_PASSWORD}
    cluster:
      nodes: ${REDIS_CLUSTER_NODES}
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
    producer:
      acks: all
      retries: 3
    consumer:
      enable-auto-commit: false

server:
  port: 8080
  compression:
    enabled: true

logging:
  level:
    root: WARN
    solid.humank.genaidemo: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: ECommerce/Production
```

### Environment Variables

```bash
# Stored in AWS Secrets Manager: production/ecommerce/config
SPRING_PROFILES_ACTIVE=production
DB_HOST=ecommerce-prod.cluster-xxxxx.ap-northeast-1.rds.amazonaws.com
DB_NAME=ecommerce_production
DB_USERNAME=ecommerce_app
DB_PASSWORD=<stored-in-secrets-manager>
REDIS_HOST=ecommerce-prod.xxxxx.cache.amazonaws.com
REDIS_PASSWORD=<stored-in-secrets-manager>
REDIS_CLUSTER_NODES=node1:6379,node2:6379,node3:6379
KAFKA_BOOTSTRAP_SERVERS=b-1.prod.xxxxx.kafka.ap-northeast-1.amazonaws.com:9092
AWS_REGION=ap-northeast-1
S3_BUCKET=ecommerce-production-assets
CLOUDFRONT_DISTRIBUTION_ID=E1234567890ABC
JWT_SECRET=<stored-in-secrets-manager>
ENCRYPTION_KEY=<stored-in-secrets-manager>
```

### Resource Specifications

```yaml
# Kubernetes resource limits
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"

# Horizontal Pod Autoscaler
minReplicas: 4
maxReplicas: 20
targetCPUUtilizationPercentage: 60
targetMemoryUtilizationPercentage: 70
```

### Access

```bash
# Configure kubectl for production
aws eks update-kubeconfig \
  --name ecommerce-production \
  --region ap-northeast-1 \
  --profile production

# Verify access
kubectl get nodes
kubectl get pods -n production
```

## Configuration Management

### Secrets Management

All sensitive configuration is stored in AWS Secrets Manager:

```bash
# Retrieve secrets
aws secretsmanager get-secret-value \
  --secret-id ${ENV}/ecommerce/config \
  --region ap-northeast-1

# Update secrets
aws secretsmanager update-secret \
  --secret-id ${ENV}/ecommerce/config \
  --secret-string file://secrets.json
```

### Configuration Hierarchy

1. **Default Configuration**: `application.yml`
2. **Environment-Specific**: `application-${ENV}.yml`
3. **Secrets Manager**: Runtime secrets
4. **Environment Variables**: Override specific values
5. **Command Line Arguments**: Highest priority

### Configuration Validation

```bash
# Validate configuration
./gradlew bootRun --args='--spring.profiles.active=${ENV} --spring.config.validate=true'

# Check active configuration
curl http://localhost:8080/actuator/env
```

## Environment-Specific Features

### Feature Flags

```yaml
# Feature flags per environment
features:
  new-checkout-flow:
    local: true
    staging: true
    production: false
  
  advanced-analytics:
    local: false
    staging: true
    production: true
```

### Rate Limiting

| Environment | Requests/Minute | Burst |
|-------------|-----------------|-------|
| Local | Unlimited | N/A |
| Staging | 1000 | 100 |
| Production | 10000 | 1000 |

### Cache TTL

| Cache Type | Local | Staging | Production |
|------------|-------|---------|------------|
| Product Catalog | 5 min | 15 min | 30 min |
| Customer Session | 30 min | 1 hour | 2 hours |
| API Response | 1 min | 5 min | 10 min |

## Monitoring Configuration

### CloudWatch Metrics

```yaml
# Production metrics
metrics:

  - name: APIResponseTime

    namespace: ECommerce/Production
    dimensions:

      - Environment: production
      - Service: backend
  
  - name: ErrorRate

    namespace: ECommerce/Production
    dimensions:

      - Environment: production
      - Service: backend

```

### Alarms

```yaml
# Production alarms
alarms:

  - name: HighErrorRate

    threshold: 5
    period: 300
    evaluation_periods: 2
    
  - name: HighResponseTime

    threshold: 2000
    period: 300
    evaluation_periods: 2
```

## Database Configuration

### Connection Pools

| Environment | Max Pool Size | Min Idle | Connection Timeout |
|-------------|---------------|----------|-------------------|
| Local | 5 | 2 | 10s |
| Staging | 10 | 5 | 20s |
| Production | 20 | 10 | 20s |

### Read Replicas

- **Local**: None
- **Staging**: None
- **Production**: 2 read replicas for read-heavy operations

## Backup Configuration

### Database Backups

| Environment | Frequency | Retention | Point-in-Time Recovery |
|-------------|-----------|-----------|------------------------|
| Local | None | N/A | No |
| Staging | Daily | 7 days | Yes (24 hours) |
| Production | Hourly | 30 days | Yes (7 days) |

### Application Backups

- **Configuration**: Versioned in Git
- **Secrets**: Backed up in Secrets Manager
- **Logs**: Retained in CloudWatch (30 days staging, 90 days production)

## Network Configuration

### VPC Configuration

```yaml
# Production VPC
VPC:
  CIDR: 10.0.0.0/16
  
  PublicSubnets:

    - 10.0.1.0/24  # AZ-1
    - 10.0.2.0/24  # AZ-2
    - 10.0.3.0/24  # AZ-3
  
  PrivateSubnets:

    - 10.0.11.0/24  # AZ-1
    - 10.0.12.0/24  # AZ-2
    - 10.0.13.0/24  # AZ-3
  
  DatabaseSubnets:

    - 10.0.21.0/24  # AZ-1
    - 10.0.22.0/24  # AZ-2
    - 10.0.23.0/24  # AZ-3

```

### Security Groups

- **ALB Security Group**: Allow 80, 443 from internet
- **Application Security Group**: Allow 8080 from ALB
- **Database Security Group**: Allow 5432 from application
- **Redis Security Group**: Allow 6379 from application
- **Kafka Security Group**: Allow 9092 from application

## Troubleshooting

### Configuration Issues

```bash
# Check active profile
curl http://localhost:8080/actuator/env | jq '.activeProfiles'

# Check property source
curl http://localhost:8080/actuator/env | jq '.propertySources'

# Validate database connection
kubectl exec -it ${POD_NAME} -- \
  psql -h ${DB_HOST} -U ${DB_USERNAME} -d ${DB_NAME} -c "SELECT 1"
```

### Common Issues

1. **Database Connection Timeout**
   - Check security group rules
   - Verify database endpoint
   - Check connection pool settings

2. **Redis Connection Failed**
   - Verify Redis endpoint
   - Check password configuration
   - Verify security group rules

3. **Kafka Connection Issues**
   - Check bootstrap servers
   - Verify security group rules
   - Check MSK cluster status


**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly
