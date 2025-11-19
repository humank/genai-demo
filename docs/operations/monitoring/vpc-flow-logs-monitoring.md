# VPC Flow Logs Network Insights Monitoring

> **Last Updated**: 2025-10-22  
> **Owner**: DevOps & Security Teams  
> **Status**: Active

## Overview

VPC Flow Logs provide comprehensive network traffic visibility and security insights for the GenAI Demo application infrastructure.

## Requirements Addressed

- **Requirement 13.16**: Record all VPC traffic details
- **Requirement 13.17**: Detect anomalous traffic patterns  
- **Requirement 13.18**: Provide network evidence for security events

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                         VPC Traffic                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      VPC Flow Logs                           │
│  - Traffic Type: ALL (accepted + rejected)                   │
│  - Destination: CloudWatch Logs                              │
│  - Format: Custom detailed format                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                ┌──────────┴──────────┐
                │                     │
                ▼                     ▼
┌──────────────────────┐  ┌──────────────────────┐
│  CloudWatch Logs     │  │   S3 Archival        │
│  - Real-time query   │  │   - 7-year retention │
│  - 30-day retention  │  │   - Glacier storage  │
└──────────┬───────────┘  └──────────────────────┘
           │
           ├─────────────────────┬─────────────────────┐
           │                     │                     │
           ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Anomaly Detection│  │Security Analysis │  │ CloudWatch       │
│ Lambda (15 min)  │  │Lambda (10 min)   │  │ Insights Queries │
└────────┬─────────┘  └────────┬─────────┘  └──────────────────┘
         │                     │
         ▼                     ▼
