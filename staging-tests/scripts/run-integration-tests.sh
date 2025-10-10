#!/bin/bash
set -e

# Integration Test Execution Script
# This script orchestrates the execution of integration tests in staging environment

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
TARGET_HOST="${TARGET_HOST:-localhost:8080}"
TEST_ENVIRONMENT="${TEST_ENVIRONMENT:-staging}"
CATEGORY="${1:-all}"
PARALLEL="${PARALLEL:-false}"
CLEANUP="${CLEANUP:-true}"

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
Usage: $0 [CATEGORY] [OPTIONS]

CATEGORIES:
    all         Run all integration tests (default)
    database    Run database integration tests only
    cache       Run Redis cache integration tests only
    messaging   Run Kafka messaging integration tests only
    monitoring  Run monitoring integration tests only

OPTIONS:
    --parallel      Run tests in parallel
    --no-cleanup    Skip cleanup after tests
    --help         Show this help message

ENVIRONMENT VARIABLES:
    TARGET_HOST     Target application host (default: localhost:8080)
    TEST_ENVIRONMENT Test environment (default: staging)
    PARALLEL        Run tests in parallel (default: false)
    CLEANUP         Cleanup after tests (default: true)

EXAMPLES:
    $0                          # Run all integration tests
    $0 database                 # Run database tests only
    $0 all --parallel          # Run all tests in parallel
    $0 cache --no-cleanup      # Run cache tests without cleanup
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --parallel)
            PARALLEL=true
            shift
            ;;
        --no-cleanup)
            CLEANUP=false
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        database|cache|messaging|monitoring|all)
            CATEGORY=$1
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
mkdir -p "$PROJECT_ROOT/reports/integration"
mkdir -p "$PROJECT_ROOT/logs"

log_info "Starting integration tests..."
log_info "Category: $CATEGORY"
log_info "Target Host: $TARGET_HOST"
log_info "Environment: $TEST_ENVIRONMENT"
log_info "Parallel Execution: $PARALLEL"

# Start infrastructure
log_info "Starting staging infrastructure..."
cd "$PROJECT_ROOT/config"

if ! docker-compose -f docker-compose-staging.yml ps | grep -q "Up"; then
    log_info "Starting Docker Compose services..."
    docker-compose -f docker-compose-staging.yml up -d
    
    # Wait for services to be ready
    log_info "Waiting for services to be ready..."
    "$SCRIPT_DIR/wait-for-services.sh"
else
    log_info "Services are already running"
fi

