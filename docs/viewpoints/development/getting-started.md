# Getting Started Guide

## Overview

Welcome to our development team! This guide will help you quickly set up your development environment, understand the project structure, and complete your first contribution. Whether you're an experienced developer or just starting with our tech stack, this guide provides everything you need.

## 📋 Prerequisites Checklist

Before starting, please ensure you have the following:

### Essential Tools

#### Java Development Environment
- [ ] **Java 21** - OpenJDK or Oracle JDK
  ```bash
  # Check Java version
  java -version
  # Should display Java 21.x.x
  ```

#### Frontend Development Environment
- [ ] **Node.js 18+** - Frontend development and toolchain
  ```bash
  # Check Node.js version
  node --version
  # Should display v18.x.x or higher
  ```

#### Version Control and Containerization
- [ ] **Git** - Version control system
  ```bash
  # Check Git version
  git --version
  ```
- [ ] **Docker** - Containerized development environment
  ```bash
  # Check Docker version
  docker --version
  ```

#### Cloud Tools
- [ ] **AWS CLI** - Cloud resource management
  ```bash
  # Install AWS CLI
  curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
  unzip awscliv2.zip
  sudo ./aws/install
  
  # Verify installation
  aws --version
  ```

### Recommended Tools

#### Development Environment
- [ ] **IntelliJ IDEA Ultimate** - Java development IDE (recommended)
  - Supports Spring Boot, JPA, Cucumber
  - Built-in Git integration and database tools
- [ ] **VS Code** - Lightweight editor
  - Suitable for frontend development and documentation editing
  - Rich extension ecosystem

#### API and Database Tools
- [ ] **Postman** or **Insomnia** - API testing tools
- [ ] **DBeaver** - Database management tool
- [ ] **Kiro IDE** - AI-assisted development tool

### Software Installation Guide

#### Install Java using SDKMAN
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 21
sdk install java 21.0.1-tem
sdk use java 21.0.1-tem

# Set as default version
sdk default java 21.0.1-tem

# Verify installation
java -version
javac -version
```

#### Install Node.js using NVM
```bash
# Install NVM
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# Install Node.js 18
nvm install 18
nvm use 18
nvm alias default 18

# Verify installation
node --version
npm --version
```

#### Docker Installation
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install docker.io docker-compose

# macOS (using Homebrew)
brew install docker docker-compose

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Verify installation
docker --version
docker-compose --version
```

## ⚙️ Environment Setup

### 1. Project Clone and Initial Setup

```bash
# Clone project
git clone https://github.com/your-org/genai-demo.git
cd genai-demo

# Check project structure
ls -la

# Set Git configuration
git config user.name "Your Name"
git config user.email "your.email@company.com"

# Install Git hooks
cp scripts/pre-commit .git/hooks/
chmod +x .git/hooks/pre-commit
```

### 2. Backend Environment Setup

#### Gradle Build and Testing
```bash
# Check Gradle version
./gradlew --version

# Clean and build project
./gradlew clean build

# Run all tests
./gradlew test

# Run specific test types
./gradlew unitTest           # Unit tests
./gradlew integrationTest    # Integration tests
./gradlew cucumber          # BDD tests

# Generate test reports
./gradlew jacocoTestReport

# Check code quality
./gradlew checkstyleMain spotbugsMain
```

#### Application Startup
```bash
# Start with default profile (development environment)
./gradlew bootRun

# Start with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'

# Check if application started successfully
curl http://localhost:8080/actuator/health
```

### 3. Frontend Environment Setup

#### CMC Management Frontend (Next.js)
```bash
cd cmc-frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Build production version
npm run build

# Run tests
npm test

# Check code quality
npm run lint
npm run type-check
```

#### Consumer Frontend (Angular)
```bash
cd consumer-frontend

# Install dependencies
npm install

# Start development server
npm start

# Build production version
npm run build

# Run tests
npm test

# Run E2E tests
npm run e2e
```

### 4. Database Setup

#### Development Environment (H2 Embedded Database)
```bash
# H2 database starts automatically, no additional setup needed
# Access H2 console via the following URL
# http://localhost:8080/h2-console

# Connection information:
# JDBC URL: jdbc:h2:file:./data/devdb
# User Name: sa
# Password: (leave empty)
```

