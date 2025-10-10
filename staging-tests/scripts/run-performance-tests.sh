#!/bin/bash

# Gatling Performance Tests Execution Script
# This script orchestrates the execution of all performance tests using Gatling

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
GATLING_HOME="${GATLING_HOME:-/opt/gatling}"
SIMULATIONS_DIR="$PROJECT_ROOT/performance/gatling/simulations"
RESULTS_DIR="$PROJECT_ROOT/reports/performance"
TARGET_HOST="${TARGET_HOST:-localhost:8080}"
TEST_SCENARIO="${1:-all}"  # Default to 'all' if no scenario specified

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $(date '+%Y-%m-%d %H:%M:%S') - $1"
}

# Ensure results directory exists
mkdir -p "$RESULTS_DIR"

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Gatling is installed
    if [ ! -d "$GATLING_HOME" ]; then
        log_error "Gatling not found at $GATLING_HOME"
        log_info "Please install Gatling or set GATLING_HOME environment variable"
        exit 1
    fi
    
    # Check if Gatling executable exists
    if [ ! -f "$GATLING_HOME/bin/gatling.sh" ]; then
        log_error "Gatling executable not found at $GATLING_HOME/bin/gatling.sh"
        exit 1
    fi
    
    # Check if target application is running
    if ! curl -f "http://$TARGET_HOST/actuator/health" > /dev/null 2>&1; then
        log_warning "Target application at $TARGET_HOST may not be running"
        log_warning "Performance tests may fail if the application is not available"
    else
        log_success "Target application at $TARGET_HOST is responding"
    fi
    
    log_success "Prerequisites check completed"
}

# Function to run Gatling test
run_gatling_test() {
    local simulation_class=$1
    local test_name=$2
    local users=${3:-100}
    local duration=${4:-300}
    local description=$5
    
    log_info "Running $description..."
    log_info "Simulation: $simulation_class"
    log_info "Users: $users, Duration: ${duration}s, Target: $TARGET_HOST"
    
    cd "$GATLING_HOME"
    
    # Create unique run directory
    local run_timestamp=$(date '+%Y%m%d_%H%M%S')
    local run_dir="$RESULTS_DIR/${test_name}_${run_timestamp}"
    
    # Run Gatling simulation
    ./bin/gatling.sh \
        -sf "$SIMULATIONS_DIR" \
        -s "$simulation_class" \
        -rf "$run_dir" \
        -rd "$test_name" \
        -Dusers="$users" \
        -Dduration="${duration}s" \
        -Dhost="http://$TARGET_HOST" \
        -Dgatling.core.outputDirectoryBaseName="$test_name" \
        2>&1 | tee "$PROJECT_ROOT/logs/gatling-${test_name}.log"
    
    local exit_code=${PIPESTATUS[0]}
    
    if [ $exit_code -eq 0 ]; then
        log_success "$description completed successfully"
        log_info "Report available at: $run_dir/index.html"
        return 0
    else
        log_error "$description failed with exit code $exit_code"
        return $exit_code
    fi
}

# Function to run normal load test
run_normal_load_test() {
    run_gatling_test "NormalLoadSimulation" "normal-load" 100 300 "Normal Load Test (100 users, 5 minutes)"
}

# Function to run peak load test
run_peak_load_test() {
    run_gatling_test "PeakLoadSimulation" "peak-load" 500 600 "Peak Load Test (500 users, 10 minutes)"
}

# Function to run stress test
run_stress_test() {
    run_gatling_test "StressTestSimulation" "stress-test" 1000 900 "Stress Test (1000 users, 15 minutes)"
}

# Function to run endurance test
run_endurance_test() {
    run_gatling_test "EnduranceTestSimulation" "endurance-test" 200 3600 "Endurance Test (200 users, 1 hour)"
}

# Function to run spike test
run_spike_test() {
    run_gatling_test "SpikeTestSimulation" "spike-test" 2000 180 "Spike Test (2000 users, 3 minutes)"
}

# Function to start performance monitoring
start_performance_monitoring() {
    log_info "Starting performance monitoring..."
    
    # Start system monitoring in background
    python3 "$SCRIPT_DIR/performance-monitor.py" \
        --duration 7200 \
        --output "$RESULTS_DIR/system-metrics.json" \
        > "$PROJECT_ROOT/logs/performance-monitor.log" 2>&1 &
    
    local monitor_pid=$!
    echo $monitor_pid > "$PROJECT_ROOT/logs/monitor.pid"
    
    log_info "Performance monitoring started (PID: $monitor_pid)"
}

# Function to stop performance monitoring
stop_performance_monitoring() {
    if [ -f "$PROJECT_ROOT/logs/monitor.pid" ]; then
        local monitor_pid=$(cat "$PROJECT_ROOT/logs/monitor.pid")
        if kill -0 $monitor_pid 2>/dev/null; then
            log_info "Stopping performance monitoring (PID: $monitor_pid)..."
            kill $monitor_pid
            rm -f "$PROJECT_ROOT/logs/monitor.pid"
            log_success "Performance monitoring stopped"
        fi
    fi
}

# Function to generate consolidated performance report
generate_consolidated_report() {
    log_info "Generating consolidated performance report..."
    
    python3 "$SCRIPT_DIR/generate-gatling-report.py" \
        --results-dir "$RESULTS_DIR" \
        --output "$RESULTS_DIR/consolidated-performance-report.html" \
        --include-system-metrics
    
    log_success "Consolidated performance report generated: $RESULTS_DIR/consolidated-performance-report.html"
}

