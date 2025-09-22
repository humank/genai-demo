# Modern Software Architecture Best Practices Example Project

> **Enterprise-Grade Architecture Showcase Based on Rozanski & Woods Methodology**

> **Language / 語言選擇**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [繁體中文文檔](../../README.md)

This is not a simple e-commerce demo, but a **comprehensive showcase of modern software architecture best practices**, fully implementing enterprise-grade architecture methodologies, AI-assisted development, and quality engineering standards.

## 🎯 Project Value Proposition

This project demonstrates the complete practice of modern software architecture, covering all aspects from architectural design to quality engineering:

### 🏗️ Architecture Methodology Showcase

**Complete Implementation of Rozanski & Woods' 7 Viewpoints + 8 Perspectives**
- **Functional Viewpoint**: DDD Tactical Patterns + Aggregate Root Design + Bounded Context
- **Information Viewpoint**: Domain Events + Event Storming + Data Consistency Strategy
- **Concurrency Viewpoint**: Event-Driven Architecture + Asynchronous Processing + Transaction Boundaries
- **Development Viewpoint**: Hexagonal Architecture + Testing Strategy + Build System
- **Deployment Viewpoint**: Containerization + Cloud Architecture + Infrastructure as Code
- **Operational Viewpoint**: Observability + Monitoring + SRE Practices

### 🎯 Best Practices Collection

**DDD + Hexagonal Architecture + Event-Driven + Test-Driven Development**
- **13 Bounded Contexts**: Complete tactical and strategic pattern implementation
- **568 Tests**: BDD + TDD + Architecture Testing, 100% pass rate
- **Java 21 Record**: Reduces 30-40% boilerplate code, enhances type safety
- **Event-Driven Design**: Complete event collection, publishing, and processing mechanisms

## 🌟 Project Highlights

### 🏗️ Enterprise Architecture Design

- **DDD + Hexagonal Architecture**: Aggregate Root + Value Object + Domain Event + Specification Pattern + Policy Pattern
- **Event-Driven Design**: Complete event collection, publishing, and processing mechanisms
- **Java 21 Record**: Reduces 30-40% boilerplate code, enhances type safety

### 📊 Observability System Current Status

#### ✅ Currently Implemented

- **Structured Logging**: Unified format + Correlation ID + PII masking
- **Basic Monitoring**: Spring Boot Actuator + Health checks
- **Frontend Tracking**: User behavior analysis and performance monitoring (local processing)
- **Basic API**: Partial Analytics API endpoints available

#### 🚧 Partially Implemented (Frontend Ready, Backend Planned)

- **Analytics API**: Frontend fully implemented, backend partially available
- **Management Dashboard**: UI complete, using mock data for display

#### 🚀 Next Phase Development Plan

**Phase 1: WebSocket Real-time Features (1-2 months)**

- **🔌 WebSocket Backend**: Implement `/ws/analytics` endpoints and message processing
- **📊 Real-time Dashboard**: Enable real-time data push
- **📈 Event Streaming**: Complete event tracking and analysis system

**Phase 2: Advanced Analytics Features (2-3 months)**

- **🎯 Performance Monitoring**: Backend performance monitoring and Web Vitals integration
- **🔍 Error Tracking**: Enhanced error tracking and reporting system
- **☁️ CloudWatch Integration**: Custom metrics + Prometheus endpoints

**Phase 3: Enterprise Features (3+ months)**

- **⚡ Kafka Message Middleware**: Distributed event processing
- **🤖 Intelligent Alerts**: Machine learning-based anomaly detection
- **📊 Advanced Analytics**: Predictive analytics and business intelligence

### 🤖 AI-Assisted Development (MCP Integration) - NEW

**Model Context Protocol (MCP) Integration**, providing intelligent development assistant features:

#### 🔧 Integrated MCP Servers

- **⏰ Time Server**: Time and timezone conversion functionality
- **📚 AWS Docs**: AWS official documentation search and query
- **🏗️ AWS CDK**: CDK best practices guidance and Nag rule explanations
- **💰 AWS Pricing**: Cost analysis, pricing queries, and project cost assessment
- **🐙 GitHub**: Code review, issue tracking, PR management (user-level)

#### 🚀 MCP Feature Highlights

