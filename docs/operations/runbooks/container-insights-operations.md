# Container Insights Operations Runbook

**Last Updated**: 2025-10-22  
**Severity Levels**: 🔴 Critical | 🟡 Warning | 🟢 Info

## Quick Reference

| Alert | Severity | Response Time | Escalation |
|-------|----------|---------------|------------|
| Pod High CPU (>80%) | 🟡 Warning | 15 minutes | After 1 hour |
| Pod High Memory (>85%) | 🔴 Critical | 5 minutes | After 30 minutes |
| Container Restart Rate (>5/10min) | 🔴 Critical | 5 minutes | Immediate |
| Network Errors (>10/5min) | 🟡 Warning | 15 minutes | After 1 hour |

## Incident Response Procedures

### 🔴 Critical: High Memory Utilization (>85%)

**Alert**: `{environment}-pod-high-memory`

**Symptoms**:
- Pod memory utilization exceeds 85%
- Potential OOMKilled events
- Application performance degradation

**Immediate Actions** (0-5 minutes):
1. **Identify Affected Pods**:
```bash
# Get pods with high memory usage
kubectl top pods --all-namespaces --sort-by=memory

# Check pod status
kubectl get pods -n <namespace> -o wide
```

2. **Check for OOMKilled Events**:
```bash
# Check pod events
kubectl describe pod <pod-name> -n <namespace> | grep -i oom

# Check container restart count
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.status.containerStatuses[*].restartCount}'
```

3. **Review Memory Metrics**:
```bash
# Query CloudWatch for memory trends
aws cloudwatch get-metric-statistics \
    --namespace ContainerInsights \
    --metric-name pod_memory_utilization \
    --dimensions Name=PodName,Value=<pod-name> \
    --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average
```

**Investigation** (5-15 minutes):
1. **Analyze Application Logs**:
```bash
# Get recent logs
kubectl logs <pod-name> -n <namespace> --tail=100

# Search for memory-related errors
kubectl logs <pod-name> -n <namespace> | grep -i "memory\|heap\|oom"
```

2. **Check Memory Limits**:
```bash
# Get pod resource limits
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.spec.containers[*].resources}'
```

3. **Review Container Restart Analysis**:
```bash
# Check Lambda function logs
aws logs tail /aws/lambda/{environment}-container-restart-analysis --follow
```

**Resolution Options**:

**Option A: Increase Memory Limits** (Temporary Fix)
```bash
# Edit deployment
kubectl edit deployment <deployment-name> -n <namespace>

# Update memory limits
spec:
  containers:
  - name: <container-name>
    resources:
      limits:
        memory: "2Gi"  # Increase from current value
      requests:
        memory: "1Gi"
```

**Option B: Scale Horizontally**
```bash
# Increase replica count
kubectl scale deployment <deployment-name> -n <namespace> --replicas=5
```

**Option C: Restart Pod** (If memory leak suspected)
```bash
# Delete pod to trigger restart
kubectl delete pod <pod-name> -n <namespace>
```

**Post-Incident**:
- Document root cause
- Update memory limits in Helm charts
- Schedule code review for memory optimization
- Update runbook if new patterns discovered

---

### 🔴 Critical: Container Restart Loop (>5 restarts/10min)

**Alert**: `{environment}-container-restart-rate`

**Symptoms**:
- Frequent container restarts
- CrashLoopBackOff status
- Service unavailability

**Immediate Actions** (0-5 minutes):
1. **Check Pod Status**:
```bash
# Get pod status and restart count
kubectl get pods -n <namespace> -o wide

# Describe pod for events
kubectl describe pod <pod-name> -n <namespace>
```

2. **Review Recent Logs**:
```bash
# Get logs from current container
kubectl logs <pod-name> -n <namespace>

# Get logs from previous container (if crashed)
kubectl logs <pod-name> -n <namespace> --previous
```

3. **Check Container Restart Analysis**:
```bash
# View automated analysis results
aws logs filter-log-events \
    --log-group-name /aws/lambda/{environment}-container-restart-analysis \
    --start-time $(date -u -d '30 minutes ago' +%s)000 \
    --filter-pattern "root_cause"
```

**Investigation** (5-15 minutes):
1. **Identify Root Cause**:
   - **OOMKilled**: Memory limit too low
   - **Error/Exception**: Application bug
   - **Configuration**: Missing env vars or secrets
   - **Probe Failure**: Health check misconfiguration

2. **Check Configuration**:
```bash
# Verify environment variables
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.spec.containers[*].env}'

# Check secrets
kubectl get secrets -n <namespace>

# Verify ConfigMaps
kubectl get configmaps -n <namespace>
```

3. **Review Health Probes**:
```bash
# Check liveness and readiness probes
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.spec.containers[*].livenessProbe}'
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.spec.containers[*].readinessProbe}'
```

