import * as cdk from 'aws-cdk-lib';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';

export interface DataRetentionStackProps extends cdk.StackProps {
    environment: string;
    kmsKey: kms.Key;
}

/**
 * Data Retention Stack implementing automated data lifecycle management
 * Implements requirement 11.4: WHEN data is stored THEN the system SHALL implement proper retention policies
 */
export class DataRetentionStack extends cdk.Stack {
    public readonly logRetentionFunction: lambda.Function;
    public readonly dataArchiveBucket: s3.Bucket;

    constructor(scope: Construct, id: string, props: DataRetentionStackProps) {
        super(scope, id, props);

        // S3 bucket for data archival with lifecycle policies
        this.dataArchiveBucket = this.createDataArchiveBucket(props.environment, props.kmsKey);

        // Lambda function for log retention management
        this.logRetentionFunction = this.createLogRetentionFunction(props.environment, props.kmsKey);

        // CloudWatch Log Groups with retention policies
        this.createLogGroups(props.environment, props.kmsKey);

        // EventBridge rules for automated cleanup
        this.createCleanupSchedules(props.environment);

        // Outputs
        this.createOutputs();
    }

    private createDataArchiveBucket(environment: string, kmsKey: kms.Key): s3.Bucket {
        const bucket = new s3.Bucket(this, 'DataArchiveBucket', {
            bucketName: `genai-demo-data-archive-${environment}-${this.account}`,
            encryption: s3.BucketEncryption.KMS,
            encryptionKey: kmsKey,
            blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
            versioned: true,
            lifecycleRules: [
                {
                    id: 'ApplicationLogsLifecycle',
                    enabled: true,
                    prefix: 'application-logs/',
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(30),
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(90),
                        },
                        {
                            storageClass: s3.StorageClass.DEEP_ARCHIVE,
                            transitionAfter: cdk.Duration.days(365),
                        },
                    ],
                    expiration: cdk.Duration.days(2555), // 7 years
                },
                {
                    id: 'SecurityLogsLifecycle',
                    enabled: true,
                    prefix: 'security-logs/',
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(90),
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(365),
                        },
                        {
                            storageClass: s3.StorageClass.DEEP_ARCHIVE,
                            transitionAfter: cdk.Duration.days(1095), // 3 years
                        },
                    ],
                    expiration: cdk.Duration.days(2555), // 7 years
                },
                {
                    id: 'AuditLogsLifecycle',
                    enabled: true,
                    prefix: 'audit-logs/',
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(90),
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(365),
                        },
                        {
                            storageClass: s3.StorageClass.DEEP_ARCHIVE,
                            transitionAfter: cdk.Duration.days(1095), // 3 years
                        },
                    ],
                    expiration: cdk.Duration.days(3650), // 10 years
                },
                {
                    id: 'MetricsLifecycle',
                    enabled: true,
                    prefix: 'metrics/',
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(15),
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(90),
                        },
                    ],
                    expiration: cdk.Duration.days(365), // 1 year
                },
                {
                    id: 'TracesLifecycle',
                    enabled: true,
                    prefix: 'traces/',
                    transitions: [
                        {
                            storageClass: s3.StorageClass.INFREQUENT_ACCESS,
                            transitionAfter: cdk.Duration.days(7),
                        },
                        {
                            storageClass: s3.StorageClass.GLACIER,
                            transitionAfter: cdk.Duration.days(30),
                        },
                    ],
                    expiration: cdk.Duration.days(90), // 3 months
                },
                {
                    id: 'TempDataLifecycle',
                    enabled: true,
                    prefix: 'temp-data/',
                    expiration: cdk.Duration.days(7), // 1 week
                },
            ],
        });

        cdk.Tags.of(bucket).add('Environment', environment);
        cdk.Tags.of(bucket).add('Purpose', 'DataArchival');

        return bucket;
    }

    private createLogRetentionFunction(environment: string, kmsKey: kms.Key): lambda.Function {
        const func = new lambda.Function(this, 'LogRetentionFunction', {
            functionName: `genai-demo-log-retention-${environment}`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(15),
            memorySize: 512,
            environment: {
                ENVIRONMENT: environment,
                ARCHIVE_BUCKET: this.dataArchiveBucket.bucketName,
                KMS_KEY_ID: kmsKey.keyId,
            },
            code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime, timedelta
from typing import Dict, List

def handler(event, context):
    """
    Lambda function to manage log retention and archival
    """
    environment = os.environ['ENVIRONMENT']
    archive_bucket = os.environ['ARCHIVE_BUCKET']
    kms_key_id = os.environ['KMS_KEY_ID']
    
    logs_client = boto3.client('logs')
    s3_client = boto3.client('s3')
    
    # Define retention policies
    retention_policies = {
        'application-logs': {
            'hot_days': 30,
            'warm_days': 90,
            'cold_days': 2555,  # 7 years
            'log_groups': [
                f'/aws/eks/genai-demo-{environment}',
                f'/aws/application/genai-demo-{environment}'
            ]
        },
        'security-logs': {
            'hot_days': 90,
            'warm_days': 365,
            'cold_days': 2555,  # 7 years
            'log_groups': [
                f'/aws/security/genai-demo-{environment}'
            ]
        },
        'audit-logs': {
            'hot_days': 90,
            'warm_days': 365,
            'cold_days': 3650,  # 10 years
            'log_groups': [
                f'/aws/cloudtrail/genai-demo-{environment}'
            ]
        }
    }
    
    results = []
    
    for data_type, policy in retention_policies.items():
        try:
            result = process_log_retention(
                logs_client, s3_client, data_type, policy, 
                archive_bucket, kms_key_id
            )
            results.append(result)
        except Exception as e:
            print(f"Error processing {data_type}: {str(e)}")
            results.append({
                'data_type': data_type,
                'status': 'error',
                'message': str(e)
            })
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'message': 'Log retention processing completed',
            'results': results
        })
    }

