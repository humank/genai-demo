#!/bin/bash

# Staging Resources Management Script
# 
# This script manages staging environment resources including:
# - Starting/stopping EKS clusters
# - Starting/stopping RDS instances
# - Starting/stopping ElastiCache clusters
# - Resource status checking
#
# Usage:
#   ./manage-staging-resources.sh start|stop|status
#
# Requirements: 12.7, 12.11

set -e

# Configuration
AWS_REGION="${AWS_REGION:-ap-northeast-1}"
ENVIRONMENT="staging"
LOG_FILE="staging-resources-$(date +%Y%m%d-%H%M%S).log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

# Check AWS CLI is installed
check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        error "AWS CLI is not installed. Please install it first."
        exit 1
    fi
    log "AWS CLI found: $(aws --version)"
}

# Get EKS cluster name
get_eks_cluster() {
    aws eks list-clusters --region "$AWS_REGION" --query "clusters[?contains(@, '$ENVIRONMENT')]" --output text
}

# Get RDS cluster identifier
get_rds_cluster() {
    aws rds describe-db-clusters --region "$AWS_REGION" --query "DBClusters[?contains(DBClusterIdentifier, '$ENVIRONMENT')].DBClusterIdentifier" --output text
}

# Get ElastiCache cluster identifier
get_elasticache_cluster() {
    aws elasticache describe-cache-clusters --region "$AWS_REGION" --query "CacheClusters[?contains(CacheClusterId, '$ENVIRONMENT')].CacheClusterId" --output text
}

# Start EKS cluster (scale up node groups)
start_eks() {
    local cluster_name=$1
    log "Starting EKS cluster: $cluster_name"
    
    # Get node groups
    local node_groups=$(aws eks list-nodegroups --cluster-name "$cluster_name" --region "$AWS_REGION" --query "nodegroups" --output text)
    
    for ng in $node_groups; do
        log "Scaling up node group: $ng"
        aws eks update-nodegroup-config \
            --cluster-name "$cluster_name" \
            --nodegroup-name "$ng" \
            --scaling-config minSize=2,maxSize=10,desiredSize=2 \
            --region "$AWS_REGION"
    done
    
    log "✓ EKS cluster $cluster_name started"
}

# Stop EKS cluster (scale down node groups)
stop_eks() {
    local cluster_name=$1
    log "Stopping EKS cluster: $cluster_name"
    
    # Get node groups
    local node_groups=$(aws eks list-nodegroups --cluster-name "$cluster_name" --region "$AWS_REGION" --query "nodegroups" --output text)
    
    for ng in $node_groups; do
        log "Scaling down node group: $ng"
        aws eks update-nodegroup-config \
            --cluster-name "$cluster_name" \
            --nodegroup-name "$ng" \
            --scaling-config minSize=0,maxSize=10,desiredSize=0 \
            --region "$AWS_REGION"
    done
    
    log "✓ EKS cluster $cluster_name stopped"
}

# Start RDS cluster
start_rds() {
    local cluster_id=$1
    log "Starting RDS cluster: $cluster_id"
    
    aws rds start-db-cluster \
        --db-cluster-identifier "$cluster_id" \
        --region "$AWS_REGION"
    
    log "Waiting for RDS cluster to be available..."
    aws rds wait db-cluster-available \
        --db-cluster-identifier "$cluster_id" \
        --region "$AWS_REGION"
    
    log "✓ RDS cluster $cluster_id started"
}

# Stop RDS cluster
stop_rds() {
    local cluster_id=$1
    log "Stopping RDS cluster: $cluster_id"
    
    aws rds stop-db-cluster \
        --db-cluster-identifier "$cluster_id" \
        --region "$AWS_REGION"
    
    log "✓ RDS cluster $cluster_id stopped"
}

