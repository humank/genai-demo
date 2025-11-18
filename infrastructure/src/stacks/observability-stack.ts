import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatch_actions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as grafana from 'aws-cdk-lib/aws-grafana';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';
import { LambdaInsightsMonitoring } from '../constructs/lambda-insights-monitoring';
import { ApplicationInsightsRum } from '../constructs/application-insights-rum';
import { CloudWatchSyntheticsMonitoring } from '../constructs/cloudwatch-synthetics-monitoring';
import { VpcFlowLogsMonitoring } from '../constructs/vpc-flow-logs-monitoring';

export interface ObservabilityStackProps extends cdk.StackProps {
    vpc: ec2.Vpc;
    kmsKey: kms.Key;
    eksCluster?: any; // Optional EKS cluster reference for Container Insights
    environment?: string; // Environment name for resource naming
    // EKS resource utilization monitoring configuration
    resourceUtilizationConfig?: {
        enabled: boolean;
        targetUtilization: number; // Target > 70%
        enableIdleResourceDetection: boolean;
        enableIntelligentScaling: boolean;
    };
    // EKS cluster references for resource monitoring
    eksClusterNames?: string[];
    // Multi-region configuration
    multiRegionConfig?: {
        enabled: boolean;
        regions: string[];
        primaryRegion: string;
        crossRegionReplication: boolean;
    };
    // API endpoint for Synthetics monitoring
    apiEndpoint?: string;
    // Enable CloudWatch Synthetics proactive monitoring
    enableSyntheticsMonitoring?: boolean;
}

export class ObservabilityStack extends cdk.Stack {
    public readonly logGroup: logs.LogGroup;
    public readonly dashboard: cloudwatch.Dashboard;
    public xrayRole: iam.Role;
    public grafanaWorkspace: grafana.CfnWorkspace;
    public containerInsightsRole: iam.Role;
    public lambdaInsightsMonitoring: LambdaInsightsMonitoring;
    public syntheticsMonitoring?: CloudWatchSyntheticsMonitoring;
    public vpcFlowLogsMonitoring?: VpcFlowLogsMonitoring;
    public criticalAlertTopic?: sns.Topic;
    public warningAlertTopic?: sns.Topic;

