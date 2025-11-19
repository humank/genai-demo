# Deployment Examples

> **Purpose**: Practical, runnable examples for common deployment tasks  
> **Last Updated**: 2024-11-19  
> **Owner**: DevOps Team

---

## Overview

This document provides practical, tested examples for deploying and managing the E-Commerce Platform. All examples are runnable and include expected outputs and troubleshooting tips.

---

## Example 1: Deploy to Development Environment

### Scenario

Deploy the latest application version to the development environment using GitHub Actions.

### Prerequisites

- GitHub repository access
- AWS credentials configured
- kubectl configured for dev cluster

### Steps

**1. Trigger Deployment via GitHub Actions**

```bash
# Trigger deployment workflow
gh workflow run deploy-dev.yml \
  --ref develop \
  --field environment=development \
  --field version=latest

# Check workflow status
gh run list --workflow=deploy-dev.yml --limit 1
```

**2. Monitor Deployment Progress**

```bash
# Watch deployment status
kubectl rollout status deployment/order-service -n development

# Check pod status
kubectl get pods -n development -l app=order-service

# View deployment logs
kubectl logs -f deployment/order-service -n development --tail=50
```

**Expected Output**:

```text
deployment "order-service" successfully rolled out

NAME                             READY   STATUS    RESTARTS   AGE
order-service-7d9f8c6b5d-abc12   1/1     Running   0          2m
order-service-7d9f8c6b5d-def34   1/1     Running   0          2m
order-service-7d9f8c6b5d-ghi56   1/1     Running   0          2m
```

### Troubleshooting

**Issue**: Pods stuck in `ImagePullBackOff`

```bash
# Check image pull errors
kubectl describe pod <pod-name> -n development

# Verify ECR access
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

**Issue**: Deployment timeout

```bash
# Check events
kubectl get events -n development --sort-by='.lastTimestamp'

# Check resource limits
kubectl describe deployment order-service -n development
```

---

## Example 2: Blue-Green Deployment to Production

### Scenario

Deploy a new version to production using blue-green deployment strategy with zero downtime.

### Prerequisites

- Production cluster access
- Approved change request
- Tested in staging environment

### Steps

**1. Deploy Green Environment**

```bash
# Set environment variables
export VERSION="v2.5.0"
export NAMESPACE="production"
export SERVICE="order-service"

# Deploy green version
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${SERVICE}-green
  namespace: ${NAMESPACE}
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ${SERVICE}
      version: green
  template:
    metadata:
      labels:
        app: ${SERVICE}
        version: green
    spec:
      containers:
      - name: ${SERVICE}
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/${SERVICE}:${VERSION}
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
EOF

# Wait for green deployment to be ready
kubectl rollout status deployment/${SERVICE}-green -n ${NAMESPACE}
```

**2. Verify Green Environment**

```bash
# Check pod health
kubectl get pods -n ${NAMESPACE} -l app=${SERVICE},version=green

# Test green endpoints (internal)
kubectl run test-pod --rm -i --tty --image=curlimages/curl -- \
  curl http://${SERVICE}-green.${NAMESPACE}.svc.cluster.local:8080/actuator/health

# Run smoke tests
./scripts/smoke-test.sh --target=green --namespace=${NAMESPACE}
```

**3. Switch Traffic to Green**

```bash
# Update service selector to point to green
kubectl patch service ${SERVICE} -n ${NAMESPACE} -p '{"spec":{"selector":{"version":"green"}}}'

# Verify traffic switch
kubectl describe service ${SERVICE} -n ${NAMESPACE}

# Monitor metrics
kubectl top pods -n ${NAMESPACE} -l app=${SERVICE},version=green
```

**4. Monitor and Validate**

```bash
# Watch error rates
kubectl logs -f deployment/${SERVICE}-green -n ${NAMESPACE} | grep ERROR

# Check application metrics
curl https://api.example.com/actuator/metrics/http.server.requests

# Monitor for 10 minutes
sleep 600
```

**5. Cleanup Blue Environment**

```bash
# Scale down blue deployment
kubectl scale deployment/${SERVICE}-blue -n ${NAMESPACE} --replicas=0

