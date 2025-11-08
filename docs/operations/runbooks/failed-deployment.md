# Runbook: Failed Deployment

## Symptoms

- Deployment stuck in progress
- New pods failing to start
- ImagePullBackOff errors
- CrashLoopBackOff errors
- Rollout status shows failure
- Health checks failing on new pods

## Impact

- **Severity**: P1 - High (P0 if production deployment)
- **Affected Users**: Potentially all users if production deployment fails
- **Business Impact**: Service disruption, delayed feature releases

## Detection

- **Alert**: `DeploymentFailed` alert fires
- **Monitoring Dashboard**: Deployment Dashboard > Rollout Status
- **Log Patterns**:
  - `Failed to pull image`
  - `Back-off restarting failed container`
  - `Readiness probe failed`
  - `Liveness probe failed`

## Diagnosis

### Step 1: Check Deployment Status

```bash
# Check rollout status
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Get deployment details
kubectl describe deployment ecommerce-backend -n ${NAMESPACE}

# Check replica sets
kubectl get rs -n ${NAMESPACE} -l app=ecommerce-backend

# Check pod status
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend
```

### Step 2: Identify Failed Pods

```bash
# Get pods with issues
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend | grep -v "Running\|Completed"

# Describe problematic pod
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}

# Check pod events
kubectl get events -n ${NAMESPACE} --field-selector involvedObject.name=${POD_NAME} --sort-by='.lastTimestamp'
```

### Step 3: Analyze Pod Logs

```bash
# Check current container logs
kubectl logs ${POD_NAME} -n ${NAMESPACE}

# Check previous container logs (if pod restarted)
kubectl logs ${POD_NAME} -n ${NAMESPACE} --previous

# Check init container logs
kubectl logs ${POD_NAME} -n ${NAMESPACE} -c init-container-name

# Stream logs in real-time
kubectl logs -f ${POD_NAME} -n ${NAMESPACE}
```

### Step 4: Check Image Issues

```bash
# Verify image exists in ECR
aws ecr describe-images \
  --repository-name ecommerce-backend \
  --image-ids imageTag=${VERSION}

# Check image pull secrets
kubectl get secret -n ${NAMESPACE} | grep ecr

# Verify secret is valid
kubectl get secret ecr-registry-secret -n ${NAMESPACE} -o yaml

# Test image pull manually
docker pull ${ECR_REGISTRY}/ecommerce-backend:${VERSION}
```

### Step 5: Check Configuration Issues

```bash
# Check ConfigMap
kubectl get configmap ecommerce-config -n ${NAMESPACE} -o yaml

# Check Secrets
kubectl get secret ecommerce-secrets -n ${NAMESPACE} -o jsonpath='{.data}' | jq

# Verify environment variables
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- env | grep -E "DB_|REDIS_|KAFKA_"

# Check mounted volumes
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- ls -la /config
```

### Step 6: Check Resource Constraints

```bash
# Check node resources
kubectl describe nodes | grep -A 5 "Allocated resources"

# Check if pods are pending due to resources
kubectl get pods -n ${NAMESPACE} -o wide | grep Pending

# Check resource quotas
kubectl get resourcequota -n ${NAMESPACE}

# Check limit ranges
kubectl get limitrange -n ${NAMESPACE}
```

### Step 7: Check Health Probes

```bash
# Test readiness probe manually
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -f http://localhost:8080/actuator/health/readiness

# Test liveness probe manually
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  curl -f http://localhost:8080/actuator/health/liveness

# Check probe configuration
kubectl get deployment ecommerce-backend -n ${NAMESPACE} -o yaml | grep -A 10 "livenessProbe\|readinessProbe"
```

## Resolution

### Immediate Actions

1. **Pause rollout** to prevent further issues:

```bash
kubectl rollout pause deployment/ecommerce-backend -n ${NAMESPACE}
```

1. **Assess impact**:

```bash
# Check how many pods are healthy
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend | grep "Running.*1/1" | wc -l

# Check if service is still available
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health
```

1. **Decide on action**:
   - If enough healthy pods: Fix and resume
   - If service degraded: Rollback immediately

### Root Cause Fixes

#### If caused by image pull failure

1. **Verify image exists**:

```bash
aws ecr describe-images \
  --repository-name ecommerce-backend \
  --image-ids imageTag=${VERSION}
```

1. **Update image pull secret** if expired:

```bash
# Get new ECR token
TOKEN=$(aws ecr get-login-password --region ${AWS_REGION})

# Update secret
kubectl create secret docker-registry ecr-registry-secret \
  --docker-server=${ECR_REGISTRY} \
  --docker-username=AWS \
  --docker-password=${TOKEN} \
  --namespace=${NAMESPACE} \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart deployment
kubectl rollout restart deployment/ecommerce-backend -n ${NAMESPACE}
```

1. **Fix image tag** if incorrect:

```bash
kubectl set image deployment/ecommerce-backend \
  ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${CORRECT_VERSION} \
  -n ${NAMESPACE}
```

#### If caused by application startup failure

1. **Check application logs** for startup errors:

```bash
kubectl logs ${POD_NAME} -n ${NAMESPACE} | grep -i "error\|exception\|failed"
```

1. **Common startup issues**:

**Database connection failure**:

```bash
# Verify database connectivity
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  pg_isready -h ${DB_HOST} -p 5432

# Check database credentials
kubectl get secret ecommerce-secrets -n ${NAMESPACE} -o jsonpath='{.data.DB_PASSWORD}' | base64 -d

# Test connection
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "SELECT 1"
```

