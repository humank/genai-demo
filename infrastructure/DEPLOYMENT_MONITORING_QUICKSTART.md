# Deployment Monitoring - Quick Start Guide

## 5-Minute Setup

### Step 1: Deploy the Stack (2 minutes)

```bash
cd infrastructure
cdk deploy DeploymentMonitoring --require-approval never
```

### Step 2: Subscribe to Alerts (1 minute)

```bash
# Get the topic ARN
TOPIC_ARN=$(aws cloudformation describe-stacks \
    --stack-name DeploymentMonitoring \
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentAlertTopicArn`].OutputValue' \
    --output text)

# Subscribe your email
aws sns subscribe \
    --topic-arn $TOPIC_ARN \
    --protocol email \
    --notification-endpoint your-email@example.com

# Confirm subscription in your email
```

### Step 3: Access Dashboard (1 minute)

```bash
# Get dashboard URL
aws cloudformation describe-stacks \
    --stack-name DeploymentMonitoring \
    --query 'Stacks[0].Outputs[?OutputKey==`DeploymentDashboardUrl`].OutputValue' \
    --output text

# Or open directly
open "https://$(aws configure get region).console.aws.amazon.com/cloudwatch/home?region=$(aws configure get region)#dashboards:name=genai-demo-production-deployment-monitoring"
```

### Step 4: Verify Metrics (1 minute)

```bash
# Wait 5 minutes for first metrics collection, then check
aws cloudwatch get-metric-statistics \
    --namespace genai-demo/Deployment \
    --metric-name PipelineSuccessRate \
    --start-time $(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average
```

## What You Get

### 📊 Real-time Dashboard

- Pipeline success rates
- Deployment success rates
- Execution time trends
- Failure tracking

### 🔔 Instant Alerts

You'll receive email notifications for:
- Any pipeline failure
- Any deployment failure
- Success rate drops below 80%
- Deployments taking longer than 30 minutes
- Pipeline executions taking longer than 60 minutes

### 📈 Historical Data

- Track deployment trends over time
- Identify patterns in failures
- Monitor performance improvements
- Analyze deployment frequency

## Common Use Cases

### Monitor Production Deployments

```bash
# Deploy with production configuration
cdk deploy DeploymentMonitoring \
    -c environment=production \
    -c projectName=genai-demo
```

### Multi-Region Monitoring

```typescript
new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    multiRegionConfig: {
        enabled: true,
        regions: ['us-east-1', 'us-west-2', 'eu-west-1'],
        primaryRegion: 'us-east-1',
    },
});
```

### Integration with Existing Alerts

```typescript
new DeploymentMonitoringStack(app, 'DeploymentMonitoring', {
    projectName: 'genai-demo',
    environment: 'production',
    alertingTopic: existingAlertingStack.alertTopic,
});
```

## Troubleshooting

### No Metrics Showing?

```bash
# Check Lambda logs
aws logs tail /aws/lambda/DeploymentMonitoring-DeploymentMetricsFunction --follow

# Verify Lambda is running
aws lambda list-functions --query 'Functions[?contains(FunctionName, `DeploymentMetrics`)]'
```

### Not Receiving Alerts?

```bash
# Check SNS subscriptions
aws sns list-subscriptions-by-topic --topic-arn $TOPIC_ARN

# Verify subscription is confirmed
aws sns get-subscription-attributes --subscription-arn <subscription-arn>
```

### Dashboard Not Loading?

```bash
# Verify dashboard exists
aws cloudwatch list-dashboards | grep deployment-monitoring

# Check dashboard content
aws cloudwatch get-dashboard --dashboard-name genai-demo-production-deployment-monitoring
```

## Next Steps

1. **Customize Thresholds**: Adjust alarm thresholds based on your deployment patterns
2. **Add More Subscribers**: Subscribe additional team members or tools
3. **Integrate with Tools**: Connect to PagerDuty, Slack, or other incident management tools
4. **Review Regularly**: Check dashboard weekly to identify improvement opportunities

## Cost

**~$12/month per environment**

- Very cost-effective for the visibility provided
- Scales with deployment frequency
- No additional charges for viewing dashboard or receiving alerts

## Support

For detailed documentation, see:
- [Full Documentation](./DEPLOYMENT_MONITORING.md)
- [AWS Code Services Guide](./AWS_CODE_SERVICES_DEPLOYMENT.md)
- [Integration Examples](./examples/deployment-monitoring-integration.ts)

---

**Ready to deploy?** Run `cdk deploy DeploymentMonitoring` now!
