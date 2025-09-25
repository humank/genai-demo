import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as grafana from 'aws-cdk-lib/aws-grafana';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface ObservabilityStackProps extends cdk.StackProps {
    vpc: ec2.Vpc;
    kmsKey: kms.Key;
    eksCluster?: any; // Optional EKS cluster reference for Container Insights
    environment?: string; // Environment name for resource naming
}

export class ObservabilityStack extends cdk.Stack {
    public readonly logGroup: logs.LogGroup;
    public readonly dashboard: cloudwatch.Dashboard;
    public xrayRole: iam.Role;
    public grafanaWorkspace: grafana.CfnWorkspace;
    public containerInsightsRole: iam.Role;

    constructor(scope: Construct, id: string, props: ObservabilityStackProps) {
        super(scope, id, props);

        const environment = props.environment || 'development';

        // Create CloudWatch Log Group
        this.logGroup = new logs.LogGroup(this, 'ApplicationLogGroup', {
            logGroupName: `/aws/genai-demo/application`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create CloudWatch Dashboard
        this.dashboard = new cloudwatch.Dashboard(this, 'ApplicationDashboard', {
            dashboardName: `GenAI-Demo-${environment}`,
        });

        // Add basic widgets to dashboard
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# GenAI Demo Monitoring Dashboard\n\nEnvironment: ${environment}\n\n## AWS Native Concurrency Monitoring System\n\n- **CloudWatch Container Insights**: EKS cluster monitoring\n- **X-Ray Distributed Tracing**: Request chain tracking\n- **Amazon Managed Grafana**: Unified monitoring dashboard\n- **Spring Boot Actuator**: Application metrics export`,
                width: 24,
                height: 4,
            })
        );

        // Configure CloudWatch Container Insights
        this.configureContainerInsights(environment);

        // Configure X-Ray distributed tracing
        this.configureXRayTracing(environment);

        // Set up Amazon Managed Grafana
        this.setupManagedGrafana(environment);

        // Add concurrency monitoring widgets
        this.addConcurrencyMonitoringWidgets(environment);

        // Add Aurora PostgreSQL deadlock monitoring widgets
        this.addAuroraDeadlockMonitoringWidgets();

        // Create automated log analysis for deadlock detection
        this.createDeadlockLogAnalysis();

