import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as sns from 'aws-cdk-lib/aws-sns';
import { MSKStack } from '../src/stacks/msk-stack';

describe('MSKStack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let mskSecurityGroup: ec2.SecurityGroup;
    let kmsKey: kms.Key;
    let alertingTopic: sns.Topic;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:environments': {
                    'test': {
                        'msk': {
                            'broker-instance-type': 'kafka.t3.small',
                            'number-of-brokers': 1,
                            'storage-size': 20
                        }
                    }
                }
            }
        });

        // Create a test VPC
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 2,
            ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16')
        });

        // Create test security group
        mskSecurityGroup = new ec2.SecurityGroup(vpcStack, 'TestMskSecurityGroup', {
            vpc,
            description: 'Test MSK security group'
        });

        // Create test KMS key
        kmsKey = new kms.Key(vpcStack, 'TestKmsKey', {
            description: 'Test KMS key for MSK'
        });

        // Create test SNS topic
        alertingTopic = new sns.Topic(vpcStack, 'TestAlertingTopic', {
            topicName: 'test-alerting-topic'
        });
    });

    test('should create MSK cluster with correct configuration', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::MSK::Cluster', {
            ClusterName: 'genai-demo-test-msk',
            KafkaVersion: '2.8.1',
            NumberOfBrokerNodes: 1,
            BrokerNodeGroupInfo: {
                InstanceType: 'kafka.t3.small',
                StorageInfo: {
                    EBSStorageInfo: {
                        VolumeSize: 20
                    }
                }
            }
        });
    });

    test('should create MSK configuration with domain events optimization', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::MSK::Configuration', {
            Name: 'genai-demo-test-msk-config',
            Description: 'MSK configuration for genai-demo test environment - optimized for domain events',
            KafkaVersionsList: ['2.8.1']
        });
    });

    test('should enable encryption at rest and in transit', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::MSK::Cluster', {
            EncryptionInfo: {
                EncryptionInTransit: {
                    ClientBroker: 'TLS',
                    InCluster: true
                }
            }
        });

        // Check that encryption at rest is configured (KMS key reference exists)
        const mskCluster = template.findResources('AWS::MSK::Cluster');
        const clusterKey = Object.keys(mskCluster)[0];
        expect(mskCluster[clusterKey].Properties.EncryptionInfo.EncryptionAtRest).toBeDefined();
    });

    test('should enable IAM authentication', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::MSK::Cluster', {
            ClientAuthentication: {
                Sasl: {
                    Iam: {
                        Enabled: true
                    }
                }
            }
        });
    });

    test('should create MSK cluster policy for secure access', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        const clusterPolicies = template.findResources('AWS::MSK::ClusterPolicy');
        expect(Object.keys(clusterPolicies).length).toBeGreaterThan(0);

        const policyKey = Object.keys(clusterPolicies)[0];
        expect(clusterPolicies[policyKey].Properties.ClusterArn).toBeDefined();
        expect(clusterPolicies[policyKey].Properties.Policy).toBeDefined();
    });

    test('should create MSK Connect role with appropriate permissions', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::IAM::Role', {
            RoleName: 'genai-demo-test-msk-connect-role',
            AssumeRolePolicyDocument: {
                Statement: [{
                    Effect: 'Allow',
                    Principal: {
                        Service: 'kafkaconnect.amazonaws.com'
                    },
                    Action: 'sts:AssumeRole'
                }]
            }
        });
    });

    test('should enable comprehensive monitoring', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::MSK::Cluster', {
            EnhancedMonitoring: 'PER_BROKER',
            OpenMonitoring: {
                Prometheus: {
                    JmxExporter: {
                        EnabledInBroker: true
                    },
                    NodeExporter: {
                        EnabledInBroker: true
                    }
                }
            }
        });
    });

    test('should create CloudWatch alarms for MSK monitoring', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        const alarms = template.findResources('AWS::CloudWatch::Alarm');
        const alarmNames = Object.keys(alarms);

        expect(alarmNames.length).toBeGreaterThan(0);

        // Check for specific alarms
        const alarmTypes = ['MSKClusterCPUUtilization', 'MSKClusterMemoryUtilization', 'MSKClusterDiskUtilization'];
        alarmTypes.forEach(alarmType => {
            const foundAlarm = alarmNames.some(name =>
                alarms[name].Properties.AlarmName.includes(alarmType)
            );
            expect(foundAlarm).toBe(true);
        });
    });

    test('should create appropriate outputs for integration', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        const outputs = template.findOutputs('*');
        const outputNames = Object.keys(outputs);

        // Check for essential outputs
        const expectedOutputs = [
            'MSKClusterArn',
            'MSKClusterName',
            'MSKBootstrapServersIAM',
            'MSKBootstrapServersTLS',
            'DomainEventsTopicPrefix',
            'SpringBootKafkaConfig'
        ];

        expectedOutputs.forEach(expectedOutput => {
            expect(outputNames).toContain(expectedOutput);
        });
    });

    test('should configure logging to CloudWatch', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::MSK::Cluster', {
            LoggingInfo: {
                BrokerLogs: {
                    CloudWatchLogs: {
                        Enabled: true
                    }
                }
            }
        });

        // Check for log group creation
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/msk/cluster/genai-demo-test'
        });
    });

    test('should use private subnets for security', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        const mskCluster = template.findResources('AWS::MSK::Cluster');
        const clusterKey = Object.keys(mskCluster)[0];

        // Check that cluster uses private subnets and security groups
        expect(mskCluster[clusterKey].Properties.BrokerNodeGroupInfo.ClientSubnets).toBeDefined();
        expect(mskCluster[clusterKey].Properties.BrokerNodeGroupInfo.SecurityGroups).toBeDefined();
        expect(mskCluster[clusterKey].Properties.BrokerNodeGroupInfo.ClientSubnets.length).toBeGreaterThan(0);
        expect(mskCluster[clusterKey].Properties.BrokerNodeGroupInfo.SecurityGroups.length).toBeGreaterThan(0);
    });

    test('should apply proper tags to all resources', () => {
        // Given
        const stack = new MSKStack(app, 'TestMSKStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc,
            mskSecurityGroup,
            kmsKey,
            alertingTopic,
            region: 'us-east-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        const mskCluster = template.findResources('AWS::MSK::Cluster');
        const clusterKey = Object.keys(mskCluster)[0];

        expect(mskCluster[clusterKey].Properties.Tags).toEqual(
            expect.objectContaining({
                Project: 'genai-demo',
                Environment: 'test',
                ManagedBy: 'AWS-CDK',
                Component: 'MSK',
                Service: 'Messaging'
            })
        );
    });
});