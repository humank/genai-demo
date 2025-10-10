#!/bin/bash
set -e

# Service Readiness Validation Script
# This script waits for all staging services to be ready before running tests

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
MAX_WAIT_TIME=${MAX_WAIT_TIME:-300}  # 5 minutes
CHECK_INTERVAL=${CHECK_INTERVAL:-5}  # 5 seconds

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

# Function to check if a service is ready
check_service() {
    local service_name=$1
    local check_command=$2
    local max_attempts=$((MAX_WAIT_TIME / CHECK_INTERVAL))
    local attempt=1
    
    log_info "Checking $service_name readiness..."
    
    while [ $attempt -le $max_attempts ]; do
        if eval "$check_command" > /dev/null 2>&1; then
            log_success "$service_name is ready"
            return 0
        fi
        
        if [ $((attempt % 6)) -eq 0 ]; then  # Log every 30 seconds
            log_info "Waiting for $service_name... (attempt $attempt/$max_attempts)"
        fi
        
        sleep $CHECK_INTERVAL
        attempt=$((attempt + 1))
    done
    
    log_error "$service_name failed to become ready within $MAX_WAIT_TIME seconds"
    return 1
}

# Function to check PostgreSQL
check_postgres() {
    check_service "PostgreSQL Primary" "docker exec postgres-primary pg_isready -U postgres -d genai_demo_staging"
    local primary_status=$?
    
    check_service "PostgreSQL Replica" "docker exec postgres-replica pg_isready -U postgres -d genai_demo_staging"
    local replica_status=$?
    
    return $((primary_status + replica_status))
}

# Function to check Redis cluster
check_redis() {
    check_service "Redis Master" "docker exec redis-master redis-cli -a staging_redis_password ping"
    local master_status=$?
    
    check_service "Redis Replica 1" "docker exec redis-replica-1 redis-cli -a staging_redis_password ping"
    local replica1_status=$?
    
    check_service "Redis Replica 2" "docker exec redis-replica-2 redis-cli -a staging_redis_password ping"
    local replica2_status=$?
    
    # Check Sentinel instances
    check_service "Redis Sentinel 1" "docker exec redis-sentinel-1 redis-cli -p 26379 ping"
    local sentinel1_status=$?
    
    check_service "Redis Sentinel 2" "docker exec redis-sentinel-2 redis-cli -p 26379 ping"
    local sentinel2_status=$?
    
    check_service "Redis Sentinel 3" "docker exec redis-sentinel-3 redis-cli -p 26379 ping"
    local sentinel3_status=$?
    
    return $((master_status + replica1_status + replica2_status + sentinel1_status + sentinel2_status + sentinel3_status))
}

# Function to check Kafka cluster
check_kafka() {
    check_service "Zookeeper" "docker exec zookeeper /bin/bash -c 'echo ruok | nc localhost 2181'"
    local zk_status=$?
    
    check_service "Kafka Broker 1" "docker exec kafka-1 kafka-broker-api-versions --bootstrap-server localhost:9092"
    local kafka1_status=$?
    
    check_service "Kafka Broker 2" "docker exec kafka-2 kafka-broker-api-versions --bootstrap-server localhost:9093"
    local kafka2_status=$?
    
    check_service "Kafka Broker 3" "docker exec kafka-3 kafka-broker-api-versions --bootstrap-server localhost:9094"
    local kafka3_status=$?
    
    return $((zk_status + kafka1_status + kafka2_status + kafka3_status))
}

# Function to check DynamoDB Local
check_dynamodb() {
    check_service "DynamoDB Local" "curl -f http://localhost:8000/"
}

# Function to check LocalStack
check_localstack() {
    check_service "LocalStack" "curl -f http://localhost:4566/_localstack/health"
}

# Function to check monitoring stack
check_monitoring() {
    check_service "Prometheus" "curl -f http://localhost:9090/-/healthy"
    local prometheus_status=$?
    
    check_service "Grafana" "curl -f http://localhost:3000/api/health"
    local grafana_status=$?
    
    check_service "Node Exporter" "curl -f http://localhost:9100/metrics"
    local node_exporter_status=$?
    
    return $((prometheus_status + grafana_status + node_exporter_status))
}

# Function to validate service connectivity
validate_connectivity() {
    log_info "Validating service connectivity..."
    
    # Test PostgreSQL connection
    if docker exec postgres-primary psql -U postgres -d genai_demo_staging -c "SELECT 1;" > /dev/null 2>&1; then
        log_success "PostgreSQL connection validated"
    else
        log_error "PostgreSQL connection failed"
        return 1
    fi
    
    # Test Redis connection
    if docker exec redis-master redis-cli -a staging_redis_password set test_key test_value > /dev/null 2>&1; then
        if docker exec redis-master redis-cli -a staging_redis_password get test_key | grep -q "test_value"; then
            docker exec redis-master redis-cli -a staging_redis_password del test_key > /dev/null 2>&1
            log_success "Redis connection validated"
        else
            log_error "Redis read/write validation failed"
            return 1
        fi
    else
        log_error "Redis connection failed"
        return 1
    fi
    
    # Test Kafka connection by creating and listing topics
    if docker exec kafka-1 kafka-topics --bootstrap-server localhost:9092 --create --topic test-connectivity --partitions 1 --replication-factor 1 > /dev/null 2>&1; then
        if docker exec kafka-1 kafka-topics --bootstrap-server localhost:9092 --list | grep -q "test-connectivity"; then
            docker exec kafka-1 kafka-topics --bootstrap-server localhost:9092 --delete --topic test-connectivity > /dev/null 2>&1
            log_success "Kafka connection validated"
        else
            log_error "Kafka topic validation failed"
            return 1
        fi
    else
        log_error "Kafka connection failed"
        return 1
    fi
    
    return 0
}

# Main execution
log_info "Starting service readiness checks..."
log_info "Maximum wait time: ${MAX_WAIT_TIME} seconds"
log_info "Check interval: ${CHECK_INTERVAL} seconds"

OVERALL_STATUS=0

# Check all services
log_info "=== Checking Database Services ==="
check_postgres || OVERALL_STATUS=1

log_info "=== Checking Cache Services ==="
check_redis || OVERALL_STATUS=1

log_info "=== Checking Messaging Services ==="
check_kafka || OVERALL_STATUS=1

log_info "=== Checking AWS Services ==="
check_dynamodb || OVERALL_STATUS=1
check_localstack || OVERALL_STATUS=1

log_info "=== Checking Monitoring Services ==="
check_monitoring || OVERALL_STATUS=1

# Validate connectivity if all services are ready
if [ $OVERALL_STATUS -eq 0 ]; then
    log_info "=== Validating Service Connectivity ==="
    validate_connectivity || OVERALL_STATUS=1
fi

# Final result
if [ $OVERALL_STATUS -eq 0 ]; then
    log_success "All services are ready and validated!"
    log_info "You can now run integration tests"
else
    log_error "Some services failed readiness checks!"
    log_info "Check Docker Compose logs: docker-compose -f config/docker-compose-staging.yml logs"
fi

exit $OVERALL_STATUS