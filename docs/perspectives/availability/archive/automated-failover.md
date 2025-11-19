# Automated Failover Mechanisms

> **Last Updated**: 2025-11-17  
> **Status**: ✅ Operational  
> **Owner**: SRE & Platform Team

## Purpose

This document describes the automated failover mechanisms that enable the Enterprise E-Commerce Platform to achieve < 30 seconds RTO with zero manual intervention. The system employs multiple layers of failover detection and response to ensure continuous availability.

## Failover Architecture

### Multi-Layer Failover Strategy

```text
┌──────────────────────────────────────────────────────────┐
│ Layer 1: DNS-Based Failover (Route 53)                  │
│ Detection: 30s | Response: 30s | Scope: Regional        │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│ Layer 2: Application-Layer Smart Routing                │
│ Detection: 5s | Response: 5s | Scope: Service-level     │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│ Layer 3: Database Automatic Failover (Aurora)           │
│ Detection: 10s | Response: 30s | Scope: Database        │
└────────────────────┬─────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────┐
│ Layer 4: Kubernetes Self-Healing (EKS)                  │
│ Detection: 10s | Response: 10s | Scope: Pod-level       │
└──────────────────────────────────────────────────────────┘
```

## Layer 1: DNS-Based Failover

### Route 53 Health Checks

#### Configuration

```typescript
// infrastructure/lib/constructs/health-check.ts
export class HealthCheckConstruct extends Construct {
  public readonly healthCheck: route53.CfnHealthCheck;
  
  constructor(scope: Construct, id: string, props: HealthCheckProps) {
    super(scope, id);
    
    this.healthCheck = new route53.CfnHealthCheck(this, 'HealthCheck', {
      healthCheckConfig: {
        type: 'HTTPS',
        resourcePath: '/actuator/health/readiness',
        fullyQualifiedDomainName: props.endpoint,
        port: 443,
        requestInterval: 30,  // Check every 30 seconds
        failureThreshold: 2,   // 2 consecutive failures = unhealthy
        measureLatency: true,
        enableSNI: true,
      },
      healthCheckTags: [{
        key: 'Name',
        value: props.name
      }, {
        key: 'Region',
        value: props.region
      }]
    });
    
    // CloudWatch alarm for health check failures
    new cloudwatch.Alarm(this, 'HealthCheckAlarm', {
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Route53',
        metricName: 'HealthCheckStatus',
        dimensionsMap: {
          HealthCheckId: this.healthCheck.attrHealthCheckId
        },
        statistic: 'Minimum',
        period: cdk.Duration.minutes(1),
      }),
      threshold: 1,
      evaluationPeriods: 2,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      alarmDescription: `Health check failure for ${props.name}`,
      actionsEnabled: true,
    });
  }
}
```

#### Health Check Endpoint

