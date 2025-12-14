# Runbook: Service Outage

## Symptoms

- Service health check failing
- 503/504 errors from load balancer
- All API endpoints returning errors
- Zero successful requests

## Impact

- **Severity**: P0 - Critical
- **Affected Users**: All users
- **Business Impact**: Complete service unavailability, revenue loss

## Detection

- **Alert**: `APIEndpointDown` alert fires
- **Monitoring Dashboard**: Operations Dashboard shows service down
- **Log Patterns**: Connection refused, service unavailable

## Diagnosis

### Step 1: Check Service Status

```bash
# Check pod status
kubectl get pods -n production -l app=ecommerce-backend

# Check service endpoints
kubectl get endpoints ecommerce-backend -n production

# Check load balancer
kubectl get svc ecommerce-backend -n production
```

### Step 2: Check Recent Changes

```bash
# Check recent deployments
kubectl rollout history deployment/ecommerce-backend -n production

# Check recent events
kubectl get events -n production --sort-by='.lastTimestamp' | head -20
```

### Step 3: Check Dependencies

```bash
# Check database connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  pg_isready -h ${DB_HOST} -p 5432

# Check Redis connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  redis-cli -h ${REDIS_HOST} ping

# Check Kafka connectivity
kubectl exec -it ${POD_NAME} -n production -- \
  kafka-broker-api-versions --bootstrap-server ${KAFKA_BOOTSTRAP}
```

## Resolution

### Immediate Actions

1. **Notify stakeholders**:

```bash
# Send critical incident notification
# Subject: P0 - Service Outage
# All services are currently unavailable. Team is investigating.
```

1. **Check pod logs**:

```bash
kubectl logs -f deployment/ecommerce-backend -n production --tail=100
```

1. **Restart pods if needed**:

```bash
kubectl rollout restart deployment/ecommerce-backend -n production
```

### Root Cause Fixes

#### If caused by failed deployment

```bash
# Rollback to previous version
kubectl rollout undo deployment/ecommerce-backend -n production
```

#### If caused by configuration issue

```bash
# Restore previous configuration
kubectl apply -f previous-config.yaml
kubectl rollout restart deployment/ecommerce-backend -n production
```

#### If caused by dependency failure

```bash
# Check and fix database/Redis/Kafka
# See respective runbooks
```

## Verification

- [ ] All pods are running
- [ ] Health checks passing
- [ ] API endpoints responding
- [ ] Smoke tests passing
- [ ] Error rate < 1%
- [ ] Response times normal

## Prevention

1. **Deployment safety**:
   - Use canary deployments
   - Implement proper health checks
   - Test in staging first

2. **Dependency resilience**:
   - Implement circuit breakers
   - Add retry logic
   - Use fallback mechanisms

3. **Monitoring**:
   - Comprehensive health checks
   - Dependency monitoring
   - Automated rollback on failure

## Escalation

- **Immediate**: Notify on-call engineer via PagerDuty
- **5 minutes**: Escalate to team lead
- **15 minutes**: Escalate to engineering manager

## Related

- [Failed Deployment](failed-deployment.md)
- [Database Connection Issues](database-connection-issues.md)
- [Rollback Procedures](../deployment/rollback.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team
