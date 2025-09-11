#!/bin/bash

# Test GitOps Setup for GenAI Demo
# This script validates the ArgoCD and Argo Rollouts installation and configuration

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARGOCD_NAMESPACE="argocd"
ROLLOUTS_NAMESPACE="argo-rollouts"
GENAI_DEMO_NAMESPACE="genai-demo"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Test functions
test_prerequisites() {
    log_info "Testing prerequisites..."
    
    local failed=0
    
    # Test kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed"
        failed=1
    else
        log_success "kubectl is available"
    fi
    
    # Test cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        failed=1
    else
        log_success "Kubernetes cluster is accessible"
    fi
    
    # Test ArgoCD CLI
    if ! command -v argocd &> /dev/null; then
        log_warning "ArgoCD CLI is not installed (optional)"
    else
        log_success "ArgoCD CLI is available"
    fi
    
    # Test Argo Rollouts CLI
    if ! command -v kubectl-argo-rollouts &> /dev/null; then
        log_warning "Argo Rollouts CLI is not installed (optional)"
    else
        log_success "Argo Rollouts CLI is available"
    fi
    
    return $failed
}

test_namespaces() {
    log_info "Testing namespaces..."
    
    local failed=0
    
    # Check ArgoCD namespace
    if kubectl get namespace ${ARGOCD_NAMESPACE} &> /dev/null; then
        log_success "ArgoCD namespace exists"
    else
        log_error "ArgoCD namespace does not exist"
        failed=1
    fi
    
    # Check Argo Rollouts namespace
    if kubectl get namespace ${ROLLOUTS_NAMESPACE} &> /dev/null; then
        log_success "Argo Rollouts namespace exists"
    else
        log_error "Argo Rollouts namespace does not exist"
        failed=1
    fi
    
    # Check GenAI Demo namespace
    if kubectl get namespace ${GENAI_DEMO_NAMESPACE} &> /dev/null; then
        log_success "GenAI Demo namespace exists"
    else
        log_warning "GenAI Demo namespace does not exist (will be created by ArgoCD)"
    fi
    
    return $failed
}

test_argocd_installation() {
    log_info "Testing ArgoCD installation..."
    
    local failed=0
    
    # Check ArgoCD deployments
    local deployments=("argocd-server" "argocd-application-controller" "argocd-repo-server" "argocd-dex-server")
    
    for deployment in "${deployments[@]}"; do
        if kubectl get deployment ${deployment} -n ${ARGOCD_NAMESPACE} &> /dev/null; then
            local ready=$(kubectl get deployment ${deployment} -n ${ARGOCD_NAMESPACE} -o jsonpath='{.status.readyReplicas}')
            local desired=$(kubectl get deployment ${deployment} -n ${ARGOCD_NAMESPACE} -o jsonpath='{.spec.replicas}')
            
            if [[ "${ready}" == "${desired}" ]]; then
                log_success "ArgoCD ${deployment} is ready (${ready}/${desired})"
            else
                log_error "ArgoCD ${deployment} is not ready (${ready}/${desired})"
                failed=1
            fi
        else
            log_error "ArgoCD ${deployment} does not exist"
            failed=1
        fi
    done
    
    return $failed
}

test_argo_rollouts_installation() {
    log_info "Testing Argo Rollouts installation..."
    
    local failed=0
    
    # Check Argo Rollouts deployment
    if kubectl get deployment argo-rollouts -n ${ROLLOUTS_NAMESPACE} &> /dev/null; then
        local ready=$(kubectl get deployment argo-rollouts -n ${ROLLOUTS_NAMESPACE} -o jsonpath='{.status.readyReplicas}')
        local desired=$(kubectl get deployment argo-rollouts -n ${ROLLOUTS_NAMESPACE} -o jsonpath='{.spec.replicas}')
        
        if [[ "${ready}" == "${desired}" ]]; then
            log_success "Argo Rollouts controller is ready (${ready}/${desired})"
        else
            log_error "Argo Rollouts controller is not ready (${ready}/${desired})"
            failed=1
        fi
    else
        log_error "Argo Rollouts deployment does not exist"
        failed=1
    fi
    
    return $failed
}

test_argocd_applications() {
    log_info "Testing ArgoCD applications..."
    
    local failed=0
    local applications=("genai-demo-backend" "genai-demo-cmc-frontend" "genai-demo-consumer-frontend")
    
    for app in "${applications[@]}"; do
        if kubectl get application ${app} -n ${ARGOCD_NAMESPACE} &> /dev/null; then
            local health=$(kubectl get application ${app} -n ${ARGOCD_NAMESPACE} -o jsonpath='{.status.health.status}')
            local sync=$(kubectl get application ${app} -n ${ARGOCD_NAMESPACE} -o jsonpath='{.status.sync.status}')
            
            log_info "Application ${app}: Health=${health}, Sync=${sync}"
            
            if [[ "${health}" == "Healthy" && "${sync}" == "Synced" ]]; then
                log_success "Application ${app} is healthy and synced"
            else
                log_warning "Application ${app} status: Health=${health}, Sync=${sync}"
            fi
        else
            log_error "Application ${app} does not exist"
            failed=1
        fi
    done
    
    return $failed
}

