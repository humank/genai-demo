# Database Maintenance Guide

## Overview

This guide covers routine database maintenance tasks for PostgreSQL RDS instances.

## Routine Maintenance Tasks

### Daily Tasks

#### Monitor Database Performance

```bash
# Check database metrics
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name CPUUtilization \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production \
  --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average

# Check connection count
psql -c "SELECT count(*) FROM pg_stat_activity;"

# Check slow queries
psql -c "SELECT query, calls, mean_time FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;"
```

#### Check Backup Status

```bash
# Verify automated backups
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-production \
  --snapshot-type automated \
  --query 'DBSnapshots[0].[DBSnapshotIdentifier,SnapshotCreateTime,Status]'
```

### Weekly Tasks

#### Analyze and Vacuum

```sql
-- Analyze all tables
ANALYZE;

-- Vacuum analyze (reclaim space and update statistics)
VACUUM ANALYZE;

-- Check for bloat
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
       n_dead_tup
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;
```

#### Review Slow Queries

```sql
-- Get slowest queries
SELECT query, calls, total_time, mean_time, max_time,
       stddev_time, rows
FROM pg_stat_statements
WHERE mean_time > 100  -- queries taking > 100ms
ORDER BY mean_time DESC
LIMIT 20;

-- Reset statistics after review
SELECT pg_stat_statements_reset();
```

#### Check Index Usage

```sql
-- Find unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Find missing indexes (tables with many sequential scans)
SELECT schemaname, tablename, seq_scan, seq_tup_read,
       idx_scan, seq_tup_read / seq_scan AS avg_seq_tup
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;
```

### Monthly Tasks

#### Reindex

```sql
-- Reindex database (during maintenance window)
REINDEX DATABASE ecommerce_production;

-- Or reindex specific tables
REINDEX TABLE orders;
REINDEX TABLE customers;
```

#### Update Statistics

```sql
-- Update table statistics
ANALYZE VERBOSE;

-- Check statistics age
SELECT schemaname, tablename, last_analyze, last_autoanalyze
FROM pg_stat_user_tables
ORDER BY last_analyze NULLS FIRST;
```

#### Review and Optimize Queries

```sql
-- Identify queries needing optimization
SELECT query, calls, total_time, mean_time,
       (total_time / sum(total_time) OVER ()) * 100 AS percentage
FROM pg_stat_statements
WHERE calls > 100
ORDER BY total_time DESC
LIMIT 20;
```

### Quarterly Tasks

#### Database Upgrade Planning

```bash
# Check current version
psql -c "SELECT version();"

# Review upgrade path
aws rds describe-db-engine-versions \
  --engine postgres \
  --engine-version 14.7 \
  --query 'DBEngineVersions[0].ValidUpgradeTarget'
```

#### Capacity Planning

```sql
-- Check database size growth
SELECT pg_database.datname,
       pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database
ORDER BY pg_database_size(pg_database.datname) DESC;

-- Check table sizes
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
```

## VACUUM and Space Management

### Understanding VACUUM

PostgreSQL uses Multi-Version Concurrency Control (MVCC), which creates dead tuples when rows are updated or deleted. VACUUM reclaims this space and updates statistics.

#### Types of VACUUM

**VACUUM (Standard)**
- Reclaims space for reuse within the same table
- Does not return space to the operating system
- Can run concurrently with normal operations
- Recommended for regular maintenance

**VACUUM FULL**
- Rewrites entire table to reclaim space
- Returns space to the operating system
- Requires exclusive lock (blocks all operations)
- Use only when necessary due to downtime

**VACUUM ANALYZE**
- Combines VACUUM with statistics update
- Recommended for most maintenance scenarios

### VACUUM Procedures

#### Manual VACUUM

```sql
-- Vacuum specific table
VACUUM orders;

-- Vacuum with analyze
VACUUM ANALYZE orders;

-- Vacuum all tables in database
VACUUM;

-- Verbose output for monitoring
VACUUM VERBOSE ANALYZE orders;

-- Check last vacuum time
SELECT schemaname, tablename, 
       last_vacuum, last_autovacuum,
       n_dead_tup, n_live_tup,
       ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_tuple_percent
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

#### VACUUM FULL Procedures

**⚠️ WARNING**: VACUUM FULL requires exclusive table lock and can take hours on large tables.

```sql
-- Before VACUUM FULL
-- 1. Create backup
-- 2. Schedule maintenance window
-- 3. Notify users of downtime

-- Check table bloat first
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       ROUND(100 * pg_relation_size(schemaname||'.'||tablename) / 
             NULLIF(pg_total_relation_size(schemaname||'.'||tablename), 0), 2) AS table_percent
FROM pg_stat_user_tables
WHERE pg_total_relation_size(schemaname||'.'||tablename) > 1073741824  -- > 1GB
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Execute VACUUM FULL (during maintenance window)
VACUUM FULL VERBOSE ANALYZE orders;

-- Monitor progress
SELECT pid, query, state, 
       now() - query_start AS duration
FROM pg_stat_activity
WHERE query LIKE 'VACUUM FULL%';
```

**VACUUM FULL Risks**:
- Requires 2x table size in disk space temporarily
- Blocks all table access during operation
- Can take hours on large tables
- May cause replication lag
- Consider pg_repack as alternative

### Autovacuum Configuration

#### Check Autovacuum Status

```sql
-- Check if autovacuum is enabled
SHOW autovacuum;

-- Check autovacuum settings
SELECT name, setting, unit, context
FROM pg_settings
WHERE name LIKE 'autovacuum%'
ORDER BY name;

-- Monitor autovacuum activity
SELECT schemaname, tablename,
       last_autovacuum, last_autoanalyze,
       autovacuum_count, autoanalyze_count
FROM pg_stat_user_tables
ORDER BY last_autovacuum DESC NULLS LAST;
```

#### Autovacuum Tuning

```sql
-- RDS Parameter Group Settings (recommended)
-- autovacuum = on
-- autovacuum_max_workers = 3-5 (based on CPU cores)
-- autovacuum_naptime = 15s (default: 1min)
-- autovacuum_vacuum_threshold = 50
-- autovacuum_analyze_threshold = 50
-- autovacuum_vacuum_scale_factor = 0.1 (10% of table)
-- autovacuum_analyze_scale_factor = 0.05 (5% of table)

-- Per-table autovacuum tuning for high-churn tables
ALTER TABLE orders SET (
  autovacuum_vacuum_scale_factor = 0.05,
  autovacuum_analyze_scale_factor = 0.02,
  autovacuum_vacuum_threshold = 100,
  autovacuum_analyze_threshold = 100
);

-- Disable autovacuum for specific table (not recommended)
ALTER TABLE archive_table SET (autovacuum_enabled = false);

-- Check per-table settings
SELECT schemaname, tablename, reloptions
FROM pg_tables
WHERE reloptions IS NOT NULL;
```

#### Monitor Autovacuum Performance

```sql
-- Check autovacuum running processes
SELECT pid, usename, datname, state,
       now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE query LIKE '%autovacuum%'
  AND query NOT LIKE '%pg_stat_activity%';

-- Check autovacuum statistics
SELECT schemaname, tablename,
       n_tup_ins, n_tup_upd, n_tup_del,
       n_live_tup, n_dead_tup,
       last_vacuum, last_autovacuum,
       vacuum_count, autovacuum_count
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;
```

### Table Bloat Detection

#### Bloat Detection Queries

```sql
-- Detect table bloat
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       n_dead_tup,
       n_live_tup,
       ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS bloat_percent,
       CASE 
         WHEN n_dead_tup > n_live_tup * 0.2 THEN 'HIGH'
         WHEN n_dead_tup > n_live_tup * 0.1 THEN 'MEDIUM'
         ELSE 'LOW'
       END AS bloat_level
FROM pg_stat_user_tables
WHERE n_live_tup > 0
ORDER BY n_dead_tup DESC;

-- Detailed bloat estimation
SELECT schemaname, tablename,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       ROUND(100 * pg_relation_size(schemaname||'.'||tablename)::numeric / 
             NULLIF(pg_total_relation_size(schemaname||'.'||tablename), 0), 2) AS table_percent,
       n_dead_tup,
       ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup, 0), 2) AS dead_tuple_ratio
FROM pg_stat_user_tables
WHERE pg_total_relation_size(schemaname||'.'||tablename) > 104857600  -- > 100MB
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Index bloat detection
SELECT schemaname, tablename, indexname,
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
       idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE pg_relation_size(indexrelid) > 104857600  -- > 100MB
ORDER BY pg_relation_size(indexrelid) DESC;
```

#### Bloat Remediation

```sql
-- For moderate bloat (< 30%): Regular VACUUM
VACUUM ANALYZE orders;

-- For high bloat (> 30%): Consider VACUUM FULL or pg_repack
-- Option 1: VACUUM FULL (requires downtime)
VACUUM FULL VERBOSE ANALYZE orders;

-- Option 2: pg_repack (online, no downtime - see below)
```

### pg_repack for Online Table Reorganization

pg_repack is a PostgreSQL extension that reorganizes tables without blocking reads/writes.

#### Installing pg_repack

```bash
# On RDS, pg_repack is available as an extension
# Enable in parameter group: shared_preload_libraries = 'pg_repack'

# Create extension in database
psql -c "CREATE EXTENSION pg_repack;"

# Verify installation
psql -c "SELECT * FROM pg_available_extensions WHERE name = 'pg_repack';"
```

#### Using pg_repack

```bash
# Repack specific table
pg_repack -h your-rds-endpoint.amazonaws.com \
          -U postgres \
          -d ecommerce_production \
          -t orders \
          --no-order

# Repack all tables in database
pg_repack -h your-rds-endpoint.amazonaws.com \
          -U postgres \
          -d ecommerce_production \
          --no-order

