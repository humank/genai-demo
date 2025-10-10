# Staging Tests

This directory contains comprehensive integration, performance, and cross-region tests that run in staging environments with real services.

## Directory Structure

```
staging-tests/
├── integration/           # Integration tests with real services
│   ├── database/         # PostgreSQL and Aurora integration tests
│   ├── cache/           # Redis cluster integration tests
│   ├── messaging/       # Kafka integration tests
│   └── monitoring/      # Observability and monitoring tests
├── performance/         # Gatling performance tests
│   ├── simulations/     # Gatling simulation scripts
│   └── scenarios/       # Test scenario configurations
├── cross-region/        # Cross-region and disaster recovery tests
│   ├── disaster-recovery/  # DR scenario tests
│   └── replication/     # Cross-region replication tests
├── config/              # Docker Compose and service configurations
│   ├── docker-compose-staging.yml
│   └── service-configs/ # Individual service configurations
└── scripts/             # Automation and execution scripts
    ├── run-integration-tests.sh
    ├── run-performance-tests.sh
    └── run-cross-region-tests.sh
```

## Test Categories

### Integration Tests
- **Database**: Connection pooling, failover, performance validation
- **Cache**: Redis cluster operations, Sentinel failover scenarios
- **Messaging**: Kafka throughput, partition rebalancing, cross-region replication
- **Monitoring**: Health checks, metrics collection, alerting validation

### Performance Tests
- **Load Testing**: Normal and peak load scenarios using Gatling
- **Stress Testing**: System breaking point identification
- **Endurance Testing**: Long-running stability validation
- **Baseline Testing**: Performance regression detection

### Cross-Region Tests
- **Data Consistency**: Cross-region data replication and consistency validation
- **Failover Scenarios**: Complete and partial region failure testing
- **Load Balancing**: Geographic, weighted, health-based, and capacity-based routing
- **Business Flows**: End-to-end business workflows across multiple regions
- **Disaster Recovery**: Multi-region failover scenarios
- **Network Partitioning**: Split-brain and network failure scenarios

## Execution

### Prerequisites
- Docker and Docker Compose installed
- Python 3.11+ with required packages
- Gatling installed for performance tests
- AWS CLI configured for cross-region tests

### Running Tests

```bash
# Run all integration tests
./scripts/run-integration-tests.sh

# Run performance tests
./scripts/run-performance-tests.sh

# Run cross-region tests
./scripts/run-cross-region-tests.sh

# Run specific test category
./scripts/run-integration-tests.sh --category database
./scripts/run-performance-tests.sh --scenario normal-load

# Run specific cross-region tests
python3 cross-region/test_cross_region_data_consistency.py
python3 cross-region/test_failover_scenarios.py
python3 cross-region/test_load_balancing.py
python3 cross-region/test_end_to_end_business_flow.py
```

### Cross-Region Test Details

#### Data Consistency Tests (`test_cross_region_data_consistency.py`)
Tests data replication and consistency across multiple AWS regions:
- **Write and Replicate**: Validates data written to one region replicates to others within 100ms (P99)
- **Concurrent Writes**: Tests concurrent writes to multiple regions with eventual consistency
- **Conflict Resolution**: Validates Last-Write-Wins (LWW) conflict resolution strategy

**Configuration**:
```python
config = TestConfig(
    primary_region="us-east-1",
    secondary_regions=["us-west-2", "eu-west-1"],
    max_replication_delay_ms=100,  # P99 target
    api_endpoints={
        "us-east-1": "https://api-us-east-1.example.com",
        "us-west-2": "https://api-us-west-2.example.com",
        "eu-west-1": "https://api-eu-west-1.example.com"
    }
)
```

#### Failover Scenarios Tests (`test_failover_scenarios.py`)
Tests failover mechanisms and system resilience:
- **Complete Region Failure**: Validates automatic failover with RTO < 2 minutes
- **Partial Service Failure**: Tests graceful degradation and traffic routing
- **Network Partition**: Validates split-brain prevention and consistency
- **Automatic Recovery**: Tests traffic redistribution when region recovers

**Success Criteria**:
- Failover time ≤ 120 seconds (RTO target)
- No data loss during failover (RPO < 1 second)
- Availability ≥ 99% during failover
- Automatic recovery and rebalancing

#### Load Balancing Tests (`test_load_balancing.py`)
Tests traffic distribution across regions:
- **Geographic Routing**: Users routed to nearest region (>95% accuracy)
- **Weighted Routing**: Traffic distributed according to configured weights
- **Health-Based Routing**: Traffic avoids unhealthy regions
- **Capacity-Based Routing**: Traffic shifts when region reaches capacity

**Performance Targets**:
- P95 latency < 200ms
- Routing accuracy > 95%
- Error rate < 1%
- Traffic distribution within 5% of target weights

#### End-to-End Business Flow Tests (`test_end_to_end_business_flow.py`)
Tests complete business workflows across regions:
- **Customer Registration and Order**: Complete customer lifecycle across regions
- **Cross-Region Order Fulfillment**: Inventory allocation and fulfillment
- **Payment Processing**: Multi-region payment coordination

**Workflow Validation**:
- All workflow steps complete successfully
- Data consistency maintained across regions
- Workflow completion time < 30 seconds
- No data loss or corruption

### Test Reports

Test results and reports are generated in:
- `reports/integration/` - Integration test results
- `reports/performance/` - Gatling performance reports
- `reports/cross-region/` - Cross-region test results

## Configuration

### Environment Variables
- `TARGET_HOST` - Target application host (default: localhost:8080)
- `TEST_ENVIRONMENT` - Test environment (staging, production)
- `AWS_REGION` - Primary AWS region for testing
- `SECONDARY_REGION` - Secondary region for cross-region tests

### Service Configuration
Services are configured via Docker Compose with production-like settings:
- PostgreSQL with connection pooling and replication
- Redis cluster with Sentinel for high availability
- Kafka cluster with cross-region replication
- Monitoring stack with Prometheus and Grafana

## Development Guidelines

### Adding New Tests
1. Choose appropriate test category (integration/performance/cross-region)
2. Follow existing test structure and naming conventions
3. Include proper cleanup and resource management
4. Add test documentation and expected outcomes
5. Update relevant execution scripts

### Test Data Management
- Use realistic test data that mirrors production patterns
- Implement proper cleanup procedures
- Avoid hardcoded values, use configuration files
- Consider data privacy and security requirements

### Performance Considerations
- Tests should be designed for parallel execution where possible
- Include proper resource limits and timeouts
- Monitor test execution resource usage
- Implement performance regression detection