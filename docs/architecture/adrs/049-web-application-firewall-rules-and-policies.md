---
adr_number: 049
title: "Web Application Firewall (WAF) Rules and Policies"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [048, 050, 051, 053]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["security", "performance"]
---

# ADR-049: Web Application Firewall (WAF) Rules and Policies

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires comprehensive application-layer (Layer 7) protection against:

- **SQL Injection Attacks**: Malicious SQL queries targeting database
- **Cross-Site Scripting (XSS)**: Injection of malicious scripts
- **HTTP Floods**: Overwhelming application with HTTP requests
- **Bot Attacks**: Automated scraping, credential stuffing, inventory hoarding
- **Known Vulnerabilities**: OWASP Top 10 exploits
- **Geo-Based Attacks**: Attacks from high-risk countries
- **Rate Abuse**: Excessive requests from single sources

The platform needs a Web Application Firewall (WAF) that can:

- Protect against OWASP Top 10 vulnerabilities
- Implement intelligent rate limiting
- Detect and block malicious bots
- Support custom rules for application-specific threats
- Provide real-time monitoring and logging
- Minimize false positives
- Scale with application traffic

### Business Context

**Business Drivers**:
- **Data Protection**: Protect customer PII and payment information
- **Regulatory Compliance**: PCI-DSS, GDPR requirements
- **Brand Reputation**: Security breaches damage customer trust
- **Revenue Protection**: Prevent bot-driven inventory hoarding

**Taiwan-Specific Context**:
- **Frequent Web Attacks**: Taiwan experiences high volume of application-layer attacks
- **E-Commerce Targeting**: Online shopping platforms are prime targets
- **Bot Activity**: High bot traffic from automated scraping and fraud attempts
- **Credential Stuffing**: Frequent attempts using leaked credentials

**Constraints**:
- Budget: ~$500/month for WAF (included in DDoS protection budget)
- Must not impact legitimate user experience
- Must integrate with CloudFront and ALB
- Must support multi-region deployment

### Technical Context

**Current State**:
- CloudFront distribution configured
- AWS Shield Advanced enabled
- No WAF rules configured
- No rate limiting implemented
- No bot protection

**Attack Vectors**:
- **SQL Injection**: Login, search, product filters
- **XSS**: Product reviews, user profiles, comments
- **CSRF**: State-changing operations (checkout, profile updates)
- **HTTP Floods**: Login, search, checkout endpoints
- **Bot Scraping**: Product catalog, pricing information
- **Credential Stuffing**: Login endpoint with leaked credentials

## Decision Drivers

1. **Security Coverage**: Protect against OWASP Top 10 and common attacks
2. **False Positive Rate**: < 0.1% legitimate traffic blocked
3. **Performance**: < 5ms latency overhead
4. **Flexibility**: Support custom rules for application-specific threats
5. **Visibility**: Comprehensive logging and monitoring
6. **Cost-Effectiveness**: Stay within $500/month budget
7. **Ease of Management**: Manageable rule complexity
8. **Integration**: Seamless integration with CloudFront and ALB

## Considered Options

### Option 1: AWS WAF with Managed Rules + Custom Rules (Recommended)

**Description**: AWS WAF with combination of AWS Managed Rules and custom rules

**Rule Sets**:
- AWS Managed Rules (Core Rule Set, Known Bad Inputs, SQL Database, OWASP Top 10)
- Custom rate limiting rules
- Custom geo-blocking rules
- Custom bot detection rules
- Custom application-specific rules

**Pros**:
- ✅ **Comprehensive Protection**: Covers OWASP Top 10 and common attacks
- ✅ **Managed Rules**: AWS maintains and updates rules automatically
- ✅ **Custom Flexibility**: Can add application-specific rules
- ✅ **AWS Integration**: Native integration with CloudFront and ALB
- ✅ **Real-Time Updates**: Managed rules updated automatically
- ✅ **Cost-Effective**: $5/month Web ACL + $1/million requests
- ✅ **Scalability**: Handles high traffic volumes
- ✅ **Logging**: Comprehensive logging to S3 and CloudWatch

