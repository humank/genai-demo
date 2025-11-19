# ADR-018: Container Orchestration with Amazon EKS

**Status**: Accepted  
**Date**: 2024-11-19  
**Decision Makers**: Architecture Team, DevOps Team

---

## Context

The GenAI Demo e-commerce platform requires a robust container orchestration solution to manage microservices deployment, scaling, and operations. We need to choose between self-managed Kubernetes, Amazon EKS, or alternative orchestration platforms.

### Business Context

- **Scalability**: Handle variable traffic loads (10x during peak)
- **Availability**: 99.9% uptime requirement
- **Cost**: Optimize infrastructure costs
- **Team Expertise**: Limited Kubernetes expertise
- **Time to Market**: Fast deployment required

### Technical Context

- **Current State**: Monolithic application on EC2
- **Target State**: Microservices architecture
- **Constraints**: AWS-based infrastructure
- **Dependencies**: RDS, ElastiCache, MSK

---

## Decision Drivers

1. **Operational Overhead**: Minimize cluster management burden
2. **AWS Integration**: Seamless integration with AWS services
3. **Scalability**: Auto-scaling capabilities
4. **Security**: Built-in security features
5. **Cost**: Predictable pricing model
6. **Team Productivity**: Reduce learning curve

---

## Considered Options

### Option 1: Self-Managed Kubernetes on EC2

**Pros**:
- Full control over cluster configuration
- No EKS control plane costs
- Flexibility in version management
- Custom networking configurations

**Cons**:
- High operational overhead
- Manual upgrades and patching
- Complex high availability setup
- Requires deep Kubernetes expertise
- Security management burden

**Cost**: Lower infrastructure cost, higher operational cost  
**Risk**: High - Complex operations, potential downtime

### Option 2: Amazon EKS (Elastic Kubernetes Service)

**Pros**:
- Managed control plane
- Automatic upgrades and patching
- AWS service integration (IAM, VPC, ALB)
- High availability by default
- Reduced operational overhead
- AWS support and SLAs

**Cons**:
- Control plane costs ($0.10/hour per cluster)
- Less flexibility in control plane configuration
- AWS vendor lock-in
- Learning curve for EKS-specific features

**Cost**: $73/month per cluster + worker nodes  
**Risk**: Low - Managed service, proven reliability

### Option 3: Amazon ECS (Elastic Container Service)

**Pros**:
- Simpler than Kubernetes
- Deep AWS integration
- No control plane costs
- Easier learning curve

**Cons**:
- Limited ecosystem compared to Kubernetes
- Less portable
- Fewer advanced features
- Smaller community

**Cost**: No control plane costs, only worker nodes  
**Risk**: Medium - Less flexibility, potential migration challenges

---

## Decision Outcome

**Chosen Option**: Amazon EKS (Option 2)

### Rationale

1. **Managed Operations**: EKS handles control plane management, reducing operational burden by ~70%
2. **AWS Integration**: Native integration with IAM, VPC, ALB, and other AWS services
3. **Industry Standard**: Kubernetes is the de facto standard for container orchestration
4. **Ecosystem**: Rich ecosystem of tools and community support
5. **Scalability**: Proven ability to handle large-scale deployments
6. **Future-Proof**: Kubernetes skills are transferable and widely applicable

### Implementation Details

```yaml
# EKS Cluster Configuration
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: genai-demo-prod
  region: us-west-2
  version: "1.28"

vpc:
  cidr: 10.0.0.0/16
  nat:
    gateway: HighlyAvailable

managedNodeGroups:
  - name: app-nodes
    instanceType: t3.medium
    minSize: 3
    maxSize: 10
    desiredCapacity: 3
    volumeSize: 50
    ssh:
      allow: false
    labels:
      role: application
    tags:
      Environment: production
      
  - name: system-nodes
    instanceType: t3.small
    minSize: 2
    maxSize: 4
    desiredCapacity: 2
    labels:
      role: system
    taints:
      - key: system
        value: "true"
        effect: NoSchedule

addons:
  - name: vpc-cni
  - name: coredns
  - name: kube-proxy
  - name: aws-ebs-csi-driver

iam:
  withOIDC: true
  serviceAccounts:
    - metadata:
        name: aws-load-balancer-controller
        namespace: kube-system
      wellKnownPolicies:
        awsLoadBalancerController: true
```