┌──────────────────┐  ┌──────────────────┐
│ Warning Alerts   │  │ Critical Alerts  │
│ (SNS)            │  │ (SNS)            │
└──────────────────┘  └──────────────────┘
```

## Features

### 1. Comprehensive Traffic Logging

**What is Logged**:
- Source and destination IP addresses
- Source and destination ports
- Protocol (TCP, UDP, ICMP)
- Packet and byte counts
- Action (ACCEPT or REJECT)
- Flow direction (ingress or egress)
- Start and end timestamps

**Log Format**:
```
${version} ${account-id} ${interface-id} ${srcaddr} ${dstaddr} ${srcport} ${dstport} ${protocol} ${packets} ${bytes} ${start} ${end} ${action} ${log-status}
```

**Example Log Entry**:
```
2 123456789012 eni-1a2b3c4d 10.0.1.5 10.0.2.10 49152 80 6 20 4000 1620000000 1620000060 ACCEPT OK
```

### 2. Anomaly Detection

**Detection Capabilities**:

#### High Traffic Volume
- **Threshold**: > 1GB in 1 hour
- **Analysis**: Sum of bytes by source IP
- **Alert Level**: WARNING
- **Use Cases**:
  - Data exfiltration detection
  - DDoS attack identification
  - Unusual backup/migration activity

#### High Connection Count
- **Threshold**: > 1000 connections in 1 hour
- **Analysis**: Count of connections by source IP
- **Alert Level**: WARNING
- **Use Cases**:
  - Bot activity detection
  - Connection flooding
  - Distributed attacks

**Analysis Schedule**: Every 15 minutes

**CloudWatch Insights Query**:
```sql
fields @timestamp, srcaddr, dstaddr, srcport, dstport, protocol, bytes, packets
| stats sum(bytes) as total_bytes, sum(packets) as total_packets, count() as connection_count by srcaddr
| sort total_bytes desc
| limit 20
```

### 3. Security Analysis

**Threat Detection**:

#### Port Scanning
- **Threshold**: > 50 different ports accessed
- **Analysis**: Count of unique destination ports by source IP
- **Alert Level**: CRITICAL
- **Indicators**:
  - Sequential port access
  - High rejection rate
  - Short time window

#### Suspicious Port Access
- **Monitored Ports**:
  - 22 (SSH)
  - 23 (Telnet)
  - 3389 (RDP)
  - 445 (SMB)
  - 135, 139 (Windows RPC)
- **Threshold**: > 10 rejected attempts
- **Alert Level**: CRITICAL
- **Use Cases**:
  - Brute force detection
  - Unauthorized access attempts
  - Malware activity

**Analysis Schedule**: Every 10 minutes

**CloudWatch Insights Query**:
```sql
fields @timestamp, srcaddr, dstaddr, srcport, dstport, protocol, action
| filter action = "REJECT"
| stats count() as reject_count by srcaddr, dstport
| sort reject_count desc
| limit 50
```

### 4. Long-term Archival

**S3 Lifecycle**:
- **0-90 days**: Standard storage
- **90-365 days**: Glacier storage
- **365-2555 days**: Deep Archive storage
- **After 2555 days (7 years)**: Automatic deletion

**Compliance**: Meets regulatory requirements for 7-year log retention

**Storage Optimization**:
- Automatic compression
- Lifecycle transitions
- Cost-effective long-term storage

## Dashboards

### Main Dashboard: VPCFlowLogs-{environment}

**Widgets**:

1. **Traffic Volume (Bytes)**
   - Bytes In/Out over time
   - 5-minute granularity
   - Identifies traffic spikes

2. **Connection Count**
   - Packets In/Out over time
   - 5-minute granularity
   - Monitors connection patterns

3. **Anomalies Detected**
   - High traffic volume count
   - High connection count
   - 15-minute granularity

4. **Security Events**
   - Port scanning incidents
   - Suspicious port access
   - 10-minute granularity

5. **Top Traffic Sources**
   - CloudWatch Logs Insights widget
   - Last hour analysis
   - Top 10 sources by bytes

### Integration with Main Dashboard

VPC Flow Logs metrics are also integrated into the main GenAI Demo monitoring dashboard with:
- Overview widget with key features
- Link to dedicated VPC Flow Logs dashboard
- Log group and archival bucket information

## Alerts

### Warning-Level Alerts

**High Traffic Volume**:
- **Condition**: > 5 anomalies in 30 minutes
- **Evaluation**: 2 periods of 15 minutes
- **Action**: SNS notification to warning topic
- **Response**: Investigate traffic source and pattern

**High Connection Count**:
- **Condition**: > 5 anomalies in 30 minutes
- **Evaluation**: 2 periods of 15 minutes
- **Action**: SNS notification to warning topic
- **Response**: Check for bot activity or connection flooding

### Critical-Level Alerts

**Port Scanning Detected**:
- **Condition**: ≥ 1 incident detected
- **Evaluation**: 1 period of 10 minutes
- **Action**: SNS notification to critical topic
- **Response**: Block source IP, investigate intent

**Suspicious Port Access**:
- **Condition**: ≥ 1 incident detected
- **Evaluation**: 1 period of 10 minutes
- **Action**: SNS notification to critical topic
- **Response**: Review security groups, block if malicious

## CloudWatch Insights Queries

### Pre-defined Queries

#### 1. Top Talkers
```sql
fields @timestamp, srcaddr, dstaddr, bytes, packets
| stats sum(bytes) as total_bytes, sum(packets) as total_packets by srcaddr
| sort total_bytes desc
| limit 20
```

**Purpose**: Identify highest traffic sources

#### 2. Rejected Connections
```sql
fields @timestamp, srcaddr, dstaddr, dstport, protocol
| filter action = "REJECT"
| stats count() as reject_count by srcaddr, dstport
| sort reject_count desc
| limit 50
```

**Purpose**: Analyze blocked traffic patterns

#### 3. Port Scanning Detection
```sql
fields @timestamp, srcaddr, dstport
| stats count_distinct(dstport) as unique_ports by srcaddr
| filter action = "REJECT"
| sort unique_ports desc
| limit 20
```

**Purpose**: Identify potential port scanning activity

### Custom Query Examples

#### Traffic by Protocol
```sql
fields @timestamp, protocol, bytes
| stats sum(bytes) as total_bytes by protocol
| sort total_bytes desc
```

#### Top Destinations
```sql
fields @timestamp, dstaddr, bytes
| stats sum(bytes) as total_bytes by dstaddr
| sort total_bytes desc
| limit 20
```

#### Traffic Timeline
```sql
fields @timestamp, bytes
| stats sum(bytes) as total_bytes by bin(5m)
| sort @timestamp asc
```

## Operational Procedures

### Daily Operations

1. **Morning Check** (9:00 AM):
   - Review overnight alerts
   - Check dashboard for anomalies
   - Verify analysis functions executed successfully

2. **Afternoon Review** (2:00 PM):
   - Review traffic patterns
   - Investigate any warnings
   - Update security rules if needed

3. **Evening Summary** (6:00 PM):
   - Generate daily traffic report
   - Document any incidents
   - Plan follow-up actions

### Weekly Tasks

1. **Monday**: Review previous week's alerts and trends
2. **Wednesday**: Analyze top talkers and verify legitimacy
3. **Friday**: Security audit of rejected connections

### Monthly Tasks

1. **First Week**: Review and tune detection thresholds
2. **Second Week**: Audit S3 archival and lifecycle
3. **Third Week**: Security team review of incidents
4. **Fourth Week**: Cost optimization review

## Performance Metrics

### Target Metrics

- **Flow Log Delivery Latency**: < 5 minutes
- **Analysis Function Success Rate**: > 99%
- **Alert Delivery Time**: < 2 minutes
- **Query Response Time**: < 30 seconds
- **S3 Archival Success Rate**: 100%

### Monitoring Metrics

```bash
# Flow log delivery rate
aws cloudwatch get-metric-statistics \
  --namespace AWS/Logs \
  --metric-name IncomingLogEvents \
  --dimensions Name=LogGroupName,Value=/aws/vpc/flowlogs/production \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --period 300 \
  --statistics Sum

