#!/bin/bash

# Performance and Reliability Test Execution Script
# This script runs the performance and reliability tests multiple times
# to validate consistency and collect performance metrics

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESULTS_DIR="$PROJECT_ROOT/build/reports/performance-reliability"
LOG_FILE="$RESULTS_DIR/test-execution.log"
ITERATIONS=3
CONCURRENT_RUNS=2

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

# Initialize results directory
init_results_dir() {
    log_info "Initializing results directory: $RESULTS_DIR"
    mkdir -p "$RESULTS_DIR"
    
    # Clear previous log
    > "$LOG_FILE"
    
    log_info "Performance and Reliability Test Execution Started at $(date)"
    log_info "Project Root: $PROJECT_ROOT"
    log_info "Results Directory: $RESULTS_DIR"
}

# Check system resources
check_system_resources() {
    log_info "Checking system resources..."
    
    # Check available memory
    if command -v free >/dev/null 2>&1; then
        AVAILABLE_MEMORY=$(free -m | awk 'NR==2{printf "%.1f", $7/1024}')
        log_info "Available Memory: ${AVAILABLE_MEMORY}GB"
        
        if (( $(echo "$AVAILABLE_MEMORY < 4.0" | bc -l) )); then
            log_warning "Low available memory detected. Tests may run slower."
        fi
    fi
    
    # Check disk space
    AVAILABLE_DISK=$(df -h "$PROJECT_ROOT" | awk 'NR==2 {print $4}')
    log_info "Available Disk Space: $AVAILABLE_DISK"
    
    # Check Java version
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1)
        log_info "Java Version: $JAVA_VERSION"
    fi
}

# Run single test iteration
run_single_iteration() {
    local iteration=$1
    local test_type=$2
    
    log_info "Running iteration $iteration for $test_type tests..."
    
    local start_time=$(date +%s)
    local iteration_log="$RESULTS_DIR/iteration-${iteration}-${test_type}.log"
    
    case "$test_type" in
        "performance-reliability")
            if (cd "$PROJECT_ROOT" && ./gradlew :app:test --tests "solid.humank.genaidemo.integration.PerformanceReliabilityTest" \
                --info --stacktrace > "$iteration_log" 2>&1); then
                local end_time=$(date +%s)
                local duration=$((end_time - start_time))
                log_success "Iteration $iteration completed successfully in ${duration}s"
                echo "$duration" > "$RESULTS_DIR/iteration-${iteration}-duration.txt"
                return 0
            else
                local end_time=$(date +%s)
                local duration=$((end_time - start_time))
                log_error "Iteration $iteration failed after ${duration}s"
                echo "FAILED" > "$RESULTS_DIR/iteration-${iteration}-duration.txt"
                return 1
            fi
            ;;
        "simple-e2e")
            if (cd "$PROJECT_ROOT" && ./gradlew :app:test --tests "solid.humank.genaidemo.integration.SimpleEndToEndValidationTest" \
                --info --stacktrace > "$iteration_log" 2>&1); then
                local end_time=$(date +%s)
                local duration=$((end_time - start_time))
                log_success "Simple E2E iteration $iteration completed in ${duration}s"
                return 0
            else
                log_error "Simple E2E iteration $iteration failed"
                return 1
            fi
            ;;
        "full-e2e")
            if (cd "$PROJECT_ROOT" && ./gradlew :app:test --tests "solid.humank.genaidemo.integration.EndToEndIntegrationTest" \
                --info --stacktrace > "$iteration_log" 2>&1); then
                local end_time=$(date +%s)
                local duration=$((end_time - start_time))
                log_success "Full E2E iteration $iteration completed in ${duration}s"
                return 0
            else
                log_error "Full E2E iteration $iteration failed"
                return 1
            fi
            ;;
    esac
}

