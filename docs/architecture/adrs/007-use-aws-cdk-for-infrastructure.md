---
adr_number: 007
title: "Use AWS CDK for Infrastructure as Code"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [001, 005]
affected_viewpoints: ["deployment", "operational"]
affected_perspectives: ["evolution", "development-resource"]
---

# ADR-007: Use AWS CDK for Infrastructure as Code

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a robust Infrastructure as Code (IaC) solution that:

- Provisions and manages AWS infrastructure reliably
- Supports version control and code review for infrastructure changes
- Enables automated deployment and rollback
- Provides type safety and IDE support
- Integrates well with CI/CD pipelines
- Supports multiple environments (dev, staging, production)
- Allows infrastructure testing before deployment
- Maintains consistency across environments

### Business Context

**Business Drivers**:
- Need for repeatable, consistent infrastructure deployments
- Requirement for disaster recovery and environment replication
- Compliance requirements for infrastructure audit trails
- Team growth requiring infrastructure automation
- Multi-region deployment for global users
- Cost optimization through infrastructure as code

**Constraints**:
- AWS cloud platform (already decided)
- Team has strong Java/TypeScript experience
- Limited DevOps/infrastructure experience
- Budget: Infrastructure management should not require dedicated team
- Timeline: 3 months to production deployment

### Technical Context

**Current State**:
- AWS cloud infrastructure
- Spring Boot application (Java 21)
- PostgreSQL on RDS (ADR-001)
- Kafka on MSK (ADR-005)
- EKS for container orchestration
- Multiple environments needed (dev, staging, production)

**Requirements**:
- Provision VPC, subnets, security groups
- Manage RDS PostgreSQL instances
- Configure EKS clusters
- Set up MSK Kafka clusters
- Configure ElastiCache Redis
- Manage IAM roles and policies
- Set up CloudWatch monitoring
- Support multi-region deployment

## Decision Drivers

1. **Type Safety**: Catch infrastructure errors at compile time
2. **Developer Experience**: Familiar programming language (TypeScript/Java)
3. **AWS Integration**: Native AWS service support
4. **Testing**: Ability to test infrastructure code
5. **Reusability**: Create reusable infrastructure components
6. **Team Skills**: Leverage existing programming skills
7. **Maintainability**: Easy to understand and modify
8. **Cost**: No additional licensing costs

## Considered Options

### Option 1: AWS CDK (Cloud Development Kit)

**Description**: Infrastructure as Code using TypeScript/Python/Java with AWS constructs

**Pros**:
- ✅ Type-safe infrastructure code (TypeScript/Java)
- ✅ Full IDE support (autocomplete, refactoring)
- ✅ Familiar programming language for team
- ✅ Reusable constructs and patterns
- ✅ Built-in testing framework
- ✅ Native AWS service support
- ✅ Synthesizes to CloudFormation
- ✅ Active AWS development and support
- ✅ Higher-level abstractions (L2/L3 constructs)
- ✅ Can use npm packages for extensions

**Cons**:
- ⚠️ Learning curve for CDK concepts
- ⚠️ Generates CloudFormation (inherits CF limitations)
- ⚠️ Younger than Terraform (less mature ecosystem)
- ⚠️ AWS-specific (vendor lock-in)

**Cost**: $0 (open source)

**Risk**: **Low** - AWS-backed, growing adoption

### Option 2: Terraform

**Description**: HashiCorp's infrastructure as code tool with HCL language

**Pros**:
- ✅ Multi-cloud support (not AWS-specific)
- ✅ Mature ecosystem and community
- ✅ Large module library
- ✅ State management built-in
- ✅ Plan/apply workflow
- ✅ Wide industry adoption

**Cons**:
- ❌ HCL is new language to learn
- ❌ No type safety or IDE support
- ❌ Limited testing capabilities
- ❌ Verbose configuration
- ❌ State management complexity
- ❌ Slower AWS feature adoption
- ❌ Requires separate Terraform Cloud for team collaboration