        // Outputs
        this.createOutputs(environment);
    }

    /**
     * Add Aurora PostgreSQL deadlock monitoring widgets to the dashboard
     */
    private addAuroraDeadlockMonitoringWidgets(): void {
        const dbInstanceIdentifier = `genai-demo-${this.stackName}-primary-aurora`;

        // Aurora PostgreSQL Deadlocks Widget
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Aurora PostgreSQL - Deadlocks',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'Deadlocks',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Deadlock Count',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Database Connections and Lock Contention Widget
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Aurora PostgreSQL - Connections & Performance',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'DatabaseConnections',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Active Connections',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'ReadLatency',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Read Latency (s)',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'WriteLatency',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Write Latency (s)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // CPU and Memory Utilization Widget
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Aurora PostgreSQL - Resource Utilization',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'CPUUtilization',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'CPU Utilization (%)',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'FreeableMemory',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Freeable Memory (Bytes)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Performance Insights Widget (Text widget with link)
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## Performance Insights
                
**Aurora PostgreSQL Performance Insights** provides detailed database performance monitoring:

- **Lock Analysis**: View lock trees and blocked sessions
- **Wait Events**: Monitor Lock:transactionid, Lock:relation, Lock:tuple events  
- **Top SQL**: Identify queries causing deadlocks
- **Database Load**: Track db.Concurrency.deadlocks metric

[Open Performance Insights Console](https://console.aws.amazon.com/rds/home?region=${this.region}#performance-insights-v20206:)

**Key Metrics to Monitor:**
- \`db.Concurrency.deadlocks\` - Deadlocks per minute
- \`db.Locks.num_blocked_sessions\` - Blocked sessions count
- \`db.Transactions.blocked_transactions\` - Blocked transactions count`,
                width: 24,
                height: 8,
            })
        );
    }

    /**
     * Create automated log analysis for Aurora PostgreSQL deadlock detection
     */
    private createDeadlockLogAnalysis(): void {
        // Create Lambda function for log analysis
        const deadlockAnalysisFunction = new lambda.Function(this, 'DeadlockAnalysisFunction', {
            runtime: lambda.Runtime.PYTHON_3_9,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta

def handler(event, context):
    logs_client = boto3.client('logs')
    cloudwatch = boto3.client('cloudwatch')
    
    log_group_name = os.environ['LOG_GROUP_NAME']
    
    try:
        # Start Log Insights query for deadlock detection
        query_response = logs_client.start_query(
            logGroupName=log_group_name,
            startTime=int((datetime.now() - timedelta(minutes=15)).timestamp()),
            endTime=int(datetime.now().timestamp()),
            queryString='''
                fields @timestamp, @message
                | filter @message like /deadlock/i
                | filter @message like /ERROR/i or @message like /FATAL/i
                | stats count() as deadlock_count by bin(5m)
                | sort @timestamp desc
            '''
        )
        
        query_id = query_response['queryId']
        
        # Wait for query to complete
        import time
        time.sleep(10)
        
        results = logs_client.get_query_results(queryId=query_id)
        
        deadlock_count = 0
        if results['results']:
            for result in results['results']:
                for field in result:
                    if field['field'] == 'deadlock_count':
                        deadlock_count += int(field['value'])
        
        # Send custom metric to CloudWatch
        cloudwatch.put_metric_data(
            Namespace='Custom/Aurora/PostgreSQL',
            MetricData=[
                {
                    'MetricName': 'DeadlockLogCount',
                    'Value': deadlock_count,
                    'Unit': 'Count',
                    'Timestamp': datetime.now(),
                    'Dimensions': [
                        {
                            'Name': 'Environment',
                            'Value': os.environ['ENVIRONMENT']
                        }
                    ]
                }
            ]
        )
        
        # If deadlocks detected, create additional analysis
        if deadlock_count > 0:
            detailed_query = logs_client.start_query(
                logGroupName=log_group_name,
                startTime=int((datetime.now() - timedelta(minutes=15)).timestamp()),
                endTime=int(datetime.now().timestamp()),
                queryString='''
                    fields @timestamp, @message
                    | filter @message like /deadlock/i
                    | filter @message like /ERROR/i or @message like /FATAL/i
                    | sort @timestamp desc
                    | limit 10
                '''
            )
            
            print(f"Deadlocks detected: {deadlock_count}")
            print(f"Detailed analysis query ID: {detailed_query['queryId']}")
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'deadlock_count': deadlock_count,
                'query_id': query_id
            })
        }
        
    except Exception as e:
        print(f"Error analyzing logs: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }
            `),
            timeout: cdk.Duration.minutes(2),
            environment: {
                'LOG_GROUP_NAME': `/aws/rds/instance/genai-demo-${this.stackName}-primary-aurora/postgresql`,
                'ENVIRONMENT': this.stackName,
            },
        });

        // Grant permissions to the Lambda function
        deadlockAnalysisFunction.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'logs:StartQuery',
                    'logs:GetQueryResults',
                    'logs:DescribeLogGroups',
                    'logs:DescribeLogStreams',
                ],
                resources: [
                    `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/rds/instance/*`,
                ],
            })
        );

        deadlockAnalysisFunction.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'cloudwatch:PutMetricData',
                ],
                resources: ['*'],
            })
        );

        // Create CloudWatch Events rule to trigger the Lambda every 15 minutes
        const deadlockAnalysisRule = new events.Rule(this, 'DeadlockAnalysisRule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(15)),
            description: 'Analyze Aurora PostgreSQL logs for deadlocks every 15 minutes',
        });

        deadlockAnalysisRule.addTarget(new eventsTargets.LambdaFunction(deadlockAnalysisFunction));

        // Note: CloudWatch Log Insights queries can be created manually in the console
        // Query for deadlock analysis: 
        // fields @timestamp, @message | filter @message like /deadlock/i | filter @message like /ERROR/i or @message like /FATAL/i | sort @timestamp desc | limit 50
        
        // Query for lock contention analysis:
        // fields @timestamp, @message | filter @message like /lock/i and (@message like /wait/i or @message like /timeout/i) | filter @message like /ERROR/i or @message like /WARNING/i | stats count() by bin(5m) | sort @timestamp desc

        // Output the Lambda function ARN
        new cdk.CfnOutput(this, 'DeadlockAnalysisFunctionArn', {
            value: deadlockAnalysisFunction.functionArn,
            description: 'ARN of the deadlock analysis Lambda function',
            exportName: `${this.stackName}-deadlock-analysis-function-arn`,
        });
    }

    /**
     * Configure CloudWatch Container Insights for EKS cluster monitoring
     */
    private configureContainerInsights(environment: string): void {
        // Create IAM role for Container Insights
        this.containerInsightsRole = new iam.Role(this, 'ContainerInsightsRole', {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchLogsFullAccess'),
            ],
        });

        // Add custom policy for Container Insights
        this.containerInsightsRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'ec2:DescribeVolumes',
                'ec2:DescribeTags',
                'logs:PutLogEvents',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:DescribeLogStreams',
                'logs:DescribeLogGroups',
                'cloudwatch:PutMetricData',
            ],
            resources: ['*'],
        }));

        // Create log group for Container Insights
        const containerInsightsLogGroup = new logs.LogGroup(this, 'ContainerInsightsLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/performance`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create log group for application logs
        const applicationLogGroup = new logs.LogGroup(this, 'EKSApplicationLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/application`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create log group for dataplane logs
        const dataplaneLogGroup = new logs.LogGroup(this, 'EKSDataplaneLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/dataplane`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create log group for host logs
        const hostLogGroup = new logs.LogGroup(this, 'EKSHostLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/host`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });
    }

    /**
     * Configure X-Ray distributed tracing integration
     */
    private configureXRayTracing(environment: string): void {
        // Create IAM role for X-Ray daemon
        this.xrayRole = new iam.Role(this, 'XRayRole', {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AWSXRayDaemonWriteAccess'),
            ],
        });

        // Add additional permissions for X-Ray
        this.xrayRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'xray:PutTraceSegments',
                'xray:PutTelemetryRecords',
                'xray:GetSamplingRules',
                'xray:GetSamplingTargets',
                'xray:GetSamplingStatisticSummaries',
            ],
            resources: ['*'],
        }));

        // Create X-Ray sampling rule for the application
        const samplingRule = new lambda.CfnFunction(this, 'XRaySamplingRuleFunction', {
            runtime: 'python3.9',
            handler: 'index.handler',
            code: {
                zipFile: `
import boto3
import json

def handler(event, context):
    xray = boto3.client('xray')
    
    try:
        # Create sampling rule for GenAI Demo application
        response = xray.create_sampling_rule(
            SamplingRule={
                'RuleName': 'GenAIDemoSamplingRule',
                'Priority': 9000,
                'FixedRate': 0.1,
                'ReservoirSize': 1,
                'ServiceName': 'genai-demo',
                'ServiceType': '*',
                'Host': '*',
                'HTTPMethod': '*',
                'URLPath': '*',
                'Version': 1
            }
        )
        return {
            'statusCode': 200,
            'body': json.dumps('Sampling rule created successfully')
        }
    except Exception as e:
        if 'RuleAlreadyExistsException' in str(e):
            return {
                'statusCode': 200,
                'body': json.dumps('Sampling rule already exists')
            }
        else:
            print(f"Error creating sampling rule: {str(e)}")
            return {
                'statusCode': 500,
                'body': json.dumps(f'Error: {str(e)}')
            }
                `,
            },
            role: this.xrayRole.roleArn,
            timeout: 60,
        });
    }

    /**
     * Set up Amazon Managed Grafana workspace
     */
    private setupManagedGrafana(environment: string): void {
        // Create IAM role for Grafana
        const grafanaRole = new iam.Role(this, 'GrafanaRole', {
            assumedBy: new iam.ServicePrincipal('grafana.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchReadOnlyAccess'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AWSXRayReadOnlyAccess'),
            ],
        });

        // Add additional permissions for Grafana
        grafanaRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
                'logs:GetLogEvents',
                'logs:StartQuery',
                'logs:StopQuery',
                'logs:GetQueryResults',
                'cloudwatch:DescribeAlarmsForMetric',
                'cloudwatch:DescribeAlarmHistory',
                'cloudwatch:DescribeAlarms',
                'cloudwatch:ListMetrics',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:GetMetricData',
                'tag:GetResources',
            ],
            resources: ['*'],
        }));

        // Create Amazon Managed Grafana workspace
        this.grafanaWorkspace = new grafana.CfnWorkspace(this, 'GrafanaWorkspace', {
            accountAccessType: 'CURRENT_ACCOUNT',
            authenticationProviders: ['AWS_SSO'],
            permissionType: 'SERVICE_MANAGED',
            name: `genai-demo-${environment}`,
            description: `GenAI Demo AWS Native Concurrency Monitoring System for ${environment} environment`,
            dataSources: ['CLOUDWATCH', 'XRAY', 'PROMETHEUS'],
            notificationDestinations: ['SNS'],
            organizationRoleName: grafanaRole.roleName,
            roleArn: grafanaRole.roleArn,
            stackSetName: `genai-demo-grafana-${environment}`,
            // Enhanced configuration for concurrency monitoring
            configuration: JSON.stringify({
                unifiedAlerting: {
                    enabled: true,
                },
                plugins: {
                    pluginAdminEnabled: true,
                },
            }),
        });

        // Create Prometheus workspace for KEDA metrics using CloudFormation
        const prometheusWorkspace = new cdk.CfnResource(this, 'PrometheusWorkspace', {
            type: 'AWS::APS::Workspace',
            properties: {
                Alias: `genai-demo-${environment}-prometheus`,
                Tags: [
                    {
                        Key: 'Environment',
                        Value: environment,
                    },
                    {
                        Key: 'Project',
                        Value: 'genai-demo',
                    },
                    {
                        Key: 'Component',
                        Value: 'Monitoring',
                    },
                ],
            },
        });

        // Output Prometheus workspace details
        new cdk.CfnOutput(this, 'PrometheusWorkspaceId', {
            value: prometheusWorkspace.getAtt('WorkspaceId').toString(),
            exportName: `${environment}-PrometheusWorkspaceId`,
        });

        new cdk.CfnOutput(this, 'PrometheusEndpoint', {
            value: prometheusWorkspace.getAtt('PrometheusEndpoint').toString(),
            exportName: `${environment}-PrometheusEndpoint`,
        });
    }

    /**
     * Add concurrency monitoring widgets to the dashboard
     */
    private addConcurrencyMonitoringWidgets(environment: string): void {
        // EKS Container Insights Metrics
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'EKS Cluster - Container Insights',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'ContainerInsights',
                        metricName: 'cluster_cpu_utilization',
                        dimensionsMap: {
                            ClusterName: `genai-demo-${environment}`,
                        },
                        statistic: 'Average',
                        label: 'Cluster CPU Utilization (%)',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'ContainerInsights',
                        metricName: 'cluster_memory_utilization',
                        dimensionsMap: {
                            ClusterName: `genai-demo-${environment}`,
                        },
                        statistic: 'Average',
                        label: 'Cluster Memory Utilization (%)',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'ContainerInsights',
                        metricName: 'cluster_number_of_running_pods',
                        dimensionsMap: {
                            ClusterName: `genai-demo-${environment}`,
                        },
                        statistic: 'Average',
                        label: 'Running Pods',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Thread Pool Metrics (from Spring Boot Actuator)
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Application Thread Pool Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/ThreadPool',
                        metricName: 'executor.active',
                        dimensionsMap: {
                            Environment: environment,
                            Name: 'taskExecutor',
                        },
                        statistic: 'Average',
                        label: 'Active Threads',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/ThreadPool',
                        metricName: 'executor.pool.max',
                        dimensionsMap: {
                            Environment: environment,
                            Name: 'taskExecutor',
                        },
                        statistic: 'Average',
                        label: 'Max Pool Size',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/ThreadPool',
                        metricName: 'executor.queued',
                        dimensionsMap: {
                            Environment: environment,
                            Name: 'taskExecutor',
                        },
                        statistic: 'Average',
                        label: 'Queued Tasks',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // X-Ray Service Map and Traces
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## X-Ray Distributed Tracing

**Service Map**: [View Service Map](https://console.aws.amazon.com/xray/home?region=${this.region}#/service-map)

**Traces**: [View Traces](https://console.aws.amazon.com/xray/home?region=${this.region}#/traces)

**Key Metrics**:
- Response Time Distribution
- Error Rate Analysis
- Service Dependencies
- Bottleneck Identification

**Sampling Configuration**:
- Fixed Rate: 10% of requests
- Reservoir: 1 request per second minimum
- Service: genai-demo`,
                width: 12,
                height: 6,
            })
        );

        // Grafana Dashboard Link
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## Amazon Managed Grafana

**Workspace**: [Open Grafana Dashboard](https://g-${this.grafanaWorkspace.attrId}.grafana-workspace.${this.region}.amazonaws.com/)

**Data Sources**:
- CloudWatch Metrics
- CloudWatch Logs
- X-Ray Traces
- Prometheus Metrics (KEDA)

**Key Dashboards**:
- EKS Cluster Overview
- Application Performance
- Thread Pool Monitoring
- Distributed Tracing Analysis`,
                width: 12,
                height: 6,
            })
        );

        // JVM and Application Metrics
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'JVM Memory and GC Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/JVM',
                        metricName: 'jvm.memory.used',
                        dimensionsMap: {
                            Environment: environment,
                            Area: 'heap',
                        },
                        statistic: 'Average',
                        label: 'Heap Memory Used (Bytes)',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/JVM',
                        metricName: 'jvm.memory.max',
                        dimensionsMap: {
                            Environment: environment,
                            Area: 'heap',
                        },
                        statistic: 'Average',
                        label: 'Max Heap Memory (Bytes)',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/JVM',
                        metricName: 'jvm.gc.pause',
                        dimensionsMap: {
                            Environment: environment,
                        },
                        statistic: 'Average',
                        label: 'GC Pause Time (ms)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // HTTP Request Metrics
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'HTTP Request Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/HTTP',
                        metricName: 'http.server.requests',
                        dimensionsMap: {
                            Environment: environment,
                            Status: '2xx',
                        },
                        statistic: 'Sum',
                        label: 'Successful Requests',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/HTTP',
                        metricName: 'http.server.requests',
                        dimensionsMap: {
                            Environment: environment,
                            Status: '5xx',
                        },
                        statistic: 'Sum',
                        label: 'Server Errors',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/HTTP',
                        metricName: 'http.server.requests.duration',
                        dimensionsMap: {
                            Environment: environment,
                        },
                        statistic: 'Average',
                        label: 'Response Time (ms)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );
    }

    /**
     * Create comprehensive outputs for the observability stack
     */
    private createOutputs(environment: string): void {
        new cdk.CfnOutput(this, 'LogGroupName', {
            value: this.logGroup.logGroupName,
            exportName: `${environment}-LogGroupName`,
        });

        new cdk.CfnOutput(this, 'DashboardURL', {
            value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.dashboard.dashboardName}`,
            exportName: `${environment}-DashboardURL`,
        });

        new cdk.CfnOutput(this, 'XRayServiceMapURL', {
            value: `https://console.aws.amazon.com/xray/home?region=${this.region}#/service-map`,
            exportName: `${environment}-XRayServiceMapURL`,
        });

        new cdk.CfnOutput(this, 'GrafanaWorkspaceId', {
            value: this.grafanaWorkspace.attrId,
            exportName: `${environment}-GrafanaWorkspaceId`,
        });

        new cdk.CfnOutput(this, 'GrafanaWorkspaceURL', {
            value: `https://g-${this.grafanaWorkspace.attrId}.grafana-workspace.${this.region}.amazonaws.com/`,
            exportName: `${environment}-GrafanaWorkspaceURL`,
        });

        new cdk.CfnOutput(this, 'ContainerInsightsRoleArn', {
            value: this.containerInsightsRole.roleArn,
            exportName: `${environment}-ContainerInsightsRoleArn`,
        });

        new cdk.CfnOutput(this, 'XRayRoleArn', {
            value: this.xrayRole.roleArn,
            exportName: `${environment}-XRayRoleArn`,
        });
    }
}