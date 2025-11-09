# Disaster Recovery and High Availability Guide

> **Last Updated**: 2025-11-08  
> **Status**: Active  
> **Owner**: Operations & Database Team

## Overview

This guide provides detailed procedures for disaster recovery and high availability operations, with specific focus on RDS Multi-AZ configuration, read replica management, cross-region replication, and automated failover mechanisms.

For strategic DR planning, see [Disaster Recovery Perspective](../../perspectives/availability/disaster-recovery.md).

## Table of Contents

- [RDS Multi-AZ Configuration](#rds-multi-az-configuration)
- [Read Replica Management](#read-replica-management)
- [Cross-Region Replication](#cross-region-replication)
- [Automated Failover Testing](#automated-failover-testing)
- [Manual Failover Procedures](#manual-failover-procedures)
- [Split-Brain Prevention](#split-brain-prevention)
- [Data Consistency Verification](#data-consistency-verification)
- [HA Monitoring and Alerting](#ha-monitoring-and-alerting)

---

## RDS Multi-AZ Configuration

### Architecture Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer (EKS)                  │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                │
│  │  Pod 1   │  │  Pod 2   │  │  Pod 3   │                │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘                │
│       │             │             │                        │
│       └─────────────┴─────────────┘                        │
│                     │                                       │
└─────────────────────┼───────────────────────────────────────┘
                      │
                      │ Single DNS Endpoint
                      │ (Automatic Failover)
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              RDS Multi-AZ Deployment                        │
│                                                             │
│  ┌──────────────────────────┐    ┌──────────────────────┐ │
│  │   Primary Instance       │    │  Standby Instance    │ │
│  │   (us-east-1a)          │    │  (us-east-1b)        │ │
│  │                          │    │                      │ │
│  │  ┌────────────────────┐  │    │  ┌────────────────┐ │ │
│  │  │  PostgreSQL 15.4   │  │    │  │  PostgreSQL    │ │ │
│  │  │  db.r5.xlarge      │  │    │  │  15.4          │ │ │
│  │  │  4 vCPU, 32 GB RAM │  │    │  │  (Replica)     │ │ │
│  │  └────────────────────┘  │    │  └────────────────┘ │ │
│  │                          │    │                      │ │
│  │  Status: ACTIVE          │◄───┤  Status: STANDBY    │ │
│  │  Read/Write: YES         │    │  Read/Write: NO     │ │
│  └──────────────────────────┘    └──────────────────────┘ │
│              │                            ▲                │
│              │ Synchronous Replication    │                │
│              └────────────────────────────┘                │
│                                                             │
│  Automatic Failover: 60-120 seconds                        │
│  Data Loss: Zero (synchronous replication)                 │
└─────────────────────────────────────────────────────────────┘
```

### Configuration Details


#### Primary Instance Configuration

```yaml
DBInstanceIdentifier: ecommerce-db-primary
DBInstanceClass: db.r5.xlarge
Engine: postgres
EngineVersion: "15.4"
MultiAZ: true
AllocatedStorage: 500  # GB
StorageType: gp3
StorageEncrypted: true
KmsKeyId: arn:aws:kms:REGION:ACCOUNT_ID:key/YOUR_KMS_KEY_ID

AvailabilityZone: us-east-1a  # Primary AZ
SecondaryAvailabilityZone: us-east-1b  # Standby AZ

BackupRetentionPeriod: 7  # days
PreferredBackupWindow: "03:00-04:00"
PreferredMaintenanceWindow: "sun:04:00-sun:05:00"

AutoMinorVersionUpgrade: true
DeletionProtection: true
EnableCloudwatchLogsExports:
  - postgresql
  - upgrade

PerformanceInsightsEnabled: true
PerformanceInsightsRetentionPeriod: 7  # days

Tags:
  Environment: production
  Application: ecommerce
  ManagedBy: terraform
```

#### Synchronous Replication

Multi-AZ deployment uses **synchronous replication**:

- **Write Process**:
  1. Application writes to primary instance
  2. Primary commits to local storage
  3. Primary replicates to standby (synchronous)
  4. Standby acknowledges write
  5. Primary confirms to application
  
- **Characteristics**:
  - Zero data loss (RPO = 0)
  - Slight write latency increase (~1-2ms)
  - Automatic failover capability
  - No read traffic to standby


### Automatic Failover Behavior

#### Failover Triggers

RDS automatically initiates failover when:

1. **Primary Instance Failure**
   - Instance crash or hardware failure
   - Operating system failure
   - Network connectivity loss

2. **Availability Zone Failure**
   - Complete AZ outage
   - Network partition affecting AZ

3. **Storage Failure**
   - EBS volume failure
   - Storage I/O errors

4. **Maintenance Operations**
   - OS patching (if configured)
   - Database engine upgrades
   - Instance class changes

#### Failover Process

```text
Time: T+0s
┌─────────────────────────────────────────────────────────┐
│ Primary Instance (us-east-1a) - FAILURE DETECTED       │
│ Standby Instance (us-east-1b) - HEALTHY                │
└─────────────────────────────────────────────────────────┘

Time: T+10s
┌─────────────────────────────────────────────────────────┐
│ RDS initiates automatic failover                       │
│ DNS CNAME update in progress                           │
└─────────────────────────────────────────────────────────┘

Time: T+60s
┌─────────────────────────────────────────────────────────┐
│ Standby promoted to Primary (us-east-1b)               │
│ DNS CNAME points to new primary                        │
│ New standby being provisioned (us-east-1a)             │
└─────────────────────────────────────────────────────────┘

Time: T+120s
┌─────────────────────────────────────────────────────────┐
│ Failover complete                                       │
│ Applications reconnect automatically                    │
│ New standby synchronized                                │
└─────────────────────────────────────────────────────────┘
```

#### Failover Metrics

| Metric | Target | Typical |
|--------|--------|---------|
| **Detection Time** | < 30 seconds | 10-20 seconds |
| **DNS Propagation** | < 60 seconds | 30-45 seconds |
| **Total Failover Time** | < 120 seconds | 60-90 seconds |
| **Data Loss** | 0 transactions | 0 transactions |


---

## Read Replica Management

### Read Replica Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                    Primary Region (us-east-1)                │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Primary Instance (Multi-AZ)                           │ │
│  │  ecommerce-db-primary                                  │ │
│  │  Read/Write: YES                                       │ │
│  └────────────┬───────────────────────────────────────────┘ │
│               │ Asynchronous Replication                    │
│               │                                              │
│  ┌────────────▼───────────────┐  ┌────────────────────────┐ │
│  │  Read Replica 1            │  │  Read Replica 2        │ │
│  │  (us-east-1c)              │  │  (us-east-1d)          │ │
│  │  ecommerce-db-replica-1    │  │  ecommerce-db-replica-2│ │
│  │  Read Only: YES            │  │  Read Only: YES        │ │
│  │  Lag: < 1 second           │  │  Lag: < 1 second       │ │
│  └────────────────────────────┘  └────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
               │
               │ Cross-Region Replication
               │
┌──────────────▼───────────────────────────────────────────────┐
│                    DR Region (us-west-2)                     │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Cross-Region Read Replica                            │ │
│  │  ecommerce-db-dr-replica                              │ │
│  │  Read Only: YES                                       │ │
│  │  Lag: < 5 seconds                                     │ │
│  │  Can be promoted to standalone                       │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### Creating Read Replicas

#### Same-Region Read Replica

```bash
# Create read replica in same region
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-db-replica-1 \
  --source-db-instance-identifier ecommerce-db-primary \
  --db-instance-class db.r5.large \
  --availability-zone us-east-1c \
  --publicly-accessible false \
  --tags Key=Environment,Value=production \
         Key=Purpose,Value=read-scaling

# Monitor creation progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-replica-1 \
  --query 'DBInstances[0].DBInstanceStatus'

# Wait for available status
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-replica-1
```

#### Cross-Region Read Replica

```bash
# Create cross-region read replica for DR
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-db-dr-replica \
  --source-db-instance-identifier arn:aws:rds:us-east-1:123456789012:db:ecommerce-db-primary \
  --db-instance-class db.r5.xlarge \
  --region us-west-2 \
  --storage-encrypted \
  --kms-key-id arn:aws:kms:REGION:ACCOUNT_ID:key/YOUR_KMS_KEY_ID \
  --tags Key=Environment,Value=production \
         Key=Purpose,Value=disaster-recovery

# Monitor replication lag
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2 \
  --query 'DBInstances[0].StatusInfos'
```


### Read Replica Promotion

#### When to Promote

Promote a read replica to standalone instance when:

1. **Disaster Recovery**: Primary region failure
2. **Region Migration**: Moving to new primary region
3. **Testing**: DR drill or testing procedures
4. **Performance**: Isolating workloads

#### Promotion Process

```bash
# Step 1: Stop application writes to primary (if possible)
kubectl scale deployment order-service --replicas=0 -n production
kubectl scale deployment customer-service --replicas=0 -n production

# Step 2: Verify replication lag is minimal
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2 \
  --query 'DBInstances[0].[DBInstanceIdentifier,StatusInfos]'

# Expected output: ReplicaLag should be < 5 seconds

# Step 3: Promote read replica
aws rds promote-read-replica \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2 \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00"

# Step 4: Monitor promotion progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2 \
  --query 'DBInstances[0].DBInstanceStatus'

# Wait for 'available' status (typically 2-5 minutes)
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2

# Step 5: Enable Multi-AZ on promoted instance
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2 \
  --multi-az \
  --apply-immediately

# Step 6: Update application configuration
kubectl set env deployment/order-service \
  DATABASE_URL="jdbc:postgresql://ecommerce-db-dr-replica.us-west-2.rds.amazonaws.com:5432/ecommerce" \
  -n production

# Step 7: Scale up applications
kubectl scale deployment order-service --replicas=9 -n production
kubectl scale deployment customer-service --replicas=9 -n production

# Step 8: Verify connectivity
kubectl exec -it deployment/order-service -n production -- \
  psql -h ecommerce-db-dr-replica.us-west-2.rds.amazonaws.com \
       -U admin -d ecommerce -c "SELECT version();"
```

#### Post-Promotion Checklist

- [ ] Verify database is read-write
- [ ] Confirm Multi-AZ is enabled
- [ ] Check automated backups are configured
- [ ] Verify application connectivity
- [ ] Monitor replication lag (if new replicas created)
- [ ] Update DNS records if needed
- [ ] Document promotion in incident log
- [ ] Create new read replicas if required


---

## Cross-Region Replication

### Replication Setup

#### Architecture

```text
Primary Region (us-east-1)          DR Region (us-west-2)
┌─────────────────────────┐         ┌─────────────────────────┐
│                         │         │                         │
│  Primary DB (Multi-AZ)  │────────►│  Read Replica           │
│  ecommerce-db-primary   │  Async  │  ecommerce-db-dr-replica│
│                         │  Repl   │                         │
│  Write: YES             │         │  Write: NO              │
│  Read: YES              │         │  Read: YES              │
│                         │         │  Promotable: YES        │
│                         │         │                         │
│  Lag: 0 (Multi-AZ)      │         │  Lag: 1-5 seconds       │
└─────────────────────────┘         └─────────────────────────┘
         │                                     │
         │                                     │
         ▼                                     ▼
┌─────────────────────────┐         ┌─────────────────────────┐
│  Automated Backups      │         │  Automated Backups      │
│  Retention: 7 days      │         │  Retention: 7 days      │
│  PITR: Enabled          │         │  PITR: Enabled          │
└─────────────────────────┘         └─────────────────────────┘
```

#### Configuration Steps

```bash
# Step 1: Ensure primary has automated backups enabled
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-db-primary \
  --backup-retention-period 7 \
  --apply-immediately

# Step 2: Create cross-region read replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-db-dr-replica \
  --source-db-instance-identifier arn:aws:rds:us-east-1:123456789012:db:ecommerce-db-primary \
  --db-instance-class db.r5.xlarge \
  --region us-west-2 \
  --storage-encrypted \
  --kms-key-id arn:aws:kms:REGION:ACCOUNT_ID:key/YOUR_KMS_KEY_ID \
  --publicly-accessible false \
  --db-subnet-group-name ecommerce-subnet-group-dr \
  --vpc-security-group-ids sg-0123456789abcdef0 \
  --monitoring-interval 60 \
  --monitoring-role-arn arn:aws:iam::123456789012:role/rds-monitoring-role \
  --enable-performance-insights \
  --performance-insights-retention-period 7 \
  --tags Key=Environment,Value=production \
         Key=Purpose,Value=disaster-recovery \
         Key=Region,Value=dr

# Step 3: Wait for replica creation (can take 30-60 minutes)
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2

# Step 4: Verify replication status
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-dr-replica \
  --region us-west-2 \
  --query 'DBInstances[0].[DBInstanceIdentifier,DBInstanceStatus,ReadReplicaSourceDBInstanceIdentifier]'
```

### Monitoring Replication Lag

```bash
# Create CloudWatch alarm for replication lag
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-db-dr-replica-lag \
  --alarm-description "Alert when DR replica lag exceeds 60 seconds" \
  --metric-name ReplicaLag \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 60 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-dr-replica \
  --alarm-actions arn:aws:sns:us-west-2:123456789012:ops-alerts \
  --region us-west-2

# Query current replication lag
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-dr-replica \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum \
  --region us-west-2
```


### Replication Lag Management

#### Acceptable Lag Thresholds

| Scenario | Threshold | Action |
|----------|-----------|--------|
| **Normal Operations** | < 5 seconds | Monitor |
| **High Load** | < 30 seconds | Investigate |
| **Warning** | 30-60 seconds | Alert team |
| **Critical** | > 60 seconds | Immediate action |

#### Troubleshooting High Lag

```bash
# 1. Check primary instance load
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name CPUUtilization \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum

# 2. Check network throughput
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name NetworkTransmitThroughput \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum

# 3. Check for long-running transactions on primary
psql -h ecommerce-db-primary.us-east-1.rds.amazonaws.com \
     -U admin -d ecommerce -c "
SELECT pid, usename, application_name, state, 
       now() - xact_start AS duration,
       query
FROM pg_stat_activity
WHERE state != 'idle'
  AND xact_start IS NOT NULL
ORDER BY duration DESC
LIMIT 10;"

# 4. Check replica status
psql -h ecommerce-db-dr-replica.us-west-2.rds.amazonaws.com \
     -U admin -d ecommerce -c "
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;"
```

#### Mitigation Strategies

1. **Reduce Primary Load**
   ```bash
   # Scale read traffic to read replicas
   kubectl set env deployment/product-service \
     DATABASE_READ_URL="jdbc:postgresql://ecommerce-db-replica-1.us-east-1.rds.amazonaws.com:5432/ecommerce"
   ```

2. **Optimize Long Transactions**
   ```sql
   -- Terminate long-running queries if safe
   SELECT pg_terminate_backend(pid)
   FROM pg_stat_activity
   WHERE state != 'idle'
     AND now() - xact_start > interval '5 minutes'
     AND query NOT LIKE '%pg_stat_activity%';
   ```

3. **Increase Replica Instance Size**
   ```bash
   aws rds modify-db-instance \
     --db-instance-identifier ecommerce-db-dr-replica \
     --db-instance-class db.r5.2xlarge \
     --apply-immediately \
     --region us-west-2
   ```


---

## Automated Failover Testing

### Test Framework

#### Monthly Automated Tests

```bash
#!/bin/bash
# automated-failover-test.sh
# Runs monthly automated failover tests

set -e

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
LOG_FILE="/var/log/failover-tests/test-${TIMESTAMP}.log"
METRICS_FILE="/var/log/failover-tests/metrics-${TIMESTAMP}.json"

echo "=== Automated Failover Test Started at $(date) ===" | tee -a $LOG_FILE

# Step 1: Pre-test validation
echo "Step 1: Pre-test validation" | tee -a $LOG_FILE
./scripts/validate-ha-setup.sh | tee -a $LOG_FILE

# Step 2: Record baseline metrics
echo "Step 2: Recording baseline metrics" | tee -a $LOG_FILE
START_TIME=$(date +%s)
./scripts/record-baseline-metrics.sh > $METRICS_FILE

# Step 3: Trigger failover
echo "Step 3: Triggering Multi-AZ failover" | tee -a $LOG_FILE
aws rds reboot-db-instance \
  --db-instance-identifier ecommerce-db-primary \
  --force-failover | tee -a $LOG_FILE

# Step 4: Monitor failover progress
echo "Step 4: Monitoring failover progress" | tee -a $LOG_FILE
./scripts/monitor-failover.sh | tee -a $LOG_FILE

# Step 5: Wait for completion
echo "Step 5: Waiting for failover completion" | tee -a $LOG_FILE
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-primary

END_TIME=$(date +%s)
FAILOVER_DURATION=$((END_TIME - START_TIME))

# Step 6: Verify services
echo "Step 6: Verifying services" | tee -a $LOG_FILE
./scripts/verify-services.sh | tee -a $LOG_FILE

# Step 7: Run smoke tests
echo "Step 7: Running smoke tests" | tee -a $LOG_FILE
./scripts/smoke-tests.sh | tee -a $LOG_FILE

# Step 8: Generate report
echo "Step 8: Generating test report" | tee -a $LOG_FILE
cat <<EOF > "/var/log/failover-tests/report-${TIMESTAMP}.md"
# Failover Test Report

**Date**: $(date)
**Test Type**: Automated Multi-AZ Failover
**Duration**: ${FAILOVER_DURATION} seconds
**Status**: SUCCESS

## Metrics

- **Failover Duration**: ${FAILOVER_DURATION}s (Target: < 120s)
- **Data Loss**: 0 transactions (Target: 0)
- **Service Downtime**: Measured in smoke tests
- **Application Errors**: Logged in application metrics

## Test Steps

1. Pre-test validation: PASSED
2. Baseline metrics recorded: PASSED
3. Failover triggered: PASSED
4. Failover monitoring: PASSED
5. Service verification: PASSED
6. Smoke tests: PASSED

## Recommendations

$(./scripts/generate-recommendations.sh)

EOF

echo "=== Automated Failover Test Completed at $(date) ===" | tee -a $LOG_FILE
echo "Report: /var/log/failover-tests/report-${TIMESTAMP}.md"
```

### Test Scenarios

#### Scenario 1: Multi-AZ Failover Test

```bash
# Test Multi-AZ automatic failover
aws rds reboot-db-instance \
  --db-instance-identifier ecommerce-db-primary \
  --force-failover

# Expected Results:
# - Failover completes in < 120 seconds
# - Zero data loss
# - Applications reconnect automatically
# - New standby provisioned in original AZ
```

#### Scenario 2: Read Replica Promotion Test

```bash
# Test read replica promotion to standalone
./scripts/test-replica-promotion.sh

# Steps:
# 1. Create test read replica
# 2. Wait for replication to sync
# 3. Promote replica
# 4. Verify read-write capability
# 5. Clean up test resources
```

#### Scenario 3: Cross-Region Failover Test

```bash
# Test complete region failover
./scripts/test-cross-region-failover.sh

# Steps:
# 1. Verify DR replica lag < 5 seconds
# 2. Stop primary region applications
# 3. Promote DR replica
# 4. Update DNS to DR region
# 5. Start DR region applications
# 6. Verify end-to-end functionality
# 7. Measure RTO/RPO
```


### Test Metrics Collection

```bash
#!/bin/bash
# monitor-failover.sh
# Monitors failover progress and collects metrics

INSTANCE_ID="ecommerce-db-primary"
START_TIME=$(date +%s)
DETECTION_TIME=""
DNS_UPDATE_TIME=""
COMPLETION_TIME=""

echo "Monitoring failover for $INSTANCE_ID..."

# Monitor instance status
while true; do
  STATUS=$(aws rds describe-db-instances \
    --db-instance-identifier $INSTANCE_ID \
    --query 'DBInstances[0].DBInstanceStatus' \
    --output text)
  
  CURRENT_TIME=$(date +%s)
  ELAPSED=$((CURRENT_TIME - START_TIME))
  
  echo "[${ELAPSED}s] Status: $STATUS"
  
  case $STATUS in
    "rebooting")
      if [ -z "$DETECTION_TIME" ]; then
        DETECTION_TIME=$ELAPSED
        echo "Failover detected at ${DETECTION_TIME}s"
      fi
      ;;
    "available")
      COMPLETION_TIME=$ELAPSED
      echo "Failover completed at ${COMPLETION_TIME}s"
      break
      ;;
  esac
  
  sleep 5
done

# Test DNS resolution
echo "Testing DNS resolution..."
DNS_START=$(date +%s)
while true; do
  ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier $INSTANCE_ID \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)
  
  if nslookup $ENDPOINT > /dev/null 2>&1; then
    DNS_UPDATE_TIME=$(($(date +%s) - DNS_START))
    echo "DNS updated in ${DNS_UPDATE_TIME}s"
    break
  fi
  
  sleep 2
done

# Output metrics
cat <<EOF
{
  "failover_detection_time": ${DETECTION_TIME},
  "dns_update_time": ${DNS_UPDATE_TIME},
  "total_failover_time": ${COMPLETION_TIME},
  "target_rto": 120,
  "target_rpo": 0,
  "status": "$([ $COMPLETION_TIME -lt 120 ] && echo 'PASS' || echo 'FAIL')"
}
EOF
```

---

## Manual Failover Procedures

### When to Perform Manual Failover

1. **Planned Maintenance**
   - OS patching in specific AZ
   - Hardware maintenance
   - Network maintenance

2. **Performance Issues**
   - Primary AZ experiencing degradation
   - Network latency issues
   - Storage performance problems

3. **Testing**
   - DR drills
   - Failover procedure validation
   - Team training

### Manual Failover Steps

#### Pre-Failover Checklist

- [ ] Verify standby instance is healthy
- [ ] Check replication lag (should be 0 for Multi-AZ)
- [ ] Notify stakeholders of planned failover
- [ ] Schedule during maintenance window
- [ ] Prepare rollback plan
- [ ] Have incident response team on standby

#### Execution Steps

```bash
# Step 1: Verify current configuration
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-primary \
  --query 'DBInstances[0].[DBInstanceIdentifier,MultiAZ,AvailabilityZone,SecondaryAvailabilityZone]'

# Step 2: Check application health
kubectl get pods -n production -o wide
curl -s https://api.ecommerce.example.com/actuator/health | jq .

# Step 3: Initiate manual failover
aws rds reboot-db-instance \
  --db-instance-identifier ecommerce-db-primary \
  --force-failover

# Step 4: Monitor failover progress
watch -n 5 'aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-primary \
  --query "DBInstances[0].[DBInstanceStatus,AvailabilityZone]"'

# Step 5: Wait for completion
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-db-primary

# Step 6: Verify new primary AZ
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-primary \
  --query 'DBInstances[0].[AvailabilityZone,SecondaryAvailabilityZone]'

# Step 7: Test application connectivity
kubectl exec -it deployment/order-service -n production -- \
  psql -h ecommerce-db-primary.us-east-1.rds.amazonaws.com \
       -U admin -d ecommerce -c "SELECT 1;"

# Step 8: Run smoke tests
./scripts/smoke-tests.sh

# Step 9: Monitor for 15 minutes
./scripts/monitor-post-failover.sh
```


#### Post-Failover Verification

```bash
#!/bin/bash
# verify-post-failover.sh
# Comprehensive post-failover verification

echo "=== Post-Failover Verification ==="

# 1. Database connectivity
echo "1. Testing database connectivity..."
psql -h ecommerce-db-primary.us-east-1.rds.amazonaws.com \
     -U admin -d ecommerce -c "SELECT version();" && echo "✓ PASS" || echo "✗ FAIL"

# 2. Replication status
echo "2. Checking replication status..."
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-primary \
  --query 'DBInstances[0].[MultiAZ,DBInstanceStatus]' && echo "✓ PASS" || echo "✗ FAIL"

# 3. Application health
echo "3. Checking application health..."
for service in order-service customer-service product-service; do
  STATUS=$(kubectl get deployment $service -n production -o jsonpath='{.status.conditions[?(@.type=="Available")].status}')
  echo "  $service: $STATUS"
done

# 4. Transaction test
echo "4. Running transaction test..."
psql -h ecommerce-db-primary.us-east-1.rds.amazonaws.com \
     -U admin -d ecommerce <<EOF
BEGIN;
CREATE TEMP TABLE failover_test (id INT, test_time TIMESTAMP);
INSERT INTO failover_test VALUES (1, NOW());
SELECT * FROM failover_test;
ROLLBACK;
EOF
echo "✓ PASS"

# 5. Performance metrics
echo "5. Checking performance metrics..."
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name DatabaseConnections \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average

# 6. Error rate check
echo "6. Checking error rates..."
kubectl logs -l app=order-service -n production --since=5m | grep -i error | wc -l

echo "=== Verification Complete ==="
```

### Emergency Rollback

If failover causes issues:

```bash
# Option 1: Failback to original AZ (if Multi-AZ)
aws rds reboot-db-instance \
  --db-instance-identifier ecommerce-db-primary \
  --force-failover

# Option 2: Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-db-emergency-restore \
  --db-snapshot-identifier ecommerce-db-snapshot-pre-failover

# Option 3: Promote read replica
aws rds promote-read-replica \
  --db-instance-identifier ecommerce-db-replica-1
```

---

## Split-Brain Prevention

### What is Split-Brain?

Split-brain occurs when network partition causes multiple instances to believe they are primary, potentially leading to data inconsistency.

### RDS Multi-AZ Protection

RDS Multi-AZ **automatically prevents split-brain** through:

1. **Single Writer Principle**
   - Only one instance accepts writes at any time
   - Standby is read-only and cannot accept writes
   - Enforced at database engine level

2. **Fencing Mechanism**
   - AWS automatically fences failed primary
   - STONITH (Shoot The Other Node In The Head)
   - Failed instance cannot rejoin as primary

3. **Quorum-Based Failover**
   - AWS control plane acts as tie-breaker
   - Requires majority consensus for failover
   - Prevents simultaneous promotions

### Split-Brain Detection

```bash
#!/bin/bash
# detect-split-brain.sh
# Monitors for potential split-brain scenarios

INSTANCE_ID="ecommerce-db-primary"

# Check if multiple instances claim to be primary
PRIMARIES=$(aws rds describe-db-instances \
  --filters "Name=db-instance-id,Values=ecommerce-db-*" \
  --query 'DBInstances[?ReadReplicaSourceDBInstanceIdentifier==`null`].DBInstanceIdentifier' \
  --output text | wc -w)

if [ $PRIMARIES -gt 1 ]; then
  echo "⚠️  ALERT: Multiple primary instances detected!"
  echo "Instances claiming to be primary:"
  aws rds describe-db-instances \
    --filters "Name=db-instance-id,Values=ecommerce-db-*" \
    --query 'DBInstances[?ReadReplicaSourceDBInstanceIdentifier==`null`].[DBInstanceIdentifier,DBInstanceStatus,AvailabilityZone]' \
    --output table
  
  # Send alert
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:critical-alerts \
    --subject "CRITICAL: Potential Split-Brain Detected" \
    --message "Multiple RDS instances detected as primary. Immediate investigation required."
else
  echo "✓ No split-brain detected. Single primary instance confirmed."
fi
```


### Application-Level Protection

```java
// Connection pool configuration with failover protection
@Configuration
public class DataSourceConfiguration {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Primary endpoint (automatically handles Multi-AZ failover)
        config.setJdbcUrl("jdbc:postgresql://ecommerce-db-primary.us-east-1.rds.amazonaws.com:5432/ecommerce");
        config.setUsername("admin");
        config.setPassword(getPassword());
        
        // Connection pool settings
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);  // 30 seconds
        config.setIdleTimeout(600000);       // 10 minutes
        config.setMaxLifetime(1800000);      // 30 minutes
        
        // Failover protection
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // Retry logic
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "10");
        config.addDataSourceProperty("connectTimeout", "10");
        
        return new HikariDataSource(config);
    }
}
```

### Network Partition Handling

```bash
# Monitor network connectivity between AZs
#!/bin/bash
# monitor-az-connectivity.sh

PRIMARY_AZ="us-east-1a"
STANDBY_AZ="us-east-1b"

# Test connectivity to both AZs
echo "Testing connectivity to $PRIMARY_AZ..."
ping -c 5 10.0.10.10  # Primary AZ subnet

echo "Testing connectivity to $STANDBY_AZ..."
ping -c 5 10.0.11.10  # Standby AZ subnet

# Check VPC flow logs for dropped packets
aws ec2 describe-flow-logs \
  --filter "Name=resource-id,Values=vpc-12345678" \
  --query 'FlowLogs[0].FlowLogId'

# Query recent flow logs
aws logs filter-log-events \
  --log-group-name /aws/vpc/flowlogs \
  --start-time $(($(date +%s) - 3600))000 \
  --filter-pattern "[version, account, eni, source, destination, srcport, destport, protocol, packets, bytes, windowstart, windowend, action=REJECT, flowlogstatus]" \
  --max-items 10
```

---

## Data Consistency Verification

### Post-Failover Consistency Checks

```bash
#!/bin/bash
# verify-data-consistency.sh
# Comprehensive data consistency verification after failover

echo "=== Data Consistency Verification ==="

DB_HOST="ecommerce-db-primary.us-east-1.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"

# 1. Check table row counts
echo "1. Verifying table row counts..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME <<EOF
SELECT 
  schemaname,
  tablename,
  n_live_tup as row_count
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC
LIMIT 20;
EOF

# 2. Verify critical data integrity
echo "2. Checking critical data integrity..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME <<EOF
-- Check for orphaned orders
SELECT COUNT(*) as orphaned_orders
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- Check for orders without items
SELECT COUNT(*) as orders_without_items
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE oi.id IS NULL;

-- Check for negative inventory
SELECT COUNT(*) as negative_inventory
FROM products
WHERE stock_quantity < 0;

-- Check for future-dated orders
SELECT COUNT(*) as future_orders
FROM orders
WHERE created_at > NOW();
EOF

# 3. Verify referential integrity
echo "3. Verifying referential integrity..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME <<EOF
-- Check all foreign key constraints
SELECT 
  tc.table_name,
  tc.constraint_name,
  tc.constraint_type
FROM information_schema.table_constraints tc
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_schema = 'public'
ORDER BY tc.table_name;
EOF

# 4. Check sequence consistency
echo "4. Checking sequence consistency..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME <<EOF
SELECT 
  schemaname,
  sequencename,
  last_value,
  is_called
FROM pg_sequences
WHERE schemaname = 'public'
ORDER BY sequencename;
EOF

# 5. Verify indexes
echo "5. Verifying index integrity..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME <<EOF
-- Check for invalid indexes
SELECT 
  schemaname,
  tablename,
  indexname,
  indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND indexname IN (
    SELECT indexrelid::regclass::text
    FROM pg_index
    WHERE NOT indisvalid
  );
EOF

# 6. Check for corrupted data
echo "6. Checking for data corruption..."
psql -h $DB_HOST -U $DB_USER -d $DB_NAME <<EOF
-- Run ANALYZE to update statistics
ANALYZE;

-- Check for bloat
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as external_size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;
EOF

echo "=== Consistency Verification Complete ==="
```


### Checksum Verification

```bash
#!/bin/bash
# checksum-verification.sh
# Verify data checksums between primary and replica

PRIMARY_HOST="ecommerce-db-primary.us-east-1.rds.amazonaws.com"
REPLICA_HOST="ecommerce-db-dr-replica.us-west-2.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"

echo "=== Checksum Verification ==="

# Function to calculate table checksum
calculate_checksum() {
  local host=$1
  local table=$2
  
  psql -h $host -U $DB_USER -d $DB_NAME -t -c "
    SELECT MD5(STRING_AGG(t::text, '' ORDER BY t::text))
    FROM (SELECT * FROM $table) t;
  "
}

# Tables to verify
TABLES=("customers" "orders" "order_items" "products" "payments")

for table in "${TABLES[@]}"; do
  echo "Verifying $table..."
  
  PRIMARY_CHECKSUM=$(calculate_checksum $PRIMARY_HOST $table)
  REPLICA_CHECKSUM=$(calculate_checksum $REPLICA_HOST $table)
  
  if [ "$PRIMARY_CHECKSUM" == "$REPLICA_CHECKSUM" ]; then
    echo "  ✓ $table: MATCH"
  else
    echo "  ✗ $table: MISMATCH"
    echo "    Primary: $PRIMARY_CHECKSUM"
    echo "    Replica: $REPLICA_CHECKSUM"
  fi
done

echo "=== Checksum Verification Complete ==="
```

### Transaction Log Verification

```sql
-- Verify transaction log consistency
-- Run on primary after failover

-- Check for gaps in transaction IDs
SELECT 
  CASE 
    WHEN txid_current() - last_txid > 1000000 
    THEN 'WARNING: Large transaction ID gap detected'
    ELSE 'OK: Transaction IDs consistent'
  END as status,
  txid_current() as current_txid,
  last_txid,
  txid_current() - last_txid as gap
FROM (
  SELECT MAX(xmin::text::bigint) as last_txid
  FROM orders
) t;

-- Verify WAL position
SELECT 
  pg_current_wal_lsn() as current_wal_position,
  pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') / 1024 / 1024 as wal_mb;

-- Check for unresolved prepared transactions
SELECT 
  gid,
  prepared,
  owner,
  database
FROM pg_prepared_xacts;
```

---

## HA Monitoring and Alerting

### CloudWatch Metrics

#### Critical Metrics

```bash
# Create comprehensive CloudWatch dashboard
aws cloudwatch put-dashboard \
  --dashboard-name ecommerce-db-ha-monitoring \
  --dashboard-body file://ha-dashboard.json
```

```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/RDS", "DatabaseConnections", {"stat": "Average"}],
          [".", "CPUUtilization", {"stat": "Average"}],
          [".", "FreeableMemory", {"stat": "Average"}],
          [".", "ReadLatency", {"stat": "Average"}],
          [".", "WriteLatency", {"stat": "Average"}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "RDS Performance Metrics",
        "yAxis": {
          "left": {"min": 0}
        }
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["AWS/RDS", "ReplicaLag", {"stat": "Maximum"}]
        ],
        "period": 60,
        "stat": "Maximum",
        "region": "us-west-2",
        "title": "DR Replica Lag",
        "yAxis": {
          "left": {"min": 0, "max": 60}
        },
        "annotations": {
          "horizontal": [
            {"value": 30, "label": "Warning", "color": "#ff9900"},
            {"value": 60, "label": "Critical", "color": "#ff0000"}
          ]
        }
      }
    }
  ]
}
```


### Alert Configuration

```bash
# Critical: Multi-AZ failover detected
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-db-failover-detected \
  --alarm-description "Alert when Multi-AZ failover occurs" \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 60 \
  --evaluation-periods 1 \
  --threshold 0 \
  --comparison-operator LessThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:critical-alerts

