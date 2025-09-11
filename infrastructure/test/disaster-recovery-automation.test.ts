import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as sns from 'aws-cdk-lib/aws-sns';
import { DisasterRecoveryAutomation } from '../lib/constructs/disaster-recovery-automation';

// Helper function to create health checks with proper typing
function createHealthCheck(scope: cdk.Stack, id: string, fqdn: string): route53.CfnHealthCheck {
    return new route53.CfnHealthCheck(scope, id, {
        type: 'HTTPS',
        resourcePath: '/health',
        fullyQualifiedDomainName: fqdn
    } as any);
}

describe('DisasterRecoveryAutomation', () => {
    let app: cdk.App;
    let stack: cdk.Stack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        stack = new cdk.Stack(app, 'TestStack', {
            env: {
                account: '123456789012',
                region: 'ap-northeast-1'
            }
        });
    });

    describe('Basic Construction', () => {
        test('should create DR automation construct with minimal configuration', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            const drAutomation = new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'test',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            expect(drAutomation).toBeDefined();
            expect(drAutomation.failoverStateMachine).toBeDefined();
            expect(drAutomation.chaosTestingStateMachine).toBeDefined();
            expect(drAutomation.monthlyTestingRule).toBeDefined();
            expect(drAutomation.drMonitoringDashboard).toBeDefined();
            expect(drAutomation.failoverLambda).toBeDefined();
            expect(drAutomation.chaosTestingLambda).toBeDefined();
        });

        test('should create DR automation construct with full configuration', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');
            const auroraCluster = rds.DatabaseCluster.fromDatabaseClusterAttributes(stack, 'AuroraCluster', {
                clusterIdentifier: 'test-cluster'
            });
            const hostedZone = route53.HostedZone.fromHostedZoneAttributes(stack, 'HostedZone', {
                hostedZoneId: 'Z123456789',
                zoneName: 'example.com'
            });
            const primaryHealthCheck = createHealthCheck(stack, 'PrimaryHealthCheck', 'api.example.com');
            const secondaryHealthCheck = createHealthCheck(stack, 'SecondaryHealthCheck', 'api-dr.example.com');

            // When
            const drAutomation = new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                auroraCluster,
                hostedZone,
                primaryHealthCheck,
                secondaryHealthCheck,
                alertingTopic
            });

            // Then
            expect(drAutomation).toBeDefined();
            template = Template.fromStack(stack);

            // Verify Lambda functions are created
            template.hasResourceProperties('AWS::Lambda::Function', {
                FunctionName: 'test-project-production-dr-failover'
            });

            template.hasResourceProperties('AWS::Lambda::Function', {
                FunctionName: 'test-project-production-dr-chaos-testing'
            });
        });
    });

    describe('Lambda Functions', () => {
        test('should create failover Lambda with correct configuration', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::Lambda::Function', {
                FunctionName: 'test-project-production-dr-failover',
                Runtime: 'python3.11',
                Handler: 'index.handler',
                Timeout: 900, // 15 minutes
                MemorySize: 512,
                Environment: {
                    Variables: {
                        PROJECT_NAME: 'test-project',
                        ENVIRONMENT: 'production',
                        PRIMARY_REGION: 'ap-east-2',
                        SECONDARY_REGION: 'ap-northeast-1',
                        LOG_LEVEL: 'INFO'
                    }
                }
            });
        });

        test('should create chaos testing Lambda with correct configuration', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::Lambda::Function', {
                FunctionName: 'test-project-production-dr-chaos-testing',
                Runtime: 'python3.11',
                Handler: 'index.handler',
                Timeout: 1800, // 30 minutes
                MemorySize: 1024,
                Environment: {
                    Variables: {
                        PROJECT_NAME: 'test-project',
                        ENVIRONMENT: 'production',
                        PRIMARY_REGION: 'ap-east-2',
                        SECONDARY_REGION: 'ap-northeast-1',
                        LOG_LEVEL: 'INFO'
                    }
                }
            });
        });
    });

    describe('Step Functions State Machines', () => {
        test('should create failover state machine', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::StepFunctions::StateMachine', {
                StateMachineName: 'test-project-production-dr-failover',
                Comment: 'Automated disaster recovery failover workflow'
            });
        });

        test('should create chaos testing state machine', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::StepFunctions::StateMachine', {
                StateMachineName: 'test-project-production-dr-chaos-testing',
                Comment: 'Automated chaos engineering and DR testing workflow'
            });
        });
    });

    describe('EventBridge Rules', () => {
        test('should create monthly testing rule', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'test-project-production-monthly-dr-test',
                Description: 'Trigger monthly DR testing on the first Sunday of each month',
                ScheduleExpression: 'cron(0 2 ? * SUN#1 *)',
                State: 'ENABLED'
            });
        });
    });

    describe('CloudWatch Dashboard', () => {
        test('should create DR monitoring dashboard', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
                DashboardName: 'test-project-production-dr-automation-monitoring'
            });
        });
    });

    describe('IAM Roles and Policies', () => {
        test('should create DR automation role with correct permissions', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'test-project-production-dr-automation-role',
                Description: 'IAM role for disaster recovery automation operations'
            });
        });
    });

    describe('Systems Manager Parameters', () => {
        test('should create DR automation configuration parameter', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/test-project/production/dr/automation-config',
                Description: 'Enhanced DR automation configuration for test-project production',
                Tier: 'Standard'
            });
        });
    });

    describe('Health Check Monitoring', () => {
        test('should create health check monitoring when health checks are provided', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');
            const primaryHealthCheck = createHealthCheck(stack, 'PrimaryHealthCheck', 'api.example.com');
            const secondaryHealthCheck = createHealthCheck(stack, 'SecondaryHealthCheck', 'api-dr.example.com');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                primaryHealthCheck,
                secondaryHealthCheck,
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);

            // Should create alarm for primary health check failure
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'test-project-production-primary-health-check-failure-automated',
                AlarmDescription: 'Primary region health check failure - triggers automated failover',
                ComparisonOperator: 'LessThanThreshold',
                Threshold: 1,
                EvaluationPeriods: 3,
                TreatMissingData: 'breaching'
            });

            // Should create EventBridge rule for automated failover trigger
            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'test-project-production-automated-failover-trigger',
                Description: 'Trigger automated failover when primary health check fails'
            });
        });
    });

    describe('Outputs', () => {
        test('should create all required outputs', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');

            // When
            new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                alertingTopic
            });

            // Then
            template = Template.fromStack(stack);

            // Check for outputs
            template.hasOutput('FailoverStateMachineArn', {});
            template.hasOutput('ChaosTestingStateMachineArn', {});
            template.hasOutput('MonthlyTestingRuleArn', {});
            template.hasOutput('DRMonitoringDashboardUrl', {});
            template.hasOutput('FailoverLambdaArn', {});
            template.hasOutput('ChaosTestingLambdaArn', {});
        });
    });

    describe('Integration Tests', () => {
        test('should integrate properly with existing infrastructure', () => {
            // Given
            const alertingTopic = new sns.Topic(stack, 'AlertingTopic');
            const auroraCluster = rds.DatabaseCluster.fromDatabaseClusterAttributes(stack, 'AuroraCluster', {
                clusterIdentifier: 'test-cluster'
            });
            const hostedZone = route53.HostedZone.fromHostedZoneAttributes(stack, 'HostedZone', {
                hostedZoneId: 'Z123456789',
                zoneName: 'example.com'
            });

            // When
            const drAutomation = new DisasterRecoveryAutomation(stack, 'DRAutomation', {
                projectName: 'test-project',
                environment: 'production',
                primaryRegion: 'ap-east-2',
                secondaryRegion: 'ap-northeast-1',
                auroraCluster,
                hostedZone,
                alertingTopic
            });

            // Then
            expect(drAutomation.failoverStateMachine).toBeDefined();
            expect(drAutomation.chaosTestingStateMachine).toBeDefined();
            expect(drAutomation.drMonitoringDashboard).toBeDefined();

            template = Template.fromStack(stack);

            // Should create all necessary resources
            template.resourceCountIs('AWS::Lambda::Function', 2);
            template.resourceCountIs('AWS::StepFunctions::StateMachine', 2);
            template.resourceCountIs('AWS::Events::Rule', 1);
            template.resourceCountIs('AWS::CloudWatch::Dashboard', 1);
            template.resourceCountIs('AWS::IAM::Role', 1);
            template.resourceCountIs('AWS::SSM::Parameter', 1);
        });
    });
});