**Cons**:
- ⚠️ **Rule Tuning**: Requires initial tuning to minimize false positives
- ⚠️ **Complexity**: Multiple rule sets to manage
- ⚠️ **Learning Curve**: Team needs training on WAF configuration

**Cost**: ~$500/month (for 100M requests)

**Risk**: **Low** - Proven AWS service with extensive production use

### Option 2: AWS WAF with Managed Rules Only (Basic)

**Description**: Use only AWS Managed Rules without custom rules

**Pros**:
- ✅ **Simple Setup**: Minimal configuration required
- ✅ **Automatic Updates**: AWS maintains rules
- ✅ **Lower Cost**: Fewer rules = lower cost

**Cons**:
- ❌ **Limited Protection**: No application-specific rules
- ❌ **No Rate Limiting**: Cannot implement custom rate limits
- ❌ **No Geo-Blocking**: Cannot block specific countries
- ❌ **No Bot Protection**: Limited bot detection capabilities

**Cost**: ~$300/month

**Risk**: **Medium** - Insufficient for comprehensive protection

### Option 3: Third-Party WAF (Cloudflare, Imperva)

**Description**: Use third-party WAF service

**Pros**:
- ✅ **Advanced Features**: Bot management, DDoS protection, CDN
- ✅ **Specialized Protection**: WAF is core business
- ✅ **Global Network**: Large edge network

**Cons**:
- ❌ **Higher Cost**: $1,000-5,000/month
- ❌ **Vendor Lock-In**: Difficult to migrate
- ❌ **Integration Complexity**: Requires DNS changes
- ❌ **Data Privacy**: Traffic routed through third-party

**Cost**: $1,000-5,000/month

**Risk**: **Medium** - Vendor dependency and higher cost

### Option 4: Open-Source WAF (ModSecurity)

**Description**: Self-hosted open-source WAF

**Pros**:
- ✅ **No Licensing Cost**: Open-source
- ✅ **Full Control**: Complete customization

**Cons**:
- ❌ **Operational Overhead**: Requires dedicated team
- ❌ **Maintenance Burden**: Manual rule updates
- ❌ **Scalability Challenges**: Difficult to scale
- ❌ **No Managed Updates**: Must manually update rules

**Cost**: $0 licensing + $5,000/month operational overhead

**Risk**: **High** - Not suitable for cloud-native architecture

## Decision Outcome

**Chosen Option**: **AWS WAF with Managed Rules + Custom Rules**

### Rationale

AWS WAF with managed and custom rules was selected for the following reasons:

1. **Comprehensive Protection**: Covers OWASP Top 10 and application-specific threats
2. **Cost-Effective**: $500/month fits within budget
3. **AWS Integration**: Seamless integration with CloudFront and Shield Advanced
4. **Automatic Updates**: Managed rules updated by AWS automatically
5. **Flexibility**: Can add custom rules for application-specific threats
6. **Scalability**: Handles high traffic volumes without performance impact
7. **Visibility**: Comprehensive logging and monitoring

**WAF Rule Architecture**:

**Rule Priority Order** (evaluated in order):
1. **Allow List** (Priority 1-100): Whitelist known good IPs/User-Agents
2. **Block List** (Priority 101-200): Blacklist known malicious IPs
3. **Rate Limiting** (Priority 201-300): Prevent abuse
4. **AWS Managed Rules** (Priority 301-400): OWASP Top 10, SQL injection, XSS
5. **Custom Rules** (Priority 401-500): Application-specific rules
6. **Default Action**: Allow (after all rules evaluated)

**Why Not Managed Rules Only**: Insufficient protection for application-specific threats and no rate limiting capabilities.

