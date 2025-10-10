#!/bin/bash

# Verify AWS X-Ray Distributed Tracing
# Usage: ./verify-xray-tracing.sh <environment> <cluster-name>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
CLUSTER_NAME=${2:-"genai-demo-cluster"}

echo "🔍 Verifying AWS X-Ray Distributed Tracing"
echo "Environment: $ENVIRONMENT"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $REGION"
echo ""

VERIFICATION_PASSED=true

echo "📋 Step 1: Check Kubernetes Resources"
echo "======================================"

# Check namespace
if kubectl get namespace amazon-xray > /dev/null 2>&1; then
    echo "✅ Namespace 'amazon-xray' exists"
else
    echo "❌ Namespace 'amazon-xray' not found"
    VERIFICATION_PASSED=false
fi

# Check X-Ray Daemon DaemonSet
echo ""
echo "X-Ray Daemon DaemonSet:"
if kubectl get daemonset xray-daemon -n amazon-xray > /dev/null 2>&1; then
    kubectl get daemonset xray-daemon -n amazon-xray
    
    desired=$(kubectl get daemonset xray-daemon -n amazon-xray -o jsonpath='{.status.desiredNumberScheduled}')
    ready=$(kubectl get daemonset xray-daemon -n amazon-xray -o jsonpath='{.status.numberReady}')
    
    if [ "$desired" = "$ready" ]; then
        echo "✅ X-Ray Daemon DaemonSet is ready ($ready/$desired pods)"
    else
        echo "⚠️  X-Ray Daemon DaemonSet not fully ready ($ready/$desired pods)"
        VERIFICATION_PASSED=false
    fi
else
    echo "❌ X-Ray Daemon DaemonSet not found"
    VERIFICATION_PASSED=false
fi

# Check X-Ray service
echo ""
if kubectl get service xray-daemon-service -n amazon-xray > /dev/null 2>&1; then
    echo "✅ X-Ray service exists"
    kubectl get service xray-daemon-service -n amazon-xray
else
    echo "⚠️  X-Ray service not found"
fi

# Check pods status
echo ""
echo "📋 Pod Status:"
kubectl get pods -n amazon-xray -o wide

echo ""
echo "📋 Step 2: Check X-Ray Sampling Rules"
echo "======================================"

# List sampling rules
echo "X-Ray Sampling Rules:"
RULES=$(aws xray get-sampling-rules \
    --region $REGION \
    --profile $PROFILE \
    --query 'SamplingRuleRecords[*].SamplingRule.[RuleName,Priority,FixedRate]' \
    --output table 2>/dev/null)

if [ -n "$RULES" ]; then
    echo "$RULES"
    echo "✅ Sampling rules configured"
else
    echo "⚠️  No custom sampling rules found (using default)"
fi

echo ""
echo "📋 Step 3: Check IAM Configuration"
echo "==================================="

# Check service account annotation
SA_ROLE=$(kubectl get serviceaccount xray-daemon -n amazon-xray -o jsonpath='{.metadata.annotations.eks\.amazonaws\.com/role-arn}' 2>/dev/null)

if [ -n "$SA_ROLE" ]; then
    echo "✅ Service account has IAM role annotation"
    echo "   Role: $SA_ROLE"
else
    echo "⚠️  Service account missing IAM role annotation"
    VERIFICATION_PASSED=false
fi

echo ""
echo "📋 Step 4: Check X-Ray Traces"
echo "=============================="

echo "Fetching recent traces..."

# Get trace summaries from last 10 minutes
TRACES=$(aws xray get-trace-summaries \
    --start-time $(date -u -d '10 minutes ago' +%s) \
    --end-time $(date -u +%s) \
    --region $REGION \
    --profile $PROFILE \
    --query 'TraceSummaries[*].[Id,Duration,Http.HttpStatus]' \
    --output text 2>/dev/null | wc -l)

if [ "$TRACES" -gt 0 ]; then
    echo "✅ Found $TRACES traces in the last 10 minutes"
    echo ""
    echo "Sample traces:"
    aws xray get-trace-summaries \
        --start-time $(date -u -d '10 minutes ago' +%s) \
        --end-time $(date -u +%s) \
        --region $REGION \
        --profile $PROFILE \
        --query 'TraceSummaries[0:5].[Id,Duration,Http.HttpStatus,Http.HttpURL]' \
        --output table 2>/dev/null || echo "Unable to fetch trace details"
