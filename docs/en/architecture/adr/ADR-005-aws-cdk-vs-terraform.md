# ADR-005: AWS CDK vs Terraform for Infrastructure as Code

## Status

**Accepted** - 2024-01-15

## Context

The GenAI Demo project requires a robust Infrastructure as Code (IaC) solution to manage AWS resources across multiple environments (development, staging, production) and regions (Taiwan ap-east-2, Tokyo ap-northeast-1). The infrastructure includes complex components like EKS clusters, RDS Aurora Global Database, MSK clusters, and comprehensive observability stack.

### Business Objectives

- Reproducible infrastructure deployments across environments
- Version-controlled infrastructure changes
- Automated disaster recovery infrastructure provisioning
- Cost-effective resource management with environment-specific sizing
- Compliance with AWS Well-Architected Framework principles

### Technical Requirements

- Multi-region deployment (Taiwan primary, Tokyo DR)
- Complex networking (VPC, subnets, security groups, peering)
- Container orchestration (EKS with Graviton3 ARM64 nodes)
- Database replication (Aurora Global Database)
- Event streaming (MSK with cross-region replication)
- Observability stack (CloudWatch, OpenSearch, X-Ray, Prometheus, Grafana)
- SSL/TLS certificates (ACM with Route 53 validation)
- Load balancing (ALB with SSL termination)

### Team Constraints

- Java/TypeScript development expertise
- Limited DevOps team size (2-3 engineers)
- Need for rapid iteration and testing
- Integration with existing CI/CD pipelines (GitHub Actions)

## Decision

We choose **AWS CDK (Cloud Development Kit) with TypeScript** as our primary Infrastructure as Code solution.

### Implementation Strategy

#### Project Structure

```typescript
../../infrastructure/
├── bin/
│   ├── infrastructure.ts          // Main deployment entry
│   ├── multi-region-deployment.ts // Multi-region orchestration
│   └── analytics.ts               // Analytics pipeline
├── lib/
│   ├── stacks/
│   │   ├── network-stack.ts       // VPC, subnets, security groups
│   │   ├── certificate-stack.ts   // ACM certificates, Route 53
│   │   ├── core-infrastructure-stack.ts // EKS, RDS, MSK
│   │   ├── observability-stack.ts // Monitoring and logging
│   │   └── analytics-stack.ts     // Data pipeline
│   ├── constructs/
│   │   ├── eks-cluster.ts         // Reusable EKS construct
│   │   ├── aurora-global.ts       // Global database construct
│   │   └── msk-cluster.ts         // Kafka cluster construct
│   └── config/
│       └── environment-config.ts  // Environment-specific settings
├── test/
│   ├── unit/                      // Unit tests for constructs
│   ├── integration/               // Integration tests
│   └── snapshot/                  // CDK snapshot tests
└── cdk.json                       // CDK configuration
```

#### Multi-Environment Configuration

```typescript
// environment-config.ts
export interface EnvironmentConfig {
  vpc: {
    cidr: string;
    maxAzs: number;
  };
  eks: {
    nodeInstanceTypes: string[];
    minNodes: number;
    maxNodes: number;
  };
  rds: {
    instanceClass: string;
    multiAz: boolean;
    backupRetention: number;
  };
  msk: {
    brokerNodes: number;
    instanceType: string;
  };
}

export const environments: Record<string, EnvironmentConfig> = {
  development: {
    vpc: { cidr: '10.0.0.0/16', maxAzs: 2 },
    eks: { 
      nodeInstanceTypes: ['t3.medium'], 
      minNodes: 1, 
      maxNodes: 3 
    },
    rds: { 
      instanceClass: 'db.t3.micro', 
      multiAz: false, 
      backupRetention: 7 
    },
    msk: { brokerNodes: 2, instanceType: 'kafka.t3.small' }
  },
  production: {
    vpc: { cidr: '10.1.0.0/16', maxAzs: 3 },
    eks: { 
      nodeInstanceTypes: ['m6g.large'], 
      minNodes: 2, 
      maxNodes: 10 
    },
    rds: { 
      instanceClass: 'db.r6g.large', 
      multiAz: true, 
      backupRetention: 30 
    },
    msk: { brokerNodes: 3, instanceType: 'kafka.m5.large' }
  }
};
```

#### Multi-Region Deployment

