---
title: "Backup and Recovery"
viewpoint: "Operational"
status: "active"
last_updated: "2025-10-23"
stakeholders: ["Operations Team", "DBA Team", "SRE Team", "Security Team"]
---

# Backup and Recovery

> **Viewpoint**: Operational  
> **Purpose**: Define backup strategies and recovery procedures for the E-Commerce Platform  
> **Audience**: Operations Team, DBA Team, SRE Team, Security Team

## Overview

This document describes backup strategies, recovery procedures, and disaster recovery plans to ensure data protection and business continuity.

## Backup Architecture

### High-Level Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                     Production Environment                       │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   RDS    │  │  Redis   │  │  Kafka   │  │   EKS    │       │
│  │ Database │  │  Cache   │  │ Streams  │  │  Config  │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
└───────┼─────────────┼─────────────┼─────────────┼──────────────┘
        │             │             │             │
        │ Automated   │ Snapshots   │ Replication │ Export
        │ Snapshots   │             │             │
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backup Storage Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   RDS    │  │ElastiCache│ │  Kafka   │  │    S3    │       │
│  │ Snapshots│  │ Snapshots │  │  Backup  │  │  Bucket  │       │
│  │  (EBS)   │  │   (EBS)   │  │ Cluster  │  │ (Config) │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
└───────┼─────────────┼─────────────┼─────────────┼──────────────┘
        │             │             │             │
        │ Copy        │ Export      │ Mirror      │ Lifecycle
        │             │             │             │
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Long-Term Storage (S3)                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  S3 Standard (0-30 days)                                 │  │
│  │  ├─ RDS Snapshots (exported)                            │  │
│  │  ├─ Redis Snapshots                                     │  │
│  │  ├─ Kafka Topic Archives                                │  │
│  │  └─ Configuration Backups                               │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  S3 Intelligent-Tiering (31-90 days)                    │  │
│  │  └─ Automated transition based on access patterns       │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  S3 Glacier Flexible Retrieval (91-365 days)            │  │
│  │  └─ Infrequently accessed backups                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  S3 Glacier Deep Archive (1-7 years)                    │  │
│  │  └─ Compliance and long-term retention                  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
        │
        │ Cross-Region Replication
        ▼
┌─────────────────────────────────────────────────────────────────┐
│              DR Region (us-west-2) - S3 Bucket                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Replicated Backups (Same lifecycle policies)            │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow

**Backup Creation Flow**:

```text

1. Source System → Backup Trigger (Scheduled/Manual)
2. Backup Service → Create Snapshot/Export
3. Snapshot Storage → Native Service Storage (EBS/ElastiCache)
4. Export Process → Copy to S3 (if applicable)
5. S3 Lifecycle → Transition to appropriate storage class
6. Cross-Region Replication → Copy to DR region
7. Verification Service → Validate backup integrity
8. Monitoring → Record metrics and send alerts

```

**Recovery Flow**:

```text

1. Recovery Request → Identify required backup
2. Backup Location → Retrieve from storage tier
3. Restore Service → Create new instance/cluster
4. Data Validation → Verify integrity and completeness
5. Application Update → Point to restored resource
6. Health Check → Verify application functionality
7. Monitoring → Track recovery metrics

```

## Recovery Objectives

### Service Level Targets

| Component | RTO (Recovery Time Objective) | RPO (Recovery Point Objective) | Backup Frequency |
|-----------|-------------------------------|--------------------------------|------------------|
| RDS Database (Primary) | 15 minutes | 5 minutes | Continuous (automated snapshots) |
| RDS Database (Standby) | Immediate (auto-failover) | 0 (synchronous replication) | N/A |
| Redis Cache | 5 minutes | 1 hour | Hourly snapshots |
| Kafka Topics | 30 minutes | 15 minutes | Continuous replication |
| Application Configuration | 10 minutes | 24 hours | Daily |
| Infrastructure (CDK) | 1 hour | 24 hours | Version controlled (Git) |

### Detailed RPO/RTO Analysis

#### RDS Database (Primary)

**RTO: 15 minutes**

- Snapshot identification: 2 minutes
- Instance provisioning: 8 minutes
- Data restoration: 3 minutes
- DNS/connection update: 1 minute
- Health verification: 1 minute

**RPO: 5 minutes**

- Automated snapshots: Every 5 minutes
- Transaction logs: Continuous
- Point-in-time recovery: 5-minute granularity
- Data loss window: Maximum 5 minutes

**Business Impact**:

- Critical: Order processing, payment transactions
- Acceptable data loss: Up to 5 minutes of transactions
- Recovery priority: Highest (P0)

#### RDS Database (Multi-AZ Standby)

**RTO: Immediate (30-120 seconds)**

- Automatic failover detection: 30 seconds
- DNS propagation: 30-60 seconds
- Connection re-establishment: 30 seconds

**RPO: 0 (Zero data loss)**

- Synchronous replication to standby
- All committed transactions replicated
- No data loss during failover

**Business Impact**:

- Transparent to application
- No manual intervention required
- Maintains transaction consistency

#### Redis Cache

**RTO: 5 minutes**

- Snapshot identification: 1 minute
- Cluster provisioning: 2 minutes
- Data restoration: 1 minute
- Application reconnection: 1 minute

**RPO: 1 hour**

- Hourly automated snapshots
- Cache data is non-critical (can be rebuilt)
- Acceptable data loss: Up to 1 hour of cache state

**Business Impact**:

- Medium: Temporary performance degradation
- Cache miss rate increases temporarily
- Application remains functional (slower)
- Recovery priority: Medium (P2)

#### Kafka Topics

**RTO: 30 minutes**

- Failover to backup cluster: 5 minutes
- Consumer group rebalancing: 10 minutes
- Message replay verification: 10 minutes
- Application reconnection: 5 minutes

**RPO: 15 minutes**

- Continuous replication with 15-minute lag
- Message retention: 7 days
- Replay capability from any point

**Business Impact**:

- Medium: Event processing delay
- Messages queued during recovery
- No message loss (retained for replay)
- Recovery priority: Medium (P2)

#### Application Configuration

**RTO: 10 minutes**

- Backup retrieval from S3: 2 minutes
- ConfigMap/Secret application: 3 minutes
- Pod restart and initialization: 4 minutes
- Health check verification: 1 minute

**RPO: 24 hours**

- Daily automated backups
- Configuration changes are infrequent
- Acceptable loss: Up to 24 hours of config changes

**Business Impact**:

- Low: Configuration rarely changes
- Manual reconfiguration possible
- Recovery priority: Low (P3)

#### Infrastructure (CDK)

**RTO: 1 hour**

- Code checkout from Git: 5 minutes
- CDK synthesis: 10 minutes
- Stack deployment: 40 minutes
- Verification and testing: 5 minutes

**RPO: 24 hours**

- Git commits provide version history
- Infrastructure as Code (IaC)
- Point-in-time recovery via Git

**Business Impact**:

- Low: Infrastructure changes are planned
- Rollback via Git history
- Recovery priority: Low (P3)

## Backup Storage Strategy

### S3 Storage Architecture

**Bucket Configuration**:

```yaml
Primary Backup Bucket:
  Name: ecommerce-backups-prod-us-east-1
  Region: us-east-1
  Versioning: Enabled
  Encryption: AES-256 (SSE-S3)
  Access Logging: Enabled
  Object Lock: Enabled (Compliance mode)
  
DR Backup Bucket:
  Name: ecommerce-backups-prod-us-west-2
  Region: us-west-2
  Replication: Cross-region from primary
  Encryption: AES-256 (SSE-S3)
```

### S3 Lifecycle Policies

**Database Backups Lifecycle**:

```json
{
  "Rules": [
    {
      "Id": "database-backup-lifecycle",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "rds-snapshots/"
      },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "INTELLIGENT_TIERING"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER_FLEXIBLE_RETRIEVAL"
        },
        {
          "Days": 365,
          "StorageClass": "DEEP_ARCHIVE"
        }
      ],
      "Expiration": {
        "Days": 2555
      },
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 90
      }
    }
  ]
}
```

**Cache Snapshots Lifecycle**:

```json
{
  "Rules": [
    {
      "Id": "redis-snapshot-lifecycle",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "redis-snapshots/"
      },
      "Transitions": [
        {
          "Days": 7,
          "StorageClass": "INTELLIGENT_TIERING"
        },
        {
          "Days": 30,
          "StorageClass": "GLACIER_FLEXIBLE_RETRIEVAL"
        }
      ],
      "Expiration": {
        "Days": 90
      }
    }
  ]
}
```

**Configuration Backups Lifecycle**:

```json
{
  "Rules": [
    {
      "Id": "config-backup-lifecycle",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "k8s-config/"
      },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "INTELLIGENT_TIERING"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER_FLEXIBLE_RETRIEVAL"
        }
      ],
      "Expiration": {
        "Days": 365
      }
    }
  ]
}
```

**Application Logs Lifecycle**:

```json
{
  "Rules": [
    {
      "Id": "application-logs-lifecycle",
      "Status": "Enabled",
      "Filter": {
        "Prefix": "logs/"
      },
      "Transitions": [
        {
          "Days": 30,
          "StorageClass": "INTELLIGENT_TIERING"
        },
        {
          "Days": 90,
          "StorageClass": "GLACIER_FLEXIBLE_RETRIEVAL"
        },
        {
          "Days": 365,
          "StorageClass": "DEEP_ARCHIVE"
        }
      ],
      "Expiration": {
        "Days": 2555
      }
    }
  ]
}
```

### Storage Class Selection Guide

| Storage Class | Use Case | Retrieval Time | Cost (per GB/month) |
|---------------|----------|----------------|---------------------|
| S3 Standard | Active backups (0-30 days) | Milliseconds | $0.023 |
| S3 Intelligent-Tiering | Unpredictable access (31-90 days) | Milliseconds | $0.0125-$0.023 |
| S3 Glacier Flexible | Infrequent access (91-365 days) | Minutes to hours | $0.0036 |
| S3 Glacier Deep Archive | Long-term compliance (1-7 years) | 12-48 hours | $0.00099 |

### Cross-Region Replication

**Replication Configuration**:

```yaml
Replication Rule:
  Source Bucket: ecommerce-backups-prod-us-east-1
  Destination Bucket: ecommerce-backups-prod-us-west-2
  
  Replication Criteria:

    - All objects with prefix: rds-snapshots/
    - All objects with prefix: redis-snapshots/
    - All objects with prefix: k8s-config/
    - All objects with prefix: kafka-archives/
  
  Replication Options:

    - Replicate delete markers: No
    - Replicate encrypted objects: Yes
    - Replicate object metadata: Yes
    - Replicate object tags: Yes
    - Replication Time Control (RTC): Enabled (15 minutes)
  
  Monitoring:

    - Replication lag alert: > 30 minutes
    - Failed replication alert: Any failure
    - Metrics: Published to CloudWatch

```

## Database Backup Strategy

### RDS Automated Backups

**Configuration**:

```yaml
Automated Backups:
  Enabled: true
  Retention Period: 30 days
  Backup Window: 03:00-04:00 UTC (off-peak)
  Backup Type: Automated snapshots
  Encryption: AES-256 (KMS)
  KMS Key: arn:aws:kms:us-east-1:123456789012:key/backup-key
  
Point-in-Time Recovery:
  Enabled: true
  Retention: 30 days
  Granularity: 5 minutes
  Transaction Logs: Continuous backup
  
Multi-AZ Deployment:
  Enabled: true
  Synchronous Replication: us-east-1a → us-east-1b
  Automatic Failover: Yes
  Failover Time: 60-120 seconds
```

**Snapshot Export to S3**:

```bash
# Export RDS snapshot to S3 for long-term storage
aws rds start-export-task \
  --export-task-identifier ecommerce-db-export-2025-10-23 \
  --source-arn arn:aws:rds:us-east-1:123456789012:snapshot:ecommerce-prod-db-2025-10-23 \
  --s3-bucket-name ecommerce-backups-prod-us-east-1 \
  --s3-prefix rds-snapshots/2025/10/23/ \
  --iam-role-arn arn:aws:iam::123456789012:role/rds-s3-export-role \
  --kms-key-id arn:aws:kms:us-east-1:123456789012:key/backup-key \
  --export-only '["schema1.table1", "schema1.table2"]'  # Optional: specific tables

# Monitor export progress
aws rds describe-export-tasks \
  --export-task-identifier ecommerce-db-export-2025-10-23
```

