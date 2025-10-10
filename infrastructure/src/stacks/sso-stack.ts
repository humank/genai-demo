import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as sso from 'aws-cdk-lib/aws-sso';
import * as cloudtrail from 'aws-cdk-lib/aws-cloudtrail';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface SSOStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly region?: string;
    readonly ssoInstanceArn?: string;
    readonly primaryRegion?: string;
    readonly secondaryRegions?: string[];
    readonly crossRegionEnabled?: boolean;
    readonly securityMonitoringTopic?: sns.ITopic;
}

/**
 * AWS SSO Identity Management Integration Stack with Multi-Region Support
 * 
 * This stack implements unified identity authentication system with:
 * 1. Cross-region SSO integration and permission sets
 * 2. Unified RBAC strategy across all regions
 * 3. Cross-region audit log collection via CloudTrail
 * 4. Security event correlation and analysis
 * 
 * Requirements: 4.3.1 - Multi-Region Security and Identity Management
 * 
 * Note: Actual user and group assignments need to be done via AWS CLI or Console
 * as they require real SSO user/group IDs which are not available at CDK deployment time.
 */
export class SSOStack extends cdk.Stack {
    public readonly developerPermissionSet: sso.CfnPermissionSet;
    public readonly adminPermissionSet: sso.CfnPermissionSet;
    public readonly readOnlyPermissionSet: sso.CfnPermissionSet;
    public readonly dataAnalystPermissionSet: sso.CfnPermissionSet;
    public readonly crossRegionAdminPermissionSet: sso.CfnPermissionSet;
    public readonly crossRegionCloudTrail: cloudtrail.Trail;
    public readonly crossRegionAuditBucket: s3.Bucket;
    public readonly securityEventCorrelationFunction: lambda.Function;

    constructor(scope: Construct, id: string, props: SSOStackProps) {
        super(scope, id, props);

        const { 
            environment, 
            projectName, 
            region, 
            ssoInstanceArn, 
            primaryRegion, 
            secondaryRegions, 
            crossRegionEnabled,
            securityMonitoringTopic 
        } = props;

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'SSO');
        cdk.Tags.of(this).add('Component', 'Identity');
        cdk.Tags.of(this).add('MultiRegion', crossRegionEnabled ? 'true' : 'false');

        // Validate SSO instance ARN
        const instanceArn = ssoInstanceArn || this.node.tryGetContext('ssoInstanceArn');
        if (!instanceArn) {
            throw new Error('SSO Instance ARN must be provided via props or CDK context');
        }

        // Create cross-region audit infrastructure first
        if (crossRegionEnabled) {
            this.crossRegionAuditBucket = this.createCrossRegionAuditBucket(projectName, environment, primaryRegion, secondaryRegions);
            this.crossRegionCloudTrail = this.createCrossRegionCloudTrail(projectName, environment, this.crossRegionAuditBucket);
        }

        // Create permission sets with cross-region support
        this.developerPermissionSet = this.createDeveloperPermissionSet(instanceArn, projectName, environment, region, crossRegionEnabled);
        this.adminPermissionSet = this.createAdminPermissionSet(instanceArn, projectName, environment, region, crossRegionEnabled);
        this.readOnlyPermissionSet = this.createReadOnlyPermissionSet(instanceArn, projectName, environment, region, crossRegionEnabled);
        this.dataAnalystPermissionSet = this.createDataAnalystPermissionSet(instanceArn, projectName, environment, region, crossRegionEnabled);

        // Create cross-region admin permission set for multi-region operations
        if (crossRegionEnabled) {
            this.crossRegionAdminPermissionSet = this.createCrossRegionAdminPermissionSet(instanceArn, projectName, environment, primaryRegion, secondaryRegions);
        }

        // Create cross-account roles for multi-region access with enhanced RBAC
        this.createCrossAccountRoles(projectName, environment, region, crossRegionEnabled, primaryRegion, secondaryRegions);

        // Create security event correlation function
        if (crossRegionEnabled && securityMonitoringTopic) {
            this.securityEventCorrelationFunction = this.createSecurityEventCorrelationFunction(
                projectName, 
                environment, 
                securityMonitoringTopic,
                primaryRegion,
                secondaryRegions
            );
        }

        // Configure cross-region security monitoring integration
        if (crossRegionEnabled) {
            this.configureCrossRegionSecurityMonitoring(projectName, environment, primaryRegion, secondaryRegions);
        }