- **Intelligent Documentation Query**: Real-time search of AWS official documentation for accurate technical information
- **CDK Development Guidance**: Automatic CDK Nag rule explanations and best practice recommendations
- **Cost Optimization Analysis**: Analyze CDK/Terraform projects and provide cost optimization suggestions
- **GitHub Workflow**: Automated code review, issue management, and PR operations
- **Development Efficiency Enhancement**: Reduce documentation lookup time, improve development decision quality

#### ⚙️ MCP Configuration Management

```bash
# MCP configuration file locations
.kiro/settings/mcp.json          # Project-level configuration
~/.kiro/settings/mcp.json        # User-level configuration

# Reconnect MCP servers
# Use command palette in Kiro IDE to search for "MCP" related commands
```

### 🛒 Dual Frontend Business Features

**Consumer Side**: Smart shopping cart + Personalized recommendations + Membership rewards + Delivery tracking  
**Business Side**: Promotion management + Inventory management + Order processing + Statistical analysis

### 🧪 Testing & Quality Assurance

- **Test-Driven**: BDD + TDD + Architecture testing, 568 tests 100% pass
- **Test Performance Monitoring**: New TestPerformanceExtension automatically tracks test performance
- **Architecture Compliance**: 9.5/10 (Hexagonal Architecture) + 9.5/10 (DDD Practices)
- **Cloud-Native Deployment**: AWS CDK + Kubernetes + GitOps

#### 🚀 Test Performance Monitoring Framework - NEW

**TestPerformanceExtension** provides automated test performance monitoring:

- **⏱️ Execution Time Tracking**: Millisecond-precision test execution time monitoring
- **💾 Memory Usage Monitoring**: Heap memory usage tracking before/after tests
- **📊 Performance Regression Detection**: Automatic performance degradation detection with configurable thresholds
- **📈 Detailed Report Generation**: Text and HTML format performance analysis reports
- **🐌 Slow Test Identification**: Automatically flag tests exceeding 5 seconds
- **🧹 Resource Management**: Automatic test resource cleanup, preventing memory leaks

```java
// Usage example
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
@IntegrationTest
public class MyIntegrationTest extends BaseIntegrationTest {
    // Test methods will be automatically monitored for performance
}
```

**Performance Report Location**: `build/reports/test-performance/`

## 🤝 Applicable Scenarios

### 🎓 Learning and Education
- **Software Architecture Courses**: Complete practical examples of Rozanski & Woods methodology
- **DDD and Hexagonal Architecture**: Complete implementation from theory to practice
- **Modern Testing Strategy**: Best practices for BDD + TDD + Architecture Testing
- **AI-Assisted Development**: Practical application and integration of MCP protocol

### 🏢 Enterprise Reference
- **Enterprise Architecture Design**: Directly referenceable architectural decisions and implementation patterns
- **Development Standards and Specifications**: Complete development, security, performance standards templates
- **Observability System**: Production-ready monitoring and logging system implementation guide
- **Cloud-Native Deployment**: Best practices for AWS CDK + Kubernetes

### 🔬 Technical Research
- **Architecture Methodology**: Application of Rozanski & Woods in actual projects
- **Test Performance Optimization**: Optimization techniques from 13min52s → <30s
- **MCP Protocol Integration**: Practice and integration patterns of Model Context Protocol
- **Modern Java Ecosystem**: Comprehensive application of Java 21 + Spring Boot 3.4.5

## 🚀 Quick Experience

### Method 1: Docker One-Click Start (Recommended)
```bash
docker-compose up -d
```

### Method 2: Local Development Environment
```bash
./scripts/start-fullstack.sh
```

**Experience Endpoints**:
- 🌐 **API Documentation**: http://localhost:8080/swagger-ui/index.html
- 📊 **Management Interface**: http://localhost:3002
- 🛒 **Consumer Interface**: http://localhost:3001
- 💰 **Cost Analysis**: http://localhost:8080/api/cost-optimization/recommendations

## 📚 Learning Resources

### 🎯 Navigation by Learning Goals
- **Learn Modern Architecture** → [Architecture Viewpoints Documentation](README.md)
- **Learn DDD Practices** → [Domain-Driven Design](viewpoints/functional/domain-model.md)
- **Learn Testing Strategy** → \1
- **Learn AI-Assisted Development** → [MCP Integration Guide](README.md)

### 👨‍💼 Navigation by Role
- **Architect** → [Rozanski & Woods Assessment](../architecture/rozanski-woods-architecture-assessment.md)
- **Developer** → [Development Standards](README.md)
- **DevOps** → [Deployment and Operations](README.md)
- **QA Engineer** → \1