# Get resource status
get_status() {
    log "=== Staging Environment Status ==="
    echo ""
    
    # EKS Status
    log "EKS Clusters:"
    local eks_clusters=$(get_eks_cluster)
    if [ -n "$eks_clusters" ]; then
        for cluster in $eks_clusters; do
            local status=$(aws eks describe-cluster --name "$cluster" --region "$AWS_REGION" --query "cluster.status" --output text)
            echo "  - $cluster: $status"
            
            # Node group status
            local node_groups=$(aws eks list-nodegroups --cluster-name "$cluster" --region "$AWS_REGION" --query "nodegroups" --output text)
            for ng in $node_groups; do
                local ng_status=$(aws eks describe-nodegroup --cluster-name "$cluster" --nodegroup-name "$ng" --region "$AWS_REGION" --query "nodegroup.status" --output text)
                local desired=$(aws eks describe-nodegroup --cluster-name "$cluster" --nodegroup-name "$ng" --region "$AWS_REGION" --query "nodegroup.scalingConfig.desiredSize" --output text)
                echo "    - Node Group $ng: $ng_status (Desired: $desired)"
            done
        done
    else
        echo "  No EKS clusters found"
    fi
    echo ""
    
    # RDS Status
    log "RDS Clusters:"
    local rds_clusters=$(get_rds_cluster)
    if [ -n "$rds_clusters" ]; then
        for cluster in $rds_clusters; do
            local status=$(aws rds describe-db-clusters --db-cluster-identifier "$cluster" --region "$AWS_REGION" --query "DBClusters[0].Status" --output text)
            echo "  - $cluster: $status"
        done
    else
        echo "  No RDS clusters found"
    fi
    echo ""
    
    # ElastiCache Status
    log "ElastiCache Clusters:"
    local cache_clusters=$(get_elasticache_cluster)
    if [ -n "$cache_clusters" ]; then
        for cluster in $cache_clusters; do
            local status=$(aws elasticache describe-cache-clusters --cache-cluster-id "$cluster" --region "$AWS_REGION" --query "CacheClusters[0].CacheClusterStatus" --output text)
            echo "  - $cluster: $status"
        done
    else
        echo "  No ElastiCache clusters found"
    fi
    echo ""
}

# Start all resources
start_all() {
    log "Starting all staging resources..."
    
    # Start RDS first (takes longest)
    local rds_clusters=$(get_rds_cluster)
    if [ -n "$rds_clusters" ]; then
        for cluster in $rds_clusters; do
            local status=$(aws rds describe-db-clusters --db-cluster-identifier "$cluster" --region "$AWS_REGION" --query "DBClusters[0].Status" --output text)
            if [ "$status" == "stopped" ]; then
                start_rds "$cluster"
            else
                log "RDS cluster $cluster is already running ($status)"
            fi
        done
    fi
    
    # Start EKS
    local eks_clusters=$(get_eks_cluster)
    if [ -n "$eks_clusters" ]; then
        for cluster in $eks_clusters; do
            start_eks "$cluster"
        done
    fi
    
    log "✓ All resources started"
}

# Stop all resources
stop_all() {
    log "Stopping all staging resources..."
    
    # Stop EKS first
    local eks_clusters=$(get_eks_cluster)
    if [ -n "$eks_clusters" ]; then
        for cluster in $eks_clusters; do
            stop_eks "$cluster"
        done
    fi
    
    # Stop RDS
    local rds_clusters=$(get_rds_cluster)
    if [ -n "$rds_clusters" ]; then
        for cluster in $rds_clusters; do
            local status=$(aws rds describe-db-clusters --db-cluster-identifier "$cluster" --region "$AWS_REGION" --query "DBClusters[0].Status" --output text)
            if [ "$status" == "available" ]; then
                stop_rds "$cluster"
            else
                log "RDS cluster $cluster is not available ($status)"
            fi
        done
    fi
    
    log "✓ All resources stopped"
}

# Main script
main() {
    check_aws_cli
    
    case "${1:-}" in
        start)
            start_all
            ;;
        stop)
            stop_all
            ;;
        status)
            get_status
            ;;
        *)
            echo "Usage: $0 {start|stop|status}"
            echo ""
            echo "Commands:"
            echo "  start   - Start all staging resources"
            echo "  stop    - Stop all staging resources"
            echo "  status  - Show status of all staging resources"
            exit 1
            ;;
    esac
    
    log "Log file: $LOG_FILE"
}

main "$@"
