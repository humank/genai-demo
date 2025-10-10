import * as cdk from 'aws-cdk-lib';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as lambdaEventSources from 'aws-cdk-lib/aws-lambda-event-sources';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import { Construct } from 'constructs';

export interface CrossRegionSyncStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly kmsKey: kms.IKey;
    readonly region: string;
    readonly isPrimaryRegion: boolean;
    readonly targetRegions: string[];
    readonly mskClusterArn?: string;
    readonly existingEventBus?: events.IEventBus;
}

/**
 * Cross-Region Sync Stack for Active-Active Multi-Region Architecture
 * 
 * Features:
 * - Cross-region EventBridge replication with ordering guarantees
 * - Event filtering and routing mechanisms
 * - Retry logic for failed event replication
 * - Integration with existing MSK and EventBridge infrastructure
 * - Comprehensive monitoring and alerting
 * 
 * Created: 2025年9月30日 下午2:34 (台北時間)
 * Task: 4.1 - Cross-Region Event Replication and Synchronization
 */
export class CrossRegionSyncStack extends cdk.Stack {
    public readonly eventBus: events.IEventBus;
    public readonly crossRegionRole: iam.Role;
    public readonly eventReplicationFunction: lambda.Function;
    public readonly deadLetterQueue: sqs.Queue;
    public readonly alertTopic: sns.Topic;
    public readonly eventOrderingQueue: sqs.Queue;
    public readonly eventFilterFunction: lambda.Function;

    constructor(scope: Construct, id: string, props: CrossRegionSyncStackProps) {
        super(scope, id, props);

        const { 
            environment, 
            projectName, 
            kmsKey, 
            region, 
            isPrimaryRegion, 
            targetRegions,
            mskClusterArn,
            existingEventBus
        } = props;

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'CrossRegionSync');
        cdk.Tags.of(this).add('Region', region);
        cdk.Tags.of(this).add('RegionType', isPrimaryRegion ? 'Primary' : 'Secondary');
        cdk.Tags.of(this).add('Purpose', 'CrossRegionEventReplication');

        // Create alert topic for cross-region sync monitoring
        this.alertTopic = new sns.Topic(this, 'CrossRegionSyncAlertTopic', {
            displayName: 'Cross-Region Sync Alert Topic',
            topicName: `${projectName}-${environment}-cross-region-sync-alerts`,
            masterKey: kmsKey,
        });

        // Create or use existing EventBridge custom bus
        this.eventBus = existingEventBus || new events.EventBus(this, 'CrossRegionEventBus', {
            eventBusName: `${projectName}-${environment}-cross-region-events`,
            kmsKey: kmsKey,
        });

        // Create dead letter queue for failed events
        this.deadLetterQueue = this.createDeadLetterQueue(kmsKey);

        // Create event ordering queue for sequence preservation
        this.eventOrderingQueue = this.createEventOrderingQueue(kmsKey);

        // Create cross-region IAM role
        this.crossRegionRole = this.createCrossRegionRole(targetRegions);

        // Create event filter function
        this.eventFilterFunction = this.createEventFilterFunction(environment, kmsKey);

        // Create event replication function
        this.eventReplicationFunction = this.createEventReplicationFunction(
            environment, 
            targetRegions, 
            kmsKey,
            mskClusterArn
        );

        // Create EventBridge rules for cross-region replication
        this.createEventBridgeRules(targetRegions);

        // Create monitoring and alerting
        this.createMonitoringAndAlerting(environment);

