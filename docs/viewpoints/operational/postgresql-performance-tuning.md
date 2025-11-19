---
title: "PostgreSQL Performance Tuning Guide"
viewpoint: "Operational"
status: "active"
last_updated: "2025-01-22"
stakeholders: ["Operations Team", "DBA Team", "SRE Team", "Development Team"]
---

# PostgreSQL Performance Tuning Guide

> **Viewpoint**: Operational  
> **Purpose**: Comprehensive guide for PostgreSQL performance optimization  
> **Audience**: Operations Team, DBA Team, SRE Team, Development Team

## Overview

This guide provides a systematic approach to PostgreSQL performance tuning, covering parameter optimization, query tuning, index strategies, connection pooling, and maintenance procedures.

## Performance Tuning Methodology

### 1. Assessment Phase

**Identify Performance Bottlenecks**:

```sql
-- Check current database performance metrics
SELECT 
  datname,
  numbackends AS connections,
  xact_commit AS commits,
  xact_rollback AS rollbacks,
  blks_read AS disk_reads,
  blks_hit AS cache_hits,
  round(100.0 * blks_hit / NULLIF(blks_hit + blks_read, 0), 2) AS cache_hit_ratio
FROM pg_stat_database
WHERE datname = 'ecommerce'
ORDER BY datname;

-- Identify slow queries
SELECT 
  query,
  calls,
  total_exec_time,
  mean_exec_time,
  max_exec_time,
  stddev_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

### 2. Baseline Establishment

**Capture Current Performance Metrics**:

```bash
#!/bin/bash
# Script: capture-baseline-metrics.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
OUTPUT_DIR="/var/log/postgresql/baselines"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p $OUTPUT_DIR

# Capture database statistics
psql -h $DB_HOST -U admin -d $DB_NAME -o "$OUTPUT_DIR/baseline_$TIMESTAMP.txt" << EOF
-- Database size and activity
SELECT 
  'Database Size' AS metric,
  pg_size_pretty(pg_database_size('$DB_NAME')) AS value;

SELECT 
  'Active Connections' AS metric,
  count(*) AS value
FROM pg_stat_activity
WHERE state = 'active';

-- Cache hit ratio
SELECT 
  'Cache Hit Ratio' AS metric,
  round(100.0 * sum(blks_hit) / NULLIF(sum(blks_hit + blks_read), 0), 2) || '%' AS value
FROM pg_stat_database;

-- Transaction rate
SELECT 
  'Transactions/sec' AS metric,
  round(sum(xact_commit + xact_rollback) / extract(epoch from (now() - stats_reset)), 2) AS value
FROM pg_stat_database;

-- Top 10 largest tables
SELECT 
  'Top Tables' AS metric,
  schemaname || '.' || tablename AS table_name,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 10;
EOF

echo "Baseline metrics captured: $OUTPUT_DIR/baseline_$TIMESTAMP.txt"
```

### 3. Tuning Implementation

**Systematic Tuning Approach**:

1. Memory configuration (shared_buffers, work_mem, effective_cache_size)
2. Query optimization (indexes, query rewriting)
3. Connection pooling (HikariCP, pgBouncer)
4. Maintenance operations (VACUUM, ANALYZE)
5. Write optimization (WAL, checkpoints)

### 4. Validation and Monitoring

**Verify Improvements**:

```bash
# Compare before/after metrics
./compare-performance-metrics.sh baseline_before.txt baseline_after.txt
```

## PostgreSQL Parameter Tuning

### Memory Configuration Parameters

#### shared_buffers

**Purpose**: PostgreSQL's main memory cache for data pages

**Tuning Guidelines**:

- **Default**: 128MB (too low for production)
- **Recommended**: 25% of total RAM (up to 8-16GB)
- **AWS RDS**: Limited by instance type

**Configuration**:

```sql
-- Check current value
SHOW shared_buffers;

-- Recommended settings by instance size
-- db.t3.medium (4GB RAM): shared_buffers = 1GB
-- db.r5.large (16GB RAM): shared_buffers = 4GB
-- db.r5.xlarge (32GB RAM): shared_buffers = 8GB
-- db.r5.2xlarge (64GB RAM): shared_buffers = 16GB

-- AWS RDS parameter group modification
aws rds modify-db-parameter-group \
  --db-parameter-group-name ecommerce-params \
  --parameters "ParameterName=shared_buffers,ParameterValue={DBInstanceClassMemory/4},ApplyMethod=pending-reboot"
```

**Validation**:

```sql
-- Monitor buffer cache hit ratio (target: >99%)
SELECT 
  sum(heap_blks_read) AS heap_read,
  sum(heap_blks_hit) AS heap_hit,
  round(100.0 * sum(heap_blks_hit) / NULLIF(sum(heap_blks_hit + heap_blks_read), 0), 2) AS cache_hit_ratio
FROM pg_statio_user_tables;
```

#### work_mem

**Purpose**: Memory for sorting and hash operations per query operation

**Tuning Guidelines**:

- **Default**: 4MB (often too low)
- **Recommended**: Start with 16-64MB, adjust based on workload
- **Formula**: (Total RAM - shared_buffers) / (max_connections * 2-3)

**Configuration**:

```sql
-- Check current value
SHOW work_mem;

-- Set globally (requires restart)
ALTER SYSTEM SET work_mem = '64MB';

-- Set per session for specific queries
SET work_mem = '256MB';

-- Set per query
SELECT /*+ Set(work_mem '256MB') */ * FROM large_table ORDER BY column;
```

**Monitoring**:

```sql
-- Identify queries using temp files (sign of insufficient work_mem)
SELECT 
  query,
  calls,
  total_exec_time,
  temp_blks_written,
  temp_blks_written * 8192 / 1024 / 1024 AS temp_mb
FROM pg_stat_statements
WHERE temp_blks_written > 0
ORDER BY temp_blks_written DESC
LIMIT 20;
```

#### effective_cache_size

**Purpose**: Hint to query planner about available OS cache

**Tuning Guidelines**:

- **Default**: 4GB
- **Recommended**: 50-75% of total RAM
- **Does not allocate memory**, only affects query planning

**Configuration**:

```sql
-- Check current value
SHOW effective_cache_size;

-- Recommended settings
-- db.t3.medium (4GB RAM): effective_cache_size = 3GB
-- db.r5.large (16GB RAM): effective_cache_size = 12GB
-- db.r5.xlarge (32GB RAM): effective_cache_size = 24GB
-- db.r5.2xlarge (64GB RAM): effective_cache_size = 48GB

-- Set value
ALTER SYSTEM SET effective_cache_size = '24GB';
SELECT pg_reload_conf();
```

#### maintenance_work_mem

**Purpose**: Memory for maintenance operations (VACUUM, CREATE INDEX, ALTER TABLE)

**Tuning Guidelines**:

- **Default**: 64MB
- **Recommended**: 256MB - 2GB
- **Maximum**: 2GB (PostgreSQL limitation)

**Configuration**:

```sql
-- Check current value
SHOW maintenance_work_mem;

-- Set globally
ALTER SYSTEM SET maintenance_work_mem = '1GB';
SELECT pg_reload_conf();

-- Set for specific maintenance operation
SET maintenance_work_mem = '2GB';
REINDEX TABLE large_table;
```

### Query Planner Parameters

#### random_page_cost

**Purpose**: Cost estimate for random disk I/O

**Tuning Guidelines**:

- **Default**: 4.0 (for spinning disks)
- **SSD/NVMe**: 1.1 - 1.5
- **AWS EBS gp3**: 1.1
- **AWS EBS io2**: 1.0

**Configuration**:

```sql
-- Check current value
SHOW random_page_cost;

