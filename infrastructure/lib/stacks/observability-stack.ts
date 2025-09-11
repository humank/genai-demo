import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as destinations from 'aws-cdk-lib/aws-logs-destinations';
import * as opensearch from 'aws-cdk-lib/aws-opensearchservice';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface ObservabilityStackProps extends cdk.StackProps {
    vpc: ec2.Vpc;
    environment: string;
    projectName: string;
    region?: string;
}

export class ObservabilityStack extends cdk.Stack {
    public readonly openSearchDomain: opensearch.Domain;
    public readonly logsBucket: s3.Bucket;
    public readonly applicationLogGroup: logs.LogGroup;
    public readonly infrastructureLogGroup: logs.LogGroup;

    // SNS Topics for different alert levels
    public readonly alertingTopic: sns.Topic;
    public readonly warningTopic: sns.Topic;
    public readonly infoTopic: sns.Topic;

    // Additional resources
    public readonly logArchiveBucket: s3.Bucket;
    public readonly dataLakeBucket: s3.Bucket;
    public readonly observabilityRole: iam.Role;
    public readonly logGroups: logs.LogGroup[];
    public readonly xrayTracingEnabled: boolean;
    public readonly eventBridgeRules: events.Rule[];
    public readonly cloudWatchAlarms: cloudwatch.Alarm[];

    constructor(scope: Construct, id: string, props: ObservabilityStackProps) {
        super(scope, id, props);

        // Initialize arrays and flags
        this.xrayTracingEnabled = true;
        this.eventBridgeRules = [];
        this.cloudWatchAlarms = [];

        // Create SNS Topics for different alert levels
        this.alertingTopic = this.createAlertingTopic(props.environment);
        this.warningTopic = this.createWarningTopic(props.environment);
        this.infoTopic = this.createInfoTopic(props.environment);

        // Create IAM Role for observability services
        this.observabilityRole = this.createObservabilityRole(props.environment);

        // Create S3 buckets
        this.logsBucket = this.createLogsBucket(props.environment);
        this.logArchiveBucket = this.createLogArchiveBucket(props.environment);
        this.dataLakeBucket = this.createDataLakeBucket(props.environment);

        // Create CloudWatch Log Groups
        this.applicationLogGroup = this.createApplicationLogGroup(props.environment);
        this.infrastructureLogGroup = this.createInfrastructureLogGroup(props.environment);
        this.logGroups = [this.applicationLogGroup, this.infrastructureLogGroup];

        // Create OpenSearch domain for log analysis
        this.openSearchDomain = this.createOpenSearchDomain(props.vpc, props.environment);

        // Create log forwarding Lambda function
        const logForwardingFunction = this.createLogForwardingFunction(props.environment);

        // Set up log streaming from CloudWatch to OpenSearch
        this.setupLogStreaming(logForwardingFunction);

        // Create automated log lifecycle management
        this.setupLogLifecycleManagement(props.environment);

        // Output important resources
        this.createOutputs();
    }

