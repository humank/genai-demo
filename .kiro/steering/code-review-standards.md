---
inclusion: always
---

# Code Review Standards and Guidelines

## Code Review Process

### Review Workflow

#### Pull Request Requirements

- [ ] **Title**: Clear, descriptive title following format: `[TYPE] Brief description`
  - Types: `FEAT`, `FIX`, `REFACTOR`, `DOCS`, `TEST`, `CHORE`
- [ ] **Description**: Detailed explanation of changes, why they were made
- [ ] **Linked Issues**: Reference to related issues or user stories
- [ ] **Testing**: Evidence of testing (unit tests, manual testing results)
- [ ] **Breaking Changes**: Clearly documented if any
- [ ] **Screenshots**: For UI changes, include before/after screenshots

#### Review Assignment Rules

- **Minimum Reviewers**: 2 reviewers required
- **Required Reviewers**:
  - At least 1 senior developer
  - Domain expert for the affected area
  - Security reviewer for security-related changes
- **Review Timeline**: Reviews must be completed within 24 hours
- **Self-Review**: Author must review their own PR first

### Review Checklist

#### Functional Requirements

- [ ] **Business Logic**: Code correctly implements the requirements
- [ ] **Edge Cases**: Proper handling of edge cases and error conditions
- [ ] **Input Validation**: All inputs are properly validated
- [ ] **Output Correctness**: Outputs match expected format and content
- [ ] **Integration**: Proper integration with existing systems

#### Code Quality

- [ ] **Readability**: Code is clear and self-documenting
- [ ] **Maintainability**: Code is easy to modify and extend
- [ ] **Complexity**: Methods and classes are not overly complex
- [ ] **Naming**: Variables, methods, and classes have meaningful names
- [ ] **Comments**: Complex logic is properly commented

#### Architecture and Design

- [ ] **Design Patterns**: Appropriate design patterns are used
- [ ] **SOLID Principles**: Code follows SOLID principles
- [ ] **DDD Compliance**: Domain-driven design principles are followed
- [ ] **Layer Separation**: Proper separation of concerns across layers
- [ ] **Dependencies**: Dependencies are properly managed and injected

## Code Quality Standards

### Naming Conventions

#### Java Naming Standards

```java
// ✅ Good: Clear, descriptive names
public class CustomerRegistrationService {
    
    private final CustomerRepository customerRepository;
    private final EmailNotificationService emailNotificationService;
    
    public Customer registerNewCustomer(CustomerRegistrationRequest request) {
        validateRegistrationRequest(request);
        
        Customer customer = createCustomerFromRequest(request);
        Customer savedCustomer = customerRepository.save(customer);
        
        sendWelcomeEmail(savedCustomer);
        
        return savedCustomer;
    }
    
    private void validateRegistrationRequest(CustomerRegistrationRequest request) {
        if (isEmailAlreadyRegistered(request.getEmail())) {
            throw new EmailAlreadyRegisteredException(request.getEmail());
        }
    }
}

// ❌ Bad: Unclear, abbreviated names
public class CustRegSvc {
    private final CustRepo repo;
    private final EmailSvc emailSvc;
    
    public Cust regCust(CustRegReq req) {
        // Unclear what this method does
        validate(req);
        Cust c = create(req);
        Cust saved = repo.save(c);
        sendEmail(saved);
        return saved;
    }
}
```

#### Method Naming Guidelines

```java
// ✅ Good: Verb-noun pattern, clear intent
public boolean isCustomerEligibleForDiscount(Customer customer)
public void sendOrderConfirmationEmail(Order order)
public List<Product> findProductsByCategory(Category category)
public void validatePaymentInformation(PaymentInfo paymentInfo)

// ❌ Bad: Unclear intent, poor grammar
public boolean customerDiscount(Customer customer)
public void email(Order order)
public List<Product> products(Category category)
public void payment(PaymentInfo paymentInfo)
```

### Code Structure Standards

#### Method Length and Complexity

