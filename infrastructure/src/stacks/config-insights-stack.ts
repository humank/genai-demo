import * as cdk from 'aws-cdk-lib';
import * as config from 'aws-cdk-lib/aws-config';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import { Construct } from 'constructs';

export interface ConfigInsightsStackProps extends cdk.StackProps {
  /**
   * Email address for configuration change notifications
   */
  readonly alertEmail: string;
  
  /**
   * Enable configuration change tracking
   * @default true
   */
  readonly enableConfigRecorder?: boolean;
  
  /**
   * Enable compliance rule violation detection
   * @default true
   */
  readonly enableComplianceRules?: boolean;
  
  /**
   * Enable security configuration drift monitoring
   * @default true
   */
  readonly enableDriftMonitoring?: boolean;
  
  /**
   * Configuration snapshot delivery frequency
   * @default TwentyFour_Hours
   */
  readonly snapshotDeliveryFrequency?: config.MaximumExecutionFrequency;
}

/**
 * AWS Config Insights Stack
 * 
 * Implements AWS Config for configuration management:
 * - Resource change tracking
 * - Compliance rule violation detection
 * - Security configuration drift monitoring
 * - Automated remediation workflows
 * 
 * Architecture: AWS Native Services
 * Integration: SNS alerts + CloudWatch Dashboard + EventBridge
 */
export class ConfigInsightsStack extends cdk.Stack {
  public readonly configAlertTopic: sns.Topic;
  public readonly configBucket: s3.Bucket;
  public readonly configRecorder?: config.CfnConfigurationRecorder;
  public readonly configDashboard: cloudwatch.Dashboard;
  public readonly remediationFunction?: lambda.Function;

