---
title: "Latency Optimization Strategy"
type: "perspective-detail"
category: "location"
stakeholders: ["performance-engineers", "infrastructure-architects", "developers"]
last_updated: "2025-10-24"
version: "1.0"
status: "active"
related_docs:

  - "overview.md"
  - "multi-region.md"
  - "../../perspectives/performance/README.md"
  - "../../viewpoints/deployment/README.md"

tags: ["latency", "performance", "cdn", "caching", "optimization"]
---

# Latency Optimization Strategy

## Overview

This document details the comprehensive latency optimization strategy for the Enterprise E-Commerce Platform, covering CDN configuration, caching strategies, database optimization, and network tuning to ensure optimal performance for users worldwide.

## Latency Targets

### Performance Objectives

| Metric | Target | Measurement |
|--------|--------|-------------|
| Same-Region API Latency | < 200ms | 95th percentile |
| Cross-Region API Latency | < 500ms | 95th percentile |
| CDN Edge Latency | < 50ms | 95th percentile |
| Database Query Latency | < 100ms | 95th percentile |
| Page Load Time | < 2 seconds | 95th percentile |
| Time to First Byte (TTFB) | < 300ms | 95th percentile |

### Regional Latency Targets

```text
User Location → Target Latency
────────────────────────────────
North America → US-EAST-1:     < 50ms
Europe        → EU-WEST-1:     < 50ms
Asia Pacific  → AP-SE-1:       < 100ms
South America → US-EAST-1:     < 150ms
Africa        → EU-WEST-1:     < 200ms
Middle East   → EU-WEST-1:     < 150ms
```

## CDN Strategy

### CloudFront Configuration

**Distribution Setup**:

```text
┌─────────────────────────────────────────────────────────┐
│              CloudFront Distribution                     │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  Edge Locations (400+ globally)                          │
│  ├─ North America: 100+ locations                        │
│  ├─ Europe: 80+ locations                                │
│  ├─ Asia Pacific: 100+ locations                         │
│  ├─ South America: 20+ locations                         │
│  ├─ Africa: 10+ locations                                │
│  └─ Middle East: 10+ locations                           │
│                                                           │
│  Origins:                                                 │
│  ├─ US-EAST-1 (Primary)                                  │
│  ├─ EU-WEST-1 (Secondary)                                │
│  └─ AP-SE-1 (Tertiary)                                   │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

**Cache Behaviors**:

```yaml
# Static Assets (Images, CSS, JS)
PathPattern: /static/*
CacheTTL: 86400 # 24 hours
Compress: true
ViewerProtocolPolicy: redirect-to-https

# API Responses (Cacheable)
PathPattern: /api/v1/products/*
CacheTTL: 300 # 5 minutes
Compress: true
ForwardHeaders: Authorization
ViewerProtocolPolicy: https-only

# Dynamic Content (No Cache)
PathPattern: /api/v1/orders/*
CacheTTL: 0
Compress: true
ForwardHeaders: Authorization, Cookie
ViewerProtocolPolicy: https-only
```

**Origin Failover**:

```text
Primary Origin: US-EAST-1
  ├─ Health Check: /health
  ├─ Timeout: 10 seconds
  └─ Failure Threshold: 3 attempts

Secondary Origin: EU-WEST-1
  ├─ Activated on primary failure
  ├─ Health Check: /health
  └─ Automatic failback when primary recovers
```

### Content Optimization

**Image Optimization**:

- WebP format for modern browsers
- JPEG fallback for legacy browsers
- Responsive images with srcset
- Lazy loading for below-the-fold images
- Image compression (80% quality)

**JavaScript Optimization**:

- Minification and bundling
- Code splitting by route
- Tree shaking to remove unused code
- Async/defer loading for non-critical scripts
- Service worker for offline caching

**CSS Optimization**:

- Minification and bundling
- Critical CSS inlined
- Non-critical CSS loaded asynchronously
- Remove unused CSS

## Caching Strategy

### Multi-Layer Caching

```text
┌─────────────────────────────────────────────────────────┐
│                  Caching Layers                          │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  Layer 1: Browser Cache (Client-Side)                   │
│  ├─ Static Assets: 1 year                                │
│  ├─ API Responses: 5 minutes                             │
│  └─ Service Worker: Offline support                      │
│                                                           │
│  Layer 2: CDN Edge Cache (CloudFront)                    │
│  ├─ Static Assets: 24 hours                              │
│  ├─ Product Catalog: 5 minutes                           │
│  └─ Hit Rate Target: > 90%                               │
│                                                           │
│  Layer 3: Application Cache (Redis)                      │
│  ├─ Session Data: 30 minutes                             │
│  ├─ Product Data: 15 minutes                             │
│  ├─ User Preferences: 1 hour                             │
│  └─ Shopping Cart: 24 hours                              │
│                                                           │
│  Layer 4: Database Query Cache                           │
│  ├─ Query Results: 5 minutes                             │
│  ├─ Aggregations: 15 minutes                             │
│  └─ Read Replicas: Near real-time                        │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

### Redis Caching Configuration

**Cluster Setup**:

```yaml
# Production Redis Configuration
cluster:
  mode: cluster
  nodes: 3
  replicas: 1
  
memory:
  maxmemory: 8gb
  policy: allkeys-lru
  
persistence:
  enabled: true
  strategy: aof
  fsync: everysec
  
performance:
  tcp-backlog: 511
  timeout: 300
  tcp-keepalive: 300
```

**Cache Key Strategy**:

```java
@Service
public class CacheKeyGenerator {
    
    public String generateProductKey(String productId) {
        return String.format("product:%s", productId);
    }
    
    public String generateUserSessionKey(String userId) {
        return String.format("session:%s", userId);
    }
    
    public String generateCartKey(String userId) {
        return String.format("cart:%s", userId);
    }
    
    public String generateCatalogKey(String category, int page) {
        return String.format("catalog:%s:page:%d", category, page);
    }
}
```

**Cache Invalidation**:

```java
@Service
public class CacheInvalidationService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        // Invalidate product cache
        String productKey = "product:" + event.getProductId();
        redisTemplate.delete(productKey);
        
        // Invalidate catalog cache
        String catalogPattern = "catalog:*";
        redisTemplate.delete(redisTemplate.keys(catalogPattern));
    }
    
    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Invalidate cart cache
        String cartKey = "cart:" + event.getCustomerId();
        redisTemplate.delete(cartKey);
    }
}
```

## Database Optimization

### Read Replica Strategy

**Configuration**:

```text
Primary Database (US-EAST-1)
  ├─ Writes: All write operations
  ├─ Reads: 20% of read operations
  └─ Replication Lag Target: 0ms
  
Read Replica 1 (EU-WEST-1)
  ├─ Reads: 40% of read operations
  ├─ Replication Lag Target: < 1 second
  └─ Promotion Ready: Yes
  
Read Replica 2 (AP-SE-1)
  ├─ Reads: 40% of read operations
  ├─ Replication Lag Target: < 2 seconds
  └─ Promotion Ready: Yes
```

**Query Optimization**:

```sql
-- Index Strategy
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_orders_customer ON orders(customer_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders(status, created_at DESC);

-- Composite Indexes for Common Queries
CREATE INDEX idx_products_category_price ON products(category_id, price);
CREATE INDEX idx_orders_customer_status ON orders(customer_id, status, created_at DESC);

-- Partial Indexes for Specific Conditions
CREATE INDEX idx_active_products ON products(category_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_pending_orders ON orders(customer_id) WHERE status = 'PENDING';
```

**Connection Pooling**:

```yaml
# HikariCP Configuration
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 20000
  idle-timeout: 300000
  max-lifetime: 1200000
  leak-detection-threshold: 60000
```

### Query Caching

**Application-Level Query Cache**:

```java
@Service
public class ProductQueryService {
    
    @Cacheable(value = "products", key = "#productId")
    public Product findById(String productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
    }
    
    @Cacheable(value = "product-catalog", key = "#category + '-' + #page")
    public Page<Product> findByCategory(String category, int page) {
        return productRepository.findByCategory(category, PageRequest.of(page, 20));
    }
}
```

## Network Optimization

### TCP Optimization

**Kernel Parameters**:

```bash
# /etc/sysctl.conf
net.core.rmem_max = 134217728
net.core.wmem_max = 134217728
net.ipv4.tcp_rmem = 4096 87380 67108864
net.ipv4.tcp_wmem = 4096 65536 67108864
net.ipv4.tcp_congestion_control = bbr
net.ipv4.tcp_fastopen = 3
```

### HTTP/2 and HTTP/3

**Configuration**:

```nginx
# Nginx Configuration
http {
    # Enable HTTP/2
    listen 443 ssl http2;
    
    # Enable HTTP/3 (QUIC)
    listen 443 quic reuseport;
    add_header Alt-Svc 'h3=":443"; ma=86400';
    
    # Connection optimization
    keepalive_timeout 65;
    keepalive_requests 100;
    
    # Compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript;
}
```

### DNS Optimization

**Route 53 Configuration**:

```yaml
# Latency-Based Routing
routing_policy: latency
health_check:
  enabled: true
  interval: 30
  failure_threshold: 3
  
regions:

  - name: us-east-1

    weight: 50
    health_check: /health
  
  - name: eu-west-1

    weight: 30
    health_check: /health
  
  - name: ap-southeast-1

    weight: 20
    health_check: /health
```

## API Optimization

### GraphQL Optimization

**Query Batching**:

```graphql
# Single Request with Multiple Queries
query BatchedQueries {
  product1: product(id: "prod-1") {
    id
    name
    price
  }
  
  product2: product(id: "prod-2") {
    id
    name
    price
  }
  
  categories {
    id
    name
  }
}
```

**DataLoader for N+1 Prevention**:

```java
@Component
public class ProductDataLoader implements BatchLoader<String, Product> {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Override
    public CompletionStage<List<Product>> load(List<String> productIds) {
        return CompletableFuture.supplyAsync(() -> 
            productRepository.findAllById(productIds)
        );
    }
}
```

### REST API Optimization

**Response Compression**:

```java
@Configuration
public class CompressionConfiguration {
    
    @Bean
    public FilterRegistrationBean<GzipFilter> gzipFilter() {
        FilterRegistrationBean<GzipFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipFilter());
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
```

**Pagination**:

```java
@GetMapping("/products")
public ResponseEntity<Page<ProductDto>> getProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    
    Page<Product> products = productService.findAll(
        PageRequest.of(page, size, Sort.by("createdAt").descending())
    );
    
    return ResponseEntity.ok(products.map(ProductDto::from));
}
```

**Field Selection**:

```java
@GetMapping("/products/{id}")
public ResponseEntity<ProductDto> getProduct(
        @PathVariable String id,
        @RequestParam(required = false) String fields) {
    
    Product product = productService.findById(id);
    
    if (fields != null) {
        // Return only requested fields
        return ResponseEntity.ok(ProductDto.fromWithFields(product, fields));
    }
    
    return ResponseEntity.ok(ProductDto.from(product));
}
```

## Monitoring and Measurement

### Latency Monitoring

**CloudWatch Metrics**:

```yaml
metrics:

  - name: APILatency

    namespace: ECommerce/API
    dimensions:

      - Region
      - Endpoint

    statistics:

      - Average
      - p50
      - p95
      - p99

    period: 60
  
  - name: DatabaseLatency

    namespace: ECommerce/Database
    dimensions:

      - Region
      - QueryType

    statistics:

      - Average
      - p95
      - p99

    period: 60
```

**Real User Monitoring (RUM)**:

```javascript
// Frontend Performance Monitoring
window.addEventListener('load', () => {
  const perfData = window.performance.timing;
  const pageLoadTime = perfData.loadEventEnd - perfData.navigationStart;
  const connectTime = perfData.responseEnd - perfData.requestStart;
  const renderTime = perfData.domComplete - perfData.domLoading;
  
  // Send to analytics
  analytics.track('PagePerformance', {
    pageLoadTime,
    connectTime,
    renderTime,
    region: getUserRegion(),
    page: window.location.pathname
  });
});
```

### Performance Testing

**Load Testing Script**:

```bash
#!/bin/bash
# scripts/load-test-latency.sh

# Test US-EAST-1
echo "Testing US-EAST-1..."
ab -n 1000 -c 10 https://api-us-east-1.example.com/api/v1/products

# Test EU-WEST-1
echo "Testing EU-WEST-1..."
ab -n 1000 -c 10 https://api-eu-west-1.example.com/api/v1/products

# Test AP-SE-1
echo "Testing AP-SE-1..."
ab -n 1000 -c 10 https://api-ap-southeast-1.example.com/api/v1/products
```

**Synthetic Monitoring**:

```yaml
# CloudWatch Synthetics Canary
canary:
  name: latency-monitor
  schedule: rate(5 minutes)
  
  script: |
    const synthetics = require('Synthetics');
    
    const apiTest = async function () {
      const regions = ['us-east-1', 'eu-west-1', 'ap-southeast-1'];
      
      for (const region of regions) {
        const startTime = Date.now();
        const response = await synthetics.executeHttpStep(
          `Test ${region}`,
          `https://api-${region}.example.com/api/v1/health`
        );
        const latency = Date.now() - startTime;
        
        await synthetics.addUserAgentHeader();
        await synthetics.takeScreenshot(`${region}-response`, 'result');
        
        // Verify latency target
        if (latency > 500) {
          throw new Error(`High latency in ${region}: ${latency}ms`);
        }
      }
    };
    
    exports.handler = async () => {
      return await apiTest();
    };
```

## Best Practices

### Development Best Practices

1. **Minimize Round Trips**:
   - Batch API requests when possible
   - Use GraphQL for flexible data fetching
   - Implement pagination for large datasets

2. **Optimize Queries**:
   - Use indexes for frequently queried fields
   - Avoid N+1 query problems
   - Use query result caching

3. **Reduce Payload Size**:
   - Compress responses (gzip/brotli)
   - Return only requested fields
   - Use efficient data formats (Protocol Buffers for internal APIs)

4. **Implement Caching**:
   - Cache at multiple layers
   - Use appropriate TTLs
   - Implement cache invalidation strategies

### Infrastructure Best Practices

1. **Use CDN Effectively**:
   - Cache static assets at edge locations
   - Use origin shield to reduce origin load
   - Implement cache warming for popular content

2. **Optimize Network**:
   - Use HTTP/2 or HTTP/3
   - Enable TCP Fast Open
   - Use BBR congestion control

3. **Database Optimization**:
   - Use read replicas in each region
   - Implement connection pooling
   - Monitor and optimize slow queries

4. **Monitor Continuously**:
   - Track latency metrics by region
   - Set up alerts for latency spikes
   - Conduct regular performance testing

## Troubleshooting

### High Latency Diagnosis

**Step 1: Identify Layer**:

```bash
# Check CDN latency
curl -w "@curl-format.txt" -o /dev/null -s https://cdn.example.com/static/logo.png

# Check API latency
curl -w "@curl-format.txt" -o /dev/null -s https://api.example.com/api/v1/health

# Check database latency
psql -h db.example.com -c "EXPLAIN ANALYZE SELECT * FROM products LIMIT 10;"
```

**Step 2: Analyze Metrics**:

```bash
# CloudWatch metrics
aws cloudwatch get-metric-statistics \
  --namespace ECommerce/API \
  --metric-name APILatency \
  --dimensions Name=Region,Value=us-east-1 \
  --start-time 2025-10-24T00:00:00Z \
  --end-time 2025-10-24T23:59:59Z \
  --period 300 \
  --statistics Average,Maximum
```

**Step 3: Common Issues and Solutions**:

| Issue | Symptom | Solution |
|-------|---------|----------|
| CDN Cache Miss | High TTFB | Increase cache TTL, implement cache warming |
| Database Slow Query | High query latency | Add indexes, optimize query, use caching |
| Network Congestion | High cross-region latency | Use regional endpoints, implement retry logic |
| Connection Pool Exhaustion | Timeout errors | Increase pool size, optimize connection usage |
| Memory Pressure | Slow response times | Scale up instances, optimize memory usage |

## Related Documentation

- [Overview](overview.md) - Location Perspective overview
- [Multi-Region Deployment](multi-region.md) - Regional architecture
- [Performance Perspective](../../perspectives/performance/README.md) - Performance optimization
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Infrastructure details

---

**Document Status**: ✅ Complete  
**Review Date**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
