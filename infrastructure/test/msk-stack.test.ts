import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { MSKStack } from '../src/stacks/msk-stack';

describe('MSKStack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let kmsKey: kms.Key;

    beforeEach(() => {
        app = new cdk.App();
        
        // Create test VPC
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 3,
            natGateways: 1,
        });

        // Create test KMS key
        const kmsStack = new cdk.Stack(app, 'TestKmsStack');
        kmsKey = new kms.Key(kmsStack, 'TestKey', {
            description: 'Test KMS key for MSK',
        });
    });

    test('MSK Stack creates successfully', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        // Verify MSK cluster is created
        template.hasResourceProperties('AWS::MSK::Cluster', {
            ClusterName: 'TestMSKStack-msk-cluster',
            KafkaVersion: '2.8.1',
            NumberOfBrokerNodes: 3,
        });
    });

    test('MSK Cluster has correct broker configuration', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::MSK::Cluster', {
            BrokerNodeGroupInfo: {
                InstanceType: 'kafka.m5.xlarge',
                StorageInfo: {
                    EBSStorageInfo: {
                        VolumeSize: 1000,
                    },
                },
            },
        });
    });

    test('MSK Cluster has encryption enabled', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::MSK::Cluster', {
            EncryptionInfo: {
                EncryptionAtRest: {
                    DataVolumeKMSKeyId: Match.anyValue()
                },
                EncryptionInTransit: {
                    ClientBroker: 'TLS',
                    InCluster: true,
                },
            },
        });
    });

    test('MSK Cluster has authentication configured', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::MSK::Cluster', {
            ClientAuthentication: {
                Sasl: {
                    Scram: {
                        Enabled: true,
                    },
                    Iam: {
                        Enabled: true,
                    },
                },
            },
        });
    });

    test('MSK Cluster has monitoring enabled', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::MSK::Cluster', {
            EnhancedMonitoring: 'PER_TOPIC_PER_PARTITION',
            OpenMonitoring: {
                Prometheus: {
                    JmxExporter: {
                        EnabledInBroker: true,
                    },
                    NodeExporter: {
                        EnabledInBroker: true,
                    },
                },
            },
        });
    });

    test('MSK Configuration is created with correct properties', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::MSK::Configuration', {
            Name: 'TestMSKStack-msk-config',
            KafkaVersionsList: ['2.8.1'],
        });
    });

    test('Security Group has correct ingress rules', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        // Check for MSK ports
        template.hasResourceProperties('AWS::EC2::SecurityGroup', {
            SecurityGroupIngress: Match.arrayWith([
                Match.objectLike({
                    IpProtocol: 'tcp',
                    FromPort: 9092,
                    ToPort: 9092,
                    CidrIp: Match.anyValue(),
                }),
                Match.objectLike({
                    IpProtocol: 'tcp',
                    FromPort: 9094,
                    ToPort: 9094,
                    CidrIp: Match.anyValue(),
                }),
                Match.objectLike({
                    IpProtocol: 'tcp',
                    FromPort: 9096,
                    ToPort: 9096,
                    CidrIp: Match.anyValue(),
                }),
                Match.objectLike({
                    IpProtocol: 'tcp',
                    FromPort: 2181,
                    ToPort: 2181,
                    CidrIp: Match.anyValue(),
                }),
            ]),
        });
    });

    test('IAM roles and policies are created', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        // Check MSK Service Role
        template.hasResourceProperties('AWS::IAM::Role', {
            AssumeRolePolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Principal: {
                            Service: 'kafka.amazonaws.com',
                        },
                        Action: 'sts:AssumeRole',
                    },
                ],
            },
        });

        // Check MSK Cluster Policy
        template.hasResourceProperties('AWS::IAM::ManagedPolicy', {
            PolicyDocument: {
                Statement: Match.arrayWith([
                    Match.objectLike({
                        Effect: 'Allow',
                        Action: Match.arrayWith([
                            'kafka-cluster:Connect',
                            'kafka-cluster:AlterCluster',
                            'kafka-cluster:DescribeCluster',
                        ]),
                    }),
                ]),
            },
        });
    });

    test('CloudWatch Log Group is created', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/msk/genai-demo-test',
            RetentionInDays: 30,
        });
    });

    test('Stack outputs are created', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        // Check for required outputs
        template.hasOutput('MSKClusterArn', {});
        template.hasOutput('MSKClusterName', {});
        template.hasOutput('MSKBootstrapServers', {});
        template.hasOutput('MSKBootstrapServersTls', {});
        template.hasOutput('MSKZookeeperConnectString', {});
        template.hasOutput('MSKSecurityGroupId', {});
        template.hasOutput('MSKClusterPolicyArn', {});
    });

    test('Stack has correct tags', () => {
        // WHEN
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            kmsKey,
            region: 'ap-east-2',
            isPrimaryRegion: true,
        });

        // THEN
        const template = Template.fromStack(stack);
        
        template.hasResourceProperties('AWS::MSK::Cluster', {
            Tags: {
                Environment: 'test',
                Purpose: 'DataFlowTracking',
                Region: 'ap-east-2',
                RegionType: 'Primary',
            },
        });
    });
});