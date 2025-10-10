# Cross-Region Configuration Management

## Overview

This document describes the enhanced cross-region configuration management system implemented in the Secrets Stack. The system provides automatic synchronization of secrets, ConfigMaps, and configuration parameters across multiple AWS regions for Active-Active deployment scenarios.

## Features

### 🔄 Cross-Region Secret Synchronization

- Automatic replication of AWS Secrets Manager secrets across regions
- Real-time synchronization triggered by EventBridge events
- Support for multiple replication regions
- Conflict resolution and error handling

### 🗺️ ConfigMap Synchronization

- Automatic synchronization of Kubernetes ConfigMaps
- Integration with EKS clusters across regions
- Selective synchronization of non-sensitive configuration data
- Support for custom namespaces and ConfigMap names

### 🔍 Configuration Drift Detection

- Automated detection of configuration inconsistencies across regions
- Hourly drift detection scans
- CloudWatch metrics and alerting integration
- Detailed drift reports with remediation guidance

### 🚀 GitOps Multi-Region Deployment Pipeline

- Integrated deployment pipeline for multi-region infrastructure
- Blue-green deployment strategy support
- Automatic rollback on failure
- Health check validation

## Architecture

```text
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Primary       │    │   Region 2      │    │   Region 3      │
│   Region        │    │                 │    │                 │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │   Secrets   │◄┼────┼►│   Secrets   │ │    │ │   Secrets   │ │
│ │  Manager    │ │    │ │  Manager    │ │    │ │  Manager    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │ ┌─────────────┐ │    │ ┌─────────────┐ │
│ │ ConfigMaps  │◄┼────┼►│ ConfigMaps  │ │    │ │ ConfigMaps  │ │
│ │    (EKS)    │ │    │ │    (EKS)    │ │    │ │    (EKS)    │ │
│ └─────────────┘ │    │ └─────────────┘ │    │ └─────────────┘ │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │                 │    │                 │
│ │EventBridge  │ │    │                 │    │                 │
│ │   Rules     │ │    │                 │    │                 │
│ └─────────────┘ │    │                 │    │                 │
│                 │    │                 │    │                 │
│ ┌─────────────┐ │    │                 │    │                 │
│ │   Drift     │ │    │                 │    │                 │
│ │ Detection   │ │    │                 │    │                 │
│ └─────────────┘ │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Components

### Lambda Functions

#### 1. Cross-Region Sync Lambda

- **Function Name**: `{project}-{environment}-cross-region-sync`
- **Purpose**: Synchronizes secrets across regions
- **Triggers**: EventBridge events, manual invocation
- **Timeout**: 10 minutes
- **Memory**: 512 MB

#### 2. ConfigMap Sync Lambda

- **Function Name**: `{project}-{environment}-configmap-sync`
- **Purpose**: Synchronizes ConfigMaps in Kubernetes clusters
- **Triggers**: EventBridge events, scheduled execution
- **Timeout**: 5 minutes
- **Memory**: 256 MB

#### 3. Drift Detection Lambda

- **Function Name**: `{project}-{environment}-drift-detection`
- **Purpose**: Detects configuration drift across regions
- **Triggers**: Scheduled (hourly), manual invocation
- **Timeout**: 15 minutes
- **Memory**: 512 MB

### EventBridge Rules

#### Secrets Manager Event Rule

- **Rule Name**: `{project}-{environment}-secrets-events`
- **Event Pattern**: Captures Secrets Manager API calls
- **Targets**: Cross-region sync and ConfigMap sync Lambdas

### Parameter Store Configuration

#### Global Configuration Parameters

- `/genai-demo/{environment}/global/secrets/cross-region-config`
- `/genai-demo/{environment}/global/secrets/gitops-config`
- `/genai-demo/{environment}/global/secrets/configmap-sync-config`
- `/genai-demo/{environment}/global/secrets/drift-detection-config`

## Deployment

### Prerequisites

- AWS CDK v2.x installed
- AWS CLI configured with appropriate permissions
- Multi-region deployment enabled in CDK context

### Basic Deployment

```bash
# Deploy with multi-region support
./infrastructure/deploy-unified.sh full -e production --enable-multi-region

# Deploy only secrets and configuration management
./infrastructure/deploy-unified.sh security -e production --enable-multi-region
```

### Advanced Deployment with Custom Regions

```bash
# Set custom replication regions
export REPLICATION_REGIONS="ap-northeast-1,ap-southeast-1,us-west-2"

# Deploy with custom configuration
./infrastructure/deploy-unified.sh full -e production \
  --enable-multi-region \
  -r ap-east-2 \
  -a ops@company.com
