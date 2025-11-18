import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import { Construct } from 'constructs';

export interface LambdaInsightsMonitoringProps {
    environment: string;
    dashboard: cloudwatch.Dashboard;
    lambdaFunctions?: lambda.IFunction[];
}

/**
 * Lambda Insights Monitoring Construct
 * Requirements: 13.7, 13.8, 13.9
 * 
 * Provides:
 * - Execution metrics collection (13.7)
 * - Cold start pattern analysis (13.8)
 * - Cost optimization recommendations (13.9)
 */
export class LambdaInsightsMonitoring extends Construct {
    public readonly insightsLayer: lambda.ILayerVersion;
    public readonly coldStartAnalysisFunction: lambda.Function;
    public readonly costOptimizationFunction: lambda.Function;

    constructor(scope: Construct, id: string, props: LambdaInsightsMonitoringProps) {
        super(scope, id);

        const { environment, dashboard } = props;

        // Create Lambda Insights Layer reference
        this.insightsLayer = this.createInsightsLayer();

        // Add Lambda Insights dashboard widgets
        this.addLambdaInsightsDashboardWidgets(dashboard, environment);

        // Create cold start analysis function
        this.coldStartAnalysisFunction = this.createColdStartAnalysisFunction(environment);

        // Create cost optimization function
        this.costOptimizationFunction = this.createCostOptimizationFunction(environment);

        // Create CloudWatch alarms for Lambda metrics
        this.createLambdaInsightsAlarms(environment, dashboard);
    }

    /**
     * Create Lambda Insights Layer reference
     * Lambda Insights provides enhanced monitoring for Lambda functions
     */
    private createInsightsLayer(): lambda.ILayerVersion {
        // Lambda Insights extension layer ARN for ap-northeast-1 (Taiwan)
        // See: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Lambda-Insights-extension-versionsx86-64.html
        const insightsLayerArn = `arn:aws:lambda:${cdk.Stack.of(this).region}:580247275435:layer:LambdaInsightsExtension:38`;
        
        return lambda.LayerVersion.fromLayerVersionArn(
            this,
            'LambdaInsightsLayer',
            insightsLayerArn
        );
    }

