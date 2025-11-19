# Runbook: High Memory Usage

## Symptoms

- Memory utilization > 90% for extended period
- OutOfMemoryError in application logs
- Pods being killed by OOMKiller
- Slow garbage collection
- Application unresponsiveness
- Increased swap usage

## Impact

- **Severity**: P0 - Critical
- **Affected Users**: All users may experience service interruption
- **Business Impact**: Service crashes, data loss risk, complete outage

## Detection

- **Alert**: `HighMemoryUsage` alert fires
- **Monitoring Dashboard**: Operations Dashboard > Infrastructure > Memory Utilization
- **Log Patterns**:
  - `java.lang.OutOfMemoryError`
  - `GC overhead limit exceeded`
  - `OOMKilled` in pod events

## Diagnosis

### Step 1: Identify Memory Usage Pattern

```bash
# Check current memory usage
kubectl top pods -n production -l app=ecommerce-backend

# Get detailed pod resource usage
kubectl describe pod ${POD_NAME} -n production | grep -A 10 "Limits\|Requests"

# Check pod events for OOM kills
kubectl get events -n production --field-selector involvedObject.name=${POD_NAME} | grep OOM
```

### Step 2: Analyze Heap Memory

```bash
# Get heap dump (WARNING: This will pause the application briefly)
kubectl exec -it ${POD_NAME} -n production -- \
  jmap -dump:live,format=b,file=/tmp/heap-dump.hprof 1

# Copy heap dump locally for analysis
kubectl cp production/${POD_NAME}:/tmp/heap-dump.hprof \
  ./heap-dump-$(date +%Y%m%d-%H%M%S).hprof

# Get heap histogram (lighter weight)
kubectl exec -it ${POD_NAME} -n production -- \
  jmap -histo:live 1 | head -50
```

### Step 3: Check Garbage Collection

```bash
# Check GC logs
kubectl logs ${POD_NAME} -n production | grep -i "gc\|garbage"

# Get GC statistics
kubectl exec -it ${POD_NAME} -n production -- \
  jstat -gcutil 1 1000 10

# Check for memory leaks
kubectl exec -it ${POD_NAME} -n production -- \
  jstat -gccause 1
```

### Step 4: Analyze Application Metrics

```bash
# Check JVM memory metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/jvm.memory.max

# Check heap memory by area
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:nonheap

# Check memory pools
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=id:PS\ Eden\ Space
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=id:PS\ Old\ Gen
```

### Step 5: Identify Memory Leaks

```bash
# Check for common leak patterns in logs
kubectl logs ${POD_NAME} -n production --tail=5000 | grep -i "leak\|retain\|cache"

# Check thread count (threads consume memory)
kubectl exec -it ${POD_NAME} -n production -- \
  jstack 1 | grep "^\"" | wc -l

# List all threads
kubectl exec -it ${POD_NAME} -n production -- \
  jstack 1 > thread-dump-$(date +%Y%m%d-%H%M%S).txt
```

## Resolution

### Immediate Actions

1. **Restart affected pod** (temporary relief):

```bash
# Delete pod to trigger restart
kubectl delete pod ${POD_NAME} -n production

# Or restart entire deployment
kubectl rollout restart deployment/ecommerce-backend -n production
```

1. **Scale horizontally** to distribute load:

```bash
# Increase replica count
kubectl scale deployment/ecommerce-backend --replicas=8 -n production
```

1. **Force garbage collection** (if pod is still responsive):

```bash
kubectl exec -it ${POD_NAME} -n production -- \
  jcmd 1 GC.run
```

### Root Cause Fixes

#### If caused by insufficient memory allocation

1. **Increase memory limits**:

```yaml
# Update deployment
resources:
  requests:
    memory: "2Gi"    # Increase from 1Gi
  limits:
    memory: "4Gi"    # Increase from 2Gi
```

1. **Adjust JVM heap settings**:

```yaml
env:

  - name: JAVA_OPTS

    value: "-Xms2g -Xmx3g -XX:MaxMetaspaceSize=512m"
```

1. **Apply changes**:

```bash
kubectl apply -f deployment.yaml
kubectl rollout status deployment/ecommerce-backend -n production
```

#### If caused by memory leak

1. **Analyze heap dump** using tools like:
   - Eclipse Memory Analyzer (MAT)
   - VisualVM
   - JProfiler

2. **Common leak patterns to look for**:
   - Unclosed database connections
   - Unbounded caches
   - Static collections growing indefinitely
   - Event listeners not removed
   - ThreadLocal variables not cleaned

3. **Fix identified leaks**:

```java
// Example: Fix cache leak
@Configuration
public class CacheConfiguration {
    @Bean
    public CacheManager cacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("products",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class, Product.class,
                    ResourcePoolsBuilder.heap(1000)  // Limit cache size
                        .offheap(100, MemoryUnit.MB))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(30)))
            )
            .build(true);
    }
}
```