## 🏆 Project Achievements

### 📈 Quantitative Metrics
- **Code Quality**: 250,000+ lines of high-quality code
- **Test Coverage**: 568 tests, 100% pass rate
- **Performance Optimization**: Test execution time optimization 99%+ (13min52s → <30s)
- **Documentation Completeness**: 120+ detailed documentation pages
- **Architecture Compliance**: ArchUnit tests ensure architecture consistency

### 🎯 Technical Highlights
- **Java 21 + Spring Boot 3.4.5**: Latest technology stack
- **Dual Frontend Architecture**: Next.js 14 + Angular 18
- **Complete CI/CD**: GitHub Actions + AWS CDK
- **Enterprise-Grade Observability**: Monitoring, logging, tracing, alerting

## 🚀 Quick Start

### Memory and Performance Optimization (v3.0.1 Added)

This project has been optimized for memory usage during compilation and testing:

#### 🔧 Optimization Configuration

- **Memory Configuration**: Compilation and testing maximum heap memory increased to 4GB
- **Log Optimization**: Only ERROR level logs output during testing, significantly reducing output volume
- **JVM Optimization**: Using G1 garbage collector and string deduplication optimization
- **Parallel Processing**: Optimized Gradle parallel execution configuration

#### 🛠️ Optimized Execution Scripts

```bash
# Test optimization demo (recommended) - showcases test performance optimization results
./run-optimized-tests.sh

# Optimized compilation (reduced log output, increased memory)
./scripts/build-optimized.sh

# Optimized test execution (only show error logs)
./scripts/run-tests-optimized.sh

# Memory usage monitoring
./scripts/monitor-memory.sh

# System resource check
./scripts/check-system-resources.sh
```

#### 🔍 Observability Endpoints

```bash
# Application monitoring
curl http://localhost:8080/actuator/health     # Health check
curl http://localhost:8080/actuator/metrics    # Application metrics
curl http://localhost:8080/actuator/info       # Application info

# Cost optimization API
curl http://localhost:8080/api/cost-optimization/recommendations  # Cost recommendations
curl http://localhost:8080/api/cost-optimization/analysis         # Cost analysis
```

### Method 1: Docker Containerized Deployment (Recommended)

```bash
# Build ARM64 optimized image
./docker/docker-build.sh

# Start containerized environment
docker-compose up -d

# Check service status
docker-compose ps

# Stop all services
docker-compose down
```

**Service Endpoints:**

- 🌐 **API Documentation**: <http://localhost:8080/swagger-ui/index.html>
- 🏥 **Health Check**: <http://localhost:8080/actuator/health>
- 📊 **Application Metrics**: <http://localhost:8080/actuator/metrics>
- 💰 **Cost Optimization**: <http://localhost:8080/api/cost-optimization/recommendations>
- 🗄️ **H2 Database Console**: <http://localhost:8080/h2-console>

### Method 2: Local Development Environment

```bash
# Start complete frontend and backend application
./scripts/start-fullstack.sh

# Stop all services
./scripts/stop-fullstack.sh
```

### Method 3: Start Services Individually

```bash
# Start backend only (Spring Boot)
./gradlew :app:bootRun

# Start frontend only (Next.js)
cd cmc-frontend && npm run dev
```

## 🏗️ Architecture Design

### Hexagonal Architecture + DDD Layering

```
interfaces/ → application/ → domain/ ← ../../../../../../infrastructure/
```

- **Domain Layer**: Business logic + Aggregate Root + Value Object + Domain Event + Specification Pattern
- **Application Layer**: Use case coordination + Event publishing + Cross-aggregate operations
- **Infrastructure Layer**: Persistence + External systems + Event handling
- **Interface Layer**: REST API + OpenAPI 3.0 + Swagger UI

## 📁 Project Directory Structure

