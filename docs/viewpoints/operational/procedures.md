---
title: "Operational Procedures"
viewpoint: "Operational"
status: "active"
last_updated: "2025-01-22"
stakeholders: ["Operations Team", "SRE Team", "DevOps Engineers", "Support Team"]
---

# Operational Procedures

> **Viewpoint**: Operational  
> **Purpose**: Document step-by-step operational procedures for the E-Commerce Platform  
> **Audience**: Operations Team, SRE Team, DevOps Engineers, Support Team

## Overview

This document provides detailed operational procedures for common tasks including startup, shutdown, scaling, troubleshooting, and maintenance activities.

## Service Management Procedures

### Service Startup Procedure

**Prerequisites**:

- AWS infrastructure is healthy
- Database is accessible
- Redis cache is available
- Kafka brokers are running

**Steps**:

1. **Verify Infrastructure Health**

```bash
# Check EKS cluster status
kubectl cluster-info
kubectl get nodes

# Check database connectivity
psql -h ecommerce-prod-db.xxx.rds.amazonaws.com -U admin -d ecommerce -c "SELECT 1;"

# Check Redis connectivity
redis-cli -h ecommerce-redis.xxx.cache.amazonaws.com ping

# Check Kafka brokers
kafka-broker-api-versions.sh --bootstrap-server kafka-broker:9092
```

1. **Start Core Services** (in order)

```bash
# 1. Start Customer Service
kubectl scale deployment/customer-service --replicas=3 -n customer-context
kubectl rollout status deployment/customer-service -n customer-context

# 2. Start Product Service
kubectl scale deployment/product-service --replicas=3 -n product-context
kubectl rollout status deployment/product-service -n product-context

# 3. Start Order Service
kubectl scale deployment/order-service --replicas=5 -n order-context
kubectl rollout status deployment/order-service -n order-context

# 4. Start Payment Service
kubectl scale deployment/payment-service --replicas=3 -n payment-context
kubectl rollout status deployment/payment-service -n payment-context

# 5. Start remaining services
kubectl scale deployment --all --replicas=2 -n notification-context
kubectl scale deployment --all --replicas=2 -n search-context
```

1. **Verify Service Health**

```bash
# Check all pods are running
kubectl get pods --all-namespaces | grep -v Running

# Check service endpoints
for service in customer product order payment; do
  curl -f https://api.ecommerce-platform.com/api/v1/$service/health || echo "$service health check failed"
done

# Check metrics
curl https://api.ecommerce-platform.com/actuator/metrics
```

1. **Enable Traffic**

```bash
# Update load balancer to route traffic
kubectl annotate service api-gateway service.beta.kubernetes.io/aws-load-balancer-backend-protocol=http

# Verify traffic flow
watch -n 5 'kubectl top pods -n order-context'
```

### Service Shutdown Procedure

**Use Cases**:

- Planned maintenance
- Emergency shutdown
- Cost optimization (non-prod environments)

**Steps**:

1. **Notify Stakeholders**

```bash
# Post to status page
curl -X POST https://status.ecommerce-platform.com/api/incidents \
  -H "Authorization: Bearer $STATUS_PAGE_TOKEN" \
  -d '{"status": "investigating", "message": "Planned maintenance in progress"}'

# Notify via Slack
curl -X POST $SLACK_WEBHOOK_URL \
  -d '{"text": "🔧 Planned maintenance starting. Services will be unavailable for 2 hours."}'
```

1. **Drain Traffic**

```bash
# Stop accepting new requests
kubectl annotate service api-gateway service.beta.kubernetes.io/aws-load-balancer-backend-protocol=none

# Wait for in-flight requests to complete (5 minutes)
sleep 300
```

1. **Shutdown Services** (reverse order)

```bash
# 1. Stop non-critical services
kubectl scale deployment --all --replicas=0 -n notification-context
kubectl scale deployment --all --replicas=0 -n search-context

# 2. Stop Payment Service
kubectl scale deployment/payment-service --replicas=0 -n payment-context

# 3. Stop Order Service
kubectl scale deployment/order-service --replicas=0 -n order-context

# 4. Stop Product Service
kubectl scale deployment/product-service --replicas=0 -n product-context

# 5. Stop Customer Service
kubectl scale deployment/customer-service --replicas=0 -n customer-context
```

1. **Verify Shutdown**

```bash
# Verify no pods are running
kubectl get pods --all-namespaces --field-selector=status.phase=Running

# Check for any stuck pods
kubectl get pods --all-namespaces --field-selector=status.phase!=Succeeded,status.phase!=Failed
```

## Scaling Procedures

### Manual Scaling

**Scale Up**:

```bash
# Scale specific service
kubectl scale deployment/order-service --replicas=10 -n order-context

# Verify scaling
kubectl get hpa -n order-context
kubectl top pods -n order-context
```

**Scale Down**:

```bash
# Gradually scale down
kubectl scale deployment/order-service --replicas=3 -n order-context

# Monitor for issues
watch -n 5 'kubectl get pods -n order-context'
```

### Auto-Scaling Configuration

