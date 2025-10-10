#!/bin/bash

# Deploy CloudWatch Agent for Custom Metrics
# Usage: ./deploy-custom-metrics.sh <environment> <cluster-name>

set -e

# Configuration
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
CLUSTER_NAME=${2:-"genai-demo-cluster"}

echo "🚀 Deploying CloudWatch Agent for Custom Metrics"
echo "Environment: $ENVIRONMENT"
echo "Cluster: $CLUSTER_NAME"
echo "Region: $REGION"
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Error: kubectl not configured"
    exit 1
fi

echo "📋 Step 1: Deploy CloudWatch Agent Custom Metrics Configuration"
echo "================================================================"

# Deploy custom metrics configuration
kubectl apply -f infrastructure/k8s/monitoring/cloudwatch-agent-custom-metrics-config.yaml

echo "✅ CloudWatch Agent custom metrics configuration deployed"

echo ""
echo "📋 Step 2: Verify Deployment"
echo "============================="

echo "Waiting for pods to be ready..."
sleep 10

# Check CloudWatch Agent pods
echo ""
echo "CloudWatch Agent Custom Metrics pods:"
kubectl get pods -n amazon-cloudwatch -l name=cloudwatch-agent-custom

# Check service
echo ""
echo "CloudWatch Agent StatsD Service:"
kubectl get service cloudwatch-agent-statsd -n amazon-cloudwatch

# Check if pods are running
AGENT_READY=$(kubectl get pods -n amazon-cloudwatch -l name=cloudwatch-agent-custom --field-selector=status.phase=Running --no-headers | wc -l)

echo ""
if [ "$AGENT_READY" -gt 0 ]; then
    echo "✅ CloudWatch Agent custom metrics deployment successful!"
    echo ""
    echo "📊 Custom Metrics Configuration:"
    echo "- StatsD endpoint: cloudwatch-agent-statsd.amazon-cloudwatch.svc.cluster.local:8125"
    echo "- collectd endpoint: cloudwatch-agent-statsd.amazon-cloudwatch.svc.cluster.local:25826"
    echo "- Namespace: GenAIDemo/Application"
    echo "- Export interval: 60 seconds"
    echo ""
    echo "📋 Application Configuration:"
    echo "Add to your application.yml:"
    echo ""
    echo "management:"
    echo "  metrics:"
    echo "    export:"
    echo "      statsd:"
    echo "        enabled: true"
    echo "        host: cloudwatch-agent-statsd.amazon-cloudwatch.svc.cluster.local"
    echo "        port: 8125"
    echo ""
    echo "🔍 Verify metrics:"
    echo "aws cloudwatch list-metrics --namespace GenAIDemo/Application --region ${REGION}"
    echo ""
    echo "📚 View metrics in CloudWatch:"
    echo "https://${REGION}.console.aws.amazon.com/cloudwatch/home?region=${REGION}#metricsV2:graph=~();namespace=~'GenAIDemo*2fApplication"
else
    echo "⚠️  CloudWatch Agent pods are not ready yet"
    echo "CloudWatch Agent ready: $AGENT_READY"
    echo ""
    echo "Check pod status:"
    echo "kubectl get pods -n amazon-cloudwatch"
    echo ""
    echo "Check pod logs:"
    echo "kubectl logs -n amazon-cloudwatch -l name=cloudwatch-agent-custom"
fi

echo ""
echo "🎉 Custom metrics deployment completed!"