```java
// interfaces/rest/health/HealthCheckController.java
@RestController
@RequestMapping("/actuator/health")
public class HealthCheckController {
    
    private final HealthIndicatorRegistry healthIndicatorRegistry;
    private final HealthCheckService healthCheckService;
    
    @GetMapping("/readiness")
    public ResponseEntity<HealthStatus> readiness() {
        HealthStatus status = healthCheckService.checkReadiness();
        
        if (status.isHealthy()) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
    }
    
    @GetMapping("/liveness")
    public ResponseEntity<HealthStatus> liveness() {
        HealthStatus status = healthCheckService.checkLiveness();
        
        if (status.isHealthy()) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
    }
}

// application/health/HealthCheckService.java
@Service
public class HealthCheckService {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public HealthStatus checkReadiness() {
        List<ComponentHealth> components = new ArrayList<>();
        
        // Check database connectivity
        components.add(checkDatabase());
        
        // Check cache connectivity
        components.add(checkCache());
        
        // Check message queue
        components.add(checkMessageQueue());
        
        boolean allHealthy = components.stream().allMatch(ComponentHealth::isHealthy);
        
        return new HealthStatus(
            allHealthy ? "UP" : "DOWN",
            components,
            Instant.now()
        );
    }
    
    private ComponentHealth checkDatabase() {
        try {
            Connection conn = dataSource.getConnection();
            boolean valid = conn.isValid(5); // 5 second timeout
            conn.close();
            
            return new ComponentHealth("database", valid ? "UP" : "DOWN", 
                                      valid ? null : "Connection validation failed");
        } catch (SQLException e) {
            return new ComponentHealth("database", "DOWN", e.getMessage());
        }
    }
    
    private ComponentHealth checkCache() {
        try {
            String testKey = "health-check-" + UUID.randomUUID();
            redisTemplate.opsForValue().set(testKey, "test", 5, TimeUnit.SECONDS);
            String value = redisTemplate.opsForValue().get(testKey);
            redisTemplate.delete(testKey);
            
            return new ComponentHealth("cache", "test".equals(value) ? "UP" : "DOWN", null);
        } catch (Exception e) {
            return new ComponentHealth("cache", "DOWN", e.getMessage());
        }
    }
    
    private ComponentHealth checkMessageQueue() {
        try {
            // Send test message to health check topic
            kafkaTemplate.send("health-check-topic", "test").get(5, TimeUnit.SECONDS);
            return new ComponentHealth("message-queue", "UP", null);
        } catch (Exception e) {
            return new ComponentHealth("message-queue", "DOWN", e.getMessage());
        }
    }
}
```

### Failover Decision Logic

**Trigger Conditions**:
1. 2 consecutive health check failures (60 seconds)
2. HTTP status code 503 (Service Unavailable)
3. Connection timeout (10 seconds)
4. SSL/TLS handshake failure

**Automatic Actions**:
1. Mark region as unhealthy in Route 53
2. Remove region from DNS responses
3. Redistribute traffic to healthy regions
4. Send critical alert to operations team
5. Trigger automated incident response

## Layer 2: Application-Layer Smart Routing

### Smart Routing Components

#### Region Detector

```java
// infrastructure/routing/RegionDetector.java
@Component
public class RegionDetector {
    
    private static final String AWS_REGION_ENV = "AWS_REGION";
    private static final String AWS_DEFAULT_REGION_ENV = "AWS_DEFAULT_REGION";
    
    private final String currentRegion;
    
    public RegionDetector() {
        this.currentRegion = detectRegion();
        logger.info("Detected current region: {}", currentRegion);
    }
    
    public String detectRegion() {
        // Try environment variable first
        String region = System.getenv(AWS_REGION_ENV);
        if (region != null && !region.isEmpty()) {
            return normalizeRegion(region);
        }
        
        region = System.getenv(AWS_DEFAULT_REGION_ENV);
        if (region != null && !region.isEmpty()) {
            return normalizeRegion(region);
        }
        
        // Try EC2 metadata service
        try {
            region = EC2MetadataUtils.getEC2InstanceRegion();
            if (region != null) {
                return normalizeRegion(region);
            }
        } catch (Exception e) {
            logger.warn("Failed to get region from EC2 metadata", e);
        }
        
        // Try availability zone
        try {
            String az = EC2MetadataUtils.getAvailabilityZone();
            if (az != null) {
                return normalizeRegion(az.substring(0, az.length() - 1));
            }
        } catch (Exception e) {
            logger.warn("Failed to get region from availability zone", e);
        }
        
        // Default to taiwan
        logger.warn("Could not detect region, defaulting to taiwan");
        return "taiwan";
    }
    
    private String normalizeRegion(String region) {
        if (region.contains("northeast-1")) {
            return "taiwan";
        } else if (region.contains("northeast-1")) {
            return "japan";
        }
        return region;
    }
    
    public String getCurrentRegion() {
        return currentRegion;
    }
}
```

#### Health Checker

