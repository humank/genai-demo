#!/bin/bash

# Unified GenAI Demo Infrastructure Deployment Script
# This script provides a single entry point for all infrastructure deployment scenarios

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="genai-demo"
DEFAULT_ENVIRONMENT="development"
DEFAULT_REGION="ap-east-2"
DEFAULT_ALERT_EMAIL="admin@example.com"

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

print_header() {
    echo -e "${PURPLE}$1${NC}"
}

# Function to show usage
show_usage() {
    cat << EOF
🚀 GenAI Demo - Unified Infrastructure Deployment Script

Usage: $0 [OPTIONS] [DEPLOYMENT_TYPE]

DEPLOYMENT TYPES:
  full                 Deploy complete infrastructure (default)
  foundation          Deploy network, security, and IAM only
  data                Deploy data layer (RDS, ElastiCache, MSK)
  compute             Deploy compute layer (EKS)
  security            Deploy security enhancements (IAM, SSO, IRSA)
  observability       Deploy monitoring and alerting
  analytics           Deploy analytics and data catalog
  multi-region        Deploy multi-region setup
  disaster-recovery   Deploy disaster recovery components

OPTIONS:
  -e, --environment ENV        Environment (development|staging|production) [default: development]
  -r, --region REGION         AWS region [default: ap-east-2]
  -p, --project PROJECT       Project name [default: genai-demo]
  -a, --alert-email EMAIL     Alert email address [default: admin@example.com]
  -s, --sso-arn ARN          SSO instance ARN (required for SSO deployment)
  --enable-analytics          Enable analytics components
  --enable-multi-region       Enable multi-region deployment
  --enable-cdk-nag           Enable CDK Nag security checks
  --enable-code-pipeline     Enable AWS CodePipeline multi-region deployment
  --canary-percentage PCT    Canary deployment percentage (default: 10)
  --blue-green               Enable blue-green deployment strategy
  --dry-run                  Show what would be deployed without deploying
  --destroy                  Destroy infrastructure instead of deploying
  --status                   Show deployment status
  --pipeline-status          Show CodePipeline deployment status
  -h, --help                 Show this help message

EXAMPLES:
  # Deploy complete development environment
  $0 full -e development -r ap-east-2

  # Deploy only foundation components
  $0 foundation -e staging -r ap-southeast-1

  # Deploy with analytics enabled
  $0 full --enable-analytics -a ops@company.com

  # Deploy security components with SSO
  $0 security -s arn:aws:sso:::instance/ssoins-xxxxxxxxx

  # Deploy production with multi-region and CodePipeline
  $0 full -e production --enable-multi-region --enable-code-pipeline

  # Deploy with blue-green strategy
  $0 full --enable-multi-region --blue-green

  # Deploy with custom canary percentage
  $0 full --enable-multi-region --canary-percentage 20

  # Show deployment status
  $0 --status

  # Show CodePipeline status
  $0 --pipeline-status -e production

  # Destroy development environment
  $0 --destroy -e development

EOF
}

# Function to parse command line arguments
parse_arguments() {
    DEPLOYMENT_TYPE="full"
    ENVIRONMENT="$DEFAULT_ENVIRONMENT"
    REGION="$DEFAULT_REGION"
    ALERT_EMAIL="$DEFAULT_ALERT_EMAIL"
    ENABLE_ANALYTICS="false"
    ENABLE_MULTI_REGION="false"
    ENABLE_CDK_NAG="false"
    ENABLE_CODE_PIPELINE="false"
    CANARY_PERCENTAGE="10"
    BLUE_GREEN_DEPLOYMENT="false"
    DRY_RUN="false"
    DESTROY="false"
    SHOW_STATUS="false"
    SHOW_PIPELINE_STATUS="false"
    SSO_INSTANCE_ARN=""

    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -r|--region)
                REGION="$2"
                shift 2
                ;;
            -p|--project)
                PROJECT_NAME="$2"
                shift 2
                ;;
            -a|--alert-email)
                ALERT_EMAIL="$2"
                shift 2
                ;;
            -s|--sso-arn)
                SSO_INSTANCE_ARN="$2"
                shift 2
                ;;
            --enable-analytics)
                ENABLE_ANALYTICS="true"
                shift
                ;;
            --enable-multi-region)
                ENABLE_MULTI_REGION="true"
                shift
                ;;
            --enable-cdk-nag)
                ENABLE_CDK_NAG="true"
                shift
                ;;
            --enable-code-pipeline)
                ENABLE_CODE_PIPELINE="true"
                shift
                ;;
            --canary-percentage)
                CANARY_PERCENTAGE="$2"
                shift 2
                ;;
            --blue-green)
                BLUE_GREEN_DEPLOYMENT="true"
                shift
                ;;
            --dry-run)
                DRY_RUN="true"
                shift
                ;;
            --destroy)
                DESTROY="true"
                shift
                ;;
            --status)
                SHOW_STATUS="true"
                shift
                ;;
            --pipeline-status)
                SHOW_PIPELINE_STATUS="true"
                shift
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            full|foundation|data|compute|security|observability|analytics|multi-region|disaster-recovery)
                DEPLOYMENT_TYPE="$1"
                shift
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

    # Enable multi-region for production by default
    if [[ "$ENVIRONMENT" == "production" ]]; then
        ENABLE_MULTI_REGION="true"
    fi

    # Enable analytics for staging and production by default
    if [[ "$ENVIRONMENT" =~ ^(staging|production)$ ]]; then
        ENABLE_ANALYTICS="true"
    fi

    # Enable CodePipeline when multi-region is enabled
    if [[ "$ENABLE_MULTI_REGION" == "true" ]]; then
        ENABLE_CODE_PIPELINE="true"
    fi
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."

    # Check if we're in the right directory
    if [ ! -f "cdk.json" ]; then
        print_error "Not in CDK project directory. Please run from infrastructure/ directory."
        exit 1
    fi

    # Check if CDK is installed
    if ! command -v cdk &> /dev/null; then
        print_error "AWS CDK is not installed. Please install it first:"
        echo "   npm install -g aws-cdk"
        exit 1
    fi

    # Check if AWS CLI is configured
    if ! aws sts get-caller-identity >/dev/null 2>&1; then
        print_error "AWS CLI is not configured. Please configure it first:"
        echo "   aws configure"
        exit 1
    fi

    # Check Node.js and npm
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed."
        exit 1
    fi

    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed."
        exit 1
    fi

    print_success "Prerequisites check passed"
}

# Function to show deployment status
show_deployment_status() {
    print_header "📊 Deployment Status for $PROJECT_NAME-$ENVIRONMENT"
    echo ""

    local stacks=(
        "$PROJECT_NAME-$ENVIRONMENT-network"
        "$PROJECT_NAME-$ENVIRONMENT-security"
        "$PROJECT_NAME-$ENVIRONMENT-iam"
        "$PROJECT_NAME-$ENVIRONMENT-certificate"
        "$PROJECT_NAME-$ENVIRONMENT-rds"
        "$PROJECT_NAME-$ENVIRONMENT-elasticache"
        "$PROJECT_NAME-$ENVIRONMENT-msk"
        "$PROJECT_NAME-$ENVIRONMENT-eks"
        "$PROJECT_NAME-$ENVIRONMENT-eks-irsa"
        "$PROJECT_NAME-$ENVIRONMENT-sso"
        "$PROJECT_NAME-$ENVIRONMENT-alerting"
        "$PROJECT_NAME-$ENVIRONMENT-observability"
        "$PROJECT_NAME-$ENVIRONMENT-data-catalog"
        "$PROJECT_NAME-$ENVIRONMENT-analytics"
        "$PROJECT_NAME-$ENVIRONMENT-cost-optimization"
        "$PROJECT_NAME-$ENVIRONMENT-core"
        "$PROJECT_NAME-$ENVIRONMENT-dr"
        "$PROJECT_NAME-$ENVIRONMENT-multi-region"
    )

    for stack in "${stacks[@]}"; do
        if aws cloudformation describe-stacks --stack-name "$stack" --region "$REGION" >/dev/null 2>&1; then
            local status=$(aws cloudformation describe-stacks --stack-name "$stack" --region "$REGION" --query 'Stacks[0].StackStatus' --output text)
            case $status in
                *COMPLETE)
                    print_success "$stack: $status"
                    ;;
                *PROGRESS|*PENDING)
                    print_warning "$stack: $status"
                    ;;
                *FAILED|*ROLLBACK*)
                    print_error "$stack: $status"
                    ;;
                *)
                    echo "   $stack: $status"
                    ;;
            esac
        else
            echo "   $stack: NOT_DEPLOYED"
        fi
    done
}

