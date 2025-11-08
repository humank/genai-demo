# Performance Verification and Testing

> **Last Updated**: 2025-10-23  
> **Status**: ✅ Active

## Overview

This document defines the verification and testing strategies for ensuring the e-commerce platform meets performance requirements. It covers load testing, performance monitoring, profiling techniques, and continuous performance validation.

## Testing Strategy

### Test Pyramid for Performance

```text
        /\
       /  \
      / E2E \     5% - End-to-End Performance Tests
     /______\
    /        \
   /Integration\ 15% - Integration Performance Tests
  /____________\
 /              \
/  Unit + Perf   \ 80% - Unit Tests with Performance Assertions
/________________\
```

### Performance Test Types

| Test Type | Purpose | Frequency | Duration | Tools |
|-----------|---------|-----------|----------|-------|
| Load Testing | Verify system handles expected load | Weekly | 30-60 min | JMeter, Gatling |
| Stress Testing | Find breaking points | Monthly | 60-120 min | JMeter, Gatling |
| Spike Testing | Verify sudden traffic handling | Weekly | 15-30 min | JMeter |
| Endurance Testing | Detect memory leaks | Before releases | 24 hours | JMeter |
| Scalability Testing | Verify auto-scaling | Weekly | 30-60 min | JMeter + K8s |

## Load Testing

### JMeter Test Plan Structure

#### Basic Load Test Configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan testname="E-Commerce Load Test">
      <elementProp name="TestPlan.user_defined_variables" elementType="Arguments">
        <collectionProp name="Arguments.arguments">
          <elementProp name="BASE_URL" elementType="Argument">
            <stringProp name="Argument.value">${__P(baseUrl,http://localhost:8080)}</stringProp>
          </elementProp>
          <elementProp name="USERS" elementType="Argument">
            <stringProp name="Argument.value">${__P(users,100)}</stringProp>
          </elementProp>
          <elementProp name="RAMP_UP" elementType="Argument">
            <stringProp name="Argument.value">${__P(rampUp,60)}</stringProp>
          </elementProp>
          <elementProp name="DURATION" elementType="Argument">
            <stringProp name="Argument.value">${__P(duration,1800)}</stringProp>
          </elementProp>
        </collectionProp>
      </elementProp>
    </TestPlan>
    
    <ThreadGroup testname="User Threads">
      <stringProp name="ThreadGroup.num_threads">${USERS}</stringProp>
      <stringProp name="ThreadGroup.ramp_time">${RAMP_UP}</stringProp>
      <stringProp name="ThreadGroup.duration">${DURATION}</stringProp>
      <boolProp name="ThreadGroup.scheduler">true</boolProp>
    </ThreadGroup>
  </hashTree>
</jmeterTestPlan>
```

#### Test Scenarios

**Scenario 1: Browse Products**

```xml
<HTTPSamplerProxy testname="Get Product List">
  <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
  <stringProp name="HTTPSampler.path">/api/v1/products</stringProp>
  <stringProp name="HTTPSampler.method">GET</stringProp>
  
  <ResponseAssertion testname="Response Time Assertion">
    <stringProp name="Assertion.test_field">Assertion.response_time</stringProp>
    <stringProp name="Assertion.test_type">Assertion.duration</stringProp>
    <stringProp name="Assertion.test_string">1000</stringProp>
  </ResponseAssertion>
</HTTPSamplerProxy>
```

**Scenario 2: Add to Cart**

```xml
<HTTPSamplerProxy testname="Add to Cart">
  <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
  <stringProp name="HTTPSampler.path">/api/v1/cart/items</stringProp>
  <stringProp name="HTTPSampler.method">POST</stringProp>
  <boolProp name="HTTPSampler.postBodyRaw">true</boolProp>
  <elementProp name="HTTPsampler.Arguments" elementType="Arguments">
    <collectionProp name="Arguments.arguments">
      <elementProp name="" elementType="HTTPArgument">
        <stringProp name="Argument.value">{"productId":"${productId}","quantity":1}</stringProp>
      </elementProp>
    </collectionProp>
  </elementProp>
</HTTPSamplerProxy>
```

**Scenario 3: Submit Order**

```xml
<HTTPSamplerProxy testname="Submit Order">
  <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
  <stringProp name="HTTPSampler.path">/api/v1/orders</stringProp>
  <stringProp name="HTTPSampler.method">POST</stringProp>
</HTTPSamplerProxy>
```

### Load Test Execution

#### Command Line Execution

```bash
# Run load test with 100 users
jmeter -n -t load-test.jmx \
  -JbaseUrl=https://staging.example.com \
  -Jusers=100 \
  -JrampUp=60 \
  -Jduration=1800 \
  -l results.jtl \
  -e -o report

# Run with different load profiles
# Normal load
jmeter -n -t load-test.jmx -Jusers=100 -Jduration=1800

# Peak load
jmeter -n -t load-test.jmx -Jusers=500 -Jduration=1800

# Stress test
jmeter -n -t load-test.jmx -Jusers=1000 -Jduration=3600
```

#### CI/CD Integration

```yaml
# .github/workflows/performance-test.yml
name: Performance Testing

on:
  schedule:

    - cron: '0 2 * * 1'  # Weekly on Monday 2 AM

  workflow_dispatch:

jobs:
  performance-test:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      
      - name: Setup JMeter

        run: |
          wget https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.5.tgz
          tar -xzf apache-jmeter-5.5.tgz
      
      - name: Run Load Test

        run: |
          apache-jmeter-5.5/bin/jmeter -n \
            -t tests/performance/load-test.jmx \
            -JbaseUrl=${{ secrets.STAGING_URL }} \
            -Jusers=100 \
            -l results.jtl \
            -e -o report
      
      - name: Check Performance Thresholds

        run: |
          python scripts/check-performance-thresholds.py results.jtl
      
      - name: Upload Results

        uses: actions/upload-artifact@v3
        with:
          name: performance-report
          path: report/
```

### Load Test Scenarios

#### Scenario 1: Normal Load

**Objective**: Verify system handles typical business hours traffic

**Configuration**:

- Users: 100 concurrent users
- Ramp-up: 5 minutes
- Duration: 30 minutes
- Think time: 3-5 seconds between requests

**Success Criteria**:

- 95th percentile response time ≤ 1000ms
- Error rate < 0.1%
- CPU utilization < 70%
- Memory utilization < 80%

#### Scenario 2: Peak Load

**Objective**: Verify system handles peak traffic (Black Friday)

**Configuration**:

- Users: 500 concurrent users
- Ramp-up: 10 minutes
- Duration: 30 minutes
- Think time: 2-3 seconds between requests

**Success Criteria**:

- 95th percentile response time ≤ 1500ms
- Error rate < 0.5%
- Auto-scaling triggers appropriately
- System remains stable

#### Scenario 3: Spike Test

**Objective**: Verify system handles sudden traffic spikes

**Configuration**:

- Users: 0 to 500 in 1 minute
- Duration: 10 minutes at peak
- Ramp-down: 5 minutes

**Success Criteria**:

- No failed requests during spike
- Auto-scaling responds within 5 minutes
- System recovers after spike
- Error rate < 1% during spike

#### Scenario 4: Endurance Test

**Objective**: Detect memory leaks and resource exhaustion

**Configuration**:

- Users: 200 concurrent users
- Duration: 24 hours
- Constant load throughout

**Success Criteria**:

- Performance remains stable over 24 hours
- No memory leaks detected
- No resource exhaustion
- Error rate remains < 0.1%

## Performance Monitoring

### Application Performance Monitoring (APM)

#### CloudWatch Metrics

**Custom Metrics**:

```java
@Component
public class PerformanceMetrics {
    
    private final MeterRegistry meterRegistry;
    
    @EventListener
    public void recordApiRequest(HttpRequestEvent event) {
        Timer.builder("api.request.duration")
            .tag("endpoint", event.getEndpoint())
            .tag("method", event.getMethod())
            .tag("status", String.valueOf(event.getStatus()))
            .register(meterRegistry)
            .record(event.getDuration(), TimeUnit.MILLISECONDS);
        
        Counter.builder("api.requests.total")
            .tag("endpoint", event.getEndpoint())
            .tag("status", String.valueOf(event.getStatus()))
            .register(meterRegistry)
            .increment();
    }
}
```

**Dashboard Configuration**:

```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/ApplicationELB", "TargetResponseTime", {"stat": "p95"}],
          [".", "RequestCount", {"stat": "Sum"}],
          [".", "HTTPCode_Target_5XX_Count", {"stat": "Sum"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "API Performance"
      }
    }
  ]
}
```

#### X-Ray Tracing

```java
@Component
@Aspect
public class XRayTracingAspect {
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object traceRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        Segment segment = AWSXRay.beginSegment("api-request");
        
        try {
            segment.putAnnotation("endpoint", getEndpoint(joinPoint));
            segment.putAnnotation("method", getMethod(joinPoint));
            
            Object result = joinPoint.proceed();
            
            segment.putMetadata("response", result);
            return result;
            
        } catch (Exception e) {
            segment.addException(e);
            throw e;
        } finally {
            AWSXRay.endSegment();
        }
    }
}
```

### Database Performance Monitoring

#### RDS Performance Insights

**Key Metrics to Monitor**:

- Database load (average active sessions)
- Top SQL statements by execution time
- Wait events (I/O, CPU, lock)
- Connection count

**Alert Configuration**:

```yaml
DatabaseCPUAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: RDS-High-CPU
    MetricName: CPUUtilization
    Namespace: AWS/RDS
    Statistic: Average
    Period: 300
    EvaluationPeriods: 2
    Threshold: 80
    ComparisonOperator: GreaterThanThreshold
```

#### Slow Query Log Analysis

```sql
-- Enable slow query log
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;  -- 100ms threshold
SET GLOBAL log_queries_not_using_indexes = 'ON';

-- Analyze slow queries
SELECT 
    query_time,
    lock_time,
    rows_examined,
    rows_sent,
    sql_text
FROM mysql.slow_log
WHERE query_time > 0.1
ORDER BY query_time DESC
LIMIT 20;
```

### Frontend Performance Monitoring

#### CloudWatch RUM

```typescript
// Initialize CloudWatch RUM
import { AwsRum } from 'aws-rum-web';

const awsRum = new AwsRum(
  'ecommerce-app',
  '1.0.0',
  'us-east-1',
  {
    sessionSampleRate: 1,
    guestRoleArn: 'arn:aws:iam::123456789012:role/RUM-Monitor',
    identityPoolId: 'us-east-1:xxxxx',
    endpoint: 'https://dataplane.rum.us-east-1.amazonaws.com',
    telemetries: ['performance', 'errors', 'http'],
    allowCookies: true,
    enableXRay: true
  }
);
```

#### Lighthouse CI

```yaml
# lighthouserc.js
module.exports = {
  ci: {
    collect: {
      url: ['http://localhost:3000/'],
      numberOfRuns: 3,
    },
    assert: {
      assertions: {
        'categories:performance': ['error', {minScore: 0.9}],
        'categories:accessibility': ['error', {minScore: 0.9}],
        'first-contentful-paint': ['error', {maxNumericValue: 1500}],
        'largest-contentful-paint': ['error', {maxNumericValue: 2500}],
        'cumulative-layout-shift': ['error', {maxNumericValue: 0.1}],
      },
    },
    upload: {
      target: 'temporary-public-storage',
    },
  },
};
```

## Performance Profiling

### Application Profiling

#### JVM Profiling with Async-Profiler

```bash
# Start profiling
./profiler.sh start -d 60 -f flamegraph.html <pid>

# Profile specific event
./profiler.sh start -e cpu -d 60 <pid>
./profiler.sh start -e alloc -d 60 <pid>

# Generate flame graph
./profiler.sh stop -f flamegraph.html <pid>
```

#### Spring Boot Actuator Profiling

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: metrics,health,prometheus,threaddump,heapdump
  metrics:
    export:
      prometheus:
        enabled: true
```

### Database Profiling

#### Query Execution Plan Analysis

```sql
-- PostgreSQL
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT o.id, o.total_amount, c.name
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.status = 'PENDING'
ORDER BY o.created_date DESC
LIMIT 20;

-- Look for:
-- 1. Sequential scans (should use indexes)
-- 2. High buffer usage
-- 3. Expensive operations (sorts, nested loops)
```

#### Index Usage Analysis

```sql
-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY schemaname, tablename;

-- Check missing indexes
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    seq_tup_read / seq_scan AS avg_seq_tup_read
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;
```

## Performance Regression Testing

### Automated Performance Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPerformanceExtension(maxExecutionTimeMs = 1000, maxMemoryIncreaseMB = 50)
class OrderApiPerformanceTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void should_handle_order_creation_within_performance_budget() {
        // Given
        CreateOrderRequest request = createValidOrderRequest();
        
        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "/api/v1/orders",
            request,
            OrderResponse.class
        );
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(responseTime).isLessThan(1000);  // 1 second budget
    }
    
    @Test
    void should_handle_concurrent_order_requests() throws InterruptedException {
        int numberOfThreads = 50;
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    restTemplate.postForEntity("/api/v1/orders", 
                        createValidOrderRequest(), OrderResponse.class);
                    responseTimes.add(System.currentTimeMillis() - startTime);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Verify 95th percentile
        long p95 = calculatePercentile(responseTimes, 95);
        assertThat(p95).isLessThan(1500);
    }
}
```

### Performance Baseline

```json
{
  "baseline": {
    "version": "1.0.0",
    "date": "2025-10-23",
    "metrics": {
      "api_response_time_p95": 850,
      "database_query_time_p95": 45,
      "cache_hit_rate": 87,
      "throughput": 520
    }
  },
  "thresholds": {
    "api_response_time_p95": {
      "max_regression": 0.10,
      "absolute_max": 1000
    },
    "database_query_time_p95": {
      "max_regression": 0.15,
      "absolute_max": 100
    },
    "cache_hit_rate": {
      "min_value": 0.80
    }
  }
}
```

## Continuous Performance Validation

### Performance Gates in CI/CD

```python
# scripts/check-performance-thresholds.py
import json
import sys

