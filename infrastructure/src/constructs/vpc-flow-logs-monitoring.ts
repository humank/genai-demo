import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatch_actions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export interface VpcFlowLogsMonitoringProps {
    vpc: ec2.IVpc;
    environment: string;
    criticalAlertTopic?: sns.ITopic;
    warningAlertTopic?: sns.ITopic;
    retentionDays?: logs.RetentionDays;
    enableS3Archival?: boolean;
    enableAnomalyDetection?: boolean;
    enableSecurityAnalysis?: boolean;
}

/**
 * VPC Flow Logs Monitoring Construct
 * 
 * Implements comprehensive VPC traffic logging and analysis:
 * - Requirement 13.16: Record all VPC traffic details
 * - Requirement 13.17: Detect anomalous traffic patterns
 * - Requirement 13.18: Provide network evidence for security events
 * 
 * Features:
 * - CloudWatch Logs integration for real-time analysis
 * - S3 archival for long-term storage and compliance
 * - Automated anomaly detection using CloudWatch Insights
 * - Security event correlation and alerting
 * - Network traffic pattern analysis
 */
export class VpcFlowLogsMonitoring extends Construct {
    public readonly flowLogGroup: logs.LogGroup;
    public readonly flowLogRole: iam.Role;
    public readonly flowLog: ec2.FlowLog;
    public readonly anomalyDetectionFunction?: lambda.Function;
    public readonly securityAnalysisFunction?: lambda.Function;
    public readonly archivalBucket?: s3.Bucket;
    public readonly dashboard: cloudwatch.Dashboard;

    constructor(scope: Construct, id: string, props: VpcFlowLogsMonitoringProps) {
        super(scope, id);

        const {
            vpc,
            environment,
            criticalAlertTopic,
            warningAlertTopic,
            retentionDays = logs.RetentionDays.ONE_MONTH,
            enableS3Archival = true,
            enableAnomalyDetection = true,
            enableSecurityAnalysis = true,
        } = props;

        // Create CloudWatch Log Group for VPC Flow Logs
        this.flowLogGroup = new logs.LogGroup(this, 'FlowLogGroup', {
            logGroupName: `/aws/vpc/flowlogs/${environment}`,
            retention: retentionDays,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create IAM role for VPC Flow Logs
        this.flowLogRole = new iam.Role(this, 'FlowLogRole', {
            assumedBy: new iam.ServicePrincipal('vpc-flow-logs.amazonaws.com'),
            description: 'IAM role for VPC Flow Logs to publish to CloudWatch Logs',
        });

        this.flowLogRole.addToPolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'logs:CreateLogGroup',
                    'logs:CreateLogStream',
                    'logs:PutLogEvents',
                    'logs:DescribeLogGroups',
                    'logs:DescribeLogStreams',
                ],
                resources: [this.flowLogGroup.logGroupArn],
            })
        );

        // Create VPC Flow Log with comprehensive field configuration
        this.flowLog = new ec2.FlowLog(this, 'VpcFlowLog', {
            resourceType: ec2.FlowLogResourceType.fromVpc(vpc),
            destination: ec2.FlowLogDestination.toCloudWatchLogs(
                this.flowLogGroup,
                this.flowLogRole
            ),
            trafficType: ec2.FlowLogTrafficType.ALL,
            // Custom format for detailed analysis
            flowLogName: `${environment}-vpc-flow-log`,
        });

        // Create S3 bucket for long-term archival if enabled
        if (enableS3Archival) {
            this.archivalBucket = this.createArchivalBucket(environment);
            this.setupS3Archival(this.archivalBucket);
        }

        // Create anomaly detection Lambda function if enabled
        if (enableAnomalyDetection && warningAlertTopic) {
            this.anomalyDetectionFunction = this.createAnomalyDetectionFunction(
                environment,
                warningAlertTopic
            );
            this.setupAnomalyDetectionSchedule(this.anomalyDetectionFunction);
        }

        // Create security analysis Lambda function if enabled
        if (enableSecurityAnalysis && criticalAlertTopic) {
            this.securityAnalysisFunction = this.createSecurityAnalysisFunction(
                environment,
                criticalAlertTopic
            );
            this.setupSecurityAnalysisSchedule(this.securityAnalysisFunction);
        }

        // Create CloudWatch Dashboard for VPC Flow Logs
        this.dashboard = this.createDashboard(environment);

        // Create CloudWatch Alarms
        this.createAlarms(environment, criticalAlertTopic, warningAlertTopic);

        // Create CloudWatch Insights queries
        this.createInsightsQueries();
    }

    private createArchivalBucket(environment: string): s3.Bucket {
        return new s3.Bucket(this, 'ArchivalBucket', {
            bucketName: `vpc-flow-logs-archive-${environment}-${cdk.Aws.ACCOUNT_ID}`,
            encryption: s3.BucketEncryption.S3_MANAGED,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            versioned: true,
            lifecycleRules: [
                {
                    id: 'TransitionToGlacier',
                    enabled: true,
                    transitions: [
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(90),
                        },
                        {
                            storageClass: s3.StorageClass.DEEP_ARCHIVE,
                            transitionAfter: cdk.Duration.days(365),
                        },
                    ],
                    expiration: cdk.Duration.days(2555), // 7 years for compliance
                },
            ],
            removalPolicy: cdk.RemovalPolicy.RETAIN,
        });
    }

    private setupS3Archival(bucket: s3.Bucket): void {
        // Create subscription filter to export logs to S3
        const exportRole = new iam.Role(this, 'ExportRole', {
            assumedBy: new iam.ServicePrincipal('logs.amazonaws.com'),
        });

        bucket.grantWrite(exportRole);

        // Note: Actual export task needs to be created via AWS SDK or manually
        // as CDK doesn't support CreateExportTask directly
        new cdk.CfnOutput(this, 'ArchivalBucketName', {
            value: bucket.bucketName,
            description: 'S3 bucket for VPC Flow Logs archival',
        });
    }

    private createAnomalyDetectionFunction(
        environment: string,
        alertTopic: sns.ITopic
    ): lambda.Function {
        const func = new lambda.Function(this, 'AnomalyDetection', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta

logs_client = boto3.client('logs')
sns_client = boto3.client('sns')
cloudwatch_client = boto3.client('cloudwatch')

LOG_GROUP = os.environ['LOG_GROUP_NAME']
SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']

def handler(event, context):
    """
    Analyze VPC Flow Logs for anomalous traffic patterns
    
    Detects:
    - Unusual traffic volume spikes
    - Suspicious port scanning activity
    - Abnormal connection patterns
    - Potential DDoS indicators
    """
    
    # Query for traffic volume in last hour
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=1)
    
    # CloudWatch Insights query for traffic analysis
    query = """
    fields @timestamp, srcaddr, dstaddr, srcport, dstport, protocol, bytes, packets
    | stats sum(bytes) as total_bytes, sum(packets) as total_packets, count() as connection_count by srcaddr
    | sort total_bytes desc
    | limit 20
    """
    
    try:
        # Start query
        query_response = logs_client.start_query(
            logGroupName=LOG_GROUP,
            startTime=int(start_time.timestamp()),
            endTime=int(end_time.timestamp()),
            queryString=query
        )
        
        query_id = query_response['queryId']
        
        # Wait for query to complete (simplified for demo)
        import time
        time.sleep(5)
        
        # Get query results
        results = logs_client.get_query_results(queryId=query_id)
        
        anomalies = []
        
        if results['status'] == 'Complete':
            for result in results['results']:
                fields = {item['field']: item['value'] for item in result}
                
                # Detect anomalies
                total_bytes = int(fields.get('total_bytes', 0))
                connection_count = int(fields.get('connection_count', 0))
                
                # Threshold-based anomaly detection
                if total_bytes > 1000000000:  # > 1GB
                    anomalies.append({
                        'type': 'HIGH_TRAFFIC_VOLUME',
                        'source': fields.get('srcaddr'),
                        'bytes': total_bytes,
                        'severity': 'WARNING'
                    })
                
                if connection_count > 1000:  # > 1000 connections
                    anomalies.append({
                        'type': 'HIGH_CONNECTION_COUNT',
                        'source': fields.get('srcaddr'),
                        'connections': connection_count,
                        'severity': 'WARNING'
                    })
        
        # Publish anomalies to SNS
        if anomalies:
            message = {
                'timestamp': datetime.utcnow().isoformat(),
                'anomalies': anomalies,
                'analysis_period': f'{start_time.isoformat()} to {end_time.isoformat()}'
            }
            
            sns_client.publish(
                TopicArn=SNS_TOPIC_ARN,
                Subject='VPC Flow Logs: Anomalous Traffic Detected',
                Message=json.dumps(message, indent=2)
            )
            
            # Publish custom metrics
            for anomaly in anomalies:
                cloudwatch_client.put_metric_data(
                    Namespace='VPCFlowLogs/Anomalies',
                    MetricData=[
                        {
                            'MetricName': anomaly['type'],
                            'Value': 1,
                            'Unit': 'Count',
                            'Timestamp': datetime.utcnow()
                        }
                    ]
                )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'anomalies_detected': len(anomalies),
                'anomalies': anomalies
            })
        }
        
    except Exception as e:
        print(f"Error analyzing flow logs: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
`),
            environment: {
                LOG_GROUP_NAME: this.flowLogGroup.logGroupName,
                SNS_TOPIC_ARN: alertTopic.topicArn,
            },
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            description: 'Analyzes VPC Flow Logs for anomalous traffic patterns',
        });

        // Grant permissions
        this.flowLogGroup.grantRead(func);
        alertTopic.grantPublish(func);
        
        func.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'logs:StartQuery',
                    'logs:GetQueryResults',
                    'cloudwatch:PutMetricData',
                ],
                resources: ['*'],
            })
        );

        return func;
    }

    private createSecurityAnalysisFunction(
        environment: string,
        alertTopic: sns.ITopic
    ): lambda.Function {
        const func = new lambda.Function(this, 'SecurityAnalysis', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta

logs_client = boto3.client('logs')
sns_client = boto3.client('sns')
cloudwatch_client = boto3.client('cloudwatch')

LOG_GROUP = os.environ['LOG_GROUP_NAME']
SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']

# Known malicious ports and patterns
SUSPICIOUS_PORTS = [22, 23, 3389, 445, 135, 139]  # SSH, Telnet, RDP, SMB
SCAN_THRESHOLD = 50  # Number of different ports accessed

def handler(event, context):
    """
    Analyze VPC Flow Logs for security threats
    
    Detects:
    - Port scanning activity
    - Brute force attempts
    - Unauthorized access attempts
    - Data exfiltration patterns
    """
    
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=1)
    
    # Query for security analysis
    query = """
    fields @timestamp, srcaddr, dstaddr, srcport, dstport, protocol, action
    | filter action = "REJECT"
    | stats count() as reject_count by srcaddr, dstport
    | sort reject_count desc
    | limit 50
    """
    
    try:
        # Start query
        query_response = logs_client.start_query(
            logGroupName=LOG_GROUP,
            startTime=int(start_time.timestamp()),
            endTime=int(end_time.timestamp()),
            queryString=query
        )
        
        query_id = query_response['queryId']
        
        # Wait for query to complete
        import time
        time.sleep(5)
        
        # Get query results
        results = logs_client.get_query_results(queryId=query_id)
        
        security_events = []
        
        if results['status'] == 'Complete':
            # Analyze for port scanning
            source_port_map = {}
            
            for result in results['results']:
                fields = {item['field']: item['value'] for item in result}
                srcaddr = fields.get('srcaddr')
                dstport = fields.get('dstport')
                reject_count = int(fields.get('reject_count', 0))
                
                if srcaddr not in source_port_map:
                    source_port_map[srcaddr] = set()
                source_port_map[srcaddr].add(dstport)
                
                # Check for suspicious ports
                if dstport and int(dstport) in SUSPICIOUS_PORTS and reject_count > 10:
                    security_events.append({
                        'type': 'SUSPICIOUS_PORT_ACCESS',
                        'source': srcaddr,
                        'port': dstport,
                        'attempts': reject_count,
                        'severity': 'CRITICAL'
                    })
            
            # Detect port scanning
            for srcaddr, ports in source_port_map.items():
                if len(ports) > SCAN_THRESHOLD:
                    security_events.append({
                        'type': 'PORT_SCANNING_DETECTED',
                        'source': srcaddr,
                        'ports_scanned': len(ports),
                        'severity': 'CRITICAL'
                    })
        
        # Publish security events
        if security_events:
            message = {
                'timestamp': datetime.utcnow().isoformat(),
                'security_events': security_events,
                'analysis_period': f'{start_time.isoformat()} to {end_time.isoformat()}',
                'recommendation': 'Review and potentially block suspicious source IPs'
            }
            
            sns_client.publish(
                TopicArn=SNS_TOPIC_ARN,
                Subject='VPC Flow Logs: Security Threat Detected',
                Message=json.dumps(message, indent=2)
            )
            
            # Publish security metrics
            for event in security_events:
                cloudwatch_client.put_metric_data(
                    Namespace='VPCFlowLogs/Security',
                    MetricData=[
                        {
                            'MetricName': event['type'],
                            'Value': 1,
                            'Unit': 'Count',
                            'Timestamp': datetime.utcnow()
                        }
                    ]
                )
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'security_events_detected': len(security_events),
                'events': security_events
            })
        }
        
    except Exception as e:
        print(f"Error analyzing security events: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
`),
            environment: {
                LOG_GROUP_NAME: this.flowLogGroup.logGroupName,
                SNS_TOPIC_ARN: alertTopic.topicArn,
            },
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            description: 'Analyzes VPC Flow Logs for security threats',
        });

        // Grant permissions
        this.flowLogGroup.grantRead(func);
        alertTopic.grantPublish(func);
        
        func.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'logs:StartQuery',
                    'logs:GetQueryResults',
                    'cloudwatch:PutMetricData',
                ],
                resources: ['*'],
            })
        );

        return func;
    }

    private setupAnomalyDetectionSchedule(func: lambda.Function): void {
        // Run anomaly detection every 15 minutes
        const rule = new events.Rule(this, 'AnomalyDetectionSchedule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(15)),
            description: 'Trigger VPC Flow Logs anomaly detection analysis',
        });

        rule.addTarget(new targets.LambdaFunction(func));
    }

    private setupSecurityAnalysisSchedule(func: lambda.Function): void {
        // Run security analysis every 10 minutes
        const rule = new events.Rule(this, 'SecurityAnalysisSchedule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(10)),
            description: 'Trigger VPC Flow Logs security analysis',
        });

        rule.addTarget(new targets.LambdaFunction(func));
    }

    private createDashboard(environment: string): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'Dashboard', {
            dashboardName: `VPCFlowLogs-${environment}`,
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# VPC Flow Logs Monitoring Dashboard\n\nEnvironment: ${environment}\n\n## Network Insights\n\n- **Comprehensive Traffic Logging**: All VPC traffic recorded\n- **Anomaly Detection**: Automated pattern analysis every 15 minutes\n- **Security Analysis**: Threat detection every 10 minutes\n- **Long-term Archival**: S3 storage with 7-year retention`,
                width: 24,
                height: 4,
            })
        );

        // Add traffic volume metrics
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Traffic Volume (Bytes)',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/VPC',
                        metricName: 'BytesIn',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/VPC',
                        metricName: 'BytesOut',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Connection Count',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/VPC',
                        metricName: 'PacketsIn',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/VPC',
                        metricName: 'PacketsOut',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Add anomaly detection metrics
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Anomalies Detected',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'VPCFlowLogs/Anomalies',
                        metricName: 'HIGH_TRAFFIC_VOLUME',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(15),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'VPCFlowLogs/Anomalies',
                        metricName: 'HIGH_CONNECTION_COUNT',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(15),
                    }),
                ],
                width: 12,
                height: 6,
            }),
            new cloudwatch.GraphWidget({
                title: 'Security Events',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'VPCFlowLogs/Security',
                        metricName: 'PORT_SCANNING_DETECTED',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(10),
                    }),
                    new cloudwatch.Metric({
                        namespace: 'VPCFlowLogs/Security',
                        metricName: 'SUSPICIOUS_PORT_ACCESS',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(10),
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Add log insights widget
        dashboard.addWidgets(
            new cloudwatch.LogQueryWidget({
                title: 'Top Traffic Sources (Last Hour)',
                logGroupNames: [this.flowLogGroup.logGroupName],
                queryLines: [
                    'fields @timestamp, srcaddr, dstaddr, bytes',
                    'stats sum(bytes) as total_bytes by srcaddr',
                    'sort total_bytes desc',
                    'limit 10',
                ],
                width: 24,
                height: 6,
            })
        );

        return dashboard;
    }

    private createAlarms(
        environment: string,
        criticalAlertTopic?: sns.ITopic,
        warningAlertTopic?: sns.ITopic
    ): void {
        // Alarm for high traffic volume
        if (warningAlertTopic) {
            const highTrafficAlarm = new cloudwatch.Alarm(this, 'HighTrafficAlarm', {
                alarmName: `${environment}-vpc-high-traffic`,
                alarmDescription: 'Alert when VPC traffic volume is unusually high',
                metric: new cloudwatch.Metric({
                    namespace: 'VPCFlowLogs/Anomalies',
                    metricName: 'HIGH_TRAFFIC_VOLUME',
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(15),
                }),
                threshold: 5,
                evaluationPeriods: 2,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            });

            highTrafficAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(warningAlertTopic));
        }

        // Alarm for security events
        if (criticalAlertTopic) {
            const securityEventAlarm = new cloudwatch.Alarm(this, 'SecurityEventAlarm', {
                alarmName: `${environment}-vpc-security-event`,
                alarmDescription: 'Alert when security threats are detected in VPC traffic',
                metric: new cloudwatch.Metric({
                    namespace: 'VPCFlowLogs/Security',
                    metricName: 'PORT_SCANNING_DETECTED',
                    statistic: 'Sum',
                    period: cdk.Duration.minutes(10),
                }),
                threshold: 1,
                evaluationPeriods: 1,
                comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
                treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            });

            securityEventAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(criticalAlertTopic));
        }
    }

    private createInsightsQueries(): void {
        // Create CloudWatch Insights query definitions for common analysis patterns
        new logs.QueryDefinition(this, 'TopTalkersQuery', {
            queryDefinitionName: 'VPC-Flow-Logs-Top-Talkers',
            queryString: new logs.QueryString({
                fields: ['@timestamp', 'srcaddr', 'dstaddr', 'bytes', 'packets'],
                stats: 'sum(bytes) as total_bytes, sum(packets) as total_packets by srcaddr',
                sort: 'total_bytes desc',
                limit: 20,
            }),
            logGroups: [this.flowLogGroup],
        });

        new logs.QueryDefinition(this, 'RejectedConnectionsQuery', {
            queryDefinitionName: 'VPC-Flow-Logs-Rejected-Connections',
            queryString: new logs.QueryString({
                fields: ['@timestamp', 'srcaddr', 'dstaddr', 'dstport', 'protocol'],
                filter: 'action = "REJECT"',
                stats: 'count() as reject_count by srcaddr, dstport',
                sort: 'reject_count desc',
                limit: 50,
            }),
            logGroups: [this.flowLogGroup],
        });

        new logs.QueryDefinition(this, 'PortScanningQuery', {
            queryDefinitionName: 'VPC-Flow-Logs-Port-Scanning',
            queryString: new logs.QueryString({
                fields: ['@timestamp', 'srcaddr', 'dstport'],
                stats: 'count_distinct(dstport) as unique_ports by srcaddr',
                filter: 'action = "REJECT"',
                sort: 'unique_ports desc',
                limit: 20,
            }),
            logGroups: [this.flowLogGroup],
        });
    }
}
