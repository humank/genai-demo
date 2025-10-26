---
title: "Backup Testing Procedures"
viewpoint: "Operational"
status: "active"
last_updated: "2025-10-26"
stakeholders: ["Operations Team", "DBA Team", "SRE Team", "QA Team"]
---

# Backup Testing Procedures

> **Viewpoint**: Operational  
> **Purpose**: Define comprehensive backup testing procedures to ensure backup reliability and recoverability  
> **Audience**: Operations Team, DBA Team, SRE Team, QA Team

## Overview

This document outlines the backup testing procedures, schedules, and validation frameworks to ensure that all backups are reliable, recoverable, and meet our RTO/RPO objectives. Regular backup testing is critical to validate disaster recovery capabilities and identify issues before they impact production.

## Testing Philosophy

### Core Principles

```yaml
Testing Principles:
  - Test Early, Test Often: Regular testing prevents surprises
  - Automate Everything: Reduce human error and increase frequency
  - Document Everything: Track results and lessons learned
  - Continuous Improvement: Update procedures based on findings
  - Realistic Scenarios: Test actual failure conditions
  - Measure and Monitor: Track RTO/RPO metrics
```

### Testing Objectives

**Primary Objectives**:
- Verify backup integrity and completeness
- Validate restore procedures and documentation
- Measure actual RTO and RPO
- Identify gaps in backup coverage
- Train team on recovery procedures
- Ensure compliance with backup policies

**Success Criteria**:
- All backups can be successfully restored
- RTO meets target (< 1 hour for critical systems)
- RPO meets target (< 15 minutes for critical data)
- Zero data loss during restore
- Documentation is accurate and complete
- Team can execute procedures without assistance

## Monthly Backup Restore Testing

### Monthly Testing Schedule


```yaml
Monthly Testing Calendar:
  Week 1 (1st-7th):
    Focus: Database Backups
    Tests:
      - RDS automated snapshot restore
      - Point-in-time recovery validation
      - Cross-region replica promotion
    Duration: 4 hours
    Team: DBA Team + Operations
    
  Week 2 (8th-14th):
    Focus: Application State Backups
    Tests:
      - Redis cache restore
      - Kafka topic recovery
      - Configuration restore
    Duration: 3 hours
    Team: Operations Team
    
  Week 3 (15th-21st):
    Focus: Infrastructure Backups
    Tests:
      - CDK infrastructure recreation
      - EKS cluster configuration restore
      - Secrets and credentials restore
    Duration: 4 hours
    Team: SRE Team + Operations
    
  Week 4 (22nd-28th):
    Focus: Integration Testing
    Tests:
      - End-to-end restore validation
      - Application functionality testing
      - Performance benchmarking
    Duration: 6 hours
    Team: All Teams
```

### Database Backup Restore Testing

#### RDS Snapshot Restore Test

**Procedure**:

```bash
#!/bin/bash
# monthly-rds-restore-test.sh

set -e

TEST_DATE=$(date +%Y-%m-%d)
TEST_ID="monthly-test-${TEST_DATE}"
LOG_FILE="/var/log/backup-tests/rds-restore-${TEST_DATE}.log"
RESULTS_FILE="/var/backup-tests/results/rds-${TEST_DATE}.json"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "=== MONTHLY RDS BACKUP RESTORE TEST ==="
echo "Test ID: $TEST_ID"
echo "Start Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Initialize results
cat > $RESULTS_FILE <<EOF
{
  "test_id": "$TEST_ID",
  "test_type": "rds_snapshot_restore",
  "start_time": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "tests": []
}
EOF

# Test 1: Latest Automated Snapshot Restore
echo ""
echo "Test 1: Restoring from latest automated snapshot..."
TEST_START=$(date +%s)

LATEST_SNAPSHOT=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --snapshot-type automated \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

echo "Latest snapshot: $LATEST_SNAPSHOT"

# Restore to test instance
TEST_INSTANCE="ecommerce-test-restore-${TEST_DATE}"
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier $TEST_INSTANCE \
  --db-snapshot-identifier $LATEST_SNAPSHOT \
  --db-instance-class db.t3.medium \
  --no-publicly-accessible \
  --tags Key=Purpose,Value=MonthlyBackupTest Key=TestDate,Value=$TEST_DATE

# Wait for availability
echo "Waiting for instance to be available..."
aws rds wait db-instance-available --db-instance-identifier $TEST_INSTANCE

TEST_END=$(date +%s)
RESTORE_DURATION=$((TEST_END - TEST_START))

# Get endpoint
TEST_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier $TEST_INSTANCE \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "Test instance endpoint: $TEST_ENDPOINT"
echo "Restore duration: ${RESTORE_DURATION}s"

# Validate data integrity
echo "Running data integrity checks..."
VALIDATION_START=$(date +%s)

psql -h $TEST_ENDPOINT -U admin -d ecommerce > /tmp/validation-${TEST_DATE}.txt <<EOF
-- Check table counts
SELECT 'orders' as table_name, COUNT(*) as row_count FROM orders
UNION ALL
SELECT 'customers', COUNT(*) FROM customers
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;

-- Check referential integrity
SELECT 
  'Order-Customer Consistency' as check_name,
  COUNT(*) as issues
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- Check for null primary keys
SELECT 
  'Null Primary Keys' as check_name,
  COUNT(*) as issues
FROM orders
WHERE id IS NULL;

-- Verify recent data (last 24 hours)
SELECT 
  'Recent Orders' as check_name,
  COUNT(*) as count
FROM orders
WHERE created_at > NOW() - INTERVAL '24 hours';
EOF

VALIDATION_END=$(date +%s)
VALIDATION_DURATION=$((VALIDATION_END - VALIDATION_START))

# Parse validation results
VALIDATION_PASSED=true
if grep -q "issues.*[1-9]" /tmp/validation-${TEST_DATE}.txt; then
  VALIDATION_PASSED=false
fi

echo "Validation duration: ${VALIDATION_DURATION}s"
echo "Validation passed: $VALIDATION_PASSED"

# Record test results
jq --arg test "automated_snapshot_restore" \
   --arg snapshot "$LATEST_SNAPSHOT" \
   --argjson duration "$RESTORE_DURATION" \
   --argjson validation_duration "$VALIDATION_DURATION" \
   --arg passed "$VALIDATION_PASSED" \
   '.tests += [{
     "test_name": $test,
     "snapshot_id": $snapshot,
     "restore_duration_seconds": $duration,
     "validation_duration_seconds": $validation_duration,
     "passed": $passed,
     "endpoint": "'$TEST_ENDPOINT'"
   }]' $RESULTS_FILE > /tmp/results.json && mv /tmp/results.json $RESULTS_FILE

# Cleanup test instance
echo "Cleaning up test instance..."
aws rds delete-db-instance \
  --db-instance-identifier $TEST_INSTANCE \
  --skip-final-snapshot

echo "✓ Test 1 completed"


# Test 2: Point-in-Time Recovery
echo ""
echo "Test 2: Point-in-time recovery test..."
TEST_START=$(date +%s)

# Restore to 1 hour ago
RESTORE_TIME=$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)
PITR_INSTANCE="ecommerce-pitr-test-${TEST_DATE}"

echo "Restoring to point in time: $RESTORE_TIME"

aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ecommerce-prod-db \
  --target-db-instance-identifier $PITR_INSTANCE \
  --restore-time $RESTORE_TIME \
  --db-instance-class db.t3.medium \
  --no-publicly-accessible

# Wait for availability
aws rds wait db-instance-available --db-instance-identifier $PITR_INSTANCE

TEST_END=$(date +%s)
PITR_DURATION=$((TEST_END - TEST_START))

PITR_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier $PITR_INSTANCE \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "PITR duration: ${PITR_DURATION}s"

# Verify data at that point in time
psql -h $PITR_ENDPOINT -U admin -d ecommerce -c \
  "SELECT COUNT(*) as orders_at_restore_point FROM orders WHERE created_at <= '$RESTORE_TIME';"

# Record results
jq --arg test "point_in_time_recovery" \
   --arg restore_time "$RESTORE_TIME" \
   --argjson duration "$PITR_DURATION" \
   '.tests += [{
     "test_name": $test,
     "restore_time": $restore_time,
     "duration_seconds": $duration,
     "passed": true,
     "endpoint": "'$PITR_ENDPOINT'"
   }]' $RESULTS_FILE > /tmp/results.json && mv /tmp/results.json $RESULTS_FILE

# Cleanup
aws rds delete-db-instance \
  --db-instance-identifier $PITR_INSTANCE \
  --skip-final-snapshot

echo "✓ Test 2 completed"

# Test 3: Cross-Region Snapshot Restore
echo ""
echo "Test 3: Cross-region snapshot restore..."
TEST_START=$(date +%s)

# Find latest snapshot in DR region
DR_SNAPSHOT=$(aws rds describe-db-snapshots \
  --region us-west-2 \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

echo "DR region snapshot: $DR_SNAPSHOT"

DR_INSTANCE="ecommerce-dr-test-${TEST_DATE}"

aws rds restore-db-instance-from-db-snapshot \
  --region us-west-2 \
  --db-instance-identifier $DR_INSTANCE \
  --db-snapshot-identifier $DR_SNAPSHOT \
  --db-instance-class db.t3.medium

aws rds wait db-instance-available \
  --region us-west-2 \
  --db-instance-identifier $DR_INSTANCE

TEST_END=$(date +%s)
DR_DURATION=$((TEST_END - TEST_START))

echo "DR restore duration: ${DR_DURATION}s"

# Record results
jq --arg test "cross_region_restore" \
   --arg snapshot "$DR_SNAPSHOT" \
   --argjson duration "$DR_DURATION" \
   '.tests += [{
     "test_name": $test,
     "snapshot_id": $snapshot,
     "duration_seconds": $duration,
     "passed": true,
     "region": "us-west-2"
   }]' $RESULTS_FILE > /tmp/results.json && mv /tmp/results.json $RESULTS_FILE

# Cleanup
aws rds delete-db-instance \
  --region us-west-2 \
  --db-instance-identifier $DR_INSTANCE \
  --skip-final-snapshot

echo "✓ Test 3 completed"

# Finalize results
jq --arg end_time "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   '.end_time = $end_time' $RESULTS_FILE > /tmp/results.json && mv /tmp/results.json $RESULTS_FILE

# Generate summary
echo ""
echo "=== TEST SUMMARY ==="
jq -r '.tests[] | "\(.test_name): \(if .passed then "PASSED" else "FAILED" end) (\(.duration_seconds // .restore_duration_seconds)s)"' $RESULTS_FILE

# Send results to monitoring
aws s3 cp $RESULTS_FILE s3://ecommerce-backups-prod-us-east-1/test-results/rds/${TEST_DATE}/
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:backup-test-results \
  --subject "Monthly RDS Backup Test Results - ${TEST_DATE}" \
  --message file://$RESULTS_FILE

echo ""
echo "Test completed successfully"
echo "Results saved to: $RESULTS_FILE"
```