    private createLogsBucket(environment: string): s3.Bucket {
        return new s3.Bucket(this, 'LogsBucket', {
            bucketName: `genai-demo-logs-${environment}-${this.account}`,
            versioned: false,
            publicReadAccess: false,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            encryption: s3.BucketEncryption.S3_MANAGED,
            lifecycleRules: [
                {
                    id: 'LogsLifecycleRule',
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
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });
    }

    private createApplicationLogGroup(environment: string): logs.LogGroup {
        return new logs.LogGroup(this, 'ApplicationLogGroup', {
            logGroupName: `/aws/containerinsights/genai-demo-cluster/application`,
            retention: logs.RetentionDays.ONE_WEEK, // Short retention in CloudWatch, longer in S3
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });
    }

    private createInfrastructureLogGroup(environment: string): logs.LogGroup {
        return new logs.LogGroup(this, 'InfrastructureLogGroup', {
            logGroupName: `/aws/containerinsights/genai-demo-cluster/dataplane`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });
    }

    private createOpenSearchDomain(vpc: ec2.Vpc, environment: string): opensearch.Domain {
        // Create security group for OpenSearch
        const openSearchSecurityGroup = new ec2.SecurityGroup(this, 'OpenSearchSecurityGroup', {
            vpc: vpc,
            description: 'Security group for OpenSearch domain',
            allowAllOutbound: true
        });

        // Allow HTTPS access from VPC
        openSearchSecurityGroup.addIngressRule(
            ec2.Peer.ipv4(vpc.vpcCidrBlock),
            ec2.Port.tcp(443),
            'Allow HTTPS access from VPC'
        );

        // Get observability configuration from context
        const observabilityConfig = this.node.tryGetContext('genai-demo:observability') || {};
        const openSearchConfig = observabilityConfig.opensearch?.[environment] || observabilityConfig.opensearch?.development || {};

        // Determine configuration based on environment-specific settings
        const isMultiAzEnvironment = openSearchConfig['multi-az'] || false;
        const instanceType = openSearchConfig['instance-type'] || 't3.small.search';
        const instanceCount = openSearchConfig['instance-count'] || 1;
        const volumeSize = openSearchConfig['volume-size'] || 20;

        // Create OpenSearch domain
        const domain = new opensearch.Domain(this, 'LogsOpenSearchDomain', {
            version: opensearch.EngineVersion.OPENSEARCH_2_11,
            domainName: `genai-demo-logs-${environment}`,

            // Capacity configuration based on environment-specific settings
            capacity: {
                masterNodes: isMultiAzEnvironment ? Math.max(3, instanceCount) : 1,
                masterNodeInstanceType: isMultiAzEnvironment ? 'm6g.medium.search' : instanceType,
                dataNodes: instanceCount,
                dataNodeInstanceType: instanceType
            },

            // EBS configuration
            ebs: {
                volumeSize: volumeSize,
                volumeType: ec2.EbsDeviceVolumeType.GP3
            },

            // VPC configuration
            vpc: vpc,
            vpcSubnets: [{ subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }],
            securityGroups: [openSearchSecurityGroup],

            // Zone awareness - only enable for Multi-AZ compatible instances
            zoneAwareness: {
                enabled: isMultiAzEnvironment,
                availabilityZoneCount: isMultiAzEnvironment ? 3 : undefined
            },

            // Logging configuration
            logging: {
                slowSearchLogEnabled: true,
                appLogEnabled: true,
                slowIndexLogEnabled: true
            },

            // Node to node encryption
            nodeToNodeEncryption: true,
            encryptionAtRest: {
                enabled: true
            },

            // Domain endpoint options
            enforceHttps: true,
            tlsSecurityPolicy: opensearch.TLSSecurityPolicy.TLS_1_2,

            // Access policy - allow access from Lambda and VPC
            accessPolicies: [
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    principals: [new iam.AnyPrincipal()],
                    actions: ['es:*'],
                    resources: [`arn:aws:es:${this.region}:${this.account}:domain/genai-demo-logs-${environment}/*`],
                    conditions: {
                        IpAddress: {
                            'aws:SourceIp': [vpc.vpcCidrBlock]
                        }
                    }
                })
            ],

            removalPolicy: environment === 'production' ? cdk.RemovalPolicy.RETAIN : cdk.RemovalPolicy.DESTROY
        });

        return domain;
    }

