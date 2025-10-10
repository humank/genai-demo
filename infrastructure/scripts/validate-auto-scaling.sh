#!/bin/bash

# Validate Auto-Scaling Configuration
# Usage: ./validate-auto-scaling.sh <environment>

set -e

# Configuration
PROJECT_NAME="genai-demo"
REGION="ap-northeast-1"
PROFILE="default"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
NAMESPACE="$PROJECT_NAME"

echo "🔍 Validating Auto-Scaling Configuration"
echo "Environment: $ENVIRONMENT"
echo "Namespace: $NAMESPACE"
echo "Region: $REGION"
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Error: kubectl not configured or cluster not accessible"
    echo "Please configure kubectl to access the EKS cluster"
    exit 1
fi

# Function to check component status
check_component() {
    local component=$1
    local namespace=$2
    local resource_type=$3
    local resource_name=$4
    
    echo "📋 Checking $component..."
    
    if kubectl get $resource_type $resource_name -n $namespace > /dev/null 2>&1; then
        echo "✅ $component is installed"
        return 0
    else
        echo "❌ $component is NOT installed"
        return 1
    fi
}

# Function to display resource status
display_status() {
    local resource_type=$1
    local namespace=$2
    local label=$3
    
    echo ""
    echo "📊 $label Status:"
    kubectl get $resource_type -n $namespace -o wide
    echo ""
}

# Validation Results
VALIDATION_PASSED=true

echo "🔍 Step 1: Verify Metrics Server"
echo "=================================="
if check_component "Metrics Server" "kube-system" "deployment" "metrics-server"; then
    display_status "deployment" "kube-system" "Metrics Server"
    
    # Test metrics collection
    echo "Testing metrics collection..."
    if kubectl top nodes > /dev/null 2>&1; then
        echo "✅ Node metrics collection working"
        kubectl top nodes
    else
        echo "⚠️  Warning: Node metrics not available yet"
        VALIDATION_PASSED=false
    fi
    
    echo ""
    if kubectl top pods -n $NAMESPACE > /dev/null 2>&1; then
        echo "✅ Pod metrics collection working"
        kubectl top pods -n $NAMESPACE
    else
        echo "⚠️  Warning: Pod metrics not available yet"
        VALIDATION_PASSED=false
    fi
else
    echo "❌ Metrics Server not found"
    echo "Install with: kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml"
    VALIDATION_PASSED=false
fi

echo ""
echo "🔍 Step 2: Verify HPA Configuration"
echo "===================================="
if kubectl get hpa -n $NAMESPACE > /dev/null 2>&1; then
    echo "✅ HPA resources found"
    display_status "hpa" "$NAMESPACE" "HPA"
    
    # Check HPA details
    echo "📋 HPA Details:"
    for hpa in $(kubectl get hpa -n $NAMESPACE -o jsonpath='{.items[*].metadata.name}'); do
        echo ""
        echo "HPA: $hpa"
        kubectl describe hpa $hpa -n $NAMESPACE | grep -A 10 "Metrics:"
        
        # Check if HPA can read metrics
        current_cpu=$(kubectl get hpa $hpa -n $NAMESPACE -o jsonpath='{.status.currentMetrics[?(@.type=="Resource")].resource.current.averageUtilization}')
        if [ -n "$current_cpu" ]; then
            echo "✅ HPA can read CPU metrics: ${current_cpu}%"
        else
            echo "⚠️  Warning: HPA cannot read CPU metrics"
            VALIDATION_PASSED=false
        fi
    done
else
    echo "❌ No HPA resources found in namespace $NAMESPACE"
    VALIDATION_PASSED=false
fi

echo ""
echo "🔍 Step 3: Verify Cluster Autoscaler"
echo "====================================="
if check_component "Cluster Autoscaler" "kube-system" "deployment" "cluster-autoscaler"; then
    display_status "deployment" "kube-system" "Cluster Autoscaler"
    
    # Check Cluster Autoscaler logs
    echo "📋 Recent Cluster Autoscaler logs:"
    kubectl logs -n kube-system deployment/cluster-autoscaler --tail=20 | grep -E "(scale|node)" || true
    
    # Check for scaling events
    echo ""
    echo "📋 Recent scaling events:"
    kubectl get events -n kube-system --sort-by='.lastTimestamp' | grep cluster-autoscaler | tail -10 || echo "No recent events"
else
    echo "❌ Cluster Autoscaler not found"
    VALIDATION_PASSED=false