**Missing environment variables**:

```bash
# Check required env vars
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- env | sort

# Add missing env vars
kubectl set env deployment/ecommerce-backend \
  MISSING_VAR=value \
  -n ${NAMESPACE}
```

**Configuration file issues**:

```bash
# Check ConfigMap
kubectl get configmap ecommerce-config -n ${NAMESPACE} -o yaml

# Update ConfigMap
kubectl create configmap ecommerce-config \
  --from-file=application.yml \
  --namespace=${NAMESPACE} \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart to pick up changes
kubectl rollout restart deployment/ecommerce-backend -n ${NAMESPACE}
```

#### If caused by health check failure

1. **Adjust health check timing**:

```yaml
# Increase initialDelaySeconds if app needs more time to start
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60  # Increase from 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30  # Increase from 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

1. **Fix health check endpoint**:

```java
@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check critical dependencies
            checkDatabase();
            checkRedis();
            
            return Health.up()
                .withDetail("database", "UP")
                .withDetail("redis", "UP")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### If caused by resource constraints

1. **Check node capacity**:

```bash
kubectl describe nodes | grep -A 5 "Allocated resources"
```

1. **Scale cluster** if needed:

```bash
# Add more nodes
eksctl scale nodegroup \
  --cluster=ecommerce-${ENV} \
  --name=ng-1 \
  --nodes=6 \
  --nodes-min=3 \
  --nodes-max=10
```

1. **Adjust resource requests**:

```yaml
resources:
  requests:
    memory: "1Gi"    # Reduce if too high
    cpu: "250m"      # Reduce if too high
  limits:
    memory: "2Gi"
    cpu: "500m"
```

#### If caused by database migration failure

1. **Check migration status**:

```bash
kubectl logs ${POD_NAME} -n ${NAMESPACE} | grep -i "flyway\|migration"
```

1. **Fix migration**:

```bash
# Connect to database
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME}

# Check migration history
SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;

# Mark failed migration as resolved if needed
UPDATE flyway_schema_history 
SET success = true 
WHERE version = '1.2.3' AND success = false;

# Or repair Flyway
kubectl exec -it ${POD_NAME} -n ${NAMESPACE} -- \
  ./gradlew flywayRepair
```

### Rollback Procedure

If fixes don't work quickly, rollback:

```bash
# Rollback to previous version
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE}

# Or rollback to specific revision
kubectl rollout history deployment/ecommerce-backend -n ${NAMESPACE}
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE} --to-revision=3

# Monitor rollback
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Verify rollback
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health
```

## Verification

- [ ] All pods are running (1/1 Ready)
- [ ] Deployment rollout completed successfully
- [ ] Health checks passing
- [ ] No error logs in recent logs
- [ ] API endpoints responding correctly
- [ ] Smoke tests passing
- [ ] Metrics showing normal behavior

### Verification Commands

```bash
# Check deployment status
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Verify all pods healthy
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend

# Check health endpoints
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/info

# Run smoke tests
./scripts/run-smoke-tests.sh ${ENVIRONMENT}

# Check metrics
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/metrics
```

## Prevention

### 1. Pre-Deployment Validation

```bash
# Validate deployment manifest
kubectl apply --dry-run=client -f deployment.yaml

# Validate with kubeval
kubeval deployment.yaml

# Run deployment tests in staging
./scripts/test-deployment.sh staging
```

### 2. Deployment Best Practices

```yaml
# Use deployment strategy
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0  # Ensure zero downtime

# Set appropriate resource limits
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "2Gi"
    cpu: "1000m"

# Configure health checks properly
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
  failureThreshold: 3
```

### 3. Automated Testing

```yaml
# CI/CD pipeline checks

- name: Build and test

  run: ./gradlew build test

- name: Build Docker image

  run: docker build -t ${IMAGE}:${TAG} .

- name: Scan image for vulnerabilities

  run: trivy image ${IMAGE}:${TAG}

- name: Deploy to staging

  run: kubectl apply -f k8s/staging/

- name: Run smoke tests

  run: ./scripts/smoke-test.sh staging

- name: Run integration tests

  run: ./scripts/integration-test.sh staging
```

### 4. Monitoring and Alerts

```yaml
# Set up deployment monitoring

- alert: DeploymentStuck

  expr: kube_deployment_status_replicas_updated{deployment="ecommerce-backend"} < kube_deployment_spec_replicas{deployment="ecommerce-backend"}
  for: 10m
  
- alert: PodCrashLooping

  expr: rate(kube_pod_container_status_restarts_total{pod=~"ecommerce-backend.*"}[15m]) > 0
  for: 5m
```

### 5. Deployment Checklist

- [ ] All tests passing in CI/CD
- [ ] Staging deployment successful
- [ ] Database migrations tested
- [ ] Configuration validated
- [ ] Resource limits appropriate
- [ ] Health checks configured
- [ ] Rollback plan ready
- [ ] Monitoring in place
- [ ] Team notified

## Escalation

- **L1 Support**: DevOps team (immediate response)
- **L2 Support**: Backend engineering team (code/config issues)
- **L3 Support**: Platform team (infrastructure issues)
- **On-Call Engineer**: Check PagerDuty

## Related

- [Rollback Procedures](../deployment/rollback.md)
- [Service Outage](service-outage.md)
- [Database Connection Issues](database-connection-issues.md)
- [Pod Restart Loop](pod-restart-loop.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Monthly