# Lambda function performance
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=AnomalyDetection \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --period 300 \
  --statistics Average,Maximum,Minimum
```

## Cost Optimization

### Cost Breakdown

1. **CloudWatch Logs**:
   - Ingestion: $0.50 per GB
   - Storage: $0.03 per GB/month
   - Insights queries: $0.005 per GB scanned

2. **S3 Storage**:
   - Standard: $0.023 per GB/month
   - Glacier: $0.004 per GB/month
   - Deep Archive: $0.00099 per GB/month

3. **Lambda Execution**:
   - Anomaly Detection: ~$5/month
   - Security Analysis: ~$5/month

### Optimization Strategies

1. **Reduce Log Volume**:
   - Filter unnecessary traffic
   - Sample non-critical flows
   - Aggregate similar connections

2. **Optimize Retention**:
   - Reduce CloudWatch retention to 7 days
   - Rely on S3 for long-term storage
   - Implement intelligent archival

3. **Query Optimization**:
   - Use time-based filters
   - Limit result sets
   - Cache frequent queries

## Security Best Practices

### Access Control

1. **IAM Policies**:
   - Restrict log group access
   - Limit S3 bucket permissions
   - Use least privilege principle

2. **Encryption**:
   - CloudWatch Logs: AWS-managed keys
   - S3: SSE-S3 or SSE-KMS
   - In-transit: TLS 1.2+

3. **Audit Trail**:
   - Enable CloudTrail for API calls
   - Monitor access patterns
   - Alert on unauthorized access

### Incident Response

1. **Detection**: Automated alerts via Lambda functions
2. **Analysis**: CloudWatch Insights queries
3. **Containment**: Security group updates, IP blocking
4. **Eradication**: Remove threat, patch vulnerabilities
5. **Recovery**: Restore normal operations
6. **Lessons Learned**: Document and improve

## Troubleshooting

### Common Issues

1. **No Logs Appearing**:
   - Check VPC Flow Log status
   - Verify IAM role permissions
   - Review CloudWatch Logs service limits

2. **High Costs**:
   - Review log volume
   - Check lifecycle transitions
   - Optimize query patterns

3. **False Positives**:
   - Tune detection thresholds
   - Whitelist legitimate sources
   - Implement time-based rules

4. **Analysis Function Failures**:
   - Check Lambda timeout
   - Review error logs
   - Verify SNS permissions

## References

- [AWS VPC Flow Logs Documentation](https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html)
- [CloudWatch Logs Insights Query Syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html)
- [S3 Lifecycle Configuration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lifecycle-mgmt.html)

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-22 | 1.0 | Initial monitoring guide | Kiro AI |

