# Test Performance Monitoring Framework

## Overview

This project implements a comprehensive test performance monitoring framework that automatically tracks test execution time, memory usage, and generates detailed performance analysis reports.

## 🚀 Core Features

### TestPerformanceExtension

The core component for automated test performance monitoring:

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class MyIntegrationTest extends BaseIntegrationTest {
    // Test methods are automatically monitored for performance
    
    @Test
    void should_process_order_within_performance_limits() {
        // Test logic
        // Automatically monitors execution time and memory usage
    }
}
```

### Configuration Options

- `maxExecutionTimeMs`: Maximum allowed execution time (default: 5000ms)
- `maxMemoryIncreaseMB`: Maximum allowed memory increase (default: 50MB)
- `generateReports`: Whether to generate detailed reports (default: true)
- `checkRegressions`: Whether to check for performance regressions (default: true)

## 📊 Monitoring Metrics

### Execution Time Tracking

- **Millisecond Precision**: Accurately tracks execution time for each test method
- **Slow Test Identification**: Automatically marks tests exceeding 5 seconds as slow tests
- **Very Slow Test Warnings**: Tests exceeding 30 seconds are marked as errors

### Memory Usage Monitoring

- **Heap Memory Tracking**: Compares heap memory usage before and after tests
- **Memory Growth Calculation**: Precisely calculates memory growth during tests
- **Memory Usage Percentage**: Usage relative to maximum available memory

### Performance Thresholds

- **Slow Test Warning**: > 5 seconds
- **Very Slow Test Error**: > 30 seconds
- **Memory Usage Warning**: > 50MB increase
- **Memory Usage Critical**: > 80% of available heap memory

## 🛠️ Core Components

### TestPerformanceMonitor

JUnit 5 extension providing comprehensive test performance monitoring:

```java
public class TestPerformanceMonitor implements BeforeAllCallback, AfterAllCallback,
        BeforeEachCallback, AfterEachCallback, TestWatcher {
    
    // Automatically tracks:
    // - Test execution times
    // - Memory usage during tests
    // - Performance regressions
    // - Resource cleanup
}
```

**Key Features**:

- Concurrent test execution tracking (thread-safe data structures)
- Automatic report generation in `build/reports/test-performance/`
- Performance regression detection
- Slow test identification and warnings

### TestPerformanceResourceManager

Test resource monitoring and management component:

```java
@TestComponent
public class TestPerformanceResourceManager {
    
    public ResourceUsageStats getResourceUsageStats() {
        // Returns current resource usage statistics:
        // - Current memory usage and maximum available memory
        // - Memory usage percentage
        // - Active test resources count
    }
    
    public void forceCleanup() {
        // Forces cleanup of all test resources
        // Triggers System.gc() to free memory
    }
}
```

### TestPerformanceConfiguration

Spring test configuration for performance monitoring setup:

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

**TestPerformanceListener provides**:

- Automatic cleanup before and after each test method
- Database cleanup (properly handles foreign key constraints)
- Cache clearing
- Mock reset functionality
- Application state reset
- Temporary resource cleanup
- Final cleanup after test class completion

## 📈 Reporting System

### Report Structure

```text
build/reports/test-performance/
├── performance-report.html          # Interactive HTML report (via TestPerformanceReportGenerator)
├── performance-data.csv             # Raw performance data (via TestPerformanceReportGenerator)
├── overall-performance-summary.txt  # Overall statistics (via TestPerformanceMonitor)
└── {TestClass}-performance-report.txt # Individual class reports (via TestPerformanceMonitor)
```

### Report Contents

#### Individual Class Reports

- Test execution times
- Memory usage
- Failure cause analysis

#### Overall Summary Report

- Total tests executed
- Success rate statistics
- Average execution times
- Performance analysis: slow test identification, top 5 slowest tests

#### HTML Reports

- Interactive charts and detailed analysis (generated separately)
- Visual performance trends
- Drillable detailed data

#### CSV Data

- Raw performance data for further analysis
- Can be imported into other analysis tools

## 🔧 Gradle Test Task Configuration

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
        // HttpComponents specific JVM parameters
        '-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.SimpleLog',
        '-Dsun.net.useExclusiveBind=false',
        '-Djava.net.preferIPv4Stack=true'
    ]
    
    // Enhanced system properties for integration tests
    systemProperties = [
        'junit.jupiter.execution.timeout.default': '2m',
        'spring.profiles.active': 'test',
        'test.resource.cleanup.enabled': 'true',
        'test.memory.monitoring.enabled': 'true'
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

## 🎯 Performance Thresholds and Monitoring

### Monitoring Thresholds

- **Slow Test Warning**: > 5 seconds
- **Very Slow Test Error**: > 30 seconds
- **Memory Usage Warning**: > 50MB increase
- **Memory Usage Critical**: > 80% of available heap memory

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

#### Automated Report Generation

- **HTML Reports**: Interactive charts and detailed analysis
- **CSV Exports**: Raw data for further analysis
- **Trend Analysis**: Performance regression detection
- **Resource Usage**: Memory and CPU utilization tracking

## 🧹 Test Resource Management

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

## 📊 Usage Examples

### Basic Usage

```java
@TestPerformanceExtension(maxExecutionTimeMs = 5000, maxMemoryIncreaseMB = 50)
@IntegrationTest
public class OrderProcessingTest extends BaseIntegrationTest {
    
    @Test
    void should_process_order_quickly() {
        // Test logic
        // Automatically monitors performance
    }
}
```

### Advanced Configuration

```java
@TestPerformanceExtension(
    maxExecutionTimeMs = 10000,
    maxMemoryIncreaseMB = 100,
    generateReports = true,
    checkRegressions = true
)
@IntegrationTest
public class ComplexIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_handle_complex_scenario() {
        // Complex test logic
        // Higher performance thresholds
    }
}
```

### Performance Report Generation

```bash
# Generate performance reports
./gradlew generatePerformanceReport

# Run all tests and generate reports
./gradlew runAllTestsWithReport

# View reports
open build/reports/test-performance/performance-report.html
```

## 🔍 Troubleshooting

### Common Issues

#### Out of Memory

- Increase test JVM heap memory size
- Check for memory leaks in tests
- Use `forceResourceCleanup()` for manual cleanup

#### Slow Test Execution

- Check slow test reports
- Optimize database queries
- Reduce unnecessary Spring context loading

#### Performance Regressions

- Review performance trend reports
- Compare historical execution data
- Identify performance bottlenecks

### Monitoring and Reporting

#### Performance Metrics

- **Test Execution Time**: Per test and per class
- **Memory Usage**: Before/after each test
- **Resource Utilization**: CPU, memory, database connections
- **Failure Rates**: Success/failure statistics

#### Report Generation

```bash
# Generate performance reports
./gradlew generatePerformanceReport

# View reports
open build/reports/test-performance/performance-report.html
```

#### Performance Regression Detection

- Automatic detection of tests exceeding thresholds
- Historical performance comparison
- Trend analysis and alerting
- Integration with CI/CD pipelines

This framework ensures consistent, monitored, and optimized test performance across the entire application.
