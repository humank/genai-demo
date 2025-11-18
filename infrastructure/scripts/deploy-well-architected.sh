#!/bin/bash

# AWS Well-Architected Tool Deployment Script
# Purpose: Deploy Well-Architected Tool stack for automated architecture assessment
# Usage: ./deploy-well-architected.sh [environment] [workload-name] [alert-email] [review-owner]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="${1:-staging}"
WORKLOAD_NAME="${2:-GenAI-Demo-${ENVIRONMENT}}"
ALERT_EMAIL="${3:-architecture-team@example.com}"
REVIEW_OWNER="${4:-lead-architect@example.com}"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}AWS Well-Architected Tool Deployment${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Environment: ${ENVIRONMENT}"
echo "Workload Name: ${WORKLOAD_NAME}"
echo "Alert Email: ${ALERT_EMAIL}"
echo "Review Owner: ${REVIEW_OWNER}"
echo ""

# Validate AWS credentials
echo -e "${YELLOW}Validating AWS credentials...${NC}"
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    echo -e "${RED}Error: AWS credentials not configured${NC}"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region || echo "ap-northeast-1")

echo -e "${GREEN}✓ AWS Account: ${ACCOUNT_ID}${NC}"
echo -e "${GREEN}✓ Region: ${REGION}${NC}"
echo ""

# Deploy the stack
echo -e "${YELLOW}Deploying Well-Architected Tool stack...${NC}"

cd "$(dirname "$0")/.."

npx cdk deploy WellArchitectedStack \
  --context environment="${ENVIRONMENT}" \
  --context workloadName="${WORKLOAD_NAME}" \
  --context alertEmail="${ALERT_EMAIL}" \
  --context reviewOwner="${REVIEW_OWNER}" \
  --require-approval never

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Well-Architected Tool stack deployed successfully${NC}"
else
    echo -e "${RED}✗ Deployment failed${NC}"
    exit 1
fi

# Get stack outputs
echo ""
echo -e "${YELLOW}Retrieving stack outputs...${NC}"

WORKLOAD_ID=$(aws cloudformation describe-stacks \
  --stack-name WellArchitectedStack \
  --query 'Stacks[0].Outputs[?OutputKey==`WorkloadId`].OutputValue' \
  --output text)

REPORT_BUCKET=$(aws cloudformation describe-stacks \
  --stack-name WellArchitectedStack \
  --query 'Stacks[0].Outputs[?OutputKey==`ReportBucket`].OutputValue' \
  --output text)

LAMBDA_ARN=$(aws cloudformation describe-stacks \
  --stack-name WellArchitectedStack \
  --query 'Stacks[0].Outputs[?OutputKey==`AssessmentLambdaArn`].OutputValue' \
  --output text)

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Workload ID: ${WORKLOAD_ID}"
echo "Report Bucket: ${REPORT_BUCKET}"
echo "Assessment Lambda: ${LAMBDA_ARN}"
echo ""

# Trigger initial assessment
echo -e "${YELLOW}Triggering initial assessment...${NC}"

aws lambda invoke \
  --function-name "${LAMBDA_ARN}" \
  --payload '{"action": "assess"}' \
  --cli-binary-format raw-in-base64-out \
  response.json

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Initial assessment triggered${NC}"
    echo ""
    echo "Assessment results:"
    cat response.json | jq '.'
    rm response.json
else
    echo -e "${RED}✗ Failed to trigger assessment${NC}"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Next Steps${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "1. Access Well-Architected Tool console:"
echo "   https://console.aws.amazon.com/wellarchitected/home?region=${REGION}#/workloads/${WORKLOAD_ID}"
echo ""
echo "2. Complete workload questions for comprehensive assessment"
echo ""
echo "3. Review assessment reports in S3:"
echo "   aws s3 ls s3://${REPORT_BUCKET}/assessments/${ENVIRONMENT}/"
echo ""
echo "4. Monitor weekly assessments (every Monday at 9 AM)"
echo ""
echo "5. Track monthly milestones (1st of each month)"
echo ""
echo -e "${GREEN}Deployment completed successfully!${NC}"
