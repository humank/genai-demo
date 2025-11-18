# CloudWatch Synthetics Operations Runbook

> **Last Updated**: 2025-10-22  
> **Status**: ✅ Active  
> **Audience**: DevOps Engineers, SRE Team

## Quick Reference

| Scenario | Severity | Response Time | Escalation |
|----------|----------|---------------|------------|
| Canary Failure | Critical | < 5 minutes | Immediate |
| High Latency | Warning | < 15 minutes | After 2 occurrences |
| Canary Timeout | Warning | < 15 minutes | After 3 occurrences |
| Multiple Failures | Critical | < 2 minutes | Immediate + Manager |

## Incident Response Procedures

### Scenario 1: API Health Check Canary Failure

**Alert**: `CRITICAL: CloudWatch Synthetics Canary Failed - production-api-health-check`

**Immediate Actions** (< 5 minutes):

1. **Verify Alert Legitimacy**
   ```bash
   # Check canary status
   aws synthetics get-canary-runs \
       --name production-api-health-check \
       --max-results 5
   
   # Check recent logs
   aws logs tail /aws/lambda/cwsyn-production-api-health-check \
       --since 5m
   ```

2. **Check Application Health**
   ```bash
   # Test endpoint directly
   curl -v https://api.genai-demo.com/actuator/health
   
   # Check EKS pod status
   kubectl get pods -n genai-demo
   
   # Check service status
   kubectl get svc -n genai-demo
   ```

3. **Assess Impact**
   - Is the application actually down?
   - Are users affected?
   - Is this a false positive?

**Investigation Steps**:

1. **Review Canary Artifacts**
   ```bash
   # Download latest artifacts
   aws s3 ls s3://production-synthetics-canary-artifacts-{account-id}/production-api-health-check/ \
       --recursive | tail -5
   
   # Download specific run
   aws s3 cp s3://production-synthetics-canary-artifacts-{account-id}/production-api-health-check/{run-id}/ \
       ./artifacts/ --recursive
   ```

2. **Check Related Services**
   ```bash
   # Check ALB target health
   aws elbv2 describe-target-health \
       --target-group-arn {target-group-arn}
   
   # Check Aurora database
   aws rds describe-db-instances \
       --db-instance-identifier genai-demo-primary
   
   # Check Redis cluster
   aws elasticache describe-replication-groups \
       --replication-group-id genai-demo-redis
   ```

3. **Review Recent Changes**
   ```bash
   # Check recent deployments
   kubectl rollout history deployment/genai-demo-backend -n genai-demo
   
   # Check ArgoCD sync status
   argocd app get genai-demo-backend
   ```

**Resolution Actions**:

**If Application is Down**:
```bash
# Restart pods
kubectl rollout restart deployment/genai-demo-backend -n genai-demo

# Check pod logs
kubectl logs -f deployment/genai-demo-backend -n genai-demo --tail=100

# If needed, rollback deployment
kubectl rollout undo deployment/genai-demo-backend -n genai-demo
```

**If False Positive**:
```bash
# Restart canary
aws synthetics stop-canary --name production-api-health-check
sleep 30
aws synthetics start-canary --name production-api-health-check
```

**Post-Incident**:
- Document root cause
- Update runbook if needed
- Review canary configuration
- Implement preventive measures

### Scenario 2: High Latency Alert

**Alert**: `WARNING: High API Latency Detected - production-api-health-check`

**Immediate Actions** (< 15 minutes):

1. **Verify Latency Issue**
   ```bash
   # Check canary metrics
   aws cloudwatch get-metric-statistics \
       --namespace CloudWatchSynthetics \
       --metric-name Duration \
       --dimensions Name=CanaryName,Value=production-api-health-check \
       --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
       --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
       --period 300 \
       --statistics Average,Maximum
   ```

2. **Check Application Performance**
   ```bash
   # Check pod resource usage
   kubectl top pods -n genai-demo
   
   # Check node resource usage
   kubectl top nodes
   
   # Check HPA status
   kubectl get hpa -n genai-demo
   ```

3. **Identify Bottleneck**
   ```bash
   # Check database performance
   aws rds describe-db-instances \
       --db-instance-identifier genai-demo-primary \
       --query 'DBInstances[0].{CPU:CPUUtilization,Connections:DatabaseConnections}'
   
   # Check slow queries
   aws rds describe-db-log-files \
       --db-instance-identifier genai-demo-primary
   ```

**Investigation Steps**:

