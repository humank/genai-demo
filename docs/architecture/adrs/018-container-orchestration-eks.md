---
adr_number: 018
title: "Container Orchestration with Amazon EKS"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 002, 019, 020, 021]
affected_viewpoints: ["deployment", "operational", "development"]
affected_perspectives: ["performance", "availability", "security", "cost"]
---

# ADR-018: Container Orchestration with Amazon EKS

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The GenAI Demo e-commerce platform requires a robust container orchestration solution to manage microservices deployment, scaling, and operations. As we transition from a monolithic architecture to microservices, we need a platform that can:

- Orchestrate dozens of containerized microservices
- Handle automatic scaling based on traffic patterns
- Provide service discovery and load balancing
- Support rolling deployments with zero downtime
- Enable consistent deployments across environments
- Integrate seamlessly with AWS infrastructure

### Business Context

**Business Drivers**:

- **Scalability**: Handle variable traffic loads (10x during peak shopping events like Double 11, Black Friday)
- **Availability**: 99.9% uptime requirement for customer-facing services
- **Cost Optimization**: Efficient resource utilization to minimize infrastructure costs
- **Time to Market**: Fast deployment cycles (multiple deployments per day)
- **Global Expansion**: Support multi-region deployment for Taiwan and international markets

**Constraints**:

- Budget: $15,000/month for container infrastructure
- Team expertise: Limited Kubernetes expertise (2 engineers with basic knowledge)
- Timeline: 8 weeks to production-ready cluster
- Compliance: Must support PCI-DSS for payment processing
- Geopolitical: Taiwan-specific resilience requirements

### Technical Context

**Current State**:

- Monolithic Spring Boot application on EC2 Auto Scaling Groups
- Manual deployment process (2-4 hours per deployment)
- Limited horizontal scaling capabilities
- No container orchestration in place

**Target State**:

- 15+ microservices in containerized architecture
- Automated CI/CD with GitOps
- Auto-scaling based on metrics
- Blue-green and canary deployments

**Dependencies**:

- AWS RDS PostgreSQL (ADR-001)
- Amazon ElastiCache Redis
- Amazon MSK (Kafka)
- AWS ALB for ingress
- AWS ECR for container registry

## Decision Drivers

1. **Operational Overhead**: Minimize cluster management burden for small DevOps team
2. **AWS Integration**: Seamless integration with existing AWS services (IAM, VPC, ALB, CloudWatch)
3. **Scalability**: Auto-scaling capabilities for both pods and nodes
4. **Security**: Built-in security features, RBAC, network policies, secrets management
5. **Cost Predictability**: Clear pricing model with optimization opportunities
6. **Team Productivity**: Reduce learning curve while maintaining industry-standard skills
7. **Ecosystem**: Access to Kubernetes ecosystem (Helm, operators, monitoring tools)
8. **Portability**: Avoid complete vendor lock-in, maintain migration options

## Considered Options

### Option 1: Self-Managed Kubernetes on EC2

**Description**: Deploy and manage Kubernetes clusters using kubeadm or kops on EC2 instances

**Pros**:

- ✅ Full control over cluster configuration and version
- ✅ No EKS control plane costs ($0.10/hour savings)
- ✅ Flexibility in networking and storage configurations
- ✅ Custom control plane configurations
- ✅ No AWS-specific limitations

**Cons**:

- ❌ High operational overhead (estimated 40+ hours/month)
- ❌ Manual upgrades and security patching
- ❌ Complex high availability setup for control plane
- ❌ Requires deep Kubernetes expertise (3+ years experience)
- ❌ Security management burden (etcd encryption, API server hardening)
- ❌ No AWS support for cluster issues
- ❌ DIY monitoring and logging setup

**Cost**:

- Infrastructure: ~$800/month (3 master nodes + worker nodes)
- Operational: ~$8,000/month (40 hours × $200/hour engineer time)
- **Total**: ~$8,800/month

**Risk**: **High** - Complex operations, potential downtime during upgrades, security vulnerabilities

### Option 2: Amazon EKS (Elastic Kubernetes Service) ⭐

