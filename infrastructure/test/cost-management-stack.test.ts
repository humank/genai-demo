import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { CostManagementStack } from '../lib/stacks/cost-management-stack';

describe('CostManagementStack', () => {
  let app: cdk.App;
  let stack: CostManagementStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    stack = new CostManagementStack(app, 'TestCostManagementStack', {
      alertEmail: 'test@example.com',
      taiwanBudgetLimit: 5000,
      japanBudgetLimit: 3000,
      env: {
        account: '123456789012',
        region: 'us-east-1',
      },
    });
    template = Template.fromStack(stack);
  });

  describe('SNS Topic', () => {
    test('should create cost alert topic', () => {
      template.hasResourceProperties('AWS::SNS::Topic', {
        DisplayName: 'GenAIDemo Cost Alerts',
        TopicName: 'genai-demo-cost-alerts',
      });
    });

    test('should have email subscription', () => {
      template.hasResourceProperties('AWS::SNS::Subscription', {
        Protocol: 'email',
        Endpoint: 'test@example.com',
      });
    });
  });

  describe('Cost Anomaly Detection', () => {
    test('should create anomaly monitor', () => {
      template.hasResourceProperties('AWS::CE::AnomalyMonitor', {
        MonitorName: 'GenAIDemo-MultiRegion-Monitor',
        MonitorType: 'DIMENSIONAL',
        MonitorDimension: 'SERVICE',
      });
    });

    test('should create anomaly subscription', () => {
      template.hasResourceProperties('AWS::CE::AnomalySubscription', {
        SubscriptionName: 'GenAIDemo-Anomaly-Alerts',
        Threshold: 100,
        Frequency: 'DAILY',
      });
    });
  });

  describe('Regional Budgets', () => {
    test('should create Taiwan region budget', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        Budget: {
          BudgetName: 'Taiwan-Primary-Region-Budget',
          BudgetType: 'COST',
          TimeUnit: 'MONTHLY',
          BudgetLimit: {
            Amount: 5000,
            Unit: 'USD',
          },
        },
      });
    });

    test('should create Japan DR budget', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        Budget: {
          BudgetName: 'Japan-DR-Region-Budget',
          BudgetType: 'COST',
          TimeUnit: 'MONTHLY',
          BudgetLimit: {
            Amount: 3000,
            Unit: 'USD',
          },
        },
      });
    });

    test('should configure budget notifications', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        NotificationsWithSubscribers: Match.arrayWith([
          Match.objectLike({
            Notification: {
              NotificationType: 'ACTUAL',
              ComparisonOperator: 'GREATER_THAN',
              Threshold: 80,
              ThresholdType: 'PERCENTAGE',
            },
          }),
        ]),
      });
    });
  });

  describe('Service-Level Budgets', () => {
    test('should create EKS service budget', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        Budget: {
          BudgetName: 'EKS-Service-Budget',
          BudgetLimit: {
            Amount: 2000,
            Unit: 'USD',
          },
        },
      });
    });

    test('should create RDS service budget', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        Budget: {
          BudgetName: 'RDS-Service-Budget',
          BudgetLimit: {
            Amount: 1500,
            Unit: 'USD',
          },
        },
      });
    });

    test('should create ElastiCache service budget', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        Budget: {
          BudgetName: 'ElastiCache-Service-Budget',
          BudgetLimit: {
            Amount: 800,
            Unit: 'USD',
          },
        },
      });
    });

    test('should create MSK service budget', () => {
      template.hasResourceProperties('AWS::Budgets::Budget', {
        Budget: {
          BudgetName: 'MSK-Service-Budget',
          BudgetLimit: {
            Amount: 500,
            Unit: 'USD',
          },
        },
      });
    });
  });

  describe('Cost Explorer Dashboard', () => {
    test('should create cost explorer dashboard', () => {
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'GenAIDemo-Cost-Explorer-Trends',
      });
    });
  });

  describe('Trusted Advisor Automation', () => {
    test('should create Trusted Advisor Lambda function', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'genai-demo-trusted-advisor-automation',
        Runtime: 'python3.11',
        Handler: 'index.handler',
        Timeout: 300,
      });
    });

    test('should have SNS topic ARN in environment', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Environment: {
          Variables: {
            SNS_TOPIC_ARN: Match.anyValue(),
          },
        },
      });
    });

    test('should grant Support API permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'support:DescribeTrustedAdvisorChecks',
                'support:DescribeTrustedAdvisorCheckResult',
                'support:RefreshTrustedAdvisorCheck',
              ]),
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });

    test('should create weekly schedule', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        Name: 'genai-demo-trusted-advisor-weekly',
        ScheduleExpression: 'cron(0 9 ? * MON *)',
      });
    });
  });

  describe('Stack Outputs', () => {
    test('should export cost alert topic ARN', () => {
      template.hasOutput('CostAlertTopicArn', {
        Export: {
          Name: 'GenAIDemo-CostAlertTopicArn',
        },
      });
    });
  });

  describe('Resource Tags', () => {
    test('should have project tags', () => {
      const resources = template.findResources('AWS::SNS::Topic');
      const topicResource = Object.values(resources)[0];
      expect(topicResource.Properties?.Tags).toBeDefined();
    });
  });
});