-- Set for SSD storage
ALTER SYSTEM SET random_page_cost = 1.1;
SELECT pg_reload_conf();
```

#### effective_io_concurrency

**Purpose**: Number of concurrent disk I/O operations

**Tuning Guidelines**:

- **Default**: 1
- **SSD**: 200
- **NVMe**: 200-300
- **AWS EBS**: 200

**Configuration**:

```sql
-- Set for SSD storage
ALTER SYSTEM SET effective_io_concurrency = 200;
SELECT pg_reload_conf();
```

### Connection Parameters

#### max_connections

**Purpose**: Maximum number of concurrent connections

**Tuning Guidelines**:

- **Default**: 100
- **Recommended**: Based on application needs and available memory
- **Formula**: Consider work_mem * max_connections < available RAM

**Configuration**:

```sql
-- Check current connections
SELECT count(*) FROM pg_stat_activity;

-- Set max connections (requires restart)
ALTER SYSTEM SET max_connections = 200;
-- Restart required
```

**Best Practice**: Use connection pooling (pgBouncer) instead of increasing max_connections

## Query Optimization Workflow

### Step 1: Identify Slow Queries

**Enable pg_stat_statements**:

```sql
-- Check if extension is enabled
SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';

-- Enable if not present
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Configure tracking
ALTER SYSTEM SET pg_stat_statements.track = 'all';
ALTER SYSTEM SET pg_stat_statements.max = 10000;
SELECT pg_reload_conf();
```

**Find Slow Queries**:

```sql
-- Top 20 slowest queries by average execution time
SELECT 
  substring(query, 1, 100) AS short_query,
  calls,
  round(total_exec_time::numeric, 2) AS total_time_ms,
  round(mean_exec_time::numeric, 2) AS avg_time_ms,
  round(max_exec_time::numeric, 2) AS max_time_ms,
  round((100 * total_exec_time / sum(total_exec_time) OVER ())::numeric, 2) AS percent_total
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;

-- Queries with most total execution time
SELECT 
  substring(query, 1, 100) AS short_query,
  calls,
  round(total_exec_time::numeric, 2) AS total_time_ms,
  round(mean_exec_time::numeric, 2) AS avg_time_ms
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;

-- Queries using temp files (need more work_mem)
SELECT 
  substring(query, 1, 100) AS short_query,
  calls,
  temp_blks_written,
  round((temp_blks_written * 8192 / 1024 / 1024)::numeric, 2) AS temp_mb
FROM pg_stat_statements
WHERE temp_blks_written > 0
ORDER BY temp_blks_written DESC
LIMIT 20;
```

### Step 2: Analyze Query Execution Plans

**Use EXPLAIN ANALYZE**:

```sql
-- Basic explain
EXPLAIN SELECT * FROM orders WHERE customer_id = 'CUST-001';

-- Detailed explain with actual execution
EXPLAIN (ANALYZE, BUFFERS, VERBOSE) 
SELECT o.*, c.name 
FROM orders o 
JOIN customers c ON o.customer_id = c.id 
WHERE o.order_date >= '2024-01-01';

-- Explain with costs and timing
EXPLAIN (ANALYZE, COSTS, TIMING, BUFFERS)
SELECT * FROM products WHERE category = 'Electronics' ORDER BY price DESC LIMIT 10;
```

**Interpret Execution Plans**:

```sql
-- Look for these issues:
-- 1. Sequential Scans on large tables
-- 2. High cost estimates
-- 3. Nested Loop joins on large datasets
-- 4. Sort operations with high memory usage
-- 5. Bitmap Heap Scans with low selectivity
```

### Step 3: Query Rewriting Techniques

**Avoid SELECT ***:

```sql
-- ❌ Bad: Fetches all columns
SELECT * FROM orders WHERE customer_id = 'CUST-001';

-- ✅ Good: Fetch only needed columns
SELECT id, order_date, total_amount, status 
FROM orders 
WHERE customer_id = 'CUST-001';
```

**Use EXISTS instead of IN for subqueries**:

```sql
-- ❌ Bad: IN with subquery
SELECT * FROM customers 
WHERE id IN (SELECT customer_id FROM orders WHERE order_date >= '2024-01-01');

-- ✅ Good: EXISTS
SELECT * FROM customers c
WHERE EXISTS (
  SELECT 1 FROM orders o 
  WHERE o.customer_id = c.id AND o.order_date >= '2024-01-01'
);
```

**Optimize JOINs**:

```sql
-- ❌ Bad: Multiple LEFT JOINs
SELECT o.*, c.*, p.*, i.*
FROM orders o
LEFT JOIN customers c ON o.customer_id = c.id
LEFT JOIN payments p ON o.id = p.order_id
LEFT JOIN invoices i ON o.id = i.order_id;

-- ✅ Good: Only join what's needed
SELECT o.id, o.order_date, c.name, p.status
FROM orders o
INNER JOIN customers c ON o.customer_id = c.id
LEFT JOIN payments p ON o.id = p.order_id
WHERE o.order_date >= '2024-01-01';
```

**Use LIMIT with ORDER BY**:

```sql
-- ❌ Bad: No limit on large result set
SELECT * FROM products ORDER BY created_at DESC;

-- ✅ Good: Use pagination
SELECT * FROM products 
ORDER BY created_at DESC 
LIMIT 20 OFFSET 0;
```

## Index Strategy and Optimization

### Index Types and Use Cases

#### B-tree Indexes (Default)

**Best For**: Equality and range queries

```sql
-- Create B-tree index
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);

-- Composite index for multiple columns
CREATE INDEX idx_orders_customer_date ON orders(customer_id, order_date);

-- Partial index for specific conditions
CREATE INDEX idx_active_orders ON orders(order_date) 
WHERE status = 'ACTIVE';
```

#### Hash Indexes

**Best For**: Equality comparisons only

```sql
-- Create hash index (PostgreSQL 10+)
CREATE INDEX idx_products_sku_hash ON products USING HASH (sku);
```

#### GIN Indexes

**Best For**: Full-text search, JSONB, arrays

```sql
-- GIN index for JSONB
CREATE INDEX idx_products_attributes ON products USING GIN (attributes);

-- GIN index for full-text search
CREATE INDEX idx_products_search ON products USING GIN (to_tsvector('english', name || ' ' || description));

-- GIN index for arrays
CREATE INDEX idx_products_tags ON products USING GIN (tags);
```

#### GiST Indexes

**Best For**: Geometric data, full-text search

```sql
-- GiST index for geometric data
CREATE INDEX idx_stores_location ON stores USING GIST (location);

-- GiST index for range types
CREATE INDEX idx_promotions_period ON promotions USING GIST (valid_period);
```

### Index Design Best Practices

**1. Analyze Query Patterns**:

```sql
-- Find missing indexes
SELECT 
  schemaname,
  tablename,
  seq_scan,
  seq_tup_read,
  idx_scan,
  seq_tup_read / NULLIF(seq_scan, 0) AS avg_seq_tup_read
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;
```

**2. Create Covering Indexes**:

```sql
-- Include additional columns in index
CREATE INDEX idx_orders_customer_covering 
ON orders(customer_id) 
INCLUDE (order_date, total_amount, status);

-- Query can be satisfied by index alone (Index-Only Scan)
SELECT order_date, total_amount, status 
FROM orders 
WHERE customer_id = 'CUST-001';
```

**3. Use Partial Indexes**:

```sql
-- Index only active records
CREATE INDEX idx_active_customers 
ON customers(email) 
WHERE status = 'ACTIVE';

