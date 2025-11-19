# High Availability Design

> **Last Updated**: 2025-10-24  
> **Status**: Active  
> **Owner**: Infrastructure & Architecture Team

## Overview

This document describes the high availability (HA) architecture of the Enterprise E-Commerce Platform. Our HA design ensures the system remains operational even when individual components fail, achieving our 99.9% uptime target through redundancy, automatic failover, and intelligent load distribution.

## High Availability Principles

### Core Principles

1. **No Single Point of Failure (SPOF)**: Every critical component has redundancy
2. **Automatic Failover**: System recovers without manual intervention
3. **Stateless Services**: Enable horizontal scaling and easy replacement
4. **Health-Based Routing**: Traffic only goes to healthy instances
5. **Geographic Distribution**: Multi-AZ deployment for zone-level resilience

### Availability Tiers

| Tier | Availability | Redundancy | Use Case |
|------|--------------|------------|----------|
| **Critical** | 99.95% | Multi-AZ + Multi-Region | Payment, Authentication |
| **High** | 99.9% | Multi-AZ | Order, Inventory, Customer |
| **Standard** | 99.5% | Multi-AZ | Recommendations, Reviews |
| **Best Effort** | 99.0% | Single-AZ | Analytics, Reporting |

---

## Multi-AZ Deployment Architecture

### AWS Multi-AZ Strategy

```text
┌─────────────────────────────────────────────────────────────────┐
│                         AWS Region (us-east-1)                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐     │
│  │     AZ-1     │    │     AZ-2     │    │     AZ-3     │     │
│  │  (Primary)   │    │  (Secondary) │    │  (Tertiary)  │     │
│  ├──────────────┤    ├──────────────┤    ├──────────────┤     │
│  │              │    │              │    │              │     │
│  │  ┌────────┐  │    │  ┌────────┐  │    │  ┌────────┐  │     │
│  │  │  EKS   │  │    │  │  EKS   │  │    │  │  EKS   │  │     │
│  │  │ Nodes  │  │    │  │ Nodes  │  │    │  │ Nodes  │  │     │
│  │  │  (3)   │  │    │  │  (3)   │  │    │  │  (3)   │  │     │
│  │  └────────┘  │    │  └────────┘  │    │  └────────┘  │     │
│  │              │    │              │    │              │     │
│  │  ┌────────┐  │    │  ┌────────┐  │    │              │     │
│  │  │  RDS   │  │◄───┼──┤  RDS   │  │    │              │     │
│  │  │Primary │  │    │  │Standby │  │    │              │     │
│  │  └────────┘  │    │  └────────┘  │    │              │     │
│  │              │    │              │    │              │     │
│  │  ┌────────┐  │    │  ┌────────┐  │    │  ┌────────┐  │     │
│  │  │ Redis  │  │    │  │ Redis  │  │    │  │ Redis  │  │     │
│  │  │Primary │  │◄───┼──┤Replica │  │◄───┼──┤Replica │  │     │
│  │  └────────┘  │    │  └────────┘  │    │  └────────┘  │     │
│  │              │    │              │    │              │     │
│  └──────────────┘    └──────────────┘    └──────────────┘     │
│                                                                 │
│  ┌───────────────────────────────────────────────────────────┐ │
│  │         Application Load Balancer (Multi-AZ)              │ │
│  │         Health Checks + Cross-Zone Load Balancing         │ │
│  └───────────────────────────────────────────────────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Benefits

- **Zone Failure Tolerance**: System continues if entire AZ fails
- **Automatic Failover**: AWS handles infrastructure-level failover
- **Low Latency**: Cross-AZ latency typically < 2ms
- **Cost Effective**: No data transfer charges between AZs in same region

---

## Load Balancing

### Application Load Balancer (ALB)

#### Configuration

```yaml
# ALB Configuration
LoadBalancer:
  Type: application
  Scheme: internet-facing
  IpAddressType: ipv4
  
  Subnets:

    - subnet-az1-public
    - subnet-az2-public
    - subnet-az3-public
  
  SecurityGroups:

    - alb-security-group
  
  LoadBalancerAttributes:

    - Key: deletion_protection.enabled

      Value: true

    - Key: idle_timeout.timeout_seconds

      Value: 60

    - Key: routing.http2.enabled

      Value: true

    - Key: routing.http.drop_invalid_header_fields.enabled

      Value: true
