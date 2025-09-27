# DNS Resolution and Disaster Recovery - Operational Viewpoint

**Document Version**: 1.0  
**Last Updated**: September 24, 2025 6:02 PM (Taipei Time)  
**Author**: Operations Team  
**Status**: Active

## 📋 Table of Contents

- [Overview](#overview)
- [DNS Resolution Architecture](#dns-resolution-architecture)
- [User Access Flow](#user-access-flow)
- [Normal Traffic Routing](#normal-traffic-routing)
- [Disaster Recovery Mechanisms](#disaster-recovery-mechanisms)
- [Failover Procedures](#failover-procedures)
- [Health Check Mechanisms](#health-check-mechanisms)
- [Monitoring and Alerting](#monitoring-and-alerting)
- [Operations Manual](#operations-manual)

## Overview

This document provides detailed DNS resolution and disaster recovery procedures for the GenAI Demo system. The system implements a Multi-Region Active-Active architecture using Amazon Route 53 for intelligent DNS resolution and automatic failover capabilities.

### Key Features

- **Multi-Region Deployment**: Primary region (ap-east-2) and secondary region (ap-northeast-1)
- **Intelligent DNS Routing**: Route 53 with health checks and failover policies
- **Automatic Failover**: Sub-5-minute recovery time objective (RTO)
- **Global Performance**: Optimized routing based on user location
- **High Availability**: 99.9% uptime target

## DNS Resolution Architecture

### Route 53 Configuration

The system uses Amazon Route 53 as the authoritative DNS service with the following configuration:

- **Hosted Zone**: kimkao.io
- **Primary Domain**: genai-demo.kimkao.io
- **Health Check Interval**: 30 seconds
- **Failure Threshold**: 3 consecutive failures
- **TTL**: 60 seconds (during failover), 300 seconds (normal operation)

### Routing Policies

1. **Failover Routing**: Primary/secondary configuration for disaster recovery
2. **Latency-based Routing**: Route users to nearest healthy region
3. **Weighted Routing**: Gradual traffic shifting during deployments
4. **Geolocation Routing**: Region-specific routing for compliance

## User Access Flow

### Normal Operation Flow

1. User enters genai-demo.kimkao.io in browser
2. DNS resolver queries Route 53 authoritative servers
3. Route 53 performs health checks on both regions
4. Returns IP address of healthy primary region (ap-east-2)
5. User connects to CloudFront edge location
6. CloudFront forwards request to primary region ALB
7. Request processed by EKS cluster in primary region

### Failover Operation Flow

1. Route 53 health checks detect primary region failure
2. After 3 consecutive failures (90 seconds), marks primary as unhealthy
3. DNS queries automatically return secondary region IP (ap-northeast-1)
4. New user connections route to secondary region
5. Existing connections may experience brief interruption
6. Full failover completed within 5 minutes

## Disaster Recovery Mechanisms

### Health Check Configuration

```yaml
Primary Region Health Check:
  - Endpoint: https://genai-demo.kimkao.io/actuator/health
  - Interval: 30 seconds
  - Timeout: 10 seconds
  - Failure Threshold: 3 attempts
  - Success Threshold: 2 attempts

Secondary Region Health Check:
  - Endpoint: https://genai-demo-backup.kimkao.io/actuator/health
  - Interval: 30 seconds
  - Timeout: 10 seconds
  - Failure Threshold: 3 attempts
  - Success Threshold: 2 attempts
```

### Automatic Failover Triggers

- HTTP 5xx error rate > 50% for 2 minutes
- Response time > 10 seconds for 1 minute
- Connection timeout > 30 seconds
- Health check endpoint unreachable

### Manual Failover Triggers

- Planned maintenance
- Security incidents
- Performance degradation
- Regional service outages

## Monitoring and Alerting

### CloudWatch Alarms

```yaml
DNS Health Check Alarms:
  PrimaryRegionDown:
    - Metric: Route53 HealthCheckStatus
    - Threshold: < 1 (unhealthy)
    - Action: SNS → PagerDuty (P1)
  
  SecondaryRegionDown:
    - Metric: Route53 HealthCheckStatus  
    - Threshold: < 1 (unhealthy)
    - Action: SNS → PagerDuty (P0)

Application Performance Alarms:
  HighErrorRate:
    - Metric: ALB 5xx error rate
    - Threshold: > 5%
    - Action: SNS → Slack + Email
  
  HighLatency:
    - Metric: ALB response time
    - Threshold: > 2 seconds (95th percentile)
    - Action: SNS → Slack
```

### Monitoring Dashboard

Key metrics to monitor:

- DNS resolution time by region
- Health check status for both regions
- Application response times
- Error rates by region
- Traffic distribution between regions
- Failover event frequency

## Operations Manual

### Daily Operations Checklist

- [ ] Verify health checks are passing for both regions
- [ ] Check DNS resolution from multiple global locations
- [ ] Review error rates and response times
- [ ] Validate SSL certificate expiration dates
- [ ] Monitor cost and usage metrics

### Weekly Operations Tasks

- [ ] Test manual failover procedures
- [ ] Review and update DNS configurations
- [ ] Analyze traffic patterns and performance
- [ ] Update disaster recovery documentation
- [ ] Conduct team training on procedures

### Emergency Response Procedures

#### P0 Incident (Complete Service Outage)

1. **Immediate Response** (0-5 minutes)
   - Verify incident scope and impact
   - Check Route 53 health check status
   - Initiate emergency communication

2. **Assessment** (5-15 minutes)
   - Determine root cause
   - Evaluate failover options
   - Coordinate with technical teams

3. **Resolution** (15-30 minutes)
   - Execute failover if automatic failover failed
   - Monitor service restoration
   - Communicate status updates

#### P1 Incident (Partial Service Impact)

1. **Response** (0-15 minutes)
   - Assess impact and affected users
   - Review monitoring dashboards
   - Determine if failover is needed

2. **Investigation** (15-60 minutes)
   - Identify root cause
   - Implement temporary fixes
   - Plan permanent resolution

### Recovery Procedures

#### Failback to Primary Region

1. **Preparation**
   - Verify primary region is fully operational
   - Confirm all services are healthy
   - Plan gradual traffic shift

2. **Execution**
   - Update Route 53 health checks
   - Gradually shift traffic (25%, 50%, 75%, 100%)
   - Monitor error rates and performance

3. **Validation**
   - Confirm all traffic routing correctly
   - Verify application functionality
   - Update monitoring and alerting

## Performance Optimization

### DNS Performance Tuning

- Optimize TTL values for balance between performance and failover speed
- Use Route 53 Resolver for improved resolution times
- Implement DNS prefetching for critical resources
- Monitor and optimize health check intervals

### Global Performance Monitoring

- Deploy synthetic monitoring from multiple global locations
- Track DNS resolution times by region
- Monitor CDN performance and cache hit rates
- Analyze user experience metrics by geography

---

**Related Documents**:
- [DNS Disaster Recovery](dns-disaster-recovery.md) - Detailed technical implementation
- [Deployment Architecture](../deployment/deployment-architecture.md) - Infrastructure deployment details
- [Observability Overview](observability-overview.md) - Monitoring and alerting setup

**Emergency Contacts**:
- Operations Team: ops-team@company.com
- Emergency Hotline: +886-911-DNS-OPS (911-367-677)
- PagerDuty: Automatic escalation for P0/P1 incidents