-- Index only recent orders
CREATE INDEX idx_recent_orders 
ON orders(order_date) 
WHERE order_date >= '2024-01-01';
```

**4. Optimize Composite Indexes**:

```sql
-- Order matters: most selective column first
CREATE INDEX idx_orders_status_date ON orders(status, order_date);

-- This index can be used for:
-- WHERE status = 'PENDING'
-- WHERE status = 'PENDING' AND order_date >= '2024-01-01'

-- But NOT efficiently for:
-- WHERE order_date >= '2024-01-01' (without status)
```

### Index Maintenance

**Identify Unused Indexes**:

```sql
-- Find indexes that are never used
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'
ORDER BY pg_relation_size(indexrelid) DESC;
```

**Find Duplicate Indexes**:

```sql
-- Identify duplicate indexes
SELECT 
  pg_size_pretty(sum(pg_relation_size(idx))::bigint) AS size,
  (array_agg(idx))[1] AS idx1,
  (array_agg(idx))[2] AS idx2,
  (array_agg(idx))[3] AS idx3
FROM (
  SELECT 
    indexrelid::regclass AS idx,
    (indrelid::text || E'\n' || indclass::text || E'\n' || 
     indkey::text || E'\n' || COALESCE(indexprs::text, '') || E'\n' || 
     COALESCE(indpred::text, '')) AS key
  FROM pg_index
) sub
GROUP BY key
HAVING count(*) > 1
ORDER BY sum(pg_relation_size(idx)) DESC;
```

**Rebuild Bloated Indexes**:

```sql
-- Check index bloat
SELECT 
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;

-- Rebuild index concurrently (no downtime)
CREATE INDEX CONCURRENTLY idx_orders_customer_id_new ON orders(customer_id);
DROP INDEX CONCURRENTLY idx_orders_customer_id;
ALTER INDEX idx_orders_customer_id_new RENAME TO idx_orders_customer_id;

-- Or use REINDEX CONCURRENTLY (PostgreSQL 12+)
REINDEX INDEX CONCURRENTLY idx_orders_customer_id;
```

## Connection Pool Tuning

### HikariCP Configuration (Application Layer)

**Optimal Settings**:

```yaml
# application.yml
spring:
  datasource:
    hikari:
      # Connection pool size
      maximum-pool-size: 20
      minimum-idle: 5
      
      # Connection timeout
      connection-timeout: 20000  # 20 seconds
      
      # Idle timeout
      idle-timeout: 300000  # 5 minutes
      
      # Max lifetime
      max-lifetime: 1200000  # 20 minutes
      
      # Validation
      validation-timeout: 5000
      connection-test-query: SELECT 1
      
      # Leak detection
      leak-detection-threshold: 60000  # 60 seconds
      
      # Pool name
      pool-name: EcommerceHikariPool
```

**Sizing Guidelines**:

```text
Formula: connections = ((core_count * 2) + effective_spindle_count)

For CPU-bound applications:

- connections = core_count * 2

For I/O-bound applications:

- connections = core_count * 4

Example:

- 4 core CPU, SSD storage
- connections = (4 * 2) + 1 = 9
- Set maximum-pool-size = 10-20

```

**Monitoring HikariCP**:

```java
@Component
public class HikariMetrics {
    
    @Autowired
    private HikariDataSource dataSource;
    
    @Scheduled(fixedRate = 60000)
    public void logPoolMetrics() {
        HikariPoolMXBean poolMXBean = dataSource.getHikariPoolMXBean();
        
        logger.info("HikariCP Metrics: " +
            "Active={}, Idle={}, Total={}, Waiting={}", 
            poolMXBean.getActiveConnections(),
            poolMXBean.getIdleConnections(),
            poolMXBean.getTotalConnections(),
            poolMXBean.getThreadsAwaitingConnection()
        );
    }
}
```

### pgBouncer Configuration (Database Layer)

**Installation and Setup**:

```bash
# Install pgBouncer
sudo apt-get install pgbouncer

# Configure pgBouncer
sudo vi /etc/pgbouncer/pgbouncer.ini
```

**Optimal Configuration**:

```ini
[databases]
ecommerce = host=ecommerce-prod-db.xxx.rds.amazonaws.com port=5432 dbname=ecommerce

[pgbouncer]
# Connection pooling mode
pool_mode = transaction  # transaction, session, or statement

# Connection limits
max_client_conn = 1000
default_pool_size = 25
min_pool_size = 10
reserve_pool_size = 5
reserve_pool_timeout = 3

# Server connection limits
max_db_connections = 100
max_user_connections = 100

# Timeouts
server_idle_timeout = 600
server_lifetime = 3600
server_connect_timeout = 15
query_timeout = 0
query_wait_timeout = 120
client_idle_timeout = 0
idle_transaction_timeout = 0

# Logging
log_connections = 1
log_disconnections = 1
log_pooler_errors = 1

# Performance
listen_addr = *
listen_port = 6432
auth_type = md5
auth_file = /etc/pgbouncer/userlist.txt
admin_users = pgbouncer_admin
stats_users = pgbouncer_stats
```

**Pool Mode Selection**:

1. **Session Mode** (default):
   - One server connection per client connection
   - Best for: Applications that use SET commands, temp tables
   - Least efficient pooling

2. **Transaction Mode** (recommended):
   - Server connection released after each transaction
   - Best for: Most web applications
   - Most efficient pooling

3. **Statement Mode**:
   - Server connection released after each statement
   - Best for: Simple query applications
   - Cannot use transactions

**Monitoring pgBouncer**:

```bash
# Connect to pgBouncer admin console
psql -h localhost -p 6432 -U pgbouncer_admin pgbouncer

# Show pool statistics
SHOW POOLS;

# Show client connections
SHOW CLIENTS;

# Show server connections
SHOW SERVERS;

# Show statistics
SHOW STATS;

# Show configuration
SHOW CONFIG;
```

**pgBouncer Metrics to Monitor**:

```sql
-- In pgBouncer admin console
SHOW POOLS;
-- Monitor:
-- - cl_active: Active client connections
-- - cl_waiting: Clients waiting for connection
-- - sv_active: Active server connections
-- - sv_idle: Idle server connections
-- - maxwait: Max wait time for connection
```

## VACUUM and Autovacuum Tuning

### Understanding VACUUM

**Purpose**:

- Reclaim storage from dead tuples
- Update statistics for query planner
- Prevent transaction ID wraparound
- Update visibility map for index-only scans

**Types of VACUUM**:

```sql
-- Regular VACUUM (non-blocking)
VACUUM;
VACUUM ANALYZE;
VACUUM VERBOSE ANALYZE orders;

-- VACUUM FULL (requires exclusive lock, rewrites table)
VACUUM FULL orders;

-- VACUUM FREEZE (prevent transaction ID wraparound)
VACUUM FREEZE;
```

### Autovacuum Configuration

**Check Autovacuum Status**:

```sql
-- Check if autovacuum is enabled
SHOW autovacuum;

-- Check autovacuum activity
SELECT 
  schemaname,
  tablename,
  last_vacuum,
  last_autovacuum,
  last_analyze,
  last_autoanalyze,
  vacuum_count,
  autovacuum_count,
  analyze_count,
  autoanalyze_count
FROM pg_stat_user_tables
ORDER BY last_autovacuum DESC NULLS LAST;
```

**Autovacuum Parameters**:

```sql
-- Global settings (requires restart)
ALTER SYSTEM SET autovacuum = on;
ALTER SYSTEM SET autovacuum_max_workers = 3;
ALTER SYSTEM SET autovacuum_naptime = '1min';

