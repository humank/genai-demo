# Coding Standards and Guidelines

## Overview

This document defines the coding standards and best practice guidelines for the project, ensuring code consistency, readability, and maintainability. These standards cover Java backend, TypeScript/React/Angular frontend, API design, database design, and other aspects.

## 🎯 Core Principles

### 1. Consistency Principle
- Follow unified coding style and formatting
- Use consistent naming conventions
- Maintain consistency in project structure and architectural patterns
- Unified error handling and logging approaches

### 2. Readability Principle
- Write self-documenting code
- Use meaningful and descriptive variable and method names
- Add appropriate comments to explain complex logic and business rules
- Keep code concise and clear, avoid over-complexity

### 3. Maintainability Principle
- Follow SOLID principles and DDD tactical patterns
- Keep methods and classes concise with single responsibility
- Avoid code duplication, extract common logic
- Design code structures that are easy to test and extend

### 4. Security Principle
- Follow secure coding practices
- Perform strict input validation and output encoding
- Protect sensitive data, avoid information leakage
- Implement appropriate authentication and authorization mechanisms

## 📋 Java Coding Standards

### Naming Conventions

#### Classes and Interfaces
```java
// ✅ Correct: Use PascalCase, descriptive names
public class CustomerRegistrationService { }
public interface PaymentGatewayAdapter { }
public class OrderCreatedEvent { }

// ❌ Wrong: Abbreviations, unclear names
public class CustRegSvc { }
public interface PmtGw { }
public class Event1 { }
```

#### Methods and Variables
```java
// ✅ Correct: Use camelCase, verb-noun pattern
public Customer findCustomerById(String customerId) { }
public boolean isEligibleForDiscount(Customer customer) { }
public void sendWelcomeEmail(String emailAddress) { }

private final CustomerRepository customerRepository;
private final EmailNotificationService emailNotificationService;

// ❌ Wrong: Unclear names
public Customer get(String id) { }
public boolean check(Customer c) { }
public void send(String addr) { }

private final CustomerRepository repo;
private final EmailNotificationService svc;
```

#### Constants and Enums
```java
// ✅ Correct: Use UPPER_SNAKE_CASE
public static final String DEFAULT_CURRENCY_CODE = "TWD";
public static final int MAX_RETRY_ATTEMPTS = 3;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}

// ❌ Wrong: Inconsistent naming
public static final String defaultCurrency = "TWD";
public static final int maxRetry = 3;
```

### Code Structure Standards

#### Method Design
```java
// ✅ Correct: Concise methods, single responsibility
@Service
@Transactional
public class OrderProcessingService {
    
    public Order processOrder(ProcessOrderCommand command) {
        validateOrderCommand(command);
        
        Order order = createOrderFromCommand(command);
        reserveInventory(order);
        processPayment(order);
        
        Order savedOrder = orderRepository.save(order);
        publishOrderCreatedEvent(savedOrder);
        
        return savedOrder;
    }
    
    private void validateOrderCommand(ProcessOrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Order command cannot be null");
        }
        if (command.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("Order must contain at least one item");
        }
    }
}

// ❌ Wrong: Method too long, mixed responsibilities
public Order processOrder(ProcessOrderCommand command) {
    // 50+ lines of mixed validation, calculation, processing logic
    if (command != null && !command.getItems().isEmpty()) {
        // Complex validation logic...
        // Complex calculation logic...
        // Complex processing logic...
        // Complex storage logic...
    }
}
```

#### Class Design
```java
// ✅ Correct: Single responsibility, clear purpose
@AggregateRoot(name = "Customer", boundedContext = "Customer")
public class Customer implements AggregateRootInterface {
    
    private final CustomerId id;
    private CustomerName name;
    private Email email;
    private CustomerStatus status;
    
    public void updateProfile(CustomerName newName, Email newEmail) {
        validateProfileUpdate(newName, newEmail);
        
        this.name = newName;
        this.email = newEmail;
        
        collectEvent(CustomerProfileUpdatedEvent.create(this.id, newName, newEmail));
    }
    
    private void validateProfileUpdate(CustomerName name, Email email) {
        if (this.status == CustomerStatus.SUSPENDED) {
            throw new BusinessRuleViolationException("Cannot update profile of suspended customer");
        }
    }
}

// ❌ Wrong: Multiple responsibilities, unclear purpose
@Service
public class CustomerService {
    // Handles customers, orders, products, payments, notifications, reports...
    // 500+ lines of mixed responsibilities
}
```

