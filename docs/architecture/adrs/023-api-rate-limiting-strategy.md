---
adr_number: 023
title: "API Rate Limiting Strategy (Token Bucket vs Leaky Bucket)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [004, 009, 022, 050]
affected_viewpoints: ["functional", "deployment", "operational"]
affected_perspectives: ["performance", "security", "availability"]
---

# ADR-023: API Rate Limiting Strategy (Token Bucket vs Leaky Bucket)

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform needs rate limiting to:

- Protect backend services from overload and abuse
- Ensure fair resource allocation among users
- Prevent DDoS attacks and API abuse
- Maintain system stability during traffic spikes
- Support different rate limits for different user tiers (free, premium, enterprise)
- Provide graceful degradation under high load
- Enable burst traffic handling for legitimate use cases

### Business Context

**Business Drivers**:

- Protect system from malicious traffic (DDoS, scraping)
- Ensure fair usage across all customers
- Support tiered pricing model (different limits per tier)
- Maintain 99.9% availability during traffic spikes
- Prevent resource exhaustion from single user
- Enable burst capacity for legitimate peak usage

**Business Constraints**:

- Must not impact legitimate user experience
- Different limits for different API endpoints
- Support for API key-based rate limiting
- Real-time rate limit feedback to clients
- Cost-effective implementation

### Technical Context

**Current State**:

- RESTful API design (ADR-009)
- Redis distributed caching (ADR-004)
- Distributed locking (ADR-022)
- API security strategy (ADR-050)
- Multiple application instances (horizontal scaling)

**Requirements**:

- Multi-level rate limiting (global, per-user, per-IP, per-endpoint)
- Support for burst traffic (allow temporary spikes)
- Distributed rate limiting across instances
- Low latency (< 5ms overhead)
- Real-time limit tracking and feedback
- Configurable limits per user tier
- Rate limit headers in responses

## Decision Drivers

1. **Burst Handling**: Allow legitimate burst traffic
2. **Fairness**: Prevent single user from monopolizing resources
3. **Performance**: < 5ms overhead per request
4. **Flexibility**: Different limits for different endpoints/users
5. **Distributed**: Work across multiple instances
6. **User Experience**: Clear feedback on rate limits
7. **Cost**: Leverage existing infrastructure
8. **Security**: Protect against DDoS and abuse

## Considered Options

### Option 1: Token Bucket Algorithm (Recommended)

**Description**: Users accumulate tokens at a fixed rate, consume tokens per request, allows burst traffic

**Pros**:

- ✅ Allows burst traffic (accumulated tokens)
- ✅ Flexible rate control (different token costs)
- ✅ Better user experience (smooth traffic)
- ✅ Industry standard (AWS, Google, Stripe)
- ✅ Simple to implement with Redis
- ✅ Supports different user tiers easily
- ✅ Predictable behavior
- ✅ Low latency (< 5ms)

**Cons**:

- ⚠️ Can allow large bursts if bucket is full
- ⚠️ Requires token refill logic
- ⚠️ More complex than fixed window

**Cost**:

- Development: 5 person-days
- Infrastructure: $0 (uses existing Redis)
- Maintenance: Low

**Risk**: **Low** - Well-understood algorithm

**Implementation Complexity**: Medium

### Option 2: Leaky Bucket Algorithm

**Description**: Requests enter a queue, processed at fixed rate, excess requests overflow

**Pros**:

- ✅ Smooth, constant output rate
- ✅ Prevents burst traffic completely
- ✅ Simple conceptual model
- ✅ Predictable resource usage

**Cons**:

- ❌ No burst traffic support (poor UX)
- ❌ Requests queued (increased latency)
- ❌ Queue management complexity
- ❌ Memory overhead for queue
- ❌ Harder to implement distributed
- ❌ Less flexible for different endpoints

**Cost**:

- Development: 8 person-days
- Infrastructure: $0 (uses existing Redis)
- Maintenance: Medium

**Risk**: **Medium** - Queue management complexity