-- Vacuum cost delay (throttling)
ALTER SYSTEM SET autovacuum_vacuum_cost_delay = '20ms';
ALTER SYSTEM SET autovacuum_vacuum_cost_limit = 200;

-- Thresholds for triggering autovacuum
ALTER SYSTEM SET autovacuum_vacuum_threshold = 50;
ALTER SYSTEM SET autovacuum_vacuum_scale_factor = 0.2;
ALTER SYSTEM SET autovacuum_analyze_threshold = 50;
ALTER SYSTEM SET autovacuum_analyze_scale_factor = 0.1;

-- Reload configuration
SELECT pg_reload_conf();
```

**Per-Table Autovacuum Settings**:

```sql
-- Aggressive autovacuum for high-churn tables
ALTER TABLE orders SET (
  autovacuum_vacuum_scale_factor = 0.05,
  autovacuum_vacuum_threshold = 100,
  autovacuum_analyze_scale_factor = 0.02,
  autovacuum_analyze_threshold = 50
);

-- Disable autovacuum for append-only tables
ALTER TABLE audit_logs SET (
  autovacuum_enabled = false
);

-- Increase autovacuum workers for large tables
ALTER TABLE products SET (
  autovacuum_vacuum_cost_delay = 10,
  autovacuum_vacuum_cost_limit = 1000
);
```

### Monitoring Dead Tuples

**Check Table Bloat**:

```sql
-- Identify tables with high dead tuple ratio
SELECT 
  schemaname,
  tablename,
  n_live_tup,
  n_dead_tup,
  round(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_ratio,
  last_vacuum,
  last_autovacuum
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;

-- Estimate table bloat
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
  round(100.0 * pg_relation_size(schemaname||'.'||tablename) / 
    NULLIF(pg_total_relation_size(schemaname||'.'||tablename), 0), 2) AS table_ratio
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
```

### Manual VACUUM Strategy

**Scheduled VACUUM**:

```bash
#!/bin/bash
# Script: scheduled-vacuum.sh
# Schedule: Daily at 2:00 AM

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"

# Vacuum high-churn tables
psql -h $DB_HOST -U admin -d $DB_NAME << EOF
-- Vacuum orders table
VACUUM VERBOSE ANALYZE orders;

-- Vacuum order_items table
VACUUM VERBOSE ANALYZE order_items;

-- Vacuum shopping_carts table
VACUUM VERBOSE ANALYZE shopping_carts;

-- Vacuum sessions table
VACUUM VERBOSE ANALYZE sessions;

-- Check for tables needing vacuum
SELECT 
  schemaname || '.' || tablename AS table_name,
  n_dead_tup,
  round(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_ratio
FROM pg_stat_user_tables
WHERE n_dead_tup > 10000
ORDER BY n_dead_tup DESC;
EOF

echo "Scheduled vacuum completed at $(date)"
```

**VACUUM FULL Strategy** (requires downtime):

```bash
#!/bin/bash
# Script: vacuum-full-maintenance.sh
# Schedule: Monthly during maintenance window

# Create backup before VACUUM FULL
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier pre-vacuum-full-$(date +%Y%m%d)

# Perform VACUUM FULL on bloated tables
psql -h $DB_HOST -U admin -d $DB_NAME << EOF
-- VACUUM FULL on specific tables (requires exclusive lock)
VACUUM FULL VERBOSE orders;
VACUUM FULL VERBOSE order_items;

-- Reindex after VACUUM FULL
REINDEX TABLE orders;
REINDEX TABLE order_items;

-- Update statistics
ANALYZE orders;
ANALYZE order_items;
EOF

echo "VACUUM FULL maintenance completed at $(date)"
```

## Statistics Collection Optimization

### Understanding PostgreSQL Statistics

**Statistics Types**:

1. **Table statistics**: Row counts, dead tuples, vacuum activity
2. **Index statistics**: Index scans, tuples read/fetched
3. **Query statistics**: Execution times, buffer usage (pg_stat_statements)

### Configure Statistics Collection

**Statistics Parameters**:

```sql
-- Enable statistics collection
ALTER SYSTEM SET track_activities = on;
ALTER SYSTEM SET track_counts = on;
ALTER SYSTEM SET track_io_timing = on;
ALTER SYSTEM SET track_functions = 'all';

-- Configure statistics target (default: 100, range: 1-10000)
ALTER SYSTEM SET default_statistics_target = 100;

-- Per-column statistics target
ALTER TABLE orders ALTER COLUMN customer_id SET STATISTICS 1000;
ALTER TABLE products ALTER COLUMN category SET STATISTICS 500;

-- Reload configuration
SELECT pg_reload_conf();
```

**Statistics Target Guidelines**:

- **Default (100)**: Sufficient for most columns
- **Higher (500-1000)**: For columns with high cardinality or used in complex queries
- **Lower (10-50)**: For columns with low cardinality

### Manual Statistics Update

**ANALYZE Command**:

```sql
-- Analyze entire database
ANALYZE;

-- Analyze specific table
ANALYZE VERBOSE orders;

-- Analyze specific columns
ANALYZE orders (customer_id, order_date, status);

-- Check when tables were last analyzed
SELECT 
  schemaname,
  tablename,
  last_analyze,
  last_autoanalyze,
  analyze_count,
  autoanalyze_count
FROM pg_stat_user_tables
ORDER BY last_analyze DESC NULLS LAST;
```

**Scheduled Statistics Update**:

```bash
#!/bin/bash
# Script: update-statistics.sh
# Schedule: Daily at 3:00 AM

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"

psql -h $DB_HOST -U admin -d $DB_NAME << EOF
-- Update statistics for all tables
ANALYZE VERBOSE;

-- Check for tables with outdated statistics
SELECT 
  schemaname || '.' || tablename AS table_name,
  n_mod_since_analyze,
  last_analyze,
  last_autoanalyze
FROM pg_stat_user_tables
WHERE n_mod_since_analyze > 10000
ORDER BY n_mod_since_analyze DESC;
EOF

echo "Statistics update completed at $(date)"
```

### Monitor Statistics Quality

**Check Statistics Accuracy**:

```sql
-- Compare estimated vs actual row counts
EXPLAIN (ANALYZE, BUFFERS) 
SELECT * FROM orders WHERE status = 'PENDING';

-- Check histogram bounds for a column
SELECT 
  tablename,
  attname,
  n_distinct,
  correlation
FROM pg_stats
WHERE tablename = 'orders'
ORDER BY attname;

-- Identify columns with poor statistics
SELECT 
  schemaname,
  tablename,
  attname,
  n_distinct,
  correlation,
  most_common_vals,
  most_common_freqs
FROM pg_stats
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
  AND n_distinct < 0  -- Negative means estimated
ORDER BY abs(n_distinct) DESC;
```

## WAL Configuration Optimization

### Write-Ahead Log (WAL) Basics

**Purpose**:

- Ensure data durability
- Enable point-in-time recovery
- Support replication

### WAL Parameters for Write-Heavy Workloads

**Core WAL Settings**:

```sql
-- WAL level (minimal, replica, logical)
ALTER SYSTEM SET wal_level = 'replica';

-- WAL buffer size (default: -1 = 1/32 of shared_buffers)
ALTER SYSTEM SET wal_buffers = '16MB';

-- WAL writer delay
ALTER SYSTEM SET wal_writer_delay = '200ms';

-- WAL writer flush after
ALTER SYSTEM SET wal_writer_flush_after = '1MB';

-- Reload configuration
SELECT pg_reload_conf();
```

**Synchronous Commit Settings**:

```sql
-- Synchronous commit modes:
-- - on: Wait for WAL write to disk (safest, slowest)
-- - remote_write: Wait for WAL write to replica
-- - remote_apply: Wait for WAL apply on replica
-- - local: Wait for local WAL write only
-- - off: Don't wait (fastest, risk of data loss)

-- For high-throughput writes (accept small data loss risk)
ALTER SYSTEM SET synchronous_commit = 'off';

-- For critical transactions (per-session)
SET synchronous_commit = 'on';
BEGIN;
-- Critical transaction
COMMIT;
SET synchronous_commit = 'off';
```

**WAL Archiving**:

```sql
-- Enable WAL archiving
ALTER SYSTEM SET archive_mode = 'on';
ALTER SYSTEM SET archive_command = 'test ! -f /mnt/wal_archive/%f && cp %p /mnt/wal_archive/%f';
ALTER SYSTEM SET archive_timeout = '300';  -- 5 minutes

-- For AWS RDS (automatic)
-- WAL archiving is managed by AWS
```

### Monitor WAL Activity

**Check WAL Generation Rate**:

```sql
-- Current WAL location
SELECT pg_current_wal_lsn();

-- WAL generation rate
SELECT 
  pg_size_pretty(
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0')
  ) AS wal_generated;

-- WAL files
SELECT 
  count(*) AS wal_files,
  pg_size_pretty(sum(size)) AS total_size
FROM pg_ls_waldir();
```

**Monitor WAL Writes**:

```sql
-- Check WAL write statistics
SELECT 
  checkpoints_timed,
  checkpoints_req,
  checkpoint_write_time,
  checkpoint_sync_time,
  buffers_checkpoint,
  buffers_clean,
  buffers_backend
FROM pg_stat_bgwriter;
```

## Checkpoint Tuning for Write-Heavy Workloads

### Understanding Checkpoints

**Purpose**:

- Write dirty buffers to disk
- Create recovery points
- Prevent WAL growth

**Checkpoint Triggers**:

1. Time-based: checkpoint_timeout
2. WAL-based: max_wal_size
3. Manual: CHECKPOINT command

### Checkpoint Configuration

**Optimal Settings for Write-Heavy Workloads**:

```sql
-- Checkpoint timeout (default: 5min)
-- Increase for write-heavy workloads to reduce checkpoint frequency
ALTER SYSTEM SET checkpoint_timeout = '15min';

-- Maximum WAL size before checkpoint (default: 1GB)
-- Increase for write-heavy workloads
ALTER SYSTEM SET max_wal_size = '4GB';

-- Minimum WAL size to keep (default: 80MB)
ALTER SYSTEM SET min_wal_size = '1GB';

-- Checkpoint completion target (default: 0.5)
-- Spread checkpoint I/O over this fraction of checkpoint_timeout
-- Higher value = smoother I/O, longer checkpoint duration
ALTER SYSTEM SET checkpoint_completion_target = 0.9;

-- Checkpoint warning (default: 30s)
-- Log if checkpoints happen more frequently than this
ALTER SYSTEM SET checkpoint_warning = '30s';

-- Reload configuration
SELECT pg_reload_conf();
```

**Tuning Guidelines**:

1. **For OLTP workloads** (many small transactions):

   ```sql
   checkpoint_timeout = '10min'
   max_wal_size = '2GB'
   checkpoint_completion_target = 0.9
   ```

2. **For batch/ETL workloads** (large bulk operations):

   ```sql
   checkpoint_timeout = '30min'
   max_wal_size = '8GB'
   checkpoint_completion_target = 0.9
   ```

3. **For mixed workloads**:

   ```sql
   checkpoint_timeout = '15min'
   max_wal_size = '4GB'
   checkpoint_completion_target = 0.9
   ```

### Monitor Checkpoint Performance

**Check Checkpoint Statistics**:

```sql
-- View checkpoint activity
SELECT 
  checkpoints_timed,
  checkpoints_req,
  checkpoint_write_time,
  checkpoint_sync_time,
  buffers_checkpoint,
  buffers_clean,
  buffers_backend,
  buffers_backend_fsync,
  buffers_alloc,
  stats_reset
FROM pg_stat_bgwriter;

-- Calculate checkpoint frequency
SELECT 
  'Timed checkpoints' AS type,
  checkpoints_timed AS count,
  round(checkpoints_timed::numeric / 
    EXTRACT(epoch FROM (now() - stats_reset)) * 3600, 2) AS per_hour
FROM pg_stat_bgwriter
UNION ALL
SELECT 
  'Requested checkpoints' AS type,
  checkpoints_req AS count,
  round(checkpoints_req::numeric / 
    EXTRACT(epoch FROM (now() - stats_reset)) * 3600, 2) AS per_hour
FROM pg_stat_bgwriter;
```

**Identify Checkpoint Issues**:

```bash
# Check PostgreSQL logs for checkpoint warnings
grep "checkpoints are occurring too frequently" /var/log/postgresql/postgresql.log

# If you see this warning frequently:
# 1. Increase max_wal_size
# 2. Increase checkpoint_timeout
# 3. Monitor checkpoint_req vs checkpoints_timed ratio
```

**Checkpoint Monitoring Script**:

```bash
#!/bin/bash
# Script: monitor-checkpoints.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"

psql -h $DB_HOST -U admin -d $DB_NAME << EOF
-- Checkpoint statistics
SELECT 
  'Checkpoint Frequency' AS metric,
  round((checkpoints_timed + checkpoints_req)::numeric / 
    EXTRACT(epoch FROM (now() - stats_reset)) * 3600, 2) || ' per hour' AS value
FROM pg_stat_bgwriter
UNION ALL
SELECT 
  'Timed vs Requested Ratio' AS metric,
  round(checkpoints_timed::numeric / NULLIF(checkpoints_req, 0), 2)::text AS value
FROM pg_stat_bgwriter
UNION ALL
SELECT 
  'Average Checkpoint Write Time' AS metric,
  round(checkpoint_write_time::numeric / NULLIF(checkpoints_timed + checkpoints_req, 0), 2) || ' ms' AS value
FROM pg_stat_bgwriter
UNION ALL
SELECT 
  'Average Checkpoint Sync Time' AS metric,
  round(checkpoint_sync_time::numeric / NULLIF(checkpoints_timed + checkpoints_req, 0), 2) || ' ms' AS value
FROM pg_stat_bgwriter;

-- WAL generation rate
SELECT 
  'WAL Generation Rate' AS metric,
  pg_size_pretty(
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0')
  ) AS value;
EOF
```

### Optimize Background Writer

**Background Writer Parameters**:

```sql
-- Background writer delay (default: 200ms)
ALTER SYSTEM SET bgwriter_delay = '200ms';

-- LRU scan depth (default: 128)
ALTER SYSTEM SET bgwriter_lru_maxpages = 100;

-- Multiplier for LRU scan (default: 2.0)
ALTER SYSTEM SET bgwriter_lru_multiplier = 2.0;

-- Flush after this many pages (default: 512kB)
ALTER SYSTEM SET bgwriter_flush_after = '512kB';

-- Reload configuration
SELECT pg_reload_conf();
```

**Monitor Background Writer**:

```sql
-- Check background writer efficiency
SELECT 
  buffers_clean,
  maxwritten_clean,
  buffers_alloc,
  round(100.0 * buffers_clean / NULLIF(buffers_alloc, 0), 2) AS clean_ratio
FROM pg_stat_bgwriter;
```

## Advanced Monitoring and Diagnostics

### pg_stat_statements Analysis and Optimization

**Enable and Configure pg_stat_statements**:

```sql
-- Check if extension is enabled
SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';

-- Enable extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Configure tracking parameters
ALTER SYSTEM SET pg_stat_statements.track = 'all';
ALTER SYSTEM SET pg_stat_statements.max = 10000;
ALTER SYSTEM SET pg_stat_statements.track_utility = on;
ALTER SYSTEM SET pg_stat_statements.track_planning = on;
SELECT pg_reload_conf();
```

**Analyze Query Performance Patterns**:

```sql
-- Top 20 queries by total execution time
SELECT 
  substring(query, 1, 80) AS short_query,
  calls,
  round(total_exec_time::numeric, 2) AS total_time_ms,
  round(mean_exec_time::numeric, 2) AS avg_time_ms,
  round(min_exec_time::numeric, 2) AS min_time_ms,
  round(max_exec_time::numeric, 2) AS max_time_ms,
  round(stddev_exec_time::numeric, 2) AS stddev_time_ms,
  round((100 * total_exec_time / sum(total_exec_time) OVER ())::numeric, 2) AS percent_total
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;

-- Queries with high variability (potential optimization candidates)
SELECT 
  substring(query, 1, 80) AS short_query,
  calls,
  round(mean_exec_time::numeric, 2) AS avg_time_ms,
  round(stddev_exec_time::numeric, 2) AS stddev_time_ms,
  round((stddev_exec_time / NULLIF(mean_exec_time, 0))::numeric, 2) AS coefficient_of_variation
FROM pg_stat_statements
WHERE calls > 100
ORDER BY (stddev_exec_time / NULLIF(mean_exec_time, 0)) DESC
LIMIT 20;

-- Queries with most buffer cache misses
SELECT 
  substring(query, 1, 80) AS short_query,
  calls,
  shared_blks_hit,
  shared_blks_read,
  round(100.0 * shared_blks_hit / NULLIF(shared_blks_hit + shared_blks_read, 0), 2) AS cache_hit_ratio,
  shared_blks_written,
  temp_blks_written
FROM pg_stat_statements
WHERE shared_blks_read > 0
ORDER BY shared_blks_read DESC
LIMIT 20;

-- Reset statistics (use carefully)
SELECT pg_stat_statements_reset();
```

### Slow Query Log Analysis

**Configure Slow Query Logging**:

```sql
-- Enable slow query logging
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- Log queries > 1 second
ALTER SYSTEM SET log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h ';
ALTER SYSTEM SET log_checkpoints = on;
ALTER SYSTEM SET log_connections = on;
ALTER SYSTEM SET log_disconnections = on;
ALTER SYSTEM SET log_lock_waits = on;
ALTER SYSTEM SET log_temp_files = 0;  -- Log all temp file usage
ALTER SYSTEM SET log_autovacuum_min_duration = 0;  -- Log all autovacuum activity
SELECT pg_reload_conf();
```

**Analyze Slow Query Logs**:

```bash
#!/bin/bash
# Script: analyze-slow-queries.sh

LOG_FILE="/var/log/postgresql/postgresql.log"
OUTPUT_DIR="/var/log/postgresql/analysis"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p $OUTPUT_DIR

# Extract slow queries
grep "duration:" $LOG_FILE | \
  awk '{print $NF, $0}' | \
  sort -rn | \
  head -100 > "$OUTPUT_DIR/slow_queries_$TIMESTAMP.txt"

# Analyze query patterns
echo "=== Top 10 Slowest Queries ===" > "$OUTPUT_DIR/analysis_$TIMESTAMP.txt"
grep "duration:" $LOG_FILE | \
  sed 's/.*duration: \([0-9.]*\) ms.*/\1/' | \
  sort -rn | \
  head -10 >> "$OUTPUT_DIR/analysis_$TIMESTAMP.txt"

# Count queries by duration range
echo -e "\n=== Query Duration Distribution ===" >> "$OUTPUT_DIR/analysis_$TIMESTAMP.txt"
echo "1-5 seconds: $(grep 'duration:' $LOG_FILE | awk '$NF ~ /^[1-4][0-9]{3}\.[0-9]+ ms$/' | wc -l)" >> "$OUTPUT_DIR/analysis_$TIMESTAMP.txt"
echo "5-10 seconds: $(grep 'duration:' $LOG_FILE | awk '$NF ~ /^[5-9][0-9]{3}\.[0-9]+ ms$/' | wc -l)" >> "$OUTPUT_DIR/analysis_$TIMESTAMP.txt"
echo ">10 seconds: $(grep 'duration:' $LOG_FILE | awk '$NF ~ /^[1-9][0-9]{4,}\.[0-9]+ ms$/' | wc -l)" >> "$OUTPUT_DIR/analysis_$TIMESTAMP.txt"

echo "Slow query analysis completed: $OUTPUT_DIR/analysis_$TIMESTAMP.txt"
```

**Use pgBadger for Log Analysis**:

```bash
# Install pgBadger
sudo apt-get install pgbadger

# Generate HTML report from PostgreSQL logs
pgbadger -f stderr \
  --prefix '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h ' \
  /var/log/postgresql/postgresql.log \
  -o /var/www/html/pgbadger_report.html

# Generate incremental reports
pgbadger -f stderr \
  --incremental \
  --outdir /var/www/html/pgbadger/ \
  /var/log/postgresql/postgresql-*.log
```

### Lock Monitoring and Deadlock Analysis

**Monitor Active Locks**:

```sql
-- View all current locks
SELECT 
  l.locktype,
  l.database,
  l.relation::regclass AS table_name,
  l.page,
  l.tuple,
  l.virtualxid,
  l.transactionid,
  l.mode,
  l.granted,
  a.usename,
  a.query,
  a.query_start,
  age(now(), a.query_start) AS query_age
FROM pg_locks l
LEFT JOIN pg_stat_activity a ON l.pid = a.pid
WHERE NOT l.granted
ORDER BY a.query_start;

-- Find blocking queries
SELECT 
  blocked_locks.pid AS blocked_pid,
  blocked_activity.usename AS blocked_user,
  blocking_locks.pid AS blocking_pid,
  blocking_activity.usename AS blocking_user,
  blocked_activity.query AS blocked_statement,
  blocking_activity.query AS blocking_statement,
  blocked_activity.application_name AS blocked_application,
  blocking_activity.application_name AS blocking_application
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks 
  ON blocking_locks.locktype = blocked_locks.locktype
  AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
  AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
  AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
  AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
  AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
  AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
  AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
  AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
  AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
  AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- Kill blocking query (use with caution)
SELECT pg_terminate_backend(blocking_pid);
```

**Deadlock Detection and Analysis**:

```sql
-- Configure deadlock detection
ALTER SYSTEM SET deadlock_timeout = '1s';
ALTER SYSTEM SET log_lock_waits = on;
SELECT pg_reload_conf();

-- Analyze deadlock logs
-- Check PostgreSQL logs for "deadlock detected" messages
```

**Deadlock Analysis Script**:

```bash
#!/bin/bash
# Script: analyze-deadlocks.sh

LOG_FILE="/var/log/postgresql/postgresql.log"
OUTPUT_FILE="/var/log/postgresql/deadlock_analysis.txt"

echo "=== Deadlock Analysis Report ===" > $OUTPUT_FILE
echo "Generated: $(date)" >> $OUTPUT_FILE
echo "" >> $OUTPUT_FILE

# Count deadlocks
DEADLOCK_COUNT=$(grep -c "deadlock detected" $LOG_FILE)
echo "Total deadlocks detected: $DEADLOCK_COUNT" >> $OUTPUT_FILE
echo "" >> $OUTPUT_FILE

# Extract deadlock details
echo "=== Recent Deadlocks ===" >> $OUTPUT_FILE
grep -A 20 "deadlock detected" $LOG_FILE | tail -100 >> $OUTPUT_FILE

echo "Deadlock analysis completed: $OUTPUT_FILE"
```

### Table and Index Bloat Detection

**Detect Table Bloat**:

```sql
-- Estimate table bloat
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
  n_live_tup,
  n_dead_tup,
  round(100.0 * n_dead_tup / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_ratio,
  last_vacuum,
  last_autovacuum
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC
LIMIT 20;

-- Detailed bloat estimation
SELECT 
  schemaname || '.' || tablename AS table_name,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS indexes_size,
  round(100.0 * (pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) / 
    NULLIF(pg_total_relation_size(schemaname||'.'||tablename), 0), 2) AS index_ratio
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
```

**Detect Index Bloat**:

```sql
-- Identify bloated indexes
SELECT 
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch,
  CASE 
    WHEN idx_scan = 0 THEN 'UNUSED'
    WHEN idx_scan < 100 THEN 'RARELY USED'
    ELSE 'ACTIVE'
  END AS usage_status
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;

-- Find duplicate or redundant indexes
SELECT 
  pg_size_pretty(sum(pg_relation_size(idx))::bigint) AS total_size,
  (array_agg(idx))[1] AS index1,
  (array_agg(idx))[2] AS index2,
  (array_agg(idx))[3] AS index3,
  (array_agg(idx))[4] AS index4
FROM (
  SELECT 
    indexrelid::regclass AS idx,
    (indrelid::text || E'\n' || indclass::text || E'\n' || 
     indkey::text || E'\n' || COALESCE(indexprs::text, '') || E'\n' || 
     COALESCE(indpred::text, '')) AS key
  FROM pg_index
) sub
GROUP BY key
HAVING count(*) > 1
ORDER BY sum(pg_relation_size(idx)) DESC;
```

### Replication Monitoring and Lag Analysis

**Monitor Replication Status**:

```sql
-- Check replication slots
SELECT 
  slot_name,
  slot_type,
  database,
  active,
  pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS replication_lag_bytes
FROM pg_replication_slots;

-- Monitor replication lag
SELECT 
  client_addr,
  application_name,
  state,
  sync_state,
  pg_wal_lsn_diff(pg_current_wal_lsn(), sent_lsn) AS send_lag_bytes,
  pg_wal_lsn_diff(sent_lsn, write_lsn) AS write_lag_bytes,
  pg_wal_lsn_diff(write_lsn, flush_lsn) AS flush_lag_bytes,
  pg_wal_lsn_diff(flush_lsn, replay_lsn) AS replay_lag_bytes,
  pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) AS total_lag_bytes
FROM pg_stat_replication;

-- Check replication delay in time
SELECT 
  application_name,
  client_addr,
  state,
  sync_state,
  write_lag,
  flush_lag,
  replay_lag
FROM pg_stat_replication;
```

**Replication Monitoring Script**:

```bash
#!/bin/bash
# Script: monitor-replication.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
ALERT_THRESHOLD_MB=100

psql -h $DB_HOST -U admin -d $DB_NAME -t -A -F"," << EOF | while IFS=, read app_name lag_bytes state
SELECT 
  application_name,
  pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) AS lag_bytes,
  state
FROM pg_stat_replication;
EOF
do
  LAG_MB=$((lag_bytes / 1024 / 1024))
  
  if [ $LAG_MB -gt $ALERT_THRESHOLD_MB ]; then
    echo "ALERT: Replication lag for $app_name is ${LAG_MB}MB (threshold: ${ALERT_THRESHOLD_MB}MB)"
    # Send alert to monitoring system
  fi
done
```

### Connection Monitoring and Leak Detection

**Monitor Connection Usage**:

```sql
-- Current connection statistics
SELECT 
  count(*) AS total_connections,
  count(*) FILTER (WHERE state = 'active') AS active,
  count(*) FILTER (WHERE state = 'idle') AS idle,
  count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_transaction,
  count(*) FILTER (WHERE state = 'idle in transaction (aborted)') AS idle_in_transaction_aborted
FROM pg_stat_activity;

-- Connections by database
SELECT 
  datname,
  count(*) AS connections,
  count(*) FILTER (WHERE state = 'active') AS active,
  count(*) FILTER (WHERE state = 'idle') AS idle
FROM pg_stat_activity
GROUP BY datname
ORDER BY connections DESC;

-- Connections by application
SELECT 
  application_name,
  count(*) AS connections,
  count(*) FILTER (WHERE state = 'active') AS active,
  max(age(now(), query_start)) AS max_query_age
FROM pg_stat_activity
WHERE application_name IS NOT NULL
GROUP BY application_name
ORDER BY connections DESC;

-- Long-running queries
SELECT 
  pid,
  usename,
  application_name,
  client_addr,
  state,
  query_start,
  age(now(), query_start) AS query_age,
  substring(query, 1, 100) AS query
FROM pg_stat_activity
WHERE state = 'active'
  AND query_start < now() - interval '5 minutes'
ORDER BY query_start;

-- Idle in transaction (potential connection leaks)
SELECT 
  pid,
  usename,
  application_name,
  client_addr,
  state,
  state_change,
  age(now(), state_change) AS idle_age,
  substring(query, 1, 100) AS last_query
FROM pg_stat_activity
WHERE state LIKE 'idle in transaction%'
  AND state_change < now() - interval '5 minutes'
ORDER BY state_change;
```

**Connection Leak Detection Script**:

```bash
#!/bin/bash
# Script: detect-connection-leaks.sh

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
IDLE_THRESHOLD_MINUTES=10

psql -h $DB_HOST -U admin -d $DB_NAME << EOF
-- Find potential connection leaks
SELECT 
  'Connection Leak Alert' AS alert_type,
  pid,
  usename,
  application_name,
  client_addr,
  state,
  age(now(), state_change) AS idle_duration,
  substring(query, 1, 100) AS last_query
FROM pg_stat_activity
WHERE state LIKE 'idle in transaction%'
  AND state_change < now() - interval '$IDLE_THRESHOLD_MINUTES minutes'
ORDER BY state_change;

-- Terminate leaked connections (use with caution)
-- SELECT pg_terminate_backend(pid) FROM pg_stat_activity 
-- WHERE state = 'idle in transaction' 
-- AND state_change < now() - interval '30 minutes';
EOF
```

### Cache Hit Ratio Analysis

**Monitor Buffer Cache Performance**:

```sql
-- Overall cache hit ratio (target: >99%)
SELECT 
  sum(heap_blks_read) AS heap_read,
  sum(heap_blks_hit) AS heap_hit,
  round(100.0 * sum(heap_blks_hit) / NULLIF(sum(heap_blks_hit + heap_blks_read), 0), 2) AS cache_hit_ratio
FROM pg_statio_user_tables;

-- Cache hit ratio by table
SELECT 
  schemaname,
  tablename,
  heap_blks_read,
  heap_blks_hit,
  round(100.0 * heap_blks_hit / NULLIF(heap_blks_hit + heap_blks_read, 0), 2) AS cache_hit_ratio,
  idx_blks_read,
  idx_blks_hit,
  round(100.0 * idx_blks_hit / NULLIF(idx_blks_hit + idx_blks_read, 0), 2) AS index_cache_hit_ratio
FROM pg_statio_user_tables
WHERE heap_blks_read + heap_blks_hit > 0
ORDER BY heap_blks_read DESC
LIMIT 20;

-- Database-wide cache statistics
SELECT 
  datname,
  blks_read,
  blks_hit,
  round(100.0 * blks_hit / NULLIF(blks_hit + blks_read, 0), 2) AS cache_hit_ratio,
  tup_returned,
  tup_fetched,
  round(100.0 * tup_fetched / NULLIF(tup_returned, 0), 2) AS fetch_ratio
FROM pg_stat_database
WHERE datname = 'ecommerce';
```

**Identify Tables with Poor Cache Performance**:

```sql
-- Tables with low cache hit ratio
SELECT 
  schemaname || '.' || tablename AS table_name,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
  heap_blks_read,
  heap_blks_hit,
  round(100.0 * heap_blks_hit / NULLIF(heap_blks_hit + heap_blks_read, 0), 2) AS cache_hit_ratio
FROM pg_statio_user_tables
WHERE heap_blks_read > 1000
  AND round(100.0 * heap_blks_hit / NULLIF(heap_blks_hit + heap_blks_read, 0), 2) < 95
ORDER BY heap_blks_read DESC;
```

### Disk I/O and Throughput Monitoring

**Monitor Disk I/O Statistics**:

```sql
-- Table I/O statistics
SELECT 
  schemaname,
  tablename,
  heap_blks_read,
  heap_blks_hit,
  idx_blks_read,
  idx_blks_hit,
  toast_blks_read,
  toast_blks_hit,
  tidx_blks_read,
  tidx_blks_hit
FROM pg_statio_user_tables
WHERE heap_blks_read > 0
ORDER BY heap_blks_read DESC
LIMIT 20;

-- Index I/O statistics
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_blks_read,
  idx_blks_hit,
  round(100.0 * idx_blks_hit / NULLIF(idx_blks_hit + idx_blks_read, 0), 2) AS cache_hit_ratio
FROM pg_statio_user_indexes
WHERE idx_blks_read > 0
ORDER BY idx_blks_read DESC
LIMIT 20;

-- Temporary file usage (indicates insufficient work_mem)
SELECT 
  datname,
  temp_files,
  pg_size_pretty(temp_bytes) AS temp_size
FROM pg_stat_database
WHERE temp_files > 0
ORDER BY temp_bytes DESC;
```

**Monitor WAL I/O**:

```sql
-- WAL statistics
SELECT 
  pg_current_wal_lsn() AS current_wal_lsn,
  pg_walfile_name(pg_current_wal_lsn()) AS current_wal_file,
  pg_size_pretty(
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0')
  ) AS total_wal_generated;

-- WAL write rate
SELECT 
  'WAL Write Rate' AS metric,
  pg_size_pretty(
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') / 
    EXTRACT(epoch FROM (now() - pg_postmaster_start_time()))
  ) || '/sec' AS value;
```

### Query Plan Analysis and Optimization

**Analyze Query Execution Plans**:

```sql
-- Enable detailed query planning information
SET auto_explain.log_min_duration = 1000;  -- Log plans for queries > 1s
SET auto_explain.log_analyze = true;
SET auto_explain.log_buffers = true;
SET auto_explain.log_timing = true;
SET auto_explain.log_triggers = true;
SET auto_explain.log_verbose = true;

-- Analyze specific query
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, COSTS, TIMING)
SELECT o.*, c.name, c.email
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.order_date >= '2024-01-01'
  AND o.status = 'PENDING'
