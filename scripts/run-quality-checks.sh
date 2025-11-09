#!/bin/bash
#
# Documentation Quality Checks Runner
#
# This script runs all automated quality checks on the documentation:
# - Link validation
# - Spelling and grammar checks
# - Template compliance checks
# - Completeness validation
# - Cross-reference validation
# - Diagram validation

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Print header
print_header() {
    echo ""
    echo -e "${BOLD}${BLUE}================================================================================${NC}"
    echo -e "${BOLD}${BLUE}$(printf '%*s' $(((${#1}+80)/2)) "$1")${NC}"
    echo -e "${BOLD}${BLUE}================================================================================${NC}"
    echo ""
}

# Print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Print warning
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Print info
print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Track results
TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

# Run a check and track results
run_check() {
    local check_name="$1"
    local check_command="$2"
    
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
    
    print_info "Running: $check_name"
    
    if eval "$check_command"; then
        print_success "$check_name passed"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
        return 0
    else
        print_error "$check_name failed"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
        return 1
    fi
}

# Main execution
print_header "Documentation Quality Checks"

cd "$PROJECT_ROOT"

# Check 1: Documentation Completeness
print_header "Check 1: Documentation Completeness"
run_check "Completeness Validation" "python3 scripts/validate-documentation-completeness.py" || true

# Check 2: Cross-Reference Validation
print_header "Check 2: Cross-Reference Validation"
run_check "Cross-Reference Validation" "python3 scripts/validate-cross-references.py" || true

# Check 3: Diagram Validation
print_header "Check 3: Diagram Validation"
run_check "Diagram Validation" "python3 scripts/validate-diagrams.py" || true

# Check 4: Link Validation (if markdown-link-check is available)
print_header "Check 4: Link Validation"
if command -v markdown-link-check &> /dev/null; then
    print_info "Running markdown-link-check on key files..."
    
    # Check main README
    if markdown-link-check docs/README.md --quiet 2>/dev/null; then
        print_success "Main README links valid"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
        print_warning "Main README has broken links"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
else
    print_warning "markdown-link-check not installed, skipping"
    print_info "Install with: npm install -g markdown-link-check"
fi

# Check 5: Spelling Check (if cspell is available)
print_header "Check 5: Spelling Check"
if command -v cspell &> /dev/null; then
    print_info "Running spell check on documentation..."
    
    if cspell "docs/**/*.md" --no-progress --quiet 2>/dev/null; then
        print_success "No spelling errors found"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
        print_warning "Spelling errors found"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
else
    print_warning "cspell not installed, skipping"
    print_info "Install with: npm install -g cspell"
fi

# Check 6: Markdown Lint (if markdownlint is available)
print_header "Check 6: Markdown Lint"
if command -v markdownlint &> /dev/null; then
    print_info "Running markdown linter..."
    
    if markdownlint docs/**/*.md --config .markdownlint.json 2>/dev/null; then
        print_success "Markdown lint passed"
        PASSED_CHECKS=$((PASSED_CHECKS + 1))
    else
        print_warning "Markdown lint issues found"
        FAILED_CHECKS=$((FAILED_CHECKS + 1))
    fi
    TOTAL_CHECKS=$((TOTAL_CHECKS + 1))
else
    print_warning "markdownlint not installed, skipping"
    print_info "Install with: npm install -g markdownlint-cli"
fi

# Generate summary report
print_header "Quality Checks Summary"

echo -e "${BOLD}Total Checks: $TOTAL_CHECKS${NC}"
echo -e "${GREEN}Passed: $PASSED_CHECKS${NC}"
echo -e "${RED}Failed: $FAILED_CHECKS${NC}"

if [ $TOTAL_CHECKS -gt 0 ]; then
    SUCCESS_RATE=$(awk "BEGIN {printf \"%.2f\", ($PASSED_CHECKS/$TOTAL_CHECKS)*100}")
    echo -e "${BOLD}Success Rate: $SUCCESS_RATE%${NC}"
fi

# Save summary to file
REPORT_DIR="$PROJECT_ROOT/docs/reports"
mkdir -p "$REPORT_DIR"

cat > "$REPORT_DIR/quality-checks-summary.txt" << EOF
Documentation Quality Checks Summary
Generated: $(date)

Total Checks: $TOTAL_CHECKS
Passed: $PASSED_CHECKS
Failed: $FAILED_CHECKS
Success Rate: ${SUCCESS_RATE}%

Individual Check Results:
- Completeness Validation: $([ -f "$REPORT_DIR/completeness-validation-report.json" ] && echo "✓" || echo "✗")
- Cross-Reference Validation: $([ -f "$REPORT_DIR/cross-reference-validation-report.json" ] && echo "✓" || echo "✗")
- Diagram Validation: $([ -f "$REPORT_DIR/diagram-validation-report.json" ] && echo "✓" || echo "✗")

Detailed reports available in: $REPORT_DIR/
EOF

print_info "Summary report saved to: $REPORT_DIR/quality-checks-summary.txt"

# Exit with appropriate code
if [ $FAILED_CHECKS -eq 0 ]; then
    echo ""
    print_success "All quality checks passed!"
    exit 0
else
    echo ""
    print_error "$FAILED_CHECKS quality check(s) failed"
    print_warning "Please review the detailed reports and fix the issues"
    exit 1
fi