**Implementation Complexity**: High

### Option 3: Fixed Window Counter

**Description**: Count requests in fixed time windows (e.g., per minute)

**Pros**:

- ✅ Very simple to implement
- ✅ Low memory usage
- ✅ Fast (< 2ms)
- ✅ Easy to understand

**Cons**:

- ❌ Burst at window boundaries (2x limit possible)
- ❌ Unfair (early requests get priority)
- ❌ Poor user experience
- ❌ No burst handling

**Cost**:

- Development: 2 person-days
- Infrastructure: $0
- Maintenance: Low

**Risk**: **Medium** - Boundary burst problem

**Implementation Complexity**: Low

### Option 4: Sliding Window Log

**Description**: Track timestamp of each request, count requests in sliding window

**Pros**:

- ✅ Accurate rate limiting
- ✅ No boundary burst problem
- ✅ Fair distribution

**Cons**:

- ❌ High memory usage (store all timestamps)
- ❌ Expensive to compute (scan all timestamps)
- ❌ Doesn't scale well
- ❌ No burst support

**Cost**:

- Development: 6 person-days
- Infrastructure: Higher Redis memory
- Maintenance: High

**Risk**: **High** - Scalability concerns

**Implementation Complexity**: High

## Decision Outcome

**Chosen Option**: **Token Bucket Algorithm**

### Rationale

Token Bucket was selected for the following reasons:

1. **Burst Support**: Allows legitimate burst traffic (accumulated tokens)
2. **User Experience**: Smooth traffic handling, better than strict limits
3. **Industry Standard**: Used by AWS, Google Cloud, Stripe, GitHub
4. **Flexibility**: Easy to configure different limits per endpoint/user
5. **Performance**: Low latency (< 5ms) with Redis implementation
6. **Distributed**: Works well across multiple instances
7. **Cost-Effective**: Uses existing Redis infrastructure
8. **Proven**: Battle-tested in production systems

**Rate Limiting Strategy**:

**Multi-Level Limits**:

- **Global**: 10,000 requests/minute (system-wide)
- **Per-User**: 100 requests/minute (authenticated users)
- **Per-IP**: 1,000 requests/minute (anonymous users)
- **Per-Endpoint Sensitive**: 10 requests/minute (payment, admin)

**User Tiers**:

- **Free**: 100 req/min, burst 150
- **Premium**: 500 req/min, burst 750
- **Enterprise**: 2,000 req/min, burst 3,000

**Token Bucket Parameters**:

- **Bucket Capacity**: 1.5x rate limit (allows 50% burst)
- **Refill Rate**: Rate limit per minute
- **Token Cost**: 1 token per request (can vary by endpoint)

**Why Not Leaky Bucket**: No burst support, poor user experience, higher complexity.

**Why Not Fixed Window**: Boundary burst problem, unfair distribution.

**Why Not Sliding Window Log**: High memory usage, doesn't scale well.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Implement rate limiting logic | Code examples, library support |
| Operations Team | Low | Monitor rate limit metrics | Dashboards and alerts |
| End Users | Low | May see 429 errors if exceeded | Clear error messages, retry-after headers |
| API Consumers | Medium | Need to handle rate limits | Documentation, SDKs with retry logic |
| Business | Positive | Protected from abuse, fair usage | N/A |
| Security Team | Positive | DDoS protection | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All API endpoints (rate limiting middleware)
- Authentication layer (user tier identification)
- Infrastructure layer (Redis rate limit storage)
- API Gateway (rate limit enforcement)
- Monitoring and alerting (rate limit metrics)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positives | Medium | Medium | Generous burst capacity, monitoring |
| Redis unavailability | Low | High | Fallback to in-memory, circuit breaker |
| Distributed clock skew | Low | Low | Use Redis time, not local time |
| Rate limit bypass | Low | High | Multiple enforcement layers |
| Performance impact | Low | Medium | Optimize Redis operations, caching |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Core Implementation (Week 1-2)

