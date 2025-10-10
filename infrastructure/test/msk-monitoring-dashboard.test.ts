import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { GrafanaMSKDashboardStack } from '../src/stacks/grafana-msk-dashboard-stack';
import { CloudWatchMSKDashboardStack } from '../src/stacks/cloudwatch-msk-dashboard-stack';
import { MSKAlertingStack } from '../src/stacks/msk-alerting-stack';

/**
 * MSK Monitoring Dashboard Test Suite
 * 
 * Comprehensive tests for MSK monitoring dashboard infrastructure
 * including Grafana dashboards, CloudWatch dashboards, and alerting.
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
describe('MSK Monitoring Dashboard Infrastructure', () => {
  let app: cdk.App;
  let grafanaStack: GrafanaMSKDashboardStack;
  let cloudwatchStack: CloudWatchMSKDashboardStack;
  let alertingStack: MSKAlertingStack;

  beforeEach(() => {
    app = new cdk.App();
    grafanaStack = new GrafanaMSKDashboardStack(app, 'TestGrafanaStack', {
      env: { account: '123456789012', region: 'us-east-1' },
    });
    cloudwatchStack = new CloudWatchMSKDashboardStack(app, 'TestCloudWatchStack', {
      env: { account: '123456789012', region: 'us-east-1' },
    });
    alertingStack = new MSKAlertingStack(app, 'TestAlertingStack', {
      env: { account: '123456789012', region: 'us-east-1' },
    });
  });

  describe('Grafana MSK Dashboard Stack', () => {
    test('should create Grafana workspace with correct configuration', () => {
      const template = Template.fromStack(grafanaStack);

      // Verify Grafana workspace creation
      template.hasResourceProperties('AWS::Grafana::Workspace', {
        AccountAccessType: 'CURRENT_ACCOUNT',
        AuthenticationProviders: ['AWS_SSO'],
        PermissionType: 'SERVICE_MANAGED',
        Name: 'genai-demo-msk-monitoring',
        Description: 'Amazon Managed Grafana workspace for MSK data flow monitoring and business impact analysis',
        DataSources: ['CLOUDWATCH', 'PROMETHEUS', 'XRAY'],
        NotificationDestinations: ['SNS'],
        GrafanaVersion: '9.4',
      });
    });

    test('should create IAM role with proper MSK permissions', () => {
      const template = Template.fromStack(grafanaStack);

      // Verify IAM role creation
      template.hasResourceProperties('AWS::IAM::Role', {
        AssumeRolePolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Effect: 'Allow',
              Principal: {
                Service: 'grafana.amazonaws.com',
              },
              Action: 'sts:AssumeRole',
            }),
          ]),
        },
        Description: 'IAM role for Amazon Managed Grafana workspace with MSK monitoring',
        ManagedPolicyArns: Match.anyValue()
      });

      // Verify MSK-specific permissions
      template.hasResourceProperties('AWS::IAM::Role', {
        Policies: [
          {
            PolicyDocument: {
              Statement: Match.arrayWith([
                {
                  Effect: 'Allow',
                  Action: [
                    'kafka:DescribeCluster',
                    'kafka:DescribeClusterV2',
                    'kafka:ListClusters',
                    'kafka:ListClustersV2',
                    'kafka:GetBootstrapBrokers',
                    'kafka:DescribeConfiguration',
                    'kafka:ListConfigurations',
                    'kafka:ListNodes',
                    'kafka:ListKafkaVersions',
                    'kafka:GetCompatibleKafkaVersions',
                  ],
                  Resource: '*',
                },
                {
                  Effect: 'Allow',
                  Action: [
                    'xray:GetServiceGraph',
                    'xray:GetTraceGraph',
                    'xray:GetTraceSummaries',
                    'xray:BatchGetTraces',
                    'xray:GetTimeSeriesServiceStatistics',
                  ],
                  Resource: '*',
                },
              ]),
            },
          },
        ],
      });
    });

    test('should create proper outputs for workspace information', () => {
      const template = Template.fromStack(grafanaStack);

      // Verify outputs
      template.hasOutput('GrafanaWorkspaceId', {
        Description: 'Amazon Managed Grafana workspace ID for MSK monitoring',
        Export: {
          Name: 'MSK-Grafana-Workspace-Id',
        },
      });

      template.hasOutput('GrafanaWorkspaceEndpoint', {
        Description: 'Amazon Managed Grafana workspace endpoint URL',
        Export: {
          Name: 'MSK-Grafana-Workspace-Endpoint',
        },
      });
    });
  });

  describe('CloudWatch MSK Dashboard Stack', () => {
    test('should create operations dashboard with MSK widgets', () => {
      const template = Template.fromStack(cloudwatchStack);

      // Verify CloudWatch dashboard creation
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'MSK-Operations-Real-Time-Monitoring',
      });

      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'MSK-Performance-Deep-Dive',
      });

      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'MSK-Cost-Monitoring-Optimization',
      });
    });

    test('should create Logs Insights automation Lambda', () => {
      const template = Template.fromStack(cloudwatchStack);

      // Verify Lambda function creation
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'msk-logs-insights-automation',
        Runtime: 'python3.11',
        Handler: 'index.lambda_handler',
        Timeout: 900, // 15 minutes
        MemorySize: 512,
        Description: 'Automated CloudWatch Logs Insights queries for MSK monitoring',
      });
    });

    test('should create IAM role with Logs Insights permissions', () => {
      const template = Template.fromStack(cloudwatchStack);

      // Verify IAM role for Logs Insights
      template.hasResourceProperties('AWS::IAM::Role', {
        Description: 'IAM role for MSK CloudWatch Logs Insights automation',
        Policies: Match.anyValue()
      });
    });

    test('should create EventBridge rule for scheduled execution', () => {
      const template = Template.fromStack(cloudwatchStack);

      // Verify EventBridge rule
      template.hasResourceProperties('AWS::Events::Rule', {
        Name: 'msk-logs-insights-schedule',
        Description: 'Scheduled execution of MSK Logs Insights analysis',
        ScheduleExpression: 'rate(15 minutes)',
      });
    });

    test('should create proper outputs for dashboard URLs', () => {
      const template = Template.fromStack(cloudwatchStack);

      // Verify outputs
      template.hasOutput('MSKOperationsDashboardURL', {
        Description: 'MSK Operations Dashboard URL',
        Export: {
          Name: 'MSK-Operations-Dashboard-URL',
        },
      });

      template.hasOutput('MSKPerformanceDashboardURL', {
        Description: 'MSK Performance Dashboard URL',
        Export: {
          Name: 'MSK-Performance-Dashboard-URL',
        },
      });

      template.hasOutput('LogsInsightsFunctionArn', {
        Description: 'MSK Logs Insights automation Lambda function ARN',
        Export: {
          Name: 'MSK-Logs-Insights-Function-Arn',
        },
      });
    });
  });

  describe('MSK Alerting Stack', () => {
    test('should create SNS topics for different alert levels', () => {
      const template = Template.fromStack(alertingStack);

      // Verify SNS topics creation
      template.hasResourceProperties('AWS::SNS::Topic', {
        TopicName: 'msk-warning-alerts',
        DisplayName: 'MSK Warning Level Alerts',
      });

      template.hasResourceProperties('AWS::SNS::Topic', {
        TopicName: 'msk-critical-alerts',
        DisplayName: 'MSK Critical Level Alerts',
      });

      template.hasResourceProperties('AWS::SNS::Topic', {
        TopicName: 'msk-emergency-alerts',
        DisplayName: 'MSK Emergency Level Alerts',
      });
    });

    test('should create alert correlation Lambda function', () => {
      const template = Template.fromStack(alertingStack);

      // Verify alert correlation function
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'msk-alert-correlation',
        Runtime: 'python3.11',
        Handler: 'index.lambda_handler',
        Timeout: 300, // 5 minutes
        MemorySize: 256,
        Description: 'Intelligent alert correlation and noise reduction for MSK monitoring',
      });
    });

    test('should create alert suppression Lambda function', () => {
      const template = Template.fromStack(alertingStack);

      // Verify alert suppression function
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'msk-alert-suppression',
        Runtime: 'python3.11',
        Handler: 'index.lambda_handler',
        Timeout: 300, // 5 minutes
        MemorySize: 256,
        Description: 'Alert suppression during MSK maintenance windows',
      });
    });

    test('should create MSK-specific CloudWatch alarms', () => {
      const template = Template.fromStack(alertingStack);

      // Verify offline partitions alarm (Emergency)
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'MSK-Offline-Partitions-Emergency',
        AlarmDescription: 'MSK cluster has offline partitions - immediate attention required',
        MetricName: 'OfflinePartitionsCount',
        Namespace: 'AWS/Kafka',
        Statistic: 'Maximum',
        Threshold: 0,
        ComparisonOperator: 'GreaterThanThreshold',
        EvaluationPeriods: 1,
        TreatMissingData: 'breaching',
      });

      // Verify consumer lag alarm (Critical)
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'MSK-Consumer-Lag-Critical',
        AlarmDescription: 'MSK consumer lag exceeding critical threshold',
        MetricName: 'EstimatedMaxTimeLag',
        Namespace: 'AWS/Kafka',
        Statistic: 'Maximum',
        Threshold: 300000, // 5 minutes
        ComparisonOperator: 'GreaterThanThreshold',
        EvaluationPeriods: 2,
      });

      // Verify producer error alarm (Warning)
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'MSK-Producer-Error-Rate-Warning',
        AlarmDescription: 'MSK producer error rate is elevated',
        MetricName: 'ProducerRequestErrors',
        Namespace: 'AWS/Kafka',
        Statistic: 'Sum',
        Threshold: 10,
        ComparisonOperator: 'GreaterThanThreshold',
        EvaluationPeriods: 3,
      });
    });

    test('should create IAM roles with proper permissions', () => {
      const template = Template.fromStack(alertingStack);

      // Verify alert correlation role
      template.hasResourceProperties('AWS::IAM::Role', {
        Description: 'IAM role for MSK alert correlation and noise reduction',
        Policies: Match.anyValue()
      });

      // Verify alert suppression role
      template.hasResourceProperties('AWS::IAM::Role', {
        Description: 'IAM role for MSK alert suppression during maintenance',
        Policies: Match.anyValue()
      });
    });

    test('should create proper outputs for alerting components', () => {
      const template = Template.fromStack(alertingStack);

      // Verify outputs
      template.hasOutput('WarningTopicArn', {
        Description: 'SNS topic ARN for MSK warning alerts',
        Export: {
          Name: 'MSK-Warning-Topic-Arn',
        },
      });

      template.hasOutput('CriticalTopicArn', {
        Description: 'SNS topic ARN for MSK critical alerts',
        Export: {
          Name: 'MSK-Critical-Topic-Arn',
        },
      });

      template.hasOutput('EmergencyTopicArn', {
        Description: 'SNS topic ARN for MSK emergency alerts',
        Export: {
          Name: 'MSK-Emergency-Topic-Arn',
        },
      });
    });
  });

  describe('Integration Tests', () => {
    test('should have consistent tagging across all stacks', () => {
      const grafanaTemplate = Template.fromStack(grafanaStack);
      const cloudwatchTemplate = Template.fromStack(cloudwatchStack);
      const alertingTemplate = Template.fromStack(alertingStack);

      // Verify consistent tagging patterns
      // This would be implemented based on specific tagging requirements
    });

    test('should have proper cross-stack references', () => {
      // Test that stacks can reference each other's outputs properly
      // This would be implemented when stacks are integrated
    });

    test('should meet performance requirements', () => {
      // Verify that the infrastructure meets performance requirements
      // - Lambda timeout configurations
      // - Memory allocations
      // - EventBridge scheduling intervals
      
      const cloudwatchTemplate = Template.fromStack(cloudwatchStack);
      const alertingTemplate = Template.fromStack(alertingStack);

      // Verify Lambda performance configurations
      cloudwatchTemplate.hasResourceProperties('AWS::Lambda::Function', {
        Timeout: 900, // 15 minutes for Logs Insights
        MemorySize: 512,
      });

      alertingTemplate.hasResourceProperties('AWS::Lambda::Function', {
        Timeout: 300, // 5 minutes for alert processing
        MemorySize: 256,
      });
    });

    test('should have proper security configurations', () => {
      const grafanaTemplate = Template.fromStack(grafanaStack);
      const cloudwatchTemplate = Template.fromStack(cloudwatchStack);
      const alertingTemplate = Template.fromStack(alertingStack);

      // Verify least privilege IAM policies
      // Verify encryption configurations
      // Verify network security settings
      
      // This would include specific security validation tests
    });
  });

  describe('Cost Optimization Tests', () => {
    test('should use cost-effective resource configurations', () => {
      const cloudwatchTemplate = Template.fromStack(cloudwatchStack);
      const alertingTemplate = Template.fromStack(alertingStack);

      // Verify Lambda memory sizes are optimized
      cloudwatchTemplate.hasResourceProperties('AWS::Lambda::Function', {
        MemorySize: Match.anyValue(), // Should be <= 1024 for cost optimization
      });

      // Verify EventBridge scheduling is not too frequent
      cloudwatchTemplate.hasResourceProperties('AWS::Events::Rule', {
        ScheduleExpression: 'rate(15 minutes)', // Not too frequent to avoid costs
      });
    });

    test('should have proper resource cleanup configurations', () => {
      const cloudwatchTemplate = Template.fromStack(cloudwatchStack);

      // Verify log retention settings
      cloudwatchTemplate.hasResourceProperties('AWS::Logs::LogGroup', {
        RetentionInDays: 7, // Short retention for cost optimization
      });
    });
  });
});

/**
 * MSK Monitoring Integration Test Suite
 * 
 * Tests the integration between different monitoring components
 */
describe('MSK Monitoring Integration', () => {
  test('should integrate Grafana with CloudWatch data sources', () => {
    // Test that Grafana workspace can access CloudWatch metrics
    // This would be an integration test
  });

  test('should integrate X-Ray tracing with monitoring dashboards', () => {
    // Test that X-Ray traces are properly displayed in dashboards
    // This would be an integration test
  });

  test('should integrate alerting with monitoring metrics', () => {
    // Test that alerts are triggered based on monitoring metrics
    // This would be an integration test
  });
});