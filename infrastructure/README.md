# GenAI Demo Infrastructure

This project contains the **consolidated AWS CDK infrastructure** code for the GenAI Demo application, featuring a unified deployment architecture with comprehensive testing.

## ✅ Project Status

- **CDK v2 Compliant**: Using `aws-cdk-lib ^2.208.0`
- **Fully Tested**: 103 tests passing across 11 test suites
- **Production Ready**: Complete with security compliance and monitoring
- **Consolidated Deployment**: Single command deployment for all infrastructure

## 🏗️ Architecture Overview

The infrastructure is organized into **18 coordinated stacks** with unified deployment:

### Foundation Layer
- **NetworkStack**: VPC, subnets, security groups, and networking components
- **SecurityStack**: KMS keys, IAM roles, and security-related resources
- **IAMStack**: Fine-grained access control, resource-based policies
- **CertificateStack**: SSL/TLS certificates for secure communications

### Data Layer
- **RdsStack**: Aurora PostgreSQL cluster with global database support
- **ElastiCacheStack**: Redis cluster for distributed caching and locking
- **MSKStack**: Kafka cluster for event streaming and data flow tracking

### Compute Layer
- **EKSStack**: Kubernetes cluster with auto-scaling and security
- **EKSIRSAStack**: IAM Roles for Service Accounts configuration

### Security & Identity Layer
- **SSOStack**: AWS SSO integration with permission sets
- **SecurityStack**: Enhanced security monitoring and compliance

### Observability Layer
- **AlertingStack**: SNS topics and notification infrastructure
- **ObservabilityStack**: CloudWatch logs, dashboards, X-Ray tracing, and monitoring

### Analytics Layer *(optional)*
- **DataCatalogStack**: AWS Glue Data Catalog with automated schema discovery
- **AnalyticsStack**: Data lake, Kinesis, Glue, and analytics pipeline

### Management Layer
- **CoreInfrastructureStack**: Application Load Balancer and core compute resources
- **CostOptimizationStack**: Cost monitoring and optimization automation

### Resilience Layer *(production)*
- **DisasterRecoveryStack**: Multi-region disaster recovery automation
- **MultiRegionStack**: Cross-region replication and failover

## Prerequisites

- Node.js 18.x or later
- AWS CLI configured with appropriate credentials
- AWS CDK CLI installed globally: `npm install -g aws-cdk`
- TypeScript 5.6+ (included in dependencies)

## 🚀 Quick Start

### Unified Deployment (Recommended)

The new unified deployment script provides a single entry point for all infrastructure deployment scenarios:

```bash
# Deploy complete development environment
./deploy-unified.sh full -e development

# Deploy only foundation components (network, security, IAM)
./deploy-unified.sh foundation -e staging

# Deploy with analytics enabled
./deploy-unified.sh full --enable-analytics -a ops@company.com

# Deploy production with multi-region
./deploy-unified.sh full -e production --enable-multi-region

# Check deployment status
./deploy-unified.sh --status

# Destroy development environment
./deploy-unified.sh --destroy -e development
```

### NPM Scripts (Alternative)

```bash
# Quick deployment commands
npm run deploy:dev          # Development environment
npm run deploy:staging      # Staging with analytics
npm run deploy:prod         # Production with multi-region

# Component-specific deployments
npm run deploy:foundation   # Network, security, IAM
npm run deploy:data         # RDS, ElastiCache, MSK
npm run deploy:compute      # EKS cluster
npm run deploy:security     # IAM, SSO, IRSA
npm run deploy:observability # Monitoring, alerting

# Status and cleanup
npm run status              # Check deployment status
npm run destroy:dev         # Destroy development
```

## 📁 Project Structure