**Manual Snapshots**:

```bash
# Create manual snapshot before major changes
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier ecommerce-prod-db-pre-migration-2025-10-23

# List snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db

# Copy snapshot to another region (DR)
aws rds copy-db-snapshot \
  --source-db-snapshot-identifier arn:aws:rds:us-east-1:123456789012:snapshot:ecommerce-prod-db-2025-10-23 \
  --target-db-snapshot-identifier ecommerce-prod-db-2025-10-23-dr \
  --region us-west-2
```

## Backup Encryption and Security

### Encryption Standards

**At-Rest Encryption**:

```yaml
RDS Database:
  Encryption: AES-256
  Key Management: AWS KMS
  Key Rotation: Automatic (annual)
  Snapshot Encryption: Inherited from source
  
Redis Cache:
  Encryption: AES-256
  Key Management: AWS KMS
  Snapshot Encryption: Enabled
  
S3 Backups:
  Default Encryption: SSE-S3 (AES-256)
  Sensitive Data: SSE-KMS
  Key Management: AWS KMS
  Bucket Key: Enabled (cost optimization)
  
Kafka:
  Encryption: TLS 1.2+
  Key Management: AWS KMS
  Topic Encryption: Enabled
```

**In-Transit Encryption**:

```yaml
Backup Transfer:
  Protocol: TLS 1.2+
  Certificate Validation: Required
  
Cross-Region Replication:
  Encryption: TLS 1.2+
  Integrity Check: MD5/SHA-256
  
Restore Operations:
  Protocol: TLS 1.2+
  Authentication: IAM roles
```

### Access Control

**IAM Policies**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BackupCreation",
      "Effect": "Allow",
      "Principal": {
        "Service": "backup.amazonaws.com"
      },
      "Action": [
        "rds:CreateDBSnapshot",
        "elasticache:CreateSnapshot",
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:rds:*:*:snapshot:*",
        "arn:aws:elasticache:*:*:snapshot:*",
        "arn:aws:s3:::ecommerce-backups-prod-*/*"
      ]
    },
    {
      "Sid": "BackupRestore",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:role/ops-team-role"
      },
      "Action": [
        "rds:RestoreDBInstanceFromDBSnapshot",
        "elasticache:CreateReplicationGroup",
        "s3:GetObject"
      ],
      "Resource": "*",
      "Condition": {
        "StringEquals": {
          "aws:RequestedRegion": ["us-east-1", "us-west-2"]
        }
      }
    },
    {
      "Sid": "BackupDeletion",
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::123456789012:role/backup-lifecycle-role"
      },
      "Action": [
        "rds:DeleteDBSnapshot",
        "elasticache:DeleteSnapshot",
        "s3:DeleteObject"
      ],
      "Resource": "*",
      "Condition": {
        "DateGreaterThan": {
          "aws:CurrentTime": "2025-01-01T00:00:00Z"
        }
      }
    }
  ]
}
```

**S3 Bucket Policy**:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyUnencryptedObjectUploads",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::ecommerce-backups-prod-*/*",
      "Condition": {
        "StringNotEquals": {
          "s3:x-amz-server-side-encryption": ["AES256", "aws:kms"]
        }
      }
    },
    {
      "Sid": "DenyInsecureTransport",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::ecommerce-backups-prod-*",
        "arn:aws:s3:::ecommerce-backups-prod-*/*"
      ],
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      }
    },
    {
      "Sid": "RequireMFAForDeletion",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:DeleteObject",
      "Resource": "arn:aws:s3:::ecommerce-backups-prod-*/*",
      "Condition": {
        "BoolIfExists": {
          "aws:MultiFactorAuthPresent": "false"
        }
      }
    }
  ]
}
```

### KMS Key Management

**Backup KMS Key Configuration**:

```yaml
Key Alias: alias/ecommerce-backup-key
Key Type: Symmetric
Key Spec: SYMMETRIC_DEFAULT
Key Usage: ENCRYPT_DECRYPT

Key Policy:

  - Service: RDS, ElastiCache, S3
  - Principals: backup.amazonaws.com
  - Actions: Encrypt, Decrypt, GenerateDataKey
  
Key Rotation:
  Enabled: true
  Frequency: Annual (automatic)
  
Multi-Region:
  Enabled: true
  Replica Regions: us-west-2
```

## Backup Verification and Integrity

### Automated Verification

**Database Backup Verification**:

```bash
#!/bin/bash
# automated-db-backup-verification.sh

set -e

SNAPSHOT_ID=$1
TEST_INSTANCE="ecommerce-verify-$(date +%s)"
VERIFICATION_RESULTS="/tmp/backup-verification-$(date +%Y%m%d).log"

echo "Starting backup verification for snapshot: $SNAPSHOT_ID" | tee -a $VERIFICATION_RESULTS

# 1. Restore snapshot to test instance
echo "Step 1: Restoring snapshot..." | tee -a $VERIFICATION_RESULTS
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier $TEST_INSTANCE \
  --db-snapshot-identifier $SNAPSHOT_ID \
  --db-instance-class db.t3.medium \
  --no-publicly-accessible \
  --tags Key=Purpose,Value=BackupVerification

# 2. Wait for instance availability
echo "Step 2: Waiting for instance to be available..." | tee -a $VERIFICATION_RESULTS
aws rds wait db-instance-available \
  --db-instance-identifier $TEST_INSTANCE

# 3. Get endpoint
ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier $TEST_INSTANCE \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "Step 3: Running integrity checks on $ENDPOINT..." | tee -a $VERIFICATION_RESULTS

# 4. Run comprehensive integrity checks
psql -h $ENDPOINT -U admin -d ecommerce <<EOF | tee -a $VERIFICATION_RESULTS
-- Check table counts
SELECT 'orders' as table_name, COUNT(*) as row_count FROM orders
UNION ALL
SELECT 'customers', COUNT(*) FROM customers
UNION ALL
SELECT 'products', COUNT(*) FROM products
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items;

-- Check data consistency
SELECT 
  'Order-Customer Consistency' as check_name,
  COUNT(*) as inconsistent_records
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- Check referential integrity
SELECT 
  'Order-Items Consistency' as check_name,
  COUNT(*) as inconsistent_records
FROM order_items oi
LEFT JOIN orders o ON oi.order_id = o.id
WHERE o.id IS NULL;

-- Check for data corruption
SELECT 
  'Null Primary Keys' as check_name,
  COUNT(*) as corrupt_records
FROM orders
WHERE id IS NULL;

-- Verify recent data
SELECT 
  'Recent Orders' as check_name,
  COUNT(*) as count
FROM orders
WHERE created_at > NOW() - INTERVAL '24 hours';
EOF

# 5. Calculate checksums
echo "Step 4: Calculating checksums..." | tee -a $VERIFICATION_RESULTS
psql -h $ENDPOINT -U admin -d ecommerce -t -c \
  "SELECT md5(string_agg(id::text, ',' ORDER BY id)) FROM orders;" \
  | tee -a $VERIFICATION_RESULTS

# 6. Compare with production checksums (if available)
if [ -f "/var/backup/checksums/production-checksums.txt" ]; then
  echo "Step 5: Comparing with production checksums..." | tee -a $VERIFICATION_RESULTS
  diff /var/backup/checksums/production-checksums.txt $VERIFICATION_RESULTS || true
fi

# 7. Cleanup
echo "Step 6: Cleaning up test instance..." | tee -a $VERIFICATION_RESULTS
aws rds delete-db-instance \
  --db-instance-identifier $TEST_INSTANCE \
  --skip-final-snapshot

# 8. Send verification report
echo "Step 7: Sending verification report..." | tee -a $VERIFICATION_RESULTS
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:backup-verification \
  --subject "Backup Verification Report - $SNAPSHOT_ID" \
  --message file://$VERIFICATION_RESULTS

echo "Backup verification completed successfully" | tee -a $VERIFICATION_RESULTS
```

### Integrity Checking

**Checksum Verification**:

```bash
#!/bin/bash
# calculate-backup-checksums.sh

# Calculate checksums for S3 objects
aws s3api list-objects-v2 \
  --bucket ecommerce-backups-prod-us-east-1 \
  --prefix rds-snapshots/ \
  --query 'Contents[*].[Key,ETag,Size]' \
  --output text | while read key etag size; do
  
  echo "Object: $key"
  echo "  ETag: $etag"
  echo "  Size: $size bytes"
  echo "  MD5: $(aws s3api head-object --bucket ecommerce-backups-prod-us-east-1 --key $key --query Metadata.md5 --output text)"
  echo "---"
done

# Verify S3 object integrity
aws s3api head-object \
  --bucket ecommerce-backups-prod-us-east-1 \
  --key rds-snapshots/2025/10/23/snapshot.sql.gz \
  --checksum-mode ENABLED
```

**Data Consistency Checks**:

```sql
-- Database consistency verification queries
-- Run these after backup restoration

-- 1. Check for orphaned records
SELECT 'Orphaned Order Items' as issue,
       COUNT(*) as count
FROM order_items oi
LEFT JOIN orders o ON oi.order_id = o.id
WHERE o.id IS NULL;

-- 2. Check for invalid foreign keys
SELECT 'Invalid Customer References' as issue,
       COUNT(*) as count
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- 3. Check for data type consistency
SELECT 'Invalid Email Formats' as issue,
       COUNT(*) as count
FROM customers
WHERE email !~ '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$';

-- 4. Check for business rule violations
SELECT 'Negative Order Totals' as issue,
       COUNT(*) as count
FROM orders
WHERE total_amount < 0;

-- 5. Check temporal consistency
SELECT 'Future Dated Orders' as issue,
       COUNT(*) as count
FROM orders
WHERE created_at > NOW();

-- 6. Check aggregate consistency
SELECT 
  o.id as order_id,
  o.total_amount as order_total,
  SUM(oi.quantity * oi.unit_price) as calculated_total,
  ABS(o.total_amount - SUM(oi.quantity * oi.unit_price)) as difference
FROM orders o
JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.total_amount
HAVING ABS(o.total_amount - SUM(oi.quantity * oi.unit_price)) > 0.01;
```

### Database Backup Verification

**Weekly Verification Process**:

```bash
#!/bin/bash
# verify-db-backup.sh

# 1. Restore latest snapshot to test instance
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-test-restore \
  --db-snapshot-identifier latest-automated-snapshot

# 2. Wait for instance to be available
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-test-restore

# 3. Run data integrity checks
psql -h ecommerce-test-restore.xxx.rds.amazonaws.com \
     -U admin -d ecommerce \
     -c "SELECT COUNT(*) FROM orders;"
     
psql -h ecommerce-test-restore.xxx.rds.amazonaws.com \
     -U admin -d ecommerce \
     -c "SELECT COUNT(*) FROM customers;"

# 4. Delete test instance
aws rds delete-db-instance \
  --db-instance-identifier ecommerce-test-restore \
  --skip-final-snapshot
```

## Backup Cost Optimization

### Cost Analysis

**Monthly Backup Costs (Estimated)**:

```yaml
RDS Snapshots:
  Automated Snapshots (30 days): $150/month
  Manual Snapshots (90 days): $50/month
  Cross-Region Copies: $75/month
  Total RDS: $275/month

S3 Storage:
  Standard (0-30 days): $100/month (4.3 TB)
  Intelligent-Tiering (31-90 days): $40/month (3.2 TB)
  Glacier Flexible (91-365 days): $15/month (4.2 TB)
  Glacier Deep Archive (1-7 years): $8/month (8 TB)
  Total S3: $163/month

Data Transfer:
  Cross-Region Replication: $50/month
  Snapshot Exports: $25/month
  Total Transfer: $75/month

Redis Snapshots:
  Automated Snapshots: $30/month
  Manual Snapshots: $10/month
  Total Redis: $40/month

Total Monthly Cost: $553/month
Annual Cost: $6,636/year
```

### Cost Optimization Strategies

#### 1. Lifecycle Policy Optimization

**Aggressive Tiering**:

```json
{
  "Rules": [
    {
      "Id": "cost-optimized-lifecycle",
      "Status": "Enabled",
      "Transitions": [
        {
          "Days": 7,
          "StorageClass": "INTELLIGENT_TIERING"
        },
        {
          "Days": 30,
          "StorageClass": "GLACIER_FLEXIBLE_RETRIEVAL"
        },
        {
          "Days": 90,
          "StorageClass": "DEEP_ARCHIVE"
        }
      ]
    }
  ]
}
```