# Run consistency tests
run_consistency_tests() {
    log_info "Running consistency tests ($ITERATIONS iterations)..."
    
    local success_count=0
    local total_duration=0
    
    for i in $(seq 1 $ITERATIONS); do
        if run_single_iteration "$i" "performance-reliability"; then
            ((success_count++))
            
            # Read duration if available
            if [[ -f "$RESULTS_DIR/iteration-${i}-duration.txt" ]]; then
                local duration=$(cat "$RESULTS_DIR/iteration-${i}-duration.txt")
                if [[ "$duration" != "FAILED" ]]; then
                    total_duration=$((total_duration + duration))
                fi
            fi
        fi
        
        # Small delay between iterations
        sleep 2
    done
    
    local success_rate=$((success_count * 100 / ITERATIONS))
    local avg_duration=0
    if [[ $success_count -gt 0 ]]; then
        avg_duration=$((total_duration / success_count))
    fi
    
    log_info "Consistency Test Results:"
    log_info "  Success Rate: ${success_rate}% (${success_count}/${ITERATIONS})"
    log_info "  Average Duration: ${avg_duration}s"
    
    # Save results
    cat > "$RESULTS_DIR/consistency-results.txt" << EOF
Consistency Test Results
========================
Total Iterations: $ITERATIONS
Successful: $success_count
Failed: $((ITERATIONS - success_count))
Success Rate: ${success_rate}%
Average Duration: ${avg_duration}s
EOF

    if [[ $success_rate -ge 95 ]]; then
        log_success "Consistency tests passed (≥95% success rate)"
        return 0
    else
        log_error "Consistency tests failed (<95% success rate)"
        return 1
    fi
}

# Run concurrent tests
run_concurrent_tests() {
    log_info "Running concurrent tests ($CONCURRENT_RUNS parallel executions)..."
    
    local pids=()
    local start_time=$(date +%s)
    
    # Start concurrent test executions
    for i in $(seq 1 $CONCURRENT_RUNS); do
        (
            run_single_iteration "concurrent-$i" "simple-e2e"
            echo $? > "$RESULTS_DIR/concurrent-${i}-result.txt"
        ) &
        pids+=($!)
    done
    
    # Wait for all concurrent tests to complete
    local success_count=0
    for pid in "${pids[@]}"; do
        if wait "$pid"; then
            ((success_count++))
        fi
    done
    
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    
    log_info "Concurrent Test Results:"
    log_info "  Parallel Executions: $CONCURRENT_RUNS"
    log_info "  Successful: $success_count"
    log_info "  Total Duration: ${total_duration}s"
    
    # Save results
    cat > "$RESULTS_DIR/concurrent-results.txt" << EOF
Concurrent Test Results
=======================
Parallel Executions: $CONCURRENT_RUNS
Successful: $success_count
Failed: $((CONCURRENT_RUNS - success_count))
Total Duration: ${total_duration}s
EOF

    if [[ $success_count -eq $CONCURRENT_RUNS ]]; then
        log_success "Concurrent tests passed (all executions successful)"
        return 0
    else
        log_error "Concurrent tests failed (some executions failed)"
        return 1
    fi
}

# Run memory stress test
run_memory_stress_test() {
    log_info "Running memory stress test..."
    
    # Run test with memory monitoring
    if (cd "$PROJECT_ROOT" && ./gradlew :app:test --tests "solid.humank.genaidemo.integration.PerformanceReliabilityTest.shouldValidateMemoryUsagePatterns" \
        --info --stacktrace > "$RESULTS_DIR/memory-stress.log" 2>&1); then
        log_success "Memory stress test completed successfully"
        return 0
    else
        log_error "Memory stress test failed"
        return 1
    fi
}

# Run performance benchmark
run_performance_benchmark() {
    log_info "Running performance benchmark..."
    
    # Run performance-focused tests
    if (cd "$PROJECT_ROOT" && ./gradlew :app:test --tests "solid.humank.genaidemo.integration.PerformanceReliabilityTest.shouldValidateLoadTestPerformance" \
        --info --stacktrace > "$RESULTS_DIR/performance-benchmark.log" 2>&1); then
        log_success "Performance benchmark completed successfully"
        return 0
    else
        log_error "Performance benchmark failed"
        return 1
    fi
}

