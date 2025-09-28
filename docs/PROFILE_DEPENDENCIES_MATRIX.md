# Profile Dependencies Matrix

## 📊 **Complete Service Dependencies Matrix**

### **Service Dependencies Overview**

| Service Type | Local | Test | Staging | Production |
|----------|-------|------|---------|------------|
| **Database** | H2 In-Memory | H2 In-Memory | PostgreSQL (RDS) | PostgreSQL (RDS) |
| **Cache/Distributed Lock** | Redis Single/Sentinel | Disabled | ElastiCache/EKS Redis | ElastiCache Cluster |
| **Message Queue** | Disabled | Disabled | MSK (Kafka) | MSK (Kafka) |
| **Event Store** | In-Memory | In-Memory | Kafka Topics | Kafka Topics |
| **Tracing System** | Disabled | Disabled | AWS X-Ray + OTLP | AWS X-Ray + OTLP |
| **Monitoring Metrics** | Basic | Disabled | CloudWatch + Prometheus | CloudWatch + Prometheus |
| **Logging System** | Console | Console | CloudWatch Logs | CloudWatch Logs |
| **Configuration Management** | Local Files | Local Files | K8s ConfigMap/Secret | K8s ConfigMap/Secret |
| **Service Discovery** | None | None | Kubernetes DNS | Kubernetes DNS |
| **Load Balancing** | None | None | ALB/NLB | ALB/NLB |
| **Security Authentication** | Relaxed | Disabled | IAM + IRSA | IAM + IRSA |

## 🔧 **Detailed Configuration Analysis**

### **1. Local Profile (Local Development + Testing)**

#### **Database Layer**
```yaml
Database: H2 In-Memory Database
├── Driver: org.h2.Driver
├── URL: jdbc:h2:mem:genaidemo
├── Schema: create-drop (rebuild on each restart)
├── Console: Enabled (http://localhost:8080/h2-console)
├── Migration: Flyway disabled (avoid circular dependencies)
└── Connection Pool: HikariCP (10 max, 2 min)
```

#### **Cache Layer**
```yaml
Redis: Optional Configuration
├── Mode: SINGLE (default) / SENTINEL (HA testing)
├── Host: localhost:6379
├── Database: 1 (development dedicated)
├── Pool Size: 20 connections
├── Timeout: 2s (fast feedback)
├── Sentinel Nodes: localhost:26379,26380,26381
└── Failover: Optional enabled (for testing)
```

#### **Event System**
```yaml
Event Publisher: In-Memory Implementation
├── Type: InMemoryDomainEventPublisher
├── Async: false (synchronous processing for debugging)
├── Storage: In-memory cache
├── Persistence: None
└── Replay: Not supported
```

#### **Monitoring System**
```yaml
Observability: Minimized
├── Tracing: Disabled
├── Metrics: Basic JVM metrics
├── Logging: Verbose (DEBUG level)
├── Health Checks: Basic
└── Analytics: Disabled
```

### **2. Test Profile (CI/CD Testing)**

#### **Database Layer**
```yaml
Database: H2 In-Memory Database (Minimized)
├── Driver: org.h2.Driver
├── URL: jdbc:h2:mem:testdb
├── Schema: create-drop
├── Console: Disabled
├── Migration: Disabled (fast startup)
└── Connection Pool: 5 max, 1 min (minimal resources)
```

#### **External Dependencies**
```yaml
External Services: All Disabled
├── Redis: Disabled
├── Kafka: Disabled
├── Tracing: Disabled
├── Metrics Export: Disabled
└── Analytics: Disabled
```

#### **Event System**
```yaml
Event Publisher: In-Memory Implementation
├── Type: InMemoryDomainEventPublisher
├── Async: false
├── Storage: In-memory
└── Cleanup: Automatic cleanup
```

### **3. Staging Profile (AWS Pre-production)**

