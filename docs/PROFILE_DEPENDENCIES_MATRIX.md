# Profile 依賴服務對照表

## 📊 **完整的依賴服務矩陣**

### **服務依賴總覽**

| 服務類型 | Local | Test | Staging | Production |
|----------|-------|------|---------|------------|
| **資料庫** | H2 記憶體 | H2 記憶體 | PostgreSQL (RDS) | PostgreSQL (RDS) |
| **快取/分散式鎖** | Redis 單機/Sentinel | 禁用 | ElastiCache/EKS Redis | ElastiCache Cluster |
| **訊息佇列** | 禁用 | 禁用 | MSK (Kafka) | MSK (Kafka) |
| **事件儲存** | 記憶體 | 記憶體 | Kafka Topics | Kafka Topics |
| **追蹤系統** | 禁用 | 禁用 | AWS X-Ray + OTLP | AWS X-Ray + OTLP |
| **監控指標** | 基本 | 禁用 | CloudWatch + Prometheus | CloudWatch + Prometheus |
| **日誌系統** | Console | Console | CloudWatch Logs | CloudWatch Logs |
| **配置管理** | 本機檔案 | 本機檔案 | K8s ConfigMap/Secret | K8s ConfigMap/Secret |
| **服務發現** | 無 | 無 | Kubernetes DNS | Kubernetes DNS |
| **負載均衡** | 無 | 無 | ALB/NLB | ALB/NLB |
| **安全認證** | 寬鬆 | 禁用 | IAM + IRSA | IAM + IRSA |

## 🔧 **詳細配置分析**

### **1. Local Profile (本機開發+測試)**

#### **資料庫層**
```yaml
Database: H2 記憶體資料庫
├── Driver: org.h2.Driver
├── URL: jdbc:h2:mem:genaidemo
├── Schema: create-drop (每次重啟重建)
├── Console: 啟用 (http://localhost:8080/h2-console)
├── Migration: Flyway 禁用 (避免循環依賴)
└── Connection Pool: HikariCP (10 max, 2 min)
```

#### **快取層**
```yaml
Redis: 可選配置
├── Mode: SINGLE (預設) / SENTINEL (HA 測試)
├── Host: localhost:6379
├── Database: 1 (開發專用)
├── Pool Size: 20 connections
├── Timeout: 2s (快速反饋)
├── Sentinel Nodes: localhost:26379,26380,26381
└── Failover: 可選啟用 (測試用)
```

#### **事件系統**
```yaml
Event Publisher: 記憶體實作
├── Type: InMemoryDomainEventPublisher
├── Async: false (同步處理，便於除錯)
├── Storage: 記憶體暫存
├── Persistence: 無
└── Replay: 不支援
```

#### **監控系統**
```yaml
Observability: 最小化
├── Tracing: 禁用
├── Metrics: 基本 JVM 指標
├── Logging: 詳細 (DEBUG 級別)
├── Health Checks: 基本
└── Analytics: 禁用
```

### **2. Test Profile (CI/CD 測試)**

#### **資料庫層**
```yaml
Database: H2 記憶體資料庫 (最小化)
├── Driver: org.h2.Driver
├── URL: jdbc:h2:mem:testdb
├── Schema: create-drop
├── Console: 禁用
├── Migration: 禁用 (快速啟動)
└── Connection Pool: 5 max, 1 min (最小資源)
```

#### **外部依賴**
```yaml
External Services: 全部禁用
├── Redis: 禁用
├── Kafka: 禁用
├── Tracing: 禁用
├── Metrics Export: 禁用
└── Analytics: 禁用
```

#### **事件系統**
```yaml
Event Publisher: 記憶體實作
├── Type: InMemoryDomainEventPublisher
├── Async: false
├── Storage: 記憶體
└── Cleanup: 自動清理
```

### **3. Staging Profile (AWS 預發布)**

#### **資料庫層**
```yaml
Database: PostgreSQL (Amazon RDS)
├── Driver: org.postgresql.Driver
├── URL: jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}
├── Schema: validate (不自動修改)
├── Migration: Flyway 啟用 (PostgreSQL scripts)
├── Connection Pool: 20 max, 5 min
├── Batch Size: 20
└── Dialect: PostgreSQLDialect
```

#### **快取層**
```yaml
Redis: ElastiCache 或 EKS Redis
├── Mode: CLUSTER (ElastiCache) / SENTINEL (EKS)
├── Nodes: ${REDIS_CLUSTER_NODES} 或 Sentinel 節點
├── Database: 0 (cluster mode 不支援 database 選擇)
├── Pool Size: 50 connections (25 master + 25 slave)
├── Timeout: 3s
├── Failover: 啟用 (45s timeout)
├── Health Check: 45s interval
└── Security: AUTH token 支援
```

#### **訊息佇列**
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

#### **事件系統**
```yaml
Event Publisher: Kafka 實作
├── Type: KafkaDomainEventPublisher
├── Async: true
├── Storage: MSK Topics
├── Persistence: 持久化
├── Replay: 支援
└── Dead Letter Queue: 支援
```

