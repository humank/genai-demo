import * as cdk from 'aws-cdk-lib';
import * as certificatemanager from 'aws-cdk-lib/aws-certificatemanager';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import { AwsCliLayer } from 'aws-cdk-lib/lambda-layer-awscli';
import { Construct } from 'constructs';

export interface CoreInfrastructureStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly domain?: string;
    readonly vpc: ec2.IVpc;
    readonly albSecurityGroup: ec2.ISecurityGroup;
    readonly eksSecurityGroup: ec2.ISecurityGroup;
    readonly hostedZone?: route53.IHostedZone;
    readonly certificate?: certificatemanager.ICertificate;
    readonly wildcardCertificate?: certificatemanager.ICertificate;
}

export class CoreInfrastructureStack extends cdk.Stack {
    public readonly loadBalancer: elbv2.ApplicationLoadBalancer;
    public readonly eksCluster: eks.Cluster;

    constructor(scope: Construct, id: string, props: CoreInfrastructureStackProps) {
        super(scope, id, props);

        const {
            environment,
            projectName,
            domain,
            vpc,
            albSecurityGroup,
            eksSecurityGroup,
            hostedZone,
            certificate,
            wildcardCertificate
        } = props;

        // Apply common tags to the stack
        const commonTags = {
            Project: projectName,
            Environment: environment,
            ManagedBy: 'AWS-CDK',
            Component: 'CoreInfrastructure'
        };

        Object.entries(commonTags).forEach(([key, value]) => {
            cdk.Tags.of(this).add(key, value);
        });

        // Create Application Load Balancer
        this.loadBalancer = this.createApplicationLoadBalancer(
            vpc,
            albSecurityGroup,
            projectName,
            environment,
            domain,
            certificate,
            wildcardCertificate
        );

        // Create EKS Cluster
        this.eksCluster = this.createEKSCluster(
            vpc,
            eksSecurityGroup,
            projectName,
            environment
        );

        // Create DNS records if domain and hosted zone are available
        if (domain && hostedZone) {
            this.createDnsRecords(domain, hostedZone, this.loadBalancer);
        }

        // Create outputs for cross-stack references
        this.createOutputs(projectName, environment, domain, certificate);
    }

    private createApplicationLoadBalancer(
        vpc: ec2.IVpc,
        albSecurityGroup: ec2.ISecurityGroup,
        projectName: string,
        environment: string,
        domain?: string,
        certificate?: certificatemanager.ICertificate,
        wildcardCertificate?: certificatemanager.ICertificate
    ): elbv2.ApplicationLoadBalancer {
        // Create Application Load Balancer
        const alb = new elbv2.ApplicationLoadBalancer(this, 'ApplicationLoadBalancer', {
            loadBalancerName: `${projectName}-${environment}-alb`,
            vpc: vpc,
            internetFacing: true,
            securityGroup: albSecurityGroup,
            vpcSubnets: {
                subnetType: ec2.SubnetType.PUBLIC
            },
            deletionProtection: environment === 'production',
            // Enable access logs for production
            ...(environment === 'production' && {
                // Access logs will be configured later when S3 bucket is available
            })
        });

        // Create default target group for health checks
        const defaultTargetGroup = new elbv2.ApplicationTargetGroup(this, 'DefaultTargetGroup', {
            targetGroupName: `${projectName}-${environment}-tg`,
            port: 8080,
            protocol: elbv2.ApplicationProtocol.HTTP,
            vpc: vpc,
            targetType: elbv2.TargetType.IP,
            healthCheck: {
                enabled: true,
                path: '/actuator/health',
                protocol: elbv2.Protocol.HTTP,
                port: '8080',
                healthyThresholdCount: 2,
                unhealthyThresholdCount: 3,
                timeout: cdk.Duration.seconds(10),
                interval: cdk.Duration.seconds(30),
                healthyHttpCodes: '200'
            },
            deregistrationDelay: cdk.Duration.seconds(30)
        });

        // HTTP Listener (redirects to HTTPS)
        alb.addListener('HttpListener', {
            port: 80,
            protocol: elbv2.ApplicationProtocol.HTTP,
            defaultAction: elbv2.ListenerAction.redirect({
                protocol: 'HTTPS',
                port: '443',
                permanent: true
            })
        });

        // HTTPS Listener (if certificates are available)
        if (domain && certificate) {
            const httpsListener = alb.addListener('HttpsListener', {
                port: 443,
                protocol: elbv2.ApplicationProtocol.HTTPS,
                certificates: [certificate],
                defaultTargetGroups: [defaultTargetGroup],
                sslPolicy: elbv2.SslPolicy.TLS12_EXT
            });

            // Add additional certificates for wildcard support
            if (wildcardCertificate) {
                httpsListener.addCertificates('WildcardCertificate', [wildcardCertificate]);
            }
        } else {
            // If no domain/certificate, create a basic HTTP listener
            alb.addListener('HttpOnlyListener', {
                port: 80,
                protocol: elbv2.ApplicationProtocol.HTTP,
                defaultTargetGroups: [defaultTargetGroup]
            });
        }

        // Add tags
        cdk.Tags.of(alb).add('Name', `${projectName}-${environment}-alb`);
        cdk.Tags.of(alb).add('Environment', environment);
        cdk.Tags.of(alb).add('Project', projectName);
        cdk.Tags.of(alb).add('Service', 'ALB');

        cdk.Tags.of(defaultTargetGroup).add('Name', `${projectName}-${environment}-tg`);
        cdk.Tags.of(defaultTargetGroup).add('Environment', environment);
        cdk.Tags.of(defaultTargetGroup).add('Project', projectName);

        return alb;
    }