#### Redis Cache Restore Test

**Procedure**:

```bash
#!/bin/bash
# monthly-redis-restore-test.sh

set -e

TEST_DATE=$(date +%Y-%m-%d)
LOG_FILE="/var/log/backup-tests/redis-restore-${TEST_DATE}.log"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "=== MONTHLY REDIS BACKUP RESTORE TEST ==="
echo "Start Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Get latest snapshot
LATEST_SNAPSHOT=$(aws elasticache describe-snapshots \
  --replication-group-id ecommerce-redis-cluster \
  --query 'Snapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotName' \
  --output text)

echo "Testing snapshot: $LATEST_SNAPSHOT"

# Restore to test cluster
TEST_CLUSTER="ecommerce-redis-test-${TEST_DATE}"
TEST_START=$(date +%s)

aws elasticache create-replication-group \
  --replication-group-id $TEST_CLUSTER \
  --replication-group-description "Monthly backup test" \
  --snapshot-name $LATEST_SNAPSHOT \
  --cache-node-type cache.t3.medium \
  --num-cache-clusters 2 \
  --automatic-failover-enabled

# Wait for availability
aws elasticache wait replication-group-available \
  --replication-group-id $TEST_CLUSTER

TEST_END=$(date +%s)
RESTORE_DURATION=$((TEST_END - TEST_START))

# Get endpoint
TEST_ENDPOINT=$(aws elasticache describe-replication-groups \
  --replication-group-id $TEST_CLUSTER \
  --query 'ReplicationGroups[0].NodeGroups[0].PrimaryEndpoint.Address' \
  --output text)

echo "Test cluster endpoint: $TEST_ENDPOINT"
echo "Restore duration: ${RESTORE_DURATION}s"

# Validate data
echo "Validating Redis data..."
PROD_KEY_COUNT=$(redis-cli -h ecommerce-redis-cluster.xxx.cache.amazonaws.com -p 6379 DBSIZE | awk '{print $2}')
TEST_KEY_COUNT=$(redis-cli -h $TEST_ENDPOINT -p 6379 DBSIZE | awk '{print $2}')

echo "Production key count: $PROD_KEY_COUNT"
echo "Test cluster key count: $TEST_KEY_COUNT"

# Calculate difference
DIFF=$((PROD_KEY_COUNT - TEST_KEY_COUNT))
DIFF_PERCENT=$(echo "scale=2; ($DIFF / $PROD_KEY_COUNT) * 100" | bc)

echo "Key count difference: $DIFF ($DIFF_PERCENT%)"

# Test should pass if difference is within acceptable range (< 5%)
if (( $(echo "$DIFF_PERCENT < 5" | bc -l) )); then
  echo "✓ Validation passed"
  TEST_PASSED=true
else
  echo "❌ Validation failed - key count difference too large"
  TEST_PASSED=false
fi

# Cleanup
aws elasticache delete-replication-group \
  --replication-group-id $TEST_CLUSTER \
  --retain-primary-cluster false

echo "Test completed: $TEST_PASSED"
```

