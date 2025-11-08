# Database Backup Procedures

## RDS Automated Backup Configuration (Detailed)

### Automated Backup Settings

**Primary Database Configuration**:

```yaml
Database Instance: ecommerce-prod-db
Instance Class: db.r5.2xlarge
Engine: PostgreSQL 14.7
Multi-AZ: Enabled

Automated Backup Configuration:
  BackupRetentionPeriod: 30 days
  PreferredBackupWindow: "03:00-04:00" UTC
  BackupType: Automated snapshots
  StorageType: EBS snapshots
  Encryption: 
    Enabled: true
    KMSKeyId: arn:aws:kms:us-east-1:123456789012:key/backup-key
    Algorithm: AES-256
  
  CopyTagsToSnapshot: true
  Tags:

    - Key: Environment

      Value: production

    - Key: BackupType

      Value: automated

    - Key: Compliance

      Value: required
```

### Backup Window Optimization

**Selecting Optimal Backup Window**:

```bash
# Analyze database activity patterns
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name DatabaseConnections \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-prod-db \
  --start-time 2025-10-16T00:00:00Z \
  --end-time 2025-10-23T00:00:00Z \
  --period 3600 \
  --statistics Average,Maximum \
  --output table

# Identify low-activity periods for backup window
# Recommended: 03:00-04:00 UTC (off-peak hours)
```

**Modifying Backup Window**:

```bash
# Update backup window
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-prod-db \
  --preferred-backup-window "03:00-04:00" \
  --apply-immediately

# Verify configuration
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBInstances[0].PreferredBackupWindow'
```

## Manual Snapshot Procedures

### Creating Manual Snapshots

**Pre-Deployment Snapshot**:

```bash
#!/bin/bash
# create-pre-deployment-snapshot.sh

set -e

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
SNAPSHOT_ID="ecommerce-prod-db-pre-deploy-${TIMESTAMP}"
DB_INSTANCE="ecommerce-prod-db"

echo "Creating pre-deployment snapshot: ${SNAPSHOT_ID}"

# Create snapshot
aws rds create-db-snapshot \
  --db-instance-identifier ${DB_INSTANCE} \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --tags \
    Key=Purpose,Value=pre-deployment \
    Key=CreatedBy,Value=${USER} \
    Key=Timestamp,Value=${TIMESTAMP} \
    Key=Environment,Value=production

# Wait for snapshot to complete
echo "Waiting for snapshot to complete..."
aws rds wait db-snapshot-completed \
  --db-snapshot-identifier ${SNAPSHOT_ID}

# Verify snapshot
SNAPSHOT_STATUS=$(aws rds describe-db-snapshots \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --query 'DBSnapshots[0].Status' \
  --output text)

if [ "${SNAPSHOT_STATUS}" == "available" ]; then
  echo "✓ Snapshot created successfully: ${SNAPSHOT_ID}"
  echo "Snapshot ARN: $(aws rds describe-db-snapshots \
    --db-snapshot-identifier ${SNAPSHOT_ID} \
    --query 'DBSnapshots[0].DBSnapshotArn' \
    --output text)"
else
  echo "✗ Snapshot creation failed with status: ${SNAPSHOT_STATUS}"
  exit 1
fi
```

**Pre-Migration Snapshot**:

```bash
#!/bin/bash
# create-pre-migration-snapshot.sh

MIGRATION_NAME=$1
if [ -z "$MIGRATION_NAME" ]; then
  echo "Usage: $0 <migration-name>"
  exit 1
fi

SNAPSHOT_ID="ecommerce-prod-db-pre-migration-${MIGRATION_NAME}-$(date +%Y%m%d)"

# Create snapshot with migration context
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --tags \
    Key=Purpose,Value=pre-migration \
    Key=Migration,Value=${MIGRATION_NAME} \
    Key=CreatedBy,Value=${USER} \
    Key=Date,Value=$(date +%Y-%m-%d)

echo "Pre-migration snapshot created: ${SNAPSHOT_ID}"
```

### Manual Snapshot Best Practices

**Naming Conventions**:

```yaml
Format: {instance}-{purpose}-{identifier}-{date}

Examples:

  - ecommerce-prod-db-pre-deploy-20251023-143000
  - ecommerce-prod-db-pre-migration-v2.5-20251023
  - ecommerce-prod-db-manual-backup-20251023
  - ecommerce-prod-db-compliance-q4-20251023

Components:
  instance: Database instance identifier
  purpose: pre-deploy | pre-migration | manual-backup | compliance
  identifier: Deployment version, migration name, or quarter
  date: YYYYMMDD or YYYYMMDD-HHMMSS
```

**Tagging Strategy**:

```yaml
Required Tags:

  - Purpose: Reason for snapshot creation
  - CreatedBy: User or automation system
  - Environment: production | staging | development
  - Date: Creation date (YYYY-MM-DD)

Optional Tags:

  - Migration: Migration name (for pre-migration snapshots)
  - Version: Application version (for pre-deployment snapshots)
  - Ticket: JIRA ticket number
  - Compliance: Compliance requirement (SOC2, PCI-DSS, GDPR)
  - RetentionPeriod: Custom retention period in days

```

## Point-in-Time Recovery (PITR) Procedures

### Understanding PITR

**PITR Capabilities**:

```yaml
Recovery Granularity: 5 minutes
Maximum Recovery Window: 30 days (based on retention period)
Recovery Method: Transaction log replay
Data Loss: Maximum 5 minutes (RPO)

Supported Scenarios:

  - Accidental data deletion
  - Application bugs causing data corruption
  - Failed deployments or migrations
  - Rollback to specific point before incident

```

### PITR Recovery Process

**Step 1: Identify Recovery Point**:

```bash
#!/bin/bash
# identify-recovery-point.sh

# List recent snapshots to understand available recovery window
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,Status]' \
  --output table

# Check earliest restorable time
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBInstances[0].[LatestRestorableTime,EarliestRestorableTime]' \
  --output table

# Example output:
# Latest Restorable Time: 2025-10-23T14:30:00Z
# Earliest Restorable Time: 2025-09-23T14:30:00Z
```

**Step 2: Perform PITR**:

```bash
#!/bin/bash
# perform-pitr-recovery.sh

RESTORE_TIME=$1  # Format: 2025-10-23T10:30:00Z
TARGET_INSTANCE="ecommerce-prod-db-pitr-$(date +%s)"

if [ -z "$RESTORE_TIME" ]; then
  echo "Usage: $0 <restore-time>"
  echo "Example: $0 2025-10-23T10:30:00Z"
  exit 1
fi

echo "Starting PITR to: ${RESTORE_TIME}"
echo "Target instance: ${TARGET_INSTANCE}"

# Restore to point in time
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ecommerce-prod-db \
  --target-db-instance-identifier ${TARGET_INSTANCE} \
  --restore-time ${RESTORE_TIME} \
  --db-instance-class db.r5.2xlarge \
  --vpc-security-group-ids sg-0123456789abcdef0 \
  --db-subnet-group-name ecommerce-db-subnet-group \
  --publicly-accessible false \
  --tags \
    Key=Purpose,Value=pitr-recovery \
    Key=RestoreTime,Value=${RESTORE_TIME} \
    Key=CreatedBy,Value=${USER}

echo "Waiting for instance to be available..."
aws rds wait db-instance-available \
  --db-instance-identifier ${TARGET_INSTANCE}

# Get endpoint
ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ${TARGET_INSTANCE} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "✓ PITR completed successfully"
echo "Restored instance: ${TARGET_INSTANCE}"
echo "Endpoint: ${ENDPOINT}"
echo ""
echo "Next steps:"
echo "1. Verify data integrity"
echo "2. Test application connectivity"
echo "3. If verified, update application to use new endpoint"
echo "4. Delete old instance after confirmation"
```

**Step 3: Verify Restored Data**:

```bash
#!/bin/bash
# verify-pitr-data.sh

RESTORED_ENDPOINT=$1

if [ -z "$RESTORED_ENDPOINT" ]; then
  echo "Usage: $0 <restored-endpoint>"
  exit 1
fi

echo "Verifying restored database at: ${RESTORED_ENDPOINT}"

# Connect and run verification queries
psql -h ${RESTORED_ENDPOINT} -U admin -d ecommerce <<EOF
-- Check record counts
SELECT 'orders' as table_name, COUNT(*) as count FROM orders
UNION ALL
SELECT 'customers', COUNT(*) FROM customers
UNION ALL
SELECT 'products', COUNT(*) FROM products;

-- Check latest records
SELECT 'Latest Order' as check_type, MAX(created_at) as latest_timestamp
FROM orders
UNION ALL
SELECT 'Latest Customer', MAX(created_at) FROM customers;

-- Verify specific data if incident is known
-- Example: Check if accidentally deleted orders are present
SELECT COUNT(*) as deleted_orders_recovered
FROM orders
WHERE id IN ('order-123', 'order-456', 'order-789');
EOF

echo "✓ Data verification completed"
```

### PITR Use Cases

**Use Case 1: Accidental Data Deletion**:

```bash
# Scenario: Accidentally deleted 100 orders at 10:25 AM
# Recovery: Restore to 10:24 AM (1 minute before deletion)

INCIDENT_TIME="2025-10-23T10:25:00Z"
RECOVERY_TIME="2025-10-23T10:24:00Z"

./perform-pitr-recovery.sh ${RECOVERY_TIME}
```

**Use Case 2: Failed Migration Rollback**:

```bash
# Scenario: Migration started at 02:00 AM, failed at 02:45 AM
# Recovery: Restore to 01:59 AM (before migration)

MIGRATION_START="2025-10-23T02:00:00Z"
RECOVERY_TIME="2025-10-23T01:59:00Z"

./perform-pitr-recovery.sh ${RECOVERY_TIME}
```

**Use Case 3: Application Bug Data Corruption**:

```bash
# Scenario: Bug deployed at 14:00, corrupted data until 14:30
# Recovery: Restore to 13:59 (before bug deployment)

BUG_DEPLOYMENT="2025-10-23T14:00:00Z"
RECOVERY_TIME="2025-10-23T13:59:00Z"

./perform-pitr-recovery.sh ${RECOVERY_TIME}
```

## Cross-Region Backup Replication

### Replication Strategy

**Multi-Region Backup Architecture**:

```yaml
Primary Region: us-east-1

  - Automated snapshots: 30 days retention
  - Manual snapshots: 90 days retention
  - Transaction logs: Continuous

DR Region: us-west-2

  - Replicated automated snapshots: 30 days retention
  - Replicated manual snapshots: 90 days retention
  - Read replica: Real-time replication

Replication Method:

  - Automated: Copy snapshots to DR region daily
  - Manual: Copy critical snapshots immediately
  - Real-time: Multi-AZ read replica in DR region

```

### Automated Cross-Region Snapshot Copy

**Setup Automated Replication**:

```bash
#!/bin/bash
# setup-cross-region-replication.sh

SOURCE_REGION="us-east-1"
TARGET_REGION="us-west-2"
DB_INSTANCE="ecommerce-prod-db"
KMS_KEY_TARGET="arn:aws:kms:us-west-2:123456789012:key/backup-key-dr"

echo "Setting up cross-region snapshot replication"
echo "Source: ${SOURCE_REGION} → Target: ${TARGET_REGION}"

# Enable automated snapshot copying
aws rds modify-db-instance \
  --db-instance-identifier ${DB_INSTANCE} \
  --region ${SOURCE_REGION} \
  --backup-retention-period 30 \
  --apply-immediately

# Note: Automated cross-region copy must be configured via Lambda or EventBridge
# See automation script below
```

**Lambda Function for Automated Copy**:

```python
# lambda-cross-region-snapshot-copy.py
import boto3
import os
from datetime import datetime

def lambda_handler(event, context):
    """
    Automatically copy RDS snapshots to DR region
    Triggered by EventBridge on snapshot creation
    """
    
    source_region = os.environ['SOURCE_REGION']
    target_region = os.environ['TARGET_REGION']
    kms_key_id = os.environ['TARGET_KMS_KEY']
    
    rds_source = boto3.client('rds', region_name=source_region)
    rds_target = boto3.client('rds', region_name=target_region)
    
    # Get snapshot details from event
    snapshot_arn = event['detail']['SourceArn']
    snapshot_id = event['detail']['SourceIdentifier']
    
    # Generate target snapshot ID
    target_snapshot_id = f"{snapshot_id}-dr"
    
    try:
        # Copy snapshot to target region
        response = rds_target.copy_db_snapshot(
            SourceDBSnapshotIdentifier=snapshot_arn,
            TargetDBSnapshotIdentifier=target_snapshot_id,
            KmsKeyId=kms_key_id,
            CopyTags=True,
            Tags=[
                {'Key': 'ReplicatedFrom', 'Value': source_region},
                {'Key': 'ReplicationDate', 'Value': datetime.now().isoformat()},
                {'Key': 'Purpose', 'Value': 'disaster-recovery'}
            ]
        )
        
        print(f"✓ Snapshot copy initiated: {target_snapshot_id}")
        print(f"  Source: {snapshot_arn}")
        print(f"  Target Region: {target_region}")
        
        return {
            'statusCode': 200,
            'body': {
                'message': 'Snapshot copy initiated',
                'targetSnapshotId': target_snapshot_id,
                'targetRegion': target_region
            }
        }
        
    except Exception as e:
        print(f"✗ Error copying snapshot: {str(e)}")
        raise

# EventBridge Rule (CloudFormation/CDK)
"""
Resources:
  SnapshotCopyRule:
    Type: AWS::Events::Rule
    Properties:
      Description: "Trigger Lambda on RDS snapshot creation"
      EventPattern:
        source:

          - aws.rds

        detail-type:

          - "RDS DB Snapshot Event"

        detail:
          EventCategories:

            - creation

          SourceType:

            - SNAPSHOT

      State: ENABLED
      Targets:

        - Arn: !GetAtt SnapshotCopyLambda.Arn

          Id: SnapshotCopyTarget
"""
```

### Manual Cross-Region Copy

**Copy Specific Snapshot**:

```bash
#!/bin/bash
# copy-snapshot-to-dr.sh

SNAPSHOT_ID=$1
SOURCE_REGION="us-east-1"
TARGET_REGION="us-west-2"
KMS_KEY="arn:aws:kms:us-west-2:123456789012:key/backup-key-dr"

if [ -z "$SNAPSHOT_ID" ]; then
  echo "Usage: $0 <snapshot-id>"
  exit 1
fi

# Get source snapshot ARN
SOURCE_ARN=$(aws rds describe-db-snapshots \
  --region ${SOURCE_REGION} \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --query 'DBSnapshots[0].DBSnapshotArn' \
  --output text)

TARGET_SNAPSHOT_ID="${SNAPSHOT_ID}-dr"

echo "Copying snapshot to DR region"
echo "Source: ${SOURCE_ARN}"
echo "Target: ${TARGET_SNAPSHOT_ID} (${TARGET_REGION})"

# Copy snapshot
aws rds copy-db-snapshot \
  --region ${TARGET_REGION} \
  --source-db-snapshot-identifier ${SOURCE_ARN} \
  --target-db-snapshot-identifier ${TARGET_SNAPSHOT_ID} \
  --kms-key-id ${KMS_KEY} \
  --copy-tags \
  --tags \
    Key=ReplicatedFrom,Value=${SOURCE_REGION} \
    Key=SourceSnapshot,Value=${SNAPSHOT_ID} \
    Key=ReplicationDate,Value=$(date +%Y-%m-%d)

# Monitor copy progress
echo "Monitoring copy progress..."
while true; do
  STATUS=$(aws rds describe-db-snapshots \
    --region ${TARGET_REGION} \
    --db-snapshot-identifier ${TARGET_SNAPSHOT_ID} \
    --query 'DBSnapshots[0].Status' \
    --output text 2>/dev/null)
  
  if [ "$STATUS" == "available" ]; then
    echo "✓ Snapshot copy completed: ${TARGET_SNAPSHOT_ID}"
    break
  elif [ "$STATUS" == "failed" ]; then
    echo "✗ Snapshot copy failed"
    exit 1
  else
    echo "  Status: ${STATUS} (waiting...)"
    sleep 30
  fi
done

# Verify copied snapshot
aws rds describe-db-snapshots \
  --region ${TARGET_REGION} \
  --db-snapshot-identifier ${TARGET_SNAPSHOT_ID} \
  --query 'DBSnapshots[0].[DBSnapshotIdentifier,Status,AllocatedStorage,Encrypted]' \
  --output table
```

### Read Replica for Real-Time Replication

**Create Cross-Region Read Replica**:

```bash
#!/bin/bash
# create-cross-region-read-replica.sh

SOURCE_DB="ecommerce-prod-db"
SOURCE_REGION="us-east-1"
TARGET_REGION="us-west-2"
REPLICA_ID="ecommerce-prod-db-replica-dr"

echo "Creating cross-region read replica"
echo "Source: ${SOURCE_DB} (${SOURCE_REGION})"
echo "Replica: ${REPLICA_ID} (${TARGET_REGION})"

# Create read replica in DR region
aws rds create-db-instance-read-replica \
  --db-instance-identifier ${REPLICA_ID} \
  --source-db-instance-identifier arn:aws:rds:${SOURCE_REGION}:123456789012:db:${SOURCE_DB} \
  --region ${TARGET_REGION} \
  --db-instance-class db.r5.2xlarge \
  --publicly-accessible false \
  --vpc-security-group-ids sg-dr-0123456789abcdef0 \
  --db-subnet-group-name ecommerce-db-subnet-group-dr \
  --storage-encrypted \
  --kms-key-id arn:aws:kms:${TARGET_REGION}:123456789012:key/backup-key-dr \
  --tags \
    Key=Purpose,Value=disaster-recovery \
    Key=ReplicaOf,Value=${SOURCE_DB} \
    Key=Region,Value=${TARGET_REGION}

echo "Waiting for replica to be available..."
aws rds wait db-instance-available \
  --region ${TARGET_REGION} \
  --db-instance-identifier ${REPLICA_ID}

# Get replica endpoint
REPLICA_ENDPOINT=$(aws rds describe-db-instances \
  --region ${TARGET_REGION} \
  --db-instance-identifier ${REPLICA_ID} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "✓ Read replica created successfully"
echo "Replica endpoint: ${REPLICA_ENDPOINT}"
echo "Replication lag: Monitor via CloudWatch metric 'ReplicaLag'"
```

**Monitor Replication Lag**:

```bash
#!/bin/bash
# monitor-replication-lag.sh

REPLICA_ID="ecommerce-prod-db-replica-dr"
TARGET_REGION="us-west-2"

# Get current replication lag
aws cloudwatch get-metric-statistics \
  --region ${TARGET_REGION} \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=${REPLICA_ID} \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum \
  --output table

# Alert if lag exceeds threshold
LAG=$(aws cloudwatch get-metric-statistics \
  --region ${TARGET_REGION} \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=${REPLICA_ID} \
  --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --query 'Datapoints[0].Average' \
  --output text)

if (( $(echo "$LAG > 300" | bc -l) )); then
  echo "⚠ WARNING: Replication lag is ${LAG} seconds (threshold: 300s)"
  # Send alert
  aws sns publish \
    --region ${TARGET_REGION} \
    --topic-arn arn:aws:sns:${TARGET_REGION}:123456789012:replication-alerts \
    --subject "High Replication Lag Alert" \
    --message "Replication lag for ${REPLICA_ID} is ${LAG} seconds"
fi
```

## Database Export Procedures

### pg_dump Export

**Full Database Export**:

```bash
#!/bin/bash
# export-database-full.sh

set -e

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
EXPORT_DATE=$(date +%Y%m%d-%H%M%S)
EXPORT_FILE="ecommerce-full-export-${EXPORT_DATE}.sql"
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/pg-dumps/"

echo "Starting full database export"
echo "Database: ${DB_NAME}"
echo "Export file: ${EXPORT_FILE}"

# Export with compression
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --format=custom \
  --compress=9 \
  --verbose \
  --file=${EXPORT_FILE}.dump

# Also create plain SQL for readability
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --format=plain \
  --verbose \
  | gzip -9 > ${EXPORT_FILE}.gz

# Calculate checksums
md5sum ${EXPORT_FILE}.dump > ${EXPORT_FILE}.dump.md5
md5sum ${EXPORT_FILE}.gz > ${EXPORT_FILE}.gz.md5

# Upload to S3
echo "Uploading to S3..."
aws s3 cp ${EXPORT_FILE}.dump ${S3_BUCKET} --storage-class STANDARD
aws s3 cp ${EXPORT_FILE}.gz ${S3_BUCKET} --storage-class STANDARD
aws s3 cp ${EXPORT_FILE}.dump.md5 ${S3_BUCKET}
aws s3 cp ${EXPORT_FILE}.gz.md5 ${S3_BUCKET}

# Get file sizes
DUMP_SIZE=$(du -h ${EXPORT_FILE}.dump | cut -f1)
GZ_SIZE=$(du -h ${EXPORT_FILE}.gz | cut -f1)

echo "✓ Export completed successfully"
echo "  Custom format: ${EXPORT_FILE}.dump (${DUMP_SIZE})"
echo "  Plain SQL (gzipped): ${EXPORT_FILE}.gz (${GZ_SIZE})"
echo "  S3 location: ${S3_BUCKET}"

# Cleanup local files (optional)
# rm ${EXPORT_FILE}.dump ${EXPORT_FILE}.gz ${EXPORT_FILE}.dump.md5 ${EXPORT_FILE}.gz.md5
```

**Schema-Only Export**:

```bash
#!/bin/bash
# export-schema-only.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
EXPORT_FILE="ecommerce-schema-$(date +%Y%m%d).sql"

echo "Exporting database schema only"

PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --schema-only \
  --format=plain \
  --file=${EXPORT_FILE}

# Upload to S3
aws s3 cp ${EXPORT_FILE} s3://ecommerce-backups-prod-us-east-1/schemas/

echo "✓ Schema export completed: ${EXPORT_FILE}"
```

**Table-Specific Export**:

```bash
#!/bin/bash
# export-specific-tables.sh

TABLES=$1  # Comma-separated list: "orders,customers,products"

if [ -z "$TABLES" ]; then
  echo "Usage: $0 <table1,table2,table3>"
  exit 1
fi

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
EXPORT_FILE="ecommerce-tables-$(echo $TABLES | tr ',' '-')-$(date +%Y%m%d).sql.gz"

echo "Exporting tables: ${TABLES}"

# Convert comma-separated list to pg_dump table arguments
TABLE_ARGS=""
IFS=',' read -ra TABLE_ARRAY <<< "$TABLES"
for table in "${TABLE_ARRAY[@]}"; do
  TABLE_ARGS="${TABLE_ARGS} --table=${table}"
done

# Export specific tables
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  ${TABLE_ARGS} \
  --format=plain \
  | gzip -9 > ${EXPORT_FILE}

# Upload to S3
aws s3 cp ${EXPORT_FILE} s3://ecommerce-backups-prod-us-east-1/table-exports/

echo "✓ Table export completed: ${EXPORT_FILE}"
```

### Logical Backup with pg_basebackup

**Base Backup for Replication**:

```bash
#!/bin/bash
# create-base-backup.sh

BACKUP_DIR="/var/backups/postgresql/base-$(date +%Y%m%d-%H%M%S)"
DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_USER="replication_user"

echo "Creating base backup"
echo "Backup directory: ${BACKUP_DIR}"

# Create base backup
PGPASSWORD=${REPLICATION_PASSWORD} pg_basebackup \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --pgdata=${BACKUP_DIR} \
  --format=tar \
  --gzip \
  --compress=9 \
  --checkpoint=fast \
  --progress \
  --verbose

# Create backup manifest
cat > ${BACKUP_DIR}/backup_manifest.txt <<EOF
Backup Date: $(date)
Database Host: ${DB_HOST}
Backup Method: pg_basebackup
Format: tar + gzip
Compression Level: 9
EOF

# Calculate total size
BACKUP_SIZE=$(du -sh ${BACKUP_DIR} | cut -f1)
echo "✓ Base backup completed: ${BACKUP_SIZE}"

# Upload to S3
tar -czf ${BACKUP_DIR}.tar.gz ${BACKUP_DIR}
aws s3 cp ${BACKUP_DIR}.tar.gz s3://ecommerce-backups-prod-us-east-1/base-backups/

echo "✓ Uploaded to S3"
```

### Export Optimization

**Parallel Export for Large Databases**:

```bash
#!/bin/bash
# parallel-export.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
EXPORT_DIR="./exports-$(date +%Y%m%d)"
JOBS=4  # Number of parallel jobs

mkdir -p ${EXPORT_DIR}

echo "Starting parallel export with ${JOBS} jobs"

# Export using directory format for parallel processing
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --format=directory \
  --jobs=${JOBS} \
  --compress=9 \
  --verbose \
  --file=${EXPORT_DIR}

# Create archive
tar -czf ${EXPORT_DIR}.tar.gz ${EXPORT_DIR}

# Upload to S3
aws s3 cp ${EXPORT_DIR}.tar.gz s3://ecommerce-backups-prod-us-east-1/parallel-exports/

echo "✓ Parallel export completed"
```

**Exclude Large Tables**:

```bash
#!/bin/bash
# export-exclude-large-tables.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
EXPORT_FILE="ecommerce-without-logs-$(date +%Y%m%d).sql.gz"

# Exclude audit logs and temporary tables
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --exclude-table=audit_logs \
  --exclude-table=temp_* \
  --exclude-table=session_data \
  --format=plain \
  | gzip -9 > ${EXPORT_FILE}

echo "✓ Export completed (large tables excluded): ${EXPORT_FILE}"
```

## Incremental Backup Strategies

### Transaction Log Shipping

**Continuous Archive Mode**:

```sql
-- Enable WAL archiving (requires restart)
ALTER SYSTEM SET wal_level = 'replica';
ALTER SYSTEM SET archive_mode = 'on';
ALTER SYSTEM SET archive_command = 'aws s3 cp %p s3://ecommerce-backups-prod-us-east-1/wal-archives/%f';
ALTER SYSTEM SET archive_timeout = '300';  -- 5 minutes

-- Verify settings
SELECT name, setting FROM pg_settings 
WHERE name IN ('wal_level', 'archive_mode', 'archive_command', 'archive_timeout');
```

**WAL Archive Management**:

```bash
#!/bin/bash
# manage-wal-archives.sh

S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/wal-archives/"
RETENTION_DAYS=30

echo "Managing WAL archives"

# List old WAL files
aws s3 ls ${S3_BUCKET} --recursive | \
  awk '{print $4}' | \
  while read file; do
    FILE_DATE=$(echo $file | grep -oP '\d{8}')
    if [ ! -z "$FILE_DATE" ]; then
      AGE_DAYS=$(( ($(date +%s) - $(date -d $FILE_DATE +%s)) / 86400 ))
      if [ $AGE_DAYS -gt $RETENTION_DAYS ]; then
        echo "Deleting old WAL file: $file (${AGE_DAYS} days old)"
        aws s3 rm ${S3_BUCKET}${file}
      fi
    fi
  done

echo "✓ WAL archive cleanup completed"
```

### Differential Backups

**Create Differential Backup**:

```bash
#!/bin/bash
# create-differential-backup.sh

LAST_FULL_BACKUP="ecommerce-full-20251020"
DIFF_BACKUP_DIR="/var/backups/postgresql/diff-$(date +%Y%m%d-%H%M%S)"
DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"

echo "Creating differential backup"
echo "Base backup: ${LAST_FULL_BACKUP}"

# Get LSN from last full backup
LAST_LSN=$(cat /var/backups/postgresql/${LAST_FULL_BACKUP}/backup_label | \
  grep "CHECKPOINT LOCATION" | awk '{print $3}')

echo "Last checkpoint LSN: ${LAST_LSN}"

# Create differential backup (only changed blocks since last full)
# Note: This requires pg_probackup or similar tool
pg_probackup backup \
  --instance=ecommerce-prod \
  --backup-mode=DELTA \
  --stream \
  --compress-algorithm=zlib \
  --compress-level=9

echo "✓ Differential backup completed"
```

### Incremental Backup Schedule

**Backup Schedule Strategy**:

```yaml
Weekly Schedule:
  Sunday 02:00 UTC:
    Type: Full backup (pg_basebackup)
    Retention: 4 weeks
    Storage: S3 Standard
    
  Monday-Saturday 02:00 UTC:
    Type: Differential backup
    Retention: 7 days
    Storage: S3 Standard
    
  Continuous:
    Type: WAL archiving
    Retention: 30 days
    Storage: S3 Intelligent-Tiering

Recovery Capability:

  - Full restore: From any Sunday backup
  - Point-in-time: Any time within 30 days
  - Incremental restore: Apply differential + WAL logs

```

**Automated Backup Scheduler**:

```bash
#!/bin/bash
# automated-backup-scheduler.sh

DAY_OF_WEEK=$(date +%u)  # 1=Monday, 7=Sunday

if [ "$DAY_OF_WEEK" -eq 7 ]; then
  # Sunday: Full backup
  echo "Running full backup (Sunday)"
  /usr/local/bin/create-full-backup.sh
else
  # Monday-Saturday: Differential backup
  echo "Running differential backup (Day $DAY_OF_WEEK)"
  /usr/local/bin/create-differential-backup.sh
fi

# Always archive WAL logs
/usr/local/bin/archive-wal-logs.sh
```

## Backup Compression and Optimization

### Compression Strategies

**Compression Level Comparison**:

```yaml
Compression Levels (gzip):
  Level 1 (Fast):
    Compression Ratio: 60%
    Speed: Very Fast (100 MB/s)
    CPU Usage: Low
    Use Case: Real-time backups, large databases
    
  Level 6 (Default):
    Compression Ratio: 75%
    Speed: Fast (50 MB/s)
    CPU Usage: Medium
    Use Case: Standard backups
    
  Level 9 (Maximum):
    Compression Ratio: 80%
    Speed: Slow (20 MB/s)
    CPU Usage: High
    Use Case: Long-term storage, compliance backups

Recommendation:

  - Real-time/Automated: Level 6
  - Manual/Archival: Level 9
  - Large databases (>1TB): Level 1-3

```

**Optimized Compression Script**:

```bash
#!/bin/bash
# optimized-compression-backup.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
BACKUP_TYPE=$1  # "fast" or "maximum"

case $BACKUP_TYPE in
  fast)
    COMPRESS_LEVEL=1
    COMPRESS_TOOL="pigz"  # Parallel gzip
    ;;
  maximum)
    COMPRESS_LEVEL=9
    COMPRESS_TOOL="gzip"
    ;;
  *)
    echo "Usage: $0 {fast|maximum}"
    exit 1
    ;;
esac

EXPORT_FILE="ecommerce-backup-$(date +%Y%m%d-%H%M%S).sql"

echo "Creating backup with ${BACKUP_TYPE} compression"
echo "Compression level: ${COMPRESS_LEVEL}"
echo "Compression tool: ${COMPRESS_TOOL}"

# Export and compress in pipeline
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --format=plain \
  | ${COMPRESS_TOOL} -${COMPRESS_LEVEL} > ${EXPORT_FILE}.gz

# Calculate compression ratio
ORIGINAL_SIZE=$(PGPASSWORD=${DB_PASSWORD} psql \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --tuples-only \
  --command="SELECT pg_database_size('${DB_NAME}');" | xargs)

COMPRESSED_SIZE=$(stat -f%z ${EXPORT_FILE}.gz 2>/dev/null || stat -c%s ${EXPORT_FILE}.gz)
RATIO=$(echo "scale=2; (1 - $COMPRESSED_SIZE / $ORIGINAL_SIZE) * 100" | bc)

echo "✓ Backup completed"
echo "  Original size: $(numfmt --to=iec $ORIGINAL_SIZE)"
echo "  Compressed size: $(numfmt --to=iec $COMPRESSED_SIZE)"
echo "  Compression ratio: ${RATIO}%"
```

### Parallel Compression

**Multi-threaded Compression**:

```bash
#!/bin/bash
# parallel-compression-backup.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"
THREADS=4
EXPORT_FILE="ecommerce-backup-$(date +%Y%m%d-%H%M%S).sql"

echo "Creating backup with parallel compression (${THREADS} threads)"

# Use pigz for parallel compression
PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --format=plain \
  | pigz -p ${THREADS} -9 > ${EXPORT_FILE}.gz

echo "✓ Parallel compression completed"
```

### Deduplication

**Identify Duplicate Data**:

```sql
-- Find duplicate rows in tables
SELECT 
  schemaname,
  tablename,
  COUNT(*) as total_rows,
  COUNT(DISTINCT ctid) as unique_rows,
  COUNT(*) - COUNT(DISTINCT ctid) as duplicate_rows
FROM pg_stat_user_tables
GROUP BY schemaname, tablename
HAVING COUNT(*) > COUNT(DISTINCT ctid);

-- Estimate deduplication savings
SELECT 
  pg_size_pretty(SUM(pg_total_relation_size(schemaname||'.'||tablename))) as total_size,
  pg_size_pretty(SUM(pg_total_relation_size(schemaname||'.'||tablename)) * 
    (1 - COUNT(DISTINCT ctid)::float / COUNT(*))) as potential_savings
FROM pg_stat_user_tables
WHERE COUNT(*) > COUNT(DISTINCT ctid);
```

### Backup Size Optimization

**Exclude Unnecessary Data**:

```bash
#!/bin/bash
# optimized-size-backup.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"

# Exclude tables that can be regenerated
EXCLUDE_TABLES=(
  "audit_logs"           # Can be archived separately
  "session_data"         # Temporary data
  "cache_entries"        # Can be rebuilt
  "search_index"         # Can be reindexed
  "temp_*"              # Temporary tables
)

# Build exclude arguments
EXCLUDE_ARGS=""
for table in "${EXCLUDE_TABLES[@]}"; do
  EXCLUDE_ARGS="${EXCLUDE_ARGS} --exclude-table=${table}"
done

echo "Creating optimized backup (excluding ${#EXCLUDE_TABLES[@]} table patterns)"

PGPASSWORD=${DB_PASSWORD} pg_dump \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  ${EXCLUDE_ARGS} \
  --format=custom \
  --compress=9 \
  --file=ecommerce-optimized-$(date +%Y%m%d).dump

echo "✓ Optimized backup completed"
```

**Vacuum Before Backup**:

```bash
#!/bin/bash
# vacuum-before-backup.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
DB_USER="admin"

echo "Running VACUUM FULL to reclaim space before backup"

# Vacuum all tables (run during maintenance window)
PGPASSWORD=${DB_PASSWORD} psql \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --command="VACUUM FULL ANALYZE;"

# Get database size after vacuum
SIZE_AFTER=$(PGPASSWORD=${DB_PASSWORD} psql \
  --host=${DB_HOST} \
  --username=${DB_USER} \
  --dbname=${DB_NAME} \
  --tuples-only \
  --command="SELECT pg_size_pretty(pg_database_size('${DB_NAME}'));")

echo "✓ VACUUM completed"
echo "  Database size: ${SIZE_AFTER}"
echo "  Ready for backup"
```

## Backup Validation and Test Restore Procedures

### Automated Validation

**Daily Backup Validation**:

```bash
#!/bin/bash
# daily-backup-validation.sh

set -e

VALIDATION_LOG="/var/log/backup-validation-$(date +%Y%m%d).log"
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1"

echo "=== Backup Validation Report ===" | tee -a ${VALIDATION_LOG}
echo "Date: $(date)" | tee -a ${VALIDATION_LOG}
echo "" | tee -a ${VALIDATION_LOG}

# 1. Verify RDS snapshots exist
echo "1. Checking RDS snapshots..." | tee -a ${VALIDATION_LOG}
LATEST_SNAPSHOT=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

if [ -z "$LATEST_SNAPSHOT" ]; then
  echo "  ✗ FAILED: No RDS snapshots found" | tee -a ${VALIDATION_LOG}
  exit 1
else
  SNAPSHOT_AGE=$(aws rds describe-db-snapshots \
    --db-snapshot-identifier ${LATEST_SNAPSHOT} \
    --query 'DBSnapshots[0].SnapshotCreateTime' \
    --output text)
  echo "  ✓ Latest snapshot: ${LATEST_SNAPSHOT}" | tee -a ${VALIDATION_LOG}
  echo "    Created: ${SNAPSHOT_AGE}" | tee -a ${VALIDATION_LOG}
fi

# 2. Verify S3 backups exist
echo "" | tee -a ${VALIDATION_LOG}
echo "2. Checking S3 backups..." | tee -a ${VALIDATION_LOG}
BACKUP_COUNT=$(aws s3 ls ${S3_BUCKET}/pg-dumps/ | wc -l)
if [ $BACKUP_COUNT -eq 0 ]; then
  echo "  ✗ FAILED: No S3 backups found" | tee -a ${VALIDATION_LOG}
  exit 1
else
  echo "  ✓ Found ${BACKUP_COUNT} backup files in S3" | tee -a ${VALIDATION_LOG}
fi

# 3. Verify backup file integrity
echo "" | tee -a ${VALIDATION_LOG}
echo "3. Verifying backup file integrity..." | tee -a ${VALIDATION_LOG}
LATEST_BACKUP=$(aws s3 ls ${S3_BUCKET}/pg-dumps/ | sort | tail -n 1 | awk '{print $4}')
if [ ! -z "$LATEST_BACKUP" ]; then
  # Download and verify checksum
  aws s3 cp ${S3_BUCKET}/pg-dumps/${LATEST_BACKUP} /tmp/
  aws s3 cp ${S3_BUCKET}/pg-dumps/${LATEST_BACKUP}.md5 /tmp/
  
  cd /tmp
  if md5sum -c ${LATEST_BACKUP}.md5; then
    echo "  ✓ Checksum verified for ${LATEST_BACKUP}" | tee -a ${VALIDATION_LOG}
  else
    echo "  ✗ FAILED: Checksum mismatch for ${LATEST_BACKUP}" | tee -a ${VALIDATION_LOG}
    exit 1
  fi
  rm /tmp/${LATEST_BACKUP} /tmp/${LATEST_BACKUP}.md5
fi

# 4. Verify cross-region replication
echo "" | tee -a ${VALIDATION_LOG}
echo "4. Checking cross-region replication..." | tee -a ${VALIDATION_LOG}
DR_SNAPSHOT=$(aws rds describe-db-snapshots \
  --region us-west-2 \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

if [ -z "$DR_SNAPSHOT" ]; then
  echo "  ⚠ WARNING: No DR snapshots found in us-west-2" | tee -a ${VALIDATION_LOG}
else
  echo "  ✓ DR snapshot exists: ${DR_SNAPSHOT}" | tee -a ${VALIDATION_LOG}
fi

# 5. Check backup age
echo "" | tee -a ${VALIDATION_LOG}
echo "5. Checking backup freshness..." | tee -a ${VALIDATION_LOG}
SNAPSHOT_TIMESTAMP=$(aws rds describe-db-snapshots \
  --db-snapshot-identifier ${LATEST_SNAPSHOT} \
  --query 'DBSnapshots[0].SnapshotCreateTime' \
  --output text)

SNAPSHOT_AGE_HOURS=$(( ($(date +%s) - $(date -d "$SNAPSHOT_TIMESTAMP" +%s)) / 3600 ))

if [ $SNAPSHOT_AGE_HOURS -gt 24 ]; then
  echo "  ✗ FAILED: Latest snapshot is ${SNAPSHOT_AGE_HOURS} hours old (threshold: 24h)" | tee -a ${VALIDATION_LOG}
  exit 1
else
  echo "  ✓ Latest snapshot is ${SNAPSHOT_AGE_HOURS} hours old" | tee -a ${VALIDATION_LOG}
fi

echo "" | tee -a ${VALIDATION_LOG}
echo "=== Validation Summary ===" | tee -a ${VALIDATION_LOG}
echo "✓ All backup validation checks passed" | tee -a ${VALIDATION_LOG}

# Send report
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:backup-validation \
  --subject "Daily Backup Validation - PASSED" \
  --message file://${VALIDATION_LOG}
```

### Test Restore Procedures

**Weekly Test Restore**:

```bash
#!/bin/bash
# weekly-test-restore.sh

set -e

TEST_INSTANCE="ecommerce-test-restore-$(date +%s)"
VALIDATION_DB="ecommerce_validation"
REPORT_FILE="/var/log/test-restore-$(date +%Y%m%d).log"

echo "=== Weekly Test Restore Report ===" | tee ${REPORT_FILE}
echo "Date: $(date)" | tee -a ${REPORT_FILE}
echo "Test Instance: ${TEST_INSTANCE}" | tee -a ${REPORT_FILE}
echo "" | tee -a ${REPORT_FILE}

# 1. Get latest snapshot
echo "Step 1: Identifying latest snapshot..." | tee -a ${REPORT_FILE}
LATEST_SNAPSHOT=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

echo "  Snapshot: ${LATEST_SNAPSHOT}" | tee -a ${REPORT_FILE}

# 2. Restore snapshot
echo "" | tee -a ${REPORT_FILE}
echo "Step 2: Restoring snapshot..." | tee -a ${REPORT_FILE}
START_TIME=$(date +%s)

aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ${TEST_INSTANCE} \
  --db-snapshot-identifier ${LATEST_SNAPSHOT} \
  --db-instance-class db.t3.medium \
  --no-publicly-accessible \
  --tags Key=Purpose,Value=test-restore Key=AutoDelete,Value=true

# 3. Wait for availability
echo "  Waiting for instance to be available..." | tee -a ${REPORT_FILE}
aws rds wait db-instance-available \
  --db-instance-identifier ${TEST_INSTANCE}

RESTORE_TIME=$(($(date +%s) - START_TIME))
echo "  ✓ Restore completed in ${RESTORE_TIME} seconds" | tee -a ${REPORT_FILE}

# 4. Get endpoint
ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ${TEST_INSTANCE} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "  Endpoint: ${ENDPOINT}" | tee -a ${REPORT_FILE}

# 5. Run data integrity checks
echo "" | tee -a ${REPORT_FILE}
echo "Step 3: Running data integrity checks..." | tee -a ${REPORT_FILE}

PGPASSWORD=${DB_PASSWORD} psql -h ${ENDPOINT} -U admin -d ecommerce > /tmp/integrity-check.txt <<EOF
-- Table counts
\echo '=== Table Counts ==='
SELECT 'orders' as table_name, COUNT(*) as count FROM orders
UNION ALL SELECT 'customers', COUNT(*) FROM customers
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items;

-- Referential integrity
\echo ''
\echo '=== Referential Integrity ==='
SELECT 'Orphaned Orders' as check_name, COUNT(*) as issues
FROM orders o LEFT JOIN customers c ON o.customer_id = c.id WHERE c.id IS NULL
UNION ALL
SELECT 'Orphaned Order Items', COUNT(*) 
FROM order_items oi LEFT JOIN orders o ON oi.order_id = o.id WHERE o.id IS NULL;

-- Data consistency
\echo ''
\echo '=== Data Consistency ==='
SELECT 'Null Primary Keys' as check_name, COUNT(*) as issues FROM orders WHERE id IS NULL
UNION ALL
SELECT 'Invalid Emails', COUNT(*) FROM customers WHERE email !~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$'
UNION ALL
SELECT 'Negative Amounts', COUNT(*) FROM orders WHERE total_amount < 0;

-- Recent data
\echo ''
\echo '=== Recent Data ==='
SELECT 'Orders (24h)' as metric, COUNT(*) as count FROM orders WHERE created_at > NOW() - INTERVAL '24 hours'
UNION ALL
SELECT 'Customers (24h)', COUNT(*) FROM customers WHERE created_at > NOW() - INTERVAL '24 hours';
EOF

cat /tmp/integrity-check.txt | tee -a ${REPORT_FILE}

# 6. Verify specific business data
echo "" | tee -a ${REPORT_FILE}
echo "Step 4: Verifying business data..." | tee -a ${REPORT_FILE}

TOTAL_ORDERS=$(PGPASSWORD=${DB_PASSWORD} psql -h ${ENDPOINT} -U admin -d ecommerce -t -c "SELECT COUNT(*) FROM orders;")
TOTAL_CUSTOMERS=$(PGPASSWORD=${DB_PASSWORD} psql -h ${ENDPOINT} -U admin -d ecommerce -t -c "SELECT COUNT(*) FROM customers;")

echo "  Total Orders: ${TOTAL_ORDERS}" | tee -a ${REPORT_FILE}
echo "  Total Customers: ${TOTAL_CUSTOMERS}" | tee -a ${REPORT_FILE}

# 7. Performance test
echo "" | tee -a ${REPORT_FILE}
echo "Step 5: Running performance test..." | tee -a ${REPORT_FILE}

QUERY_TIME=$(PGPASSWORD=${DB_PASSWORD} psql -h ${ENDPOINT} -U admin -d ecommerce -c "\timing on" -c "SELECT COUNT(*) FROM orders WHERE created_at > NOW() - INTERVAL '7 days';" 2>&1 | grep "Time:" | awk '{print $2}')

echo "  Query execution time: ${QUERY_TIME}" | tee -a ${REPORT_FILE}

# 8. Cleanup
echo "" | tee -a ${REPORT_FILE}
echo "Step 6: Cleaning up test instance..." | tee -a ${REPORT_FILE}

aws rds delete-db-instance \
  --db-instance-identifier ${TEST_INSTANCE} \
  --skip-final-snapshot

echo "  ✓ Test instance deleted" | tee -a ${REPORT_FILE}

# 9. Summary
echo "" | tee -a ${REPORT_FILE}
echo "=== Test Restore Summary ===" | tee -a ${REPORT_FILE}
echo "✓ Snapshot restore: PASSED (${RESTORE_TIME}s)" | tee -a ${REPORT_FILE}
echo "✓ Data integrity: PASSED" | tee -a ${REPORT_FILE}
echo "✓ Business data: VERIFIED" | tee -a ${REPORT_FILE}
echo "✓ Performance: ACCEPTABLE" | tee -a ${REPORT_FILE}
echo "" | tee -a ${REPORT_FILE}
echo "RTO Achieved: ${RESTORE_TIME} seconds (Target: 900s)" | tee -a ${REPORT_FILE}

# Send report
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:backup-validation \
  --subject "Weekly Test Restore - PASSED" \
  --message file://${REPORT_FILE}

echo "✓ Test restore completed successfully"
```

### Restore Verification Checklist

**Post-Restore Verification**:

```yaml
Database Level:

  - [ ] Database is accessible
  - [ ] All schemas present
  - [ ] All tables present
  - [ ] Table row counts match expected values
  - [ ] Indexes are intact
  - [ ] Constraints are enforced
  - [ ] Triggers are functional
  - [ ] Stored procedures exist

Data Integrity:

  - [ ] No orphaned records
  - [ ] Foreign key relationships intact
  - [ ] No null primary keys
  - [ ] Data types are correct
  - [ ] Timestamps are reasonable
  - [ ] No data corruption

Business Logic:

  - [ ] Recent transactions present
  - [ ] Order totals calculate correctly
  - [ ] Customer balances are accurate
  - [ ] Inventory counts are reasonable
  - [ ] Payment records are complete

Performance:

  - [ ] Query performance is acceptable
  - [ ] Index usage is optimal
  - [ ] No blocking queries
  - [ ] Connection pool is healthy
  - [ ] Resource utilization is normal

Application:

  - [ ] Application can connect
  - [ ] Read operations work
  - [ ] Write operations work
  - [ ] Authentication works
  - [ ] Business workflows function

```

### Continuous Validation

**Automated Monitoring**:

```bash
#!/bin/bash
# continuous-backup-monitoring.sh

# Run every hour via cron
# 0 * * * * /usr/local/bin/continuous-backup-monitoring.sh

# Check backup age
LATEST_SNAPSHOT_AGE=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotCreateTime' \
  --output text)

AGE_HOURS=$(( ($(date +%s) - $(date -d "$LATEST_SNAPSHOT_AGE" +%s)) / 3600 ))

if [ $AGE_HOURS -gt 6 ]; then
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:backup-alerts \
    --subject "ALERT: Backup Age Exceeded" \
    --message "Latest backup is ${AGE_HOURS} hours old (threshold: 6 hours)"
fi

# Check backup size anomalies
LATEST_SIZE=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].AllocatedStorage' \
  --output text)

PREVIOUS_SIZE=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-2].AllocatedStorage' \
  --output text)

SIZE_CHANGE=$(echo "scale=2; ($LATEST_SIZE - $PREVIOUS_SIZE) / $PREVIOUS_SIZE * 100" | bc)

if (( $(echo "$SIZE_CHANGE > 20" | bc -l) )); then
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:backup-alerts \
    --subject "ALERT: Backup Size Anomaly" \
    --message "Backup size increased by ${SIZE_CHANGE}% (${PREVIOUS_SIZE}GB → ${LATEST_SIZE}GB)"
fi
```

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-26  
**Owner**: Operations Team, DBA Team