# Function to run database integration tests
run_database_tests() {
    log_info "Running database integration tests..."
    
    cd "$PROJECT_ROOT/integration/database"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run database tests
    python -m pytest test_database_integration.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/integration/database-results.xml" \
        --html="$PROJECT_ROOT/reports/integration/database-report.html" \
        --self-contained-html \
        2>&1 | tee "$PROJECT_ROOT/logs/database-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Database integration tests passed"
    else
        log_error "Database integration tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Function to run cache integration tests
run_cache_tests() {
    log_info "Running cache integration tests..."
    
    cd "$PROJECT_ROOT/integration/cache"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run cache tests
    python -m pytest test_redis_integration.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/integration/cache-results.xml" \
        --html="$PROJECT_ROOT/reports/integration/cache-report.html" \
        --self-contained-html \
        2>&1 | tee "$PROJECT_ROOT/logs/cache-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Cache integration tests passed"
    else
        log_error "Cache integration tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Function to run messaging integration tests
run_messaging_tests() {
    log_info "Running messaging integration tests..."
    
    cd "$PROJECT_ROOT/integration/messaging"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run messaging tests
    python -m pytest test_kafka_integration.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/integration/messaging-results.xml" \
        --html="$PROJECT_ROOT/reports/integration/messaging-report.html" \
        --self-contained-html \
        2>&1 | tee "$PROJECT_ROOT/logs/messaging-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Messaging integration tests passed"
    else
        log_error "Messaging integration tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Function to run monitoring integration tests
run_monitoring_tests() {
    log_info "Running monitoring integration tests..."
    
    cd "$PROJECT_ROOT/integration/monitoring"
    
    # Install Python dependencies if needed
    if [ -f "requirements.txt" ]; then
        pip install -r requirements.txt > /dev/null 2>&1
    fi
    
    # Run monitoring tests
    python -m pytest test_monitoring_integration.py \
        --verbose \
        --tb=short \
        --junit-xml="$PROJECT_ROOT/reports/integration/monitoring-results.xml" \
        --html="$PROJECT_ROOT/reports/integration/monitoring-report.html" \
        --self-contained-html \
        2>&1 | tee "$PROJECT_ROOT/logs/monitoring-tests.log"
    
    local exit_code=${PIPESTATUS[0]}
    if [ $exit_code -eq 0 ]; then
        log_success "Monitoring integration tests passed"
    else
        log_error "Monitoring integration tests failed (exit code: $exit_code)"
        return $exit_code
    fi
}

# Main test execution
OVERALL_EXIT_CODE=0

if [ "$PARALLEL" = "true" ]; then
    log_info "Running tests in parallel..."
    
    case $CATEGORY in
        all)
            run_database_tests &
            DB_PID=$!
            run_cache_tests &
            CACHE_PID=$!
            run_messaging_tests &
            MSG_PID=$!
            run_monitoring_tests &
            MON_PID=$!
            
            wait $DB_PID || OVERALL_EXIT_CODE=1
            wait $CACHE_PID || OVERALL_EXIT_CODE=1
            wait $MSG_PID || OVERALL_EXIT_CODE=1
            wait $MON_PID || OVERALL_EXIT_CODE=1
            ;;
        database)
            run_database_tests || OVERALL_EXIT_CODE=1
            ;;
        cache)
            run_cache_tests || OVERALL_EXIT_CODE=1
            ;;
        messaging)
            run_messaging_tests || OVERALL_EXIT_CODE=1
            ;;
        monitoring)
            run_monitoring_tests || OVERALL_EXIT_CODE=1
            ;;
    esac
else
    log_info "Running tests sequentially..."
    
    case $CATEGORY in
        all)
            run_database_tests || OVERALL_EXIT_CODE=1
            run_cache_tests || OVERALL_EXIT_CODE=1
            run_messaging_tests || OVERALL_EXIT_CODE=1
            run_monitoring_tests || OVERALL_EXIT_CODE=1
            ;;
        database)
            run_database_tests || OVERALL_EXIT_CODE=1
            ;;
        cache)
            run_cache_tests || OVERALL_EXIT_CODE=1
            ;;
        messaging)
            run_messaging_tests || OVERALL_EXIT_CODE=1
            ;;
        monitoring)
            run_monitoring_tests || OVERALL_EXIT_CODE=1
            ;;
    esac
fi

# Generate consolidated report
log_info "Generating consolidated test report..."
python3 "$SCRIPT_DIR/generate-integration-report.py" "$PROJECT_ROOT/reports/integration"

# Cleanup
if [ "$CLEANUP" = "true" ]; then
    log_info "Cleaning up test environment..."
    cd "$PROJECT_ROOT/config"
    docker-compose -f docker-compose-staging.yml down
    log_success "Cleanup completed"
else
    log_warning "Skipping cleanup (services are still running)"
fi

# Final result
if [ $OVERALL_EXIT_CODE -eq 0 ]; then
    log_success "All integration tests completed successfully!"
    log_info "Reports available in: $PROJECT_ROOT/reports/integration/"
else
    log_error "Some integration tests failed!"
    log_info "Check logs in: $PROJECT_ROOT/logs/"
    log_info "Check reports in: $PROJECT_ROOT/reports/integration/"
fi

exit $OVERALL_EXIT_CODE