#### Local PostgreSQL (using Docker)
```bash
# Start PostgreSQL container
docker run --name postgres-dev \
  -e POSTGRES_DB=genaidemo \
  -e POSTGRES_USER=dev \
  -e POSTGRES_PASSWORD=dev123 \
  -p 5432:5432 \
  -d postgres:15

# Run database migrations
./gradlew flywayMigrate

# Check database connection
./gradlew flywayInfo
```

#### Start Complete Environment using Docker Compose
```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🏗️ Project Structure Deep Dive

### Overall Architecture
```
genai-demo/
├── app/                        # Spring Boot main application
│   ├── src/main/java/         # Java source code
│   │   └── solid/humank/genaidemo/
│   │       ├── domain/        # Domain layer (DDD core)
│   │       │   ├── customer/  # Customer aggregate
│   │       │   ├── order/     # Order aggregate
│   │       │   └── shared/    # Shared kernel
│   │       ├── application/   # Application layer (use case implementation)
│   │       │   ├── customer/  # Customer use cases
│   │       │   └── order/     # Order use cases
│   │       └── infrastructure/ # Infrastructure layer
│   │           ├── persistence/ # Data persistence
│   │           ├── web/       # Web controllers
│   │           └── messaging/ # Message handling
│   ├── src/test/              # Test code
│   │   ├── java/             # Java tests
│   │   └── resources/        # Test resources
│   │       └── features/     # BDD feature files
│   └── src/main/resources/   # Application resources
│       ├── application.yml   # Application configuration
│       └── db/migration/     # Database migration scripts
├── cmc-frontend/              # CMC management frontend
│   ├── src/                  # Source directory
│   │   ├── app/             # Next.js application
│   │   ├── components/      # React components
│   │   ├── pages/           # Page routes
│   │   └── styles/          # Style files
│   ├── public/              # Static assets
│   └── tests/               # Frontend tests
├── consumer-frontend/         # Consumer frontend
│   ├── src/                 # Angular source code
│   │   ├── app/            # Angular application
│   │   ├── assets/         # Static assets
│   │   └── environments/   # Environment configuration
│   └── e2e/                # E2E tests
├── infrastructure/           # AWS CDK infrastructure
│   ├── lib/                # CDK constructs
│   ├── bin/                # CDK application entry
│   └── test/               # Infrastructure tests
├── docs/                    # Project documentation
│   ├── viewpoints/         # Architecture viewpoint documentation
│   │   ├── functional/     # Functional viewpoint
│   │   ├── information/    # Information viewpoint
│   │   ├── deployment/     # Deployment viewpoint
│   │   └── development/    # Development viewpoint
│   └── diagrams/           # Architecture diagrams
├── scripts/                # Automation scripts
│   ├── build/             # Build scripts
│   ├── deploy/            # Deployment scripts
│   └── test/              # Test scripts
└── .kiro/                 # Kiro IDE configuration
    ├── hooks/             # Git hooks
    └── steering/          # Development guidelines
```

### Core Module Descriptions

#### Domain Layer
- **Aggregate Roots**: Business entity roots, such as `Customer`, `Order`
- **Value Objects**: Immutable business concepts, such as `Email`, `Money`
- **Domain Services**: Business logic across aggregates
- **Domain Events**: Representation of business events

#### Application Layer
- **Application Services**: Use case coordinators
- **Commands & Queries**: CQRS pattern implementation
- **Event Handlers**: Domain event processing

#### Infrastructure Layer
- **Database Adapters**: JPA entities and repository implementations
- **Web Adapters**: REST controllers and DTOs
- **Message Adapters**: Event publishing and subscription

## 🎯 First Contribution Step-by-Step Guide

### 1. Choose Appropriate Tasks

#### Beginner-Friendly Task Types
- **Documentation Improvements**: Fix typos, update outdated information, add examples
- **Test Enhancements**: Increase test coverage, fix test cases
- **Code Refactoring**: Improve code readability, extract duplicate logic
- **Small Feature Implementation**: Simple CRUD operations, validation logic

#### Ways to Find Tasks
```bash
# Check GitHub Issues
# Filter by labels: good-first-issue, documentation, testing, refactoring

# Or start with code quality improvements
./gradlew checkstyleMain  # Check code style issues
./gradlew spotbugsMain    # Check potential bugs
./gradlew jacocoTestReport # Check test coverage
```

### 2. Create Development Branch

```bash
# Ensure on latest main branch
git checkout main
git pull origin main