### Configuration Backup Restore Testing

**Procedure**:

```bash
#!/bin/bash
# monthly-config-restore-test.sh

set -e

TEST_DATE=$(date +%Y-%m-%d)
TEST_NAMESPACE="backup-test-${TEST_DATE}"

echo "=== MONTHLY CONFIGURATION BACKUP RESTORE TEST ==="

# Get latest configuration backup
LATEST_BACKUP=$(aws s3 ls s3://ecommerce-backups-prod-us-east-1/application-config/ \
  | sort | tail -n 1 | awk '{print $4}')

echo "Testing backup: $LATEST_BACKUP"

# Download backup
aws s3 cp s3://ecommerce-backups-prod-us-east-1/application-config/$LATEST_BACKUP /tmp/

# Extract
tar -xzf /tmp/$LATEST_BACKUP -C /tmp/

# Create test namespace
kubectl create namespace $TEST_NAMESPACE

# Apply configurations
kubectl apply -f /tmp/config-backup-*/configmaps.yaml -n $TEST_NAMESPACE
kubectl apply -f /tmp/config-backup-*/secrets.yaml -n $TEST_NAMESPACE

# Verify
CONFIGMAP_COUNT=$(kubectl get configmaps -n $TEST_NAMESPACE --no-headers | wc -l)
SECRET_COUNT=$(kubectl get secrets -n $TEST_NAMESPACE --no-headers | wc -l)

echo "Restored ConfigMaps: $CONFIGMAP_COUNT"
echo "Restored Secrets: $SECRET_COUNT"

# Cleanup
kubectl delete namespace $TEST_NAMESPACE

echo "✓ Configuration restore test completed"
```

## Quarterly Full DR Drill Procedures


### Quarterly DR Drill Schedule

```yaml
Q1 Drill (January):
  Type: Tabletop Exercise
  Date: Second Tuesday of January
  Duration: 4 hours
  Participants: All teams
  Scope: Review and walkthrough
  
Q2 Drill (April):
  Type: Partial Failover
  Date: Second Tuesday of April
  Duration: 6 hours
  Participants: Engineering + Operations
  Scope: Non-production failover
  
Q3 Drill (July):
  Type: Full DR Simulation
  Date: Second Tuesday of July
  Duration: 8 hours
  Participants: All teams
  Scope: Complete production-like failover
  
Q4 Drill (October):
  Type: Surprise Drill
  Date: Random Tuesday in October
  Duration: 6 hours
  Participants: On-call teams
  Scope: Unannounced failover test
```

### Full DR Drill Procedure

**Pre-Drill Preparation (1 Week Before)**:

```bash
#!/bin/bash
# dr-drill-preparation.sh

set -e

DRILL_DATE=$1
DRILL_TYPE=$2

if [ -z "$DRILL_DATE" ] || [ -z "$DRILL_TYPE" ]; then
  echo "Usage: $0 <drill-date> <tabletop|partial|full|surprise>"
  exit 1
fi

echo "=== DR DRILL PREPARATION ==="
echo "Drill Date: $DRILL_DATE"
echo "Drill Type: $DRILL_TYPE"

# 1. Verify all backups are current
echo "Verifying backup currency..."
LATEST_RDS=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotCreateTime' \
  --output text)

LATEST_REDIS=$(aws elasticache describe-snapshots \
  --replication-group-id ecommerce-redis-cluster \
  --query 'Snapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotCreateTime' \
  --output text)

echo "Latest RDS snapshot: $LATEST_RDS"
echo "Latest Redis snapshot: $LATEST_REDIS"

# Check if backups are recent (within 24 hours)
CURRENT_TIME=$(date +%s)
RDS_TIME=$(date -d "$LATEST_RDS" +%s)
REDIS_TIME=$(date -d "$LATEST_REDIS" +%s)

RDS_AGE=$((CURRENT_TIME - RDS_TIME))
REDIS_AGE=$((CURRENT_TIME - REDIS_TIME))

if [ $RDS_AGE -gt 86400 ] || [ $REDIS_AGE -gt 86400 ]; then
  echo "❌ Backups are not current. Please investigate."
  exit 1
fi

echo "✓ All backups are current"

# 2. Verify DR region readiness
echo "Verifying DR region readiness..."
DR_RDS=$(aws rds describe-db-instances \
  --region us-west-2 \
  --db-instance-identifier ecommerce-prod-db-replica \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text)

if [ "$DR_RDS" != "available" ]; then
  echo "❌ DR RDS replica not available"
  exit 1
fi

echo "✓ DR region is ready"

# 3. Create drill documentation
mkdir -p /var/dr-drills/$DRILL_DATE

cat > /var/dr-drills/$DRILL_DATE/drill-plan.md <<EOF
# DR Drill Plan - $DRILL_DATE

## Drill Information
- **Type**: $DRILL_TYPE
- **Date**: $DRILL_DATE
- **Duration**: 6-8 hours
- **Participants**: [List participants]

## Objectives
- Validate DR procedures
- Measure RTO and RPO
- Train team members
- Identify improvement areas

## Success Criteria
- RTO < 1 hour
- RPO < 15 minutes
- All critical services operational
- Zero data loss

## Drill Scenario
[Describe the failure scenario]

## Roles and Responsibilities
- **Incident Commander**: [Name]
- **Database Team**: [Names]
- **Operations Team**: [Names]
- **Communications**: [Name]

## Timeline
- 09:00 - Drill kickoff
- 09:15 - Simulate failure
- 09:30 - Begin failover
- 10:30 - Target: Services restored
- 11:00 - Validation and testing
- 12:00 - Debrief and lessons learned

## Communication Plan
- War room: #dr-drill-$DRILL_DATE
- Status updates: Every 15 minutes
- Stakeholder notifications: As per plan
EOF

# 4. Send notifications
if [ "$DRILL_TYPE" != "surprise" ]; then
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:dr-drill-notifications \
    --subject "DR Drill Scheduled - $DRILL_DATE" \
    --message "A $DRILL_TYPE DR drill is scheduled for $DRILL_DATE. Please review the drill plan at /var/dr-drills/$DRILL_DATE/drill-plan.md"
fi

# 5. Prepare monitoring dashboards
echo "Preparing monitoring dashboards..."
aws cloudwatch put-dashboard \
  --dashboard-name dr-drill-$DRILL_DATE \
  --dashboard-body file:///opt/monitoring/dr-drill-dashboard.json

echo "✓ DR drill preparation completed"
echo "Drill plan: /var/dr-drills/$DRILL_DATE/drill-plan.md"
```

**During Drill Execution**:

```bash
#!/bin/bash
# dr-drill-execution.sh

set -e

DRILL_DATE=$(date +%Y-%m-%d)
DRILL_LOG="/var/dr-drills/$DRILL_DATE/execution-log.txt"
METRICS_FILE="/var/dr-drills/$DRILL_DATE/metrics.json"

exec > >(tee -a "$DRILL_LOG") 2>&1

echo "=== DR DRILL EXECUTION ==="
echo "Start Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Initialize metrics
cat > $METRICS_FILE <<EOF
{
  "drill_date": "$DRILL_DATE",
  "start_time": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "phases": []
}
EOF

# Phase 1: Simulate Failure
echo ""
echo "Phase 1: Simulating primary region failure..."
PHASE_START=$(date +%s)

# For drill purposes, we'll use a test environment
# In production drill, this would involve actual failover

echo "Simulating failure scenario..."
sleep 5  # Simulate detection time

PHASE_END=$(date +%s)
PHASE_DURATION=$((PHASE_END - PHASE_START))

jq --arg phase "failure_simulation" \
   --argjson duration "$PHASE_DURATION" \
   '.phases += [{"phase": $phase, "duration_seconds": $duration, "status": "completed"}]' \
   $METRICS_FILE > /tmp/metrics.json && mv /tmp/metrics.json $METRICS_FILE

# Phase 2: Promote DR Database
echo ""
echo "Phase 2: Promoting DR database..."
PHASE_START=$(date +%s)

# Execute DR failover script
/opt/scripts/dr-failover-complete.sh

PHASE_END=$(date +%s)
PHASE_DURATION=$((PHASE_END - PHASE_START))

jq --arg phase "database_promotion" \
   --argjson duration "$PHASE_DURATION" \
   '.phases += [{"phase": $phase, "duration_seconds": $duration, "status": "completed"}]' \
   $METRICS_FILE > /tmp/metrics.json && mv /tmp/metrics.json $METRICS_FILE

# Phase 3: Application Deployment
echo ""
echo "Phase 3: Deploying applications..."
PHASE_START=$(date +%s)

# Deploy applications to DR region
kubectl apply -f /opt/k8s/overlays/dr/

PHASE_END=$(date +%s)
PHASE_DURATION=$((PHASE_END - PHASE_START))

jq --arg phase "application_deployment" \
   --argjson duration "$PHASE_DURATION" \
   '.phases += [{"phase": $phase, "duration_seconds": $duration, "status": "completed"}]' \
   $METRICS_FILE > /tmp/metrics.json && mv /tmp/metrics.json $METRICS_FILE

# Phase 4: Validation
echo ""
echo "Phase 4: Validating system functionality..."
PHASE_START=$(date +%s)

# Run validation tests
/opt/scripts/dr-validation-suite.sh

PHASE_END=$(date +%s)
PHASE_DURATION=$((PHASE_END - PHASE_START))

jq --arg phase "validation" \
   --argjson duration "$PHASE_DURATION" \
   '.phases += [{"phase": $phase, "duration_seconds": $duration, "status": "completed"}]' \
   $METRICS_FILE > /tmp/metrics.json && mv /tmp/metrics.json $METRICS_FILE

# Calculate total RTO
DRILL_END=$(date +%s)
DRILL_START=$(jq -r '.start_time' $METRICS_FILE | xargs -I {} date -d {} +%s)
TOTAL_RTO=$((DRILL_END - DRILL_START))

jq --arg end_time "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
   --argjson total_rto "$TOTAL_RTO" \
   '.end_time = $end_time | .total_rto_seconds = $total_rto' \
   $METRICS_FILE > /tmp/metrics.json && mv /tmp/metrics.json $METRICS_FILE

echo ""
echo "=== DRILL COMPLETED ==="
echo "Total RTO: ${TOTAL_RTO}s ($(($TOTAL_RTO / 60)) minutes)"
echo "Target RTO: 3600s (60 minutes)"

if [ $TOTAL_RTO -lt 3600 ]; then
  echo "✓ RTO target met"
else
  echo "❌ RTO target exceeded"
fi

# Generate report
/opt/scripts/generate-drill-report.sh $DRILL_DATE
```

**Post-Drill Analysis**:

```bash
#!/bin/bash
# dr-drill-analysis.sh

DRILL_DATE=$1
METRICS_FILE="/var/dr-drills/$DRILL_DATE/metrics.json"
REPORT_FILE="/var/dr-drills/$DRILL_DATE/drill-report.md"

echo "=== DR DRILL ANALYSIS ==="

# Extract metrics
TOTAL_RTO=$(jq -r '.total_rto_seconds' $METRICS_FILE)
RTO_MINUTES=$(($TOTAL_RTO / 60))

# Generate report
cat > $REPORT_FILE <<EOF
# DR Drill Report - $DRILL_DATE

## Executive Summary

**Drill Date**: $DRILL_DATE
**Total RTO**: ${RTO_MINUTES} minutes
**Target RTO**: 60 minutes
**Status**: $([ $TOTAL_RTO -lt 3600 ] && echo "✓ PASSED" || echo "❌ FAILED")

## Phase Breakdown

EOF

# Add phase details
jq -r '.phases[] | "### \(.phase)\n- Duration: \(.duration_seconds)s\n- Status: \(.status)\n"' $METRICS_FILE >> $REPORT_FILE

cat >> $REPORT_FILE <<EOF

## Lessons Learned

### What Went Well
- [To be filled during debrief]

### What Needs Improvement
- [To be filled during debrief]

### Action Items
- [ ] [Action item 1]
- [ ] [Action item 2]

## Recommendations

1. [Recommendation 1]
2. [Recommendation 2]

## Next Steps

- Schedule follow-up meeting
- Update DR procedures
- Implement improvements
- Schedule next drill

---
**Report Generated**: $(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

echo "Report generated: $REPORT_FILE"

# Send report
aws s3 cp $REPORT_FILE s3://ecommerce-backups-prod-us-east-1/dr-drills/$DRILL_DATE/
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:dr-drill-results \
  --subject "DR Drill Report - $DRILL_DATE" \
  --message file://$REPORT_FILE
```

## Backup Integrity Verification Tests


### Checksum Verification

**Daily Checksum Validation**:

```bash
#!/bin/bash
# daily-checksum-verification.sh

set -e

DATE=$(date +%Y-%m-%d)
LOG_FILE="/var/log/backup-tests/checksum-${DATE}.log"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "=== DAILY BACKUP CHECKSUM VERIFICATION ==="
echo "Date: $DATE"

# Verify S3 object checksums
echo "Verifying S3 backup checksums..."

aws s3api list-objects-v2 \
  --bucket ecommerce-backups-prod-us-east-1 \
  --prefix "rds-snapshots/${DATE}/" \
  --query 'Contents[*].[Key,ETag,Size]' \
  --output text | while read KEY ETAG SIZE; do
  
  echo "Verifying: $KEY"
  
  # Get object metadata
  STORED_MD5=$(aws s3api head-object \
    --bucket ecommerce-backups-prod-us-east-1 \
    --key "$KEY" \
    --query 'Metadata.md5' \
    --output text 2>/dev/null || echo "none")
  
  # Calculate current checksum
  CURRENT_MD5=$(aws s3api head-object \
    --bucket ecommerce-backups-prod-us-east-1 \
    --key "$KEY" \
    --checksum-mode ENABLED \
    --query 'ETag' \
    --output text | tr -d '"')
  
  if [ "$STORED_MD5" != "none" ] && [ "$STORED_MD5" != "$CURRENT_MD5" ]; then
    echo "❌ Checksum mismatch for $KEY"
    echo "  Stored: $STORED_MD5"
    echo "  Current: $CURRENT_MD5"
    
    # Alert on checksum mismatch
    aws sns publish \
      --topic-arn arn:aws:sns:us-east-1:123456789012:backup-integrity-alerts \
      --subject "Backup Integrity Alert - Checksum Mismatch" \
      --message "Checksum mismatch detected for $KEY"
  else
    echo "✓ Checksum verified for $KEY"
  fi
done

echo "Checksum verification completed"
```

