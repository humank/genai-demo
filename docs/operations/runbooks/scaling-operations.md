# Runbook: Scaling Operations

## Overview

This runbook covers manual and automatic scaling procedures for the Enterprise E-Commerce Platform.

## When to Scale

### Scale Up Triggers

- CPU utilization > 70% for 10+ minutes
- Memory utilization > 80% for 5+ minutes
- Request queue length increasing
- Response time degradation
- Anticipated traffic spike (marketing campaign, sales event)

### Scale Down Triggers

- CPU utilization < 30% for 30+ minutes
- Memory utilization < 50% for 30+ minutes
- Low request rate during off-peak hours
- Cost optimization needs

## Horizontal Pod Autoscaling (HPA)

### Check Current HPA Status

```bash
# Get HPA status
kubectl get hpa -n production

# Describe HPA for details
kubectl describe hpa ecommerce-backend-hpa -n production

# Check current metrics
kubectl get hpa ecommerce-backend-hpa -n production -o yaml

# View HPA events
kubectl get events -n production --field-selector involvedObject.name=ecommerce-backend-hpa
```

### Configure HPA

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ecommerce-backend-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ecommerce-backend
  minReplicas: 4
  maxReplicas: 20
  metrics:

    - type: Resource

      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60

    - type: Resource

      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 70

    - type: Pods

      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:

        - type: Percent

          value: 50
          periodSeconds: 60

        - type: Pods

          value: 2
          periodSeconds: 60
      selectPolicy: Max
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:

        - type: Percent

          value: 10
          periodSeconds: 60

        - type: Pods

          value: 1
          periodSeconds: 60
      selectPolicy: Min
```

### Apply HPA Configuration

```bash
# Apply HPA
kubectl apply -f hpa.yaml

# Verify HPA is working
kubectl get hpa -n production -w

# Check HPA metrics
kubectl top pods -n production -l app=ecommerce-backend
```

## Manual Scaling

### Scale Application Pods

#### Scale Up

```bash
# Check current replica count
kubectl get deployment ecommerce-backend -n production

# Scale up to 8 replicas
kubectl scale deployment/ecommerce-backend --replicas=8 -n production

# Watch scaling progress
kubectl get pods -n production -l app=ecommerce-backend -w

# Verify all pods are running
kubectl get pods -n production -l app=ecommerce-backend | grep "1/1.*Running" | wc -l
```

#### Scale Down

```bash
# Scale down to 4 replicas
kubectl scale deployment/ecommerce-backend --replicas=4 -n production

# Watch scaling progress
kubectl get pods -n production -l app=ecommerce-backend -w

# Verify pods terminated gracefully
kubectl get events -n production | grep "ecommerce-backend" | grep "Killing"
```

### Scale Database

#### Read Replicas

```bash
# Add read replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-prod-replica-3 \
  --source-db-instance-identifier ecommerce-production \
  --db-instance-class db.r5.xlarge \
  --availability-zone ap-northeast-1c

# Wait for replica to be available
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-prod-replica-3

# Update application to use new replica
kubectl set env deployment/ecommerce-backend \
  DB_READ_REPLICAS="replica1.xxx.rds.amazonaws.com,replica2.xxx.rds.amazonaws.com,replica3.xxx.rds.amazonaws.com" \
  -n production
```

#### Vertical Scaling (Instance Size)

```bash
# Modify RDS instance class
aws rds modify-db-instance \
  --db-instance-identifier ecommerce-production \
  --db-instance-class db.r5.2xlarge \
  --apply-immediately

# Monitor modification progress
aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].DBInstanceStatus'

# Wait for modification to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-production
```

### Scale Cache (Redis)

#### Add Cache Nodes

```bash
# Increase number of cache nodes
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --num-cache-clusters 5 \
  --apply-immediately

# Monitor scaling progress
aws elasticache describe-replication-groups \
  --replication-group-id ecommerce-prod-redis \
  --query 'ReplicationGroups[0].Status'
```

#### Vertical Scaling (Node Type)

```bash
# Modify cache node type
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --cache-node-type cache.r5.xlarge \
  --apply-immediately
```

### Scale Kafka (MSK)

#### Add Brokers

```bash
# Get current broker count
aws kafka describe-cluster \
  --cluster-arn ${CLUSTER_ARN} \
  --query 'ClusterInfo.NumberOfBrokerNodes'

# Update broker count
aws kafka update-broker-count \
  --cluster-arn ${CLUSTER_ARN} \
  --current-version ${CURRENT_VERSION} \
  --target-number-of-broker-nodes 6

