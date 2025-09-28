# Staging Environment Testing Strategy

## 📋 **Overview**

The local development environment (Local Profile) uses complete in-memory implementations to simulate all external dependencies, including distributed locks, caches, etc.
All tests involving real external services (Redis, ElastiCache, MSK, etc.) must be conducted in the Staging environment.

**Core Principles**:
- 🏠 **Local/Test**: 100% in-memory simulation, zero external dependencies
- 🎭 **Staging**: Real AWS service integration testing
- 🚀 **Production**: Staging-validated configurations

## 🎯 **Testing Layer Strategy**

### **Local Testing (Local/Test Profile)**
```
Contract Tests → DistributedLockManagerContractTest
In-Memory Implementation Tests → InMemoryDistributedLockManagerTest
Unit Tests → Business logic testing
Integration Tests → In-memory implementation + H2
Cucumber Tests → In-memory implementation + H2
```

**Characteristics**:
- ✅ Zero external dependencies
- ✅ Fast execution (< 2 minutes)
- ✅ 100% reproducible
- ✅ Complete contract validation

### **Staging Environment Testing**
```
Redis Integration Tests → Real ElastiCache
Database Integration Tests → Real RDS
Kafka Integration Tests → Real MSK
End-to-End Tests → Complete AWS service chain
Performance Tests → Real network latency
Failover Tests → Real high availability
```

**Characteristics**:
- 🔗 Real AWS services
- 📊 Real performance data
- 🔄 Failover validation
- 💰 Generates AWS costs

## 🏗️ **Staging Environment Test Implementation**

### **1. Redis/ElastiCache Integration Tests**

**Test Objective**: Validate Redis distributed lock behavior in real environment

**Test Script Example**:
```bash
#!/bin/bash
# staging-redis-tests.sh

echo "🔧 Setting up Staging Redis Tests..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=staging
export REDIS_MODE=CLUSTER
export REDIS_CLUSTER_NODES=${STAGING_REDIS_CLUSTER_NODES}
export REDIS_PASSWORD=${STAGING_REDIS_PASSWORD}

echo "📋 Running Redis Integration Tests..."

# Basic connectivity test
echo "Testing Redis connectivity..."
curl -f http://staging-app/actuator/health/redis || exit 1

# Distributed lock test
echo "Testing distributed locks..."
# Use REST API or CLI tools to test lock operations

# Concurrency test
echo "Testing concurrent lock operations..."
# Start multiple concurrent requests to test lock competition

# Failover test
echo "Testing Redis failover..."
# Simulate node failure, test automatic failover

echo "✅ Redis Integration Tests Completed"
```

**Test Content**:
- Redis connection and authentication
- Distributed lock acquisition and release
- Lock expiration and automatic cleanup
- Connection pool management
- Failover and recovery
- Performance benchmarking

### **2. Database Integration Tests**

**Test Script Example**:
```bash
#!/bin/bash
# staging-database-tests.sh

echo "🔧 Setting up Staging Database Tests..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=staging
export DB_HOST=${STAGING_DB_HOST}
export DB_USERNAME=${STAGING_DB_USERNAME}
export DB_PASSWORD=${STAGING_DB_PASSWORD}

echo "📋 Running Database Integration Tests..."

# Connectivity test
echo "Testing database connectivity..."
psql -h $DB_HOST -U $DB_USERNAME -d genai_demo -c "SELECT 1;" || exit 1

# Flyway Migration test
echo "Testing Flyway migrations..."
# Run application, verify Migration success

# Performance test
echo "Testing database performance..."
# Execute performance test queries

# Transaction test
echo "Testing transaction management..."
# Test transaction isolation and rollback

echo "✅ Database Integration Tests Completed"
```

### **3. MSK Kafka Integration Tests**

