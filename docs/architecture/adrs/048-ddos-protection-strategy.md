---
adr_number: 048
title: "DDoS Protection Strategy (Multi-Layer Defense)"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [049, 050, 053, 056]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["security", "availability", "performance"]
---

# ADR-048: DDoS Protection Strategy (Multi-Layer Defense)

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform, operating in Taiwan, faces significant DDoS (Distributed Denial of Service) attack risks due to:

- **Geopolitical Tensions**: Taiwan-China relations create elevated cyber attack risks
- **High-Value Target**: E-commerce platform with financial transactions
- **24/7 Availability Requirement**: Business-critical system requiring 99.9% uptime
- **Multi-Channel Access**: Web, mobile, and API endpoints all vulnerable
- **Reputation Risk**: DDoS attacks can damage customer trust and brand reputation

The platform requires a comprehensive, multi-layer DDoS protection strategy that can:

- Detect and mitigate Layer 3/4 (network/transport) attacks
- Detect and mitigate Layer 7 (application) attacks
- Maintain service availability during attacks
- Minimize false positives (legitimate traffic blocked)
- Provide real-time monitoring and alerting
- Scale automatically to handle attack traffic
- Integrate with existing AWS infrastructure

### Business Context

**Business Drivers**:

- **Revenue Protection**: Downtime costs $10,000/hour in lost sales
- **Customer Trust**: DDoS attacks damage brand reputation
- **Regulatory Compliance**: Must maintain service availability commitments
- **Competitive Advantage**: Reliable service differentiates from competitors

**Taiwan-Specific Context**:

- **Frequent Attacks**: Taiwan experiences regular DDoS attacks from China-based sources
- **Political Sensitivity**: Attacks often coincide with political events
- **Submarine Cable Vulnerability**: Limited international connectivity increases risk
- **Regional Targeting**: Taiwan-based services are high-priority targets

**Constraints**:

- Budget: $3,000-5,000/month for DDoS protection
- Must integrate with existing AWS infrastructure
- Cannot impact legitimate user experience
- Must support multi-region deployment (Taiwan + Tokyo)

### Technical Context

**Current State**:

- AWS EKS deployment with Application Load Balancer
- CloudFront CDN for static content
- No dedicated DDoS protection (only AWS Shield Standard)
- No WAF (Web Application Firewall) configured
- Single region deployment (Taiwan)

**Attack Vectors**:

- **Layer 3/4 Attacks**: SYN floods, UDP floods, ICMP floods
- **Layer 7 Attacks**: HTTP floods, Slowloris, application-specific attacks
- **Volumetric Attacks**: Bandwidth exhaustion (10-100 Gbps)
- **Protocol Attacks**: Exploiting protocol weaknesses
- **Application Attacks**: Targeting specific endpoints (login, search, checkout)

## Decision Drivers

1. **Availability**: Maintain 99.9% uptime during attacks
2. **Attack Coverage**: Protect against Layer 3/4 and Layer 7 attacks
3. **Response Time**: Detect and mitigate attacks within 1 minute
4. **False Positives**: < 0.1% legitimate traffic blocked
5. **Cost-Effectiveness**: Balance protection level with budget
6. **Scalability**: Handle attacks up to 100 Gbps
7. **Integration**: Seamless integration with AWS infrastructure
8. **Monitoring**: Real-time visibility into attack patterns

## Considered Options

### Option 1: AWS Shield Advanced + WAF + CloudFront (Recommended)

**Description**: Comprehensive AWS-native DDoS protection with multi-layer defense

**Architecture**:

```text
Internet → CloudFront (CDN) → AWS Shield Advanced → WAF → ALB → EKS
```

**Pros**:

- ✅ **Comprehensive Protection**: Layer 3/4 (Shield) + Layer 7 (WAF)
- ✅ **AWS Integration**: Native integration with CloudFront, ALB, Route 53
- ✅ **DDoS Response Team**: 24/7 AWS DDoS Response Team (DRT) support
- ✅ **Cost Protection**: DDoS cost protection (no scaling charges during attacks)
- ✅ **Advanced Detection**: Machine learning-based attack detection
- ✅ **Real-Time Mitigation**: Automatic mitigation within seconds
- ✅ **CloudFront Benefits**: Hide origin IP, absorb attack traffic at edge
- ✅ **Health-Based Routing**: Route 53 health checks for automatic failover

