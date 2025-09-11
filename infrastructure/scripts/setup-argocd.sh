#!/bin/bash

# Setup ArgoCD with GitOps for GenAI Demo
# This script installs and configures ArgoCD with Argo Rollouts for advanced deployment strategies

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="${SCRIPT_DIR}/../k8s"
ARGOCD_VERSION="v2.9.3"
ARGO_ROLLOUTS_VERSION="v1.6.4"
NAMESPACE="argocd"
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

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if kubectl is installed
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
        exit 1
    fi
    
    # Check if kubectl can connect to cluster
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please check your kubeconfig."
        exit 1
    fi
    
    # Check if helm is installed
    if ! command -v helm &> /dev/null; then
        log_error "helm is not installed. Please install helm first."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Install ArgoCD
install_argocd() {
    log_info "Installing ArgoCD..."
    
    # Create namespace
    kubectl create namespace ${NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    
    # Install ArgoCD using official manifests
    kubectl apply -n ${NAMESPACE} -f https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml
    
    # Apply custom configuration
    log_info "Applying custom ArgoCD configuration..."
    kubectl apply -f "${K8S_DIR}/argocd/install.yaml"
    
    # Wait for ArgoCD to be ready
    log_info "Waiting for ArgoCD to be ready..."
    kubectl wait --for=condition=available --timeout=600s deployment/argocd-server -n ${NAMESPACE}
    kubectl wait --for=condition=available --timeout=600s deployment/argocd-application-controller -n ${NAMESPACE}
    kubectl wait --for=condition=available --timeout=600s deployment/argocd-repo-server -n ${NAMESPACE}
    
    log_success "ArgoCD installed successfully"
}

# Install Argo Rollouts
install_argo_rollouts() {
    log_info "Installing Argo Rollouts..."
    
    # Apply Argo Rollouts manifests
    kubectl apply -f "${K8S_DIR}/argocd/argo-rollouts.yaml"
    
    # Wait for Argo Rollouts to be ready
    log_info "Waiting for Argo Rollouts to be ready..."
    kubectl wait --for=condition=available --timeout=300s deployment/argo-rollouts -n argo-rollouts
    
    log_success "Argo Rollouts installed successfully"
}

# Setup GitOps applications
setup_gitops_applications() {
    log_info "Setting up GitOps applications..."
    
    # Create genai-demo namespace
    kubectl create namespace ${GENAI_DEMO_NAMESPACE} --dry-run=client -o yaml | kubectl apply -f -
    
    # Apply ArgoCD project
    kubectl apply -f "${K8S_DIR}/gitops/projects/genai-demo-project.yaml"
    
    # Apply ArgoCD applications
    kubectl apply -f "${K8S_DIR}/gitops/applications/"
    
    log_success "GitOps applications configured successfully"
}

# Setup rollout configurations
setup_rollout_configurations() {
    log_info "Setting up rollout configurations..."
    
    # Apply rollout configurations
    kubectl apply -f "${K8S_DIR}/rollouts/"
    
    log_success "Rollout configurations applied successfully"
}

# Setup monitoring and rollback automation
setup_monitoring() {
    log_info "Setting up monitoring and rollback automation..."
    
    # Apply monitoring configurations
    kubectl apply -f "${K8S_DIR}/monitoring/"
    
    log_success "Monitoring and rollback automation configured successfully"
}

# Get ArgoCD admin password
get_argocd_password() {
    log_info "Retrieving ArgoCD admin password..."
    
    # Wait for secret to be created
    kubectl wait --for=condition=complete --timeout=300s job/argocd-server -n ${NAMESPACE} 2>/dev/null || true
    
    # Get the password
    ARGOCD_PASSWORD=$(kubectl -n ${NAMESPACE} get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d 2>/dev/null || echo "admin")
    
    log_success "ArgoCD admin password: ${ARGOCD_PASSWORD}"
    echo "Please save this password securely!"
}

# Setup port forwarding for ArgoCD UI
setup_port_forwarding() {
    log_info "Setting up port forwarding for ArgoCD UI..."
    
    cat << EOF

To access ArgoCD UI, run the following command in a separate terminal:

kubectl port-forward svc/argocd-server -n ${NAMESPACE} 8080:443

Then open your browser and navigate to: https://localhost:8080

Username: admin
Password: ${ARGOCD_PASSWORD}

EOF
}

# Install ArgoCD CLI
install_argocd_cli() {
    log_info "Installing ArgoCD CLI..."
    
    if command -v argocd &> /dev/null; then
        log_warning "ArgoCD CLI is already installed"
        return
    fi
    
    # Detect OS and architecture
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    case ${ARCH} in
        x86_64) ARCH="amd64" ;;
        aarch64|arm64) ARCH="arm64" ;;
        *) log_error "Unsupported architecture: ${ARCH}"; exit 1 ;;
    esac
    
    # Download and install ArgoCD CLI
    curl -sSL -o argocd https://github.com/argoproj/argo-cd/releases/download/${ARGOCD_VERSION}/argocd-${OS}-${ARCH}
    chmod +x argocd
    sudo mv argocd /usr/local/bin/
    
    log_success "ArgoCD CLI installed successfully"
}