    constructor(scope: Construct, id: string, props: ObservabilityStackProps) {
        super(scope, id, props);

        const environment = props.environment || 'development';

        // Create CloudWatch Log Group
        this.logGroup = new logs.LogGroup(this, 'ApplicationLogGroup', {
            logGroupName: `/aws/genai-demo/application`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create SNS topics for alerting
        this.criticalAlertTopic = new sns.Topic(this, 'CriticalAlertTopic', {
            topicName: `${environment}-critical-alerts`,
            displayName: 'Critical Alerts for GenAI Demo',
        });

        this.warningAlertTopic = new sns.Topic(this, 'WarningAlertTopic', {
            topicName: `${environment}-warning-alerts`,
            displayName: 'Warning Alerts for GenAI Demo',
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

        // Add RDS Performance Insights deep monitoring
        this.addRDSPerformanceInsightsMonitoring(environment);

        // Add Lambda Insights intelligent monitoring (Requirements 13.7, 13.8, 13.9)
        this.addLambdaInsightsMonitoring(environment);

        // Add Application Insights RUM monitoring (Requirements 13.10, 13.11, 13.12)
        this.addApplicationInsightsRumMonitoring(environment);

        // Add CloudWatch Synthetics proactive monitoring (Requirements 13.13, 13.14, 13.15)
        if (props.enableSyntheticsMonitoring && props.apiEndpoint) {
            this.addCloudWatchSyntheticsMonitoring(environment, props.apiEndpoint);
        }

        // Add VPC Flow Logs network insights (Requirements 13.16, 13.17, 13.18)
        this.addVpcFlowLogsMonitoring(environment, props.vpc);

        // Outputs
        this.createOutputs(environment);

        // Add EKS resource utilization monitoring if enabled
        if (props.resourceUtilizationConfig?.enabled) {
            this.addEKSResourceUtilizationMonitoring(environment, props.resourceUtilizationConfig, props.eksClusterNames);
            this.addIdleResourceDetection(environment, props.eksClusterNames);
            this.addIntelligentScalingRecommendations(environment, props.resourceUtilizationConfig);
        }
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
     * Requirements: 13.1, 13.2, 13.3
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

        // Add custom policy for Container Insights with enhanced permissions
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
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
                'eks:DescribeCluster',
                'eks:ListClusters',
            ],
            resources: ['*'],
        }));

        // Create log group for Container Insights performance metrics
        const containerInsightsLogGroup = new logs.LogGroup(this, 'ContainerInsightsLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/performance`,
            retention: logs.RetentionDays.TWO_WEEKS, // Extended retention for analysis
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create log group for application logs
        const applicationLogGroup = new logs.LogGroup(this, 'EKSApplicationLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/application`,
            retention: logs.RetentionDays.TWO_WEEKS,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create log group for dataplane logs
        const dataplaneLogGroup = new logs.LogGroup(this, 'EKSDataplaneLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/dataplane`,
            retention: logs.RetentionDays.TWO_WEEKS,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create log group for host logs
        const hostLogGroup = new logs.LogGroup(this, 'EKSHostLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/host`,
            retention: logs.RetentionDays.TWO_WEEKS,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Configure Container Insights alarms for pod resource anomalies (Requirement 13.2)
        this.configureContainerInsightsAlarms(environment);

        // Configure automated container restart analysis (Requirement 13.3)
        this.configureContainerRestartAnalysis(environment, containerInsightsLogGroup);
    }

    /**
     * Configure CloudWatch alarms for Container Insights anomaly detection
     * Requirement 13.2: Automatic alerting for pod resource anomalies
     */
    private configureContainerInsightsAlarms(environment: string): void {
        // Alarm for high pod CPU utilization
        const podCpuAlarm = new cloudwatch.Alarm(this, 'PodHighCpuAlarm', {
            alarmName: `${environment}-pod-high-cpu`,
            alarmDescription: 'Alert when pod CPU utilization exceeds 80%',
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'pod_cpu_utilization',
                dimensionsMap: {
                    ClusterName: `${environment}-genai-demo-cluster`,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 80,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for high pod memory utilization
        const podMemoryAlarm = new cloudwatch.Alarm(this, 'PodHighMemoryAlarm', {
            alarmName: `${environment}-pod-high-memory`,
            alarmDescription: 'Alert when pod memory utilization exceeds 85%',
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'pod_memory_utilization',
                dimensionsMap: {
                    ClusterName: `${environment}-genai-demo-cluster`,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 85,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for pod network errors
        const podNetworkErrorAlarm = new cloudwatch.Alarm(this, 'PodNetworkErrorAlarm', {
            alarmName: `${environment}-pod-network-errors`,
            alarmDescription: 'Alert when pod network errors exceed threshold',
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'pod_network_rx_errors',
                dimensionsMap: {
                    ClusterName: `${environment}-genai-demo-cluster`,
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 10,
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for container restart rate
        const containerRestartAlarm = new cloudwatch.Alarm(this, 'ContainerRestartAlarm', {
            alarmName: `${environment}-container-restart-rate`,
            alarmDescription: 'Alert when container restart rate is high',
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'pod_number_of_container_restarts',
                dimensionsMap: {
                    ClusterName: `${environment}-genai-demo-cluster`,
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(10),
            }),
            threshold: 5,
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Connect alarms to SNS topic if available
        if (this.criticalAlertTopic) {
            podCpuAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));
            podMemoryAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));
            podNetworkErrorAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.warningAlertTopic || this.criticalAlertTopic));
            containerRestartAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));
        }
    }

    /**
     * Configure automated container restart analysis using Lambda
     * Requirement 13.3: Record complete event chain and root cause analysis for container crashes/restarts
     */
    private configureContainerRestartAnalysis(environment: string, logGroup: logs.LogGroup): void {
        // Create Lambda function for container restart analysis
        const restartAnalysisFunction = new lambda.Function(this, 'ContainerRestartAnalysis', {
            functionName: `${environment}-container-restart-analysis`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime, timedelta

cloudwatch = boto3.client('cloudwatch')
logs_client = boto3.client('logs')

def handler(event, context):
    """
    Analyze container restart events and provide root cause analysis
    """
    cluster_name = os.environ.get('CLUSTER_NAME', 'genai-demo-cluster')
    log_group = os.environ.get('LOG_GROUP_NAME')
    
    try:
        # Query CloudWatch Logs Insights for restart events
        query = """
        fields @timestamp, kubernetes.pod_name, kubernetes.namespace_name, kubernetes.container_name, log
        | filter kubernetes.container_name like /genai-demo/
        | filter log like /OOMKilled|CrashLoopBackOff|Error|Exception/
        | sort @timestamp desc
        | limit 20
        """
        
        end_time = datetime.now()
        start_time = end_time - timedelta(minutes=15)
        
        query_response = logs_client.start_query(
            logGroupName=log_group,
            startTime=int(start_time.timestamp()),
            endTime=int(end_time.timestamp()),
            queryString=query
        )
        
        query_id = query_response['queryId']
        
        # Wait for query to complete (simplified for demo)
        import time
        time.sleep(2)
        
        results = logs_client.get_query_results(queryId=query_id)
        
        # Analyze restart patterns
        restart_analysis = {
            'timestamp': datetime.now().isoformat(),
            'cluster': cluster_name,
            'restart_events': len(results.get('results', [])),
            'analysis': []
        }
        
        for result in results.get('results', []):
            event_data = {field['field']: field['value'] for field in result}
            
            # Determine root cause
            log_message = event_data.get('log', '')
            root_cause = 'Unknown'
            
            if 'OOMKilled' in log_message:
                root_cause = 'Out of Memory - Container exceeded memory limits'
            elif 'CrashLoopBackOff' in log_message:
                root_cause = 'Application crash - Check application logs for errors'
            elif 'Exception' in log_message or 'Error' in log_message:
                root_cause = 'Application error - Review error logs and stack traces'
            
            restart_analysis['analysis'].append({
                'pod': event_data.get('kubernetes.pod_name', 'unknown'),
                'namespace': event_data.get('kubernetes.namespace_name', 'default'),
                'container': event_data.get('kubernetes.container_name', 'unknown'),
                'timestamp': event_data.get('@timestamp', ''),
                'root_cause': root_cause,
                'log_snippet': log_message[:200]
            })
        
        # Publish custom metric for restart analysis
        cloudwatch.put_metric_data(
            Namespace='ContainerInsights/Analysis',
            MetricData=[
                {
                    'MetricName': 'ContainerRestartEvents',
                    'Value': restart_analysis['restart_events'],
                    'Unit': 'Count',
                    'Timestamp': datetime.now(),
                    'Dimensions': [
                        {'Name': 'ClusterName', 'Value': cluster_name}
                    ]
                }
            ]
        )
        
        print(json.dumps(restart_analysis, indent=2))
        
        return {
            'statusCode': 200,
            'body': json.dumps(restart_analysis)
        }
        
    except Exception as e:
        print(f"Error analyzing container restarts: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
`),
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            environment: {
                CLUSTER_NAME: `${environment}-genai-demo-cluster`,
                LOG_GROUP_NAME: logGroup.logGroupName,
            },
        });

        // Grant permissions to Lambda function
        restartAnalysisFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:StartQuery',
                'logs:GetQueryResults',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
                'cloudwatch:PutMetricData',
            ],
            resources: ['*'],
        }));

        // Create EventBridge rule to trigger analysis every 15 minutes
        const analysisRule = new events.Rule(this, 'ContainerRestartAnalysisRule', {
            ruleName: `${environment}-container-restart-analysis`,
            description: 'Trigger container restart analysis every 15 minutes',
            schedule: events.Schedule.rate(cdk.Duration.minutes(15)),
        });

        analysisRule.addTarget(new targets.LambdaFunction(restartAnalysisFunction));

        // Output Lambda function ARN
        new cdk.CfnOutput(this, 'ContainerRestartAnalysisFunctionArn', {
            value: restartAnalysisFunction.functionArn,
            description: 'Container Restart Analysis Lambda Function ARN',
            exportName: `${environment}-container-restart-analysis-function-arn`,
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
            // Configuration is handled via other properties in CDK v2
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

    private addEKSResourceUtilizationMonitoring(environment: string, config: any, clusterNames?: string[]): void {
        // Create EKS resource utilization dashboard
        const eksUtilizationDashboard = new cloudwatch.Dashboard(this, 'EKSResourceUtilizationDashboard', {
            dashboardName: `genai-demo-${environment}-eks-resource-utilization`,
            defaultInterval: cdk.Duration.hours(1)
        });

        if (clusterNames && clusterNames.length > 0) {
            clusterNames.forEach((clusterName, index) => {
                // Node Group CPU utilization widget
                eksUtilizationDashboard.addWidgets(
                    new cloudwatch.GraphWidget({
                        title: `${clusterName} - Node Group CPU Utilization (Target > ${config.targetUtilization}%)`,
                        left: [
                            new cloudwatch.Metric({
                                namespace: 'ContainerInsights',
                                metricName: 'node_cpu_utilization',
                                dimensionsMap: {
                                    ClusterName: clusterName
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(5),
                                label: 'CPU Utilization %'
                            })
                        ],
                        right: [
                            new cloudwatch.Metric({
                                namespace: 'ContainerInsights',
                                metricName: 'node_memory_utilization',
                                dimensionsMap: {
                                    ClusterName: clusterName
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(5),
                                label: 'Memory Utilization %'
                            })
                        ],
                        width: 12,
                        height: 6
                    })
                );

                // Pod resource utilization widget
                eksUtilizationDashboard.addWidgets(
                    new cloudwatch.GraphWidget({
                        title: `${clusterName} - Pod Resource Utilization`,
                        left: [
                            new cloudwatch.Metric({
                                namespace: 'ContainerInsights',
                                metricName: 'pod_cpu_utilization',
                                dimensionsMap: {
                                    ClusterName: clusterName
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(5),
                                label: 'Pod CPU Utilization %'
                            })
                        ],
                        right: [
                            new cloudwatch.Metric({
                                namespace: 'ContainerInsights',
                                metricName: 'pod_memory_utilization',
                                dimensionsMap: {
                                    ClusterName: clusterName
                                },
                                statistic: 'Average',
                                period: cdk.Duration.minutes(5),
                                label: 'Pod Memory Utilization %'
                            })
                        ],
                        width: 12,
                        height: 6
                    })
                );

                // Create resource utilization alarms
                this.createResourceUtilizationAlarms(clusterName, config.targetUtilization, environment);
            });
        }

        // Add resource utilization summary widget
        eksUtilizationDashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## EKS Resource Utilization Monitoring

**Environment**: ${environment}
**Target Utilization**: > ${config.targetUtilization}%

**Key Metrics**:
- **Node CPU/Memory Utilization**: Target > ${config.targetUtilization}%
- **Pod Resource Efficiency**: Ratio of pod usage to node capacity
- **Resource Waste Detection**: Identify underutilized resources

**Optimization Strategies**:
1. **Right-sizing**: Adjust node group instance types based on actual usage
2. **Pod Density**: Optimize pod placement to maximize node utilization
3. **Vertical Pod Autoscaling**: Automatically adjust pod resource requests
4. **Horizontal Pod Autoscaling**: Scale pods based on demand

**Alert Thresholds**:
- **Low Utilization**: < 30% for > 2 hours (consider scaling down)
- **High Utilization**: > 90% for > 15 minutes (consider scaling up)
- **Resource Waste**: Efficiency < 50% (optimize pod placement)

**Cost Optimization**:
- **Idle Resource Detection**: Automatically identify unused resources
- **Reserved Instance Recommendations**: For predictable workloads
- **Spot Instance Integration**: For fault-tolerant workloads`,
                width: 24,
                height: 12
            })
        );
    }

    private createResourceUtilizationAlarms(clusterName: string, targetUtilization: number, environment: string): void {
        // Low CPU utilization alarm
        const lowCpuAlarm = new cloudwatch.Alarm(this, `${clusterName}LowCpuUtilizationAlarm`, {
            alarmName: `genai-demo-${environment}-${clusterName}-low-cpu-utilization`,
            alarmDescription: `Low CPU utilization detected in ${clusterName} cluster`,
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'node_cpu_utilization',
                dimensionsMap: {
                    ClusterName: clusterName
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(15)
            }),
            threshold: 30, // Less than 30% CPU utilization
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            evaluationPeriods: 8, // 2 hours (8 * 15 minutes)
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });

        // High CPU utilization alarm
        const highCpuAlarm = new cloudwatch.Alarm(this, `${clusterName}HighCpuUtilizationAlarm`, {
            alarmName: `genai-demo-${environment}-${clusterName}-high-cpu-utilization`,
            alarmDescription: `High CPU utilization detected in ${clusterName} cluster`,
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'node_cpu_utilization',
                dimensionsMap: {
                    ClusterName: clusterName
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5)
            }),
            threshold: 90, // Greater than 90% CPU utilization
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            evaluationPeriods: 3, // 15 minutes (3 * 5 minutes)
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING
        });
    }

    private addIdleResourceDetection(environment: string, clusterNames?: string[]): void {
        // Create Lambda function for idle resource detection
        const idleResourceDetectionFunction = new lambda.Function(this, 'IdleResourceDetectionFunction', {
            functionName: `genai-demo-${environment}-idle-resource-detection`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import json
import boto3
import datetime
from typing import Dict, List, Any

def handler(event, context):
    """
    Detect idle resources in EKS clusters
    """
    cloudwatch = boto3.client('cloudwatch')
    
    idle_resources = []
    
    try:
        cluster_names = event.get('clusterNames', [])
        
        for cluster_name in cluster_names:
            # Analyze node utilization
            node_analysis = analyze_node_utilization(cloudwatch, cluster_name)
            idle_resources.extend(node_analysis)
        
        # Publish idle resource metrics
        if idle_resources:
            cloudwatch.put_metric_data(
                Namespace='GenAIDemo/ResourceUtilization',
                MetricData=[
                    {
                        'MetricName': 'IdleResourceCount',
                        'Value': len(idle_resources),
                        'Unit': 'Count',
                        'Timestamp': datetime.datetime.utcnow()
                    }
                ]
            )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'idleResources': idle_resources,
                'totalIdleCount': len(idle_resources)
            })
        }
        
    except Exception as e:
        print(f"Error in idle resource detection: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def analyze_node_utilization(cloudwatch, cluster_name: str) -> List[Dict[str, Any]]:
    """Analyze node utilization to detect idle nodes"""
    idle_nodes = []
    
    # Get node CPU utilization over the last 24 hours
    cpu_response = cloudwatch.get_metric_statistics(
        Namespace='ContainerInsights',
        MetricName='node_cpu_utilization',
        Dimensions=[
            {'Name': 'ClusterName', 'Value': cluster_name}
        ],
        StartTime=datetime.datetime.utcnow() - datetime.timedelta(hours=24),
        EndTime=datetime.datetime.utcnow(),
        Period=3600,
        Statistics=['Average']
    )
    
    if cpu_response['Datapoints']:
        avg_cpu = sum(dp['Average'] for dp in cpu_response['Datapoints']) / len(cpu_response['Datapoints'])
        
        if avg_cpu < 10:  # Less than 10% CPU utilization
            idle_nodes.append({
                'type': 'IDLE_NODE',
                'cluster': cluster_name,
                'resource': 'node',
                'utilization': avg_cpu,
                'recommendation': 'Consider scaling down node group or using smaller instance types',
                'potential_savings': 'Up to 80% cost reduction',
                'idle_duration': '24+ hours'
            })
    
    return idle_nodes
            `),
            timeout: cdk.Duration.minutes(5),
            memorySize: 256,
            environment: {
                'ENVIRONMENT': environment
            }
        });

        // Grant necessary permissions
        idleResourceDetectionFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:PutMetricData',
                'eks:DescribeCluster',
                'eks:ListClusters'
            ],
            resources: ['*']
        }));

        // Schedule the function to run every 4 hours
        const idleResourceRule = new events.Rule(this, 'IdleResourceDetectionSchedule', {
            ruleName: `genai-demo-${environment}-idle-resource-detection-schedule`,
            schedule: events.Schedule.rate(cdk.Duration.hours(4)),
            description: 'Detect idle resources every 4 hours'
        });

        idleResourceRule.addTarget(new eventsTargets.LambdaFunction(idleResourceDetectionFunction, {
            event: events.RuleTargetInput.fromObject({
                clusterNames: clusterNames || [],
                environment: environment
            })
        }));
    }

    private addIntelligentScalingRecommendations(environment: string, config: any): void {
        // Add intelligent scaling recommendations to the main dashboard
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'EKS Resource Utilization Overview',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'GenAIDemo/ResourceUtilization',
                        metricName: 'IdleResourceCount',
                        statistic: 'Average',
                        period: cdk.Duration.hours(1),
                        label: 'Idle Resources Count'
                    })
                ],
                width: 12,
                height: 6
            })
        );

        // Add resource optimization recommendations widget
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## EKS Intelligent Scaling Recommendations

**Environment**: ${environment}
**Target Utilization**: > ${config.targetUtilization}%

**Automated Analysis**:
- **Idle Resource Detection**: Runs every 4 hours
- **Resource Efficiency Monitoring**: Continuous monitoring
- **Cost Optimization Recommendations**: Based on actual usage patterns

**Scaling Strategies**:
1. **Vertical Pod Autoscaling (VPA)**: Automatically adjust pod resource requests
2. **Horizontal Pod Autoscaling (HPA)**: Scale pods based on CPU/Memory/Custom metrics
3. **Cluster Autoscaling**: Automatically adjust node group size
4. **KEDA Integration**: Event-driven autoscaling for specific workloads

**Key Metrics**:
- **Node Utilization**: Target > ${config.targetUtilization}% CPU/Memory
- **Pod Density**: Maximize pods per node while maintaining performance
- **Resource Efficiency**: Ratio of requested vs actual resource usage
- **Cost per Request**: Monitor cost efficiency of scaling decisions

**Optimization Actions**:
1. **Right-size Node Groups**: Use appropriate instance types for workload
2. **Optimize Pod Resource Requests**: Align requests with actual usage
3. **Implement Reserved Instances**: For predictable baseline capacity
4. **Use Spot Instances**: For fault-tolerant batch workloads

**Alert Thresholds**:
- **Idle Resources**: > 5 idle resources for > 4 hours
- **Low Efficiency**: Resource efficiency < 50%
- **High Waste**: Potential savings > $100/month`,
                width: 12,
                height: 10
            })
        );
    }

    /**
     * Add RDS Performance Insights deep monitoring
     * Implements Task 56: Integrate RDS Performance Insights deep monitoring
     * - Enable Aurora Performance Insights with query performance tracking
     * - Implement slow query automatic analysis
     * - Add connection pool optimization recommendations
     */
    private addRDSPerformanceInsightsMonitoring(environment: string): void {
        const dbInstanceIdentifier = `genai-demo-${environment}-primary-aurora`;

        // Performance Insights - Top SQL Queries Widget
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'RDS Performance Insights - Top SQL Queries',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'DBLoad',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(1),
                        label: 'Database Load (Active Sessions)',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'DBLoadCPU',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(1),
                        label: 'CPU Load',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'DBLoadNonCPU',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(1),
                        label: 'Non-CPU Load (I/O, Locks)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Performance Insights - Wait Events Analysis
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'RDS Performance Insights - Wait Events',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'ReadIOPS',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Read IOPS',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'WriteIOPS',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Write IOPS',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'ReadThroughput',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Read Throughput (Bytes/s)',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RDS',
                        metricName: 'WriteThroughput',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Average',
                        label: 'Write Throughput (Bytes/s)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Connection Pool Monitoring
        this.dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'RDS - Connection Pool Metrics',
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
                        metricName: 'MaximumUsedTransactionIDs',
                        dimensionsMap: {
                            DBInstanceIdentifier: dbInstanceIdentifier,
                        },
                        statistic: 'Maximum',
                        label: 'Max Transaction IDs',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Query Performance Analysis Widget
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## RDS Performance Insights - Deep Monitoring

**Performance Insights Console**: [Open Performance Insights](https://console.aws.amazon.com/rds/home?region=${this.region}#performance-insights-v20206:resourceId=${dbInstanceIdentifier})

### Query Performance Tracking

**Top SQL Queries Analysis**:
- **Database Load (DBLoad)**: Total active sessions (target: < vCPU count)
- **CPU Load**: CPU-bound queries requiring optimization
- **Non-CPU Load**: I/O, locks, and wait events

**Key Performance Metrics**:
1. **DBLoad**: Average active sessions (should be < number of vCPUs)
2. **DBLoadCPU**: CPU-intensive queries
3. **DBLoadNonCPU**: I/O and lock contention
4. **Top SQL**: Queries consuming most database time

### Slow Query Analysis

**Automatic Detection**:
- Queries > 1000ms logged automatically
- \`pg_stat_statements\` enabled for query tracking
- CloudWatch Logs Insights for pattern analysis

**Query Optimization Recommendations**:
1. **Add Indexes**: For frequently filtered columns
2. **Optimize JOINs**: Reduce cross-table operations
3. **Use Connection Pooling**: Reduce connection overhead
4. **Implement Caching**: For frequently accessed data

### Connection Pool Optimization

**Current Configuration**:
- **Max Connections**: 1000 (configured in parameter group)
- **Shared Buffers**: 256MB
- **Effective Cache Size**: 1GB

**Optimization Recommendations**:
1. **Monitor Active Connections**: Keep < 80% of max
2. **Connection Pooling**: Use HikariCP with optimal settings
   - \`maximumPoolSize\`: 20 per instance
   - \`minimumIdle\`: 5
   - \`connectionTimeout\`: 20000ms
   - \`idleTimeout\`: 300000ms
3. **Connection Lifecycle**: Implement proper connection management
4. **Prepared Statements**: Use for frequently executed queries

### Performance Insights Features

**Wait Event Analysis**:
- **Lock:transactionid**: Transaction lock contention
- **Lock:relation**: Table-level lock contention
- **Lock:tuple**: Row-level lock contention
- **IO:DataFileRead**: Disk I/O bottlenecks

**Top SQL Identification**:
- Queries by execution time
- Queries by CPU time
- Queries by I/O operations
- Queries by lock wait time

**Database Load Breakdown**:
- By wait event type
- By SQL statement
- By user/application
- By database

### Automated Monitoring

**CloudWatch Alarms**:
- DBLoad > vCPU count (sustained)
- Connection count > 800 (80% threshold)
- Slow query rate > 10/minute
- Lock wait time > 5 seconds

**Automated Actions**:
- Slow query log analysis every 15 minutes
- Connection pool health checks
- Performance degradation alerts
- Automatic query optimization suggestions`,
                width: 24,
                height: 20,
            })
        );

        // Create Lambda function for slow query analysis
        this.createSlowQueryAnalysisFunction(environment, dbInstanceIdentifier);

        // Create CloudWatch alarms for Performance Insights metrics
        this.createPerformanceInsightsAlarms(environment, dbInstanceIdentifier);
    }

    /**
     * Create Lambda function for automated slow query analysis
     */
    private createSlowQueryAnalysisFunction(environment: string, dbInstanceIdentifier: string): void {
        const slowQueryAnalysisFunction = new lambda.Function(this, 'SlowQueryAnalysisFunction', {
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
    rds_client = boto3.client('rds')
    
    log_group_name = '/aws/rds/cluster/genai-demo-${environment}-primary-aurora/postgresql'
    db_instance_id = os.environ['DB_INSTANCE_ID']
    
    try:
        # Query CloudWatch Logs for slow queries
        query_response = logs_client.start_query(
            logGroupName=log_group_name,
            startTime=int((datetime.now() - timedelta(minutes=15)).timestamp()),
            endTime=int(datetime.now().timestamp()),
            queryString='''
                fields @timestamp, @message
                | filter @message like /duration:/
                | parse @message /duration: (?<duration>\\\\d+\\\\.\\\\d+) ms/
                | filter duration > 1000
                | stats count() as slow_query_count, avg(duration) as avg_duration, max(duration) as max_duration by bin(5m)
                | sort @timestamp desc
            '''
        )
        
        query_id = query_response['queryId']
        
        # Wait for query to complete
        import time
        time.sleep(10)
        
        results = logs_client.get_query_results(queryId=query_id)
        
        slow_query_count = 0
        avg_duration = 0
        max_duration = 0
        
        if results['results']:
            for result in results['results']:
                for field in result:
                    if field['field'] == 'slow_query_count':
                        slow_query_count += int(float(field['value']))
                    elif field['field'] == 'avg_duration':
                        avg_duration = max(avg_duration, float(field['value']))
                    elif field['field'] == 'max_duration':
                        max_duration = max(max_duration, float(field['value']))
        
        # Publish custom metrics to CloudWatch
        cloudwatch.put_metric_data(
            Namespace='Custom/RDS/PerformanceInsights',
            MetricData=[
                {
                    'MetricName': 'SlowQueryCount',
                    'Value': slow_query_count,
                    'Unit': 'Count',
                    'Timestamp': datetime.now(),
                    'Dimensions': [
                        {'Name': 'DBInstanceIdentifier', 'Value': db_instance_id},
                        {'Name': 'Environment', 'Value': environment}
                    ]
                },
                {
                    'MetricName': 'AverageSlowQueryDuration',
                    'Value': avg_duration,
                    'Unit': 'Milliseconds',
                    'Timestamp': datetime.now(),
                    'Dimensions': [
                        {'Name': 'DBInstanceIdentifier', 'Value': db_instance_id},
                        {'Name': 'Environment', 'Value': environment}
                    ]
                },
                {
                    'MetricName': 'MaxSlowQueryDuration',
                    'Value': max_duration,
                    'Unit': 'Milliseconds',
                    'Timestamp': datetime.now(),
                    'Dimensions': [
                        {'Name': 'DBInstanceIdentifier', 'Value': db_instance_id},
                        {'Name': 'Environment', 'Value': environment}
                    ]
                }
            ]
        )
        
        # Generate optimization recommendations if slow queries detected
        if slow_query_count > 10:
            recommendations = generate_optimization_recommendations(slow_query_count, avg_duration, max_duration)
            
            # Publish recommendations to CloudWatch Logs
            logs_client.put_log_events(
                logGroupName='/aws/lambda/slow-query-analysis',
                logStreamName=f'{environment}-recommendations',
                logEvents=[
                    {
                        'timestamp': int(datetime.now().timestamp() * 1000),
                        'message': json.dumps({
                            'timestamp': datetime.now().isoformat(),
                            'environment': environment,
                            'db_instance': db_instance_id,
                            'slow_query_count': slow_query_count,
                            'avg_duration_ms': avg_duration,
                            'max_duration_ms': max_duration,
                            'recommendations': recommendations
                        })
                    }
                ]
            )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'slow_query_count': slow_query_count,
                'avg_duration': avg_duration,
                'max_duration': max_duration
            })
        }
        
    except Exception as e:
        print(f'Error analyzing slow queries: {str(e)}')
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def generate_optimization_recommendations(count, avg_duration, max_duration):
    recommendations = []
    
    if count > 50:
        recommendations.append({
            'priority': 'HIGH',
            'category': 'Query Optimization',
            'recommendation': 'High volume of slow queries detected. Review and optimize top SQL queries using Performance Insights.',
            'action': 'Analyze top SQL queries in Performance Insights console and add appropriate indexes.'
        })
    
    if avg_duration > 5000:
        recommendations.append({
            'priority': 'HIGH',
            'category': 'Query Performance',
            'recommendation': 'Average query duration exceeds 5 seconds. Consider query optimization and caching.',
            'action': 'Implement query result caching using Redis and optimize complex JOINs.'
        })
    
    if max_duration > 30000:
        recommendations.append({
            'priority': 'CRITICAL',
            'category': 'Query Timeout',
            'recommendation': 'Queries exceeding 30 seconds detected. Risk of connection timeout.',
            'action': 'Implement query timeout limits and break down complex queries into smaller operations.'
        })
    
    recommendations.append({
        'priority': 'MEDIUM',
        'category': 'Connection Pool',
        'recommendation': 'Review connection pool configuration for optimal performance.',
        'action': 'Verify HikariCP settings: maximumPoolSize=20, minimumIdle=5, connectionTimeout=20000ms'
    })
    
    return recommendations
            `),
            timeout: cdk.Duration.minutes(5),
            memorySize: 256,
            environment: {
                'ENVIRONMENT': environment,
                'DB_INSTANCE_ID': dbInstanceIdentifier
            }
        });

        // Grant necessary permissions
        slowQueryAnalysisFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:StartQuery',
                'logs:GetQueryResults',
                'logs:PutLogEvents',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'cloudwatch:PutMetricData',
                'rds:DescribeDBInstances',
                'rds:DescribeDBClusters'
            ],
            resources: ['*']
        }));

        // Schedule the function to run every 15 minutes
        const slowQueryAnalysisRule = new events.Rule(this, 'SlowQueryAnalysisSchedule', {
            ruleName: `genai-demo-${environment}-slow-query-analysis-schedule`,
            schedule: events.Schedule.rate(cdk.Duration.minutes(15)),
            description: 'Analyze slow queries every 15 minutes'
        });

        slowQueryAnalysisRule.addTarget(new eventsTargets.LambdaFunction(slowQueryAnalysisFunction));

        // Create log group for recommendations
        new logs.LogGroup(this, 'SlowQueryRecommendationsLogGroup', {
            logGroupName: '/aws/lambda/slow-query-analysis',
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });
    }

    /**
     * Create CloudWatch alarms for Performance Insights metrics
     */
    private createPerformanceInsightsAlarms(environment: string, dbInstanceIdentifier: string): void {
        // Alarm for high database load
        const highDBLoadAlarm = new cloudwatch.Alarm(this, 'HighDBLoadAlarm', {
            alarmName: `${environment}-rds-high-db-load`,
            alarmDescription: 'Database load exceeds vCPU count - performance degradation likely',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/RDS',
                metricName: 'DBLoad',
                dimensionsMap: {
                    DBInstanceIdentifier: dbInstanceIdentifier,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 4, // Assuming 4 vCPUs for r6g.large
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for high connection count
        const highConnectionCountAlarm = new cloudwatch.Alarm(this, 'HighConnectionCountAlarm', {
            alarmName: `${environment}-rds-high-connection-count`,
            alarmDescription: 'Database connections exceed 80% of maximum - connection pool optimization needed',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/RDS',
                metricName: 'DatabaseConnections',
                dimensionsMap: {
                    DBInstanceIdentifier: dbInstanceIdentifier,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 800, // 80% of 1000 max connections
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for slow queries
        const slowQueryAlarm = new cloudwatch.Alarm(this, 'SlowQueryAlarm', {
            alarmName: `${environment}-rds-slow-query-rate`,
            alarmDescription: 'High rate of slow queries detected - query optimization recommended',
            metric: new cloudwatch.Metric({
                namespace: 'Custom/RDS/PerformanceInsights',
                metricName: 'SlowQueryCount',
                dimensionsMap: {
                    DBInstanceIdentifier: dbInstanceIdentifier,
                    Environment: environment,
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(15),
            }),
            threshold: 10,
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Add alarms to dashboard (separate widgets for each alarm)
        this.dashboard.addWidgets(
            new cloudwatch.AlarmWidget({
                title: 'RDS - DB Load',
                alarm: highDBLoadAlarm,
                width: 8,
                height: 4,
            }),
            new cloudwatch.AlarmWidget({
                title: 'RDS - Connection Count',
                alarm: highConnectionCountAlarm,
                width: 8,
                height: 4,
            }),
            new cloudwatch.AlarmWidget({
                title: 'RDS - Slow Queries',
                alarm: slowQueryAlarm,
                width: 8,
                height: 4,
            })
        );
    }

    /**
     * Add Lambda Insights intelligent monitoring
     * Requirements: 13.7, 13.8, 13.9
     * 
     * Provides:
     * - Execution metrics collection (13.7)
     * - Cold start pattern analysis (13.8)
     * - Cost optimization recommendations (13.9)
     */
    private addLambdaInsightsMonitoring(environment: string): void {
        // Create Lambda Insights monitoring construct
        this.lambdaInsightsMonitoring = new LambdaInsightsMonitoring(this, 'LambdaInsightsMonitoring', {
            environment,
            dashboard: this.dashboard,
        });

        // Output Lambda Insights function ARNs
        new cdk.CfnOutput(this, 'ColdStartAnalysisFunctionArn', {
            value: this.lambdaInsightsMonitoring.coldStartAnalysisFunction.functionArn,
            description: 'ARN of the Lambda cold start analysis function',
            exportName: `${environment}-cold-start-analysis-function-arn`,
        });

        new cdk.CfnOutput(this, 'CostOptimizationFunctionArn', {
            value: this.lambdaInsightsMonitoring.costOptimizationFunction.functionArn,
            description: 'ARN of the Lambda cost optimization function',
            exportName: `${environment}-cost-optimization-function-arn`,
        });
    }

    /**
     * Add Application Insights frontend monitoring with Real User Monitoring (RUM)
     * Requirements: 13.10, 13.11, 13.12
     * 
     * Provides:
     * - Real User Monitoring (RUM) for frontend applications (13.10)
     * - JavaScript error tracking with context collection (13.11)
     * - Core Web Vitals performance monitoring (13.12)
     */
    private addApplicationInsightsRumMonitoring(environment: string): void {
        // Consumer Frontend RUM (Angular)
        const consumerRum = new ApplicationInsightsRum(this, 'ConsumerRum', {
            appMonitorName: `genai-demo-consumer-${environment}`,
            domain: environment === 'production' ? 'consumer.genai-demo.com' : `consumer-${environment}.genai-demo.com`,
            enableSessionRecording: true,
            enableXRay: true,
            sessionSampleRate: environment === 'production' ? 0.1 : 0.5, // 10% in prod, 50% in dev
            tags: {
                Environment: environment,
                Service: 'consumer-frontend',
                Framework: 'Angular',
                ManagedBy: 'CDK',
            },
        });

        // CMC Management Frontend RUM (Next.js)
        const cmcRum = new ApplicationInsightsRum(this, 'CmcRum', {
            appMonitorName: `genai-demo-cmc-${environment}`,
            domain: environment === 'production' ? 'cmc.genai-demo.com' : `cmc-${environment}.genai-demo.com`,
            enableSessionRecording: true,
            enableXRay: true,
            sessionSampleRate: environment === 'production' ? 0.2 : 0.5, // 20% in prod, 50% in dev
            tags: {
                Environment: environment,
                Service: 'cmc-frontend',
                Framework: 'NextJS',
                ManagedBy: 'CDK',
            },
        });

        // Add RUM metrics to dashboard
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## Application Insights - Real User Monitoring (RUM)\n\n**Consumer Frontend (Angular)**: ${consumerRum.appMonitor.name}\n**CMC Frontend (Next.js)**: ${cmcRum.appMonitor.name}\n\n### Key Metrics:\n- Page Load Performance\n- JavaScript Errors\n- Core Web Vitals (LCP, FID, CLS)\n- HTTP Request Tracking`,
                width: 24,
                height: 3,
            }),
            new cloudwatch.GraphWidget({
                title: 'Frontend RUM - Page Load Performance',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'PageLoadTime',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'Average',
                        label: 'Consumer Frontend (Avg)',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'PageLoadTime',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'p95',
                        label: 'Consumer Frontend (P95)',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'PageLoadTime',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'Average',
                        label: 'CMC Frontend (Avg)',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'PageLoadTime',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'p95',
                        label: 'CMC Frontend (P95)',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Frontend RUM - JavaScript Errors',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'JsErrorCount',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'Sum',
                        label: 'Consumer Errors',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'JsErrorCount',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'Sum',
                        label: 'CMC Errors',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Frontend RUM - Core Web Vitals (Consumer)',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'LargestContentfulPaint',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'p75',
                        label: 'LCP (P75) - Target < 2.5s',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'FirstInputDelay',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'p75',
                        label: 'FID (P75) - Target < 100ms',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'CumulativeLayoutShift',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'p75',
                        label: 'CLS (P75) - Target < 0.1',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Frontend RUM - Core Web Vitals (CMC)',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'LargestContentfulPaint',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'p75',
                        label: 'LCP (P75) - Target < 2.5s',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'FirstInputDelay',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'p75',
                        label: 'FID (P75) - Target < 100ms',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'CumulativeLayoutShift',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'p75',
                        label: 'CLS (P75) - Target < 0.1',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Frontend RUM - HTTP Request Performance',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'HttpRequestDuration',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'Average',
                        label: 'Consumer HTTP Avg',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'HttpRequestDuration',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'Average',
                        label: 'CMC HTTP Avg',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Frontend RUM - Session Count',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'SessionCount',
                        dimensionsMap: {
                            application_name: consumerRum.appMonitor.name,
                        },
                        statistic: 'Sum',
                        label: 'Consumer Sessions',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/RUM',
                        metricName: 'SessionCount',
                        dimensionsMap: {
                            application_name: cmcRum.appMonitor.name,
                        },
                        statistic: 'Sum',
                        label: 'CMC Sessions',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Create CloudWatch alarms for RUM metrics
        this.createRumAlarms(environment, consumerRum.appMonitor.name, cmcRum.appMonitor.name);

        // Output RUM configuration
        new cdk.CfnOutput(this, 'ConsumerRumAppMonitorId', {
            value: consumerRum.appMonitor.ref,
            description: 'Consumer Frontend RUM App Monitor ID',
            exportName: `${environment}-consumer-rum-app-monitor-id`,
        });

        new cdk.CfnOutput(this, 'ConsumerRumIdentityPoolId', {
            value: consumerRum.identityPool.ref,
            description: 'Consumer Frontend RUM Identity Pool ID',
            exportName: `${environment}-consumer-rum-identity-pool-id`,
        });

        new cdk.CfnOutput(this, 'CmcRumAppMonitorId', {
            value: cmcRum.appMonitor.ref,
            description: 'CMC Frontend RUM App Monitor ID',
            exportName: `${environment}-cmc-rum-app-monitor-id`,
        });

        new cdk.CfnOutput(this, 'CmcRumIdentityPoolId', {
            value: cmcRum.identityPool.ref,
            description: 'CMC Frontend RUM Identity Pool ID',
            exportName: `${environment}-cmc-rum-identity-pool-id`,
        });
    }

    /**
     * Create CloudWatch alarms for RUM metrics
     */
    private createRumAlarms(environment: string, consumerAppName: string, cmcAppName: string): void {
        // Alarm for high JavaScript error rate (Consumer)
        const consumerErrorAlarm = new cloudwatch.Alarm(this, 'ConsumerHighJsErrorAlarm', {
            alarmName: `${environment}-consumer-high-js-error-rate`,
            alarmDescription: 'High JavaScript error rate detected in Consumer frontend',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/RUM',
                metricName: 'JsErrorCount',
                dimensionsMap: {
                    application_name: consumerAppName,
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 10, // More than 10 errors in 5 minutes
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for high JavaScript error rate (CMC)
        const cmcErrorAlarm = new cloudwatch.Alarm(this, 'CmcHighJsErrorAlarm', {
            alarmName: `${environment}-cmc-high-js-error-rate`,
            alarmDescription: 'High JavaScript error rate detected in CMC frontend',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/RUM',
                metricName: 'JsErrorCount',
                dimensionsMap: {
                    application_name: cmcAppName,
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 10,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for poor Core Web Vitals - LCP (Consumer)
        const consumerLcpAlarm = new cloudwatch.Alarm(this, 'ConsumerPoorLcpAlarm', {
            alarmName: `${environment}-consumer-poor-lcp`,
            alarmDescription: 'Poor Largest Contentful Paint (LCP) in Consumer frontend - Target < 2.5s',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/RUM',
                metricName: 'LargestContentfulPaint',
                dimensionsMap: {
                    application_name: consumerAppName,
                },
                statistic: 'p75',
                period: cdk.Duration.minutes(15),
            }),
            threshold: 2500, // 2.5 seconds in milliseconds
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for poor Core Web Vitals - FID (Consumer)
        const consumerFidAlarm = new cloudwatch.Alarm(this, 'ConsumerPoorFidAlarm', {
            alarmName: `${environment}-consumer-poor-fid`,
            alarmDescription: 'Poor First Input Delay (FID) in Consumer frontend - Target < 100ms',
            metric: new cloudwatch.Metric({
                namespace: 'AWS/RUM',
                metricName: 'FirstInputDelay',
                dimensionsMap: {
                    application_name: consumerAppName,
                },
                statistic: 'p75',
                period: cdk.Duration.minutes(15),
            }),
            threshold: 100, // 100 milliseconds
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Add alarms to SNS topics if available
        if (this.criticalAlertTopic) {
            consumerErrorAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));
            cmcErrorAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));
        }

        if (this.warningAlertTopic) {
            consumerLcpAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.warningAlertTopic));
            consumerFidAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.warningAlertTopic));
        }

        // Add alarm widgets to dashboard
        this.dashboard.addWidgets(
            new cloudwatch.AlarmWidget({
                title: 'RUM - Consumer JS Errors',
                alarm: consumerErrorAlarm,
                width: 8,
                height: 4,
            }),
            new cloudwatch.AlarmWidget({
                title: 'RUM - CMC JS Errors',
                alarm: cmcErrorAlarm,
                width: 8,
                height: 4,
            }),
            new cloudwatch.AlarmWidget({
                title: 'RUM - Consumer LCP',
                alarm: consumerLcpAlarm,
                width: 8,
                height: 4,
            })
        );
    }

    /**
     * Add CloudWatch Synthetics proactive monitoring
     * Requirements: 13.13, 13.14, 13.15
     */
    private addCloudWatchSyntheticsMonitoring(environment: string, apiEndpoint: string): void {
        // Define critical business process endpoints to monitor
        const criticalEndpoints = [
            {
                path: '/actuator/health',
                method: 'GET',
                expectedStatusCode: 200,
                maxLatencyMs: 2000,
            },
            {
                path: '/actuator/health/readiness',
                method: 'GET',
                expectedStatusCode: 200,
                maxLatencyMs: 2000,
            },
            {
                path: '/actuator/health/liveness',
                method: 'GET',
                expectedStatusCode: 200,
                maxLatencyMs: 2000,
            },
            {
                path: '/api/v1/customers',
                method: 'GET',
                expectedStatusCode: 200,
                maxLatencyMs: 3000,
            },
            {
                path: '/api/v1/orders',
                method: 'GET',
                expectedStatusCode: 200,
                maxLatencyMs: 3000,
            },
        ];

        // Create CloudWatch Synthetics monitoring construct
        this.syntheticsMonitoring = new CloudWatchSyntheticsMonitoring(this, 'SyntheticsMonitoring', {
            environment,
            apiEndpoint,
            criticalAlertTopic: this.criticalAlertTopic,
            warningAlertTopic: this.warningAlertTopic,
            enableDetailedMonitoring: true,
            executionFrequencyMinutes: 1, // 1-minute detection as per requirement 13.14
            criticalEndpoints,
        });

        // Add Synthetics metrics to dashboard
        this.syntheticsMonitoring.addToDashboard(this.dashboard);

        // Add Synthetics overview widget
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## CloudWatch Synthetics Proactive Monitoring

**Purpose**: Automated end-to-end functional tests and API health monitoring

**Canaries**:
- **API Health Check**: Monitors /actuator/health endpoint every 1 minute
- **Business Process Monitors**: Validates critical API endpoints
- **E2E Functional Tests**: Complete application workflow validation

**Detection Time**: < 1 minute for API endpoint failures

**Monitored Endpoints**:
${criticalEndpoints.map((ep) => `- ${ep.method} ${ep.path} (max latency: ${ep.maxLatencyMs}ms)`).join('\n')}

**Artifacts**: Stored in S3 bucket for 30 days

**Requirements**: 13.13, 13.14, 13.15`,
                width: 24,
                height: 8,
            })
        );

        // Output Synthetics information
        new cdk.CfnOutput(this, 'SyntheticsCanaryCount', {
            value: this.syntheticsMonitoring.canaries.length.toString(),
            description: 'Number of active CloudWatch Synthetics canaries',
            exportName: `${environment}-SyntheticsCanaryCount`,
        });

        new cdk.CfnOutput(this, 'SyntheticsArtifactsBucket', {
            value: this.syntheticsMonitoring.artifactsBucket.bucketName,
            description: 'S3 bucket for Synthetics canary artifacts',
            exportName: `${environment}-SyntheticsArtifactsBucket`,
        });
    }

    /**
     * Add VPC Flow Logs network insights monitoring
     * Requirements: 13.16, 13.17, 13.18
     */
    private addVpcFlowLogsMonitoring(environment: string, vpc: ec2.IVpc): void {
        // Create VPC Flow Logs monitoring construct
        this.vpcFlowLogsMonitoring = new VpcFlowLogsMonitoring(this, 'VpcFlowLogsMonitoring', {
            vpc,
            environment,
            criticalAlertTopic: this.criticalAlertTopic,
            warningAlertTopic: this.warningAlertTopic,
            retentionDays: logs.RetentionDays.ONE_MONTH,
            enableS3Archival: true,
            enableAnomalyDetection: true,
            enableSecurityAnalysis: true,
        });

        // Add VPC Flow Logs overview widget to main dashboard
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## VPC Flow Logs Network Insights

**Purpose**: Comprehensive VPC traffic logging and security analysis

**Features**:
- **Traffic Logging**: All VPC traffic recorded to CloudWatch Logs
- **Anomaly Detection**: Automated analysis every 15 minutes
- **Security Analysis**: Threat detection every 10 minutes
- **Long-term Archival**: S3 storage with 7-year retention for compliance

**Detection Capabilities**:
- High traffic volume spikes (> 1GB)
- High connection counts (> 1000 connections)
- Port scanning activity (> 50 different ports)
- Suspicious port access (SSH, RDP, SMB)
- Brute force attempts
- Data exfiltration patterns

**CloudWatch Insights Queries**:
- Top Talkers: Identify highest traffic sources
- Rejected Connections: Analyze blocked traffic
- Port Scanning: Detect scanning activity

**Requirements**: 13.16, 13.17, 13.18`,
                width: 24,
                height: 10,
            })
        );

        // Add link to VPC Flow Logs dashboard
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `### VPC Flow Logs Dashboard

View detailed network insights: [VPC Flow Logs Dashboard](https://console.aws.amazon.com/cloudwatch/home?region=${cdk.Aws.REGION}#dashboards:name=VPCFlowLogs-${environment})

**Log Group**: ${this.vpcFlowLogsMonitoring.flowLogGroup.logGroupName}

**Archival Bucket**: ${this.vpcFlowLogsMonitoring.archivalBucket?.bucketName || 'Not configured'}`,
                width: 24,
                height: 4,
            })
        );

        // Output VPC Flow Logs information
        new cdk.CfnOutput(this, 'VpcFlowLogsLogGroup', {
            value: this.vpcFlowLogsMonitoring.flowLogGroup.logGroupName,
            description: 'CloudWatch Log Group for VPC Flow Logs',
            exportName: `${environment}-VpcFlowLogsLogGroup`,
        });

        if (this.vpcFlowLogsMonitoring.archivalBucket) {
            new cdk.CfnOutput(this, 'VpcFlowLogsArchivalBucket', {
                value: this.vpcFlowLogsMonitoring.archivalBucket.bucketName,
                description: 'S3 bucket for VPC Flow Logs long-term archival',
                exportName: `${environment}-VpcFlowLogsArchivalBucket`,
            });
        }

        new cdk.CfnOutput(this, 'VpcFlowLogsDashboard', {
            value: this.vpcFlowLogsMonitoring.dashboard.dashboardName,
            description: 'CloudWatch Dashboard for VPC Flow Logs',
            exportName: `${environment}-VpcFlowLogsDashboard`,
        });
    }
}
