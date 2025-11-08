---
adr_number: 050
title: "API Security and Rate Limiting Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [014, 023, 048, 049, 051]
affected_viewpoints: ["functional", "deployment"]
affected_perspectives: ["security", "performance"]
---

# ADR-050: API Security and Rate Limiting Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform's APIs require comprehensive security measures to protect against:

- **Credential Stuffing**: Automated login attempts using leaked credentials
- **Brute Force Attacks**: Repeated password guessing attempts
- **API Abuse**: Excessive requests consuming resources
- **Bot Attacks**: Automated scraping and data harvesting
- **Account Takeover**: Unauthorized access to user accounts
- **Inventory Hoarding**: Bots reserving inventory without purchasing
- **Price Scraping**: Competitors harvesting pricing data

The platform needs multi-layer API security that includes:

- Strong authentication mechanisms
- Multi-level rate limiting
- Bot detection and mitigation
- API key management for third-party integrations
- Request validation and sanitization
- Comprehensive audit logging

### Business Context

**Business Drivers**:

- **Revenue Protection**: Prevent bot-driven inventory hoarding ($50K/month loss)
- **Data Protection**: Protect customer PII and business data
- **Competitive Advantage**: Prevent price scraping by competitors
- **User Experience**: Ensure legitimate users have fast, reliable access
- **Regulatory Compliance**: PCI-DSS, GDPR requirements

**Taiwan-Specific Context**:

- **High Bot Activity**: 30-40% of traffic is bot-driven
- **Credential Stuffing**: Frequent attempts using leaked databases
- **Price Competition**: Aggressive price scraping by competitors
- **Fraud Attempts**: High rate of fraudulent account creation

**Constraints**:

- Must not impact legitimate user experience
- Must support high traffic volumes (10K req/s peak)
- Must integrate with existing JWT authentication
- Budget: Included in overall security budget

### Technical Context

**Current State**:

- JWT-based authentication (ADR-014)
- No rate limiting implemented
- No bot detection
- No API key management for third-parties
- Basic input validation

**Attack Vectors**:

- **Login Endpoint**: Credential stuffing, brute force
- **Search Endpoint**: Price scraping, inventory checking
- **Checkout Endpoint**: Inventory hoarding, fraud attempts
- **API Endpoints**: Excessive requests, data harvesting
- **Account Creation**: Fake account creation for fraud

## Decision Drivers

1. **Security**: Protect against credential stuffing and bot attacks
2. **Performance**: Minimal latency overhead (< 5ms)
3. **User Experience**: No impact on legitimate users
4. **Flexibility**: Support different rate limits per endpoint
5. **Scalability**: Handle 10K req/s peak traffic
6. **Visibility**: Comprehensive logging and monitoring
7. **Cost-Effectiveness**: Use existing infrastructure where possible
8. **Integration**: Seamless integration with JWT authentication

## Considered Options

### Option 1: Multi-Layer Rate Limiting + JWT + CAPTCHA (Recommended)

**Description**: Comprehensive API security with multiple protection layers

**Architecture**:

```mermaid
graph LR
    N1["Request"]
    N2["CloudFront"]
    N1 --> N2
    N3["WAF (Layer 1)"]
    N2 --> N3
    N4["API Gateway (Layer 2)"]
    N3 --> N4
    N5["Application (Layer 3)"]
    N4 --> N5
```

**Protection Layers**:

- **Layer 1 - WAF**: Global and per-IP rate limiting
- **Layer 2 - API Gateway**: Per-user and per-API-key rate limiting
- **Layer 3 - Application**: Endpoint-specific rate limiting and CAPTCHA

**Pros**:

- ✅ **Comprehensive Protection**: Multiple layers of defense
- ✅ **Flexible Rate Limits**: Different limits per endpoint and user type
- ✅ **Bot Detection**: CAPTCHA for suspicious requests
- ✅ **JWT Integration**: Seamless integration with existing auth
- ✅ **API Key Support**: Managed API keys for third-parties
- ✅ **Performance**: < 5ms overhead
- ✅ **Scalability**: Handles high traffic volumes

**Cons**:

- ⚠️ **Complexity**: Multiple rate limiting layers to manage
- ⚠️ **CAPTCHA UX**: May impact user experience for suspicious requests

**Cost**: $0 (using existing infrastructure)

**Risk**: **Low** - Proven approach with multiple production deployments

### Option 2: WAF Rate Limiting Only (Basic)

**Description**: Use only WAF for rate limiting

**Pros**:

- ✅ **Simple**: Single layer of rate limiting
- ✅ **Low Cost**: No additional infrastructure

**Cons**:

- ❌ **Limited Flexibility**: Cannot differentiate by user type
- ❌ **No Bot Detection**: No CAPTCHA or advanced bot detection
- ❌ **No API Key Support**: Cannot manage third-party API keys

**Cost**: $0

**Risk**: **Medium** - Insufficient for comprehensive protection

### Option 3: Third-Party API Security (Cloudflare Bot Management)

**Description**: Use third-party bot management service

**Pros**:

- ✅ **Advanced Bot Detection**: Machine learning-based detection
- ✅ **Managed Service**: Less operational overhead

**Cons**:

- ❌ **High Cost**: $1,000-5,000/month
- ❌ **Vendor Lock-In**: Difficult to migrate
- ❌ **Integration Complexity**: Requires significant changes

**Cost**: $1,000-5,000/month

**Risk**: **Medium** - Vendor dependency and high cost

## Decision Outcome

**Chosen Option**: **Multi-Layer Rate Limiting + JWT + CAPTCHA**

### Rationale

Multi-layer rate limiting was selected for the following reasons:

1. **Comprehensive Protection**: Multiple layers provide defense in depth
2. **Flexibility**: Different rate limits for different endpoints and user types
3. **Cost-Effective**: Uses existing infrastructure (WAF, API Gateway, Application)
4. **Performance**: < 5ms overhead per request
5. **User Experience**: Minimal impact on legitimate users
6. **Bot Detection**: CAPTCHA for suspicious requests
7. **API Key Management**: Support for third-party integrations

**Rate Limiting Strategy**:

**Layer 1 - WAF (Global Limits)**:

- Global: 10,000 req/min (all traffic)
- Per-IP: 2,000 req/min (per source IP)
- Purpose: Prevent volumetric attacks

**Layer 2 - API Gateway (User-Based Limits)**:

- Per-User (Authenticated): 100 req/min
- Per-API-Key (Third-Party): 1,000 req/min
- Purpose: Prevent abuse by authenticated users

**Layer 3 - Application (Endpoint-Specific Limits)**:

- Login: 10 req/min per IP (prevent credential stuffing)
- Search: 100 req/min per IP (prevent scraping)
- Checkout: 20 req/min per user (prevent inventory hoarding)
- Account Creation: 5 req/min per IP (prevent fake accounts)
- Purpose: Protect sensitive endpoints

**Authentication Security**:

- JWT with 15-minute expiration (ADR-014)
- Refresh token rotation
- Multi-factor authentication (MFA) for admin accounts
- Password policy: min 12 chars, complexity requirements
- Account lockout: 5 failed attempts = 15-min lockout

**Bot Detection**:

- CAPTCHA for suspicious login attempts (> 3 failures)
- Device fingerprinting
- Behavioral analysis (request patterns, timing)
- User-Agent validation

**Why Not WAF Only**: Insufficient flexibility for user-based and endpoint-specific rate limiting.

**Why Not Third-Party**: High cost ($1K-5K/month) not justified when we can implement comprehensive protection using existing infrastructure.

## Implementation Plan

### Phase 1: WAF Rate Limiting (Week 1)

- [x] Configure global rate limiting (10,000 req/min)
- [x] Configure per-IP rate limiting (2,000 req/min)
- [x] Test with load testing tools
- [x] Monitor for false positives

### Phase 2: Application-Level Rate Limiting (Week 2)

- [x] Implement rate limiting middleware (Spring Boot)
- [x] Configure endpoint-specific limits
- [x] Implement Redis-based rate limiting (distributed)
- [x] Add rate limit headers (X-RateLimit-Limit, X-RateLimit-Remaining)
- [x] Test with load testing

### Phase 3: CAPTCHA Integration (Week 3)

- [x] Integrate Google reCAPTCHA v3
- [x] Implement CAPTCHA for suspicious login attempts
- [x] Configure CAPTCHA thresholds
- [x] Test CAPTCHA flow
- [x] Monitor CAPTCHA solve rates

### Phase 4: API Key Management (Week 4)

- [x] Implement API key generation and management
- [x] Configure API key rate limiting
- [x] Implement API key rotation
- [x] Add API key documentation
- [x] Onboard third-party partners

### Phase 5: Monitoring and Alerting (Week 5)

- [x] Configure CloudWatch metrics
- [x] Set up rate limiting alerts
- [x] Create monitoring dashboard
- [x] Configure automated response (Lambda)
- [x] Document runbooks

### Rollback Strategy

**Trigger Conditions**:

- False positive rate > 1% (legitimate users blocked)
- Performance degradation > 10ms
- Service outage caused by rate limiting

**Rollback Steps**:

1. Increase rate limits temporarily
2. Disable specific rate limiting rules
3. Investigate and fix issues
4. Re-deploy with corrections

**Rollback Time**: < 15 minutes

## Monitoring and Success Criteria

### Success Metrics

- ✅ **Attack Prevention**: 100% of credential stuffing attempts blocked
- ✅ **False Positive Rate**: < 0.1% legitimate users affected
- ✅ **Performance**: < 5ms latency overhead
- ✅ **Bot Detection**: 95% of bot traffic identified
- ✅ **User Experience**: No complaints from legitimate users
- ✅ **Cost**: No additional infrastructure cost

### Monitoring Plan

**CloudWatch Metrics**:

- `api.rate_limit.exceeded` (count by endpoint)
- `api.authentication.failed` (count)
- `api.captcha.triggered` (count)
- `api.captcha.solved` (count)
- `api.bot.detected` (count)

