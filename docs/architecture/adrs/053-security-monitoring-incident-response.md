---
adr_number: 053
title: "Security Monitoring and Incident Response"
date: 2025-10-25
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [008, 043, 052, 054]
affected_viewpoints: ["operational", "deployment"]
affected_perspectives: ["security", "availability"]
---

# ADR-053: Security Monitoring and Incident Response

## Status

**Accepted** - 2025-10-25

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires comprehensive security monitoring and incident response capabilities to:

- Detect security threats in real-time
- Respond to security incidents rapidly
- Maintain audit trails for compliance
- Prevent data breaches and unauthorized access
- Meet regulatory requirements for security monitoring
- Protect against sophisticated cyber attacks

Taiwan's cyber security environment presents unique challenges:
- Frequent DDoS attacks from state-sponsored actors
- Advanced Persistent Threat (APT) campaigns
- High-value e-commerce platform as attractive target
- Need for 24/7 security operations
- Regulatory compliance (Taiwan Cyber Security Management Act)

### Business Context

**Business Drivers**:
- Protect customer data and business operations
- Maintain platform availability and reputation
- Comply with security regulations
- Minimize financial impact of security incidents
- Enable rapid incident response and recovery

**Constraints**:
- Must operate 24/7 with minimal false positives
- Cannot impact system performance
- Must integrate with existing monitoring (ADR-008)
- Budget: $5,000/month for security monitoring tools

### Technical Context

**Current State**:
- Basic CloudWatch monitoring (ADR-008)
- No dedicated security monitoring
- No intrusion detection system
- No security incident response process
- Manual security log analysis

**Requirements**:
- Real-time threat detection
- Automated incident response
- Comprehensive audit logging
- Security event correlation
- Threat intelligence integration
- 24/7 security operations center (SOC)

## Decision Drivers

1. **Detection Speed**: Detect threats in real-time (< 1 minute)
2. **Response Time**: Respond to incidents rapidly (< 15 minutes)
3. **Coverage**: Monitor all attack vectors comprehensively
4. **Accuracy**: Minimize false positives (< 5%)
5. **Compliance**: Meet regulatory audit requirements
6. **Scalability**: Handle 100K+ events per second
7. **Cost**: Optimize operational costs
8. **Integration**: Integrate with existing AWS infrastructure

## Considered Options

### Option 1: AWS Native Security Monitoring (Recommended)

**Description**: Comprehensive security monitoring using AWS native services

**Components**:
- **AWS GuardDuty**: Threat detection for AWS accounts and workloads
- **AWS Security Hub**: Centralized security findings management
- **AWS CloudTrail**: API audit logging
- **VPC Flow Logs**: Network traffic monitoring
- **AWS WAF Logs**: Web application firewall logs
- **Amazon EventBridge**: Event-driven automation
- **AWS Lambda**: Automated incident response
- **Amazon SNS**: Alert notifications

**Pros**:
- ✅ Native AWS integration (no agents required)
- ✅ Comprehensive threat detection (ML-powered)
- ✅ Automated response capabilities
- ✅ Centralized security management
- ✅ Compliance-ready audit trails
- ✅ Scalable and cost-effective
- ✅ Continuous threat intelligence updates
- ✅ Low operational overhead

**Cons**:
- ⚠️ Limited customization compared to SIEM
- ⚠️ AWS-specific (not multi-cloud)
- ⚠️ Additional cost for GuardDuty and Security Hub

**Cost**: $3,000/month (GuardDuty $1,500, Security Hub $500, storage $1,000)

**Risk**: **Low** - Proven AWS services

### Option 2: Third-Party SIEM (Splunk, ELK Stack)

**Description**: Deploy dedicated Security Information and Event Management system

**Pros**:
- ✅ Advanced correlation and analytics
- ✅ Customizable detection rules
- ✅ Multi-cloud support
- ✅ Rich visualization and reporting
- ✅ Extensive integration ecosystem

