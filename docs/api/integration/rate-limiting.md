# API Rate Limiting

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This document describes the rate limiting strategy for the GenAI Demo API.

---

## Quick Reference

For related API documentation, see:
- [API Overview](../README.md) - Complete API documentation
- [API Security](../security/README.md) - API security measures
- [Integration Guide](README.md) - Integration overview

---

## Rate Limiting Strategy

### Rate Limit Tiers

| Tier | Requests/Minute | Requests/Hour | Requests/Day |
|------|-----------------|---------------|--------------|
| **Anonymous** | 10 | 100 | 1,000 |
| **Authenticated** | 60 | 1,000 | 10,000 |
| **Premium** | 300 | 5,000 | 50,000 |
| **Enterprise** | 1,000 | 20,000 | 200,000 |

### Rate Limit Headers

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1700000000
Retry-After: 30
```

---

## Implementation

### Spring Boot Configuration

```java
@Configuration
public class RateLimitConfiguration {
    
    @Bean
    public RateLimiter rateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new RedisRateLimiter(redisTemplate);
    }
}

@Component
public class RedisRateLimiter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean allowRequest(String clientId, int limit, Duration window) {
        String key = "rate_limit:" + clientId;
        Long current = redisTemplate.opsForValue().increment(key);
        
        if (current == 1) {
            redisTemplate.expire(key, window);
        }
        
        return current <= limit;
    }
}
```

### Rate Limiting Filter

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = extractClientId(request);
        RateLimitTier tier = determineRateLimitTier(request);
        
        if (!rateLimiter.allowRequest(clientId, tier.getLimit(), tier.getWindow())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Limit", String.valueOf(tier.getLimit()));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(getResetTime()));
            response.setHeader("Retry-After", String.valueOf(tier.getWindow().getSeconds()));
            
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests\"}"
            );
            return;
        }
        
        // Add rate limit headers
        addRateLimitHeaders(response, clientId, tier);
        
        filterChain.doFilter(request, response);
    }
    
    private String extractClientId(HttpServletRequest request) {
        // Try API key first
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return "api_key:" + apiKey;
        }
        
        // Try JWT token
        String token = extractJwtToken(request);
        if (token != null) {
            return "user:" + extractUserId(token);
        }
        
        // Fall back to IP address
        return "ip:" + request.getRemoteAddr();
    }
    
    private RateLimitTier determineRateLimitTier(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            return apiKeyService.getTier(apiKey);
        }
        
        String token = extractJwtToken(request);
        if (token != null) {
            return userService.getTier(extractUserId(token));
        }
        
        return RateLimitTier.ANONYMOUS;
    }
}
```

---

## Rate Limit Response

### Success Response

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1700000000
Content-Type: application/json

{
  "data": { ... }
}
```

### Rate Limit Exceeded

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1700000000
Retry-After: 30
Content-Type: application/json

{
  "error": "rate_limit_exceeded",
  "message": "Too many requests. Please try again later.",
  "retryAfter": 30,
  "resetAt": "2024-11-19T10:30:00Z"
}
```

---

## Client Implementation

### Handling Rate Limits

```java
public class ApiClient {
    
    private final RestTemplate restTemplate;
    private final RateLimitHandler rateLimitHandler;
    
    public <T> T makeRequest(String url, Class<T> responseType) {
        try {
            ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
            
            // Track rate limit headers
            rateLimitHandler.updateLimits(
                response.getHeaders().getFirst("X-RateLimit-Limit"),
                response.getHeaders().getFirst("X-RateLimit-Remaining"),
                response.getHeaders().getFirst("X-RateLimit-Reset")
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException.TooManyRequests e) {
            // Handle rate limit exceeded
            String retryAfter = e.getResponseHeaders().getFirst("Retry-After");
            rateLimitHandler.handleRateLimitExceeded(Integer.parseInt(retryAfter));
            
            // Retry after waiting
            return makeRequestWithRetry(url, responseType, Integer.parseInt(retryAfter));
        }
    }
    
    private <T> T makeRequestWithRetry(String url, Class<T> responseType, int retryAfter) {
        try {
            Thread.sleep(retryAfter * 1000);
            return makeRequest(url, responseType);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted", e);
        }
    }
}
```

### JavaScript/TypeScript Client

```typescript
class ApiClient {
  private rateLimitInfo = {
    limit: 0,
    remaining: 0,
    reset: 0
  };

  async makeRequest<T>(url: string): Promise<T> {
    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${this.getToken()}`,
          'X-API-Key': this.getApiKey()
        }
      });

      // Update rate limit info
      this.rateLimitInfo = {
        limit: parseInt(response.headers.get('X-RateLimit-Limit') || '0'),
        remaining: parseInt(response.headers.get('X-RateLimit-Remaining') || '0'),
        reset: parseInt(response.headers.get('X-RateLimit-Reset') || '0')
      };

      if (response.status === 429) {
        const retryAfter = parseInt(response.headers.get('Retry-After') || '60');
        await this.wait(retryAfter * 1000);
        return this.makeRequest<T>(url);
      }

      return response.json();
    } catch (error) {
      console.error('API request failed:', error);
      throw error;
    }
  }

  private wait(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  getRateLimitInfo() {
    return this.rateLimitInfo;
  }
}
```

---

## Best Practices

### For API Consumers

✅ **Do**:
- Monitor rate limit headers
- Implement exponential backoff
- Cache responses when possible
- Use webhooks instead of polling
- Batch requests when available

❌ **Don't**:
- Ignore rate limit headers
- Retry immediately after 429
- Make unnecessary requests
- Poll frequently for updates

### For API Providers

✅ **Do**:
- Provide clear rate limit headers
- Document rate limits clearly
- Offer different tiers
- Implement gradual rate limiting
- Monitor rate limit violations

❌ **Don't**:
- Block clients permanently
- Change limits without notice
- Apply same limits to all endpoints
- Ignore burst traffic patterns

---

## Monitoring

### Rate Limit Metrics

```java
@Component
public class RateLimitMetrics {
    
    private final MeterRegistry meterRegistry;
    
    public void recordRateLimitExceeded(String clientId, String tier) {
        Counter.builder("rate_limit.exceeded")
            .tag("client_id", clientId)
            .tag("tier", tier)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordRateLimitUsage(String clientId, int remaining, int limit) {
        Gauge.builder("rate_limit.usage", () -> 
            (double) (limit - remaining) / limit * 100)
            .tag("client_id", clientId)
            .register(meterRegistry);
    }
}
```

### Alerts

```yaml
# Prometheus Alert Rules
groups:
  - name: rate-limit-alerts
    rules:
      - alert: HighRateLimitUsage
        expr: rate_limit_usage > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate limit usage detected"
          
      - alert: FrequentRateLimitExceeded
        expr: rate(rate_limit_exceeded_total[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Frequent rate limit violations"
```

---

## Upgrading Tiers

### Request Tier Upgrade

```http
POST /api/v1/account/upgrade-tier
Authorization: Bearer <token>
Content-Type: application/json

{
  "requestedTier": "PREMIUM",
  "reason": "Increased usage requirements",
  "estimatedUsage": {
    "requestsPerDay": 25000,
    "peakRequestsPerMinute": 150
  }
}
```

### Response

```json
{
  "status": "approved",
  "newTier": "PREMIUM",
  "effectiveDate": "2024-11-20T00:00:00Z",
  "limits": {
    "requestsPerMinute": 300,
    "requestsPerHour": 5000,
    "requestsPerDay": 50000
  }
}
```

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: API Team
