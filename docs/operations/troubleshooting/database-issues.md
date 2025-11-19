# Database Troubleshooting Guide

## Overview

This document provides comprehensive troubleshooting procedures for database-related issues in the Enterprise E-Commerce Platform. It covers query performance analysis, connection pool management, deadlock resolution, and advanced PostgreSQL diagnostics.

**Target Audience**: Database administrators, DevOps engineers, backend developers  
**Prerequisites**: Access to database, kubectl, psql client  
**Related Documents**:

- [Database Maintenance](../maintenance/database-maintenance.md)
- [Slow API Responses Runbook](../runbooks/slow-api-responses.md)

---

## Table of Contents

1. [Query Performance Analysis](#query-performance-analysis)
2. [Connection Pool Exhaustion](#connection-pool-exhaustion)
3. [Deadlock Detection and Resolution](#deadlock-detection-and-resolution)
4. [Lock Contention Analysis](#lock-contention-analysis)
5. [Transaction Isolation Issues](#transaction-isolation-issues)
6. [Replication Lag Troubleshooting](#replication-lag-troubleshooting)
7. [Database Parameter Tuning](#database-parameter-tuning)
8. [pg_stat_statements Analysis](#pg_stat_statements-analysis)
9. [EXPLAIN ANALYZE Interpretation](#explain-analyze-interpretation)

---

## Query Performance Analysis

### Overview

Query performance issues are the most common cause of application slowdowns. This section provides systematic procedures for identifying and resolving slow queries.

### Symptoms

- API response times > 2 seconds
- Database CPU usage > 70%
- High I/O wait times
- Application timeouts
- User complaints about slow page loads

### Diagnostic Procedures

#### Step 1: Identify Slow Queries

**Using pg_stat_statements** (recommended):

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
ORDER BY mean_time DESC
LIMIT 10;
```

**Using pg_stat_activity** (real-time):

```sql
-- Currently running slow queries
SELECT 
    pid,
    now() - query_start AS duration,
    state,
    query
FROM pg_stat_activity
WHERE state != 'idle'
    AND query_start < now() - interval '5 seconds'
ORDER BY duration DESC;
```

**Using application logs**:

```bash
# Find slow queries in application logs
kubectl logs -l app=ecommerce-backend -n production | \
    grep "SlowQuery" | \
    awk '{print $NF}' | \
    sort | uniq -c | sort -rn | head -20
```

#### Step 2: Analyze Query Execution Plan

```sql
-- Get detailed execution plan
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING) 
SELECT * FROM orders 
WHERE customer_id = 'CUST-123' 
    AND order_date > '2025-01-01';
```

**Key metrics to check**:

- **Execution Time**: Total time vs. planning time
- **Rows**: Estimated vs. actual rows
- **Buffers**: Shared hits vs. reads (cache efficiency)
- **Scan Type**: Sequential scan vs. index scan

#### Step 3: Check Missing Indexes

```sql
-- Find tables with sequential scans
SELECT 
    schemaname,
    tablename,
    seq_scan,
    seq_tup_read,
    idx_scan,
    seq_tup_read / seq_scan AS avg_seq_tup_read
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;

-- Find unused indexes (candidates for removal)
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE idx_scan = 0
    AND indexname NOT LIKE '%_pkey'
ORDER BY pg_relation_size(indexrelid) DESC;
```

#### Step 4: Analyze Table Statistics

```sql
-- Check if statistics are up to date
SELECT 
    schemaname,
    tablename,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze,
    n_live_tup,
    n_dead_tup
FROM pg_stat_user_tables
WHERE last_analyze < now() - interval '7 days'
    OR last_autoanalyze < now() - interval '7 days'
ORDER BY n_live_tup DESC;
```

### Resolution Strategies

#### Strategy 1: Add Missing Indexes

```sql
-- Example: Add index for frequently filtered column
CREATE INDEX CONCURRENTLY idx_orders_customer_date 
ON orders(customer_id, order_date);

-- Composite index for common query patterns
CREATE INDEX CONCURRENTLY idx_orders_status_date 
ON orders(status, order_date) 
WHERE status IN ('PENDING', 'PROCESSING');
```

**Best Practices**:

- Use `CONCURRENTLY` to avoid locking
- Create indexes during low-traffic periods
- Monitor index size and usage
- Consider partial indexes for filtered queries

#### Strategy 2: Optimize Query Structure

**Before** (inefficient):

```sql
SELECT * FROM orders o
WHERE o.customer_id IN (
    SELECT customer_id FROM customers WHERE region = 'US'
);
```

**After** (optimized):

```sql
SELECT o.* FROM orders o
INNER JOIN customers c ON o.customer_id = c.customer_id
WHERE c.region = 'US';
```

#### Strategy 3: Update Table Statistics

```sql
-- Analyze specific table
ANALYZE orders;

-- Analyze entire database
ANALYZE;

-- Vacuum and analyze
VACUUM ANALYZE orders;
```

#### Strategy 4: Query Rewriting

**Use LIMIT for pagination**:

```sql
-- Instead of fetching all rows
SELECT * FROM orders 
WHERE customer_id = 'CUST-123'
ORDER BY order_date DESC
LIMIT 20 OFFSET 0;
```

**Use EXISTS instead of COUNT**:

```sql
-- Instead of: SELECT COUNT(*) > 0
SELECT EXISTS(
    SELECT 1 FROM orders 
    WHERE customer_id = 'CUST-123'
);
```

### Monitoring and Prevention

```sql
-- Create monitoring view for slow queries
CREATE OR REPLACE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_time / 1000 AS total_seconds,
    mean_time / 1000 AS mean_seconds,
    max_time / 1000 AS max_seconds
FROM pg_stat_statements
WHERE mean_time > 1000  -- > 1 second
ORDER BY mean_time DESC;
```

**Set up alerts**:

```yaml
# Prometheus alert rule

- alert: SlowDatabaseQueries

  expr: pg_stat_statements_mean_time_seconds > 1
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Slow database queries detected"
    description: "Query {{ $labels.query }} has mean time {{ $value }}s"
```

---

## Connection Pool Exhaustion

### Overview

Connection pool exhaustion occurs when all available database connections are in use, preventing new requests from being processed.

### Symptoms

- "Too many connections" errors
- "Connection pool exhausted" exceptions
- Application timeouts
- HikariCP warnings in logs
- Increased connection wait times

### Root Cause Analysis

#### Step 1: Check Current Connection Usage

```sql
-- Total connections by state
SELECT 
    state,
    count(*) as connections
FROM pg_stat_activity
GROUP BY state
ORDER BY connections DESC;

-- Connections by application
SELECT 
    application_name,
    state,
    count(*) as connections
FROM pg_stat_activity
WHERE application_name != ''
GROUP BY application_name, state
ORDER BY connections DESC;

-- Long-running connections
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    now() - state_change AS duration,
    query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

#### Step 2: Check Connection Pool Configuration

```bash
# Check HikariCP configuration
kubectl exec -it ${POD_NAME} -n production -- \
    curl http://localhost:8080/actuator/metrics/hikaricp.connections.active

# Check pool metrics
kubectl exec -it ${POD_NAME} -n production -- \
    curl http://localhost:8080/actuator/metrics/hikaricp.connections | jq
```

#### Step 3: Identify Connection Leaks

```sql
-- Find idle connections holding locks
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    now() - state_change AS idle_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
    AND now() - state_change > interval '5 minutes'
ORDER BY idle_duration DESC;

-- Check for connections with long-held locks
SELECT 
    l.pid,
    l.mode,
    l.granted,
    a.usename,
    a.query,
    now() - a.query_start AS duration
FROM pg_locks l
JOIN pg_stat_activity a ON l.pid = a.pid
WHERE NOT l.granted
    OR now() - a.query_start > interval '1 minute'
ORDER BY duration DESC;
```

#### Step 4: Analyze Connection Patterns

```bash
# Check connection pool metrics over time
kubectl exec -it ${POD_NAME} -n production -- \
    curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending

# Monitor connection acquisition time
kubectl exec -it ${POD_NAME} -n production -- \
    curl http://localhost:8080/actuator/metrics/hikaricp.connections.acquire
```

### Resolution Strategies

#### Immediate Actions (Emergency)

```sql
-- Kill idle connections (use with caution)
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle'
    AND now() - state_change > interval '10 minutes'
    AND pid != pg_backend_pid();

-- Kill specific problematic connection
SELECT pg_terminate_backend(12345);  -- Replace with actual PID

-- Cancel long-running query (gentler than terminate)
SELECT pg_cancel_backend(12345);
```

#### Short-term Fix

```bash
# Increase connection pool size temporarily
kubectl set env deployment/ecommerce-backend \
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30 \
    -n production

# Restart application to reset connections
kubectl rollout restart deployment/ecommerce-backend -n production
```

#### Long-term Solutions

**1. Optimize Connection Pool Configuration**:

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000  # Enable leak detection
```

**2. Implement Connection Validation**:

```yaml
spring:
  datasource:
    hikari:
      connection-test-query: SELECT 1
      validation-timeout: 5000
```

**3. Add Connection Pool Monitoring**:

```java
@Component
public class ConnectionPoolMonitor {
    
    @Scheduled(fixedRate = 60000)
    public void monitorConnectionPool() {
        HikariPoolMXBean poolProxy = hikariDataSource.getHikariPoolMXBean();
        
        logger.info("Connection Pool Stats - Active: {}, Idle: {}, Total: {}, Waiting: {}",
            poolProxy.getActiveConnections(),
            poolProxy.getIdleConnections(),
            poolProxy.getTotalConnections(),
            poolProxy.getThreadsAwaitingConnection());
        
        if (poolProxy.getActiveConnections() > poolProxy.getTotalConnections() * 0.8) {
            logger.warn("Connection pool usage > 80%");
        }
    }
}
```

**4. Fix Application Code**:

```java
// ❌ BAD: Connection leak
public void processOrder(String orderId) {
    Connection conn = dataSource.getConnection();
    // ... processing ...
    // Connection never closed!
}

// ✅ GOOD: Use try-with-resources
public void processOrder(String orderId) {
    try (Connection conn = dataSource.getConnection()) {
        // ... processing ...
    } // Connection automatically closed
}
```

### Prevention

**Set up monitoring alerts**:

```yaml
# Prometheus alert

- alert: HighConnectionPoolUsage

  expr: hikaricp_connections_active / hikaricp_connections_max > 0.8
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Connection pool usage > 80%"

- alert: ConnectionPoolExhausted

  expr: hikaricp_connections_pending > 0
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Connection pool exhausted - requests waiting"
```

**Regular maintenance**:

```sql
-- Weekly connection audit
SELECT 
    application_name,
    state,
    count(*) as connections,
    max(now() - state_change) as max_duration
FROM pg_stat_activity
GROUP BY application_name, state
ORDER BY connections DESC;
```

---

## Deadlock Detection and Resolution

### Overview

Deadlocks occur when two or more transactions are waiting for each other to release locks, creating a circular dependency.

### Symptoms

- "Deadlock detected" errors in logs
- Transactions timing out
- Application retries failing
- Specific operations consistently failing

### Detection Procedures

#### Step 1: Enable Deadlock Logging

```sql
-- Enable detailed deadlock logging
ALTER SYSTEM SET log_lock_waits = on;
ALTER SYSTEM SET deadlock_timeout = '1s';
ALTER SYSTEM SET log_min_duration_statement = 1000;
SELECT pg_reload_conf();
```

#### Step 2: Check PostgreSQL Logs

```bash
# View recent deadlocks
kubectl logs -l app=postgres -n production | grep "deadlock detected"

# Detailed deadlock information
psql -c "SELECT * FROM pg_stat_database WHERE datname = 'ecommerce';"
```

#### Step 3: Analyze Deadlock Details

```sql
-- Check current locks
SELECT 
    l.locktype,
    l.database,
    l.relation::regclass,
    l.page,
    l.tuple,
    l.virtualxid,
    l.transactionid,
    l.mode,
    l.granted,
    a.usename,
    a.query,
    a.query_start
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
    blocking_activity.query AS blocking_statement
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
```

### Common Deadlock Scenarios

#### Scenario 1: Update Order Deadlock

**Problem**: Two transactions updating orders in different order

```sql
-- Transaction 1
BEGIN;
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-1';
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-2';
COMMIT;

-- Transaction 2 (concurrent)
BEGIN;
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-2';  -- Waits for T1
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-1';  -- Deadlock!
COMMIT;
```

**Solution**: Always update rows in consistent order (e.g., by ID)

```sql
-- Both transactions update in same order
BEGIN;
UPDATE orders SET status = 'PROCESSING' 
WHERE id IN ('ORDER-1', 'ORDER-2')
ORDER BY id;  -- Consistent ordering prevents deadlock
COMMIT;
```

#### Scenario 2: Foreign Key Deadlock

**Problem**: Concurrent inserts with foreign key checks

```sql
-- Transaction 1
BEGIN;
INSERT INTO order_items (order_id, product_id) VALUES ('ORDER-1', 'PROD-1');
-- Acquires share lock on orders table

-- Transaction 2
BEGIN;
UPDATE orders SET total = 100 WHERE id = 'ORDER-1';  -- Waits for T1
-- Meanwhile T1 tries to insert another item...
```

**Solution**: Use SELECT FOR UPDATE to acquire locks explicitly

```sql
BEGIN;
SELECT * FROM orders WHERE id = 'ORDER-1' FOR UPDATE;
INSERT INTO order_items (order_id, product_id) VALUES ('ORDER-1', 'PROD-1');
COMMIT;
```

#### Scenario 3: Index Deadlock

**Problem**: Concurrent updates causing index lock conflicts

**Solution**:

- Batch updates when possible
- Use smaller transactions
- Consider using advisory locks

### Resolution Workflows

#### Workflow 1: Immediate Resolution

```sql
-- Identify and kill blocking transaction
SELECT pg_terminate_backend(blocking_pid)
FROM (
    SELECT blocking_locks.pid AS blocking_pid
    FROM pg_catalog.pg_locks blocked_locks
    JOIN pg_catalog.pg_locks blocking_locks 
        ON blocking_locks.locktype = blocked_locks.locktype
        AND blocking_locks.pid != blocked_locks.pid
    WHERE NOT blocked_locks.granted
    LIMIT 1
) AS blocker;
```

#### Workflow 2: Application-Level Retry

```java
@Transactional
@Retryable(
    value = {DeadlockLoserDataAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 100, multiplier = 2)
)
public void updateOrder(String orderId, OrderStatus status) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    order.setStatus(status);
    orderRepository.save(order);
}
```

#### Workflow 3: Code Refactoring

**Before** (deadlock-prone):

```java
public void processOrders(List<String> orderIds) {
    for (String orderId : orderIds) {
        Order order = orderRepository.findById(orderId).get();
        order.process();
        orderRepository.save(order);
    }
}
```

**After** (deadlock-safe):

```java
public void processOrders(List<String> orderIds) {
    // Sort IDs to ensure consistent lock order
    List<String> sortedIds = orderIds.stream()
        .sorted()
        .collect(Collectors.toList());
    
    for (String orderId : sortedIds) {
        Order order = orderRepository.findById(orderId).get();
        order.process();
        orderRepository.save(order);
    }
}
```

### Prevention Strategies

**1. Use Explicit Locking**:

```sql
-- Acquire locks in consistent order
BEGIN;
SELECT * FROM orders 
WHERE id IN ('ORDER-1', 'ORDER-2')
ORDER BY id
FOR UPDATE;

-- Now safe to update
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-1';
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-2';
COMMIT;
```

**2. Keep Transactions Short**:

```java
// ❌ BAD: Long transaction
@Transactional
public void processOrder(String orderId) {
    Order order = orderRepository.findById(orderId).get();
    order.process();
    
    // External API call inside transaction!
    paymentService.processPayment(order);
    
    orderRepository.save(order);
}

// ✅ GOOD: Short transaction
public void processOrder(String orderId) {
    // External call outside transaction
    PaymentResult result = paymentService.processPayment(orderId);
    
    // Short transaction
    updateOrderStatus(orderId, result);
}

@Transactional
private void updateOrderStatus(String orderId, PaymentResult result) {
    Order order = orderRepository.findById(orderId).get();
    order.updatePaymentStatus(result);
    orderRepository.save(order);
}
```

**3. Use Advisory Locks**:

```sql
-- Application-level locking
SELECT pg_advisory_lock(hashtext('ORDER-123'));
-- ... perform operations ...
SELECT pg_advisory_unlock(hashtext('ORDER-123'));
```

**4. Monitor Deadlock Frequency**:

```sql
-- Create monitoring view
CREATE OR REPLACE VIEW deadlock_stats AS
SELECT 
    datname,
    deadlocks,
    deadlocks::float / (EXTRACT(EPOCH FROM (now() - stats_reset)) / 3600) AS deadlocks_per_hour
FROM pg_stat_database
WHERE datname = 'ecommerce';
```

---

## Lock Contention Analysis

### Overview

Lock contention occurs when multiple transactions compete for the same database resources, causing performance degradation.

### Symptoms

- Slow query execution despite good query plans
- High wait times in pg_stat_activity
- Increased transaction latency
- Lock wait timeouts

### Analysis Procedures

#### Step 1: Identify Lock Contention

```sql
-- Current lock waits
SELECT 
    waiting.pid AS waiting_pid,
    waiting.query AS waiting_query,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query,
    now() - waiting.query_start AS waiting_duration
FROM pg_stat_activity AS waiting
JOIN pg_locks AS waiting_locks ON waiting.pid = waiting_locks.pid
JOIN pg_locks AS blocking_locks 
    ON waiting_locks.locktype = blocking_locks.locktype
    AND waiting_locks.database IS NOT DISTINCT FROM blocking_locks.database
    AND waiting_locks.relation IS NOT DISTINCT FROM blocking_locks.relation
    AND waiting_locks.page IS NOT DISTINCT FROM blocking_locks.page
    AND waiting_locks.tuple IS NOT DISTINCT FROM blocking_locks.tuple
    AND waiting_locks.virtualxid IS NOT DISTINCT FROM blocking_locks.virtualxid
    AND waiting_locks.transactionid IS NOT DISTINCT FROM blocking_locks.transactionid
    AND waiting_locks.classid IS NOT DISTINCT FROM blocking_locks.classid
    AND waiting_locks.objid IS NOT DISTINCT FROM blocking_locks.objid
    AND waiting_locks.objsubid IS NOT DISTINCT FROM blocking_locks.objsubid
    AND waiting_locks.pid != blocking_locks.pid
JOIN pg_stat_activity AS blocking ON blocking_locks.pid = blocking.pid
WHERE NOT waiting_locks.granted
    AND blocking_locks.granted;
```

#### Step 2: Analyze Lock Types

```sql
-- Lock types by table
SELECT 
    l.relation::regclass AS table_name,
    l.mode,
    count(*) AS lock_count
FROM pg_locks l
JOIN pg_stat_activity a ON l.pid = a.pid
WHERE l.relation IS NOT NULL
GROUP BY l.relation, l.mode
ORDER BY lock_count DESC;

-- Lock wait events
SELECT 
    wait_event_type,
    wait_event,
    count(*) AS count
FROM pg_stat_activity
WHERE wait_event IS NOT NULL
GROUP BY wait_event_type, wait_event
ORDER BY count DESC;
```

#### Step 3: Identify Hot Tables

```sql
-- Tables with most lock conflicts
SELECT 
    schemaname,
    tablename,
    n_tup_ins + n_tup_upd + n_tup_del AS total_modifications,
    n_tup_hot_upd,
    n_live_tup,
    n_dead_tup
FROM pg_stat_user_tables
ORDER BY total_modifications DESC
LIMIT 20;
```

### Optimization Strategies

#### Strategy 1: Reduce Lock Scope

```sql
-- ❌ BAD: Locks entire table
BEGIN;
LOCK TABLE orders IN EXCLUSIVE MODE;
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-1';
COMMIT;

-- ✅ GOOD: Row-level lock only
BEGIN;
UPDATE orders SET status = 'PROCESSING' WHERE id = 'ORDER-1';
COMMIT;
```

#### Strategy 2: Use Lower Isolation Levels

```sql
-- For read-heavy operations
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT * FROM orders WHERE customer_id = 'CUST-123';
COMMIT;

-- For operations that can tolerate dirty reads
BEGIN TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
SELECT count(*) FROM orders;
COMMIT;
```

#### Strategy 3: Optimize UPDATE Operations

```sql
-- ❌ BAD: Updates all rows even if no change
UPDATE orders SET last_checked = now();

-- ✅ GOOD: Only update changed rows
UPDATE orders 
SET last_checked = now()
WHERE last_checked < now() - interval '1 hour';
```

#### Strategy 4: Use SELECT FOR UPDATE SKIP LOCKED

```sql
-- Process queue without blocking
SELECT * FROM order_queue
WHERE status = 'PENDING'
ORDER BY created_at
LIMIT 10
FOR UPDATE SKIP LOCKED;
```

#### Strategy 5: Partition Hot Tables

```sql
-- Partition orders by date to reduce contention
CREATE TABLE orders_2025_01 PARTITION OF orders
FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE orders_2025_02 PARTITION OF orders
FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
```

### Monitoring Lock Contention

```sql
-- Create monitoring function
CREATE OR REPLACE FUNCTION check_lock_contention()
RETURNS TABLE (
    waiting_pid int,
    waiting_query text,
    blocking_pid int,
    blocking_query text,
    wait_duration interval
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        w.pid,
        w.query,
        b.pid,
        b.query,
        now() - w.query_start
    FROM pg_stat_activity w
    JOIN pg_locks wl ON w.pid = wl.pid
    JOIN pg_locks bl ON wl.locktype = bl.locktype
        AND wl.database IS NOT DISTINCT FROM bl.database
        AND wl.relation IS NOT DISTINCT FROM bl.relation
        AND wl.pid != bl.pid
    JOIN pg_stat_activity b ON bl.pid = b.pid
    WHERE NOT wl.granted AND bl.granted;
END;
$$ LANGUAGE plpgsql;

-- Use the function
SELECT * FROM check_lock_contention();
```

---

## Transaction Isolation Issues

### Overview

Transaction isolation level issues can cause data inconsistencies, phantom reads, or unnecessary lock contention.

### Common Issues

#### Issue 1: Phantom Reads

**Symptom**: Query returns different results within same transaction

```sql
-- Transaction 1
BEGIN TRANSACTION ISOLATION LEVEL READ COMMITTED;
SELECT count(*) FROM orders WHERE status = 'PENDING';  -- Returns 10

-- Transaction 2 inserts new order
INSERT INTO orders (status) VALUES ('PENDING');

-- Transaction 1 (continued)
SELECT count(*) FROM orders WHERE status = 'PENDING';  -- Returns 11 (phantom!)
COMMIT;
```

**Solution**: Use REPEATABLE READ or SERIALIZABLE

```sql
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
SELECT count(*) FROM orders WHERE status = 'PENDING';  -- Returns 10
-- Even if T2 inserts, this will still return 10
SELECT count(*) FROM orders WHERE status = 'PENDING';  -- Returns 10
COMMIT;
```

#### Issue 2: Lost Updates

**Symptom**: Concurrent updates overwrite each other

```sql
-- Transaction 1
BEGIN;
SELECT balance FROM accounts WHERE id = 'ACC-1';  -- balance = 100
-- ... application logic ...
UPDATE accounts SET balance = 150 WHERE id = 'ACC-1';

-- Transaction 2 (concurrent)
BEGIN;
SELECT balance FROM accounts WHERE id = 'ACC-1';  -- balance = 100
-- ... application logic ...
UPDATE accounts SET balance = 120 WHERE id = 'ACC-1';  -- Overwrites T1!
```

**Solution**: Use SELECT FOR UPDATE or optimistic locking

```sql
-- Pessimistic locking
BEGIN;
SELECT balance FROM accounts WHERE id = 'ACC-1' FOR UPDATE;
UPDATE accounts SET balance = balance + 50 WHERE id = 'ACC-1';
COMMIT;

-- Optimistic locking
BEGIN;
UPDATE accounts 
SET balance = balance + 50, version = version + 1
WHERE id = 'ACC-1' AND version = 5;
-- Check affected rows, retry if 0
COMMIT;
```

#### Issue 3: Serialization Failures

**Symptom**: "could not serialize access" errors with SERIALIZABLE isolation

```sql
-- Transaction 1
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
SELECT sum(amount) FROM orders WHERE customer_id = 'CUST-1';
INSERT INTO order_summary (customer_id, total) VALUES ('CUST-1', 1000);

-- Transaction 2 (concurrent)
BEGIN TRANSACTION ISOLATION LEVEL SERIALIZABLE;
INSERT INTO orders (customer_id, amount) VALUES ('CUST-1', 100);
COMMIT;  -- May fail with serialization error

-- Transaction 1 (continued)
COMMIT;  -- One of these will fail
```

**Solution**: Implement retry logic

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
@Retryable(
    value = {SerializationFailureException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 50, multiplier = 2)
)
public void updateOrderSummary(String customerId) {
    // Transaction logic
}
```

### Choosing Isolation Levels

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Performance | Use Case |
|----------------|------------|---------------------|--------------|-------------|----------|
| READ UNCOMMITTED | Yes | Yes | Yes | Highest | Analytics, reporting |
| READ COMMITTED | No | Yes | Yes | High | Most operations |
| REPEATABLE READ | No | No | Yes | Medium | Financial calculations |
| SERIALIZABLE | No | No | No | Lowest | Critical transactions |

**Application Configuration**:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        connection:
          isolation: 2  # READ_COMMITTED (default)
```

**Per-transaction override**:

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void criticalOperation() {
    // Transaction logic
}
```

---

## Replication Lag Troubleshooting

### Overview

Replication lag occurs when read replicas fall behind the primary database, causing stale data reads.

### Symptoms

- Stale data in read queries
- Replication lag alerts
- Inconsistent data across replicas
- "Replica not available" errors

### Monitoring Replication Lag

```sql
-- On primary: Check replication status
SELECT 
    client_addr,
    state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    sync_state,
    pg_wal_lsn_diff(sent_lsn, replay_lsn) AS lag_bytes
FROM pg_stat_replication;

-- On replica: Check lag time
SELECT 
    now() - pg_last_xact_replay_timestamp() AS replication_lag;

-- Detailed replication metrics
SELECT 
    slot_name,
    slot_type,
    database,
    active,
    pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) AS retained_bytes
FROM pg_replication_slots;
```

### Root Cause Analysis

#### Cause 1: High Write Volume

```sql
-- Check write activity on primary
SELECT 
    schemaname,
    tablename,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    n_tup_ins + n_tup_upd + n_tup_del AS total_writes
FROM pg_stat_user_tables
ORDER BY total_writes DESC
LIMIT 20;

-- Check WAL generation rate
SELECT 
    pg_wal_lsn_diff(pg_current_wal_lsn(), '0/0') / 1024 / 1024 AS wal_mb_generated;
```

#### Cause 2: Network Issues

```bash
# Test network latency between primary and replica
ping -c 10 replica-host

# Check network bandwidth
iperf3 -c replica-host -t 30

# Monitor replication connection
netstat -an | grep 5432
```

#### Cause 3: Replica Resource Constraints

```sql
-- On replica: Check resource usage
SELECT 
    datname,
    numbackends,
    xact_commit,
    xact_rollback,
    blks_read,
    blks_hit,
    tup_returned,
    tup_fetched
FROM pg_stat_database
WHERE datname = 'ecommerce';

-- Check for long-running queries on replica
SELECT 
    pid,
    now() - query_start AS duration,
    state,
    query
FROM pg_stat_activity
WHERE state != 'idle'
ORDER BY duration DESC;
```

#### Cause 4: Replication Slot Issues

```sql
-- Check for inactive replication slots
SELECT 
    slot_name,
    slot_type,
    database,
    active,
    pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) / 1024 / 1024 AS lag_mb
FROM pg_replication_slots
WHERE NOT active;

-- Remove inactive slots
SELECT pg_drop_replication_slot('slot_name');
```

### Resolution Strategies

#### Strategy 1: Optimize Replica Configuration

```conf
# postgresql.conf on replica
max_standby_streaming_delay = 30s
hot_standby_feedback = on
wal_receiver_status_interval = 1s
```

#### Strategy 2: Reduce Write Load

```sql
-- Batch updates instead of individual
UPDATE orders 
SET status = 'PROCESSED'
WHERE id = ANY(ARRAY['ORDER-1', 'ORDER-2', 'ORDER-3']);

-- Use COPY for bulk inserts
COPY orders FROM '/tmp/orders.csv' WITH CSV;
```

#### Strategy 3: Add More Replicas

```bash
# Create additional read replica
aws rds create-db-instance-read-replica \
    --db-instance-identifier ecommerce-replica-2 \
    --source-db-instance-identifier ecommerce-primary \
    --db-instance-class db.r5.large
```

#### Strategy 4: Implement Read-After-Write Consistency

```java
@Service
public class OrderService {
    
    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;
    
    @Autowired
    @Qualifier("replicaDataSource")
    private DataSource replicaDataSource;
    
    // Write to primary
    @Transactional("primaryTransactionManager")
    public Order createOrder(CreateOrderCommand command) {
        Order order = new Order(command);
        return orderRepository.save(order);
    }
    
    // Read from replica with fallback
    public Order getOrder(String orderId) {
        try {
            return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        } catch (ReplicationLagException e) {
            // Fallback to primary if replica is lagging
            return orderRepositoryPrimary.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        }
    }
}
```

### Monitoring and Alerting

```yaml
# Prometheus alert rules

- alert: HighReplicationLag

  expr: pg_replication_lag_seconds > 30
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Replication lag > 30 seconds"
    description: "Replica {{ $labels.instance }} is {{ $value }}s behind"

- alert: CriticalReplicationLag

  expr: pg_replication_lag_seconds > 300
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "Critical replication lag > 5 minutes"
```

---

## Database Parameter Tuning

### Overview

Proper database parameter tuning is essential for optimal performance. This section covers scenario-specific tuning recommendations.

### Memory Parameters

#### Scenario 1: High Read Workload

```conf
# postgresql.conf
shared_buffers = 8GB                    # 25% of RAM
effective_cache_size = 24GB             # 75% of RAM
work_mem = 64MB                         # Per operation
maintenance_work_mem = 2GB              # For VACUUM, CREATE INDEX
```

#### Scenario 2: High Write Workload

```conf
# postgresql.conf
shared_buffers = 8GB
wal_buffers = 16MB
checkpoint_timeout = 15min
max_wal_size = 4GB
checkpoint_completion_target = 0.9
```

#### Scenario 3: Mixed Workload

```conf
# postgresql.conf
shared_buffers = 8GB
effective_cache_size = 24GB
work_mem = 32MB
maintenance_work_mem = 1GB
wal_buffers = 16MB
```

### Connection Parameters

```conf
# For connection pooling
max_connections = 200
superuser_reserved_connections = 3

# Connection timeouts
tcp_keepalives_idle = 60
tcp_keepalives_interval = 10
tcp_keepalives_count = 3
```

### Query Planner Parameters

```conf
# Cost-based optimizer
random_page_cost = 1.1                  # For SSD
effective_io_concurrency = 200          # For SSD
default_statistics_target = 100         # More accurate statistics

# Parallel query execution
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
```

### Autovacuum Parameters

```conf
# Aggressive autovacuum for high-write tables
autovacuum_max_workers = 4
autovacuum_naptime = 10s
autovacuum_vacuum_threshold = 50
autovacuum_vacuum_scale_factor = 0.1
autovacuum_analyze_threshold = 50
autovacuum_analyze_scale_factor = 0.05
```

### Logging Parameters

```conf
# Performance monitoring
log_min_duration_statement = 1000       # Log queries > 1s
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d,app=%a,client=%h '
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on
log_temp_files = 0
```

### Applying Parameter Changes

```sql
-- Check current values
SHOW shared_buffers;
SHOW work_mem;

-- Change parameters (requires restart for some)
ALTER SYSTEM SET work_mem = '64MB';
SELECT pg_reload_conf();

-- Verify changes
SHOW work_mem;
```

### Parameter Tuning Workflow

1. **Baseline Measurement**:

```bash
# Run pgbench for baseline
pgbench -i -s 100 ecommerce
pgbench -c 10 -j 2 -t 1000 ecommerce
```

1. **Apply Changes**:

```sql
ALTER SYSTEM SET work_mem = '64MB';
SELECT pg_reload_conf();
```

1. **Measure Impact**:

```bash
pgbench -c 10 -j 2 -t 1000 ecommerce
```

1. **Compare Results**:

```sql
SELECT 
    query,
    calls,
    mean_time,
    max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

---

## pg_stat_statements Analysis

### Overview

`pg_stat_statements` is a powerful extension for tracking query execution statistics. This section provides comprehensive analysis techniques.

### Setup and Configuration

```sql
-- Enable extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Configure parameters
ALTER SYSTEM SET pg_stat_statements.track = 'all';
ALTER SYSTEM SET pg_stat_statements.max = 10000;
ALTER SYSTEM SET pg_stat_statements.track_utility = on;
SELECT pg_reload_conf();

-- Verify installation
SELECT * FROM pg_stat_statements LIMIT 1;
```

### Analysis Techniques

#### Technique 1: Find Slowest Queries

```sql
-- Top 20 slowest queries by average time
SELECT 
    query,
    calls,
    total_time / 1000 AS total_seconds,
    mean_time / 1000 AS mean_seconds,
    max_time / 1000 AS max_seconds,
    stddev_time / 1000 AS stddev_seconds,
    rows,
    100.0 * shared_blks_hit / NULLIF(shared_blks_hit + shared_blks_read, 0) AS cache_hit_ratio
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY mean_time DESC
LIMIT 20;
```

#### Technique 2: Find Most Frequently Called Queries

```sql
-- Queries called most often
SELECT 
    query,
    calls,
    total_time / 1000 AS total_seconds,
    mean_time / 1000 AS mean_seconds,
    rows,
    100.0 * shared_blks_hit / NULLIF(shared_blks_hit + shared_blks_read, 0) AS cache_hit_ratio
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY calls DESC
LIMIT 20;
```

#### Technique 3: Find Queries with High Total Time

```sql
-- Queries consuming most total time
SELECT 
    query,
    calls,
    total_time / 1000 AS total_seconds,
    mean_time / 1000 AS mean_seconds,
    (total_time / sum(total_time) OVER ()) * 100 AS percentage_of_total
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%'
ORDER BY total_time DESC
LIMIT 20;
```

#### Technique 4: Analyze Cache Efficiency

```sql
-- Queries with poor cache hit ratio
SELECT 
    query,
    calls,
    shared_blks_hit,
    shared_blks_read,
    shared_blks_hit + shared_blks_read AS total_blks,
    CASE 
        WHEN shared_blks_hit + shared_blks_read = 0 THEN 0
        ELSE 100.0 * shared_blks_hit / (shared_blks_hit + shared_blks_read)
    END AS cache_hit_ratio
FROM pg_stat_statements
WHERE shared_blks_read > 0
ORDER BY cache_hit_ratio ASC, shared_blks_read DESC
LIMIT 20;
```

#### Technique 5: Find Queries with High Variance

```sql
-- Queries with inconsistent performance
SELECT 
    query,
    calls,
    mean_time / 1000 AS mean_seconds,
    stddev_time / 1000 AS stddev_seconds,
    max_time / 1000 AS max_seconds,
    min_time / 1000 AS min_seconds,
    CASE 
        WHEN mean_time = 0 THEN 0
        ELSE (stddev_time / mean_time) * 100
    END AS coefficient_of_variation
FROM pg_stat_statements
WHERE calls > 100
    AND mean_time > 0
ORDER BY coefficient_of_variation DESC
LIMIT 20;
```

#### Technique 6: Analyze I/O Patterns

```sql
-- Queries with high I/O
SELECT 
    query,
    calls,
    shared_blks_read,
    shared_blks_written,
    local_blks_read,
    local_blks_written,
    temp_blks_read,
    temp_blks_written,
    (shared_blks_read + local_blks_read + temp_blks_read) AS total_reads,
    (shared_blks_written + local_blks_written + temp_blks_written) AS total_writes
FROM pg_stat_statements
WHERE shared_blks_read + local_blks_read + temp_blks_read > 0
ORDER BY total_reads DESC
LIMIT 20;
```

#### Technique 7: Find Queries Using Temp Files

```sql
-- Queries spilling to disk
SELECT 
    query,
    calls,
    temp_blks_read,
    temp_blks_written,
    (temp_blks_written * 8192 / 1024 / 1024) AS temp_mb_written
FROM pg_stat_statements
WHERE temp_blks_written > 0
ORDER BY temp_blks_written DESC
LIMIT 20;
```

### Creating Useful Views

```sql
-- Create comprehensive analysis view
CREATE OR REPLACE VIEW query_performance_summary AS
SELECT 
    queryid,
    LEFT(query, 100) AS query_preview,
    calls,
    total_time / 1000 AS total_seconds,
    mean_time / 1000 AS mean_seconds,
    max_time / 1000 AS max_seconds,
    stddev_time / 1000 AS stddev_seconds,
    rows,
    rows / NULLIF(calls, 0) AS rows_per_call,
    100.0 * shared_blks_hit / NULLIF(shared_blks_hit + shared_blks_read, 0) AS cache_hit_ratio,
    shared_blks_read,
    shared_blks_written,
    temp_blks_written,
    (temp_blks_written * 8192 / 1024 / 1024) AS temp_mb
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat_statements%';

-- Use the view
SELECT * FROM query_performance_summary
WHERE mean_seconds > 1
ORDER BY total_seconds DESC;
```

### Resetting Statistics

```sql
-- Reset all statistics
SELECT pg_stat_statements_reset();

-- Reset statistics for specific query
SELECT pg_stat_statements_reset(userid, dbid, queryid)
FROM pg_stat_statements
WHERE query LIKE '%specific_query%';
```

### Automated Monitoring

```sql
-- Create monitoring function
CREATE OR REPLACE FUNCTION monitor_slow_queries()
RETURNS TABLE (
    query_preview text,
    calls bigint,
    mean_seconds numeric,
    total_seconds numeric
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        LEFT(query, 100),
        calls,
        ROUND((mean_time / 1000)::numeric, 2),
        ROUND((total_time / 1000)::numeric, 2)
    FROM pg_stat_statements
    WHERE mean_time > 1000  -- > 1 second
    ORDER BY mean_time DESC
    LIMIT 10;
END;
$$ LANGUAGE plpgsql;

-- Schedule regular checks
SELECT * FROM monitor_slow_queries();
```

---

## EXPLAIN ANALYZE Interpretation

### Overview

`EXPLAIN ANALYZE` provides detailed execution plans and actual runtime statistics. This section covers interpretation techniques.

### Basic Usage

```sql
-- Basic EXPLAIN
EXPLAIN 
SELECT * FROM orders WHERE customer_id = 'CUST-123';

-- EXPLAIN ANALYZE (actually executes query)
EXPLAIN ANALYZE
SELECT * FROM orders WHERE customer_id = 'CUST-123';

-- Detailed analysis with all options
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, TIMING, FORMAT JSON)
SELECT * FROM orders WHERE customer_id = 'CUST-123';
```

### Understanding Output

#### Node Types

**Sequential Scan**:

```text
Seq Scan on orders  (cost=0.00..1234.56 rows=100 width=200)
                    (actual time=0.123..45.678 rows=95 loops=1)
```

- **Meaning**: Full table scan (reads every row)
- **When OK**: Small tables (< 1000 rows)
- **When BAD**: Large tables, should use index

**Index Scan**:

```text
Index Scan using idx_orders_customer on orders
    (cost=0.42..8.44 rows=1 width=200)
    (actual time=0.012..0.015 rows=1 loops=1)
```

- **Meaning**: Uses index to find rows
- **Good**: Efficient for selective queries
- **Cost**: Lower than sequential scan

**Index Only Scan**:

```text
Index Only Scan using idx_orders_customer_status on orders
    (cost=0.42..4.44 rows=1 width=16)
    (actual time=0.008..0.010 rows=1 loops=1)
    Heap Fetches: 0
```

- **Meaning**: All data from index, no table access
- **Best**: Most efficient, no heap access needed

**Bitmap Scan**:

```text
Bitmap Heap Scan on orders
    (cost=12.34..56.78 rows=100 width=200)
    ->  Bitmap Index Scan on idx_orders_status
        (cost=0.00..12.31 rows=100 width=0)
```

- **Meaning**: Uses index to build bitmap, then scans heap
- **When**: Multiple index conditions or large result sets

#### Key Metrics

**Cost**:

```text
(cost=0.42..8.44 rows=1 width=200)
       ^^^^  ^^^^
       startup  total
```

- **Startup cost**: Cost before first row
- **Total cost**: Cost to return all rows
- **Units**: Arbitrary, compare relative values

**Actual Time**:

```text
(actual time=0.012..0.015 rows=1 loops=1)
            ^^^^^  ^^^^^
            first  last
```

- **First row time**: Time to first row (ms)
- **Last row time**: Time to last row (ms)
- **Loops**: Number of times node executed

**Rows**:

```text
(cost=0.42..8.44 rows=100 width=200)
                 ^^^^^^^^
                 estimated
(actual time=0.012..0.015 rows=95 loops=1)
                          ^^^^^^^
                          actual
```

- **Estimated rows**: Planner's estimate
- **Actual rows**: Actual rows returned
- **Large difference**: Statistics may be outdated

### Interpretation Examples

#### Example 1: Missing Index

```sql
EXPLAIN ANALYZE
SELECT * FROM orders WHERE customer_id = 'CUST-123';

-- Output:
Seq Scan on orders  (cost=0.00..15234.56 rows=100 width=200)
                    (actual time=0.123..456.789 rows=95 loops=1)
  Filter: (customer_id = 'CUST-123'::text)
  Rows Removed by Filter: 99905
Planning Time: 0.123 ms
Execution Time: 456.912 ms
```

**Analysis**:

- ❌ Sequential scan on large table
- ❌ High execution time (456ms)
- ❌ Many rows filtered out (99905)
- **Solution**: Create index on customer_id

```sql
CREATE INDEX CONCURRENTLY idx_orders_customer 
ON orders(customer_id);
```

#### Example 2: Outdated Statistics

```sql
EXPLAIN ANALYZE
SELECT * FROM orders WHERE status = 'PENDING';

-- Output:
Index Scan using idx_orders_status on orders
    (cost=0.42..8.44 rows=1 width=200)
    (actual time=0.012..123.456 rows=10000 loops=1)
```

**Analysis**:

- ❌ Estimated 1 row, actual 10,000 rows
- ❌ Planner chose wrong plan
- **Solution**: Update statistics

```sql
ANALYZE orders;
```

#### Example 3: Inefficient Join

```sql
EXPLAIN ANALYZE
SELECT o.*, c.name 
FROM orders o
JOIN customers c ON o.customer_id = c.id
WHERE o.status = 'PENDING';

-- Output:
Nested Loop  (cost=0.85..25678.90 rows=100 width=250)
             (actual time=0.123..1234.567 rows=100 loops=1)
  ->  Seq Scan on orders o  (cost=0.00..15234.56 rows=100 width=200)
                             (actual time=0.100..456.789 rows=100 loops=1)
        Filter: (status = 'PENDING'::text)
  ->  Index Scan using customers_pkey on customers c
                             (cost=0.42..8.44 rows=1 width=50)
                             (actual time=0.005..0.006 rows=1 loops=100)
        Index Cond: (id = o.customer_id)
```

**Analysis**:

- ❌ Sequential scan on orders
- ❌ Nested loop with 100 iterations
- **Solution**: Add index on orders.status

```sql
CREATE INDEX CONCURRENTLY idx_orders_status 
ON orders(status);
```

#### Example 4: Excessive Sorting

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders 
ORDER BY order_date DESC 
LIMIT 100;

-- Output:
Limit  (cost=15234.56..15234.81 rows=100 width=200)
       (actual time=456.789..456.890 rows=100 loops=1)
  Buffers: shared hit=1234 read=5678
  ->  Sort  (cost=15234.56..15484.56 rows=100000 width=200)
            (actual time=456.789..456.850 rows=100 loops=1)
        Sort Key: order_date DESC
        Sort Method: top-N heapsort  Memory: 50kB
        Buffers: shared hit=1234 read=5678
        ->  Seq Scan on orders  (cost=0.00..12345.00 rows=100000 width=200)
                                (actual time=0.012..234.567 rows=100000 loops=1)
              Buffers: shared hit=1234 read=5678
```

**Analysis**:

- ❌ Full table scan then sort
- ❌ High buffer reads (5678)
- **Solution**: Create index on order_date

```sql
CREATE INDEX CONCURRENTLY idx_orders_date 
ON orders(order_date DESC);
```

#### Example 5: Temp File Usage

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT customer_id, count(*), sum(total_amount)
FROM orders
GROUP BY customer_id;

-- Output:
HashAggregate  (cost=23456.78..24567.89 rows=10000 width=40)
               (actual time=1234.567..1345.678 rows=10000 loops=1)
  Group Key: customer_id
  Buffers: shared hit=5000 read=10000, temp read=2000 written=2000
  ->  Seq Scan on orders  (cost=0.00..12345.00 rows=100000 width=20)
                          (actual time=0.012..234.567 rows=100000 loops=1)
        Buffers: shared hit=5000 read=10000
```

**Analysis**:

- ❌ Using temp files (disk spill)
- ❌ Indicates insufficient work_mem
- **Solution**: Increase work_mem

```sql
SET work_mem = '256MB';
-- Or permanently:
ALTER SYSTEM SET work_mem = '256MB';
SELECT pg_reload_conf();
```

### Buffer Analysis

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 'CUST-123';

-- Output with buffer details:
Index Scan using idx_orders_customer on orders
    (cost=0.42..8.44 rows=1 width=200)
    (actual time=0.012..0.015 rows=1 loops=1)
  Index Cond: (customer_id = 'CUST-123'::text)
  Buffers: shared hit=4 read=0
```

**Buffer Metrics**:

- **shared hit**: Blocks found in cache (good)
- **shared read**: Blocks read from disk (slower)
- **Cache hit ratio**: hit / (hit + read) × 100%
- **Target**: > 95% cache hit ratio

### Optimization Workflow

1. **Identify Problem**:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM orders WHERE customer_id = 'CUST-123';
```

1. **Analyze Output**:

- Check for sequential scans
- Compare estimated vs actual rows
- Check buffer usage
- Look for temp file usage

1. **Apply Fix**:

```sql
-- Add index
CREATE INDEX CONCURRENTLY idx_orders_customer 
ON orders(customer_id);

-- Update statistics
ANALYZE orders;

-- Adjust parameters
SET work_mem = '256MB';
```

1. **Verify Improvement**:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM orders WHERE customer_id = 'CUST-123';
```

1. **Compare Results**:

- Execution time reduced?
- Better plan chosen?
- Fewer buffer reads?

### Common Patterns and Solutions

| Pattern | Problem | Solution |
|---------|---------|----------|
| Seq Scan on large table | Missing index | CREATE INDEX |
| Estimated ≠ Actual rows | Outdated statistics | ANALYZE table |
| Temp files used | Low work_mem | Increase work_mem |
| Many buffer reads | Poor cache hit | Increase shared_buffers |
| Nested Loop with many iterations | Inefficient join | Add index or use hash join |
| Sort operation | No index for ORDER BY | CREATE INDEX on sort column |

---

## Quick Reference

### Emergency Commands

```sql
-- Kill blocking query
SELECT pg_terminate_backend(pid);

-- Cancel long-running query
SELECT pg_cancel_backend(pid);

-- Reset statistics
SELECT pg_stat_statements_reset();

-- Force checkpoint
CHECKPOINT;

-- Reload configuration
SELECT pg_reload_conf();
```

### Diagnostic Queries

```sql
-- Current activity
SELECT * FROM pg_stat_activity WHERE state != 'idle';

-- Lock conflicts
SELECT * FROM pg_locks WHERE NOT granted;

-- Slow queries
SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;

-- Replication lag
SELECT now() - pg_last_xact_replay_timestamp() AS lag;

-- Cache hit ratio
SELECT 
    sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) AS ratio
FROM pg_statio_user_tables;
```

### Performance Checklist

- [ ] Indexes created for frequently queried columns
- [ ] Statistics up to date (ANALYZE run recently)
- [ ] No long-running idle transactions
- [ ] Connection pool properly sized
- [ ] work_mem sufficient (no temp file usage)
- [ ] Cache hit ratio > 95%
- [ ] No replication lag > 30 seconds
- [ ] Autovacuum running regularly
- [ ] No table bloat > 20%
- [ ] Query execution time < 1 second

---

## Related Documentation

- [Database Maintenance](../maintenance/database-maintenance.md) - Regular maintenance procedures
- [Slow API Responses Runbook](../runbooks/slow-api-responses.md) - API performance troubleshooting
- [Monitoring Strategy](../monitoring/monitoring-strategy.md) - Database monitoring setup

---

**Last Updated**: 2025-10-25  
**Owner**: Database Team  
**Review Cycle**: Quarterly  
**Version**: 1.0
