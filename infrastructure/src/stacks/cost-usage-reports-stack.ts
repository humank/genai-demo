import * as cdk from 'aws-cdk-lib';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as cur from 'aws-cdk-lib/aws-cur';
import * as glue from 'aws-cdk-lib/aws-glue';
import * as athena from 'aws-cdk-lib/aws-athena';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import { Construct } from 'constructs';

/**
 * Cost and Usage Reports (CUR) Stack
 * 
 * Implements detailed cost breakdown and attribution reporting with:
 * - AWS Cost and Usage Reports (CUR) with hourly granularity
 * - AWS Glue Data Catalog for cost data querying
 * - Amazon Athena for SQL-based cost analysis
 * - Automated cost anomaly detection with root cause analysis
 * - Budget overspend risk early warning system
 * 
 * Requirements: 13.22, 13.23, 13.24
 */
export class CostUsageReportsStack extends cdk.Stack {
  public readonly curBucket: s3.Bucket;
  public readonly athenaResultsBucket: s3.Bucket;
  public readonly glueDatabase: glue.CfnDatabase;
  public readonly athenaWorkgroup: athena.CfnWorkGroup;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // ========================================
    // S3 Buckets for CUR and Athena Results
    // ========================================

    // S3 bucket for Cost and Usage Reports
    this.curBucket = new s3.Bucket(this, 'CostUsageReportsBucket', {
      bucketName: `genai-demo-cur-${this.account}-${this.region}`,
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      versioned: true,
      lifecycleRules: [
        {
          id: 'DeleteOldReports',
          enabled: true,
          expiration: cdk.Duration.days(365), // Keep reports for 1 year
          transitions: [
            {
              storageClass: s3.StorageClass.INTELLIGENT_TIERING,
              transitionAfter: cdk.Duration.days(30),
            },
            {
              storageClass: s3.StorageClass.GLACIER,
              transitionAfter: cdk.Duration.days(90),
            },
          ],
        },
      ],
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // Grant AWS Billing service access to the bucket
    this.curBucket.addToResourcePolicy(
      new iam.PolicyStatement({
        sid: 'AllowBillingServiceAccess',
        effect: iam.Effect.ALLOW,
        principals: [new iam.ServicePrincipal('billingreports.amazonaws.com')],
        actions: [
          's3:GetBucketAcl',
          's3:GetBucketPolicy',
          's3:PutObject',
        ],
        resources: [
          this.curBucket.bucketArn,
          `${this.curBucket.bucketArn}/*`,
        ],
      })
    );

    // S3 bucket for Athena query results
    this.athenaResultsBucket = new s3.Bucket(this, 'AthenaResultsBucket', {
      bucketName: `genai-demo-athena-results-${this.account}-${this.region}`,
      encryption: s3.BucketEncryption.S3_MANAGED,
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      lifecycleRules: [
        {
          id: 'DeleteOldQueryResults',
          enabled: true,
          expiration: cdk.Duration.days(30), // Keep query results for 30 days
        },
      ],
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // ========================================
    // Cost and Usage Report Definition
    // ========================================

    const curReport = new cur.CfnReportDefinition(this, 'CostUsageReport', {
      reportName: 'genai-demo-hourly-cost-report',
      timeUnit: 'HOURLY', // Hourly granularity for detailed analysis
      format: 'Parquet', // Parquet format for efficient querying
      compression: 'Parquet',
      s3Bucket: this.curBucket.bucketName,
      s3Prefix: 'cur-reports',
      s3Region: this.region,
      additionalSchemaElements: ['RESOURCES'], // Include resource-level details
      refreshClosedReports: true,
      reportVersioning: 'OVERWRITE_REPORT',
      additionalArtifacts: ['ATHENA'], // Enable Athena integration
    });

    // ========================================
    // AWS Glue Data Catalog
    // ========================================

    // Glue database for cost data
    this.glueDatabase = new glue.CfnDatabase(this, 'CostDataCatalog', {
      catalogId: this.account,
      databaseInput: {
        name: 'cost_usage_reports',
        description: 'AWS Cost and Usage Reports data catalog for detailed cost analysis',
      },
    });

    // Glue crawler to automatically discover CUR data schema
    const glueCrawlerRole = new iam.Role(this, 'GlueCrawlerRole', {
      assumedBy: new iam.ServicePrincipal('glue.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSGlueServiceRole'),
      ],
    });

    this.curBucket.grantRead(glueCrawlerRole);

    const glueCrawler = new glue.CfnCrawler(this, 'CostDataCrawler', {
      name: 'cost-usage-reports-crawler',
      role: glueCrawlerRole.roleArn,
      databaseName: this.glueDatabase.ref,
      targets: {
        s3Targets: [
          {
            path: `s3://${this.curBucket.bucketName}/cur-reports/`,
          },
        ],
      },
      schedule: {
        scheduleExpression: 'cron(0 2 * * ? *)', // Run daily at 2 AM
      },
      schemaChangePolicy: {
        updateBehavior: 'UPDATE_IN_DATABASE',
        deleteBehavior: 'LOG',
      },
    });

    // ========================================
    // Amazon Athena Workgroup
    // ========================================

    this.athenaWorkgroup = new athena.CfnWorkGroup(this, 'CostAnalysisWorkgroup', {
      name: 'cost-analysis-workgroup',
      description: 'Athena workgroup for cost and usage analysis queries',
      workGroupConfiguration: {
        resultConfiguration: {
          outputLocation: `s3://${this.athenaResultsBucket.bucketName}/query-results/`,
          encryptionConfiguration: {
            encryptionOption: 'SSE_S3',
          },
        },
        enforceWorkGroupConfiguration: true,
        publishCloudWatchMetricsEnabled: true,
        bytesScannedCutoffPerQuery: 1000000000000, // 1 TB limit per query
      },
    });

    // ========================================
    // Cost Anomaly Detection Lambda
    // ========================================

    // Lambda function for cost anomaly detection and root cause analysis
    const costAnomalyDetectionLambda = new lambda.Function(this, 'CostAnomalyDetection', {
      functionName: 'cost-anomaly-detection',
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('src/lambda/cost-anomaly-detector'),
      timeout: cdk.Duration.minutes(5),
      memorySize: 512,
      environment: {
        ATHENA_WORKGROUP: this.athenaWorkgroup.name!,
        GLUE_DATABASE: this.glueDatabase.ref,
        SNS_TOPIC_ARN: '', // Will be set after SNS topic creation
        ATHENA_OUTPUT_LOCATION: `s3://${this.athenaResultsBucket.bucketName}/query-results/`,
      },
    });

    // Grant Lambda permissions
    this.curBucket.grantRead(costAnomalyDetectionLambda);
    this.athenaResultsBucket.grantReadWrite(costAnomalyDetectionLambda);
    
    costAnomalyDetectionLambda.addToRolePolicy(
      new iam.PolicyStatement({
        actions: [
          'athena:StartQueryExecution',
          'athena:GetQueryExecution',
          'athena:GetQueryResults',
          'glue:GetDatabase',
          'glue:GetTable',
          'glue:GetPartitions',
        ],
        resources: ['*'],
      })
    );

    // ========================================
    // SNS Topic for Cost Alerts
    // ========================================

    const costAlertTopic = new sns.Topic(this, 'CostAlertTopic', {
      topicName: 'cost-anomaly-alerts',
      displayName: 'Cost Anomaly Detection Alerts',
    });

    // Update Lambda environment variable with SNS topic ARN
    costAnomalyDetectionLambda.addEnvironment('SNS_TOPIC_ARN', costAlertTopic.topicArn);
    costAlertTopic.grantPublish(costAnomalyDetectionLambda);

    // Add email subscription (configure via AWS Console or parameter)
    // costAlertTopic.addSubscription(
    //   new subscriptions.EmailSubscription('devops@example.com')
    // );

    // ========================================
    // EventBridge Rule for Daily Analysis
    // ========================================

    // Schedule daily cost anomaly detection
    const dailyAnalysisRule = new events.Rule(this, 'DailyCostAnalysis', {
      ruleName: 'daily-cost-anomaly-detection',
      description: 'Trigger cost anomaly detection daily at 3 AM',
      schedule: events.Schedule.cron({
        minute: '0',
        hour: '3',
        day: '*',
        month: '*',
        year: '*',
      }),
    });

    dailyAnalysisRule.addTarget(new targets.LambdaFunction(costAnomalyDetectionLambda));

    // ========================================
    // CloudWatch Dashboard for Cost Insights
    // ========================================

    const costInsightsDashboard = new cloudwatch.Dashboard(this, 'CostInsightsDashboard', {
      dashboardName: 'cost-usage-reports-insights',
    });

    costInsightsDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Cost Anomalies Detected',
        left: [
          new cloudwatch.Metric({
            namespace: 'CostManagement/Insights',
            metricName: 'CostAnomaliesDetected',
            statistic: 'Sum',
            period: cdk.Duration.days(1),
          }),
        ],
        width: 12,
      }),
      new cloudwatch.GraphWidget({
        title: 'Budget Overspend Risk',
        left: [
          new cloudwatch.Metric({
            namespace: 'CostManagement/Insights',
            metricName: 'BudgetOverspendRisk',
            statistic: 'Maximum',
            period: cdk.Duration.days(1),
          }),
        ],
        width: 12,
      })
    );

