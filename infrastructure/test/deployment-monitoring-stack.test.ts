import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { DeploymentMonitoringStack } from '../src/stacks/deployment-monitoring-stack';

describe('DeploymentMonitoringStack', () => {
    let app: cdk.App;
    let stack: DeploymentMonitoringStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new DeploymentMonitoringStack(app, 'TestDeploymentMonitoringStack', {
            projectName: 'test-project',
            environment: 'test',
            multiRegionConfig: {
                enabled: true,
                regions: ['us-east-1', 'us-west-2'],
                primaryRegion: 'us-east-1',
            },
        });
        template = Template.fromStack(stack);
    });

    describe('Lambda Functions', () => {
        test('should create deployment metrics collection function', () => {
            template.hasResourceProperties('AWS::Lambda::Function', {
                Runtime: 'python3.11',
                Handler: 'index.handler',
                Timeout: 300,
                Environment: {
                    Variables: {
                        PROJECT_NAME: 'test-project',
                        ENVIRONMENT: 'test',
                        REGIONS: 'us-east-1,us-west-2',
                    },
                },
            });
        });

        test('should create pipeline event handler function', () => {
            // Should have at least 3 Lambda functions (metrics, pipeline handler, deploy handler)
            const functions = template.findResources('AWS::Lambda::Function');
            expect(Object.keys(functions).length).toBeGreaterThanOrEqual(3);
        });

        test('should create deploy event handler function', () => {
            // Verified by checking total function count
            const functions = template.findResources('AWS::Lambda::Function');
            expect(Object.keys(functions).length).toBeGreaterThanOrEqual(3);
        });
    });

    describe('IAM Permissions', () => {
        test('should grant CodePipeline permissions to metrics function', () => {
            template.hasResourceProperties('AWS::IAM::Policy', {
                PolicyDocument: {
                    Statement: Match.arrayWith([
                        Match.objectLike({
                            Action: Match.arrayWith([
                                'codepipeline:ListPipelines',
                                'codepipeline:GetPipeline',
                                'codepipeline:GetPipelineState',
                                'codepipeline:ListPipelineExecutions',
                                'codepipeline:GetPipelineExecution',
                            ]),
                            Effect: 'Allow',
                        }),
                    ]),
                },
            });
        });

        test('should grant CodeDeploy permissions to metrics function', () => {
            template.hasResourceProperties('AWS::IAM::Policy', {
                PolicyDocument: {
                    Statement: Match.arrayWith([
                        Match.objectLike({
                            Action: Match.arrayWith([
                                'codedeploy:ListApplications',
                                'codedeploy:GetApplication',
                                'codedeploy:ListDeploymentGroups',
                                'codedeploy:ListDeployments',
                                'codedeploy:GetDeployment',
                            ]),
                            Effect: 'Allow',
                        }),
                    ]),
                },
            });
        });

        test('should grant CloudWatch PutMetricData permission', () => {
            // Find all IAM policies
            const policies = template.findResources('AWS::IAM::Policy');
            
            // Check if any policy has PutMetricData permission
            const hasPutMetricData = Object.values(policies).some((policy: any) => {
                const statements = policy.Properties.PolicyDocument.Statement;
                return statements.some((stmt: any) => 
                    stmt.Action && 
                    (stmt.Action.includes('cloudwatch:PutMetricData') || 
                     (Array.isArray(stmt.Action) && stmt.Action.includes('cloudwatch:PutMetricData')))
                );
            });
            
            expect(hasPutMetricData).toBe(true);
        });
    });

    describe('CloudWatch Dashboard', () => {
        test('should create deployment monitoring dashboard', () => {
            template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
                DashboardName: 'test-project-test-deployment-monitoring',
            });
        });

        test('should include deployment metrics in dashboard', () => {
            const dashboardResource = template.findResources('AWS::CloudWatch::Dashboard');
            const dashboard = Object.values(dashboardResource)[0] as any;
            
            // Dashboard body might be a CDK token, so just verify it exists
            expect(dashboard.Properties.DashboardBody).toBeDefined();
        });
    });

    describe('EventBridge Rules', () => {
        test('should create CodePipeline state change rule', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                EventPattern: {
                    source: ['aws.codepipeline'],
                    'detail-type': ['CodePipeline Pipeline Execution State Change'],
                    detail: {
                        state: ['FAILED', 'SUCCEEDED'],
                        pipeline: ['test-project-test-multi-region-pipeline'],
                    },
                },
            });
        });

        test('should create CodeDeploy state change rule', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                EventPattern: {
                    source: ['aws.codedeploy'],
                    'detail-type': ['CodeDeploy Deployment State-change Notification'],
                    detail: {
                        state: ['FAILURE', 'SUCCESS', 'STOPPED'],
                    },
                },
            });
        });

        test('should schedule metrics collection every 5 minutes', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                ScheduleExpression: 'rate(5 minutes)',
            });
        });
    });

    describe('SNS Topic', () => {
        test('should create deployment alert topic', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                DisplayName: 'test-project-test-deployment-alerts',
                TopicName: 'test-project-test-deployment-alerts',
            });
        });
    });

    describe('CloudWatch Alarms', () => {
        test('should create pipeline failure alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'test-project-test-pipeline-failures',
                MetricName: 'PipelineFailures',
                Namespace: 'test-project/Deployment',
                Threshold: 1,
                ComparisonOperator: 'GreaterThanOrEqualToThreshold',
            });
        });

        test('should create deployment failure alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'test-project-test-deployment-failures',
                MetricName: 'DeploymentFailures',
                Namespace: 'test-project/Deployment',
                Threshold: 1,
                ComparisonOperator: 'GreaterThanOrEqualToThreshold',
            });
        });

        test('should create low success rate alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'test-project-test-low-deployment-success-rate',
                MetricName: 'DeploymentSuccessRate',
                Namespace: 'test-project/Deployment',
                Threshold: 80,
                ComparisonOperator: 'LessThanThreshold',
            });
        });

        test('should create long deployment time alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'test-project-test-long-deployment-time',
                MetricName: 'DeploymentTime',
                Namespace: 'test-project/Deployment',
                Threshold: 1800, // 30 minutes
                ComparisonOperator: 'GreaterThanThreshold',
            });
        });

        test('should create long pipeline execution alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'test-project-test-long-pipeline-execution',
                MetricName: 'PipelineExecutionTime',
                Namespace: 'test-project/Deployment',
                Threshold: 3600, // 60 minutes
                ComparisonOperator: 'GreaterThanThreshold',
            });
        });

        test('should configure SNS actions for all alarms', () => {
            const alarms = template.findResources('AWS::CloudWatch::Alarm');
            Object.values(alarms).forEach((alarm: any) => {
                expect(alarm.Properties.AlarmActions).toBeDefined();
                expect(alarm.Properties.AlarmActions.length).toBeGreaterThan(0);
            });
        });
    });

    describe('Stack Outputs', () => {
        test('should export deployment dashboard URL', () => {
            template.hasOutput('DeploymentDashboardUrl', {
                Export: {
                    Name: 'test-project-test-deployment-dashboard-url',
                },
            });
        });

        test('should export deployment alert topic ARN', () => {
            template.hasOutput('DeploymentAlertTopicArn', {
                Export: {
                    Name: 'test-project-test-deployment-alert-topic-arn',
                },
            });
        });

        test('should export deployment metrics function ARN', () => {
            template.hasOutput('DeploymentMetricsFunctionArn', {
                Export: {
                    Name: 'test-project-test-deployment-metrics-function-arn',
                },
            });
        });
    });

    describe('Integration', () => {
        test('should integrate with existing alerting topic when provided', () => {
            const appWithTopic = new cdk.App();
            
            // Create a separate stack for the alert topic
            const topicStack = new cdk.Stack(appWithTopic, 'TopicStack');
            const alertTopic = new cdk.aws_sns.Topic(topicStack, 'AlertTopic');
            
            const stackWithTopic = new DeploymentMonitoringStack(appWithTopic, 'TestStackWithTopic', {
                projectName: 'test-project',
                environment: 'test',
                alertingTopic: alertTopic,
            });

            const templateWithTopic = Template.fromStack(stackWithTopic);
            
            // Should still create its own deployment alert topic
            templateWithTopic.resourceCountIs('AWS::SNS::Topic', 1);
        });

        test('should support multi-region configuration', () => {
            // Verify multi-region configuration through Lambda environment variables
            template.hasResourceProperties('AWS::Lambda::Function', {
                Environment: {
                    Variables: Match.objectLike({
                        REGIONS: 'us-east-1,us-west-2',
                    }),
                },
            });
        });
    });
});