def process_log_retention(logs_client, s3_client, data_type: str, 
                         policy: Dict, archive_bucket: str, kms_key_id: str) -> Dict:
    """
    Process log retention for a specific data type
    """
    hot_cutoff = datetime.now() - timedelta(days=policy['hot_days'])
    warm_cutoff = datetime.now() - timedelta(days=policy['warm_days'])
    cold_cutoff = datetime.now() - timedelta(days=policy['cold_days'])
    
    processed_groups = 0
    archived_streams = 0
    deleted_streams = 0
    
    for log_group in policy['log_groups']:
        try:
            # Check if log group exists
            logs_client.describe_log_groups(logGroupNamePrefix=log_group)
            
            # Get log streams
            paginator = logs_client.get_paginator('describe_log_streams')
            
            for page in paginator.paginate(logGroupName=log_group):
                for stream in page['logStreams']:
                    stream_name = stream['logStreamName']
                    last_event_time = datetime.fromtimestamp(
                        stream.get('lastEventTime', 0) / 1000
                    )
                    
                    # Archive old streams to S3
                    if last_event_time < warm_cutoff:
                        archive_log_stream(
                            logs_client, s3_client, log_group, stream_name,
                            archive_bucket, data_type, kms_key_id
                        )
                        archived_streams += 1
                    
                    # Delete very old streams
                    if last_event_time < cold_cutoff:
                        try:
                            logs_client.delete_log_stream(
                                logGroupName=log_group,
                                logStreamName=stream_name
                            )
                            deleted_streams += 1
                        except Exception as e:
                            print(f"Error deleting stream {stream_name}: {str(e)}")
            
            processed_groups += 1
            
        except logs_client.exceptions.ResourceNotFoundException:
            print(f"Log group {log_group} not found")
        except Exception as e:
            print(f"Error processing log group {log_group}: {str(e)}")
    
    return {
        'data_type': data_type,
        'status': 'success',
        'processed_groups': processed_groups,
        'archived_streams': archived_streams,
        'deleted_streams': deleted_streams
    }

