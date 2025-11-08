# Runbook: Slow API Responses

## Symptoms

- API response time > 2s (95th percentile)
- Timeout errors from clients
- Increased request queue length
- User complaints about slow page loads
- High latency in distributed traces

## Impact

- **Severity**: P1 - High
- **Affected Users**: All users experience degraded performance
- **Business Impact**: Poor user experience, potential cart abandonment, revenue loss

## Detection

- **Alert**: `SlowAPIResponseTime` alert fires
- **Monitoring Dashboard**: Operations Dashboard > Performance > API Response Times
- **Log Patterns**:
  - `Request took longer than expected`
  - `Slow query detected`
  - `Timeout waiting for response`

## Diagnosis

### Step 1: Identify Slow Endpoints

```bash
# Check response times by endpoint
curl http://localhost:8080/actuator/metrics/http.server.requests | jq '.measurements'

# Get detailed metrics for specific endpoint
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/v1/orders" | jq

# Check Prometheus metrics
curl -g 'http://prometheus:9090/api/v1/query?query=histogram_quantile(0.95,rate(http_request_duration_seconds_bucket[5m]))' | jq
```

### Step 2: Analyze Request Patterns

```bash
# Check request rate
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep "HTTP" | awk '{print $1}' | uniq -c | sort -rn

# Identify slow requests in logs
kubectl logs deployment/ecommerce-backend -n production --tail=5000 | \
  grep -i "slow\|timeout" | head -50

# Check for specific slow endpoints
kubectl logs deployment/ecommerce-backend -n production --tail=5000 | \
  grep "duration" | awk '$NF > 2000' | head -20
```

### Step 3: Check Database Performance

```bash
# Check active database queries
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT pid, now() - query_start as duration, state, query 
   FROM pg_stat_activity 
   WHERE state != 'idle' 
   ORDER BY duration DESC 
   LIMIT 20;"

# Check for slow queries
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT query, calls, total_time, mean_time, max_time 
   FROM pg_stat_statements 
   ORDER BY mean_time DESC 
   LIMIT 20;"

# Check for table locks
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT locktype, relation::regclass, mode, granted, pid 
   FROM pg_locks 
   WHERE NOT granted 
   ORDER BY pid;"

# Check database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

### Step 4: Check Cache Performance

```bash
# Check cache hit rate
curl http://localhost:8080/actuator/metrics/cache.gets?tag=result:hit
curl http://localhost:8080/actuator/metrics/cache.gets?tag=result:miss

