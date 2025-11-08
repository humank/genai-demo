# Enterprise E-Commerce Platform

> **A Modern Software Architecture Showcase Based on Rozanski & Woods Methodology, Domain-Driven Design, and Behavior-Driven Development**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS CDK](https://img.shields.io/badge/AWS%20CDK-2.x-yellow.svg)](https://aws.amazon.com/cdk/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📑 Table of Contents

- [🎯 Project Overview](#-project-overview)
- [🏛️ Architecture Methodology: Rozanski & Woods](#️-architecture-methodology-rozanski--woods)
  - [📐 Architecture Viewpoints](#-architecture-viewpoints-system-structure)
  - [🎯 Quality Perspectives](#-quality-perspectives-cross-cutting-concerns)
  - [🗺️ Quick Navigation Guide](#️-quick-navigation-guide)
- [📊 Domain Model: Bounded Contexts](#-domain-model-bounded-contexts)
- [🛠️ Technology Stack](#️-technology-stack)
- [🚀 Quick Start](#-quick-start)
- [🏗️ Project Structure](#️-project-structure)
- [🧪 Testing Strategy](#-testing-strategy)
- [☁️ AWS Deployment](#️-aws-deployment)
- [📈 Observability](#-observability)
- [📚 Documentation Structure](#-documentation-structure)

---

## 🎯 Project Overview

This project demonstrates enterprise-grade software architecture practices through a comprehensive e-commerce platform implementation. It showcases how to design, develop, test, and deploy a production-ready system using industry-leading methodologies and tools.

> **🏛️ Architecture First**: This project emphasizes **architecture-driven development** using the Rozanski & Woods methodology. See [Architecture Methodology](#️-architecture-methodology-rozanski--woods) for details.

### Core Design Principles

**🏗️ Architecture-Driven Design (Rozanski & Woods)**

This project is built on the **Rozanski & Woods Software Systems Architecture** methodology, providing:

- **7 Viewpoints** for systematic structural analysis:
  - [Context](docs/viewpoints/context/README.md), [Functional](docs/viewpoints/functional/README.md), [Information](docs/viewpoints/information/README.md), [Concurrency](docs/viewpoints/concurrency/README.md)
  - [Development](docs/viewpoints/development/README.md), [Deployment](docs/viewpoints/deployment/README.md), [Operational](docs/viewpoints/operational/README.md)

- **8 Perspectives** for quality attribute analysis:
  - [Security](docs/perspectives/security/README.md), [Performance](docs/perspectives/performance/README.md), [Availability](docs/perspectives/availability/README.md), [Evolution](docs/perspectives/evolution/README.md)
  - [Accessibility](docs/perspectives/accessibility/README.md), [Development Resource](docs/perspectives/development-resource/README.md), [i18n](docs/perspectives/internationalization/README.md), [Location](docs/perspectives/location/README.md)

- **Hexagonal Architecture**: Clean separation between business logic and infrastructure concerns
- **Event-Driven Architecture**: Asynchronous communication through domain events

📖 **Learn More**: [Complete Methodology Guide](docs/rozanski-woods-methodology-guide.md)

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

## 🏛️ Architecture Methodology: Rozanski & Woods

This project follows the **Rozanski & Woods Software Systems Architecture** methodology, providing systematic architectural analysis through **7 Viewpoints** (system structure) and **8 Perspectives** (quality attributes).

### 📐 Architecture Viewpoints (System Structure)

Viewpoints describe **WHAT** the system is and **HOW** it's organized:

```text
┌─────────────────────────────────────────────────────────────────┐
│                    CONTEXT VIEWPOINT                            │
│              (System Boundaries & External Relations)           │
└────────────────────────┬────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌──────▼──────┐  ┌────▼──────────┐
│  FUNCTIONAL  │  │ INFORMATION │  │  CONCURRENCY  │
│  (Business   │  │  (Data &    │  │  (Parallel    │
│  Capabilities)│  │   Events)   │  │  Processing)  │
└───────┬──────┘  └──────┬──────┘  └────┬──────────┘
        │                │                │
        └────────────────┼────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
┌───────▼──────┐  ┌──────▼──────┐  ┌────▼──────────┐
│ DEVELOPMENT  │  │ DEPLOYMENT  │  │  OPERATIONAL  │
│ (Code &      │  │ (Infra &    │  │  (Monitoring  │
│  Build)      │  │  Scaling)   │  │  & Support)   │
└──────────────┘  └─────────────┘  └───────────────┘
```

| Viewpoint | Purpose | Documentation |
|-----------|---------|---------------|
| **[Context](docs/viewpoints/context/README.md)** | System boundaries, external integrations, stakeholders | [📄 View Docs](docs/viewpoints/context/README.md) |
| **[Functional](docs/viewpoints/functional/README.md)** | Business capabilities, use cases, bounded contexts | [📄 View Docs](docs/viewpoints/functional/README.md) |
| **[Information](docs/viewpoints/information/README.md)** | Data models, event flows, data ownership | [📄 View Docs](docs/viewpoints/information/README.md) |
| **[Concurrency](docs/viewpoints/concurrency/README.md)** | Asynchronous processing, event-driven patterns | [📄 View Docs](docs/viewpoints/concurrency/README.md) |
| **[Development](docs/viewpoints/development/README.md)** | Code organization, build process, module dependencies | [📄 View Docs](docs/viewpoints/development/README.md) |
| **[Deployment](docs/viewpoints/deployment/README.md)** | Infrastructure, AWS services, scaling strategy | [📄 View Docs](docs/viewpoints/deployment/README.md) |
| **[Operational](docs/viewpoints/operational/README.md)** | Monitoring, logging, incident response, maintenance | [📄 View Docs](docs/viewpoints/operational/README.md) |

### 🎯 Quality Perspectives (Cross-Cutting Concerns)

Perspectives describe **quality attributes** that affect the entire system:

```text
┌─────────────────────────────────────────────────────────────────┐
│                    ALL VIEWPOINTS                               │
│  (Functional, Information, Concurrency, Development,            │
│   Deployment, Operational, Context)                             │
└────────────────────────┬────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌─────▼─────┐   ┌───▼────────┐
    │Security │    │Performance│   │Availability│
    │         │    │& Scaling  │   │& Resilience│
    └────┬────┘    └─────┬─────┘   └───┬────────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌─────▼─────┐   ┌───▼────────┐
    │Evolution│    │Accessibility   │Development │
    │         │    │           │   │Resource    │
    └────┬────┘    └─────┬─────┘   └───┬────────┘
         │               │               │
         └───────────────┼───────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
    ┌────▼────────┐              ┌───────▼────┐
    │i18n         │              │Location    │
    │             │              │            │
    └─────────────┘              └────────────┘
```

| Perspective | Key Concerns | Documentation |
|-------------|--------------|---------------|
| **[Security](docs/perspectives/security/README.md)** | Authentication, authorization, encryption, compliance | [📄 View Docs](docs/perspectives/security/README.md) |
| **[Performance & Scalability](docs/perspectives/performance/README.md)** | Response times, throughput, horizontal scaling | [📄 View Docs](docs/perspectives/performance/README.md) |
| **[Availability & Resilience](docs/perspectives/availability/README.md)** | High availability, disaster recovery, fault tolerance | [📄 View Docs](docs/perspectives/availability/README.md) |
| **[Evolution](docs/perspectives/evolution/README.md)** | Extensibility, maintainability, technology evolution | [📄 View Docs](docs/perspectives/evolution/README.md) |
| **[Accessibility](docs/perspectives/accessibility/README.md)** | UI accessibility, API usability, documentation | [📄 View Docs](docs/perspectives/accessibility/README.md) |
| **[Development Resource](docs/perspectives/development-resource/README.md)** | Team structure, skills, tools, productivity | [📄 View Docs](docs/perspectives/development-resource/README.md) |
| **[Internationalization](docs/perspectives/internationalization/README.md)** | Multi-language support, localization | [📄 View Docs](docs/perspectives/internationalization/README.md) |
| **[Location](docs/perspectives/location/README.md)** | Geographic distribution, data residency, latency | [📄 View Docs](docs/perspectives/location/README.md) |

### 🗺️ Quick Navigation Guide

**For New Team Members:**

1. Start with [Context Viewpoint](docs/viewpoints/context/README.md) → Understand system boundaries
2. Read [Functional Viewpoint](docs/viewpoints/functional/README.md) → Learn what the system does
3. Review [Development Viewpoint](docs/viewpoints/development/README.md) → Understand code organization

**For Architects:**

- Review all [Viewpoints](docs/viewpoints/README.md) for complete system understanding
- Check [Perspectives](docs/perspectives/README.md) for quality attributes
- Review [Architecture Decisions (ADRs)](docs/architecture/adrs/README.md) for design rationale

**For Developers:**

- Focus on [Development Viewpoint](docs/viewpoints/development/README.md) for code structure
- Review [Functional Viewpoint](docs/viewpoints/functional/README.md) for business logic
- Check [Information Viewpoint](docs/viewpoints/information/README.md) for data models

**For Operations:**

- Focus on [Deployment Viewpoint](docs/viewpoints/deployment/README.md) for infrastructure
- Review [Operational Viewpoint](docs/viewpoints/operational/README.md) for procedures
- Check [Operations Runbooks](docs/operations/runbooks/README.md) for incident response

## 📊 Domain Model: Bounded Contexts

The system is organized into **13 bounded contexts** following Domain-Driven Design principles:

```text
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

**📖 Detailed Documentation:** See [Functional Viewpoint](docs/viewpoints/functional/README.md) for complete bounded context descriptions and interactions.

## 🛠️ Technology Stack

### Architecture & Design

- **Architecture Methodology**: Rozanski & Woods (7 Viewpoints + 8 Perspectives)
- **Architecture Patterns**: Hexagonal Architecture, Event-Driven Architecture, CQRS
- **Domain Modeling**: Domain-Driven Design (DDD), Event Storming
- **Documentation**: PlantUML, Mermaid, Architecture Decision Records (ADRs)
- **Architecture Testing**: ArchUnit for enforcing architectural rules

### Backend

- **Framework**: Spring Boot 3.3.13 with Java 21
- **Data Access**: Spring Data JPA + Hibernate
- **Database**: PostgreSQL (production), H2 (local/test)
- **Caching**: Redis (staging/production), In-memory (local)
- **Messaging**: Apache Kafka (MSK in production)
- **API Documentation**: SpringDoc OpenAPI 3 + Swagger UI

### Testing

- **Unit Testing**: JUnit 5 + Mockito + AssertJ
- **BDD Testing**: Cucumber 7 with Gherkin
- **Architecture Testing**: ArchUnit
- **Performance Testing**: Custom test performance framework
- **Coverage**: JaCoCo (target: 80%+)

### Infrastructure

- **Cloud Provider**: AWS
- **IaC Tool**: AWS CDK (TypeScript)
- **Container Orchestration**: Amazon EKS
- **Service Mesh**: AWS App Mesh
- **CI/CD**: GitHub Actions + ArgoCD

### Observability

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

1. **Start local dependencies**

```bash
# Start PostgreSQL and Redis
docker-compose up -d
```

1. **Run the application**

```bash
# Run with local profile (unit tests only)
./gradlew :app:bootRun --args='--spring.profiles.active=local'
```

1. **Access the application**

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Actuator: <http://localhost:8080/actuator>

### Development Commands

We provide convenient `make` commands for common development tasks:

```bash
# View all available commands
make help

# 📊 Diagram Commands
make validate         # Validate all diagrams and references
make generate         # Generate all diagrams from PlantUML sources
make diagrams         # Validate and generate diagrams (combined)

# 🔧 Development Setup
make dev-setup        # Complete development environment setup
make setup-hooks      # Set up Git hooks for validation
make clean-hooks      # Remove Git hooks
make status           # Check project status (hooks, diagrams, etc.)

# ✅ Pre-commit Checks
make pre-commit       # Run all pre-commit validations
```

**Quick Examples:**

```bash
# Before committing changes
make pre-commit       # Validates diagrams and generates missing ones

# Check current project status
make status           # Shows hooks status and diagram counts

# Set up development environment (first time)
make dev-setup        # Sets up Git hooks and validates setup
```

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

```text
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

## 🛠️ Development Workflow

### Make Commands Reference

The project includes a comprehensive `Makefile` with convenient commands for common development tasks. Run `make help` to see all available commands.

#### Diagram Management

```bash
# Validate diagram references and syntax
make validate

# Generate PNG diagrams from PlantUML sources
make generate

# Validate and generate (combined operation)
make diagrams

# Validate specific diagram
make validate-diagram FILE=docs/diagrams/viewpoints/system-context.puml

# Generate specific diagram
make generate-diagram FILE=docs/diagrams/viewpoints/system-context.puml
```

#### Development Setup

```bash
# Complete development environment setup
# - Sets up Git hooks
# - Validates configuration
# - Shows next steps
make dev-setup

# Set up Git hooks for automatic validation
# - Pre-commit: Validates diagram references
# - Commit-msg: Validates commit message format
# - Pre-push: Comprehensive validation and generation
make setup-hooks

# Remove Git hooks
make clean-hooks

# Check project status
# - Shows Git hooks status
# - Shows diagram counts
# - Suggests quick actions
make status
```

#### Pre-commit Workflow

```bash
# Run all pre-commit checks
# - Validates all diagrams
# - Generates missing diagrams
# - Ensures everything is ready to commit
make pre-commit
```

#### Maintenance Commands

```bash
# Clean generated diagram files (use with caution!)
make clean-generated

# View all available commands with descriptions
make help
```

### Git Hooks

The project uses Git hooks to maintain code quality. Set them up with:

```bash
make setup-hooks
```

This creates three hooks:

1. **Pre-commit Hook**: Validates diagram references before commit
2. **Commit Message Hook**: Ensures commit messages follow conventional format
3. **Pre-push Hook**: Runs comprehensive validation before push

**Commit Message Format:**

```text
<type>(<scope>): <description>

Types: feat, fix, docs, style, refactor, test, chore, perf
Examples:
  feat(auth): add user authentication
  fix(api): resolve timeout issue
  docs(diagrams): update system context diagram
```

**Bypassing Hooks** (when necessary):

```bash
git commit --no-verify    # Skip pre-commit and commit-msg hooks
git push --no-verify      # Skip pre-push hook
```

### Automated Validation (CI/CD)

GitHub Actions automatically validates:

- ✅ PlantUML diagram syntax
- ✅ Diagram references in documentation
- ✅ Documentation structure
- ✅ Markdown linting
- ✅ Hook configuration

See `.github/workflows/validate-documentation.yml` for details.

## 🧪 Testing Strategy

### Test Pyramid

```text
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

## 📚 Documentation Structure

Our documentation follows the **Rozanski & Woods** methodology with clear separation between structure and quality:

### 📐 Architecture Documentation

```text
docs/
├── viewpoints/              # System Structure (7 Viewpoints)
│   ├── context/            # System boundaries & external relations
│   ├── functional/         # Business capabilities & use cases
│   ├── information/        # Data models & event flows
│   ├── concurrency/        # Asynchronous processing patterns
│   ├── development/        # Code organization & build process
│   ├── deployment/         # Infrastructure & deployment strategy
│   └── operational/        # Monitoring, logging, incident response
│
├── perspectives/           # Quality Attributes (8 Perspectives)
│   ├── security/          # Authentication, authorization, encryption
│   ├── performance/       # Response times, throughput, scaling
│   ├── availability/      # High availability, disaster recovery
│   ├── evolution/         # Extensibility, maintainability
│   ├── accessibility/     # UI/API usability, documentation
│   ├── development-resource/  # Team structure, skills, tools
│   ├── internationalization/  # Multi-language support
│   └── location/          # Geographic distribution, latency
│
├── architecture/          # Architecture Decisions & Patterns
│   ├── adrs/             # Architecture Decision Records
│   └── patterns/         # Design patterns used
│
├── operations/           # Operational Guides
│   ├── runbooks/        # Incident response procedures
│   ├── monitoring/      # Monitoring setup & dashboards
│   └── deployment/      # Deployment procedures
│
└── api/                 # API Documentation
    ├── rest/           # REST API specifications
    └── events/         # Domain event catalog
```

### 🔗 Key Documentation Links

| Category | Description | Link |
|----------|-------------|------|
| **Architecture Overview** | Complete methodology guide | [📖 Rozanski & Woods Guide](docs/rozanski-woods-methodology-guide.md) |
| **Viewpoints Index** | All 7 viewpoints overview | [📐 Viewpoints](docs/viewpoints/README.md) |
| **Perspectives Index** | All 8 perspectives overview | [🎯 Perspectives](docs/perspectives/README.md) |
| **Architecture Decisions** | ADR repository | [📋 ADRs](docs/architecture/adrs/README.md) |
| **Operations Guide** | Runbooks & procedures | [🔧 Operations](docs/operations/README.md) |
| **API Documentation** | REST API & Events | [🔌 API Docs](docs/api/README.md) |

### 📖 API Documentation

- **OpenAPI 3.0**: Complete API specification at `/api-docs`
- **Swagger UI**: Interactive API explorer at `/swagger-ui.html`
- **Event Catalog**: Domain events documentation in [docs/api/events/](docs/api/events/)
- **Postman Collection**: Pre-configured API requests (coming soon)

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
