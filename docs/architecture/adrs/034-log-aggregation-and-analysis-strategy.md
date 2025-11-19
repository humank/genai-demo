---
adr_number: 034
title: "Log Aggregation and Analysis Strategy"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [8, 43]
affected_viewpoints: ["operational", "deployment"]
affected_perspectives: ["availability", "performance", "security"]
decision_makers: ["Architecture Team", "Operations Team", "Security Team"]
---

# ADR-034: Log Aggregation and Analysis Strategy

## Status

**Status**: Accepted

**Date**: 2025-10-25

**Decision Makers**: Architecture Team, Operations Team, Security Team

## Context

### Problem Statement

The Enterprise E-Commerce Platform generates logs from multiple sources across distributed services, requiring a centralized log aggregation and analysis solution. We need to:

- Collect logs from all services (backend, frontend, infrastructure)
- Enable real-time log search and analysis
- Support security incident investigation
- Maintain compliance with data retention policies
- Provide cost-effective long-term log storage
- Enable correlation across multi-region deployments

### Business Context

**Business Drivers**:

- Regulatory compliance requiring audit trails (GDPR, PCI-DSS, Taiwan Personal Data Protection Act)
- Security incident response requiring rapid log analysis
- Operational troubleshooting requiring centralized log access
- Performance optimization requiring log-based insights
- Cost optimization requiring efficient log storage

**Business Constraints**:

- Budget constraints for log storage and analysis tools
- Data residency requirements for Taiwan and Japan regions
- 7-year retention requirement for audit logs
- Real-time alerting requirements for security events

**Business Requirements**:

- Support 1TB+ daily log volume
- Enable sub-second search response times
- Provide 99.9% log ingestion availability
- Support structured logging format
- Enable cross-region log correlation

### Technical Context

**Current Architecture**:

- Services log to stdout/stderr (12-factor app pattern)
- Container logs collected by Kubernetes
- No centralized log aggregation currently
- Limited log retention (7 days local)
- Manual log analysis required

**Technical Constraints**:

- Must integrate with AWS EKS
- Must support structured JSON logging
- Must handle high log volume (1TB+/day)
- Must support multi-region deployment
- Must integrate with existing monitoring (CloudWatch, Grafana)

**Dependencies**:

- ADR-008: Observability platform (CloudWatch + X-Ray + Grafana)
- ADR-043: Multi-region observability
- AWS services (CloudWatch Logs, S3, Athena, Kinesis)

## Decision Drivers

- **Cost Efficiency**: Need cost-effective solution for 1TB+/day log volume with 7-year retention
- **Search Performance**: Sub-second search response for operational troubleshooting
- **Scalability**: Handle growing log volume as system scales
- **Integration**: Seamless integration with existing AWS infrastructure
- **Compliance**: Meet regulatory retention and audit requirements
- **Multi-Region**: Support cross-region log correlation
- **Real-time Analysis**: Enable real-time alerting and anomaly detection

## Considered Options

### Option 1: AWS CloudWatch Logs + S3 + Athena

**Description**:
Use AWS native services for log aggregation. CloudWatch Logs for real-time ingestion and short-term storage, S3 for long-term archival, and Athena for ad-hoc analysis.

**Pros** ✅:

- Native AWS integration with EKS, Lambda, RDS
- No infrastructure management required
- Automatic scaling and high availability
- Cost-effective long-term storage with S3
- Integrated with CloudWatch Alarms and Metrics
- Built-in encryption and access control
- Multi-region support with cross-region replication

**Cons** ❌:

- CloudWatch Logs expensive for high volume ($0.50/GB ingestion + $0.03/GB storage)
- Athena query performance slower than dedicated search engines
- Limited real-time search capabilities
- Query costs can be high for frequent analysis
- Less flexible than dedicated log platforms

**Cost**:

- **Implementation Cost**: 2 person-weeks (configuration and setup)
- **Monthly Cost**:
  - CloudWatch Logs: $500/TB ingestion + $30/TB/month storage (30-day retention)
  - S3 Glacier: $4/TB/month (long-term archival)
  - Athena: $5/TB scanned
  - Total: ~$550/month for 1TB/day + $5-20/month Athena queries
- **Total Cost of Ownership (3 years)**: ~$20,000

**Risk**: Low

**Risk Description**: Proven AWS service with high reliability, but cost can escalate with volume

**Effort**: Low

**Effort Description**: Minimal setup, mostly configuration

### Option 2: ELK Stack (Elasticsearch + Logstash + Kibana)

**Description**:
Deploy self-managed ELK stack on EKS for full-featured log aggregation, search, and visualization.

**Pros** ✅:

- Powerful full-text search capabilities
- Rich visualization with Kibana
- Flexible data transformation with Logstash
- Real-time search and analysis
- Large ecosystem and community support
- Advanced analytics and machine learning features

