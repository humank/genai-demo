# Design Document

## Overview

This design document outlines the architecture and implementation strategy for refactoring the test code structure from a monolithic approach to a profile-based, layered testing strategy. The design separates fast unit tests for local development from comprehensive integration tests that run in staging environments.

## Architecture

### Current State Analysis

The current test architecture has several issues:
- **Mixed Test Types**: Unit tests and integration tests are intermixed in the same directory structure
- **Heavy Dependencies**: Many tests marked as "unit tests" actually load full Spring Boot context
- **External Service Dependencies**: Tests depend on Redis, Kafka, Aurora, DynamoDB in local environment
- **Performance Issues**: Test execution takes 5-10 minutes and uses 6GB+ memory
- **CI/CD Bottlenecks**: Slow feedback loop for pull request validation

### Target Architecture

```
Project Structure:
├── app/src/test/java/                    # Fast Unit Tests (Local Development)
│   ├── domain/                          # Pure business logic tests
│   ├── application/                     # Application service tests with mocks
│   ├── architecture/                    # ArchUnit tests
│   └── testutils/                       # Test utilities and builders
│
├── staging-tests/                       # Integration Tests (Staging Environment)
│   ├── integration/
│   │   ├── database/                    # Database integration tests
│   │   ├── cache/                       # Redis integration tests
│   │   ├── messaging/                   # Kafka integration tests
│   │   └── monitoring/                  # Observability tests
│   ├── performance/                     # Performance and load tests
│   ├── cross-region/                    # Cross-region and DR tests
│   ├── config/                          # Docker Compose and configurations
│   └── scripts/                         # Execution scripts
│
└── aws-codebuild/                       # AWS CodeBuild Configuration
    ├── buildspec-unit-tests.yml        # Fast unit test execution
    ├── buildspec-integration-tests.yml # Staging integration tests
    └── pipeline-template.yml           # CodePipeline CloudFormation template
```

## Components and Interfaces

### Test Layer Separation

#### Unit Test Layer (Local Development)
- **Purpose**: Fast feedback for business logic validation
- **Scope**: Domain logic, application services, utilities
- **Dependencies**: Mockito 5.x (mocking framework), JUnit 5 (test framework), AssertJ (assertions)
- **Constraints**: No Spring Boot context, no external services
- **Performance**: <50ms per test, <1GB total memory

```java
// Example Unit Test Structure
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private EmailService emailService;
    @InjectMocks private CustomerService customerService;
    
    @Test
    void should_create_customer_when_valid_data_provided() {
        // Pure business logic testing with mocks
    }
}
```

#### Integration Test Layer (Staging Environment)
- **Purpose**: Comprehensive system integration validation
- **Scope**: External services, cross-region scenarios, performance
- **Dependencies**: Docker Compose, real services, Python test framework
- **Constraints**: Production-like environment, comprehensive coverage
- **Performance**: Optimized for thoroughness over speed

```python
# Example Integration Test Structure
class TestDatabaseIntegration:
    def test_aurora_connection_with_failover(self):
        # Test real Aurora connection and failover scenarios
        
    def test_cross_region_data_replication(self):
        # Test actual cross-region data replication
```

### Build System Integration

#### Gradle Test Tasks Configuration

```gradle
// Fast Unit Tests (Local Development)
tasks.register('quickTest', Test) {
    description = 'Fast unit tests for immediate feedback'
    useJUnitPlatform {
        excludeTags 'integration', 'slow', 'external'
        includeTags 'unit', 'fast'
    }
    maxHeapSize = '1g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0
    
    // Performance optimizations
    jvmArgs += [
        '-XX:+UseG1GC',
        '-XX:MaxGCPauseMillis=100',
        '-Djunit.jupiter.execution.parallel.enabled=true'
    ]
}

// Comprehensive Unit Tests (Pre-commit)
tasks.register('unitTest', Test) {
    description = 'All unit tests for pre-commit validation'
    useJUnitPlatform {
        excludeTags 'integration', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = 2
    timeout = Duration.ofMinutes(5)
}

// Staging Integration Tests
tasks.register('stagingTest', Exec) {
    description = 'Run staging integration tests'
    workingDir 'staging-tests'
    commandLine './scripts/run-integration-tests.sh'
    environment 'TEST_ENVIRONMENT', 'staging'
}
```

#### CI/CD Pipeline Integration with AWS Code Services