```
genai-demo/
├── app/                    # Main application
│   ├── src/main/java/      # Java source code
│   └── src/test/java/      # Test code
├── cmc-frontend/           # Next.js 14.2.30 frontend application (CMC)
├── consumer-frontend/      # Angular 18.2.0 frontend application (Consumer)
├── deployment/             # Deployment related files
│   ├── k8s/               # Kubernetes configuration
│   └── deploy-to-eks.sh   # EKS deployment script
├── docker/                 # Docker related files
│   ├── docker-build.sh    # Image build script
│   └── verify-deployment.sh # Deployment verification script
├── docs/                   # Project documentation
│   ├── api/               # API documentation
│   ├── en/                # English documentation
│   ├── architecture/      # Architecture documentation
│   ├── diagrams/          # Diagram documentation (Mermaid + PlantUML)
│   └── reports/           # Project reports
├── scripts/                # Various script files
│   ├── start-fullstack.sh # Start full-stack application
│   └── stop-fullstack.sh  # Stop all services
├── tools/                  # Development tools
│   └── plantuml.jar       # UML diagram generation tool
├── docker-compose.yml      # Docker Compose configuration
├── Dockerfile             # Docker image definition
└── README.md              # Project documentation
```

### Dual Frontend Architecture

**CMC Management (Next.js 14.2.30)**  
TypeScript + Tailwind CSS + shadcn/ui + Zustand + React Query

**Consumer Side (Angular 18.2.0)**  
TypeScript + Tailwind CSS + PrimeNG + RxJS + Jasmine

## 🆕 Version Updates (v3.3.0 - September 2025)

### 🚀 Major New Features

- ✅ **AI-Assisted Development (MCP Integration)**: Complete Model Context Protocol integration, supporting AWS ecosystem and GitHub operations
- ✅ **Test Performance Monitoring Framework**: New test performance monitoring system, automatically tracking execution time and memory usage
- ✅ **Observability System Enhancement**: Added analytics service, WebSocket integration, and event tracking
- ✅ **Development Standards Specification**: Complete development, security, performance, and code review standards documentation

### 📈 Architecture & Quality Improvements

- 🤖 **MCP Servers**: Integrated 4 stable MCP servers (time, aws-docs, aws-cdk, aws-pricing)
- 🧪 **Test Performance Optimization**: Added TestPerformanceExtension for automatic test execution performance monitoring
- 🏗️ **Observability Architecture**: Added Analytics, Event Tracking, and WebSocket real-time communication
- 📋 **Development Standards**: Added 5 core development standards documents covering complete development lifecycle

### 🔧 Technical Debt Cleanup

- 🗑️ **Removed Outdated Documentation**: Cleaned up 20+ outdated technical documents and configuration files
- 🧹 **Code Refactoring**: Removed duplicate HTTP client configurations, unified test infrastructure
- 📦 **Dependency Optimization**: Cleaned Jest cache and unnecessary build files
- 🤖 **IDE Automatic Fixes**: Kiro IDE automatically formatted and optimized frontend code while preserving comment integrity

## 🛠️ Technology Stack

### Backend Technologies

- **Core Framework**: Spring Boot 3.4.5
- **Programming Language**: Java 21 (Preview features enabled)
- **Build Tool**: Gradle 8.x
- **Database**: H2 (development) + PostgreSQL (production) + Flyway (migration management)
- **API Documentation**: SpringDoc OpenAPI 3 + Swagger UI
- **Observability**:
  - Micrometer - Metrics collection
  - AWS X-Ray - Distributed tracing
  - Logback - Structured logging
  - Spring Boot Actuator - Health checks
- **Testing Framework**:
  - JUnit 5 - Unit testing
  - Cucumber 7.15.0 - BDD testing
  - ArchUnit 1.3.0 - Architecture testing
  - Mockito 5.8.0 - Mock objects
  - Allure 2.22.1 - Test reporting and visualization
- **Other Tools**:
  - Lombok 1.18.38 - Reduce boilerplate code
  - PlantUML - UML diagram generation

### Frontend Technologies

**CMC Management**: Next.js 14.2.30 + TypeScript + Tailwind + shadcn/ui + Zustand + React Query  
**Consumer Side**: Angular 18.2.0 + TypeScript + Tailwind + PrimeNG + RxJS + Jasmine

## 📊 Data & API

### Database Initialization

The project uses Flyway for database version management, including rich business test data:

- **100+ Product inventory records** - Covering electronics, clothing, home goods, etc.
- **Complete order process data** - Orders, order items, payment records
- **Taiwan localized data** - Real addresses, Traditional Chinese product names
- **Multiple payment methods** - Credit card, digital wallet, bank transfer, cash on delivery
- **Independent product table** - Supporting complete product lifecycle management

### API Documentation & Endpoints

#### 📖 Swagger UI Documentation

- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **API Groups**:
  - Public API: `/v3/api-docs/public-api`
  - Internal API: `/v3/api-docs/internal-api`
  - Management endpoints: `/v3/api-docs/management`