**Update HPA**:

```bash
# Update Horizontal Pod Autoscaler
kubectl autoscale deployment order-service \
  --cpu-percent=70 \
  --min=3 \
  --max=20 \
  -n order-context

# Verify HPA
kubectl get hpa order-service -n order-context
kubectl describe hpa order-service -n order-context
```

## Troubleshooting Procedures

### High CPU Usage

**Symptoms**:

- CPU utilization > 80%
- Slow response times
- Increased error rates

**Investigation**:

```bash
# 1. Identify affected pods
kubectl top pods --all-namespaces --sort-by=cpu

# 2. Check pod logs
kubectl logs <pod-name> -n <namespace> --tail=100

# 3. Get pod details
kubectl describe pod <pod-name> -n <namespace>

# 4. Check for CPU throttling
kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/<namespace>/pods/<pod-name>
```

**Resolution**:

```bash
# Option 1: Scale horizontally
kubectl scale deployment/<service> --replicas=<new-count> -n <namespace>

# Option 2: Increase CPU limits
kubectl set resources deployment/<service> \
  --limits=cpu=2000m \
  --requests=cpu=1000m \
  -n <namespace>

# Option 3: Restart pods
kubectl rollout restart deployment/<service> -n <namespace>
```

### High Memory Usage

**Symptoms**:

- Memory utilization > 85%
- OOMKilled pods
- Pod restarts

**Investigation**:

```bash
# 1. Check memory usage
kubectl top pods --all-namespaces --sort-by=memory

# 2. Check for memory leaks
kubectl logs <pod-name> -n <namespace> | grep -i "OutOfMemory"

# 3. Get heap dump (Java applications)
kubectl exec <pod-name> -n <namespace> -- \
  jmap -dump:format=b,file=/tmp/heap.hprof 1
```

**Resolution**:

```bash
# Option 1: Increase memory limits
kubectl set resources deployment/<service> \
  --limits=memory=2Gi \
  --requests=memory=1Gi \
  -n <namespace>

# Option 2: Restart affected pods
kubectl delete pod <pod-name> -n <namespace>

# Option 3: Scale horizontally
kubectl scale deployment/<service> --replicas=<new-count> -n <namespace>
```

### Database Connection Issues

**Symptoms**:

- "Too many connections" errors
- Connection timeouts
- Slow queries

**Investigation**:

```bash
# 1. Check active connections
psql -h <rds-endpoint> -U admin -d ecommerce \
  -c "SELECT count(*) FROM pg_stat_activity;"

# 2. Check connection pool status
kubectl logs <pod-name> -n <namespace> | grep -i "connection pool"

# 3. Check RDS metrics
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-prod-db \
  --query 'DBInstances[0].DBInstanceStatus'
```

**Resolution**:

```bash
# Option 1: Kill idle connections
psql -h <rds-endpoint> -U admin -d ecommerce \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle' AND state_change < now() - interval '10 minutes';"

# Option 2: Increase max connections (requires restart)
aws rds modify-db-parameter-group \
  --db-parameter-group-name ecommerce-params \
  --parameters "ParameterName=max_connections,ParameterValue=200,ApplyMethod=pending-reboot"

# Option 3: Optimize connection pool
kubectl set env deployment/<service> \
  SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20 \
  -n <namespace>
```

### Service Unavailable

**Symptoms**:

- 503 Service Unavailable errors
- No healthy pods
- Load balancer health checks failing

**Investigation**:

```bash
# 1. Check pod status
kubectl get pods -n <namespace>

# 2. Check pod events
kubectl get events -n <namespace> --sort-by='.lastTimestamp'

# 3. Check service endpoints
kubectl get endpoints <service-name> -n <namespace>

# 4. Check logs
kubectl logs <pod-name> -n <namespace> --previous
```

**Resolution**:

```bash
# Option 1: Rollback to previous version
kubectl rollout undo deployment/<service> -n <namespace>

# Option 2: Force pod restart
kubectl delete pod -l app=<service> -n <namespace>

# Option 3: Check and fix configuration
kubectl get configmap <service>-config -n <namespace> -o yaml
kubectl edit configmap <service>-config -n <namespace>
```

## Routine Maintenance Procedures

### Daily Maintenance Checklist

**Timing**: Execute at 2:00 AM UTC (low traffic period)

**Automated Tasks** (via cron or AWS Systems Manager):

1. **Log Management**

```bash
#!/bin/bash
# Script: /opt/maintenance/daily-log-rotation.sh
# Schedule: 0 2 * * * (Daily at 2:00 AM)

# Archive old logs to S3
aws logs create-export-task \
  --log-group-name /aws/eks/ecommerce-platform \
  --from $(date -d '7 days ago' +%s)000 \
  --to $(date -d '1 day ago' +%s)000 \
  --destination ecommerce-logs-archive \
  --destination-prefix logs/$(date +%Y/%m/%d)

# Clean up old CloudWatch log streams (>30 days)
aws logs describe-log-streams \
  --log-group-name /aws/eks/ecommerce-platform \
  --order-by LastEventTime \
  --descending \
  --query "logStreams[?lastEventTimestamp<\`$(date -d '30 days ago' +%s)000\`].logStreamName" \
  --output text | \
  xargs -I {} aws logs delete-log-stream \
    --log-group-name /aws/eks/ecommerce-platform \
    --log-stream-name {}

echo "Daily log rotation completed at $(date)"
```