```

## Configuration

### Secrets Stack Configuration

```typescript
const secretsStack = new SecretsStack(this, 'SecretsStack', {
  environment: 'production',
  projectName: 'genai-demo',
  region: 'ap-east-2',
  vpc: networkStack.vpc,
  secretsManagerKey: kmsStack.secretsManagerKey,
  enableMultiRegion: true,
  replicationRegions: ['ap-northeast-1', 'ap-southeast-1'],
  alertingTopic: alertingStack.alertTopic
});
```

### Parameter Store Configuration

```json
{
  "primaryRegion": "ap-east-2",
  "replicationRegions": ["ap-northeast-1", "ap-southeast-1"],
  "syncEnabled": true,
  "driftDetectionEnabled": true,
  "configMapSyncEnabled": true,
  "lastSyncTimestamp": "2025-01-21T10:30:00Z"
}
```

### ConfigMap Sync Configuration

```json
{
  "enabled": true,
  "namespace": "default",
  "configMapName": "genai-demo-production-config",
  "syncInterval": "5m",
  "excludeSensitiveKeys": true,
  "includeKeys": ["log_level", "cache_ttl", "max_connections", "session_timeout"],
  "kubernetesServiceAccount": "genai-demo-production-secrets-sync"
}
```

### Drift Detection Configuration

```json
{
  "enabled": true,
  "checkInterval": "1h",
  "alertThreshold": 1,
  "autoRemediation": false,
  "includeSecrets": true,
  "includeParameters": true,
  "includeConfigMaps": true,
  "retentionDays": 30
}
```

## Monitoring and Alerting

### CloudWatch Metrics

#### Configuration Drift Metrics
- `genai-demo/ConfigurationDrift/DriftCount`: Number of configuration drifts detected
- `genai-demo/ConfigurationDrift/HasDrift`: Binary indicator of drift presence

#### Lambda Function Metrics
- `AWS/Lambda/Invocations`: Function invocation count
- `AWS/Lambda/Errors`: Function error count
- `AWS/Lambda/Duration`: Function execution duration

#### EventBridge Metrics
- `AWS/Events/MatchedEvents`: Number of events matched by rules

### CloudWatch Alarms

#### Critical Alarms
```bash
# Configuration drift detected
aws cloudwatch put-metric-alarm \
  --alarm-name "ConfigurationDriftDetected" \
  --alarm-description "Configuration drift detected across regions" \
  --metric-name "DriftCount" \
  --namespace "genai-demo/ConfigurationDrift" \
  --statistic "Sum" \
  --period 300 \
  --threshold 1 \
  --comparison-operator "GreaterThanOrEqualToThreshold"

# Cross-region sync failures
aws cloudwatch put-metric-alarm \
  --alarm-name "CrossRegionSyncFailures" \
  --alarm-description "Cross-region sync Lambda function failures" \
  --metric-name "Errors" \
  --namespace "AWS/Lambda" \
  --dimensions "Name=FunctionName,Value=genai-demo-production-cross-region-sync" \
  --statistic "Sum" \
  --period 300 \
  --threshold 1 \
  --comparison-operator "GreaterThanOrEqualToThreshold"
```

### Dashboard Widgets

The Observability Stack automatically creates dashboard widgets for:
- Configuration drift detection status
- Cross-region sync performance
- ConfigMap synchronization status
- Secrets Manager activity
- Multi-region health overview

## Testing

### Automated Testing
```bash
# Run comprehensive test suite
./infrastructure/test-cross-region-config.sh

# Test specific components
aws lambda invoke \
  --function-name genai-demo-production-cross-region-sync \
  --payload '{"action": "test_sync"}' \
  /tmp/sync-test-response.json
```

### Manual Testing

#### Test Secret Synchronization
```bash
# Update a secret in primary region
aws secretsmanager update-secret \
  --secret-id "production/genai-demo/application" \
  --secret-string '{"test_key": "test_value"}' \
  --region ap-east-2

# Wait for synchronization (30 seconds)
sleep 30

# Verify in replication regions
aws secretsmanager get-secret-value \
  --secret-id "production/genai-demo/application" \
  --region ap-northeast-1
```

#### Test Drift Detection
```bash
# Trigger drift detection manually
aws lambda invoke \
  --function-name genai-demo-production-drift-detection \
  --payload '{"action": "manual_check"}' \
  /tmp/drift-response.json

# Check results
cat /tmp/drift-response.json
```

## Troubleshooting

### Common Issues

#### 1. Cross-Region Sync Failures
**Symptoms**: Secrets not synchronized across regions
**Causes**: 
- IAM permission issues
- Network connectivity problems
- KMS key access issues

**Solutions**:
```bash
# Check Lambda function logs
aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/genai-demo"

