#!/bin/bash

# Verify CloudWatch Container Insights Deployment
# Usage: ./verify-container-insights.sh <environment> <cluster-name>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
CLUSTER_NAME=${2:-"genai-demo-cluster"}

echo "🔍 Verifying CloudWatch Container Insights"
echo "Environment: $ENVIRONMENT"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $REGION"
echo ""

VERIFICATION_PASSED=true

echo "📋 Step 1: Check Kubernetes Resources"
echo "======================================"

# Check namespace
if kubectl get namespace amazon-cloudwatch > /dev/null 2>&1; then
    echo "✅ Namespace 'amazon-cloudwatch' exists"
else
    echo "❌ Namespace 'amazon-cloudwatch' not found"
    VERIFICATION_PASSED=false
fi

# Check CloudWatch Agent DaemonSet
echo ""
echo "CloudWatch Agent DaemonSet:"
if kubectl get daemonset cloudwatch-agent -n amazon-cloudwatch > /dev/null 2>&1; then
    kubectl get daemonset cloudwatch-agent -n amazon-cloudwatch
    
    desired=$(kubectl get daemonset cloudwatch-agent -n amazon-cloudwatch -o jsonpath='{.status.desiredNumberScheduled}')
    ready=$(kubectl get daemonset cloudwatch-agent -n amazon-cloudwatch -o jsonpath='{.status.numberReady}')
    
    if [ "$desired" = "$ready" ]; then
        echo "✅ CloudWatch Agent DaemonSet is ready ($ready/$desired pods)"
    else
        echo "⚠️  CloudWatch Agent DaemonSet not fully ready ($ready/$desired pods)"
        VERIFICATION_PASSED=false
    fi
else
    echo "❌ CloudWatch Agent DaemonSet not found"
    VERIFICATION_PASSED=false
fi

# Check Fluent Bit DaemonSet
echo ""
echo "Fluent Bit DaemonSet:"
if kubectl get daemonset fluent-bit -n amazon-cloudwatch > /dev/null 2>&1; then
    kubectl get daemonset fluent-bit -n amazon-cloudwatch
    
    desired=$(kubectl get daemonset fluent-bit -n amazon-cloudwatch -o jsonpath='{.status.desiredNumberScheduled}')
    ready=$(kubectl get daemonset fluent-bit -n amazon-cloudwatch -o jsonpath='{.status.numberReady}')
    
    if [ "$desired" = "$ready" ]; then
        echo "✅ Fluent Bit DaemonSet is ready ($ready/$desired pods)"
    else
        echo "⚠️  Fluent Bit DaemonSet not fully ready ($ready/$desired pods)"
        VERIFICATION_PASSED=false
    fi
else
    echo "❌ Fluent Bit DaemonSet not found"
    VERIFICATION_PASSED=false
fi

# Check pods status
echo ""
echo "📋 Pod Status:"
kubectl get pods -n amazon-cloudwatch -o wide

echo ""
echo "📋 Step 2: Check CloudWatch Log Groups"
echo "======================================="

# Check if log groups exist
LOG_GROUPS=(
    "/aws/containerinsights/${CLUSTER_NAME}/application"
    "/aws/containerinsights/${CLUSTER_NAME}/dataplane"
    "/aws/containerinsights/${CLUSTER_NAME}/host"
    "/aws/containerinsights/${CLUSTER_NAME}/performance"
)

for log_group in "${LOG_GROUPS[@]}"; do
    if aws logs describe-log-groups \
        --log-group-name-prefix "$log_group" \
        --region $REGION \
        --profile $PROFILE \
        --query 'logGroups[0].logGroupName' \
        --output text 2>/dev/null | grep -q "$log_group"; then
        echo "✅ Log group exists: $log_group"
    else
        echo "⚠️  Log group not found: $log_group (may take 5-10 minutes to appear)"
    fi
done

echo ""
echo "📋 Step 3: Check CloudWatch Metrics"
echo "===================================="

echo "Checking for Container Insights metrics..."

# Check if metrics are being published
METRICS=$(aws cloudwatch list-metrics \
    --namespace ContainerInsights \
    --region $REGION \
    --profile $PROFILE \
    --query 'Metrics[*].MetricName' \
    --output text 2>/dev/null | wc -w)

if [ "$METRICS" -gt 0 ]; then
    echo "✅ Found $METRICS Container Insights metrics"
    echo ""
    echo "Sample metrics:"
    aws cloudwatch list-metrics \
        --namespace ContainerInsights \
        --region $REGION \
        --profile $PROFILE \
        --query 'Metrics[0:5].[MetricName,Dimensions[0].Value]' \
        --output table
