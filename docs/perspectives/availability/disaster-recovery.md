# Disaster Recovery

> **Last Updated**: 2025-10-24
> **Status**: Active
> **Owner**: Operations & Infrastructure Team

## Overview

This document defines the disaster recovery (DR) strategy for the Enterprise E-Commerce Platform. Our DR plan ensures business continuity in the event of catastrophic failures, including complete region outages, data center disasters, or major security incidents.

## Disaster Recovery Objectives

### Recovery Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| **RTO (Recovery Time Objective)** | 30 minutes | Maximum acceptable downtime for complete region failure |
| **RPO (Recovery Point Objective)** | 5 minutes | Maximum acceptable data loss |
| **Data Durability** | 99.999999999% (11 nines) | S3 cross-region replication |
| **DR Test Frequency** | Bi-annual | Validate DR procedures and team readiness |

### Disaster Scenarios

| Scenario | Probability | Impact | RTO | RPO |
|----------|-------------|--------|-----|-----|
| **Complete Region Failure** | Very Low | Critical | 30 min | 5 min |
| **Data Center Disaster** | Very Low | Critical | 30 min | 5 min |
| **Ransomware Attack** | Low | Critical | 2 hours | 1 hour |
| **Data Corruption** | Low | High | 1 hour | 15 min |
| **Major Security Breach** | Low | High | 4 hours | 1 hour |

---

## DR Strategy

### Multi-Region Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                         Route 53 (Global DNS)                   │
│                    Health Check + Failover Routing              │
└────────────────────────┬────────────────────────────────────────┘
                         │
            ┌────────────┴────────────┐
            │                         │
┌───────────▼──────────┐    ┌────────▼──────────────┐
│   Primary Region     │    │    DR Region          │
│   (us-east-1)        │    │    (us-west-2)        │
│   ACTIVE             │    │    STANDBY            │
├──────────────────────┤    ├───────────────────────┤
│                      │    │                       │
│  ┌────────────────┐  │    │  ┌────────────────┐  │
│  │  EKS Cluster   │  │    │  │  EKS Cluster   │  │
│  │  (Active)      │  │    │  │  (Warm Standby)│  │
│  │  9 nodes       │  │    │  │  3 nodes       │  │
│  └────────────────┘  │    │  └────────────────┘  │
│                      │    │                       │
│  ┌────────────────┐  │    │  ┌────────────────┐  │
│  │  RDS Primary   │──┼────┼─►│  RDS Replica   │  │
│  │  Multi-AZ      │  │    │  │  (Read Replica)│  │
│  └────────────────┘  │    │  └────────────────┘  │
│                      │    │                       │
│  ┌────────────────┐  │    │  ┌────────────────┐  │
│  │  S3 Bucket     │──┼────┼─►│  S3 Bucket     │  │
│  │  (Primary)     │  │    │  │  (Replica)     │  │
│  └────────────────┘  │    │  └────────────────┘  │
│                      │    │                       │
│  ┌────────────────┐  │    │  ┌────────────────┐  │
│  │  ElastiCache   │  │    │  │  ElastiCache   │  │
│  │  (Active)      │  │    │  │  (Standby)     │  │
│  └────────────────┘  │    │  └────────────────┘  │
│                      │    │                       │
└──────────────────────┘    └───────────────────────┘
```

### DR Modes

#### Warm Standby (Current)

- **Description**: DR region runs with minimal capacity
- **Cost**: Medium (30% of primary region cost)
- **RTO**: 30 minutes
- **RPO**: 5 minutes
- **Use Case**: Balance between cost and recovery speed

#### Hot Standby (Future)

- **Description**: DR region runs at full capacity
- **Cost**: High (100% of primary region cost)
- **RTO**: 5 minutes
- **RPO**: 1 minute
- **Use Case**: Mission-critical systems requiring minimal downtime

---

## Backup Strategy

### Database Backups

#### Automated Backups

```yaml
RDS Automated Backups:
  BackupRetentionPeriod: 7 days
  PreferredBackupWindow: "03:00-04:00 UTC"
  BackupType: Automated snapshot

  Features:

    - Point-in-time recovery (PITR)
    - Transaction log backups every 5 minutes
    - Automatic backup to S3
    - Cross-region backup copy enabled

```

#### Manual Snapshots

```bash
# Create manual snapshot before major changes
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-db \
  --db-snapshot-identifier ecommerce-db-pre-migration-2025-10-24

