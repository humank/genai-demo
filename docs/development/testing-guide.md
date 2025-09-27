# Testing Guide

Please refer to our comprehensive testing guidelines:

- **Development Standards - Testing** (Please refer to internal project documentation)
- **Test Performance Standards** (Please refer to internal project documentation)
- **Performance Standards** (Please refer to internal project documentation)

## Quick Reference

For detailed testing standards and guidelines, please consult:

1. **[Development Standards](../../.kiro/steering/development-standards.md)** - Core testing requirements and standards
2. **[Test Performance Standards](../../.kiro/steering/test-performance-standards.md)** - Performance monitoring and optimization
3. **[Performance Standards](../../.kiro/steering/performance-standards.md)** - Overall performance guidelines

## Testing Framework

Our project uses a comprehensive testing framework including:

- **Unit Testing**: JUnit 5 + Mockito + AssertJ
- **Integration Testing**: Spring Boot Test + TestContainers
- **BDD Testing**: Cucumber 7 + Gherkin
- **Architecture Testing**: ArchUnit
- **Performance Testing**: Custom performance monitoring extensions

## Test Categories

- **Unit Tests**: Fast, isolated tests for business logic
- **Integration Tests**: Component interaction testing
- **End-to-End Tests**: Complete user journey testing
- **Performance Tests**: Load and stress testing
- **Architecture Tests**: Architectural compliance verification

For detailed implementation guidelines, please refer to the internal steering documentation.
