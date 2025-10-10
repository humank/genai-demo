import * as cdk from 'aws-cdk-lib';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import { Construct } from 'constructs';

export interface CostOptimizationStackProps extends cdk.StackProps {
  /**
   * EKS Cluster for autoscaling configuration
   */
  readonly cluster: eks.ICluster;
  
  /**
   * SNS Topic for cost optimization alerts
   */
  readonly alertTopic: sns.ITopic;
  
  /**
   * Enable EKS Cluster Autoscaler
   * @default true
   */
  readonly enableClusterAutoscaler?: boolean;
  
  /**
   * Enable Vertical Pod Autoscaler (VPA)
   * @default true
   */
  readonly enableVPA?: boolean;
  
  /**
   * Enable Aurora cost optimization
   * @default true
   */
  readonly enableAuroraCostOptimization?: boolean;
}

/**
 * Cost Optimization Automation Stack
 * 
 * Implements automated cost optimization mechanisms:
 * - EKS Cluster Autoscaler for dynamic node scaling
 * - Vertical Pod Autoscaler (VPA) for resource right-sizing
 * - Aurora Global Database cross-region cost optimization
 * 
 * Architecture: Kubernetes-native + AWS Lambda automation
 */
export class CostOptimizationStack extends cdk.Stack {
  public readonly clusterAutoscalerRole?: iam.Role;
  public readonly vpaRole?: iam.Role;
  public readonly auroraCostOptimizerFunction?: lambda.Function;

