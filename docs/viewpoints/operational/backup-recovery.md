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

## Database Backup Strategy

### RDS Automated Backups

**Configuration**:
```yaml
Automated Backups:
  Enabled: true
  Retention Period: 30 days
  Backup Window: 03:00-04:00 UTC (off-peak)
  Backup Type: Automated snapshots
  
Point-in-Time Recovery:
  Enabled: true
  Retention: 30 days
  Granularity: 5 minutes
  
Multi-AZ Deployment:
  Enabled: true
  Synchronous Replication: us-east-1a → us-east-1b
  Automatic Failover: Yes
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
  Encryption: AES-256
```

**Backup Script**:
```bash
#!/bin/bash
# backup-k8s-config.sh

DATE=$(date +%Y-%m-%d)
BACKUP_DIR="s3://ecommerce-backups/k8s-config/$DATE"

# Backup ConfigMaps
kubectl get configmaps --all-namespaces -o yaml > configmaps.yaml
aws s3 cp configmaps.yaml $BACKUP_DIR/

# Backup Secrets (encrypted)
kubectl get secrets --all-namespaces -o yaml > secrets.yaml
aws s3 cp secrets.yaml $BACKUP_DIR/ --sse AES256

# Backup Deployments
kubectl get deployments --all-namespaces -o yaml > deployments.yaml
aws s3 cp deployments.yaml $BACKUP_DIR/

# Backup Services
kubectl get services --all-namespaces -o yaml > services.yaml
aws s3 cp services.yaml $BACKUP_DIR/
```

## Recovery Procedures

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
```
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

## Backup Monitoring

### Backup Health Checks

```yaml
CloudWatch Alarms:
  - RDS Backup Failed
  - Redis Snapshot Failed
  - Configuration Backup Failed
  - Cross-Region Replication Lag > 1 hour
  
Metrics:
  - backup.rds.success (counter)
  - backup.rds.duration (timer)
  - backup.redis.success (counter)
  - backup.config.success (counter)
  - replication.lag.seconds (gauge)
```

### Backup Verification Schedule

| Backup Type | Verification Frequency | Method |
|-------------|----------------------|---------|
| RDS Automated | Weekly | Restore to test instance |
| RDS Manual | Before use | Metadata check |
| Redis | Monthly | Restore to test cluster |
| Configuration | Weekly | Apply to test environment |
| Cross-Region | Monthly | Verify replication lag |

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

- [Operational Overview](overview.md) - Overall operational approach
- [Monitoring and Alerting](monitoring-alerting.md) - Monitoring strategies
- [Operational Procedures](procedures.md) - Step-by-step procedures
- [Physical Architecture](../deployment/physical-architecture.md) - Infrastructure details

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-23  
**Owner**: Operations Team
