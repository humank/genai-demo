# Database Operations Examples

> **Purpose**: Practical, runnable examples for common database operations  
> **Last Updated**: 2024-11-19  
> **Owner**: Database Team

---

## Overview

This document provides practical, tested examples for database backup, restore, performance tuning, and maintenance operations.

---

## Example 1: Create Manual Database Backup

### Scenario

Create an on-demand backup before performing maintenance or risky operations.

### Prerequisites

- AWS CLI configured
- RDS instance access
- Sufficient storage quota

### Steps

```bash
# Set variables
export DB_INSTANCE="ecommerce-production"
export SNAPSHOT_ID="manual-backup-$(date +%Y%m%d-%H%M%S)"

# Create snapshot
aws rds create-db-snapshot \
  --db-instance-identifier ${DB_INSTANCE} \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --tags Key=Type,Value=Manual Key=Purpose,Value=PreMaintenance

# Monitor snapshot progress
aws rds describe-db-snapshots \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --query 'DBSnapshots[0].[Status,PercentProgress]' \
  --output table

# Wait for completion
aws rds wait db-snapshot-completed \
  --db-snapshot-identifier ${SNAPSHOT_ID}

# Verify snapshot
aws rds describe-db-snapshots \
  --db-snapshot-identifier ${SNAPSHOT_ID} \
  --query 'DBSnapshots[0].[DBSnapshotIdentifier,SnapshotCreateTime,Status,AllocatedStorage]' \
  --output table
```

**Expected Output**:

```text
---------------------------------------------------------------
|                    DescribeDBSnapshots                      |
+----------------------------------+------------+--------+-----+
|  manual-backup-20241119-143000   | 2024-11-19 | available | 500 |
+----------------------------------+------------+--------+-----+
```

### Troubleshooting

**Issue**: Snapshot creation fails

```bash
# Check instance status
aws rds describe-db-instances \
  --db-instance-identifier ${DB_INSTANCE} \
  --query 'DBInstances[0].DBInstanceStatus'

# Check for ongoing snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier ${DB_INSTANCE} \
  --query 'DBSnapshots[?Status==`creating`]'
```

---

## Example 2: Restore Database from Snapshot

### Scenario

Restore database to a specific point in time or from a snapshot.

### Prerequisites

- Valid snapshot available
- Sufficient RDS quota
- Network configuration prepared

### Steps

**1. List Available Snapshots**

```bash
# List recent snapshots
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-production \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime,Status]' \
  --output table | head -20
```

**2. Restore from Snapshot**

```bash
# Set variables
export SOURCE_SNAPSHOT="manual-backup-20241119-143000"
export NEW_INSTANCE="ecommerce-production-restored"
export SUBNET_GROUP="ecommerce-db-subnet-group"
export SECURITY_GROUP="sg-0123456789abcdef0"

# Restore database
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ${NEW_INSTANCE} \
  --db-snapshot-identifier ${SOURCE_SNAPSHOT} \
  --db-instance-class db.r5.xlarge \
  --db-subnet-group-name ${SUBNET_GROUP} \
  --vpc-security-group-ids ${SECURITY_GROUP} \
  --publicly-accessible false \
  --multi-az true \
  --tags Key=Environment,Value=Restored Key=Purpose,Value=Recovery

# Monitor restore progress
aws rds describe-db-instances \
  --db-instance-identifier ${NEW_INSTANCE} \
  --query 'DBInstances[0].[DBInstanceStatus,PercentProgress]' \
  --output table

# Wait for availability
aws rds wait db-instance-available \
  --db-instance-identifier ${NEW_INSTANCE}
```

**3. Verify Restored Database**

```bash
# Get endpoint
export RESTORED_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ${NEW_INSTANCE} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Test connection
psql -h ${RESTORED_ENDPOINT} \
     -U ${DB_USER} \
     -d ecommerce \
     -c "SELECT version();"

# Verify data
psql -h ${RESTORED_ENDPOINT} \
     -U ${DB_USER} \
     -d ecommerce \
     -c "SELECT COUNT(*) FROM orders;"
```