# Critical: High replication lag
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-db-dr-replica-high-lag \
  --alarm-description "Alert when DR replica lag exceeds 60 seconds" \
  --metric-name ReplicaLag \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 60 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-dr-replica \
  --alarm-actions arn:aws:sns:us-west-2:123456789012:critical-alerts \
  --region us-west-2

# Warning: Elevated CPU usage
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-db-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:ops-alerts

# Warning: Low freeable memory
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-db-low-memory \
  --alarm-description "Alert when freeable memory drops below 2GB" \
  --metric-name FreeableMemory \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --evaluation-periods 2 \
  --threshold 2147483648 \
  --comparison-operator LessThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:ops-alerts

# Critical: Database connections exhausted
aws cloudwatch put-metric-alarm \
  --alarm-name ecommerce-db-connection-exhaustion \
  --alarm-description "Alert when connections exceed 90% of max" \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 60 \
  --evaluation-periods 2 \
  --threshold 180 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-primary \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:critical-alerts
```

### Custom Monitoring Scripts

```bash
#!/bin/bash
# ha-health-check.sh
# Comprehensive HA health monitoring

echo "=== HA Health Check $(date) ==="

# 1. Check Multi-AZ status
echo "1. Multi-AZ Status:"
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-primary \
  --query 'DBInstances[0].[MultiAZ,DBInstanceStatus,AvailabilityZone,SecondaryAvailabilityZone]' \
  --output table

