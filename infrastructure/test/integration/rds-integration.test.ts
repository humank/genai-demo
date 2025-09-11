import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { GenAIDemoInfrastructureStack } from '../../lib/infrastructure-stack';

describe('RDS Integration Tests', () => {
    let app: cdk.App;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:environment': 'test',
                'genai-demo:project-name': 'genai-demo',
                'genai-demo:environments': {
                    'test': {
                        'vpc-cidr': '10.0.0.0/16',
                        'nat-gateways': 1,
                        'rds-instance-type': 'db.t3.micro',
                        'rds-storage': 20,
                        'rds-multi-az': false,
                        'retention-policies': {
                            'backups': 7
                        }
                    }
                },
                'genai-demo:regions': {
                    'primary': 'ap-northeast-1'
                }
            }
        });
    });

    test('should integrate RDS stack with main infrastructure stack', () => {
        // Given
        const stack = new GenAIDemoInfrastructureStack(app, 'TestInfraStack', {
            environment: 'test',
            projectName: 'genai-demo'
        });

        // When
        const template = Template.fromStack(stack);

        // Then - Verify main infrastructure components exist
        template.hasResourceProperties('AWS::EC2::VPC', {
            CidrBlock: '10.0.0.0/16'
        });

        template.hasResourceProperties('AWS::ElasticLoadBalancingV2::LoadBalancer', {
            Type: 'application'
        });

        // Then - Verify RDS stack is created and integrated
        expect(stack.rdsStack).toBeDefined();
        expect(stack.rdsStack.database).toBeDefined();
        expect(stack.rdsStack.databaseSecret).toBeDefined();
        expect(stack.rdsStack.kmsKey).toBeDefined();
    });

    test('should create RDS database with correct VPC integration', () => {
        // Given
        const stack = new GenAIDemoInfrastructureStack(app, 'TestInfraStack', {
            environment: 'test',
            projectName: 'genai-demo'
        });

        // When
        const rdsTemplate = Template.fromStack(stack.rdsStack);

        // Then - Verify RDS instance is created
        rdsTemplate.hasResourceProperties('AWS::RDS::DBInstance', {
            DBInstanceIdentifier: 'genai-demo-test-postgres',
            Engine: 'postgres',
            EngineVersion: '15.4',
            DBInstanceClass: 'db.t3.micro',
            AllocatedStorage: '20',
            StorageEncrypted: true,
            MultiAZ: false,
            DBName: 'genaidemo'
        });

        // Then - Verify database subnet group uses isolated subnets
        rdsTemplate.hasResourceProperties('AWS::RDS::DBSubnetGroup', {
            DBSubnetGroupDescription: 'Database subnet group for genai-demo test'
        });

        // Then - Verify KMS key for encryption
        rdsTemplate.hasResourceProperties('AWS::KMS::Key', {
            Description: 'KMS key for genai-demo test RDS database encryption',
            EnableKeyRotation: true
        });

        // Then - Verify secrets manager integration
        rdsTemplate.hasResourceProperties('AWS::SecretsManager::Secret', {
            Name: 'genai-demo/test/database/credentials',
            Description: 'Database credentials for genai-demo test'
        });
    });

    test('should create proper security group integration', () => {
        // Given
        const stack = new GenAIDemoInfrastructureStack(app, 'TestInfraStack', {
            environment: 'test',
            projectName: 'genai-demo'
        });

        // When
        const mainTemplate = Template.fromStack(stack);

        // Then - Verify RDS security group exists in main stack
        mainTemplate.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for RDS PostgreSQL database'
        });

        // Then - Verify EKS security group exists
        mainTemplate.hasResourceProperties('AWS::EC2::SecurityGroup', {
            GroupDescription: 'Security group for EKS cluster and worker nodes'
        });
    });

    test('should create monitoring and alerting for RDS', () => {
        // Given
        const stack = new GenAIDemoInfrastructureStack(app, 'TestInfraStack', {
            environment: 'test',
            projectName: 'genai-demo'
        });

        // When
        const rdsTemplate = Template.fromStack(stack.rdsStack);

        // Then - Verify CloudWatch alarms are created
        rdsTemplate.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-cpu-high',
            MetricName: 'CPUUtilization',
            Namespace: 'AWS/RDS'
        });

        rdsTemplate.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-connections-high',
            MetricName: 'DatabaseConnections'
        });

        rdsTemplate.hasResourceProperties('AWS::CloudWatch::Alarm', {
            AlarmName: 'genai-demo-test-rds-storage-low',
            MetricName: 'FreeStorageSpace'
        });

        // Then - Verify SNS topic for alerts
        rdsTemplate.hasResourceProperties('AWS::SNS::Topic', {
            TopicName: 'genai-demo-test-rds-alerts'
        });
    });

    test('should create Parameter Store integration', () => {
        // Given
        const stack = new GenAIDemoInfrastructureStack(app, 'TestInfraStack', {
            environment: 'test',
            projectName: 'genai-demo'
        });

        // When
        const rdsTemplate = Template.fromStack(stack.rdsStack);

        // Then - Verify Parameter Store parameters are created
        rdsTemplate.hasResourceProperties('AWS::SSM::Parameter', {
            Description: 'Database endpoint for genai-demo test',
            Type: 'String',
            Tier: 'Standard'
        });

        rdsTemplate.hasResourceProperties('AWS::SSM::Parameter', {
            Description: 'Database port for genai-demo test',
            Type: 'String',
            Tier: 'Standard'
        });

        rdsTemplate.hasResourceProperties('AWS::SSM::Parameter', {
            Description: 'Database name for genai-demo test',
            Type: 'String',
            Value: 'genaidemo',
            Tier: 'Standard'
        });

        rdsTemplate.hasResourceProperties('AWS::SSM::Parameter', {
            Description: 'Database secret ARN for genai-demo test',
            Type: 'String',
            Tier: 'Standard'
        });

        rdsTemplate.hasResourceProperties('AWS::SSM::Parameter', {
            Description: 'Database URL template for genai-demo test',
            Type: 'String',
            Tier: 'Standard'
        });
    });

    test('should create proper stack outputs for cross-stack references', () => {
        // Given
        const stack = new GenAIDemoInfrastructureStack(app, 'TestInfraStack', {
            environment: 'test',
            projectName: 'genai-demo'
        });

        // When
        const mainTemplate = Template.fromStack(stack);
        const rdsTemplate = Template.fromStack(stack.rdsStack);

        // Then - Verify main stack has RDS stack reference
        mainTemplate.hasOutput('RdsStackName', {
            Description: 'RDS stack name'
        });

        // Then - Verify RDS stack has proper outputs
        rdsTemplate.hasOutput('DatabaseInstanceId', {
            Description: 'RDS PostgreSQL instance identifier',
            Export: {
                Name: 'genai-demo-test-rds-instance-id'
            }
        });

        rdsTemplate.hasOutput('DatabaseEndpoint', {
            Description: 'RDS PostgreSQL endpoint hostname',
            Export: {
                Name: 'genai-demo-test-rds-endpoint'
            }
        });

        rdsTemplate.hasOutput('DatabaseSecretArn', {
            Description: 'ARN of the database credentials secret',
            Export: {
                Name: 'genai-demo-test-rds-secret-arn'
            }
        });

        rdsTemplate.hasOutput('SpringBootDatabaseConfig', {
            Description: 'Spring Boot database configuration properties',
            Export: {
                Name: 'genai-demo-test-spring-db-config'
            }
        });
    });

    test('should handle production environment with enhanced configuration', () => {
        // Given
        const prodApp = new cdk.App({
            context: {
                'genai-demo:environment': 'production',
                'genai-demo:project-name': 'genai-demo',
                'genai-demo:environments': {
                    'production': {
                        'vpc-cidr': '10.2.0.0/16',
                        'nat-gateways': 3,
                        'rds-instance-type': 'db.r6g.large',
                        'rds-storage': 100,
                        'rds-multi-az': true,
                        'retention-policies': {
                            'backups': 30
                        }
                    }
                },
                'genai-demo:regions': {
                    'primary': 'ap-east-2'
                }
            }
        });

        const stack = new GenAIDemoInfrastructureStack(prodApp, 'ProdInfraStack', {
            environment: 'production',
            projectName: 'genai-demo'
        });

        // When
        const rdsTemplate = Template.fromStack(stack.rdsStack);

        // Then - Verify production-specific configuration
        rdsTemplate.hasResourceProperties('AWS::RDS::DBInstance', {
            DBInstanceClass: 'db.r6g.large',
            AllocatedStorage: '100',
            MultiAZ: true,
            BackupRetentionPeriod: 30,
            DeletionProtection: true,
            DeleteAutomatedBackups: false
        });

        // Then - Verify production parameter group settings
        rdsTemplate.hasResourceProperties('AWS::RDS::DBParameterGroup', {
            Parameters: {
                'max_connections': '200',
                'shared_buffers': '256MB',
                'effective_cache_size': '1GB',
                'work_mem': '8MB',
                'maintenance_work_mem': '128MB'
            }
        });
    });
});