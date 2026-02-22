# BDD/Cucumber Testing Guide

## Overview

Behavior-Driven Development (BDD) with Cucumber enables writing tests in natural language (Gherkin) that describe business behavior. These tests serve as living documentation and bridge the gap between business requirements and technical implementation.

**Purpose**: Test complete business workflows using executable specifications in plain language.

**Key Characteristics**:
- **Business-Readable**: Written in Given-When-Then format
- **Executable**: Automated tests verifying business behavior  
- **Living Documentation**: Tests serve as up-to-date documentation
- **Collaboration**: Enables business and technical team collaboration

---

## Basic Setup

### Cucumber Spring Configuration

Based on your actual code at `app/src/test/java/solid/humank/genaidemo/bdd/CucumberSpringConfiguration.java`:

```java
@CucumberContextConfiguration
@SpringBootTest(
    classes = GenAiDemoApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
    // Spring Boot auto-configuration handles everything
}
```

### Cucumber Properties

```properties
# src/test/resources/cucumber.properties
cucumber.publish.enabled=false
cucumber.plugin=pretty, html:build/reports/cucumber/cucumber.html
cucumber.glue=solid.humank.genaidemo.bdd
cucumber.features=src/test/resources/features
```

---

## Writing Gherkin Scenarios

### Feature File Structure

Based on your actual feature file at `app/src/test/resources/features/customer/membership_system.feature`:

```gherkin
# 會員系統管理 - Membership System Management
Feature: Membership System Management
  As an e-commerce platform
  I want to provide a comprehensive membership system
  So that I can improve customer loyalty and purchase experience

  Background:
    Given the following membership levels exist in the system:
      | Level    | Min Spending | Discount Rate | Loyalty Rate | Birthday Discount |
      | BRONZE   |            0 |            0% |           1% |                5% |
      | SILVER   |        50000 |            3% |           2% |                8% |
      | GOLD     |       150000 |            5% |           3% |               10% |
      | PLATINUM |       300000 |            8% |           5% |               15% |
    And customer "John" exists with SILVER level, total spending 80000, loyalty points 1600

  Scenario: Automatic membership level upgrade
    Given customer "John" current level is "SILVER" with total spending 140000
    When customer "John" completes an order of 15000
    Then customer's total spending should be updated to 155000
    And customer's membership level should be automatically upgraded to "GOLD"
    And system should send level upgrade notification

  Scenario: Member discount calculation
    Given customer "John" is a "SILVER" member
    When customer purchases products totaling 10000
    Then should receive 3% member discount
    And discount amount should be 300
    And final payment amount should be 9700
```

### Best Practices

#### ✅ Good Scenario Writing

```gherkin
# Clear, focused, business-oriented
Scenario: Customer receives birthday discount
  Given customer "Alice" is a "SILVER" member
  And current month is customer's birthday month
  When customer purchases products totaling 5000
  Then should receive 8% birthday discount
  And final payment amount should be 4600

# Uses data tables for clarity
Scenario: Calculate loyalty points for different levels
  Given the following customers:
    | Name | Level  | Purchase Amount |
    | John | BRONZE |            1000 |
    | Jane | SILVER |            1000 |
  When they complete their purchases
  Then they should earn the following loyalty points:
    | Name | Points |
    | John |     10 |
    | Jane |     20 |
```

#### ❌ Bad Scenario Writing

```gherkin
# BAD: Too technical
Scenario: Test database update
  Given a customer record exists in the database
  When I call the updateCustomer() method
  Then the database should be updated

# BAD: Too vague
Scenario: Customer buys something
  Given a customer
  When they buy stuff
  Then something happens
```

---

## Implementing Step Definitions

### Basic Step Definition

```java
package solid.humank.genaidemo.bdd.customer;

import io.cucumber.java.en.*;
import io.cucumber.datatable.DataTable;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

public class MembershipSystemSteps {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private MembershipService membershipService;
    
    // Scenario context - shared state between steps
    private Customer currentCustomer;
    private BigDecimal calculatedDiscount;
    
    @Given("customer {string} is a {string} member")
    public void customerIsAMember(String customerName, String membershipLevel) {
        currentCustomer = customerRepository.findByName(customerName)
            .orElseGet(() -> createCustomer(customerName, membershipLevel));
        
        assertThat(currentCustomer.getMembershipLevel().name())
            .isEqualTo(membershipLevel);
    }
    
    @When("customer purchases products totaling {int}")
    public void customerPurchasesProductsTotaling(int amount) {
        Money purchaseAmount = Money.of(
            new BigDecimal(amount),
            Currency.getInstance("TWD")
        );
        
        calculatedDiscount = membershipService.calculateDiscount(
            currentCustomer,
            purchaseAmount
        );
    }
    
    @Then("should receive {int}% member discount")
    public void shouldReceiveMemberDiscount(int expectedPercentage) {
        BigDecimal expectedRate = new BigDecimal(expectedPercentage)
            .divide(new BigDecimal(100));
        
        // Verify discount rate matches expected
        assertThat(calculatedDiscount).isNotNull();
    }
    
    @And("discount amount should be {int}")
    public void discountAmountShouldBe(int expectedAmount) {
        assertThat(calculatedDiscount)
            .isEqualByComparingTo(new BigDecimal(expectedAmount));
    }
    
    private Customer createCustomer(String name, String membershipLevel) {
        return new Customer(
            CustomerId.generate(),
            new CustomerName(name),
            Email.of(name.toLowerCase() + "@example.com"),
            MembershipLevel.valueOf(membershipLevel)
        );
    }
}
```