1. **Review X-Ray Traces**
   - Navigate to X-Ray console
   - Filter traces by high latency
   - Identify slow service calls

2. **Check CloudWatch Metrics**
   ```bash
   # Check application metrics
   aws cloudwatch get-metric-statistics \
       --namespace GenAIDemo/Application \
       --metric-name ResponseTime \
       --dimensions Name=Environment,Value=production \
       --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
       --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
       --period 300 \
       --statistics Average,p95,p99
   ```

3. **Analyze Database Queries**
   ```sql
   -- Connect to Aurora and check slow queries
   SELECT query, calls, total_time, mean_time
   FROM pg_stat_statements
   ORDER BY mean_time DESC
   LIMIT 10;
   ```

**Resolution Actions**:

**If Resource Constrained**:
```bash
# Scale up pods
kubectl scale deployment/genai-demo-backend --replicas=10 -n genai-demo

# Check auto-scaling
kubectl describe hpa genai-demo-backend-hpa -n genai-demo
```

**If Database Issue**:
```bash
# Check connection pool
kubectl exec -it deployment/genai-demo-backend -n genai-demo -- \
    curl localhost:8080/actuator/metrics/hikaricp.connections.active

# Restart application to reset connections
kubectl rollout restart deployment/genai-demo-backend -n genai-demo
```

**If Cache Issue**:
```bash
# Check Redis performance
aws elasticache describe-cache-clusters \
    --cache-cluster-id genai-demo-redis-001 \
    --show-cache-node-info

# Clear cache if needed (use with caution)
redis-cli -h {redis-endpoint} FLUSHDB
```

### Scenario 3: Multiple Canary Failures

**Alert**: `CRITICAL: Multiple Synthetics Canaries Failed`

**Immediate Actions** (< 2 minutes):

1. **Declare Incident**
   ```bash
   # Create incident in PagerDuty/Slack
   # Notify on-call manager
   # Start incident bridge
   ```

2. **Quick Health Check**
   ```bash
   # Check all critical services
   for service in backend frontend database redis kafka; do
       echo "Checking $service..."
       kubectl get pods -l app=$service -n genai-demo
   done
   ```

3. **Check Infrastructure**
   ```bash
   # Check EKS cluster health
   aws eks describe-cluster --name genai-demo-production
   
   # Check ALB health
   aws elbv2 describe-load-balancers
   
   # Check VPC connectivity
   aws ec2 describe-vpc-peering-connections
   ```

**Investigation Steps**:

1. **Identify Common Failure Pattern**
   - All canaries failing → Infrastructure issue
   - Specific endpoint pattern → Application issue
   - Intermittent failures → Network issue

2. **Check AWS Service Health**
   - Visit AWS Service Health Dashboard
   - Check for regional outages
   - Review AWS status page

3. **Review Recent Changes**
   ```bash
   # Check recent deployments
   argocd app list
   
   # Check CDK deployments
   aws cloudformation describe-stacks \
       --stack-name ObservabilityStack
   
   # Check recent configuration changes
   kubectl get configmap -n genai-demo -o yaml
   ```

**Resolution Actions**:

**If Infrastructure Issue**:
```bash
# Check and restart critical services
kubectl get pods -n genai-demo | grep -v Running

# Restart failed pods
kubectl delete pod {pod-name} -n genai-demo

# Check node health
kubectl describe nodes | grep -A 5 Conditions
```

**If Network Issue**:
```bash
# Check security groups
aws ec2 describe-security-groups \
    --group-ids {security-group-id}

# Check network ACLs
aws ec2 describe-network-acls

# Test connectivity
kubectl run test-pod --image=busybox -it --rm -- \
    wget -O- http://genai-demo-backend:8080/actuator/health
```

**If Regional Outage**:
```bash
# Failover to DR region (if configured)
# Update Route 53 health checks
aws route53 change-resource-record-sets \
    --hosted-zone-id {zone-id} \
    --change-batch file://failover-config.json

# Notify stakeholders
# Follow disaster recovery procedures
```

## Maintenance Procedures

### Updating Canary Configuration

**Before Deployment**:
1. Test canary script locally
2. Review changes with team
3. Plan deployment window
4. Notify stakeholders

**Deployment Steps**:
```bash
# 1. Update CDK stack
cd infrastructure
npm run build

# 2. Review changes
cdk diff ObservabilityStack

# 3. Deploy changes
cdk deploy ObservabilityStack

# 4. Verify canaries
aws synthetics describe-canaries

# 5. Monitor first runs
aws synthetics get-canary-runs \
    --name production-api-health-check \
    --max-results 3
```

**Rollback Procedure**:
```bash
# Revert CDK changes
git revert {commit-hash}

# Redeploy previous version
cdk deploy ObservabilityStack

# Verify rollback
aws synthetics describe-canaries
```

### Adding New Canary

**Planning**:
1. Identify endpoint to monitor
2. Define success criteria
3. Set latency thresholds
4. Plan alert routing

**Implementation**:
```typescript
// Add to criticalEndpoints array
{
    path: '/api/v1/new-endpoint',
    method: 'GET',
    expectedStatusCode: 200,
    maxLatencyMs: 3000,
}
```

**Validation**:
```bash
# Deploy and verify
cdk deploy ObservabilityStack

# Check canary creation
aws synthetics describe-canaries \
    --names production-business-process-{index}

# Monitor first runs
aws synthetics get-canary-runs \
    --name production-business-process-{index} \
    --max-results 5
```

### Removing Canary

**Before Removal**:
1. Verify canary is no longer needed
2. Check for dependent alerts
3. Archive historical data
4. Update documentation

**Removal Steps**:
```bash
# 1. Stop canary
aws synthetics stop-canary \
    --name production-old-canary

# 2. Remove from CDK stack
# (Remove from criticalEndpoints array)

# 3. Deploy changes
cdk deploy ObservabilityStack

# 4. Verify removal
aws synthetics describe-canaries
```

## Monitoring and Reporting

### Daily Health Check

```bash
#!/bin/bash
# daily-synthetics-health-check.sh

echo "CloudWatch Synthetics Daily Health Check"
echo "=========================================="

# Get all canaries
canaries=$(aws synthetics describe-canaries --query 'Canaries[*].Name' --output text)

for canary in $canaries; do
    echo "Checking $canary..."
    
    # Get success rate
    success_rate=$(aws cloudwatch get-metric-statistics \
        --namespace CloudWatchSynthetics \
        --metric-name SuccessPercent \
        --dimensions Name=CanaryName,Value=$canary \
        --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
        --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
        --period 86400 \
        --statistics Average \
        --query 'Datapoints[0].Average' \
        --output text)
    
    echo "  Success Rate: $success_rate%"
    
    # Get average latency
    avg_latency=$(aws cloudwatch get-metric-statistics \
        --namespace CloudWatchSynthetics \
        --metric-name Duration \
        --dimensions Name=CanaryName,Value=$canary \
        --start-time $(date -u -d '24 hours ago' +%Y-%m-%dT%H:%M:%S) \
        --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
        --period 86400 \
        --statistics Average \
        --query 'Datapoints[0].Average' \
        --output text)
    
    echo "  Average Latency: ${avg_latency}ms"
    echo ""
done
```

### Weekly Report

```bash
#!/bin/bash
# weekly-synthetics-report.sh

echo "CloudWatch Synthetics Weekly Report"
echo "===================================="
echo "Period: $(date -d '7 days ago' +%Y-%m-%d) to $(date +%Y-%m-%d)"
echo ""

# Generate report for each canary
canaries=$(aws synthetics describe-canaries --query 'Canaries[*].Name' --output text)

for canary in $canaries; do
    echo "Canary: $canary"
    echo "-------------------"
    
    # Get metrics
    aws cloudwatch get-metric-statistics \
        --namespace CloudWatchSynthetics \
        --metric-name SuccessPercent \
        --dimensions Name=CanaryName,Value=$canary \
        --start-time $(date -u -d '7 days ago' +%Y-%m-%dT%H:%M:%S) \
        --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
        --period 604800 \
        --statistics Average,Minimum \
        --output table
    
    echo ""
done
```

## Escalation Matrix

| Level | Role | Contact | Response Time |
|-------|------|---------|---------------|
| L1 | On-Call Engineer | PagerDuty | < 5 minutes |
| L2 | Senior DevOps | Slack + Phone | < 15 minutes |
| L3 | Engineering Manager | Phone | < 30 minutes |
| L4 | CTO | Phone | < 1 hour |

## Related Documentation

- [CloudWatch Synthetics Proactive Monitoring](../monitoring/cloudwatch-synthetics-proactive-monitoring.md)
- [Incident Response Procedures](../incident-response.md)
- [Disaster Recovery Runbook](../disaster-recovery-runbook.md)
- [Observability Stack Operations](../observability-operations.md)

---

**Document Owner**: DevOps Team  
**Review Cycle**: Quarterly  
**Next Review**: 2026-01-22