        // Create outputs
        this.createOutputs(environment);
    }

    /**
     * Create dead letter queue for failed event replication
     */
    private createDeadLetterQueue(kmsKey: kms.IKey): sqs.Queue {
        return new sqs.Queue(this, 'CrossRegionSyncDLQ', {
            queueName: `${this.stackName}-cross-region-sync-dlq.fifo`,
            encryption: sqs.QueueEncryption.KMS,
            encryptionMasterKey: kmsKey,
            fifo: true,
            contentBasedDeduplication: true,
            retentionPeriod: cdk.Duration.days(14),
            visibilityTimeout: cdk.Duration.minutes(5),
        });
    }

    /**
     * Create event ordering queue for sequence preservation
     */
    private createEventOrderingQueue(kmsKey: kms.IKey): sqs.Queue {
        return new sqs.Queue(this, 'EventOrderingQueue', {
            queueName: `${this.stackName}-event-ordering-queue.fifo`,
            encryption: sqs.QueueEncryption.KMS,
            encryptionMasterKey: kmsKey,
            fifo: true,
            contentBasedDeduplication: true,
            retentionPeriod: cdk.Duration.days(4),
            visibilityTimeout: cdk.Duration.minutes(2),
            deadLetterQueue: {
                queue: this.deadLetterQueue,
                maxReceiveCount: 3,
            },
        });
    }

    /**
     * Create cross-region IAM role with necessary permissions
     */
    private createCrossRegionRole(targetRegions: string[]): iam.Role {
        const role = new iam.Role(this, 'CrossRegionSyncRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            description: 'Cross-region event replication role',
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AWSXRayDaemonWriteAccess'),
            ],
        });

        // Add EventBridge permissions for all target regions
        targetRegions.forEach(targetRegion => {
            role.addToPolicy(new iam.PolicyStatement({
                effect: iam.Effect.ALLOW,
                actions: [
                    'events:PutEvents',
                    'events:DescribeRule',
                    'events:ListTargetsByRule',
                ],
                resources: [
                    `arn:aws:events:${targetRegion}:${this.account}:event-bus/*`,
                    `arn:aws:events:${targetRegion}:${this.account}:rule/*`,
                ],
            }));
        });

        // Add SQS permissions
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'sqs:SendMessage',
                'sqs:ReceiveMessage',
                'sqs:DeleteMessage',
                'sqs:GetQueueAttributes',
            ],
            resources: [
                this.deadLetterQueue.queueArn,
                this.eventOrderingQueue.queueArn,
            ],
        }));

        // Add SNS permissions for alerting
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'sns:Publish',
            ],
            resources: [this.alertTopic.topicArn],
        }));

        // Add CloudWatch Logs permissions
        role.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
            ],
            resources: [`arn:aws:logs:${this.region}:${this.account}:log-group:/aws/lambda/*`],
        }));

        return role;
    }

    /**
     * Create event filter function for intelligent event routing
     */
    private createEventFilterFunction(environment: string, kmsKey: kms.IKey): lambda.Function {
        const logGroup = new logs.LogGroup(this, 'EventFilterFunctionLogGroup', {
            logGroupName: `/aws/lambda/${this.stackName}-event-filter`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            encryptionKey: kmsKey,
        });

        return new lambda.Function(this, 'EventFilterFunction', {
            functionName: `${this.stackName}-event-filter`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(1),
            memorySize: 256,
            role: this.crossRegionRole,
            environment: {
                'ENVIRONMENT': environment,
                'REGION': this.region,
                'EVENT_ORDERING_QUEUE_URL': this.eventOrderingQueue.queueUrl,
                'ALERT_TOPIC_ARN': this.alertTopic.topicArn,
                'LOG_LEVEL': 'INFO',
            },
            code: lambda.Code.fromInline(`
import json
import boto3
import os
import logging
from datetime import datetime, timezone
from typing import Dict, List, Any

# Configure logging
logger = logging.getLogger()
logger.setLevel(os.environ.get('LOG_LEVEL', 'INFO'))

# Initialize AWS clients
sqs = boto3.client('sqs')
sns = boto3.client('sns')

def handler(event, context):
    """
    Event filter function for cross-region replication
    Filters and routes events based on business rules
    """
    try:
        logger.info(f"Processing event filter request: {json.dumps(event, default=str)}")
        
        # Extract event details
        detail = event.get('detail', {})
        source = event.get('source', '')
        detail_type = event.get('detail-type', '')
        
        # Apply filtering rules
        should_replicate = apply_filtering_rules(source, detail_type, detail)
        
        if should_replicate:
            # Add event to ordering queue for sequence preservation
            queue_message = {
                'eventId': event.get('id', ''),
                'source': source,
                'detailType': detail_type,
                'detail': detail,
                'timestamp': datetime.now(timezone.utc).isoformat(),
                'region': os.environ['REGION'],
                'replicationRequired': True
            }
            
            # Send to ordering queue with message group ID for FIFO
            message_group_id = f"{source}-{detail.get('aggregateId', 'default')}"
            
            sqs.send_message(
                QueueUrl=os.environ['EVENT_ORDERING_QUEUE_URL'],
                MessageBody=json.dumps(queue_message),
                MessageGroupId=message_group_id,
                MessageDeduplicationId=event.get('id', context.aws_request_id)
            )
            
            logger.info(f"Event queued for replication: {event.get('id', '')}")
        else:
            logger.info(f"Event filtered out: {event.get('id', '')} - {source}")
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'eventId': event.get('id', ''),
                'shouldReplicate': should_replicate,
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
        }
        
    except Exception as e:
        logger.error(f"Error in event filter: {str(e)}")
        
        # Send alert for filtering failures
        try:
            sns.publish(
                TopicArn=os.environ['ALERT_TOPIC_ARN'],
                Subject='Cross-Region Event Filter Error',
                Message=f"Error filtering event: {str(e)}\\nEvent: {json.dumps(event, default=str)}"
            )
        except Exception as alert_error:
            logger.error(f"Failed to send alert: {str(alert_error)}")
        
        raise e

def apply_filtering_rules(source: str, detail_type: str, detail: Dict[str, Any]) -> bool:
    """
    Apply business rules to determine if event should be replicated
    """
    # Rule 1: Always replicate critical business events
    critical_sources = [
        'genai-demo.customer',
        'genai-demo.order',
        'genai-demo.payment',
        'genai-demo.inventory'
    ]
    
    if source in critical_sources:
        return True
    
    # Rule 2: Replicate events with high priority
    if detail.get('priority') == 'HIGH':
        return True
    
    # Rule 3: Filter out internal system events
    internal_sources = [
        'genai-demo.monitoring',
        'genai-demo.health-check',
        'genai-demo.metrics'
    ]
    
    if source in internal_sources:
        return False
    
    # Rule 4: Replicate events that affect data consistency
    consistency_events = [
        'EntityCreated',
        'EntityUpdated',
        'EntityDeleted',
        'StateChanged'
    ]
    
    if detail_type in consistency_events:
        return True
    
    # Rule 5: Filter out events older than 5 minutes (stale events)
    event_timestamp = detail.get('timestamp')
    if event_timestamp:
        try:
            event_time = datetime.fromisoformat(event_timestamp.replace('Z', '+00:00'))
            current_time = datetime.now(timezone.utc)
            age_minutes = (current_time - event_time).total_seconds() / 60
            
            if age_minutes > 5:
                logger.warning(f"Filtering out stale event: {age_minutes:.2f} minutes old")
                return False
        except Exception as e:
            logger.warning(f"Could not parse event timestamp: {event_timestamp}, error: {e}")
    
    # Default: replicate the event
    return True
            `),
            logGroup: logGroup,
        });
    }

    /**
     * Create event replication function for cross-region synchronization
     */
    private createEventReplicationFunction(
        environment: string, 
        targetRegions: string[], 
        kmsKey: kms.IKey,
        mskClusterArn?: string
    ): lambda.Function {
        const logGroup = new logs.LogGroup(this, 'EventReplicationFunctionLogGroup', {
            logGroupName: `/aws/lambda/${this.stackName}-event-replication`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
            encryptionKey: kmsKey,
        });

        return new lambda.Function(this, 'EventReplicationFunction', {
            functionName: `${this.stackName}-event-replication`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            role: this.crossRegionRole,
            environment: {
                'ENVIRONMENT': environment,
                'SOURCE_REGION': this.region,
                'TARGET_REGIONS': JSON.stringify(targetRegions),
                'EVENT_BUS_NAME': this.eventBus.eventBusName,
                'MSK_CLUSTER_ARN': mskClusterArn || '',
                'ALERT_TOPIC_ARN': this.alertTopic.topicArn,
                'DLQ_URL': this.deadLetterQueue.queueUrl,
                'LOG_LEVEL': 'INFO',
            },
            code: lambda.Code.fromInline(`
import json
import boto3
import os
import logging
from datetime import datetime, timezone
from typing import Dict, List, Any
import uuid

# Configure logging
logger = logging.getLogger()
logger.setLevel(os.environ.get('LOG_LEVEL', 'INFO'))

# Initialize AWS clients
events_client = boto3.client('events')
sns = boto3.client('sns')
sqs = boto3.client('sqs')

def handler(event, context):
    """
    Event replication function for cross-region synchronization
    Processes events from SQS ordering queue and replicates to target regions
    """
    try:
        logger.info(f"Processing event replication batch: {len(event.get('Records', []))} records")
        
        target_regions = json.loads(os.environ['TARGET_REGIONS'])
        event_bus_name = os.environ['EVENT_BUS_NAME']
        
        successful_replications = 0
        failed_replications = 0
        
        for record in event.get('Records', []):
            try:
                # Parse SQS message
                message_body = json.loads(record['body'])
                event_data = prepare_event_for_replication(message_body)
                
                # Replicate to each target region
                for target_region in target_regions:
                    if target_region != os.environ['SOURCE_REGION']:
                        success = replicate_event_to_region(event_data, target_region, event_bus_name)
                        if success:
                            successful_replications += 1
                        else:
                            failed_replications += 1
                
                logger.info(f"Replicated event {message_body.get('eventId', '')} to {len(target_regions)} regions")
                
            except Exception as record_error:
                logger.error(f"Error processing record: {str(record_error)}")
                failed_replications += 1
                
                # Send failed event to DLQ
                try:
                    sqs.send_message(
                        QueueUrl=os.environ['DLQ_URL'],
                        MessageBody=record['body'],
                        MessageAttributes={
                            'ErrorReason': {
                                'StringValue': str(record_error),
                                'DataType': 'String'
                            },
                            'FailedAt': {
                                'StringValue': datetime.now(timezone.utc).isoformat(),
                                'DataType': 'String'
                            }
                        }
                    )
                except Exception as dlq_error:
                    logger.error(f"Failed to send message to DLQ: {str(dlq_error)}")
        
        # Send summary alert if there were failures
        if failed_replications > 0:
            send_replication_alert(successful_replications, failed_replications)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'successful_replications': successful_replications,
                'failed_replications': failed_replications,
                'timestamp': datetime.now(timezone.utc).isoformat()
            })
        }
        
    except Exception as e:
        logger.error(f"Error in event replication: {str(e)}")
        
        # Send critical alert
        try:
            sns.publish(
                TopicArn=os.environ['ALERT_TOPIC_ARN'],
                Subject='Critical: Cross-Region Event Replication Failure',
                Message=f"Critical error in event replication: {str(e)}\\nContext: {context.aws_request_id}"
            )
        except Exception as alert_error:
            logger.error(f"Failed to send critical alert: {str(alert_error)}")
        
        raise e

def prepare_event_for_replication(message_body: Dict[str, Any]) -> Dict[str, Any]:
    """
    Prepare event data for cross-region replication
    """
    return {
        'Source': message_body.get('source', 'genai-demo.cross-region'),
        'DetailType': message_body.get('detailType', 'Cross-Region Event'),
        'Detail': json.dumps({
            **message_body.get('detail', {}),
            'replicationMetadata': {
                'sourceRegion': os.environ['SOURCE_REGION'],
                'replicationId': str(uuid.uuid4()),
                'replicationTimestamp': datetime.now(timezone.utc).isoformat(),
                'originalEventId': message_body.get('eventId', ''),
                'sequenceNumber': message_body.get('sequenceNumber', 0)
            }
        }),
        'Resources': [],
        'Time': datetime.now(timezone.utc)
    }

def replicate_event_to_region(event_data: Dict[str, Any], target_region: str, event_bus_name: str) -> bool:
    """
    Replicate event to a specific target region
    """
    try:
        # Create region-specific EventBridge client
        regional_events_client = boto3.client('events', region_name=target_region)
        
        # Put event to target region's EventBridge
        response = regional_events_client.put_events(
            Entries=[{
                **event_data,
                'EventBusName': event_bus_name
            }]
        )
        
        # Check for failures
        if response.get('FailedEntryCount', 0) > 0:
            logger.error(f"Failed to replicate event to {target_region}: {response.get('Entries', [])}")
            return False
        
        logger.info(f"Successfully replicated event to {target_region}")
        return True
        
    except Exception as e:
        logger.error(f"Error replicating event to {target_region}: {str(e)}")
        return False

def send_replication_alert(successful: int, failed: int):
    """
    Send alert about replication status
    """
    try:
        message = f"""Cross-Region Event Replication Summary:
        
Successful Replications: {successful}
Failed Replications: {failed}
Source Region: {os.environ['SOURCE_REGION']}
Target Regions: {os.environ['TARGET_REGIONS']}
Timestamp: {datetime.now(timezone.utc).isoformat()}

Please check CloudWatch Logs for detailed error information.
        """
        
        sns.publish(
            TopicArn=os.environ['ALERT_TOPIC_ARN'],
            Subject=f'Cross-Region Replication Alert - {failed} Failures',
            Message=message
        )
    except Exception as e:
        logger.error(f"Failed to send replication alert: {str(e)}")
            `),
            logGroup: logGroup,
        });
    }

    /**
     * Create EventBridge rules for cross-region replication
     */
    private createEventBridgeRules(targetRegions: string[]): void {
        // Rule for filtering events before replication
        const eventFilterRule = new events.Rule(this, 'EventFilterRule', {
            eventBus: this.eventBus,
            ruleName: `${this.stackName}-event-filter-rule`,
            description: 'Filter events for cross-region replication',
            eventPattern: {
                source: [
                    'genai-demo.customer',
                    'genai-demo.order',
                    'genai-demo.payment',
                    'genai-demo.inventory',
                ],
                detailType: [
                    'EntityCreated',
                    'EntityUpdated',
                    'EntityDeleted',
                    'StateChanged',
                ],
            },
        });

        // Add event filter function as target
        eventFilterRule.addTarget(new eventsTargets.LambdaFunction(this.eventFilterFunction, {
            retryAttempts: 3,
            maxEventAge: cdk.Duration.minutes(5),
            deadLetterQueue: this.deadLetterQueue,
        }));

        // Rule for processing events from ordering queue
        const eventReplicationRule = new events.Rule(this, 'EventReplicationRule', {
            ruleName: `${this.stackName}-event-replication-rule`,
            description: 'Process events from ordering queue for replication',
            schedule: events.Schedule.rate(cdk.Duration.minutes(1)), // Process every minute
        });

        // Add SQS as event source for replication function
        this.eventReplicationFunction.addEventSource(
            new lambdaEventSources.SqsEventSource(this.eventOrderingQueue, {
                batchSize: 10,
                // maxBatchingWindow is not supported for FIFO queues
                reportBatchItemFailures: true,
            })
        );
    }

    /**
     * Create monitoring and alerting for cross-region sync
     */
    private createMonitoringAndAlerting(environment: string): void {
        // Create CloudWatch log groups for monitoring
        const syncLogGroup = new logs.LogGroup(this, 'CrossRegionSyncLogGroup', {
            logGroupName: `/aws/cross-region-sync/${this.stackName}`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create custom metrics Lambda for monitoring
        const metricsFunction = new lambda.Function(this, 'CrossRegionSyncMetricsFunction', {
            functionName: `${this.stackName}-sync-metrics`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(2),
            memorySize: 256,
            role: this.crossRegionRole,
            environment: {
                'ENVIRONMENT': environment,
                'REGION': this.region,
                'DLQ_URL': this.deadLetterQueue.queueUrl,
                'ORDERING_QUEUE_URL': this.eventOrderingQueue.queueUrl,
            },
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timezone

cloudwatch = boto3.client('cloudwatch')
sqs = boto3.client('sqs')

def handler(event, context):
    """
    Collect and publish custom metrics for cross-region sync monitoring
    """
    try:
        # Get queue metrics
        dlq_attributes = sqs.get_queue_attributes(
            QueueUrl=os.environ['DLQ_URL'],
            AttributeNames=['ApproximateNumberOfMessages', 'ApproximateNumberOfMessagesNotVisible']
        )
        
        ordering_queue_attributes = sqs.get_queue_attributes(
            QueueUrl=os.environ['ORDERING_QUEUE_URL'],
            AttributeNames=['ApproximateNumberOfMessages', 'ApproximateNumberOfMessagesNotVisible']
        )
        
        # Publish custom metrics
        cloudwatch.put_metric_data(
            Namespace='CrossRegionSync',
            MetricData=[
                {
                    'MetricName': 'DeadLetterQueueMessages',
                    'Value': int(dlq_attributes['Attributes']['ApproximateNumberOfMessages']),
                    'Unit': 'Count',
                    'Dimensions': [
                        {'Name': 'Environment', 'Value': os.environ['ENVIRONMENT']},
                        {'Name': 'Region', 'Value': os.environ['REGION']}
                    ]
                },
                {
                    'MetricName': 'OrderingQueueMessages',
                    'Value': int(ordering_queue_attributes['Attributes']['ApproximateNumberOfMessages']),
                    'Unit': 'Count',
                    'Dimensions': [
                        {'Name': 'Environment', 'Value': os.environ['ENVIRONMENT']},
                        {'Name': 'Region', 'Value': os.environ['REGION']}
                    ]
                },
                {
                    'MetricName': 'SyncHealthCheck',
                    'Value': 1,
                    'Unit': 'Count',
                    'Dimensions': [
                        {'Name': 'Environment', 'Value': os.environ['ENVIRONMENT']},
                        {'Name': 'Region', 'Value': os.environ['REGION']}
                    ]
                }
            ]
        )
        
        return {'statusCode': 200, 'body': 'Metrics published successfully'}
        
    except Exception as e:
        print(f"Error publishing metrics: {str(e)}")
        return {'statusCode': 500, 'body': f'Error: {str(e)}'}
            `),
        });

        // Add CloudWatch permissions to metrics function
        metricsFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: ['cloudwatch:PutMetricData'],
            resources: ['*'],
        }));

        // Schedule metrics collection every 5 minutes
        const metricsRule = new events.Rule(this, 'CrossRegionSyncMetricsRule', {
            schedule: events.Schedule.rate(cdk.Duration.minutes(5)),
            description: 'Collect cross-region sync metrics every 5 minutes',
        });

        metricsRule.addTarget(new eventsTargets.LambdaFunction(metricsFunction));
    }

    /**
     * Create CloudFormation outputs
     */
    private createOutputs(environment: string): void {
        new cdk.CfnOutput(this, 'EventBusArn', {
            value: this.eventBus.eventBusArn,
            description: 'Cross-Region Event Bus ARN',
            exportName: `${this.stackName}-EventBusArn`,
        });

        new cdk.CfnOutput(this, 'EventBusName', {
            value: this.eventBus.eventBusName,
            description: 'Cross-Region Event Bus Name',
            exportName: `${this.stackName}-EventBusName`,
        });

        new cdk.CfnOutput(this, 'EventReplicationFunctionArn', {
            value: this.eventReplicationFunction.functionArn,
            description: 'Event Replication Function ARN',
            exportName: `${this.stackName}-EventReplicationFunctionArn`,
        });

        new cdk.CfnOutput(this, 'EventFilterFunctionArn', {
            value: this.eventFilterFunction.functionArn,
            description: 'Event Filter Function ARN',
            exportName: `${this.stackName}-EventFilterFunctionArn`,
        });

        new cdk.CfnOutput(this, 'CrossRegionRoleArn', {
            value: this.crossRegionRole.roleArn,
            description: 'Cross-Region Sync Role ARN',
            exportName: `${this.stackName}-CrossRegionRoleArn`,
        });

        new cdk.CfnOutput(this, 'AlertTopicArn', {
            value: this.alertTopic.topicArn,
            description: 'Cross-Region Sync Alert Topic ARN',
            exportName: `${this.stackName}-AlertTopicArn`,
        });

        new cdk.CfnOutput(this, 'DeadLetterQueueUrl', {
            value: this.deadLetterQueue.queueUrl,
            description: 'Dead Letter Queue URL',
            exportName: `${this.stackName}-DeadLetterQueueUrl`,
        });

        new cdk.CfnOutput(this, 'EventOrderingQueueUrl', {
            value: this.eventOrderingQueue.queueUrl,
            description: 'Event Ordering Queue URL',
            exportName: `${this.stackName}-EventOrderingQueueUrl`,
        });
    }
}