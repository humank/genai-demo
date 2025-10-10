#!/bin/bash

# Test Performance Comparison Script
# Compares baseline metrics with current metrics to validate improvements

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <baseline_metrics.csv> <current_metrics.csv>"
    echo ""
    echo "Example:"
    echo "  $0 build/reports/performance-baseline/baseline_metrics_20250123_143000.csv \\"
    echo "     build/reports/performance-baseline/baseline_metrics_20250124_100000.csv"
    exit 1
fi

BASELINE_FILE=$1
CURRENT_FILE=$2
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPARISON_DIR="$PROJECT_ROOT/build/reports/performance-comparison"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Validate input files
if [ ! -f "$BASELINE_FILE" ]; then
    echo -e "${RED}Error: Baseline file not found: $BASELINE_FILE${NC}"
    exit 1
fi

if [ ! -f "$CURRENT_FILE" ]; then
    echo -e "${RED}Error: Current metrics file not found: $CURRENT_FILE${NC}"
    exit 1
fi

mkdir -p "$COMPARISON_DIR"

echo "========================================="
echo "Test Performance Comparison"
echo "========================================="
echo ""
echo "Baseline: $BASELINE_FILE"
echo "Current:  $CURRENT_FILE"
echo ""

# Function to calculate percentage improvement
calculate_improvement() {
    local baseline=$1
    local current=$2
    
    if [ "$baseline" -eq 0 ]; then
        echo "N/A"
        return
    fi
    
    local improvement=$(echo "scale=2; (($baseline - $current) / $baseline) * 100" | bc)
    echo "$improvement"
}

# Function to determine if target is met
check_target() {
    local improvement=$1
    local target=$2
    
    if [ "$improvement" == "N/A" ]; then
        echo "UNKNOWN"
        return
    fi
    
    local result=$(echo "$improvement >= $target" | bc)
    if [ "$result" -eq 1 ]; then
        echo "MET"
    else
        echo "NOT MET"
    fi
}

# Generate comparison report
cat > "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md" << EOF
# Test Performance Comparison Report

**Generated:** $(date)
**Baseline File:** $(basename "$BASELINE_FILE")
**Current File:** $(basename "$CURRENT_FILE")

## Executive Summary

This report compares test performance metrics before and after the test code refactoring initiative.

### Performance Targets

| Metric | Target | Status |
|--------|--------|--------|
EOF

# Parse and compare metrics
declare -A baseline_metrics
declare -A current_metrics

# Read baseline metrics (skip header)
tail -n +2 "$BASELINE_FILE" | while IFS=, read -r task duration total failed skipped; do
    baseline_metrics["${task}_duration"]=$duration
    baseline_metrics["${task}_total"]=$total
    baseline_metrics["${task}_failed"]=$failed
done

# Read current metrics (skip header)
tail -n +2 "$CURRENT_FILE" | while IFS=, read -r task duration total failed skipped; do
    current_metrics["${task}_duration"]=$duration
    current_metrics["${task}_total"]=$total
    current_metrics["${task}_failed"]=$failed
done

# Calculate improvements for key metrics
echo ""
echo "=== Calculating Performance Improvements ==="
echo ""

# Create detailed comparison table
cat >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md" << EOF

## Detailed Comparison

### Execution Time Comparison

| Task | Baseline (s) | Current (s) | Improvement (%) | Target | Status |
|------|--------------|-------------|-----------------|--------|--------|
EOF

