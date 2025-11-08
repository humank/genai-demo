# Code Review Guide

> **Last Updated**: 2025-10-25

## Overview

Code review is a critical part of our development process. This guide provides comprehensive guidelines for both authors and reviewers to ensure high-quality, maintainable code.

## Code Review Philosophy

### Core Principles

1. **Collaborative Learning**: Reviews are opportunities to learn and share knowledge
2. **Constructive Feedback**: Focus on improvement, not criticism
3. **Shared Ownership**: Everyone is responsible for code quality
4. **Continuous Improvement**: Learn from each review
5. **Respectful Communication**: Be kind and professional

### Goals

- **Quality Assurance**: Catch bugs and issues early
- **Knowledge Sharing**: Spread understanding across the team
- **Consistency**: Maintain coding standards
- **Mentorship**: Help team members grow
- **Documentation**: Ensure code is well-documented

## Review Process

### For Authors

#### Before Creating a Pull Request

**1. Self-Review**

```bash
# Review your own changes
git diff develop...feature/JIRA-123-new-feature

# Check for common issues

- Commented-out code
- Debug statements
- TODO comments
- Hardcoded values
- Missing tests

```

**2. Run All Checks**

```bash
# Run tests
./gradlew test

# Check code style
./gradlew checkstyleMain

# Run static analysis
./gradlew pmdMain spotbugsMain

# Verify build
./gradlew clean build
```

**3. Update Documentation**

- Update README if needed
- Add/update JavaDoc for public APIs
- Update API documentation
- Add comments for complex logic

**4. Prepare PR Description**

Use the PR template and provide:

- Clear description of changes
- Context and motivation
- Testing performed
- Screenshots (for UI changes)
- Breaking changes (if any)

#### During Review

**1. Respond Promptly**

- Acknowledge feedback within 24 hours
- Address comments systematically
- Ask for clarification if needed

**2. Be Open to Feedback**

```markdown
✅ Good Response:
"Good catch! I'll refactor this to use the builder pattern."

✅ Good Response:
"I chose this approach because X. However, I see your point about Y. 
Let me try your suggestion."

❌ Bad Response:
"This works fine, no need to change it."

❌ Bad Response:
"That's just your opinion."
```

**3. Resolve Conversations**

- Mark conversations as resolved after addressing
- Explain your changes in response
- Request re-review if significant changes made

#### After Approval

**1. Final Checks**

```bash
# Ensure branch is up to date
git checkout develop
git pull origin develop
git checkout feature/JIRA-123-new-feature
git rebase develop

# Run tests one more time
./gradlew test
```

**2. Merge**

- Use "Squash and merge" for feature branches
- Ensure commit message follows conventions
- Delete branch after merge

### For Reviewers

#### Review Checklist

**Functionality (Priority: High)**

- [ ] Code correctly implements the requirements
- [ ] Edge cases are handled
- [ ] Error conditions are handled properly
- [ ] Business logic is correct
- [ ] No obvious bugs

**Code Quality (Priority: High)**

- [ ] Code follows project coding standards
- [ ] Naming is clear and descriptive
- [ ] Methods are focused and not too long
- [ ] No code duplication
- [ ] Appropriate use of design patterns

**Architecture (Priority: High)**

- [ ] Follows hexagonal architecture
- [ ] Proper layer separation
- [ ] DDD patterns used correctly
- [ ] No architectural violations
- [ ] Dependencies are properly injected

**Testing (Priority: High)**

- [ ] Tests are included
- [ ] Tests are meaningful and comprehensive
- [ ] Tests follow naming conventions
- [ ] Test coverage is adequate (>80%)
- [ ] Tests actually test the right things

**Security (Priority: High)**

- [ ] No security vulnerabilities
- [ ] Input validation is present
- [ ] No SQL injection risks
- [ ] No XSS vulnerabilities
- [ ] Sensitive data is protected

**Performance (Priority: Medium)**

- [ ] No obvious performance issues
- [ ] Database queries are optimized
- [ ] No N+1 query problems
- [ ] Appropriate use of caching
- [ ] No memory leaks

**Documentation (Priority: Medium)**

- [ ] Public APIs have JavaDoc
- [ ] Complex logic is commented
- [ ] README updated if needed
- [ ] API documentation updated

**Maintainability (Priority: Medium)**

- [ ] Code is readable
- [ ] Code is maintainable
- [ ] No technical debt introduced
- [ ] Refactoring opportunities noted

#### Review Workflow

**1. Initial Review (15-30 minutes)**

```markdown

1. Read PR description and understand context
2. Review changed files list
3. Check test coverage report
4. Review tests first
5. Review implementation
6. Check for architectural issues

```

**2. Detailed Review**

