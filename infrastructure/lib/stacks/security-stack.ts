import * as cdk from 'aws-cdk-lib';
import * as cloudtrail from 'aws-cdk-lib/aws-cloudtrail';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface SecurityStackProps extends cdk.StackProps {
    environment: string;
    projectName: string;
    vpc?: any; // Optional VPC for compatibility
}

/**
 * Security Stack implementing IAM roles and policies with least privilege
 * Implements requirement 11.3: WHEN accessing observability tools THEN the system SHALL require proper IAM authentication
 */
export class SecurityStack extends cdk.Stack {
    public readonly applicationRole: iam.Role;
    public readonly observabilityRole: iam.Role;
    public readonly kmsKey: kms.Key;
    public readonly cloudTrail: cloudtrail.Trail;
    public readonly criticalAlertsTopic: sns.Topic;

    constructor(scope: Construct, id: string, props: SecurityStackProps) {
        super(scope, id, props);

        // KMS Key for encryption
        this.kmsKey = this.createKmsKey(props.environment);

        // CloudTrail for audit logging
        this.cloudTrail = this.createCloudTrail(props.environment);

        // Application service role with least privilege
        this.applicationRole = this.createApplicationRole(props.environment);

        // Observability service role
        this.observabilityRole = this.createObservabilityRole(props.environment);

        // Critical alerts SNS topic
        this.criticalAlertsTopic = this.createCriticalAlertsTopic(props.projectName, props.environment);

        // Create secrets for secure configuration
        this.createSecrets(props.environment);

        // Output important ARNs
        this.createOutputs();
    }

    private createKmsKey(environment: string): kms.Key {
        const key = new kms.Key(this, 'GenAIDemoKmsKey', {
            alias: `genai-demo-${environment}-key`,
            description: `KMS key for GenAI Demo ${environment} environment`,
            enableKeyRotation: true,
            rotationPeriod: cdk.Duration.days(365),
            policy: new iam.PolicyDocument({
                statements: [
                    new iam.PolicyStatement({
                        sid: 'EnableRootAccess',
                        effect: iam.Effect.ALLOW,
                        principals: [new iam.AccountRootPrincipal()],
                        actions: ['kms:*'],
                        resources: ['*'],
                    }),
                    new iam.PolicyStatement({
                        sid: 'AllowCloudWatchLogs',
                        effect: iam.Effect.ALLOW,
                        principals: [new iam.ServicePrincipal('logs.amazonaws.com')],
                        actions: [
                            'kms:Encrypt',
                            'kms:Decrypt',
                            'kms:ReEncrypt*',
                            'kms:GenerateDataKey*',
                            'kms:DescribeKey',
                        ],
                        resources: ['*'],
                    }),
                ],
            }),
        });

        cdk.Tags.of(key).add('Environment', environment);
        cdk.Tags.of(key).add('Purpose', 'Encryption');

        return key;
    }