```yaml
# buildspec-unit-tests.yml (AWS CodeBuild for Unit Tests)
version: 0.2
phases:
  install:
    runtime-versions:
      java: corretto21
    commands:
      - echo "Installing dependencies..."
      - ./gradlew --version
  pre_build:
    commands:
      - echo "Pre-build phase started on `date`"
      - echo "Running quick validation..."
  build:
    commands:
      - echo "Build phase started on `date`"
      - echo "Running fast unit tests..."
      - ./gradlew quickTest --info
      - echo "Running comprehensive unit tests..."
      - ./gradlew unitTest --info
  post_build:
    commands:
      - echo "Post-build phase completed on `date`"
      - echo "Unit test execution completed"
reports:
  unit-test-reports:
    files:
      - '**/*'
    base-directory: 'build/reports/tests'
    file-format: 'JUNITXML'
artifacts:
  files:
    - 'build/reports/**/*'
    - 'build/test-results/**/*'
  name: unit-test-artifacts

---

# buildspec-integration-tests.yml (AWS CodeBuild for Integration Tests)
version: 0.2
phases:
  install:
    runtime-versions:
      java: corretto21
      python: 3.11
    commands:
      - echo "Installing integration test dependencies..."
      - pip install -r staging-tests/requirements.txt
      - docker --version
      - docker-compose --version
  pre_build:
    commands:
      - echo "Setting up staging infrastructure..."
      - cd staging-tests
      - docker-compose -f config/docker-compose-staging.yml up -d
      - ./scripts/wait-for-services.sh
  build:
    commands:
      - echo "Running integration tests..."
      - ./scripts/run-integration-tests.sh
      - ./scripts/run-performance-tests.sh
      - ./scripts/run-cross-region-tests.sh
  post_build:
    commands:
      - echo "Cleaning up infrastructure..."
      - docker-compose -f config/docker-compose-staging.yml down
      - echo "Integration test execution completed"
reports:
  integration-test-reports:
    files:
      - '**/*.xml'
    base-directory: 'staging-tests/reports'
    file-format: 'JUNITXML'
artifacts:
  files:
    - 'staging-tests/reports/**/*'
    - 'staging-tests/logs/**/*'
  name: integration-test-artifacts

---

# CodePipeline Configuration (pipeline-template.yml)
AWSTemplateFormatVersion: '2010-09-09'
Description: 'Test Code Refactoring CI/CD Pipeline'

Parameters:
  RepositoryName:
    Type: String
    Default: 'genai-demo'
  BranchName:
    Type: String
    Default: 'develop'

Resources:
  # Unit Tests CodeBuild Project
  UnitTestProject:
    Type: AWS::CodeBuild::Project
    Properties:
      Name: !Sub '${RepositoryName}-unit-tests'
      ServiceRole: !Ref CodeBuildServiceRole
      Artifacts:
        Type: CODEPIPELINE
      Environment:
        Type: LINUX_CONTAINER
        ComputeType: BUILD_GENERAL1_MEDIUM
        Image: aws/codebuild/amazonlinux2-x86_64-standard:5.0
      Source:
        Type: CODEPIPELINE
        BuildSpec: buildspec-unit-tests.yml
      TimeoutInMinutes: 10

  # Integration Tests CodeBuild Project  
  IntegrationTestProject:
    Type: AWS::CodeBuild::Project
    Properties:
      Name: !Sub '${RepositoryName}-integration-tests'
      ServiceRole: !Ref CodeBuildServiceRole
      Artifacts:
        Type: CODEPIPELINE
      Environment:
        Type: LINUX_CONTAINER
        ComputeType: BUILD_GENERAL1_LARGE
        Image: aws/codebuild/amazonlinux2-x86_64-standard:5.0
        PrivilegedMode: true  # Required for Docker
      Source:
        Type: CODEPIPELINE
        BuildSpec: buildspec-integration-tests.yml
      TimeoutInMinutes: 45

  # CodePipeline
  TestPipeline:
    Type: AWS::CodePipeline::Pipeline
    Properties:
      Name: !Sub '${RepositoryName}-test-pipeline'
      RoleArn: !GetAtt CodePipelineServiceRole.Arn
      ArtifactStore:
        Type: S3
        Location: !Ref ArtifactsBucket
      Stages:
        - Name: Source
          Actions:
            - Name: SourceAction
              ActionTypeId:
                Category: Source
                Owner: AWS
                Provider: CodeCommit
                Version: '1'
              Configuration:
                RepositoryName: !Ref RepositoryName
                BranchName: !Ref BranchName
              OutputArtifacts:
                - Name: SourceOutput

        - Name: UnitTests
          Actions:
            - Name: RunUnitTests
              ActionTypeId:
                Category: Build
                Owner: AWS
                Provider: CodeBuild
                Version: '1'
              Configuration:
                ProjectName: !Ref UnitTestProject
              InputArtifacts:
                - Name: SourceOutput
              OutputArtifacts:
                - Name: UnitTestOutput

        - Name: IntegrationTests
          Actions:
            - Name: RunIntegrationTests
              ActionTypeId:
                Category: Build
                Owner: AWS
                Provider: CodeBuild
                Version: '1'
              Configuration:
                ProjectName: !Ref IntegrationTestProject
              InputArtifacts:
                - Name: SourceOutput
              OutputArtifacts:
                - Name: IntegrationTestOutput
              RunOrder: 1
```

## Data Models

### Test Configuration Model

```yaml
# Test Configuration Schema
test-configuration:
  unit-tests:
    memory-limit: "1GB"
    timeout: "5 minutes"
    parallel-execution: true
    excluded-tags: ["integration", "slow", "external"]
    
  integration-tests:
    environment: "staging"
    services:
      - postgresql
      - redis
      - kafka
      - dynamodb
      - localstack
    timeout: "30 minutes"
    
  performance-tests:
    load-patterns:
      - concurrent-users: 100
      - duration: "10 minutes"
      - ramp-up: "2 minutes"
```

### Migration Mapping Model

```yaml
# Migration Mapping Configuration
migration-rules:
  move-to-staging:
    patterns:
      - "**/*IntegrationTest.java"
      - "**/*@SpringBootTest*"
      - "**/config/*ConfigurationTest.java"
    destinations:
      database: "staging-tests/integration/database/"
      cache: "staging-tests/integration/cache/"
      messaging: "staging-tests/integration/messaging/"
      
  convert-to-unit:
    patterns:
      - "**/application/**/*Test.java"
      - "**/domain/**/*Test.java"
    transformations:
      - remove: "@SpringBootTest"
      - add: "@ExtendWith(MockitoExtension.class)"
      - mock: ["Repository", "ExternalService"]
```

