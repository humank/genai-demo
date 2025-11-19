---
adr_number: 012
title: "BDD with Cucumber for Requirements Specification"
date: 2025-10-24
status: "accepted"
supersedes: []
superseded_by: null
related_adrs: [006]
affected_viewpoints: ["development"]
affected_perspectives: ["development-resource", "evolution"]
---

# ADR-012: BDD with Cucumber for Requirements Specification

## Status

**Accepted** - 2025-10-24

## Context

### Problem Statement

The Enterprise E-Commerce Platform requires a methodology that:

- Bridges the gap between business requirements and technical implementation
- Provides executable specifications that serve as living documentation
- Enables collaboration between business stakeholders and developers
- Ensures requirements are testable and verifiable
- Supports Test-Driven Development (TDD) workflow
- Maintains alignment between business goals and implementation
- Provides clear acceptance criteria for features
- Enables regression testing of business scenarios

### Business Context

**Business Drivers**:

- Need for clear, unambiguous requirements
- Requirement for business stakeholder involvement in development
- Compliance requirements for documented business rules
- Need for living documentation that stays up-to-date
- Reduction of misunderstandings between business and technical teams
- Support for agile development with clear acceptance criteria

**Constraints**:

- Team has limited BDD experience
- Business stakeholders have limited technical knowledge
- Must integrate with existing testing strategy (ADR-006)
- Timeline: 3 months to establish BDD practice
- Budget: No additional tooling costs

### Technical Context

**Current State**:

- Spring Boot 3.4.5 + Java 21
- JUnit 5 for testing (ADR-006)
- Domain-Driven Design approach (ADR-002)
- Hexagonal Architecture (ADR-002)
- Event-driven architecture (ADR-003)

**Requirements**:

- Business-readable specifications
- Executable specifications
- Integration with CI/CD pipeline
- Support for multiple languages (English, Chinese)
- Reusable step definitions
- Clear test reporting
- Version control for specifications

## Decision Drivers

1. **Business Collaboration**: Enable non-technical stakeholders to understand tests
2. **Living Documentation**: Specifications that stay current with code
3. **Executable Specifications**: Tests that verify business requirements
4. **Ubiquitous Language**: Use domain language in specifications
5. **Test Automation**: Automated execution of business scenarios
6. **Maintainability**: Easy to update as requirements change
7. **Integration**: Works with existing testing framework
8. **Cost**: Free and open source

## Considered Options

### Option 1: Cucumber with Gherkin (BDD Framework)

**Description**: BDD framework using Gherkin syntax for business-readable specifications

**Pros**:

- ✅ Business-readable Gherkin syntax (Given-When-Then)
- ✅ Excellent Java integration (Cucumber-JVM)
- ✅ Supports multiple languages
- ✅ Large community and ecosystem
- ✅ Integration with Spring Boot
- ✅ Reusable step definitions
- ✅ Clear test reports
- ✅ IDE support (IntelliJ, VS Code)
- ✅ CI/CD integration
- ✅ Living documentation generation

**Cons**:

- ⚠️ Learning curve for Gherkin syntax
- ⚠️ Can be verbose for simple scenarios
- ⚠️ Requires discipline to maintain

**Cost**: $0 (open source)

**Risk**: **Low** - Mature, widely adopted

### Option 2: JBehave

**Description**: Java BDD framework similar to Cucumber

**Pros**:

- ✅ Java-native BDD framework
- ✅ Similar to Cucumber
- ✅ Good Spring integration

**Cons**:

- ❌ Smaller community than Cucumber
- ❌ Less active development
- ❌ Fewer IDE plugins
- ❌ Less documentation

**Cost**: $0

**Risk**: **Medium** - Smaller ecosystem

### Option 3: Spock Framework

**Description**: Groovy-based testing framework with BDD-style syntax

**Pros**:

- ✅ Expressive syntax
- ✅ Good for unit tests
- ✅ Data-driven testing

**Cons**:

- ❌ Requires Groovy knowledge
- ❌ Not business-readable
- ❌ Less suitable for collaboration with non-technical stakeholders
- ❌ Primarily for developers

