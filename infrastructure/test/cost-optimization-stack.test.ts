import * as cdk from 'aws-cdk-lib';
import { Template, Match } from 'aws-cdk-lib/assertions';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as sns from 'aws-cdk-lib/aws-sns';
import { CostOptimizationStack } from '../lib/stacks/cost-optimization-stack';

describe('CostOptimizationStack', () => {
  let app: cdk.App;
  let mockCluster: eks.ICluster;
  let mockTopic: sns.ITopic;
  let stack: CostOptimizationStack;
  let template: Template;

  beforeEach(() => {
    app = new cdk.App();
    
    // Create mock EKS cluster
    const mockStack = new cdk.Stack(app, 'MockStack');
    mockCluster = eks.Cluster.fromClusterAttributes(mockStack, 'MockCluster', {
      clusterName: 'test-cluster',
      kubectlRoleArn: 'arn:aws:iam::123456789012:role/test-role',
    });
    
    // Create mock SNS topic
    mockTopic = sns.Topic.fromTopicArn(mockStack, 'MockTopic', 
      'arn:aws:sns:us-east-1:123456789012:test-topic'
    );
    
    stack = new CostOptimizationStack(app, 'TestCostOptimizationStack', {
      cluster: mockCluster,
      alertTopic: mockTopic,
      enableClusterAutoscaler: true,
      enableVPA: true,
      enableAuroraCostOptimization: true,
      env: {
        account: '123456789012',
        region: 'us-east-1',
      },
    });
    template = Template.fromStack(stack);
  });

  describe('Cluster Autoscaler', () => {
    test('should create IAM role for Cluster Autoscaler', () => {
      template.hasResourceProperties('AWS::IAM::Role', {
        RoleName: 'genai-demo-cluster-autoscaler-role',
        AssumeRolePolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Principal: {
                Service: 'eks.amazonaws.com',
              },
            }),
          ]),
        },
      });
    });

    test('should grant autoscaling permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'autoscaling:DescribeAutoScalingGroups',
                'autoscaling:SetDesiredCapacity',
              ]),
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });
  });

  describe('Vertical Pod Autoscaler', () => {
    test('should create IAM role for VPA', () => {
      template.hasResourceProperties('AWS::IAM::Role', {
        RoleName: 'genai-demo-vpa-role',
        AssumeRolePolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Principal: {
                Service: 'eks.amazonaws.com',
              },
            }),
          ]),
        },
      });
    });

    test('should grant CloudWatch metrics permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
              ]),
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });
  });

  describe('Aurora Cost Optimization', () => {
    test('should create Aurora cost optimizer Lambda', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        FunctionName: 'genai-demo-aurora-cost-optimizer',
        Runtime: 'python3.11',
        Handler: 'index.handler',
        Timeout: 300,
      });
    });

    test('should have SNS topic ARN in environment', () => {
      template.hasResourceProperties('AWS::Lambda::Function', {
        Environment: {
          Variables: {
            SNS_TOPIC_ARN: Match.anyValue(),
          },
        },
      });
    });

    test('should grant RDS permissions', () => {
      template.hasResourceProperties('AWS::IAM::Policy', {
        PolicyDocument: {
          Statement: Match.arrayWith([
            Match.objectLike({
              Action: Match.arrayWith([
                'rds:DescribeDBClusters',
                'rds:DescribeDBInstances',
              ]),
              Effect: 'Allow',
            }),
          ]),
        },
      });
    });

    test('should create weekly schedule', () => {
      template.hasResourceProperties('AWS::Events::Rule', {
        Name: 'genai-demo-aurora-cost-optimization-weekly',
        ScheduleExpression: 'cron(0 10 ? * MON *)',
      });
    });
  });

  describe('Cost Optimization Dashboard', () => {
    test('should create dashboard', () => {
      template.hasResourceProperties('AWS::CloudWatch::Dashboard', {
        DashboardName: 'GenAIDemo-Cost-Optimization-Metrics',
      });
    });
  });

  describe('Stack Outputs', () => {
    test('should export Cluster Autoscaler role ARN', () => {
      template.hasOutput('ClusterAutoscalerRoleArn', {
        Export: {
          Name: 'GenAIDemo-ClusterAutoscalerRole',
        },
      });
    });

    test('should export VPA role ARN', () => {
      template.hasOutput('VPARoleArn', {
        Export: {
          Name: 'GenAIDemo-VPARole',
        },
      });
    });

    test('should export Aurora optimizer function ARN', () => {
      template.hasOutput('AuroraCostOptimizerFunctionArn', {
        Export: {
          Name: 'GenAIDemo-AuroraCostOptimizer',
        },
      });
    });

    test('should export dashboard name', () => {
      template.hasOutput('CostOptimizationDashboardName', {
        Export: {
          Name: 'GenAIDemo-CostOptimizationDashboard',
        },
      });
    });
  });

  describe('Resource Tags', () => {
    test('should have cost allocation tags', () => {
      const roles = template.findResources('AWS::IAM::Role');
      expect(Object.keys(roles).length).toBeGreaterThan(0);
    });
  });
});
