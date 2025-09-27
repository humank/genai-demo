# Spring Boot Profile Usage Guide

## 📋 **Standardized Profile Architecture**

### **Profile Naming Standards**

Following Spring Boot best practices, we use the following standard profiles:

| Profile | Purpose | Environment | Database | Redis | Kafka/MSK |
|---------|---------|-------------|----------|-------|-----------|
| `development` | Local Development | Local | H2 | Single/Sentinel | Disabled |
| `test` | Automated Testing | CI/CD | H2 | Disabled | Disabled |
| `staging` | Pre-production | Kubernetes | PostgreSQL | ElastiCache/EKS | MSK |
| `production` | Production | AWS | PostgreSQL | ElastiCache Cluster | MSK |

### **Profile Composition Strategy**

Spring Boot supports profile composition, our configuration is as follows:

```yaml
spring:
  profiles:
    group:
      development: \"development,openapi\"
      test: \"test,openapi\"
      staging: \"staging,openapi,msk\"
      production: \"production,openapi,msk\"
```

## 🚀 **Usage Instructions**

### **1. Local Development Environment**

```bash
# Basic development environment
export SPRING_PROFILES_ACTIVE=development
./gradlew bootRun

# Or specify directly
./gradlew bootRun --args='--spring.profiles.active=development'
```

**Features:**

- H2 in-memory database
- Single Redis instance
- In-memory event processing
- OpenAPI documentation enabled
- Detailed logging output

### **2. Test Environment**

```bash
# Run tests
./gradlew test

# Manually specify test profile
./gradlew test -Dspring.profiles.active=test
```

**Features:**

- H2 in-memory database
- Redis completely disabled
- In-memory event processing
- Minimal logging output
- Fast startup

### **3. Staging Environment**

```bash
# Kubernetes deployment
export SPRING_PROFILES_ACTIVE=staging
# Used with Kubernetes ConfigMap and Secret
```

**Features:**

- PostgreSQL database
- ElastiCache or EKS Redis
- MSK Kafka integration
- AWS X-Ray tracing
- Production-grade monitoring

### **4. Production Environment**

```bash
# Production deployment
export SPRING_PROFILES_ACTIVE=production
```

**Features:**

- PostgreSQL database
- ElastiCache Cluster
- MSK Kafka integration
- Complete observability
- Enhanced security

## 🔧 **Redis Configuration Strategy**

### **Development Profile**

```bash
# Single Redis (default)
export REDIS_MODE=SINGLE
./scripts/redis-dev.sh start-single

# HA testing
export REDIS_MODE=SENTINEL
export REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381
./scripts/redis-dev.sh start-ha
```

### **Staging/Production Profile**

```bash
# ElastiCache Cluster
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=your-cluster-endpoint

# EKS Redis Sentinel
export REDIS_MODE=SENTINEL
export REDIS_SENTINEL_NODES=sentinel-1:26379,sentinel-2:26379,sentinel-3:26379
```

## 📁 **Configuration File Structure**

```text
app/src/main/resources/
├── application.yml                 # Base configuration
├── application-development.yml     # Development environment
├── application-test.yml           # Test environment
├── application-staging.yml        # Staging environment
├── application-production.yml     # Production environment
├── application-msk.yml           # MSK-specific configuration
└── application-openapi.yml       # OpenAPI configuration

app/src/test/resources/
└── application-test.yml          # Test-specific configuration
```

## 🎯 **Best Practices**

### **1. Profile Selection Principles**

- **Development Phase**: Use `development`
- **Unit Testing**: Automatically uses `test`
- **Integration Testing**: Use `staging` or `test`
- **Production Deployment**: Use `production`

### **2. Environment Variable Management**

```bash
# .env file (local development)
SPRING_PROFILES_ACTIVE=development
REDIS_MODE=SINGLE

# Kubernetes ConfigMap (staging/production)
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  SPRING_PROFILES_ACTIVE: \"staging\"
  REDIS_MODE: \"CLUSTER\"
```

### **3. Conditional Bean Configuration**

```java
@Component
@Profile(\"development\")
public class DevelopmentService {
    // Only enabled in development environment
}

@Component
@Profile({\"staging\", \"production\"})
public class ProductionService {
    // Only enabled in staging and production environments
}
```

## 🔍 **Troubleshooting**

### **Common Issues**

#### 1. Profile Not Loading Correctly

```bash
# Check current profile
curl http://localhost:8080/actuator/env | jq '.activeProfiles'

# Or check logs
grep \"The following profiles are active\" logs/application.log
```

#### 2. Redis Connection Failure

```bash
# Check Redis status
./scripts/redis-dev.sh status

# Test connection
./scripts/redis-dev.sh test
```

#### 3. Configuration Conflicts

```bash
# Check configuration properties
curl http://localhost:8080/actuator/configprops
```

## 📊 **Profile Validation Checklist**

### **Development Environment Check**

- [ ] H2 Console accessible: <http://localhost:8080/h2-console>
- [ ] Redis connection normal
- [ ] OpenAPI documentation available: <http://localhost:8080/swagger-ui.html>
- [ ] Health check passes: <http://localhost:8080/actuator/health>

### **Test Environment Check**

- [ ] All tests pass
- [ ] No external dependencies
- [ ] Fast startup (< 30 seconds)
- [ ] Reasonable memory usage

### **Production Environment Check**

- [ ] Database connection normal
- [ ] Redis Cluster connection normal
- [ ] MSK Kafka connection normal
- [ ] Monitoring metrics normal
- [ ] Security configuration enabled

---

**Last Updated**: September 24, 2025 8:40 AM (Taipei Time)  
**Maintainer**: Development Team  
**Version**: 2.0.0
