#!/bin/bash

# Verify CloudWatch Custom Metrics
# Usage: ./verify-custom-metrics.sh <environment>

set -e

REGION="ap-northeast-1"
PROFILE="default"
ENVIRONMENT=${1:-"staging"}

echo "🔍 Verifying CloudWatch Custom Metrics"
echo "Environment: $ENVIRONMENT"
echo ""

VERIFICATION_PASSED=true

echo "📋 Step 1: Check Kubernetes Resources"
echo "======================================"

# Check CloudWatch Agent Custom
if kubectl get daemonset cloudwatch-agent-custom -n amazon-cloudwatch > /dev/null 2>&1; then
    echo "✅ CloudWatch Agent Custom DaemonSet exists"
    kubectl get daemonset cloudwatch-agent-custom -n amazon-cloudwatch
else
    echo "⚠️  CloudWatch Agent Custom DaemonSet not found"
    VERIFICATION_PASSED=false
fi

# Check StatsD Service
echo ""
if kubectl get service cloudwatch-agent-statsd -n amazon-cloudwatch > /dev/null 2>&1; then
    echo "✅ StatsD Service exists"
    kubectl get service cloudwatch-agent-statsd -n amazon-cloudwatch
else
    echo "⚠️  StatsD Service not found"
    VERIFICATION_PASSED=false
fi

echo ""
echo "📋 Step 2: Check Custom Metrics in CloudWatch"
echo "=============================================="

echo "Checking for custom metrics..."
METRICS=$(aws cloudwatch list-metrics \
    --namespace "GenAIDemo/Application" \
    --region $REGION \
    --profile $PROFILE \
    --query 'Metrics[*].MetricName' \
    --output text 2>/dev/null | wc -w)

if [ "$METRICS" -gt 0 ]; then
    echo "✅ Found $METRICS custom metrics"
    echo ""
    echo "Sample metrics:"
    aws cloudwatch list-metrics \
        --namespace "GenAIDemo/Application" \
        --region $REGION \
        --profile $PROFILE \
        --query 'Metrics[0:10].[MetricName,Dimensions[0].Value]' \
        --output table
else
    echo "⚠️  No custom metrics found yet (may take 1-2 minutes after first export)"
fi

echo ""
echo "🎯 Verification Summary"
echo "======================="

if [ "$VERIFICATION_PASSED" = true ]; then
    echo "✅ All checks passed!"
    echo ""
    echo "📊 Custom Metrics Status:"
    echo "- CloudWatch Agent: ✅ Running"
    echo "- StatsD Service: ✅ Available"
    echo "- Metrics: $([ "$METRICS" -gt 0 ] && echo "✅ Publishing" || echo "⚠️  Waiting for data")"
    echo ""
    echo "📋 View metrics:"
    echo "https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#metricsV2:graph=~();namespace=~'GenAIDemo*2fApplication"
    exit 0
else
    echo "⚠️  Some checks failed"
    echo "Please review the warnings above"
    exit 1
fi
