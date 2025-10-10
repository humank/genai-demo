import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as eks from 'aws-cdk-lib/aws-eks';
import { Construct } from 'constructs';

export interface IAMStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly region?: string;
    readonly cluster?: eks.ICluster;
}

/**
 * IAM Fine-grained Access Control Stack
 * 
 * This stack implements comprehensive IAM policies and roles for:
 * 1. Resource-based IAM policies for AWS services
 * 2. IRSA (IAM Roles for Service Accounts) for EKS
 * 3. AWS SSO integration preparation
 * 
 * Requirements: 3.1 - Security Architecture Enhancement
 */
export class IAMStack extends cdk.Stack {
    public readonly applicationRole: iam.Role;
    public readonly monitoringRole: iam.Role;
    public readonly dataAccessRole: iam.Role;
    public readonly adminRole: iam.Role;

    constructor(scope: Construct, id: string, props: IAMStackProps) {
        super(scope, id, props);

        const { environment, projectName, region } = props;

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'IAM');
        cdk.Tags.of(this).add('Component', 'Security');

        // Create resource-based IAM policies
        this.createResourceBasedPolicies(projectName, environment, region);

        // Create application roles
        this.applicationRole = this.createApplicationRole(projectName, environment, region);
        this.monitoringRole = this.createMonitoringRole(projectName, environment, region);
        this.dataAccessRole = this.createDataAccessRole(projectName, environment, region);
        this.adminRole = this.createAdminRole(projectName, environment, region);

        // Create managed policies for reuse
        this.createManagedPolicies(projectName, environment, region);