# Verify IAM permissions
aws iam simulate-principal-policy \
  --policy-source-arn "arn:aws:iam::ACCOUNT:role/CrossRegionSyncLambdaRole" \
  --action-names "secretsmanager:GetSecretValue" \
  --resource-arns "*"

# Test network connectivity
aws lambda invoke \
  --function-name genai-demo-production-cross-region-sync \
  --payload '{"action": "connectivity_test"}' \
  /tmp/connectivity-test.json
```

#### 2. ConfigMap Sync Issues
**Symptoms**: ConfigMaps not updated in Kubernetes
**Causes**:
- EKS cluster access issues
- Service account permissions
- Kubernetes API connectivity

**Solutions**:
```bash
# Check EKS cluster status
aws eks describe-cluster --name genai-demo-production-cluster

# Verify service account
kubectl get serviceaccount genai-demo-production-secrets-sync

# Check ConfigMap
kubectl get configmap genai-demo-production-config -o yaml
```

#### 3. Drift Detection False Positives
**Symptoms**: Drift alerts for expected differences
**Causes**:
- Timing issues during synchronization
- Expected regional differences
- Configuration hash mismatches

**Solutions**:
```bash
# Check drift detection configuration
aws ssm get-parameter \
  --name "/genai-demo/production/global/secrets/drift-detection-config"

# Review drift detection logs
aws logs filter-log-events \
  --log-group-name "/aws/lambda/genai-demo-production-drift-detection" \
  --start-time $(date -d '1 hour ago' +%s)000
```

### Log Analysis

#### Lambda Function Logs
```bash
# Cross-region sync logs
aws logs tail /aws/lambda/genai-demo-production-cross-region-sync --follow

# ConfigMap sync logs
aws logs tail /aws/lambda/genai-demo-production-configmap-sync --follow

# Drift detection logs
aws logs tail /aws/lambda/genai-demo-production-drift-detection --follow
```

#### EventBridge Event Tracking
```bash
# Check EventBridge rule metrics
aws cloudwatch get-metric-statistics \
  --namespace "AWS/Events" \
  --metric-name "MatchedEvents" \
  --dimensions "Name=RuleName,Value=genai-demo-production-secrets-events" \
  --start-time $(date -d '1 hour ago' --iso-8601) \
  --end-time $(date --iso-8601) \
  --period 300 \
  --statistics Sum
```

## Security Considerations

### IAM Permissions
- Lambda functions use least-privilege IAM roles
- Cross-region access is explicitly granted
- KMS key permissions are region-specific

### Encryption
- All secrets are encrypted with customer-managed KMS keys
- Cross-region replication maintains encryption
- ConfigMaps exclude sensitive data

### Network Security
- Lambda functions run in private subnets
- VPC endpoints used for AWS service access
- Security groups restrict outbound traffic

## Best Practices

### 1. Secret Management
- Use descriptive secret names with environment prefixes
- Implement proper secret rotation schedules
- Monitor secret access patterns

### 2. Configuration Drift
- Set appropriate drift detection thresholds
- Implement automated remediation for critical drifts
- Regular review of drift detection reports

### 3. Multi-Region Deployment
- Test failover scenarios regularly
- Monitor cross-region latency
- Implement proper health checks

### 4. Monitoring and Alerting
- Set up comprehensive CloudWatch alarms
- Use SNS for critical alert notifications
- Implement escalation procedures

## Performance Optimization

### Lambda Function Optimization
- Use appropriate memory allocation
- Implement connection pooling for AWS clients
- Cache frequently accessed data

### EventBridge Optimization
- Use specific event patterns to reduce noise
- Implement proper error handling and retries
- Monitor rule performance metrics

### Cross-Region Optimization
- Choose replication regions based on latency requirements
- Implement intelligent routing for read operations
- Use regional caching where appropriate

## Maintenance

### Regular Tasks
- Review and update IAM policies
- Monitor Lambda function performance
- Update drift detection thresholds
- Test disaster recovery procedures

### Quarterly Reviews
- Analyze cross-region sync performance
- Review security configurations
- Update documentation
- Conduct failover testing

### Annual Tasks
- Security audit of cross-region access
- Performance optimization review
- Cost analysis and optimization
- Architecture review and updates

## Support and Documentation

### Additional Resources
- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [AWS EventBridge Documentation](https://docs.aws.amazon.com/eventbridge/)
- [Kubernetes ConfigMap Documentation](https://kubernetes.io/docs/concepts/configuration/configmap/)

### Getting Help
- Check CloudWatch logs for detailed error information
- Use the test script for automated diagnostics
- Review Parameter Store configuration for settings
- Contact the development team for complex issues

---

**Last Updated**: January 21, 2025  
**Version**: 1.0  
**Maintainer**: Development Team