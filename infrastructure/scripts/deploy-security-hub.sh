#!/bin/bash

# Deploy Security Hub Comprehensive Security Insights
# Requirements: 13.25, 13.26, 13.27

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Security Hub Deployment Script${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: AWS CLI is not installed${NC}"
    exit 1
fi

if ! command -v cdk &> /dev/null; then
    echo -e "${RED}Error: AWS CDK is not installed${NC}"
    echo "Install with: npm install -g aws-cdk"
    exit 1
fi

# Get AWS account and region
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region)

if [ -z "$AWS_REGION" ]; then
    AWS_REGION="ap-northeast-1"
    echo -e "${YELLOW}No region configured, using default: ${AWS_REGION}${NC}"
fi

echo -e "${GREEN}✓ AWS Account: ${AWS_ACCOUNT}${NC}"
echo -e "${GREEN}✓ AWS Region: ${AWS_REGION}${NC}"
echo ""

# Prompt for notification email
read -p "Enter security notification email address: " NOTIFICATION_EMAIL

if [ -z "$NOTIFICATION_EMAIL" ]; then
    echo -e "${RED}Error: Notification email is required${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Notification Email: ${NOTIFICATION_EMAIL}${NC}"
echo ""

# Deploy Security Hub stack
echo -e "${YELLOW}Deploying Security Hub stack...${NC}"

cd "$PROJECT_ROOT/infrastructure"

cdk deploy SecurityHubStack \
  --context notificationEmail="$NOTIFICATION_EMAIL" \
  --context enableAutomatedResponse=true \
  --require-approval never

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Security Hub stack deployed successfully${NC}"
else
    echo -e "${RED}✗ Security Hub stack deployment failed${NC}"
    exit 1
fi

echo ""

# Enable Security Hub integrations
echo -e "${YELLOW}Enabling Security Hub integrations...${NC}"

# Enable GuardDuty (if not already enabled)
echo "Checking GuardDuty status..."
GUARDDUTY_DETECTOR=$(aws guardduty list-detectors --region "$AWS_REGION" --query 'DetectorIds[0]' --output text)

if [ "$GUARDDUTY_DETECTOR" == "None" ] || [ -z "$GUARDDUTY_DETECTOR" ]; then
    echo "Enabling GuardDuty..."
    GUARDDUTY_DETECTOR=$(aws guardduty create-detector \
        --enable \
        --finding-publishing-frequency FIFTEEN_MINUTES \
        --region "$AWS_REGION" \
        --query 'DetectorId' \
        --output text)
    echo -e "${GREEN}✓ GuardDuty enabled: ${GUARDDUTY_DETECTOR}${NC}"
else
    echo -e "${GREEN}✓ GuardDuty already enabled: ${GUARDDUTY_DETECTOR}${NC}"
fi

# Enable Inspector (if not already enabled)
echo "Checking Inspector status..."
INSPECTOR_STATUS=$(aws inspector2 batch-get-account-status --region "$AWS_REGION" 2>/dev/null || echo "not-enabled")

if [[ "$INSPECTOR_STATUS" == "not-enabled" ]]; then
    echo "Enabling Inspector..."
    aws inspector2 enable \
        --resource-types EC2 ECR LAMBDA \
        --region "$AWS_REGION" 2>/dev/null || echo "Inspector enable initiated"
    echo -e "${GREEN}✓ Inspector enabled${NC}"
else
    echo -e "${GREEN}✓ Inspector already enabled${NC}"
fi

# Enable Macie (if not already enabled)
echo "Checking Macie status..."
MACIE_STATUS=$(aws macie2 get-macie-session --region "$AWS_REGION" 2>/dev/null || echo "not-enabled")

if [[ "$MACIE_STATUS" == "not-enabled" ]]; then
    echo "Enabling Macie..."
    aws macie2 enable-macie \
        --finding-publishing-frequency FIFTEEN_MINUTES \
        --status ENABLED \
        --region "$AWS_REGION" 2>/dev/null || echo "Macie enable initiated"
    echo -e "${GREEN}✓ Macie enabled${NC}"
else
    echo -e "${GREEN}✓ Macie already enabled${NC}"
fi

echo ""

# Verify Security Hub configuration
echo -e "${YELLOW}Verifying Security Hub configuration...${NC}"

# Check enabled standards
ENABLED_STANDARDS=$(aws securityhub get-enabled-standards --region "$AWS_REGION" --query 'StandardsSubscriptions[*].StandardsArn' --output text)

echo "Enabled Security Standards:"
echo "$ENABLED_STANDARDS" | tr '\t' '\n' | while read -r standard; do
    if [ -n "$standard" ]; then
        echo -e "${GREEN}  ✓ ${standard}${NC}"
    fi
done

echo ""

# Check EventBridge rules
echo "Checking EventBridge rules..."
CRITICAL_RULE=$(aws events list-rules --region "$AWS_REGION" --name-prefix "SecurityHubStack-CriticalFindingsRule" --query 'Rules[0].Name' --output text)
HIGH_RULE=$(aws events list-rules --region "$AWS_REGION" --name-prefix "SecurityHubStack-HighFindingsRule" --query 'Rules[0].Name' --output text)

if [ "$CRITICAL_RULE" != "None" ] && [ -n "$CRITICAL_RULE" ]; then
    echo -e "${GREEN}✓ Critical findings rule configured${NC}"
else
    echo -e "${YELLOW}⚠ Critical findings rule not found${NC}"
fi

if [ "$HIGH_RULE" != "None" ] && [ -n "$HIGH_RULE" ]; then
    echo -e "${GREEN}✓ High findings rule configured${NC}"
else
    echo -e "${YELLOW}⚠ High findings rule not found${NC}"
fi

echo ""

# Display next steps
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Next Steps:"
echo "1. Confirm SNS email subscriptions (check your inbox)"
echo "2. Review Security Hub dashboard: https://console.aws.amazon.com/securityhub/"
echo "3. Configure finding suppression rules if needed"
echo "4. Review and test automated remediation actions"
echo "5. Set up additional integrations (AWS Config, IAM Access Analyzer)"
echo ""
echo "Documentation: docs/security-hub-comprehensive-insights.md"
echo ""
echo -e "${GREEN}Security Hub is now monitoring your AWS environment!${NC}"