**Start with Tests:**

```java
// Check test quality
@Test
void should_create_customer_when_valid_data_provided() {
    // ✅ Good: Clear Given-When-Then structure
    // ✅ Good: Descriptive test name
    // ✅ Good: Tests behavior, not implementation
}
```

**Review Implementation:**

```java
// Check for common issues
public class CustomerService {
    // ✅ Good: Constructor injection
    private final CustomerRepository repository;
    
    // ❌ Issue: Method too long
    public void processCustomer() {
        // 50+ lines of code
    }
    
    // ❌ Issue: No error handling
    public Customer findById(String id) {
        return repository.findById(id).get();
    }
}
```

**3. Provide Feedback**

Use conventional comments:

```markdown
**Blocking Issues (Must Fix):**
issue: This will cause a NullPointerException if customer is null.
Please add null check or use Optional.

issue: SQL injection vulnerability. Use parameterized queries instead
of string concatenation.

**Suggestions (Should Consider):**
suggestion: Consider extracting this logic into a separate method
for better readability.

suggestion: This could be simplified using Java streams.

**Questions (Need Clarification):**
question: Why did you choose this approach over using the existing
CustomerValidator?

question: Is this change backward compatible with the existing API?

**Minor Issues (Nice to Have):**
nit: Consider using a more descriptive variable name here.

nit: Missing JavaDoc for this public method.

**Positive Feedback:**
praise: Great use of the builder pattern here!

praise: Excellent test coverage for edge cases.
```

#### Review Comments Best Practices

**Be Specific:**

```markdown
❌ Bad: "This is wrong."

✅ Good: "This will throw NullPointerException when customer is null.
Consider using Optional.ofNullable() or adding a null check."
```

**Be Constructive:**

```markdown
❌ Bad: "Why would you do it this way?"

✅ Good: "I see you're using approach X. Have you considered approach Y?
It might be more maintainable because..."
```

**Provide Context:**

```markdown
❌ Bad: "Use streams here."

✅ Good: "Consider using streams here for better readability:
customers.stream()
    .filter(Customer::isActive)
    .collect(Collectors.toList())"
```

**Ask Questions:**

```markdown
✅ Good: "I'm not familiar with this pattern. Could you explain
why you chose this approach?"

✅ Good: "Have you considered the performance impact when the list
is very large?"
```

## Review Scenarios

### Scenario 1: New Feature

**What to Focus On:**

1. **Requirements**: Does it meet the acceptance criteria?
2. **Tests**: Are all scenarios covered?
3. **Architecture**: Does it fit the existing architecture?
4. **Documentation**: Is it properly documented?

**Example Review:**

```markdown
## Overall Assessment
Good implementation of the customer registration feature. The code is
clean and well-tested. I have a few suggestions for improvement.

## Blocking Issues
issue: Missing validation for duplicate email addresses. This could
cause database constraint violations.

## Suggestions
suggestion: Consider extracting the email validation logic into a
separate EmailValidator class for reusability.

suggestion: The CustomerRegistrationService.register() method is quite
long. Consider breaking it down into smaller methods.

## Questions
question: Should we send a welcome email immediately or queue it for
async processing?

## Positive Feedback
praise: Excellent test coverage! I especially like the edge case tests
for invalid email formats.

praise: Good use of the builder pattern for test data creation.
```

### Scenario 2: Bug Fix

**What to Focus On:**

1. **Root Cause**: Is the root cause addressed?
2. **Tests**: Is there a test that would have caught this bug?
3. **Similar Issues**: Are there similar issues elsewhere?
4. **Regression**: Could this fix break something else?

**Example Review:**

```markdown
## Overall Assessment
The fix addresses the immediate issue, but I have concerns about
potential similar issues in other parts of the codebase.

## Blocking Issues
issue: This fix only addresses the symptom. The root cause is that
we're not validating the discount percentage. Please add validation
in the Discount value object constructor.

## Suggestions
suggestion: Add a regression test that would have caught this bug.

suggestion: Search for similar discount calculations in other services
and verify they don't have the same issue.

## Questions
question: Have you verified this fix works with negative discount
percentages?
```

### Scenario 3: Refactoring

**What to Focus On:**

1. **Behavior**: Does behavior remain unchanged?
2. **Tests**: Do all tests still pass?
3. **Improvement**: Is the code actually better?
4. **Scope**: Is the scope appropriate?

**Example Review:**

```markdown
## Overall Assessment
Good refactoring that improves code readability. The extracted methods
make the logic much clearer.

## Suggestions
suggestion: Consider adding a comment explaining why we chose this
particular refactoring approach.

suggestion: The OrderValidator class could benefit from being made
package-private since it's only used within this package.

## Positive Feedback
praise: Much better! The extracted methods make the business logic
much easier to understand.

praise: Good job maintaining backward compatibility while improving
the internal structure.
```

