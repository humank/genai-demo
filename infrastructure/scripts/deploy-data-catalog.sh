#!/bin/bash

# Deploy AWS Glue Data Catalog Stack for GenAI Demo
# This script deploys the DataCatalogStack with automated schema discovery

set -e

# Configuration
ENVIRONMENT=${1:-development}
REGION=${2:-ap-east-2}
ENABLE_CDK_NAG=${3:-false}

echo "🚀 Deploying AWS Glue Data Catalog Stack"
echo "   Environment: $ENVIRONMENT"
echo "   Region: $REGION"
echo "   CDK Nag: $ENABLE_CDK_NAG"

# Check if required stacks exist
echo "📋 Checking prerequisites..."

# Check if NetworkStack exists
if ! aws cloudformation describe-stacks --stack-name "${ENVIRONMENT}-NetworkStack" --region "$REGION" >/dev/null 2>&1; then
    echo "❌ NetworkStack not found. Please deploy NetworkStack first."
    exit 1
fi

# Check if SecurityStack exists
if ! aws cloudformation describe-stacks --stack-name "${ENVIRONMENT}-SecurityStack" --region "$REGION" >/dev/null 2>&1; then
    echo "❌ SecurityStack not found. Please deploy SecurityStack first."
    exit 1
fi

# Check if RdsStack exists
if ! aws cloudformation describe-stacks --stack-name "${ENVIRONMENT}-RdsStack" --region "$REGION" >/dev/null 2>&1; then
    echo "❌ RdsStack not found. Please deploy RdsStack first."
    exit 1
fi

# Check if AlertingStack exists
if ! aws cloudformation describe-stacks --stack-name "${ENVIRONMENT}-AlertingStack" --region "$REGION" >/dev/null 2>&1; then
    echo "❌ AlertingStack not found. Please deploy AlertingStack first."
    exit 1
fi

echo "✅ All prerequisite stacks found"

# Build the project
echo "🔨 Building CDK project..."
npm run build

# Deploy DataCatalogStack
echo "🚀 Deploying DataCatalogStack..."
npx cdk deploy "${ENVIRONMENT}-DataCatalogStack" \
    --context environment="$ENVIRONMENT" \
    --context region="$REGION" \
    --context enableCdkNag="$ENABLE_CDK_NAG" \
    --require-approval never \
    --region "$REGION"

if [ $? -eq 0 ]; then
    echo "✅ DataCatalogStack deployed successfully!"
    
    # Get stack outputs
    echo "📋 Stack Outputs:"
    aws cloudformation describe-stacks \
        --stack-name "${ENVIRONMENT}-DataCatalogStack" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[*].[OutputKey,OutputValue]' \
        --output table
    
    echo ""
    echo "🎯 Next Steps:"
    echo "1. Verify Glue Crawler in AWS Console: https://${REGION}.console.aws.amazon.com/glue/home?region=${REGION}#catalog:tab=crawlers"
    echo "2. Check CloudWatch Dashboard: https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#dashboards:"
    echo "3. Monitor crawler execution logs in CloudWatch Logs"
    echo "4. The crawler will run daily at 2 AM or can be triggered manually"
    
else
    echo "❌ DataCatalogStack deployment failed!"
    exit 1
fi