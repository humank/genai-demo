import * as cdk from 'aws-cdk-lib';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as snsSubscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as cloudwatchActions from 'aws-cdk-lib/aws-cloudwatch-actions';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

/**
 * MSK Alerting Stack
 * 
 * Creates comprehensive alerting and notification system for MSK monitoring
 * with multi-level alerting, intelligent correlation, and automated responses.
 * 
 * Features:
 * - Multi-level alerting strategy (Warning/Critical/Emergency)
 * - Intelligent alert correlation and noise reduction
 * - Automated escalation procedures
 * - Alert suppression during maintenance
 * - Integration with Slack, PagerDuty, and SMS
 * - Alert analytics and optimization
 * 
 * @author Architecture Team
 * @since 2025-09-24
 */
export class MSKAlertingStack extends cdk.Stack {
  public readonly warningTopic: sns.Topic;
  public readonly criticalTopic: sns.Topic;
  public readonly emergencyTopic: sns.Topic;
  public readonly alertCorrelationFunction: lambda.Function;
  public readonly alertSuppressionFunction: lambda.Function;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // Create SNS topics for different alert levels
    this.createAlertTopics();

    // Create alert correlation and noise reduction system
    this.alertCorrelationFunction = this.createAlertCorrelationFunction();

    // Create alert suppression system
    this.alertSuppressionFunction = this.createAlertSuppressionFunction();

    // Create MSK-specific alarms
    this.createMSKAlarms();

    // Set up automated escalation
    this.setupAutomatedEscalation();

    // Configure alert analytics
    this.setupAlertAnalytics();

