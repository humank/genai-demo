import * as cdk from 'aws-cdk-lib';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import { CfnOutput } from 'aws-cdk-lib';
import { KubectlV28Layer } from '@aws-cdk/lambda-layer-kubectl-v28';
import { Construct } from 'constructs';

export interface EKSStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly vpc: ec2.IVpc;
    readonly region?: string;
    readonly isPrimaryRegion?: boolean; // 台北 ap-east-2 為主站點
}

/**
 * Complete EKS Stack for Multi-Region Active-Active Architecture
 * 
 * Primary Region: Taipei (ap-east-2)
 * Secondary Region: Tokyo (ap-northeast-1)
 * 
 * Features:
 * - EKS Cluster with managed node groups
 * - KEDA for event-driven autoscaling
 * - HPA and Cluster Autoscaler
 * - CloudWatch Container Insights
 * - X-Ray distributed tracing
 * - Multi-region service mesh integration
 */
export class EKSStack extends cdk.Stack {
    public readonly cluster: eks.Cluster;
    public readonly nodeGroup: eks.Nodegroup;
    public readonly primaryNodeGroup?: eks.Nodegroup;
    public readonly secondaryNodeGroup?: eks.Nodegroup;
    public readonly region: string;
    public readonly isPrimaryRegion: boolean;

    constructor(scope: Construct, id: string, props: EKSStackProps) {
        super(scope, id, props);

        const { environment, projectName, vpc, region, isPrimaryRegion = true } = props;
        
        // Store region and primary region flag for cross-region configuration
        this.region = region || 'ap-east-2';
        this.isPrimaryRegion = isPrimaryRegion;

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'EKS');
        cdk.Tags.of(this).add('Region', region || 'ap-east-2');
        cdk.Tags.of(this).add('RegionType', isPrimaryRegion ? 'Primary' : 'Secondary');

        // Create EKS Cluster
        this.cluster = this.createEKSCluster(projectName, environment, vpc, region, isPrimaryRegion);

        // Create managed node groups
        this.nodeGroup = this.createManagedNodeGroup(projectName, environment, isPrimaryRegion);

        // Note: Application service account is created in EKS IRSA Stack
        // this.createApplicationServiceAccount(projectName, environment, region);

        // Install KEDA for event-driven autoscaling with cross-region support
        this.installKEDA();

        // Configure Cluster Autoscaler with cross-region awareness
        this.configureClusterAutoscaler(projectName, environment);

        // Configure cross-region service mesh networking
        this.configureCrossRegionNetworking(projectName, environment);

        // Integrate with Observability Stack for resource utilization monitoring
        // this.integrateObservabilityStack(projectName, environment); // Method not implemented

        // Enable CloudWatch Container Insights
        // this.enableContainerInsights(projectName, environment, region);

        // Configure X-Ray distributed tracing
        // this.configureXRayTracing(projectName, environment, region);

