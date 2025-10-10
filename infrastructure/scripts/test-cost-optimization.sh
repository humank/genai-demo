#!/bin/bash

# Test Cost Optimization Stack Configuration
# Purpose: Validate EKS autoscaling and Aurora cost optimization setup

set -e

echo "========================================="
echo "Cost Optimization Stack Validation"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Get EKS cluster name
CLUSTER_NAME=$(aws eks list-clusters --query 'clusters[0]' --output text 2>/dev/null || echo "")

if [ -z "$CLUSTER_NAME" ]; then
    echo -e "${YELLOW}⚠ No EKS cluster found${NC}"
    exit 1
fi

echo "Using EKS Cluster: $CLUSTER_NAME"
echo ""

# Test 1: Check Cluster Autoscaler Deployment
echo "Test 1: Checking Cluster Autoscaler..."
CA_DEPLOYMENT=$(kubectl get deployment cluster-autoscaler -n kube-system 2>/dev/null || echo "")

if [ -n "$CA_DEPLOYMENT" ]; then
    echo -e "${GREEN}✓ Cluster Autoscaler deployed${NC}"
    kubectl get deployment cluster-autoscaler -n kube-system
else
    echo -e "${YELLOW}⚠ Cluster Autoscaler not found (may not be deployed yet)${NC}"
fi
echo ""

# Test 2: Check VPA Components
echo "Test 2: Checking Vertical Pod Autoscaler..."
VPA_RECOMMENDER=$(kubectl get deployment vpa-recommender -n kube-system 2>/dev/null || echo "")

if [ -n "$VPA_RECOMMENDER" ]; then
    echo -e "${GREEN}✓ VPA Recommender deployed${NC}"
    kubectl get deployment vpa-recommender -n kube-system
else
    echo -e "${YELLOW}⚠ VPA not found (may not be deployed yet)${NC}"
fi
echo ""


# Test 3: Check VPA CRDs
echo "Test 3: Checking VPA Custom Resource Definitions..."
VPA_CRD=$(kubectl get crd verticalpodautoscalers.autoscaling.k8s.io 2>/dev/null || echo "")

if [ -n "$VPA_CRD" ]; then
    echo -e "${GREEN}✓ VPA CRD installed${NC}"
else
    echo -e "${YELLOW}⚠ VPA CRD not found${NC}"
fi
echo ""

# Test 4: Check Application VPA Configuration
echo "Test 4: Checking Application VPA Configuration..."
APP_VPA=$(kubectl get vpa genai-demo-app-vpa -n default 2>/dev/null || echo "")

if [ -n "$APP_VPA" ]; then
    echo -e "${GREEN}✓ Application VPA configured${NC}"
    kubectl get vpa genai-demo-app-vpa -n default
else
    echo -e "${YELLOW}⚠ Application VPA not found${NC}"
fi
echo ""

# Test 5: Check Aurora Cost Optimizer Lambda
echo "Test 5: Checking Aurora Cost Optimizer Lambda..."
AURORA_LAMBDA=$(aws lambda get-function --function-name genai-demo-aurora-cost-optimizer 2>/dev/null || echo "")

if [ -n "$AURORA_LAMBDA" ]; then
    echo -e "${GREEN}✓ Aurora Cost Optimizer Lambda exists${NC}"
    echo "$AURORA_LAMBDA" | jq '.Configuration | {FunctionName, Runtime, Timeout}'
else
    echo -e "${YELLOW}⚠ Lambda function not found (may not be deployed yet)${NC}"
fi
echo ""

# Test 6: Check EventBridge Rule for Aurora Optimization
echo "Test 6: Checking Aurora Optimization Schedule..."
AURORA_RULE=$(aws events describe-rule --name genai-demo-aurora-cost-optimization-weekly 2>/dev/null || echo "")

if [ -n "$AURORA_RULE" ]; then
    echo -e "${GREEN}✓ Aurora optimization weekly schedule configured${NC}"
    echo "$AURORA_RULE" | jq '{Name, ScheduleExpression, State}'
else
    echo -e "${YELLOW}⚠ EventBridge rule not found${NC}"
fi
echo ""

# Test 7: Check Cost Optimization Dashboard
echo "Test 7: Checking Cost Optimization Dashboard..."
DASHBOARD=$(aws cloudwatch get-dashboard --dashboard-name "GenAIDemo-Cost-Optimization-Metrics" 2>/dev/null || echo "")

if [ -n "$DASHBOARD" ]; then
    echo -e "${GREEN}✓ Cost Optimization Dashboard exists${NC}"
else
    echo -e "${YELLOW}⚠ Dashboard not found (may not be deployed yet)${NC}"
fi
echo ""

# Test 8: Check Node Autoscaling Activity
echo "Test 8: Checking Recent Autoscaling Activity..."
ASG_ACTIVITIES=$(aws autoscaling describe-scaling-activities --max-records 5 2>/dev/null || echo "")

if [ -n "$ASG_ACTIVITIES" ]; then
    echo -e "${GREEN}✓ Autoscaling activity found${NC}"
    echo "$ASG_ACTIVITIES" | jq '.Activities[] | {ActivityId, Description, StartTime, StatusCode}'
else
    echo -e "${YELLOW}⚠ No recent autoscaling activity${NC}"
fi
echo ""

echo "========================================="
echo "Validation Complete"
echo "========================================="