def check_performance_thresholds(results_file, baseline_file):
    with open(results_file) as f:
        results = json.load(f)
    
    with open(baseline_file) as f:
        baseline = json.load(f)
    
    failures = []
    
    # Check response time
    if results['p95_response_time'] > baseline['thresholds']['api_response_time_p95']['absolute_max']:
        failures.append(f"Response time {results['p95_response_time']}ms exceeds threshold")
    
    # Check regression
    regression = (results['p95_response_time'] - baseline['baseline']['metrics']['api_response_time_p95']) / baseline['baseline']['metrics']['api_response_time_p95']
    if regression > baseline['thresholds']['api_response_time_p95']['max_regression']:
        failures.append(f"Response time regression {regression*100:.1f}% exceeds 10% threshold")
    
    # Check error rate
    if results['error_rate'] > 0.01:
        failures.append(f"Error rate {results['error_rate']*100:.2f}% exceeds 1% threshold")
    
    if failures:
        print("Performance test FAILED:")
        for failure in failures:
            print(f"  - {failure}")
        sys.exit(1)
    else:
        print("Performance test PASSED")
        sys.exit(0)
```

### Performance Monitoring Dashboard

**Grafana Dashboard Configuration**:

```json
{
  "dashboard": {
    "title": "Performance Monitoring",
    "panels": [
      {
        "title": "API Response Time (P95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))",
            "legendFormat": "{{endpoint}}"
          }
        ],
        "alert": {
          "conditions": [
            {
              "evaluator": {
                "params": [1.0],
                "type": "gt"
              },
              "operator": {
                "type": "and"
              },
              "query": {
                "params": ["A", "5m", "now"]
              },
              "reducer": {
                "params": [],
                "type": "avg"
              },
              "type": "query"
            }
          ]
        }
      }
    ]
  }
}
```

## Performance Testing Best Practices

### Test Data Management

```java
@Component
public class PerformanceTestDataGenerator {
    
    public void generateTestData(int customerCount, int productCount, int orderCount) {
        // Generate customers
        List<Customer> customers = IntStream.range(0, customerCount)
            .mapToObj(i -> createTestCustomer("customer-" + i))
            .collect(Collectors.toList());
        customerRepository.saveAll(customers);
        
        // Generate products
        List<Product> products = IntStream.range(0, productCount)
            .mapToObj(i -> createTestProduct("product-" + i))
            .collect(Collectors.toList());
        productRepository.saveAll(products);
        
        // Generate orders
        Random random = new Random();
        List<Order> orders = IntStream.range(0, orderCount)
            .mapToObj(i -> createTestOrder(
                customers.get(random.nextInt(customerCount)),
                products.get(random.nextInt(productCount))
            ))
            .collect(Collectors.toList());
        orderRepository.saveAll(orders);
    }
}
```

### Environment Isolation

- Use dedicated performance testing environment
- Match production configuration (instance types, database size)
- Isolate from other testing activities
- Clean state before each test run

### Result Analysis

```python
# scripts/analyze-performance-results.py
import pandas as pd
import matplotlib.pyplot as plt

def analyze_results(results_file):
    df = pd.read_csv(results_file)
    
    # Calculate percentiles
    p50 = df['response_time'].quantile(0.50)
    p95 = df['response_time'].quantile(0.95)
    p99 = df['response_time'].quantile(0.99)
    
    print(f"Response Time Percentiles:")
    print(f"  P50: {p50:.2f}ms")
    print(f"  P95: {p95:.2f}ms")
    print(f"  P99: {p99:.2f}ms")
    
    # Plot response time distribution
    plt.figure(figsize=(10, 6))
    plt.hist(df['response_time'], bins=50)
    plt.xlabel('Response Time (ms)')
    plt.ylabel('Frequency')
    plt.title('Response Time Distribution')
    plt.savefig('response_time_distribution.png')
    
    # Identify slow requests
    slow_requests = df[df['response_time'] > 1000]
    print(f"\nSlow Requests (>1000ms): {len(slow_requests)}")
    print(slow_requests[['endpoint', 'response_time']].head(10))
```

## Related Documentation

- [Performance Overview](overview.md) - High-level performance perspective
- [Performance Requirements](requirements.md) - Specific performance targets
- [Scalability Strategy](scalability.md) - Horizontal scaling approach
- [Optimization Guidelines](optimization.md) - Performance optimization techniques

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-23  
**Owner**: Architecture Team