# Repack with specific options
pg_repack -h your-rds-endpoint.amazonaws.com \
          -U postgres \
          -d ecommerce_production \
          -t orders \
          --no-order \
          --jobs 2 \
          --wait-timeout 60

# Dry run to estimate time
pg_repack -h your-rds-endpoint.amazonaws.com \
          -U postgres \
          -d ecommerce_production \
          -t orders \
          --dry-run
```

#### pg_repack Monitoring

```sql
-- Monitor pg_repack progress
SELECT pid, usename, datname, state,
       now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE query LIKE '%repack%'
  AND query NOT LIKE '%pg_stat_activity%';

-- Check table size before and after
SELECT pg_size_pretty(pg_total_relation_size('orders'));
```

### Space Reclamation Strategies

#### Strategy Selection

| Bloat Level | Strategy | Downtime | Duration |
|-------------|----------|----------|----------|
| < 10% | Regular VACUUM | None | Minutes |
| 10-30% | VACUUM ANALYZE | None | Minutes-Hours |
| 30-50% | pg_repack | Minimal | Hours |
| > 50% | VACUUM FULL or pg_repack | VACUUM FULL: Yes<br>pg_repack: Minimal | Hours-Days |

#### Space Reclamation Workflow

```sql
-- 1. Identify bloated tables
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
       ROUND(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS bloat_percent
FROM pg_stat_user_tables
WHERE n_dead_tup > n_live_tup * 0.1
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 2. Try VACUUM first
VACUUM ANALYZE orders;

-- 3. Check if bloat reduced
-- (Re-run bloat detection query)

-- 4. If bloat persists, use pg_repack
-- (See pg_repack commands above)

-- 5. Monitor space reclaimed
SELECT pg_size_pretty(pg_database_size(current_database())) AS database_size;
```

### TOAST Table Management

TOAST (The Oversized-Attribute Storage Technique) stores large column values separately.

#### Monitor TOAST Tables

```sql
-- Find tables with TOAST
SELECT schemaname, tablename,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
       pg_size_pretty(pg_relation_size(schemaname||'.pg_toast.pg_toast_'||
                      (schemaname||'.'||tablename)::regclass::oid)) AS toast_size
FROM pg_stat_user_tables
WHERE EXISTS (
  SELECT 1 FROM pg_class
  WHERE relname = 'pg_toast_' || (schemaname||'.'||tablename)::regclass::oid
)
ORDER BY pg_relation_size(schemaname||'.pg_toast.pg_toast_'||
         (schemaname||'.'||tablename)::regclass::oid) DESC;

-- Check TOAST bloat
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - 
                      pg_relation_size(schemaname||'.'||tablename)) AS toast_and_index_size
FROM pg_stat_user_tables
WHERE pg_total_relation_size(schemaname||'.'||tablename) > 
      pg_relation_size(schemaname||'.'||tablename) * 2
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

#### TOAST Table Maintenance

```sql
-- Vacuum TOAST tables
VACUUM pg_toast.pg_toast_16384;  -- Replace with actual TOAST table name

-- Analyze TOAST tables
ANALYZE pg_toast.pg_toast_16384;

-- Reindex TOAST tables
REINDEX TABLE pg_toast.pg_toast_16384;
```

### Database Size Monitoring

#### Current Size Monitoring

```sql
-- Database size
SELECT pg_database.datname,
       pg_size_pretty(pg_database_size(pg_database.datname)) AS size,
       pg_database_size(pg_database.datname) AS size_bytes
FROM pg_database
ORDER BY pg_database_size(pg_database.datname) DESC;

-- Table sizes with breakdown
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
       pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
       pg_size_pretty(pg_indexes_size(schemaname||'.'||tablename)) AS indexes_size,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - 
                      pg_relation_size(schemaname||'.'||tablename) - 
                      pg_indexes_size(schemaname||'.'||tablename)) AS toast_size,
       pg_total_relation_size(schemaname||'.'||tablename) AS total_bytes
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;

-- Tablespace usage
SELECT spcname, 
       pg_size_pretty(pg_tablespace_size(spcname)) AS size
FROM pg_tablespace;
```

#### Size Growth Tracking

```sql
-- Create size tracking table
CREATE TABLE IF NOT EXISTS database_size_history (
    recorded_at TIMESTAMP DEFAULT NOW(),
    database_name TEXT,
    size_bytes BIGINT,
    table_count INTEGER,
    index_count INTEGER
);

-- Record current size (run daily)
INSERT INTO database_size_history (database_name, size_bytes, table_count, index_count)
SELECT current_database(),
       pg_database_size(current_database()),
       (SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema')),
       (SELECT count(*) FROM pg_indexes WHERE schemaname NOT IN ('pg_catalog', 'information_schema'));

-- View growth trend
SELECT recorded_at::date AS date,
       pg_size_pretty(size_bytes) AS size,
       pg_size_pretty(size_bytes - LAG(size_bytes) OVER (ORDER BY recorded_at)) AS daily_growth
FROM database_size_history
WHERE recorded_at > NOW() - INTERVAL '30 days'
ORDER BY recorded_at DESC;
```

#### Size Forecasting

```sql
-- Calculate average daily growth
WITH daily_growth AS (
    SELECT recorded_at::date AS date,
           size_bytes,
           size_bytes - LAG(size_bytes) OVER (ORDER BY recorded_at::date) AS growth
    FROM database_size_history
    WHERE recorded_at > NOW() - INTERVAL '90 days'
)
SELECT pg_size_pretty(AVG(growth)) AS avg_daily_growth,
       pg_size_pretty(AVG(growth) * 30) AS projected_monthly_growth,
       pg_size_pretty(AVG(growth) * 365) AS projected_yearly_growth
FROM daily_growth
WHERE growth > 0;

-- Forecast when storage will reach capacity
WITH growth_rate AS (
    SELECT AVG(size_bytes - LAG(size_bytes) OVER (ORDER BY recorded_at)) AS daily_growth
    FROM database_size_history
    WHERE recorded_at > NOW() - INTERVAL '90 days'
),
current_size AS (
    SELECT pg_database_size(current_database()) AS size_bytes
),
storage_limit AS (
    SELECT 1099511627776 AS limit_bytes  -- 1TB example
)
SELECT pg_size_pretty(current_size.size_bytes) AS current_size,
       pg_size_pretty(storage_limit.limit_bytes) AS storage_limit,
       pg_size_pretty(storage_limit.limit_bytes - current_size.size_bytes) AS remaining,
       ROUND((storage_limit.limit_bytes - current_size.size_bytes) / 
             NULLIF(growth_rate.daily_growth, 0)) AS days_until_full,
       (NOW() + ((storage_limit.limit_bytes - current_size.size_bytes) / 
                 NULLIF(growth_rate.daily_growth, 0) || ' days')::INTERVAL)::date AS estimated_full_date
FROM current_size, storage_limit, growth_rate;
```

### Space Management Best Practices

#### Regular Maintenance Schedule

```bash
# Daily: Monitor autovacuum
psql -c "SELECT * FROM pg_stat_progress_vacuum;"

# Weekly: Check bloat
psql -f check_bloat.sql

# Monthly: Review and optimize
psql -c "VACUUM ANALYZE;"

# Quarterly: Deep analysis and pg_repack if needed
pg_repack -d ecommerce_production --dry-run
```

#### Alerting Thresholds

```yaml
# CloudWatch/Grafana alerts
- DatabaseStorageUsage: > 80% for 1 hour
- TableBloatHigh: Dead tuples > 30% for 1 day
- AutovacuumNotRunning: No autovacuum for 24 hours
- DatabaseGrowthAnomaly: Growth > 2x average for 1 day
```

#### Emergency Space Recovery

```sql
-- 1. Identify largest tables
SELECT schemaname, tablename,
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;

-- 2. Check for old data to archive
SELECT MIN(created_at), MAX(created_at), COUNT(*)
FROM orders
WHERE created_at < NOW() - INTERVAL '2 years';

-- 3. Archive old data
CREATE TABLE orders_archive AS
SELECT * FROM orders
WHERE created_at < NOW() - INTERVAL '2 years';

DELETE FROM orders
WHERE created_at < NOW() - INTERVAL '2 years';

-- 4. Reclaim space
VACUUM FULL ANALYZE orders;

-- 5. Verify space reclaimed
SELECT pg_size_pretty(pg_database_size(current_database()));
```

## Performance Tuning

### Connection Pool Optimization

```yaml
# HikariCP configuration
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
```

### Query Optimization

```sql
-- Use EXPLAIN ANALYZE to understand query performance
EXPLAIN ANALYZE
SELECT o.*, c.name, c.email
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.created_at > NOW() - INTERVAL '7 days';

-- Add indexes based on query patterns
CREATE INDEX CONCURRENTLY idx_orders_created_at ON orders(created_at);
CREATE INDEX CONCURRENTLY idx_orders_customer_created ON orders(customer_id, created_at);
```

### Parameter Tuning

```sql
-- Check current parameters
SHOW shared_buffers;
SHOW effective_cache_size;
SHOW work_mem;

-- Recommended settings for production (via RDS parameter group)
-- shared_buffers: 25% of RAM
-- effective_cache_size: 75% of RAM
-- work_mem: (Total RAM - shared_buffers) / max_connections / 2
-- maintenance_work_mem: 1-2 GB
```

## Monitoring

### Key Metrics

- **CPU Utilization**: Should be < 70%
- **Free Memory**: Should be > 20%
- **Database Connections**: Should be < 80% of max
- **Read/Write IOPS**: Monitor for bottlenecks
- **Replication Lag**: Should be < 1 second

### Alerts

```yaml
# CloudWatch alarms

- DatabaseCPUHigh: CPU > 80% for 10 minutes
- DatabaseMemoryLow: Free memory < 10% for 5 minutes
- DatabaseConnectionsHigh: Connections > 90% for 5 minutes
- ReplicationLagHigh: Lag > 5 seconds for 5 minutes

```