# Monitor scaling operation
aws kafka describe-cluster-operation \
  --cluster-operation-arn ${OPERATION_ARN}
```

#### Vertical Scaling (Broker Type)

```bash
# Update broker type
aws kafka update-broker-type \
  --cluster-arn ${CLUSTER_ARN} \
  --current-version ${CURRENT_VERSION} \
  --target-instance-type kafka.m5.2xlarge
```

## Cluster Scaling (EKS Nodes)

### Check Current Node Status

```bash
# Get node count and status
kubectl get nodes

# Check node resource usage
kubectl top nodes

# Check pod distribution
kubectl get pods -n production -o wide | awk '{print $7}' | sort | uniq -c
```

### Scale Node Group

#### Using eksctl

```bash
# Scale node group
eksctl scale nodegroup \
  --cluster=ecommerce-production \
  --name=ng-1 \
  --nodes=8 \
  --nodes-min=4 \
  --nodes-max=12

# Verify scaling
kubectl get nodes -w
```

#### Using AWS CLI

```bash
# Update Auto Scaling Group
aws autoscaling update-auto-scaling-group \
  --auto-scaling-group-name eks-ng-1-xxxxx \
  --min-size 4 \
  --max-size 12 \
  --desired-capacity 8

# Wait for nodes to be ready
kubectl wait --for=condition=Ready nodes --all --timeout=600s
```

### Add New Node Group

```bash
# Create new node group with larger instances
eksctl create nodegroup \
  --cluster=ecommerce-production \
  --name=ng-large \
  --node-type=t3.xlarge \
  --nodes=3 \
  --nodes-min=2 \
  --nodes-max=6 \
  --node-labels="workload=compute-intensive"

# Migrate pods to new node group
kubectl cordon -l node-group=ng-1
kubectl drain -l node-group=ng-1 --ignore-daemonsets --delete-emptydir-data

# Verify pods running on new nodes
kubectl get pods -n production -o wide
```

## Scaling for Specific Events

### Pre-Event Scaling (Marketing Campaign)

```bash
# 1 hour before event
# Scale application pods
kubectl scale deployment/ecommerce-backend --replicas=12 -n production

# Add database read replicas
aws rds create-db-instance-read-replica \
  --db-instance-identifier ecommerce-prod-replica-temp \
  --source-db-instance-identifier ecommerce-production

# Increase cache capacity
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --num-cache-clusters 6 \
  --apply-immediately

# Scale EKS nodes
eksctl scale nodegroup \
  --cluster=ecommerce-production \
  --name=ng-1 \
  --nodes=10

# Warm up cache
./scripts/warm-cache.sh

# Pre-load frequently accessed data
./scripts/preload-data.sh
```

### Post-Event Scale Down

```bash
# Wait 2 hours after event ends
# Scale down application pods
kubectl scale deployment/ecommerce-backend --replicas=4 -n production

# Remove temporary read replica
aws rds delete-db-instance \
  --db-instance-identifier ecommerce-prod-replica-temp \
  --skip-final-snapshot

# Reduce cache capacity
aws elasticache modify-replication-group \
  --replication-group-id ecommerce-prod-redis \
  --num-cache-clusters 3 \
  --apply-immediately

# Scale down EKS nodes
eksctl scale nodegroup \
  --cluster=ecommerce-production \
  --name=ng-1 \
  --nodes=6
```

## Monitoring During Scaling

### Key Metrics to Watch

```bash
# Monitor pod CPU/Memory
watch -n 5 'kubectl top pods -n production -l app=ecommerce-backend'

# Monitor node resources
watch -n 5 'kubectl top nodes'

# Monitor API response times
watch -n 5 'curl -s http://localhost:8080/actuator/metrics/http.server.requests | jq ".measurements[0].value"'

# Monitor request rate
watch -n 5 'kubectl logs deployment/ecommerce-backend -n production --tail=100 | grep "HTTP" | wc -l'

# Monitor error rate
watch -n 5 'kubectl logs deployment/ecommerce-backend -n production --tail=100 | grep "ERROR" | wc -l'
```

### Grafana Dashboards

- **Scaling Dashboard**: Monitor scaling metrics in real-time
- **Resource Utilization**: Track CPU, memory, network
- **Application Performance**: Response times, throughput, errors
- **Cost Dashboard**: Track scaling costs

## Verification

### After Scaling Up

- [ ] All new pods are running (1/1 Ready)
- [ ] New pods passing health checks
- [ ] Load distributed across all pods
- [ ] Response times improved or stable
- [ ] Error rate normal (< 1%)
- [ ] No resource constraints
- [ ] Database connections healthy
- [ ] Cache hit rate maintained

### After Scaling Down

- [ ] Remaining pods are running
- [ ] No service disruption
- [ ] Response times stable
- [ ] Error rate normal
- [ ] Resource utilization appropriate
- [ ] Cost reduced as expected

### Verification Commands

```bash
# Check pod status
kubectl get pods -n production -l app=ecommerce-backend

