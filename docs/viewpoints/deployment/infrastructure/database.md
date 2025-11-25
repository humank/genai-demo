---
title: "Database Infrastructure"
type: "deployment-viewpoint"
category: "infrastructure"
status: "active"
last_updated: "2025-10-23"
owner: "DevOps Team"
---

# Database Infrastructure

> **Service**: Amazon RDS (Relational Database Service)

## Primary Instance Configuration

**Instance Details**:
```yaml
Identifier: ecommerce-platform-prod-primary
Engine: PostgreSQL 15.4
Instance Class: db.r5.xlarge
  - vCPU: 4
  - Memory: 32 GiB
  - Network: Up to 10 Gbps
  - EBS Bandwidth: Up to 4,750 Mbps

Storage:
  Type: gp3 (General Purpose SSD)
  Allocated: 500 GB
  Max Allocated: 2000 GB (auto-scaling enabled)
  IOPS: 12,000
  Throughput: 500 MB/s

Deployment:
  Multi-AZ: Enabled
  Primary AZ: us-east-1a
  Standby AZ: us-east-1b
```

**Database Configuration**:
```sql
-- PostgreSQL Parameters
max_connections = 200
shared_buffers = 8GB
effective_cache_size = 24GB
maintenance_work_mem = 2GB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 20MB
min_wal_size = 2GB
max_wal_size = 8GB
max_worker_processes = 4
max_parallel_workers_per_gather = 2
max_parallel_workers = 4
```

## Read Replica Configuration

**Replica 1** (us-east-1c):
```yaml
Identifier: ecommerce-platform-prod-replica-1
Instance Class: db.r5.large
  - vCPU: 2
  - Memory: 16 GiB
Storage:
  Type: gp3
  Allocated: 500 GB
Replication:
  Source: ecommerce-platform-prod-primary
  Lag: < 1 second (typical)
```

**Replica 2** (us-east-1a):
```yaml
Identifier: ecommerce-platform-prod-replica-2
Instance Class: db.r5.large
  - vCPU: 2
  - Memory: 16 GiB
Storage:
  Type: gp3
  Allocated: 500 GB
Replication:
  Source: ecommerce-platform-prod-primary
  Lag: < 1 second (typical)
```

## Backup and Maintenance

**Automated Backups**:
- Backup Retention: 7 days
- Backup Window: 03:00-04:00 UTC (off-peak hours)
- Copy to Region: us-west-2 (disaster recovery)
- Point-in-Time Recovery: Enabled (up to 7 days)

**Maintenance Window**:
- Preferred Window: Sun 04:00-05:00 UTC
- Auto Minor Version Upgrade: Enabled

**Monitoring**:
- Enhanced Monitoring: Enabled (60-second granularity)
- Performance Insights: Enabled (7-day retention)
- CloudWatch Alarms:
  - CPU Utilization > 80%
  - Free Storage Space < 20%
  - Database Connections > 180
  - Replication Lag > 5 seconds
