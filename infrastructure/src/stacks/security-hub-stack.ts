import * as cdk from 'aws-cdk-lib';
import * as securityhub from 'aws-cdk-lib/aws-securityhub';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as subscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
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
      code: lambda.Code.fromInline(`
import json
import boto3
import os
from datetime import datetime

securityhub = boto3.client('securityhub')
sns = boto3.client('sns')
ssm = boto3.client('ssm')

CRITICAL_TOPIC_ARN = os.environ['CRITICAL_TOPIC_ARN']
HIGH_TOPIC_ARN = os.environ['HIGH_TOPIC_ARN']

def handler(event, context):
    """
    Automated incident response for Security Hub findings
    
    Handles:
    - Finding classification and prioritization
    - Automated remediation for known issues
    - Escalation to security team
    - Incident tracking and logging
    """
    
    print(f"Processing Security Hub finding: {json.dumps(event)}")
    
    # Extract finding details
    detail = event.get('detail', {})
    findings = detail.get('findings', [])
    
    for finding in findings:
        finding_id = finding.get('Id')
        severity = finding.get('Severity', {}).get('Label', 'INFORMATIONAL')
        title = finding.get('Title', 'Unknown')
        description = finding.get('Description', '')
        resource_type = finding.get('Resources', [{}])[0].get('Type', 'Unknown')
        resource_id = finding.get('Resources', [{}])[0].get('Id', 'Unknown')
        compliance_status = finding.get('Compliance', {}).get('Status', 'UNKNOWN')
        
        print(f"Processing finding: {finding_id}")
        print(f"Severity: {severity}, Title: {title}")
        print(f"Resource: {resource_type} - {resource_id}")
        
        # Automated remediation based on finding type
        remediation_action = None
        
        # Example: Automated remediation for common security issues
        if 'S3' in resource_type and 'public' in title.lower():
            remediation_action = remediate_s3_public_access(resource_id)
        elif 'SecurityGroup' in resource_type and 'unrestricted' in title.lower():
            remediation_action = remediate_security_group(resource_id)
        elif 'IAM' in resource_type and 'password policy' in title.lower():
            remediation_action = remediate_iam_password_policy()
        
        # Create incident ticket for CRITICAL and HIGH findings
        if severity in ['CRITICAL', 'HIGH']:
            create_incident_ticket(finding_id, severity, title, description, resource_id)
        
        # Send notifications
        if severity == 'CRITICAL':
            send_notification(CRITICAL_TOPIC_ARN, finding_id, severity, title, description, 
                            resource_id, remediation_action)
        elif severity == 'HIGH':
            send_notification(HIGH_TOPIC_ARN, finding_id, severity, title, description, 
                            resource_id, remediation_action)
        
        # Update finding with remediation status
        if remediation_action:
            update_finding_note(finding_id, remediation_action)
    
    return {
        'statusCode': 200,
        'body': json.dumps(f'Processed {len(findings)} findings')
    }

def remediate_s3_public_access(bucket_arn):
    """Remediate S3 bucket public access"""
    try:
        s3 = boto3.client('s3')
        bucket_name = bucket_arn.split(':')[-1]
        
        # Block public access
        s3.put_public_access_block(
            Bucket=bucket_name,
            PublicAccessBlockConfiguration={
                'BlockPublicAcls': True,
                'IgnorePublicAcls': True,
                'BlockPublicPolicy': True,
                'RestrictPublicBuckets': True
            }
        )
        
        return f"Automatically blocked public access for S3 bucket: {bucket_name}"
    except Exception as e:
        return f"Failed to remediate S3 bucket: {str(e)}"

def remediate_security_group(sg_id):
    """Remediate overly permissive security group"""
    try:
        # Extract security group ID from ARN
        sg_id_clean = sg_id.split('/')[-1]
        
        # Log the issue for manual review (automated removal could break services)
        return f"Security group {sg_id_clean} flagged for manual review - overly permissive rules detected"
    except Exception as e:
        return f"Failed to analyze security group: {str(e)}"

def remediate_iam_password_policy():
    """Remediate IAM password policy"""
    try:
        iam = boto3.client('iam')
        
        # Update password policy to meet security standards
        iam.update_account_password_policy(
            MinimumPasswordLength=14,
            RequireSymbols=True,
            RequireNumbers=True,
            RequireUppercaseCharacters=True,
            RequireLowercaseCharacters=True,
            AllowUsersToChangePassword=True,
            ExpirePasswords=True,
            MaxPasswordAge=90,
            PasswordReusePrevention=24
        )
        
        return "Automatically updated IAM password policy to meet security standards"
    except Exception as e:
        return f"Failed to update IAM password policy: {str(e)}"

def create_incident_ticket(finding_id, severity, title, description, resource_id):
    """Create incident ticket in Systems Manager OpsCenter"""
    try:
        ssm.create_ops_item(
            Title=f"[{severity}] {title}",
            Description=f"""
Security Hub Finding: {finding_id}

Severity: {severity}
Resource: {resource_id}

Description:
{description}

Action Required:
- Review the security finding
- Implement remediation steps
- Update finding status in Security Hub
            """,
            Source='SecurityHub',
            Priority=1 if severity == 'CRITICAL' else 2,
            Tags=[
                {'Key': 'Severity', 'Value': severity},
                {'Key': 'Source', 'Value': 'SecurityHub'},
                {'Key': 'FindingId', 'Value': finding_id}
            ]
        )
        print(f"Created incident ticket for finding: {finding_id}")
    except Exception as e:
        print(f"Failed to create incident ticket: {str(e)}")

def send_notification(topic_arn, finding_id, severity, title, description, resource_id, remediation):
    """Send SNS notification for security finding"""
    try:
        message = f"""
🚨 Security Hub Alert - {severity} Severity

Finding ID: {finding_id}
Title: {title}
Resource: {resource_id}

Description:
{description}

Automated Remediation:
{remediation if remediation else 'No automated remediation available - manual review required'}

Timestamp: {datetime.utcnow().isoformat()}

Please review this finding in AWS Security Hub console.
        """
        
        sns.publish(
            TopicArn=topic_arn,
            Subject=f'[{severity}] Security Hub Alert: {title[:50]}',
            Message=message
        )
        print(f"Sent notification for finding: {finding_id}")
    except Exception as e:
        print(f"Failed to send notification: {str(e)}")

def update_finding_note(finding_id, note):
    """Update Security Hub finding with remediation note"""
    try:
        securityhub.update_findings(
            Filters={'Id': [{'Value': finding_id, 'Comparison': 'EQUALS'}]},
            Note={
                'Text': f"Automated Response: {note}",
                'UpdatedBy': 'AutomatedIncidentResponse'
            }
        )
        print(f"Updated finding note: {finding_id}")
    except Exception as e:
        print(f"Failed to update finding note: {str(e)}")
`),
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