        // Create outputs
        this.createOutputs(projectName, environment, crossRegionEnabled);
    }

    /**
     * Create Developer Permission Set with Cross-Region Support
     * Provides development access with restrictions on production resources across multiple regions
     */
    private createDeveloperPermissionSet(instanceArn: string, projectName: string, environment: string, region?: string, crossRegionEnabled?: boolean): sso.CfnPermissionSet {
        const developerPermissionSet = new sso.CfnPermissionSet(this, 'DeveloperPermissionSet', {
            instanceArn: instanceArn,
            name: `${projectName}-${environment}-Developer`,
            description: `Developer access for ${projectName} ${environment} environment`,
            sessionDuration: 'PT8H', // 8 hours
            managedPolicies: [
                'arn:aws:iam::aws:policy/PowerUserAccess'
            ],
            inlinePolicy: {
                Version: '2012-10-17',
                Statement: [
                    // Allow IAM operations for development resources
                    {
                        Sid: 'AllowIAMForDevelopment',
                        Effect: 'Allow',
                        Action: [
                            'iam:PassRole',
                            'iam:CreateRole',
                            'iam:AttachRolePolicy',
                            'iam:DetachRolePolicy',
                            'iam:PutRolePolicy',
                            'iam:DeleteRolePolicy',
                            'iam:GetRole',
                            'iam:GetRolePolicy',
                            'iam:ListRolePolicies',
                            'iam:ListAttachedRolePolicies',
                            'iam:TagRole',
                            'iam:UntagRole'
                        ],
                        Resource: [
                            `arn:aws:iam::${this.account}:role/${projectName}-${environment}-*`,
                            `arn:aws:iam::${this.account}:role/cdk-*`
                        ]
                    },
                    // Deny dangerous operations on production resources
                    {
                        Sid: 'DenyProductionResourceModification',
                        Effect: 'Deny',
                        Action: [
                            'iam:DeleteRole',
                            'iam:DetachRolePolicy',
                            'rds:DeleteDBCluster',
                            'rds:DeleteDBInstance',
                            'elasticache:DeleteReplicationGroup',
                            'kafka:DeleteCluster',
                            's3:DeleteBucket'
                        ],
                        Resource: '*',
                        Condition: {
                            StringEquals: {
                                'aws:ResourceTag/Environment': 'production'
                            }
                        }
                    },
                    // Allow EKS access for development
                    {
                        Sid: 'AllowEKSAccess',
                        Effect: 'Allow',
                        Action: [
                            'eks:*'
                        ],
                        Resource: [
                            `arn:aws:eks:${region || 'ap-east-2'}:${this.account}:cluster/${projectName}-${environment}-*`
                        ]
                    },
                    // Allow CloudFormation for CDK deployments
                    {
                        Sid: 'AllowCloudFormationAccess',
                        Effect: 'Allow',
                        Action: [
                            'cloudformation:*'
                        ],
                        Resource: [
                            `arn:aws:cloudformation:${region || 'ap-east-2'}:${this.account}:stack/${projectName}-${environment}-*/*`,
                            `arn:aws:cloudformation:${region || 'ap-east-2'}:${this.account}:stack/CDKToolkit/*`
                        ]
                    },
                    // Allow SSM Parameter Store access
                    {
                        Sid: 'AllowParameterStoreAccess',
                        Effect: 'Allow',
                        Action: [
                            'ssm:GetParameter',
                            'ssm:GetParameters',
                            'ssm:GetParametersByPath',
                            'ssm:PutParameter',
                            'ssm:DeleteParameter'
                        ],
                        Resource: [
                            `arn:aws:ssm:${region || 'ap-east-2'}:${this.account}:parameter/${projectName}/${environment}/*`,
                            `arn:aws:ssm:${region || 'ap-east-2'}:${this.account}:parameter/cdk-bootstrap/*`,
                            ...(crossRegionEnabled ? [
                                `arn:aws:ssm:*:${this.account}:parameter/${projectName}/${environment}/*`,
                                `arn:aws:ssm:*:${this.account}:parameter/cdk-bootstrap/*`
                            ] : [])
                        ]
                    },
                    // Cross-region access for developers (if enabled)
                    ...(crossRegionEnabled ? [{
                        Sid: 'AllowCrossRegionDeveloperAccess',
                        Effect: 'Allow',
                        Action: [
                            'ec2:DescribeRegions',
                            'ec2:DescribeAvailabilityZones',
                            'sts:AssumeRole',
                            'sts:GetCallerIdentity'
                        ],
                        Resource: '*'
                    }, {
                        Sid: 'AllowCrossRegionResourceAccess',
                        Effect: 'Allow',
                        Action: [
                            'eks:DescribeCluster',
                            'eks:ListClusters',
                            'rds:DescribeDBClusters',
                            'rds:DescribeDBInstances',
                            'cloudformation:DescribeStacks',
                            'cloudformation:ListStacks'
                        ],
                        Resource: [
                            `arn:aws:eks:*:${this.account}:cluster/${projectName}-${environment}-*`,
                            `arn:aws:rds:*:${this.account}:cluster:${projectName}-${environment}-*`,
                            `arn:aws:rds:*:${this.account}:db:${projectName}-${environment}-*`,
                            `arn:aws:cloudformation:*:${this.account}:stack/${projectName}-${environment}-*/*`
                        ]
                    }] : [])
                ]
            },
            tags: [
                {
                    key: 'Environment',
                    value: environment
                },
                {
                    key: 'Project',
                    value: projectName
                },
                {
                    key: 'PermissionSetType',
                    value: 'Developer'
                }
            ]
        });

        return developerPermissionSet;
    }

    /**
     * Create Admin Permission Set with Cross-Region Support
     * Provides full administrative access with time restrictions across multiple regions
     */
    private createAdminPermissionSet(instanceArn: string, projectName: string, environment: string, region?: string, crossRegionEnabled?: boolean): sso.CfnPermissionSet {
        const adminPermissionSet = new sso.CfnPermissionSet(this, 'AdminPermissionSet', {
            instanceArn: instanceArn,
            name: `${projectName}-${environment}-Admin`,
            description: `Administrator access for ${projectName} ${environment} environment`,
            sessionDuration: 'PT4H', // 4 hours for security
            managedPolicies: [
                'arn:aws:iam::aws:policy/AdministratorAccess'
            ],
            inlinePolicy: {
                Version: '2012-10-17',
                Statement: [
                    // Additional logging for admin actions
                    {
                        Sid: 'RequireCloudTrailLogging',
                        Effect: 'Deny',
                        Action: '*',
                        Resource: '*',
                        Condition: {
                            Bool: {
                                'aws:CloudTrailLogged': 'false'
                            }
                        }
                    },
                    // Require MFA for sensitive operations
                    {
                        Sid: 'RequireMFAForSensitiveOperations',
                        Effect: 'Deny',
                        Action: [
                            'iam:DeleteRole',
                            'iam:DeletePolicy',
                            'rds:DeleteDBCluster',
                            'rds:DeleteDBInstance',
                            'elasticache:DeleteReplicationGroup',
                            'kafka:DeleteCluster',
                            's3:DeleteBucket',
                            'kms:ScheduleKeyDeletion',
                            ...(crossRegionEnabled ? [
                                'rds:FailoverGlobalCluster',
                                'route53:ChangeResourceRecordSets',
                                'cloudfront:UpdateDistribution'
                            ] : [])
                        ],
                        Resource: '*',
                        Condition: {
                            BoolIfExists: {
                                'aws:MultiFactorAuthPresent': 'false'
                            }
                        }
                    },
                    // Cross-region admin access (if enabled)
                    ...(crossRegionEnabled ? [{
                        Sid: 'AllowCrossRegionAdminAccess',
                        Effect: 'Allow',
                        Action: [
                            'route53:*',
                            'cloudfront:*',
                            'rds:FailoverGlobalCluster',
                            'rds:CreateGlobalCluster',
                            'rds:DeleteGlobalCluster',
                            'rds:ModifyGlobalCluster',
                            'elasticache:CreateGlobalReplicationGroup',
                            'elasticache:ModifyGlobalReplicationGroup',
                            'elasticache:DeleteGlobalReplicationGroup'
                        ],
                        Resource: '*'
                    }] : [])
                ]
            },
            tags: [
                {
                    key: 'Environment',
                    value: environment
                },
                {
                    key: 'Project',
                    value: projectName
                },
                {
                    key: 'PermissionSetType',
                    value: 'Admin'
                }
            ]
        });

        return adminPermissionSet;
    }

    /**
     * Create Read-Only Permission Set with Cross-Region Support
     * Provides read-only access for monitoring and auditing across multiple regions
     */
    private createReadOnlyPermissionSet(instanceArn: string, projectName: string, environment: string, region?: string, crossRegionEnabled?: boolean): sso.CfnPermissionSet {
        const readOnlyPermissionSet = new sso.CfnPermissionSet(this, 'ReadOnlyPermissionSet', {
            instanceArn: instanceArn,
            name: `${projectName}-${environment}-ReadOnly`,
            description: `Read-only access for ${projectName} ${environment} environment`,
            sessionDuration: 'PT12H', // 12 hours for monitoring
            managedPolicies: [
                'arn:aws:iam::aws:policy/ReadOnlyAccess'
            ],
            inlinePolicy: {
                Version: '2012-10-17',
                Statement: [
                    // Allow CloudWatch Insights queries
                    {
                        Sid: 'AllowCloudWatchInsights',
                        Effect: 'Allow',
                        Action: [
                            'logs:StartQuery',
                            'logs:StopQuery',
                            'logs:GetQueryResults',
                            'logs:GetLogEvents'
                        ],
                        Resource: '*'
                    },
                    // Allow X-Ray trace analysis
                    {
                        Sid: 'AllowXRayAnalysis',
                        Effect: 'Allow',
                        Action: [
                            'xray:GetTraceGraph',
                            'xray:GetTraceSummaries',
                            'xray:BatchGetTraces',
                            'xray:GetServiceGraph',
                            'xray:GetTimeSeriesServiceStatistics'
                        ],
                        Resource: '*'
                    },
                    // Allow Cost Explorer access
                    {
                        Sid: 'AllowCostExplorerAccess',
                        Effect: 'Allow',
                        Action: [
                            'ce:GetCostAndUsage',
                            'ce:GetDimensionValues',
                            'ce:GetReservationCoverage',
                            'ce:GetReservationPurchaseRecommendation',
                            'ce:GetReservationUtilization',
                            'ce:GetUsageReport',
                            'ce:ListCostCategoryDefinitions'
                        ],
                        Resource: '*'
                    }
                ]
            },
            tags: [
                {
                    key: 'Environment',
                    value: environment
                },
                {
                    key: 'Project',
                    value: projectName
                },
                {
                    key: 'PermissionSetType',
                    value: 'ReadOnly'
                }
            ]
        });

        return readOnlyPermissionSet;
    }

    /**
     * Create Data Analyst Permission Set with Cross-Region Support
     * Provides access to data services and analytics tools across multiple regions
     */
    private createDataAnalystPermissionSet(instanceArn: string, projectName: string, environment: string, region?: string, crossRegionEnabled?: boolean): sso.CfnPermissionSet {
        const dataAnalystPermissionSet = new sso.CfnPermissionSet(this, 'DataAnalystPermissionSet', {
            instanceArn: instanceArn,
            name: `${projectName}-${environment}-DataAnalyst`,
            description: `Data analyst access for ${projectName} ${environment} environment`,
            sessionDuration: 'PT8H', // 8 hours for data analysis
            managedPolicies: [
                'arn:aws:iam::aws:policy/service-role/AmazonRedshiftServiceRole',
                'arn:aws:iam::aws:policy/AmazonAthenaFullAccess'
            ],
            inlinePolicy: {
                Version: '2012-10-17',
                Statement: [
                    // S3 access for data buckets
                    {
                        Sid: 'AllowS3DataAccess',
                        Effect: 'Allow',
                        Action: [
                            's3:GetObject',
                            's3:ListBucket',
                            's3:GetBucketLocation',
                            's3:GetObjectVersion',
                            's3:ListBucketVersions'
                        ],
                        Resource: [
                            `arn:aws:s3:::${projectName}-${environment}-data-*`,
                            `arn:aws:s3:::${projectName}-${environment}-data-*/*`,
                            `arn:aws:s3:::${projectName}-${environment}-analytics-*`,
                            `arn:aws:s3:::${projectName}-${environment}-analytics-*/*`
                        ]
                    },
                    // Glue Data Catalog access
                    {
                        Sid: 'AllowGlueDataCatalogAccess',
                        Effect: 'Allow',
                        Action: [
                            'glue:GetDatabase',
                            'glue:GetDatabases',
                            'glue:GetTable',
                            'glue:GetTables',
                            'glue:GetPartition',
                            'glue:GetPartitions',
                            'glue:GetCrawler',
                            'glue:GetCrawlers',
                            'glue:GetCrawlerMetrics'
                        ],
                        Resource: [
                            `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:catalog`,
                            `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:database/${projectName}-${environment}-*`,
                            `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:table/${projectName}-${environment}-*/*`,
                            `arn:aws:glue:${region || 'ap-east-2'}:${this.account}:crawler/${projectName}-${environment}-*`
                        ]
                    },
                    // RDS read-only access for analytics
                    {
                        Sid: 'AllowRDSReadOnlyAccess',
                        Effect: 'Allow',
                        Action: [
                            'rds:DescribeDBClusters',
                            'rds:DescribeDBInstances',
                            'rds:DescribeDBClusterSnapshots',
                            'rds:DescribeDBSnapshots',
                            'rds-db:connect'
                        ],
                        Resource: [
                            `arn:aws:rds:${region || 'ap-east-2'}:${this.account}:cluster:${projectName}-${environment}-*`,
                            `arn:aws:rds:${region || 'ap-east-2'}:${this.account}:db:${projectName}-${environment}-*`,
                            `arn:aws:rds-db:${region || 'ap-east-2'}:${this.account}:dbuser:${projectName}-${environment}-*/readonly-user`
                        ]
                    },
                    // QuickSight access (if used)
                    {
                        Sid: 'AllowQuickSightAccess',
                        Effect: 'Allow',
                        Action: [
                            'quicksight:CreateDataSet',
                            'quicksight:CreateDataSource',
                            'quicksight:CreateAnalysis',
                            'quicksight:CreateDashboard',
                            'quicksight:DescribeDataSet',
                            'quicksight:DescribeDataSource',
                            'quicksight:DescribeAnalysis',
                            'quicksight:DescribeDashboard',
                            'quicksight:ListDataSets',
                            'quicksight:ListDataSources',
                            'quicksight:ListAnalyses',
                            'quicksight:ListDashboards'
                        ],
                        Resource: '*'
                    }
                ]
            },
            tags: [
                {
                    key: 'Environment',
                    value: environment
                },
                {
                    key: 'Project',
                    value: projectName
                },
                {
                    key: 'PermissionSetType',
                    value: 'DataAnalyst'
                }
            ]
        });

        return dataAnalystPermissionSet;
    }

    /**
     * Create Cross-Region Admin Permission Set
     * Provides administrative access specifically for multi-region operations
     */
    private createCrossRegionAdminPermissionSet(
        instanceArn: string, 
        projectName: string, 
        environment: string, 
        primaryRegion?: string,
        secondaryRegions?: string[]
    ): sso.CfnPermissionSet {
        const crossRegionAdminPermissionSet = new sso.CfnPermissionSet(this, 'CrossRegionAdminPermissionSet', {
            instanceArn: instanceArn,
            name: `${projectName}-${environment}-CrossRegionAdmin`,
            description: `Cross-region administrator access for ${projectName} ${environment} environment`,
            sessionDuration: 'PT2H', // 2 hours for security
            managedPolicies: [
                'arn:aws:iam::aws:policy/PowerUserAccess'
            ],
            inlinePolicy: {
                Version: '2012-10-17',
                Statement: [
                    // Global services access
                    {
                        Sid: 'AllowGlobalServicesAccess',
                        Effect: 'Allow',
                        Action: [
                            'route53:*',
                            'cloudfront:*',
                            'iam:ListRoles',
                            'iam:GetRole',
                            'iam:PassRole',
                            'sts:AssumeRole'
                        ],
                        Resource: '*'
                    },
                    // Cross-region database operations
                    {
                        Sid: 'AllowCrossRegionDatabaseOperations',
                        Effect: 'Allow',
                        Action: [
                            'rds:*GlobalCluster*',
                            'rds:FailoverGlobalCluster',
                            'rds:CreateDBCluster',
                            'rds:ModifyDBCluster',
                            'rds:DescribeDBClusters',
                            'rds:DescribeGlobalClusters'
                        ],
                        Resource: '*'
                    },
                    // Cross-region cache operations
                    {
                        Sid: 'AllowCrossRegionCacheOperations',
                        Effect: 'Allow',
                        Action: [
                            'elasticache:*GlobalReplicationGroup*',
                            'elasticache:CreateReplicationGroup',
                            'elasticache:ModifyReplicationGroup',
                            'elasticache:DescribeReplicationGroups',
                            'elasticache:DescribeGlobalReplicationGroups'
                        ],
                        Resource: '*'
                    },
                    // Cross-region networking
                    {
                        Sid: 'AllowCrossRegionNetworking',
                        Effect: 'Allow',
                        Action: [
                            'ec2:*VpcPeering*',
                            'ec2:*TransitGateway*',
                            'ec2:DescribeRegions',
                            'ec2:DescribeAvailabilityZones',
                            'ec2:DescribeVpcs',
                            'ec2:DescribeSubnets',
                            'ec2:DescribeSecurityGroups'
                        ],
                        Resource: '*'
                    },
                    // Cross-region monitoring and logging
                    {
                        Sid: 'AllowCrossRegionMonitoring',
                        Effect: 'Allow',
                        Action: [
                            'cloudwatch:*',
                            'logs:*',
                            'xray:*',
                            'cloudtrail:*'
                        ],
                        Resource: '*'
                    },
                    // Require MFA for critical cross-region operations
                    {
                        Sid: 'RequireMFAForCriticalOperations',
                        Effect: 'Deny',
                        Action: [
                            'rds:FailoverGlobalCluster',
                            'rds:DeleteGlobalCluster',
                            'route53:ChangeResourceRecordSets',
                            'cloudfront:UpdateDistribution'
                        ],
                        Resource: '*',
                        Condition: {
                            BoolIfExists: {
                                'aws:MultiFactorAuthPresent': 'false'
                            }
                        }
                    }
                ]
            },
            tags: [
                {
                    key: 'Environment',
                    value: environment
                },
                {
                    key: 'Project',
                    value: projectName
                },
                {
                    key: 'PermissionSetType',
                    value: 'CrossRegionAdmin'
                },
                {
                    key: 'PrimaryRegion',
                    value: primaryRegion || 'unknown'
                }
            ]
        });

        return crossRegionAdminPermissionSet;
    }

    /**
     * Create cross-region audit bucket for CloudTrail logs
     */
    private createCrossRegionAuditBucket(
        projectName: string, 
        environment: string, 
        primaryRegion?: string,
        secondaryRegions?: string[]
    ): s3.Bucket {
        // Create KMS key for audit log encryption
        const auditLogKey = new kms.Key(this, 'AuditLogEncryptionKey', {
            description: `Cross-region audit log encryption key for ${projectName}-${environment}`,
            enableKeyRotation: true,
            keySpec: kms.KeySpec.SYMMETRIC_DEFAULT,
            keyUsage: kms.KeyUsage.ENCRYPT_DECRYPT,
            policy: new iam.PolicyDocument({
                statements: [
                    new iam.PolicyStatement({
                        sid: 'EnableIAMUserPermissions',
                        effect: iam.Effect.ALLOW,
                        principals: [new iam.AccountRootPrincipal()],
                        actions: ['kms:*'],
                        resources: ['*']
                    }),
                    new iam.PolicyStatement({
                        sid: 'AllowCloudTrailEncryption',
                        effect: iam.Effect.ALLOW,
                        principals: [new iam.ServicePrincipal('cloudtrail.amazonaws.com')],
                        actions: [
                            'kms:GenerateDataKey*',
                            'kms:DescribeKey',
                            'kms:Encrypt',
                            'kms:ReEncrypt*',
                            'kms:Decrypt'
                        ],
                        resources: ['*']
                    })
                ]
            })
        });

        // Create audit bucket with cross-region replication
        const auditBucket = new s3.Bucket(this, 'CrossRegionAuditBucket', {
            bucketName: `${projectName}-${environment}-cross-region-audit-${this.account}-${this.region}`,
            encryption: s3.BucketEncryption.KMS,
            encryptionKey: auditLogKey,
            versioned: true,
            lifecycleRules: [
                {
                    id: 'AuditLogRetention',
                    enabled: true,
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(30)
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(90)
                        },
                        {
                            storageClass: s3.StorageClass.DEEP_ARCHIVE,
                            transitionAfter: cdk.Duration.days(365)
                        }
                    ],
                    expiration: cdk.Duration.days(2555) // 7 years retention
                }
            ],
            publicReadAccess: false,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            removalPolicy: cdk.RemovalPolicy.RETAIN
        });

        // Add bucket policy for CloudTrail
        auditBucket.addToResourcePolicy(new iam.PolicyStatement({
            sid: 'AWSCloudTrailAclCheck',
            effect: iam.Effect.ALLOW,
            principals: [new iam.ServicePrincipal('cloudtrail.amazonaws.com')],
            actions: ['s3:GetBucketAcl'],
            resources: [auditBucket.bucketArn]
        }));

        auditBucket.addToResourcePolicy(new iam.PolicyStatement({
            sid: 'AWSCloudTrailWrite',
            effect: iam.Effect.ALLOW,
            principals: [new iam.ServicePrincipal('cloudtrail.amazonaws.com')],
            actions: ['s3:PutObject'],
            resources: [`${auditBucket.bucketArn}/*`],
            conditions: {
                StringEquals: {
                    's3:x-amz-acl': 'bucket-owner-full-control'
                }
            }
        }));

        return auditBucket;
    }

    /**
     * Create cross-region CloudTrail for unified audit logging
     */
    private createCrossRegionCloudTrail(
        projectName: string, 
        environment: string, 
        auditBucket: s3.Bucket
    ): cloudtrail.Trail {
        const trail = new cloudtrail.Trail(this, 'CrossRegionCloudTrail', {
            trailName: `${projectName}-${environment}-cross-region-audit`,
            bucket: auditBucket,
            s3KeyPrefix: 'cloudtrail-logs',
            includeGlobalServiceEvents: true,
            isMultiRegionTrail: true,
            enableFileValidation: true,
            sendToCloudWatchLogs: true,
            cloudWatchLogGroup: new logs.LogGroup(this, 'CloudTrailLogGroup', {
                logGroupName: `/aws/cloudtrail/${projectName}-${environment}-cross-region`,
                retention: logs.RetentionDays.ONE_YEAR,
                removalPolicy: cdk.RemovalPolicy.RETAIN
            }),
            managementEvents: cloudtrail.ReadWriteType.ALL
        });

        // Add data events for S3 and Lambda
        trail.addS3EventSelector([{
            bucket: auditBucket,
            objectPrefix: 'sensitive-data/'
        }]);

        // Add event selectors for SSO and IAM events
        trail.addEventSelector(cloudtrail.DataResourceType.S3_OBJECT, [
            `${auditBucket.bucketArn}/*`
        ]);

        // Add CloudTrail Insights (done via CloudFormation after trail creation)
        const cfnTrail = trail.node.defaultChild as cloudtrail.CfnTrail;
        cfnTrail.insightSelectors = [
            {
                insightType: 'ApiCallRateInsight'
            }
        ];

        return trail;
    }

    /**
     * Create security event correlation function
     */
    private createSecurityEventCorrelationFunction(
        projectName: string, 
        environment: string, 
        alertsTopic: sns.ITopic,
        primaryRegion?: string,
        secondaryRegions?: string[]
    ): lambda.Function {
        const correlationFunction = new lambda.Function(this, 'SecurityEventCorrelationFunction', {
            functionName: `${projectName}-${environment}-security-event-correlation`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(10),
            memorySize: 512,
            environment: {
                ENVIRONMENT: environment,
                PROJECT_NAME: projectName,
                ALERTS_TOPIC_ARN: alertsTopic.topicArn,
                PRIMARY_REGION: primaryRegion || this.region,
                SECONDARY_REGIONS: JSON.stringify(secondaryRegions || [])
            },
            code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime, timedelta
from collections import defaultdict

def handler(event, context):
    """
    Correlate security events across multiple regions for unified threat detection
    """
    sns = boto3.client('sns')
    cloudtrail = boto3.client('cloudtrail')
    alerts_topic_arn = os.environ['ALERTS_TOPIC_ARN']
    primary_region = os.environ['PRIMARY_REGION']
    secondary_regions = json.loads(os.environ.get('SECONDARY_REGIONS', '[]'))
    
    try:
        # Parse incoming security event
        if 'Records' in event:
            # CloudTrail log event
            return process_cloudtrail_event(event, sns, alerts_topic_arn, primary_region, secondary_regions)
        elif 'source' in event:
            # EventBridge event
            return process_eventbridge_event(event, sns, alerts_topic_arn, primary_region, secondary_regions)
        else:
            print(f"Unknown event format: {json.dumps(event)}")
            return {'statusCode': 200, 'body': 'Event processed'}
            
    except Exception as e:
        print(f"Error in security event correlation: {str(e)}")
        sns.publish(
            TopicArn=alerts_topic_arn,
            Subject=f"Security Event Correlation Error - {os.environ['PROJECT_NAME']}",
            Message=f"Error in security event correlation: {str(e)}\\n\\nEvent: {json.dumps(event)}"
        )
        raise e

def process_cloudtrail_event(event, sns, alerts_topic_arn, primary_region, secondary_regions):
    """Process CloudTrail events for cross-region security analysis"""
    suspicious_patterns = []
    
    for record in event.get('Records', []):
        if 's3' in record:
            # S3 CloudTrail log delivery
            bucket = record['s3']['bucket']['name']
            key = record['s3']['object']['key']
            
            # Analyze CloudTrail logs for suspicious patterns
            patterns = analyze_cloudtrail_logs(bucket, key, primary_region, secondary_regions)
            suspicious_patterns.extend(patterns)
    
    if suspicious_patterns:
        send_correlation_alert(sns, alerts_topic_arn, suspicious_patterns, 'CloudTrail')
    
    return {'statusCode': 200, 'body': f'Processed {len(suspicious_patterns)} suspicious patterns'}

def process_eventbridge_event(event, sns, alerts_topic_arn, primary_region, secondary_regions):
    """Process EventBridge security events"""
    event_source = event.get('source', '')
    detail_type = event.get('detail-type', '')
    
    if event_source == 'aws.sso':
        return process_sso_event(event, sns, alerts_topic_arn, primary_region, secondary_regions)
    elif event_source == 'aws.iam':
        return process_iam_event(event, sns, alerts_topic_arn, primary_region, secondary_regions)
    elif event_source == 'aws.guardduty':
        return process_guardduty_cross_region(event, sns, alerts_topic_arn, primary_region, secondary_regions)
    
    return {'statusCode': 200, 'body': 'EventBridge event processed'}

def process_sso_event(event, sns, alerts_topic_arn, primary_region, secondary_regions):
    """Process SSO events for cross-region correlation"""
    detail = event.get('detail', {})
    event_name = detail.get('eventName', '')
    
    # Detect suspicious SSO activities
    if event_name in ['AssumeRoleWithSAML', 'AssumeRoleWithWebIdentity']:
        source_ip = detail.get('sourceIPAddress', '')
        user_identity = detail.get('userIdentity', {})
        
        # Check for impossible travel (same user from different regions quickly)
        if is_impossible_travel(user_identity, source_ip, event.get('time')):
            send_correlation_alert(
                sns, 
                alerts_topic_arn, 
                [{
                    'type': 'impossible_travel',
                    'user': user_identity.get('userName', 'unknown'),
                    'source_ip': source_ip,
                    'event_time': event.get('time'),
                    'event_name': event_name
                }], 
                'SSO'
            )
    
    return {'statusCode': 200, 'body': 'SSO event processed'}

def process_iam_event(event, sns, alerts_topic_arn, primary_region, secondary_regions):
    """Process IAM events for privilege escalation detection"""
    detail = event.get('detail', {})
    event_name = detail.get('eventName', '')
    
    # Detect privilege escalation attempts
    high_risk_actions = [
        'AttachUserPolicy',
        'AttachRolePolicy',
        'PutUserPolicy',
        'PutRolePolicy',
        'CreateRole',
        'CreateUser'
    ]
    
    if event_name in high_risk_actions:
        user_identity = detail.get('userIdentity', {})
        
        send_correlation_alert(
            sns,
            alerts_topic_arn,
            [{
                'type': 'privilege_escalation_attempt',
                'user': user_identity.get('userName', 'unknown'),
                'action': event_name,
                'region': event.get('awsRegion'),
                'event_time': event.get('time')
            }],
            'IAM'
        )
    
    return {'statusCode': 200, 'body': 'IAM event processed'}

def process_guardduty_cross_region(event, sns, alerts_topic_arn, primary_region, secondary_regions):
    """Process GuardDuty findings across regions"""
    detail = event.get('detail', {})
    finding_type = detail.get('type', '')
    severity = detail.get('severity', 0)
    
    # Correlate high-severity findings across regions
    if severity >= 7.0:
        send_correlation_alert(
            sns,
            alerts_topic_arn,
            [{
                'type': 'high_severity_guardduty',
                'finding_type': finding_type,
                'severity': severity,
                'region': event.get('region'),
                'event_time': event.get('time')
            }],
            'GuardDuty'
        )
    
    return {'statusCode': 200, 'body': 'GuardDuty event processed'}

def analyze_cloudtrail_logs(bucket, key, primary_region, secondary_regions):
    """Analyze CloudTrail logs for suspicious patterns"""
    # This would implement log analysis logic
    # For now, return empty list
    return []

def is_impossible_travel(user_identity, source_ip, event_time):
    """Detect impossible travel scenarios"""
    # This would implement geolocation and timing analysis
    # For now, return False
    return False

def send_correlation_alert(sns, alerts_topic_arn, patterns, source_type):
    """Send correlated security alert"""
    message = f"""
🔍 CROSS-REGION SECURITY EVENT CORRELATION 🔍

Source: {source_type}
Patterns Detected: {len(patterns)}

Details:
{json.dumps(patterns, indent=2)}

This alert indicates potential coordinated security events across multiple regions.
Immediate investigation recommended.
    """
    
    sns.publish(
        TopicArn=alerts_topic_arn,
        Subject=f"🔍 Cross-Region Security Correlation: {source_type}",
        Message=message
    )
            `)
        });

        // Grant permissions
        alertsTopic.grantPublish(correlationFunction);
        
        correlationFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudtrail:LookupEvents',
                'cloudtrail:GetTrailStatus',
                's3:GetObject',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents'
            ],
            resources: ['*']
        }));

        return correlationFunction;
    }

    /**
     * Configure cross-region security monitoring integration
     */
    private configureCrossRegionSecurityMonitoring(
        projectName: string, 
        environment: string, 
        primaryRegion?: string,
        secondaryRegions?: string[]
    ): void {
        // Create EventBridge rules for SSO events
        const ssoEventRule = new events.Rule(this, 'SSOEventRule', {
            ruleName: `${projectName}-${environment}-sso-events`,
            description: 'Capture SSO events for cross-region correlation',
            eventPattern: {
                source: ['aws.sso'],
                detailType: ['AWS API Call via CloudTrail']
            }
        });

        // Create EventBridge rules for IAM events
        const iamEventRule = new events.Rule(this, 'IAMEventRule', {
            ruleName: `${projectName}-${environment}-iam-events`,
            description: 'Capture IAM events for privilege escalation detection',
            eventPattern: {
                source: ['aws.iam'],
                detailType: ['AWS API Call via CloudTrail'],
                detail: {
                    eventName: [
                        'AttachUserPolicy',
                        'AttachRolePolicy',
                        'PutUserPolicy',
                        'PutRolePolicy',
                        'CreateRole',
                        'CreateUser',
                        'AssumeRole'
                    ]
                }
            }
        });

        // Add targets to correlation function if it exists
        if (this.securityEventCorrelationFunction) {
            ssoEventRule.addTarget(new targets.LambdaFunction(this.securityEventCorrelationFunction));
            iamEventRule.addTarget(new targets.LambdaFunction(this.securityEventCorrelationFunction));
        }
    }

    /**
     * Create enhanced cross-account roles for multi-region access with unified RBAC
     */
    private createCrossAccountRoles(
        projectName: string, 
        environment: string, 
        region?: string, 
        crossRegionEnabled?: boolean,
        primaryRegion?: string,
        secondaryRegions?: string[]
    ): void {
        // Enhanced cross-region access role for Active-Active architecture
        const crossRegionRole = new iam.Role(this, 'CrossRegionAccessRole', {
            roleName: `${projectName}-${environment}-cross-region-role`,
            description: crossRegionEnabled 
                ? 'Role for cross-region access in Active-Active architecture'
                : 'Role for cross-region access during disaster recovery',
            assumedBy: new iam.AccountPrincipal(this.account),
            maxSessionDuration: cdk.Duration.hours(crossRegionEnabled ? 4 : 2)
        });

        // Enhanced cross-region access policies for Active-Active
        const crossRegionPolicyStatements = [
            new iam.PolicyStatement({
                sid: 'AllowCrossRegionAccess',
                effect: iam.Effect.ALLOW,
                actions: [
                    'ec2:DescribeRegions',
                    'ec2:DescribeAvailabilityZones',
                    'route53:*',
                    'cloudfront:*',
                    'rds:DescribeGlobalClusters',
                    'rds:FailoverGlobalCluster'
                ],
                resources: ['*']
            }),
            new iam.PolicyStatement({
                sid: 'AllowAssumeRoleInOtherRegions',
                effect: iam.Effect.ALLOW,
                actions: ['sts:AssumeRole'],
                resources: [
                    `arn:aws:iam::${this.account}:role/${projectName}-*-cross-region-role`
                ]
            })
        ];

        // Add enhanced permissions for Active-Active architecture
        if (crossRegionEnabled) {
            crossRegionPolicyStatements.push(
                new iam.PolicyStatement({
                    sid: 'AllowActiveActiveOperations',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        // Global database operations
                        'rds:CreateGlobalCluster',
                        'rds:ModifyGlobalCluster',
                        'rds:DeleteGlobalCluster',
                        'rds:AddRoleToDBCluster',
                        'rds:RemoveRoleFromDBCluster',
                        // Global cache operations
                        'elasticache:CreateGlobalReplicationGroup',
                        'elasticache:ModifyGlobalReplicationGroup',
                        'elasticache:DeleteGlobalReplicationGroup',
                        'elasticache:IncreaseNodeGroupsInGlobalReplicationGroup',
                        'elasticache:DecreaseNodeGroupsInGlobalReplicationGroup',
                        // Cross-region networking
                        'ec2:CreateVpcPeeringConnection',
                        'ec2:AcceptVpcPeeringConnection',
                        'ec2:DeleteVpcPeeringConnection',
                        'ec2:CreateTransitGateway',
                        'ec2:CreateTransitGatewayPeeringAttachment',
                        'ec2:AcceptTransitGatewayPeeringAttachment',
                        // Cross-region monitoring
                        'cloudwatch:PutMetricData',
                        'logs:CreateLogGroup',
                        'logs:CreateLogStream',
                        'logs:PutLogEvents',
                        'xray:PutTraceSegments',
                        'xray:PutTelemetryRecords'
                    ],
                    resources: ['*']
                }),
                new iam.PolicyStatement({
                    sid: 'AllowCrossRegionSecretAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'secretsmanager:GetSecretValue',
                        'secretsmanager:DescribeSecret',
                        'secretsmanager:ReplicateSecretToRegions'
                    ],
                    resources: [
                        `arn:aws:secretsmanager:*:${this.account}:secret:${projectName}/${environment}/*`
                    ]
                }),
                new iam.PolicyStatement({
                    sid: 'AllowCrossRegionParameterAccess',
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'ssm:GetParameter',
                        'ssm:GetParameters',
                        'ssm:GetParametersByPath',
                        'ssm:PutParameter'
                    ],
                    resources: [
                        `arn:aws:ssm:*:${this.account}:parameter/${projectName}/${environment}/*`
                    ]
                })
            );
        }

        crossRegionRole.attachInlinePolicy(new iam.Policy(this, 'CrossRegionAccessPolicy', {
            policyName: 'CrossRegionAccess',
            statements: crossRegionPolicyStatements
        }));

        // Create region-specific execution roles
        if (crossRegionEnabled && secondaryRegions) {
            secondaryRegions.forEach((secondaryRegion, index) => {
                const regionRole = new iam.Role(this, `RegionExecutionRole${index}`, {
                    roleName: `${projectName}-${environment}-${secondaryRegion}-execution-role`,
                    description: `Execution role for ${secondaryRegion} region operations`,
                    assumedBy: new iam.CompositePrincipal(
                        new iam.AccountPrincipal(this.account),
                        new iam.ServicePrincipal('lambda.amazonaws.com'),
                        new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
                    ),
                    maxSessionDuration: cdk.Duration.hours(1)
                });

                regionRole.attachInlinePolicy(new iam.Policy(this, `RegionExecutionPolicy${index}`, {
                    policyName: `${secondaryRegion}ExecutionPolicy`,
                    statements: [
                        new iam.PolicyStatement({
                            sid: 'AllowRegionSpecificOperations',
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'eks:*',
                                'ecs:*',
                                'lambda:*',
                                'rds:*',
                                'elasticache:*',
                                'ec2:*',
                                's3:*'
                            ],
                            resources: ['*'],
                            conditions: {
                                StringEquals: {
                                    'aws:RequestedRegion': secondaryRegion
                                }
                            }
                        })
                    ]
                }));

                cdk.Tags.of(regionRole).add('RoleType', 'RegionExecution');
                cdk.Tags.of(regionRole).add('TargetRegion', secondaryRegion);
                cdk.Tags.of(regionRole).add('Purpose', 'ActiveActive');
            });
        }

        cdk.Tags.of(crossRegionRole).add('RoleType', 'CrossRegion');
        cdk.Tags.of(crossRegionRole).add('Purpose', crossRegionEnabled ? 'ActiveActive' : 'DisasterRecovery');
        cdk.Tags.of(crossRegionRole).add('MultiRegionEnabled', crossRegionEnabled ? 'true' : 'false');
    }

    /**
     * Create stack outputs
     */
    private createOutputs(projectName: string, environment: string, crossRegionEnabled?: boolean): void {
        new cdk.CfnOutput(this, 'DeveloperPermissionSetArn', {
            value: this.developerPermissionSet.attrPermissionSetArn,
            exportName: `${this.stackName}-DeveloperPermissionSetArn`,
            description: 'ARN of the Developer Permission Set'
        });

        new cdk.CfnOutput(this, 'AdminPermissionSetArn', {
            value: this.adminPermissionSet.attrPermissionSetArn,
            exportName: `${this.stackName}-AdminPermissionSetArn`,
            description: 'ARN of the Admin Permission Set'
        });

        new cdk.CfnOutput(this, 'ReadOnlyPermissionSetArn', {
            value: this.readOnlyPermissionSet.attrPermissionSetArn,
            exportName: `${this.stackName}-ReadOnlyPermissionSetArn`,
            description: 'ARN of the ReadOnly Permission Set'
        });

        new cdk.CfnOutput(this, 'DataAnalystPermissionSetArn', {
            value: this.dataAnalystPermissionSet.attrPermissionSetArn,
            exportName: `${this.stackName}-DataAnalystPermissionSetArn`,
            description: 'ARN of the DataAnalyst Permission Set'
        });

        // Cross-region specific outputs
        if (crossRegionEnabled) {
            new cdk.CfnOutput(this, 'CrossRegionAdminPermissionSetArn', {
                value: this.crossRegionAdminPermissionSet.attrPermissionSetArn,
                exportName: `${this.stackName}-CrossRegionAdminPermissionSetArn`,
                description: 'ARN of the Cross-Region Admin Permission Set'
            });

            new cdk.CfnOutput(this, 'CrossRegionAuditBucketName', {
                value: this.crossRegionAuditBucket.bucketName,
                exportName: `${this.stackName}-CrossRegionAuditBucketName`,
                description: 'Name of the cross-region audit bucket'
            });

            new cdk.CfnOutput(this, 'CrossRegionCloudTrailArn', {
                value: this.crossRegionCloudTrail.trailArn,
                exportName: `${this.stackName}-CrossRegionCloudTrailArn`,
                description: 'ARN of the cross-region CloudTrail'
            });

            if (this.securityEventCorrelationFunction) {
                new cdk.CfnOutput(this, 'SecurityEventCorrelationFunctionArn', {
                    value: this.securityEventCorrelationFunction.functionArn,
                    exportName: `${this.stackName}-SecurityEventCorrelationFunctionArn`,
                    description: 'ARN of the security event correlation function'
                });
            }
        }

        // Output instructions for manual account assignments
        new cdk.CfnOutput(this, 'AccountAssignmentInstructions', {
            value: `Use AWS CLI or Console to assign users/groups to permission sets. Example: aws sso-admin create-account-assignment --instance-arn <SSO_INSTANCE_ARN> --target-id ${this.account} --target-type AWS_ACCOUNT --permission-set-arn ${this.developerPermissionSet.attrPermissionSetArn} --principal-type USER --principal-id <USER_ID>`,
            description: 'Instructions for creating account assignments'
        });

        // Multi-region configuration status
        new cdk.CfnOutput(this, 'MultiRegionSSOEnabled', {
            value: crossRegionEnabled ? 'true' : 'false',
            exportName: `${this.stackName}-MultiRegionSSOEnabled`,
            description: 'Whether multi-region SSO is enabled'
        });
    }
}