## Troubleshooting

### High CPU Usage

```sql
-- Find CPU-intensive queries
SELECT pid, usename, application_name, state,
       query_start, now() - query_start AS duration,
       query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;

-- Terminate long-running query if needed
SELECT pg_terminate_backend(pid);
```

### Connection Leaks

```sql
-- Find idle connections
SELECT pid, usename, application_name, state,
       state_change, now() - state_change AS idle_duration
FROM pg_stat_activity
WHERE state = 'idle'
  AND now() - state_change > INTERVAL '10 minutes'
ORDER BY idle_duration DESC;

-- Terminate idle connections
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
  AND now() - state_change > INTERVAL '30 minutes';
```

### Deadlocks

```sql
-- Check for deadlocks
SELECT * FROM pg_stat_database WHERE datname = 'ecommerce_production';

-- View blocking queries
SELECT blocked_locks.pid AS blocked_pid,
       blocked_activity.usename AS blocked_user,
       blocking_locks.pid AS blocking_pid,
       blocking_activity.usename AS blocking_user,
       blocked_activity.query AS blocked_statement,
       blocking_activity.query AS blocking_statement
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks ON blocking_locks.locktype = blocked_locks.locktype
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

## Maintenance Windows

### Scheduling

- **Production**: Sunday 02:00-04:00 UTC (low traffic period)
- **Staging**: Any time with notification

### Maintenance Checklist

- [ ] Notify stakeholders
- [ ] Create database snapshot
- [ ] Run VACUUM ANALYZE
- [ ] Reindex if needed
- [ ] Update statistics
- [ ] Check for slow queries
- [ ] Review and optimize
- [ ] Verify backup completion
- [ ] Document changes

## Database Upgrade and Migration Procedures

### Overview

This section covers PostgreSQL version upgrades, including major and minor version upgrades, zero-downtime migration strategies, and rollback procedures.

### PostgreSQL Version Upgrade Strategy

#### Version Types

**Minor Version Upgrades** (e.g., 14.7 → 14.8)
- Bug fixes and security patches
- No breaking changes
- Minimal downtime (automatic during maintenance window)
- Low risk

**Major Version Upgrades** (e.g., 14.x → 15.x)
- New features and improvements
- Potential breaking changes
- Requires careful planning and testing
- Higher risk

#### Upgrade Planning Checklist

- [ ] Review release notes for breaking changes
- [ ] Check extension compatibility
- [ ] Verify application compatibility
- [ ] Test upgrade in non-production environment
- [ ] Plan rollback strategy
- [ ] Schedule maintenance window
- [ ] Notify stakeholders
- [ ] Create pre-upgrade backup
- [ ] Document upgrade procedure

### Minor Version Upgrades (RDS)

Minor version upgrades are straightforward and can be automated.

#### Automatic Minor Version Upgrades

```bash
# Enable automatic minor version upgrades
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --auto-minor-version-upgrade \
  --apply-immediately

# Check current version and available upgrades
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].[EngineVersion,AutoMinorVersionUpgrade]'

# View available minor versions
aws rds describe-db-engine-versions \
  --engine postgres \
  --engine-version 14.7 \
  --query 'DBEngineVersions[0].ValidUpgradeTarget[?IsMajorVersionUpgrade==`false`]'
```

#### Manual Minor Version Upgrade

```bash
# Upgrade to specific minor version
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --engine-version 14.10 \
  --apply-immediately

# Monitor upgrade progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].[DBInstanceStatus,EngineVersion]'

# Verify upgrade completion
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production
```

**Downtime**: 2-5 minutes for minor version upgrades

### Major Version Upgrades

Major version upgrades require more planning and testing.

#### Pre-Upgrade Assessment

```bash
# Check current version
psql -c "SELECT version();"

# List all extensions and versions
psql -c "SELECT extname, extversion FROM pg_extension ORDER BY extname;"

# Check for deprecated features
psql -c "SELECT * FROM pg_settings WHERE name LIKE '%deprecated%';"

# Identify objects that may need attention
psql -c "
SELECT n.nspname, c.relname, c.relkind
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema')
  AND c.relkind IN ('r', 'v', 'm', 'f')
ORDER BY n.nspname, c.relname;
"

# Check for custom data types
psql -c "
SELECT n.nspname, t.typname
FROM pg_type t
JOIN pg_namespace n ON n.oid = t.typnamespace
WHERE n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
  AND t.typtype = 'c'
ORDER BY n.nspname, t.typname;
"
```

#### Compatibility Testing

```bash
# Create test database from production snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-test-upgrade \
  --db-snapshot-identifier ecommerce-production-pre-upgrade \
  --db-instance-class db.r6g.xlarge

# Upgrade test database
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-test-upgrade \
  --engine-version 15.4 \
  --allow-major-version-upgrade \
  --apply-immediately

# Wait for upgrade to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-test-upgrade

# Run compatibility tests
psql -h ecommerce-test-upgrade.xxx.rds.amazonaws.com \
     -U postgres \
     -d ecommerce_production \
     -f compatibility_tests.sql
```

**compatibility_tests.sql**:
```sql
-- Test all extensions load correctly
SELECT extname, extversion FROM pg_extension ORDER BY extname;

-- Test critical queries
\i test_queries.sql

-- Check for any errors in logs
SELECT * FROM pg_stat_database WHERE datname = current_database();

-- Verify all tables accessible
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;

-- Test write operations
BEGIN;
CREATE TEMP TABLE upgrade_test (id SERIAL, data TEXT);
INSERT INTO upgrade_test (data) VALUES ('test');
SELECT * FROM upgrade_test;
ROLLBACK;

-- Check for any deprecated features in use
SELECT * FROM pg_settings WHERE name LIKE '%deprecated%' AND setting != '';
```

#### Application Compatibility Validation

```bash
# Run application test suite against upgraded database
export DATABASE_URL="postgresql://postgres:password@ecommerce-test-upgrade.xxx.rds.amazonaws.com:5432/ecommerce_production"

# Run unit tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run end-to-end tests
./gradlew e2eTest

# Run performance benchmarks
./gradlew performanceTest

# Check application logs for any warnings or errors
grep -i "error\|warning\|deprecated" logs/application.log
```

#### Major Version Upgrade Procedure (RDS)

**Method 1: In-Place Upgrade (Downtime Required)**

```bash
# 1. Create final backup before upgrade
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-production \
  --db-snapshot-identifier ecommerce-production-pre-upgrade-$(date +%Y%m%d-%H%M%S)

# 2. Wait for snapshot to complete
aws rds wait db-snapshot-completed \
  --db-snapshot-identifier ecommerce-production-pre-upgrade-$(date +%Y%m%d-%H%M%S)

# 3. Perform upgrade
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --engine-version 15.4 \
  --allow-major-version-upgrade \
  --apply-immediately

# 4. Monitor upgrade progress
watch -n 30 'aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query "DBInstances[0].[DBInstanceStatus,EngineVersion]"'

# 5. Wait for upgrade to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production

# 6. Verify upgrade
psql -h ecommerce-production.xxx.rds.amazonaws.com \
     -U postgres \
     -c "SELECT version();"

# 7. Run post-upgrade validation
psql -h ecommerce-production.xxx.rds.amazonaws.com \
     -U postgres \
     -d ecommerce_production \
     -f post_upgrade_validation.sql
```

**Downtime**: 15-60 minutes depending on database size

**Method 2: Blue-Green Deployment (Minimal Downtime)**

```bash
# 1. Create read replica with new version
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-production-v15 \
  --source-db-instance-identifier ecommerce-production \
  --db-instance-class db.r6g.2xlarge \
  --engine-version 15.4

# 2. Wait for replica to catch up
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production-v15

# 3. Monitor replication lag
watch -n 10 'aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production-v15 \
  --start-time $(date -u -d "5 minutes ago" +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Average'

# 4. When lag is minimal, promote replica
aws rds promote-read-replica \
  --db-instance-identifier ecommerce-production-v15

# 5. Update application connection string
# (Use DNS CNAME or update configuration)

# 6. Verify application connectivity
curl -f https://api.example.com/health

# 7. Keep old instance as backup for 24-48 hours
# Then delete if no issues
```

**Downtime**: 1-5 minutes (DNS propagation + connection pool refresh)

### Zero-Downtime Upgrades with Logical Replication

Logical replication allows zero-downtime major version upgrades.

#### Logical Replication Setup

```sql
-- On source database (old version)
-- 1. Enable logical replication
ALTER SYSTEM SET wal_level = 'logical';
ALTER SYSTEM SET max_replication_slots = 10;
ALTER SYSTEM SET max_wal_senders = 10;

-- Restart required (brief downtime)
-- Via RDS: Reboot instance

-- 2. Create publication for all tables
CREATE PUBLICATION upgrade_pub FOR ALL TABLES;

-- Or for specific tables
CREATE PUBLICATION upgrade_pub FOR TABLE 
  customers, orders, products, order_items;

-- 3. Verify publication
SELECT * FROM pg_publication;
SELECT * FROM pg_publication_tables;
```

```bash
# On target database (new version)
# 1. Create new RDS instance with new PostgreSQL version
aws rds create-db-instance \
  --db-instance-identifier ecommerce-production-v15 \
  --db-instance-class db.r6g.2xlarge \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password "${DB_PASSWORD}" \
  --allocated-storage 500 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name production-subnet-group

# 2. Restore schema to new instance (without data)
pg_dump -h old-instance.rds.amazonaws.com \
        -U postgres \
        -d ecommerce_production \
        --schema-only \
        --no-owner \
        --no-privileges \
        -f schema.sql

psql -h new-instance.rds.amazonaws.com \
     -U postgres \
     -d ecommerce_production \
     -f schema.sql
