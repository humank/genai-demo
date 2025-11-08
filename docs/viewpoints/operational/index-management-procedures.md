## Index Management Procedures

### Index Creation Best Practices

#### Creating Indexes Concurrently

**Purpose**: Create indexes without blocking writes to the table

**Best Practice - Use CONCURRENTLY**:

```sql
-- ✅ GOOD: Create index without blocking writes
CREATE INDEX CONCURRENTLY idx_orders_customer_id ON orders(customer_id);

-- ✅ GOOD: Create unique index concurrently
CREATE UNIQUE INDEX CONCURRENTLY idx_customers_email ON customers(email);

-- ✅ GOOD: Create partial index concurrently
CREATE INDEX CONCURRENTLY idx_active_orders 
ON orders(order_date) 
WHERE status = 'ACTIVE';

-- ❌ BAD: Blocks all writes during creation
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
```

**CONCURRENTLY Considerations**:

- Takes longer than regular index creation
- Requires two table scans instead of one
- Cannot be run inside a transaction block
- May fail if there are deadlocks or unique violations
- Requires more disk space during creation

**Handle CONCURRENTLY Failures**:

```sql
-- Check for invalid indexes after failed CONCURRENTLY operation
SELECT 
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_indexes i
JOIN pg_class c ON c.relname = i.indexname
WHERE NOT indisvalid
  AND schemaname NOT IN ('pg_catalog', 'information_schema');

-- Drop invalid index and retry
DROP INDEX CONCURRENTLY idx_orders_customer_id;
CREATE INDEX CONCURRENTLY idx_orders_customer_id ON orders(customer_id);
```

#### Index Creation Monitoring

**Monitor Index Creation Progress**:

```sql
-- Check active index creation operations
SELECT 
  pid,
  now() - query_start AS duration,
  query,
  state
FROM pg_stat_activity
WHERE query LIKE 'CREATE INDEX%'
  AND state = 'active';

-- Monitor index build progress (PostgreSQL 12+)
SELECT 
  p.phase,
  p.blocks_total,
  p.blocks_done,
  round(100.0 * p.blocks_done / NULLIF(p.blocks_total, 0), 2) AS percent_complete,
  p.tuples_total,
  p.tuples_done
FROM pg_stat_progress_create_index p
JOIN pg_stat_activity a ON p.pid = a.pid;
```

**Estimate Index Creation Time**:

```sql
-- Estimate time based on table size and system performance
SELECT 
  schemaname || '.' || tablename AS table_name,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS table_size,
  n_live_tup AS row_count,
  -- Rough estimate: 1-2 minutes per GB for CONCURRENTLY
  round((pg_total_relation_size(schemaname||'.'||tablename) / 1024.0 / 1024.0 / 1024.0) * 1.5) || ' minutes' AS estimated_time
FROM pg_stat_user_tables
WHERE schemaname || '.' || tablename = 'public.orders';
```

### Index Usage Analysis

#### Identify Missing Indexes

**Analyze Sequential Scans on Large Tables**:

```sql
-- Tables with high sequential scan activity (potential missing indexes)
SELECT 
  schemaname,
  tablename,
  seq_scan,
  seq_tup_read,
  idx_scan,
  n_live_tup,
  round(100.0 * seq_scan / NULLIF(seq_scan + idx_scan, 0), 2) AS seq_scan_pct,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS table_size
FROM pg_stat_user_tables
WHERE n_live_tup > 10000  -- Only large tables
  AND seq_scan > 100      -- Significant sequential scans
  AND seq_scan > idx_scan -- More seq scans than index scans
ORDER BY seq_tup_read DESC
LIMIT 20;
```

**Analyze Query Patterns for Missing Indexes**:

```sql
-- Queries that might benefit from indexes (using pg_stat_statements)
SELECT 
  substring(query, 1, 100) AS query_snippet,
  calls,
  round(total_exec_time::numeric, 2) AS total_time_ms,
  round(mean_exec_time::numeric, 2) AS avg_time_ms,
  shared_blks_read,
  shared_blks_hit,
  round(100.0 * shared_blks_hit / NULLIF(shared_blks_hit + shared_blks_read, 0), 2) AS cache_hit_ratio
FROM pg_stat_statements
WHERE shared_blks_read > 1000  -- High disk reads
  AND calls > 100              -- Frequently executed
ORDER BY shared_blks_read DESC
LIMIT 20;
```

**Suggest Indexes Based on WHERE Clauses**:

```sql
-- Analyze slow queries for potential index candidates
-- This requires manual analysis of EXPLAIN output
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM orders 
WHERE customer_id = 'CUST-001' 
  AND order_date >= '2024-01-01'
  AND status = 'PENDING';

-- Look for:
-- 1. Seq Scan on large tables
-- 2. High "Buffers: shared read" values
-- 3. Filter conditions removing many rows
```

#### Identify Unused Indexes

**Find Indexes Never Used**:

```sql
-- Indexes with zero scans (candidates for removal)
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS table_size
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'  -- Exclude primary keys
  AND indexname NOT LIKE '%_fkey'  -- Exclude foreign keys
ORDER BY pg_relation_size(indexrelid) DESC;
```

**Find Rarely Used Indexes**:

```sql
-- Indexes with very low usage (< 100 scans)
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
  round(pg_relation_size(indexrelid)::numeric / NULLIF(idx_scan, 0), 2) AS bytes_per_scan
FROM pg_stat_user_indexes
WHERE idx_scan > 0 
  AND idx_scan < 100
  AND indexname NOT LIKE '%_pkey'
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;
```

**Analyze Index Usage Over Time**:

```bash
#!/bin/bash
# Script: track-index-usage.sh
# Schedule: Daily to track index usage trends

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
OUTPUT_DIR="/var/log/postgresql/index-usage"
TIMESTAMP=$(date +%Y%m%d)

mkdir -p $OUTPUT_DIR

psql -h $DB_HOST -U admin -d $DB_NAME -o "$OUTPUT_DIR/index_usage_$TIMESTAMP.csv" << EOF
COPY (
  SELECT 
    current_date AS snapshot_date,
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_relation_size(indexrelid) AS index_size_bytes
  FROM pg_stat_user_indexes
  ORDER BY schemaname, tablename, indexname
) TO STDOUT WITH CSV HEADER;
EOF

echo "Index usage snapshot saved: $OUTPUT_DIR/index_usage_$TIMESTAMP.csv"
```

#### Identify Duplicate and Redundant Indexes

**Find Exact Duplicate Indexes**:

```sql
-- Indexes with identical definitions
SELECT 
  pg_size_pretty(sum(pg_relation_size(idx))::bigint) AS total_wasted_size,
  (array_agg(idx))[1] AS index1,
  (array_agg(idx))[2] AS index2,
  (array_agg(idx))[3] AS index3,
  (array_agg(idx))[4] AS index4
FROM (
  SELECT 
    indexrelid::regclass AS idx,
    (indrelid::text || E'\n' || 
     indclass::text || E'\n' || 
     indkey::text || E'\n' || 
     COALESCE(indexprs::text, '') || E'\n' || 
     COALESCE(indpred::text, '')) AS key
  FROM pg_index
) sub
GROUP BY key
HAVING count(*) > 1
ORDER BY sum(pg_relation_size(idx)) DESC;
```

**Find Redundant Indexes (Left-Prefix)**:

```sql
-- Indexes where one is a left-prefix of another
-- Example: idx(a,b,c) makes idx(a) and idx(a,b) redundant
SELECT 
  i1.indexrelid::regclass AS redundant_index,
  i2.indexrelid::regclass AS covering_index,
  pg_size_pretty(pg_relation_size(i1.indexrelid)) AS redundant_size,
  i1.indkey AS redundant_columns,
  i2.indkey AS covering_columns
FROM pg_index i1
JOIN pg_index i2 ON i1.indrelid = i2.indrelid
WHERE i1.indexrelid <> i2.indexrelid
  AND i1.indkey::text LIKE i2.indkey::text || '%'
  AND i1.indisunique = false
  AND i2.indisunique = false
ORDER BY pg_relation_size(i1.indexrelid) DESC;
```

### Index Maintenance Procedures

#### REINDEX Operations

**When to REINDEX**:

- Index bloat detected (>30% bloat ratio)
- After bulk data modifications
- Corrupted index (rare)
- Performance degradation over time

**REINDEX Methods**:

```sql
-- Method 1: REINDEX CONCURRENTLY (PostgreSQL 12+, no downtime)
REINDEX INDEX CONCURRENTLY idx_orders_customer_id;

-- Method 2: REINDEX TABLE CONCURRENTLY (all indexes on table)
REINDEX TABLE CONCURRENTLY orders;

-- Method 3: Manual rebuild (more control, no downtime)
CREATE INDEX CONCURRENTLY idx_orders_customer_id_new ON orders(customer_id);
DROP INDEX CONCURRENTLY idx_orders_customer_id;
ALTER INDEX idx_orders_customer_id_new RENAME TO idx_orders_customer_id;

-- Method 4: REINDEX without CONCURRENTLY (requires downtime, faster)
REINDEX INDEX idx_orders_customer_id;
```

**Detect Index Bloat**:

```sql
-- Estimate index bloat
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
    WHEN pg_relation_size(indexrelid) > 100 * 1024 * 1024 THEN 'LARGE'
    ELSE 'NORMAL'
  END AS status
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;

-- More accurate bloat estimation (requires pgstattuple extension)
CREATE EXTENSION IF NOT EXISTS pgstattuple;

SELECT 
  indexrelname AS index_name,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
  round(100.0 * (pg_relation_size(indexrelid) - 
    (SELECT (avg_leaf_density/100) * pg_relation_size(indexrelid) 
     FROM pgstatindex(indexrelid::regclass::text))) / 
    NULLIF(pg_relation_size(indexrelid), 0), 2) AS bloat_pct
FROM pg_stat_user_indexes
WHERE pg_relation_size(indexrelid) > 10 * 1024 * 1024  -- > 10MB
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;
```

**Scheduled REINDEX Script**:

```bash
#!/bin/bash
# Script: scheduled-reindex.sh
# Schedule: Monthly during maintenance window

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
BLOAT_THRESHOLD=30  # Reindex if bloat > 30%

# Create backup before REINDEX
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier pre-reindex-$(date +%Y%m%d)

# Identify bloated indexes
psql -h $DB_HOST -U admin -d $DB_NAME -t -A -F"," << EOF | while IFS=, read schema table index size bloat
SELECT 
  schemaname,
  tablename,
  indexname,
  pg_relation_size(indexrelid),
  30  -- Placeholder for bloat percentage
FROM pg_stat_user_indexes
WHERE pg_relation_size(indexrelid) > 100 * 1024 * 1024  -- > 100MB
ORDER BY pg_relation_size(indexrelid) DESC;
EOF
do
  if [ "$bloat" -gt "$BLOAT_THRESHOLD" ]; then
    echo "Reindexing $schema.$table.$index (bloat: ${bloat}%)"
    psql -h $DB_HOST -U admin -d $DB_NAME << REINDEX_SQL
REINDEX INDEX CONCURRENTLY $schema.$index;
REINDEX_SQL
  fi
done

echo "Scheduled reindex completed at $(date)"
```

#### Index Size Monitoring

**Monitor Index Growth**:

```sql
-- Track index sizes over time
SELECT 
  schemaname,
  tablename,
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) AS current_size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS table_total_size,
  round(100.0 * pg_relation_size(indexrelid) / 
    NULLIF(pg_total_relation_size(schemaname||'.'||tablename), 0), 2) AS index_ratio
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;
```

**Index Size Alerts**:

```sql
-- Identify indexes consuming excessive space
SELECT 
  schemaname || '.' || tablename AS table_name,
  indexname,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename::regclass)) AS table_size,
  round(pg_relation_size(indexrelid)::numeric / 
    NULLIF(pg_relation_size(schemaname||'.'||tablename::regclass), 0), 2) AS index_to_table_ratio
FROM pg_stat_user_indexes
WHERE pg_relation_size(indexrelid) > pg_relation_size(schemaname||'.'||tablename::regclass)
ORDER BY pg_relation_size(indexrelid) DESC;
```

### Partial Index Strategies

#### When to Use Partial Indexes

**Use Cases**:

- Filtering on status columns (e.g., active records only)
- Date range queries (e.g., recent orders only)
- Boolean flags (e.g., is_deleted = false)
- Sparse data (e.g., non-null values only)

**Partial Index Examples**:

```sql
-- Index only active customers
CREATE INDEX CONCURRENTLY idx_active_customers_email 
ON customers(email) 
WHERE status = 'ACTIVE';

-- Index only recent orders (last 90 days)
CREATE INDEX CONCURRENTLY idx_recent_orders_date 
ON orders(order_date) 
WHERE order_date >= CURRENT_DATE - INTERVAL '90 days';

-- Index only non-deleted records
CREATE INDEX CONCURRENTLY idx_products_category 
ON products(category) 
WHERE deleted_at IS NULL;

-- Index only pending and processing orders
CREATE INDEX CONCURRENTLY idx_active_orders_status 
ON orders(customer_id, order_date) 
WHERE status IN ('PENDING', 'PROCESSING');

-- Index only high-value orders
CREATE INDEX CONCURRENTLY idx_high_value_orders 
ON orders(customer_id, order_date) 
WHERE total_amount > 1000;
```

**Partial Index Benefits**:

- Smaller index size (faster scans, less storage)
- Faster index maintenance (fewer entries to update)
- Better cache utilization
- Reduced write overhead

