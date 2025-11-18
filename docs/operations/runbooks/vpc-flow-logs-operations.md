# VPC Flow Logs Operations Runbook

> **Last Updated**: 2025-10-22  
> **Owner**: DevOps Team  
> **Status**: Active

## Overview

This runbook provides operational procedures for managing and troubleshooting VPC Flow Logs network insights monitoring.

## Purpose

VPC Flow Logs provide comprehensive network traffic visibility for:
- **Requirement 13.16**: Recording all VPC traffic details
- **Requirement 13.17**: Detecting anomalous traffic patterns
- **Requirement 13.18**: Providing network evidence for security events

## Architecture

```
VPC Traffic
    ↓
VPC Flow Logs
    ↓
CloudWatch Logs ← Lambda Analysis Functions
    ↓              ↓
S3 Archival    SNS Alerts
```

## Key Components

### 1. VPC Flow Log

- **Resource**: VPC-level flow log
- **Traffic Type**: ALL (accepted and rejected)
- **Destination**: CloudWatch Logs
- **Format**: Custom format with detailed fields
- **Retention**: 30 days in CloudWatch, 7 years in S3

### 2. CloudWatch Log Group

- **Name**: `/aws/vpc/flowlogs/{environment}`
- **Retention**: 30 days
- **Purpose**: Real-time analysis and querying

### 3. Analysis Functions

#### Anomaly Detection Lambda
- **Schedule**: Every 15 minutes
- **Purpose**: Detect unusual traffic patterns
- **Alerts**: Warning-level SNS notifications
- **Detections**:
  - High traffic volume (> 1GB)
  - High connection count (> 1000 connections)

#### Security Analysis Lambda
- **Schedule**: Every 10 minutes
- **Purpose**: Detect security threats
- **Alerts**: Critical-level SNS notifications
- **Detections**:
  - Port scanning (> 50 different ports)
  - Suspicious port access (SSH, RDP, SMB)
  - Brute force attempts

### 4. S3 Archival Bucket

- **Purpose**: Long-term compliance storage
- **Lifecycle**:
  - 90 days: Transition to Glacier
  - 365 days: Transition to Deep Archive
  - 2555 days (7 years): Expiration
- **Encryption**: S3-managed (SSE-S3)

## Common Operations

### Viewing Flow Logs

#### Via CloudWatch Logs Insights

```sql
-- Top traffic sources in last hour
fields @timestamp, srcaddr, dstaddr, bytes, packets
| stats sum(bytes) as total_bytes, sum(packets) as total_packets by srcaddr
| sort total_bytes desc
| limit 20
```

```sql
-- Rejected connections
fields @timestamp, srcaddr, dstaddr, dstport, protocol
| filter action = "REJECT"
| stats count() as reject_count by srcaddr, dstport
| sort reject_count desc
| limit 50
```

```sql
-- Port scanning detection
fields @timestamp, srcaddr, dstport
| stats count_distinct(dstport) as unique_ports by srcaddr
| filter action = "REJECT"
| sort unique_ports desc
| limit 20
```

#### Via AWS CLI

```bash
# Get recent flow logs
aws logs tail /aws/vpc/flowlogs/production --follow

# Query for specific IP
aws logs filter-log-events \
  --log-group-name /aws/vpc/flowlogs/production \
  --filter-pattern "10.0.1.100" \
  --start-time $(date -u -d '1 hour ago' +%s)000
```

### Investigating Anomalies

#### High Traffic Volume Alert

1. **Identify Source**:
   ```sql
   fields @timestamp, srcaddr, dstaddr, bytes
   | stats sum(bytes) as total_bytes by srcaddr
   | sort total_bytes desc
   | limit 10
   ```

2. **Analyze Traffic Pattern**:
   ```sql
   fields @timestamp, srcaddr, dstaddr, dstport, protocol
   | filter srcaddr = "SUSPICIOUS_IP"
   | stats count() by dstport, protocol
   ```

3. **Actions**:
   - Verify if traffic is legitimate (e.g., data migration, backup)
   - Check if source IP is internal or external
   - Consider rate limiting or blocking if malicious

#### Port Scanning Alert

1. **Identify Scanner**:
   ```sql
   fields @timestamp, srcaddr, dstport
   | filter action = "REJECT"
   | stats count_distinct(dstport) as ports_scanned by srcaddr
   | sort ports_scanned desc
   ```

2. **Analyze Scan Pattern**:
   ```sql
   fields @timestamp, srcaddr, dstaddr, dstport
   | filter srcaddr = "SCANNER_IP"
   | stats count() by dstport
   | sort count desc
   ```

3. **Actions**:
   - Add source IP to WAF block list
   - Update security group rules
   - Report to AWS Abuse if external
   - Document incident for security review

### Managing Archival

#### Check S3 Storage

```bash
# List archived logs
aws s3 ls s3://vpc-flow-logs-archive-production-{account-id}/ --recursive

# Check storage class distribution
aws s3api list-objects-v2 \
  --bucket vpc-flow-logs-archive-production-{account-id} \
  --query 'Contents[].{Key:Key,StorageClass:StorageClass,Size:Size}' \
  --output table
```

#### Restore from Glacier

```bash
# Initiate restore (takes 3-5 hours for Glacier)
aws s3api restore-object \
  --bucket vpc-flow-logs-archive-production-{account-id} \
  --key path/to/log/file \
  --restore-request Days=7,GlacierJobParameters={Tier=Standard}

# Check restore status
aws s3api head-object \
  --bucket vpc-flow-logs-archive-production-{account-id} \
  --key path/to/log/file
```

