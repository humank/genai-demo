---
adr_number: 030
title: "API Gateway Pattern"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [9, 23, 14, 50]
affected_viewpoints: ["functional", "deployment", "operational"]
affected_perspectives: ["security", "performance", "evolution"]
decision_makers: ["Architecture Team", "Backend Team", "DevOps Team"]
---

# ADR-030: API Gateway Pattern

## Status

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Backend Team, DevOps Team

## Context

### Problem Statement

The Enterprise E-Commerce Platform consists of multiple microservices (13 bounded contexts) that need to be exposed to external clients (web frontends, mobile apps, third-party integrations). We need to decide on an API Gateway strategy to:

- Provide a unified entry point for all client requests
- Handle cross-cutting concerns (authentication, rate limiting, logging)
- Route requests to appropriate backend services
- Transform requests/responses as needed
- Protect backend services from direct exposure

This decision impacts:

- API security and authentication
- Request routing and load balancing
- Rate limiting and throttling
- API versioning and evolution
- Monitoring and observability
- Developer experience

### Business Context

**Business Drivers**:

- Unified API experience for frontend applications
- Centralized security and authentication
- Simplified client integration (single endpoint)
- Consistent rate limiting and throttling
- Reduced backend service complexity
- Support for API versioning and evolution

**Business Constraints**:

- Multiple client types (Next.js CMC, Angular Consumer, Mobile apps, Third-party integrations)
- High traffic volume (peak: 10,000 req/s)
- Low latency requirements (< 100ms gateway overhead)
- 99.9% availability requirement
- Cost optimization for AWS services
- Support for gradual migration from monolith

**Business Requirements**:

- Single entry point for all API requests
- JWT-based authentication at gateway level
- Rate limiting per client/IP/endpoint
- Request/response transformation
- API versioning support (v1, v2)
- Comprehensive logging and monitoring

### Technical Context

**Current Architecture**:

- Backend: Spring Boot microservices (13 bounded contexts)
- Frontend: Next.js (CMC), Angular (Consumer)
- Authentication: JWT tokens (ADR-014)
- Rate Limiting: Application-level (ADR-023, ADR-050)
- Deployment: AWS EKS (ADR-018)
- Observability: CloudWatch + X-Ray + Grafana (ADR-008)

**Technical Constraints**:

- Must integrate with existing JWT authentication
- Must support existing rate limiting strategy
- Must work with AWS EKS deployment
- Must provide low latency (< 100ms overhead)
- Must support high throughput (10,000 req/s)
- Must integrate with existing observability stack

**Dependencies**:

- ADR-009: RESTful API Design (API standards)
- ADR-023: API Rate Limiting Strategy (rate limiting implementation)
- ADR-014: JWT-Based Authentication (authentication mechanism)
- ADR-050: API Security and Rate Limiting Strategy (security requirements)

## Decision Drivers

- **Performance**: Low latency overhead (< 100ms)
- **Scalability**: Support high throughput (10,000 req/s)
- **Security**: Centralized authentication and authorization
- **Flexibility**: Easy to configure routing and transformations
- **Cost**: Optimize AWS service costs
- **Maintainability**: Simple to operate and troubleshoot
- **Integration**: Seamless integration with existing infrastructure

## Considered Options

### Option 1: AWS API Gateway (Managed Service)

**Description**:
Use AWS API Gateway as a fully managed API gateway service with built-in features for authentication, rate limiting, caching, and monitoring.

**Pros** ✅:

- **Fully Managed**: No infrastructure to manage, automatic scaling
- **Native AWS Integration**: Seamless integration with Lambda, Cognito, WAF, CloudWatch
- **Built-in Features**: Authentication, rate limiting, caching, request/response transformation
- **High Availability**: Multi-AZ deployment, 99.95% SLA
- **Security**: AWS WAF integration, API keys, usage plans
- **Monitoring**: CloudWatch metrics, X-Ray tracing, access logs
- **Cost-Effective for Low Traffic**: Pay-per-request pricing
- **Easy Setup**: Quick to configure via AWS Console or CDK

**Cons** ❌:

- **Vendor Lock-in**: Tightly coupled to AWS ecosystem
- **Cost at Scale**: Expensive for high traffic (>10M requests/month)
- **Limited Customization**: Restricted to AWS-provided features
- **Cold Start**: REST API has 29-second timeout, HTTP API has 30-second timeout
- **Complexity**: Two types (REST API vs HTTP API) with different features
- **Performance**: Additional network hop, potential latency
- **Limited Transformation**: VTL (Velocity Template Language) for transformations

**Cost**:

- **REST API**: $3.50 per million requests + $0.09/GB data transfer
- **HTTP API**: $1.00 per million requests + $0.09/GB data transfer
- **Estimated Monthly Cost** (100M requests): $100-350 + data transfer
- **Total Cost of Ownership (3 years)**: ~$15,000-50,000

**Risk**: Low

**Risk Description**: Proven AWS service with high reliability

**Effort**: Low

**Effort Description**: Quick setup with AWS CDK, minimal configuration

### Option 2: Kong Gateway (Open Source / Enterprise)

**Description**:
Deploy Kong Gateway as a self-hosted API gateway on EKS with plugins for authentication, rate limiting, logging, and transformations.

**Pros** ✅:

- **Open Source**: Free community edition, no vendor lock-in
- **Highly Customizable**: Extensive plugin ecosystem (50+ plugins)
- **Performance**: Low latency (< 10ms overhead), high throughput
- **Flexibility**: Custom plugins in Lua, support for any backend
- **Multi-Cloud**: Works on any Kubernetes cluster, not AWS-specific
- **Rich Features**: Authentication, rate limiting, caching, transformations, circuit breaker
- **Developer Experience**: Declarative configuration, GitOps-friendly
- **Enterprise Option**: Kong Enterprise for advanced features (RBAC, analytics)

**Cons** ❌:

- **Self-Hosted**: Need to manage infrastructure, scaling, updates
- **Operational Overhead**: Monitoring, logging, troubleshooting required
- **Database Dependency**: Requires PostgreSQL for configuration storage
- **Learning Curve**: Need to learn Kong configuration and plugin system
- **High Availability**: Need to configure multi-replica deployment
- **Cost**: Infrastructure costs (EC2, RDS) + operational overhead
- **Enterprise Features**: Advanced features require paid license

**Cost**:

- **Infrastructure**: $500-1000/month (EKS nodes, RDS PostgreSQL)
- **Operational Overhead**: 2 person-days/month ($2,000/month)
- **Enterprise License** (optional): $3,000-10,000/month
- **Total Cost of Ownership (3 years)**: ~$100,000-150,000 (community) or $200,000-400,000 (enterprise)

**Risk**: Medium

**Risk Description**: Requires operational expertise, potential downtime during upgrades

**Effort**: High

**Effort Description**: Significant setup and configuration, ongoing maintenance

### Option 3: Spring Cloud Gateway (Application-Level)

**Description**:
Deploy Spring Cloud Gateway as a Spring Boot application on EKS, leveraging Spring ecosystem for routing, filtering, and integration.

**Pros** ✅:

- **Spring Ecosystem**: Native integration with Spring Boot, Spring Security, Spring Cloud
- **Java-Based**: Familiar technology for Java developers, easy to customize
- **Reactive**: Built on Spring WebFlux for high performance
- **Flexible Routing**: Powerful routing DSL, predicates, and filters
- **Custom Filters**: Easy to write custom filters in Java
- **Observability**: Spring Boot Actuator, Micrometer integration
- **No Additional Infrastructure**: Runs as Spring Boot app on existing EKS
- **Cost-Effective**: No additional service costs, only compute resources

**Cons** ❌:

