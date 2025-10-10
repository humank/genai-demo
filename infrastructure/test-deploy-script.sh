#!/bin/bash

# Test script for deploy-unified.sh AWS Code Services functionality
# This script tests the new multi-region deployment features

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# Test 1: Check if script exists and is executable
test_script_exists() {
    print_test "Checking if deploy-unified.sh exists and is executable"
    
    if [ -f "./deploy-unified.sh" ] && [ -x "./deploy-unified.sh" ]; then
        print_pass "deploy-unified.sh exists and is executable"
        return 0
    else
        print_fail "deploy-unified.sh not found or not executable"
        return 1
    fi
}

# Test 2: Test help message includes new options
test_help_message() {
    print_test "Testing help message includes new AWS Code Services options"
    
    local help_output=$(./deploy-unified.sh --help 2>&1)
    
    if echo "$help_output" | grep -q "enable-code-pipeline" && \
       echo "$help_output" | grep -q "canary-percentage" && \
       echo "$help_output" | grep -q "blue-green" && \
       echo "$help_output" | grep -q "pipeline-status"; then
        print_pass "Help message includes new AWS Code Services options"
        return 0
    else
        print_fail "Help message missing new options"
        echo "Expected options: --enable-code-pipeline, --canary-percentage, --blue-green, --pipeline-status"
        return 1
    fi
}

# Test 3: Test dry-run with multi-region and code pipeline
test_dry_run_multi_region() {
    print_test "Testing dry-run with multi-region and CodePipeline enabled"
    
    # This should not fail and should show what would be deployed
    if ./deploy-unified.sh full --enable-multi-region --enable-code-pipeline --dry-run -e development -r ap-east-2 2>&1 | grep -q "DRY RUN MODE"; then
        print_pass "Dry-run with multi-region and CodePipeline works"
        return 0
    else
        print_fail "Dry-run with multi-region and CodePipeline failed"
        return 1
    fi
}

# Test 4: Test parameter parsing
test_parameter_parsing() {
    print_test "Testing parameter parsing for new options"
    
    # Test canary percentage parsing
    local output=$(./deploy-unified.sh full --canary-percentage 25 --dry-run -e development 2>&1)
    
    if echo "$output" | grep -q "DRY RUN MODE"; then
        print_pass "Parameter parsing works correctly"
        return 0
    else
        print_fail "Parameter parsing failed"
        return 1
    fi
}

# Test 5: Test pipeline status (should handle non-existent pipeline gracefully)
test_pipeline_status() {
    print_test "Testing pipeline status command"
    
    # This should handle non-existent pipeline gracefully
    local status_output=$(./deploy-unified.sh --pipeline-status -e development -r ap-east-2 2>&1)
    
    if echo "$status_output" | grep -q "Pipeline.*does not exist" || echo "$status_output" | grep -q "Pipeline Status"; then
        print_pass "Pipeline status command works (handles non-existent pipeline)"
        return 0
    else
        print_fail "Pipeline status command failed"
        echo "Output: $status_output"
        return 1
    fi
}

# Test 6: Test function definitions exist
test_function_definitions() {
    print_test "Testing that new function definitions exist in script"
    
    local functions_to_check=(
        "setup_aws_code_services_pipeline"
        "create_multi_region_codepipeline"
        "setup_multi_region_codebuild"
        "configure_multi_region_codedeploy"
        "show_pipeline_status"
        "create_code_services_iam_roles"
    )
    
    local missing_functions=()
    
    for func in "${functions_to_check[@]}"; do
        if ! grep -q "^$func()" "./deploy-unified.sh"; then
            missing_functions+=("$func")
        fi
    done
    
    if [ ${#missing_functions[@]} -eq 0 ]; then
        print_pass "All required function definitions found"
        return 0
    else
        print_fail "Missing function definitions: ${missing_functions[*]}"
        return 1
    fi
}

# Test 7: Test AWS CLI availability (prerequisite)
test_aws_cli() {
    print_test "Testing AWS CLI availability"
    
    if command -v aws &> /dev/null; then
        print_pass "AWS CLI is available"
        
        # Test if AWS credentials are configured
        if aws sts get-caller-identity >/dev/null 2>&1; then
            print_pass "AWS credentials are configured"
            return 0
        else
            print_info "AWS credentials not configured (expected for testing)"
            return 0
        fi
    else
        print_fail "AWS CLI not found"
        return 1
    fi
}

# Main test runner
main() {
    echo "🧪 Testing deploy-unified.sh AWS Code Services functionality"
    echo "============================================================"
    echo ""
    
    local tests=(
        "test_script_exists"
        "test_help_message"
        "test_function_definitions"
        "test_parameter_parsing"
        "test_dry_run_multi_region"
        "test_aws_cli"
        "test_pipeline_status"
    )
    
    local passed=0
    local failed=0
    
    for test in "${tests[@]}"; do
        if $test; then
            ((passed++))
        else
            ((failed++))
        fi
        echo ""
    done
    
    echo "============================================================"
    echo "Test Results:"
    print_pass "Passed: $passed"
    if [ $failed -gt 0 ]; then
        print_fail "Failed: $failed"
        exit 1
    else
        print_pass "All tests passed!"
        exit 0
    fi
}

# Run tests
main "$@"