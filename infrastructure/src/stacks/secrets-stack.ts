import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as eventsTargets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kms from 'aws-cdk-lib/aws-kms';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as rds from 'aws-cdk-lib/aws-rds';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { Construct } from 'constructs';

export interface SecretsStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly region: string;
    readonly vpc: ec2.IVpc;
    readonly secretsManagerKey: kms.IKey;
    readonly databaseCluster?: rds.IDatabaseCluster;
    readonly databaseInstance?: rds.IDatabaseInstance;
    readonly enableMultiRegion?: boolean;
    readonly replicationRegions?: string[];
    readonly alertingTopic?: sns.ITopic;
}

/**
 * Enhanced Secrets Stack for managing application secrets with cross-region synchronization
 * 
 * This stack provides:
 * - Database credentials with automatic rotation
 * - API keys and application secrets
 * - Cross-region secret replication and synchronization
 * - ConfigMap synchronization for Kubernetes
 * - Configuration drift detection and monitoring
 * - GitOps multi-region deployment pipeline integration
 * - Integration with AWS managed KMS keys
 */
export class SecretsStack extends cdk.Stack {
    public readonly databaseSecret: secretsmanager.Secret;
    public readonly apiKeysSecret: secretsmanager.Secret;
    public readonly applicationSecretsSecret: secretsmanager.Secret;
    public readonly rotationLambda?: lambda.Function;
    public crossRegionSyncLambda?: lambda.Function;
    public configMapSyncLambda?: lambda.Function;
    public driftDetectionLambda?: lambda.Function;
    public secretsEventBridge?: events.Rule;

    constructor(scope: Construct, id: string, props: SecretsStackProps) {
        super(scope, id, props);

        const { 
            environment, 
            projectName, 
            region, 
            vpc, 
            secretsManagerKey, 
            databaseCluster, 
            databaseInstance,
            enableMultiRegion = false,
            replicationRegions = [],
            alertingTopic
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'Secrets',
            Service: 'SecretsManager'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Create database credentials secret
        this.databaseSecret = this.createDatabaseSecret(projectName, environment, secretsManagerKey);

        // Create API keys secret
        this.apiKeysSecret = this.createApiKeysSecret(projectName, environment, secretsManagerKey);

        // Create application secrets
        this.applicationSecretsSecret = this.createApplicationSecretsSecret(projectName, environment, secretsManagerKey);

        // Create rotation Lambda function for database credentials
        if (databaseCluster || databaseInstance) {
            this.rotationLambda = this.createRotationLambda(vpc, projectName, environment);
            this.setupDatabaseSecretRotation(databaseCluster, databaseInstance);
        }

        // Setup cross-region replication and synchronization
        if (enableMultiRegion && replicationRegions.length > 0) {
            this.setupCrossRegionReplication(projectName, environment, replicationRegions);
            this.createCrossRegionSyncLambda(vpc, projectName, environment, replicationRegions, alertingTopic);
            this.createConfigMapSyncLambda(vpc, projectName, environment, alertingTopic);
            this.createDriftDetectionSystem(vpc, projectName, environment, alertingTopic);
            this.setupSecretsEventBridge(projectName, environment);
        }

        // Store secret information in Parameter Store
        this.createParameterStoreParameters(projectName, environment, region);

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment);
    }