test_rollout_configurations() {
    log_info "Testing Rollout configurations..."
    
    local failed=0
    local rollouts=("genai-demo-backend" "genai-demo-cmc-frontend" "genai-demo-consumer-frontend")
    
    for rollout in "${rollouts[@]}"; do
        if kubectl get rollout ${rollout} -n ${GENAI_DEMO_NAMESPACE} &> /dev/null; then
            local status=$(kubectl get rollout ${rollout} -n ${GENAI_DEMO_NAMESPACE} -o jsonpath='{.status.phase}')
            local ready=$(kubectl get rollout ${rollout} -n ${GENAI_DEMO_NAMESPACE} -o jsonpath='{.status.readyReplicas}')
            local desired=$(kubectl get rollout ${rollout} -n ${GENAI_DEMO_NAMESPACE} -o jsonpath='{.spec.replicas}')
            
            log_info "Rollout ${rollout}: Status=${status}, Ready=${ready}/${desired}"
            
            if [[ "${status}" == "Healthy" ]]; then
                log_success "Rollout ${rollout} is healthy"
            else
                log_warning "Rollout ${rollout} status: ${status}"
            fi
        else
            log_warning "Rollout ${rollout} does not exist (may not be deployed yet)"
        fi
    done
    
    return $failed
}

test_analysis_templates() {
    log_info "Testing Analysis Templates..."
    
    local failed=0
    local templates=("success-rate" "frontend-success-rate")
    
    for template in "${templates[@]}"; do
        if kubectl get analysistemplate ${template} -n ${GENAI_DEMO_NAMESPACE} &> /dev/null; then
            log_success "Analysis template ${template} exists"
        else
            log_warning "Analysis template ${template} does not exist"
        fi
    done
    
    return $failed
}

test_services_and_ingress() {
    log_info "Testing Services and Ingress..."
    
    local failed=0
    
    # Test ArgoCD server service
    if kubectl get service argocd-server -n ${ARGOCD_NAMESPACE} &> /dev/null; then
        log_success "ArgoCD server service exists"
    else
        log_error "ArgoCD server service does not exist"
        failed=1
    fi
    
    # Test application services (if they exist)
    local services=("genai-demo-backend-active" "genai-demo-backend-preview" 
                   "genai-demo-cmc-frontend-stable" "genai-demo-cmc-frontend-canary"
                   "genai-demo-consumer-frontend-stable" "genai-demo-consumer-frontend-canary")
    
    for service in "${services[@]}"; do
        if kubectl get service ${service} -n ${GENAI_DEMO_NAMESPACE} &> /dev/null; then
            log_success "Service ${service} exists"
        else
            log_warning "Service ${service} does not exist (may not be deployed yet)"
        fi
    done
    
    return $failed
}

test_rbac_and_permissions() {
    log_info "Testing RBAC and permissions..."
    
    local failed=0
    
    # Test ArgoCD project
    if kubectl get appproject genai-demo -n ${ARGOCD_NAMESPACE} &> /dev/null; then
        log_success "ArgoCD project 'genai-demo' exists"
    else
        log_error "ArgoCD project 'genai-demo' does not exist"
        failed=1
    fi
    
    # Test service accounts
    local service_accounts=("genai-demo-backend" "genai-demo-cmc-frontend" "genai-demo-consumer-frontend")
    
    for sa in "${service_accounts[@]}"; do
        if kubectl get serviceaccount ${sa} -n ${GENAI_DEMO_NAMESPACE} &> /dev/null; then
            log_success "Service account ${sa} exists"
        else
            log_warning "Service account ${sa} does not exist (may not be deployed yet)"
        fi
    done
    
    return $failed
}

test_monitoring_integration() {
    log_info "Testing monitoring integration..."
    
    local failed=0
    
    # Test ServiceMonitors
    if kubectl get crd servicemonitors.monitoring.coreos.com &> /dev/null; then
        log_success "ServiceMonitor CRD exists (Prometheus Operator installed)"
        
        # Check for ServiceMonitors
        local monitors=("genai-demo-backend" "rollback-automation")
        for monitor in "${monitors[@]}"; do
            if kubectl get servicemonitor ${monitor} -n ${GENAI_DEMO_NAMESPACE} &> /dev/null; then
                log_success "ServiceMonitor ${monitor} exists"
            else
                log_warning "ServiceMonitor ${monitor} does not exist"
            fi
        done
    else
        log_warning "ServiceMonitor CRD does not exist (Prometheus Operator not installed)"
    fi
    
    return $failed
}

