# Test Performance Standards and Monitoring

## Overview

This document provides specialized reference for test performance monitoring, resource management, and optimization in our Spring Boot application.

> **📋 主要標準**: 基本的測試效能標準請參考 [Development Standards](development-standards.md#advanced-test-performance-framework)

> **🎯 用途**: 本文件作為測試效能監控的深度技術參考，包含詳細的實作指南和故障排除

## Test Performance Framework

### Core Components

#### 1. TestPerformanceExtension

Annotation-based performance monitoring for automatic test performance tracking.

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class MyIntegrationTest extends BaseIntegrationTest {
    // Tests are automatically monitored for performance
}
```

**Configuration Options:**

- `maxExecutionTimeMs`: Maximum allowed execution time (default: 5000ms)
- `maxMemoryIncreaseMB`: Maximum allowed memory increase (default: 50MB)
- `generateReports`: Whether to generate detailed reports (default: true)
- `checkRegressions`: Whether to check for performance regressions (default: true)

**Implementation Details:**

- Implemented as JUnit 5 extension using `@ExtendWith(TestPerformanceMonitor.class)`
- Provides automatic test execution time monitoring and memory usage tracking
- Generates detailed execution reports in `build/reports/test-performance/`
- Supports both class-level and method-level application

#### 2. TestPerformanceMonitor

JUnit 5 extension that provides comprehensive test performance monitoring.

**Features:**

- Test execution time tracking with millisecond precision
- Memory usage monitoring (heap memory before/after each test)
- Performance regression detection with configurable thresholds
- Detailed text-based reports (HTML reports via TestPerformanceReportGenerator)
- Slow test identification (>5s warning, >30s error)
- Concurrent test execution tracking with thread-safe data structures
- Automatic report generation in `build/reports/test-performance/`

**Performance Thresholds:**

- Slow Test Warning: > 5 seconds
- Very Slow Test Error: > 30 seconds  
- Memory Usage Warning: > 50MB increase

#### 3. TestPerformanceResourceManager

Component for monitoring and managing test resources.

```java
@TestComponent
public class TestPerformanceResourceManager {
    
    public ResourceUsageStats getResourceUsageStats() {
        // Returns current resource usage statistics including:
        // - Current memory usage and maximum available
        // - Memory usage percentage
        // - Active test resources count
    }
    
    public void forceCleanup() {
        // Forces cleanup of all test resources
        // Triggers System.gc() to free memory
    }
}
```

**ResourceUsageStats includes:**

- Total tests executed
- Current memory used vs maximum available
- Memory usage percentage calculation
- Total memory allocated during test execution
- Active test resources count

#### 4. TestPerformanceConfiguration

Spring Test configuration for performance monitoring setup.

```java
@TestConfiguration
@Profile("test")
public class TestPerformanceConfiguration {
    
    @Bean
    public TestPerformanceListener testPerformanceListener() {
        return new TestPerformanceListener();
    }
}
```

**TestPerformanceListener provides:**

- Automatic cleanup before and after each test method
- Database cleanup with proper foreign key constraint handling
- Cache clearing between tests
- Mock reset functionality
- Application state reset
- Temporary resource cleanup
- Final cleanup after test class completion

## Gradle Test Task Configuration

### Optimized Test Tasks

```gradle
// Unit tests - fast feedback for daily development
tasks.register('unitTest', Test) {
    description = 'Fast unit tests (~5MB, ~50ms each)'
    useJUnitPlatform {
        excludeTags 'integration', 'end-to-end', 'slow'
        includeTags 'unit'
    }
    maxHeapSize = '2g'
    maxParallelForks = Runtime.runtime.availableProcessors()
    forkEvery = 0  // No JVM restart for speed
}

// Integration tests - pre-commit verification
tasks.register('integrationTest', Test) {
    description = 'Integration tests (~50MB, ~500ms each)'
    useJUnitPlatform {
        includeTags 'integration'
        excludeTags 'end-to-end', 'slow'
    }
    maxHeapSize = '6g'
    minHeapSize = '2g'
    maxParallelForks = 1
    forkEvery = 5
    timeout = Duration.ofMinutes(30)
    
    // HttpComponents optimization and JVM tuning
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=1g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication',
        '-XX:G1HeapRegionSize=32m',
        '-XX:+UnlockExperimentalVMOptions',
        '-XX:G1NewSizePercent=20',
        '-XX:G1MaxNewSizePercent=30',
        '-Xshare:off',
        // HttpComponents specific JVM parameters
        '-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog',
        '-Dorg.apache.commons.logging.simplelog.showdatetime=true',
        '-Dorg.apache.commons.logging.simplelog.log.org.apache.http=DEBUG',
        '-Dorg.apache.http.wire=DEBUG',
        // Network timeout configuration
        '-Dsun.net.useExclusiveBind=false',
        '-Djava.net.preferIPv4Stack=true'
    ]
    
    // Enhanced system properties for integration tests
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '2m',
        'spring.profiles.active': 'test',
        'http.client.connection.timeout': '10000',
        'http.client.socket.timeout': '30000',
        'test.resource.cleanup.enabled': 'true',
        'test.memory.monitoring.enabled': 'true'
    ]
}

