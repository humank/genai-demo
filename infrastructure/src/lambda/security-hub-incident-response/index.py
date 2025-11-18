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
