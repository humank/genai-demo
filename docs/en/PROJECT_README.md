# GenAI Demo - Enterprise E-commerce Platform Demonstration Project

> **Language / 語言選擇**  
> 🇺🇸 **English**: You are reading the English version  
> 🇹🇼 **繁體中文**: [Chinese Documentation](../../README.md)

A full-stack e-commerce platform based on DDD + Hexagonal Architecture, integrating enterprise-grade observability, AI-assisted development, and cloud-native deployment modern applications.

## 🌟 Project Highlights

### 🏗️ Enterprise-Grade Architecture Design

- **DDD + Hexagonal Architecture**: Aggregate Root + Value Object + Domain Event + Specification Pattern + Policy Pattern
- **Event-Driven Design**: Complete event collection, publishing, and processing mechanisms
- **Java 21 Record**: Reduces 30-40% boilerplate code, enhances type safety

### 📊 Complete Observability System (v3.3.0 Enhanced!)

- **Distributed Tracing**: AWS X-Ray + Jaeger cross-service request tracing
- **Structured Logging**: Unified format + Correlation ID + PII masking
- **Business Metrics**: CloudWatch custom metrics + Prometheus endpoints
- **Cost Optimization**: Resource right-sizing analysis + Real-time cost tracking
- **Real-time Analytics**: WebSocket integration + Event tracking + User behavior analysis (NEW!)

#### 🆕 New Observability Features

- **📊 Analytics Service**: User behavior analysis and business metrics collection
- **🔌 WebSocket Integration**: Real-time data push and event notifications
- **📈 Event Tracking**: Complete event tracking and analysis system
- **🎯 Performance Monitoring**: Frontend performance monitoring and Web Vitals tracking
- **🔍 Error Tracking**: Enhanced error tracking and reporting system

### 🤖 AI-Assisted Development (MCP Integration) - NEW

**Model Context Protocol (MCP) Integration**, providing intelligent development assistant features:

#### 🔧 Integrated MCP Servers

- **⏰ Time Server**: Time and timezone conversion functionality
- **📚 AWS Docs**: AWS official documentation search and queries
- **🏗️ AWS CDK**: CDK best practices guidance and Nag rule explanations
- **💰 AWS Pricing**: Cost analysis, pricing queries, and project cost assessment
- **🐙 GitHub**: Code review, issue tracking, PR management (user-level)

#### 🚀 MCP Feature Highlights

- **Intelligent Documentation Queries**: Real-time search of AWS official documentation for accurate technical information
- **CDK Development Guidance**: Automatically explain CDK Nag rules, provide best practice recommendations
- **Cost Optimization Analysis**: Analyze CDK/Terraform projects, provide cost optimization suggestions
- **GitHub Workflow**: Automated code review, issue management, and PR operations
- **Development Efficiency Enhancement**: Reduce documentation search time, improve development decision quality

#### ⚙️ MCP Configuration Management

```bash
# MCP configuration file locations
.kiro/settings/mcp.json          # Project-level configuration
~/.kiro/settings/mcp.json        # User-level configuration

# Reconnect MCP servers
# Use command palette in Kiro IDE to search for "MCP" related commands
```

### 🛒 Dual Frontend Business Features

**Consumer Side**: Smart shopping cart + Personalized recommendations + Member rewards + Delivery tracking  
**Business Side**: Promotion management + Inventory management + Order processing + Statistical analysis

### 🧪 Testing and Quality Assurance

- **Test-Driven**: BDD + TDD + Architecture testing, 568 tests 100% pass
- **Test Performance Monitoring**: Brand new TestPerformanceExtension automatically tracks test performance
- **Architecture Compliance**: 9.5/10 (Hexagonal Architecture) + 9.5/10 (DDD Practices)
- **Cloud-Native Deployment**: AWS CDK + Kubernetes + GitOps

#### 🚀 Test Performance Monitoring Framework - NEW

**TestPerformanceExtension** provides automated test performance monitoring:

- **⏱️ Execution Time Tracking**: Millisecond-precision test execution time monitoring
- **💾 Memory Usage Monitoring**: Heap memory usage tracking before and after tests
- **📊 Performance Regression Detection**: Automatic detection of performance degradation with configurable thresholds
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
# Test optimization demonstration (Recommended) - Shows test performance optimization results
./run-optimized-tests.sh

# Optimized compilation (Reduced log output, increased memory)
./scripts/build-optimized.sh

# Optimized test execution (Only shows error logs)
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
# Build ARM64 optimized images
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
# Start complete frontend and backend applications
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

### Hexagonal Architecture + DDD Layers