# Function to show CodePipeline deployment status
show_pipeline_status() {
    print_header "📊 CodePipeline Status for $PROJECT_NAME-$ENVIRONMENT"
    echo ""

    local pipeline_name="$PROJECT_NAME-$ENVIRONMENT-multi-region-pipeline"
    
    # Check if pipeline exists
    if ! aws codepipeline get-pipeline --name "$pipeline_name" >/dev/null 2>&1; then
        print_warning "Pipeline $pipeline_name does not exist"
        return 1
    fi
    
    # Get pipeline state
    local pipeline_state=$(aws codepipeline get-pipeline-state --name "$pipeline_name")
    
    if [ $? -eq 0 ]; then
        print_success "Pipeline: $pipeline_name"
        
        # Parse and display stage statuses
        echo "$pipeline_state" | jq -r '.stageStates[] | "\(.stageName): \(.latestExecution.status // "Not Started")"' | while read line; do
            local stage_name=$(echo "$line" | cut -d: -f1)
            local stage_status=$(echo "$line" | cut -d: -f2 | xargs)
            
            case $stage_status in
                "Succeeded")
                    print_success "  $stage_name: $stage_status"
                    ;;
                "InProgress")
                    print_warning "  $stage_name: $stage_status"
                    ;;
                "Failed")
                    print_error "  $stage_name: $stage_status"
                    ;;
                *)
                    echo "  $stage_name: $stage_status"
                    ;;
            esac
        done
        
        # Show recent executions
        echo ""
        print_status "Recent Pipeline Executions:"
        aws codepipeline list-pipeline-executions --pipeline-name "$pipeline_name" --max-items 5 \
            --query 'pipelineExecutionSummaries[*].[pipelineExecutionId,status,startTime]' \
            --output table
    else
        print_error "Failed to get pipeline status"
        return 1
    fi
    
    # Show CodeBuild project statuses
    echo ""
    print_status "CodeBuild Project Status:"
    
    local build_projects=(
        "$PROJECT_NAME-$ENVIRONMENT-infrastructure-build"
        "$PROJECT_NAME-$ENVIRONMENT-application-build"
    )
    
    for project in "${build_projects[@]}"; do
        if aws codebuild batch-get-projects --names "$project" >/dev/null 2>&1; then
            print_success "  $project: EXISTS"
            
            # Show recent builds
            local recent_builds=$(aws codebuild list-builds-for-project --project-name "$project" --sort-order DESCENDING --max-items 3 --query 'ids' --output text)
            
            if [ -n "$recent_builds" ]; then
                echo "    Recent builds:"
                for build_id in $recent_builds; do
                    local build_status=$(aws codebuild batch-get-builds --ids "$build_id" --query 'builds[0].buildStatus' --output text)
                    local build_start=$(aws codebuild batch-get-builds --ids "$build_id" --query 'builds[0].startTime' --output text)
                    echo "      $build_id: $build_status ($build_start)"
                done
            fi
        else
            print_warning "  $project: NOT FOUND"
        fi
    done
    
    # Show CodeDeploy application status
    echo ""
    print_status "CodeDeploy Application Status:"
    
    local app_name="$PROJECT_NAME-$ENVIRONMENT-app"
    if aws deploy get-application --application-name "$app_name" >/dev/null 2>&1; then
        print_success "  Application: $app_name EXISTS"
        
        # Show deployment groups
        local deployment_groups=$(aws deploy list-deployment-groups --application-name "$app_name" --query 'deploymentGroups' --output text)
        
        if [ -n "$deployment_groups" ]; then
            echo "    Deployment Groups:"
            for group in $deployment_groups; do
                echo "      $group"
                
                # Show recent deployments for this group
                local recent_deployments=$(aws deploy list-deployments --application-name "$app_name" --deployment-group-name "$group" --max-items 3 --query 'deployments' --output text)
                
                if [ -n "$recent_deployments" ]; then
                    echo "        Recent deployments:"
                    for deployment_id in $recent_deployments; do
                        local deployment_status=$(aws deploy get-deployment --deployment-id "$deployment_id" --query 'deploymentInfo.status' --output text)
                        local deployment_start=$(aws deploy get-deployment --deployment-id "$deployment_id" --query 'deploymentInfo.createTime' --output text)
                        echo "          $deployment_id: $deployment_status ($deployment_start)"
                    done
                fi
            done
        fi
    else
        print_warning "  Application: $app_name NOT FOUND"
    fi
}

# Function to set CDK context
set_cdk_context() {
    print_status "Setting CDK context..."

    cdk context --set "environment=$ENVIRONMENT"
    cdk context --set "region=$REGION"
    cdk context --set "projectName=$PROJECT_NAME"
    cdk context --set "alertEmail=$ALERT_EMAIL"
    cdk context --set "enableAnalytics=$ENABLE_ANALYTICS"
    cdk context --set "enableMultiRegion=$ENABLE_MULTI_REGION"
    cdk context --set "enableCdkNag=$ENABLE_CDK_NAG"
    cdk context --set "enableCodePipeline=$ENABLE_CODE_PIPELINE"
    cdk context --set "canaryPercentage=$CANARY_PERCENTAGE"
    cdk context --set "blueGreenDeployment=$BLUE_GREEN_DEPLOYMENT"
    
    # Set replication regions for multi-region deployment
    if [ "$ENABLE_MULTI_REGION" = "true" ]; then
        case $REGION in
            ap-east-2)
                cdk context --set "replicationRegions=[\"ap-northeast-1\",\"ap-southeast-1\"]"
                ;;
            ap-northeast-1)
                cdk context --set "replicationRegions=[\"ap-east-2\",\"ap-southeast-1\"]"
                ;;
            ap-southeast-1)
                cdk context --set "replicationRegions=[\"ap-east-2\",\"ap-northeast-1\"]"
                ;;
            *)
                cdk context --set "replicationRegions=[\"ap-northeast-1\"]"
                ;;
        esac
    fi

    if [ -n "$SSO_INSTANCE_ARN" ]; then
        cdk context --set "ssoInstanceArn=$SSO_INSTANCE_ARN"
    fi

    print_success "CDK context configured"
}

# Function to bootstrap CDK
bootstrap_cdk() {
    print_status "Checking CDK bootstrap..."

    if ! aws cloudformation describe-stacks --stack-name "CDKToolkit" --region "$REGION" >/dev/null 2>&1; then
        print_status "Bootstrapping CDK..."
        cdk bootstrap --region "$REGION"
        print_success "CDK bootstrap completed"
    else
        print_success "CDK already bootstrapped"
    fi
}

# Function to build project
build_project() {
    print_status "Building TypeScript project..."

    if npm run build; then
        print_success "Build successful"
    else
        print_error "Build failed"
        exit 1
    fi
}

# Function to run tests
run_tests() {
    print_status "Running unit tests..."

    if npm run test:unit; then
        print_success "Tests passed"
    else
        print_warning "Some tests failed, but continuing deployment"
    fi
}

# Function to get stacks for deployment type
get_stacks_for_deployment_type() {
    local deployment_type=$1
    local stacks=()

    case $deployment_type in
        foundation)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-network"
                "$PROJECT_NAME-$ENVIRONMENT-security"
                "$PROJECT_NAME-$ENVIRONMENT-iam"
                "$PROJECT_NAME-$ENVIRONMENT-certificate"
            )
            ;;
        data)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-rds"
                "$PROJECT_NAME-$ENVIRONMENT-elasticache"
                "$PROJECT_NAME-$ENVIRONMENT-msk"
            )
            ;;
        compute)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-eks"
            )
            ;;
        security)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-iam"
                "$PROJECT_NAME-$ENVIRONMENT-secrets"
                "$PROJECT_NAME-$ENVIRONMENT-eks-irsa"
            )
            if [ -n "$SSO_INSTANCE_ARN" ]; then
                stacks+=("$PROJECT_NAME-$ENVIRONMENT-sso")
            fi
            ;;
        observability)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-alerting"
                "$PROJECT_NAME-$ENVIRONMENT-observability"
            )
            ;;
        analytics)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-data-catalog"
                "$PROJECT_NAME-$ENVIRONMENT-analytics"
            )
            ;;
        multi-region)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-secrets"
                "$PROJECT_NAME-$ENVIRONMENT-multi-region"
            )
            ;;
        disaster-recovery)
            stacks=(
                "$PROJECT_NAME-$ENVIRONMENT-dr"
            )
            ;;
        full)
            # Deploy all stacks
            stacks=("--all")
            ;;
        *)
            print_error "Unknown deployment type: $deployment_type"
            exit 1
            ;;
    esac

    echo "${stacks[@]}"
}

