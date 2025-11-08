# Deployment Process

## Overview

This document provides step-by-step deployment procedures for the Enterprise E-Commerce Platform across different environments.

## Deployment Environments

- **Local**: Developer workstation
- **Staging**: Pre-production testing environment
- **Production**: Live customer-facing environment

## Prerequisites

### Required Tools

- AWS CLI v2.x
- kubectl v1.28+
- Docker v24.x+
- Gradle 8.x
- Node.js 18.x+ (for frontend deployments)

### Required Access

- AWS IAM credentials with appropriate permissions
- EKS cluster access (kubeconfig)
- Container registry access (ECR)
- Secrets Manager access

## Deployment to Staging

### Step 1: Pre-Deployment Checks

```bash
# Verify AWS credentials
aws sts get-caller-identity

# Verify kubectl access
kubectl cluster-info

# Check current staging status
kubectl get pods -n staging
kubectl get services -n staging
```

### Step 2: Build Application

```bash
# Build backend application
cd app
./gradlew clean build

# Run tests
./gradlew test

# Build Docker image
docker build -t ecommerce-backend:${VERSION} .

# Tag for ECR
docker tag ecommerce-backend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### Step 3: Push to Container Registry

```bash
# Login to ECR
aws ecr get-login-password --region ${AWS_REGION} | \
  docker login --username AWS --password-stdin \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

# Push image
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### Step 4: Update Kubernetes Manifests

```bash
# Update deployment manifest with new image version
cd infrastructure/k8s/overlays/staging

# Edit kustomization.yaml to update image tag
kustomize edit set image \
  ecommerce-backend=${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}
```

### Step 5: Apply Database Migrations

```bash
# Run Flyway migrations
kubectl exec -it deployment/ecommerce-backend -n staging -- \
  ./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}

# Verify migration status
kubectl exec -it deployment/ecommerce-backend -n staging -- \
  ./gradlew flywayInfo
```

### Step 6: Deploy to Staging

```bash
# Apply Kubernetes manifests
kubectl apply -k infrastructure/k8s/overlays/staging

# Watch deployment progress
kubectl rollout status deployment/ecommerce-backend -n staging

# Verify pods are running
kubectl get pods -n staging -l app=ecommerce-backend
```

### Step 7: Verify Deployment

```bash
# Check pod logs
kubectl logs -f deployment/ecommerce-backend -n staging

# Check service endpoints
kubectl get svc -n staging

# Run smoke tests
./scripts/run-smoke-tests.sh staging

# Verify health endpoints
curl https://staging.ecommerce.example.com/actuator/health
curl https://staging.ecommerce.example.com/actuator/info
```

### Step 8: Post-Deployment Validation

- [ ] All pods are in Running state
- [ ] Health checks are passing
- [ ] Smoke tests pass
- [ ] No error logs in CloudWatch
- [ ] Metrics are being reported
- [ ] Database connections are healthy

## Deployment to Production

### Step 1: Pre-Deployment Checks

```bash
# Verify staging deployment is stable
./scripts/verify-staging-health.sh

# Check production status
kubectl get pods -n production
kubectl get services -n production

# Verify no ongoing incidents
# Check monitoring dashboards
# Review recent error rates
```

### Step 2: Create Deployment Plan

Document the following:

- Deployment window (date and time)
- Expected downtime (if any)
- Rollback plan
- Communication plan
- On-call engineers

### Step 3: Notify Stakeholders

```bash
# Send deployment notification
# - Engineering team
# - Product team
# - Customer support
# - Management

# Example notification:
# Subject: Production Deployment - [DATE] [TIME]
# - Version: ${VERSION}
# - Changes: [CHANGELOG_URL]
# - Expected duration: 30 minutes
# - Rollback plan: Available
```

### Step 4: Enable Maintenance Mode (if needed)

```bash
# For zero-downtime deployments, skip this step
# For deployments requiring downtime:

kubectl apply -f infrastructure/k8s/maintenance-mode.yaml
```

### Step 5: Build and Push Production Image

```bash
# Build production image
docker build -t ecommerce-backend:${VERSION} \
  --build-arg ENV=production .

# Tag for production ECR
docker tag ecommerce-backend:${VERSION} \
  ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}-prod

# Push to production registry
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ecommerce-backend:${VERSION}-prod
```

### Step 6: Apply Database Migrations

```bash
# Backup database before migration
aws rds create-db-snapshot \
  --db-instance-identifier ecommerce-prod \
  --db-snapshot-identifier ecommerce-prod-pre-${VERSION}

# Run migrations
kubectl exec -it deployment/ecommerce-backend -n production -- \
  ./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://${PROD_DB_HOST}:5432/${PROD_DB_NAME}

# Verify migration
kubectl exec -it deployment/ecommerce-backend -n production -- \
  ./gradlew flywayInfo
```