# Copy snapshot to DR region
aws rds copy-db-snapshot \
  --source-db-snapshot-identifier arn:aws:rds:us-east-1:123456789012:snapshot:ecommerce-db-pre-migration-2025-10-24 \
  --target-db-snapshot-identifier ecommerce-db-pre-migration-2025-10-24 \
  --source-region us-east-1 \
  --region us-west-2
```

#### Backup Schedule

| Backup Type | Frequency | Retention | Storage Location |
|-------------|-----------|-----------|------------------|
| **Automated Snapshot** | Daily | 7 days | Primary + DR region |
| **Transaction Logs** | Every 5 minutes | 7 days | Primary + DR region |
| **Manual Snapshot** | Before major changes | 30 days | Primary + DR region |
| **Monthly Archive** | Monthly | 1 year | S3 Glacier |

### Application Data Backups

#### S3 Cross-Region Replication

```yaml
S3 Replication Configuration:
  SourceBucket: ecommerce-data-us-east-1
  DestinationBucket: ecommerce-data-us-west-2

  ReplicationRule:
    Status: Enabled
    Priority: 1

    Filter:
      Prefix: ""  # Replicate all objects

    Destination:
      Bucket: arn:aws:s3:::ecommerce-data-us-west-2
      ReplicationTime:
        Status: Enabled
        Time:
          Minutes: 15  # Replicate within 15 minutes

      Metrics:
        Status: Enabled
        EventThreshold:
          Minutes: 15

    DeleteMarkerReplication:
      Status: Enabled
```

### Configuration Backups

```bash
# Backup Kubernetes configurations
kubectl get all --all-namespaces -o yaml > k8s-backup-$(date +%Y%m%d).yaml

# Backup AWS CDK state
aws s3 sync ./cdk.out s3://ecommerce-cdk-backup/$(date +%Y%m%d)/

# Backup application configurations
aws secretsmanager get-secret-value --secret-id ecommerce/prod/config \
  --query SecretString --output text > config-backup-$(date +%Y%m%d).json
```

---

## Disaster Recovery Procedures

### DR Activation Checklist

#### Phase 1: Assessment (0-5 minutes)

- [ ] **Confirm Disaster**: Verify primary region is truly unavailable
- [ ] **Assess Impact**: Determine scope of failure
- [ ] **Notify Stakeholders**: Alert leadership and teams
- [ ] **Activate DR Team**: Assemble incident response team
- [ ] **Document Start Time**: Record DR activation timestamp

#### Phase 2: DNS Failover (5-10 minutes)

```bash
# Update Route 53 health check to fail primary region
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file://failover-to-dr.json

# Verify DNS propagation
dig ecommerce.example.com
nslookup ecommerce.example.com
```

```json
{
  "Changes": [{
    "Action": "UPSERT",
    "ResourceRecordSet": {
      "Name": "ecommerce.example.com",
      "Type": "A",
      "SetIdentifier": "Primary",
      "Failover": "PRIMARY",
      "HealthCheckId": "primary-health-check",
      "AliasTarget": {
        "HostedZoneId": "Z1234567890ABC",
        "DNSName": "primary-alb.us-east-1.elb.amazonaws.com",
        "EvaluateTargetHealth": true
      }
    }
  }]
}
```

#### Phase 3: Scale DR Infrastructure (10-20 minutes)

```bash
# Scale EKS node group in DR region
aws eks update-nodegroup-config \
  --cluster-name ecommerce-cluster-dr \
  --nodegroup-name application-nodes \
  --scaling-config minSize=9,maxSize=30,desiredSize=9

# Scale application deployments
kubectl scale deployment order-service --replicas=9 -n production
kubectl scale deployment customer-service --replicas=9 -n production
kubectl scale deployment product-service --replicas=9 -n production

# Verify pod readiness
kubectl get pods -n production -w
```

#### Phase 4: Promote Database (20-25 minutes)

```bash
# Promote read replica to standalone instance
aws rds promote-read-replica \
  --db-instance-identifier ecommerce-db-dr

# Wait for promotion to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-dr

# Update application configuration
kubectl set env deployment/order-service \
  DATABASE_URL=jdbc:postgresql://ecommerce-db-dr.us-west-2.rds.amazonaws.com:5432/ecommerce
```

#### Phase 5: Verify Services (25-30 minutes)

```bash
# Run health checks
curl https://ecommerce.example.com/actuator/health

# Verify critical flows
./scripts/smoke-test.sh

# Check monitoring dashboards
# - Application metrics
# - Database connections
# - Cache hit rates
# - Error rates
```

### DR Activation Script

```bash
#!/bin/bash
# dr-activate.sh - Automated DR activation script