```java
// infrastructure/routing/HealthChecker.java
@Component
public class HealthChecker {
    
    private final Map<String, HealthStatus> regionHealth = new ConcurrentHashMap<>();
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;
    
    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void refreshAllRegions() {
        List.of("taiwan", "japan").parallelStream().forEach(region -> {
            HealthStatus status = checkRegionHealth(region);
            HealthStatus previousStatus = regionHealth.put(region, status);
            
            // Detect status change
            if (previousStatus != null && previousStatus.isHealthy() != status.isHealthy()) {
                handleHealthStatusChange(region, previousStatus, status);
            }
            
            // Publish metrics
            publishHealthMetrics(region, status);
        });
    }
    
    private HealthStatus checkRegionHealth(String region) {
        long startTime = System.currentTimeMillis();
        
        try {
            String endpoint = getRegionEndpoint(region);
            
            // Check database
            boolean dbHealthy = checkDatabaseHealth(endpoint);
            
            // Check cache
            boolean cacheHealthy = checkCacheHealth(endpoint);
            
            // Check message queue
            boolean mqHealthy = checkMessageQueueHealth(endpoint);
            
            long latency = System.currentTimeMillis() - startTime;
            
            boolean overallHealthy = dbHealthy && cacheHealthy && mqHealthy && latency < 5000;
            
            return new HealthStatus(
                overallHealthy,
                latency,
                Map.of(
                    "database", dbHealthy,
                    "cache", cacheHealthy,
                    "messageQueue", mqHealthy
                ),
                Instant.now()
            );
        } catch (Exception e) {
            logger.error("Health check failed for region: {}", region, e);
            return HealthStatus.unhealthy(e.getMessage());
        }
    }
    
    private boolean checkDatabaseHealth(String endpoint) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                endpoint + "/actuator/health/db",
                Map.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Database health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    private void handleHealthStatusChange(String region, HealthStatus previous, HealthStatus current) {
        if (previous.isHealthy() && !current.isHealthy()) {
            logger.error("Region {} became UNHEALTHY", region);
            publishAlert(region, "UNHEALTHY", current);
        } else if (!previous.isHealthy() && current.isHealthy()) {
            logger.info("Region {} recovered to HEALTHY", region);
            publishAlert(region, "RECOVERED", current);
        }
    }
    
    private void publishHealthMetrics(String region, HealthStatus status) {
        meterRegistry.gauge("region.health.status", 
            Tags.of("region", region), 
            status.isHealthy() ? 1.0 : 0.0);
        
        meterRegistry.gauge("region.health.latency", 
            Tags.of("region", region), 
            status.getLatency());
    }
    
    public boolean isHealthy(String region) {
        HealthStatus status = regionHealth.get(region);
        return status != null && status.isHealthy();
    }
    
    public HealthStatus getHealthStatus(String region) {
        return regionHealth.getOrDefault(region, HealthStatus.unknown());
    }
}
```

#### Route Selector

```java
// infrastructure/routing/RouteSelector.java
@Component
public class RouteSelector {
    
    private final RegionDetector regionDetector;
    private final HealthChecker healthChecker;
    
    public String selectOptimalRegion() {
        String currentRegion = regionDetector.getCurrentRegion();
        
        // Prefer local region if healthy
        if (healthChecker.isHealthy(currentRegion)) {
            return currentRegion;
        }
        
        // Failover to backup region
        String backupRegion = getBackupRegion(currentRegion);
        
        if (healthChecker.isHealthy(backupRegion)) {
            logger.warn("Failing over from {} to {}", currentRegion, backupRegion);
            return backupRegion;
        }
        
        // Both regions unhealthy - use current region and let it fail
        logger.error("All regions unhealthy, using current region: {}", currentRegion);
        return currentRegion;
    }
    
    public String selectBackupRegion(String primaryRegion) {
        return getBackupRegion(primaryRegion);
    }
    
    private String getBackupRegion(String region) {
        return "taiwan".equals(region) ? "japan" : "taiwan";
    }
    
    public EndpointInfo selectOptimalEndpoint(String serviceType) {
        String region = selectOptimalRegion();
        HealthStatus health = healthChecker.getHealthStatus(region);
        
        return new EndpointInfo(
            region,
            getServiceEndpoint(region, serviceType),
            health.getLatency(),
            health.isHealthy()
        );
    }
    
    private String getServiceEndpoint(String region, String serviceType) {
        return switch (serviceType) {
            case "database" -> getDatabaseEndpoint(region);
            case "cache" -> getCacheEndpoint(region);
            case "messageQueue" -> getMessageQueueEndpoint(region);
            default -> throw new IllegalArgumentException("Unknown service type: " + serviceType);
        };
    }
}
```