```java
// ✅ Good: Short, focused method
public void processOrder(Order order) {
    validateOrder(order);
    calculateOrderTotal(order);
    applyDiscounts(order);
    reserveInventory(order);
    processPayment(order);
    sendConfirmation(order);
}

private void validateOrder(Order order) {
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    if (order.getItems().isEmpty()) {
        throw new BusinessRuleViolationException("Order must contain at least one item");
    }
}

// ❌ Bad: Long, complex method doing too much
public void processOrder(Order order) {
    // 50+ lines of mixed validation, calculation, and processing logic
    if (order != null && !order.getItems().isEmpty()) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item.getQuantity() > 0 && item.getProduct() != null) {
                BigDecimal itemTotal = item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity()));
                if (item.getProduct().getCategory().equals("PREMIUM")) {
                    // Complex discount calculation logic...
                }
                total = total.add(itemTotal);
            }
        }
        // More complex logic continues...
    }
}
```

#### Class Design Standards

```java
// ✅ Good: Single responsibility, clear purpose
@Service
@Transactional
public class OrderProcessingService {
    
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    
    public Order processOrder(ProcessOrderCommand command) {
        Order order = createOrderFromCommand(command);
        
        reserveInventory(order);
        processPayment(order);
        saveOrder(order);
        sendNotifications(order);
        
        return order;
    }
    
    // Additional focused methods...
}

// ❌ Bad: Multiple responsibilities, unclear purpose
@Service
public class OrderService {
    // Handles orders, customers, products, payments, notifications, reports...
    // 500+ lines of mixed responsibilities
}
```

### Error Handling Standards

#### Exception Handling Best Practices

```java
// ✅ Good: Specific exceptions, proper error context
@Service
public class CustomerService {
    
    public Customer findCustomerById(String customerId) {
        try {
            return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                    "Customer not found with ID: " + customerId));
        } catch (DataAccessException e) {
            logger.error("Database error while fetching customer: {}", customerId, e);
            throw new CustomerServiceException("Unable to retrieve customer data", e);
        }
    }
    
    public Customer updateCustomer(String customerId, UpdateCustomerRequest request) {
        Customer customer = findCustomerById(customerId);
        
        try {
            validateUpdateRequest(request);
            updateCustomerFields(customer, request);
            return customerRepository.save(customer);
        } catch (ValidationException e) {
            logger.warn("Invalid update request for customer {}: {}", customerId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating customer {}", customerId, e);
            throw new CustomerServiceException("Failed to update customer", e);
        }
    }
}

// ❌ Bad: Generic exceptions, poor error handling
@Service
public class CustomerService {
    
    public Customer findCustomerById(String customerId) {
        try {
            return customerRepository.findById(customerId).get(); // Can throw NoSuchElementException
        } catch (Exception e) {
            throw new RuntimeException("Error"); // Too generic
        }
    }
}
```

## Security Review Standards

### Security Checklist

- [ ] **Input Validation**: All user inputs are validated and sanitized
- [ ] **SQL Injection**: Parameterized queries are used
- [ ] **XSS Prevention**: Output is properly encoded
- [ ] **Authentication**: Proper authentication mechanisms in place
- [ ] **Authorization**: Access control is correctly implemented
- [ ] **Sensitive Data**: No sensitive data in logs or error messages
- [ ] **Encryption**: Sensitive data is encrypted at rest and in transit

#### Security Code Examples

```java
// ✅ Good: Proper input validation and parameterized queries
@RestController
public class CustomerController {
    
    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
        // Input validation is handled by @Valid annotation
        String sanitizedName = htmlSanitizer.sanitize(request.getName());
        
        CreateCustomerCommand command = new CreateCustomerCommand(
            sanitizedName,
            request.getEmail(),
            passwordEncoder.encode(request.getPassword())
        );
        
        Customer customer = customerService.createCustomer(command);
        return ResponseEntity.ok(CustomerResponse.from(customer));
    }
}

@Repository
public class CustomerRepository {
    
    // ✅ Good: Parameterized query prevents SQL injection
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.status = :status")
    Optional<Customer> findByEmailAndStatus(@Param("email") String email, @Param("status") String status);
}

// ❌ Bad: No input validation, potential SQL injection
@RestController
public class CustomerController {
    
    @PostMapping("/customers")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody Map<String, String> request) {
        // No validation, direct use of user input
        String query = "SELECT * FROM customers WHERE email = '" + request.get("email") + "'";
        // SQL injection vulnerability
    }
}
```