**Cons**:
- ❌ High cost ($10,000-20,000/month)
- ❌ Complex deployment and maintenance
- ❌ Requires dedicated security team
- ❌ Performance overhead
- ❌ Steep learning curve

**Cost**: $15,000/month (Splunk Enterprise Security)

**Risk**: **Medium** - High operational complexity

### Option 3: Managed Security Service Provider (MSSP)

**Description**: Outsource security monitoring to third-party SOC

**Pros**:
- ✅ 24/7 professional monitoring
- ✅ Expert incident response
- ✅ No internal SOC required
- ✅ Compliance support

**Cons**:
- ❌ Very high cost ($20,000-50,000/month)
- ❌ Less control over security operations
- ❌ Data privacy concerns
- ❌ Vendor dependency
- ❌ Communication overhead

**Cost**: $30,000/month

**Risk**: **Medium** - Vendor dependency

## Decision Outcome

**Chosen Option**: **AWS Native Security Monitoring (Option 1)**

### Rationale

AWS native security monitoring was selected for the following reasons:

1. **Cost-Effective**: $3,000/month vs $15,000+ for alternatives
2. **Native Integration**: Seamless integration with AWS infrastructure
3. **Automated Detection**: ML-powered threat detection with continuous updates
4. **Scalability**: Handles platform growth without additional infrastructure
5. **Low Overhead**: No agents or additional infrastructure required
6. **Compliance-Ready**: Built-in compliance reporting and audit trails
7. **Rapid Deployment**: Can be enabled in hours, not weeks
8. **Automated Response**: Event-driven automation with Lambda

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Monitoring                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  GuardDuty   │  │ Security Hub │  │  CloudTrail  │     │
│  │ Threat       │  │ Centralized  │  │ API Audit    │     │
│  │ Detection    │  │ Findings     │  │ Logging      │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│                    ┌───────▼────────┐                       │
│                    │  EventBridge   │                       │
│                    │  Event Router  │                       │
│                    └───────┬────────┘                       │
│                            │                                 │
│         ┌──────────────────┼──────────────────┐             │
│         │                  │                  │             │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐    │
│  │   Lambda     │  │     SNS      │  │   Lambda     │    │
│  │ Auto-Block   │  │   Alerts     │  │  Forensics   │    │
│  │   Malicious  │  │ Notification │  │  Collection  │    │
│  │     IPs      │  │              │  │              │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### AWS GuardDuty Configuration

**Threat Detection Categories**:
- **Reconnaissance**: Port scanning, unusual API calls
- **Instance Compromise**: Malware, cryptocurrency mining, backdoor communication
- **Account Compromise**: Credential theft, unusual API activity
- **Bucket Compromise**: S3 data exfiltration, policy changes
- **Kubernetes Threats**: Container compromise, privilege escalation

**Detection Mechanisms**:
- Machine learning models
- Threat intelligence feeds
- Anomaly detection
- Behavioral analysis

**Implementation**:
```python
# Enable GuardDuty via CDK
guardduty = aws_guardduty.CfnDetector(
    self, "GuardDuty",
    enable=True,
    finding_publishing_frequency="FIFTEEN_MINUTES",
    data_sources=aws_guardduty.CfnDetector.CFNDataSourceConfigurationsProperty(
        s3_logs=aws_guardduty.CfnDetector.CFNS3LogsConfigurationProperty(enable=True),
        kubernetes=aws_guardduty.CfnDetector.CFNKubernetesConfigurationProperty(
            audit_logs=aws_guardduty.CfnDetector.CFNKubernetesAuditLogsConfigurationProperty(enable=True)
        )
    )
)
```

### AWS Security Hub Configuration

**Security Standards**:
- AWS Foundational Security Best Practices
- CIS AWS Foundations Benchmark
- PCI DSS
- NIST Cybersecurity Framework

**Integration**:
- GuardDuty findings
- AWS Config compliance checks
- IAM Access Analyzer findings
- Macie sensitive data findings
- Inspector vulnerability findings

