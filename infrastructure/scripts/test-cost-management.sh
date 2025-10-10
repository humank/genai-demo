#!/bin/bash

# Test Cost Management Stack Configuration
# Purpose: Validate AWS Cost Management integrated monitoring setup

set -e

echo "========================================="
echo "Cost Management Stack Validation"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test 1: Check AWS Budgets
echo "Test 1: Checking AWS Budgets..."
BUDGETS=$(aws budgets describe-budgets --account-id $(aws sts get-caller-identity --query Account --output text) 2>/dev/null || echo "")

if [ -n "$BUDGETS" ]; then
    echo -e "${GREEN}✓ AWS Budgets configured${NC}"
    echo "$BUDGETS" | jq '.Budgets[] | {BudgetName, BudgetLimit, TimeUnit}'
else
    echo -e "${YELLOW}⚠ No budgets found (may not be deployed yet)${NC}"
fi
echo ""

# Test 2: Check Cost Anomaly Detection
echo "Test 2: Checking Cost Anomaly Detection..."
ANOMALY_MONITORS=$(aws ce get-anomaly-monitors 2>/dev/null || echo "")

if [ -n "$ANOMALY_MONITORS" ]; then
    echo -e "${GREEN}✓ Cost Anomaly Detection configured${NC}"
    echo "$ANOMALY_MONITORS" | jq '.AnomalyMonitors[] | {MonitorName, MonitorType}'
else
    echo -e "${YELLOW}⚠ No anomaly monitors found${NC}"
fi
echo ""

# Test 3: Check CloudWatch Dashboard
echo "Test 3: Checking Cost Explorer Dashboard..."
DASHBOARD=$(aws cloudwatch get-dashboard --dashboard-name "GenAIDemo-Cost-Explorer-Trends" 2>/dev/null || echo "")

if [ -n "$DASHBOARD" ]; then
    echo -e "${GREEN}✓ Cost Explorer Dashboard exists${NC}"
else
    echo -e "${YELLOW}⚠ Dashboard not found (may not be deployed yet)${NC}"
fi
echo ""


# Test 4: Check Trusted Advisor Lambda Function
echo "Test 4: Checking Trusted Advisor Automation..."
LAMBDA_FUNCTION=$(aws lambda get-function --function-name genai-demo-trusted-advisor-automation 2>/dev/null || echo "")

if [ -n "$LAMBDA_FUNCTION" ]; then
    echo -e "${GREEN}✓ Trusted Advisor Lambda Function exists${NC}"
    echo "$LAMBDA_FUNCTION" | jq '.Configuration | {FunctionName, Runtime, Timeout}'
else
    echo -e "${YELLOW}⚠ Lambda function not found (may not be deployed yet)${NC}"
fi
echo ""

# Test 5: Check EventBridge Rule for Trusted Advisor
echo "Test 5: Checking Trusted Advisor Schedule..."
RULE=$(aws events describe-rule --name genai-demo-trusted-advisor-weekly 2>/dev/null || echo "")

if [ -n "$RULE" ]; then
    echo -e "${GREEN}✓ Trusted Advisor weekly schedule configured${NC}"
    echo "$RULE" | jq '{Name, ScheduleExpression, State}'
else
    echo -e "${YELLOW}⚠ EventBridge rule not found${NC}"
fi
echo ""

# Test 6: Check SNS Topic for Cost Alerts
echo "Test 6: Checking Cost Alert SNS Topic..."
TOPICS=$(aws sns list-topics 2>/dev/null | jq -r '.Topics[].TopicArn' | grep "genai-demo-cost-alerts" || echo "")

if [ -n "$TOPICS" ]; then
    echo -e "${GREEN}✓ Cost Alert SNS Topic exists${NC}"
    echo "$TOPICS"
else
    echo -e "${YELLOW}⚠ SNS topic not found${NC}"
fi
echo ""

echo "========================================="
echo "Validation Complete"
echo "========================================="