**Cost**: $0 (open source), Terraform Cloud: $20/user/month

**Risk**: **Low** - Mature, widely adopted

### Option 3: AWS CloudFormation (YAML/JSON)

**Description**: AWS native IaC using YAML or JSON templates

**Pros**:
- ✅ Native AWS service
- ✅ No additional tools needed
- ✅ Direct AWS integration
- ✅ Change sets for preview
- ✅ Rollback support

**Cons**:
- ❌ YAML/JSON is verbose and error-prone
- ❌ No type safety
- ❌ Limited IDE support
- ❌ Difficult to test
- ❌ No reusable components
- ❌ Poor developer experience
- ❌ Hard to maintain large templates

**Cost**: $0

**Risk**: **Low** - AWS native, but poor DX

### Option 4: Pulumi

**Description**: Infrastructure as Code using general-purpose programming languages

**Pros**:
- ✅ Use TypeScript/Python/Go/C#
- ✅ Type safety and IDE support
- ✅ Multi-cloud support
- ✅ Good testing support
- ✅ Familiar programming paradigms

**Cons**:
- ❌ Requires Pulumi Cloud for state management
- ❌ Smaller community than Terraform
- ❌ Additional service dependency
- ❌ Less AWS-specific optimization
- ❌ Licensing costs for teams

**Cost**: Free tier limited, Team: $75/user/month

**Risk**: **Medium** - Smaller ecosystem, cost concerns

## Decision Outcome

**Chosen Option**: **AWS CDK (Cloud Development Kit) with TypeScript**

### Rationale

AWS CDK was selected for the following reasons:

1. **Type Safety**: TypeScript provides compile-time error checking for infrastructure code
2. **Developer Experience**: Team already knows TypeScript (Next.js frontend)
3. **IDE Support**: Full IntelliSense, refactoring, and debugging support
4. **AWS Native**: Best-in-class AWS service support and fastest feature adoption
5. **Testing**: Built-in testing framework for infrastructure validation
6. **Reusability**: Create custom constructs for common patterns
7. **Higher Abstractions**: L2/L3 constructs reduce boilerplate
8. **No Additional Costs**: Open source with no licensing fees
9. **CloudFormation Backend**: Inherits CF's reliability and rollback capabilities

**Implementation Strategy**:

**Language Choice**: TypeScript
- Team has TypeScript experience (Next.js)
- Best CDK documentation and examples
- Strong type safety
- Excellent IDE support

**Project Structure**:
```
infrastructure/
├── bin/
│   └── app.ts              # CDK app entry point
├── lib/
│   ├── stacks/
│   │   ├── network-stack.ts
│   │   ├── database-stack.ts
│   │   ├── compute-stack.ts
│   │   └── monitoring-stack.ts
│   └── constructs/
│       ├── vpc-construct.ts
│       └── rds-construct.ts
├── test/
│   └── infrastructure.test.ts
├── cdk.json
└── package.json
```

**Why Not Terraform**: While Terraform is mature and multi-cloud, our commitment to AWS and team's TypeScript skills make CDK a better fit. HCL is another language to learn with no type safety.

**Why Not CloudFormation**: YAML/JSON templates are too verbose and error-prone. CDK provides better developer experience while still using CloudFormation backend.

**Why Not Pulumi**: Requires paid service for team collaboration and state management. CDK is free and AWS-native.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Development Team | Medium | Need to learn CDK concepts | Training, examples, documentation |
| Operations Team | High | Infrastructure now in code | Training, runbooks, gradual adoption |
| Architects | Positive | Infrastructure versioned and reviewable | Architecture reviews for infrastructure |
| Security Team | Positive | Infrastructure changes auditable | Security scanning in CI/CD |
| Business | Positive | Faster, more reliable deployments | N/A |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:
- All AWS infrastructure provisioning
- Deployment processes
- Environment management
- Disaster recovery procedures
- Cost management
- Security configuration

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| CDK learning curve | High | Medium | Training, examples, pair programming |
| CloudFormation limitations | Medium | Medium | Understand CF limits, use escape hatches |
| Infrastructure drift | Medium | High | Regular drift detection, automated reconciliation |
| Breaking changes in CDK | Low | Medium | Pin CDK versions, test upgrades |
| Vendor lock-in to AWS | Low | Low | Acceptable trade-off for AWS commitment |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup and Training (Week 1-2)