**Verify Partial Index Usage**:

```sql
-- Check if query uses partial index
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM customers 
WHERE status = 'ACTIVE' 
  AND email = 'customer@example.com';

-- Should show: Index Scan using idx_active_customers_email
```

### Covering Index Optimization

#### Understanding Covering Indexes

**Purpose**: Include additional columns in index to satisfy queries without table access (Index-Only Scan)

**Covering Index Syntax**:

```sql
-- Include additional columns in index
CREATE INDEX CONCURRENTLY idx_orders_customer_covering 
ON orders(customer_id) 
INCLUDE (order_date, total_amount, status);

-- Query satisfied by index alone
SELECT order_date, total_amount, status 
FROM orders 
WHERE customer_id = 'CUST-001';
```

**Covering Index Examples**:

```sql
-- Cover common query patterns
CREATE INDEX CONCURRENTLY idx_products_category_covering 
ON products(category) 
INCLUDE (name, price, stock_quantity);

-- Cover JOIN queries
CREATE INDEX CONCURRENTLY idx_order_items_covering 
ON order_items(order_id) 
INCLUDE (product_id, quantity, unit_price);

-- Cover aggregation queries
CREATE INDEX CONCURRENTLY idx_orders_customer_aggregation 
ON orders(customer_id, order_date) 
INCLUDE (total_amount);
```

**Verify Index-Only Scan**:

```sql
-- Check if query uses Index-Only Scan
EXPLAIN (ANALYZE, BUFFERS)
SELECT order_date, total_amount, status 
FROM orders 
WHERE customer_id = 'CUST-001';

-- Should show: Index Only Scan using idx_orders_customer_covering
-- Heap Fetches: 0 (indicates no table access needed)
```

**Covering Index Trade-offs**:

- **Pros**: Faster queries (no table access), better cache utilization
- **Cons**: Larger index size, slower writes, more storage

**When to Use Covering Indexes**:

- Frequently executed queries with specific column access patterns
- Queries that would otherwise require table lookups
- Read-heavy workloads where query performance is critical
- When index size increase is acceptable

### Index Rebuild Scheduling

#### Maintenance Window Planning

**Determine Optimal Maintenance Windows**:

```sql
-- Analyze query patterns by hour
SELECT 
  extract(hour from query_start) AS hour,
  count(*) AS query_count,
  round(avg(extract(epoch from (now() - query_start))), 2) AS avg_duration_sec
FROM pg_stat_activity
WHERE query_start IS NOT NULL
  AND state = 'active'
GROUP BY extract(hour from query_start)
ORDER BY hour;

-- Identify low-traffic periods
SELECT 
  extract(dow from query_start) AS day_of_week,
  extract(hour from query_start) AS hour,
  count(*) AS query_count
FROM pg_stat_activity
WHERE query_start >= now() - interval '7 days'
GROUP BY extract(dow from query_start), extract(hour from query_start)
ORDER BY query_count ASC
LIMIT 20;
```

**Scheduled Index Maintenance Script**:

```bash
#!/bin/bash
# Script: weekly-index-maintenance.sh
# Schedule: Weekly during low-traffic window (Sunday 2:00 AM)

DB_HOST="ecommerce-prod-db.xxx.rds.amazonaws.com"
DB_NAME="ecommerce"
LOG_FILE="/var/log/postgresql/index-maintenance-$(date +%Y%m%d).log"

echo "=== Index Maintenance Started: $(date) ===" | tee -a $LOG_FILE

# Step 1: Identify and remove unused indexes
echo "Step 1: Checking for unused indexes..." | tee -a $LOG_FILE
psql -h $DB_HOST -U admin -d $DB_NAME << EOF | tee -a $LOG_FILE
SELECT 
  'Unused Index: ' || schemaname || '.' || indexname AS message,
  pg_size_pretty(pg_relation_size(indexrelid)) AS size,
  'DROP INDEX CONCURRENTLY ' || schemaname || '.' || indexname || ';' AS drop_command
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'
  AND pg_relation_size(indexrelid) > 10 * 1024 * 1024  -- > 10MB
ORDER BY pg_relation_size(indexrelid) DESC;
EOF

# Step 2: Reindex bloated indexes
echo "Step 2: Reindexing bloated indexes..." | tee -a $LOG_FILE
psql -h $DB_HOST -U admin -d $DB_NAME << EOF | tee -a $LOG_FILE
-- Reindex large indexes (>100MB)
DO \$\$
DECLARE
  idx_record RECORD;
BEGIN
  FOR idx_record IN 
    SELECT schemaname, indexname
    FROM pg_stat_user_indexes
    WHERE pg_relation_size(indexrelid) > 100 * 1024 * 1024
    ORDER BY pg_relation_size(indexrelid) DESC
    LIMIT 5
  LOOP
    RAISE NOTICE 'Reindexing %.%', idx_record.schemaname, idx_record.indexname;
    EXECUTE format('REINDEX INDEX CONCURRENTLY %I.%I', 
                   idx_record.schemaname, idx_record.indexname);
  END LOOP;
END \$\$;
EOF

# Step 3: Update statistics
echo "Step 3: Updating statistics..." | tee -a $LOG_FILE
psql -h $DB_HOST -U admin -d $DB_NAME << EOF | tee -a $LOG_FILE
ANALYZE VERBOSE;
EOF

# Step 4: Generate index usage report
echo "Step 4: Generating index usage report..." | tee -a $LOG_FILE
psql -h $DB_HOST -U admin -d $DB_NAME << EOF | tee -a $LOG_FILE
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;
EOF

echo "=== Index Maintenance Completed: $(date) ===" | tee -a $LOG_FILE
```