test_connectivity() {
    log_info "Testing connectivity..."
    
    local failed=0
    
    # Test ArgoCD server connectivity
    if kubectl port-forward svc/argocd-server -n ${ARGOCD_NAMESPACE} 8080:443 &> /dev/null &
    then
        local port_forward_pid=$!
        sleep 5
        
        if curl -k -s https://localhost:8080/healthz &> /dev/null; then
            log_success "ArgoCD server is accessible"
        else
            log_warning "ArgoCD server is not accessible via port-forward"
        fi
        
        kill $port_forward_pid 2>/dev/null || true
    else
        log_warning "Cannot establish port-forward to ArgoCD server"
    fi
    
    return $failed
}

run_comprehensive_test() {
    log_info "Running comprehensive GitOps test suite..."
    echo "=================================================="
    
    local total_failed=0
    
    # Run all tests
    test_prerequisites || ((total_failed++))
    echo ""
    
    test_namespaces || ((total_failed++))
    echo ""
    
    test_argocd_installation || ((total_failed++))
    echo ""
    
    test_argo_rollouts_installation || ((total_failed++))
    echo ""
    
    test_argocd_applications || ((total_failed++))
    echo ""
    
    test_rollout_configurations || ((total_failed++))
    echo ""
    
    test_analysis_templates || ((total_failed++))
    echo ""
    
    test_services_and_ingress || ((total_failed++))
    echo ""
    
    test_rbac_and_permissions || ((total_failed++))
    echo ""
    
    test_monitoring_integration || ((total_failed++))
    echo ""
    
    test_connectivity || ((total_failed++))
    echo ""
    
    # Summary
    echo "=================================================="
    if [[ $total_failed -eq 0 ]]; then
        log_success "All tests passed! GitOps setup is working correctly."
    else
        log_warning "Some tests failed or showed warnings. Review the output above."
    fi
    
    return $total_failed
}

show_status() {
    log_info "GitOps Status Summary"
    echo "=================================================="
    
    # ArgoCD status
    echo "ArgoCD Applications:"
    kubectl get applications -n ${ARGOCD_NAMESPACE} 2>/dev/null || echo "No applications found"
    echo ""
    
    # Rollouts status
    echo "Argo Rollouts:"
    kubectl get rollouts -n ${GENAI_DEMO_NAMESPACE} 2>/dev/null || echo "No rollouts found"
    echo ""
    
    # Analysis runs
    echo "Recent Analysis Runs:"
    kubectl get analysisruns -n ${GENAI_DEMO_NAMESPACE} --sort-by=.metadata.creationTimestamp 2>/dev/null | tail -5 || echo "No analysis runs found"
    echo ""
}

show_logs() {
    local component=${1:-"all"}
    
    case $component in
        argocd)
            log_info "ArgoCD Application Controller logs:"
            kubectl logs -n ${ARGOCD_NAMESPACE} deployment/argocd-application-controller --tail=50
            ;;
        rollouts)
            log_info "Argo Rollouts Controller logs:"
            kubectl logs -n ${ROLLOUTS_NAMESPACE} deployment/argo-rollouts --tail=50
            ;;
        all)
            show_logs argocd
            echo ""
            show_logs rollouts
            ;;
        *)
            log_error "Unknown component: $component"
            echo "Available components: argocd, rollouts, all"
            ;;
    esac
}

# Main function
main() {
    case "${1:-test}" in
        test)
            run_comprehensive_test
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "${2:-all}"
            ;;
        prerequisites)
            test_prerequisites
            ;;
        namespaces)
            test_namespaces
            ;;
        argocd)
            test_argocd_installation
            ;;
        rollouts)
            test_argo_rollouts_installation
            ;;
        applications)
            test_argocd_applications
            ;;
        connectivity)
            test_connectivity
            ;;
        *)
            echo "Usage: $0 {test|status|logs|prerequisites|namespaces|argocd|rollouts|applications|connectivity}"
            echo ""
            echo "Commands:"
            echo "  test           - Run comprehensive test suite"
            echo "  status         - Show GitOps status summary"
            echo "  logs [component] - Show logs (argocd|rollouts|all)"
            echo "  prerequisites  - Test prerequisites only"
            echo "  namespaces     - Test namespaces only"
            echo "  argocd         - Test ArgoCD installation only"
            echo "  rollouts       - Test Argo Rollouts installation only"
            echo "  applications   - Test ArgoCD applications only"
            echo "  connectivity   - Test connectivity only"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"