#### Smart Routing DataSource

```java
// infrastructure/routing/SmartRoutingDataSource.java
@Component
public class SmartRoutingDataSource extends AbstractRoutingDataSource {
    
    private final RouteSelector routeSelector;
    private final Map<String, DataSource> dataSources;
    
    @PostConstruct
    public void init() {
        setTargetDataSources(new HashMap<>(dataSources));
        setDefaultTargetDataSource(dataSources.get("taiwan-datasource"));
        afterPropertiesSet();
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        String region = routeSelector.selectOptimalRegion();
        return region + "-datasource";
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        try {
            return super.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to get connection, attempting failover", e);
            
            // Force refresh health status
            String currentRegion = routeSelector.selectOptimalRegion();
            String backupRegion = routeSelector.selectBackupRegion(currentRegion);
            
            // Try backup region
            DataSource backupDataSource = dataSources.get(backupRegion + "-datasource");
            if (backupDataSource != null) {
                return backupDataSource.getConnection();
            }
            
            throw e;
        }
    }
}
```

### Failover Performance

**Detection Time**: 5 seconds (health check interval)  
**Response Time**: < 1 second (in-memory routing decision)  
**Total RTO**: < 10 seconds  
**User Impact**: Transparent (automatic retry)

## Layer 3: Database Automatic Failover

### Aurora Multi-AZ Failover

#### Configuration

```typescript
// Aurora cluster with automatic failover
const cluster = new rds.DatabaseCluster(this, 'Cluster', {
  engine: rds.DatabaseClusterEngine.auroraPostgres({
    version: rds.AuroraPostgresEngineVersion.VER_15_4
  }),
  writer: rds.ClusterInstance.provisioned('writer', {
    instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.XLARGE),
    enablePerformanceInsights: true,
    performanceInsightRetention: rds.PerformanceInsightRetention.LONG_TERM,
  }),
  readers: [
    rds.ClusterInstance.provisioned('reader-1', {
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE),
      promotionTier: 0, // Highest priority for promotion
    }),
    rds.ClusterInstance.provisioned('reader-2', {
      instanceType: ec2.InstanceType.of(ec2.InstanceClass.R6G, ec2.InstanceSize.LARGE),
      promotionTier: 1, // Second priority
    }),
  ],
  vpc: vpc,
  vpcSubnets: {
    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
    availabilityZones: ['ap-northeast-1a', 'ap-northeast-1c', 'ap-northeast-1d']
  },
  backup: {
    retention: cdk.Duration.days(7),
    preferredWindow: '03:00-04:00',
  },
  cloudwatchLogsExports: ['postgresql'],
  cloudwatchLogsRetention: logs.RetentionDays.ONE_MONTH,
  monitoringInterval: cdk.Duration.seconds(60),
  enablePerformanceInsights: true,
});
```

#### Failover Process

1. **Detection** (10 seconds):
   - Aurora monitors primary instance health
   - Detects instance failure or unresponsiveness
   - Initiates automatic failover

2. **Promotion** (20 seconds):
   - Promotes highest-priority reader to writer
   - Updates DNS endpoint to point to new writer
   - Maintains read replicas

3. **Recovery** (30 seconds total):
   - Applications reconnect to new writer
   - Connection pools refresh
   - Normal operations resume