1. **Health Check Verification**

```bash
#!/bin/bash
# Script: /opt/maintenance/daily-health-check.sh
# Schedule: 0 2 * * *

# Check all service health endpoints
SERVICES=("customer" "product" "order" "payment" "inventory")
FAILED_SERVICES=()

for service in "${SERVICES[@]}"; do
  if ! curl -f -s https://api.ecommerce-platform.com/api/v1/$service/health > /dev/null; then
    FAILED_SERVICES+=("$service")
  fi
done

# Alert if any service is unhealthy
if [ ${#FAILED_SERVICES[@]} -gt 0 ]; then
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
    --subject "Daily Health Check Failed" \
    --message "Failed services: ${FAILED_SERVICES[*]}"
fi

echo "Daily health check completed at $(date)"
```

1. **Disk Space Monitoring**

```bash
#!/bin/bash
# Script: /opt/maintenance/daily-disk-check.sh
# Schedule: 0 2 * * *

# Check EBS volume usage
kubectl get nodes -o json | \
  jq -r '.items[] | .metadata.name' | \
  while read node; do
    USAGE=$(kubectl describe node $node | grep -A 5 "Allocated resources" | grep "ephemeral-storage" | awk '{print $3}')
    if [ "${USAGE%\%}" -gt 80 ]; then
      aws sns publish \
        --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
        --subject "High Disk Usage Alert" \
        --message "Node $node disk usage: $USAGE"
    fi
  done

echo "Daily disk check completed at $(date)"
```

1. **Backup Verification**

```bash
#!/bin/bash
# Script: /opt/maintenance/daily-backup-verify.sh
# Schedule: 0 2 * * *

# Verify RDS automated backup
LATEST_BACKUP=$(aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-prod-db \
  --snapshot-type automated \
  --query 'DBSnapshots[0].{Time:SnapshotCreateTime,Status:Status}' \
  --output json)

BACKUP_AGE=$(echo $LATEST_BACKUP | jq -r '.Time' | xargs -I {} date -d {} +%s)
CURRENT_TIME=$(date +%s)
AGE_HOURS=$(( ($CURRENT_TIME - $BACKUP_AGE) / 3600 ))

if [ $AGE_HOURS -gt 26 ]; then
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
    --subject "Backup Age Alert" \
    --message "Latest backup is $AGE_HOURS hours old"
fi

echo "Daily backup verification completed at $(date)"
```

**Manual Verification Tasks**:

- [ ] Review CloudWatch dashboards for anomalies
- [ ] Check PagerDuty for overnight incidents
- [ ] Verify all automated maintenance scripts executed successfully
- [ ] Review error rate trends from previous 24 hours

### Weekly Maintenance Tasks

**Timing**: Execute on Sunday at 3:00 AM UTC

**Automated Tasks**:

1. **Cache Performance Analysis**

```bash
#!/bin/bash
# Script: /opt/maintenance/weekly-cache-analysis.sh
# Schedule: 0 3 * * 0 (Sunday at 3:00 AM)

# Check Redis cache hit rate
REDIS_ENDPOINT="ecommerce-redis.xxx.cache.amazonaws.com"
HIT_RATE=$(redis-cli -h $REDIS_ENDPOINT INFO stats | grep "keyspace_hits" | awk -F: '{print $2}')
MISS_RATE=$(redis-cli -h $REDIS_ENDPOINT INFO stats | grep "keyspace_misses" | awk -F: '{print $2}')

TOTAL=$((HIT_RATE + MISS_RATE))
if [ $TOTAL -gt 0 ]; then
  HIT_PERCENTAGE=$((HIT_RATE * 100 / TOTAL))
  
  if [ $HIT_PERCENTAGE -lt 80 ]; then
    aws sns publish \
      --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
      --subject "Low Cache Hit Rate" \
      --message "Cache hit rate: $HIT_PERCENTAGE% (target: >80%)"
  fi
fi

# Check memory usage
MEMORY_USED=$(redis-cli -h $REDIS_ENDPOINT INFO memory | grep "used_memory_human" | awk -F: '{print $2}')
MEMORY_MAX=$(redis-cli -h $REDIS_ENDPOINT INFO memory | grep "maxmemory_human" | awk -F: '{print $2}')

echo "Weekly cache analysis completed at $(date)"
echo "Hit rate: $HIT_PERCENTAGE%, Memory: $MEMORY_USED / $MEMORY_MAX"
```

1. **Database Statistics Update**

```bash
#!/bin/bash
# Script: /opt/maintenance/weekly-db-stats.sh
# Schedule: 0 3 * * 0

DB_ENDPOINT="ecommerce-prod-db.xxx.rds.amazonaws.com"

# Update table statistics
psql -h $DB_ENDPOINT -U admin -d ecommerce << EOF
-- Analyze all tables
ANALYZE VERBOSE;

-- Check for table bloat
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS external_size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 20;
EOF

echo "Weekly database statistics update completed at $(date)"
```