```
interfaces/ → application/ → domain/ ← infrastructure/
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
- ✅ **Test Performance Monitoring Framework**: Brand new test performance monitoring system, automatically tracking execution time and memory usage
- ✅ **Observability System Enhancement**: Added analytics services, WebSocket integration, and event tracking
- ✅ **Development Standards Specification**: Complete development, security, performance, and code review standard documentation

### 📈 Architecture and Quality Improvements

- 🤖 **MCP Servers**: Integrated 4 stable MCP servers (time, aws-docs, aws-cdk, aws-pricing)
- 🧪 **Test Performance Optimization**: Added TestPerformanceExtension to automatically monitor test execution performance
- 🏗️ **Observability Architecture**: Added Analytics, Event Tracking, and WebSocket real-time communication
- 📋 **Development Standards**: Added 5 core development standard documents, covering the complete development lifecycle

### 🔧 Technical Debt Cleanup

- 🗑️ **Removed Outdated Documentation**: Cleaned up 20+ outdated technical documents and configuration files
- 🧹 **Code Refactoring**: Removed duplicate HTTP client configurations, unified test infrastructure
- 📦 **Dependency Optimization**: Cleaned up Jest cache and unnecessary build files

## 🛠️ Technology Stack

### Backend Technologies

- **Core Framework**: Spring Boot 3.4.5
- **Programming Language**: Java 21 (Preview features enabled)
- **Build Tool**: Gradle 8.x
- **Database**: H2 (Development) + PostgreSQL (Production) + Flyway (Migration management)
- **API Documentation**: SpringDoc OpenAPI 3 + Swagger UI
- **Observability**:
  - Micrometer - Metrics collection
  - AWS X-Ray - Distributed tracing
  - Logback - Structured logging
  - Spring Boot Actuator - Health checks
- **Testing Frameworks**:
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

## 🧪 Testing

### 🚀 Test Optimization Demonstration Script (Recommended)

The project provides a test optimization demonstration script that showcases the results of test performance optimization:

```bash
# Run test optimization demonstration - Shows before and after optimization comparison
./run-optimized-tests.sh
```

**Script Features:**

- 📊 **Performance Comparison Display**: Before optimization 13min 52sec → After optimization < 30sec (99%+ improvement)
- 🎯 **Layered Testing Strategy**: Unit → Integration → E2E test pyramid
- 💾 **Memory Optimization**: From 6GB → 1-3GB (50-83% savings)
- ⚡ **Parallel Execution**: Multi-core parallel processing, significantly improving efficiency
- 📈 **Real-time Performance Statistics**: Shows execution time for each test phase
- 🎨 **Colored Output**: Clear visual progress and result display

**Recommended Development Workflow:**

1. **During Development**: `./gradlew quickTest` (Quick feedback, < 5 seconds)
2. **Before Commit**: `./gradlew unitTest` (Complete unit tests, < 10 seconds)
3. **PR Check**: `./gradlew integrationTest` (Integration verification)
4. **Before Release**: `./gradlew test` (Complete test suite)

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
./gradlew quickTest                     # Quick tests - for daily development (< 2 minutes)
./gradlew integrationTest               # Integration tests (~50MB, ~500ms each)
./gradlew e2eTest                       # End-to-end tests (~500MB, ~3s each)
./gradlew preCommitTest                 # Pre-commit tests (< 5 minutes)
./gradlew fullTest                      # Complete tests - for pre-release use
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
- **Hexagonal Architecture Compliance** - Ensures port and adapter separation
- **DDD Tactical Patterns** - Ensures correct use of Aggregate Root, Value Object, Domain Event, Specification Pattern, Policy Pattern
- **Package Structure Standards** - Ensures package structure complies with DDD layered architecture
- **Annotation Validation** - Ensures correct use of `@AggregateRoot`, `@ValueObject`, `@Specification`, `@Policy` annotations

### BDD Testing

Using Cucumber 7.15.0 for Behavior-Driven Development testing, covering:

- **Consumer Features** (Consumer) - Shopping journey, shopping cart management
- **Customer Management** (Customer) - Membership system, reward points, member discounts
- **Order Management** (Order) - Order aggregate root, order workflow
- **Inventory Management** (Inventory) - Inventory management
- **Payment Processing** (Payment) - Payment aggregate root, payment discounts
- **Logistics Delivery** (Logistics) - Delivery management, delivery system
- **Notification Service** (Notification) - Notification management, notification service
- **Promotional Activities** (Promotion) - Coupon system, flash sales, convenience store coupons, add-on activities, gift activities
- **Product Management** (Product) - Product search, product combinations
- **Pricing Management** (Pricing) - Commission rates
- **Complete Workflow** (Workflow) - End-to-end business processes

## 📚 Documentation

> **Documentation Center**: [docs/README.md](docs/README.md) - Complete documentation navigation and categorization

The project includes rich documentation, organized by functionality:

### 🎯 Quick Navigation

- **👨‍💼 Project Manager**: [Project Summary 2025](docs/reports/project-summary-2025.md) | [Architecture Overview](docs/diagrams/mermaid/architecture-overview.md)
- **🏗️ Architect**: [Architecture Decision Records](docs/architecture/adr/) | [Architecture Documentation](docs/architecture/) | [Diagram Documentation](docs/diagrams/)
- **👨‍💻 Developer**: [Development Guide](docs/development/) | [API Documentation](docs/api/) | [Development Instructions](docs/development/instructions.md)
- **🚀 DevOps**: [Deployment Documentation](docs/deployment/) | [Docker Guide](docs/deployment/docker-guide.md)
- **🔍 Observability**: [Production Environment Testing Guide](docs/observability/production-observability-testing-guide.md) | [Observability System](docs/observability/)
- **🤖 MCP Integration**: [MCP Guide](docs/mcp/) | [AI-Assisted Development](docs/mcp/README.md)

### 📊 Core Diagrams (Mermaid - Direct GitHub Display)

- [🏗️ System Architecture Overview](docs/diagrams/mermaid/architecture-overview.md) - Complete system architecture diagram
- [🔵 Hexagonal Architecture](docs/diagrams/mermaid/hexagonal-architecture.md) - Port and adapter architecture
- [🏛️ DDD Layered Architecture](docs/diagrams/mermaid/ddd-layered-architecture.md) - Domain-Driven Design layers
- [⚡ Event-Driven Architecture](docs/diagrams/mermaid/event-driven-architecture.md) - Event processing mechanisms
- [🔌 API Interaction Diagram](docs/diagrams/mermaid/api-interactions.md) - API call relationships

### 📋 Detailed UML Diagrams (PlantUML)

- **Structural Diagrams**: Class diagrams, object diagrams, component diagrams, deployment diagrams, package diagrams, composite structure diagrams
- **Behavioral Diagrams**: Use case diagrams, activity diagrams, state diagrams
- **Interaction Diagrams**: Sequence diagrams, communication diagrams, interaction overview diagrams, timing diagrams
- **Event Storming**: Big Picture, Process Level, Design Level

### 🏆 Core Reports (September 2025 Update)

- [📋 Project Summary Report 2025](docs/reports/project-summary-2025.md) - Complete project achievements and technical highlights summary
- [🏗️ Architecture Excellence Report 2025](docs/reports/architecture-excellence-2025.md) - Detailed architecture assessment and best practices analysis
- [🚀 Technology Stack Detailed Description 2025](docs/reports/technology-stack-2025.md) - Complete technology selection and implementation details
- [📝 Documentation Cleanup Report 2025](docs/reports/documentation-cleanup-2025.md) - Documentation reorganization and optimization records

### 🛠️ Diagram Generation Tools

```bash
# Generate all PlantUML diagrams
./scripts/generate-diagrams.sh