## Error Handling

### Test Failure Classification

#### Unit Test Failures
- **Business Logic Errors**: Domain rule violations, calculation errors
- **Mockito Configuration Issues**: Incorrect Mockito mock setup or verification
- **Test Data Problems**: Invalid test builders or fixtures
- **Performance Degradation**: Tests exceeding time thresholds

```java
// Error Handling in Unit Tests
@Test
void should_handle_invalid_customer_data_gracefully() {
    // Given
    CreateCustomerCommand invalidCommand = createInvalidCommand();
    
    // When & Then
    assertThatThrownBy(() -> customerService.createCustomer(invalidCommand))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Invalid customer data")
        .satisfies(ex -> {
            ValidationException validationEx = (ValidationException) ex;
            assertThat(validationEx.getFieldErrors()).isNotEmpty();
        });
}
```

#### Integration Test Failures
- **Service Connectivity Issues**: External service unavailability
- **Configuration Problems**: Incorrect service configurations
- **Environment Issues**: Infrastructure setup problems
- **Data Consistency Problems**: Cross-service data synchronization issues

```python
# Error Handling in Integration Tests
class TestErrorHandling:
    def test_database_connection_failure_recovery(self):
        try:
            # Simulate database failure
            self.simulate_database_failure()
            
            # Test recovery mechanism
            result = self.customer_service.create_customer(valid_data)
            
            # Verify graceful degradation
            assert result.status == "pending"
            assert "database_unavailable" in result.warnings
            
        except Exception as e:
            # Provide detailed diagnostic information
            self.collect_diagnostic_info()
            raise AssertionError(f"Integration test failed: {e}")
```

### Rollback and Recovery Strategies

#### Migration Rollback Plan
1. **Immediate Rollback**: Revert to previous test configuration within 1 hour
2. **Partial Rollback**: Keep successful migrations, rollback problematic ones
3. **Data Preservation**: Maintain test coverage metrics and historical data
4. **Communication Plan**: Notify team of rollback and next steps

#### Test Environment Recovery
1. **Infrastructure Reset**: Automated Docker Compose restart procedures
2. **Data Cleanup**: Automated test data cleanup between test runs
3. **Service Health Checks**: Automated validation of service availability
4. **Monitoring Integration**: Real-time monitoring of test environment health

## Testing Strategy

### Unit Test Strategy

#### Test Categories and Scope
- **Domain Tests**: Pure business logic, value objects, domain services
- **Application Tests**: Use case implementations with mocked dependencies
- **Utility Tests**: Helper functions, builders, converters
- **Architecture Tests**: ArchUnit rules for architectural compliance

#### Performance Optimization Techniques
- **Parallel Execution**: JUnit 5 parallel test execution
- **Memory Management**: Optimized JVM settings and garbage collection
- **Mockito Optimization**: Efficient Mockito mock creation and reuse patterns
- **Test Data Optimization**: Lightweight test builders and fixtures

```java
// Optimized Test Configuration
@TestMethodOrder(OrderAnnotation.class)
@ExtendWith(MockitoExtension.class)
class OptimizedCustomerServiceTest {
    
    @Mock private CustomerRepository customerRepository;
    @InjectMocks private CustomerService customerService;
    
    // Reusable test data
    private static final CustomerTestDataBuilder CUSTOMER_BUILDER = 
        CustomerTestDataBuilder.aCustomer();
    
    @Test
    @Order(1)
    void should_create_customer_successfully() {
        // Fast test with minimal setup
    }
}
```

### Integration Test Strategy

#### Service Integration Testing
- **Database Integration**: Real PostgreSQL and Aurora connections
- **Cache Integration**: Real Redis cluster testing
- **Messaging Integration**: Real Kafka cluster testing
- **Cross-Region Testing**: Multi-region service validation

#### Infrastructure as Code Testing
- **Docker Compose Validation**: Service orchestration testing
- **Configuration Testing**: Environment-specific configuration validation
- **Health Check Testing**: Service availability and readiness validation
- **Performance Baseline Testing**: Establish performance benchmarks

```python
# Integration Test Framework
class BaseIntegrationTest:
    def setup_method(self):
        self.services = ServiceOrchestrator()
        self.services.start_all()
        self.wait_for_services_ready()
    
    def teardown_method(self):
        self.services.cleanup()
        self.collect_metrics()
    
    def wait_for_services_ready(self, timeout=60):
        # Wait for all services to be healthy
        pass
```

### Performance Testing Strategy

#### Load Testing Scenarios
- **Normal Load**: Baseline performance under typical usage
- **Peak Load**: Performance under maximum expected load
- **Stress Testing**: Breaking point identification
- **Endurance Testing**: Long-running stability validation

#### Monitoring and Metrics
- **Response Time Monitoring**: 95th percentile response times
- **Throughput Monitoring**: Requests per second capacity
- **Resource Utilization**: CPU, memory, and I/O monitoring
- **Error Rate Monitoring**: Error frequency and patterns

## Testing Tools and Scripts

### Integration Testing Tools

#### Database Integration Testing
```python
# staging-tests/integration/database/test_database_integration.py
import pytest
import psycopg2
import boto3
from testcontainers.postgres import PostgresContainer
from testcontainers.localstack import LocalStackContainer

class DatabaseIntegrationTestSuite:
    def __init__(self):
        self.postgres_container = None
        self.localstack_container = None
        self.aurora_client = None
    
    def setup_infrastructure(self):
        """Setup test database infrastructure"""
        # PostgreSQL for local testing
        self.postgres_container = PostgresContainer("postgres:15")
        self.postgres_container.start()
        
        # LocalStack for AWS services simulation
        self.localstack_container = LocalStackContainer()
        self.localstack_container.start()
        
        # Aurora client for production-like testing
        self.aurora_client = boto3.client('rds', region_name='us-east-1')
    
    def test_connection_pooling_performance(self):
        """Test database connection pool under load"""
        import concurrent.futures
        import time
        
        def execute_query(query_id):
            start_time = time.time()
            # Execute database operation
            connection = self.get_connection()
            cursor = connection.cursor()
            cursor.execute("SELECT pg_sleep(0.1), %s", (query_id,))
            result = cursor.fetchone()
            cursor.close()
            connection.close()
            return time.time() - start_time
        
        # Test concurrent connections
        with concurrent.futures.ThreadPoolExecutor(max_workers=50) as executor:
            futures = [executor.submit(execute_query, i) for i in range(100)]
            response_times = [future.result() for future in futures]
        
        # Validate performance metrics
        avg_response_time = sum(response_times) / len(response_times)
        p95_response_time = sorted(response_times)[int(0.95 * len(response_times))]
        
        assert avg_response_time < 0.5, f"Average response time too high: {avg_response_time}s"
        assert p95_response_time < 1.0, f"95th percentile too high: {p95_response_time}s"
    
    def test_aurora_failover_scenario(self):
        """Test Aurora cluster failover behavior"""
        # Test primary instance failure simulation
        # Test read replica promotion
        # Test connection recovery
        pass
```

#### Cache Integration Testing
```python
# staging-tests/integration/cache/test_redis_integration.py
import redis
import time
import pytest
from redis.sentinel import Sentinel

class RedisIntegrationTestSuite:
    def __init__(self):
        self.redis_client = None
        self.sentinel = None
    
    def setup_redis_cluster(self):
        """Setup Redis cluster for testing"""
        # Redis Sentinel configuration
        sentinel_hosts = [('localhost', 26379)]
        self.sentinel = Sentinel(sentinel_hosts)
        self.redis_client = self.sentinel.master_for('mymaster')
    
    def test_cache_performance_under_load(self):
        """Test Redis performance under concurrent load"""
        import concurrent.futures
        
        def cache_operation(operation_id):
            start_time = time.time()
            
            # Write operation
            key = f"test_key_{operation_id}"
            value = f"test_value_{operation_id}" * 100  # ~1KB value
            self.redis_client.set(key, value, ex=300)  # 5 min expiry
            
            # Read operation
            retrieved_value = self.redis_client.get(key)
            
            # Delete operation
            self.redis_client.delete(key)
            
            return time.time() - start_time
        
        # Execute concurrent operations
        with concurrent.futures.ThreadPoolExecutor(max_workers=100) as executor:
            futures = [executor.submit(cache_operation, i) for i in range(1000)]
            response_times = [future.result() for future in futures]
        
        # Performance validation
        avg_response_time = sum(response_times) / len(response_times)
        assert avg_response_time < 0.01, f"Cache operations too slow: {avg_response_time}s"
    
    def test_redis_failover_recovery(self):
        """Test Redis Sentinel failover scenarios"""
        # Test master failure simulation
        # Test automatic failover to replica
        # Test client reconnection
        pass
```

#### Messaging Integration Testing
```python
# staging-tests/integration/messaging/test_kafka_integration.py
from kafka import KafkaProducer, KafkaConsumer
import json
import time
import threading
from concurrent.futures import ThreadPoolExecutor

class KafkaIntegrationTestSuite:
    def __init__(self):
        self.producer = None
        self.consumer = None
        self.bootstrap_servers = ['localhost:9092']
    
    def setup_kafka_infrastructure(self):
        """Setup Kafka cluster for testing"""
        self.producer = KafkaProducer(
            bootstrap_servers=self.bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
            acks='all',  # Wait for all replicas
            retries=3
        )
    
    def test_message_throughput_performance(self):
        """Test Kafka message throughput under load"""
        topic_name = 'performance-test-topic'
        message_count = 10000
        
        # Producer performance test
        start_time = time.time()
        
        def produce_messages(batch_start, batch_size):
            for i in range(batch_start, batch_start + batch_size):
                message = {
                    'id': i,
                    'timestamp': time.time(),
                    'data': 'x' * 1024  # 1KB message
                }
                self.producer.send(topic_name, value=message)
        
        # Parallel message production
        with ThreadPoolExecutor(max_workers=10) as executor:
            batch_size = message_count // 10
            futures = []
            for i in range(0, message_count, batch_size):
                future = executor.submit(produce_messages, i, batch_size)
                futures.append(future)
            
            # Wait for all batches to complete
            for future in futures:
                future.result()
        
        self.producer.flush()  # Ensure all messages are sent
        production_time = time.time() - start_time
        
        # Calculate throughput
        messages_per_second = message_count / production_time
        assert messages_per_second > 1000, f"Throughput too low: {messages_per_second} msg/s"
    
    def test_cross_region_replication(self):
        """Test Kafka cross-region replication"""
        # Test message replication across regions
        # Test replication lag monitoring
        # Test failover scenarios
        pass
```

### Performance Testing Tools

#### Gatling Integration Scripts
```bash
#!/bin/bash
# staging-tests/scripts/run-performance-tests.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Configuration
GATLING_HOME="${GATLING_HOME:-/opt/gatling}"
SIMULATIONS_DIR="$PROJECT_ROOT/performance/gatling/simulations"
RESULTS_DIR="$PROJECT_ROOT/reports/performance"
TARGET_HOST="${TARGET_HOST:-localhost:8080}"

# Ensure results directory exists
mkdir -p "$RESULTS_DIR"

echo "Starting Gatling performance test suite..."
echo "Target: $TARGET_HOST"
echo "Results will be saved to: $RESULTS_DIR"

# Function to run Gatling test
run_gatling_test() {
    local simulation_class=$1
    local test_name=$2
    local users=${3:-100}
    local duration=${4:-300}
    
    echo "Running $test_name with $users users for ${duration}s..."
    
    cd "$GATLING_HOME"
    
    ./bin/gatling.sh \
        -sf "$SIMULATIONS_DIR" \
        -s "$simulation_class" \
        -rf "$RESULTS_DIR" \
        -rd "$test_name" \
        -Dusers="$users" \
        -Dduration="${duration}s" \
        -Dhost="$TARGET_HOST"
    
    echo "$test_name completed. Report available in: $RESULTS_DIR"
}

# Performance Test Scenarios
echo "=== Running Gatling Load Tests ==="

# Normal Load Test (100 users, 5 minutes)
run_gatling_test "NormalLoadSimulation" "normal-load" 100 300

# Peak Load Test (500 users, 10 minutes)
run_gatling_test "PeakLoadSimulation" "peak-load" 500 600

# Stress Test (1000 users, 15 minutes)
run_gatling_test "StressTestSimulation" "stress-test" 1000 900

# Endurance Test (200 users, 1 hour)
run_gatling_test "EnduranceTestSimulation" "endurance-test" 200 3600

echo "=== Gatling Performance Tests Completed ==="

# Generate consolidated report
python3 "$SCRIPT_DIR/generate-gatling-report.py" "$RESULTS_DIR"

echo "Consolidated performance report generated: $RESULTS_DIR/consolidated-report.html"
```

#### Gatling Simulation Scripts
```scala
// staging-tests/performance/gatling/simulations/NormalLoadSimulation.scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class NormalLoadSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Performance Test")

  val users = Integer.getInteger("users", 100)
  val duration = System.getProperty("duration", "300s")

  // Customer creation scenario
  val customerCreationScenario = scenario("Customer Creation Load Test")
    .exec(
      http("Create Customer")
        .post("/api/v1/customers")
        .body(StringBody("""
          {
            "name": "Customer ${__Random(1,10000)}",
            "email": "customer${__Random(1,10000)}@example.com",
            "phone": "+1${__Random(1000000000,9999999999)}"
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("customerId"))
    )
    .pause(1, 3)
    .exec(
      http("Get Customer")
        .get("/api/v1/customers/${customerId}")
        .check(status.is(200))
        .check(jsonPath("$.name").exists)
    )
    .pause(2, 5)

  // Order creation scenario
  val orderCreationScenario = scenario("Order Creation Load Test")
    .exec(
      http("Create Order")
        .post("/api/v1/orders")
        .body(StringBody("""
          {
            "customerId": "CUST-${__Random(1,1000)}",
            "items": [
              {
                "productId": "PROD-${__Random(1,100)}",
                "quantity": ${__Random(1,5)},
                "price": ${__Random(10,100)}.99
              }
            ]
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("orderId"))
    )
    .pause(1, 2)
    .exec(
      http("Get Order")
        .get("/api/v1/orders/${orderId}")
        .check(status.is(200))
        .check(jsonPath("$.status").exists)
    )

  setUp(
    customerCreationScenario.inject(
      rampUsers(users / 2) during (30 seconds),
      constantUsersPerSec(users / 4) during (duration.toInt seconds - 60),
      rampUsers(0) during (30 seconds)
    ),
    orderCreationScenario.inject(
      rampUsers(users / 2) during (30 seconds),
      constantUsersPerSec(users / 4) during (duration.toInt seconds - 60),
      rampUsers(0) during (30 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(2000),  // 95th percentile < 2s
     global.responseTime.percentile4.lt(5000),  // 99th percentile < 5s
     global.successfulRequests.percent.gt(99),   // Success rate > 99%
     global.responseTime.mean.lt(1000)          // Mean response time < 1s
   )
}
```