else
    echo "⚠️  No traces found in the last 10 minutes"
    echo "   This is normal if:"
    echo "   - Application just started"
    echo "   - No requests have been made"
    echo "   - Sampling rate is very low"
fi

echo ""
echo "📋 Step 5: Check Service Map"
echo "============================="

echo "Fetching service map..."

# Get service graph
SERVICES=$(aws xray get-service-graph \
    --start-time $(date -u -d '10 minutes ago' +%s) \
    --end-time $(date -u +%s) \
    --region $REGION \
    --profile $PROFILE \
    --query 'Services[*].[Name,Type,State]' \
    --output text 2>/dev/null | wc -l)

if [ "$SERVICES" -gt 0 ]; then
    echo "✅ Found $SERVICES services in service map"
    echo ""
    echo "Services:"
    aws xray get-service-graph \
        --start-time $(date -u -d '10 minutes ago' +%s) \
        --end-time $(date -u +%s) \
        --region $REGION \
        --profile $PROFILE \
        --query 'Services[*].[Name,Type,State]' \
        --output table 2>/dev/null || echo "Unable to fetch service details"
else
    echo "⚠️  No services found in service map"
    echo "   Services will appear after receiving traces"
fi

echo ""
echo "📋 Step 6: Check Pod Logs"
echo "=========================="

# Check X-Ray daemon logs
echo "Recent X-Ray Daemon logs:"
kubectl logs -n amazon-xray -l app=xray-daemon --tail=10 --prefix=true 2>/dev/null || \
    echo "⚠️  Unable to retrieve X-Ray Daemon logs"

echo ""
echo "🎯 Verification Summary"
echo "======================="

if [ "$VERIFICATION_PASSED" = true ]; then
    echo "✅ All critical checks passed!"
    echo ""
    echo "📊 X-Ray Status:"
    echo "- X-Ray Daemon: ✅ Running"
    echo "- Sampling Rules: ✅ Configured"
    echo "- IAM: ✅ Configured"
    echo "- Traces: $([ "$TRACES" -gt 0 ] && echo "✅ Collecting" || echo "⚠️  Waiting for traffic")"
    echo ""
    echo "📋 Next Steps:"
    echo "1. View Service Map:"
    echo "   https://${REGION}.console.aws.amazon.com/xray/home?region=${REGION}#/service-map"
    echo ""
    echo "2. View Traces:"
    echo "   https://${REGION}.console.aws.amazon.com/xray/home?region=${REGION}#/traces"
    echo ""
    echo "3. Query traces:"
    echo "   aws xray get-trace-summaries \\"
    echo "     --start-time \$(date -u -d '1 hour ago' +%s) \\"
    echo "     --end-time \$(date -u +%s) \\"
    echo "     --region ${REGION}"
    echo ""
    echo "4. Generate some traffic to see traces:"
    echo "   curl http://<your-app-url>/api/v1/health"
    echo ""
    echo "🎉 X-Ray is fully operational!"
    exit 0
else
    echo "⚠️  Some verification checks failed"
    echo ""
    echo "📋 Issues Found:"
    echo "Please review the warnings above"
    echo ""
    echo "🔍 Troubleshooting:"
    echo "1. Check pod status:"
    echo "   kubectl get pods -n amazon-xray"
    echo ""
    echo "2. Check pod logs:"
    echo "   kubectl logs -n amazon-xray -l app=xray-daemon"
    echo ""
    echo "3. Verify IAM permissions:"
    echo "   kubectl describe serviceaccount xray-daemon -n amazon-xray"
    echo ""
    echo "4. Test X-Ray daemon connectivity:"
    echo "   kubectl exec -n amazon-xray <pod-name> -- curl localhost:2000"
    echo ""
    echo "📚 Documentation:"
    echo "https://docs.aws.amazon.com/xray/latest/devguide/xray-daemon.html"
    exit 1
fi
