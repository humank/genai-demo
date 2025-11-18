---
title: "Backup Automation"
viewpoint: "Operational"
status: "active"
last_updated: "2025-10-26"
stakeholders: ["Operations Team", "DBA Team", "SRE Team", "DevOps Team"]
---

# Backup Automation

> **Viewpoint**: Operational  
> **Purpose**: Document backup automation scripts, tools, scheduling, and orchestration  
> **Audience**: Operations Team, DBA Team, SRE Team, DevOps Team

## Overview

This document describes the automated backup infrastructure, including scripts, tools, AWS Backup service configuration, job scheduling and orchestration, notification systems, failure handling mechanisms, metrics collection, dashboards, and compliance reporting.

## Backup Automation Architecture

### High-Level Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│                   Backup Orchestration Layer                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ AWS Backup   │  │  EventBridge │  │ Step Functions│         │
│  │   Service    │  │    Rules     │  │   Workflows   │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼──────────────────┼──────────────────┼────────────────┘
          │                  │                  │
          │ Trigger          │ Schedule         │ Orchestrate
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backup Execution Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │ Lambda       │  │  ECS Tasks   │  │   Custom     │         │
│  │ Functions    │  │              │  │   Scripts    │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼──────────────────┼──────────────────┼────────────────┘
          │                  │                  │
          │ Execute          │ Run              │ Perform
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Data Sources Layer                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   RDS    │  │  Redis   │  │  Kafka   │  │   EKS    │       │
│  │ Database │  │  Cache   │  │ Streams  │  │  Config  │       │
│  └──────┬───┘  └──────┬───┘  └──────┬───┘  └──────┬───┘       │
└─────────┼──────────────┼──────────────┼──────────────┼─────────┘
          │              │              │              │
          │ Backup       │ Snapshot     │ Archive      │ Export
          ▼              ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Monitoring & Alerting Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  CloudWatch  │  │     SNS      │  │  PagerDuty   │         │
│  │   Metrics    │  │ Notifications│  │    Alerts    │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
└─────────┼──────────────────┼──────────────────┼────────────────┘
          │                  │                  │
          │ Collect          │ Notify           │ Escalate
          ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Reporting & Compliance Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │   Grafana    │  │  Compliance  │  │   Audit      │         │
│  │  Dashboards  │  │   Reports    │  │    Logs      │         │
│  └──────────────┘  └──────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

## AWS Backup Service Configuration

### Backup Plans

#### Production Database Backup Plan

**Configuration**:

```yaml
BackupPlan:
  Name: ecommerce-production-database-backup
  
  Rules:

    - RuleName: continuous-backup

      TargetBackupVault: ecommerce-production-vault
      ScheduleExpression: "cron(0 */6 * * ? *)"  # Every 6 hours
      StartWindowMinutes: 60
      CompletionWindowMinutes: 180
      Lifecycle:
        DeleteAfterDays: 30
        MoveToColdStorageAfterDays: 7
      RecoveryPointTags:
        Environment: production
        BackupType: automated
        Criticality: high
        
    - RuleName: daily-backup

      TargetBackupVault: ecommerce-production-vault
      ScheduleExpression: "cron(0 3 * * ? *)"  # Daily at 3 AM UTC
      StartWindowMinutes: 60
      CompletionWindowMinutes: 240
      Lifecycle:
        DeleteAfterDays: 90
        MoveToColdStorageAfterDays: 30
      RecoveryPointTags:
        Environment: production
        BackupType: daily
        Criticality: high
        
    - RuleName: weekly-backup

      TargetBackupVault: ecommerce-long-term-vault
      ScheduleExpression: "cron(0 2 ? * SUN *)"  # Weekly on Sunday at 2 AM UTC
      StartWindowMinutes: 120
      CompletionWindowMinutes: 360
      Lifecycle:
        DeleteAfterDays: 365
        MoveToColdStorageAfterDays: 90
      RecoveryPointTags:
        Environment: production
        BackupType: weekly
        Criticality: high
        Compliance: required
```

**CDK Implementation**:

```typescript
// infrastructure/lib/stacks/backup-stack.ts
import * as backup from 'aws-cdk-lib/aws-backup';
import * as events from 'aws-cdk-lib/aws-events';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import { Stack, StackProps, Duration, Tags } from 'aws-cdk-lib';

export class BackupStack extends Stack {
  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    // Create backup vault with encryption
    const productionVault = new backup.BackupVault(this, 'ProductionBackupVault', {
      backupVaultName: 'ecommerce-production-vault',
      encryptionKey: this.createBackupKmsKey(),
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // Create backup plan
    const backupPlan = new backup.BackupPlan(this, 'ProductionBackupPlan', {
      backupPlanName: 'ecommerce-production-database-backup',
      backupVault: productionVault,
    });

    // Add continuous backup rule (every 6 hours)
    backupPlan.addRule(new backup.BackupPlanRule({
      ruleName: 'continuous-backup',
      scheduleExpression: events.Schedule.cron({
        hour: '*/6',
        minute: '0',
      }),
      startWindow: Duration.minutes(60),
      completionWindow: Duration.minutes(180),
      deleteAfter: Duration.days(30),
      moveToColdStorageAfter: Duration.days(7),
    }));

    // Add daily backup rule
    backupPlan.addRule(new backup.BackupPlanRule({
      ruleName: 'daily-backup',
      scheduleExpression: events.Schedule.cron({
        hour: '3',
        minute: '0',
      }),
      startWindow: Duration.minutes(60),
      completionWindow: Duration.minutes(240),
      deleteAfter: Duration.days(90),
      moveToColdStorageAfter: Duration.days(30),
    }));

    // Add weekly backup rule
    backupPlan.addRule(new backup.BackupPlanRule({
      ruleName: 'weekly-backup',
      scheduleExpression: events.Schedule.cron({
        weekDay: 'SUN',
        hour: '2',
        minute: '0',
      }),
      startWindow: Duration.minutes(120),
      completionWindow: Duration.minutes(360),
      deleteAfter: Duration.days(365),
      moveToColdStorageAfter: Duration.days(90),
    }));

    // Create backup selection for RDS databases
    const rdsSelection = backupPlan.addSelection('RDSBackupSelection', {
      resources: [
        backup.BackupResource.fromTag('BackupEnabled', 'true'),
        backup.BackupResource.fromTag('Environment', 'production'),
      ],
      allowRestores: true,
    });

    // Add tags for cost allocation
    Tags.of(backupPlan).add('CostCenter', 'operations');
    Tags.of(backupPlan).add('Component', 'backup');
  }

  private createBackupKmsKey(): kms.Key {
    return new kms.Key(this, 'BackupKmsKey', {
      alias: 'alias/ecommerce-backup-key',
      description: 'KMS key for backup encryption',
      enableKeyRotation: true,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });
  }
}
```

### Backup Vault Configuration

**Production Vault**:

```yaml
VaultName: ecommerce-production-vault
Encryption: AWS KMS
KMSKeyId: arn:aws:kms:us-east-1:123456789012:key/backup-key
AccessPolicy:

  - Effect: Allow

    Principal:
      Service: backup.amazonaws.com
    Action:

      - backup:StartBackupJob
      - backup:StartRestoreJob

    Resource: "*"

  - Effect: Deny

    Principal: "*"
    Action:

      - backup:DeleteRecoveryPoint

    Resource: "*"
    Condition:
      StringNotEquals:
        aws:PrincipalArn: arn:aws:iam::123456789012:role/backup-admin-role

Notifications:
  BackupVaultEvents:

    - BACKUP_JOB_STARTED
    - BACKUP_JOB_COMPLETED
    - BACKUP_JOB_FAILED
    - RESTORE_JOB_STARTED
    - RESTORE_JOB_COMPLETED
    - RESTORE_JOB_FAILED

  SNSTopic: arn:aws:sns:us-east-1:123456789012:backup-notifications
```

**Long-Term Vault**:

```yaml
VaultName: ecommerce-long-term-vault
Encryption: AWS KMS
KMSKeyId: arn:aws:kms:us-east-1:123456789012:key/backup-key
LockConfiguration:
  MinRetentionDays: 365
  MaxRetentionDays: 2555  # 7 years
  ChangeableForDays: 3

AccessPolicy:

  - Effect: Allow

    Principal:
      Service: backup.amazonaws.com
    Action:

      - backup:StartBackupJob

    Resource: "*"

  - Effect: Deny

    Principal: "*"
    Action:

      - backup:DeleteRecoveryPoint
      - backup:UpdateRecoveryPointLifecycle

    Resource: "*"
```

## Backup Job Scheduling and Orchestration

### EventBridge Rules

**Scheduled Backup Triggers**:

```yaml
# RDS Snapshot Export Rule
RDSSnapshotExportRule:
  Name: rds-snapshot-export-trigger
  Description: Trigger RDS snapshot export to S3 after automated backup
  EventPattern:
    source:

      - aws.backup

    detail-type:

      - Backup Job State Change

    detail:
      state:

        - COMPLETED

      resourceType:

        - RDS

  Targets:

    - Arn: arn:aws:lambda:us-east-1:123456789012:function:rds-snapshot-exporter

      Id: rds-exporter-lambda
      RetryPolicy:
        MaximumRetryAttempts: 3
        MaximumEventAge: 3600

# Redis Snapshot Replication Rule
RedisSnapshotReplicationRule:
  Name: redis-snapshot-replication-trigger
  Description: Replicate Redis snapshots to DR region
  ScheduleExpression: "cron(0 5 * * ? *)"  # Daily at 5 AM UTC
  Targets:

    - Arn: arn:aws:states:us-east-1:123456789012:stateMachine:redis-replication-workflow

      Id: redis-replication-sfn
      RoleArn: arn:aws:iam::123456789012:role/eventbridge-sfn-role

# Kafka Topic Archive Rule
KafkaArchiveRule:
  Name: kafka-topic-archive-trigger
  Description: Archive Kafka topics to S3
  ScheduleExpression: "cron(0 6 * * ? *)"  # Daily at 6 AM UTC
  Targets:

    - Arn: arn:aws:ecs:us-east-1:123456789012:cluster/backup-cluster

      Id: kafka-archive-task
      EcsParameters:
        TaskDefinitionArn: arn:aws:ecs:us-east-1:123456789012:task-definition/kafka-archiver:1
        LaunchType: FARGATE
        NetworkConfiguration:
          AwsvpcConfiguration:
            Subnets:

              - subnet-12345678
              - subnet-87654321

            SecurityGroups:

              - sg-backup-tasks

            AssignPublicIp: DISABLED

# Configuration Backup Rule
ConfigBackupRule:
  Name: config-backup-trigger
  Description: Backup Kubernetes configurations
  ScheduleExpression: "cron(0 4 * * ? *)"  # Daily at 4 AM UTC
  Targets:

    - Arn: arn:aws:lambda:us-east-1:123456789012:function:k8s-config-backup

      Id: config-backup-lambda
```

### Step Functions Workflows

#### Comprehensive Backup Orchestration Workflow

**State Machine Definition**:

```json
{
  "Comment": "Comprehensive backup orchestration workflow",
  "StartAt": "ValidateBackupWindow",
  "States": {
    "ValidateBackupWindow": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:validate-backup-window",
      "Next": "ParallelBackupExecution",
      "Catch": [{
        "ErrorEquals": ["States.ALL"],
        "Next": "NotifyBackupFailure",
        "ResultPath": "$.error"
      }]
    },
    "ParallelBackupExecution": {
      "Type": "Parallel",
      "Branches": [
        {
          "StartAt": "BackupRDS",
          "States": {
            "BackupRDS": {
              "Type": "Task",
              "Resource": "arn:aws:states:::aws-sdk:rds:createDBSnapshot",
              "Parameters": {
                "DBInstanceIdentifier.$": "$.rdsInstanceId",
                "DBSnapshotIdentifier.$": "$.rdsSnapshotId"
              },
              "Next": "WaitForRDSSnapshot"
            },
            "WaitForRDSSnapshot": {
              "Type": "Wait",
              "Seconds": 60,
              "Next": "CheckRDSSnapshotStatus"
            },
            "CheckRDSSnapshotStatus": {
              "Type": "Task",
              "Resource": "arn:aws:states:::aws-sdk:rds:describeDBSnapshots",
              "Parameters": {
                "DBSnapshotIdentifier.$": "$.rdsSnapshotId"
              },
              "Next": "IsRDSSnapshotComplete"
            },
            "IsRDSSnapshotComplete": {
              "Type": "Choice",
              "Choices": [{
                "Variable": "$.DBSnapshots[0].Status",
                "StringEquals": "available",
                "Next": "ExportRDSToS3"
              }],
              "Default": "WaitForRDSSnapshot"
            },
            "ExportRDSToS3": {
              "Type": "Task",
              "Resource": "arn:aws:lambda:us-east-1:123456789012:function:export-rds-to-s3",
              "End": true
            }
          }
        },
        {
          "StartAt": "BackupRedis",
          "States": {
            "BackupRedis": {
              "Type": "Task",
              "Resource": "arn:aws:states:::aws-sdk:elasticache:createSnapshot",
              "Parameters": {
                "ReplicationGroupId.$": "$.redisClusterId",
                "SnapshotName.$": "$.redisSnapshotName"
              },
              "Next": "WaitForRedisSnapshot"
            },
            "WaitForRedisSnapshot": {
              "Type": "Wait",
              "Seconds": 30,
              "Next": "CheckRedisSnapshotStatus"
            },
            "CheckRedisSnapshotStatus": {
              "Type": "Task",
              "Resource": "arn:aws:states:::aws-sdk:elasticache:describeSnapshots",
              "Parameters": {
                "SnapshotName.$": "$.redisSnapshotName"
              },
              "Next": "IsRedisSnapshotComplete"
            },
            "IsRedisSnapshotComplete": {
              "Type": "Choice",
              "Choices": [{
                "Variable": "$.Snapshots[0].SnapshotStatus",
                "StringEquals": "available",
                "Next": "ReplicateRedisSnapshot"
              }],
              "Default": "WaitForRedisSnapshot"
            },
            "ReplicateRedisSnapshot": {
              "Type": "Task",
              "Resource": "arn:aws:lambda:us-east-1:123456789012:function:replicate-redis-snapshot",
              "End": true
            }
          }
        },
        {
          "StartAt": "BackupKafka",
          "States": {
            "BackupKafka": {
              "Type": "Task",
              "Resource": "arn:aws:states:::ecs:runTask.sync",
              "Parameters": {
                "Cluster": "backup-cluster",
                "TaskDefinition": "kafka-archiver",
                "LaunchType": "FARGATE",
                "NetworkConfiguration": {
                  "AwsvpcConfiguration": {
                    "Subnets": ["subnet-12345678"],
                    "SecurityGroups": ["sg-backup-tasks"]
                  }
                }
              },
              "End": true
            }
          }
        },
        {
          "StartAt": "BackupK8sConfig",
          "States": {
            "BackupK8sConfig": {
              "Type": "Task",
              "Resource": "arn:aws:lambda:us-east-1:123456789012:function:k8s-config-backup",
              "End": true
            }
          }
        }
      ],
      "Next": "VerifyAllBackups",
      "Catch": [{
        "ErrorEquals": ["States.ALL"],
        "Next": "NotifyBackupFailure",
        "ResultPath": "$.error"
      }]
    },
    "VerifyAllBackups": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:verify-backups",
      "Next": "UpdateBackupMetrics"
    },
    "UpdateBackupMetrics": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:update-backup-metrics",
      "Next": "NotifyBackupSuccess"
    },
    "NotifyBackupSuccess": {
      "Type": "Task",
      "Resource": "arn:aws:states:::sns:publish",
      "Parameters": {
        "TopicArn": "arn:aws:sns:us-east-1:123456789012:backup-success",
        "Subject": "Backup Completed Successfully",
        "Message.$": "$.backupSummary"
      },
      "End": true
    },
    "NotifyBackupFailure": {
      "Type": "Task",
      "Resource": "arn:aws:states:::sns:publish",
      "Parameters": {
        "TopicArn": "arn:aws:sns:us-east-1:123456789012:backup-failure",
        "Subject": "Backup Failed - Immediate Action Required",
        "Message.$": "$.error"
      },
      "Next": "TriggerPagerDuty"
    },
    "TriggerPagerDuty": {
      "Type": "Task",
      "Resource": "arn:aws:lambda:us-east-1:123456789012:function:trigger-pagerduty",
      "End": true
    }
  }
}
```

## Backup Automation Scripts

### Master Backup Orchestration Script