- **Self-Hosted**: Need to manage deployment, scaling, updates
- **Limited Features**: Fewer built-in features compared to Kong or AWS API Gateway
- **Operational Overhead**: Monitoring, logging, troubleshooting required
- **Performance**: Higher latency than Kong (Java overhead)
- **Scaling**: Need to configure auto-scaling, load balancing
- **High Availability**: Need to deploy multiple replicas
- **Learning Curve**: Need to learn Spring Cloud Gateway configuration

**Cost**:

- **Infrastructure**: $300-500/month (EKS nodes)
- **Operational Overhead**: 1.5 person-days/month ($1,500/month)
- **Total Cost of Ownership (3 years)**: ~$70,000-90,000

**Risk**: Medium

**Risk Description**: Requires Spring expertise, potential performance issues

**Effort**: Medium

**Effort Description**: Moderate setup, requires Spring Cloud Gateway knowledge

## Decision Outcome

**Chosen Option**: Option 2 - Kong Gateway (Open Source)

**Rationale**:
We chose Kong Gateway as our API gateway solution. This decision prioritizes performance, flexibility, and long-term cost optimization over managed service convenience:

1. **Performance**: Kong provides < 10ms latency overhead compared to AWS API Gateway's higher latency. For high-traffic e-commerce platform (10,000 req/s peak), this translates to significant performance improvement.

2. **Cost Optimization**: At our traffic volume (100M+ requests/month), Kong's infrastructure costs ($500-1000/month) are significantly lower than AWS API Gateway's pay-per-request pricing ($100-350/month + data transfer). Long-term savings justify operational overhead.

3. **Flexibility and Customization**: Kong's extensive plugin ecosystem (50+ plugins) and ability to write custom Lua plugins provide flexibility for future requirements (custom authentication, advanced rate limiting, request transformations).

4. **Multi-Cloud Strategy**: Kong works on any Kubernetes cluster, supporting potential future multi-cloud or hybrid cloud strategy. Not locked into AWS ecosystem.

5. **Developer Experience**: Declarative configuration and GitOps-friendly approach align with our infrastructure-as-code strategy (ADR-007). Configuration stored in Git, versioned, and reviewed.

6. **Enterprise Readiness**: Kong Community Edition provides all essential features. Option to upgrade to Kong Enterprise for advanced features (RBAC, analytics, developer portal) if needed.

7. **Kubernetes Native**: Kong runs natively on Kubernetes (EKS), leveraging existing container orchestration infrastructure (ADR-018). No additional infrastructure required.

8. **Proven at Scale**: Kong is used by major companies (Nasdaq, Expedia, Samsung) handling billions of requests per day. Proven reliability and performance.

**Key Factors in Decision**:

1. **Performance**: < 10ms latency overhead critical for user experience
2. **Cost**: Long-term cost savings at high traffic volume
3. **Flexibility**: Extensive plugin ecosystem for future requirements
4. **Team Capability**: Team has Kubernetes expertise, can manage Kong deployment

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Backend Team | Medium | Need to configure Kong routing and plugins | Training on Kong configuration, documentation |
| DevOps Team | High | Responsible for Kong deployment and operations | Kong training, operational runbooks, monitoring setup |
| Frontend Team | Low | Transparent change, same API endpoints | Communication about gateway deployment |
| Security Team | Medium | Need to configure authentication and rate limiting | Security configuration review, penetration testing |
| Operations Team | High | New component to monitor and troubleshoot | Monitoring dashboards, alerting, runbooks |

### Impact Radius Assessment

**Selected Impact Radius**: System

**Impact Description**:

- **System**: Changes affect entire API infrastructure
  - All API requests routed through Kong Gateway
  - All services must be configured in Kong
  - All authentication handled at gateway level
  - All rate limiting enforced at gateway level
  - All API monitoring includes gateway metrics

### Affected Components