// End-to-end tests - pre-release verification
tasks.register('e2eTest', Test) {
    description = 'End-to-end tests (~500MB, ~3s each)'
    useJUnitPlatform {
        includeTags 'end-to-end'
    }
    maxHeapSize = '8g'
    minHeapSize = '3g'
    maxParallelForks = 1
    forkEvery = 2
    timeout = Duration.ofHours(1)
    
    // E2E test specific JVM parameters
    jvmArgs += [
        '--enable-preview',
        '-XX:MaxMetaspaceSize=2g',
        '-XX:+UseG1GC',
        '-XX:+UseStringDeduplication',
        '-XX:G1HeapRegionSize=32m',
        '-XX:+UnlockExperimentalVMOptions',
        '-XX:G1NewSizePercent=20',
        '-XX:G1MaxNewSizePercent=30',
        '-Xshare:off',
        '-Djava.security.egd=file:/dev/./urandom'
    ]
    
    // E2E test system properties
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '5m',
        'spring.profiles.active': 'test',
        'spring.main.lazy-initialization': 'false',
        'http.client.connection.timeout': '30000',
        'http.client.socket.timeout': '60000',
        'test.performance.monitoring.enabled': 'true'
    ]
}
```

### Test Task Hierarchy

```bash
# Development workflow
./gradlew quickTest              # Daily development (< 2 minutes)
./gradlew preCommitTest          # Pre-commit verification (< 5 minutes)
./gradlew fullTest               # Pre-release verification (< 30 minutes)

# Specific test types
./gradlew unitTest               # Unit tests only
./gradlew integrationTest        # Integration tests only
./gradlew e2eTest               # End-to-end tests only
./gradlew cucumber              # BDD Cucumber tests
```

## Performance Thresholds and Monitoring

### Performance Thresholds

- **Slow Test Warning**: > 5 seconds
- **Very Slow Test Error**: > 30 seconds
- **Memory Usage Warning**: > 50MB increase
- **Memory Usage Critical**: > 80% of available heap

### Automatic Performance Monitoring

#### Test Execution Monitoring

```java
// Automatic monitoring with TestPerformanceMonitor
public class TestPerformanceMonitor implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, TestWatcher {
    
    // Automatically tracks:
    // - Test execution times
    // - Memory usage during tests
    // - Performance regressions
    // - Resource cleanup
}
```

#### Performance Report Generation

- **HTML Reports**: Interactive charts and detailed analysis
- **CSV Exports**: Raw data for further analysis
- **Trend Analysis**: Performance regression detection
- **Resource Usage**: Memory and CPU utilization tracking

### Performance Report Structure

```
build/reports/test-performance/
├── performance-report.html          # Interactive HTML report with charts (via TestPerformanceReportGenerator)
├── performance-data.csv             # Raw performance data (via TestPerformanceReportGenerator)
├── overall-performance-summary.txt  # Summary statistics (via TestPerformanceMonitor)
└── {TestClass}-performance-report.txt # Individual class reports (via TestPerformanceMonitor)
```

**Report Contents:**

- **Individual Class Reports**: Test execution times, memory usage, failure causes
- **Overall Summary**: Total tests executed, success rates, average execution times
- **Performance Analysis**: Slow test identification, top 5 slowest tests
- **HTML Reports**: Interactive charts and detailed analysis (generated separately)
- **CSV Data**: Raw performance data for further analysis

## Test Resource Management

### Resource Cleanup Strategy

#### Automatic Cleanup

```java
// TestPerformanceConfiguration provides automatic cleanup
public static class TestPerformanceListener extends AbstractTestExecutionListener {
    
    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        // Automatic cleanup after each test method:
        // - Database cleanup
        // - Cache clearing
        // - Mock resetting
        // - Temporary resource cleanup
    }
}
```

#### Manual Resource Management

```java
// BaseIntegrationTest provides manual resource management
protected void forceResourceCleanup() {
    // Force cleanup when needed during tests
}

