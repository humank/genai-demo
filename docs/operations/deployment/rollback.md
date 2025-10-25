# Rollback Procedures

## Overview

This document provides procedures for rolling back deployments when issues are detected in staging or production environments.

## When to Rollback

### Rollback Triggers

Initiate rollback immediately if any of the following occur:

- **Critical Errors**: Error rate > 5% for more than 5 minutes
- **Performance Degradation**: Response time > 3x baseline for more than 10 minutes
- **Data Corruption**: Any evidence of data integrity issues
- **Security Breach**: Any security vulnerability exploited
- **Service Outage**: Service unavailable for more than 2 minutes
- **Failed Health Checks**: Health checks failing for more than 3 minutes
- **Database Migration Failure**: Migration cannot be completed or causes errors

### Decision Matrix

| Severity | Impact | Action | Timeline |
|----------|--------|--------|----------|
| P0 - Critical | Service down or data at risk | Immediate rollback | < 5 minutes |
| P1 - High | Significant degradation | Rollback after 10 min monitoring | < 15 minutes |
| P2 - Medium | Minor issues affecting some users | Evaluate and decide | < 30 minutes |
| P3 - Low | Cosmetic or non-critical issues | Fix forward or schedule rollback | Next deployment |

## Rollback Types

### 1. Application Rollback

Rolling back the application code to the previous version.

### 2. Database Rollback

Rolling back database schema changes (if possible).

### 3. Configuration Rollback

Rolling back configuration changes.

### 4. Full System Rollback

Rolling back all components to previous state.

## Application Rollback Procedures

### Kubernetes Deployment Rollback

#### Quick Rollback (Recommended)

```bash
# Rollback to previous version
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE}

# Rollback to specific revision
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE} --to-revision=3

# Check rollout status
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}

# Verify pods are running
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend
```

#### Manual Rollback

```bash
# Get previous image version
PREVIOUS_VERSION=$(kubectl get deployment ecommerce-backend -n ${NAMESPACE} \
  -o jsonpath='{.metadata.annotations.previous-version}')

# Update deployment with previous version
kubectl set image deployment/ecommerce-backend \
  ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${PREVIOUS_VERSION} \
  -n ${NAMESPACE}

# Watch rollout
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}
```

### Verification Steps

After application rollback:

```bash
# 1. Check pod status
kubectl get pods -n ${NAMESPACE} -l app=ecommerce-backend

# 2. Check logs for errors
kubectl logs -f deployment/ecommerce-backend -n ${NAMESPACE} --tail=100

# 3. Verify health endpoints
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health

# 4. Run smoke tests
./scripts/run-smoke-tests.sh ${ENVIRONMENT}

# 5. Check metrics
# - Error rate should decrease
# - Response time should normalize
# - CPU/Memory usage should stabilize
```

## Database Rollback Procedures

### Flyway Migration Rollback

#### Check Migration Status

```bash
# View migration history
kubectl exec -it deployment/ecommerce-backend -n ${NAMESPACE} -- \
  ./gradlew flywayInfo

# Check current version
kubectl exec -it deployment/ecommerce-backend -n ${NAMESPACE} -- \
  ./gradlew flywayInfo | grep "Current version"
```

#### Rollback Options

**Option 1: Undo Migration (if undo scripts exist)**

```bash
# Run undo migration
kubectl exec -it deployment/ecommerce-backend -n ${NAMESPACE} -- \
  ./gradlew flywayUndo

# Verify rollback
kubectl exec -it deployment/ecommerce-backend -n ${NAMESPACE} -- \
  ./gradlew flywayInfo
```

**Option 2: Restore from Backup**

```bash
# 1. Stop application to prevent writes
kubectl scale deployment/ecommerce-backend --replicas=0 -n ${NAMESPACE}

# 2. Restore database from snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier ecommerce-${ENV}-restored \
  --db-snapshot-identifier ecommerce-${ENV}-pre-${VERSION}

# 3. Wait for restore to complete
aws rds wait db-instance-available \
  --db-instance-identifier ecommerce-${ENV}-restored

# 4. Update application to use restored database
# Update connection string in Secrets Manager

# 5. Restart application
kubectl scale deployment/ecommerce-backend --replicas=${REPLICA_COUNT} -n ${NAMESPACE}
```