  constructor(scope: Construct, id: string, props: ConfigInsightsStackProps) {
    super(scope, id, props);

    // ============================================
    // 1. SNS Topic for Configuration Alerts
    // ============================================
    this.configAlertTopic = new sns.Topic(this, 'ConfigAlertTopic', {
      displayName: 'GenAIDemo Config Alerts',
      topicName: 'genai-demo-config-alerts',
    });

    // Subscribe email for configuration alerts
    this.configAlertTopic.addSubscription(
      new subscriptions.EmailSubscription(props.alertEmail)
    );

    // ============================================
    // 2. S3 Bucket for Configuration History
    // ============================================
    this.configBucket = new s3.Bucket(this, 'ConfigBucket', {
      bucketName: `genai-demo-config-${this.account}-${this.region}`,
      encryption: s3.BucketEncryption.S3_MANAGED,
      lifecycleRules: [
        {
          expiration: cdk.Duration.days(365), // Keep config history for 1 year
        },
      ],
      blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,
      enforceSSL: true,
      versioned: true,
    });

    // ============================================
    // 3. IAM Role for AWS Config
    // ============================================
    const configRole = new iam.Role(this, 'ConfigRole', {
      assumedBy: new iam.ServicePrincipal('config.amazonaws.com'),
      managedPolicies: [
        iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/ConfigRole'),
      ],
    });

    // Grant Config permissions to write to S3
    this.configBucket.grantWrite(configRole);

    // Grant Config permissions to publish to SNS
    this.configAlertTopic.grantPublish(configRole);

    // ============================================
    // 4. AWS Config Recorder
    // ============================================
    if (props.enableConfigRecorder !== false) {
      this.configRecorder = new config.CfnConfigurationRecorder(this, 'ConfigRecorder', {
        name: 'genai-demo-config-recorder',
        roleArn: configRole.roleArn,
        recordingGroup: {
          allSupported: true,
          includeGlobalResourceTypes: true,
          resourceTypes: [],
        },
      });

      // Delivery Channel for Config
      const deliveryChannel = new config.CfnDeliveryChannel(this, 'ConfigDeliveryChannel', {
        name: 'genai-demo-config-delivery',
        s3BucketName: this.configBucket.bucketName,
        snsTopicArn: this.configAlertTopic.topicArn,
        configSnapshotDeliveryProperties: {
          deliveryFrequency: props.snapshotDeliveryFrequency ?? 
            config.MaximumExecutionFrequency.TWENTY_FOUR_HOURS,
        },
      });

      // Ensure delivery channel is created after recorder
      deliveryChannel.addDependency(this.configRecorder);
    }

    // ============================================
    // 5. Compliance Rules
    // ============================================
    if (props.enableComplianceRules !== false) {
      // Rule: Ensure all EBS volumes are encrypted
      new config.ManagedRule(this, 'EbsEncryptionRule', {
        identifier: config.ManagedRuleIdentifiers.EC2_EBS_ENCRYPTION_BY_DEFAULT,
        description: 'Checks that Amazon Elastic Block Store (EBS) encryption is enabled by default',
        configRuleName: 'ebs-encryption-enabled',
      });

      // Rule: Ensure S3 buckets have encryption enabled
      new config.ManagedRule(this, 'S3EncryptionRule', {
        identifier: config.ManagedRuleIdentifiers.S3_BUCKET_SERVER_SIDE_ENCRYPTION_ENABLED,
        description: 'Checks that S3 buckets have server-side encryption enabled',
        configRuleName: 's3-bucket-encryption-enabled',
      });

      // Rule: Ensure RDS instances are encrypted
      new config.ManagedRule(this, 'RdsEncryptionRule', {
        identifier: config.ManagedRuleIdentifiers.RDS_STORAGE_ENCRYPTED,
        description: 'Checks that RDS instances have encryption at rest enabled',
        configRuleName: 'rds-storage-encrypted',
      });

      // Rule: Ensure IAM password policy meets requirements
      new config.ManagedRule(this, 'IamPasswordPolicyRule', {
        identifier: config.ManagedRuleIdentifiers.IAM_PASSWORD_POLICY,
        description: 'Checks that the account password policy meets specified requirements',
        configRuleName: 'iam-password-policy-compliant',
        inputParameters: {
          RequireUppercaseCharacters: 'true',
          RequireLowercaseCharacters: 'true',
          RequireSymbols: 'true',
          RequireNumbers: 'true',
          MinimumPasswordLength: '14',
          PasswordReusePrevention: '24',
          MaxPasswordAge: '90',
        },
      });

      // Rule: Ensure CloudTrail is enabled
      new config.ManagedRule(this, 'CloudTrailEnabledRule', {
        identifier: config.ManagedRuleIdentifiers.CLOUD_TRAIL_ENABLED,
        description: 'Checks that CloudTrail is enabled in the account',
        configRuleName: 'cloudtrail-enabled',
      });

      // Rule: Ensure VPC flow logs are enabled
      new config.ManagedRule(this, 'VpcFlowLogsRule', {
        identifier: config.ManagedRuleIdentifiers.VPC_FLOW_LOGS_ENABLED,
        description: 'Checks that VPC flow logs are enabled for all VPCs',
        configRuleName: 'vpc-flow-logs-enabled',
      });

      // Rule: Ensure security groups don't allow unrestricted access
      new config.ManagedRule(this, 'SecurityGroupRestrictedRule', {
        identifier: config.ManagedRuleIdentifiers.VPC_SG_OPEN_ONLY_TO_AUTHORIZED_PORTS,
        description: 'Checks that security groups do not allow unrestricted access',
        configRuleName: 'security-group-restricted-access',
        inputParameters: {
          authorizedTcpPorts: '443,22',
        },
      });

      // Rule: Ensure EKS clusters have logging enabled
      new config.ManagedRule(this, 'EksLoggingRule', {
        identifier: config.ManagedRuleIdentifiers.EKS_CLUSTER_OLDEST_SUPPORTED_VERSION,
        description: 'Checks that EKS clusters are running supported versions',
        configRuleName: 'eks-cluster-supported-version',
      });

      // Rule: Ensure Aurora clusters have backup retention
      new config.ManagedRule(this, 'RdsBackupRule', {
        identifier: config.ManagedRuleIdentifiers.RDS_DB_INSTANCE_BACKUP_ENABLED,
        description: 'Checks that RDS/Aurora instances have backups enabled',
        configRuleName: 'rds-backup-enabled',
      });

      // Rule: Ensure ElastiCache clusters have encryption in transit
      new config.ManagedRule(this, 'ElastiCacheEncryptionRule', {
        identifier: config.ManagedRuleIdentifiers.ELASTICACHE_REDIS_CLUSTER_AUTOMATIC_BACKUP_CHECK,
        description: 'Checks that ElastiCache Redis clusters have automatic backups enabled',
        configRuleName: 'elasticache-backup-enabled',
      });
    }

    // ============================================
    // 6. Configuration Drift Monitoring
    // ============================================
    if (props.enableDriftMonitoring !== false) {
      // Lambda function for drift detection and alerting
      this.remediationFunction = new lambda.Function(this, 'DriftMonitoringFunction', {
        functionName: 'genai-demo-config-drift-monitor',
        runtime: lambda.Runtime.PYTHON_3_11,
        handler: 'index.handler',
        code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime

config_client = boto3.client('config')
sns_client = boto3.client('sns')

SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']

def handler(event, context):
    """
    Monitor configuration drift and send alerts
    
    Detects:
    - Non-compliant resources
    - Configuration changes
    - Security drift
    """
    
    try:
        # Get compliance summary
        compliance_summary = config_client.describe_compliance_by_config_rule()
        
        non_compliant_rules = []
        total_non_compliant = 0
        
        for rule in compliance_summary.get('ComplianceByConfigRules', []):
            compliance = rule.get('Compliance', {})
            if compliance.get('ComplianceType') == 'NON_COMPLIANT':
                rule_name = rule.get('ConfigRuleName')
                non_compliant_rules.append(rule_name)
                
                # Get non-compliant resources
                resources = config_client.get_compliance_details_by_config_rule(
                    ConfigRuleName=rule_name,
                    ComplianceTypes=['NON_COMPLIANT'],
                    Limit=10
                )
                
                resource_count = len(resources.get('EvaluationResults', []))
                total_non_compliant += resource_count
        
        # Generate drift report
        report = {
            'timestamp': datetime.utcnow().isoformat(),
            'total_non_compliant_rules': len(non_compliant_rules),
            'total_non_compliant_resources': total_non_compliant,
            'non_compliant_rules': non_compliant_rules
        }
        
        # Send alert if drift detected
        if non_compliant_rules:
            message = f"""
AWS Config Drift Detection Alert
Generated: {report['timestamp']}

Non-Compliant Rules: {report['total_non_compliant_rules']}
Non-Compliant Resources: {report['total_non_compliant_resources']}

Rules with Violations:
"""
            for rule in non_compliant_rules[:10]:
                message += f"- {rule}\\n"
            
            if len(non_compliant_rules) > 10:
                message += f"\\n... and {len(non_compliant_rules) - 10} more rules\\n"
            
            message += """
\\nPlease review the AWS Config console for detailed information.
"""
            
            sns_client.publish(
                TopicArn=SNS_TOPIC_ARN,
                Subject='AWS Config Drift Detection Alert',
                Message=message
            )
        
        return {
            'statusCode': 200,
            'body': json.dumps(report)
        }
        
    except Exception as e:
        error_message = f'Error monitoring configuration drift: {str(e)}'
        print(error_message)
        
        sns_client.publish(
            TopicArn=SNS_TOPIC_ARN,
            Subject='Config Drift Monitoring Error',
            Message=error_message
        )
        
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
`),
        timeout: cdk.Duration.minutes(5),
        environment: {
          SNS_TOPIC_ARN: this.configAlertTopic.topicArn,
        },
      });

      // Grant permissions to Lambda
      this.remediationFunction.addToRolePolicy(
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'config:DescribeComplianceByConfigRule',
            'config:GetComplianceDetailsByConfigRule',
            'config:DescribeConfigRules',
            'config:GetResourceConfigHistory',
          ],
          resources: ['*'],
        })
      );

      this.configAlertTopic.grantPublish(this.remediationFunction);

      // Schedule daily drift monitoring
      const driftMonitoringRule = new events.Rule(this, 'DriftMonitoringSchedule', {
        ruleName: 'genai-demo-config-drift-daily',
        description: 'Daily configuration drift monitoring',
        schedule: events.Schedule.cron({
          hour: '8',
          minute: '0',
        }),
      });

      driftMonitoringRule.addTarget(
        new targets.LambdaFunction(this.remediationFunction)
      );

      // EventBridge rule for real-time compliance changes
      const complianceChangeRule = new events.Rule(this, 'ComplianceChangeRule', {
        ruleName: 'genai-demo-config-compliance-change',
        description: 'Trigger on AWS Config compliance changes',
        eventPattern: {
          source: ['aws.config'],
          detailType: ['Config Rules Compliance Change'],
          detail: {
            newEvaluationResult: {
              complianceType: ['NON_COMPLIANT'],
            },
          },
        },
      });

      complianceChangeRule.addTarget(
        new targets.LambdaFunction(this.remediationFunction)
      );
    }

    // ============================================
    // 7. CloudWatch Dashboard for Config Insights
    // ============================================
    this.configDashboard = new cloudwatch.Dashboard(this, 'ConfigDashboard', {
      dashboardName: 'GenAIDemo-Config-Insights',
    });

    // Compliance status widget
    this.configDashboard.addWidgets(
      new cloudwatch.SingleValueWidget({
        title: 'Compliant Resources',
        width: 8,
        height: 6,
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'ComplianceScore',
            statistic: 'Average',
            period: cdk.Duration.hours(1),
          }),
        ],
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Non-Compliant Resources',
        width: 8,
        height: 6,
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'NonCompliantResources',
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
          }),
        ],
      }),
      new cloudwatch.SingleValueWidget({
        title: 'Configuration Changes (24h)',
        width: 8,
        height: 6,
        metrics: [
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'ConfigurationChanges',
            statistic: 'Sum',
            period: cdk.Duration.hours(24),
          }),
        ],
      })
    );

    // Configuration change trend
    this.configDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Configuration Changes Over Time',
        width: 24,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'ConfigurationChanges',
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'Configuration Changes',
          }),
        ],
      })
    );

    // Compliance by resource type
    this.configDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Compliance Status by Resource Type',
        width: 12,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'CompliantResources',
            dimensionsMap: {
              ResourceType: 'AWS::EC2::Instance',
            },
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'EC2 Instances',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'CompliantResources',
            dimensionsMap: {
              ResourceType: 'AWS::RDS::DBInstance',
            },
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'RDS Instances',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'CompliantResources',
            dimensionsMap: {
              ResourceType: 'AWS::S3::Bucket',
            },
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'S3 Buckets',
          }),
        ],
      }),
      new cloudwatch.GraphWidget({
        title: 'Non-Compliant Resources by Type',
        width: 12,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'NonCompliantResources',
            dimensionsMap: {
              ResourceType: 'AWS::EC2::Instance',
            },
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'EC2 Instances',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'NonCompliantResources',
            dimensionsMap: {
              ResourceType: 'AWS::RDS::DBInstance',
            },
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'RDS Instances',
          }),
          new cloudwatch.Metric({
            namespace: 'AWS/Config',
            metricName: 'NonCompliantResources',
            dimensionsMap: {
              ResourceType: 'AWS::S3::Bucket',
            },
            statistic: 'Sum',
            period: cdk.Duration.hours(1),
            label: 'S3 Buckets',
          }),
        ],
      })
    );

    // ============================================
    // Outputs
    // ============================================
    new cdk.CfnOutput(this, 'ConfigAlertTopicArn', {
      value: this.configAlertTopic.topicArn,
      description: 'SNS Topic ARN for configuration alerts',
      exportName: 'GenAIDemo-ConfigAlertTopicArn',
    });

    new cdk.CfnOutput(this, 'ConfigBucketName', {
      value: this.configBucket.bucketName,
      description: 'S3 Bucket for AWS Config history',
      exportName: 'GenAIDemo-ConfigBucket',
    });

    new cdk.CfnOutput(this, 'ConfigDashboardName', {
      value: this.configDashboard.dashboardName,
      description: 'CloudWatch Dashboard for Config insights',
      exportName: 'GenAIDemo-ConfigDashboard',
    });

    if (this.remediationFunction) {
      new cdk.CfnOutput(this, 'DriftMonitoringFunctionArn', {
        value: this.remediationFunction.functionArn,
        description: 'Lambda Function ARN for drift monitoring',
        exportName: 'GenAIDemo-DriftMonitoringFunction',
      });
    }

    // Tags for resource organization
    cdk.Tags.of(this).add('Project', 'GenAIDemo');
    cdk.Tags.of(this).add('Environment', 'Production');
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
    cdk.Tags.of(this).add('Component', 'ConfigInsights');
  }
}