**Implementation**:
```python
# Enable Security Hub via CDK
security_hub = aws_securityhub.CfnHub(
    self, "SecurityHub",
    enable_default_standards=True,
    control_finding_generator="SECURITY_CONTROL"
)

# Enable standards
aws_securityhub.CfnStandard(
    self, "CISStandard",
    standards_arn="arn:aws:securityhub:region::standards/cis-aws-foundations-benchmark/v/1.4.0"
)
```

### Intrusion Detection System (IDS)

**Approach**: Network-based IDS using Suricata on EC2

**Deployment**:
- Suricata instances in each VPC
- Mirror VPC traffic to IDS instances
- Analyze traffic for malicious patterns
- Alert on suspicious activity

**Detection Rules**:
- OWASP Top 10 attack patterns
- Known malware signatures
- Anomalous traffic patterns
- Data exfiltration attempts
- Command and control communication

**Implementation**:
```yaml
# Suricata configuration
vars:
  address-groups:
    HOME_NET: "[10.0.0.0/8]"
    EXTERNAL_NET: "!$HOME_NET"

rule-files:
  - suricata.rules
  - emerging-threats.rules
  - custom-rules.rules

outputs:
  - eve-log:
      enabled: yes
      filetype: regular
      filename: eve.json
      types:
        - alert
        - http
        - dns
        - tls
```

### Automated Incident Response

**Response Actions**:
1. **Auto-Block Malicious IPs**: Update WAF and Security Groups
2. **Isolate Compromised Instances**: Remove from load balancer, restrict network
3. **Collect Forensics**: Snapshot volumes, capture memory, save logs
4. **Notify Security Team**: Send alerts via SNS, PagerDuty
5. **Create Incident Ticket**: Automatically create Jira ticket

**Implementation**:
```python
# Lambda function for automated response
def lambda_handler(event, context):
    finding = event['detail']['findings'][0]
    severity = finding['Severity']['Label']
    finding_type = finding['Type']
    
    if severity in ['HIGH', 'CRITICAL']:
        # Extract malicious IP
        if 'RemoteIpDetails' in finding['Service']['Action']:
            malicious_ip = finding['Service']['Action']['RemoteIpDetails']['IpAddressV4']
            
            # Block IP in WAF
            waf_client.update_ip_set(
                Name='BlockedIPs',
                Scope='REGIONAL',
                Addresses=[f"{malicious_ip}/32"]
            )
            
            # Block IP in Security Group
            ec2_client.revoke_security_group_ingress(
                GroupId=security_group_id,
                IpPermissions=[{
                    'IpProtocol': '-1',
                    'IpRanges': [{'CidrIp': f"{malicious_ip}/32"}]
                }]
            )
            
            # Send alert
            sns_client.publish(
                TopicArn=alert_topic_arn,
                Subject=f"Security Alert: {finding_type}",
                Message=f"Blocked malicious IP: {malicious_ip}"
            )
            
            # Create incident ticket
            create_incident_ticket(finding)
    
    return {'statusCode': 200}
```

### Security Event Correlation

**SIEM Capabilities**:
- Correlate events across multiple sources
- Detect multi-stage attacks
- Identify attack patterns
- Generate security insights

**Implementation with CloudWatch Insights**:
```sql
-- Detect brute force attacks
fields @timestamp, userIdentity.principalId, sourceIPAddress, errorCode
| filter eventName = "ConsoleLogin" and errorCode = "Failed authentication"
| stats count() as failedAttempts by sourceIPAddress, userIdentity.principalId
| filter failedAttempts > 5
| sort failedAttempts desc

-- Detect data exfiltration
fields @timestamp, sourceIPAddress, bytesOut
| filter bytesOut > 1000000000  -- 1GB
| stats sum(bytesOut) as totalBytes by sourceIPAddress
| filter totalBytes > 10000000000  -- 10GB
| sort totalBytes desc
```

### Threat Intelligence Integration

**Sources**:
- AWS Threat Intelligence
- AlienVault OTX
- Abuse.ch
- Taiwan CERT threat feeds
- Custom threat intelligence