### Exception Handling Standards

#### Custom Exception Hierarchy
```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;
    
    protected DomainException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : Map.of();
    }
    
    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getContext() { return context; }
}

// Business rule violation exception
public class BusinessRuleViolationException extends DomainException {
    public BusinessRuleViolationException(String rule, String message) {
        super("BUSINESS_RULE_VIOLATION", message, Map.of("rule", rule));
    }
}

// Resource not found exception
public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s with id %s not found", resourceType, resourceId),
              Map.of("resourceType", resourceType, "resourceId", resourceId));
    }
}
```

#### Exception Handling Best Practices
```java
// ✅ Correct: Specific exception handling, appropriate context
@Service
public class CustomerService {
    
    public Customer findCustomerById(String customerId) {
        try {
            return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        } catch (DataAccessException e) {
            logger.error("Database error while fetching customer: {}", customerId, e);
            throw new CustomerServiceException("Unable to retrieve customer data", e);
        }
    }
}

// ❌ Wrong: Generic exceptions, lack of context
public Customer findCustomerById(String customerId) {
    try {
        return customerRepository.findById(customerId).get();
    } catch (Exception e) {
        throw new RuntimeException("Error");
    }
}
```

## 🌐 Frontend Coding Standards

### TypeScript Standards

#### Type Definitions
```typescript
// ✅ Correct: Clear type definitions
interface Customer {
  readonly id: string;
  name: string;
  email: string;
  status: CustomerStatus;
  createdAt: Date;
  updatedAt: Date;
}

type CustomerStatus = 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';

interface CreateCustomerRequest {
  name: string;
  email: string;
  initialStatus?: CustomerStatus;
}

// ❌ Wrong: Using any, lack of type safety
interface Customer {
  id: any;
  name: any;
  email: any;
  status: any;
}
```

#### React Component Standards
```typescript
// ✅ Correct: Functional component, clear props types
interface CustomerListProps {
  customers: Customer[];
  onCustomerSelect: (customer: Customer) => void;
  loading?: boolean;
}

export const CustomerList: React.FC<CustomerListProps> = ({
  customers,
  onCustomerSelect,
  loading = false
}) => {
  const handleCustomerClick = useCallback((customer: Customer) => {
    onCustomerSelect(customer);
  }, [onCustomerSelect]);

  if (loading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="customer-list">
      {customers.map(customer => (
        <CustomerCard
          key={customer.id}
          customer={customer}
          onClick={handleCustomerClick}
        />
      ))}
    </div>
  );
};

// ❌ Wrong: Class component, lack of type definitions
class CustomerList extends React.Component {
  render() {
    return (
      <div>
        {this.props.customers.map(customer => (
          <div key={customer.id} onClick={() => this.props.onSelect(customer)}>
            {customer.name}
          </div>
        ))}
      </div>
    );
  }
}
```

### Angular Standards

#### Service Design
```typescript
// ✅ Correct: Injectable service, clear types
@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private readonly apiUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.apiUrl).pipe(
      catchError(this.handleError<Customer[]>('getCustomers', []))
    );
  }

  getCustomerById(id: string): Observable<Customer> {
    const url = `${this.apiUrl}/${id}`;
    return this.http.get<Customer>(url).pipe(
      catchError(this.handleError<Customer>(`getCustomer id=${id}`))
    );
  }

  private handleError<T>(operation = 'operation', result?: T) {
    return (error: any): Observable<T> => {
      console.error(`${operation} failed: ${error.message}`);
      return of(result as T);
    };
  }
}
```

## 🔌 API Design Guidelines

### REST API Conventions

#### URL Naming Standards

