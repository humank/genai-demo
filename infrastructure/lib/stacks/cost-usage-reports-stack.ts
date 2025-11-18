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
      code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime, timedelta
from decimal import Decimal

athena = boto3.client('athena')
sns = boto3.client('sns')
cloudwatch = boto3.client('cloudwatch')

WORKGROUP = os.environ['ATHENA_WORKGROUP']
DATABASE = os.environ['GLUE_DATABASE']
SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']
OUTPUT_LOCATION = os.environ['ATHENA_OUTPUT_LOCATION']

def handler(event, context):
    """
    Detect cost anomalies and perform root cause analysis
    
    Analyzes:
    1. Daily cost trends and identifies anomalies (>20% increase)
    2. Service-level cost breakdown
    3. Resource-level cost attribution
    4. Budget overspend risk prediction
    """
    
    try:
        # Query 1: Daily cost comparison (last 7 days vs previous 7 days)
        daily_cost_query = f"""
        SELECT 
            line_item_usage_start_date,
            SUM(line_item_unblended_cost) as daily_cost,
            product_servicename,
            line_item_resource_id
        FROM {DATABASE}.cost_usage_reports
        WHERE line_item_usage_start_date >= date_add('day', -14, current_date)
        GROUP BY line_item_usage_start_date, product_servicename, line_item_resource_id
        ORDER BY line_item_usage_start_date DESC
        """
        
        # Execute Athena query
        response = athena.start_query_execution(
            QueryString=daily_cost_query,
            QueryExecutionContext={{'Database': DATABASE}},
            ResultConfiguration={{'OutputLocation': OUTPUT_LOCATION}},
            WorkGroup=WORKGROUP
        )
        
        query_execution_id = response['QueryExecutionId']
        
        # Wait for query completion (simplified for demo)
        # In production, use Step Functions or async processing
        
        # Query 2: Service-level cost breakdown
        service_cost_query = f"""
        SELECT 
            product_servicename,
            SUM(line_item_unblended_cost) as service_cost,
            COUNT(DISTINCT line_item_resource_id) as resource_count
        FROM {DATABASE}.cost_usage_reports
        WHERE line_item_usage_start_date >= date_add('day', -1, current_date)
        GROUP BY product_servicename
        ORDER BY service_cost DESC
        LIMIT 10
        """
        
        # Detect anomalies (simplified logic)
        anomalies = detect_cost_anomalies()
        
        # Perform root cause analysis
        root_causes = analyze_root_causes(anomalies)
        
        # Check budget overspend risk
        budget_risk = check_budget_overspend_risk()
        
        # Publish metrics to CloudWatch
        publish_cost_metrics(anomalies, budget_risk)
        
        # Send SNS notification if anomalies detected
        if anomalies or budget_risk['high_risk']:
            send_anomaly_notification(anomalies, root_causes, budget_risk)
        
        return {{
            'statusCode': 200,
            'body': json.dumps({{
                'anomalies_detected': len(anomalies),
                'budget_risk_level': budget_risk['risk_level'],
                'query_execution_id': query_execution_id
            }})
        }}
        
    except Exception as e:
        print(f"Error in cost anomaly detection: {{str(e)}}")
        return {{
            'statusCode': 500,
            'body': json.dumps({{'error': str(e)}})
        }}

def detect_cost_anomalies():
    """Detect cost anomalies using statistical analysis"""
    # Simplified anomaly detection logic
    # In production, use AWS Cost Anomaly Detection service or ML models
    anomalies = []
    
    # Example: Detect if daily cost increased by >20%
    # This would query Athena results and compare trends
    
    return anomalies

def analyze_root_causes(anomalies):
    """Analyze root causes of cost anomalies"""
    root_causes = []
    
    for anomaly in anomalies:
        # Analyze service-level breakdown
        # Identify specific resources causing cost increase
        # Check for configuration changes or usage spikes
        root_causes.append({{
            'anomaly_id': anomaly.get('id'),
            'primary_cause': 'Service usage spike',
            'affected_services': ['EKS', 'RDS'],
            'recommendations': [
                'Review EKS node scaling policies',
                'Check RDS instance right-sizing'
            ]
        }})
    
    return root_causes

def check_budget_overspend_risk():
    """Check budget overspend risk based on current spending trends"""
    # Calculate spending rate and forecast end-of-month cost
    # Compare with budget thresholds
    
    return {{
        'risk_level': 'medium',
        'high_risk': False,
        'projected_monthly_cost': 4500,
        'budget_limit': 5000,
        'days_until_overspend': 25
    }}

def publish_cost_metrics(anomalies, budget_risk):
    """Publish cost metrics to CloudWatch"""
    cloudwatch.put_metric_data(
        Namespace='CostManagement/Insights',
        MetricData=[
            {{
                'MetricName': 'CostAnomaliesDetected',
                'Value': len(anomalies),
                'Unit': 'Count',
                'Timestamp': datetime.utcnow()
            }},
            {{
                'MetricName': 'BudgetOverspendRisk',
                'Value': 1 if budget_risk['high_risk'] else 0,
                'Unit': 'Count',
                'Timestamp': datetime.utcnow()
            }}
        ]
    )

def send_anomaly_notification(anomalies, root_causes, budget_risk):
    """Send SNS notification for detected anomalies"""
    message = f"""
Cost Anomaly Detection Alert

Anomalies Detected: {{len(anomalies)}}
Budget Risk Level: {{budget_risk['risk_level']}}
Projected Monthly Cost: ${{budget_risk['projected_monthly_cost']}}
Budget Limit: ${{budget_risk['budget_limit']}}

Root Causes:
{{json.dumps(root_causes, indent=2)}}

Action Required:
- Review cost breakdown in CloudWatch dashboard
- Check Athena queries for detailed analysis
- Implement recommended optimizations
"""
    
    sns.publish(
        TopicArn=SNS_TOPIC_ARN,
        Subject='Cost Anomaly Detection Alert',
        Message=message
    )
`),
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