**Estimated Savings**: $80/month (49% reduction in S3 costs)

#### 2. Snapshot Retention Optimization

**Reduced Retention Periods**:

```yaml
Before:
  RDS Automated: 30 days
  RDS Manual: 90 days
  Redis: 7 days

After (Non-Critical Environments):
  RDS Automated: 7 days
  RDS Manual: 30 days
  Redis: 3 days

Estimated Savings: $120/month
```

#### 3. Compression and Deduplication

**Database Export Compression**:

```bash
# Export with compression
pg_dump -h $DB_HOST -U admin ecommerce | gzip -9 > backup.sql.gz

# Typical compression ratios:
# - Text data: 80-90% reduction
# - JSON data: 70-85% reduction
# - Binary data: 20-40% reduction

# Average compression: 75% reduction
# Storage savings: $75/month
```

#### 4. Incremental Backups

**Incremental Backup Strategy**:

```yaml
Full Backups:
  Frequency: Weekly (Sunday)
  Retention: 4 weeks
  
Incremental Backups:
  Frequency: Daily
  Retention: 7 days
  Method: Transaction log shipping
  
Estimated Savings: $100/month (40% reduction)
```

#### 5. S3 Intelligent-Tiering

**Automatic Cost Optimization**:

```yaml
Configuration:
  Frequent Access Tier: First 30 days
  Infrequent Access Tier: 30-90 days
  Archive Instant Access: 90-180 days
  Archive Access: 180-365 days
  Deep Archive Access: 365+ days
  
Benefits:

  - Automatic tier transitions
  - No retrieval fees for frequent/infrequent tiers
  - Monitoring fee: $0.0025 per 1,000 objects
  
Estimated Savings: $60/month
```

#### 6. Cross-Region Replication Optimization

**Selective Replication**:

```yaml
Replicate Only:

  - Critical database backups
  - Compliance-required data
  - Recent backups (< 30 days)

Do Not Replicate:

  - Cache snapshots
  - Logs older than 30 days
  - Non-critical configuration backups

Estimated Savings: $35/month (70% reduction in transfer costs)
```

#### 7. Backup Consolidation

**Eliminate Redundant Backups**:

```yaml
Before:

  - RDS automated snapshots: Every 5 minutes
  - Manual snapshots: Daily
  - Export to S3: Daily
  
After:

  - RDS automated snapshots: Every 5 minutes (keep)
  - Manual snapshots: Weekly only
  - Export to S3: Weekly (for long-term storage)

Estimated Savings: $45/month
```

#### 8. Reserved Capacity

**S3 Storage Lens Recommendations**:

```yaml
Purchase S3 Storage Lens:
  Cost: $0.20 per million objects analyzed
  Benefit: Identify unused/redundant backups
  
Estimated Savings: $30/month from cleanup
```

### Cost Monitoring

**CloudWatch Cost Metrics**:

```yaml
Metrics:

  - backup.storage.cost.daily
  - backup.transfer.cost.daily
  - backup.snapshot.cost.daily
  
Alarms:

  - Daily cost > $25 (alert)
  - Monthly cost > $600 (critical)
  - Cost increase > 20% week-over-week (warning)

```

**Cost Allocation Tags**:

```yaml
Tags:

  - Environment: production|staging|development
  - Component: rds|redis|kafka|config
  - CostCenter: operations
  - Retention: short-term|long-term|compliance
  - Criticality: critical|important|standard

```

### Total Potential Savings

```yaml
Optimization Strategy Summary:

  1. Lifecycle Policy Optimization: $80/month
  2. Retention Optimization: $120/month
  3. Compression: $75/month
  4. Incremental Backups: $100/month
  5. Intelligent-Tiering: $60/month
  6. Selective Replication: $35/month
  7. Backup Consolidation: $45/month
  8. Storage Lens Cleanup: $30/month

Total Monthly Savings: $545/month
Optimized Monthly Cost: $8/month (98.6% reduction)
Annual Savings: $6,540/year

Note: Actual savings depend on implementation scope and data characteristics
```

## Redis Cache Backup

### ElastiCache Backup Configuration

```yaml
Backup Configuration:
  Automatic Backups: Enabled
  Backup Retention: 7 days
  Backup Window: 04:00-05:00 UTC
  Snapshot Name: ecommerce-redis-auto-{date}
  
Manual Snapshots:
  Before Deployments: Yes
  Before Configuration Changes: Yes
  Retention: 30 days
```

**Manual Backup**:

```bash
# Create manual snapshot
aws elasticache create-snapshot \
  --replication-group-id ecommerce-redis-cluster \
  --snapshot-name ecommerce-redis-pre-deployment-2025-10-23

# Export snapshot to S3 for long-term storage
aws elasticache copy-snapshot \
  --source-snapshot-name ecommerce-redis-pre-deployment-2025-10-23 \
  --target-snapshot-name ecommerce-redis-archive-2025-10-23 \
  --target-bucket s3://ecommerce-backups/redis/
```

### Redis Backup Methods

#### RDB (Redis Database) Snapshots

**RDB Configuration**:

```yaml
# redis.conf
save 900 1      # Save if at least 1 key changed in 900 seconds
save 300 10     # Save if at least 10 keys changed in 300 seconds
save 60 10000   # Save if at least 10000 keys changed in 60 seconds

dbfilename dump.rdb
dir /var/lib/redis
rdbcompression yes
rdbchecksum yes

# Backup on shutdown
stop-writes-on-bgsave-error yes
```

**Manual RDB Backup**:

```bash
#!/bin/bash
# redis-rdb-backup.sh

set -e

REDIS_HOST="ecommerce-redis-cluster.xxx.cache.amazonaws.com"
REDIS_PORT=6379
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/redis-snapshots"
DATE=$(date +%Y-%m-%d-%H%M%S)
BACKUP_DIR="/tmp/redis-backup-${DATE}"

mkdir -p $BACKUP_DIR

echo "Starting Redis RDB backup at $DATE"

# 1. Trigger BGSAVE on Redis
redis-cli -h $REDIS_HOST -p $REDIS_PORT BGSAVE

# 2. Wait for BGSAVE to complete
while [ "$(redis-cli -h $REDIS_HOST -p $REDIS_PORT LASTSAVE)" == "$(redis-cli -h $REDIS_HOST -p $REDIS_PORT LASTSAVE)" ]; do
  echo "Waiting for BGSAVE to complete..."
  sleep 5
done

echo "BGSAVE completed"

# 3. For ElastiCache, use AWS CLI to create snapshot
aws elasticache create-snapshot \
  --replication-group-id ecommerce-redis-cluster \
  --snapshot-name ecommerce-redis-rdb-${DATE} \
  --tags Key=BackupType,Value=RDB Key=Date,Value=${DATE}

# 4. Wait for snapshot to be available
aws elasticache wait snapshot-available \
  --snapshot-name ecommerce-redis-rdb-${DATE}

# 5. Export snapshot to S3
aws elasticache copy-snapshot \
  --source-snapshot-name ecommerce-redis-rdb-${DATE} \
  --target-snapshot-name ecommerce-redis-rdb-${DATE} \
  --target-bucket ecommerce-backups-prod-us-east-1 \
  --kms-key-id arn:aws:kms:us-east-1:123456789012:key/backup-key

echo "RDB backup completed and exported to S3"

# 6. Record backup metadata
cat > ${BACKUP_DIR}/metadata.json <<EOF
{
  "backup_type": "RDB",
  "timestamp": "${DATE}",
  "redis_host": "${REDIS_HOST}",
  "snapshot_name": "ecommerce-redis-rdb-${DATE}",
  "s3_location": "${S3_BUCKET}/${DATE}/",
  "key_count": $(redis-cli -h $REDIS_HOST -p $REDIS_PORT DBSIZE | awk '{print $2}')
}
EOF

aws s3 cp ${BACKUP_DIR}/metadata.json ${S3_BUCKET}/${DATE}/metadata.json

# 7. Cleanup
rm -rf $BACKUP_DIR

echo "Redis RDB backup process completed successfully"
```

#### AOF (Append-Only File) Backup

**AOF Configuration**:

```yaml
# redis.conf
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec  # Options: always, everysec, no

# AOF rewrite configuration
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-load-truncated yes
aof-use-rdb-preamble yes
```

**AOF Backup Script**:

```bash
#!/bin/bash
# redis-aof-backup.sh

set -e

REDIS_HOST="ecommerce-redis-cluster.xxx.cache.amazonaws.com"
REDIS_PORT=6379
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/redis-aof"
DATE=$(date +%Y-%m-%d-%H%M%S)

echo "Starting Redis AOF backup at $DATE"

# 1. Trigger AOF rewrite to compact the file
redis-cli -h $REDIS_HOST -p $REDIS_PORT BGREWRITEAOF

# 2. Wait for rewrite to complete
while [ "$(redis-cli -h $REDIS_HOST -p $REDIS_PORT INFO persistence | grep aof_rewrite_in_progress | cut -d: -f2 | tr -d '\r')" == "1" ]; do
  echo "Waiting for AOF rewrite to complete..."
  sleep 5
done

echo "AOF rewrite completed"

# 3. For self-managed Redis, copy AOF file
# For ElastiCache, use snapshot method
aws elasticache create-snapshot \
  --replication-group-id ecommerce-redis-cluster \
  --snapshot-name ecommerce-redis-aof-${DATE} \
  --tags Key=BackupType,Value=AOF Key=Date,Value=${DATE}

# 4. Export to S3
aws elasticache copy-snapshot \
  --source-snapshot-name ecommerce-redis-aof-${DATE} \
  --target-snapshot-name ecommerce-redis-aof-${DATE} \
  --target-bucket ecommerce-backups-prod-us-east-1

echo "AOF backup completed and exported to S3"
```

### Redis Restore Procedures

#### Restore from RDB Snapshot

**ElastiCache Restore**:

```bash
#!/bin/bash
# redis-restore-from-rdb.sh

set -e

SNAPSHOT_NAME=$1
NEW_CLUSTER_ID="ecommerce-redis-restored-$(date +%s)"

if [ -z "$SNAPSHOT_NAME" ]; then
  echo "Usage: $0 <snapshot-name>"
  exit 1
fi

echo "Restoring Redis cluster from RDB snapshot: $SNAPSHOT_NAME"

# 1. Create new replication group from snapshot
aws elasticache create-replication-group \
  --replication-group-id $NEW_CLUSTER_ID \
  --replication-group-description "Restored from $SNAPSHOT_NAME" \
  --snapshot-name $SNAPSHOT_NAME \
  --cache-node-type cache.r5.large \
  --engine redis \
  --engine-version 7.0 \
  --num-cache-clusters 3 \
  --automatic-failover-enabled \
  --multi-az-enabled \
  --cache-subnet-group-name ecommerce-redis-subnet-group \
  --security-group-ids sg-xxxxxxxxx \
  --preferred-cache-cluster-a-zs us-east-1a us-east-1b us-east-1c \
  --tags Key=Environment,Value=production Key=RestoredFrom,Value=$SNAPSHOT_NAME

# 2. Wait for cluster to be available
echo "Waiting for cluster to be available..."
aws elasticache wait replication-group-available \
  --replication-group-id $NEW_CLUSTER_ID

# 3. Get cluster endpoint
CLUSTER_ENDPOINT=$(aws elasticache describe-replication-groups \
  --replication-group-id $NEW_CLUSTER_ID \
  --query 'ReplicationGroups[0].NodeGroups[0].PrimaryEndpoint.Address' \
  --output text)

echo "Cluster restored successfully"
echo "Primary Endpoint: $CLUSTER_ENDPOINT"
echo "Cluster ID: $NEW_CLUSTER_ID"

# 4. Verify data
echo "Verifying restored data..."
KEY_COUNT=$(redis-cli -h $CLUSTER_ENDPOINT -p 6379 DBSIZE | awk '{print $2}')
echo "Total keys in restored cluster: $KEY_COUNT"

# 5. Update application configuration
echo "Update application to use new Redis endpoint: $CLUSTER_ENDPOINT"
```

#### Restore from AOF File

**Self-Managed Redis Restore**:

```bash
#!/bin/bash
# redis-restore-from-aof.sh

set -e

AOF_BACKUP_FILE=$1
REDIS_DATA_DIR="/var/lib/redis"
REDIS_SERVICE="redis-server"

if [ -z "$AOF_BACKUP_FILE" ]; then
  echo "Usage: $0 <aof-backup-file>"
  exit 1
fi

echo "Restoring Redis from AOF file: $AOF_BACKUP_FILE"

# 1. Stop Redis service
echo "Stopping Redis service..."
systemctl stop $REDIS_SERVICE

# 2. Backup current data
echo "Backing up current data..."
cp -r $REDIS_DATA_DIR ${REDIS_DATA_DIR}.backup.$(date +%s)

# 3. Copy AOF file to Redis data directory
echo "Copying AOF file..."
cp $AOF_BACKUP_FILE ${REDIS_DATA_DIR}/appendonly.aof
chown redis:redis ${REDIS_DATA_DIR}/appendonly.aof
chmod 640 ${REDIS_DATA_DIR}/appendonly.aof

# 4. Start Redis service
echo "Starting Redis service..."
systemctl start $REDIS_SERVICE

# 5. Wait for Redis to be ready
echo "Waiting for Redis to be ready..."
until redis-cli ping | grep -q PONG; do
  echo "Waiting for Redis..."
  sleep 2
done

# 6. Verify data
echo "Verifying restored data..."
KEY_COUNT=$(redis-cli DBSIZE | awk '{print $2}')
echo "Total keys in restored instance: $KEY_COUNT"

echo "Redis restore from AOF completed successfully"
```

### ElastiCache Snapshot Management

#### Automated Snapshot Lifecycle

**Snapshot Retention Policy**:

```yaml
Automated Snapshots:
  Retention: 7 days
  Frequency: Daily at 04:00 UTC
  Naming: ecommerce-redis-auto-YYYY-MM-DD
  
Manual Snapshots:
  Retention: 30 days
  Naming: ecommerce-redis-manual-YYYY-MM-DD-purpose
  
Long-Term Archive:
  Retention: 90 days
  Storage: S3 Intelligent-Tiering
  Naming: ecommerce-redis-archive-YYYY-MM-DD
```

**Snapshot Cleanup Script**:

```bash
#!/bin/bash
# cleanup-old-redis-snapshots.sh

set -e

RETENTION_DAYS=30
CUTOFF_DATE=$(date -d "$RETENTION_DAYS days ago" +%Y-%m-%d)

echo "Cleaning up Redis snapshots older than $CUTOFF_DATE"

# Get all manual snapshots
SNAPSHOTS=$(aws elasticache describe-snapshots \
  --snapshot-source ecommerce-redis-cluster \
  --query "Snapshots[?SnapshotSource=='manual'].SnapshotName" \
  --output text)

for SNAPSHOT in $SNAPSHOTS; do
  # Extract date from snapshot name (format: ecommerce-redis-manual-YYYY-MM-DD-*)
  SNAPSHOT_DATE=$(echo $SNAPSHOT | grep -oP '\d{4}-\d{2}-\d{2}' | head -1)
  
  if [[ "$SNAPSHOT_DATE" < "$CUTOFF_DATE" ]]; then
    echo "Deleting old snapshot: $SNAPSHOT (Date: $SNAPSHOT_DATE)"
    
    # Check if snapshot is exported to S3
    S3_KEY="redis-snapshots/${SNAPSHOT_DATE}/${SNAPSHOT}"
    if aws s3 ls s3://ecommerce-backups-prod-us-east-1/$S3_KEY >/dev/null 2>&1; then
      echo "Snapshot already archived to S3, safe to delete"
      aws elasticache delete-snapshot --snapshot-name $SNAPSHOT
    else
      echo "WARNING: Snapshot not found in S3, skipping deletion"
    fi
  fi
done

echo "Snapshot cleanup completed"
```

#### Cross-Region Snapshot Copy

**Disaster Recovery Snapshot Replication**:

```bash
#!/bin/bash
# replicate-redis-snapshots.sh

set -e

SOURCE_REGION="us-east-1"
TARGET_REGION="us-west-2"
REPLICATION_GROUP="ecommerce-redis-cluster"

echo "Replicating Redis snapshots from $SOURCE_REGION to $TARGET_REGION"

# Get latest snapshot
LATEST_SNAPSHOT=$(aws elasticache describe-snapshots \
  --region $SOURCE_REGION \
  --replication-group-id $REPLICATION_GROUP \
  --query 'Snapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotName' \
  --output text)

echo "Latest snapshot: $LATEST_SNAPSHOT"

# Copy snapshot to target region
aws elasticache copy-snapshot \
  --region $TARGET_REGION \
  --source-snapshot-name $LATEST_SNAPSHOT \
  --target-snapshot-name ${LATEST_SNAPSHOT}-dr \
  --source-region $SOURCE_REGION \
  --kms-key-id arn:aws:kms:${TARGET_REGION}:123456789012:key/backup-key

echo "Snapshot replication initiated to $TARGET_REGION"

# Wait for copy to complete
aws elasticache wait snapshot-available \
  --region $TARGET_REGION \
  --snapshot-name ${LATEST_SNAPSHOT}-dr

echo "Snapshot replication completed successfully"
```

## Kafka Backup Strategy

### Topic Replication

```yaml
Kafka Configuration:
  Replication Factor: 3
  Min In-Sync Replicas: 2
  Unclean Leader Election: false
  
Topic Backup:
  Method: MirrorMaker 2.0
  Destination: Backup Kafka cluster (us-west-2)
  Lag Monitoring: < 1000 messages
```

**Backup Verification**:

```bash
# Check replication lag
kafka-consumer-groups.sh \
  --bootstrap-server kafka-broker:9092 \
  --describe --group mirror-maker-group

# Verify topic data
kafka-console-consumer.sh \
  --bootstrap-server kafka-broker:9092 \
  --topic order-events \
  --from-beginning --max-messages 10
```

### Kafka Topic Backup and Recovery

#### Automated Topic Backup

**MirrorMaker 2.0 Configuration**:

```yaml
# kafka-mirror-maker-config.yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaMirrorMaker2
metadata:
  name: ecommerce-mirror-maker
  namespace: kafka
spec:
  version: 3.5.0
  replicas: 3
  connectCluster: "backup-cluster"
  
  clusters:

    - alias: "source-cluster"

      bootstrapServers: kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092
      config:
        config.storage.replication.factor: 3
        offset.storage.replication.factor: 3
        status.storage.replication.factor: 3
        
    - alias: "backup-cluster"

      bootstrapServers: kafka-backup-1.us-west-2:9092,kafka-backup-2.us-west-2:9092
      config:
        config.storage.replication.factor: 3
        offset.storage.replication.factor: 3
        status.storage.replication.factor: 3
  
  mirrors:

    - sourceCluster: "source-cluster"

      targetCluster: "backup-cluster"
      sourceConnector:
        config:
          replication.factor: 3
          offset-syncs.topic.replication.factor: 3
          sync.topic.acls.enabled: "false"
          refresh.topics.interval.seconds: 60
          
      heartbeatConnector:
        config:
          heartbeats.topic.replication.factor: 3
          
      checkpointConnector:
        config:
          checkpoints.topic.replication.factor: 3
          sync.group.offsets.enabled: "true"
          sync.group.offsets.interval.seconds: 60
          emit.checkpoints.interval.seconds: 60
          
      topicsPattern: "order-events|customer-events|payment-events|inventory-events"
      groupsPattern: ".*"
```

**Topic Archive to S3**:

```bash
#!/bin/bash
# kafka-topic-archive.sh

set -e

KAFKA_BROKER="kafka-broker:9092"
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/kafka-archives"
DATE=$(date +%Y-%m-%d)
TOPICS=("order-events" "customer-events" "payment-events" "inventory-events")

echo "Starting Kafka topic archive for $DATE"

for TOPIC in "${TOPICS[@]}"; do
  echo "Archiving topic: $TOPIC"
  
  # Export topic data to JSON
  kafka-console-consumer.sh \
    --bootstrap-server $KAFKA_BROKER \
    --topic $TOPIC \
    --from-beginning \
    --timeout-ms 60000 \
    --property print.timestamp=true \
    --property print.key=true \
    --property print.offset=true \
    --property print.partition=true > /tmp/${TOPIC}-${DATE}.json
  
  # Compress the export
  gzip -9 /tmp/${TOPIC}-${DATE}.json
  
  # Upload to S3
  aws s3 cp /tmp/${TOPIC}-${DATE}.json.gz \
    ${S3_BUCKET}/${DATE}/${TOPIC}/ \
    --storage-class INTELLIGENT_TIERING \
    --metadata "topic=${TOPIC},date=${DATE},format=json"
  
  # Calculate and store checksum
  md5sum /tmp/${TOPIC}-${DATE}.json.gz | awk '{print $1}' > /tmp/${TOPIC}-${DATE}.md5
  aws s3 cp /tmp/${TOPIC}-${DATE}.md5 ${S3_BUCKET}/${DATE}/${TOPIC}/
  
  # Cleanup local files
  rm -f /tmp/${TOPIC}-${DATE}.json.gz /tmp/${TOPIC}-${DATE}.md5
  
  echo "Completed archiving topic: $TOPIC"
done

# Record archive metadata
cat > /tmp/archive-metadata-${DATE}.json <<EOF
{
  "archive_date": "${DATE}",
  "topics": $(printf '%s\n' "${TOPICS[@]}" | jq -R . | jq -s .),
  "kafka_cluster": "${KAFKA_BROKER}",
  "s3_location": "${S3_BUCKET}/${DATE}",
  "retention_days": 90
}
EOF

aws s3 cp /tmp/archive-metadata-${DATE}.json ${S3_BUCKET}/${DATE}/metadata.json
rm -f /tmp/archive-metadata-${DATE}.json

echo "Kafka topic archive completed successfully"
```

#### Kafka Topic Recovery

**Restore from Backup Cluster**:

```bash
#!/bin/bash
# kafka-topic-restore.sh

set -e

SOURCE_CLUSTER="kafka-backup-1.us-west-2:9092"
TARGET_CLUSTER="kafka-broker:9092"
TOPIC=$1
CONSUMER_GROUP=$2

if [ -z "$TOPIC" ] || [ -z "$CONSUMER_GROUP" ]; then
  echo "Usage: $0 <topic> <consumer-group>"
  exit 1
fi

echo "Restoring topic: $TOPIC from backup cluster"

# 1. Create topic on target cluster if it doesn't exist
kafka-topics.sh --bootstrap-server $TARGET_CLUSTER \
  --describe --topic $TOPIC 2>/dev/null || \
kafka-topics.sh --bootstrap-server $TARGET_CLUSTER \
  --create --topic $TOPIC \
  --partitions 6 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config compression.type=lz4

# 2. Reset consumer group offsets to beginning
kafka-consumer-groups.sh --bootstrap-server $TARGET_CLUSTER \
  --group $CONSUMER_GROUP \
  --topic $TOPIC \
  --reset-offsets --to-earliest \
  --execute

# 3. Start MirrorMaker to replicate from backup to target
kafka-mirror-maker.sh \
  --consumer.config consumer-backup.properties \
  --producer.config producer-target.properties \
  --whitelist $TOPIC \
  --num.streams 3

echo "Topic restoration initiated. Monitor replication lag."
```

**Restore from S3 Archive**:

```bash
#!/bin/bash
# kafka-restore-from-s3.sh

set -e

TOPIC=$1
ARCHIVE_DATE=$2
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/kafka-archives"
KAFKA_BROKER="kafka-broker:9092"

if [ -z "$TOPIC" ] || [ -z "$ARCHIVE_DATE" ]; then
  echo "Usage: $0 <topic> <archive-date (YYYY-MM-DD)>"
  exit 1
fi

echo "Restoring topic $TOPIC from S3 archive dated $ARCHIVE_DATE"

# 1. Download archive from S3
aws s3 cp ${S3_BUCKET}/${ARCHIVE_DATE}/${TOPIC}/${TOPIC}-${ARCHIVE_DATE}.json.gz /tmp/
aws s3 cp ${S3_BUCKET}/${ARCHIVE_DATE}/${TOPIC}/${TOPIC}-${ARCHIVE_DATE}.md5 /tmp/

# 2. Verify checksum
EXPECTED_MD5=$(cat /tmp/${TOPIC}-${ARCHIVE_DATE}.md5)
ACTUAL_MD5=$(md5sum /tmp/${TOPIC}-${ARCHIVE_DATE}.json.gz | awk '{print $1}')

if [ "$EXPECTED_MD5" != "$ACTUAL_MD5" ]; then
  echo "ERROR: Checksum mismatch! Archive may be corrupted."
  exit 1
fi

echo "Checksum verified successfully"

# 3. Decompress archive
gunzip /tmp/${TOPIC}-${ARCHIVE_DATE}.json.gz

# 4. Replay messages to Kafka topic
echo "Replaying messages to topic: $TOPIC"
cat /tmp/${TOPIC}-${ARCHIVE_DATE}.json | while IFS= read -r line; do
  # Extract key and value from archived format
  KEY=$(echo $line | jq -r '.key')
  VALUE=$(echo $line | jq -r '.value')
  
  # Produce message to Kafka
  echo "$VALUE" | kafka-console-producer.sh \
    --bootstrap-server $KAFKA_BROKER \
    --topic $TOPIC \
    --property "key.separator=:" \
    --property "parse.key=true" \
    --property "key=$KEY"
done

# 5. Verify message count
RESTORED_COUNT=$(kafka-run-class.sh kafka.tools.GetOffsetShell \
  --broker-list $KAFKA_BROKER \
  --topic $TOPIC \
  --time -1 | awk -F: '{sum += $3} END {print sum}')

echo "Restored $RESTORED_COUNT messages to topic $TOPIC"

# 6. Cleanup
rm -f /tmp/${TOPIC}-${ARCHIVE_DATE}.json /tmp/${TOPIC}-${ARCHIVE_DATE}.md5

echo "Topic restoration from S3 completed successfully"
```

#### Kafka Consumer Group Offset Backup

**Backup Consumer Offsets**:

```bash
#!/bin/bash
# backup-consumer-offsets.sh

KAFKA_BROKER="kafka-broker:9092"
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/kafka-offsets"
DATE=$(date +%Y-%m-%d-%H%M%S)

# Get all consumer groups
CONSUMER_GROUPS=$(kafka-consumer-groups.sh \
  --bootstrap-server $KAFKA_BROKER \
  --list)

for GROUP in $CONSUMER_GROUPS; do
  echo "Backing up offsets for consumer group: $GROUP"
  
  # Export offsets to JSON
  kafka-consumer-groups.sh \
    --bootstrap-server $KAFKA_BROKER \
    --group $GROUP \
    --describe \
    --members \
    --verbose > /tmp/${GROUP}-offsets-${DATE}.txt
  
  # Upload to S3
  aws s3 cp /tmp/${GROUP}-offsets-${DATE}.txt \
    ${S3_BUCKET}/${DATE}/${GROUP}/ \
    --metadata "group=${GROUP},date=${DATE}"
  
  rm -f /tmp/${GROUP}-offsets-${DATE}.txt
done

echo "Consumer offset backup completed"
```

## S3 Bucket Versioning and Replication

### S3 Versioning Configuration

**Enable Versioning on Critical Buckets**:

```bash
#!/bin/bash
# enable-s3-versioning.sh

BUCKETS=(
  "ecommerce-backups-prod-us-east-1"
  "ecommerce-config-prod"
  "ecommerce-application-data"
)

for BUCKET in "${BUCKETS[@]}"; do
  echo "Enabling versioning on bucket: $BUCKET"
  
  # Enable versioning
  aws s3api put-bucket-versioning \
    --bucket $BUCKET \
    --versioning-configuration Status=Enabled
  
  echo "Versioning enabled on $BUCKET"
done
```

**Versioning Lifecycle Policy**:

```json
{
  "Rules": [
    {
      "Id": "version-lifecycle",
      "Status": "Enabled",
      "NoncurrentVersionTransitions": [
        {
          "NoncurrentDays": 30,
          "StorageClass": "INTELLIGENT_TIERING"
        },
        {
          "NoncurrentDays": 90,
          "StorageClass": "GLACIER_FLEXIBLE_RETRIEVAL"
        }
      ],
      "NoncurrentVersionExpiration": {
        "NoncurrentDays": 365
      }
    }
  ]
}
```

### S3 Cross-Region Replication

**Replication Configuration**:

```bash
#!/bin/bash
# configure-s3-replication.sh

SOURCE_BUCKET="ecommerce-backups-prod-us-east-1"
DEST_BUCKET="ecommerce-backups-prod-us-west-2"
REPLICATION_ROLE="arn:aws:iam::123456789012:role/s3-replication-role"

# Create replication configuration
cat > replication-config.json <<EOF
{
  "Role": "${REPLICATION_ROLE}",
  "Rules": [
    {
      "ID": "replicate-all-objects",
      "Status": "Enabled",
      "Priority": 1,
      "Filter": {},
      "Destination": {
        "Bucket": "arn:aws:s3:::${DEST_BUCKET}",
        "ReplicationTime": {
          "Status": "Enabled",
          "Time": {
            "Minutes": 15
          }
        },
        "Metrics": {
          "Status": "Enabled",
          "EventThreshold": {
            "Minutes": 15
          }
        },
        "StorageClass": "INTELLIGENT_TIERING",
        "EncryptionConfiguration": {
          "ReplicaKmsKeyID": "arn:aws:kms:us-west-2:123456789012:key/backup-key"
        }
      },
      "DeleteMarkerReplication": {
        "Status": "Disabled"
      }
    }
  ]
}
EOF

# Apply replication configuration
aws s3api put-bucket-replication \
  --bucket $SOURCE_BUCKET \
  --replication-configuration file://replication-config.json

echo "S3 replication configured from $SOURCE_BUCKET to $DEST_BUCKET"
```

## EFS Backup Procedures

### AWS Backup for EFS

**Backup Plan Configuration**:

```yaml
# efs-backup-plan.yaml
BackupPlan:
  Name: ecommerce-efs-backup-plan
  
  Rules:

    - RuleName: daily-backup

      TargetBackupVault: ecommerce-backup-vault
      ScheduleExpression: "cron(0 5 * * ? *)"  # Daily at 5 AM UTC
      StartWindowMinutes: 60
      CompletionWindowMinutes: 120
      Lifecycle:
        DeleteAfterDays: 30
        MoveToColdStorageAfterDays: 7
      RecoveryPointTags:
        Environment: production
        BackupType: automated
```

**Create Backup Plan**:

```bash
#!/bin/bash
# create-efs-backup-plan.sh

set -e

BACKUP_VAULT="ecommerce-backup-vault"
EFS_ID="fs-12345678"
IAM_ROLE="arn:aws:iam::123456789012:role/aws-backup-service-role"

echo "Creating EFS backup plan"

# Create backup vault
aws backup create-backup-vault \
  --backup-vault-name $BACKUP_VAULT \
  --encryption-key-arn arn:aws:kms:us-east-1:123456789012:key/backup-key || true

# Create backup plan
BACKUP_PLAN_ID=$(aws backup create-backup-plan \
  --backup-plan file://efs-backup-plan.json \
  --query 'BackupPlanId' \
  --output text)

echo "EFS backup plan configured successfully"
```

## Application Configuration Backup

### Configuration Management

```yaml
Configuration Sources:

  - Kubernetes ConfigMaps
  - Kubernetes Secrets
  - AWS Secrets Manager
  - AWS Systems Manager Parameter Store
  
Backup Strategy:
  Method: Automated export to S3
  Frequency: Daily
  Retention: 90 days
  Encryption: AES-256 (KMS)
```

**Comprehensive Backup Script**:

```bash
#!/bin/bash
# backup-application-config.sh

set -e

DATE=$(date +%Y-%m-%d-%H%M%S)
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/application-config"
BACKUP_DIR="/tmp/config-backup-${DATE}"
KMS_KEY="arn:aws:kms:us-east-1:123456789012:key/backup-key"

mkdir -p $BACKUP_DIR

echo "Starting application configuration backup at $DATE"

# Backup Kubernetes resources
kubectl get configmaps --all-namespaces -o yaml > ${BACKUP_DIR}/configmaps.yaml
kubectl get secrets --all-namespaces -o yaml > ${BACKUP_DIR}/secrets.yaml
kubectl get deployments --all-namespaces -o yaml > ${BACKUP_DIR}/deployments.yaml
kubectl get services --all-namespaces -o yaml > ${BACKUP_DIR}/services.yaml

# Backup AWS Secrets Manager
aws secretsmanager list-secrets --query 'SecretList[*].Name' --output text | while read SECRET_NAME; do
  SECRET_VALUE=$(aws secretsmanager get-secret-value --secret-id "$SECRET_NAME" --query 'SecretString' --output text)
  echo "$SECRET_VALUE" > ${BACKUP_DIR}/secrets-manager-${SECRET_NAME}.json
done

# Compress and upload
tar -czf /tmp/config-backup-${DATE}.tar.gz -C /tmp config-backup-${DATE}
aws s3 cp /tmp/config-backup-${DATE}.tar.gz \
  ${S3_BUCKET}/${DATE}/config-backup.tar.gz \
  --sse aws:kms \
  --sse-kms-key-id $KMS_KEY

# Cleanup
rm -rf $BACKUP_DIR /tmp/config-backup-${DATE}.tar.gz

echo "Application configuration backup completed successfully"
```

## Secrets and Credentials Backup (Encrypted)

### AWS Secrets Manager Backup

**Automated Secrets Backup**:

```bash
#!/bin/bash
# backup-secrets-manager.sh

set -e

DATE=$(date +%Y-%m-%d-%H%M%S)
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1/secrets"
BACKUP_DIR="/tmp/secrets-backup-${DATE}"
KMS_KEY="arn:aws:kms:us-east-1:123456789012:key/backup-key"

mkdir -p $BACKUP_DIR

echo "Starting AWS Secrets Manager backup at $DATE"

# Get all secrets
SECRETS=$(aws secretsmanager list-secrets --query 'SecretList[*].[Name,ARN]' --output text)

while IFS=$'\t' read -r SECRET_NAME SECRET_ARN; do
  echo "Backing up secret: $SECRET_NAME"
  
  # Get secret value and metadata
  SECRET_DATA=$(aws secretsmanager get-secret-value \
    --secret-id "$SECRET_ARN" \
    --query '{SecretString:SecretString,VersionId:VersionId}' \
    --output json)
  
  echo "$SECRET_DATA" > ${BACKUP_DIR}/${SECRET_NAME}-value.json
done <<< "$SECRETS"

# Compress and encrypt
tar -czf /tmp/secrets-backup-${DATE}.tar.gz -C /tmp secrets-backup-${DATE}

# Upload to S3 with KMS encryption
aws s3 cp /tmp/secrets-backup-${DATE}.tar.gz \
  ${S3_BUCKET}/${DATE}/secrets-backup.tar.gz \
  --sse aws:kms \
  --sse-kms-key-id $KMS_KEY \
  --metadata "backup-date=${DATE},encrypted=true"

# Cleanup
rm -rf $BACKUP_DIR /tmp/secrets-backup-${DATE}.tar.gz

echo "Secrets Manager backup completed successfully"
```

## Backup Automation

> **🤖 Backup Automation**: For comprehensive backup automation documentation including scripts, tools, AWS Backup service configuration, job scheduling and orchestration, notification systems, failure handling, metrics, dashboards, and compliance reporting, see [Backup Automation](backup-automation.md).

## Recovery Procedures

> **📖 Detailed Restore Procedures**: For comprehensive step-by-step restore workflows including database, application state, configuration, partial restores, validation, and rollback procedures, see [Detailed Restore Procedures](detailed-restore-procedures.md).

### Database Recovery

#### Point-in-Time Recovery

```bash
# Restore database to specific point in time
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ecommerce-prod-db \
  --target-db-instance-identifier ecommerce-prod-db-restored \
  --restore-time 2025-10-23T10:30:00Z

# Wait for restoration
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-db-restored

# Update application to use restored database
kubectl set env deployment/order-service \
  DATABASE_HOST=ecommerce-prod-db-restored.xxx.rds.amazonaws.com
```

#### Snapshot Recovery

```bash
# Restore from specific snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-prod-db-restored \
  --db-snapshot-identifier ecommerce-prod-db-snapshot-2025-10-23

# Verify data integrity
psql -h ecommerce-prod-db-restored.xxx.rds.amazonaws.com \
     -U admin -d ecommerce \
     -f verify-data-integrity.sql
```

### Redis Cache Recovery

```bash
# Restore from snapshot
aws elasticache create-replication-group \
  --replication-group-id ecommerce-redis-restored \
  --replication-group-description "Restored Redis cluster" \
  --snapshot-name ecommerce-redis-snapshot-2025-10-23 \
  --cache-node-type cache.r5.large \
  --num-cache-clusters 3

# Update application configuration
kubectl set env deployment/order-service \
  REDIS_HOST=ecommerce-redis-restored.xxx.cache.amazonaws.com
```

### Application Configuration Recovery

```bash
# Restore ConfigMaps from backup
aws s3 cp s3://ecommerce-backups/k8s-config/2025-10-23/configmaps.yaml .
kubectl apply -f configmaps.yaml

# Restore Secrets from backup
aws s3 cp s3://ecommerce-backups/k8s-config/2025-10-23/secrets.yaml .
kubectl apply -f secrets.yaml

# Restart affected pods
kubectl rollout restart deployment/order-service -n order-context
```

## Disaster Recovery

### DR Strategy

**Multi-Region Setup**:

```text
Primary Region: us-east-1
DR Region: us-west-2

Replication:

  - RDS: Cross-region read replica
  - Redis: Backup snapshots copied to DR region
  - Kafka: MirrorMaker 2.0 replication
  - S3: Cross-region replication enabled

```

### DR Failover Procedure

**Step 1: Assess Situation**

```bash
# Check primary region status
aws rds describe-db-instances \
  --region us-east-1 \
  --db-instance-identifier ecommerce-prod-db

# Check DR region readiness
aws rds describe-db-instances \
  --region us-west-2 \
  --db-instance-identifier ecommerce-prod-db-replica
```

**Step 2: Promote DR Database**

```bash
# Promote read replica to standalone instance
aws rds promote-read-replica \
  --region us-west-2 \
  --db-instance-identifier ecommerce-prod-db-replica

# Wait for promotion
aws rds wait db-instance-available \
  --region us-west-2 \
  --db-instance-identifier ecommerce-prod-db-replica
```

**Step 3: Update DNS**

```bash
# Update Route 53 to point to DR region
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file://failover-dns-change.json
```

**Step 4: Deploy Application to DR Region**

```bash
# Deploy application stack to DR region
cdk deploy --region us-west-2 --all

# Verify application health
curl https://api-dr.ecommerce-platform.com/health
```

### DR Testing

**Quarterly DR Drill**:

```yaml
Schedule: Quarterly (January, April, July, October)
Duration: 4 hours
Scope: Full failover to DR region

Test Procedure:

  1. Announce DR drill to team
  2. Simulate primary region failure
  3. Execute failover procedure
  4. Verify application functionality
  5. Measure RTO and RPO
  6. Failback to primary region
  7. Document lessons learned
  
Success Criteria:

  - RTO < 1 hour
  - RPO < 15 minutes
  - All critical services operational
  - No data loss

```

## Complete Disaster Recovery Procedures

### DR Overview

**Disaster Recovery Objectives**:

```yaml
Recovery Time Objective (RTO): 1 hour
Recovery Point Objective (RPO): 15 minutes
Availability Target: 99.95% (4.38 hours downtime/year)

DR Scope:

  - Complete system recovery from catastrophic failure
  - Multi-region failover capability
  - Infrastructure recreation from CDK
  - Data synchronization and validation
  - Business continuity procedures

```

### Disaster Scenarios and Response

#### Scenario 1: Complete Primary Region Failure

**Trigger Conditions**:

- Primary region (us-east-1) completely unavailable
- Multiple availability zones affected
- AWS service outage confirmed
- Estimated recovery time > 1 hour

**Response Procedure**:

**Phase 1: Assessment and Declaration (0-15 minutes)**

```bash
#!/bin/bash
# dr-assessment.sh

set -e

echo "=== DISASTER RECOVERY ASSESSMENT ==="
echo "Timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"

# 1. Check primary region health
echo "Checking primary region (us-east-1) health..."
PRIMARY_HEALTH=$(aws health describe-events \
  --region us-east-1 \
  --filter eventTypeCategories=issue \
  --query 'events[?eventTypeCode==`AWS_EC2_OPERATIONAL_ISSUE`]' \
  --output json 2>&1 || echo "UNREACHABLE")

if [[ "$PRIMARY_HEALTH" == "UNREACHABLE" ]]; then
  echo "❌ PRIMARY REGION UNREACHABLE"
  DR_REQUIRED="YES"
else
  echo "✓ Primary region responding"
  DR_REQUIRED="NO"
fi

# 2. Check critical services
echo "Checking critical service availability..."
SERVICES=("RDS" "ElastiCache" "EKS" "MSK")
for SERVICE in "${SERVICES[@]}"; do
  STATUS=$(aws $SERVICE describe-* --region us-east-1 2>&1 || echo "FAILED")
  if [[ "$STATUS" == "FAILED" ]]; then
    echo "❌ $SERVICE unavailable"
  else
    echo "✓ $SERVICE available"
  fi
done

# 3. Check DR region readiness
echo "Checking DR region (us-west-2) readiness..."
DR_RDS=$(aws rds describe-db-instances \
  --region us-west-2 \
  --db-instance-identifier ecommerce-prod-db-replica \
  --query 'DBInstances[0].DBInstanceStatus' \
  --output text)

echo "DR RDS Status: $DR_RDS"

# 4. Calculate data lag
echo "Calculating replication lag..."
REPLICATION_LAG=$(aws rds describe-db-instances \
  --region us-west-2 \
  --db-instance-identifier ecommerce-prod-db-replica \
  --query 'DBInstances[0].StatusInfos[?StatusType==`read replication`].Status' \
  --output text)

echo "Replication Status: $REPLICATION_LAG"

# 5. Decision
if [[ "$DR_REQUIRED" == "YES" ]]; then
  echo ""
  echo "=== DISASTER RECOVERY REQUIRED ==="
  echo "Initiating DR failover to us-west-2"
  echo "Estimated RTO: 1 hour"
  echo "Estimated RPO: 15 minutes"
  
  # Send critical alert
  aws sns publish \
    --region us-west-2 \
    --topic-arn arn:aws:sns:us-west-2:123456789012:dr-critical-alerts \
    --subject "DISASTER RECOVERY INITIATED" \
    --message "Primary region failure detected. DR failover in progress."
  
  exit 0
else
  echo ""
  echo "=== NO DR REQUIRED ==="
  echo "Primary region operational"
  exit 1
fi
```

**Phase 2: Multi-Region Failover (15-45 minutes)**

```bash
#!/bin/bash
# dr-failover-complete.sh

set -e

DR_REGION="us-west-2"
PRIMARY_REGION="us-east-1"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
LOG_FILE="/var/log/dr-failover-${TIMESTAMP}.log"

exec > >(tee -a "$LOG_FILE") 2>&1

echo "=== DISASTER RECOVERY FAILOVER ==="
echo "Start Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "DR Region: $DR_REGION"

# Step 1: Promote RDS Read Replica (5 minutes)
echo ""
echo "Step 1: Promoting RDS read replica to primary..."
aws rds promote-read-replica \
  --region $DR_REGION \
  --db-instance-identifier ecommerce-prod-db-replica

echo "Waiting for RDS promotion to complete..."
aws rds wait db-instance-available \
  --region $DR_REGION \
  --db-instance-identifier ecommerce-prod-db-replica

RDS_ENDPOINT=$(aws rds describe-db-instances \
  --region $DR_REGION \
  --db-instance-identifier ecommerce-prod-db-replica \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "✓ RDS promoted. New endpoint: $RDS_ENDPOINT"

# Step 2: Restore Redis from latest snapshot (5 minutes)
echo ""
echo "Step 2: Restoring Redis cluster..."
LATEST_REDIS_SNAPSHOT=$(aws elasticache describe-snapshots \
  --region $DR_REGION \
  --query 'Snapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotName' \
  --output text)

aws elasticache create-replication-group \
  --region $DR_REGION \
  --replication-group-id ecommerce-redis-dr \
  --replication-group-description "DR Redis cluster" \
  --snapshot-name $LATEST_REDIS_SNAPSHOT \
  --cache-node-type cache.r5.large \
  --num-cache-clusters 3 \
  --automatic-failover-enabled \
  --multi-az-enabled

echo "Waiting for Redis cluster to be available..."
aws elasticache wait replication-group-available \
  --region $DR_REGION \
  --replication-group-id ecommerce-redis-dr

REDIS_ENDPOINT=$(aws elasticache describe-replication-groups \
  --region $DR_REGION \
  --replication-group-id ecommerce-redis-dr \
  --query 'ReplicationGroups[0].NodeGroups[0].PrimaryEndpoint.Address' \
  --output text)

echo "✓ Redis restored. Endpoint: $REDIS_ENDPOINT"

# Step 3: Deploy infrastructure with CDK (15 minutes)
echo ""
echo "Step 3: Deploying infrastructure to DR region..."
cd /opt/infrastructure

# Update CDK context for DR region
cat > cdk.context.json <<EOF
{
  "region": "$DR_REGION",
  "environment": "production-dr",
  "databaseEndpoint": "$RDS_ENDPOINT",
  "redisEndpoint": "$REDIS_ENDPOINT",
  "drMode": true
}
EOF

# Deploy all stacks
cdk deploy --region $DR_REGION --all --require-approval never

echo "✓ Infrastructure deployed to DR region"

# Step 4: Deploy application to EKS (10 minutes)
echo ""
echo "Step 4: Deploying application to DR EKS cluster..."

# Update kubeconfig for DR cluster
aws eks update-kubeconfig \
  --region $DR_REGION \
  --name ecommerce-eks-dr

# Apply Kubernetes manifests
kubectl apply -f /opt/k8s/base/
kubectl apply -f /opt/k8s/overlays/production-dr/

# Update environment variables
kubectl set env deployment/order-service \
  DATABASE_HOST=$RDS_ENDPOINT \
  REDIS_HOST=$REDIS_ENDPOINT \
  AWS_REGION=$DR_REGION

kubectl set env deployment/customer-service \
  DATABASE_HOST=$RDS_ENDPOINT \
  REDIS_HOST=$REDIS_ENDPOINT \
  AWS_REGION=$DR_REGION

kubectl set env deployment/product-service \
  DATABASE_HOST=$RDS_ENDPOINT \
  REDIS_HOST=$REDIS_ENDPOINT \
  AWS_REGION=$DR_REGION

# Wait for deployments to be ready
kubectl wait --for=condition=available --timeout=600s \
  deployment/order-service \
  deployment/customer-service \
  deployment/product-service

echo "✓ Application deployed and ready"

# Step 5: Update DNS to point to DR region (2 minutes)
echo ""
echo "Step 5: Updating Route 53 DNS records..."

DR_ALB=$(kubectl get svc ingress-nginx-controller \
  -n ingress-nginx \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

# Create DNS change batch
cat > /tmp/dns-change-batch.json <<EOF
{
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "api.ecommerce-platform.com",
        "Type": "CNAME",
        "TTL": 60,
        "ResourceRecords": [
          {
            "Value": "$DR_ALB"
          }
        ]
      }
    },
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "www.ecommerce-platform.com",
        "Type": "CNAME",
        "TTL": 60,
        "ResourceRecords": [
          {
            "Value": "$DR_ALB"
          }
        ]
      }
    }
  ]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file:///tmp/dns-change-batch.json

echo "✓ DNS updated to point to DR region"

# Step 6: Verify system health (3 minutes)
echo ""
echo "Step 6: Verifying system health..."

# Wait for DNS propagation
sleep 60

# Health check
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://api.ecommerce-platform.com/health)

if [[ "$HEALTH_STATUS" == "200" ]]; then
  echo "✓ Health check passed"
else
  echo "❌ Health check failed with status: $HEALTH_STATUS"
  exit 1
fi

# Test critical endpoints
echo "Testing critical endpoints..."
curl -s https://api.ecommerce-platform.com/api/v1/orders | jq .
curl -s https://api.ecommerce-platform.com/api/v1/customers | jq .
curl -s https://api.ecommerce-platform.com/api/v1/products | jq .

echo "✓ All critical endpoints responding"

# Step 7: Enable monitoring and alerting
echo ""
echo "Step 7: Enabling monitoring and alerting..."

# Update CloudWatch dashboards
aws cloudwatch put-dashboard \
  --region $DR_REGION \
  --dashboard-name ecommerce-production-dr \
  --dashboard-body file:///opt/monitoring/dr-dashboard.json

# Update SNS topics
aws sns publish \
  --region $DR_REGION \
  --topic-arn arn:aws:sns:$DR_REGION:123456789012:operations-alerts \
  --subject "DR Failover Complete" \
  --message "System successfully failed over to DR region $DR_REGION"

echo "✓ Monitoring and alerting enabled"

# Final summary
echo ""
echo "=== DISASTER RECOVERY FAILOVER COMPLETE ==="
echo "End Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "DR Region: $DR_REGION"
echo "RDS Endpoint: $RDS_ENDPOINT"
echo "Redis Endpoint: $REDIS_ENDPOINT"
echo "Application URL: https://api.ecommerce-platform.com"
echo ""
echo "Next Steps:"
echo "1. Monitor system performance and errors"
echo "2. Verify data integrity"
echo "3. Communicate status to stakeholders"
echo "4. Plan for failback when primary region recovers"
```

