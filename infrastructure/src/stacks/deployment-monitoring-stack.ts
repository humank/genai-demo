import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as snsSubscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

export interface DeploymentMonitoringStackProps extends cdk.StackProps {
    projectName: string;
    environment: string;
    alertingTopic?: sns.ITopic;
    multiRegionConfig?: {
        enabled: boolean;
        regions: string[];
        primaryRegion: string;
    };
}

/**
 * Deployment Monitoring Stack
 * 
 * Provides comprehensive monitoring for AWS Code Services deployments including:
 * - Deployment success rate tracking
 * - Deployment time monitoring
 * - Deployment failure alerts
 * - Cross-region deployment status
 */
export class DeploymentMonitoringStack extends cdk.Stack {
    public readonly deploymentDashboard: cloudwatch.Dashboard;
    public readonly deploymentMetricsFunction: lambda.Function;
    public readonly deploymentAlertTopic: sns.Topic;

    constructor(scope: Construct, id: string, props: DeploymentMonitoringStackProps) {
        super(scope, id, props);

        const { projectName, environment, alertingTopic, multiRegionConfig } = props;

        // Create SNS topic for deployment alerts
        this.deploymentAlertTopic = new sns.Topic(this, 'DeploymentAlertTopic', {
            displayName: `${projectName}-${environment}-deployment-alerts`,
            topicName: `${projectName}-${environment}-deployment-alerts`,
        });

        // Create Lambda function for deployment metrics collection
        this.deploymentMetricsFunction = this.createDeploymentMetricsFunction(
            projectName,
            environment,
            multiRegionConfig
        );

        // Create CloudWatch Dashboard for deployment monitoring
        this.deploymentDashboard = this.createDeploymentDashboard(
            projectName,
            environment,
            multiRegionConfig
        );

        // Setup EventBridge rules for deployment events
        this.setupDeploymentEventRules(projectName, environment);

        // Create deployment failure alarms
        this.createDeploymentFailureAlarms(projectName, environment);

        // Create deployment time alarms
        this.createDeploymentTimeAlarms(projectName, environment);

        // Create outputs
        this.createOutputs(projectName, environment);
    }