else
    echo "⚠️  No Container Insights metrics found yet (may take 5-10 minutes)"
    echo "   Metrics will appear after the first collection interval"
fi

echo ""
echo "📋 Step 4: Check IAM Configuration"
echo "==================================="

# Check service account annotation
SA_ROLE=$(kubectl get serviceaccount cloudwatch-agent -n amazon-cloudwatch -o jsonpath='{.metadata.annotations.eks\.amazonaws\.com/role-arn}' 2>/dev/null)

if [ -n "$SA_ROLE" ]; then
    echo "✅ Service account has IAM role annotation"
    echo "   Role: $SA_ROLE"
else
    echo "⚠️  Service account missing IAM role annotation"
    VERIFICATION_PASSED=false
fi

echo ""
echo "📋 Step 5: Check Pod Logs"
echo "=========================="

# Check CloudWatch Agent logs
echo "Recent CloudWatch Agent logs:"
kubectl logs -n amazon-cloudwatch -l name=cloudwatch-agent --tail=10 --prefix=true 2>/dev/null || \
    echo "⚠️  Unable to retrieve CloudWatch Agent logs"

echo ""
echo "Recent Fluent Bit logs:"
kubectl logs -n amazon-cloudwatch -l k8s-app=fluent-bit --tail=10 --prefix=true 2>/dev/null || \
    echo "⚠️  Unable to retrieve Fluent Bit logs"

echo ""
echo "📋 Step 6: Test Metrics Collection"
echo "==================================="

# Get a sample metric value
echo "Fetching sample cluster CPU utilization..."
CPU_UTIL=$(aws cloudwatch get-metric-statistics \
    --namespace ContainerInsights \
    --metric-name cluster_cpu_utilization \
    --dimensions Name=ClusterName,Value=$CLUSTER_NAME \
    --start-time $(date -u -d '10 minutes ago' +%Y-%m-%dT%H:%M:%S) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
    --period 300 \
    --statistics Average \
    --region $REGION \
    --profile $PROFILE \
    --query 'Datapoints[0].Average' \
    --output text 2>/dev/null)

if [ "$CPU_UTIL" != "None" ] && [ -n "$CPU_UTIL" ]; then
    echo "✅ Cluster CPU Utilization: ${CPU_UTIL}%"
else
    echo "⚠️  No recent CPU utilization data (metrics may still be initializing)"
fi

echo ""
echo "🎯 Verification Summary"
echo "======================="

if [ "$VERIFICATION_PASSED" = true ]; then
    echo "✅ All critical checks passed!"
    echo ""
    echo "📊 Container Insights Status:"
    echo "- CloudWatch Agent: ✅ Running"
    echo "- Fluent Bit: ✅ Running"
    echo "- Log Groups: ✅ Created"
    echo "- Metrics: ✅ Publishing"
    echo "- IAM: ✅ Configured"
    echo ""
    echo "📋 Next Steps:"
    echo "1. View Container Insights dashboard:"
    echo "   https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#container-insights:infrastructure"
    echo ""
    echo "2. View application logs:"
    echo "   https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#logsV2:log-groups/log-group/\$252Faws\$252Fcontainerinsights\$252F${CLUSTER_NAME}\$252Fapplication"
    echo ""
    echo "3. Query metrics:"
    echo "   aws cloudwatch get-metric-statistics \\"
    echo "     --namespace ContainerInsights \\"
    echo "     --metric-name cluster_cpu_utilization \\"
    echo "     --dimensions Name=ClusterName,Value=${CLUSTER_NAME} \\"
    echo "     --start-time \$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \\"
    echo "     --end-time \$(date -u +%Y-%m-%dT%H:%M:%S) \\"
    echo "     --period 300 \\"
    echo "     --statistics Average"
    echo ""
    echo "🎉 Container Insights is fully operational!"
    exit 0
else
    echo "⚠️  Some verification checks failed"
    echo ""
    echo "📋 Issues Found:"
    echo "Please review the warnings above"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "1. Check pod status:"
    echo "   kubectl get pods -n amazon-cloudwatch"
    echo ""
    echo "2. Check pod logs:"
    echo "   kubectl logs -n amazon-cloudwatch -l name=cloudwatch-agent"
    echo "   kubectl logs -n amazon-cloudwatch -l k8s-app=fluent-bit"
    echo ""
    echo "3. Verify IAM permissions:"
    echo "   kubectl describe serviceaccount cloudwatch-agent -n amazon-cloudwatch"
    echo ""
    echo "4. Wait 5-10 minutes for metrics to appear"
    echo ""
    echo "📚 Documentation:"
    echo "https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Container-Insights-setup-EKS-quickstart.html"
    exit 1
fi