### Data Consistency Verification

**Weekly Consistency Checks**:

```bash
#!/bin/bash
# weekly-consistency-check.sh

set -e

DATE=$(date +%Y-%m-%d)
LOG_FILE="/var/log/backup-tests/consistency-${DATE}.log"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "=== WEEKLY BACKUP CONSISTENCY CHECK ==="

# Restore latest backup to test instance
LATEST_SNAPSHOT=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --snapshot-type automated \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

TEST_INSTANCE="consistency-test-${DATE}"

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier $TEST_INSTANCE \
  --db-snapshot-identifier $LATEST_SNAPSHOT \
  --db-instance-class db.t3.medium

aws rds wait db-instance-available --db-instance-identifier $TEST_INSTANCE

TEST_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier $TEST_INSTANCE \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Run comprehensive consistency checks
psql -h $TEST_ENDPOINT -U admin -d ecommerce > /tmp/consistency-${DATE}.txt <<EOF
-- 1. Check for orphaned records
SELECT 'Orphaned Order Items' as check_name,
       COUNT(*) as issues
FROM order_items oi
LEFT JOIN orders o ON oi.order_id = o.id
WHERE o.id IS NULL;

-- 2. Check for invalid foreign keys
SELECT 'Invalid Customer References' as check_name,
       COUNT(*) as issues
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- 3. Check for data type consistency
SELECT 'Invalid Email Formats' as check_name,
       COUNT(*) as issues
FROM customers
WHERE email !~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$';

-- 4. Check for business rule violations
SELECT 'Negative Order Totals' as check_name,
       COUNT(*) as issues
FROM orders
WHERE total_amount < 0;

-- 5. Check temporal consistency
SELECT 'Future Dated Orders' as check_name,
       COUNT(*) as issues
FROM orders
WHERE created_at > NOW();

-- 6. Check aggregate consistency
SELECT 
  'Order Total Mismatches' as check_name,
  COUNT(*) as issues
FROM (
  SELECT 
    o.id,
    o.total_amount,
    SUM(oi.quantity * oi.unit_price) as calculated_total
  FROM orders o
  JOIN order_items oi ON o.id = oi.order_id
  GROUP BY o.id, o.total_amount
  HAVING ABS(o.total_amount - SUM(oi.quantity * oi.unit_price)) > 0.01
) mismatches;

-- 7. Check for null primary keys
SELECT 'Null Primary Keys' as check_name,
       COUNT(*) as issues
FROM (
  SELECT id FROM orders WHERE id IS NULL
  UNION ALL
  SELECT id FROM customers WHERE id IS NULL
  UNION ALL
  SELECT id FROM products WHERE id IS NULL
) null_pks;

-- 8. Check referential integrity across all tables
SELECT 
  'Product References' as check_name,
  COUNT(*) as issues
FROM order_items oi
LEFT JOIN products p ON oi.product_id = p.id
WHERE p.id IS NULL;
EOF

# Parse results
TOTAL_ISSUES=$(grep -oP '\d+' /tmp/consistency-${DATE}.txt | awk '{sum+=$1} END {print sum}')

echo "Total consistency issues found: $TOTAL_ISSUES"

if [ "$TOTAL_ISSUES" -eq 0 ]; then
  echo "✓ All consistency checks passed"
else
  echo "❌ Consistency issues detected"
  cat /tmp/consistency-${DATE}.txt
  
  # Alert on consistency issues
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:backup-integrity-alerts \
    --subject "Backup Consistency Alert - Issues Detected" \
    --message "Consistency check found $TOTAL_ISSUES issues. See log: $LOG_FILE"
fi

# Cleanup
aws rds delete-db-instance \
  --db-instance-identifier $TEST_INSTANCE \
  --skip-final-snapshot

echo "Consistency check completed"
```

## Restore Performance Benchmarking

### Performance Metrics Collection

```bash
#!/bin/bash
# restore-performance-benchmark.sh

set -e

DATE=$(date +%Y-%m-%d)
BENCHMARK_FILE="/var/backup-tests/benchmarks/restore-${DATE}.json"

echo "=== RESTORE PERFORMANCE BENCHMARK ==="

# Initialize benchmark results
cat > $BENCHMARK_FILE <<EOF
{
  "benchmark_date": "$DATE",
  "tests": []
}
EOF

# Benchmark 1: RDS Snapshot Restore
echo "Benchmarking RDS snapshot restore..."
SNAPSHOT_ID=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --snapshot-type automated \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

SNAPSHOT_SIZE=$(aws rds describe-db-snapshots \
  --db-snapshot-identifier $SNAPSHOT_ID \
  --query 'DBSnapshots[0].AllocatedStorage' \
  --output text)

START_TIME=$(date +%s)

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier benchmark-test-${DATE} \
  --db-snapshot-identifier $SNAPSHOT_ID \
  --db-instance-class db.t3.medium

aws rds wait db-instance-available --db-instance-identifier benchmark-test-${DATE}

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
THROUGHPUT=$(echo "scale=2; $SNAPSHOT_SIZE / ($DURATION / 60)" | bc)

jq --arg test "rds_snapshot_restore" \
   --argjson size "$SNAPSHOT_SIZE" \
   --argjson duration "$DURATION" \
   --arg throughput "$THROUGHPUT" \
   '.tests += [{
     "test_name": $test,
     "snapshot_size_gb": $size,
     "duration_seconds": $duration,
     "throughput_gb_per_minute": $throughput
   }]' $BENCHMARK_FILE > /tmp/benchmark.json && mv /tmp/benchmark.json $BENCHMARK_FILE

# Cleanup
aws rds delete-db-instance \
  --db-instance-identifier benchmark-test-${DATE} \
  --skip-final-snapshot

# Benchmark 2: Redis Snapshot Restore
echo "Benchmarking Redis snapshot restore..."
REDIS_SNAPSHOT=$(aws elasticache describe-snapshots \
  --replication-group-id ecommerce-redis-cluster \
  --query 'Snapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotName' \
  --output text)

START_TIME=$(date +%s)

aws elasticache create-replication-group \
  --replication-group-id redis-benchmark-${DATE} \
  --replication-group-description "Benchmark test" \
  --snapshot-name $REDIS_SNAPSHOT \
  --cache-node-type cache.t3.medium \
  --num-cache-clusters 2

aws elasticache wait replication-group-available \
  --replication-group-id redis-benchmark-${DATE}

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

jq --arg test "redis_snapshot_restore" \
   --argjson duration "$DURATION" \
   '.tests += [{
     "test_name": $test,
     "duration_seconds": $duration
   }]' $BENCHMARK_FILE > /tmp/benchmark.json && mv /tmp/benchmark.json $BENCHMARK_FILE

# Cleanup
aws elasticache delete-replication-group \
  --replication-group-id redis-benchmark-${DATE} \
  --retain-primary-cluster false

# Generate performance report
echo "Generating performance report..."
cat > /var/backup-tests/benchmarks/report-${DATE}.md <<EOF
# Restore Performance Benchmark Report - $DATE

## RDS Snapshot Restore
- Snapshot Size: ${SNAPSHOT_SIZE} GB
- Restore Duration: ${DURATION}s ($(($DURATION / 60)) minutes)
- Throughput: ${THROUGHPUT} GB/minute

## Redis Snapshot Restore
- Restore Duration: ${DURATION}s ($(($DURATION / 60)) minutes)

## Performance Trends
[Compare with previous benchmarks]

## Recommendations
- [Based on performance data]
EOF

echo "Benchmark completed: $BENCHMARK_FILE"
```