# Function to run all performance tests
run_all_performance_tests() {
    local failed_tests=()
    
    log_info "Running complete performance test suite..."
    
    # Start monitoring
    start_performance_monitoring
    
    # Run tests sequentially to avoid resource conflicts
    run_normal_load_test || failed_tests+=("normal-load")
    sleep 30  # Cool-down period between tests
    
    run_peak_load_test || failed_tests+=("peak-load")
    sleep 30
    
    run_stress_test || failed_tests+=("stress-test")
    sleep 30
    
    run_endurance_test || failed_tests+=("endurance-test")
    sleep 30
    
    run_spike_test || failed_tests+=("spike-test")
    
    # Stop monitoring
    stop_performance_monitoring
    
    # Report results
    if [ ${#failed_tests[@]} -eq 0 ]; then
        log_success "All performance tests completed successfully!"
        return 0
    else
        log_error "The following performance tests failed: ${failed_tests[*]}"
        return 1
    fi
}

# Function to display usage information
show_usage() {
    echo "Usage: $0 [SCENARIO] [OPTIONS]"
    echo ""
    echo "Available scenarios:"
    echo "  all           - Run all performance test scenarios"
    echo "  normal-load   - Run normal load test (100 users, 5 minutes)"
    echo "  peak-load     - Run peak load test (500 users, 10 minutes)"
    echo "  stress-test   - Run stress test (1000 users, 15 minutes)"
    echo "  endurance     - Run endurance test (200 users, 1 hour)"
    echo "  spike-test    - Run spike test (2000 users, 3 minutes)"
    echo ""
    echo "Options:"
    echo "  --users N     - Override number of concurrent users"
    echo "  --duration N  - Override test duration in seconds"
    echo "  --host HOST   - Override target host (default: localhost:8080)"
    echo ""
    echo "Examples:"
    echo "  $0 normal-load"
    echo "  $0 stress-test --users 500 --duration 600"
    echo "  $0 all --host staging.example.com:8080"
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --users)
                CUSTOM_USERS="$2"
                shift 2
                ;;
            --duration)
                CUSTOM_DURATION="$2"
                shift 2
                ;;
            --host)
                TARGET_HOST="$2"
                shift 2
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                if [ -z "$TEST_SCENARIO" ] || [ "$TEST_SCENARIO" = "all" ]; then
                    TEST_SCENARIO="$1"
                fi
                shift
                ;;
        esac
    done
}

# Main execution
main() {
    log_info "Starting performance test execution"
    log_info "Scenario: $TEST_SCENARIO"
    log_info "Target host: $TARGET_HOST"
    
    # Check prerequisites
    check_prerequisites
    
    # Execute based on scenario
    case "$TEST_SCENARIO" in
        "normal-load")
            if [ -n "$CUSTOM_USERS" ] && [ -n "$CUSTOM_DURATION" ]; then
                run_gatling_test "NormalLoadSimulation" "normal-load" "$CUSTOM_USERS" "$CUSTOM_DURATION" "Custom Normal Load Test"
            else
                run_normal_load_test
            fi
            ;;
        "peak-load")
            if [ -n "$CUSTOM_USERS" ] && [ -n "$CUSTOM_DURATION" ]; then
                run_gatling_test "PeakLoadSimulation" "peak-load" "$CUSTOM_USERS" "$CUSTOM_DURATION" "Custom Peak Load Test"
            else
                run_peak_load_test
            fi
            ;;
        "stress-test")
            if [ -n "$CUSTOM_USERS" ] && [ -n "$CUSTOM_DURATION" ]; then
                run_gatling_test "StressTestSimulation" "stress-test" "$CUSTOM_USERS" "$CUSTOM_DURATION" "Custom Stress Test"
            else
                run_stress_test
            fi
            ;;
        "endurance")
            if [ -n "$CUSTOM_USERS" ] && [ -n "$CUSTOM_DURATION" ]; then
                run_gatling_test "EnduranceTestSimulation" "endurance-test" "$CUSTOM_USERS" "$CUSTOM_DURATION" "Custom Endurance Test"
            else
                run_endurance_test
            fi
            ;;
        "spike-test")
            if [ -n "$CUSTOM_USERS" ] && [ -n "$CUSTOM_DURATION" ]; then
                run_gatling_test "SpikeTestSimulation" "spike-test" "$CUSTOM_USERS" "$CUSTOM_DURATION" "Custom Spike Test"
            else
                run_spike_test
            fi
            ;;
        "all")
            run_all_performance_tests
            ;;
        *)
            log_error "Unknown scenario: $TEST_SCENARIO"
            show_usage
            exit 1
            ;;
    esac
    
    local test_result=$?
    
    # Generate consolidated report
    if [ $test_result -eq 0 ]; then
        generate_consolidated_report
        log_success "Performance test execution completed successfully!"
        log_info "Reports available in: $RESULTS_DIR"
    else
        log_error "Performance test execution failed!"
        log_info "Check logs in: $PROJECT_ROOT/logs"
        exit $test_result
    fi
}

# Parse arguments and execute
parse_arguments "$@"

# Execute main function if script is run directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main
fi