# Calculate hit rate
HIT=$(curl -s http://localhost:8080/actuator/metrics/cache.gets?tag=result:hit | jq '.measurements[0].value')
MISS=$(curl -s http://localhost:8080/actuator/metrics/cache.gets?tag=result:miss | jq '.measurements[0].value')
echo "Cache hit rate: $(echo "scale=2; $HIT / ($HIT + $MISS) * 100" | bc)%"

# Check Redis performance
kubectl exec -it redis-0 -n production -- redis-cli INFO stats | grep -E "keyspace_hits|keyspace_misses"
kubectl exec -it redis-0 -n production -- redis-cli INFO stats | grep instantaneous_ops_per_sec
kubectl exec -it redis-0 -n production -- redis-cli SLOWLOG GET 10
```

### Step 5: Analyze Distributed Traces

```bash
# Get trace IDs for slow requests
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep "duration.*[3-9][0-9][0-9][0-9]" | \
  grep -oP 'traceId=\K[a-f0-9]+'

# View trace in X-Ray console or use AWS CLI
aws xray get-trace-summaries \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --filter-expression 'duration > 2'

# Get detailed trace
aws xray batch-get-traces --trace-ids ${TRACE_ID}
```

### Step 6: Check External Dependencies

```bash
# Check external API response times
kubectl logs deployment/ecommerce-backend -n production --tail=1000 | \
  grep "external" | grep "duration"

# Test payment gateway
curl -w "@curl-format.txt" -o /dev/null -s https://payment-gateway.example.com/health

# Test email service
curl -w "@curl-format.txt" -o /dev/null -s https://email-service.example.com/health

# Check Kafka lag
kubectl exec -it kafka-0 -n production -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group ecommerce-consumer
```

### Step 7: Check Resource Utilization

```bash
# Check CPU and memory
kubectl top pods -n production -l app=ecommerce-backend

# Check network I/O
kubectl exec -it ${POD_NAME} -n production -- \
  cat /proc/net/dev

# Check disk I/O
kubectl exec -it ${POD_NAME} -n production -- \
  iostat -x 1 5
```

## Resolution

### Immediate Actions

1. **Scale horizontally** if resource-constrained:

```bash
kubectl scale deployment/ecommerce-backend --replicas=8 -n production
```

1. **Clear cache** if stale data causing issues:

```bash
# Clear application cache
curl -X POST http://localhost:8080/actuator/caches/products -d '{"action":"clear"}'

# Clear Redis cache
kubectl exec -it redis-0 -n production -- redis-cli FLUSHDB
```

1. **Kill slow queries** if blocking others:

```bash
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT pg_terminate_backend(pid) 
   FROM pg_stat_activity 
   WHERE state = 'active' 
   AND now() - query_start > interval '30 seconds';"
```

### Root Cause Fixes

#### If caused by slow database queries

1. **Identify slow queries**:

```sql
-- Get slowest queries
SELECT query, calls, total_time, mean_time, max_time,
       stddev_time, rows
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;
```

1. **Analyze query execution plan**:

```sql
EXPLAIN ANALYZE
SELECT o.*, c.name, c.email
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.created_at > NOW() - INTERVAL '7 days';
```

1. **Add missing indexes**:

```sql
-- Create index on frequently queried columns
CREATE INDEX CONCURRENTLY idx_orders_created_at 
ON orders(created_at);

CREATE INDEX CONCURRENTLY idx_orders_customer_id_created_at 
ON orders(customer_id, created_at);

-- Verify index usage
EXPLAIN ANALYZE [your query];
```

1. **Optimize query**:

```java
// Before: N+1 query problem
public List<OrderDTO> getOrders() {
    List<Order> orders = orderRepository.findAll();
    return orders.stream()
        .map(order -> {
            Customer customer = customerRepository.findById(order.getCustomerId());
            return new OrderDTO(order, customer);
        })
        .collect(Collectors.toList());
}

// After: Use JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.createdAt > :date")
List<Order> findRecentOrdersWithCustomer(@Param("date") LocalDateTime date);
```

#### If caused by inefficient caching

1. **Implement caching for frequently accessed data**:

```java
@Service
public class ProductService {
    
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Product findById(String id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
    
    @Cacheable(value = "productList", key = "#category")
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    @CacheEvict(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }
}
```

1. **Configure cache TTL**:

```yaml
spring:
  cache:
    redis:
      time-to-live: 1800000  # 30 minutes
    cache-names:

      - products
      - customers
      - categories

```

1. **Implement cache warming**:

```java
@Component
public class CacheWarmer {
    
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    public void warmCache() {
        // Pre-load frequently accessed data
        List<Product> popularProducts = productRepository.findPopularProducts();
        popularProducts.forEach(product -> 
            cacheManager.getCache("products").put(product.getId(), product)
        );
    }
}
```

#### If caused by external API slowness

1. **Implement timeout and circuit breaker**:

```java
@Service
public class PaymentService {
    
    private final CircuitBreaker circuitBreaker;
    
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
    @Retry(name = "paymentGateway", fallbackMethod = "paymentFallback")
    @TimeLimiter(name = "paymentGateway")
    public CompletableFuture<PaymentResult> processPayment(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> 
            paymentGatewayClient.process(request)
        );
    }
    
    public CompletableFuture<PaymentResult> paymentFallback(PaymentRequest request, Exception ex) {
        // Return cached result or queue for later processing
        return CompletableFuture.completedFuture(
            PaymentResult.pending("Payment queued for processing")
        );
    }
}
```

1. **Configure resilience4j**:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentGateway:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
        permittedNumberOfCallsInHalfOpenState: 3
  
  retry:
    instances:
      paymentGateway:
        maxAttempts: 3
        waitDuration: 1000
        exponentialBackoffMultiplier: 2
  
  timelimiter:
    instances:
      paymentGateway:
        timeoutDuration: 5s
```

#### If caused by inefficient code

1. **Profile application**:

```bash
# Get CPU profile
kubectl exec -it ${POD_NAME} -n production -- \
  async-profiler.sh -d 60 -f /tmp/profile.html 1

# Copy profile locally
kubectl cp production/${POD_NAME}:/tmp/profile.html ./profile.html
```

1. **Optimize hot paths**:

```java
// Before: Inefficient
public List<OrderDTO> getOrders() {
    return orderRepository.findAll().stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
}

// After: Use projection
@Query("SELECT new com.example.OrderDTO(o.id, o.total, c.name) " +
       "FROM Order o JOIN o.customer c")
List<OrderDTO> findAllOrderDTOs();

// Or use database view
@Query(value = "SELECT * FROM order_summary_view", nativeQuery = true)
List<OrderSummaryDTO> findOrderSummaries();
```

1. **Implement pagination**:

```java
@GetMapping("/orders")
public Page<OrderDTO> getOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    return orderService.findOrders(pageable);
}
```

#### If caused by connection pool exhaustion

1. **Increase connection pool size**:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 15
      connection-timeout: 20000
      idle-timeout: 300000
```

1. **Fix connection leaks**:

```java
// Ensure proper resource cleanup
@Transactional
public void processOrder(Order order) {
    try {
        // Process order
        orderRepository.save(order);
    } catch (Exception e) {
        // Handle exception
        throw new OrderProcessingException("Failed to process order", e);
    }
    // Transaction automatically closed
}
```

## Verification

- [ ] API response time < 2s (95th percentile)
- [ ] No timeout errors
- [ ] Database query times normal
- [ ] Cache hit rate > 80%
- [ ] No slow query alerts
- [ ] External API calls within SLA
- [ ] Resource utilization normal
- [ ] User experience improved

### Verification Commands

```bash
# Monitor response times
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq ".measurements[0].value"'

# Check database performance
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c \
  "SELECT max(now() - query_start) as max_duration FROM pg_stat_activity WHERE state = 'active';"

# Verify cache hit rate
curl -s http://localhost:8080/actuator/metrics/cache.gets | jq
```

## Prevention

### 1. Performance Testing

```bash
# Regular load testing
./scripts/load-test.sh --duration=30m --users=500

# Performance regression testing
./scripts/performance-test.sh --baseline=v1.0.0 --current=v1.1.0
```

### 2. Database Optimization

```sql
-- Regular maintenance
VACUUM ANALYZE;
REINDEX DATABASE ecommerce_production;

-- Monitor index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY schemaname, tablename;
```

### 3. Monitoring and Alerting

```yaml
# Set up performance alerts

- alert: APIResponseTimeDegrading

  expr: rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m]) > 1
  for: 10m
  
- alert: DatabaseQuerySlow

  expr: rate(pg_stat_statements_mean_exec_time_seconds[5m]) > 0.5
  for: 5m
```

### 4. Code Review Checklist

- [ ] Database queries optimized
- [ ] Proper indexing in place
- [ ] Caching implemented where appropriate
- [ ] No N+1 query problems
- [ ] Pagination for large datasets
- [ ] Connection pooling configured
- [ ] Timeouts set for external calls
- [ ] Circuit breakers implemented

## Escalation

- **L1 Support**: DevOps team (scaling, cache clearing)
- **L2 Support**: Backend engineering team (query optimization, code fixes)
- **L3 Support**: Database administrator (database tuning)
- **On-Call Engineer**: Check PagerDuty

## Related

- [High CPU Usage](high-cpu-usage.md)
- [Database Connection Issues](database-connection-issues.md)
- [Cache Issues](cache-issues.md)
- [Service Outage](service-outage.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Monthly
