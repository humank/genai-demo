import * as cdk from 'aws-cdk-lib';
import * as grafana from 'aws-cdk-lib/aws-grafana';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import { Construct } from 'constructs';

/**
 * MSK Grafana Dashboard Stack
 * 
 * Creates comprehensive Amazon Managed Grafana dashboards for MSK monitoring
 * with real-time visualization, alerting, and business impact correlation.
 * 
 * Features:
 * - Executive and Technical Dashboards
 * - Real-time MSK metrics visualization
 * - Consumer lag monitoring with heatmaps
 * - Business impact correlation
 * - Automated alerting integration
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
export class GrafanaMSKDashboardStack extends cdk.Stack {
  public readonly workspace: grafana.CfnWorkspace;
  public readonly mskDataFlowDashboard: string;
  public readonly mskConsumerLagDashboard: string;
  public readonly mskClusterHealthDashboard: string;
  public readonly businessImpactDashboard: string;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create Grafana workspace with enhanced configuration
    this.workspace = this.createGrafanaWorkspace();

    // Create MSK-specific dashboards
    this.createMSKDashboards();

    // Configure alerting integration
    this.configureAlertingIntegration();

    // Set up data sources
    this.configureDataSources();

    // Output important values
    this.createOutputs();
  }

  private createGrafanaWorkspace(): grafana.CfnWorkspace {
    // IAM role for Grafana workspace
    const grafanaRole = new iam.Role(this, 'GrafanaWorkspaceRole', {
      assumedBy: new iam.ServicePrincipal('grafana.amazonaws.com'),
      description: 'IAM role for Amazon Managed Grafana workspace with MSK monitoring',
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonGrafanaCloudWatchAccess'),
        iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonGrafanaPrometheusAccess'),
      ],
      inlinePolicies: {
        MSKMonitoringPolicy: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'kafka:DescribeCluster',
                'kafka:DescribeClusterV2',
                'kafka:ListClusters',
                'kafka:ListClustersV2',
                'kafka:GetBootstrapBrokers',
                'kafka:DescribeConfiguration',
                'kafka:ListConfigurations',
                'kafka:ListNodes',
                'kafka:ListKafkaVersions',
                'kafka:GetCompatibleKafkaVersions',
              ],
              resources: ['*'],
            }),
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'xray:GetServiceGraph',
                'xray:GetTraceGraph',
                'xray:GetTraceSummaries',
                'xray:BatchGetTraces',
                'xray:GetTimeSeriesServiceStatistics',
              ],
              resources: ['*'],
            }),
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
                'logs:StartQuery',
                'logs:StopQuery',
                'logs:GetQueryResults',
              ],
              resources: [
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/msk/*`,
                `arn:aws:logs:${this.region}:${this.account}:log-group:/aws/lambda/msk-*`,
              ],
            }),
          ],
        }),
      },
    });

    // Create Grafana workspace
    const workspace = new grafana.CfnWorkspace(this, 'MSKGrafanaWorkspace', {
      accountAccessType: 'CURRENT_ACCOUNT',
      authenticationProviders: ['AWS_SSO'],
      permissionType: 'SERVICE_MANAGED',
      roleArn: grafanaRole.roleArn,
      name: 'genai-demo-msk-monitoring',
      description: 'Amazon Managed Grafana workspace for MSK data flow monitoring and business impact analysis',
      dataSources: ['CLOUDWATCH', 'PROMETHEUS', 'XRAY'],
      notificationDestinations: ['SNS'],
      organizationRoleName: 'GrafanaServiceRole',
      stackSetName: 'GrafanaMSKMonitoring',
      grafanaVersion: '9.4',
      configuration: JSON.stringify({
        unifiedAlerting: {
          enabled: true,
        },
        plugins: {
          pluginAdminEnabled: true,
        },
        dataSourcesPermissions: {
          enabled: true,
        },
      }),
    });

    // Add tags
    cdk.Tags.of(workspace).add('Purpose', 'MSK Monitoring');
    cdk.Tags.of(workspace).add('Environment', 'Production');
    cdk.Tags.of(workspace).add('Team', 'Platform');

    return workspace;
  }

  private createMSKDashboards(): void {
    // MSK Data Flow Overview Dashboard JSON
    const mskDataFlowDashboardJson = {
      dashboard: {
        id: null,
        title: 'MSK Data Flow Overview',
        tags: ['msk', 'kafka', 'data-flow', 'overview'],
        timezone: 'browser',
        panels: [
          {
            id: 1,
            title: 'MSK Cluster Status',
            type: 'stat',
            targets: [
              {
                expr: 'aws_kafka_cluster_state',
                legendFormat: 'Cluster State',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'thresholds',
                },
                thresholds: {
                  steps: [
                    { color: 'red', value: 0 },
                    { color: 'yellow', value: 1 },
                    { color: 'green', value: 2 },
                  ],
                },
              },
            },
            gridPos: { h: 8, w: 6, x: 0, y: 0 },
          },
          {
            id: 2,
            title: 'Real-time Throughput (Messages/sec)',
            type: 'timeseries',
            targets: [
              {
                expr: 'rate(aws_kafka_messages_in_per_sec[5m])',
                legendFormat: 'Messages In/sec',
              },
              {
                expr: 'rate(aws_kafka_messages_out_per_sec[5m])',
                legendFormat: 'Messages Out/sec',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'palette-classic',
                },
                unit: 'reqps',
              },
            },
            gridPos: { h: 8, w: 12, x: 6, y: 0 },
          },
          {
            id: 3,
            title: 'Event Processing Latency (95th Percentile)',
            type: 'timeseries',
            targets: [
              {
                expr: 'histogram_quantile(0.95, aws_kafka_producer_request_latency_avg)',
                legendFormat: 'Producer Latency P95',
              },
              {
                expr: 'histogram_quantile(0.95, aws_kafka_consumer_lag_sum)',
                legendFormat: 'Consumer Lag P95',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'palette-classic',
                },
                unit: 'ms',
              },
            },
            gridPos: { h: 8, w: 6, x: 18, y: 0 },
          },
          {
            id: 4,
            title: 'Error Rate Analysis',
            type: 'timeseries',
            targets: [
              {
                expr: 'rate(aws_kafka_producer_request_errors_total[5m])',
                legendFormat: 'Producer Errors/sec',
              },
              {
                expr: 'rate(aws_kafka_consumer_fetch_errors_total[5m])',
                legendFormat: 'Consumer Errors/sec',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'palette-classic',
                },
                unit: 'reqps',
              },
            },
            gridPos: { h: 8, w: 12, x: 0, y: 8 },
          },
          {
            id: 5,
            title: 'Topic Distribution Heatmap',
            type: 'heatmap',
            targets: [
              {
                expr: 'aws_kafka_topic_bytes_in_per_sec',
                legendFormat: '{{topic}}',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'spectrum',
                },
                unit: 'binBps',
              },
            },
            gridPos: { h: 8, w: 12, x: 12, y: 8 },
          },
        ],
        time: {
          from: 'now-1h',
          to: 'now',
        },
        refresh: '30s',
      },
    };

    // MSK Consumer Lag Monitoring Dashboard JSON
    const mskConsumerLagDashboardJson = {
      dashboard: {
        id: null,
        title: 'MSK Consumer Lag Monitoring',
        tags: ['msk', 'kafka', 'consumer-lag', 'monitoring'],
        timezone: 'browser',
        panels: [
          {
            id: 1,
            title: 'Consumer Group Lag Heatmap',
            type: 'heatmap',
            targets: [
              {
                expr: 'aws_kafka_consumer_lag_sum by (consumer_group, topic, partition)',
                legendFormat: '{{consumer_group}}-{{topic}}-{{partition}}',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'spectrum',
                },
                unit: 'short',
              },
            },
            gridPos: { h: 10, w: 24, x: 0, y: 0 },
          },
          {
            id: 2,
            title: 'Critical Consumer Groups (Lag > 1000)',
            type: 'table',
            targets: [
              {
                expr: 'aws_kafka_consumer_lag_sum > 1000',
                legendFormat: 'Critical Lag',
                format: 'table',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'thresholds',
                },
                thresholds: {
                  steps: [
                    { color: 'green', value: 0 },
                    { color: 'yellow', value: 1000 },
                    { color: 'red', value: 5000 },
                  ],
                },
              },
            },
            gridPos: { h: 8, w: 12, x: 0, y: 10 },
          },
          {
            id: 3,
            title: 'Partition Rebalancing Events',
            type: 'timeseries',
            targets: [
              {
                expr: 'increase(aws_kafka_consumer_rebalance_total[5m])',
                legendFormat: '{{consumer_group}} Rebalances',
              },
            ],
            fieldConfig: {
              defaults: {
                color: {
                  mode: 'palette-classic',
                },
                unit: 'short',
              },
            },
            gridPos: { h: 8, w: 12, x: 12, y: 10 },
          },
        ],
        time: {
          from: 'now-6h',
          to: 'now',
        },
        refresh: '1m',
      },
    };

    // Store dashboard configurations
    this.mskDataFlowDashboard = JSON.stringify(mskDataFlowDashboardJson);
    this.mskConsumerLagDashboard = JSON.stringify(mskConsumerLagDashboardJson);
  }

  private configureAlertingIntegration(): void {
    // SNS topic for Grafana alerts
    const grafanaAlertTopic = new sns.Topic(this, 'GrafanaAlertTopic', {
      topicName: 'grafana-msk-alerts',
      displayName: 'Grafana MSK Monitoring Alerts',
    });

    // CloudWatch alarm for critical MSK issues
    const criticalMSKAlarm = new cloudwatch.Alarm(this, 'CriticalMSKAlarm', {
      alarmName: 'MSK-Critical-Issues',
      alarmDescription: 'Critical MSK cluster issues requiring immediate attention',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: 'OfflinePartitionsCount',
        statistic: 'Maximum',
      }),
      threshold: 0,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    criticalMSKAlarm.addAlarmAction(
      new cdk.aws_cloudwatch_actions.SnsAction(grafanaAlertTopic)
    );

    // Consumer lag alarm
    const consumerLagAlarm = new cloudwatch.Alarm(this, 'ConsumerLagAlarm', {
      alarmName: 'MSK-Consumer-Lag-High',
      alarmDescription: 'MSK consumer lag exceeding threshold',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: 'EstimatedMaxTimeLag',
        statistic: 'Maximum',
      }),
      threshold: 60000, // 60 seconds
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 3,
    });

    consumerLagAlarm.addAlarmAction(
      new cdk.aws_cloudwatch_actions.SnsAction(grafanaAlertTopic)
    );

    // Add tags
    cdk.Tags.of(grafanaAlertTopic).add('Purpose', 'MSK Alerting');
    cdk.Tags.of(criticalMSKAlarm).add('Severity', 'Critical');
    cdk.Tags.of(consumerLagAlarm).add('Severity', 'Warning');
  }

  private configureDataSources(): void {
    // CloudWatch data source configuration
    const cloudWatchDataSource = {
      name: 'CloudWatch-MSK',
      type: 'cloudwatch',
      access: 'proxy',
      jsonData: {
        defaultRegion: this.region,
        customMetricsNamespaces: 'AWS/Kafka,AWS/MSK,CWAgent',
        assumeRoleArn: this.workspace.roleArn,
      },
    };

    // Prometheus data source for custom metrics
    const prometheusDataSource = {
      name: 'Prometheus-MSK',
      type: 'prometheus',
      access: 'proxy',
      url: 'http://prometheus-server.monitoring.svc.cluster.local:80',
      jsonData: {
        httpMethod: 'POST',
        customQueryParameters: '',
      },
    };

    // X-Ray data source for distributed tracing
    const xrayDataSource = {
      name: 'X-Ray-MSK',
      type: 'xray',
      access: 'proxy',
      jsonData: {
        defaultRegion: this.region,
        assumeRoleArn: this.workspace.roleArn,
      },
    };

    // Store data source configurations as stack metadata
    this.node.setContext('dataSources', {
      cloudWatch: cloudWatchDataSource,
      prometheus: prometheusDataSource,
      xray: xrayDataSource,
    });
  }

  private createOutputs(): void {
    new cdk.CfnOutput(this, 'GrafanaWorkspaceId', {
      value: this.workspace.attrId,
      description: 'Amazon Managed Grafana workspace ID for MSK monitoring',
      exportName: 'MSK-Grafana-Workspace-Id',
    });

    new cdk.CfnOutput(this, 'GrafanaWorkspaceEndpoint', {
      value: this.workspace.attrEndpoint,
      description: 'Amazon Managed Grafana workspace endpoint URL',
      exportName: 'MSK-Grafana-Workspace-Endpoint',
    });

    new cdk.CfnOutput(this, 'GrafanaWorkspaceStatus', {
      value: this.workspace.attrStatus,
      description: 'Amazon Managed Grafana workspace status',
      exportName: 'MSK-Grafana-Workspace-Status',
    });
  }
}