**Why Not Third-Party WAF**: Higher cost ($1K-5K/month) and vendor lock-in not justified when AWS WAF provides comprehensive protection at lower cost.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Low | Minimal code changes | Documentation and training |
| Operations Team | Medium | Need to monitor and tune WAF rules | Training, runbooks, dashboards |
| Security Team | Positive | Enhanced security posture | Regular security reviews |
| End Users | None | Transparent protection | N/A |
| Finance Team | Low | $500/month within budget | N/A |
| Business Team | Positive | Reduced security risk | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All public-facing endpoints (web, mobile, API)
- CloudFront distribution
- Application Load Balancer
- Logging and monitoring systems
- Incident response procedures

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positives blocking legitimate traffic | Medium | High | Careful rule tuning, whitelist for known IPs, gradual rollout |
| Performance degradation | Low | Medium | Monitor latency, optimize rules |
| Rule complexity | Medium | Medium | Document rules, regular reviews |
| Bypass attacks | Low | High | Regular security audits, penetration testing |
| Cost overrun | Low | Low | Monitor request volume, set budget alerts |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: AWS Managed Rules Setup (Week 1)

- [x] Create WAF Web ACL
- [x] Enable AWS Managed Rules:
  - Core Rule Set (CRS)
  - Known Bad Inputs
  - SQL Database Protection
  - Linux Operating System
  - POSIX Operating System
  - Windows Operating System
  - PHP Application
  - WordPress Application
- [x] Configure rule actions (Block, Count, Allow)
- [x] Associate WAF with CloudFront distribution
- [x] Test with simulated attacks

### Phase 2: Rate Limiting Rules (Week 2)

- [x] Implement global rate limiting (10,000 req/min)
- [x] Implement per-IP rate limiting (2,000 req/min)
- [x] Implement per-user rate limiting (100 req/min)
- [x] Implement endpoint-specific rate limiting:
  - Login: 10 req/min per IP
  - Search: 100 req/min per IP
  - Checkout: 20 req/min per IP
  - API: 1,000 req/min per API key
- [x] Test rate limiting with load testing

### Phase 3: Geo-Blocking and Custom Rules (Week 3)

- [x] Configure geo-blocking (optional):
  - Block high-risk countries (if business allows)
  - Allow Taiwan, Japan, US, EU
- [x] Implement IP reputation rules
- [x] Implement User-Agent filtering (block known bad bots)
- [x] Implement custom rules for application-specific threats
- [x] Test custom rules with simulated attacks

### Phase 4: Logging and Monitoring (Week 4)

- [x] Configure WAF logging to S3
- [x] Set up CloudWatch metrics and alarms
- [x] Create CloudWatch dashboard for WAF monitoring
- [x] Configure SNS notifications for security team
- [x] Set up log analysis (Athena queries)
- [x] Configure automated response (Lambda + EventBridge)

### Phase 5: Tuning and Optimization (Week 5)

- [x] Analyze false positives
- [x] Tune rule sensitivity
- [x] Add whitelist for known good IPs
- [x] Optimize rule order for performance
- [x] Document runbooks and procedures
- [x] Train operations team

### Rollback Strategy

**Trigger Conditions**:
- False positive rate > 1% (legitimate traffic blocked)
- Performance degradation > 10ms latency
- Service outage caused by WAF rules
- Cost overrun > 50% of budget

**Rollback Steps**:
1. Change rule actions from Block to Count (monitoring mode)
2. Disable specific rules causing issues
3. Remove WAF association from CloudFront
4. Investigate and fix issues
5. Re-deploy with corrections

**Rollback Time**: < 15 minutes

## Monitoring and Success Criteria

### Success Metrics

- ✅ **Attack Blocking**: 100% of known attacks blocked
- ✅ **False Positive Rate**: < 0.1% legitimate traffic blocked
- ✅ **Performance**: < 5ms latency overhead
- ✅ **Availability**: No service degradation from WAF
- ✅ **Cost**: Stay within $500/month budget
- ✅ **Coverage**: All OWASP Top 10 vulnerabilities protected