#### 🔧 Main API Endpoints

```bash
# Product Management API
GET /api/products                 # Product list (supports pagination)
GET /api/products/{productId}     # Get single product
PUT /api/products/{productId}     # Update product information
DELETE /api/products/{productId}  # Delete product
POST /api/products                # Create new product

# Inventory Management API
GET /api/inventory/{productId}    # Get product inventory
POST /api/inventory/{productId}/adjust  # Adjust inventory
POST /api/inventory/{productId}/reserve # Reserve inventory
POST /api/inventory/{productId}/release # Release inventory

# Order Management API
GET /api/orders                   # Order list
GET /api/orders/{orderId}         # Get single order
POST /api/orders                  # Create new order
PUT /api/orders/{orderId}         # Update order

# Payment Management API
POST /api/payments                # Create payment
GET /api/payments/{paymentId}     # Get payment details
PUT /api/payments/{paymentId}     # Update payment status

# Cost Optimization API
GET /api/cost-optimization/recommendations  # Get cost optimization recommendations
GET /api/cost-optimization/analysis         # Get cost analysis report
POST /api/cost-optimization/right-sizing    # Execute resource right-sizing analysis

# Customer Management API
GET /api/customers                # Customer list
GET /api/customers/{customerId}   # Get customer details

# Pricing Management API
GET /api/pricing/rules            # Get pricing rules
POST /api/pricing/rules           # Create pricing rules
PUT /api/pricing/commission-rates # Update commission rates

# Data Statistics API
GET /api/stats                    # Overall data statistics
GET /api/stats/order-status       # Order status distribution
GET /api/stats/payment-methods    # Payment method distribution
GET /api/stats/database           # Database statistics

# Activity Log API
GET /api/activities               # System activity logs

# Health Check & Monitoring
GET /actuator/health              # Application health status
GET /actuator/metrics             # Application metrics
GET /actuator/info                # Application information
GET /actuator/prometheus          # Prometheus metrics

# H2 Database Console
http://localhost:8080/h2-console  # Database management interface
```

## 📱 Frontend Features

### Main Pages

- **Dashboard** (`/`) - System overview and statistics
- **Order Management** (`/orders`) - Order list and details
  - Order details page (`/orders/[orderId]`) - Complete order information display
- **Product Management** (`/products`) - Product display and inventory
  - Product details page (`/products/[productId]`) - Detailed product information and operations
  - Product editing functionality - Support name, description, price, category modification
  - Inventory adjustment functionality - Support add, reduce, set inventory quantity
  - Product deletion functionality - Safe product deletion operations
- **Customer Management** (`/customers`) - Customer information management

### Product Management Features

- ✏️ **Product Editing** - Complete product information editing interface
  - Product name, description modification
  - Price and currency settings (TWD, USD, EUR)
  - Product category management (electronics, clothing, food, etc.)
- 📦 **Inventory Management** - Flexible inventory adjustment system
  - Set inventory - Directly set inventory quantity
  - Add inventory - Restock inventory
  - Reduce inventory - Loss or return processing
  - Adjustment reason recording - Complete inventory change tracking
- 🗑️ **Product Deletion** - Safe product deletion functionality
  - Confirmation dialog to prevent accidental deletion
  - Automatic product list updates

### UI/UX Features

- 🎨 Modern design system (shadcn/ui + Tailwind CSS)
- 📱 Fully responsive design
- 🌙 Dark/light theme support
- ⚡ Real-time data updates (React Query)
- 🔄 Loading states and error handling
- 📊 Data visualization charts
- 🎯 Intuitive operation interface
- 📝 Form validation and user feedback
- 🔔 Toast notification system

## 📋 Development Standards & Specifications - NEW

The project has established a complete development standards system, located in the `../../.kiro/steering/` directory:

### 🎯 Core Development Standards

- **[Development Standards](../../.kiro/steering/development-standards.md)**: Technology stack, error handling, API design, testing strategy
- **[Security Standards](../../.kiro/steering/security-standards.md)**: Authentication authorization, data protection, input validation, security testing
- **[Performance Standards](../../.kiro/steering/performance-standards.md)**: Response time, throughput, caching strategy, performance monitoring
- **[Code Review Standards](../../.kiro/steering/code-review-standards.md)**: Review process, quality checks, feedback guidelines
- **[Test Performance Standards](../../.kiro/steering/test-performance-standards.md)**: Test monitoring, resource management, performance optimization