    private createLogForwardingFunction(environment: string): lambda.Function {
        // Create IAM role for Lambda function
        const lambdaRole = new iam.Role(this, 'LogForwardingLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
            ],
            inlinePolicies: {
                OpenSearchAccess: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'es:ESHttpPost',
                                'es:ESHttpPut',
                                'es:ESHttpGet'
                            ],
                            resources: [this.openSearchDomain.domainArn + '/*']
                        })
                    ]
                }),
                CloudWatchLogsAccess: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'logs:CreateLogGroup',
                                'logs:CreateLogStream',
                                'logs:PutLogEvents'
                            ],
                            resources: ['*']
                        })
                    ]
                })
            }
        });

        // Create Lambda function for log forwarding
        const logForwardingFunction = new lambda.Function(this, 'LogForwardingFunction', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            role: lambdaRole,
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            environment: {
                OPENSEARCH_ENDPOINT: this.openSearchDomain.domainEndpoint,
                ENVIRONMENT: environment
            },
            code: lambda.Code.fromInline(`
import json
import gzip
import base64
import boto3
import urllib3
from datetime import datetime
import os

# Initialize OpenSearch client
http = urllib3.PoolManager()
opensearch_endpoint = os.environ['OPENSEARCH_ENDPOINT']
environment = os.environ['ENVIRONMENT']

def handler(event, context):
    """
    Lambda function to forward CloudWatch Logs to OpenSearch
    """
    try:
        # Decode and decompress the log data
        cw_data = event['awslogs']['data']
        compressed_payload = base64.b64decode(cw_data)
        uncompressed_payload = gzip.decompress(compressed_payload)
        log_data = json.loads(uncompressed_payload)
        
        # Process each log event
        for log_event in log_data['logEvents']:
            # Parse the log message
            message = log_event['message']
            timestamp = datetime.fromtimestamp(log_event['timestamp'] / 1000)
            
            # Try to parse JSON structured logs
            try:
                parsed_message = json.loads(message)
                log_document = {
                    '@timestamp': timestamp.isoformat(),
                    'environment': environment,
                    'log_group': log_data['logGroup'],
                    'log_stream': log_data['logStream'],
                    **parsed_message
                }
            except json.JSONDecodeError:
                # Handle non-JSON logs
                log_document = {
                    '@timestamp': timestamp.isoformat(),
                    'environment': environment,
                    'log_group': log_data['logGroup'],
                    'log_stream': log_data['logStream'],
                    'message': message
                }
            
            # Create index name based on date
            index_name = f"genai-demo-logs-{timestamp.strftime('%Y-%m-%d')}"
            
            # Send to OpenSearch
            url = f"https://{opensearch_endpoint}/{index_name}/_doc"
            headers = {'Content-Type': 'application/json'}
            
            response = http.request(
                'POST',
                url,
                body=json.dumps(log_document),
                headers=headers
            )
            
            if response.status != 201:
                print(f"Failed to index log: {response.status} - {response.data}")
        
        return {
            'statusCode': 200,
            'body': json.dumps('Logs forwarded successfully')
        }
        
    except Exception as e:
        print(f"Error processing logs: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }
      `)
        });

        return logForwardingFunction;
    }

    private setupLogStreaming(logForwardingFunction: lambda.Function): void {
        // Create subscription filter for application logs
        new logs.SubscriptionFilter(this, 'ApplicationLogsSubscription', {
            logGroup: this.applicationLogGroup,
            destination: new destinations.LambdaDestination(logForwardingFunction),
            filterPattern: logs.FilterPattern.allEvents()
        });

        // Create subscription filter for infrastructure logs
        new logs.SubscriptionFilter(this, 'InfrastructureLogsSubscription', {
            logGroup: this.infrastructureLogGroup,
            destination: new destinations.LambdaDestination(logForwardingFunction),
            filterPattern: logs.FilterPattern.allEvents()
        });
    }

    private setupLogLifecycleManagement(environment: string): void {
        // Create Lambda function for log export to S3
        const logExportRole = new iam.Role(this, 'LogExportLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
            ],
            inlinePolicies: {
                LogsExportAccess: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'logs:CreateExportTask',
                                'logs:DescribeExportTasks'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                's3:PutObject',
                                's3:GetBucketAcl'
                            ],
                            resources: [
                                this.logsBucket.bucketArn,
                                this.logsBucket.bucketArn + '/*'
                            ]
                        })
                    ]
                })
            }
        });

        const logExportFunction = new lambda.Function(this, 'LogExportFunction', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            role: logExportRole,
            timeout: cdk.Duration.minutes(15),
            environment: {
                LOGS_BUCKET: this.logsBucket.bucketName,
                APPLICATION_LOG_GROUP: this.applicationLogGroup.logGroupName,
                INFRASTRUCTURE_LOG_GROUP: this.infrastructureLogGroup.logGroupName
            },
            code: lambda.Code.fromInline(`
import boto3
import json
from datetime import datetime, timedelta
import os

logs_client = boto3.client('logs')
s3_bucket = os.environ['LOGS_BUCKET']
app_log_group = os.environ['APPLICATION_LOG_GROUP']
infra_log_group = os.environ['INFRASTRUCTURE_LOG_GROUP']

def handler(event, context):
    """
    Export CloudWatch Logs to S3 for long-term storage
    """
    try:
        # Calculate date range for export (7 days ago)
        end_time = datetime.now() - timedelta(days=7)
        start_time = end_time - timedelta(days=1)
        
        # Convert to timestamps
        from_time = int(start_time.timestamp() * 1000)
        to_time = int(end_time.timestamp() * 1000)
        
        # Export application logs
        app_export_response = logs_client.create_export_task(
            logGroupName=app_log_group,
            fromTime=from_time,
            to=to_time,
            destination=s3_bucket,
            destinationPrefix=f'application-logs/{start_time.strftime("%Y/%m/%d")}'
        )
        
        # Export infrastructure logs
        infra_export_response = logs_client.create_export_task(
            logGroupName=infra_log_group,
            fromTime=from_time,
            to=to_time,
            destination=s3_bucket,
            destinationPrefix=f'infrastructure-logs/{start_time.strftime("%Y/%m/%d")}'
        )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'application_export_task': app_export_response['taskId'],
                'infrastructure_export_task': infra_export_response['taskId']
            })
        }
        
    except Exception as e:
        print(f"Error exporting logs: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }
      `)
        });

        // Create EventBridge rule to trigger log export daily
        const logExportRule = new events.Rule(this, 'LogExportRule', {
            schedule: events.Schedule.cron({
                minute: '0',
                hour: '2', // Run at 2 AM daily
                day: '*',
                month: '*',
                year: '*'
            }),
            description: 'Daily log export to S3'
        });

        logExportRule.addTarget(new targets.LambdaFunction(logExportFunction));
    }

    private createOutputs(): void {
        new cdk.CfnOutput(this, 'OpenSearchDomainEndpoint', {
            value: this.openSearchDomain.domainEndpoint,
            description: 'OpenSearch domain endpoint for log analysis'
        });

        new cdk.CfnOutput(this, 'OpenSearchDashboardsUrl', {
            value: `https://${this.openSearchDomain.domainEndpoint}/_dashboards/`,
            description: 'OpenSearch Dashboards URL'
        });

        new cdk.CfnOutput(this, 'LogsBucketName', {
            value: this.logsBucket.bucketName,
            description: 'S3 bucket for log archival'
        });

        new cdk.CfnOutput(this, 'ApplicationLogGroupName', {
            value: this.applicationLogGroup.logGroupName,
            description: 'CloudWatch Log Group for application logs'
        });

        new cdk.CfnOutput(this, 'InfrastructureLogGroupName', {
            value: this.infrastructureLogGroup.logGroupName,
            description: 'CloudWatch Log Group for infrastructure logs'
        });
    }

    private createAlertingTopic(environment: string): sns.Topic {
        return new sns.Topic(this, 'AlertingTopic', {
            topicName: `genai-demo-${environment}-alerting`,
            displayName: `GenAI Demo ${environment} Alerting Topic`
        });
    }

    private createWarningTopic(environment: string): sns.Topic {
        return new sns.Topic(this, 'WarningTopic', {
            topicName: `genai-demo-${environment}-warning`,
            displayName: `GenAI Demo ${environment} Warning Topic`
        });
    }

    private createInfoTopic(environment: string): sns.Topic {
        return new sns.Topic(this, 'InfoTopic', {
            topicName: `genai-demo-${environment}-info`,
            displayName: `GenAI Demo ${environment} Info Topic`
        });
    }

    private createObservabilityRole(environment: string): iam.Role {
        return new iam.Role(this, 'ObservabilityRole', {
            roleName: `genai-demo-${environment}-observability-role`,
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchLogsFullAccess'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonS3FullAccess')
            ]
        });
    }

    private createLogArchiveBucket(environment: string): s3.Bucket {
        return new s3.Bucket(this, 'LogArchiveBucket', {
            bucketName: `genai-demo-${environment}-log-archive-${this.account}`,
            versioned: true,
            encryption: s3.BucketEncryption.S3_MANAGED,
            lifecycleRules: [{
                id: 'LogArchiveLifecycle',
                enabled: true,
                transitions: [{
                    storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                    transitionAfter: cdk.Duration.days(30)
                }, {
                    storageClass: s3.StorageClass.GLACIER,
                    transitionAfter: cdk.Duration.days(90)
                }, {
                    storageClass: s3.StorageClass.DEEP_ARCHIVE,
                    transitionAfter: cdk.Duration.days(365)
                }],
                expiration: cdk.Duration.days(2555) // 7 years
            }],
            publicReadAccess: false,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL
        });
    }

    private createDataLakeBucket(environment: string): s3.Bucket {
        return new s3.Bucket(this, 'DataLakeBucket', {
            bucketName: `genai-demo-${environment}-data-lake-${this.account}`,
            versioned: true,
            encryption: s3.BucketEncryption.S3_MANAGED,
            lifecycleRules: [{
                id: 'DataLakeLifecycle',
                enabled: true,
                transitions: [{
                    storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                    transitionAfter: cdk.Duration.days(30)
                }, {
                    storageClass: s3.StorageClass.GLACIER,
                    transitionAfter: cdk.Duration.days(90)
                }]
            }],
            publicReadAccess: false,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL
        });
    }
}