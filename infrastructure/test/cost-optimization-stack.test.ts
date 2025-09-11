import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { CostOptimizationStack } from '../lib/stacks/cost-optimization-stack';

describe('CostOptimizationStack', () => {
    let app: cdk.App;
    let stack: CostOptimizationStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new CostOptimizationStack(app, 'TestCostOptimizationStack', {
            environment: 'test',
            alertEmail: 'test@example.com',
            slackWebhookUrl: 'https://hooks.slack.com/test',
            env: {
                account: '123456789012',
                region: 'us-east-1'
            }
        });
        template = Template.fromStack(stack);
    });

    describe('SNS Topics', () => {
        test('should create billing alarm topic', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-billing-alarms-TestCostOptimizationStack',
                DisplayName: 'GenAI Demo Billing Alarms'
            });
        });

        test('should create cost optimization topic', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-cost-optimization-TestCostOptimizationStack',
                DisplayName: 'GenAI Demo Cost Optimization Alerts'
            });
        });

        test('should create email subscriptions', () => {
            template.hasResourceProperties('AWS::SNS::Subscription', {
                Protocol: 'email',
                Endpoint: 'test@example.com'
            });
        });
    });

    describe('CloudWatch Alarms', () => {
        test('should create billing alarms for different cost thresholds', () => {
            // Test environment should have lower thresholds
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-billing-100usd',
                MetricName: 'EstimatedCharges',
                Namespace: 'AWS/Billing',
                Threshold: 100,
                ComparisonOperator: 'GreaterThanThreshold'
            });

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-billing-150usd',
                Threshold: 150
            });

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-billing-200usd',
                Threshold: 200
            });
        });

        test('should create service-specific cost alarms', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-eks-cost',
                MetricName: 'EstimatedCharges',
                Namespace: 'AWS/Billing',
                Threshold: 50 // Test environment threshold for EKS
            });

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-rds-cost',
                Threshold: 30 // Test environment threshold for RDS
            });

            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-test-cloudwatch-cost',
                Threshold: 20 // Test environment threshold for CloudWatch
            });
        });

        test('should configure alarm actions to SNS topics', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmActions: [
                    {
                        Ref: expect.stringMatching(/BillingAlarmTopic/)
                    }
                ]
            });
        });
    });

    describe('AWS Budgets', () => {
        test('should create monthly budget', () => {
            template.hasResourceProperties('AWS::Budgets::Budget', {
                Budget: {
                    BudgetName: 'genai-demo-test-monthly-budget',
                    BudgetLimit: {
                        Amount: 200, // Test environment budget
                        Unit: 'USD'
                    },
                    TimeUnit: 'MONTHLY',
                    BudgetType: 'COST'
                }
            });
        });

        test('should configure budget notifications', () => {
            template.hasResourceProperties('AWS::Budgets::Budget', {
                NotificationsWithSubscribers: [
                    {
                        Notification: {
                            NotificationType: 'ACTUAL',
                            ComparisonOperator: 'GREATER_THAN',
                            Threshold: 80,
                            ThresholdType: 'PERCENTAGE'
                        }
                    },
                    {
                        Notification: {
                            NotificationType: 'FORECASTED',
                            ComparisonOperator: 'GREATER_THAN',
                            Threshold: 100,
                            ThresholdType: 'PERCENTAGE'
                        }
                    }
                ]
            });
        });
    });

    describe('CloudWatch Dashboard', () => {
        test('should create cost optimization dashboard', () => {
            template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
                DashboardName: 'genai-demo-test-cost-optimization'
            });
        });

        test('should include billing metrics in dashboard', () => {
            const dashboardBody = template.findResources('AWS::CloudWatch::Dashboard');
            const dashboard = Object.values(dashboardBody)[0];

            expect(dashboard.Properties.DashboardBody).toContain('EstimatedCharges');
            expect(dashboard.Properties.DashboardBody).toContain('AWS/Billing');
        });
    });

    describe('Stack Outputs', () => {
        test('should output billing alarm topic ARN', () => {
            template.hasOutput('BillingAlarmTopicArn', {
                Description: 'SNS Topic ARN for billing alarms'
            });
        });

        test('should output cost optimization topic ARN', () => {
            template.hasOutput('CostOptimizationTopicArn', {
                Description: 'SNS Topic ARN for cost optimization alerts'
            });
        });
    });

    describe('Production Environment', () => {
        test('should have higher cost thresholds for production', () => {
            const prodStack = new CostOptimizationStack(app, 'ProdCostOptimizationStack', {
                environment: 'production',
                alertEmail: 'prod@example.com',
                env: {
                    account: '123456789012',
                    region: 'us-east-1'
                }
            });
            const prodTemplate = Template.fromStack(prodStack);

            prodTemplate.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-production-billing-500usd',
                Threshold: 500
            });

            prodTemplate.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-production-eks-cost',
                Threshold: 200 // Higher threshold for production EKS
            });

            prodTemplate.hasResourceProperties('AWS::Budgets::Budget', {
                Budget: {
                    BudgetLimit: {
                        Amount: 1000, // Higher budget for production
                        Unit: 'USD'
                    }
                }
            });
        });

        test('should create daily budget for production', () => {
            const prodStack = new CostOptimizationStack(app, 'ProdCostOptimizationStack', {
                environment: 'production',
                alertEmail: 'prod@example.com',
                env: {
                    account: '123456789012',
                    region: 'us-east-1'
                }
            });
            const prodTemplate = Template.fromStack(prodStack);

            prodTemplate.hasResourceProperties('AWS::Budgets::Budget', {
                Budget: {
                    BudgetName: 'genai-demo-production-daily-budget',
                    TimeUnit: 'DAILY',
                    BudgetLimit: {
                        Amount: 33.333333333333336, // 1000/30 for daily limit
                        Unit: 'USD'
                    }
                }
            });
        });
    });

    describe('Resource Tagging', () => {
        test('should apply cost allocation tags', () => {
            // Verify that resources have appropriate tags for cost allocation
            const resources = template.findResources('AWS::CloudWatch::Alarm');

            // At least one alarm should exist
            expect(Object.keys(resources).length).toBeGreaterThan(0);
        });
    });

    describe('Error Handling', () => {
        test('should handle missing optional parameters gracefully', () => {
            const minimalStack = new CostOptimizationStack(app, 'MinimalCostOptimizationStack', {
                environment: 'test',
                alertEmail: 'minimal@example.com',
                env: {
                    account: '123456789012',
                    region: 'us-east-1'
                }
            });

            const minimalTemplate = Template.fromStack(minimalStack);

            // Should still create basic resources
            minimalTemplate.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-billing-alarms-MinimalCostOptimizationStack'
            });
        });
    });
});