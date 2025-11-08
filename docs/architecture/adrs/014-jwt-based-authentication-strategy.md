---
adr_number: 014
title: "JWT-Based Authentication Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [015, 052]
affected_viewpoints: ["functional", "deployment", "operational"]
affected_perspectives: ["security", "performance"]
---

# ADR-014: JWT-Based Authentication Strategy

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a secure, scalable, and stateless authentication mechanism that can:

- Support both web and mobile clients
- Scale horizontally without session affinity
- Enable microservices architecture with distributed authentication
- Provide fine-grained access control
- Support token refresh without re-authentication
- Minimize database lookups for authentication
- Enable single sign-on (SSO) capabilities

### Business Context

**Business Drivers**:

- Multi-channel access (web, mobile, API partners)
- Expected 100K+ concurrent users at peak
- 24/7 availability requirement
- Need for API access by third-party integrations
- Regulatory compliance (GDPR, data protection)

**Constraints**:

- Must support stateless authentication for horizontal scaling
- Token expiration must balance security and user experience
- Must integrate with existing Spring Security framework
- Budget: No additional licensing costs preferred

### Technical Context

**Current State**:

- Spring Boot 3.4.5 with Spring Security
- Microservices architecture with multiple services
- AWS EKS deployment with auto-scaling
- No existing authentication system (greenfield)

**Requirements**:

- Stateless authentication (no server-side sessions)
- Support for role-based access control (RBAC)
- Token expiration and refresh mechanism
- Secure token storage and transmission
- Audit trail for authentication events
- Support for token revocation

## Decision Drivers

1. **Scalability**: Must support horizontal scaling without session affinity
2. **Performance**: Minimize authentication overhead (< 10ms per request)
3. **Security**: Industry-standard security practices
4. **Statelessness**: Enable true stateless microservices
5. **Developer Experience**: Easy to implement and test
6. **Standards Compliance**: Use widely adopted standards
7. **Cost**: No additional licensing fees
8. **Flexibility**: Support multiple client types

## Considered Options

### Option 1: JWT (JSON Web Tokens) with RS256

**Description**: Stateless tokens signed with RSA asymmetric keys

**Pros**:

- ✅ Truly stateless - no database lookup needed
- ✅ Horizontal scaling friendly
- ✅ Industry standard (RFC 7519)
- ✅ Self-contained (includes user info and permissions)
- ✅ Asymmetric signing enables distributed verification
- ✅ Excellent Spring Security integration
- ✅ Supports token refresh pattern
- ✅ Can include custom claims for RBAC
- ✅ No licensing costs

**Cons**:

- ⚠️ Token revocation requires additional mechanism
- ⚠️ Larger token size than session IDs
- ⚠️ Cannot update permissions until token expires
- ⚠️ Key management complexity

**Cost**: $0 (open standard, built into Spring Security)

**Risk**: **Low** - Proven technology with extensive production use

### Option 2: Session-Based Authentication with Redis

**Description**: Traditional session cookies with centralized session store

**Pros**:

- ✅ Easy to implement
- ✅ Immediate session invalidation
- ✅ Smaller cookie size
- ✅ Can update permissions immediately
- ✅ Familiar pattern

**Cons**:

- ❌ Requires session affinity or shared session store
- ❌ Additional Redis dependency for sessions
- ❌ Database lookup on every request
- ❌ Not truly stateless
- ❌ Harder to scale horizontally
- ❌ Single point of failure (Redis)

**Cost**: $500/month (Redis cluster for sessions)

**Risk**: **Medium** - Scalability limitations

### Option 3: OAuth 2.0 with External Provider (Auth0, Okta)

**Description**: Delegate authentication to third-party provider

**Pros**:

- ✅ Managed service (less operational overhead)
- ✅ Advanced features (MFA, social login)
- ✅ Compliance certifications
- ✅ Professional support

**Cons**:

- ❌ Monthly licensing costs ($500-2000/month)
- ❌ Vendor lock-in
- ❌ External dependency
- ❌ Data privacy concerns (user data with third party)
- ❌ Network latency for authentication
- ❌ Less control over authentication flow

**Cost**: $1,500/month (Auth0 Professional plan)

**Risk**: **Medium** - Vendor dependency, cost escalation

### Option 4: API Keys for Service-to-Service

**Description**: Simple API keys for authentication

**Pros**:

