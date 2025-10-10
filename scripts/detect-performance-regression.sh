#!/bin/bash

# Performance Regression Detection Script
# Monitors test performance and alerts on regressions

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BASELINE_DIR="$PROJECT_ROOT/build/reports/performance-baseline"
REGRESSION_DIR="$PROJECT_ROOT/build/reports/performance-regression"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Regression thresholds (percentage)
EXECUTION_TIME_THRESHOLD=10  # Alert if execution time increases by more than 10%
MEMORY_THRESHOLD=15          # Alert if memory usage increases by more than 15%
FAILURE_RATE_THRESHOLD=1     # Alert if failure rate increases by more than 1%

echo "========================================="
echo "Performance Regression Detection"
echo "========================================="
echo ""

# Find the most recent baseline
LATEST_BASELINE=$(find "$BASELINE_DIR" -name "baseline_metrics_*.csv" -type f | sort -r | head -1)

if [ -z "$LATEST_BASELINE" ]; then
    echo -e "${YELLOW}Warning: No baseline metrics found. Please run measure-test-performance-baseline.sh first.${NC}"
    exit 0
fi

echo "Using baseline: $(basename "$LATEST_BASELINE")"
echo ""

# Run current test suite and capture metrics
echo "Running current test suite..."
cd "$PROJECT_ROOT"

# Create temporary metrics file
CURRENT_METRICS="$REGRESSION_DIR/current_metrics_${TIMESTAMP}.csv"
mkdir -p "$REGRESSION_DIR"

echo "Task,Duration(s),TotalTests,FailedTests,SkippedTests" > "$CURRENT_METRICS"

# Function to run test and capture metrics
run_and_measure() {
    local task=$1
    
    echo "  Measuring: $task"
    
    local start_time=$(date +%s)
    ./gradlew "$task" --no-daemon --console=plain > /dev/null 2>&1 || true
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # Extract test results
    local test_results_dir="$PROJECT_ROOT/app/build/test-results"
    if [ -d "$test_results_dir" ]; then
        local total_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "tests=" {} \; | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
        local failed_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "failures=" {} \; | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
        local skipped_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "skipped=" {} \; | sed 's/.*skipped="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
        
        echo "$task,$duration,$total_tests,$failed_tests,$skipped_tests" >> "$CURRENT_METRICS"
    fi
    
    ./gradlew clean > /dev/null 2>&1
}

# Measure key test tasks
run_and_measure "quickTest"
run_and_measure "unitTest"

echo ""
echo "=== Analyzing for Regressions ==="
echo ""

# Initialize regression report
REGRESSION_REPORT="$REGRESSION_DIR/regression_report_${TIMESTAMP}.md"

cat > "$REGRESSION_REPORT" << EOF
# Performance Regression Detection Report

**Generated:** $(date)
**Baseline:** $(basename "$LATEST_BASELINE")
**Current Metrics:** $(basename "$CURRENT_METRICS")

## Regression Analysis

EOF

# Track if any regressions found
REGRESSIONS_FOUND=0