**Test Script Example**:
```bash
#!/bin/bash
# staging-kafka-tests.sh

echo "🔧 Setting up Staging Kafka Tests..."

# Set environment variables
export SPRING_PROFILES_ACTIVE=staging
export KAFKA_BOOTSTRAP_SERVERS=${STAGING_KAFKA_BOOTSTRAP_SERVERS}

echo "📋 Running Kafka Integration Tests..."

# Connectivity test
echo "Testing Kafka connectivity..."
kafka-topics --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --list || exit 1

# Event publishing test
echo "Testing event publishing..."
# Publish test events

# Event consumption test
echo "Testing event consumption..."
# Verify events are correctly consumed

# Failure recovery test
echo "Testing Kafka resilience..."
# Test recovery after network interruption

echo "✅ Kafka Integration Tests Completed"
```

### **4. End-to-End Tests**

**Test Script Example**:
```bash
#!/bin/bash
# staging-e2e-tests.sh

echo "🔧 Setting up Staging E2E Tests..."

# Set complete Staging environment
export SPRING_PROFILES_ACTIVE=staging
# ... all environment variables

echo "📋 Running End-to-End Tests..."

# Complete business process test
echo "Testing complete customer journey..."
# 1. Create customer
# 2. Create order
# 3. Process payment
# 4. Update inventory
# 5. Send notifications

# Cross-service data consistency test
echo "Testing cross-service data consistency..."
# Verify event-driven data consistency

# Performance and latency test
echo "Testing performance under load..."
# Execute load tests

echo "✅ End-to-End Tests Completed"
```

## 🚀 **CI/CD Integration**

### **GitHub Actions Workflow**

```yaml
# .github/workflows/staging-integration-tests.yml
name: Staging Integration Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  workflow_dispatch:  # Manual trigger

jobs:
  staging-tests:
    runs-on: ubuntu-latest
    if: |
      github.event_name == 'schedule' || 
      github.event_name == 'workflow_dispatch' ||
      contains(github.event.head_commit.message, '[staging-test]')
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v4
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: ap-northeast-1
    
    - name: Run Redis Integration Tests
      env:
        STAGING_REDIS_CLUSTER_NODES: ${{ secrets.STAGING_REDIS_CLUSTER_NODES }}
        STAGING_REDIS_PASSWORD: ${{ secrets.STAGING_REDIS_PASSWORD }}
      run: |
        chmod +x scripts/staging-redis-tests.sh
        ./scripts/staging-redis-tests.sh
    
    - name: Run Database Integration Tests
      env:
        STAGING_DB_HOST: ${{ secrets.STAGING_DB_HOST }}
        STAGING_DB_USERNAME: ${{ secrets.STAGING_DB_USERNAME }}
        STAGING_DB_PASSWORD: ${{ secrets.STAGING_DB_PASSWORD }}
      run: |
        chmod +x scripts/staging-database-tests.sh
        ./scripts/staging-database-tests.sh
    
    - name: Run Kafka Integration Tests
      env:
        STAGING_KAFKA_BOOTSTRAP_SERVERS: ${{ secrets.STAGING_KAFKA_BOOTSTRAP_SERVERS }}
      run: |
        chmod +x scripts/staging-kafka-tests.sh
        ./scripts/staging-kafka-tests.sh
    
    - name: Run End-to-End Tests
      env:
        STAGING_APP_URL: ${{ secrets.STAGING_APP_URL }}
      run: |
        chmod +x scripts/staging-e2e-tests.sh
        ./scripts/staging-e2e-tests.sh
    
    - name: Upload Test Reports
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: staging-test-reports
        path: |
          test-reports/
          logs/
    
    - name: Notify on Failure
      if: failure()
      uses: 8398a7/action-slack@v3
      with:
        status: failure
        text: 'Staging integration tests failed! Please check the logs.'
      env:
        SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

## 📊 **Test Monitoring and Reporting**

### **Test Results Dashboard**

Use CloudWatch or Grafana to build test results dashboard:

- **Test Execution Status**: Success/failure trends
- **Test Execution Time**: Execution time for various test types
- **Service Health Status**: Health status of Redis, RDS, MSK
- **Error Rate Analysis**: Error type and frequency analysis
- **Performance Metrics**: Response time, throughput trends

### **Alert Configuration**

```yaml
# CloudWatch Alarms for Staging Tests
StagingTestFailureAlarm:
  Type: AWS::CloudWatch::Alarm
  Properties:
    AlarmName: StagingTestFailure
    AlarmDescription: Staging integration tests are failing
    MetricName: TestFailureRate
    Namespace: GenAIDemo/StagingTests
    Statistic: Average
    Period: 300
    EvaluationPeriods: 1
    Threshold: 0.1  # 10% failure rate
    ComparisonOperator: GreaterThanThreshold
    AlarmActions:
      - !Ref SNSTopicArn