# Generate comprehensive report
generate_report() {
    log_info "Generating comprehensive performance and reliability report..."
    
    local report_file="$RESULTS_DIR/comprehensive-report.md"
    
    cat > "$report_file" << EOF
# Performance and Reliability Test Report

Generated: $(date)

## Test Environment

- Project Root: $PROJECT_ROOT
- Java Version: $(java -version 2>&1 | head -n 1)
- Available Memory: $(free -h 2>/dev/null | awk 'NR==2{print $7}' || echo "N/A")
- Available Disk: $(df -h "$PROJECT_ROOT" | awk 'NR==2 {print $4}')

## Test Execution Summary

### Consistency Tests
EOF

    if [[ -f "$RESULTS_DIR/consistency-results.txt" ]]; then
        cat "$RESULTS_DIR/consistency-results.txt" >> "$report_file"
    else
        echo "Consistency tests were not executed" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

### Concurrent Tests
EOF

    if [[ -f "$RESULTS_DIR/concurrent-results.txt" ]]; then
        cat "$RESULTS_DIR/concurrent-results.txt" >> "$report_file"
    else
        echo "Concurrent tests were not executed" >> "$report_file"
    fi
    
    cat >> "$report_file" << EOF

## Test Files Generated

EOF
    
    # List all generated files
    find "$RESULTS_DIR" -type f -name "*.log" -o -name "*.txt" | while read -r file; do
        local filename=$(basename "$file")
        local filesize=$(du -h "$file" | cut -f1)
        echo "- $filename ($filesize)" >> "$report_file"
    done
    
    cat >> "$report_file" << EOF

## Recommendations

### Performance Optimization
- Monitor memory usage patterns during test execution
- Ensure proper resource cleanup after test completion
- Validate response times meet requirements (<2s for most endpoints)

### Reliability Improvements
- Maintain >95% success rate for consistency tests
- Ensure concurrent test execution doesn't cause failures
- Implement proper error handling and retry mechanisms

### Monitoring
- Set up continuous performance monitoring
- Implement alerting for performance regressions
- Track memory usage trends over time

## Next Steps

1. Review individual test logs for detailed analysis
2. Address any performance bottlenecks identified
3. Implement continuous performance testing in CI/CD pipeline
4. Set up performance regression detection

EOF

    log_success "Comprehensive report generated: $report_file"
}

# Cleanup function
cleanup() {
    log_info "Cleaning up test artifacts..."
    
    # Kill any remaining background processes
    jobs -p | xargs -r kill 2>/dev/null || true
    
    # Clean up temporary files
    (cd "$PROJECT_ROOT" && ./gradlew clean >/dev/null 2>&1) || true
    
    log_info "Cleanup completed"
}

# Main execution function
main() {
    log_info "Starting Performance and Reliability Test Suite"
    
    # Initialize
    init_results_dir
    check_system_resources
    
    # Set up cleanup trap
    trap cleanup EXIT
    
    local overall_success=true
    
    # Run test suites
    if ! run_consistency_tests; then
        overall_success=false
    fi
    
    if ! run_concurrent_tests; then
        overall_success=false
    fi
    
    if ! run_memory_stress_test; then
        overall_success=false
    fi
    
    if ! run_performance_benchmark; then
        overall_success=false
    fi
    
    # Generate report
    generate_report
    
    # Final results
    if $overall_success; then
        log_success "All performance and reliability tests completed successfully!"
        log_info "Results available in: $RESULTS_DIR"
        exit 0
    else
        log_error "Some performance and reliability tests failed!"
        log_info "Check logs in: $RESULTS_DIR"
        exit 1
    fi
}

# Script entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi