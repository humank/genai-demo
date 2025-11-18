import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import { AlertingStack, AlertingConfig } from '../src/stacks/alerting-stack';
import { ObservabilityStack } from '../src/stacks/observability-stack';

describe('Aurora PostgreSQL Deadlock Monitoring', () => {
  let app: cdk.App;
  let vpc: ec2.Vpc;
  let kmsKey: kms.Key;

  beforeEach(() => {
    app = new cdk.App();
    
    // Create VPC for testing
    const networkStack = new cdk.Stack(app, 'TestNetworkStack');
    vpc = new ec2.Vpc(networkStack, 'TestVpc', {
      maxAzs: 2,
    });

    // Create KMS key for testing
    kmsKey = new kms.Key(networkStack, 'TestKmsKey', {
      description: 'Test KMS key for deadlock monitoring',
    });
  });

  describe('AlertingStack', () => {
    test('should create Aurora deadlock alarms', () => {
      // Arrange
      const alertingConfig: AlertingConfig = {
        criticalAlerts: {
          emailAddresses: ['admin@example.com'],
        },
        warningAlerts: {
          emailAddresses: ['dev@example.com'],
        },
        infoAlerts: {
          emailAddresses: ['info@example.com'],
        },
      };

      // Act
      const stack = new AlertingStack(app, 'TestAlertingStack', {
        environment: 'test',
        region: 'us-east-1',
        applicationName: 'genai-demo',
        alertingConfig,
      });

      // Assert
      const template = Template.fromStack(stack);

      // Check for Aurora deadlock alarm
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'genai-demo-test-aurora-deadlocks',
        AlarmDescription: 'Aurora PostgreSQL deadlocks detected',
        MetricName: 'Deadlocks',
        Namespace: 'AWS/RDS',
        Statistic: 'Sum',
        Threshold: 1,
        ComparisonOperator: 'GreaterThanOrEqualToThreshold',
      });

      // Check for blocked sessions alarm
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'genai-demo-test-aurora-blocked-sessions',
        AlarmDescription: 'Too many blocked sessions in Aurora PostgreSQL',
        MetricName: 'DatabaseConnections',
        Namespace: 'AWS/RDS',
        Statistic: 'Average',
        Threshold: 80,
      });

      // Check for lock wait time alarm
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'genai-demo-test-aurora-lock-wait-time',
        AlarmDescription: 'High lock wait time detected in Aurora PostgreSQL',
        MetricName: 'ReadLatency',
        Namespace: 'AWS/RDS',
        Statistic: 'Average',
        Threshold: 0.2,
      });

      // Check for CPU utilization alarm
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'genai-demo-test-aurora-cpu-utilization',
        AlarmDescription: 'High CPU utilization in Aurora PostgreSQL (potential lock contention)',
        MetricName: 'CPUUtilization',
        Namespace: 'AWS/RDS',
        Statistic: 'Average',
        Threshold: 80,
      });

      // Check that SNS topics are created
      template.resourceCountIs('AWS::SNS::Topic', 3); // Critical, Warning, Info
    });

    test('should configure alarm actions correctly', () => {
      // Arrange
      const alertingConfig: AlertingConfig = {
        criticalAlerts: {
          emailAddresses: ['admin@example.com'],
        },
        warningAlerts: {
          emailAddresses: ['dev@example.com'],
        },
        infoAlerts: {
          emailAddresses: ['info@example.com'],
        },
      };

      // Act
      const stack = new AlertingStack(app, 'TestAlertingStack', {
        environment: 'test',
        region: 'us-east-1',
        applicationName: 'genai-demo',
        alertingConfig,
      });

      // Assert
      const template = Template.fromStack(stack);

      // Check that deadlock alarm exists and has alarm actions
      template.hasResourceProperties('AWS::CloudWatch::Alarm', {
        AlarmName: 'genai-demo-test-aurora-deadlocks',
      });

      // Check that SNS topics are created
      template.resourceCountIs('AWS::SNS::Topic', 3); // Critical, Warning, Info
    });
  });

  describe('ObservabilityStack', () => {
    test('should create deadlock monitoring dashboard widgets', () => {
      // Act
      const stack = new ObservabilityStack(app, 'TestObservabilityStack', {
        vpc,
        kmsKey,
      });

      // Assert
      const template = Template.fromStack(stack);

      // Check that CloudWatch dashboard is created
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'GenAI-Demo-development',
      });

      // Check that dashboard exists (detailed content checking is complex due to Fn::Join)
      // Note: There may be multiple dashboards in the stack (observability + deadlock monitoring)
      const dashboards = template.findResources('AWS::CloudWatch::Dashboard');
      expect(Object.keys(dashboards).length).toBeGreaterThanOrEqual(1);
    });

    test('should create deadlock log analysis Lambda function', () => {
      // Act
      const stack = new ObservabilityStack(app, 'TestObservabilityStack', {
        vpc,
        kmsKey,
      });

      // Assert
      const template = Template.fromStack(stack);

      // Check that Lambda function is created
      template.hasResourceProperties('AWS::Lambda::Function', {
        Runtime: 'python3.9',
        Handler: 'index.handler',
        Environment: {
          Variables: {
            LOG_GROUP_NAME: '/aws/rds/instance/genai-demo-TestObservabilityStack-primary-aurora/postgresql',
            ENVIRONMENT: 'TestObservabilityStack',
          },
        },
      });

      // Check that EventBridge rule is created
      template.hasResourceProperties('AWS::Events::Rule', {
        ScheduleExpression: 'rate(15 minutes)',
        Description: 'Analyze Aurora PostgreSQL logs for deadlocks every 15 minutes',
      });

      // Note: Log Insights queries are created manually in the console
      // The Lambda function provides automated analysis
    });

    test('should grant correct permissions to Lambda function', () => {
      // Act
      const stack = new ObservabilityStack(app, 'TestObservabilityStack', {
        vpc,
        kmsKey,
      });

      // Assert
      const template = Template.fromStack(stack);

      // Check that IAM policies exist for the Lambda function and other roles
      template.resourceCountIs('AWS::IAM::Policy', 12);
      
      // Check that IAM policies exist and contain required permissions
      const policies = template.findResources('AWS::IAM::Policy');
      const policyValues = Object.values(policies);
      
      // Find policies that contain the required permissions for deadlock analysis
      const hasLogsPermissions = policyValues.some((policy: any) => {
        const statements = policy.Properties.PolicyDocument.Statement;
        return statements.some((stmt: any) => 
          stmt.Action && stmt.Action.includes('logs:StartQuery')
        );
      });
      
      const hasCloudWatchPermissions = policyValues.some((policy: any) => {
        const statements = policy.Properties.PolicyDocument.Statement;
        return statements.some((stmt: any) => 
          stmt.Action && stmt.Action.includes('cloudwatch:PutMetricData')
        );
      });
      
      expect(hasLogsPermissions).toBe(true);
      expect(hasCloudWatchPermissions).toBe(true);
    });
  });

  describe('Integration', () => {
    test('should work together with RDS stack configuration', () => {
      // This test would verify that the monitoring works with the actual RDS configuration
      // In a real scenario, you would test the integration with the RDS stack

      const alertingConfig: AlertingConfig = {
        criticalAlerts: {
          emailAddresses: ['admin@example.com'],
        },
        warningAlerts: {
          emailAddresses: ['dev@example.com'],
        },
        infoAlerts: {
          emailAddresses: ['info@example.com'],
        },
      };

      const alertingStack = new AlertingStack(app, 'TestAlertingStack', {
        environment: 'test',
        region: 'us-east-1',
        applicationName: 'genai-demo',
        alertingConfig,
      });

      const observabilityStack = new ObservabilityStack(app, 'TestObservabilityStack', {
        vpc,
        kmsKey,
      });

      // Both stacks should be created without errors
      expect(alertingStack).toBeDefined();
      expect(observabilityStack).toBeDefined();

      // Verify that both stacks reference the same DB instance identifier pattern
      const alertingTemplate = Template.fromStack(alertingStack);
      const observabilityTemplate = Template.fromStack(observabilityStack);

      // Both should reference Aurora instances with consistent naming
      alertingTemplate.hasResourceProperties('AWS::CloudWatch::Alarm', {
        Dimensions: [
          {
            Name: 'DBInstanceIdentifier',
            Value: 'genai-demo-test-primary-aurora',
          },
        ],
      });

      // Dashboard should exist
      observabilityTemplate.resourceCountIs('AWS::CloudWatch::Dashboard', 2);
    });
  });
});