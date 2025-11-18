import * as cdk from 'aws-cdk-lib';
import * as ce from 'aws-cdk-lib/aws-ce';
import * as budgets from 'aws-cdk-lib/aws-budgets';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as cur from 'aws-cdk-lib/aws-cur';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as path from 'path';
import { Construct } from 'constructs';

export interface CostManagementStackProps extends cdk.StackProps {
  /**
   * Email address for cost alert notifications
   */
  readonly alertEmail: string;
  
  /**
   * Monthly budget limit for Taiwan primary region (USD)
   * @default 5000
   */
  readonly taiwanBudgetLimit?: number;
  
  /**
   * Monthly budget limit for Japan DR region (USD)
   * @default 3000
   */
  readonly japanBudgetLimit?: number;
  
  /**
   * Enable Cost and Usage Reports
   * @default false
   */
  readonly enableCostReports?: boolean;
  
  /**
   * Enable Cost Explorer trend analysis dashboard
   * @default true
   */
  readonly enableCostExplorerDashboard?: boolean;
  
  /**
   * Enable Trusted Advisor automation
   * @default true
   */
  readonly enableTrustedAdvisorAutomation?: boolean;
}

/**
 * Cost Management Stack
 * 
 * Implements AWS Native cost management services:
 * - AWS Cost Anomaly Detection
 * - AWS Budgets (Taiwan + Japan regions)
 * - Service-level budgets (EKS, RDS, ElastiCache, MSK)
 * - Cost and Usage Reports (optional)
 * 
 * Architecture: AWS Native Services (No Lambda)
 * Integration: SNS alerts + CloudWatch Dashboard
 */
export class CostManagementStack extends cdk.Stack {
  public readonly costAlertTopic: sns.Topic;
  public readonly costReportsBucket?: s3.Bucket;
  public readonly costExplorerDashboard?: cloudwatch.Dashboard;
  public readonly trustedAdvisorFunction?: lambda.Function;

