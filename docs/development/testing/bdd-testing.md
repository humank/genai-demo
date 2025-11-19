# BDD Testing Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide provides comprehensive guidance for Behavior-Driven Development (BDD) testing using Cucumber and Gherkin in the GenAI Demo project.

---

## Quick Reference

For complete testing standards, see:
- [Testing Strategy](testing-strategy.md) - Complete testing approach
- [Testing Standards](.kiro/steering/testing-strategy.md) - Testing development standards
- [Unit Testing](unit-testing.md) - Unit testing guide

---

## BDD Principles

### What is BDD?

Behavior-Driven Development (BDD) is a software development approach that:
- Uses natural language to describe system behavior
- Focuses on business value and user needs
- Enables collaboration between technical and non-technical stakeholders
- Provides living documentation

### Key Benefits

✅ **Improved Communication**: Business and technical teams speak the same language
✅ **Living Documentation**: Tests serve as up-to-date documentation
✅ **Business Focus**: Tests describe business behavior, not implementation
✅ **Early Bug Detection**: Scenarios catch misunderstandings early

---

## Gherkin Syntax

### Basic Structure

```gherkin
Feature: Order Submission
  As a customer
  I want to submit my order
  So that I can purchase products

  Scenario: Submit order successfully
    Given a customer with ID "CUST-001"
    And the customer has items in shopping cart
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved
```

### Gherkin Keywords

- **Feature**: High-level description of a software feature
- **Scenario**: Concrete example of business rule
- **Given**: Initial context (preconditions)
- **When**: Event or action
- **Then**: Expected outcome
- **And**: Additional steps
- **But**: Negative additional steps

---

## Writing Good Scenarios

### Best Practices

✅ **Do**:
- Use business language, not technical terms
- Focus on behavior, not implementation
- Keep scenarios independent
- Use concrete examples
- Make scenarios readable by non-technical stakeholders

❌ **Don't**:
- Include implementation details
- Make scenarios dependent on each other
- Use technical jargon
- Write overly complex scenarios

### Good vs Bad Examples

```gherkin
# ✅ Good: Business-focused, clear
Scenario: Customer receives discount for large order
  Given a premium customer
  And the customer has items worth $150 in cart
  When the customer proceeds to checkout
  Then a 10% discount should be applied
  And the final total should be $135

# ❌ Bad: Too technical, implementation-focused
Scenario: Discount calculation
  Given CustomerRepository.findById returns premium customer
  And OrderService.calculateTotal returns 150.00
  When DiscountService.applyDiscount is called
  Then the result should be 135.00
```

---

## Step Definitions

### Implementing Steps

```java
@Given("a customer with ID {string}")
public void aCustomerWithId(String customerId) {
    this.customerId = CustomerId.of(customerId);
    this.customer = customerRepository.findById(customerId)
        .orElseThrow(() -> new CustomerNotFoundException(customerId));
}

@Given("the customer has items in shopping cart")
public void theCustomerHasItemsInShoppingCart() {
    this.cart = ShoppingCart.create(customerId);
    cart.addItem(ProductId.of("PROD-001"), 2);
    cart.addItem(ProductId.of("PROD-002"), 1);
}

@When("the customer submits the order")
public void theCustomerSubmitsTheOrder() {
    SubmitOrderCommand command = new SubmitOrderCommand(
        customerId,
        cart.getItems()
    );
    this.order = orderService.submitOrder(command);
}

@Then("the order status should be {string}")
public void theOrderStatusShouldBe(String expectedStatus) {
    assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
}

@Then("an order confirmation email should be sent")
public void anOrderConfirmationEmailShouldBeSent() {
    verify(emailService).sendOrderConfirmation(
        eq(customer.getEmail()),
        eq(order.getId())
    );
}
```

---

## Scenario Outlines

### Data-Driven Testing

```gherkin
Scenario Outline: Calculate shipping cost based on weight
  Given an order with weight <weight> kg
  When calculating shipping cost
  Then the shipping cost should be $<cost>

  Examples:
    | weight | cost  |
    | 1      | 5.00  |
    | 5      | 10.00 |
    | 10     | 15.00 |
    | 20     | 25.00 |
```

### Implementation

```java
@Given("an order with weight {double} kg")
public void anOrderWithWeight(double weight) {
    this.order = Order.builder()
        .weight(Weight.of(weight))
        .build();
}

@When("calculating shipping cost")
public void calculatingShippingCost() {
    this.shippingCost = shippingService.calculateCost(order);
}

@Then("the shipping cost should be ${double}")
public void theShippingCostShouldBe(double expectedCost) {
    assertThat(shippingCost.getAmount())
        .isEqualByComparingTo(BigDecimal.valueOf(expectedCost));
}
```

---

## Background Steps

### Shared Setup