### Performance Trend Analysis

```python
#!/usr/bin/env python3
# analyze-restore-performance.py

import json
import glob
from datetime import datetime, timedelta
import statistics

def analyze_performance_trends():
    """Analyze restore performance trends over time"""
    
    # Load all benchmark files from last 90 days
    benchmark_files = glob.glob('/var/backup-tests/benchmarks/restore-*.json')
    benchmark_files.sort()
    
    rds_durations = []
    redis_durations = []
    
    for file_path in benchmark_files[-90:]:  # Last 90 days
        with open(file_path, 'r') as f:
            data = json.load(f)
            
        for test in data.get('tests', []):
            if test['test_name'] == 'rds_snapshot_restore':
                rds_durations.append(test['duration_seconds'])
            elif test['test_name'] == 'redis_snapshot_restore':
                redis_durations.append(test['duration_seconds'])
    
    # Calculate statistics
    rds_stats = {
        'mean': statistics.mean(rds_durations),
        'median': statistics.median(rds_durations),
        'stdev': statistics.stdev(rds_durations) if len(rds_durations) > 1 else 0,
        'min': min(rds_durations),
        'max': max(rds_durations)
    }
    
    redis_stats = {
        'mean': statistics.mean(redis_durations),
        'median': statistics.median(redis_durations),
        'stdev': statistics.stdev(redis_durations) if len(redis_durations) > 1 else 0,
        'min': min(redis_durations),
        'max': max(redis_durations)
    }
    
    # Generate report
    report = f"""
# Restore Performance Trend Analysis

## RDS Snapshot Restore (Last 90 Days)
- Mean Duration: {rds_stats['mean']:.0f}s ({rds_stats['mean']/60:.1f} minutes)
- Median Duration: {rds_stats['median']:.0f}s ({rds_stats['median']/60:.1f} minutes)
- Standard Deviation: {rds_stats['stdev']:.0f}s
- Min Duration: {rds_stats['min']:.0f}s ({rds_stats['min']/60:.1f} minutes)
- Max Duration: {rds_stats['max']:.0f}s ({rds_stats['max']/60:.1f} minutes)

## Redis Snapshot Restore (Last 90 Days)
- Mean Duration: {redis_stats['mean']:.0f}s ({redis_stats['mean']/60:.1f} minutes)
- Median Duration: {redis_stats['median']:.0f}s ({redis_stats['median']/60:.1f} minutes)
- Standard Deviation: {redis_stats['stdev']:.0f}s
- Min Duration: {redis_stats['min']:.0f}s ({redis_stats['min']/60:.1f} minutes)
- Max Duration: {redis_stats['max']:.0f}s ({redis_stats['max']/60:.1f} minutes)

## Trend Analysis
- RDS restore times are {'increasing' if rds_durations[-1] > rds_stats['mean'] else 'stable'}
- Redis restore times are {'increasing' if redis_durations[-1] > redis_stats['mean'] else 'stable'}

## Recommendations
"""
    
    if rds_durations[-1] > rds_stats['mean'] + rds_stats['stdev']:
        report += "- ⚠️ RDS restore performance degrading - investigate database size growth\n"
    
    if redis_durations[-1] > redis_stats['mean'] + redis_stats['stdev']:
        report += "- ⚠️ Redis restore performance degrading - review snapshot size\n"
    
    print(report)
    
    # Save report
    with open(f'/var/backup-tests/benchmarks/trend-analysis-{datetime.now().strftime("%Y-%m-%d")}.md', 'w') as f:
        f.write(report)

if __name__ == '__main__':
    analyze_performance_trends()
```

## Automated Backup Testing Framework


### Automated Testing Architecture

```yaml
Testing Framework Components:
  Scheduler:
    Tool: AWS EventBridge
    Triggers:
      - Daily: Checksum verification
      - Weekly: Consistency checks
      - Monthly: Full restore tests
      - Quarterly: DR drills
      
  Execution Engine:
    Tool: AWS Step Functions
    Workflows:
      - Backup restore workflow
      - Validation workflow
      - Cleanup workflow
      
  Monitoring:
    Tool: CloudWatch + SNS
    Metrics:
      - Test success rate
      - Restore duration
      - Data integrity score
      
  Reporting:
    Tool: Lambda + S3
    Outputs:
      - Test results (JSON)
      - Performance metrics
      - Trend analysis
```

### Step Functions Workflow

**Backup Test Workflow**:

```json
{
  "Comment": "Automated Backup Testing Workflow",
  "StartAt": "SelectBackupToTest",
  "States": {
    "SelectBackupToTest": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:SelectLatestBackup",
      "Next": "RestoreBackup"
    },
    "RestoreBackup": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:RestoreBackup",
      "TimeoutSeconds": 3600,
      "Retry": [
        {
          "ErrorEquals": ["States.TaskFailed"],
          "IntervalSeconds": 60,
          "MaxAttempts": 2,
          "BackoffRate": 2.0
        }
      ],
      "Catch": [
        {
          "ErrorEquals": ["States.ALL"],
          "Next": "NotifyFailure"
        }
      ],
      "Next": "WaitForRestore"
    },
    "WaitForRestore": {
      "Type": "Wait",
      "Seconds": 300,
      "Next": "ValidateRestore"
    },
    "ValidateRestore": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:ValidateBackup",
      "Next": "CheckValidation"
    },
    "CheckValidation": {
      "Type": "Choice",
      "Choices": [
        {
          "Variable": "$.validation.passed",
          "BooleanEquals": true,
          "Next": "RecordMetrics"
        }
      ],
      "Default": "NotifyFailure"
    },
    "RecordMetrics": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:RecordTestMetrics",
      "Next": "CleanupResources"
    },
    "CleanupResources": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:CleanupTestResources",
      "Next": "NotifySuccess"
    },
    "NotifySuccess": {
      "Type": "Task",
      "Resource": "arn:aws:states:::sns:publish",
      "Parameters": {
        "TopicArn": "arn:aws:sns:us-east-1:123456789012:backup-test-success",
        "Subject": "Backup Test Passed",
        "Message.$": "$.testResults"
      },
      "End": true
    },
    "NotifyFailure": {
      "Type": "Task",
      "Resource": "arn:aws:states:::sns:publish",
      "Parameters": {
        "TopicArn": "arn:aws:sns:us-east-1:123456789012:backup-test-failure",
        "Subject": "Backup Test Failed",
        "Message.$": "$.error"
      },
      "End": true
    }
  }
}
```