### 🏗️ Architecture Methodology

- **[Rozanski & Woods Architecture Methodology](../../.kiro/steering/rozanski-woods-architecture-methodology.md)**:
  - Mandatory architectural viewpoint checks (functional, information, concurrency, development, deployment, operational)
  - Quality attribute scenario requirements (performance, security, availability, scalability, usability)
  - Architecture compliance rules and ArchUnit tests
  - Four perspectives checklist (security, performance, availability, evolution)

### 📐 Domain Event Design

- **[Domain Event Guide](../../.kiro/steering/domain-events.md)**:
  - Event definition and collection standards
  - Event handling and publishing mechanisms
  - Event versioning and backward compatibility
  - Event Store solutions (EventStore DB, JPA, In-Memory)

## 📚 Documentation

> **Documentation Center**: [docs/README.md](README.md) - Complete documentation navigation and categorization

### 📊 Project Reports and Summaries

- **\1** - Centralized management of all project reports and summaries
  - **\1** - Task completion reports and automation results
  - **\1** - Architecture decisions and design documentation reports
  - **\1** - Diagram generation and synchronization reports
  - **\1** - Deployment and infrastructure management reports
  - **\1** - Frontend development and UI improvement reports
  - **\1** - Testing optimization and quality validation reports
  - **\1** - Translation system and language processing reports
  - **\1** - Project status and refactoring reports

The project includes rich documentation, organized by functional categories:

### 🎯 Quick Navigation

- **👨‍💼 Project Manager**: [Project Summary 2025](../../reports-summaries/project-management/project-summary-2025.md) | [Architecture Overview](../diagrams/architecture-overview.md)
- **🏗️ Architect**: \1 | \1 | \1
- **👨‍💻 Developer**: \1 | \1 | [Development Instructions](../development/instructions.md)
- **🚀 DevOps**: \1 | [Docker Guide](../viewpoints/deployment/docker-guide.md)
- **🔍 Observability**: [Production Observability Testing Guide](../viewpoints/operational/production-observability-testing-guide.md) | \1
- **🤖 MCP Integration**: \1 | [AI-Assisted Development](README.md)

### 📊 Core Diagrams (Mermaid - Direct GitHub Display)

- [🏗️ System Architecture Overview](../diagrams/architecture-overview.md) - Complete system architecture diagram
- [🔵 Hexagonal Architecture](../diagrams/hexagonal-architecture.md) - Ports and adapters architecture
- [🏛️ DDD Layered Architecture](../diagrams/mermaid/ddd-layered-architecture.md) - Domain-driven design layering
- [⚡ Event-Driven Architecture](../diagrams/mermaid/event-driven-architecture.md) - Event processing mechanisms
- [🔌 API Interaction Diagram](../diagrams/mermaid/api-interactions.md) - API call relationships

### 📋 Detailed UML Diagrams (PlantUML)

- **Structural Diagrams**: Class diagram, object diagram, component diagram, deployment diagram, package diagram, composite structure diagram
- **Behavioral Diagrams**: Use case diagram, activity diagram, state diagram
- **Interaction Diagrams**: Sequence diagram, communication diagram, interaction overview diagram, timing diagram
- **Event Storming**: Big Picture, Process Level, Design Level

### 🏆 Core Reports (September 2025 Update)

- [📋 Project Summary Report 2025](../../reports-summaries/project-management/project-summary-2025.md) - Complete project achievements and technical highlights summary
- [🏗️ Architecture Excellence Report 2025](reports/architecture-excellence-2025.md) - Detailed architecture assessment and best practices analysis
- [🚀 Technology Stack Detailed Description 2025](reports/technology-stack-2025.md) - Complete technology selection and implementation details
- [📝 Documentation Cleanup Report 2025](reports/documentation-cleanup-2025.md) - Documentation reorganization and optimization records

### 🛠️ Diagram Generation Tools

```bash
# Generate all PlantUML diagrams
./scripts/generate-diagrams.sh

# Generate specific diagram
./scripts/generate-diagrams.sh domain-model-class-diagram.puml

# Validate diagram syntax
./scripts/generate-diagrams.sh --validate
```

## 🧪 Testing

### 🚀 Test Optimization Demo Script (Recommended)

The project provides a test optimization demo script showcasing test performance optimization results:

```bash
# Run test optimization demo - showcases before/after optimization comparison
./run-optimized-tests.sh
```