1. **Security Certificate Check**

```bash
#!/bin/bash
# Script: /opt/maintenance/weekly-cert-check.sh
# Schedule: 0 3 * * 0

# Check SSL certificate expiration
DOMAIN="api.ecommerce-platform.com"
EXPIRY_DATE=$(echo | openssl s_client -servername $DOMAIN -connect $DOMAIN:443 2>/dev/null | \
  openssl x509 -noout -enddate | cut -d= -f2)

EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_EPOCH - $CURRENT_EPOCH) / 86400 ))

if [ $DAYS_UNTIL_EXPIRY -lt 30 ]; then
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
    --subject "SSL Certificate Expiring Soon" \
    --message "Certificate for $DOMAIN expires in $DAYS_UNTIL_EXPIRY days"
fi

echo "Weekly certificate check completed at $(date)"
```

1. **Performance Metrics Report**

```bash
#!/bin/bash
# Script: /opt/maintenance/weekly-performance-report.sh
# Schedule: 0 3 * * 0

# Generate weekly performance report
aws cloudwatch get-metric-statistics \
  --namespace AWS/EKS \
  --metric-name CPUUtilization \
  --dimensions Name=ClusterName,Value=ecommerce-prod \
  --start-time $(date -d '7 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 3600 \
  --statistics Average,Maximum \
  --output json > /tmp/weekly-cpu-report.json

# Send report to S3
aws s3 cp /tmp/weekly-cpu-report.json \
  s3://ecommerce-ops-reports/weekly/$(date +%Y-%m-%d)/cpu-report.json

echo "Weekly performance report generated at $(date)"
```

**Manual Tasks**:

- [ ] Review weekly performance trends
- [ ] Check for security vulnerabilities in dependencies
- [ ] Review and update on-call rotation
- [ ] Verify backup restoration procedures (test restore)
- [ ] Review capacity planning metrics
- [ ] Update runbooks based on incidents from past week

### Monthly Maintenance Procedures

**Timing**: First Sunday of each month, 2:00 AM - 6:00 AM UTC

**Downtime Planning**:

- Notify stakeholders 1 week in advance
- Schedule during lowest traffic period
- Prepare rollback plan
- Have on-call team available

**Automated Tasks**:

1. **Database Maintenance**

```bash
#!/bin/bash
# Script: /opt/maintenance/monthly-db-maintenance.sh
# Schedule: 0 2 1-7 * 0 (First Sunday of month at 2:00 AM)

DB_ENDPOINT="ecommerce-prod-db.xxx.rds.amazonaws.com"

# Create maintenance window notification
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
  --subject "Monthly Database Maintenance Starting" \
  --message "Database maintenance window: 2:00 AM - 6:00 AM UTC"

# Vacuum full on large tables (requires downtime)
psql -h $DB_ENDPOINT -U admin -d ecommerce << EOF
-- Vacuum analyze all tables
VACUUM FULL ANALYZE;

-- Reindex all indexes
REINDEX DATABASE ecommerce;

-- Update statistics
ANALYZE;

-- Check for dead tuples
SELECT schemaname, tablename, n_dead_tup, n_live_tup,
       round(n_dead_tup * 100.0 / NULLIF(n_live_tup + n_dead_tup, 0), 2) AS dead_ratio
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY n_dead_tup DESC;
EOF

# Verify database health
psql -h $DB_ENDPOINT -U admin -d ecommerce -c "SELECT version();"

echo "Monthly database maintenance completed at $(date)"
```

1. **Index Optimization**

```bash
#!/bin/bash
# Script: /opt/maintenance/monthly-index-optimization.sh
# Schedule: 0 2 1-7 * 0

DB_ENDPOINT="ecommerce-prod-db.xxx.rds.amazonaws.com"

# Identify unused indexes
psql -h $DB_ENDPOINT -U admin -d ecommerce << EOF
-- Find unused indexes
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexname NOT LIKE '%_pkey'
ORDER BY pg_relation_size(indexrelid) DESC;

-- Find duplicate indexes
SELECT pg_size_pretty(SUM(pg_relation_size(idx))::BIGINT) AS size,
       (array_agg(idx))[1] AS idx1,
       (array_agg(idx))[2] AS idx2,
       (array_agg(idx))[3] AS idx3,
       (array_agg(idx))[4] AS idx4
FROM (
    SELECT indexrelid::regclass AS idx,
           (indrelid::text ||E'\n'|| indclass::text ||E'\n'|| indkey::text ||E'\n'||
            COALESCE(indexprs::text,'')||E'\n' || COALESCE(indpred::text,'')) AS key
    FROM pg_index
) sub
GROUP BY key
HAVING COUNT(*) > 1
ORDER BY SUM(pg_relation_size(idx)) DESC;
EOF

echo "Monthly index optimization analysis completed at $(date)"
```

