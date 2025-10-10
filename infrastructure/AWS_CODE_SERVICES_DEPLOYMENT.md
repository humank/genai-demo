# AWS Code Services Multi-Region Deployment Guide

## Overview

The `deploy-unified.sh` script has been enhanced to support multi-region deployment using AWS Code Services (CodePipeline, CodeBuild, and CodeDeploy). This provides automated, scalable, and reliable deployment pipelines with support for canary and blue-green deployment strategies.

## New Features

### 🚀 AWS Code Services Integration

- **CodePipeline**: Multi-region deployment orchestration
- **CodeBuild**: Infrastructure and application builds
- **CodeDeploy**: Blue-green and canary deployment strategies
- **CloudWatch**: Automated monitoring and rollback triggers

### 🌍 Multi-Region Deployment

- Parallel deployment across multiple AWS regions
- Cross-region synchronization and monitoring
- Automated failover and disaster recovery

### 📊 Deployment Strategies

- **Canary Deployment**: Gradual traffic shifting (configurable percentage)
- **Blue-Green Deployment**: Zero-downtime deployments
- **Automated Rollback**: Based on CloudWatch alarms

## Usage

### Basic Multi-Region Deployment

```bash
# Deploy with multi-region and CodePipeline enabled
./deploy-unified.sh full -e production --enable-multi-region --enable-code-pipeline

# Deploy with custom canary percentage
./deploy-unified.sh full -e production --enable-multi-region --canary-percentage 20

# Deploy with blue-green strategy
./deploy-unified.sh full -e production --enable-multi-region --blue-green
```

### New Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `--enable-code-pipeline` | Enable AWS CodePipeline multi-region deployment | false |
| `--canary-percentage PCT` | Canary deployment percentage | 10 |
| `--blue-green` | Enable blue-green deployment strategy | false |
| `--pipeline-status` | Show CodePipeline deployment status | - |

### Monitoring and Status

```bash
# Check infrastructure deployment status
./deploy-unified.sh --status -e production -r ap-east-2

# Check CodePipeline deployment status
./deploy-unified.sh --pipeline-status -e production -r ap-east-2
```

## Architecture

### Pipeline Structure

```
Source (S3) → Build (CodeBuild) → Deploy (Multi-Region)
    ↓              ↓                    ↓
Artifacts    Infrastructure      Primary Region
Bucket       + Application       + Replication Regions
```

### Components Created

#### CodePipeline
- **Pipeline Name**: `{PROJECT_NAME}-{ENVIRONMENT}-multi-region-pipeline`
- **Stages**: Source, Build, Deploy Infrastructure, Deploy Application
- **Artifacts**: Stored in S3 with versioning enabled

#### CodeBuild Projects
1. **Infrastructure Build**: `{PROJECT_NAME}-{ENVIRONMENT}-infrastructure-build`
   - CDK synthesis and testing
   - CloudFormation template generation
   
2. **Application Build**: `{PROJECT_NAME}-{ENVIRONMENT}-application-build`
   - Spring Boot application compilation
   - Docker image building and pushing to ECR

#### CodeDeploy Applications
- **Application Name**: `{PROJECT_NAME}-{ENVIRONMENT}-app`
- **Deployment Groups**: One per region
- **Deployment Configurations**:
  - Canary: `{PROJECT_NAME}-{ENVIRONMENT}-canary-10-percent`
  - Blue-Green: `{PROJECT_NAME}-{ENVIRONMENT}-blue-green`

### IAM Roles Created

The script automatically creates the following IAM roles:

1. **CodePipelineServiceRole**: For pipeline execution
2. **CodeBuildServiceRole**: For build project execution
3. **CodeDeployServiceRole**: For deployment execution
4. **CloudFormationServiceRole**: For infrastructure deployment

## Deployment Flow

### Phase 1: Foundation Infrastructure
- Network, Security, IAM, Certificates
- Deployed in primary region first, then replication regions

### Phase 2: Data Layer
- RDS Aurora Global Database
- ElastiCache, MSK with cross-region replication
- Deployed with proper dependencies

### Phase 3: Compute Layer
- EKS clusters in all regions
- Application load balancers
- Auto-scaling configurations

### Phase 4: Global Services
- Route53 global routing
- CloudFront CDN
- Cross-region synchronization

### Phase 5: Code Services Pipeline
- CodePipeline creation
- CodeBuild project setup
- CodeDeploy application configuration
- CloudWatch alarms for auto-rollback

## Monitoring and Alerting

### CloudWatch Alarms

Automatically created alarms for each region:

- **High Error Rate**: > 10 errors in 2 minutes
- **High Response Time**: > 2 seconds average
- **Deployment Failures**: Automatic rollback triggers

### Metrics Tracked

- Pipeline execution status
- Build success/failure rates
- Deployment success rates
- Application performance metrics
- Cross-region synchronization health

## Rollback Strategy

### Automatic Rollback Triggers

1. **CloudWatch Alarms**: High error rate or response time
2. **Deployment Failures**: CodeDeploy deployment failures
3. **Health Check Failures**: Application health check failures

### Manual Rollback

```bash
# Rollback via CodeDeploy
aws deploy stop-deployment --deployment-id <deployment-id> --auto-rollback-enabled

# Rollback via Pipeline
aws codepipeline stop-pipeline-execution --pipeline-name <pipeline-name> --pipeline-execution-id <execution-id>
```

## Troubleshooting

### Common Issues

1. **IAM Permission Errors**
   - Ensure AWS credentials have sufficient permissions
   - Check if service roles were created successfully

2. **Build Failures**
   - Check CodeBuild logs in CloudWatch
   - Verify source code is available in S3 artifacts bucket

3. **Deployment Failures**
   - Check CodeDeploy deployment logs
   - Verify target infrastructure is healthy

### Debug Commands

```bash
# Check pipeline execution details
aws codepipeline get-pipeline-execution --pipeline-name <pipeline-name> --pipeline-execution-id <execution-id>

# Check build logs
aws logs get-log-events --log-group-name /aws/codebuild/<project-name> --log-stream-name <stream-name>

# Check deployment status
aws deploy get-deployment --deployment-id <deployment-id>
```

## Best Practices

### Security
- Use least-privilege IAM roles
- Enable CloudTrail for audit logging
- Encrypt artifacts in S3
- Use VPC endpoints for private communication

### Performance
- Use build caching to speed up builds
- Implement parallel deployments where possible
- Monitor and optimize build times
- Use appropriate instance types for builds

### Cost Optimization
- Use spot instances for non-critical builds
- Implement build artifact lifecycle policies
- Monitor CodeBuild usage and optimize
- Use reserved capacity for predictable workloads

## Integration with Existing Infrastructure

### CDK Stacks
The pipeline integrates with existing CDK stacks:
- Reuses existing IAM roles where possible
- Maintains existing dependencies
- Preserves current configuration patterns

### Monitoring
Integrates with existing observability stack:
- CloudWatch dashboards
- SNS notifications
- X-Ray tracing
- Custom metrics

## Testing

Run the test suite to verify functionality:

```bash
./test-deploy-script.sh
```

This validates:
- Script syntax and functions
- Parameter parsing
- Help message content
- Dry-run functionality
- AWS CLI integration

## Support

For issues or questions:
1. Check CloudWatch logs for detailed error messages
2. Use `--dry-run` to validate configuration
3. Use `--pipeline-status` to monitor deployment progress
4. Review IAM permissions if access errors occur

## Future Enhancements

Planned improvements:
- Integration with AWS CodeStar for project management
- Support for AWS CodeGuru for code quality analysis
- Enhanced monitoring with AWS X-Ray integration
- Support for multi-account deployments