**Cons** ❌:

- High operational overhead (cluster management, upgrades, scaling)
- Expensive infrastructure costs (EC2, EBS for Elasticsearch cluster)
- Requires dedicated team expertise
- Complex multi-region setup
- High memory and CPU requirements
- Elasticsearch licensing concerns (Elastic License vs Open Source)

**Cost**:

- **Implementation Cost**: 6 person-weeks (cluster setup, configuration, integration)
- **Monthly Cost**:
  - EC2 instances: $2,000/month (3x r5.2xlarge for Elasticsearch)
  - EBS storage: $500/month (5TB SSD)
  - Data transfer: $200/month
  - Total: ~$2,700/month
- **Total Cost of Ownership (3 years)**: ~$100,000 + operational overhead

**Risk**: High

**Risk Description**: Operational complexity, potential downtime during upgrades, scaling challenges

**Effort**: High

**Effort Description**: Significant setup and ongoing maintenance effort

### Option 3: Grafana Loki

**Description**:
Use Grafana Loki for log aggregation with S3 backend storage, integrated with existing Grafana dashboards.

**Pros** ✅:

- Cost-effective (indexes only metadata, not full text)
- Native integration with Grafana
- Horizontally scalable
- S3 backend for cost-effective storage
- Simple query language (LogQL)
- Lower resource requirements than Elasticsearch
- Open source with active community

**Cons** ❌:

- Limited full-text search capabilities
- Less mature than ELK stack
- Requires label-based querying (learning curve)
- Limited advanced analytics features
- Smaller ecosystem compared to Elasticsearch

**Cost**:

- **Implementation Cost**: 4 person-weeks (deployment, configuration, integration)
- **Monthly Cost**:
  - EC2 instances: $800/month (Loki components)
  - S3 storage: $23/TB/month
  - Data transfer: $100/month
  - Total: ~$950/month for 1TB/day
- **Total Cost of Ownership (3 years)**: ~$35,000

**Risk**: Medium

**Risk Description**: Less mature platform, potential limitations for complex queries

**Effort**: Medium

**Effort Description**: Moderate setup effort, simpler than ELK

## Decision Outcome

**Chosen Option**: Option 1 - AWS CloudWatch Logs + S3 + Athena

**Rationale**:
We chose AWS CloudWatch Logs with S3 archival and Athena for analysis as the optimal solution for our log aggregation needs. This decision balances cost, operational simplicity, and functionality:

1. **Cost Efficiency**: While CloudWatch Logs has higher ingestion costs, the overall TCO is lowest when considering operational overhead. S3 Glacier provides extremely cost-effective long-term storage for compliance.

2. **Operational Simplicity**: As a fully managed service, CloudWatch Logs requires minimal operational overhead, allowing our team to focus on application development rather than infrastructure management.

3. **AWS Integration**: Native integration with our existing AWS infrastructure (EKS, RDS, Lambda) provides seamless log collection without additional agents or configuration.

4. **Scalability**: Automatic scaling handles our growing log volume without manual intervention.

5. **Multi-Region Support**: CloudWatch Logs supports our multi-region architecture (Taipei + Tokyo) with cross-region log aggregation.

6. **Compliance**: Built-in encryption, access control, and retention policies meet our regulatory requirements.

**Key Factors in Decision**:

1. **Total Cost of Ownership**: $20K over 3 years vs $100K (ELK) or $35K (Loki)
2. **Operational Overhead**: Zero infrastructure management vs significant effort for self-managed solutions
3. **Time to Value**: 2 weeks implementation vs 4-6 weeks for alternatives
4. **Risk Profile**: Low risk with proven AWS service vs higher risk with self-managed platforms

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation Strategy |
|-------------|--------------|-------------|-------------------|
| Development Team | Medium | Need to adopt structured logging format | Provide logging libraries and examples |
| Operations Team | Low | Simplified log access through CloudWatch console | Training on CloudWatch Logs Insights |
| Security Team | Low | Centralized security log analysis | Configure security-specific log groups and alarms |
| Business | Low | Improved incident response time | Demonstrate value through metrics |
| Compliance Team | Low | Automated audit trail retention | Document retention policies and access controls |

### Impact Radius Assessment

**Selected Impact Radius**: System

**Impact Description**:

- **System**: Changes affect all services across all bounded contexts
  - All services must adopt structured JSON logging
  - All services must configure CloudWatch Logs agent
  - All services must follow log retention policies
  - Cross-region log correlation requires unified log format

### Affected Components