```

```sql
-- On target database
-- 3. Create subscription
CREATE SUBSCRIPTION upgrade_sub
CONNECTION 'host=old-instance.rds.amazonaws.com port=5432 dbname=ecommerce_production user=postgres password=xxx'
PUBLICATION upgrade_pub;

-- 4. Monitor replication status
SELECT * FROM pg_stat_subscription;

-- Check replication lag
SELECT 
  subname,
  received_lsn,
  latest_end_lsn,
  latest_end_time,
  now() - latest_end_time AS replication_lag
FROM pg_stat_subscription;

-- Monitor table sync status
SELECT * FROM pg_subscription_rel;
```

#### Cutover Procedure

```sql
-- 1. Stop application writes (maintenance mode)
-- Update application configuration or use feature flag

-- 2. Wait for replication to catch up (lag = 0)
SELECT 
  subname,
  now() - latest_end_time AS replication_lag
FROM pg_stat_subscription;

-- 3. Verify data consistency
-- On source
SELECT count(*) FROM orders;
SELECT max(created_at) FROM orders;

-- On target
SELECT count(*) FROM orders;
SELECT max(created_at) FROM orders;

-- 4. Disable subscription
ALTER SUBSCRIPTION upgrade_sub DISABLE;

-- 5. Drop subscription (after verification)
DROP SUBSCRIPTION upgrade_sub;

-- 6. Update application to point to new database
-- Update DNS CNAME or configuration

-- 7. Enable application writes

-- 8. Verify application functionality
-- Run smoke tests

-- 9. Monitor for issues
-- Check application logs and metrics
```

**Downtime**: < 1 minute (application restart + connection pool refresh)

### pg_upgrade for Self-Managed PostgreSQL

For self-managed PostgreSQL (not RDS), use pg_upgrade for in-place upgrades.

#### pg_upgrade Prerequisites

```bash
# 1. Install new PostgreSQL version alongside old version
sudo apt-get install postgresql-15 postgresql-contrib-15

# 2. Stop PostgreSQL service
sudo systemctl stop postgresql

# 3. Create backup
sudo -u postgres pg_dumpall > /backup/full_backup_$(date +%Y%m%d).sql

# 4. Verify old and new versions
/usr/lib/postgresql/14/bin/postgres --version
/usr/lib/postgresql/15/bin/postgres --version
```

#### pg_upgrade Execution

```bash
# 1. Check compatibility (dry run)
sudo -u postgres /usr/lib/postgresql/15/bin/pg_upgrade \
  --old-datadir=/var/lib/postgresql/14/main \
  --new-datadir=/var/lib/postgresql/15/main \
  --old-bindir=/usr/lib/postgresql/14/bin \
  --new-bindir=/usr/lib/postgresql/15/bin \
  --check