**Description**: AWS-managed Kubernetes service with managed control plane

**Pros**:

- ✅ Managed control plane (automatic upgrades, patching, HA)
- ✅ Native AWS integration (IAM, VPC, ALB, CloudWatch, X-Ray)
- ✅ High availability by default (multi-AZ control plane)
- ✅ Reduced operational overhead (estimated 10 hours/month)
- ✅ AWS support and SLAs (99.95% control plane availability)
- ✅ EKS Managed Node Groups for simplified node management
- ✅ Fargate support for serverless containers
- ✅ Industry-standard Kubernetes (portable skills)
- ✅ Rich ecosystem compatibility (Helm, ArgoCD, Prometheus)
- ✅ AWS security features (Pod Identity, Secrets Manager integration)

**Cons**:

- ⚠️ Control plane costs ($0.10/hour = $73/month per cluster)
- ⚠️ Less flexibility in control plane configuration
- ⚠️ Some AWS vendor lock-in (IAM roles, ALB controller)
- ⚠️ EKS-specific learning curve (IRSA, ALB Ingress)
- ⚠️ Upgrade windows may not align with preferences

**Cost**:

- Control plane: $73/month per cluster
- Worker nodes: ~$1,200/month (6 × t3.medium)
- Load balancer: ~$50/month
- Data transfer: ~$100/month
- **Total**: ~$1,423/month (production cluster)

**Risk**: **Low** - Managed service, proven reliability, AWS support

### Option 3: Amazon ECS (Elastic Container Service)

**Description**: AWS-native container orchestration service

**Pros**:

- ✅ Simpler than Kubernetes (lower learning curve)
- ✅ Deep AWS integration (native IAM, CloudWatch)
- ✅ No control plane costs
- ✅ Fargate support for serverless
- ✅ Easier for AWS-experienced teams
- ✅ Simpler networking model

**Cons**:

- ❌ Limited ecosystem compared to Kubernetes
- ❌ Less portable (AWS-specific)
- ❌ Fewer advanced features (no custom operators)
- ❌ Smaller community and fewer resources
- ❌ Limited third-party tool integration
- ❌ No Helm equivalent for package management
- ❌ Skills not transferable outside AWS

**Cost**:

- No control plane costs
- Worker nodes: ~$1,200/month
- **Total**: ~$1,200/month

**Risk**: **Medium** - Less flexibility, potential migration challenges, limited ecosystem

### Option 4: Amazon EKS with Fargate

**Description**: EKS with serverless compute using AWS Fargate

**Pros**:

- ✅ No node management required
- ✅ Pay-per-pod pricing
- ✅ Automatic scaling
- ✅ Enhanced security isolation
- ✅ Reduced operational overhead

**Cons**:

- ❌ Higher cost for sustained workloads (2-3x EC2)
- ❌ Limited pod configuration options
- ❌ No DaemonSets support
- ❌ Cold start latency (30-60 seconds)
- ❌ Limited storage options
- ❌ No GPU support

**Cost**:

- Control plane: $73/month
- Fargate compute: ~$3,500/month (based on vCPU/memory hours)
- **Total**: ~$3,573/month

**Risk**: **Low** - Managed service, but higher cost

## Decision Outcome

**Chosen Option**: **Amazon EKS with Managed Node Groups (Option 2)**

### Rationale

Amazon EKS was selected as the container orchestration platform for the following reasons:

1. **Managed Operations**: EKS handles control plane management, reducing operational burden by ~70% compared to self-managed Kubernetes. This is critical given our limited Kubernetes expertise.

2. **AWS Integration**: Native integration with IAM (Pod Identity), VPC (CNI), ALB (Ingress Controller), CloudWatch, and X-Ray provides seamless observability and security without additional tooling.

3. **Industry Standard**: Kubernetes is the de facto standard for container orchestration. Skills learned are transferable, and the ecosystem provides solutions for almost any requirement.

4. **Ecosystem Access**: Full access to Kubernetes ecosystem including Helm charts, operators, service meshes (Istio/Linkerd), and GitOps tools (ArgoCD, Flux).