**Cons**:

- ⚠️ **Cost**: $3,000/month base + $1/million requests (WAF)
- ⚠️ **Complexity**: Requires configuration and tuning
- ⚠️ **Learning Curve**: Team needs training on Shield Advanced features

**Cost**:

- Shield Advanced: $3,000/month
- WAF: ~$500/month (estimated for 100M requests)
- CloudFront: ~$500/month (data transfer)
- **Total**: ~$4,000/month

**Risk**: **Low** - Proven AWS service with extensive production use

### Option 2: AWS Shield Standard + WAF + CloudFront (Budget Option)

**Description**: Basic AWS DDoS protection with WAF for application layer

**Architecture**:

```text
Internet → CloudFront (CDN) → WAF → ALB → EKS
```

**Pros**:

- ✅ **Lower Cost**: No Shield Advanced fees
- ✅ **Basic Protection**: Shield Standard included free
- ✅ **Application Protection**: WAF for Layer 7 attacks
- ✅ **CloudFront Benefits**: Edge caching and basic DDoS absorption

**Cons**:

- ❌ **Limited Protection**: No advanced Layer 3/4 mitigation
- ❌ **No DRT Support**: No 24/7 DDoS Response Team
- ❌ **No Cost Protection**: Scaling charges during attacks
- ❌ **Manual Mitigation**: Requires manual intervention for large attacks
- ❌ **Slower Response**: No automatic advanced mitigation

**Cost**: ~$1,000/month (WAF + CloudFront only)

**Risk**: **High** - Insufficient for Taiwan's threat environment

### Option 3: Third-Party DDoS Protection (Cloudflare, Akamai)

**Description**: Use third-party DDoS protection service

**Pros**:

- ✅ **Specialized Protection**: DDoS protection is core business
- ✅ **Global Network**: Large edge network for traffic absorption
- ✅ **Advanced Features**: Bot management, rate limiting, caching

**Cons**:

- ❌ **Higher Cost**: $5,000-10,000/month for enterprise plans
- ❌ **Vendor Lock-In**: Difficult to migrate away
- ❌ **Integration Complexity**: Requires DNS changes and configuration
- ❌ **Data Privacy**: Traffic routed through third-party
- ❌ **Latency**: Additional hop in traffic path

**Cost**: $5,000-10,000/month

**Risk**: **Medium** - Vendor dependency and higher cost

### Option 4: On-Premises DDoS Appliance

**Description**: Deploy dedicated DDoS protection hardware

**Pros**:

- ✅ **Full Control**: Complete control over protection
- ✅ **No Recurring Fees**: One-time hardware cost

**Cons**:

- ❌ **High Initial Cost**: $50,000-100,000 for hardware
- ❌ **Operational Overhead**: Requires dedicated team
- ❌ **Limited Capacity**: Fixed capacity, cannot scale
- ❌ **Single Point of Failure**: Hardware failure risk
- ❌ **Not Cloud-Native**: Doesn't fit AWS architecture

**Cost**: $50,000-100,000 initial + $10,000/year maintenance

**Risk**: **High** - Not suitable for cloud-native architecture

## Decision Outcome

**Chosen Option**: **AWS Shield Advanced + WAF + CloudFront (Multi-Layer Defense)**

### Rationale

AWS Shield Advanced with WAF and CloudFront was selected for the following reasons:

1. **Comprehensive Protection**: Covers both Layer 3/4 (Shield) and Layer 7 (WAF) attacks
2. **Taiwan Context**: Proven effective against China-based DDoS attacks
3. **24/7 Support**: AWS DDoS Response Team provides expert assistance
4. **Cost Protection**: No scaling charges during attacks (critical for budget predictability)
5. **AWS Integration**: Seamless integration with existing infrastructure
6. **Automatic Mitigation**: Machine learning-based detection and mitigation
7. **CloudFront Benefits**: Hides origin IP and absorbs attack traffic at edge
8. **Scalability**: Can handle attacks up to 100+ Gbps