#### **監控系統**
```yaml
Observability: 完整監控
├── Tracing: AWS X-Ray + OTLP
│   ├── Sampling Rate: 0.1 (10%)
│   ├── Exporter: OTLP (adot-collector:4317)
│   └── Plugins: EC2Plugin, EKSPlugin
├── Metrics:
│   ├── Prometheus: 啟用 (30s interval)
│   ├── CloudWatch: 啟用 (GenAIDemo/K8s namespace)
│   └── Custom Tags: environment, cluster, namespace, pod
├── Logging:
│   ├── Level: INFO
│   ├── Format: 結構化 JSON
│   └── Correlation: traceId, spanId, correlationId
└── Health Checks: Kubernetes probes 啟用
```

### **4. Production Profile (AWS 生產)**

#### **資料庫層**
```yaml
Database: PostgreSQL (Amazon RDS Multi-AZ)
├── Driver: org.postgresql.Driver
├── Schema: validate (嚴格驗證)
├── Migration: Flyway 啟用 (生產 scripts)
├── Connection Pool: 更大的 pool size
├── Batch Size: 25 (優化效能)
├── Fetch Size: 100
├── Second Level Cache: 啟用 (JCache)
└── Query Cache: 啟用
```

#### **快取層**
```yaml
Redis: ElastiCache Cluster Mode
├── Mode: CLUSTER (強制)
├── Nodes: ${REDIS_CLUSTER_NODES} (多節點)
├── Pool Size: 100 connections (50 master + 50 slave)
├── Timeout: 5s (生產網路延遲)
├── Failover: 啟用 (60s timeout)
├── Health Check: 60s interval (減少開銷)
├── Security: AUTH token + 加密傳輸
├── Read Scaling: 啟用 slave 讀取
└── Max Redirections: 10 (大型 cluster)
```

#### **訊息佇列**
```yaml
Kafka: Amazon MSK (生產級配置)
├── Bootstrap Servers: 多個 broker
├── Security: SASL_SSL + IAM + 加密
├── Producer:
│   ├── Acks: all (最高可靠性)
│   ├── Retries: 無限重試
│   ├── Idempotence: 啟用 (exactly-once)
│   ├── Compression: snappy
│   └── Buffer Memory: 32MB
├── Consumer:
│   ├── Isolation Level: read_committed
│   ├── Session Timeout: 30s
│   └── Max Poll Interval: 5 minutes
└── Topics: 生產級 partition 和 replication
```

#### **事件系統**
```yaml
Event Publisher: Kafka 實作 (生產級)
├── Type: KafkaDomainEventPublisher
├── Async: true
├── Storage: MSK Topics (持久化)
├── Persistence: 高可用性
├── Replay: 完整支援
├── Dead Letter Queue: 完整錯誤處理
├── Monitoring: 完整指標
└── Alerting: 生產級告警
```

#### **監控系統**
```yaml
Observability: 企業級監控
├── Tracing: AWS X-Ray (生產級)
│   ├── Sampling: 智慧採樣
│   ├── Business Metrics: 100% 採樣
│   ├── Infrastructure: 50% 採樣
│   └── JVM Metrics: 10% 採樣
├── Metrics:
│   ├── CloudWatch: 完整指標 (GenAIDemo-Prod)
│   ├── Custom Metrics: 業務指標
│   ├── Retention: 30 天
│   └── Cardinality: 50,000 上限
├── Logging:
│   ├── Level: WARN/INFO (優化效能)
│   ├── Format: 結構化 JSON
│   ├── Retention: 優化策略
│   └── High Volume Threshold: 5000
├── Analytics:
│   ├── Real-time: WebSocket 支援
│   ├── Cost Optimization: 啟用
│   ├── Right-sizing: 啟用
│   └── Performance Analysis: 啟用
└── Alerting: 完整的告警策略
```

## 🔄 **服務間依賴關係**

### **Local Profile 依賴圖**
```
Application
├── H2 Database (embedded)
├── Redis (optional, Docker)
├── File System (logs, temp files)
└── JVM (metrics, health checks)
```

### **Staging Profile 依賴圖**
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

### **Production Profile 依賴圖**
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

## 📋 **依賴服務清單**

### **必要依賴 (所有 Profile)**
- JVM Runtime
- Spring Boot Framework
- Logging Framework (Logback)

### **Local Profile 額外依賴**
- H2 Database (embedded)
- Redis (Docker, optional)

### **Test Profile 額外依賴**
- H2 Database (embedded)
- JUnit 5 Test Framework

### **Staging Profile 額外依賴**
- PostgreSQL (RDS)
- Redis (ElastiCache/EKS)
- Kafka (MSK)
- AWS X-Ray
- ADOT Collector
- Kubernetes
- CloudWatch
- Prometheus

### **Production Profile 額外依賴**
- PostgreSQL (RDS Multi-AZ)
- Redis (ElastiCache Cluster)
- Kafka (MSK Multi-AZ)
- AWS X-Ray
- ADOT Collector
- Kubernetes (EKS)
- CloudWatch (完整套件)
- Prometheus
- ALB/NLB
- IAM/IRSA
- KMS
- Secrets Manager
- Route 53

---

**更新日期**: 2025年9月24日 上午8:57 (台北時間)  
**維護者**: 開發團隊  
**版本**: 2.0.0