    // ========================================
    // CloudWatch Alarms
    // ========================================

    // Alarm for cost anomalies
    new cloudwatch.Alarm(this, 'CostAnomalyAlarm', {
      alarmName: 'cost-anomaly-detected',
      alarmDescription: 'Alert when cost anomalies are detected',
      metric: new cloudwatch.Metric({
        namespace: 'CostManagement/Insights',
        metricName: 'CostAnomaliesDetected',
        statistic: 'Sum',
        period: cdk.Duration.days(1),
      }),
      threshold: 1,
      evaluationPeriods: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      actionsEnabled: true,
    }).addAlarmAction({
      bind: () => ({ alarmActionArn: costAlertTopic.topicArn }),
    });

    // Alarm for budget overspend risk
    new cloudwatch.Alarm(this, 'BudgetOverspendRiskAlarm', {
      alarmName: 'budget-overspend-risk-high',
      alarmDescription: 'Alert when budget overspend risk is high',
      metric: new cloudwatch.Metric({
        namespace: 'CostManagement/Insights',
        metricName: 'BudgetOverspendRisk',
        statistic: 'Maximum',
        period: cdk.Duration.days(1),
      }),
      threshold: 1,
      evaluationPeriods: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      actionsEnabled: true,
    }).addAlarmAction({
      bind: () => ({ alarmActionArn: costAlertTopic.topicArn }),
    });