5. **Scalability**: Cluster Autoscaler and Karpenter provide efficient node scaling. Horizontal Pod Autoscaler handles application scaling.

6. **Security**: Built-in features like Pod Security Standards, Network Policies, Secrets encryption, and IRSA meet our compliance requirements.

7. **Cost-Effective**: While EKS has control plane costs, the reduced operational overhead and efficient resource utilization make it more cost-effective than self-managed options.

8. **Future-Proof**: Can leverage Fargate for specific workloads, add Spot instances for cost optimization, or migrate to multi-cluster setup as we scale.

**Why Not Self-Managed**: The operational overhead and expertise requirements are too high for our team size. The $73/month control plane cost is negligible compared to the engineering time saved.

**Why Not ECS**: While simpler, ECS lacks the ecosystem and portability of Kubernetes. Our team's Kubernetes skills would be more valuable long-term.

**Why Not Fargate-Only**: The cost premium (2-3x) is not justified for our sustained workloads. We'll use Fargate selectively for batch jobs and spiky workloads.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | High | New deployment model, Kubernetes concepts | Training program, documentation, gradual migration |
| DevOps Team | High | New platform to manage, CI/CD changes | EKS certification, AWS support, runbooks |
| Operations Team | Medium | New monitoring and troubleshooting tools | Training, dashboards, alerting setup |
| QA Team | Medium | New testing environments | Namespace isolation, preview environments |
| Security Team | Medium | New security model (RBAC, network policies) | Security review, policy templates |
| Business | Low | Transparent to end users | Phased rollout, comprehensive testing |

### Impact Radius

**Selected Impact Radius**: **System-wide**

This decision affects:

- **All Microservices**: Every service will be deployed on EKS
- **CI/CD Pipelines**: Complete overhaul of deployment processes
- **Monitoring Stack**: New Prometheus/Grafana setup, CloudWatch integration
- **Networking**: VPC CNI, service mesh considerations
- **Security**: RBAC policies, network policies, secrets management
- **Infrastructure as Code**: CDK/Terraform updates for EKS resources
- **Developer Workflow**: Local development with minikube/kind, kubectl usage

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Learning curve delays project | Medium | Medium | Structured training program, AWS support, hire consultant for initial setup |
| Migration causes service disruption | Low | High | Blue-green deployment, comprehensive testing, rollback plan |
| Cost exceeds projections | Low | Medium | Cost monitoring dashboards, right-sizing, Spot instances |
| Security misconfiguration | Medium | High | Security review, Pod Security Standards, network policies, regular audits |
| Cluster upgrade issues | Low | Medium | Test upgrades in staging, follow AWS upgrade guide, maintain rollback capability |
| Node scaling issues during peak | Medium | High | Pre-scale before events, Karpenter for fast scaling, capacity reservations |

**Overall Risk Level**: **Medium**

## Implementation Plan

### Phase 1: Foundation Setup (Weeks 1-2)

**Infrastructure Setup**:

- [ ] Create EKS cluster using AWS CDK/Terraform
- [ ] Configure VPC with public/private subnets
- [ ] Set up Managed Node Groups (3 node groups: system, application, batch)
- [ ] Install core add-ons (VPC CNI, CoreDNS, kube-proxy, EBS CSI driver)
- [ ] Configure AWS Load Balancer Controller
- [ ] Set up External DNS for Route 53 integration
- [ ] Configure cluster autoscaler or Karpenter

**Security Setup**:

- [ ] Configure IRSA (IAM Roles for Service Accounts)
- [ ] Set up Pod Security Standards (restricted/baseline)
- [ ] Create RBAC roles and bindings
- [ ] Configure network policies
- [ ] Set up Secrets Store CSI Driver with AWS Secrets Manager
- [ ] Enable control plane logging to CloudWatch

### Phase 2: Observability & CI/CD (Weeks 3-4)

**Monitoring Setup**:

