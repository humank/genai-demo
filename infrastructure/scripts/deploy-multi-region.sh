#!/bin/bash

# Multi-Region Infrastructure Deployment Script
# This script deploys the complete multi-region infrastructure for GenAI Demo

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="production"
PROJECT_NAME="genai-demo"
DOMAIN=""
PRIMARY_REGION="ap-east-2"
SECONDARY_REGION="ap-northeast-1"
ENABLE_DR="true"
DRY_RUN="false"
DEPLOY_ALL="false"

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Deploy multi-region infrastructure for GenAI Demo

OPTIONS:
    -e, --environment ENVIRONMENT    Environment (development|staging|production) [default: production]
    -p, --project-name PROJECT       Project name [default: genai-demo]
    -d, --domain DOMAIN             Domain name (e.g., kimkao.io)
    --primary-region REGION         Primary region [default: ap-east-2]
    --secondary-region REGION       Secondary region [default: ap-northeast-1]
    --disable-dr                    Disable disaster recovery deployment
    --dry-run                       Show what would be deployed without deploying
    --deploy-all                    Deploy all stacks without confirmation
    -h, --help                      Show this help message

EXAMPLES:
    # Deploy production multi-region infrastructure
    $0 --environment production --domain kimkao.io

    # Deploy with custom regions
    $0 --primary-region us-east-1 --secondary-region us-west-2

    # Dry run to see what would be deployed
    $0 --dry-run --domain example.com

    # Deploy all stacks without confirmation
    $0 --deploy-all --domain kimkao.io
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -p|--project-name)
            PROJECT_NAME="$2"
            shift 2
            ;;
        -d|--domain)
            DOMAIN="$2"
            shift 2
            ;;
        --primary-region)
            PRIMARY_REGION="$2"
            shift 2
            ;;
        --secondary-region)
            SECONDARY_REGION="$2"
            shift 2
            ;;
        --disable-dr)
            ENABLE_DR="false"
            shift
            ;;
        --dry-run)
            DRY_RUN="true"
            shift
            ;;
        --deploy-all)
            DEPLOY_ALL="true"
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(development|staging|production)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT. Must be development, staging, or production."
    exit 1
fi

# Validate regions
if [[ "$PRIMARY_REGION" == "$SECONDARY_REGION" ]]; then
    print_error "Primary and secondary regions must be different."
    exit 1
fi

# Check if CDK is installed
if ! command -v cdk &> /dev/null; then
    print_error "AWS CDK is not installed. Please install it first:"
    echo "npm install -g aws-cdk"
    exit 1
fi

# Check if AWS CLI is configured
if ! aws sts get-caller-identity &> /dev/null; then
    print_error "AWS CLI is not configured or credentials are invalid."
    echo "Please run 'aws configure' or set up your AWS credentials."
    exit 1
fi

# Print deployment configuration
print_status "Multi-Region Infrastructure Deployment Configuration"
echo "=================================================="
echo "Environment:        $ENVIRONMENT"
echo "Project Name:       $PROJECT_NAME"
echo "Domain:             ${DOMAIN:-"Not specified"}"
echo "Primary Region:     $PRIMARY_REGION"
echo "Secondary Region:   $SECONDARY_REGION"
echo "Disaster Recovery:  $ENABLE_DR"
echo "Dry Run:            $DRY_RUN"
echo "=================================================="

# Set CDK context
CDK_CONTEXT=""
CDK_CONTEXT="$CDK_CONTEXT --context genai-demo:environment=$ENVIRONMENT"
CDK_CONTEXT="$CDK_CONTEXT --context genai-demo:project-name=$PROJECT_NAME"
if [[ -n "$DOMAIN" ]]; then
    CDK_CONTEXT="$CDK_CONTEXT --context genai-demo:domain=$DOMAIN"
fi
CDK_CONTEXT="$CDK_CONTEXT --context genai-demo:regions.primary=$PRIMARY_REGION"
CDK_CONTEXT="$CDK_CONTEXT --context genai-demo:regions.secondary=$SECONDARY_REGION"
CDK_CONTEXT="$CDK_CONTEXT --context genai-demo:multi-region.enable-dr=$ENABLE_DR"

# Function to deploy stacks
deploy_stacks() {
    local deployment_type=$1
    
    print_status "Starting $deployment_type deployment..."
    
    # Build the project first
    print_status "Building CDK project..."
    npm run build
    
    if [[ "$DRY_RUN" == "true" ]]; then
        print_status "Performing dry run (cdk diff)..."
        cdk diff $CDK_CONTEXT --app "node bin/multi-region-deployment.js" --all
        return 0
    fi
    
    # Bootstrap CDK in both regions if needed
    print_status "Bootstrapping CDK in primary region ($PRIMARY_REGION)..."
    cdk bootstrap aws://$(aws sts get-caller-identity --query Account --output text)/$PRIMARY_REGION
    
    if [[ "$ENABLE_DR" == "true" ]]; then
        print_status "Bootstrapping CDK in secondary region ($SECONDARY_REGION)..."
        cdk bootstrap aws://$(aws sts get-caller-identity --query Account --output text)/$SECONDARY_REGION
    fi
    
    # Deploy stacks
    if [[ "$DEPLOY_ALL" == "true" ]]; then
        print_status "Deploying all stacks..."
        cdk deploy $CDK_CONTEXT --app "node bin/multi-region-deployment.js" --all --require-approval never
    else
        print_status "Deploying stacks with confirmation..."
        cdk deploy $CDK_CONTEXT --app "node bin/multi-region-deployment.js" --all
    fi
}

# Confirmation prompt (unless --deploy-all is specified)
if [[ "$DEPLOY_ALL" != "true" && "$DRY_RUN" != "true" ]]; then
    echo
    print_warning "This will deploy infrastructure to AWS which may incur costs."
    read -p "Do you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Deployment cancelled."
        exit 0
    fi
fi

# Change to infrastructure directory
cd "$(dirname "$0")/.."

# Deploy the infrastructure
if [[ "$ENABLE_DR" == "true" ]]; then
    deploy_stacks "multi-region"
else
    deploy_stacks "single-region"
fi

print_success "Infrastructure deployment completed successfully!"

# Print useful information
echo
print_status "Deployment Summary"
echo "=================="
if [[ -n "$DOMAIN" ]]; then
    echo "Primary API Endpoint:    https://api.$DOMAIN"
    if [[ "$ENABLE_DR" == "true" ]]; then
        echo "Secondary API Endpoint:  https://api-dr.$DOMAIN"
        echo "Failover Endpoint:       https://api.$DOMAIN (automatic failover)"
        echo "Latency-based Endpoint:  https://api-latency.$DOMAIN"
    fi
fi

echo
print_status "Next Steps"
echo "=========="
echo "1. Configure your application to use the deployed infrastructure"
echo "2. Set up monitoring and alerting"
echo "3. Test failover procedures (if DR is enabled)"
echo "4. Configure CI/CD pipelines"

if [[ "$ENABLE_DR" == "true" ]]; then
    echo
    print_warning "Disaster Recovery Notes"
    echo "======================="
    echo "- Aurora Global Database is configured for cross-region replication"
    echo "- MSK MirrorMaker 2.0 is set up for event replication"
    echo "- Route 53 health checks will automatically failover traffic"
    echo "- Test failover procedures regularly"
fi

echo
print_status "To destroy the infrastructure later, run:"
echo "cdk destroy $CDK_CONTEXT --app \"node bin/multi-region-deployment.js\" --all"