def archive_log_stream(logs_client, s3_client, log_group: str, stream_name: str,
                      archive_bucket: str, data_type: str, kms_key_id: str):
    """
    Archive a log stream to S3
    """
    try:
        # Export log stream to S3
        export_task = logs_client.create_export_task(
            logGroupName=log_group,
            logStreamNamePrefix=stream_name,
            fromTime=0,
            to=int(datetime.now().timestamp() * 1000),
            destination=archive_bucket,
            destinationPrefix=f"{data_type}/{log_group.replace('/', '_')}/{stream_name}"
        )
        
        print(f"Started export task {export_task['taskId']} for {stream_name}")
        
    except Exception as e:
        print(f"Error archiving stream {stream_name}: {str(e)}")
      `),
        });

        // Grant permissions to the function
        func.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:DescribeLogGroups',
                'logs:DescribeLogStreams',
                'logs:DeleteLogStream',
                'logs:CreateExportTask',
                'logs:DescribeExportTasks',
            ],
            resources: ['*'],
        }));

        this.dataArchiveBucket.grantReadWrite(func);
        kmsKey.grantEncryptDecrypt(func);

        cdk.Tags.of(func).add('Environment', environment);
        cdk.Tags.of(func).add('Purpose', 'DataRetention');

        return func;
    }

    private createLogGroups(environment: string, kmsKey: kms.Key): void {
        // Application log group
        new logs.LogGroup(this, 'ApplicationLogGroup', {
            logGroupName: `/aws/eks/genai-demo-${environment}`,
            retention: logs.RetentionDays.ONE_MONTH,
            encryptionKey: kmsKey,
        });

        // Security log group
        new logs.LogGroup(this, 'SecurityLogGroup', {
            logGroupName: `/aws/security/genai-demo-${environment}`,
            retention: logs.RetentionDays.THREE_MONTHS,
            encryptionKey: kmsKey,
        });

        // Audit log group (handled by CloudTrail)
        new logs.LogGroup(this, 'AuditLogGroup', {
            logGroupName: `/aws/cloudtrail/genai-demo-${environment}`,
            retention: logs.RetentionDays.ONE_YEAR,
            encryptionKey: kmsKey,
        });
    }

    private createCleanupSchedules(environment: string): void {
        // Daily cleanup schedule
        const dailyCleanupRule = new events.Rule(this, 'DailyCleanupRule', {
            ruleName: `genai-demo-daily-cleanup-${environment}`,
            description: 'Daily data retention cleanup',
            schedule: events.Schedule.cron({
                minute: '0',
                hour: '2', // 2 AM UTC
                day: '*',
                month: '*',
                year: '*',
            }),
        });

        dailyCleanupRule.addTarget(new targets.LambdaFunction(this.logRetentionFunction));

        // Weekly deep cleanup schedule
        const weeklyCleanupRule = new events.Rule(this, 'WeeklyCleanupRule', {
            ruleName: `genai-demo-weekly-cleanup-${environment}`,
            description: 'Weekly deep data retention cleanup',
            schedule: events.Schedule.cron({
                minute: '0',
                hour: '3', // 3 AM UTC
                day: '*',
                month: '*',
                year: '*',
                weekDay: 'SUN', // Sunday
            }),
        });

        weeklyCleanupRule.addTarget(new targets.LambdaFunction(this.logRetentionFunction, {
            event: events.RuleTargetInput.fromObject({
                cleanup_type: 'deep',
                force_cleanup: true,
            }),
        }));

        cdk.Tags.of(dailyCleanupRule).add('Environment', environment);
        cdk.Tags.of(weeklyCleanupRule).add('Environment', environment);
    }

    private createOutputs(): void {
        new cdk.CfnOutput(this, 'DataArchiveBucketName', {
            value: this.dataArchiveBucket.bucketName,
            description: 'Name of the data archive S3 bucket',
            exportName: 'genai-demo-data-archive-bucket',
        });

        new cdk.CfnOutput(this, 'LogRetentionFunctionArn', {
            value: this.logRetentionFunction.functionArn,
            description: 'ARN of the log retention Lambda function',
            exportName: 'genai-demo-log-retention-function-arn',
        });
    }
}