ORDER BY o.order_date DESC
LIMIT 100;

-- Compare different query plans
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 'CUST-001';

-- Force different plan with hints (if pg_hint_plan extension available)
/*+ SeqScan(orders) */
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 'CUST-001';
```

**Identify Suboptimal Query Plans**:

```sql
-- Queries with sequential scans on large tables
SELECT 
  schemaname,
  tablename,
  seq_scan,
  seq_tup_read,
  idx_scan,
  n_live_tup,
  round(100.0 * seq_scan / NULLIF(seq_scan + idx_scan, 0), 2) AS seq_scan_ratio
FROM pg_stat_user_tables
WHERE n_live_tup > 10000
  AND seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;

-- Queries using temp files (need more work_mem)
SELECT 
  substring(query, 1, 100) AS query,
  calls,
  temp_blks_written,
  pg_size_pretty(temp_blks_written * 8192) AS temp_size,
  round(mean_exec_time::numeric, 2) AS avg_time_ms
FROM pg_stat_statements
WHERE temp_blks_written > 0
ORDER BY temp_blks_written DESC
LIMIT 20;
```

**Query Plan Optimization Checklist**:

```sql
-- Check planner statistics are up to date
SELECT 
  schemaname,
  tablename,
  last_analyze,
  last_autoanalyze,
  n_mod_since_analyze