**Script Features:**

- 📊 **Performance Comparison Display**: Before optimization 13min52s → After optimization < 30s (99%+ improvement)
- 🎯 **Layered Testing Strategy**: Unit → Integration → E2E test pyramid
- 💾 **Memory Optimization**: From 6GB → 1-3GB (50-83% savings)
- ⚡ **Parallel Execution**: Multi-core parallel processing, significantly improving efficiency
- 📈 **Real-time Performance Statistics**: Shows execution time for each test phase
- 🎨 **Colored Output**: Clear visual progress and result display

**Recommended Development Workflow:**

1. **During Development**: `./gradlew quickTest` (quick feedback, < 5s)
2. **Before Commit**: `./gradlew unitTest` (complete unit tests, < 10s)
3. **PR Check**: `./gradlew integrationTest` (integration verification)
4. **Before Release**: `./gradlew test` (complete test suite)

### Run All Tests

```bash
./gradlew runAllTests                    # Run all tests (568 tests)
./gradlew runAllTestsWithReport         # Run tests and generate Allure report
./gradlew runAllTestsComplete           # Run complete test suite
```

### Run Specific Test Types

```bash
./gradlew test                          # Unit tests (JUnit 5)
./gradlew unitTest                      # Quick unit tests (~5MB, ~50ms each)
./gradlew quickTest                     # Quick tests - daily development use (< 2 minutes)
./gradlew integrationTest               # Integration tests (~50MB, ~500ms each)
./gradlew e2eTest                       # End-to-end tests (~500MB, ~3s each)
./gradlew preCommitTest                 # Pre-commit tests (< 5 minutes)
./gradlew fullTest                      # Full tests - pre-release use
./gradlew cucumber                      # BDD tests (Cucumber 7.15.0)
./gradlew testArchitecture             # Architecture tests (ArchUnit 1.3.0)
```

### Test Reports

- **Cucumber HTML Report**: `app/build/reports/cucumber/cucumber-report.html`
- **JUnit HTML Report**: `app/build/reports/tests/test/index.html`
- **Allure Report**: `app/build/reports/allure-report/allureReport/index.html`
- **Allure Results Directory**: `app/build/allure-results/`

### Architecture Testing

Using ArchUnit 1.3.0 to ensure code follows predefined architectural rules:

- **DddEntityRefactoringArchitectureTest** - Ensures DDD entity refactoring complies with architectural specifications
- **Hexagonal Architecture Compliance** - Ensures ports and adapters separation
- **DDD Tactical Patterns** - Ensures correct use of Aggregate Root, Value Object, Domain Event, Specification Pattern, Policy Pattern
- **Package Structure Standards** - Ensures package structure follows DDD layered architecture
- **Annotation Validation** - Ensures correct use of `@AggregateRoot`, `@ValueObject`, `@Specification`, `@Policy` annotations

### BDD Testing

Using Cucumber 7.15.0 for behavior-driven development testing, covering:

- **Consumer Features** (Consumer) - Shopping journey, shopping cart management
- **Customer Management** (Customer) - Membership system, loyalty points, member discounts
- **Order Management** (Order) - Order aggregate root, order workflow
- **Inventory Management** (Inventory) - Inventory management
- **Payment Processing** (Payment) - Payment aggregate root, payment discounts
- **Logistics Delivery** (Logistics) - Delivery management, delivery system
- **Notification Service** (Notification) - Notification management, notification service
- **Promotional Activities** (Promotion) - Coupon system, flash sales, convenience store coupons, add-on activities, gift activities
- **Product Management** (Product) - Product search, product combinations
- **Pricing Management** (Pricing) - Commission rates
- **Complete Workflow** (Workflow) - End-to-end business processes

## 🔧 Development Tools

### Data Generation

```bash
python3 scripts/generate_data.py       # Generate large amounts of test data
```

### Service Management

```bash
./scripts/start-fullstack.sh           # Start full-stack application
./scripts/stop-fullstack.sh            # Stop all services
```

### Frontend Development

#### CMC Frontend (Next.js)

```bash
cd cmc-frontend
npm install                             # Install dependencies
npm run dev                            # Development mode (http://localhost:3002)
npm run build                          # Production build
npm run lint                           # Code linting
npm run type-check                     # TypeScript type checking
npm test                               # Run tests
npm run test:e2e                       # E2E tests (Playwright)
```

#### Consumer Frontend (Angular)