**Expected Output**:

```text
 count
-------
 15234
(1 row)
```

---

## Example 3: Point-in-Time Recovery (PITR)

### Scenario

Restore database to a specific timestamp to recover from data corruption or accidental deletion.

### Prerequisites

- Automated backups enabled
- Target restore time within backup retention period
- Sufficient RDS quota

### Steps

```bash
# Set variables
export SOURCE_INSTANCE="ecommerce-production"
export TARGET_INSTANCE="ecommerce-pitr-recovery"
export RESTORE_TIME="2024-11-19T14:00:00Z"

# Get latest restorable time
aws rds describe-db-instances \
  --db-instance-identifier ${SOURCE_INSTANCE} \
  --query 'DBInstances[0].LatestRestorableTime'

# Perform PITR
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ${SOURCE_INSTANCE} \
  --target-db-instance-identifier ${TARGET_INSTANCE} \
  --restore-time ${RESTORE_TIME} \
  --db-instance-class db.r5.xlarge \
  --multi-az true \
  --publicly-accessible false

# Monitor restore
aws rds wait db-instance-available \
  --db-instance-identifier ${TARGET_INSTANCE}

# Verify restored data
export PITR_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier ${TARGET_INSTANCE} \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

psql -h ${PITR_ENDPOINT} -U ${DB_USER} -d ecommerce \
  -c "SELECT MAX(created_at) FROM orders;"
```

---

## Example 4: Database Performance Tuning

### Scenario

Identify and optimize slow queries to improve database performance.

### Prerequisites

- `pg_stat_statements` extension enabled
- Database connection access
- Monitoring tools configured

### Steps

**1. Enable Query Statistics**

```sql
-- Connect to database
psql -h ${DB_HOST} -U ${DB_USER} -d ecommerce

-- Enable pg_stat_statements
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Verify extension
SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';
```

**2. Identify Slow Queries**

```sql
-- Top 10 slowest queries by average execution time
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    max_time,
    stddev_time,
    rows
FROM pg_stat_statements
WHERE mean_time > 100  -- queries taking > 100ms
ORDER BY mean_time DESC
LIMIT 10;

-- Queries with high total time
SELECT 
    query,
    calls,
    total_time,
    mean_time,
    (total_time / sum(total_time) OVER ()) * 100 AS percentage
FROM pg_stat_statements
ORDER BY total_time DESC
LIMIT 10;

-- Queries with many calls
SELECT 
    query,
    calls,
    mean_time,
    total_time
FROM pg_stat_statements
WHERE calls > 1000
ORDER BY calls DESC
LIMIT 10;
```

**Expected Output**:

```text
                    query                     | calls | total_time | mean_time | max_time
----------------------------------------------+-------+------------+-----------+----------
 SELECT * FROM orders WHERE customer_id = $1  | 5234  | 523400.50  | 100.01    | 1500.23
 SELECT * FROM products WHERE category = $1   | 8901  | 445050.00  | 50.00     | 800.45
```

**3. Analyze Query Execution Plans**

```sql
-- Analyze specific slow query
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 
SELECT * FROM orders 
WHERE customer_id = '12345' 
  AND created_at >= '2024-01-01';

-- Check for missing indexes
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    seq_tup_read / NULLIF(seq_scan, 0) AS avg_seq_tup
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 10;
```

**4. Create Indexes**

```sql
-- Create index for slow query
CREATE INDEX CONCURRENTLY idx_orders_customer_created 
ON orders(customer_id, created_at);

-- Verify index creation
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'orders'
  AND indexname = 'idx_orders_customer_created';

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE indexname = 'idx_orders_customer_created';
```

**5. Verify Performance Improvement**

```sql
-- Reset statistics
SELECT pg_stat_statements_reset();

-- Wait for queries to execute
-- Then check improved performance
SELECT 
    query,
    calls,
    mean_time,
    max_time
FROM pg_stat_statements
WHERE query LIKE '%customer_id%'
ORDER BY mean_time DESC;
```