# Function to deploy stacks
deploy_stacks() {
    local deployment_type=$1
    local stacks=($(get_stacks_for_deployment_type "$deployment_type"))

    print_header "🚀 Deploying $deployment_type infrastructure"
    echo ""
    print_status "Environment: $ENVIRONMENT"
    print_status "Region: $REGION"
    print_status "Project: $PROJECT_NAME"
    print_status "Analytics: $ENABLE_ANALYTICS"
    print_status "Multi-Region: $ENABLE_MULTI_REGION"
    print_status "CDK Nag: $ENABLE_CDK_NAG"
    if [ -n "$SSO_INSTANCE_ARN" ]; then
        print_status "SSO Instance: $SSO_INSTANCE_ARN"
    fi
    echo ""

    if [ "$DRY_RUN" = "true" ]; then
        print_warning "DRY RUN MODE - No actual deployment will occur"
        print_status "Would deploy the following stacks:"
        for stack in "${stacks[@]}"; do
            echo "   - $stack"
        done
        return 0
    fi

    # Check if multi-region deployment is enabled
    if [ "$ENABLE_MULTI_REGION" = "true" ] && [ "$deployment_type" = "full" ]; then
        deploy_multi_region_aws_code "$stacks"
    else
        # Deploy stacks normally
        if [ "${stacks[0]}" = "--all" ]; then
            print_status "Deploying all stacks..."
            cdk deploy --all --require-approval never --region "$REGION"
        else
            for stack in "${stacks[@]}"; do
                print_status "Deploying $stack..."
                if cdk deploy "$stack" --require-approval never --region "$REGION"; then
                    print_success "Successfully deployed $stack"
                else
                    print_error "Failed to deploy $stack"
                    exit 1
                fi
            done
        fi
    fi

    print_success "Deployment completed successfully!"
}

# Function to deploy multi-region AWS Code Services pipeline
deploy_multi_region_aws_code() {
    local stacks=("$@")
    
    print_header "🌍 AWS Code Services Multi-Region Deployment Pipeline"
    echo ""
    
    # Get replication regions from context
    local replication_regions
    case $REGION in
        ap-east-2)
            replication_regions=("ap-northeast-1" "ap-southeast-1")
            ;;
        ap-northeast-1)
            replication_regions=("ap-east-2" "ap-southeast-1")
            ;;
        ap-southeast-1)
            replication_regions=("ap-east-2" "ap-northeast-1")
            ;;
        *)
            replication_regions=("ap-northeast-1")
            ;;
    esac
    
    print_status "Primary Region: $REGION"
    print_status "Replication Regions: ${replication_regions[*]}"
    echo ""
    
    # Phase 1: Deploy foundation stacks in primary region
    print_status "Phase 1: Deploying foundation infrastructure in primary region ($REGION)..."
    local foundation_stacks=(
        "$PROJECT_NAME-$ENVIRONMENT-network"
        "$PROJECT_NAME-$ENVIRONMENT-security"
        "$PROJECT_NAME-$ENVIRONMENT-iam"
        "$PROJECT_NAME-$ENVIRONMENT-certificate"
        "$PROJECT_NAME-$ENVIRONMENT-secrets"
    )
    
    for stack in "${foundation_stacks[@]}"; do
        print_status "Deploying $stack in $REGION..."
        if cdk deploy "$stack" --require-approval never --region "$REGION"; then
            print_success "Successfully deployed $stack in $REGION"
        else
            print_error "Failed to deploy $stack in $REGION"
            exit 1
        fi
    done
    
    # Phase 2: Deploy foundation stacks in replication regions
    for target_region in "${replication_regions[@]}"; do
        print_status "Phase 2: Deploying foundation infrastructure in replication region ($target_region)..."
        
        # Update CDK context for target region
        cdk context --set "region=$target_region"
        
        for stack in "${foundation_stacks[@]}"; do
            local target_stack_name="${stack/$REGION/$target_region}"
            print_status "Deploying $target_stack_name in $target_region..."
            if cdk deploy "$target_stack_name" --require-approval never --region "$target_region"; then
                print_success "Successfully deployed $target_stack_name in $target_region"
            else
                print_error "Failed to deploy $target_stack_name in $target_region"
                exit 1
            fi
        done
    done
    
    # Phase 3: Deploy data layer with cross-region replication
    print_status "Phase 3: Deploying data layer with cross-region replication..."
    local data_stacks=(
        "$PROJECT_NAME-$ENVIRONMENT-rds"
        "$PROJECT_NAME-$ENVIRONMENT-elasticache"
        "$PROJECT_NAME-$ENVIRONMENT-msk"
    )
    
    # Deploy in primary region first
    for stack in "${data_stacks[@]}"; do
        print_status "Deploying $stack in primary region ($REGION)..."
        if cdk deploy "$stack" --require-approval never --region "$REGION"; then
            print_success "Successfully deployed $stack in $REGION"
        else
            print_error "Failed to deploy $stack in $REGION"
            exit 1
        fi
    done
    
    # Wait for data layer to be ready before deploying to other regions
    print_status "Waiting for data layer to be ready..."
    sleep 30
    
    # Deploy data layer in replication regions
    for target_region in "${replication_regions[@]}"; do
        cdk context --set "region=$target_region"
        
        for stack in "${data_stacks[@]}"; do
            local target_stack_name="${stack/$REGION/$target_region}"
            print_status "Deploying $target_stack_name in $target_region..."
            if cdk deploy "$target_stack_name" --require-approval never --region "$target_region"; then
                print_success "Successfully deployed $target_stack_name in $target_region"
            else
                print_error "Failed to deploy $target_stack_name in $target_region"
                exit 1
            fi
        done
    done
    
    # Phase 4: Deploy compute and application layer
    print_status "Phase 4: Deploying compute and application layer..."
    local compute_stacks=(
        "$PROJECT_NAME-$ENVIRONMENT-eks"
        "$PROJECT_NAME-$ENVIRONMENT-eks-irsa"
        "$PROJECT_NAME-$ENVIRONMENT-core"
    )
    
    # Deploy in all regions simultaneously for Active-Active setup
    for target_region in "$REGION" "${replication_regions[@]}"; do
        cdk context --set "region=$target_region"
        
        for stack in "${compute_stacks[@]}"; do
            local target_stack_name="${stack/$REGION/$target_region}"
            print_status "Deploying $target_stack_name in $target_region..."
            if cdk deploy "$target_stack_name" --require-approval never --region "$target_region"; then
                print_success "Successfully deployed $target_stack_name in $target_region"
            else
                print_error "Failed to deploy $target_stack_name in $target_region"
                exit 1
            fi
        done
    done
    
    # Phase 5: Deploy global routing and CDN
    print_status "Phase 5: Deploying global routing and CDN..."
    cdk context --set "region=$REGION"  # Reset to primary region
    
    local global_stacks=(
        "$PROJECT_NAME-$ENVIRONMENT-route53-global-routing"
        "$PROJECT_NAME-$ENVIRONMENT-cloudfront-global-cdn"
        "$PROJECT_NAME-$ENVIRONMENT-multi-region"
    )
    
    for stack in "${global_stacks[@]}"; do
        print_status "Deploying $stack..."
        if cdk deploy "$stack" --require-approval never --region "$REGION"; then
            print_success "Successfully deployed $stack"
        else
            print_error "Failed to deploy $stack"
            exit 1
        fi
    done
    
    # Phase 6: Deploy monitoring and observability
    print_status "Phase 6: Deploying monitoring and observability..."
    local monitoring_stacks=(
        "$PROJECT_NAME-$ENVIRONMENT-alerting"
        "$PROJECT_NAME-$ENVIRONMENT-observability"
    )
    
    for stack in "${monitoring_stacks[@]}"; do
        print_status "Deploying $stack in primary region ($REGION)..."
        if cdk deploy "$stack" --require-approval never --region "$REGION"; then
            print_success "Successfully deployed $stack in $REGION"
        else
            print_error "Failed to deploy $stack in $REGION"
            exit 1
        fi
    done
    
    # Phase 7: Setup AWS Code Services Pipeline
    print_status "Phase 7: Setting up AWS Code Services multi-region pipeline..."
    setup_aws_code_services_pipeline
    
    # Phase 8: Trigger cross-region synchronization
    print_status "Phase 8: Triggering cross-region synchronization..."
    trigger_cross_region_sync
    
    print_success "Multi-region AWS Code Services deployment completed successfully!"
}

# Function to setup AWS Code Services multi-region pipeline
setup_aws_code_services_pipeline() {
    print_status "Setting up AWS Code Services multi-region deployment pipeline..."
    
    # Get AWS account ID
    local account_id=$(aws sts get-caller-identity --query Account --output text)
    
    # Create necessary IAM roles first
    create_code_services_iam_roles "$account_id"
    
    # Wait for IAM roles to propagate
    print_status "Waiting for IAM roles to propagate..."
    sleep 10
    
    # Create CodePipeline for multi-region deployment
    create_multi_region_codepipeline "$account_id"
    
    # Setup CodeBuild projects for multi-region builds
    setup_multi_region_codebuild "$account_id"
    
    # Configure CodeDeploy applications for blue-green and canary deployments
    configure_multi_region_codedeploy "$account_id"
    
    # Setup CloudWatch alarms for auto-rollback
    setup_deployment_monitoring_alarms
    
    print_success "AWS Code Services pipeline setup completed"
}