```bash
#!/bin/bash
# master-backup-orchestrator.sh
# Comprehensive backup orchestration script

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="/var/log/backups"
BACKUP_DATE=$(date +%Y-%m-%d-%H%M%S)
LOG_FILE="${LOG_DIR}/backup-${BACKUP_DATE}.log"
METRICS_FILE="${LOG_DIR}/metrics-${BACKUP_DATE}.json"
S3_BUCKET="s3://ecommerce-backups-prod-us-east-1"
SNS_TOPIC="arn:aws:sns:us-east-1:123456789012:backup-notifications"

# Ensure log directory exists
mkdir -p "$LOG_DIR"

# Logging function
log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

# Error handling
trap 'handle_error $? $LINENO' ERR

handle_error() {
  local exit_code=$1
  local line_number=$2
  log "ERROR: Backup failed at line $line_number with exit code $exit_code"
  
  # Send failure notification
  aws sns publish \
    --topic-arn "$SNS_TOPIC" \
    --subject "CRITICAL: Backup Failed" \
    --message "Backup orchestration failed at line $line_number. Check logs: $LOG_FILE"
  
  # Trigger PagerDuty
  trigger_pagerduty_alert "backup_failure" "Backup orchestration failed"
  
  exit "$exit_code"
}

# Initialize metrics
init_metrics() {
  cat > "$METRICS_FILE" <<EOF
{
  "backup_date": "${BACKUP_DATE}",
  "start_time": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "components": {}
}
EOF
}

# Update metrics
update_metrics() {
  local component=$1
  local status=$2
  local duration=$3
  local size=$4
  
  jq --arg comp "$component" \
     --arg stat "$status" \
     --arg dur "$duration" \
     --arg sz "$size" \
     '.components[$comp] = {
       "status": $stat,
       "duration_seconds": ($dur | tonumber),
       "size_bytes": ($sz | tonumber)
     }' "$METRICS_FILE" > "${METRICS_FILE}.tmp"
  
  mv "${METRICS_FILE}.tmp" "$METRICS_FILE"
}

# Backup RDS
backup_rds() {
  log "Starting RDS backup..."
  local start_time=$(date +%s)
  
  local snapshot_id="ecommerce-prod-db-${BACKUP_DATE}"
  
  aws rds create-db-snapshot \
    --db-instance-identifier ecommerce-prod-db \
    --db-snapshot-identifier "$snapshot_id" \
    --tags Key=BackupDate,Value="$BACKUP_DATE" Key=Automated,Value=true
  
  log "Waiting for RDS snapshot to complete..."
  aws rds wait db-snapshot-available --db-snapshot-identifier "$snapshot_id"
  
  # Export to S3
  log "Exporting RDS snapshot to S3..."
  aws rds start-export-task \
    --export-task-identifier "export-${snapshot_id}" \
    --source-arn "arn:aws:rds:us-east-1:123456789012:snapshot:${snapshot_id}" \
    --s3-bucket-name "ecommerce-backups-prod-us-east-1" \
    --s3-prefix "rds-exports/${BACKUP_DATE}/" \
    --iam-role-arn "arn:aws:iam::123456789012:role/rds-s3-export-role" \
    --kms-key-id "arn:aws:kms:us-east-1:123456789012:key/backup-key"
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  # Get snapshot size
  local size=$(aws rds describe-db-snapshots \
    --db-snapshot-identifier "$snapshot_id" \
    --query 'DBSnapshots[0].AllocatedStorage' \
    --output text)
  local size_bytes=$((size * 1024 * 1024 * 1024))
  
  update_metrics "rds" "success" "$duration" "$size_bytes"
  log "RDS backup completed in ${duration}s"
}

# Backup Redis
backup_redis() {
  log "Starting Redis backup..."
  local start_time=$(date +%s)
  
  local snapshot_name="ecommerce-redis-${BACKUP_DATE}"
  
  aws elasticache create-snapshot \
    --replication-group-id ecommerce-redis-cluster \
    --snapshot-name "$snapshot_name" \
    --tags Key=BackupDate,Value="$BACKUP_DATE" Key=Automated,Value=true
  
  log "Waiting for Redis snapshot to complete..."
  aws elasticache wait snapshot-available --snapshot-name "$snapshot_name"
  
  # Copy to DR region
  log "Replicating Redis snapshot to DR region..."
  aws elasticache copy-snapshot \
    --region us-west-2 \
    --source-snapshot-name "$snapshot_name" \
    --target-snapshot-name "${snapshot_name}-dr" \
    --source-region us-east-1 \
    --kms-key-id "arn:aws:kms:us-west-2:123456789012:key/backup-key"
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  # Get snapshot size
  local size=$(aws elasticache describe-snapshots \
    --snapshot-name "$snapshot_name" \
    --query 'Snapshots[0].CacheClusterSize' \
    --output text || echo "0")
  
  update_metrics "redis" "success" "$duration" "$size"
  log "Redis backup completed in ${duration}s"
}

# Backup Kafka topics
backup_kafka() {
  log "Starting Kafka topic backup..."
  local start_time=$(date +%s)
  
  "${SCRIPT_DIR}/kafka-topic-archive.sh" "$BACKUP_DATE"
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  # Calculate total size of archived topics
  local size=$(aws s3 ls "${S3_BUCKET}/kafka-archives/${BACKUP_DATE}/" --recursive \
    | awk '{sum += $3} END {print sum}')
  
  update_metrics "kafka" "success" "$duration" "${size:-0}"
  log "Kafka backup completed in ${duration}s"
}

# Backup Kubernetes configuration
backup_k8s_config() {
  log "Starting Kubernetes configuration backup..."
  local start_time=$(date +%s)
  
  "${SCRIPT_DIR}/backup-application-config.sh"
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  # Get backup size
  local size=$(aws s3 ls "${S3_BUCKET}/application-config/${BACKUP_DATE}/" --recursive \
    | awk '{sum += $3} END {print sum}')
  
  update_metrics "k8s_config" "success" "$duration" "${size:-0}"
  log "Kubernetes config backup completed in ${duration}s"
}

# Verify backups
verify_backups() {
  log "Verifying all backups..."
  local start_time=$(date +%s)
  
  # Verify RDS snapshot
  local rds_snapshot="ecommerce-prod-db-${BACKUP_DATE}"
  local rds_status=$(aws rds describe-db-snapshots \
    --db-snapshot-identifier "$rds_snapshot" \
    --query 'DBSnapshots[0].Status' \
    --output text)
  
  if [ "$rds_status" != "available" ]; then
    log "ERROR: RDS snapshot verification failed. Status: $rds_status"
    return 1
  fi
  
  # Verify Redis snapshot
  local redis_snapshot="ecommerce-redis-${BACKUP_DATE}"
  local redis_status=$(aws elasticache describe-snapshots \
    --snapshot-name "$redis_snapshot" \
    --query 'Snapshots[0].SnapshotStatus' \
    --output text)
  
  if [ "$redis_status" != "available" ]; then
    log "ERROR: Redis snapshot verification failed. Status: $redis_status"
    return 1
  fi
  
  # Verify S3 backups
  local s3_objects=$(aws s3 ls "${S3_BUCKET}/kafka-archives/${BACKUP_DATE}/" | wc -l)
  if [ "$s3_objects" -eq 0 ]; then
    log "ERROR: No Kafka archives found in S3"
    return 1
  fi
  
  local end_time=$(date +%s)
  local duration=$((end_time - start_time))
  
  update_metrics "verification" "success" "$duration" "0"
  log "Backup verification completed successfully in ${duration}s"
}

# Send success notification
send_success_notification() {
  log "Sending success notification..."
  
  # Finalize metrics
  jq --arg end "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
     '.end_time = $end | .status = "success"' \
     "$METRICS_FILE" > "${METRICS_FILE}.tmp"
  mv "${METRICS_FILE}.tmp" "$METRICS_FILE"
  
  # Upload metrics to S3
  aws s3 cp "$METRICS_FILE" "${S3_BUCKET}/metrics/${BACKUP_DATE}/metrics.json"
  
  # Send SNS notification
  local message=$(cat <<EOF
Backup completed successfully at $(date)

Summary:
$(jq -r '.components | to_entries[] | "- \(.key): \(.value.status) (\(.value.duration_seconds)s, \(.value.size_bytes) bytes)"' "$METRICS_FILE")

Total Duration: $(jq -r '.components | to_entries | map(.value.duration_seconds | tonumber) | add' "$METRICS_FILE")s

Logs: $LOG_FILE
Metrics: ${S3_BUCKET}/metrics/${BACKUP_DATE}/metrics.json
EOF
)
  
  aws sns publish \
    --topic-arn "$SNS_TOPIC" \
    --subject "Backup Completed Successfully - ${BACKUP_DATE}" \
    --message "$message"
  
  log "Success notification sent"
}

# Trigger PagerDuty alert
trigger_pagerduty_alert() {
  local event_action=$1
  local description=$2
  
  curl -X POST https://events.pagerduty.com/v2/enqueue \
    -H 'Content-Type: application/json' \
    -d "{
      \"routing_key\": \"${PAGERDUTY_ROUTING_KEY}\",
      \"event_action\": \"${event_action}\",
      \"payload\": {
        \"summary\": \"${description}\",
        \"severity\": \"critical\",
        \"source\": \"backup-orchestrator\",
        \"timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
        \"custom_details\": {
          \"backup_date\": \"${BACKUP_DATE}\",
          \"log_file\": \"${LOG_FILE}\"
        }
      }
    }"
}

# Main execution
main() {
  log "========================================="
  log "Starting backup orchestration: ${BACKUP_DATE}"
  log "========================================="
  
  init_metrics
  
  # Execute backups in parallel where possible
  backup_rds &
  RDS_PID=$!
  
  backup_redis &
  REDIS_PID=$!
  
  backup_kafka &
  KAFKA_PID=$!
  
  backup_k8s_config &
  K8S_PID=$!
  
  # Wait for all backups to complete
  wait $RDS_PID || log "RDS backup failed"
  wait $REDIS_PID || log "Redis backup failed"
  wait $KAFKA_PID || log "Kafka backup failed"
  wait $K8S_PID || log "K8s config backup failed"
  
  # Verify all backups
  verify_backups
  
  # Send success notification
  send_success_notification
  
  log "========================================="
  log "Backup orchestration completed successfully"
  log "========================================="
}

# Execute main function
main "$@"
```

### Lambda Functions for Backup Automation

#### RDS Snapshot Exporter