- **All Backend Services**: Configured as Kong upstreams
- **Frontend Applications**: API calls routed through Kong
- **Authentication**: JWT validation at Kong gateway
- **Rate Limiting**: Enforced at Kong gateway
- **Monitoring**: Kong metrics added to observability stack
- **CI/CD**: Kong configuration deployed via CDK

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| Kong deployment failure | Low | High | Blue-green deployment, rollback plan | DevOps Team |
| Performance degradation | Medium | High | Load testing, performance monitoring | DevOps Team |
| Configuration errors | Medium | Medium | Configuration validation, staging testing | Backend Team |
| Database failure (PostgreSQL) | Low | High | Multi-AZ RDS, automated backups | DevOps Team |
| Learning curve | High | Low | Training sessions, documentation | Tech Lead |
| Operational overhead | Medium | Medium | Automation, monitoring, runbooks | DevOps Team |

**Overall Risk Level**: Medium

**Risk Mitigation Plan**:

- Comprehensive load testing before production deployment
- Blue-green deployment strategy for zero-downtime migration
- Multi-AZ RDS PostgreSQL for Kong configuration database
- Automated monitoring and alerting for Kong health
- Detailed operational runbooks for common issues
- Training sessions for DevOps and Backend teams

## Implementation Plan

### Phase 1: Kong Setup and Configuration (Timeline: Week 1-2)

**Objectives**:

- Deploy Kong on EKS
- Configure PostgreSQL database
- Set up basic routing

**Tasks**:

- [ ] Deploy PostgreSQL RDS for Kong configuration (Multi-AZ)
- [ ] Deploy Kong on EKS using Helm chart (2 replicas minimum)
- [ ] Configure Kong Ingress Controller
- [ ] Set up Kong Admin API access (secured)
- [ ] Configure basic routing for one service (Customer API)
- [ ] Test basic request routing
- [ ] Configure health checks and readiness probes

**Deliverables**:

- Kong deployed on EKS with PostgreSQL backend
- Basic routing working for one service
- Health checks configured

**Success Criteria**:

- Kong pods running and healthy
- PostgreSQL database accessible
- Basic routing working with < 10ms latency overhead

### Phase 2: Authentication and Security (Timeline: Week 2-3)

**Objectives**:

- Configure JWT authentication
- Set up rate limiting
- Integrate with AWS WAF

**Tasks**:

- [ ] Configure Kong JWT plugin for authentication
- [ ] Integrate with existing JWT token validation (ADR-014)
- [ ] Configure rate limiting plugin (ADR-023, ADR-050)
- [ ] Set up API key authentication for third-party integrations
- [ ] Configure CORS plugin for frontend applications
- [ ] Integrate Kong with AWS WAF (ADR-049)
- [ ] Configure request/response logging
- [ ] Test authentication and rate limiting

**Deliverables**:

- JWT authentication working at gateway
- Rate limiting enforced
- Security plugins configured

**Success Criteria**:

- JWT tokens validated correctly
- Rate limiting working per client/IP/endpoint
- Unauthorized requests blocked

### Phase 3: Service Migration (Timeline: Week 3-5)

**Objectives**:

- Migrate all services to Kong
- Configure routing for all endpoints
- Test end-to-end flows

**Tasks**:

- [ ] Configure Kong routes for all 13 bounded contexts
- [ ] Set up service-specific rate limits
- [ ] Configure request/response transformations (if needed)
- [ ] Migrate Customer API endpoints
- [ ] Migrate Order API endpoints
- [ ] Migrate Product API endpoints
- [ ] Migrate remaining service endpoints
- [ ] Test all API endpoints through Kong
- [ ] Validate performance and latency

**Deliverables**:

- All services routed through Kong
- All endpoints tested and working
- Performance validated

**Success Criteria**:

- All API endpoints accessible through Kong
- Latency overhead < 10ms
- No functional regressions

### Phase 4: Monitoring and Operations (Timeline: Week 5-6)

**Objectives**:

- Set up monitoring and alerting
- Create operational runbooks
- Train operations team

**Tasks**:

- [ ] Configure Kong Prometheus plugin for metrics
- [ ] Create Grafana dashboards for Kong metrics
- [ ] Set up CloudWatch alarms for Kong health
- [ ] Configure X-Ray tracing through Kong
- [ ] Create operational runbooks (deployment, troubleshooting, scaling)
- [ ] Document Kong configuration and plugins
- [ ] Conduct training sessions for DevOps and Backend teams
- [ ] Perform load testing and capacity planning
- [ ] Create disaster recovery procedures

**Deliverables**:

- Monitoring dashboards operational
- Alerting configured
- Operational runbooks created
- Team trained

**Success Criteria**:

- All Kong metrics visible in Grafana
- Alerts triggering correctly
- Team comfortable with Kong operations

### Rollback Strategy

**Trigger Conditions**:

- Kong gateway unavailable for > 5 minutes
- Performance degradation > 50ms latency increase
- Critical security vulnerability discovered
- Database failure preventing Kong operation

**Rollback Steps**:

1. **Immediate Action**: Route traffic directly to backend services (bypass Kong)
2. **DNS Update**: Update Route 53 to point to backend load balancers
3. **Service Validation**: Verify backend services accessible directly
4. **Communication**: Notify team of rollback and investigation plan
5. **Root Cause Analysis**: Investigate Kong failure and plan remediation

**Rollback Time**: 5-10 minutes (DNS propagation)

**Rollback Testing**: Test rollback procedure in staging environment monthly

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| Gateway Latency | < 10ms (p95) | Kong Prometheus metrics | Real-time |
| Gateway Availability | > 99.9% | Kong health checks | Real-time |
| Request Success Rate | > 99.5% | Kong access logs | Daily |
| Authentication Success Rate | > 99.9% | Kong JWT plugin metrics | Daily |
| Rate Limit Accuracy | 100% | Kong rate limiting metrics | Weekly |

### Monitoring Plan

**Dashboards**:

- **Kong Performance Dashboard**: Latency, throughput, error rates
- **Kong Security Dashboard**: Authentication failures, rate limit hits, blocked requests
- **Kong Health Dashboard**: Pod health, database connections, resource usage

**Alerts**:

- **Critical**: Kong gateway unavailable (PagerDuty)
- **Critical**: Database connection failure (PagerDuty)
- **Warning**: Latency > 50ms p95 (Slack)
- **Warning**: Error rate > 1% (Slack)
- **Info**: Rate limit threshold reached (Slack)

**Review Schedule**:

- **Real-time**: Automated monitoring and alerting
- **Daily**: Review error logs and performance metrics
- **Weekly**: Capacity planning and optimization review
- **Monthly**: Comprehensive performance and cost review

### Key Performance Indicators (KPIs)

- **Performance KPI**: Gateway latency < 10ms (p95)
- **Reliability KPI**: Gateway availability > 99.9%
- **Security KPI**: Zero unauthorized access incidents
- **Cost KPI**: Gateway infrastructure cost < $1,000/month

## Consequences

### Positive Consequences ✅

- **Centralized Security**: Authentication and authorization at gateway level
- **Simplified Clients**: Single entry point for all API requests
- **Performance**: Low latency overhead (< 10ms)
- **Flexibility**: Extensive plugin ecosystem for future requirements
- **Cost Optimization**: Lower cost at high traffic volume
- **Multi-Cloud Ready**: Not locked into AWS ecosystem
- **Developer Experience**: Declarative configuration, GitOps-friendly
- **Observability**: Centralized logging and monitoring

### Negative Consequences ❌

- **Operational Overhead**: Need to manage Kong deployment and operations (Mitigation: Automation, monitoring, runbooks)
- **Learning Curve**: Team needs to learn Kong configuration (Mitigation: Training and documentation)
- **Database Dependency**: Kong requires PostgreSQL (Mitigation: Multi-AZ RDS with automated backups)
- **Single Point of Failure**: Gateway failure affects all services (Mitigation: Multi-replica deployment, health checks, rollback plan)

