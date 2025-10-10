# Enterprise E-Commerce Platform

> **A Modern Software Architecture Showcase Based on Rozanski & Woods Methodology, Domain-Driven Design, and Behavior-Driven Development**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS CDK](https://img.shields.io/badge/AWS%20CDK-2.x-yellow.svg)](https://aws.amazon.com/cdk/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 🎯 Project Overview

This project demonstrates enterprise-grade software architecture practices through a comprehensive e-commerce platform implementation. It showcases how to design, develop, test, and deploy a production-ready system using industry-leading methodologies and tools.

### Core Design Principles

**🏗️ Architecture-Driven Design**
- **Rozanski & Woods Viewpoints & Perspectives**: Systematic architectural analysis across 7 viewpoints (Functional, Information, Concurrency, Development, Deployment, Operational, Context) and 8 perspectives (Security, Performance, Availability, Evolution, etc.)
- **Hexagonal Architecture**: Clean separation between business logic and infrastructure concerns
- **Event-Driven Architecture**: Asynchronous communication through domain events

**🎯 Domain-Driven Design (DDD)**
- **Strategic Design**: 13 bounded contexts with clear business boundaries
- **Tactical Patterns**: Complete implementation of aggregates, entities, value objects, domain services, and repositories
- **Ubiquitous Language**: Consistent terminology between business and technical teams
- **Event Storming**: Visual modeling from business processes to system design

**📋 Behavior-Driven Development (BDD)**
- **Gherkin Scenarios**: 28+ feature files describing business requirements
- **Acceptance Criteria**: Clear, testable specifications for each feature
- **Living Documentation**: Tests serve as executable specifications
- **Cucumber Integration**: Automated BDD test execution

**🧪 Environment-Specific Testing Strategy**
- **Local Environment**: Unit tests only (fast feedback loop)
- **Staging Environment**: Integration tests with real AWS services
- **Production Environment**: Full end-to-end tests and monitoring

**☁️ Infrastructure as Code (IaC)**
- **AWS CDK**: Complete infrastructure definition in TypeScript
- **Multi-Stack Architecture**: Modular, reusable infrastructure components
- **Multi-Region Support**: Cross-region deployment capabilities
- **GitOps**: Automated deployment through ArgoCD

## 📊 Architecture Overview

### Bounded Contexts

The system is organized into 13 bounded contexts, each representing a distinct business capability:

```
├── Customer Management      # Customer profiles, authentication, membership
├── Product Catalog         # Product information, categories, search
├── Inventory Management    # Stock tracking, warehouse management
├── Order Management        # Order lifecycle, order processing
├── Payment Processing      # Payment methods, transactions, refunds
├── Promotion Engine        # Discounts, coupons, flash sales, bundles
├── Pricing Strategy        # Dynamic pricing, commission rates
├── Shopping Cart           # Cart management, item selection
├── Logistics & Delivery    # Shipping, tracking, delivery management
├── Notification Service    # Email, SMS, push notifications
├── Reward Points          # Loyalty program, points accumulation
├── Analytics & Reporting   # Business intelligence, metrics
└── Workflow Orchestration  # Process coordination, saga patterns
```

### Technology Stack

**Backend**
- **Framework**: Spring Boot 3.3.13 with Java 21
- **Data Access**: Spring Data JPA + Hibernate
- **Database**: PostgreSQL (production), H2 (local/test)
- **Caching**: Redis (staging/production), In-memory (local)
- **Messaging**: Apache Kafka (MSK in production)
- **API Documentation**: SpringDoc OpenAPI 3 + Swagger UI

**Testing**
- **Unit Testing**: JUnit 5 + Mockito + AssertJ
- **BDD Testing**: Cucumber 7 with Gherkin
- **Architecture Testing**: ArchUnit
- **Performance Testing**: Custom test performance framework
- **Coverage**: JaCoCo (target: 80%+)

**Infrastructure**
- **Cloud Provider**: AWS
- **IaC Tool**: AWS CDK (TypeScript)
- **Container Orchestration**: Amazon EKS
- **Service Mesh**: AWS App Mesh
- **CI/CD**: GitHub Actions + ArgoCD

**Observability**
- **Metrics**: Spring Boot Actuator + Prometheus + CloudWatch
- **Logging**: Structured logging with correlation IDs
- **Tracing**: AWS X-Ray for distributed tracing
- **Monitoring**: Amazon Managed Grafana
- **Alerting**: CloudWatch Alarms + SNS

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Gradle 8.x** (included via wrapper)
- **Docker** and Docker Compose
- **Node.js 18+** (for CDK)
- **AWS CLI** (for cloud deployment)

### Local Development Setup

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/genai-demo.git
cd genai-demo
```

2. **Start local dependencies**
```bash
# Start PostgreSQL and Redis
docker-compose up -d
```

3. **Run the application**
```bash
# Run with local profile (unit tests only)
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

4. **Access the application**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator

### Running Tests

```bash
# Unit tests only (local environment)
./gradlew :app:test

# Run specific test categories
./gradlew :app:test --tests "*UnitTest"

# Run BDD tests
./gradlew :app:cucumber

# Generate coverage report
./gradlew :app:jacocoTestReport

# Architecture compliance tests
./gradlew :app:test --tests "*ArchitectureTest"
```

### Integration Testing (Staging)

Integration tests run against real AWS services in the staging environment:

```bash
# Deploy to staging
cd infrastructure
npm run deploy:staging

# Run integration tests
cd ../staging-tests
./gradlew test
```

## 🏗️ Project Structure

```
.
├── app/                          # Main application
│   ├── src/main/java/
│   │   └── solid/humank/genaidemo/
│   │       ├── application/      # Application services (use cases)
│   │       ├── domain/           # Domain model (DDD)
│   │       │   ├── customer/     # Customer bounded context
│   │       │   ├── order/        # Order bounded context
│   │       │   ├── product/      # Product bounded context
│   │       │   └── ...           # Other bounded contexts
│   │       └── infrastructure/   # Infrastructure adapters
│   │           ├── persistence/  # Database repositories
│   │           ├── messaging/    # Event publishers
│   │           ├── security/     # Security configuration
│   │           └── observability/# Metrics, logging, tracing
│   └── src/test/
│       ├── java/                 # Unit tests
│       └── resources/features/   # BDD feature files
│
├── infrastructure/               # AWS CDK infrastructure
│   ├── lib/stacks/              # CDK stack definitions
│   │   ├── network-stack.ts     # VPC, subnets, security groups
│   │   ├── eks-stack.ts         # Kubernetes cluster
│   │   ├── rds-stack.ts         # PostgreSQL database
│   │   ├── msk-stack.ts         # Kafka cluster
│   │   ├── observability-stack.ts # Monitoring setup
│   │   └── ...                  # Other infrastructure stacks
│   └── test/                    # Infrastructure tests
│
├── staging-tests/               # Integration tests for staging
├── cmc-frontend/                # Customer management console (Next.js)
├── consumer-frontend/           # Consumer app (Angular)
├── .kiro/                       # Kiro AI assistant configuration
│   ├── hooks/                   # Automated quality checks
│   └── steering/                # Development standards
└── docs/                        # Documentation (empty, to be populated)
```

## 🧪 Testing Strategy

### Test Pyramid

```
        /\
       /  \  E2E Tests (5%)
      /____\  - Production environment
     /      \  - Full user journeys
    / Integ. \ Integration Tests (15%)
   /__________\ - Staging environment
  /            \ - Real AWS services
 /  Unit Tests  \ Unit Tests (80%)
/________________\ - Local environment
                   - Fast feedback
```

### Environment-Specific Testing

| Environment | Test Type | Scope | Infrastructure |
|-------------|-----------|-------|----------------|
| **Local** | Unit Tests | Business logic, domain model | H2, In-memory |
| **Staging** | Integration Tests | Service integration, AWS services | RDS, MSK, ElastiCache |
| **Production** | E2E Tests | Complete user journeys, monitoring | Full AWS stack |

### BDD Feature Coverage

28+ feature files covering:
- Customer management and membership
- Product catalog and search
- Shopping cart operations
- Order processing workflow
- Payment processing
- Promotion engine (coupons, flash sales, bundles)
- Logistics and delivery
- Notification system
- Reward points program

## ☁️ AWS Deployment

### Infrastructure Components

The application deploys to AWS using CDK with the following components:

**Networking**
- VPC with public/private subnets across 3 AZs
- NAT Gateways for private subnet internet access
- Security groups with least-privilege access

**Compute**
- Amazon EKS cluster for container orchestration
- Auto-scaling node groups
- Fargate profiles for serverless pods

**Data**
- Amazon RDS PostgreSQL (Multi-AZ)
- Amazon ElastiCache Redis (cluster mode)
- Amazon MSK (Managed Kafka)

**Observability**
- Amazon CloudWatch for metrics and logs
- AWS X-Ray for distributed tracing
- Amazon Managed Grafana for dashboards
- CloudWatch Alarms for alerting

**Security**
- AWS IAM roles and policies
- AWS Secrets Manager for credentials
- AWS Certificate Manager for TLS
- AWS WAF for application firewall

### Deployment Process

```bash
# Install dependencies
cd infrastructure
npm install

# Bootstrap CDK (first time only)
npx cdk bootstrap aws://ACCOUNT-ID/REGION

# Deploy to staging
npm run deploy:staging

# Deploy to production
npm run deploy:production

# Destroy infrastructure
npm run destroy:staging
```

### Multi-Region Deployment

The infrastructure supports multi-region deployment for disaster recovery:

```bash
# Deploy to primary region (us-east-1)
AWS_REGION=us-east-1 npm run deploy:production

# Deploy to secondary region (us-west-2)
AWS_REGION=us-west-2 npm run deploy:production
```

## 📈 Observability

### Metrics

**Business Metrics**
- Order conversion rate
- Average order value
- Customer lifetime value
- Cart abandonment rate

**Technical Metrics**
- API response times (p50, p95, p99)
- Error rates by endpoint
- Database query performance
- Cache hit rates
- Message queue lag

### Logging

Structured logging with:
- Correlation IDs for request tracing
- Sensitive data masking
- Log levels by environment
- Centralized log aggregation in CloudWatch

### Tracing

AWS X-Ray integration provides:
- End-to-end request tracing
- Service dependency maps
- Performance bottleneck identification
- Error root cause analysis

### Dashboards

Pre-configured Grafana dashboards for:
- Application performance overview
- Infrastructure health
- Business KPIs
- Cost optimization metrics

## 🔒 Security

### Security Measures

- **Authentication**: JWT-based authentication
- **Authorization**: Role-based access control (RBAC)
- **Data Encryption**: TLS in transit, AES-256 at rest
- **Secrets Management**: AWS Secrets Manager
- **Network Security**: Private subnets, security groups, NACLs
- **Compliance**: GDPR-ready data handling

### Security Testing

- Static code analysis with SonarQube
- Dependency vulnerability scanning
- Infrastructure security with CDK Nag
- Penetration testing in staging

## 📚 Documentation

### Architecture Documentation

Following Rozanski & Woods methodology, documentation is organized by viewpoints:

- **Functional Viewpoint**: Business capabilities and use cases
- **Information Viewpoint**: Data models and event flows
- **Concurrency Viewpoint**: Asynchronous processing patterns
- **Development Viewpoint**: Code organization and build process
- **Deployment Viewpoint**: Infrastructure and deployment strategy
- **Operational Viewpoint**: Monitoring, logging, and incident response
- **Context Viewpoint**: External integrations and system boundaries

### API Documentation

- **OpenAPI 3.0**: Complete API specification
- **Swagger UI**: Interactive API explorer
- **Postman Collection**: Pre-configured API requests

## 🤝 Contributing

This project follows strict development standards:

1. **Code Style**: Follow Google Java Style Guide
2. **Testing**: Maintain 80%+ code coverage
3. **BDD**: Write Gherkin scenarios before implementation
4. **Architecture**: Comply with ArchUnit rules
5. **Documentation**: Update relevant viewpoint documentation

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Rozanski & Woods**: Software Systems Architecture methodology
- **Eric Evans**: Domain-Driven Design principles
- **Martin Fowler**: Enterprise architecture patterns
- **AWS**: Cloud infrastructure and services

---

**Built with ❤️ using modern software engineering practices**