```scala
// staging-tests/performance/gatling/simulations/StressTestSimulation.scala
package simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class StressTestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl(System.getProperty("host", "http://localhost:8080"))
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling Stress Test")

  val users = Integer.getInteger("users", 1000)
  val duration = System.getProperty("duration", "900s")

  // High-intensity customer operations
  val stressTestScenario = scenario("Stress Test Scenario")
    .exec(
      http("Bulk Customer Creation")
        .post("/api/v1/customers/batch")
        .body(StringBody("""
          {
            "customers": [
              ${__range(1, 10, customer =>
                s"""
                {
                  "name": "StressCustomer${customer}_${__Random(1,10000)}",
                  "email": "stress${customer}_${__Random(1,10000)}@example.com",
                  "phone": "+1${__Random(1000000000,9999999999)}"
                }
                """
              ).mkString(",")}
            ]
          }
        """)).asJson
        .check(status.is(201))
        .check(jsonPath("$[*].id").findAll.saveAs("customerIds"))
    )
    .pause(500 milliseconds, 1 second)
    .repeat(5) {
      exec(
        http("Concurrent Customer Queries")
          .get("/api/v1/customers/${customerIds.random()}")
          .check(status.is(200))
      )
      .pause(100 milliseconds, 500 milliseconds)
    }
    .exec(
      http("Heavy Analytics Query")
        .get("/api/v1/analytics/customer-summary")
        .queryParam("startDate", "2024-01-01")
        .queryParam("endDate", "2024-12-31")
        .queryParam("includeOrders", "true")
        .check(status.is(200))
        .check(responseTimeInMillis.lt(10000))  // Should complete within 10s even under stress
    )

  setUp(
    stressTestScenario.inject(
      // Aggressive ramp-up to stress the system
      rampUsers(users / 4) during (60 seconds),
      rampUsers(users / 2) during (120 seconds),
      rampUsers(users) during (180 seconds),
      constantUsersPerSec(users / 2) during (duration.toInt seconds - 360),
      rampUsers(0) during (60 seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(5000),   // 95th percentile < 5s (relaxed for stress test)
     global.responseTime.percentile4.lt(15000),  // 99th percentile < 15s
     global.successfulRequests.percent.gt(95),   // Success rate > 95% (relaxed for stress test)
     global.responseTime.mean.lt(3000)           // Mean response time < 3s
   )
}
```

#### K6 Performance Testing Scripts
```javascript
// staging-tests/performance/k6/load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');
export let responseTime = new Trend('response_time');

// Test configuration
export let options = {
  stages: [
    { duration: '2m', target: 100 },   // Ramp up to 100 users
    { duration: '5m', target: 100 },   // Stay at 100 users
    { duration: '2m', target: 200 },   // Ramp up to 200 users
    { duration: '5m', target: 200 },   // Stay at 200 users
    { duration: '2m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.01'],    // Error rate under 1%
    errors: ['rate<0.01'],             // Custom error rate under 1%
  },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export default function() {
  // Test customer creation API
  let customerPayload = JSON.stringify({
    name: `Customer ${Math.random()}`,
    email: `customer${Math.random()}@example.com`,
    phone: `+1${Math.floor(Math.random() * 9000000000) + 1000000000}`
  });

  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Create customer
  let createResponse = http.post(`${BASE_URL}/api/v1/customers`, customerPayload, params);
  
  let createSuccess = check(createResponse, {
    'customer creation status is 201': (r) => r.status === 201,
    'customer creation response time < 2s': (r) => r.timings.duration < 2000,
  });

  errorRate.add(!createSuccess);
  responseTime.add(createResponse.timings.duration);

  if (createSuccess) {
    let customerId = JSON.parse(createResponse.body).id;
    
    // Get customer
    let getResponse = http.get(`${BASE_URL}/api/v1/customers/${customerId}`);
    
    let getSuccess = check(getResponse, {
      'customer retrieval status is 200': (r) => r.status === 200,
      'customer retrieval response time < 1s': (r) => r.timings.duration < 1000,
    });

    errorRate.add(!getSuccess);
    responseTime.add(getResponse.timings.duration);
  }

  sleep(1); // Wait 1 second between iterations
}

// Setup function
export function setup() {
  console.log('Starting performance test...');
  console.log(`Target URL: ${BASE_URL}`);
}

// Teardown function
export function teardown(data) {
  console.log('Performance test completed');
}
```