### Application Connection Handling

```java
// config/DatabaseConfiguration.java
@Configuration
public class DatabaseConfiguration {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        // Connection pool settings for failover
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(10000); // 10 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        
        // Failover settings
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000);
        
        // Retry logic
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "10");
        config.addDataSourceProperty("connectTimeout", "10");
        
        return new HikariDataSource(config);
    }
}
```

## Layer 4: Kubernetes Self-Healing

### Pod-Level Failover

#### Liveness and Readiness Probes

```yaml
# k8s/application/backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend-api
spec:
  replicas: 5
  selector:
    matchLabels:
      app: backend-api
  template:
    metadata:
      labels:
        app: backend-api
    spec:
      containers:
      - name: backend
        image: backend-api:latest
        ports:
        - containerPort: 8080
        
        # Startup probe - for slow-starting applications
        startupProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 30  # 5 minutes max startup time
        
        # Liveness probe - restart if unhealthy
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3  # Restart after 30 seconds of failures
        
        # Readiness probe - remove from service if not ready
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3  # Remove from service after 15 seconds
        
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### Automatic Pod Restart

**Trigger Conditions**:
- Liveness probe fails 3 consecutive times
- Container process exits with non-zero code
- Out of memory (OOM) error
- Unhandled exception crashes application

**Automatic Actions**:
1. Kubernetes terminates unhealthy pod
2. New pod is scheduled on healthy node
3. Pod starts and passes startup probe
4. Pod becomes ready and receives traffic
5. Old pod is removed

**Recovery Time**: 10-30 seconds (depending on startup time)

## Failover Testing

### Automated Testing

```bash
#!/bin/bash
# scripts/test-automated-failover.sh

echo "Testing Automated Failover Mechanisms"
echo "======================================"

# Test 1: DNS-based failover
echo "Test 1: Simulating Taiwan region failure..."
./scripts/simulate-region-failure.sh taiwan
sleep 60
./scripts/verify-traffic-shift.sh japan
./scripts/restore-region.sh taiwan

# Test 2: Application-layer failover
echo "Test 2: Testing smart routing failover..."
./scripts/inject-database-latency.sh taiwan 10000
sleep 10
./scripts/verify-smart-routing.sh japan
./scripts/remove-latency-injection.sh taiwan

# Test 3: Database failover
echo "Test 3: Triggering Aurora failover..."
aws rds failover-db-cluster --db-cluster-identifier genai-demo-taiwan
sleep 30
./scripts/verify-database-connectivity.sh

# Test 4: Pod-level failover
echo "Test 4: Killing random pods..."
kubectl delete pod -l app=backend-api --random
sleep 15
./scripts/verify-service-availability.sh

echo "All failover tests completed!"
./scripts/generate-failover-report.sh
```

## Monitoring and Metrics

### Key Failover Metrics

```yaml
# Prometheus metrics
failover_events_total:
  type: counter
  labels: [layer, region, reason]
  description: Total number of failover events

failover_duration_seconds:
  type: histogram
  labels: [layer, region]
  description: Time taken to complete failover

region_health_status:
  type: gauge
  labels: [region]
  description: Current health status (1=healthy, 0=unhealthy)

failover_success_rate:
  type: gauge
  labels: [layer]
  description: Percentage of successful failovers
```

### Grafana Dashboard

**Failover Overview Panel**:
- Failover events timeline
- Current region health status
- Failover duration trends
- Success rate by layer
- Active incidents

## Best Practices

1. **Test Regularly**: Monthly automated failover tests
2. **Monitor Continuously**: Real-time health checks every 5 seconds
3. **Automate Everything**: Zero manual intervention required
4. **Document Incidents**: Post-mortem for every failover event
5. **Optimize Performance**: Minimize failover detection and response time


**Next Steps**:
1. Review [Chaos Engineering](chaos-engineering.md) for proactive testing
2. Implement monthly failover drills
3. Monitor failover metrics in Grafana dashboard
