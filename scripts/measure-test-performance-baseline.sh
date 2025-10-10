#!/bin/bash

# Test Performance Baseline Measurement Script
# This script measures current test execution times and resource usage
# to establish baseline metrics for comparison after refactoring

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BASELINE_DIR="$PROJECT_ROOT/build/reports/performance-baseline"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
GREEN='\033[0.32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "========================================="
echo "Test Performance Baseline Measurement"
echo "========================================="
echo ""

# Create baseline directory
mkdir -p "$BASELINE_DIR"

# Function to measure execution time
measure_execution_time() {
    local task_name=$1
    local start_time=$(date +%s)
    
    echo -e "${YELLOW}Measuring: $task_name${NC}"
    
    # Run the task and capture output
    cd "$PROJECT_ROOT"
    ./gradlew clean > /dev/null 2>&1
    
    local output_file="$BASELINE_DIR/${task_name}_${TIMESTAMP}.log"
    
    # Measure with time command
    { time ./gradlew "$task_name" --no-daemon --console=plain 2>&1 | tee "$output_file"; } 2> "$BASELINE_DIR/${task_name}_time.txt"
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo -e "${GREEN}✓ Completed in ${duration}s${NC}"
    echo ""
    
    # Extract test results
    local test_results_dir="$PROJECT_ROOT/app/build/test-results"
    if [ -d "$test_results_dir" ]; then
        local total_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "tests=" {} \; | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
        local failed_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "failures=" {} \; | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
        local skipped_tests=$(find "$test_results_dir" -name "*.xml" -exec grep -h "skipped=" {} \; | sed 's/.*skipped="\([0-9]*\)".*/\1/' | awk '{s+=$1} END {print s}')
        
        echo "$task_name,$duration,$total_tests,$failed_tests,$skipped_tests" >> "$BASELINE_DIR/baseline_metrics_${TIMESTAMP}.csv"
    fi
    
    return $duration
}

# Function to measure memory usage
measure_memory_usage() {
    local task_name=$1
    
    echo -e "${YELLOW}Measuring memory usage for: $task_name${NC}"
    
    cd "$PROJECT_ROOT"
    ./gradlew clean > /dev/null 2>&1
    
    # Run with memory profiling
    ./gradlew "$task_name" --no-daemon -Dorg.gradle.jvmargs="-Xmx8g -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:$BASELINE_DIR/${task_name}_gc_${TIMESTAMP}.log" > /dev/null 2>&1
    
    # Analyze GC log
    if [ -f "$BASELINE_DIR/${task_name}_gc_${TIMESTAMP}.log" ]; then
        local max_heap=$(grep -oP 'Heap\s+\K\d+' "$BASELINE_DIR/${task_name}_gc_${TIMESTAMP}.log" | sort -n | tail -1)
        echo -e "${GREEN}✓ Max heap usage: ${max_heap}K${NC}"
        echo "$task_name,$max_heap" >> "$BASELINE_DIR/memory_baseline_${TIMESTAMP}.csv"
    fi
    
    echo ""
}

# Initialize CSV files
echo "Task,Duration(s),TotalTests,FailedTests,SkippedTests" > "$BASELINE_DIR/baseline_metrics_${TIMESTAMP}.csv"
echo "Task,MaxHeapUsage(K)" > "$BASELINE_DIR/memory_baseline_${TIMESTAMP}.csv"

# Measure different test tasks
echo "=== Phase 1: Execution Time Measurement ==="
echo ""

measure_execution_time "test"
measure_execution_time "quickTest"
measure_execution_time "unitTest"
measure_execution_time "integrationTest"

echo "=== Phase 2: Memory Usage Measurement ==="
echo ""

measure_memory_usage "test"
measure_memory_usage "quickTest"
measure_memory_usage "unitTest"
measure_memory_usage "integrationTest"

# Generate summary report
echo "=== Generating Summary Report ==="
echo ""

cat > "$BASELINE_DIR/baseline_summary_${TIMESTAMP}.md" << EOF
# Test Performance Baseline Report

**Generated:** $(date)
**Project:** GenAI Demo - Test Code Refactoring

## Execution Time Baseline

| Task | Duration (s) | Total Tests | Failed Tests | Skipped Tests |
|------|--------------|-------------|--------------|---------------|
EOF

# Add execution time data
tail -n +2 "$BASELINE_DIR/baseline_metrics_${TIMESTAMP}.csv" | while IFS=, read -r task duration total failed skipped; do
    echo "| $task | $duration | $total | $failed | $skipped |" >> "$BASELINE_DIR/baseline_summary_${TIMESTAMP}.md"
done

cat >> "$BASELINE_DIR/baseline_summary_${TIMESTAMP}.md" << EOF

## Memory Usage Baseline

| Task | Max Heap Usage (MB) |
|------|---------------------|
EOF

# Add memory usage data
tail -n +2 "$BASELINE_DIR/memory_baseline_${TIMESTAMP}.csv" | while IFS=, read -r task heap; do
    heap_mb=$((heap / 1024))
    echo "| $task | $heap_mb |" >> "$BASELINE_DIR/baseline_summary_${TIMESTAMP}.md"
done

cat >> "$BASELINE_DIR/baseline_summary_${TIMESTAMP}.md" << EOF

## Performance Targets

Based on the requirements, the following improvements are targeted:

- **Unit Test Execution Time**: >60% reduction
- **Memory Usage**: >80% reduction for local testing
- **CI/CD Pipeline Time**: >50% improvement
- **Test Reliability**: >99% success rate

## Next Steps

1. Implement test refactoring according to the design
2. Re-run this baseline measurement script after refactoring
3. Compare results using the comparison script
4. Validate that all targets are met

## Baseline Files

- Execution metrics: \`baseline_metrics_${TIMESTAMP}.csv\`
- Memory metrics: \`memory_baseline_${TIMESTAMP}.csv\`
- Detailed logs: \`*_${TIMESTAMP}.log\`
- GC logs: \`*_gc_${TIMESTAMP}.log\`

EOF

echo -e "${GREEN}✓ Baseline measurement complete!${NC}"
echo ""
echo "Summary report: $BASELINE_DIR/baseline_summary_${TIMESTAMP}.md"
echo "Metrics CSV: $BASELINE_DIR/baseline_metrics_${TIMESTAMP}.csv"
echo "Memory CSV: $BASELINE_DIR/memory_baseline_${TIMESTAMP}.csv"
echo ""
echo "To compare with future measurements, run:"
echo "  ./scripts/compare-test-performance.sh $BASELINE_DIR/baseline_metrics_${TIMESTAMP}.csv <new_metrics.csv>"
