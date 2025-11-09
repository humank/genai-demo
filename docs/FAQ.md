# Frequently Asked Questions (FAQ)

This document answers common questions about the Enterprise E-Commerce Platform project.

## 📑 Table of Contents

- [General Questions](#general-questions)
- [Architecture & Design](#architecture--design)
- [Development](#development)
- [Testing](#testing)
- [Deployment & Operations](#deployment--operations)
- [Troubleshooting](#troubleshooting)

---

## General Questions

### What is this project about?

This project demonstrates enterprise-grade software architecture through a comprehensive e-commerce platform. It showcases:

- **Rozanski & Woods** methodology (7 Viewpoints + 8 Perspectives)
- **Domain-Driven Design** with 13 bounded contexts
- **Behavior-Driven Development** with Cucumber
- **Cloud-native architecture** on AWS

**Learn More**: [Project Overview](../README.md#-project-overview)

---

### Who is this project for?

This project is designed for:

- **Software Architects**: Learn systematic architecture design
- **Developers**: Study enterprise patterns and best practices
- **Students**: Understand real-world software architecture
- **Teams**: Use as a reference architecture for e-commerce systems

---

### What makes this project different?

Key differentiators:

1. **Complete Architecture Documentation**: Full implementation of Rozanski & Woods methodology
2. **Production-Ready**: Not just a demo, but production-grade code
3. **Comprehensive Testing**: 80%+ coverage with unit, integration, and BDD tests
4. **Cloud-Native**: Full AWS infrastructure with CDK
5. **Living Documentation**: Tests serve as executable specifications

---

## Architecture & Design

### Why use Rozanski & Woods methodology?

**Benefits**:

- **Systematic Analysis**: 7 viewpoints provide complete system understanding
- **Quality Focus**: 8 perspectives ensure quality attributes are addressed
- **Stakeholder Communication**: Common language for discussing architecture
- **Industry Standard**: Widely adopted in enterprise architecture

**Viewpoints** describe system structure (what and how):
- Context, Functional, Information, Concurrency
- Development, Deployment, Operational

**Perspectives** describe quality attributes (cross-cutting concerns):
- Security, Performance, Availability, Evolution
- Accessibility, Development Resource, i18n, Location

**Learn More**: [Rozanski & Woods Guide](rozanski-woods-methodology-guide.md)

---

### What's the difference between viewpoints and perspectives?

**Viewpoints** = System Structure
- Describe **WHAT** the system is and **HOW** it's organized
- Example: Functional Viewpoint shows business capabilities

**Perspectives** = Quality Attributes
- Describe **quality concerns** that affect the entire system
- Example: Security Perspective shows authentication across all viewpoints

**Analogy**: 
- Viewpoints = Different camera angles of a building
- Perspectives = Quality lenses (safety, energy efficiency, accessibility)

**Learn More**: [Architecture Methodology](rozanski-woods-methodology-guide.md)

---

### Why Domain-Driven Design (DDD)?

**Benefits**:

- **Business Alignment**: Code reflects business domain
- **Bounded Contexts**: Clear boundaries reduce complexity
- **Ubiquitous Language**: Common terminology between business and tech
- **Strategic Design**: Helps manage large, complex systems

**Our Implementation**:
- 13 bounded contexts (Customer, Order, Product, etc.)
- Complete tactical patterns (Aggregates, Entities, Value Objects)
- Event-driven communication between contexts

**Learn More**: [Functional Viewpoint](viewpoints/functional/README.md)

---

### Why Hexagonal Architecture?

**Benefits**:

- **Testability**: Business logic isolated from infrastructure
- **Flexibility**: Easy to swap infrastructure components
- **Maintainability**: Clear separation of concerns
- **Domain Focus**: Business logic is the center

**Structure**:
```
Domain (Core) ← Application ← Infrastructure
                            ← Interfaces
```

**Learn More**: [Development Viewpoint](viewpoints/development/README.md)

---

## Development

### Can I run this without AWS?

**Yes!** Use the `local` profile:

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

**Local Profile Uses**:
- H2 in-memory database (instead of PostgreSQL)
- In-memory cache (instead of Redis)
- In-memory message broker (instead of Kafka)

**Perfect for**:
- Development
- Unit testing
- Quick experimentation

**Learn More**: [Local Development Setup](development/setup/local-environment.md)

---

### How do I add a new bounded context?

**Steps**:

1. **Define Boundaries**: Identify business capabilities
2. **Create Domain Model**: Aggregates, entities, value objects
3. **Implement Repository**: Data access interface
4. **Add Application Service**: Use case orchestration
5. **Create Infrastructure**: Repository implementation
6. **Add Domain Events**: For cross-context communication
7. **Write Tests**: Unit, integration, and BDD tests
8. **Update Documentation**: Functional viewpoint

**Learn More**: [DDD Implementation Guide](development/ddd-implementation-guide.md)

---

### What coding standards should I follow?

**Key Standards**:

- **Style**: Google Java Style Guide
- **Naming**: PascalCase for classes, camelCase for methods
- **Architecture**: Hexagonal + DDD patterns
- **Testing**: 80%+ coverage required
- **Documentation**: JavaDoc for public APIs

**Tools**:
- Checkstyle for style enforcement
- ArchUnit for architecture rules
- JaCoCo for coverage

**Learn More**: [Coding Standards](development/coding-standards/README.md)

---

### How do I set up my IDE?

**Supported IDEs**:
- IntelliJ IDEA (recommended)
- Eclipse
- VS Code

**Setup Steps**:

1. Import as Gradle project
2. Install required plugins
3. Configure code style
4. Set up run configurations

**Learn More**: [IDE Configuration](development/setup/ide-configuration.md)

---

## Testing

### What's the testing strategy?

**Test Pyramid**:

```
     /\
    /E2E\     5% - Production environment
   /____\
  /Integ.\   15% - Staging environment
 /________\
/   Unit   \ 80% - Local environment
/___________\
```

**Environment-Specific**:
- **Local**: Unit tests only (fast feedback)
- **Staging**: Integration tests with real AWS services
- **Production**: E2E tests and monitoring

**Learn More**: [Testing Strategy](development/testing/testing-strategy.md)

---

### How do I run tests?

**Unit Tests**:
```bash
./gradlew :app:test
```

**BDD Tests**:
```bash
./gradlew :app:cucumber
```

**Coverage Report**:
```bash
./gradlew :app:jacocoTestReport
# View: build/reports/jacoco/test/html/index.html
```

**Architecture Tests**:
```bash
./gradlew :app:test --tests "*ArchitectureTest"
```

**All Pre-commit Checks**:
```bash
make pre-commit
```

**Learn More**: [Testing Guide](development/testing/README.md)

---

### How do I write BDD tests?

**Steps**:

1. **Write Gherkin Scenario**:
```gherkin
Feature: Customer Registration
  
  Scenario: Successful registration
    Given a new customer with valid information
    When they submit the registration form
    Then their account should be created
    And they should receive a welcome email
```

2. **Implement Step Definitions**:
```java
@Given("a new customer with valid information")
public void aNewCustomerWithValidInformation() {
    // Setup test data
}
```

3. **Run Tests**:
```bash
./gradlew :app:cucumber
```

**Learn More**: [BDD Testing Guide](development/testing/bdd-testing.md)

---

### Why is test coverage important?

**Benefits**:

- **Confidence**: Safe refactoring
- **Documentation**: Tests show how code works
- **Quality**: Catches bugs early
- **Maintainability**: Easier to change code

**Our Target**: 80%+ line coverage

**Focus Areas**:
- Business logic (domain layer)
- Application services
- Critical paths

**Learn More**: [Testing Strategy](development/testing/testing-strategy.md)

---

## Deployment & Operations

### How do I deploy to AWS?

**Prerequisites**:
- AWS account
- AWS CLI configured
- Node.js 18+ (for CDK)

**Steps**:

1. **Install Dependencies**:
```bash
cd infrastructure
npm install
```

2. **Bootstrap CDK** (first time only):
```bash
npx cdk bootstrap aws://ACCOUNT-ID/REGION
```

3. **Deploy to Staging**:
```bash
npm run deploy:staging
```

4. **Deploy to Production**:
```bash
npm run deploy:production
```

**Learn More**: [Deployment Guide](operations/deployment/README.md)

---

### What AWS services are used?

**Core Services**:
- **EKS**: Kubernetes orchestration
- **RDS**: PostgreSQL database
- **MSK**: Managed Kafka
- **ElastiCache**: Redis cache
- **CloudWatch**: Monitoring and logging
- **X-Ray**: Distributed tracing

**Supporting Services**:
- VPC, Security Groups, IAM
- Secrets Manager, Certificate Manager
- Route 53, CloudFront, S3

**Learn More**: [Deployment Viewpoint](viewpoints/deployment/README.md)

---

### How do I monitor the application?

**Monitoring Stack**:

- **Metrics**: CloudWatch + Prometheus
- **Logging**: Structured logs in CloudWatch
- **Tracing**: AWS X-Ray for distributed tracing
- **Dashboards**: Amazon Managed Grafana
- **Alerts**: CloudWatch Alarms + SNS

**Key Metrics**:
- API response times (p50, p95, p99)
- Error rates by endpoint
- Database query performance
- Cache hit rates
- Business metrics (orders, revenue)

**Learn More**: [Monitoring Guide](operations/monitoring/monitoring-strategy.md)

---

### What if something goes wrong in production?

**Incident Response**:

1. **Check Runbooks**: [Operations Runbooks](operations/runbooks/README.md)
2. **Review Dashboards**: Grafana dashboards
3. **Check Logs**: CloudWatch Logs
4. **Follow Procedures**: Incident response runbook

**Common Issues**:
- [Troubleshooting Guide](operations/troubleshooting/common-issues.md)
- [Debugging Guide](operations/troubleshooting/debugging-guide.md)

**Get Help**: yikaikao@gmail.com

---

## Troubleshooting

### Application won't start

**Common Causes**:

1. **Port Already in Use**:
```bash
# Check what's using port 8080
lsof -i :8080
# Kill the process
kill -9 <PID>
```

2. **Database Not Running**:
```bash
# Start Docker services
docker-compose up -d
```

3. **Missing Dependencies**:
```bash
# Clean and rebuild
./gradlew clean build
```

**Learn More**: [Troubleshooting Guide](operations/troubleshooting/common-issues.md)

---

### Tests are failing

**Common Causes**:

1. **Outdated Dependencies**:
```bash
./gradlew clean build --refresh-dependencies
```

2. **Database State Issues**:
```bash
# Reset database
docker-compose down -v
docker-compose up -d
```

3. **Architecture Rule Violations**:
```bash
# Check ArchUnit tests
./gradlew :app:test --tests "*ArchitectureTest"
```

**Learn More**: [Testing Troubleshooting](development/testing/troubleshooting.md)

---

### Diagrams won't generate

**Common Causes**:

1. **PlantUML Syntax Error**:
```bash
# Validate diagrams
make validate
```

2. **Missing PlantUML**:
```bash
# Install PlantUML
brew install plantuml  # macOS
# or download from https://plantuml.com/
```

3. **Broken References**:
```bash
# Check cross-references
./scripts/validate-cross-references.py
```

**Learn More**: [Diagram Generation Guide](diagrams/README.md)

---

### How do I get help?

**Resources**:

1. **Documentation**: Check [docs/](../README.md#-documentation)
2. **This FAQ**: Search this document
3. **GitHub Issues**: [Search existing issues](https://github.com/yourusername/genai-demo/issues)
4. **Discussions**: [Ask in Discussions](https://github.com/yourusername/genai-demo/discussions)
5. **Email**: yikaikao@gmail.com

**Before Asking**:
- Search documentation
- Check existing issues
- Try troubleshooting guides
- Provide error messages and logs

---

## Additional Questions

### Where can I find API documentation?

**Multiple Formats**:

- **Interactive**: Swagger UI at http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- **Documentation**: [API Documentation](api/README.md)
- **Postman**: Collection (coming soon)

---

### How do I contribute?

**Quick Steps**:

1. Fork the repository
2. Create a feature branch
3. Make changes following [Coding Standards](development/coding-standards/README.md)
4. Write tests (80%+ coverage)
5. Run `make pre-commit`
6. Submit pull request

**Learn More**: [Contributing Guide](../CONTRIBUTING.md)

---

### Is this production-ready?

**Yes!** This project includes:

- ✅ Comprehensive testing (80%+ coverage)
- ✅ Security best practices
- ✅ Monitoring and observability
- ✅ Disaster recovery procedures
- ✅ Operational runbooks
- ✅ CI/CD pipeline
- ✅ Infrastructure as Code

**However**: Review and adapt to your specific requirements before production use.

---

### Can I use this for my project?

**Yes!** This project is MIT licensed.

**You can**:
- Use as reference architecture
- Copy and modify code
- Use in commercial projects
- Learn from the implementation

**Please**:
- Give credit where appropriate
- Share improvements back (optional)
- Follow the license terms

**Learn More**: [LICENSE](../LICENSE)

---

### How do I stay updated?

**Ways to Stay Informed**:

- ⭐ Star the repository on GitHub
- 👀 Watch for releases
- 📧 Subscribe to discussions
- 📰 Check [CHANGELOG.md](../CHANGELOG.md)

---

## Still Have Questions?

If your question isn't answered here:

1. **Search Documentation**: [docs/README.md](README.md)
2. **Check Issues**: [GitHub Issues](https://github.com/yourusername/genai-demo/issues)
3. **Ask Community**: [GitHub Discussions](https://github.com/yourusername/genai-demo/discussions)
4. **Email Maintainer**: yikaikao@gmail.com

**We're here to help!** 🤝

---

**Last Updated**: 2024-11-09  
**Maintainer**: yikaikao@gmail.com
