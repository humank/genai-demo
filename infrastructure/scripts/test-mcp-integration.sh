#!/bin/bash

# ============================================
# MCP AWS Pricing Integration Test Script
# ============================================
# Purpose: Verify MCP tools installation and configuration
# Usage: ./infrastructure/scripts/test-mcp-integration.sh
# ============================================

# Don't exit on error - we want to run all tests
set +e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TOTAL_TESTS=0

# ============================================
# Helper Functions
# ============================================

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((TESTS_PASSED++))
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
    ((TESTS_FAILED++))
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

run_test() {
    local test_name=$1
    local test_command=$2
    
    ((TOTAL_TESTS++))
    log_info "Running test: $test_name"
    
    if eval "$test_command" > /dev/null 2>&1; then
        log_success "$test_name: PASSED"
        return 0
    else
        log_error "$test_name: FAILED"
        return 1
    fi
}

# ============================================
# Test Suite
# ============================================

echo "=========================================="
echo "MCP AWS Pricing Integration Test Suite"
echo "=========================================="
echo ""

# Test 1: Check uv installation
((TOTAL_TESTS++))
log_info "Test 1: Checking uv installation..."
if command -v uv &> /dev/null; then
    UV_VERSION=$(uv --version)
    log_success "uv is installed: $UV_VERSION"
else
    log_error "uv is not installed"
    log_warning "Install with: curl -LsSf https://astral.sh/uv/install.sh | sh"
fi

# Test 2: Check uvx installation
((TOTAL_TESTS++))
log_info "Test 2: Checking uvx installation..."
if command -v uvx &> /dev/null; then
    log_success "uvx is installed"
else
    log_error "uvx is not installed"
fi

# Test 3: Check MCP configuration file
((TOTAL_TESTS++))
log_info "Test 3: Checking MCP configuration file..."
if [ -f ".kiro/settings/mcp-cost-analysis.json" ]; then
    log_success "MCP configuration file exists"
else
    log_error "MCP configuration file not found"
    log_warning "Expected location: .kiro/settings/mcp-cost-analysis.json"
fi

# Test 4: Validate MCP configuration JSON
((TOTAL_TESTS++))
log_info "Test 4: Validating MCP configuration JSON..."
if [ -f ".kiro/settings/mcp-cost-analysis.json" ]; then
    if jq empty .kiro/settings/mcp-cost-analysis.json 2>/dev/null; then
        log_success "MCP configuration JSON is valid"
    else
        log_error "MCP configuration JSON is invalid"
    fi
else
    log_warning "Skipping JSON validation (file not found)"
fi

# Test 5: Check TCO calculator script
((TOTAL_TESTS++))
log_info "Test 5: Checking TCO calculator script..."
if [ -f "infrastructure/scripts/mcp-tco-calculator.sh" ]; then
    log_success "TCO calculator script exists"
else
    log_error "TCO calculator script not found"
fi

# Test 6: Check script permissions
((TOTAL_TESTS++))
log_info "Test 6: Checking script permissions..."
if [ -x "infrastructure/scripts/mcp-tco-calculator.sh" ]; then
    log_success "TCO calculator script is executable"
else
    log_error "TCO calculator script is not executable"
    log_warning "Fix with: chmod +x infrastructure/scripts/mcp-tco-calculator.sh"
fi

# Test 7: Check AWS CLI
((TOTAL_TESTS++))
log_info "Test 7: Checking AWS CLI..."
if command -v aws &> /dev/null; then
    AWS_VERSION=$(aws --version 2>&1 | cut -d' ' -f1)
    log_success "AWS CLI is installed: $AWS_VERSION"
else
    log_error "AWS CLI is not installed"
fi

# Test 8: Check AWS credentials
((TOTAL_TESTS++))
log_info "Test 8: Checking AWS credentials..."
if aws sts get-caller-identity &> /dev/null; then
    AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
    log_success "AWS credentials are configured (Account: $AWS_ACCOUNT)"
else
    log_error "AWS credentials are not configured"
fi

# Test 9: Check jq installation
((TOTAL_TESTS++))
log_info "Test 9: Checking jq installation..."
if command -v jq &> /dev/null; then
    JQ_VERSION=$(jq --version)
    log_success "jq is installed: $JQ_VERSION"
else
    log_error "jq is not installed"
    log_warning "Install with: sudo apt-get install jq (Ubuntu) or brew install jq (macOS)"
fi

# Test 10: Check bc installation
((TOTAL_TESTS++))
log_info "Test 10: Checking bc installation..."
if command -v bc &> /dev/null; then
    log_success "bc is installed"
else
    log_error "bc is not installed"
    log_warning "Install with: sudo apt-get install bc (Ubuntu) or brew install bc (macOS)"
fi

# Test 11: Check reports directory
((TOTAL_TESTS++))
log_info "Test 11: Checking reports directory..."
if [ -d "reports/cost-analysis" ]; then
    log_success "Reports directory exists"
else
    log_warning "Reports directory does not exist (will be created on first run)"
fi

# Test 12: Test MCP AWS Pricing server (optional)
((TOTAL_TESTS++))
log_info "Test 12: Testing MCP AWS Pricing server..."
if command -v uvx &> /dev/null; then
    if timeout 10s uvx awslabs.aws-pricing-mcp-server@latest --help &> /dev/null; then
        log_success "MCP AWS Pricing server is accessible"
    else
        log_warning "MCP AWS Pricing server test timed out or failed (may require first-time download)"
    fi
else
    log_warning "Skipping MCP server test (uvx not available)"
fi

# ============================================
# Test Summary
# ============================================

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Total Tests: $TOTAL_TESTS"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo "=========================================="

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed! MCP integration is ready.${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Run TCO analysis: ./infrastructure/scripts/mcp-tco-calculator.sh"
    echo "2. View reports in: reports/cost-analysis/"
    echo "3. Check GitHub Actions: .github/workflows/cost-analysis.yml"
    exit 0
else
    echo -e "${RED}❌ Some tests failed. Please fix the issues above.${NC}"
    echo ""
    echo "Common fixes:"
    echo "1. Install uv: curl -LsSf https://astral.sh/uv/install.sh | sh"
    echo "2. Install jq: sudo apt-get install jq (Ubuntu) or brew install jq (macOS)"
    echo "3. Install bc: sudo apt-get install bc (Ubuntu) or brew install bc (macOS)"
    echo "4. Configure AWS: aws configure"
    echo "5. Make script executable: chmod +x infrastructure/scripts/mcp-tco-calculator.sh"
    exit 1
fi
