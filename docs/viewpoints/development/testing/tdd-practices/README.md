# Test-Driven Development Practices

## Overview

This directory contains practical guides and best practices for Test-Driven Development (TDD).

## TDD Principles

### Red-Green-Refactor Cycle
1. **Red** - Write a failing test
2. **Green** - Write minimal code to make the test pass
3. **Refactor** - Refactor code to improve quality

### Test Structure
- **Arrange** - Set up test data
- **Act** - Execute the behavior being tested
- **Assert** - Verify the results

## Test Categories

### Unit Tests (80%)
- **Characteristics**: Fast, isolated, focused
- **Scope**: Pure business logic, utility functions
- **Tools**: JUnit 5, Mockito, AssertJ
- **Performance**: < 50ms, < 5MB

### Integration Tests (15%)
- **Characteristics**: Component interaction, database integration
- **Scope**: Repository, external services
- **Tools**: @DataJpaTest, @WebMvcTest
- **Performance**: < 500ms, < 50MB

### End-to-End Tests (5%)
- **Characteristics**: Complete business process verification
- **Scope**: User journeys, system integration
- **Tools**: @SpringBootTest, TestContainers
- **Performance**: < 3s, < 500MB

## Best Practices

### Test Naming
```java
@Test
void should_throw_exception_when_email_is_invalid() {
    // Test content
}
```

### Test Data Builders
```java
public static CustomerTestDataBuilder aCustomer() {
    return new CustomerTestDataBuilder();
}

Customer customer = aCustomer()
    .withName("John Doe")
    .withEmail("john@example.com")
    .build();
```

### Mock Usage Principles
- Mock external dependencies
- Don't mock value objects
- Avoid over-mocking

## Related Documentation

- [TDD/BDD Testing](../tdd-bdd-testing.md)
- [Test Performance Standards](../../../../../.kiro/steering/test-performance-standards.md)
- [Development Standards](../../../../../.kiro/steering/development-standards.md)

---

**Maintainer**: Development Team  
**Last Updated**: January 21, 2025
![Microservices Overview](../../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)