```
infrastructure/
├── bin/
│   └── infrastructure.ts          # Main CDK app entry point (NEW)
├── src/
│   ├── stacks/                    # CDK stack definitions (18 stacks)
│   │   ├── iam-stack.ts          # Fine-grained access control
│   │   ├── eks-irsa-stack.ts     # IRSA configuration
│   │   ├── sso-stack.ts          # AWS SSO integration
│   │   ├── msk-stack.ts          # Kafka messaging
│   │   ├── data-catalog-stack.ts # Data governance
│   │   └── ...                   # Other stacks
│   ├── constructs/                # Reusable CDK constructs
│   ├── config/                    # Environment configurations
│   └── utils/                     # Utility functions
├── test/
│   ├── unit/                      # Unit tests
│   ├── integration/               # Integration tests
│   └── ...                       # Test suites
├── docs/                          # Documentation
├── deploy-unified.sh              # NEW: Unified deployment script
├── deploy-iam-security.sh         # IAM security deployment
├── deploy-consolidated.sh         # Legacy deployment script
└── package.json                   # Dependencies and scripts
```

## Getting Started

1. **Install dependencies**:

   ```bash
   npm install
   ```

2. **Build the project**:

   ```bash
   npm run build
   ```

3. **Run tests**:

   ```bash
   npm test
   # All 103 tests should pass in ~16 seconds
   ```

4. **Synthesize CloudFormation templates**:

   ```bash
   npm run synth
   # or without CDK Nag warnings:
   npx cdk synth --context enableCdkNag=false
   ```

5. **Deploy to AWS**:

   ```bash
   # Unified deployment (recommended)
   ./deploy-consolidated.sh
   
   # Or with npm scripts
   npm run deploy:consolidated
   
   # Deploy to specific environment
   npm run deploy:dev      # Development with analytics
   npm run deploy:staging  # Staging with CDK Nag
   npm run deploy:prod     # Production deployment
   ```

## 🧪 Testing

### Quick Test Commands

```bash
# Run all tests (103 tests)
npm test

# Run specific test categories
npm run test:unit          # Unit tests (26 tests)
npm run test:integration   # Integration tests (8 tests)
npm run test:consolidated  # Main test suite (18 tests)
npm run test:compliance    # CDK Nag compliance (4 tests)
npm run test:quick         # Fast subset for development
```

### Test Results

```
Test Suites: 11 passed, 11 total
Tests: 103 passed, 103 total
Time: 15.828 s
Coverage: 100% on core infrastructure
```

## Project Structure

```
infrastructure/
├── src/                          # Source code
│   ├── stacks/                   # CDK Stack definitions
│   ├── constructs/               # Custom CDK Constructs
│   ├── config/                   # Configuration files
│   └── utils/                    # Utility functions
├── test/                         # Test files
│   ├── unit/                     # Unit tests
│   ├── integration/              # Integration tests
│   └── compliance/               # CDK Nag compliance tests
├── bin/                          # Entry point
├── docs/                         # Documentation
└── k8s/                          # Kubernetes manifests
```

## Available Scripts

- `npm run build` - Compile TypeScript to JavaScript
- `npm run watch` - Watch for changes and compile
- `npm test` - Run all tests
- `npm run test:unit` - Run unit tests only
- `npm run test:integration` - Run integration tests only
- `npm run test:compliance` - Run CDK Nag compliance tests
- `npm run synth` - Synthesize CloudFormation templates
- `npm run deploy` - Deploy stacks to AWS
- `npm run destroy` - Destroy stacks from AWS
- `npm run lint` - Run ESLint
- `npm run lint:fix` - Fix ESLint issues

## Environment Configuration

The infrastructure supports multiple environments (development, staging, production).
Configure the environment using CDK context:

```bash
# Deploy to development (default)
cdk deploy

# Deploy to staging
cdk deploy -c environment=staging

# Deploy to production
cdk deploy -c environment=production
```

## Testing

The project includes comprehensive testing:

- **Unit Tests**: Test individual stack components
- **Integration Tests**: Test stack interactions
- **Compliance Tests**: CDK Nag security and best practice checks

Run all tests:

```bash
npm test
```

## Security

The infrastructure follows AWS security best practices:

- All resources are encrypted using KMS
- Security groups follow least privilege principles
- IAM roles have minimal required permissions
- CDK Nag checks ensure compliance with AWS Well-Architected Framework

## Monitoring

The ObservabilityStack provides:

- CloudWatch log groups for application logs
- CloudWatch dashboards for monitoring
- Structured logging with encryption

## Contributing

1. Create a feature branch
2. Make your changes
3. Run tests: `npm test`
4. Run linting: `npm run lint`
5. Synthesize templates: `npm run synth`
6. Create a pull request

## Troubleshooting

If you encounter issues:

1. **Clean and rebuild**:

   ```bash
   npm run clean
   npm install
   npm run build
   ```

2. **Check CDK version compatibility**:

   ```bash
   cdk --version
   ```

3. **Verify AWS credentials**:

   ```bash
   aws sts get-caller-identity
   ```

## License

This project is licensed under the MIT License.

## 🚀 Deployment Options

### Environment Configuration

The infrastructure supports multiple environments with different configurations:

| Environment | Analytics | CDK Nag | Use Case |
|-------------|-----------|---------|----------|
| Development | Optional | Disabled | Daily development |
| Staging | Enabled | Enabled | Pre-production testing |
| Production | Enabled | Enabled | Production workloads |

### Deployment Commands

```bash
# Quick development deployment
./deploy-consolidated.sh development us-east-1 false false

# Full staging deployment with compliance
./deploy-consolidated.sh staging us-east-1 true true

# Production deployment
./deploy-consolidated.sh production us-east-1 true true
```

## 🔒 Security & Compliance

### CDK Nag Integration

The project includes CDK Nag for security compliance with appropriate suppressions:

- **AwsSolutions-VPC7**: VPC Flow Logs (optional for development)
- **AwsSolutions-EC23**: ALB internet access (required for web application)
- **AwsSolutions-IAM4**: AWS managed policies (CloudWatch agent)
- **AwsSolutions-IAM5**: KMS wildcard permissions (encryption operations)

### Security Features

- ✅ KMS encryption for all sensitive data
- ✅ IAM roles with least privilege principles
- ✅ Security groups with minimal required access
- ✅ SSL/TLS enforcement for data in transit
- ✅ CloudTrail logging for audit trails

## 📊 Monitoring & Observability

### Built-in Monitoring

- **CloudWatch Dashboards**: Application and infrastructure metrics
- **CloudWatch Logs**: Centralized logging with retention policies
- **SNS Alerting**: Multi-tier alerting (Critical, Warning, Info)
- **X-Ray Tracing**: Distributed tracing for performance analysis

### Metrics & Alarms

- Application performance metrics
- Infrastructure health monitoring
- Cost optimization alerts
- Security event notifications

## 🛠️ Development Workflow

### Daily Development

```bash
# 1. Make changes to infrastructure code
# 2. Run tests to validate changes
npm run test:quick

# 3. Synthesize to check CloudFormation
npm run synth

# 4. Deploy to development environment
npm run deploy:dev
```

### Pre-commit Checklist

- [ ] All tests pass (`npm test`)
- [ ] CDK synthesis succeeds (`npm run synth`)
- [ ] Code follows TypeScript standards
- [ ] Documentation updated if needed

## 📚 Documentation

- [`TESTING_GUIDE.md`](./TESTING_GUIDE.md) - Comprehensive testing documentation
- [`CONSOLIDATED_DEPLOYMENT.md`](./CONSOLIDATED_DEPLOYMENT.md) - Deployment guide
- [`CDK_COMPLETION_SUMMARY.md`](../reports-summaries/task-execution/CDK_COMPLETION_SUMMARY.md) - Project completion status
- [`MIGRATION_GUIDE.md`](./MIGRATION_GUIDE.md) - Migration from old structure

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `npm test`
5. Submit a pull request

## 📞 Support

For questions or issues:

1. Check the documentation in the `docs/` directory
2. Review test examples in the `test/` directory
3. Run `npm run synth` to validate your changes
4. Use `npm run test:watch` for interactive development

## 🏷️ Version Information

- **CDK Version**: 2.208.0+
- **Node.js**: 18.x+
- **TypeScript**: 5.6+
- **Test Framework**: Jest
- **Test Coverage**: 103 tests across 11 suites