# Wait 24 hours, then delete if no issues
# kubectl delete deployment/${SERVICE}-blue -n ${NAMESPACE}
```

### Rollback Procedure

If issues are detected:

```bash
# Immediately switch back to blue
kubectl patch service ${SERVICE} -n ${NAMESPACE} -p '{"spec":{"selector":{"version":"blue"}}}'

# Scale up blue if needed
kubectl scale deployment/${SERVICE}-blue -n ${NAMESPACE} --replicas=3

# Verify rollback
kubectl get pods -n ${NAMESPACE} -l app=${SERVICE},version=blue
```

---

## Example 3: Database Migration Deployment

### Scenario

Deploy application with database schema changes using Flyway migrations.

### Prerequisites

- Database backup completed
- Migration scripts tested in staging
- Maintenance window scheduled

### Steps

**1. Create Database Backup**

```bash
# Create manual snapshot
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-production \
  --db-snapshot-identifier pre-migration-$(date +%Y%m%d-%H%M%S)

# Wait for snapshot completion
aws rds wait db-snapshot-completed \
  --db-snapshot-identifier pre-migration-$(date +%Y%m%d-%H%M%S)

# Verify snapshot
aws rds describe-db-snapshots \
  --db-snapshot-identifier pre-migration-$(date +%Y%m%d-%H%M%S) \
  --query 'DBSnapshots[0].[Status,PercentProgress]'
```

**2. Enable Maintenance Mode**

```bash
# Scale down application to 1 replica
kubectl scale deployment order-service -n production --replicas=1

# Enable maintenance page
kubectl apply -f k8s/maintenance-mode.yaml

# Verify maintenance mode
curl https://api.example.com/health
```

**3. Run Database Migrations**

```bash
# Connect to database
export DB_HOST=$(aws rds describe-db-instances \
  --db-instance-identifier ecommerce-production \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Run Flyway migrations
./gradlew flywayMigrate \
  -Dflyway.url=jdbc:postgresql://${DB_HOST}:5432/ecommerce \
  -Dflyway.user=${DB_USER} \
  -Dflyway.password=${DB_PASSWORD}

# Verify migration
./gradlew flywayInfo
```

**Expected Output**:

```text
+-----------+---------+---------------------+------+---------------------+---------+
| Category  | Version | Description         | Type | Installed On        | State   |
+-----------+---------+---------------------+------+---------------------+---------+
| Versioned | 1.0     | Initial schema      | SQL  | 2024-01-01 10:00:00 | Success |
| Versioned | 1.1     | Add order table     | SQL  | 2024-02-01 10:00:00 | Success |
| Versioned | 1.2     | Add payment table   | SQL  | 2024-11-19 14:30:00 | Success |
+-----------+---------+---------------------+------+---------------------+---------+
```

**4. Deploy New Application Version**

```bash
# Deploy application with migration support
kubectl set image deployment/order-service \
  order-service=<account-id>.dkr.ecr.us-east-1.amazonaws.com/order-service:v2.5.0 \
  -n production

# Wait for rollout
kubectl rollout status deployment/order-service -n production
```

**5. Verify and Disable Maintenance Mode**

```bash
# Run smoke tests
./scripts/smoke-test.sh --namespace=production

# Check application logs
kubectl logs -f deployment/order-service -n production --tail=100

# Disable maintenance mode
kubectl delete -f k8s/maintenance-mode.yaml

# Scale back to normal
kubectl scale deployment order-service -n production --replicas=3
```

### Rollback Procedure

If migration fails:

```bash
# Restore from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-production-restored \
  --db-snapshot-identifier pre-migration-$(date +%Y%m%d-%H%M%S)

# Update DNS to point to restored instance
# Deploy previous application version
kubectl set image deployment/order-service \
  order-service=<account-id>.dkr.ecr.us-east-1.amazonaws.com/order-service:v2.4.0 \
  -n production
```

---

## Example 4: Canary Deployment

### Scenario

Gradually roll out a new version to a small percentage of users before full deployment.

### Prerequisites

- Istio or similar service mesh configured
- Monitoring and alerting set up
- Rollback plan prepared

### Steps

**1. Deploy Canary Version**

```bash
# Deploy canary with 10% traffic
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: order-service-canary
  namespace: production
spec:
  selector:
    app: order-service
    version: canary
  ports:
  - port: 8080
    targetPort: 8080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service-canary
  namespace: production
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order-service
      version: canary
  template:
    metadata:
      labels:
        app: order-service
        version: canary
    spec:
      containers:
      - name: order-service
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/order-service:v2.5.0
        # ... (same configuration as stable)
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: order-service
  namespace: production
spec:
  hosts:
  - order-service
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: order-service-canary
        port:
          number: 8080
  - route:
    - destination:
        host: order-service
        port:
          number: 8080
      weight: 90
    - destination:
        host: order-service-canary
        port:
          number: 8080
      weight: 10
EOF
```

**2. Monitor Canary Metrics**

```bash
# Watch canary error rate
kubectl logs -f deployment/order-service-canary -n production | grep ERROR | wc -l

# Compare response times
kubectl exec -it <prometheus-pod> -n monitoring -- \
  promtool query instant \
  'histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{version="canary"}[5m]))'

# Check canary vs stable
./scripts/compare-metrics.sh --canary --stable --duration=10m
```

**3. Gradually Increase Traffic**

```bash
# Increase to 25%
kubectl patch virtualservice order-service -n production --type=json -p='[
  {"op": "replace", "path": "/spec/http/1/route/0/weight", "value": 75},
  {"op": "replace", "path": "/spec/http/1/route/1/weight", "value": 25}
]'