1. **Security Patch Application**

```bash
#!/bin/bash
# Script: /opt/maintenance/monthly-security-patches.sh
# Schedule: 0 2 1-7 * 0

# Update EKS cluster version (if available)
CURRENT_VERSION=$(aws eks describe-cluster \
  --name ecommerce-prod \
  --query 'cluster.version' \
  --output text)

echo "Current EKS version: $CURRENT_VERSION"

# Check for available updates
aws eks describe-update \
  --name ecommerce-prod \
  --update-id latest

# Update node groups
aws eks update-nodegroup-version \
  --cluster-name ecommerce-prod \
  --nodegroup-name ecommerce-prod-nodes \
  --force

echo "Monthly security patches applied at $(date)"
```

1. **Capacity Planning Analysis**

```bash
#!/bin/bash
# Script: /opt/maintenance/monthly-capacity-analysis.sh
# Schedule: 0 2 1-7 * 0

# Analyze resource usage trends
aws cloudwatch get-metric-statistics \
  --namespace AWS/EKS \
  --metric-name CPUUtilization \
  --dimensions Name=ClusterName,Value=ecommerce-prod \
  --start-time $(date -d '30 days ago' -u +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 86400 \
  --statistics Average,Maximum \
  --output json > /tmp/monthly-capacity-report.json

# Generate capacity forecast
python3 /opt/maintenance/capacity-forecast.py \
  --input /tmp/monthly-capacity-report.json \
  --output /tmp/capacity-forecast.json

# Upload to S3
aws s3 cp /tmp/capacity-forecast.json \
  s3://ecommerce-ops-reports/monthly/$(date +%Y-%m)/capacity-forecast.json

echo "Monthly capacity analysis completed at $(date)"
```

**Manual Tasks**:

- [ ] Review and update disaster recovery plan
- [ ] Conduct security audit
- [ ] Review and optimize cloud costs
- [ ] Update documentation for any infrastructure changes
- [ ] Review SLA compliance metrics
- [ ] Conduct post-mortem reviews for major incidents
- [ ] Update capacity planning forecasts
- [ ] Review and update monitoring alerts

### Quarterly Maintenance and Upgrade Planning

**Timing**: First weekend of each quarter (January, April, July, October)

**Planning Phase** (2 weeks before):

1. **Upgrade Assessment**

```bash
#!/bin/bash
# Script: /opt/maintenance/quarterly-upgrade-assessment.sh

# Check for available upgrades
echo "=== Kubernetes Version Check ==="
kubectl version --short

echo "=== Available EKS Versions ==="
aws eks describe-addon-versions \
  --kubernetes-version $(kubectl version -o json | jq -r '.serverVersion.gitVersion' | sed 's/v//') \
  --query 'addons[*].addonName' \
  --output table

echo "=== RDS Engine Versions ==="
aws rds describe-db-engine-versions \
  --engine postgres \
  --engine-version 14 \
  --query 'DBEngineVersions[*].EngineVersion' \
  --output table

echo "=== Application Dependencies ==="
cd /opt/ecommerce-platform
./gradlew dependencyUpdates

echo "Quarterly upgrade assessment completed at $(date)"
```

1. **Risk Assessment**

- Identify breaking changes in upgrades
- Review compatibility matrix
- Assess impact on dependent services
- Create rollback plan

1. **Testing Plan**

- Schedule staging environment testing
- Define test scenarios
- Identify success criteria
- Plan load testing

**Execution Phase**:

1. **Staging Environment Upgrade**

```bash
#!/bin/bash
# Script: /opt/maintenance/quarterly-staging-upgrade.sh

# Upgrade staging environment
kubectl config use-context staging

# Update Kubernetes version
aws eks update-cluster-version \
  --name ecommerce-staging \
  --kubernetes-version 1.28

# Wait for upgrade to complete
aws eks wait cluster-active --name ecommerce-staging

# Update node groups
aws eks update-nodegroup-version \
  --cluster-name ecommerce-staging \
  --nodegroup-name ecommerce-staging-nodes

# Deploy updated applications
kubectl apply -f k8s/staging/

echo "Staging environment upgraded at $(date)"
```

1. **Production Upgrade** (with downtime window)

```bash
#!/bin/bash
# Script: /opt/maintenance/quarterly-production-upgrade.sh

# Notify stakeholders
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
  --subject "Production Upgrade Starting" \
  --message "Maintenance window: 2:00 AM - 6:00 AM UTC"

# Create backup before upgrade
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier pre-upgrade-$(date +%Y%m%d)

# Upgrade production cluster
kubectl config use-context production

aws eks update-cluster-version \
  --name ecommerce-prod \
  --kubernetes-version 1.28

# Monitor upgrade progress
watch -n 30 'aws eks describe-cluster --name ecommerce-prod --query cluster.status'

echo "Production upgrade completed at $(date)"
```

**Post-Upgrade Tasks**:

- [ ] Verify all services are healthy
- [ ] Run smoke tests
- [ ] Monitor error rates for 24 hours
- [ ] Update documentation
- [ ] Conduct post-upgrade review