```gherkin
Feature: Order Management

  Background:
    Given the system is running
    And the database is clean
    And a customer with ID "CUST-001" exists

  Scenario: Submit order
    Given the customer has items in cart
    When the customer submits the order
    Then the order should be created

  Scenario: Cancel order
    Given the customer has a pending order
    When the customer cancels the order
    Then the order status should be "CANCELLED"
```

---

## Tags

### Organizing Scenarios

```gherkin
@smoke @critical
Scenario: Customer can login
  Given a registered customer
  When the customer logs in with valid credentials
  Then the customer should be authenticated

@integration @payment
Scenario: Process payment
  Given an order ready for payment
  When payment is processed
  Then payment should be confirmed

@slow @e2e
Scenario: Complete order flow
  Given a new customer
  When the customer completes the entire order process
  Then the order should be delivered
```

### Running Tagged Scenarios

```bash
# Run smoke tests only
./gradlew cucumber -Dcucumber.filter.tags="@smoke"

# Run all except slow tests
./gradlew cucumber -Dcucumber.filter.tags="not @slow"

# Run critical integration tests
./gradlew cucumber -Dcucumber.filter.tags="@critical and @integration"
```

---

## Test Data Management

### Using Data Tables

```gherkin
Scenario: Create order with multiple items
  Given a customer with ID "CUST-001"
  When the customer adds the following items to cart:
    | Product ID | Quantity | Price |
    | PROD-001   | 2        | 10.00 |
    | PROD-002   | 1        | 25.00 |
    | PROD-003   | 3        | 5.00  |
  Then the cart total should be $60.00
```

### Implementation

```java
@When("the customer adds the following items to cart:")
public void theCustomerAddsItems(DataTable dataTable) {
    List<Map<String, String>> rows = dataTable.asMaps();
    
    for (Map<String, String> row : rows) {
        ProductId productId = ProductId.of(row.get("Product ID"));
        int quantity = Integer.parseInt(row.get("Quantity"));
        Money price = Money.of(new BigDecimal(row.get("Price")));
        
        cart.addItem(productId, quantity, price);
    }
}
```

---

## Hooks

### Setup and Teardown

```java
@Before
public void setUp() {
    // Runs before each scenario
    cleanDatabase();
    initializeTestData();
}

@After
public void tearDown() {
    // Runs after each scenario
    cleanupTestData();
}

@Before("@database")
public void setUpDatabase() {
    // Runs only for scenarios tagged with @database
    initializeDatabaseConnection();
}

@After("@database")
public void tearDownDatabase() {
    closeDatabaseConnection();
}
```

---

## Best Practices

### Scenario Independence

```gherkin
# ✅ Good: Each scenario is independent
Scenario: Submit first order
  Given a new customer
  When the customer submits an order
  Then the order should be created

Scenario: Submit second order
  Given a customer with one previous order
  When the customer submits another order
  Then the order should be created

# ❌ Bad: Scenarios depend on each other
Scenario: Create customer
  When a customer is created
  Then the customer should exist

Scenario: Submit order
  # Depends on previous scenario
  When the customer submits an order
  Then the order should be created
```

### Ubiquitous Language

Use domain language from your bounded contexts:

```gherkin
# ✅ Good: Uses domain language
Scenario: Reserve inventory for order
  Given an order with 5 units of product "PROD-001"
  When inventory is reserved
  Then 5 units should be allocated to the order

# ❌ Bad: Uses technical language
Scenario: Update inventory table
  Given a row in inventory_items table
  When UPDATE statement is executed
  Then the quantity column should be decremented
```

---

## Running BDD Tests

### Gradle Commands

```bash
# Run all Cucumber tests
./gradlew cucumber

# Run with specific tags
./gradlew cucumber -Dcucumber.filter.tags="@smoke"

# Generate reports
./gradlew cucumber
open build/reports/cucumber/index.html
```

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run BDD Tests
  run: ./gradlew cucumber -Dcucumber.filter.tags="@smoke or @critical"

- name: Publish Test Results
  uses: actions/upload-artifact@v3
  with:
    name: cucumber-reports
    path: build/reports/cucumber/
```

---

## Reporting

### Cucumber Reports

Cucumber generates several report formats:
- **HTML**: Interactive web report
- **JSON**: Machine-readable format
- **JUnit XML**: CI/CD integration

### Allure Integration

```java
@CucumberOptions(
    plugin = {
        "pretty",
        "html:build/reports/cucumber/index.html",
        "json:build/reports/cucumber/cucumber.json",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
    }
)
public class CucumberTestRunner {
}
```

---

## Troubleshooting

### Common Issues

**Issue**: Step definition not found
- **Solution**: Check package scanning in runner, verify step definition methods

**Issue**: Scenarios are flaky
- **Solution**: Ensure test data independence, check for timing issues

**Issue**: Slow test execution
- **Solution**: Use tags to run subsets, optimize database operations

---

## Related Documentation

- [Testing Strategy](testing-strategy.md)
- [Unit Testing](unit-testing.md)
- [Integration Testing](integration-testing.md)
- [Gherkin Guide](gherkin-guide.md)

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Development Team