    private createDnsRecords(
        domain: string,
        hostedZone: route53.IHostedZone,
        loadBalancer: elbv2.ApplicationLoadBalancer
    ): void {
        // Main domain A record
        const mainARecord = new route53.ARecord(this, 'AliasRecord', {
            zone: hostedZone,
            recordName: domain,
            target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
            comment: `Main A record for ${domain} pointing to ALB`
        });

        // Create wildcard DNS record for subdomains
        const domainParts = domain.split('.');
        if (domainParts.length > 2) {
            // For subdomains like dev.kimkao.io, create *.dev.kimkao.io
            const wildcardARecord = new route53.ARecord(this, 'WildcardAliasRecord', {
                zone: hostedZone,
                recordName: `*.${domain}`,
                target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
                comment: `Wildcard A record for *.${domain} pointing to ALB`
            });
        }

        // Create specific subdomain A records for common services
        const subdomains = ['api', 'cmc', 'shop', 'grafana', 'logs'];
        subdomains.forEach(subdomain => {
            new route53.ARecord(this, `${subdomain.charAt(0).toUpperCase() + subdomain.slice(1)}ARecord`, {
                zone: hostedZone,
                recordName: `${subdomain}.${domain}`,
                target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(loadBalancer)),
                comment: `A record for ${subdomain}.${domain} pointing to ALB`
            });
        });
    }

    private createOutputs(
        projectName: string,
        environment: string,
        domain?: string,
        certificate?: certificatemanager.ICertificate
    ): void {
        // Load Balancer Outputs
        new cdk.CfnOutput(this, 'LoadBalancerArn', {
            value: this.loadBalancer.loadBalancerArn,
            description: 'Application Load Balancer ARN',
            exportName: `${projectName}-${environment}-alb-arn`
        });

        new cdk.CfnOutput(this, 'LoadBalancerDnsName', {
            value: this.loadBalancer.loadBalancerDnsName,
            description: 'Application Load Balancer DNS name',
            exportName: `${projectName}-${environment}-alb-dns-name`
        });

        new cdk.CfnOutput(this, 'LoadBalancerHostedZoneId', {
            value: this.loadBalancer.loadBalancerCanonicalHostedZoneId,
            description: 'Application Load Balancer hosted zone ID',
            exportName: `${projectName}-${environment}-alb-hosted-zone-id`
        });

        // DNS A Record information (if domain is configured)
        if (domain) {
            new cdk.CfnOutput(this, 'DnsARecordTarget', {
                value: this.loadBalancer.loadBalancerDnsName,
                description: 'DNS A record target (ALB DNS name)',
                exportName: `${projectName}-${environment}-dns-a-record-target`
            });

            // Kubernetes Ingress configuration helper (if certificate is available)
            if (certificate) {
                new cdk.CfnOutput(this, 'KubernetesIngressAnnotations', {
                    value: JSON.stringify({
                        'kubernetes.io/ingress.class': 'alb',
                        'alb.ingress.kubernetes.io/scheme': 'internet-facing',
                        'alb.ingress.kubernetes.io/target-type': 'ip',
                        'alb.ingress.kubernetes.io/certificate-arn': certificate.certificateArn,
                        'alb.ingress.kubernetes.io/ssl-redirect': '443',
                        'alb.ingress.kubernetes.io/listen-ports': '[{"HTTP": 80}, {"HTTPS": 443}]'
                    }),
                    description: 'Kubernetes Ingress annotations for ALB integration with SSL',
                    exportName: `${projectName}-${environment}-k8s-ingress-annotations`
                });
            }
        }

        // Environment and Project Information
        new cdk.CfnOutput(this, 'Environment', {
            value: environment,
            description: 'Deployment environment'
        });

        new cdk.CfnOutput(this, 'ProjectName', {
            value: projectName,
            description: 'Project name'
        });

        // EKS Cluster Outputs
        new cdk.CfnOutput(this, 'EksClusterName', {
            value: this.eksCluster.clusterName,
            description: 'EKS cluster name',
            exportName: `${projectName}-${environment}-eks-cluster-name`
        });

        new cdk.CfnOutput(this, 'EksClusterArn', {
            value: this.eksCluster.clusterArn,
            description: 'EKS cluster ARN',
            exportName: `${projectName}-${environment}-eks-cluster-arn`
        });

        new cdk.CfnOutput(this, 'EksClusterEndpoint', {
            value: this.eksCluster.clusterEndpoint,
            description: 'EKS cluster endpoint',
            exportName: `${projectName}-${environment}-eks-cluster-endpoint`
        });

        new cdk.CfnOutput(this, 'EksClusterSecurityGroupId', {
            value: this.eksCluster.clusterSecurityGroupId,
            description: 'EKS cluster security group ID',
            exportName: `${projectName}-${environment}-eks-cluster-sg-id`
        });

        new cdk.CfnOutput(this, 'EksOidcIssuerUrl', {
            value: this.eksCluster.clusterOpenIdConnectIssuerUrl,
            description: 'EKS OIDC issuer URL for service accounts',
            exportName: `${projectName}-${environment}-eks-oidc-issuer-url`
        });

        new cdk.CfnOutput(this, 'EksKubectlRoleArn', {
            value: this.eksCluster.kubectlRole?.roleArn || 'Not configured',
            description: 'EKS kubectl role ARN',
            exportName: `${projectName}-${environment}-eks-kubectl-role-arn`
        });
    }

    private createEKSCluster(
        vpc: ec2.IVpc,
        eksSecurityGroup: ec2.ISecurityGroup,
        projectName: string,
        environment: string
    ): eks.Cluster {
        // Get environment-specific configuration
        const envConfig = this.node.tryGetContext('genai-demo:environments')?.[environment] || {};

        const nodeInstanceType = envConfig['eks-node-type'] || 't3.medium';
        const minNodes = envConfig['eks-min-nodes'] || 1;
        const maxNodes = envConfig['eks-max-nodes'] || 3;
        const costOptimization = envConfig['cost-optimization'] || {};

        // Create kubectl layer for EKS cluster
        const kubectlLayer = new AwsCliLayer(this, 'KubectlLayer');

        // Create EKS cluster with comprehensive configuration
        const cluster = new eks.Cluster(this, 'EKSCluster', {
            clusterName: `${projectName}-${environment}`,
            version: eks.KubernetesVersion.V1_28,
            vpc: vpc,
            vpcSubnets: [
                {
                    subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
                }
            ],

            // Security configuration
            endpointAccess: eks.EndpointAccess.PUBLIC_AND_PRIVATE,
            securityGroup: eksSecurityGroup,

            // Disable default capacity - we'll add managed node groups
            defaultCapacity: 0,

            // Enable comprehensive cluster logging for observability
            clusterLogging: [
                eks.ClusterLoggingTypes.API,
                eks.ClusterLoggingTypes.AUDIT,
                eks.ClusterLoggingTypes.AUTHENTICATOR,
                eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
                eks.ClusterLoggingTypes.SCHEDULER
            ],

            // Add kubectl layer
            kubectlLayer: kubectlLayer
        });

        // Add tags to the cluster
        cdk.Tags.of(cluster).add('Name', `${projectName}-${environment}-eks`);
        cdk.Tags.of(cluster).add('Environment', environment);
        cdk.Tags.of(cluster).add('Project', projectName);
        cdk.Tags.of(cluster).add('Service', 'EKS');
        cdk.Tags.of(cluster).add('ManagedBy', 'AWS-CDK');

        // Add Graviton3 ARM64 managed node group for production, regular instances for dev
        const nodeGroupConfig = this.getNodeGroupConfiguration(nodeInstanceType, environment, costOptimization);

        cluster.addNodegroupCapacity('ManagedNodeGroup', {
            nodegroupName: `${projectName}-${environment}-nodes`,
            instanceTypes: nodeGroupConfig.instanceTypes,
            minSize: minNodes,
            maxSize: maxNodes,
            desiredSize: Math.max(minNodes, Math.min(2, maxNodes)),

            // Use private subnets for security
            subnets: {
                subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
            },

            // Capacity configuration
            capacityType: nodeGroupConfig.capacityType,

            // Auto-scaling configuration
            forceUpdate: true,

            // Disk configuration
            diskSize: environment === 'production' ? 50 : 20,

            // AMI configuration for ARM64 support
            amiType: nodeGroupConfig.amiType,

            // Remote access (disabled for security)
            remoteAccess: undefined,

            // Labels for workload scheduling
            labels: {
                'node-type': 'managed',
                'environment': environment,
                'project': projectName,
                'architecture': nodeGroupConfig.architecture
            },

            // Taints for specialized workloads (if needed)
            taints: nodeGroupConfig.taints,

            // Tags
            tags: {
                Name: `${projectName}-${environment}-node-group`,
                Environment: environment,
                Project: projectName,
                Service: 'EKS-NodeGroup',
                Architecture: nodeGroupConfig.architecture
            }
        });

        // Install essential add-ons
        this.installEKSAddOns(cluster, projectName, environment);

        // Configure RBAC and service accounts
        this.configureRBACAndServiceAccounts(cluster, projectName, environment);

        // Set up horizontal pod autoscaling
        this.configureHorizontalPodAutoscaling(cluster, projectName, environment);

        // Create Kubernetes manifests for the application
        this.createKubernetesManifests(cluster, projectName, environment);

        return cluster;
    }

    private createEKSMastersRole(projectName: string, environment: string): iam.Role {
        const role = new iam.Role(this, 'EKSMastersRole', {
            roleName: `${projectName}-${environment}-eks-masters-role`,
            assumedBy: new iam.AccountRootPrincipal(),
            description: 'IAM role for EKS cluster masters access',
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEKSClusterPolicy')
            ]
        });

        // Add tags to the role
        cdk.Tags.of(role).add('Name', `${projectName}-${environment}-eks-masters-role`);
        cdk.Tags.of(role).add('Environment', environment);
        cdk.Tags.of(role).add('Project', projectName);
        cdk.Tags.of(role).add('Service', 'EKS-IAM');

        return role;
    }

    private getNodeGroupConfiguration(nodeInstanceType: string, environment: string, costOptimization: any): {
        instanceTypes: ec2.InstanceType[];
        capacityType: eks.CapacityType;
        amiType: eks.NodegroupAmiType;
        architecture: string;
        taints?: eks.TaintSpec[];
    } {
        // Determine if we should use ARM64 Graviton3 instances
        const useGraviton = environment === 'production' || nodeInstanceType.includes('m6g') || nodeInstanceType.includes('c6g') || nodeInstanceType.includes('r6g');

        let instanceTypes: ec2.InstanceType[];
        let amiType: eks.NodegroupAmiType;
        let architecture: string;

        if (useGraviton) {
            // Use Graviton3 ARM64 instances for production
            instanceTypes = [
                ec2.InstanceType.of(ec2.InstanceClass.M6G, ec2.InstanceSize.LARGE),
                ec2.InstanceType.of(ec2.InstanceClass.M6G, ec2.InstanceSize.XLARGE)
            ];
            amiType = eks.NodegroupAmiType.AL2_ARM_64;
            architecture = 'arm64';
        } else {
            // Use x86_64 instances for development
            instanceTypes = [
                ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MEDIUM),
                ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.LARGE)
            ];
            amiType = eks.NodegroupAmiType.AL2_X86_64;
            architecture = 'x86_64';
        }

        // Determine capacity type based on cost optimization settings
        const capacityType = costOptimization['spot-instances'] && costOptimization['spot-percentage'] > 0
            ? eks.CapacityType.SPOT
            : eks.CapacityType.ON_DEMAND;

        return {
            instanceTypes,
            capacityType,
            amiType,
            architecture,
            // Add taints for ARM64 nodes to ensure proper scheduling
            taints: useGraviton ? [{
                key: 'kubernetes.io/arch',
                value: 'arm64',
                effect: eks.TaintEffect.NO_SCHEDULE
            }] : undefined
        };
    }

    private installEKSAddOns(cluster: eks.Cluster, projectName: string, environment: string): void {
        // AWS Load Balancer Controller for ALB integration
        cluster.addHelmChart('AWSLoadBalancerController', {
            chart: 'aws-load-balancer-controller',
            repository: 'https://aws.github.io/eks-charts',
            namespace: 'kube-system',
            values: {
                clusterName: cluster.clusterName,
                serviceAccount: {
                    create: false,
                    name: 'aws-load-balancer-controller'
                },
                region: this.region,
                vpcId: cluster.vpc.vpcId
            }
        });

        // Cluster Autoscaler for automatic node scaling
        cluster.addHelmChart('ClusterAutoscaler', {
            chart: 'cluster-autoscaler',
            repository: 'https://kubernetes.github.io/autoscaler',
            namespace: 'kube-system',
            values: {
                autoDiscovery: {
                    clusterName: cluster.clusterName
                },
                awsRegion: this.region,
                rbac: {
                    serviceAccount: {
                        create: false,
                        name: 'cluster-autoscaler'
                    }
                }
            }
        });

        // Metrics Server for HPA
        cluster.addHelmChart('MetricsServer', {
            chart: 'metrics-server',
            repository: 'https://kubernetes-sigs.github.io/metrics-server/',
            namespace: 'kube-system',
            values: {
                args: [
                    '--cert-dir=/tmp',
                    '--secure-port=4443',
                    '--kubelet-preferred-address-types=InternalIP,ExternalIP,Hostname',
                    '--kubelet-use-node-status-port'
                ]
            }
        });
    }

    private configureRBACAndServiceAccounts(cluster: eks.Cluster, projectName: string, environment: string): void {
        // Create service account for AWS Load Balancer Controller
        const albServiceAccount = cluster.addServiceAccount('AWSLoadBalancerControllerServiceAccount', {
            name: 'aws-load-balancer-controller',
            namespace: 'kube-system'
        });

        // Add IAM policy for ALB controller
        albServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'iam:CreateServiceLinkedRole',
                'ec2:DescribeAccountAttributes',
                'ec2:DescribeAddresses',
                'ec2:DescribeAvailabilityZones',
                'ec2:DescribeInternetGateways',
                'ec2:DescribeVpcs',
                'ec2:DescribeSubnets',
                'ec2:DescribeSecurityGroups',
                'ec2:DescribeInstances',
                'ec2:DescribeNetworkInterfaces',
                'ec2:DescribeTags',
                'ec2:GetCoipPoolUsage',
                'ec2:DescribeCoipPools',
                'elasticloadbalancing:DescribeLoadBalancers',
                'elasticloadbalancing:DescribeLoadBalancerAttributes',
                'elasticloadbalancing:DescribeListeners',
                'elasticloadbalancing:DescribeListenerCertificates',
                'elasticloadbalancing:DescribeSSLPolicies',
                'elasticloadbalancing:DescribeRules',
                'elasticloadbalancing:DescribeTargetGroups',
                'elasticloadbalancing:DescribeTargetGroupAttributes',
                'elasticloadbalancing:DescribeTargetHealth',
                'elasticloadbalancing:DescribeTags'
            ],
            resources: ['*']
        }));

        albServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cognito-idp:DescribeUserPoolClient',
                'acm:ListCertificates',
                'acm:DescribeCertificate',
                'iam:ListServerCertificates',
                'iam:GetServerCertificate',
                'waf-regional:GetWebACL',
                'waf-regional:GetWebACLForResource',
                'waf-regional:AssociateWebACL',
                'waf-regional:DisassociateWebACL',
                'wafv2:GetWebACL',
                'wafv2:GetWebACLForResource',
                'wafv2:AssociateWebACL',
                'wafv2:DisassociateWebACL',
                'shield:DescribeProtection',
                'shield:GetSubscriptionState',
                'shield:DescribeSubscription',
                'shield:CreateProtection',
                'shield:DeleteProtection'
            ],
            resources: ['*']
        }));

        albServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'elasticloadbalancing:CreateLoadBalancer',
                'elasticloadbalancing:CreateTargetGroup'
            ],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'elasticloadbalancing:CreateLBTagKeys': 'elbv2.k8s.aws/cluster'
                }
            }
        }));

        // Create service account for Cluster Autoscaler
        const autoscalerServiceAccount = cluster.addServiceAccount('ClusterAutoscalerServiceAccount', {
            name: 'cluster-autoscaler',
            namespace: 'kube-system'
        });

        autoscalerServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'autoscaling:DescribeAutoScalingGroups',
                'autoscaling:DescribeAutoScalingInstances',
                'autoscaling:DescribeLaunchConfigurations',
                'autoscaling:DescribeTags',
                'autoscaling:SetDesiredCapacity',
                'autoscaling:TerminateInstanceInAutoScalingGroup',
                'ec2:DescribeLaunchTemplateVersions'
            ],
            resources: ['*']
        }));

        // Create service account for application workloads
        const appServiceAccount = cluster.addServiceAccount('GenAIDemoAppServiceAccount', {
            name: 'genai-demo-app',
            namespace: 'default'
        });

        // Add permissions for application to access AWS services
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'rds:DescribeDBInstances',
                'rds:DescribeDBClusters',
                'kafka:DescribeCluster',
                'kafka:GetBootstrapBrokers',
                'kafka:ListClusters',
                'secretsmanager:GetSecretValue',
                'secretsmanager:DescribeSecret',
                'ssm:GetParameter',
                'ssm:GetParameters',
                'ssm:GetParametersByPath'
            ],
            resources: ['*']
        }));
    }

    private configureHorizontalPodAutoscaling(cluster: eks.Cluster, projectName: string, environment: string): void {
        // Create HPA configuration for the application
        const hpaManifest = {
            apiVersion: 'autoscaling/v2',
            kind: 'HorizontalPodAutoscaler',
            metadata: {
                name: `${projectName}-hpa`,
                namespace: 'default',
                labels: {
                    app: projectName,
                    environment: environment
                }
            },
            spec: {
                scaleTargetRef: {
                    apiVersion: 'apps/v1',
                    kind: 'Deployment',
                    name: `${projectName}-deployment`
                },
                minReplicas: environment === 'production' ? 2 : 1,
                maxReplicas: environment === 'production' ? 10 : 3,
                metrics: [
                    {
                        type: 'Resource',
                        resource: {
                            name: 'cpu',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 70
                            }
                        }
                    },
                    {
                        type: 'Resource',
                        resource: {
                            name: 'memory',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 80
                            }
                        }
                    }
                ],
                behavior: {
                    scaleUp: {
                        stabilizationWindowSeconds: 60,
                        policies: [
                            {
                                type: 'Percent',
                                value: 100,
                                periodSeconds: 15
                            }
                        ]
                    },
                    scaleDown: {
                        stabilizationWindowSeconds: 300,
                        policies: [
                            {
                                type: 'Percent',
                                value: 10,
                                periodSeconds: 60
                            }
                        ]
                    }
                }
            }
        };

        cluster.addManifest('HorizontalPodAutoscaler', hpaManifest);
    }

    private createKubernetesManifests(cluster: eks.Cluster, projectName: string, environment: string): void {
        // Create namespace for the application
        const namespaceManifest = {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: projectName,
                labels: {
                    name: projectName,
                    environment: environment
                }
            }
        };

        cluster.addManifest('ApplicationNamespace', namespaceManifest);

        // Create deployment manifest
        const deploymentManifest = {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            metadata: {
                name: `${projectName}-deployment`,
                namespace: 'default',
                labels: {
                    app: projectName,
                    environment: environment
                }
            },
            spec: {
                replicas: environment === 'production' ? 2 : 1,
                selector: {
                    matchLabels: {
                        app: projectName
                    }
                },
                template: {
                    metadata: {
                        labels: {
                            app: projectName,
                            environment: environment
                        }
                    },
                    spec: {
                        serviceAccountName: 'genai-demo-app',
                        containers: [
                            {
                                name: projectName,
                                image: `${projectName}:latest`, // Will be updated by CI/CD
                                ports: [
                                    {
                                        containerPort: 8080,
                                        name: 'http'
                                    }
                                ],
                                env: [
                                    {
                                        name: 'SPRING_PROFILES_ACTIVE',
                                        value: environment === 'development' ? 'dev' : 'production'
                                    },
                                    {
                                        name: 'ENVIRONMENT',
                                        value: environment
                                    }
                                ],
                                resources: {
                                    requests: {
                                        cpu: environment === 'production' ? '500m' : '250m',
                                        memory: environment === 'production' ? '1Gi' : '512Mi'
                                    },
                                    limits: {
                                        cpu: environment === 'production' ? '2' : '1',
                                        memory: environment === 'production' ? '2Gi' : '1Gi'
                                    }
                                },
                                livenessProbe: {
                                    httpGet: {
                                        path: '/actuator/health/liveness',
                                        port: 8080
                                    },
                                    initialDelaySeconds: 60,
                                    periodSeconds: 30,
                                    timeoutSeconds: 10,
                                    failureThreshold: 3
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: '/actuator/health/readiness',
                                        port: 8080
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 10,
                                    timeoutSeconds: 5,
                                    failureThreshold: 3
                                },
                                startupProbe: {
                                    httpGet: {
                                        path: '/actuator/health',
                                        port: 8080
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 10,
                                    timeoutSeconds: 5,
                                    failureThreshold: 30
                                }
                            }
                        ],
                        nodeSelector: environment === 'production' ? {
                            'kubernetes.io/arch': 'arm64'
                        } : undefined,
                        tolerations: environment === 'production' ? [
                            {
                                key: 'kubernetes.io/arch',
                                operator: 'Equal',
                                value: 'arm64',
                                effect: 'NoSchedule'
                            }
                        ] : undefined
                    }
                }
            }
        };

        cluster.addManifest('ApplicationDeployment', deploymentManifest);

        // Create service manifest
        const serviceManifest = {
            apiVersion: 'v1',
            kind: 'Service',
            metadata: {
                name: `${projectName}-service`,
                namespace: 'default',
                labels: {
                    app: projectName,
                    environment: environment
                }
            },
            spec: {
                selector: {
                    app: projectName
                },
                ports: [
                    {
                        port: 80,
                        targetPort: 8080,
                        protocol: 'TCP',
                        name: 'http'
                    }
                ],
                type: 'ClusterIP'
            }
        };

        cluster.addManifest('ApplicationService', serviceManifest);

        // Create ingress manifest (if certificate is available)
        const certificate = this.node.tryGetContext('certificate');
        if (certificate) {
            const ingressManifest = {
                apiVersion: 'networking.k8s.io/v1',
                kind: 'Ingress',
                metadata: {
                    name: `${projectName}-ingress`,
                    namespace: 'default',
                    labels: {
                        app: projectName,
                        environment: environment
                    },
                    annotations: {
                        'kubernetes.io/ingress.class': 'alb',
                        'alb.ingress.kubernetes.io/scheme': 'internet-facing',
                        'alb.ingress.kubernetes.io/target-type': 'ip',
                        'alb.ingress.kubernetes.io/certificate-arn': certificate.certificateArn,
                        'alb.ingress.kubernetes.io/ssl-redirect': '443',
                        'alb.ingress.kubernetes.io/listen-ports': '[{"HTTP": 80}, {"HTTPS": 443}]',
                        'alb.ingress.kubernetes.io/healthcheck-path': '/actuator/health',
                        'alb.ingress.kubernetes.io/healthcheck-interval-seconds': '30',
                        'alb.ingress.kubernetes.io/healthcheck-timeout-seconds': '10',
                        'alb.ingress.kubernetes.io/healthy-threshold-count': '2',
                        'alb.ingress.kubernetes.io/unhealthy-threshold-count': '3'
                    }
                },
                spec: {
                    rules: [
                        {
                            http: {
                                paths: [
                                    {
                                        path: '/',
                                        pathType: 'Prefix',
                                        backend: {
                                            service: {
                                                name: `${projectName}-service`,
                                                port: {
                                                    number: 80
                                                }
                                            }
                                        }
                                    }
                                ]
                            }
                        }
                    ]
                }
            };

            cluster.addManifest('ApplicationIngress', ingressManifest);
        }
    }
}