#### Database Performance Testing
```python
# staging-tests/performance/database/db_performance_test.py
import asyncio
import asyncpg
import time
import statistics
from concurrent.futures import ThreadPoolExecutor
import matplotlib.pyplot as plt
import pandas as pd

class DatabasePerformanceTest:
    def __init__(self, connection_string):
        self.connection_string = connection_string
        self.results = []
    
    async def test_connection_pool_performance(self):
        """Test database connection pool under various loads"""
        pool = await asyncpg.create_pool(
            self.connection_string,
            min_size=10,
            max_size=50,
            command_timeout=60
        )
        
        async def execute_query(query_id):
            start_time = time.time()
            async with pool.acquire() as connection:
                # Simulate typical application query
                result = await connection.fetch("""
                    SELECT c.id, c.name, COUNT(o.id) as order_count
                    FROM customers c
                    LEFT JOIN orders o ON c.id = o.customer_id
                    WHERE c.created_date > NOW() - INTERVAL '30 days'
                    GROUP BY c.id, c.name
                    LIMIT 100
                """)
            return time.time() - start_time, len(result)
        
        # Test different concurrency levels
        concurrency_levels = [10, 25, 50, 100, 200]
        
        for concurrency in concurrency_levels:
            print(f"Testing with {concurrency} concurrent connections...")
            
            tasks = [execute_query(i) for i in range(concurrency)]
            results = await asyncio.gather(*tasks)
            
            response_times = [r[0] for r in results]
            
            self.results.append({
                'concurrency': concurrency,
                'avg_response_time': statistics.mean(response_times),
                'p95_response_time': statistics.quantiles(response_times, n=20)[18],  # 95th percentile
                'p99_response_time': statistics.quantiles(response_times, n=100)[98], # 99th percentile
                'min_response_time': min(response_times),
                'max_response_time': max(response_times)
            })
        
        await pool.close()
    
    def generate_performance_report(self):
        """Generate performance analysis report"""
        df = pd.DataFrame(self.results)
        
        # Create performance charts
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))
        
        # Average response time vs concurrency
        ax1.plot(df['concurrency'], df['avg_response_time'], marker='o')
        ax1.set_title('Average Response Time vs Concurrency')
        ax1.set_xlabel('Concurrent Connections')
        ax1.set_ylabel('Response Time (seconds)')
        ax1.grid(True)
        
        # Percentile comparison
        ax2.plot(df['concurrency'], df['p95_response_time'], marker='o', label='95th percentile')
        ax2.plot(df['concurrency'], df['p99_response_time'], marker='s', label='99th percentile')
        ax2.set_title('Response Time Percentiles')
        ax2.set_xlabel('Concurrent Connections')
        ax2.set_ylabel('Response Time (seconds)')
        ax2.legend()
        ax2.grid(True)
        
        # Response time range
        ax3.fill_between(df['concurrency'], df['min_response_time'], df['max_response_time'], alpha=0.3)
        ax3.plot(df['concurrency'], df['avg_response_time'], marker='o', color='red', label='Average')
        ax3.set_title('Response Time Range')
        ax3.set_xlabel('Concurrent Connections')
        ax3.set_ylabel('Response Time (seconds)')
        ax3.legend()
        ax3.grid(True)
        
        # Performance degradation analysis
        baseline_avg = df.iloc[0]['avg_response_time']
        degradation = [(row['avg_response_time'] / baseline_avg - 1) * 100 for _, row in df.iterrows()]
        ax4.bar(df['concurrency'], degradation)
        ax4.set_title('Performance Degradation (%)')
        ax4.set_xlabel('Concurrent Connections')
        ax4.set_ylabel('Degradation from Baseline (%)')
        ax4.grid(True)
        
        plt.tight_layout()
        plt.savefig('staging-tests/reports/database-performance-analysis.png', dpi=300, bbox_inches='tight')
        
        # Generate summary report
        return {
            'summary': df.to_dict('records'),
            'recommendations': self.generate_recommendations(df)
        }
    
    def generate_recommendations(self, df):
        """Generate performance recommendations based on test results"""
        recommendations = []
        
        # Check if response time increases significantly with concurrency
        max_degradation = max([(row['avg_response_time'] / df.iloc[0]['avg_response_time'] - 1) * 100 
                              for _, row in df.iterrows()])
        
        if max_degradation > 50:
            recommendations.append("Consider increasing connection pool size or optimizing queries")
        
        # Check 95th percentile performance
        max_p95 = df['p95_response_time'].max()
        if max_p95 > 2.0:
            recommendations.append("95th percentile response time exceeds 2 seconds - investigate slow queries")
        
        # Check for connection pool exhaustion
        if df.iloc[-1]['avg_response_time'] > df.iloc[-2]['avg_response_time'] * 2:
            recommendations.append("Possible connection pool exhaustion at high concurrency")
        
        return recommendations

# Usage example
async def run_database_performance_tests():
    connection_string = "postgresql://user:password@localhost:5432/testdb"
    test_suite = DatabasePerformanceTest(connection_string)
    
    await test_suite.test_connection_pool_performance()
    report = test_suite.generate_performance_report()
    
    print("Database Performance Test Results:")
    for result in report['summary']:
        print(f"Concurrency: {result['concurrency']}, "
              f"Avg Response: {result['avg_response_time']:.3f}s, "
              f"P95: {result['p95_response_time']:.3f}s")
    
    print("\nRecommendations:")
    for rec in report['recommendations']:
        print(f"- {rec}")

if __name__ == "__main__":
    asyncio.run(run_database_performance_tests())
```

### Monitoring and Reporting Tools