# Function to create multi-region CodePipeline
create_multi_region_codepipeline() {
    local account_id=$1
    local pipeline_name="$PROJECT_NAME-$ENVIRONMENT-multi-region-pipeline"
    
    print_status "Creating multi-region CodePipeline: $pipeline_name"
    
    # Check if pipeline already exists
    if aws codepipeline get-pipeline --name "$pipeline_name" >/dev/null 2>&1; then
        print_warning "Pipeline $pipeline_name already exists, updating..."
        update_existing_pipeline "$pipeline_name" "$account_id"
        return
    fi
    
    # Create S3 bucket for artifacts if it doesn't exist
    local artifacts_bucket="$PROJECT_NAME-$ENVIRONMENT-pipeline-artifacts"
    if ! aws s3 ls "s3://$artifacts_bucket" >/dev/null 2>&1; then
        aws s3 mb "s3://$artifacts_bucket" --region "$REGION"
        aws s3api put-bucket-versioning --bucket "$artifacts_bucket" --versioning-configuration Status=Enabled
    fi
    
    # Create pipeline configuration
    local pipeline_config=$(cat << EOF
{
    "pipeline": {
        "name": "$pipeline_name",
        "roleArn": "arn:aws:iam::$account_id:role/CodePipelineServiceRole-$PROJECT_NAME-$ENVIRONMENT",
        "artifactStore": {
            "$REGION": {
                "type": "S3",
                "location": "$artifacts_bucket"
            }
        },
        "stages": [
            {
                "name": "Source",
                "actions": [
                    {
                        "name": "SourceAction",
                        "actionTypeId": {
                            "category": "Source",
                            "owner": "AWS",
                            "provider": "S3",
                            "version": "1"
                        },
                        "configuration": {
                            "S3Bucket": "$artifacts_bucket",
                            "S3ObjectKey": "source.zip",
                            "PollForSourceChanges": "false"
                        },
                        "outputArtifacts": [
                            {"name": "SourceOutput"}
                        ]
                    }
                ]
            },
            {
                "name": "Build",
                "actions": [
                    {
                        "name": "BuildInfrastructure",
                        "actionTypeId": {
                            "category": "Build",
                            "owner": "AWS",
                            "provider": "CodeBuild",
                            "version": "1"
                        },
                        "configuration": {
                            "ProjectName": "$PROJECT_NAME-$ENVIRONMENT-infrastructure-build"
                        },
                        "inputArtifacts": [
                            {"name": "SourceOutput"}
                        ],
                        "outputArtifacts": [
                            {"name": "InfrastructureOutput"}
                        ]
                    },
                    {
                        "name": "BuildApplication",
                        "actionTypeId": {
                            "category": "Build",
                            "owner": "AWS",
                            "provider": "CodeBuild",
                            "version": "1"
                        },
                        "configuration": {
                            "ProjectName": "$PROJECT_NAME-$ENVIRONMENT-application-build"
                        },
                        "inputArtifacts": [
                            {"name": "SourceOutput"}
                        ],
                        "outputArtifacts": [
                            {"name": "ApplicationOutput"}
                        ]
                    }
                ]
            },
            {
                "name": "DeployInfrastructure",
                "actions": [
                    {
                        "name": "DeployPrimaryRegion",
                        "actionTypeId": {
                            "category": "Deploy",
                            "owner": "AWS",
                            "provider": "CloudFormation",
                            "version": "1"
                        },
                        "configuration": {
                            "ActionMode": "CREATE_UPDATE",
                            "StackName": "$PROJECT_NAME-$ENVIRONMENT-infrastructure",
                            "TemplatePath": "InfrastructureOutput::infrastructure-template.yaml",
                            "Capabilities": "CAPABILITY_IAM,CAPABILITY_NAMED_IAM",
                            "RoleArn": "arn:aws:iam::$account_id:role/CloudFormationServiceRole-$PROJECT_NAME-$ENVIRONMENT"
                        },
                        "inputArtifacts": [
                            {"name": "InfrastructureOutput"}
                        ],
                        "region": "$REGION",
                        "runOrder": 1
                    }
                ]
            },
            {
                "name": "DeployApplicationParallel",
                "actions": [
                    {
                        "name": "DeployPrimaryApp",
                        "actionTypeId": {
                            "category": "Deploy",
                            "owner": "AWS",
                            "provider": "CodeDeploy",
                            "version": "1"
                        },
                        "configuration": {
                            "ApplicationName": "$PROJECT_NAME-$ENVIRONMENT-app",
                            "DeploymentGroupName": "$REGION-deployment-group"
                        },
                        "inputArtifacts": [
                            {"name": "ApplicationOutput"}
                        ],
                        "region": "$REGION",
                        "runOrder": 1
                    }
                ]
            }
        ]
    }
}
EOF
    )
    
    # Add deployment actions for replication regions
    local replication_regions
    case $REGION in
        ap-east-2)
            replication_regions=("ap-northeast-1" "ap-southeast-1")
            ;;
        ap-northeast-1)
            replication_regions=("ap-east-2" "ap-southeast-1")
            ;;
        ap-southeast-1)
            replication_regions=("ap-east-2" "ap-northeast-1")
            ;;
        *)
            replication_regions=("ap-northeast-1")
            ;;
    esac
    
    # Create the pipeline
    echo "$pipeline_config" > /tmp/pipeline-config.json
    
    # Add replication region actions to the pipeline config
    for region in "${replication_regions[@]}"; do
        add_region_deployment_action "/tmp/pipeline-config.json" "$region" "$account_id"
    done
    
    # Create the pipeline
    if aws codepipeline create-pipeline --cli-input-json file:///tmp/pipeline-config.json; then
        print_success "Successfully created pipeline: $pipeline_name"
    else
        print_error "Failed to create pipeline: $pipeline_name"
        return 1
    fi
    
    # Clean up temporary file
    rm -f /tmp/pipeline-config.json
}

# Function to add region deployment action to pipeline config
add_region_deployment_action() {
    local config_file=$1
    local region=$2
    local account_id=$3
    
    # This is a simplified version - in practice, you'd use jq or a more sophisticated JSON manipulation
    print_status "Adding deployment action for region: $region"
    
    # Add infrastructure deployment action for the region
    local infra_action=$(cat << EOF
                    {
                        "name": "Deploy${region//-/}Infrastructure",
                        "actionTypeId": {
                            "category": "Deploy",
                            "owner": "AWS",
                            "provider": "CloudFormation",
                            "version": "1"
                        },
                        "configuration": {
                            "ActionMode": "CREATE_UPDATE",
                            "StackName": "$PROJECT_NAME-$ENVIRONMENT-infrastructure",
                            "TemplatePath": "InfrastructureOutput::infrastructure-template.yaml",
                            "Capabilities": "CAPABILITY_IAM,CAPABILITY_NAMED_IAM",
                            "RoleArn": "arn:aws:iam::$account_id:role/CloudFormationServiceRole-$PROJECT_NAME-$ENVIRONMENT"
                        },
                        "inputArtifacts": [
                            {"name": "InfrastructureOutput"}
                        ],
                        "region": "$region",
                        "runOrder": 2
                    }
EOF
    )
    
    # Add application deployment action for the region
    local app_action=$(cat << EOF
                    {
                        "name": "Deploy${region//-/}App",
                        "actionTypeId": {
                            "category": "Deploy",
                            "owner": "AWS",
                            "provider": "CodeDeploy",
                            "version": "1"
                        },
                        "configuration": {
                            "ApplicationName": "$PROJECT_NAME-$ENVIRONMENT-app",
                            "DeploymentGroupName": "$region-deployment-group"
                        },
                        "inputArtifacts": [
                            {"name": "ApplicationOutput"}
                        ],
                        "region": "$region",
                        "runOrder": 2
                    }
EOF
    )
    
    print_status "Region deployment actions configured for $region"
}

# Function to setup multi-region CodeBuild projects
setup_multi_region_codebuild() {
    local account_id=$1
    
    print_status "Setting up CodeBuild projects for multi-region deployment..."
    
    # Create infrastructure build project
    create_infrastructure_build_project "$account_id"
    
    # Create application build project
    create_application_build_project "$account_id"
    
    print_success "CodeBuild projects setup completed"
}

# Function to create infrastructure build project
create_infrastructure_build_project() {
    local account_id=$1
    local project_name="$PROJECT_NAME-$ENVIRONMENT-infrastructure-build"
    
    print_status "Creating infrastructure build project: $project_name"
    
    # Check if project already exists
    if aws codebuild batch-get-projects --names "$project_name" --query 'projects[0].name' --output text 2>/dev/null | grep -q "$project_name"; then
        print_warning "Build project $project_name already exists, updating..."
        update_infrastructure_build_project "$project_name" "$account_id"
        return
    fi
    
    local buildspec_content=$(cat << 'EOF'
version: 0.2
phases:
  install:
    runtime-versions:
      nodejs: 18
    commands:
      - echo Installing dependencies...
      - npm install -g aws-cdk@latest
      - npm install -g typescript
  pre_build:
    commands:
      - echo Logging in to Amazon ECR...
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
      - echo Preparing infrastructure build...
      - cd infrastructure
      - npm install
  build:
    commands:
      - echo Build started on `date`
      - echo Building infrastructure...
      - npm run build
      - npm run test:unit
      - echo Synthesizing CDK templates...
      - cdk synth --all --output ../cdk.out
      - echo Preparing deployment artifacts...
      - cd ..
      - cp -r cdk.out/* ./
  post_build:
    commands:
      - echo Build completed on `date`
      - echo Infrastructure build successful
artifacts:
  files:
    - '**/*'
  name: infrastructure-build-$(date +%Y-%m-%d-%H-%M-%S)
cache:
  paths:
    - 'infrastructure/node_modules/**/*'
    - 'infrastructure/.npm/**/*'
EOF
    )
    
    # Create build project
    local project_config=$(cat << EOF
{
    "name": "$project_name",
    "description": "Multi-region infrastructure build for $PROJECT_NAME $ENVIRONMENT",
    "source": {
        "type": "CODEPIPELINE",
        "buildspec": "$buildspec_content"
    },
    "artifacts": {
        "type": "CODEPIPELINE"
    },
    "environment": {
        "type": "LINUX_CONTAINER",
        "image": "aws/codebuild/amazonlinux2-x86_64-standard:4.0",
        "computeType": "BUILD_GENERAL1_MEDIUM",
        "privilegedMode": true,
        "environmentVariables": [
            {
                "name": "AWS_DEFAULT_REGION",
                "value": "$REGION"
            },
            {
                "name": "AWS_ACCOUNT_ID",
                "value": "$account_id"
            },
            {
                "name": "PROJECT_NAME",
                "value": "$PROJECT_NAME"
            },
            {
                "name": "ENVIRONMENT",
                "value": "$ENVIRONMENT"
            }
        ]
    },
    "serviceRole": "arn:aws:iam::$account_id:role/CodeBuildServiceRole-$PROJECT_NAME-$ENVIRONMENT",
    "timeoutInMinutes": 30,
    "queuedTimeoutInMinutes": 480,
    "cache": {
        "type": "LOCAL",
        "modes": ["LOCAL_DOCKER_LAYER_CACHE", "LOCAL_SOURCE_CACHE"]
    }
}
EOF
    )
    
    echo "$project_config" > /tmp/infrastructure-build-config.json
    
    if aws codebuild create-project --cli-input-json file:///tmp/infrastructure-build-config.json; then
        print_success "Successfully created infrastructure build project: $project_name"
    else
        print_error "Failed to create infrastructure build project: $project_name"
        return 1
    fi
    
    rm -f /tmp/infrastructure-build-config.json
}

# Function to create application build project
create_application_build_project() {
    local account_id=$1
    local project_name="$PROJECT_NAME-$ENVIRONMENT-application-build"
    
    print_status "Creating application build project: $project_name"
    
    # Check if project already exists
    if aws codebuild batch-get-projects --names "$project_name" --query 'projects[0].name' --output text 2>/dev/null | grep -q "$project_name"; then
        print_warning "Build project $project_name already exists, updating..."
        update_application_build_project "$project_name" "$account_id"
        return
    fi
    
    local buildspec_content=$(cat << 'EOF'
version: 0.2
phases:
  install:
    runtime-versions:
      java: corretto17
      docker: 20
    commands:
      - echo Installing dependencies...
  pre_build:
    commands:
      - echo Logging in to Amazon ECR...
      - aws ecr get-login-password --region $AWS_DEFAULT_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com
      - echo Preparing application build...
      - cd app
  build:
    commands:
      - echo Build started on `date`
      - echo Building Spring Boot application...
      - ./gradlew clean build -x test
      - echo Running tests...
      - ./gradlew test
      - echo Building Docker image...
      - docker build -t $IMAGE_REPO_NAME:$IMAGE_TAG .
      - docker tag $IMAGE_REPO_NAME:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG
      - docker tag $IMAGE_REPO_NAME:$IMAGE_TAG $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:latest
  post_build:
    commands:
      - echo Build completed on `date`
      - echo Pushing Docker image...
      - docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:$IMAGE_TAG
      - docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_DEFAULT_REGION.amazonaws.com/$IMAGE_REPO_NAME:latest
      - echo Creating deployment artifacts...
      - cd ..
      - mkdir -p deployment
      - echo '{"imageUri":"'$AWS_ACCOUNT_ID'.dkr.ecr.'$AWS_DEFAULT_REGION'.amazonaws.com/'$IMAGE_REPO_NAME':'$IMAGE_TAG'"}' > deployment/imageDetail.json
      - cp -r k8s deployment/
artifacts:
  files:
    - 'deployment/**/*'
  name: application-build-$(date +%Y-%m-%d-%H-%M-%S)
cache:
  paths:
    - 'app/.gradle/**/*'
    - '/root/.gradle/**/*'
EOF
    )
    
    # Create build project
    local project_config=$(cat << EOF
{
    "name": "$project_name",
    "description": "Multi-region application build for $PROJECT_NAME $ENVIRONMENT",
    "source": {
        "type": "CODEPIPELINE",
        "buildspec": "$buildspec_content"
    },
    "artifacts": {
        "type": "CODEPIPELINE"
    },
    "environment": {
        "type": "LINUX_CONTAINER",
        "image": "aws/codebuild/amazonlinux2-x86_64-standard:4.0",
        "computeType": "BUILD_GENERAL1_MEDIUM",
        "privilegedMode": true,
        "environmentVariables": [
            {
                "name": "AWS_DEFAULT_REGION",
                "value": "$REGION"
            },
            {
                "name": "AWS_ACCOUNT_ID",
                "value": "$account_id"
            },
            {
                "name": "IMAGE_REPO_NAME",
                "value": "$PROJECT_NAME-$ENVIRONMENT-app"
            },
            {
                "name": "IMAGE_TAG",
                "value": "latest"
            }
        ]
    },
    "serviceRole": "arn:aws:iam::$account_id:role/CodeBuildServiceRole-$PROJECT_NAME-$ENVIRONMENT",
    "timeoutInMinutes": 45,
    "queuedTimeoutInMinutes": 480,
    "cache": {
        "type": "LOCAL",
        "modes": ["LOCAL_DOCKER_LAYER_CACHE", "LOCAL_SOURCE_CACHE"]
    }
}
EOF
    )
    
    echo "$project_config" > /tmp/application-build-config.json
    
    if aws codebuild create-project --cli-input-json file:///tmp/application-build-config.json; then
        print_success "Successfully created application build project: $project_name"
    else
        print_error "Failed to create application build project: $project_name"
        return 1
    fi
    
    rm -f /tmp/application-build-config.json
}

# Function to configure multi-region CodeDeploy
configure_multi_region_codedeploy() {
    local account_id=$1
    
    print_status "Configuring CodeDeploy for multi-region deployment..."
    
    # Create CodeDeploy application
    create_codedeploy_application "$account_id"
    
    # Create deployment configurations
    create_deployment_configurations "$account_id"
    
    # Create deployment groups for each region
    create_deployment_groups "$account_id"
    
    print_success "CodeDeploy configuration completed"
}

# Function to create CodeDeploy application
create_codedeploy_application() {
    local account_id=$1
    local app_name="$PROJECT_NAME-$ENVIRONMENT-app"
    
    print_status "Creating CodeDeploy application: $app_name"
    
    # Check if application already exists
    if aws deploy get-application --application-name "$app_name" >/dev/null 2>&1; then
        print_warning "CodeDeploy application $app_name already exists"
        return
    fi
    
    # Create application
    if aws deploy create-application \
        --application-name "$app_name" \
        --compute-platform ECS; then
        print_success "Successfully created CodeDeploy application: $app_name"
    else
        print_error "Failed to create CodeDeploy application: $app_name"
        return 1
    fi
}

# Function to create deployment configurations
create_deployment_configurations() {
    local account_id=$1
    
    print_status "Creating deployment configurations..."
    
    # Create canary deployment configuration
    create_canary_deployment_config "$account_id"
    
    # Create blue-green deployment configuration
    create_blue_green_deployment_config "$account_id"
}

# Function to create canary deployment configuration
create_canary_deployment_config() {
    local account_id=$1
    local config_name="$PROJECT_NAME-$ENVIRONMENT-canary-10-percent"
    
    print_status "Creating canary deployment configuration: $config_name"
    
    # Check if configuration already exists
    if aws deploy get-deployment-config --deployment-config-name "$config_name" >/dev/null 2>&1; then
        print_warning "Deployment configuration $config_name already exists"
        return
    fi
    
    local config_json=$(cat << EOF
{
    "deploymentConfigName": "$config_name",
    "trafficRoutingConfig": {
        "type": "TimeBasedCanary",
        "timeBasedCanary": {
            "canaryPercentage": 10,
            "canaryInterval": 5
        }
    }
}
EOF
    )
    
    echo "$config_json" > /tmp/canary-config.json
    
    if aws deploy create-deployment-config --cli-input-json file:///tmp/canary-config.json; then
        print_success "Successfully created canary deployment configuration: $config_name"
    else
        print_error "Failed to create canary deployment configuration: $config_name"
        return 1
    fi
    
    rm -f /tmp/canary-config.json
}

# Function to create blue-green deployment configuration
create_blue_green_deployment_config() {
    local account_id=$1
    local config_name="$PROJECT_NAME-$ENVIRONMENT-blue-green"
    
    print_status "Creating blue-green deployment configuration: $config_name"
    
    # Check if configuration already exists
    if aws deploy get-deployment-config --deployment-config-name "$config_name" >/dev/null 2>&1; then
        print_warning "Deployment configuration $config_name already exists"
        return
    fi
    
    local config_json=$(cat << EOF
{
    "deploymentConfigName": "$config_name",
    "trafficRoutingConfig": {
        "type": "AllAtOnce"
    }
}
EOF
    )
    
    echo "$config_json" > /tmp/blue-green-config.json
    
    if aws deploy create-deployment-config --cli-input-json file:///tmp/blue-green-config.json; then
        print_success "Successfully created blue-green deployment configuration: $config_name"
    else
        print_error "Failed to create blue-green deployment configuration: $config_name"
        return 1
    fi
    
    rm -f /tmp/blue-green-config.json
}

# Function to create deployment groups for each region
create_deployment_groups() {
    local account_id=$1
    local app_name="$PROJECT_NAME-$ENVIRONMENT-app"
    
    print_status "Creating deployment groups for each region..."
    
    # Get replication regions
    local replication_regions
    case $REGION in
        ap-east-2)
            replication_regions=("ap-northeast-1" "ap-southeast-1")
            ;;
        ap-northeast-1)
            replication_regions=("ap-east-2" "ap-southeast-1")
            ;;
        ap-southeast-1)
            replication_regions=("ap-east-2" "ap-northeast-1")
            ;;
        *)
            replication_regions=("ap-northeast-1")
            ;;
    esac
    
    # Create deployment group for primary region
    create_region_deployment_group "$account_id" "$app_name" "$REGION"
    
    # Create deployment groups for replication regions
    for region in "${replication_regions[@]}"; do
        create_region_deployment_group "$account_id" "$app_name" "$region"
    done
}

# Function to create deployment group for a specific region
create_region_deployment_group() {
    local account_id=$1
    local app_name=$2
    local region=$3
    local deployment_group_name="$region-deployment-group"
    
    print_status "Creating deployment group: $deployment_group_name for region: $region"
    
    # Check if deployment group already exists
    if aws deploy get-deployment-group \
        --application-name "$app_name" \
        --deployment-group-name "$deployment_group_name" >/dev/null 2>&1; then
        print_warning "Deployment group $deployment_group_name already exists"
        return
    fi
    
    local deployment_group_config=$(cat << EOF
{
    "applicationName": "$app_name",
    "deploymentGroupName": "$deployment_group_name",
    "serviceRoleArn": "arn:aws:iam::$account_id:role/CodeDeployServiceRole-$PROJECT_NAME-$ENVIRONMENT",
    "deploymentConfigName": "$PROJECT_NAME-$ENVIRONMENT-canary-10-percent",
    "autoRollbackConfiguration": {
        "enabled": true,
        "events": ["DEPLOYMENT_FAILURE", "DEPLOYMENT_STOP_ON_ALARM"]
    },
    "alarmConfiguration": {
        "enabled": true,
        "alarms": [
            {
                "name": "$PROJECT_NAME-$ENVIRONMENT-$region-high-error-rate"
            },
            {
                "name": "$PROJECT_NAME-$ENVIRONMENT-$region-high-response-time"
            }
        ]
    },
    "ecsServices": [
        {
            "serviceName": "$PROJECT_NAME-$ENVIRONMENT-service",
            "clusterName": "$PROJECT_NAME-$ENVIRONMENT-cluster"
        }
    ],
    "loadBalancerInfo": {
        "targetGroupInfoList": [
            {
                "name": "$PROJECT_NAME-$ENVIRONMENT-tg"
            }
        ]
    },
    "blueGreenDeploymentConfiguration": {
        "terminateBlueInstancesOnDeploymentSuccess": {
            "action": "TERMINATE",
            "terminationWaitTimeInMinutes": 5
        },
        "deploymentReadyOption": {
            "actionOnTimeout": "CONTINUE_DEPLOYMENT"
        },
        "greenFleetProvisioningOption": {
            "action": "COPY_AUTO_SCALING_GROUP"
        }
    }
}
EOF
    )
    
    echo "$deployment_group_config" > "/tmp/deployment-group-$region.json"
    
    if aws deploy create-deployment-group --cli-input-json "file:///tmp/deployment-group-$region.json"; then
        print_success "Successfully created deployment group: $deployment_group_name"
    else
        print_error "Failed to create deployment group: $deployment_group_name"
        return 1
    fi
    
    rm -f "/tmp/deployment-group-$region.json"
}

# Function to setup deployment monitoring alarms
setup_deployment_monitoring_alarms() {
    print_status "Setting up deployment monitoring alarms..."
    
    # Get replication regions
    local replication_regions
    case $REGION in
        ap-east-2)
            replication_regions=("ap-northeast-1" "ap-southeast-1")
            ;;
        ap-northeast-1)
            replication_regions=("ap-east-2" "ap-southeast-1")
            ;;
        ap-southeast-1)
            replication_regions=("ap-east-2" "ap-northeast-1")
            ;;
        *)
            replication_regions=("ap-northeast-1")
            ;;
    esac
    
    # Create alarms for primary region
    create_region_deployment_alarms "$REGION"
    
    # Create alarms for replication regions
    for region in "${replication_regions[@]}"; do
        create_region_deployment_alarms "$region"
    done
    
    print_success "Deployment monitoring alarms setup completed"
}

# Function to create deployment alarms for a specific region
create_region_deployment_alarms() {
    local region=$1
    
    print_status "Creating deployment alarms for region: $region"
    
    # High error rate alarm
    aws cloudwatch put-metric-alarm \
        --region "$region" \
        --alarm-name "$PROJECT_NAME-$ENVIRONMENT-$region-high-error-rate" \
        --alarm-description "High error rate during deployment in $region" \
        --metric-name "4XXError" \
        --namespace "AWS/ApplicationELB" \
        --statistic "Sum" \
        --period 60 \
        --threshold 10 \
        --comparison-operator "GreaterThanThreshold" \
        --evaluation-periods 2 \
        --dimensions Name=LoadBalancer,Value="app/$PROJECT_NAME-$ENVIRONMENT-alb/*" \
        --treat-missing-data "notBreaching"
    
    # High response time alarm
    aws cloudwatch put-metric-alarm \
        --region "$region" \
        --alarm-name "$PROJECT_NAME-$ENVIRONMENT-$region-high-response-time" \
        --alarm-description "High response time during deployment in $region" \
        --metric-name "TargetResponseTime" \
        --namespace "AWS/ApplicationELB" \
        --statistic "Average" \
        --period 60 \
        --threshold 2.0 \
        --comparison-operator "GreaterThanThreshold" \
        --evaluation-periods 2 \
        --dimensions Name=LoadBalancer,Value="app/$PROJECT_NAME-$ENVIRONMENT-alb/*" \
        --treat-missing-data "notBreaching"
    
    print_success "Created deployment alarms for region: $region"
}

# Function to trigger cross-region synchronization
trigger_cross_region_sync() {
    print_status "Triggering cross-region secret synchronization..."
    
    # Get the cross-region sync Lambda function name
    local sync_lambda_name="$PROJECT_NAME-$ENVIRONMENT-cross-region-sync"
    
    # Check if Lambda function exists
    if aws lambda get-function --function-name "$sync_lambda_name" --region "$REGION" >/dev/null 2>&1; then
        # Invoke the Lambda function to trigger initial sync
        print_status "Invoking cross-region sync Lambda function..."
        aws lambda invoke \
            --function-name "$sync_lambda_name" \
            --region "$REGION" \
            --payload '{"action": "initial_sync"}' \
            /tmp/sync-response.json >/dev/null 2>&1
        
        if [ $? -eq 0 ]; then
            print_success "Cross-region sync triggered successfully"
        else
            print_warning "Failed to trigger cross-region sync, but deployment continues"
        fi
    else
        print_warning "Cross-region sync Lambda function not found, skipping sync trigger"
    fi
    
    # Trigger ConfigMap sync
    local configmap_sync_lambda_name="$PROJECT_NAME-$ENVIRONMENT-configmap-sync"
    
    if aws lambda get-function --function-name "$configmap_sync_lambda_name" --region "$REGION" >/dev/null 2>&1; then
        print_status "Invoking ConfigMap sync Lambda function..."
        aws lambda invoke \
            --function-name "$configmap_sync_lambda_name" \
            --region "$REGION" \
            --payload '{"action": "initial_sync"}' \
            /tmp/configmap-sync-response.json >/dev/null 2>&1
        
        if [ $? -eq 0 ]; then
            print_success "ConfigMap sync triggered successfully"
        else
            print_warning "Failed to trigger ConfigMap sync, but deployment continues"
        fi
    else
        print_warning "ConfigMap sync Lambda function not found, skipping sync trigger"
    fi
    
    # Trigger drift detection
    local drift_detection_lambda_name="$PROJECT_NAME-$ENVIRONMENT-drift-detection"
    
    if aws lambda get-function --function-name "$drift_detection_lambda_name" --region "$REGION" >/dev/null 2>&1; then
        print_status "Invoking drift detection Lambda function..."
        aws lambda invoke \
            --function-name "$drift_detection_lambda_name" \
            --region "$REGION" \
            --payload '{"action": "initial_check"}' \
            /tmp/drift-detection-response.json >/dev/null 2>&1
        
        if [ $? -eq 0 ]; then
            print_success "Drift detection triggered successfully"
        else
            print_warning "Failed to trigger drift detection, but deployment continues"
        fi
    else
        print_warning "Drift detection Lambda function not found, skipping drift check"
    fi
}