    // ========================================
    // Outputs
    // ========================================

    new cdk.CfnOutput(this, 'CURBucketName', {
      value: this.curBucket.bucketName,
      description: 'S3 bucket for Cost and Usage Reports',
      exportName: 'CostUsageReportsBucketName',
    });

    new cdk.CfnOutput(this, 'GlueDatabaseName', {
      value: this.glueDatabase.ref,
      description: 'Glue database for cost data catalog',
      exportName: 'CostDataCatalogDatabaseName',
    });

    new cdk.CfnOutput(this, 'AthenaWorkgroupName', {
      value: this.athenaWorkgroup.name!,
      description: 'Athena workgroup for cost analysis',
      exportName: 'CostAnalysisWorkgroupName',
    });

    new cdk.CfnOutput(this, 'CostAlertTopicArn', {
      value: costAlertTopic.topicArn,
      description: 'SNS topic for cost anomaly alerts',
      exportName: 'CostAnomalyAlertTopicArn',
    });

    new cdk.CfnOutput(this, 'CostInsightsDashboardURL', {
      value: `https://console.aws.amazon.com/cloudwatch/home?region=${this.region}#dashboards:name=${costInsightsDashboard.dashboardName}`,
      description: 'CloudWatch dashboard for cost insights',
    });
  }
}