# 2. Check read replicas
echo "2. Read Replica Status:"
aws rds describe-db-instances \
  --filters "Name=db-instance-id,Values=ecommerce-db-replica-*" \
  --query 'DBInstances[].[DBInstanceIdentifier,DBInstanceStatus,ReadReplicaSourceDBInstanceIdentifier]' \
  --output table

# 3. Check replication lag
echo "3. Replication Lag:"
for replica in ecommerce-db-replica-1 ecommerce-db-replica-2; do
  LAG=$(aws cloudwatch get-metric-statistics \
    --namespace AWS/RDS \
    --metric-name ReplicaLag \
    --dimensions Name=DBInstanceIdentifier,Value=$replica \
    --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average \
    --query 'Datapoints[0].Average')
  
  echo "  $replica: ${LAG:-N/A} seconds"
done

# 4. Check DR replica lag
echo "4. DR Replica Lag:"
DR_LAG=$(aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-db-dr-replica \
  --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region us-west-2 \
  --query 'Datapoints[0].Average')

echo "  ecommerce-db-dr-replica: ${DR_LAG:-N/A} seconds"

# 5. Check automated backups
echo "5. Automated Backups:"
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-db-primary \
  --query 'DBInstances[0].[BackupRetentionPeriod,PreferredBackupWindow,LatestRestorableTime]' \
  --output table