## Performance Review Standards

### Performance Checklist

- [ ] **Database Queries**: Efficient queries with proper indexing
- [ ] **N+1 Problems**: No N+1 query problems
- [ ] **Caching**: Appropriate use of caching mechanisms
- [ ] **Lazy Loading**: Proper use of lazy loading for large datasets
- [ ] **Memory Usage**: No memory leaks or excessive memory usage
- [ ] **Async Processing**: Long-running operations are asynchronous

#### Performance Code Examples

```java
// ✅ Good: Efficient query with JOIN FETCH
@Repository
public class OrderRepository {
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items JOIN FETCH o.customer WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") String orderId);
    
    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.orderDate DESC")
    Page<Order> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);
}

@Service
@CacheConfig(cacheNames = "customers")
public class CustomerService {
    
    @Cacheable(key = "#customerId")
    public Customer findById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}

// ❌ Bad: N+1 query problem
@Service
public class OrderService {
    
    public List<OrderSummary> getOrderSummaries(String customerId) {
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        
        return orders.stream()
            .map(order -> {
                // This causes N+1 queries - one for each order's items
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                return new OrderSummary(order, items);
            })
            .collect(Collectors.toList());
    }
}
```

## Testing Review Standards

### Test Quality Checklist

- [ ] **Test Coverage**: Adequate test coverage for new code (>80%)
- [ ] **Test Types**: Appropriate mix of unit, integration, and E2E tests
- [ ] **Test Naming**: Clear, descriptive test method names
- [ ] **Test Structure**: Tests follow Given-When-Then structure
- [ ] **Test Data**: Tests use appropriate test data and builders
- [ ] **Assertions**: Meaningful assertions that verify expected behavior
- [ ] **Edge Cases**: Tests cover edge cases and error conditions

#### Test Code Examples

```java
// ✅ Good: Clear test structure and meaningful assertions
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EmailService emailService;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void should_create_customer_and_send_welcome_email_when_valid_request_provided() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "john@example.com",
            "encodedPassword"
        );
        
        Customer expectedCustomer = Customer.builder()
            .id("customer-123")
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        when(customerRepository.save(any(Customer.class))).thenReturn(expectedCustomer);
        
        // When
        Customer result = customerService.createCustomer(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        verify(customerRepository).save(any(Customer.class));
        verify(emailService).sendWelcomeEmail("john@example.com", "John Doe");
    }
    
    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand(
            "John Doe",
            "existing@example.com",
            "encodedPassword"
        );
        
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(command))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage("Email already exists: existing@example.com");
        
        verify(customerRepository, never()).save(any(Customer.class));
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }
}

// ❌ Bad: Unclear test structure and weak assertions
@Test
void testCreateCustomer() {
    Customer customer = customerService.createCustomer(command);
    assertThat(customer).isNotNull(); // Too weak assertion
}
```

## Documentation Review Standards

### Documentation Checklist

- [ ] **API Documentation**: Public APIs are properly documented
- [ ] **Code Comments**: Complex logic is explained with comments
- [ ] **README Updates**: README is updated for significant changes
- [ ] **Architecture Documentation**: Architectural decisions are documented
- [ ] **Migration Guides**: Breaking changes include migration guides

#### Documentation Examples