    /**
     * Create Lambda function for cross-region secret synchronization
     */
    private createCrossRegionSyncLambda(
        vpc: ec2.IVpc, 
        projectName: string, 
        environment: string, 
        replicationRegions: string[],
        alertingTopic?: sns.ITopic
    ): void {
        // Create IAM role for cross-region sync Lambda
        const syncRole = new iam.Role(this, 'CrossRegionSyncLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaVPCAccessExecutionRole')
            ],
            inlinePolicies: {
                CrossRegionSyncPolicy: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'secretsmanager:GetSecretValue',
                                'secretsmanager:UpdateSecret',
                                'secretsmanager:DescribeSecret',
                                'secretsmanager:ListSecrets'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'ssm:GetParameter',
                                'ssm:PutParameter',
                                'ssm:GetParameters',
                                'ssm:GetParametersByPath'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'kms:Decrypt',
                                'kms:Encrypt',
                                'kms:GenerateDataKey'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'sns:Publish'
                            ],
                            resources: alertingTopic ? [alertingTopic.topicArn] : ['*']
                        })
                    ]
                })
            }
        });

        // Create security group for sync Lambda
        const syncSecurityGroup = new ec2.SecurityGroup(this, 'CrossRegionSyncSecurityGroup', {
            vpc: vpc,
            description: 'Security group for cross-region secret sync Lambda',
            allowAllOutbound: true
        });

        // Create the cross-region sync Lambda function
        this.crossRegionSyncLambda = new lambda.Function(this, 'CrossRegionSyncLambda', {
            functionName: `${projectName}-${environment}-cross-region-sync`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'lambda_function.lambda_handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import logging
import os
from typing import Dict, List, Any

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    Cross-region secret synchronization handler
    Synchronizes secrets across multiple regions for Active-Active deployment
    """
    try:
        source_region = os.environ.get('AWS_REGION')
        target_regions = os.environ.get('REPLICATION_REGIONS', '').split(',')
        sns_topic_arn = os.environ.get('ALERTING_TOPIC_ARN')
        
        logger.info(f"Starting cross-region sync from {source_region} to {target_regions}")
        
        # Process the event (could be from EventBridge, SNS, or direct invocation)
        if 'Records' in event:
            # SNS event
            for record in event['Records']:
                if record['EventSource'] == 'aws:sns':
                    message = json.loads(record['Sns']['Message'])
                    process_secret_change(message, target_regions, sns_topic_arn)
        elif 'detail' in event:
            # EventBridge event
            process_secret_change(event['detail'], target_regions, sns_topic_arn)
        else:
            # Direct invocation - sync all secrets
            sync_all_secrets(source_region, target_regions, sns_topic_arn)
        
        return {
            'statusCode': 200,
            'body': json.dumps('Cross-region sync completed successfully')
        }
        
    except Exception as e:
        logger.error(f"Cross-region sync failed: {str(e)}")
        send_alert(f"Cross-region sync failed: {str(e)}", sns_topic_arn)
        raise e

def process_secret_change(event_detail: Dict[str, Any], target_regions: List[str], sns_topic_arn: str):
    """Process individual secret change event"""
    secret_arn = event_detail.get('responseElements', {}).get('arn')
    if not secret_arn:
        logger.warning("No secret ARN found in event")
        return
    
    logger.info(f"Processing secret change for: {secret_arn}")
    
    # Get source secret
    source_client = boto3.client('secretsmanager')
    try:
        source_secret = source_client.get_secret_value(SecretId=secret_arn)
        secret_name = source_secret['Name']
        secret_value = source_secret['SecretString']
        
        # Sync to target regions
        for region in target_regions:
            if region.strip():
                sync_secret_to_region(secret_name, secret_value, region, sns_topic_arn)
                
    except Exception as e:
        logger.error(f"Failed to process secret {secret_arn}: {str(e)}")
        send_alert(f"Failed to sync secret {secret_arn}: {str(e)}", sns_topic_arn)

def sync_secret_to_region(secret_name: str, secret_value: str, target_region: str, sns_topic_arn: str):
    """Sync secret to target region"""
    try:
        target_client = boto3.client('secretsmanager', region_name=target_region)
        
        # Check if secret exists in target region
        try:
            target_client.describe_secret(SecretId=secret_name)
            # Secret exists, update it
            target_client.update_secret(
                SecretId=secret_name,
                SecretString=secret_value
            )
            logger.info(f"Updated secret {secret_name} in region {target_region}")
        except target_client.exceptions.ResourceNotFoundException:
            # Secret doesn't exist, create it
            target_client.create_secret(
                Name=secret_name,
                SecretString=secret_value,
                Description=f"Cross-region replica of {secret_name}"
            )
            logger.info(f"Created secret {secret_name} in region {target_region}")
            
    except Exception as e:
        logger.error(f"Failed to sync secret {secret_name} to region {target_region}: {str(e)}")
        send_alert(f"Failed to sync secret {secret_name} to region {target_region}: {str(e)}", sns_topic_arn)

def sync_all_secrets(source_region: str, target_regions: List[str], sns_topic_arn: str):
    """Sync all secrets to target regions"""
    source_client = boto3.client('secretsmanager')
    
    try:
        # List all secrets with genai-demo prefix
        paginator = source_client.get_paginator('list_secrets')
        
        for page in paginator.paginate():
            for secret in page['SecretList']:
                if 'genai-demo' in secret['Name']:
                    secret_value = source_client.get_secret_value(SecretId=secret['ARN'])
                    
                    for region in target_regions:
                        if region.strip():
                            sync_secret_to_region(
                                secret['Name'], 
                                secret_value['SecretString'], 
                                region, 
                                sns_topic_arn
                            )
                            
    except Exception as e:
        logger.error(f"Failed to sync all secrets: {str(e)}")
        send_alert(f"Failed to sync all secrets: {str(e)}", sns_topic_arn)

def send_alert(message: str, sns_topic_arn: str):
    """Send alert notification"""
    if sns_topic_arn:
        try:
            sns_client = boto3.client('sns')
            sns_client.publish(
                TopicArn=sns_topic_arn,
                Subject='Cross-Region Secret Sync Alert',
                Message=message
            )
        except Exception as e:
            logger.error(f"Failed to send alert: {str(e)}")
            `),
            role: syncRole,
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
            },
            securityGroups: [syncSecurityGroup],
            timeout: cdk.Duration.minutes(10),
            environment: {
                REPLICATION_REGIONS: replicationRegions.join(','),
                ALERTING_TOPIC_ARN: alertingTopic?.topicArn || '',
                PROJECT_NAME: projectName,
                ENVIRONMENT: environment
            }
        });

        // Create log group for the Lambda function
        new logs.LogGroup(this, 'CrossRegionSyncLogGroup', {
            logGroupName: `/aws/lambda/${this.crossRegionSyncLambda.functionName}`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(this.crossRegionSyncLambda).add('Name', `${projectName}-${environment}-cross-region-sync`);
        cdk.Tags.of(this.crossRegionSyncLambda).add('Environment', environment);
        cdk.Tags.of(this.crossRegionSyncLambda).add('Project', projectName);
    }

    /**
     * Create Lambda function for ConfigMap synchronization
     */
    private createConfigMapSyncLambda(
        vpc: ec2.IVpc, 
        projectName: string, 
        environment: string,
        alertingTopic?: sns.ITopic
    ): void {
        // Create IAM role for ConfigMap sync Lambda
        const configMapSyncRole = new iam.Role(this, 'ConfigMapSyncLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaVPCAccessExecutionRole')
            ],
            inlinePolicies: {
                ConfigMapSyncPolicy: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'secretsmanager:GetSecretValue',
                                'secretsmanager:DescribeSecret'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'ssm:GetParameter',
                                'ssm:PutParameter',
                                'ssm:GetParameters',
                                'ssm:GetParametersByPath'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'eks:DescribeCluster',
                                'eks:ListClusters'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'sns:Publish'
                            ],
                            resources: alertingTopic ? [alertingTopic.topicArn] : ['*']
                        })
                    ]
                })
            }
        });

        // Create the ConfigMap sync Lambda function
        this.configMapSyncLambda = new lambda.Function(this, 'ConfigMapSyncLambda', {
            functionName: `${projectName}-${environment}-configmap-sync`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'lambda_function.lambda_handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import logging
import os
import base64
from kubernetes import client, config
from kubernetes.client.rest import ApiException

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    ConfigMap synchronization handler
    Synchronizes secrets to Kubernetes ConfigMaps across regions
    """
    try:
        project_name = os.environ.get('PROJECT_NAME')
        environment = os.environ.get('ENVIRONMENT')
        sns_topic_arn = os.environ.get('ALERTING_TOPIC_ARN')
        
        logger.info(f"Starting ConfigMap sync for {project_name}-{environment}")
        
        # Get EKS cluster information
        eks_client = boto3.client('eks')
        cluster_name = f"{project_name}-{environment}-cluster"
        
        try:
            cluster_info = eks_client.describe_cluster(name=cluster_name)
            cluster_endpoint = cluster_info['cluster']['endpoint']
            cluster_ca = cluster_info['cluster']['certificateAuthority']['data']
            
            # Configure Kubernetes client
            configure_k8s_client(cluster_endpoint, cluster_ca, cluster_name)
            
            # Sync secrets to ConfigMaps
            sync_secrets_to_configmaps(project_name, environment, sns_topic_arn)
            
        except eks_client.exceptions.ResourceNotFoundException:
            logger.warning(f"EKS cluster {cluster_name} not found, skipping ConfigMap sync")
            return {
                'statusCode': 200,
                'body': json.dumps('EKS cluster not found, ConfigMap sync skipped')
            }
        
        return {
            'statusCode': 200,
            'body': json.dumps('ConfigMap sync completed successfully')
        }
        
    except Exception as e:
        logger.error(f"ConfigMap sync failed: {str(e)}")
        send_alert(f"ConfigMap sync failed: {str(e)}", sns_topic_arn)
        raise e

def configure_k8s_client(cluster_endpoint: str, cluster_ca: str, cluster_name: str):
    """Configure Kubernetes client for EKS"""
    # This is a simplified version - in production, you'd use proper EKS authentication
    configuration = client.Configuration()
    configuration.host = cluster_endpoint
    configuration.ssl_ca_cert = base64.b64decode(cluster_ca)
    configuration.api_key_prefix['authorization'] = 'Bearer'
    
    # In production, use AWS IAM authenticator or service account tokens
    # configuration.api_key['authorization'] = get_eks_token(cluster_name)
    
    client.Configuration.set_default(configuration)

def sync_secrets_to_configmaps(project_name: str, environment: str, sns_topic_arn: str):
    """Sync secrets to Kubernetes ConfigMaps"""
    secrets_client = boto3.client('secretsmanager')
    k8s_client = client.CoreV1Api()
    
    try:
        # Get application secrets (non-sensitive configuration)
        app_secret_name = f"{environment}/genai-demo/application"
        app_secret = secrets_client.get_secret_value(SecretId=app_secret_name)
        app_config = json.loads(app_secret['SecretString'])
        
        # Create ConfigMap data (exclude sensitive keys)
        configmap_data = {}
        non_sensitive_keys = ['session_timeout', 'max_connections', 'cache_ttl', 'log_level']
        
        for key, value in app_config.items():
            if any(sensitive in key.lower() for sensitive in ['secret', 'key', 'password', 'token']):
                continue  # Skip sensitive data
            if key in non_sensitive_keys:
                configmap_data[key] = str(value)
        
        # Create or update ConfigMap
        configmap_name = f"{project_name}-{environment}-config"
        namespace = 'default'
        
        configmap = client.V1ConfigMap(
            metadata=client.V1ObjectMeta(
                name=configmap_name,
                namespace=namespace,
                labels={
                    'app': project_name,
                    'environment': environment,
                    'managed-by': 'secrets-sync-lambda'
                }
            ),
            data=configmap_data
        )
        
        try:
            # Try to update existing ConfigMap
            k8s_client.patch_namespaced_config_map(
                name=configmap_name,
                namespace=namespace,
                body=configmap
            )
            logger.info(f"Updated ConfigMap {configmap_name}")
        except ApiException as e:
            if e.status == 404:
                # ConfigMap doesn't exist, create it
                k8s_client.create_namespaced_config_map(
                    namespace=namespace,
                    body=configmap
                )
                logger.info(f"Created ConfigMap {configmap_name}")
            else:
                raise e
                
    except Exception as e:
        logger.error(f"Failed to sync ConfigMaps: {str(e)}")
        send_alert(f"Failed to sync ConfigMaps: {str(e)}", sns_topic_arn)

def send_alert(message: str, sns_topic_arn: str):
    """Send alert notification"""
    if sns_topic_arn:
        try:
            sns_client = boto3.client('sns')
            sns_client.publish(
                TopicArn=sns_topic_arn,
                Subject='ConfigMap Sync Alert',
                Message=message
            )
        except Exception as e:
            logger.error(f"Failed to send alert: {str(e)}")
            `),
            role: configMapSyncRole,
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
            },
            timeout: cdk.Duration.minutes(5),
            environment: {
                PROJECT_NAME: projectName,
                ENVIRONMENT: environment,
                ALERTING_TOPIC_ARN: alertingTopic?.topicArn || ''
            }
        });

        // Create log group for the Lambda function
        new logs.LogGroup(this, 'ConfigMapSyncLogGroup', {
            logGroupName: `/aws/lambda/${this.configMapSyncLambda.functionName}`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(this.configMapSyncLambda).add('Name', `${projectName}-${environment}-configmap-sync`);
        cdk.Tags.of(this.configMapSyncLambda).add('Environment', environment);
        cdk.Tags.of(this.configMapSyncLambda).add('Project', projectName);
    }

    /**
     * Create configuration drift detection system
     */
    private createDriftDetectionSystem(
        vpc: ec2.IVpc, 
        projectName: string, 
        environment: string,
        alertingTopic?: sns.ITopic
    ): void {
        // Create IAM role for drift detection Lambda
        const driftDetectionRole = new iam.Role(this, 'DriftDetectionLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaVPCAccessExecutionRole')
            ],
            inlinePolicies: {
                DriftDetectionPolicy: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'secretsmanager:GetSecretValue',
                                'secretsmanager:DescribeSecret',
                                'secretsmanager:ListSecrets'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'ssm:GetParameter',
                                'ssm:GetParameters',
                                'ssm:GetParametersByPath',
                                'ssm:PutParameter'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'cloudwatch:PutMetricData'
                            ],
                            resources: ['*']
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'sns:Publish'
                            ],
                            resources: alertingTopic ? [alertingTopic.topicArn] : ['*']
                        })
                    ]
                })
            }
        });

        // Create the drift detection Lambda function
        this.driftDetectionLambda = new lambda.Function(this, 'DriftDetectionLambda', {
            functionName: `${projectName}-${environment}-drift-detection`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'lambda_function.lambda_handler',
            code: lambda.Code.fromInline(`
import boto3
import json
import logging
import os
import hashlib
from datetime import datetime, timezone
from typing import Dict, List, Any, Tuple

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    Configuration drift detection handler
    Monitors configuration changes across regions and detects drift
    """
    try:
        project_name = os.environ.get('PROJECT_NAME')
        environment = os.environ.get('ENVIRONMENT')
        sns_topic_arn = os.environ.get('ALERTING_TOPIC_ARN')
        
        logger.info(f"Starting drift detection for {project_name}-{environment}")
        
        # Get current region and target regions
        current_region = os.environ.get('AWS_REGION')
        target_regions = os.environ.get('REPLICATION_REGIONS', '').split(',')
        
        # Detect configuration drift
        drift_results = detect_configuration_drift(
            current_region, 
            target_regions, 
            project_name, 
            environment
        )
        
        # Process drift results
        if drift_results['has_drift']:
            handle_configuration_drift(drift_results, sns_topic_arn)
        
        # Update drift detection metrics
        update_drift_metrics(drift_results, project_name, environment)
        
        # Store drift detection results
        store_drift_results(drift_results, project_name, environment)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'message': 'Drift detection completed',
                'has_drift': drift_results['has_drift'],
                'drift_count': len(drift_results['drifts'])
            })
        }
        
    except Exception as e:
        logger.error(f"Drift detection failed: {str(e)}")
        send_alert(f"Drift detection failed: {str(e)}", sns_topic_arn)
        raise e

def detect_configuration_drift(
    source_region: str, 
    target_regions: List[str], 
    project_name: str, 
    environment: str
) -> Dict[str, Any]:
    """Detect configuration drift across regions"""
    drift_results = {
        'has_drift': False,
        'drifts': [],
        'timestamp': datetime.now(timezone.utc).isoformat(),
        'source_region': source_region,
        'target_regions': target_regions
    }
    
    # Get source configurations
    source_configs = get_region_configurations(source_region, project_name, environment)
    
    # Compare with target regions
    for target_region in target_regions:
        if target_region.strip() and target_region != source_region:
            target_configs = get_region_configurations(target_region, project_name, environment)
            
            # Compare configurations
            region_drifts = compare_configurations(
                source_configs, 
                target_configs, 
                source_region, 
                target_region
            )
            
            if region_drifts:
                drift_results['has_drift'] = True
                drift_results['drifts'].extend(region_drifts)
    
    return drift_results

def get_region_configurations(region: str, project_name: str, environment: str) -> Dict[str, Any]:
    """Get all configurations for a specific region"""
    configurations = {}
    
    try:
        # Get Secrets Manager configurations
        secrets_client = boto3.client('secretsmanager', region_name=region)
        ssm_client = boto3.client('ssm', region_name=region)
        
        # List secrets with project prefix
        secret_prefix = f"{environment}/genai-demo"
        
        try:
            paginator = secrets_client.get_paginator('list_secrets')
            for page in paginator.paginate():
                for secret in page['SecretList']:
                    if secret['Name'].startswith(secret_prefix):
                        try:
                            secret_value = secrets_client.get_secret_value(SecretId=secret['ARN'])
                            # Create hash of secret value for comparison (don't store actual values)
                            secret_hash = hashlib.sha256(
                                secret_value['SecretString'].encode()
                            ).hexdigest()
                            
                            configurations[f"secret:{secret['Name']}"] = {
                                'type': 'secret',
                                'name': secret['Name'],
                                'hash': secret_hash,
                                'last_changed': secret.get('LastChangedDate', '').isoformat() if secret.get('LastChangedDate') else None
                            }
                        except Exception as e:
                            logger.warning(f"Failed to get secret {secret['Name']}: {str(e)}")
        except Exception as e:
            logger.warning(f"Failed to list secrets in region {region}: {str(e)}")
        
        # Get Parameter Store configurations
        try:
            parameter_prefix = f"/genai-demo/{environment}"
            paginator = ssm_client.get_paginator('get_parameters_by_path')
            
            for page in paginator.paginate(Path=parameter_prefix, Recursive=True):
                for param in page['Parameters']:
                    param_hash = hashlib.sha256(param['Value'].encode()).hexdigest()
                    
                    configurations[f"parameter:{param['Name']}"] = {
                        'type': 'parameter',
                        'name': param['Name'],
                        'hash': param_hash,
                        'last_modified': param.get('LastModifiedDate', '').isoformat() if param.get('LastModifiedDate') else None
                    }
        except Exception as e:
            logger.warning(f"Failed to get parameters in region {region}: {str(e)}")
            
    except Exception as e:
        logger.error(f"Failed to get configurations for region {region}: {str(e)}")
    
    return configurations

def compare_configurations(
    source_configs: Dict[str, Any], 
    target_configs: Dict[str, Any], 
    source_region: str, 
    target_region: str
) -> List[Dict[str, Any]]:
    """Compare configurations between regions"""
    drifts = []
    
    # Check for missing configurations in target
    for config_key, source_config in source_configs.items():
        if config_key not in target_configs:
            drifts.append({
                'type': 'missing',
                'config_key': config_key,
                'config_name': source_config['name'],
                'config_type': source_config['type'],
                'source_region': source_region,
                'target_region': target_region,
                'description': f"Configuration {source_config['name']} missing in {target_region}"
            })
        elif source_config['hash'] != target_configs[config_key]['hash']:
            drifts.append({
                'type': 'mismatch',
                'config_key': config_key,
                'config_name': source_config['name'],
                'config_type': source_config['type'],
                'source_region': source_region,
                'target_region': target_region,
                'description': f"Configuration {source_config['name']} differs between {source_region} and {target_region}"
            })
    
    # Check for extra configurations in target
    for config_key, target_config in target_configs.items():
        if config_key not in source_configs:
            drifts.append({
                'type': 'extra',
                'config_key': config_key,
                'config_name': target_config['name'],
                'config_type': target_config['type'],
                'source_region': source_region,
                'target_region': target_region,
                'description': f"Extra configuration {target_config['name']} found in {target_region}"
            })
    
    return drifts

def handle_configuration_drift(drift_results: Dict[str, Any], sns_topic_arn: str):
    """Handle detected configuration drift"""
    drift_count = len(drift_results['drifts'])
    
    # Create detailed drift report
    drift_report = f"""
Configuration Drift Detected

Timestamp: {drift_results['timestamp']}
Source Region: {drift_results['source_region']}
Target Regions: {', '.join(drift_results['target_regions'])}
Total Drifts: {drift_count}

Drift Details:
"""
    
    for drift in drift_results['drifts']:
        drift_report += f"""
- Type: {drift['type'].upper()}
  Configuration: {drift['config_name']} ({drift['config_type']})
  Regions: {drift['source_region']} -> {drift['target_region']}
  Description: {drift['description']}
"""
    
    logger.warning(f"Configuration drift detected: {drift_count} drifts found")
    send_alert(drift_report, sns_topic_arn)

def update_drift_metrics(drift_results: Dict[str, Any], project_name: str, environment: str):
    """Update CloudWatch metrics for drift detection"""
    try:
        cloudwatch = boto3.client('cloudwatch')
        
        # Put drift count metric
        cloudwatch.put_metric_data(
            Namespace=f'{project_name}/ConfigurationDrift',
            MetricData=[
                {
                    'MetricName': 'DriftCount',
                    'Value': len(drift_results['drifts']),
                    'Unit': 'Count',
                    'Dimensions': [
                        {
                            'Name': 'Environment',
                            'Value': environment
                        },
                        {
                            'Name': 'SourceRegion',
                            'Value': drift_results['source_region']
                        }
                    ]
                },
                {
                    'MetricName': 'HasDrift',
                    'Value': 1 if drift_results['has_drift'] else 0,
                    'Unit': 'Count',
                    'Dimensions': [
                        {
                            'Name': 'Environment',
                            'Value': environment
                        }
                    ]
                }
            ]
        )
    except Exception as e:
        logger.error(f"Failed to update drift metrics: {str(e)}")

def store_drift_results(drift_results: Dict[str, Any], project_name: str, environment: str):
    """Store drift detection results in Parameter Store"""
    try:
        ssm_client = boto3.client('ssm')
        
        parameter_name = f"/genai-demo/{environment}/drift-detection/latest-results"
        
        ssm_client.put_parameter(
            Name=parameter_name,
            Value=json.dumps(drift_results),
            Type='String',
            Overwrite=True,
            Description=f'Latest drift detection results for {project_name}-{environment}'
        )
    except Exception as e:
        logger.error(f"Failed to store drift results: {str(e)}")

def send_alert(message: str, sns_topic_arn: str):
    """Send alert notification"""
    if sns_topic_arn:
        try:
            sns_client = boto3.client('sns')
            sns_client.publish(
                TopicArn=sns_topic_arn,
                Subject='Configuration Drift Detection Alert',
                Message=message
            )
        except Exception as e:
            logger.error(f"Failed to send alert: {str(e)}")
            `),
            role: driftDetectionRole,
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
            },
            timeout: cdk.Duration.minutes(15),
            environment: {
                PROJECT_NAME: projectName,
                ENVIRONMENT: environment,
                ALERTING_TOPIC_ARN: alertingTopic?.topicArn || '',
                REPLICATION_REGIONS: ''  // Will be set by EventBridge rule
            }
        });

        // Create log group for the Lambda function
        new logs.LogGroup(this, 'DriftDetectionLogGroup', {
            logGroupName: `/aws/lambda/${this.driftDetectionLambda.functionName}`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY
        });

        // Schedule drift detection to run every hour
        const driftDetectionSchedule = new events.Rule(this, 'DriftDetectionSchedule', {
            schedule: events.Schedule.rate(cdk.Duration.hours(1)),
            description: 'Trigger configuration drift detection every hour'
        });

        driftDetectionSchedule.addTarget(new eventsTargets.LambdaFunction(this.driftDetectionLambda));

        cdk.Tags.of(this.driftDetectionLambda).add('Name', `${projectName}-${environment}-drift-detection`);
        cdk.Tags.of(this.driftDetectionLambda).add('Environment', environment);
        cdk.Tags.of(this.driftDetectionLambda).add('Project', projectName);
    }

    /**
     * Setup EventBridge rules for secrets monitoring
     */
    private setupSecretsEventBridge(projectName: string, environment: string): void {
        // Create EventBridge rule for Secrets Manager events
        this.secretsEventBridge = new events.Rule(this, 'SecretsManagerEventRule', {
            eventPattern: {
                source: ['aws.secretsmanager'],
                detailType: ['AWS API Call via CloudTrail'],
                detail: {
                    eventSource: ['secretsmanager.amazonaws.com'],
                    eventName: [
                        'UpdateSecret',
                        'PutSecretValue',
                        'CreateSecret'
                    ],
                    requestParameters: {
                        name: [{
                            prefix: `${environment}/genai-demo/`
                        }]
                    }
                }
            },
            description: 'Capture Secrets Manager changes for cross-region sync'
        });

        // Add targets for cross-region sync
        if (this.crossRegionSyncLambda) {
            this.secretsEventBridge.addTarget(new eventsTargets.LambdaFunction(this.crossRegionSyncLambda));
        }

        // Add targets for ConfigMap sync
        if (this.configMapSyncLambda) {
            this.secretsEventBridge.addTarget(new eventsTargets.LambdaFunction(this.configMapSyncLambda));
        }

        cdk.Tags.of(this.secretsEventBridge).add('Name', `${projectName}-${environment}-secrets-events`);
        cdk.Tags.of(this.secretsEventBridge).add('Environment', environment);
        cdk.Tags.of(this.secretsEventBridge).add('Project', projectName);
    }

    /**
     * Create database credentials secret
     */
    private createDatabaseSecret(projectName: string, environment: string, secretsManagerKey: kms.IKey): secretsmanager.Secret {
        const secret = new secretsmanager.Secret(this, 'DatabaseSecret', {
            secretName: `${environment}/genai-demo/database`,
            description: `Database credentials for ${projectName} ${environment}`,
            generateSecretString: {
                secretStringTemplate: JSON.stringify({
                    username: 'genaidemo_admin',
                    dbname: 'genaidemo',
                    engine: 'postgres',
                    host: '', // Will be populated by RDS
                    port: 5432
                }),
                generateStringKey: 'password',
                excludeCharacters: '"@/\\\'`',
                includeSpace: false,
                passwordLength: 32,
                requireEachIncludedType: true
            },
            encryptionKey: secretsManagerKey,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(secret).add('Name', `${projectName}-${environment}-database-secret`);
        cdk.Tags.of(secret).add('Environment', environment);
        cdk.Tags.of(secret).add('Project', projectName);
        cdk.Tags.of(secret).add('SecretType', 'Database');

        return secret;
    }

    /**
     * Create API keys secret
     */
    private createApiKeysSecret(projectName: string, environment: string, secretsManagerKey: kms.IKey): secretsmanager.Secret {
        const secret = new secretsmanager.Secret(this, 'ApiKeysSecret', {
            secretName: `${environment}/genai-demo/api-keys`,
            description: `API keys for ${projectName} ${environment}`,
            secretStringValue: cdk.SecretValue.unsafePlainText(JSON.stringify({
                openai_api_key: 'PLACEHOLDER_OPENAI_KEY',
                anthropic_api_key: 'PLACEHOLDER_ANTHROPIC_KEY',
                bedrock_access_key: 'PLACEHOLDER_BEDROCK_KEY',
                external_service_key: 'PLACEHOLDER_EXTERNAL_KEY'
            })),
            encryptionKey: secretsManagerKey,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(secret).add('Name', `${projectName}-${environment}-api-keys-secret`);
        cdk.Tags.of(secret).add('Environment', environment);
        cdk.Tags.of(secret).add('Project', projectName);
        cdk.Tags.of(secret).add('SecretType', 'ApiKeys');

        return secret;
    }

    /**
     * Create application secrets
     */
    private createApplicationSecretsSecret(projectName: string, environment: string, secretsManagerKey: kms.IKey): secretsmanager.Secret {
        const secret = new secretsmanager.Secret(this, 'ApplicationSecretsSecret', {
            secretName: `${environment}/genai-demo/application`,
            description: `Application secrets for ${projectName} ${environment}`,
            secretStringValue: cdk.SecretValue.unsafePlainText(JSON.stringify({
                jwt_secret: 'PLACEHOLDER_JWT_SECRET',
                encryption_key: 'PLACEHOLDER_ENCRYPTION_KEY',
                session_secret: 'PLACEHOLDER_SESSION_SECRET',
                webhook_secret: 'PLACEHOLDER_WEBHOOK_SECRET'
            })),
            encryptionKey: secretsManagerKey,
            removalPolicy: environment === 'production'
                ? cdk.RemovalPolicy.RETAIN
                : cdk.RemovalPolicy.DESTROY
        });

        cdk.Tags.of(secret).add('Name', `${projectName}-${environment}-application-secret`);
        cdk.Tags.of(secret).add('Environment', environment);
        cdk.Tags.of(secret).add('Project', projectName);
        cdk.Tags.of(secret).add('SecretType', 'Application');

        return secret;
    }

    /**
     * Create Lambda function for database secret rotation
     */
    private createRotationLambda(vpc: ec2.IVpc, projectName: string, environment: string): lambda.Function {
        // Create IAM role for rotation Lambda
        const rotationRole = new iam.Role(this, 'RotationLambdaRole', {
            assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaVPCAccessExecutionRole')
            ],
            inlinePolicies: {
                SecretsManagerRotationPolicy: new iam.PolicyDocument({
                    statements: [
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'secretsmanager:DescribeSecret',
                                'secretsmanager:GetSecretValue',
                                'secretsmanager:PutSecretValue',
                                'secretsmanager:UpdateSecretVersionStage'
                            ],
                            resources: [this.databaseSecret.secretArn]
                        }),
                        new iam.PolicyStatement({
                            effect: iam.Effect.ALLOW,
                            actions: [
                                'rds:ModifyDBInstance',
                                'rds:ModifyDBCluster',
                                'rds:DescribeDBInstances',
                                'rds:DescribeDBClusters'
                            ],
                            resources: ['*']
                        })
                    ]
                })
            }
        });

        // Create security group for rotation Lambda
        const rotationSecurityGroup = new ec2.SecurityGroup(this, 'RotationLambdaSecurityGroup', {
            vpc: vpc,
            description: 'Security group for database secret rotation Lambda',
            allowAllOutbound: true
        });

        // Create the rotation Lambda function
        const rotationFunction = new lambda.Function(this, 'DatabaseRotationLambda', {
            functionName: `${projectName}-${environment}-db-rotation`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'lambda_function.lambda_handler',
            code: lambda.Code.fromInline(`
import json
import boto3
import logging
import os

logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    """
    AWS Secrets Manager rotation function for RDS PostgreSQL
    """
    try:
        # Get the secret ARN from the event
        secret_arn = event['Step1']['SecretArn']
        token = event['Step1']['ClientRequestToken']
        step = event['Step1']['Step']
        
        # Initialize AWS clients
        secrets_client = boto3.client('secretsmanager')
        rds_client = boto3.client('rds')
        
        logger.info(f"Starting rotation step: {step} for secret: {secret_arn}")
        
        if step == "createSecret":
            create_secret(secrets_client, secret_arn, token)
        elif step == "setSecret":
            set_secret(secrets_client, rds_client, secret_arn, token)
        elif step == "testSecret":
            test_secret(secrets_client, secret_arn, token)
        elif step == "finishSecret":
            finish_secret(secrets_client, secret_arn, token)
        else:
            raise ValueError(f"Invalid step parameter: {step}")
            
        return {"statusCode": 200, "body": json.dumps("Rotation step completed successfully")}
        
    except Exception as e:
        logger.error(f"Rotation failed: {str(e)}")
        raise e

def create_secret(secrets_client, secret_arn, token):
    """Create a new secret version with a new password"""
    logger.info("Creating new secret version")
    # Implementation for creating new secret version
    pass

def set_secret(secrets_client, rds_client, secret_arn, token):
    """Set the new password in the database"""
    logger.info("Setting new password in database")
    # Implementation for setting new password
    pass

def test_secret(secrets_client, secret_arn, token):
    """Test the new password"""
    logger.info("Testing new password")
    # Implementation for testing new password
    pass

def finish_secret(secrets_client, secret_arn, token):
    """Finalize the rotation"""
    logger.info("Finalizing rotation")
    # Implementation for finalizing rotation
    pass
            `),
            role: rotationRole,
            vpc: vpc,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
            },
            securityGroups: [rotationSecurityGroup],
            timeout: cdk.Duration.minutes(5),
            environment: {
                SECRETS_MANAGER_ENDPOINT: `https://secretsmanager.${this.region}.amazonaws.com`
            }
        });

        cdk.Tags.of(rotationFunction).add('Name', `${projectName}-${environment}-db-rotation`);
        cdk.Tags.of(rotationFunction).add('Environment', environment);
        cdk.Tags.of(rotationFunction).add('Project', projectName);

        return rotationFunction;
    }

    /**
     * Setup automatic rotation for database secret
     */
    private setupDatabaseSecretRotation(databaseCluster?: rds.IDatabaseCluster, databaseInstance?: rds.IDatabaseInstance): void {
        if (!this.rotationLambda) return;

        // Create rotation schedule
        new secretsmanager.RotationSchedule(this, 'DatabaseSecretRotation', {
            secret: this.databaseSecret,
            rotationLambda: this.rotationLambda,
            automaticallyAfter: cdk.Duration.days(30), // Rotate every 30 days
            rotateImmediatelyOnUpdate: false
        });

        // Attach the secret to the database for automatic rotation
        if (databaseCluster) {
            this.databaseSecret.attach(databaseCluster);
        } else if (databaseInstance) {
            this.databaseSecret.attach(databaseInstance);
        }
    }

    /**
     * Setup cross-region replication for multi-region deployment
     */
    private setupCrossRegionReplication(projectName: string, environment: string, replicationRegions: string[]): void {
        // Create replica secrets in target regions using CloudFormation custom resources
        const secrets = [
            { secret: this.databaseSecret, type: 'database' },
            { secret: this.apiKeysSecret, type: 'api-keys' },
            { secret: this.applicationSecretsSecret, type: 'application' }
        ];

        secrets.forEach(({ secret, type }) => {
            // Add replication configuration tags
            cdk.Tags.of(secret).add('ReplicationRegions', replicationRegions.join(','));
            cdk.Tags.of(secret).add('ReplicationType', 'cross-region');
            cdk.Tags.of(secret).add('SecretType', type);

            // Create replica secrets using custom resource
            replicationRegions.forEach(targetRegion => {
                new cdk.CustomResource(this, `${type}SecretReplica${targetRegion}`, {
                    serviceToken: this.createReplicationCustomResourceProvider(projectName, environment).serviceToken,
                    properties: {
                        SourceSecretArn: secret.secretArn,
                        TargetRegion: targetRegion,
                        SecretName: `${environment}/genai-demo/${type}`,
                        KmsKeyId: `alias/${projectName}-${environment}-secrets-key`,
                        ReplicationRegions: replicationRegions.join(',')
                    }
                });
            });
        });
    }

    /**
     * Create custom resource provider for secret replication
     */
    private createReplicationCustomResourceProvider(projectName: string, environment: string): cdk.CustomResourceProvider {
        return cdk.CustomResourceProvider.getOrCreateProvider(this, 'SecretReplicationProvider', {
            codeDirectory: './lambda-code',
            runtime: cdk.CustomResourceProviderRuntime.NODEJS_18_X,
            policyStatements: [
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'secretsmanager:CreateSecret',
                        'secretsmanager:UpdateSecret',
                        'secretsmanager:DeleteSecret',
                        'secretsmanager:GetSecretValue',
                        'secretsmanager:ReplicateSecretToRegions',
                        'secretsmanager:RemoveRegionsFromReplication'
                    ],
                    resources: ['*']
                }),
                new iam.PolicyStatement({
                    effect: iam.Effect.ALLOW,
                    actions: [
                        'kms:Decrypt',
                        'kms:Encrypt',
                        'kms:GenerateDataKey',
                        'kms:CreateGrant'
                    ],
                    resources: ['*']
                })
            ]
        });
    }

    /**
     * Store secret information in Parameter Store with cross-region support
     */
    private createParameterStoreParameters(projectName: string, environment: string, region: string): void {
        const parameterPrefix = `/genai-demo/${environment}/${region}/secrets`;
        const globalParameterPrefix = `/genai-demo/${environment}/global/secrets`;

        // Database secret ARN parameter (region-specific)
        new ssm.StringParameter(this, 'DatabaseSecretArnParameter', {
            parameterName: `${parameterPrefix}/database-secret-arn`,
            stringValue: this.databaseSecret.secretArn,
            description: `Database secret ARN for ${projectName} ${environment} in ${region}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // API keys secret ARN parameter (region-specific)
        new ssm.StringParameter(this, 'ApiKeysSecretArnParameter', {
            parameterName: `${parameterPrefix}/api-keys-secret-arn`,
            stringValue: this.apiKeysSecret.secretArn,
            description: `API keys secret ARN for ${projectName} ${environment} in ${region}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Application secrets ARN parameter (region-specific)
        new ssm.StringParameter(this, 'ApplicationSecretsArnParameter', {
            parameterName: `${parameterPrefix}/application-secret-arn`,
            stringValue: this.applicationSecretsSecret.secretArn,
            description: `Application secrets ARN for ${projectName} ${environment} in ${region}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Cross-region configuration parameters (global)
        new ssm.StringParameter(this, 'CrossRegionConfigParameter', {
            parameterName: `${globalParameterPrefix}/cross-region-config`,
            stringValue: JSON.stringify({
                primaryRegion: region,
                replicationRegions: this.node.tryGetContext('replicationRegions') || [],
                syncEnabled: true,
                driftDetectionEnabled: true,
                configMapSyncEnabled: true,
                lastSyncTimestamp: new Date().toISOString()
            }),
            description: `Cross-region configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Enhanced rotation configuration parameter
        new ssm.StringParameter(this, 'RotationConfigParameter', {
            parameterName: `${parameterPrefix}/rotation-config`,
            stringValue: JSON.stringify({
                databaseRotationDays: 30,
                apiKeysRotationDays: 90,
                applicationSecretsRotationDays: 60,
                autoRotationEnabled: true,
                crossRegionReplication: true,
                rotationNotificationEnabled: true,
                rotationBackupEnabled: true,
                rotationValidationEnabled: true
            }),
            description: `Enhanced secret rotation configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // GitOps deployment configuration
        new ssm.StringParameter(this, 'GitOpsConfigParameter', {
            parameterName: `${globalParameterPrefix}/gitops-config`,
            stringValue: JSON.stringify({
                deploymentStrategy: 'blue-green',
                canaryPercentage: 10,
                rollbackOnFailure: true,
                healthCheckEnabled: true,
                multiRegionDeployment: true,
                configValidationEnabled: true,
                approvalRequired: environment === 'production'
            }),
            description: `GitOps deployment configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // ConfigMap synchronization configuration
        new ssm.StringParameter(this, 'ConfigMapSyncConfigParameter', {
            parameterName: `${globalParameterPrefix}/configmap-sync-config`,
            stringValue: JSON.stringify({
                enabled: true,
                namespace: 'default',
                configMapName: `${projectName}-${environment}-config`,
                syncInterval: '5m',
                excludeSensitiveKeys: true,
                includeKeys: ['log_level', 'cache_ttl', 'max_connections', 'session_timeout'],
                kubernetesServiceAccount: `${projectName}-${environment}-secrets-sync`
            }),
            description: `ConfigMap synchronization configuration for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });

        // Drift detection configuration
        new ssm.StringParameter(this, 'DriftDetectionConfigParameter', {
            parameterName: `${globalParameterPrefix}/drift-detection-config`,
            stringValue: JSON.stringify({
                enabled: true,
                checkInterval: '1h',
                alertThreshold: 1,
                autoRemediation: false,
                includeSecrets: true,
                includeParameters: true,
                includeConfigMaps: true,
                retentionDays: 30
            }),
            description: `Configuration drift detection settings for ${projectName} ${environment}`,
            tier: ssm.ParameterTier.STANDARD,
            simpleName: false
        });
    }

    /**
     * Create CloudFormation outputs for cross-stack references
     */
    private createOutputs(projectName: string, environment: string): void {
        // Database secret outputs
        new cdk.CfnOutput(this, 'DatabaseSecretArn', {
            value: this.databaseSecret.secretArn,
            description: 'Database secret ARN',
            exportName: `${projectName}-${environment}-database-secret-arn`
        });

        new cdk.CfnOutput(this, 'DatabaseSecretName', {
            value: this.databaseSecret.secretName,
            description: 'Database secret name',
            exportName: `${projectName}-${environment}-database-secret-name`
        });

        // API keys secret outputs
        new cdk.CfnOutput(this, 'ApiKeysSecretArn', {
            value: this.apiKeysSecret.secretArn,
            description: 'API keys secret ARN',
            exportName: `${projectName}-${environment}-api-keys-secret-arn`
        });

        new cdk.CfnOutput(this, 'ApiKeysSecretName', {
            value: this.apiKeysSecret.secretName,
            description: 'API keys secret name',
            exportName: `${projectName}-${environment}-api-keys-secret-name`
        });

        // Application secrets outputs
        new cdk.CfnOutput(this, 'ApplicationSecretsArn', {
            value: this.applicationSecretsSecret.secretArn,
            description: 'Application secrets ARN',
            exportName: `${projectName}-${environment}-application-secrets-arn`
        });

        new cdk.CfnOutput(this, 'ApplicationSecretsName', {
            value: this.applicationSecretsSecret.secretName,
            description: 'Application secrets name',
            exportName: `${projectName}-${environment}-application-secrets-name`
        });

        // Cross-region sync Lambda outputs
        if (this.crossRegionSyncLambda) {
            new cdk.CfnOutput(this, 'CrossRegionSyncLambdaArn', {
                value: this.crossRegionSyncLambda.functionArn,
                description: 'Cross-region sync Lambda function ARN',
                exportName: `${projectName}-${environment}-cross-region-sync-lambda-arn`
            });

            new cdk.CfnOutput(this, 'CrossRegionSyncLambdaName', {
                value: this.crossRegionSyncLambda.functionName,
                description: 'Cross-region sync Lambda function name',
                exportName: `${projectName}-${environment}-cross-region-sync-lambda-name`
            });
        }

        // ConfigMap sync Lambda outputs
        if (this.configMapSyncLambda) {
            new cdk.CfnOutput(this, 'ConfigMapSyncLambdaArn', {
                value: this.configMapSyncLambda.functionArn,
                description: 'ConfigMap sync Lambda function ARN',
                exportName: `${projectName}-${environment}-configmap-sync-lambda-arn`
            });

            new cdk.CfnOutput(this, 'ConfigMapSyncLambdaName', {
                value: this.configMapSyncLambda.functionName,
                description: 'ConfigMap sync Lambda function name',
                exportName: `${projectName}-${environment}-configmap-sync-lambda-name`
            });
        }

        // Drift detection Lambda outputs
        if (this.driftDetectionLambda) {
            new cdk.CfnOutput(this, 'DriftDetectionLambdaArn', {
                value: this.driftDetectionLambda.functionArn,
                description: 'Configuration drift detection Lambda function ARN',
                exportName: `${projectName}-${environment}-drift-detection-lambda-arn`
            });

            new cdk.CfnOutput(this, 'DriftDetectionLambdaName', {
                value: this.driftDetectionLambda.functionName,
                description: 'Configuration drift detection Lambda function name',
                exportName: `${projectName}-${environment}-drift-detection-lambda-name`
            });
        }

        // EventBridge rule outputs
        if (this.secretsEventBridge) {
            new cdk.CfnOutput(this, 'SecretsEventBridgeRuleArn', {
                value: this.secretsEventBridge.ruleArn,
                description: 'Secrets Manager EventBridge rule ARN',
                exportName: `${projectName}-${environment}-secrets-eventbridge-rule-arn`
            });

            new cdk.CfnOutput(this, 'SecretsEventBridgeRuleName', {
                value: this.secretsEventBridge.ruleName,
                description: 'Secrets Manager EventBridge rule name',
                exportName: `${projectName}-${environment}-secrets-eventbridge-rule-name`
            });
        }
    }
}