### Technical Debt

**Debt Introduced**:

- **Kong Expertise**: Team needs to maintain Kong expertise
- **Configuration Management**: Kong configuration needs to be versioned and managed
- **Database Maintenance**: PostgreSQL database requires regular maintenance

**Debt Repayment Plan**:

- **Training**: Quarterly Kong training sessions for team
- **Documentation**: Maintain comprehensive Kong documentation
- **Automation**: Automate Kong configuration deployment and updates
- **Monitoring**: Continuous monitoring and optimization

### Long-term Implications

This decision establishes Kong Gateway as our API gateway for the next 3-5 years. As the platform evolves:

- Consider Kong Enterprise for advanced features (RBAC, analytics, developer portal)
- Evaluate Kong Mesh for service mesh capabilities
- Monitor Kong performance and optimize configuration
- Keep Kong and plugins updated to latest versions
- Reassess if traffic patterns change significantly (> 100,000 req/s)

Kong Gateway provides foundation for API management, enabling centralized security, rate limiting, and monitoring while maintaining high performance and flexibility.

## Related Decisions

### Related ADRs

- [ADR-009: RESTful API Design](009-restful-api-design-with-openapi.md) - API design standards
- [ADR-023: API Rate Limiting Strategy](023-api-rate-limiting-strategy.md) - Rate limiting implementation
- [ADR-014: JWT-Based Authentication](014-jwt-based-authentication-strategy.md) - Authentication mechanism
- [ADR-050: API Security and Rate Limiting Strategy](050-api-security-and-rate-limiting-strategy.md) - Security requirements

### Affected Viewpoints

- [Functional Viewpoint](../../viewpoints/functional/README.md) - API routing and functionality
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Kong deployment on EKS
- [Operational Viewpoint](../../viewpoints/operational/README.md) - Kong operations and monitoring

### Affected Perspectives

- [Security Perspective](../../perspectives/security/README.md) - Centralized authentication and authorization
- [Performance Perspective](../../perspectives/performance/README.md) - Gateway latency and throughput
- [Evolution Perspective](../../perspectives/evolution/README.md) - API versioning and evolution

## Notes

### Assumptions

- Traffic volume: 100M+ requests/month
- Team has Kubernetes expertise
- PostgreSQL RDS available for Kong configuration
- AWS EKS cluster available for Kong deployment
- Team willing to learn Kong configuration

### Constraints

- Must integrate with existing JWT authentication (ADR-014)
- Must support existing rate limiting strategy (ADR-023, ADR-050)
- Must work with AWS EKS deployment (ADR-018)
- Must provide low latency (< 100ms overhead)
- Must support high throughput (10,000 req/s)

### Open Questions

- Should we use Kong Community Edition or Kong Enterprise?
- What is optimal number of Kong replicas for high availability?
- Should we use Kong DB-less mode or PostgreSQL mode?
- How to handle Kong configuration versioning and rollback?

### Follow-up Actions

- [ ] Deploy Kong on staging EKS cluster - DevOps Team
- [ ] Configure PostgreSQL RDS for Kong - DevOps Team
- [ ] Create Kong configuration templates - Backend Team
- [ ] Develop Kong operational runbooks - DevOps Team
- [ ] Conduct Kong training sessions - Tech Lead
- [ ] Perform load testing and capacity planning - DevOps Team
- [ ] Set up monitoring dashboards and alerts - DevOps Team

### References

- [Kong Gateway Documentation](https://docs.konghq.com/gateway/latest/)
- [Kong on Kubernetes](https://docs.konghq.com/kubernetes-ingress-controller/latest/)
- [Kong Plugin Hub](https://docs.konghq.com/hub/)
- [Kong Performance Benchmarks](https://konghq.com/blog/kong-gateway-performance)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [Kong vs AWS API Gateway Comparison](https://konghq.com/blog/kong-vs-aws-api-gateway)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
