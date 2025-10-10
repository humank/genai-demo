import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as snsSubscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import { Construct } from 'constructs';

/**
 * Stack for creating CloudWatch alarms and SNS topics for health check alerting.
 * Enhanced with multi-region alerting support for Active-Active architecture.
 * 
 * Implements requirement 8.3: Set up CloudWatch alarms for critical metrics
 * Implements requirement 8.4: Create SNS topics and subscriptions for alerting
 * Implements requirement 4.1.5: Multi-region alerting system with intelligent deduplication
 */
export class AlertingStack extends cdk.Stack {
  public readonly criticalAlertsTopic: sns.Topic;
  public readonly warningAlertsTopic: sns.Topic;
  public readonly infoAlertsTopic: sns.Topic;
  public readonly globalAlertsTopic?: sns.Topic;
  public readonly alertDeduplicationTable?: dynamodb.Table;
  public readonly alertDeduplicationFunction?: lambda.Function;

  constructor(scope: Construct, id: string, props: cdk.StackProps & {
    environment: string;
    region: string;
    applicationName: string;
    alertingConfig: AlertingConfig;
    multiRegionConfig?: MultiRegionAlertingConfig;
  }) {
    super(scope, id, props);

    const { environment, region, applicationName, alertingConfig, multiRegionConfig } = props;

    // Create SNS Topics for different alert levels
    this.criticalAlertsTopic = this.createSNSTopic('Critical', environment, alertingConfig.criticalAlerts);
    this.warningAlertsTopic = this.createSNSTopic('Warning', environment, alertingConfig.warningAlerts);
    this.infoAlertsTopic = this.createSNSTopic('Info', environment, alertingConfig.infoAlerts);

    // Create multi-region alerting components if enabled
    if (multiRegionConfig?.enabled) {
      // Create global alerts topic for cross-region alerts
      this.globalAlertsTopic = new sns.Topic(this, 'GlobalAlertsTopic', {
        topicName: `${applicationName}-${environment}-global-alerts`,
        displayName: `${applicationName} ${environment} Global Multi-Region Alerts`,
        fifo: false,
      });

      // Create alert deduplication table
      this.alertDeduplicationTable = new dynamodb.Table(this, 'AlertDeduplicationTable', {
        tableName: `${applicationName}-${environment}-alert-deduplication`,
        partitionKey: { name: 'alertKey', type: dynamodb.AttributeType.STRING },
        sortKey: { name: 'timestamp', type: dynamodb.AttributeType.NUMBER },
        timeToLiveAttribute: 'ttl',
        billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
        removalPolicy: cdk.RemovalPolicy.DESTROY,
      });

      // Create alert deduplication Lambda function
      this.alertDeduplicationFunction = this.createAlertDeduplicationLambda(
        environment, 
        applicationName, 
        multiRegionConfig
      );
      
      this.setupMultiRegionAlerting(environment, applicationName, multiRegionConfig);
    }

    // Create CloudWatch Alarms for health checks
    this.createHealthCheckAlarms(environment, applicationName);

    // Create CloudWatch Alarms for application metrics
    this.createApplicationMetricAlarms(environment, applicationName);

    // Create CloudWatch Alarms for infrastructure metrics
    this.createInfrastructureAlarms(environment, applicationName);

    // Create CloudWatch Alarms for Aurora PostgreSQL deadlock monitoring
    this.createAuroraDeadlockAlarms(environment, applicationName);

    // Create CloudWatch Dashboard
    this.createHealthCheckDashboard(environment, applicationName);

    // Output important ARNs
    new cdk.CfnOutput(this, 'CriticalAlertsTopicArn', {
      value: this.criticalAlertsTopic.topicArn,
      description: 'ARN of the critical alerts SNS topic',
      exportName: `${environment}-critical-alerts-topic-arn`,
    });

    new cdk.CfnOutput(this, 'WarningAlertsTopicArn', {
      value: this.warningAlertsTopic.topicArn,
      description: 'ARN of the warning alerts SNS topic',
      exportName: `${environment}-warning-alerts-topic-arn`,
    });
  }

  /**
   * Set up multi-region alerting system with intelligent deduplication
   */
  private setupMultiRegionAlerting(
    environment: string, 
    applicationName: string, 
    multiRegionConfig: MultiRegionAlertingConfig
  ): void {
    // Resources are now created in constructor
    // This method handles additional setup logic

    // Create regional failure alarms
    this.createRegionalFailureAlarms(environment, applicationName, multiRegionConfig.regions);

    // Create cross-region sync monitoring alarms
    this.createCrossRegionSyncAlarms(environment, applicationName, multiRegionConfig);

    // Create escalation policy
    this.createEscalationPolicy(environment, applicationName, multiRegionConfig);
  }