### Monitoring Plan

**CloudWatch Metrics**:
- `AllowedRequests` (count)
- `BlockedRequests` (count)
- `CountedRequests` (count)
- `PassedRequests` (count)
- `SampledRequests` (sample of blocked requests)

**Custom Metrics**:
- Block rate by rule
- Block rate by country
- Block rate by IP
- Block rate by User-Agent
- Rate limiting triggers

**Alerts**:
- **P0 Critical**: Block rate > 50% (potential false positives or major attack)
- **P1 High**: Block rate > 10% (investigate for false positives)
- **P2 Medium**: Unusual traffic patterns
- **P3 Low**: New attack patterns detected

**Security Monitoring**:
- Real-time WAF log analysis (Kinesis + Lambda)
- Attack pattern analysis (Athena queries on S3 logs)
- Geo-location analysis of blocked requests
- Bot detection and analysis
- SQL injection attempt analysis
- XSS attempt analysis

**Review Schedule**:
- **Real-Time**: 24/7 monitoring dashboard
- **Daily**: Review blocked requests and false positives
- **Weekly**: Analyze attack patterns and tune rules
- **Monthly**: Security review and rule optimization
- **Quarterly**: Comprehensive security audit

## Consequences

### Positive Consequences

- ✅ **Enhanced Security**: Protection against OWASP Top 10 and common attacks
- ✅ **Automatic Updates**: Managed rules updated by AWS
- ✅ **Flexibility**: Custom rules for application-specific threats
- ✅ **Visibility**: Comprehensive logging and monitoring
- ✅ **Cost-Effective**: $500/month within budget
- ✅ **Performance**: < 5ms latency overhead
- ✅ **Scalability**: Handles high traffic volumes
- ✅ **Compliance**: Helps meet PCI-DSS and GDPR requirements

### Negative Consequences

- ⚠️ **False Positive Risk**: Potential for blocking legitimate traffic
- ⚠️ **Operational Overhead**: Requires monitoring and tuning
- ⚠️ **Rule Complexity**: Multiple rule sets to manage
- ⚠️ **Learning Curve**: Team needs training on WAF configuration

### Technical Debt

**Identified Debt**:
1. Manual rule tuning (acceptable initially)
2. No automated false positive detection
3. Limited bot detection (basic User-Agent filtering)

**Debt Repayment Plan**:
- **Q2 2026**: Implement automated rule tuning based on traffic patterns
- **Q3 2026**: Implement advanced bot detection (AWS WAF Bot Control)
- **Q4 2026**: Implement machine learning-based anomaly detection

## Related Decisions

