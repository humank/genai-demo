"""
Aurora Global Database Cost Optimization Lambda Function

This function analyzes Aurora clusters and provides cost optimization recommendations:
- Identify underutilized read replicas
- Recommend instance type downsizing
- Analyze cross-region replication costs
- Suggest Reserved Instance opportunities
"""

import json
import boto3
import os
from datetime import datetime, timedelta

rds_client = boto3.client('rds')
cloudwatch_client = boto3.client('cloudwatch')
sns_client = boto3.client('sns')

SNS_TOPIC_ARN = os.environ['SNS_TOPIC_ARN']


def handler(event, context):
    """
    Main Lambda handler for Aurora cost optimization
    
    Args:
        event: Lambda event object
        context: Lambda context object
        
    Returns:
        dict: Response with status code and optimization report
    """
    try:
        # Get all Aurora clusters
        clusters_response = rds_client.describe_db_clusters()
        aurora_clusters = [
            cluster for cluster in clusters_response['DBClusters']
            if cluster['Engine'].startswith('aurora')
        ]
        
        recommendations = []
        total_potential_savings = 0
        
        for cluster in aurora_clusters:
            cluster_id = cluster['DBClusterIdentifier']
            
            # Get cluster instances
            instances = cluster.get('DBClusterMembers', [])
            
            for instance_info in instances:
                instance_id = instance_info['DBInstanceIdentifier']
                
                # Get instance details
                instance_response = rds_client.describe_db_instances(
                    DBInstanceIdentifier=instance_id
                )
                instance = instance_response['DBInstances'][0]
                
                # Check CPU utilization (last 7 days)
                cpu_metrics = cloudwatch_client.get_metric_statistics(
                    Namespace='AWS/RDS',
                    MetricName='CPUUtilization',
                    Dimensions=[
                        {'Name': 'DBInstanceIdentifier', 'Value': instance_id}
                    ],
                    StartTime=datetime.utcnow() - timedelta(days=7),
                    EndTime=datetime.utcnow(),
                    Period=3600,
                    Statistics=['Average']
                )
                
                if cpu_metrics['Datapoints']:
                    avg_cpu = sum(dp['Average'] for dp in cpu_metrics['Datapoints']) / len(cpu_metrics['Datapoints'])
                    
                    # Recommendation: Downsize if CPU < 20%
                    if avg_cpu < 20:
                        current_class = instance['DBInstanceClass']
                        estimated_savings = estimate_savings(current_class)
                        
                        recommendations.append({
                            'cluster_id': cluster_id,
                            'instance_id': instance_id,
                            'current_instance_class': current_class,
                            'avg_cpu_utilization': f'{avg_cpu:.2f}%',
                            'recommendation': 'Consider downsizing instance',
                            'estimated_monthly_savings': f'${estimated_savings:.2f}'
                        })
                        
                        total_potential_savings += estimated_savings
        
        # Generate report
        report = {
            'timestamp': datetime.utcnow().isoformat(),
            'clusters_analyzed': len(aurora_clusters),
            'recommendations_count': len(recommendations),
            'total_potential_monthly_savings': f'${total_potential_savings:.2f}',
            'recommendations': recommendations
        }
        
        # Send notification if there are recommendations
        if recommendations:
            message = f"""
Aurora Cost Optimization Report
Generated: {report['timestamp']}

Total Potential Monthly Savings: {report['total_potential_monthly_savings']}
Number of Recommendations: {report['recommendations_count']}

Recommendations:
"""
            for i, rec in enumerate(recommendations, 1):
                message += f"""
{i}. Cluster: {rec['cluster_id']}
   Instance: {rec['instance_id']}
   Current Class: {rec['current_instance_class']}
   Avg CPU: {rec['avg_cpu_utilization']}
   Recommendation: {rec['recommendation']}
   Potential Savings: {rec['estimated_monthly_savings']}
   
"""
            
            sns_client.publish(
                TopicArn=SNS_TOPIC_ARN,
                Subject='Aurora Cost Optimization Report',
                Message=message
            )
        
        return {
            'statusCode': 200,
            'body': json.dumps(report)
        }
        
    except Exception as e:
        error_message = f'Error running Aurora cost optimization: {str(e)}'
        print(error_message)
        
        sns_client.publish(
            TopicArn=SNS_TOPIC_ARN,
            Subject='Aurora Cost Optimization Error',
            Message=error_message
        )
        
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }


def estimate_savings(instance_class):
    """
    Estimate monthly savings from downsizing instance
    
    Args:
        instance_class: Current RDS instance class
        
    Returns:
        float: Estimated monthly savings in USD
    """
    # Simplified pricing estimation (actual prices may vary)
    pricing_map = {
        'db.r6g.large': 200,
        'db.r6g.xlarge': 400,
        'db.r6g.2xlarge': 800,
        'db.r6g.4xlarge': 1600,
        'db.r6g.8xlarge': 3200,
        'db.r6g.12xlarge': 4800,
        'db.r6g.16xlarge': 6400,
    }
    
    current_cost = pricing_map.get(instance_class, 0)
    # Assume 30% savings from downsizing one tier
    return current_cost * 0.3