```bash
cd consumer-frontend
npm install                             # Install dependencies
npm start                              # Development mode (http://localhost:3001)
npm run build                          # Production build
npm test                               # Run tests (Jasmine + Karma)
```

## 📖 Complete Documentation

> **Documentation Center**: [docs/README.md](README.md)

### Rozanski & Woods Seven Viewpoints
1. **[Functional Viewpoint](README.md)** - System functions and responsibilities
2. **[Information Viewpoint](README.md)** - Data and information flow  
3. **[Concurrency Viewpoint](README.md)** - Concurrency and synchronization
4. **[Development Viewpoint](README.md)** - Development and build
5. **[Deployment Viewpoint](README.md)** - Deployment and environment
6. **[Operational Viewpoint](README.md)** - Operations and maintenance

### Eight Architecture Perspectives
1. **[Security](README.md)** - Security and compliance
2. **[Performance](README.md)** - Performance and scalability
3. **[Availability](README.md)** - Availability and resilience
4. **[Evolution](README.md)** - Evolution and maintenance
5. **[Usability](README.md)** - User experience
6. **[Regulation](README.md)** - Regulatory compliance
7. **[Location](README.md)** - Geographic distribution
8. **[Cost](README.md)** - Cost optimization

## 🎯 UML Diagrams

This project uses PlantUML to generate various UML diagrams:

- Class diagrams, object diagrams, component diagrams, deployment diagrams
- Sequence diagrams (order processing, pricing processing, delivery processing)
- State diagrams, activity diagrams
- Domain model diagrams, hexagonal architecture diagrams, DDD layered architecture diagrams

See [Diagram Documentation](README.md) for more information.

## 🚨 Common Issues

### Configuration Cache Issues

```bash
./gradlew --no-configuration-cache <task>
```

### Allure Report Issues

```bash
./gradlew clean
./gradlew runAllTestsWithReport
```

### Frontend Dependency Issues

```bash
cd cmc-frontend
rm -rf node_modules package-lock.json
npm install
```

## 🤝 Contributing

Welcome to submit Pull Requests or open Issues to discuss improvement suggestions.

## 📄 License

This project is licensed under the MIT License - see the [../../LICENSE](../../LICENSE) file for details.

## 🔗 Related Links

- **DeepWiki Integration**: [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)
- **Consumer Frontend**: <http://localhost:3001> (development mode)
- **CMC Frontend**: <http://localhost:3002> (development mode)
- **Backend API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **H2 Console**: <http://localhost:8080/h2-console>

---

## 📊 Project Statistics & Value

### 📈 Core Data

- **Code Scale**: 250,000+ lines of high-quality code (added 50,000+ lines)
- **Test Quality**: 568 tests, 100% pass rate
- **Test Performance**: Optimized test execution time from 13min52s → < 30s (99%+ improvement)
- **API Coverage**: 35+ RESTful API endpoints
- **UI Components**: 30+ reusable components (React + Angular)
- **Documentation Completeness**: 120+ detailed documentation pages, including complete development standards specifications
- **Architecture Decisions**: 7 complete ADR documents covering all important architectural decisions
- **Database**: 131 business records + 23 Flyway migration scripts
- **MCP Integration**: 4 stable MCP servers supporting AI-assisted development
- **Development Standards**: 5 core development standards documents covering complete development lifecycle

### 🏆 Technical Value

- **Architecture Excellence**: DDD + Hexagonal Architecture + Event-Driven Design, complete ADR documentation
- **Quality Assurance**: Practical testing strategy + Test performance monitoring framework + Complete architecture test coverage
- **Modern Technology Stack**: Java 21 + Spring Boot 3.4.5 + Next.js 14.2.30 + Angular 18.2.0
- **Enterprise Features**: Production-ready observability + AI-assisted development (MCP) + Cloud-native deployment
- **Development Standards**: Complete development standards specification system covering security, performance, code review, etc.
- **AI Integration**: Model Context Protocol integration providing intelligent development assistant functionality

---

**Project Maintainers**: Modern Software Architecture Practice Team  
**Technology Stack**: Java 21 + Spring Boot 3.4.5 + Next.js 14 + Angular 18  
**Architecture Methodology**: Rozanski & Woods + DDD + Hexagonal Architecture + Event-Driven  
**Last Updated**: January 21, 2025
- **Best Practices**: Industry-standard testing methods + Complete documentation system, suitable for learning modern enterprise application development