#### Real-time Performance Monitoring
```python
# staging-tests/scripts/performance-monitor.py
import psutil
import docker
import time
import json
from datetime import datetime
import matplotlib.pyplot as plt
from collections import deque

class PerformanceMonitor:
    def __init__(self):
        self.docker_client = docker.from_env()
        self.metrics_history = {
            'cpu': deque(maxlen=300),  # 5 minutes of data (1 sample per second)
            'memory': deque(maxlen=300),
            'disk_io': deque(maxlen=300),
            'network_io': deque(maxlen=300)
        }
    
    def collect_system_metrics(self):
        """Collect system-level performance metrics"""
        # CPU usage
        cpu_percent = psutil.cpu_percent(interval=1)
        
        # Memory usage
        memory = psutil.virtual_memory()
        memory_percent = memory.percent
        
        # Disk I/O
        disk_io = psutil.disk_io_counters()
        
        # Network I/O
        network_io = psutil.net_io_counters()
        
        timestamp = datetime.now()
        
        metrics = {
            'timestamp': timestamp.isoformat(),
            'cpu_percent': cpu_percent,
            'memory_percent': memory_percent,
            'memory_available_gb': memory.available / (1024**3),
            'disk_read_mb_s': disk_io.read_bytes / (1024**2),
            'disk_write_mb_s': disk_io.write_bytes / (1024**2),
            'network_sent_mb_s': network_io.bytes_sent / (1024**2),
            'network_recv_mb_s': network_io.bytes_recv / (1024**2)
        }
        
        # Store in history
        self.metrics_history['cpu'].append((timestamp, cpu_percent))
        self.metrics_history['memory'].append((timestamp, memory_percent))
        
        return metrics
    
    def collect_container_metrics(self):
        """Collect Docker container performance metrics"""
        container_metrics = []
        
        for container in self.docker_client.containers.list():
            try:
                stats = container.stats(stream=False)
                
                # Calculate CPU percentage
                cpu_delta = stats['cpu_stats']['cpu_usage']['total_usage'] - \
                           stats['precpu_stats']['cpu_usage']['total_usage']
                system_delta = stats['cpu_stats']['system_cpu_usage'] - \
                              stats['precpu_stats']['system_cpu_usage']
                cpu_percent = (cpu_delta / system_delta) * 100.0
                
                # Calculate memory usage
                memory_usage = stats['memory_stats']['usage']
                memory_limit = stats['memory_stats']['limit']
                memory_percent = (memory_usage / memory_limit) * 100.0
                
                container_metrics.append({
                    'container_name': container.name,
                    'cpu_percent': cpu_percent,
                    'memory_usage_mb': memory_usage / (1024**2),
                    'memory_percent': memory_percent,
                    'network_rx_mb': sum(net['rx_bytes'] for net in stats['networks'].values()) / (1024**2),
                    'network_tx_mb': sum(net['tx_bytes'] for net in stats['networks'].values()) / (1024**2)
                })
                
            except Exception as e:
                print(f"Error collecting metrics for container {container.name}: {e}")
        
        return container_metrics
    
    def generate_real_time_dashboard(self):
        """Generate real-time performance dashboard"""
        fig, ((ax1, ax2), (ax3, ax4)) = plt.subplots(2, 2, figsize=(15, 10))
        
        # CPU usage over time
        if self.metrics_history['cpu']:
            times, cpu_values = zip(*self.metrics_history['cpu'])
            ax1.plot(times, cpu_values)
            ax1.set_title('CPU Usage Over Time')
            ax1.set_ylabel('CPU %')
            ax1.grid(True)
        
        # Memory usage over time
        if self.metrics_history['memory']:
            times, memory_values = zip(*self.metrics_history['memory'])
            ax2.plot(times, memory_values, color='orange')
            ax2.set_title('Memory Usage Over Time')
            ax2.set_ylabel('Memory %')
            ax2.grid(True)
        
        # Container resource usage
        container_metrics = self.collect_container_metrics()
        if container_metrics:
            container_names = [c['container_name'] for c in container_metrics]
            cpu_usage = [c['cpu_percent'] for c in container_metrics]
            memory_usage = [c['memory_percent'] for c in container_metrics]
            
            ax3.bar(container_names, cpu_usage)
            ax3.set_title('Container CPU Usage')
            ax3.set_ylabel('CPU %')
            ax3.tick_params(axis='x', rotation=45)
            
            ax4.bar(container_names, memory_usage, color='orange')
            ax4.set_title('Container Memory Usage')
            ax4.set_ylabel('Memory %')
            ax4.tick_params(axis='x', rotation=45)
        
        plt.tight_layout()
        plt.savefig('staging-tests/reports/real-time-dashboard.png', dpi=300, bbox_inches='tight')
        plt.close()

# Continuous monitoring script
def run_continuous_monitoring(duration_minutes=30):
    monitor = PerformanceMonitor()
    
    print(f"Starting performance monitoring for {duration_minutes} minutes...")
    
    end_time = time.time() + (duration_minutes * 60)
    
    while time.time() < end_time:
        # Collect metrics
        system_metrics = monitor.collect_system_metrics()
        container_metrics = monitor.collect_container_metrics()
        
        # Log metrics
        print(f"[{system_metrics['timestamp']}] "
              f"CPU: {system_metrics['cpu_percent']:.1f}% "
              f"Memory: {system_metrics['memory_percent']:.1f}% "
              f"Containers: {len(container_metrics)}")
        
        # Generate dashboard every 30 seconds
        if int(time.time()) % 30 == 0:
            monitor.generate_real_time_dashboard()
        
        time.sleep(1)
    
    print("Performance monitoring completed")

if __name__ == "__main__":
    run_continuous_monitoring(30)  # Monitor for 30 minutes
```

這些工具和腳本涵蓋了：

1. **整合測試工具**: 資料庫、快取、訊息佇列的整合測試
2. **壓力測試工具**: JMeter 和 K6 腳本進行負載測試
3. **性能效率測試**: 資料庫連線池性能測試
4. **監控工具**: 即時性能監控和儀表板

你覺得這些工具設計如何？還需要針對哪些特定的測試場景補充工具？

## Implementation Phases

### Phase 1: Infrastructure Setup (Weeks 1-2)
- Create staging-tests directory structure
- Implement Docker Compose configurations
- Create execution scripts and automation
- Set up CI/CD pipeline integration

### Phase 2: Test Migration (Weeks 3-4)
- Identify and categorize existing tests
- Migrate integration tests to staging environment
- Convert remaining tests to pure unit tests
- Update build configurations

### Phase 3: Optimization (Weeks 5-6)
- Optimize unit test performance
- Enhance integration test coverage
- Implement monitoring and metrics
- Create documentation and guidelines

### Phase 4: Validation (Weeks 7-8)
- Validate performance improvements
- Conduct team training
- Monitor success metrics
- Make final adjustments

This design provides a comprehensive foundation for implementing the test code refactoring while maintaining high quality and performance standards.