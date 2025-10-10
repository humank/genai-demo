#!/bin/bash

# Verify HPA Metrics Source (Prometheus)
# Usage: ./verify-hpa-metrics.sh <environment>

set -e

# Configuration
PROJECT_NAME="genai-demo"
NAMESPACE="$PROJECT_NAME"

# Parse arguments
ENVIRONMENT=${1:-"staging"}

echo "🔍 Verifying HPA Metrics Source"
echo "Environment: $ENVIRONMENT"
echo "Namespace: $NAMESPACE"
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Error: kubectl not configured"
    exit 1
fi

VERIFICATION_PASSED=true

echo "📊 Step 1: Check Metrics Server"
echo "================================"

if kubectl get deployment metrics-server -n kube-system > /dev/null 2>&1; then
    echo "✅ Metrics Server is installed"
    
    # Check if metrics server is ready
    ready_replicas=$(kubectl get deployment metrics-server -n kube-system -o jsonpath='{.status.readyReplicas}')
    desired_replicas=$(kubectl get deployment metrics-server -n kube-system -o jsonpath='{.spec.replicas}')
    
    if [ "$ready_replicas" = "$desired_replicas" ]; then
        echo "✅ Metrics Server is ready ($ready_replicas/$desired_replicas replicas)"
    else
        echo "⚠️  Metrics Server not fully ready ($ready_replicas/$desired_replicas replicas)"
        VERIFICATION_PASSED=false
    fi
    
    # Test metrics availability
    echo ""
    echo "Testing metrics availability..."
    if kubectl top nodes > /dev/null 2>&1; then
        echo "✅ Node metrics are available"
        kubectl top nodes
    else
        echo "❌ Node metrics are NOT available"
        VERIFICATION_PASSED=false
    fi
    
    echo ""
    if kubectl top pods -n $NAMESPACE > /dev/null 2>&1; then
        echo "✅ Pod metrics are available"
        kubectl top pods -n $NAMESPACE
    else
        echo "❌ Pod metrics are NOT available"
        VERIFICATION_PASSED=false
    fi
else
    echo "❌ Metrics Server is NOT installed"
    echo "Install with: kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml"
    VERIFICATION_PASSED=false
fi

echo ""
echo "📊 Step 2: Check Prometheus (Optional)"
echo "======================================="

# Check if Prometheus is installed
if kubectl get servicemonitor -n kube-system > /dev/null 2>&1; then
    echo "✅ ServiceMonitor CRD is available (Prometheus Operator installed)"
    
    # Check for Prometheus instance
    if kubectl get prometheus -A > /dev/null 2>&1; then
        echo "✅ Prometheus instance found"
        kubectl get prometheus -A
    else
        echo "⚠️  No Prometheus instance found"
        echo "   Prometheus is optional but recommended for advanced metrics"
    fi
    
    # Check for ServiceMonitors
    echo ""
    echo "📋 Available ServiceMonitors:"
    kubectl get servicemonitor -A | grep -E "(NAMESPACE|$PROJECT_NAME|cluster-autoscaler)" || echo "No relevant ServiceMonitors found"
else
    echo "ℹ️  Prometheus Operator not installed (optional)"
    echo "   HPA can work with Metrics Server alone"
    echo "   Prometheus provides additional custom metrics capabilities"
fi

echo ""
echo "📊 Step 3: Verify HPA Metrics Configuration"
echo "============================================"

# Get all HPAs
hpas=$(kubectl get hpa -n $NAMESPACE -o jsonpath='{.items[*].metadata.name}')

if [ -z "$hpas" ]; then
    echo "❌ No HPA resources found in namespace $NAMESPACE"
    VERIFICATION_PASSED=false
else
    echo "✅ Found HPA resources: $hpas"
    echo ""
    
    for hpa in $hpas; do
        echo "📋 Analyzing HPA: $hpa"
        echo "-----------------------------------"
        
        # Get HPA details
        kubectl get hpa $hpa -n $NAMESPACE -o yaml > /tmp/hpa-$hpa.yaml
        
        # Check metrics configuration
        echo "Configured metrics:"
        kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.spec.metrics[*].type}' | tr ' ' '\n' | sort | uniq -c
        
        # Check current metrics
        echo ""
        echo "Current metrics status:"
        
        # CPU metrics
        cpu_current=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.status.currentMetrics[?(@.type=="Resource")].resource.current.averageUtilization}' 2>/dev/null)
        cpu_target=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.spec.metrics[?(@.resource.name=="cpu")].resource.target.averageUtilization}' 2>/dev/null)
        
        if [ -n "$cpu_current" ]; then
            echo "  CPU: ${cpu_current}% / ${cpu_target}% (current/target) ✅"
        else
            echo "  CPU: No data available ❌"
            VERIFICATION_PASSED=false
        fi
        
        # Memory metrics
        mem_current=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.status.currentMetrics[?(@.resource.name=="memory")].resource.current.averageUtilization}' 2>/dev/null)
        mem_target=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.spec.metrics[?(@.resource.name=="memory")].resource.target.averageUtilization}' 2>/dev/null)
        
        if [ -n "$mem_target" ]; then
            if [ -n "$mem_current" ]; then
                echo "  Memory: ${mem_current}% / ${mem_target}% (current/target) ✅"
            else
                echo "  Memory: No data available ❌"
                VERIFICATION_PASSED=false
            fi
        fi
        
        # Check for custom metrics
        custom_metrics=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.spec.metrics[?(@.type=="Pods")].pods.metric.name}' 2>/dev/null)
        if [ -n "$custom_metrics" ]; then
            echo "  Custom metrics: $custom_metrics"
            echo "  ⚠️  Custom metrics require Prometheus or custom metrics API"
        fi
        
        # Check HPA conditions
        echo ""
        echo "HPA conditions:"
        kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{range .status.conditions[*]}{.type}{"\t"}{.status}{"\t"}{.message}{"\n"}{end}' | column -t
        
        # Check scaling behavior
        echo ""
        echo "Scaling behavior:"
        current_replicas=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.status.currentReplicas}')
        desired_replicas=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.status.desiredReplicas}')
        min_replicas=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.spec.minReplicas}')
        max_replicas=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.spec.maxReplicas}')
        
        echo "  Current: $current_replicas"
        echo "  Desired: $desired_replicas"
        echo "  Min: $min_replicas"
        echo "  Max: $max_replicas"
        
        if [ "$current_replicas" = "$desired_replicas" ]; then
            echo "  Status: Stable ✅"
        else
            echo "  Status: Scaling in progress 🔄"
        fi
        
        echo ""
    done
fi

echo ""
echo "📊 Step 4: Check Target Deployment Resource Requests"
echo "====================================================="

# Check if deployments have resource requests
deployments=$(kubectl get deployment -n $NAMESPACE -o jsonpath='{.items[*].metadata.name}')

for deployment in $deployments; do
    echo "📋 Deployment: $deployment"
    
    # Get resource requests
    cpu_request=$(kubectl get deployment $deployment -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].resources.requests.cpu}')
    mem_request=$(kubectl get deployment $deployment -n $NAMESPACE -o jsonpath='{.spec.template.spec.containers[0].resources.requests.memory}')
    
    if [ -n "$cpu_request" ] && [ -n "$mem_request" ]; then
        echo "  CPU Request: $cpu_request ✅"
        echo "  Memory Request: $mem_request ✅"
    else
        echo "  ⚠️  Missing resource requests!"
        echo "  CPU Request: ${cpu_request:-NOT SET}"
        echo "  Memory Request: ${mem_request:-NOT SET}"
        VERIFICATION_PASSED=false
    fi
    echo ""
done

echo ""
echo "📊 Step 5: Test Metrics API"
echo "============================"

echo "Testing Metrics API endpoints..."

# Test metrics.k8s.io API
if kubectl get --raw /apis/metrics.k8s.io/v1beta1/nodes > /dev/null 2>&1; then
    echo "✅ Metrics API (metrics.k8s.io) is accessible"
else
    echo "❌ Metrics API (metrics.k8s.io) is NOT accessible"
    VERIFICATION_PASSED=false
fi

# Test custom metrics API (if available)
if kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1 > /dev/null 2>&1; then
    echo "✅ Custom Metrics API is accessible"
    echo "   Available custom metrics:"
    kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1 | jq -r '.resources[].name' 2>/dev/null | head -10 || echo "   (Unable to list metrics)"
else
    echo "ℹ️  Custom Metrics API not available (optional)"
    echo "   Required only for custom Prometheus metrics"
fi

echo ""
echo "📊 Step 6: Check HPA Controller"
echo "================================"

# Check HPA controller logs
echo "Recent HPA controller activity:"
kubectl logs -n kube-system -l app=kube-controller-manager --tail=20 2>/dev/null | grep -i "horizontalpodautoscaler" || \
    echo "⚠️  Unable to access HPA controller logs (may require different log source)"

echo ""
echo "🎯 Verification Summary"
echo "======================="

if [ "$VERIFICATION_PASSED" = true ]; then
    echo "✅ All verification checks passed!"
    echo ""
    echo "📋 Metrics Source Summary:"
    echo "- Metrics Server: ✅ Running and providing metrics"
    echo "- HPA Configuration: ✅ Properly configured"
    echo "- Resource Requests: ✅ Defined on all deployments"
    echo "- Metrics API: ✅ Accessible"
    echo ""
    echo "🎉 HPA metrics source is properly configured!"
    echo ""
    echo "📚 Metrics Sources:"
    echo "1. Primary: Metrics Server (CPU/Memory)"
    echo "2. Optional: Prometheus (Custom metrics)"
    echo "3. Optional: Custom Metrics API (Advanced use cases)"
    exit 0
else
    echo "⚠️  Some verification checks failed"
    echo ""
    echo "📋 Issues Found:"
    echo "Please review the warnings above and fix the issues"
    echo ""
    echo "📚 Common fixes:"
    echo "1. Ensure Metrics Server is installed and running"
    echo "2. Wait 1-2 minutes for metrics to be available"
    echo "3. Verify resource requests are defined on all pods"
    echo "4. Check HPA configuration for errors"
    echo ""
    echo "🔍 Troubleshooting commands:"
    echo "kubectl describe hpa -n $NAMESPACE"
    echo "kubectl logs -n kube-system deployment/metrics-server"
    echo "kubectl top pods -n $NAMESPACE"
    exit 1
fi
