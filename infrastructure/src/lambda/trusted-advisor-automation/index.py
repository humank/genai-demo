import json
import boto3
import os
from datetime import datetime

support_client = boto3.client('support', region_name='us-east-1')
sns_client = boto3.client('sns')

SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']

def handler(event, context):
    """
    Automated Trusted Advisor check execution and reporting
    
    Checks:
    - Cost Optimization recommendations
    - Security best practices
    - Performance improvements
    - Service limits
    """
    
    try:
        # Get all Trusted Advisor checks
        checks_response = support_client.describe_trusted_advisor_checks(
            language='en'
        )
        
        cost_optimization_checks = [
            check for check in checks_response['checks']
            if check['category'] == 'cost_optimizing'
        ]
        
        recommendations = []
        total_potential_savings = 0
        
        for check in cost_optimization_checks:
            check_id = check['id']
            check_name = check['name']
            
            # Get check result
            result = support_client.describe_trusted_advisor_check_result(
                checkId=check_id,
                language='en'
            )
            
            check_result = result['result']
            
            if check_result['status'] in ['warning', 'error']:
                flagged_resources = check_result.get('flaggedResources', [])
                
                if flagged_resources:
                    estimated_savings = sum(
                        float(resource.get('metadata', [0])[0] or 0)
                        for resource in flagged_resources
                        if resource.get('metadata')
                    )
                    
                    total_potential_savings += estimated_savings
                    
                    recommendations.append({
                        'check_name': check_name,
                        'status': check_result['status'],
                        'resources_flagged': len(flagged_resources),
                        'estimated_monthly_savings': f'${estimated_savings:.2f}',
                        'description': check.get('description', 'No description available')
                    })
        
        # Generate report
        report = {
            'timestamp': datetime.utcnow().isoformat(),
            'total_checks_reviewed': len(cost_optimization_checks),
            'recommendations_count': len(recommendations),
            'total_potential_monthly_savings': f'${total_potential_savings:.2f}',
            'recommendations': recommendations
        }
        
        # Send notification if there are recommendations
        if recommendations:
            message = f"""
Trusted Advisor Cost Optimization Report
Generated: {report['timestamp']}

Total Potential Monthly Savings: {report['total_potential_monthly_savings']}
Number of Recommendations: {report['recommendations_count']}

Top Recommendations:
"""
            for i, rec in enumerate(recommendations[:5], 1):
                message += f"""
{i}. {rec['check_name']}
   Status: {rec['status']}
   Resources: {rec['resources_flagged']}
   Potential Savings: {rec['estimated_monthly_savings']}
   
"""
            
            sns_client.publish(
                TopicArn=SNS_TOPIC_ARN,
                Subject='Trusted Advisor Cost Optimization Report',
                Message=message
            )
        
        return {
            'statusCode': 200,
            'body': json.dumps(report)
        }
        
    except Exception as e:
        error_message = f'Error running Trusted Advisor automation: {str(e)}'
        print(error_message)
        
        sns_client.publish(
            TopicArn=SNS_TOPIC_ARN,
            Subject='Trusted Advisor Automation Error',
            Message=error_message
        )
        
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
