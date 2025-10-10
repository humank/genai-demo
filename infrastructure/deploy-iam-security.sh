#!/bin/bash

# Deploy IAM Fine-grained Access Control - Task 10 Implementation
# This script deploys the IAM, SSO, and EKS IRSA stacks for enhanced security

set -e

# Configuration
PROJECT_NAME="genai-demo"
ENVIRONMENT=${1:-"development"}
REGION=${2:-"ap-east-2"}
SSO_INSTANCE_ARN=${3:-""}

echo "🚀 Starting IAM Fine-grained Access Control Deployment"
echo "   Project: $PROJECT_NAME"
echo "   Environment: $ENVIRONMENT"
echo "   Region: $REGION"
echo "   SSO Instance ARN: ${SSO_INSTANCE_ARN:-"Not provided - will use CDK context"}"
echo ""

# Function to check if stack exists
check_stack_exists() {
    local stack_name=$1
    aws cloudformation describe-stacks --stack-name "$stack_name" --region "$REGION" >/dev/null 2>&1
}

# Function to deploy stack with error handling
deploy_stack() {
    local stack_name=$1
    local description=$2
    
    echo "📦 Deploying $description..."
    echo "   Stack: $stack_name"
    
    if cdk deploy "$stack_name" --require-approval never --region "$REGION"; then
        echo "✅ Successfully deployed $description"
    else
        echo "❌ Failed to deploy $description"
        exit 1
    fi
    echo ""
}

# Function to check prerequisites
check_prerequisites() {
    echo "🔍 Checking prerequisites..."
    
    # Check if CDK is installed
    if ! command -v cdk &> /dev/null; then
        echo "❌ AWS CDK is not installed. Please install it first:"
        echo "   npm install -g aws-cdk"
        exit 1
    fi
    
    # Check if AWS CLI is configured
    if ! aws sts get-caller-identity >/dev/null 2>&1; then
        echo "❌ AWS CLI is not configured. Please configure it first:"
        echo "   aws configure"
        exit 1
    fi
    
    # Check if we're in the right directory
    if [ ! -f "cdk.json" ]; then
        echo "❌ Not in CDK project directory. Please run from infrastructure/ directory."
        exit 1
    fi
    
    echo "✅ Prerequisites check passed"
    echo ""
}

# Function to set CDK context
set_cdk_context() {
    echo "⚙️  Setting CDK context..."
    
    # Set basic context
    cdk context --set "environment=$ENVIRONMENT"
    cdk context --set "region=$REGION"
    cdk context --set "projectName=$PROJECT_NAME"
    
    # Set SSO context if provided
    if [ -n "$SSO_INSTANCE_ARN" ]; then
        cdk context --set "ssoInstanceArn=$SSO_INSTANCE_ARN"
        echo "   SSO Instance ARN set in context"
    else
        echo "   ⚠️  SSO Instance ARN not provided - SSO stack may fail without it"
        echo "   You can provide it as: $0 $ENVIRONMENT $REGION <SSO_INSTANCE_ARN>"
    fi
    
    echo "✅ CDK context configured"
    echo ""
}

# Function to bootstrap CDK if needed
bootstrap_cdk() {
    echo "🔧 Checking CDK bootstrap..."
    
    if ! check_stack_exists "CDKToolkit"; then
        echo "📦 Bootstrapping CDK..."
        cdk bootstrap --region "$REGION"
        echo "✅ CDK bootstrap completed"
    else
        echo "✅ CDK already bootstrapped"
    fi
    echo ""
}

# Function to build TypeScript
build_project() {
    echo "🔨 Building TypeScript project..."
    
    if npm run build; then
        echo "✅ Build successful"
    else
        echo "❌ Build failed"
        exit 1
    fi
    echo ""
}

# Function to validate stacks
validate_stacks() {
    echo "🔍 Validating CDK stacks..."
    
    local stacks=(
        "${PROJECT_NAME}-${ENVIRONMENT}-iam"
        "${PROJECT_NAME}-${ENVIRONMENT}-sso"
        "${PROJECT_NAME}-${ENVIRONMENT}-eks-irsa"
    )
    
    for stack in "${stacks[@]}"; do
        if cdk synth "$stack" >/dev/null 2>&1; then
            echo "✅ $stack - Valid"
        else
            echo "❌ $stack - Invalid"
            echo "   Run 'cdk synth $stack' for details"
            exit 1
        fi
    done
    
    echo "✅ All stacks validated successfully"
    echo ""
}