# Check HPA status
kubectl get hpa -n production

# Check resource usage
kubectl top pods -n production -l app=ecommerce-backend

# Test API endpoints
./scripts/smoke-test.sh production

# Check metrics
curl http://localhost:8080/actuator/metrics/http.server.requests
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## Troubleshooting

### Pods Not Scaling

```bash
# Check HPA status
kubectl describe hpa ecommerce-backend-hpa -n production

# Check metrics server
kubectl get apiservice v1beta1.metrics.k8s.io -o yaml

# Check resource limits
kubectl describe deployment ecommerce-backend -n production | grep -A 5 "Limits\|Requests"

# Check events
kubectl get events -n production --sort-by='.lastTimestamp' | grep -i scale
```

### Nodes Not Scaling

```bash
# Check Auto Scaling Group
aws autoscaling describe-auto-scaling-groups \
  --auto-scaling-group-names eks-ng-1-xxxxx

# Check scaling activities
aws autoscaling describe-scaling-activities \
  --auto-scaling-group-name eks-ng-1-xxxxx \
  --max-records 10

# Check node capacity
kubectl describe nodes | grep -A 5 "Allocatable"
```

### Performance Not Improving

```bash
# Check if pods are actually receiving traffic
kubectl logs deployment/ecommerce-backend -n production --tail=100 | grep "HTTP"

# Check service endpoints
kubectl get endpoints ecommerce-backend -n production

# Check load balancer
kubectl describe service ecommerce-backend -n production

# Check for bottlenecks
kubectl top pods -n production
kubectl top nodes
```

## Cost Optimization

### Right-Sizing

```bash
# Analyze resource usage over time
kubectl top pods -n production -l app=ecommerce-backend --containers

# Adjust resource requests/limits based on actual usage
kubectl set resources deployment/ecommerce-backend \
  --requests=cpu=400m,memory=1.5Gi \
  --limits=cpu=800m,memory=3Gi \
  -n production
```

### Spot Instances

```bash
# Create spot instance node group
eksctl create nodegroup \
  --cluster=ecommerce-production \
  --name=ng-spot \
  --node-type=t3.large \
  --nodes=3 \
  --nodes-min=2 \
  --nodes-max=8 \
  --spot \
  --instance-types=t3.large,t3a.large,t2.large

# Label pods for spot instances
kubectl label deployment ecommerce-backend-worker \
  workload=spot-compatible \
  -n production
```

### Scheduled Scaling

```bash
# Scale down during off-peak hours (2 AM - 6 AM)
# Create CronJob for scheduled scaling
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: scale-down-offpeak
  namespace: production
spec:
  schedule: "0 2 * * *"  # 2 AM daily
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: scaler
          containers:

          - name: kubectl

            image: bitnami/kubectl:latest
            command:

            - /bin/sh
            - -c
            - kubectl scale deployment/ecommerce-backend --replicas=2 -n production

          restartPolicy: OnFailure
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: scale-up-peak
  namespace: production
spec:
  schedule: "0 6 * * *"  # 6 AM daily
  jobTemplate:
    spec:
      template:
        spec:
          serviceAccountName: scaler
          containers:

          - name: kubectl

            image: bitnami/kubectl:latest
            command:

            - /bin/sh
            - -c
            - kubectl scale deployment/ecommerce-backend --replicas=6 -n production

          restartPolicy: OnFailure
EOF
```

## Best Practices

### 1. Gradual Scaling

- Scale in small increments (1-2 pods at a time)
- Monitor impact before scaling further
- Allow time for pods to warm up

### 2. Predictive Scaling

- Analyze historical traffic patterns
- Pre-scale before known traffic spikes
- Use scheduled scaling for predictable patterns

### 3. Testing

- Test scaling procedures in staging
- Conduct load tests to verify scaling behavior
- Practice scaling during low-traffic periods

### 4. Documentation

- Document scaling decisions
- Track scaling events and outcomes
- Update runbooks based on learnings

## Related

- [High CPU Usage](high-cpu-usage.md)
- [High Memory Usage](high-memory-usage.md)
- [Slow API Responses](slow-api-responses.md)
- [Service Outage](service-outage.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly
