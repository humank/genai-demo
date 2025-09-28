# Coding Standards

## Overview

This directory contains the project's coding standards and best practices, covering Java backend, frontend development, API design, and documentation writing across all aspects.

## Coding Standards Summary

### Core Principles
1. **Consistency** - Maintain consistent coding style throughout the project
2. **Readability** - Code should be easy to understand and maintain
3. **Simplicity** - Avoid unnecessary complexity
4. **Security** - Follow secure coding practices
5. **Performance** - Consider the performance impact of code

### Scope of Application
- Java backend development
- React/Next.js frontend development
- Angular frontend development
- API design and documentation
- Database design
- Test code

## Core Documentation

- **[API Design Standards](api-design-standards.md)** - REST API design specifications and best practices

## Java Coding Standards

### Naming Conventions
```java
// Class names: PascalCase
public class CustomerService {
    
    // Constants: UPPER_SNAKE_CASE
    private static final String DEFAULT_CURRENCY = "TWD";
    
    // Variables and methods: camelCase
    private final CustomerRepository customerRepository;
    
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation
    }
}
```

### Code Organization
```java
// 1. Static imports
import static org.assertj.core.api.Assertions.assertThat;

// 2. Standard library imports
import java.time.LocalDateTime;
import java.util.List;

// 3. Third-party library imports
import org.springframework.stereotype.Service;

// 4. Project internal imports
import solid.humank.genaidemo.domain.Customer;

@Service
public class CustomerService {
    // 1. Static variables
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    // 2. Instance variables
    private final CustomerRepository customerRepository;
    
    // 3. Constructor
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    // 4. Public methods
    public Customer createCustomer(CreateCustomerCommand command) {
        // Implementation
    }
    
    // 5. Private methods
    private void validateCommand(CreateCustomerCommand command) {
        // Implementation
    }
}
```

### Annotation Usage
```java
// DDD annotations
@AggregateRoot(name = "Customer", description = "Customer aggregate root")
public class Customer {
    
    @ValueObject
    public record CustomerId(String value) {
        // Value object implementation
    }
}

// Spring annotations
@Service
@Transactional
public class CustomerApplicationService {
    // Application service implementation
}

// Test annotations
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
}
```

## API Design Standards

For detailed API design specifications, please refer to: **[API Design Standards](api-design-standards.md)**

### Core API Principles
- RESTful design principles
- Unified error handling
- Versioning strategy
- Security best practices

## Frontend Coding Standards

### React/Next.js Standards
```typescript
// Component naming: PascalCase
interface CustomerListProps {
  customers: Customer[];
  onCustomerSelect: (customer: Customer) => void;
}

export const CustomerList: React.FC<CustomerListProps> = ({
  customers,
  onCustomerSelect
}) => {
  // Hook usage
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  
  // Event handler functions: handleXxx
  const handleCustomerClick = useCallback((customer: Customer) => {
    setSelectedCustomer(customer);
    onCustomerSelect(customer);
  }, [onCustomerSelect]);
  
  return (
    <div className="customer-list">
      {customers.map(customer => (
        <CustomerCard
          key={customer.id}
          customer={customer}
          onClick={() => handleCustomerClick(customer)}
        />
      ))}
    </div>
  );
};
```

### Angular Standards
```typescript
// Service naming: XxxService
@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private readonly apiUrl = '/api/v1/customers';
  
  constructor(private http: HttpClient) {}
  
  // Method naming: verb-first
  getCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(this.apiUrl);
  }
  
  createCustomer(customer: CreateCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.apiUrl, customer);
  }
}

// Component naming: XxxComponent
@Component({
  selector: 'app-customer-list',
  templateUrl: './customer-list.component.html',
  styleUrls: ['./customer-list.component.scss']
})
export class CustomerListComponent implements OnInit {
  customers: Customer[] = [];
  
  constructor(private customerService: CustomerService) {}
  
  ngOnInit(): void {
    this.loadCustomers();
  }
  
  private loadCustomers(): void {
    this.customerService.getCustomers().subscribe({
      next: (customers) => this.customers = customers,
      error: (error) => console.error('Failed to load customers', error)
    });
  }
}
```

## API Design Standards

### REST API Specifications
```java
@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {
    
    // GET retrieve resource list
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Implementation
    }
    
    // GET retrieve single resource
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
        // Implementation
    }
    
    // POST create resource
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        // Implementation
    }
    
    // PUT update resource
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        // Implementation
    }
    
    // DELETE delete resource
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        // Implementation
    }
}
```

### URL Design Specifications
```
# Resource naming: plural nouns
GET    /api/v1/customers           # Get customer list
GET    /api/v1/customers/{id}      # Get specific customer
POST   /api/v1/customers           # Create customer
PUT    /api/v1/customers/{id}      # Update customer
DELETE /api/v1/customers/{id}      # Delete customer

# Nested resources
GET    /api/v1/customers/{id}/orders    # Get customer's orders
POST   /api/v1/customers/{id}/orders    # Create order for customer

# Action resources
POST   /api/v1/orders/{id}/cancel       # Cancel order
POST   /api/v1/orders/{id}/ship         # Ship order
```

## Test Coding Standards

### Test Naming
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    // Test method naming: should_expectedBehavior_when_condition
    @Test
    void should_create_customer_when_valid_command_provided() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand("John Doe", "john@example.com");
        
        // When
        Customer result = customerService.createCustomer(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
    }
    
    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand("John Doe", "existing@example.com");
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerService.createCustomer(command))
            .isInstanceOf(EmailAlreadyExistsException.class)
            .hasMessage("Email already exists: existing@example.com");
    }
}
```

## Code Quality Tools

### Static Analysis Tools
- **Checkstyle** - Code style checking
- **SpotBugs** - Potential bug detection
- **SonarQube** - Code quality analysis
- **ESLint** - JavaScript/TypeScript code checking

### Formatting Tools
- **Spotless** - Java code formatting
- **Prettier** - Frontend code formatting
- **EditorConfig** - Unified editor configuration

### Configuration Examples
```gradle
// build.gradle
spotless {
    java {
        googleJavaFormat('1.15.0')
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = '10.3'
    configFile = file('config/checkstyle/checkstyle.xml')
}
```

## Related Resources

### Internal Documentation
- [Development Standards](../../../../.kiro/steering/development-standards.md)
- [Code Review Standards](../../../../.kiro/steering/code-review-standards.md)
- [Testing Standards](../testing/README.md)

### External Resources
- Google Java Style Guide
- Airbnb JavaScript Style Guide
- Angular Style Guide

---

**Maintainer**: Development Team  
**Last Updated**: January 21, 2025  
**Version**: 1.0
![Microservices Overview](../../../diagrams/viewpoints/development/microservices-overview.puml)
![Microservices Overview](../../../diagrams/viewpoints/development/architecture/microservices-overview.mmd)