- [x] Install AWS CDK CLI
  ```bash
  npm install -g aws-cdk
  cdk --version
  ```

- [x] Create CDK project structure
  ```bash
  mkdir infrastructure && cd infrastructure
  cdk init app --language typescript
  ```

- [x] Configure AWS credentials and profiles
- [x] Set up CDK context for environments
- [x] Conduct team training on CDK basics

### Phase 2: Network Infrastructure (Week 2-3)

- [ ] Create VPC stack
  ```typescript
  export class NetworkStack extends Stack {
    public readonly vpc: ec2.Vpc;
    
    constructor(scope: Construct, id: string, props?: StackProps) {
      super(scope, id, props);
      
      this.vpc = new ec2.Vpc(this, 'VPC', {
        maxAzs: 3,
        natGateways: 2,
        subnetConfiguration: [
          {
            name: 'Public',
            subnetType: ec2.SubnetType.PUBLIC,
          },
          {
            name: 'Private',
            subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
          },
          {
            name: 'Isolated',
            subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
          },
        ],
      });
    }
  }
  ```

- [ ] Configure security groups
- [ ] Set up VPC endpoints
- [ ] Deploy and test network stack

### Phase 3: Database Infrastructure (Week 3-4)

- [ ] Create RDS stack
  ```typescript
  export class DatabaseStack extends Stack {
    public readonly database: rds.DatabaseInstance;
    
    constructor(scope: Construct, id: string, props: DatabaseStackProps) {
      super(scope, id, props);
      
      this.database = new rds.DatabaseInstance(this, 'Database', {
        engine: rds.DatabaseInstanceEngine.postgres({
          version: rds.PostgresEngineVersion.VER_15,
        }),
        instanceType: ec2.InstanceType.of(
          ec2.InstanceClass.R5,
          ec2.InstanceSize.XLARGE
        ),
        vpc: props.vpc,
        vpcSubnets: {
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        },
        multiAz: true,
        allocatedStorage: 100,
        storageEncrypted: true,
        backupRetention: Duration.days(7),
        deletionProtection: true,
      });
    }
  }
  ```

- [ ] Configure read replicas
- [ ] Set up automated backups
- [ ] Deploy and test database stack

### Phase 4: Compute Infrastructure (Week 4-5)

- [ ] Create EKS cluster stack
  ```typescript
  export class ComputeStack extends Stack {
    public readonly cluster: eks.Cluster;
    
    constructor(scope: Construct, id: string, props: ComputeStackProps) {
      super(scope, id, props);
      
      this.cluster = new eks.Cluster(this, 'Cluster', {
        version: eks.KubernetesVersion.V1_28,
        vpc: props.vpc,
        defaultCapacity: 0,
      });
      
      this.cluster.addNodegroupCapacity('NodeGroup', {
        instanceTypes: [
          new ec2.InstanceType('t3.large'),
        ],
        minSize: 2,
        maxSize: 10,
        desiredSize: 3,
      });
    }
  }
  ```

- [ ] Configure auto-scaling
- [ ] Set up load balancers
- [ ] Deploy and test compute stack

### Phase 5: Messaging and Caching (Week 5-6)

- [ ] Create MSK Kafka cluster
- [ ] Create ElastiCache Redis cluster
- [ ] Configure security and networking
- [ ] Deploy and test

### Phase 6: Monitoring and Observability (Week 6-7)