Follow RESTful design principles:
- Use plural nouns for resources
- Use HTTP verbs for operations
- Nested resources for relationships
- Action endpoints for non-CRUD operations

For detailed API design guidelines, refer to: [API Design Standards](coding-standards/api-design-standards.md)

#### HTTP Status Code Standards
- **200 OK**: Successful GET, PUT, PATCH
- **201 Created**: Successful POST
- **204 No Content**: Successful DELETE
- **400 Bad Request**: Validation errors, malformed requests
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Authorization failed
- **404 Not Found**: Resource not found
- **409 Conflict**: Business rule violation
- **422 Unprocessable Entity**: Semantic validation errors
- **500 Internal Server Error**: System errors

#### Request/Response Format Standards
```java
// Request DTO
public record CreateCustomerRequest(
    @NotBlank String name,
    @Email String email,
    @Valid AddressDto address
) {}

// Response DTO
public record CustomerResponse(
    String id,
    String name,
    String email,
    AddressDto address,
    Instant createdAt,
    Instant updatedAt
) {}

// Error Response
public record ErrorResponse(
    String errorCode,
    String message,
    Map<String, Object> context,
    Instant timestamp,
    List<FieldError> fieldErrors
) {}
```

## 🗄️ Database Design Guidelines

### Table Naming Conventions
```sql
-- ✅ Correct: Plural form, snake_case
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customer_orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    order_date TIMESTAMP NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL
);

-- ❌ Wrong: Inconsistent naming
CREATE TABLE Customer (
    ID UUID PRIMARY KEY,
    CustomerName VARCHAR(100),
    Email VARCHAR(255)
);
```

### Index Strategy
```sql
-- Primary key index (automatically created)
-- Foreign key indexes
CREATE INDEX idx_customer_orders_customer_id ON customer_orders(customer_id);

-- Query optimization indexes
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status_created ON customers(status, created_at);

-- Composite indexes for complex queries
CREATE INDEX idx_orders_customer_date ON customer_orders(customer_id, order_date);
```

### JPA Entity Design
```java
// ✅ Correct: Clear entity mapping
@Entity
@Table(name = "customers")
public class Customer {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Lazy loading associations
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
}
```

## 📝 Documentation Writing Guidelines

### Code Comment Standards

#### JavaDoc Standards
```java
/**
 * Service for managing customer lifecycle operations.
 * 
 * This service handles customer registration, profile updates, and account management.
 * It integrates with email service for notifications and maintains audit trails
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
     * 1. Validates customer information
     * 2. Checks for duplicate email addresses
     * 3. Creates customer record
     * 4. Sends welcome email
     * 5. Records registration event
     * 
     * @param command the customer creation command containing all required information
     * @return the created customer with generated ID and timestamps
     * @throws EmailAlreadyExistsException if the email is already registered
     * @throws ValidationException if the customer information is invalid
     */
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation logic...
    }
}
```

#### Inline Comment Standards
```java
public void processComplexBusinessLogic(Order order) {
    // High-risk orders require additional verification
    // This includes orders from certain regions or with specific patterns
    if (isHighRiskOrder(order)) {
        scheduleAdditionalVerification(order);
    }
    
    // Discount calculation needs to consider customer level and promotions
    BigDecimal discount = calculateDiscount(order);
    order.applyDiscount(discount);
    
    // TODO: Implement dynamic pricing logic (JIRA-123)
    // FIXME: Handle out-of-stock scenarios (BUG-456)
}
```

### Markdown Documentation Standards

#### Document Structure
```markdown
# Document Title

## Overview
Brief explanation of the document's purpose and scope.

## Table of Contents
- [Section 1](#section-1)
- [Section 2](#section-2)

## Section 1
Detailed content...

### Subsection 1.1
More detailed content...

## Code Examples
```java
// Code example
public class Example {
    // Implementation...
}
```

## Related Resources
- Related Document 1
- Related Document 2

---
**Last Updated**: January 21, 2025  
**Maintainer**: Development Team  
**Version**: 1.0
```

## 🔍 Code Review Guidelines

### Review Process

