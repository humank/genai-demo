#!/bin/bash

# Test script for cross-region configuration management
# This script validates the cross-region secret synchronization functionality

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="genai-demo"
ENVIRONMENT="development"
PRIMARY_REGION="ap-east-2"
REPLICATION_REGIONS=("ap-northeast-1" "ap-southeast-1")

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
    echo -e "${BLUE}$1${NC}"
}

# Function to test secret synchronization
test_secret_sync() {
    local secret_name="$1"
    local test_value="test-$(date +%s)"
    
    print_status "Testing secret synchronization for: $secret_name"
    
    # Update secret in primary region
    print_status "Updating secret in primary region ($PRIMARY_REGION)..."
    aws secretsmanager update-secret \
        --secret-id "$secret_name" \
        --secret-string "{\"test_key\": \"$test_value\"}" \
        --region "$PRIMARY_REGION" >/dev/null 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "Secret updated in primary region"
    else
        print_error "Failed to update secret in primary region"
        return 1
    fi
    
    # Wait for synchronization
    print_status "Waiting for cross-region synchronization (30 seconds)..."
    sleep 30
    
    # Check replication regions
    local sync_success=true
    for region in "${REPLICATION_REGIONS[@]}"; do
        print_status "Checking secret in region: $region"
        
        local replica_value
        replica_value=$(aws secretsmanager get-secret-value \
            --secret-id "$secret_name" \
            --region "$region" \
            --query 'SecretString' \
            --output text 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            if echo "$replica_value" | grep -q "$test_value"; then
                print_success "Secret synchronized successfully in $region"
            else
                print_error "Secret value mismatch in $region"
                sync_success=false
            fi
        else
            print_error "Failed to retrieve secret from $region"
            sync_success=false
        fi
    done
    
    if [ "$sync_success" = true ]; then
        print_success "Cross-region synchronization test passed for $secret_name"
        return 0
    else
        print_error "Cross-region synchronization test failed for $secret_name"
        return 1
    fi
}

# Function to test Lambda functions
test_lambda_functions() {
    print_header "Testing Lambda Functions"
    
    local functions=(
        "$PROJECT_NAME-$ENVIRONMENT-cross-region-sync"
        "$PROJECT_NAME-$ENVIRONMENT-configmap-sync"
        "$PROJECT_NAME-$ENVIRONMENT-drift-detection"
    )
    
    for func in "${functions[@]}"; do
        print_status "Testing Lambda function: $func"
        
        if aws lambda get-function --function-name "$func" --region "$PRIMARY_REGION" >/dev/null 2>&1; then
            print_success "Lambda function $func exists"
            
            # Test function invocation
            print_status "Testing function invocation..."
            local response
            response=$(aws lambda invoke \
                --function-name "$func" \
                --region "$PRIMARY_REGION" \
                --payload '{"test": true}' \
                /tmp/lambda-response.json 2>&1)
            
            if [ $? -eq 0 ]; then
                print_success "Lambda function $func invoked successfully"
            else
                print_warning "Lambda function $func invocation failed: $response"
            fi
        else
            print_error "Lambda function $func not found"
        fi
    done
}

# Function to test EventBridge rules
test_eventbridge_rules() {
    print_header "Testing EventBridge Rules"
    
    local rule_name="$PROJECT_NAME-$ENVIRONMENT-secrets-events"
    
    print_status "Checking EventBridge rule: $rule_name"
    
    if aws events describe-rule --name "$rule_name" --region "$PRIMARY_REGION" >/dev/null 2>&1; then
        print_success "EventBridge rule $rule_name exists"
        
        # Check rule status
        local rule_state
        rule_state=$(aws events describe-rule \
            --name "$rule_name" \
            --region "$PRIMARY_REGION" \
            --query 'State' \
            --output text)
        
        if [ "$rule_state" = "ENABLED" ]; then
            print_success "EventBridge rule is enabled"
        else
            print_warning "EventBridge rule is not enabled: $rule_state"
        fi
    else
        print_error "EventBridge rule $rule_name not found"
    fi
}

# Function to test Parameter Store configuration
test_parameter_store() {
    print_header "Testing Parameter Store Configuration"
    
    local parameters=(
        "/genai-demo/$ENVIRONMENT/global/secrets/cross-region-config"
        "/genai-demo/$ENVIRONMENT/global/secrets/gitops-config"
        "/genai-demo/$ENVIRONMENT/global/secrets/configmap-sync-config"
        "/genai-demo/$ENVIRONMENT/global/secrets/drift-detection-config"
    )
    
    for param in "${parameters[@]}"; do
        print_status "Checking parameter: $param"
        
        if aws ssm get-parameter --name "$param" --region "$PRIMARY_REGION" >/dev/null 2>&1; then
            print_success "Parameter $param exists"
        else
            print_error "Parameter $param not found"
        fi
    done
}

# Function to test drift detection
test_drift_detection() {
    print_header "Testing Configuration Drift Detection"
    
    local drift_lambda="$PROJECT_NAME-$ENVIRONMENT-drift-detection"
    
    print_status "Triggering drift detection..."
    
    local response
    response=$(aws lambda invoke \
        --function-name "$drift_lambda" \
        --region "$PRIMARY_REGION" \
        --payload '{"action": "test_drift_detection"}' \
        /tmp/drift-response.json 2>&1)
    
    if [ $? -eq 0 ]; then
        print_success "Drift detection triggered successfully"
        
        # Check response
        if [ -f /tmp/drift-response.json ]; then
            local status_code
            status_code=$(cat /tmp/drift-response.json | jq -r '.statusCode' 2>/dev/null)
            
            if [ "$status_code" = "200" ]; then
                print_success "Drift detection completed successfully"
            else
                print_warning "Drift detection returned status: $status_code"
            fi
        fi
    else
        print_error "Failed to trigger drift detection: $response"
    fi
}

# Function to generate test report
generate_test_report() {
    print_header "Generating Test Report"
    
    local report_file="/tmp/cross-region-config-test-report.json"
    
    cat > "$report_file" << EOF
{
    "test_timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "project_name": "$PROJECT_NAME",
    "environment": "$ENVIRONMENT",
    "primary_region": "$PRIMARY_REGION",
    "replication_regions": $(printf '%s\n' "${REPLICATION_REGIONS[@]}" | jq -R . | jq -s .),
    "test_results": {
        "secret_sync_test": "$secret_sync_result",
        "lambda_functions_test": "$lambda_test_result",
        "eventbridge_rules_test": "$eventbridge_test_result",
        "parameter_store_test": "$parameter_test_result",
        "drift_detection_test": "$drift_test_result"
    },
    "recommendations": [
        "Monitor CloudWatch metrics for cross-region sync performance",
        "Set up alerts for configuration drift detection",
        "Regularly test failover scenarios",
        "Validate ConfigMap synchronization in Kubernetes clusters"
    ]
}
EOF
    
    print_success "Test report generated: $report_file"
    cat "$report_file"
}

# Main test execution
main() {
    print_header "🧪 Cross-Region Configuration Management Test Suite"
    print_header "=================================================="
    echo ""
    
    print_status "Project: $PROJECT_NAME"
    print_status "Environment: $ENVIRONMENT"
    print_status "Primary Region: $PRIMARY_REGION"
    print_status "Replication Regions: ${REPLICATION_REGIONS[*]}"
    echo ""
    
    # Initialize test results
    secret_sync_result="PASS"
    lambda_test_result="PASS"
    eventbridge_test_result="PASS"
    parameter_test_result="PASS"
    drift_test_result="PASS"
    
    # Test Lambda functions
    if ! test_lambda_functions; then
        lambda_test_result="FAIL"
    fi
    echo ""
    
    # Test EventBridge rules
    if ! test_eventbridge_rules; then
        eventbridge_test_result="FAIL"
    fi
    echo ""
    
    # Test Parameter Store
    if ! test_parameter_store; then
        parameter_test_result="FAIL"
    fi
    echo ""
    
    # Test secret synchronization
    local test_secrets=(
        "$ENVIRONMENT/genai-demo/application"
    )
    
    for secret in "${test_secrets[@]}"; do
        if ! test_secret_sync "$secret"; then
            secret_sync_result="FAIL"
        fi
    done
    echo ""
    
    # Test drift detection
    if ! test_drift_detection; then
        drift_test_result="FAIL"
    fi
    echo ""
    
    # Generate test report
    generate_test_report
    echo ""
    
    # Summary
    print_header "Test Summary"
    echo "Secret Sync: $secret_sync_result"
    echo "Lambda Functions: $lambda_test_result"
    echo "EventBridge Rules: $eventbridge_test_result"
    echo "Parameter Store: $parameter_test_result"
    echo "Drift Detection: $drift_test_result"
    echo ""
    
    if [[ "$secret_sync_result" == "PASS" && "$lambda_test_result" == "PASS" && 
          "$eventbridge_test_result" == "PASS" && "$parameter_test_result" == "PASS" && 
          "$drift_test_result" == "PASS" ]]; then
        print_success "All tests passed! Cross-region configuration management is working correctly."
        exit 0
    else
        print_error "Some tests failed. Please check the logs and fix the issues."
        exit 1
    fi
}

# Check prerequisites
if ! command -v aws &> /dev/null; then
    print_error "AWS CLI is not installed or not in PATH"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    print_error "jq is not installed or not in PATH"
    exit 1
fi

# Run main function
main "$@"