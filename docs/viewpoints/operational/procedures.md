---
title: "Operational Procedures"
viewpoint: "Operational"
status: "active"
last_updated: "2025-10-23"
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

2. **Start Core Services** (in order)
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

3. **Verify Service Health**
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

4. **Enable Traffic**
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

2. **Drain Traffic**
```bash
# Stop accepting new requests
kubectl annotate service api-gateway service.beta.kubernetes.io/aws-load-balancer-backend-protocol=none

# Wait for in-flight requests to complete (5 minutes)
sleep 300
```

3. **Shutdown Services** (reverse order)
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

4. **Verify Shutdown**
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

## Maintenance Procedures

### Database Maintenance

**Monthly Tasks**:
```bash
# 1. Vacuum and analyze
psql -h <rds-endpoint> -U admin -d ecommerce \
  -c "VACUUM ANALYZE;"

# 2. Reindex tables
psql -h <rds-endpoint> -U admin -d ecommerce \
  -c "REINDEX DATABASE ecommerce;"

# 3. Update statistics
psql -h <rds-endpoint> -U admin -d ecommerce \
  -c "ANALYZE;"

# 4. Check for bloat
psql -h <rds-endpoint> -U admin -d ecommerce \
  -f check-table-bloat.sql
```

### Cache Maintenance

**Weekly Tasks**:
```bash
# 1. Check cache hit rate
redis-cli -h <redis-endpoint> INFO stats | grep hit_rate

# 2. Check memory usage
redis-cli -h <redis-endpoint> INFO memory

# 3. Clear expired keys
redis-cli -h <redis-endpoint> --scan --pattern "expired:*" | xargs redis-cli DEL

# 4. Verify replication
redis-cli -h <redis-endpoint> INFO replication
```

### Log Rotation

**Daily Tasks**:
```bash
# 1. Archive old logs to S3
aws logs create-export-task \
  --log-group-name /aws/eks/ecommerce-platform \
  --from $(date -d '7 days ago' +%s)000 \
  --to $(date -d '1 day ago' +%s)000 \
  --destination ecommerce-logs-archive

# 2. Delete old log streams
aws logs delete-log-stream \
  --log-group-name /aws/eks/ecommerce-platform \
  --log-stream-name <old-stream-name>
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

**Document Version**: 1.0  
**Last Updated**: 2025-10-23  
**Owner**: Operations Team
