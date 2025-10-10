import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as glue from 'aws-cdk-lib/aws-glue';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface DataCatalogStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly auroraCluster: rds.IDatabaseCluster;
    readonly databaseSecret: secretsmanager.ISecret;
    readonly rdsSecurityGroup: ec2.ISecurityGroup;
}

/**
 * AWS Glue Data Catalog Stack for GenAI Demo
 * 
 * This stack creates:
 * - AWS Glue Database for automated schema discovery
 * - Glue Crawler with daily schedule and real-time triggers
 * - Aurora PostgreSQL connection configuration
 * - CloudWatch monitoring and SNS alerts
 * - Automatic detection of schema changes across 13 bounded contexts
 * 
 * Requirements: 2.1 - Data architecture governance mechanism
 */
export class DataCatalogStack extends cdk.Stack {
    public readonly glueDatabase: glue.CfnDatabase;
    public readonly glueCrawler: glue.CfnCrawler;
    public readonly auroraConnection: glue.CfnConnection;
    public readonly crawlerRole: iam.Role;
    public readonly triggerFunction: lambda.Function;
    public readonly monitoringDashboard: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: DataCatalogStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            vpc,
            auroraCluster,
            databaseSecret,
            rdsSecurityGroup
        } = props;

        // Create alert topic for data catalog monitoring
        const alertTopic = new sns.Topic(this, 'DataCatalogAlertTopic', {
            displayName: 'Data Catalog Alert Topic',
            topicName: `${projectName}-${environment}-data-catalog-alerts`
        });

        // Apply common tags
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'DataCatalog',
            Service: 'Glue'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Create IAM role for Glue Crawler
        this.crawlerRole = this.createGlueCrawlerRole(projectName, environment);

        // Create Glue Database
        this.glueDatabase = this.createGlueDatabase(projectName, environment);

        // Create Aurora connection for Glue
        this.auroraConnection = this.createAuroraConnection(
            projectName,
            environment,
            vpc,
            auroraCluster,
            databaseSecret,
            rdsSecurityGroup
        );

        // Create Glue Crawler with automated discovery
        this.glueCrawler = this.createGlueCrawler(
            projectName,
            environment,
            this.crawlerRole,
            this.glueDatabase,
            this.auroraConnection
        );

        // Create Lambda function for real-time crawler triggering
        this.triggerFunction = this.createCrawlerTriggerFunction(
            projectName,
            environment,
            this.glueCrawler
        );

        // Set up real-time change detection
        this.setupRealTimeDiscovery(auroraCluster, this.triggerFunction);

        // Create monitoring dashboard
        this.monitoringDashboard = this.createMonitoringDashboard(
            projectName,
            environment,
            this.glueCrawler,
            alertTopic
        );

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment);
    }

    /**
     * Create IAM role for Glue Crawler with necessary permissions
     */
    private createGlueCrawlerRole(projectName: string, environment: string): iam.Role {
        const role = new iam.Role(this, 'GlueCrawlerRole', {
            roleName: `${projectName}-${environment}-glue-crawler-role`,
            assumedBy: new iam.ServicePrincipal('glue.amazonaws.com'),
            description: 'IAM role for AWS Glue Crawler to access Aurora PostgreSQL and manage Data Catalog',
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSGlueServiceRole')
            ]
        });

        // Add permissions for Aurora access
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'AuroraAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'rds:DescribeDBClusters',
                'rds:DescribeDBInstances',
                'rds:DescribeDBSubnetGroups'
            ],
            resources: ['*']
        }));

        // Add permissions for VPC access
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'VPCAccess',
            effect: iam.Effect.ALLOW,
            actions: [
                'ec2:CreateNetworkInterface',
                'ec2:DeleteNetworkInterface',
                'ec2:DescribeNetworkInterfaces',
                'ec2:DescribeVpcs',
                'ec2:DescribeSubnets',
                'ec2:DescribeSecurityGroups'
            ],
            resources: ['*']
        }));

        // Add permissions for CloudWatch logging
        role.addToPolicy(new iam.PolicyStatement({
            sid: 'CloudWatchLogs',
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents'
            ],
            resources: [`arn:aws:logs:${this.region}:${this.account}:log-group:/aws-glue/*`]
        }));

        return role;
    }

    /**
     * Create Glue Database for genai-demo catalog
     */
    private createGlueDatabase(projectName: string, environment: string): glue.CfnDatabase {
        return new glue.CfnDatabase(this, 'GenAIDemoDatabase', {
            catalogId: this.account,
            databaseInput: {
                name: `${projectName}_${environment}_catalog`,
                description: `Auto-discovered schema catalog for ${projectName} ${environment} application`,
                parameters: {
                    'classification': 'postgresql',
                    'typeOfData': 'relational',
                    'created_by': 'aws-glue-crawler',
                    'auto_discovery': 'true',
                    'bounded_contexts': '13',
                    'project': projectName,
                    'environment': environment
                }
            }
        });
    }

    /**
     * Create Aurora PostgreSQL connection for Glue
     */
    private createAuroraConnection(
        projectName: string,
        environment: string,
        vpc: ec2.IVpc,
        auroraCluster: rds.IDatabaseCluster,
        databaseSecret: secretsmanager.ISecret,
        rdsSecurityGroup: ec2.ISecurityGroup
    ): glue.CfnConnection {
        return new glue.CfnConnection(this, 'AuroraConnection', {
            catalogId: this.account,
            connectionInput: {
                name: `${projectName}-${environment}-aurora-connection`,
                description: `Connection to ${projectName} ${environment} Aurora PostgreSQL cluster for schema discovery`,
                connectionType: 'JDBC',
                connectionProperties: {
                    'JDBC_CONNECTION_URL': cdk.Fn.sub('jdbc:postgresql://${ClusterEndpoint}:5432/genaidemo', {
                        'ClusterEndpoint': (auroraCluster as any).clusterEndpoint?.hostname || 'aurora-cluster-endpoint'
                    }),
                    'USERNAME': databaseSecret.secretValueFromJson('username').unsafeUnwrap(),
                    'PASSWORD': databaseSecret.secretValueFromJson('password').unsafeUnwrap()
                },
                physicalConnectionRequirements: {
                    availabilityZone: vpc.availabilityZones[0],
                    securityGroupIdList: [rdsSecurityGroup.securityGroupId],
                    subnetId: vpc.privateSubnets[0].subnetId
                }
            }
        });
    }

    /**
     * Create Glue Crawler with automated schema discovery
     */
    private createGlueCrawler(
        projectName: string,
        environment: string,
        crawlerRole: iam.Role,
        glueDatabase: glue.CfnDatabase,
        auroraConnection: glue.CfnConnection
    ): glue.CfnCrawler {
        return new glue.CfnCrawler(this, 'GenAIDemoAuroraCrawler', {
            name: `${projectName}-${environment}-aurora-auto-discovery`,
            role: crawlerRole.roleArn,
            databaseName: glueDatabase.ref,
            description: 'Automatically discovers and catalogs all tables in GenAI Demo Aurora database across 13 bounded contexts',
            
            targets: {
                jdbcTargets: [{
                    connectionName: auroraConnection.ref,
                    path: 'genaidemo/%', // Scan all tables in genaidemo database
                    exclusions: [
                        'genaidemo/flyway_schema_history',  // Exclude Flyway management table
                        'genaidemo/information_schema/%',   // Exclude PostgreSQL system tables
                        'genaidemo/pg_catalog/%',           // Exclude PostgreSQL catalog tables
                        'genaidemo/pg_toast/%'              // Exclude PostgreSQL TOAST tables
                    ]
                }]
            },

            // Schema change handling policy
            schemaChangePolicy: {
                updateBehavior: 'UPDATE_IN_DATABASE',  // Automatically update existing table structures
                deleteBehavior: 'LOG'                  // Log but don't delete removed tables from catalog
            },

            // Recrawl policy
            recrawlPolicy: {
                recrawlBehavior: 'CRAWL_EVERYTHING'    // Complete scan every time for accuracy
            },

            // Daily schedule - run at 2 AM to avoid business hours
            schedule: {
                scheduleExpression: 'cron(0 2 * * ? *)'
            },

            // Configuration for optimal discovery
            configuration: JSON.stringify({
                "Version": 1.0,
                "CrawlerOutput": {
                    "Partitions": { "AddOrUpdateBehavior": "InheritFromTable" },
                    "Tables": { "AddOrUpdateBehavior": "MergeNewColumns" }
                },
                "Grouping": {
                    "TableGroupingPolicy": "CombineCompatibleSchemas"
                }
            })
        });
    }

    /**
     * Create Lambda function to trigger crawler on RDS events
     */
    private createCrawlerTriggerFunction(
        projectName: string,
        environment: string,
        glueCrawler: glue.CfnCrawler
    ): lambda.Function {
        const triggerFunction = new lambda.Function(this, 'CrawlerTriggerFunction', {
            functionName: `${projectName}-${environment}-glue-crawler-trigger`,
            runtime: lambda.Runtime.PYTHON_3_9,
            handler: 'trigger_crawler.handler',
            timeout: cdk.Duration.minutes(5),
            description: 'Triggers Glue Crawler when Aurora schema changes are detected',
            environment: {
                'CRAWLER_NAME': glueCrawler.ref,
                'PROJECT_NAME': projectName,
                'ENVIRONMENT': environment
            },
            code: lambda.Code.fromInline(`
import boto3
import json
import logging
import os

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

def handler(event, context):
    """
    Triggers AWS Glue Crawler when RDS events indicate schema changes
    """
    glue = boto3.client('glue')
    crawler_name = os.environ['CRAWLER_NAME']
    
    try:
        logger.info(f"Received event: {json.dumps(event)}")
        
        # Check if this is an RDS event that indicates schema changes
        if is_schema_change_event(event):
            logger.info(f"Schema change detected, checking crawler status")
            
            # Get crawler status
            response = glue.get_crawler(Name=crawler_name)
            state = response['Crawler']['State']
            
            if state == 'READY':
                # Start crawler
                glue.start_crawler(Name=crawler_name)
                logger.info(f"Started crawler {crawler_name}")
                
                return {
                    'statusCode': 200,
                    'body': json.dumps({
                        'message': f'Crawler {crawler_name} started successfully',
                        'event_type': 'schema_change_detected'
                    })
                }
            else:
                logger.info(f"Crawler {crawler_name} is in state {state}, skipping")
                return {
                    'statusCode': 200,
                    'body': json.dumps({
                        'message': f'Crawler {crawler_name} is busy, state: {state}',
                        'event_type': 'crawler_busy'
                    })
                }
        else:
            logger.info("Event does not indicate schema change, ignoring")
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': 'Event ignored - no schema change detected',
                    'event_type': 'ignored'
                })
            }
            
    except Exception as e:
        logger.error(f"Error processing event: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': str(e),
                'event_type': 'error'
            })
        }

def is_schema_change_event(event):
    """
    Determines if the RDS event indicates a potential schema change
    """
    try:
        # Check if this is an RDS event
        if event.get('source') != 'aws.rds':
            return False
            
        # Check event categories that might indicate schema changes
        detail = event.get('detail', {})
        event_categories = detail.get('EventCategories', [])
        
        # Schema change indicators
        schema_change_categories = [
            'configuration change',
            'creation',
            'deletion',
            'maintenance'
        ]
        
        return any(category in event_categories for category in schema_change_categories)
        
    except Exception as e:
        logger.error(f"Error checking event type: {str(e)}")
        return False
            `)
        });

        // Grant permissions to start Glue Crawler
        triggerFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'glue:GetCrawler',
                'glue:StartCrawler'
            ],
            resources: [
                `arn:aws:glue:${this.region}:${this.account}:crawler/${glueCrawler.ref}`
            ]
        }));

        return triggerFunction;
    }

    /**
     * Set up real-time discovery using RDS events
     */
    private setupRealTimeDiscovery(
        auroraCluster: rds.IDatabaseCluster,
        triggerFunction: lambda.Function
    ): void {
        // Create EventBridge rule for RDS events
        const rdsEventRule = new events.Rule(this, 'RDSSchemaChangeRule', {
            ruleName: `${this.stackName}-rds-schema-change-detection`,
            description: 'Trigger Glue Crawler when Aurora schema changes are detected',
            eventPattern: {
                source: ['aws.rds'],
                detailType: ['RDS DB Instance Event', 'RDS DB Cluster Event'],
                detail: {
                    'EventCategories': [
                        'configuration change',
                        'creation',
                        'deletion',
                        'maintenance'
                    ],
                    'SourceId': [(auroraCluster as any).clusterIdentifier || 'aurora-cluster']
                }
            }
        });

        // Add Lambda function as target
        rdsEventRule.addTarget(new targets.LambdaFunction(triggerFunction));
    }

    /**
     * Create CloudWatch monitoring dashboard
     */
    private createMonitoringDashboard(
        projectName: string,
        environment: string,
        glueCrawler: glue.CfnCrawler,
        alertTopic: sns.ITopic
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'DataCatalogDashboard', {
            dashboardName: `${projectName}-${environment}-data-catalog-monitoring`,
            defaultInterval: cdk.Duration.hours(24)
        });

        // Glue Crawler execution metrics
        const crawlerSuccessMetric = new cloudwatch.Metric({
            namespace: 'AWS/Glue',
            metricName: 'glue.driver.aggregate.numCompletedTasks',
            dimensionsMap: {
                'JobName': glueCrawler.ref,
                'JobRunId': 'ALL'
            },
            statistic: 'Sum',
            period: cdk.Duration.minutes(5)
        });

        const crawlerFailureMetric = new cloudwatch.Metric({
            namespace: 'AWS/Glue',
            metricName: 'glue.driver.aggregate.numFailedTasks',
            dimensionsMap: {
                'JobName': glueCrawler.ref,
                'JobRunId': 'ALL'
            },
            statistic: 'Sum',
            period: cdk.Duration.minutes(5)
        });

        // Add widgets to dashboard
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Glue Crawler Execution Status',
                left: [crawlerSuccessMetric],
                right: [crawlerFailureMetric],
                width: 12,
                height: 6
            })
        );

        // Create alarms
        const crawlerFailureAlarm = new cloudwatch.Alarm(this, 'CrawlerFailureAlarm', {
            alarmName: `${projectName}-${environment}-glue-crawler-failure`,
            metric: crawlerFailureMetric,
            threshold: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
            evaluationPeriods: 1,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            alarmDescription: 'Glue Crawler failed to complete successfully'
        });

        // Add SNS notification
        crawlerFailureAlarm.addAlarmAction(new cloudwatchActions.SnsAction(alertTopic as sns.Topic));

        return dashboard;
    }

    /**
     * Create CloudFormation outputs
     */
    private createOutputs(projectName: string, environment: string): void {
        new cdk.CfnOutput(this, 'GlueDatabaseName', {
            value: this.glueDatabase.ref,
            description: 'AWS Glue Database name for data catalog',
            exportName: `${projectName}-${environment}-glue-database-name`
        });

        new cdk.CfnOutput(this, 'GlueCrawlerName', {
            value: this.glueCrawler.ref,
            description: 'AWS Glue Crawler name for automated schema discovery',
            exportName: `${projectName}-${environment}-glue-crawler-name`
        });

        new cdk.CfnOutput(this, 'AuroraConnectionName', {
            value: this.auroraConnection.ref,
            description: 'AWS Glue connection name for Aurora PostgreSQL',
            exportName: `${projectName}-${environment}-aurora-connection-name`
        });

        new cdk.CfnOutput(this, 'DataCatalogDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.monitoringDashboard.dashboardName}`,
            description: 'CloudWatch Dashboard URL for Data Catalog monitoring'
        });
    }
}