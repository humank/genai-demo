# Contributing to Enterprise E-Commerce Platform

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing to this project.

## 📑 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)
- [Pull Request Process](#pull-request-process)
- [Community](#community)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inspiring community for all. Please be respectful and constructive in your interactions.

### Expected Behavior

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on what is best for the community
- Show empathy towards other community members

### Unacceptable Behavior

- Harassment, discrimination, or offensive comments
- Trolling, insulting, or derogatory comments
- Public or private harassment
- Publishing others' private information

**Report Issues**: yikaikao@gmail.com

---

## Getting Started

### Prerequisites

Before you begin, ensure you have:

- Java 21 or higher
- Gradle 8.x (included via wrapper)
- Docker and Docker Compose
- Node.js 18+ (for CDK)
- Git

### Fork and Clone

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:

```bash
git clone https://github.com/YOUR-USERNAME/genai-demo.git
cd genai-demo
```

3. **Add upstream remote**:

```bash
git remote add upstream https://github.com/ORIGINAL-OWNER/genai-demo.git
```

### Set Up Development Environment

```bash
# Run one-command setup
make dev-setup

# Or manually:
docker-compose up -d              # Start dependencies
./gradlew :app:build              # Build application
make setup-hooks                  # Set up Git hooks
```

**Detailed Setup**: See [Development Setup Guide](docs/development/setup/README.md)

---

## Development Workflow

### 1. Create a Branch

Create a feature branch from `main`:

```bash
git checkout main
git pull upstream main
git checkout -b feature/your-feature-name
```

**Branch Naming Convention**:
- `feature/` - New features
- `fix/` - Bug fixes
- `docs/` - Documentation changes
- `refactor/` - Code refactoring
- `test/` - Test additions or fixes
- `chore/` - Maintenance tasks

### 2. Make Changes

Follow our coding standards and best practices:

- Write clean, readable code
- Follow [Coding Standards](docs/development/coding-standards/README.md)
- Add tests for new functionality
- Update documentation as needed

### 3. Test Your Changes

```bash
# Run unit tests
./gradlew :app:test

# Run BDD tests
./gradlew :app:cucumber

# Check coverage
./gradlew :app:jacocoTestReport

# Run architecture tests
./gradlew :app:test --tests "*ArchitectureTest"

# Run all pre-commit checks
make pre-commit
```

### 4. Commit Your Changes

Follow [Conventional Commits](https://www.conventionalcommits.org/) format:

```bash
git add .
git commit -m "feat(context): add new feature"
```

**Commit Message Format**:
```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Test additions or fixes
- `chore`: Maintenance tasks
- `perf`: Performance improvements

**Examples**:
```
feat(auth): add JWT authentication
fix(api): resolve timeout issue in order endpoint
docs(architecture): update deployment viewpoint
test(customer): add unit tests for customer service
```

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a pull request on GitHub.

---

## Coding Standards

### Java Code Style

We follow the **Google Java Style Guide** with some modifications:

#### Formatting

- **Indentation**: 4 spaces (not tabs)
- **Line Length**: 120 characters maximum
- **Braces**: K&R style (opening brace on same line)

#### Naming Conventions

```java
// Classes: PascalCase
public class CustomerService { }

// Methods: camelCase with verb-noun pattern
public Customer findCustomerById(String id) { }

// Variables: camelCase, descriptive names
private String customerEmail;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRY_ATTEMPTS = 3;

// Packages: lowercase, singular nouns
package solid.humank.genaidemo.domain.customer;
```

#### Code Organization

```java
// Order: static fields, instance fields, constructors, methods
public class Order {
    // 1. Static fields
    private static final Logger logger = LoggerFactory.getLogger(Order.class);
    
    // 2. Instance fields
    private final OrderId id;
    private OrderStatus status;
    
    // 3. Constructors
    public Order(OrderId id) {
        this.id = id;
    }
    
    // 4. Public methods
    public void submit() { }
    
    // 5. Private methods
    private void validate() { }
}
```

### Architecture Patterns

Follow **Domain-Driven Design** and **Hexagonal Architecture** principles:

#### Domain Layer

```java
// Aggregate Root
@AggregateRoot
public class Customer extends AggregateRoot {
    // Business logic here
    public void updateProfile(CustomerName name, Email email) {
        // Validate
        // Update state
        // Collect domain event
        collectEvent(CustomerProfileUpdatedEvent.create(id, name, email));
    }
}

// Value Object (use Records)
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}

// Domain Event (use Records)
public record CustomerCreatedEvent(
    CustomerId customerId,
    CustomerName name,
    Email email,
    UUID eventId,
    LocalDateTime occurredOn
) implements DomainEvent { }
```

#### Application Layer

```java
@Service
@Transactional
public class CustomerApplicationService {
    private final CustomerRepository customerRepository;
    private final DomainEventApplicationService eventService;
    
    public void createCustomer(CreateCustomerCommand command) {
        // 1. Create aggregate
        Customer customer = new Customer(command.name(), command.email());
        
        // 2. Save aggregate
        customerRepository.save(customer);
        
        // 3. Publish events
        eventService.publishEventsFromAggregate(customer);
    }
}
```

**Detailed Standards**: [Coding Standards](docs/development/coding-standards/README.md)

---

## Testing Requirements

### Test Coverage

- **Minimum Coverage**: 80% line coverage
- **Focus**: Business logic and domain model
- **Tools**: JaCoCo for coverage reporting

### Test Types

#### Unit Tests (80% of tests)

```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    @Test
    void should_create_customer_when_valid_data_provided() {
        // Given
        CreateCustomerCommand command = new CreateCustomerCommand("John", "john@example.com");
        
        // When
        Customer customer = customerService.createCustomer(command);
        
        // Then
        assertThat(customer).isNotNull();
        assertThat(customer.getName()).isEqualTo("John");
        verify(customerRepository).save(any(Customer.class));
    }
}
```

#### BDD Tests (Cucumber)

```gherkin
Feature: Customer Registration
  
  Scenario: Successful customer registration
    Given a new customer with valid information
    When they submit the registration form
    Then their account should be created
    And they should receive a welcome email
```

#### Architecture Tests (ArchUnit)

```java
@ArchTest
static final ArchRule domainLayerRules = classes()
    .that().resideInAPackage("..domain..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "java..");
```

### Running Tests

```bash
# Unit tests
./gradlew :app:test

# BDD tests
./gradlew :app:cucumber

# Coverage report
./gradlew :app:jacocoTestReport
# View: build/reports/jacoco/test/html/index.html

# Architecture tests
./gradlew :app:test --tests "*ArchitectureTest"
```

**Detailed Testing Guide**: [Testing Strategy](docs/development/testing/testing-strategy.md)

---

## Documentation

### Documentation Requirements

When making changes, update relevant documentation:

- **Code Changes**: Update inline comments and JavaDoc
- **API Changes**: Update OpenAPI specifications
- **Architecture Changes**: Update relevant viewpoint documentation
- **New Features**: Add to functional viewpoint and user guides

### Documentation Structure

```text
docs/
├── viewpoints/              # Architecture viewpoints
├── perspectives/            # Quality perspectives
├── architecture/            # ADRs and patterns
├── api/                     # API documentation
├── development/             # Developer guides
└── operations/              # Operational procedures
```

### Writing Documentation

Follow our [Documentation Style Guide](docs/STYLE-GUIDE.md):

#### Markdown Standards

- Use ATX-style headers (`#` not `===`)
- One sentence per line for better diffs
- Use relative links for internal references
- Include code examples where appropriate

#### Diagrams

- Use PlantUML for architecture diagrams
- Use Mermaid for simple flow diagrams
- Store diagrams in `docs/diagrams/`
- Generate diagrams: `make diagrams`

#### Examples

```markdown
# Good Documentation

## Overview

This component handles customer authentication using JWT tokens.

## Usage

```java
CustomerService service = new CustomerService(repository);
Customer customer = service.findById("123");
```

## Related Documentation

- [Security Perspective](../perspectives/security/README.md)
- [Authentication Guide](./authentication.md)
```

**Detailed Guide**: [Documentation Style Guide](docs/STYLE-GUIDE.md)

---

## Pull Request Process

### Before Submitting

1. **Run all checks**:
   ```bash
   make pre-commit
   ```

2. **Ensure tests pass**:
   ```bash
   ./gradlew :app:test
   ./gradlew :app:cucumber
   ```

3. **Check coverage**:
   ```bash
   ./gradlew :app:jacocoTestReport
   # Ensure coverage is above 80%
   ```

4. **Update documentation**:
   - Update relevant viewpoint documentation
   - Add/update API documentation
   - Update CHANGELOG.md

### Pull Request Template

When creating a pull request, include:

```markdown
## Description

Brief description of changes

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues

Closes #123

## Testing

- [ ] Unit tests added/updated
- [ ] BDD tests added/updated
- [ ] Manual testing performed

## Documentation

- [ ] Code comments updated
- [ ] API documentation updated
- [ ] Architecture documentation updated

## Checklist

- [ ] Code follows style guidelines
- [ ] Tests pass locally
- [ ] Coverage is above 80%
- [ ] Documentation is updated
- [ ] No breaking changes (or documented)
```

### Review Process

1. **Automated Checks**: CI/CD runs automatically
2. **Code Review**: At least one approval required
3. **Architecture Review**: For significant changes
4. **Merge**: Squash and merge to main

### After Merge

- Delete your feature branch
- Update your local main branch
- Close related issues

---

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Email**: yikaikao@gmail.com

### Getting Help

- Check [FAQ](README.md#-faq)
- Search [existing issues](https://github.com/yourusername/genai-demo/issues)
- Ask in [Discussions](https://github.com/yourusername/genai-demo/discussions)
- Email maintainer: yikaikao@gmail.com

### Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project documentation

---

## Additional Resources

### Documentation

- [Development Guide](docs/development/README.md)
- [Architecture Guide](docs/rozanski-woods-methodology-guide.md)
- [Testing Guide](docs/development/testing/testing-strategy.md)
- [API Documentation](docs/api/README.md)

### External Resources

- [Domain-Driven Design](https://www.domainlanguage.com/ddd/)
- [Rozanski & Woods Methodology](https://www.viewpoints-and-perspectives.info/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)

---

## Questions?

If you have questions about contributing:

- Check this guide and linked documentation
- Search [existing issues](https://github.com/yourusername/genai-demo/issues)
- Ask in [Discussions](https://github.com/yourusername/genai-demo/discussions)
- Email: yikaikao@gmail.com

**Thank you for contributing!** 🎉

---

**Last Updated**: 2024-11-09