**Cost**: $0

**Risk**: **Medium** - Not true BDD

### Option 4: Plain JUnit with Descriptive Names

**Description**: Use JUnit with very descriptive test method names

**Pros**:

- ✅ No additional framework
- ✅ Team already knows JUnit
- ✅ Simple

**Cons**:

- ❌ Not business-readable
- ❌ No living documentation
- ❌ No collaboration with business stakeholders
- ❌ Doesn't bridge business-technical gap

**Cost**: $0

**Risk**: **High** - Misses BDD benefits

## Decision Outcome

**Chosen Option**: **Cucumber with Gherkin (BDD Framework)**

### Rationale

Cucumber with Gherkin was selected for the following reasons:

1. **Business Collaboration**: Gherkin syntax is readable by non-technical stakeholders
2. **Ubiquitous Language**: Scenarios use domain language from DDD
3. **Living Documentation**: Feature files serve as up-to-date documentation
4. **Executable Specifications**: Scenarios are automated tests
5. **Mature Ecosystem**: Large community, excellent tooling, extensive documentation
6. **Spring Boot Integration**: Seamless integration with existing stack
7. **Reusability**: Step definitions can be reused across scenarios
8. **Reporting**: Clear, business-friendly test reports
9. **IDE Support**: Excellent plugins for IntelliJ IDEA and VS Code

**Implementation Strategy**:

**Gherkin Structure**:

```gherkin
Feature: Order Submission
  As a customer
  I want to submit an order
  So that I can purchase products

  Background:
    Given a customer with ID "CUST-001"
    And the customer has a valid payment method

  Scenario: Submit order successfully
    Given the customer has items in shopping cart:
      | Product ID | Quantity | Price |
      | PROD-001   | 2        | 10.00 |
      | PROD-002   | 1        | 20.00 |
    When the customer submits the order
    Then the order status should be "PENDING"
    And an order confirmation email should be sent
    And inventory should be reserved for:
      | Product ID | Quantity |
      | PROD-001   | 2        |
      | PROD-002   | 1        |

  Scenario: Submit order with insufficient inventory
    Given the customer has items in shopping cart:
      | Product ID | Quantity | Price |
      | PROD-001   | 100      | 10.00 |
    And product "PROD-001" has only 50 units in stock
    When the customer submits the order
    Then the order should be rejected
    And the customer should see error "Insufficient inventory"
```

**Step Definitions**:

```java
@SpringBootTest
@CucumberContextConfiguration
public class OrderStepDefinitions {
    
    @Autowired
    private OrderApplicationService orderService;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    private Customer customer;
    private Order order;
    private Exception thrownException;
    
    @Given("a customer with ID {string}")
    public void aCustomerWithId(String customerId) {
        customer = customerRepository.findById(CustomerId.of(customerId))
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
    
    @Given("the customer has items in shopping cart:")
    public void theCustomerHasItemsInShoppingCart(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            String productId = row.get("Product ID");
            int quantity = Integer.parseInt(row.get("Quantity"));
            BigDecimal price = new BigDecimal(row.get("Price"));
            
            customer.addToCart(
                ProductId.of(productId),
                quantity,
                Money.of(price)
            );
        }
    }
    
    @When("the customer submits the order")
    public void theCustomerSubmitsTheOrder() {
        try {
            SubmitOrderCommand command = new SubmitOrderCommand(
                customer.getId(),
                customer.getCartItems()
            );
            order = orderService.submitOrder(command);
        } catch (Exception e) {
            thrownException = e;
        }
    }
    
    @Then("the order status should be {string}")
    public void theOrderStatusShouldBe(String expectedStatus) {
        assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
    }
    
    @Then("an order confirmation email should be sent")
    public void anOrderConfirmationEmailShouldBeSent() {
        // Verify email was sent (mock verification or event check)
        verify(emailService).sendOrderConfirmation(
            eq(customer.getEmail()),
            eq(order.getId())
        );
    }
    
    @Then("inventory should be reserved for:")
    public void inventoryShouldBeReservedFor(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        
        for (Map<String, String> row : rows) {
            String productId = row.get("Product ID");
            int quantity = Integer.parseInt(row.get("Quantity"));
            
            verify(inventoryService).reserveInventory(
                eq(ProductId.of(productId)),
                eq(quantity)
            );
        }
    }
    
    @Then("the order should be rejected")
    public void theOrderShouldBeRejected() {
        assertThat(thrownException).isNotNull();
        assertThat(order).isNull();
    }
    
    @Then("the customer should see error {string}")
    public void theCustomerShouldSeeError(String expectedError) {
        assertThat(thrownException.getMessage()).contains(expectedError);
    }
}
```

