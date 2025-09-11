# Multi-Stack Architecture Implementation

## Overview

This document describes the implementation of the multi-stack architecture for the GenAI Demo infrastructure, refactoring from a single monolithic stack to a modular, maintainable architecture.

## Architecture Design

### Stack Structure

The infrastructure is now organized into four separate stacks:

1. **Main Stack** (`genai-demo-development-primary`)
   - Orchestrates all other stacks
   - Manages cross-stack dependencies
   - Provides aggregated outputs and references

2. **Network Stack** (`genai-demo-development-primary/NetworkStack`)
   - VPC with public, private, and database subnets
   - Security groups for all services (ALB, EKS, RDS, MSK)
   - VPC Flow Logs and network monitoring
   - EKS subnet tagging for service discovery

3. **Certificate Stack** (`genai-demo-development-primary/CertificateStack`)
   - ACM certificates with DNS validation
   - Route 53 hosted zone lookup
   - Certificate monitoring and alerting
   - Wildcard certificate support

4. **Core Infrastructure Stack** (`genai-demo-development-primary/CoreInfrastructureStack`)
   - Application Load Balancer with SSL termination
   - DNS A records for domain routing
   - Target groups and health checks
   - Kubernetes Ingress configuration helpers

### Benefits of Multi-Stack Architecture

#### 1. **Separation of Concerns**

- Each stack has a single, well-defined responsibility
- Network infrastructure is isolated from application infrastructure
- Certificate management is separate from load balancing

#### 2. **Independent Deployment**

- Stacks can be deployed independently when changes don't affect dependencies
- Faster deployment times for isolated changes
- Reduced blast radius for infrastructure changes

#### 3. **Improved Maintainability**

- Smaller, focused codebases are easier to understand and modify
- Clear boundaries between different infrastructure components
- Easier to test individual components in isolation

#### 4. **Enhanced Reusability**

- Individual stacks can be reused across different environments
- Common patterns can be extracted and shared
- Easier to create environment-specific variations

#### 5. **Better Resource Organization**

- Resources are logically grouped by function
- Easier to track costs and resource usage per component
- Simplified troubleshooting and debugging

## Implementation Details

### File Structure

```
infrastructure/
├── lib/
│   ├── stacks/
│   │   ├── network-stack.ts           # VPC and networking resources
│   │   ├── certificate-stack.ts       # ACM certificates and DNS
│   │   ├── core-infrastructure-stack.ts # ALB and shared resources
│   │   └── index.ts                   # Stack exports
│   ├── genai-demo-infrastructure-stack.ts # Main orchestration stack
│   └── infrastructure-stack.ts        # Legacy stack (deprecated)
├── test/
│   ├── network-stack.test.ts          # Network stack tests
│   ├── certificate-stack.test.ts      # Certificate stack tests
│   ├── core-infrastructure-stack.test.ts # Core infrastructure tests
│   └── infrastructure.test.ts         # Main stack orchestration tests
└── bin/
    └── infrastructure.ts               # CDK app entry point
```

### Cross-Stack Dependencies

The stacks have the following dependency relationships:

```
Main Stack
├── Network Stack (independent)
├── Certificate Stack (independent)
└── Core Infrastructure Stack
    ├── depends on → Network Stack (VPC, Security Groups)
    └── depends on → Certificate Stack (Certificates, Hosted Zone)
```

### Stack Naming Conventions

- **Environment-based naming**: `{projectName}-{environment}-{region}`
- **Nested stack naming**: `{mainStack}/{StackType}Stack`
- **Resource naming**: `{projectName}-{environment}-{resourceType}`

### Tagging Strategy

All stacks implement consistent tagging:

```typescript
const commonTags = {
  Project: projectName,
  Environment: environment,
  ManagedBy: 'AWS-CDK',
  Component: stackType, // 'Network', 'Certificate', 'CoreInfrastructure'
  StackType: stackType
};
```

### Cross-Stack References

The main stack provides helper methods to access resources from nested stacks:

```typescript
// Access VPC from Network Stack
const vpc = mainStack.getVpc();

// Access security groups
const securityGroups = mainStack.getSecurityGroups();

// Access certificates
const certificates = mainStack.getCertificates();

// Access load balancer
const loadBalancer = mainStack.getLoadBalancer();
```

## Deployment

### CDK Commands

```bash
# List all stacks
npx cdk list

# Deploy all stacks
npx cdk deploy --all

# Deploy specific stack
npx cdk deploy genai-demo-development-primary/NetworkStack

# Synthesize templates
npx cdk synth

# View differences
npx cdk diff
```

### Stack Deployment Order

CDK automatically handles deployment order based on dependencies:

1. Network Stack (no dependencies)
2. Certificate Stack (no dependencies)
3. Core Infrastructure Stack (depends on Network and Certificate)
4. Main Stack (orchestration)

## Testing Strategy

### Test Organization

Each stack has its own test file focusing on:

- **Network Stack Tests**: VPC configuration, security groups, subnet tagging
- **Certificate Stack Tests**: ACM certificates, DNS validation, monitoring
- **Core Infrastructure Tests**: ALB configuration, DNS records, SSL termination
- **Main Stack Tests**: Cross-stack integration, dependency management

### Test Execution

```bash
# Run all tests
npm test

# Run specific stack tests
npm test -- --testPathPattern="network-stack.test.ts"
npm test -- --testPathPattern="certificate-stack.test.ts"
npm test -- --testPathPattern="core-infrastructure-stack.test.ts"
npm test -- --testPathPattern="infrastructure.test.ts"
```

## Migration from Monolithic Stack

### What Changed

1. **Single Stack → Multiple Stacks**: Refactored monolithic `infrastructure-stack.ts` into modular stacks
2. **Improved Organization**: Resources grouped by logical function rather than deployment unit
3. **Enhanced Testing**: Individual stack testing with focused test scenarios
4. **Better Dependency Management**: Explicit cross-stack dependencies with CDK dependency tracking

### Backward Compatibility

- All existing outputs are preserved in the main stack
- Resource naming conventions remain consistent
- Environment configuration continues to work as before
- Cross-stack references are handled transparently

## Future Enhancements

### Planned Improvements

1. **Environment-Specific Stacks**: Different stack configurations per environment
2. **Regional Stacks**: Multi-region deployment support
3. **Service-Specific Stacks**: EKS, RDS, and MSK as separate stacks
4. **Shared Resource Stacks**: Common resources shared across applications

### Extensibility

The modular architecture makes it easy to:

- Add new infrastructure components as separate stacks
- Implement environment-specific variations
- Create reusable infrastructure patterns
- Support multi-region deployments

## Conclusion

The multi-stack architecture provides a solid foundation for scalable, maintainable infrastructure management. The separation of concerns, improved testability, and enhanced deployment flexibility make this architecture well-suited for production environments and future growth.
