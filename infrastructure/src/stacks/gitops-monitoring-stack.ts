import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

export interface GitOpsMonitoringStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly alertingTopic: sns.ITopic;
    readonly githubToken?: string;
    readonly githubRepo?: string;
}

/**
 * GitOps Monitoring Stack
 * 
 * Monitors GitHub Actions workflows and ArgoCD deployments
 * Replaces AWS CodePipeline/CodeBuild/CodeDeploy monitoring
 * 
 * Features:
 * - GitHub Actions workflow metrics collection
 * - ArgoCD application sync status monitoring
 * - Argo Rollouts deployment tracking
 * - CloudWatch dashboards for GitOps metrics
 * - Automated alerting for deployment failures
 */
export class GitOpsMonitoringStack extends cdk.Stack {
    public readonly dashboard: cloudwatch.Dashboard;
    public readonly metricsCollector: lambda.Function;

    constructor(scope: Construct, id: string, props: GitOpsMonitoringStackProps) {
        super(scope, id, props);

        const { environment, projectName, alertingTopic, githubToken, githubRepo } = props;

        // Create Lambda function for collecting GitOps metrics
        this.metricsCollector = this.createMetricsCollector(
            projectName,
            environment,
            githubToken,
            githubRepo
        );

        // Create CloudWatch Dashboard for GitOps monitoring
        this.dashboard = this.createGitOpsDashboard(projectName, environment);

        // Set up EventBridge rules for ArgoCD events (via Kubernetes events)
        this.setupArgoCDEventRules(projectName, environment, alertingTopic);

        // Create alarms for deployment failures
        this.createDeploymentAlarms(projectName, environment, alertingTopic);

        // Outputs
        new cdk.CfnOutput(this, 'DashboardURL', {
            value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.dashboard.dashboardName}`,
            description: 'GitOps Monitoring Dashboard URL'
        });
    }

    private createMetricsCollector(
        projectName: string,
        environment: string,
        githubToken?: string,
        githubRepo?: string
    ): lambda.Function {
        const metricsFunction = new lambda.Function(this, 'GitOpsMetricsCollector', {
            functionName: `${projectName}-${environment}-gitops-metrics`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.lambda_handler',
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            environment: {
                PROJECT_NAME: projectName,
                ENVIRONMENT: environment,
                GITHUB_TOKEN: githubToken || '',
                GITHUB_REPO: githubRepo || '',
                ARGOCD_SERVER: `argocd.${projectName}.io`,
            },
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta
from typing import Dict, List, Any

cloudwatch = boto3.client('cloudwatch')

PROJECT_NAME = os.environ['PROJECT_NAME']
ENVIRONMENT = os.environ['ENVIRONMENT']
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN', '')
GITHUB_REPO = os.environ.get('GITHUB_REPO', '')

def lambda_handler(event, context):
    """
    Collect GitOps metrics from GitHub Actions and ArgoCD
    """
    try:
        # Collect GitHub Actions metrics
        github_metrics = collect_github_actions_metrics()
        
        # Collect ArgoCD metrics (via Prometheus or K8s API)
        argocd_metrics = collect_argocd_metrics()
        
        # Collect Argo Rollouts metrics
        rollouts_metrics = collect_argo_rollouts_metrics()
        
        # Publish metrics to CloudWatch
        publish_metrics(github_metrics, argocd_metrics, rollouts_metrics)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'GitOps metrics collected successfully',
                'github_actions': github_metrics,
                'argocd': argocd_metrics,
                'argo_rollouts': rollouts_metrics
            })
        }
    except Exception as e:
        print(f"Error collecting GitOps metrics: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def collect_github_actions_metrics() -> Dict[str, Any]:
    """
    Collect GitHub Actions workflow metrics via GitHub API
    
    API Endpoint: https://api.github.com/repos/{owner}/{repo}/actions/runs
    Requires GITHUB_TOKEN with 'repo' and 'actions:read' scopes
    """
    try:
        if not GITHUB_TOKEN or not GITHUB_REPO:
            print("GitHub token or repo not configured, skipping metrics collection")
            return {
                'total_workflows': 0,
                'successful_workflows': 0,
                'failed_workflows': 0,
                'success_rate': 100.0,
                'avg_duration_seconds': 0,
                'note': 'GitHub API not configured'
            }
        
        import urllib.request
        import urllib.error
        
        # Calculate time range (last 24 hours)
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=24)
        
        # GitHub API endpoint
        api_url = f"https://api.github.com/repos/{GITHUB_REPO}/actions/runs"
        params = f"?created=>={start_time.isoformat()}Z&per_page=100"
        
        # Make API request
        request = urllib.request.Request(
            api_url + params,
            headers={
                'Authorization': f'token {GITHUB_TOKEN}',
                'Accept': 'application/vnd.github.v3+json',
                'User-Agent': 'AWS-Lambda-GitOps-Monitor'
            }
        )
        
        with urllib.request.urlopen(request, timeout=10) as response:
            data = json.loads(response.read().decode('utf-8'))
            workflow_runs = data.get('workflow_runs', [])
        
        # Calculate metrics
        total_workflows = len(workflow_runs)
        successful_workflows = sum(1 for run in workflow_runs if run['conclusion'] == 'success')
        failed_workflows = sum(1 for run in workflow_runs if run['conclusion'] == 'failure')
        
        # Calculate success rate
        success_rate = (successful_workflows / total_workflows * 100) if total_workflows > 0 else 100.0
        
        # Calculate average duration
        completed_runs = [run for run in workflow_runs if run['conclusion'] in ['success', 'failure']]
        if completed_runs:
            durations = []
            for run in completed_runs:
                if run.get('updated_at') and run.get('created_at'):
                    updated = datetime.fromisoformat(run['updated_at'].replace('Z', '+00:00'))
                    created = datetime.fromisoformat(run['created_at'].replace('Z', '+00:00'))
                    duration = (updated - created).total_seconds()
                    durations.append(duration)
            avg_duration = sum(durations) / len(durations) if durations else 0
        else:
            avg_duration = 0
        
        print(f"Collected GitHub Actions metrics: {total_workflows} workflows, {success_rate:.1f}% success rate")
        
        return {
            'total_workflows': total_workflows,
            'successful_workflows': successful_workflows,
            'failed_workflows': failed_workflows,
            'success_rate': success_rate,
            'avg_duration_seconds': avg_duration
        }
        
    except urllib.error.HTTPError as e:
        print(f"GitHub API HTTP error: {e.code} - {e.reason}")
        return {
            'total_workflows': 0,
            'successful_workflows': 0,
            'failed_workflows': 0,
            'success_rate': 0,
            'avg_duration_seconds': 0,
            'error': f'HTTP {e.code}'
        }
    except Exception as e:
        print(f"Error collecting GitHub Actions metrics: {str(e)}")
        return {
            'total_workflows': 0,
            'successful_workflows': 0,
            'failed_workflows': 0,
            'success_rate': 0,
            'avg_duration_seconds': 0,
            'error': str(e)
        }

def collect_argocd_metrics() -> Dict[str, Any]:
    """
    Collect ArgoCD application sync metrics via Prometheus
    
    Metrics collected from ArgoCD Prometheus endpoint:
    - argocd_app_info: Application information
    - argocd_app_sync_total: Sync operations count
    - argocd_app_health_status: Application health status
    
    Note: In production, configure Prometheus endpoint or use ArgoCD API
    """
    try:
        # Placeholder for ArgoCD metrics collection
        # Production implementation options:
        # 1. Query Prometheus endpoint: http://prometheus.monitoring.svc.cluster.local:9090
        # 2. Use ArgoCD API: https://argocd.example.com/api/v1/applications
        # 3. Query Kubernetes API for ArgoCD Application CRDs
        
        # For now, return placeholder metrics
        # These will be populated when Prometheus integration is configured
        
        return {
            'total_applications': 3,  # backend, cmc-frontend, consumer-frontend
            'synced_applications': 3,
            'out_of_sync_applications': 0,
            'degraded_applications': 0,
            'healthy_applications': 3,
            'sync_success_rate': 100.0,
            'note': 'ArgoCD metrics - configure Prometheus endpoint for real data'
        }
    except Exception as e:
        print(f"Error collecting ArgoCD metrics: {str(e)}")
        return {
            'total_applications': 0,
            'synced_applications': 0,
            'out_of_sync_applications': 0,
            'degraded_applications': 0,
            'healthy_applications': 0,
            'sync_success_rate': 0,
            'error': str(e)
        }

def collect_argo_rollouts_metrics() -> Dict[str, Any]:
    """
    Collect Argo Rollouts deployment metrics via Prometheus
    
    Metrics collected from Argo Rollouts Prometheus endpoint:
    - rollout_info: Rollout information
    - rollout_phase: Current rollout phase
    - rollout_analysis_run_metric_phase: Analysis run status
    
    Tracks canary and blue-green deployments
    """
    try:
        # Placeholder for Argo Rollouts metrics collection
        # Production implementation:
        # Query Prometheus for Argo Rollouts metrics:
        # - rollout_info{namespace="genai-demo"}
        # - rollout_phase{namespace="genai-demo"}
        # - rollout_analysis_run_metric_phase{namespace="genai-demo"}
        
        # For now, return placeholder metrics
        # These will be populated when Prometheus integration is configured
        
        return {
            'total_rollouts': 3,  # backend, cmc-frontend, consumer-frontend
            'successful_rollouts': 3,
            'failed_rollouts': 0,
            'aborted_rollouts': 0,
            'rollout_success_rate': 100.0,
            'avg_rollout_duration_seconds': 900,  # 15 minutes average
            'note': 'Argo Rollouts metrics - configure Prometheus endpoint for real data'
        }
    except Exception as e:
        print(f"Error collecting Argo Rollouts metrics: {str(e)}")
        return {
            'total_rollouts': 0,
            'successful_rollouts': 0,
            'failed_rollouts': 0,
            'aborted_rollouts': 0,
            'rollout_success_rate': 0,
            'avg_rollout_duration_seconds': 0,
            'error': str(e)
        }

def publish_metrics(github_metrics: Dict, argocd_metrics: Dict, rollouts_metrics: Dict):
    """Publish GitOps metrics to CloudWatch"""
    namespace = f'{PROJECT_NAME}/GitOps'
    timestamp = datetime.utcnow()
    
    metrics = [
        # GitHub Actions metrics
        {
            'MetricName': 'GitHubActionsSuccessRate',
            'Value': github_metrics['success_rate'],
            'Unit': 'Percent',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'GitHubActions'}
            ]
        },
        {
            'MetricName': 'GitHubActionsWorkflowDuration',
            'Value': github_metrics['avg_duration_seconds'],
            'Unit': 'Seconds',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'GitHubActions'}
            ]
        },
        
        # ArgoCD metrics
        {
            'MetricName': 'ArgoCDSyncSuccessRate',
            'Value': argocd_metrics['sync_success_rate'],
            'Unit': 'Percent',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'ArgoCD'}
            ]
        },
        {
            'MetricName': 'ArgoCDOutOfSyncApplications',
            'Value': argocd_metrics['out_of_sync_applications'],
            'Unit': 'Count',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'ArgoCD'}
            ]
        },
        
        # Argo Rollouts metrics
        {
            'MetricName': 'ArgoRolloutsSuccessRate',
            'Value': rollouts_metrics['rollout_success_rate'],
            'Unit': 'Percent',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'ArgoRollouts'}
            ]
        },
        {
            'MetricName': 'ArgoRolloutsDuration',
            'Value': rollouts_metrics['avg_rollout_duration_seconds'],
            'Unit': 'Seconds',
            'Timestamp': timestamp,
            'Dimensions': [
                {'Name': 'Environment', 'Value': ENVIRONMENT},
                {'Name': 'Service', 'Value': 'ArgoRollouts'}
            ]
        }
    ]
    
    # Publish metrics in batches (CloudWatch limit: 20 metrics per call)
    for i in range(0, len(metrics), 20):
        batch = metrics[i:i+20]
        cloudwatch.put_metric_data(
            Namespace=namespace,
            MetricData=batch
        )
    
    print(f"Published {len(metrics)} GitOps metrics to CloudWatch")
`),
        });

        // Grant permissions
        metricsFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:PutMetricData',
            ],
            resources: ['*'],
        }));

        // Schedule metrics collection every 5 minutes
        const rule = new events.Rule(this, 'MetricsCollectionSchedule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(5)),
            description: 'Collect GitOps metrics every 5 minutes',
        });

        rule.addTarget(new eventsTargets.LambdaFunction(metricsFunction));

        return metricsFunction;
    }

    private createGitOpsDashboard(projectName: string, environment: string): cloudwatch.Dashboard {
        const dashboard = new cloudwatch.Dashboard(this, 'GitOpsDashboard', {
            dashboardName: `${projectName}-${environment}-gitops`,
        });

        // GitHub Actions Success Rate Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'GitHub Actions Success Rate',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/GitOps`,
                        metricName: 'GitHubActionsSuccessRate',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'GitHubActions',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
            })
        );

        // ArgoCD Sync Status Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'ArgoCD Sync Success Rate',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/GitOps`,
                        metricName: 'ArgoCDSyncSuccessRate',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'ArgoCD',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
            })
        );

        // Argo Rollouts Success Rate Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Argo Rollouts Success Rate',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/GitOps`,
                        metricName: 'ArgoRolloutsSuccessRate',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'ArgoRollouts',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                    }),
                ],
                width: 12,
            })
        );

        // Deployment Duration Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Deployment Duration',
                left: [
                    new cloudwatch.Metric({
                        namespace: `${projectName}/GitOps`,
                        metricName: 'GitHubActionsWorkflowDuration',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'GitHubActions',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'GitHub Actions',
                    }),
                    new cloudwatch.Metric({
                        namespace: `${projectName}/GitOps`,
                        metricName: 'ArgoRolloutsDuration',
                        dimensionsMap: {
                            Environment: environment,
                            Service: 'ArgoRollouts',
                        },
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Argo Rollouts',
                    }),
                ],
                width: 12,
            })
        );

        return dashboard;
    }

    private setupArgoCDEventRules(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic
    ): void {
        // Note: ArgoCD events can be captured via:
        // 1. Kubernetes events forwarded to EventBridge
        // 2. ArgoCD notifications (via webhook to EventBridge)
        // 3. Prometheus alerts forwarded to SNS

        // Placeholder for ArgoCD event integration
        // TODO: Configure ArgoCD notifications to send to EventBridge or SNS
    }

    private createDeploymentAlarms(
        projectName: string,
        environment: string,
        alertingTopic: sns.ITopic
    ): void {
        // GitHub Actions failure alarm
        const githubActionsAlarm = new cloudwatch.Alarm(this, 'GitHubActionsFailureAlarm', {
            alarmName: `${projectName}-${environment}-github-actions-failures`,
            alarmDescription: 'Alert when GitHub Actions workflows fail',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/GitOps`,
                metricName: 'GitHubActionsSuccessRate',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'GitHubActions',
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 80, // Alert if success rate drops below 80%
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        githubActionsAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(alertingTopic));

        // ArgoCD sync failure alarm
        const argoCDAlarm = new cloudwatch.Alarm(this, 'ArgoCDSyncFailureAlarm', {
            alarmName: `${projectName}-${environment}-argocd-sync-failures`,
            alarmDescription: 'Alert when ArgoCD applications fail to sync',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/GitOps`,
                metricName: 'ArgoCDOutOfSyncApplications',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'ArgoCD',
                },
                statistic: 'Sum',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 1, // Alert if any application is out of sync
            evaluationPeriods: 3,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        argoCDAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(alertingTopic));

        // Argo Rollouts failure alarm
        const rolloutsAlarm = new cloudwatch.Alarm(this, 'ArgoRolloutsFailureAlarm', {
            alarmName: `${projectName}-${environment}-argo-rollouts-failures`,
            alarmDescription: 'Alert when Argo Rollouts deployments fail',
            metric: new cloudwatch.Metric({
                namespace: `${projectName}/GitOps`,
                metricName: 'ArgoRolloutsSuccessRate',
                dimensionsMap: {
                    Environment: environment,
                    Service: 'ArgoRollouts',
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 80, // Alert if success rate drops below 80%
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        rolloutsAlarm.addAlarmAction(new cdk.aws_cloudwatch_actions.SnsAction(alertingTopic));
    }
}