# 6. Check recent snapshots
echo "6. Recent Snapshots:"
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-db-primary \
  --max-records 5 \
  --query 'DBSnapshots[].[DBSnapshotIdentifier,SnapshotCreateTime,Status]' \
  --output table

# 7. Overall health score
echo "7. Overall Health Score:"
HEALTH_SCORE=100

# Deduct points for issues
if [ "$DR_LAG" != "N/A" ] && [ $(echo "$DR_LAG > 30" | bc) -eq 1 ]; then
  HEALTH_SCORE=$((HEALTH_SCORE - 20))
  echo "  -20: High DR replica lag"
fi

echo "  Final Score: $HEALTH_SCORE/100"

if [ $HEALTH_SCORE -ge 90 ]; then
  echo "  Status: ✓ HEALTHY"
elif [ $HEALTH_SCORE -ge 70 ]; then
  echo "  Status: ⚠ WARNING"
else
  echo "  Status: ✗ CRITICAL"
fi

echo "=== Health Check Complete ==="
```


### Grafana Dashboard

```yaml
# grafana-ha-dashboard.yaml
# Import into Grafana for HA monitoring

apiVersion: 1
providers:
  - name: 'RDS HA Monitoring'
    orgId: 1
    folder: 'Database'
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards

dashboard:
  title: "RDS High Availability Monitoring"
  tags: ["rds", "ha", "database"]
  timezone: "browser"
  panels:
    - title: "Multi-AZ Status"
      type: "stat"
      targets:
        - expr: 'aws_rds_multi_az{dbinstance="ecommerce-db-primary"}'
      
    - title: "Replication Lag"
      type: "graph"
      targets:
        - expr: 'aws_rds_replica_lag{dbinstance=~"ecommerce-db-.*"}'
      yaxes:
        - format: "s"
          label: "Lag (seconds)"
      alert:
        conditions:
          - evaluator:
              params: [60]
              type: "gt"
            operator:
              type: "and"
            query:
              params: ["A", "5m", "now"]
            reducer:
              type: "avg"
            type: "query"
        name: "High Replication Lag"
        
    - title: "Database Connections"
      type: "graph"
      targets:
        - expr: 'aws_rds_database_connections{dbinstance="ecommerce-db-primary"}'
      yaxes:
        - format: "short"
          label: "Connections"
          
    - title: "Failover Events (Last 24h)"
      type: "table"
      targets:
        - expr: 'changes(aws_rds_failover_count{dbinstance="ecommerce-db-primary"}[24h])'
