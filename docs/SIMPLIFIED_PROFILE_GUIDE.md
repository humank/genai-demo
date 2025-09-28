# Simplified Profile Architecture Guide

## 🎯 **Practical 3-Profile Design**

Based on actual workflow requirements, we adopt a simplified but practical profile architecture:

### **Profile Architecture Overview**

| Profile | Environment | Purpose | Database | Redis | Kafka | Deployment Location |
|---------|-------------|---------|----------|-------|-------|-------------------|
| `local` | Local | Development+Testing | H2 | Single/Sentinel | Disabled | Local Machine |
| `staging` | AWS | Integration Testing | RDS | ElastiCache | MSK | AWS Tokyo |
| `production` | AWS | Production | RDS | ElastiCache Cluster | MSK | AWS Tokyo |

## 🚀 **Actual Workflow**

### **1. Local Development Phase**
```bash
# Daily development
export SPRING_PROFILES_ACTIVE=local
./gradlew bootRun

# Local testing (using same profile)
./gradlew test  # Automatically uses test profile (minimized configuration)
```

**Features:**
- H2 in-memory database (fast restart)
- Optional Redis (single or HA testing)
- In-memory event processing
- Complete development tools (H2 Console, OpenAPI)

### **2. AWS Staging Phase**
```bash
# Deploy to AWS for integration testing
export SPRING_PROFILES_ACTIVE=staging
```

**Features:**
- Real AWS services (RDS, ElastiCache, MSK)
- Complete monitoring and tracing
- Full production environment simulation
- UAT and integration testing

### **3. AWS Production Phase**
```bash
# Production deployment
export SPRING_PROFILES_ACTIVE=production
```

**Features:**
- High availability configuration
- Complete security settings
- Production-grade monitoring and alerting
- Disaster recovery mechanisms

## 💡 **Why This Design?**

### **Reasons for Merging Development + Test**

1. **Actual Workflow**: You perform both development and testing locally
2. **Resource Efficiency**: Avoid maintaining multiple similar configurations
3. **Simplified Management**: Reduce complexity of profile switching
4. **Fast Feedback**: Unified local environment makes issues easier to reproduce

### **Reasons for Keeping Test Profile**

1. **CI/CD Requirements**: Automated testing needs minimized configuration
2. **Isolation**: Tests should not depend on external services
3. **Speed**: Test environment needs fast startup and shutdown

## 🔧 **Configuration File Structure**

```
app/src/main/resources/
├── application.yml              # Base configuration
├── application-local.yml        # Local development+testing
├── application-staging.yml      # AWS pre-production
├── application-production.yml   # AWS production
├── application-msk.yml         # MSK-specific configuration
└── application-openapi.yml     # OpenAPI configuration

app/src/test/resources/
└── application-test.yml        # CI/CD testing only (minimized)
```

## 🎮 **Usage Examples**

### **Local Development**
```bash
# Start Redis
./scripts/redis-dev.sh start-single

# Set environment
export SPRING_PROFILES_ACTIVE=local
export REDIS_MODE=SINGLE

# Start application
./gradlew bootRun

# Run tests
./gradlew test  # Automatically uses test profile
```

### **Redis HA Testing**
```bash
# Start HA environment
./scripts/redis-dev.sh start-ha

# Set environment
export SPRING_PROFILES_ACTIVE=local
export REDIS_MODE=SENTINEL
export REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381

# Start application and test failover
./gradlew bootRun
```

### **AWS Deployment**
```bash
# Staging deployment
export SPRING_PROFILES_ACTIVE=staging
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=your-staging-cluster

# Production deployment
export SPRING_PROFILES_ACTIVE=production
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=your-production-cluster
```

## 🔍 **Profile Selection Decision Tree**

```
Where are you?
├── Local Machine
│   ├── Developing new features → local
│   ├── Local testing → local
│   └── Unit testing → test (automatic)
├── AWS Environment
│   ├── Integration testing → staging
│   ├── UAT testing → staging
│   └── Production deployment → production
```

## 📊 **Profile Comparison**

| Feature | local | test | staging | production |
|---------|-------|------|---------|------------|
| Database | H2 | H2 | RDS | RDS |
| Redis | Optional | Disabled | ElastiCache | ElastiCache Cluster |
| Kafka | Disabled | Disabled | MSK | MSK |
| Monitoring | Basic | Disabled | Complete | Complete |
| Security | Relaxed | Disabled | Strict | Strictest |
| Startup Speed | Fast | Fastest | Medium | Slow |

## 🎯 **Best Practices**

### **Local Development**
- Use `local` profile for all local work
- Enable/disable Redis as needed
- Utilize H2 Console for database debugging

### **Testing Strategy**
- Unit tests automatically use `test` profile
- Integration tests can use `local` or `staging`
- E2E tests recommended to use `staging`

### **Deployment Strategy**
- Deploy to `staging` first for validation
- Deploy to `production` after passing all tests
- Use environment variables to manage different environment configurations

---

**Updated**: September 27, 2025 5:50 PM (Taipei Time)  
**Maintainer**: Development Team  
**Version**: 2.0.0 (Simplified)