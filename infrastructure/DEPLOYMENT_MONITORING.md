# Deployment Monitoring Stack

## Overview

The Deployment Monitoring Stack provides comprehensive monitoring and alerting for AWS Code Services deployments, including CodePipeline and CodeDeploy. This stack addresses the monitoring requirements for multi-region active-active deployments.

## Features

### 📊 Deployment Metrics Collection

Automatically collects and publishes the following metrics to CloudWatch:

#### CodePipeline Metrics
- **Pipeline Success Rate**: Percentage of successful pipeline executions
- **Pipeline Execution Time**: Average time for pipeline executions to complete
- **Pipeline Failures**: Count of failed pipeline executions

#### CodeDeploy Metrics
- **Deployment Success Rate**: Percentage of successful deployments
- **Deployment Time**: Average time for deployments to complete
- **Deployment Failures**: Count of failed deployments

### 🔔 Real-time Alerts

Automated alerts for deployment issues:

1. **Pipeline Failure Alert**: Triggered when any pipeline execution fails
2. **Deployment Failure Alert**: Triggered when any deployment fails
3. **Low Success Rate Alert**: Triggered when deployment success rate drops below 80%
4. **Long Deployment Time Alert**: Triggered when deployment exceeds 30 minutes
5. **Long Pipeline Execution Alert**: Triggered when pipeline execution exceeds 60 minutes

### 📈 CloudWatch Dashboard

Interactive dashboard with:
- Real-time success rate tracking
- Deployment time trends
- Failure tracking and analysis
- Multi-region deployment status (when enabled)

### 🎯 EventBridge Integration

Captures deployment events from:
- CodePipeline state changes (SUCCESS, FAILED)
- CodeDeploy state changes (SUCCESS, FAILURE, STOPPED)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Deployment Monitoring Stack                 │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐      ┌──────────────────────────────┐    │
│  │ EventBridge  │─────▶│  Event Handler Lambdas       │    │
│  │   Rules      │      │  - Pipeline Events           │    │
│  └──────────────┘      │  - Deploy Events             │    │
│         │              └──────────────────────────────┘    │
│         │                          │                        │
│         │                          ▼                        │
│         │              ┌──────────────────────────────┐    │
│         └─────────────▶│   SNS Alert Topic            │    │
│                        └──────────────────────────────┘    │
│                                                               │
│  ┌──────────────┐      ┌──────────────────────────────┐    │
│  │  Scheduled   │─────▶│  Metrics Collection Lambda   │    │
│  │  Rule (5min) │      │  - Collect Pipeline Metrics  │    │
│  └──────────────┘      │  - Collect Deploy Metrics    │    │
│                        │  - Publish to CloudWatch     │    │
│                        └──────────────────────────────┘    │
│                                    │                        │
│                                    ▼                        │
│                        ┌──────────────────────────────┐    │
│                        │   CloudWatch Metrics         │    │
│                        │   - Success Rates            │    │
│                        │   - Execution Times          │    │
│                        │   - Failure Counts           │    │
│                        └──────────────────────────────┘    │
│                                    │                        │
│                                    ▼                        │
│                        ┌──────────────────────────────┐    │
│                        │   CloudWatch Dashboard       │    │
│                        │   - Real-time Visualization  │    │
│                        └──────────────────────────────┘    │
│                                    │                        │
│                                    ▼                        │
│                        ┌──────────────────────────────┐    │
│                        │   CloudWatch Alarms          │    │
│                        │   - Failure Alerts           │    │
│                        │   - Performance Alerts       │    │
│                        └──────────────────────────────┘    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Usage

### Basic Deployment

```typescript
import { DeploymentMonitoringStack } from './stacks/deployment-monitoring-stack';

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
});
```

### With Existing Alerting Topic

```typescript
import { DeploymentMonitoringStack } from './stacks/deployment-monitoring-stack';

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: existingAlertingStack.alertTopic,
});
```

### Multi-Region Configuration