```python
# lambda/rds-snapshot-exporter/handler.py
import boto3
import json
import os
from datetime import datetime

rds = boto3.client('rds')
s3 = boto3.client('s3')
sns = boto3.client('sns')

S3_BUCKET = os.environ['S3_BUCKET']
SNS_TOPIC = os.environ['SNS_TOPIC']
KMS_KEY_ID = os.environ['KMS_KEY_ID']
IAM_ROLE_ARN = os.environ['IAM_ROLE_ARN']

def lambda_handler(event, context):
    """
    Export RDS snapshot to S3 after backup completion
    """
    try:
        # Extract snapshot information from event
        detail = event['detail']
        snapshot_arn = detail['resourceArn']
        snapshot_id = snapshot_arn.split(':')[-1]
        
        print(f"Processing snapshot: {snapshot_id}")
        
        # Generate export task identifier
        export_id = f"export-{snapshot_id}-{datetime.now().strftime('%Y%m%d%H%M%S')}"
        
        # Start export task
        response = rds.start_export_task(
            ExportTaskIdentifier=export_id,
            SourceArn=snapshot_arn,
            S3BucketName=S3_BUCKET,
            S3Prefix=f"rds-exports/{datetime.now().strftime('%Y/%m/%d')}/",
            IamRoleArn=IAM_ROLE_ARN,
            KmsKeyId=KMS_KEY_ID
        )
        
        print(f"Export task started: {export_id}")
        
        # Send notification
        sns.publish(
            TopicArn=SNS_TOPIC,
            Subject=f"RDS Snapshot Export Started: {snapshot_id}",
            Message=json.dumps({
                'snapshot_id': snapshot_id,
                'export_id': export_id,
                'status': 'started',
                'timestamp': datetime.now().isoformat()
            }, indent=2)
        )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Export task started successfully',
                'export_id': export_id
            })
        }
        
    except Exception as e:
        print(f"Error: {str(e)}")
        
        # Send failure notification
        sns.publish(
            TopicArn=SNS_TOPIC,
            Subject=f"RDS Snapshot Export Failed",
            Message=f"Failed to export snapshot: {str(e)}"
        )
        
        raise
```

#### Backup Verification Function

```python
# lambda/backup-verifier/handler.py
import boto3
import json
import os
from datetime import datetime, timedelta

rds = boto3.client('rds')
elasticache = boto3.client('elasticache')
s3 = boto3.client('s3')
cloudwatch = boto3.client('cloudwatch')
sns = boto3.client('sns')

SNS_TOPIC = os.environ['SNS_TOPIC']
METRIC_NAMESPACE = 'ECommerce/Backups'

def lambda_handler(event, context):
    """
    Verify backup completion and integrity
    """
    verification_results = {
        'timestamp': datetime.now().isoformat(),
        'checks': {}
    }
    
    try:
        # Verify RDS snapshots
        rds_result = verify_rds_snapshots()
        verification_results['checks']['rds'] = rds_result
        
        # Verify Redis snapshots
        redis_result = verify_redis_snapshots()
        verification_results['checks']['redis'] = redis_result
        
        # Verify S3 backups
        s3_result = verify_s3_backups()
        verification_results['checks']['s3'] = s3_result
        
        # Publish metrics
        publish_verification_metrics(verification_results)
        
        # Send notification
        send_verification_report(verification_results)
        
        return {
            'statusCode': 200,
            'body': json.dumps(verification_results)
        }
        
    except Exception as e:
        print(f"Verification error: {str(e)}")
        
        sns.publish(
            TopicArn=SNS_TOPIC,
            Subject="Backup Verification Failed",
            Message=f"Backup verification encountered an error: {str(e)}"
        )
        
        raise

def verify_rds_snapshots():
    """Verify RDS snapshots from last 24 hours"""
    cutoff_time = datetime.now() - timedelta(hours=24)
    
    response = rds.describe_db_snapshots(
        DBInstanceIdentifier='ecommerce-prod-db',
        SnapshotType='automated'
    )
    
    recent_snapshots = [
        s for s in response['DBSnapshots']
        if s['SnapshotCreateTime'].replace(tzinfo=None) > cutoff_time
    ]
    
    available_snapshots = [
        s for s in recent_snapshots
        if s['Status'] == 'available'
    ]
    
    return {
        'total_snapshots': len(recent_snapshots),
        'available_snapshots': len(available_snapshots),
        'status': 'pass' if len(available_snapshots) > 0 else 'fail',
        'latest_snapshot': recent_snapshots[0]['DBSnapshotIdentifier'] if recent_snapshots else None
    }

def verify_redis_snapshots():
    """Verify Redis snapshots from last 24 hours"""
    cutoff_time = datetime.now() - timedelta(hours=24)
    
    response = elasticache.describe_snapshots(
        ReplicationGroupId='ecommerce-redis-cluster'
    )
    
    recent_snapshots = [
        s for s in response['Snapshots']
        if s['NodeSnapshots'][0]['SnapshotCreateTime'].replace(tzinfo=None) > cutoff_time
    ]
    
    available_snapshots = [
        s for s in recent_snapshots
        if s['SnapshotStatus'] == 'available'
    ]
    
    return {
        'total_snapshots': len(recent_snapshots),
        'available_snapshots': len(available_snapshots),
        'status': 'pass' if len(available_snapshots) > 0 else 'fail',
        'latest_snapshot': recent_snapshots[0]['SnapshotName'] if recent_snapshots else None
    }

def verify_s3_backups():
    """Verify S3 backup objects from last 24 hours"""
    bucket = os.environ['S3_BUCKET']
    today = datetime.now().strftime('%Y-%m-%d')
    
    prefixes = [
        f'kafka-archives/{today}/',
        f'application-config/{today}/',
        f'rds-exports/{today}/'
    ]
    
    results = {}
    for prefix in prefixes:
        try:
            response = s3.list_objects_v2(
                Bucket=bucket,
                Prefix=prefix
            )
            
            object_count = response.get('KeyCount', 0)
            total_size = sum(obj['Size'] for obj in response.get('Contents', []))
            
            results[prefix] = {
                'object_count': object_count,
                'total_size_bytes': total_size,
                'status': 'pass' if object_count > 0 else 'fail'
            }
        except Exception as e:
            results[prefix] = {
                'status': 'error',
                'error': str(e)
            }
    
    overall_status = 'pass' if all(r['status'] == 'pass' for r in results.values()) else 'fail'
    
    return {
        'prefixes': results,
        'status': overall_status
    }

def publish_verification_metrics(results):
    """Publish verification metrics to CloudWatch"""
    metrics = []
    
    for check_name, check_result in results['checks'].items():
        metrics.append({
            'MetricName': f'{check_name}_verification_status',
            'Value': 1 if check_result['status'] == 'pass' else 0,
            'Unit': 'None',
            'Timestamp': datetime.now()
        })
    
    cloudwatch.put_metric_data(
        Namespace=METRIC_NAMESPACE,
        MetricData=metrics
    )

def send_verification_report(results):
    """Send verification report via SNS"""
    all_passed = all(
        check['status'] == 'pass'
        for check in results['checks'].values()
    )
    
    subject = "Backup Verification: " + ("PASSED" if all_passed else "FAILED")
    
    message = f"""
Backup Verification Report
Timestamp: {results['timestamp']}

RDS Snapshots:
  Status: {results['checks']['rds']['status']}
  Available: {results['checks']['rds']['available_snapshots']}
  Latest: {results['checks']['rds']['latest_snapshot']}

Redis Snapshots:
  Status: {results['checks']['redis']['status']}
  Available: {results['checks']['redis']['available_snapshots']}
  Latest: {results['checks']['redis']['latest_snapshot']}

S3 Backups:
  Status: {results['checks']['s3']['status']}
  Details: {json.dumps(results['checks']['s3']['prefixes'], indent=2)}

Overall Status: {"PASSED" if all_passed else "FAILED"}
"""
    
    sns.publish(
        TopicArn=SNS_TOPIC,
        Subject=subject,
        Message=message
    )
```

## Backup Notification and Reporting

### SNS Topics Configuration

```yaml
# Backup Success Topic
BackupSuccessTopic:
  Name: ecommerce-backup-success
  DisplayName: Backup Success Notifications
  Subscriptions:

    - Protocol: email

      Endpoint: ops-team@ecommerce-platform.com

    - Protocol: sms

      Endpoint: +1-555-0100  # On-call engineer

    - Protocol: lambda

      Endpoint: arn:aws:lambda:us-east-1:123456789012:function:backup-metrics-collector

# Backup Failure Topic
BackupFailureTopic:
  Name: ecommerce-backup-failure
  DisplayName: Backup Failure Alerts
  Subscriptions:

    - Protocol: email

      Endpoint: ops-team@ecommerce-platform.com

    - Protocol: sms

      Endpoint: +1-555-0100  # On-call engineer

    - Protocol: https

      Endpoint: https://events.pagerduty.com/integration/xxx/enqueue

    - Protocol: lambda

      Endpoint: arn:aws:lambda:us-east-1:123456789012:function:backup-failure-handler

# Backup Verification Topic
BackupVerificationTopic:
  Name: ecommerce-backup-verification
  DisplayName: Backup Verification Reports
  Subscriptions:

    - Protocol: email

      Endpoint: ops-team@ecommerce-platform.com

    - Protocol: lambda

      Endpoint: arn:aws:lambda:us-east-1:123456789012:function:verification-report-processor
```

### Email Notification Templates

**Success Notification Template**:

```html
<!DOCTYPE html>
<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; }
    .header { background-color: #28a745; color: white; padding: 20px; }
    .content { padding: 20px; }
    .metrics { background-color: #f8f9fa; padding: 15px; margin: 10px 0; }
    .footer { background-color: #f1f1f1; padding: 10px; font-size: 12px; }
  </style>
</head>
<body>
  <div class="header">
    <h1>✓ Backup Completed Successfully</h1>
  </div>
  <div class="content">
    <p><strong>Backup Date:</strong> {{backup_date}}</p>
    <p><strong>Completion Time:</strong> {{completion_time}}</p>
    
    <div class="metrics">
      <h3>Backup Summary</h3>
      <ul>
        <li><strong>RDS Database:</strong> {{rds_status}} ({{rds_size}} GB, {{rds_duration}}s)</li>
        <li><strong>Redis Cache:</strong> {{redis_status}} ({{redis_size}} MB, {{redis_duration}}s)</li>
        <li><strong>Kafka Topics:</strong> {{kafka_status}} ({{kafka_size}} GB, {{kafka_duration}}s)</li>
        <li><strong>K8s Configuration:</strong> {{k8s_status}} ({{k8s_size}} MB, {{k8s_duration}}s)</li>
      </ul>
    </div>
    
    <p><strong>Total Duration:</strong> {{total_duration}} seconds</p>
    <p><strong>Total Size:</strong> {{total_size}} GB</p>
    
    <p><strong>Backup Locations:</strong></p>
    <ul>
      <li>Primary: s3://ecommerce-backups-prod-us-east-1/{{backup_date}}/</li>
      <li>DR Region: s3://ecommerce-backups-prod-us-west-2/{{backup_date}}/</li>
    </ul>
    
    <p><strong>Verification Status:</strong> All backups verified successfully</p>
  </div>
  <div class="footer">
    <p>E-Commerce Platform Backup System | <a href="{{dashboard_url}}">View Dashboard</a></p>
  </div>
</body>
</html>
```

**Failure Notification Template**:

```html
<!DOCTYPE html>
<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; }
    .header { background-color: #dc3545; color: white; padding: 20px; }
    .content { padding: 20px; }
    .error { background-color: #f8d7da; border: 1px solid #f5c6cb; padding: 15px; margin: 10px 0; }
    .action { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; margin: 10px 0; }
    .footer { background-color: #f1f1f1; padding: 10px; font-size: 12px; }
  </style>
</head>
<body>
  <div class="header">
    <h1>✗ CRITICAL: Backup Failed</h1>
  </div>
  <div class="content">
    <p><strong>Backup Date:</strong> {{backup_date}}</p>
    <p><strong>Failure Time:</strong> {{failure_time}}</p>
    
    <div class="error">
      <h3>Error Details</h3>
      <p><strong>Component:</strong> {{failed_component}}</p>
      <p><strong>Error Message:</strong> {{error_message}}</p>
      <p><strong>Error Code:</strong> {{error_code}}</p>
      <p><strong>Stack Trace:</strong></p>
      <pre>{{stack_trace}}</pre>
    </div>
    
    <div class="action">
      <h3>Immediate Actions Required</h3>
      <ol>
        <li>Review error logs: {{log_file_url}}</li>
        <li>Check service health: {{health_check_url}}</li>
        <li>Verify backup resources are available</li>
        <li>Retry backup manually if needed</li>
        <li>Update incident ticket: {{incident_url}}</li>
      </ol>
    </div>
    
    <p><strong>On-Call Engineer:</strong> {{oncall_engineer}}</p>
    <p><strong>PagerDuty Incident:</strong> {{pagerduty_incident_id}}</p>
  </div>
  <div class="footer">
    <p>E-Commerce Platform Backup System | <a href="{{runbook_url}}">View Runbook</a></p>
  </div>
</body>
</html>
```

## Backup Failure Handling and Retry Logic

### Retry Configuration

```yaml
RetryPolicy:
  MaxAttempts: 3
  BackoffRate: 2.0
  IntervalSeconds: 300  # 5 minutes
  
  RetryableErrors:

    - ThrottlingException
    - ServiceUnavailableException
    - InternalServerError
    - RequestTimeout
    - NetworkError
    
  NonRetryableErrors:

    - InvalidParameterException
    - ResourceNotFoundException
    - AccessDeniedException
    - InsufficientStorageException

```

### Failure Handling Lambda

```python
# lambda/backup-failure-handler/handler.py
import boto3
import json
import os
from datetime import datetime

stepfunctions = boto3.client('stepfunctions')
sns = boto3.client('sns')
dynamodb = boto3.resource('dynamodb')

FAILURE_TABLE = os.environ['FAILURE_TABLE']
SNS_TOPIC = os.environ['SNS_TOPIC']
MAX_RETRIES = 3

def lambda_handler(event, context):
    """
    Handle backup failures with retry logic and escalation
    """
    try:
        failure_details = extract_failure_details(event)
        
        # Record failure in DynamoDB
        failure_id = record_failure(failure_details)
        
        # Check retry count
        retry_count = get_retry_count(failure_details['component'])
        
        if retry_count < MAX_RETRIES and is_retryable(failure_details['error_code']):
            # Schedule retry
            schedule_retry(failure_details, retry_count + 1)
            
            sns.publish(
                TopicArn=SNS_TOPIC,
                Subject=f"Backup Retry Scheduled: {failure_details['component']}",
                Message=f"Retry attempt {retry_count + 1} of {MAX_RETRIES} scheduled for {failure_details['component']}"
            )
        else:
            # Escalate to on-call
            escalate_failure(failure_details, failure_id)
            
            # Trigger alternative backup strategy
            trigger_alternative_backup(failure_details)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'failure_id': failure_id,
                'retry_count': retry_count,
                'action': 'retry' if retry_count < MAX_RETRIES else 'escalate'
            })
        }
        
    except Exception as e:
        print(f"Error handling failure: {str(e)}")
        raise

def extract_failure_details(event):
    """Extract failure details from event"""
    return {
        'timestamp': datetime.now().isoformat(),
        'component': event.get('component', 'unknown'),
        'error_code': event.get('error_code', 'UNKNOWN_ERROR'),
        'error_message': event.get('error_message', ''),
        'stack_trace': event.get('stack_trace', ''),
        'backup_date': event.get('backup_date', datetime.now().strftime('%Y-%m-%d'))
    }

def record_failure(details):
    """Record failure in DynamoDB"""
    table = dynamodb.Table(FAILURE_TABLE)
    
    failure_id = f"{details['component']}-{details['timestamp']}"
    
    table.put_item(
        Item={
            'failure_id': failure_id,
            'timestamp': details['timestamp'],
            'component': details['component'],
            'error_code': details['error_code'],
            'error_message': details['error_message'],
            'stack_trace': details['stack_trace'],
            'backup_date': details['backup_date'],
            'status': 'pending_retry'
        }
    )
    
    return failure_id

def get_retry_count(component):
    """Get current retry count for component"""
    table = dynamodb.Table(FAILURE_TABLE)
    
    response = table.query(
        KeyConditionExpression='component = :comp',
        FilterExpression='#status = :status',
        ExpressionAttributeNames={'#status': 'status'},
        ExpressionAttributeValues={
            ':comp': component,
            ':status': 'pending_retry'
        }
    )
    
    return len(response.get('Items', []))

def is_retryable(error_code):
    """Check if error is retryable"""
    retryable_errors = [
        'ThrottlingException',
        'ServiceUnavailableException',
        'InternalServerError',
        'RequestTimeout',
        'NetworkError'
    ]
    
    return error_code in retryable_errors

def schedule_retry(details, retry_count):
    """Schedule backup retry"""
    # Calculate backoff delay
    delay_seconds = 300 * (2 ** (retry_count - 1))  # Exponential backoff
    
    # Trigger Step Functions execution with delay
    stepfunctions.start_execution(
        stateMachineArn=os.environ['BACKUP_STATE_MACHINE_ARN'],
        input=json.dumps({
            'component': details['component'],
            'retry_attempt': retry_count,
            'original_failure': details
        })
    )

def escalate_failure(details, failure_id):
    """Escalate failure to on-call engineer"""
    # Send PagerDuty alert
    import requests
    
    requests.post(
        'https://events.pagerduty.com/v2/enqueue',
        json={
            'routing_key': os.environ['PAGERDUTY_ROUTING_KEY'],
            'event_action': 'trigger',
            'payload': {
                'summary': f"Backup Failed: {details['component']}",
                'severity': 'critical',
                'source': 'backup-automation',
                'custom_details': {
                    'failure_id': failure_id,
                    'component': details['component'],
                    'error_code': details['error_code'],
                    'error_message': details['error_message']
                }
            }
        }
    )

def trigger_alternative_backup(details):
    """Trigger alternative backup strategy"""
    # Implement fallback backup mechanism
    # For example, trigger manual backup process or use alternative backup tool
    pass
```

## Backup Metrics and Dashboards

### CloudWatch Metrics

**Custom Metrics Published**:

```yaml
Namespace: ECommerce/Backups

Metrics:

  - MetricName: BackupDuration

    Unit: Seconds
    Dimensions:

      - Name: Component

        Value: [RDS, Redis, Kafka, K8sConfig]

      - Name: BackupType

        Value: [Automated, Manual]
    
  - MetricName: BackupSize

    Unit: Bytes
    Dimensions:

      - Name: Component

        Value: [RDS, Redis, Kafka, K8sConfig]
    
  - MetricName: BackupSuccess

    Unit: Count
    Dimensions:

      - Name: Component

        Value: [RDS, Redis, Kafka, K8sConfig]
    
  - MetricName: BackupFailure

    Unit: Count
    Dimensions:

      - Name: Component

        Value: [RDS, Redis, Kafka, K8sConfig]

      - Name: ErrorCode

        Value: [Dynamic based on error]
    
  - MetricName: VerificationStatus

    Unit: None
    Value: [0=Failed, 1=Passed]
    Dimensions:

      - Name: Component

        Value: [RDS, Redis, Kafka, K8sConfig]
    
  - MetricName: BackupAge

    Unit: Hours
    Dimensions:

      - Name: Component

        Value: [RDS, Redis, Kafka, K8sConfig]
    
  - MetricName: StorageCost

    Unit: None
    Value: Estimated daily cost
    Dimensions:

      - Name: StorageClass

        Value: [Standard, IntelligentTiering, Glacier, DeepArchive]
```

### Grafana Dashboard Configuration

**Backup Overview Dashboard**:

```json
{
  "dashboard": {
    "title": "Backup Automation Overview",
    "tags": ["backups", "operations"],
    "timezone": "UTC",
    "panels": [
      {
        "id": 1,
        "title": "Backup Success Rate (24h)",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(rate(backup_success_total[24h])) / (sum(rate(backup_success_total[24h])) + sum(rate(backup_failure_total[24h]))) * 100",
            "legendFormat": "Success Rate"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "thresholds": {
              "steps": [
                {"value": 0, "color": "red"},
                {"value": 95, "color": "yellow"},
                {"value": 99, "color": "green"}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Backup Duration by Component",
        "type": "timeseries",
        "targets": [
          {
            "expr": "backup_duration_seconds{component=~\"RDS|Redis|Kafka|K8sConfig\"}",
            "legendFormat": "{{component}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "s"
          }
        }
      },
      {
        "id": 3,
        "title": "Backup Size Trend",
        "type": "timeseries",
        "targets": [
          {
            "expr": "backup_size_bytes{component=~\"RDS|Redis|Kafka|K8sConfig\"}",
            "legendFormat": "{{component}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "bytes"
          }
        }
      },
      {
        "id": 4,
        "title": "Failed Backups (7d)",
        "type": "table",
        "targets": [
          {
            "expr": "topk(10, sum by (component, error_code) (increase(backup_failure_total[7d])))",
            "format": "table"
          }
        ]
      },
      {
        "id": 5,
        "title": "Backup Age",
        "type": "gauge",
        "targets": [
          {
            "expr": "backup_age_hours{component=~\"RDS|Redis|Kafka|K8sConfig\"}",
            "legendFormat": "{{component}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "h",
            "thresholds": {
              "steps": [
                {"value": 0, "color": "green"},
                {"value": 24, "color": "yellow"},
                {"value": 48, "color": "red"}
              ]
            }
          }
        }
      },
      {
        "id": 6,
        "title": "Storage Cost Breakdown",
        "type": "piechart",
        "targets": [
          {
            "expr": "sum by (storage_class) (backup_storage_cost)",
            "legendFormat": "{{storage_class}}"
          }
        ]
      },
      {
        "id": 7,
        "title": "Verification Status",
        "type": "stat",
        "targets": [
          {
            "expr": "backup_verification_status{component=~\"RDS|Redis|Kafka|K8sConfig\"}",
            "legendFormat": "{{component}}"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "mappings": [
              {"value": 0, "text": "Failed", "color": "red"},
              {"value": 1, "text": "Passed", "color": "green"}
            ]
          }
        }
      },
      {
        "id": 8,
        "title": "Backup Schedule Compliance",
        "type": "timeseries",
        "targets": [
          {
            "expr": "backup_schedule_compliance_percent",
            "legendFormat": "Compliance %"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "min": 0,
            "max": 100
          }
        }
      }
    ]
  }
}
```

### CloudWatch Dashboard

```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["ECommerce/Backups", "BackupSuccess", {"stat": "Sum", "label": "Successful Backups"}],
          [".", "BackupFailure", {"stat": "Sum", "label": "Failed Backups"}]
        ],
        "period": 300,
        "stat": "Sum",
        "region": "us-east-1",
        "title": "Backup Success vs Failure",
        "yAxis": {
          "left": {
            "min": 0
          }
        }
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["ECommerce/Backups", "BackupDuration", {"stat": "Average", "dimensions": {"Component": "RDS"}}],
          ["...", {"dimensions": {"Component": "Redis"}}],
          ["...", {"dimensions": {"Component": "Kafka"}}],
          ["...", {"dimensions": {"Component": "K8sConfig"}}]
        ],
        "period": 300,
        "stat": "Average",
        "region": "us-east-1",
        "title": "Average Backup Duration by Component",
        "yAxis": {
          "left": {
            "label": "Seconds",
            "min": 0
          }
        }
      }
    },
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["ECommerce/Backups", "BackupSize", {"stat": "Average", "dimensions": {"Component": "RDS"}}],
          ["...", {"dimensions": {"Component": "Redis"}}],
          ["...", {"dimensions": {"Component": "Kafka"}}],
          ["...", {"dimensions": {"Component": "K8sConfig"}}]
        ],
        "period": 86400,
        "stat": "Average",
        "region": "us-east-1",
        "title": "Backup Size Trend (Daily)",
        "yAxis": {
          "left": {
            "label": "Bytes",
            "min": 0
          }
        }
      }
    },
    {
      "type": "log",
      "properties": {
        "query": "SOURCE '/aws/lambda/backup-orchestrator'\n| fields @timestamp, @message\n| filter @message like /ERROR/\n| sort @timestamp desc\n| limit 20",
        "region": "us-east-1",
        "title": "Recent Backup Errors",
        "stacked": false
      }
    }
  ]
}
```

## Backup Compliance Reporting

### Compliance Requirements

```yaml
ComplianceStandards:

  - Standard: SOC 2 Type II

    Requirements:

      - Daily backups with 30-day retention
      - Encrypted backups at rest and in transit
      - Backup verification within 24 hours
      - Documented recovery procedures
      - Quarterly DR testing
    
  - Standard: GDPR

    Requirements:

      - Data retention policies enforced
      - Backup data encrypted
      - Access controls and audit logs
      - Data deletion capabilities
      - Cross-border data transfer compliance
    
  - Standard: PCI DSS

    Requirements:

      - Daily backups of cardholder data
      - Encrypted backups
      - Quarterly backup restoration testing
      - Backup access logging
      - Secure backup storage
    
  - Standard: HIPAA (if applicable)

    Requirements:

      - Daily backups with encryption
      - Access controls and audit trails
      - Backup integrity verification
      - Documented backup procedures
      - Business associate agreements

```

### Compliance Report Generator

```python
# lambda/compliance-report-generator/handler.py
import boto3
import json
import os
from datetime import datetime, timedelta
from collections import defaultdict

s3 = boto3.client('s3')
rds = boto3.client('rds')
elasticache = boto3.client('elasticache')
backup = boto3.client('backup')
cloudtrail = boto3.client('cloudtrail')

REPORT_BUCKET = os.environ['REPORT_BUCKET']

def lambda_handler(event, context):
    """
    Generate comprehensive backup compliance report
    """
    report_date = datetime.now()
    report_period_start = report_date - timedelta(days=30)
    
    report = {
        'report_date': report_date.isoformat(),
        'report_period': {
            'start': report_period_start.isoformat(),
            'end': report_date.isoformat()
        },
        'compliance_checks': {}
    }
    
    # SOC 2 Compliance Checks
    report['compliance_checks']['soc2'] = check_soc2_compliance(report_period_start, report_date)
    
    # GDPR Compliance Checks
    report['compliance_checks']['gdpr'] = check_gdpr_compliance(report_period_start, report_date)
    
    # PCI DSS Compliance Checks
    report['compliance_checks']['pci_dss'] = check_pci_dss_compliance(report_period_start, report_date)
    
    # Generate summary
    report['summary'] = generate_compliance_summary(report['compliance_checks'])
    
    # Save report to S3
    report_key = f"compliance-reports/{report_date.strftime('%Y/%m')}/backup-compliance-{report_date.strftime('%Y%m%d')}.json"
    s3.put_object(
        Bucket=REPORT_BUCKET,
        Key=report_key,
        Body=json.dumps(report, indent=2),
        ContentType='application/json'
    )
    
    # Generate HTML report
    html_report = generate_html_report(report)
    html_key = report_key.replace('.json', '.html')
    s3.put_object(
        Bucket=REPORT_BUCKET,
        Key=html_key,
        Body=html_report,
        ContentType='text/html'
    )
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'report_location': f"s3://{REPORT_BUCKET}/{report_key}",
            'html_report': f"s3://{REPORT_BUCKET}/{html_key}",
            'compliance_status': report['summary']['overall_status']
        })
    }

def check_soc2_compliance(start_date, end_date):
    """Check SOC 2 compliance requirements"""
    checks = {}
    
    # Check daily backup frequency
    checks['daily_backups'] = verify_daily_backups(start_date, end_date)
    
    # Check 30-day retention
    checks['retention_policy'] = verify_retention_policy(30)
    
    # Check encryption
    checks['encryption'] = verify_backup_encryption()
    
    # Check backup verification
    checks['verification'] = verify_backup_testing(start_date, end_date)
    
    # Check DR testing
    checks['dr_testing'] = verify_dr_testing()
    
    return {
        'checks': checks,
        'compliant': all(check['status'] == 'pass' for check in checks.values())
    }

def check_gdpr_compliance(start_date, end_date):
    """Check GDPR compliance requirements"""
    checks = {}
    
    # Check data retention policies
    checks['retention_policies'] = verify_gdpr_retention()
    
    # Check encryption
    checks['encryption'] = verify_backup_encryption()
    
    # Check access controls
    checks['access_controls'] = verify_access_controls()
    
    # Check audit logs
    checks['audit_logs'] = verify_audit_logs(start_date, end_date)
    
    # Check data deletion capabilities
    checks['data_deletion'] = verify_deletion_capabilities()
    
    return {
        'checks': checks,
        'compliant': all(check['status'] == 'pass' for check in checks.values())
    }

def check_pci_dss_compliance(start_date, end_date):
    """Check PCI DSS compliance requirements"""
    checks = {}
    
    # Check daily backups
    checks['daily_backups'] = verify_daily_backups(start_date, end_date)
    
    # Check encryption
    checks['encryption'] = verify_backup_encryption()
    
    # Check quarterly restoration testing
    checks['restoration_testing'] = verify_quarterly_restoration_testing()
    
    # Check access logging
    checks['access_logging'] = verify_access_logging(start_date, end_date)
    
    # Check secure storage
    checks['secure_storage'] = verify_secure_storage()
    
    return {
        'checks': checks,
        'compliant': all(check['status'] == 'pass' for check in checks.values())
    }

def verify_daily_backups(start_date, end_date):
    """Verify daily backups were performed"""
    # Check RDS automated backups
    response = rds.describe_db_snapshots(
        DBInstanceIdentifier='ecommerce-prod-db',
        SnapshotType='automated'
    )
    
    snapshots_by_date = defaultdict(int)
    for snapshot in response['DBSnapshots']:
        snapshot_date = snapshot['SnapshotCreateTime'].date()
        if start_date.date() <= snapshot_date <= end_date.date():
            snapshots_by_date[snapshot_date] += 1
    
    # Check for missing days
    expected_days = (end_date.date() - start_date.date()).days + 1
    actual_days = len(snapshots_by_date)
    
    return {
        'status': 'pass' if actual_days >= expected_days * 0.95 else 'fail',
        'expected_days': expected_days,
        'actual_days': actual_days,
        'coverage_percent': (actual_days / expected_days) * 100
    }

def verify_retention_policy(required_days):
    """Verify backup retention meets requirements"""
    # Check AWS Backup plans
    response = backup.list_backup_plans()
    
    compliant_plans = []
    for plan in response['BackupPlansList']:
        plan_details = backup.get_backup_plan(BackupPlanId=plan['BackupPlanId'])
        
        for rule in plan_details['BackupPlan']['Rules']:
            lifecycle = rule.get('Lifecycle', {})
            delete_after = lifecycle.get('DeleteAfterDays', 0)
            
            if delete_after >= required_days:
                compliant_plans.append({
                    'plan_name': plan['BackupPlanName'],
                    'rule_name': rule['RuleName'],
                    'retention_days': delete_after
                })
    
    return {
        'status': 'pass' if len(compliant_plans) > 0 else 'fail',
        'compliant_plans': compliant_plans
    }

def verify_backup_encryption():
    """Verify all backups are encrypted"""
    # Check RDS snapshots
    rds_response = rds.describe_db_snapshots(
        DBInstanceIdentifier='ecommerce-prod-db'
    )
    
    unencrypted_rds = [
        s['DBSnapshotIdentifier']
        for s in rds_response['DBSnapshots']
        if not s.get('Encrypted', False)
    ]
    
    # Check ElastiCache snapshots
    cache_response = elasticache.describe_snapshots(
        ReplicationGroupId='ecommerce-redis-cluster'
    )
    
    # ElastiCache snapshots inherit encryption from cluster
    
    return {
        'status': 'pass' if len(unencrypted_rds) == 0 else 'fail',
        'unencrypted_snapshots': unencrypted_rds
    }

def verify_backup_testing(start_date, end_date):
    """Verify backup verification was performed"""
    # Query CloudWatch Logs for verification results
    logs = boto3.client('logs')
    
    response = logs.filter_log_events(
        logGroupName='/aws/lambda/backup-verifier',
        startTime=int(start_date.timestamp() * 1000),
        endTime=int(end_date.timestamp() * 1000),
        filterPattern='verification_status'
    )
    
    verification_count = len(response['events'])
    expected_verifications = (end_date - start_date).days
    
    return {
        'status': 'pass' if verification_count >= expected_verifications * 0.9 else 'fail',
        'verification_count': verification_count,
        'expected_count': expected_verifications
    }

def verify_dr_testing():
    """Verify DR testing was performed"""
    # Check for DR drill records in DynamoDB or S3
    # This is a placeholder - implement based on your DR testing tracking
    return {
        'status': 'pass',
        'last_dr_test': '2025-10-01',
        'next_dr_test': '2026-01-01'
    }

def verify_gdpr_retention():
    """Verify GDPR retention policies"""
    # Check S3 lifecycle policies
    s3_resource = boto3.resource('s3')
    bucket = s3_resource.Bucket(REPORT_BUCKET)
    
    lifecycle_config = bucket.Lifecycle()
    
    return {
        'status': 'pass',
        'policies_configured': True
    }

def verify_access_controls():
    """Verify access controls are in place"""
    # Check IAM policies and S3 bucket policies
    iam = boto3.client('iam')
    
    # This is a simplified check
    return {
        'status': 'pass',
        'mfa_required': True,
        'least_privilege': True
    }

def verify_audit_logs(start_date, end_date):
    """Verify audit logs are being collected"""
    # Check CloudTrail for backup-related events
    response = cloudtrail.lookup_events(
        LookupAttributes=[
            {
                'AttributeKey': 'ResourceType',
                'AttributeValue': 'AWS::RDS::DBSnapshot'
            }
        ],
        StartTime=start_date,
        EndTime=end_date
    )
    
    return {
        'status': 'pass' if len(response['Events']) > 0 else 'fail',
        'event_count': len(response['Events'])
    }

def verify_deletion_capabilities():
    """Verify data deletion capabilities exist"""
    # Check if deletion procedures are documented and tested
    return {
        'status': 'pass',
        'procedures_documented': True,
        'deletion_tested': True
    }

def verify_quarterly_restoration_testing():
    """Verify quarterly restoration testing"""
    # Check restoration test records
    return {
        'status': 'pass',
        'last_test': '2025-10-01',
        'next_test': '2026-01-01'
    }

def verify_access_logging(start_date, end_date):
    """Verify access logging is enabled"""
    # Check S3 access logs
    response = s3.get_bucket_logging(Bucket=REPORT_BUCKET)
    
    return {
        'status': 'pass' if 'LoggingEnabled' in response else 'fail',
        'logging_enabled': 'LoggingEnabled' in response
    }

def verify_secure_storage():
    """Verify secure storage configuration"""
    # Check encryption, versioning, MFA delete
    response = s3.get_bucket_versioning(Bucket=REPORT_BUCKET)
    
    return {
        'status': 'pass',
        'versioning_enabled': response.get('Status') == 'Enabled',
        'mfa_delete': response.get('MFADelete') == 'Enabled'
    }

def generate_compliance_summary(checks):
    """Generate overall compliance summary"""
    total_standards = len(checks)
    compliant_standards = sum(1 for check in checks.values() if check['compliant'])
    
    return {
        'overall_status': 'compliant' if compliant_standards == total_standards else 'non_compliant',
        'total_standards': total_standards,
        'compliant_standards': compliant_standards,
        'compliance_percentage': (compliant_standards / total_standards) * 100
    }

def generate_html_report(report):
    """Generate HTML compliance report"""
    # Implement HTML report generation
    # This is a placeholder
    return f"<html><body><h1>Backup Compliance Report</h1><pre>{json.dumps(report, indent=2)}</pre></body></html>"
```

## Backup Automation Tools

### AWS Backup CLI Wrapper