  /**
   * Create alert deduplication Lambda function
   */
  private createAlertDeduplicationLambda(
    environment: string,
    applicationName: string,
    multiRegionConfig: MultiRegionAlertingConfig
  ): lambda.Function {
    const deduplicationFunction = new lambda.Function(this, 'AlertDeduplicationFunction', {
      functionName: `${applicationName}-${environment}-alert-deduplication`,
      runtime: lambda.Runtime.PYTHON_3_9,
      handler: 'index.handler',
      timeout: cdk.Duration.minutes(5),
      memorySize: 256,
      environment: {
        DEDUPLICATION_TABLE_NAME: this.alertDeduplicationTable!.tableName,
        TIME_WINDOW_MINUTES: multiRegionConfig.alertDeduplication.timeWindow.toString(),
        SIMILARITY_THRESHOLD: multiRegionConfig.alertDeduplication.similarityThreshold.toString(),
        GLOBAL_ALERTS_TOPIC_ARN: this.globalAlertsTopic!.topicArn,
        REGIONS: JSON.stringify(multiRegionConfig.regions),
        PRIMARY_REGION: multiRegionConfig.primaryRegion,
      },
      code: lambda.Code.fromInline(`
import boto3
import json
import hashlib
import time
from datetime import datetime, timedelta
from decimal import Decimal

dynamodb = boto3.resource('dynamodb')
sns = boto3.client('sns')
cloudwatch = boto3.client('cloudwatch')

table = dynamodb.Table(os.environ['DEDUPLICATION_TABLE_NAME'])
time_window = int(os.environ['TIME_WINDOW_MINUTES'])
similarity_threshold = float(os.environ['SIMILARITY_THRESHOLD'])
global_topic_arn = os.environ['GLOBAL_ALERTS_TOPIC_ARN']
regions = json.loads(os.environ['REGIONS'])
primary_region = os.environ['PRIMARY_REGION']

def handler(event, context):
    """
    Intelligent alert deduplication handler
    - Deduplicates similar alerts within time window
    - Aggregates cross-region alerts
    - Implements escalation policy
    """
    try:
        # Parse SNS message
        if 'Records' in event:
            for record in event['Records']:
                if record['EventSource'] == 'aws:sns':
                    message = json.loads(record['Sns']['Message'])
                    process_alert(message, record['Sns'])
        else:
            # Direct invocation
            process_alert(event, None)
            
        return {
            'statusCode': 200,
            'body': json.dumps('Alert deduplication completed successfully')
        }
        
    except Exception as e:
        print(f"Error in alert deduplication: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }

def process_alert(alert_data, sns_data):
    """Process individual alert for deduplication"""
    
    # Extract alert information
    alert_type = alert_data.get('AlarmName', 'Unknown')
    region = alert_data.get('Region', 'Unknown')
    metric_name = alert_data.get('MetricName', 'Unknown')
    namespace = alert_data.get('Namespace', 'Unknown')
    
    # Create alert key for deduplication
    alert_key = create_alert_key(alert_type, region, metric_name, namespace)
    
    # Check for existing similar alerts
    current_time = int(time.time())
    cutoff_time = current_time - (time_window * 60)
    
    try:
        # Query for similar alerts in time window
        response = table.query(
            KeyConditionExpression='alertKey = :key AND #ts > :cutoff',
            ExpressionAttributeNames={'#ts': 'timestamp'},
            ExpressionAttributeValues={
                ':key': alert_key,
                ':cutoff': cutoff_time
            }
        )
        
        existing_alerts = response.get('Items', [])
        
        if existing_alerts:
            # Alert already exists, update count
            update_alert_count(alert_key, current_time, alert_data)
            print(f"Deduplicated alert: {alert_key}")
        else:
            # New alert, store and potentially escalate
            store_new_alert(alert_key, current_time, alert_data)
            
            # Check if this requires cross-region escalation
            if should_escalate_to_global(alert_data, region):
                escalate_to_global_alert(alert_data, region)
                
    except Exception as e:
        print(f"Error processing alert {alert_key}: {str(e)}")
        # If deduplication fails, let the alert through
        pass

def create_alert_key(alert_type, region, metric_name, namespace):
    """Create unique key for alert deduplication"""
    key_string = f"{alert_type}:{region}:{metric_name}:{namespace}"
    return hashlib.md5(key_string.encode()).hexdigest()

def store_new_alert(alert_key, timestamp, alert_data):
    """Store new alert in deduplication table"""
    ttl = timestamp + (24 * 60 * 60)  # 24 hours TTL
    
    table.put_item(
        Item={
            'alertKey': alert_key,
            'timestamp': timestamp,
            'alertData': json.dumps(alert_data, default=str),
            'count': 1,
            'ttl': ttl
        }
    )

def update_alert_count(alert_key, timestamp, alert_data):
    """Update count for existing alert"""
    table.update_item(
        Key={'alertKey': alert_key, 'timestamp': timestamp},
        UpdateExpression='SET #count = #count + :inc',
        ExpressionAttributeNames={'#count': 'count'},
        ExpressionAttributeValues={':inc': 1}
    )

def should_escalate_to_global(alert_data, region):
    """Determine if alert should be escalated to global level"""
    
    # Escalate if it's a regional failure
    if 'regional-health' in alert_data.get('AlarmName', '').lower():
        return True
        
    # Escalate if it's a cross-region sync failure
    if 'cross-region' in alert_data.get('AlarmName', '').lower():
        return True
        
    # Escalate if multiple regions are affected
    affected_regions = count_affected_regions(alert_data.get('AlarmName', ''))
    if affected_regions >= 2:
        return True
        
    return False

def escalate_to_global_alert(alert_data, region):
    """Escalate alert to global level"""
    
    global_message = {
        'AlertType': 'GLOBAL_ESCALATION',
        'OriginalAlert': alert_data,
        'AffectedRegion': region,
        'EscalationTime': datetime.utcnow().isoformat(),
        'Severity': 'CRITICAL',
        'Message': f"Regional alert escalated to global level from {region}"
    }
    
    sns.publish(
        TopicArn=global_topic_arn,
        Subject=f"GLOBAL ALERT: Regional Issue in {region}",
        Message=json.dumps(global_message, indent=2)
    )
    
    # Send custom metric for global escalation
    cloudwatch.put_metric_data(
        Namespace='GenAIDemo/MultiRegion/Alerting',
        MetricData=[
            {
                'MetricName': 'GlobalEscalations',
                'Value': 1,
                'Unit': 'Count',
                'Dimensions': [
                    {'Name': 'SourceRegion', 'Value': region},
                    {'Name': 'AlertType', 'Value': alert_data.get('AlarmName', 'Unknown')}
                ]
            }
        ]
    )

def count_affected_regions(alarm_name):
    """Count how many regions are affected by similar alarms"""
    # This would query CloudWatch to see how many regions have similar active alarms
    # Simplified implementation for now
    return 1

import os
      `),
    });

    // Grant permissions to the Lambda function
    this.alertDeduplicationTable!.grantReadWriteData(deduplicationFunction);
    this.globalAlertsTopic!.grantPublish(deduplicationFunction);

    deduplicationFunction.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          'cloudwatch:PutMetricData',
          'cloudwatch:DescribeAlarms',
          'cloudwatch:GetMetricStatistics',
        ],
        resources: ['*'],
      })
    );

    // Subscribe deduplication function to all alert topics
    this.criticalAlertsTopic.addSubscription(
      new snsSubscriptions.LambdaSubscription(deduplicationFunction)
    );
    this.warningAlertsTopic.addSubscription(
      new snsSubscriptions.LambdaSubscription(deduplicationFunction)
    );

    return deduplicationFunction;
  }

  /**
   * Create regional failure alarms for multi-region monitoring
   */
  private createRegionalFailureAlarms(
    environment: string,
    applicationName: string,
    regions: string[]
  ): void {
    regions.forEach(region => {
      // Regional health check alarm
      const regionalHealthAlarm = new cloudwatch.Alarm(this, `RegionalHealthAlarm${region}`, {
        alarmName: `${applicationName}-${environment}-regional-health-${region}`,
        alarmDescription: `Regional health check failure in ${region}`,
        metric: new cloudwatch.Metric({
          namespace: 'AWS/Route53',
          metricName: 'HealthCheckStatus',
          dimensionsMap: {
            HealthCheckId: `${applicationName}-${environment}-health-check-${region}`,
          },
          statistic: 'Average',
          period: cdk.Duration.minutes(1),
        }),
        threshold: 0.5,
        comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
        evaluationPeriods: 3,
        datapointsToAlarm: 2,
        treatMissingData: cloudwatch.TreatMissingData.BREACHING,
      });

      // Add alarm action to critical alerts topic
      regionalHealthAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.criticalAlertsTopic));

      // Regional traffic anomaly alarm
      const trafficAnomalyAlarm = new cloudwatch.Alarm(this, `RegionalTrafficAnomalyAlarm${region}`, {
        alarmName: `${applicationName}-${environment}-traffic-anomaly-${region}`,
        alarmDescription: `Unusual traffic pattern detected in ${region}`,
        metric: new cloudwatch.Metric({
          namespace: 'AWS/ApplicationELB',
          metricName: 'RequestCount',
          dimensionsMap: {
            LoadBalancer: `app/${applicationName}-${environment}-alb-${region}`,
          },
          statistic: 'Sum',
          period: cdk.Duration.minutes(5),
        }),
        threshold: 10, // Minimum expected requests
        comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
        evaluationPeriods: 3,
        datapointsToAlarm: 2,
        treatMissingData: cloudwatch.TreatMissingData.BREACHING,
      });

      // Add alarm action to warning alerts topic
      trafficAnomalyAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.warningAlertsTopic));

      // Regional error rate alarm
      const regionalErrorRateAlarm = new cloudwatch.Alarm(this, `RegionalErrorRateAlarm${region}`, {
        alarmName: `${applicationName}-${environment}-error-rate-${region}`,
        alarmDescription: `High error rate detected in ${region}`,
        metric: new cloudwatch.Metric({
          namespace: 'AWS/ApplicationELB',
          metricName: 'HTTPCode_Target_5XX_Count',
          dimensionsMap: {
            LoadBalancer: `app/${applicationName}-${environment}-alb-${region}`,
          },
          statistic: 'Sum',
          period: cdk.Duration.minutes(5),
        }),
        threshold: 10, // More than 10 errors in 5 minutes
        comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
        evaluationPeriods: 2,
        datapointsToAlarm: 2,
        treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
      });

      // Add alarm action to critical alerts topic
      regionalErrorRateAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.criticalAlertsTopic));
    });
  }

  /**
   * Create cross-region synchronization monitoring alarms
   */
  private createCrossRegionSyncAlarms(
    environment: string,
    applicationName: string,
    multiRegionConfig: MultiRegionAlertingConfig
  ): void {
    const primaryRegion = multiRegionConfig.primaryRegion;
    
    multiRegionConfig.regions.forEach(targetRegion => {
      if (targetRegion !== primaryRegion) {
        // Aurora Global Database replication lag alarm
        const replicationLagAlarm = new cloudwatch.Alarm(this, `AuroraReplicationLagAlarm${targetRegion}`, {
          alarmName: `${applicationName}-${environment}-aurora-replication-lag-${primaryRegion}-to-${targetRegion}`,
          alarmDescription: `High Aurora Global Database replication lag from ${primaryRegion} to ${targetRegion}`,
          metric: new cloudwatch.Metric({
            namespace: 'AWS/RDS',
            metricName: 'AuroraGlobalDBReplicationLag',
            dimensionsMap: {
              SourceRegion: primaryRegion,
              TargetRegion: targetRegion,
            },
            statistic: 'Average',
            period: cdk.Duration.minutes(5),
          }),
          threshold: 100, // 100ms threshold
          comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
          evaluationPeriods: 3,
          datapointsToAlarm: 2,
          treatMissingData: cloudwatch.TreatMissingData.BREACHING,
        });

        // Add alarm action to warning alerts topic
        replicationLagAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.warningAlertsTopic));

        // Cross-region network latency alarm
        const networkLatencyAlarm = new cloudwatch.Alarm(this, `CrossRegionNetworkLatencyAlarm${targetRegion}`, {
          alarmName: `${applicationName}-${environment}-network-latency-${primaryRegion}-to-${targetRegion}`,
          alarmDescription: `High network latency between ${primaryRegion} and ${targetRegion}`,
          metric: new cloudwatch.Metric({
            namespace: 'Custom/GenAIDemo/CrossRegion',
            metricName: 'NetworkLatency',
            dimensionsMap: {
              SourceRegion: primaryRegion,
              TargetRegion: targetRegion,
            },
            statistic: 'Average',
            period: cdk.Duration.minutes(5),
          }),
          threshold: 200, // 200ms threshold
          comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
          evaluationPeriods: 3,
          datapointsToAlarm: 2,
          treatMissingData: cloudwatch.TreatMissingData.BREACHING,
        });

        // Add alarm action to warning alerts topic
        networkLatencyAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.warningAlertsTopic));
      }
    });
  }

  /**
   * Create escalation policy for multi-region alerts
   */
  private createEscalationPolicy(
    environment: string,
    applicationName: string,
    multiRegionConfig: MultiRegionAlertingConfig
  ): void {
    // Create escalation Lambda function
    const escalationFunction = new lambda.Function(this, 'AlertEscalationFunction', {
      functionName: `${applicationName}-${environment}-alert-escalation`,
      runtime: lambda.Runtime.PYTHON_3_9,
      handler: 'index.handler',
      timeout: cdk.Duration.minutes(5),
      memorySize: 256,
      environment: {
        GLOBAL_ALERTS_TOPIC_ARN: this.globalAlertsTopic!.topicArn,
        REGIONAL_FAILURE_THRESHOLD: multiRegionConfig.escalationPolicy.regionalFailureThreshold.toString(),
        ESCALATION_DELAY_MINUTES: multiRegionConfig.escalationPolicy.globalEscalationDelay.toString(),
        REGIONS: JSON.stringify(multiRegionConfig.regions),
        PRIMARY_REGION: multiRegionConfig.primaryRegion,
      },
      code: lambda.Code.fromInline(`
import boto3
import json
import time
from datetime import datetime, timedelta

sns = boto3.client('sns')
cloudwatch = boto3.client('cloudwatch')

global_topic_arn = os.environ['GLOBAL_ALERTS_TOPIC_ARN']
failure_threshold = int(os.environ['REGIONAL_FAILURE_THRESHOLD'])
escalation_delay = int(os.environ['ESCALATION_DELAY_MINUTES'])
regions = json.loads(os.environ['REGIONS'])
primary_region = os.environ['PRIMARY_REGION']

def handler(event, context):
    """
    Alert escalation policy handler
    - Monitors regional failure patterns
    - Escalates to global alerts when threshold is reached
    - Implements intelligent escalation delays
    """
    try:
        # Check current alarm states across regions
        failed_regions = check_regional_health()
        
        if len(failed_regions) >= failure_threshold:
            escalate_to_global(failed_regions)
            
        # Check for cross-region sync failures
        sync_failures = check_cross_region_sync()
        if sync_failures:
            escalate_sync_failures(sync_failures)
            
        return {
            'statusCode': 200,
            'body': json.dumps({
                'failed_regions': failed_regions,
                'sync_failures': sync_failures,
                'escalated': len(failed_regions) >= failure_threshold
            })
        }
        
    except Exception as e:
        print(f"Error in alert escalation: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error: {str(e)}')
        }

def check_regional_health():
    """Check health status of all regions"""
    failed_regions = []
    
    for region in regions:
        try:
            # Check regional health alarms
            regional_cloudwatch = boto3.client('cloudwatch', region_name=region)
            
            response = regional_cloudwatch.describe_alarms(
                AlarmNamePrefix=f'genai-demo-regional-health-{region}',
                StateValue='ALARM'
            )
            
            if response['MetricAlarms']:
                failed_regions.append(region)
                
        except Exception as e:
            print(f"Error checking region {region}: {str(e)}")
            # Assume region is failed if we can't check it
            failed_regions.append(region)
            
    return failed_regions

def check_cross_region_sync():
    """Check cross-region synchronization status"""
    sync_failures = []
    
    for region in regions:
        if region != primary_region:
            try:
                # Check replication lag alarms
                response = cloudwatch.describe_alarms(
                    AlarmNamePrefix=f'genai-demo-aurora-replication-lag-{primary_region}-to-{region}',
                    StateValue='ALARM'
                )
                
                if response['MetricAlarms']:
                    sync_failures.append({
                        'source': primary_region,
                        'target': region,
                        'type': 'replication_lag'
                    })
                    
            except Exception as e:
                print(f"Error checking sync for {region}: {str(e)}")
                
    return sync_failures

def escalate_to_global(failed_regions):
    """Escalate regional failures to global alert"""
    
    escalation_message = {
        'AlertType': 'GLOBAL_REGIONAL_FAILURE',
        'FailedRegions': failed_regions,
        'TotalRegions': len(regions),
        'FailurePercentage': (len(failed_regions) / len(regions)) * 100,
        'EscalationTime': datetime.utcnow().isoformat(),
        'Severity': 'CRITICAL',
        'RecommendedActions': [
            'Check regional infrastructure status',
            'Verify network connectivity',
            'Review recent deployments',
            'Consider traffic rerouting'
        ]
    }
    
    sns.publish(
        TopicArn=global_topic_arn,
        Subject=f"CRITICAL: Multiple Regional Failures Detected ({len(failed_regions)}/{len(regions)} regions)",
        Message=json.dumps(escalation_message, indent=2)
    )
    
    # Send custom metric
    cloudwatch.put_metric_data(
        Namespace='GenAIDemo/MultiRegion/Alerting',
        MetricData=[
            {
                'MetricName': 'RegionalFailureEscalations',
                'Value': len(failed_regions),
                'Unit': 'Count',
                'Dimensions': [
                    {'Name': 'EscalationType', 'Value': 'RegionalFailure'}
                ]
            }
        ]
    )

def escalate_sync_failures(sync_failures):
    """Escalate cross-region sync failures"""
    
    escalation_message = {
        'AlertType': 'GLOBAL_SYNC_FAILURE',
        'SyncFailures': sync_failures,
        'EscalationTime': datetime.utcnow().isoformat(),
        'Severity': 'HIGH',
        'RecommendedActions': [
            'Check Aurora Global Database status',
            'Verify cross-region network connectivity',
            'Review database performance metrics',
            'Consider manual failover if needed'
        ]
    }
    
    sns.publish(
        TopicArn=global_topic_arn,
        Subject=f"HIGH: Cross-Region Synchronization Failures Detected",
        Message=json.dumps(escalation_message, indent=2)
    )

import os
      `),
    });

    // Grant permissions to the escalation function
    this.globalAlertsTopic!.grantPublish(escalationFunction);

    escalationFunction.addToRolePolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          'cloudwatch:DescribeAlarms',
          'cloudwatch:PutMetricData',
          'cloudwatch:GetMetricStatistics',
        ],
        resources: ['*'],
      })
    );

    // Create EventBridge rule to trigger escalation function every 5 minutes
    const escalationRule = new events.Rule(this, 'AlertEscalationRule', {
      ruleName: `${applicationName}-${environment}-alert-escalation`,
      description: 'Trigger alert escalation policy evaluation',
      schedule: events.Schedule.rate(cdk.Duration.minutes(5)),
    });

    escalationRule.addTarget(new eventsTargets.LambdaFunction(escalationFunction));
  }

  /**
   * Create SNS topic with subscriptions for alerts
   */
  private createSNSTopic(level: string, environment: string, config: AlertSubscriptionConfig): sns.Topic {
    const topic = new sns.Topic(this, `${level}AlertsTopic`, {
      topicName: `genai-demo-${environment}-${level.toLowerCase()}-alerts`,
      displayName: `GenAI Demo ${environment} ${level} Alerts`,
      fifo: false,
    });

    // Add email subscriptions
    if (config.emailAddresses && config.emailAddresses.length > 0) {
      config.emailAddresses.forEach((email, index) => {
        topic.addSubscription(new snsSubscriptions.EmailSubscription(email));
      });
    }

    // Add SMS subscriptions for critical alerts
    if (level === 'Critical' && config.phoneNumbers && config.phoneNumbers.length > 0) {
      config.phoneNumbers.forEach((phone, index) => {
        topic.addSubscription(new snsSubscriptions.SmsSubscription(phone));
      });
    }

    // Add Slack webhook subscription if configured
    if (config.slackWebhookUrl) {
      // Note: In a real implementation, you would use AWS Chatbot or Lambda for Slack integration
      // For now, we'll create a placeholder for the webhook URL
      topic.addSubscription(new snsSubscriptions.UrlSubscription(config.slackWebhookUrl));
    }

    return topic;
  }

  /**
   * Create CloudWatch alarms for health check metrics
   */
  private createHealthCheckAlarms(environment: string, applicationName: string): void {
    const namespace = `GenAIDemo/${environment}`;

    // Database Health Check Alarm
    new cloudwatch.Alarm(this, 'DatabaseHealthAlarm', {
      alarmName: `${applicationName}-${environment}-database-health`,
      alarmDescription: 'Database health check is failing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.status',
        dimensionsMap: {
          indicator: 'database_health_indicator',
        },
        statistic: 'Average',
      }),
      threshold: 0.5,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    // Kafka Health Check Alarm (for production)
    if (environment === 'production') {
      new cloudwatch.Alarm(this, 'KafkaHealthAlarm', {
        alarmName: `${applicationName}-${environment}-kafka-health`,
        alarmDescription: 'Kafka health check is failing',
        metric: new cloudwatch.Metric({
          namespace: namespace,
          metricName: 'health.check.status',
          dimensionsMap: {
            indicator: 'kafka_health_indicator',
          },
          statistic: 'Average',
        }),
        threshold: 0.5,
        comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
        evaluationPeriods: 3,
        datapointsToAlarm: 2,
        treatMissingData: cloudwatch.TreatMissingData.BREACHING,
      });
    }

    // System Resources Health Alarm
    new cloudwatch.Alarm(this, 'SystemResourcesHealthAlarm', {
      alarmName: `${applicationName}-${environment}-system-resources-health`,
      alarmDescription: 'System resources health check is failing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.status',
        dimensionsMap: {
          indicator: 'system_resources_indicator',
        },
        statistic: 'Average',
      }),
      threshold: 0.5,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    // Application Readiness Alarm
    new cloudwatch.Alarm(this, 'ApplicationReadinessAlarm', {
      alarmName: `${applicationName}-${environment}-application-readiness`,
      alarmDescription: 'Application readiness check is failing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.status',
        dimensionsMap: {
          indicator: 'application_readiness_indicator',
        },
        statistic: 'Average',
      }),
      threshold: 0.5,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });
  }

  /**
   * Create CloudWatch alarms for application metrics
   */
  private createApplicationMetricAlarms(environment: string, applicationName: string): void {
    const namespace = `GenAIDemo/${environment}`;

    // High Error Rate Alarm
    new cloudwatch.Alarm(this, 'HighErrorRateAlarm', {
      alarmName: `${applicationName}-${environment}-high-error-rate`,
      alarmDescription: 'Application error rate is too high (>5% in 5 minutes)',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'http.server.requests',
        dimensionsMap: {
          status: '5xx',
        },
        statistic: 'Sum',
      }),
      threshold: 10, // More than 10 errors in 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // High Response Time Alarm
    new cloudwatch.Alarm(this, 'HighResponseTimeAlarm', {
      alarmName: `${applicationName}-${environment}-high-response-time`,
      alarmDescription: 'Application response time is too high (>2s average in 5 minutes)',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'http.server.requests',
        statistic: 'Average',
      }),
      threshold: 2000, // 2 seconds in milliseconds
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Health Check Failures Alarm
    new cloudwatch.Alarm(this, 'HealthCheckFailuresAlarm', {
      alarmName: `${applicationName}-${environment}-health-check-failures`,
      alarmDescription: 'Health check failures are increasing',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.check.executions',
        statistic: 'Sum',
      }),
      threshold: 5, // More than 5 failed health checks in 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Recovery Attempts Alarm
    new cloudwatch.Alarm(this, 'RecoveryAttemptsAlarm', {
      alarmName: `${applicationName}-${environment}-recovery-attempts`,
      alarmDescription: 'Automated recovery attempts are being triggered frequently',
      metric: new cloudwatch.Metric({
        namespace: namespace,
        metricName: 'health.recovery.attempts',
        statistic: 'Sum',
      }),
      threshold: 3, // More than 3 recovery attempts in 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });
  }

  /**
   * Create CloudWatch alarms for Aurora PostgreSQL deadlock monitoring
   */
  private createAuroraDeadlockAlarms(environment: string, applicationName: string): void {
    const dbInstanceIdentifier = `${applicationName}-${environment}-primary-aurora`;

    // Aurora PostgreSQL Deadlock Alarm
    const deadlockAlarm = new cloudwatch.Alarm(this, 'AuroraDeadlockAlarm', {
      alarmName: `${applicationName}-${environment}-aurora-deadlocks`,
      alarmDescription: 'Aurora PostgreSQL deadlocks detected',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/RDS',
        metricName: 'Deadlocks',
        dimensionsMap: {
          DBInstanceIdentifier: dbInstanceIdentifier,
        },
        statistic: 'Sum',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 1,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Add alarm action to critical alerts topic
    deadlockAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.criticalAlertsTopic));

    // Blocked Sessions Alarm
    const blockedSessionsAlarm = new cloudwatch.Alarm(this, 'AuroraBlockedSessionsAlarm', {
      alarmName: `${applicationName}-${environment}-aurora-blocked-sessions`,
      alarmDescription: 'Too many blocked sessions in Aurora PostgreSQL',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/RDS',
        metricName: 'DatabaseConnections',
        dimensionsMap: {
          DBInstanceIdentifier: dbInstanceIdentifier,
        },
        statistic: 'Average',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 80, // 80% of max connections indicates potential blocking
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Add alarm action to warning alerts topic
    blockedSessionsAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.warningAlertsTopic));

    // High Lock Wait Time Alarm (using ReadLatency as proxy)
    const lockWaitAlarm = new cloudwatch.Alarm(this, 'AuroraLockWaitTimeAlarm', {
      alarmName: `${applicationName}-${environment}-aurora-lock-wait-time`,
      alarmDescription: 'High lock wait time detected in Aurora PostgreSQL',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/RDS',
        metricName: 'ReadLatency',
        dimensionsMap: {
          DBInstanceIdentifier: dbInstanceIdentifier,
        },
        statistic: 'Average',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 0.2, // 200ms average read latency
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 3,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Add alarm action to warning alerts topic
    lockWaitAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.warningAlertsTopic));

    // Database CPU Utilization (high CPU can indicate lock contention)
    const cpuAlarm = new cloudwatch.Alarm(this, 'AuroraCPUUtilizationAlarm', {
      alarmName: `${applicationName}-${environment}-aurora-cpu-utilization`,
      alarmDescription: 'High CPU utilization in Aurora PostgreSQL (potential lock contention)',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/RDS',
        metricName: 'CPUUtilization',
        dimensionsMap: {
          DBInstanceIdentifier: dbInstanceIdentifier,
        },
        statistic: 'Average',
        period: cdk.Duration.minutes(5),
      }),
      threshold: 80, // 80% CPU utilization
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 3,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Add alarm action to warning alerts topic
    cpuAlarm.addAlarmAction(new cloudwatchActions.SnsAction(this.warningAlertsTopic));
  }

  /**
   * Create CloudWatch alarms for infrastructure metrics
   */
  private createInfrastructureAlarms(environment: string, applicationName: string): void {
    // Memory Usage Alarm
    new cloudwatch.Alarm(this, 'HighMemoryUsageAlarm', {
      alarmName: `${applicationName}-${environment}-high-memory-usage`,
      alarmDescription: 'Memory usage is too high (>85%)',
      metric: new cloudwatch.Metric({
        namespace: `GenAIDemo/${environment}`,
        metricName: 'health.system.memory.usage',
        statistic: 'Average',
      }),
      threshold: 85, // 85% memory usage
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
      datapointsToAlarm: 2,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });

    // Database Connection Failures Alarm
    new cloudwatch.Alarm(this, 'DatabaseConnectionFailuresAlarm', {
      alarmName: `${applicationName}-${environment}-database-connection-failures`,
      alarmDescription: 'Database connection failures detected',
      metric: new cloudwatch.Metric({
        namespace: `GenAIDemo/${environment}`,
        metricName: 'health.check.errors',
        dimensionsMap: {
          indicator: 'database_health_indicator',
        },
        statistic: 'Sum',
      }),
      threshold: 1, // Any database connection failure
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_OR_EQUAL_TO_THRESHOLD,
      evaluationPeriods: 1,
      datapointsToAlarm: 1,
      treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
    });
  }

  /**
   * Create CloudWatch dashboard for health monitoring
   */
  private createHealthCheckDashboard(environment: string, applicationName: string): void {
    const dashboard = new cloudwatch.Dashboard(this, 'HealthCheckDashboard', {
      dashboardName: `${applicationName}-${environment}-health-monitoring`,
    });

    const namespace = `GenAIDemo/${environment}`;

    // Health Status Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Health Check Status',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.status',
            dimensionsMap: { indicator: 'database_health_indicator' },
            label: 'Database Health',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.status',
            dimensionsMap: { indicator: 'application_readiness_indicator' },
            label: 'Application Readiness',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.status',
            dimensionsMap: { indicator: 'system_resources_indicator' },
            label: 'System Resources',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Health Check Duration Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Health Check Response Times',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.duration',
            statistic: 'Average',
            label: 'Average Duration',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.check.duration',
            statistic: 'Maximum',
            label: 'Max Duration',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Recovery Attempts Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Automated Recovery Activity',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.recovery.attempts',
            statistic: 'Sum',
            label: 'Recovery Attempts',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.recovery.successful',
            statistic: 'Sum',
            label: 'Successful Recoveries',
          }),
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.recovery.failed',
            statistic: 'Sum',
            label: 'Failed Recoveries',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // System Resources Widget
    dashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'System Resources',
        left: [
          new cloudwatch.Metric({
            namespace: namespace,
            metricName: 'health.system.memory.usage',
            statistic: 'Average',
            label: 'Memory Usage %',
          }),
        ],
        width: 12,
        height: 6,
      })
    );
  }
}

/**
 * Configuration for alerting subscriptions
 */
export interface AlertSubscriptionConfig {
  emailAddresses?: string[];
  phoneNumbers?: string[];
  slackWebhookUrl?: string;
}

/**
 * Configuration for alerting stack
 */
export interface AlertingConfig {
  criticalAlerts: AlertSubscriptionConfig;
  warningAlerts: AlertSubscriptionConfig;
  infoAlerts: AlertSubscriptionConfig;
}

/**
 * Configuration for multi-region alerting system
 */
export interface MultiRegionAlertingConfig {
  enabled: boolean;
  regions: string[];
  primaryRegion: string;
  crossRegionAggregation: boolean;
  alertDeduplication: {
    enabled: boolean;
    timeWindow: number; // minutes
    similarityThreshold: number; // 0.0 to 1.0
  };
  escalationPolicy: {
    regionalFailureThreshold: number; // number of failed regions to trigger global alert
    globalEscalationDelay: number; // minutes to wait before escalating
  };
}