import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import { Template, Match } from 'aws-cdk-lib/assertions';
import { NetworkSecurityStack } from '../src/stacks/network-security-stack';

describe('NetworkSecurityStack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let kmsKey: kms.Key;
    let stack: NetworkSecurityStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        
        // Create a test VPC
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 2,
            ipAddresses: ec2.IpAddresses.cidr('10.0.0.0/16')
        });

        // Create a test KMS key
        kmsKey = new kms.Key(vpcStack, 'TestKey', {
            description: 'Test KMS key'
        });

        // Create the NetworkSecurityStack
        stack = new NetworkSecurityStack(app, 'TestNetworkSecurityStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            region: 'ap-east-2',
            kmsKey: kmsKey
        });

        template = Template.fromStack(stack);
    });

    describe('Security Groups', () => {
        test('should create multi-layer security groups', () => {
            // Web tier security group
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for web tier (ALB/CloudFront)',
                GroupName: 'genai-demo-test-web-sg'
            });

            // Application tier security group
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for application tier (EKS pods)',
                GroupName: 'genai-demo-test-app-sg'
            });

            // Cache tier security group
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for cache tier (ElastiCache Redis)',
                GroupName: 'genai-demo-test-cache-sg'
            });

            // Database tier security group
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for database tier (Aurora PostgreSQL)',
                GroupName: 'genai-demo-test-database-sg'
            });

            // Management tier security group
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for management tier (bastion, admin tools)',
                GroupName: 'genai-demo-test-management-sg'
            });

            // Monitoring tier security group
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for monitoring tier (Prometheus, Grafana)',
                GroupName: 'genai-demo-test-monitoring-sg'
            });
        });

        test('should configure proper ingress rules for web tier', () => {
            // Check that web security group has proper ingress rules
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for web tier (ALB/CloudFront)',
                GroupName: 'genai-demo-test-web-sg',
                SecurityGroupIngress: [
                    {
                        CidrIp: '0.0.0.0/0',
                        Description: 'Allow HTTPS from internet',
                        FromPort: 443,
                        IpProtocol: 'tcp',
                        ToPort: 443
                    },
                    {
                        CidrIp: '0.0.0.0/0',
                        Description: 'Allow HTTP from internet (redirect to HTTPS)',
                        FromPort: 80,
                        IpProtocol: 'tcp',
                        ToPort: 80
                    }
                ]
            });
        });

        test('should configure proper ingress rules for application tier', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
                IpProtocol: 'tcp',
                FromPort: 8080,
                ToPort: 8080,
                Description: 'Allow traffic from web tier'
            });

            template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
                IpProtocol: 'tcp',
                FromPort: 8443,
                ToPort: 8443,
                Description: 'Allow secure traffic from web tier'
            });
        });

        test('should configure proper ingress rules for database tier', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
                IpProtocol: 'tcp',
                FromPort: 5432,
                ToPort: 5432,
                Description: 'Allow PostgreSQL access from application tier'
            });
        });

        test('should configure proper ingress rules for cache tier', () => {
            template.hasResourceProperties('AWS::EC2::SecurityGroupIngress', {
                IpProtocol: 'tcp',
                FromPort: 6379,
                ToPort: 6379,
                Description: 'Allow Redis access from application tier'
            });
        });
    });

    describe('AWS WAF', () => {
        test('should create WAF Web ACL', () => {
            template.hasResourceProperties('AWS::WAFv2::WebACL', {
                Name: 'genai-demo-test-cross-region-web-acl',
                Description: 'Web ACL for cross-region GenAI Demo application protection',
                Scope: 'REGIONAL',
                DefaultAction: {
                    Allow: {}
                }
            });
        });

        test('should include AWS managed rule sets', () => {
            template.hasResourceProperties('AWS::WAFv2::WebACL', {
                Rules: Match.arrayWith([
                    Match.objectLike({
                        Name: 'AWSManagedRulesCommonRuleSet',
                        Priority: 1,
                        Statement: {
                            ManagedRuleGroupStatement: {
                                VendorName: 'AWS',
                                Name: 'AWSManagedRulesCommonRuleSet'
                            }
                        }
                    }),
                    Match.objectLike({
                        Name: 'AWSManagedRulesKnownBadInputsRuleSet',
                        Priority: 2,
                        Statement: {
                            ManagedRuleGroupStatement: {
                                VendorName: 'AWS',
                                Name: 'AWSManagedRulesKnownBadInputsRuleSet'
                            }
                        }
                    }),
                    Match.objectLike({
                        Name: 'AWSManagedRulesSQLiRuleSet',
                        Priority: 3,
                        Statement: {
                            ManagedRuleGroupStatement: {
                                VendorName: 'AWS',
                                Name: 'AWSManagedRulesSQLiRuleSet'
                            }
                        }
                    })
                ])
            });
        });

        test('should include rate limiting rule', () => {
            template.hasResourceProperties('AWS::WAFv2::WebACL', {
                Rules: Match.arrayWith([
                    Match.objectLike({
                        Name: 'RateLimitRule',
                        Priority: 4,
                        Action: {
                            Block: {}
                        },
                        Statement: {
                            RateBasedStatement: {
                                Limit: 2000,
                                AggregateKeyType: 'IP'
                            }
                        }
                    })
                ])
            });
        });

        test('should include geographic blocking rule', () => {
            template.hasResourceProperties('AWS::WAFv2::WebACL', {
                Rules: Match.arrayWith([
                    Match.objectLike({
                        Name: 'GeoBlockRule',
                        Priority: 5,
                        Action: {
                            Block: {}
                        },
                        Statement: {
                            GeoMatchStatement: {
                                CountryCodes: ['CN', 'RU', 'KP']
                            }
                        }
                    })
                ])
            });
        });
    });

    describe('GuardDuty', () => {
        test('should enable GuardDuty detector', () => {
            template.hasResourceProperties('AWS::GuardDuty::Detector', {
                Enable: true,
                FindingPublishingFrequency: 'FIFTEEN_MINUTES',
                DataSources: {
                    S3Logs: {
                        Enable: true
                    },
                    Kubernetes: {
                        AuditLogs: {
                            Enable: true
                        }
                    },
                    MalwareProtection: {
                        ScanEc2InstanceWithFindings: {
                            EbsVolumes: true
                        }
                    }
                }
            });
        });

        test('should have proper tags', () => {
            template.hasResourceProperties('AWS::GuardDuty::Detector', {
                Tags: Match.arrayWith([
                    {
                        Key: 'Environment',
                        Value: 'test'
                    },
                    {
                        Key: 'Project',
                        Value: 'genai-demo'
                    },
                    {
                        Key: 'Stack',
                        Value: 'NetworkSecurity'
                    }
                ])
            });
        });
    });

    describe('VPC Flow Logs', () => {
        test('should create S3 bucket for flow logs', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                BucketName: 'genai-demo-test-vpc-flow-logs-ap-east-2',
                BucketEncryption: {
                    ServerSideEncryptionConfiguration: [
                        {
                            ServerSideEncryptionByDefault: {
                                SSEAlgorithm: 'aws:kms'
                            }
                        }
                    ]
                },
                PublicAccessBlockConfiguration: {
                    BlockPublicAcls: true,
                    BlockPublicPolicy: true,
                    IgnorePublicAcls: true,
                    RestrictPublicBuckets: true
                },
                VersioningConfiguration: {
                    Status: 'Enabled'
                }
            });
        });

        test('should create IAM role for VPC Flow Logs', () => {
            template.hasResourceProperties('AWS::IAM::Role', {
                RoleName: 'genai-demo-test-vpc-flow-logs-role',
                AssumeRolePolicyDocument: {
                    Statement: [
                        {
                            Effect: 'Allow',
                            Principal: {
                                Service: 'vpc-flow-logs.amazonaws.com'
                            },
                            Action: 'sts:AssumeRole'
                        }
                    ]
                }
            });
        });

        test('should create CloudWatch log group for flow logs', () => {
            template.hasResourceProperties('AWS::Logs::LogGroup', {
                LogGroupName: '/aws/vpc/flowlogs/genai-demo-test',
                RetentionInDays: 30
            });
        });

        test('should create VPC flow logs for CloudWatch', () => {
            template.hasResourceProperties('AWS::EC2::FlowLog', {
                ResourceType: 'VPC',
                TrafficType: 'ALL',
                LogDestinationType: 'cloud-watch-logs',
                MaxAggregationInterval: 60
            });
        });

        test('should create VPC flow logs for S3', () => {
            template.hasResourceProperties('AWS::EC2::FlowLog', {
                ResourceType: 'VPC',
                TrafficType: 'ALL',
                LogDestinationType: 's3',
                MaxAggregationInterval: 600
            });
        });

        test('should create subnet-level flow logs', () => {
            // Should create flow logs for private subnets
            template.hasResourceProperties('AWS::EC2::FlowLog', {
                ResourceType: 'Subnet',
                TrafficType: 'ALL',
                LogDestinationType: 'cloud-watch-logs',
                MaxAggregationInterval: 60
            });
        });
    });

    describe('Outputs', () => {
        test('should create proper stack outputs', () => {
            template.hasOutput('WebACLArn', {
                Description: 'ARN of the WAF Web ACL'
            });

            template.hasOutput('GuardDutyDetectorId', {
                Description: 'ID of the GuardDuty detector'
            });

            template.hasOutput('FlowLogsBucketName', {
                Description: 'Name of the VPC Flow Logs S3 bucket'
            });

            template.hasOutput('WebSecurityGroupId', {
                Description: 'ID of the web tier security group'
            });

            template.hasOutput('AppSecurityGroupId', {
                Description: 'ID of the application tier security group'
            });

            template.hasOutput('DatabaseSecurityGroupId', {
                Description: 'ID of the database tier security group'
            });

            template.hasOutput('CacheSecurityGroupId', {
                Description: 'ID of the cache tier security group'
            });
        });
    });

    describe('Tagging', () => {
        test('should apply proper tags to resources', () => {
            // Check that stack-level tags are applied
            const resources = template.findResources('AWS::EC2::SecurityGroup');
            Object.values(resources).forEach(resource => {
                expect(resource.Properties?.Tags).toBeDefined();
            });
        });
    });

    describe('Security Best Practices', () => {
        test('should not allow all outbound traffic by default', () => {
            // Check that security groups exist with proper configuration
            const securityGroups = template.findResources('AWS::EC2::SecurityGroup');
            
            // Should have 6 security groups
            expect(Object.keys(securityGroups)).toHaveLength(6);
            
            // Check specific security groups have proper egress configuration
            // Web tier should not have SecurityGroupEgress (uses separate egress rules)
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for web tier (ALB/CloudFront)',
                GroupName: 'genai-demo-test-web-sg'
            });
            
            // Cache and Database tiers should have explicit deny rules
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for cache tier (ElastiCache Redis)',
                SecurityGroupEgress: [
                    {
                        CidrIp: '255.255.255.255/32',
                        Description: 'Disallow all traffic',
                        FromPort: 252,
                        IpProtocol: 'icmp',
                        ToPort: 86
                    }
                ]
            });
            
            template.hasResourceProperties('AWS::EC2::SecurityGroup', {
                GroupDescription: 'Security group for database tier (Aurora PostgreSQL)',
                SecurityGroupEgress: [
                    {
                        CidrIp: '255.255.255.255/32',
                        Description: 'Disallow all traffic',
                        FromPort: 252,
                        IpProtocol: 'icmp',
                        ToPort: 86
                    }
                ]
            });
        });

        test('should use KMS encryption for S3 bucket', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                BucketEncryption: {
                    ServerSideEncryptionConfiguration: [
                        {
                            ServerSideEncryptionByDefault: {
                                SSEAlgorithm: 'aws:kms'
                            }
                        }
                    ]
                }
            });
        });

        test('should block all public access on S3 bucket', () => {
            template.hasResourceProperties('AWS::S3::Bucket', {
                PublicAccessBlockConfiguration: {
                    BlockPublicAcls: true,
                    BlockPublicPolicy: true,
                    IgnorePublicAcls: true,
                    RestrictPublicBuckets: true
                }
            });
        });
    });
});