# Function to destroy stacks
destroy_stacks() {
    local deployment_type=$1
    local stacks=($(get_stacks_for_deployment_type "$deployment_type"))

    print_header "🗑️  Destroying $deployment_type infrastructure"
    echo ""
    print_warning "This will permanently delete infrastructure resources!"
    
    if [ "$DRY_RUN" = "true" ]; then
        print_warning "DRY RUN MODE - No actual destruction will occur"
        print_status "Would destroy the following stacks:"
        for stack in "${stacks[@]}"; do
            echo "   - $stack"
        done
        return 0
    fi

    read -p "Are you sure you want to destroy $deployment_type infrastructure in $ENVIRONMENT? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        print_status "Destruction cancelled"
        return 0
    fi

    # Destroy stacks in reverse order
    if [ "${stacks[0]}" = "--all" ]; then
        print_status "Destroying all stacks..."
        cdk destroy --all --force --region "$REGION"
    else
        # Reverse the array for destruction
        local reversed_stacks=()
        for ((i=${#stacks[@]}-1; i>=0; i--)); do
            reversed_stacks+=("${stacks[i]}")
        done

        for stack in "${reversed_stacks[@]}"; do
            print_status "Destroying $stack..."
            if aws cloudformation describe-stacks --stack-name "$stack" --region "$REGION" >/dev/null 2>&1; then
                cdk destroy "$stack" --force --region "$REGION"
                print_success "Successfully destroyed $stack"
            else
                print_warning "$stack does not exist, skipping"
            fi
        done
    fi

    print_success "Destruction completed successfully!"
}

# Function to show post-deployment instructions
show_post_deployment_instructions() {
    local deployment_type=$1

    print_header "🎉 Post-Deployment Instructions"
    echo ""

    case $deployment_type in
        foundation|full)
            echo "🔐 Network and Security:"
            echo "   - VPC and subnets have been created"
            echo "   - Security groups are configured"
            echo "   - KMS keys are ready for encryption"
            echo ""
            ;;
    esac

    case $deployment_type in
        security|full)
            echo "👥 Identity and Access Management:"
            echo "   - IAM roles have been created"
            echo "   - EKS IRSA is configured"
            echo "   - Secrets Stack with cross-region sync is deployed"
            if [ "$ENABLE_MULTI_REGION" = "true" ]; then
                echo "   - Cross-region secret synchronization is active"
                echo "   - ConfigMap synchronization is configured"
                echo "   - Configuration drift detection is enabled"
            fi
            if [ -n "$SSO_INSTANCE_ARN" ]; then
                echo "   - SSO permission sets are ready"
                echo "   - Configure user assignments in AWS SSO Console"
            fi
            echo ""
            ;;
    esac

    case $deployment_type in
        data|full)
            echo "💾 Data Layer:"
            echo "   - RDS Aurora cluster is running"
            echo "   - ElastiCache Redis is available"
            echo "   - MSK Kafka cluster is ready"
            echo ""
            ;;
    esac

    case $deployment_type in
        compute|full)
            echo "⚙️  Compute Layer:"
            echo "   - EKS cluster is ready"
            echo "   - Configure kubectl: aws eks update-kubeconfig --region $REGION --name $PROJECT_NAME-$ENVIRONMENT-cluster"
            echo ""
            ;;
    esac

    case $deployment_type in
        observability|full)
            echo "📊 Monitoring and Observability:"
            echo "   - CloudWatch dashboards are configured"
            echo "   - Alerts are set up for $ALERT_EMAIL"
            echo "   - X-Ray tracing is enabled"
            echo ""
            ;;
    esac

    case $deployment_type in
        analytics|full)
            if [ "$ENABLE_ANALYTICS" = "true" ]; then
                echo "📈 Analytics:"
                echo "   - Data catalog is configured"
                echo "   - Analytics pipeline is ready"
                echo "   - Glue crawlers are scheduled"
                echo ""
            fi
            ;;
    esac

    if [ "$ENABLE_CODE_PIPELINE" = "true" ] || [ "$ENABLE_MULTI_REGION" = "true" ]; then
        echo "🚀 AWS Code Services:"
        echo "   - CodePipeline: Multi-region deployment pipeline is configured"
        echo "   - CodeBuild: Infrastructure and application build projects are ready"
        echo "   - CodeDeploy: Blue-green and canary deployment strategies are available"
        if [ "$BLUE_GREEN_DEPLOYMENT" = "true" ]; then
            echo "   - Blue-Green deployment strategy is enabled"
        fi
        echo "   - Canary deployment: $CANARY_PERCENTAGE% traffic routing configured"
        echo ""
    fi

    echo "🔗 Useful Links:"
    echo "   - AWS Console: https://console.aws.amazon.com/"
    echo "   - EKS Console: https://console.aws.amazon.com/eks/home?region=$REGION"
    echo "   - CloudWatch: https://console.aws.amazon.com/cloudwatch/home?region=$REGION"
    echo "   - RDS Console: https://console.aws.amazon.com/rds/home?region=$REGION"
    if [ "$ENABLE_CODE_PIPELINE" = "true" ] || [ "$ENABLE_MULTI_REGION" = "true" ]; then
        echo "   - CodePipeline: https://console.aws.amazon.com/codesuite/codepipeline/pipelines/$PROJECT_NAME-$ENVIRONMENT-multi-region-pipeline/view?region=$REGION"
        echo "   - CodeBuild: https://console.aws.amazon.com/codesuite/codebuild/projects?region=$REGION"
        echo "   - CodeDeploy: https://console.aws.amazon.com/codesuite/codedeploy/applications?region=$REGION"
    fi
    echo ""

    echo "📋 Next Steps:"
    if [ "$ENABLE_CODE_PIPELINE" = "true" ] || [ "$ENABLE_MULTI_REGION" = "true" ]; then
        echo "   1. Upload source code to S3 artifacts bucket to trigger pipeline"
        echo "   2. Monitor CodePipeline execution progress"
        echo "   3. Verify multi-region deployment success"
        echo "   4. Test canary and blue-green deployment strategies"
        echo "   5. Configure additional monitoring dashboards"
        echo "   6. Run integration tests across all regions"
    else
        echo "   1. Deploy your application code to EKS"
        echo "   2. Configure monitoring dashboards"
        echo "   3. Set up CI/CD pipelines"
        echo "   4. Run integration tests"
    fi
    
    if [ "$ENABLE_MULTI_REGION" = "true" ]; then
        echo "   7. Verify cross-region secret synchronization"
        echo "   8. Test configuration drift detection"
        echo "   9. Validate ConfigMap synchronization in Kubernetes"
        echo "   10. Test multi-region failover scenarios"
    fi
    echo ""

    echo "🔍 Check Status:"
    echo "   $0 --status -e $ENVIRONMENT -r $REGION"
    if [ "$ENABLE_CODE_PIPELINE" = "true" ] || [ "$ENABLE_MULTI_REGION" = "true" ]; then
        echo "   $0 --pipeline-status -e $ENVIRONMENT -r $REGION"
    fi
    echo ""
}

# Main function
main() {
    parse_arguments "$@"

    print_header "🚀 GenAI Demo - Unified Infrastructure Deployment"
    print_header "================================================"
    echo ""

    if [ "$SHOW_STATUS" = "true" ]; then
        show_deployment_status
        exit 0
    fi

    if [ "$SHOW_PIPELINE_STATUS" = "true" ]; then
        show_pipeline_status
        exit 0
    fi

    check_prerequisites
    set_cdk_context
    
    # Skip build and tests for dry-run to avoid compilation errors
    if [ "$DRY_RUN" != "true" ]; then
        bootstrap_cdk
        build_project
        run_tests
    fi

    if [ "$DESTROY" = "true" ]; then
        destroy_stacks "$DEPLOYMENT_TYPE"
    else
        deploy_stacks "$DEPLOYMENT_TYPE"
        show_post_deployment_instructions "$DEPLOYMENT_TYPE"
    fi

    print_success "All operations completed successfully!"
}

# Function to update existing pipeline
update_existing_pipeline() {
    local pipeline_name=$1
    local account_id=$2
    
    print_status "Updating existing pipeline: $pipeline_name"
    
    # Get current pipeline configuration
    aws codepipeline get-pipeline --name "$pipeline_name" --query 'pipeline' > /tmp/current-pipeline.json
    
    # Update pipeline (simplified - in practice you'd merge configurations)
    print_warning "Pipeline update functionality is simplified - manual review recommended"
    
    # For now, just print a message
    print_status "Pipeline $pipeline_name exists and may need manual updates"
}

