import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import { Construct } from 'constructs';

export interface CostDashboardStackProps extends cdk.StackProps {
  /**
   * Dashboard name
   * @default 'GenAIDemo-Cost-Management'
   */
  readonly dashboardName?: string;
}

/**
 * Cost Dashboard Stack
 * 
 * Creates CloudWatch Dashboard for cost monitoring:
 * - Estimated charges tracking
 * - Regional cost comparison (Taiwan vs Japan)
 * - Service-level cost breakdown
 * - Budget utilization tracking
 * 
 * Architecture: AWS Native CloudWatch Dashboard
 * Integration: AWS Billing metrics + Cost Management Stack
 */
export class CostDashboardStack extends cdk.Stack {
  public readonly dashboard: cloudwatch.Dashboard;

  constructor(scope: Construct, id: string, props?: CostDashboardStackProps) {
    super(scope, id, props);

    const dashboardName = props?.dashboardName ?? 'GenAIDemo-Cost-Management';

    // ============================================
    // Create CloudWatch Dashboard
    // ============================================
    this.dashboard = new cloudwatch.Dashboard(this, 'CostManagementDashboard', {
      dashboardName,
      periodOverride: cloudwatch.PeriodOverride.AUTO,
    });

    // ============================================
    // 1. Overall Estimated Charges Widget
    // ============================================
    this.dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Total Estimated Charges (USD)',
        width: 24,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
            },
            label: 'Total Charges',
          }),
        ],
        leftYAxis: {
          label: 'Cost (USD)',
          showUnits: false,
        },
      })
    );

    // ============================================
    // 2. Regional Cost Comparison
    // ============================================
    this.dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Cost by Region (Taiwan vs Japan)',
        width: 12,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
              Region: 'ap-northeast-1',
            },
            label: 'Taiwan (ap-northeast-1)',
            color: cloudwatch.Color.BLUE,
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
              Region: 'ap-northeast-1',
            },
            label: 'Japan (ap-northeast-1)',
            color: cloudwatch.Color.GREEN,
          }),
        ],
        leftYAxis: {
          label: 'Cost (USD)',
          showUnits: false,
        },
      }),
      
      // Regional cost pie chart
      new cloudwatch.SingleValueWidget({
        title: 'Current Regional Distribution',
        width: 12,
        height: 6,
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
              Region: 'ap-northeast-1',
            },
            label: 'Taiwan',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
              Region: 'ap-northeast-1',
            },
            label: 'Japan',
          }),
        ],
      })
    );

    // ============================================
    // 3. Service-Level Cost Breakdown
    // ============================================
    const services = [
      { name: 'EKS', service: 'Amazon Elastic Kubernetes Service', color: cloudwatch.Color.PURPLE },
      { name: 'RDS', service: 'Amazon Relational Database Service', color: cloudwatch.Color.ORANGE },
      { name: 'ElastiCache', service: 'Amazon ElastiCache', color: cloudwatch.Color.RED },
      { name: 'MSK', service: 'Amazon Managed Streaming for Apache Kafka', color: cloudwatch.Color.BROWN },
    ];

    this.dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Cost by Service',
        width: 24,
        height: 6,
        left: services.map(({ name, service, color }) =>
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
              ServiceName: service,
            },
            label: name,
            color,
          })
        ),
        leftYAxis: {
          label: 'Cost (USD)',
          showUnits: false,
        },
        stacked: true,
      })
    );

    // ============================================
    // 4. Individual Service Widgets
    // ============================================
    const serviceWidgets = services.map(({ name, service }) =>
      new cloudwatch.SingleValueWidget({
        title: `${name} Current Cost`,
        width: 6,
        height: 4,
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Billing',
            metricName: 'EstimatedCharges',
            statistic: 'Maximum',
            period: cdk.Duration.hours(6),
            dimensionsMap: {
              Currency: 'USD',
              ServiceName: service,
            },
          }),
        ],
      })
    );

    this.dashboard.addWidgets(...serviceWidgets);

    // ============================================
    // 5. Budget Utilization Tracking
    // ============================================
    this.dashboard.addWidgets(
      new cloudwatch.TextWidget({
        markdown: `
# Cost Management Dashboard

## Budget Limits
- **Taiwan Primary Region**: $5,000/month
- **Japan DR Region**: $3,000/month
- **EKS Service**: $2,000/month
- **RDS Service**: $1,500/month
- **ElastiCache Service**: $800/month
- **MSK Service**: $500/month

## Alert Thresholds
- **Regional Budgets**: 80% actual, 100% forecasted
- **Service Budgets**: 90% actual

## Cost Anomaly Detection
- **Threshold**: $100 daily anomaly
- **Frequency**: Daily monitoring
- **Alerts**: SNS notifications

## Resources
- [AWS Cost Explorer](https://console.aws.amazon.com/cost-management/home#/cost-explorer)
- [AWS Budgets](https://console.aws.amazon.com/billing/home#/budgets)
- [Cost Anomaly Detection](https://console.aws.amazon.com/cost-management/home#/anomaly-detection)
        `,
        width: 24,
        height: 8,
      })
    );

    // ============================================
    // Outputs
    // ============================================
    new cdk.CfnOutput(this, 'DashboardUrl', {
      value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${dashboardName}`,
      description: 'CloudWatch Dashboard URL for cost monitoring',
      exportName: 'GenAIDemo-CostDashboardUrl',
    });

    // Tags
    cdk.Tags.of(this).add('Project', 'GenAIDemo');
    cdk.Tags.of(this).add('Component', 'CostMonitoring');
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
  }
}