# Create feature branch (use descriptive names)
git checkout -b feature/add-customer-validation
# or
git checkout -b fix/order-calculation-bug
# or
git checkout -b docs/update-api-documentation
```

### 3. Follow Development Standards and Best Practices

#### Java Coding Standards
```java
// ✅ Correct: Clear class and method naming
@Service
@Transactional
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

// ❌ Wrong: Unclear naming and structure
@Service
public class CustSvc {
    public Cust reg(CustReq req) {
        // Unclear implementation
    }
}
```

#### API Design Guidelines
```java
// ✅ Correct: RESTful API design
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
    
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {
        
        Customer customer = customerService.createCustomer(request);
        CustomerResponse response = CustomerResponse.from(customer);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
        Customer customer = customerService.findById(id);
        CustomerResponse response = CustomerResponse.from(customer);
        
        return ResponseEntity.ok(response);
    }
}
```

#### Frontend Coding Standards (React/TypeScript)
```typescript
// ✅ Correct: Type-safe React component
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
```

### 4. Test-Driven Development (TDD) Practice

#### BDD Scenario Writing
```gherkin
# src/test/resources/features/customer-registration.feature
Feature: Customer Registration
  As a new user
  I want to register for an account
  So that I can access the system

  Scenario: Successful customer registration
    Given I am a new customer with valid information
      | name          | John Doe           |
      | email         | john@example.com   |
      | password      | SecurePass123!     |
    When I submit the registration form
    Then I should receive a confirmation email
    And my account should be created successfully
    And I should be redirected to the welcome page

  Scenario: Registration with duplicate email
    Given a customer already exists with email "existing@example.com"
    When I try to register with the same email
    Then I should see an error message "Email already registered"
    And my account should not be created
```

#### Unit Test Implementation
```java
@ExtendWith(MockitoExtension.class)
class CustomerRegistrationServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private EmailNotificationService emailNotificationService;
    
    @InjectMocks
    private CustomerRegistrationService customerRegistrationService;
    
    @Test
    void should_create_customer_and_send_welcome_email_when_valid_request_provided() {
        // Given
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
            "John Doe",
            "john@example.com",
            "SecurePass123!"
        );
        
        Customer expectedCustomer = Customer.builder()
            .id("customer-123")
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(expectedCustomer);
        
        // When
        Customer result = customerRegistrationService.registerNewCustomer(request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        
        verify(customerRepository).save(any(Customer.class));
        verify(emailNotificationService).sendWelcomeEmail("john@example.com", "John Doe");
    }
    
    @Test
    void should_throw_exception_when_email_already_exists() {
        // Given
        CustomerRegistrationRequest request = new CustomerRegistrationRequest(
            "John Doe",
            "existing@example.com",
            "SecurePass123!"
        );
        
        when(customerRepository.existsByEmail("existing@example.com")).thenReturn(true);
        
        // When & Then
        assertThatThrownBy(() -> customerRegistrationService.registerNewCustomer(request))
            .isInstanceOf(EmailAlreadyRegisteredException.class)
            .hasMessage("Email already registered: existing@example.com");
        
        verify(customerRepository, never()).save(any(Customer.class));
        verify(emailNotificationService, never()).sendWelcomeEmail(anyString(), anyString());
    }
}
```

#### Integration Testing
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class CustomerRegistrationIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Test
    void should_register_customer_successfully() {
        // Given
        CreateCustomerRequest request = new CreateCustomerRequest(
            "John Doe",
            "john@example.com",
            "SecurePass123!"
        );
        
        // When
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
            "/api/v1/customers",
            request,
            CustomerResponse.class
        );
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("John Doe");
        
        // Verify data in database
        Optional<Customer> savedCustomer = customerRepository.findByEmail("john@example.com");
        assertThat(savedCustomer).isPresent();
        assertThat(savedCustomer.get().getName()).isEqualTo("John Doe");
    }
}
```

### 5. Code Commit and Push

#### Commit Message Guidelines
```bash
# Use Conventional Commits format
git add .

# Feature addition
git commit -m "feat(customer): add customer registration validation"

# Bug fix
git commit -m "fix(order): correct order total calculation logic"

# Documentation update
git commit -m "docs(api): update customer API documentation"

# Test addition
git commit -m "test(customer): add unit tests for customer service"

# Refactoring
git commit -m "refactor(order): extract order calculation logic"

# Push to remote branch
git push origin feature/add-customer-validation
```

### 6. Create Pull Request

#### PR Title and Description Template
```markdown
## 📋 Pull Request Title
[FEAT] Add customer registration validation

## 📝 Description
### Changes Made
- Added email validation for customer registration
- Implemented password strength checking
- Added duplicate email checking

### Reason for Changes
- Improve system security
- Prevent invalid data from entering system
- Enhance user experience

### Testing
- [x] Unit tests passed
- [x] Integration tests passed
- [x] BDD scenarios verified
- [x] Manual testing completed

### Checklist
- [x] Code follows coding standards
- [x] All tests pass
- [x] Documentation updated
- [x] No breaking changes
- [x] Self-reviewed code

### Related Issues
Closes #123
Related to #456

### Screenshots (if applicable)
[Include before/after screenshots for UI changes]
```

#### PR Checklist
- [ ] **Code Quality**: Passes all static analysis checks
- [ ] **Test Coverage**: New code has appropriate test coverage
- [ ] **Documentation Updated**: Relevant documentation updated
- [ ] **Backward Compatible**: No breaking of existing functionality
- [ ] **Performance Impact**: Assessed impact on system performance
- [ ] **Security Considerations**: Checked for potential security issues

## 🧪 Test Execution Guide

### Test Layer Strategy

#### Unit Tests (80% coverage target)
```bash
# Run all unit tests
./gradlew unitTest

# Run specific class tests
./gradlew test --tests "CustomerServiceTest"

# Run specific method tests
./gradlew test --tests "CustomerServiceTest.should_create_customer_successfully"

# Generate test reports
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

#### Integration Tests (15% coverage target)
```bash
# Run integration tests
./gradlew integrationTest

# Run database integration tests
./gradlew test --tests "*IntegrationTest"

# Run web layer integration tests
./gradlew test --tests "*ControllerTest"
```

#### BDD Tests (5% coverage target)
```bash
# Run all BDD tests
./gradlew cucumber

# Run specific feature BDD tests
./gradlew cucumber --tests "*CustomerRegistration*"

# Generate BDD reports
open build/reports/cucumber/index.html
```

#### Performance Tests
```bash
# Run performance tests
./gradlew performanceTest

# Generate performance reports
./gradlew generatePerformanceReport
open build/reports/performance/index.html
```

### Frontend Testing

#### React Testing (Jest + Testing Library)
```bash
cd cmc-frontend

# Run all tests
npm test

# Run specific test file
npm test CustomerList.test.tsx

# Run tests and generate coverage report
npm test -- --coverage

# Run E2E tests
npm run e2e
```

#### Angular Testing (Jasmine + Karma)
```bash
cd consumer-frontend

# Run unit tests
npm test

# Run E2E tests
npm run e2e

# Generate test coverage report
npm run test:coverage
```

## 🔍 Common Issues and Troubleshooting

### Build Issues

#### Java Version Mismatch
```bash
# Problem: Java version is not 21
# Solution:
sdk list java
sdk use java 21.0.1-tem

# Verify
java -version
./gradlew --version
```

#### Gradle Build Failure
```bash
# Clean build cache
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# Check dependency conflicts
./gradlew dependencies

# Complete rebuild
./gradlew clean build
```

#### Out of Memory Issues
```bash
# Increase Gradle memory
export GRADLE_OPTS="-Xmx4g -XX:+UseG1GC"

# Or set in gradle.properties
echo "org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC" >> gradle.properties
```

### Test Issues

#### Test Database Connection Failure
```bash
# Check H2 database files
ls -la data/

# Reset test database
rm -rf data/testdb*
./gradlew test
```

#### Test Interference
```java
// Ensure test isolation
@Transactional
@Rollback
class CustomerServiceTest {
    
    @BeforeEach
    void setUp() {
        // Clean test data
        customerRepository.deleteAll();
    }
}
```

### Frontend Issues

#### Node.js Dependency Conflicts
```bash
# Clean node_modules
rm -rf node_modules package-lock.json
npm install

# Or use npm ci for clean install
npm ci
```

#### Port Conflicts
```bash
# Check port usage
lsof -i :8080  # Backend
lsof -i :3000  # React
lsof -i :4200  # Angular

# Kill process using port
kill -9 <PID>

# Or start with different port
npm start -- --port 3001
```

### Docker Issues

#### Container Startup Failure
```bash
# Check Docker service status
sudo systemctl status docker

# Restart Docker service
sudo systemctl restart docker

# Clean Docker resources
docker system prune -a
```

#### Database Container Connection Issues
```bash
# Check container status
docker ps -a

# View container logs
docker logs postgres-dev