### Annual Database Health Assessment

**Timing**: First weekend of January

**Comprehensive Database Review**:

1. **Performance Analysis**

```bash
#!/bin/bash
# Script: /opt/maintenance/annual-db-health-assessment.sh

DB_ENDPOINT="ecommerce-prod-db.xxx.rds.amazonaws.com"

# Generate comprehensive health report
psql -h $DB_ENDPOINT -U admin -d ecommerce << EOF
-- Database size and growth
SELECT 
  pg_database.datname,
  pg_size_pretty(pg_database_size(pg_database.datname)) AS size
FROM pg_database
ORDER BY pg_database_size(pg_database.datname) DESC;

-- Table sizes and row counts
SELECT 
  schemaname,
  tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS total_size,
  pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) AS table_size,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) AS index_size,
  n_live_tup AS row_count
FROM pg_tables
JOIN pg_stat_user_tables USING (schemaname, tablename)
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
LIMIT 50;

-- Slow queries analysis
SELECT 
  query,
  calls,
  total_time,
  mean_time,
  max_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 20;

-- Index usage statistics
SELECT 
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch,
  pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC
LIMIT 50;

-- Connection statistics
SELECT 
  datname,
  count(*) AS connections,
  max(backend_start) AS oldest_connection
FROM pg_stat_activity
GROUP BY datname;

-- Lock analysis
SELECT 
  locktype,
  database,
  relation::regclass,
  mode,
  count(*)
FROM pg_locks
GROUP BY locktype, database, relation, mode
ORDER BY count(*) DESC;
EOF

echo "Annual database health assessment completed at $(date)"
```

1. **Capacity Planning**

- Analyze 12-month growth trends
- Project storage requirements for next year
- Plan for compute resource scaling
- Budget planning for infrastructure costs

1. **Disaster Recovery Testing**

```bash
#!/bin/bash
# Script: /opt/maintenance/annual-dr-test.sh

# Full disaster recovery drill
echo "Starting annual DR test at $(date)"

# 1. Create test snapshot
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod-db \
  --db-snapshot-identifier dr-test-$(date +%Y%m%d)

# 2. Restore to test instance
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-dr-test \
  --db-snapshot-identifier dr-test-$(date +%Y%m%d)

# 3. Wait for restoration
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-dr-test

# 4. Verify data integrity
psql -h ecommerce-dr-test.xxx.rds.amazonaws.com -U admin -d ecommerce << EOF
SELECT count(*) FROM customers;
SELECT count(*) FROM orders;
SELECT count(*) FROM products;
EOF

# 5. Clean up test instance
aws rds delete-db-instance \
  --db-instance-identifier ecommerce-dr-test \
  --skip-final-snapshot

echo "Annual DR test completed at $(date)"
```

1. **Security Audit**

- Review IAM policies and roles
- Audit database user permissions
- Review network security groups
- Scan for security vulnerabilities
- Update security documentation

**Annual Review Checklist**:

- [ ] Review and update architecture documentation
- [ ] Conduct cost optimization analysis
- [ ] Review SLA compliance for the year
- [ ] Update disaster recovery procedures
- [ ] Review and update monitoring strategies
- [ ] Conduct team training on new procedures
- [ ] Update capacity planning for next year
- [ ] Review and optimize backup strategies

### Maintenance Window Planning

**Standard Maintenance Windows**:

| Frequency | Day | Time (UTC) | Duration | Downtime |
|-----------|-----|------------|----------|----------|
| Daily | Every day | 2:00 AM | 30 min | No |
| Weekly | Sunday | 3:00 AM | 1 hour | No |
| Monthly | First Sunday | 2:00 AM | 4 hours | Partial (15 min) |
| Quarterly | First weekend | 2:00 AM | 8 hours | Yes (2 hours) |
| Annual | First weekend of Jan | 2:00 AM | 12 hours | Yes (4 hours) |

**Communication Plan**:

1. **Advance Notification**