- [ ] Deploy Prometheus stack (kube-prometheus-stack)
- [ ] Configure Grafana dashboards
- [ ] Set up CloudWatch Container Insights
- [ ] Configure AWS X-Ray for distributed tracing
- [ ] Set up Fluent Bit for log aggregation
- [ ] Create alerting rules and PagerDuty integration

**CI/CD Setup**:

- [ ] Configure GitHub Actions for container builds
- [ ] Set up ArgoCD for GitOps deployments
- [ ] Create Helm charts for all services
- [ ] Implement image scanning with Trivy
- [ ] Set up preview environments for PRs
- [ ] Configure deployment notifications

### Phase 3: Application Migration (Weeks 5-6)

**Non-Critical Services**:

- [ ] Migrate notification service
- [ ] Migrate product catalog service
- [ ] Migrate search service
- [ ] Test auto-scaling behavior
- [ ] Validate monitoring and alerting
- [ ] Performance testing

**Critical Services**:

- [ ] Migrate order service
- [ ] Migrate payment service
- [ ] Migrate customer service
- [ ] Implement circuit breakers
- [ ] Configure pod disruption budgets
- [ ] Set up horizontal pod autoscalers

### Phase 4: Production Rollout (Weeks 7-8)

**Production Deployment**:

- [ ] Blue-green deployment to production
- [ ] Traffic shifting (10% → 50% → 100%)
- [ ] 24/7 monitoring during transition
- [ ] Performance validation
- [ ] Security audit
- [ ] Documentation updates

**Optimization**:

- [ ] Right-size node instances
- [ ] Implement Spot instances for non-critical workloads
- [ ] Configure resource quotas and limits
- [ ] Set up cost allocation tags
- [ ] Create operational runbooks

### Rollback Strategy

**Trigger Conditions**:

- Service availability drops below 99%
- Response time degradation > 50% (p95)
- Critical functionality unavailable > 5 minutes
- Data integrity issues detected
- Security incident related to EKS

**Rollback Steps**:

1. **Immediate** (< 5 minutes):
   - Route traffic back to EC2 instances via ALB target group switch
   - Scale up EC2 Auto Scaling Group
   - Disable EKS ingress

2. **Short-term** (< 30 minutes):
   - Verify EC2 services are healthy
   - Update DNS if needed
   - Notify stakeholders

3. **Investigation** (< 24 hours):
   - Analyze root cause
   - Document findings
   - Plan remediation

**Rollback Time**: < 15 minutes for traffic switch, < 1 hour for full rollback

**Rollback Testing**: Monthly rollback drills in staging environment

## Monitoring and Success Criteria

### Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Deployment frequency | > 10/week | ArgoCD metrics |
| Deployment success rate | > 99% | ArgoCD metrics |
| Mean time to deploy | < 10 minutes | CI/CD pipeline metrics |
| Service availability | > 99.9% | CloudWatch/Prometheus |
| Pod startup time | < 30 seconds | Kubernetes metrics |
| Auto-scaling response | < 2 minutes | Custom metrics |
| Infrastructure cost | < $2,000/month | AWS Cost Explorer |
| Incident count (EKS-related) | < 2/month | PagerDuty |
| Mean time to recovery | < 15 minutes | Incident tracking |

### Monitoring Plan

**Cluster Health**:

```yaml
# Key metrics to monitor
- kube_node_status_condition
- kube_pod_status_phase
- kube_deployment_status_replicas_available
- container_cpu_usage_seconds_total
- container_memory_usage_bytes
- kube_pod_container_status_restarts_total
```

**Application Health**:

```yaml
# Application metrics
- http_requests_total
- http_request_duration_seconds
- http_requests_in_flight
- application_errors_total
```

**AWS Integration**:

- CloudWatch Container Insights for node/pod metrics
- AWS X-Ray for distributed tracing
- CloudWatch Logs for centralized logging
- AWS Cost Explorer for cost tracking

**Alerting Rules**:

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| HighPodRestarts | restarts > 5 in 10m | Warning | Investigate logs |
| NodeNotReady | node not ready > 5m | Critical | Page on-call |
| HighMemoryUsage | memory > 85% | Warning | Scale or optimize |
| DeploymentFailed | rollout failed | Critical | Rollback |
| ClusterAutoscalerFailed | scaling failed | Critical | Manual intervention |