    /**
     * Create Lambda function for collecting deployment metrics
     */
    private createDeploymentMetricsFunction(
        projectName: string,
        environment: string,
        multiRegionConfig?: any
    ): lambda.Function {
        const metricsFunction = new lambda.Function(this, 'DeploymentMetricsFunction', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta
from typing import Dict, List, Any

cloudwatch = boto3.client('cloudwatch')
# GitHub Actions and ArgoCD metrics will be collected via Prometheus/CloudWatch

PROJECT_NAME = os.environ['PROJECT_NAME']
ENVIRONMENT = os.environ['ENVIRONMENT']
REGIONS = os.environ.get('REGIONS', '').split(',')

def handler(event, context):
    """
    Collect and publish deployment metrics to CloudWatch
    """
    try:
        # Collect GitHub Actions metrics (via GitHub API)
        github_metrics = collect_github_actions_metrics()
        
        # Collect ArgoCD metrics (via Prometheus)
        argocd_metrics = collect_argocd_metrics()
        
        # Publish metrics to CloudWatch
        publish_metrics(github_metrics, argocd_metrics)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Deployment metrics collected successfully',
                'github_actions_metrics': github_metrics,
                'argocd_metrics': argocd_metrics
            })
        }
    except Exception as e:
        print(f"Error collecting deployment metrics: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }

def collect_pipeline_metrics() -> Dict[str, Any]:
    """Collect CodePipeline execution metrics"""
    pipeline_name = f"{PROJECT_NAME}-{ENVIRONMENT}-multi-region-pipeline"
    
    try:
        # Get recent pipeline executions
        response = codepipeline.list_pipeline_executions(
            pipelineName=pipeline_name,
            maxResults=10
        )
        
        executions = response.get('pipelineExecutionSummaries', [])
        
        total_executions = len(executions)
        successful_executions = sum(1 for e in executions if e['status'] == 'Succeeded')
        failed_executions = sum(1 for e in executions if e['status'] == 'Failed')
        
        success_rate = (successful_executions / total_executions * 100) if total_executions > 0 else 0
        
        # Calculate average execution time
        execution_times = []
        for execution in executions:
            if 'startTime' in execution and 'lastUpdateTime' in execution:
                duration = (execution['lastUpdateTime'] - execution['startTime']).total_seconds()
                execution_times.append(duration)
        
        avg_execution_time = sum(execution_times) / len(execution_times) if execution_times else 0
        
        return {
            'total_executions': total_executions,
            'successful_executions': successful_executions,
            'failed_executions': failed_executions,
            'success_rate': success_rate,
            'avg_execution_time_seconds': avg_execution_time
        }
    except Exception as e:
        print(f"Error collecting pipeline metrics: {str(e)}")
        return {
            'total_executions': 0,
            'successful_executions': 0,
            'failed_executions': 0,
            'success_rate': 0,
            'avg_execution_time_seconds': 0
        }

def collect_deploy_metrics() -> Dict[str, Any]:
    """Collect CodeDeploy deployment metrics"""
    app_name = f"{PROJECT_NAME}-{ENVIRONMENT}-app"
    
    try:
        # Get deployment groups
        response = codedeploy.list_deployment_groups(applicationName=app_name)
        deployment_groups = response.get('deploymentGroups', [])
        
        all_deployments = []
        for group in deployment_groups:
            deployments = codedeploy.list_deployments(
                applicationName=app_name,
                deploymentGroupName=group,
                includeOnlyStatuses=['Succeeded', 'Failed', 'Stopped'],
                maxResults=10
            )
            all_deployments.extend(deployments.get('deployments', []))
        
        total_deployments = len(all_deployments)
        
        # Get deployment details
        successful_deployments = 0
        failed_deployments = 0
        deployment_times = []
        
        for deployment_id in all_deployments[:10]:  # Limit to recent 10
            try:
                deployment_info = codedeploy.get_deployment(deploymentId=deployment_id)
                deployment = deployment_info['deploymentInfo']
                
                status = deployment.get('status')
                if status == 'Succeeded':
                    successful_deployments += 1
                elif status in ['Failed', 'Stopped']:
                    failed_deployments += 1
                
                # Calculate deployment time
                if 'createTime' in deployment and 'completeTime' in deployment:
                    duration = (deployment['completeTime'] - deployment['createTime']).total_seconds()
                    deployment_times.append(duration)
            except Exception as e:
                print(f"Error getting deployment {deployment_id}: {str(e)}")
        
        success_rate = (successful_deployments / total_deployments * 100) if total_deployments > 0 else 0
        avg_deployment_time = sum(deployment_times) / len(deployment_times) if deployment_times else 0
        
        return {
            'total_deployments': total_deployments,
            'successful_deployments': successful_deployments,
            'failed_deployments': failed_deployments,
            'success_rate': success_rate,
            'avg_deployment_time_seconds': avg_deployment_time
        }
    except Exception as e:
        print(f"Error collecting deploy metrics: {str(e)}")
        return {
            'total_deployments': 0,
            'successful_deployments': 0,
            'failed_deployments': 0,
            'success_rate': 0,
            'avg_deployment_time_seconds': 0
        }

def publish_metrics(pipeline_metrics: Dict, deploy_metrics: Dict):
    """Publish metrics to CloudWatch"""
    namespace = f'{PROJECT_NAME}/Deployment'
    timestamp = datetime.utcnow()
    
    metrics = [
        # Pipeline metrics
        {
            'MetricName': 'PipelineSuccessRate',
            'Value': pipeline_metrics['success_rate'],
            'Unit': 'Percent',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'CodePipeline'}
            ]
        },
        {
            'MetricName': 'PipelineExecutionTime',
            'Value': pipeline_metrics['avg_execution_time_seconds'],
            'Unit': 'Seconds',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'CodePipeline'}
            ]
        },
        {
            'MetricName': 'PipelineFailures',
            'Value': pipeline_metrics['failed_executions'],
            'Unit': 'Count',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'CodePipeline'}
            ]
        },
        # Deploy metrics
        {
            'MetricName': 'DeploymentSuccessRate',
            'Value': deploy_metrics['success_rate'],
            'Unit': 'Percent',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'CodeDeploy'}
            ]
        },
        {
            'MetricName': 'DeploymentTime',
            'Value': deploy_metrics['avg_deployment_time_seconds'],
            'Unit': 'Seconds',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'CodeDeploy'}
            ]
        },
        {
            'MetricName': 'DeploymentFailures',
            'Value': deploy_metrics['failed_deployments'],
            'Unit': 'Count',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'CodeDeploy'}
            ]
        }
    ]
    
    # Publish metrics in batches
    for i in range(0, len(metrics), 20):
        batch = metrics[i:i+20]
        cloudwatch.put_metric_data(
            Namespace=namespace,
            MetricData=batch
        )
    
    print(f"Published {len(metrics)} metrics to CloudWatch")
            `),
            timeout: cdk.Duration.minutes(5),
            environment: {
                PROJECT_NAME: projectName,
                ENVIRONMENT: environment,
                REGIONS: multiRegionConfig?.regions.join(',') || '',
            },
            logRetention: logs.RetentionDays.ONE_WEEK,
        });

        // Grant permissions
        metricsFunction.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'codepipeline:ListPipelines',
                    'codepipeline:GetPipeline',
                    'codepipeline:GetPipelineState',
                    'codepipeline:ListPipelineExecutions',
                    'codepipeline:GetPipelineExecution',
                ],
                resources: ['*'],
            })
        );

        metricsFunction.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'codedeploy:ListApplications',
                    'codedeploy:GetApplication',
                    'codedeploy:ListDeploymentGroups',
                    'codedeploy:ListDeployments',
                    'codedeploy:GetDeployment',
                    'codedeploy:GetDeploymentGroup',
                ],
                resources: ['*'],
            })
        );

        metricsFunction.addToRolePolicy(
            new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'cloudwatch:PutMetricData',
                ],
                resources: ['*'],
            })
        );

        // Schedule the function to run every 5 minutes
        const rule = new events.Rule(this, 'DeploymentMetricsSchedule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(5)),
            description: 'Collect deployment metrics every 5 minutes',
        });

        rule.addTarget(new eventsTargets.LambdaFunction(metricsFunction));

        return metricsFunction;
    }

    /**
     * Create CloudWatch Dashboard for deployment monitoring
     */
    private createDeploymentDashboard(
        projectName: string,
        environment: string,
        multiRegionConfig?: any
    ): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'DeploymentMonitoringDashboard', {
            dashboardName: `${projectName}-${environment}-deployment-monitoring`,
            defaultInterval: cdk.Duration.hours(1),
        });

        // Add header widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# Deployment Monitoring Dashboard

Environment: **${environment}**
Project: **${projectName}**

## Metrics Overview
- **Pipeline Success Rate**: Percentage of successful pipeline executions
- **Deployment Success Rate**: Percentage of successful deployments
- **Deployment Time**: Average time for deployments to complete
- **Failure Tracking**: Real-time monitoring of deployment failures`,
                width: 24,
                height: 4,
            })
        );

        // Pipeline Success Rate Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'CodePipeline Success Rate',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/Deployment`,
                        metricName: 'PipelineSuccessRate',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'CodePipeline',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Success Rate (%)',
                    }),
                ],
                width: 12,
                height: 6,
                leftYAxis: {
                    min: 0,
                    max: 100,
                },
            })
        );

        // Deployment Success Rate Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'CodeDeploy Success Rate',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/Deployment`,
                        metricName: 'DeploymentSuccessRate',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'CodeDeploy',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Success Rate (%)',
                    }),
                ],
                width: 12,
                height: 6,
                leftYAxis: {
                    min: 0,
                    max: 100,
                },
            })
        );

        // Pipeline Execution Time Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Pipeline Execution Time',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/Deployment`,
                        metricName: 'PipelineExecutionTime',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'CodePipeline',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Avg Execution Time (seconds)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Deployment Time Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Deployment Time',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/Deployment`,
                        metricName: 'DeploymentTime',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'CodeDeploy',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Avg Deployment Time (seconds)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Failure Tracking Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Deployment Failures',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/Deployment`,
                        metricName: 'PipelineFailures',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'CodePipeline',
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.hours(1),
                        label: 'Pipeline Failures',
                        color: cloudwatch.Color.RED,
                    }),
                    new cloudwatch.Metric({
                        namespace: `${projectName}/Deployment`,
                        metricName: 'DeploymentFailures',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'CodeDeploy',
                        },
                        statistic: 'Sum',
                        period: cdk.Duration.hours(1),
                        label: 'Deployment Failures',
                        color: cloudwatch.Color.ORANGE,
                    }),
                ],
                width: 24,
                height: 6,
            })
        );

        return dashboard;
    }

    /**
     * Setup EventBridge rules for deployment events
     */
    private setupDeploymentEventRules(projectName: string, environment: string): void {
        // CodePipeline state change events
        const pipelineStateChangeRule = new events.Rule(this, 'PipelineStateChangeRule', {
            eventPattern: {
                source: ['aws.codepipeline'],
                detailType: ['CodePipeline Pipeline Execution State Change'],
                detail: {
                    state: ['FAILED', 'SUCCEEDED'],
                    pipeline: [`${projectName}-${environment}-multi-region-pipeline`],
                },
            },
            description: 'Capture CodePipeline state changes',
        });

        // Create Lambda function to handle pipeline events
        const pipelineEventHandler = new lambda.Function(this, 'PipelineEventHandler', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import os

sns = boto3.client('sns')
TOPIC_ARN = os.environ['TOPIC_ARN']

def handler(event, context):
    detail = event['detail']
    pipeline = detail['pipeline']
    state = detail['state']
    execution_id = detail['execution-id']
    
    message = f"""
Deployment Alert: CodePipeline State Change

Pipeline: {pipeline}
State: {state}
Execution ID: {execution_id}
Time: {event['time']}

{'⚠️ FAILURE DETECTED' if state == 'FAILED' else '✅ SUCCESS'}
"""
    
    sns.publish(
        TopicArn=TOPIC_ARN,
        Subject=f'CodePipeline {state}: {pipeline}',
        Message=message
    )
    
    return {'statusCode': 200}
            `),
            environment: {
                TOPIC_ARN: this.deploymentAlertTopic.topicArn,
            },
            timeout: cdk.Duration.seconds(30),
        });

        this.deploymentAlertTopic.grantPublish(pipelineEventHandler);
        pipelineStateChangeRule.addTarget(new eventsTargets.LambdaFunction(pipelineEventHandler));

        // CodeDeploy state change events
        const deployStateChangeRule = new events.Rule(this, 'DeployStateChangeRule', {
            eventPattern: {
                source: ['aws.codedeploy'],
                detailType: ['CodeDeploy Deployment State-change Notification'],
                detail: {
                    state: ['FAILURE', 'SUCCESS', 'STOPPED'],
                },
            },
            description: 'Capture CodeDeploy state changes',
        });

        // Create Lambda function to handle deploy events
        const deployEventHandler = new lambda.Function(this, 'DeployEventHandler', {
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import os

sns = boto3.client('sns')
TOPIC_ARN = os.environ['TOPIC_ARN']

def handler(event, context):
    detail = event['detail']
    application = detail.get('application', 'Unknown')
    deployment_group = detail.get('deploymentGroup', 'Unknown')
    deployment_id = detail.get('deploymentId', 'Unknown')
    state = detail['state']
    region = detail.get('region', 'Unknown')
    
    message = f"""
Deployment Alert: CodeDeploy State Change

Application: {application}
Deployment Group: {deployment_group}
Deployment ID: {deployment_id}
State: {state}
Region: {region}
Time: {event['time']}

{'⚠️ FAILURE DETECTED' if state == 'FAILURE' else '✅ SUCCESS' if state == 'SUCCESS' else '⏹️ STOPPED'}
"""
    
    sns.publish(
        TopicArn=TOPIC_ARN,
        Subject=f'CodeDeploy {state}: {application}',
        Message=message
    )
    
    return {'statusCode': 200}
            `),
            environment: {
                TOPIC_ARN: this.deploymentAlertTopic.topicArn,
            },
            timeout: cdk.Duration.seconds(30),
        });

        this.deploymentAlertTopic.grantPublish(deployEventHandler);
        deployStateChangeRule.addTarget(new eventsTargets.LambdaFunction(deployEventHandler));
    }

    /**
     * Create alarms for deployment failures
     */
    private createDeploymentFailureAlarms(projectName: string, environment: string): void {
        // Pipeline failure alarm
        const pipelineFailureAlarm = new cloudwatch.Alarm(this, 'PipelineFailureAlarm', {
            alarmName: `${projectName}-${environment}-pipeline-failures`,
            alarmDescription: 'Alert when CodePipeline executions fail',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Deployment`,
                metricName: 'PipelineFailures',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'CodePipeline',
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 1,
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        pipelineFailureAlarm.addAlarmAction(
            new cdk.aws_cloudwatch_actions.SnsAction(this.deploymentAlertTopic)
        );

        // Deployment failure alarm
        const deploymentFailureAlarm = new cloudwatch.Alarm(this, 'DeploymentFailureAlarm', {
            alarmName: `${projectName}-${environment}-deployment-failures`,
            alarmDescription: 'Alert when CodeDeploy deployments fail',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Deployment`,
                metricName: 'DeploymentFailures',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'CodeDeploy',
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 1,
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        deploymentFailureAlarm.addAlarmAction(
            new cdk.aws_cloudwatch_actions.SnsAction(this.deploymentAlertTopic)
        );

        // Low success rate alarm
        const lowSuccessRateAlarm = new cloudwatch.Alarm(this, 'LowSuccessRateAlarm', {
            alarmName: `${projectName}-${environment}-low-deployment-success-rate`,
            alarmDescription: 'Alert when deployment success rate drops below 80%',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Deployment`,
                metricName: 'DeploymentSuccessRate',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'CodeDeploy',
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(15),
            }),
            threshold: 80,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        lowSuccessRateAlarm.addAlarmAction(
            new cdk.aws_cloudwatch_actions.SnsAction(this.deploymentAlertTopic)
        );
    }

    /**
     * Create alarms for deployment time
     */
    private createDeploymentTimeAlarms(projectName: string, environment: string): void {
        // Long deployment time alarm
        const longDeploymentAlarm = new cloudwatch.Alarm(this, 'LongDeploymentTimeAlarm', {
            alarmName: `${projectName}-${environment}-long-deployment-time`,
            alarmDescription: 'Alert when deployment time exceeds 30 minutes',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Deployment`,
                metricName: 'DeploymentTime',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'CodeDeploy',
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 1800, // 30 minutes in seconds
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        longDeploymentAlarm.addAlarmAction(
            new cdk.aws_cloudwatch_actions.SnsAction(this.deploymentAlertTopic)
        );

        // Long pipeline execution alarm
        const longPipelineAlarm = new cloudwatch.Alarm(this, 'LongPipelineExecutionAlarm', {
            alarmName: `${projectName}-${environment}-long-pipeline-execution`,
            alarmDescription: 'Alert when pipeline execution exceeds 60 minutes',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/Deployment`,
                metricName: 'PipelineExecutionTime',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'CodePipeline',
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 3600, // 60 minutes in seconds
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        longPipelineAlarm.addAlarmAction(
            new cdk.aws_cloudwatch_actions.SnsAction(this.deploymentAlertTopic)
        );
    }

    /**
     * Create stack outputs
     */
    private createOutputs(projectName: string, environment: string): void {
        new cdk.CfnOutput(this, 'DeploymentDashboardUrl', {
            value: `https://${this.region}.console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.deploymentDashboard.dashboardName}`,
            description: 'Deployment monitoring dashboard URL',
            exportName: `${projectName}-${environment}-deployment-dashboard-url`,
        });

        new cdk.CfnOutput(this, 'DeploymentAlertTopicArn', {
            value: this.deploymentAlertTopic.topicArn,
            description: 'Deployment alert SNS topic ARN',
            exportName: `${projectName}-${environment}-deployment-alert-topic-arn`,
        });

        new cdk.CfnOutput(this, 'DeploymentMetricsFunctionArn', {
            value: this.deploymentMetricsFunction.functionArn,
            description: 'Deployment metrics collection Lambda function ARN',
            exportName: `${projectName}-${environment}-deployment-metrics-function-arn`,
        });
    }
}
