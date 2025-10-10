import * as cdk from 'aws-cdk-lib';
import * as eks from 'aws-cdk-lib/aws-eks';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export interface EKSIRSAStackProps extends cdk.StackProps {
    readonly environment: string;
    readonly projectName: string;
    readonly region?: string;
    readonly cluster: eks.ICluster;
    readonly applicationRole: iam.IRole;
    readonly monitoringRole: iam.IRole;
    readonly dataAccessRole: iam.IRole;
    readonly adminRole: iam.IRole;
}

/**
 * EKS IRSA (IAM Roles for Service Accounts) Configuration Stack
 * 
 * This stack configures IRSA for EKS by:
 * 1. Creating service accounts with proper annotations
 * 2. Binding IAM roles to service accounts
 * 3. Setting up namespace-based access control
 * 4. Configuring pod security policies
 * 
 * Requirements: 3.1 - Security Architecture Enhancement
 */
export class EKSIRSAStack extends cdk.Stack {
    public readonly applicationServiceAccount: eks.ServiceAccount;
    public readonly monitoringServiceAccount: eks.ServiceAccount;
    public readonly dataServiceAccount: eks.ServiceAccount;
    public readonly adminServiceAccount: eks.ServiceAccount;

    constructor(scope: Construct, id: string, props: EKSIRSAStackProps) {
        super(scope, id, props);

        const { environment, projectName, region, cluster, applicationRole, monitoringRole, dataAccessRole, adminRole } = props;

        // Apply common tags
        cdk.Tags.of(this).add('Environment', environment);
        cdk.Tags.of(this).add('Project', projectName);
        cdk.Tags.of(this).add('Stack', 'EKS-IRSA');
        cdk.Tags.of(this).add('Component', 'Security');

        // Create namespaces for different service types
        this.createNamespaces(cluster, projectName, environment);

        // Create service accounts with IRSA configuration
        this.applicationServiceAccount = this.createApplicationServiceAccount(cluster, applicationRole, projectName, environment);
        this.monitoringServiceAccount = this.createMonitoringServiceAccount(cluster, monitoringRole, projectName, environment);
        this.dataServiceAccount = this.createDataServiceAccount(cluster, dataAccessRole, projectName, environment);
        this.adminServiceAccount = this.createAdminServiceAccount(cluster, adminRole, projectName, environment);

        // Configure RBAC (Role-Based Access Control)
        this.configureRBAC(cluster, projectName, environment);

        // Configure Network Policies for additional security
        this.configureNetworkPolicies(cluster, projectName, environment);

        // Configure Pod Security Standards
        this.configurePodSecurityStandards(cluster, projectName, environment);

        // Create outputs
        this.createOutputs(projectName, environment);
    }

    /**
     * Create namespaces for different service types
     */
    private createNamespaces(cluster: eks.ICluster, projectName: string, environment: string): void {
        // Application namespace
        cluster.addManifest('ApplicationNamespace', {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: 'application',
                labels: {
                    'name': 'application',
                    'project': projectName,
                    'environment': environment,
                    'pod-security.kubernetes.io/enforce': 'restricted',
                    'pod-security.kubernetes.io/audit': 'restricted',
                    'pod-security.kubernetes.io/warn': 'restricted'
                },
                annotations: {
                    'description': 'Namespace for application services'
                }
            }
        });

