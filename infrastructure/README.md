# GenAI Demo Infrastructure

This CDK project defines the AWS infrastructure for the GenAI Demo application with comprehensive observability capabilities.

## Architecture Overview

The infrastructure includes:

- **VPC**: Multi-AZ VPC with public, private, and database subnets
- **EKS**: Kubernetes cluster for container orchestration (to be implemented)
- **RDS**: PostgreSQL database with Multi-AZ deployment (to be implemented)
- **MSK**: Managed Streaming for Apache Kafka (to be implemented)
- **Observability**: CloudWatch, X-Ray, OpenSearch Service (to be implemented)
- **Analytics**: Kinesis, S3, QuickSight (to be implemented)

## Project Structure

```
infrastructure/
├── bin/
│   └── infrastructure.ts          # CDK app entry point
├── lib/
│   └── infrastructure-stack.ts    # Main infrastructure stack
├── test/                          # CDK tests
├── cdk.json                       # CDK configuration
├── cdk.context.json              # Environment-specific context
└── deploy.config.ts              # Deployment configurations
```

## Environment Configuration

The project supports multiple environments:

- **development**: Single AZ, minimal resources
- **staging**: Production-like setup for testing
- **production**: Multi-AZ, high availability
- **production-dr**: Disaster recovery region

## Deployment Commands

### Prerequisites

```bash
npm install
npm run build
```

### Development Environment

```bash
# Deploy to development environment
npx cdk deploy genai-demo-development-primary --context genai-demo:environment=development

# Synthesize CloudFormation template
npx cdk synth genai-demo-development-primary --context genai-demo:environment=development
```

### Production Environment

```bash
# Deploy to production environment
npx cdk deploy genai-demo-production-primary --context genai-demo:environment=production

# Deploy with specific account/region
CDK_DEFAULT_ACCOUNT=123456789012 CDK_DEFAULT_REGION=ap-east-2 npx cdk deploy genai-demo-production-primary --context genai-demo:environment=production
```

### Multi-Region Deployment (Future)

```bash
# Primary region (Taiwan)
npx cdk deploy genai-demo-production-primary --context genai-demo:environment=production

# Secondary region (Tokyo) - for disaster recovery
npx cdk deploy genai-demo-production-secondary --context genai-demo:environment=production-dr
```

## Useful Commands

- `npm run build`   - Compile TypeScript to JavaScript
- `npm run watch`   - Watch for changes and compile
- `npm run test`    - Perform Jest unit tests
- `npx cdk deploy`  - Deploy this stack to your default AWS account/region
- `npx cdk diff`    - Compare deployed stack with current state
- `npx cdk synth`   - Emits the synthesized CloudFormation template
- `npx cdk destroy` - Destroy the deployed stack

## Context Configuration

The project uses CDK context for environment-specific configuration:

```json
{
  "genai-demo:environment": "development|staging|production",
  "genai-demo:project-name": "genai-demo",
  "genai-demo:primary-region": "ap-east-2",
  "genai-demo:secondary-region": "ap-northeast-1",
  "genai-demo:domain": "kimkao.io"
}
```

## Implementation Status

- [x] **Task 4**: CDK project structure and basic VPC
- [ ] **Task 5**: Core infrastructure components (ALB, Security Groups)
- [ ] **Task 6**: EKS cluster infrastructure
- [ ] **Task 7**: RDS PostgreSQL database
- [ ] **Task 8**: Amazon MSK cluster
- [ ] **Task 9-16**: Observability and analytics components
- [ ] **Task 17**: Multi-region disaster recovery

## Security Considerations

- All resources are deployed in private subnets where possible
- Security groups follow least privilege principles
- Secrets are managed through AWS Secrets Manager
- IAM roles use minimal required permissions
- VPC Flow Logs enabled for network monitoring

## Cost Optimization

- Development environment uses minimal resources
- Production environment uses right-sized instances
- Auto-scaling configured for variable workloads
- Log retention policies to manage storage costs