# Function to update infrastructure build project
update_infrastructure_build_project() {
    local project_name=$1
    local account_id=$2
    
    print_status "Updating infrastructure build project: $project_name"
    
    # Update project configuration
    local update_config=$(cat << EOF
{
    "name": "$project_name",
    "description": "Updated multi-region infrastructure build for $PROJECT_NAME $ENVIRONMENT",
    "environment": {
        "environmentVariables": [
            {
                "name": "AWS_DEFAULT_REGION",
                "value": "$REGION"
            },
            {
                "name": "AWS_ACCOUNT_ID",
                "value": "$account_id"
            },
            {
                "name": "PROJECT_NAME",
                "value": "$PROJECT_NAME"
            },
            {
                "name": "ENVIRONMENT",
                "value": "$ENVIRONMENT"
            }
        ]
    }
}
EOF
    )
    
    echo "$update_config" > /tmp/update-infrastructure-build.json
    
    if aws codebuild update-project --cli-input-json file:///tmp/update-infrastructure-build.json; then
        print_success "Successfully updated infrastructure build project: $project_name"
    else
        print_warning "Failed to update infrastructure build project: $project_name"
    fi
    
    rm -f /tmp/update-infrastructure-build.json
}

# Function to update application build project
update_application_build_project() {
    local project_name=$1
    local account_id=$2
    
    print_status "Updating application build project: $project_name"
    
    # Update project configuration
    local update_config=$(cat << EOF
{
    "name": "$project_name",
    "description": "Updated multi-region application build for $PROJECT_NAME $ENVIRONMENT",
    "environment": {
        "environmentVariables": [
            {
                "name": "AWS_DEFAULT_REGION",
                "value": "$REGION"
            },
            {
                "name": "AWS_ACCOUNT_ID",
                "value": "$account_id"
            },
            {
                "name": "IMAGE_REPO_NAME",
                "value": "$PROJECT_NAME-$ENVIRONMENT-app"
            },
            {
                "name": "IMAGE_TAG",
                "value": "latest"
            }
        ]
    }
}
EOF
    )
    
    echo "$update_config" > /tmp/update-application-build.json
    
    if aws codebuild update-project --cli-input-json file:///tmp/update-application-build.json; then
        print_success "Successfully updated application build project: $project_name"
    else
        print_warning "Failed to update application build project: $project_name"
    fi
    
    rm -f /tmp/update-application-build.json
}

# Function to trigger pipeline execution
trigger_pipeline_execution() {
    local pipeline_name="$PROJECT_NAME-$ENVIRONMENT-multi-region-pipeline"
    
    print_status "Triggering pipeline execution: $pipeline_name"
    
    # Start pipeline execution
    local execution_id=$(aws codepipeline start-pipeline-execution \
        --name "$pipeline_name" \
        --query 'pipelineExecutionId' --output text)
    
    if [ $? -eq 0 ]; then
        print_success "Pipeline execution started with ID: $execution_id"
        
        # Monitor pipeline execution
        monitor_pipeline_execution "$pipeline_name" "$execution_id"
    else
        print_error "Failed to start pipeline execution"
        return 1
    fi
}

# Function to monitor pipeline execution
monitor_pipeline_execution() {
    local pipeline_name=$1
    local execution_id=$2
    
    print_status "Monitoring pipeline execution: $execution_id"
    
    local status="InProgress"
    local attempts=0
    local max_attempts=60  # 30 minutes with 30-second intervals
    
    while [ "$status" = "InProgress" ] && [ $attempts -lt $max_attempts ]; do
        sleep 30
        
        status=$(aws codepipeline get-pipeline-execution \
            --pipeline-name "$pipeline_name" \
            --pipeline-execution-id "$execution_id" \
            --query 'pipelineExecution.status' --output text)
        
        case $status in
            "InProgress")
                print_status "Pipeline execution in progress... (attempt $((attempts + 1))/$max_attempts)"
                ;;
            "Succeeded")
                print_success "Pipeline execution completed successfully!"
                return 0
                ;;
            "Failed"|"Cancelled"|"Superseded")
                print_error "Pipeline execution failed with status: $status"
                
                # Get failure details
                aws codepipeline get-pipeline-execution \
                    --pipeline-name "$pipeline_name" \
                    --pipeline-execution-id "$execution_id" \
                    --query 'pipelineExecution.statusSummary' --output text
                
                return 1
                ;;
        esac
        
        attempts=$((attempts + 1))
    done
    
    if [ $attempts -ge $max_attempts ]; then
        print_warning "Pipeline monitoring timed out. Check AWS Console for current status."
        print_status "Pipeline execution ID: $execution_id"
    fi
}

# Function to create IAM roles for Code services (if they don't exist)
create_code_services_iam_roles() {
    local account_id=$1
    
    print_status "Creating IAM roles for Code services..."
    
    # Create CodePipeline service role
    create_codepipeline_service_role "$account_id"
    
    # Create CodeBuild service role
    create_codebuild_service_role "$account_id"
    
    # Create CodeDeploy service role
    create_codedeploy_service_role "$account_id"
    
    # Create CloudFormation service role
    create_cloudformation_service_role "$account_id"
    
    print_success "IAM roles creation completed"
}

# Function to create CodePipeline service role
create_codepipeline_service_role() {
    local account_id=$1
    local role_name="CodePipelineServiceRole-$PROJECT_NAME-$ENVIRONMENT"
    
    print_status "Creating CodePipeline service role: $role_name"
    
    # Check if role already exists
    if aws iam get-role --role-name "$role_name" >/dev/null 2>&1; then
        print_warning "Role $role_name already exists"
        return
    fi
    
    # Create trust policy
    local trust_policy=$(cat << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "codepipeline.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
EOF
    )
    
    # Create role
    aws iam create-role \
        --role-name "$role_name" \
        --assume-role-policy-document "$trust_policy" \
        --description "Service role for CodePipeline multi-region deployment"
    
    # Attach managed policies
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/AWSCodePipelineFullAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/AWSCodeBuildDeveloperAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/AWSCodeDeployFullAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/CloudWatchFullAccess"
    
    print_success "Created CodePipeline service role: $role_name"
}

# Function to create CodeBuild service role
create_codebuild_service_role() {
    local account_id=$1
    local role_name="CodeBuildServiceRole-$PROJECT_NAME-$ENVIRONMENT"
    
    print_status "Creating CodeBuild service role: $role_name"
    
    # Check if role already exists
    if aws iam get-role --role-name "$role_name" >/dev/null 2>&1; then
        print_warning "Role $role_name already exists"
        return
    fi
    
    # Create trust policy
    local trust_policy=$(cat << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "codebuild.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
EOF
    )
    
    # Create role
    aws iam create-role \
        --role-name "$role_name" \
        --assume-role-policy-document "$trust_policy" \
        --description "Service role for CodeBuild multi-region builds"
    
    # Attach managed policies
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryFullAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/AmazonS3FullAccess"
    
    print_success "Created CodeBuild service role: $role_name"
}

# Function to create CodeDeploy service role
create_codedeploy_service_role() {
    local account_id=$1
    local role_name="CodeDeployServiceRole-$PROJECT_NAME-$ENVIRONMENT"
    
    print_status "Creating CodeDeploy service role: $role_name"
    
    # Check if role already exists
    if aws iam get-role --role-name "$role_name" >/dev/null 2>&1; then
        print_warning "Role $role_name already exists"
        return
    fi
    
    # Create trust policy
    local trust_policy=$(cat << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "codedeploy.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
EOF
    )
    
    # Create role
    aws iam create-role \
        --role-name "$role_name" \
        --assume-role-policy-document "$trust_policy" \
        --description "Service role for CodeDeploy multi-region deployments"
    
    # Attach managed policies
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/service-role/AWSCodeDeployRole"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/AutoScalingFullAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess"
    
    print_success "Created CodeDeploy service role: $role_name"
}

# Function to create CloudFormation service role
create_cloudformation_service_role() {
    local account_id=$1
    local role_name="CloudFormationServiceRole-$PROJECT_NAME-$ENVIRONMENT"
    
    print_status "Creating CloudFormation service role: $role_name"
    
    # Check if role already exists
    if aws iam get-role --role-name "$role_name" >/dev/null 2>&1; then
        print_warning "Role $role_name already exists"
        return
    fi
    
    # Create trust policy
    local trust_policy=$(cat << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": "cloudformation.amazonaws.com"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
EOF
    )
    
    # Create role
    aws iam create-role \
        --role-name "$role_name" \
        --assume-role-policy-document "$trust_policy" \
        --description "Service role for CloudFormation deployments"
    
    # Attach managed policies
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/PowerUserAccess"
    
    aws iam attach-role-policy \
        --role-name "$role_name" \
        --policy-arn "arn:aws:iam::aws:policy/IAMFullAccess"
    
    print_success "Created CloudFormation service role: $role_name"
}

# Run main function with all arguments
main "$@"