---

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn Kubernetes concepts | Training, documentation, gradual migration |
| DevOps Team | High | New deployment and monitoring tools | EKS training, AWS support |
| Operations Team | Medium | New monitoring and troubleshooting | Runbooks, training, AWS support |
| Business | Low | Transparent to end users | Phased rollout, testing |

### Impact Radius

**Selected Impact Radius**: System-wide

- Affects all microservices deployment
- Changes CI/CD pipelines
- New monitoring and logging setup
- Infrastructure as Code updates

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|-------------------|
| Learning curve delays | Medium | Medium | Training program, AWS support, phased rollout |
| Migration complexity | Medium | High | Detailed migration plan, staging environment testing |
| Cost overruns | Low | Medium | Cost monitoring, right-sizing instances |
| Service disruption | Low | High | Blue-green deployment, comprehensive testing |

**Overall Risk Level**: Medium

---

## Implementation Plan

### Phase 1: Preparation (Weeks 1-2)

- [ ] EKS cluster setup in development environment
- [ ] Team training on Kubernetes and EKS
- [ ] CI/CD pipeline updates
- [ ] Monitoring and logging setup

### Phase 2: Migration (Weeks 3-6)

- [ ] Migrate non-critical services
- [ ] Test auto-scaling and failover
- [ ] Performance testing
- [ ] Security audit

### Phase 3: Production Rollout (Weeks 7-8)

- [ ] Migrate critical services
- [ ] Blue-green deployment
- [ ] Production monitoring
- [ ] Documentation updates

### Rollback Strategy

**Trigger Conditions**:
- Service availability < 99%
- Response time degradation > 50%
- Critical functionality unavailable > 5 minutes

**Rollback Steps**:
1. Route traffic back to EC2 instances
2. Disable EKS deployments
3. Verify service health
4. Investigate and fix issues

**Rollback Time**: < 15 minutes

---

## Monitoring and Success Criteria

### Success Metrics

- [ ] Deployment time reduced by 50%
- [ ] Auto-scaling response time < 2 minutes
- [ ] Service availability > 99.9%
- [ ] Infrastructure costs within 20% of projections
- [ ] Zero critical incidents during migration

### Monitoring Plan

- **CloudWatch**: EKS cluster metrics, node health
- **Prometheus**: Application metrics, custom metrics
- **Grafana**: Visualization dashboards
- **X-Ray**: Distributed tracing
- **Cost Explorer**: Cost monitoring and optimization

---

## Consequences

### Positive Consequences

- ✅ Reduced operational overhead (70% reduction in cluster management)
- ✅ Improved scalability and reliability
- ✅ Better AWS service integration
- ✅ Industry-standard platform
- ✅ Rich ecosystem and community support

### Negative Consequences

- ❌ Additional cost ($73/month per cluster)
- ❌ Learning curve for team
- ❌ Some AWS vendor lock-in
- ❌ Migration complexity

### Technical Debt

- Need to migrate existing monitoring to Kubernetes-native tools
- Update all deployment scripts and CI/CD pipelines
- Retrain team on Kubernetes operations

**Debt Repayment Plan**: 6 months post-migration

---

## Related Decisions

- [ADR-015: Microservices Architecture](015-microservices-architecture.md)
- [ADR-019: Service Mesh with Istio](019-service-mesh-istio.md)
- [ADR-020: GitOps with ArgoCD](020-gitops-argocd.md)

---

## References

- [Amazon EKS Documentation](https://docs.aws.amazon.com/eks/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [EKS Best Practices Guide](https://aws.github.io/aws-eks-best-practices/)
- [Cost Optimization Guide](https://aws.amazon.com/eks/pricing/)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Architecture Team  
**Reviewers**: DevOps Team, Security Team