# Process each task
for task in "test" "quickTest" "unitTest" "integrationTest"; do
    # Read baseline
    baseline_duration=$(grep "^$task," "$BASELINE_FILE" | cut -d',' -f2)
    baseline_total=$(grep "^$task," "$BASELINE_FILE" | cut -d',' -f3)
    baseline_failed=$(grep "^$task," "$BASELINE_FILE" | cut -d',' -f4)
    
    # Read current
    current_duration=$(grep "^$task," "$CURRENT_FILE" | cut -d',' -f2)
    current_total=$(grep "^$task," "$CURRENT_FILE" | cut -d',' -f3)
    current_failed=$(grep "^$task," "$CURRENT_FILE" | cut -d',' -f4)
    
    if [ -n "$baseline_duration" ] && [ -n "$current_duration" ]; then
        improvement=$(calculate_improvement "$baseline_duration" "$current_duration")
        
        # Determine target based on task
        if [[ "$task" == *"unit"* ]] || [[ "$task" == "quickTest" ]]; then
            target="60"
        else
            target="50"
        fi
        
        status=$(check_target "$improvement" "$target")
        
        # Color code status
        if [ "$status" == "MET" ]; then
            status_icon="✅"
        else
            status_icon="❌"
        fi
        
        echo "| $task | $baseline_duration | $current_duration | $improvement% | >$target% | $status_icon $status |" >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md"
        
        echo -e "${BLUE}$task:${NC}"
        echo "  Baseline: ${baseline_duration}s"
        echo "  Current:  ${current_duration}s"
        echo "  Improvement: ${improvement}%"
        echo "  Target: >${target}%"
        echo "  Status: $status_icon $status"
        echo ""
    fi
done

# Test reliability comparison
cat >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md" << EOF

### Test Reliability Comparison

| Task | Baseline Success Rate | Current Success Rate | Target | Status |
|------|----------------------|---------------------|--------|--------|
EOF

for task in "test" "quickTest" "unitTest" "integrationTest"; do
    baseline_total=$(grep "^$task," "$BASELINE_FILE" | cut -d',' -f3)
    baseline_failed=$(grep "^$task," "$BASELINE_FILE" | cut -d',' -f4)
    
    current_total=$(grep "^$task," "$CURRENT_FILE" | cut -d',' -f3)
    current_failed=$(grep "^$task," "$CURRENT_FILE" | cut -d',' -f4)
    
    if [ -n "$baseline_total" ] && [ "$baseline_total" -gt 0 ]; then
        baseline_success=$(echo "scale=2; (($baseline_total - $baseline_failed) / $baseline_total) * 100" | bc)
        current_success=$(echo "scale=2; (($current_total - $current_failed) / $current_total) * 100" | bc)
        
        target_met=$(echo "$current_success >= 99" | bc)
        if [ "$target_met" -eq 1 ]; then
            status="✅ MET"
        else
            status="❌ NOT MET"
        fi
        
        echo "| $task | ${baseline_success}% | ${current_success}% | >99% | $status |" >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md"
    fi
done

# Add recommendations section
cat >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md" << EOF

## Recommendations

### Achievements

EOF

# Check which targets were met
unit_test_improvement=$(grep "^unitTest," "$CURRENT_FILE" | cut -d',' -f2)
baseline_unit=$(grep "^unitTest," "$BASELINE_FILE" | cut -d',' -f2)

if [ -n "$unit_test_improvement" ] && [ -n "$baseline_unit" ]; then
    improvement=$(calculate_improvement "$baseline_unit" "$unit_test_improvement")
    target_check=$(check_target "$improvement" "60")
    
    if [ "$target_check" == "MET" ]; then
        echo "- ✅ Unit test execution time reduction target achieved (>60%)" >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md"
    else
        echo "- ❌ Unit test execution time reduction target not yet achieved (current: ${improvement}%, target: >60%)" >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md"
    fi
fi

cat >> "$COMPARISON_DIR/comparison_report_${TIMESTAMP}.md" << EOF

### Areas for Improvement

- Continue optimizing test execution strategies
- Monitor memory usage trends
- Implement additional performance regression detection
- Enhance CI/CD pipeline efficiency

### Next Steps

1. Review detailed metrics for tasks that didn't meet targets
2. Identify bottlenecks in test execution
3. Implement additional optimizations
4. Re-run comparison after optimizations

## Appendix

### Baseline Metrics File
\`\`\`
$(cat "$BASELINE_FILE")
\`\`\`

### Current Metrics File
\`\`\`
$(cat "$CURRENT_FILE")
\`\`\`

EOF

echo -e "${GREEN}✓ Comparison report generated!${NC}"
echo ""
echo "Report: $COMPARISON_DIR/comparison_report_${TIMESTAMP}.md"
echo ""
echo "To view the report:"
echo "  cat $COMPARISON_DIR/comparison_report_${TIMESTAMP}.md"