```typescript
// multi-region-deployment.ts
export class MultiRegionDeployment {
  constructor(app: App) {
    const primaryRegion = 'ap-east-2';    // Taiwan
    const drRegion = 'ap-northeast-1';    // Tokyo
    
    // Primary region stack
    const primaryStack = new CoreInfrastructureStack(app, 'genai-demo-production-primary', {
      env: { region: primaryRegion },
      config: environments.production,
      isPrimary: true
    });
    
    // DR region stack
    const drStack = new CoreInfrastructureStack(app, 'genai-demo-production-dr', {
      env: { region: drRegion },
      config: environments.production,
      isPrimary: false,
      primaryStack: primaryStack
    });
    
    // Cross-region dependencies
    drStack.addDependency(primaryStack);
  }
}
```

#### Reusable Constructs

```typescript
// eks-cluster.ts
export class EksCluster extends Construct {
  public readonly cluster: eks.Cluster;
  
  constructor(scope: Construct, id: string, props: EksClusterProps) {
    super(scope, id);
    
    this.cluster = new eks.Cluster(this, 'Cluster', {
      version: eks.KubernetesVersion.V1_28,
      vpc: props.vpc,
      defaultCapacity: 0,
      endpointAccess: eks.EndpointAccess.PUBLIC_AND_PRIVATE,
      clusterLogging: [
        eks.ClusterLoggingTypes.API,
        eks.ClusterLoggingTypes.AUDIT,
        eks.ClusterLoggingTypes.AUTHENTICATOR
      ]
    });
    
    // Add Graviton3 ARM64 node group
    this.cluster.addNodegroupCapacity('GravitonNodes', {
      instanceTypes: [ec2.InstanceType.of(ec2.InstanceClass.M6G, ec2.InstanceSize.LARGE)],
      minSize: props.config.eks.minNodes,
      maxSize: props.config.eks.maxNodes,
      desiredSize: props.config.eks.minNodes,
      amiType: eks.NodegroupAmiType.AL2_ARM_64
    });
  }
}
```

## Consequences

### Positive Outcomes

#### Developer Experience

- **Type Safety**: TypeScript provides compile-time error checking
- **IDE Support**: Full IntelliSense and refactoring capabilities
- **Familiar Language**: Leverages existing TypeScript expertise
- **Object-Oriented**: Natural modeling of infrastructure components

#### AWS Integration

- **Native AWS Support**: First-class support for all AWS services
- **Latest Features**: Immediate access to new AWS services and features
- **Best Practices**: Built-in AWS best practices and patterns
- **CloudFormation**: Generates optimized CloudFormation templates

#### Testing and Validation

- **Unit Testing**: Test infrastructure logic before deployment
- **Snapshot Testing**: Detect unintended infrastructure changes
- **Integration Testing**: Validate cross-stack dependencies
- **Synthesis Validation**: Catch errors before deployment

#### Operational Benefits

- **Deployment Speed**: Faster deployments compared to Terraform
- **Rollback Capability**: CloudFormation rollback on failures
- **Change Detection**: Precise change detection and preview
- **Resource Tagging**: Automatic resource tagging and organization

### Negative Outcomes

#### Vendor Lock-in

- **AWS Specific**: Cannot easily migrate to other cloud providers
- **CloudFormation Dependency**: Tied to CloudFormation limitations
- **Service Coverage**: Some AWS services may have delayed CDK support

#### Learning Curve

- **CDK Concepts**: Team needs to learn CDK-specific patterns
- **CloudFormation Knowledge**: Understanding underlying CloudFormation helpful
- **Debugging**: More complex debugging compared to declarative tools

#### Operational Challenges

- **State Management**: CloudFormation state management complexity
- **Large Stacks**: Performance issues with very large stacks
- **Cross-Stack References**: Complex dependencies between stacks

### Mitigation Strategies

#### Multi-Cloud Preparation

- **Abstraction Layer**: Create abstraction layer for cloud resources
- **Documentation**: Maintain detailed infrastructure documentation
- **Terraform Evaluation**: Regular evaluation of Terraform for specific use cases

#### Team Training

- **CDK Workshops**: Comprehensive CDK training for team
- **Best Practices**: Establish CDK coding standards and patterns
- **Code Reviews**: Mandatory reviews for infrastructure changes

#### Operational Excellence