```typescript
const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    multiRegionConfig: {
        enabled: true,
        regions: ['us-east-1', 'us-west-2', 'eu-west-1'],
        primaryRegion: 'us-east-1',
    },
});
```

## Metrics Reference

### Custom Metrics Namespace

All metrics are published to the namespace: `{PROJECT_NAME}/Deployment`

### Metric Dimensions

- **Environment**: The deployment environment (development, staging, production)
- **Service**: The AWS service (CodePipeline, CodeDeploy)

### Metric Details

| Metric Name | Unit | Description | Typical Value |
|-------------|------|-------------|---------------|
| PipelineSuccessRate | Percent | Percentage of successful pipeline executions | > 95% |
| PipelineExecutionTime | Seconds | Average pipeline execution time | 300-1800s |
| PipelineFailures | Count | Number of failed pipeline executions | 0 |
| DeploymentSuccessRate | Percent | Percentage of successful deployments | > 95% |
| DeploymentTime | Seconds | Average deployment time | 180-1200s |
| DeploymentFailures | Count | Number of failed deployments | 0 |

## Alarms Configuration

### Pipeline Failure Alarm

- **Threshold**: ≥ 1 failure
- **Evaluation Period**: 5 minutes
- **Action**: Send SNS notification

### Deployment Failure Alarm

- **Threshold**: ≥ 1 failure
- **Evaluation Period**: 5 minutes
- **Action**: Send SNS notification

### Low Success Rate Alarm

- **Threshold**: < 80% success rate
- **Evaluation Period**: 15 minutes (2 data points)
- **Action**: Send SNS notification

### Long Deployment Time Alarm

- **Threshold**: > 1800 seconds (30 minutes)
- **Evaluation Period**: 5 minutes
- **Action**: Send SNS notification

### Long Pipeline Execution Alarm

- **Threshold**: > 3600 seconds (60 minutes)
- **Evaluation Period**: 5 minutes
- **Action**: Send SNS notification

## Dashboard Access

After deployment, access the dashboard via:

```bash
# Get dashboard URL from stack outputs
aws cloudformation describe-stacks \
    --stack-name DeploymentMonitoring \
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentDashboardUrl`].OutputValue' \
    --output text
```

Or navigate to:
```
https://{region}.console.aws.amazon.com/cloudwatch/home?region={region}#dashboards:name={project}-{environment}-deployment-monitoring
```

## Alert Notifications

### Subscribe to Alerts

```bash
# Subscribe email to deployment alerts
aws sns subscribe \
    --topic-arn $(aws cloudformation describe-stacks \
        --stack-name DeploymentMonitoring \
        --query 'Stacks[0].Outputs[?OutputKey==`DeploymentAlertTopicArn`].OutputValue' \
        --output text) \
    --protocol email \
    --notification-endpoint your-email@example.com
```

### Alert Message Format

#### Pipeline State Change Alert

```
Deployment Alert: CodePipeline State Change

Pipeline: genai-demo-production-multi-region-pipeline
State: FAILED
Execution ID: abc123-def456-ghi789
Time: 2025-01-22T10:30:00Z

⚠️ FAILURE DETECTED
```

#### Deployment State Change Alert

```
Deployment Alert: CodeDeploy State Change

Application: genai-demo-production-app
Deployment Group: production-us-east-1
Deployment ID: d-ABCDEF123
State: FAILURE
Region: us-east-1
Time: 2025-01-22T10:30:00Z

⚠️ FAILURE DETECTED
```

## Monitoring Best Practices

### 1. Set Up Email Notifications

Subscribe key team members to the deployment alert topic:

```bash
aws sns subscribe \
    --topic-arn <deployment-alert-topic-arn> \
    --protocol email \
    --notification-endpoint ops-team@example.com