## Troubleshooting

### Issue: No Flow Logs Appearing

**Symptoms**: CloudWatch Log Group is empty

**Diagnosis**:
1. Check VPC Flow Log status:
   ```bash
   aws ec2 describe-flow-logs --filter "Name=resource-id,Values=vpc-xxxxx"
   ```

2. Verify IAM role permissions:
   ```bash
   aws iam get-role --role-name VpcFlowLogsRole
   ```

**Resolution**:
- Ensure VPC Flow Log is in "ACTIVE" state
- Verify IAM role has `logs:CreateLogStream` and `logs:PutLogEvents` permissions
- Check CloudWatch Logs service limits

### Issue: Analysis Functions Failing

**Symptoms**: No anomaly/security alerts being generated

**Diagnosis**:
1. Check Lambda function errors:
   ```bash
   aws logs tail /aws/lambda/AnomalyDetection --follow
   ```

2. Review CloudWatch metrics:
   ```bash
   aws cloudwatch get-metric-statistics \
     --namespace AWS/Lambda \
     --metric-name Errors \
     --dimensions Name=FunctionName,Value=AnomalyDetection \
     --start-time $(date -u -d '1 hour ago' +%s) \
     --end-time $(date -u +%s) \
     --period 300 \
     --statistics Sum
   ```

**Resolution**:
- Check Lambda function timeout (should be 5 minutes)
- Verify SNS topic permissions
- Ensure CloudWatch Logs Insights query syntax is correct
- Review Lambda execution role permissions

### Issue: High S3 Storage Costs

**Symptoms**: Unexpected S3 costs for archival bucket

**Diagnosis**:
1. Check bucket size:
   ```bash
   aws s3 ls s3://vpc-flow-logs-archive-production-{account-id}/ \
     --recursive --summarize
   ```

2. Review lifecycle transitions:
   ```bash
   aws s3api get-bucket-lifecycle-configuration \
     --bucket vpc-flow-logs-archive-production-{account-id}
   ```

**Resolution**:
- Verify lifecycle rules are transitioning to Glacier (90 days)
- Consider reducing CloudWatch Logs retention
- Evaluate if 7-year retention is required for all logs
- Implement log filtering to reduce volume

### Issue: False Positive Alerts

**Symptoms**: Too many anomaly alerts for legitimate traffic

**Diagnosis**:
1. Review alert patterns:
   ```sql
   fields @timestamp, srcaddr, total_bytes, connection_count
   | filter @message like /HIGH_TRAFFIC_VOLUME|HIGH_CONNECTION_COUNT/
   | stats count() by srcaddr
   ```

2. Identify legitimate high-traffic sources

**Resolution**:
- Adjust thresholds in Lambda functions:
  - `HIGH_TRAFFIC_VOLUME`: Increase from 1GB
  - `HIGH_CONNECTION_COUNT`: Increase from 1000
- Add whitelist for known high-traffic sources
- Implement time-based thresholds (higher during business hours)

## Maintenance Procedures

### Monthly Review

1. **Review Top Talkers**:
   - Identify highest traffic sources
   - Verify all are legitimate
   - Document any new patterns

2. **Security Audit**:
   - Review all security alerts from past month
   - Verify all incidents were addressed
   - Update security group rules if needed

3. **Cost Optimization**:
   - Review S3 storage costs
   - Verify lifecycle transitions are working
   - Consider adjusting retention policies

### Quarterly Tasks

1. **Threshold Tuning**:
   - Review false positive rate
   - Adjust detection thresholds
   - Update Lambda function code if needed

2. **Compliance Verification**:
   - Verify 7-year retention is maintained
   - Ensure all required logs are archived
   - Document any gaps

3. **Disaster Recovery Test**:
   - Test S3 restore process
   - Verify CloudWatch Logs Insights queries
   - Validate alert notifications

## Metrics and SLAs

### Key Metrics

- **Flow Log Delivery Latency**: < 5 minutes
- **Analysis Function Success Rate**: > 99%
- **Alert Delivery Time**: < 2 minutes
- **S3 Archival Success Rate**: 100%

### Monitoring

```bash
# Check flow log delivery
aws cloudwatch get-metric-statistics \
  --namespace AWS/Logs \
  --metric-name IncomingLogEvents \
  --dimensions Name=LogGroupName,Value=/aws/vpc/flowlogs/production \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --period 300 \
  --statistics Sum

# Check Lambda function performance
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=AnomalyDetection \
  --start-time $(date -u -d '1 hour ago' +%s) \
  --end-time $(date -u +%s) \
  --period 300 \
  --statistics Average,Maximum
```

## Escalation

### Level 1: DevOps Team
- Flow log delivery issues
- Analysis function errors
- S3 archival problems

### Level 2: Security Team
- Security threat investigations
- Port scanning incidents
- Suspicious traffic patterns

### Level 3: Network Team
- VPC configuration issues
- Network performance problems
- Cross-region connectivity

## Related Documentation

- [VPC Flow Logs Monitoring Guide](../monitoring/vpc-flow-logs-monitoring.md)
- [Security Incident Response](../../perspectives/security/incident-response.md)
- [Network Architecture](../../viewpoints/infrastructure/network-architecture.md)

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-22 | 1.0 | Initial runbook creation | Kiro AI |