# Main deployment function
deploy_iam_security() {
    echo "🚀 Starting IAM Security Stack Deployment..."
    echo ""
    
    # Deploy stacks in dependency order
    
    # 1. Deploy IAM Stack first (provides roles)
    deploy_stack "${PROJECT_NAME}-${ENVIRONMENT}-iam" "IAM Fine-grained Access Control Stack"
    
    # 2. Deploy SSO Stack (can be deployed independently)
    if [ -n "$SSO_INSTANCE_ARN" ] || cdk context --get ssoInstanceArn >/dev/null 2>&1; then
        deploy_stack "${PROJECT_NAME}-${ENVIRONMENT}-sso" "AWS SSO Integration Stack"
    else
        echo "⚠️  Skipping SSO Stack deployment - SSO Instance ARN not provided"
        echo "   To deploy SSO stack later, provide SSO Instance ARN:"
        echo "   $0 $ENVIRONMENT $REGION <SSO_INSTANCE_ARN>"
        echo ""
    fi
    
    # 3. Deploy EKS IRSA Stack (depends on IAM Stack and existing EKS Stack)
    if check_stack_exists "${PROJECT_NAME}-${ENVIRONMENT}-eks"; then
        deploy_stack "${PROJECT_NAME}-${ENVIRONMENT}-eks-irsa" "EKS IRSA Configuration Stack"
    else
        echo "⚠️  EKS Stack not found - skipping EKS IRSA deployment"
        echo "   Deploy EKS Stack first, then run this script again"
        echo ""
    fi
}

# Function to display post-deployment instructions
show_post_deployment_instructions() {
    echo "🎉 IAM Fine-grained Access Control Deployment Complete!"
    echo ""
    echo "📋 Post-deployment Steps:"
    echo ""
    echo "1. 🔐 Verify IAM Roles:"
    echo "   aws iam list-roles --query 'Roles[?contains(RoleName, \`${PROJECT_NAME}-${ENVIRONMENT}\`)].RoleName' --output table"
    echo ""
    echo "2. 🎯 Check Service Accounts (if EKS IRSA deployed):"
    echo "   kubectl get serviceaccounts -A | grep ${PROJECT_NAME}"
    echo ""
    echo "3. 🔍 Verify IRSA Configuration:"
    echo "   kubectl describe serviceaccount ${PROJECT_NAME}-app-sa -n application"
    echo ""
    
    if [ -n "$SSO_INSTANCE_ARN" ] || cdk context --get ssoInstanceArn >/dev/null 2>&1; then
        echo "4. 👥 Configure SSO Account Assignments:"
        echo "   - Go to AWS SSO Console"
        echo "   - Assign users/groups to permission sets"
        echo "   - Or use AWS CLI:"
        echo "   aws sso-admin create-account-assignment \\"
        echo "     --instance-arn <SSO_INSTANCE_ARN> \\"
        echo "     --target-id $(aws sts get-caller-identity --query Account --output text) \\"
        echo "     --target-type AWS_ACCOUNT \\"
        echo "     --permission-set-arn <PERMISSION_SET_ARN> \\"
        echo "     --principal-type USER \\"
        echo "     --principal-id <USER_ID>"
        echo ""
    fi
    
    echo "5. 🧪 Test Access Control:"
    echo "   - Deploy a test application using the service accounts"
    echo "   - Verify AWS service access works correctly"
    echo "   - Check CloudWatch logs for any permission issues"
    echo ""
    echo "6. 📊 Monitor Security:"
    echo "   - Check CloudTrail for IAM role usage"
    echo "   - Monitor CloudWatch for authentication events"
    echo "   - Review AWS Config for compliance"
    echo ""
    echo "🔗 Useful Links:"
    echo "   - IAM Console: https://console.aws.amazon.com/iam/"
    echo "   - SSO Console: https://console.aws.amazon.com/singlesignon/"
    echo "   - EKS Console: https://console.aws.amazon.com/eks/home?region=${REGION}#/clusters"
    echo ""
}

# Function to handle cleanup on failure
cleanup_on_failure() {
    echo ""
    echo "❌ Deployment failed. Check the error messages above."
    echo ""
    echo "🔧 Troubleshooting Tips:"
    echo "1. Check AWS credentials and permissions"
    echo "2. Verify CDK context settings: cdk context --list"
    echo "3. Check stack dependencies are deployed"
    echo "4. Review CloudFormation events in AWS Console"
    echo ""
    echo "🗑️  To clean up partial deployment:"
    echo "   cdk destroy ${PROJECT_NAME}-${ENVIRONMENT}-eks-irsa"
    echo "   cdk destroy ${PROJECT_NAME}-${ENVIRONMENT}-sso"
    echo "   cdk destroy ${PROJECT_NAME}-${ENVIRONMENT}-iam"
    echo ""
}

# Set up error handling
trap cleanup_on_failure ERR

# Main execution
main() {
    echo "🔐 IAM Fine-grained Access Control Deployment Script"
    echo "=================================================="
    echo ""
    
    check_prerequisites
    set_cdk_context
    bootstrap_cdk
    build_project
    validate_stacks
    deploy_iam_security
    show_post_deployment_instructions
    
    echo "✅ All operations completed successfully!"
}

# Run main function
main "$@"