### Working with Data Tables

```java
@Given("the following membership levels exist in the system:")
public void theFollowingMembershipLevelsExist(DataTable dataTable) {
    List<Map<String, String>> rows = dataTable.asMaps();
    
    for (Map<String, String> row : rows) {
        MembershipLevel level = MembershipLevel.valueOf(row.get("Level"));
        int minSpending = Integer.parseInt(row.get("Min Spending"));
        int discountRate = parsePercentage(row.get("Discount Rate"));
        
        membershipConfigRepository.save(new MembershipConfig(
            level,
            Money.of(new BigDecimal(minSpending), Currency.getInstance("TWD")),
            new BigDecimal(discountRate).divide(new BigDecimal(100))
        ));
    }
}

private int parsePercentage(String percentage) {
    return Integer.parseInt(percentage.replace("%", "").trim());
}
```

### Scenario Context Management

```java
@Component
public class ScenarioContext {
    private final Map<String, Object> context = new HashMap<>();
    
    public void set(String key, Object value) {
        context.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) context.get(key);
    }
    
    public void clear() {
        context.clear();
    }
}

// Usage in step definitions
@Autowired
private ScenarioContext scenarioContext;

@When("customer completes an order")
public void customerCompletesAnOrder() {
    Order order = createOrder();
    scenarioContext.set("currentOrder", order);
}

@Then("order should be confirmed")
public void orderShouldBeConfirmed() {
    Order order = scenarioContext.get("currentOrder");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
}
```

---

## Hooks and Setup

### Before and After Hooks

```java
public class CucumberHooks {
    
    @Autowired
    private ScenarioContext scenarioContext;
    
    @Before
    public void beforeScenario() {
        scenarioContext.clear();
        System.out.println("Setting up scenario");
    }
    
    @After
    public void afterScenario() {
        cleanupTestData();
        System.out.println("Cleaning up scenario");
    }
    
    // Tagged hooks - only run for scenarios with specific tags
    @Before("@database")
    public void beforeDatabaseScenario() {
        System.out.println("Setting up database");
    }
}
```

---

## Advanced Patterns

### Scenario Outlines (Parameterized Scenarios)

```gherkin
Scenario Outline: Calculate discount for different membership levels
  Given customer "<name>" is a "<level>" member
  When customer purchases products totaling <amount>
  Then should receive <discount_rate>% member discount
  And discount amount should be <discount_amount>

  Examples:
    | name  | level    | amount | discount_rate | discount_amount |
    | John  | BRONZE   |  10000 |             0 |               0 |
    | Jane  | SILVER   |  10000 |             3 |             300 |
    | Bob   | GOLD     |  10000 |             5 |             500 |
    | Alice | PLATINUM |  10000 |             8 |             800 |
```

### Using Tags for Organization

```gherkin
@membership @discount
Feature: Membership Discount System

  @smoke @critical
  Scenario: Silver member receives discount
    Given customer "John" is a "SILVER" member
    When customer purchases products totaling 10000
    Then should receive 3% member discount

  @regression
  Scenario: Gold member receives higher discount
    Given customer "Jane" is a "GOLD" member
    When customer purchases products totaling 10000
    Then should receive 5% member discount
```

### Running Tagged Scenarios

```bash
# Run only smoke tests
./gradlew cucumber -Dcucumber.filter.tags="@smoke"

# Run all tests except work-in-progress
./gradlew cucumber -Dcucumber.filter.tags="not @wip"

# Run membership AND discount tests
./gradlew cucumber -Dcucumber.filter.tags="@membership and @discount"
```

---

## Best Practices

### 1. Keep Scenarios Independent

Each scenario should be able to run independently without relying on other scenarios.

### 2. Use Background for Common Setup

```gherkin
Feature: Order Management

  Background:
    Given the following products exist:
      | ID   | Name      | Price |
      | P001 | Product A |  1000 |
    And customer "John" exists with SILVER membership

  Scenario: Create order
    When customer adds product "P001" to cart
    Then order should be created
```

### 3. Use Meaningful Step Names

Focus on business behavior, not technical implementation.

### 4. Avoid UI-Specific Language

```gherkin
# GOOD: Business behavior
When customer updates their email address
Then customer email should be updated

# BAD: UI implementation
When I click the "Edit" button
And I type in the email field
```

---

## Quick Reference

### Gherkin Keywords

```gherkin
Feature: High-level description
Background: Common setup
Scenario: Specific test case
Scenario Outline: Parameterized test
Examples: Data for Scenario Outline

Given: Precondition
When: Action
Then: Expected outcome
And: Additional step
```

### Step Definition Patterns

```java
// String parameter
@Given("customer {string} exists")
public void customerExists(String name) { }

// Integer parameter
@When("customer purchases {int} items")
public void customerPurchases(int quantity) { }

// Word parameter (no spaces)
@Given("customer has {word} membership")
public void customerHasMembership(String level) { }

// Data table
@Given("the following customers exist:")
public void customersExist(DataTable dataTable) { }
```

---

## Related Documentation

- **Testing Strategy**: #[[file:../../steering/testing-strategy.md]]
- **Unit Testing**: #[[file:unit-testing-guide.md]]
- **Integration Testing**: #[[file:integration-testing-guide.md]]
- **Test Performance**: #[[file:test-performance-guide.md]]

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-22  
**Owner**: Development Team