**Integration**:
- Automatically update WAF IP blocklists
- Enrich GuardDuty findings
- Correlate with known threat actors
- Share intelligence with Taiwan CERT

**Implementation**:
```python
# Update threat intelligence feeds
def update_threat_feeds():
    # Fetch threat intelligence
    malicious_ips = fetch_threat_intelligence()
    
    # Update WAF IP set
    waf_client.update_ip_set(
        Name='ThreatIntelligenceIPs',
        Scope='REGIONAL',
        Addresses=[f"{ip}/32" for ip in malicious_ips]
    )
    
    # Update GuardDuty threat list
    guardduty_client.create_threat_intel_set(
        DetectorId=detector_id,
        Name='CustomThreatIntel',
        Format='TXT',
        Location=f"s3://{bucket}/threat-intel.txt",
        Activate=True
    )
```

### 24/7 Security Operations

**SOC Structure**:
- **Tier 1**: Alert triage and initial response (outsourced to Taiwan SOC)
- **Tier 2**: Incident investigation and remediation (internal team)
- **Tier 3**: Advanced threat hunting and forensics (internal team)

**On-Call Rotation**:
- 24/7 on-call coverage
- Primary and secondary on-call engineers
- Escalation to security lead for critical incidents
- PagerDuty integration for alerting

**Incident Classification**:
- **P0 (Critical)**: Active data breach, system compromise
- **P1 (High)**: Attempted breach, high-severity vulnerability
- **P2 (Medium)**: Security policy violation, medium-severity finding
- **P3 (Low)**: Informational finding, low-severity issue

**Response SLAs**:
- P0: 15 minutes response, 1 hour resolution
- P1: 1 hour response, 4 hours resolution
- P2: 4 hours response, 24 hours resolution
- P3: 24 hours response, 1 week resolution

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Security Team | High | New monitoring tools and processes | Training, runbooks, automation |
| Operations Team | Medium | Additional alerts and incidents | Alert tuning, automation |
| Development Team | Low | Security findings to address | Security training, tools |
| End Users | None | Transparent security monitoring | N/A |
| Compliance Team | Positive | Enhanced audit capabilities | Regular compliance reports |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All AWS accounts and resources
- All network traffic
- All API calls
- All security events
- Incident response processes

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| False positive alerts | High | Medium | Alert tuning, ML training, feedback loop |
| Alert fatigue | Medium | Medium | Prioritization, automation, aggregation |
| Missed threats | Low | Critical | Multiple detection layers, threat intelligence |
| Performance impact | Low | Low | Serverless architecture, efficient queries |
| Cost overrun | Medium | Medium | Budget alerts, cost optimization |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Core Monitoring (Week 1-2)

- [ ] Enable AWS GuardDuty in all regions
- [ ] Enable AWS Security Hub
- [ ] Configure CloudTrail logging
- [ ] Enable VPC Flow Logs
- [ ] Set up S3 bucket for log storage
- [ ] Configure log retention policies

### Phase 2: Automated Response (Week 3-4)

- [ ] Create EventBridge rules for security events
- [ ] Implement Lambda functions for auto-response
- [ ] Configure SNS topics for alerts
- [ ] Integrate with PagerDuty
- [ ] Create incident response runbooks
- [ ] Test automated response workflows

### Phase 3: IDS Deployment (Week 5-6)

- [ ] Deploy Suricata instances
- [ ] Configure VPC traffic mirroring
- [ ] Set up detection rules
- [ ] Integrate with Security Hub
- [ ] Test detection capabilities
- [ ] Tune false positive rates

### Phase 4: SOC Operations (Week 7-8)

- [ ] Establish on-call rotation
- [ ] Create security dashboards
- [ ] Implement threat intelligence feeds
- [ ] Set up security event correlation
- [ ] Conduct tabletop exercises
- [ ] Document incident response procedures

### Rollback Strategy

**Trigger Conditions**:
- Excessive false positives (> 10% of alerts)
- Performance degradation (> 5% overhead)
- Cost overrun (> 150% of budget)
- Operational issues