```

### 2. Review Dashboard Regularly

- Check success rates daily
- Monitor deployment time trends
- Investigate any failures immediately

### 3. Tune Alarm Thresholds

Adjust thresholds based on your deployment patterns:

```typescript
// Example: Custom threshold for long deployment time
const customAlarm = new cloudwatch.Alarm(this, 'CustomDeploymentTimeAlarm', {
    metric: deploymentTimeMetric,
    threshold: 2400, // 40 minutes instead of default 30
    evaluationPeriods: 2,
});
```

### 4. Integrate with Incident Management

Forward SNS notifications to incident management tools:

```bash
# Example: Subscribe PagerDuty endpoint
aws sns subscribe \
    --topic-arn <deployment-alert-topic-arn> \
    --protocol https \
    --notification-endpoint https://events.pagerduty.com/integration/<key>/enqueue
```

## Troubleshooting

### No Metrics Appearing

1. **Check Lambda Execution**:
   ```bash
   aws logs tail /aws/lambda/<metrics-function-name> --follow
   ```

2. **Verify IAM Permissions**:
   - Ensure Lambda has CodePipeline and CodeDeploy read permissions
   - Ensure Lambda has CloudWatch PutMetricData permission

3. **Check EventBridge Rules**:
   ```bash
   aws events list-rules --name-prefix <project-name>
   ```

### Alarms Not Triggering

1. **Verify Alarm State**:
   ```bash
   aws cloudwatch describe-alarms --alarm-names <alarm-name>
   ```

2. **Check Metric Data**:
   ```bash
   aws cloudwatch get-metric-statistics \
       --namespace <project-name>/Deployment \
       --metric-name DeploymentFailures \
       --start-time <start> \
       --end-time <end> \
       --period 300 \
       --statistics Sum
   ```

3. **Verify SNS Subscription**:
   ```bash
   aws sns list-subscriptions-by-topic --topic-arn <topic-arn>
   ```

### Dashboard Not Loading

1. **Check Dashboard Exists**:
   ```bash
   aws cloudwatch list-dashboards
   ```

2. **Verify Dashboard Content**:
   ```bash
   aws cloudwatch get-dashboard --dashboard-name <dashboard-name>
   ```

## Integration with Existing Stacks

### With Observability Stack

```typescript
// In your main stack file
const observability = new ObservabilityStack(app, 'Observability', {
    vpc,
    kmsKey,
    environment: 'production',
});

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: observability.alertTopic, // Reuse existing alert topic
});
```

### With Alerting Stack

```typescript
const alerting = new AlertingStack(app, 'Alerting', {
    environment: 'production',
    applicationName: 'genai-demo',
});

const deploymentMonitoring = new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: alerting.alertTopic,
});
```

## Cost Considerations

### Estimated Monthly Costs

- **Lambda Executions**: ~$0.20 (288 executions/day × 30 days)
- **CloudWatch Metrics**: ~$3.00 (6 custom metrics)
- **CloudWatch Alarms**: ~$5.00 (5 alarms)
- **CloudWatch Dashboard**: ~$3.00 (1 dashboard)
- **SNS Notifications**: ~$0.50 (assuming 100 notifications/month)

**Total Estimated Cost**: ~$12/month per environment

### Cost Optimization Tips

1. **Adjust Collection Frequency**: Change from 5 minutes to 10 minutes if less granularity is acceptable
2. **Reduce Metric Retention**: Use shorter retention periods for non-critical metrics
3. **Consolidate Alarms**: Combine related alarms where possible

## Testing

Run the test suite:

```bash
cd infrastructure
npm test -- deployment-monitoring-stack.test.ts
```

## Related Documentation

- [AWS Code Services Deployment Guide](./AWS_CODE_SERVICES_DEPLOYMENT.md)
- [Observability Stack Documentation](./docs/observability-stack.md)
- [Multi-Region Active-Active Spec](../.kiro/specs/multi-region-active-active/)

## Support

For issues or questions:
1. Check CloudWatch Logs for Lambda function errors
2. Review EventBridge rule configurations
3. Verify IAM permissions for all components
4. Check SNS topic subscriptions

## Future Enhancements

Planned improvements:
- Integration with AWS X-Ray for detailed trace analysis
- Support for custom deployment metrics
- Enhanced multi-region correlation and analysis
- Integration with AWS Cost Explorer for deployment cost tracking
- Automated remediation actions based on deployment failures