```

#### Target Groups

```yaml
TargetGroup:
  Name: order-service-tg
  Protocol: HTTP
  Port: 8080
  VpcId: vpc-main
  
  HealthCheckEnabled: true
  HealthCheckProtocol: HTTP
  HealthCheckPath: /actuator/health
  HealthCheckIntervalSeconds: 30
  HealthCheckTimeoutSeconds: 5
  HealthyThresholdCount: 2
  UnhealthyThresholdCount: 3
  
  TargetType: ip
  
  Deregistration:
    ConnectionTermination:
      Enabled: true
    DelaySeconds: 30
```

#### Health Check Strategy

```java
@RestController
@RequestMapping("/actuator/health")
public class HealthCheckController {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;
    
    @GetMapping
    public ResponseEntity<HealthStatus> health() {
        HealthStatus status = new HealthStatus();
        
        // Check database
        status.setDatabase(checkDatabase());
        
        // Check cache
        status.setCache(checkCache());
        
        // Check critical dependencies
        status.setDependencies(checkDependencies());
        
        if (status.isHealthy()) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.status(503).body(status);
        }
    }
    
    private ComponentHealth checkDatabase() {
        try {
            dataSource.getConnection().createStatement().execute("SELECT 1");
            return ComponentHealth.up();
        } catch (Exception e) {
            return ComponentHealth.down(e.getMessage());
        }
    }
}
```

### Load Balancing Algorithms

#### Round Robin (Default)

- Distributes requests evenly across targets
- Simple and effective for homogeneous instances

#### Least Outstanding Requests

- Routes to target with fewest active connections
- Better for varying request processing times

```yaml
TargetGroupAttributes:

  - Key: load_balancing.algorithm.type

    Value: least_outstanding_requests
```

---

## Kubernetes High Availability

### EKS Cluster Configuration

```yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: ecommerce-cluster
  region: us-east-1
  version: "1.28"

availabilityZones:

  - us-east-1a
  - us-east-1b
  - us-east-1c

managedNodeGroups:

  - name: application-nodes

    instanceType: t3.large
    minSize: 3
    maxSize: 15
    desiredCapacity: 9  # 3 per AZ
    
    availabilityZones:

      - us-east-1a
      - us-east-1b
      - us-east-1c
    
    labels:
      role: application
      environment: production
    
    tags:
      k8s.io/cluster-autoscaler/enabled: "true"
      k8s.io/cluster-autoscaler/ecommerce-cluster: "owned"
```

### Pod Distribution

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 9  # 3 per AZ
  
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 3
      maxUnavailable: 1
  
  template:
    spec:
      # Spread pods across AZs
      topologySpreadConstraints:

        - maxSkew: 1

          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app: order-service
      
      # Prefer different nodes
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:

            - weight: 100

              podAffinityTerm:
                labelSelector:
                  matchLabels:
                    app: order-service
                topologyKey: kubernetes.io/hostname
      
      containers:

        - name: order-service

          image: order-service:latest
          
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          
          # Liveness probe
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          
          # Readiness probe
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
```

### Horizontal Pod Autoscaler (HPA)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  
  minReplicas: 9   # 3 per AZ
  maxReplicas: 30  # 10 per AZ
  
  metrics:

    - type: Resource

      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    
    - type: Resource

      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
    
    - type: Pods

      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
  
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:

        - type: Percent

          value: 50
          periodSeconds: 60
    
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:

        - type: Percent

          value: 10
          periodSeconds: 60
```

---

## Database High Availability

### RDS Multi-AZ Configuration

```yaml
DBInstance:
  DBInstanceIdentifier: ecommerce-db
  DBInstanceClass: db.r6g.xlarge
  Engine: postgres
  EngineVersion: "15.3"
  
  # Multi-AZ for automatic failover
  MultiAZ: true
  
  # Storage
  AllocatedStorage: 500
  StorageType: gp3
  StorageEncrypted: true
  
  # Backup
  BackupRetentionPeriod: 7
  PreferredBackupWindow: "03:00-04:00"
  
  # Maintenance
  PreferredMaintenanceWindow: "sun:04:00-sun:05:00"
  AutoMinorVersionUpgrade: true
  
  # High Availability
  AvailabilityZone: us-east-1a  # Primary
  # Standby automatically created in different AZ
```

### Automatic Failover

```text
┌─────────────────────────────────────────────────────────┐
│                    Normal Operation                     │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Application ──────► Primary DB (AZ-1)                 │
│                           │                             │
│                           │ Synchronous                 │
│                           │ Replication                 │
│                           ▼                             │
│                      Standby DB (AZ-2)                  │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Primary Failure Detected               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Application ──X──► Primary DB (AZ-1) [FAILED]         │
│       │                                                 │
│       │ DNS Update                                      │
│       │ (60-120 seconds)                                │
│       │                                                 │
│       └──────────► Standby DB (AZ-2)                    │
│                    [Promoted to Primary]                │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Read Replicas

```yaml
ReadReplica:

  - DBInstanceIdentifier: ecommerce-db-read-1

    SourceDBInstanceIdentifier: ecommerce-db
    AvailabilityZone: us-east-1b
    PubliclyAccessible: false
  
  - DBInstanceIdentifier: ecommerce-db-read-2

    SourceDBInstanceIdentifier: ecommerce-db
    AvailabilityZone: us-east-1c
    PubliclyAccessible: false
```

### Connection Pooling

```java
@Configuration
public class DatabaseConfiguration {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Primary database
        config.setJdbcUrl(primaryDbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        
        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Health check
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        return new HikariDataSource(config);
    }
    
    @Bean
    public DataSource readReplicaDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Read replica
        config.setJdbcUrl(readReplicaDbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setReadOnly(true);
        
        // Connection pool settings
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        
        return new HikariDataSource(config);
    }
}
```

---

## Cache High Availability

### ElastiCache Redis Cluster Mode

```yaml
ReplicationGroup:
  ReplicationGroupId: ecommerce-cache
  ReplicationGroupDescription: E-commerce cache cluster
  
  # Cluster mode enabled for automatic sharding
  CacheNodeType: cache.r6g.large
  NumNodeGroups: 3  # 3 shards
  ReplicasPerNodeGroup: 2  # 2 replicas per shard
  
  # Multi-AZ with automatic failover
  AutomaticFailoverEnabled: true
  MultiAZEnabled: true
  
  # Snapshot and backup
  SnapshotRetentionLimit: 5
  SnapshotWindow: "03:00-05:00"
  
  # Maintenance
  PreferredMaintenanceWindow: "sun:05:00-sun:07:00"
  
  # Security
  AtRestEncryptionEnabled: true
  TransitEncryptionEnabled: true
```

### Redis Cluster Architecture

```text
┌─────────────────────────────────────────────────────────┐
│              Redis Cluster (3 Shards)                   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Shard 1 (Slots 0-5461)                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Primary  │──┤ Replica  │──┤ Replica  │             │
│  │  (AZ-1)  │  │  (AZ-2)  │  │  (AZ-3)  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                         │
│  Shard 2 (Slots 5462-10922)                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Primary  │──┤ Replica  │──┤ Replica  │             │
│  │  (AZ-2)  │  │  (AZ-3)  │  │  (AZ-1)  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                         │
│  Shard 3 (Slots 10923-16383)                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Primary  │──┤ Replica  │──┤ Replica  │             │
│  │  (AZ-3)  │  │  (AZ-1)  │  │  (AZ-2)  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Cache Failover Handling

```java
@Configuration
public class RedisConfiguration {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration(clusterNodes);
        
