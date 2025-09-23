import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { RdsStack } from '../src/stacks/rds-stack';

describe('RDS Stack', () => {
    let app: cdk.App;
    let vpc: ec2.Vpc;
    let rdsSecurityGroup: ec2.SecurityGroup;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:environments': {
                    'test': {
                        'rds-instance-type': 'db.t3.micro',
                        'rds-storage': 20,
                        'rds-multi-az': false,
                        'retention-policies': {
                            'backups': 7
                        }
                    },
                    'production': {
                        'rds-instance-type': 'db.r6g.large',
                        'rds-storage': 100,
                        'rds-multi-az': true,
                        'retention-policies': {
                            'backups': 30
                        }
                    }
                }
            }
        });

        // Create a test VPC
        const vpcStack = new cdk.Stack(app, 'TestVpcStack');
        vpc = new ec2.Vpc(vpcStack, 'TestVpc', {
            maxAzs: 3,
            subnetConfiguration: [
                {
                    cidrMask: 24,
                    name: 'Public',
                    subnetType: ec2.SubnetType.PUBLIC,
                },
                {
                    cidrMask: 24,
                    name: 'Private',
                    subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
                },
                {
                    cidrMask: 24,
                    name: 'Database',
                    subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
                }
            ]
        });

        rdsSecurityGroup = new ec2.SecurityGroup(vpcStack, 'TestRdsSecurityGroup', {
            vpc: vpc,
            description: 'Test RDS security group'
        });
    });

    test('should create RDS PostgreSQL instance with correct configuration', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::RDS::DBInstance', {
            DBInstanceIdentifier: 'genai-demo-test-postgres',
            Engine: 'postgres',
            EngineVersion: '15.4',
            DBInstanceClass: 'db.t3.micro',
            AllocatedStorage: '20',
            MaxAllocatedStorage: 40,
            StorageType: 'gp3',
            StorageEncrypted: true,
            MultiAZ: false,
            DBName: 'genaidemo',
            BackupRetentionPeriod: 7,
            DeleteAutomatedBackups: true,
            DeletionProtection: false,
            AutoMinorVersionUpgrade: true,
            PreferredBackupWindow: '03:00-04:00',
            PreferredMaintenanceWindow: 'sun:04:00-sun:05:00',
            EnablePerformanceInsights: true,
            MonitoringInterval: 60,
            EnableCloudwatchLogsExports: ['postgresql', 'upgrade']
        });
    });

    test('should create production RDS instance with Multi-AZ and enhanced configuration', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStackProd', {
            environment: 'production',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::RDS::DBInstance', {
            DBInstanceIdentifier: 'genai-demo-production-postgres',
            Engine: 'postgres',
            EngineVersion: '15.4',
            DBInstanceClass: 'db.r6g.large',
            AllocatedStorage: '100',
            MaxAllocatedStorage: 200,
            StorageType: 'gp3',
            StorageEncrypted: true,
            MultiAZ: true,
            DBName: 'genaidemo',
            BackupRetentionPeriod: 30,
            DeleteAutomatedBackups: false,
            DeletionProtection: true,
            AutoMinorVersionUpgrade: false,
            EnablePerformanceInsights: true,
            MonitoringInterval: 60
        });
    });

    test('should create KMS key for database encryption', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::KMS::Key', {
            Description: 'KMS key for genai-demo test RDS database encryption',
            EnableKeyRotation: true,
            KeySpec: 'SYMMETRIC_DEFAULT',
            KeyUsage: 'ENCRYPT_DECRYPT'
        });

        template.hasResourceProperties('AWS::KMS::Alias', {
            AliasName: 'alias/genai-demo-test-rds-key'
        });
    });

    test('should create database credentials secret', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::SecretsManager::Secret', {
            Name: 'genai-demo/test/database/credentials',
            Description: 'Database credentials for genai-demo test',
            GenerateSecretString: {
                SecretStringTemplate: JSON.stringify({
                    username: 'genaidemo_admin',
                    dbname: 'genaidemo'
                }),
                GenerateStringKey: 'password',
                ExcludeCharacters: '"@/\\\'',
                IncludeSpace: false,
                PasswordLength: 32
            }
        });
    });

    test('should create parameter group with optimized PostgreSQL settings', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::RDS::DBParameterGroup', {
            Description: 'PostgreSQL parameter group for genai-demo test',
            Family: 'postgres15',
            Parameters: Match.objectLike({
                'max_connections': '100',
                'shared_preload_libraries': 'pg_stat_statements',
                'shared_buffers': '128MB',
                'effective_cache_size': '512MB',
                'work_mem': '4MB',
                'maintenance_work_mem': '64MB',
                'checkpoint_completion_target': '0.9',
                'wal_buffers': '16MB',
                'default_statistics_target': '100',
                'random_page_cost': '1.1',
                'effective_io_concurrency': '200',
                'log_statement': 'all',
                'log_duration': '1',
                'log_min_duration_statement': '1000',
                'log_checkpoints': '1',
                'log_connections': '1',
                'log_disconnections': '1',
                'log_lock_waits': '1',
                'log_temp_files': '0',
                'track_activity_query_size': '2048',
                'pg_stat_statements.track': 'all',
                'pg_stat_statements.max': '10000'
            })
        });
    });

    test('should create production parameter group with enhanced settings', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStackProd', {
            environment: 'production',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::RDS::DBParameterGroup', {
            Parameters: Match.objectLike({
                'max_connections': '200',
                'shared_buffers': '256MB',
                'effective_cache_size': '1GB',
                'work_mem': '8MB',
                'maintenance_work_mem': '128MB'
            })
        });
    });

    test('should create database subnet group in isolated subnets', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::RDS::DBSubnetGroup', {
            DBSubnetGroupDescription: 'Database subnet group for genai-demo test',
            SubnetIds: Match.anyValue()
        });
    });

    test('should create CloudWatch alarms for database monitoring', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        // CPU Utilization Alarm
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-cpu-high',
            AlarmDescription: 'Database CPU utilization is high',
            MetricName: 'CPUUtilization',
            Namespace: 'AWS/RDS',
            Statistic: 'Average',
            Threshold: 90,
            ComparisonOperator: 'GreaterThanThreshold',
            EvaluationPeriods: 2
        });

        // Database Connections Alarm
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-connections-high',
            AlarmDescription: 'Database connection count is high',
            MetricName: 'DatabaseConnections',
            Threshold: 80
        });

        // Free Storage Space Alarm
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-storage-low',
            AlarmDescription: 'Database free storage space is low',
            MetricName: 'FreeStorageSpace',
            Threshold: 2147483648 // 2 GB in bytes
        });

        // Read Latency Alarm
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-read-latency-high',
            AlarmDescription: 'Database read latency is high',
            MetricName: 'ReadLatency',
            Threshold: 0.2
        });

        // Write Latency Alarm
        template.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-write-latency-high',
            AlarmDescription: 'Database write latency is high',
            MetricName: 'WriteLatency',
            Threshold: 0.2
        });
    });

    test('should create SNS topic for database alerts', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::SNS::Topic', {
            TopicName: 'genai-demo-test-rds-alerts',
            DisplayName: 'genai-demo test RDS Alerts'
        });
    });

    test('should create Parameter Store parameters for database connection', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::SSM::Parameter', {
            Name: '/genai-demo/test/ap-northeast-1/database/endpoint',
            Type: 'String',
            Description: 'Database endpoint for genai-demo test'
        });

        template.hasResourceProperties('AWS::SSM::Parameter', {
            Name: '/genai-demo/test/ap-northeast-1/database/port',
            Type: 'String',
            Description: 'Database port for genai-demo test'
        });

        template.hasResourceProperties('AWS::SSM::Parameter', {
            Name: '/genai-demo/test/ap-northeast-1/database/name',
            Type: 'String',
            Value: 'genaidemo',
            Description: 'Database name for genai-demo test'
        });

        template.hasResourceProperties('AWS::SSM::Parameter', {
            Name: '/genai-demo/test/ap-northeast-1/database/secret-arn',
            Type: 'String',
            Description: 'Database secret ARN for genai-demo test'
        });
    });

    test('should create CloudWatch log groups for database logs', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/rds/instance/genai-demo-test-postgres/postgresql',
            RetentionInDays: 7
        });

        template.hasResourceProperties('AWS::Logs::LogGroup', {
            LogGroupName: '/aws/rds/instance/genai-demo-test-postgres/upgrade',
            RetentionInDays: 7
        });
    });

    test('should create proper stack outputs', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then
        template.hasOutput('DatabaseInstanceId', {
            Description: 'RDS PostgreSQL instance identifier',
            Export: {
                Name: 'genai-demo-test-rds-instance-id'
            }
        });

        template.hasOutput('DatabaseEndpoint', {
            Description: 'RDS PostgreSQL endpoint hostname',
            Export: {
                Name: 'genai-demo-test-rds-endpoint'
            }
        });

        template.hasOutput('DatabasePort', {
            Description: 'RDS PostgreSQL port',
            Export: {
                Name: 'genai-demo-test-rds-port'
            }
        });

        template.hasOutput('DatabaseSecretArn', {
            Description: 'ARN of the database credentials secret',
            Export: {
                Name: 'genai-demo-test-rds-secret-arn'
            }
        });

        template.hasOutput('SpringBootDatabaseConfig', {
            Description: 'Spring Boot database configuration properties',
            Export: {
                Name: 'genai-demo-test-spring-db-config'
            }
        });
    });

    test('should apply proper tags to all resources', () => {
        // Given
        const stack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Check that RDS instance has the Name tag
        template.hasResourceProperties('AWS::RDS::DBInstance', {
            Tags: Match.arrayWith([
                { Key: 'Name', Value: 'genai-demo-test-postgres' }
            ])
        });

        // Check that SNS topic has proper tags
        template.hasResourceProperties('AWS::SNS::Topic', {
            Tags: Match.arrayWith([
                { Key: 'Name', Value: 'genai-demo-test-rds-alerts' }
            ])
        });
    });

    test('should have different removal policies for production vs non-production', () => {
        // Given - Test environment
        const testStack = new RdsStack(app, 'TestRdsStack', {
            environment: 'test',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // Given - Production environment
        const prodStack = new RdsStack(app, 'ProdRdsStack', {
            environment: 'production',
            projectName: 'genai-demo',
            vpc: vpc,
            rdsSecurityGroup: rdsSecurityGroup,
            region: 'ap-northeast-1'
        });

        // When
        const testTemplate = Template.fromStack(testStack);
        const prodTemplate = Template.fromStack(prodStack);

        // Then - Test environment should have destroy policy
        testTemplate.hasResourceProperties('AWS::RDS::DBInstance', {
            DeletionProtection: false,
            DeleteAutomatedBackups: true
        });

        // Then - Production environment should have retain/snapshot policy
        prodTemplate.hasResourceProperties('AWS::RDS::DBInstance', {
            DeletionProtection: true,
            DeleteAutomatedBackups: false
        });
    });
});