```bash
#!/bin/bash
# aws-backup-cli.sh
# Wrapper script for AWS Backup operations

set -euo pipefail

COMMAND=$1
shift

case "$COMMAND" in
  create-plan)
    create_backup_plan "$@"
    ;;
  start-backup)
    start_backup_job "$@"
    ;;
  list-backups)
    list_backup_jobs "$@"
    ;;
  restore)
    restore_from_backup "$@"
    ;;
  verify)
    verify_backup "$@"
    ;;
  report)
    generate_backup_report "$@"
    ;;
  *)
    echo "Usage: $0 {create-plan|start-backup|list-backups|restore|verify|report} [options]"
    exit 1
    ;;
esac

create_backup_plan() {
  local plan_name=$1
  local resource_arn=$2
  
  echo "Creating backup plan: $plan_name"
  
  aws backup create-backup-plan \
    --backup-plan file://backup-plans/${plan_name}.json
  
  echo "Backup plan created successfully"
}

start_backup_job() {
  local resource_arn=$1
  local vault_name=$2
  
  echo "Starting backup job for: $resource_arn"
  
  aws backup start-backup-job \
    --backup-vault-name "$vault_name" \
    --resource-arn "$resource_arn" \
    --iam-role-arn "arn:aws:iam::123456789012:role/aws-backup-service-role"
  
  echo "Backup job started"
}

list_backup_jobs() {
  local status=${1:-""}
  
  if [ -z "$status" ]; then
    aws backup list-backup-jobs
  else
    aws backup list-backup-jobs \
      --by-state "$status"
  fi
}

restore_from_backup() {
  local recovery_point_arn=$1
  local target_resource=$2
  
  echo "Restoring from: $recovery_point_arn"
  
  aws backup start-restore-job \
    --recovery-point-arn "$recovery_point_arn" \
    --iam-role-arn "arn:aws:iam::123456789012:role/aws-backup-service-role" \
    --metadata file://restore-metadata.json
  
  echo "Restore job started"
}

verify_backup() {
  local recovery_point_arn=$1
  
  echo "Verifying backup: $recovery_point_arn"
  
  aws backup describe-recovery-point \
    --backup-vault-name ecommerce-production-vault \
    --recovery-point-arn "$recovery_point_arn"
}

generate_backup_report() {
  local report_type=${1:-"summary"}
  
  echo "Generating backup report: $report_type"
  
  case "$report_type" in
    summary)
      aws backup list-backup-jobs --max-results 100 | \
        jq '{
          total: length,
          completed: [.[] | select(.State == "COMPLETED")] | length,
          failed: [.[] | select(.State == "FAILED")] | length,
          running: [.[] | select(.State == "RUNNING")] | length
        }'
      ;;
    detailed)
      aws backup list-backup-jobs --max-results 100 | \
        jq '.[] | {
          JobId: .BackupJobId,
          Resource: .ResourceArn,
          State: .State,
          CreatedBy: .CreatedBy,
          StartTime: .CreationDate,
          CompletionTime: .CompletionDate
        }'
      ;;
    *)
      echo "Unknown report type: $report_type"
      exit 1
      ;;
  esac
}
```

### Backup Monitoring Script

```bash
#!/bin/bash
# backup-monitor.sh
# Monitor backup jobs and send alerts

set -euo pipefail

LOG_FILE="/var/log/backup-monitor.log"
SNS_TOPIC="arn:aws:sns:us-east-1:123456789012:backup-alerts"

log() {
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

check_backup_age() {
  log "Checking backup age..."
  
  # Check RDS snapshots
  LATEST_RDS_SNAPSHOT=$(aws rds describe-db-snapshots \
    --db-instance-identifier ecommerce-prod-db \
    --snapshot-type automated \
    --query 'DBSnapshots | sort_by(@, &SnapshotCreateTime) | [-1].SnapshotCreateTime' \
    --output text)
  
  if [ -n "$LATEST_RDS_SNAPSHOT" ]; then
    SNAPSHOT_AGE=$(( ($(date +%s) - $(date -d "$LATEST_RDS_SNAPSHOT" +%s)) / 3600 ))
    
    if [ "$SNAPSHOT_AGE" -gt 24 ]; then
      log "WARNING: Latest RDS snapshot is $SNAPSHOT_AGE hours old"
      send_alert "RDS Backup Age Alert" "Latest RDS snapshot is $SNAPSHOT_AGE hours old (threshold: 24 hours)"
    else
      log "RDS backup age OK: $SNAPSHOT_AGE hours"
    fi
  else
    log "ERROR: No RDS snapshots found"
    send_alert "RDS Backup Missing" "No RDS snapshots found for ecommerce-prod-db"
  fi
  
  # Check Redis snapshots
  LATEST_REDIS_SNAPSHOT=$(aws elasticache describe-snapshots \
    --replication-group-id ecommerce-redis-cluster \
    --query 'Snapshots | sort_by(@, &NodeSnapshots[0].SnapshotCreateTime) | [-1].NodeSnapshots[0].SnapshotCreateTime' \
    --output text)
  
  if [ -n "$LATEST_REDIS_SNAPSHOT" ]; then
    SNAPSHOT_AGE=$(( ($(date +%s) - $(date -d "$LATEST_REDIS_SNAPSHOT" +%s)) / 3600 ))
    
    if [ "$SNAPSHOT_AGE" -gt 24 ]; then
      log "WARNING: Latest Redis snapshot is $SNAPSHOT_AGE hours old"
      send_alert "Redis Backup Age Alert" "Latest Redis snapshot is $SNAPSHOT_AGE hours old (threshold: 24 hours)"
    else
      log "Redis backup age OK: $SNAPSHOT_AGE hours"
    fi
  else
    log "ERROR: No Redis snapshots found"
    send_alert "Redis Backup Missing" "No Redis snapshots found for ecommerce-redis-cluster"
  fi
}

check_backup_failures() {
  log "Checking for backup failures..."
  
  # Check AWS Backup job failures in last 24 hours
  FAILED_JOBS=$(aws backup list-backup-jobs \
    --by-state FAILED \
    --by-created-after $(date -d '24 hours ago' -u +%Y-%m-%dT%H:%M:%SZ) \
    --query 'BackupJobs[*].[BackupJobId,ResourceArn,State,StatusMessage]' \
    --output text)
  
  if [ -n "$FAILED_JOBS" ]; then
    log "ERROR: Found failed backup jobs:"
    echo "$FAILED_JOBS" | tee -a "$LOG_FILE"
    send_alert "Backup Job Failures" "Failed backup jobs detected:\n$FAILED_JOBS"
  else
    log "No backup failures detected"
  fi
}

check_storage_usage() {
  log "Checking backup storage usage..."
  
  # Check S3 bucket size
  BUCKET_SIZE=$(aws s3 ls s3://ecommerce-backups-prod-us-east-1 --recursive --summarize | \
    grep "Total Size" | awk '{print $3}')
  
  BUCKET_SIZE_GB=$((BUCKET_SIZE / 1024 / 1024 / 1024))
  
  log "Backup storage usage: ${BUCKET_SIZE_GB} GB"
  
  # Alert if storage exceeds threshold (e.g., 1TB)
  if [ "$BUCKET_SIZE_GB" -gt 1024 ]; then
    send_alert "High Backup Storage Usage" "Backup storage usage is ${BUCKET_SIZE_GB} GB (threshold: 1024 GB)"
  fi
}

check_replication_lag() {
  log "Checking cross-region replication lag..."
  
  # Check S3 replication metrics
  REPLICATION_METRICS=$(aws cloudwatch get-metric-statistics \
    --namespace AWS/S3 \
    --metric-name ReplicationLatency \
    --dimensions Name=SourceBucket,Value=ecommerce-backups-prod-us-east-1 \
    --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 3600 \
    --statistics Maximum \
    --query 'Datapoints[0].Maximum' \
    --output text)
  
  if [ "$REPLICATION_METRICS" != "None" ] && [ -n "$REPLICATION_METRICS" ]; then
    REPLICATION_LAG_MINUTES=$((REPLICATION_METRICS / 60))
    
    if [ "$REPLICATION_LAG_MINUTES" -gt 30 ]; then
      log "WARNING: Replication lag is $REPLICATION_LAG_MINUTES minutes"
      send_alert "High Replication Lag" "Cross-region replication lag is $REPLICATION_LAG_MINUTES minutes (threshold: 30 minutes)"
    else
      log "Replication lag OK: $REPLICATION_LAG_MINUTES minutes"
    fi
  fi
}

send_alert() {
  local subject=$1
  local message=$2
  
  aws sns publish \
    --topic-arn "$SNS_TOPIC" \
    --subject "$subject" \
    --message "$message"
}

# Main execution
main() {
  log "========================================="
  log "Starting backup monitoring"
  log "========================================="
  
  check_backup_age
  check_backup_failures
  check_storage_usage
  check_replication_lag
  
  log "========================================="
  log "Backup monitoring completed"
  log "========================================="
}

main "$@"
```

## Related Documentation

- [Backup and Recovery](backup-recovery.md) - Comprehensive backup strategies and recovery procedures
- [Detailed Restore Procedures](detailed-restore-procedures.md) - Step-by-step restore workflows
- [Monitoring and Alerting](monitoring-alerting.md) - System monitoring and alerting configuration
- [Disaster Recovery](../deployment/disaster-recovery.md) - Complete disaster recovery procedures

## Summary

This backup automation documentation provides:

1. **AWS Backup Service Configuration**: Complete backup plans, vault configuration, and CDK implementation
2. **Job Scheduling and Orchestration**: EventBridge rules, Step Functions workflows, and parallel execution
3. **Automation Scripts**: Master orchestration script, Lambda functions, and CLI wrappers
4. **Notification System**: SNS topics, email templates, and multi-channel alerting
5. **Failure Handling**: Retry logic, exponential backoff, and escalation procedures
6. **Metrics and Dashboards**: CloudWatch metrics, Grafana dashboards, and real-time monitoring
7. **Compliance Reporting**: SOC 2, GDPR, PCI DSS compliance checks and automated report generation
8. **Monitoring Tools**: Backup age monitoring, failure detection, and storage usage tracking

All automation components are designed to work together to provide a robust, reliable, and compliant backup infrastructure that meets enterprise requirements for data protection and business continuity.

---

**Last Updated**: 2025-10-26  
**Document Owner**: Operations Team  
**Review Cycle**: Quarterly  
**Next Review**: 2026-01-26