        // Connection pool
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        
        LettucePoolingClientConfiguration clientConfig = 
            LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofSeconds(5))
                .build();
        
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        
        // Serialization
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return template;
    }
}
```

---

## Stateless Service Design

### Principles

1. **No Local State**: All state stored in external systems (database, cache)
2. **Session Externalization**: Sessions stored in Redis
3. **Idempotent Operations**: Safe to retry operations
4. **Horizontal Scalability**: Can add/remove instances freely

### Implementation

```java
@Service
public class StatelessOrderService {
    
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Order> redisTemplate;
    
    public Order getOrder(String orderId) {
        // Try cache first
        Order cachedOrder = redisTemplate.opsForValue().get("order:" + orderId);
        if (cachedOrder != null) {
            return cachedOrder;
        }
        
        // Fetch from database
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Cache for future requests
        redisTemplate.opsForValue().set("order:" + orderId, order, 
            Duration.ofMinutes(15));
        
        return order;
    }
    
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        // Idempotent operation using idempotency key
        String idempotencyKey = command.getIdempotencyKey();
        
        // Check if already processed
        Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder != null) {
            return existingOrder;
        }
        
        // Create new order
        Order order = Order.create(command);
        order.setIdempotencyKey(idempotencyKey);
        
        return orderRepository.save(order);
    }
}
```

---

## Health Checks and Monitoring

### Comprehensive Health Checks

```java
@Component
public class ComprehensiveHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean isHealthy = true;
        
        // Database health
        try {
            checkDatabase();
            details.put("database", "UP");
        } catch (Exception e) {
            details.put("database", "DOWN: " + e.getMessage());
            isHealthy = false;
        }
        
        // Cache health
        try {
            checkCache();
            details.put("cache", "UP");
        } catch (Exception e) {
            details.put("cache", "DOWN: " + e.getMessage());
            isHealthy = false;
        }
        
        // Disk space
        try {
            checkDiskSpace();
            details.put("diskSpace", "UP");
        } catch (Exception e) {
            details.put("diskSpace", "DOWN: " + e.getMessage());
            isHealthy = false;
        }
        
        // External dependencies
        try {
            checkExternalDependencies();
            details.put("external", "UP");
        } catch (Exception e) {
            details.put("external", "DEGRADED: " + e.getMessage());
            // Don't mark as unhealthy for external dependencies
        }
        
        return isHealthy ? 
            Health.up().withDetails(details).build() :
            Health.down().withDetails(details).build();
    }
}
```

### Monitoring Dashboard

Key metrics to monitor:

- **Instance Health**: Number of healthy vs unhealthy instances
- **Request Distribution**: Requests per instance
- **Response Times**: P50, P95, P99 latencies
- **Error Rates**: 4xx and 5xx errors
- **Resource Utilization**: CPU, memory, network

---

## Testing High Availability

### Chaos Engineering Tests

```bash
# Test pod failure
kubectl delete pod -l app=order-service --force

# Test AZ failure simulation
kubectl cordon node-az1-1
kubectl cordon node-az1-2
kubectl cordon node-az1-3

# Test database failover
aws rds reboot-db-instance --db-instance-identifier ecommerce-db --force-failover

# Test cache failover
aws elasticache test-failover --replication-group-id ecommerce-cache --node-group-id 0001
```

### Load Testing

```bash
# Simulate high load
k6 run --vus 1000 --duration 30m load-test.js

# Monitor during test
kubectl top nodes
kubectl top pods
```

---

**Related Documents**:

- [Fault Tolerance](fault-tolerance.md) - Application-level resilience