    private createCloudTrail(environment: string): cloudtrail.Trail {
        // S3 bucket for CloudTrail logs
        const cloudTrailBucket = new s3.Bucket(this, 'CloudTrailBucket', {
            bucketName: `genai-demo-cloudtrail-${environment}-${this.account}`,
            encryption: s3.BucketEncryption.KMS,
            encryptionKey: this.kmsKey,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            versioned: true,
            lifecycleRules: [
                {
                    id: 'CloudTrailLogRetention',
                    enabled: true,
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(30),
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(90),
                        },
                    ],
                    expiration: cdk.Duration.days(2555), // 7 years
                },
            ],
        });

        const trail = new cloudtrail.Trail(this, 'GenAIDemoCloudTrail', {
            trailName: `genai-demo-${environment}-trail`,
            bucket: cloudTrailBucket,
            encryptionKey: this.kmsKey,
            includeGlobalServiceEvents: true,
            isMultiRegionTrail: true,
            enableFileValidation: true,
            sendToCloudWatchLogs: true,
            cloudWatchLogGroup: new logs.LogGroup(this, 'CloudTrailLogGroup', {
                logGroupName: `/aws/cloudtrail/genai-demo-${environment}`,
                retention: logs.RetentionDays.ONE_YEAR,
                encryptionKey: this.kmsKey,
            }),
        });

        cdk.Tags.of(trail).add('Environment', environment);
        cdk.Tags.of(trail).add('Purpose', 'SecurityAudit');

        return trail;
    }

    private createApplicationRole(environment: string): iam.Role {
        const role = new iam.Role(this, 'ApplicationRole', {
            roleName: `genai-demo-application-${environment}-role`,
            assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
            description: 'IAM role for GenAI Demo application with least privilege access',
            maxSessionDuration: cdk.Duration.hours(12),
        });

        // CloudWatch Logs permissions
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'CloudWatchLogsAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
            ],
            resources: [
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/ecs/genai-demo-${environment}*`,
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/application/genai-demo-${environment}*`,
            ],
        }));

        // CloudWatch Metrics permissions
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'CloudWatchMetricsAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:PutMetricData',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
            ],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'cloudwatch:namespace': [
                        'GenAIDemo/Application',
                        'GenAIDemo/Business',
                        'AWS/ECS',
                    ],
                },
            },
        }));

        // X-Ray tracing permissions
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'XRayAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'xray:PutTraceSegments',
                'xray:PutTelemetryRecords',
                'xray:GetSamplingRules',
                'xray:GetSamplingTargets',
            ],
            resources: ['*'],
        }));

        // Secrets Manager permissions (specific secrets only)
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'SecretsManagerAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'secretsmanager:GetSecretValue',
                'secretsmanager:DescribeSecret',
            ],
            resources: [
                `arn:aws:secretsmanager:${this.region}:${this.account}:secret:genai-demo/${environment}/*`,
            ],
        }));

        // KMS permissions for decryption
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'KMSDecryptAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'kms:Decrypt',
                'kms:DescribeKey',
                'kms:GenerateDataKey',
            ],
            resources: [this.kmsKey.keyArn],
        }));

        // RDS permissions (specific database only)
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'RDSAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'rds-db:connect',
            ],
            resources: [
                `arn:aws:rds-db:${this.region}:${this.account}:dbuser:genai-demo-${environment}-db/genai_demo_app`,
            ],
        }));

        cdk.Tags.of(role).add('Environment', environment);
        cdk.Tags.of(role).add('Purpose', 'ApplicationAccess');

        return role;
    }

    private createObservabilityRole(environment: string): iam.Role {
        const role = new iam.Role(this, 'ObservabilityRole', {
            roleName: `genai-demo-observability-${environment}-role`,
            assumedBy: new iam.CompositePrincipal(
                new iam.ServicePrincipal('ecs-tasks.amazonaws.com'),
                new iam.ServicePrincipal('lambda.amazonaws.com')
            ),
            description: 'IAM role for observability services (Prometheus, Grafana, etc.)',
            maxSessionDuration: cdk.Duration.hours(12),
        });

        // CloudWatch full read access for observability
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'CloudWatchReadAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:GetMetricData',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
                'cloudwatch:GetDashboard',
                'cloudwatch:ListDashboards',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
                'logs:GetLogEvents',
                'logs:FilterLogEvents',
                'logs:StartQuery',
                'logs:StopQuery',
                'logs:GetQueryResults',
            ],
            resources: ['*'],
        }));

        // X-Ray read access
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'XRayReadAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'xray:GetTraceGraph',
                'xray:GetTraceSummaries',
                'xray:BatchGetTraces',
                'xray:GetServiceGraph',
                'xray:GetTimeSeriesServiceStatistics',
            ],
            resources: ['*'],
        }));

        // ECS read access for service discovery
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'ECSReadAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'ecs:ListClusters',
                'ecs:ListServices',
                'ecs:ListTasks',
                'ecs:DescribeClusters',
                'ecs:DescribeServices',
                'ecs:DescribeTasks',
                'ecs:DescribeTaskDefinition',
            ],
            resources: ['*'],
        }));

        // EC2 read access for instance discovery
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'EC2ReadAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'ec2:DescribeInstances',
                'ec2:DescribeRegions',
                'ec2:DescribeAvailabilityZones',
            ],
            resources: ['*'],
        }));

        cdk.Tags.of(role).add('Environment', environment);
        cdk.Tags.of(role).add('Purpose', 'ObservabilityAccess');

        return role;
    }

    private createSecrets(environment: string): void {
        // Database credentials
        new secretsmanager.Secret(this, 'DatabaseSecret', {
            secretName: `genai-demo/${environment}/database`,
            description: 'Database credentials for GenAI Demo application',
            encryptionKey: this.kmsKey,
            generateSecretString: {
                secretStringTemplate: JSON.stringify({ username: 'genai_demo_app' }),
                generateStringKey: 'password',
                excludeCharacters: '"@/\\\'',
                passwordLength: 32,
            },
        });

        // Kafka credentials
        new secretsmanager.Secret(this, 'KafkaSecret', {
            secretName: `genai-demo/${environment}/kafka`,
            description: 'Kafka credentials and configuration',
            encryptionKey: this.kmsKey,
            secretObjectValue: {
                username: cdk.SecretValue.unsafePlainText('genai-demo-app'),
                password: cdk.SecretValue.unsafePlainText('CHANGE_ME_IN_PRODUCTION'),
                bootstrapServers: cdk.SecretValue.unsafePlainText('localhost:9092'),
            },
        });

        // Application secrets
        new secretsmanager.Secret(this, 'ApplicationSecret', {
            secretName: `genai-demo/${environment}/application`,
            description: 'Application-specific secrets',
            encryptionKey: this.kmsKey,
            secretObjectValue: {
                jwtSecret: cdk.SecretValue.unsafePlainText('CHANGE_ME_IN_PRODUCTION'),
                encryptionKey: cdk.SecretValue.unsafePlainText('CHANGE_ME_IN_PRODUCTION'),
            },
        });
    }

    private createCriticalAlertsTopic(projectName: string, environment: string): sns.Topic {
        const topic = new sns.Topic(this, 'CriticalAlertsTopic', {
            topicName: `${projectName}-${environment}-critical-alerts`,
            displayName: `Critical Alerts for ${projectName} ${environment}`,
            masterKey: this.kmsKey
        });

        cdk.Tags.of(topic).add('Environment', environment);
        cdk.Tags.of(topic).add('Purpose', 'CriticalAlerting');

        return topic;
    }

    private createOutputs(): void {
        new cdk.CfnOutput(this, 'ApplicationRoleArn', {
            value: this.applicationRole.roleArn,
            description: 'ARN of the application IAM role',
            exportName: `genai-demo-application-role-arn`,
        });

        new cdk.CfnOutput(this, 'ObservabilityRoleArn', {
            value: this.observabilityRole.roleArn,
            description: 'ARN of the observability IAM role',
            exportName: `genai-demo-observability-role-arn`,
        });

        new cdk.CfnOutput(this, 'KmsKeyArn', {
            value: this.kmsKey.keyArn,
            description: 'ARN of the KMS key for encryption',
            exportName: `genai-demo-kms-key-arn`,
        });

        new cdk.CfnOutput(this, 'CloudTrailArn', {
            value: this.cloudTrail.trailArn,
            description: 'ARN of the CloudTrail for audit logging',
            exportName: `genai-demo-cloudtrail-arn`,
        });
    }
}