#!/bin/bash
set -e

# Cross-Region Test Execution Script
# This script orchestrates cross-region and disaster recovery tests

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
PRIMARY_REGION="${PRIMARY_REGION:-us-east-1}"
SECONDARY_REGION="${SECONDARY_REGION:-us-west-2}"
TEST_TYPE="${1:-all}"
TARGET_HOST="${TARGET_HOST:-localhost:8080}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Help function
show_help() {
    cat << EOF
Usage: $0 [TEST_TYPE] [OPTIONS]

TEST TYPES:
    all                 Run all cross-region tests (default)
    disaster-recovery   Run disaster recovery tests only
    replication        Run data replication tests only
    network-partition  Run network partition tests only

OPTIONS:
    --primary-region    Primary AWS region (default: us-east-1)
    --secondary-region  Secondary AWS region (default: us-west-2)
    --help             Show this help message

ENVIRONMENT VARIABLES:
    PRIMARY_REGION      Primary AWS region (default: us-east-1)
    SECONDARY_REGION    Secondary AWS region (default: us-west-2)
    TARGET_HOST        Target application host (default: localhost:8080)

EXAMPLES:
    $0                                    # Run all cross-region tests
    $0 disaster-recovery                  # Run DR tests only
    $0 replication --primary-region us-east-1 --secondary-region eu-west-1
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --primary-region)
            PRIMARY_REGION="$2"
            shift 2
            ;;
        --secondary-region)
            SECONDARY_REGION="$2"
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        all|disaster-recovery|replication|network-partition)
            TEST_TYPE=$1
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Ensure required directories exist
mkdir -p "$PROJECT_ROOT/reports/cross-region"
mkdir -p "$PROJECT_ROOT/logs"

log_info "Starting cross-region tests..."
log_info "Test Type: $TEST_TYPE"
log_info "Primary Region: $PRIMARY_REGION"
log_info "Secondary Region: $SECONDARY_REGION"
log_info "Target Host: $TARGET_HOST"

# Function to check AWS CLI configuration
check_aws_config() {
    log_info "Checking AWS CLI configuration..."
    
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI not found. Please install AWS CLI."
        return 1
    fi
    
    if ! aws sts get-caller-identity > /dev/null 2>&1; then
        log_error "AWS CLI not configured. Please run 'aws configure'."
        return 1
    fi
    
    log_success "AWS CLI is configured"
    return 0
}

# Function to run disaster recovery tests
run_disaster_recovery_tests() {
    log_info "Running disaster recovery tests..."
    
    cd "$PROJECT_ROOT/cross-region/disaster-recovery"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run disaster recovery tests
    python -m pytest test_disaster_recovery.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/cross-region/disaster-recovery-results.xml" \
        --html="$PROJECT_ROOT/reports/cross-region/disaster-recovery-report.html" \
        --self-contained-html \
        -s \
        --primary-region="$PRIMARY_REGION" \
        --secondary-region="$SECONDARY_REGION" \
        2>&1 | tee "$PROJECT_ROOT/logs/disaster-recovery-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Disaster recovery tests passed"
    else
        log_error "Disaster recovery tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Function to run replication tests
run_replication_tests() {
    log_info "Running data replication tests..."
    
    cd "$PROJECT_ROOT/cross-region/replication"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run replication tests
    python -m pytest test_replication.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/cross-region/replication-results.xml" \
        --html="$PROJECT_ROOT/reports/cross-region/replication-report.html" \
        --self-contained-html \
        -s \
        --primary-region="$PRIMARY_REGION" \
        --secondary-region="$SECONDARY_REGION" \
        2>&1 | tee "$PROJECT_ROOT/logs/replication-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Data replication tests passed"
    else
        log_error "Data replication tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Function to run network partition tests
run_network_partition_tests() {
    log_info "Running network partition tests..."
    
    cd "$PROJECT_ROOT/cross-region"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run network partition tests
    python -m pytest test_network_partition.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/cross-region/network-partition-results.xml" \
        --html="$PROJECT_ROOT/reports/cross-region/network-partition-report.html" \
        --self-contained-html \
        -s \
        --primary-region="$PRIMARY_REGION" \
        --secondary-region="$SECONDARY_REGION" \
        2>&1 | tee "$PROJECT_ROOT/logs/network-partition-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Network partition tests passed"
    else
        log_error "Network partition tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Function to setup cross-region test environment
setup_cross_region_environment() {
    log_info "Setting up cross-region test environment..."
    
    # Create test resources in both regions if needed
    # This is a placeholder for actual AWS resource creation
    log_info "Creating test resources in $PRIMARY_REGION..."
    log_info "Creating test resources in $SECONDARY_REGION..."
    
    # Validate connectivity between regions
    log_info "Validating cross-region connectivity..."
    
    log_success "Cross-region environment setup completed"
}

# Function to cleanup cross-region test environment
cleanup_cross_region_environment() {
    log_info "Cleaning up cross-region test environment..."
    
    # Cleanup test resources in both regions
    # This is a placeholder for actual AWS resource cleanup
    log_info "Cleaning up test resources in $PRIMARY_REGION..."
    log_info "Cleaning up test resources in $SECONDARY_REGION..."
    
    log_success "Cross-region environment cleanup completed"
}

# Main test execution
OVERALL_EXIT_CODE=0

# Check AWS configuration
check_aws_config || exit 1

# Setup cross-region environment
setup_cross_region_environment

# Trap to ensure cleanup on exit
trap 'cleanup_cross_region_environment' EXIT

case $TEST_TYPE in
    all)
        log_info "Running all cross-region test types..."
        
        run_disaster_recovery_tests || OVERALL_EXIT_CODE=1
        run_replication_tests || OVERALL_EXIT_CODE=1
        run_network_partition_tests || OVERALL_EXIT_CODE=1
        ;;
    disaster-recovery)
        run_disaster_recovery_tests || OVERALL_EXIT_CODE=1
        ;;
    replication)
        run_replication_tests || OVERALL_EXIT_CODE=1
        ;;
    network-partition)
        run_network_partition_tests || OVERALL_EXIT_CODE=1
        ;;
    *)
        log_error "Unknown test type: $TEST_TYPE"
        show_help
        exit 1
        ;;
esac

# Generate consolidated report
log_info "Generating consolidated cross-region test report..."
python3 "$SCRIPT_DIR/generate-cross-region-report.py" "$PROJECT_ROOT/reports/cross-region"

# Cleanup
cleanup_cross_region_environment

# Final result
if [ $OVERALL_EXIT_CODE -eq 0 ]; then
    log_success "All cross-region tests completed successfully!"
    log_info "Reports available in: $PROJECT_ROOT/reports/cross-region/"
else
    log_error "Some cross-region tests failed!"
    log_info "Check logs in: $PROJECT_ROOT/logs/"
    log_info "Check reports in: $PROJECT_ROOT/reports/cross-region/"
fi

exit $OVERALL_EXIT_CODE