        // Create outputs
        this.createOutputs(projectName, environment);
    }

    /**
     * Create resource-based IAM policies for AWS services
     */
    private createResourceBasedPolicies(projectName: string, environment: string, region?: string): void {
        // S3 Resource Policy for application buckets
        const s3ResourcePolicy = new iam.PolicyDocument({
            statements: [
                new iam.PolicyStatement({
                    sid: 'AllowEKSServiceAccountAccess',
                    effect: iam.Effect.ALLOW,
                    principals: [
                        new iam.ArnPrincipal(`arn:aws:iam::${this.account}:role/${projectName}-${environment}-app-role`),
                        new iam.ArnPrincipal(`arn:aws:iam::${this.account}:role/${projectName}-${environment}-data-role`)
                    ],
                    actions: [
                        's3:GetObject',
                        's3:PutObject',
                        's3:DeleteObject',
                        's3:ListBucket',
                        's3:GetBucketLocation'
                    ],
                    resources: [
                        `arn:aws:s3:::${projectName}-${environment}-*`,
                        `arn:aws:s3:::${projectName}-${environment}-*/*`
                    ],
                    conditions: {
                        StringEquals: {
                            's3:x-amz-server-side-encryption': 'AES256'
                        },
                        StringLike: {
                            's3:x-amz-server-side-encryption-aws-kms-key-id': `arn:aws:kms:${region || 'ap-east-2'}:${this.account}:key/*`
                        }
                    }
                })
            ]
        });

        // Aurora Database Resource Policy (via IAM Database Authentication)
        const auroraAccessPolicy = new iam.ManagedPolicy(this, 'AuroraAccessPolicy', {
            managedPolicyName: `${projectName}-${environment}-aurora-access`,
            description: 'Policy for Aurora database access via IAM authentication',
            statements: [
                new iam.PolicyStatement({
                    sid: 'AllowRDSConnect',
                    effect: iam.Effect.ALLOW,
                    actions: ['rds-db:connect'],
                    resources: [
                        `arn:aws:rds-db:${region || 'ap-east-2'}:${this.account}:dbuser:${projectName}-${environment}-cluster/app-user`,
                        `arn:aws:rds-db:${region || 'ap-east-2'}:${this.account}:dbuser:${projectName}-${environment}-cluster/readonly-user`
                    ],
                    conditions: {
                        StringEquals: {
                            'aws:RequestedRegion': region || 'ap-east-2'
                        }
                    }
                })
            ]
        });

        // MSK Kafka Resource Policy
        const mskAccessPolicy = new iam.ManagedPolicy(this, 'MSKAccessPolicy', {
            managedPolicyName: `${projectName}-${environment}-msk-access`,
            description: 'Policy for MSK Kafka cluster access',
            statements: [
                new iam.PolicyStatement({
                    sid: 'AllowMSKClusterAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka:DescribeCluster',
                        'kafka:GetBootstrapBrokers',
                        'kafka:DescribeClusterV2',
                        'kafka:ListClusters'
                    ],
                    resources: [
                        `arn:aws:kafka:${region || 'ap-east-2'}:${this.account}:cluster/${projectName}-${environment}-msk/*`
                    ]
                }),
                new iam.PolicyStatement({
                    sid: 'AllowMSKTopicAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka-cluster:Connect',
                        'kafka-cluster:AlterCluster',
                        'kafka-cluster:DescribeCluster'
                    ],
                    resources: [
                        `arn:aws:kafka:${region || 'ap-east-2'}:${this.account}:cluster/${projectName}-${environment}-msk/*`
                    ]
                }),
                new iam.PolicyStatement({
                    sid: 'AllowMSKTopicOperations',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka-cluster:CreateTopic',
                        'kafka-cluster:DescribeTopic',
                        'kafka-cluster:WriteData',
                        'kafka-cluster:ReadData'
                    ],
                    resources: [
                        `arn:aws:kafka:${region || 'ap-east-2'}:${this.account}:topic/${projectName}-${environment}-msk/*`
                    ]
                }),
                new iam.PolicyStatement({
                    sid: 'AllowMSKConsumerGroupAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kafka-cluster:AlterGroup',
                        'kafka-cluster:DescribeGroup'
                    ],
                    resources: [
                        `arn:aws:kafka:${region || 'ap-east-2'}:${this.account}:group/${projectName}-${environment}-msk/*`
                    ]
                })
            ]
        });

        // ElastiCache Redis Resource Policy
        const elastiCacheAccessPolicy = new iam.ManagedPolicy(this, 'ElastiCacheAccessPolicy', {
            managedPolicyName: `${projectName}-${environment}-elasticache-access`,
            description: 'Policy for ElastiCache Redis cluster access',
            statements: [
                new iam.PolicyStatement({
                    sid: 'AllowElastiCacheAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'elasticache:DescribeCacheClusters',
                        'elasticache:DescribeReplicationGroups',
                        'elasticache:DescribeCacheSubnetGroups',
                        'elasticache:DescribeCacheParameterGroups'
                    ],
                    resources: [
                        `arn:aws:elasticache:${region || 'ap-east-2'}:${this.account}:cluster:${projectName}-${environment}-*`,
                        `arn:aws:elasticache:${region || 'ap-east-2'}:${this.account}:replicationgroup:${projectName}-${environment}-*`
                    ]
                })
            ]
        });

        // Store policies as stack properties for reference
        cdk.Tags.of(auroraAccessPolicy).add('PolicyType', 'ResourceBased');
        cdk.Tags.of(mskAccessPolicy).add('PolicyType', 'ResourceBased');
        cdk.Tags.of(elastiCacheAccessPolicy).add('PolicyType', 'ResourceBased');
    }

    /**
     * Create application role for EKS service accounts
     */
    private createApplicationRole(projectName: string, environment: string, region?: string): iam.Role {
        const applicationRole = new iam.Role(this, 'ApplicationRole', {
            roleName: `${projectName}-${environment}-app-role`,
            description: 'IAM role for application service accounts in EKS',
            assumedBy: new iam.WebIdentityPrincipal('arn:aws:iam::' + this.account + ':oidc-provider/oidc.eks.' + (region || 'ap-east-2') + '.amazonaws.com/id/*', {
                'StringEquals': {
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:sub`]: 'system:serviceaccount:default:genai-demo-app',
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:aud`]: 'sts.amazonaws.com'
                }
            }),
            maxSessionDuration: cdk.Duration.hours(12)
        });

        // Attach managed policies
        applicationRole.addManagedPolicy(
            iam.ManagedPolicy.fromManagedPolicyName(this, 'AuroraAccessPolicyRef', `${projectName}-${environment}-aurora-access`)
        );
        applicationRole.addManagedPolicy(
            iam.ManagedPolicy.fromManagedPolicyName(this, 'MSKAccessPolicyRef', `${projectName}-${environment}-msk-access`)
        );
        applicationRole.addManagedPolicy(
            iam.ManagedPolicy.fromManagedPolicyName(this, 'ElastiCacheAccessPolicyRef', `${projectName}-${environment}-elasticache-access`)
        );

        // Add inline policies for specific application needs
        applicationRole.attachInlinePolicy(new iam.Policy(this, 'ApplicationInlinePolicy', {
            policyName: 'ApplicationSpecificAccess',
            statements: [
                // CloudWatch Metrics and Logs
                new iam.PolicyStatement({
                    sid: 'AllowCloudWatchAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'cloudwatch:PutMetricData',
                        'cloudwatch:GetMetricStatistics',
                        'cloudwatch:ListMetrics',
                        'logs:CreateLogGroup',
                        'logs:CreateLogStream',
                        'logs:PutLogEvents',
                        'logs:DescribeLogStreams',
                        'logs:DescribeLogGroups'
                    ],
                    resources: ['*'],
                    conditions: {
                        StringEquals: {
                            'aws:RequestedRegion': region || 'ap-east-2'
                        }
                    }
                }),
                // X-Ray Distributed Tracing
                new iam.PolicyStatement({
                    sid: 'AllowXRayAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'xray:PutTraceSegments',
                        'xray:PutTelemetryRecords',
                        'xray:GetSamplingRules',
                        'xray:GetSamplingTargets'
                    ],
                    resources: ['*']
                }),
                // Parameter Store for configuration
                new iam.PolicyStatement({
                    sid: 'AllowParameterStoreAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'ssm:GetParameter',
                        'ssm:GetParameters',
                        'ssm:GetParametersByPath'
                    ],
                    resources: [
                        `arn:aws:ssm:${region || 'ap-east-2'}:${this.account}:parameter/${projectName}/${environment}/*`,
                        `arn:aws:ssm:${region || 'ap-east-2'}:${this.account}:parameter/${projectName}/common/*`
                    ]
                }),
                // Secrets Manager for sensitive data
                new iam.PolicyStatement({
                    sid: 'AllowSecretsManagerAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'secretsmanager:GetSecretValue',
                        'secretsmanager:DescribeSecret'
                    ],
                    resources: [
                        `arn:aws:secretsmanager:${region || 'ap-east-2'}:${this.account}:secret:${projectName}/${environment}/*`
                    ]
                }),
                // KMS for encryption/decryption
                new iam.PolicyStatement({
                    sid: 'AllowKMSAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kms:Decrypt',
                        'kms:GenerateDataKey',
                        'kms:DescribeKey'
                    ],
                    resources: [
                        `arn:aws:kms:${region || 'ap-east-2'}:${this.account}:key/*`
                    ],
                    conditions: {
                        StringEquals: {
                            'kms:ViaService': [
                                `secretsmanager.${region || 'ap-east-2'}.amazonaws.com`,
                                `ssm.${region || 'ap-east-2'}.amazonaws.com`,
                                `s3.${region || 'ap-east-2'}.amazonaws.com`
                            ]
                        }
                    }
                })
            ]
        }));

        cdk.Tags.of(applicationRole).add('RoleType', 'Application');
        cdk.Tags.of(applicationRole).add('ServiceAccount', 'genai-demo-app');

        return applicationRole;
    }

    /**
     * Create monitoring role for observability components
     */
    private createMonitoringRole(projectName: string, environment: string, region?: string): iam.Role {
        const monitoringRole = new iam.Role(this, 'MonitoringRole', {
            roleName: `${projectName}-${environment}-monitoring-role`,
            description: 'IAM role for monitoring and observability service accounts',
            assumedBy: new iam.WebIdentityPrincipal('arn:aws:iam::' + this.account + ':oidc-provider/oidc.eks.' + (region || 'ap-east-2') + '.amazonaws.com/id/*', {
                'StringEquals': {
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:sub`]: 'system:serviceaccount:monitoring:*',
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:aud`]: 'sts.amazonaws.com'
                }
            }),
            maxSessionDuration: cdk.Duration.hours(12)
        });

        // Attach AWS managed policies for monitoring
        monitoringRole.addManagedPolicy(
            iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy')
        );

        // Add inline policy for comprehensive monitoring access
        monitoringRole.attachInlinePolicy(new iam.Policy(this, 'MonitoringInlinePolicy', {
            policyName: 'MonitoringSpecificAccess',
            statements: [
                // Enhanced CloudWatch access
                new iam.PolicyStatement({
                    sid: 'AllowEnhancedCloudWatchAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'cloudwatch:*',
                        'logs:*',
                        'ec2:DescribeVolumes',
                        'ec2:DescribeTags',
                        'ec2:DescribeInstances'
                    ],
                    resources: ['*']
                }),
                // X-Ray full access for tracing
                new iam.PolicyStatement({
                    sid: 'AllowXRayFullAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'xray:*'
                    ],
                    resources: ['*']
                }),
                // EKS cluster access for metrics collection
                new iam.PolicyStatement({
                    sid: 'AllowEKSAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'eks:DescribeCluster',
                        'eks:ListClusters',
                        'eks:DescribeNodegroup',
                        'eks:ListNodegroups'
                    ],
                    resources: [
                        `arn:aws:eks:${region || 'ap-east-2'}:${this.account}:cluster/${projectName}-${environment}-*`
                    ]
                })
            ]
        }));

        cdk.Tags.of(monitoringRole).add('RoleType', 'Monitoring');
        cdk.Tags.of(monitoringRole).add('ServiceAccount', 'monitoring-*');

        return monitoringRole;
    }

    /**
     * Create data access role for data processing components
     */
    private createDataAccessRole(projectName: string, environment: string, region?: string): iam.Role {
        const dataAccessRole = new iam.Role(this, 'DataAccessRole', {
            roleName: `${projectName}-${environment}-data-role`,
            description: 'IAM role for data processing and analytics service accounts',
            assumedBy: new iam.WebIdentityPrincipal('arn:aws:iam::' + this.account + ':oidc-provider/oidc.eks.' + (region || 'ap-east-2') + '.amazonaws.com/id/*', {
                'StringEquals': {
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:sub`]: 'system:serviceaccount:data:*',
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:aud`]: 'sts.amazonaws.com'
                }
            }),
            maxSessionDuration: cdk.Duration.hours(8)
        });

        // Add comprehensive data access policies
        dataAccessRole.attachInlinePolicy(new iam.Policy(this, 'DataAccessInlinePolicy', {
            policyName: 'DataProcessingAccess',
            statements: [
                // S3 full access for data buckets
                new iam.PolicyStatement({
                    sid: 'AllowS3DataAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        's3:GetObject',
                        's3:PutObject',
                        's3:DeleteObject',
                        's3:ListBucket',
                        's3:GetBucketLocation',
                        's3:GetObjectVersion',
                        's3:ListBucketVersions'
                    ],
                    resources: [
                        `arn:aws:s3:::${projectName}-${environment}-data-*`,
                        `arn:aws:s3:::${projectName}-${environment}-data-*/*`,
                        `arn:aws:s3:::${projectName}-${environment}-analytics-*`,
                        `arn:aws:s3:::${projectName}-${environment}-analytics-*/*`
                    ]
                }),
                // Glue Data Catalog access
                new iam.PolicyStatement({
                    sid: 'AllowGlueDataCatalogAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'glue:GetDatabase',
                        'glue:GetDatabases',
                        'glue:GetTable',
                        'glue:GetTables',
                        'glue:GetPartition',
                        'glue:GetPartitions',
                        'glue:BatchCreatePartition',
                        'glue:BatchDeletePartition',
                        'glue:BatchUpdatePartition'
                    ],
                    resources: [
                        `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:catalog`,
                        `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:database/${projectName}-${environment}-*`,
                        `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:table/${projectName}-${environment}-*/*`
                    ]
                }),
                // Athena query access
                new iam.PolicyStatement({
                    sid: 'AllowAthenaAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'athena:StartQueryExecution',
                        'athena:GetQueryExecution',
                        'athena:GetQueryResults',
                        'athena:StopQueryExecution',
                        'athena:GetWorkGroup'
                    ],
                    resources: [
                        `arn:aws:athena:${region || 'ap-east-2'}:${this.account}:workgroup/${projectName}-${environment}-*`
                    ]
                })
            ]
        }));

        cdk.Tags.of(dataAccessRole).add('RoleType', 'DataAccess');
        cdk.Tags.of(dataAccessRole).add('ServiceAccount', 'data-*');

        return dataAccessRole;
    }

    /**
     * Create admin role for administrative operations
     */
    private createAdminRole(projectName: string, environment: string, region?: string): iam.Role {
        const adminRole = new iam.Role(this, 'AdminRole', {
            roleName: `${projectName}-${environment}-admin-role`,
            description: 'IAM role for administrative operations',
            assumedBy: new iam.WebIdentityPrincipal('arn:aws:iam::' + this.account + ':oidc-provider/oidc.eks.' + (region || 'ap-east-2') + '.amazonaws.com/id/*', {
                'StringEquals': {
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:sub`]: 'system:serviceaccount:kube-system:admin-*',
                    [`oidc.eks.${region || 'ap-east-2'}.amazonaws.com/id/*:aud`]: 'sts.amazonaws.com'
                }
            }),
            maxSessionDuration: cdk.Duration.hours(4)
        });

        // Add administrative policies with restrictions
        adminRole.attachInlinePolicy(new iam.Policy(this, 'AdminInlinePolicy', {
            policyName: 'AdminAccess',
            statements: [
                // Allow most administrative actions
                new iam.PolicyStatement({
                    sid: 'AllowAdminAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'ec2:*',
                        'eks:*',
                        'rds:*',
                        'elasticache:*',
                        'kafka:*',
                        's3:*',
                        'cloudwatch:*',
                        'logs:*',
                        'xray:*',
                        'ssm:*',
                        'secretsmanager:*',
                        'kms:*'
                    ],
                    resources: ['*'],
                    conditions: {
                        StringEquals: {
                            'aws:RequestedRegion': region || 'ap-east-2'
                        }
                    }
                }),
                // Deny dangerous IAM operations
                new iam.PolicyStatement({
                    sid: 'DenyDangerousIAMOperations',
                    effect: iam.Effect.DENY,
                    actions: [
                        'iam:DeleteRole',
                        'iam:DeletePolicy',
                        'iam:DetachRolePolicy',
                        'iam:DetachUserPolicy',
                        'iam:DeleteUser',
                        'iam:CreateUser',
                        'iam:AttachUserPolicy'
                    ],
                    resources: ['*'],
                    conditions: {
                        StringNotLike: {
                            'iam:ResourceTag/Project': projectName
                        }
                    }
                })
            ]
        }));

        cdk.Tags.of(adminRole).add('RoleType', 'Admin');
        cdk.Tags.of(adminRole).add('ServiceAccount', 'admin-*');

        return adminRole;
    }

    /**
     * Create reusable managed policies
     */
    private createManagedPolicies(projectName: string, environment: string, region?: string): void {
        // Common application policy
        new iam.ManagedPolicy(this, 'CommonApplicationPolicy', {
            managedPolicyName: `${projectName}-${environment}-common-app`,
            description: 'Common policy for all application components',
            statements: [
                new iam.PolicyStatement({
                    sid: 'AllowBasicCloudWatchAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'cloudwatch:PutMetricData',
                        'logs:CreateLogStream',
                        'logs:PutLogEvents'
                    ],
                    resources: ['*']
                }),
                new iam.PolicyStatement({
                    sid: 'AllowBasicXRayAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'xray:PutTraceSegments',
                        'xray:PutTelemetryRecords'
                    ],
                    resources: ['*']
                })
            ]
        });

        // Read-only policy for monitoring and debugging
        new iam.ManagedPolicy(this, 'ReadOnlyPolicy', {
            managedPolicyName: `${projectName}-${environment}-readonly`,
            description: 'Read-only access policy for monitoring and debugging',
            statements: [
                new iam.PolicyStatement({
                    sid: 'AllowReadOnlyAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'ec2:Describe*',
                        'eks:Describe*',
                        'eks:List*',
                        'rds:Describe*',
                        'elasticache:Describe*',
                        'kafka:Describe*',
                        'kafka:List*',
                        's3:GetObject',
                        's3:ListBucket',
                        'cloudwatch:Get*',
                        'cloudwatch:List*',
                        'cloudwatch:Describe*',
                        'logs:Describe*',
                        'logs:Get*',
                        'xray:Get*',
                        'xray:BatchGet*'
                    ],
                    resources: ['*']
                })
            ]
        });
    }

    /**
     * Create stack outputs
     */
    private createOutputs(projectName: string, environment: string): void {
        new cdk.CfnOutput(this, 'ApplicationRoleArn', {
            value: this.applicationRole.roleArn,
            exportName: `${this.stackName}-ApplicationRoleArn`,
            description: 'ARN of the application IAM role'
        });

        new cdk.CfnOutput(this, 'MonitoringRoleArn', {
            value: this.monitoringRole.roleArn,
            exportName: `${this.stackName}-MonitoringRoleArn`,
            description: 'ARN of the monitoring IAM role'
        });

        new cdk.CfnOutput(this, 'DataAccessRoleArn', {
            value: this.dataAccessRole.roleArn,
            exportName: `${this.stackName}-DataAccessRoleArn`,
            description: 'ARN of the data access IAM role'
        });

        new cdk.CfnOutput(this, 'AdminRoleArn', {
            value: this.adminRole.roleArn,
            exportName: `${this.stackName}-AdminRoleArn`,
            description: 'ARN of the admin IAM role'
        });
    }
}