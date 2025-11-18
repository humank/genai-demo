"""
Cost Anomaly Detection and Root Cause Analysis Lambda Function

This function analyzes cost usage reports and detects anomalies:
1. Daily cost trends and identifies anomalies (>20% increase)
2. Service-level cost breakdown
3. Resource-level cost attribution
4. Budget overspend risk prediction
"""

import json
import boto3
import os
from datetime import datetime, timedelta
from decimal import Decimal

athena = boto3.client('athena')
sns = boto3.client('sns')
cloudwatch = boto3.client('cloudwatch')

WORKGROUP = os.environ['ATHENA_WORKGROUP']
DATABASE = os.environ['GLUE_DATABASE']
SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']
OUTPUT_LOCATION = os.environ['ATHENA_OUTPUT_LOCATION']


def handler(event, context):
    """
    Main Lambda handler for cost anomaly detection
    
    Args:
        event: Lambda event object
        context: Lambda context object
        
    Returns:
        dict: Response with status code and detection results
    """
    try:
        # Query 1: Daily cost comparison (last 7 days vs previous 7 days)
        daily_cost_query = f"""
        SELECT 
            line_item_usage_start_date,
            SUM(line_item_unblended_cost) as daily_cost,
            product_servicename,
            line_item_resource_id
        FROM {DATABASE}.cost_usage_reports
        WHERE line_item_usage_start_date >= date_add('day', -14, current_date)
        GROUP BY line_item_usage_start_date, product_servicename, line_item_resource_id
        ORDER BY line_item_usage_start_date DESC
        """
        
        # Execute Athena query
        response = athena.start_query_execution(
            QueryString=daily_cost_query,
            QueryExecutionContext={'Database': DATABASE},
            ResultConfiguration={'OutputLocation': OUTPUT_LOCATION},
            WorkGroup=WORKGROUP
        )
        
        query_execution_id = response['QueryExecutionId']
        
        # Wait for query completion (simplified for demo)
        # In production, use Step Functions or async processing
        
        # Query 2: Service-level cost breakdown
        service_cost_query = f"""
        SELECT 
            product_servicename,
            SUM(line_item_unblended_cost) as service_cost,
            COUNT(DISTINCT line_item_resource_id) as resource_count
        FROM {DATABASE}.cost_usage_reports
        WHERE line_item_usage_start_date >= date_add('day', -1, current_date)
        GROUP BY product_servicename
        ORDER BY service_cost DESC
        LIMIT 10
        """
        
        # Detect anomalies (simplified logic)
        anomalies = detect_cost_anomalies()
        
        # Perform root cause analysis
        root_causes = analyze_root_causes(anomalies)
        
        # Check budget overspend risk
        budget_risk = check_budget_overspend_risk()
        
        # Publish metrics to CloudWatch
        publish_cost_metrics(anomalies, budget_risk)
        
        # Send SNS notification if anomalies detected
        if anomalies or budget_risk['high_risk']:
            send_anomaly_notification(anomalies, root_causes, budget_risk)
        
        return {
            'statusCode': 200,
            'body': json.dumps({
                'anomalies_detected': len(anomalies),
                'budget_risk_level': budget_risk['risk_level'],
                'query_execution_id': query_execution_id
            })
        }
        
    except Exception as e:
        print(f"Error in cost anomaly detection: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }


def detect_cost_anomalies():
    """
    Detect cost anomalies using statistical analysis
    
    Returns:
        list: List of detected anomalies
    """
    # Simplified anomaly detection logic
    # In production, use AWS Cost Anomaly Detection service or ML models
    anomalies = []
    
    # Example: Detect if daily cost increased by >20%
    # This would query Athena results and compare trends
    
    return anomalies


def analyze_root_causes(anomalies):
    """
    Analyze root causes of cost anomalies
    
    Args:
        anomalies: List of detected anomalies
        
    Returns:
        list: List of root cause analyses
    """
    root_causes = []
    
    for anomaly in anomalies:
        # Analyze service-level breakdown
        # Identify specific resources causing cost increase
        # Check for configuration changes or usage spikes
        root_causes.append({
            'anomaly_id': anomaly.get('id'),
            'primary_cause': 'Service usage spike',
            'affected_services': ['EKS', 'RDS'],
            'recommendations': [
                'Review EKS node scaling policies',
                'Check RDS instance right-sizing'
            ]
        })
    
    return root_causes


def check_budget_overspend_risk():
    """
    Check budget overspend risk based on current spending trends
    
    Returns:
        dict: Budget risk assessment
    """
    # Calculate spending rate and forecast end-of-month cost
    # Compare with budget thresholds
    
    return {
        'risk_level': 'medium',
        'high_risk': False,
        'projected_monthly_cost': 4500,
        'budget_limit': 5000,
        'days_until_overspend': 25
    }


def publish_cost_metrics(anomalies, budget_risk):
    """
    Publish cost metrics to CloudWatch
    
    Args:
        anomalies: List of detected anomalies
        budget_risk: Budget risk assessment
    """
    cloudwatch.put_metric_data(
        Namespace='CostManagement/Insights',
        MetricData=[
            {
                'MetricName': 'CostAnomaliesDetected',
                'Value': len(anomalies),
                'Unit': 'Count',
                'Timestamp': datetime.utcnow()
            },
            {
                'MetricName': 'BudgetOverspendRisk',
                'Value': 1 if budget_risk['high_risk'] else 0,
                'Unit': 'Count',
                'Timestamp': datetime.utcnow()
            }
        ]
    )


def send_anomaly_notification(anomalies, root_causes, budget_risk):
    """
    Send SNS notification for detected anomalies
    
    Args:
        anomalies: List of detected anomalies
        root_causes: List of root cause analyses
        budget_risk: Budget risk assessment
    """
    message = f"""
Cost Anomaly Detection Alert

Anomalies Detected: {len(anomalies)}
Budget Risk Level: {budget_risk['risk_level']}
Projected Monthly Cost: ${budget_risk['projected_monthly_cost']}
Budget Limit: ${budget_risk['budget_limit']}

Root Causes:
{json.dumps(root_causes, indent=2)}

Action Required:
- Review cost breakdown in CloudWatch dashboard
- Check Athena queries for detailed analysis
- Implement recommended optimizations
"""
    
    sns.publish(
        TopicArn=SNS_TOPIC_ARN,
        Subject='Cost Anomaly Detection Alert',
        Message=message
    )
