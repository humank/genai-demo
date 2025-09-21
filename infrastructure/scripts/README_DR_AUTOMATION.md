# Disaster Recovery Automation Scripts

This directory contains scripts and tools for managing and testing the Enhanced Disaster Recovery Automation system.

## Overview

The DR automation system provides fully automated disaster recovery capabilities including:

- **Automated Failover**: Automatic promotion of Aurora Global Database and DNS routing updates
- **Chaos Engineering**: Automated testing and validation of DR readiness
- **Monthly Testing**: Scheduled comprehensive DR tests
- **Monitoring**: Real-time monitoring and alerting for DR components

## Scripts

### dr-automation-test.js

Main testing and validation script for DR automation.

#### Installation

```bash
# Install dependencies
cd infrastructure
npm install

# Make script executable
chmod +x scripts/dr-automation-test.js
```

#### Usage

##### Test Failover Workflow

```bash
# Basic failover test
node scripts/dr-automation-test.js test-failover \
  --project genai-demo \
  --environment production

# Full failover test with all parameters
node scripts/dr-automation-test.js test-failover \
  --project genai-demo \
  --environment production \
  --primary-health-check-id ABCD1234567890 \
  --secondary-health-check-id EFGH1234567890 \
  --global-cluster-id genai-demo-global-cluster \
  --secondary-cluster-id genai-demo-secondary-cluster \
  --hosted-zone-id Z1234567890ABC \
  --domain-name api.kimkao.io \
  --secondary-alb-dns genai-demo-alb-dr-123456789.ap-northeast-1.elb.amazonaws.com \
  --notification-topic-arn arn:aws:sns:ap-northeast-1:123456789012:genai-demo-dr-alerts
```

##### Run Chaos Engineering Tests

```bash
# Monthly DR test
node scripts/dr-automation-test.js test-chaos \
  --project genai-demo \
  --environment production \
  --test-type monthly_dr_test

# Health check failure simulation
node scripts/dr-automation-test.js test-chaos \
  --project genai-demo \
  --environment production \
  --test-type health_check_failure

# Network partition simulation
node scripts/dr-automation-test.js test-chaos \
  --project genai-demo \
  --environment production \
  --test-type network_partition
```

##### Validate DR Readiness

```bash
# Comprehensive DR readiness validation
node scripts/dr-automation-test.js validate \
  --project genai-demo \
  --environment production
```

##### Generate DR Report

```bash
# Generate comprehensive DR report
node scripts/dr-automation-test.js report \
  --project genai-demo \
  --environment production \
  --output dr-report-$(date +%Y%m%d).json
```

#### NPM Scripts

The following NPM scripts are available for convenience:

```bash
# Test failover workflow
npm run dr:test-failover -- --project genai-demo --environment production

# Run chaos engineering tests
npm run dr:test-chaos -- --project genai-demo --environment production

# Validate DR readiness
npm run dr:validate -- --project genai-demo --environment production

# Generate DR report
npm run dr:report -- --project genai-demo --environment production --output report.json
```

## Configuration

### Environment Variables

Set the following environment variables before running the scripts:

```bash
export AWS_REGION=ap-northeast-1
export AWS_PROFILE=your-aws-profile  # Optional
```

### AWS Credentials

Ensure you have appropriate AWS credentials configured with the following permissions:

- **Step Functions**: Execute state machines
- **Lambda**: Invoke functions
- **CloudWatch**: Read metrics and alarms
- **Route 53**: Read health checks
- **RDS**: Read cluster information
- **Systems Manager**: Read parameters
- **SNS**: Publish messages (for notifications)

### Required IAM Permissions

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "states:StartExecution",
                "states:DescribeExecution",
                "states:DescribeStateMachine",
                "lambda:InvokeFunction",
                "cloudwatch:GetMetricStatistics",
                "cloudwatch:DescribeAlarms",
                "route53:GetHealthCheck",
                "route53:ListHealthChecks",
                "rds:DescribeDBClusters",
                "rds:DescribeGlobalClusters",
                "ssm:GetParameter",
                "ssm:GetParameters",
                "sns:Publish"
            ],
            "Resource": "*"
        }
    ]
}
```

## Test Types

### Failover Tests

- **Health Check Validation**: Verifies health check responsiveness
- **Aurora Promotion**: Tests Aurora Global Database failover
- **DNS Updates**: Validates DNS routing changes
- **End-to-End**: Complete failover workflow

### Chaos Engineering Tests

- **Monthly DR Test**: Comprehensive monthly validation
- **Health Check Failure**: Simulates health check failures
- **Network Partition**: Tests network connectivity issues
- **Database Failure**: Simulates database unavailability

### Validation Tests

- **Health Checks**: Validates health check configuration
- **Aurora Global Database**: Checks replication status
- **DNS Configuration**: Verifies DNS setup
- **Automation Components**: Tests Step Functions and Lambda
- **Monitoring**: Validates CloudWatch and alerting

## Output Examples

### Successful Failover Test

```
🚀 Starting failover workflow test...
✅ DR automation configuration loaded
✅ Failover workflow started: arn:aws:states:ap-northeast-1:123456789012:execution:genai-demo-production-dr-failover:dr-failover-test-1640995200000
⏳ Monitoring execution...
   Status: RUNNING
   Status: RUNNING
   Status: SUCCEEDED
✅ Execution completed successfully
📋 Execution output: {
  "statusCode": 200,
  "success": true,
  "operations": [
    {
      "step": "health_validation",
      "success": true,
      "details": { ... }
    },
    {
      "step": "aurora_promotion",
      "success": true,
      "details": { ... }
    },
    {
      "step": "dns_update",
      "success": true,
      "details": { ... }
    }
  ],
  "timestamp": "2024-01-01T12:00:00.000Z"
}
```

### DR Readiness Validation

```
🔍 Validating DR readiness...
✅ DR automation configuration loaded
  🔍 Validating health checks...
  🔍 Validating Aurora Global Database...
  🔍 Validating DNS configuration...
  🔍 Validating automation components...
  🔍 Validating monitoring and alerting...
✅ DR system is ready
📊 DR Readiness: 5/5 checks passed (100%)
```

### DR Report

```
📊 Generating DR report...
✅ DR automation configuration loaded

📋 DR REPORT
==================================================
Project: genai-demo
Environment: production
Overall Status: READY
Checks Passed: 5/5

🔧 RECOMMENDATIONS:
1. DR system is fully operational. Consider running monthly chaos tests to maintain readiness.
```

## Troubleshooting

### Common Issues

#### 1. Configuration Not Found

```
❌ Failed to load DR automation configuration: ParameterNotFound
```

**Solution**: Ensure the DR automation stack has been deployed and the configuration parameter exists.

```bash
# Check if parameter exists
aws ssm get-parameter --name "/genai-demo/production/dr/automation-config"
```

#### 2. Insufficient Permissions

```
❌ Failover workflow test failed: AccessDenied
```

**Solution**: Verify your AWS credentials have the required permissions listed above.

#### 3. State Machine Not Found

```
❌ Execution failed: StateMachineDoesNotExist
```

**Solution**: Ensure the DR automation infrastructure has been deployed.

```bash
# List state machines
aws stepfunctions list-state-machines --query 'stateMachines[?contains(name, `genai-demo-production-dr`)]'
```

#### 4. Health Check Issues

```
❌ Health check validation failed: HealthCheckNotFound
```

**Solution**: Verify health checks exist and are properly configured.

```bash
# List health checks
aws route53 list-health-checks --query 'HealthChecks[?contains(CallerReference, `genai-demo`)]'
```

### Debug Mode

Enable debug logging by setting the LOG_LEVEL environment variable:

```bash
export LOG_LEVEL=DEBUG
node scripts/dr-automation-test.js validate --project genai-demo --environment production
```

### Manual Verification

You can manually verify DR components using AWS CLI:

```bash
# Check Step Functions state machines
aws stepfunctions list-state-machines --query 'stateMachines[?contains(name, `genai-demo`)]'

# Check Lambda functions
aws lambda list-functions --query 'Functions[?contains(FunctionName, `genai-demo-dr`)]'

# Check CloudWatch dashboards
aws cloudwatch list-dashboards --query 'DashboardEntries[?contains(DashboardName, `genai-demo`)]'

# Check EventBridge rules
aws events list-rules --query 'Rules[?contains(Name, `genai-demo`)]'
```

## Integration with CI/CD

### GitHub Actions

Add DR testing to your GitHub Actions workflow:

```yaml
- name: Test DR Automation
  run: |
    cd infrastructure
    npm install
    npm run dr:validate -- --project genai-demo --environment production
  env:
    AWS_REGION: ap-northeast-1
    AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
    AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
```

### Scheduled Testing

Set up scheduled DR testing using cron:

```bash
# Add to crontab for monthly testing
0 2 1 * * cd /path/to/infrastructure && npm run dr:test-chaos -- --project genai-demo --environment production --test-type monthly_dr_test
```

## Monitoring and Alerting

### CloudWatch Metrics

The DR automation system publishes custom metrics:

- `DR/FailoverSuccess` - Successful failover operations
- `DR/FailoverFailure` - Failed failover operations
- `DR/TestSuccess` - Successful chaos tests
- `DR/TestFailure` - Failed chaos tests
- `DR/ReadinessScore` - Overall DR readiness score (0-100)

### SNS Notifications

Configure SNS subscriptions for DR alerts:

```bash
# Subscribe to DR alerts
aws sns subscribe \
  --topic-arn arn:aws:sns:ap-northeast-1:123456789012:genai-demo-production-dr-alerts \
  --protocol email \
  --notification-endpoint your-email@example.com
```

## Best Practices

1. **Regular Testing**: Run DR tests monthly or after significant infrastructure changes
2. **Monitor Metrics**: Set up CloudWatch alarms for DR metrics
3. **Document Changes**: Update DR documentation when making infrastructure changes
4. **Validate Configuration**: Always validate DR readiness after deployments
5. **Review Reports**: Regularly review DR reports and address recommendations
6. **Test Scenarios**: Test different failure scenarios, not just happy path
7. **Update Procedures**: Keep DR procedures updated with infrastructure changes

## Support

For issues or questions:

1. Check the troubleshooting section above
2. Review CloudWatch logs for detailed error information
3. Consult the main DR automation documentation
4. Contact the DevOps team for assistance

## Related Documentation

- [DR Automation Implementation Guide](../docs/DR_AUTOMATION_IMPLEMENTATION.md)
- [Infrastructure Troubleshooting Guide](../TROUBLESHOOTING.md)
- [Multi-Region Architecture Documentation](infrastructure/MULTI_REGION_ARCHITECTURE.md)