**Multi-Layer Defense Strategy**:

**Layer 1 - CloudFront (Edge Protection)**:

- Hide origin server IP addresses
- Absorb volumetric attacks at edge locations
- Cache static content to reduce origin load
- Geo-blocking for high-risk countries (optional)

**Layer 2 - AWS Shield Advanced (Network/Transport Protection)**:

- Automatic detection of Layer 3/4 attacks
- Real-time mitigation within seconds
- Protection against SYN floods, UDP floods, reflection attacks
- DDoS cost protection (no scaling charges)

**Layer 3 - AWS WAF (Application Protection)**:

- Rate limiting (2000 requests/min per IP)
- SQL injection and XSS protection
- Bot detection and mitigation
- Custom rules for application-specific attacks
- Geo-blocking for high-risk regions

**Layer 4 - Application Load Balancer (Distribution)**:

- Health checks and automatic failover
- Connection draining during attacks
- SSL/TLS termination

**Layer 5 - Auto-Scaling (Capacity)**:

- Automatic scaling based on traffic
- Absorb attack traffic with additional capacity
- Cost-effective scaling with Spot Instances

**Why Not Shield Standard**: Insufficient protection for Taiwan's threat environment. No advanced mitigation or DRT support.

**Why Not Third-Party**: Higher cost ($5K-10K/month) and vendor lock-in not justified when AWS Shield Advanced provides comprehensive protection at lower cost.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Low | Minimal code changes | Documentation and training |
| Operations Team | Medium | Need to monitor and respond to attacks | Training, runbooks, 24/7 on-call |
| Security Team | Positive | Enhanced security posture | Regular security reviews |
| End Users | None | Transparent protection | N/A |
| Finance Team | Medium | $4K/month additional cost | Budget approval obtained |
| Business Team | Positive | Reduced downtime risk | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- All public-facing endpoints (web, mobile, API)
- CloudFront distribution configuration
- Route 53 DNS configuration
- Application Load Balancer configuration
- WAF rules and policies
- Monitoring and alerting systems

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positives blocking legitimate traffic | Low | High | Careful WAF rule tuning, whitelist for known IPs |
| Shield Advanced cost overrun | Low | Medium | Cost monitoring, budget alerts |
| Configuration errors | Medium | High | Thorough testing, staged rollout |
| Attack bypassing protection | Low | Critical | Regular security audits, penetration testing |
| DRT response delay | Low | Medium | Proactive monitoring, automated mitigation |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: CloudFront Setup (Week 1)

- [x] Create CloudFront distribution
- [x] Configure origin (ALB) with custom headers
- [x] Enable origin access control (OAC)
- [x] Configure SSL/TLS (ACM certificate)
- [x] Set up geo-restriction (optional)
- [x] Configure caching policies
- [x] Update DNS (Route 53) to point to CloudFront

### Phase 2: AWS Shield Advanced Activation (Week 2)

- [x] Subscribe to AWS Shield Advanced
- [x] Associate Shield Advanced with CloudFront, ALB, Route 53
- [x] Configure DDoS Response Team (DRT) access
- [x] Set up Shield Advanced notifications
- [x] Configure health-based routing in Route 53
- [x] Test Shield Advanced detection and mitigation

### Phase 3: WAF Configuration (Week 3)

- [x] Create WAF Web ACL
- [x] Configure AWS Managed Rules (Core Rule Set, Known Bad Inputs)
- [x] Configure rate limiting rules (2000 req/min per IP)
- [x] Configure geo-blocking rules (optional)
- [x] Configure custom rules for application-specific attacks
- [x] Associate WAF with CloudFront distribution
- [x] Test WAF rules with simulated attacks

### Phase 4: Monitoring and Alerting (Week 4)