```bash
#!/bin/bash
# Script: /opt/maintenance/notify-maintenance-window.sh

MAINTENANCE_TYPE=$1  # daily, weekly, monthly, quarterly, annual
MAINTENANCE_DATE=$2

case $MAINTENANCE_TYPE in
  daily)
    # No notification needed for daily maintenance
    ;;
  weekly)
    # Notify 24 hours in advance
    aws sns publish \
      --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
      --subject "Weekly Maintenance Scheduled" \
      --message "Weekly maintenance scheduled for $MAINTENANCE_DATE at 3:00 AM UTC. No downtime expected."
    ;;
  monthly)
    # Notify 1 week in advance
    aws sns publish \
      --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
      --subject "Monthly Maintenance Scheduled" \
      --message "Monthly maintenance scheduled for $MAINTENANCE_DATE at 2:00 AM UTC. Expect 15 minutes of downtime."
    
    # Post to status page
    curl -X POST https://status.ecommerce-platform.com/api/scheduled-maintenances \
      -H "Authorization: Bearer $STATUS_PAGE_TOKEN" \
      -d "{\"scheduled_for\": \"$MAINTENANCE_DATE\", \"duration\": 240, \"message\": \"Monthly database maintenance\"}"
    ;;
  quarterly)
    # Notify 2 weeks in advance
    aws sns publish \
      --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
      --subject "Quarterly Maintenance Scheduled" \
      --message "Quarterly upgrade maintenance scheduled for $MAINTENANCE_DATE. Expect 2 hours of downtime."
    
    # Email to all stakeholders
    aws ses send-email \
      --from ops@ecommerce-platform.com \
      --to stakeholders@ecommerce-platform.com \
      --subject "Quarterly Maintenance Notification" \
      --text "Quarterly maintenance scheduled for $MAINTENANCE_DATE at 2:00 AM UTC. Expected downtime: 2 hours."
    ;;
  annual)
    # Notify 1 month in advance
    aws sns publish \
      --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
      --subject "Annual Maintenance Scheduled" \
      --message "Annual comprehensive maintenance scheduled for $MAINTENANCE_DATE. Expect 4 hours of downtime."
    ;;
esac

echo "Maintenance notification sent for $MAINTENANCE_TYPE maintenance on $MAINTENANCE_DATE"
```

1. **Status Updates During Maintenance**

```bash
#!/bin/bash
# Script: /opt/maintenance/maintenance-status-update.sh

STATUS=$1  # started, in-progress, completed, failed
MESSAGE=$2

# Update status page
curl -X PATCH https://status.ecommerce-platform.com/api/incidents/current \
  -H "Authorization: Bearer $STATUS_PAGE_TOKEN" \
  -d "{\"status\": \"$STATUS\", \"message\": \"$MESSAGE\"}"

# Post to Slack
curl -X POST $SLACK_WEBHOOK_URL \
  -d "{\"text\": \"🔧 Maintenance Update: $MESSAGE (Status: $STATUS)\"}"

echo "Status updated: $STATUS - $MESSAGE"
```

### Maintenance Task Automation

**AWS Systems Manager Configuration**:

1. **Create Maintenance Window**

```bash
# Create daily maintenance window
aws ssm create-maintenance-window \
  --name "DailyMaintenance" \
  --schedule "cron(0 2 * * ? *)" \
  --duration 1 \
  --cutoff 0 \
  --allow-unassociated-targets

# Create weekly maintenance window
aws ssm create-maintenance-window \
  --name "WeeklyMaintenance" \
  --schedule "cron(0 3 ? * SUN *)" \
  --duration 2 \
  --cutoff 0 \
  --allow-unassociated-targets

# Create monthly maintenance window
aws ssm create-maintenance-window \
  --name "MonthlyMaintenance" \
  --schedule "cron(0 2 ? * SUN#1 *)" \
  --duration 4 \
  --cutoff 0 \
  --allow-unassociated-targets
```

1. **Register Maintenance Tasks**

```bash
# Register daily log rotation task
aws ssm register-task-with-maintenance-window \
  --window-id mw-0123456789abcdef0 \
  --task-type RUN_COMMAND \
  --task-arn "AWS-RunShellScript" \
  --service-role-arn "arn:aws:iam::123456789012:role/MaintenanceWindowRole" \
  --task-invocation-parameters '{
    "RunCommand": {
      "Parameters": {
        "commands": ["/opt/maintenance/daily-log-rotation.sh"]
      }
    }
  }' \
  --priority 1 \
  --max-concurrency 1 \
  --max-errors 0

# Register weekly cache analysis task
aws ssm register-task-with-maintenance-window \
  --window-id mw-0123456789abcdef1 \
  --task-type RUN_COMMAND \
  --task-arn "AWS-RunShellScript" \
  --service-role-arn "arn:aws:iam::123456789012:role/MaintenanceWindowRole" \
  --task-invocation-parameters '{
    "RunCommand": {
      "Parameters": {
        "commands": ["/opt/maintenance/weekly-cache-analysis.sh"]
      }
    }
  }' \
  --priority 1 \
  --max-concurrency 1 \
  --max-errors 0
```

1. **Cron Job Configuration** (Alternative to Systems Manager)

