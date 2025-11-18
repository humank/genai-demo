import json
import boto3
import os
from datetime import datetime
from typing import Dict, List, Any

wellarchitected = boto3.client('wellarchitected')
s3 = boto3.client('s3')
sns = boto3.client('sns')

WORKLOAD_ID = os.environ['WORKLOAD_ID']
REPORT_BUCKET = os.environ['REPORT_BUCKET']
NOTIFICATION_TOPIC_ARN = os.environ['NOTIFICATION_TOPIC_ARN']
ENVIRONMENT = os.environ['ENVIRONMENT']

# Risk priority mapping
RISK_PRIORITY = {
    'UNANSWERED': 1,
    'HIGH': 2,
    'MEDIUM': 3,
    'NONE': 4,
    'NOT_APPLICABLE': 5
}

def handler(event, context):
    """
    Main handler for Well-Architected assessment automation
    """
    try:
        action = event.get('action', 'assess')
        
        if action == 'create_milestone':
            return create_milestone()
        else:
            return perform_assessment()
            
    except Exception as e:
        print(f"Error in assessment: {str(e)}")
        send_notification(
            subject=f"Well-Architected Assessment Failed - {ENVIRONMENT}",
            message=f"Assessment failed with error: {str(e)}"
        )
        raise

def perform_assessment() -> Dict[str, Any]:
    """
    Perform comprehensive Well-Architected assessment
    """
    print(f"Starting assessment for workload: {WORKLOAD_ID}")
    
    # Get workload details
    workload = wellarchitected.get_workload(WorkloadId=WORKLOAD_ID)
    
    # Get all lens reviews
    lens_reviews = get_lens_reviews()
    
    # Analyze each lens
    assessment_results = {
        'workload_id': WORKLOAD_ID,
        'workload_name': workload['Workload']['WorkloadName'],
        'environment': ENVIRONMENT,
        'assessment_date': datetime.utcnow().isoformat(),
        'lenses': []
    }
    
    for lens in lens_reviews:
        lens_alias = lens['LensAlias']
        print(f"Analyzing lens: {lens_alias}")
        
        # Get detailed lens review
        lens_review = wellarchitected.get_lens_review(
            WorkloadId=WORKLOAD_ID,
            LensAlias=lens_alias
        )
        
        # Get all improvements for this lens
        improvements = get_lens_improvements(lens_alias)
        
        # Analyze pillar risks
        pillar_analysis = analyze_pillars(lens_alias)
        
        lens_result = {
            'lens_alias': lens_alias,
            'lens_name': lens['LensName'],
            'lens_version': lens.get('LensVersion', 'N/A'),
            'risk_counts': lens['RiskCounts'],
            'pillar_analysis': pillar_analysis,
            'improvements': improvements,
            'improvement_plan': generate_improvement_plan(improvements)
        }
        
        assessment_results['lenses'].append(lens_result)
    
    # Generate comprehensive report
    report = generate_assessment_report(assessment_results)
    
    # Save report to S3
    report_key = save_report_to_s3(report, assessment_results)
    
    # Send notification
    send_assessment_notification(assessment_results, report_key)
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'message': 'Assessment completed successfully',
            'report_key': report_key,
            'summary': get_assessment_summary(assessment_results)
        })
    }

def get_lens_reviews() -> List[Dict]:
    """
    Get all lens reviews for the workload
    """
    response = wellarchitected.list_lens_reviews(WorkloadId=WORKLOAD_ID)
    return response.get('LensReviewSummaries', [])

def get_lens_improvements(lens_alias: str) -> List[Dict]:
    """
    Get prioritized improvements for a lens
    """
    improvements = []
    next_token = None
    
    while True:
        params = {
            'WorkloadId': WORKLOAD_ID,
            'LensAlias': lens_alias
        }
        if next_token:
            params['NextToken'] = next_token
            
        response = wellarchitected.list_lens_review_improvements(**params)
        
        for item in response.get('ImprovementSummaries', []):
            improvements.append({
                'pillar_id': item['PillarId'],
                'question_id': item['QuestionId'],
                'question_title': item['QuestionTitle'],
                'risk': item['Risk'],
                'improvement_plan_url': item.get('ImprovementPlanUrl', ''),
                'priority': RISK_PRIORITY.get(item['Risk'], 99)
            })
        
        next_token = response.get('NextToken')
        if not next_token:
            break
    
    # Sort by priority (highest risk first)
    improvements.sort(key=lambda x: x['priority'])
    
    return improvements

def analyze_pillars(lens_alias: str) -> Dict[str, Any]:
    """
    Analyze each pillar in detail
    """
    pillar_analysis = {}
    
    # Get lens review
    lens_review = wellarchitected.get_lens_review(
        WorkloadId=WORKLOAD_ID,
        LensAlias=lens_alias
    )
    
    for pillar in lens_review['LensReview'].get('PillarReviewSummaries', []):
        pillar_id = pillar['PillarId']
        
        pillar_analysis[pillar_id] = {
            'pillar_name': pillar['PillarName'],
            'risk_counts': pillar['RiskCounts'],
            'notes': pillar.get('Notes', ''),
            'improvement_status': get_pillar_improvement_status(pillar['RiskCounts'])
        }
    
    return pillar_analysis

def get_pillar_improvement_status(risk_counts: Dict[str, int]) -> str:
    """
    Determine improvement status based on risk counts
    """
    high_risks = risk_counts.get('HIGH', 0)
    medium_risks = risk_counts.get('MEDIUM', 0)
    unanswered = risk_counts.get('UNANSWERED', 0)
    
    if high_risks > 0 or unanswered > 3:
        return 'CRITICAL'
    elif medium_risks > 5:
        return 'NEEDS_ATTENTION'
    elif medium_risks > 0:
        return 'MONITOR'
    else:
        return 'GOOD'

def generate_improvement_plan(improvements: List[Dict]) -> List[Dict]:
    """
    Generate prioritized improvement action plan
    """
    action_plan = []
    
    # Group improvements by priority
    critical = [i for i in improvements if i['risk'] in ['UNANSWERED', 'HIGH']]
    medium = [i for i in improvements if i['risk'] == 'MEDIUM']
    
    # Phase 1: Critical improvements (0-30 days)
    if critical:
        action_plan.append({
            'phase': 1,
            'timeline': '0-30 days',
            'priority': 'CRITICAL',
            'actions': critical[:5],  # Top 5 critical items
            'description': 'Address high-risk items and unanswered questions immediately'
        })
    
    # Phase 2: Medium priority improvements (30-90 days)
    if medium:
        action_plan.append({
            'phase': 2,
            'timeline': '30-90 days',
            'priority': 'MEDIUM',
            'actions': medium[:10],  # Top 10 medium items
            'description': 'Implement medium-risk improvements systematically'
        })
    
    # Phase 3: Continuous improvement (90+ days)
    remaining = improvements[15:]  # Remaining items
    if remaining:
        action_plan.append({
            'phase': 3,
            'timeline': '90+ days',
            'priority': 'LOW',
            'actions': remaining,
            'description': 'Ongoing optimization and best practice adoption'
        })
    
    return action_plan

def generate_assessment_report(results: Dict[str, Any]) -> str:
    """
    Generate comprehensive assessment report in Markdown format
    """
    report = f"""# AWS Well-Architected Assessment Report

**Workload**: {results['workload_name']}
**Environment**: {results['environment']}
**Assessment Date**: {results['assessment_date']}
**Workload ID**: {results['workload_id']}

---

## Executive Summary

"""
    
    # Add summary for each lens
    for lens in results['lenses']:
        report += f"### {lens['lens_name']}\n\n"
        report += f"**Version**: {lens['lens_version']}\n\n"
        
        # Risk summary
        risk_counts = lens['risk_counts']
        report += "**Risk Summary**:\n"
        report += f"- 🔴 High Risk: {risk_counts.get('HIGH', 0)}\n"
        report += f"- 🟡 Medium Risk: {risk_counts.get('MEDIUM', 0)}\n"
        report += f"- 🟢 No Risk: {risk_counts.get('NONE', 0)}\n"
        report += f"- ⚪ Unanswered: {risk_counts.get('UNANSWERED', 0)}\n\n"
        
        # Pillar analysis
        report += "**Pillar Analysis**:\n\n"
        for pillar_id, pillar_data in lens['pillar_analysis'].items():
            status_emoji = {
                'CRITICAL': '🔴',
                'NEEDS_ATTENTION': '🟡',
                'MONITOR': '🟠',
                'GOOD': '🟢'
            }.get(pillar_data['improvement_status'], '⚪')
            
            report += f"{status_emoji} **{pillar_data['pillar_name']}**: {pillar_data['improvement_status']}\n"
        
        report += "\n---\n\n"
    
    # Add improvement plan
    report += "## Prioritized Improvement Plan\n\n"
    
    for lens in results['lenses']:
        if lens['improvement_plan']:
            report += f"### {lens['lens_name']} Improvements\n\n"
            
            for phase in lens['improvement_plan']:
                report += f"#### Phase {phase['phase']}: {phase['timeline']} ({phase['priority']} Priority)\n\n"
                report += f"{phase['description']}\n\n"
                
                if phase['actions']:
                    report += "**Actions**:\n\n"
                    for i, action in enumerate(phase['actions'][:5], 1):  # Limit to 5 per phase in report
                        report += f"{i}. **{action['question_title']}** ({action['risk']} risk)\n"
                        if action['improvement_plan_url']:
                            report += f"   - [Improvement Guide]({action['improvement_plan_url']})\n"
                    report += "\n"
    
    report += "\n---\n\n"
    report += "## Next Steps\n\n"
    report += "1. Review and prioritize the improvement actions\n"
    report += "2. Assign owners for each improvement item\n"
    report += "3. Create tracking tickets in your project management system\n"
    report += "4. Schedule follow-up assessment in 30 days\n"
    report += "5. Update workload answers as improvements are implemented\n"
    
    return report

def save_report_to_s3(report: str, results: Dict[str, Any]) -> str:
    """
    Save assessment report to S3
    """
    timestamp = datetime.utcnow().strftime('%Y%m%d-%H%M%S')
    report_key = f"assessments/{ENVIRONMENT}/{timestamp}/assessment-report.md"
    
    # Save Markdown report
    s3.put_object(
        Bucket=REPORT_BUCKET,
        Key=report_key,
        Body=report.encode('utf-8'),
        ContentType='text/markdown',
        ServerSideEncryption='AES256'
    )
    
    # Save JSON data
    json_key = f"assessments/{ENVIRONMENT}/{timestamp}/assessment-data.json"
    s3.put_object(
        Bucket=REPORT_BUCKET,
        Key=json_key,
        Body=json.dumps(results, indent=2).encode('utf-8'),
        ContentType='application/json',
        ServerSideEncryption='AES256'
    )
    
    print(f"Report saved to s3://{REPORT_BUCKET}/{report_key}")
    
    return report_key

def get_assessment_summary(results: Dict[str, Any]) -> Dict[str, Any]:
    """
    Generate assessment summary
    """
    total_high = 0
    total_medium = 0
    total_unanswered = 0
    total_improvements = 0
    
    for lens in results['lenses']:
        risk_counts = lens['risk_counts']
        total_high += risk_counts.get('HIGH', 0)
        total_medium += risk_counts.get('MEDIUM', 0)
        total_unanswered += risk_counts.get('UNANSWERED', 0)
        total_improvements += len(lens['improvements'])
    
    return {
        'total_high_risks': total_high,
        'total_medium_risks': total_medium,
        'total_unanswered': total_unanswered,
        'total_improvements': total_improvements,
        'overall_status': 'CRITICAL' if total_high > 0 or total_unanswered > 5 else 'NEEDS_ATTENTION' if total_medium > 10 else 'GOOD'
    }

def send_assessment_notification(results: Dict[str, Any], report_key: str):
    """
    Send assessment notification via SNS
    """
    summary = get_assessment_summary(results)
    
    subject = f"Well-Architected Assessment Complete - {ENVIRONMENT} - {summary['overall_status']}"
    
    message = f"""
Well-Architected Assessment Completed

Workload: {results['workload_name']}
Environment: {ENVIRONMENT}
Assessment Date: {results['assessment_date']}

Summary:
- High Risk Items: {summary['total_high_risks']}
- Medium Risk Items: {summary['total_medium_risks']}
- Unanswered Questions: {summary['total_unanswered']}
- Total Improvements Identified: {summary['total_improvements']}

Overall Status: {summary['overall_status']}

Report Location: s3://{REPORT_BUCKET}/{report_key}

Please review the detailed report and prioritize improvement actions.
"""
    
    send_notification(subject, message)

def create_milestone() -> Dict[str, Any]:
    """
    Create a milestone for the workload
    """
    print(f"Creating milestone for workload: {WORKLOAD_ID}")
    
    milestone_name = f"Monthly Review - {datetime.utcnow().strftime('%Y-%m')}"
    
    response = wellarchitected.create_milestone(
        WorkloadId=WORKLOAD_ID,
        MilestoneName=milestone_name
    )
    
    send_notification(
        subject=f"Well-Architected Milestone Created - {ENVIRONMENT}",
        message=f"Milestone '{milestone_name}' created successfully for workload {WORKLOAD_ID}"
    )
    
    return {
        'statusCode': 200,
        'body': json.dumps({
            'message': 'Milestone created successfully',
            'milestone_number': response['MilestoneNumber']
        })
    }

def send_notification(subject: str, message: str):
    """
    Send SNS notification
    """
    sns.publish(
        TopicArn=NOTIFICATION_TOPIC_ARN,
        Subject=subject,
        Message=message
    )
    print(f"Notification sent: {subject}")
