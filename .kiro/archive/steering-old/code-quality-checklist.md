# Code Quality Checklist

## Overview

This document provides a comprehensive checklist for code quality standards. Use this as a quick reference during development and code reviews.

**Purpose**: Quick checklist for daily development and code reviews.
**Detailed Examples**: See `.kiro/examples/code-patterns/` for comprehensive guides.

---

## Naming Conventions

### Must Follow

- [ ] Classes: PascalCase (e.g., `OrderService`, `CustomerRepository`)
- [ ] Methods: camelCase with verb-noun pattern (e.g., `findCustomerById`, `calculateTotal`)
- [ ] Variables: camelCase, descriptive names (e.g., `customerEmail`, `orderTotal`)
- [ ] Constants: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_ATTEMPTS`, `DEFAULT_TIMEOUT`)
- [ ] Packages: lowercase, singular nouns (e.g., `domain.order.model`)
- [ ] Test methods: should_expectedBehavior_when_condition

### Must Avoid

- [ ] ❌ Abbreviations (e.g., `cust` instead of `customer`)
- [ ] ❌ Single letter variables (except loop counters)
- [ ] ❌ Hungarian notation (e.g., `strName`, `intCount`)
- [ ] ❌ Meaningless names (e.g., `data`, `info`, `manager`)

---

## Error Handling

### Must Follow

- [ ] Use specific exception types
- [ ] Include error context in exceptions
- [ ] Log errors with structured data
- [ ] Use try-with-resources for closeable resources
- [ ] Handle errors at appropriate level

### Must Avoid

- [ ] ❌ Empty catch blocks
- [ ] ❌ Generic `catch (Exception e)`
- [ ] ❌ Swallowing exceptions
- [ ] ❌ Using exceptions for control flow

### Quick Check

```java
// ✅ GOOD: Specific exception with context
throw new CustomerNotFoundException(
    "Customer not found",
    Map.of("customerId", customerId)
);

// ❌ BAD: Generic exception
throw new RuntimeException("Error");
```

**Detailed Guide**: #[[file:../examples/code-patterns/error-handling.md]]

---

## API Design

### Must Follow

- [ ] RESTful URL conventions (`/api/v1/customers`)
- [ ] Proper HTTP methods (GET, POST, PUT, DELETE)
- [ ] Consistent response format
- [ ] Input validation with `@Valid`
- [ ] Proper HTTP status codes

### HTTP Status Codes

- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST
- **204 No Content**: Successful DELETE
- **400 Bad Request**: Validation errors
- **404 Not Found**: Resource not found
- **409 Conflict**: Business rule violation
- **500 Internal Server Error**: System errors

**Detailed Guide**: #[[file:../examples/code-patterns/api-design.md]]

---

## Security

### Must Follow

- [ ] Input validation on all endpoints
- [ ] Parameterized queries (no string concatenation)
- [ ] Output encoding to prevent XSS
- [ ] Authentication on protected endpoints
- [ ] Authorization checks
- [ ] Sensitive data encryption

### Must Avoid

- [ ] ❌ SQL injection vulnerabilities
- [ ] ❌ XSS vulnerabilities
- [ ] ❌ Hardcoded credentials
- [ ] ❌ Sensitive data in logs

### Quick Check

```java
// ✅ GOOD: Parameterized query
@Query("SELECT c FROM Customer c WHERE c.email = :email")
Optional<Customer> findByEmail(@Param("email") String email);

// ❌ BAD: String concatenation (SQL injection risk)
String query = "SELECT * FROM customers WHERE email = '" + email + "'";
```

**Detailed Guide**: #[[file:../examples/code-patterns/security-patterns.md]]

---

## Performance

### Must Follow

- [ ] Database query optimization
- [ ] Proper indexing on frequently queried fields
- [ ] Use pagination for large result sets
- [ ] Implement caching for frequently accessed data
- [ ] Async processing for long-running operations
- [ ] Avoid N+1 query problems

### Quick Check

```java
// ✅ GOOD: Use JOIN FETCH to avoid N+1
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") String id);

// ❌ BAD: N+1 query problem
List<Order> orders = orderRepository.findAll();
orders.forEach(order -> order.getItems().size()); // N+1!
```

**Detailed Guide**: #[[file:../examples/code-patterns/performance-optimization.md]]

---

## Code Structure

### Method Length

- [ ] Methods < 20 lines
- [ ] Single level of abstraction per method
- [ ] Extract complex logic into separate methods

### Class Size

- [ ] Classes < 200 lines
- [ ] Single responsibility per class
- [ ] Split large classes into focused ones

### Parameter Lists

- [ ] Methods with ≤ 3 parameters
- [ ] Use parameter objects for > 3 parameters
- [ ] Use builder pattern for complex objects

---

## Documentation

### Must Follow

- [ ] Public APIs have Javadoc
- [ ] Complex logic has inline comments
- [ ] README updated for significant changes
- [ ] API documentation updated

### Javadoc Standards

```java
/**

 * Submits an order for processing.
 * 
 * @param command the order submission command
 * @return the submitted order
 * @throws OrderNotFoundException if order not found
 * @throws BusinessRuleViolationException if business rules violated

 */
public Order submitOrder(SubmitOrderCommand command) {
    // Implementation
}
```

---

## Code Review Checklist

### Functionality

- [ ] Code correctly implements requirements
- [ ] Edge cases handled properly
- [ ] Error conditions handled
- [ ] Business rules validated

### Design

- [ ] Follows SOLID principles
- [ ] Follows Tell, Don't Ask
- [ ] No Law of Demeter violations
- [ ] Appropriate use of design patterns

### Testing

- [ ] Unit tests for business logic
- [ ] Integration tests for infrastructure
- [ ] Test coverage > 80%
- [ ] Tests are clear and maintainable

### Security

- [ ] Input validation implemented
- [ ] No security vulnerabilities
- [ ] Sensitive data protected
- [ ] Authentication/authorization correct

### Performance

- [ ] No obvious performance issues
- [ ] Database queries optimized
- [ ] Caching used appropriately
- [ ] No memory leaks

### Maintainability

- [ ] Code is readable and clear
- [ ] Naming is descriptive
- [ ] No code duplication
- [ ] Documentation updated

**Detailed Review Guide**: #[[file:../examples/process/code-review-guide.md]]

---

## Validation Commands

### Code Quality

```bash
./gradlew test jacocoTestReport  # Check test coverage
./gradlew pmdMain                # Check code smells
./gradlew checkstyleMain         # Check code style
./gradlew spotbugsMain           # Find bugs
```

### Architecture

```bash
./gradlew archUnit               # Verify architecture rules
```

### Security

```bash
./gradlew dependencyCheckAnalyze # Check dependencies
```

---

## Quick Reference

| Category | Key Check | Tool |
|----------|-----------|------|
| Naming | Descriptive, consistent | Code review |
| Error Handling | Specific exceptions with context | PMD |
| API Design | RESTful, proper status codes | Code review |
| Security | Input validation, no SQL injection | SpotBugs |
| Performance | No N+1, proper indexing | Code review |
| Testing | Coverage > 80% | JaCoCo |

---

## Related Documentation

- **Core Principles**: #[[file:core-principles.md]]
- **Design Principles**: #[[file:design-principles.md]]
- **DDD Patterns**: #[[file:ddd-tactical-patterns.md]]
- **Code Pattern Examples**: #[[file:../examples/code-patterns/]]

---

**Document Version**: 1.0
**Last Updated**: 2025-01-17
**Owner**: Development Team