- [x] Implement Token Bucket algorithm with Redis
- [x] Create rate limiter service
- [x] Add rate limit middleware
- [x] Implement multi-level rate limiting
- [x] Add rate limit headers to responses

### Phase 2: User Tier Support (Week 3)

- [x] Implement user tier identification
- [x] Configure limits per tier
- [x] Add API key-based rate limiting
- [x] Implement endpoint-specific limits
- [x] Add rate limit bypass for internal services

### Phase 3: Monitoring & Feedback (Week 4)

- [x] Add rate limit metrics
- [x] Create rate limit dashboards
- [x] Implement rate limit alerts
- [x] Add rate limit logging
- [x] Create rate limit documentation

### Phase 4: Optimization (Week 5)

- [x] Performance testing and tuning
- [x] Optimize Redis operations
- [x] Add rate limit caching
- [x] Implement graceful degradation
- [x] Load testing with rate limits

### Rollback Strategy

**Trigger Conditions**:

- Rate limiter latency > 10ms
- False positive rate > 5%
- Redis errors > 1%
- User complaints > threshold

**Rollback Steps**:

1. Disable rate limiting middleware
2. Allow all requests through
3. Investigate and fix issues
4. Re-enable with higher limits
5. Gradually tighten limits

**Rollback Time**: < 15 minutes

## Monitoring and Success Criteria

### Success Metrics

- ✅ Rate limiter latency < 5ms (95th percentile)
- ✅ False positive rate < 1%
- ✅ DDoS attack mitigation > 99%
- ✅ System availability maintained at 99.9%
- ✅ User complaints < 0.1% of requests
- ✅ Rate limit bypass attempts detected

### Monitoring Plan

**Application Metrics**:

```java
@Component
public class RateLimitMetrics {
    private final Counter rateLimitExceeded;
    private final Counter rateLimitAllowed;
    private final Timer rateLimitCheckTime;
    private final Gauge activeRateLimits;
    
    // Track rate limit performance
}
```

**CloudWatch Metrics**:

- Rate limit checks per second
- Rate limit exceeded count
- Rate limit check latency
- Active rate limits by user/IP
- Burst usage percentage
- Token bucket fill rate

**Alerts**:

- Rate limit check latency > 10ms
- Rate limit exceeded spike (> 100/min)
- Redis rate limit errors > 1%
- Burst capacity exhausted > 80%
- Suspicious rate limit patterns

**Review Schedule**:

- Daily: Check rate limit metrics
- Weekly: Review rate limit patterns
- Monthly: Adjust limits based on usage
- Quarterly: Rate limit strategy review

## Consequences

### Positive Consequences

- ✅ **Protection**: Prevents DDoS and API abuse
- ✅ **Fairness**: Ensures fair resource allocation
- ✅ **Stability**: Maintains system stability under load
- ✅ **Flexibility**: Supports different user tiers
- ✅ **User Experience**: Allows burst traffic
- ✅ **Cost Control**: Prevents resource exhaustion
- ✅ **Security**: Additional security layer

### Negative Consequences

- ⚠️ **Complexity**: Additional middleware layer
- ⚠️ **Latency**: 2-5ms overhead per request
- ⚠️ **False Positives**: Legitimate users may be limited
- ⚠️ **Monitoring**: Need to track rate limit metrics
- ⚠️ **Documentation**: Users need to understand limits

### Technical Debt

**Identified Debt**:

1. Static rate limits (can be dynamic based on load)
2. Simple token cost (can vary by endpoint complexity)
3. No automatic limit adjustment (future enhancement)

**Debt Repayment Plan**:

- **Q2 2026**: Implement dynamic rate limits based on system load
- **Q3 2026**: Add endpoint-specific token costs
- **Q4 2026**: Implement ML-based anomaly detection

## Related Decisions

- [ADR-009: RESTful API Design with OpenAPI 3.0](009-restful-api-design-with-openapi.md) - Rate limits in API design
- [ADR-022: Distributed Locking with Redis](022-distributed-locking-with-redis.md) - Redisson for distributed operations

