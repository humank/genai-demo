#!/bin/bash

# Redis Development Environment Management Script
# Usage: ./scripts/redis-dev.sh [command]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
}

# Function to start single Redis for development
start_single() {
    print_status "Starting single Redis instance for development..."
    cd "$PROJECT_ROOT"
    
    docker-compose -f docker-compose-redis-dev.yml up -d redis-single
    
    print_success "Single Redis instance started on port 6379"
    print_status "Connection string: redis://localhost:6379"
    print_status "Set REDIS_MODE=SINGLE and SPRING_PROFILES_ACTIVE=local in your environment"
}

# Function to start Redis HA setup for testing
start_ha() {
    print_status "Starting Redis HA setup (Master + Replica + 3 Sentinels)..."
    cd "$PROJECT_ROOT"
    
    docker-compose -f docker-compose-redis-dev.yml --profile ha up -d
    
    print_success "Redis HA setup started"
    print_status "Master: localhost:6380"
    print_status "Replica: localhost:6381"
    print_status "Sentinels: localhost:26379, localhost:26380, localhost:26381"
    print_status "Set REDIS_MODE=SENTINEL and SPRING_PROFILES_ACTIVE=local in your environment"
    print_status "Set REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381"
}

# Function to stop all Redis instances
stop() {
    print_status "Stopping all Redis instances..."
    cd "$PROJECT_ROOT"
    
    docker-compose -f docker-compose-redis-dev.yml --profile ha down
    docker-compose -f docker-compose-redis-dev.yml down
    
    print_success "All Redis instances stopped"
}

# Function to show status
status() {
    print_status "Redis Development Environment Status:"
    echo ""
    
    # Check single Redis
    if docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -q "redis-dev-single"; then
        print_success "Single Redis: Running"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "redis-dev-single"
    else
        print_warning "Single Redis: Not running"
    fi
    
    echo ""
    
    # Check HA setup
    if docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -q "redis-dev-master"; then
        print_success "HA Setup: Running"
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "redis-dev"
    else
        print_warning "HA Setup: Not running"
    fi
}

# Function to test Redis connection
test_connection() {
    print_status "Testing Redis connections..."
    
    # Test single Redis
    if docker ps --format "{{.Names}}" | grep -q "redis-dev-single"; then
        print_status "Testing single Redis (port 6379)..."
        if docker exec redis-dev-single redis-cli ping > /dev/null 2>&1; then
            print_success "Single Redis: Connection OK"
        else
            print_error "Single Redis: Connection failed"
        fi
    fi
    
    # Test HA setup
    if docker ps --format "{{.Names}}" | grep -q "redis-dev-master"; then
        print_status "Testing HA setup..."
        
        # Test master
        if docker exec redis-dev-master redis-cli ping > /dev/null 2>&1; then
            print_success "Master Redis: Connection OK"
        else
            print_error "Master Redis: Connection failed"
        fi
        
        # Test replica
        if docker exec redis-dev-replica redis-cli ping > /dev/null 2>&1; then
            print_success "Replica Redis: Connection OK"
        else
            print_error "Replica Redis: Connection failed"
        fi
        
        # Test sentinels
        for i in 1 2 3; do
            if docker exec redis-dev-sentinel-$i redis-cli -p 26379 ping > /dev/null 2>&1; then
                print_success "Sentinel $i: Connection OK"
            else
                print_error "Sentinel $i: Connection failed"
            fi
        done
    fi
}

# Function to simulate failover (for HA testing)
simulate_failover() {
    if ! docker ps --format "{{.Names}}" | grep -q "redis-dev-master"; then
        print_error "HA setup is not running. Start it first with: $0 start-ha"
        exit 1
    fi
    
    print_warning "Simulating master failure..."
    docker stop redis-dev-master
    
    print_status "Waiting for failover to complete..."
    sleep 10
    
    print_status "Checking sentinel status..."
    docker exec redis-dev-sentinel-1 redis-cli -p 26379 sentinel masters
    
    print_status "Restarting original master (will become replica)..."
    docker start redis-dev-master
    
    print_success "Failover simulation completed"
}

# Function to show logs
logs() {
    local service=${2:-""}
    cd "$PROJECT_ROOT"
    
    if [ -n "$service" ]; then
        docker-compose -f docker-compose-redis-dev.yml logs -f "$service"
    else
        docker-compose -f docker-compose-redis-dev.yml logs -f
    fi
}

# Function to clean up
cleanup() {
    print_status "Cleaning up Redis development environment..."
    cd "$PROJECT_ROOT"
    
    docker-compose -f docker-compose-redis-dev.yml --profile ha down -v
    docker-compose -f docker-compose-redis-dev.yml down -v
    
    print_success "Cleanup completed"
}

# Function to show help
show_help() {
    echo "Redis Development Environment Management"
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start-single    Start single Redis instance (default for development)"
    echo "  start-ha        Start Redis HA setup (master + replica + sentinels)"
    echo "  stop            Stop all Redis instances"
    echo "  status          Show status of Redis instances"
    echo "  test            Test Redis connections"
    echo "  failover        Simulate failover (HA setup only)"
    echo "  logs [service]  Show logs (optionally for specific service)"
    echo "  cleanup         Stop and remove all containers and volumes"
    echo "  help            Show this help message"
    echo ""
    echo "Environment Variables for Spring Boot:"
    echo "  Profile:        SPRING_PROFILES_ACTIVE=local"
    echo "  Single Redis:   REDIS_MODE=SINGLE"
    echo "  HA Redis:       REDIS_MODE=SENTINEL"
    echo "                  REDIS_SENTINEL_NODES=localhost:26379,localhost:26380,localhost:26381"
    echo ""
}

# Main script logic
case "${1:-help}" in
    "start-single")
        check_docker
        start_single
        ;;
    "start-ha")
        check_docker
        start_ha
        ;;
    "stop")
        check_docker
        stop
        ;;
    "status")
        check_docker
        status
        ;;
    "test")
        check_docker
        test_connection
        ;;
    "failover")
        check_docker
        simulate_failover
        ;;
    "logs")
        check_docker
        logs "$@"
        ;;
    "cleanup")
        check_docker
        cleanup
        ;;
    "help"|*)
        show_help
        ;;
esac