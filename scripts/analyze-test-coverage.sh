#!/bin/bash

# Test Coverage Analysis Script
# Analyzes test coverage and generates comprehensive reports
# Last Updated: October 1, 2025 (Taipei Time)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
MIN_COVERAGE=80
TARGET_COVERAGE=85
EXCELLENT_COVERAGE=90

echo -e "${BLUE}==================================================================${NC}"
echo -e "${BLUE}Test Coverage Analysis${NC}"
echo -e "${BLUE}==================================================================${NC}"
echo ""

# Navigate to project root
cd "$PROJECT_ROOT/app"

# Run tests with coverage
echo -e "${BLUE}Running tests with coverage...${NC}"
./gradlew clean test jacocoTestReport --no-daemon

# Check if coverage report exists
COVERAGE_REPORT="build/reports/jacoco/test/html/index.html"
if [ ! -f "$COVERAGE_REPORT" ]; then
    echo -e "${RED}Error: Coverage report not found at $COVERAGE_REPORT${NC}"
    exit 1
fi

echo -e "${GREEN}Coverage report generated successfully${NC}"
echo ""

# Extract coverage metrics from XML report
COVERAGE_XML="build/reports/jacoco/test/jacocoTestReport.xml"
if [ -f "$COVERAGE_XML" ]; then
    echo -e "${BLUE}Coverage Summary:${NC}"
    
    # Extract line coverage
    LINE_COVERED=$(grep -oP 'type="LINE".*?covered="\K[0-9]+' "$COVERAGE_XML" | head -1)
    LINE_MISSED=$(grep -oP 'type="LINE".*?missed="\K[0-9]+' "$COVERAGE_XML" | head -1)
    
    if [ -n "$LINE_COVERED" ] && [ -n "$LINE_MISSED" ]; then
        LINE_TOTAL=$((LINE_COVERED + LINE_MISSED))
        LINE_PERCENTAGE=$(awk "BEGIN {printf \"%.2f\", ($LINE_COVERED / $LINE_TOTAL) * 100}")
        
        echo -e "  Line Coverage: ${LINE_COVERED}/${LINE_TOTAL} (${LINE_PERCENTAGE}%)"
        
        # Color code based on coverage
        if (( $(echo "$LINE_PERCENTAGE >= $EXCELLENT_COVERAGE" | bc -l) )); then
            echo -e "  Status: ${GREEN}EXCELLENT${NC} (>= ${EXCELLENT_COVERAGE}%)"
        elif (( $(echo "$LINE_PERCENTAGE >= $TARGET_COVERAGE" | bc -l) )); then
            echo -e "  Status: ${GREEN}GOOD${NC} (>= ${TARGET_COVERAGE}%)"
        elif (( $(echo "$LINE_PERCENTAGE >= $MIN_COVERAGE" | bc -l) )); then
            echo -e "  Status: ${YELLOW}ACCEPTABLE${NC} (>= ${MIN_COVERAGE}%)"
        else
            echo -e "  Status: ${RED}INSUFFICIENT${NC} (< ${MIN_COVERAGE}%)"
        fi
    fi
    
    # Extract branch coverage
    BRANCH_COVERED=$(grep -oP 'type="BRANCH".*?covered="\K[0-9]+' "$COVERAGE_XML" | head -1)
    BRANCH_MISSED=$(grep -oP 'type="BRANCH".*?missed="\K[0-9]+' "$COVERAGE_XML" | head -1)
    
    if [ -n "$BRANCH_COVERED" ] && [ -n "$BRANCH_MISSED" ]; then
        BRANCH_TOTAL=$((BRANCH_COVERED + BRANCH_MISSED))
        BRANCH_PERCENTAGE=$(awk "BEGIN {printf \"%.2f\", ($BRANCH_COVERED / $BRANCH_TOTAL) * 100}")
        
        echo -e "  Branch Coverage: ${BRANCH_COVERED}/${BRANCH_TOTAL} (${BRANCH_PERCENTAGE}%)"
    fi
    
    echo ""
fi

# Run coverage verification
echo -e "${BLUE}Verifying coverage meets minimum threshold (${MIN_COVERAGE}%)...${NC}"
if ./gradlew jacocoTestCoverageVerification --no-daemon 2>&1 | grep -q "FAILED"; then
    echo -e "${RED}Coverage verification FAILED${NC}"
    echo -e "${YELLOW}Coverage is below minimum threshold of ${MIN_COVERAGE}%${NC}"
    exit 1
else
    echo -e "${GREEN}Coverage verification PASSED${NC}"
fi

echo ""

# Analyze uncovered code
echo -e "${BLUE}Analyzing uncovered code...${NC}"
if [ -f "$COVERAGE_XML" ]; then
    # Find packages with low coverage
    echo -e "${YELLOW}Packages needing attention:${NC}"
    
    # This is a simplified analysis - in production, use a proper XML parser
    grep -A 5 'name="solid.humank.genaidemo' "$COVERAGE_XML" | \
        grep -E 'package|LINE' | \
        head -20
fi

echo ""

# Generate summary report
SUMMARY_REPORT="build/reports/test-coverage-summary.txt"
cat > "$SUMMARY_REPORT" << EOF
Test Coverage Analysis Report
Generated: $(date '+%Y-%m-%d %H:%M:%S')

Coverage Summary:
- Line Coverage: ${LINE_PERCENTAGE}%
- Branch Coverage: ${BRANCH_PERCENTAGE}%
- Minimum Required: ${MIN_COVERAGE}%
- Target: ${TARGET_COVERAGE}%

Status: $([ $(echo "$LINE_PERCENTAGE >= $MIN_COVERAGE" | bc -l) -eq 1 ] && echo "PASSED" || echo "FAILED")

Reports:
- HTML Report: $COVERAGE_REPORT
- XML Report: $COVERAGE_XML
- Summary: $SUMMARY_REPORT

Recommendations:
$(if (( $(echo "$LINE_PERCENTAGE < $MIN_COVERAGE" | bc -l) )); then
    echo "- Increase test coverage to meet minimum threshold"
    echo "- Focus on uncovered packages and classes"
    echo "- Add tests for edge cases and error conditions"
elif (( $(echo "$LINE_PERCENTAGE < $TARGET_COVERAGE" | bc -l) )); then
    echo "- Good coverage, aim for target of ${TARGET_COVERAGE}%"
    echo "- Review uncovered code for critical paths"
elif (( $(echo "$LINE_PERCENTAGE < $EXCELLENT_COVERAGE" | bc -l) )); then
    echo "- Excellent coverage, maintain current quality"
    echo "- Consider edge cases and error scenarios"
else
    echo "- Outstanding coverage!"
    echo "- Maintain current quality standards"
fi)

EOF

echo -e "${GREEN}Summary report generated: $SUMMARY_REPORT${NC}"
echo ""

# Open HTML report
echo -e "${BLUE}Opening coverage report in browser...${NC}"
if command -v open &> /dev/null; then
    open "$COVERAGE_REPORT"
elif command -v xdg-open &> /dev/null; then
    xdg-open "$COVERAGE_REPORT"
else
    echo -e "${YELLOW}Please open manually: $COVERAGE_REPORT${NC}"
fi

echo ""
echo -e "${GREEN}Coverage analysis complete!${NC}"
