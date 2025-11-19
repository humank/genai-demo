# Gherkin Writing Guide

> **Status**: ✅ Active  
> **Last Updated**: 2024-11-19

## Overview

This guide provides best practices for writing Gherkin scenarios in the GenAI Demo project.

---

## Quick Reference

For complete BDD testing guidance, see:
- [BDD Testing](bdd-testing.md) - Complete BDD guide
- [Testing Strategy](testing-strategy.md) - Overall testing approach

---

## Gherkin Basics

### Structure

```gherkin
Feature: [Feature Name]
  [Feature Description]
  
  Scenario: [Scenario Name]
    Given [precondition]
    When [action]
    Then [expected outcome]
```

### Keywords

- **Feature**: High-level business functionality
- **Scenario**: Specific example of feature behavior
- **Given**: Set up initial state
- **When**: Trigger action or event
- **Then**: Verify expected outcome
- **And/But**: Additional steps

---

## Writing Effective Scenarios

### Use Business Language

```gherkin
# ✅ Good: Business-focused
Scenario: Premium customer receives discount
  Given a premium customer
  When they place an order over $100
  Then they receive a 10% discount

# ❌ Bad: Technical implementation
Scenario: Discount calculation
  Given CustomerType = "PREMIUM"
  When OrderTotal > 100.00
  Then DiscountPercentage = 0.10
```

### Be Specific and Concrete

```gherkin
# ✅ Good: Specific values
Scenario: Calculate shipping for heavy order
  Given an order weighing 15 kg
  When calculating shipping cost
  Then the shipping cost should be $25.00

# ❌ Bad: Vague
Scenario: Calculate shipping
  Given a heavy order
  When calculating shipping
  Then shipping cost is high
```

### One Scenario, One Behavior

```gherkin
# ✅ Good: Single behavior
Scenario: Reject order with insufficient inventory
  Given a product with 5 units in stock
  When a customer orders 10 units
  Then the order should be rejected
  And an "insufficient inventory" error should be shown

# ❌ Bad: Multiple behaviors
Scenario: Order processing
  Given a customer and products
  When order is submitted
  Then inventory is checked
  And payment is processed
  And email is sent
  And shipping is scheduled
```

---

## Scenario Patterns

### Happy Path

```gherkin
Scenario: Successfully submit order
  Given a customer with items in cart
  And sufficient inventory is available
  When the customer submits the order
  Then the order should be created
  And the order status should be "PENDING"
```

### Error Handling

```gherkin
Scenario: Reject order with invalid payment
  Given a customer with items in cart
  When the customer submits order with invalid payment details
  Then the order should be rejected
  And an error message "Invalid payment information" should be displayed
```

### Edge Cases

```gherkin
Scenario: Handle order with exactly zero inventory
  Given a product with 0 units in stock
  When a customer attempts to order 1 unit
  Then the order should be rejected
  And the product should be marked as "out of stock"
```

---

## Data Tables

### Simple Tables

```gherkin
Scenario: Add multiple items to cart
  Given a customer
  When the customer adds the following items:
    | Product | Quantity |
    | Laptop  | 1        |
    | Mouse   | 2        |
    | Keyboard| 1        |
  Then the cart should contain 3 items
```

### Complex Tables

```gherkin
Scenario: Calculate order total with discounts
  Given the following products in cart:
    | Product ID | Name    | Price | Quantity | Discount |
    | PROD-001   | Laptop  | 1000  | 1        | 10%      |
    | PROD-002   | Mouse   | 50    | 2        | 0%       |
  Then the order total should be $1000
```

---

## Scenario Outlines

### Basic Outline

```gherkin
Scenario Outline: Validate email format
  Given a registration form
  When the user enters email "<email>"
  Then the email validation should be "<result>"

  Examples:
    | email              | result  |
    | user@example.com   | valid   |
    | invalid.email      | invalid |
    | @example.com       | invalid |
    | user@              | invalid |
```

### Multiple Examples

```gherkin
Scenario Outline: Calculate shipping by weight and distance
  Given an order weighing <weight> kg
  And shipping to <distance> km away
  When calculating shipping cost
  Then the cost should be $<cost>

  Examples: Light packages
    | weight | distance | cost  |
    | 1      | 10       | 5.00  |
    | 2      | 10       | 6.00  |

  Examples: Heavy packages
    | weight | distance | cost  |
    | 10     | 10       | 15.00 |
    | 20     | 10       | 25.00 |
```

---

## Background

### Shared Setup