**Option 3: Manual Rollback (Last Resort)**

```sql
-- Connect to database
psql -h ${DB_HOST} -U ${DB_USERNAME} -d ${DB_NAME}

-- Begin transaction
BEGIN;

-- Manually reverse changes
-- Example: Drop new column
ALTER TABLE orders DROP COLUMN new_column;

-- Verify changes
\d orders

-- Commit if correct
COMMIT;

-- Or rollback if issues
ROLLBACK;
```

### Database Rollback Verification

```bash
# 1. Verify schema version
kubectl exec -it deployment/ecommerce-backend -n ${NAMESPACE} -- \
  ./gradlew flywayInfo

# 2. Run database tests
./scripts/run-database-tests.sh ${ENVIRONMENT}

# 3. Verify data integrity
# Run data validation queries
# Check row counts
# Verify foreign key constraints
```

## Configuration Rollback

### Secrets Manager Rollback

```bash
# Get previous version
aws secretsmanager list-secret-version-ids \
  --secret-id ${ENV}/ecommerce/config

# Restore previous version
aws secretsmanager update-secret-version-stage \
  --secret-id ${ENV}/ecommerce/config \
  --version-stage AWSCURRENT \
  --move-to-version-id ${PREVIOUS_VERSION_ID}

# Restart pods to pick up new configuration
kubectl rollout restart deployment/ecommerce-backend -n ${NAMESPACE}
```

### ConfigMap Rollback

```bash
# Get previous ConfigMap
kubectl get configmap ecommerce-config -n ${NAMESPACE} -o yaml > previous-config.yaml

# Apply previous ConfigMap
kubectl apply -f previous-config.yaml

# Restart pods
kubectl rollout restart deployment/ecommerce-backend -n ${NAMESPACE}
```

## Frontend Rollback

### S3 + CloudFront Rollback

```bash
# 1. Identify previous version
aws s3 ls s3://ecommerce-${ENV}-frontend/versions/

# 2. Restore previous version
aws s3 sync s3://ecommerce-${ENV}-frontend/versions/${PREVIOUS_VERSION}/ \
  s3://ecommerce-${ENV}-frontend/ \
  --delete

# 3. Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id ${DISTRIBUTION_ID} \
  --paths "/*"

# 4. Verify rollback
curl https://${FRONTEND_URL}/ | grep "version"
```

## Complete System Rollback

### Step-by-Step Procedure

#### Step 1: Assess Situation

```bash
# Check all components
kubectl get pods -n ${NAMESPACE}
kubectl get services -n ${NAMESPACE}

# Check metrics
# - Error rates
# - Response times
# - Resource usage

# Check logs
kubectl logs -f deployment/ecommerce-backend -n ${NAMESPACE} --tail=100
```

#### Step 2: Notify Stakeholders

```bash
# Send rollback notification
# Subject: URGENT - Rollback in Progress - ${ENVIRONMENT}
# - Issue: [DESCRIPTION]
# - Impact: [USER IMPACT]
# - Action: Rolling back to version ${PREVIOUS_VERSION}
# - ETA: [ESTIMATED TIME]
```

#### Step 3: Enable Maintenance Mode (if needed)

```bash
# For production rollbacks with expected downtime
kubectl apply -f infrastructure/k8s/maintenance-mode.yaml
```

#### Step 4: Rollback Application

```bash
# Rollback deployment
kubectl rollout undo deployment/ecommerce-backend -n ${NAMESPACE}

# Wait for rollout to complete
kubectl rollout status deployment/ecommerce-backend -n ${NAMESPACE}
```

#### Step 5: Rollback Database (if needed)

```bash
# If database changes were made
# Follow Database Rollback Procedures above
```

#### Step 6: Rollback Configuration (if needed)

```bash
# If configuration changes were made
# Follow Configuration Rollback procedures above
```

#### Step 7: Verify System Health

```bash
# Check all pods are running
kubectl get pods -n ${NAMESPACE}

# Verify health endpoints
curl https://${ENVIRONMENT}.ecommerce.example.com/actuator/health

# Run smoke tests
./scripts/run-smoke-tests.sh ${ENVIRONMENT}

# Monitor metrics for 15 minutes
# - Error rate should be < 1%
# - Response time should be < 2s
# - No critical alerts
```

#### Step 8: Disable Maintenance Mode

```bash
# If maintenance mode was enabled
kubectl delete -f infrastructure/k8s/maintenance-mode.yaml
```

#### Step 9: Post-Rollback Communication

```bash
# Send rollback completion notification
# Subject: Rollback Complete - ${ENVIRONMENT}
# - Rollback status: Success
# - Current version: ${PREVIOUS_VERSION}
# - System status: Operational
# - Root cause analysis: In progress
```

## Rollback Verification Checklist

### Application Verification

- [ ] All pods are in Running state
- [ ] Health checks are passing
- [ ] No error logs in recent logs
- [ ] Smoke tests pass
- [ ] API endpoints responding correctly

### Database Verification

- [ ] Database schema version is correct
- [ ] Data integrity checks pass
- [ ] No orphaned records
- [ ] Foreign key constraints intact
- [ ] Database tests pass

### Performance Verification

- [ ] Error rate < 1%
- [ ] Response time < 2s (95th percentile)
- [ ] CPU usage < 70%
- [ ] Memory usage < 80%
- [ ] Database connections healthy

### Business Verification

- [ ] Critical user flows working
- [ ] Orders can be placed
- [ ] Payments processing
- [ ] Customer accounts accessible
- [ ] No increase in support tickets

## Post-Rollback Actions

### Immediate Actions

1. **Monitor System**: Continue monitoring for 1 hour
2. **Document Incident**: Create incident report
3. **Preserve Evidence**: Save logs, metrics, and error messages
4. **Notify Stakeholders**: Update all stakeholders on status

### Follow-Up Actions

1. **Root Cause Analysis**: Conduct RCA within 24 hours
2. **Fix Issues**: Address root cause in development
3. **Update Tests**: Add tests to prevent recurrence
4. **Update Procedures**: Improve deployment and rollback procedures
5. **Team Retrospective**: Conduct team retrospective

## Rollback Time Estimates

| Component | Rollback Time | Verification Time | Total Time |
|-----------|---------------|-------------------|------------|
| Application Only | 2-5 minutes | 5-10 minutes | 7-15 minutes |
| Application + Config | 5-10 minutes | 10-15 minutes | 15-25 minutes |
| Application + Database | 10-30 minutes | 15-30 minutes | 25-60 minutes |
| Full System | 15-45 minutes | 20-40 minutes | 35-85 minutes |

## Rollback Prevention

### Pre-Deployment Checks

- [ ] All tests passing in CI/CD
- [ ] Staging deployment successful
- [ ] Performance tests passed
- [ ] Security scan passed
- [ ] Database migration tested
- [ ] Rollback plan documented

### Deployment Best Practices

- [ ] Use canary deployments
- [ ] Deploy during low-traffic periods
- [ ] Have rollback plan ready
- [ ] Monitor closely during deployment
- [ ] Keep previous version available
- [ ] Test rollback procedure in staging

## Troubleshooting Rollback Issues

### Rollback Fails

```bash
# If rollback fails, try manual approach
# 1. Scale down current deployment
kubectl scale deployment/ecommerce-backend --replicas=0 -n ${NAMESPACE}

# 2. Create new deployment with previous version
kubectl apply -f previous-deployment.yaml

# 3. Verify new deployment
kubectl get pods -n ${NAMESPACE}
```

### Database Restore Fails

```bash
# If database restore fails
# 1. Check snapshot status
aws rds describe-db-snapshots \
  --db-snapshot-identifier ecommerce-${ENV}-pre-${VERSION}

# 2. Try alternative snapshot
aws rds describe-db-snapshots \
  --db-instance-identifier ecommerce-${ENV} \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime]' \
  --output table

# 3. Contact AWS support if needed
```

## Related Documentation

- [Deployment Process](deployment-process.md)
- [Environment Configuration](environments.md)
- [Monitoring Guide](../monitoring/monitoring-strategy.md)
- [Troubleshooting Guide](../troubleshooting/common-issues.md)
- [Incident Response](../runbooks/service-outage.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly
