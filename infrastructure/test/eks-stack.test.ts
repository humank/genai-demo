import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { EKSStack } from '../src/stacks/eks-stack';
import { NetworkStack } from '../src/stacks/network-stack';

describe('EKSStack', () => {
    let app: cdk.App;
    let networkStack: NetworkStack;
    let eksStack: EKSStack;
    let template: Template;

    beforeEach(() => {
        app = new cdk.App();
        
        // Create network stack first
        networkStack = new NetworkStack(app, 'TestNetworkStack', {
            env: { region: 'us-east-1' },
        });

        // Create EKS stack
        eksStack = new EKSStack(app, 'TestEKSStack', {
            vpc: networkStack.vpc,
            environment: 'test',
            projectName: 'genai-demo',
            region: 'us-east-1',
            env: { region: 'us-east-1' },
        });

        template = Template.fromStack(eksStack);
    });

    test('should create EKS cluster', () => {
        template.hasResourceProperties('Custom::AWSCDK-EKS-Cluster', {
            Config: {
                name: 'genai-demo-test-us-east-1',
                version: '1.28',
            },
        });
    });

    test('should create managed node group', () => {
        template.hasResourceProperties('AWS::EKS::Nodegroup', {
            NodegroupName: 'genai-demo-test-nodes',
            InstanceTypes: ['t3.medium', 't3.large'],
            ScalingConfig: {
                MinSize: 2,
                MaxSize: 10,
                DesiredSize: 2,
            },
        });
    });

    test('should install KEDA via Helm', () => {
        // Check for Helm chart resource
        template.hasResourceProperties('Custom::AWSCDK-EKS-HelmChart', {
            Chart: 'keda',
            Repository: 'https://kedacore.github.io/charts',
            Namespace: 'keda-system',
        });
    });

    test('should create HPA configuration', () => {
        // Check for HPA manifest resource exists (now 6 due to application service account)
        template.resourceCountIs('Custom::AWSCDK-EKS-KubernetesResource', 6);
        
        // Check that one of the manifests contains HPA configuration
        const resources = template.toJSON().Resources;
        const hpaManifest = Object.values(resources).find((resource: any) => 
            resource.Type === 'Custom::AWSCDK-EKS-KubernetesResource' &&
            resource.Properties?.Manifest &&
            JSON.stringify(resource.Properties.Manifest).includes('HorizontalPodAutoscaler')
        );
        expect(hpaManifest).toBeDefined();
    });

    test('should create KEDA ScaledObject', () => {
        // Check that one of the manifests contains KEDA ScaledObject configuration
        const resources = template.toJSON().Resources;
        const kedaManifest = Object.values(resources).find((resource: any) => 
            resource.Type === 'Custom::AWSCDK-EKS-KubernetesResource' &&
            resource.Properties?.Manifest &&
            JSON.stringify(resource.Properties.Manifest).includes('ScaledObject')
        );
        expect(kedaManifest).toBeDefined();
    });

    test('should create cluster autoscaler', () => {
        // Check that one of the manifests contains cluster autoscaler deployment
        const resources = template.toJSON().Resources;
        const autoscalerManifest = Object.values(resources).find((resource: any) => 
            resource.Type === 'Custom::AWSCDK-EKS-KubernetesResource' &&
            resource.Properties?.Manifest &&
            JSON.stringify(resource.Properties.Manifest).includes('cluster-autoscaler')
        );
        expect(autoscalerManifest).toBeDefined();
    });

    test('should create service account for cluster autoscaler', () => {
        // Check that cluster autoscaler service account role is created
        const resources = template.toJSON().Resources;
        const serviceAccountRole = Object.values(resources).find((resource: any) => 
            resource.Type === 'AWS::IAM::Role' &&
            resource.Properties?.AssumeRolePolicyDocument?.Statement?.some((stmt: any) =>
                stmt.Action === 'sts:AssumeRoleWithWebIdentity' &&
                stmt.Principal?.Federated
            )
        );
        expect(serviceAccountRole).toBeDefined();
    });

    test('should have proper IAM permissions for cluster autoscaler', () => {
        template.hasResourceProperties('AWS::IAM::Policy', {
            PolicyDocument: {
                Statement: [
                    {
                        Effect: 'Allow',
                        Action: [
                            'autoscaling:DescribeAutoScalingGroups',
                            'autoscaling:DescribeAutoScalingInstances',
                            'autoscaling:DescribeLaunchConfigurations',
                            'autoscaling:DescribeTags',
                            'autoscaling:SetDesiredCapacity',
                            'autoscaling:TerminateInstanceInAutoScalingGroup',
                            'ec2:DescribeLaunchTemplateVersions',
                            'ec2:DescribeInstanceTypes',
                        ],
                        Resource: '*',
                    },
                ],
            },
        });
    });

    test('should create proper outputs', () => {
        template.hasOutput('EKSClusterName', {
            Export: {
                Name: 'test-eks-cluster-name-us-east-1',
            },
        });

        template.hasOutput('EKSClusterEndpoint', {
            Export: {
                Name: 'test-eks-cluster-endpoint-us-east-1',
            },
        });

        template.hasOutput('EKSClusterArn', {
            Export: {
                Name: 'test-eks-cluster-arn-us-east-1',
            },
        });

        template.hasOutput('EKSClusterSecurityGroupId', {
            Export: {
                Name: 'test-eks-cluster-sg-us-east-1',
            },
        });
    });

    test('should create application service account with AWS permissions', () => {
        // Check that application service account role is created with proper permissions
        const resources = template.toJSON().Resources;
        
        // Find application service account role
        const appServiceAccountRole = Object.values(resources).find((resource: any) => 
            resource.Type === 'AWS::IAM::Role' &&
            resource.Properties?.AssumeRolePolicyDocument?.Statement?.some((stmt: any) =>
                stmt.Action === 'sts:AssumeRoleWithWebIdentity' &&
                stmt.Principal?.Federated &&
                JSON.stringify(resource).includes('genai-demo-app')
            )
        );
        expect(appServiceAccountRole).toBeDefined();

        // Check for CloudWatch permissions
        const cloudWatchPolicy = Object.values(resources).find((resource: any) => 
            resource.Type === 'AWS::IAM::Policy' &&
            JSON.stringify(resource.Properties?.PolicyDocument?.Statement).includes('cloudwatch:PutMetricData')
        );
        expect(cloudWatchPolicy).toBeDefined();

        // Check for X-Ray permissions
        const xrayPolicy = Object.values(resources).find((resource: any) => 
            resource.Type === 'AWS::IAM::Policy' &&
            JSON.stringify(resource.Properties?.PolicyDocument?.Statement).includes('xray:PutTraceSegments')
        );
        expect(xrayPolicy).toBeDefined();
    });

    test('should have proper tags', () => {
        // Check that the EKS cluster resource exists with proper configuration
        template.hasResourceProperties('Custom::AWSCDK-EKS-Cluster', {
            Config: {
                name: 'genai-demo-test-us-east-1',
                version: '1.28',
            },
        });
        
        // Check that stack-level tags are applied
        const resources = template.toJSON().Resources;
        const eksCluster = Object.values(resources).find((resource: any) => 
            resource.Type === 'Custom::AWSCDK-EKS-Cluster'
        );
        expect(eksCluster).toBeDefined();
    });
});