import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatch_actions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as aps from 'aws-cdk-lib/aws-aps';
import { Construct } from 'constructs';

export interface ObservabilityStackProps extends cdk.StackProps {
    vpc: ec2.Vpc;
    environment?: string;
    eksClusterName?: string;
}

export class ObservabilityStack extends cdk.Stack {
    public readonly logGroup: logs.LogGroup;
    public readonly dashboard: cloudwatch.Dashboard;
    public readonly criticalAlertTopic: sns.Topic;
    public readonly warningAlertTopic: sns.Topic;
    public readonly prometheusWorkspace: aps.CfnWorkspace;

    constructor(scope: Construct, id: string, props: ObservabilityStackProps) {
        super(scope, id, props);

        const environment = props.environment || 'production';
        const eksClusterName = props.eksClusterName || `${environment}-genai-demo-cluster`;

        // Create CloudWatch Log Group
        this.logGroup = new logs.LogGroup(this, 'ApplicationLogGroup', {
            logGroupName: `/aws/genai-demo/${environment}/application`,
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

        // Create Prometheus Workspace
        this.prometheusWorkspace = new aps.CfnWorkspace(this, 'PrometheusWorkspace', {
            alias: `genai-demo-${environment}-prometheus`,
            tags: [
                { key: 'Environment', value: environment },
                { key: 'Project', value: 'genai-demo' },
                { key: 'Component', value: 'Monitoring' },
            ],
        });

        // Create Container Insights Role
        const containerInsightsRole = new iam.Role(this, 'ContainerInsightsRole', {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchLogsFullAccess'),
            ],
        });

        containerInsightsRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:PutMetricData',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
                'ec2:DescribeVolumes',
                'ec2:DescribeTags',
                'logs:PutLogEvents',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:DescribeLogStreams',
                'logs:DescribeLogGroups',
                'eks:DescribeCluster',
                'eks:ListClusters',
            ],
            resources: ['*'],
        }));

        // Create Container Insights Log Groups
        new logs.LogGroup(this, 'ContainerInsightsLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/performance`,
            retention: logs.RetentionDays.TWO_WEEKS,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        new logs.LogGroup(this, 'EKSApplicationLogGroup', {
            logGroupName: `/aws/containerinsights/${environment}-genai-demo/application`,
            retention: logs.RetentionDays.TWO_WEEKS,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create CloudWatch Dashboard
        this.dashboard = new cloudwatch.Dashboard(this, 'ApplicationDashboard', {
            dashboardName: `GenAI-Demo-${environment}`,
        });

        // Add basic monitoring widgets
        this.dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `# GenAI Demo Monitoring Dashboard\n\nEnvironment: ${environment}\n\n## Monitoring Components\n- CloudWatch Container Insights\n- Amazon Managed Prometheus\n- X-Ray Distributed Tracing`,
                width: 24,
                height: 3,
            })
        );

        // EKS Pod CPU Alarm
        const podHighCpuAlarm = new cloudwatch.Alarm(this, 'PodHighCpuAlarm', {
            alarmName: `${environment}-pod-high-cpu`,
            alarmDescription: 'Alert when pod CPU utilization exceeds 80%',
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'pod_cpu_utilization',
                dimensionsMap: {
                    ClusterName: eksClusterName,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 80,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            actionsEnabled: true,
        });
        
        podHighCpuAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));

        // EKS Pod Memory Alarm
        const podHighMemoryAlarm = new cloudwatch.Alarm(this, 'PodHighMemoryAlarm', {
            alarmName: `${environment}-pod-high-memory`,
            alarmDescription: 'Alert when pod memory utilization exceeds 85%',
            metric: new cloudwatch.Metric({
                namespace: 'ContainerInsights',
                metricName: 'pod_memory_utilization',
                dimensionsMap: {
                    ClusterName: eksClusterName,
                },
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 85,
            evaluationPeriods: 2,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
            actionsEnabled: true,
        });
        
        podHighMemoryAlarm.addAlarmAction(new cloudwatch_actions.SnsAction(this.criticalAlertTopic));

        // X-Ray Role
        const xrayRole = new iam.Role(this, 'XRayRole', {
            assumedBy: new iam.CompositePrincipal(
                new iam.ServicePrincipal('ec2.amazonaws.com'),
                new iam.ServicePrincipal('lambda.amazonaws.com')
            ),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AWSXRayDaemonWriteAccess'),
            ],
        });

        xrayRole.addToPolicy(new iam.PolicyStatement({
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

        // Outputs
        new cdk.CfnOutput(this, 'LogGroupName', {
            value: this.logGroup.logGroupName,
            exportName: `${environment}-LogGroupName`,
        });

        new cdk.CfnOutput(this, 'DashboardURL', {
            value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${this.dashboard.dashboardName}`,
            exportName: `${environment}-DashboardURL`,
        });

        new cdk.CfnOutput(this, 'PrometheusWorkspaceId', {
            value: this.prometheusWorkspace.attrWorkspaceId,
            exportName: `${environment}-PrometheusWorkspaceId`,
        });

        new cdk.CfnOutput(this, 'PrometheusEndpoint', {
            value: this.prometheusWorkspace.attrPrometheusEndpoint,
            exportName: `${environment}-PrometheusEndpoint`,
        });

        new cdk.CfnOutput(this, 'XRayServiceMapURL', {
            value: `https://console.aws.amazon.com/xray/home?region=${this.region}#/service-map`,
            exportName: `${environment}-XRayServiceMapURL`,
        });

        new cdk.CfnOutput(this, 'ContainerInsightsRoleArn', {
            value: containerInsightsRole.roleArn,
            exportName: `${environment}-ContainerInsightsRoleArn`,
        });

        new cdk.CfnOutput(this, 'XRayRoleArn', {
            value: xrayRole.roleArn,
            exportName: `${environment}-XRayRoleArn`,
        });
    }
}
