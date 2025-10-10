#!/bin/bash

# Test Auto-Scaling Behavior Under Load
# Usage: ./test-auto-scaling.sh <environment> <test-type>
# Test types: scale-up, scale-down, full

set -e

# Configuration
PROJECT_NAME="genai-demo"
NAMESPACE="$PROJECT_NAME"
DEPLOYMENT_NAME="$PROJECT_NAME-backend"

# Parse arguments
ENVIRONMENT=${1:-"staging"}
TEST_TYPE=${2:-"full"}

echo "🧪 Testing Auto-Scaling Behavior"
echo "Environment: $ENVIRONMENT"
echo "Namespace: $NAMESPACE"
echo "Test Type: $TEST_TYPE"
echo ""

# Check if kubectl is configured
if ! kubectl cluster-info > /dev/null 2>&1; then
    echo "❌ Error: kubectl not configured"
    exit 1
fi

# Function to wait for condition
wait_for_condition() {
    local description=$1
    local command=$2
    local timeout=${3:-300}
    local interval=10
    local elapsed=0
    
    echo "⏳ Waiting for: $description"
    
    while [ $elapsed -lt $timeout ]; do
        if eval "$command" > /dev/null 2>&1; then
            echo "✅ Condition met: $description"
            return 0
        fi
        
        echo "   Still waiting... (${elapsed}s/${timeout}s)"
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    
    echo "❌ Timeout waiting for: $description"
    return 1
}

# Function to get current replica count
get_replica_count() {
    kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE -o jsonpath='{.status.replicas}' 2>/dev/null || echo "0"
}

# Function to get HPA status
get_hpa_status() {
    kubectl get hpa -n $NAMESPACE -o custom-columns=\
NAME:.metadata.name,\
CURRENT:.status.currentReplicas,\
DESIRED:.status.desiredReplicas,\
MIN:.spec.minReplicas,\
MAX:.spec.maxReplicas,\
CPU:.status.currentMetrics[0].resource.current.averageUtilization,\
TARGET_CPU:.spec.metrics[0].resource.target.averageUtilization
}

# Function to monitor scaling
monitor_scaling() {
    local duration=$1
    local interval=10
    local elapsed=0
    
    echo ""
    echo "📊 Monitoring scaling for ${duration}s..."
    echo "Time | Replicas | CPU% | Memory% | Pods Ready"
    echo "-----+----------+------+---------+-----------"
    
    while [ $elapsed -lt $duration ]; do
        replicas=$(get_replica_count)
        
        # Get CPU and memory usage
        cpu_usage=$(kubectl top pods -n $NAMESPACE --no-headers 2>/dev/null | \
            awk '{sum+=$2} END {print sum}' | sed 's/m//')
        mem_usage=$(kubectl top pods -n $NAMESPACE --no-headers 2>/dev/null | \
            awk '{sum+=$3} END {print sum}' | sed 's/Mi//')
        
        # Get ready pods
        ready_pods=$(kubectl get pods -n $NAMESPACE --no-headers 2>/dev/null | \
            grep -c "Running" || echo "0")
        
        printf "%4ds | %8s | %4s | %7s | %9s\n" \
            $elapsed "$replicas" "${cpu_usage:-N/A}" "${mem_usage:-N/A}" "$ready_pods"
        
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    
    echo ""
}

# Test Scale Up
test_scale_up() {
    echo "🚀 Test 1: Scale Up Behavior"
    echo "=============================="
    
    initial_replicas=$(get_replica_count)
    echo "📊 Initial replicas: $initial_replicas"
    
    echo ""
    echo "📊 Initial HPA status:"
    get_hpa_status
    
    echo ""
    echo "🔥 Starting load generator..."
    
    # Create load generator pod
    kubectl run load-generator-$$ \
        --image=busybox \
        --restart=Never \
        --namespace=$NAMESPACE \
        --command -- /bin/sh -c \
        "while true; do wget -q -O- http://$DEPLOYMENT_NAME:8080/actuator/health; done" \
        > /dev/null 2>&1 &
    
    LOAD_GEN_PID=$!
    
    echo "✅ Load generator started (PID: $LOAD_GEN_PID)"
    echo ""
    
    # Monitor scaling for 5 minutes
    monitor_scaling 300
    
    # Check if scaling occurred
    final_replicas=$(get_replica_count)
    echo "📊 Final replicas: $final_replicas"
    
    echo ""
    echo "📊 Final HPA status:"
    get_hpa_status
    
    # Cleanup load generator
    echo ""
    echo "🧹 Cleaning up load generator..."
    kubectl delete pod load-generator-$$ -n $NAMESPACE --force --grace-period=0 > /dev/null 2>&1 || true
    
    echo ""
    if [ "$final_replicas" -gt "$initial_replicas" ]; then
        echo "✅ Scale up test PASSED"
        echo "   Scaled from $initial_replicas to $final_replicas replicas"
        return 0
    else
        echo "⚠️  Scale up test INCONCLUSIVE"
        echo "   Replicas remained at $initial_replicas"
        echo "   This may be normal if load was insufficient or HPA already at max"
        return 1
    fi
}

# Test Scale Down
test_scale_down() {
    echo ""
    echo "📉 Test 2: Scale Down Behavior"
    echo "==============================="
    
    initial_replicas=$(get_replica_count)
    echo "📊 Initial replicas: $initial_replicas"
    
    if [ "$initial_replicas" -le 2 ]; then
        echo "⚠️  Already at minimum replicas, skipping scale down test"
        return 0
    fi
    
    echo ""
    echo "📊 Initial HPA status:"
    get_hpa_status
    
    echo ""
    echo "⏸️  Waiting for load to decrease..."
    echo "   (HPA will wait for stabilization window: ~5 minutes)"
    
    # Monitor scaling for 10 minutes (includes stabilization window)
    monitor_scaling 600
    
    # Check if scaling occurred
    final_replicas=$(get_replica_count)
    echo "📊 Final replicas: $final_replicas"
    
    echo ""
    echo "📊 Final HPA status:"
    get_hpa_status
    
    echo ""
    if [ "$final_replicas" -lt "$initial_replicas" ]; then
        echo "✅ Scale down test PASSED"
        echo "   Scaled from $initial_replicas to $final_replicas replicas"
        return 0
    else
        echo "⚠️  Scale down test INCONCLUSIVE"
        echo "   Replicas remained at $initial_replicas"
        echo "   This may be normal if load is still high or within stabilization window"
        return 1
    fi
}

# Test Cluster Autoscaler
test_cluster_autoscaler() {
    echo ""
    echo "🖥️  Test 3: Cluster Autoscaler Behavior"
    echo "========================================"
    
    initial_nodes=$(kubectl get nodes --no-headers | wc -l)
    echo "📊 Initial nodes: $initial_nodes"
    
    echo ""
    echo "📊 Node status:"
    kubectl get nodes -o wide
    
    echo ""
    echo "🔍 Checking for pending pods..."
    pending_pods=$(kubectl get pods -n $NAMESPACE --field-selector=status.phase=Pending --no-headers | wc -l)
    
    if [ "$pending_pods" -gt 0 ]; then
        echo "⚠️  Found $pending_pods pending pods"
        echo "   Cluster Autoscaler should add nodes if needed"
        
        # Wait for nodes to be added
        wait_for_condition "New nodes to be added" \
            "[ \$(kubectl get nodes --no-headers | wc -l) -gt $initial_nodes ]" \
            300
        
        final_nodes=$(kubectl get nodes --no-headers | wc -l)
        
        if [ "$final_nodes" -gt "$initial_nodes" ]; then
            echo "✅ Cluster Autoscaler test PASSED"
            echo "   Nodes increased from $initial_nodes to $final_nodes"
        else
            echo "⚠️  Cluster Autoscaler test INCONCLUSIVE"
            echo "   Nodes remained at $initial_nodes"
        fi
    else
        echo "✅ No pending pods - cluster has sufficient capacity"
    fi
    
    echo ""
    echo "📊 Cluster Autoscaler logs (last 20 lines):"
    kubectl logs -n kube-system deployment/cluster-autoscaler --tail=20 | grep -E "(scale|node)" || true
}

# Main test execution
echo "🔍 Pre-test validation..."
echo ""

# Check if HPA exists
if ! kubectl get hpa -n $NAMESPACE > /dev/null 2>&1; then
    echo "❌ Error: No HPA found in namespace $NAMESPACE"
    exit 1
fi

# Check if deployment exists
if ! kubectl get deployment $DEPLOYMENT_NAME -n $NAMESPACE > /dev/null 2>&1; then
    echo "❌ Error: Deployment $DEPLOYMENT_NAME not found"
    exit 1
fi

echo "✅ Pre-test validation passed"
echo ""

# Run tests based on test type
case $TEST_TYPE in
    scale-up)
        test_scale_up
        ;;
    scale-down)
        test_scale_down
        ;;
    cluster)
        test_cluster_autoscaler
        ;;
    full)
        test_scale_up
        sleep 30
        test_scale_down
        sleep 30
        test_cluster_autoscaler
        ;;
    *)
        echo "❌ Error: Invalid test type: $TEST_TYPE"
        echo "Valid types: scale-up, scale-down, cluster, full"
        exit 1
        ;;
esac

echo ""
echo "🎉 Auto-scaling tests completed!"
echo ""
echo "📋 Summary:"
echo "- Test Type: $TEST_TYPE"
echo "- Namespace: $NAMESPACE"
echo "- Deployment: $DEPLOYMENT_NAME"
echo ""
echo "📚 Next steps:"
echo "1. Review test results above"
echo "2. Check HPA metrics: kubectl get hpa -n $NAMESPACE"
echo "3. Check pod status: kubectl get pods -n $NAMESPACE"
echo "4. Review Cluster Autoscaler logs if needed"
echo "5. Fine-tune thresholds based on observed behavior"