```

---

## Operational Procedures

### Daily Operations

```bash
# Daily HA health check (automated via cron)
0 9 * * * /opt/scripts/ha-health-check.sh | mail -s "Daily HA Health Report" ops@example.com

# Daily replication lag check
*/15 * * * * /opt/scripts/check-replication-lag.sh

# Daily backup verification
0 10 * * * /opt/scripts/verify-backups.sh
```

### Weekly Operations

```bash
# Weekly failover test (automated)
0 2 * * 0 /opt/scripts/weekly-failover-test.sh

# Weekly DR replica verification
0 3 * * 0 /opt/scripts/verify-dr-replica.sh

# Weekly backup restoration test
0 4 * * 0 /opt/scripts/test-backup-restore.sh
```

### Monthly Operations

```bash
# Monthly full DR drill
0 2 1 * * /opt/scripts/monthly-dr-drill.sh

# Monthly capacity planning review
0 10 1 * * /opt/scripts/capacity-planning-report.sh

# Monthly HA configuration audit
0 11 1 * * /opt/scripts/audit-ha-configuration.sh
```

### Incident Response

#### Severity Levels

| Severity | Description | Response Time | Escalation |
|----------|-------------|---------------|------------|
| **P0 - Critical** | Primary database down, Multi-AZ failover failed | Immediate | CTO, VP Engineering |
| **P1 - High** | High replication lag (>60s), Performance degradation | 15 minutes | Engineering Manager |
| **P2 - Medium** | Elevated lag (30-60s), Minor issues | 1 hour | Team Lead |
| **P3 - Low** | Monitoring alerts, Non-critical issues | 4 hours | On-call engineer |

#### Incident Response Playbook

```bash
#!/bin/bash
# incident-response.sh
# Automated incident response for HA issues