# Compare metrics
for task in "quickTest" "unitTest"; do
    # Read baseline
    baseline_duration=$(grep "^$task," "$LATEST_BASELINE" | cut -d',' -f2)
    baseline_total=$(grep "^$task," "$LATEST_BASELINE" | cut -d',' -f3)
    baseline_failed=$(grep "^$task," "$LATEST_BASELINE" | cut -d',' -f4)
    
    # Read current
    current_duration=$(grep "^$task," "$CURRENT_METRICS" | cut -d',' -f2)
    current_total=$(grep "^$task," "$CURRENT_METRICS" | cut -d',' -f3)
    current_failed=$(grep "^$task," "$CURRENT_METRICS" | cut -d',' -f4)
    
    if [ -n "$baseline_duration" ] && [ -n "$current_duration" ]; then
        # Calculate percentage change
        duration_change=$(echo "scale=2; (($current_duration - $baseline_duration) / $baseline_duration) * 100" | bc)
        
        echo "### $task" >> "$REGRESSION_REPORT"
        echo "" >> "$REGRESSION_REPORT"
        echo "| Metric | Baseline | Current | Change |" >> "$REGRESSION_REPORT"
        echo "|--------|----------|---------|--------|" >> "$REGRESSION_REPORT"
        echo "| Execution Time | ${baseline_duration}s | ${current_duration}s | ${duration_change}% |" >> "$REGRESSION_REPORT"
        
        # Check for execution time regression
        regression_check=$(echo "$duration_change > $EXECUTION_TIME_THRESHOLD" | bc)
        if [ "$regression_check" -eq 1 ]; then
            echo "" >> "$REGRESSION_REPORT"
            echo "⚠️ **REGRESSION DETECTED**: Execution time increased by ${duration_change}% (threshold: ${EXECUTION_TIME_THRESHOLD}%)" >> "$REGRESSION_REPORT"
            echo "" >> "$REGRESSION_REPORT"
            
            echo -e "${RED}⚠️  REGRESSION: $task execution time increased by ${duration_change}%${NC}"
            REGRESSIONS_FOUND=$((REGRESSIONS_FOUND + 1))
        else
            echo -e "${GREEN}✓ $task: No execution time regression${NC}"
        fi
        
        # Check failure rate
        if [ "$baseline_total" -gt 0 ] && [ "$current_total" -gt 0 ]; then
            baseline_failure_rate=$(echo "scale=2; ($baseline_failed / $baseline_total) * 100" | bc)
            current_failure_rate=$(echo "scale=2; ($current_failed / $current_total) * 100" | bc)
            failure_rate_change=$(echo "scale=2; $current_failure_rate - $baseline_failure_rate" | bc)
            
            echo "| Failure Rate | ${baseline_failure_rate}% | ${current_failure_rate}% | ${failure_rate_change}% |" >> "$REGRESSION_REPORT"
            
            failure_regression=$(echo "$failure_rate_change > $FAILURE_RATE_THRESHOLD" | bc)
            if [ "$failure_regression" -eq 1 ]; then
                echo "" >> "$REGRESSION_REPORT"
                echo "⚠️ **REGRESSION DETECTED**: Failure rate increased by ${failure_rate_change}% (threshold: ${FAILURE_RATE_THRESHOLD}%)" >> "$REGRESSION_REPORT"
                echo "" >> "$REGRESSION_REPORT"
                
                echo -e "${RED}⚠️  REGRESSION: $task failure rate increased by ${failure_rate_change}%${NC}"
                REGRESSIONS_FOUND=$((REGRESSIONS_FOUND + 1))
            else
                echo -e "${GREEN}✓ $task: No failure rate regression${NC}"
            fi
        fi
        
        echo "" >> "$REGRESSION_REPORT"
    fi
done

# Summary
cat >> "$REGRESSION_REPORT" << EOF

## Summary

EOF

if [ "$REGRESSIONS_FOUND" -eq 0 ]; then
    echo "✅ **No performance regressions detected**" >> "$REGRESSION_REPORT"
    echo ""
    echo -e "${GREEN}=========================================${NC}"
    echo -e "${GREEN}✓ No performance regressions detected${NC}"
    echo -e "${GREEN}=========================================${NC}"
else
    echo "⚠️ **$REGRESSIONS_FOUND performance regression(s) detected**" >> "$REGRESSION_REPORT"
    echo ""
    echo "Please review the detailed analysis above and take corrective action." >> "$REGRESSION_REPORT"
    echo ""
    echo -e "${RED}=========================================${NC}"
    echo -e "${RED}⚠️  $REGRESSIONS_FOUND regression(s) detected${NC}"
    echo -e "${RED}=========================================${NC}"
fi

echo ""
echo "Regression report: $REGRESSION_REPORT"
echo ""

# Exit with error code if regressions found
if [ "$REGRESSIONS_FOUND" -gt 0 ]; then
    exit 1
fi

exit 0