# 2. Review check output for any issues
cat pg_upgrade_output.d/*.txt

# 3. Perform actual upgrade
sudo -u postgres /usr/lib/postgresql/15/bin/pg_upgrade \
  --old-datadir=/var/lib/postgresql/14/main \
  --new-datadir=/var/lib/postgresql/15/main \
  --old-bindir=/usr/lib/postgresql/14/bin \
  --new-bindir=/usr/lib/postgresql/15/bin \
  --link  # Use hard links for faster upgrade

# 4. Start new version
sudo systemctl start postgresql@15-main

# 5. Run analyze script
sudo -u postgres ./analyze_new_cluster.sh

# 6. Update statistics
sudo -u postgres psql -c "ANALYZE VERBOSE;"

# 7. Verify upgrade
sudo -u postgres psql -c "SELECT version();"
```

#### pg_upgrade Best Practices

**Use --link option**:
- Faster upgrade (uses hard links instead of copying)
- Requires old and new data directories on same filesystem
- Cannot rollback easily (old cluster is modified)

**Use --clone option** (PostgreSQL 13+):
- Fast like --link but safer
- Uses filesystem cloning (reflinks)
- Requires filesystem support (XFS, Btrfs)

**Without --link or --clone**:
- Slower (copies all data)
- Safer (old cluster unchanged)
- Easier rollback

```bash
# Recommended for production: Copy mode with backup
sudo -u postgres /usr/lib/postgresql/15/bin/pg_upgrade \
  --old-datadir=/var/lib/postgresql/14/main \
  --new-datadir=/var/lib/postgresql/15/main \
  --old-bindir=/usr/lib/postgresql/14/bin \
  --new-bindir=/usr/lib/postgresql/15/bin \
  --jobs=4  # Parallel processing for faster upgrade
```

### Extension Upgrade Procedures

Extensions must be upgraded separately from PostgreSQL version upgrades.

#### Check Extension Versions

```sql
-- List installed extensions
SELECT extname, extversion, 
       (SELECT extversion FROM pg_available_extension_versions 
        WHERE name = extname AND installed = false 
        ORDER BY version DESC LIMIT 1) AS latest_version
FROM pg_extension
WHERE extname NOT IN ('plpgsql')
ORDER BY extname;

-- Check for available updates
SELECT name, installed_version, default_version, comment
FROM pg_available_extensions
WHERE installed_version IS NOT NULL
  AND installed_version != default_version
ORDER BY name;
```

#### Upgrade Extensions

```sql
-- Upgrade single extension
ALTER EXTENSION pg_stat_statements UPDATE;

-- Upgrade to specific version
ALTER EXTENSION pg_stat_statements UPDATE TO '1.10';

-- Upgrade all extensions
DO $$
DECLARE
    ext RECORD;
BEGIN
    FOR ext IN 
        SELECT extname 
        FROM pg_extension 
        WHERE extname NOT IN ('plpgsql')
    LOOP
        EXECUTE format('ALTER EXTENSION %I UPDATE', ext.extname);
        RAISE NOTICE 'Updated extension: %', ext.extname;
    END LOOP;
END $$;

-- Verify upgrades
SELECT extname, extversion FROM pg_extension ORDER BY extname;
```

#### Extension-Specific Considerations

**pg_stat_statements**:
```sql
-- May require parameter changes
ALTER SYSTEM SET pg_stat_statements.track = 'all';
ALTER SYSTEM SET pg_stat_statements.max = 10000;

-- Restart required
-- Via RDS: Reboot instance

-- Verify
SELECT * FROM pg_stat_statements LIMIT 1;
```

**PostGIS**:
```sql
-- Check PostGIS version
SELECT PostGIS_Version();

-- Upgrade PostGIS (requires specific procedure)
SELECT postgis_extensions_upgrade();

-- Or manual upgrade
ALTER EXTENSION postgis UPDATE TO '3.3.3';
ALTER EXTENSION postgis_topology UPDATE TO '3.3.3';

-- Update spatial reference systems
SELECT UpdateGeometrySRID('public', 'spatial_table', 'geom', 4326);
```

**pgcrypto**:
```sql
-- Upgrade pgcrypto
ALTER EXTENSION pgcrypto UPDATE;

-- Test encryption functions
SELECT pgp_sym_encrypt('test', 'password');
```

### Rollback Procedures

#### Rollback Strategy Decision Tree

```
Upgrade Failed?
├─ Yes
│  ├─ Before Cutover?
│  │  ├─ RDS: Restore from snapshot
│  │  └─ Self-managed: Start old cluster
│  └─ After Cutover?
│     ├─ < 1 hour: Restore from snapshot
│     ├─ 1-24 hours: Restore + replay WAL
│     └─ > 24 hours: Logical replication back
└─ No: Continue monitoring
```

#### RDS Rollback from Snapshot

```bash
# 1. Identify pre-upgrade snapshot
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-production \
  --query 'DBSnapshots[?SnapshotCreateTime>=`2024-01-01`].[DBSnapshotIdentifier,SnapshotCreateTime]' \
  --output table

# 2. Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-production-rollback \
  --db-snapshot-identifier ecommerce-production-pre-upgrade-20240115

# 3. Wait for restore to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production-rollback

# 4. Update application connection string
# Point to rollback instance

# 5. Verify application functionality

# 6. Delete failed upgrade instance
aws rds delete-db-instance \
  --db-instance-identifier ecommerce-production \
  --skip-final-snapshot

# 7. Rename rollback instance to production
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production-rollback \
  --new-db-instance-identifier ecommerce-production \
  --apply-immediately
```

**Data Loss**: Any data written after snapshot creation is lost

#### pg_upgrade Rollback

```bash
# If upgrade failed before completion
# 1. Stop new cluster
sudo systemctl stop postgresql@15-main

# 2. Start old cluster
sudo systemctl start postgresql@14-main

# 3. Verify old cluster
sudo -u postgres psql -c "SELECT version();"

# 4. Update application connection

# If upgrade completed but issues found
# 1. Restore from backup
sudo -u postgres psql < /backup/full_backup_20240115.sql

# 2. Or restore from filesystem backup
sudo systemctl stop postgresql@15-main
sudo rm -rf /var/lib/postgresql/15/main
sudo cp -a /backup/postgresql-14-data /var/lib/postgresql/14/main
sudo systemctl start postgresql@14-main
```

#### Logical Replication Rollback

If issues found after cutover, replicate back to old version:

```sql
-- On new database (now source)
CREATE PUBLICATION rollback_pub FOR ALL TABLES;

-- On old database (now target)
CREATE SUBSCRIPTION rollback_sub
CONNECTION 'host=new-instance.rds.amazonaws.com port=5432 dbname=ecommerce_production user=postgres password=xxx'
PUBLICATION rollback_pub;

-- Wait for sync
SELECT * FROM pg_stat_subscription;

-- Cutover back to old version
-- (Follow cutover procedure in reverse)
```

### Upgrade Risk Assessment

#### Risk Matrix

| Risk Factor | Low Risk | Medium Risk | High Risk |
|-------------|----------|-------------|-----------|
| **Version Jump** | Minor version | 1 major version | 2+ major versions |
| **Database Size** | < 100 GB | 100 GB - 1 TB | > 1 TB |
| **Downtime Tolerance** | > 1 hour | 15-60 minutes | < 15 minutes |
| **Extension Count** | 0-2 | 3-5 | 6+ |
| **Custom Code** | None | Minimal | Extensive |
| **Testing Coverage** | Comprehensive | Moderate | Limited |

#### Risk Mitigation Strategies

**High Risk Upgrades**:
- Use logical replication for zero-downtime
- Extensive testing in staging environment
- Gradual rollout (test → staging → production)
- Have rollback plan ready
- Schedule during lowest traffic period
- Have DBA on standby

**Medium Risk Upgrades**:
- Use blue-green deployment
- Test in staging environment
- Have rollback plan ready
- Schedule during maintenance window

**Low Risk Upgrades**:
- In-place upgrade acceptable
- Basic testing in staging
- Standard maintenance window

#### Pre-Upgrade Checklist

```bash
# Create comprehensive checklist
cat > upgrade_checklist.md << 'EOF'
# PostgreSQL Upgrade Checklist

## Pre-Upgrade (1 week before)
- [ ] Review release notes for breaking changes
- [ ] Check extension compatibility
- [ ] Test upgrade in staging environment
- [ ] Run application test suite against upgraded database
- [ ] Document current performance baselines
- [ ] Create upgrade runbook
- [ ] Schedule maintenance window
- [ ] Notify stakeholders

## Pre-Upgrade (1 day before)
- [ ] Create fresh backup
- [ ] Verify backup integrity
- [ ] Test restore procedure
- [ ] Prepare rollback plan
- [ ] Review monitoring dashboards
- [ ] Prepare communication templates

## During Upgrade
- [ ] Enable maintenance mode
- [ ] Create final backup
- [ ] Execute upgrade procedure
- [ ] Monitor upgrade progress
- [ ] Run post-upgrade validation
- [ ] Verify application connectivity
- [ ] Check for errors in logs
- [ ] Run smoke tests

## Post-Upgrade (immediate)
- [ ] Disable maintenance mode
- [ ] Monitor application metrics
- [ ] Monitor database metrics
- [ ] Check for slow queries
- [ ] Verify replication (if applicable)
- [ ] Run ANALYZE on all tables
- [ ] Update documentation

## Post-Upgrade (24 hours)
- [ ] Review application logs
- [ ] Review database logs
- [ ] Compare performance metrics
- [ ] Check for any anomalies
- [ ] Gather user feedback
- [ ] Document lessons learned

## Post-Upgrade (1 week)
- [ ] Delete old snapshots (keep pre-upgrade)
- [ ] Update monitoring baselines
- [ ] Schedule next upgrade
- [ ] Update upgrade procedures
EOF
```

### Post-Upgrade Validation

```sql
-- Post-upgrade validation script
-- Save as post_upgrade_validation.sql

-- 1. Verify version
SELECT version();

-- 2. Check all extensions loaded
SELECT extname, extversion FROM pg_extension ORDER BY extname;

-- 3. Verify all tables accessible
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_stat_user_tables
ORDER BY schemaname, tablename;

-- 4. Check for any errors
SELECT * FROM pg_stat_database WHERE datname = current_database();

-- 5. Verify indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
ORDER BY schemaname, tablename, indexname;

-- 6. Test write operations
BEGIN;
CREATE TEMP TABLE upgrade_validation (
    id SERIAL PRIMARY KEY,
    data TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
INSERT INTO upgrade_validation (data) VALUES ('test1'), ('test2'), ('test3');
SELECT * FROM upgrade_validation;
ROLLBACK;

-- 7. Check for any deprecated features
SELECT name, setting, source
FROM pg_settings
WHERE name LIKE '%deprecated%' AND setting != '';

-- 8. Verify replication (if applicable)
SELECT * FROM pg_stat_replication;

-- 9. Check for any blocking queries
SELECT pid, usename, state, query_start, query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY query_start;

-- 10. Verify statistics are up to date
SELECT schemaname, tablename, last_analyze, last_autoanalyze
FROM pg_stat_user_tables
ORDER BY last_analyze NULLS FIRST
LIMIT 20;

-- Success message
SELECT 'Post-upgrade validation completed successfully!' AS status;
```

### Upgrade Documentation Template

```markdown
# PostgreSQL Upgrade: [Version X] to [Version Y]

## Upgrade Details
- **Date**: YYYY-MM-DD
- **Start Time**: HH:MM UTC
- **End Time**: HH:MM UTC
- **Duration**: X minutes
- **Performed By**: [Name]
- **Method**: [In-place / Blue-Green / Logical Replication]

## Pre-Upgrade State
- **PostgreSQL Version**: X.Y.Z
- **Database Size**: XXX GB
- **Table Count**: XXX
- **Extension Count**: XX
- **Snapshot ID**: snap-xxxxx

## Upgrade Procedure
1. [Step 1]
2. [Step 2]
...

## Issues Encountered
- [Issue 1]: [Resolution]
- [Issue 2]: [Resolution]

## Post-Upgrade Validation
- [ ] Version verified
- [ ] Extensions verified
- [ ] Application connectivity verified
- [ ] Performance metrics normal
- [ ] No errors in logs

## Performance Comparison
| Metric | Before | After | Change |
|--------|--------|-------|--------|
| Avg Query Time | X ms | Y ms | +/- Z% |
| Connection Count | X | Y | +/- Z |
| CPU Usage | X% | Y% | +/- Z% |

## Lessons Learned
- [Lesson 1]
- [Lesson 2]

## Recommendations
- [Recommendation 1]
- [Recommendation 2]
```


**Last Updated**: 2025-11-08  
**Owner**: Database Team  
**Review Cycle**: Quarterly

## Capacity Planning and Scaling

### Overview

This section covers database capacity planning, growth analysis, and scaling strategies to ensure the database can handle current and future workloads efficiently.

### Database Growth Analysis

#### Historical Growth Tracking

```sql
-- Create growth tracking table (if not exists)
CREATE TABLE IF NOT EXISTS database_growth_metrics (
    recorded_at TIMESTAMP DEFAULT NOW(),
    database_name TEXT,
    total_size_bytes BIGINT,
    table_count INTEGER,
    index_count INTEGER,
    connection_count INTEGER,
    transaction_rate NUMERIC,
    query_rate NUMERIC
);

-- Record daily metrics (schedule via cron or AWS Systems Manager)
INSERT INTO database_growth_metrics (
    database_name, total_size_bytes, table_count, index_count,
    connection_count, transaction_rate, query_rate
)
SELECT 
    current_database(),
    pg_database_size(current_database()),
    (SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema')),
    (SELECT count(*) FROM pg_indexes WHERE schemaname NOT IN ('pg_catalog', 'information_schema')),
    (SELECT count(*) FROM pg_stat_activity WHERE state != 'idle'),
    (SELECT sum(xact_commit + xact_rollback) FROM pg_stat_database WHERE datname = current_database()),
    (SELECT sum(tup_inserted + tup_updated + tup_deleted) FROM pg_stat_database WHERE datname = current_database());

-- View growth trends
SELECT 
    recorded_at::date AS date,
    pg_size_pretty(total_size_bytes) AS database_size,
    pg_size_pretty(total_size_bytes - LAG(total_size_bytes) OVER (ORDER BY recorded_at)) AS daily_growth,
    table_count,
    index_count,
    connection_count
FROM database_growth_metrics
WHERE recorded_at > NOW() - INTERVAL '90 days'
ORDER BY recorded_at DESC;

-- Calculate growth rates
WITH daily_metrics AS (
    SELECT 
        recorded_at::date AS date,
        total_size_bytes,
        total_size_bytes - LAG(total_size_bytes) OVER (ORDER BY recorded_at::date) AS daily_growth
    FROM database_growth_metrics
    WHERE recorded_at > NOW() - INTERVAL '90 days'
)
SELECT 
    pg_size_pretty(AVG(daily_growth)) AS avg_daily_growth,
    pg_size_pretty(AVG(daily_growth) * 7) AS avg_weekly_growth,
    pg_size_pretty(AVG(daily_growth) * 30) AS avg_monthly_growth,
    pg_size_pretty(AVG(daily_growth) * 365) AS projected_yearly_growth,
    ROUND(AVG(daily_growth) * 365.0 / NULLIF(MIN(total_size_bytes), 0) * 100, 2) AS yearly_growth_percent
FROM daily_metrics
WHERE daily_growth > 0;
```

#### Table-Level Growth Analysis

```sql
-- Track individual table growth
CREATE TABLE IF NOT EXISTS table_growth_metrics (
    recorded_at TIMESTAMP DEFAULT NOW(),
    schema_name TEXT,
    table_name TEXT,
    total_size_bytes BIGINT,
    table_size_bytes BIGINT,
    index_size_bytes BIGINT,
    row_count BIGINT,
    dead_tuples BIGINT
);

-- Record table metrics
INSERT INTO table_growth_metrics (
    schema_name, table_name, total_size_bytes, table_size_bytes,
    index_size_bytes, row_count, dead_tuples
)
SELECT 
    schemaname,
    tablename,
    pg_total_relation_size(schemaname||'.'||tablename),
    pg_relation_size(schemaname||'.'||tablename),
    pg_indexes_size(schemaname||'.'||tablename),
    n_live_tup,
    n_dead_tup
FROM pg_stat_user_tables;

-- Analyze table growth trends
SELECT 
    schema_name,
    table_name,
    pg_size_pretty(MAX(total_size_bytes)) AS current_size,
    pg_size_pretty(MAX(total_size_bytes) - MIN(total_size_bytes)) AS growth_90_days,
    ROUND((MAX(total_size_bytes) - MIN(total_size_bytes))::numeric / 
          NULLIF(MIN(total_size_bytes), 0) * 100, 2) AS growth_percent,
    pg_size_pretty((MAX(total_size_bytes) - MIN(total_size_bytes)) / 90.0 * 365) AS projected_yearly_growth
FROM table_growth_metrics
WHERE recorded_at > NOW() - INTERVAL '90 days'
GROUP BY schema_name, table_name
HAVING MAX(total_size_bytes) > 104857600  -- > 100MB
ORDER BY (MAX(total_size_bytes) - MIN(total_size_bytes)) DESC
LIMIT 20;
```

### Capacity Forecasting

#### Storage Capacity Forecasting

```sql
-- Forecast when storage will reach capacity
WITH growth_analysis AS (
    SELECT 
        AVG(total_size_bytes - LAG(total_size_bytes) OVER (ORDER BY recorded_at)) AS daily_growth_bytes
    FROM database_growth_metrics
    WHERE recorded_at > NOW() - INTERVAL '90 days'
      AND total_size_bytes > LAG(total_size_bytes) OVER (ORDER BY recorded_at)
),
current_state AS (
    SELECT 
        pg_database_size(current_database()) AS current_size_bytes,
        -- Get allocated storage from RDS (example: 500GB)
        500 * 1024 * 1024 * 1024::bigint AS allocated_storage_bytes
),
forecast AS (
    SELECT 
        cs.current_size_bytes,
        cs.allocated_storage_bytes,
        ga.daily_growth_bytes,
        cs.allocated_storage_bytes - cs.current_size_bytes AS remaining_bytes,
        CASE 
            WHEN ga.daily_growth_bytes > 0 THEN
                (cs.allocated_storage_bytes - cs.current_size_bytes) / ga.daily_growth_bytes
            ELSE NULL
        END AS days_until_full
    FROM current_state cs, growth_analysis ga
)
SELECT 
    pg_size_pretty(current_size_bytes) AS current_size,
    pg_size_pretty(allocated_storage_bytes) AS allocated_storage,
    pg_size_pretty(remaining_bytes) AS remaining_space,
    ROUND(current_size_bytes::numeric / allocated_storage_bytes * 100, 2) AS usage_percent,
    pg_size_pretty(daily_growth_bytes) AS avg_daily_growth,
    ROUND(days_until_full) AS days_until_full,
    (NOW() + (days_until_full || ' days')::INTERVAL)::date AS estimated_full_date,
    CASE 
        WHEN days_until_full < 30 THEN 'CRITICAL - Scale immediately'
        WHEN days_until_full < 90 THEN 'WARNING - Plan scaling soon'
        WHEN days_until_full < 180 THEN 'NOTICE - Monitor closely'
        ELSE 'OK - Capacity sufficient'
    END AS capacity_status
FROM forecast;
```

#### Connection Capacity Forecasting

```sql
-- Analyze connection usage trends
WITH connection_metrics AS (
    SELECT 
        recorded_at,
        connection_count,
        LAG(connection_count) OVER (ORDER BY recorded_at) AS prev_connection_count
    FROM database_growth_metrics
    WHERE recorded_at > NOW() - INTERVAL '90 days'
)
SELECT 
    MAX(connection_count) AS peak_connections,
    ROUND(AVG(connection_count), 0) AS avg_connections,
    ROUND(STDDEV(connection_count), 0) AS stddev_connections,
    ROUND(AVG(connection_count) + 2 * STDDEV(connection_count), 0) AS recommended_max_connections,
    -- Get current max_connections setting
    (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') AS current_max_connections,
    CASE 
        WHEN MAX(connection_count) > (SELECT setting::int * 0.8 FROM pg_settings WHERE name = 'max_connections') 
        THEN 'WARNING - Near capacity'
        WHEN MAX(connection_count) > (SELECT setting::int * 0.6 FROM pg_settings WHERE name = 'max_connections')
        THEN 'NOTICE - Monitor closely'
        ELSE 'OK - Capacity sufficient'
    END AS connection_capacity_status
FROM connection_metrics;

-- Identify connection patterns by time of day
SELECT 
    EXTRACT(HOUR FROM recorded_at) AS hour_of_day,
    ROUND(AVG(connection_count), 0) AS avg_connections,
    MAX(connection_count) AS peak_connections
FROM database_growth_metrics
WHERE recorded_at > NOW() - INTERVAL '30 days'
GROUP BY EXTRACT(HOUR FROM recorded_at)
ORDER BY hour_of_day;
```

#### IOPS and Throughput Forecasting

```bash
# Monitor IOPS usage via CloudWatch
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReadIOPS \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production \
  --start-time $(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average,Maximum \
  --query 'Datapoints[*].[Timestamp,Average,Maximum]' \
  --output table

# Monitor Write IOPS
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name WriteIOPS \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production \
  --start-time $(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average,Maximum \
  --query 'Datapoints[*].[Timestamp,Average,Maximum]' \
  --output table

# Check current IOPS allocation
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].[Iops,StorageType,AllocatedStorage]' \
  --output table
```

### Read Replica Scaling

#### When to Add Read Replicas

**Indicators**:
- Read query latency increasing (> 100ms p95)
- Primary instance CPU > 70% consistently
- Read-heavy workload (read:write ratio > 3:1)
- Geographic distribution requirements
- Reporting queries impacting production performance

#### Creating Read Replicas

```bash
# Create read replica in same region
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-production-read-1 \
  --source-db-instance-identifier ecommerce-production \
  --db-instance-class db.r6g.xlarge \
  --availability-zone us-east-1b \
  --publicly-accessible false \
  --tags Key=Environment,Value=production Key=Role,Value=read-replica

# Create read replica in different region (cross-region)
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-production-read-tokyo \
  --source-db-instance-identifier arn:aws:rds:us-east-1:123456789012:db:ecommerce-production \
  --db-instance-class db.r6g.xlarge \
  --region ap-northeast-1 \
  --publicly-accessible false

# Monitor replica creation progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production-read-1 \
  --query 'DBInstances[0].[DBInstanceStatus,ReadReplicaSourceDBInstanceIdentifier]'

# Wait for replica to be available
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production-read-1
```

#### Read Replica Configuration

```bash
# Configure read replica with different instance class
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production-read-1 \
  --db-instance-class db.r6g.2xlarge \
  --apply-immediately

# Enable automated backups on read replica
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production-read-1 \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --apply-immediately

# Monitor replication lag
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production-read-1 \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum
```

```sql
-- Check replication status from primary
SELECT client_addr, state, sync_state, 
       pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) AS lag_bytes,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)) AS lag_size
FROM pg_stat_replication;

-- Check replication lag from replica
SELECT now() - pg_last_xact_replay_timestamp() AS replication_lag;
```

#### Application Configuration for Read Replicas

```yaml
# Spring Boot configuration for read replicas
spring:
  datasource:
    primary:
      jdbc-url: jdbc:postgresql://ecommerce-production.xxx.rds.amazonaws.com:5432/ecommerce_production
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 20
        
    replica:
      jdbc-url: jdbc:postgresql://ecommerce-production-read-1.xxx.rds.amazonaws.com:5432/ecommerce_production
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
      hikari:
        maximum-pool-size: 30
        read-only: true
```

```java
// Read replica routing configuration
@Configuration
public class DatabaseRoutingConfiguration {
    
    @Bean
    @Primary
    public DataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource) {
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DatabaseType.PRIMARY, primaryDataSource);
        targetDataSources.put(DatabaseType.REPLICA, replicaDataSource);
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        
        return routingDataSource;
    }
}

// Use @Transactional(readOnly = true) for read operations
@Transactional(readOnly = true)
public List<Order> findRecentOrders(String customerId) {
    // This will route to read replica
    return orderRepository.findByCustomerId(customerId);
}
```

#### Read Replica Promotion

```bash
# Promote read replica to standalone instance (for DR or migration)
aws rds promote-read-replica \
  --db-instance-identifier ecommerce-production-read-1 \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00"

# Monitor promotion progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production-read-1 \
  --query 'DBInstances[0].[DBInstanceStatus,ReadReplicaSourceDBInstanceIdentifier]'

# After promotion, update application connection string
# The promoted replica becomes a standalone primary instance
```

**Use Cases for Promotion**:
- Disaster recovery (primary failure)
- Regional migration
- Blue-green deployment
- Testing major changes

### Vertical Scaling (Instance Type Upgrade)

#### When to Scale Vertically

**Indicators**:
- CPU utilization consistently > 70%
- Memory pressure (high swap usage)
- IOPS consistently at provisioned limit
- Query performance degradation despite optimization
- Connection pool exhaustion

#### Instance Type Selection

```bash
# List available instance classes
aws rds describe-orderable-db-instance-options \
  --engine postgres \
  --engine-version 15.4 \
  --query 'OrderableDBInstanceOptions[*].[DBInstanceClass,StorageType]' \
  --output table | sort | uniq
```

**Instance Class Comparison**:

| Instance Class | vCPU | Memory | Network | Use Case |
|----------------|------|--------|---------|----------|
| db.t3.medium | 2 | 4 GB | Low | Development/Testing |
| db.t3.large | 2 | 8 GB | Moderate | Small production |
| db.r6g.xlarge | 4 | 32 GB | Up to 10 Gbps | Medium production |
| db.r6g.2xlarge | 8 | 64 GB | Up to 10 Gbps | Large production |
| db.r6g.4xlarge | 16 | 128 GB | Up to 10 Gbps | High-performance |
| db.r6g.8xlarge | 32 | 256 GB | 12 Gbps | Very large databases |
| db.r6g.16xlarge | 64 | 512 GB | 25 Gbps | Enterprise scale |

**Graviton2 (r6g) vs Intel (r5)**:
- r6g: Better price/performance (up to 40% better)
- r6g: ARM-based (ensure application compatibility)
- r5: x86-based (broader compatibility)

#### Vertical Scaling Procedure

```bash
# 1. Create snapshot before scaling
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-production \
  --db-snapshot-identifier ecommerce-production-pre-scale-$(date +%Y%m%d-%H%M%S)

# 2. Modify instance class
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --db-instance-class db.r6g.2xlarge \
  --apply-immediately

# 3. Monitor scaling progress
watch -n 30 'aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query "DBInstances[0].[DBInstanceStatus,DBInstanceClass]"'

# 4. Wait for modification to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production
```

**Downtime**: 5-15 minutes (instance restart required)

#### Zero-Downtime Vertical Scaling

```bash
# Use blue-green deployment for zero-downtime scaling
# 1. Create read replica with larger instance class
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-production-scaled \
  --source-db-instance-identifier ecommerce-production \
  --db-instance-class db.r6g.2xlarge

# 2. Wait for replica to catch up
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production-scaled

# Monitor replication lag
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production-scaled \
  --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60 \
  --statistics Average

# 3. When lag is minimal, promote replica
aws rds promote-read-replica \
  --db-instance-identifier ecommerce-production-scaled

# 4. Update application connection string (DNS CNAME or configuration)
# 5. Verify application connectivity
# 6. Delete old instance after verification period
```

**Downtime**: < 1 minute (connection pool refresh)

### Storage Scaling and IOPS Optimization

#### Storage Types Comparison

| Storage Type | IOPS | Throughput | Use Case | Cost |
|--------------|------|------------|----------|------|
| gp2 (General Purpose SSD) | 3 IOPS/GB (min 100, max 16,000) | 250 MB/s | General workloads | $ |
| gp3 (General Purpose SSD) | 3,000 baseline, up to 16,000 | 125 MB/s baseline, up to 1,000 MB/s | Most workloads | $ (20% cheaper than gp2) |
| io1 (Provisioned IOPS) | Up to 64,000 | Up to 1,000 MB/s | I/O intensive | $$$ |
| io2 (Provisioned IOPS) | Up to 64,000 | Up to 1,000 MB/s | Mission critical | $$$$ |

#### Storage Scaling Procedure

```bash
# Check current storage configuration
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].[AllocatedStorage,StorageType,Iops,MaxAllocatedStorage]'

# Increase storage size (online, no downtime)
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --allocated-storage 1000 \
  --apply-immediately

# Enable storage autoscaling
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --max-allocated-storage 2000 \
  --apply-immediately

# Upgrade storage type from gp2 to gp3
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --storage-type gp3 \
  --allocated-storage 500 \
  --iops 12000 \
  --storage-throughput 500 \
  --apply-immediately

# Monitor storage modification
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].[DBInstanceStatus,AllocatedStorage,StorageType]'
```

**Notes**:
- Storage scaling is online (no downtime)
- Can only increase storage, not decrease
- Storage autoscaling prevents running out of space
- gp2 → gp3 migration requires brief downtime

#### IOPS Optimization

```bash
# Monitor current IOPS usage
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReadIOPS \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production \
  --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average,Maximum
```

```sql
-- Identify I/O intensive queries
SELECT query, calls, 
       total_time, mean_time,
       shared_blks_read, shared_blks_written,
       local_blks_read, local_blks_written,
       temp_blks_read, temp_blks_written
FROM pg_stat_statements
WHERE shared_blks_read + shared_blks_written > 1000
ORDER BY shared_blks_read + shared_blks_written DESC
LIMIT 20;

-- Check buffer cache hit ratio (should be > 99%)
SELECT 
    sum(heap_blks_read) AS heap_read,
    sum(heap_blks_hit) AS heap_hit,
    ROUND(sum(heap_blks_hit) / NULLIF(sum(heap_blks_hit) + sum(heap_blks_read), 0) * 100, 2) AS cache_hit_ratio
FROM pg_statio_user_tables;

-- Identify tables with high I/O
SELECT schemaname, tablename,
       heap_blks_read, heap_blks_hit,
       idx_blks_read, idx_blks_hit,
       ROUND(heap_blks_hit::numeric / NULLIF(heap_blks_hit + heap_blks_read, 0) * 100, 2) AS table_hit_ratio,
       ROUND(idx_blks_hit::numeric / NULLIF(idx_blks_hit + idx_blks_read, 0) * 100, 2) AS index_hit_ratio
FROM pg_statio_user_tables
WHERE heap_blks_read + idx_blks_read > 0
ORDER BY heap_blks_read + idx_blks_read DESC
LIMIT 20;
```

**IOPS Optimization Strategies**:
1. Increase shared_buffers (more data in memory)
2. Add appropriate indexes to reduce sequential scans
3. Optimize queries to reduce I/O operations
4. Use connection pooling to reduce connection overhead
5. Upgrade to gp3 or io1/io2 for higher IOPS
6. Consider read replicas to distribute read load

### Connection Scaling and Pooling Strategies

#### Connection Pool Sizing

**Formula**: `max_connections = (available_memory - shared_buffers) / work_mem / expected_concurrent_queries`

**Recommended Settings**:
```yaml
# For db.r6g.xlarge (32 GB RAM)
max_connections: 200
shared_buffers: 8GB  # 25% of RAM
work_mem: 64MB
maintenance_work_mem: 2GB

# For db.r6g.2xlarge (64 GB RAM)
max_connections: 400
shared_buffers: 16GB
work_mem: 64MB
maintenance_work_mem: 4GB
```

#### Application Connection Pool Configuration

```yaml
# HikariCP configuration (per application instance)
spring:
  datasource:
    hikari:
      maximum-pool-size: 20  # Per app instance
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
      
# Calculate total connections
# Total = (number_of_app_instances * maximum-pool-size) + buffer
# Example: 10 app instances * 20 connections = 200 connections
# Set max_connections = 250 (with 50 connection buffer)
```

#### PgBouncer for Connection Pooling

```bash
# Install PgBouncer on separate EC2 instance
sudo apt-get update
sudo apt-get install pgbouncer

# Configure PgBouncer
sudo nano /etc/pgbouncer/pgbouncer.ini
```

```ini
[databases]
ecommerce_production = host=ecommerce-production.xxx.rds.amazonaws.com port=5432 dbname=ecommerce_production

[pgbouncer]
listen_addr = 0.0.0.0
listen_port = 6432
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 25
reserve_pool_size = 5
reserve_pool_timeout = 3
max_db_connections = 100
max_user_connections = 100
server_idle_timeout = 600
server_lifetime = 3600
server_connect_timeout = 15
query_timeout = 0
```

```bash
# Start PgBouncer
sudo systemctl start pgbouncer
sudo systemctl enable pgbouncer

# Monitor PgBouncer
psql -h localhost -p 6432 -U pgbouncer pgbouncer -c "SHOW POOLS;"
psql -h localhost -p 6432 -U pgbouncer pgbouncer -c "SHOW CLIENTS;"
psql -h localhost -p 6432 -U pgbouncer pgbouncer -c "SHOW SERVERS;"

# Update application to connect via PgBouncer
# jdbc:postgresql://pgbouncer-host:6432/ecommerce_production
```

**PgBouncer Benefits**:
- Reduces database connections (1000 app connections → 100 DB connections)
- Faster connection establishment
- Better resource utilization
- Supports connection pooling modes: session, transaction, statement

#### Connection Monitoring

```sql
-- Monitor active connections
SELECT 
    datname,
    usename,
    application_name,
    client_addr,
    state,
    count(*) AS connection_count
FROM pg_stat_activity
GROUP BY datname, usename, application_name, client_addr, state
ORDER BY connection_count DESC;

-- Identify connection leaks
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    state_change,
    now() - state_change AS idle_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle'
  AND now() - state_change > INTERVAL '10 minutes'
ORDER BY idle_duration DESC;

-- Connection usage by database
SELECT datname, count(*) AS connections,
       max_conn.setting::int AS max_connections,
       ROUND(count(*)::numeric / max_conn.setting::int * 100, 2) AS usage_percent
FROM pg_stat_activity, 
     (SELECT setting FROM pg_settings WHERE name = 'max_connections') AS max_conn
GROUP BY datname, max_conn.setting
ORDER BY connections DESC;
```

### Horizontal Scaling Strategies

#### Sharding Considerations

**When to Consider Sharding**:
- Single database > 2 TB
- Write throughput exceeds single instance capacity
- Geographic data distribution requirements
- Tenant isolation requirements (multi-tenancy)

**Sharding Strategies**:

1. **Range-Based Sharding**
   - Shard by date ranges (e.g., orders by year)
   - Shard by ID ranges (e.g., customer_id 1-1M, 1M-2M)
   - Pros: Simple, predictable
   - Cons: Uneven distribution, hot shards

2. **Hash-Based Sharding**
   - Shard by hash of key (e.g., hash(customer_id) % num_shards)
   - Pros: Even distribution
   - Cons: Complex range queries, difficult rebalancing

3. **Geographic Sharding**
   - Shard by region (e.g., US, EU, APAC)
   - Pros: Data locality, compliance
   - Cons: Uneven distribution, cross-shard queries

4. **Functional Sharding**
   - Separate databases by bounded context
   - Pros: Clear boundaries, independent scaling
   - Cons: Cross-context queries complex

**Sharding Implementation Example**:

```sql
-- Create shard routing table
CREATE TABLE shard_routing (
    shard_id INTEGER PRIMARY KEY,
    shard_name TEXT NOT NULL,
    db_host TEXT NOT NULL,
    db_port INTEGER NOT NULL,
    db_name TEXT NOT NULL,
    min_customer_id BIGINT,
    max_customer_id BIGINT,
    is_active BOOLEAN DEFAULT true
);

-- Insert shard configuration
INSERT INTO shard_routing VALUES
(1, 'shard-1', 'shard1.xxx.rds.amazonaws.com', 5432, 'ecommerce_shard1', 1, 1000000, true),
(2, 'shard-2', 'shard2.xxx.rds.amazonaws.com', 5432, 'ecommerce_shard2', 1000001, 2000000, true),
(3, 'shard-3', 'shard3.xxx.rds.amazonaws.com', 5432, 'ecommerce_shard3', 2000001, 3000000, true);
```

```java
// Application-level sharding logic
@Component
public class ShardRouter {
    
    private final Map<Integer, DataSource> shardDataSources;
    
    public DataSource getShardForCustomer(Long customerId) {
        // Hash-based routing
        int shardId = (int) (customerId % shardDataSources.size()) + 1;
        return shardDataSources.get(shardId);
    }
    
    public List<DataSource> getAllShards() {
        return new ArrayList<>(shardDataSources.values());
    }
}

// Query across all shards
public List<Order> findOrdersByDateRange(LocalDate startDate, LocalDate endDate) {
    List<Order> allOrders = new ArrayList<>();
    
    for (DataSource shard : shardRouter.getAllShards()) {
        try (Connection conn = shard.getConnection()) {
            List<Order> shardOrders = queryOrdersFromShard(conn, startDate, endDate);
            allOrders.addAll(shardOrders);
        }
    }
    
    return allOrders.stream()
        .sorted(Comparator.comparing(Order::getCreatedAt))
        .collect(Collectors.toList());
}
```

**Sharding Challenges**:
- Cross-shard queries and joins
- Distributed transactions
- Rebalancing shards
- Schema migrations across shards
- Backup and restore complexity

**Alternatives to Sharding**:
- Vertical partitioning (separate tables to different databases)
- Read replicas for read scaling
- Caching layer (Redis) to reduce database load
- Archive old data to separate database
- Use managed sharding solutions (Citus, Vitess)

### Multi-Region Scaling Considerations

#### Multi-Region Architecture Patterns

**Pattern 1: Active-Passive (DR)**
- Primary region handles all traffic
- Secondary region for disaster recovery
- Cross-region read replica
- RTO: 5-15 minutes, RPO: < 1 minute

**Pattern 2: Active-Active (Global)**
- Multiple regions handle traffic simultaneously
- Cross-region replication
- Complex conflict resolution
- RTO: < 1 minute, RPO: < 1 minute

**Pattern 3: Active-Read (Distributed Reads)**
- Primary region for writes
- Multiple regions for reads
- Read replicas in each region
- RTO: 5-15 minutes, RPO: < 1 minute

#### Cross-Region Read Replica Setup

```bash
# Create cross-region read replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-production-tokyo \
  --source-db-instance-identifier arn:aws:rds:us-east-1:123456789012:db:ecommerce-production \
  --db-instance-class db.r6g.xlarge \
  --region ap-northeast-1 \
  --publicly-accessible false \
  --storage-encrypted \
  --kms-key-id arn:aws:kms:ap-northeast-1:123456789012:key/xxxxx

# Monitor cross-region replication lag
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name ReplicaLag \
  --dimensions Name=DBInstanceIdentifier,Value=ecommerce-production-tokyo \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average,Maximum \
  --region ap-northeast-1
```

**Cross-Region Considerations**:
- Network latency (50-200ms between regions)
- Data transfer costs ($0.02/GB out, $0.00/GB in)
- Compliance and data residency requirements
- Encryption key management across regions
- Monitoring and alerting in each region

### Cost Optimization for Database Scaling

#### Cost Analysis

```bash
# Calculate current database costs
aws ce get-cost-and-usage \
  --time-period Start=2024-01-01,End=2024-01-31 \
  --granularity MONTHLY \
  --metrics BlendedCost \
  --filter file://rds-filter.json

# rds-filter.json
{
  "Dimensions": {
    "Key": "SERVICE",
    "Values": ["Amazon Relational Database Service"]
  }
}

# Get detailed RDS pricing
aws pricing get-products \
  --service-code AmazonRDS \
  --filters Type=TERM_MATCH,Field=instanceType,Value=db.r6g.xlarge \
           Type=TERM_MATCH,Field=location,Value="US East (N. Virginia)" \
  --region us-east-1
```

#### Cost Optimization Strategies

**1. Right-Sizing**
```sql
-- Analyze resource utilization
SELECT 
    'CPU' AS metric,
    CASE 
        WHEN avg_cpu < 30 THEN 'Over-provisioned - Consider smaller instance'
        WHEN avg_cpu > 70 THEN 'Under-provisioned - Consider larger instance'
        ELSE 'Appropriately sized'
    END AS recommendation
FROM (
    SELECT 50 AS avg_cpu  -- Replace with actual CloudWatch metric
) AS metrics;

-- Check if read replicas are utilized
SELECT 
    client_addr,
    count(*) AS connection_count,
    sum(CASE WHEN state = 'active' THEN 1 ELSE 0 END) AS active_queries
FROM pg_stat_activity
GROUP BY client_addr
ORDER BY connection_count DESC;
```

**2. Reserved Instances**
```bash
# Purchase 1-year reserved instance (save ~35%)
aws rds purchase-reserved-db-instances-offering \
  --reserved-db-instances-offering-id xxxxx \
  --reserved-db-instance-id ecommerce-production-ri \
  --db-instance-count 1

# Purchase 3-year reserved instance (save ~60%)
aws rds purchase-reserved-db-instances-offering \
  --reserved-db-instances-offering-id xxxxx \
  --reserved-db-instance-id ecommerce-production-ri-3yr \
  --db-instance-count 1
```

**3. Storage Optimization**
```bash
# Use gp3 instead of gp2 (20% cost savings)
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --storage-type gp3 \
  --apply-immediately

# Enable storage autoscaling to avoid over-provisioning
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --max-allocated-storage 2000 \
  --apply-immediately

# Archive old data to S3 (much cheaper than RDS storage)
# RDS storage: $0.115/GB-month
# S3 Standard: $0.023/GB-month
# S3 Glacier: $0.004/GB-month
```

**4. Backup Optimization**
```bash
# Reduce backup retention for non-production
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-staging \
  --backup-retention-period 3 \
  --apply-immediately

# Use snapshot copy to cheaper region for long-term retention
aws rds copy-db-snapshot \
  --source-db-snapshot-identifier ecommerce-production-monthly-202401 \
  --target-db-snapshot-identifier ecommerce-production-monthly-202401-archive \
  --source-region us-east-1 \
  --region us-west-2
```

**5. Development/Testing Cost Reduction**
```bash
# Use smaller instances for dev/test
# Production: db.r6g.2xlarge ($0.904/hour)
# Staging: db.r6g.xlarge ($0.452/hour) - 50% savings
# Development: db.t3.large ($0.145/hour) - 84% savings

# Stop dev/test instances when not in use
aws rds stop-db-instance \
  --db-instance-identifier ecommerce-development

# Start when needed
aws rds start-db-instance \
  --db-instance-identifier ecommerce-development
```

**Cost Optimization Checklist**:
- [ ] Right-size instances based on actual usage
- [ ] Use Reserved Instances for production (35-60% savings)
- [ ] Upgrade to gp3 storage (20% savings)
- [ ] Enable storage autoscaling
- [ ] Archive old data to S3
- [ ] Reduce backup retention for non-production
- [ ] Use smaller instances for dev/test
- [ ] Stop dev/test instances when not in use
- [ ] Delete unused snapshots
- [ ] Delete unused read replicas
- [ ] Monitor and optimize cross-region data transfer

### Capacity Planning Best Practices

#### Regular Capacity Reviews

**Monthly Review**:
- Review growth metrics and trends
- Check resource utilization (CPU, memory, IOPS, storage)
- Identify performance bottlenecks
- Review and optimize slow queries
- Update capacity forecasts

**Quarterly Review**:
- Comprehensive capacity planning
- Evaluate scaling strategy effectiveness
- Review cost optimization opportunities
- Plan for upcoming features/campaigns
- Update disaster recovery plans

**Annual Review**:
- Long-term capacity planning (1-3 years)
- Technology refresh planning
- Major version upgrade planning
- Architecture review and optimization

#### Capacity Planning Checklist

- [ ] Monitor database growth trends weekly
- [ ] Track key performance metrics daily
- [ ] Review capacity forecasts monthly
- [ ] Test scaling procedures quarterly
- [ ] Update capacity plans after major releases
- [ ] Document capacity decisions and rationale
- [ ] Maintain capacity planning dashboard
- [ ] Set up proactive alerts for capacity thresholds
- [ ] Plan for seasonal traffic variations
- [ ] Budget for capacity expansion

#### Capacity Planning Dashboard

```sql
-- Create comprehensive capacity view
CREATE OR REPLACE VIEW capacity_planning_dashboard AS
SELECT 
    -- Current State
    pg_size_pretty(pg_database_size(current_database())) AS current_db_size,
    (SELECT count(*) FROM pg_stat_activity WHERE state != 'idle') AS active_connections,
    (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') AS max_connections,
    
    -- Growth Metrics (from tracking tables)
    (SELECT pg_size_pretty(AVG(total_size_bytes - LAG(total_size_bytes) OVER (ORDER BY recorded_at)))
     FROM database_growth_metrics
     WHERE recorded_at > NOW() - INTERVAL '30 days') AS avg_daily_growth,
    
    -- Capacity Status
    CASE 
        WHEN (SELECT count(*) FROM pg_stat_activity WHERE state != 'idle')::float / 
             (SELECT setting::int FROM pg_settings WHERE name = 'max_connections') > 0.8 
        THEN 'WARNING - Connection capacity'
        ELSE 'OK'
    END AS capacity_status;

-- Query the dashboard
SELECT * FROM capacity_planning_dashboard;
```

---

**Last Updated**: 2025-11-08  
**Owner**: Database Team  
**Review Cycle**: Quarterly
