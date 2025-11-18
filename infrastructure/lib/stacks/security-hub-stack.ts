import * as cdk from 'aws-cdk-lib';
import * as securityhub from 'aws-cdk-lib/aws-securityhub';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as path from 'path';
import { Construct } from 'constructs';

export interface SecurityHubStackProps extends cdk.StackProps {
  readonly notificationEmail: string;
  readonly enableAutomatedResponse?: boolean;
}

/**
 * Security Hub Stack - Comprehensive Security Insights
 * 
 * This stack implements:
 * - Unified security findings collection and correlation (Requirement 13.25)
 * - Threat intelligence integration (Requirement 13.26)
 * - Automated incident response for high-risk findings (Requirement 13.27)
 * 
 * Architecture:
 * - AWS Security Hub for centralized security findings
 * - EventBridge for automated incident response
 * - Lambda for automated remediation actions
 * - SNS for security alerting
 */
export class SecurityHubStack extends cdk.Stack {
  public readonly securityHub: securityhub.CfnHub;
  public readonly criticalFindingsTopic: sns.Topic;
  public readonly highFindingsTopic: sns.Topic;
  public readonly incidentResponseFunction: lambda.Function;

  constructor(scope: Construct, id: string, props: SecurityHubStackProps) {
    super(scope, id, props);

    // ========================================
    // 1. Enable AWS Security Hub
    // ========================================
    
    this.securityHub = new securityhub.CfnHub(this, 'SecurityHub', {
      controlFindingGenerator: 'SECURITY_CONTROL',
      enableDefaultStandards: true,
      tags: {
        Name: 'GenAI-Demo-Security-Hub',
        Environment: 'Production',
        ManagedBy: 'CDK',
      },
    });

    // Enable AWS Foundational Security Best Practices standard
    const fsbpStandard = new securityhub.CfnStandard(this, 'FSBPStandard', {
      standardsArn: `arn:aws:securityhub:${this.region}::standards/aws-foundational-security-best-practices/v/1.0.0`,
    });
    fsbpStandard.node.addDependency(this.securityHub);

    // Enable CIS AWS Foundations Benchmark
    const cisStandard = new securityhub.CfnStandard(this, 'CISStandard', {
      standardsArn: `arn:aws:securityhub:${this.region}::standards/cis-aws-foundations-benchmark/v/1.2.0`,
    });
    cisStandard.node.addDependency(this.securityHub);

    // Enable PCI DSS standard
    const pciStandard = new securityhub.CfnStandard(this, 'PCIStandard', {
      standardsArn: `arn:aws:securityhub:${this.region}::standards/pci-dss/v/3.2.1`,
    });
    pciStandard.node.addDependency(this.securityHub);

    // ========================================
    // 2. SNS Topics for Security Alerts
    // ========================================
    
    // Critical findings topic (CRITICAL severity)
    this.criticalFindingsTopic = new sns.Topic(this, 'CriticalFindingsTopic', {
      displayName: 'Security Hub Critical Findings',
      topicName: 'security-hub-critical-findings',
    });

    this.criticalFindingsTopic.addSubscription(
      new subscriptions.EmailSubscription(props.notificationEmail)
    );

    // High findings topic (HIGH severity)
    this.highFindingsTopic = new sns.Topic(this, 'HighFindingsTopic', {
      displayName: 'Security Hub High Findings',
      topicName: 'security-hub-high-findings',
    });

    this.highFindingsTopic.addSubscription(
      new subscriptions.EmailSubscription(props.notificationEmail)
    );

    // ========================================
    // 3. Automated Incident Response Lambda
    // ========================================
    
    // Lambda function for automated incident response
    this.incidentResponseFunction = new lambda.Function(this, 'IncidentResponseFunction', {
      runtime: lambda.Runtime.PYTHON_3_11,
      handler: 'index.handler',
      code: lambda.Code.fromAsset(path.join(__dirname, '../../src/lambda/security-hub-incident-response')),
      environment: {
        CRITICAL_TOPIC_ARN: this.criticalFindingsTopic.topicArn,
        HIGH_TOPIC_ARN: this.highFindingsTopic.topicArn,
      },
      timeout: cdk.Duration.minutes(5),
      logRetention: logs.RetentionDays.ONE_MONTH,
      description: 'Automated incident response for Security Hub findings',
    });

    // Grant permissions to Lambda function
    this.incidentResponseFunction.addToRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'securityhub:GetFindings',
        'securityhub:UpdateFindings',
        'securityhub:BatchUpdateFindings',
      ],
      resources: ['*'],
    }));

    this.incidentResponseFunction.addToRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        's3:PutPublicAccessBlock',
        's3:GetPublicAccessBlock',
      ],
      resources: ['*'],
    }));

    this.incidentResponseFunction.addToRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'ec2:DescribeSecurityGroups',
        'ec2:RevokeSecurityGroupIngress',
      ],
      resources: ['*'],
    }));

    this.incidentResponseFunction.addToRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'iam:UpdateAccountPasswordPolicy',
        'iam:GetAccountPasswordPolicy',
      ],
      resources: ['*'],
    }));

    this.incidentResponseFunction.addToRolePolicy(new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'ssm:CreateOpsItem',
        'ssm:AddTagsToResource',
      ],
      resources: ['*'],
    }));

    this.criticalFindingsTopic.grantPublish(this.incidentResponseFunction);
    this.highFindingsTopic.grantPublish(this.incidentResponseFunction);

    // ========================================
    // 4. EventBridge Rules for Automated Response
    // ========================================
    
    // Rule for CRITICAL findings
    const criticalFindingsRule = new events.Rule(this, 'CriticalFindingsRule', {
      description: 'Trigger automated response for CRITICAL Security Hub findings',
      eventPattern: {
        source: ['aws.securityhub'],
        detailType: ['Security Hub Findings - Imported'],
        detail: {
          findings: {
            Severity: {
              Label: ['CRITICAL'],
            },
            Workflow: {
              Status: ['NEW', 'NOTIFIED'],
            },
          },
        },
      },
    });

    criticalFindingsRule.addTarget(new targets.LambdaFunction(this.incidentResponseFunction));

    // Rule for HIGH findings
    const highFindingsRule = new events.Rule(this, 'HighFindingsRule', {
      description: 'Trigger automated response for HIGH Security Hub findings',
      eventPattern: {
        source: ['aws.securityhub'],
        detailType: ['Security Hub Findings - Imported'],
        detail: {
          findings: {
            Severity: {
              Label: ['HIGH'],
            },
            Workflow: {
              Status: ['NEW', 'NOTIFIED'],
            },
          },
        },
      },
    });

    highFindingsRule.addTarget(new targets.LambdaFunction(this.incidentResponseFunction));

    // ========================================
    // 5. Threat Intelligence Integration
    // ========================================
    
    // Enable AWS GuardDuty integration (if not already enabled)
    // Note: GuardDuty must be enabled separately, but Security Hub will automatically
    // import GuardDuty findings for threat intelligence correlation

    // Enable AWS Inspector integration
    // Note: Inspector must be enabled separately, but Security Hub will automatically
    // import Inspector findings for vulnerability management

    // Enable AWS Macie integration
    // Note: Macie must be enabled separately, but Security Hub will automatically
    // import Macie findings for data security

    // ========================================
    // 6. CloudFormation Outputs
    // ========================================
    
    new cdk.CfnOutput(this, 'SecurityHubArn', {
      value: this.securityHub.attrArn,
      description: 'Security Hub ARN',
      exportName: 'SecurityHubArn',
    });

    new cdk.CfnOutput(this, 'CriticalFindingsTopicArn', {
      value: this.criticalFindingsTopic.topicArn,
      description: 'SNS Topic for Critical Security Findings',
      exportName: 'CriticalFindingsTopicArn',
    });

    new cdk.CfnOutput(this, 'HighFindingsTopicArn', {
      value: this.highFindingsTopic.topicArn,
      description: 'SNS Topic for High Security Findings',
      exportName: 'HighFindingsTopicArn',
    });

    new cdk.CfnOutput(this, 'IncidentResponseFunctionArn', {
      value: this.incidentResponseFunction.functionArn,
      description: 'Automated Incident Response Lambda Function ARN',
      exportName: 'IncidentResponseFunctionArn',
    });

    // Tags
    cdk.Tags.of(this).add('Component', 'Security');
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
    cdk.Tags.of(this).add('Purpose', 'SecurityInsights');
  }
}