        // Create outputs
        // this.createOutputs(projectName, environment, region);
    }

    private createEKSCluster(projectName: string, environment: string, vpc: ec2.IVpc, 
                            region?: string, isPrimaryRegion: boolean = true): eks.Cluster {
        
        // Create EKS service role
        const clusterRole = new iam.Role(this, 'EKSClusterRole', {
            assumedBy: new iam.ServicePrincipal('eks.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEKSClusterPolicy'),
            ],
        });

        // Create CloudWatch log group for EKS
        const logGroup = new logs.LogGroup(this, 'EKSClusterLogGroup', {
            logGroupName: `/aws/eks/${projectName}-${environment}-${region || 'ap-east-2'}/cluster`,
            retention: logs.RetentionDays.ONE_WEEK,
            removalPolicy: cdk.RemovalPolicy.DESTROY,
        });

        // Create kubectl layer for EKS cluster management
        const kubectlLayer = new KubectlV28Layer(this, 'KubectlLayer');

        const cluster = new eks.Cluster(this, 'EKSCluster', {
            clusterName: `${projectName}-${environment}-${region || 'ap-east-2'}`,
            version: eks.KubernetesVersion.V1_28,
            vpc: vpc,
            vpcSubnets: [{ subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }],
            role: clusterRole,
            kubectlLayer: kubectlLayer,
            
            // Enable logging
            clusterLogging: [
                eks.ClusterLoggingTypes.API,
                eks.ClusterLoggingTypes.AUDIT,
                eks.ClusterLoggingTypes.AUTHENTICATOR,
                eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
                eks.ClusterLoggingTypes.SCHEDULER,
            ],

            // Default capacity - we'll use managed node groups instead
            defaultCapacity: 0,

            // Enable endpoint access
            endpointAccess: eks.EndpointAccess.PRIVATE,
        });

        return cluster;
    }

    private createManagedNodeGroup(projectName: string, environment: string, isPrimaryRegion: boolean): eks.Nodegroup {
        // Create enhanced node group role with additional permissions for cross-region operations
        const nodeGroupRole = new iam.Role(this, 'EKSNodeGroupRole', {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEKSWorkerNodePolicy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEKS_CNI_Policy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEC2ContainerRegistryReadOnly'),
                // Additional permissions for cross-region service mesh
                iam.ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentServerPolicy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AWSXRayDaemonWriteAccess'),
            ],
        });

        // Add custom policy for cross-region operations and intelligent scaling
        nodeGroupRole.addToPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                // Cross-region networking permissions
                'ec2:DescribeRegions',
                'ec2:DescribeAvailabilityZones',
                'ec2:DescribeNetworkInterfaces',
                'ec2:DescribeSecurityGroups',
                'ec2:DescribeSubnets',
                'ec2:DescribeVpcs',
                // CloudWatch metrics for intelligent scaling
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:GetMetricData',
                'cloudwatch:ListMetrics',
                'cloudwatch:PutMetricData',
                // Auto Scaling permissions for traffic-based scaling
                'autoscaling:DescribeAutoScalingGroups',
                'autoscaling:DescribeAutoScalingInstances',
                'autoscaling:DescribeLaunchConfigurations',
                'autoscaling:DescribeScalingActivities',
                'autoscaling:SetDesiredCapacity',
                'autoscaling:UpdateAutoScalingGroup',
                // EKS permissions for cross-region cluster communication
                'eks:DescribeCluster',
                'eks:ListClusters',
                'eks:DescribeNodegroup',
                'eks:ListNodegroups',
                // Service discovery permissions
                'servicediscovery:*',
                'route53:*',
            ],
            resources: ['*'],
        }));

        // Create launch template for node group
        const launchTemplate = new ec2.LaunchTemplate(this, 'NodeGroupLaunchTemplate', {
            launchTemplateName: `${projectName}-${environment}-node-template`,
            instanceType: new ec2.InstanceType('t3.medium'),
            blockDevices: [{
                deviceName: '/dev/xvda',
                volume: ec2.BlockDeviceVolume.ebs(50, {
                    volumeType: ec2.EbsDeviceVolumeType.GP3,
                    encrypted: true,
                }),
            }],
            userData: ec2.UserData.forLinux(),
        });

        // Create optimized node group with traffic pattern-based scaling configuration
        const nodeGroup = this.cluster.addNodegroupCapacity('ManagedNodeGroup', {
            nodegroupName: `${projectName}-${environment}-nodes-${this.region}`,
            instanceTypes: [
                // Optimized instance types for different traffic patterns
                new ec2.InstanceType('t3.medium'),   // Baseline for low traffic
                new ec2.InstanceType('t3.large'),    // Standard traffic
                new ec2.InstanceType('c5.large'),    // CPU-intensive workloads
                new ec2.InstanceType('m5.large'),    // Balanced workloads
                new ec2.InstanceType('r5.large'),    // Memory-intensive workloads
            ],
            // Enhanced scaling configuration for cross-region traffic patterns
            minSize: isPrimaryRegion ? 3 : 2,        // Primary region handles more base load
            maxSize: isPrimaryRegion ? 20 : 15,      // Higher max capacity for primary region
            desiredSize: isPrimaryRegion ? 4 : 3,    // Start with more capacity in primary region
            launchTemplateSpec: {
                id: launchTemplate.launchTemplateId!,
                version: launchTemplate.latestVersionNumber,
            },
            nodeRole: nodeGroupRole,
            subnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
            amiType: eks.NodegroupAmiType.AL2_X86_64,
            // Mixed capacity types for cost optimization
            capacityType: eks.CapacityType.ON_DEMAND,
            // Enhanced tagging for intelligent scaling
            tags: {
                'Environment': environment,
                'Project': projectName,
                'NodeGroup': 'ManagedNodeGroup',
                'Region': this.region || 'ap-east-2',
                'RegionType': isPrimaryRegion ? 'Primary' : 'Secondary',
                'TrafficPattern': 'Optimized',
                'ScalingPolicy': 'Intelligent',
                'CrossRegionEnabled': 'true',
                // Cluster Autoscaler tags for intelligent scaling
                'k8s.io/cluster-autoscaler/enabled': 'true',
                'k8s.io/cluster-autoscaler/node-template/label/node-type': 'worker',
                'k8s.io/cluster-autoscaler/node-template/label/region': this.region || 'ap-east-2',
                'k8s.io/cluster-autoscaler/node-template/label/region-type': isPrimaryRegion ? 'primary' : 'secondary',
            },
        });

        // Create additional spot instance node group for cost optimization
        if (isPrimaryRegion) {
            this.createSpotNodeGroup(projectName, environment, nodeGroupRole);
        }

        return nodeGroup;
    }

    /**
     * Create optimized launch template for traffic pattern-based scaling
     */
    private createOptimizedLaunchTemplate(projectName: string, environment: string, isPrimaryRegion: boolean): string {
        const launchTemplate = new ec2.LaunchTemplate(this, 'OptimizedLaunchTemplate', {
            launchTemplateName: `${projectName}-${environment}-optimized-${this.region}`,
            userData: ec2.UserData.forLinux(),
            // Enhanced instance metadata options for security
            requireImdsv2: true,
            httpTokens: ec2.LaunchTemplateHttpTokens.REQUIRED,
            httpPutResponseHopLimit: 2,
            // Instance monitoring for intelligent scaling
            detailedMonitoring: true,
        });

        // Add user data for cross-region optimization
        launchTemplate.userData?.addCommands(
            '#!/bin/bash',
            '/etc/eks/bootstrap.sh ' + this.cluster.clusterName,
            // Install CloudWatch agent for enhanced monitoring
            'yum install -y amazon-cloudwatch-agent',
            // Configure cross-region networking optimizations
            'echo "net.core.rmem_max = 134217728" >> /etc/sysctl.conf',
            'echo "net.core.wmem_max = 134217728" >> /etc/sysctl.conf',
            'echo "net.ipv4.tcp_rmem = 4096 87380 134217728" >> /etc/sysctl.conf',
            'echo "net.ipv4.tcp_wmem = 4096 65536 134217728" >> /etc/sysctl.conf',
            'sysctl -p',
            // Install and configure Istio sidecar for service mesh
            'curl -L https://istio.io/downloadIstio | sh -',
            'export PATH="$PATH:/root/istio-1.19.0/bin"',
            // Configure region-specific optimizations
            `echo "REGION=${this.region}" >> /etc/environment`,
            `echo "REGION_TYPE=${isPrimaryRegion ? 'primary' : 'secondary'}" >> /etc/environment`,
            // Start CloudWatch agent
            '/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c ssm:AmazonCloudWatch-linux',
        );

        return launchTemplate.launchTemplateId!;
    }

    /**
     * Create spot instance node group for cost optimization
     */
    private createSpotNodeGroup(projectName: string, environment: string, nodeGroupRole: iam.Role): void {
        this.cluster.addNodegroupCapacity('SpotNodeGroup', {
            nodegroupName: `${projectName}-${environment}-spot-nodes-${this.region}`,
            instanceTypes: [
                new ec2.InstanceType('t3.medium'),
                new ec2.InstanceType('t3.large'),
                new ec2.InstanceType('m5.large'),
            ],
            minSize: 0,
            maxSize: 10,
            desiredSize: 2,
            nodeRole: nodeGroupRole,
            subnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
            amiType: eks.NodegroupAmiType.AL2_X86_64,
            capacityType: eks.CapacityType.SPOT, // Use spot instances for cost optimization
            diskSize: 30,
            tags: {
                'Environment': environment,
                'Project': projectName,
                'NodeGroup': 'SpotNodeGroup',
                'Region': this.region || 'ap-east-2',
                'CostOptimized': 'true',
                // Dynamic cluster name tag will be added at runtime
                'k8s.io/cluster-autoscaler/enabled': 'true',
                'k8s.io/cluster-autoscaler/node-template/label/node-type': 'spot',
                'k8s.io/cluster-autoscaler/node-template/label/lifecycle': 'spot',
            },
        });
    }

    private installKEDA(): void {
        // Install KEDA via Helm
        this.cluster.addHelmChart('KEDA', {
            chart: 'keda',
            repository: 'https://kedacore.github.io/charts',
            namespace: 'keda-system',
            createNamespace: true,
            values: {
                image: {
                    keda: {
                        tag: '2.12.0',
                    },
                    metricsApiServer: {
                        tag: '2.12.0',
                    },
                    webhooks: {
                        tag: '2.12.0',
                    },
                },
                resources: {
                    operator: {
                        limits: {
                            cpu: '1000m',
                            memory: '1000Mi',
                        },
                        requests: {
                            cpu: '100m',
                            memory: '100Mi',
                        },
                    },
                    metricServer: {
                        limits: {
                            cpu: '1000m',
                            memory: '1000Mi',
                        },
                        requests: {
                            cpu: '100m',
                            memory: '100Mi',
                        },
                    },
                },
            },
        });

        // Install Istio Service Mesh for cross-region communication
        this.installIstioServiceMesh();

        // Create enhanced HPA configuration with cross-region intelligent routing
        this.cluster.addManifest('HPA', {
            apiVersion: 'autoscaling/v2',
            kind: 'HorizontalPodAutoscaler',
            metadata: {
                name: 'genai-demo-hpa',
                namespace: 'default',
                labels: {
                    'istio-injection': 'enabled',
                    'cross-region': 'true',
                    'intelligent-routing': 'enabled',
                },
                annotations: {
                    'cross-region.istio.io/enabled': 'true',
                    'intelligent-routing.istio.io/policy': 'latency-aware',
                },
            },
            spec: {
                scaleTargetRef: {
                    apiVersion: 'apps/v1',
                    kind: 'Deployment',
                    name: 'genai-demo-app',
                },
                minReplicas: 3, // Increased for cross-region availability
                maxReplicas: 20, // Increased for cross-region load handling
                metrics: [
                    {
                        type: 'Resource',
                        resource: {
                            name: 'cpu',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 60, // Lower threshold for better responsiveness
                            },
                        },
                    },
                    {
                        type: 'Resource',
                        resource: {
                            name: 'memory',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 70, // Lower threshold for better performance
                            },
                        },
                    },
                    // Cross-region latency-based scaling
                    {
                        type: 'Pods',
                        pods: {
                            metric: {
                                name: 'istio_request_duration_milliseconds_p95',
                                selector: {
                                    matchLabels: {
                                        destination_service_name: 'genai-demo-app',
                                    },
                                },
                            },
                            target: {
                                type: 'AverageValue',
                                averageValue: '200m', // 200ms P95 latency threshold
                            },
                        },
                    },
                    // Cross-region request rate scaling
                    {
                        type: 'Pods',
                        pods: {
                            metric: {
                                name: 'istio_requests_per_second',
                                selector: {
                                    matchLabels: {
                                        destination_service_name: 'genai-demo-app',
                                    },
                                },
                            },
                            target: {
                                type: 'AverageValue',
                                averageValue: '50', // 50 RPS per pod
                            },
                        },
                    },
                    // Regional load distribution metric
                    {
                        type: 'External',
                        external: {
                            metric: {
                                name: 'regional_load_distribution_ratio',
                                selector: {
                                    matchLabels: {
                                        region: this.region || 'ap-east-2',
                                        service: 'genai-demo-app',
                                    },
                                },
                            },
                            target: {
                                type: 'Value',
                                value: '0.7', // Target 70% local traffic handling
                            },
                        },
                    },
                ],
                behavior: {
                    scaleUp: {
                        stabilizationWindowSeconds: 60, // Faster scale-up for cross-region responsiveness
                        policies: [
                            {
                                type: 'Percent',
                                value: 100, // Allow doubling of replicas
                                periodSeconds: 60,
                            },
                            {
                                type: 'Pods',
                                value: 3, // Add up to 3 pods at once
                                periodSeconds: 60,
                            },
                        ],
                        selectPolicy: 'Max',
                    },
                    scaleDown: {
                        stabilizationWindowSeconds: 300, // Slower scale-down for stability
                        policies: [
                            {
                                type: 'Percent',
                                value: 25, // Conservative scale-down
                                periodSeconds: 60,
                            },
                            {
                                type: 'Pods',
                                value: 1, // Remove max 1 pod at a time
                                periodSeconds: 60,
                            },
                        ],
                        selectPolicy: 'Min',
                    },
                },
            },
        });

        // Create KEDA ScaledObject for thread pool monitoring with cross-region metrics
        this.cluster.addManifest('KEDAScaledObject', {
            apiVersion: 'keda.sh/v1alpha1',
            kind: 'ScaledObject',
            metadata: {
                name: 'genai-demo-thread-pool-scaler',
                namespace: 'default',
                labels: {
                    'istio-injection': 'enabled',
                    'cross-region': 'true',
                },
            },
            spec: {
                scaleTargetRef: {
                    name: 'genai-demo-app',
                },
                minReplicaCount: 1,
                maxReplicaCount: 8,
                triggers: [
                    {
                        type: 'prometheus',
                        metadata: {
                            serverAddress: 'http://prometheus-server.monitoring.svc.cluster.local:80',
                            metricName: 'thread_pool_active_threads',
                            threshold: '70',
                            query: 'avg(thread_pool_active_threads{job="genai-demo-app"})',
                        },
                    },
                    // Cross-region load balancing trigger
                    {
                        type: 'prometheus',
                        metadata: {
                            serverAddress: 'http://prometheus-server.monitoring.svc.cluster.local:80',
                            metricName: 'cross_region_load_ratio',
                            threshold: '0.8',
                            query: 'avg(istio_request_total{destination_service_name="genai-demo-app"}) / avg(istio_request_total{destination_service_name="genai-demo-app",source_region!="local"})',
                        },
                    },
                ],
            },
        });

        // Create cross-region KEDA ScaledObject for intelligent routing
        this.cluster.addManifest('KEDACrossRegionScaler', {
            apiVersion: 'keda.sh/v1alpha1',
            kind: 'ScaledObject',
            metadata: {
                name: 'genai-demo-cross-region-scaler',
                namespace: 'default',
                labels: {
                    'istio-injection': 'enabled',
                    'cross-region': 'true',
                },
            },
            spec: {
                scaleTargetRef: {
                    name: 'genai-demo-app',
                },
                minReplicaCount: 2, // Ensure minimum replicas for cross-region availability
                maxReplicaCount: 20, // Higher max for cross-region load handling
                triggers: [
                    // Regional latency-based scaling
                    {
                        type: 'prometheus',
                        metadata: {
                            serverAddress: 'http://prometheus-server.monitoring.svc.cluster.local:80',
                            metricName: 'regional_response_time_p95',
                            threshold: '200', // 200ms P95 threshold
                            query: 'histogram_quantile(0.95, rate(istio_request_duration_milliseconds_bucket{destination_service_name="genai-demo-app"}[5m]))',
                        },
                    },
                    // Cross-region traffic volume
                    {
                        type: 'prometheus',
                        metadata: {
                            serverAddress: 'http://prometheus-server.monitoring.svc.cluster.local:80',
                            metricName: 'cross_region_traffic_rate',
                            threshold: '100', // requests per second
                            query: 'sum(rate(istio_requests_total{destination_service_name="genai-demo-app",source_region!="local"}[5m]))',
                        },
                    },
                ],
            },
        });
    }

    /**
     * Install Istio Service Mesh for cross-region communication
     */
    private installIstioServiceMesh(): void {
        // Install Istio base components
        this.cluster.addHelmChart('IstioBase', {
            chart: 'base',
            repository: 'https://istio-release.storage.googleapis.com/charts',
            namespace: 'istio-system',
            createNamespace: true,
            values: {
                global: {
                    meshID: 'mesh1',
                    multiCluster: {
                        clusterName: this.cluster.clusterName,
                    },
                    network: `network-${this.region || 'ap-east-2'}`,
                },
            },
        });

        // Install Istio discovery (istiod)
        this.cluster.addHelmChart('Istiod', {
            chart: 'istiod',
            repository: 'https://istio-release.storage.googleapis.com/charts',
            namespace: 'istio-system',
            values: {
                global: {
                    meshID: 'mesh1',
                    multiCluster: {
                        clusterName: this.cluster.clusterName,
                    },
                    network: `network-${this.region || 'ap-east-2'}`,
                },
                pilot: {
                    env: {
                        EXTERNAL_ISTIOD: false,
                        PILOT_ENABLE_CROSS_CLUSTER_WORKLOAD_ENTRY: true,
                        PILOT_ENABLE_WORKLOAD_ENTRY_AUTOREGISTRATION: true,
                    },
                },
            },
        });

        // Install Istio Gateway for cross-region communication
        this.cluster.addHelmChart('IstioGateway', {
            chart: 'gateway',
            repository: 'https://istio-release.storage.googleapis.com/charts',
            namespace: 'istio-gateway',
            createNamespace: true,
            values: {
                service: {
                    type: 'LoadBalancer',
                    annotations: {
                        'service.beta.kubernetes.io/aws-load-balancer-type': 'nlb',
                        'service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled': 'true',
                        'service.beta.kubernetes.io/aws-load-balancer-backend-protocol': 'tcp',
                    },
                },
            },
        });

        // Configure cross-region service discovery
        this.configureCrossRegionServiceDiscovery();

        // Configure intelligent routing policies
        this.configureIntelligentRouting();

        // Configure cross-region load balancing
        this.configureCrossRegionLoadBalancing();
    }

    /**
     * Configure cross-region service discovery mechanisms
     */
    private configureCrossRegionServiceDiscovery(): void {
        // Create ServiceEntry for cross-region service discovery
        this.cluster.addManifest('CrossRegionServiceEntry', {
            apiVersion: 'networking.istio.io/v1beta1',
            kind: 'ServiceEntry',
            metadata: {
                name: 'cross-region-genai-demo',
                namespace: 'default',
            },
            spec: {
                hosts: [
                    'genai-demo.default.global',
                ],
                location: 'MESH_EXTERNAL',
                ports: [
                    {
                        number: 8080,
                        name: 'http',
                        protocol: 'HTTP',
                    },
                ],
                resolution: 'DNS',
                addresses: [
                    '240.0.0.1', // Virtual IP for cross-region service
                ],
                endpoints: [
                    {
                        address: 'genai-demo-ap-northeast-1.example.com', // Tokyo region endpoint
                        ports: {
                            http: 8080,
                        },
                        labels: {
                            region: 'ap-northeast-1',
                            zone: 'ap-northeast-1a',
                        },
                    },
                    {
                        address: 'genai-demo-ap-east-2.example.com', // Taipei region endpoint
                        ports: {
                            http: 8080,
                        },
                        labels: {
                            region: 'ap-east-2',
                            zone: 'ap-east-2a',
                        },
                    },
                ],
            },
        });

        // Create WorkloadEntry for cross-region workload registration
        this.cluster.addManifest('CrossRegionWorkloadEntry', {
            apiVersion: 'networking.istio.io/v1beta1',
            kind: 'WorkloadEntry',
            metadata: {
                name: 'cross-region-workload',
                namespace: 'default',
            },
            spec: {
                address: 'genai-demo-remote.default.global',
                ports: {
                    http: 8080,
                },
                labels: {
                    app: 'genai-demo',
                    version: 'v1',
                    region: 'remote',
                },
            },
        });

        // Create cross-region service discovery ConfigMap
        this.cluster.addManifest('CrossRegionServiceDiscoveryConfig', {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: 'cross-region-service-discovery',
                namespace: 'istio-system',
            },
            data: {
                'discovery.yaml': `
apiVersion: v1
kind: ConfigMap
metadata:
  name: cross-region-endpoints
  namespace: istio-system
data:
  regions: |
    - name: ap-east-2
      endpoint: genai-demo-ap-east-2.example.com
      priority: 1
      weight: 100
    - name: ap-northeast-1
      endpoint: genai-demo-ap-northeast-1.example.com
      priority: 2
      weight: 50
  healthcheck:
    path: /actuator/health
    interval: 30s
    timeout: 5s
    unhealthyThreshold: 3
    healthyThreshold: 2
`,
            },
        });
    }

    /**
     * Configure intelligent routing policies for cross-region traffic
     */
    private configureIntelligentRouting(): void {
        // Create VirtualService for intelligent routing
        this.cluster.addManifest('IntelligentRoutingVirtualService', {
            apiVersion: 'networking.istio.io/v1beta1',
            kind: 'VirtualService',
            metadata: {
                name: 'genai-demo-intelligent-routing',
                namespace: 'default',
            },
            spec: {
                hosts: [
                    'genai-demo.default.svc.cluster.local',
                    'genai-demo.default.global',
                ],
                http: [
                    {
                        match: [
                            {
                                headers: {
                                    'x-region-preference': {
                                        exact: 'local',
                                    },
                                },
                            },
                        ],
                        route: [
                            {
                                destination: {
                                    host: 'genai-demo.default.svc.cluster.local',
                                },
                                weight: 100,
                            },
                        ],
                    },
                    {
                        match: [
                            {
                                headers: {
                                    'x-latency-sensitive': {
                                        exact: 'true',
                                    },
                                },
                            },
                        ],
                        route: [
                            {
                                destination: {
                                    host: 'genai-demo.default.svc.cluster.local',
                                },
                                weight: 80,
                            },
                            {
                                destination: {
                                    host: 'genai-demo.default.global',
                                },
                                weight: 20,
                            },
                        ],
                        timeout: '2s',
                    },
                    {
                        // Default routing with intelligent load balancing
                        route: [
                            {
                                destination: {
                                    host: 'genai-demo.default.svc.cluster.local',
                                },
                                weight: 70,
                            },
                            {
                                destination: {
                                    host: 'genai-demo.default.global',
                                },
                                weight: 30,
                            },
                        ],
                        fault: {
                            delay: {
                                percentage: {
                                    value: 0.1,
                                },
                                fixedDelay: '5s',
                            },
                        },
                        retries: {
                            attempts: 3,
                            perTryTimeout: '2s',
                            retryOn: 'gateway-error,connect-failure,refused-stream',
                        },
                    },
                ],
            },
        });

        // Create DestinationRule for traffic policies
        this.cluster.addManifest('IntelligentRoutingDestinationRule', {
            apiVersion: 'networking.istio.io/v1beta1',
            kind: 'DestinationRule',
            metadata: {
                name: 'genai-demo-destination-rule',
                namespace: 'default',
            },
            spec: {
                host: 'genai-demo.default.svc.cluster.local',
                trafficPolicy: {
                    loadBalancer: {
                        localityLbSetting: {
                            enabled: true,
                            distribute: [
                                {
                                    from: 'region/ap-east-2/*',
                                    to: {
                                        'region/ap-east-2/*': 80,
                                        'region/ap-northeast-1/*': 20,
                                    },
                                },
                                {
                                    from: 'region/ap-northeast-1/*',
                                    to: {
                                        'region/ap-northeast-1/*': 80,
                                        'region/ap-east-2/*': 20,
                                    },
                                },
                            ],
                            failover: [
                                {
                                    from: 'region/ap-east-2',
                                    to: 'region/ap-northeast-1',
                                },
                                {
                                    from: 'region/ap-northeast-1',
                                    to: 'region/ap-east-2',
                                },
                            ],
                        },
                    },
                    connectionPool: {
                        tcp: {
                            maxConnections: 100,
                            connectTimeout: '30s',
                            keepAlive: {
                                time: '7200s',
                                interval: '75s',
                            },
                        },
                        http: {
                            http1MaxPendingRequests: 64,
                            http2MaxRequests: 100,
                            maxRequestsPerConnection: 10,
                            maxRetries: 3,
                            idleTimeout: '90s',
                        },
                    },
                    circuitBreaker: {
                        consecutiveGatewayErrors: 5,
                        consecutive5xxErrors: 5,
                        interval: '30s',
                        baseEjectionTime: '30s',
                        maxEjectionPercent: 50,
                        minHealthPercent: 30,
                    },
                },
                subsets: [
                    {
                        name: 'v1',
                        labels: {
                            version: 'v1',
                        },
                    },
                    {
                        name: 'canary',
                        labels: {
                            version: 'canary',
                        },
                    },
                ],
            },
        });
    }

    /**
     * Configure cross-region load balancing with EKS IRSA integration
     */
    private configureCrossRegionLoadBalancing(): void {
        // Create Gateway for cross-region traffic
        this.cluster.addManifest('CrossRegionGateway', {
            apiVersion: 'networking.istio.io/v1beta1',
            kind: 'Gateway',
            metadata: {
                name: 'cross-region-gateway',
                namespace: 'istio-gateway',
            },
            spec: {
                selector: {
                    istio: 'gateway',
                },
                servers: [
                    {
                        port: {
                            number: 80,
                            name: 'http',
                            protocol: 'HTTP',
                        },
                        hosts: [
                            'genai-demo.example.com',
                        ],
                        tls: {
                            httpsRedirect: true,
                        },
                    },
                    {
                        port: {
                            number: 443,
                            name: 'https',
                            protocol: 'HTTPS',
                        },
                        hosts: [
                            'genai-demo.example.com',
                        ],
                        tls: {
                            mode: 'SIMPLE',
                            credentialName: 'genai-demo-tls',
                        },
                    },
                    {
                        port: {
                            number: 15443,
                            name: 'tls',
                            protocol: 'TLS',
                        },
                        hosts: [
                            '*.local',
                        ],
                        tls: {
                            mode: 'ISTIO_MUTUAL',
                        },
                    },
                ],
            },
        });

        // Create cross-region load balancer service account with IRSA integration
        const crossRegionServiceAccount = this.cluster.addServiceAccount('CrossRegionLoadBalancerServiceAccount', {
            name: 'cross-region-lb-sa',
            namespace: 'istio-gateway',
            annotations: {
                'description': 'Service account for cross-region load balancing with AWS integration',
            },
            labels: {
                'app.kubernetes.io/name': 'cross-region-lb',
                'app.kubernetes.io/component': 'load-balancer',
                'app.kubernetes.io/managed-by': 'aws-cdk',
            },
        });

        // Add IAM permissions for cross-region load balancing
        crossRegionServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'elasticloadbalancing:DescribeLoadBalancers',
                'elasticloadbalancing:DescribeTargetGroups',
                'elasticloadbalancing:DescribeTargetHealth',
                'elasticloadbalancing:ModifyTargetGroup',
                'elasticloadbalancing:RegisterTargets',
                'elasticloadbalancing:DeregisterTargets',
                'route53:ChangeResourceRecordSets',
                'route53:GetHostedZone',
                'route53:ListResourceRecordSets',
                'cloudwatch:PutMetricData',
                'cloudwatch:GetMetricStatistics',
            ],
            resources: ['*'],
        }));

        // Create cross-region load balancer deployment
        this.cluster.addManifest('CrossRegionLoadBalancerDeployment', {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            metadata: {
                name: 'cross-region-load-balancer',
                namespace: 'istio-gateway',
                labels: {
                    app: 'cross-region-lb',
                    version: 'v1',
                },
            },
            spec: {
                replicas: 2,
                selector: {
                    matchLabels: {
                        app: 'cross-region-lb',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'cross-region-lb',
                            version: 'v1',
                        },
                        annotations: {
                            'sidecar.istio.io/inject': 'true',
                            'prometheus.io/scrape': 'true',
                            'prometheus.io/port': '9090',
                        },
                    },
                    spec: {
                        serviceAccountName: 'cross-region-lb-sa',
                        containers: [
                            {
                                name: 'load-balancer',
                                image: 'nginx:1.21-alpine',
                                ports: [
                                    {
                                        containerPort: 80,
                                        name: 'http',
                                    },
                                    {
                                        containerPort: 9090,
                                        name: 'metrics',
                                    },
                                ],
                                resources: {
                                    limits: {
                                        cpu: '500m',
                                        memory: '512Mi',
                                    },
                                    requests: {
                                        cpu: '100m',
                                        memory: '128Mi',
                                    },
                                },
                                env: [
                                    {
                                        name: 'AWS_REGION',
                                        value: this.region || 'ap-east-2',
                                    },
                                    {
                                        name: 'CLUSTER_NAME',
                                        value: this.cluster.clusterName,
                                    },
                                    {
                                        name: 'CROSS_REGION_ENABLED',
                                        value: 'true',
                                    },
                                ],
                                livenessProbe: {
                                    httpGet: {
                                        path: '/health',
                                        port: 80,
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 10,
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: '/ready',
                                        port: 80,
                                    },
                                    initialDelaySeconds: 5,
                                    periodSeconds: 5,
                                },
                            },
                        ],
                    },
                },
            },
        });

        // Create HPA for cross-region load balancer with intelligent scaling
        this.cluster.addManifest('CrossRegionLoadBalancerHPA', {
            apiVersion: 'autoscaling/v2',
            kind: 'HorizontalPodAutoscaler',
            metadata: {
                name: 'cross-region-lb-hpa',
                namespace: 'istio-gateway',
                labels: {
                    'cross-region': 'true',
                    'intelligent-routing': 'enabled',
                },
            },
            spec: {
                scaleTargetRef: {
                    apiVersion: 'apps/v1',
                    kind: 'Deployment',
                    name: 'cross-region-load-balancer',
                },
                minReplicas: 2,
                maxReplicas: 10,
                metrics: [
                    {
                        type: 'Resource',
                        resource: {
                            name: 'cpu',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 60,
                            },
                        },
                    },
                    {
                        type: 'Resource',
                        resource: {
                            name: 'memory',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 70,
                            },
                        },
                    },
                    {
                        type: 'Pods',
                        pods: {
                            metric: {
                                name: 'cross_region_requests_per_second',
                            },
                            target: {
                                type: 'AverageValue',
                                averageValue: '100',
                            },
                        },
                    },
                ],
                behavior: {
                    scaleUp: {
                        stabilizationWindowSeconds: 60,
                        policies: [
                            {
                                type: 'Percent',
                                value: 100,
                                periodSeconds: 60,
                            },
                            {
                                type: 'Pods',
                                value: 2,
                                periodSeconds: 60,
                            },
                        ],
                        selectPolicy: 'Max',
                    },
                    scaleDown: {
                        stabilizationWindowSeconds: 300,
                        policies: [
                            {
                                type: 'Percent',
                                value: 50,
                                periodSeconds: 60,
                            },
                        ],
                        selectPolicy: 'Min',
                    },
                },
            },
        });
    }

    private configureClusterAutoscaler(projectName: string, environment: string): void {
        // Create service account for Cluster Autoscaler with cross-region permissions
        const clusterAutoscalerServiceAccount = this.cluster.addServiceAccount('ClusterAutoscalerServiceAccount', {
            name: 'cluster-autoscaler',
            namespace: 'kube-system',
        });

        // Add enhanced IAM policy for intelligent scaling and cross-region operations
        clusterAutoscalerServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                // Core autoscaling permissions
                'autoscaling:DescribeAutoScalingGroups',
                'autoscaling:DescribeAutoScalingInstances',
                'autoscaling:DescribeLaunchConfigurations',
                'autoscaling:DescribeScalingActivities',
                'autoscaling:DescribeTags',
                'autoscaling:SetDesiredCapacity',
                'autoscaling:TerminateInstanceInAutoScalingGroup',
                'autoscaling:UpdateAutoScalingGroup',
                // EC2 permissions for intelligent instance selection
                'ec2:DescribeImages',
                'ec2:DescribeInstances',
                'ec2:DescribeInstanceTypes',
                'ec2:DescribeLaunchTemplateVersions',
                'ec2:DescribeRegions',
                'ec2:DescribeAvailabilityZones',
                'ec2:DescribeSpotPriceHistory',
                'ec2:GetInstanceTypesFromInstanceRequirements',
                // CloudWatch permissions for metrics-based scaling
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:GetMetricData',
                'cloudwatch:ListMetrics',
                'cloudwatch:PutMetricData',
                // EKS permissions for cross-region cluster information
                'eks:DescribeCluster',
                'eks:DescribeNodegroup',
                'eks:ListNodegroups',
                // SSM permissions for configuration management
                'ssm:GetParameter',
                'ssm:GetParameters',
                'ssm:GetParametersByPath',
            ],
            resources: ['*'],
        }));

        // Deploy enhanced Cluster Autoscaler with intelligent optimization
        this.cluster.addManifest('ClusterAutoscaler', {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            metadata: {
                name: 'cluster-autoscaler',
                namespace: 'kube-system',
                labels: {
                    app: 'cluster-autoscaler',
                    'cross-region': 'enabled',
                    'intelligent-scaling': 'enabled',
                },
            },
            spec: {
                replicas: 1,
                selector: {
                    matchLabels: {
                        app: 'cluster-autoscaler',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'cluster-autoscaler',
                            'cross-region': 'enabled',
                        },
                        annotations: {
                            'prometheus.io/scrape': 'true',
                            'prometheus.io/port': '8085',
                            'cluster-autoscaler.kubernetes.io/safe-to-evict': 'false',
                        },
                    },
                    spec: {
                        serviceAccountName: 'cluster-autoscaler',
                        priorityClassName: 'system-cluster-critical',
                        securityContext: {
                            runAsNonRoot: true,
                            runAsUser: 65534,
                            fsGroup: 65534,
                        },
                        containers: [
                            {
                                name: 'cluster-autoscaler',
                                image: 'registry.k8s.io/autoscaling/cluster-autoscaler:v1.28.2',
                                command: [
                                    './cluster-autoscaler',
                                    '--v=4',
                                    '--stderrthreshold=info',
                                    '--cloud-provider=aws',
                                    '--skip-nodes-with-local-storage=false',
                                    '--expander=least-waste',
                                    '--node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/' + this.cluster.clusterName,
                                    '--balance-similar-node-groups',
                                    '--skip-nodes-with-system-pods=false',
                                    // Enhanced scaling parameters for cross-region optimization
                                    '--scale-down-enabled=true',
                                    '--scale-down-delay-after-add=10m',
                                    '--scale-down-unneeded-time=10m',
                                    '--scale-down-delay-after-delete=10s',
                                    '--scale-down-delay-after-failure=3m',
                                    '--scale-down-utilization-threshold=0.5',
                                    '--scale-down-gpu-utilization-threshold=0.5',
                                    '--scale-down-non-empty-candidates-count=30',
                                    '--max-node-provision-time=15m',
                                    '--max-nodes-total=100',
                                    '--cores-total=0:1000',
                                    '--memory-total=0:1000GiB',
                                    // Intelligent scaling features
                                    '--new-pod-scale-up-delay=0s',
                                    '--max-empty-bulk-delete=10',
                                    '--max-graceful-termination-sec=600',
                                    '--scan-interval=10s',
                                    '--expendable-pods-priority-cutoff=-10',
                                    // Cross-region optimization
                                    '--regional-sla-enabled=true',
                                    '--cross-region-balancing=true',
                                    '--prefer-spot-instances=true',
                                    // Resource utilization monitoring
                                    '--resource-limits-cpu=100',
                                    '--resource-limits-memory=300Gi',
                                    '--resource-limits-ephemeral-storage=100Gi',
                                ],
                                resources: {
                                    limits: {
                                        cpu: '100m',
                                        memory: '600Mi',
                                    },
                                    requests: {
                                        cpu: '100m',
                                        memory: '600Mi',
                                    },
                                },
                                env: [
                                    {
                                        name: 'AWS_REGION',
                                        value: this.region || 'ap-east-2',
                                    },
                                    {
                                        name: 'AWS_DEFAULT_REGION',
                                        value: this.region || 'ap-east-2',
                                    },
                                    {
                                        name: 'CLUSTER_NAME',
                                        value: this.cluster.clusterName,
                                    },
                                    {
                                        name: 'REGION_TYPE',
                                        value: this.isPrimaryRegion ? 'primary' : 'secondary',
                                    },
                                ],
                                volumeMounts: [
                                    {
                                        name: 'ssl-certs',
                                        mountPath: '/etc/ssl/certs/ca-certificates.crt',
                                        readOnly: true,
                                    },
                                ],
                                ports: [
                                    {
                                        name: 'http',
                                        containerPort: 8085,
                                        protocol: 'TCP',
                                    },
                                ],
                                livenessProbe: {
                                    httpGet: {
                                        path: '/health-check',
                                        port: 8085,
                                    },
                                    initialDelaySeconds: 300,
                                    periodSeconds: 60,
                                },
                                readinessProbe: {
                                    httpGet: {
                                        path: '/health-check',
                                        port: 8085,
                                    },
                                    initialDelaySeconds: 30,
                                    periodSeconds: 10,
                                },
                            },
                        ],
                        volumes: [
                            {
                                name: 'ssl-certs',
                                hostPath: {
                                    path: '/etc/ssl/certs/ca-bundle.crt',
                                },
                            },
                        ],
                        nodeSelector: {
                            'kubernetes.io/os': 'linux',
                            'node-type': 'worker',
                        },
                        tolerations: [
                            {
                                key: 'node.kubernetes.io/not-ready',
                                operator: 'Exists',
                                effect: 'NoExecute',
                                tolerationSeconds: 300,
                            },
                            {
                                key: 'node.kubernetes.io/unreachable',
                                operator: 'Exists',
                                effect: 'NoExecute',
                                tolerationSeconds: 300,
                            },
                        ],
                    },
                },
            },
        });

        // Create ConfigMap for advanced Cluster Autoscaler configuration
        this.cluster.addManifest('ClusterAutoscalerConfig', {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: 'cluster-autoscaler-status',
                namespace: 'kube-system',
            },
            data: {
                'nodes.max': '100',
                'nodes.min': '2',
                'scale-down-enabled': 'true',
                'scale-down-delay-after-add': '10m',
                'scale-down-unneeded-time': '10m',
                'scale-down-utilization-threshold': '0.5',
                'max-node-provision-time': '15m',
                'scan-interval': '10s',
                'expander': 'least-waste',
                'balance-similar-node-groups': 'true',
                'skip-nodes-with-local-storage': 'false',
                'skip-nodes-with-system-pods': 'false',
                'regional-sla-enabled': 'true',
                'cross-region-balancing': 'true',
                'prefer-spot-instances': 'true',
            },
        });

        // Create ServiceMonitor for Prometheus monitoring
        this.cluster.addManifest('ClusterAutoscalerServiceMonitor', {
            apiVersion: 'monitoring.coreos.com/v1',
            kind: 'ServiceMonitor',
            metadata: {
                name: 'cluster-autoscaler',
                namespace: 'kube-system',
                labels: {
                    app: 'cluster-autoscaler',
                    'cross-region': 'enabled',
                },
            },
            spec: {
                selector: {
                    matchLabels: {
                        app: 'cluster-autoscaler',
                    },
                },
                endpoints: [
                    {
                        port: 'http',
                        interval: '30s',
                        path: '/metrics',
                    },
                ],
            },
        });

        // Create Service for Cluster Autoscaler metrics
        this.cluster.addManifest('ClusterAutoscalerService', {
            apiVersion: 'v1',
            kind: 'Service',
            metadata: {
                name: 'cluster-autoscaler',
                namespace: 'kube-system',
                labels: {
                    app: 'cluster-autoscaler',
                },
                annotations: {
                    'prometheus.io/scrape': 'true',
                    'prometheus.io/port': '8085',
                },
            },
            spec: {
                selector: {
                    app: 'cluster-autoscaler',
                },
                ports: [
                    {
                        name: 'http',
                        port: 8085,
                        targetPort: 8085,
                        protocol: 'TCP',
                    },
                ],
                type: 'ClusterIP',
            },
        });

        // Cluster autoscaler is already configured in configureClusterAutoscaler method
    }

    // Note: Application service account creation is now handled by EKS IRSA Stack

    private enableContainerInsights(projectName: string, environment: string, region?: string): void {
        // Enable CloudWatch Container Insights for the EKS cluster
        // This is done by deploying the CloudWatch agent as a DaemonSet
        
        // Create service account for CloudWatch agent
        const cloudWatchAgentServiceAccount = this.cluster.addServiceAccount('CloudWatchAgentServiceAccount', {
            name: 'cloudwatch-agent',
            namespace: 'amazon-cloudwatch',
        });

        // Add IAM permissions for CloudWatch agent
        cloudWatchAgentServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'cloudwatch:PutMetricData',
                'ec2:DescribeVolumes',
                'ec2:DescribeTags',
                'logs:PutLogEvents',
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:DescribeLogStreams',
                'logs:DescribeLogGroups',
                'logs:PutRetentionPolicy',
            ],
            resources: ['*'],
        }));

        // Create namespace for CloudWatch
        this.cluster.addManifest('CloudWatchNamespace', {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: 'amazon-cloudwatch',
                labels: {
                    name: 'amazon-cloudwatch',
                },
            },
        });

        // Deploy CloudWatch agent configuration
        this.cluster.addManifest('CloudWatchAgentConfig', {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: 'cwagentconfig',
                namespace: 'amazon-cloudwatch',
            },
            data: {
                'cwagentconfig.json': JSON.stringify({
                    agent: {
                        region: region || 'ap-east-2',
                    },
                    logs: {
                        metrics_collected: {
                            kubernetes: {
                                cluster_name: this.cluster.clusterName,
                                metrics_collection_interval: 60,
                            },
                        },
                        force_flush_interval: 5,
                    },
                    metrics: {
                        namespace: 'ContainerInsights',
                        metrics_collected: {
                            cpu: {
                                measurement: ['cpu_usage_idle', 'cpu_usage_iowait', 'cpu_usage_user', 'cpu_usage_system'],
                                metrics_collection_interval: 60,
                            },
                            disk: {
                                measurement: ['used_percent'],
                                metrics_collection_interval: 60,
                                resources: ['*'],
                            },
                            diskio: {
                                measurement: ['io_time', 'read_bytes', 'write_bytes', 'reads', 'writes'],
                                metrics_collection_interval: 60,
                                resources: ['*'],
                            },
                            mem: {
                                measurement: ['mem_used_percent'],
                                metrics_collection_interval: 60,
                            },
                            netstat: {
                                measurement: ['tcp_established', 'tcp_time_wait'],
                                metrics_collection_interval: 60,
                            },
                        },
                    },
                }),
            },
        });

        // Deploy CloudWatch agent DaemonSet
        this.cluster.addManifest('CloudWatchAgentDaemonSet', {
            apiVersion: 'apps/v1',
            kind: 'DaemonSet',
            metadata: {
                name: 'cloudwatch-agent',
                namespace: 'amazon-cloudwatch',
            },
            spec: {
                selector: {
                    matchLabels: {
                        name: 'cloudwatch-agent',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            name: 'cloudwatch-agent',
                        },
                    },
                    spec: {
                        containers: [
                            {
                                name: 'cloudwatch-agent',
                                image: 'amazon/cloudwatch-agent:1.300032.2b361',
                                ports: [
                                    {
                                        containerPort: 8125,
                                        hostPort: 8125,
                                        protocol: 'UDP',
                                    },
                                ],
                                resources: {
                                    limits: {
                                        cpu: '200m',
                                        memory: '200Mi',
                                    },
                                    requests: {
                                        cpu: '200m',
                                        memory: '200Mi',
                                    },
                                },
                                env: [
                                    {
                                        name: 'AWS_REGION',
                                        value: region || 'ap-east-2',
                                    },
                                    {
                                        name: 'K8S_NAMESPACE',
                                        valueFrom: {
                                            fieldRef: {
                                                fieldPath: 'metadata.namespace',
                                            },
                                        },
                                    },
                                    {
                                        name: 'HOST_IP',
                                        valueFrom: {
                                            fieldRef: {
                                                fieldPath: 'status.hostIP',
                                            },
                                        },
                                    },
                                    {
                                        name: 'HOST_NAME',
                                        valueFrom: {
                                            fieldRef: {
                                                fieldPath: 'spec.nodeName',
                                            },
                                        },
                                    },
                                    {
                                        name: 'K8S_POD_NAME',
                                        valueFrom: {
                                            fieldRef: {
                                                fieldPath: 'metadata.name',
                                            },
                                        },
                                    },
                                ],
                                volumeMounts: [
                                    {
                                        name: 'cwagentconfig',
                                        mountPath: '/etc/cwagentconfig',
                                    },
                                    {
                                        name: 'rootfs',
                                        mountPath: '/rootfs',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'dockersock',
                                        mountPath: '/var/run/docker.sock',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'varlibdocker',
                                        mountPath: '/var/lib/docker',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'sys',
                                        mountPath: '/sys',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'devdisk',
                                        mountPath: '/dev/disk',
                                        readOnly: true,
                                    },
                                ],
                            },
                        ],
                        volumes: [
                            {
                                name: 'cwagentconfig',
                                configMap: {
                                    name: 'cwagentconfig',
                                },
                            },
                            {
                                name: 'rootfs',
                                hostPath: {
                                    path: '/',
                                },
                            },
                            {
                                name: 'dockersock',
                                hostPath: {
                                    path: '/var/run/docker.sock',
                                },
                            },
                            {
                                name: 'varlibdocker',
                                hostPath: {
                                    path: '/var/lib/docker',
                                },
                            },
                            {
                                name: 'sys',
                                hostPath: {
                                    path: '/sys',
                                },
                            },
                            {
                                name: 'devdisk',
                                hostPath: {
                                    path: '/dev/disk/',
                                },
                            },
                        ],
                        terminationGracePeriodSeconds: 60,
                        serviceAccountName: 'cloudwatch-agent',
                        hostNetwork: true,
                    },
                },
            },
        });

        // Deploy Fluent Bit for log collection
        this.cluster.addManifest('FluentBitServiceAccount', {
            apiVersion: 'v1',
            kind: 'ServiceAccount',
            metadata: {
                name: 'fluent-bit',
                namespace: 'amazon-cloudwatch',
            },
        });

        this.cluster.addManifest('FluentBitClusterRole', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'ClusterRole',
            metadata: {
                name: 'fluent-bit-role',
            },
            rules: [
                {
                    nonResourceURLs: ['/metrics'],
                    verbs: ['get'],
                },
                {
                    apiGroups: [''],
                    resources: ['namespaces', 'pods', 'pods/logs', 'nodes', 'nodes/proxy'],
                    verbs: ['get', 'list', 'watch'],
                },
            ],
        });

        this.cluster.addManifest('FluentBitClusterRoleBinding', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'ClusterRoleBinding',
            metadata: {
                name: 'fluent-bit-role-binding',
            },
            roleRef: {
                apiGroup: 'rbac.authorization.k8s.io',
                kind: 'ClusterRole',
                name: 'fluent-bit-role',
            },
            subjects: [
                {
                    kind: 'ServiceAccount',
                    name: 'fluent-bit',
                    namespace: 'amazon-cloudwatch',
                },
            ],
        });

        this.cluster.addManifest('FluentBitConfigMap', {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: 'fluent-bit-config',
                namespace: 'amazon-cloudwatch',
                labels: {
                    'k8s-app': 'fluent-bit',
                },
            },
            data: {
                'fluent-bit.conf': `
[SERVICE]
    Flush                     5
    Grace                     30
    Log_Level                 info
    Daemon                    off
    Parsers_File              parsers.conf
    HTTP_Server               On
    HTTP_Listen               0.0.0.0
    HTTP_Port                 2020
    storage.path              /var/fluent-bit/state/flb-storage/
    storage.sync              normal
    storage.checksum          off
    storage.backlog.mem_limit 5M

@INCLUDE application-log.conf
@INCLUDE dataplane-log.conf
@INCLUDE host-log.conf
`,
                'application-log.conf': `
[INPUT]
    Name                tail
    Tag                 application.*
    Exclude_Path        /var/log/containers/cloudwatch-agent*, /var/log/containers/fluent-bit*
    Path                /var/log/containers/*.log
    multiline.parser    docker, cri
    DB                  /var/fluent-bit/state/flb_container.db
    Mem_Buf_Limit       50MB
    Skip_Long_Lines     On
    Refresh_Interval    10
    Rotate_Wait         30
    storage.type        filesystem
    Read_from_Head      Off

[FILTER]
    Name                kubernetes
    Match               application.*
    Kube_URL            https://kubernetes.default.svc:443
    Kube_CA_File        /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
    Kube_Token_File     /var/run/secrets/kubernetes.io/serviceaccount/token
    Kube_Tag_Prefix     application.var.log.containers.
    Merge_Log           On
    Merge_Log_Key       log_processed
    K8S-Logging.Parser  On
    K8S-Logging.Exclude Off
    Labels              Off
    Annotations         Off
    Use_Kubelet         On
    Kubelet_Port        10250
    Buffer_Size         0

[OUTPUT]
    Name                cloudwatch_logs
    Match               application.*
    region              ${region || 'ap-east-2'}
    log_group_name      /aws/containerinsights/${this.cluster.clusterName}/application
    log_stream_prefix   ${environment}-
    auto_create_group   true
    extra_user_agent    container-insights
`,
                'dataplane-log.conf': `
[INPUT]
    Name                systemd
    Tag                 dataplane.systemd.*
    Systemd_Filter      _SYSTEMD_UNIT=docker.service
    Systemd_Filter      _SYSTEMD_UNIT=kubelet.service
    DB                  /var/fluent-bit/state/systemd.db
    Path                /var/log/journal
    Read_From_Tail      On

[FILTER]
    Name                modify
    Match               dataplane.systemd.*
    Rename              _HOSTNAME                   hostname
    Rename              _SYSTEMD_UNIT               systemd_unit
    Rename              MESSAGE                     message
    Remove_regex        ^((?!hostname|systemd_unit|message).)*$

[OUTPUT]
    Name                cloudwatch_logs
    Match               dataplane.systemd.*
    region              ${region || 'ap-east-2'}
    log_group_name      /aws/containerinsights/${this.cluster.clusterName}/dataplane
    log_stream_prefix   ${environment}-
    auto_create_group   true
    extra_user_agent    container-insights
`,
                'host-log.conf': `
[INPUT]
    Name                tail
    Tag                 host.dmesg
    Path                /var/log/dmesg
    Key                 message
    DB                  /var/fluent-bit/state/flb_dmesg.db
    Mem_Buf_Limit       5MB
    Skip_Long_Lines     On
    Refresh_Interval    10

[INPUT]
    Name                tail
    Tag                 host.messages
    Path                /var/log/messages
    Parser              syslog
    DB                  /var/fluent-bit/state/flb_messages.db
    Mem_Buf_Limit       5MB
    Skip_Long_Lines     On
    Refresh_Interval    10

[INPUT]
    Name                tail
    Tag                 host.secure
    Path                /var/log/secure
    Parser              syslog
    DB                  /var/fluent-bit/state/flb_secure.db
    Mem_Buf_Limit       5MB
    Skip_Long_Lines     On
    Refresh_Interval    10

[OUTPUT]
    Name                cloudwatch_logs
    Match               host.*
    region              ${region || 'ap-east-2'}
    log_group_name      /aws/containerinsights/${this.cluster.clusterName}/host
    log_stream_prefix   ${environment}-
    auto_create_group   true
    extra_user_agent    container-insights
`,
                'parsers.conf': `
[PARSER]
    Name                syslog
    Format              regex
    Regex               ^(?<time>[^ ]* {1,2}[^ ]* [^ ]*) (?<host>[^ ]*) (?<ident>[a-zA-Z0-9_\/\.\-]*)(?:\[(?<pid>[0-9]+)\])?(?:[^\:]*\:)? *(?<message>.*)$
    Time_Key            time
    Time_Format         %b %d %H:%M:%S

[PARSER]
    Name                container_firstline
    Format              regex
    Regex               (?<log>(?<="log":")\S(?!\.).*?)(?<!\\)".*(?<stream>(?<="stream":").*?)".*(?<time>\d{4}-\d{1,2}-\d{1,2}T\d{2}:\d{2}:\d{2}\.\w*).*(?=})
    Time_Key            time
    Time_Format         %Y-%m-%dT%H:%M:%S.%LZ

[PARSER]
    Name                cwagent_firstline
    Format              regex
    Regex               (?<log>(?<="log":")\d{4}[\/-]\d{1,2}[\/-]\d{1,2}[ T]\d{2}:\d{2}:\d{2}(?!\.).*?)(?<!\\)".*(?<stream>(?<="stream":").*?)".*(?<time>\d{4}-\d{1,2}-\d{1,2}T\d{2}:\d{2}:\d{2}\.\w*).*(?=})
    Time_Key            time
    Time_Format         %Y-%m-%dT%H:%M:%S.%LZ
`,
            },
        });

        this.cluster.addManifest('FluentBitDaemonSet', {
            apiVersion: 'apps/v1',
            kind: 'DaemonSet',
            metadata: {
                name: 'fluent-bit',
                namespace: 'amazon-cloudwatch',
                labels: {
                    'k8s-app': 'fluent-bit',
                    version: 'v1',
                    'kubernetes.io/cluster-service': 'true',
                },
            },
            spec: {
                selector: {
                    matchLabels: {
                        'k8s-app': 'fluent-bit',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            'k8s-app': 'fluent-bit',
                            version: 'v1',
                            'kubernetes.io/cluster-service': 'true',
                        },
                    },
                    spec: {
                        containers: [
                            {
                                name: 'fluent-bit',
                                image: 'public.ecr.aws/aws-observability/aws-for-fluent-bit:stable',
                                imagePullPolicy: 'Always',
                                env: [
                                    {
                                        name: 'AWS_REGION',
                                        value: region || 'ap-east-2',
                                    },
                                    {
                                        name: 'CLUSTER_NAME',
                                        value: this.cluster.clusterName,
                                    },
                                    {
                                        name: 'HTTP_SERVER',
                                        value: 'On',
                                    },
                                    {
                                        name: 'HTTP_PORT',
                                        value: '2020',
                                    },
                                    {
                                        name: 'READ_FROM_HEAD',
                                        value: 'Off',
                                    },
                                    {
                                        name: 'READ_FROM_TAIL',
                                        value: 'On',
                                    },
                                    {
                                        name: 'HOST_NAME',
                                        valueFrom: {
                                            fieldRef: {
                                                fieldPath: 'spec.nodeName',
                                            },
                                        },
                                    },
                                    {
                                        name: 'CI_VERSION',
                                        value: 'k8s/1.3.26',
                                    },
                                ],
                                resources: {
                                    limits: {
                                        memory: '200Mi',
                                    },
                                    requests: {
                                        cpu: '500m',
                                        memory: '100Mi',
                                    },
                                },
                                volumeMounts: [
                                    {
                                        name: 'fluentbitstate',
                                        mountPath: '/var/fluent-bit/state',
                                    },
                                    {
                                        name: 'varlog',
                                        mountPath: '/var/log',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'varlibdockercontainers',
                                        mountPath: '/var/lib/docker/containers',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'fluent-bit-config',
                                        mountPath: '/fluent-bit/etc/',
                                    },
                                    {
                                        name: 'runlogjournal',
                                        mountPath: '/run/log/journal',
                                        readOnly: true,
                                    },
                                    {
                                        name: 'dmesg',
                                        mountPath: '/var/log/dmesg',
                                        readOnly: true,
                                    },
                                ],
                            },
                        ],
                        terminationGracePeriodSeconds: 10,
                        hostNetwork: true,
                        dnsPolicy: 'ClusterFirstWithHostNet',
                        volumes: [
                            {
                                name: 'fluentbitstate',
                                hostPath: {
                                    path: '/var/fluent-bit/state',
                                },
                            },
                            {
                                name: 'varlog',
                                hostPath: {
                                    path: '/var/log',
                                },
                            },
                            {
                                name: 'varlibdockercontainers',
                                hostPath: {
                                    path: '/var/lib/docker/containers',
                                },
                            },
                            {
                                name: 'fluent-bit-config',
                                configMap: {
                                    name: 'fluent-bit-config',
                                },
                            },
                            {
                                name: 'runlogjournal',
                                hostPath: {
                                    path: '/run/log/journal',
                                },
                            },
                            {
                                name: 'dmesg',
                                hostPath: {
                                    path: '/var/log/dmesg',
                                },
                            },
                        ],
                        serviceAccountName: 'fluent-bit',
                        tolerations: [
                            {
                                key: 'node-role.kubernetes.io/master',
                                operator: 'Exists',
                                effect: 'NoSchedule',
                            },
                            {
                                operator: 'Exists',
                                effect: 'NoExecute',
                            },
                            {
                                operator: 'Exists',
                                effect: 'NoSchedule',
                            },
                        ],
                    },
                },
            },
        });
    }

    private configureXRayTracing(projectName: string, environment: string, region?: string): void {
        // Create service account for X-Ray daemon
        const xrayServiceAccount = this.cluster.addServiceAccount('XRayDaemonServiceAccount', {
            name: 'xray-daemon',
            namespace: 'default',
        });

        // Add IAM permissions for X-Ray daemon
        xrayServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'xray:PutTraceSegments',
                'xray:PutTelemetryRecords',
                'xray:GetSamplingRules',
                'xray:GetSamplingTargets',
                'xray:GetSamplingStatisticSummaries',
            ],
            resources: ['*'],
        }));

        // Deploy X-Ray daemon as DaemonSet
        this.cluster.addManifest('XRayDaemonDaemonSet', {
            apiVersion: 'apps/v1',
            kind: 'DaemonSet',
            metadata: {
                name: 'xray-daemon',
                namespace: 'default',
                labels: {
                    app: 'xray-daemon',
                },
            },
            spec: {
                selector: {
                    matchLabels: {
                        app: 'xray-daemon',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'xray-daemon',
                        },
                    },
                    spec: {
                        serviceAccountName: 'xray-daemon',
                        hostNetwork: true,
                        containers: [
                            {
                                name: 'xray-daemon',
                                image: 'amazon/aws-xray-daemon:3.3.7',
                                command: ['/usr/bin/xray', '-o', '-b', '0.0.0.0:2000'],
                                resources: {
                                    limits: {
                                        cpu: '256m',
                                        memory: '32Mi',
                                    },
                                    requests: {
                                        cpu: '256m',
                                        memory: '32Mi',
                                    },
                                },
                                ports: [
                                    {
                                        name: 'xray-ingest',
                                        containerPort: 2000,
                                        hostPort: 2000,
                                        protocol: 'UDP',
                                    },
                                ],
                                env: [
                                    {
                                        name: 'AWS_REGION',
                                        value: region || 'ap-east-2',
                                    },
                                ],
                            },
                        ],
                    },
                },
            },
        });

        // Create X-Ray service for service discovery
        this.cluster.addManifest('XRayService', {
            apiVersion: 'v1',
            kind: 'Service',
            metadata: {
                name: 'xray-service',
                namespace: 'default',
                labels: {
                    app: 'xray-daemon',
                },
            },
            spec: {
                selector: {
                    app: 'xray-daemon',
                },
                clusterIP: 'None',
                ports: [
                    {
                        name: 'xray-ingest',
                        port: 2000,
                        protocol: 'UDP',
                    },
                ],
            },
        });

        // Create X-Ray sampling rule ConfigMap
        this.cluster.addManifest('XRaySamplingRules', {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: 'xray-sampling-rules',
                namespace: 'default',
            },
            data: {
                'sampling-rules.json': JSON.stringify({
                    version: 2,
                    default: {
                        fixed_target: 1,
                        rate: 0.1,
                    },
                    rules: [
                        {
                            description: 'GenAI Demo Application',
                            service_name: 'genai-demo',
                            http_method: '*',
                            url_path: '*',
                            fixed_target: 2,
                            rate: 0.05,
                        },
                        {
                            description: 'Health Check Endpoints',
                            service_name: '*',
                            http_method: 'GET',
                            url_path: '/actuator/health*',
                            fixed_target: 0,
                            rate: 0.0,
                        },
                        {
                            description: 'Metrics Endpoints',
                            service_name: '*',
                            http_method: 'GET',
                            url_path: '/actuator/metrics*',
                            fixed_target: 0,
                            rate: 0.0,
                        },
                    ],
                }),
            },
        });
    }

    private createOutputs(projectName: string, environment: string, region?: string): void {
        const regionSuffix = region || 'ap-east-2';

        new cdk.CfnOutput(this, 'EKSClusterName', {
            value: this.cluster.clusterName,
            exportName: `${environment}-eks-cluster-name-${regionSuffix}`,
            description: 'EKS Cluster Name',
        });

        new cdk.CfnOutput(this, 'EKSClusterEndpoint', {
            value: this.cluster.clusterEndpoint,
            exportName: `${environment}-eks-cluster-endpoint-${regionSuffix}`,
            description: 'EKS Cluster Endpoint',
        });

        new cdk.CfnOutput(this, 'EKSClusterArn', {
            value: this.cluster.clusterArn,
            exportName: `${environment}-eks-cluster-arn-${regionSuffix}`,
            description: 'EKS Cluster ARN',
        });

        new cdk.CfnOutput(this, 'EKSClusterSecurityGroupId', {
            value: this.cluster.clusterSecurityGroupId,
            exportName: `${environment}-eks-cluster-sg-${regionSuffix}`,
            description: 'EKS Cluster Security Group ID',
        });

        new cdk.CfnOutput(this, 'ContainerInsightsLogGroup', {
            value: `/aws/containerinsights/${this.cluster.clusterName}/performance`,
            exportName: `${environment}-container-insights-log-group-${regionSuffix}`,
            description: 'CloudWatch Container Insights Log Group',
        });

        new cdk.CfnOutput(this, 'XRayServiceMapURL', {
            value: `https://console.aws.amazon.com/xray/home?region=${regionSuffix}#/service-map`,
            exportName: `${environment}-xray-service-map-url-${regionSuffix}`,
            description: 'X-Ray Service Map Console URL',
        });
    }

    /**
     * Configure cross-region networking and service mesh integration
     */
    private configureCrossRegionNetworking(projectName: string, environment: string): void {
        // Create cross-region networking namespace
        this.cluster.addManifest('CrossRegionNetworkingNamespace', {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: 'cross-region-networking',
                labels: {
                    'istio-injection': 'enabled',
                    'cross-region': 'true',
                    'project': projectName,
                    'environment': environment,
                },
            },
        });

        // Create cross-region service mesh configuration
        this.cluster.addManifest('CrossRegionMeshConfig', {
            apiVersion: 'v1',
            kind: 'ConfigMap',
            metadata: {
                name: 'cross-region-mesh-config',
                namespace: 'istio-system',
            },
            data: {
                'mesh': `
defaultConfig:
  discoveryRefreshDelay: 10s
  proxyStatsMatcher:
    inclusionRegexps:
    - ".*circuit_breakers.*"
    - ".*upstream_rq_retry.*"
    - ".*upstream_rq_pending.*"
    - ".*_cx_.*"
  concurrency: 2
  configPath: "/etc/istio/proxy"
  binaryPath: "/usr/local/bin/envoy"
  serviceCluster: ${this.cluster.clusterName}
  drainDuration: 45s
  parentShutdownDuration: 1m0s
  interceptionMode: REDIRECT
  proxyAdminPort: 15000
  controlPlaneAuthPolicy: MUTUAL_TLS
  discoveryAddress: istiod.istio-system.svc:15010
trustDomain: cluster.local
defaultProviders:
  metrics:
  - prometheus
  tracing:
  - jaeger
  accessLogging:
  - envoy
extensionProviders:
- name: prometheus
  prometheus: {}
- name: jaeger
  envoyOtelAls:
    service: jaeger.istio-system.svc.cluster.local
    port: 14250
- name: envoy
  envoyFileAccessLog:
    path: /dev/stdout
meshNetworks:
  network-${this.region}:
    endpoints:
    - fromRegistry: ${this.cluster.clusterName}
    gateways:
    - address: 0.0.0.0
      port: 15443
`,
            },
        });

        // Create cross-region endpoint slice controller
        this.cluster.addManifest('CrossRegionEndpointSliceController', {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            metadata: {
                name: 'cross-region-endpoint-controller',
                namespace: 'cross-region-networking',
                labels: {
                    app: 'cross-region-endpoint-controller',
                    version: 'v1',
                },
            },
            spec: {
                replicas: 1,
                selector: {
                    matchLabels: {
                        app: 'cross-region-endpoint-controller',
                    },
                },
                template: {
                    metadata: {
                        labels: {
                            app: 'cross-region-endpoint-controller',
                            version: 'v1',
                        },
                        annotations: {
                            'sidecar.istio.io/inject': 'true',
                        },
                    },
                    spec: {
                        serviceAccountName: 'cross-region-lb-sa',
                        containers: [
                            {
                                name: 'endpoint-controller',
                                image: 'k8s.gcr.io/sig-network/kube-proxy:v1.28.0',
                                command: [
                                    '/bin/sh',
                                    '-c',
                                    'while true; do echo "Cross-region endpoint controller running"; sleep 30; done',
                                ],
                                env: [
                                    {
                                        name: 'CLUSTER_NAME',
                                        value: this.cluster.clusterName,
                                    },
                                    {
                                        name: 'REGION',
                                        value: this.region,
                                    },
                                    {
                                        name: 'IS_PRIMARY_REGION',
                                        value: this.isPrimaryRegion.toString(),
                                    },
                                ],
                                resources: {
                                    limits: {
                                        cpu: '100m',
                                        memory: '128Mi',
                                    },
                                    requests: {
                                        cpu: '50m',
                                        memory: '64Mi',
                                    },
                                },
                            },
                        ],
                    },
                },
            },
        });

        // Create cross-region service monitor for Prometheus
        this.cluster.addManifest('CrossRegionServiceMonitor', {
            apiVersion: 'monitoring.coreos.com/v1',
            kind: 'ServiceMonitor',
            metadata: {
                name: 'cross-region-metrics',
                namespace: 'monitoring',
                labels: {
                    app: 'cross-region-monitoring',
                    release: 'prometheus',
                },
            },
            spec: {
                selector: {
                    matchLabels: {
                        'cross-region': 'true',
                    },
                },
                endpoints: [
                    {
                        port: 'metrics',
                        interval: '30s',
                        path: '/stats/prometheus',
                    },
                ],
                namespaceSelector: {
                    matchNames: [
                        'default',
                        'istio-system',
                        'istio-gateway',
                        'cross-region-networking',
                    ],
                },
            },
        });

        // Create cross-region traffic policy
        this.cluster.addManifest('CrossRegionTrafficPolicy', {
            apiVersion: 'networking.istio.io/v1beta1',
            kind: 'Sidecar',
            metadata: {
                name: 'cross-region-sidecar',
                namespace: 'default',
            },
            spec: {
                workloadSelector: {
                    labels: {
                        app: 'genai-demo',
                    },
                },
                egress: [
                    {
                        hosts: [
                            './*',
                            'istio-system/*',
                            'cross-region-networking/*',
                            'genai-demo.default.global',
                        ],
                    },
                ],
                ingress: [
                    {
                        port: {
                            number: 8080,
                            name: 'http',
                            protocol: 'HTTP',
                        },
                        defaultEndpoint: '127.0.0.1:8080',
                    },
                ],
            },
        });

        // Add stack outputs
        new CfnOutput(this, 'EKSClusterName', {
            value: this.cluster.clusterName,
            description: 'EKS Cluster Name',
            exportName: `${projectName}-${environment}-eks-cluster-name-${this.region}`,
        });

        new CfnOutput(this, 'EKSClusterArn', {
            value: this.cluster.clusterArn,
            description: 'EKS Cluster ARN',
            exportName: `${projectName}-${environment}-eks-cluster-arn-${this.region}`,
        });

        new CfnOutput(this, 'EKSClusterEndpoint', {
            value: this.cluster.clusterEndpoint,
            description: 'EKS Cluster Endpoint',
            exportName: `${projectName}-${environment}-eks-cluster-endpoint-${this.region}`,
        });

        new CfnOutput(this, 'EKSClusterSecurityGroupId', {
            value: this.cluster.clusterSecurityGroupId,
            description: 'EKS Cluster Security Group ID',
            exportName: `${projectName}-${environment}-eks-cluster-sg-${this.region}`,
        });
    }

}