#### Pull Request Requirements
- [ ] **Title**: Clear, descriptive title following format: `[TYPE] Brief description`
  - Types: `FEAT`, `FIX`, `REFACTOR`, `DOCS`, `TEST`, `CHORE`
- [ ] **Description**: Detailed explanation of changes and reasons
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
- [ ] **Business Logic**: Code correctly implements requirements
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
- [ ] **DDD Compliance**: Follows domain-driven design principles
- [ ] **Layer Separation**: Proper separation of concerns across layers
- [ ] **Dependencies**: Dependencies are properly managed and injected

### Feedback Guidelines

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

## 🛠️ Tools and Automation

### Code Formatting Tools

#### Java Tool Configuration
```xml
<!-- Checkstyle configuration -->
<checkstyle>
    <module name="Checker">
        <module name="TreeWalker">
            <module name="NamingConventions"/>
            <module name="LineLength">
                <property name="max" value="120"/>
            </module>
            <module name="MethodLength">
                <property name="max" value="20"/>
            </module>
        </module>
    </module>
</checkstyle>
```

#### TypeScript Tool Configuration
```json
// .eslintrc.json
{
  "extends": [
    "@typescript-eslint/recommended",
    "prettier"
  ],
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/explicit-function-return-type": "warn",
    "prefer-const": "error",
    "no-var": "error"
  }
}

// prettier.config.js
module.exports = {
  semi: true,
  trailingComma: 'es5',
  singleQuote: true,
  printWidth: 100,
  tabWidth: 2
};
```

### IDE Configuration

#### IntelliJ IDEA Settings
```xml
<!-- .idea/codeStyles/Project.xml -->
<component name="ProjectCodeStyleConfiguration">
  <code_scheme name="Project">
    <JavaCodeStyleSettings>
      <option name="IMPORT_LAYOUT_TABLE">
        <value>
          <package name="java" withSubpackages="true" static="false"/>
          <package name="javax" withSubpackages="true" static="false"/>
          <emptyLine/>
          <package name="org" withSubpackages="true" static="false"/>
          <emptyLine/>
          <package name="com" withSubpackages="true" static="false"/>
          <emptyLine/>
          <package name="" withSubpackages="true" static="false"/>
        </value>
      </option>
    </JavaCodeStyleSettings>
  </code_scheme>
</component>
```

#### VS Code Settings
```json
// .vscode/settings.json
{
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": true,
    "source.organizeImports": true
  },
  "typescript.preferences.importModuleSpecifier": "relative",
  "typescript.suggest.autoImports": true
}
```

### Automated Checks

#### Pre-commit Hooks
```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.4.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-json

  - repo: https://github.com/psf/black
    rev: 22.10.0
    hooks:
      - id: black
        language_version: python3

  - repo: local
    hooks:
      - id: checkstyle
        name: Checkstyle
        entry: ./gradlew checkstyleMain
        language: system
        pass_filenames: false
```

#### CI/CD Pipeline Checks
```yaml
# .github/workflows/code-quality.yml
name: Code Quality

on:
  pull_request:
    branches: [ main, develop ]

jobs:
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Checkstyle
        run: ./gradlew checkstyleMain
      
      - name: Run SpotBugs
        run: ./gradlew spotbugsMain
      
      - name: Run Tests
        run: ./gradlew test
      
      - name: Generate Test Report
        run: ./gradlew jacocoTestReport
      
      - name: Check Coverage
        run: ./gradlew jacocoTestCoverageVerification
```

## 📊 Quality Metrics and Thresholds

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

## 🔗 Related Resources

### Internal Documentation
- [Development Viewpoint Overview](README.md)
- [Architecture Design Standards](architecture/)
- [Testing Standards](testing/)
- [Build and Deployment](build-system/)

### External References
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- [Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)
- [Effective Java](https://www.amazon.com/Effective-Java-Joshua-Bloch/dp/0134685997)

---

**Last Updated**: January 21, 2025  
**Maintainer**: Development Team  
**Version**: 1.0

> 💡 **Tip**: Coding standards are not constraints, but the foundation for team collaboration. Following these standards enables us to collaborate and maintain code more efficiently.