**Resolution Options**:

**Option A: Fix Application Error**
```bash
# Rollback to previous version
kubectl rollout undo deployment/<deployment-name> -n <namespace>

# Check rollout status
kubectl rollout status deployment/<deployment-name> -n <namespace>
```

**Option B: Adjust Health Probes**
```bash
# Edit deployment
kubectl edit deployment <deployment-name> -n <namespace>

# Increase probe timing
livenessProbe:
  initialDelaySeconds: 60  # Increase from 30
  periodSeconds: 20        # Increase from 10
  timeoutSeconds: 5
  failureThreshold: 5      # Increase from 3
```

**Option C: Fix Configuration**
```bash
# Update ConfigMap
kubectl edit configmap <configmap-name> -n <namespace>

# Restart pods to pick up changes
kubectl rollout restart deployment/<deployment-name> -n <namespace>
```

**Post-Incident**:
- Fix application bug if identified
- Update health probe configuration
- Add integration tests for startup scenarios
- Document lessons learned

---

### 🟡 Warning: High CPU Utilization (>80%)

**Alert**: `{environment}-pod-high-cpu`

**Symptoms**:
- Pod CPU utilization exceeds 80%
- Slow response times
- Request queuing

**Immediate Actions** (0-15 minutes):
1. **Identify High CPU Pods**:
```bash
# Get pods sorted by CPU usage
kubectl top pods --all-namespaces --sort-by=cpu

# Check specific pod CPU
kubectl top pod <pod-name> -n <namespace>
```

2. **Review CPU Metrics**:
```bash
# Query CloudWatch for CPU trends
aws cloudwatch get-metric-statistics \
    --namespace ContainerInsights \
    --metric-name pod_cpu_utilization \
    --dimensions Name=PodName,Value=<pod-name> \
    --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average,Maximum
```

3. **Check Application Logs**:
```bash
# Look for CPU-intensive operations
kubectl logs <pod-name> -n <namespace> --tail=100 | grep -i "processing\|query\|batch"
```

**Investigation** (15-30 minutes):
1. **Analyze CPU Usage Patterns**:
   - Is it sustained or spike?
   - Correlated with specific requests?
   - Time-based pattern (e.g., batch jobs)?

2. **Check for Inefficient Code**:
```bash
# Review application metrics
kubectl port-forward <pod-name> 8080:8080 -n <namespace>
curl http://localhost:8080/actuator/metrics/process.cpu.usage
```

3. **Verify HPA Configuration**:
```bash
# Check HPA status
kubectl get hpa -n <namespace>

# Describe HPA for details
kubectl describe hpa <hpa-name> -n <namespace>
```

**Resolution Options**:

**Option A: Scale Horizontally**
```bash
# Manual scaling
kubectl scale deployment <deployment-name> -n <namespace> --replicas=8

# Or adjust HPA
kubectl edit hpa <hpa-name> -n <namespace>
```

**Option B: Increase CPU Limits**
```bash
# Edit deployment
kubectl edit deployment <deployment-name> -n <namespace>

# Update CPU limits
spec:
  containers:
  - name: <container-name>
    resources:
      limits:
        cpu: "2000m"  # Increase from current value
      requests:
        cpu: "1000m"
```

**Option C: Optimize Application**
- Profile application code
- Optimize database queries
- Implement caching
- Review batch processing logic

**Post-Incident**:
- Schedule performance optimization sprint
- Review and tune HPA thresholds
- Implement application profiling
- Update capacity planning

---

### 🟡 Warning: Network Errors (>10 errors/5min)

**Alert**: `{environment}-pod-network-errors`

**Symptoms**:
- High network error rate
- Connection timeouts
- Intermittent service failures

**Immediate Actions** (0-15 minutes):
1. **Check Network Metrics**:
```bash
# Query network error metrics
aws cloudwatch get-metric-statistics \
    --namespace ContainerInsights \
    --metric-name pod_network_rx_errors \
    --dimensions Name=ClusterName,Value=<cluster-name> \
    --start-time $(date -u -d '30 minutes ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Sum
```

2. **Identify Affected Pods**:
```bash
# Get pod network stats
kubectl exec <pod-name> -n <namespace> -- netstat -s | grep error

# Check pod network interfaces
kubectl exec <pod-name> -n <namespace> -- ip -s link
```

3. **Review VPC Flow Logs**:
```bash
# Query VPC Flow Logs
aws logs filter-log-events \
    --log-group-name /aws/vpc/flowlogs \
    --start-time $(date -u -d '30 minutes ago' +%s)000 \
    --filter-pattern "[version, account, eni, source, destination, srcport, destport, protocol, packets, bytes, windowstart, windowend, action=REJECT, flowlogstatus]"
```