# Install Argo Rollouts CLI
install_rollouts_cli() {
    log_info "Installing Argo Rollouts CLI..."
    
    if command -v kubectl-argo-rollouts &> /dev/null; then
        log_warning "Argo Rollouts CLI is already installed"
        return
    fi
    
    # Detect OS and architecture
    OS=$(uname -s | tr '[:upper:]' '[:lower:]')
    ARCH=$(uname -m)
    
    case ${ARCH} in
        x86_64) ARCH="amd64" ;;
        aarch64|arm64) ARCH="arm64" ;;
        *) log_error "Unsupported architecture: ${ARCH}"; exit 1 ;;
    esac
    
    # Download and install Argo Rollouts CLI
    curl -sSL -o kubectl-argo-rollouts https://github.com/argoproj/argo-rollouts/releases/download/${ARGO_ROLLOUTS_VERSION}/kubectl-argo-rollouts-${OS}-${ARCH}
    chmod +x kubectl-argo-rollouts
    sudo mv kubectl-argo-rollouts /usr/local/bin/
    
    log_success "Argo Rollouts CLI installed successfully"
}

# Verify installation
verify_installation() {
    log_info "Verifying installation..."
    
    # Check ArgoCD pods
    log_info "Checking ArgoCD pods..."
    kubectl get pods -n ${NAMESPACE}
    
    # Check Argo Rollouts pods
    log_info "Checking Argo Rollouts pods..."
    kubectl get pods -n argo-rollouts
    
    # Check ArgoCD applications
    log_info "Checking ArgoCD applications..."
    kubectl get applications -n ${NAMESPACE}
    
    # Check rollouts
    log_info "Checking rollouts..."
    kubectl get rollouts -n ${GENAI_DEMO_NAMESPACE}
    
    log_success "Installation verification completed"
}

# Cleanup function
cleanup() {
    log_warning "Cleaning up ArgoCD installation..."
    
    # Delete applications
    kubectl delete applications --all -n ${NAMESPACE} 2>/dev/null || true
    
    # Delete ArgoCD
    kubectl delete -n ${NAMESPACE} -f https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml 2>/dev/null || true
    
    # Delete Argo Rollouts
    kubectl delete -f "${K8S_DIR}/argocd/argo-rollouts.yaml" 2>/dev/null || true
    
    # Delete namespaces
    kubectl delete namespace ${NAMESPACE} 2>/dev/null || true
    kubectl delete namespace argo-rollouts 2>/dev/null || true
    
    log_success "Cleanup completed"
}

# Main function
main() {
    case "${1:-install}" in
        install)
            log_info "Starting ArgoCD GitOps installation..."
            check_prerequisites
            install_argocd_cli
            install_rollouts_cli
            install_argocd
            install_argo_rollouts
            setup_gitops_applications
            setup_rollout_configurations
            setup_monitoring
            get_argocd_password
            verify_installation
            setup_port_forwarding
            log_success "ArgoCD GitOps installation completed successfully!"
            ;;
        uninstall)
            cleanup
            ;;
        verify)
            verify_installation
            ;;
        password)
            get_argocd_password
            ;;
        port-forward)
            kubectl port-forward svc/argocd-server -n ${NAMESPACE} 8080:443
            ;;
        *)
            echo "Usage: $0 {install|uninstall|verify|password|port-forward}"
            echo ""
            echo "Commands:"
            echo "  install      - Install ArgoCD and Argo Rollouts with GitOps configuration"
            echo "  uninstall    - Remove ArgoCD and Argo Rollouts"
            echo "  verify       - Verify the installation"
            echo "  password     - Get ArgoCD admin password"
            echo "  port-forward - Start port forwarding for ArgoCD UI"
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"