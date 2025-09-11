import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { ObservabilityStack } from '../lib/stacks/observability-stack';

describe('ObservabilityStack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let stack: ObservabilityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:environments:development': {
                    'retention-policies': {
                        'logs': 7,
                        'metrics': 7,
                        'backups': 7,
                        'snapshots': 3
                    }
                },
                'genai-demo:observability': {
                    'log-retention-days': 7,
                    'metrics-retention-days': 30,
                    'trace-sampling-rate': 0.1
                }
            }
        });

        // Create a VPC for testing
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 2,
            cidr: '10.0.0.0/16'
        });

        stack = new ObservabilityStack(app, 'TestObservabilityStack', {
            environment: 'development',
            projectName: 'genai-demo',
            vpc: vpc,
            region: 'ap-northeast-1'
        });

        template = Template.fromStack(stack);
    });

    describe('SNS Topics', () => {
        test('should create three SNS topics for different alert levels', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-development-critical-alerts',
                DisplayName: 'Critical Alerts for genai-demo development'
            });

            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-development-warning-alerts',
                DisplayName: 'Warning Alerts for genai-demo development'
            });

            template.hasResourceProperties('AWS::SNS::Topic', {
                TopicName: 'genai-demo-development-info-alerts',
                DisplayName: 'Info Alerts for genai-demo development'
            });
        });

        test('should encrypt SNS topics with KMS', () => {
            template.hasResourceProperties('AWS::SNS::Topic', {
                KmsMasterKeyId: Match.anyValue()
            });
        });
    });

    describe('S3 Buckets', () => {
        test('should create log archive bucket with proper configuration', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                BucketName: Match.stringLikeRegexp('genai-demo-development-log-archive-.*'),
                BucketEncryption: {
                    ServerSideEncryptionConfiguration: [
                        {
                            ServerSideEncryptionByDefault: {
                                SSEAlgorithm: 'aws:kms',
                                KMSMasterKeyID: Match.anyValue()
                            }
                        }
                    ]
                },
                VersioningConfiguration: {
                    Status: 'Enabled'
                },
                PublicAccessBlockConfiguration: {
                    BlockPublicAcls: true,
                    BlockPublicPolicy: true,
                    IgnorePublicAcls: true,
                    RestrictPublicBuckets: true
                }
            });
        });

        test('should create data lake bucket with proper configuration', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                BucketName: Match.stringLikeRegexp('genai-demo-development-data-lake-.*'),
                BucketEncryption: {
                    ServerSideEncryptionConfiguration: [
                        {
                            ServerSideEncryptionByDefault: {
                                SSEAlgorithm: 'aws:kms',
                                KMSMasterKeyID: Match.anyValue()
                            }
                        }
                    ]
                },
                VersioningConfiguration: {
                    Status: 'Enabled'
                }
            });
        });

        test('should configure lifecycle rules for cost optimization', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                LifecycleConfiguration: {
                    Rules: [
                        {
                            Id: Match.anyValue(),
                            Status: 'Enabled',
                            Transitions: Match.arrayWith([
                                {
                                    StorageClass: 'STANDARD_IA',
                                    TransitionInDays: 30
                                },
                                {
                                    StorageClass: 'GLACIER',
                                    TransitionInDays: Match.anyValue()
                                }
                            ])
                        }
                    ]
                }
            });
        });
    });

    describe('CloudWatch Log Groups', () => {
        test('should create application log groups', () => {
            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/application',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/application-error',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/application-audit',
                RetentionInDays: 7
            });
        });

        test('should create infrastructure log groups', () => {
            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/infrastructure/eks-cluster',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/infrastructure/alb-access',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/infrastructure/rds-error',
                RetentionInDays: 7
            });
        });

        test('should create observability log groups', () => {
            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/observability/xray-traces',
                RetentionInDays: 7
            });

            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/genai-demo/development/observability/cloudwatch-insights',
                RetentionInDays: 7
            });
        });

        test('should encrypt log groups with KMS', () => {
            template.hasResourceProperties('AWS::Logs::LogGroup', {
                KmsKeyId: Match.anyValue()
            });
        });
    });

    describe('KMS Key', () => {
        test('should create KMS key for observability encryption', () => {
            template.hasResourceProperties('AWS::KMS::Key', {
                Description: 'KMS key for genai-demo development observability encryption',
                EnableKeyRotation: true,
                KeyRotationStatus: true
            });
        });

        test('should create KMS alias for observability key', () => {
            template.hasResourceProperties('AWS::KMS::Alias', {
                AliasName: 'alias/genai-demo-development-observability-key'
            });
        });

        test('should have proper key policy for observability services', () => {
            template.hasResourceProperties('AWS::KMS::Key', {
                KeyPolicy: {
                    Statement: Match.arrayWith([
                        {
                            Sid: 'Enable IAM User Permissions',
                            Effect: 'Allow',
                            Principal: {
                                AWS: Match.anyValue()
                            },
                            Action: 'kms:*',
                            Resource: '*'
                        },
                        {
                            Sid: 'Allow CloudWatch Logs to encrypt logs',
                            Effect: 'Allow',
                            Principal: {
                                Service: Match.stringLikeRegexp('logs\\..*\\.amazonaws\\.com')
                            },
                            Action: Match.arrayWith([
                                'kms:Encrypt',
                                'kms:Decrypt',
                                'kms:GenerateDataKey*'
                            ])
                        },
                        {
                            Sid: 'Allow X-Ray to encrypt traces',
                            Effect: 'Allow',
                            Principal: {
                                Service: 'xray.amazonaws.com'
                            },
                            Action: Match.arrayWith([
                                'kms:Encrypt',
                                'kms:Decrypt',
                                'kms:GenerateDataKey*'
                            ])
                        }
                    ])
                }
            });
        });
    });

    describe('IAM Role', () => {
        test('should create observability IAM role', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'genai-demo-development-observability-role',
                AssumeRolePolicyDocument: {
                    Statement: Match.arrayWith([
                        {
                            Effect: 'Allow',
                            Principal: {
                                Service: Match.arrayWith([
                                    'logs.amazonaws.com',
                                    'events.amazonaws.com',
                                    'firehose.amazonaws.com',
                                    'lambda.amazonaws.com'
                                ])
                            },
                            Action: 'sts:AssumeRole'
                        }
                    ])
                }
            });
        });

        test('should have proper managed policies attached', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                ManagedPolicyArns: Match.arrayWith([
                    'arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy',
                    'arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess'
                ])
            });
        });

        test('should have inline policy for observability permissions', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                Policies: [
                    {
                        PolicyName: 'ObservabilityPolicy',
                        PolicyDocument: {
                            Statement: Match.arrayWith([
                                {
                                    Effect: 'Allow',
                                    Action: Match.arrayWith([
                                        'logs:CreateLogGroup',
                                        'logs:CreateLogStream',
                                        'logs:PutLogEvents'
                                    ]),
                                    Resource: Match.stringLikeRegexp('arn:aws:logs:.*:.*:log-group:/aws/genai-demo/.*')
                                },
                                {
                                    Effect: 'Allow',
                                    Action: Match.arrayWith([
                                        's3:PutObject',
                                        's3:GetObject'
                                    ]),
                                    Resource: Match.anyValue()
                                },
                                {
                                    Effect: 'Allow',
                                    Action: Match.arrayWith([
                                        'events:PutEvents',
                                        'sns:Publish',
                                        'cloudwatch:PutMetricData'
                                    ]),
                                    Resource: Match.anyValue()
                                }
                            ])
                        }
                    }
                ]
            });
        });
    });

    describe('X-Ray Configuration', () => {
        test('should store X-Ray tracing configuration in Parameter Store', () => {
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/development/observability/xray-tracing',
                Type: 'String',
                Description: 'X-Ray tracing configuration for observability'
            });
        });

        test('should enable X-Ray tracing', () => {
            expect(stack.xrayTracingEnabled).toBe(true);
        });
    });

    describe('EventBridge Rules', () => {
        test('should create infrastructure health event rule', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'genai-demo-development-infrastructure-health',
                Description: 'Route infrastructure health events to appropriate notification channels',
                EventPattern: {
                    source: ['aws.ec2', 'aws.rds', 'aws.eks', 'aws.kafka'],
                    'detail-type': [
                        'EC2 Instance State-change Notification',
                        'RDS DB Instance Event',
                        'EKS Cluster State Change',
                        'MSK Cluster State Change'
                    ]
                }
            });
        });

        test('should create security events rule', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'genai-demo-development-security-events',
                Description: 'Route security events to critical alerting channel',
                EventPattern: {
                    source: ['aws.guardduty', 'aws.securityhub', 'aws.config'],
                    'detail-type': [
                        'GuardDuty Finding',
                        'Security Hub Findings - Imported',
                        'Config Rules Compliance Change'
                    ]
                }
            });
        });

        test('should create CloudWatch alarm state changes rule', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                Name: 'genai-demo-development-alarm-state-changes',
                Description: 'Route CloudWatch alarm state changes to appropriate channels',
                EventPattern: {
                    source: ['aws.cloudwatch'],
                    'detail-type': ['CloudWatch Alarm State Change'],
                    detail: {
                        state: {
                            value: ['ALARM', 'OK', 'INSUFFICIENT_DATA']
                        }
                    }
                }
            });
        });

        test('should have SNS targets for event rules', () => {
            template.hasResourceProperties('AWS::Events::Rule', {
                Targets: [
                    {
                        Arn: Match.anyValue(),
                        Id: Match.anyValue()
                    }
                ]
            });
        });
    });

    describe('CloudWatch Alarms', () => {
        test('should create high CPU utilization alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-development-high-cpu-utilization',
                AlarmDescription: 'Alarm when CPU utilization exceeds 80%',
                MetricName: 'CPUUtilization',
                Namespace: 'AWS/EC2',
                Statistic: 'Average',
                Threshold: 80,
                ComparisonOperator: 'GreaterThanThreshold',
                EvaluationPeriods: 2
            });
        });

        test('should create high memory utilization alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-development-high-memory-utilization',
                AlarmDescription: 'Alarm when memory utilization exceeds 85%',
                MetricName: 'mem_used_percent',
                Namespace: 'CWAgent',
                Threshold: 85
            });
        });

        test('should create low disk space alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-development-low-disk-space',
                AlarmDescription: 'Alarm when disk space is below 20%',
                MetricName: 'disk_used_percent',
                Namespace: 'CWAgent',
                Threshold: 80
            });
        });

        test('should create high error rate alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-development-high-error-rate',
                AlarmDescription: 'Alarm when error rate exceeds 5%',
                Threshold: 5
            });
        });

        test('should create high response time alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-development-high-response-time',
                AlarmDescription: 'Alarm when response time exceeds 2 seconds',
                MetricName: 'TargetResponseTime',
                Namespace: 'AWS/ApplicationELB',
                Threshold: 2
            });
        });

        test('should create database connection failures alarm', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmName: 'genai-demo-development-database-connection-failures',
                AlarmDescription: 'Alarm when database connection failures occur',
                MetricName: 'DatabaseConnections',
                Namespace: 'AWS/RDS',
                ComparisonOperator: 'LessThanThreshold'
            });
        });

        test('should have SNS actions for alarms', () => {
            template.hasResourceProperties('AWS::CloudWatch::Alarm', {
                AlarmActions: [Match.anyValue()],
                OKActions: [Match.anyValue()]
            });
        });
    });

    describe('Parameter Store Configuration', () => {
        test('should store log groups configuration', () => {
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/development/observability/log-groups',
                Type: 'String',
                Description: 'CloudWatch Log Groups configuration for observability'
            });
        });

        test('should store SNS topics configuration', () => {
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/development/observability/sns-topics',
                Type: 'String',
                Description: 'SNS topics configuration for alerting'
            });
        });

        test('should store S3 buckets configuration', () => {
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/development/observability/s3-buckets',
                Type: 'String',
                Description: 'S3 buckets configuration for log archival and data lake'
            });
        });

        test('should store X-Ray configuration', () => {
            template.hasResourceProperties('AWS::SSM::Parameter', {
                Name: '/genai-demo/development/observability/xray-config',
                Type: 'String',
                Description: 'X-Ray tracing configuration'
            });
        });
    });

    describe('Stack Outputs', () => {
        test('should create outputs for SNS topic ARNs', () => {
            template.hasOutput('AlertingTopicArn', {
                Description: 'ARN of the critical alerting SNS topic',
                Export: {
                    Name: 'genai-demo-development-alerting-topic-arn'
                }
            });

            template.hasOutput('WarningTopicArn', {
                Description: 'ARN of the warning alerting SNS topic',
                Export: {
                    Name: 'genai-demo-development-warning-topic-arn'
                }
            });

            template.hasOutput('InfoTopicArn', {
                Description: 'ARN of the info alerting SNS topic',
                Export: {
                    Name: 'genai-demo-development-info-topic-arn'
                }
            });
        });

        test('should create outputs for S3 bucket names', () => {
            template.hasOutput('LogArchiveBucketName', {
                Description: 'Name of the log archive S3 bucket',
                Export: {
                    Name: 'genai-demo-development-log-archive-bucket-name'
                }
            });

            template.hasOutput('DataLakeBucketName', {
                Description: 'Name of the data lake S3 bucket',
                Export: {
                    Name: 'genai-demo-development-data-lake-bucket-name'
                }
            });
        });

        test('should create output for observability role ARN', () => {
            template.hasOutput('ObservabilityRoleArn', {
                Description: 'ARN of the observability services IAM role',
                Export: {
                    Name: 'genai-demo-development-observability-role-arn'
                }
            });
        });

        test('should create observability stack summary output', () => {
            template.hasOutput('ObservabilityStackSummary', {
                Description: 'Summary of observability infrastructure components',
                Export: {
                    Name: 'genai-demo-development-observability-summary'
                }
            });
        });
    });

    describe('Resource Tagging', () => {
        test('should tag all resources with common tags', () => {
            // Check that resources have proper tags
            template.hasResourceProperties('AWS::SNS::Topic', {
                Tags: Match.arrayWith([
                    {
                        Key: 'Project',
                        Value: 'genai-demo'
                    },
                    {
                        Key: 'Environment',
                        Value: 'development'
                    },
                    {
                        Key: 'ManagedBy',
                        Value: 'AWS-CDK'
                    },
                    {
                        Key: 'Component',
                        Value: 'Observability'
                    }
                ])
            });
        });

        test('should tag resources with specific purpose tags', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                Tags: Match.arrayWith([
                    {
                        Key: 'Purpose',
                        Value: Match.anyValue()
                    }
                ])
            });
        });
    });

    describe('Cross-Region Replication', () => {
        test('should configure cross-region replication when enabled', () => {
            const stackWithReplication = new ObservabilityStack(app, 'TestObservabilityStackWithReplication', {
                environment: 'production',
                projectName: 'genai-demo',
                vpc: vpc,
                region: 'ap-northeast-1'
            });

            const replicationTemplate = Template.fromStack(stackWithReplication);

            replicationTemplate.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'genai-demo-production-cross-region-log-replication',
                AssumeRolePolicyDocument: {
                    Statement: [
                        {
                            Effect: 'Allow',
                            Principal: {
                                Service: 'firehose.amazonaws.com'
                            },
                            Action: 'sts:AssumeRole'
                        }
                    ]
                }
            });
        });
    });

    describe('Environment-Specific Configuration', () => {
        test('should use production retention policies for production environment', () => {
            const prodStack = new ObservabilityStack(app, 'TestObservabilityStackProd', {
                environment: 'production',
                projectName: 'genai-demo',
                vpc: vpc,
                region: 'ap-northeast-1'
            });

            const prodTemplate = Template.fromStack(prodStack);

            // Production should have RETAIN removal policy
            prodTemplate.hasResourceProperties('AWS::S3::Bucket', {
                DeletionPolicy: 'Retain'
            });

            prodTemplate.hasResourceProperties('AWS::Logs::LogGroup', {
                DeletionPolicy: 'Retain'
            });
        });

        test('should use development retention policies for development environment', () => {
            // Development should have DESTROY removal policy (default behavior)
            template.hasResourceProperties('AWS::S3::Bucket', {
                DeletionPolicy: Match.absent()
            });
        });
    });
});