**Alerts**:

- **P0 Critical**: Rate limit exceeded > 1000/min (potential attack)
- **P1 High**: Failed authentication > 100/min (credential stuffing)
- **P2 Medium**: CAPTCHA solve rate < 80% (potential false positives)
- **P3 Low**: Unusual traffic patterns

**Review Schedule**:

- **Real-Time**: 24/7 monitoring dashboard
- **Daily**: Review rate limiting metrics
- **Weekly**: Analyze attack patterns
- **Monthly**: Optimize rate limits based on traffic patterns

## Consequences

### Positive Consequences

- ✅ **Enhanced Security**: Protection against credential stuffing and bot attacks
- ✅ **Revenue Protection**: Prevent inventory hoarding and fraud
- ✅ **Data Protection**: Prevent price scraping and data harvesting
- ✅ **User Experience**: Minimal impact on legitimate users
- ✅ **Cost-Effective**: No additional infrastructure cost
- ✅ **Scalability**: Handles high traffic volumes
- ✅ **Flexibility**: Different rate limits per endpoint and user type

### Negative Consequences

- ⚠️ **Operational Complexity**: Multiple rate limiting layers to manage
- ⚠️ **CAPTCHA UX**: May impact user experience for suspicious requests
- ⚠️ **False Positive Risk**: Potential for blocking legitimate users

### Technical Debt

**Identified Debt**:

1. Manual rate limit tuning (acceptable initially)
2. Basic bot detection (User-Agent only)
3. No machine learning-based anomaly detection

**Debt Repayment Plan**:

- **Q2 2026**: Implement automated rate limit tuning
- **Q3 2026**: Implement advanced bot detection (device fingerprinting, behavioral analysis)
- **Q4 2026**: Implement machine learning-based anomaly detection

## Related Decisions

- [ADR-014: JWT-Based Authentication Strategy](014-jwt-based-authentication-strategy.md) - Authentication mechanism
- [ADR-023: API Rate Limiting Strategy (Token Bucket vs Leaky Bucket)](023-api-rate-limiting-strategy.md) - Rate limiting algorithm
- [ADR-048: DDoS Protection Strategy (Multi-Layer Defense)](048-ddos-protection-strategy.md) - Overall DDoS protection
- [ADR-049: Web Application Firewall (WAF) Rules and Policies](049-web-application-firewall-rules-and-policies.md) - WAF configuration
- [ADR-051: Input Validation and Sanitization Strategy](051-input-validation-and-sanitization-strategy.md) - Input validation

## Notes

### Rate Limiting Configuration

**Spring Boot Rate Limiting (Bucket4j)**:

```java
@Configuration
public class RateLimitingConfiguration {
    
    @Bean
    public RateLimiter loginRateLimiter() {
        return RateLimiter.builder()
            .limit(Limit.of(10).per(Duration.ofMinutes(1)))
            .build();
    }
    
    @Bean
    public RateLimiter searchRateLimiter() {
        return RateLimiter.builder()
            .limit(Limit.of(100).per(Duration.ofMinutes(1)))
            .build();
    }
}
```

**Redis-Based Distributed Rate Limiting**:

```java
@Component
public class RedisRateLimiter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public boolean isAllowed(String key, int limit, Duration window) {
        String redisKey = "rate_limit:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        
        if (count == 1) {
            redisTemplate.expire(redisKey, window);
        }
        
        return count <= limit;
    }
}
```

### CAPTCHA Configuration

**Google reCAPTCHA v3**:

```yaml
# application.yml
recaptcha:
  site-key: ${RECAPTCHA_SITE_KEY}
  secret-key: ${RECAPTCHA_SECRET_KEY}
  threshold: 0.5  # Score threshold (0.0-1.0)
```

**CAPTCHA Trigger Logic**:

```java
@Service
public class CaptchaService {
    
    public boolean requiresCaptcha(String ipAddress) {
        int failedAttempts = getFailedAttempts(ipAddress);
        return failedAttempts >= 3;
    }
    
    public boolean verifyCaptcha(String token) {
        // Verify with Google reCAPTCHA API
        float score = verifyWithGoogle(token);
        return score >= 0.5;
    }
}
```

### API Key Management

**API Key Generation**:

```java
@Service
public class ApiKeyService {
    
    public ApiKey generateApiKey(String partnerId) {
        String key = UUID.randomUUID().toString();
        String hashedKey = BCrypt.hashpw(key, BCrypt.gensalt());
        
        ApiKey apiKey = new ApiKey(
            partnerId,
            hashedKey,
            LocalDateTime.now().plusYears(1)  // 1-year expiration
        );
        
        return apiKeyRepository.save(apiKey);
    }
}
```

### Cost Breakdown

**Monthly Costs**:

- WAF Rate Limiting: $0 (included in WAF cost)
- Application Rate Limiting: $0 (using existing Redis)
- CAPTCHA: $0 (free tier: 1M assessments/month)
- API Key Management: $0 (application logic)
- **Total**: $0 (no additional cost)

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