**Why Not JBehave**: Smaller community and less active development compared to Cucumber.

**Why Not Spock**: Not business-readable, primarily for developers, doesn't enable business collaboration.

**Why Not Plain JUnit**: Misses the key benefit of BDD - collaboration with business stakeholders through executable specifications.

## Impact Analysis

### Stakeholder Impact

| Stakeholder | Impact Level | Description | Mitigation |
|-------------|--------------|-------------|------------|
| Business Stakeholders | High | Can read and validate requirements | Training on Gherkin, workshops |
| Product Owners | High | Write acceptance criteria in Gherkin | Training, templates, examples |
| Developers | High | Write step definitions and scenarios | Training, pair programming |
| QA Team | High | Use scenarios for testing | Training, test automation guides |
| Architects | Medium | Ensure scenarios align with architecture | Review process, guidelines |

### Impact Radius

**Selected Impact Radius**: **System**

Affects:

- Requirements gathering process
- Development workflow (TDD/BDD)
- Testing strategy
- Documentation approach
- Stakeholder collaboration
- CI/CD pipeline

### Risk Assessment

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| Gherkin learning curve | High | Medium | Training, templates, examples, workshops |
| Scenario maintenance overhead | Medium | Medium | Regular reviews, refactoring, reusable steps |
| Business stakeholder engagement | Medium | High | Demonstrate value, involve early, regular collaboration |
| Over-specification | Medium | Medium | Focus on business value, avoid technical details |
| Step definition duplication | Medium | Low | Reusable step library, code reviews |

**Overall Risk Level**: **Low**

## Implementation Plan

### Phase 1: Setup and Training (Week 1-2)

- [ ] Add Cucumber dependencies

  ```xml
  <dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-java</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-spring</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>io.cucumber</groupId>
    <artifactId>cucumber-junit-platform-engine</artifactId>
    <version>7.14.0</version>
    <scope>test</scope>
  </dependency>
  ```

- [ ] Configure Cucumber

  ```java
  @Suite
  @IncludeEngines("cucumber")
  @SelectClasspathResource("features")
  @ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
                          value = "pretty, html:target/cucumber-reports/cucumber.html")
  @ConfigurationParameter(key = GLUE_PROPERTY_NAME, 
                          value = "solid.humank.genaidemo.bdd")
  public class CucumberTestRunner {
  }
  ```

- [ ] Create project structure

  ```
  src/test/
  ├── java/
  │   └── solid/humank/genaidemo/bdd/
  │       ├── CucumberTestRunner.java
  │       ├── CucumberSpringConfiguration.java
  │       ├── steps/
  │       │   ├── CustomerSteps.java
  │       │   ├── OrderSteps.java
  │       │   └── CommonSteps.java
  │       └── support/
  │           ├── TestDataBuilder.java
  │           └── ScenarioContext.java
  └── resources/
      └── features/
          ├── customer/
          │   ├── customer-registration.feature
          │   └── customer-profile.feature
          ├── order/
          │   ├── order-submission.feature
          │   └── order-cancellation.feature
          └── product/
              └── product-search.feature
  ```

- [ ] Conduct team training
  - Gherkin syntax workshop
  - Writing effective scenarios
  - Step definition best practices
  - BDD workflow demonstration

### Phase 2: Core Step Definitions (Week 2-3)

