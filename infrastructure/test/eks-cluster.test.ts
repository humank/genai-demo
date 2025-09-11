import * as cdk from 'aws-cdk-lib';
import { Match, Template } from 'aws-cdk-lib/assertions';
import { CoreInfrastructureStack } from '../lib/stacks/core-infrastructure-stack';
import { NetworkStack } from '../lib/stacks/network-stack';

describe('EKS Cluster Infrastructure', () => {
    let app: cdk.App;
    let networkStack: NetworkStack;
    let coreStack: CoreInfrastructureStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App({
            context: {
                'genai-demo:environments': {
                    development: {
                        'vpc-cidr': '10.0.0.0/16',
                        'nat-gateways': 1,
                        'eks-node-type': 't3.medium',
                        'eks-min-nodes': 1,
                        'eks-max-nodes': 3,
                        'cost-optimization': {
                            'spot-instances': false,
                            'spot-percentage': 0
                        }
                    }
                },
                '@aws-cdk/aws-eks:cluster-logging': true,
                'aws-cdk:enableDiffNoFail': true
            }
        });

        networkStack = new NetworkStack(app, 'TestNetworkStack', {
            environment: 'development',
            projectName: 'genai-demo',
            env: {
                account: '123456789012',
                region: 'ap-east-2'
            }
        });

        coreStack = new CoreInfrastructureStack(app, 'TestCoreStack', {
            environment: 'development',
            projectName: 'genai-demo',
            vpc: networkStack.vpc,
            albSecurityGroup: networkStack.albSecurityGroup,
            eksSecurityGroup: networkStack.eksSecurityGroup,
            env: {
                account: '123456789012',
                region: 'ap-east-2'
            }
        });

        template = Template.fromStack(coreStack);
    });

    describe('EKS Cluster Configuration', () => {
        test('should create EKS cluster with correct configuration', () => {
            // Check that the EKS cluster nested stack exists
            template.resourceCountIs('AWS::CloudFormation::Stack', 2);

            // Check that one of the stacks is the cluster resource provider
            const resources = template.toJSON().Resources;
            const clusterStack = Object.keys(resources).find(key =>
                key.includes('ClusterResourceProvider') &&
                resources[key].Type === 'AWS::CloudFormation::Stack'
            );
            expect(clusterStack).toBeDefined();
        });

        test('should create EKS cluster with proper tags', () => {
            // Check that the cluster resource provider stack has proper tags
            template.hasResourceProperties('AWS::CloudFormation::Stack', {
                Tags: Match.arrayWith([
                    { Key: 'Environment', Value: 'development' },
                    { Key: 'Project', Value: 'genai-demo' }
                ])
            });
        });
    });

    describe('EKS Node Group Configuration', () => {
        test('should create managed node group with correct configuration', () => {
            template.hasResourceProperties('AWS::EKS::Nodegroup', {
                NodegroupName: 'genai-demo-development-nodes',
                InstanceTypes: ['t3.medium', 't3.large'],
                AmiType: 'AL2_x86_64',
                CapacityType: 'ON_DEMAND'
            });
        });

        test('should configure node group with proper labels', () => {
            template.hasResourceProperties('AWS::EKS::Nodegroup', {
                Labels: {
                    'node-type': 'managed',
                    'environment': 'development',
                    'project': 'genai-demo',
                    'architecture': 'x86_64'
                }
            });
        });
    });

    describe('EKS Add-ons and Helm Charts', () => {
        test('should install AWS Load Balancer Controller', () => {
            template.hasResourceProperties('Custom::AWSCDK-EKS-HelmChart', {
                Chart: 'aws-load-balancer-controller',
                Repository: 'https://aws.github.io/eks-charts',
                Namespace: 'kube-system'
            });
        });

        test('should install Cluster Autoscaler', () => {
            template.hasResourceProperties('Custom::AWSCDK-EKS-HelmChart', {
                Chart: 'cluster-autoscaler',
                Repository: 'https://kubernetes.github.io/autoscaler',
                Namespace: 'kube-system'
            });
        });

        test('should install Metrics Server', () => {
            template.hasResourceProperties('Custom::AWSCDK-EKS-HelmChart', {
                Chart: 'metrics-server',
                Repository: 'https://kubernetes-sigs.github.io/metrics-server/',
                Namespace: 'kube-system'
            });
        });
    });

    describe('EKS Outputs', () => {
        test('should export EKS cluster information', () => {
            template.hasOutput('EksClusterName', {
                Export: {
                    Name: 'genai-demo-development-eks-cluster-name'
                }
            });

            template.hasOutput('EksClusterArn', {
                Export: {
                    Name: 'genai-demo-development-eks-cluster-arn'
                }
            });

            template.hasOutput('EksClusterEndpoint', {
                Export: {
                    Name: 'genai-demo-development-eks-cluster-endpoint'
                }
            });
        });
    });
});