#### **Database Layer**
```yaml
Database: PostgreSQL (Amazon RDS)
├── Driver: org.postgresql.Driver
├── URL: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
├── Schema: validate (no automatic modifications)
├── Migration: Flyway enabled (PostgreSQL scripts)
├── Connection Pool: 20 max, 5 min
├── Batch Size: 20
└── Dialect: PostgreSQLDialect
```

#### **Cache Layer**
```yaml
Redis: ElastiCache or EKS Redis
├── Mode: CLUSTER (ElastiCache) / SENTINEL (EKS)
├── Nodes: ${REDIS_CLUSTER_NODES} or Sentinel nodes
├── Database: 0 (cluster mode doesn't support database selection)
├── Pool Size: 50 connections (25 master + 25 slave)
├── Timeout: 3s
├── Failover: Enabled (45s timeout)
├── Health Check: 45s interval
└── Security: AUTH token support
```

#### **Message Queue**
```yaml
Kafka: Amazon MSK
├── Bootstrap Servers: ${KAFKA_BOOTSTRAP_SERVERS}
├── Security: SASL_SSL + IAM
├── Producer: 
│   ├── Acks: all
│   ├── Retries: 3
│   ├── Batch Size: 16KB
│   └── Compression: snappy
├── Consumer:
│   ├── Group ID: genai-demo-k8s
│   ├── Auto Offset Reset: earliest
│   ├── Max Poll Records: 500
│   └── Enable Auto Commit: false
└── Topics: customer, order, payment, inventory
```

#### **Event System**
```yaml
Event Publisher: Kafka Implementation
├── Type: KafkaDomainEventPublisher
├── Async: true
├── Storage: MSK Topics
├── Persistence: Persistent
├── Replay: Supported
└── Dead Letter Queue: Supported
```

#### **Monitoring System**
```yaml
Observability: Complete Monitoring
├── Tracing: AWS X-Ray + OTLP
│   ├── Sampling Rate: 0.1 (10%)
│   ├── Exporter: OTLP (adot-collector:4317)
│   └── Plugins: EC2Plugin, EKSPlugin
├── Metrics:
│   ├── Prometheus: Enabled (30s interval)
│   ├── CloudWatch: Enabled (GenAIDemo/K8s namespace)
│   └── Custom Tags: environment, cluster, namespace, pod
├── Logging:
│   ├── Level: INFO
│   ├── Format: Structured JSON
│   └── Correlation: traceId, spanId, correlationId
└── Health Checks: Kubernetes probes enabled
```

### **4. Production Profile (AWS Production)**

#### **Database Layer**
```yaml
Database: PostgreSQL (Amazon RDS Multi-AZ)
├── Driver: org.postgresql.Driver
├── Schema: validate (strict validation)
├── Migration: Flyway enabled (production scripts)
├── Connection Pool: Larger pool size
├── Batch Size: 25 (performance optimization)
├── Fetch Size: 100
├── Second Level Cache: Enabled (JCache)
└── Query Cache: Enabled
```

#### **Cache Layer**
```yaml
Redis: ElastiCache Cluster Mode
├── Mode: CLUSTER (mandatory)
├── Nodes: ${REDIS_CLUSTER_NODES} (multi-node)
├── Pool Size: 100 connections (50 master + 50 slave)
├── Timeout: 5s (production network latency)
├── Failover: Enabled (60s timeout)
├── Health Check: 60s interval (reduce overhead)
├── Security: AUTH token + encrypted transmission
├── Read Scaling: Enable slave reads
└── Max Redirections: 10 (large cluster)
```

#### **Message Queue**
```yaml
Kafka: Amazon MSK (Production-grade Configuration)
├── Bootstrap Servers: Multiple brokers
├── Security: SASL_SSL + IAM + encryption
├── Producer:
│   ├── Acks: all (highest reliability)
│   ├── Retries: Infinite retries
│   ├── Idempotence: Enabled (exactly-once)
│   ├── Compression: snappy
│   └── Buffer Memory: 32MB
├── Consumer:
│   ├── Isolation Level: read_committed
│   ├── Session Timeout: 30s
│   └── Max Poll Interval: 5 minutes
└── Topics: Production-grade partitions and replication
```