## Notes

### Token Bucket Implementation

```java
@Service
public class TokenBucketRateLimiter {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public boolean tryConsume(String key, int tokens, int capacity, int refillRate) {
        RBucket<TokenBucket> bucket = redissonClient.getBucket("rate:limit:" + key);
        
        TokenBucket tokenBucket = bucket.get();
        if (tokenBucket == null) {
            tokenBucket = new TokenBucket(capacity, refillRate);
        }
        
        // Refill tokens based on time elapsed
        long now = System.currentTimeMillis();
        long timePassed = now - tokenBucket.getLastRefillTime();
        int tokensToAdd = (int) (timePassed * refillRate / 60000); // per minute
        
        tokenBucket.refill(tokensToAdd, capacity);
        tokenBucket.setLastRefillTime(now);
        
        // Try to consume tokens
        boolean allowed = tokenBucket.tryConsume(tokens);
        
        // Save updated bucket
        bucket.set(tokenBucket, 1, TimeUnit.HOURS);
        
        return allowed;
    }
}

@Data
public class TokenBucket {
    private int tokens;
    private int capacity;
    private int refillRate;
    private long lastRefillTime;
    
    public TokenBucket(int capacity, int refillRate) {
        this.tokens = capacity;
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.lastRefillTime = System.currentTimeMillis();
    }
    
    public void refill(int tokensToAdd, int capacity) {
        this.tokens = Math.min(this.tokens + tokensToAdd, capacity);
    }
    
    public boolean tryConsume(int tokens) {
        if (this.tokens >= tokens) {
            this.tokens -= tokens;
            return true;
        }
        return false;
    }
}
```

### Rate Limit Middleware

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    @Autowired
    private TokenBucketRateLimiter rateLimiter;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String userId = extractUserId(request);
        String endpoint = request.getRequestURI();
        
        // Get rate limit configuration
        RateLimitConfig config = getRateLimitConfig(userId, endpoint);
        
        // Check rate limit
        String key = String.format("%s:%s", userId, endpoint);
        boolean allowed = rateLimiter.tryConsume(
            key, 
            1, // tokens to consume
            config.getCapacity(), 
            config.getRefillRate()
        );
        
        if (allowed) {
            // Add rate limit headers
            response.addHeader("X-RateLimit-Limit", String.valueOf(config.getRefillRate()));
            response.addHeader("X-RateLimit-Remaining", String.valueOf(getRemainingTokens(key)));
            response.addHeader("X-RateLimit-Reset", String.valueOf(getResetTime(key)));
            
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            response.setStatus(429); // Too Many Requests
            response.addHeader("Retry-After", String.valueOf(getRetryAfter(key)));
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
        }
    }
}
```

### Rate Limit Configuration

```yaml
# application.yml
rate-limit:
  global:
    capacity: 15000  # 1.5x of 10000 req/min
    refill-rate: 10000  # 10000 req/min
  
  tiers:
    free:
      capacity: 150  # 1.5x of 100 req/min
      refill-rate: 100
    premium:
      capacity: 750  # 1.5x of 500 req/min
      refill-rate: 500
    enterprise:
      capacity: 3000  # 1.5x of 2000 req/min
      refill-rate: 2000
  
  endpoints:
    /api/v1/orders:
      capacity: 150
      refill-rate: 100
    /api/v1/payments:
      capacity: 15  # 1.5x of 10 req/min (sensitive)
      refill-rate: 10
    /api/v1/admin:
      capacity: 15
      refill-rate: 10
```

### Rate Limit Response Headers

```text
HTTP/1.1 200 OK
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 75
X-RateLimit-Reset: 1640000000

HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1640000060
Retry-After: 60
```

### Rate Limit Key Strategy

```text
Pattern: {type}:{identifier}:{endpoint}

Examples:

- user:CUST-123:/api/v1/orders
- ip:192.168.1.1:/api/v1/products
- apikey:key-abc123:/api/v1/data
- global:system:/api/v1/*

```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
