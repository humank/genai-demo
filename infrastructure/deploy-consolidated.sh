#!/bin/bash

# Consolidated CDK Deployment Script
# This script demonstrates how to deploy the consolidated infrastructure

set -e

echo "🚀 GenAI Demo - Consolidated CDK Deployment"
echo "=========================================="

# Configuration
ENVIRONMENT=${1:-development}
REGION=${2:-us-east-1}
ENABLE_ANALYTICS=${3:-true}
ENABLE_CDK_NAG=${4:-true}

echo "📋 Configuration:"
echo "   Environment: $ENVIRONMENT"
echo "   Region: $REGION"
echo "   Analytics: $ENABLE_ANALYTICS"
echo "   CDK Nag: $ENABLE_CDK_NAG"
echo ""

# Build the project
echo "🔨 Building CDK project..."
npm run build

# Validate the project
echo "✅ Validating CDK project..."
npm run test:unit

# Deploy with context parameters
echo "🚀 Deploying consolidated infrastructure..."

cdk deploy --all \
  --context environment=$ENVIRONMENT \
  --context region=$REGION \
  --context enableAnalytics=$ENABLE_ANALYTICS \
  --context enableCdkNag=$ENABLE_CDK_NAG \
  --context alertEmail="admin@example.com" \
  --require-approval never

echo ""
echo "✅ Deployment completed successfully!"
echo ""
echo "📊 Deployed Stacks:"
echo "   • NetworkStack - VPC, subnets, security groups"
echo "   • SecurityStack - KMS keys, IAM roles"
echo "   • AlertingStack - SNS topics for monitoring"
echo "   • CoreInfrastructureStack - Load balancer, compute resources"
echo "   • ObservabilityStack - CloudWatch, monitoring"
if [ "$ENABLE_ANALYTICS" = "true" ]; then
echo "   • AnalyticsStack - S3 data lake, Kinesis, Glue, QuickSight"
fi
echo ""
echo "🔗 Next Steps:"
echo "   1. Configure your application to use the deployed infrastructure"
echo "   2. Set up monitoring alerts and dashboards"
echo "   3. Deploy your application code"
echo ""
echo "📚 Documentation:"
echo "   • Main deployment: bin/infrastructure.ts"
echo "   • Multi-region: bin/multi-region-deployment.ts"
echo "   • Analytics only: bin/analytics.ts (deprecated - use main deployment)"