## Common Review Patterns

### Pattern 1: Long Methods

**Issue:**

```java
public void processOrder(Order order) {
    // 50+ lines of mixed concerns
}
```

**Feedback:**

```markdown
suggestion: This method is quite long and handles multiple concerns.
Consider breaking it down:

```java
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);
    applyDiscounts(order);
    reserveInventory(order);
    processPayment(order);
    sendConfirmation(order);
}

private void validateOrder(Order order) {
    // Validation logic
}

private void calculateTotal(Order order) {
    // Calculation logic
}
```text

```

### Pattern 2: Missing Error Handling

**Issue:**
```java
public Customer findById(String id) {
    return repository.findById(id).get();
}
```

**Feedback:**

```markdown
issue: This will throw NoSuchElementException if customer not found.
Use orElseThrow with a specific exception:

```java
public Customer findById(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new CustomerNotFoundException(
            "Customer not found with ID: " + id));
}
```text

```

### Pattern 3: Insufficient Tests

**Issue:**
```java
@Test
void testCreateCustomer() {
    Customer customer = service.createCustomer(request);
    assertNotNull(customer);
}
```

**Feedback:**

```markdown
suggestion: This test is too weak. Consider testing:

- Actual customer properties
- Email validation
- Duplicate email handling
- Event publication

Example:
```java
@Test
void should_create_customer_with_correct_properties() {
    // Given
    CreateCustomerRequest request = new CreateCustomerRequest(
        "John Doe", "john@example.com", "password123"
    );
    
    // When
    Customer customer = service.createCustomer(request);
    
    // Then
    assertThat(customer.getName()).isEqualTo("John Doe");
    assertThat(customer.getEmail()).isEqualTo("john@example.com");
    assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
}
```text

```

## Review Metrics

### Time Guidelines

- **Small PR (< 200 lines)**: 15-30 minutes
- **Medium PR (200-500 lines)**: 30-60 minutes
- **Large PR (> 500 lines)**: 60+ minutes (consider splitting)

### Response Time

- **Initial Review**: Within 24 hours
- **Follow-up Review**: Within 4 hours
- **Final Approval**: Within 2 hours

### Quality Metrics

- **Approval Rate**: Aim for 70-80% approval on first review
- **Iteration Count**: Average 1-2 iterations per PR
- **Review Comments**: 3-5 meaningful comments per PR

## Tools and Automation

### Automated Checks

```yaml
# .github/workflows/pr-checks.yml
name: PR Checks

on: pull_request

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:

      - uses: actions/checkout@v3
      
      - name: Run Tests

        run: ./gradlew test
      
      - name: Check Code Style

        run: ./gradlew checkstyleMain
      
      - name: Run Static Analysis

        run: ./gradlew pmdMain spotbugsMain
      
      - name: Check Coverage

        run: ./gradlew jacocoTestCoverageVerification
```

### Review Tools

- **GitHub PR Review**: Primary review tool
- **SonarQube**: Automated code quality analysis
- **Codecov**: Test coverage tracking
- **Dependabot**: Dependency updates

## Best Practices

### For Authors

✅ **Do:**

- Keep PRs small and focused
- Provide context in PR description
- Respond to feedback promptly
- Be open to suggestions
- Thank reviewers for their time

❌ **Don't:**

- Create large PRs (>500 lines)
- Mix multiple concerns in one PR
- Take feedback personally
- Ignore reviewer comments
- Rush through reviews

### For Reviewers

✅ **Do:**

- Review promptly
- Be constructive and specific
- Ask questions when unclear
- Provide positive feedback
- Suggest alternatives

❌ **Don't:**

- Be overly critical
- Nitpick on style (use automated tools)
- Block on personal preferences
- Review when tired or rushed
- Forget to approve when satisfied

## Conflict Resolution

### When Reviewers Disagree

1. **Discuss**: Have a conversation to understand different perspectives
2. **Escalate**: Involve tech lead or architect if needed
3. **Document**: Record the decision and rationale
4. **Move Forward**: Make a decision and proceed

### When Author Disagrees with Feedback

1. **Understand**: Ask for clarification
2. **Explain**: Provide your reasoning
3. **Compromise**: Find middle ground
4. **Escalate**: Involve tech lead if needed

## Related Documentation

- [Git Workflow](git-workflow.md)
- [Coding Standards](../coding-standards/java-standards.md)
- [Testing Strategy](../testing/testing-strategy.md)
- [Architecture Principles](../../architecture/principles/design-principles.md)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-25  
**Maintained By**: Development Team