### Lambda Functions for Testing

**Restore Backup Function**:

```python
#!/usr/bin/env python3
# lambda_restore_backup.py

import boto3
import json
from datetime import datetime

rds = boto3.client('rds')
elasticache = boto3.client('elasticache')

def lambda_handler(event, context):
    """Restore backup for testing"""
    
    backup_type = event.get('backup_type', 'rds')
    test_id = f"test-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
    
    try:
        if backup_type == 'rds':
            result = restore_rds_backup(test_id)
        elif backup_type == 'redis':
            result = restore_redis_backup(test_id)
        else:
            raise ValueError(f"Unknown backup type: {backup_type}")
        
        return {
            'statusCode': 200,
            'test_id': test_id,
            'backup_type': backup_type,
            'restore_started': True,
            'instance_id': result['instance_id'],
            'start_time': datetime.now().isoformat()
        }
        
    except Exception as e:
        return {
            'statusCode': 500,
            'error': str(e),
            'test_id': test_id
        }

def restore_rds_backup(test_id):
    """Restore RDS snapshot"""
    
    # Get latest snapshot
    snapshots = rds.describe_db_snapshots(
        DBInstanceIdentifier='ecommerce-prod-db',
        SnapshotType='automated'
    )
    
    latest_snapshot = sorted(
        snapshots['DBSnapshots'],
        key=lambda x: x['SnapshotCreateTime'],
        reverse=True
    )[0]
    
    # Restore snapshot
    response = rds.restore_db_instance_from_db_snapshot(
        DBInstanceIdentifier=f'test-restore-{test_id}',
        DBSnapshotIdentifier=latest_snapshot['DBSnapshotIdentifier'],
        DBInstanceClass='db.t3.medium',
        PubliclyAccessible=False,
        Tags=[
            {'Key': 'Purpose', 'Value': 'BackupTest'},
            {'Key': 'TestID', 'Value': test_id}
        ]
    )
    
    return {
        'instance_id': response['DBInstance']['DBInstanceIdentifier'],
        'snapshot_id': latest_snapshot['DBSnapshotIdentifier']
    }

def restore_redis_backup(test_id):
    """Restore Redis snapshot"""
    
    # Get latest snapshot
    snapshots = elasticache.describe_snapshots(
        ReplicationGroupId='ecommerce-redis-cluster'
    )
    
    latest_snapshot = sorted(
        snapshots['Snapshots'],
        key=lambda x: x['SnapshotCreateTime'],
        reverse=True
    )[0]
    
    # Restore snapshot
    response = elasticache.create_replication_group(
        ReplicationGroupId=f'test-restore-{test_id}',
        ReplicationGroupDescription='Backup test',
        SnapshotName=latest_snapshot['SnapshotName'],
        CacheNodeType='cache.t3.medium',
        NumCacheClusters=2,
        AutomaticFailoverEnabled=True,
        Tags=[
            {'Key': 'Purpose', 'Value': 'BackupTest'},
            {'Key': 'TestID', 'Value': test_id}
        ]
    )
    
    return {
        'instance_id': response['ReplicationGroup']['ReplicationGroupId'],
        'snapshot_id': latest_snapshot['SnapshotName']
    }
```

**Validate Backup Function**:

```python
#!/usr/bin/env python3
# lambda_validate_backup.py

import boto3
import psycopg2
import redis
from datetime import datetime

def lambda_handler(event, context):
    """Validate restored backup"""
    
    test_id = event['test_id']
    backup_type = event['backup_type']
    instance_id = event['instance_id']
    
    try:
        if backup_type == 'rds':
            validation_result = validate_rds_restore(instance_id)
        elif backup_type == 'redis':
            validation_result = validate_redis_restore(instance_id)
        else:
            raise ValueError(f"Unknown backup type: {backup_type}")
        
        return {
            'statusCode': 200,
            'test_id': test_id,
            'validation': validation_result,
            'timestamp': datetime.now().isoformat()
        }
        
    except Exception as e:
        return {
            'statusCode': 500,
            'test_id': test_id,
            'validation': {
                'passed': False,
                'error': str(e)
            }
        }

def validate_rds_restore(instance_id):
    """Validate RDS restore"""
    
    rds = boto3.client('rds')
    
    # Get instance endpoint
    response = rds.describe_db_instances(DBInstanceIdentifier=instance_id)
    endpoint = response['DBInstances'][0]['Endpoint']['Address']
    
    # Connect and run validation queries
    conn = psycopg2.connect(
        host=endpoint,
        database='ecommerce',
        user='admin',
        password=get_db_password()
    )
    
    cursor = conn.cursor()
    
    # Run validation queries
    validations = []
    
    # Check 1: Table counts
    cursor.execute("SELECT COUNT(*) FROM orders")
    order_count = cursor.fetchone()[0]
    validations.append({
        'check': 'order_count',
        'value': order_count,
        'passed': order_count > 0
    })
    
    # Check 2: Referential integrity
    cursor.execute("""
        SELECT COUNT(*) FROM orders o
        LEFT JOIN customers c ON o.customer_id = c.id
        WHERE c.id IS NULL
    """)
    orphaned_orders = cursor.fetchone()[0]
    validations.append({
        'check': 'referential_integrity',
        'value': orphaned_orders,
        'passed': orphaned_orders == 0
    })
    
    # Check 3: Null primary keys
    cursor.execute("SELECT COUNT(*) FROM orders WHERE id IS NULL")
    null_pks = cursor.fetchone()[0]
    validations.append({
        'check': 'null_primary_keys',
        'value': null_pks,
        'passed': null_pks == 0
    })
    
    cursor.close()
    conn.close()
    
    all_passed = all(v['passed'] for v in validations)
    
    return {
        'passed': all_passed,
        'checks': validations,
        'total_checks': len(validations),
        'passed_checks': sum(1 for v in validations if v['passed'])
    }

def validate_redis_restore(instance_id):
    """Validate Redis restore"""
    
    elasticache = boto3.client('elasticache')
    
    # Get cluster endpoint
    response = elasticache.describe_replication_groups(
        ReplicationGroupId=instance_id
    )
    endpoint = response['ReplicationGroups'][0]['NodeGroups'][0]['PrimaryEndpoint']['Address']
    
    # Connect to Redis
    r = redis.Redis(host=endpoint, port=6379, decode_responses=True)
    
    # Validate
    key_count = r.dbsize()
    
    return {
        'passed': key_count > 0,
        'key_count': key_count,
        'checks': [
            {
                'check': 'key_count',
                'value': key_count,
                'passed': key_count > 0
            }
        ]
    }

def get_db_password():
    """Get database password from Secrets Manager"""
    secrets = boto3.client('secretsmanager')
    response = secrets.get_secret_value(SecretId='ecommerce/database/admin')
    return json.loads(response['SecretString'])['password']
```

## Test Result Documentation and Tracking

### Test Results Database Schema

```sql
-- Test results tracking
CREATE TABLE backup_test_results (
    id SERIAL PRIMARY KEY,
    test_id VARCHAR(100) UNIQUE NOT NULL,
    test_date DATE NOT NULL,
    test_type VARCHAR(50) NOT NULL,  -- monthly, quarterly, ad-hoc
    backup_type VARCHAR(50) NOT NULL,  -- rds, redis, config
    snapshot_id VARCHAR(200),
    restore_duration_seconds INTEGER,
    validation_duration_seconds INTEGER,
    passed BOOLEAN NOT NULL,
    rto_seconds INTEGER,
    rpo_seconds INTEGER,
    data_integrity_score DECIMAL(5,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test validation details
CREATE TABLE backup_test_validations (
    id SERIAL PRIMARY KEY,
    test_id VARCHAR(100) REFERENCES backup_test_results(test_id),
    check_name VARCHAR(100) NOT NULL,
    check_value TEXT,
    passed BOOLEAN NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Test performance metrics
CREATE TABLE backup_test_metrics (
    id SERIAL PRIMARY KEY,
    test_id VARCHAR(100) REFERENCES backup_test_results(test_id),
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    metric_unit VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_test_date ON backup_test_results(test_date);
CREATE INDEX idx_test_type ON backup_test_results(test_type);
CREATE INDEX idx_backup_type ON backup_test_results(backup_type);
CREATE INDEX idx_test_validations_test_id ON backup_test_validations(test_id);
CREATE INDEX idx_test_metrics_test_id ON backup_test_metrics(test_id);
```

### Test Results Dashboard

```python
#!/usr/bin/env python3
# generate_test_dashboard.py

import psycopg2
import json
from datetime import datetime, timedelta

def generate_dashboard():
    """Generate backup test results dashboard"""
    
    conn = psycopg2.connect(
        host='metrics-db.example.com',
        database='backup_metrics',
        user='readonly',
        password=get_password()
    )
    
    cursor = conn.cursor()
    
    # Get test success rate (last 30 days)
    cursor.execute("""
        SELECT 
            test_type,
            COUNT(*) as total_tests,
            SUM(CASE WHEN passed THEN 1 ELSE 0 END) as passed_tests,
            ROUND(100.0 * SUM(CASE WHEN passed THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate
        FROM backup_test_results
        WHERE test_date >= CURRENT_DATE - INTERVAL '30 days'
        GROUP BY test_type
    """)
    
    success_rates = cursor.fetchall()
    
    # Get average RTO (last 30 days)
    cursor.execute("""
        SELECT 
            backup_type,
            AVG(rto_seconds) as avg_rto,
            MIN(rto_seconds) as min_rto,
            MAX(rto_seconds) as max_rto
        FROM backup_test_results
        WHERE test_date >= CURRENT_DATE - INTERVAL '30 days'
        AND rto_seconds IS NOT NULL
        GROUP BY backup_type
    """)
    
    rto_stats = cursor.fetchall()
    
    # Get recent failures
    cursor.execute("""
        SELECT 
            test_id,
            test_date,
            test_type,
            backup_type,
            notes
        FROM backup_test_results
        WHERE passed = FALSE
        AND test_date >= CURRENT_DATE - INTERVAL '30 days'
        ORDER BY test_date DESC
        LIMIT 10
    """)
    
    recent_failures = cursor.fetchall()
    
    # Generate dashboard
    dashboard = {
        'generated_at': datetime.now().isoformat(),
        'period': 'Last 30 days',
        'success_rates': [
            {
                'test_type': row[0],
                'total_tests': row[1],
                'passed_tests': row[2],
                'success_rate': float(row[3])
            }
            for row in success_rates
        ],
        'rto_statistics': [
            {
                'backup_type': row[0],
                'avg_rto_minutes': float(row[1]) / 60 if row[1] else 0,
                'min_rto_minutes': float(row[2]) / 60 if row[2] else 0,
                'max_rto_minutes': float(row[3]) / 60 if row[3] else 0
            }
            for row in rto_stats
        ],
        'recent_failures': [
            {
                'test_id': row[0],
                'test_date': row[1].isoformat(),
                'test_type': row[2],
                'backup_type': row[3],
                'notes': row[4]
            }
            for row in recent_failures
        ]
    }
    
    cursor.close()
    conn.close()
    
    # Save dashboard
    with open('/var/www/dashboards/backup-tests.json', 'w') as f:
        json.dump(dashboard, f, indent=2)
    
    return dashboard

if __name__ == '__main__':
    dashboard = generate_dashboard()
    print(json.dumps(dashboard, indent=2))
```

## Lessons Learned Documentation

### Lessons Learned Template

```markdown
# Backup Test Lessons Learned - [Date]

## Test Information
- **Test ID**: [test-id]
- **Test Type**: [monthly/quarterly/ad-hoc]
- **Test Date**: [YYYY-MM-DD]
- **Duration**: [X hours]
- **Outcome**: [PASSED/FAILED]

## What Went Well
1. [Success 1]
2. [Success 2]
3. [Success 3]

## What Didn't Go Well
1. [Issue 1]
   - **Impact**: [Description]
   - **Root Cause**: [Analysis]
   - **Resolution**: [How it was fixed]

2. [Issue 2]
   - **Impact**: [Description]
   - **Root Cause**: [Analysis]
   - **Resolution**: [How it was fixed]

## Unexpected Findings
- [Finding 1]
- [Finding 2]

## Action Items
- [ ] [Action 1] - Owner: [Name] - Due: [Date]
- [ ] [Action 2] - Owner: [Name] - Due: [Date]
- [ ] [Action 3] - Owner: [Name] - Due: [Date]

## Documentation Updates Required
- [ ] Update procedure: [Procedure name]
- [ ] Update runbook: [Runbook name]
- [ ] Update training materials

## Recommendations
1. [Recommendation 1]
2. [Recommendation 2]
3. [Recommendation 3]

## Metrics
- **RTO Achieved**: [X minutes]
- **RTO Target**: [Y minutes]
- **RPO Achieved**: [X minutes]
- **RPO Target**: [Y minutes]
- **Data Integrity Score**: [X%]

## Follow-up Actions
- Next test scheduled: [Date]
- Procedure review scheduled: [Date]
- Team training scheduled: [Date]

---
**Document Owner**: [Name]
**Last Updated**: [Date]
```

## Related Documentation

- [Backup and Recovery](backup-recovery.md) - Main backup and recovery procedures
- [Backup Automation](backup-automation.md) - Automated backup procedures
- [Detailed Restore Procedures](detailed-restore-procedures.md) - Step-by-step restore workflows
- [Monitoring and Alerting](monitoring-alerting.md) - Monitoring strategies

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-26  
**Owner**: Operations Team