**Rollback Steps**:
1. Disable automated response actions
2. Reduce GuardDuty sensitivity
3. Pause IDS deployment
4. Revert to manual monitoring
5. Investigate and fix issues

**Rollback Time**: < 2 hours

## Monitoring and Success Criteria

### Success Metrics

- ✅ Threat detection time: < 1 minute
- ✅ Incident response time: < 15 minutes (P0)
- ✅ False positive rate: < 5%
- ✅ Security event coverage: > 95%
- ✅ Compliance audit pass rate: 100%
- ✅ Mean time to detect (MTTD): < 5 minutes
- ✅ Mean time to respond (MTTR): < 30 minutes

### Monitoring Plan

**CloudWatch Metrics**:
- `security.threats.detected` (count by severity)
- `security.incidents.created` (count by priority)
- `security.response.time` (histogram)
- `security.false.positives` (count)
- `security.compliance.score` (gauge)

**Alerts**:
- Critical security finding detected
- Incident response SLA breach
- False positive rate > 10%
- Compliance score < 90%
- Threat intelligence feed update failure

**Review Schedule**:
- Daily: Review security alerts and incidents
- Weekly: Analyze threat trends and patterns
- Monthly: Compliance and audit review
- Quarterly: Security posture assessment

## Consequences

### Positive Consequences

- ✅ **Real-Time Detection**: Detect threats within minutes
- ✅ **Automated Response**: Rapid incident response with automation
- ✅ **Comprehensive Coverage**: Monitor all attack vectors
- ✅ **Compliance-Ready**: Built-in audit trails and reporting
- ✅ **Cost-Effective**: $3,000/month for enterprise-grade monitoring
- ✅ **Scalable**: Handles platform growth automatically
- ✅ **Low Overhead**: No agents or additional infrastructure

### Negative Consequences

- ⚠️ **Alert Fatigue**: Potential for too many alerts initially
- ⚠️ **False Positives**: ML models require tuning
- ⚠️ **Operational Overhead**: 24/7 SOC operations required
- ⚠️ **Learning Curve**: Team needs training on new tools
- ⚠️ **Cost**: $3,000/month ongoing operational cost

### Technical Debt

**Identified Debt**:
1. Manual threat intelligence integration (acceptable initially)
2. Basic security event correlation (acceptable for MVP)
3. No advanced threat hunting capabilities
4. Limited forensics automation

**Debt Repayment Plan**:
- **Q2 2026**: Implement advanced threat hunting with Athena
- **Q3 2026**: Automate forensics collection and analysis
- **Q4 2026**: Integrate with SOAR platform for orchestration
- **2027**: Implement AI-powered threat detection

## Related Decisions

- [ADR-008: Use CloudWatch + X-Ray + Grafana for Observability](008-use-cloudwatch-xray-grafana-for-observability.md) - Base monitoring infrastructure
- [ADR-043: Observability for Multi-Region Operations](043-observability-multi-region-operations.md) - Multi-region monitoring
- [ADR-052: Authentication Security Hardening](052-authentication-security-hardening.md) - Authentication security
- [ADR-054: Data Loss Prevention (DLP) Strategy](054-data-loss-prevention-strategy.md) - Data protection monitoring

## Notes

### GuardDuty Pricing

- VPC Flow Logs: $1.00 per GB analyzed
- CloudTrail Events: $4.40 per million events
- S3 Data Events: $0.80 per million events
- EKS Audit Logs: $0.40 per GB analyzed

**Estimated Monthly Cost**: $1,500 for 100K users

### Security Hub Pricing

- Security checks: $0.0010 per check
- Finding ingestion: $0.00003 per finding
- Estimated: $500/month

### Incident Response Runbooks

1. **Data Breach Response**
2. **DDoS Attack Response**
3. **Account Compromise Response**
4. **Malware Infection Response**
5. **Data Exfiltration Response**

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-25  
**Next Review**: 2026-01-25 (Quarterly)