- [ADR-048: DDoS Protection Strategy (Multi-Layer Defense)](048-ddos-protection-strategy.md) - Overall DDoS protection
- [ADR-050: API Security and Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - Application-level protection
- [ADR-051: Input Validation and Sanitization Strategy](051-input-validation-and-sanitization-strategy.md) - Application-level validation
- [ADR-053: Security Monitoring and Incident Response](053-security-monitoring-and-incident-response.md) - Security operations

## Notes

### AWS Managed Rule Groups

**Core Rule Set (CRS)**:
- General web application protection
- Covers common vulnerabilities
- Recommended for all applications

**Known Bad Inputs**:
- Blocks requests with known malicious patterns
- Protects against common exploits

**SQL Database Protection**:
- Protects against SQL injection attacks
- Covers MySQL, PostgreSQL, Oracle, SQL Server

**Linux/Windows Operating System**:
- Protects against OS-specific exploits
- Blocks command injection attempts

**PHP/WordPress Application**:
- Protects against PHP and WordPress vulnerabilities
- Blocks common CMS exploits

### Rate Limiting Configuration

**Global Rate Limiting**:
```json
{
  "Name": "GlobalRateLimit",
  "Priority": 201,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 10000,
      "AggregateKeyType": "IP"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Per-IP Rate Limiting**:
```json
{
  "Name": "PerIPRateLimit",
  "Priority": 202,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 2000,
      "AggregateKeyType": "IP"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Endpoint-Specific Rate Limiting (Login)**:
```json
{
  "Name": "LoginRateLimit",
  "Priority": 203,
  "Statement": {
    "RateBasedStatement": {
      "Limit": 10,
      "AggregateKeyType": "IP",
      "ScopeDownStatement": {
        "ByteMatchStatement": {
          "FieldToMatch": {
            "UriPath": {}
          },
          "PositionalConstraint": "STARTS_WITH",
          "SearchString": "/api/v1/auth/login"
        }
      }
    }
  },
  "Action": {
    "Block": {}
  }
}
```

### Geo-Blocking Configuration

**Block High-Risk Countries** (Optional):
```json
{
  "Name": "GeoBlockHighRisk",
  "Priority": 101,
  "Statement": {
    "GeoMatchStatement": {
      "CountryCodes": ["CN", "RU", "KP"]
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Note**: Geo-blocking should be used carefully as it may block legitimate users. Consider business impact before implementing.

### Custom Rule Examples

**Block Known Malicious IPs**:
```json
{
  "Name": "BlockMaliciousIPs",
  "Priority": 102,
  "Statement": {
    "IPSetReferenceStatement": {
      "Arn": "arn:aws:wafv2:region:account:regional/ipset/malicious-ips/id"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

**Block Bad User-Agents**:
```json
{
  "Name": "BlockBadUserAgents",
  "Priority": 401,
  "Statement": {
    "ByteMatchStatement": {
      "FieldToMatch": {
        "SingleHeader": {
          "Name": "user-agent"
        }
      },
      "PositionalConstraint": "CONTAINS",
      "SearchString": "bot|crawler|scraper"
    }
  },
  "Action": {
    "Block": {}
  }
}
```

### WAF Logging Configuration

**S3 Logging**:
- Bucket: `waf-logs-{account-id}-{region}`
- Prefix: `AWSLogs/{account-id}/WAFLogs/{region}/`
- Retention: 90 days
- Format: JSON

**CloudWatch Logs**:
- Log Group: `/aws/waf/cloudfront`
- Retention: 30 days
- Metrics: Extracted from logs

**Kinesis Data Firehose**:
- Real-time log streaming
- Lambda processing for alerts
- S3 backup

### Cost Breakdown

**Monthly Costs** (for 100M requests):
- Web ACL: $5/month
- Rules: $1/rule/month × 20 rules = $20/month
- Requests: $0.60/million requests × 100M = $60/month
- Managed Rule Groups: $10/rule group/month × 5 groups = $50/month
- Logging: $0.50/GB × 100GB = $50/month
- **Total**: ~$185/month (well within $500 budget)

**Cost Optimization**:
- Use Count mode for non-critical rules (no charge)
- Optimize rule order (evaluate cheaper rules first)
- Use sampling for logging (reduce log volume)

### Emergency Procedures

**During Active Attack**:
1. **Immediate**: Verify WAF is blocking malicious requests
2. **5 minutes**: Analyze attack patterns in WAF logs
3. **10 minutes**: Adjust rate limits if needed
4. **15 minutes**: Add custom rules for attack-specific patterns
5. **30 minutes**: Communicate with stakeholders
6. **Post-Attack**: Conduct post-mortem and update rules

**False Positive Response**:
1. **Immediate**: Identify affected rule
2. **5 minutes**: Change rule action from Block to Count
3. **10 minutes**: Analyze blocked requests
4. **15 minutes**: Add whitelist for legitimate traffic
5. **30 minutes**: Re-enable rule with whitelist
6. **Post-Incident**: Document and update runbooks

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