        // Monitoring namespace
        cluster.addManifest('MonitoringNamespace', {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: 'monitoring',
                labels: {
                    'name': 'monitoring',
                    'project': projectName,
                    'environment': environment,
                    'pod-security.kubernetes.io/enforce': 'privileged',
                    'pod-security.kubernetes.io/audit': 'privileged',
                    'pod-security.kubernetes.io/warn': 'privileged'
                },
                annotations: {
                    'description': 'Namespace for monitoring and observability services'
                }
            }
        });

        // Data namespace
        cluster.addManifest('DataNamespace', {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: 'data',
                labels: {
                    'name': 'data',
                    'project': projectName,
                    'environment': environment,
                    'pod-security.kubernetes.io/enforce': 'restricted',
                    'pod-security.kubernetes.io/audit': 'restricted',
                    'pod-security.kubernetes.io/warn': 'restricted'
                },
                annotations: {
                    'description': 'Namespace for data processing services'
                }
            }
        });

        // Admin namespace
        cluster.addManifest('AdminNamespace', {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: {
                name: 'admin',
                labels: {
                    'name': 'admin',
                    'project': projectName,
                    'environment': environment,
                    'pod-security.kubernetes.io/enforce': 'privileged',
                    'pod-security.kubernetes.io/audit': 'privileged',
                    'pod-security.kubernetes.io/warn': 'privileged'
                },
                annotations: {
                    'description': 'Namespace for administrative services'
                }
            }
        });
    }

    /**
     * Create application service account with IRSA
     */
    private createApplicationServiceAccount(cluster: eks.ICluster, applicationRole: iam.IRole, projectName: string, environment: string): eks.ServiceAccount {
        const serviceAccount = cluster.addServiceAccount('ApplicationServiceAccount', {
            name: `${projectName}-app-sa`,
            namespace: 'application',
            annotations: {
                'eks.amazonaws.com/role-arn': applicationRole.roleArn,
                'description': 'Service account for application pods'
            },
            labels: {
                'app.kubernetes.io/name': projectName,
                'app.kubernetes.io/component': 'application',
                'app.kubernetes.io/managed-by': 'aws-cdk'
            }
        });

        // Role association is handled through the serviceAccount creation above
        // The eks.amazonaws.com/role-arn annotation is sufficient for IRSA

        return serviceAccount;
    }

    /**
     * Create monitoring service account with IRSA
     */
    private createMonitoringServiceAccount(cluster: eks.ICluster, monitoringRole: iam.IRole, projectName: string, environment: string): eks.ServiceAccount {
        const serviceAccount = cluster.addServiceAccount('MonitoringServiceAccount', {
            name: `${projectName}-monitoring-sa`,
            namespace: 'monitoring',
            annotations: {
                'eks.amazonaws.com/role-arn': monitoringRole.roleArn,
                'description': 'Service account for monitoring and observability pods'
            },
            labels: {
                'app.kubernetes.io/name': projectName,
                'app.kubernetes.io/component': 'monitoring',
                'app.kubernetes.io/managed-by': 'aws-cdk'
            }
        });

        // Role association is handled through the serviceAccount creation above
        // The eks.amazonaws.com/role-arn annotation is sufficient for IRSA

        return serviceAccount;
    }

    /**
     * Create data service account with IRSA
     */
    private createDataServiceAccount(cluster: eks.ICluster, dataAccessRole: iam.IRole, projectName: string, environment: string): eks.ServiceAccount {
        const serviceAccount = cluster.addServiceAccount('DataServiceAccount', {
            name: `${projectName}-data-sa`,
            namespace: 'data',
            annotations: {
                'eks.amazonaws.com/role-arn': dataAccessRole.roleArn,
                'description': 'Service account for data processing pods'
            },
            labels: {
                'app.kubernetes.io/name': projectName,
                'app.kubernetes.io/component': 'data',
                'app.kubernetes.io/managed-by': 'aws-cdk'
            }
        });

        // Role association is handled through the serviceAccount creation above
        // The eks.amazonaws.com/role-arn annotation is sufficient for IRSA

        return serviceAccount;
    }

    /**
     * Create admin service account with IRSA
     */
    private createAdminServiceAccount(cluster: eks.ICluster, adminRole: iam.IRole, projectName: string, environment: string): eks.ServiceAccount {
        const serviceAccount = cluster.addServiceAccount('AdminServiceAccount', {
            name: `${projectName}-admin-sa`,
            namespace: 'admin',
            annotations: {
                'eks.amazonaws.com/role-arn': adminRole.roleArn,
                'description': 'Service account for administrative pods'
            },
            labels: {
                'app.kubernetes.io/name': projectName,
                'app.kubernetes.io/component': 'admin',
                'app.kubernetes.io/managed-by': 'aws-cdk'
            }
        });

        // Role association is handled through the serviceAccount creation above
        // The eks.amazonaws.com/role-arn annotation is sufficient for IRSA

        return serviceAccount;
    }

    /**
     * Configure RBAC for service accounts
     */
    private configureRBAC(cluster: eks.ICluster, projectName: string, environment: string): void {
        // Application RBAC
        cluster.addManifest('ApplicationRole', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'Role',
            metadata: {
                namespace: 'application',
                name: `${projectName}-app-role`
            },
            rules: [
                {
                    apiGroups: [''],
                    resources: ['pods', 'services', 'configmaps', 'secrets'],
                    verbs: ['get', 'list', 'watch']
                },
                {
                    apiGroups: ['apps'],
                    resources: ['deployments', 'replicasets'],
                    verbs: ['get', 'list', 'watch']
                }
            ]
        });

        cluster.addManifest('ApplicationRoleBinding', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'RoleBinding',
            metadata: {
                name: `${projectName}-app-rolebinding`,
                namespace: 'application'
            },
            subjects: [
                {
                    kind: 'ServiceAccount',
                    name: `${projectName}-app-sa`,
                    namespace: 'application'
                }
            ],
            roleRef: {
                kind: 'Role',
                name: `${projectName}-app-role`,
                apiGroup: 'rbac.authorization.k8s.io'
            }
        });

        // Monitoring RBAC (needs cluster-wide access)
        cluster.addManifest('MonitoringClusterRole', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'ClusterRole',
            metadata: {
                name: `${projectName}-monitoring-clusterrole`
            },
            rules: [
                {
                    apiGroups: [''],
                    resources: ['nodes', 'nodes/proxy', 'services', 'endpoints', 'pods'],
                    verbs: ['get', 'list', 'watch']
                },
                {
                    apiGroups: ['extensions'],
                    resources: ['ingresses'],
                    verbs: ['get', 'list', 'watch']
                },
                {
                    apiGroups: ['apps'],
                    resources: ['deployments', 'daemonsets', 'replicasets', 'statefulsets'],
                    verbs: ['get', 'list', 'watch']
                },
                {
                    nonResourceURLs: ['/metrics'],
                    verbs: ['get']
                }
            ]
        });

        cluster.addManifest('MonitoringClusterRoleBinding', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'ClusterRoleBinding',
            metadata: {
                name: `${projectName}-monitoring-clusterrolebinding`
            },
            subjects: [
                {
                    kind: 'ServiceAccount',
                    name: `${projectName}-monitoring-sa`,
                    namespace: 'monitoring'
                }
            ],
            roleRef: {
                kind: 'ClusterRole',
                name: `${projectName}-monitoring-clusterrole`,
                apiGroup: 'rbac.authorization.k8s.io'
            }
        });

        // Data RBAC
        cluster.addManifest('DataRole', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'Role',
            metadata: {
                namespace: 'data',
                name: `${projectName}-data-role`
            },
            rules: [
                {
                    apiGroups: [''],
                    resources: ['pods', 'services', 'configmaps', 'secrets', 'persistentvolumeclaims'],
                    verbs: ['get', 'list', 'watch', 'create', 'update', 'patch']
                },
                {
                    apiGroups: ['apps'],
                    resources: ['deployments', 'jobs'],
                    verbs: ['get', 'list', 'watch', 'create', 'update', 'patch']
                },
                {
                    apiGroups: ['batch'],
                    resources: ['jobs', 'cronjobs'],
                    verbs: ['get', 'list', 'watch', 'create', 'update', 'patch']
                }
            ]
        });

        cluster.addManifest('DataRoleBinding', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'RoleBinding',
            metadata: {
                name: `${projectName}-data-rolebinding`,
                namespace: 'data'
            },
            subjects: [
                {
                    kind: 'ServiceAccount',
                    name: `${projectName}-data-sa`,
                    namespace: 'data'
                }
            ],
            roleRef: {
                kind: 'Role',
                name: `${projectName}-data-role`,
                apiGroup: 'rbac.authorization.k8s.io'
            }
        });

        // Admin RBAC (cluster admin access)
        cluster.addManifest('AdminClusterRoleBinding', {
            apiVersion: 'rbac.authorization.k8s.io/v1',
            kind: 'ClusterRoleBinding',
            metadata: {
                name: `${projectName}-admin-clusterrolebinding`
            },
            subjects: [
                {
                    kind: 'ServiceAccount',
                    name: `${projectName}-admin-sa`,
                    namespace: 'admin'
                }
            ],
            roleRef: {
                kind: 'ClusterRole',
                name: 'cluster-admin',
                apiGroup: 'rbac.authorization.k8s.io'
            }
        });
    }

    /**
     * Configure Network Policies for additional security
     */
    private configureNetworkPolicies(cluster: eks.ICluster, projectName: string, environment: string): void {
        // Application namespace network policy
        cluster.addManifest('ApplicationNetworkPolicy', {
            apiVersion: 'networking.k8s.io/v1',
            kind: 'NetworkPolicy',
            metadata: {
                name: `${projectName}-app-netpol`,
                namespace: 'application'
            },
            spec: {
                podSelector: {},
                policyTypes: ['Ingress', 'Egress'],
                ingress: [
                    {
                        from: [
                            {
                                namespaceSelector: {
                                    matchLabels: {
                                        name: 'monitoring'
                                    }
                                }
                            },
                            {
                                namespaceSelector: {
                                    matchLabels: {
                                        name: 'kube-system'
                                    }
                                }
                            }
                        ]
                    }
                ],
                egress: [
                    {
                        to: [],
                        ports: [
                            {
                                protocol: 'TCP',
                                port: 53
                            },
                            {
                                protocol: 'UDP',
                                port: 53
                            }
                        ]
                    },
                    {
                        to: [],
                        ports: [
                            {
                                protocol: 'TCP',
                                port: 443
                            },
                            {
                                protocol: 'TCP',
                                port: 80
                            }
                        ]
                    }
                ]
            }
        });

        // Data namespace network policy
        cluster.addManifest('DataNetworkPolicy', {
            apiVersion: 'networking.k8s.io/v1',
            kind: 'NetworkPolicy',
            metadata: {
                name: `${projectName}-data-netpol`,
                namespace: 'data'
            },
            spec: {
                podSelector: {},
                policyTypes: ['Ingress', 'Egress'],
                ingress: [
                    {
                        from: [
                            {
                                namespaceSelector: {
                                    matchLabels: {
                                        name: 'application'
                                    }
                                }
                            },
                            {
                                namespaceSelector: {
                                    matchLabels: {
                                        name: 'monitoring'
                                    }
                                }
                            }
                        ]
                    }
                ],
                egress: [
                    {
                        to: [],
                        ports: [
                            {
                                protocol: 'TCP',
                                port: 53
                            },
                            {
                                protocol: 'UDP',
                                port: 53
                            }
                        ]
                    },
                    {
                        to: [],
                        ports: [
                            {
                                protocol: 'TCP',
                                port: 443
                            },
                            {
                                protocol: 'TCP',
                                port: 5432
                            },
                            {
                                protocol: 'TCP',
                                port: 6379
                            },
                            {
                                protocol: 'TCP',
                                port: 9092
                            }
                        ]
                    }
                ]
            }
        });
    }

    /**
     * Configure Pod Security Standards
     */
    private configurePodSecurityStandards(cluster: eks.ICluster, projectName: string, environment: string): void {
        // Pod Security Policy for application namespace
        cluster.addManifest('ApplicationPodSecurityPolicy', {
            apiVersion: 'policy/v1beta1',
            kind: 'PodSecurityPolicy',
            metadata: {
                name: `${projectName}-app-psp`
            },
            spec: {
                privileged: false,
                allowPrivilegeEscalation: false,
                requiredDropCapabilities: ['ALL'],
                volumes: [
                    'configMap',
                    'emptyDir',
                    'projected',
                    'secret',
                    'downwardAPI',
                    'persistentVolumeClaim'
                ],
                runAsUser: {
                    rule: 'MustRunAsNonRoot'
                },
                seLinux: {
                    rule: 'RunAsAny'
                },
                fsGroup: {
                    rule: 'RunAsAny'
                }
            }
        });

        // Pod Security Policy for monitoring namespace (more permissive)
        cluster.addManifest('MonitoringPodSecurityPolicy', {
            apiVersion: 'policy/v1beta1',
            kind: 'PodSecurityPolicy',
            metadata: {
                name: `${projectName}-monitoring-psp`
            },
            spec: {
                privileged: true,
                allowPrivilegeEscalation: true,
                allowedCapabilities: ['*'],
                volumes: ['*'],
                runAsUser: {
                    rule: 'RunAsAny'
                },
                seLinux: {
                    rule: 'RunAsAny'
                },
                fsGroup: {
                    rule: 'RunAsAny'
                }
            }
        });
    }

    /**
     * Create stack outputs
     */
    private createOutputs(projectName: string, environment: string): void {
        new cdk.CfnOutput(this, 'ApplicationServiceAccountName', {
            value: this.applicationServiceAccount.serviceAccountName,
            exportName: `${this.stackName}-ApplicationServiceAccountName`,
            description: 'Name of the application service account'
        });

        new cdk.CfnOutput(this, 'MonitoringServiceAccountName', {
            value: this.monitoringServiceAccount.serviceAccountName,
            exportName: `${this.stackName}-MonitoringServiceAccountName`,
            description: 'Name of the monitoring service account'
        });

        new cdk.CfnOutput(this, 'DataServiceAccountName', {
            value: this.dataServiceAccount.serviceAccountName,
            exportName: `${this.stackName}-DataServiceAccountName`,
            description: 'Name of the data service account'
        });

        new cdk.CfnOutput(this, 'AdminServiceAccountName', {
            value: this.adminServiceAccount.serviceAccountName,
            exportName: `${this.stackName}-AdminServiceAccountName`,
            description: 'Name of the admin service account'
        });

        new cdk.CfnOutput(this, 'IRSASetupComplete', {
            value: 'true',
            exportName: `${this.stackName}-IRSASetupComplete`,
            description: 'Indicates that IRSA setup is complete'
        });
    }
}