- **Stack Decomposition**: Break large stacks into smaller, manageable units
- **Testing Strategy**: Comprehensive testing at all levels
- **Monitoring**: Monitor deployment performance and success rates

## Alternatives Considered

### 1. Terraform

**Pros**:

- Multi-cloud support
- Large community and ecosystem
- Mature tooling and practices
- Declarative syntax

**Cons**:

- Less native AWS integration
- State management complexity
- Slower AWS feature adoption
- Limited type safety

**Verdict**: Rejected due to team expertise and AWS-native requirements

### 2. AWS CloudFormation (Raw)

**Pros**:

- Native AWS service
- No additional tools required
- Direct AWS support

**Cons**:

- Verbose YAML/JSON syntax
- Limited reusability
- No type checking
- Difficult testing

**Verdict**: Rejected due to developer experience concerns

### 3. Pulumi

**Pros**:

- Multi-cloud support
- Multiple language support
- Good type safety

**Cons**:

- Smaller community
- Additional service dependency
- Less AWS-specific optimization

**Verdict**: Considered but CDK provides better AWS integration

## Well-Architected Framework Assessment

### Operational Excellence

- **Infrastructure as Code**: Version-controlled, repeatable deployments
- **Automated Testing**: Comprehensive testing strategy prevents issues
- **Monitoring**: Built-in CloudWatch integration for infrastructure monitoring
- **Documentation**: Self-documenting code with TypeScript types

### Security

- **Least Privilege**: IAM roles and policies generated with minimal permissions
- **Encryption**: Automatic encryption at rest and in transit
- **Network Security**: VPC, security groups, and NACLs properly configured
- **Compliance**: Built-in compliance with AWS security best practices

### Reliability

- **Multi-AZ Deployment**: Automatic multi-AZ resource distribution
- **Backup and Recovery**: Automated backup configuration
- **Health Checks**: Built-in health monitoring and alerting
- **Rollback Capability**: CloudFormation rollback on deployment failures

### Performance Efficiency

- **Right-Sizing**: Environment-specific resource sizing
- **Auto Scaling**: Automatic scaling configuration
- **Monitoring**: Performance monitoring and alerting
- **ARM64 Optimization**: Graviton3 instances for cost-performance optimization

### Cost Optimization

- **Resource Tagging**: Automatic cost allocation tags
- **Environment Sizing**: Appropriate resource sizing per environment
- **Spot Instances**: Development environment cost optimization
- **Reserved Instances**: Production environment cost optimization

## Implementation Timeline

### Phase 1: Foundation (Weeks 1-2)

- Set up CDK project structure
- Implement basic networking stack
- Create development environment

### Phase 2: Core Services (Weeks 3-4)

- Implement EKS cluster
- Set up RDS Aurora
- Configure MSK cluster

### Phase 3: Observability (Weeks 5-6)

- Implement monitoring stack
- Set up logging pipeline
- Configure alerting

### Phase 4: Multi-Region (Weeks 7-8)

- Implement DR region
- Configure cross-region replication
- Test failover procedures

### Phase 5: Optimization (Weeks 9-10)

- Performance tuning
- Cost optimization
- Security hardening

## Monitoring and Success Metrics

### Deployment Metrics

- **Deployment Success Rate**: > 95%
- **Deployment Time**: < 30 minutes for full stack
- **Rollback Time**: < 10 minutes
- **Change Failure Rate**: < 5%

### Operational Metrics

- **Infrastructure Drift**: Zero tolerance
- **Security Compliance**: 100% compliance with security policies
- **Cost Variance**: < 10% variance from budget
- **Availability**: > 99.9% uptime

### Team Metrics

- **Developer Productivity**: Reduced infrastructure change lead time
- **Error Rate**: Reduced infrastructure-related incidents
- **Knowledge Sharing**: Team members can contribute to infrastructure

## Related Decisions

- \1
- \1
- \1
- [ADR-016: Well-Architected Framework Compliance](./ADR-016-well-architected-compliance.md)

## References

- [AWS CDK Developer Guide](https://docs.aws.amazon.com/cdk/v2/guide/)
- [AWS CDK API Reference](https://docs.aws.amazon.com/cdk/api/v2/)
- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [Infrastructure as Code Best Practices](https://aws.amazon.com/blogs/devops/best-practices-for-developing-cloud-applications-with-aws-cdk/)