### Step 7: Deploy Using Canary Strategy

```bash
# Deploy canary (10% traffic)
kubectl apply -f infrastructure/k8s/overlays/production/canary.yaml

# Monitor canary metrics for 15 minutes
# - Error rate
# - Response time
# - CPU/Memory usage
# - Business metrics (orders, payments)

# If canary is healthy, proceed to full deployment
kubectl apply -k infrastructure/k8s/overlays/production

# Watch rollout
kubectl rollout status deployment/ecommerce-backend -n production
```

### Step 8: Verify Production Deployment

```bash
# Check all pods are running
kubectl get pods -n production -l app=ecommerce-backend

# Verify health endpoints
curl https://api.ecommerce.example.com/actuator/health

# Run production smoke tests
./scripts/run-smoke-tests.sh production

# Monitor key metrics
# - API response times
# - Error rates
# - Database connections
# - Cache hit rates
```

### Step 9: Monitor Post-Deployment

Monitor for 1 hour after deployment:

- [ ] Error rates are normal
- [ ] Response times are within SLA
- [ ] No increase in customer support tickets
- [ ] Business metrics are normal (orders, payments)
- [ ] No alerts triggered

### Step 10: Disable Maintenance Mode

```bash
# If maintenance mode was enabled
kubectl delete -f infrastructure/k8s/maintenance-mode.yaml
```

### Step 11: Post-Deployment Communication

```bash
# Send deployment completion notification
# Subject: Production Deployment Complete - [VERSION]
# - Deployment status: Success
# - All systems operational
# - Monitoring continues for 24 hours
```

## Frontend Deployment

### CMC Frontend (Next.js)

```bash
# Build frontend
cd cmc-frontend
npm run build

# Deploy to S3 + CloudFront
aws s3 sync out/ s3://cmc-frontend-${ENV}/

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id ${DISTRIBUTION_ID} \
  --paths "/*"
```

### Consumer Frontend (Angular)

```bash
# Build frontend
cd consumer-frontend
npm run build:${ENV}

# Deploy to S3 + CloudFront
aws s3 sync dist/ s3://consumer-frontend-${ENV}/

# Invalidate CloudFront cache
aws cloudfront create-invalidation \
  --distribution-id ${DISTRIBUTION_ID} \
  --paths "/*"
```

## Deployment Checklist

### Pre-Deployment

- [ ] All tests passing in CI/CD
- [ ] Code review completed
- [ ] Security scan passed
- [ ] Performance tests passed
- [ ] Staging deployment successful
- [ ] Deployment plan documented
- [ ] Rollback plan prepared
- [ ] Stakeholders notified

### During Deployment

- [ ] Database backup created
- [ ] Migrations applied successfully
- [ ] Application deployed
- [ ] Health checks passing
- [ ] Smoke tests passing
- [ ] Monitoring active

### Post-Deployment

- [ ] All services healthy
- [ ] Metrics within normal range
- [ ] No critical errors
- [ ] Customer-facing features working
- [ ] Deployment documented
- [ ] Stakeholders notified

## Deployment Automation

### CI/CD Pipeline

```yaml
# .github/workflows/deploy.yml
name: Deploy to Production

on:
  push:
    tags:

      - 'v*'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:

      - name: Checkout code

        uses: actions/checkout@v3
      
      - name: Build application

        run: ./gradlew build
      
      - name: Build Docker image

        run: docker build -t ecommerce-backend:${GITHUB_REF_NAME} .
      
      - name: Push to ECR

        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin
          docker push ${ECR_REGISTRY}/ecommerce-backend:${GITHUB_REF_NAME}
      
      - name: Deploy to EKS

        run: |
          kubectl set image deployment/ecommerce-backend \
            ecommerce-backend=${ECR_REGISTRY}/ecommerce-backend:${GITHUB_REF_NAME}
```

## Troubleshooting

### Deployment Fails

```bash
# Check pod status
kubectl describe pod ${POD_NAME} -n ${NAMESPACE}

# Check logs
kubectl logs ${POD_NAME} -n ${NAMESPACE}

# Check events
kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp'
```

### Rollback Required

See [Rollback Procedures](rollback.md) for detailed rollback steps.

## Related Documentation

- [Environment Configuration](environments.md)
- [Rollback Procedures](rollback.md)
- [Monitoring Guide](../monitoring/monitoring-strategy.md)
- [Troubleshooting Guide](../troubleshooting/common-issues.md)

---

**Last Updated**: 2025-10-25  
**Owner**: DevOps Team  
**Review Cycle**: Quarterly
