# Runbook: Backup and Restore Operations

## Overview

This runbook covers database backup and restore procedures for the Enterprise E-Commerce Platform.

## Backup Procedures

### Automated Backups

Automated backups run according to schedule:

- **Production**: Hourly snapshots, 30-day retention
- **Staging**: Daily snapshots, 7-day retention

### Manual Backup

#### Create Manual Snapshot

```bash
# Create RDS snapshot
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-production \
  --db-snapshot-identifier ecommerce-prod-manual-$(date +%Y%m%d-%H%M%S)

# Wait for snapshot to complete
aws rds wait db-snapshot-available \
  --db-snapshot-identifier ecommerce-prod-manual-$(date +%Y%m%d-%H%M%S)

# Verify snapshot
aws rds describe-db-snapshots \
  --db-snapshot-identifier ecommerce-prod-manual-$(date +%Y%m%d-%H%M%S)
```

#### Export Database Dump

```bash
# Create logical backup
kubectl exec -it ${POD_NAME} -n production -- \
  pg_dump -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -F c -f /tmp/backup-$(date +%Y%m%d).dump

# Copy backup locally
kubectl cp production/${POD_NAME}:/tmp/backup-$(date +%Y%m%d).dump \
  ./backup-$(date +%Y%m%d).dump

# Upload to S3
aws s3 cp ./backup-$(date +%Y%m%d).dump \
  s3://ecommerce-backups/database/backup-$(date +%Y%m%d).dump
```

## Restore Procedures

### Restore from RDS Snapshot

#### Step 1: List Available Snapshots

```bash
# List recent snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-production \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime]' \
  --output table | head -20
```

#### Step 2: Stop Application

```bash
# Scale down to prevent writes
kubectl scale deployment/ecommerce-backend --replicas=0 -n production
```

#### Step 3: Restore Snapshot

```bash
# Restore to new instance
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-prod-restored \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --db-instance-class db.r5.xlarge \
  --multi-az

# Wait for restore to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-restored
```

#### Step 4: Update Application Configuration

```bash
# Update database endpoint in Secrets Manager
aws secretsmanager update-secret \
  --secret-id production/ecommerce/config \
  --secret-string '{
    "DB_HOST": "ecommerce-prod-restored.xxxxx.rds.amazonaws.com",
    "DB_NAME": "ecommerce_production",
    "DB_USERNAME": "ecommerce_app",
    "DB_PASSWORD": "..."
  }'
```

#### Step 5: Restart Application

```bash
# Scale up application
kubectl scale deployment/ecommerce-backend --replicas=4 -n production

# Verify connectivity
kubectl logs -f deployment/ecommerce-backend -n production
```

### Restore from Database Dump

#### Step 1: Download Backup

```bash
# Download from S3
aws s3 cp s3://ecommerce-backups/database/${BACKUP_FILE} \
  ./restore-backup.dump
```

#### Step 2: Stop Application

```bash
kubectl scale deployment/ecommerce-backend --replicas=0 -n production
```

#### Step 3: Restore Database

```bash
# Copy dump to pod
kubectl cp ./restore-backup.dump \
  production/${POD_NAME}:/tmp/restore-backup.dump

# Restore database
kubectl exec -it ${POD_NAME} -n production -- \
  pg_restore -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c -F c /tmp/restore-backup.dump
```

#### Step 4: Verify Restore

```bash
# Check table counts
kubectl exec -it ${POD_NAME} -n production -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} \
  -c "SELECT schemaname, tablename, n_live_tup 
      FROM pg_stat_user_tables 
      ORDER BY n_live_tup DESC;"

# Verify data integrity
./scripts/verify-database-integrity.sh
```

#### Step 5: Restart Application

```bash
kubectl scale deployment/ecommerce-backend --replicas=4 -n production
```

## Point-in-Time Recovery

### Restore to Specific Time

```bash
# Restore to specific timestamp
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ecommerce-production \
  --target-db-instance-identifier ecommerce-prod-pitr \
  --restore-time 2025-10-25T10:30:00Z \
  --db-instance-class db.r5.xlarge

# Wait for restore
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-pitr
```

## Verification Checklist

### After Restore

- [ ] Database is accessible
- [ ] All tables present
- [ ] Row counts match expectations
- [ ] Foreign key constraints intact
- [ ] Application can connect
- [ ] Critical queries execute successfully
- [ ] Business logic functions correctly
- [ ] No data corruption detected

### Data Integrity Checks

```sql
-- Check for orphaned records
SELECT COUNT(*) FROM orders WHERE customer_id NOT IN (SELECT id FROM customers);

-- Verify referential integrity
SELECT conname, conrelid::regclass, confrelid::regclass
FROM pg_constraint
WHERE contype = 'f' AND convalidated = false;

-- Check for duplicate primary keys
SELECT id, COUNT(*) FROM orders GROUP BY id HAVING COUNT(*) > 1;
```

## Backup Testing

### Monthly Backup Test

1. Restore backup to test environment
2. Verify data integrity
3. Run application tests
4. Document results
5. Update procedures if needed

## Backup Retention Policy

| Environment | Frequency | Retention | Type |
|-------------|-----------|-----------|------|
| Production | Hourly | 7 days | Automated snapshot |
| Production | Daily | 30 days | Automated snapshot |
| Production | Weekly | 90 days | Manual snapshot |
| Staging | Daily | 7 days | Automated snapshot |

## Disaster Recovery

### RTO and RPO Targets

- **RTO** (Recovery Time Objective): < 1 hour
- **RPO** (Recovery Point Objective): < 15 minutes

### DR Procedure

1. Assess situation and impact
2. Notify stakeholders
3. Activate DR plan
4. Restore from most recent backup
5. Verify data integrity
6. Resume operations
7. Conduct post-mortem

## Escalation

- **L1 Support**: DevOps team
- **L2 Support**: Database administrator
- **AWS Support**: For RDS-specific issues

## Related

- [Database Connection Issues](database-connection-issues.md)
- [Data Inconsistency](data-inconsistency.md)
- [Rollback Procedures](../deployment/rollback.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team
