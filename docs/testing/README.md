# Integration Testing Documentation

## Overview

This directory contains comprehensive documentation for the integration testing framework, particularly focusing on the unified HTTP client configuration strategy and reactivated test suite.

## Quick Start

- **New to the project?** Start with the [New Developer Onboarding Guide](new-developer-onboarding-guide.md)
- **Need to run tests?** Check the [Test Execution and Maintenance Guide](test-execution-maintenance-guide.md)
- **Having issues?** Consult the [Troubleshooting Guides](#troubleshooting)

## Documentation Index

### Core Guides

#### 🚀 [New Developer Onboarding Guide](new-developer-onboarding-guide.md)

**Start here if you're new to the project**

- Quick 5-minute setup verification
- Your first integration test walkthrough
- Common patterns and best practices
- Performance monitoring basics
- Week-by-week learning plan

#### 📋 [Test Execution and Maintenance Guide](test-execution-maintenance-guide.md)

**Complete guide for running and maintaining tests**

- Test categories and classification (Unit, Integration, E2E)
- Execution commands for different scenarios
- Performance monitoring and optimization
- Maintenance procedures and schedules
- CI/CD integration

### Technical Configuration

#### ⚙️ [HTTP Client Configuration Guide](http-client-configuration-guide.md)

**Deep dive into the unified HTTP client strategy**

- Problem background and solution architecture
- UnifiedTestHttpClientConfiguration details
- Implementation guidelines and best practices
- Dependency management and Gradle configuration
- Migration checklist for existing tests

#### 📝 [Test Configuration Examples](test-configuration-examples.md)

**Practical examples for different test scenarios**

- Basic integration test patterns
- Advanced observability testing
- Custom configuration examples
- Error handling patterns
- Performance optimization examples

### Troubleshooting

#### 🔧 [TestRestTemplate Troubleshooting Guide](testresttemplate-troubleshooting-guide.md)

**Solutions for HTTP client and TestRestTemplate issues**

- NoClassDefFoundError resolution
- Connection timeout solutions
- Configuration conflict resolution
- SSL/TLS certificate issues
- Port conflict prevention

#### 🚨 [Common Test Failures Troubleshooting](common-test-failures-troubleshooting.md)

**Comprehensive troubleshooting for test failures**

- HTTP client and connection issues
- Configuration and bean creation problems
- Memory and performance issues
- Test data and state conflicts
- Environment and CI/CD issues

## Quick Reference

### Test Execution Commands

```bash
# Daily development - fast feedback
./gradlew quickTest              # Unit tests only (< 2 minutes)

# Pre-commit verification  
./gradlew preCommitTest          # Unit + Integration (< 5 minutes)

# Pre-release validation
./gradlew fullTest               # All tests including E2E (< 30 minutes)

# Specific test types
./gradlew unitTest               # Fast unit tests
./gradlew integrationTest        # Integration tests with HTTP client
./gradlew e2eTest               # End-to-end tests
./gradlew cucumber              # BDD Cucumber tests

# Performance monitoring
./gradlew generatePerformanceReport  # Generate performance reports
```

### Performance Thresholds

| Test Type | Execution Time | Memory Usage | Success Rate |
|-----------|---------------|--------------|--------------|
| Unit Tests | < 50ms | < 5MB | > 99% |
| Integration Tests | < 500ms | < 50MB | > 95% |
| E2E Tests | < 3s | < 500MB | > 90% |

### Test Class Template

```java
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
class MyFeatureIntegrationTest extends BaseIntegrationTest {
    
    @Test
    void should_validate_feature_functionality() {
        // Given
        String endpoint = baseUrl + "/api/v1/my-feature";
        
        // When
        ResponseEntity<String> response = restTemplate.getForEntity(endpoint, String.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    
    @AfterEach
    void cleanup() {
        if (!isMemoryUsageAcceptable()) {
            forceResourceCleanup();
        }
    }
}
```

## Key Components

### BaseIntegrationTest

- Pre-configured `TestRestTemplate` with proper timeouts
- Random port assignment for test isolation
- Resource management utilities
- Performance monitoring integration

### @TestPerformanceExtension

- Automatic execution time monitoring
- Memory usage tracking
- Performance regression detection
- Report generation

### UnifiedTestHttpClientConfiguration

- Consistent HTTP client setup across all tests
- HttpComponents dependency management
- Timeout configuration for test environments

## Best Practices Summary

### ✅ Do This

- Extend `BaseIntegrationTest` for integration tests
- Use `@TestPerformanceExtension` for performance monitoring
- Implement proper resource cleanup in `@AfterEach`
- Use the provided `TestRestTemplate` instance
- Generate unique test data to avoid conflicts
- Follow Given-When-Then test structure

### ❌ Avoid This

- Creating custom `TestRestTemplate` instances
- Skipping performance monitoring annotations
- Ignoring memory usage warnings
- Using hardcoded URLs instead of `baseUrl`
- Leaving resources uncleaned after tests
- Writing tests that depend on each other

## Performance Monitoring

### Reports Location

- **HTML Reports**: `build/reports/test-performance/performance-report.html`
- **Text Summaries**: `build/reports/test-performance/overall-performance-summary.txt`
- **Individual Reports**: `build/reports/test-performance/{TestClass}-performance-report.txt`

### Key Metrics

- **Execution Time**: Per test and per class
- **Memory Usage**: Before/after each test with increase tracking
- **Resource Utilization**: CPU, memory, database connections
- **Performance Trends**: Regression detection over time

## Troubleshooting Quick Reference

| Issue | Quick Fix | Documentation |
|-------|-----------|---------------|
| NoClassDefFoundError | Check HttpComponents dependencies | [TestRestTemplate Troubleshooting](testresttemplate-troubleshooting-guide.md#noclassdeffounderror-httpcomponents-classes) |
| Connection Timeout | Verify timeout configuration | [TestRestTemplate Troubleshooting](testresttemplate-troubleshooting-guide.md#testresttemplate-connection-timeouts) |
| Memory Issues | Increase heap size, implement cleanup | [Common Failures](common-test-failures-troubleshooting.md#outofmemoryerror-during-tests) |
| Bean Conflicts | Remove redundant configurations | [Common Failures](common-test-failures-troubleshooting.md#multiple-bean-definition-conflicts) |
| Port Conflicts | Use random port assignment | [TestRestTemplate Troubleshooting](testresttemplate-troubleshooting-guide.md#port-conflicts-in-parallel-test-execution) |

## Getting Help

### Documentation Path

1. **Start Here**: [New Developer Onboarding Guide](new-developer-onboarding-guide.md)
2. **Learn Patterns**: [Test Configuration Examples](test-configuration-examples.md)
3. **Understand Architecture**: [HTTP Client Configuration Guide](http-client-configuration-guide.md)
4. **Troubleshoot Issues**: [Troubleshooting Guides](#troubleshooting)
5. **Maintain Tests**: [Test Execution and Maintenance Guide](test-execution-maintenance-guide.md)

### Team Resources

- **Pair Programming**: Schedule with experienced team members
- **Code Reviews**: Get feedback on test implementations
- **Performance Reports**: Review and optimize based on metrics
- **Team Meetings**: Discuss testing strategies and improvements

## Related Documentation

- **[Performance Standards](../performance-standards.md)** - Overall performance guidelines
- **[Development Standards](../development-standards.md)** - General development practices
- **[Security Standards](../security-standards.md)** - Security testing requirements
- **[Test Optimization Guidelines](test-optimization-guidelines.md)** - Existing optimization guide

## Contributing to Documentation

When updating this documentation:

1. **Keep Examples Current**: Ensure code examples match current implementation
2. **Update Performance Thresholds**: Adjust based on actual performance data
3. **Add New Patterns**: Document new testing patterns as they emerge
4. **Maintain Links**: Ensure all internal links work correctly
5. **Test Instructions**: Verify all commands and examples work

## Feedback and Improvements

This documentation is continuously improved based on:

- Developer feedback and questions
- Common issues encountered in practice
- Performance monitoring insights
- New testing patterns and requirements

Please contribute improvements and report issues to help maintain high-quality documentation that serves the entire development team.