**Investigation** (15-30 minutes):
1. **Check Security Groups**:
```bash
# Get security group rules
aws ec2 describe-security-groups \
    --group-ids <security-group-id> \
    --query 'SecurityGroups[*].IpPermissions'
```

2. **Verify Service Mesh Configuration**:
```bash
# Check Istio/service mesh status
kubectl get virtualservices -n <namespace>
kubectl get destinationrules -n <namespace>
```

3. **Test Network Connectivity**:
```bash
# Test from pod
kubectl exec <pod-name> -n <namespace> -- curl -v <service-url>

# Check DNS resolution
kubectl exec <pod-name> -n <namespace> -- nslookup <service-name>
```

**Resolution Options**:

**Option A: Fix Security Group Rules**
```bash
# Add missing ingress rule
aws ec2 authorize-security-group-ingress \
    --group-id <security-group-id> \
    --protocol tcp \
    --port 8080 \
    --source-group <source-security-group-id>
```

**Option B: Restart Network Components**
```bash
# Restart CoreDNS
kubectl rollout restart deployment/coredns -n kube-system

# Restart affected pods
kubectl delete pod <pod-name> -n <namespace>
```

**Option C: Update Service Mesh Configuration**
```bash
# Fix VirtualService
kubectl edit virtualservice <vs-name> -n <namespace>

# Verify configuration
kubectl describe virtualservice <vs-name> -n <namespace>
```

**Post-Incident**:
- Document network configuration changes
- Update security group documentation
- Review service mesh policies
- Implement network monitoring improvements

---

## Escalation Procedures

### Level 1: On-Call Engineer (0-30 minutes)
- Initial triage and investigation
- Implement immediate fixes
- Document actions taken

### Level 2: Senior DevOps (30-60 minutes)
- Complex troubleshooting
- Architecture-level decisions
- Coordinate with development team

### Level 3: Engineering Manager (>60 minutes)
- Major incident coordination
- Stakeholder communication
- Resource allocation decisions

## Communication Templates

### Incident Notification
```
🔴 INCIDENT: Container Insights Alert - {Alert Name}

Severity: {Critical/Warning}
Environment: {Production/Staging}
Affected Service: {Service Name}
Impact: {User Impact Description}

Current Status: {Investigating/Mitigating/Resolved}
ETA: {Estimated Resolution Time}

Actions Taken:
- {Action 1}
- {Action 2}

Next Steps:
- {Next Step 1}
- {Next Step 2}

Incident Commander: {Name}
```

### Resolution Notification
```
✅ RESOLVED: Container Insights Alert - {Alert Name}

Duration: {Total Incident Duration}
Root Cause: {Brief Description}

Resolution:
- {Resolution Action 1}
- {Resolution Action 2}

Preventive Measures:
- {Prevention 1}
- {Prevention 2}

Post-Mortem: {Link to Post-Mortem Document}
```

## Useful Commands Reference

### CloudWatch Queries
```bash
# Get all Container Insights metrics
aws cloudwatch list-metrics --namespace ContainerInsights

# Query specific metric
aws cloudwatch get-metric-statistics \
    --namespace ContainerInsights \
    --metric-name {metric-name} \
    --dimensions Name=ClusterName,Value={cluster-name} \
    --start-time {start-time} \
    --end-time {end-time} \
    --period 300 \
    --statistics Average,Maximum,Minimum
```

### Kubernetes Commands
```bash
# Get resource usage
kubectl top nodes
kubectl top pods --all-namespaces

# Get pod details
kubectl describe pod {pod-name} -n {namespace}
kubectl get pod {pod-name} -n {namespace} -o yaml

# Get logs
kubectl logs {pod-name} -n {namespace} --tail=100
kubectl logs {pod-name} -n {namespace} --previous

# Execute commands in pod
kubectl exec -it {pod-name} -n {namespace} -- /bin/bash
```

### Lambda Function Commands
```bash
# View restart analysis logs
aws logs tail /aws/lambda/{environment}-container-restart-analysis --follow

# Invoke function manually
aws lambda invoke \
    --function-name {environment}-container-restart-analysis \
    --payload '{}' \
    response.json
```

## Monitoring Checklist

### Daily
- [ ] Review Container Insights dashboard
- [ ] Check for any active alarms
- [ ] Review container restart trends
- [ ] Verify log collection is working

### Weekly
- [ ] Analyze restart patterns
- [ ] Review resource utilization trends
- [ ] Check alarm threshold effectiveness
- [ ] Update runbook if needed

### Monthly
- [ ] Review and optimize alert thresholds
- [ ] Analyze cost of Container Insights
- [ ] Update documentation
- [ ] Conduct incident response drill

---

**Document Owner**: DevOps Team  
**Last Incident Review**: 2025-10-22  
**Next Review**: 2025-11-22