**Review Schedule**:

- **Daily**: Check Grafana dashboards, review alerts
- **Weekly**: Capacity planning, cost review
- **Monthly**: Security audit, upgrade planning
- **Quarterly**: Architecture review, optimization

## Consequences

### Positive Consequences

- ✅ **Reduced Operational Overhead**: 70% reduction in cluster management time (40 hours → 10 hours/month)
- ✅ **Improved Deployment Velocity**: From 2-4 hours to < 10 minutes per deployment
- ✅ **Better Scalability**: Auto-scaling handles 10x traffic spikes automatically
- ✅ **Enhanced Reliability**: Multi-AZ deployment, self-healing pods, rolling updates
- ✅ **Industry-Standard Platform**: Kubernetes skills are transferable and valuable
- ✅ **Rich Ecosystem**: Access to Helm, operators, service meshes, GitOps tools
- ✅ **AWS Integration**: Seamless IAM, VPC, ALB, CloudWatch integration
- ✅ **Security Improvements**: RBAC, network policies, pod security standards
- ✅ **Cost Visibility**: Better resource tracking and optimization opportunities

### Negative Consequences

- ⚠️ **Control Plane Cost**: $73/month per cluster ($876/year)
- ⚠️ **Learning Curve**: Team needs 2-4 weeks to become proficient
- ⚠️ **AWS Vendor Lock-in**: Some EKS-specific features (IRSA, ALB controller)
- ⚠️ **Complexity Increase**: More moving parts than EC2-based deployment
- ⚠️ **Migration Effort**: 8 weeks of dedicated work for full migration

### Technical Debt

**Identified Debt**:

1. **No Service Mesh**: Initial deployment without Istio/Linkerd (acceptable for current scale)
2. **Single Cluster**: No multi-cluster setup for disaster recovery
3. **Manual Secrets Rotation**: Not fully automated yet
4. **Limited GitOps Coverage**: Some resources still managed manually

**Debt Repayment Plan**:

| Debt Item | Timeline | Effort |
|-----------|----------|--------|
| Service mesh evaluation | Q2 2026 | 2 weeks |
| Multi-cluster DR setup | Q3 2026 | 4 weeks |
| Automated secrets rotation | Q2 2026 | 1 week |
| Full GitOps coverage | Q1 2026 | 2 weeks |

## Configuration Examples

### EKS Cluster Configuration (CDK)

```typescript
import * as eks from 'aws-cdk-lib/aws-eks';
import * as ec2 from 'aws-cdk-lib/aws-ec2';

const cluster = new eks.Cluster(this, 'GenAIDemoCluster', {
  version: eks.KubernetesVersion.V1_29,
  clusterName: 'genai-demo-prod',
  defaultCapacity: 0, // We'll add managed node groups
  endpointAccess: eks.EndpointAccess.PRIVATE,
  vpc: vpc,
  vpcSubnets: [{ subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }],

  // Enable control plane logging
  clusterLogging: [
    eks.ClusterLoggingTypes.API,
    eks.ClusterLoggingTypes.AUDIT,
    eks.ClusterLoggingTypes.AUTHENTICATOR,
    eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
    eks.ClusterLoggingTypes.SCHEDULER,
  ],
});

// Application node group
cluster.addNodegroupCapacity('AppNodes', {
  instanceTypes: [ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MEDIUM)],
  minSize: 3,
  maxSize: 10,
  desiredSize: 3,
  diskSize: 50,
  labels: { role: 'application' },
  tags: { Environment: 'production' },
});

// System node group
cluster.addNodegroupCapacity('SystemNodes', {
  instanceTypes: [ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.SMALL)],
  minSize: 2,
  maxSize: 4,
  desiredSize: 2,
  labels: { role: 'system' },
  taints: [{ key: 'system', value: 'true', effect: eks.TaintEffect.NO_SCHEDULE }],
});
```

### eksctl Configuration

```yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: genai-demo-prod
  region: ap-northeast-1
  version: "1.29"
  tags:
    Environment: production
    Project: genai-demo
    CostCenter: engineering

vpc:
  cidr: 10.0.0.0/16
  nat:
    gateway: HighlyAvailable
  clusterEndpoints:
    privateAccess: true
    publicAccess: false

iam:
  withOIDC: true
  serviceAccounts:
    - metadata:
        name: aws-load-balancer-controller
        namespace: kube-system
      wellKnownPolicies:
        awsLoadBalancerController: true
    - metadata:
        name: external-dns
        namespace: kube-system
      wellKnownPolicies:
        externalDNS: true
    - metadata:
        name: cluster-autoscaler
        namespace: kube-system
      wellKnownPolicies:
        autoScaler: true

managedNodeGroups:
  - name: app-nodes
    instanceType: t3.medium
    minSize: 3
    maxSize: 10
    desiredCapacity: 3
    volumeSize: 50
    volumeType: gp3
    privateNetworking: true
    labels:
      role: application
    tags:
      Environment: production
    iam:
      withAddonPolicies:
        imageBuilder: true
        autoScaler: true
        cloudWatch: true

  - name: system-nodes
    instanceType: t3.small
    minSize: 2
    maxSize: 4
    desiredCapacity: 2
    volumeSize: 30
    volumeType: gp3
    privateNetworking: true
    labels:
      role: system
    taints:
      - key: system
        value: "true"
        effect: NoSchedule

addons:
  - name: vpc-cni
    version: latest
    configurationValues: '{"enableNetworkPolicy": "true"}'
  - name: coredns
    version: latest
  - name: kube-proxy
    version: latest
  - name: aws-ebs-csi-driver
    version: latest
    serviceAccountRoleARN: arn:aws:iam::ACCOUNT_ID:role/EBSCSIDriverRole

cloudWatch:
  clusterLogging:
    enableTypes:
      - api
      - audit
      - authenticator
      - controllerManager
      - scheduler
    logRetentionInDays: 30
```

### Helm Values for Core Services

```yaml
# values-production.yaml
replicaCount: 3

image:
  repository: ACCOUNT_ID.dkr.ecr.ap-northeast-1.amazonaws.com/order-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

podDisruptionBudget:
  enabled: true
  minAvailable: 2

serviceAccount:
  create: true
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT_ID:role/OrderServiceRole

ingress:
  enabled: true
  className: alb
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/healthcheck-path: /actuator/health
    alb.ingress.kubernetes.io/ssl-redirect: "443"
  hosts:
    - host: api.genai-demo.com
      paths:
        - path: /api/orders
          pathType: Prefix

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5

env:
  - name: SPRING_PROFILES_ACTIVE
    value: production
  - name: JAVA_OPTS
    value: "-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"
```

## Related Decisions

- **ADR-001**: [PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - Database accessed from EKS pods
- **ADR-002**: [Hexagonal Architecture](002-adopt-hexagonal-architecture.md) - Microservices architecture deployed on EKS
- **ADR-019**: [CI/CD Pipeline Strategy](019-cicd-pipeline-strategy.md) - Deployment automation for EKS
- **ADR-020**: [Database Migration Strategy](020-database-migration-strategy-flyway.md) - Flyway migrations run as Kubernetes Jobs
- **ADR-021**: [API Gateway Strategy](021-api-gateway-strategy.md) - API Gateway integration with EKS ingress

## References

- [Amazon EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Amazon EKS Best Practices Guide](https://aws.github.io/aws-eks-best-practices/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [EKS Blueprints](https://aws-quickstart.github.io/cdk-eks-blueprints/)
- [Karpenter Documentation](https://karpenter.sh/)
- [ArgoCD Documentation](https://argo-cd.readthedocs.io/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)

---

**Document Status**: ✅ Accepted
**Last Reviewed**: 2025-10-24
**Next Review**: 2026-01-24 (Quarterly)
**Owner**: Architecture Team, DevOps Team
**Reviewers**: Security Team, Platform Team
