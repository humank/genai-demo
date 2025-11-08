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

## Related Documentation

- [Backup and Restore](backup-restore.md)
- [Database Connection Issues Runbook](../runbooks/database-connection-issues.md)
- [Slow API Responses Runbook](../runbooks/slow-api-responses.md)

---

**Last Updated**: 2025-10-25  
**Owner**: Database Team  
**Review Cycle**: Quarterly