#### **Event System**
```yaml
Event Publisher: Kafka Implementation (Production-grade)
├── Type: KafkaDomainEventPublisher
├── Async: true
├── Storage: MSK Topics (persistent)
├── Persistence: High availability
├── Replay: Full support
├── Dead Letter Queue: Complete error handling
├── Monitoring: Complete metrics
└── Alerting: Production-grade alerts
```

#### **Monitoring System**
```yaml
Observability: Enterprise-grade Monitoring
├── Tracing: AWS X-Ray (Production-grade)
│   ├── Sampling: Intelligent sampling
│   ├── Business Metrics: 100% sampling
│   ├── Infrastructure: 50% sampling
│   └── JVM Metrics: 10% sampling
├── Metrics:
│   ├── CloudWatch: Complete metrics (GenAIDemo-Prod)
│   ├── Custom Metrics: Business metrics
│   ├── Retention: 30 days
│   └── Cardinality: 50,000 limit
├── Logging:
│   ├── Level: WARN/INFO (performance optimization)
│   ├── Format: Structured JSON
│   ├── Retention: Optimization strategy
│   └── High Volume Threshold: 5000
├── Analytics:
│   ├── Real-time: WebSocket support
│   ├── Cost Optimization: Enabled
│   ├── Right-sizing: Enabled
│   └── Performance Analysis: Enabled
└── Alerting: Complete alerting strategy
```

## 🔄 **Inter-service Dependencies**

### **Local Profile Dependency Graph**
```
Application
├── H2 Database (embedded)
├── Redis (optional, Docker)
├── File System (logs, temp files)
└── JVM (metrics, health checks)
```

### **Staging Profile Dependency Graph**
```
Application (EKS Pod)
├── PostgreSQL (RDS)
├── Redis (ElastiCache/EKS)
├── Kafka (MSK)
├── X-Ray Daemon (sidecar)
├── ADOT Collector (DaemonSet)
├── CloudWatch (metrics/logs)
├── Prometheus (metrics)
└── Kubernetes API (service discovery)
```

### **Production Profile Dependency Graph**
```
Application (EKS Pod)
├── PostgreSQL (RDS Multi-AZ)
├── Redis (ElastiCache Cluster)
├── Kafka (MSK Multi-AZ)
├── X-Ray Daemon (sidecar)
├── ADOT Collector (DaemonSet)
├── CloudWatch (metrics/logs/alarms)
├── Prometheus (metrics)
├── ALB/NLB (load balancing)
├── IAM (authentication/authorization)
├── KMS (encryption)
├── Secrets Manager (secrets)
└── Route 53 (DNS/health checks)
```

## 📋 **Service Dependencies List**

### **Required Dependencies (All Profiles)**
- JVM Runtime
- Spring Boot Framework
- Logging Framework (Logback)

### **Local Profile Additional Dependencies**
- H2 Database (embedded)
- Redis (Docker, optional)

### **Test Profile Additional Dependencies**
- H2 Database (embedded)
- JUnit 5 Test Framework

### **Staging Profile Additional Dependencies**
- PostgreSQL (RDS)
- Redis (ElastiCache/EKS)
- Kafka (MSK)
- AWS X-Ray
- ADOT Collector
- Kubernetes
- CloudWatch
- Prometheus

### **Production Profile Additional Dependencies**
- PostgreSQL (RDS Multi-AZ)
- Redis (ElastiCache Cluster)
- Kafka (MSK Multi-AZ)
- AWS X-Ray
- ADOT Collector
- Kubernetes (EKS)
- CloudWatch (complete suite)
- Prometheus
- ALB/NLB
- IAM/IRSA
- KMS
- Secrets Manager
- Route 53

---

**Updated**: September 27, 2025 5:50 PM (Taipei Time)  
**Maintainer**: Development Team  
**Version**: 2.0.0