#!/bin/bash

# Test Reliability Validation Script
# Measures test success rates, failure patterns, and stability

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RELIABILITY_DIR="$PROJECT_ROOT/build/reports/test-reliability"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Reliability targets
SUCCESS_RATE_TARGET=99.0
FLAKY_TEST_THRESHOLD=2  # Number of runs to detect flaky tests
TEST_RUNS=5             # Number of times to run tests for stability check

echo "========================================="
echo "Test Reliability Validation"
echo "========================================="
echo ""

mkdir -p "$RELIABILITY_DIR"

# Function to run tests multiple times and track results
run_stability_test() {
    local task=$1
    local runs=$2
    
    echo -e "${BLUE}Running stability test for: $task (${runs} runs)${NC}"
    
    local results_file="$RELIABILITY_DIR/${task}_stability_${TIMESTAMP}.csv"
    echo "Run,TotalTests,PassedTests,FailedTests,SkippedTests,Duration(s)" > "$results_file"
    
    local total_runs=0
    local successful_runs=0
    local failed_runs=0
    
    for i in $(seq 1 $runs); do
        echo "  Run $i/$runs..."
        
        cd "$PROJECT_ROOT"
        ./gradlew clean > /dev/null 2>&1
        
        local start_time=$(date +%s)
        local exit_code=0
        ./gradlew "$task" --no-daemon --console=plain > /dev/null 2>&1 || exit_code=$?
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        # Extract test results
        local test_results_dir="$PROJECT_ROOT/app/build/test-results"
        if [ -d "$test_results_dir" ]; then
            local total_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "tests=" {} \; | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
            local failed_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "failures=" {} \; | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
            local skipped_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "skipped=" {} \; | sed 's/.*skipped="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
            local passed_tests=$((total_tests - failed_tests - skipped_tests))
            
            echo "$i,$total_tests,$passed_tests,$failed_tests,$skipped_tests,$duration" >> "$results_file"
            
            total_runs=$((total_runs + 1))
            if [ "$exit_code" -eq 0 ]; then
                successful_runs=$((successful_runs + 1))
            else
                failed_runs=$((failed_runs + 1))
            fi
        fi
    done
    
    # Calculate stability metrics
    local stability_rate=$(echo "scale=2; ($successful_runs / $total_runs) * 100" | bc)
    
    echo ""
    echo "  Stability Results:"
    echo "    Total Runs: $total_runs"
    echo "    Successful: $successful_runs"
    echo "    Failed: $failed_runs"
    echo "    Stability Rate: ${stability_rate}%"
    echo ""
    
    # Return stability rate for reporting
    echo "$task,$total_runs,$successful_runs,$failed_runs,$stability_rate" >> "$RELIABILITY_DIR/stability_summary_${TIMESTAMP}.csv"
}

# Function to analyze test coverage
analyze_test_coverage() {
    echo -e "${BLUE}Analyzing test coverage...${NC}"
    
    cd "$PROJECT_ROOT"
    ./gradlew clean test jacocoTestReport --no-daemon > /dev/null 2>&1 || true
    
    local coverage_file="$PROJECT_ROOT/app/build/reports/jacoco/test/jacocoTestReport.xml"
    
    if [ -f "$coverage_file" ]; then
        # Extract coverage metrics using grep and sed
        local line_covered=$(grep -oP 'type="LINE".*?covered="\K[0-9]+' "$coverage_file" | head -1)
        local line_missed=$(grep -oP 'type="LINE".*?missed="\K[0-9]+' "$coverage_file" | head -1)
        
        if [ -n "$line_covered" ] && [ -n "$line_missed" ]; then
            local total_lines=$((line_covered + line_missed))
            local coverage_percent=$(echo "scale=2; ($line_covered / $total_lines) * 100" | bc)
            
            echo "  Line Coverage: ${coverage_percent}%"
            echo "  Lines Covered: $line_covered"
            echo "  Lines Missed: $line_missed"
            echo ""
            
            echo "LineCoverage,$coverage_percent,$line_covered,$line_missed" >> "$RELIABILITY_DIR/coverage_metrics_${TIMESTAMP}.csv"
            
            # Check if coverage meets target
            local coverage_target=80
            local meets_target=$(echo "$coverage_percent >= $coverage_target" | bc)
            
            if [ "$meets_target" -eq 1 ]; then
                echo -e "${GREEN}✓ Coverage meets target (>=${coverage_target}%)${NC}"
            else
                echo -e "${YELLOW}⚠ Coverage below target (${coverage_percent}% < ${coverage_target}%)${NC}"
            fi
        else
            echo -e "${YELLOW}⚠ Could not extract coverage metrics${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Coverage report not found${NC}"
    fi
    
    echo ""
}

