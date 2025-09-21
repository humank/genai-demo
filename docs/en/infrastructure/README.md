# GenAI Demo Infrastructure

This project contains the **consolidated AWS CDK infrastructure** code for the GenAI Demo application, featuring a unified deployment architecture with comprehensive testing.

## ✅ Project Status

- **CDK v2 Compliant**: Using `aws-cdk-lib ^2.208.0`
- **Fully Tested**: 103 tests passing across 11 test suites
- **Production Ready**: Complete with security compliance and monitoring
- **Consolidated Deployment**: Single command deployment for all infrastructure

## 🏗️ Architecture Overview

The infrastructure is organized into **6 coordinated stacks**:

- **NetworkStack**: VPC, subnets, security groups, and networking components
- **SecurityStack**: KMS keys, IAM roles, and security-related resources  
- **AlertingStack**: SNS topics and notification infrastructure
- **CoreInfrastructureStack**: Application Load Balancer and core compute resources
- **ObservabilityStack**: CloudWatch logs, dashboards, and monitoring
- **AnalyticsStack** *(optional)*: Data lake, Kinesis, Glue, and analytics pipeline

## Prerequisites

- Node.js 18.x or later
- AWS CLI configured with appropriate credentials
- AWS CDK CLI installed globally: `npm install -g aws-cdk`

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
   ```

4. **Synthesize CloudFormation templates**:

   ```bash
   npm run synth
   ```

5. **Deploy to AWS**:

   ```bash
   npm run deploy
   ```

## Project Structure

```
../../infrastructure/
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