fi

echo ""
echo "🔍 Step 4: Check Node Group Configuration"
echo "=========================================="
echo "📋 Current nodes:"
kubectl get nodes -o wide

echo ""
echo "📋 Node resource allocation:"
kubectl describe nodes | grep -A 5 "Allocated resources:" | head -20

echo ""
echo "🔍 Step 5: Check Pod Resource Requests"
echo "======================================="
echo "📋 Pod resource requests in $NAMESPACE:"
kubectl get pods -n $NAMESPACE -o custom-columns=\
NAME:.metadata.name,\
CPU_REQUEST:.spec.containers[*].resources.requests.cpu,\
MEMORY_REQUEST:.spec.containers[*].resources.requests.memory,\
CPU_LIMIT:.spec.containers[*].resources.limits.cpu,\
MEMORY_LIMIT:.spec.containers[*].resources.limits.memory

# Check if pods have resource requests defined
echo ""
echo "📋 Checking resource requests..."
pods_without_requests=$(kubectl get pods -n $NAMESPACE -o json | \
    jq -r '.items[] | select(.spec.containers[].resources.requests == null) | .metadata.name' | wc -l)

if [ "$pods_without_requests" -eq 0 ]; then
    echo "✅ All pods have resource requests defined"
else
    echo "⚠️  Warning: $pods_without_requests pods without resource requests"
    VALIDATION_PASSED=false
fi

echo ""
echo "🔍 Step 6: Check Monitoring Configuration"
echo "=========================================="

# Check if Prometheus is available
if kubectl get servicemonitor -n kube-system cluster-autoscaler > /dev/null 2>&1; then
    echo "✅ Cluster Autoscaler ServiceMonitor found"
else
    echo "⚠️  Warning: Cluster Autoscaler ServiceMonitor not found"
fi

# Check for HPA metrics
echo ""
echo "📋 HPA Metrics (if Prometheus available):"
echo "Query: kube_horizontalpodautoscaler_status_current_replicas"
echo "Query: kube_horizontalpodautoscaler_status_desired_replicas"

echo ""
echo "🔍 Step 7: Performance Analysis"
echo "================================"

# Calculate current cluster utilization
total_cpu_capacity=$(kubectl top nodes --no-headers | awk '{sum+=$2} END {print sum}')
total_cpu_usage=$(kubectl top nodes --no-headers | awk '{sum+=$3} END {print sum}')

if [ -n "$total_cpu_capacity" ] && [ "$total_cpu_capacity" != "0" ]; then
    cpu_utilization=$(echo "scale=2; $total_cpu_usage * 100 / $total_cpu_capacity" | bc)
    echo "📊 Cluster CPU Utilization: ${cpu_utilization}%"
    
    if (( $(echo "$cpu_utilization > 80" | bc -l) )); then
        echo "⚠️  Warning: High CPU utilization - consider scaling"
    elif (( $(echo "$cpu_utilization < 30" | bc -l) )); then
        echo "💡 Info: Low CPU utilization - consider scaling down"
    else
        echo "✅ CPU utilization is healthy"
    fi
fi

echo ""
echo "📋 Pod distribution across nodes:"
kubectl get pods -n $NAMESPACE -o wide | awk '{print $7}' | sort | uniq -c

echo ""
echo "🎯 Validation Summary"
echo "====================="

if [ "$VALIDATION_PASSED" = true ]; then
    echo "✅ All validation checks passed!"
    echo ""
    echo "📋 Configuration Summary:"
    echo "- Metrics Server: ✅ Running"
    echo "- HPA: ✅ Configured and working"
    echo "- Cluster Autoscaler: ✅ Running"
    echo "- Resource Requests: ✅ Defined"
    echo "- Monitoring: ✅ Configured"
    echo ""
    echo "🎉 Auto-scaling is properly configured!"
    exit 0
else
    echo "⚠️  Some validation checks failed"
    echo ""
    echo "📋 Issues Found:"
    echo "Please review the warnings above and fix the issues"
    echo ""
    echo "📚 Common fixes:"
    echo "1. Install Metrics Server:"
    echo "   kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml"
    echo ""
    echo "2. Wait for metrics to be available (may take 1-2 minutes)"
    echo ""
    echo "3. Ensure pods have resource requests defined"
    echo ""
    echo "4. Check Cluster Autoscaler logs for errors:"
    echo "   kubectl logs -n kube-system deployment/cluster-autoscaler"
    exit 1
fi