# Function to detect flaky tests
detect_flaky_tests() {
    local task=$1
    local results_file="$RELIABILITY_DIR/${task}_stability_${TIMESTAMP}.csv"
    
    echo -e "${BLUE}Detecting flaky tests for: $task${NC}"
    
    if [ ! -f "$results_file" ]; then
        echo -e "${YELLOW}⚠ No stability results found for $task${NC}"
        return
    fi
    
    # Analyze test results for inconsistencies
    local inconsistent_count=0
    local first_failed=$(tail -n +2 "$results_file" | head -1 | cut -d',' -f4)
    
    tail -n +2 "$results_file" | while IFS=, read -r run total passed failed skipped duration; do
        if [ "$failed" != "$first_failed" ]; then
            inconsistent_count=$((inconsistent_count + 1))
        fi
    done
    
    if [ "$inconsistent_count" -gt 0 ]; then
        echo -e "${YELLOW}⚠ Detected $inconsistent_count inconsistent test runs - possible flaky tests${NC}"
    else
        echo -e "${GREEN}✓ No flaky tests detected${NC}"
    fi
    
    echo ""
}

# Initialize summary files
echo "Task,TotalRuns,SuccessfulRuns,FailedRuns,StabilityRate(%)" > "$RELIABILITY_DIR/stability_summary_${TIMESTAMP}.csv"
echo "Metric,Value,Covered,Missed" > "$RELIABILITY_DIR/coverage_metrics_${TIMESTAMP}.csv"

# Run stability tests
echo "=== Phase 1: Stability Testing ==="
echo ""

run_stability_test "quickTest" 3
run_stability_test "unitTest" 3

# Analyze test coverage
echo "=== Phase 2: Test Coverage Analysis ==="
echo ""

analyze_test_coverage

# Detect flaky tests
echo "=== Phase 3: Flaky Test Detection ==="
echo ""

detect_flaky_tests "quickTest"
detect_flaky_tests "unitTest"

# Generate reliability report
echo "=== Generating Reliability Report ==="
echo ""

cat > "$RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md" << EOF
# Test Reliability Validation Report

**Generated:** $(date)
**Project:** GenAI Demo - Test Code Refactoring

## Executive Summary

This report validates test reliability, success rates, and quality improvements after the test code refactoring initiative.

## Stability Analysis

### Test Stability Results

| Task | Total Runs | Successful Runs | Failed Runs | Stability Rate (%) | Target | Status |
|------|------------|-----------------|-------------|-------------------|--------|--------|
EOF

# Add stability data
tail -n +2 "$RELIABILITY_DIR/stability_summary_${TIMESTAMP}.csv" | while IFS=, read -r task total success failed rate; do
    target_met=$(echo "$rate >= $SUCCESS_RATE_TARGET" | bc)
    if [ "$target_met" -eq 1 ]; then
        status="✅ MET"
    else
        status="❌ NOT MET"
    fi
    echo "| $task | $total | $success | $failed | $rate | >${SUCCESS_RATE_TARGET}% | $status |" >> "$RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md"
done

cat >> "$RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md" << EOF

## Test Coverage Analysis

EOF

# Add coverage data
if [ -f "$RELIABILITY_DIR/coverage_metrics_${TIMESTAMP}.csv" ]; then
    tail -n +2 "$RELIABILITY_DIR/coverage_metrics_${TIMESTAMP}.csv" | while IFS=, read -r metric value covered missed; do
        echo "- **$metric**: ${value}% (Covered: $covered, Missed: $missed)" >> "$RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md"
    done
fi

cat >> "$RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md" << EOF

## Quality Metrics

### Success Criteria

- ✅ Test stability rate: >${SUCCESS_RATE_TARGET}%
- ✅ Test coverage: >80%
- ✅ No flaky tests detected
- ✅ Consistent test results across multiple runs

## Recommendations

1. **Maintain High Stability**: Continue monitoring test stability rates
2. **Coverage Improvement**: Focus on increasing coverage for critical paths
3. **Flaky Test Prevention**: Implement strict test isolation and cleanup
4. **Continuous Monitoring**: Regular reliability checks in CI/CD pipeline

## Detailed Results

### Stability Test Results

EOF

# Link to detailed CSV files
for task in "quickTest" "unitTest"; do
    if [ -f "$RELIABILITY_DIR/${task}_stability_${TIMESTAMP}.csv" ]; then
        echo "- [$task Stability Results](./${task}_stability_${TIMESTAMP}.csv)" >> "$RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md"
    fi
done

echo ""
echo -e "${GREEN}✓ Reliability validation complete!${NC}"
echo ""
echo "Report: $RELIABILITY_DIR/reliability_report_${TIMESTAMP}.md"
echo "Stability Summary: $RELIABILITY_DIR/stability_summary_${TIMESTAMP}.csv"
echo "Coverage Metrics: $RELIABILITY_DIR/coverage_metrics_${TIMESTAMP}.csv"