- [x] Configure CloudWatch metrics for Shield Advanced
- [x] Configure CloudWatch metrics for WAF
- [x] Set up CloudWatch alarms for attack detection
- [x] Configure SNS notifications for security team
- [x] Create CloudWatch dashboard for DDoS monitoring
- [x] Set up log aggregation (WAF logs to S3)
- [x] Configure automated response (Lambda + EventBridge)

### Phase 5: Testing and Validation (Week 5)

- [x] Conduct simulated DDoS attacks (Layer 3/4)
- [x] Conduct simulated application attacks (Layer 7)
- [x] Validate automatic mitigation
- [x] Test DRT escalation process
- [x] Validate false positive rate
- [x] Load testing with legitimate traffic
- [x] Document runbooks and procedures

### Rollback Strategy

**Trigger Conditions**:

- False positive rate > 1% (legitimate traffic blocked)
- Service degradation during normal traffic
- Configuration errors causing outages
- Cost overrun > 50% of budget

**Rollback Steps**:

1. Disable WAF rules causing false positives
2. Remove CloudFront distribution (revert to direct ALB access)
3. Downgrade to Shield Standard if Shield Advanced issues
4. Restore previous DNS configuration
5. Investigate and fix issues
6. Re-deploy with corrections

**Rollback Time**: < 1 hour

## Monitoring and Success Criteria

### Success Metrics

- ✅ **Availability**: Maintain 99.9% uptime during attacks
- ✅ **Attack Mitigation**: 100% of attacks detected and mitigated
- ✅ **Response Time**: Mitigation within 1 minute of attack detection
- ✅ **False Positives**: < 0.1% legitimate traffic blocked
- ✅ **Cost Protection**: No scaling charges during attacks
- ✅ **User Experience**: No degradation for legitimate users

### Monitoring Plan

**CloudWatch Metrics**:

- `DDoSDetected` (Shield Advanced)
- `DDoSAttackBitsPerSecond` (Shield Advanced)
- `DDoSAttackPacketsPerSecond` (Shield Advanced)
- `DDoSAttackRequestsPerSecond` (Shield Advanced)
- `AllowedRequests` (WAF)
- `BlockedRequests` (WAF)
- `CountedRequests` (WAF)
- `CloudFront4xxErrorRate`
- `CloudFront5xxErrorRate`
- `CloudFrontRequests`

**Alerts**:

- **P0 Critical**: DDoS attack detected (immediate notification)
- **P1 High**: WAF block rate > 10% (potential attack or false positives)
- **P2 Medium**: CloudFront error rate > 5%
- **P3 Low**: Unusual traffic patterns

**Security Monitoring**:

- Real-time WAF log analysis (Kinesis + Lambda)
- Attack pattern analysis (Athena queries on S3 logs)
- Geo-location analysis of attack sources
- Bot detection and analysis

**Review Schedule**:

- **Real-Time**: 24/7 monitoring dashboard
- **Daily**: Review attack logs and blocked requests
- **Weekly**: Analyze attack patterns and tune WAF rules
- **Monthly**: Security review with DRT (if attacks occurred)
- **Quarterly**: Comprehensive security audit and penetration testing

## Consequences

### Positive Consequences

- ✅ **Enhanced Availability**: 99.9% uptime maintained during attacks
- ✅ **Comprehensive Protection**: Multi-layer defense against all attack types
- ✅ **Cost Predictability**: DDoS cost protection prevents budget overruns
- ✅ **Expert Support**: 24/7 AWS DRT assistance
- ✅ **Automatic Mitigation**: Machine learning-based detection and response
- ✅ **Customer Trust**: Reliable service builds brand reputation
- ✅ **Competitive Advantage**: Superior availability vs competitors
- ✅ **Regulatory Compliance**: Meets availability commitments

### Negative Consequences

- ⚠️ **Increased Cost**: $4,000/month additional infrastructure cost
- ⚠️ **Operational Complexity**: Requires monitoring and tuning
- ⚠️ **False Positive Risk**: Potential for blocking legitimate traffic
- ⚠️ **Configuration Overhead**: Initial setup and ongoing maintenance
- ⚠️ **Dependency on AWS**: Reliance on AWS Shield Advanced service

### Technical Debt

**Identified Debt**:

1. Manual WAF rule tuning (acceptable initially)
2. No automated attack response beyond AWS defaults
3. Limited geo-blocking (may need expansion)

**Debt Repayment Plan**:

- **Q2 2026**: Implement automated WAF rule tuning based on attack patterns
- **Q3 2026**: Develop custom automated response (Lambda functions)
- **Q4 2026**: Expand geo-blocking based on attack source analysis

## Related Decisions

- [ADR-049: Web Application Firewall (WAF) Rules and Policies](049-web-application-firewall-rules-and-policies.md) - Detailed WAF configuration
- [ADR-050: API Security and Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - Application-level protection
- [ADR-053: Security Monitoring and Incident Response](053-security-monitoring-and-incident-response.md) - Security operations
- [ADR-056: Network Segmentation and Isolation Strategy](056-network-segmentation-and-isolation-strategy.md) - Network security

## Notes

### DDoS Protection Layers

**Layer 1 - CloudFront (Edge)**:

- **Purpose**: Hide origin IP, absorb volumetric attacks
- **Protection**: Bandwidth exhaustion, volumetric attacks
- **Capacity**: Unlimited (AWS global edge network)

**Layer 2 - Shield Advanced (Network/Transport)**:

- **Purpose**: Detect and mitigate Layer 3/4 attacks
- **Protection**: SYN floods, UDP floods, reflection attacks
- **Capacity**: Up to 100+ Gbps

**Layer 3 - WAF (Application)**:

- **Purpose**: Protect against Layer 7 attacks
- **Protection**: HTTP floods, SQL injection, XSS, bot attacks
- **Capacity**: Configurable rate limits

**Layer 4 - ALB (Distribution)**:

- **Purpose**: Distribute traffic and health checks
- **Protection**: Connection draining, health-based routing
- **Capacity**: Auto-scaling based on traffic

**Layer 5 - Auto-Scaling (Capacity)**:

- **Purpose**: Absorb attack traffic with additional capacity
- **Protection**: Capacity-based attacks
- **Capacity**: Unlimited (auto-scaling)

### Taiwan-Specific Considerations

**Attack Patterns**:

- **Timing**: Attacks often coincide with political events
- **Sources**: Primarily from China-based IP ranges
- **Types**: Mix of volumetric (Layer 3/4) and application (Layer 7) attacks
- **Duration**: Typically 1-4 hours, sometimes sustained for days

**Mitigation Strategies**:

- **Geo-Blocking**: Consider blocking China IP ranges during attacks (business impact assessment required)
- **Proactive Monitoring**: Increased monitoring during sensitive political periods
- **DRT Engagement**: Proactive engagement with AWS DRT during high-risk periods
- **Multi-Region**: Leverage Tokyo region for failover during Taiwan-specific attacks

### Cost Breakdown

**Monthly Costs**:

- AWS Shield Advanced: $3,000/month (base fee)
- WAF: $5/month (Web ACL) + $1/million requests (~$500/month for 100M requests)
- CloudFront: $0.085/GB data transfer (~$500/month for 6TB)
- Route 53: $0.50/hosted zone + $0.40/million queries (~$50/month)
- **Total**: ~$4,050/month

**Cost Protection**:

- Shield Advanced includes DDoS cost protection
- No scaling charges during attacks (EC2, ELB, CloudFront, Route 53)
- Potential savings: $10,000+ during large attacks

### Emergency Procedures

**During Active Attack**:

1. **Immediate**: Verify Shield Advanced is mitigating
2. **5 minutes**: Review WAF logs for application-layer attacks
3. **10 minutes**: Engage AWS DRT if attack persists
4. **15 minutes**: Consider additional mitigation (geo-blocking, rate limiting)
5. **30 minutes**: Communicate with stakeholders
6. **Post-Attack**: Conduct post-mortem and update runbooks

**DRT Escalation**:

- **Trigger**: Attack persists > 15 minutes or causes service degradation
- **Contact**: AWS Support (Enterprise plan) or DRT hotline
- **Information**: Attack type, affected resources, business impact
- **Response Time**: < 15 minutes for P0 incidents

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