```gherkin
Feature: Order Management

  Background:
    Given the system is initialized
    And a customer "John Doe" with email "john@example.com" exists
    And the following products are available:
      | Product ID | Name   | Price | Stock |
      | PROD-001   | Laptop | 1000  | 10    |
      | PROD-002   | Mouse  | 50    | 50    |

  Scenario: Submit order
    When the customer orders 1 "Laptop"
    Then the order should be created

  Scenario: Check inventory
    When checking inventory for "Laptop"
    Then 10 units should be available
```

---

## Tags

### Organizing Scenarios

```gherkin
@smoke @critical
Scenario: User can login
  Given a registered user
  When the user logs in
  Then the user should be authenticated

@integration @slow
Scenario: Complete checkout process
  Given a user with items in cart
  When the user completes checkout
  Then the order should be confirmed

@wip
Scenario: New feature in development
  Given a new feature
  When testing
  Then it should work
```

### Tag Categories

- **@smoke**: Critical functionality tests
- **@integration**: Integration tests
- **@e2e**: End-to-end tests
- **@slow**: Long-running tests
- **@wip**: Work in progress
- **@bug**: Bug reproduction tests
- **@manual**: Manual testing required

---

## Best Practices

### Do's ✅

1. **Use declarative style**
   ```gherkin
   # ✅ Good
   Given a premium customer
   
   # ❌ Bad
   Given I navigate to the customer page
   And I select "Premium" from the dropdown
   And I click "Save"
   ```

2. **Focus on business value**
   ```gherkin
   # ✅ Good
   Then the customer receives a confirmation email
   
   # ❌ Bad
   Then the EmailService.send() method is called
   ```

3. **Keep scenarios independent**
   ```gherkin
   # ✅ Good: Each scenario sets up its own data
   Scenario: First order
     Given a new customer
     When they place an order
     Then the order is created
   
   Scenario: Second order
     Given a customer with one previous order
     When they place another order
     Then the order is created
   ```

### Don'ts ❌

1. **Don't include implementation details**
   ```gherkin
   # ❌ Bad
   Given the database has a row in customers table
   When I execute SELECT * FROM orders
   Then the result set should contain 1 row
   ```

2. **Don't make scenarios too long**
   ```gherkin
   # ❌ Bad: Too many steps
   Given step 1
   And step 2
   And step 3
   And step 4
   And step 5
   When action
   Then result 1
   And result 2
   And result 3
   And result 4
   ```

3. **Don't use technical jargon**
   ```gherkin
   # ❌ Bad
   Given the REST API endpoint /api/v1/orders
   When I POST a JSON payload
   Then I receive a 201 HTTP status code
   ```

---

## Common Patterns

### CRUD Operations

```gherkin
Scenario: Create a customer
  When a new customer registers with email "john@example.com"
  Then the customer should be created
  And the customer should receive a welcome email

Scenario: Read customer details
  Given a customer with ID "CUST-001"
  When retrieving customer details
  Then the customer name should be "John Doe"

Scenario: Update customer information
  Given a customer with ID "CUST-001"
  When updating the customer's email to "newemail@example.com"
  Then the customer's email should be updated

Scenario: Delete a customer
  Given a customer with ID "CUST-001"
  When deleting the customer
  Then the customer should no longer exist
```

### Validation

```gherkin
Scenario: Validate required fields
  When submitting an order without customer information
  Then the order should be rejected
  And an error "Customer information required" should be shown

Scenario: Validate data format
  When entering an invalid email format
  Then a validation error should be displayed
  And the form should not be submitted
```

### State Transitions

```gherkin
Scenario: Order state transition
  Given an order in "PENDING" status
  When payment is confirmed
  Then the order status should change to "CONFIRMED"
  And an order confirmation should be sent
```

---

## Anti-Patterns

### Avoid UI-Centric Scenarios

```gherkin
# ❌ Bad: Too UI-focused
Scenario: Login
  Given I am on the login page
  When I enter "user@example.com" in the email field
  And I enter "password123" in the password field
  And I click the "Login" button
  Then I should see the dashboard

# ✅ Good: Business-focused
Scenario: User authentication
  Given a registered user with email "user@example.com"
  When the user logs in with valid credentials
  Then the user should be authenticated
  And the user should access their dashboard
```

### Avoid Conjunctive Steps

```gherkin
# ❌ Bad: Multiple actions in one step
When the user logs in and navigates to orders and filters by date

# ✅ Good: Separate steps
When the user logs in
And navigates to orders
And filters by date range
```

---


**Document Version**: 1.0  
**Last Updated**: 2024-11-19  
**Owner**: Development Team