---

## Example 5: Database Maintenance - VACUUM and ANALYZE

### Scenario

Perform routine maintenance to reclaim space and update statistics.

### Prerequisites

- Database connection access
- Maintenance window scheduled
- Sufficient disk space

### Steps

**1. Check Table Bloat**

```sql
-- Check for bloated tables
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
    n_dead_tup,
    n_live_tup,
    ROUND(n_dead_tup * 100.0 / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_tuple_percent
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC
LIMIT 10;
```

**Expected Output**:

```text
 schemaname | tablename | total_size | n_dead_tup | n_live_tup | dead_tuple_percent
------------+-----------+------------+------------+------------+--------------------
 public     | orders    | 2048 MB    | 125000     | 500000     | 20.00
 public     | products  | 512 MB     | 50000      | 200000     | 20.00
```

**2. Perform VACUUM ANALYZE**

```sql
-- Vacuum specific table
VACUUM (VERBOSE, ANALYZE) orders;

-- Vacuum all tables
VACUUM (VERBOSE, ANALYZE);

-- Full vacuum (requires exclusive lock)
VACUUM FULL VERBOSE orders;
```

**Expected Output**:

```text
INFO:  vacuuming "public.orders"
INFO:  "orders": removed 125000 dead row versions in 15625 pages
INFO:  "orders": found 125000 removable, 500000 nonremovable row versions
INFO:  analyzing "public.orders"
INFO:  "orders": scanned 62500 of 62500 pages, containing 500000 live rows
```

**3. Update Statistics**

```sql
-- Analyze specific table
ANALYZE VERBOSE orders;

-- Analyze all tables
ANALYZE VERBOSE;

-- Check statistics freshness
SELECT 
    schemaname,
    tablename,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY last_analyze NULLS FIRST;
```

**4. Monitor Autovacuum**

```sql
-- Check autovacuum settings
SELECT 
    name,
    setting,
    unit,
    short_desc
FROM pg_settings
WHERE name LIKE 'autovacuum%';

-- Check autovacuum activity
SELECT 
    schemaname,
    tablename,
    last_autovacuum,
    autovacuum_count
FROM pg_stat_user_tables
WHERE last_autovacuum IS NOT NULL
ORDER BY last_autovacuum DESC
LIMIT 10;
```

---

## Example 6: Connection Pool Management

### Scenario

Monitor and manage database connections to prevent connection exhaustion.

### Prerequisites

- Database connection access
- HikariCP or similar connection pool configured

### Steps

**1. Check Current Connections**

```sql
-- Count active connections
SELECT 
    datname,
    count(*) AS connections
FROM pg_stat_activity
GROUP BY datname
ORDER BY connections DESC;

-- Check connection details
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query_start,
    state_change,
    query
FROM pg_stat_activity
WHERE datname = 'ecommerce'
ORDER BY query_start DESC;
```

**Expected Output**:

```text
  datname   | connections
------------+-------------
 ecommerce  | 45
 postgres   | 5
```

**2. Identify Idle Connections**

```sql
-- Find long-running idle connections
SELECT 
    pid,
    usename,
    application_name,
    state,
    state_change,
    now() - state_change AS idle_duration
FROM pg_stat_activity
WHERE state = 'idle'
  AND now() - state_change > interval '10 minutes'
ORDER BY idle_duration DESC;

-- Terminate idle connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
  AND now() - state_change > interval '30 minutes'
  AND pid <> pg_backend_pid();
```

**3. Monitor Connection Pool (HikariCP)**

```bash
# Check HikariCP metrics via JMX
jconsole

# Or via Spring Boot Actuator
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

**4. Optimize Connection Pool Settings**

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000  # 5 minutes
      max-lifetime: 1200000  # 20 minutes
      connection-timeout: 20000  # 20 seconds
      leak-detection-threshold: 60000  # 1 minute
```

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Database Team