  constructor(scope: Construct, id: string, props: CostManagementStackProps) {
    super(scope, id, props);

    const taiwanBudget = props.taiwanBudgetLimit ?? 5000;
    const japanBudget = props.japanBudgetLimit ?? 3000;

    // ============================================
    // 1. SNS Topic for All Cost Alerts
    // ============================================
    this.costAlertTopic = new sns.Topic(this, 'CostAlertTopic', {
      displayName: 'GenAIDemo Cost Alerts',
      topicName: 'genai-demo-cost-alerts',
    });

    // Subscribe email for cost alerts
    this.costAlertTopic.addSubscription(
      new subscriptions.EmailSubscription(props.alertEmail)
    );

    // ============================================
    // 2. Cost Anomaly Detection (AWS Native)
    // ============================================
    const anomalyMonitor = new ce.CfnAnomalyMonitor(this, 'CostAnomalyMonitor', {
      monitorName: 'GenAIDemo-MultiRegion-Monitor',
      monitorType: 'DIMENSIONAL',
      monitorDimension: 'SERVICE',
    });

    new ce.CfnAnomalySubscription(this, 'CostAnomalySubscription', {
      subscriptionName: 'GenAIDemo-Anomaly-Alerts',
      threshold: 100, // $100 異常閾值
      frequency: 'DAILY',
      monitorArnList: [anomalyMonitor.attrMonitorArn],
      subscribers: [
        {
          type: 'SNS',
          address: this.costAlertTopic.topicArn,
        },
      ],
    });

    // ============================================
    // 3. AWS Budgets - Taiwan Primary Region
    // ============================================
    new budgets.CfnBudget(this, 'TaiwanRegionBudget', {
      budget: {
        budgetName: 'Taiwan-Primary-Region-Budget',
        budgetType: 'COST',
        timeUnit: 'MONTHLY',
        budgetLimit: {
          amount: taiwanBudget,
          unit: 'USD',
        },
        costFilters: {
          Region: ['ap-northeast-1'],
        },
      },
      notificationsWithSubscribers: [
        {
          notification: {
            notificationType: 'ACTUAL',
            comparisonOperator: 'GREATER_THAN',
            threshold: 80, // 80% 告警
            thresholdType: 'PERCENTAGE',
          },
          subscribers: [
            {
              subscriptionType: 'SNS',
              address: this.costAlertTopic.topicArn,
            },
          ],
        },
        {
          notification: {
            notificationType: 'FORECASTED',
            comparisonOperator: 'GREATER_THAN',
            threshold: 100, // 預測超支
            thresholdType: 'PERCENTAGE',
          },
          subscribers: [
            {
              subscriptionType: 'SNS',
              address: this.costAlertTopic.topicArn,
            },
          ],
        },
      ],
    });

    // ============================================
    // 4. AWS Budgets - Japan DR Region
    // ============================================
    new budgets.CfnBudget(this, 'JapanDRBudget', {
      budget: {
        budgetName: 'Japan-DR-Region-Budget',
        budgetType: 'COST',
        timeUnit: 'MONTHLY',
        budgetLimit: {
          amount: japanBudget,
          unit: 'USD',
        },
        costFilters: {
          Region: ['ap-northeast-1'],
        },
      },
      notificationsWithSubscribers: [
        {
          notification: {
            notificationType: 'ACTUAL',
            comparisonOperator: 'GREATER_THAN',
            threshold: 80,
            thresholdType: 'PERCENTAGE',
          },
          subscribers: [
            {
              subscriptionType: 'SNS',
              address: this.costAlertTopic.topicArn,
            },
          ],
        },
      ],
    });

    // ============================================
    // 5. Service-Level Budgets
    // ============================================
    const serviceBudgets = [
      { name: 'EKS', service: 'Amazon Elastic Kubernetes Service', limit: 2000 },
      { name: 'RDS', service: 'Amazon Relational Database Service', limit: 1500 },
      { name: 'ElastiCache', service: 'Amazon ElastiCache', limit: 800 },
      { name: 'MSK', service: 'Amazon Managed Streaming for Apache Kafka', limit: 500 },
    ];

    serviceBudgets.forEach(({ name, service, limit }) => {
      new budgets.CfnBudget(this, `${name}ServiceBudget`, {
        budget: {
          budgetName: `${name}-Service-Budget`,
          budgetType: 'COST',
          timeUnit: 'MONTHLY',
          budgetLimit: {
            amount: limit,
            unit: 'USD',
          },
          costFilters: {
            Service: [service],
          },
        },
        notificationsWithSubscribers: [
          {
            notification: {
              notificationType: 'ACTUAL',
              comparisonOperator: 'GREATER_THAN',
              threshold: 90, // 90% 告警
              thresholdType: 'PERCENTAGE',
            },
            subscribers: [
              {
                subscriptionType: 'SNS',
                address: this.costAlertTopic.topicArn,
              },
            ],
          },
        ],
      });
    });

    // ============================================
    // 6. Cost and Usage Reports (Optional)
    // ============================================
    if (props.enableCostReports) {
      this.costReportsBucket = new s3.Bucket(this, 'CostReportsBucket', {
        bucketName: `genai-demo-cost-reports-${this.account}`,
        encryption: s3.BucketEncryption.S3_MANAGED,
        lifecycleRules: [
          {
            expiration: cdk.Duration.days(90), // 90 天後刪除
          },
        ],
        blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
        enforceSSL: true,
      });

      new cur.CfnReportDefinition(this, 'CostUsageReport', {
        reportName: 'genai-demo-cost-usage-report',
        timeUnit: 'DAILY',
        format: 'Parquet',
        compression: 'Parquet',
        s3Bucket: this.costReportsBucket.bucketName,
        s3Prefix: 'cost-reports/',
        s3Region: this.region,
        additionalSchemaElements: ['RESOURCES'],
        reportVersioning: 'OVERWRITE_REPORT',
        refreshClosedReports: true,
      });
    }

    // ============================================
    // Outputs
    // ============================================
    new cdk.CfnOutput(this, 'CostAlertTopicArn', {
      value: this.costAlertTopic.topicArn,
      description: 'SNS Topic ARN for cost alerts',
      exportName: 'GenAIDemo-CostAlertTopicArn',
    });

    if (this.costReportsBucket) {
      new cdk.CfnOutput(this, 'CostReportsBucketName', {
        value: this.costReportsBucket.bucketName,
        description: 'S3 Bucket for Cost and Usage Reports',
        exportName: 'GenAIDemo-CostReportsBucket',
      });
    }

    if (this.costExplorerDashboard) {
      new cdk.CfnOutput(this, 'CostExplorerDashboardName', {
        value: this.costExplorerDashboard.dashboardName,
        description: 'CloudWatch Dashboard for Cost Explorer trends',
        exportName: 'GenAIDemo-CostExplorerDashboard',
      });
    }

    if (this.trustedAdvisorFunction) {
      new cdk.CfnOutput(this, 'TrustedAdvisorFunctionArn', {
        value: this.trustedAdvisorFunction.functionArn,
        description: 'Lambda Function ARN for Trusted Advisor automation',
        exportName: 'GenAIDemo-TrustedAdvisorFunction',
      });
    }

    // ============================================
    // 7. Cost Explorer Trend Analysis Dashboard
    // ============================================
    if (props.enableCostExplorerDashboard !== false) {
      this.costExplorerDashboard = new cloudwatch.Dashboard(this, 'CostExplorerDashboard', {
        dashboardName: 'GenAIDemo-Cost-Explorer-Trends',
      });

      // Monthly cost trend widget
      this.costExplorerDashboard.addWidgets(
        new cloudwatch.GraphWidget({
          title: 'Monthly Cost Trend (Last 6 Months)',
          width: 12,
          height: 6,
          left: [
            new cloudwatch.Metric({
              namespace: 'AWS/Billing',
              metricName: 'EstimatedCharges',
              dimensionsMap: {
                Currency: 'USD',
              },
              statistic: 'Maximum',
              period: cdk.Duration.hours(6),
            }),
          ],
        })
      );

      // Cost by service breakdown
      const services = ['AmazonEC2', 'AmazonRDS', 'AmazonElastiCache', 'AmazonMSK', 'AmazonEKS'];
      const serviceMetrics = services.map(service => 
        new cloudwatch.Metric({
          namespace: 'AWS/Billing',
          metricName: 'EstimatedCharges',
          dimensionsMap: {
            Currency: 'USD',
            ServiceName: service,
          },
          statistic: 'Maximum',
          period: cdk.Duration.hours(6),
        })
      );

      this.costExplorerDashboard.addWidgets(
        new cloudwatch.GraphWidget({
          title: 'Cost by Service (Daily)',
          width: 12,
          height: 6,
          left: serviceMetrics,
          stacked: true,
        })
      );

      // Regional cost comparison
      this.costExplorerDashboard.addWidgets(
        new cloudwatch.GraphWidget({
          title: 'Regional Cost Comparison',
          width: 12,
          height: 6,
          left: [
            new cloudwatch.Metric({
              namespace: 'AWS/Billing',
              metricName: 'EstimatedCharges',
              dimensionsMap: {
                Currency: 'USD',
                Region: 'ap-northeast-1', // Taiwan
              },
              statistic: 'Maximum',
              period: cdk.Duration.hours(6),
              label: 'Taiwan (Primary)',
            }),
            new cloudwatch.Metric({
              namespace: 'AWS/Billing',
              metricName: 'EstimatedCharges',
              dimensionsMap: {
                Currency: 'USD',
                Region: 'ap-northeast-1', // Japan
              },
              statistic: 'Maximum',
              period: cdk.Duration.hours(6),
              label: 'Japan (DR)',
            }),
          ],
        })
      );

      // Budget utilization widget
      this.costExplorerDashboard.addWidgets(
        new cloudwatch.SingleValueWidget({
          title: 'Taiwan Budget Utilization',
          width: 6,
          height: 6,
          metrics: [
            new cloudwatch.MathExpression({
              expression: '(m1 / ' + taiwanBudget + ') * 100',
              usingMetrics: {
                m1: new cloudwatch.Metric({
                  namespace: 'AWS/Billing',
                  metricName: 'EstimatedCharges',
                  dimensionsMap: {
                    Currency: 'USD',
                  },
                  statistic: 'Maximum',
                  period: cdk.Duration.hours(6),
                }),
              },
              label: 'Budget Used (%)',
            }),
          ],
        }),
        new cloudwatch.SingleValueWidget({
          title: 'Japan Budget Utilization',
          width: 6,
          height: 6,
          metrics: [
            new cloudwatch.MathExpression({
              expression: '(m1 / ' + japanBudget + ') * 100',
              usingMetrics: {
                m1: new cloudwatch.Metric({
                  namespace: 'AWS/Billing',
                  metricName: 'EstimatedCharges',
                  dimensionsMap: {
                    Currency: 'USD',
                  },
                  statistic: 'Maximum',
                  period: cdk.Duration.hours(6),
                }),
              },
              label: 'Budget Used (%)',
            }),
          ],
        })
      );
    }

    // ============================================
    // 8. Trusted Advisor Automation
    // ============================================
    if (props.enableTrustedAdvisorAutomation !== false) {
      // Lambda function for Trusted Advisor checks
      this.trustedAdvisorFunction = new lambda.Function(this, 'TrustedAdvisorFunction', {
        functionName: 'genai-demo-trusted-advisor-automation',
        runtime: lambda.Runtime.PYTHON_3_11,
        handler: 'index.handler',
        code: lambda.Code.fromAsset(path.join(__dirname, '../../src/lambda/trusted-advisor-automation')),
        timeout: cdk.Duration.minutes(5),
        environment: {
          SNS_TOPIC_ARN: this.costAlertTopic.topicArn,
        },
      });

      // Grant permissions to Lambda
      this.trustedAdvisorFunction.addToRolePolicy(
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'support:DescribeTrustedAdvisorChecks',
            'support:DescribeTrustedAdvisorCheckResult',
            'support:RefreshTrustedAdvisorCheck',
          ],
          resources: ['*'],
        })
      );

      this.costAlertTopic.grantPublish(this.trustedAdvisorFunction);

      // Schedule weekly Trusted Advisor checks
      const trustedAdvisorRule = new events.Rule(this, 'TrustedAdvisorSchedule', {
        ruleName: 'genai-demo-trusted-advisor-weekly',
        description: 'Weekly Trusted Advisor cost optimization checks',
        schedule: events.Schedule.cron({
          weekDay: 'MON',
          hour: '9',
          minute: '0',
        }),
      });

      trustedAdvisorRule.addTarget(
        new targets.LambdaFunction(this.trustedAdvisorFunction)
      );
    }

    // Tags for cost allocation
    cdk.Tags.of(this).add('Project', 'GenAIDemo');
    cdk.Tags.of(this).add('Environment', 'Production');
    cdk.Tags.of(this).add('CostCenter', 'Engineering');
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
  }
}