- **All Microservices**: Must implement structured logging
- **EKS Cluster**: Configure Fluent Bit for log forwarding
- **Lambda Functions**: Configure CloudWatch Logs integration
- **RDS Databases**: Enable CloudWatch Logs export
- **Monitoring Dashboards**: Add log-based metrics and alerts

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy | Owner |
|------|-------------|--------|-------------------|-------|
| High log volume costs | Medium | High | Implement log sampling, optimize log levels, use S3 archival | Operations Team |
| CloudWatch Logs service limits | Low | Medium | Monitor usage, request limit increases proactively | Operations Team |
| Log data loss during ingestion | Low | High | Configure retry logic, monitor ingestion metrics | Development Team |
| Slow Athena query performance | Medium | Medium | Optimize partitioning, use columnar format (Parquet) | Data Team |
| Cross-region log correlation complexity | Medium | Medium | Implement unified log format with region tags | Architecture Team |

**Overall Risk Level**: Low

**Risk Mitigation Plan**:

- Implement cost monitoring and alerting for CloudWatch Logs usage
- Establish log retention policies (30 days hot, 7 years cold)
- Configure log sampling for high-volume debug logs
- Use S3 Intelligent-Tiering for cost optimization
- Implement structured logging standards across all services

## Implementation Plan

### Phase 1: Foundation Setup (Timeline: Week 1)

**Objectives**:

- Configure CloudWatch Logs infrastructure
- Establish logging standards
- Set up S3 archival

**Tasks**:

- [ ] Create CloudWatch Log Groups for each service
- [ ] Configure log retention policies (30 days)
- [ ] Set up S3 bucket for log archival with lifecycle policies
- [ ] Configure S3 Glacier transition (30 days → Glacier)
- [ ] Create IAM roles and policies for log access
- [ ] Document structured logging format (JSON schema)

**Deliverables**:

- CloudWatch Log Groups configured
- S3 archival bucket with lifecycle policies
- Logging standards documentation

**Success Criteria**:

- All log groups created and accessible
- S3 archival working with automatic transition
- IAM policies tested and validated

### Phase 2: Service Integration (Timeline: Week 2-3)

**Objectives**:

- Integrate all services with CloudWatch Logs
- Implement structured logging
- Configure log forwarding

**Tasks**:

- [ ] Deploy Fluent Bit DaemonSet on EKS clusters
- [ ] Configure Fluent Bit to forward logs to CloudWatch
- [ ] Update all services to use structured JSON logging
- [ ] Implement logging libraries for Java/TypeScript
- [ ] Configure RDS CloudWatch Logs export
- [ ] Configure Lambda CloudWatch Logs integration
- [ ] Add correlation IDs for request tracing

**Deliverables**:

- All services logging to CloudWatch
- Structured JSON log format implemented
- Correlation IDs for distributed tracing

**Success Criteria**:

- 100% of services sending logs to CloudWatch
- All logs in structured JSON format
- Correlation IDs present in all service logs

### Phase 3: Analysis and Alerting (Timeline: Week 4)

**Objectives**:

- Configure log analysis with Athena
- Set up log-based metrics and alarms
- Create operational dashboards

**Tasks**:

- [ ] Configure Athena for S3 log analysis
- [ ] Create Athena tables with partitioning
- [ ] Set up CloudWatch Logs Insights queries
- [ ] Configure log-based metrics (error rates, latency)
- [ ] Create CloudWatch Alarms for critical errors
- [ ] Build Grafana dashboards for log visualization
- [ ] Document common log queries and runbooks

**Deliverables**:

- Athena configured for log analysis
- Log-based metrics and alarms
- Operational dashboards in Grafana

**Success Criteria**:

- Athena queries return results in < 10 seconds
- Critical error alarms triggering correctly
- Dashboards showing real-time log metrics

### Rollback Strategy

**Trigger Conditions**:

- CloudWatch Logs ingestion failure rate > 5%
- Log storage costs exceed budget by 50%
- Critical service performance degradation due to logging overhead

**Rollback Steps**:

1. **Immediate Action**: Disable Fluent Bit log forwarding for non-critical services
2. **Service Rollback**: Revert to local container logging
3. **Cost Control**: Reduce log retention to 7 days
4. **Verification**: Confirm services operating normally without CloudWatch Logs

**Rollback Time**: < 30 minutes

**Rollback Testing**: Test rollback procedure in staging environment monthly

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement Method | Review Frequency |
|--------|--------|-------------------|------------------|
| Log Ingestion Success Rate | > 99.9% | CloudWatch Metrics | Daily |
| Log Search Response Time | < 5 seconds | CloudWatch Logs Insights | Weekly |
| Log Storage Cost | < $600/month | AWS Cost Explorer | Weekly |
| Log Retention Compliance | 100% | Automated audit | Monthly |
| Cross-Region Log Correlation | < 10 seconds lag | Custom metric | Daily |

### Monitoring Plan

**Dashboards**:

- **CloudWatch Logs Dashboard**: Ingestion rates, error rates, storage usage
- **Grafana Log Analytics Dashboard**: Log-based metrics, error trends, service health
- **Cost Dashboard**: Daily log costs, storage costs, Athena query costs

