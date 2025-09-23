import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface RdsStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly rdsSecurityGroup: ec2.ISecurityGroup;
    readonly region: string;
    readonly regionType?: 'primary' | 'secondary';
    readonly globalClusterIdentifier?: string;
}

export class RdsStack extends cdk.Stack {
    public readonly database?: rds.DatabaseInstance;
    public readonly auroraCluster?: rds.DatabaseCluster;
    public readonly globalCluster?: rds.CfnGlobalCluster;
    public readonly databaseSecret: secretsmanager.Secret;
    public readonly parameterGroup: rds.ParameterGroup;
    public readonly subnetGroup: rds.SubnetGroup;
    public readonly kmsKey: kms.Key;

    constructor(scope: Construct, id: string, props: RdsStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            vpc,
            rdsSecurityGroup,
            region,
            regionType = 'primary',
            globalClusterIdentifier
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Database',
            Service: 'RDS'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Get environment-specific configuration
        const envConfig = this.node.tryGetContext('genai-demo:environments')?.[environment] || {};
        const regionConfig = this.node.tryGetContext('genai-demo:regions')?.regions?.[region] || {};
        const retentionPolicies = envConfig['retention-policies'] || {};
        const multiRegionConfig = this.node.tryGetContext('genai-demo:multi-region') || {};

        // Create KMS key for database encryption
        this.kmsKey = this.createKmsKey(projectName, environment);

        // Create database subnet group
        this.subnetGroup = this.createSubnetGroup(vpc, projectName, environment);

        // Create parameter group for PostgreSQL optimization
        this.parameterGroup = this.createParameterGroup(projectName, environment);

        // Create database credentials secret
        this.databaseSecret = this.createDatabaseSecret(projectName, environment);

        // Determine if we should use Aurora Global Database for multi-region setup
        const useAuroraGlobal = environment === 'production' && multiRegionConfig['enable-dr'];

        if (useAuroraGlobal) {
            // Create Aurora Global Database for multi-region setup
            if (regionType === 'primary') {
                this.globalCluster = this.createAuroraGlobalCluster(projectName, environment);
                this.auroraCluster = this.createAuroraCluster(
                    vpc,
                    rdsSecurityGroup,
                    projectName,
                    environment,
                    envConfig,
                    retentionPolicies,
                    regionType,
                    this.globalCluster.ref
                );
            } else {
                // Secondary region - create cluster as part of global cluster
                this.auroraCluster = this.createAuroraCluster(
                    vpc,
                    rdsSecurityGroup,
                    projectName,
                    environment,
                    envConfig,
                    retentionPolicies,
                    regionType,
                    globalClusterIdentifier
                );
            }
        } else {
            // Create standard RDS PostgreSQL instance for non-production or single-region
            this.database = this.createRdsInstance(
                vpc,
                rdsSecurityGroup,
                projectName,
                environment,
                envConfig,
                retentionPolicies
            );
        }

        // Create CloudWatch monitoring and alarms
        this.createMonitoringAndAlarms(projectName, environment);

        // Store database connection information in Parameter Store
        this.createParameterStoreParameters(projectName, environment, region);

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment);
    }

    private createKmsKey(projectName: string, environment: string): kms.Key {
        const key = new kms.Key(this, 'DatabaseKmsKey', {
            alias: `${projectName}-${environment}-rds-key`,
            description: `KMS key for ${projectName} ${environment} RDS database encryption`,
            enableKeyRotation: true,
            keySpec: kms.KeySpec.SYMMETRIC_DEFAULT,
            keyUsage: kms.KeyUsage.ENCRYPT_DECRYPT,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY
        });

        // Add key policy for RDS service
        key.addToResourcePolicy(new cdk.aws_iam.PolicyStatement({
            sid: 'AllowRDSAccess',
            effect: cdk.aws_iam.Effect.ALLOW,
            principals: [new cdk.aws_iam.ServicePrincipal('rds.amazonaws.com')],
            actions: [
                'kms:Decrypt',
                'kms:GenerateDataKey',
                'kms:CreateGrant'
            ],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'kms:ViaService': `rds.${this.region}.amazonaws.com`
                }
            }
        }));

        cdk.Tags.of(key).add('Name', `${projectName}-${environment}-rds-kms-key`);
        cdk.Tags.of(key).add('Environment', environment);
        cdk.Tags.of(key).add('Project', projectName);

        return key;
    }

    private createSubnetGroup(vpc: ec2.IVpc, projectName: string, environment: string): rds.SubnetGroup {
        const subnetGroup = new rds.SubnetGroup(this, 'DatabaseSubnetGroup', {
            subnetGroupName: `${projectName}-${environment}-db-subnet-group`,
            description: `Database subnet group for ${projectName} ${environment}`,
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_ISOLATED
            },
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(subnetGroup).add('Name', `${projectName}-${environment}-db-subnet-group`);
        cdk.Tags.of(subnetGroup).add('Environment', environment);
        cdk.Tags.of(subnetGroup).add('Project', projectName);

        return subnetGroup;
    }

    private createAuroraGlobalCluster(projectName: string, environment: string): rds.CfnGlobalCluster {
        const globalCluster = new rds.CfnGlobalCluster(this, 'AuroraGlobalCluster', {
            globalClusterIdentifier: `${projectName}-${environment}-global`,
            engine: 'aurora-postgresql',
            engineVersion: '15.4',
            // databaseName: 'genaidemo', // Not supported in CfnGlobalCluster
            // masterUsername: 'genaidemo_admin', // Not supported in CfnGlobalCluster
            // masterUserPassword: this.databaseSecret.secretValueFromJson('password').unsafeUnwrap(), // Not supported in CfnGlobalCluster
            storageEncrypted: true,
            deletionProtection: true
            // backupRetentionPeriod: 30 // Not supported in CfnGlobalCluster
        });

        cdk.Tags.of(globalCluster).add('Name', `${projectName}-${environment}-global-cluster`);
        cdk.Tags.of(globalCluster).add('Environment', environment);
        cdk.Tags.of(globalCluster).add('Project', projectName);
        cdk.Tags.of(globalCluster).add('DatabaseType', 'Aurora-Global');

        return globalCluster;
    }

    private createAuroraCluster(
        vpc: ec2.IVpc,
        rdsSecurityGroup: ec2.ISecurityGroup,
        projectName: string,
        environment: string,
        envConfig: any,
        retentionPolicies: any,
        regionType: 'primary' | 'secondary',
        globalClusterIdentifier?: string
    ): rds.DatabaseCluster {
        // Get environment-specific configuration
        const instanceType = envConfig['rds-instance-type'] || 'db.r6g.large';
        const backupRetention = retentionPolicies['backups'] || 30;

        // Create Aurora cluster parameter group
        const clusterParameterGroup = new rds.ParameterGroup(this, 'AuroraClusterParameterGroup', {
            description: `Aurora PostgreSQL cluster parameter group for ${projectName} ${environment}`,
            engine: rds.DatabaseClusterEngine.auroraPostgres({
                version: rds.AuroraPostgresEngineVersion.VER_15_4
            }),
            parameters: {
                'shared_preload_libraries': 'pg_stat_statements',
                'log_statement': 'all',
                'log_duration': '1',
                'log_min_duration_statement': '1000',
                'track_activity_query_size': '2048',
                'pg_stat_statements.track': 'all',
                'pg_stat_statements.max': '10000'
            }
        });

        // Create Aurora cluster
        const cluster = new rds.DatabaseCluster(this, 'AuroraCluster', {
            clusterIdentifier: `${projectName}-${environment}-${regionType}-aurora`,
            engine: rds.DatabaseClusterEngine.auroraPostgres({
                version: rds.AuroraPostgresEngineVersion.VER_15_4
            }),

            // Network configuration
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_ISOLATED
            },
            securityGroups: [rdsSecurityGroup],
            subnetGroup: this.subnetGroup,

            // Instance configuration
            writer: rds.ClusterInstance.provisioned('writer', {
                instanceType: ec2.InstanceType.of(
                    this.getInstanceClass(instanceType),
                    this.getInstanceSize(instanceType)
                ),
                enablePerformanceInsights: true,
                performanceInsightEncryptionKey: this.kmsKey,
                performanceInsightRetention: rds.PerformanceInsightRetention.LONG_TERM
            }),

            readers: regionType === 'primary' ? [
                rds.ClusterInstance.provisioned('reader1', {
                    instanceType: ec2.InstanceType.of(
                        this.getInstanceClass(instanceType),
                        this.getInstanceSize(instanceType)
                    ),
                    enablePerformanceInsights: true,
                    performanceInsightEncryptionKey: this.kmsKey
                })
            ] : [],

            // Database configuration
            credentials: regionType === 'primary'
                ? rds.Credentials.fromSecret(this.databaseSecret)
                : undefined,
            defaultDatabaseName: 'genaidemo',
            parameterGroup: clusterParameterGroup,

            // Storage configuration
            storageEncrypted: true,
            storageEncryptionKey: this.kmsKey,

            // Backup and maintenance
            backup: {
                retention: cdk.Duration.days(backupRetention),
                preferredWindow: '03:00-04:00'
            },
            preferredMaintenanceWindow: 'sun:04:00-sun:05:00',

            // Monitoring
            monitoringInterval: cdk.Duration.seconds(60),
            cloudwatchLogsExports: ['postgresql'],

            // Global cluster configuration
            ...(globalClusterIdentifier && {
                // For secondary regions, we need to use CfnDBCluster for global cluster support
            }),

            // Removal policy
            removalPolicy: cdk.RemovalPolicy.SNAPSHOT,
            deletionProtection: true
        });

        // If this is part of a global cluster, we need to modify the underlying CFN resource
        if (globalClusterIdentifier) {
            const cfnCluster = cluster.node.defaultChild as rds.CfnDBCluster;
            cfnCluster.globalClusterIdentifier = globalClusterIdentifier;

            // For secondary regions, remove master credentials as they're inherited from global cluster
            if (regionType === 'secondary') {
                cfnCluster.masterUsername = undefined;
                cfnCluster.masterUserPassword = undefined;
            }
        }

        // Add tags
        cdk.Tags.of(cluster).add('Name', `${projectName}-${environment}-${regionType}-aurora`);
        cdk.Tags.of(cluster).add('Environment', environment);
        cdk.Tags.of(cluster).add('Project', projectName);
        cdk.Tags.of(cluster).add('Engine', 'Aurora-PostgreSQL');
        cdk.Tags.of(cluster).add('RegionType', regionType);
        cdk.Tags.of(cluster).add('GlobalCluster', globalClusterIdentifier || 'none');

        return cluster;
    }

    private createParameterGroup(projectName: string, environment: string): rds.ParameterGroup {
        const parameterGroup = new rds.ParameterGroup(this, 'DatabaseParameterGroup', {
            description: `PostgreSQL parameter group for ${projectName} ${environment}`,
            engine: rds.DatabaseInstanceEngine.postgres({
                version: rds.PostgresEngineVersion.VER_15_4
            }),
            parameters: {
                // Connection and authentication
                'max_connections': environment === 'production' ? '200' : '100',
                'shared_preload_libraries': 'pg_stat_statements',

                // Memory configuration
                'shared_buffers': environment === 'production' ? '256MB' : '128MB',
                'effective_cache_size': environment === 'production' ? '1GB' : '512MB',
                'work_mem': environment === 'production' ? '8MB' : '4MB',
                'maintenance_work_mem': environment === 'production' ? '128MB' : '64MB',

                // Checkpoint and WAL configuration
                'checkpoint_completion_target': '0.9',
                'wal_buffers': '16MB',
                'default_statistics_target': '100',

                // Query optimization
                'random_page_cost': '1.1',
                'effective_io_concurrency': '200',

                // Logging configuration for monitoring
                'log_statement': 'all',
                'log_duration': '1',
                'log_min_duration_statement': '1000',
                'log_checkpoints': '1',
                'log_connections': '1',
                'log_disconnections': '1',
                'log_lock_waits': '1',
                'log_temp_files': '0',

                // Performance insights
                'track_activity_query_size': '2048',
                'pg_stat_statements.track': 'all',
                'pg_stat_statements.max': '10000'
            }
        });

        cdk.Tags.of(parameterGroup).add('Name', `${projectName}-${environment}-postgres-params`);
        cdk.Tags.of(parameterGroup).add('Environment', environment);
        cdk.Tags.of(parameterGroup).add('Project', projectName);

        return parameterGroup;
    }

    private createDatabaseSecret(projectName: string, environment: string): secretsmanager.Secret {
        const secret = new secretsmanager.Secret(this, 'DatabaseSecret', {
            secretName: `${projectName}/${environment}/database/credentials`,
            description: `Database credentials for ${projectName} ${environment}`,
            generateSecretString: {
                secretStringTemplate: JSON.stringify({
                    username: 'genaidemo_admin',
                    dbname: 'genaidemo'
                }),
                generateStringKey: 'password',
                excludeCharacters: '"@/\\\'',
                includeSpace: false,
                passwordLength: 32
            },
            encryptionKey: this.kmsKey,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(secret).add('Name', `${projectName}-${environment}-db-secret`);
        cdk.Tags.of(secret).add('Environment', environment);
        cdk.Tags.of(secret).add('Project', projectName);

        return secret;
    }

    private createRdsInstance(
        vpc: ec2.IVpc,
        rdsSecurityGroup: ec2.ISecurityGroup,
        projectName: string,
        environment: string,
        envConfig: any,
        retentionPolicies: any
    ): rds.DatabaseInstance {
        // Get environment-specific configuration
        const instanceType = envConfig['rds-instance-type'] || 'db.t3.micro';
        const allocatedStorage = envConfig['rds-storage'] || 20;
        const multiAz = envConfig['rds-multi-az'] || false;
        const backupRetention = retentionPolicies['backups'] || 7;

        // Create CloudWatch log groups for database logs
        const postgresLogGroup = new logs.LogGroup(this, 'PostgresLogGroup', {
            logGroupName: `/aws/rds/instance/${projectName}-${environment}-postgres/postgresql`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        const upgradeLogGroup = new logs.LogGroup(this, 'UpgradeLogGroup', {
            logGroupName: `/aws/rds/instance/${projectName}-${environment}-postgres/upgrade`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        // Create the RDS instance
        const database = new rds.DatabaseInstance(this, 'PostgresDatabase', {
            instanceIdentifier: `${projectName}-${environment}-postgres`,
            engine: rds.DatabaseInstanceEngine.postgres({
                version: rds.PostgresEngineVersion.VER_15_4
            }),
            instanceType: ec2.InstanceType.of(
                this.getInstanceClass(instanceType),
                this.getInstanceSize(instanceType)
            ),

            // Network configuration
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_ISOLATED
            },
            securityGroups: [rdsSecurityGroup],
            subnetGroup: this.subnetGroup,

            // Database configuration
            credentials: rds.Credentials.fromSecret(this.databaseSecret),
            databaseName: 'genaidemo',
            parameterGroup: this.parameterGroup,

            // Storage configuration
            allocatedStorage: allocatedStorage,
            maxAllocatedStorage: allocatedStorage * 2, // Auto-scaling up to 2x
            storageType: rds.StorageType.GP3,
            storageEncrypted: true,
            storageEncryptionKey: this.kmsKey,

            // High availability and backup configuration
            multiAz: multiAz,
            backupRetention: cdk.Duration.days(backupRetention),
            deleteAutomatedBackups: environment !== 'production',
            deletionProtection: environment === 'production',

            // Maintenance and monitoring
            autoMinorVersionUpgrade: environment !== 'production',
            preferredBackupWindow: '03:00-04:00', // UTC
            preferredMaintenanceWindow: 'sun:04:00-sun:05:00', // UTC

            // Performance Insights
            enablePerformanceInsights: true,
            performanceInsightRetention: environment === 'production'
                ? rds.PerformanceInsightRetention.LONG_TERM
                : rds.PerformanceInsightRetention.DEFAULT,
            performanceInsightEncryptionKey: this.kmsKey,

            // Monitoring and logging
            monitoringInterval: cdk.Duration.seconds(60),
            cloudwatchLogsExports: ['postgresql', 'upgrade'],

            // Removal policy
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.SNAPSHOT
                : cdk.RemovalPolicy.DESTROY
        });

        // Add tags to the database instance
        cdk.Tags.of(database).add('Name', `${projectName}-${environment}-postgres`);
        cdk.Tags.of(database).add('Environment', environment);
        cdk.Tags.of(database).add('Project', projectName);
        cdk.Tags.of(database).add('Engine', 'PostgreSQL');
        cdk.Tags.of(database).add('Version', '15.4');
        cdk.Tags.of(database).add('MultiAZ', multiAz.toString());

        return database;
    }

    private getInstanceClass(instanceType: string): ec2.InstanceClass {
        const parts = instanceType.split('.');
        if (parts.length < 2) return ec2.InstanceClass.T3;

        const classMap: { [key: string]: ec2.InstanceClass } = {
            't3': ec2.InstanceClass.T3,
            't4g': ec2.InstanceClass.T4G,
            'm5': ec2.InstanceClass.M5,
            'm6g': ec2.InstanceClass.M6G,
            'r5': ec2.InstanceClass.R5,
            'r6g': ec2.InstanceClass.R6G
        };

        return classMap[parts[1]] || ec2.InstanceClass.T3;
    }

    private getInstanceSize(instanceType: string): ec2.InstanceSize {
        const parts = instanceType.split('.');
        if (parts.length < 3) return ec2.InstanceSize.MICRO;

        const sizeMap: { [key: string]: ec2.InstanceSize } = {
            'micro': ec2.InstanceSize.MICRO,
            'small': ec2.InstanceSize.SMALL,
            'medium': ec2.InstanceSize.MEDIUM,
            'large': ec2.InstanceSize.LARGE,
            'xlarge': ec2.InstanceSize.XLARGE,
            '2xlarge': ec2.InstanceSize.XLARGE2,
            '4xlarge': ec2.InstanceSize.XLARGE4
        };

        return sizeMap[parts[2]] || ec2.InstanceSize.MICRO;
    }

    private createMonitoringAndAlarms(projectName: string, environment: string): void {
        // Create SNS topic for database alerts
        const alertTopic = new sns.Topic(this, 'DatabaseAlertTopic', {
            topicName: `${projectName}-${environment}-rds-alerts`,
            displayName: `${projectName} ${environment} RDS Alerts`
        });

        // Determine which database resource to monitor
        const databaseResource = this.auroraCluster || this.database;
        if (!databaseResource) return;

        // CPU Utilization Alarm
        const cpuAlarm = new cloudwatch.Alarm(this, 'DatabaseCpuAlarm', {
            alarmName: `${projectName}-${environment}-${this.auroraCluster ? 'aurora' : 'rds'}-cpu-high`,
            alarmDescription: 'Database CPU utilization is high',
            metric: databaseResource.metricCPUUtilization({
                period: cdk.Duration.minutes(5),
                statistic: 'Average'
            }),
            threshold: environment === 'production' ? 80 : 90,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        cpuAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic));

        // Database Connection Alarm
        const connectionAlarm = new cloudwatch.Alarm(this, 'DatabaseConnectionAlarm', {
            alarmName: `${projectName}-${environment}-${this.auroraCluster ? 'aurora' : 'rds'}-connections-high`,
            alarmDescription: 'Database connection count is high',
            metric: databaseResource.metricDatabaseConnections({
                period: cdk.Duration.minutes(5),
                statistic: 'Average'
            }),
            threshold: environment === 'production' ? 160 : 80, // 80% of max_connections
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        connectionAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic));

        // Free Storage Space Alarm
        // Free Storage Space Alarm (only for RDS instances, not Aurora clusters)
        if (this.database) {
            const storageAlarm = new cloudwatch.Alarm(this, 'DatabaseStorageAlarm', {
                alarmName: `${projectName}-${environment}-rds-storage-low`,
                alarmDescription: 'Database free storage space is low',
                metric: this.database.metricFreeStorageSpace({
                    period: cdk.Duration.minutes(5),
                    statistic: 'Average'
                }),
                threshold: 2 * 1024 * 1024 * 1024, // 2 GB in bytes
                evaluationPeriods: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
            });

            storageAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic));
        }

        // Read/Write Latency Alarms
        const dbIdentifier = this.auroraCluster
            ? this.auroraCluster.clusterIdentifier
            : this.database?.instanceIdentifier || 'unknown';

        const namespace = this.auroraCluster ? 'AWS/RDS' : 'AWS/RDS';
        const dimensionKey = this.auroraCluster ? 'DBClusterIdentifier' : 'DBInstanceIdentifier';

        const readLatencyAlarm = new cloudwatch.Alarm(this, 'DatabaseReadLatencyAlarm', {
            alarmName: `${projectName}-${environment}-${this.auroraCluster ? 'aurora' : 'rds'}-read-latency-high`,
            alarmDescription: 'Database read latency is high',
            metric: new cloudwatch.Metric({
                namespace: namespace,
                metricName: 'ReadLatency',
                dimensionsMap: {
                    [dimensionKey]: dbIdentifier
                },
                period: cdk.Duration.minutes(5),
                statistic: 'Average'
            }),
            threshold: 0.2, // 200ms
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        readLatencyAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic));

        const writeLatencyAlarm = new cloudwatch.Alarm(this, 'DatabaseWriteLatencyAlarm', {
            alarmName: `${projectName}-${environment}-${this.auroraCluster ? 'aurora' : 'rds'}-write-latency-high`,
            alarmDescription: 'Database write latency is high',
            metric: new cloudwatch.Metric({
                namespace: namespace,
                metricName: 'WriteLatency',
                dimensionsMap: {
                    [dimensionKey]: dbIdentifier
                },
                period: cdk.Duration.minutes(5),
                statistic: 'Average'
            }),
            threshold: 0.2, // 200ms
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        writeLatencyAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic));

        // Aurora-specific monitoring
        if (this.auroraCluster) {
            // Aurora replica lag alarm
            const replicaLagAlarm = new cloudwatch.Alarm(this, 'AuroraReplicaLagAlarm', {
                alarmName: `${projectName}-${environment}-aurora-replica-lag-high`,
                alarmDescription: 'Aurora replica lag is high',
                metric: new cloudwatch.Metric({
                    namespace: 'AWS/RDS',
                    metricName: 'AuroraReplicaLag',
                    dimensionsMap: {
                        DBClusterIdentifier: this.auroraCluster.clusterIdentifier
                    },
                    period: cdk.Duration.minutes(5),
                    statistic: 'Average'
                }),
                threshold: 30000, // 30 seconds in milliseconds
                evaluationPeriods: 2,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
            });

            replicaLagAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic));
        }

        // Add tags to monitoring resources
        cdk.Tags.of(alertTopic).add('Name', `${projectName}-${environment}-rds-alerts`);
        cdk.Tags.of(alertTopic).add('Environment', environment);
        cdk.Tags.of(alertTopic).add('Project', projectName);
        cdk.Tags.of(alertTopic).add('Service', 'RDS-Monitoring');
    }

    private createParameterStoreParameters(projectName: string, environment: string, region: string): void {
        const parameterPrefix = `/genai-demo/${environment}/${region}/database`;

        // Get endpoint information from either Aurora cluster or RDS instance
        const endpoint = this.auroraCluster
            ? this.auroraCluster.clusterEndpoint
            : this.database?.instanceEndpoint;

        // Database endpoint parameter
        if (endpoint) {
            new ssm.StringParameter(this, 'DatabaseEndpointParameter', {
                parameterName: `${parameterPrefix}/endpoint`,
                stringValue: endpoint.hostname,
                description: `Database endpoint for ${projectName} ${environment}`,
                tier: ssm.ParameterTier.STANDARD,
                simpleName: false
            });

            // Database port parameter
            new ssm.StringParameter(this, 'DatabasePortParameter', {
                parameterName: `${parameterPrefix}/port`,
                stringValue: endpoint.port.toString(),
                description: `Database port for ${projectName} ${environment}`,
                tier: ssm.ParameterTier.STANDARD,
                simpleName: false
            });
        }

        // Aurora-specific parameters
        if (this.auroraCluster) {
            // Reader endpoint parameter
            new ssm.StringParameter(this, 'DatabaseReaderEndpointParameter', {
                parameterName: `${parameterPrefix}/reader-endpoint`,
                stringValue: this.auroraCluster.clusterReadEndpoint.hostname,
                description: `Database reader endpoint for ${projectName} ${environment}`,
                tier: ssm.ParameterTier.STANDARD,
                simpleName: false
            });

            // Global cluster identifier parameter
            if (this.globalCluster) {
                new ssm.StringParameter(this, 'GlobalClusterIdentifierParameter', {
                    parameterName: `${parameterPrefix}/global-cluster-id`,
                    stringValue: this.globalCluster.ref,
                    description: `Global cluster identifier for ${projectName} ${environment}`,
                    tier: ssm.ParameterTier.STANDARD,
                    simpleName: false
                });
            }
        }

        // Database name parameter
        new ssm.StringParameter(this, 'DatabaseNameParameter', {
            parameterName: `${parameterPrefix}/name`,
            stringValue: 'genaidemo',
            description: `Database name for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Database secret ARN parameter
        new ssm.StringParameter(this, 'DatabaseSecretArnParameter', {
            parameterName: `${parameterPrefix}/secret-arn`,
            stringValue: this.databaseSecret.secretArn,
            description: `Database secret ARN for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Database connection URL template parameter
        if (endpoint) {
            new ssm.StringParameter(this, 'DatabaseUrlTemplateParameter', {
                parameterName: `${parameterPrefix}/url-template`,
                stringValue: `jdbc:postgresql://${endpoint.hostname}:${endpoint.port}/genaidemo`,
                description: `Database URL template for ${projectName} ${environment}`,
                tier: ssm.ParameterTier.STANDARD,
                simpleName: false
            });
        }
    }

    private createOutputs(projectName: string, environment: string): void {
        // Determine database type and get appropriate identifiers
        const databaseType = this.auroraCluster ? 'aurora' : 'rds';
        const endpoint = this.auroraCluster
            ? this.auroraCluster.clusterEndpoint
            : this.database?.instanceEndpoint;
        const identifier = this.auroraCluster
            ? this.auroraCluster.clusterIdentifier
            : this.database?.instanceIdentifier || 'unknown';

        // Database instance/cluster outputs
        new cdk.CfnOutput(this, 'DatabaseInstanceId', {
            value: identifier,
            description: `${databaseType.toUpperCase()} PostgreSQL ${this.auroraCluster ? 'cluster' : 'instance'} identifier`,
            exportName: `${projectName}-${environment}-${databaseType}-instance-id`
        });

        if (endpoint) {
            new cdk.CfnOutput(this, 'DatabaseEndpoint', {
                value: endpoint.hostname,
                description: `${databaseType.toUpperCase()} PostgreSQL endpoint hostname`,
                exportName: `${projectName}-${environment}-${databaseType}-endpoint`
            });

            new cdk.CfnOutput(this, 'DatabasePort', {
                value: endpoint.port.toString(),
                description: `${databaseType.toUpperCase()} PostgreSQL port`,
                exportName: `${projectName}-${environment}-${databaseType}-port`
            });
        }

        // Aurora-specific outputs
        if (this.auroraCluster) {
            new cdk.CfnOutput(this, 'DatabaseReaderEndpoint', {
                value: this.auroraCluster.clusterReadEndpoint.hostname,
                description: 'Aurora PostgreSQL reader endpoint hostname',
                exportName: `${projectName}-${environment}-aurora-reader-endpoint`
            });

            if (this.globalCluster) {
                new cdk.CfnOutput(this, 'GlobalClusterIdentifier', {
                    value: this.globalCluster.ref,
                    description: 'Aurora Global Cluster identifier',
                    exportName: `${projectName}-${environment}-global-cluster-id`
                });
            }
        }

        new cdk.CfnOutput(this, 'DatabaseName', {
            value: 'genaidemo',
            description: 'RDS PostgreSQL database name',
            exportName: `${projectName}-${environment}-rds-database-name`
        });

        // Security and credentials outputs
        new cdk.CfnOutput(this, 'DatabaseSecretArn', {
            value: this.databaseSecret.secretArn,
            description: 'ARN of the database credentials secret',
            exportName: `${projectName}-${environment}-rds-secret-arn`
        });

        new cdk.CfnOutput(this, 'DatabaseSecretName', {
            value: this.databaseSecret.secretName,
            description: 'Name of the database credentials secret',
            exportName: `${projectName}-${environment}-rds-secret-name`
        });

        new cdk.CfnOutput(this, 'DatabaseKmsKeyId', {
            value: this.kmsKey.keyId,
            description: 'KMS key ID for database encryption',
            exportName: `${projectName}-${environment}-rds-kms-key-id`
        });

        new cdk.CfnOutput(this, 'DatabaseKmsKeyArn', {
            value: this.kmsKey.keyArn,
            description: 'KMS key ARN for database encryption',
            exportName: `${projectName}-${environment}-rds-kms-key-arn`
        });

        // Configuration outputs
        new cdk.CfnOutput(this, 'DatabaseParameterGroupName', {
            value: this.parameterGroup.node.defaultChild ?
                (this.parameterGroup.node.defaultChild as rds.CfnDBParameterGroup).ref :
                `${projectName}-${environment}-postgres-params`,
            description: 'Database parameter group name',
            exportName: `${projectName}-${environment}-rds-parameter-group`
        });

        new cdk.CfnOutput(this, 'DatabaseSubnetGroupName', {
            value: this.subnetGroup.node.defaultChild ?
                (this.subnetGroup.node.defaultChild as rds.CfnDBSubnetGroup).ref :
                `${projectName}-${environment}-db-subnet-group`,
            description: 'Database subnet group name',
            exportName: `${projectName}-${environment}-rds-subnet-group`
        });

        // Connection information for applications
        if (endpoint) {
            new cdk.CfnOutput(this, 'DatabaseConnectionUrl', {
                value: `jdbc:postgresql://${endpoint.hostname}:${endpoint.port}/genaidemo`,
                description: 'JDBC connection URL for the database',
                exportName: `${projectName}-${environment}-${databaseType}-connection-url`
            });

            // Spring Boot configuration helper
            new cdk.CfnOutput(this, 'SpringBootDatabaseConfig', {
                value: JSON.stringify({
                    'spring.datasource.url': `jdbc:postgresql://${endpoint.hostname}:${endpoint.port}/genaidemo`,
                    'spring.datasource.driver-class-name': 'org.postgresql.Driver',
                    'spring.jpa.database-platform': 'org.hibernate.dialect.PostgreSQLDialect',
                    'spring.jpa.hibernate.ddl-auto': 'validate',
                    'spring.flyway.locations': 'classpath:db/migration/postgresql',
                    ...(this.auroraCluster && {
                        'spring.datasource.hikari.read-only-url': `jdbc:postgresql://${this.auroraCluster.clusterReadEndpoint.hostname}:${this.auroraCluster.clusterReadEndpoint.port}/genaidemo`
                    })
                }),
                description: 'Spring Boot database configuration properties',
                exportName: `${projectName}-${environment}-spring-db-config`
            });
        }
    }
}