**Phase 3: Data Synchronization and Validation (45-60 minutes)**

```bash
#!/bin/bash
# dr-data-validation.sh

set -e

DR_REGION="us-west-2"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)

echo "=== DATA VALIDATION POST-FAILOVER ==="

# 1. Database integrity checks
echo "Running database integrity checks..."
psql -h $RDS_ENDPOINT -U admin -d ecommerce <<EOF
-- Check table counts
SELECT 'orders' as table_name, COUNT(*) as row_count FROM orders
UNION ALL
SELECT 'customers', COUNT(*) FROM customers
UNION ALL
SELECT 'products', COUNT(*) FROM products;

-- Check referential integrity
SELECT 
  'Order-Customer Consistency' as check_name,
  COUNT(*) as issues
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
WHERE c.id IS NULL;

-- Check for data corruption
SELECT 
  'Null Primary Keys' as check_name,
  COUNT(*) as corrupt_records
FROM orders
WHERE id IS NULL;

-- Verify recent transactions
SELECT 
  'Recent Orders (last hour)' as check_name,
  COUNT(*) as count
FROM orders
WHERE created_at > NOW() - INTERVAL '1 hour';
EOF

# 2. Redis data verification
echo "Verifying Redis data..."
REDIS_KEY_COUNT=$(redis-cli -h $REDIS_ENDPOINT -p 6379 DBSIZE | awk '{print $2}')
echo "Redis key count: $REDIS_KEY_COUNT"

# 3. Kafka topic verification
echo "Verifying Kafka topics..."
kafka-topics.sh --bootstrap-server kafka-broker:9092 --list

# 4. Application state verification
echo "Verifying application state..."
kubectl get pods --all-namespaces
kubectl get deployments --all-namespaces

echo "✓ Data validation complete"
```

#### Scenario 2: Database Corruption or Data Loss

**Trigger Conditions**:

- Database corruption detected
- Accidental data deletion
- Ransomware attack
- Data integrity violations

**Response Procedure**:

```bash
#!/bin/bash
# dr-database-recovery.sh

set -e

RECOVERY_POINT=$1  # Format: YYYY-MM-DDTHH:MM:SSZ
RECOVERY_TYPE=$2   # snapshot or point-in-time

if [ -z "$RECOVERY_POINT" ] || [ -z "$RECOVERY_TYPE" ]; then
  echo "Usage: $0 <recovery-point> <snapshot|point-in-time>"
  exit 1
fi

echo "=== DATABASE RECOVERY ==="
echo "Recovery Point: $RECOVERY_POINT"
echo "Recovery Type: $RECOVERY_TYPE"

# 1. Create backup of current state (even if corrupted)
echo "Creating backup of current state..."
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier ecommerce-prod-db-pre-recovery-$(date +%s)

# 2. Restore database
if [[ "$RECOVERY_TYPE" == "point-in-time" ]]; then
  echo "Performing point-in-time recovery..."
  aws rds restore-db-instance-to-point-in-time \
    --source-db-instance-identifier ecommerce-prod-db \
    --target-db-instance-identifier ecommerce-prod-db-recovered \
    --restore-time $RECOVERY_POINT
else
  echo "Restoring from snapshot..."
  SNAPSHOT_ID=$(aws rds describe-db-snapshots \
    --db-instance-identifier ecommerce-prod-db \
    --query "DBSnapshots[?SnapshotCreateTime<='$RECOVERY_POINT'] | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier" \
    --output text)
  
  aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier ecommerce-prod-db-recovered \
    --db-snapshot-identifier $SNAPSHOT_ID
fi

# 3. Wait for recovery
echo "Waiting for database recovery..."
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-db-recovered

# 4. Validate recovered data
echo "Validating recovered data..."
RECOVERED_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ecommerce-prod-db-recovered \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

psql -h $RECOVERED_ENDPOINT -U admin -d ecommerce -f /opt/scripts/data-validation.sql

# 5. Switch application to recovered database
echo "Switching application to recovered database..."
kubectl set env deployment/order-service DATABASE_HOST=$RECOVERED_ENDPOINT
kubectl set env deployment/customer-service DATABASE_HOST=$RECOVERED_ENDPOINT
kubectl set env deployment/product-service DATABASE_HOST=$RECOVERED_ENDPOINT

# 6. Monitor for issues
echo "Monitoring application health..."
sleep 60
kubectl get pods --all-namespaces | grep -v Running

echo "✓ Database recovery complete"
```

#### Scenario 3: Infrastructure Recreation from CDK

**Trigger Conditions**:

- Complete infrastructure loss
- Account compromise requiring rebuild
- Migration to new AWS account
- Infrastructure drift correction

**Response Procedure**:

```bash
#!/bin/bash
# dr-infrastructure-recreation.sh

set -e

TARGET_REGION=$1
ENVIRONMENT=$2

if [ -z "$TARGET_REGION" ] || [ -z "$ENVIRONMENT" ]; then
  echo "Usage: $0 <region> <environment>"
  exit 1
fi

echo "=== INFRASTRUCTURE RECREATION ==="
echo "Target Region: $TARGET_REGION"
echo "Environment: $ENVIRONMENT"

# 1. Prepare CDK environment
echo "Preparing CDK environment..."
cd /opt/infrastructure

# Install dependencies
npm install

# Bootstrap CDK (if needed)
cdk bootstrap aws://123456789012/$TARGET_REGION

# 2. Retrieve configuration from backups
echo "Retrieving configuration from S3..."
aws s3 cp s3://ecommerce-backups-prod-us-east-1/infrastructure/latest/ \
  /tmp/infrastructure-config/ \
  --recursive

# 3. Deploy network stack
echo "Deploying network stack..."
cdk deploy NetworkStack \
  --region $TARGET_REGION \
  --context environment=$ENVIRONMENT \
  --require-approval never

# 4. Deploy security stack
echo "Deploying security stack..."
cdk deploy SecurityStack \
  --region $TARGET_REGION \
  --context environment=$ENVIRONMENT \
  --require-approval never

# 5. Deploy database stack
echo "Deploying database stack..."
cdk deploy DatabaseStack \
  --region $TARGET_REGION \
  --context environment=$ENVIRONMENT \
  --require-approval never

# Wait for RDS to be available
RDS_IDENTIFIER=$(aws rds describe-db-instances \
  --region $TARGET_REGION \
  --query "DBInstances[?DBInstanceIdentifier contains(@, 'ecommerce')].DBInstanceIdentifier" \
  --output text)

aws rds wait db-instance-available \
  --region $TARGET_REGION \
  --db-instance-identifier $RDS_IDENTIFIER

# 6. Restore database from backup
echo "Restoring database from backup..."
LATEST_SNAPSHOT=$(aws rds describe-db-snapshots \
  --region $TARGET_REGION \
  --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].DBSnapshotIdentifier' \
  --output text)

if [ -n "$LATEST_SNAPSHOT" ]; then
  aws rds restore-db-instance-from-db-snapshot \
    --region $TARGET_REGION \
    --db-instance-identifier $RDS_IDENTIFIER-restored \
    --db-snapshot-identifier $LATEST_SNAPSHOT
fi

# 7. Deploy cache stack
echo "Deploying cache stack..."
cdk deploy CacheStack \
  --region $TARGET_REGION \
  --context environment=$ENVIRONMENT \
  --require-approval never

# 8. Deploy EKS stack
echo "Deploying EKS stack..."
cdk deploy EKSStack \
  --region $TARGET_REGION \
  --context environment=$ENVIRONMENT \
  --require-approval never

# Wait for EKS cluster
EKS_CLUSTER=$(aws eks list-clusters \
  --region $TARGET_REGION \
  --query 'clusters[0]' \
  --output text)

aws eks wait cluster-active \
  --region $TARGET_REGION \
  --name $EKS_CLUSTER

# 9. Deploy observability stack
echo "Deploying observability stack..."
cdk deploy ObservabilityStack \
  --region $TARGET_REGION \
  --context environment=$ENVIRONMENT \
  --require-approval never

# 10. Verify all stacks
echo "Verifying all stacks..."
cdk list --region $TARGET_REGION

echo "✓ Infrastructure recreation complete"
```

### Business Continuity Procedures

#### Communication Plan During DR

**Stakeholder Communication Matrix**:

```yaml
Internal Stakeholders:
  Executive Team:
    Notification: Immediate (within 5 minutes)
    Method: Phone call + SMS
    Frequency: Every 30 minutes until resolved
    Contact: CEO, CTO, COO
    
  Engineering Team:
    Notification: Immediate (within 2 minutes)
    Method: PagerDuty + Slack
    Frequency: Continuous updates in #incident-response
    Contact: All on-call engineers
    
  Operations Team:
    Notification: Immediate (within 2 minutes)
    Method: PagerDuty + Slack
    Frequency: Continuous updates
    Contact: All operations staff
    
  Customer Support:
    Notification: Within 10 minutes
    Method: Email + Slack
    Frequency: Every hour
    Contact: Support team leads

External Stakeholders:
  Customers:
    Notification: Within 30 minutes
    Method: Status page + Email
    Frequency: Every 2 hours
    Message Template: "We are experiencing technical difficulties..."
    
  Partners:
    Notification: Within 1 hour
    Method: Email + API status endpoint
    Frequency: Every 4 hours
    Contact: Integration partners
    
  Regulatory Bodies:
    Notification: Within 24 hours (if required)
    Method: Formal notification
    Frequency: As required
    Contact: Compliance team
```

**Communication Templates**:

```markdown
# Internal Alert Template
Subject: [CRITICAL] Disaster Recovery Initiated - Primary Region Failure

Team,

We have initiated disaster recovery procedures due to a complete failure of our primary region (us-east-1).

Status: DR Failover in Progress
Estimated Recovery Time: 1 hour
Current Phase: [Phase name]
Impact: All services temporarily unavailable

Actions Required:

- Engineering: Monitor DR failover progress
- Operations: Prepare for post-recovery validation
- Support: Prepare customer communications

Next Update: [Time]

Incident Commander: [Name]
War Room: #incident-response

---

# Customer Communication Template
Subject: Service Disruption - We're Working to Restore Service

Dear Valued Customer,

We are currently experiencing a service disruption affecting our e-commerce platform. Our team is actively working to restore full service.

What We Know:

- Issue detected at: [Time]
- Estimated resolution: [Time]
- Affected services: [List]

What We're Doing:

- Activated disaster recovery procedures
- Failing over to backup systems
- Monitoring restoration progress

What You Can Do:

- Check status.ecommerce-platform.com for updates
- Contact support@ecommerce-platform.com for urgent issues

We apologize for the inconvenience and appreciate your patience.

Best regards,
E-Commerce Platform Team
```

#### DR Drill Procedures and Schedules

**Annual DR Drill Schedule**:

```yaml
Q1 Drill (January):
  Type: Tabletop Exercise
  Duration: 2 hours
  Scope: Walk through DR procedures
  Participants: All teams
  Objectives:

    - Review DR procedures
    - Identify gaps in documentation
    - Update contact lists
    - Verify backup locations
    
Q2 Drill (April):
  Type: Partial Failover Test
  Duration: 4 hours
  Scope: Failover non-production environment
  Participants: Engineering + Operations
  Objectives:

    - Test RDS promotion
    - Test infrastructure deployment
    - Measure RTO/RPO
    - Validate monitoring
    
Q3 Drill (July):
  Type: Full DR Simulation
  Duration: 8 hours
  Scope: Complete failover to DR region
  Participants: All teams
  Objectives:

    - Execute complete DR procedure
    - Test all recovery scenarios
    - Validate business continuity
    - Test communication plan
    
Q4 Drill (October):
  Type: Surprise DR Drill
  Duration: 6 hours
  Scope: Unannounced failover test
  Participants: On-call teams
  Objectives:

    - Test real-world response
    - Measure actual RTO/RPO
    - Identify training needs
    - Update procedures

```

**DR Drill Execution Checklist**:

```markdown
# Pre-Drill (1 week before)

- [ ] Schedule drill date and time
- [ ] Notify all participants
- [ ] Prepare test environment
- [ ] Review DR procedures
- [ ] Assign roles and responsibilities
- [ ] Prepare monitoring dashboards
- [ ] Set up communication channels

# During Drill

- [ ] Start timer for RTO measurement
- [ ] Execute DR procedures step-by-step
- [ ] Document all actions taken
- [ ] Record any issues or delays
- [ ] Measure RPO (data loss)
- [ ] Test all critical systems
- [ ] Verify data integrity
- [ ] Test application functionality
- [ ] Validate monitoring and alerting

# Post-Drill (within 24 hours)

- [ ] Calculate actual RTO and RPO
- [ ] Document lessons learned
- [ ] Identify procedure gaps
- [ ] Update DR documentation
- [ ] Schedule remediation tasks
- [ ] Share results with stakeholders
- [ ] Update training materials

```

### Post-DR Validation Checklist

**System Validation**:

```yaml
Infrastructure Validation:

  - [ ] All EKS nodes healthy and ready
  - [ ] RDS database accessible and responsive
  - [ ] Redis cache operational
  - [ ] Kafka cluster healthy
  - [ ] Load balancers distributing traffic
  - [ ] Auto-scaling groups configured
  - [ ] Security groups properly configured
  - [ ] IAM roles and policies applied
  - [ ] KMS keys accessible
  - [ ] VPC peering connections active

Application Validation:

  - [ ] All pods running and ready
  - [ ] Health checks passing
  - [ ] API endpoints responding
  - [ ] Authentication working
  - [ ] Authorization rules enforced
  - [ ] Session management functional
  - [ ] Cache hit rates normal
  - [ ] Message queues processing
  - [ ] Background jobs running
  - [ ] Scheduled tasks executing

Data Validation:

  - [ ] Database integrity checks passed
  - [ ] No orphaned records
  - [ ] Referential integrity maintained
  - [ ] Recent transactions present
  - [ ] Data counts match expectations
  - [ ] Checksums verified
  - [ ] No data corruption detected
  - [ ] Replication lag acceptable
  - [ ] Backup processes running
  - [ ] Archive data accessible

Monitoring Validation:

  - [ ] CloudWatch metrics flowing
  - [ ] Logs being collected
  - [ ] Alerts configured and firing
  - [ ] Dashboards displaying data
  - [ ] Traces being captured
  - [ ] APM agents reporting
  - [ ] Synthetic monitors passing
  - [ ] Status page updated
  - [ ] Incident tracking active
  - [ ] On-call rotations updated

Security Validation:

  - [ ] TLS certificates valid
  - [ ] Secrets accessible
  - [ ] Encryption at rest enabled
  - [ ] Encryption in transit enforced
  - [ ] WAF rules active
  - [ ] DDoS protection enabled
  - [ ] Security groups locked down
  - [ ] Audit logging enabled
  - [ ] Compliance checks passing
  - [ ] Vulnerability scans clean

Business Validation:

  - [ ] Order processing functional
  - [ ] Payment processing working
  - [ ] Customer registration active
  - [ ] Product catalog accessible
  - [ ] Search functionality working
  - [ ] Email notifications sending
  - [ ] SMS notifications sending
  - [ ] Inventory updates processing
  - [ ] Reporting queries running
  - [ ] Analytics data flowing

```

**Performance Validation**:

```bash
#!/bin/bash
# dr-performance-validation.sh

set -e

echo "=== POST-DR PERFORMANCE VALIDATION ==="

# 1. API response time validation
echo "Testing API response times..."
for ENDPOINT in "/api/v1/orders" "/api/v1/customers" "/api/v1/products"; do
  RESPONSE_TIME=$(curl -o /dev/null -s -w '%{time_total}' https://api.ecommerce-platform.com$ENDPOINT)
  echo "$ENDPOINT: ${RESPONSE_TIME}s"
  
  if (( $(echo "$RESPONSE_TIME > 2.0" | bc -l) )); then
    echo "❌ Response time exceeds 2s threshold"
  else
    echo "✓ Response time acceptable"
  fi
done

# 2. Database query performance
echo "Testing database query performance..."
psql -h $RDS_ENDPOINT -U admin -d ecommerce <<EOF
EXPLAIN ANALYZE SELECT * FROM orders WHERE customer_id = 'CUST-001' LIMIT 10;
EXPLAIN ANALYZE SELECT * FROM products WHERE category = 'Electronics' LIMIT 20;
EOF

# 3. Cache hit rate
echo "Checking Redis cache hit rate..."
CACHE_STATS=$(redis-cli -h $REDIS_ENDPOINT -p 6379 INFO stats | grep keyspace)
echo "$CACHE_STATS"

# 4. Load test
echo "Running load test..."
ab -n 1000 -c 10 https://api.ecommerce-platform.com/api/v1/products

echo "✓ Performance validation complete"
```

## Backup Monitoring and Alerting

### CloudWatch Metrics

**Backup Operation Metrics**:

```yaml
Custom Metrics:
  backup.rds.snapshot.created:
    Type: Counter
    Dimensions: [Environment, SnapshotType]
    Unit: Count
    
  backup.rds.snapshot.duration:
    Type: Timer
    Dimensions: [Environment, DatabaseSize]
    Unit: Seconds
    
  backup.rds.snapshot.size:
    Type: Gauge
    Dimensions: [Environment, SnapshotId]
    Unit: Gigabytes
    
  backup.redis.snapshot.created:
    Type: Counter
    Dimensions: [Environment, ClusterId]
    Unit: Count
    
  backup.s3.object.uploaded:
    Type: Counter
    Dimensions: [Environment, BackupType, StorageClass]
    Unit: Count
    
  backup.verification.success:
    Type: Counter
    Dimensions: [Environment, BackupType]
    Unit: Count
    
  backup.verification.duration:
    Type: Timer
    Dimensions: [Environment, BackupType]
    Unit: Seconds
    
  backup.replication.lag:
    Type: Gauge
    Dimensions: [SourceRegion, DestinationRegion]
    Unit: Seconds
    
  backup.cost.daily:
    Type: Gauge
    Dimensions: [Environment, Service]
    Unit: USD
```

### CloudWatch Alarms

**Critical Alarms (P0)**:

```yaml
RDS Backup Failure:
  Metric: backup.rds.snapshot.created
  Condition: Sum < 1 in 1 hour
  Threshold: 0
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:critical-alerts
    - PagerDuty: High Priority

  Description: "RDS automated backup failed"
  
RDS Snapshot Age:
  Metric: AWS/RDS DBSnapshotAge
  Condition: Maximum > 86400 seconds (24 hours)
  Threshold: 86400
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:critical-alerts
    - PagerDuty: High Priority

  Description: "No recent RDS snapshot available"
  
Cross-Region Replication Failure:
  Metric: AWS/S3 ReplicationLatency
  Condition: Maximum > 3600 seconds (1 hour)
  Threshold: 3600
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:critical-alerts
    - PagerDuty: High Priority

  Description: "Cross-region replication lag exceeded 1 hour"
```

**Warning Alarms (P1)**:

```yaml
Redis Snapshot Failure:
  Metric: backup.redis.snapshot.created
  Condition: Sum < 1 in 2 hours
  Threshold: 0
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:warning-alerts

  Description: "Redis snapshot creation failed"
  
Backup Verification Failure:
  Metric: backup.verification.success
  Condition: Sum < 1 in 7 days
  Threshold: 0
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:warning-alerts

  Description: "Weekly backup verification not completed"
  
S3 Upload Failure:
  Metric: backup.s3.object.uploaded
  Condition: Sum < expected count in 1 day
  Threshold: varies by backup type
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:warning-alerts

  Description: "S3 backup upload incomplete"
  
Backup Cost Anomaly:
  Metric: backup.cost.daily
  Condition: > 120% of 7-day average
  Threshold: Dynamic (based on average)
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:cost-alerts

  Description: "Backup costs increased significantly"
```

**Informational Alarms (P2)**:

```yaml
Backup Duration Increase:
  Metric: backup.rds.snapshot.duration
  Condition: Average > 3600 seconds (1 hour)
  Threshold: 3600
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:info-alerts

  Description: "Backup duration increased, may need optimization"
  
Storage Class Transition:
  Metric: AWS/S3 StorageClassTransitions
  Condition: Sum > 0
  Threshold: 0
  Actions:

    - SNS: arn:aws:sns:us-east-1:123456789012:info-alerts

  Description: "Objects transitioned to different storage class"
```

### Backup Verification Schedule

| Backup Type | Verification Frequency | Method | Duration | Success Criteria |
|-------------|----------------------|---------|----------|------------------|
| RDS Automated | Weekly (Sunday 02:00 UTC) | Full restore to test instance | 30 minutes | Data integrity checks pass |
| RDS Manual | Before use | Metadata validation | 5 minutes | Snapshot status = available |
| RDS Export (S3) | Monthly | Download and verify checksums | 1 hour | MD5 checksums match |
| Redis | Monthly (1st of month) | Restore to test cluster | 15 minutes | Key count matches |
| Configuration | Weekly (Monday 01:00 UTC) | Apply to test namespace | 10 minutes | All resources created |
| Cross-Region | Daily | Replication lag check | 2 minutes | Lag < 15 minutes |
| S3 Objects | Daily | Object count and size | 5 minutes | Matches expected values |

## Backup Testing Procedures

> **🧪 Comprehensive Backup Testing**: For detailed backup testing procedures including monthly restore testing, quarterly DR drills, backup integrity verification, restore performance benchmarking, automated testing framework, test result documentation, and lessons learned tracking, see [Backup Testing Procedures](backup-testing-procedures.md).

**Quick Reference**:

- **Monthly Testing**: Database, application state, and infrastructure backup restore tests
- **Quarterly DR Drills**: Full disaster recovery simulations with RTO/RPO measurement
- **Integrity Verification**: Daily checksum validation and weekly consistency checks
- **Performance Benchmarking**: Restore performance metrics and trend analysis
- **Automated Framework**: AWS Step Functions workflows with Lambda validation
- **Results Tracking**: Comprehensive test results database and dashboard

## Data Retention Policy

### Retention Periods

```yaml
Production Data:
  Database Backups:
    Automated: 30 days
    Manual: 90 days
    Compliance: 7 years (archived to Glacier)
    
  Cache Snapshots:
    Automated: 7 days
    Manual: 30 days
    
  Application Logs:
    Hot Storage (CloudWatch): 30 days
    Cold Storage (S3): 1 year
    Archive (Glacier): 7 years
    
  Configuration Backups:
    Active: 90 days
    Archive: 1 year
    
  Kafka Messages:
    Retention: 7 days
    Archive (S3): 90 days
```

## Compliance and Audit

### Backup Compliance

**Requirements**:

- SOC 2 Type II compliance
- PCI DSS compliance (payment data)
- GDPR compliance (EU customer data)

**Audit Trail**:

```yaml
Logged Events:

  - Backup creation
  - Backup deletion
  - Restore operations
  - Configuration changes
  - Access to backup data
  
Audit Log Retention: 7 years
Audit Log Location: CloudWatch Logs + S3
```

## Related Documentation

- [Detailed Restore Procedures](detailed-restore-procedures.md) - Comprehensive step-by-step restore workflows
- [Operational Overview](overview.md) - Overall operational approach
- [Monitoring and Alerting](monitoring-alerting.md) - Monitoring strategies
- [Operational Procedures](procedures.md) - Step-by-step procedures
- [Physical Architecture](../deployment/physical-architecture.md) - Infrastructure details

---

**Document Version**: 1.1  
**Last Updated**: 2025-10-26  
**Owner**: Operations Team