# Restart container
docker restart postgres-dev
```

## 📚 Learning Resources and Advanced Guides

### Must-Read Documentation

#### Architecture and Design
- DDD Domain-Driven Design
- Hexagonal Architecture Implementation
- SOLID Design Principles

#### Testing Strategy
- TDD and BDD Practices
- Test Pyramid Strategy
- Performance Testing Guide

#### Technology Stack
- Spring Boot Best Practices
- React Development Guide
- Angular Development Guide

### Recommended Learning Path

#### Week 1: Basic Concepts and Environment Familiarity
- [ ] Complete environment setup
- [ ] Familiarize with project structure
- [ ] Read core architecture documentation
- [ ] Run first test
- [ ] Complete simple documentation fix

#### Week 2: Domain-Driven Design and Architecture Patterns
- [ ] Learn DDD tactical patterns
- [ ] Understand hexagonal architecture principles
- [ ] Implement simple aggregate root
- [ ] Write domain events
- [ ] Complete small feature development

#### Week 3: Test-Driven Development
- [ ] Master TDD red-green-refactor cycle
- [ ] Write BDD scenarios
- [ ] Implement integration tests
- [ ] Learn test double usage
- [ ] Improve test coverage

#### Week 4: Advanced Topics
- [ ] Understand microservices architecture
- [ ] Learn Saga pattern
- [ ] Implement CQRS pattern
- [ ] Master event sourcing
- [ ] Participate in code reviews

### External Learning Resources

#### Recommended Books
- **Domain-Driven Design** by Eric Evans
- **Clean Architecture** by Robert C. Martin
- **Microservices Patterns** by Chris Richardson
- **Test Driven Development** by Kent Beck
- **Refactoring** by Martin Fowler

#### Online Courses
- [Spring Boot Official Guides](https://spring.io/guides)
- [React Official Tutorial](https://reactjs.org/tutorial/tutorial.html)
- [Angular Official Tutorial](https://angular.io/tutorial)
- [AWS Developer Guide](https://docs.aws.amazon.com/)

#### Community Resources
- [DDD Community](https://github.com/ddd-crew)
- [Spring Boot GitHub](https://github.com/spring-projects/spring-boot)
- [React GitHub](https://github.com/facebook/react)
- [Angular GitHub](https://github.com/angular/angular)

### Team Collaboration and Communication

#### Communication Channels
- **Slack/Teams**: Daily communication and quick questions
- **GitHub Issues**: Feature requests and bug reports
- **Pull Requests**: Code review and discussion
- **Regular Meetings**: Sprint planning and retrospectives

#### Best Practices for Seeking Help
1. **Try to solve first**: Check documentation, search relevant resources
2. **Prepare specific questions**: Include error messages, reproduction steps, expected results
3. **Choose appropriate channel**: Use instant messaging for urgent issues, create Issues for complex problems
4. **Share solutions**: Give back knowledge learned to the team

#### Knowledge Sharing
- **Tech Talks**: Regular sharing of new technologies and best practices
- **Code Reviews**: Learning and teaching through reviews
- **Documentation Contributions**: Improve and update project documentation
- **Mentorship**: Senior developers guide new members

## 🎉 Next Steps After First Contribution

### Celebrate Achievement
Congratulations on completing your first contribution! This is an important milestone.

### Continuous Improvement
- **Reflect on Learning**: Review challenges and gains from the development process
- **Collect Feedback**: Learn improvement points from code reviews
- **Set Goals**: Set more challenging goals for next contribution

### Advanced Contribution Opportunities
- **Feature Development**: Participate in more complex feature implementation
- **Architecture Improvements**: Propose and implement architecture optimizations
- **Performance Optimization**: Identify and resolve performance bottlenecks
- **Mentorship Role**: Help other new members get started

### Professional Development
- **Skill Enhancement**: Deep dive into specific technical areas
- **Certification Exams**: Consider relevant technical certifications
- **Conference Participation**: Attend technical conferences and seminars
- **Open Source Contribution**: Participate in other open source projects

---

**Next Step**: [Coding Standards and Guidelines](coding-standards.md) →

> 💡 **Tip**: Remember, every expert was once a beginner. Don't be afraid to ask questions - the team is happy to help you grow. Continuous learning and practice are key to becoming an excellent developer!

> 🎯 **Goal**: Through this guide, you should be able to independently set up the development environment, understand the project structure, follow development standards, and successfully complete your first code contribution.