- [ ] Create base configuration

  ```java
  @SpringBootTest
  @CucumberContextConfiguration
  @ActiveProfiles("test")
  public class CucumberSpringConfiguration {
      
      @Autowired
      private ApplicationContext applicationContext;
      
      @BeforeAll
      public static void setup() {
          // Global test setup
      }
      
      @AfterAll
      public static void teardown() {
          // Global test cleanup
      }
  }
  ```

- [ ] Create scenario context

  ```java
  @Component
  @Scope("cucumber-glue")
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
  ```

- [ ] Create common step definitions

  ```java
  public class CommonSteps {
      
      @Autowired
      private ScenarioContext scenarioContext;
      
      @Before
      public void beforeScenario() {
          scenarioContext.clear();
      }
      
      @After
      public void afterScenario() {
          // Cleanup after each scenario
      }
      
      @Given("the system is ready")
      public void theSystemIsReady() {
          // Verify system is in ready state
      }
      
      @Given("the current date is {string}")
      public void theCurrentDateIs(String date) {
          LocalDate testDate = LocalDate.parse(date);
          scenarioContext.set("currentDate", testDate);
      }
  }
  ```

### Phase 3: Feature Implementation (Week 3-6)

- [ ] Write Customer features

  ```gherkin
  # customer-registration.feature
  Feature: Customer Registration
    As a new user
    I want to register an account
    So that I can make purchases
    
    Scenario: Successful registration with valid information
      Given I am on the registration page
      When I fill in the registration form with:
        | Field    | Value              |
        | Name     | John Doe           |
        | Email    | john@example.com   |
        | Password | SecurePass123!     |
      And I submit the registration form
      Then I should see a success message "Registration successful"
      And a welcome email should be sent to "john@example.com"
      And my account should be created with status "ACTIVE"
    
    Scenario: Registration fails with duplicate email
      Given a customer exists with email "existing@example.com"
      When I try to register with email "existing@example.com"
      Then I should see an error "Email already registered"
      And no account should be created
    
    Scenario Outline: Registration fails with invalid data
      When I try to register with <field> as "<value>"
      Then I should see an error "<error_message>"
      
      Examples:
        | field    | value           | error_message                |
        | email    | invalid-email   | Invalid email format         |
        | password | short           | Password too short           |
        | name     |                 | Name is required             |
  ```

- [ ] Write Order features

  ```gherkin
  # order-submission.feature
  Feature: Order Submission
    As a customer
    I want to submit orders
    So that I can purchase products
    
    Background:
      Given a customer "John Doe" with ID "CUST-001"
      And the customer has a valid payment method
      And the following products are available:
        | Product ID | Name        | Price | Stock |
        | PROD-001   | Laptop      | 999   | 10    |
        | PROD-002   | Mouse       | 29    | 50    |
        | PROD-003   | Keyboard    | 79    | 30    |
    
    Scenario: Submit order with single item
      Given the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 1        |
      When the customer submits the order
      Then the order should be created with status "PENDING"
      And the order total should be 999.00
      And inventory should be reserved:
        | Product ID | Quantity |
        | PROD-001   | 1        |
      And an order confirmation should be sent
    
    Scenario: Submit order with multiple items
      Given the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 1        |
        | PROD-002   | 2        |
        | PROD-003   | 1        |
      When the customer submits the order
      Then the order should be created with status "PENDING"
      And the order total should be 1137.00
      And inventory should be reserved for all items
    
    Scenario: Order submission fails with insufficient inventory
      Given product "PROD-001" has only 5 units in stock
      And the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 10       |
      When the customer submits the order
      Then the order should be rejected
      And the error should be "Insufficient inventory for PROD-001"
      And no inventory should be reserved
    
    Scenario: Order submission fails with invalid payment
      Given the customer has an expired payment method
      And the customer adds to cart:
        | Product ID | Quantity |
        | PROD-001   | 1        |
      When the customer submits the order
      Then the order should be rejected
      And the error should be "Payment method invalid"
  ```

- [ ] Implement step definitions for all features

### Phase 4: Business Stakeholder Collaboration (Week 6-8)