protected boolean isMemoryUsageAcceptable() {
    // Check memory usage within acceptable limits
}

protected void waitForCondition(BooleanSupplier condition, Duration timeout, String description) {
    // Wait for asynchronous operations with timeout
}
```

### Memory Management Best Practices

#### JVM Configuration for Tests

```gradle
// Optimized JVM parameters for test execution
jvmArgs += [
    '--enable-preview',
    '-XX:MaxMetaspaceSize=1g',
    '-XX:+UseG1GC',
    '-XX:+UseStringDeduplication',
    '-XX:G1HeapRegionSize=32m',
    '-XX:+UnlockExperimentalVMOptions',
    '-XX:G1NewSizePercent=20',
    '-XX:G1MaxNewSizePercent=30',
    '-Xshare:off'
]
```

#### Memory Monitoring

- **Warning Threshold**: 80% memory usage
- **Critical Threshold**: 90% memory usage
- **Automatic GC**: Triggered on critical usage
- **Periodic Cleanup**: Every 5 tests

#### 5. TestPerformanceReportGenerator

Standalone utility for generating comprehensive HTML and CSV performance reports.

```bash
# Generate performance reports
./gradlew generatePerformanceReport
```

**Generated Reports:**

- **HTML Report**: Interactive charts and detailed performance analysis
- **CSV Report**: Raw performance data for further analysis
- **Trend Analysis**: Performance regression detection over time
- **Resource Usage**: Memory and execution time correlations

## Integration with Existing Tools

### Allure Integration

```gradle
// Allure reporting with performance data
systemProperty 'allure.results.directory', layout.buildDirectory.dir("allure-results").get().asFile.absolutePath
systemProperty 'allure.epic', 'Performance Testing'
systemProperty 'allure.feature', 'Test Performance Monitoring'
```

### Cucumber Integration

```gradle
// Cucumber with performance monitoring
tasks.register('cucumber', JavaExec) {
    maxHeapSize = '4g'
    args = [
        '--plugin', 'io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm',
        '--glue', 'solid.humank.genaidemo.bdd',
        'src/test/resources/features'
    ]
}
```

## Best Practices

### Test Performance Optimization

1. **Use Appropriate Test Types**:
   - Unit tests for business logic (fast, isolated)
   - Integration tests for component interaction (moderate)
   - E2E tests for complete workflows (slow, comprehensive)

2. **Resource Management**:
   - Enable performance monitoring with `@TestPerformanceExtension`
   - Use `BaseIntegrationTest` for consistent setup
   - Implement proper cleanup in test methods

3. **Memory Optimization**:
   - Monitor memory usage during tests
   - Force cleanup when memory usage is high
   - Use appropriate heap sizes for different test types

4. **Performance Regression Detection**:
   - Automatic detection of slow tests
   - Performance trend analysis
   - Threshold-based alerting

### Test Execution Strategy

#### Development Phase

```bash
./gradlew quickTest    # Fast feedback during development
```

#### Pre-Commit Phase

```bash
./gradlew preCommitTest    # Comprehensive verification before commit
```

#### Pre-Release Phase

```bash
./gradlew fullTest    # Complete test suite including performance validation
```

## Monitoring and Reporting

### Performance Metrics

- **Test Execution Time**: Per test and per class
- **Memory Usage**: Before/after each test
- **Resource Utilization**: CPU, memory, database connections
- **Failure Rates**: Success/failure statistics

### Report Generation

```bash
# Generate performance reports
./gradlew generatePerformanceReport

# View reports
open build/reports/test-performance/performance-report.html
```

### Performance Regression Detection

- Automatic detection of tests exceeding thresholds
- Historical performance comparison
- Trend analysis and alerting
- Integration with CI/CD pipelines

This framework ensures consistent, monitored, and optimized test performance across the entire application.