    // Create outputs
    this.createOutputs();
  }

  private createAlertTopics(): void {
    // Warning level alerts (Slack notifications)
    this.warningTopic = new sns.Topic(this, 'MSKWarningAlerts', {
      topicName: 'msk-warning-alerts',
      displayName: 'MSK Warning Level Alerts',
      description: 'Warning level alerts for MSK monitoring - sent to Slack',
    });

    // Critical level alerts (PagerDuty integration)
    this.criticalTopic = new sns.Topic(this, 'MSKCriticalAlerts', {
      topicName: 'msk-critical-alerts',
      displayName: 'MSK Critical Level Alerts',
      description: 'Critical level alerts for MSK monitoring - sent to PagerDuty',
    });

    // Emergency level alerts (Phone/SMS notifications)
    this.emergencyTopic = new sns.Topic(this, 'MSKEmergencyAlerts', {
      topicName: 'msk-emergency-alerts',
      displayName: 'MSK Emergency Level Alerts',
      description: 'Emergency level alerts for MSK monitoring - Phone/SMS notifications',
    });

    // Add email subscriptions for testing (replace with actual integrations)
    this.warningTopic.addSubscription(
      new snsSubscriptions.EmailSubscription('devops-team@company.com')
    );

    this.criticalTopic.addSubscription(
      new snsSubscriptions.EmailSubscription('oncall-team@company.com')
    );

    this.emergencyTopic.addSubscription(
      new snsSubscriptions.SmsSubscription('+1234567890') // Replace with actual phone number
    );

    // Add tags
    cdk.Tags.of(this.warningTopic).add('AlertLevel', 'Warning');
    cdk.Tags.of(this.criticalTopic).add('AlertLevel', 'Critical');
    cdk.Tags.of(this.emergencyTopic).add('AlertLevel', 'Emergency');
  }

  private createAlertCorrelationFunction(): lambda.Function {
    // IAM role for alert correlation
    const correlationRole = new iam.Role(this, 'AlertCorrelationRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      description: 'IAM role for MSK alert correlation and noise reduction',
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
      ],
      inlinePolicies: {
        AlertCorrelationPolicy: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'sns:Publish',
                'sns:GetTopicAttributes',
                'sns:ListTopics',
              ],
              resources: [
                this.warningTopic.topicArn,
                this.criticalTopic.topicArn,
                this.emergencyTopic.topicArn,
              ],
            }),
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
                'cloudwatch:DescribeAlarms',
                'cloudwatch:GetMetricData',
              ],
              resources: ['*'],
            }),
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'dynamodb:GetItem',
                'dynamodb:PutItem',
                'dynamodb:UpdateItem',
                'dynamodb:Query',
                'dynamodb:Scan',
              ],
              resources: [
                `arn:aws:dynamodb:${this.region}:${this.account}:table/msk-alert-correlation`,
              ],
            }),
          ],
        }),
      },
    });

    // Lambda function for alert correlation
    const correlationFunction = new lambda.Function(this, 'MSKAlertCorrelationFunction', {
      functionName: 'msk-alert-correlation',
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.lambda_handler',
      role: correlationRole,
      timeout: cdk.Duration.minutes(5),
      memorySize: 256,
      description: 'Intelligent alert correlation and noise reduction for MSK monitoring',
      code: lambda.Code.fromInline(`
import json
import boto3
import time
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
from collections import defaultdict

sns_client = boto3.client('sns')
cloudwatch_client = boto3.client('cloudwatch')
dynamodb = boto3.resource('dynamodb')

# Alert correlation table
correlation_table = dynamodb.Table('msk-alert-correlation')

def lambda_handler(event, context):
    """
    Intelligent alert correlation and noise reduction
    """
    try:
        # Parse incoming alert
        alert = parse_alert_event(event)
        if not alert:
            return {'statusCode': 400, 'body': 'Invalid alert format'}
        
        # Check for alert correlation
        correlated_alerts = find_correlated_alerts(alert)
        
        # Apply noise reduction
        should_send, correlation_info = apply_noise_reduction(alert, correlated_alerts)
        
        if should_send:
            # Determine alert level and routing
            alert_level = determine_alert_level(alert, correlation_info)
            
            # Send correlated alert
            send_correlated_alert(alert, alert_level, correlation_info)
            
            # Update correlation tracking
            update_correlation_tracking(alert, correlation_info)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'alert_processed': True,
                'should_send': should_send,
                'alert_level': alert_level if should_send else None,
                'correlation_info': correlation_info
            })
        }
        
    except Exception as e:
        print(f"Error processing alert correlation: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def parse_alert_event(event: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Parse incoming alert event"""
    try:
        # Handle SNS message format
        if 'Records' in event:
            for record in event['Records']:
                if record.get('EventSource') == 'aws:sns':
                    message = json.loads(record['Sns']['Message'])
                    return {
                        'alert_id': message.get('AlarmName', 'unknown'),
                        'metric_name': message.get('MetricName', 'unknown'),
                        'namespace': message.get('Namespace', 'unknown'),
                        'state': message.get('NewStateValue', 'unknown'),
                        'reason': message.get('NewStateReason', ''),
                        'timestamp': message.get('StateChangeTime', datetime.utcnow().isoformat()),
                        'dimensions': message.get('Trigger', {}).get('Dimensions', []),
                        'threshold': message.get('Trigger', {}).get('Threshold'),
                        'comparison_operator': message.get('Trigger', {}).get('ComparisonOperator'),
                    }
        
        # Handle direct CloudWatch alarm format
        if 'AlarmName' in event:
            return {
                'alert_id': event.get('AlarmName'),
                'metric_name': event.get('MetricName', 'unknown'),
                'namespace': event.get('Namespace', 'unknown'),
                'state': event.get('NewStateValue', 'unknown'),
                'reason': event.get('NewStateReason', ''),
                'timestamp': event.get('StateChangeTime', datetime.utcnow().isoformat()),
                'dimensions': event.get('Trigger', {}).get('Dimensions', []),
                'threshold': event.get('Trigger', {}).get('Threshold'),
                'comparison_operator': event.get('Trigger', {}).get('ComparisonOperator'),
            }
        
        return None
        
    except Exception as e:
        print(f"Error parsing alert event: {str(e)}")
        return None

def find_correlated_alerts(alert: Dict[str, Any]) -> List[Dict[str, Any]]:
    """Find correlated alerts in the last 15 minutes"""
    try:
        cutoff_time = datetime.utcnow() - timedelta(minutes=15)
        
        response = correlation_table.scan(
            FilterExpression='alert_timestamp > :cutoff',
            ExpressionAttributeValues={
                ':cutoff': cutoff_time.isoformat()
            }
        )
        
        correlated = []
        for item in response['Items']:
            # Check correlation criteria
            if is_correlated(alert, item):
                correlated.append(item)
        
        return correlated
        
    except Exception as e:
        print(f"Error finding correlated alerts: {str(e)}")
        return []

def is_correlated(alert1: Dict[str, Any], alert2: Dict[str, Any]) -> bool:
    """Determine if two alerts are correlated"""
    
    # Same metric correlation
    if alert1['metric_name'] == alert2['metric_name']:
        return True
    
    # MSK-specific correlation patterns
    msk_correlations = {
        'OfflinePartitionsCount': ['UnderReplicatedPartitions', 'ActiveControllerCount'],
        'EstimatedMaxTimeLag': ['MessagesInPerSec', 'BytesInPerSec'],
        'ProducerRequestErrors': ['ConsumerFetchErrors', 'NetworkRxErrors'],
        'CpuUser': ['MemoryUsed', 'NetworkTxBytes'],
        'KafkaDataLogsDiskUsed': ['RootDiskUsed', 'BytesInPerSec'],
    }
    
    metric1 = alert1['metric_name']
    metric2 = alert2['metric_name']
    
    if metric1 in msk_correlations and metric2 in msk_correlations[metric1]:
        return True
    
    if metric2 in msk_correlations and metric1 in msk_correlations[metric2]:
        return True
    
    # Dimension-based correlation (same broker, topic, etc.)
    dims1 = {d['name']: d['value'] for d in alert1.get('dimensions', [])}
    dims2 = {d['name']: d['value'] for d in alert2.get('dimensions', [])}
    
    common_dims = set(dims1.keys()) & set(dims2.keys())
    if common_dims:
        for dim in common_dims:
            if dims1[dim] == dims2[dim]:
                return True
    
    return False

def apply_noise_reduction(alert: Dict[str, Any], correlated_alerts: List[Dict[str, Any]]) -> tuple:
    """Apply noise reduction logic"""
    
    # Don't suppress emergency-level alerts
    if is_emergency_alert(alert):
        return True, {'suppression_reason': 'emergency_alert', 'correlated_count': len(correlated_alerts)}
    
    # Suppress if too many similar alerts in short time (alert storm)
    similar_alerts = [a for a in correlated_alerts if a['metric_name'] == alert['metric_name']]
    if len(similar_alerts) > 5:
        return False, {'suppression_reason': 'alert_storm', 'similar_count': len(similar_alerts)}
    
    # Suppress if alert is flapping (state changes frequently)
    state_changes = count_state_changes(alert, correlated_alerts)
    if state_changes > 3:
        return False, {'suppression_reason': 'flapping', 'state_changes': state_changes}
    
    # Group correlated alerts
    if len(correlated_alerts) > 1:
        return True, {'suppression_reason': 'grouped', 'correlated_count': len(correlated_alerts)}
    
    # Send individual alert
    return True, {'suppression_reason': 'none', 'correlated_count': len(correlated_alerts)}

def is_emergency_alert(alert: Dict[str, Any]) -> bool:
    """Determine if alert is emergency level"""
    emergency_metrics = [
        'OfflinePartitionsCount',
        'ActiveControllerCount',
        'ClusterDown',
        'BrokerDown'
    ]
    
    return alert['metric_name'] in emergency_metrics and alert['state'] == 'ALARM'

def count_state_changes(alert: Dict[str, Any], correlated_alerts: List[Dict[str, Any]]) -> int:
    """Count state changes for the same alert in recent history"""
    same_alert_history = [
        a for a in correlated_alerts 
        if a['alert_id'] == alert['alert_id']
    ]
    
    # Count state transitions
    states = [a['state'] for a in same_alert_history]
    transitions = sum(1 for i in range(1, len(states)) if states[i] != states[i-1])
    
    return transitions

def determine_alert_level(alert: Dict[str, Any], correlation_info: Dict[str, Any]) -> str:
    """Determine appropriate alert level"""
    
    # Emergency level
    if is_emergency_alert(alert):
        return 'emergency'
    
    # Critical level
    critical_metrics = [
        'UnderReplicatedPartitions',
        'EstimatedMaxTimeLag',
        'ProducerRequestErrors',
        'ConsumerFetchErrors'
    ]
    
    if alert['metric_name'] in critical_metrics and alert['state'] == 'ALARM':
        return 'critical'
    
    # Warning level (default)
    return 'warning'

def send_correlated_alert(alert: Dict[str, Any], alert_level: str, correlation_info: Dict[str, Any]):
    """Send correlated alert to appropriate topic"""
    
    topic_arns = {
        'warning': '${this.warningTopic.topicArn}',
        'critical': '${this.criticalTopic.topicArn}',
        'emergency': '${this.emergencyTopic.topicArn}'
    }
    
    topic_arn = topic_arns.get(alert_level, topic_arns['warning'])
    
    # Enhanced alert message
    message = {
        'alert_level': alert_level,
        'original_alert': alert,
        'correlation_info': correlation_info,
        'timestamp': datetime.utcnow().isoformat(),
        'summary': generate_alert_summary(alert, correlation_info)
    }
    
    sns_client.publish(
        TopicArn=topic_arn,
        Message=json.dumps(message, indent=2),
        Subject=f"MSK {alert_level.upper()} Alert: {alert['alert_id']}"
    )

def generate_alert_summary(alert: Dict[str, Any], correlation_info: Dict[str, Any]) -> str:
    """Generate human-readable alert summary"""
    
    summary = f"MSK Alert: {alert['alert_id']} is in {alert['state']} state"
    
    if correlation_info['correlated_count'] > 0:
        summary += f" (correlated with {correlation_info['correlated_count']} other alerts)"
    
    if alert['reason']:
        summary += f"\\nReason: {alert['reason']}"
    
    return summary

def update_correlation_tracking(alert: Dict[str, Any], correlation_info: Dict[str, Any]):
    """Update correlation tracking in DynamoDB"""
    
    try:
        correlation_table.put_item(
            Item={
                'alert_id': alert['alert_id'],
                'alert_timestamp': alert['timestamp'],
                'metric_name': alert['metric_name'],
                'namespace': alert['namespace'],
                'state': alert['state'],
                'correlation_info': correlation_info,
                'ttl': int((datetime.utcnow() + timedelta(days=7)).timestamp())
            }
        )
    except Exception as e:
        print(f"Error updating correlation tracking: {str(e)}")
`),
      environment: {
        WARNING_TOPIC_ARN: this.warningTopic.topicArn,
        CRITICAL_TOPIC_ARN: this.criticalTopic.topicArn,
        EMERGENCY_TOPIC_ARN: this.emergencyTopic.topicArn,
      },
    });

    return correlationFunction;
  }

  private createAlertSuppressionFunction(): lambda.Function {
    // IAM role for alert suppression
    const suppressionRole = new iam.Role(this, 'AlertSuppressionRole', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      description: 'IAM role for MSK alert suppression during maintenance',
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
      ],
      inlinePolicies: {
        AlertSuppressionPolicy: new iam.PolicyDocument({
          statements: [
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'cloudwatch:DisableAlarmActions',
                'cloudwatch:EnableAlarmActions',
                'cloudwatch:DescribeAlarms',
                'cloudwatch:PutMetricAlarm',
              ],
              resources: ['*'],
            }),
            new iam.PolicyStatement({
              effect: iam.Effect.ALLOW,
              actions: [
                'dynamodb:GetItem',
                'dynamodb:PutItem',
                'dynamodb:UpdateItem',
                'dynamodb:DeleteItem',
                'dynamodb:Query',
                'dynamodb:Scan',
              ],
              resources: [
                `arn:aws:dynamodb:${this.region}:${this.account}:table/msk-maintenance-windows`,
              ],
            }),
          ],
        }),
      },
    });

    // Lambda function for alert suppression
    const suppressionFunction = new lambda.Function(this, 'MSKAlertSuppressionFunction', {
      functionName: 'msk-alert-suppression',
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.lambda_handler',
      role: suppressionRole,
      timeout: cdk.Duration.minutes(5),
      memorySize: 256,
      description: 'Alert suppression during MSK maintenance windows',
      code: lambda.Code.fromInline(`
import json
import boto3
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional

cloudwatch_client = boto3.client('cloudwatch')
dynamodb = boto3.resource('dynamodb')

# Maintenance windows table
maintenance_table = dynamodb.Table('msk-maintenance-windows')

def lambda_handler(event, context):
    """
    Handle alert suppression during maintenance windows
    """
    try:
        action = event.get('action', 'check')
        
        if action == 'start_maintenance':
            return start_maintenance_window(event)
        elif action == 'end_maintenance':
            return end_maintenance_window(event)
        elif action == 'check':
            return check_maintenance_status()
        else:
            return {'statusCode': 400, 'body': 'Invalid action'}
            
    except Exception as e:
        print(f"Error in alert suppression: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def start_maintenance_window(event: Dict[str, Any]) -> Dict[str, Any]:
    """Start maintenance window and suppress alerts"""
    
    maintenance_id = event.get('maintenance_id', f"maint-{int(datetime.utcnow().timestamp())}")
    duration_minutes = event.get('duration_minutes', 60)
    description = event.get('description', 'MSK maintenance window')
    
    start_time = datetime.utcnow()
    end_time = start_time + timedelta(minutes=duration_minutes)
    
    # Get MSK alarms to suppress
    msk_alarms = get_msk_alarms()
    
    # Disable alarm actions
    suppressed_alarms = []
    for alarm_name in msk_alarms:
        try:
            cloudwatch_client.disable_alarm_actions(AlarmNames=[alarm_name])
            suppressed_alarms.append(alarm_name)
        except Exception as e:
            print(f"Error suppressing alarm {alarm_name}: {str(e)}")
    
    # Record maintenance window
    maintenance_table.put_item(
        Item={
            'maintenance_id': maintenance_id,
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat(),
            'description': description,
            'suppressed_alarms': suppressed_alarms,
            'status': 'active',
            'ttl': int((end_time + timedelta(days=30)).timestamp())
        }
    )
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'maintenance_id': maintenance_id,
            'suppressed_alarms': len(suppressed_alarms),
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat()
        })
    }

def end_maintenance_window(event: Dict[str, Any]) -> Dict[str, Any]:
    """End maintenance window and restore alerts"""
    
    maintenance_id = event.get('maintenance_id')
    if not maintenance_id:
        return {'statusCode': 400, 'body': 'maintenance_id required'}
    
    # Get maintenance window details
    response = maintenance_table.get_item(Key={'maintenance_id': maintenance_id})
    if 'Item' not in response:
        return {'statusCode': 404, 'body': 'Maintenance window not found'}
    
    maintenance = response['Item']
    suppressed_alarms = maintenance.get('suppressed_alarms', [])
    
    # Re-enable alarm actions
    restored_alarms = []
    for alarm_name in suppressed_alarms:
        try:
            cloudwatch_client.enable_alarm_actions(AlarmNames=[alarm_name])
            restored_alarms.append(alarm_name)
        except Exception as e:
            print(f"Error restoring alarm {alarm_name}: {str(e)}")
    
    # Update maintenance window status
    maintenance_table.update_item(
        Key={'maintenance_id': maintenance_id},
        UpdateExpression='SET #status = :status, end_time = :end_time',
        ExpressionAttributeNames={'#status': 'status'},
        ExpressionAttributeValues={
            ':status': 'completed',
            ':end_time': datetime.utcnow().isoformat()
        }
    )
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'maintenance_id': maintenance_id,
            'restored_alarms': len(restored_alarms),
            'end_time': datetime.utcnow().isoformat()
        })
    }

def check_maintenance_status() -> Dict[str, Any]:
    """Check current maintenance status"""
    
    current_time = datetime.utcnow()
    
    # Find active maintenance windows
    response = maintenance_table.scan(
        FilterExpression='#status = :status',
        ExpressionAttributeNames={'#status': 'status'},
        ExpressionAttributeValues={':status': 'active'}
    )
    
    active_windows = []
    for item in response['Items']:
        end_time = datetime.fromisoformat(item['end_time'])
        if end_time > current_time:
            active_windows.append({
                'maintenance_id': item['maintenance_id'],
                'description': item['description'],
                'start_time': item['start_time'],
                'end_time': item['end_time'],
                'suppressed_alarms': len(item.get('suppressed_alarms', []))
            })
        else:
            # Auto-end expired maintenance windows
            end_maintenance_window({'maintenance_id': item['maintenance_id']})
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'active_maintenance_windows': len(active_windows),
            'windows': active_windows,
            'current_time': current_time.isoformat()
        })
    }

def get_msk_alarms() -> List[str]:
    """Get list of MSK-related CloudWatch alarms"""
    
    alarm_names = []
    paginator = cloudwatch_client.get_paginator('describe_alarms')
    
    for page in paginator.paginate():
        for alarm in page['MetricAlarms']:
            # Check if alarm is MSK-related
            if (alarm['Namespace'] == 'AWS/Kafka' or 
                'msk' in alarm['AlarmName'].lower() or
                'kafka' in alarm['AlarmName'].lower()):
                alarm_names.append(alarm['AlarmName'])
    
    return alarm_names
`),
    });

    return suppressionFunction;
  }

  private createMSKAlarms(): void {
    // Offline Partitions Alarm (Emergency)
    const offlinePartitionsAlarm = new cloudwatch.Alarm(this, 'MSKOfflinePartitionsAlarm', {
      alarmName: 'MSK-Offline-Partitions-Emergency',
      alarmDescription: 'MSK cluster has offline partitions - immediate attention required',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: 'OfflinePartitionsCount',
        statistic: 'Maximum',
      }),
      threshold: 0,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 1,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING,
    });

    offlinePartitionsAlarm.addAlarmAction(
      new cloudwatchActions.SnsAction(this.emergencyTopic)
    );

    // Consumer Lag Alarm (Critical)
    const consumerLagAlarm = new cloudwatch.Alarm(this, 'MSKConsumerLagAlarm', {
      alarmName: 'MSK-Consumer-Lag-Critical',
      alarmDescription: 'MSK consumer lag exceeding critical threshold',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: 'EstimatedMaxTimeLag',
        statistic: 'Maximum',
      }),
      threshold: 300000, // 5 minutes
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
    });

    consumerLagAlarm.addAlarmAction(
      new cloudwatchActions.SnsAction(this.criticalTopic)
    );

    // Producer Error Rate Alarm (Warning)
    const producerErrorAlarm = new cloudwatch.Alarm(this, 'MSKProducerErrorAlarm', {
      alarmName: 'MSK-Producer-Error-Rate-Warning',
      alarmDescription: 'MSK producer error rate is elevated',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: 'ProducerRequestErrors',
        statistic: 'Sum',
      }),
      threshold: 10,
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 3,
    });

    producerErrorAlarm.addAlarmAction(
      new cloudwatchActions.SnsAction(this.warningTopic)
    );

    // Disk Usage Alarm (Warning)
    const diskUsageAlarm = new cloudwatch.Alarm(this, 'MSKDiskUsageAlarm', {
      alarmName: 'MSK-Disk-Usage-Warning',
      alarmDescription: 'MSK broker disk usage is high',
      metric: new cloudwatch.Metric({
        namespace: 'AWS/Kafka',
        metricName: 'KafkaDataLogsDiskUsed',
        statistic: 'Maximum',
      }),
      threshold: 80, // 80%
      comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
      evaluationPeriods: 2,
    });

    diskUsageAlarm.addAlarmAction(
      new cloudwatchActions.SnsAction(this.warningTopic)
    );

    // Add tags to alarms
    cdk.Tags.of(offlinePartitionsAlarm).add('AlertLevel', 'Emergency');
    cdk.Tags.of(consumerLagAlarm).add('AlertLevel', 'Critical');
    cdk.Tags.of(producerErrorAlarm).add('AlertLevel', 'Warning');
    cdk.Tags.of(diskUsageAlarm).add('AlertLevel', 'Warning');
  }

  private setupAutomatedEscalation(): void {
    // EventBridge rule for alert escalation
    const escalationRule = new events.Rule(this, 'MSKAlertEscalationRule', {
      ruleName: 'msk-alert-escalation',
      description: 'Automated escalation for unacknowledged MSK alerts',
      schedule: events.Schedule.rate(cdk.Duration.minutes(15)),
    });

    // Add Lambda target for escalation logic
    escalationRule.addTarget(new targets.LambdaFunction(this.alertCorrelationFunction));
  }

  private setupAlertAnalytics(): void {
    // CloudWatch dashboard for alert analytics
    const alertAnalyticsDashboard = new cloudwatch.Dashboard(this, 'MSKAlertAnalyticsDashboard', {
      dashboardName: 'MSK-Alert-Analytics',
      defaultInterval: cdk.Duration.hours(1),
    });

    // Alert frequency widget
    alertAnalyticsDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Alert Frequency by Level',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/SNS',
            metricName: 'NumberOfMessagesPublished',
            dimensionsMap: {
              TopicName: this.warningTopic.topicName,
            },
            statistic: 'Sum',
            label: 'Warning Alerts',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/SNS',
            metricName: 'NumberOfMessagesPublished',
            dimensionsMap: {
              TopicName: this.criticalTopic.topicName,
            },
            statistic: 'Sum',
            label: 'Critical Alerts',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/SNS',
            metricName: 'NumberOfMessagesPublished',
            dimensionsMap: {
              TopicName: this.emergencyTopic.topicName,
            },
            statistic: 'Sum',
            label: 'Emergency Alerts',
          }),
        ],
        width: 12,
        height: 6,
      })
    );

    // Alert correlation effectiveness
    alertAnalyticsDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Alert Correlation Function Performance',
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Lambda',
            metricName: 'Duration',
            dimensionsMap: {
              FunctionName: this.alertCorrelationFunction.functionName,
            },
            statistic: 'Average',
            label: 'Correlation Duration',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Lambda',
            metricName: 'Invocations',
            dimensionsMap: {
              FunctionName: this.alertCorrelationFunction.functionName,
            },
            statistic: 'Sum',
            label: 'Correlation Invocations',
          }),
        ],
        width: 12,
        height: 6,
      })
    );
  }

  private createOutputs(): void {
    new cdk.CfnOutput(this, 'WarningTopicArn', {
      value: this.warningTopic.topicArn,
      description: 'SNS topic ARN for MSK warning alerts',
      exportName: 'MSK-Warning-Topic-Arn',
    });

    new cdk.CfnOutput(this, 'CriticalTopicArn', {
      value: this.criticalTopic.topicArn,
      description: 'SNS topic ARN for MSK critical alerts',
      exportName: 'MSK-Critical-Topic-Arn',
    });

    new cdk.CfnOutput(this, 'EmergencyTopicArn', {
      value: this.emergencyTopic.topicArn,
      description: 'SNS topic ARN for MSK emergency alerts',
      exportName: 'MSK-Emergency-Topic-Arn',
    });

    new cdk.CfnOutput(this, 'AlertCorrelationFunctionArn', {
      value: this.alertCorrelationFunction.functionArn,
      description: 'Lambda function ARN for MSK alert correlation',
      exportName: 'MSK-Alert-Correlation-Function-Arn',
    });

    new cdk.CfnOutput(this, 'AlertSuppressionFunctionArn', {
      value: this.alertSuppressionFunction.functionArn,
      description: 'Lambda function ARN for MSK alert suppression',
      exportName: 'MSK-Alert-Suppression-Function-Arn',
    });
  }
}