```

## 🔧 **Troubleshooting Guide**

### **Common Issues and Solutions**

#### **1. Redis Connection Failure**
```bash
# Check ElastiCache endpoint
aws elasticache describe-cache-clusters --show-cache-node-info

# Check security group settings
aws ec2 describe-security-groups --group-ids sg-xxx

# Test network connectivity
telnet redis-cluster-endpoint 6379
```

#### **2. Database Connection Timeout**
```bash
# Check RDS instance status
aws rds describe-db-instances --db-instance-identifier staging-db

# Check network connectivity
telnet staging-db-endpoint 5432

# Check connection pool configuration
echo "SELECT * FROM pg_stat_activity;" | psql -h $DB_HOST -U $DB_USERNAME -d genai_demo
```

#### **3. Kafka Connection Issues**
```bash
# Check MSK cluster status
aws kafka describe-cluster --cluster-arn arn:aws:kafka:...

# Check IAM permissions
aws sts get-caller-identity

# Test Kafka connectivity
kafka-console-producer --bootstrap-server $KAFKA_BOOTSTRAP_SERVERS --topic test-topic
```

### **Debugging Tools and Techniques**

```bash
# Enable verbose logging
export LOGGING_LEVEL_SOLID_HUMANK_GENAIDEMO=DEBUG
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=INFO

# Check application health status
curl -s http://staging-app/actuator/health | jq .

# Check metrics
curl -s http://staging-app/actuator/metrics | jq .

# Check environment variables
curl -s http://staging-app/actuator/env | jq .
```

## 📋 **Best Practices**

### **✅ Recommended Practices**

1. **Test Isolation**: Each test uses independent resources (different key prefixes, topics, etc.)
2. **Resource Cleanup**: Automatically clean up created resources after tests
3. **Retry Mechanism**: Implement appropriate retry for network-related tests
4. **Timeout Settings**: Set reasonable timeout values to avoid hanging tests
5. **Monitoring Integration**: Integrate test results into monitoring systems
6. **Cost Control**: Schedule test execution reasonably to control AWS costs
7. **Data Security**: Don't use real sensitive data in tests

### **🚨 Important Notes**

1. **Environment Stability**: Ensure Staging environment stability
2. **Version Consistency**: Ensure test code version matches deployment version
3. **Resource Limits**: Be aware of AWS service quotas and limits
4. **Network Security**: Ensure proper network security configuration for test environment
5. **Data Privacy**: Comply with data protection regulations

## 🔗 **Related Resources**

- Profile Management Strategy
- [Distributed Lock Contract Tests](../../app/src/test/java/solid/humank/genaidemo/infrastructure/common/lock/DistributedLockManagerContractTest.java)
- In-Memory Lock Implementation Tests
- [Deployment Viewpoint](../viewpoints/deployment/README.md)
- [Operational Viewpoint](../viewpoints/operational/README.md)

---

**Last Updated**: September 24, 2025 9:45 AM (Taipei Time)  
**Maintainer**: Development Team  
**Version**: 2.0.0  
**Status**: Active - Pure in-memory simulation strategy