# Wait and monitor (30 minutes)
sleep 1800

# Increase to 50%
kubectl patch virtualservice order-service -n production --type=json -p='[
  {"op": "replace", "path": "/spec/http/1/route/0/weight", "value": 50},
  {"op": "replace", "path": "/spec/http/1/route/1/weight", "value": 50}
]'

# Wait and monitor (30 minutes)
sleep 1800

# Increase to 100%
kubectl patch virtualservice order-service -n production --type=json -p='[
  {"op": "replace", "path": "/spec/http/1/route/0/weight", "value": 0},
  {"op": "replace", "path": "/spec/http/1/route/1/weight", "value": 100}
]'
```

**4. Promote Canary to Stable**

```bash
# Update stable deployment
kubectl set image deployment/order-service \
  order-service=<account-id>.dkr.ecr.us-east-1.amazonaws.com/order-service:v2.5.0 \
  -n production

# Remove canary deployment
kubectl delete deployment order-service-canary -n production
kubectl delete service order-service-canary -n production

# Reset virtual service
kubectl delete virtualservice order-service -n production
```

---

## Example 5: Infrastructure Deployment with AWS CDK

### Scenario

Deploy or update AWS infrastructure using CDK.

### Prerequisites

- AWS CDK installed (`npm install -g aws-cdk`)
- AWS credentials configured
- CDK project initialized

### Steps

**1. Synthesize CloudFormation Template**

```bash
# Navigate to CDK project
cd infrastructure/cdk

# Install dependencies
npm install

# Synthesize template
cdk synth

# Review changes
cdk diff
```

**Expected Output**:

```text
Stack EcommerceInfraStack
Resources
[+] AWS::EKS::Cluster EKSCluster
[~] AWS::RDS::DBInstance Database
 └─ [~] EngineVersion
     ├─ [-] 14.7
     └─ [+] 15.3
```

**2. Deploy Infrastructure**

```bash
# Deploy with approval
cdk deploy --require-approval=broadening

# Or deploy specific stack
cdk deploy EcommerceInfraStack

# Deploy all stacks
cdk deploy --all
```

**3. Verify Deployment**

```bash
# Check stack status
aws cloudformation describe-stacks \
  --stack-name EcommerceInfraStack \
  --query 'Stacks[0].StackStatus'

# Get stack outputs
aws cloudformation describe-stacks \
  --stack-name EcommerceInfraStack \
  --query 'Stacks[0].Outputs'

# Verify resources
aws eks describe-cluster --name ecommerce-cluster
aws rds describe-db-instances --db-instance-identifier ecommerce-production
```

### Rollback Procedure

```bash
# Rollback to previous version
cdk deploy --rollback

# Or manually via CloudFormation
aws cloudformation cancel-update-stack --stack-name EcommerceInfraStack
```

---

## Related Documentation

- [Deployment Viewpoint](../viewpoints/deployment/README.md)
- [Deployment Process](../viewpoints/deployment/deployment-process.md)
- [Operations Guide](../operations/deployment/README.md)
- [CI/CD Pipeline](../viewpoints/deployment/deployment-process.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: DevOps Team
