import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

/**
 * CloudWatch MSK Dashboard Stack
 * 
 * Creates comprehensive CloudWatch dashboards for MSK monitoring with
 * real-time operations team monitoring, automated insights, and cost tracking.
 * 
 * Features:
 * - Real-time MSK cluster health monitoring
 * - Throughput and latency visualization
 * - Error rate monitoring and analysis
 * - Capacity utilization tracking
 * - Cost monitoring and optimization
 * - Automated CloudWatch Logs Insights queries
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
export class CloudWatchMSKDashboardStack extends cdk.Stack {
  public readonly mskOperationsDashboard: cloudwatch.Dashboard;
  public readonly mskPerformanceDashboard: cloudwatch.Dashboard;
  public readonly mskCostDashboard: cloudwatch.Dashboard;
  public readonly logsInsightsFunction: lambda.Function;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create main operations dashboard
    this.mskOperationsDashboard = this.createOperationsDashboard();

    // Create performance monitoring dashboard
    this.mskPerformanceDashboard = this.createPerformanceDashboard();

    // Create cost monitoring dashboard
    this.mskCostDashboard = this.createCostDashboard();

    // Create CloudWatch Logs Insights automation
    this.logsInsightsFunction = this.createLogsInsightsAutomation();

    // Set up automated reporting
    this.setupAutomatedReporting();

    // Create outputs
    this.createOutputs();
  }

  private createOperationsDashboard(): cloudwatch.Dashboard {
    const dashboard = new cloudwatch.Dashboard(this, 'MSKOperationsDashboard', {
      dashboardName: 'MSK-Operations-Real-Time-Monitoring',
      defaultInterval: cdk.Duration.minutes(5),
    });

    // MSK Cluster Health Section
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '# MSK Cluster Health Overview\nReal-time monitoring for operations team',
        width: 24,
        height: 2,
      })
    );

    // Cluster status widgets
    dashboard.addWidgets(
      new cloudwatch.SingleValueWidget({
        title: 'Active Brokers',
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ActiveControllerCount',
            statistic: 'Maximum',
          }),
        ],
        width: 6,
        height: 6,
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Offline Partitions',
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'OfflinePartitionsCount',
            statistic: 'Maximum',
          }),
        ],
        width: 6,
        height: 6,
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Under Replicated Partitions',
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'UnderReplicatedPartitions',
            statistic: 'Maximum',
          }),
        ],
        width: 6,
        height: 6,
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Global Topic Count',
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'GlobalTopicCount',
            statistic: 'Maximum',
          }),
        ],
        width: 6,
        height: 6,
      })
    );

    // Throughput monitoring section
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '## Throughput Monitoring',
        width: 24,
        height: 1,
      })
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Messages In/Out per Second',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'MessagesInPerSec',
            statistic: 'Sum',
            label: 'Messages In/sec',
          }),
        ],
        right: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'MessagesOutPerSec',
            statistic: 'Sum',
            label: 'Messages Out/sec',
          }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Bytes In/Out per Second',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'BytesInPerSec',
            statistic: 'Sum',
            label: 'Bytes In/sec',
          }),
        ],
        right: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'BytesOutPerSec',
            statistic: 'Sum',
            label: 'Bytes Out/sec',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Latency monitoring section
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '## Latency and Performance',
        width: 24,
        height: 1,
      })
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Producer Request Latency (Percentiles)',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ProducerRequestLatencyAvg',
            statistic: 'Average',
            label: 'Average Latency',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ProducerRequestLatencyAvg',
            statistic: 'p95',
            label: '95th Percentile',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ProducerRequestLatencyAvg',
            statistic: 'p99',
            label: '99th Percentile',
          }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Consumer Lag Analysis',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'EstimatedMaxTimeLag',
            statistic: 'Maximum',
            label: 'Max Time Lag (ms)',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'EstimatedTimeLag',
            statistic: 'Average',
            label: 'Average Time Lag (ms)',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Error monitoring section
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '## Error Rate Monitoring',
        width: 24,
        height: 1,
      })
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Failed Message Counts',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ProducerRequestErrors',
            statistic: 'Sum',
            label: 'Producer Errors',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ConsumerFetchErrors',
            statistic: 'Sum',
            label: 'Consumer Errors',
          }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Retry Pattern Analysis',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ProducerRetries',
            statistic: 'Sum',
            label: 'Producer Retries',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'ConsumerRetries',
            statistic: 'Sum',
            label: 'Consumer Retries',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    return dashboard;
  }

  private createPerformanceDashboard(): cloudwatch.Dashboard {
    const dashboard = new cloudwatch.Dashboard(this, 'MSKPerformanceDashboard', {
      dashboardName: 'MSK-Performance-Deep-Dive',
      defaultInterval: cdk.Duration.minutes(1),
    });

    // Capacity utilization section
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '# MSK Capacity Utilization\nBroker-level resource monitoring',
        width: 24,
        height: 2,
      })
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'CPU Utilization per Broker',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'CpuUser',
            statistic: 'Average',
            label: 'CPU User %',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'CpuSystem',
            statistic: 'Average',
            label: 'CPU System %',
          }),
        ],
        width: 8,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Memory Utilization',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'MemoryUsed',
            statistic: 'Average',
            label: 'Memory Used %',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'MemoryFree',
            statistic: 'Average',
            label: 'Memory Free %',
          }),
        ],
        width: 8,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Disk Usage per Broker',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'KafkaDataLogsDiskUsed',
            statistic: 'Average',
            label: 'Data Logs Disk Used %',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'RootDiskUsed',
            statistic: 'Average',
            label: 'Root Disk Used %',
          }),
        ],
        width: 8,
        height: 6,
      })
    );

    // Network I/O monitoring
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '## Network I/O Performance',
        width: 24,
        height: 1,
      })
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Network Bytes In/Out',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'NetworkRxBytes',
            statistic: 'Sum',
            label: 'Network Rx Bytes',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'NetworkTxBytes',
            statistic: 'Sum',
            label: 'Network Tx Bytes',
          }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Network Packets In/Out',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'NetworkRxPackets',
            statistic: 'Sum',
            label: 'Network Rx Packets',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'NetworkTxPackets',
            statistic: 'Sum',
            label: 'Network Tx Packets',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    return dashboard;
  }

  private createCostDashboard(): cloudwatch.Dashboard {
    const dashboard = new cloudwatch.Dashboard(this, 'MSKCostDashboard', {
      dashboardName: 'MSK-Cost-Monitoring-Optimization',
      defaultInterval: cdk.Duration.hours(1),
    });

    // Cost monitoring widgets
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '# MSK Cost Monitoring\nUsage-based cost tracking and optimization recommendations',
        width: 24,
        height: 2,
      })
    );

    // Usage-based cost tracking
    dashboard.addWidgets(
      new cloudwatch.SingleValueWidget({
        title: 'Estimated Daily Cost',
        metrics: [
          new cloudwatch.MathExpression({
            expression: 'SEARCH(\'{AWS/Billing,Currency} MetricName="EstimatedCharges" Currency="USD" ServiceName="AmazonMSK"\')',
            label: 'Daily MSK Cost (USD)',
          }),
        ],
        width: 6,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Cost Trend Analysis (30 days)',
        left: [
          new cloudwatch.MathExpression({
            expression: 'SEARCH(\'{AWS/Billing,Currency} MetricName="EstimatedCharges" Currency="USD" ServiceName="AmazonMSK"\')',
            label: 'MSK Cost Trend',
          }),
        ],
        width: 18,
        height: 6,
      })
    );

    // Resource utilization for cost optimization
    dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: '## Resource Utilization for Cost Optimization',
        width: 24,
        height: 1,
      })
    );

    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Broker Utilization vs Capacity',
        left: [
          new cloudwatch.MathExpression({
            expression: '(m1 + m2) / 2',
            usingMetrics: {
              m1: new cloudwatch.Metric({
                namespace: 'AWS/Kafka',
                metricName: 'CpuUser',
                statistic: 'Average',
              }),
              m2: new cloudwatch.Metric({
                namespace: 'AWS/Kafka',
                metricName: 'MemoryUsed',
                statistic: 'Average',
              }),
            },
            label: 'Average Resource Utilization %',
          }),
        ],
        width: 12,
        height: 6,
      }),
      new cloudwatch.GraphWidget({
        title: 'Storage Efficiency',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Kafka',
            metricName: 'KafkaDataLogsDiskUsed',
            statistic: 'Average',
            label: 'Storage Utilization %',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    return dashboard;
  }

  private createLogsInsightsAutomation(): lambda.Function {
    // IAM role for Logs Insights Lambda
    const logsInsightsRole = new iam.Role(this, 'LogsInsightsRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      description: 'IAM role for MSK CloudWatch Logs Insights automation',
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
      ],
      inlinePolicies: {
        LogsInsightsPolicy: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'logs:StartQuery',
                'logs:StopQuery',
                'logs:GetQueryResults',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
              ],
              resources: [
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/msk/*`,
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/lambda/msk-*`,
              ],
            }),
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'cloudwatch:PutMetricData',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
              ],
              resources: ['*'],
            }),
          ],
        }),
      },
    });

    // Lambda function for automated Logs Insights queries
    const logsInsightsFunction = new lambda.Function(this, 'MSKLogsInsightsFunction', {
      functionName: 'msk-logs-insights-automation',
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.lambda_handler',
      role: logsInsightsRole,
      timeout: cdk.Duration.minutes(15),
      memorySize: 512,
      description: 'Automated CloudWatch Logs Insights queries for MSK monitoring',
      code: lambda.Code.fromInline(`
import json
import boto3
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any

logs_client = boto3.client('logs')
cloudwatch_client = boto3.client('cloudwatch')

def lambda_handler(event, context):
    """
    Automated CloudWatch Logs Insights queries for MSK monitoring
    """
    try:
        # Define MSK log analysis queries
        queries = {
            'data_flow_analysis': {
                'query': '''
                    fields @timestamp, @message
                    | filter @message like /kafka/
                    | filter @message like /producer|consumer/
                    | stats count() by bin(5m)
                    | sort @timestamp desc
                ''',
                'log_group': '/aws/msk/cluster-logs',
                'description': 'MSK data flow event lifecycle tracking'
            },
            'error_detection': {
                'query': '''
                    fields @timestamp, @message, @logStream
                    | filter @message like /ERROR|WARN|Exception/
                    | filter @message like /kafka/
                    | stats count() by @logStream, bin(5m)
                    | sort @timestamp desc
                    | limit 100
                ''',
                'log_group': '/aws/msk/cluster-logs',
                'description': 'MSK error detection and root cause analysis'
            },
            'consumer_lag_analysis': {
                'query': '''
                    fields @timestamp, @message
                    | filter @message like /consumer.*lag/
                    | parse @message /lag: (?<lag_value>\\d+)/
                    | stats avg(lag_value), max(lag_value) by bin(5m)
                    | sort @timestamp desc
                ''',
                'log_group': '/aws/msk/cluster-logs',
                'description': 'Consumer lag partition-level investigation'
            },
            'security_audit': {
                'query': '''
                    fields @timestamp, @message, sourceIPAddress, userIdentity.type
                    | filter @message like /kafka.*auth|access|permission/
                    | stats count() by sourceIPAddress, userIdentity.type, bin(1h)
                    | sort @timestamp desc
                ''',
                'log_group': '/aws/msk/cluster-logs',
                'description': 'MSK security access pattern analysis'
            },
            'performance_trend': {
                'query': '''
                    fields @timestamp, @message
                    | filter @message like /throughput|latency|performance/
                    | parse @message /throughput: (?<throughput>\\d+)/
                    | parse @message /latency: (?<latency>\\d+)/
                    | stats avg(throughput), avg(latency) by bin(15m)
                    | sort @timestamp desc
                ''',
                'log_group': '/aws/msk/cluster-logs',
                'description': 'MSK performance trend analysis'
            }
        }
        
        results = {}
        
        # Execute each query
        for query_name, query_config in queries.items():
            try:
                result = execute_logs_insights_query(
                    query_config['query'],
                    query_config['log_group'],
                    query_config['description']
                )
                results[query_name] = result
                
                # Publish custom metrics based on results
                publish_custom_metrics(query_name, result)
                
            except Exception as e:
                print(f"Error executing query {query_name}: {str(e)}")
                results[query_name] = {'error': str(e)}
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'MSK Logs Insights analysis completed',
                'results': results,
                'timestamp': datetime.utcnow().isoformat()
            })
        }
        
    except Exception as e:
        print(f"Lambda execution error: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': str(e),
                'timestamp': datetime.utcnow().isoformat()
            })
        }

def execute_logs_insights_query(query: str, log_group: str, description: str) -> Dict[str, Any]:
    """Execute CloudWatch Logs Insights query"""
    
    # Calculate time range (last 1 hour)
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=1)
    
    # Start query
    response = logs_client.start_query(
        logGroupName=log_group,
        startTime=int(start_time.timestamp()),
        endTime=int(end_time.timestamp()),
        queryString=query
    )
    
    query_id = response['queryId']
    
    # Wait for query completion
    max_wait_time = 300  # 5 minutes
    wait_interval = 5
    elapsed_time = 0
    
    while elapsed_time < max_wait_time:
        response = logs_client.get_query_results(queryId=query_id)
        
        if response['status'] == 'Complete':
            return {
                'status': 'success',
                'description': description,
                'results': response['results'],
                'statistics': response.get('statistics', {}),
                'query_id': query_id
            }
        elif response['status'] == 'Failed':
            return {
                'status': 'failed',
                'description': description,
                'error': 'Query execution failed',
                'query_id': query_id
            }
        
        time.sleep(wait_interval)
        elapsed_time += wait_interval
    
    # Timeout
    logs_client.stop_query(queryId=query_id)
    return {
        'status': 'timeout',
        'description': description,
        'error': 'Query execution timeout',
        'query_id': query_id
    }

def publish_custom_metrics(query_name: str, result: Dict[str, Any]) -> None:
    """Publish custom CloudWatch metrics based on query results"""
    
    if result.get('status') != 'success':
        return
    
    try:
        # Extract metrics from query results
        results_count = len(result.get('results', []))
        
        # Publish metric
        cloudwatch_client.put_metric_data(
            Namespace='MSK/LogsInsights',
            MetricData=[
                {
                    'MetricName': f'{query_name}_results_count',
                    'Value': results_count,
                    'Unit': 'Count',
                    'Dimensions': [
                        {
                            'Name': 'QueryType',
                            'Value': query_name
                        }
                    ]
                }
            ]
        )
        
        print(f"Published metric for {query_name}: {results_count} results")
        
    except Exception as e:
        print(f"Error publishing metrics for {query_name}: {str(e)}")
`),
      environment: {
        REGION: this.region,
        ACCOUNT_ID: this.account,
      },
    });

    return logsInsightsFunction;
  }

  private setupAutomatedReporting(): void {
    // EventBridge rule for scheduled execution
    const scheduledRule = new events.Rule(this, 'MSKLogsInsightsSchedule', {
      ruleName: 'msk-logs-insights-schedule',
      description: 'Scheduled execution of MSK Logs Insights analysis',
      schedule: events.Schedule.rate(cdk.Duration.minutes(15)),
    });

    // Add Lambda target
    scheduledRule.addTarget(new targets.LambdaFunction(this.logsInsightsFunction));

    // Create log group for Lambda function
    new logs.LogGroup(this, 'MSKLogsInsightsLogGroup', {
      logGroupName: `/aws/lambda/${this.logsInsightsFunction.functionName}`,
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });
  }

  private createOutputs(): void {
    new cdk.CfnOutput(this, 'MSKOperationsDashboardURL', {
      value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.mskOperationsDashboard.dashboardName}`,
      description: 'MSK Operations Dashboard URL',
      exportName: 'MSK-Operations-Dashboard-URL',
    });

    new cdk.CfnOutput(this, 'MSKPerformanceDashboardURL', {
      value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.mskPerformanceDashboard.dashboardName}`,
      description: 'MSK Performance Dashboard URL',
      exportName: 'MSK-Performance-Dashboard-URL',
    });

    new cdk.CfnOutput(this, 'MSKCostDashboardURL', {
      value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.mskCostDashboard.dashboardName}`,
      description: 'MSK Cost Dashboard URL',
      exportName: 'MSK-Cost-Dashboard-URL',
    });

    new cdk.CfnOutput(this, 'LogsInsightsFunctionArn', {
      value: this.logsInsightsFunction.functionArn,
      description: 'MSK Logs Insights automation Lambda function ARN',
      exportName: 'MSK-Logs-Insights-Function-Arn',
    });
  }
}