    /**
     * Add Lambda Insights dashboard widgets
     * Requirement 13.7: Collect detailed execution metrics and memory usage
     */
    private addLambdaInsightsDashboardWidgets(dashboard: cloudwatch.Dashboard, environment: string): void {
        // Lambda Insights Overview Widget
        dashboard.addWidgets(
            new cloudwatch.TextWidget({
                markdown: `## Lambda Insights - Intelligent Monitoring

**Lambda Insights** provides enhanced monitoring for AWS Lambda functions:

### Key Features
- **Execution Metrics**: Duration, memory usage, cold starts
- **Cold Start Analysis**: Pattern detection and optimization
- **Cost Optimization**: Memory and timeout recommendations
- **Performance Insights**: Bottleneck identification

### Monitored Functions
- Deadlock Analysis Function
- Container Restart Analysis Function
- Slow Query Analysis Function
- Cross-Region Security Monitor
- Traffic Analysis Function
- Encryption Enforcement Function

[Open Lambda Insights Console](https://console.aws.amazon.com/cloudwatch/home?region=${cdk.Stack.of(this).region}#lambda-insights:functions)`,
                width: 24,
                height: 6,
            })
        );

        // Lambda Execution Metrics Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Lambda - Execution Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Lambda',
                        metricName: 'Duration',
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Avg Duration (ms)',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/Lambda',
                        metricName: 'Duration',
                        statistic: 'p99',
                        period: cdk.Duration.minutes(5),
                        label: 'P99 Duration (ms)',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'AWS/Lambda',
                        metricName: 'Invocations',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Invocations',
                    }),
                    new cloudwatch.Metric({
                        namespace: 'AWS/Lambda',
                        metricName: 'Errors',
                        statistic: 'Sum',
                        period: cdk.Duration.minutes(5),
                        label: 'Errors',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Lambda Memory and Cold Start Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Lambda - Memory Usage & Cold Starts',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'LambdaInsights',
                        metricName: 'memory_utilization',
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Memory Utilization (%)',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'LambdaInsights',
                        metricName: 'init_duration',
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Cold Start Duration (ms)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );

        // Lambda Cost Metrics Widget
        dashboard.addWidgets(
            new cloudwatch.GraphWidget({
                title: 'Lambda - Cost Metrics',
                left: [
                    new cloudwatch.Metric({
                        namespace: 'LambdaInsights',
                        metricName: 'total_memory',
                        statistic: 'Average',
                        period: cdk.Duration.minutes(5),
                        label: 'Allocated Memory (MB)',
                    }),
                ],
                right: [
                    new cloudwatch.Metric({
                        namespace: 'LambdaInsights',
                        metricName: 'used_memory_max',
                        statistic: 'Maximum',
                        period: cdk.Duration.minutes(5),
                        label: 'Max Used Memory (MB)',
                    }),
                ],
                width: 12,
                height: 6,
            })
        );
    }

    /**
     * Create cold start analysis function
     * Requirement 13.8: Analyze cold start patterns and provide optimization suggestions
     */
    private createColdStartAnalysisFunction(environment: string): lambda.Function {
        const coldStartFunction = new lambda.Function(this, 'ColdStartAnalysisFunction', {
            functionName: `${environment}-lambda-cold-start-analysis`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta
from collections import defaultdict

cloudwatch = boto3.client('cloudwatch')
lambda_client = boto3.client('lambda')

def handler(event, context):
    """
    Analyze Lambda cold start patterns and provide optimization recommendations
    """
    environment = os.environ.get('ENVIRONMENT', 'development')
    
    try:
        # Get all Lambda functions in the account
        paginator = lambda_client.get_paginator('list_functions')
        functions = []
        
        for page in paginator.paginate():
            functions.extend(page['Functions'])
        
        # Filter functions related to genai-demo
        genai_functions = [f for f in functions if 'genai-demo' in f['FunctionName'].lower() or environment in f['FunctionName'].lower()]
        
        cold_start_analysis = {
            'timestamp': datetime.now().isoformat(),
            'environment': environment,
            'total_functions': len(genai_functions),
            'functions_analyzed': [],
            'recommendations': []
        }
        
        for func in genai_functions:
            function_name = func['FunctionName']
            
            # Get cold start metrics from CloudWatch
            cold_start_metrics = get_cold_start_metrics(function_name)
            
            if cold_start_metrics:
                analysis = analyze_cold_start_pattern(function_name, cold_start_metrics, func)
                cold_start_analysis['functions_analyzed'].append(analysis)
                
                # Generate recommendations
                recommendations = generate_cold_start_recommendations(analysis)
                cold_start_analysis['recommendations'].extend(recommendations)
        
        # Publish aggregated metrics
        publish_cold_start_metrics(cold_start_analysis)
        
        print(json.dumps(cold_start_analysis, indent=2))
        
        return {
            'statusCode': 200,
            'body': json.dumps(cold_start_analysis)
        }
        
    except Exception as e:
        print(f'Error analyzing cold starts: {str(e)}')
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def get_cold_start_metrics(function_name):
    """Get cold start metrics for a Lambda function"""
    try:
        end_time = datetime.now()
        start_time = end_time - timedelta(hours=24)
        
        # Get init_duration metric (cold start indicator)
        response = cloudwatch.get_metric_statistics(
            Namespace='LambdaInsights',
            MetricName='init_duration',
            Dimensions=[{'Name': 'function_name', 'Value': function_name}],
            StartTime=start_time,
            EndTime=end_time,
            Period=3600,  # 1 hour
            Statistics=['Average', 'Maximum', 'SampleCount']
        )
        
        return response.get('Datapoints', [])
    except Exception as e:
        print(f'Error getting metrics for {function_name}: {str(e)}')
        return []

def analyze_cold_start_pattern(function_name, metrics, function_config):
    """Analyze cold start patterns for a function"""
    if not metrics:
        return {
            'function_name': function_name,
            'cold_start_count': 0,
            'avg_cold_start_duration': 0,
            'max_cold_start_duration': 0,
            'pattern': 'NO_DATA'
        }
    
    total_cold_starts = sum(m['SampleCount'] for m in metrics)
    avg_duration = sum(m['Average'] * m['SampleCount'] for m in metrics) / total_cold_starts if total_cold_starts > 0 else 0
    max_duration = max((m['Maximum'] for m in metrics), default=0)
    
    # Determine pattern
    pattern = 'NORMAL'
    if total_cold_starts > 100:
        pattern = 'HIGH_FREQUENCY'
    elif avg_duration > 3000:
        pattern = 'SLOW_INITIALIZATION'
    elif max_duration > 10000:
        pattern = 'VERY_SLOW_INITIALIZATION'
    
    return {
        'function_name': function_name,
        'cold_start_count': int(total_cold_starts),
        'avg_cold_start_duration': round(avg_duration, 2),
        'max_cold_start_duration': round(max_duration, 2),
        'pattern': pattern,
        'memory_size': function_config.get('MemorySize', 0),
        'runtime': function_config.get('Runtime', 'unknown'),
        'timeout': function_config.get('Timeout', 0)
    }

def generate_cold_start_recommendations(analysis):
    """Generate optimization recommendations based on cold start analysis"""
    recommendations = []
    function_name = analysis['function_name']
    
    if analysis['pattern'] == 'HIGH_FREQUENCY':
        recommendations.append({
            'function': function_name,
            'priority': 'HIGH',
            'category': 'Cold Start Frequency',
            'issue': f"High cold start frequency: {analysis['cold_start_count']} in 24 hours",
            'recommendation': 'Consider using Provisioned Concurrency to keep functions warm',
            'action': 'Configure Provisioned Concurrency with 1-2 instances for consistent performance',
            'estimated_cost_impact': 'Medium - Provisioned Concurrency adds cost but improves latency'
        })
    
    if analysis['pattern'] in ['SLOW_INITIALIZATION', 'VERY_SLOW_INITIALIZATION']:
        recommendations.append({
            'function': function_name,
            'priority': 'HIGH' if analysis['pattern'] == 'VERY_SLOW_INITIALIZATION' else 'MEDIUM',
            'category': 'Cold Start Duration',
            'issue': f"Slow cold start: avg {analysis['avg_cold_start_duration']}ms, max {analysis['max_cold_start_duration']}ms",
            'recommendation': 'Optimize initialization code and reduce package size',
            'action': 'Move initialization outside handler, use Lambda layers, minimize dependencies',
            'estimated_cost_impact': 'Low - Code optimization has no additional cost'
        })
    
    # Memory optimization
    if analysis['memory_size'] < 512 and analysis['avg_cold_start_duration'] > 2000:
        recommendations.append({
            'function': function_name,
            'priority': 'MEDIUM',
            'category': 'Memory Configuration',
            'issue': f"Low memory allocation ({analysis['memory_size']}MB) with slow cold starts",
            'recommendation': 'Increase memory allocation to improve cold start performance',
            'action': f"Increase memory from {analysis['memory_size']}MB to 512MB or 1024MB",
            'estimated_cost_impact': 'Low - Faster execution may offset increased memory cost'
        })
    
    return recommendations

def publish_cold_start_metrics(analysis):
    """Publish aggregated cold start metrics to CloudWatch"""
    environment = analysis['environment']
    
    total_cold_starts = sum(f['cold_start_count'] for f in analysis['functions_analyzed'])
    avg_cold_start_duration = sum(f['avg_cold_start_duration'] * f['cold_start_count'] for f in analysis['functions_analyzed']) / total_cold_starts if total_cold_starts > 0 else 0
    
    cloudwatch.put_metric_data(
        Namespace='Custom/Lambda/ColdStart',
        MetricData=[
            {
                'MetricName': 'TotalColdStarts',
                'Value': total_cold_starts,
                'Unit': 'Count',
                'Timestamp': datetime.now(),
                'Dimensions': [
                    {'Name': 'Environment', 'Value': environment}
                ]
            },
            {
                'MetricName': 'AverageColdStartDuration',
                'Value': avg_cold_start_duration,
                'Unit': 'Milliseconds',
                'Timestamp': datetime.now(),
                'Dimensions': [
                    {'Name': 'Environment', 'Value': environment}
                ]
            },
            {
                'MetricName': 'FunctionsWithHighColdStarts',
                'Value': sum(1 for f in analysis['functions_analyzed'] if f['pattern'] == 'HIGH_FREQUENCY'),
                'Unit': 'Count',
                'Timestamp': datetime.now(),
                'Dimensions': [
                    {'Name': 'Environment', 'Value': environment}
                ]
            }
        ]
    )
`),
            environment: {
                ENVIRONMENT: environment,
            },
        });

        // Grant permissions
        coldStartFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'lambda:ListFunctions',
                'lambda:GetFunction',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:PutMetricData',
            ],
            resources: ['*'],
        }));

        return coldStartFunction;
    }

    /**
     * Create cost optimization function
     * Requirement 13.9: Analyze function configuration and provide cost optimization recommendations
     */
    private createCostOptimizationFunction(environment: string): lambda.Function {
        const costOptimizationFunction = new lambda.Function(this, 'CostOptimizationFunction', {
            functionName: `${environment}-lambda-cost-optimization`,
            runtime: lambda.Runtime.PYTHON_3_11,
            handler: 'index.handler',
            timeout: cdk.Duration.minutes(5),
            memorySize: 512,
            code: lambda.Code.fromInline(`
import boto3
import json
import os
from datetime import datetime, timedelta

cloudwatch = boto3.client('cloudwatch')
lambda_client = boto3.client('lambda')
ce_client = boto3.client('ce')  # Cost Explorer

def handler(event, context):
    """
    Analyze Lambda function configurations and provide cost optimization recommendations
    """
    environment = os.environ.get('ENVIRONMENT', 'development')
    
    try:
        # Get all Lambda functions
        paginator = lambda_client.get_paginator('list_functions')
        functions = []
        
        for page in paginator.paginate():
            functions.extend(page['Functions'])
        
        # Filter genai-demo functions
        genai_functions = [f for f in functions if 'genai-demo' in f['FunctionName'].lower() or environment in f['FunctionName'].lower()]
        
        cost_analysis = {
            'timestamp': datetime.now().isoformat(),
            'environment': environment,
            'total_functions': len(genai_functions),
            'functions_analyzed': [],
            'recommendations': [],
            'estimated_savings': 0
        }
        
        for func in genai_functions:
            function_name = func['FunctionName']
            
            # Get function metrics
            metrics = get_function_metrics(function_name)
            
            # Analyze cost optimization opportunities
            analysis = analyze_cost_optimization(function_name, func, metrics)
            cost_analysis['functions_analyzed'].append(analysis)
            
            # Generate recommendations
            recommendations = generate_cost_recommendations(analysis)
            cost_analysis['recommendations'].extend(recommendations)
            
            # Calculate estimated savings
            cost_analysis['estimated_savings'] += analysis.get('potential_savings', 0)
        
        # Publish cost optimization metrics
        publish_cost_metrics(cost_analysis)
        
        print(json.dumps(cost_analysis, indent=2))
        
        return {
            'statusCode': 200,
            'body': json.dumps(cost_analysis)
        }
        
    except Exception as e:
        print(f'Error analyzing cost optimization: {str(e)}')
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }

def get_function_metrics(function_name):
    """Get execution metrics for cost analysis"""
    try:
        end_time = datetime.now()
        start_time = end_time - timedelta(days=7)
        
        # Get invocation count
        invocations = cloudwatch.get_metric_statistics(
            Namespace='AWS/Lambda',
            MetricName='Invocations',
            Dimensions=[{'Name': 'FunctionName', 'Value': function_name}],
            StartTime=start_time,
            EndTime=end_time,
            Period=86400,  # 1 day
            Statistics=['Sum']
        )
        
        # Get duration
        duration = cloudwatch.get_metric_statistics(
            Namespace='AWS/Lambda',
            MetricName='Duration',
            Dimensions=[{'Name': 'FunctionName', 'Value': function_name}],
            StartTime=start_time,
            EndTime=end_time,
            Period=86400,
            Statistics=['Average', 'Maximum']
        )
        
        # Get memory utilization from Lambda Insights
        memory_util = cloudwatch.get_metric_statistics(
            Namespace='LambdaInsights',
            MetricName='memory_utilization',
            Dimensions=[{'Name': 'function_name', 'Value': function_name}],
            StartTime=start_time,
            EndTime=end_time,
            Period=86400,
            Statistics=['Average', 'Maximum']
        )
        
        return {
            'invocations': invocations.get('Datapoints', []),
            'duration': duration.get('Datapoints', []),
            'memory_utilization': memory_util.get('Datapoints', [])
        }
    except Exception as e:
        print(f'Error getting metrics for {function_name}: {str(e)}')
        return {'invocations': [], 'duration': [], 'memory_utilization': []}

def analyze_cost_optimization(function_name, function_config, metrics):
    """Analyze function for cost optimization opportunities"""
    memory_size = function_config.get('MemorySize', 128)
    timeout = function_config.get('Timeout', 3)
    
    # Calculate average invocations per day
    total_invocations = sum(m['Sum'] for m in metrics['invocations'])
    avg_invocations_per_day = total_invocations / 7 if total_invocations > 0 else 0
    
    # Calculate average duration
    avg_duration = sum(m['Average'] for m in metrics['duration']) / len(metrics['duration']) if metrics['duration'] else 0
    max_duration = max((m['Maximum'] for m in metrics['duration']), default=0)
    
    # Calculate average memory utilization
    avg_memory_util = sum(m['Average'] for m in metrics['memory_utilization']) / len(metrics['memory_utilization']) if metrics['memory_utilization'] else 0
    
    # Identify optimization opportunities
    over_provisioned_memory = avg_memory_util < 60 and memory_size > 256
    over_provisioned_timeout = max_duration < (timeout * 1000 * 0.5)  # Using less than 50% of timeout
    low_utilization = avg_invocations_per_day < 10
    
    # Calculate potential savings
    potential_savings = 0
    if over_provisioned_memory:
        # Estimate 30% cost reduction from right-sizing memory
        estimated_monthly_cost = (memory_size / 128) * avg_invocations_per_day * 30 * 0.0000166667  # Rough estimate
        potential_savings += estimated_monthly_cost * 0.3
    
    return {
        'function_name': function_name,
        'memory_size': memory_size,
        'timeout': timeout,
        'avg_invocations_per_day': round(avg_invocations_per_day, 2),
        'avg_duration_ms': round(avg_duration, 2),
        'max_duration_ms': round(max_duration, 2),
        'avg_memory_utilization': round(avg_memory_util, 2),
        'over_provisioned_memory': over_provisioned_memory,
        'over_provisioned_timeout': over_provisioned_timeout,
        'low_utilization': low_utilization,
        'potential_savings': round(potential_savings, 2)
    }

def generate_cost_recommendations(analysis):
    """Generate cost optimization recommendations"""
    recommendations = []
    function_name = analysis['function_name']
    
    if analysis['over_provisioned_memory']:
        recommended_memory = max(128, int(analysis['memory_size'] * (analysis['avg_memory_utilization'] / 100) * 1.2))  # 20% buffer
        recommendations.append({
            'function': function_name,
            'priority': 'HIGH',
            'category': 'Memory Right-Sizing',
            'issue': f"Over-provisioned memory: {analysis['memory_size']}MB with {analysis['avg_memory_utilization']}% utilization",
            'recommendation': f"Reduce memory allocation to {recommended_memory}MB",
            'action': f"Update function memory from {analysis['memory_size']}MB to {recommended_memory}MB",
            'estimated_savings': f"${'{'}analysis['potential_savings']:.2f{'}'}/month"
        })
    
    if analysis['over_provisioned_timeout']:
        recommended_timeout = max(3, int(analysis['max_duration_ms'] / 1000 * 1.5))  # 50% buffer
        recommendations.append({
            'function': function_name,
            'priority': 'MEDIUM',
            'category': 'Timeout Optimization',
            'issue': f"Over-provisioned timeout: {analysis['timeout']}s with max duration {analysis['max_duration_ms']}ms",
            'recommendation': f"Reduce timeout to {recommended_timeout}s",
            'action': f"Update function timeout from {analysis['timeout']}s to {recommended_timeout}s",
            'estimated_savings': 'Prevents unnecessary long-running executions'
        })
    
    if analysis['low_utilization']:
        recommendations.append({
            'function': function_name,
            'priority': 'LOW',
            'category': 'Function Utilization',
            'issue': f"Low utilization: {analysis['avg_invocations_per_day']:.0f} invocations/day",
            'recommendation': 'Consider consolidating with other low-traffic functions or using Step Functions',
            'action': 'Review if this function is still needed or can be combined with others',
            'estimated_savings': 'Reduces number of functions to manage'
        })
    
    # Architecture recommendations
    if analysis['avg_duration_ms'] > 60000:  # > 1 minute
        recommendations.append({
            'function': function_name,
            'priority': 'HIGH',
            'category': 'Architecture',
            'issue': f"Long-running function: avg {analysis['avg_duration_ms']}ms",
            'recommendation': 'Consider using ECS/Fargate for long-running tasks instead of Lambda',
            'action': 'Evaluate migrating to container-based compute for better cost efficiency',
            'estimated_savings': 'Significant for long-running workloads'
        })
    
    return recommendations

def publish_cost_metrics(analysis):
    """Publish cost optimization metrics to CloudWatch"""
    environment = analysis['environment']
    
    cloudwatch.put_metric_data(
        Namespace='Custom/Lambda/CostOptimization',
        MetricData=[
            {
                'MetricName': 'EstimatedMonthlySavings',
                'Value': analysis['estimated_savings'],
                'Unit': 'None',
                'Timestamp': datetime.now(),
                'Dimensions': [
                    {'Name': 'Environment', 'Value': environment}
                ]
            },
            {
                'MetricName': 'OverProvisionedFunctions',
                'Value': sum(1 for f in analysis['functions_analyzed'] if f['over_provisioned_memory']),
                'Unit': 'Count',
                'Timestamp': datetime.now(),
                'Dimensions': [
                    {'Name': 'Environment', 'Value': environment}
                ]
            },
            {
                'MetricName': 'LowUtilizationFunctions',
                'Value': sum(1 for f in analysis['functions_analyzed'] if f['low_utilization']),
                'Unit': 'Count',
                'Timestamp': datetime.now(),
                'Dimensions': [
                    {'Name': 'Environment', 'Value': environment}
                ]
            }
        ]
    )
`),
            environment: {
                ENVIRONMENT: environment,
            },
        });

        // Grant permissions
        costOptimizationFunction.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'lambda:ListFunctions',
                'lambda:GetFunction',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:PutMetricData',
                'ce:GetCostAndUsage',  // Cost Explorer
            ],
            resources: ['*'],
        }));

        return costOptimizationFunction;
    }

    /**
     * Create CloudWatch alarms for Lambda Insights metrics
     */
    private createLambdaInsightsAlarms(environment: string, dashboard: cloudwatch.Dashboard): void {
        // Alarm for high cold start rate
        const highColdStartAlarm = new cloudwatch.Alarm(this, 'HighColdStartRateAlarm', {
            alarmName: `${environment}-lambda-high-cold-start-rate`,
            alarmDescription: 'High rate of Lambda cold starts detected - consider Provisioned Concurrency',
            metric: new cloudwatch.Metric({
                namespace: 'Custom/Lambda/ColdStart',
                metricName: 'TotalColdStarts',
                dimensionsMap: {
                    Environment: environment,
                },
                statistic: 'Sum',
                period: cdk.Duration.hours(1),
            }),
            threshold: 50,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for high memory utilization
        const highMemoryUtilizationAlarm = new cloudwatch.Alarm(this, 'HighMemoryUtilizationAlarm', {
            alarmName: `${environment}-lambda-high-memory-utilization`,
            alarmDescription: 'Lambda functions approaching memory limits - increase memory allocation',
            metric: new cloudwatch.Metric({
                namespace: 'LambdaInsights',
                metricName: 'memory_utilization',
                statistic: 'Average',
                period: cdk.Duration.minutes(5),
            }),
            threshold: 90,
            evaluationPeriods: 2,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Alarm for cost optimization opportunities
        const costOptimizationAlarm = new cloudwatch.Alarm(this, 'CostOptimizationOpportunityAlarm', {
            alarmName: `${environment}-lambda-cost-optimization-opportunity`,
            alarmDescription: 'Significant cost optimization opportunities detected for Lambda functions',
            metric: new cloudwatch.Metric({
                namespace: 'Custom/Lambda/CostOptimization',
                metricName: 'EstimatedMonthlySavings',
                dimensionsMap: {
                    Environment: environment,
                },
                statistic: 'Maximum',
                period: cdk.Duration.hours(24),
            }),
            threshold: 10,  // $10/month savings opportunity
            evaluationPeriods: 1,
            comparisonOperator: cloudwatch.ComparisonOperator.GREATER_THAN_THRESHOLD,
            treatMissingData: cloudwatch.TreatMissingData.NOT_BREACHING,
        });

        // Add alarms to dashboard (separate widgets for each alarm)
        dashboard.addWidgets(
            new cloudwatch.AlarmWidget({
                title: 'Lambda Insights - Cold Start Rate',
                alarm: highColdStartAlarm,
                width: 8,
                height: 4,
            }),
            new cloudwatch.AlarmWidget({
                title: 'Lambda Insights - Memory Utilization',
                alarm: highMemoryUtilizationAlarm,
                width: 8,
                height: 4,
            }),
            new cloudwatch.AlarmWidget({
                title: 'Lambda Insights - Cost Optimization',
                alarm: costOptimizationAlarm,
                width: 8,
                height: 4,
            })
        );
    }

    /**
     * Enable Lambda Insights for a function
     * This method can be called to enable Lambda Insights on existing functions
     */
    public enableInsightsForFunction(func: lambda.Function): void {
        // Add Lambda Insights layer
        func.addLayers(this.insightsLayer);

        // Add required permissions
        func.addToRolePolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
            ],
            resources: ['*'],
        }));

        // Add environment variable to enable Lambda Insights
        func.addEnvironment('AWS_LAMBDA_EXEC_WRAPPER', '/opt/otel-instrument');
    }
}