**Alerts**:

- **Critical**: Log ingestion failure rate > 5% (PagerDuty)
- **Warning**: Log storage cost > $500/month (Email)
- **Info**: Athena query cost > $20/day (Slack)

**Review Schedule**:

- **Daily**: Quick metrics check (ingestion rate, error rate)
- **Weekly**: Cost review and optimization opportunities
- **Monthly**: Retention policy compliance audit

### Key Performance Indicators (KPIs)

- **Operational KPI**: Mean Time To Detect (MTTD) incidents < 5 minutes
- **Cost KPI**: Log cost per GB < $0.60
- **Compliance KPI**: 100% audit log retention for 7 years
- **Performance KPI**: 95th percentile log search time < 10 seconds

## Consequences

### Positive Consequences ✅

- **Simplified Operations**: No infrastructure to manage, automatic scaling
- **Cost Predictability**: Clear cost structure based on log volume
- **Fast Implementation**: 4 weeks to full deployment vs 8-12 weeks for self-managed
- **High Availability**: AWS SLA guarantees 99.9% uptime
- **Compliance Ready**: Built-in encryption, retention, and access controls
- **Multi-Region Support**: Native cross-region log aggregation
- **Integration**: Seamless integration with existing AWS services

### Negative Consequences ❌

- **Vendor Lock-in**: Tied to AWS CloudWatch Logs ecosystem (Mitigation: Use standard log formats for portability)
- **Limited Search**: Less powerful than Elasticsearch for complex queries (Mitigation: Use Athena for ad-hoc analysis)
- **Cost Scaling**: Costs increase linearly with log volume (Mitigation: Implement log sampling and retention policies)
- **Query Limitations**: CloudWatch Logs Insights has query complexity limits (Mitigation: Use Athena for complex analysis)

### Technical Debt

**Debt Introduced**:

- **Log Format Migration**: Future migration to different platform requires log format conversion
- **Query Optimization**: Athena queries may need optimization as data volume grows

**Debt Repayment Plan**:

- **Log Format**: Use industry-standard JSON format to minimize migration effort
- **Query Optimization**: Quarterly review of Athena query performance and partitioning strategy

### Long-term Implications

This decision establishes CloudWatch Logs as our standard log aggregation platform for the next 3-5 years. As log volume grows, we may need to:

- Implement more aggressive log sampling for debug logs
- Optimize S3 storage with Intelligent-Tiering
- Consider hybrid approach with Loki for specific high-volume services
- Evaluate AWS OpenSearch Service if advanced search becomes critical

The structured logging format and correlation IDs provide foundation for future observability enhancements including distributed tracing and service mesh integration.

## Related Decisions

### Related ADRs


### Affected Viewpoints

- [Operational Viewpoint](../../viewpoints/operational/README.md) - Defines operational procedures using centralized logs
- [Deployment Viewpoint](../../viewpoints/deployment/README.md) - Log forwarding configuration in deployment

### Affected Perspectives

- [Availability Perspective](../../perspectives/availability/README.md) - Log-based monitoring improves availability
- [Performance Perspective](../../perspectives/performance/README.md) - Log analysis identifies performance issues
- [Security Perspective](../../perspectives/security/README.md) - Security incident investigation using logs

## Notes

### Assumptions

- Log volume will grow to 2TB/day within 2 years
- Structured JSON logging can be adopted across all services
- CloudWatch Logs service limits sufficient for our scale
- S3 Glacier acceptable for long-term archival access patterns

### Constraints

- Must comply with 7-year retention requirement
- Must support data residency requirements (Taiwan, Japan)
- Must integrate with existing Grafana dashboards
- Must not impact service performance

### Open Questions

- Should we implement log sampling for debug logs?
- What is the optimal CloudWatch Logs retention period (30 vs 90 days)?
- Should we use CloudWatch Logs Insights or Athena for primary analysis?

### Follow-up Actions

- [ ] Create structured logging library for Java services - Development Team
- [ ] Create structured logging library for TypeScript services - Development Team
- [ ] Document log query examples and runbooks - Operations Team
- [ ] Set up cost monitoring and alerting - FinOps Team
- [ ] Conduct training on CloudWatch Logs Insights - Operations Team

### References

- [AWS CloudWatch Logs Documentation](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/)
- [AWS CloudWatch Logs Insights Query Syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html)
- [AWS Athena Best Practices](https://docs.aws.amazon.com/athena/latest/ug/performance-tuning.html)
- [Fluent Bit for Kubernetes](https://docs.fluentbit.io/manual/installation/kubernetes)
- [Structured Logging Best Practices](https://www.loggly.com/ultimate-guide/json-logging-best-practices/)

---

**ADR Template Version**: 1.0  
**Last Template Update**: 2025-01-17