SEVERITY=$1
ISSUE=$2

case $SEVERITY in
  P0)
    echo "P0 CRITICAL: $ISSUE"
    # Immediate actions
    ./scripts/notify-leadership.sh "P0: $ISSUE"
    ./scripts/activate-dr-team.sh
    ./scripts/assess-situation.sh
    ./scripts/initiate-failover-if-needed.sh
    ;;
    
  P1)
    echo "P1 HIGH: $ISSUE"
    # High priority actions
    ./scripts/notify-engineering.sh "P1: $ISSUE"
    ./scripts/investigate-issue.sh
    ./scripts/prepare-mitigation.sh
    ;;
    
  P2)
    echo "P2 MEDIUM: $ISSUE"
    # Medium priority actions
    ./scripts/notify-team.sh "P2: $ISSUE"
    ./scripts/schedule-investigation.sh
    ;;
    
  P3)
    echo "P3 LOW: $ISSUE"
    # Low priority actions
    ./scripts/log-issue.sh "P3: $ISSUE"
    ./scripts/create-ticket.sh
    ;;
esac
```

---

## Related Documentation

- [Disaster Recovery Perspective](../../perspectives/availability/disaster-recovery.md) - Strategic DR planning
- [Backup and Restore Guide](backup-restore.md) - Backup procedures
- [Database Maintenance](database-maintenance.md) - Routine maintenance
- [Physical Architecture](../../viewpoints/deployment/physical-architecture.md) - RDS infrastructure
- [Operational Runbooks](../runbooks/) - Detailed operational procedures

---

## Appendix

### Glossary

- **RTO (Recovery Time Objective)**: Maximum acceptable downtime
- **RPO (Recovery Point Objective)**: Maximum acceptable data loss
- **Multi-AZ**: Multiple Availability Zone deployment
- **PITR**: Point-in-Time Recovery
- **Split-Brain**: Multiple instances believing they are primary
- **Replication Lag**: Time delay between primary and replica

### References

- [AWS RDS Multi-AZ Documentation](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.MultiAZ.html)
- [AWS RDS Read Replicas](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ReadRepl.html)
- [PostgreSQL High Availability](https://www.postgresql.org/docs/current/high-availability.html)

---

**Last Updated**: 2025-11-08  
**Owner**: Operations & Database Team  
**Review Cycle**: Quarterly  
**Next Review**: 2026-02-08