set -e

echo "=== DR Activation Started at $(date) ==="

# Phase 1: Validation
echo "Phase 1: Validating DR readiness..."
./scripts/validate-dr-region.sh

# Phase 2: DNS Failover
echo "Phase 2: Initiating DNS failover..."
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://failover-to-dr.json

# Phase 3: Scale Infrastructure
echo "Phase 3: Scaling DR infrastructure..."
./scripts/scale-dr-infrastructure.sh

# Phase 4: Promote Database
echo "Phase 4: Promoting database replica..."
./scripts/promote-database.sh

# Phase 5: Update Configurations
echo "Phase 5: Updating application configurations..."
./scripts/update-app-configs.sh

# Phase 6: Verification
echo "Phase 6: Verifying services..."
./scripts/verify-dr-services.sh

echo "=== DR Activation Completed at $(date) ==="
echo "RTO: $(($(date +%s) - $START_TIME)) seconds"
```

---

## Data Recovery Procedures

### Point-in-Time Recovery (PITR)

```bash
# Restore database to specific point in time
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ecommerce-db \
  --target-db-instance-identifier ecommerce-db-restored \
  --restore-time 2025-10-24T10:30:00Z \
  --db-subnet-group-name ecommerce-subnet-group

# Wait for restoration
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-restored

# Verify data integrity
psql -h ecommerce-db-restored.us-east-1.rds.amazonaws.com \
  -U admin -d ecommerce \
  -c "SELECT COUNT(*) FROM orders WHERE created_at > '2025-10-24 10:00:00';"
```

### Snapshot Restoration

```bash
# List available snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-db \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime]' \
  --output table

# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-db-restored \
  --db-snapshot-identifier ecommerce-db-snapshot-2025-10-24

# Verify restoration
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-restored \
  --query 'DBInstances[0].DBInstanceStatus'
```

### S3 Object Recovery

```bash
# List object versions
aws s3api list-object-versions \
  --bucket ecommerce-data-us-east-1 \
  --prefix orders/2025/10/24/

# Restore specific version
aws s3api copy-object \
  --bucket ecommerce-data-us-east-1 \
  --copy-source ecommerce-data-us-east-1/orders/2025/10/24/order-12345.json?versionId=abc123 \
  --key orders/2025/10/24/order-12345.json

# Restore from DR region
aws s3 sync s3://ecommerce-data-us-west-2/orders/ \
  s3://ecommerce-data-us-east-1/orders/ \
  --source-region us-west-2 \
  --region us-east-1
```

---

## Failback Procedures

### Planned Failback

Once primary region is restored and verified:

#### Step 1: Prepare Primary Region (Day 1)

```bash
# Restore infrastructure
./scripts/restore-primary-infrastructure.sh

# Sync data from DR to primary
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-db-dr \
  --db-snapshot-identifier pre-failback-snapshot

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-db-primary-new \
  --db-snapshot-identifier pre-failback-snapshot
```

#### Step 2: Verify Primary Region (Day 2)

```bash
# Run comprehensive tests
./scripts/test-primary-region.sh

# Verify data consistency
./scripts/verify-data-consistency.sh

# Load test
k6 run --vus 500 --duration 1h load-test.js
```

#### Step 3: Gradual Traffic Shift (Day 3-5)

```bash
# Day 3: Route 10% traffic to primary
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://failback-10percent.json

# Day 4: Route 50% traffic to primary
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://failback-50percent.json

# Day 5: Route 100% traffic to primary
aws route53 change-resource-record-sets \
  --hosted-zone-id $HOSTED_ZONE_ID \
  --change-batch file://failback-100percent.json
```

#### Step 4: Scale Down DR Region (Day 6)

```bash
# Scale down DR infrastructure to warm standby
./scripts/scale-down-dr-region.sh

# Verify DR region is ready for next activation
./scripts/validate-dr-region.sh
```

---

## DR Testing

### Test Schedule

| Test Type | Frequency | Duration | Participants |
|-----------|-----------|----------|--------------|
| **Tabletop Exercise** | Quarterly | 2 hours | All teams |
| **Partial DR Test** | Quarterly | 4 hours | Ops + Dev |
| **Full DR Drill** | Bi-annually | 8 hours | All teams |
| **Backup Restoration** | Monthly | 1 hour | Ops team |

### DR Drill Procedure

#### Pre-Drill Preparation (1 week before)

- [ ] Schedule drill with all stakeholders
- [ ] Notify customers of planned test
- [ ] Prepare test scenarios
- [ ] Review DR procedures
- [ ] Verify backup integrity

#### Drill Execution

```bash
# 1. Simulate primary region failure
./scripts/simulate-region-failure.sh

