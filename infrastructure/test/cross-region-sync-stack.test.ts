import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import { CrossRegionSyncStack } from '../src/stacks/cross-region-sync-stack';

describe('CrossRegionSyncStack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let kmsKey: kms.Key;

    beforeEach(() => {
        app = new cdk.App();
        
        // Create VPC for testing
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 3,
            natGateways: 1,
        });

        // Create KMS key for testing
        kmsKey = new kms.Key(vpcStack, 'TestKmsKey', {
            description: 'Test KMS key for cross-region sync',
        });
    });

    test('should create cross-region sync stack with required resources', () => {
        // Given
        const props = {
            environment: 'test',
            projectName: 'genai-demo',
            kmsKey: kmsKey,
            region: 'ap-southeast-1',
            isPrimaryRegion: true,
            targetRegions: ['ap-northeast-1', 'us-west-2'],
        };

        // When
        const stack = new CrossRegionSyncStack(app, 'TestCrossRegionSyncStack', props);
        const template = Template.fromStack(stack);

        // Then - Verify EventBridge resources
        template.hasResourceProperties('AWS::Events::EventBus', {
            Name: 'genai-demo-test-cross-region-events',
        });

        // Then - Verify Lambda functions
        template.hasResourceProperties('AWS::Lambda::Function', {
            FunctionName: 'TestCrossRegionSyncStack-event-filter',
            Runtime: 'python3.11',
            Handler: 'index.handler',
        });

        template.hasResourceProperties('AWS::Lambda::Function', {
            FunctionName: 'TestCrossRegionSyncStack-event-replication',
            Runtime: 'python3.11',
            Handler: 'index.handler',
        });

        // Then - Verify SQS queues
        template.hasResourceProperties('AWS::SQS::Queue', {
            QueueName: 'TestCrossRegionSyncStack-cross-region-sync-dlq.fifo',
        });

        template.hasResourceProperties('AWS::SQS::Queue', {
            QueueName: 'TestCrossRegionSyncStack-event-ordering-queue.fifo',
            FifoQueue: true,
            ContentBasedDeduplication: true,
        });

        // Then - Verify SNS topic
        template.hasResourceProperties('AWS::SNS::Topic', {
            TopicName: 'genai-demo-test-cross-region-sync-alerts',
        });

        // Then - Verify IAM role
        template.hasResourceProperties('AWS::IAM::Role', {
            AssumeRolePolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Principal: {
                            Service: 'lambda.amazonaws.com',
                        },
                        Action: 'sts:AssumeRole',
                    },
                ],
            },
        });
    });

    test('should create EventBridge rules for cross-region replication', () => {
        // Given
        const props = {
            environment: 'test',
            projectName: 'genai-demo',
            kmsKey: kmsKey,
            region: 'ap-southeast-1',
            isPrimaryRegion: true,
            targetRegions: ['ap-northeast-1'],
        };

        // When
        const stack = new CrossRegionSyncStack(app, 'TestCrossRegionSyncStack', props);
        const template = Template.fromStack(stack);

        // Then - Verify EventBridge rule for filtering
        template.hasResourceProperties('AWS::Events::Rule', {
            Name: 'TestCrossRegionSyncStack-event-filter-rule',
            EventPattern: {
                source: [
                    'genai-demo.customer',
                    'genai-demo.order',
                    'genai-demo.payment',
                    'genai-demo.inventory',
                ],
                'detail-type': [
                    'EntityCreated',
                    'EntityUpdated',
                    'EntityDeleted',
                    'StateChanged',
                ],
            },
        });
    });

    test('should configure cross-region IAM permissions correctly', () => {
        // Given
        const props = {
            environment: 'test',
            projectName: 'genai-demo',
            kmsKey: kmsKey,
            region: 'ap-southeast-1',
            isPrimaryRegion: true,
            targetRegions: ['ap-northeast-1', 'us-west-2'],
        };

        // When
        const stack = new CrossRegionSyncStack(app, 'TestCrossRegionSyncStack', props);
        const template = Template.fromStack(stack);

        // Then - Verify IAM policy for cross-region EventBridge access
        template.hasResourceProperties('AWS::IAM::Policy', {
            PolicyDocument: Match.objectLike({
                Statement: Match.arrayWith([
                    Match.objectLike({
                        Effect: 'Allow',
                        Action: Match.arrayWith([
                            'events:PutEvents',
                        ]),
                    }),
                ]),
            }),
        });
    });

    test('should create monitoring and metrics resources', () => {
        // Given
        const props = {
            environment: 'test',
            projectName: 'genai-demo',
            kmsKey: kmsKey,
            region: 'ap-southeast-1',
            isPrimaryRegion: true,
            targetRegions: ['ap-northeast-1'],
        };

        // When
        const stack = new CrossRegionSyncStack(app, 'TestCrossRegionSyncStack', props);
        const template = Template.fromStack(stack);

        // Then - Verify metrics collection Lambda
        template.hasResourceProperties('AWS::Lambda::Function', {
            FunctionName: 'TestCrossRegionSyncStack-sync-metrics',
            Runtime: 'python3.11',
        });

        // Then - Verify CloudWatch Events rule for metrics collection
        template.hasResourceProperties('AWS::Events::Rule', {
            ScheduleExpression: 'rate(5 minutes)',
        });
    });

    test('should create proper outputs for integration', () => {
        // Given
        const props = {
            environment: 'test',
            projectName: 'genai-demo',
            kmsKey: kmsKey,
            region: 'ap-southeast-1',
            isPrimaryRegion: true,
            targetRegions: ['ap-northeast-1'],
        };

        // When
        const stack = new CrossRegionSyncStack(app, 'TestCrossRegionSyncStack', props);
        const template = Template.fromStack(stack);

        // Then - Verify outputs
        template.hasOutput('EventBusArn', {});
        template.hasOutput('EventBusName', {});
        template.hasOutput('EventReplicationFunctionArn', {});
        template.hasOutput('EventFilterFunctionArn', {});
        template.hasOutput('CrossRegionRoleArn', {});
        template.hasOutput('AlertTopicArn', {});
        template.hasOutput('DeadLetterQueueUrl', {});
        template.hasOutput('EventOrderingQueueUrl', {});
    });
});