- [ ] Create monitoring stack
  ```typescript
  export class MonitoringStack extends Stack {
    constructor(scope: Construct, id: string, props: MonitoringStackProps) {
      super(scope, id, props);
      
      // CloudWatch dashboards
      const dashboard = new cloudwatch.Dashboard(this, 'Dashboard', {
        dashboardName: 'ECommercePlatform',
      });
      
      // Alarms
      new cloudwatch.Alarm(this, 'HighCPU', {
        metric: props.cluster.metricCpuUtilization(),
        threshold: 80,
        evaluationPeriods: 2,
      });
    }
  }
  ```

- [ ] Set up CloudWatch dashboards
- [ ] Configure alarms and notifications
- [ ] Deploy and test

### Phase 7: CI/CD Integration (Week 7-8)

- [ ] Create CDK pipeline
  ```typescript
  export class PipelineStack extends Stack {
    constructor(scope: Construct, id: string, props?: StackProps) {
      super(scope, id, props);
      
      const pipeline = new pipelines.CodePipeline(this, 'Pipeline', {
        synth: new pipelines.ShellStep('Synth', {
          input: pipelines.CodePipelineSource.gitHub('org/repo', 'main'),
          commands: [
            'cd infrastructure',
            'npm ci',
            'npm run build',
            'npx cdk synth',
          ],
        }),
      });
      
      pipeline.addStage(new ApplicationStage(this, 'Dev'));
      pipeline.addStage(new ApplicationStage(this, 'Prod'));
    }
  }
  ```

- [ ] Configure automated deployments
- [ ] Set up approval gates
- [ ] Test deployment pipeline

### Rollback Strategy

**Trigger Conditions**:
- CDK deployment failures > 50%
- Team unable to manage infrastructure
- CloudFormation limitations blocking progress
- Infrastructure drift issues

**Rollback Steps**:
1. Export current infrastructure to Terraform
2. Migrate state management
3. Update CI/CD pipelines
4. Train team on Terraform
5. Decommission CDK infrastructure

**Rollback Time**: 2-3 weeks

**Note**: Rollback is complex due to CloudFormation backend. Prevention through testing is key.

## Monitoring and Success Criteria

### Success Metrics

- ✅ 100% of infrastructure defined in CDK
- ✅ Zero manual infrastructure changes
- ✅ Deployment success rate > 95%
- ✅ Infrastructure drift detection automated
- ✅ All environments consistent
- ✅ Deployment time < 30 minutes
- ✅ Rollback time < 15 minutes

### Monitoring Plan

**CDK Deployment Metrics**:
- Deployment success/failure rates
- Deployment duration
- Stack drift detection
- Resource creation/update/delete counts

**CloudFormation Monitoring**:
```typescript
// Add custom metrics
new cloudwatch.Metric({
  namespace: 'CDK/Deployments',
  metricName: 'DeploymentDuration',
  statistic: 'Average',
});
```

**Alerts**:
- Deployment failures
- Stack drift detected
- CloudFormation stack in ROLLBACK state
- Resource creation failures

**Review Schedule**:
- Daily: Check deployment status
- Weekly: Review infrastructure changes
- Monthly: Infrastructure cost optimization
- Quarterly: CDK version upgrades

## Consequences

### Positive Consequences

- ✅ **Type Safety**: Catch errors at compile time
- ✅ **IDE Support**: Full IntelliSense and refactoring
- ✅ **Testability**: Unit test infrastructure code
- ✅ **Reusability**: Create custom constructs
- ✅ **Version Control**: Infrastructure changes tracked in Git
- ✅ **Code Review**: Infrastructure changes reviewed like code
- ✅ **Consistency**: Same infrastructure across environments
- ✅ **Automation**: Automated deployments and rollbacks

### Negative Consequences

- ⚠️ **Learning Curve**: Team needs to learn CDK concepts
- ⚠️ **CloudFormation Limitations**: Inherits CF constraints
- ⚠️ **AWS Lock-in**: CDK is AWS-specific
- ⚠️ **Complexity**: More complex than simple YAML templates
- ⚠️ **Debugging**: CloudFormation errors can be cryptic