# Generate specific diagram
./scripts/generate-diagrams.sh domain-model-class-diagram.puml

# Validate diagram syntax
./scripts/generate-diagrams.sh --validate
```

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

## 🤝 Contributing

Welcome to submit Pull Requests or open Issues to discuss improvement suggestions.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Links

- **DeepWiki Integration**: [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/humank/genai-demo)
- **Consumer Frontend**: <http://localhost:3001> (Development mode)
- **CMC Frontend**: <http://localhost:3002> (Development mode)
- **Backend API**: <http://localhost:8080>
- **Swagger UI**: <http://localhost:8080/swagger-ui/index.html>
- **OpenAPI Specification**: <http://localhost:8080/v3/api-docs>
- **H2 Console**: <http://localhost:8080/h2-console>

---

## 📊 Project Statistics and Value

### 📈 Core Data

- **Code Scale**: 200,000+ lines of high-quality code
- **Test Quality**: 568 tests, 100% pass rate
- **Test Performance**: Optimized test execution time from 13min 52sec → < 30sec (99%+ improvement)
- **API Coverage**: 35+ RESTful API endpoints
- **UI Components**: 25+ reusable components (React + Angular)
- **Documentation Completeness**: 100+ detailed documentation pages, including 67-page production environment guide
- **Architecture Decisions**: 7 complete ADR documents, covering all important architectural decisions
- **Database**: 131 business records + 22 Flyway migration scripts

### 🏆 Technical Value

- **Architecture Excellence**: DDD + Hexagonal Architecture + Event-Driven Design, complete ADR documentation records
- **Quality Assurance**: Practical testing strategy + Production environment best practices + Complete architecture test coverage
- **Modern Technology Stack**: Java 21 + Spring Boot 3.4.5 + Next.js 14.2.30 + Angular 18.2.0
- **Enterprise Features**: Production-ready observability + AI-assisted development + Cloud-native deployment
- **Best Practices**: Industry-standard testing methods + Complete documentation system, suitable for learning modern enterprise application development