- ✅ Very simple to implement
- ✅ Low overhead
- ✅ Good for service-to-service

**Cons**:

- ❌ Not suitable for user authentication
- ❌ No expiration mechanism
- ❌ Difficult to rotate
- ❌ No fine-grained permissions
- ❌ Security risks if leaked

**Cost**: $0

**Risk**: **High** - Security limitations

## Decision Outcome

**Chosen Option**: **JWT with RS256 Asymmetric Signing**

### Rationale

JWT with RS256 was selected for the following reasons:

1. **Stateless Architecture**: Enables true stateless microservices, critical for horizontal scaling
2. **Performance**: No database lookup needed for authentication (< 5ms overhead)
3. **Scalability**: No session affinity required, perfect for auto-scaling EKS
4. **Security**: Industry-standard with strong cryptographic signing
5. **Flexibility**: Self-contained tokens work across all services
6. **Cost-Effective**: No licensing fees, built into Spring Security
7. **Developer Experience**: Excellent tooling and documentation
8. **Standards-Based**: RFC 7519 standard, widely supported

**Token Structure**:

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-123",
    "name": "John Doe",
    "email": "john@example.com",
    "roles": ["CUSTOMER", "PREMIUM"],
    "permissions": ["order:create", "order:read"],
    "iat": 1706356800,
    "exp": 1706357700,
    "iss": "ecommerce-platform",
    "aud": "ecommerce-api"
  },
  "signature": "..."
}
```

**Why Not Session-Based**: Requires session affinity or shared session store, limiting horizontal scalability and adding Redis dependency.

**Why Not OAuth Provider**: High cost ($1,500/month) and vendor lock-in not justified for our requirements. We can implement OAuth 2.0 ourselves with JWT.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to implement JWT handling | Training, code examples, libraries |
| Frontend Team | Medium | Need to store and send JWT tokens | Documentation, SDK provided |
| Operations Team | Low | Key management required | Automated key rotation, documentation |
| End Users | None | Transparent to users | N/A |
| Security Team | Positive | Industry-standard security | Regular security audits |
| API Partners | Positive | Standard token-based auth | API documentation |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All API endpoints (authentication required)
- All microservices (token verification)
- Frontend applications (token storage and transmission)
- API Gateway (token validation)
- Infrastructure (key management)

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Token theft/leakage | Medium | High | Short expiration (15 min), HTTPS only, HttpOnly cookies |
| Key compromise | Low | Critical | Key rotation every 90 days, HSM storage, monitoring |
| Token revocation delay | Medium | Medium | Short expiration, blacklist for critical cases |
| Clock skew issues | Low | Medium | NTP synchronization, clock skew tolerance (5 min) |
| Token size overhead | Low | Low | Minimize claims, compress if needed |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Core JWT Infrastructure (Week 1)

- [x] Generate RSA key pair (2048-bit) for signing
- [x] Store private key in AWS Secrets Manager
- [x] Implement JWT generation service
- [x] Implement JWT validation filter
- [x] Configure Spring Security with JWT
- [x] Add JWT utilities (parse, validate, extract claims)

### Phase 2: Authentication Endpoints (Week 2)

- [x] Implement `/api/v1/auth/login` endpoint
- [x] Implement `/api/v1/auth/refresh` endpoint
- [x] Implement `/api/v1/auth/logout` endpoint
- [x] Add password validation and hashing (BCrypt)
- [x] Implement rate limiting for auth endpoints
- [x] Add authentication event logging

### Phase 3: Token Refresh Mechanism (Week 3)

- [x] Implement refresh token generation
- [x] Store refresh tokens in database (hashed)
- [x] Implement refresh token rotation
- [x] Add refresh token expiration (7 days)
- [x] Implement refresh token revocation
- [x] Add refresh token cleanup job

### Phase 4: Integration and Testing (Week 4)

- [x] Integrate with all microservices
- [x] Update API Gateway for token validation
- [x] Frontend integration (token storage)
- [x] Security testing (penetration testing)
- [x] Load testing (10K concurrent users)
- [x] Documentation and examples

### Rollback Strategy

**Trigger Conditions**:

- Critical security vulnerability discovered
- Performance degradation > 50ms per request
- Token validation failures > 1%
- Key management issues

**Rollback Steps**:

1. Enable temporary session-based authentication
2. Investigate and fix JWT implementation
3. Re-deploy with fixes
4. Gradually migrate users back to JWT

**Rollback Time**: < 2 hours

## Monitoring and Success Criteria

### Success Metrics

- ✅ Authentication latency < 10ms (95th percentile)
- ✅ Token validation latency < 5ms (95th percentile)
- ✅ Zero token forgery incidents
- ✅ Token refresh success rate > 99.9%
- ✅ Authentication failure rate < 0.1%
- ✅ No unauthorized access incidents

### Monitoring Plan

**CloudWatch Metrics**:

- `auth.login.success` (count)
- `auth.login.failure` (count)
- `auth.token.validation.time` (histogram)
- `auth.token.expired` (count)
- `auth.token.invalid` (count)
- `auth.refresh.success` (count)

**Alerts**:

- Authentication failure rate > 5% for 5 minutes
- Token validation latency > 20ms for 5 minutes
- Suspicious authentication patterns (brute force)
- Key rotation failures

**Security Monitoring**:

- Failed login attempts per IP
- Token validation failures
- Refresh token usage patterns
- Anomalous authentication times/locations

**Review Schedule**:

- Daily: Check authentication metrics
- Weekly: Review failed authentication logs
- Monthly: Security audit of JWT implementation
- Quarterly: Key rotation and security review

## Consequences

### Positive Consequences

- ✅ **Horizontal Scalability**: No session affinity required
- ✅ **Performance**: Fast authentication (< 5ms overhead)
- ✅ **Stateless**: True stateless microservices
- ✅ **Security**: Industry-standard cryptographic signing
- ✅ **Flexibility**: Works across all client types
- ✅ **Cost-Effective**: No licensing fees
- ✅ **Developer-Friendly**: Excellent tooling and libraries
- ✅ **Standards-Based**: RFC 7519 compliance

### Negative Consequences

- ⚠️ **Token Revocation**: Requires additional blacklist mechanism for immediate revocation
- ⚠️ **Token Size**: Larger than session IDs (typically 500-1000 bytes)
- ⚠️ **Permission Updates**: Cannot update permissions until token expires
- ⚠️ **Key Management**: Need secure key storage and rotation process
- ⚠️ **Clock Synchronization**: Requires NTP for accurate expiration

### Technical Debt

**Identified Debt**:

1. No token blacklist implemented (acceptable for 15-min expiration)
2. Manual key rotation process (acceptable initially)
3. No token compression (acceptable for current size)

**Debt Repayment Plan**:

- **Q2 2026**: Implement Redis-based token blacklist for critical revocations
- **Q3 2026**: Automate key rotation with AWS KMS
- **Q4 2026**: Implement token compression if size becomes issue

## Related Decisions

- [ADR-015: Role-Based Access Control (RBAC) Implementation](015-role-based-access-control-implementation.md) - Authorization model
- [ADR-052: Authentication Security Hardening](052-authentication-security-hardening.md) - Additional security measures
- [ADR-009: RESTful API Design with OpenAPI 3.0](009-restful-api-design-with-openapi.md) - API authentication integration
- [ADR-007: Use AWS CDK for Infrastructure](007-use-aws-cdk-for-infrastructure.md) - Key management infrastructure

## Notes

### JWT Configuration

```yaml
# application.yml
jwt:
  secret-key: ${JWT_SECRET_KEY} # Stored in AWS Secrets Manager
  expiration: 900000 # 15 minutes in milliseconds
  refresh-expiration: 604800000 # 7 days in milliseconds
  issuer: "ecommerce-platform"
  audience: "ecommerce-api"
  algorithm: "RS256"
```

### Token Expiration Strategy

- **Access Token**: 15 minutes (short-lived for security)
- **Refresh Token**: 7 days (balance between security and UX)
- **Remember Me**: 30 days (optional, with additional security)

### Key Rotation Schedule

- **Frequency**: Every 90 days
- **Process**: Generate new key pair, dual-signing period (7 days), deprecate old key
- **Emergency Rotation**: < 1 hour if compromise suspected

### Security Best Practices

1. **HTTPS Only**: All JWT transmission over HTTPS
2. **HttpOnly Cookies**: Store tokens in HttpOnly cookies (web)
3. **Secure Storage**: Use Keychain/Keystore for mobile apps
4. **Short Expiration**: 15-minute access tokens
5. **Token Refresh**: Implement refresh token rotation
6. **Rate Limiting**: Limit authentication attempts
7. **Audit Logging**: Log all authentication events

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