### Technical Debt

**Identified Debt**:
1. No infrastructure testing yet (acceptable for MVP)
2. Manual drift detection (can be automated)
3. Limited custom constructs (will grow over time)
4. No multi-region deployment yet (future requirement)

**Debt Repayment Plan**:
- **Q1 2026**: Implement comprehensive infrastructure testing
- **Q2 2026**: Automate drift detection and remediation
- **Q3 2026**: Create library of custom constructs
- **Q4 2026**: Implement multi-region deployment

## Related Decisions

- [ADR-001: Use PostgreSQL for Primary Database](001-use-postgresql-for-primary-database.md) - RDS provisioning
- [ADR-005: Use Apache Kafka for Event Streaming](005-use-kafka-for-event-streaming.md) - MSK provisioning

## Notes

### CDK Best Practices

**1. Use L2/L3 Constructs**:
```typescript
// ✅ Good: Use L2 construct
const vpc = new ec2.Vpc(this, 'VPC', {
  maxAzs: 3,
});

// ❌ Avoid: Use L1 construct (CloudFormation)
const vpc = new ec2.CfnVPC(this, 'VPC', {
  cidrBlock: '10.0.0.0/16',
});
```

**2. Create Reusable Constructs**:
```typescript
export class DatabaseConstruct extends Construct {
  public readonly database: rds.DatabaseInstance;
  
  constructor(scope: Construct, id: string, props: DatabaseConstructProps) {
    super(scope, id);
    
    this.database = new rds.DatabaseInstance(this, 'Database', {
      // Common configuration
      engine: rds.DatabaseInstanceEngine.postgres(),
      vpc: props.vpc,
      multiAz: true,
      storageEncrypted: true,
      ...props.customConfig,
    });
  }
}
```

**3. Use Environment-Specific Configuration**:
```typescript
const app = new cdk.App();

new MyStack(app, 'Dev', {
  env: { account: '111111111111', region: 'us-east-1' },
  instanceType: 't3.small',
});

new MyStack(app, 'Prod', {
  env: { account: '222222222222', region: 'us-east-1' },
  instanceType: 'r5.xlarge',
});
```

**4. Test Infrastructure Code**:
```typescript
import { Template } from 'aws-cdk-lib/assertions';

test('VPC Created', () => {
  const app = new cdk.App();
  const stack = new NetworkStack(app, 'TestStack');
  const template = Template.fromStack(stack);
  
  template.hasResourceProperties('AWS::EC2::VPC', {
    CidrBlock: '10.0.0.0/16',
  });
});
```

### Common CDK Commands

```bash
# Initialize new project
cdk init app --language typescript

# List all stacks
cdk list

# Synthesize CloudFormation template
cdk synth

# Show differences
cdk diff

# Deploy stack
cdk deploy

# Deploy all stacks
cdk deploy --all

# Destroy stack
cdk destroy

# Check for drift
cdk diff --no-version-reporting
```

### Multi-Environment Setup

```typescript
// bin/app.ts
const app = new cdk.App();

const devEnv = {
  account: process.env.CDK_DEV_ACCOUNT,
  region: 'us-east-1',
};

const prodEnv = {
  account: process.env.CDK_PROD_ACCOUNT,
  region: 'us-east-1',
};

new NetworkStack(app, 'Dev-Network', { env: devEnv });
new DatabaseStack(app, 'Dev-Database', { env: devEnv });

new NetworkStack(app, 'Prod-Network', { env: prodEnv });
new DatabaseStack(app, 'Prod-Database', { env: prodEnv });
```

### Cost Optimization

```typescript
// Use tagging for cost allocation
cdk.Tags.of(stack).add('Environment', 'Production');
cdk.Tags.of(stack).add('Project', 'ECommerce');
cdk.Tags.of(stack).add('CostCenter', 'Engineering');

// Use removal policies
database.applyRemovalPolicy(cdk.RemovalPolicy.RETAIN);
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