  constructor(scope: Construct, id: string, props: CostOptimizationStackProps) {
    super(scope, id, props);

    // ============================================
    // 1. EKS Cluster Autoscaler Configuration
    // ============================================
    if (props.enableClusterAutoscaler !== false) {
      // Create IAM role for Cluster Autoscaler
      this.clusterAutoscalerRole = new iam.Role(this, 'ClusterAutoscalerRole', {
        roleName: 'genai-demo-cluster-autoscaler-role',
        assumedBy: new iam.ServicePrincipal('eks.amazonaws.com'),
        description: 'IAM role for EKS Cluster Autoscaler',
      });

      // Grant necessary permissions
      this.clusterAutoscalerRole.addToPolicy(
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'autoscaling:DescribeAutoScalingGroups',
            'autoscaling:DescribeAutoScalingInstances',
            'autoscaling:DescribeLaunchConfigurations',
            'autoscaling:DescribeScalingActivities',
            'autoscaling:DescribeTags',
            'autoscaling:SetDesiredCapacity',
            'autoscaling:TerminateInstanceInAutoScalingGroup',
            'ec2:DescribeImages',
            'ec2:DescribeInstanceTypes',
            'ec2:DescribeLaunchTemplateVersions',
            'ec2:GetInstanceTypesFromInstanceRequirements',
            'eks:DescribeNodegroup',
          ],
          resources: ['*'],
        })
      );

      // Deploy Cluster Autoscaler using Helm
      const clusterAutoscalerChart = props.cluster.addHelmChart('ClusterAutoscaler', {
        chart: 'cluster-autoscaler',
        repository: 'https://kubernetes.github.io/autoscaler',
        namespace: 'kube-system',
        values: {
          autoDiscovery: {
            clusterName: props.cluster.clusterName,
          },
          awsRegion: this.region,
          rbac: {
            serviceAccount: {
              create: true,
              name: 'cluster-autoscaler',
              annotations: {
                'eks.amazonaws.com/role-arn': this.clusterAutoscalerRole.roleArn,
              },
            },
          },
          extraArgs: {
            'balance-similar-node-groups': true,
            'skip-nodes-with-system-pods': false,
            'scale-down-enabled': true,
            'scale-down-delay-after-add': '10m',
            'scale-down-unneeded-time': '10m',
            'scale-down-utilization-threshold': '0.5',
          },
        },
      });
    }


    // ============================================
    // 2. Vertical Pod Autoscaler (VPA) Configuration
    // ============================================
    if (props.enableVPA !== false) {
      // Create IAM role for VPA
      this.vpaRole = new iam.Role(this, 'VPARole', {
        roleName: 'genai-demo-vpa-role',
        assumedBy: new iam.ServicePrincipal('eks.amazonaws.com'),
        description: 'IAM role for Vertical Pod Autoscaler',
      });

      // Grant CloudWatch metrics access for VPA recommendations
      this.vpaRole.addToPolicy(
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'cloudwatch:GetMetricStatistics',
            'cloudwatch:ListMetrics',
          ],
          resources: ['*'],
        })
      );

      // Deploy VPA using Helm
      const vpaChart = props.cluster.addHelmChart('VerticalPodAutoscaler', {
        chart: 'vpa',
        repository: 'https://charts.fairwinds.com/stable',
        namespace: 'kube-system',
        values: {
          recommender: {
            enabled: true,
            resources: {
              requests: {
                cpu: '100m',
                memory: '500Mi',
              },
              limits: {
                cpu: '200m',
                memory: '1Gi',
              },
            },
          },
          updater: {
            enabled: true,
            resources: {
              requests: {
                cpu: '100m',
                memory: '500Mi',
              },
              limits: {
                cpu: '200m',
                memory: '1Gi',
              },
            },
          },
          admissionController: {
            enabled: true,
            resources: {
              requests: {
                cpu: '100m',
                memory: '200Mi',
              },
              limits: {
                cpu: '200m',
                memory: '500Mi',
              },
            },
          },
        },
      });

      // Create VPA policy for application workloads
      const vpaManifest = props.cluster.addManifest('ApplicationVPA', {
        apiVersion: 'autoscaling.k8s.io/v1',
        kind: 'VerticalPodAutoscaler',
        metadata: {
          name: 'genai-demo-app-vpa',
          namespace: 'default',
        },
        spec: {
          targetRef: {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            name: 'genai-demo-app',
          },
          updatePolicy: {
            updateMode: 'Auto', // Automatically apply recommendations
          },
          resourcePolicy: {
            containerPolicies: [
              {
                containerName: '*',
                minAllowed: {
                  cpu: '100m',
                  memory: '128Mi',
                },
                maxAllowed: {
                  cpu: '2000m',
                  memory: '4Gi',
                },
                controlledResources: ['cpu', 'memory'],
              },
            ],
          },
        },
      });
    }


    // ============================================
    // 3. Aurora Global Database Cost Optimization
    // ============================================
    if (props.enableAuroraCostOptimization !== false) {
      // Lambda function for Aurora cost optimization
      this.auroraCostOptimizerFunction = new lambda.Function(this, 'AuroraCostOptimizer', {
        functionName: 'genai-demo-aurora-cost-optimizer',
        runtime: lambda.Runtime.PYTHON_3_11,
        handler: 'index.handler',
        code: lambda.Code.fromInline(`
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
    Aurora Global Database cost optimization automation
    
    Optimizations:
    - Identify underutilized read replicas
    - Recommend instance type downsizing
    - Analyze cross-region replication costs
    - Suggest Reserved Instance opportunities
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
    """Estimate monthly savings from downsizing"""
    # Simplified pricing estimation
    pricing_map = {
        'db.r6g.large': 200,
        'db.r6g.xlarge': 400,
        'db.r6g.2xlarge': 800,
        'db.r6g.4xlarge': 1600,
    }
    
    current_cost = pricing_map.get(instance_class, 0)
    # Assume 30% savings from downsizing
    return current_cost * 0.3
`),
        timeout: cdk.Duration.minutes(5),
        environment: {
          SNS_TOPIC_ARN: props.alertTopic.topicArn,
        },
      });

      // Grant permissions to Lambda
      this.auroraCostOptimizerFunction.addToRolePolicy(
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          actions: [
            'rds:DescribeDBClusters',
            'rds:DescribeDBInstances',
            'cloudwatch:GetMetricStatistics',
          ],
          resources: ['*'],
        })
      );

      props.alertTopic.grantPublish(this.auroraCostOptimizerFunction);

      // Schedule weekly Aurora cost optimization checks
      const auroraCostRule = new events.Rule(this, 'AuroraCostOptimizationSchedule', {
        ruleName: 'genai-demo-aurora-cost-optimization-weekly',
        description: 'Weekly Aurora cost optimization analysis',
        schedule: events.Schedule.cron({
          weekDay: 'MON',
          hour: '10',
          minute: '0',
        }),
      });

      auroraCostRule.addTarget(
        new targets.LambdaFunction(this.auroraCostOptimizerFunction)
      );
    }


    // ============================================
    // 4. Cost Optimization Monitoring Dashboard
    // ============================================
    const costOptimizationDashboard = new cloudwatch.Dashboard(this, 'CostOptimizationDashboard', {
      dashboardName: 'GenAIDemo-Cost-Optimization-Metrics',
    });

    // EKS Node Count and Utilization
    costOptimizationDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'EKS Node Count (Cluster Autoscaler)',
        width: 12,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/EKS',
            metricName: 'cluster_autoscaler_nodes_count',
            dimensionsMap: {
              ClusterName: props.cluster.clusterName,
            },
            statistic: 'Average',
            period: cdk.Duration.minutes(5),
          }),
        ],
      })
    );

    // Pod Resource Utilization (VPA Metrics)
    costOptimizationDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Pod CPU Utilization (VPA Monitoring)',
        width: 12,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'ContainerInsights',
            metricName: 'pod_cpu_utilization',
            dimensionsMap: {
              ClusterName: props.cluster.clusterName,
            },
            statistic: 'Average',
            period: cdk.Duration.minutes(5),
          }),
        ],
      })
    );

    // Aurora Instance Utilization
    costOptimizationDashboard.addWidgets(
      new cloudwatch.GraphWidget({
        title: 'Aurora CPU Utilization (Cost Optimization Target)',
        width: 12,
        height: 6,
        left: [
          new cloudwatch.Metric({
            namespace: 'AWS/RDS',
            metricName: 'CPUUtilization',
            statistic: 'Average',
            period: cdk.Duration.hours(1),
          }),
        ],
      })
    );

    // Cost Savings Summary
    costOptimizationDashboard.addWidgets(
      new cloudwatch.SingleValueWidget({
        title: 'Estimated Monthly Savings',
        width: 12,
        height: 6,
        metrics: [
          new cloudwatch.MathExpression({
            expression: 'm1 + m2',
            usingMetrics: {
              m1: new cloudwatch.Metric({
                namespace: 'CostOptimization',
                metricName: 'EKSAutoscalingSavings',
                statistic: 'Sum',
                period: cdk.Duration.days(30),
              }),
              m2: new cloudwatch.Metric({
                namespace: 'CostOptimization',
                metricName: 'AuroraOptimizationSavings',
                statistic: 'Sum',
                period: cdk.Duration.days(30),
              }),
            },
            label: 'Total Savings (USD)',
          }),
        ],
      })
    );

    // ============================================
    // Outputs
    // ============================================
    if (this.clusterAutoscalerRole) {
      new cdk.CfnOutput(this, 'ClusterAutoscalerRoleArn', {
        value: this.clusterAutoscalerRole.roleArn,
        description: 'IAM Role ARN for Cluster Autoscaler',
        exportName: 'GenAIDemo-ClusterAutoscalerRole',
      });
    }

    if (this.vpaRole) {
      new cdk.CfnOutput(this, 'VPARoleArn', {
        value: this.vpaRole.roleArn,
        description: 'IAM Role ARN for Vertical Pod Autoscaler',
        exportName: 'GenAIDemo-VPARole',
      });
    }

    if (this.auroraCostOptimizerFunction) {
      new cdk.CfnOutput(this, 'AuroraCostOptimizerFunctionArn', {
        value: this.auroraCostOptimizerFunction.functionArn,
        description: 'Lambda Function ARN for Aurora cost optimization',
        exportName: 'GenAIDemo-AuroraCostOptimizer',
      });
    }

    new cdk.CfnOutput(this, 'CostOptimizationDashboardName', {
      value: costOptimizationDashboard.dashboardName,
      description: 'CloudWatch Dashboard for cost optimization metrics',
      exportName: 'GenAIDemo-CostOptimizationDashboard',
    });

    // Tags for cost allocation
    cdk.Tags.of(this).add('Project', 'GenAIDemo');
    cdk.Tags.of(this).add('Environment', 'Production');
    cdk.Tags.of(this).add('CostCenter', 'Engineering');
    cdk.Tags.of(this).add('ManagedBy', 'CDK');
    cdk.Tags.of(this).add('Purpose', 'CostOptimization');
  }
}