# 2. Execute DR activation
./scripts/dr-activate.sh

# 3. Verify services
./scripts/verify-dr-services.sh

# 4. Test critical user flows
./scripts/test-critical-flows.sh

# 5. Measure RTO/RPO
./scripts/measure-recovery-metrics.sh

# 6. Execute failback
./scripts/failback-to-primary.sh
```

#### Post-Drill Review

- [ ] Document actual RTO/RPO achieved
- [ ] Identify issues and gaps
- [ ] Update DR procedures
- [ ] Create action items
- [ ] Schedule follow-up improvements

### Test Metrics

| Metric | Target | Last Test | Status |
|--------|--------|-----------|--------|
| **RTO** | 30 min | 28 min | ✅ Pass |
| **RPO** | 5 min | 3 min | ✅ Pass |
| **DNS Failover** | 5 min | 4 min | ✅ Pass |
| **Database Promotion** | 10 min | 8 min | ✅ Pass |
| **Service Verification** | 5 min | 6 min | ⚠️ Needs improvement |

---

## Monitoring and Alerting

### DR Readiness Monitoring

```yaml
# CloudWatch Alarms for DR readiness
Alarms:

  - Name: DR-Database-Replication-Lag

    Metric: ReplicaLag
    Threshold: 300 seconds
    Action: Alert DR team

  - Name: DR-S3-Replication-Lag

    Metric: ReplicationLatency
    Threshold: 900 seconds
    Action: Alert DR team

  - Name: DR-Region-Health-Check-Failed

    Metric: HealthCheckStatus
    Threshold: 2 consecutive failures
    Action: Alert DR team
```

### DR Activation Alerts

```yaml
# Alerts during DR activation
Alerts:

  - Event: DR activation initiated

    Recipients: [leadership, ops-team, dev-team]
    Channel: [email, slack, pagerduty]

  - Event: DNS failover completed

    Recipients: [ops-team]
    Channel: [slack]

  - Event: DR services verified

    Recipients: [leadership, ops-team]
    Channel: [email, slack]
```

---

## Roles and Responsibilities

### DR Team Structure

| Role | Responsibilities | Contact |
|------|------------------|---------|
| **Incident Commander** | Overall DR coordination | On-call manager |
| **Infrastructure Lead** | AWS infrastructure management | Ops team lead |
| **Database Lead** | Database failover and recovery | DBA team lead |
| **Application Lead** | Application deployment and config | Dev team lead |
| **Communications Lead** | Stakeholder communication | Product manager |
| **Security Lead** | Security validation | Security team lead |

### Communication Plan

#### Internal Communication

- **Slack Channel**: #incident-dr-activation
- **Conference Bridge**: Zoom DR room
- **Status Page**: Internal status dashboard

#### External Communication

- **Customer Notification**: Email + status page
- **Partner Notification**: API status endpoint
- **Public Status**: status.ecommerce.example.com

---

## Compliance and Documentation

### DR Documentation Requirements

- [ ] DR procedures documented and reviewed quarterly
- [ ] DR test results documented and archived
- [ ] RTO/RPO metrics tracked and reported
- [ ] Lessons learned documented after each test
- [ ] DR plan approved by leadership annually

### Audit Trail

All DR activities must be logged:

- DR activation timestamp
- Actions taken and by whom
- RTO/RPO achieved
- Issues encountered
- Resolution steps

---

## Continuous Improvement

### Post-Incident Review

After each DR activation or test:

1. **Immediate Review** (within 24 hours)
   - What went well?
   - What went wrong?
   - Actual vs. target RTO/RPO

2. **Detailed Analysis** (within 1 week)
   - Root cause analysis
   - Process improvements
   - Tool enhancements

3. **Action Items** (within 2 weeks)
   - Update DR procedures
   - Implement improvements
   - Schedule follow-up training

### DR Metrics Dashboard

Track and visualize:

- DR test frequency and results
- RTO/RPO trends
- Backup success rates
- Replication lag
- DR readiness score

---

**Related Documents**:

- [Overview](overview.md) - Availability perspective introduction
- [Requirements](requirements.md) - Recovery objectives and scenarios
- [High Availability Design](high-availability-design.md) - Application resilience patterns
