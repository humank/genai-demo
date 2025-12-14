# Runbook: High CPU Usage

## Symptoms

- CPU utilization > 80% for extended period
- Slow API response times
- Increased request latency
- Pod throttling warnings

## Impact

- **Severity**: P1 - High
- **Affected Users**: All users may experience slower response times
- **Business Impact**: Degraded user experience, potential timeout errors

## Detection

- **Alert**: `HighCPUUsage` alert fires
- **Monitoring Dashboard**: Operations Dashboard > Infrastructure > CPU Utilization
- **Log Patterns**: `WARN` messages about slow processing

## Diagnosis

### Step 1: Identify Affected Pods

```bash
# Check CPU usage across all pods
kubectl top pods -n production -l app=ecommerce-backend

# Get detailed pod metrics
kubectl describe pod ${POD_NAME} -n production
```

### Step 2: Check Recent Changes

```bash
# Check recent deployments
kubectl rollout history deployment/ecommerce-backend -n production

# Check recent configuration changes
kubectl get configmap ecommerce-config -n production -o yaml
```

### Step 3: Analyze Application Logs

```bash
# Check for CPU-intensive operations
kubectl logs ${POD_NAME} -n production --tail=500 | grep -i "slow\|timeout\|processing"

# Check for infinite loops or stuck processes
kubectl logs ${POD_NAME} -n production --tail=1000 | grep -i "error\|exception"
```

### Step 4: Profile Application

```bash
# Get thread dump
kubectl exec -it ${POD_NAME} -n production -- jstack 1 > thread-dump.txt

# Check for CPU-intensive threads
grep "runnable" thread-dump.txt | sort | uniq -c | sort -rn
```

## Resolution

### Immediate Actions

1. **Scale horizontally** to distribute load:

```bash
kubectl scale deployment/ecommerce-backend --replicas=8 -n production
```

1. **Monitor impact**:

```bash
# Watch CPU usage
watch kubectl top pods -n production -l app=ecommerce-backend
```

### Root Cause Fix

#### If caused by inefficient code

1. Identify CPU-intensive code from profiling
2. Optimize algorithm or query
3. Deploy fix through normal deployment process

#### If caused by increased load

1. Verify load is legitimate (not attack)
2. Adjust HPA settings:

```bash
kubectl edit hpa ecommerce-backend-hpa -n production
# Increase maxReplicas if needed
```

#### If caused by resource limits

1. Review and adjust resource requests/limits:

```yaml
resources:
  requests:
    cpu: "500m"
  limits:
    cpu: "1000m"  # Increase if needed
```

## Verification

- [ ] CPU usage drops below 70%
- [ ] API response times return to normal (< 2s)
- [ ] No throttling warnings in logs
- [ ] Application metrics stable
- [ ] No new alerts triggered

## Prevention

1. **Implement proper resource limits**:
   - Set appropriate CPU requests and limits
   - Use HPA for automatic scaling

2. **Code optimization**:
   - Profile code regularly
   - Optimize database queries
   - Implement caching where appropriate

3. **Load testing**:
   - Regular load testing to identify bottlenecks
   - Capacity planning based on growth projections

4. **Monitoring**:
   - Set up alerts for gradual CPU increase
   - Monitor CPU trends over time

## Escalation

- **L1 Support**: DevOps team
- **L2 Support**: Backend engineering team
- **On-Call Engineer**: Check PagerDuty

## Related

- [High Memory Usage](high-memory-usage.md)
- [Slow API Responses](slow-api-responses.md)
- [Scaling Operations](scaling-operations.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team