FROM pg_stat_user_tables
WHERE n_mod_since_analyze > 10000
ORDER BY n_mod_since_analyze DESC;

-- Update statistics if needed
ANALYZE VERBOSE;

-- Check for missing indexes
SELECT 
  schemaname,
  tablename,
  seq_scan,
  seq_tup_read,
  idx_scan,
  n_live_tup
FROM pg_stat_user_tables
WHERE seq_scan > 1000
  AND n_live_tup > 10000
  AND idx_scan < seq_scan
ORDER BY seq_tup_read DESC;
```

## Performance Tuning Checklist

### Initial Setup Checklist

- [ ] Set shared_buffers to 25% of RAM (up to 8-16GB)
- [ ] Set effective_cache_size to 50-75% of RAM
- [ ] Set work_mem based on workload (16-64MB)
- [ ] Set maintenance_work_mem to 256MB-2GB
- [ ] Set random_page_cost to 1.1 for SSD
- [ ] Set effective_io_concurrency to 200 for SSD
- [ ] Enable pg_stat_statements extension
- [ ] Configure connection pooling (HikariCP or pgBouncer)

### Query Optimization Checklist

- [ ] Identify slow queries using pg_stat_statements
- [ ] Analyze execution plans with EXPLAIN ANALYZE
- [ ] Create indexes for frequently queried columns
- [ ] Use covering indexes where appropriate
- [ ] Implement partial indexes for filtered queries
- [ ] Optimize JOIN operations
- [ ] Avoid SELECT * in production queries
- [ ] Use LIMIT for large result sets

### Maintenance Checklist

- [ ] Configure autovacuum appropriately
- [ ] Schedule manual VACUUM for high-churn tables
- [ ] Monitor dead tuple ratios
- [ ] Update statistics regularly with ANALYZE
- [ ] Rebuild bloated indexes
- [ ] Remove unused indexes
- [ ] Monitor table and index bloat

### Write Performance Checklist

- [ ] Tune checkpoint parameters for workload
- [ ] Configure WAL settings appropriately
- [ ] Consider synchronous_commit = off for non-critical data
- [ ] Monitor checkpoint frequency
- [ ] Optimize background writer settings
- [ ] Use COPY for bulk inserts
- [ ] Batch INSERT/UPDATE operations

### Monitoring Checklist

- [ ] Monitor cache hit ratio (target: >99%)
- [ ] Monitor connection pool utilization
- [ ] Track slow query trends
- [ ] Monitor checkpoint frequency
- [ ] Track WAL generation rate
- [ ] Monitor autovacuum activity
- [ ] Track table and index bloat
- [ ] Monitor replication lag (if applicable)

## Related Documentation

- [Operational Procedures](procedures.md) - Routine maintenance procedures
- [Monitoring and Alerting](monitoring-alerting.md) - Performance monitoring

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-22  
**Owner**: DBA Team, Operations Team  
**Change History**:

- 2025-01-22: Initial comprehensive PostgreSQL performance tuning guide