```java
/**
 * Service for managing customer lifecycle operations.
 * 
 * This service handles customer registration, profile updates, and account management.
 * It integrates with the email service for notifications and maintains audit trails
 * for all customer operations.
 * 
 * @author Development Team
 * @since 1.0
 */
@Service
@Transactional
public class CustomerService {
    
    /**
     * Creates a new customer account with the provided information.
     * 
     * This method performs the following operations:
     * 1. Validates the customer information
     * 2. Checks for duplicate email addresses
     * 3. Creates the customer record
     * 4. Sends a welcome email
     * 5. Records the registration event
     * 
     * @param command the customer creation command containing all required information
     * @return the created customer with generated ID and timestamps
     * @throws EmailAlreadyExistsException if the email is already registered
     * @throws ValidationException if the customer information is invalid
     */
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation with inline comments for complex logic
        
        // Complex business rule that needs explanation
        if (isHighRiskRegistration(command)) {
            // High-risk registrations require additional verification
            // This includes customers from certain regions or with specific patterns
            scheduleAdditionalVerification(command);
        }
        
        return customer;
    }
}
```

## Review Feedback Guidelines

### Providing Constructive Feedback

#### Feedback Categories

- **Must Fix**: Critical issues that block merge
- **Should Fix**: Important issues that should be addressed
- **Consider**: Suggestions for improvement
- **Nitpick**: Minor style or preference issues
- **Praise**: Positive feedback for good practices

#### Feedback Examples

```markdown
## Must Fix
- **Security Issue**: SQL injection vulnerability in line 45. Use parameterized queries.
- **Bug**: Null pointer exception possible in line 23. Add null check.

## Should Fix
- **Performance**: N+1 query problem in `getOrderSummaries()`. Consider using JOIN FETCH.
- **Error Handling**: Generic exception handling in line 67. Use specific exceptions.

## Consider
- **Design**: Consider extracting this logic into a separate service for better separation of concerns.
- **Readability**: This method is quite long. Consider breaking it into smaller methods.

## Nitpick
- **Style**: Consider using more descriptive variable names (e.g., `customerList` instead of `list`).

## Praise
- **Good Practice**: Excellent use of builder pattern for test data creation.
- **Clean Code**: Well-structured method with clear single responsibility.
```

### Responding to Feedback

#### Author Response Guidelines

- **Acknowledge**: Acknowledge all feedback, even if you disagree
- **Explain**: Provide context for decisions when necessary
- **Ask Questions**: Ask for clarification if feedback is unclear
- **Be Open**: Be open to suggestions and alternative approaches
- **Update**: Update the code based on valid feedback

#### Response Examples

```markdown
## Response to Feedback

**@reviewer1 regarding SQL injection concern:**
Good catch! I've updated the query to use parameterized queries. See commit abc123.

**@reviewer2 regarding method length:**
I understand the concern about method length. I've extracted the validation logic 
into separate methods. However, I kept the main flow together as it represents 
a single business transaction. What do you think?

**@reviewer3 regarding variable naming:**
Thanks for the suggestion. I've updated the variable names to be more descriptive.
```

## Review Metrics and Quality Gates

### Quality Gates

- **Code Coverage**: Minimum 80% line coverage for new code
- **Complexity**: Cyclomatic complexity ≤ 10 per method
- **Duplication**: No code duplication > 5 lines
- **Security**: No high or critical security vulnerabilities
- **Performance**: No performance regressions

### Review Metrics

- **Review Time**: Average time to complete review
- **Feedback Quality**: Number of issues found per review
- **Rework Rate**: Percentage of PRs requiring significant rework
- **Approval Rate**: Percentage of PRs approved on first review

## Review Tools and Automation

### Automated Checks

```yaml
# GitHub Actions workflow for automated checks
name: Code Review Automation

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run SonarQube Analysis
        uses: sonarqube-quality-gate-action@master
        
      - name: Run Security Scan
        uses: securecodewarrior/github-action-add-sarif@v1
        
      - name: Check Test Coverage
        run: ./gradlew jacocoTestReport
        
      - name: Verify Performance Benchmarks
        run: ./gradlew performanceTest
```

### Review Checklist Automation

```markdown
## Automated PR Checklist

- [ ] All tests pass
- [ ] Code coverage ≥ 80%
- [ ] No security vulnerabilities
- [ ] No performance regressions
- [ ] Documentation updated
- [ ] Breaking changes documented
- [ ] Migration guide provided (if needed)
```

This comprehensive code review standard ensures consistent, high-quality code reviews that maintain our development standards while fostering a collaborative and learning-oriented environment.