### Index Performance Impact Analysis

#### Measure Index Impact on Writes

**Analyze Write Performance**:

```sql
-- Check index write overhead
SELECT 
  schemaname,
  tablename,
  count(*) AS index_count,
  pg_size_pretty(sum(pg_relation_size(indexrelid))) AS total_index_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename::regclass)) AS table_size
FROM pg_stat_user_indexes
GROUP BY schemaname, tablename, schemaname||'.'||tablename::regclass
HAVING count(*) > 5  -- Tables with many indexes
ORDER BY count(*) DESC;
```

**Benchmark Index Impact**:

```sql
-- Test write performance with and without indexes
BEGIN;

-- Measure INSERT performance
EXPLAIN (ANALYZE, BUFFERS)
INSERT INTO orders (customer_id, order_date, total_amount, status)
SELECT 
  'CUST-' || (random() * 10000)::int,
  CURRENT_DATE - (random() * 365)::int,
  (random() * 1000)::numeric(10,2),
  'PENDING'
FROM generate_series(1, 1000);

ROLLBACK;
```

#### Analyze Index Scan Performance

**Compare Index Scan vs Sequential Scan**:

```sql
-- Force sequential scan
SET enable_indexscan = off;
SET enable_bitmapscan = off;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 'CUST-001';

-- Reset and use index scan
RESET enable_indexscan;
RESET enable_bitmapscan;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 'CUST-001';
```

**Index Selectivity Analysis**:

```sql
-- Check index selectivity (how well it filters data)
SELECT 
  schemaname,
  tablename,
  attname AS column_name,
  n_distinct,
  correlation,
  most_common_vals,
  most_common_freqs
FROM pg_stats
WHERE schemaname = 'public'
  AND tablename = 'orders'
  AND attname IN ('customer_id', 'status', 'order_date')
ORDER BY abs(correlation) DESC;

-- High correlation (close to 1 or -1) = good for index scans
-- Low n_distinct = poor index candidate
```

## Index Management Checklist

### Index Creation Checklist

- [ ] Use CREATE INDEX CONCURRENTLY for production databases
- [ ] Monitor index creation progress for large tables
- [ ] Verify index is valid after CONCURRENTLY operation
- [ ] Update statistics after index creation (ANALYZE)
- [ ] Test query performance with new index
- [ ] Document index purpose and expected usage

### Index Maintenance Checklist

- [ ] Monitor index usage weekly
- [ ] Identify and remove unused indexes monthly
- [ ] Check for duplicate/redundant indexes monthly
- [ ] Reindex bloated indexes during maintenance windows
- [ ] Track index size growth trends
- [ ] Update index statistics regularly (ANALYZE)
- [ ] Review partial index effectiveness quarterly

### Index Optimization Checklist

- [ ] Analyze query patterns for missing indexes
- [ ] Consider partial indexes for filtered queries
- [ ] Use covering indexes for frequently accessed columns
- [ ] Optimize composite index column order
- [ ] Balance read performance vs write overhead
- [ ] Monitor index scan vs sequential scan ratios
- [ ] Review index selectivity and correlation

### Index Performance Checklist

- [ ] Measure index impact on write operations
- [ ] Verify Index-Only Scans for covering indexes
- [ ] Check index bloat ratios monthly
- [ ] Monitor index cache hit ratios
- [ ] Analyze index scan performance
- [ ] Review index rebuild schedules
- [ ] Document index performance baselines
