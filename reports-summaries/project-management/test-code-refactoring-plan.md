# Test Code Refactoring Plan

**Created**: 2025年10月1日 下午8:35 (台北時間)  
**Purpose**: Reorganize test code according to profile-based development strategy  
**Status**: Planning Phase

## 📊 Current Test Code Analysis

### **Problem Summary**
- **Total Test Files**: ~150+ files
- **@SpringBootTest Tests**: ~30+ files (启动完整 Spring 容器)
- **External Service Dependencies**: ~25+ files (Redis, Kafka, Aurora, DynamoDB)
- **Cross-Region/DR Tests**: ~15+ files (跨区域、灾难恢复)
- **Performance/Concurrency Tests**: ~10+ files (高并发、性能测试)

### **Memory and Time Impact**
- **Current Unit Test**: ~500MB memory, ~2-5 seconds per test
- **Target Unit Test**: ~5MB memory, ~10-50ms per test
- **Integration Test**: Should run in staging environment only

## 🎯 Refactoring Strategy

### **Phase 1: Immediate Actions (Week 1-2)**

#### **1.1 Create Staging Test Structure**
- ✅ Created `staging-tests/` directory with complete structure
- ✅ Created execution scripts and Docker Compose configuration
- ✅ Created sample integration test (Aurora connection test)

#### **1.2 Identify Tests to Move**
```bash
# Tests to move to staging-tests/
app/src/test/java/solid/humank/genaidemo/
├── config/CrossRegionKafkaConfigurationTest.java          → staging-tests/integration/messaging/
├── config/RedisConfigurationTest.java                     → staging-tests/integration/cache/
├── config/DynamoDBConfigurationTest.java                  → staging-tests/integration/database/
├── config/EncryptionIntegrationTest.java                  → staging-tests/integration/security/
├── infrastructure/common/persistence/AuroraOptimisticLockingIntegrationTest.java → staging-tests/integration/database/
├── infrastructure/disaster/                               → staging-tests/cross-region/disaster-recovery/
├── infrastructure/performance/                            → staging-tests/performance/
├── infrastructure/observability/ (integration parts)      → staging-tests/integration/monitoring/
└── All @SpringBootTest with external dependencies         → staging-tests/integration/
```

#### **1.3 Simplify Java Unit Tests**
```bash
# Tests to keep and simplify in app/src/test/java/
├── domain/                          # ✅ Pure domain logic tests
├── application/                     # ✅ Application service logic tests  
├── architecture/                    # ✅ ArchUnit tests
├── config/ (simplified)             # ✅ Basic configuration tests only
└── testutils/                       # ✅ Test utilities and builders
```

### **Phase 2: Migration Execution (Week 3-4)**

#### **2.1 Move Integration Tests**
```bash
# Create migration script
./scripts/migrate-integration-tests.sh

# Move files with proper categorization
- Database integration tests → staging-tests/integration/database/
- Cache integration tests → staging-tests/integration/cache/
- Messaging integration tests → staging-tests/integration/messaging/
- Cross-region tests → staging-tests/cross-region/
- Performance tests → staging-tests/performance/
- Disaster recovery tests → staging-tests/cross-region/disaster-recovery/
```

#### **2.2 Convert to Unit Tests**
```java
// Before: Integration test with @SpringBootTest
@SpringBootTest
@ActiveProfiles("test")
class CustomerServiceIntegrationTest {
    @Autowired
    private CustomerService customerService;
    
    @Test
    void should_create_customer() {
        // Test with full Spring context
    }
}

// After: Pure unit test with mocks
@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void should_create_customer() {
        // Test business logic only
    }
}
```

#### **2.3 Update Build Configuration**
```gradle
// Update build.gradle test tasks
tasks.register('unitTest', Test) {
    description = 'Fast unit tests for local development'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '1g'  // Reduced from 6g
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // No JVM restart for speed
}

tasks.register('stagingTest', Exec) {
    description = 'Run staging integration tests'
    commandLine 'bash', 'staging-tests/scripts/run-integration-tests.sh'
    environment 'ENVIRONMENT', 'staging'
}
```

### **Phase 3: Optimization (Week 5-6)**

#### **3.1 Optimize Unit Tests**
- Remove unnecessary `@SpringBootTest` annotations
- Replace with `@ExtendWith(MockitoExtension.class)`
- Use test builders and fixtures
- Implement fast in-memory implementations

#### **3.2 Enhance Staging Tests**
- Add comprehensive integration test coverage
- Implement performance benchmarking
- Add cross-region testing scenarios
- Integrate with CI/CD pipeline

#### **3.3 Update Documentation**
- Update README with new test strategy
- Create developer guidelines
- Update CI/CD pipeline documentation

## 📋 Detailed Migration Plan

### **Files to Move to Staging Tests**

#### **Database Integration Tests**
```
Source → Destination
app/src/test/java/solid/humank/genaidemo/config/DynamoDBConfigurationTest.java
→ staging-tests/integration/database/test_dynamodb_integration.py

app/src/test/java/solid/humank/genaidemo/infrastructure/common/persistence/AuroraOptimisticLockingIntegrationTest.java
→ staging-tests/integration/database/test_aurora_optimistic_locking.py

app/src/test/java/solid/humank/genaidemo/infrastructure/persistence/DatabaseHealthValidator.java
→ staging-tests/integration/database/test_database_health.py
```

#### **Cache Integration Tests**
```
app/src/test/java/solid/humank/genaidemo/config/RedisConfigurationTest.java
→ staging-tests/integration/cache/test_redis_integration.py

app/src/test/java/solid/humank/genaidemo/infrastructure/cache/CrossRegionCacheService.java
→ staging-tests/integration/cache/test_cross_region_cache.py
```

#### **Messaging Integration Tests**
```
app/src/test/java/solid/humank/genaidemo/config/CrossRegionKafkaConfigurationTest.java
→ staging-tests/integration/messaging/test_kafka_integration.py

app/src/test/java/solid/humank/genaidemo/infrastructure/event/publisher/ProfileBasedEventPublishingIntegrationTest.java
→ staging-tests/integration/messaging/test_event_publishing.py
```

#### **Performance Tests**
```
app/src/test/java/solid/humank/genaidemo/infrastructure/performance/
→ staging-tests/performance/

app/src/test/java/solid/humank/genaidemo/infrastructure/common/lock/DistributedLockManagerContractTest.java
→ staging-tests/performance/test_distributed_locking.py
```

#### **Cross-Region Tests**
```
app/src/test/java/solid/humank/genaidemo/infrastructure/disaster/
→ staging-tests/cross-region/disaster-recovery/

app/src/test/java/solid/humank/genaidemo/config/EncryptionIntegrationTest.java
→ staging-tests/cross-region/test_encryption_integration.py
```

### **Files to Keep and Simplify**

#### **Domain Tests (Keep as Unit Tests)**
```
✅ Keep: app/src/test/java/solid/humank/genaidemo/domain/
- Pure business logic tests
- Value object tests  
- Domain service tests
- No external dependencies
```

#### **Application Tests (Convert to Unit Tests)**
```
✅ Convert: app/src/test/java/solid/humank/genaidemo/application/
- Remove @SpringBootTest
- Add @ExtendWith(MockitoExtension.class)
- Mock repositories and external services
- Focus on business logic validation
```

#### **Configuration Tests (Simplify)**
```
✅ Simplify: app/src/test/java/solid/humank/genaidemo/config/
- Keep: BasicProfileConfigurationUnitTest.java
- Keep: ProfileValidationTest.java
- Remove: Complex integration configuration tests
- Convert to unit tests with mocked dependencies
```

## 🚀 Expected Benefits

### **Development Experience**
- **Faster Feedback**: Unit tests run in < 2 minutes vs current 5-10 minutes
- **Lower Memory Usage**: 1GB vs current 6GB for test execution
- **Immediate Local Testing**: No external service dependencies
- **Better IDE Performance**: Faster test execution in IDE

### **CI/CD Pipeline**
- **Faster PR Validation**: Quick unit test feedback
- **Comprehensive Staging Validation**: Full integration testing in staging
- **Reduced CI Resource Usage**: Lighter unit test execution
- **Better Test Isolation**: Clear separation of test types

### **Test Quality**
- **Focused Testing**: Each test type has clear purpose
- **Better Coverage**: More comprehensive integration testing in staging
- **Realistic Testing**: Staging tests use production-like services
- **Maintainable Tests**: Clearer test organization and purpose

## 📅 Implementation Timeline

### **Week 1-2: Setup and Planning**
- [x] Create staging-tests directory structure
- [x] Create execution scripts and Docker Compose
- [x] Create sample integration tests
- [ ] Identify all tests to migrate
- [ ] Create migration scripts

### **Week 3-4: Migration Execution**
- [ ] Move integration tests to staging-tests
- [ ] Convert remaining tests to unit tests
- [ ] Update build configuration
- [ ] Update CI/CD pipeline

### **Week 5-6: Optimization and Documentation**
- [ ] Optimize unit test performance
- [ ] Enhance staging test coverage
- [ ] Update documentation
- [ ] Train team on new test strategy

### **Week 7-8: Validation and Rollout**
- [ ] Validate new test strategy
- [ ] Monitor test execution performance
- [ ] Gather team feedback
- [ ] Make final adjustments

## 🔧 Tools and Scripts

### **Migration Scripts**
```bash
# Create migration script
./scripts/create-migration-script.sh

# Execute migration
./scripts/migrate-integration-tests.sh

# Validate migration
./scripts/validate-test-migration.sh
```

### **Test Execution**
```bash
# Local development (fast unit tests)
./gradlew quickTest              # < 2 minutes
./gradlew unitTest               # < 5 minutes

# Staging environment (comprehensive integration tests)
cd staging-tests
./scripts/run-integration-tests.sh
./scripts/run-performance-tests.sh
./scripts/run-cross-region-tests.sh
```

### **CI/CD Integration**
```yaml
# .github/workflows/test-strategy.yml
name: Test Strategy
on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Run Unit Tests
        run: ./gradlew unitTest
        
  staging-tests:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop' || github.ref == 'refs/heads/main'
    environment: staging
    steps:
      - name: Run Integration Tests
        run: ./staging-tests/scripts/run-integration-tests.sh
```

## 📊 Success Metrics

### **Performance Metrics**
- **Unit Test Execution Time**: < 2 minutes (target)
- **Memory Usage**: < 1GB (target)
- **Test Coverage**: Maintain > 80%
- **CI/CD Pipeline Time**: Reduce by 50%

### **Quality Metrics**
- **Test Reliability**: > 99% pass rate
- **Test Maintainability**: Reduced test maintenance effort
- **Developer Satisfaction**: Improved development experience
- **Production Quality**: Maintained or improved production stability

This refactoring plan will transform your test strategy to align with your profile-based development approach, providing faster local development while maintaining comprehensive testing in staging environment.