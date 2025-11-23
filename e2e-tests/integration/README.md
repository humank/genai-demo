# Integration Test Framework

This directory contains comprehensive integration tests for the application, organized by service type and testing scope.

## Test Structure

```
staging-tests/integration/
├── database/                    # Database integration tests
│   ├── test_database_integration.py
│   └── requirements.txt
├── cache/                       # Cache integration tests
│   ├── test_redis_integration.py
│   └── requirements.txt
├── messaging/                   # Messaging integration tests
│   ├── test_kafka_integration.py
│   └── requirements.txt
└── monitoring/                  # Monitoring integration tests
```

## Test Categories

### Database Integration Tests
- **PostgreSQL connection pool performance tests**
- **Aurora failover and recovery scenario tests**
- **Database health validation and monitoring tests**
- **Cross-region database replication tests**
- **Test data management and cleanup procedures**

**Requirements**: 2.1, 2.2, 2.4, 6.1

### Cache Integration Tests
- **Redis cluster performance and scalability tests**
- **Redis Sentinel failover scenario tests**
- **Cross-region cache synchronization tests**
- **Cache eviction and memory management tests**
- **Cache performance benchmarking and validation**

**Requirements**: 2.1, 2.2, 2.4, 6.1

### Messaging Integration Tests
- **Kafka producer and consumer throughput tests**
- **Kafka partition rebalancing and failover tests**
- **Cross-region message replication tests**
- **Message ordering and delivery guarantee tests**
- **Messaging performance benchmarking and validation**

**Requirements**: 2.1, 2.2, 2.4, 6.1

## Running Tests

### Prerequisites

1. **Install Python dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

2. **Start required services** (using Docker Compose):
   ```bash
   cd staging-tests/config
   docker-compose -f docker-compose-staging.yml up -d
   ```

3. **Wait for services to be ready**:
   ```bash
   cd staging-tests/scripts
   ./wait-for-services.sh
   ```

### Running Individual Test Suites

#### Database Tests
```bash
cd staging-tests/integration/database
python -m pytest test_database_integration.py -v
```

#### Cache Tests
```bash
cd staging-tests/integration/cache
python -m pytest test_redis_integration.py -v
```

#### Messaging Tests
```bash
cd staging-tests/integration/messaging
python -m pytest test_kafka_integration.py -v
```

### Running All Integration Tests
```bash
cd staging-tests
python run_integration_tests.py
```

### Running Specific Test Types
```bash
# Database tests only
python run_integration_tests.py --test-type database

# Cache tests only
python run_integration_tests.py --test-type cache

# Messaging tests only
python run_integration_tests.py --test-type messaging

# With verbose logging
python run_integration_tests.py --verbose
```

## Test Configuration

### Database Configuration
- **Host**: localhost
- **Port**: 5432 (primary), 5433 (replica), 5434 (replica)
- **Database**: test_db
- **Username**: test_user
- **Password**: test_password

### Cache Configuration
- **Redis Host**: localhost
- **Redis Port**: 6379 (primary), 6380 (replica), 6381 (replica)
- **Sentinel Port**: 26379
- **Service Name**: mymaster

### Messaging Configuration
- **Kafka Brokers**: localhost:9092, localhost:9093, localhost:9094
- **Topics**: Auto-created during tests
- **Consumer Groups**: Test-specific groups

## Performance Expectations

### Database Tests
- **Connection Pool Performance**: < 500ms average response time
- **Aurora Failover**: < 30 seconds
- **Health Validation**: < 100ms query response time
- **Cross-Region Replication**: < 1000ms lag

### Cache Tests
- **Cache Operations**: < 10ms average latency
- **Sentinel Failover**: < 30 seconds
- **Cross-Region Sync**: < 1000ms latency
- **Memory Management**: Proper eviction under memory pressure

### Messaging Tests
- **Producer Throughput**: > 1000 messages/second
- **Consumer Throughput**: > 1000 messages/second
- **End-to-End Latency**: < 100ms average
- **Message Ordering**: Preserved in single partition

## Troubleshooting

### Common Issues

1. **Service Connection Failures**
   - Ensure Docker services are running
   - Check port availability
   - Verify network connectivity

2. **Test Timeouts**
   - Increase timeout values in test configuration
   - Check system resource availability
   - Monitor service performance

3. **Data Consistency Issues**
   - Verify replication configuration
   - Check network latency between regions
   - Monitor synchronization processes

### Debugging

Enable verbose logging:
```bash
python run_integration_tests.py --verbose
```

Check service logs:
```bash
docker-compose -f config/docker-compose-staging.yml logs [service-name]
```

Monitor resource usage:
```bash
docker stats
```

## Extending Tests

### Adding New Test Cases

1. **Create test method** in appropriate test suite class
2. **Follow naming convention**: `test_[functionality]_[scenario]`
3. **Include performance assertions** based on requirements
4. **Add proper cleanup** in teardown methods
5. **Update documentation** with new test descriptions

### Adding New Test Suites

1. **Create new directory** under `integration/`
2. **Implement test suite class** following existing patterns
3. **Add requirements.txt** with specific dependencies
4. **Update main test runner** to include new suite
5. **Add configuration** for new services if needed

## Continuous Integration

These tests are designed to run in CI/CD pipelines:

- **AWS CodeBuild**: See `aws-codebuild/buildspec-integration-tests.yml`
- **Local Development**: Use `scripts/run-integration-tests.sh`
- **Staging Environment**: Automated execution on code changes

## Monitoring and Reporting

Test results include:
- **Performance metrics** for each operation
- **Success/failure rates** by test category
- **Resource utilization** during test execution
- **Detailed error logs** for failed tests
- **Trend analysis** for performance regression detection