- [ ] Conduct BDD workshops with business stakeholders
  - Explain Gherkin syntax
  - Demonstrate scenario writing
  - Practice writing scenarios together
  - Review existing scenarios

- [ ] Establish scenario review process
  - Business stakeholders review scenarios
  - Developers implement step definitions
  - QA validates scenarios
  - Regular refinement sessions

- [ ] Create scenario templates

  ```gherkin
  # Template for CRUD operations
  Feature: [Entity] Management
    As a [role]
    I want to [action]
    So that [benefit]
    
    Scenario: Create [entity] successfully
      Given [preconditions]
      When I create a [entity] with [data]
      Then the [entity] should be created
      And [expected outcomes]
    
    Scenario: Update [entity] successfully
      Given a [entity] exists with [identifier]
      When I update the [entity] with [data]
      Then the [entity] should be updated
      And [expected outcomes]
  ```

### Phase 5: CI/CD Integration (Week 8-9)

- [ ] Configure Gradle task

  ```gradle
  tasks.register('cucumber', JavaExec) {
      dependsOn assemble, testClasses
      mainClass = 'io.cucumber.core.cli.Main'
      classpath = configurations.cucumberRuntime + sourceSets.main.output + sourceSets.test.output
      args = [
          '--plugin', 'pretty',
          '--plugin', 'html:build/reports/cucumber/cucumber.html',
          '--plugin', 'json:build/reports/cucumber/cucumber.json',
          '--plugin', 'io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm',
          '--glue', 'solid.humank.genaidemo.bdd',
          'src/test/resources/features'
      ]
  }
  ```

- [ ] Add to CI/CD pipeline

  ```yaml
  # .github/workflows/test.yml

  - name: Run BDD Tests

    run: ./gradlew cucumber
    
  - name: Generate Cucumber Report

    if: always()
    uses: actions/upload-artifact@v3
    with:
      name: cucumber-report
      path: build/reports/cucumber/
  ```

- [ ] Set up Allure reporting

  ```xml
  <dependency>
    <groupId>io.qameta.allure</groupId>
    <artifactId>allure-cucumber7-jvm</artifactId>
    <version>2.24.0</version>
  </dependency>
  ```

### Phase 6: Documentation and Best Practices (Week 9-10)

- [ ] Create BDD guidelines document
  - Scenario writing best practices
  - Step definition patterns
  - Naming conventions
  - Common pitfalls to avoid

- [ ] Create living documentation

  ```bash
  # Generate living documentation from feature files
  ./gradlew cucumber
  # Open build/reports/cucumber/cucumber.html
  ```

- [ ] Establish maintenance process
  - Regular scenario reviews
  - Refactoring step definitions
  - Updating scenarios with requirements changes
  - Archiving obsolete scenarios

### Rollback Strategy

**Trigger Conditions**:

- Business stakeholders not engaged after 3 months
- Scenario maintenance overhead > 30% of development time
- Team unable to adopt BDD practices
- Scenarios become out of sync with implementation

**Rollback Steps**:

1. Archive feature files for reference
2. Convert critical scenarios to JUnit tests
3. Simplify to plain JUnit with descriptive names
4. Document lessons learned
5. Re-evaluate BDD approach

**Rollback Time**: 2 weeks

## Monitoring and Success Criteria

### Success Metrics

- ✅ 100% of user stories have Gherkin scenarios
- ✅ Business stakeholder participation in scenario reviews > 80%
- ✅ Scenario pass rate > 95%
- ✅ Scenario execution time < 10 minutes
- ✅ Living documentation generated automatically
- ✅ Developer satisfaction with BDD > 4/5
- ✅ Business stakeholder satisfaction > 4/5

### Monitoring Plan

**BDD Metrics**:

- Number of scenarios per feature
- Scenario pass/fail rates
- Scenario execution time
- Step definition reuse rate
- Business stakeholder engagement

**Quality Metrics**:

- Requirements coverage by scenarios
- Defects found by BDD tests
- Time to write scenarios vs implementation
- Scenario maintenance effort

**Review Schedule**:

- Weekly: Scenario review sessions
- Monthly: BDD practice retrospective
- Quarterly: Business stakeholder feedback

## Consequences

### Positive Consequences

- ✅ **Business Collaboration**: Non-technical stakeholders can read and validate requirements
- ✅ **Living Documentation**: Scenarios serve as up-to-date documentation
- ✅ **Executable Specifications**: Requirements are automatically tested
- ✅ **Ubiquitous Language**: Scenarios use domain language
- ✅ **Clear Acceptance Criteria**: Each scenario defines "done"
- ✅ **Regression Testing**: Scenarios prevent regression
- ✅ **Requirements Traceability**: Clear link from requirement to test
- ✅ **Reduced Misunderstandings**: Shared understanding of requirements

### Negative Consequences

- ⚠️ **Learning Curve**: Team needs to learn Gherkin and BDD practices
- ⚠️ **Initial Overhead**: Writing scenarios takes time initially
- ⚠️ **Maintenance**: Scenarios need to be kept up-to-date
- ⚠️ **Engagement Required**: Requires active business stakeholder participation
- ⚠️ **Can Be Verbose**: Some scenarios can become lengthy

### Technical Debt

**Identified Debt**:

1. Not all features have BDD scenarios yet (gradual adoption)
2. Some step definitions have duplication (needs refactoring)
3. Limited Chinese language scenarios (future requirement)
4. No automated scenario generation from requirements (future enhancement)

**Debt Repayment Plan**:

- **Q1 2026**: Achieve 100% scenario coverage for all features
- **Q2 2026**: Refactor step definitions to eliminate duplication
- **Q3 2026**: Add Chinese language scenario support
- **Q4 2026**: Explore automated scenario generation tools

## Related Decisions

- [ADR-006: Environment-Specific Testing Strategy](006-environment-specific-testing-strategy.md) - BDD as part of testing strategy

## Notes

### Gherkin Best Practices

**DO**:

- Use business language, not technical jargon
- Keep scenarios focused on business value
- Use Background for common setup
- Use Scenario Outline for similar scenarios with different data
- Make scenarios independent and isolated
- Use descriptive scenario names

**DON'T**:

- Include technical implementation details
- Make scenarios dependent on each other
- Use UI-specific language (unless testing UI)
- Write overly long scenarios (> 10 steps)
- Duplicate step definitions

### Step Definition Patterns

**Parameter Types**:

```java
@ParameterType("CUST-\\d+")
public CustomerId customerId(String id) {
    return CustomerId.of(id);
}

@ParameterType("\\d+\\.\\d{2}")
public Money money(String amount) {
    return Money.of(new BigDecimal(amount));
}
```

**Data Tables**:

```java
@Given("the following products exist:")
public void theFollowingProductsExist(List<Product> products) {
    products.forEach(productRepository::save);
}

// With custom transformer
@DataTableType
public Product productEntry(Map<String, String> entry) {
    return new Product(
        ProductId.of(entry.get("Product ID")),
        entry.get("Name"),
        Money.of(new BigDecimal(entry.get("Price")))
    );
}
```

### Scenario Organization

```text
features/
├── customer/
│   ├── registration.feature
│   ├── authentication.feature
│   └── profile-management.feature
├── order/
│   ├── order-submission.feature
│   ├── order-cancellation.feature
│   └── order-tracking.feature
├── product/
│   ├── product-search.feature
│   ├── product-details.feature
│   └── product-reviews.feature
└── payment/
    ├── payment-processing.feature
    └── refund-processing.feature
```

### Example Report Output

```text
Feature: Order Submission
  ✓ Submit order with single item (1.2s)
  ✓ Submit order with multiple items (1.5s)
  ✓ Order submission fails with insufficient inventory (0.8s)
  ✓ Order submission fails with invalid payment (0.9s)

4 scenarios (4 passed)
16 steps (16 passed)
0m4.4s
```

---

**Document Status**: ✅ Accepted  
**Last Reviewed**: 2025-10-24  
**Next Review**: 2026-01-24 (Quarterly)