```bash
# Install maintenance scripts
sudo mkdir -p /opt/maintenance
sudo cp scripts/*.sh /opt/maintenance/
sudo chmod +x /opt/maintenance/*.sh

# Configure crontab
sudo crontab -e

# Add maintenance jobs
# Daily maintenance (2:00 AM)
0 2 * * * /opt/maintenance/daily-log-rotation.sh >> /var/log/maintenance/daily.log 2>&1
0 2 * * * /opt/maintenance/daily-health-check.sh >> /var/log/maintenance/daily.log 2>&1
0 2 * * * /opt/maintenance/daily-disk-check.sh >> /var/log/maintenance/daily.log 2>&1
0 2 * * * /opt/maintenance/daily-backup-verify.sh >> /var/log/maintenance/daily.log 2>&1

# Weekly maintenance (Sunday 3:00 AM)
0 3 * * 0 /opt/maintenance/weekly-cache-analysis.sh >> /var/log/maintenance/weekly.log 2>&1
0 3 * * 0 /opt/maintenance/weekly-db-stats.sh >> /var/log/maintenance/weekly.log 2>&1
0 3 * * 0 /opt/maintenance/weekly-cert-check.sh >> /var/log/maintenance/weekly.log 2>&1
0 3 * * 0 /opt/maintenance/weekly-performance-report.sh >> /var/log/maintenance/weekly.log 2>&1

# Monthly maintenance (First Sunday 2:00 AM)
0 2 1-7 * 0 /opt/maintenance/monthly-db-maintenance.sh >> /var/log/maintenance/monthly.log 2>&1
0 2 1-7 * 0 /opt/maintenance/monthly-index-optimization.sh >> /var/log/maintenance/monthly.log 2>&1
0 2 1-7 * 0 /opt/maintenance/monthly-security-patches.sh >> /var/log/maintenance/monthly.log 2>&1
0 2 1-7 * 0 /opt/maintenance/monthly-capacity-analysis.sh >> /var/log/maintenance/monthly.log 2>&1
```

1. **Monitoring Maintenance Jobs**

```bash
#!/bin/bash
# Script: /opt/maintenance/monitor-maintenance-jobs.sh

# Check if maintenance jobs are running
RUNNING_JOBS=$(ps aux | grep -E "daily-|weekly-|monthly-" | grep -v grep | wc -l)

if [ $RUNNING_JOBS -gt 0 ]; then
  echo "Maintenance jobs currently running: $RUNNING_JOBS"
  ps aux | grep -E "daily-|weekly-|monthly-" | grep -v grep
fi

# Check last execution times
echo "Last maintenance execution times:"
ls -lht /var/log/maintenance/*.log | head -10

# Check for failed jobs
FAILED_JOBS=$(grep -i "error\|failed" /var/log/maintenance/*.log | tail -20)
if [ -n "$FAILED_JOBS" ]; then
  echo "Recent failures detected:"
  echo "$FAILED_JOBS"
  
  # Alert on failures
  aws sns publish \
    --topic-arn arn:aws:sns:us-east-1:123456789012:ops-alerts \
    --subject "Maintenance Job Failures Detected" \
    --message "$FAILED_JOBS"
fi
```

## Emergency Procedures

### Emergency Rollback

**When to Use**:

- Critical bugs in production
- Performance degradation
- Data corruption

**Steps**:

```bash
# 1. Identify current version
kubectl rollout history deployment/<service> -n <namespace>

# 2. Rollback to previous version
kubectl rollout undo deployment/<service> -n <namespace>

# 3. Verify rollback
kubectl rollout status deployment/<service> -n <namespace>

# 4. Check application health
curl https://api.ecommerce-platform.com/api/v1/<service>/health

# 5. Monitor metrics
watch -n 5 'kubectl top pods -n <namespace>'
```

### Emergency Scale Down

**When to Use**:

- Cost overrun
- Resource exhaustion
- DDoS attack mitigation

**Steps**:

```bash
# 1. Scale down non-critical services
kubectl scale deployment --all --replicas=1 -n notification-context
kubectl scale deployment --all --replicas=1 -n search-context

# 2. Reduce critical service replicas
kubectl scale deployment/order-service --replicas=3 -n order-context

# 3. Enable rate limiting
kubectl apply -f rate-limit-config.yaml

# 4. Monitor impact
watch -n 5 'kubectl get hpa --all-namespaces'
```

## Runbook Links

### Service-Specific Runbooks

- [Order Service Runbook](https://runbooks.ecommerce-platform.com/order-service)
- [Customer Service Runbook](https://runbooks.ecommerce-platform.com/customer-service)
- [Payment Service Runbook](https://runbooks.ecommerce-platform.com/payment-service)
- [Product Service Runbook](https://runbooks.ecommerce-platform.com/product-service)

### Infrastructure Runbooks

- [EKS Cluster Management](https://runbooks.ecommerce-platform.com/eks-cluster)
- [RDS Database Management](https://runbooks.ecommerce-platform.com/rds-database)
- [Redis Cache Management](https://runbooks.ecommerce-platform.com/redis-cache)
- [Kafka Management](https://runbooks.ecommerce-platform.com/kafka)

## Related Documentation

- [Operational Overview](overview.md) - Overall operational approach
- [Monitoring and Alerting](monitoring-alerting.md) - Monitoring strategies
- [Backup and Recovery](backup-recovery.md) - Backup and recovery procedures
- [Deployment Process](../deployment/deployment-process.md) - Deployment procedures

---

**Document Version**: 2.0  
**Last Updated**: 2025-01-22  
**Owner**: Operations Team  
**Change History**:

- 2025-01-22: Expanded routine maintenance procedures with daily, weekly, monthly, quarterly, and annual tasks
- 2025-10-23: Initial version with basic operational procedures

</content>
</file></system_warning>

完美！現在更新任務狀態為完成：

<function_calls>
<invoke name="taskStatus">
<parameter name="taskFilePath">.kiro/specs/documentation-redesign/tasks.md
