---
title: "Cache Infrastructure"
type: "deployment-viewpoint"
category: "infrastructure"
status: "active"
last_updated: "2025-10-23"
owner: "DevOps Team"
---

# Cache Infrastructure

> **Service**: Amazon ElastiCache (Redis)

## Cluster Configuration

**Cluster Details**:
```yaml
Cluster ID: ecommerce-platform-prod-redis
Engine: Redis 7.0
Cluster Mode: Enabled
Node Type: cache.r5.large
  - vCPU: 2
  - Memory: 13.07 GiB
  - Network: Up to 10 Gbps

Shards: 3
Replicas per Shard: 1
Total Nodes: 6 (3 primary + 3 replicas)
```

**Shard Distribution**:
- Shard 1: Primary (us-east-1a), Replica (us-east-1b)
- Shard 2: Primary (us-east-1b), Replica (us-east-1c)
- Shard 3: Primary (us-east-1c), Replica (us-east-1a)

## Redis Configuration

**Parameters**:
```yaml
maxmemory-policy: allkeys-lru
timeout: 300
tcp-keepalive: 300
maxmemory-samples: 5
slowlog-log-slower-than: 10000
slowlog-max-len: 128
notify-keyspace-events: Ex
```

**Persistence**:
- AOF (Append-Only File): Enabled
- AOF Rewrite: Automatic when file grows 100%
- Snapshot: Disabled (using AOF for durability)

## Backup and Maintenance

**Automated Backups**:
- Backup Retention: 7 days
- Backup Window: 03:00-04:00 UTC
- Final Snapshot: Enabled on cluster deletion

**Maintenance Window**:
- Preferred Window: Sun 04:00-05:00 UTC
- Auto Minor Version Upgrade: Enabled

**Monitoring**:
- CloudWatch Metrics: CPUUtilization, DatabaseMemoryUsagePercentage, CurrConnections, Evictions, CacheHits/CacheMisses, ReplicationLag
- Alarms:
  - CPU > 75%
  - Memory > 90%
  - Evictions > 1000/min
  - Cache Hit Rate < 80%
