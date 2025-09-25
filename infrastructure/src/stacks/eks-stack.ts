import * as cdk from 'aws-cdk-lib';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
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

    constructor(scope: Construct, id: string, props: EKSStackProps) {
        super(scope, id, props);

        const { environment, projectName, vpc, region, isPrimaryRegion = true } = props;

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

        // Create application service account with AWS permissions
        this.createApplicationServiceAccount(projectName, environment, region);

        // Install KEDA for event-driven autoscaling
        this.installKEDA();

        // Configure Cluster Autoscaler
        this.configureClusterAutoscaler(projectName, environment);

        // Enable CloudWatch Container Insights
        this.enableContainerInsights(projectName, environment, region);

        // Configure X-Ray distributed tracing
        this.configureXRayTracing(projectName, environment, region);

        // Create outputs
        this.createOutputs(projectName, environment, region);
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
        // Create node group role
        const nodeGroupRole = new iam.Role(this, 'EKSNodeGroupRole', {
            assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
            managedPolicies: [
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEKSWorkerNodePolicy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEKS_CNI_Policy'),
                iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonEC2ContainerRegistryReadOnly'),
            ],
        });

        const nodeGroup = this.cluster.addNodegroupCapacity('ManagedNodeGroup', {
            nodegroupName: `${projectName}-${environment}-nodes`,
            instanceTypes: [
                new ec2.InstanceType('t3.medium'),
                new ec2.InstanceType('t3.large'),
            ],
            minSize: 2,
            maxSize: 10,
            desiredSize: 2,
            nodeRole: nodeGroupRole,
            subnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
            amiType: eks.NodegroupAmiType.AL2_X86_64,
            capacityType: eks.CapacityType.ON_DEMAND,
            diskSize: 20,
            tags: {
                'Environment': environment,
                'Project': projectName,
                'NodeGroup': 'ManagedNodeGroup',
            },
        });

        return nodeGroup;
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

        // Create HPA configuration
        this.cluster.addManifest('HPA', {
            apiVersion: 'autoscaling/v2',
            kind: 'HorizontalPodAutoscaler',
            metadata: {
                name: 'genai-demo-hpa',
                namespace: 'default',
            },
            spec: {
                scaleTargetRef: {
                    apiVersion: 'apps/v1',
                    kind: 'Deployment',
                    name: 'genai-demo-app',
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
                                averageUtilization: 70,
                            },
                        },
                    },
                    {
                        type: 'Resource',
                        resource: {
                            name: 'memory',
                            target: {
                                type: 'Utilization',
                                averageUtilization: 80,
                            },
                        },
                    },
                ],
            },
        });

        // Create KEDA ScaledObject for thread pool monitoring
        this.cluster.addManifest('KEDAScaledObject', {
            apiVersion: 'keda.sh/v1alpha1',
            kind: 'ScaledObject',
            metadata: {
                name: 'genai-demo-thread-pool-scaler',
                namespace: 'default',
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
                ],
            },
        });
    }

    private configureClusterAutoscaler(projectName: string, environment: string): void {
        // Create service account for cluster autoscaler
        const clusterAutoscalerServiceAccount = this.cluster.addServiceAccount('ClusterAutoscalerServiceAccount', {
            name: 'cluster-autoscaler',
            namespace: 'kube-system',
        });

        // Add IAM permissions for cluster autoscaler
        clusterAutoscalerServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'autoscaling:DescribeAutoScalingGroups',
                'autoscaling:DescribeAutoScalingInstances',
                'autoscaling:DescribeLaunchConfigurations',
                'autoscaling:DescribeTags',
                'autoscaling:SetDesiredCapacity',
                'autoscaling:TerminateInstanceInAutoScalingGroup',
                'ec2:DescribeLaunchTemplateVersions',
                'ec2:DescribeInstanceTypes',
            ],
            resources: ['*'],
        }));

        // Deploy cluster autoscaler
        this.cluster.addManifest('ClusterAutoscaler', {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            metadata: {
                name: 'cluster-autoscaler',
                namespace: 'kube-system',
                labels: {
                    app: 'cluster-autoscaler',
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
                        },
                        annotations: {
                            'prometheus.io/scrape': 'true',
                            'prometheus.io/port': '8085',
                        },
                    },
                    spec: {
                        serviceAccountName: 'cluster-autoscaler',
                        containers: [
                            {
                                image: 'registry.k8s.io/autoscaling/cluster-autoscaler:v1.28.2',
                                name: 'cluster-autoscaler',
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
                                command: [
                                    './cluster-autoscaler',
                                    '--v=4',
                                    '--stderrthreshold=info',
                                    '--cloud-provider=aws',
                                    '--skip-nodes-with-local-storage=false',
                                    '--expander=least-waste',
                                    `--node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/${this.cluster.clusterName}`,
                                    '--balance-similar-node-groups',
                                    '--skip-nodes-with-system-pods=false',
                                ],
                                volumeMounts: [
                                    {
                                        name: 'ssl-certs',
                                        mountPath: '/etc/ssl/certs/ca-certificates.crt',
                                        readOnly: true,
                                    },
                                ],
                                imagePullPolicy: 'Always',
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
                    },
                },
            },
        });
    }

    private createApplicationServiceAccount(projectName: string, environment: string, region?: string): eks.ServiceAccount {
        // Create application service account with comprehensive AWS permissions
        const appServiceAccount = this.cluster.addServiceAccount('ApplicationServiceAccount', {
            name: 'genai-demo-app',
            namespace: 'default',
        });

        // CloudWatch Metrics and Logs permissions
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                // CloudWatch Metrics
                'cloudwatch:PutMetricData',
                'cloudwatch:GetMetricStatistics',
                'cloudwatch:ListMetrics',
                // CloudWatch Logs
                'logs:CreateLogGroup',
                'logs:CreateLogStream',
                'logs:PutLogEvents',
                'logs:DescribeLogStreams',
                'logs:DescribeLogGroups',
                'logs:PutRetentionPolicy',
            ],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'aws:RequestedRegion': region || 'ap-east-2'
                }
            }
        }));

        // X-Ray Distributed Tracing permissions
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'xray:PutTraceSegments',
                'xray:PutTelemetryRecords',
                'xray:GetSamplingRules',
                'xray:GetSamplingTargets',
                'xray:GetSamplingStatisticSummaries',
            ],
            resources: ['*'],
            conditions: {
                StringEquals: {
                    'aws:RequestedRegion': region || 'ap-east-2'
                }
            }
        }));

        // Parameter Store permissions for configuration
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'ssm:GetParameter',
                'ssm:GetParameters',
                'ssm:GetParametersByPath',
                'ssm:DescribeParameters',
            ],
            resources: [
                `arn:aws:ssm:${region || 'ap-east-2'}:${this.account}:parameter/genai-demo/${environment}/*`,
                `arn:aws:ssm:${region || 'ap-east-2'}:${this.account}:parameter/genai-demo/common/*`,
            ],
        }));

        // Secrets Manager permissions for sensitive data
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'secretsmanager:GetSecretValue',
                'secretsmanager:DescribeSecret',
            ],
            resources: [
                `arn:aws:secretsmanager:${region || 'ap-east-2'}:${this.account}:secret:genai-demo/${environment}/*`,
                `arn:aws:secretsmanager:${region || 'ap-east-2'}:${this.account}:secret:${projectName}/${environment}/*`,
            ],
        }));

        // KMS permissions for decryption
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'kms:Decrypt',
                'kms:GenerateDataKey',
                'kms:DescribeKey',
            ],
            resources: [
                `arn:aws:kms:${region || 'ap-east-2'}:${this.account}:key/*`,
            ],
            conditions: {
                StringEquals: {
                    'kms:ViaService': [
                        `secretsmanager.${region || 'ap-east-2'}.amazonaws.com`,
                        `ssm.${region || 'ap-east-2'}.amazonaws.com`,
                        `logs.${region || 'ap-east-2'}.amazonaws.com`,
                    ]
                }
            }
        }));

        // S3 permissions for file storage (if needed)
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                's3:GetObject',
                's3:PutObject',
                's3:DeleteObject',
                's3:ListBucket',
                's3:GetBucketLocation',
            ],
            resources: [
                `arn:aws:s3:::${projectName}-${environment}-*`,
                `arn:aws:s3:::${projectName}-${environment}-*/*`,
            ],
        }));

        // SQS permissions for message queues (if needed)
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'sqs:SendMessage',
                'sqs:ReceiveMessage',
                'sqs:DeleteMessage',
                'sqs:GetQueueAttributes',
                'sqs:GetQueueUrl',
            ],
            resources: [
                `arn:aws:sqs:${region || 'ap-east-2'}:${this.account}:${projectName}-${environment}-*`,
            ],
        }));

        // SNS permissions for notifications (if needed)
        appServiceAccount.addToPrincipalPolicy(new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            actions: [
                'sns:Publish',
                'sns:GetTopicAttributes',
            ],
            resources: [
                `arn:aws:sns:${region || 'ap-east-2'}:${this.account}:${projectName}-${environment}-*`,
            ],
        }));

        // Add tags to the service account
        cdk.Tags.of(appServiceAccount).add('Application', projectName);
        cdk.Tags.of(appServiceAccount).add('Environment', environment);
        cdk.Tags.of(appServiceAccount).add('Component', 'Application');
        cdk.Tags.of(appServiceAccount).add('ServiceAccount', 'genai-demo-app');

        return appServiceAccount;
    }

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
}