#### If caused by large object allocation

1. **Identify large objects** from heap histogram
2. **Optimize data structures**:

```java
// Example: Stream large datasets instead of loading all
public Stream<Order> findOrdersStream(LocalDate date) {
    return orderRepository.findByDateStream(date);
}

// Process in batches
public void processOrders(LocalDate date) {
    try (Stream<Order> orders = findOrdersStream(date)) {
        orders.forEach(order -> {
            processOrder(order);
            // Object eligible for GC after processing
        });
    }
}
```

#### If caused by inefficient garbage collection

1. **Tune GC parameters**:

```yaml
env:

  - name: JAVA_OPTS

    value: >-
      -Xms2g -Xmx3g
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
      -XX:ParallelGCThreads=4
      -XX:ConcGCThreads=2
      -XX:InitiatingHeapOccupancyPercent=45
      -XX:+PrintGCDetails
      -XX:+PrintGCDateStamps
      -Xloggc:/var/log/gc.log
```

1. **Monitor GC performance**:

```bash
# Analyze GC logs
kubectl logs ${POD_NAME} -n production | grep "GC" > gc.log
# Use GCViewer or similar tool to analyze
```

## Verification

- [ ] Memory usage drops below 80%
- [ ] No OOMKilled events
- [ ] GC pause times acceptable (< 200ms)
- [ ] Application responding normally
- [ ] No memory-related errors in logs
- [ ] Heap usage stable over time
- [ ] No memory leak detected in monitoring

### Verification Commands

```bash
# Monitor memory usage over time
watch -n 5 'kubectl top pod ${POD_NAME} -n production'

# Check for OOM events
kubectl get events -n production | grep OOM

# Verify GC is working properly
kubectl logs ${POD_NAME} -n production | grep "GC" | tail -20

# Check application health
curl http://localhost:8080/actuator/health
```

## Prevention

### 1. Proper Memory Configuration

```yaml
# Set appropriate resource limits
resources:
  requests:
    memory: "2Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "1000m"

# Configure JVM heap
env:

  - name: JAVA_OPTS

    value: >-
      -Xms2g
      -Xmx3g
      -XX:MaxMetaspaceSize=512m
      -XX:+UseG1GC
      -XX:MaxGCPauseMillis=200
```

### 2. Code Best Practices

```java
// Use try-with-resources for auto-cleanup
try (Connection conn = dataSource.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // Use connection
} // Automatically closed

// Limit cache sizes
@Cacheable(value = "products", unless = "#result == null")
public Product findById(String id) {
    return productRepository.findById(id);
}

// Configure cache eviction
@CacheEvict(value = "products", allEntries = true)
@Scheduled(fixedRate = 3600000) // Every hour
public void evictAllCaches() {
    // Cache cleared automatically
}

// Stream large datasets
public void processLargeDataset() {
    try (Stream<Order> orders = orderRepository.streamAll()) {
        orders.forEach(this::processOrder);
    }
}
```

### 3. Monitoring and Alerting

```yaml
# Set up memory monitoring alerts

- alert: MemoryUsageIncreasing

  expr: rate(jvm_memory_used_bytes{area="heap"}[5m]) > 0
  for: 30m
  annotations:
    summary: "Memory usage is steadily increasing"

- alert: HighGCTime

  expr: rate(jvm_gc_pause_seconds_sum[5m]) > 0.1
  for: 10m
  annotations:
    summary: "High GC time detected"
```

### 4. Regular Memory Profiling

```bash
# Schedule regular heap dumps during low traffic
# Analyze for memory leaks
# Compare heap dumps over time

# Weekly memory analysis
./scripts/analyze-memory-usage.sh
```

### 5. Load Testing

```bash
# Regular load testing to identify memory issues
./scripts/load-test.sh --duration=1h --users=1000

# Monitor memory during load test
watch -n 10 'kubectl top pods -n staging'
```

## Escalation

- **L1 Support**: DevOps team (immediate restart)
- **L2 Support**: Backend engineering team (memory leak investigation)
- **L3 Support**: Senior architect (JVM tuning, architecture review)
- **On-Call Engineer**: Check PagerDuty

## Related

- [High CPU Usage](high-cpu-usage.md)
- [Slow API Responses](slow-api-responses.md)
- [Service Outage](service-outage.md)

## Additional Resources

- [Java Memory Management Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/)
- [G1GC Tuning Guide](https://www.oracle.com/technical-resources/articles/java/g1gc.html)
- [Eclipse Memory Analyzer](https://www.eclipse.org/mat/)
- [JVM Memory Analysis Tools](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/memleaks.html)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Monthly
